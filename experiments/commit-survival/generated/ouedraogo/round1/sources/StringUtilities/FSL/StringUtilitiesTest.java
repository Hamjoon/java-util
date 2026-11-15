package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.util.Random;

import org.junit.Test;

public class StringUtilitiesTest {

    /* ---------- equals & equalsIgnoreCase ---------- */

    @Test
    public void testEqualsBasic() {
        assertTrue(StringUtilities.equals((CharSequence) null, null));
        assertFalse(StringUtilities.equals((CharSequence) null, "a"));
        assertFalse(StringUtilities.equals("a", (CharSequence) null));
        assertTrue(StringUtilities.equals("abc", "abc"));
        assertFalse(StringUtilities.equals("abc", "ABC"));
        assertTrue(StringUtilities.equals(new StringBuilder("xyz"), new StringBuilder("xyz")));
        assertFalse(StringUtilities.equals(new StringBuilder("xyz"), new StringBuilder("XYz")));
    }

    @Test
    public void testEqualsStringOverload() {
        assertTrue(StringUtilities.equals((String) null, null));
        assertFalse(StringUtilities.equals((String) null, "test"));
        assertTrue(StringUtilities.equals("test", "test"));
        assertFalse(StringUtilities.equals("test", "Test"));
    }

    @Test
    public void testEqualsIgnoreCaseBasic() {
        assertTrue(StringUtilities.equalsIgnoreCase((CharSequence) null, null));
        assertFalse(StringUtilities.equalsIgnoreCase((CharSequence) null, "a"));
        assertTrue(StringUtilities.equalsIgnoreCase("abc", "ABC"));
        assertTrue(StringUtilities.equalsIgnoreCase(new StringBuilder("XyZ"), new StringBuilder("xYz")));
        assertFalse(StringUtilities.equalsIgnoreCase("abc", "ab"));
    }

    @Test
    public void testEqualsIgnoreCaseStringOverload() {
        assertTrue(StringUtilities.equalsIgnoreCase((String) null, null));
        assertFalse(StringUtilities.equalsIgnoreCase((String) null, "x"));
        assertTrue(StringUtilities.equalsIgnoreCase("MiXeD", "mixed"));
    }

    /* ---------- regionMatches ---------- */

    @Test
    public void testRegionMatchesExact() {
        CharSequence cs = "HelloWorld";
        CharSequence sub = "Hello";
        assertTrue(StringUtilities.regionMatches(cs, false, 0, sub, 0, 5));
        assertTrue(StringUtilities.regionMatches(cs, true, 0, sub, 0, 5));
    }

    @Test
    public void testRegionMatchesIgnoreCase() {
        CharSequence cs = "AbCdEf";
        CharSequence sub = "aBcDeF";
        assertTrue(StringUtilities.regionMatches(cs, true, 0, sub, 0, 6));
        assertFalse(StringUtilities.regionMatches(cs, false, 0, sub, 0, 6));
    }

    @Test
    public void testRegionMatchesInvalidParameters() {
        assertFalse(StringUtilities.regionMatches("abc", false, -1, "abc", 0, 3));
        assertFalse(StringUtilities.regionMatches("abc", false, 0, "abc", -1, 3));
        assertFalse(StringUtilities.regionMatches("abc", false, 0, "abc", 0, -1));
        assertFalse(StringUtilities.regionMatches("abc", false, 2, "abc", 0, 5)); // length too long
    }

