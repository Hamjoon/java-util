package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.io.UnsupportedEncodingException;

import static org.junit.Assert.*;

public class StringUtilitiesTest {

    private Locale originalLocale;

    @Before
    public void setUp() {
        // Preserve original locale in case a test changes it
        originalLocale = Locale.getDefault();
    }

    @After
    public void tearDown() {
        Locale.setDefault(originalLocale);
    }

    /*----------------------------------------------------------------------
     * equals / equalsIgnoreCase
     *----------------------------------------------------------------------*/

    @Test
    public void testEqualsBasic() {
        assertTrue(StringUtilities.equals("abc", "abc"));
        assertFalse(StringUtilities.equals("abc", "ABC"));
        assertFalse(StringUtilities.equals(null, "abc"));
        assertTrue(StringUtilities.equals(null, null));
    }

    @Test
    public void testEqualsCharSequenceImplementation() {
        CharSequence cs1 = new StringBuilder("xyz");
        CharSequence cs2 = new StringBuffer("xyz");
        assertTrue(StringUtilities.equals(cs1, cs2));
        assertFalse(StringUtilities.equals(cs1, "XYz"));
    }

    @Test
    public void testEqualsIgnoreCaseBasic() {
        assertTrue(StringUtilities.equalsIgnoreCase("abc", "ABC"));
        assertFalse(StringUtilities.equalsIgnoreCase("abc", "ab"));
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        assertFalse(StringUtilities.equalsIgnoreCase(null, "a"));
    }

    @Test
    public void testRegionMatches() {
        // Simple exact match
        assertTrue(StringUtilities.regionMatches("HelloWorld", false, 0, "Hello", 0, 5));
        // Case‑insensitive match
        assertTrue(StringUtilities.regionMatches("HelloWorld", true, 0, "hello", 0, 5));
        // Mismatch due to length
        assertFalse(StringUtilities.regionMatches("Hi", false, 0, "Hello", 0, 5));
        // Mismatch due to different characters
        assertFalse(StringUtilities.regionMatches("abcdef", false, 0, "abczef", 0, 6));
    }

