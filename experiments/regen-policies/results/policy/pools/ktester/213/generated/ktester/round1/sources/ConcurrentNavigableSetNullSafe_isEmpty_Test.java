package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_isEmpty_Test {

    /**
     * Instance used by the majority of tests – created fresh before each test.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor gives a natural‑ordering, null‑safe set.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set does not retain state between tests.
        if (set != null) {
            set.clear();
        }
    }

    /**
     * Helper that uses reflection to replace the private {@code internalSet}
     * field with a supplied value (mock or {@code null}).
     */
    private void injectInternalSet(ConcurrentNavigableSetNullSafe<?> target, NavigableSet<?> newInternalSet) throws Exception {
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        // Remove final modifier via reflection (works on most JVMs for test purposes)
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        field.set(target, newInternalSet);
    }

    @Test
    void givenDefaultConstructor_whenNew_thenIsEmpty() {
        // given – set is created in @BeforeEach
        // when – we query emptiness
        boolean empty = set.isEmpty();
        // then
        assertTrue(empty, "A newly created set should be empty");
    }

    @Test
    void givenDefaultConstructor_whenAddElement_thenNotEmpty() {
        // given
        set.add("apple");
        // when
        boolean empty = set.isEmpty();
        // then
        assertFalse(empty, "Set should not be empty after adding a non‑null element");
    }

    @Test
    void givenDefaultConstructor_whenAddNull_thenNotEmpty() {
        // given
        set.add(null);
        // when
        boolean empty = set.isEmpty();
        // then
        assertFalse(empty, "Set should not be empty after adding a null element");
    }

    @Test
    void givenSetWithElements_whenClear_thenIsEmpty() {
        // given
        set.add("apple");
        set.add(null);
        assertFalse(set.isEmpty(), "Pre‑condition: set must be non‑empty before clear");
        // when
        set.clear();
        // then
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
    }

    @Test
    void givenComparatorConstructor_whenNew_thenIsEmpty() {
        // given
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableSet<String> compSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        // when
        boolean empty = compSet.isEmpty();
        // then
        assertTrue(empty, "Set created with a comparator should start empty");
    }

    @Test
    void givenComparatorConstructor_whenAddElements_thenNotEmpty() {
        // given
        Comparator<String> length = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> compSet = new ConcurrentNavigableSetNullSafe<>(length);
        // when
        compSet.add("kiwi");
        compSet.add(null);
        // then
        assertFalse(compSet.isEmpty(), "Set should not be empty after adding elements");
    }

    @Test
    void givenCollectionConstructor_whenCollectionContainsElements_thenNotEmpty() {
        // given
        Collection<String> coll = Arrays.asList("apple", null, "banana");
        NavigableSet<String> collSet = new ConcurrentNavigableSetNullSafe<>(coll);
        // when
        boolean empty = collSet.isEmpty();
        // then
        assertFalse(empty, "Set built from a non‑empty collection should not be empty");
    }

    @Test
    void givenCollectionConstructor_whenCollectionIsEmpty_thenIsEmpty() {
        // given
        Collection<String> emptyColl = Collections.emptyList();
        NavigableSet<String> collSet = new ConcurrentNavigableSetNullSafe<>(emptyColl);
        // when
        boolean empty = collSet.isEmpty();
        // then
        assertTrue(empty, "Set built from an empty collection should be empty");
    }

    @Test
    void givenInternalSetIsNull_whenIsEmpty_thenThrowNPE() throws Exception {
        // Create a fresh instance and then null‑out its internalSet.
        ConcurrentNavigableSetNullSafe<String> unsafe = new ConcurrentNavigableSetNullSafe<>();
        injectInternalSet(unsafe, null);
        // The wrapper should simply propagate the NPE from internalSet.isEmpty()
        assertThrows(NullPointerException.class, unsafe::isEmpty, "Calling isEmpty() when internalSet is null should throw NullPointerException");
    }

    @Test
    void givenMockedInternalSet_returnsTrue_thenIsEmptyTrue() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockSet = mock(NavigableSet.class);
        when(mockSet.isEmpty()).thenReturn(true);
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        injectInternalSet(wrapper, mockSet);
        assertTrue(wrapper.isEmpty(), "Wrapper should return true when mocked internalSet.isEmpty() returns true");
        verify(mockSet, times(1)).isEmpty();
    }

    @Test
    void givenMockedInternalSet_returnsFalse_thenIsEmptyFalse() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockSet = mock(NavigableSet.class);
        when(mockSet.isEmpty()).thenReturn(false);
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        injectInternalSet(wrapper, mockSet);
        assertFalse(wrapper.isEmpty(), "Wrapper should return false when mocked internalSet.isEmpty() returns false");
        verify(mockSet, times(1)).isEmpty();
    }

    @Test
    void givenMockedInternalSet_throwsIllegalStateException_thenPropagated() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockSet = mock(NavigableSet.class);
        when(mockSet.isEmpty()).thenThrow(new IllegalStateException("illegal state"));
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        injectInternalSet(wrapper, mockSet);
        IllegalStateException ex = assertThrows(IllegalStateException.class, wrapper::isEmpty, "IllegalStateException from internalSet should be propagated unchanged");
        assertEquals("illegal state", ex.getMessage());
    }

    @Test
    void afterAddElement_isEmptyShouldReturnFalse() {
        // Adding via the public API must affect the underlying internalSet.
        set.add("orange");
        assertFalse(set.isEmpty(), "After adding an element, isEmpty() must return false");
    }

    @Test
    void afterAddAndRemoveSameElement_isEmptyShouldReturnTrue() {
        set.add("pear");
        assertFalse(set.isEmpty(), "Set should be non‑empty after addition");
        set.remove("pear");
        assertTrue(set.isEmpty(), "Set should be empty after removing the previously added element");
    }

    @Test
    void givenMockedInternalSet_throwsUnsupportedOperationException_thenPropagated() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockSet = mock(NavigableSet.class);
        when(mockSet.isEmpty()).thenThrow(new UnsupportedOperationException("read‑only"));
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        injectInternalSet(wrapper, mockSet);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, wrapper::isEmpty, "UnsupportedOperationException should be propagated");
        assertEquals("read‑only", ex.getMessage());
    }

    @Test
    void givenMockedInternalSet_throwsIllegalArgumentException_thenPropagated() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockSet = mock(NavigableSet.class);
        when(mockSet.isEmpty()).thenThrow(new IllegalArgumentException("bad argument"));
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        injectInternalSet(wrapper, mockSet);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, wrapper::isEmpty, "IllegalArgumentException should be propagated");
        assertEquals("bad argument", ex.getMessage());
    }

    @Test
    void givenMockedInternalSet_throwsConcurrentModificationException_thenPropagated() throws Exception {
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockSet = mock(NavigableSet.class);
        when(mockSet.isEmpty()).thenThrow(new ConcurrentModificationException("concurrent mod"));
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        injectInternalSet(wrapper, mockSet);
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, wrapper::isEmpty, "ConcurrentModificationException should be propagated");
        assertEquals("concurrent mod", ex.getMessage());
    }
}
