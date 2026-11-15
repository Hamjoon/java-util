package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link StringUtilities}.
 *
 * <p>This test class covers:
 * <ul>
 *   <li>normal usage of each public method</li>
 *   <li>edge cases (nulls, empty strings, whitespace, odd‑length hex strings, etc.)</li>
 *   <li>error scenarios (unsupported encoding)</li>
 *   <li>behaviour of helper methods that are exercised indirectly</li>
 * </ul>
 * </p>
 *
 * <p>Only APIs that appear in {@code StringUtilities} (or in the JDK) are used.
 * No mocking frameworks are employed – real objects are created where required.</p>
 */
public class StringUtilitiesTest {

    private Random deterministicRandom;

    @Before
    public void setUp() {
        // Fixed seed to make getRandomString / getRandomChar deterministic
        deterministicRandom = new Random(12345L);
    }

    @After
    public void tearDown() {
        deterministicRandom = null;
    }

    /* -------------------------------------------------
     *  equals / equalsIgnoreCase
     * ------------------------------------------------- */

    @Test
    public void testEqualsBothNull() {
        assertTrue(StringUtilities.equals((CharSequence) null, null));
    }

    @Test
    public void testEqualsOneNull() {
        assertFalse(StringUtilities.equals("abc", null));
        assertFalse(StringUtilities.equals(null, "abc"));
    }

    @Test
    public void testEqualsSameStringInstance() {
        String s = "same";
        assertTrue(StringUtilities.equals(s, s));
    }

    @Test
    public void testEqualsDifferentTypes() {
        CharSequence cs = new StringBuilder("builder");
        String s = "builder";
        assertTrue(StringUtilities.equals(cs, s));
    }

    @Test
    public void testEqualsDifferentContent() {
        assertFalse(StringUtilities.equals("abc", "abC"));
    }

    @Test
    public void testEqualsIgnoreCaseVarious() {
        assertTrue(StringUtilities.equalsIgnoreCase("AbC", "aBc"));
        assertFalse(StringUtilities.equalsIgnoreCase("AbC", "ab"));
        assertFalse(StringUtilities.equalsIgnoreCase("AbC", null));
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
    }

    @Test
    public void testEqualsIgnoreCaseRegionMatches() {
        // regionMatches should be exercised via equalsIgnoreCase
        assertTrue(StringUtilities.equalsIgnoreCase("ß", "ß")); // same char, case‑insensitive true
    }

