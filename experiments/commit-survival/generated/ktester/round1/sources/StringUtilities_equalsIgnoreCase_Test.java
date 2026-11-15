package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equalsIgnoreCase_Test {

    /**
     * Provides all test cases described in the JSON specification.
     *
     * Each {@link Arguments} instance contains:
     *   1. {@code CharSequence cs1}
     *   2. {@code CharSequence cs2}
     *   3. {@code boolean expectedResult}
     *   4. {@code String description} – used as the test name / assertion message.
     */
    static Stream<Arguments> provideEqualsIgnoreCaseCases() {
        // Reference equality case where both arguments refer to the same String instance
        String sameString = "Test";
        // Reference equality with a mutable CharSequence (StringBuilder)
        CharSequence mutableBuilder = new StringBuilder("abc");
        // Custom CharSequence implementation (anonymous class)
        CharSequence customSeq = new CharSequence() {

            private final String data = "abc";

            @Override
            public int length() {
                return data.length();
            }

            @Override
            public char charAt(int index) {
                return data.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return data.subSequence(start, end);
            }

            @Override
            public String toString() {
                return data;
            }
        };
        return Stream.of(// 1. Reference equality – same String instance
        Arguments.of(sameString, sameString, true, "Reference equality: both parameters refer to the same String instance"), // 2. One argument null, the other non‑null
        Arguments.of(null, "abc", false, "One argument is null while the other is non‑null"), // 3. Both arguments null
        Arguments.of(null, null, true, "Both arguments are null; they are the same reference"), // 4. Different lengths
        Arguments.of("abc", "abcd", false, "Arguments have different lengths, causing early false"), // 5. Same length, case‑insensitive match
        Arguments.of("AbCdE", "aBcDe", true, "Same length strings with characters differing only by case"), // 6. Same length, at least one character differs
        Arguments.of("AbCdE", "aBcDf", false, "Same length strings but at least one character differs (ignoring case)"), // 7. Different CharSequence implementations (StringBuilder vs StringBuffer)
        Arguments.of(new StringBuilder("Hello"), new StringBuffer("heLLo"), true, "Non‑String CharSequence implementations with equal content ignoring case"), // 8. Custom CharSequence vs String
        Arguments.of(customSeq, "ABC", true, "Custom CharSequence implementation compared with a String"), // 9. Reference equality with a mutable CharSequence
        Arguments.of(mutableBuilder, mutableBuilder, true, "Reference equality with a mutable CharSequence implementation (StringBuilder)"), // 10. Both empty strings
        Arguments.of("", "", true, "Both arguments are empty strings"), // 11. Empty vs non‑empty
        Arguments.of("", "a", false, "One argument is empty while the other is not"), // 12. Greek sigma (case‑insensitive Unicode)
        Arguments.of("Σ", "σ", true, "Greek capital sigma and small sigma differ only by case"), // 13. Emoji (supplementary Unicode character)
        Arguments.of("😀", new StringBuilder("😀"), true, "Both arguments contain the same supplementary Unicode character (emoji)"), // 14. Embedded null character
        Arguments.of("\u0000ABC", "\u0000abc", true, "Strings start with a null character followed by letters differing only by case"), // 15. Same length CharSequence implementations where the last character differs
        Arguments.of(new StringBuilder("Java"), new StringBuffer("Javb"), false, "Same length CharSequence implementations where the last character differs"));
    }

    @Test
    void whenBothReferencesAreIdentical_thenTrue() {
        CharSequence cs = "SampleText";
        assertTrue(StringUtilities.equalsIgnoreCase(cs, cs), "Identical references should be considered equal");
    }

    @Test
    void whenBothAreNull_thenTrue() {
        assertTrue(StringUtilities.equalsIgnoreCase(null, null), "Two null CharSequences should be considered equal");
    }

    @Test
    void whenOneIsNull_thenFalse() {
        assertFalse(StringUtilities.equalsIgnoreCase(null, "notnull"), "Null compared with non‑null should be false");
        assertFalse(StringUtilities.equalsIgnoreCase("notnull", null), "Non‑null compared with null should be false");
    }

    @Test
    void whenLengthsDiffer_thenFalse() {
        CharSequence cs1 = "short";
        CharSequence cs2 = "longer";
        assertFalse(StringUtilities.equalsIgnoreCase(cs1, cs2), "Different lengths must result in false");
    }

    @Test
    void whenSameContentDifferentCase_thenTrue() {
        CharSequence cs1 = "CaseInsensitive";
        CharSequence cs2 = "cAsEiNsEnSiTiVe";
        assertTrue(StringUtilities.equalsIgnoreCase(cs1, cs2), "Same characters ignoring case should be true");
    }

    @Test
    void whenContentDiffersAtOnePosition_thenFalse() {
        CharSequence cs1 = "abcdefg";
        // 'd' replaced with 'x'
        CharSequence cs2 = "abcxefg";
        assertFalse(StringUtilities.equalsIgnoreCase(cs1, cs2), "A single differing character should cause false");
    }

    @Test
    void whenBothEmptyStrings_thenTrue() {
        CharSequence cs1 = "";
        CharSequence cs2 = "";
        assertTrue(StringUtilities.equalsIgnoreCase(cs1, cs2), "Two empty CharSequences should be equal");
    }

    @Test
    void whenEmptyAndNonEmpty_thenFalse() {
        CharSequence cs1 = "";
        CharSequence cs2 = "nonempty";
        assertFalse(StringUtilities.equalsIgnoreCase(cs1, cs2), "Empty vs non‑empty must be false");
    }

    @Test
    void whenUsingStringImplementations_thenTrue() {
        // Using concrete String objects (a subclass of CharSequence)
        String s1 = "MiXeD CaSe";
        String s2 = "mixed case";
        assertTrue(StringUtilities.equalsIgnoreCase(s1, s2), "String overload should delegate correctly to CharSequence version");
    }

    @Test
    void whenUsingStringBuilderImplementations_thenTrue() {
        StringBuilder sb1 = new StringBuilder("BuilderTest");
        StringBuilder sb2 = new StringBuilder("buildertest");
        assertTrue(StringUtilities.equalsIgnoreCase(sb1, sb2), "StringBuilder instances with same characters ignoring case should be true");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideEqualsIgnoreCaseCases")
    void testEqualsIgnoreCaseParameterized(CharSequence cs1, CharSequence cs2, boolean expected, String description) {
        boolean actual = StringUtilities.equalsIgnoreCase(cs1, cs2);
        assertEquals(expected, actual, () -> "Failed case: " + description + " – expected " + expected + " but got " + actual);
    }
}
