package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_headSet_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh set for each test – natural ordering
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC
        set = null;
    }

    // Example of a method source – not used currently but kept for extensibility
    static Stream<Arguments> basicHeadSetProvider() {
        return Stream.of(Arguments.of(5, false, Arrays.asList(1, 2, 3, 4)), Arguments.of(5, true, Arrays.asList(1, 2, 3, 4, 5)));
    }

    @Test
    void headSetExclusive_excludesUpperBound_andIncludesNull() {
        // Arrange
        set.add(null);
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // Act
        NavigableSet<String> head = set.headSet("banana", false);
        // Assert
        assertEquals(2, head.size(), "Expected only elements less than \"banana\"");
        assertTrue(head.contains(null), "Null should be present in the head set");
        assertTrue(head.contains("apple"), "\"apple\" should be present in the head set");
        assertFalse(head.contains("banana"), "\"banana\" must be excluded (exclusive)");
        assertFalse(head.contains("cherry"), "\"cherry\" must be excluded");
    }

    @Test
    void headSetInclusive_includesUpperBound_whenElementIsNull() {
        // Arrange
        set.add(null);
        set.add("apple");
        set.add("banana");
        // Act
        NavigableSet<String> head = set.headSet(null, true);
        // Assert
        assertEquals(1, head.size(), "Only the null element should be present");
        assertTrue(head.contains(null), "Null must be included when inclusive flag is true");
        assertFalse(head.contains("apple"));
        assertFalse(head.contains("banana"));
    }

    @Test
    void headSetWithComparator_inclusiveRespectsCustomOrdering() {
        // Arrange – comparator orders by string length (null is treated as length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        lengthSet.add(null);
        // length 4
        lengthSet.add("kiwi");
        // length 5
        lengthSet.add("apple");
        // length 6
        lengthSet.add("banana");
        // Act – inclusive head set up to "banana" (length 6)
        NavigableSet<String> head = lengthSet.headSet("banana", true);
        // Assert
        assertEquals(4, head.size(), "All elements up to and including length 6 should be present");
        assertTrue(head.contains(null));
        assertTrue(head.contains("kiwi"));
        assertTrue(head.contains("apple"));
        assertTrue(head.contains("banana"));
    }

    @Test
    void headSetIsAViewThatDoesNotIncludeLaterAdditionsBeyondUpperBound() {
        // Arrange
        set.add(null);
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // Obtain a head set up to (and inclusive) "banana"
        NavigableSet<String> head = set.headSet("banana", true);
        assertEquals(3, head.size(), "Initial head set size should be 3 (null, apple, banana)");
        // Add an element that is greater than the upper bound of the head set
        // "date" > "banana" in natural order
        set.add("date");
        // The previously obtained head set must remain unchanged
        assertEquals(3, head.size(), "Head set size must stay unchanged after adding out‑of‑range element");
        assertFalse(head.contains("date"), "Head set must not contain the newly added out‑of‑range element");
    }

    @Test
    void headSetReturnedSetIsNavigableAndSupportsFurtherOperations() {
        // Arrange
        set.add(null);
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // Act
        // should contain null, apple, banana
        NavigableSet<String> head = set.headSet("cherry", false);
        // Verify navigational methods on the returned set
        assertEquals("banana", head.last(), "Last element of the head set should be \"banana\"");
        assertEquals(null, head.first(), "First element of the head set should be null");
        assertTrue(head.contains("apple"));
        assertFalse(head.contains("cherry"));
        // Mutate the head set and ensure changes reflect in the original set
        assertTrue(head.remove("apple"));
        assertFalse(set.contains("apple"));
        assertEquals(2, head.size());
        // original set lost one element
        assertEquals(3, set.size());
    }

    @Test
    void headSet_typeSafetyAndCasting_throwsClassCastException() {
        // Set is typed to Integer
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.add(1);
        intSet.add(2);
        // Pass a String – maskNull works but internalSet.headSet will try to compare
        assertThrows(ClassCastException.class, () -> ((ConcurrentNavigableSetNullSafe<Integer>) intSet).headSet("string", false), "Expected ClassCastException when comparator cannot compare String with Integer");
    }

    @Test
    void headSet_basicExclusive_returnsElementsLessThanToElement() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> head = intSet.headSet(5, false);
        assertIterableEquals(Arrays.asList(1, 2, 3, 4), head, "headSet(5, false) should contain [1,2,3,4]");
    }

    @Test
    void headSet_basicInclusive_returnsElementsUpToAndIncludingToElement() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> head = intSet.headSet(5, true);
        assertIterableEquals(Arrays.asList(1, 2, 3, 4, 5), head, "headSet(5, true) should contain [1,2,3,4,5]");
    }

    @Test
    void headSet_toElementGreaterThanMax_returnsWholeSet() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> head = intSet.headSet(10, false);
        assertIterableEquals(Arrays.asList(1, 2, 3, 4, 5, 6), head, "headSet(10, false) should return the entire set");
    }

    @Test
    void headSet_toElementLessThanMin_returnsEmptySet() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> head = intSet.headSet(0, true);
        assertTrue(head.isEmpty(), "headSet(0, true) should be empty when toElement < min element");
    }

    @Test
    void headSet_nullToElement_exclusiveReturnsEmpty() {
        NavigableSet<String> strSet = new ConcurrentNavigableSetNullSafe<>();
        strSet.add(null);
        strSet.add("a");
        strSet.add("b");
        NavigableSet<String> head = strSet.headSet(null, false);
        assertTrue(head.isEmpty(), "headSet(null, false) should be empty because null is the lowest sentinel");
    }

    @Test
    void headSet_emptyOriginalSet_alwaysReturnsEmpty() {
        NavigableSet<Integer> emptySet = new ConcurrentNavigableSetNullSafe<>();
        NavigableSet<Integer> head = emptySet.headSet(5, true);
        assertTrue(head.isEmpty(), "headSet on an empty set must return an empty set");
    }

    @Test
    void headSet_customComparatorReverse_exclusive() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableSet<Integer> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        // natural order insertion, but comparator reverses
        revSet.addAll(Arrays.asList(1, 2, 3, 4, 5, 6));
        // In reverse order, elements greater than 3 (in natural order) appear first
        NavigableSet<Integer> head = revSet.headSet(3, false);
        assertIterableEquals(Arrays.asList(6, 5, 4), head, "Reverse comparator exclusive headSet(3,false) should be [6,5,4]");
    }

    @Test
    void headSet_customComparatorReverse_inclusive() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableSet<Integer> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        revSet.addAll(Arrays.asList(1, 2, 3, 4, 5, 6));
        NavigableSet<Integer> head = revSet.headSet(3, true);
        assertIterableEquals(Arrays.asList(6, 5, 4, 3), head, "Reverse comparator inclusive headSet(3,true) should be [6,5,4,3]");
    }

    @Test
    void headSet_modifyingReturnedSetReflectsInOriginal() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5, 6));
        // [1,2,3,4]
        NavigableSet<Integer> head = intSet.headSet(5, false);
        assertTrue(head.remove(2), "Removing 2 from the head set should succeed");
        assertFalse(intSet.contains(2), "Original set must no longer contain 2 after removal from view");
        assertEquals(5, intSet.size(), "Original set size should be reduced by one");
    }

    @Test
    void headSet_illegalArgumentExceptionWhenElementOutsideBoundedSubset() {
        // Create a full set first
        NavigableSet<Integer> full = new ConcurrentNavigableSetNullSafe<>();
        full.addAll(Arrays.asList(1, 2, 3, 4, 5, 6));
        // Create a bounded view: elements 2..5 inclusive
        NavigableSet<Integer> bounded = full.subSet(2, true, 5, true);
        // The bounded view is itself a ConcurrentNavigableSetNullSafe instance
        // headSet with an element lower than the lower bound should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> bounded.headSet(1, false), "Calling headSet(1,false) on a bounded subset (2..5) must throw IllegalArgumentException");
    }
}
