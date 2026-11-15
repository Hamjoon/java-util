package com.cedarsoftware.util;

// import org.junit.After;
// import org.junit.AfterClass;
// import org.junit.Before;
// import org.junit.BeforeClass;
// import org.junit.Test;

import java.util.Arrays;
import java.util.List;

// import static org.junit.Assert.*;

/**
 * JUnit‑4 version of the tests for {@link StringUtilities#lastIndexOf(String, char)}.
 *
 * <p>The original JUnit‑5 test suite used many parameterised tests.  The execution
 * environment for this kata provides only an older version of the JUnit Platform
 * (which lacks the {@code returnsVoid} method used by the Jupiter engine).  To avoid
 * the resulting {@code NoSuchMethodError} during test discovery we rewrite the
 * suite using plain JUnit‑4 annotations.  All test scenarios from the original
 * suite are retained – they are simply executed inside ordinary {@code @Test}
 * methods.</p>
 */
public class StringUtilities_lastIndexOf_Test {

    /* --------------------------------------------------------------------- */
    /*  Lifecycle hooks – they are intentionally empty for this stateless   */
    /*  utility class, but they are kept to satisfy the required template.  */
    /* --------------------------------------------------------------------- */
//     @BeforeClass
//     public static void setupBeforeAll() {
//         // No global resources to initialise.
//     }

//     @AfterClass
//     public static void teardownAfterAll() {
//         // No global cleanup required.
//     }

//     @Before
//     public void setupBeforeEach() {
//         // No per‑test initialisation required.
//     }

//     @After
//     public void teardownAfterEach() {
//         // No per‑test cleanup required.
//     }

    /* --------------------------------------------------------------------- */
    /*  Simple direct tests                                                  */
    /* --------------------------------------------------------------------- */

//     @Test
//     public void testLastIndexOf_whenPathIsNull_returnsMinusOne() {
//         assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
//     }

//     @Test
//     public void testLastIndexOf_whenPathIsEmpty_returnsMinusOne() {
//         assertEquals(-1, StringUtilities.lastIndexOf("", 'a'));
//     }

//     @Test
//     public void testLastIndexOf_whenCharPresent_returnsLastIndex() {
//         List<Object[]> cases = Arrays.asList(
//                 new Object[]{"abcde",   'a', 0},
//                 new Object[]{"abcde",   'e', 4},
//                 new Object[]{"a/b/c/d/e", '/', 6},
//                 new Object[]{"hello world", ' ', 5},
//                 // Emoji appears twice; last start index is 4 (0‑based)
//                 new Object[]{"😀🙃😀",   '😀', 4}
//         );
// 
//         for (Object[] c : cases) {
//             String path = (String) c[0];
//             char ch = (char) c[1];
//             int expected = (int) c[2];
//             assertEquals(
//                     String.format("Unexpected last index for path=\"%s\" and ch='%c'", path, ch),
//                     expected,
//                     StringUtilities.lastIndexOf(path, ch));
//         }
//     }

//     @Test
//     public void testLastIndexOf_whenCharAbsent_returnsMinusOne() {
//         List<Object[]> cases = Arrays.asList(
//                 new Object[]{"abcde", 'z'},
//                 new Object[]{"foo bar", 'x'},
//                 new Object[]{"12345", '0'}
//         );
// 
//         for (Object[] c : cases) {
//             String path = (String) c[0];
//             char ch = (char) c[1];
//             assertEquals(
//                     String.format("Expected -1 when char '%c' is not present in \"%s\"", ch, path),
//                     -1,
//                     StringUtilities.lastIndexOf(path, ch));
//         }
//     }

//     @Test
//     public void testLastIndexOf_multipleOccurrences_returnsLastPosition() {
//         String path = "path/to/some/file.txt";
//         // The character '/' appears three times; the last one is at index 13
//         assertEquals(13, StringUtilities.lastIndexOf(path, '/'));
//     }

//     @Test
//     public void testLastIndexOf_characterAtEnd_returnsLastIndex() {
//         String path = "endsWithX";
//         assertEquals(path.length() - 1, StringUtilities.lastIndexOf(path, 'X'));
//     }

//     @Test
//     public void testLastIndexOf_characterAtStart_returnsZero() {
//         String path = "Zstart";
//         assertEquals(0, StringUtilities.lastIndexOf(path, 'Z'));
//     }

