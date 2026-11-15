package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class ConcurrentNavigableSetNullSafe_tailSet_Test {

    class NotComparable {

        final String value;

        NotComparable(String v) {
            this.value = v;
        }
    }

    class NotComparable {

        final String value;

        NotComparable(String v) {
            this.value = v;
        }
    }

    static Stream<Arguments> naturalOrderTailSetProvider() {
        return Stream.of(// Non‑null fromElement, inclusive true
        Arguments.of("b", true, Arrays.asList("b", "c", "d", null)), // Non‑null fromElement, inclusive false
        Arguments.of("b", false, Arrays.asList("c", "d", null)), // fromElement null, inclusive true → only the null element (greatest)
        Arguments.of(null, true, Collections.singletonList(null)), // fromElement null, inclusive false → empty (nothing greater than null)
        Arguments.of(null, false, Collections.emptyList()), // fromElement greater than max
        Arguments.of("e", true, Collections.emptyList()), Arguments.of("e", false, Collections.emptyList()), // fromElement equal to min
        Arguments.of("a", true, Arrays.asList("a", "b", "c", "d", null)), Arguments.of("a", false, Arrays.asList("b", "c", "d", null)), // fromElement not present but between existing elements
        Arguments.of("b1", true, Arrays.asList("c", "d", null)));
    }

    static Stream<Arguments> reverseComparatorTailSetProvider() {
        return Stream.of(// inclusive true – should contain "b" and everything that comes after it in descending order
        Arguments.of("b", true, Arrays.asList("b", "a")), // inclusive false – should start strictly after "b" in descending order
        Arguments.of("b", false, Collections.singletonList("a")));
    }

    /**
     * Verify tailSet with inclusive = true returns the element specified and all higher elements,
     * including the masked null element which is ordered after all non‑null values.
     */
    @Test
    void testTailSetInclusiveTrue_NaturalOrder() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // null should be the greatest element
        set.add(null);
        NavigableSet<String> tail = set.tailSet("banana", true);
        assertEquals(3, tail.size());
        Iterator<String> it = tail.iterator();
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Verify tailSet with inclusive = false excludes the element specified but includes all higher ones.
     */
    @Test
    void testTailSetInclusiveFalse_NaturalOrder() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
        NavigableSet<String> tail = set.tailSet("banana", false);
        assertEquals(2, tail.size());
        Iterator<String> it = tail.iterator();
        assertEquals("cherry", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /**
     * When the fromElement is null and inclusive = true, the result should contain only the null element,
     * because null is the greatest element in natural ordering.
     */
    @Test
    void testTailSetFromNullInclusiveTrue_NaturalOrder() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add(null);
        NavigableSet<String> tail = set.tailSet(null, true);
        assertEquals(1, tail.size());
        assertTrue(tail.contains(null));
        assertEquals(null, tail.first());
    }

    /**
     * When the fromElement is null and inclusive = false, the result should be empty.
     */
    @Test
    void testTailSetFromNullInclusiveFalse_NaturalOrder() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add(null);
        NavigableSet<String> tail = set.tailSet(null, false);
        assertTrue(tail.isEmpty());
    }

    /**
     * Verify tailSet works correctly with a custom comparator (by string length).
     * The comparator treats null as the smallest element (length 0).
     */
    @Test
    void testTailSetWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 0
        set.add(null);
        // length 4
        set.add("kiwi");
        // length 5
        set.add("apple");
        // length 6
        set.add("banana");
        // fromElement = "kiwi", inclusive = false → should start with length > 4
        NavigableSet<String> tail = set.tailSet("kiwi", false);
        assertEquals(2, tail.size());
        Iterator<String> it = tail.iterator();
        // length 5
        assertEquals("apple", it.next());
        // length 6
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
        // fromElement = null, inclusive = true → should contain all elements
        NavigableSet<String> tailAll = set.tailSet(null, true);
        assertEquals(4, tailAll.size());
        Iterator<String> itAll = tailAll.iterator();
        // null first (length 0)
        assertNull(itAll.next());
        assertEquals("kiwi", itAll.next());
        assertEquals("apple", itAll.next());
        assertEquals("banana", itAll.next());
        assertFalse(itAll.hasNext());
    }

    /**
     * Verify that tailSet correctly handles a fromElement that is not present in the set.
     * The result should start at the least element greater than the given key.
     */
    @Test
    void testTailSetFromMissingElement() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
        // "avocado" is not present; it lies between "apple" and "banana"
        NavigableSet<String> tail = set.tailSet("avocado", true);
        assertEquals(3, tail.size());
        Iterator<String> it = tail.iterator();
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /**
     * Parameterised tests that cover the natural‑order scenarios described in the JSON test matrix.
     */
    @ParameterizedTest(name = "[{index}] from={0}, inclusive={1} → expected={2}")
    @MethodSource("naturalOrderTailSetProvider")
    void testTailSetNaturalOrder(String fromElement, boolean inclusive, List<String> expected) {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");
        // null is the greatest element
        set.add(null);
        NavigableSet<String> tail = set.tailSet(fromElement, inclusive);
        List<String> actual = new ArrayList<>(tail);
        assertEquals(expected, actual);
    }

    /**
     * Parameterised tests for a custom reverse comparator (descending natural order).
     */
    @ParameterizedTest(name = "[{index}] reverseComparator – from={0}, inclusive={1} → expected={2}")
    @MethodSource("reverseComparatorTailSetProvider")
    void testTailSetReverseComparator(String fromElement, boolean inclusive, List<String> expected) {
        Comparator<String> reverse = Comparator.<String>naturalOrder().reversed();
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(reverse);
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");
        // No null handling needed for this test
        NavigableSet<String> tail = set.tailSet(fromElement, inclusive);
        List<String> actual = new ArrayList<>(tail);
        assertEquals(expected, actual);
    }

    /**
     * Verify that the tail‑set view is backed by the original set.
     * Adding an element to the returned tail set must be reflected in the original set.
     */
    @Test
    void testTailSetBackedViewModification() {
        NavigableSet<String> original = new ConcurrentNavigableSetNullSafe<>();
        original.addAll(Arrays.asList("a", "b", "c", "d"));
        NavigableSet<String> tail = original.tailSet("b", true);
        assertEquals(Arrays.asList("b", "c", "d"), new ArrayList<>(tail));
        // modify the view
        tail.add("e");
        // original set must now contain the new element
        assertTrue(original.contains("e"));
        assertEquals(5, original.size());
        // subsequent tailSet call reflects the addition
        NavigableSet<String> newTail = original.tailSet("b", true);
        assertEquals(Arrays.asList("b", "c", "d", "e"), new ArrayList<>(newTail));
    }

    /**
     * Verify that calling {@code tailSet} with an incompatible {@code fromElement} type
     * results in a {@code ClassCastException}.
     */
    @Test
    void testTailSetClassCastException_IncompatibleType() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        // fromElement is an Integer while the set holds Strings
        assertThrows(ClassCastException.class, () -> set.tailSet(1, true));
    }

    /**
     * Verify that a {@code ClassCastException} is thrown when the underlying set contains
     * non‑Comparable elements and no explicit comparator is supplied.
     */
    @Test
    void testTailSetClassCastException_NonComparableElements() {
        class NotComparable {

            final String value;

            NotComparable(String v) {
                this.value = v;
            }
        }
        // The set is created with natural ordering (no comparator)
        NavigableSet<NotComparable> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(new NotComparable("x"));
        set.add(new NotComparable("y"));
        // Attempt to obtain a tail set with an element of a different type
        assertThrows(ClassCastException.class, () -> set.tailSet(new Object(), true));
    }

    /**
     * Force the internal {@code internalSet} field to {@code null} via reflection and verify
     * that {@code tailSet} throws a {@code NullPointerException}.
     */
    @Test
    void testTailSetNullPointerException_InternalSetNull() throws Exception {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("a");
        set.add("b");
        // Use reflection to nullify the internalSet field
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(set, null);
        assertThrows(NullPointerException.class, () -> set.tailSet("a", true));
    }

    /**
     * Verify that an empty underlying set always yields an empty tail set,
     * regardless of the {@code fromElement} or {@code inclusive} flag.
     */
    @Test
    void testTailSetEmptyUnderlyingSet() {
        NavigableSet<String> empty = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(empty.isEmpty());
        NavigableSet<String> tail1 = empty.tailSet("any", true);
        NavigableSet<String> tail2 = empty.tailSet(null, false);
        assertTrue(tail1.isEmpty());
        assertTrue(tail2.isEmpty());
    }
}
