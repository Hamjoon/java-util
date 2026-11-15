package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class StringUtilitiesTest {

    /* ---------------------------------------------------------------------- */
    /*  Helper utilities for the test class                                   */
    /* ---------------------------------------------------------------------- */

    private static Locale originalLocale;

    @BeforeClass
    public static void setUpClass() {
        // Preserve the original default locale so we can restore it later.
        originalLocale = Locale.getDefault();
        // Force a known locale that is ASCII‑compatible to make hashCodeIgnoreCase predictable.
        Locale.setDefault(Locale.US);
        // Force re‑evaluation of the internal flag (reflection is used because the flag is private).
        try {
            java.lang.reflect.Field f = StringUtilities.class.getDeclaredField("isAsciiCompatibleLocale");
            f.setAccessible(true);
            f.set(null, true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void tearDownClass() {
        Locale.setDefault(originalLocale);
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /* ---------------------------------------------------------------------- */
    /*  1. equals / equalsIgnoreCase (CharSequence and String overloads)      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsCharSequence() {
        assertTrue(StringUtilities.equals("abc", "abc"));
        assertFalse(StringUtilities.equals("abc", "ABC"));
        assertTrue(StringUtilities.equals(null, null));
        assertFalse(StringUtilities.equals(null, "abc"));
        assertFalse(StringUtilities.equals("abc", null));

        // Different implementations of CharSequence
        CharSequence cs1 = new StringBuilder("xyz");
        CharSequence cs2 = new StringBuffer("xyz");
        assertTrue(StringUtilities.equals(cs1, cs2));
    }

    @Test
    public void testEqualsStringOverload() {
        assertTrue(StringUtilities.equals((String) "test", (String) "test"));
        assertFalse(StringUtilities.equals((String) "test", (String) "Test"));
        assertTrue(StringUtilities.equals((String) null, (String) null));
    }

    @Test
    public void testEqualsIgnoreCaseCharSequence() {
        assertTrue(StringUtilities.equalsIgnoreCase("abc", "ABC"));
        assertFalse(StringUtilities.equalsIgnoreCase("abc", "abd"));
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        assertFalse(StringUtilities.equalsIgnoreCase(null, "abc"));
        assertFalse(StringUtilities.equalsIgnoreCase("abc", null));

        CharSequence cs1 = new StringBuilder("MixED");
        CharSequence cs2 = new StringBuffer("mixed");
        assertTrue(StringUtilities.equalsIgnoreCase(cs1, cs2));
    }

    @Test
    public void testEqualsIgnoreCaseStringOverload() {
        assertTrue(StringUtilities.equalsIgnoreCase((String) "Hello", (String) "heLLo"));
        assertFalse(StringUtilities.equalsIgnoreCase((String) "Hello", (String) "World"));
        assertTrue(StringUtilities.equalsIgnoreCase((String) null, (String) null));
    }

    /* ---------------------------------------------------------------------- */
    /*  2. regionMatches (package‑private)                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRegionMatchesBasic() {
        assertTrue(StringUtilities.regionMatches("abcdef", false, 2, "cde", 0, 3));
        assertFalse(StringUtilities.regionMatches("abcdef", false, 2, "cDf", 0, 3));
        assertTrue(StringUtilities.regionMatches("AbCdEf", true, 0, "aBcDeF", 0, 6));
    }

    @Test
    public void testRegionMatchesEdgeCases() {
        // start index out of bounds – should return false, not throw
        assertFalse(StringUtilities.regionMatches("abc", false, 5, "abc", 0, 1));

        // negative length – should return false
        assertFalse(StringUtilities.regionMatches("abc", false, 0, "abc", 0, -1));

        // length larger than remaining characters – false
        assertFalse(StringUtilities.regionMatches("abc", false, 1, "abc", 0, 3));
    }

    @Test
    public void testRegionMatchesNullParameters() {
        thrown.expect(RuntimeException.class); // Convention.throwIfNull throws a RuntimeException (IllegalArgumentException)
        StringUtilities.regionMatches(null, false, 0, "abc", 0, 1);
    }

    /* ---------------------------------------------------------------------- */
    /*  3. equalsWithTrim / equalsIgnoreCaseWithTrim                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  hello ", "hello"));
        assertFalse(StringUtilities.equalsWithTrim("hello", "world"));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim(null, " "));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  HeLLo ", "hello"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("test", "toast"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(null, null));
    }

    /* ---------------------------------------------------------------------- */
    /*  4. isEmpty / isWhitespace / hasContent                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsEmptyAndIsWhitespace() {
        assertTrue(StringUtilities.isEmpty(null));
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty("   "));
        assertFalse(StringUtilities.isEmpty(" a "));

        assertTrue(StringUtilities.isWhitespace(null));
        assertTrue(StringUtilities.isWhitespace(""));
        assertTrue(StringUtilities.isWhitespace("\t\n "));
        assertFalse(StringUtilities.isWhitespace("x"));
    }

    @Test
    public void testHasContent() {
        assertFalse(StringUtilities.hasContent(null));
        assertFalse(StringUtilities.hasContent("   "));
        assertTrue(StringUtilities.hasContent("  x  "));
    }

    /* ---------------------------------------------------------------------- */
    /*  5. length / trimLength                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLength() {
        assertEquals(0, StringUtilities.length((String) null));
        assertEquals(0, StringUtilities.length(""));
        assertEquals(5, StringUtilities.length("hello"));
    }

    @Test
    public void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(0, StringUtilities.trimLength("   "));
        assertEquals(5, StringUtilities.trimLength("  hello  "));
    }

    /* ---------------------------------------------------------------------- */
    /*  6. lastIndexOf                                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLastIndexOf() {
        assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        assertEquals(3, StringUtilities.lastIndexOf("ab/cd/e", '/'));
        assertEquals(0, StringUtilities.lastIndexOf("/", '/'));
    }

    /* ---------------------------------------------------------------------- */
    /*  7. decode / encode                                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] data = new byte[] { (byte)0xAB, (byte)0xCD, (byte)0x01 };
        String encoded = StringUtilities.encode(data);
        assertEquals("ABCD01", encoded);

        byte[] decoded = StringUtilities.decode(encoded);
        assertArrayEquals(data, decoded);
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(StringUtilities.decode("ABC")); // length 3 – odd
    }

    @Test
    public void testDecodeInvalidHexDigits() {
        // Character.digit returns -1 for non‑hex characters; the method still computes a value.
        // We just verify that it does not throw and returns a byte array of expected length.
        byte[] result = StringUtilities.decode("ZZ");
        assertNotNull(result);
        assertEquals(1, result.length);
        // The exact value is implementation‑defined; we only assert that it equals ((-1)*16 + -1) = -17 (0xEF)
        assertEquals((byte)0xEF, result[0]);
    }

    /* ---------------------------------------------------------------------- */
    /*  8. count (String overload) / count(CharSequence, CharSequence)        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCountStringChar() {
        assertEquals(3, StringUtilities.count("banana", 'a'));
        assertEquals(0, StringUtilities.count("banana", 'z'));
        assertEquals(0, StringUtilities.count(null, 'a'));
    }

    @Test
    public void testCountCharSequence() {
        assertEquals(2, StringUtilities.count("ababa", "aba"));
        assertEquals(0, StringUtilities.count("ababa", "xyz"));
        assertEquals(0, StringUtilities.count(null, "a"));
        assertEquals(0, StringUtilities.count("abc", null));
        assertEquals(0, StringUtilities.count("", "a"));
        assertEquals(0, StringUtilities.count("abc", ""));
    }

    /* ---------------------------------------------------------------------- */
    /*  9. wildcardToRegexString                                                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testWildcardToRegexString() {
        String pattern = "*a?b[cd].(e)f\\g";
        String regex = StringUtilities.wildcardToRegexString(pattern);
        // Expected regex: ^.*a.b[cd]\.\(e\)f\\g$
        assertEquals("^.*a.b[cd]\\.\\(e\\)f\\\\g$", regex);
    }

    /* ---------------------------------------------------------------------- */
    /* 10. levenshteinDistance / damerauLevenshteinDistance                     */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testLevenshteinDistanceBasic() {
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(5, StringUtilities.levenshteinDistance(null, "hello"));
        assertEquals(4, StringUtilities.levenshteinDistance("test", null));
    }

    @Test
    public void testDamerauLevenshteinDistanceBasic() {
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("book", "back"));
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        assertEquals(4, StringUtilities.damerauLevenshteinDistance(null, "test"));
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("test", null));
    }

    /* ---------------------------------------------------------------------- */
    /* 11. getRandomString / getRandomChar                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetRandomStringDeterministic() {
        Random rnd = new Random(12345L);
        String s = StringUtilities.getRandomString(rnd, 5, 5);
        assertEquals(5, s.length());
        // First character must be uppercase, the rest lowercase
        assertTrue(Character.isUpperCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            assertTrue(Character.isLowerCase(s.charAt(i)));
        }
    }

    @Test
    public void testGetRandomChar() {
        Random rnd = new Random(0L);
        String upper = StringUtilities.getRandomChar(rnd, true);
        String lower = StringUtilities.getRandomChar(rnd, false);
        assertEquals(1, upper.length());
        assertEquals(1, lower.length());
        assertTrue(Character.isUpperCase(upper.charAt(0)));
        assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    /* ---------------------------------------------------------------------- */
    /* 12. getBytes / createString (encoding handling)                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetBytesSupportedEncoding() {
        byte[] bytes = StringUtilities.getBytes("test", "UTF-8");
        assertArrayEquals("test".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    public void testGetBytesUnsupportedEncoding() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Encoding (UNSUPPORTED) is not supported");
        StringUtilities.getBytes("test", "UNSUPPORTED");
    }

    @Test
    public void testCreateStringSupportedEncoding() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        String result = StringUtilities.createString(data, "UTF-8");
        assertEquals("hello", result);
    }

    @Test
    public void testCreateStringUnsupportedEncoding() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Encoding (UNSUPPORTED) is not supported");
        StringUtilities.createString(new byte[]{0x61}, "UNSUPPORTED");
    }

    /* ---------------------------------------------------------------------- */
    /* 13. UTF‑8 helpers (createUtf8String, createUTF8String, getUTF8Bytes)   */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCreateUtf8StringDeprecated() {
        byte[] data = "café".getBytes(StandardCharsets.UTF_8);
        String s = StringUtilities.createUtf8String(data);
        assertEquals("café", s);
    }

    @Test
    public void testCreateUTF8String() {
        byte[] data = "café".getBytes(StandardCharsets.UTF_8);
        String s = StringUtilities.createUTF8String(data);
        assertEquals("café", s);
    }

    @Test
    public void testGetUTF8Bytes() {
        byte[] expected = "café".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, StringUtilities.getUTF8Bytes("café"));
        assertNull(StringUtilities.getUTF8Bytes(null));
    }

    /* ---------------------------------------------------------------------- */
    /* 14. hashCodeIgnoreCase                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testHashCodeIgnoreCase() {
        String s1 = "AbCDe";
        String s2 = "abcde";

        // The hash code must be case‑insensitive
        assertEquals(StringUtilities.hashCodeIgnoreCase(s1), StringUtilities.hashCodeIgnoreCase(s2));

        // Compare with a manual lower‑case hash computation
        int manual = 0;
        for (char c : s2.toCharArray()) {
            manual = 31 * manual + Character.toLowerCase(c);
        }
        assertEquals(manual, StringUtilities.hashCodeIgnoreCase(s1));
    }

    @Test
    public void testHashCodeIgnoreCaseNull() {
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    /* ---------------------------------------------------------------------- */
    /* 15. trim / trimToEmpty / trimToNull / trimEmptyToDefault                */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testTrimMethods() {
        assertNull(StringUtilities.trim(null));
        assertEquals("abc", StringUtilities.trim("  abc  "));

        assertEquals("", StringUtilities.trimToEmpty(null));
        assertEquals("xyz", StringUtilities.trimToEmpty("  xyz  "));

        assertNull(StringUtilities.trimToNull("   "));
        assertEquals("data", StringUtilities.trimToNull("  data "));

        assertEquals("default", StringUtilities.trimEmptyToDefault("   ", "default"));
        assertEquals("value", StringUtilities.trimEmptyToDefault(" value ", "default"));
        assertEquals("fallback", StringUtilities.trimEmptyToDefault(null, "fallback"));
    }

    /* ---------------------------------------------------------------------- */
    /* 16. removeLeadingAndTrailingQuotes                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testRemoveLeadingAndTrailingQuotes() {
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"text\""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"text\"\""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"\"text\"\"\""));
        assertEquals("text \"inside\"", StringUtilities.removeLeadingAndTrailingQuotes("\"text \\\"inside\\\"\""));
        assertNull(StringUtilities.removeLeadingAndTrailingQuotes(null));
        assertEquals("", StringUtilities.removeLeadingAndTrailingQuotes(""));
    }

    /* ---------------------------------------------------------------------- */
    /* 17. commaSeparatedStringToSet                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testCommaSeparatedStringToSet() {
        Set<String> expected = new LinkedHashSet<>(Arrays.asList("apple", "banana", "cherry"));
        Set<String> actual = StringUtilities.commaSeparatedStringToSet(" apple , banana, cherry , apple ,  ");
        // The method uses Collectors.toSet() which does not guarantee order, so we compare via containsAll and size
        assertEquals(expected.size(), actual.size());
        assertTrue(actual.containsAll(expected));

        // Edge cases
        assertTrue(StringUtilities.commaSeparatedStringToSet(null).isEmpty());
        assertTrue(StringUtilities.commaSeparatedStringToSet("").isEmpty());
        assertTrue(StringUtilities.commaSeparatedStringToSet("   ").isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* 18. Miscellaneous – ensure coverage of all public methods               */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFOLDER_SEPARATORAndEMPTYConstants() {
        assertEquals("/", StringUtilities.FOLDER_SEPARATOR);
        assertEquals("", StringUtilities.EMPTY);
    }
}
