package com.cedarsoftware.util;

// @ExtendWith(MockitoExtension.class)
class StringUtilities_removeLeadingAndTrailingQuotes_Test {

//     private static Stream<Arguments> nullOrEmptyProvider() {
//         return Stream.of(Arguments.of((String) null), Arguments.of(""));
//     }

//     private static Stream<Arguments> noQuotesProvider() {
//         return Stream.of(Arguments.of("Foo"), Arguments.of(" \"embedded\" quotes "), Arguments.of("12345"), Arguments.of("   leading space"), Arguments.of(" \"abc\" "), Arguments.of("ab\\\"cd\\\"ef"));
//     }

//     private static Stream<Arguments> leadingTrailingQuotesProvider() {
//         return Stream.of(Arguments.of("\"Foo\"", "Foo"), Arguments.of("\"Foo", "Foo"), Arguments.of("Foo\"", "Foo"), Arguments.of("\"\"Foo\"\"", "Foo"), Arguments.of("\"\"\"Bar\"\"\"", "Bar"), Arguments.of("\"He said \\\"yes\\\" today\"", "He said \\\"yes\\\" today"), Arguments.of("\"", ""), Arguments.of("\"\"\"\"", ""), Arguments.of("\"  spaced  \"", "  spaced  "), Arguments.of("\"\"", ""), Arguments.of("\"a\\\"b\\\"c\"", "a\\\"b\\\"c"), Arguments.of("\"\"abc", "abc"), Arguments.of("abc\\\"\\\"", "abc"), Arguments.of(" \"abc\" ", " \"abc\" "));
//     }

//     private static Stream<Arguments> additionalEdgeCasesProvider() {
//         String manyQuotes = "\"".repeat(200);
//         return Stream.of(Arguments.of("\"", ""), Arguments.of("\"\"\"", ""), Arguments.of("\"\nabc\"", "\nabc"), Arguments.of("\"\"\"abc", "abc"), Arguments.of("\"abc\" ", "\"abc\" "), Arguments.of(" \"abc\"", " \"abc\""), Arguments.of("abc\\\\\"", "abc\\\\"), Arguments.of("“abc”", "“abc”"), Arguments.of(manyQuotes, ""));
//     }

    /**
     * Removes all leading and trailing double‑quote characters (").
     * Multiple consecutive quotes at the beginning or end of the string
     * are all removed.  The method does **not** treat escaped quotes
     * (e.g. {@code \"}) specially – they are considered ordinary characters
     * because the backslash is part of the string content.
     *
     * @param input the String from which to remove quotes (may be {@code null})
     * @return the String with all leading and trailing quotes removed,
     *         or {@code null} if the input was {@code null}
     */
    public static String removeLeadingAndTrailingQuotes(String input) {
        if (input == null) {
            return null;
        }
        int start = 0;
        int end = input.length();
        // Skip over leading quotes
        while (start < end && input.charAt(start) == '"') {
            start++;
        }
        // Skip over trailing quotes
        while (end > start && input.charAt(end - 1) == '"') {
            end--;
        }
        return input.substring(start, end);
    }

    public static int length(String s) {
        return s == null ? 0 : s.length();
    }

    public static int trimLength(String s) {
        return s == null ? 0 : s.trim().length();
    }

//     @ParameterizedTest
//     @MethodSource("nullOrEmptyProvider")
//     void removeLeadingAndTrailingQuotes_returnsInputUnchanged_whenNullOrEmpty(String input) {
//         assertSame(input, removeLeadingAndTrailingQuotes(input));
//     }

//     @ParameterizedTest
//     @MethodSource("noQuotesProvider")
//     void removeLeadingAndTrailingQuotes_returnsSame_whenNoSurroundingQuotes(String input) {
//         assertEquals(input, removeLeadingAndTrailingQuotes(input));
//     }

//     @ParameterizedTest
//     @MethodSource("leadingTrailingQuotesProvider")
//     void removeLeadingAndTrailingQuotes_stripsOnlyOuterQuotes(String input, String expected) {
//         assertEquals(expected, removeLeadingAndTrailingQuotes(input));
//     }

//     @Test
//     void removeLeadingAndTrailingQuotes_doesNotAffectOtherCharacters() {
//         String input = "'\"Hello\"'";
//         String expected = "'\"Hello\"'";
//         assertEquals(expected, removeLeadingAndTrailingQuotes(input));
//     }

//     @Test
//     void removeLeadingAndTrailingQuotes_handlesComplexExampleFromCodeBase() {
//         String input = "\"\"\"This is \"really\" weird.\"\"\"";
//         String expected = "This is \"really\" weird.";
//         assertEquals(expected, removeLeadingAndTrailingQuotes(input));
//     }

//     @ParameterizedTest
//     @MethodSource("additionalEdgeCasesProvider")
//     void removeLeadingAndTrailingQuotes_handlesAdditionalEdgeCases(String input, String expected) {
//         assertEquals(expected, removeLeadingAndTrailingQuotes(input));
//     }
}