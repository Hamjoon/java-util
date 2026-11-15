package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCharacter_Test {

    /**
     * Mocked Converter – the method does not use it, but it must be supplied.
     */
    @Mock
    private static Converter mockConverter;

    /**
     * Supplies all test cases for {@link #testToCharacter(Object, Object, String)}.
     *
     * <p>Each {@link Arguments} consists of:
     * <ul>
     *   <li>the {@code from} value (String, null, or other Object)</li>
     *   <li>the expected outcome – either a {@link Character} or an exception {@link Class}</li>
     *   <li>a human‑readable description (used as the parameterized test name)</li>
     * </ul>
     */
    private static Stream<Arguments> provideToCharacterCases() {
        return Stream.of(// ---- Successful conversions -------------------------------------------------
        Arguments.of("", '\u0000', "Empty string should return the null character"), Arguments.of("A", 'A', "Single‑character string returns the same character"), Arguments.of("65", 'A', "Numeric string \"65\" → 'A'"), Arguments.of("  97  ", 'a', "Numeric string with surrounding whitespace"), Arguments.of("  66  ", 'B', "Whitespace‑trimmed numeric string \"  66  \" → 'B'"), Arguments.of("0", '\u0000', "Numeric string \"0\" → null character"), Arguments.of("00065", 'A', "Leading zeros are ignored when parsing numeric string"), Arguments.of("65535", '\uFFFF', "Maximum char value (65535) parsed from digits"), Arguments.of("\\u0041", 'A', "Valid Unicode escape \"\\\\u0041\" → 'A'"), Arguments.of("\\u00a9", '©', "Unicode escape with lower‑case hex digits"), Arguments.of("\\u0042", 'B', "Another valid Unicode escape for 'B'"), // ---- Expected failure scenarios --------------------------------------------
        Arguments.of("9999999999", IllegalArgumentException.class, "Digits exceeding Integer.MAX_VALUE cause IllegalArgumentException"), Arguments.of("-1", IllegalArgumentException.class, "Negative numeric string is not all‑digits and fails Unicode parsing"), Arguments.of("+65", IllegalArgumentException.class, "Leading plus sign fails all‑digits check and results in IllegalArgumentException"), Arguments.of("AB", IllegalArgumentException.class, "Multi‑character non‑digit string is not a Unicode escape"), Arguments.of(null, NullPointerException.class, "Null source object should trigger NullPointerException"), Arguments.of(5, ClassCastException.class, "Non‑String source (Integer) should trigger ClassCastException"), Arguments.of("\\u00G1", IllegalArgumentException.class, "Unicode escape containing non‑hex character 'G'"), Arguments.of("u0041", IllegalArgumentException.class, "Missing leading backslash makes the string an invalid Unicode escape"), Arguments.of("\\u041", IllegalArgumentException.class, "Unicode escape with only three hex digits is invalid"), Arguments.of("\\u0041XYZ", IllegalArgumentException.class, "Unicode escape followed by extra characters is invalid"), Arguments.of("\\u", IllegalArgumentException.class, "Incomplete Unicode escape (only \"\\\\u\")"), Arguments.of("\\u12345", IllegalArgumentException.class, "Unicode escape with five hex digits is invalid"), Arguments.of("\\u12G4", IllegalArgumentException.class, "Unicode escape with non‑hex character 'G'"), Arguments.of("\\u0041\\u0042", IllegalArgumentException.class, "String containing multiple Unicode escapes is invalid"), Arguments.of("\\U0041", IllegalArgumentException.class, "Uppercase 'U' in escape sequence is invalid"), Arguments.of("\\u0041 ", IllegalArgumentException.class, "Unicode escape followed by trailing space is invalid"), Arguments.of("\\u 0041", IllegalArgumentException.class, "Space between \"\\\\u\" and hex digits makes the escape invalid"));
    }

    /**
     * Empty string should return the null‑character (char value 0).
     */
    @Test
    void emptyStringReturnsNullChar() {
        char result = StringConversions.toCharacter("", mockConverter);
        assertEquals('\u0000', result, "Empty string must produce the null character");
    }

    /**
     * A single‑character string should be returned unchanged.
     */
    @Test
    void singleCharacterString() {
        char result = StringConversions.toCharacter("Z", mockConverter);
        assertEquals('Z', result);
    }

    /**
     * A numeric string should be parsed as an integer and cast to a char.
     */
    @Test
    void numericStringConvertedToChar() {
        // "65" -> 'A'
        char result = StringConversions.toCharacter("65", mockConverter);
        assertEquals('A', result);
    }

    /**
     * A numeric string with surrounding whitespace should still be parsed correctly.
     */
    @Test
    void numericStringWithWhitespace() {
        char result = StringConversions.toCharacter("  97  ", mockConverter);
        assertEquals('a', result);
    }

    /**
     * A non‑digit, non‑single‑character string that is not a valid Unicode escape
     *  should cause an IllegalArgumentException.
     */
    @Test
    void invalidUnicodeEscapeThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toCharacter("AB", mockConverter), "Invalid Unicode escape should result in IllegalArgumentException");
    }

    /**
     * A proper Unicode escape sequence (e.g. \"\\u0041\") should be parsed to the correct character.
     */
    @Test
    void unicodeEscapeParsedCorrectly() {
        // The method expects the literal backslash‑u form.
        // '\\u0042' -> 'B'
        char result = StringConversions.toCharacter("\\u0042", mockConverter);
        assertEquals('B', result);
    }

    /**
     * Parameterized test that drives all scenarios (valid conversions and expected exceptions).
     *
     * @param from        the input value passed to {@code toCharacter}
     * @param expected    either a {@link Character} (for successful conversion) or a
     *                    {@link Class}&lt;? extends {@link Throwable}&gt; (for expected failure)
     * @param description textual description of the test case (used as the display name)
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideToCharacterCases")
    void testToCharacter(Object from, Object expected, String description) {
        if (expected instanceof Class) {
            // Expected an exception – verify that the correct type is thrown.
            assertThrows((Class<? extends Throwable>) expected, () -> StringConversions.toCharacter(from, mockConverter), description);
        } else {
            // Expected a successful conversion – verify the returned character.
            char result = StringConversions.toCharacter(from, mockConverter);
            assertEquals(((Character) expected).charValue(), result, description);
        }
    }
}
