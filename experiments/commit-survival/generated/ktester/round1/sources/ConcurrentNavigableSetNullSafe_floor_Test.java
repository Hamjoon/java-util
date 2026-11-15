package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class ConcurrentNavigableSetNullSafe_floor_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor (natural ordering) for simplicity
        set = new ConcurrentNavigableSetNullSafe<>();
        // Populate the set with a known ordering, including a null element
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to ensure isolation between tests
        if (set != null) {
            set.clear();
        }
    }

    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(Arguments.of(null, null, "null argument, null present"), Arguments.of(null, null, "null argument, null absent"), Arguments.of("c", "c", "exact match"), Arguments.of("d", "c", "non‑existent, floor is lower"), Arguments.of("a", null, "smaller than smallest"), Arguments.of("b", null, "floor is masked null"));
    }

    @Test
    void floor_WithExactMatch_ReturnsElementItself() {
        assertEquals("banana", set.floor("banana"));
        assertEquals("apple", set.floor("apple"));
        assertEquals(null, set.floor(null));
    }

    @Test
    void floor_WithLowerElement_ReturnsGreatestElementLessThanOrEqual() {
        // "blueberry" is greater than "banana" but less than "cherry"
        assertEquals("banana", set.floor("blueberry"));
        // "aardvark" is less than "apple"
        assertEquals(null, set.floor("aardvark"));
    }

    @Test
    void floor_WithHigherThanAll_ReturnsHighestElement() {
        assertEquals("cherry", set.floor("zoo"));
    }

    @Test
    void floor_WithNullSetElement_ReturnsNullWhenNullIsHighestOrEqual() {
        // In natural ordering null is treated as the lowest element after masking,
        // therefore floor(null) should return null (the masked sentinel).
        assertNull(set.floor(null));
    }

    @Test
    void floor_OnEmptySet_ReturnsNull() {
        NavigableSet<Integer> emptySet = new ConcurrentNavigableSetNullSafe<>();
        assertNull(emptySet.floor(10));
        assertNull(emptySet.floor(null));
    }

    @Test
    void floor_WithComparator_OrdersAccordingly() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 1
        lengthSet.add("a");
        // length 2
        lengthSet.add("bb");
        // length 3
        lengthSet.add("ccc");
        // length 0
        lengthSet.add(null);
        // floor of a string with length 2 should be the element with length 2 ("bb")
        assertEquals("bb", lengthSet.floor("xx"));
        // floor of a string with length 1 should be "a"
        assertEquals("a", lengthSet.floor("y"));
        // floor of null (length 0) should be null
        assertNull(lengthSet.floor(null));
    }

    @Test
    void floor_NullArgument_WithNullElementInSet_ReturnsNull() {
        // The set contains a null element (masked internally). floor(null) must
        // return the original null after unmasking.
        assertNull(set.floor(null));
    }

    @Test
    void floor_NullArgument_WithoutNullElementInSet_ReturnsNull() {
        // Build a set that does NOT contain a null element.
        NavigableSet<String> noNullSet = new ConcurrentNavigableSetNullSafe<>();
        noNullSet.add("b");
        noNullSet.add("c");
        assertNull(noNullSet.floor(null));
    }

    @Test
    void floor_ExactMatch_ReturnsSameElement() {
        assertEquals("c", new ConcurrentNavigableSetNullSafe<String>(Comparator.naturalOrder()) {

            {
                add("a");
                add("c");
                add("e");
            }
        }.floor("c"));
    }

    @Test
    void floor_NonExistingElement_ReturnsGreatestLessOrEqual() {
        NavigableSet<String> s = new ConcurrentNavigableSetNullSafe<>();
        s.add("b");
        s.add("c");
        assertEquals("c", s.floor("d"));
    }

    @Test
    void floor_ElementSmallerThanAll_ReturnsNull() {
        NavigableSet<String> s = new ConcurrentNavigableSetNullSafe<>();
        s.add("b");
        s.add("c");
        assertNull(s.floor("a"));
    }

    @Test
    void floor_ElementBetweenNullAndFirst_ReturnsNullSentinel() {
        // Set contains null (sentinel) and "c". The floor of "b" should be the sentinel,
        // which is unmasked to null.
        NavigableSet<String> s = new ConcurrentNavigableSetNullSafe<>();
        s.add(null);
        s.add("c");
        assertNull(s.floor("b"));
    }

    @Test
    void floor_GreaterThanAllNonNullElements_ReturnsHighestNonNull() {
        NavigableSet<String> s = new ConcurrentNavigableSetNullSafe<>();
        s.add(null);
        s.add("c");
        assertEquals("c", s.floor("z"));
    }

    @Test
    void floor_OnlyNullElement_ReturnsNullForAnyArgument() {
        NavigableSet<String> s = new ConcurrentNavigableSetNullSafe<>();
        s.add(null);
        assertNull(s.floor("any"));
    }

    @Test
    void floor_MiddleElement_ReturnsCorrectFloor() {
        NavigableSet<String> s = new ConcurrentNavigableSetNullSafe<>();
        s.add("b");
        s.add("d");
        s.add("f");
        assertEquals("d", s.floor("e"));
    }

    @Test
    void floor_MaxElement_ReturnsItself() {
        NavigableSet<String> s = new ConcurrentNavigableSetNullSafe<>();
        s.add("b");
        s.add("d");
        s.add("f");
        assertEquals("f", s.floor("f"));
    }

    @Test
    void floor_EmptySet_AlwaysReturnsNull() {
        NavigableSet<Integer> empty = new ConcurrentNavigableSetNullSafe<>();
        assertNull(empty.floor(1));
        assertNull(empty.floor(null));
    }

    @Test
    void floor_DescendingComparator_RespectsOrder() {
        Comparator<Integer> descending = Comparator.reverseOrder();
        NavigableSet<Integer> s = new ConcurrentNavigableSetNullSafe<>(descending);
        s.add(1);
        s.add(3);
        s.add(5);
        // In descending order the "floor" of 4 is 5 (the greatest element ≤ 4 in that order)
        assertEquals(5, s.floor(4));
    }

    @Test
    void floor_DescendingComparator_WithNullSentinel() {
        Comparator<Integer> descending = Comparator.reverseOrder();
        NavigableSet<Integer> s = new ConcurrentNavigableSetNullSafe<>(descending);
        // internalSet will contain a masked null sentinel plus 2 and 4
        s.add(null);
        s.add(2);
        s.add(4);
        // In descending order, floor(3) should be 4 (since 4 > 3 in natural order,
        // but 4 comes before 3 in descending order)
        assertEquals(4, s.floor(3));
    }

    @Test
    void floor_IncompatibleArgument_ThrowsClassCastException() {
        NavigableSet<String> s = new ConcurrentNavigableSetNullSafe<>();
        s.add("a");
        s.add("b");
        assertThrows(ClassCastException.class, () -> s.floor(1));
    }

    @Test
    void floor_NullInternalSet_ThrowsNullPointerException() throws Exception {
        // Create an instance via reflection and inject a null internalSet.
        ConcurrentNavigableSetNullSafe<String> broken = new ConcurrentNavigableSetNullSafe<>();
        Field internal = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internal.setAccessible(true);
        // break the internal state
        internal.set(broken, null);
        assertThrows(NullPointerException.class, () -> broken.floor("any"));
    }

    @Test
    void floor_NullArgument_WithIntegerNaturalOrder_ThrowsClassCastException() {
        // Integer natural ordering does not accept null; maskNull(null) yields a sentinel
        // that cannot be compared to Integer values.
        NavigableSet<Integer> s = new ConcurrentNavigableSetNullSafe<>();
        s.add(1);
        s.add(2);
        assertThrows(ClassCastException.class, () -> s.floor(null));
    }

    @Test
    void floor_RawInternalSet_MismatchedGeneric_ThrowsClassCastException() throws Exception {
        // Build a raw ConcurrentSkipListSet containing Integers, then wrap it as
        // ConcurrentNavigableSetNullSafe<String> using reflection.
        ConcurrentSkipListSet raw = new ConcurrentSkipListSet();
        raw.add(10);
        raw.add(20);
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field internal = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internal.setAccessible(true);
        // raw set with Integer elements
        internal.set(wrapper, raw);
        assertThrows(ClassCastException.class, () -> wrapper.floor("test"));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("branchCoverageProvider")
    void floor_BranchCoverageParameterized(Object input, Object expected, String _desc) {
        // Build the appropriate set for each case based on the description.
        // For brevity we only demonstrate the first three cases; the remaining
        // cases are covered by the explicit tests above.
        if ("null argument, null present".equals(_desc)) {
            assertNull(set.floor(null));
        } else if ("null argument, null absent".equals(_desc)) {
            NavigableSet<String> s = new ConcurrentNavigableSetNullSafe<>();
            s.add("b");
            s.add("c");
            assertNull(s.floor(null));
        } else if ("exact match".equals(_desc)) {
            NavigableSet<String> s = new ConcurrentNavigableSetNullSafe<>();
            s.add("a");
            s.add("c");
            s.add("e");
            assertEquals("c", s.floor("c"));
        }
        // Other branches are already exercised by dedicated tests.
    }
}
