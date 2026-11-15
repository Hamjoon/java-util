package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class StringUtilities_trimLength_Test {

    /**
     * Supplies the data for {@link #trimLength_basicScenarios}.
     *
     * The data mirrors the JSON supplied in the task description.
     *
     * @return a stream of arguments: (input, expectedLength, description)
     */
    static Stream<Arguments> trimLengthBasicScenarios() {
        return Stream.of(// null input → 0
        arguments(null, 0, "When the input is null, trimToEmpty should treat it as an empty string, so trimLength returns 0."), // empty string → 0
        arguments("", 0, "An empty string has length 0; trimToEmpty returns the same empty string, so trimLength is 0."), // whitespace only → 0
        arguments("   ", 0, "A string containing only whitespace is trimmed to an empty string; length after trimming is 0."), // leading/trailing spaces → trimmed length 3 ("abc")
        arguments(" abc ", 3, "Leading and trailing spaces are removed; the remaining text \"abc\" has length 3."), // no surrounding whitespace → length 3
        arguments("abc", 3, "A string without surrounding whitespace is unchanged; length is 3."), // mixed whitespace characters → trimmed length 3 ("abc")
        arguments("\t\nabc\r\n", 3, "Whitespace characters (tab, newline, carriage‑return) are trimmed; remaining \"abc\" length is 3."), // internal spaces preserved → trimmed length 5 ("a b c")
        arguments("  a b c  ", 5, "Internal spaces are preserved after trimming; the trimmed string \"a b c\" has length 5."));
    }

    @Test
    void trimLength_nullInput_returnsZero() {
        // given
        String input = null;
        // when
        int result = StringUtilities.trimLength(input);
        // then
        assertEquals(0, result, "trimLength(null) should return 0 because trimToEmpty(null) yields an empty string");
    }

    @Test
    void trimLength_emptyString_returnsZero() {
        // given
        String input = "";
        // when
        int result = StringUtilities.trimLength(input);
        // then
        assertEquals(0, result, "trimLength(\"\") should return 0 because the string is already empty");
    }

    @Test
    void trimLength_whitespaceOnly_returnsZero() {
        // given
        String input = "   \t\n  ";
        // when
        int result = StringUtilities.trimLength(input);
        // then
        assertEquals(0, result, "trimLength(whitespace) should return 0 after trimming to an empty string");
    }

    @Test
    void trimLength_regularString_returnsTrimmedLength() {
        // given
        String input = "  Hello World  ";
        // when
        int result = StringUtilities.trimLength(input);
        // then
        // "Hello World".length() == 11
        assertEquals(11, result, "trimLength should return the length of the trimmed string");
    }

    @Test
    void trimLength_stringWithNoLeadingOrTrailingSpace_returnsOriginalLength() {
        // given
        String input = "NoSpace";
        // when
        int result = StringUtilities.trimLength(input);
        // then
        assertEquals(input.length(), result, "trimLength should return the original length when no trimming is required");
    }

    /**
     * Parameterized test that iterates over all basic trimming scenarios.
     *
     * @param input       the string supplied to {@code trimLength}
     * @param expected    the expected trimmed length
     * @param description a short description for the assertion message
     */
    @ParameterizedTest(name = "{index}: trimLength({0}) = {1} → {2}")
    @MethodSource("trimLengthBasicScenarios")
    void trimLength_basicScenarios(String input, int expected, String description) {
        // when
        int actual = StringUtilities.trimLength(input);
        // then
        assertEquals(expected, actual, description);
    }
}
