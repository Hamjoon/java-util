package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

public class StringUtilitiesTest {

    private Random deterministicRandom;

    @Before
    public void setUp() {
        // deterministic Random for reproducible tests
        deterministicRandom = new Random(12345L);
    }

    @After
    public void tearDown() {
        deterministicRandom = null;
    }

    /* -------------------------------------------------
     * equals(CharSequence, CharSequence)
     * ------------------------------------------------- */
    @Test
    public void testEqualsCharSequence_BasicCases() {
        // both null
        assertTrue(StringUtilities.equals((CharSequence) null, null));

        // one null
        assertFalse(StringUtilities.equals("a", null));
        assertFalse(StringUtilities.equals(null, "a"));

        // identical strings
        assertTrue(StringUtilities.equals("hello", "hello"));

        // different case – should be false (case‑sensitive)
        assertFalse(StringUtilities.equals("Hello", "hello"));

        // different length
        assertFalse(StringUtilities.equals("abc", "ab"));

        // different CharSequence implementations
        CharSequence sb1 = new StringBuilder("test");
        CharSequence sb2 = new StringBuffer("test");
        assertTrue(StringUtilities.equals(sb1, sb2));
    }

    /* -------------------------------------------------
     * equalsIgnoreCase(CharSequence, CharSequence)
     * ------------------------------------------------- */
    @Test
    public void testEqualsIgnoreCaseCharSequence() {
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        assertFalse(StringUtilities.equalsIgnoreCase("a", null));
        assertFalse(StringUtilities.equalsIgnoreCase(null, "a"));

        assertTrue(StringUtilities.equalsIgnoreCase("Java", "java"));
        assertTrue(StringUtilities.equalsIgnoreCase("TeSt", "tEsT"));

        // different length
        assertFalse(StringUtilities.equalsIgnoreCase("abc", "ab"));

        // verify regionMatches fallback for non‑String CharSequence
        CharSequence cs1 = new StringBuilder("MiXeD");
        CharSequence cs2 = new StringBuffer("mixed");
        assertTrue(StringUtilities.equalsIgnoreCase(cs1, cs2));
    }

