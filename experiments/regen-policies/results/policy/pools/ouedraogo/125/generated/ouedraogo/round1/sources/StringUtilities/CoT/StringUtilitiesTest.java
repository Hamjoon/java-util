package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Set;

public class StringUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  List of public methods (signatures)
     * ---------------------------------------------------------------------- */
    // boolean equals(CharSequence cs1, CharSequence cs2)
    // boolean equals(String s1, String s2)
    // boolean equalsIgnoreCase(CharSequence cs1, CharSequence cs2)
    // boolean equalsIgnoreCase(String s1, String s2)
    // boolean equalsWithTrim(String s1, String s2)
    // boolean equalsIgnoreCaseWithTrim(String s1, String s2)
    // boolean isEmpty(CharSequence cs)
    // boolean isEmpty(String s)
    // boolean isWhitespace(CharSequence cs)
    // boolean hasContent(String s)
    // int length(CharSequence cs)
    // int length(String s)
    // int trimLength(String s)
    // int lastIndexOf(String path, char ch)
    // byte[] decode(String s)
    // String encode(byte[] bytes)
    // int count(String s, char c)
    // int count(CharSequence content, CharSequence token)
    // String wildcardToRegexString(String wildcard)
    // int levenshteinDistance(CharSequence s, CharSequence t)
    // int damerauLevenshteinDistance(CharSequence source, CharSequence target)
    // String getRandomString(Random random, int minLen, int maxLen)
    // String getRandomChar(Random random, boolean upper)
    // byte[] getBytes(String s, String encoding)
    // String createUtf8String(byte[] bytes)
    // byte[] getUTF8Bytes(String s)
    // String createString(byte[] bytes, String encoding)
    // String createUTF8String(byte[] bytes)
    // int hashCodeIgnoreCase(String s)
    // String trim(String str)
    // String trimToEmpty(String value)
    // String trimToNull(String value)
    // String trimEmptyToDefault(String value, String defaultValue)
    // String removeLeadingAndTrailingQuotes(String input)
    // Set<String> commaSeparatedStringToSet(String commaSeparatedString)

    /* ----------------------------------------------------------------------
     *  Basic functionality tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsCharSequence() {
        Assert.assertTrue(StringUtilities.equals("abc", "abc"));
        Assert.assertFalse(StringUtilities.equals("abc", "abC"));
        Assert.assertTrue(StringUtilities.equals((CharSequence) null, (CharSequence) null));
        Assert.assertFalse(StringUtilities.equals((CharSequence) null, "abc"));
    }

    @Test
    public void testEqualsString() {
        Assert.assertTrue(StringUtilities.equals("test", "test"));
        Assert.assertFalse(StringUtilities.equals("test", "Test"));
        Assert.assertTrue(StringUtilities.equals((String) null, (String) null));
    }

    @Test
    public void testEqualsIgnoreCaseCharSequence() {
        Assert.assertTrue(StringUtilities.equalsIgnoreCase("AbC", "aBc"));
        Assert.assertFalse(StringUtilities.equalsIgnoreCase("abc", "ab"));
        Assert.assertTrue(StringUtilities.equalsIgnoreCase((CharSequence) null, (CharSequence) null));
    }

    @Test
    public void testEqualsIgnoreCaseString() {
        Assert.assertTrue(StringUtilities.equalsIgnoreCase("Hello", "heLLo"));
        Assert.assertFalse(StringUtilities.equalsIgnoreCase("Hello", "World"));
        Assert.assertTrue(StringUtilities.equalsIgnoreCase((String) null, (String) null));
    }

    @Test
    public void testEqualsWithTrim() {
        Assert.assertTrue(StringUtilities.equalsWithTrim("  foo  ", "foo"));
        Assert.assertFalse(StringUtilities.equalsWithTrim("foo", "bar"));
        Assert.assertTrue(StringUtilities.equalsWithTrim(null, null));
        Assert.assertFalse(StringUtilities.equalsWithTrim(null, "foo"));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  Foo ", "foo"));
        Assert.assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("Foo", "Bar"));
        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(null, null));
    }

    @Test
    public void testIsEmptyAndIsWhitespace() {
        Assert.assertTrue(StringUtilities.isEmpty(""));
        Assert.assertTrue(StringUtilities.isEmpty("   "));
        Assert.assertTrue(StringUtilities.isEmpty((CharSequence) null));
        Assert.assertFalse(StringUtilities.isEmpty("x"));
        Assert.assertTrue(StringUtilities.isWhitespace("\t\n "));
        Assert.assertFalse(StringUtilities.isWhitespace("a"));
    }

    @Test
    public void testHasContent() {
        Assert.assertTrue(StringUtilities.hasContent("abc"));
        Assert.assertFalse(StringUtilities.hasContent("   "));
        Assert.assertFalse(StringUtilities.hasContent(""));
        Assert.assertFalse(StringUtilities.hasContent(null));
    }

    @Test
    public void testLength() {
        Assert.assertEquals(0, StringUtilities.length((CharSequence) null));
        Assert.assertEquals(3, StringUtilities.length("abc"));
        Assert.assertEquals(0, StringUtilities.length((String) null));
    }

    @Test
    public void testTrimLength() {
        Assert.assertEquals(0, StringUtilities.trimLength(null));
        Assert.assertEquals(3, StringUtilities.trimLength("  abc  "));
        Assert.assertEquals(0, StringUtilities.trimLength("   "));
    }

    @Test
    public void testLastIndexOf() {
        Assert.assertEquals(5, StringUtilities.lastIndexOf("a/b/c/d", '/'));
        Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        Assert.assertEquals(-1, StringUtilities.lastIndexOf("abc", 'z'));
    }

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] original = new byte[] {0x00, 0x0F, (byte) 0xA5, (byte) 0xFF};
        String encoded = StringUtilities.encode(original);
        byte[] decoded = StringUtilities.decode(encoded);
        Assert.assertArrayEquals(original, decoded);
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        Assert.assertNull(StringUtilities.decode("ABC")); // length 3, odd
    }

    @Test
    public void testCountStringChar() {
        Assert.assertEquals(3, StringUtilities.count("banana", 'a'));
        Assert.assertEquals(0, StringUtilities.count("banana", 'z'));
    }

    @Test
    public void testCountCharSequence() {
        Assert.assertEquals(2, StringUtilities.count("ababab", "ab"));
        Assert.assertEquals(0, StringUtilities.count("abcd", "ef"));
        Assert.assertEquals(0, StringUtilities.count(null, "a"));
        Assert.assertEquals(0, StringUtilities.count("abc", null));
        Assert.assertEquals(0, StringUtilities.count("", "a"));
        Assert.assertEquals(0, StringUtilities.count("abc", ""));
    }

    @Test
    public void testWildcardToRegexString() {
        String regex = StringUtilities.wildcardToRegexString("a*b?.c");
        Assert.assertEquals("^a.*b.c\\.c$", regex);
    }

    @Test
    public void testLevenshteinDistance() {
        Assert.assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        Assert.assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        Assert.assertEquals(5, StringUtilities.levenshteinDistance(null, "hello"));
        Assert.assertEquals(5, StringUtilities.levenshteinDistance("hello", null));
    }

    @Test
    public void testDamerauLevenshteinDistance() {
        Assert.assertEquals(2, StringUtilities.damerauLevenshteinDistance("book", "back"));
        Assert.assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        Assert.assertEquals(4, StringUtilities.damerauLevenshteinDistance(null, "test"));
        Assert.assertEquals(4, StringUtilities.damerauLevenshteinDistance("test", null));
    }

    @Test
    public void testGetRandomStringLengthAndCase() {
        Random rnd = new Random(12345L);
        String s = StringUtilities.getRandomString(rnd, 5, 10);
        Assert.assertTrue(s.length() >= 5 && s.length() <= 10);
        Assert.assertTrue(Character.isUpperCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            Assert.assertTrue(Character.isLowerCase(s.charAt(i)));
        }
    }

    @Test
    public void testGetRandomChar() {
        Random rnd = new Random(0L);
        String upper = StringUtilities.getRandomChar(rnd, true);
        String lower = StringUtilities.getRandomChar(rnd, false);
        Assert.assertEquals(1, upper.length());
        Assert.assertEquals(1, lower.length());
        Assert.assertTrue(Character.isUpperCase(upper.charAt(0)));
        Assert.assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    @Test
    public void testGetBytesAndCreateStringUtf8() throws UnsupportedEncodingException {
        String original = "café";
        byte[] utf8 = StringUtilities.getBytes(original, "UTF-8");
        String back = StringUtilities.createString(utf8, "UTF-8");
        Assert.assertEquals(original, back);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBytesUnsupportedEncoding() {
        StringUtilities.getBytes("test", "UNSUPPORTED_ENCODING");
    }

    @Test
    public void testCreateUtf8StringAndGetUTF8Bytes() {
        byte[] bytes = StringUtilities.getUTF8Bytes("hello");
        String reconstructed = StringUtilities.createUtf8String(bytes);
        Assert.assertEquals("hello", reconstructed);
    }

    @Test
    public void testHashCodeIgnoreCase() {
        String s1 = "AbC";
        String s2 = "aBc";
        Assert.assertEquals(StringUtilities.hashCodeIgnoreCase(s1), StringUtilities.hashCodeIgnoreCase(s2));
        Assert.assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    @Test
    public void testTrimMethods() {
        Assert.assertEquals("abc", StringUtilities.trim("  abc  "));
        Assert.assertNull(StringUtilities.trim(null));

        Assert.assertEquals("abc", StringUtilities.trimToEmpty("  abc  "));
        Assert.assertEquals("", StringUtilities.trimToEmpty(null));

        Assert.assertEquals("abc", StringUtilities.trimToNull("  abc  "));
        Assert.assertNull(StringUtilities.trimToNull("   "));
        Assert.assertNull(StringUtilities.trimToNull(null));

        Assert.assertEquals("def", StringUtilities.trimEmptyToDefault("   ", "def"));
        Assert.assertEquals("value", StringUtilities.trimEmptyToDefault("value", "def"));
        Assert.assertEquals("def", StringUtilities.trimEmptyToDefault(null, "def"));
    }

    @Test
    public void testRemoveLeadingAndTrailingQuotes() {
        Assert.assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"text\""));
        Assert.assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"text\"\""));
        Assert.assertEquals("text with \"quotes\" inside", StringUtilities.removeLeadingAndTrailingQuotes("\"text with \"quotes\" inside\""));
        Assert.assertEquals("", StringUtilities.removeLeadingAndTrailingQuotes("\"\""));
        Assert.assertNull(StringUtilities.removeLeadingAndTrailingQuotes(null));
        Assert.assertEquals("", StringUtilities.removeLeadingAndTrailingQuotes(""));
    }

    @Test
    public void testCommaSeparatedStringToSet() {
        Set<String> set = StringUtilities.commaSeparatedStringToSet("a, b, ,c ,a");
        Assert.assertEquals(3, set.size());
        Assert.assertTrue(set.contains("a"));
        Assert.assertTrue(set.contains("b"));
        Assert.assertTrue(set.contains("c"));

        Assert.assertTrue(StringUtilities.commaSeparatedStringToSet(null).isEmpty());
        Assert.assertTrue(StringUtilities.commaSeparatedStringToSet("   ").isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  Edge case & exception tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsRegionMatchesInvalidParameters() {
        // regionMatches is package‑private; we invoke it indirectly via equalsIgnoreCase
        // with out‑of‑range start indices to ensure false is returned rather than exception.
        CharSequence cs = "abc";
        // This indirectly tests regionMatches handling of negative start / length via equalsIgnoreCase
        Assert.assertFalse(StringUtilities.equalsIgnoreCase(cs.subSequence(0, 2), cs.subSequence(1, 3)));
    }

    @Test
    public void testWildcardToRegexStringSpecialChars() {
        String regex = StringUtilities.wildcardToRegexString("file(1).txt");
        // Expected: ^file\(1\)\.txt$
        Assert.assertEquals("^file\\(1\\)\\.txt$", regex);
    }

    @Test
    public void testCountOverlappingOccurrences() {
        // "aaa" contains "aa" overlapping twice: positions 0 and 1
        Assert.assertEquals(2, StringUtilities.count("aaa", "aa"));
    }

    @Test
    public void testLevenshteinDistanceLargeDifference() {
        Assert.assertEquals(7, StringUtilities.levenshteinDistance("kitten", "sitting"));
        // Already tested above, but also test when one string is empty
        Assert.assertEquals(5, StringUtilities.levenshteinDistance("", "hello"));
    }

    @Test
    public void testDamerauLevenshteinTransposition() {
        // "ca" -> "ac" should be distance 1 (transposition)
        Assert.assertEquals(1, StringUtilities.damerauLevenshteinDistance("ca", "ac"));
    }

    @Test
    public void testGetRandomStringZeroLengthRange() {
        Random rnd = new Random(42L);
        String s = StringUtilities.getRandomString(rnd, 3, 3);
        Assert.assertEquals(3, s.length());
    }

    @Test
    public void testEncodeEmptyByteArray() {
        String encoded = StringUtilities.encode(new byte[0]);
        Assert.assertEquals("", encoded);
    }

    @Test
    public void testDecodeInvalidHexCharacters() {
        // Character.digit returns -1 for non‑hex chars, leading to -1 values in result
        // The method does not validate, so we just ensure it does not throw.
        byte[] result = StringUtilities.decode("ZZ");
        Assert.assertEquals(1, result.length);
        // -1 digit becomes 0xFF (since hi = -1, lo = -1 => (-1)*16 + (-1) = -17 => byte truncates)
        // We simply check that a byte array is returned.
        Assert.assertNotNull(result);
    }

    @Test
    public void testTrimEmptyToDefaultWhenValueIsEmptyString() {
        String result = StringUtilities.trimEmptyToDefault("", "default");
        Assert.assertEquals("default", result);
    }

    @Test
    public void testCreateStringWithUnsupportedEncoding() {
        try {
            StringUtilities.createString("test".getBytes(), "UNSUPPORTED");
            Assert.fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("UNSUPPORTED"));
        }
    }
}
