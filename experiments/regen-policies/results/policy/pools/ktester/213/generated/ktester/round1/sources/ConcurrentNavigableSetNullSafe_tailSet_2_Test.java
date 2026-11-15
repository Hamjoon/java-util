package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_tailSet_2_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh set for every test case.
        set = new ConcurrentNavigableSetNullSafe<>();
        // Natural ordering – iterator order will be: apple, banana, cherry, null
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

    /**
     * Helper to create a set pre‑populated with the integer sequence [1,3,5,6,7,8,9].
     */
    private static NavigableSet<Integer> createIntegerBaseSet() {
        NavigableSet<Integer> s = new ConcurrentNavigableSetNullSafe<>();
        Collections.addAll(s, 1, 3, 5, 6, 7, 8, 9);
        return s;
    }

    @Test
    void tailSet_InclusiveFromElement() {
        // inclusive by default
        SortedSet<String> tail = set.tailSet("banana");
        // The view should contain "banana", "cherry" and the null sentinel.
        assertEquals(3, tail.size(), "tailSet should contain three elements");
        Iterator<String> it = tail.iterator();
        assertEquals("banana", it.next(), "First element should be the lower bound");
        assertEquals("cherry", it.next(), "Next element should follow natural order");
        assertNull(it.next(), "The sentinel representing null should be last");
        assertFalse(it.hasNext(), "No further elements expected");
        // Mutating the view must affect the original set.
        assertTrue(tail.remove("cherry"));
        assertFalse(set.contains("cherry"));
        assertEquals(2, tail.size());
        assertEquals(3, set.size());
    }

    @Test
    void tailSet_FromElementGreaterThanAllElements() {
        // "zzz" > any element in the set
        SortedSet<String> tail = set.tailSet("zzz");
        // Since the bound is greater than every element, the view must be empty.
        assertTrue(tail.isEmpty(), "tailSet should be empty when fromElement is greater than any element");
    }

    @Test
    void tailSet_FromNullElement() {
        // The sentinel representing null is the highest element, so a tailSet from null
        // should contain only the null element itself.
        SortedSet<String> tail = set.tailSet(null);
        assertEquals(1, tail.size(), "tailSet(null) should contain exactly the null sentinel");
        assertNull(tail.first(), "The only element should be null");
        assertNull(tail.last(), "The only element should be null");
    }

    @Test
    void tailSet_ExclusiveFromElement() {
        // exclusive lower bound
        SortedSet<String> tail = set.tailSet("banana", false);
        // Expected elements: "cherry" and null (but not "banana").
        assertEquals(2, tail.size(), "Exclusive tailSet should contain two elements");
        Iterator<String> it = tail.iterator();
        assertEquals("cherry", it.next(), "First element should be the element after the bound");
        assertNull(it.next(), "The sentinel representing null should be last");
        assertFalse(it.hasNext(), "No further elements expected");
    }

    @Test
    void tailSet_ExclusiveFromElementWhenNoHigherElementExists() {
        // Using the highest non‑null element as the bound and exclusive should leave only null.
        SortedSet<String> tail = set.tailSet("cherry", false);
        assertEquals(1, tail.size(), "Only the null sentinel should remain");
        assertNull(tail.first(), "The remaining element must be null");
    }

    @Test
    void tailSet_ModificationsReflectedInOriginalSet() {
        // includes all elements
        SortedSet<String> tail = set.tailSet("apple");
        // Add a new element via the view.
        assertTrue(tail.add("date"));
        assertTrue(set.contains("date"));
        assertTrue(tail.contains("date"));
        // Remove null via the view.
        assertTrue(tail.remove(null));
        assertFalse(set.contains(null));
        assertFalse(tail.contains(null));
    }

    @Test
    void tailSet_InclusiveBasicBehaviour_ElementPresent() {
        NavigableSet<Integer> intSet = createIntegerBaseSet();
        // inclusive by default
        SortedSet<Integer> tail = intSet.tailSet(5);
        // Expected view: [5,6,7,8,9]
        List<Integer> expected = Arrays.asList(5, 6, 7, 8, 9);
        assertEquals(expected, new ArrayList<>(tail), "tailSet(5) should contain 5 and all higher elements");
    }

    @Test
    void tailSet_InclusiveBasicBehaviour_ElementAbsent() {
        NavigableSet<Integer> intSet = createIntegerBaseSet();
        // 4 not present, should start at 5
        SortedSet<Integer> tail = intSet.tailSet(4);
        List<Integer> expected = Arrays.asList(5, 6, 7, 8, 9);
        assertEquals(expected, new ArrayList<>(tail), "tailSet(4) should start with the next higher element (5)");
    }

    @Test
    void tailSet_NullFromElement_Throws() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        assertThrows(IllegalArgumentException.class, () -> intSet.tailSet(null), "Passing null to tailSet should raise IllegalArgumentException");
    }

    @Test
    void tailSet_EmptyBackingSet_ReturnsEmptyView() {
        NavigableSet<String> emptySet = new ConcurrentNavigableSetNullSafe<>();
        SortedSet<String> tail = emptySet.tailSet("b");
        assertTrue(tail.isEmpty(), "tailSet on an empty backing set must be empty");
    }

    @Test
    void tailSet_ReverseOrderComparator() {
        NavigableSet<String> revSet = new ConcurrentNavigableSetNullSafe<>(Comparator.reverseOrder());
        // natural order a<b<c<d, but reversed in the set
        Collections.addAll(revSet, "a", "b", "c", "d");
        // inclusive by default
        SortedSet<String> tail = revSet.tailSet("c");
        // In reverse order, "tail" means elements <= "c": ["c","b","a"]
        List<String> expected = Arrays.asList("c", "b", "a");
        assertEquals(expected, new ArrayList<>(tail), "Reverse‑order tailSet should return elements <= key");
    }

    @Test
    void tailSet_FromElementGreaterThanMax_ReturnsEmpty() {
        NavigableSet<Integer> intSet = createIntegerBaseSet();
        SortedSet<Integer> tail = intSet.tailSet(10);
        assertTrue(tail.isEmpty(), "tailSet(10) where 10 > max element should be empty");
    }

    @Test
    void tailSet_FromElementLessThanMin_ReturnsWholeSet() {
        NavigableSet<Integer> intSet = createIntegerBaseSet();
        SortedSet<Integer> tail = intSet.tailSet(-5);
        List<Integer> expected = Arrays.asList(1, 3, 5, 6, 7, 8, 9);
        assertEquals(expected, new ArrayList<>(tail), "tailSet(-5) should return the whole set");
    }

    @Test
    void tailSet_ViewReflectsAdditionToOriginalSet() {
        NavigableSet<Integer> intSet = createIntegerBaseSet();
        SortedSet<Integer> tail = intSet.tailSet(5);
        // Add a new element that satisfies the lower bound.
        assertTrue(intSet.add(10));
        assertTrue(tail.contains(10), "View must automatically include elements added to the backing set");
    }

    @Test
    void tailSet_AddingElementViolatingLowerBound_Throws() {
        NavigableSet<Integer> intSet = createIntegerBaseSet();
        SortedSet<Integer> tail = intSet.tailSet(5);
        // Attempt to add an element smaller than the lower bound.
        assertThrows(IllegalArgumentException.class, () -> tail.add(3), "Adding an element below the view's lower bound must throw IllegalArgumentException");
    }

    @Test
    void tailSet_SingleElementSet_BoundEqualsElement() {
        NavigableSet<String> single = new ConcurrentNavigableSetNullSafe<>();
        single.add("alpha");
        SortedSet<String> tail = single.tailSet("alpha");
        assertEquals(1, tail.size(), "Single‑element set tailSet with matching bound should contain that element");
        assertEquals("alpha", tail.first());
    }

    @Test
    void tailSet_SingleElementSet_BoundGreaterThanElement() {
        NavigableSet<String> single = new ConcurrentNavigableSetNullSafe<>();
        single.add("alpha");
        SortedSet<String> tail = single.tailSet("beta");
        assertTrue(tail.isEmpty(), "tailSet with bound greater than the only element must be empty");
    }

    @Test
    void tailSet_CaseInsensitiveComparator() {
        NavigableSet<String> ciSet = new ConcurrentNavigableSetNullSafe<>(String.CASE_INSENSITIVE_ORDER);
        Collections.addAll(ciSet, "a", "b", "c", "d");
        // should be treated as "b"
        SortedSet<String> tail = ciSet.tailSet("B");
        List<String> expected = Arrays.asList("b", "c", "d");
        assertEquals(expected, new ArrayList<>(tail), "Case‑insensitive tailSet should treat 'B' as 'b'");
    }

    @Test
    void tailSet_ViewBecomesEmptyAfterClearingOriginalSet() {
        NavigableSet<Integer> intSet = createIntegerBaseSet();
        SortedSet<Integer> tail = intSet.tailSet(3);
        assertFalse(tail.isEmpty(), "Pre‑condition: view should not be empty before clearing");
        intSet.clear();
        assertTrue(tail.isEmpty(), "After clearing the backing set, the view must become empty");
    }

    @Test
    void tailSet_NullFromElementWithNaturalComparator_Throws() {
        NavigableSet<String> nsSet = new ConcurrentNavigableSetNullSafe<>(Comparator.naturalOrder());
        assertThrows(IllegalArgumentException.class, () -> nsSet.tailSet(null), "Even with an explicit natural comparator, null arguments must be rejected");
    }
}
