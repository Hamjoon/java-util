package com.cedarsoftware.util;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.junit.Assert.*;

public class StringUtilitiesTest {

    /* ---------- equals & equalsIgnoreCase ---------- */

    @Test
    public void testEqualsSimpleCases() {
        assertTrue(StringUtilities.equals((CharSequence) null, null));
        assertFalse(StringUtilities.equals((CharSequence) null, "a"));
        assertFalse(StringUtilities.equals("a", null));
        assertTrue(StringUtilities.equals("abc", "abc"));
        assertFalse(StringUtilities.equals("abc", "ABC"));
        assertTrue(StringUtilities.equals(new StringBuilder("xyz"), new StringBuilder("xyz")));
        assertFalse(StringUtilities.equals(new StringBuilder("xyz"), new StringBuilder("XYz")));
    }

    @Test
    public void testEqualsIgnoreCaseSimpleCases() {
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        assertFalse(StringUtilities.equalsIgnoreCase(null, "a"));
        assertFalse(StringUtilities.equalsIgnoreCase("a", null));
        assertTrue(StringUtilities.equalsIgnoreCase("abc", "ABC"));
        assertTrue(StringUtilities.equalsIgnoreCase(new StringBuilder("TeSt"), new StringBuilder("tEsT")));
        assertFalse(StringUtilities.equalsIgnoreCase("abc", "ab"));
    }

    /* ---------- regionMatches (package‑private) ---------- */

    @Test
    public void testRegionMatchesExactAndIgnoreCase() {
        CharSequence cs = "HelloWorld";
        CharSequence sub = "world";

        // exact match false because of case
        assertFalse(StringUtilities.regionMatches(cs, false, 5, sub, 0, 5));
        // ignore case true
        assertTrue(StringUtilities.regionMatches(cs, true, 5, sub, 0, 5));

        // out‑of‑range parameters
        assertFalse(StringUtilities.regionMatches(cs, true, -1, sub, 0, 5));
        assertFalse(StringUtilities.regionMatches(cs, true, 0, sub, -1, 5));
        assertFalse(StringUtilities.regionMatches(cs, true, 0, sub, 0, -1));
        assertFalse(StringUtilities.regionMatches(cs, true, 8, sub, 0, 5)); // not enough chars
    }

