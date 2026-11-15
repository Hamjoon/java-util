package com.cedarsoftware.util;

class StringUtilities_length_2_Test {

    /**
     * Provides a stream of arguments – each consists of an input string and the expected length
     * (measured in UTF‑16 code units, i.e. the value returned by {@link String#length()}).
     */
//     private static Stream<Arguments> provideStringsForLength() {
//         return Stream.of(// 1. Empty string
//         Arguments.of("", 0), // 2. Simple ASCII
//         Arguments.of("abc", 3), // 3. Japanese Hiragana – each character is a single UTF‑16 code unit
//         Arguments.of("こんにちは", 5), // 4. Woman technologist emoji (woman + zero‑width joiner + laptop)
//         Arguments.of("\uD83D\uDC69\u200D\uD83D\uDCBB", 4), // 5. Mixed control characters
//         Arguments.of("a\nb\tc\r", 6), // 6. Longer sentence
//         Arguments.of("Lorem ipsum dolor sit amet, consectetur adipiscing elit.", 56), // 7. Single supplementary emoji (grinning face) – surrogate pair → length 2
//         Arguments.of("😀", 2), // 8. Letter 'e' + combining acute accent (U+0301) → length 2
//         Arguments.of("e\u0301", 2), // 9. Null character (U+0000) – counted like any other char
//         Arguments.of("\u0000", 1), // 10. Whitespace only (space, tab, line feed, carriage return)
//         Arguments.of(" \t\n\r", 4), // 11. Mixed BMP and supplementary characters: a + emoji + b
//         Arguments.of("a😀b", 4), // 12. Lone high‑surrogate code unit – still a single char in Java
//         Arguments.of("\uD800", 1), // 13. Very long string (10 000 'a' characters) generated programmatically
//         Arguments.of(generateRepeatedString('a', 10_000), 10_000));
//     }

    /**
     * Helper that creates a string consisting of {@code count} repetitions of {@code ch}.
     */
    private static String generateRepeatedString(char ch, int count) {
        char[] arr = new char[count];
        java.util.Arrays.fill(arr, ch);
        return new String(arr);
    }

    /**
     * Escapes non‑printable characters for a readable assertion message.
     */
    private static String escapeForMessage(String s) {
        return s.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t").replace("\b", "\\b").replace("\f", "\\f");
    }

    /**
     * Returns the length of the supplied {@link String}. If the argument is
     * {@code null} the method returns {@code 0}.
     *
     * @param s the string whose length is to be obtained; may be {@code null}
     * @return {@code 0} when {@code s} is {@code null}, otherwise {@code s.length()}
     */
    public static int length(String s) {
        return (s == null) ? 0 : s.length();
    }

    /**
     * Returns the length of the supplied {@link CharSequence}. If the argument is
     * {@code null} the method returns {@code 0}.
     *
     * @param cs the character sequence whose length is to be obtained; may be {@code null}
     * @return {@code 0} when {@code cs} is {@code null}, otherwise {@code cs.length()}
     */
    public static int length(CharSequence cs) {
        return (cs == null) ? 0 : cs.length();
    }

    /**
     * Returns the length of the trimmed version of the supplied string.
     * If the argument is {@code null} the method returns {@code 0}.
     *
     * @param s the string to be trimmed and measured; may be {@code null}
     * @return the length of {@code s.trim()}, or {@code 0} when {@code s} is {@code null}
     */
    public static int trimLength(String s) {
        return (s == null) ? 0 : s.trim().length();
    }

    /**
     * Removes all leading and trailing double‑quote characters from the given
     * string. If the argument is {@code null} the method returns {@code null}.
     *
     * @param input the string from which to strip quotes; may be {@code null}
     * @return the string without surrounding quotes, or {@code null} if {@code input} is {@code null}
     */
    public static String removeLeadingAndTrailingQuotes(String input) {
        if (input == null) {
            return null;
        }
        int start = 0;
        int end = input.length();
        while (start < end && input.charAt(start) == '"') {
            start++;
        }
        while (end > start && input.charAt(end - 1) == '"') {
            end--;
        }
        return input.substring(start, end);
    }

//     @Test
//     void length_null_returnsZero() {
//         Assertions.assertEquals(0, StringUtilities.length(null), "length(null) should return 0");
//     }

//     @Test
//     void length_emptyString_returnsZero() {
//         Assertions.assertEquals(0, StringUtilities.length(""), "length(\"\") should return 0");
//     }

//     @Test
//     void length_nonEmptyString_returnsActualLength() {
//         String s = "Hello, World!";
//         Assertions.assertEquals(s.length(), StringUtilities.length(s), "length should match the actual string length");
//     }

//     @ParameterizedTest
//     @ValueSource(strings = { "a", "ab", "abc", "   leading and trailing spaces   ", "\t\n\r", "😀🐍🚀", "1234567890" })
//     void length_variousStrings_matchesStringLength(String input) {
//         Assertions.assertEquals(input.length(), StringUtilities.length(input), () -> "length should equal input.length() for \"" + escapeForMessage(input) + "\"");
//     }

//     @ParameterizedTest(name = "[{index}] length(\"{0}\") = {1}")
//     @MethodSource("provideStringsForLength")
//     void length_variousInputs_parameterized(String input, int expected) {
//         Assertions.assertEquals(expected, StringUtilities.length(input), "Unexpected length for input: \"" + escapeForMessage(input) + "\"");
//     }
}