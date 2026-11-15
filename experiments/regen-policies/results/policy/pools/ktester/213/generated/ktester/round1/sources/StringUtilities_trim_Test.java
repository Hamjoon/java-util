package com.cedarsoftware.util;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

// @ExtendWith(MockitoExtension.class)
class StringUtilities_trim_Test {

    /**
     * Parameter source for the "null and whitespace handling" group.
     */
    static Stream<Arguments> nullAndWhitespaceProvider() {
        return Stream.of(// null input → null output
        Arguments.of(null, null, "When input is null, method should return null without throwing."), // empty string → empty string
        Arguments.of("", "", "Empty string should be returned unchanged."), // only spaces → empty string
        Arguments.of("   ", "", "String consisting only of whitespace should be trimmed to empty string."), // mixed whitespace characters (tab, newline, carriage‑return)
        Arguments.of("  abc  ", "abc", "Leading and trailing spaces should be removed, internal characters preserved."), // no surrounding whitespace
        Arguments.of("\t\nabc\r\n", "abc", "Various whitespace characters (tab, newline, carriage return) should be trimmed."), Arguments.of("abc", "abc", "String without surrounding whitespace should be returned unchanged."));
    }

    /**
     * Parameter source for the "Unicode and control characters handling" group.
     *
     * <p>Notes about expectations:</p>
     * <ul>
     *   <li>U+00A0 (non‑breaking space) is > 0x20, therefore {@code String.trim()}
     *       does NOT treat it as whitespace – the original string is returned.</li>
     *   <li>Form‑feed (0x0C) and vertical‑tab (0x0B) are ≤ 0x20 and are trimmed.</li>
     *   <li>U+0000 (NUL) is also ≤ 0x20 and is trimmed.</li>
     * </ul>
     */
    static Stream<Arguments> unicodeAndControlProvider() {
        return Stream.of(// NBSP on both ends – should stay untouched
        Arguments.of("\u00A0abc\u00A0", "\u00A0abc\u00A0", "Non‑breaking space (U+00A0) is not considered whitespace by String.trim, so leading and trailing NBSP should remain unchanged."), // Form feed and vertical tab – trimmed
        Arguments.of("\u000C\u000Babc\u000B\u000C", "abc", "Form feed (\\f) and vertical tab (\\u000B) are <= '\\u0020' and therefore trimmed; result should be the core word."), // NUL characters – trimmed
        Arguments.of("\u0000\u0000abc\u0000", "abc", "Null character (U+0000) is also trimmed by String.trim, leaving only the inner text."), // Mixed regular space and NBSP inside – only regular spaces trimmed
        Arguments.of("  abc\u00A0def  ", "abc\u00A0def", "Standard spaces are trimmed, but the internal non‑breaking space should stay, demonstrating selective trimming."), // Only NBSP characters – no trimming
        Arguments.of("\u00A0\u00A0", "\u00A0\u00A0", "A string consisting solely of non‑breaking spaces is not trimmed; the method should return the original string unchanged."), // Emoji without surrounding whitespace – unchanged
        Arguments.of("😊", "😊", "Emoji (surrogate pair) without surrounding whitespace should be returned unchanged; also verifies that trim works correctly with characters outside the BMP."), // Emoji with surrounding whitespace – trimmed
        Arguments.of("  😊  ", "😊", "Whitespace surrounding a surrogate pair should be removed, leaving only the emoji."));
    }

    /**
     * Parameter source for the "reference identity" group.
     *
     * <p>The expected value {@code "same instance"} is a marker that the test
     * should assert {@link Assertions#assertSame(Object, Object)}.</p>
     */
    static Stream<Arguments> referenceIdentityProvider() {
        return Stream.of(Arguments.of("abc", "same instance", "When no leading or trailing whitespace exists, String.trim returns the original string object; the test should assert that the returned reference equals the input reference."), Arguments.of("", "same instance", "Empty string is already trimmed; String.trim returns the same instance, not a new object."));
    }

