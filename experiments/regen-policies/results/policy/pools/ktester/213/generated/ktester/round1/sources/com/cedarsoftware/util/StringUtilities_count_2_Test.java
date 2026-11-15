package com.cedarsoftware.util;

import java.util.stream.Stream;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
// import org.junit.runners.Parameterized.Parameters;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
// import static org.junit.Assert.assertEquals;
import org.junit.jupiter.api.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;

/**
 * Parameterized tests for {@link StringUtilities#count(CharSequence, CharSequence)}.
 */
@ExtendWith(MockitoExtension.class)
// @RunWith(Parameterized.class)
class StringUtilities_count_2_Test {

    /**
     * Test data container.
     */
    private final String content;
    private final String token;
    private final int expected;
    private final String description;

    /**
     * Constructor used by JUnit 4 parameterized runner.
     */
    public StringUtilities_count_2_Test(String content, String token, int expected, String description) {
        this.content = content;
        this.token = token;
        this.expected = expected;
        this.description = description;
    }

    /**
     * Provides arguments for the JUnit 5 {@code @ParameterizedTest}.
     */
    private static Stream<Arguments> countArgumentsProvider() {
        return Stream.of(
                Arguments.of(null, null, 0, "Both arguments are null"),
                Arguments.of(null, "any", 0, "Content null, token non‑null"),
                Arguments.of("any", null, 0, "Token null, content non‑null"),
                Arguments.of("", "nonEmpty", 0, "Content empty"),
                Arguments.of("nonEmpty", "", 0, "Token empty"),
                Arguments.of("hello world", "xyz", 0, "Token not present"),
                Arguments.of("hello world", "world", 1, "Token at end"),
                Arguments.of("foobar", "foo", 1, "Token at start"),
                Arguments.of("foobar", "bar", 1, "Token at end again"),
                Arguments.of("same", "same", 1, "Identical content and token"),
                Arguments.of("ababa", "ab", 2, "Two non‑overlapping 'ab'"),
                Arguments.of("ababab", "aba", 2, "Two occurrences of 'aba' with gap"),
                Arguments.of("aaaa", "aa", 3, "Three overlapping 'aa' in 'aaaa'"),
                Arguments.of("short", "muchLongerToken", 0, "Token longer than content"),
                Arguments.of("HelloWorld", "helloworld", 0, "Case‑sensitive mismatch"),
                Arguments.of(new StringBuilder("abcabc").toString(),
                        new StringBuilder("abc").toString(),
                        2,
                        "StringBuilder arguments, two occurrences"),
                Arguments.of("😀😃😀😃😀", "😀", 3, "Three emoji occurrences"),
                Arguments.of("a b a b", " a ", 2, "Token with leading/trailing spaces"),
                Arguments.of(generateRepeated("ab", 1000), "ab", 1000,
                        "Large repetitive content, 1000 occurrences"),
                Arguments.of("aaa", "aa", 2, "Overlapping 'aa' in 'aaa'"),
                Arguments.of("line1\nline2\nline1", "\n", 2, "Newline token appears twice")
        );
    }

