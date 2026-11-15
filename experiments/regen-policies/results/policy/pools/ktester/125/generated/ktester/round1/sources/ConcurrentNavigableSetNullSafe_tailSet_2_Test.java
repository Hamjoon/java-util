package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_tailSet_2_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor for most tests; individual tests can replace it.
        set = new ConcurrentNavigableSetNullSafe<>();
        // Populate with a known ordering plus a null element.
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid cross‑test contamination.
        set.clear();
    }

    private static Stream<Arguments> provideTailSetInclusiveCases() {
        return Stream.of(Arguments.of(5, Arrays.asList(5, 6, 7, 8, 9, null)), Arguments.of(1, Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, null)), Arguments.of(9, Arrays.asList(9, null)), Arguments.of(10, Collections.emptyList()));
    }

    private static Stream<Arguments> provideReverseComparatorCases() {
        return Stream.of(// In reverse order, “greater‑or‑equal” means numerically ≤ fromElement.
        Arguments.of(3, Arrays.asList(3, 2, 1)), Arguments.of(5, Arrays.asList(5, 4, 3, 2, 1)), Arguments.of(6, Collections.emptyList()));
    }

    @Test
    void tailSet_IncludesFromElement_NaturalOrder() {
        // fromElement = "banana" (inclusive)
        SortedSet<String> tail = set.tailSet("banana");
        assertNotNull(tail);
        // Expected elements: "banana", "cherry", null (null is considered greater than any non‑null
        // element because the internal sentinel is ordered after all natural values).
        List<String> expected = Arrays.asList("banana", "cherry", null);
        assertEquals(expected, new ArrayList<>(tail));
    }

    @Test
    void tailSet_FromNull_IncludesNullAndHigher() {
        // fromElement = null (inclusive) – should return the whole set.
        SortedSet<String> tail = set.tailSet(null);
        assertNotNull(tail);
        List<String> expected = Arrays.asList("apple", "banana", "cherry", null);
        assertEquals(expected, new ArrayList<>(tail));
    }

    @Test
    void tailSet_FromElementNotPresent_ReturnsHigherElements() {
        // "avocado" is not present but would be ordered between "apple" and "banana".
        SortedSet<String> tail = set.tailSet("avocado");
        // Since "avocado" is not in the set, the view starts at the next higher element ("banana").
        List<String> expected = Arrays.asList("banana", "cherry", null);
        assertEquals(expected, new ArrayList<>(tail));
    }

    @Test
    void tailSet_WithCustomComparator_LengthOrder() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> customSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        customSet.addAll(Arrays.asList("kiwi", "apple", "banana", null));
        // fromElement = "apple" (length 5, inclusive). Order: null(0), kiwi(4), apple(5), banana(6)
        SortedSet<String> tail = customSet.tailSet("apple");
        List<String> expected = Arrays.asList("apple", "banana");
        assertEquals(expected, new ArrayList<>(tail));
    }

    @Test
    void tailSet_EmptySet_ReturnsEmpty() {
        NavigableSet<Integer> empty = new ConcurrentNavigableSetNullSafe<>();
        SortedSet<Integer> tail = empty.tailSet(5);
        assertTrue(tail.isEmpty());
    }

    @Test
    void tailSet_ModificationsReflectedInOriginalSet() {
        SortedSet<String> tail = set.tailSet("banana");
        // Remove an element via the tail view.
        assertTrue(tail.remove("cherry"));
        assertFalse(set.contains("cherry"));
        // Add a new element via the tail view.
        assertTrue(tail.add("date"));
        assertTrue(set.contains("date"));
        // The view should still be sorted correctly.
        List<String> expected = Arrays.asList("banana", "date", null);
        assertEquals(expected, new ArrayList<>(tail));
    }

    @ParameterizedTest(name = "{index}: fromElement={0} → {1}")
    @MethodSource("provideTailSetInclusiveCases")
    void tailSet_Inclusive_NaturalOrder(Integer fromElement, List<Integer> expected) {
        // Build a fresh Integer set containing 1‑9 and a null sentinel.
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 1; i <= 9; i++) {
            intSet.add(i);
        }
        // null is treated as the highest element
        intSet.add(null);
        SortedSet<Integer> tail = intSet.tailSet(fromElement);
        assertEquals(expected, new ArrayList<>(tail));
    }

    @Test
    void tailSet_FromElementLessThanSmallest_IncludesAll() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 1; i <= 9; i++) {
            intSet.add(i);
        }
        intSet.add(null);
        SortedSet<Integer> tail = intSet.tailSet(0);
        List<Integer> expected = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, null);
        assertEquals(expected, new ArrayList<>(tail));
    }

    @Test
    void tailSet_FromNull_TreatsNullAsLowestKey() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 1; i <= 9; i++) {
            intSet.add(i);
        }
        intSet.add(null);
        SortedSet<Integer> tail = intSet.tailSet(null);
        // According to the implementation, null is the highest key, so tailSet(null) returns only null.
        List<Integer> expected = Collections.singletonList(null);
        assertEquals(expected, new ArrayList<>(tail));
    }

    @Test
    void tailSet_EmptySet_WithNullFromElement_ReturnsEmpty() {
        NavigableSet<Integer> empty = new ConcurrentNavigableSetNullSafe<>();
        SortedSet<Integer> tail = empty.tailSet(null);
        assertTrue(tail.isEmpty());
    }

    @ParameterizedTest(name = "{index}: from={0} → {1}")
    @MethodSource("provideReverseComparatorCases")
    void tailSet_WithReverseComparator(Integer fromElement, List<Integer> expected) {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableSet<Integer> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        for (int i = 1; i <= 5; i++) {
            revSet.add(i);
        }
        SortedSet<Integer> tail = revSet.tailSet(fromElement);
        assertEquals(expected, new ArrayList<>(tail));
    }

    @Test
    void tailSet_TypeMismatch_StringOnIntegerSet() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertThrows(ClassCastException.class, () -> intSet.tailSet("five"));
    }

    @Test
    void tailSet_TypeMismatch_ObjectOnIntegerSet() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3));
        Object obj = new Object();
        assertThrows(ClassCastException.class, () -> intSet.tailSet(obj));
    }

    @Test
    void tailSet_ComparatorThrowsNPE_OnNullFromElement() {
        Comparator<String> npeComparator = (a, b) -> {
            if (a == null || b == null) {
                throw new NullPointerException("Comparator does not accept null");
            }
            return a.compareTo(b);
        };
        NavigableSet<String> setWithBadComparator = new ConcurrentNavigableSetNullSafe<>(npeComparator);
        setWithBadComparator.addAll(Arrays.asList("a", "b"));
        assertThrows(NullPointerException.class, () -> setWithBadComparator.tailSet(null));
    }

    @Test
    void tailSet_ComparatorThrowsIAE_OnNegativeValue() {
        Comparator<Integer> iaeComparator = (a, b) -> {
            if (a < 0 || b < 0) {
                throw new IllegalArgumentException("Negative values not allowed");
            }
            return Integer.compare(a, b);
        };
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>(iaeComparator);
        set.addAll(Arrays.asList(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> set.tailSet(-1));
    }

    @Test
    void tailSet_ComparatorThrowsRuntimeException_OnSpecificValue() {
        Comparator<Integer> runtimeComparator = (a, b) -> {
            if (a == 5 || b == 5) {
                throw new RuntimeException("Value 5 is forbidden");
            }
            return Integer.compare(a, b);
        };
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>(runtimeComparator);
        set.addAll(Arrays.asList(1, 2, 3, 4, 6, 7));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> set.tailSet(5));
        assertEquals("Value 5 is forbidden", ex.getMessage());
    }
}