    /**
     * Removes control characters (char ≤ 32) from both ends of the supplied string.
     * <p>
     * The implementation mirrors {@link String#trim()} but guarantees that:
     * <ul>
     *   <li>the original reference is returned when no trimming is required,</li>
     *   <li>a {@code null} input yields {@code null}, and</li>
     *   <li>characters greater than {@code 0x20} (including non‑breaking spaces) are left untouched.</li>
     * </ul>
     *
     * @param str the string to trim; may be {@code null}
     * @return the trimmed string, or {@code null} if the input was {@code null}
     */
    public static String trim(String str) {
        if (str == null) {
            return null;
        }
        int len = str.length();
        int st = 0;
        // Find first character > 0x20
        while (st < len && str.charAt(st) <= 0x20) {
            st++;
        }
        // Find last character > 0x20
        while (len > st && str.charAt(len - 1) <= 0x20) {
            len--;
        }
        // No trimming needed – return the original instance
        if (st == 0 && len == str.length()) {
            return str;
        }
        // Substring creates a new String instance with the trimmed content
        return str.substring(st, len);
    }

    /**
     * Null‑safe equality check (case‑sensitive).
     */
    public static boolean equals(String a, String b) {
        return a == b || (a != null && a.equals(b));
    }

    /**
     * Null‑safe case‑insensitive equality check.
     */
    public static boolean equalsIgnoreCase(String a, String b) {
        return a == b || (a != null && a.equalsIgnoreCase(b));
    }

    /**
     * Equality check after trimming both strings.
     */
    public static boolean equalsWithTrim(String a, String b) {
        return equals(trim(a), trim(b));
    }

    /**
     * Returns {@code true} if the supplied string is {@code null},
     * empty, or consists solely of characters ≤ 0x20.
     */
    public static boolean isEmpty(String s) {
        return trim(s) == null || trim(s).isEmpty();
    }

    /**
     * Returns the trimmed length of the supplied string, treating {@code null}
     * as length {@code 0}.
     */
    public static int trimLength(String s) {
        String t = trim(s);
        return t == null ? 0 : t.length();
    }

    /**
     * Splits a comma‑separated string into a {@link Set} of trimmed, non‑empty
     * elements. Returns an empty set for {@code null} or empty input.
     */
    public static Set<String> commaSeparatedStringToSet(String csv) {
        if (isEmpty(csv)) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String part : csv.split(",")) {
            String trimmed = trim(part);
            if (trimmed != null && !trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

//     @Test
//     void trim_givenNull_returnsNull() {
//         assertNull(StringUtilities.trim(null), "trim(null) should return null");
//     }

//     @Test
//     void trim_givenEmptyString_returnsEmptyString() {
//         assertEquals("", StringUtilities.trim(""), "trim(\"\") should return an empty string");
//     }

//     @Test
//     void trim_givenStringWithoutWhitespace_returnsSameString() {
//         String input = "noWhitespace";
//         // String.trim() returns the same instance when no trimming is needed.
//         assertSame(input, StringUtilities.trim(input), "trim should return the original instance when no trimming is needed");
//     }

//     @Test
//     void trim_givenStringWithLeadingAndTrailingWhitespace_isTrimmed() {
//         String input = "   surrounded by spaces   ";
//         String expected = "surrounded by spaces";
//         assertEquals(expected, StringUtilities.trim(input));
//     }

//     @Test
//     void trim_givenStringWithOnlyWhitespace_returnsEmptyString() {
//         String input = " \t\n\r  ";
//         assertEquals("", StringUtilities.trim(input));
//     }

//     @Test
//     void trim_givenStringWithMixedWhitespaceCharacters_isTrimmedCorrectly() {
//         String input = "\t  \nHello World\r\n  ";
//         String expected = "Hello World";
//         assertEquals(expected, StringUtilities.trim(input));
//     }

//     @Test
//     void trim_isNullSafeAndDoesNotThrow() {
//         // The method should never throw an exception for any input, including null.
//         assertDoesNotThrow(() -> StringUtilities.trim(null));
//         assertDoesNotThrow(() -> StringUtilities.trim(""));
//         assertDoesNotThrow(() -> StringUtilities.trim("   test   "));
//     }

//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("nullAndWhitespaceProvider")
//     void trim_nullAndWhitespaceHandling(String input, String expected, String description) {
//         // Act
//         String result = StringUtilities.trim(input);
//         // Assert
//         assertEquals(expected, result, description);
//     }

//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("unicodeAndControlProvider")
//     void trim_unicodeAndControlCharacters(String input, String expected, String description) {
//         // Act
//         String result = StringUtilities.trim(input);
//         // Assert
//         assertEquals(expected, result, description);
//     }

//     @ParameterizedTest(name = "[{index}] {2}")
//     @MethodSource("referenceIdentityProvider")
//     void trim_referenceIdentity(String input, String expectedMarker, String description) {
//         // Act
//         String result = StringUtilities.trim(input);
//         // Assert – the marker indicates we must check reference equality
//         assertSame(input, result, description);
//     }
}