    /* ---------- equalsWithTrim / equalsIgnoreCaseWithTrim ---------- */

    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  foo  ", "foo"));
        assertFalse(StringUtilities.equalsWithTrim("foo", " bar "));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim(null, " "));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  Foo ", "foo"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("Foo", "  bar"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(null, null));
    }

    /* ---------- isEmpty / isWhitespace / hasContent ---------- */

    @Test
    public void testIsEmptyAndWhitespace() {
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty((CharSequence) null));
        assertTrue(StringUtilities.isEmpty("   "));
        assertFalse(StringUtilities.isEmpty(" a "));
        assertTrue(StringUtilities.isWhitespace("\t\n"));
        assertFalse(StringUtilities.isWhitespace("x"));
    }

    @Test
    public void testHasContent() {
        assertFalse(StringUtilities.hasContent(""));
        assertFalse(StringUtilities.hasContent("   "));
        assertTrue(StringUtilities.hasContent("x"));
    }

    /* ---------- length & trimLength ---------- */

    @Test
    public void testLength() {
        assertEquals(0, StringUtilities.length((CharSequence) null));
        assertEquals(3, StringUtilities.length("abc"));
        assertEquals(0, StringUtilities.length((String) null));
        assertEquals(5, StringUtilities.length(new StringBuilder("hello")));
    }

    @Test
    public void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(0, StringUtilities.trimLength("   "));
        assertEquals(3, StringUtilities.trimLength("  abc "));
    }

    /* ---------- lastIndexOf ---------- */

    @Test
    public void testLastIndexOf() {
        assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        assertEquals(4, StringUtilities.lastIndexOf("a/b/c/d", '/'));
        assertEquals(-1, StringUtilities.lastIndexOf("abcd", '/'));
    }

    /* ---------- decode / encode ---------- */

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] data = new byte[] {0x00, 0x0F, (byte)0xAB, (byte)0xFF};
        String encoded = StringUtilities.encode(data);
        assertEquals("000FABFF", encoded);
        byte[] decoded = StringUtilities.decode(encoded);
        assertArrayEquals(data, decoded);
    }

    @Test
    public void testDecodeInvalidLength() {
        assertNull(StringUtilities.decode("ABC")); // odd length
    }

    @Test
    public void testDecodeNonHexCharacters() {
        // Character.digit returns -1 for non-hex, leading to unexpected values but still returns array
        byte[] result = StringUtilities.decode("zz");
        assertEquals(1, result.length);
        assertEquals((byte) -1, result[0]); // hi=-1, lo=-1 -> (-1*16)+(-1) = -17 -> truncated to byte -17 == 239? actually byte casts.
    }

    /* ---------- count ---------- */

    @Test
    public void testCountCharAndString() {
        assertEquals(3, StringUtilities.count("banana", 'a'));
        assertEquals(0, StringUtilities.count("banana", 'z'));
        assertEquals(2, StringUtilities.count("ababab", "ab"));
        assertEquals(0, StringUtilities.count(null, "a"));
        assertEquals(0, StringUtilities.count("abc", null));
        assertEquals(0, StringUtilities.count("", "a"));
        assertEquals(0, StringUtilities.count("abc", ""));
    }

    /* ---------- wildcardToRegexString ---------- */

    @Test
    public void testWildcardToRegexString() {
        String regex = StringUtilities.wildcardToRegexString("file-?.*");
        assertEquals("^file-..*$", regex);
        assertTrue("file-a.txt".matches(regex));
        assertFalse("file-aa.txt".matches(regex));
    }

    @Test
    public void testWildcardEscaping() {
        String regex = StringUtilities.wildcardToRegexString("a+b(c)");
        // '+' is not special in our conversion, '(' and ')' should be escaped
        assertEquals("^a+b\\(c\\)$", regex);
        assertTrue("a+b(c)".matches(regex));
    }

    /* ---------- levenshteinDistance ---------- */

    @Test
    public void testLevenshteinDistanceBasic() {
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(3, StringUtilities.levenshteinDistance("", "abc"));
        assertEquals(3, StringUtilities.levenshteinDistance("abc", ""));
        assertEquals(1, StringUtilities.levenshteinDistance("kitten", "sitten"));
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
    }

    @Test
    public void testLevenshteinWithNulls() {
        assertEquals(0, StringUtilities.levenshteinDistance(null, null));
        assertEquals(5, StringUtilities.levenshteinDistance(null, "hello"));
        assertEquals(5, StringUtilities.levenshteinDistance("hello", null));
    }

    /* ---------- damerauLevenshteinDistance ---------- */

    @Test
    public void testDamerauLevenshteinBasic() {
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("ab", "ba")); // transposition
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("ca", "abc"));
    }

    @Test
    public void testDamerauLevenshteinWithNulls() {
        assertEquals(0, StringUtilities.damerauLevenshteinDistance(null, null));
        assertEquals(4, StringUtilities.damerauLevenshteinDistance(null, "test"));
        assertEquals(4, StringUtilities.damerauLevenshteinDistance("test", null));
    }

    /* ---------- getRandomString / getRandomChar ---------- */

    @Test
    public void testGetRandomStringLengthBounds() {
        Random rnd = new Random(12345L);
        String s = StringUtilities.getRandomString(rnd, 5, 5);
        assertEquals(5, s.length());
        assertTrue(Character.isUpperCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            assertTrue(Character.isLowerCase(s.charAt(i)));
        }
    }

    @Test
    public void testGetRandomCharUpperAndLower() {
        Random rnd = new Random(0);
        String upper = StringUtilities.getRandomChar(rnd, true);
        String lower = StringUtilities.getRandomChar(rnd, false);
        assertEquals(1, upper.length());
        assertEquals(1, lower.length());
        assertTrue(Character.isUpperCase(upper.charAt(0)));
        assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    /* ---------- getBytes / createString / UTF-8 helpers ---------- */

    @Test
    public void testGetBytesAndCreateStringRoundTrip() {
        String original = "café";
        byte[] utf8 = StringUtilities.getUTF8Bytes(original);
        String round = StringUtilities.createUtf8String(utf8);
        assertEquals(original, round);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBytesUnsupportedEncoding() {
        StringUtilities.getBytes("test", "UNSUPPORTED-ENCODING");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStringUnsupportedEncoding() {
        StringUtilities.createString(new byte[]{0x61}, "UNSUPPORTED-ENCODING");
    }

    /* ---------- hashCodeIgnoreCase ---------- */

    @Test
    public void testHashCodeIgnoreCase() {
        String s1 = "AbC";
        String s2 = "aBc";
        assertEquals(StringUtilities.hashCodeIgnoreCase(s1), StringUtilities.hashCodeIgnoreCase(s2));
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    /* ---------- trim, trimToEmpty, trimToNull, trimEmptyToDefault ---------- */

    @Test
    public void testTrimMethods() {
        assertNull(StringUtilities.trim(null));
        assertEquals("abc", StringUtilities.trim("  abc  "));
        assertEquals("", StringUtilities.trimToEmpty(null));
        assertEquals("xyz", StringUtilities.trimToEmpty("  xyz "));
        assertNull(StringUtilities.trimToNull("   "));
        assertEquals("data", StringUtilities.trimToNull(" data "));
        assertEquals("def", StringUtilities.trimEmptyToDefault(null, "def"));
        assertEquals("ghi", StringUtilities.trimEmptyToDefault("  ghi  ", "def"));
        assertEquals("jkl", StringUtilities.trimEmptyToDefault("jkl", "def"));
    }
}
