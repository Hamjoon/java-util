package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.io.UnsupportedEncodingException;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link StringUtilities}.
 */
public class StringUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  Public method signatures (extracted from StringUtilities)
     * ---------------------------------------------------------------------- */
    // boolean equals(CharSequence, CharSequence)
    // boolean equals(String, String)
    // boolean equalsIgnoreCase(CharSequence, CharSequence)
    // boolean equalsIgnoreCase(String, String)
    // boolean equalsWithTrim(String, String)
    // boolean equalsIgnoreCaseWithTrim(String, String)
    // boolean isEmpty(CharSequence)
    // boolean isEmpty(String)
    // boolean isWhitespace(CharSequence)
    // boolean hasContent(String)
    // int length(CharSequence)
    // int length(String)
    // int trimLength(String)
    // int lastIndexOf(String, char)
    // byte[] decode(String)
    // String encode(byte[])
    // int count(String, char)
    // int count(CharSequence, CharSequence)
    // String wildcardToRegexString(String)
    // int levenshteinDistance(CharSequence, CharSequence)
    // int damerauLevenshteinDistance(CharSequence, CharSequence)
    // String getRandomString(Random, int, int)
    // String getRandomChar(Random, boolean)
    // byte[] getBytes(String, String)
    // String createUtf8String(byte[])
    // byte[] getUTF8Bytes(String)
    // String createString(byte[], String)
    // String createUTF8String(byte[])
    // int hashCodeIgnoreCase(String)
    // String trim(String)
    // String trimToEmpty(String)
    // String trimToNull(String)
    // String trimEmptyToDefault(String, String)

    /* ----------------------------------------------------------------------
     *  BASIC FUNCTIONALITY TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsCharSequence() {
        assertTrue(StringUtilities.equals("abc", "abc"));
        assertFalse(StringUtilities.equals("abc", "abC"));
        assertFalse(StringUtilities.equals("abc", "abcd"));
    }

    @Test
    public void testEqualsString() {
        assertTrue(StringUtilities.equals((String) null, (String) null));
        assertFalse(StringUtilities.equals((String) null, "a"));
        assertTrue(StringUtilities.equals("test", "test"));
    }

    @Test
    public void testEqualsIgnoreCaseCharSequence() {
        assertTrue(StringUtilities.equalsIgnoreCase("AbC", "aBc"));
        assertFalse(StringUtilities.equalsIgnoreCase("AbC", "ab"));
    }

    @Test
    public void testEqualsIgnoreCaseString() {
        assertTrue(StringUtilities.equalsIgnoreCase((String) null, (String) null));
        assertFalse(StringUtilities.equalsIgnoreCase(null, "x"));
        assertTrue(StringUtilities.equalsIgnoreCase("Hello", "heLLo"));
    }

    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  foo  ", "foo"));
        assertFalse(StringUtilities.equalsWithTrim(" foo", "bar"));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim(null, " "));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  Foo ", "foo"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(" Foo ", "Bar"));
    }

    @Test
    public void testIsEmptyAndIsWhitespace() {
        assertTrue(StringUtilities.isEmpty((String) null));
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty("   "));
        assertFalse(StringUtilities.isEmpty(" a "));
        assertTrue(StringUtilities.isWhitespace("\t\n"));
        assertFalse(StringUtilities.isWhitespace("x"));
    }

    @Test
    public void testHasContent() {
        assertFalse(StringUtilities.hasContent(null));
        assertFalse(StringUtilities.hasContent("   "));
        assertTrue(StringUtilities.hasContent(" x "));
    }

    @Test
    public void testLength() {
        assertEquals(0, StringUtilities.length((String) null));
        assertEquals(3, StringUtilities.length("abc"));
        assertEquals(0, StringUtilities.length((CharSequence) null));
        assertEquals(5, StringUtilities.length((CharSequence) "hello"));
    }

    @Test
    public void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(3, StringUtilities.trimLength("  abc  "));
        assertEquals(0, StringUtilities.trimLength("   "));
    }

    @Test
    public void testLastIndexOf() {
        assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        assertEquals(5, StringUtilities.lastIndexOf("a/b/c/d/e/f", '/'));
        assertEquals(-1, StringUtilities.lastIndexOf("abcdef", '/'));
    }

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] original = new byte[]{0x00, 0x0F, (byte) 0xAB, (byte) 0xFF};
        String encoded = StringUtilities.encode(original);
        byte[] decoded = StringUtilities.decode(encoded);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(StringUtilities.decode("ABC")); // length 3, odd
    }

    @Test
    public void testCountMethods() {
        assertEquals(3, StringUtilities.count("banana", 'a'));
        assertEquals(2, StringUtilities.count("ababab", "ab"));
        // overlapping case: "aaa" contains "aa" only once according to implementation
        assertEquals(1, StringUtilities.count("aaa", "aa"));
        // null handling
        assertEquals(0, StringUtilities.count((String) null, 'a'));
        assertEquals(0, StringUtilities.count("test", (CharSequence) null));
    }

    @Test
    public void testWildcardToRegexString() {
        String regex = StringUtilities.wildcardToRegexString("file*?.[ch]");
        // Expected: ^file.*.\\.[ch]$  (the '.' after * is escaped because of '?')
        assertEquals("^file.*.\\.[ch]$", regex);
    }

    @Test
    public void testLevenshteinDistance() {
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        assertEquals(0, StringUtilities.levenshteinDistance(null, null));
        assertEquals(5, StringUtilities.levenshteinDistance(null, "hello"));
        assertEquals(5, StringUtilities.levenshteinDistance("hello", null));
    }

    @Test
    public void testDamerauLevenshteinDistance() {
        // transposition: "ca" -> "ac" should be distance 1
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("ca", "ac"));
        // normal case
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("kitten", "sitting"));
    }

    @Test
    public void testRandomStringGeneration() {
        Random rnd = new Random(12345L);
        String random = StringUtilities.getRandomString(rnd, 5, 10);
        assertTrue(random.length() >= 5 && random.length() <= 10);
        // first character must be uppercase
        assertTrue(Character.isUpperCase(random.charAt(0)));
        // subsequent characters must be lowercase
        for (int i = 1; i < random.length(); i++) {
            assertTrue(Character.isLowerCase(random.charAt(i)));
        }
    }

    @Test
    public void testRandomCharGeneration() {
        Random rnd = new Random(1L);
        String upper = StringUtilities.getRandomChar(rnd, true);
        String lower = StringUtilities.getRandomChar(rnd, false);
        assertEquals(1, upper.length());
        assertEquals(1, lower.length());
        assertTrue(Character.isUpperCase(upper.charAt(0)));
        assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    @Test
    public void testGetBytesAndCreateStringValidEncoding() {
        byte[] utf8 = StringUtilities.getBytes("hello", "UTF-8");
        assertNotNull(utf8);
        String reconstructed = StringUtilities.createString(utf8, "UTF-8");
        assertEquals("hello", reconstructed);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBytesUnsupportedEncodingThrows() {
        StringUtilities.getBytes("test", "UNSUPPORTED_ENCODING");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStringUnsupportedEncodingThrows() {
        StringUtilities.createString(new byte[]{0x61}, "UNSUPPORTED_ENCODING");
    }

    @Test
    public void testCreateUtf8StringAndGetUTF8BytesRoundTrip() {
        byte[] bytes = StringUtilities.getUTF8Bytes("π"); // Greek pi, multibyte in UTF‑8
        String s = StringUtilities.createUtf8String(bytes);
        assertEquals("π", s);
    }

    @Test
    public void testHashCodeIgnoreCase() {
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
        String a = "AbC";
        String b = "aBc";
        assertEquals(StringUtilities.hashCodeIgnoreCase(a), StringUtilities.hashCodeIgnoreCase(b));
    }

    @Test
    public void testTrimMethods() {
        assertNull(StringUtilities.trim(null));
        assertEquals("abc", StringUtilities.trim("  abc  "));
        assertEquals("", StringUtilities.trimToEmpty(null));
        assertEquals("xyz", StringUtilities.trimToEmpty("  xyz  "));
        assertNull(StringUtilities.trimToNull("   "));
        assertEquals("z", StringUtilities.trimToNull("  z  "));
        assertEquals("def", StringUtilities.trimEmptyToDefault(null, "def"));
        assertEquals("ghi", StringUtilities.trimEmptyToDefault("   ghi   ", "def"));
        assertEquals("jkl", StringUtilities.trimEmptyToDefault("jkl", "def"));
    }

    /* ----------------------------------------------------------------------
     *  EDGE‑CASE / EXCEPTION TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsEdgeCases() {
        // Both null
        assertTrue(StringUtilities.equals((CharSequence) null, null));
        // One null, one empty
        assertFalse(StringUtilities.equals((CharSequence) null, ""));
        // Different lengths
        assertFalse(StringUtilities.equals("abc", "ab"));
    }

    @Test
    public void testEqualsIgnoreCaseEdgeCases() {
        // Null handling
        assertTrue(StringUtilities.equalsIgnoreCase((CharSequence) null, null));
        assertFalse(StringUtilities.equalsIgnoreCase((CharSequence) null, "a"));
        // Length mismatch
        assertFalse(StringUtilities.equalsIgnoreCase("ab", "abc"));
    }

    @Test
    public void testIsEmptyEdgeCases() {
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty("   "));
        assertFalse(StringUtilities.isEmpty(" x "));
    }

    @Test
    public void testCountEdgeCases() {
        // token empty
        assertEquals(0, StringUtilities.count("anything", ""));
        // content empty
        assertEquals(0, StringUtilities.count("", "a"));
        // both null
        assertEquals(0, StringUtilities.count(null, null));
    }

    @Test
    public void testDecodeInvalidHexCharacters() {
        // Character.digit returns -1 for non‑hex; the method will still compute a byte.
        // We verify that the method does not throw and returns a byte array of expected length.
        byte[] result = StringUtilities.decode("ZZ");
        assertNotNull(result);
        assertEquals(1, result.length);
        // The value will be ((-1)*16 + -1) == -17 (mod 256 => 239)
        assertEquals((byte) 0xEF, result[0]);
    }

    @Test
    public void testWildcardEscapingSpecialChars() {
        String regex = StringUtilities.wildcardToRegexString("file(1)[test].*");
        // Expected: ^file\\(1\\)\\[test\\]\\..*$   (the '.' before '*' is escaped)
        assertEquals("^file\\(1\\)\\[test\\]\\.\\*$", regex);
    }

    @Test
    public void testLevenshteinDistanceWithEmptyStrings() {
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(4, StringUtilities.levenshteinDistance("", "test"));
        assertEquals(4, StringUtilities.levenshteinDistance("test", ""));
    }

    @Test
    public void testDamerauLevenshteinDistanceNullAndEmpty() {
        assertEquals(0, StringUtilities.damerauLevenshteinDistance(null, null));
        assertEquals(5, StringUtilities.damerauLevenshteinDistance(null, "hello"));
        assertEquals(5, StringUtilities.damerauLevenshteinDistance("world", null));
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
    }

    @Test
    public void testGetRandomStringLengthBounds() {
        Random rnd = new Random(42L);
        String s = StringUtilities.getRandomString(rnd, 3, 3);
        assertEquals(3, s.length());
    }

    @Test
    public void testTrimToEmptyWhenStringIsNull() {
        assertEquals(StringUtilities.EMPTY, StringUtilities.trimToEmpty(null));
    }

    @Test
    public void testTrimToNullWhenStringBecomesEmpty() {
        assertNull(StringUtilities.trimToNull("   "));
    }

    @Test
    public void testTrimEmptyToDefaultWhenValueIsNull() {
        String def = "default";
        assertEquals(def, StringUtilities.trimEmptyToDefault(null, def));
    }

    @Test
    public void testTrimEmptyToDefaultWhenValueIsWhitespace() {
        String def = "default";
        assertEquals(def, StringUtilities.trimEmptyToDefault("   ", def));
    }

    @Test
    public void testTrimEmptyToDefaultWhenValueIsNonEmpty() {
        String def = "default";
        assertEquals("val", StringUtilities.trimEmptyToDefault("  val  ", def));
    }
}
