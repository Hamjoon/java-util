package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.Random;

import static org.junit.Assert.*;

public class StringUtilitiesTest {

    private Random random;

    @Before
    public void setUp() {
        // Fixed seed for reproducible random strings
        random = new Random(12345L);
    }

    /*=================== equals / equalsIgnoreCase ===================*/

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
    public void testEqualsSameReference() {
        String s = "same";
        assertTrue(StringUtilities.equals(s, s));
    }

    @Test
    public void testEqualsDifferentTypesSameContent() {
        StringBuilder sb = new StringBuilder("hello");
        assertTrue(StringUtilities.equals("hello", sb));
    }

    @Test
    public void testEqualsDifferentContent() {
        assertFalse(StringUtilities.equals("abc", "abd"));
    }

    @Test
    public void testEqualsStringOverload() {
        assertTrue(StringUtilities.equals("test", "test"));
        assertFalse(StringUtilities.equals("test", "Test"));
    }

    @Test
    public void testEqualsIgnoreCase() {
        assertTrue(StringUtilities.equalsIgnoreCase("AbC", "aBc"));
        assertFalse(StringUtilities.equalsIgnoreCase("AbC", "ab"));
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        assertFalse(StringUtilities.equalsIgnoreCase("x", null));
    }

    @Test
    public void testEqualsIgnoreCaseStringOverload() {
        assertTrue(StringUtilities.equalsIgnoreCase("Hello", "hello"));
        assertFalse(StringUtilities.equalsIgnoreCase("Hello", "Hell"));
    }

    /*=================== regionMatches ===================*/

    @Test
    public void testRegionMatchesExact() {
        assertTrue(StringUtilities.regionMatches("abcdef", false, 0, "abc", 0, 3));
        assertTrue(StringUtilities.regionMatches("abcdef", true, 0, "ABC", 0, 3));
    }

    @Test
    public void testRegionMatchesCaseSensitiveFailure() {
        assertFalse(StringUtilities.regionMatches("Abc", false, 0, "aBc", 0, 3));
    }

    @Test
    public void testRegionMatchesInvalidParameters() {
        // negative start
        assertFalse(StringUtilities.regionMatches("abc", false, -1, "abc", 0, 3));
        // length longer than source
        assertFalse(StringUtilities.regionMatches("ab", false, 0, "abc", 0, 3));
        // length longer than substring
        assertFalse(StringUtilities.regionMatches("abc", false, 0, "ab", 0, 3));
    }

