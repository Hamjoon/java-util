package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import static org.junit.Assert.*;

public class StringUtilitiesTest {

    private Random seededRandom;

    @Before
    public void setUp() {
        // Fixed seed for deterministic random tests
        seededRandom = new Random(12345L);
    }

    /* ---------- equals & equalsIgnoreCase ---------- */

    @Test
    public void testEqualsBasic() {
        assertTrue(StringUtilities.equals("abc", "abc"));
        assertFalse(StringUtilities.equals("abc", "ABC"));
        assertFalse(StringUtilities.equals("abc", null));
        assertFalse(StringUtilities.equals(null, "abc"));
        assertTrue(StringUtilities.equals(null, null));

        // CharSequence implementations other than String
        CharSequence cs1 = new StringBuilder("xyz");
        CharSequence cs2 = new StringBuffer("xyz");
        assertTrue(StringUtilities.equals(cs1, cs2));
        CharSequence cs3 = new StringBuilder("xyZ");
        assertFalse(StringUtilities.equals(cs1, cs3));
    }

    @Test
    public void testEqualsIgnoreCaseBasic() {
        assertTrue(StringUtilities.equalsIgnoreCase("abc", "ABC"));
        assertTrue(StringUtilities.equalsIgnoreCase("AbC", "aBc"));
        assertFalse(StringUtilities.equalsIgnoreCase("abc", "ab"));
        assertFalse(StringUtilities.equalsIgnoreCase(null, "abc"));
        assertFalse(StringUtilities.equalsIgnoreCase("abc", null));
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));

        CharSequence cs1 = new StringBuilder("Test");
        CharSequence cs2 = new StringBuffer("tEST");
        assertTrue(StringUtilities.equalsIgnoreCase(cs1, cs2));
    }

    @Test
    public void testRegionMatchesEdgeCases() {
        // Directly test regionMatches via equalsIgnoreCase (which uses it)
        assertTrue(StringUtilities.equalsIgnoreCase("abcd", "ABCD"));
        // Different lengths
        assertFalse(StringUtilities.equalsIgnoreCase("abcd", "ABC"));
        // Null handling already covered
    }

    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  hello  ", "hello"));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim(null, "hello"));
        assertFalse(StringUtilities.equalsWithTrim("hello", null));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  HeLLo  ", "hello"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(null, null));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(null, "hello"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("hello", null));
    }

    /* ---------- isEmpty / isWhitespace / hasContent ---------- */

    @Test
    public void testIsEmptyAndWhitespace() {
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty("   "));
        assertTrue(StringUtilities.isEmpty(null));
        assertFalse(StringUtilities.isEmpty(" a "));
        assertTrue(StringUtilities.isWhitespace(" \t\n"));
        assertFalse(StringUtilities.isWhitespace("a"));
    }

    @Test
    public void testHasContent() {
        assertTrue(StringUtilities.hasContent("x"));
        assertFalse(StringUtilities.hasContent("   "));
        assertFalse(StringUtilities.hasContent(""));
        assertFalse(StringUtilities.hasContent(null));
    }

    /* ---------- length & trimLength ---------- */

    @Test
    public void testLength() {
        assertEquals(0, StringUtilities.length((String) null));
        assertEquals(0, StringUtilities.length(""));
        assertEquals(3, StringUtilities.length("abc"));
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
        assertEquals(5, StringUtilities.lastIndexOf("a/b/c/d/e/f", '/'));
        assertEquals(0, StringUtilities.lastIndexOf("a", 'a'));
    }

    /* ---------- encode / decode ---------- */

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] original = new byte[]{0x01, (byte) 0xAB, 0x00, 0x7F};
        String encoded = StringUtilities.encode(original);
        byte[] decoded = StringUtilities.decode(encoded);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void testDecodeInvalidLength() {
        assertNull(StringUtilities.decode("ABC")); // odd length
    }

    @Test
    public void testDecodeValidHex() {
        byte[] expected = new byte[]{0x0F, (byte) 0xA0, 0x55};
        assertArrayEquals(expected, StringUtilities.decode("0FA055"));
    }

    /* ---------- count ---------- */

    @Test
    public void testCountCharAndString() {
        assertEquals(3, StringUtilities.count("banana", 'a'));
        assertEquals(2, StringUtilities.count("banana", "an"));
        assertEquals(0, StringUtilities.count(null, "a"));
        assertEquals(0, StringUtilities.count("test", null));
        assertEquals(0, StringUtilities.count("", "a"));
        assertEquals(0, StringUtilities.count("abc", ""));
    }

    @Test
    public void testCountOverlapping() {
        // Overlapping occurrences: "aaa" contains "aa" twice (positions 0 and 1)
        assertEquals(2, StringUtilities.count("aaa", "aa"));
    }

    /* ---------- wildcardToRegexString ---------- */

    @Test
    public void testWildcardToRegexString() {
        String pattern = StringUtilities.wildcardToRegexString("file-?.txt");
        assertEquals("^file-..txt$", pattern); // '?' becomes '.' and other chars stay

        pattern = StringUtilities.wildcardToRegexString("data*backup?.zip");
        assertEquals("^data.*backup..zip$", pattern);
    }

    /* ---------- levenshteinDistance ---------- */

    @Test
    public void testLevenshteinDistance() {
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(5, StringUtilities.levenshteinDistance(null, "hello"));
        assertEquals(5, StringUtilities.levenshteinDistance("hello", null));
        assertEquals(2, StringUtilities.levenshteinDistance("ab", "abc"));
    }

    /* ---------- damerauLevenshteinDistance ---------- */

    @Test
    public void testDamerauLevenshteinDistance() {
        // Simple transposition case: "ca" -> "ac" distance 1
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("ca", "ac"));
        // Example from javadoc
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("book", "back"));
        // Null / empty handling
        assertEquals(4, StringUtilities.damerauLevenshteinDistance(null, "test"));
        assertEquals(4, StringUtilities.damerauLevenshteinDistance("test", null));
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
    }

    /* ---------- Random string generation ---------- */

    @Test
    public void testGetRandomChar() {
        String upper = StringUtilities.getRandomChar(seededRandom, true);
        String lower = StringUtilities.getRandomChar(seededRandom, false);
        assertEquals(1, upper.length());
        assertEquals(1, lower.length());
        assertTrue(Character.isUpperCase(upper.charAt(0)));
        assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    @Test
    public void testGetRandomStringBoundsAndCase() {
        int min = 5;
        int max = 10;
        String randomStr = StringUtilities.getRandomString(seededRandom, min, max);
        assertNotNull(randomStr);
        assertTrue(randomStr.length() >= min && randomStr.length() <= max);
        // First character uppercase, rest lowercase
        assertTrue(Character.isUpperCase(randomStr.charAt(0)));
        for (int i = 1; i < randomStr.length(); i++) {
            assertTrue(Character.isLowerCase(randomStr.charAt(i)));
        }
    }

    /* ---------- Byte encoding utilities ---------- */

    @Test
    public void testGetBytesAndCreateStringUtf8() throws Exception {
        String original = "Hello, 世界";
        byte[] utf8Bytes = StringUtilities.getBytes(original, "UTF-8");
        assertNotNull(utf8Bytes);
        String roundTrip = StringUtilities.createString(utf8Bytes, "UTF-8");
        assertEquals(original, roundTrip);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBytesUnsupportedEncoding() {
        StringUtilities.getBytes("test", "NO-SUCH-ENCODING");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStringUnsupportedEncoding() {
        StringUtilities.createString(new byte[]{0x00}, "NO-SUCH-ENCODING");
    }

    @Test
    public void testUtf8ConvenienceMethods() {
        String text = "UTF-8 test";
        byte[] bytes = StringUtilities.getUTF8Bytes(text);
        String fromBytes = StringUtilities.createUtf8String(bytes);
        assertEquals(text, fromBytes);
        // Alias method
        assertEquals(text, StringUtilities.createUTF8String(bytes));
    }

    /* ---------- hashCodeIgnoreCase ---------- */

    @Test
    public void testHashCodeIgnoreCase() {
        String s1 = "AbCde";
        String s2 = "aBcDe";
        assertEquals(StringUtilities.hashCodeIgnoreCase(s1), StringUtilities.hashCodeIgnoreCase(s2));
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    /* ---------- trim family ---------- */

    @Test
    public void testTrim() {
        assertNull(StringUtilities.trim(null));
        assertEquals("abc", StringUtilities.trim("  abc  "));
    }

    @Test
    public void testTrimToEmpty() {
        assertEquals("", StringUtilities.trimToEmpty(null));
        assertEquals("", StringUtilities.trimToEmpty("   "));
        assertEquals("xyz", StringUtilities.trimToEmpty("  xyz "));
    }

    @Test
    public void testTrimToNull() {
        assertNull(StringUtilities.trimToNull(null));
        assertNull(StringUtilities.trimToNull("   "));
        assertEquals("value", StringUtilities.trimToNull("  value "));
    }

    @Test
    public void testTrimEmptyToDefault() {
        assertEquals("default", StringUtilities.trimEmptyToDefault(null, "default"));
        assertEquals("default", StringUtilities.trimEmptyToDefault("   ", "default"));
        assertEquals("real", StringUtilities.trimEmptyToDefault("  real ", "default"));
    }

    /* ---------- removeLeadingAndTrailingQuotes ---------- */

    @Test
    public void testRemoveLeadingAndTrailingQuotes() {
        assertNull(StringUtilities.removeLeadingAndTrailingQuotes(null));
        assertEquals("", StringUtilities.removeLeadingAndTrailingQuotes(""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"text\""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"text\"\""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"\"text\"\"\""));
        assertEquals("text with \"quotes\" inside", StringUtilities.removeLeadingAndTrailingQuotes("\"text with \"quotes\" inside\""));
    }

    /* ---------- commaSeparatedStringToSet ---------- */

    @Test
    public void testCommaSeparatedStringToSet() {
        Set<String> empty = StringUtilities.commaSeparatedStringToSet(null);
        assertTrue(empty.isEmpty());

        empty = StringUtilities.commaSeparatedStringToSet("   ");
        assertTrue(empty.isEmpty());

        Set<String> result = StringUtilities.commaSeparatedStringToSet("apple, banana , , cherry,apple");
        Set<String> expected = new HashSet<>();
        Collections.addAll(expected, "apple", "banana", "cherry");
        assertEquals(expected, result);
        // Ensure no empty strings are present
        assertFalse(result.contains(""));
    }

    /* ---------- Additional edge cases for wildcard conversion ---------- */

    @Test
    public void testWildcardSpecialCharactersEscaping() {
        String pattern = StringUtilities.wildcardToRegexString("file[1].txt");
        // '[' and ']' should be escaped
        assertEquals("^file\\[1\\]\\.txt$", pattern);
    }
}
