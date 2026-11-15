package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.io.UnsupportedEncodingException;
import java.util.Random;

import static org.junit.Assert.*;

public class StringUtilitiesTest {

    private Random deterministicRandom;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // deterministic random with a fixed seed so that getRandomString / getRandomChar are repeatable
        deterministicRandom = new Random(12345L);
    }

    /* ---------------------------------------------------------------------- */
    /*  equals(CharSequence, CharSequence)                                   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEqualsCharSequence_Basic() {
        assertTrue(StringUtilities.equals("abc", "abc"));
        assertFalse(StringUtilities.equals("abc", "abC"));
        assertFalse(StringUtilities.equals("abc", "abcd"));
        assertTrue(StringUtilities.equals(null, null));
        assertFalse(StringUtilities.equals(null, "abc"));
        assertFalse(StringUtilities.equals("abc", null));
    }

    @Test
    public void testEqualsCharSequence_NonStringImplementation() {
        CharSequence cs1 = new StringBuilder("test");
        CharSequence cs2 = new StringBuffer("test");
        assertTrue(StringUtilities.equals(cs1, cs2));
        CharSequence cs3 = new StringBuilder("Test");
        assertFalse(StringUtilities.equals(cs1, cs3));
    }

    /* ---------------------------------------------------------------------- */
    /*  equals(String, String)                                                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEqualsString() {
        assertTrue(StringUtilities.equals("hello", "hello"));
        assertFalse(StringUtilities.equals("hello", "Hello"));
        assertTrue(StringUtilities.equals(null, null));
        assertFalse(StringUtilities.equals(null, "hello"));
    }

    /* ---------------------------------------------------------------------- */
    /*  equalsIgnoreCase(CharSequence, CharSequence)                         */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEqualsIgnoreCaseCharSequence() {
        assertTrue(StringUtilities.equalsIgnoreCase("AbC", "aBc"));
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        assertFalse(StringUtilities.equalsIgnoreCase(null, "a"));
        assertFalse(StringUtilities.equalsIgnoreCase("a", null));
        assertFalse(StringUtilities.equalsIgnoreCase("abc", "ab"));
        // non‑String CharSequence
        CharSequence cs1 = new StringBuilder("TeSt");
        CharSequence cs2 = new StringBuffer("tEsT");
        assertTrue(StringUtilities.equalsIgnoreCase(cs1, cs2));
    }

    /* ---------------------------------------------------------------------- */
    /*  equalsIgnoreCase(String, String)                                      */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEqualsIgnoreCaseString() {
        assertTrue(StringUtilities.equalsIgnoreCase("Java", "java"));
        assertFalse(StringUtilities.equalsIgnoreCase("Java", "JavA!"));
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
    }

    /* ---------------------------------------------------------------------- */
    /*  regionMatches (package‑private) – exercised via equalsIgnoreCase   */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testRegionMatches_EdgeCases() {
        // null checks are enforced by Convention.throwIfNull – expecting NPE
        thrown.expect(NullPointerException.class);
        StringUtilities.regionMatches(null, true, 0, "test", 0, 1);
    }

    @Test
    public void testRegionMatches_InvalidParameters() {
        // negative start / length should return false, not throw
        assertFalse(StringUtilities.regionMatches("abc", false, -1, "abc", 0, 1));
        assertFalse(StringUtilities.regionMatches("abc", false, 0, "abc", -1, 1));
        assertFalse(StringUtilities.regionMatches("abc", false, 0, "abc", 0, -1));
        // length beyond bounds
        assertFalse(StringUtilities.regionMatches("ab", false, 0, "abc", 0, 3));
    }

    /* ---------------------------------------------------------------------- */
    /*  equalsWithTrim                                                        */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  foo  ", "foo"));
        assertFalse(StringUtilities.equalsWithTrim("foo", "bar"));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim(null, "foo"));
    }

    /* ---------------------------------------------------------------------- */
    /*  equalsIgnoreCaseWithTrim                                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  Foo  ", "foo"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("Bar", "  BAR "));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("foo", "bar"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(null, null));
    }

    /* ---------------------------------------------------------------------- */
    /*  isEmpty / isWhitespace                                                */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testIsEmptyAndIsWhitespace() {
        assertTrue(StringUtilities.isEmpty((String) null));
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty("   "));
        assertFalse(StringUtilities.isEmpty(" a "));
        assertTrue(StringUtilities.isWhitespace(null));
        assertTrue(StringUtilities.isWhitespace(""));
        assertTrue(StringUtilities.isWhitespace("\t\n\r"));
        assertFalse(StringUtilities.isWhitespace("x"));
    }

    /* ---------------------------------------------------------------------- */
    /*  hasContent                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testHasContent() {
        assertFalse(StringUtilities.hasContent(null));
        assertFalse(StringUtilities.hasContent(""));
        assertFalse(StringUtilities.hasContent("   "));
        assertTrue(StringUtilities.hasContent("abc"));
    }

    /* ---------------------------------------------------------------------- */
    /*  length (CharSequence & String)                                        */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testLength() {
        assertEquals(0, StringUtilities.length((String) null));
        assertEquals(0, StringUtilities.length((CharSequence) null));
        assertEquals(5, StringUtilities.length("hello"));
        assertEquals(3, StringUtilities.length(new StringBuilder("abc")));
    }

    /* ---------------------------------------------------------------------- */
    /*  trimLength                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(0, StringUtilities.trimLength("   "));
        assertEquals(3, StringUtilities.trimLength("  abc  "));
    }

    /* ---------------------------------------------------------------------- */
    /*  lastIndexOf                                                            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testLastIndexOf() {
        assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        assertEquals(5, StringUtilities.lastIndexOf("a/b/c/d", '/'));
        assertEquals(-1, StringUtilities.lastIndexOf("abc", '/'));
    }

    /* ---------------------------------------------------------------------- */
    /*  decode / encode                                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] original = new byte[]{0x00, 0x0F, (byte) 0xAB, (byte) 0xFF};
        String encoded = StringUtilities.encode(original);
        byte[] decoded = StringUtilities.decode(encoded);
        assertArrayEquals(original, decoded);
    }

    @Test
    public void testDecodeInvalidLength() {
        assertNull(StringUtilities.decode("ABC")); // odd length
        assertNull(StringUtilities.decode("G1")); // non‑hex character yields -1 digit but still creates byte; we test odd length only
    }

    /* ---------------------------------------------------------------------- */
    /*  count (String, char) & count (CharSequence, CharSequence)            */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testCountChar() {
        assertEquals(3, StringUtilities.count("banana", 'a'));
        assertEquals(0, StringUtilities.count("banana", 'z'));
        assertEquals(0, StringUtilities.count(null, 'a'));
    }

    @Test
    public void testCountCharSequence() {
        assertEquals(2, StringUtilities.count("ababab", "ab"));
        assertEquals(0, StringUtilities.count("ababab", "cd"));
        assertEquals(0, StringUtilities.count(null, "ab"));
        assertEquals(0, StringUtilities.count("abc", null));
        assertEquals(0, StringUtilities.count("", "a"));
        assertEquals(0, StringUtilities.count("abc", ""));
    }

    /* ---------------------------------------------------------------------- */
    /*  wildcardToRegexString                                                  */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testWildcardToRegexString() {
        String regex = StringUtilities.wildcardToRegexString("file*.?xt");
        assertEquals("^file.*.\\.?xt$", regex);
        // Verify that the regex actually matches expected strings
        assertTrue("file123.txt".matches(regex));
        assertTrue("fileABC.xt".matches(regex));
        assertFalse("filenotmatch.txt".matches(regex));
    }

    /* ---------------------------------------------------------------------- */
    /*  levenshteinDistance                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testLevenshteinDistance() {
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        assertEquals(0, StringUtilities.levenshteinDistance(null, null));
        assertEquals(4, StringUtilities.levenshteinDistance(null, "test"));
        assertEquals(4, StringUtilities.levenshteinDistance("test", null));
    }

    /* ---------------------------------------------------------------------- */
    /*  damerauLevenshteinDistance                                             */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testDamerauLevenshteinDistance() {
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("ab", "ba")); // transposition
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("kitten", "sitting"));
        assertEquals(4, StringUtilities.damerauLevenshteinDistance(null, "test"));
        assertEquals(4, StringUtilities.damerauLevenshteinDistance("test", null));
    }

    /* ---------------------------------------------------------------------- */
    /*  getRandomString / getRandomChar                                       */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testGetRandomString() {
        String s = StringUtilities.getRandomString(deterministicRandom, 5, 5);
        assertEquals(5, s.length());
        assertTrue(Character.isUpperCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            assertTrue(Character.isLowerCase(s.charAt(i)));
        }
    }

    @Test
    public void testGetRandomChar() {
        String upper = StringUtilities.getRandomChar(deterministicRandom, true);
        assertEquals(1, upper.length());
        assertTrue(Character.isUpperCase(upper.charAt(0)));

        String lower = StringUtilities.getRandomChar(deterministicRandom, false);
        assertEquals(1, lower.length());
        assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    /* ---------------------------------------------------------------------- */
    /*  getBytes / createString / UTF‑8 helpers                               */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testGetBytesAndCreateString() throws UnsupportedEncodingException {
        String original = "Café";
        byte[] utf8 = StringUtilities.getBytes(original, "UTF-8");
        assertNotNull(utf8);
        String roundTrip = StringUtilities.createString(utf8, "UTF-8");
        assertEquals(original, roundTrip);
    }

    @Test
    public void testGetBytesUnsupportedEncoding() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Encoding (UNSUPPORTED) is not supported");
        StringUtilities.getBytes("test", "UNSUPPORTED");
    }

    @Test
    public void testCreateStringUnsupportedEncoding() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Encoding (UNSUPPORTED) is not supported");
        StringUtilities.createString(new byte[]{0x41}, "UNSUPPORTED");
    }

    @Test
    public void testUtf8ConvenienceMethods() {
        String s = "Hello UTF‑8 🌍";
        byte[] bytes = StringUtilities.getUTF8Bytes(s);
        assertNotNull(bytes);
        assertEquals(s, StringUtilities.createUtf8String(bytes));
        assertEquals(s, StringUtilities.createUTF8String(bytes));
    }

    /* ---------------------------------------------------------------------- */
    /*  hashCodeIgnoreCase                                                    */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testHashCodeIgnoreCase() {
        String a = "AbCdef";
        String b = "abcdef";
        assertEquals(StringUtilities.hashCodeIgnoreCase(a),
                     StringUtilities.hashCodeIgnoreCase(b));
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    /* ---------------------------------------------------------------------- */
    /*  trim / trimToEmpty / trimToNull / trimEmptyToDefault                  */
    /* ---------------------------------------------------------------------- */
    @Test
    public void testTrimMethods() {
        assertNull(StringUtilities.trim(null));
        assertEquals("abc", StringUtilities.trim("  abc  "));

        assertEquals("", StringUtilities.trimToEmpty(null));
        assertEquals("", StringUtilities.trimToEmpty("   "));

        assertNull(StringUtilities.trimToNull(null));
        assertNull(StringUtilities.trimToNull("   "));
        assertEquals("x", StringUtilities.trimToNull("  x  "));

        assertEquals("default", StringUtilities.trimEmptyToDefault(null, "default"));
        assertEquals("default", StringUtilities.trimEmptyToDefault("   ", "default"));
        assertEquals("value", StringUtilities.trimEmptyToDefault("  value  ", "default"));
    }
}
