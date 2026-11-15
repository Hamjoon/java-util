package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NavigableSet;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentModificationException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_pollLast_Test {

    private NavigableSet<String> set;

    // the internal sentinel that represents a masked null
    private static Object NULL_SENTINEL;

    @BeforeAll
    static void captureNullSentinel() throws Exception {
        // The class uses a private static final Object (commonly named NULL) to mask nulls.
        // Locate it via reflection so that we can return it from the mock.
        for (Field f : ConcurrentNavigableSetNullSafe.class.getDeclaredFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers()) && java.lang.reflect.Modifier.isFinal(f.getModifiers()) && f.getType() == Object.class) {
                f.setAccessible(true);
                NULL_SENTINEL = f.get(null);
                break;
            }
        }
        // If the implementation changes and no sentinel is found, the tests that rely on it will fail,
        // which is appropriate because the contract would have changed.
        assertNotNull(NULL_SENTINEL, "Unable to locate the internal null‑mask sentinel");
    }

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh instance of the set under test
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to aid GC and to guarantee isolation
        if (set != null) {
            set.clear();
        }
    }

    @Test
    void pollLast_NaturalOrder_RemovesAndReturnsLastElement_IncludingNull() {
        // given
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        // null is allowed and should be the last element
        assertTrue(set.add(null));
        // when
        String last = set.pollLast();
        // then
        assertEquals(null, last, "pollLast should return the masked null element");
        assertEquals(3, set.size(), "size should decrease after pollLast");
        assertFalse(set.contains(null), "null should have been removed");
        // verify remaining order (iterator should now end with \"cherry\")
        Iterator<String> it = set.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void pollLast_WithLengthComparator_RemovesAndReturnsGreatestByComparator() {
        // given – a set ordered by string length (null is treated as length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // length 6
        assertTrue(set.add("banana"));
        // length 0
        assertTrue(set.add(null));
        // when
        String last = set.pollLast();
        // then
        assertEquals("banana", last, "pollLast should return the element with greatest length");
        assertEquals(3, set.size(), "size should be reduced by one");
        assertFalse(set.contains("banana"), "the returned element must be removed");
        // remaining order: null, kiwi, apple
        Iterator<String> it = set.iterator();
        assertEquals(null, it.next());
        assertEquals("kiwi", it.next());
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void pollLast_EmptySet_ReturnsNull() {
        // given – an empty set (already created in @BeforeEach)
        // when
        String result = set.pollLast();
        // then
        assertNull(result, "pollLast on an empty set should return null");
        assertTrue(set.isEmpty(), "set should remain empty");
    }

    @Test
    void pollLast_MultipleCalls_ConsistentlyUpdatesSet() {
        // given
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertTrue(set.add("c"));
        assertTrue(set.add(null));
        // first pollLast -> null
        assertEquals(null, set.pollLast());
        assertEquals(3, set.size());
        // second pollLast -> "c"
        assertEquals("c", set.pollLast());
        assertEquals(2, set.size());
        // third pollLast -> "b"
        assertEquals("b", set.pollLast());
        assertEquals(1, set.size());
        // final pollLast -> "a"
        assertEquals("a", set.pollLast());
        assertTrue(set.isEmpty());
    }

    /**
     * Helper that creates a {@code ConcurrentNavigableSetNullSafe} instance whose
     * {@code internalSet} field is replaced by the supplied mock.
     */
    @SuppressWarnings("unchecked")
    private <E> ConcurrentNavigableSetNullSafe<E> createSetWithMock(NavigableSet<Object> mockInternal) throws Exception {
        ConcurrentNavigableSetNullSafe<E> set = new ConcurrentNavigableSetNullSafe<>();
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(set, mockInternal);
        return set;
    }

    @Test
    void pollLast_ReturnsRegularElement() throws Exception {
        // Arrange – mock returns a normal (non‑null) element.
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mock = mock(NavigableSet.class);
        when(mock.pollLast()).thenReturn("alpha");
        ConcurrentNavigableSetNullSafe<String> set = createSetWithMock(mock);
        // Act
        String result = set.pollLast();
        // Assert
        assertEquals("alpha", result);
        verify(mock, times(1)).pollLast();
    }

    @Test
    void pollLast_ReturnsNullWhenSentinelReturned() throws Exception {
        // Arrange – mock returns the internal sentinel that represents a masked null.
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mock = mock(NavigableSet.class);
        when(mock.pollLast()).thenReturn(NULL_SENTINEL);
        ConcurrentNavigableSetNullSafe<String> set = createSetWithMock(mock);
        // Act
        String result = set.pollLast();
        // Assert
        assertNull(result, "The sentinel should be unmasked to a null reference");
        verify(mock, times(1)).pollLast();
    }

    @Test
    void pollLast_ReturnsNullWhenUnderlyingReturnsNull() throws Exception {
        // Arrange – underlying set is empty, so pollLast() returns null directly.
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mock = mock(NavigableSet.class);
        when(mock.pollLast()).thenReturn(null);
        ConcurrentNavigableSetNullSafe<String> set = createSetWithMock(mock);
        // Act
        String result = set.pollLast();
        // Assert
        assertNull(result);
        verify(mock, times(1)).pollLast();
    }

    @Test
    void pollLast_PropagatesUnsupportedOperationException() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mock = mock(NavigableSet.class);
        when(mock.pollLast()).thenThrow(new UnsupportedOperationException("not supported"));
        ConcurrentNavigableSetNullSafe<String> set = createSetWithMock(mock);
        assertThrows(UnsupportedOperationException.class, set::pollLast);
        verify(mock, times(1)).pollLast();
    }

    @Test
    void pollLast_PropagatesRuntimeException() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mock = mock(NavigableSet.class);
        when(mock.pollLast()).thenThrow(new RuntimeException("boom"));
        ConcurrentNavigableSetNullSafe<String> set = createSetWithMock(mock);
        RuntimeException ex = assertThrows(RuntimeException.class, set::pollLast);
        assertEquals("boom", ex.getMessage());
        verify(mock, times(1)).pollLast();
    }

    @Test
    void pollLast_PropagatesConcurrentModificationException() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mock = mock(NavigableSet.class);
        when(mock.pollLast()).thenThrow(new ConcurrentModificationException("concurrent change"));
        ConcurrentNavigableSetNullSafe<String> set = createSetWithMock(mock);
        assertThrows(ConcurrentModificationException.class, set::pollLast);
        verify(mock, times(1)).pollLast();
    }

    @Test
    void pollLast_TypeSafety_ClassCastException_StringSet() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mock = mock(NavigableSet.class);
        // Underlying set returns an Integer while the wrapper is typed as <String>.
        when(mock.pollLast()).thenReturn(123);
        ConcurrentNavigableSetNullSafe<String> set = createSetWithMock(mock);
        assertThrows(ClassCastException.class, set::pollLast);
        verify(mock, times(1)).pollLast();
    }

    @Test
    void pollLast_TypeSafety_ClassCastException_ObjectSetWrongSentinel() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mock = mock(NavigableSet.class);
        // Return a *different* object that equals the sentinel but is not the same instance.
        // The sentinel is unique; returning any other object forces unmaskNull to treat it as a regular value,
        // which will be cast to Object (the generic type) and succeed – to provoke a ClassCastException,
        // we use a type that cannot be cast to the declared generic (e.g., Integer when the set is <String>).
        // Here we use a raw Object set to illustrate the mismatch.
        when(mock.pollLast()).thenReturn(new Object());
        ConcurrentNavigableSetNullSafe<String> set = createSetWithMock(mock);
        // The cast inside unmaskNull will attempt to cast Object to String → ClassCastException.
        assertThrows(ClassCastException.class, set::pollLast);
        verify(mock, times(1)).pollLast();
    }

    @Test
    void pollLast_GenericInteger_ReturnsInteger() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mock = mock(NavigableSet.class);
        // Integer
        when(mock.pollLast()).thenReturn(42);
        ConcurrentNavigableSetNullSafe<Integer> set = createSetWithMock(mock);
        Integer result = set.pollLast();
        assertEquals(42, result);
        verify(mock, times(1)).pollLast();
    }

    @Test
    void pollLast_GenericInteger_ClassCastExceptionWhenLongReturned() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mock = mock(NavigableSet.class);
        // Long, not Integer
        when(mock.pollLast()).thenReturn(42L);
        ConcurrentNavigableSetNullSafe<Integer> set = createSetWithMock(mock);
        assertThrows(ClassCastException.class, set::pollLast);
        verify(mock, times(1)).pollLast();
    }

    @Test
    void pollLast_NullMaskSentinelMismatch_ResultsInClassCastException() throws Exception {
        // Simulate a situation where the underlying set returns an object that is *equal*
        // to the sentinel (by toString) but not the sentinel instance itself.
        // The wrapper will treat it as a normal element and try to cast it, causing a ClassCastException.
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mock = mock(NavigableSet.class);
        Object fakeSentinel = new Object() {

            @Override
            public String toString() {
                // mimic the sentinel's string representation
                return NULL_SENTINEL.toString();
            }
        };
        when(mock.pollLast()).thenReturn(fakeSentinel);
        ConcurrentNavigableSetNullSafe<String> set = createSetWithMock(mock);
        assertThrows(ClassCastException.class, set::pollLast);
        verify(mock, times(1)).pollLast();
    }
}