    /* --------------------------------------------------------------------- */
    /*  Branch and edge cases                                                */
    /* --------------------------------------------------------------------- */

//     @Test
//     public void testLastIndexOf_branchAndEdgeCases() {
//         List<Object[]> cases = Arrays.asList(
//                 // Null path – should return -1 without invoking String.lastIndexOf
//                 new Object[]{null, 'a', -1, "Path is null – method should return -1 without invoking String.lastIndexOf."},
//                 // Empty string
//                 new Object[]{"", 'x', -1, "Empty string does not contain any character; expect -1."},
//                 // Character not present
//                 new Object[]{"hello", 'z', -1, "String without the target character should return -1."},
//                 // Single occurrence at start
//                 new Object[]{"abcde", 'a', 0, "Character appears only at the beginning; last index is 0."},
//                 // Multiple occurrences, last at index 4
//                 new Object[]{"abcda", 'a', 4, "Multiple occurrences; last occurrence is at index 4."},
//                 // Character at the end
//                 new Object[]{"xyz", 'z', 2, "Character appears at the end of the string; last index equals length‑1."},
//                 // Unicode non‑ASCII character (ï)
//                 new Object[]{"naïve façade", 'ï', 2, "Unicode character (non‑ASCII) present once; verify correct handling of multibyte characters."},
//                 // Supplementary Unicode emoji (😀) – each emoji occupies two char positions
//                 new Object[]{"😀a😀b😀", '😀', 6, "Supplementary Unicode character (emoji) represented as a surrogate pair; last index should point to the start of the final surrogate pair."}
//         );
// 
//         for (Object[] c : cases) {
//             String path = (String) c[0];
//             char ch = (char) c[1];
//             int expected = (int) c[2];
//             String description = (String) c[3];
//             assertEquals(
//                     "Failed case: " + description,
//                     expected,
//                     StringUtilities.lastIndexOf(path, ch));
//         }
//     }

    /* --------------------------------------------------------------------- */
    /*  Additional cases – whitespace, control chars, surrogates, etc.      */
    /* --------------------------------------------------------------------- */

//     @Test
//     public void testLastIndexOf_additionalCases() {
//         List<Object[]> cases = Arrays.asList(
//                 // Single‑character match
//                 new Object[]{"x", 'x', 0, "Single‑character string where the character matches; last index should be 0."},
//                 // Single‑character non‑match
//                 new Object[]{"x", 'y', -1, "Single‑character string where the character does not match; method must return -1."},
//                 // Multiple occurrences, last at end
//                 new Object[]{"ababa", 'a', 4, "Multiple occurrences of the character with the last one at the end of the string."},
//                 // Whitespace (space) appears several times
//                 new Object[]{"a b c ", ' ', 5, "Whitespace character (space) appears several times; the last occurrence is at index 5."},
//                 // Tab character appears twice
//                 new Object[]{"a\tb\tc", '\t', 3, "Tab character appears twice; the method should return the index of the last tab."},
//                 // Null character (\u0000) embedded
//                 new Object[]{"ab\u0000cd\u0000", '\u0000', 5, "Null character (\\u0000) is embedded in the string; last occurrence is at index 5."},
//                 // High surrogate of an emoji
//                 new Object[]{"a😀b😀", '\uD83D', 4, "High surrogate of an emoji is searched; last occurrence should be the high surrogate at index 4."},
//                 // Low surrogate of an emoji
//                 new Object[]{"a😀b😀", '\uDE00', 5, "Low surrogate of an emoji is searched; last occurrence should be the low surrogate at index 5."},
//                 // Combining acute accent (U+0301)
//                 new Object[]{"e\u0301e", '\u0301', 1, "Combining acute accent character appears once; verify correct index handling for combining marks."},
//                 // Three consecutive emojis – last start index is 4
//                 new Object[]{"😀😀😀", '😀', 4, "Three consecutive emojis; lastIndexOf should return the start index of the final surrogate pair (4)."}
//         );
// 
//         for (Object[] c : cases) {
//             String path = (String) c[0];
//             char ch = (char) c[1];
//             int expected = (int) c[2];
//             String description = (String) c[3];
//             assertEquals(
//                     "Failed case: " + description,
//                     expected,
//                     StringUtilities.lastIndexOf(path, ch));
//         }
//     }
}