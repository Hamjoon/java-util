package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_subSet_2_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural‑order) constructor for each test.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared so that a failing test does not affect the next one.
        if (set != null) {
            set.clear();
        }
    }

    private static Stream<Arguments> validRangeProvider() {
        return Stream.of(// String example
        Arguments.of(Arrays.asList("a", "b", "c", "d", "e", "f"), "b", "e", Arrays.asList("b", "c", "d")), // Integer example
        Arguments.of(Arrays.asList(0, 1, 2, 3, 4, 5), 1, 4, Arrays.asList(1, 2, 3)));
    }

    private static Stream<Arguments> invalidRangeProvider() {
        return Stream.of(Arguments.of("e", "b"), Arguments.of(5, 2));
    }

    @Test
    void subSet_excludesUpperBound() {
        // given
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add("date");
        // when
        // "date" is exclusive
        NavigableSet<String> sub = set.subSet("banana", "date");
        // then
        assertEquals(2, sub.size(), "Sub‑set should contain exactly two elements");
        assertTrue(sub.contains("banana"));
        assertTrue(sub.contains("cherry"));
        assertFalse(sub.contains("date"));
        assertFalse(sub.contains("apple"));
        // verify ordering (natural order)
        Iterator<String> it = sub.iterator();
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void subSet_withNullUpperBound_excludesNull() {
        // given
        // sentinel for null values
        set.add(null);
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // when: upper bound is null – null is *exclusive* because the simple
        // subSet method always uses toInclusive = false.
        NavigableSet<String> sub = set.subSet("apple", null);
        // then
        // Expected elements: "apple", "banana", "cherry" (null excluded)
        assertEquals(3, sub.size());
        assertTrue(sub.contains("apple"));
        assertTrue(sub.contains("banana"));
        assertTrue(sub.contains("cherry"));
        assertFalse(sub.contains(null));
        Iterator<String> it = sub.iterator();
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void subSet_sameFromAndTo_returnsEmptySet() {
        // given
        set.add("alpha");
        set.add("beta");
        // when
        // exclusive upper bound
        NavigableSet<String> sub = set.subSet("beta", "beta");
        // then
        assertTrue(sub.isEmpty(), "A subSet where from == to (exclusive) must be empty");
    }

    @Test
    void subSet_fromGreaterThanTo_throwsIllegalArgumentException() {
        // given
        set.add("a");
        set.add("b");
        set.add("c");
        // when / then
        assertThrows(IllegalArgumentException.class, () -> set.subSet("c", "a"), "Calling subSet with fromElement > toElement should throw IAE");
    }

    @Test
    void subSet_includingNullOnly() {
        // given
        set.add(null);
        set.add("apple");
        set.add("banana");
        // when: both bounds are null – the simple subSet method makes the lower
        // bound inclusive and the upper bound exclusive, so the result should be empty.
        NavigableSet<String> sub = set.subSet(null, null);
        // then
        assertTrue(sub.isEmpty(), "subSet(null, null) should be empty because upper bound is exclusive");
    }

    @ParameterizedTest(name = "validRangeProvider[{index}]")
    @MethodSource("validRangeProvider")
    void subSet_validRange(Object backingElements, Object from, Object to, List<?> expected) {
        // Initialise a set with the supplied elements (type inference works because
        // the elements are homogeneous).
        NavigableSet<Object> localSet = new ConcurrentNavigableSetNullSafe<>();
        backingElements.forEach(localSet::add);
        @SuppressWarnings("unchecked")
        NavigableSet<Object> sub = (NavigableSet<Object>) localSet.subSet(from, to);
        assertEquals(expected.size(), sub.size(), "Subset size does not match expected size");
        assertIterableEquals(expected, sub, "Subset contents differ from expected");
    }

    @Test
    void subSet_emptyRange_whenFromEqualsTo() {
        set.addAll(Arrays.asList("a", "b", "c"));
        NavigableSet<String> sub = set.subSet("c", "c");
        assertTrue(sub.isEmpty(), "Subset should be empty when lower == upper (exclusive)");
    }

    @ParameterizedTest(name = "invalidRangeProvider[{index}]")
    @MethodSource("invalidRangeProvider")
    void subSet_invalidRange_throwsIllegalArgumentException(Object from, Object to) {
        // Populate with a few elements of the appropriate type
        NavigableSet<Object> localSet = new ConcurrentNavigableSetNullSafe<>();
        if (from instanceof String) {
            localSet.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        } else {
            localSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        }
        assertThrows(IllegalArgumentException.class, () -> localSet.subSet(from, to), "Expected IllegalArgumentException for from > to");
    }

    @Test
    void subSet_nullFromElement_throwsNullPointerException() {
        set.addAll(Arrays.asList("a", "b", "c", "d"));
        assertThrows(NullPointerException.class, () -> set.subSet(null, "d"), "Null lower bound should raise NPE when no comparator permits nulls");
    }

    @Test
    void subSet_nullToElement_throwsNullPointerException() {
        set.addAll(Arrays.asList("a", "b", "c"));
        assertThrows(NullPointerException.class, () -> set.subSet("a", null), "Null upper bound should raise NPE when no comparator permits nulls");
    }

    @Test
    void subSet_fullRange_excludesLastElement() {
        set.addAll(Arrays.asList("a", "b", "c", "d", "e", "f"));
        // "f" exclusive
        NavigableSet<String> sub = set.subSet("a", "f");
        assertIterableEquals(Arrays.asList("a", "b", "c", "d", "e"), sub);
    }

    @Test
    void subSet_reflectsAdditionsToBackingSet() {
        set.addAll(Arrays.asList("a", "b", "d", "e"));
        // view should contain b,d
        NavigableSet<String> sub = set.subSet("b", "e");
        // Add an element that falls inside the view range via the original set
        set.add("c2");
        // The view must now contain the newly added element
        assertTrue(sub.contains("c2"));
        assertIterableEquals(Arrays.asList("b", "c2", "d"), sub);
    }

    @Test
    void subSet_reflectsRemovalsThroughView() {
        set.addAll(Arrays.asList("a", "b", "c", "d"));
        // view contains b,c,d
        NavigableSet<String> sub = set.subSet("b", "e");
        // Remove "c" via the subset view
        assertTrue(sub.remove("c"));
        // The backing set must no longer contain "c"
        assertFalse(set.contains("c"));
        // Verify the view also reflects the removal
        assertIterableEquals(Arrays.asList("b", "d"), sub);
    }

    @Test
    void subSet_lowerBoundBelowSmallestElement() {
        set.addAll(Arrays.asList("b", "c", "d"));
        // "a" < smallest element
        NavigableSet<String> sub = set.subSet("a", "c");
        assertIterableEquals(Collections.singletonList("b"), sub);
    }

    @Test
    void subSet_upperBoundAboveLargestElement() {
        set.addAll(Arrays.asList("b", "c", "d"));
        // "z" > any element
        NavigableSet<String> sub = set.subSet("c", "z");
        assertIterableEquals(Arrays.asList("c", "d"), sub);
    }

    @Test
    void subSet_onEmptyBackingSet_returnsEmptyView() {
        // set is already empty
        NavigableSet<String> sub = set.subSet("a", "b");
        assertTrue(sub.isEmpty(), "Subset of empty set must be empty");
    }

    @Test
    void subSet_onSingleElementSet() {
        set.add("m");
        NavigableSet<String> sub = set.subSet("m", "n");
        assertIterableEquals(Collections.singletonList("m"), sub);
    }

    @Test
    void subSet_nullBoundsWithNullFriendlyComparator() {
        // Comparator that treats null as the smallest element
        NavigableSet<String> nullSafeSet = new ConcurrentNavigableSetNullSafe<>(Comparator.nullsFirst(String::compareTo));
        nullSafeSet.addAll(Arrays.asList(null, "a", "b", "c"));
        // upper bound exclusive
        NavigableSet<String> sub = nullSafeSet.subSet(null, "c");
        assertIterableEquals(Arrays.asList(null, "a", "b"), sub);
    }

    @Test
    void subSet_addWithinRangeThroughView() {
        set.addAll(Arrays.asList("a", "b", "d"));
        // view currently ["b","d"]
        NavigableSet<String> sub = set.subSet("b", "e");
        assertTrue(sub.add("c"), "Adding element inside the range via view should succeed");
        // Both the original set and the view must now contain the new element
        assertTrue(set.contains("c"));
        assertIterableEquals(Arrays.asList("b", "c", "d"), sub);
    }

    @Test
    void subSet_addOutsideRangeThroughView_throwsIllegalArgumentException() {
        set.addAll(Arrays.asList("a", "b", "c", "d"));
        // view ["b","c"]
        NavigableSet<String> sub = set.subSet("b", "d");
        assertThrows(IllegalArgumentException.class, () -> sub.add("e"), "Adding element beyond exclusive upper bound must throw IAE");
    }

    @Test
    void subSet_incompatibleFromElement_throwsClassCastException() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3));
        // The set holds Integer, but we pass a String as fromElement
        assertThrows(ClassCastException.class, () -> intSet.subSet("string", 5));
    }

    @Test
    void subSet_incompatibleToElement_throwsClassCastException() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3));
        // The set holds Integer, but we pass a String as toElement
        assertThrows(ClassCastException.class, () -> intSet.subSet(1, "string"));
    }
}
