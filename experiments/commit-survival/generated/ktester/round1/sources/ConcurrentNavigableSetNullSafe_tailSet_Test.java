package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.*;

class ConcurrentNavigableSetNullSafe_tailSet_Test {

    private ConcurrentNavigableSetNullSafe<String> naturalSet;

    private ConcurrentNavigableSetNullSafe<String> lengthComparatorSet;

    @BeforeEach
    void setupBeforeEach() {
        // Natural ordering set
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        naturalSet.add("apple");
        naturalSet.add("banana");
        naturalSet.add("cherry");
        // null is allowed
        naturalSet.add(null);
        // Comparator that orders by string length (null is treated as length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthComparatorSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        lengthComparatorSet.add(null);
        // length 4
        lengthComparatorSet.add("kiwi");
        // length 5
        lengthComparatorSet.add("apple");
        // length 6
        lengthComparatorSet.add("banana");
        // natural ordering set with elements a‑e and a null sentinel
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        naturalSet.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        naturalSet.add(null);
        // comparator by length (null treated as length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthComparatorSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        lengthComparatorSet.addAll(Arrays.asList(null, "kiwi", "apple", "banana"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination
        naturalSet.clear();
        lengthComparatorSet.clear();
        naturalSet.clear();
        lengthComparatorSet.clear();
    }

    @Test
    void tailSetInclusiveTrue_NaturalOrder() {
        NavigableSet<String> tail = naturalSet.tailSet("banana", true);
        assertNotNull(tail);
        assertEquals(3, tail.size());
        Iterator<String> it = tail.iterator();
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
        // Ensure the original set is unchanged
        assertEquals(4, naturalSet.size());
    }

