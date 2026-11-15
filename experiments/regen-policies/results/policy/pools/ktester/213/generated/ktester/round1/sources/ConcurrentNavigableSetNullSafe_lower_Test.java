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
class ConcurrentNavigableSetNullSafe_lower_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor uses natural ordering and null‑masking.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared so no state leaks between tests.
        if (set != null) {
            set.clear();
        }
    }

    /**
     * Data for the "lower method basic behavior" group.
     */
    private static Stream<Arguments> basicBehaviorProvider() {
        return Stream.of(Arguments.of(List.of("a", "b", "c"), "b", "a", "Set contains ['a','b','c']; lower('b') should return the immediate predecessor 'a'."), Arguments.of(List.of("a", "b", "c"), "a", null, "Set contains ['a','b','c']; lower('a') should return null because there is no element smaller than 'a'."), Arguments.of(List.of("a", "b", "c"), "d", "c", "Set contains ['a','b','c']; lower('d') (element not present) should return the greatest element less than 'd', which is 'c'."));
    }

    /**
     * Data for the "lower method null handling" group.
     */
    private static Stream<Arguments> nullHandlingProvider() {
        return Stream.of(Arguments.of(List.of((String) null, "a"), null, null, "Set contains [null, 'a']; lower(null) should return null because null is the smallest element."), Arguments.of(List.of((String) null, "a"), "a", null, "Set contains [null, 'a']; lower('a') should return null, the masked‑null element that represents the stored null."), Arguments.of(List.of((String) null, "a", "b"), "b", "a", "Set contains [null, 'a', 'b']; lower('b') should skip the null and return 'a' as the immediate predecessor."));
    }

    /**
     * Data for the "lower method empty set" group.
     */
    private static Stream<Arguments> emptySetProvider() {
        return Stream.of(Arguments.of("any", null, "When the underlying set is empty, lower(any) must return null regardless of the argument."), Arguments.of(null, null, "When the underlying set is empty, lower(null) must also return null."));
    }

    /**
     * Data for the "lower method custom comparator (reverse order)" group.
     */
    private static Stream<Arguments> reverseComparatorProvider() {
        Comparator<String> reverse = Comparator.reverseOrder();
        return Stream.of(Arguments.of(List.of("c", "b", "a"), "b", "c", "Reverse order set ['c','b','a']; lower('b') should return 'c'."), Arguments.of(List.of("c", "b", "a"), "a", "b", "Reverse order set ['c','b','a']; lower('a') should return 'b'."), Arguments.of(List.of("c", "b", "a"), "d", "a", "Reverse order set ['c','b','a']; lower('d') (larger than any) should return greatest element 'a'."));
    }

    @Test
    void lowerReturnsGreatestElementLessThanGivenElement() {
        // Arrange
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add("date");
        // Act & Assert
        assertEquals("banana", set.lower("cherry"));
        assertEquals("apple", set.lower("banana"));
        // nothing smaller than the smallest element
        assertNull(set.lower("apple"));
    }

    @Test
    void lowerWorksWithComparatorOrdering() {
        // Comparator that orders strings by length, null considered length 0.
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
        // Act & Assert – based on length ordering
        assertEquals("bb", lengthSet.lower("ccc"));
        assertEquals("a", lengthSet.lower("bb"));
        assertEquals(null, lengthSet.lower("a"));
        // nothing smaller than null (the sentinel)
        assertNull(lengthSet.lower(null));
    }

    @Test
    void lowerHandlesNullElementInSet() {
        // Arrange – add null together with other values
        set.add(null);
        set.add("apple");
        set.add("banana");
        // Act & Assert
        // The natural ordering treats null as the smallest element.
        // Therefore the greatest element less than "apple" is null.
        assertNull(set.lower("apple"));
        // The greatest element less than "banana" is "apple".
        assertEquals("apple", set.lower("banana"));
        // The greatest element less than null does not exist.
        assertNull(set.lower(null));
    }

    @Test
    void lowerReturnsNullWhenNoElementIsLower() {
        // Arrange – only one element in the set
        set.add("single");
        // Act & Assert
        // nothing lower than the only element
        assertNull(set.lower("single"));
        // "a" is less than "single", still no lower element
        assertNull(set.lower("a"));
        // null is considered smallest
        assertNull(set.lower(null));
    }

    @Test
    void lowerWorksWithMixedNullAndComparator() {
        Comparator<String> reverseComparator = Comparator.nullsLast(Comparator.naturalOrder()).reversed();
        NavigableSet<String> revSet = new ConcurrentNavigableSetNullSafe<>(reverseComparator);
        revSet.add(null);
        revSet.add("apple");
        revSet.add("banana");
        // In reversed order, the highest (first) element is null, then "banana", then "apple".
        // lower(e) should return the element that is *greater* in natural order.
        // "banana" > "apple" in natural order
        assertEquals("banana", revSet.lower("apple"));
        // null is greater than any non‑null
        assertEquals(null, revSet.lower("banana"));
        // nothing greater than null in this ordering
        assertNull(revSet.lower(null));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("basicBehaviorProvider")
    void lowerBasicBehavior(List<String> initialElements, String query, String expected, String _description) {
        // Arrange
        initialElements.forEach(set::add);
        // Act
        String result = set.lower(query);
        // Assert
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nullHandlingProvider")
    void lowerNullHandling(List<String> initialElements, String query, String expected, String _description) {
        // Arrange
        initialElements.forEach(set::add);
        // Act
        String result = set.lower(query);
        // Assert
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("emptySetProvider")
    void lowerEmptySet(String query, String _expectedIgnored, String _description) {
        // No elements added – set is empty.
        // Act
        String result = set.lower(query);
        // Assert
        assertNull(result);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("reverseComparatorProvider")
    void lowerReverseComparator(List<String> initialElements, String query, String expected, String _description) {
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableSet<String> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        initialElements.forEach(revSet::add);
        String result = revSet.lower(query);
        assertEquals(expected, result);
    }

    @Test
    void lowerThrowsClassCastExceptionForIncompatibleType() {
        // The set is declared for Strings; passing an Object should fail.
        set.add("alpha");
        set.add("beta");
        assertThrows(ClassCastException.class, () -> {
            // The raw type is used to bypass compile‑time generics.
            @SuppressWarnings("unchecked")
            NavigableSet rawSet = set;
            rawSet.lower(new Object());
        });
    }

    @Test
    void lowerThrowsNullPointerExceptionWhenComparatorDisallowsNull() {
        // Natural order comparator throws NPE for null arguments.
        NavigableSet<String> natSet = new ConcurrentNavigableSetNullSafe<>(Comparator.naturalOrder());
        natSet.add("x");
        natSet.add("y");
        assertThrows(NullPointerException.class, () -> natSet.lower(null));
    }

    @Test
    void lowerThrowsIllegalArgumentExceptionForOutOfRangeElement() {
        // Comparator that rejects the string "bad".
        Comparator<String> strictComparator = (a, b) -> {
            if ("bad".equals(a) || "bad".equals(b)) {
                throw new IllegalArgumentException("Value not allowed");
            }
            return a.compareTo(b);
        };
        NavigableSet<String> strictSet = new ConcurrentNavigableSetNullSafe<>(strictComparator);
        strictSet.add("good");
        strictSet.add("nice");
        // Adding a disallowed element should already throw; however we test lower() directly.
        assertThrows(IllegalArgumentException.class, () -> strictSet.lower("bad"));
    }
}