    /*----------------------------------------------------------------------
     * Trim‑related comparisons
     *----------------------------------------------------------------------*/

    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  abc  ", "abc"));
        assertFalse(StringUtilities.equalsWithTrim(" abc ", "ab"));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim(null, "a"));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  AbC  ", "abc"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(" abc ", "ab"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(null, null));
    }

    /*----------------------------------------------------------------------
     * Empty / whitespace / content checks
     *----------------------------------------------------------------------*/

    @Test
    public void testIsEmptyAndIsWhitespace() {
        assertTrue(StringUtilities.isEmpty((String) null));
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty("   "));
        assertFalse(StringUtilities.isEmpty(" a "));
        assertTrue(StringUtilities.isWhitespace(null));
        assertTrue(StringUtilities.isWhitespace("\t\n"));
        assertFalse(StringUtilities.isWhitespace("x"));
    }

    @Test
    public void testHasContent() {
        assertFalse(StringUtilities.hasContent(null));
        assertFalse(StringUtilities.hasContent("   "));
        assertTrue(StringUtilities.hasContent(" x "));
    }

    /*----------------------------------------------------------------------
     * Length utilities
     *----------------------------------------------------------------------*/

    @Test
    public void testLength() {
        assertEquals(0, StringUtilities.length((String) null));
        assertEquals(5, StringUtilities.length("hello"));
        assertEquals(0, StringUtilities.length((CharSequence) null));
        assertEquals(3, StringUtilities.length(new StringBuilder("abc")));
    }

    @Test
    public void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(0, StringUtilities.trimLength("   "));
        assertEquals(3, StringUtilities.trimLength("  abc  "));
    }

    /*----------------------------------------------------------------------
     * lastIndexOf
     *----------------------------------------------------------------------*/

    @Test
    public void testLastIndexOf() {
        assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        assertEquals(3, StringUtilities.lastIndexOf("a/b/c/d", '/'));
        assertEquals(0, StringUtilities.lastIndexOf("/", '/'));
    }

    /*----------------------------------------------------------------------
     * Hex encode / decode
     *----------------------------------------------------------------------*/

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] original = new byte[] {0x00, 0x0F, (byte) 0xA5, (byte) 0xFF};
        String encoded = StringUtilities.encode(original);
        byte[] decoded = StringUtilities.decode(encoded);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void testDecodeInvalidLength() {
        assertNull(StringUtilities.decode("ABC")); // odd length
    }

    @Test
    public void testDecodeNonHexCharacters() {
        // Character.digit returns -1 for non‑hex, resulting in a negative value;
        // the method does not validate, but we can still verify the numeric result.
        byte[] result = StringUtilities.decode("zz");
        // 'z' is 35 in hex, so 35*16+35 = 595 -> (byte)595 == 0x53
        assertEquals(1, result.length);
        assertEquals((byte) 0x53, result[0]);
    }

    /*----------------------------------------------------------------------
     * Counting occurrences
     *----------------------------------------------------------------------*/

    @Test
    public void testCountCharAndString() {
        assertEquals(3, StringUtilities.count("ababab", 'a'));
        assertEquals(2, StringUtilities.count("ababab", "ab"));
        assertEquals(0, StringUtilities.count(null, "x"));
        assertEquals(0, StringUtilities.count("test", null));
        assertEquals(0, StringUtilities.count("", "a"));
        assertEquals(0, StringUtilities.count("abc", ""));
    }

    /*----------------------------------------------------------------------
     * Wildcard to regex conversion
     *----------------------------------------------------------------------*/

    @Test
    public void testWildcardToRegexString() {
        String wildcard = "*file?.txt";
        String regex = StringUtilities.wildcardToRegexString(wildcard);
        assertEquals("^.*file.\\.txt$", regex);
        // Verify that the produced regex matches expected strings
        assertTrue("myfile1.txt".matches(regex));
        assertFalse("myfile12.txt".matches(regex));
    }

    /*----------------------------------------------------------------------
     * Levenshtein distance
     *----------------------------------------------------------------------*/

    @Test
    public void testLevenshteinDistance() {
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        assertEquals(2, StringUtilities.levenshteinDistance("flaw", "lawn"));
        assertEquals(5, StringUtilities.levenshteinDistance(null, "hello"));
        assertEquals(5, StringUtilities.levenshteinDistance("hello", null));
    }

    /*----------------------------------------------------------------------
     * Damerau‑Levenshtein distance
     *----------------------------------------------------------------------*/

    @Test
    public void testDamerauLevenshteinDistance() {
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("kitten", "sitting"));
        // Transposition case: "ca" -> "ac" should be distance 1
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("ca", "ac"));
        // Example from Javadoc
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("book", "back"));
    }

    /*----------------------------------------------------------------------
     * Random string generation
     *----------------------------------------------------------------------*/

    @Test
    public void testGetRandomStringDeterministic() {
        Random random = new Random(12345L);
        String s = StringUtilities.getRandomString(random, 5, 10);
        assertTrue(s.length() >= 5 && s.length() <= 10);
        // First character must be uppercase, rest lowercase
        assertTrue(Character.isUpperCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            assertTrue(Character.isLowerCase(s.charAt(i)));
        }
    }

    @Test
    public void testGetRandomChar() {
        Random random = new Random(0L);
        String upper = StringUtilities.getRandomChar(random, true);
        String lower = StringUtilities.getRandomChar(random, false);
        assertEquals(1, upper.length());
        assertEquals(1, lower.length());
        assertTrue(Character.isUpperCase(upper.charAt(0)));
        assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    /*----------------------------------------------------------------------
     * Byte/encoding helpers
     *----------------------------------------------------------------------*/

    @Test
    public void testGetBytesSupportedEncoding() throws UnsupportedEncodingException {
        byte[] bytes = StringUtilities.getBytes("test", "UTF-8");
        assertArrayEquals("test".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBytesUnsupportedEncoding() {
        StringUtilities.getBytes("test", "UNSUPPORTED_ENCODING");
    }

    @Test
    public void testCreateUtf8StringDeprecated() {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        assertEquals("hello", StringUtilities.createUtf8String(data));
    }

    @Test
    public void testCreateUTF8String() {
        byte[] data = "world".getBytes(StandardCharsets.UTF_8);
        assertEquals("world", StringUtilities.createUTF8String(data));
    }

    @Test
    public void testGetUTF8Bytes() {
        byte[] expected = "sample".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, StringUtilities.getUTF8Bytes("sample"));
    }

    @Test
    public void testCreateStringSupportedEncoding() {
        byte[] data = "encoding".getBytes(StandardCharsets.UTF_8);
        String s = StringUtilities.createString(data, "UTF-8");
        assertEquals("encoding", s);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStringUnsupportedEncoding() {
        StringUtilities.createString("abc".getBytes(), "BAD-ENC");
    }

    /*----------------------------------------------------------------------
     * hashCodeIgnoreCase
     *----------------------------------------------------------------------*/

    @Test
    public void testHashCodeIgnoreCaseMatchesStandard() {
        String mixed = "AbCDeF";
        int expected = mixed.toLowerCase(Locale.ROOT).hashCode();
        assertEquals(expected, StringUtilities.hashCodeIgnoreCase(mixed));
        // Null handling
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    /*----------------------------------------------------------------------
     * Trim utilities
     *----------------------------------------------------------------------*/

    @Test
    public void testTrimAndTrimToEmpty() {
        assertNull(StringUtilities.trim(null));
        assertEquals("abc", StringUtilities.trim("  abc  "));
        assertEquals("", StringUtilities.trimToEmpty(null));
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
        assertEquals("def", StringUtilities.trimEmptyToDefault(null, "def"));
        assertEquals("def", StringUtilities.trimEmptyToDefault("   ", "def"));
        assertEquals("real", StringUtilities.trimEmptyToDefault("  real  ", "def"));
    }

    /*----------------------------------------------------------------------
     * Quote removal
     *----------------------------------------------------------------------*/

    @Test
    public void testRemoveLeadingAndTrailingQuotes() {
        assertNull(StringUtilities.removeLeadingAndTrailingQuotes(null));
        assertEquals("", StringUtilities.removeLeadingAndTrailingQuotes(""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"text\""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"\"text\"\"\""));
        assertEquals("text with \"quotes\" inside", StringUtilities.removeLeadingAndTrailingQuotes("\"text with \"quotes\" inside\""));
    }

    /*----------------------------------------------------------------------
     * CSV to Set conversion
     *----------------------------------------------------------------------*/

    @Test
    public void testCommaSeparatedStringToSet() {
        Set<String> empty = StringUtilities.commaSeparatedStringToSet(null);
        assertTrue(empty.isEmpty());

        Set<String> result = StringUtilities.commaSeparatedStringToSet("  apple, banana , ,apple,  ,Cherry ");
        // Order is not guaranteed; just verify content
        Set<String> expected = new HashSet<>(Arrays.asList("apple", "banana", "Cherry"));
        assertEquals(expected, result);
    }

    /*----------------------------------------------------------------------
     * Additional edge cases for count (overlapping tokens)
     *----------------------------------------------------------------------*/

    @Test
    public void testCountOverlappingTokens() {
        // "aaa" contains "aa" twice overlapping: positions 0‑1 and 1‑2
        assertEquals(2, StringUtilities.count("aaa", "aa"));
    }

    /*----------------------------------------------------------------------
     * Verify that regionMatches throws when null arguments are supplied
     * (uses Convention.throwIfNull which results in NPE)
     *----------------------------------------------------------------------*/

    @Test(expected = NullPointerException.class)
    public void testRegionMatchesNullCs() {
        StringUtilities.regionMatches(null, false, 0, "test", 0, 1);
    }

    @Test(expected = NullPointerException.class)
    public void testRegionMatchesNullSubstring() {
        StringUtilities.regionMatches("test", false, 0, null, 0, 1);
    }
}
