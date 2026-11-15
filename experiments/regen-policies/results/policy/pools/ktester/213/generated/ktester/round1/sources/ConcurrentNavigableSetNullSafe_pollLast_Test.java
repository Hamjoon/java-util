package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Comparator;
import java.util.NavigableSet;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.ConcurrentSkipListSet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_pollLast_Test {

    /**
     * The set under test – created fresh before each test method.
     */
    private NavigableSet<String> set;

    /**
     * Instances used for the majority of tests – created fresh for each test method.
     */
    private ConcurrentNavigableSetNullSafe<Integer> intSet;

    private ConcurrentNavigableSetNullSafe<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural‑order) constructor for most tests.
        set = new ConcurrentNavigableSetNullSafe<>();
        intSet = new ConcurrentNavigableSetNullSafe<>();
        stringSet = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared so that a stray reference does not affect
        // the next test (good practice even though a new instance is created).
        if (set != null) {
            set.clear();
        }
        if (intSet != null)
            intSet.clear();
        if (stringSet != null)
            stringSet.clear();
    }

    /**
     * Utility that replaces the private {@code internalSet} field of the wrapper
     * with a supplied (mocked or custom) {@link NavigableSet}.
     */
    private void injectInternalSet(Object wrapper, NavigableSet<?> replacement) throws Exception {
        Field field = wrapper.getClass().getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(wrapper, replacement);
    }

    @Test
    void pollLastOnEmptySetReturnsNull() {
        assertTrue(intSet.isEmpty(), "Set should start empty");
        assertNull(intSet.pollLast(), "pollLast on an empty set must return null");
    }

    @Test
    void pollLastRemovesAndReturnsLastElement_NaturalOrder_WithNull() {
        // natural ordering: null is masked with a sentinel that sorts after all
        // non‑null elements, therefore it is the last element.
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        assertEquals(3, set.size());
        // First pollLast should return the masked null (unmasked back to null)
        String last = set.pollLast();
        assertNull(last, "The last element (null) should be returned and unmasked to null");
        // Verify removal
        assertEquals(2, set.size());
        assertFalse(set.contains(null));
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
    }

    @Test
    void pollLastRespectsCustomComparator() {
        // Comparator based on string length – null is treated as length 0.
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 1
        assertTrue(set.add("a"));
        // length 2
        assertTrue(set.add("bb"));
        // length 3
        assertTrue(set.add("ccc"));
        // length 0 (smallest)
        assertTrue(set.add(null));
        assertEquals(4, set.size());
        // The longest element ("ccc") should be returned by pollLast.
        String last = set.pollLast();
        assertEquals("ccc", last, "Longest element according to comparator should be returned");
        // Verify that the set no longer contains the removed element.
        assertEquals(3, set.size());
        assertFalse(set.contains("ccc"));
        // Null should still be present because it is the smallest element.
        assertTrue(set.contains(null));
    }

    @Test
    void pollLastMultipleCallsYieldElementsInDescendingOrder() {
        assertTrue(set.add("alpha"));
        assertTrue(set.add("beta"));
        assertTrue(set.add("gamma"));
        // sentinel makes null the greatest element
        assertTrue(set.add(null));
        // First call – null (masked) should be returned.
        assertNull(set.pollLast(), "First pollLast should return null");
        // Second call – "gamma" is the natural order last among remaining strings.
        assertEquals("gamma", set.pollLast(), "Second pollLast should return 'gamma'");
        // Third call – "beta"
        assertEquals("beta", set.pollLast(), "Third pollLast should return 'beta'");
        // Fourth call – "alpha"
        assertEquals("alpha", set.pollLast(), "Fourth pollLast should return 'alpha'");
        // Set is now empty.
        assertTrue(set.isEmpty(), "Set should be empty after polling all elements");
        assertNull(set.pollLast(), "pollLast on an empty set must still return null");
    }

    @Test
    void pollLastReturnsGreatestElementWhenSetHasIntegers() {
        assertTrue(intSet.add(1));
        assertTrue(intSet.add(2));
        assertTrue(intSet.add(3));
        Integer last = intSet.pollLast();
        assertEquals(3, last, "The greatest integer should be returned");
        assertEquals(2, intSet.size());
        assertFalse(intSet.contains(3));
    }

    @Test
    void pollLastThrowsClassCastExceptionWhenInternalSetContainsWrongType(@Mock NavigableSet<Object> mockSet) throws Exception {
        // The mock pretends the underlying set contains a String while the wrapper is typed Integer
        when(mockSet.pollLast()).thenReturn("not-an-integer");
        ConcurrentNavigableSetNullSafe<Integer> wrapper = new ConcurrentNavigableSetNullSafe<>();
        injectInternalSet(wrapper, mockSet);
        assertThrows(ClassCastException.class, wrapper::pollLast, "Unmasking a value of an incompatible type must throw ClassCastException");
    }

    @Test
    void pollLastPropagatesComparatorRuntimeException(@Mock NavigableSet<Object> mockSet) throws Exception {
        // Simulate a comparator that throws an exception while locating the last element
        when(mockSet.pollLast()).thenThrow(new IllegalArgumentException("bad comparator"));
        ConcurrentNavigableSetNullSafe<Integer> wrapper = new ConcurrentNavigableSetNullSafe<>();
        injectInternalSet(wrapper, mockSet);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, wrapper::pollLast);
        assertEquals("bad comparator", ex.getMessage());
    }

    @Test
    void pollLastThrowsUnsupportedOperationWhenInternalSetIsUnmodifiable() throws Exception {
        // Build a mutable set, fill it, then wrap it with an unmodifiable view
        NavigableSet<Object> mutable = new ConcurrentSkipListSet<>();
        mutable.add(10);
        mutable.add(20);
        NavigableSet<Object> unmodifiable = Collections.unmodifiableNavigableSet(mutable);
        ConcurrentNavigableSetNullSafe<Integer> wrapper = new ConcurrentNavigableSetNullSafe<>();
        injectInternalSet(wrapper, unmodifiable);
        // pollLast attempts a removal, which the unmodifiable view disallows
        assertThrows(UnsupportedOperationException.class, wrapper::pollLast);
    }

    @Test
    void pollLastThrowsNullPointerExceptionWhenInternalSetIsNull() throws Exception {
        ConcurrentNavigableSetNullSafe<Integer> wrapper = new ConcurrentNavigableSetNullSafe<>();
        // Force the private delegate to null
        Field f = wrapper.getClass().getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(wrapper, null);
        assertThrows(NullPointerException.class, wrapper::pollLast, "Calling pollLast with a null internal delegate should NPE");
    }
}