    /* ---------- equalsWithTrim / equalsIgnoreCaseWithTrim ---------- */

    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  abc  ", "abc"));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim(null, "abc"));
        assertFalse(StringUtilities.equalsWithTrim("abc", null));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  aBc  ", "ABC"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(null, null));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(null, "abc"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("abc", null));
    }

    /* ---------- isEmpty, isWhitespace, hasContent ---------- */

    @Test
    public void testIsEmptyAndIsWhitespace() {
        assertTrue(StringUtilities.isEmpty((CharSequence) null));
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
        assertTrue(StringUtilities.hasContent("  x "));
    }

    /* ---------- length & trimLength ---------- */

    @Test
    public void testLengthMethods() {
        assertEquals(0, StringUtilities.length((CharSequence) null));
        assertEquals(3, StringUtilities.length("abc"));
        assertEquals(0, StringUtilities.length((String) null));
    }

    @Test
    public void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(0, StringUtilities.trimLength("   "));
        assertEquals(3, StringUtilities.trimLength("  abc  "));
    }

    /* ---------- lastIndexOf ---------- */

    @Test
    public void testLastIndexOf() {
        assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        assertEquals(5, StringUtilities.lastIndexOf("a/b/c/d", '/'));
        assertEquals(0, StringUtilities.lastIndexOf("/", '/'));
        assertEquals(-1, StringUtilities.lastIndexOf("abc", 'z'));
    }

    /* ---------- decode & encode ---------- */

    @Test
    public void testDecodeEvenLength() {
        byte[] result = StringUtilities.decode("0A1B");
        assertArrayEquals(new byte[]{0x0A, 0x1B}, result);
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(StringUtilities.decode("ABC"));
    }

    @Test
    public void testEncodeBytes() {
        byte[] data = new byte[]{(byte)0xFF, 0x00, 0x0A, 0x1B};
        String hex = StringUtilities.encode(data);
        assertEquals("FF000A1B", hex);
    }

    /* ---------- count (String & CharSequence) ---------- */

    @Test
    public void testCountString() {
        assertEquals(3, StringUtilities.count("banana", 'a'));
        assertEquals(0, StringUtilities.count("", 'a'));
        assertEquals(0, StringUtilities.count(null, 'a'));
    }

    @Test
    public void testCountCharSequence() {
        assertEquals(2, StringUtilities.count("ababab", "ab"));
        assertEquals(0, StringUtilities.count("xxxx", "ab"));
        assertEquals(0, StringUtilities.count(null, "ab"));
        assertEquals(0, StringUtilities.count("abc", null));
        assertEquals(0, StringUtilities.count("abc", ""));
    }

    /* ---------- wildcardToRegexString ---------- */

    @Test
    public void testWildcardToRegexString() {
        String pattern = StringUtilities.wildcardToRegexString("file-?.txt");
        assertEquals("^file-..txt$", pattern);
        // Verify that the generated regex works as expected
        assertTrue("file-a.txt".matches(pattern));
        assertFalse("file-aa.txt".matches(pattern));
    }

    /* ---------- levenshteinDistance ---------- */

    @Test
    public void testLevenshteinDistanceBasic() {
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(5, StringUtilities.levenshteinDistance(null, "hello"));
        assertEquals(5, StringUtilities.levenshteinDistance("hello", null));
        assertEquals(0, StringUtilities.levenshteinDistance(null, null));
    }

    /* ---------- damerauLevenshteinDistance ---------- */

    @Test
    public void testDamerauLevenshteinDistanceBasic() {
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("book", "back")); // transposition + substitution
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("ca", "abc"));
        assertEquals(0, StringUtilities.damerauLevenshteinDistance(null, null));
        assertEquals(4, StringUtilities.damerauLevenshteinDistance(null, "test"));
        assertEquals(4, StringUtilities.damerauLevenshteinDistance("test", null));
    }

    /* ---------- getRandomString & getRandomChar ---------- */

    @Test
    public void testGetRandomStringDeterministic() {
        Random rnd = new Random(12345L); // fixed seed
        String s = StringUtilities.getRandomString(rnd, 5, 5);
        assertEquals(5, s.length());
        // First character must be uppercase, rest lowercase
        assertTrue(Character.isUpperCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            assertTrue(Character.isLowerCase(s.charAt(i)));
        }
        // Verify reproducibility with same seed
        Random rnd2 = new Random(12345L);
        assertEquals(s, StringUtilities.getRandomString(rnd2, 5, 5));
    }

    @Test
    public void testGetRandomCharUpperAndLower() {
        Random rnd = new Random(1L);
        String upper = StringUtilities.getRandomChar(rnd, true);
        String lower = StringUtilities.getRandomChar(rnd, false);
        assertEquals(1, upper.length());
        assertEquals(1, lower.length());
        assertTrue(Character.isUpperCase(upper.charAt(0)));
        assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    /* ---------- getBytes / createString / UTF‑8 helpers ---------- */

    @Test
    public void testGetBytesAndCreateStringRoundTrip() {
        String original = "Café";
        byte[] utf8 = StringUtilities.getBytes(original, "UTF-8");
        assertNotNull(utf8);
        String restored = StringUtilities.createString(utf8, "UTF-8");
        assertEquals(original, restored);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBytesUnsupportedEncoding() {
        StringUtilities.getBytes("test", "UNSUPPORTED-ENCODING");
    }

    @Test
    public void testCreateUtf8StringAndGetUTF8Bytes() {
        String text = "hello";
        byte[] bytes = StringUtilities.getUTF8Bytes(text);
        String roundTrip = StringUtilities.createUtf8String(bytes);
        assertEquals(text, roundTrip);
    }

    /* ---------- hashCodeIgnoreCase ---------- */

    @Test
    public void testHashCodeIgnoreCase() {
        String a = "AbC";
        String b = "aBc";
        assertEquals(StringUtilities.hashCodeIgnoreCase(a), StringUtilities.hashCodeIgnoreCase(b));
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
        assertEquals("data", StringUtilities.trimToNull("  data "));

        assertEquals("def", StringUtilities.trimEmptyToDefault("  def  ", "default"));
        assertEquals("default", StringUtilities.trimEmptyToDefault("   ", "default"));
        assertEquals("default", StringUtilities.trimEmptyToDefault(null, "default"));
    }

    /* ---------- removeLeadingAndTrailingQuotes ---------- */

    @Test
    public void testRemoveLeadingAndTrailingQuotes() {
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"text\""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"text\"\""));
        assertEquals("text\"with\"quotes", StringUtilities.removeLeadingAndTrailingQuotes("\"text\"with\"quotes\""));
        assertNull(StringUtilities.removeLeadingAndTrailingQuotes(null));
        assertEquals("", StringUtilities.removeLeadingAndTrailingQuotes(""));
    }

    /* ---------- commaSeparatedStringToSet ---------- */

    @Test
    public void testCommaSeparatedStringToSet() {
        Set<String> set = StringUtilities.commaSeparatedStringToSet("a, b, c, a, ,");
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList("a", "b", "c")));

        assertTrue(StringUtilities.commaSeparatedStringToSet(null).isEmpty());
        assertTrue(StringUtilities.commaSeparatedStringToSet("   ").isEmpty());
    }

    /* ---------- Additional edge cases for count ---------- */

    @Test
    public void testCountEdgeCases() {
        // token longer than content
        assertEquals(0, StringUtilities.count("ab", "abc"));
        // overlapping occurrences
        assertEquals(3, StringUtilities.count("aaaa", "aa")); // aaaa -> positions 0,1,2
    }

    /* ---------- Ensure regionMatches works with empty strings ---------- */

    @Test
    public void testRegionMatchesEmptyStrings() {
        assertTrue(StringUtilities.regionMatches("", true, 0, "", 0, 0));
        assertFalse(StringUtilities.regionMatches("a", true, 0, "", 0, 1));
    }
}