    /**
     * Helper method to build a large repetitive string.
     *
     * @param fragment the fragment to repeat
     * @param count    how many times to repeat it
     * @return the concatenated string
     */
    private static String generateRepeated(String fragment, int count) {
        StringBuilder sb = new StringBuilder(fragment.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(fragment);
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------
    // Simple unit tests (JUnit 4)

//     @Test
//     void count_whenContentIsNull_returnsZero() {
//         assertEquals(0, StringUtilities.count(null, "token"));
//     }

//     @Test
//     void count_whenTokenIsNull_returnsZero() {
//         assertEquals(0, StringUtilities.count("some content", null));
//     }

//     @Test
//     void count_whenBothArgumentsAreNull_returnsZero() {
//         assertEquals(0, StringUtilities.count(null, null));
//     }

//     @Test
//     void count_whenContentIsEmpty_returnsZero() {
//         assertEquals(0, StringUtilities.count("", "token"));
//     }

//     @Test
//     void count_whenTokenIsEmpty_returnsZero() {
//         assertEquals(0, StringUtilities.count("some content", ""));
//     }

//     @Test
//     void count_whenBothAreEmpty_returnsZero() {
//         assertEquals(0, StringUtilities.count("", ""));
//     }

//     @Test
//     void count_whenTokenNotPresent_returnsZero() {
//         assertEquals(0, StringUtilities.count("hello world", "xyz"));
//     }

//     @Test
//     void count_whenTokenLongerThanContent_returnsZero() {
//         assertEquals(0, StringUtilities.count("short", "muchlonger"));
//     }

//     @Test
//     void count_whenTokenAppearsOnce_returnsOne() {
//         assertEquals(1, StringUtilities.count("the quick brown fox", "quick"));
//     }

//     @Test
//     void count_whenTokenEqualsWholeContent_returnsOne() {
//         assertEquals(1, StringUtilities.count("identical", "identical"));
//     }

//     @Test
//     void count_whenTokenAppearsMultipleTimes_returnsCorrectCount() {
//         assertEquals(3, StringUtilities.count("ababa babab abab", "ab"));
//     }

//     @Test
//     void count_whenTokenAppearsMultipleTimesWithDifferentPositions() {
//         assertEquals(4, StringUtilities.count("cat dog cat dog cat dog cat", "cat"));
//     }

//     @Test
//     void count_whenTokenOverlaps_countsEachOccurrence() {
//         // "aaa" contains "aa" at positions 0‑1 and 1‑2 → 2 matches
//         assertEquals(2, StringUtilities.count("aaa", "aa"));
//     }

//     @Test
//     void count_overlappingComplexPattern() {
//         // "abababa" contains "aba" at 0‑2, 2‑4, and 4‑6 → 3 matches
//         assertEquals(3, StringUtilities.count("abababa", "aba"));
//     }

//     @Test
//     void count_withStringBuilderArgument() {
//         StringBuilder content = new StringBuilder("builderbuilderbuilder");
//         CharSequence token = new StringBuilder("builder");
//         assertEquals(3, StringUtilities.count(content, token));
//     }

//     @Test
//     void count_withStringBufferArgument() {
//         StringBuffer content = new StringBuffer("bufferedbuffered");
//         CharSequence token = new StringBuffer("buffered");
//         assertEquals(2, StringUtilities.count(content, token));
//     }

//     @Test
//     void count_withSpecialCharacters() {
//         assertEquals(2, StringUtilities.count("a$b$c$d", "$"));
//         assertEquals(1, StringUtilities.count("line1\nline2\nline3", "\n"));
//     }

//     @Test
//     void count_withUnicodeCharacters() {
//         assertEquals(2, StringUtilities.count("😀😀😃", "😀"));
//     }

    // -----------------------------------------------------------------
    // JUnit 5 parameterized test

//     @ParameterizedTest(name = "{index}: {3}")
//     @MethodSource("countArgumentsProvider")
//     void count_parameterizedTests(String content, String token, int expected, String description) {
//         assertEquals(expected, StringUtilities.count(content, token), description);
//     }

    // -----------------------------------------------------------------
    // JUnit 4 parameterized data provider (used by the @RunWith runner)

//     @Parameters(name = "{index}: {3}")
//     public static Collection<Object[]> data() {
//         List<Object[]> list = new ArrayList<>();
//         list.add(new Object[]{null, null, 0, "Both arguments are null"});
//         list.add(new Object[]{null, "any", 0, "Content null, token non‑null"});
//         list.add(new Object[]{"any", null, 0, "Token null, content non‑null"});
//         list.add(new Object[]{"", "nonEmpty", 0, "Content empty"});
//         list.add(new Object[]{"nonEmpty", "", 0, "Token empty"});
//         list.add(new Object[]{"hello world", "xyz", 0, "Token not present"});
//         list.add(new Object[]{"hello world", "world", 1, "Token at end"});
//         list.add(new Object[]{"foobar", "foo", 1, "Token at start"});
//         list.add(new Object[]{"foobar", "bar", 1, "Token at end again"});
//         list.add(new Object[]{"same", "same", 1, "Identical content and token"});
//         list.add(new Object[]{"ababa", "ab", 2, "Two non‑overlapping 'ab'"});
//         list.add(new Object[]{"ababab", "aba", 2, "Two occurrences of 'aba' with gap"});
//         list.add(new Object[]{"aaaa", "aa", 3, "Three overlapping 'aa' in 'aaaa'"});
//         list.add(new Object[]{"short", "muchLongerToken", 0, "Token longer than content"});
//         list.add(new Object[]{"HelloWorld", "helloworld", 0, "Case‑sensitive mismatch"});
//         list.add(new Object[]{new StringBuilder("abcabc").toString(),
//                 new StringBuilder("abc").toString(),
//                 2,
//                 "StringBuilder arguments, two occurrences"});
//         list.add(new Object[]{"😀😃😀😃😀", "😀", 3, "Three emoji occurrences"});
//         list.add(new Object[]{"a b a b", " a ", 2, "Token with leading/trailing spaces"});
//         list.add(new Object[]{generateRepeated("ab", 1000), "ab", 1000,
//                 "Large repetitive content, 1000 occurrences"});
//         list.add(new Object[]{"aaa", "aa", 2, "Overlapping 'aa' in 'aaa'"});
//         list.add(new Object[]{"line1\nline2\nline1", "\n", 2, "Newline token appears twice"});
//         return list;
//     }

    // -----------------------------------------------------------------
    // JUnit 4 test that uses the fields populated by the Parameterized runner

//     @Test
//     public void count_parameterisedTests() {
//         assertEquals(description, expected, StringUtilities.count(content, token));
//     }
}