    @Test
    void tailSetInclusiveFalse_NaturalOrder() {
        NavigableSet<String> tail = naturalSet.tailSet("banana", false);
        assertNotNull(tail);
        assertEquals(2, tail.size());
        Iterator<String> it = tail.iterator();
        assertEquals("cherry", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void tailSetFromNull_InclusiveTrue_NaturalOrder() {
        // Null is the greatest element in natural ordering (as observed in iterator order)
        NavigableSet<String> tail = naturalSet.tailSet(null, true);
        assertNotNull(tail);
        assertEquals(1, tail.size());
        assertTrue(tail.contains(null));
    }

    @Test
    void tailSetFromNull_InclusiveFalse_NaturalOrder() {
        // Exclusive null should yield an empty set because there is no element greater than null
        NavigableSet<String> tail = naturalSet.tailSet(null, false);
        assertNotNull(tail);
        assertTrue(tail.isEmpty());
    }

    @Test
    void tailSetInclusiveTrue_WithLengthComparator() {
        // With the length comparator, ordering is: null (0), kiwi (4), apple (5), banana (6)
        NavigableSet<String> tail = lengthComparatorSet.tailSet("apple", true);
        assertNotNull(tail);
        assertEquals(2, tail.size());
        Iterator<String> it = tail.iterator();
        // length 5, inclusive
        assertEquals("apple", it.next());
        // length 6
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void tailSetInclusiveFalse_WithLengthComparator() {
        NavigableSet<String> tail = lengthComparatorSet.tailSet("apple", false);
        assertNotNull(tail);
        assertEquals(1, tail.size());
        Iterator<String> it = tail.iterator();
        // only elements longer than "apple"
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void tailSetFromNull_InclusiveTrue_WithLengthComparator() {
        // Null is the smallest element according to the comparator (length 0)
        NavigableSet<String> tail = lengthComparatorSet.tailSet(null, true);
        assertNotNull(tail);
        assertEquals(4, tail.size());
        Iterator<String> it = tail.iterator();
        assertEquals(null, it.next());
        assertEquals("kiwi", it.next());
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void tailSetFromNull_InclusiveFalse_WithLengthComparator() {
        // Excluding null should start from the next element (kiwi)
        NavigableSet<String> tail = lengthComparatorSet.tailSet(null, false);
        assertNotNull(tail);
        assertEquals(3, tail.size());
        Iterator<String> it = tail.iterator();
        assertEquals("kiwi", it.next());
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for basic natural‑order behavior
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, inclusive={1} => expected={2}")
    @CsvSource({ // fromElement, inclusive, expectedElements (comma‑separated)
    "'c', true,  c,d,e,null", "'c', false, d,e,null", "null, true,  a,b,c,d,e,null", "null, false, a,b,c,d,e,null", // beyond greatest element → empty
    "'z', true,  ", // beyond greatest element → empty
    "'z', false, ", "'a', true,  a,b,c,d,e,null", "'a', false, b,c,d,e,null" })
    void tailSetBasicNaturalOrder(String from, boolean inclusive, String expectedCsv) {
        NavigableSet<String> tail = naturalSet.tailSet(from, inclusive);
        List<String> expected = expectedCsv.isEmpty() ? Collections.emptyList() : Arrays.asList(expectedCsv.split(","));
        assertEquals(expected.size(), tail.size(), "Size mismatch");
        assertIterableEquals(expected, tail, "Elements mismatch");
    }

    /* --------------------------------------------------------------
     *  Comparator identity & live‑view tests
     * -------------------------------------------------------------- */
    @Test
    void tailSetPreservesComparator_NaturalOrder() {
        NavigableSet<String> tail = naturalSet.tailSet("c", true);
        assertSame(naturalSet.comparator(), tail.comparator(), "Tail set should preserve the original comparator (natural order)");
    }

    @Test
    void tailSetPreservesComparator_LengthComparator() {
        NavigableSet<String> tail = lengthComparatorSet.tailSet("apple", true);
        assertSame(lengthComparatorSet.comparator(), tail.comparator(), "Tail set should preserve the original length comparator");
    }

    @Test
    void tailSetIsLiveView_AddAndRemoveReflectInOriginal() {
        NavigableSet<String> tail = naturalSet.tailSet("c", true);
        // add a new element via the tail set
        tail.add("f");
        assertTrue(naturalSet.contains("f"), "Addition via tail set must affect original set");
        // remove an element that is present in the tail
        assertTrue(tail.remove("c"));
        assertFalse(naturalSet.contains("c"), "Removal via tail set must affect original set");
    }

    @Test
    void tailSetSizeNearEnd_NaturalOrder() {
        NavigableSet<String> tailInclusive = naturalSet.tailSet("d", true);
        assertEquals(2, tailInclusive.size(), "Inclusive tail from 'd' should contain d and e (plus null if present)");
        assertTrue(tailInclusive.containsAll(Arrays.asList("d", "e", null)));
        NavigableSet<String> tailExclusive = naturalSet.tailSet("d", false);
        assertEquals(1, tailExclusive.size(), "Exclusive tail from 'd' should contain only e (plus null if present)");
        assertTrue(tailExclusive.containsAll(Arrays.asList("e", null)));
    }

    @Test
    void tailSetFirstWhenFromNullExclusive_NaturalOrder() {
        NavigableSet<String> tail = naturalSet.tailSet(null, false);
        // sentinel for null is the lowest, so first() should be the smallest real element "a"
        assertEquals("a", tail.first(), "first() should return the smallest real element even when exclusive null");
    }

    /* --------------------------------------------------------------
     *  Edge cases with length comparator
     * -------------------------------------------------------------- */
    @Test
    void tailSetLengthComparatorFromNullInclusive() {
        NavigableSet<String> tail = lengthComparatorSet.tailSet(null, true);
        List<String> expected = Arrays.asList(null, "kiwi", "apple", "banana");
        assertIterableEquals(expected, tail);
    }

    @Test
    void tailSetLengthComparatorFromNullExclusive() {
        NavigableSet<String> tail = lengthComparatorSet.tailSet(null, false);
        List<String> expected = Arrays.asList("kiwi", "apple", "banana");
        assertIterableEquals(expected, tail);
    }

    @Test
    void tailSetLengthComparatorFromAppleInclusive() {
        NavigableSet<String> tail = lengthComparatorSet.tailSet("apple", true);
        List<String> expected = Arrays.asList("apple", "banana");
        assertIterableEquals(expected, tail);
    }

    @Test
    void tailSetLengthComparatorFromAppleExclusive() {
        NavigableSet<String> tail = lengthComparatorSet.tailSet("apple", false);
        List<String> expected = Collections.singletonList("banana");
        assertIterableEquals(expected, tail);
    }

    /* --------------------------------------------------------------
     *  Exception scenarios
     * -------------------------------------------------------------- */
    @Test
    void tailSetWithIncompatibleType_ThrowsClassCastException() {
        // naturalSet contains Strings, passing an Integer should fail
        assertThrows(ClassCastException.class, () -> naturalSet.tailSet((String) (Object) 123, true));
    }

    @Test
    void tailSetWithNonComparableObject_ThrowsClassCastException() {
        Object nonComparable = new Object();
        assertThrows(ClassCastException.class, () -> naturalSet.tailSet((String) nonComparable, false));
    }

    @Test
    void tailSetWhenInternalSetIsNull_ThrowsNullPointerException() throws Exception {
        // Create an instance via reflection with a null internalSet
        ConcurrentNavigableSetNullSafe<String> broken = new ConcurrentNavigableSetNullSafe<>();
        // Force internalSet to null
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(broken, null);
        assertThrows(NullPointerException.class, () -> broken.tailSet("any", true));
    }

    /* --------------------------------------------------------------
     *  Additional verification of iterator order for null start case
     * -------------------------------------------------------------- */
    @Test
    void iteratorOrderWhenFromNullInclusive_NaturalOrder() {
        NavigableSet<String> tail = naturalSet.tailSet(null, true);
        List<String> expected = Arrays.asList("a", "b", "c", "d", "e", null);
        assertIterableEquals(expected, tail);
    }
}
