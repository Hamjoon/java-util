package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_headSet_2_Test {

    private NavigableSet<String> set;

    private NavigableSet<Integer> intSet;

    private NavigableSet<Double> doubleSet;

    private NavigableSet<String> stringSet;

    // used for heterogeneous element tests
    private NavigableSet<Object> rawSet;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for every test to guarantee isolation.
        intSet = new ConcurrentNavigableSetNullSafe<>();
        doubleSet = new ConcurrentNavigableSetNullSafe<>();
        stringSet = new ConcurrentNavigableSetNullSafe<>();
        rawSet = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear all sets – aids GC, not strictly required.
        intSet.clear();
        doubleSet.clear();
        stringSet.clear();
        rawSet.clear();
    }

    // ---------- headSet(E) basic functionality ----------
    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// toElement = 3, expected [1,2]
        Arguments.of(Arrays.asList(1, 2, 3, 4, 5), 3, Arrays.asList(1, 2)), // toElement = 6, expected [1,2,3,4,5]
        Arguments.of(Arrays.asList(1, 2, 3, 4, 5), 6, Arrays.asList(1, 2, 3, 4, 5)), // toElement = 1, expected []
        Arguments.of(Arrays.asList(1, 2, 3, 4, 5), 1, Collections.emptyList()), // toElement = null, expected NullPointerException
        Arguments.of(Arrays.asList(1, 2, 3), null, NullPointerException.class), // empty underlying set, toElement = 10, expected []
        Arguments.of(Collections.emptyList(), 10, Collections.emptyList()));
    }

    /* -------------------------------------------------------------
     * Tests for the exclusive headSet (default) implementation.
     * ------------------------------------------------------------- */
    @Test
    void headSetExclusive_WithNullAndNaturalOrder() {
        // Arrange
        // sentinel‑sorted before any non‑null
        set.add(null);
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // Act
        // exclusive
        SortedSet<String> head = set.headSet("banana");
        // Assert – expected elements: null and "apple"
        List<String> expected = Arrays.asList(null, "apple");
        assertEquals(expected.size(), head.size(), "headSet should contain exactly the elements less than \"banana\"");
        assertIterableEquals(expected, head, "Elements in headSet must be ordered according to the set's natural ordering");
    }

    @Test
    void headSetExclusive_EmptyResult() {
        set.add("apple");
        set.add("banana");
        // exclusive, nothing smaller
        SortedSet<String> head = set.headSet("apple");
        assertTrue(head.isEmpty(), "headSet should be empty when no elements are less than the bound");
    }

    /* -------------------------------------------------------------
     * Tests for the inclusive variant.
     * ------------------------------------------------------------- */
    @Test
    void headSetInclusive_WithNullAndNaturalOrder() {
        set.add(null);
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // inclusive = true, so "banana" should be part of the view
        SortedSet<String> head = set.headSet("banana", true);
        List<String> expected = Arrays.asList(null, "apple", "banana");
        assertEquals(expected.size(), head.size());
        assertIterableEquals(expected, head);
    }

    @Test
    void headSetInclusive_BoundNotPresent() {
        set.add("apple");
        set.add("cherry");
        // "banana" is not present; inclusive flag should behave like exclusive
        SortedSet<String> head = set.headSet("banana", true);
        List<String> expected = Collections.singletonList("apple");
        assertIterableEquals(expected, head);
    }

    /* -------------------------------------------------------------
     * Verify that the returned view is live.
     * ------------------------------------------------------------- */
    @Test
    void headSetViewIsLive_AddAndRemoveThroughView() {
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
        // contains null, apple, banana
        SortedSet<String> head = set.headSet("banana", true);
        // Add via the view
        assertTrue(head.add("avocado"));
        assertTrue(set.contains("avocado"));
        assertTrue(head.contains("avocado"));
        // Remove via the view
        assertTrue(head.remove(null));
        assertFalse(set.contains(null));
        assertFalse(head.contains(null));
        // Verify final content of the backing set
        List<String> expectedBacking = Arrays.asList("apple", "avocado", "banana", "cherry");
        assertIterableEquals(expectedBacking, set);
    }

    /* -------------------------------------------------------------
     * Tests with a custom comparator (by string length, null first).
     * ------------------------------------------------------------- */
    @Test
    void headSetWithLengthComparator_Exclusive() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 0
        set.add(null);
        // length 4
        set.add("kiwi");
        // length 5
        set.add("apple");
        // length 6
        set.add("banana");
        // Exclusive bound: length 5 (i.e., "apple")
        // should contain null and "kiwi"
        SortedSet<String> head = set.headSet("apple");
        List<String> expected = Arrays.asList(null, "kiwi");
        assertIterableEquals(expected, head);
    }

    @Test
    void headSetWithLengthComparator_Inclusive() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        set.add(null);
        set.add("kiwi");
        set.add("apple");
        set.add("banana");
        // Inclusive bound: length 5 ("apple")
        SortedSet<String> head = set.headSet("apple", true);
        List<String> expected = Arrays.asList(null, "kiwi", "apple");
        assertIterableEquals(expected, head);
    }

    /* -------------------------------------------------------------
     * Edge case – headSet on a set that only contains null.
     * ------------------------------------------------------------- */
    @Test
    void headSetWhenOnlyNullPresent() {
        set.add(null);
        // exclusive → empty
        SortedSet<String> head = set.headSet(null);
        assertTrue(head.isEmpty());
        // inclusive → contains null
        SortedSet<String> headInc = set.headSet(null, true);
        assertEquals(1, headInc.size());
        assertTrue(headInc.contains(null));
    }

    @ParameterizedTest(name = "[{index}] set={0}, toElement={1}")
    @MethodSource("basicFunctionalityProvider")
    void headSetBasicFunctionality(List<Integer> initialElements, Integer toElement, Object expected) {
        // Arrange
        initialElements.forEach(intSet::add);
        // Act & Assert
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expect an exception
            assertThrows((Class<? extends Throwable>) expected, () -> intSet.headSet(toElement), "headSet should throw the expected exception for toElement=" + toElement);
        } else {
            @SuppressWarnings("unchecked")
            List<Integer> expectedList = (List<Integer>) expected;
            SortedSet<Integer> head = intSet.headSet(toElement);
            assertIterableEquals(expectedList, head, "headSet result does not match expected for toElement=" + toElement);
        }
    }

    // ---------- headSet(E) lower bound less than smallest ----------
    @Test
    void headSetLowerBoundLessThanSmallest() {
        // Set contains {1,2,3,4,5}
        Arrays.asList(1, 2, 3, 4, 5).forEach(intSet::add);
        SortedSet<Integer> head = intSet.headSet(0);
        assertTrue(head.isEmpty(), "When the bound is less than the smallest element, headSet must be empty.");
    }

    // ---------- headSet(E) non‑existing intermediate element ----------
    @Test
    void headSetNonExistingIntermediateElement() {
        // Double set {1.0,2.0,3.0,4.0,5.0}
        Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0).forEach(doubleSet::add);
        SortedSet<Double> head = doubleSet.headSet(2.5);
        List<Double> expected = Arrays.asList(1.0, 2.0);
        assertIterableEquals(expected, head, "headSet should contain all elements strictly less than 2.5");
    }

    // ---------- headSet view mutability – add within bound ----------
    @Test
    void headSetViewMutabilityAddWithinBound() {
        // Set up a Double set with bound 4.0
        Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0).forEach(doubleSet::add);
        // exclusive bound
        SortedSet<Double> view = doubleSet.headSet(4.0);
        // Adding 3.5 (within bound) should succeed
        assertTrue(view.add(3.5), "Adding an element within the exclusive bound must succeed");
        assertTrue(doubleSet.contains(3.5), "Underlying set must reflect the addition");
        assertTrue(view.contains(3.5), "View must contain the newly added element");
    }

    // ---------- headSet view mutability – add out of bound ----------
    @Test
    void headSetViewMutabilityAddOutOfBound() {
        // Set up an Integer set with exclusive bound 4
        Arrays.asList(1, 2, 3, 4, 5).forEach(intSet::add);
        // exclusive
        SortedSet<Integer> view = intSet.headSet(4);
        // Adding 5 (outside bound) must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> view.add(5), "Adding an element not less than the exclusive bound should throw IllegalArgumentException");
    }

    // ---------- headSet with custom comparator (case‑insensitive strings) ----------
    @Test
    void headSetWithCaseInsensitiveComparator() {
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;
        NavigableSet<String> ciSet = new ConcurrentNavigableSetNullSafe<>(caseInsensitive);
        ciSet.addAll(Arrays.asList("Apple", "banana", "Cherry", "date"));
        // Exclusive bound "c"
        SortedSet<String> head = ciSet.headSet("c");
        List<String> expected = Arrays.asList("Apple", "banana");
        assertIterableEquals(expected, head, "Case‑insensitive comparator should order elements accordingly");
    }

    // ---------- headSet with incompatible element type ----------
    @Test
    void headSetIncompatibleElementType() {
        // Integer set, but we pass a String as bound
        Arrays.asList(1, 2, 3).forEach(intSet::add);
        assertThrows(ClassCastException.class, () -> intSet.headSet((Integer) (Object) "string"), "Passing a bound of incompatible type must throw ClassCastException");
    }

    // ---------- headSet internal null element handling ----------
    @Test
    void headSetInternalNullElementHandling() {
        // The set allows null internally
        NavigableSet<Integer> nullableSet = new ConcurrentNavigableSetNullSafe<>();
        // Add a null sentinel (the API permits null)
        nullableSet.add(null);
        nullableSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // headSet(5) forces comparison with the internal null sentinel → NPE
        assertThrows(NullPointerException.class, () -> nullableSet.headSet(5), "Comparing stored null with a non‑null bound should result in NullPointerException");
    }

    // ---------- headSet comparator throws RuntimeException ----------
    @Test
    void headSetComparatorThrowsRuntimeException() {
        Comparator<String> badComparator = (a, b) -> {
            if ("bad".equals(a) || "bad".equals(b)) {
                throw new IllegalArgumentException("bad value encountered");
            }
            return a.compareTo(b);
        };
        NavigableSet<String> badSet = new ConcurrentNavigableSetNullSafe<>(badComparator);
        badSet.addAll(Arrays.asList("good", "alsoGood"));
        // Adding "bad" to the set is allowed, but invoking headSet("bad") triggers the comparator
        badSet.add("bad");
        assertThrows(IllegalArgumentException.class, () -> badSet.headSet("bad"), "Comparator throwing IllegalArgumentException must propagate");
    }

    // ---------- headSet mixed element types without comparator ----------
    @Test
    void headSetMixedElementTypesWithoutComparator() {
        // Raw set that stores heterogeneous elements
        rawSet.add(1);
        rawSet.add(2);
        rawSet.add("3");
        // headSet(2) requires natural ordering across Integer and String → ClassCastException
        assertThrows(ClassCastException.class, () -> rawSet.headSet(2), "Mixed element types without a comparator must cause ClassCastException");
    }
}
