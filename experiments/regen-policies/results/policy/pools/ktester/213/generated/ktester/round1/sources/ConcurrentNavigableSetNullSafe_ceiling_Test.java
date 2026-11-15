package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

class ConcurrentNavigableSetNullSafe_ceiling_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural‑order) constructor for most tests.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid cross‑test contamination.
        set.clear();
    }

    private static Stream<Arguments> provideBranchCoverageCases() {
        return Stream.of(// 1. Empty set, query null → null
        Arguments.of(List.of(), null, null, "Empty set: ceiling(null) → null"), // 2. Set contains only a null element, query null → null
        Arguments.of(List.of((Object) null), null, null, "Only stored null element → ceiling(null) returns null"), // 3. Set contains "alpha" only, query null → "alpha"
        Arguments.of(List.of("alpha"), null, "alpha", "No stored null, smallest element is ceiling of null"), // 4. Exact match "beta"
        Arguments.of(List.of("beta"), "beta", "beta", "Exact element present"), // 5. Higher element "gamma" when querying "beta"
        Arguments.of(List.of("gamma"), "beta", "gamma", "Higher element returned when exact match missing"), // 6. No element >= "zeta"
        Arguments.of(List.of("a", "b", "c"), "zeta", null, "No higher element → null"));
    }

    private static Stream<Arguments> provideAdditionalScenarios() {
        return Stream.of(// null stored + "alpha"
        Arguments.of(List.of((Object) null, "alpha"), null, null, "Stored null and non‑null, ceiling(null) returns stored null"), // same set, query "alpha"
        Arguments.of(List.of((Object) null, "alpha"), "alpha", "alpha", "Exact non‑null element present"), // only null and "alpha", query "beta" → null
        Arguments.of(List.of((Object) null, "alpha"), "beta", null, "Query greater than any element → null"), // set {beta, delta, epsilon}, query "alpha" → "beta"
        Arguments.of(List.of("beta", "delta", "epsilon"), "alpha", "beta", "Smallest greater element"), // set {beta, delta}, query "gamma" → "delta"
        Arguments.of(List.of("beta", "delta"), "gamma", "delta", "Between two elements → next higher"), // same set, query "delta" → "delta"
        Arguments.of(List.of("beta", "delta"), "delta", "delta", "Exact match on higher element"), // Integer set {1,3,5}, query 2 → 3
        Arguments.of(List.of(1, 3, 5), 2, 3, "Integer ceiling: 2 → 3"), // Integer set {1,3,5} without null, query null → 1
        Arguments.of(List.of(1, 3, 5), null, 1, "Integer ceiling: null → smallest element"));
    }

    @Test
    void ceiling_NaturalOrder_FoundExactMatch() {
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // null is allowed
        set.add(null);
        // Exact match should return the element itself
        assertEquals("banana", set.ceiling("banana"));
    }

    @Test
    void ceiling_NaturalOrder_FoundHigherElement() {
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
        // "blue" is between "banana" and "cherry"
        assertEquals("cherry", set.ceiling("blue"));
    }

    @Test
    void ceiling_NaturalOrder_NoHigherElement() {
        set.add("apple");
        set.add("banana");
        // There is no element >= "z"
        assertNull(set.ceiling("z"));
    }

    @Test
    void ceiling_NaturalOrder_WithNullArgument() {
        set.add("apple");
        set.add(null);
        // According to the class contract, null is treated as the smallest element.
        // Therefore ceiling(null) should return null itself.
        assertNull(set.ceiling(null));
    }

    @Test
    void ceiling_NaturalOrder_EmptySet() {
        // Empty set should always return null
        assertNull(set.ceiling("anything"));
    }

    @Test
    void ceiling_WithCustomComparator_LengthBased() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        set.add("kiwi");
        // length 5
        set.add("apple");
        // length 6
        set.add("banana");
        // length 0 (sentinel)
        set.add(null);
        // Null is the smallest, so ceiling(null) returns null
        assertNull(set.ceiling(null));
        // Request ceiling for a string of length 4 ("pear").
        // The comparator treats any length‑4 string as equal, so the set returns the
        // first element with length 4, which is "kiwi".
        assertEquals("kiwi", set.ceiling("pear"));
        // Request ceiling for a string longer than any element in the set.
        assertNull(set.ceiling("watermelon"));
    }

    @Test
    void ceiling_WithCustomComparator_NullIsLowest() {
        Comparator<String> reverseAlphabetical = Comparator.reverseOrder();
        set = new ConcurrentNavigableSetNullSafe<>(reverseAlphabetical);
        set.add("zebra");
        set.add("yak");
        set.add(null);
        // In reverse alphabetical order, null (masked sentinel) is still considered
        // lower than any real element, so ceiling(null) returns null.
        assertNull(set.ceiling(null));
        // Ceiling of "a" (the smallest real element) should be the highest element
        // because of reverse ordering.
        assertEquals("zebra", set.ceiling("a"));
    }

    /**
     * Branch‑coverage scenarios for {@code ceiling}.
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideBranchCoverageCases")
    void ceiling_BranchCoverage(List<Object> initialElements, Object query, Object expected, String description) {
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe<Object> localSet = new ConcurrentNavigableSetNullSafe<>();
        for (Object e : initialElements) {
            localSet.add(e);
        }
        assertEquals(expected, localSet.ceiling(query), description);
    }

    /**
     * Additional scenarios (including mixed null / non‑null and integer sets).
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideAdditionalScenarios")
    void ceiling_AdditionalScenarios(List<Object> initialElements, Object query, Object expected, String description) {
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe<Object> localSet = new ConcurrentNavigableSetNullSafe<>();
        for (Object e : initialElements) {
            localSet.add(e);
        }
        assertEquals(expected, localSet.ceiling(query), description);
    }

    @Test
    void ceiling_ClassCastException_WrongArgumentType() {
        // Set is typed as String; we deliberately pass an Integer.
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe rawSet = new ConcurrentNavigableSetNullSafe<>();
        rawSet.add("apple");
        rawSet.add("banana");
        assertThrows(ClassCastException.class, () -> rawSet.ceiling(123), "Passing an Integer to a String set should raise ClassCastException");
    }

    @Test
    void ceiling_IllegalArgumentException_CustomComparator() {
        // Comparator that throws when the sentinel value (Integer.MIN_VALUE) is compared.
        Comparator<Integer> throwingComparator = (a, b) -> {
            if (Objects.equals(a, Integer.MIN_VALUE) || Objects.equals(b, Integer.MIN_VALUE)) {
                throw new IllegalArgumentException("Sentinel value not allowed");
            }
            return Integer.compare(a, b);
        };
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>(throwingComparator);
        intSet.add(10);
        intSet.add(20);
        // The sentinel value is used internally to represent null; we invoke ceiling with it.
        assertThrows(IllegalArgumentException.class, () -> intSet.ceiling(Integer.MIN_VALUE), "Custom comparator should propagate IllegalArgumentException");
    }

    @Test
    void ceiling_NullPointerException_InternalSetNull() throws Exception {
        // Create a normal set and then null‑out its internalSet via reflection.
        ConcurrentNavigableSetNullSafe<String> vulnerableSet = new ConcurrentNavigableSetNullSafe<>();
        vulnerableSet.add("a");
        vulnerableSet.add("b");
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        // break the internal state
        internalSetField.set(vulnerableSet, null);
        assertThrows(NullPointerException.class, () -> vulnerableSet.ceiling("a"), "If internalSet is null, ceiling should throw NPE");
    }

    @Test
    void ceiling_ClassCastException_IntegerSetWrongArgument() {
        // Set is typed as Integer; we pass a String.
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe rawSet = new ConcurrentNavigableSetNullSafe<>();
        rawSet.add(1);
        rawSet.add(2);
        assertThrows(ClassCastException.class, () -> rawSet.ceiling("alpha"), "Passing a String to an Integer set should raise ClassCastException");
    }
}
