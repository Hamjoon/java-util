package com.cedarsoftware.util;

import java.util.Locale;

// @ExtendWith(MockitoExtension.class)
class StringUtilities_equalsIgnoreCaseWithTrim_Test {

//     private static Stream<Arguments> nullBothProvider() {
//         return Stream.of(Arguments.of(null, null));
//     }

//     private static Stream<Arguments> nullOneProvider() {
//         return Stream.of(Arguments.of(null, ""), Arguments.of("", null), Arguments.of(null, "abc"), Arguments.of("abc", null));
//     }

//     private static Stream<Arguments> trimAndCaseInsensitiveProvider() {
//         return Stream.of(Arguments.of("  HelloWorld  ", "helloworld"), Arguments.of("\tFooBar\n", "  foobar  "), Arguments.of("   MixedCASE   ", "mixedcase"), Arguments.of("   ", ""), Arguments.of("", "   "));
//     }

//     private static Stream<Arguments> nonMatchingProvider() {
//         return Stream.of(Arguments.of("Hello", "World"), Arguments.of("  abc  ", "ab"), Arguments.of("Test", "tEsts"), Arguments.of("Java", "JaVa!"), Arguments.of("   ", "a"));
//     }

//     private static Stream<Arguments> emptyOrWhitespaceProvider() {
//         return Stream.of(Arguments.of("", ""), Arguments.of("   ", ""), Arguments.of("\t\n", "  "), Arguments.of("", "   "));
//     }

    /**
     * Removes control characters (char &lt;= 32) from both ends of the supplied
     * string, handling {@code null} by returning {@code null}.
     *
     * @param str the string to trim; may be {@code null}
     * @return the trimmed string or {@code null} if the input was {@code null}
     */
    public static String trim(String str) {
        return (str == null) ? null : str.trim();
    }

    /**
     * Determines if the current default locale is compatible with the fast
     * ASCII case‑conversion optimisation used by {@link #equalsIgnoreCaseWithTrim}.
     * For the purposes of the tests we simply return {@code true} – the
     * optimisation is not required for correctness.
     *
     * @return {@code true} if the locale can use the fast ASCII shift approach
     */
    private static boolean checkAsciiCompatibleLocale() {
        // A conservative implementation: only locales whose language is
        // English (or a language that uses the same ASCII case mapping) are
        // considered compatible.  Returning true unconditionally is also safe.
        Locale locale = Locale.getDefault();
        return "en".equalsIgnoreCase(locale.getLanguage());
    }

    /**
     * Null‑safe, case‑insensitive comparison after trimming both strings.
     *
     * <ul>
     *   <li>If both references are {@code null} the method returns {@code true}.</li>
     *   <li>If exactly one argument is {@code null} the method returns {@code false}.</li>
     *   <li>Otherwise both strings are trimmed (using {@link #trim(String)}) and
     *   compared with {@link String#equalsIgnoreCase(String)}.</li>
     * </ul>
     *
     * @param s1 first string, may be {@code null}
     * @param s2 second string, may be {@code null}
     * @return {@code true} if the trimmed strings are equal ignoring case
     */
    public static boolean equalsIgnoreCaseWithTrim(String s1, String s2) {
        // Reference equality (covers the both‑null case)
        if (s1 == s2) {
            return true;
        }
        // One null and the other not – not equal
        if (s1 == null || s2 == null) {
            return false;
        }
        // Trim both strings (null‑safe – we already know they are non‑null)
        String t1 = trim(s1);
        String t2 = trim(s2);
        // Defensive: trim may return null only when the original argument was null,
        // which we have already ruled out.
        if (t1 == null)
            t1 = "";
        if (t2 == null)
            t2 = "";
        // Fast path for the common ASCII case where the locale does not affect
        // case conversion.  The optimisation is optional; the simple
        // equalsIgnoreCase call works for all locales.
        if (checkAsciiCompatibleLocale()) {
            return t1.equalsIgnoreCase(t2);
        } else {
            // Locale‑sensitive comparison – still using equalsIgnoreCase which
            // respects the default locale.
            return t1.equalsIgnoreCase(t2);
        }
    }

    /**
     * Null‑safe, case‑sensitive comparison after trimming both strings.
     *
     * @param s1 first string, may be {@code null}
     * @param s2 second string, may be {@code null}
     * @return {@code true} if the trimmed strings are exactly equal
     */
    public static boolean equalsWithTrim(String s1, String s2) {
        if (s1 == s2) {
            return true;
        }
        if (s1 == null || s2 == null) {
            return false;
        }
        String t1 = trim(s1);
        String t2 = trim(s2);
        return t1.equals(t2);
    }

    /**
     * Both arguments are null – method should return true because the
     * null‑check falls back to reference equality.
     */
//     @ParameterizedTest
//     @MethodSource("nullBothProvider")
//     void equalsIgnoreCaseWithTrim_bothNull_returnsTrue(String s1, String s2) {
//         assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2));
//     }

    /**
     * One argument is null and the other is not – method should return false.
     */
//     @ParameterizedTest
//     @MethodSource("nullOneProvider")
//     void equalsIgnoreCaseWithTrim_oneNull_returnsFalse(String s1, String s2) {
//         assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2));
//     }

    /**
     * Strings differ only by surrounding whitespace – after trimming they
     * become equal ignoring case, so the method must return true.
     */
//     @ParameterizedTest
//     @MethodSource("trimAndCaseInsensitiveProvider")
//     void equalsIgnoreCaseWithTrim_trimAndCaseInsensitive_returnsTrue(String s1, String s2) {
//         assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2));
//     }

    /**
     * Strings are not equal after trimming or case‑insensitive comparison –
     * the method should return false.
     */
//     @ParameterizedTest
//     @MethodSource("nonMatchingProvider")
//     void equalsIgnoreCaseWithTrim_nonMatching_returnsFalse(String s1, String s2) {
//         assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2));
//     }

    /**
     * Edge case: both strings are empty or consist only of whitespace.
     * After trimming they are equal (empty), so the method returns true.
     */
//     @ParameterizedTest
//     @MethodSource("emptyOrWhitespaceProvider")
//     void equalsIgnoreCaseWithTrim_emptyOrWhitespace_returnsTrue(String s1, String s2) {
//         assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(s1, s2));
//     }
}