package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_hashCodeIgnoreCase_Test {

    /**
     * Supplies the test data for {@link #hashCodeIgnoreCase_ExactValues(String, int)}.
     *
     * Each {@link Arguments} entry corresponds to one JSON test case:
     * <ul>
     *   <li>first argument – the string to hash (may be {@code null})</li>
     *   <li>second argument – the expected hash code</li>
     * </ul>
     */
    static Stream<Arguments> hashCodeIgnoreCaseProvider() {
        return Stream.of(// Null and empty inputs
        Arguments.of(null, 0), Arguments.of("", 0), // Simple ASCII strings
        Arguments.of("abc", 96354), Arguments.of("AbC", 96354), Arguments.of("ABC", 96354), // Mixed‑case longer string
        Arguments.of("aBcDeF", 640244218), // Digits and symbols (toLowerCase does not affect them)
        Arguments.of("123!@#", 166508119), // Locale‑specific uppercase character (İ) – should be lowered to 'i'
        Arguments.of("İstanbul", 1401103605), // German sharp s – remains unchanged
        Arguments.of("ß", 115), // Characters with multi‑code‑point case mappings
        Arguments.of("ǅǈǋ", 3068777), // Supplementary Unicode characters (outside BMP)
        Arguments.of("𐍈𐍉", 1596276), // Combining diacritical mark (a + grave accent)
        Arguments.of("à", 3175));
    }

    @Test
    @DisplayName("hashCodeIgnoreCase returns 0 for null input")
    void hashCodeIgnoreCase_NullInput_ReturnsZero() {
        // given
        String input = null;
        // when
        int result = StringUtilities.hashCodeIgnoreCase(input);
        // then
        assertEquals(0, result, "Expected hash code of null to be 0");
    }

    @Test
    @DisplayName("hashCodeIgnoreCase returns correct hash for empty string")
    void hashCodeIgnoreCase_EmptyString_ReturnsZero() {
        // given
        String input = "";
        // when
        int result = StringUtilities.hashCodeIgnoreCase(input);
        // then
        // The algorithm starts with hash = 0 and never modifies it for an empty string
        assertEquals(0, result, "Expected hash code of empty string to be 0");
    }

    @Test
    @DisplayName("hashCodeIgnoreCase is case‑insensitive")
    void hashCodeIgnoreCase_DifferentCases_SameHash() {
        // given
        String lower = "testString";
        String upper = "TESTSTRING";
        // when
        int hashLower = StringUtilities.hashCodeIgnoreCase(lower);
        int hashUpper = StringUtilities.hashCodeIgnoreCase(upper);
        // then
        assertEquals(hashLower, hashUpper, "hashCodeIgnoreCase should produce the same value for strings that differ only by case");
    }

    @Test
    @DisplayName("hashCodeIgnoreCase matches Java's String.hashCode when all chars are lower‑case")
    void hashCodeIgnoreCase_CompareWithStringHashCode_ForLowerCase() {
        // given
        String input = "aBcDeFgHiJkLmNoPqRsTuVwXyZ1234567890";
        // when
        int hashIgnoreCase = StringUtilities.hashCodeIgnoreCase(input);
        // compute expected using standard String.hashCode on the lower‑cased version
        int expected = input.toLowerCase().hashCode();
        // then
        assertEquals(expected, hashIgnoreCase, "hashCodeIgnoreCase should equal hashCode of the lower‑cased string");
    }

    @Test
    @DisplayName("hashCodeIgnoreCase produces deterministic results")
    void hashCodeIgnoreCase_Deterministic() {
        // given
        String input = "DeterministicCheck";
        // when
        int first = StringUtilities.hashCodeIgnoreCase(input);
        int second = StringUtilities.hashCodeIgnoreCase(input);
        int third = StringUtilities.hashCodeIgnoreCase(input);
        // then
        assertAll(() -> assertEquals(first, second, "First and second calls should match"), () -> assertEquals(second, third, "Second and third calls should match"));
    }

    @Test
    @DisplayName("hashCodeIgnoreCase handles Unicode characters correctly")
    void hashCodeIgnoreCase_UnicodeCharacters() {
        // given
        String mixed = "ÅßçĐëƒĞĥİĵķŁ";
        String lower = mixed.toLowerCase();
        // when
        int hashMixed = StringUtilities.hashCodeIgnoreCase(mixed);
        int hashLower = StringUtilities.hashCodeIgnoreCase(lower);
        // then
        assertEquals(hashLower, hashMixed, "Unicode characters should be lower‑cased before hashing, yielding identical results");
    }

    /**
     * Parameterized test that validates the exact hash values for a wide range of inputs,
     * including edge‑cases such as locale‑specific characters, surrogate pairs and combining marks.
     *
     * The data originates from the supplied JSON test definition.
     */
    @ParameterizedTest(name = "hashCodeIgnoreCase(''{0}'') = {1}")
    @MethodSource("hashCodeIgnoreCaseProvider")
    void hashCodeIgnoreCase_ExactValues(String input, int expected) {
        // when
        int actual = StringUtilities.hashCodeIgnoreCase(input);
        // then
        assertEquals(expected, actual, () -> "Unexpected hash for input [" + (input == null ? "null" : input) + "]");
    }
}