    /*=================== equalsWithTrim / equalsIgnoreCaseWithTrim ===================*/

    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  foo ", "foo"));
        assertFalse(StringUtilities.equalsWithTrim("foo", "bar"));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim(null, " "));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  Foo ", "foo"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("FOO", " foo "));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("foo", "bar"));
    }

    /*=================== isEmpty / isWhitespace / hasContent ===================*/

    @Test
    public void testIsEmptyAndIsWhitespace() {
        assertTrue(StringUtilities.isEmpty((CharSequence) null));
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty("   "));
        assertFalse(StringUtilities.isEmpty(" a "));
    }

    @Test
    public void testIsEmptyStringOverload() {
        assertTrue(StringUtilities.isEmpty((String) null));
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty(" \t\n"));
        assertFalse(StringUtilities.isEmpty("x"));
    }

    @Test
    public void testHasContent() {
        assertFalse(StringUtilities.hasContent(null));
        assertFalse(StringUtilities.hasContent(""));
        assertFalse(StringUtilities.hasContent("   "));
        assertTrue(StringUtilities.hasContent("a"));
    }

    /*=================== length / trimLength ===================*/

    @Test
    public void testLength() {
        assertEquals(0, StringUtilities.length((CharSequence) null));
        assertEquals(5, StringUtilities.length("hello"));
        assertEquals(0, StringUtilities.length(""));
    }

    @Test
    public void testTrimLength() {
        assertEquals(4, StringUtilities.trimLength("  abc "));
        assertEquals(0, StringUtilities.trimLength("   "));
        assertEquals(0, StringUtilities.trimLength(null));
    }

    /*=================== lastIndexOf ===================*/

    @Test
    public void testLastIndexOf() {
        assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        assertEquals(5, StringUtilities.lastIndexOf("a/b/c/d/e", '/'));
        assertEquals(0, StringUtilities.lastIndexOf("/", '/'));
    }

    /*=================== decode / encode ===================*/

    @Test
    public void testDecodeEvenLength() {
        byte[] expected = new byte[]{(byte) 0x0F, (byte) 0xA0, (byte) 0x5C};
        assertArrayEquals(expected, StringUtilities.decode("0FA05C"));
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        assertNull(StringUtilities.decode("ABC"));
    }

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] data = new byte[]{0x01, 0x23, (byte) 0xFF, 0x00};
        String encoded = StringUtilities.encode(data);
        assertEquals("0123FF00", encoded);
        assertArrayEquals(data, StringUtilities.decode(encoded));
    }

    /*=================== count ===================*/

    @Test
    public void testCountChar() {
        assertEquals(3, StringUtilities.count("banana", 'a'));
        assertEquals(0, StringUtilities.count("test", 'z'));
        assertEquals(0, StringUtilities.count(null, 'a'));
    }

    @Test
    public void testCountToken() {
        assertEquals(2, StringUtilities.count("ababab", "ab"));
        // overlapping should count each start position
        assertEquals(3, StringUtilities.count("aaa", "aa"));
        assertEquals(0, StringUtilities.count("abc", ""));
        assertEquals(0, StringUtilities.count(null, "a"));
        assertEquals(0, StringUtilities.count("abc", null));
    }

    /*=================== wildcardToRegexString ===================*/

    @Test
    public void testWildcardToRegexString() {
        String pattern = StringUtilities.wildcardToRegexString("file-?.*");
        assertTrue("file-a.txt".matches(pattern));
        assertTrue("file-z.jpeg".matches(pattern));
        assertFalse("file-12.txt".matches(pattern));

        // Escaping special characters
        String escPattern = StringUtilities.wildcardToRegexString("a+b(c)");
        assertTrue("a+b(c)".matches(escPattern));
        assertFalse("a+b(c".matches(escPattern));
    }

    /*=================== levenshteinDistance ===================*/

    @Test
    public void testLevenshteinDistanceBasic() {
        assertEquals(0, StringUtilities.levenshteinDistance("kitten", "kitten"));
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        assertEquals(5, StringUtilities.levenshteinDistance("", "hello"));
        assertEquals(5, StringUtilities.levenshteinDistance("hello", ""));
        assertEquals(0, StringUtilities.levenshteinDistance(null, null));
        assertEquals(4, StringUtilities.levenshteinDistance(null, "test"));
        assertEquals(4, StringUtilities.levenshteinDistance("test", null));
    }

    /*=================== damerauLevenshteinDistance ===================*/

    @Test
    public void testDamerauLevenshteinDistanceBasic() {
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("abc", "abc"));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("ab", "ba")); // transposition
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("abcd", "acbd")); // one transposition + one substitution
        assertEquals(5, StringUtilities.damerauLevenshteinDistance("", "hello"));
        assertEquals(5, StringUtilities.damerauLevenshteinDistance("hello", ""));
    }

    /*=================== getRandomString / getRandomChar ===================*/

    @Test
    public void testGetRandomStringLengthAndCase() {
        String randomStr = StringUtilities.getRandomString(random, 5, 10);
        assertTrue(randomStr.length() >= 5 && randomStr.length() <= 10);
        // first char uppercase
        assertTrue(Character.isUpperCase(randomStr.charAt(0)));
        // remaining chars lowercase
        for (int i = 1; i < randomStr.length(); i++) {
            assertTrue(Character.isLowerCase(randomStr.charAt(i)));
        }
    }

    @Test
    public void testGetRandomChar() {
        // Uppercase
        String upper = StringUtilities.getRandomChar(random, true);
        assertEquals(1, upper.length());
        assertTrue(Character.isUpperCase(upper.charAt(0)));

        // Lowercase
        String lower = StringUtilities.getRandomChar(random, false);
        assertEquals(1, lower.length());
        assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    /*=================== getBytes / createString (encoding) ===================*/

    @Test
    public void testUtf8RoundTrip() {
        String original = "café 🌟";
        byte[] utf8Bytes = StringUtilities.getUTF8Bytes(original);
        assertNotNull(utf8Bytes);
        String roundTrip = StringUtilities.createUtf8String(utf8Bytes);
        assertEquals(original, roundTrip);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBytesUnsupportedEncodingThrows() {
        StringUtilities.getBytes("test", "UNSUPPORTED-ENCODING");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStringUnsupportedEncodingThrows() {
        StringUtilities.createString(new byte[]{0x00}, "UNSUPPORTED-ENCODING");
    }

    /*=================== hashCodeIgnoreCase ===================*/

    @Test
    public void testHashCodeIgnoreCase() {
        String s1 = "AbCdeF";
        String s2 = "aBcDeF";
        assertEquals(StringUtilities.hashCodeIgnoreCase(s1), StringUtilities.hashCodeIgnoreCase(s2));
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    /*=================== trim / trimToEmpty / trimToNull / trimEmptyToDefault ===================*/

    @Test
    public void testTrim() {
        assertNull(StringUtilities.trim(null));
        assertEquals("abc", StringUtilities.trim("  abc  "));
    }

    @Test
    public void testTrimToEmpty() {
        assertEquals(StringUtilities.EMPTY, StringUtilities.trimToEmpty(null));
        assertEquals(StringUtilities.EMPTY, StringUtilities.trimToEmpty("   "));
        assertEquals("x", StringUtilities.trimToEmpty("  x "));
    }

    @Test
    public void testTrimToNull() {
        assertNull(StringUtilities.trimToNull(null));
        assertNull(StringUtilities.trimToNull("   "));
        assertEquals("y", StringUtilities.trimToNull("  y "));
    }

    @Test
    public void testTrimEmptyToDefault() {
        String def = "default";
        assertEquals(def, StringUtilities.trimEmptyToDefault(null, def));
        assertEquals(def, StringUtilities.trimEmptyToDefault("   ", def));
        assertEquals("value", StringUtilities.trimEmptyToDefault("  value ", def));
    }
}
