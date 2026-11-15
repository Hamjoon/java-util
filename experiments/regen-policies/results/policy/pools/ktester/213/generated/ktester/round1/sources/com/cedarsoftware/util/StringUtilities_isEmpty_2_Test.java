package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_isEmpty_2_Test {

    /**
     * Strings that consist solely of whitespace characters.
     */
    private static Stream<String> stringsWithAllWhitespace() {
        return Stream.of(" ", "\t", "\n", "\r\n", " \t \n\r ", // EM SPACE
        "\u2003", // IDEOGRAPHIC SPACE
        "\u3000");
    }

    /**
     * Strings that contain at least one non‑whitespace character.
     */
    private static Stream<String> stringsWithContentOtherThanWhitespace() {
        return Stream.of("a", " a ", "text", "0", "null", "foo\nbar", " \u00A0 ");
    }

    /**
     * Strings that consist solely of whitespace characters – expected result: true.
     */
    private static Stream<Arguments> pureWhitespaceStrings() {
        return Stream.of(Arguments.of(" ", true), Arguments.of("\t\n\r\f", true), Arguments.of("\u00A0", true), Arguments.of("\u2007", true), Arguments.of("\u202F", true), Arguments.of("\u2028", true), Arguments.of("\u2029", true), Arguments.of("\u200B", false));
    }

    /**
     * Strings that contain at least one non‑whitespace character – expected result: false.
     */
    private static Stream<Arguments> nonWhitespaceStrings() {
        return Stream.of(Arguments.of("a", false), Arguments.of("abc", false), Arguments.of(" a ", false), Arguments.of("Hello\u00A0World", false), Arguments.of("\u200Btest", false));
    }

//     @ParameterizedTest
//     @MethodSource("stringsWithAllWhitespace")
//     void testIsEmpty_whenStringHasOnlyWhitespace_returnsTrue(String s) {
//         assertTrue("Expected isEmpty to return true for whitespace‑only string: [" + s + "]", StringUtilities.isEmpty(s));
//     }

//     @ParameterizedTest
//     @MethodSource("stringsWithContentOtherThanWhitespace")
//     void testIsEmpty_whenStringHasContent_returnsFalse(String s) {
//         assertFalse("Expected isEmpty to return false for string with content: [" + s + "]", StringUtilities.isEmpty(s));
//     }

//     @ParameterizedTest
//     @NullAndEmptySource
//     void testIsEmpty_whenNullOrEmpty_returnsTrue(String s) {
//         // The parameter s will be null or empty.
//         assertTrue("Expected isEmpty to return true for null or empty string", StringUtilities.isEmpty(s));
//     }

//     @ParameterizedTest
//     @MethodSource("pureWhitespaceStrings")
//     void testIsEmpty_whenStringContainsOnlyWhitespace(String input, boolean expected) {
//         assertEquals("Unexpected result for input [" + input + "]", expected, StringUtilities.isEmpty(input));
//     }

//     @Test
//     void testIsEmpty_whenStringContainsNonWhitespace() {
//         nonWhitespaceStrings().forEach(args -> {
//             Object[] pair = args.get();
//             String input = (String) pair[0];
//             boolean expected = (Boolean) pair[1];
//             assertEquals("Unexpected result for input [" + input + "]", expected, StringUtilities.isEmpty(input));
//         });
//     }
}