    /* -------------------------------------------------
     *  equalsWithTrim / equalsIgnoreCaseWithTrim
     * ------------------------------------------------- */

    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  abc  ", "abc"));
        assertFalse(StringUtilities.equalsWithTrim(" abc ", "ab"));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim(null, " "));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  AbC  ", "abc"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("abc", "ab"));
    }

    /* -------------------------------------------------
     *  isEmpty / isWhitespace / hasContent
     * ------------------------------------------------- */

    @Test
    public void testIsEmptyAndWhitespace() {
        assertTrue(StringUtilities.isEmpty((CharSequence) null));
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isWhitespace("   \t\n"));
        assertFalse(StringUtilities.isEmpty("a"));
        assertFalse(StringUtilities.isWhitespace(" a "));
    }

    @Test
    public void testHasContent() {
        assertFalse(StringUtilities.hasContent(null));
        assertFalse(StringUtilities.hasContent("   "));
        assertTrue(StringUtilities.hasContent("x"));
    }

    /* -------------------------------------------------
     *  length / trimLength
     * ------------------------------------------------- */

    @Test
    public void testLength() {
        assertEquals(0, StringUtilities.length((CharSequence) null));
        assertEquals(5, StringUtilities.length("hello"));
    }

    @Test
    public void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(5, StringUtilities.trimLength("  hello  "));
    }

    /* -------------------------------------------------
     *  lastIndexOf
     * ------------------------------------------------- */

    @Test
    public void testLastIndexOf() {
        assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        assertEquals(4, StringUtilities.lastIndexOf("a/b/c/d", '/'));
    }

    /* -------------------------------------------------
     *  encode / decode
     * ------------------------------------------------- */

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] original = new byte[]{0x00, (byte) 0xFF, 0x5A, 0x0A};
        String encoded = StringUtilities.encode(original);
        byte[] decoded = StringUtilities.decode(encoded);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(StringUtilities.decode("ABC")); // length 3 -> odd
    }

    @Test
    public void testDecodeInvalidHexDigits() {
        // Character.digit returns -1 for non‑hex chars, causing negative values,
        // but the method does not guard against that – we verify the resulting bytes.
        byte[] result = StringUtilities.decode("GG");
        assertEquals((byte) (-1 * 16 + -1), result[0]); // -17
    }

    /* -------------------------------------------------
     *  count
     * ------------------------------------------------- */

    @Test
    public void testCountCharAndString() {
        assertEquals(3, StringUtilities.count("ababa", 'a'));
        assertEquals(2, StringUtilities.count("ababa", "ab"));
        assertEquals(0, StringUtilities.count(null, "a"));
        assertEquals(0, StringUtilities.count("abc", null));
        assertEquals(0, StringUtilities.count("", "a"));
        assertEquals(0, StringUtilities.count("abc", ""));
    }

    /* -------------------------------------------------
     *  wildcardToRegexString
     * ------------------------------------------------- */

    @Test
    public void testWildcardToRegexString() {
        String wildcard = "*file?.txt";
        String regex = StringUtilities.wildcardToRegexString(wildcard);
        assertTrue("regex should start with ^ and end with $", regex.startsWith("^") && regex.endsWith("$"));
        assertTrue("regex should match a proper filename",
                "myfile1.txt".matches(regex));
        assertFalse("regex should not match a non‑matching name",
                "myfile12.txt".matches(regex));
    }

    /* -------------------------------------------------
     *  levenshteinDistance
     * ------------------------------------------------- */

    @Test
    public void testLevenshteinDistanceBasic() {
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(3, StringUtilities.levenshteinDistance("", "abc"));
        assertEquals(3, StringUtilities.levenshteinDistance("abc", ""));
        assertEquals(1, StringUtilities.levenshteinDistance("kitten", "sitten"));
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
    }

    @Test
    public void testLevenshteinDistanceWithNulls() {
        assertEquals(4, StringUtilities.levenshteinDistance(null, "test"));
        assertEquals(4, StringUtilities.levenshteinDistance("test", null));
        assertEquals(0, StringUtilities.levenshteinDistance(null, null));
    }

    /* -------------------------------------------------
     *  damerauLevenshteinDistance
     * ------------------------------------------------- */

    @Test
    public void testDamerauLevenshteinDistanceBasic() {
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("", "abc"));
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("abc", ""));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("ca", "ac")); // transposition
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("abcd", "acbd")); // one transposition + substitution
    }

    @Test
    public void testDamerauLevenshteinDistanceWithNulls() {
        assertEquals(5, StringUtilities.damerauLevenshteinDistance(null, "hello"));
        assertEquals(5, StringUtilities.damerauLevenshteinDistance("hello", null));
        assertEquals(0, StringUtilities.damerauLevenshteinDistance(null, null));
    }

    /* -------------------------------------------------
     *  getRandomString / getRandomChar
     * ------------------------------------------------- */

    @Test
    public void testGetRandomCharUpperAndLower() {
        // With deterministicRandom the sequence is reproducible.
        String lower = StringUtilities.getRandomChar(deterministicRandom, false);
        String upper = StringUtilities.getRandomChar(deterministicRandom, true);
        assertEquals(1, lower.length());
        assertEquals(1, upper.length());
        assertTrue(Character.isLowerCase(lower.charAt(0)));
        assertTrue(Character.isUpperCase(upper.charAt(0)));
    }

    @Test
    public void testGetRandomStringProperCaseAndLengthBounds() {
        String result = StringUtilities.getRandomString(deterministicRandom, 5, 5);
        assertEquals(5, result.length());
        assertTrue(Character.isUpperCase(result.charAt(0)));
        for (int i = 1; i < result.length(); i++) {
            assertTrue(Character.isLowerCase(result.charAt(i)));
        }
    }

    /* -------------------------------------------------
     *  getBytes / createString / UTF‑8 helpers
     * ------------------------------------------------- */

    @Test
    public void testGetBytesAndCreateStringRoundTrip() {
        String original = "Hello ☺";
        byte[] bytes = StringUtilities.getBytes(original, "UTF-8");
        String restored = StringUtilities.createString(bytes, "UTF-8");
        assertEquals(original, restored);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBytesUnsupportedEncoding() {
        StringUtilities.getBytes("test", "UNSUPPORTED_ENCODING");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStringUnsupportedEncoding() {
        StringUtilities.createString(new byte[]{0x41}, "UNSUPPORTED_ENCODING");
    }

    @Test
    public void testUtf8ConvenienceMethods() {
        String text = "Unicode – тест";
        byte[] utf8 = StringUtilities.getUTF8Bytes(text);
        assertNotNull(utf8);
        assertEquals(text, StringUtilities.createUtf8String(utf8));
        assertEquals(text, StringUtilities.createUTF8String(utf8));
    }

    /* -------------------------------------------------
     *  hashCodeIgnoreCase
     * ------------------------------------------------- */

    @Test
    public void testHashCodeIgnoreCase() {
        String s1 = "AbC";
        String s2 = "aBc";
        assertEquals(StringUtilities.hashCodeIgnoreCase(s1),
                     StringUtilities.hashCodeIgnoreCase(s2));
        // Null returns 0
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    /* -------------------------------------------------
     *  trim / trimToEmpty / trimToNull / trimEmptyToDefault
     * ------------------------------------------------- */

    @Test
    public void testTrimMethods() {
        assertNull(StringUtilities.trim(null));
        assertEquals("x", StringUtilities.trim("  x  "));

        assertEquals("", StringUtilities.trimToEmpty(null));
        assertEquals("", StringUtilities.trimToEmpty("   "));
        assertEquals("y", StringUtilities.trimToEmpty("  y "));

        assertNull(StringUtilities.trimToNull(null));
        assertNull(StringUtilities.trimToNull("   "));
        assertEquals("z", StringUtilities.trimToNull("  z  "));

        assertEquals("def", StringUtilities.trimEmptyToDefault(null, "def"));
        assertEquals("def", StringUtilities.trimEmptyToDefault("   ", "def"));
        assertEquals("val", StringUtilities.trimEmptyToDefault("  val ", "def"));
    }
}