    /* -------------------------------------------------
     * equalsWithTrim(String, String)
     * ------------------------------------------------- */
    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  foo  ", "foo"));
        assertTrue(StringUtilities.equalsWithTrim("\tbar\n", "bar"));
        assertFalse(StringUtilities.equalsWithTrim("baz", "ba"));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim("x", null));
    }

    /* -------------------------------------------------
     * equalsIgnoreCaseWithTrim(String, String)
     * ------------------------------------------------- */
    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  Foo ", "foo"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("\tBaR\n", "bAr"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("baz", "ba"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(null, null));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("x", null));
    }

    /* -------------------------------------------------
     * isEmpty / isWhitespace
     * ------------------------------------------------- */
    @Test
    public void testIsEmptyAndIsWhitespace() {
        assertTrue(StringUtilities.isEmpty((CharSequence) null));
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty("   "));
        assertTrue(StringUtilities.isWhitespace("\t\n\r"));
        assertFalse(StringUtilities.isEmpty("a"));
        assertFalse(StringUtilities.isWhitespace(" a "));
    }

    /* -------------------------------------------------
     * hasContent
     * ------------------------------------------------- */
    @Test
    public void testHasContent() {
        assertFalse(StringUtilities.hasContent(null));
        assertFalse(StringUtilities.hasContent(""));
        assertFalse(StringUtilities.hasContent("   "));
        assertTrue(StringUtilities.hasContent(" x "));
    }

    /* -------------------------------------------------
     * length
     * ------------------------------------------------- */
    @Test
    public void testLength() {
        assertEquals(0, StringUtilities.length((CharSequence) null));
        assertEquals(5, StringUtilities.length("hello"));
        assertEquals(0, StringUtilities.length(""));
    }

    /* -------------------------------------------------
     * trimLength
     * ------------------------------------------------- */
    @Test
    public void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(3, StringUtilities.trimLength("  abc  "));
        assertEquals(0, StringUtilities.trimLength("   "));
    }

    /* -------------------------------------------------
     * lastIndexOf
     * ------------------------------------------------- */
    @Test
    public void testLastIndexOf() {
        assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        assertEquals(5, StringUtilities.lastIndexOf("a/b/c/d", '/'));
        assertEquals(-1, StringUtilities.lastIndexOf("abc", '/'));
    }

    /* -------------------------------------------------
     * decode / encode
     * ------------------------------------------------- */
    @Test
    public void testEncodeDecodeHex() {
        byte[] data = {0x00, 0x0F, (byte) 0xA5, (byte) 0xFF};
        String encoded = StringUtilities.encode(data);
        assertEquals("000FA5FF", encoded);

        byte[] decoded = StringUtilities.decode(encoded);
        assertArrayEquals(data, decoded);

        // odd length string should return null
        assertNull(StringUtilities.decode("ABC"));
    }

    /* -------------------------------------------------
     * count
     * ------------------------------------------------- */
    @Test
    public void testCount() {
        assertEquals(0, StringUtilities.count(null, "a"));
        assertEquals(0, StringUtilities.count("abc", null));
        assertEquals(0, StringUtilities.count("", "a"));
        assertEquals(0, StringUtilities.count("abc", ""));
        assertEquals(2, StringUtilities.count("ababa", "aba")); // overlapping allowed by implementation
        assertEquals(3, StringUtilities.count("aaa", "a"));
        assertEquals(1, StringUtilities.count("abc", "bc"));
    }

    /* -------------------------------------------------
     * wildcardToRegexString
     * ------------------------------------------------- */
    @Test
    public void testWildcardToRegexString() {
        String pattern = StringUtilities.wildcardToRegexString("file-?.txt");
        assertEquals("^file-..txt$", pattern);

        pattern = StringUtilities.wildcardToRegexString("*.java");
        assertEquals("^.*\\.java$", pattern);

        // special regex chars are escaped
        pattern = StringUtilities.wildcardToRegexString("a(b)c");
        assertEquals("^a\\(b\\)c$", pattern);
    }

    /* -------------------------------------------------
     * levenshteinDistance
     * ------------------------------------------------- */
    @Test
    public void testLevenshteinDistance() {
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        assertEquals(2, StringUtilities.levenshteinDistance("flaw", "lawn"));
        assertEquals(5, StringUtilities.levenshteinDistance(null, "hello"));
        assertEquals(5, StringUtilities.levenshteinDistance("hello", null));
    }

    /* -------------------------------------------------
     * damerauLevenshteinDistance
     * ------------------------------------------------- */
    @Test
    public void testDamerauLevenshteinDistance() {
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("book", "back"));
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("ca", "ac")); // transposition
        assertEquals(4, StringUtilities.damerauLevenshteinDistance(null, "test"));
        assertEquals(4, StringUtilities.damerauLevenshteinDistance("test", null));
    }

    /* -------------------------------------------------
     * getRandomString / getRandomChar
     * ------------------------------------------------- */
    @Test
    public void testGetRandomString() {
        String random = StringUtilities.getRandomString(deterministicRandom, 5, 5);
        assertEquals(5, random.length());
        // first character must be uppercase, rest lowercase
        assertTrue(Character.isUpperCase(random.charAt(0)));
        for (int i = 1; i < random.length(); i++) {
            assertTrue(Character.isLowerCase(random.charAt(i)));
        }
    }

    @Test
    public void testGetRandomChar() {
        // upper = true
        String upper = StringUtilities.getRandomChar(new Random(1L), true);
        assertEquals(1, upper.length());
        assertTrue(Character.isUpperCase(upper.charAt(0)));

        // upper = false
        String lower = StringUtilities.getRandomChar(new Random(1L), false);
        assertEquals(1, lower.length());
        assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    /* -------------------------------------------------
     * getBytes / createString (encoding handling)
     * ------------------------------------------------- */
    @Test
    public void testEncodingUtilities() {
        String original = "café";
        byte[] utf8 = StringUtilities.getUTF8Bytes(original);
        assertArrayEquals(original.getBytes(StandardCharsets.UTF_8), utf8);

        String roundTrip = StringUtilities.createUTF8String(utf8);
        assertEquals(original, roundTrip);

        // supported encoding
        byte[] iso = StringUtilities.getBytes(original, "ISO-8859-1");
        assertNotNull(iso);
        String fromIso = StringUtilities.createString(iso, "ISO-8859-1");
        assertEquals(original, fromIso);

        // unsupported encoding should throw IllegalArgumentException
        try {
            StringUtilities.getBytes(original, "UNSUPPORTED-ENCODING");
            fail("Expected IllegalArgumentException for unsupported encoding");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Encoding (UNSUPPORTED-ENCODING) is not supported"));
        }

        try {
            StringUtilities.createString(iso, "UNSUPPORTED-ENCODING");
            fail("Expected IllegalArgumentException for unsupported encoding");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Encoding (UNSUPPORTED-ENCODING) is not supported"));
        }
    }

    /* -------------------------------------------------
     * hashCodeIgnoreCase
     * ------------------------------------------------- */
    @Test
    public void testHashCodeIgnoreCase() {
        String s1 = "AbCdeF";
        String s2 = "abcdef";
        assertEquals(StringUtilities.hashCodeIgnoreCase(s1), StringUtilities.hashCodeIgnoreCase(s2));

        // null returns 0
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));

        // different strings
        assertNotEquals(StringUtilities.hashCodeIgnoreCase("abc"), StringUtilities.hashCodeIgnoreCase("abd"));
    }

    /* -------------------------------------------------
     * trim / trimToEmpty / trimToNull / trimEmptyToDefault
     * ------------------------------------------------- */
    @Test
    public void testTrimMethods() {
        assertNull(StringUtilities.trim(null));
        assertEquals("x", StringUtilities.trim("  x  "));

        assertEquals("", StringUtilities.trimToEmpty(null));
        assertEquals("y", StringUtilities.trimToEmpty("  y  "));

        assertNull(StringUtilities.trimToNull("   "));
        assertEquals("z", StringUtilities.trimToNull("  z  "));

        assertEquals("default", StringUtilities.trimEmptyToDefault("   ", "default"));
        assertEquals("value", StringUtilities.trimEmptyToDefault("  value  ", "default"));
        assertEquals("fallback", StringUtilities.trimEmptyToDefault(null, "fallback"));
    }

    /* -------------------------------------------------
     * removeLeadingAndTrailingQuotes
     * ------------------------------------------------- */
    @Test
    public void testRemoveLeadingAndTrailingQuotes() {
        assertNull(StringUtilities.removeLeadingAndTrailingQuotes(null));
        assertEquals("", StringUtilities.removeLeadingAndTrailingQuotes(""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"text\""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"text\"\""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"\"text\"\"\""));
        assertEquals("text with \"quotes\" inside", StringUtilities.removeLeadingAndTrailingQuotes("\"text with \"quotes\" inside\""));
    }

    /* -------------------------------------------------
     * commaSeparatedStringToSet
     * ------------------------------------------------- */
    @Test
    public void testCommaSeparatedStringToSet() {
        Set<String> empty = StringUtilities.commaSeparatedStringToSet(null);
        assertTrue(empty.isEmpty());

        empty = StringUtilities.commaSeparatedStringToSet("   ");
        assertTrue(empty.isEmpty());

        Set<String> result = StringUtilities.commaSeparatedStringToSet("a,b, c ,a,,");
        assertEquals(3, result.size());
        assertTrue(result.containsAll(Set.of("a", "b", "c")));
    }
}
