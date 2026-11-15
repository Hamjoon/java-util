package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toCharacter_Test {

    private static Converter converter;

    private static final class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final Object expected;

        final String description;

        ArgumentsHolder(Object from, Converter converter, Object expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        // JUnit 5 requires a method that returns an array of arguments.
        // The static method `toCharacterCases` unwraps these holders.
        Object[] toArray() {
            return new Object[] { from, converter, expected, description };
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up globally.
        converter = null;
    }

    private static Stream<ArgumentsHolder> toCharacterCases() {
        return Stream.of(// group: StringConversions.toCharacter tests
        new ArgumentsHolder("", null, '\u0000', "Empty string should return the null character (char value 0)."), new ArgumentsHolder("x", null, 'x', "String of length 1 should return the sole character."), new ArgumentsHolder("65", null, 'A', "All‑digit string parsed as integer 65, which corresponds to character 'A'."), new ArgumentsHolder("  66  ", null, 'B', "All‑digit string with surrounding whitespace is trimmed, parsed as 66 → 'B'."), new ArgumentsHolder("9999999999", null, IllegalArgumentException.class, "Numeric value exceeds int range; Integer.parseInt throws NumberFormatException which is wrapped in IllegalArgumentException."), new ArgumentsHolder("12a", null, IllegalArgumentException.class, "String contains non‑digit characters, so allDigits matcher fails; parseUnicodeEscape receives an invalid format and throws IllegalArgumentException."), new ArgumentsHolder("\\u0041", null, 'A', "Valid Unicode escape sequence '\\u0041' is parsed by parseUnicodeEscape and returns character 'A'."), new ArgumentsHolder("\\u00G1", null, IllegalArgumentException.class, "Invalid Unicode escape (non‑hex digit) causes parseUnicodeEscape to throw IllegalArgumentException."), // group: StringConversions.toCharacter additional tests
        new ArgumentsHolder(null, null, NullPointerException.class, "Passing a null reference as the source object should cause a NullPointerException."), new ArgumentsHolder("ab", null, IllegalArgumentException.class, "A multi‑character string that is not all digits and does not represent a valid Unicode escape should trigger IllegalArgumentException."), new ArgumentsHolder("+65", null, IllegalArgumentException.class, "The all‑digits pattern does not accept a leading plus sign, so the string falls through to parseUnicodeEscape which throws IllegalArgumentException."), new ArgumentsHolder("007", null, '\u0007', "All‑digit string with leading zeros is parsed as integer 7, which corresponds to the control character BEL (U+0007)."), new ArgumentsHolder("\\u0041extra", null, IllegalArgumentException.class, "String starts with a Unicode escape but contains trailing characters; parseUnicodeEscape rejects the format and throws IllegalArgumentException."), new ArgumentsHolder("\\n", null, IllegalArgumentException.class, "A backslash‑escaped newline is not a valid \\uXXXX escape, leading to IllegalArgumentException."), new ArgumentsHolder("70000", null, '\u1170', "Numeric value 70000 exceeds the Unicode range; casting to char truncates to the low 16 bits, resulting in character U+1170."), new ArgumentsHolder("   ", null, IllegalArgumentException.class, "A string containing only whitespace is not empty, not all digits, and not a valid Unicode escape, so parseUnicodeEscape throws IllegalArgumentException."), new ArgumentsHolder("\\u0062", null, 'b', "Valid Unicode escape for lowercase 'b' (U+0062) is correctly parsed by parseUnicodeEscape."), // group: StringConversions.toCharacter uncovered exception tests
        new ArgumentsHolder(123, null, ClassCastException.class, "Non‑String source (Integer) causes a ClassCastException when the method casts the argument to String."), new ArgumentsHolder("\\u123", null, IllegalArgumentException.class, "Unicode escape sequence with only three hex digits is malformed; parseUnicodeEscape throws IllegalArgumentException."), new ArgumentsHolder("\\u", null, IllegalArgumentException.class, "Unicode escape consisting of only the '\\u' prefix (no hex digits) is invalid; parseUnicodeEscape throws IllegalArgumentException."), new ArgumentsHolder("-65", null, IllegalArgumentException.class, "Negative numeric string does not match the all‑digits pattern; it falls through to parseUnicodeEscape, which rejects the format and throws IllegalArgumentException."));
    }

    @Test
    void toCharacter_EmptyString_ReturnsNullChar() {
        char result = StringConversions.toCharacter("", converter);
        assertEquals('\u0000', result, "Empty string should be converted to the null character");
    }

    @Test
    void toCharacter_SingleCharacterString_ReturnsSameChar() {
        char result = StringConversions.toCharacter("Z", converter);
        assertEquals('Z', result, "Single‑character string should be converted to that character");
    }

    @Test
    void toCharacter_NumericString_ReturnsCorrespondingChar() {
        // "65" is the ASCII code for 'A'
        char result = StringConversions.toCharacter("65", converter);
        assertEquals('A', result, "Numeric string \"65\" should be converted to character 'A'");
    }

    @Test
    void toCharacter_NumericStringWithWhitespace_ReturnsCorrespondingChar() {
        // Leading/trailing whitespace should be ignored by Integer.parseInt after trim()
        char result = StringConversions.toCharacter("  66  ", converter);
        assertEquals('B', result, "Numeric string with whitespace should be parsed correctly");
    }

    @Test
    void toCharacter_InvalidNumericString_ThrowsIllegalArgumentException() {
        // A numeric string that cannot be parsed as an int (e.g., too large) should cause an IAE
        assertThrows(IllegalArgumentException.class, () -> {
            StringConversions.toCharacter("9999999999", converter);
        });
    }

    @Test
    void toCharacter_UnicodeEscape_ReturnsCorrectChar() {
        // The method should interpret a Java‑style Unicode escape (e.g., "\u0043") as 'C'
        char result = StringConversions.toCharacter("\\u0043", converter);
        assertEquals('C', result, "Unicode escape \"\\u0043\" should be converted to character 'C'");
    }

    @Test
    void toCharacter_NonDigitNonUnicodeString_ThrowsIllegalArgumentException() {
        // A string that is neither a single character, all digits, nor a valid unicode escape
        // should cause the private parseUnicodeEscape to throw an IAE.
        assertThrows(IllegalArgumentException.class, () -> {
            StringConversions.toCharacter("not-a-digit-or-unicode", converter);
        });
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("toCharacterCases")
    void toCharacter_VariousInputs(Object from, Converter conv, Object expected, String description) {
        // Use the supplied converter if non‑null; otherwise fall back to the default instance.
        Converter usedConverter = conv != null ? conv : converter;
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception.
            assertThrows((Class<? extends Throwable>) expected, () -> StringConversions.toCharacter(from, usedConverter), description);
        } else {
            // Expected a character result.
            char actual = StringConversions.toCharacter(from, usedConverter);
            assertEquals(expected, actual, description);
        }
    }
}
