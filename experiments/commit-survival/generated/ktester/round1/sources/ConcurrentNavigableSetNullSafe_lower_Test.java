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
class ConcurrentNavigableSetNullSafe_lower_Test {

    private NavigableSet<String> set;

    private NavigableSet<String> lengthOrderedSet;

    @BeforeEach
    void setupBeforeEach() {
        // Default (natural ordering) set
        set = new ConcurrentNavigableSetNullSafe<>();
        // Set that orders strings by length (null is considered the greatest element)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthOrderedSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination
        set.clear();
        lengthOrderedSet.clear();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// Set contains {1,2,3}. lower(2) => 1
        Arguments.of(2, List.of(1, 2, 3), 1, "Set contains {1,2,3}. The greatest element strictly less than 2 is 1."), // Set contains {1,2,3}. lower(2.5) => 2
        Arguments.of(2.5, List.of(1, 2, 3), 2, "Set contains {1,2,3}. 2.5 is not present; the greatest element less than 2.5 is 2."), // Set contains {1,2,3}. lower(1) => null
        Arguments.of(1, List.of(1, 2, 3), null, "Set contains {1,2,3}. 1 is the smallest element, so there is no element lower than it."), // Set contains {"a","b"}. lower("c") => "b"
        Arguments.of("c", List.of("a", "b"), "b", "Set contains {\"a\",\"b\"}. The greatest element lower than \"c\" is \"b\"."));
    }

    private static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(// Set contains {null, 1, 2}. lower(null) => 2 (null is treated as greatest)
        Arguments.of(null, List.of((Integer) null, 1, 2), 2, "Set contains {null,1,2}. lower(null) should return the greatest non‑null element (2)."), // Set contains {null, 1, 2}. lower(0) => null (no element < 0)
        Arguments.of(0, List.of((Integer) null, 1, 2), null, "Set contains {null,1,2}. The greatest element less than 0 is null (masked null is the smallest)."));
    }

    private static Stream<Arguments> boundaryExtremesProvider() {
        List<Integer> extremes = List.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE);
        return Stream.of(Arguments.of(Integer.MIN_VALUE, extremes, null, "Set contains {Integer.MIN_VALUE,0,Integer.MAX_VALUE}. lower(Integer.MIN_VALUE) returns null because it is the smallest element."), Arguments.of(Integer.MAX_VALUE, extremes, 0, "Set contains {Integer.MIN_VALUE,0,Integer.MAX_VALUE}. lower(Integer.MAX_VALUE) returns 0, the greatest element strictly less than MAX_VALUE."));
    }

    @Test
    void lowerOnEmptySetReturnsNull() {
        // GIVEN an empty set
        // WHEN lower is called
        String result = set.lower("any");
        // THEN the result is null
        assertNull(result);
    }

    @Test
    void lowerWithExistingSmallerElement() {
        // GIVEN a set with several elements
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // WHEN lower is called for "banana"
        String result = set.lower("banana");
        // THEN the greatest element less than "banana" is "apple"
        assertEquals("apple", result);
    }

    @Test
    void lowerWhenNoSmallerElementReturnsNull() {
        // GIVEN a set where the queried element is the smallest
        set.add("apple");
        set.add("banana");
        // WHEN lower is called for "apple"
        String result = set.lower("apple");
        // THEN there is no smaller element
        assertNull(result);
    }

    @Test
    void lowerWithNullElementReturnsGreatestNonNull() {
        // GIVEN a set that contains null and other values
        set.add(null);
        set.add("apple");
        set.add("banana");
        // WHEN lower is called with null (null is treated as the greatest element)
        String result = set.lower(null);
        // THEN the method should return the greatest non‑null element, i.e., "banana"
        assertEquals("banana", result);
    }

    @Test
    void lowerWithComparatorAndNullReturnsGreatestByComparator() {
        // GIVEN a length‑based comparator set containing null and strings of different lengths
        // considered greatest
        lengthOrderedSet.add(null);
        // length 4
        lengthOrderedSet.add("kiwi");
        // length 5
        lengthOrderedSet.add("apple");
        // length 6
        lengthOrderedSet.add("banana");
        // WHEN lower is called with null
        String result = lengthOrderedSet.lower(null);
        // THEN the result should be the element with the longest length, i.e., "banana"
        assertEquals("banana", result);
    }

    @Test
    void lowerWithComparatorOnSpecificElement() {
        // GIVEN the same length‑ordered set
        lengthOrderedSet.add(null);
        lengthOrderedSet.add("kiwi");
        lengthOrderedSet.add("apple");
        lengthOrderedSet.add("banana");
        // WHEN lower is called for "apple" (length 5)
        String result = lengthOrderedSet.lower("apple");
        // THEN the greatest element with length < 5 is "kiwi"
        assertEquals("kiwi", result);
    }

    @Test
    void lowerWhenElementIsNullAndSetHasOnlyNull() {
        // GIVEN a set that contains only null
        set.add(null);
        // WHEN lower is called with null
        String result = set.lower(null);
        // THEN there is no element less than null
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] lower({0}) on {1} => {2}")
    @MethodSource("basicFunctionalityProvider")
    <T extends Comparable<T>> void lowerBasicFunctionality(T query, Collection<T> initialElements, T expected, String description) {
        // Build a fresh set for each invocation
        @SuppressWarnings("unchecked")
        NavigableSet<T> ns = new ConcurrentNavigableSetNullSafe<>();
        ns.addAll(initialElements);
        // Exercise the method under test
        T actual = ns.lower(query);
        // Verify the outcome
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] lower({0}) on setWithNulls => {2}")
    @MethodSource("nullHandlingProvider")
    <T extends Comparable<T>> void lowerNullHandling(T query, Collection<T> initialElements, T expected, String description) {
        @SuppressWarnings("unchecked")
        NavigableSet<T> ns = new ConcurrentNavigableSetNullSafe<>();
        ns.addAll(initialElements);
        T actual = ns.lower(query);
        assertEquals(expected, actual, description);
    }

    @Test
    void lowerOnEmptySetReturnsNull_Parameterized() {
        NavigableSet<Integer> empty = new ConcurrentNavigableSetNullSafe<>();
        assertNull(empty.lower(5), "When the set is empty, lower(anything) must return null.");
    }

    @ParameterizedTest(name = "[{index}] lower({0}) on extremeSet => {2}")
    @MethodSource("boundaryExtremesProvider")
    void lowerBoundaryExtremes(Integer query, Collection<Integer> initialElements, Integer expected, String description) {
        NavigableSet<Integer> ns = new ConcurrentNavigableSetNullSafe<>();
        ns.addAll(initialElements);
        assertEquals(expected, ns.lower(query), description);
    }

    @Test
    void lowerWithReverseOrderComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableSet<String> reverseSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        reverseSet.addAll(List.of("a", "b", "c"));
        // In reverse order: c > b > a, therefore lower("b") == "c"
        assertEquals("c", reverseSet.lower("b"), "With a reverse‑order comparator, lower(\"b\") should return \"c\".");
    }

    @Test
    void lowerWithDuplicatesIgnored() {
        NavigableSet<Integer> dupSet = new ConcurrentNavigableSetNullSafe<>();
        // duplicate 5 is ignored
        dupSet.addAll(List.of(1, 2, 3, 5, 5));
        assertEquals(3, dupSet.lower(4), "Set contains {1,2,3,5}. lower(4) should return 3.");
    }

    @Test
    void lowerWithIncompatibleTypeThrowsClassCastException() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(List.of(1, 2, 3));
        // The set expects Integer; passing a String should fail
        assertThrows(ClassCastException.class, () -> intSet.lower((Integer) (Object) "java.lang.Object"), "Calling lower() with an element of an incompatible type should throw ClassCastException.");
    }

    @Test
    void lowerWithUuidAgainstIntegerSetThrowsClassCastException() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.addAll(List.of(1, 2, 3));
        UUID uuid = UUID.randomUUID();
        assertThrows(ClassCastException.class, () -> intSet.lower((Integer) (Object) uuid), "Calling lower() with a UUID on an Integer set should throw ClassCastException.");
    }

    // because ConcurrentNavigableSetNullSafe masks null values internally.
    @Test
    void lowerNullDoesNotThrowNPEWhenNullIsSupported() {
        NavigableSet<Integer> ns = new ConcurrentNavigableSetNullSafe<>();
        ns.addAll(List.of(null, 1, 2));
        // Should return the greatest non‑null element (2) rather than throwing NPE
        assertEquals(2, ns.lower(null));
    }
}
