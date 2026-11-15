package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class CaseInsensitiveSet_contains_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private CaseInsensitiveSet<String> stringSet;

    /**
     * A set that stores non‑String elements to verify that {@code contains} falls back to normal equality.
     */
    private CaseInsensitiveSet<Object> mixedSet;

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor creates an empty, case‑insensitive set.
        stringSet = new CaseInsensitiveSet<>();
        // A mixed set to verify non‑String handling.
        mixedSet = new CaseInsensitiveSet<>();
        // Integer
        mixedSet.add(42);
        // Boolean
        mixedSet.add(true);
        // Character
        mixedSet.add('A');
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid accidental cross‑test contamination.
        stringSet.clear();
        mixedSet.clear();
    }

    static Stream<Arguments> positiveMatchProvider() {
        return Stream.of(Arguments.of("Apple"), Arguments.of("aPPle"));
    }

    static Stream<Arguments> negativeMatchProvider() {
        return Stream.of(Arguments.of("Banana"), // Integer (different type)
        Arguments.of(123));
    }

    static Stream<Arguments> classCastExceptionProvider() {
        return Stream.of(// Integer
        Arguments.of(42), // StringBuilder not Comparable<String>
        Arguments.of(new StringBuilder("Apple")), Arguments.of(new Comparable<Object>() {

            @Override
            public int compareTo(Object o) {
                return 0;
            }
        }));
    }

    @Test
    void contains_StringExactCase_ReturnsTrue() {
        // given
        stringSet.add("HelloWorld");
        // when
        boolean result = stringSet.contains("HelloWorld");
        // then
        assertTrue(result, "Set should contain the exact string that was added.");
    }

    @Test
    void contains_StringDifferentCase_ReturnsTrue() {
        // given
        stringSet.add("HelloWorld");
        // when
        // lower case
        boolean result = stringSet.contains("helloworld");
        // then
        assertTrue(result, "Set should treat strings case‑insensitively.");
    }

    @Test
    void contains_StringNotPresent_ReturnsFalse() {
        // given
        stringSet.add("Alpha");
        stringSet.add("Beta");
        // when
        boolean result = stringSet.contains("Gamma");
        // then
        assertFalse(result, "Set should return false for a string that was never added.");
    }

    @Test
    void contains_StringNullWhenNotPresent_ReturnsFalse() {
        // given: empty set (no null added)
        // when
        boolean result = stringSet.contains(null);
        // then
        assertFalse(result, "Empty set should not contain null.");
    }

    @Test
    void contains_StringNullWhenPresent_ReturnsTrue() {
        // given
        stringSet.add(null);
        // when
        boolean result = stringSet.contains(null);
        // then
        assertTrue(result, "Set should report true for a stored null element.");
    }

    @Test
    void contains_NonStringExactMatch_ReturnsTrue() {
        // given: mixedSet already contains Integer 42, Boolean true, Character 'A'
        // when
        boolean intResult = mixedSet.contains(42);
        boolean boolResult = mixedSet.contains(true);
        boolean charResult = mixedSet.contains('A');
        // then
        assertAll(() -> assertTrue(intResult, "Set should contain the Integer 42."), () -> assertTrue(boolResult, "Set should contain the Boolean true."), () -> assertTrue(charResult, "Set should contain the Character 'A'."));
    }

    @Test
    void contains_NonStringDifferentCaseIrrelevant_ReturnsFalse() {
        // given: mixedSet contains a Character 'A' (uppercase)
        // when
        // lower‑case char
        boolean result = mixedSet.contains('a');
        // then
        assertFalse(result, "Case‑insensitivity applies only to Strings; a different Character should not be considered equal.");
    }

    @Test
    void contains_NonStringNotPresent_ReturnsFalse() {
        // given: mixedSet does not contain a Double
        // when
        boolean result = mixedSet.contains(3.14);
        // then
        assertFalse(result, "Set should return false for a non‑String element that was never added.");
    }

    @Test
    void contains_InitializedFromCollection_CaseInsensitive() {
        // given
        Collection<String> source = java.util.List.of("One", "Two", "Three");
        CaseInsensitiveSet<String> fromCollection = new CaseInsensitiveSet<>(source);
        // when
        // mixed case
        boolean result = fromCollection.contains("tWo");
        // then
        assertTrue(result, "Set constructed from a collection should retain case‑insensitive behaviour.");
    }

    @Test
    void contains_InitializedFromEmptyCollection_ReturnsFalse() {
        // given
        CaseInsensitiveSet<String> emptyFromCollection = new CaseInsensitiveSet<>(Collections.emptyList());
        // when
        boolean result = emptyFromCollection.contains("anything");
        // then
        assertFalse(result, "Set built from an empty collection should not contain any element.");
    }

    @Test
    void contains_StringNullWhenNotPresent_ThrowsNullPointerException() {
        // given: empty set (no null added)
        // when / then
        assertThrows(NullPointerException.class, () -> stringSet.contains(null), "ConcurrentSkipListMap does not permit null keys – contains(null) must throw NPE.");
    }

    /**
     * Parameterized positive matches (exact & different case).
     */
    @ParameterizedTest(name = "{index} => query=''{0}''")
    @MethodSource("positiveMatchProvider")
    void contains_PositiveMatches(String query) {
        // given
        stringSet.add("Apple");
        // when
        boolean result = stringSet.contains(query);
        // then
        assertTrue(result, "Set should contain the string '" + query + "' (case‑insensitive).");
    }

    /**
     * Parameterized negative matches (absent string & wrong type).
     */
    @ParameterizedTest(name = "{index} => query=''{0}''")
    @MethodSource("negativeMatchProvider")
    void contains_NegativeMatches(Object query) {
        // given
        stringSet.add("Apple");
        // when / then
        assertFalse(stringSet.contains(query), "Set should not contain the element '" + query + "'.");
    }

    /**
     * Empty‑set behaviour – any query must return false.
     */
    @ParameterizedTest(name = "{index} => query=''{0}''")
    @ValueSource(strings = { "Anything", "apple", "" })
    void contains_EmptySetAlwaysFalse(String query) {
        // given: stringSet is empty
        // when
        boolean result = stringSet.contains(query);
        // then
        assertFalse(result, "Empty set must not contain any element, including '" + query + "'.");
    }

    /**
     * Duplicate case insertion – size must stay 1 and contains works for all cases.
     */
    @Test
    void contains_DuplicateCaseEntries() {
        // given
        boolean firstAdd = stringSet.add("Apple");
        // duplicate, different case
        boolean secondAdd = stringSet.add("apple");
        // another duplicate
        boolean thirdAdd = stringSet.add("APPLE");
        // when
        boolean containsLower = stringSet.contains("apple");
        boolean containsUpper = stringSet.contains("APPLE");
        int size = stringSet.size();
        // then
        assertAll(() -> assertTrue(firstAdd, "First insertion should succeed."), () -> assertFalse(secondAdd, "Duplicate (case‑variant) insertion must not create a new entry."), () -> assertFalse(thirdAdd, "Another duplicate must also be ignored."), () -> assertTrue(containsLower, "Set must contain lower‑case variant."), () -> assertTrue(containsUpper, "Set must contain upper‑case variant."), () -> assertEquals(1, size, "Set size must be 1 after duplicate case insertions."));
    }

    /**
     * Non‑String object queries that are not stored – should return false.
     */
    @Test
    void contains_NonStringObjectQuery_ReturnsFalse() {
        // given
        stringSet.add("Apple");
        // when
        boolean sbResult = stringSet.contains(new StringBuilder("Apple"));
        boolean customResult = stringSet.contains(new Object() {

            @Override
            public String toString() {
                return "Apple";
            }
        });
        // then
        assertAll(() -> assertFalse(sbResult, "StringBuilder should not be considered equal to stored String."), () -> assertFalse(customResult, "Object with matching toString() must not be considered equal."));
    }

    /**
     * Whitespace handling – empty string and strings consisting solely of spaces.
     */
    @Test
    void contains_WhitespaceAndEmptyString() {
        // given
        stringSet.add("");
        // three spaces
        stringSet.add("   ");
        // when / then
        assertAll(() -> assertTrue(stringSet.contains(""), "Empty string that was added must be found."), () -> assertTrue(stringSet.contains("   "), "Three‑space string must be found."), () -> assertFalse(stringSet.contains("  "), "Two‑space string should not match three‑space entry."));
    }

    /**
     * Unicode case variations – Turkish dotted I and its lower‑case forms.
     */
    @Test
    void contains_UnicodeCaseVariations() {
        // given
        // capital dotted I
        String turkish = "İstanbul";
        stringSet.add(turkish);
        // when
        boolean exact = stringSet.contains("İstanbul");
        // lower‑case with combining dot
        boolean lower = stringSet.contains("i̇stanbul");
        // then
        assertAll(() -> assertTrue(exact, "Exact Unicode string must be found."), () -> assertTrue(lower, "Unicode case‑insensitive match must be found."));
    }

    /**
     * After removal – the element must no longer be reported as present.
     */
    @Test
    void contains_AfterRemoval_ReturnsFalse() {
        // given
        stringSet.add("Apple");
        boolean removed = stringSet.remove("Apple");
        assertTrue(removed, "Removal should succeed.");
        // when
        boolean result = stringSet.contains("Apple");
        // then
        assertFalse(result, "After removal, contains should return false.");
    }

    /**
     * Large string handling – 1000‑character string.
     */
    @Test
    void contains_LargeStringHandling() {
        // given
        String large = "a".repeat(1000);
        stringSet.add(large);
        // when
        boolean result = stringSet.contains(large);
        // then
        assertTrue(result, "Set must locate a very large string key.");
    }

    /**
     * ClassCastException scenarios when the map cannot compare heterogeneous key types.
     */
    @ParameterizedTest(name = "{index} => query=''{0}'' expects ClassCastException")
    @MethodSource("classCastExceptionProvider")
    void contains_ClassCastExceptionScenarios(Object query) {
        // given
        // String key ensures map expects comparable Strings
        stringSet.add("Apple");
        // when / then
        Executable exec = () -> stringSet.contains(query);
        assertThrows(ClassCastException.class, exec, "contains(" + query + ") should throw ClassCastException due to incompatible key types.");
    }
}
