package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.junit.Test;

public class StringUtilitiesTest {

    // ----------------------------------------------------------------------
    // equals / equalsIgnoreCase
    // ----------------------------------------------------------------------
    @Test
    public void testEqualsBasic() {
        assertTrue(StringUtilities.equals((String) null, null));
        assertFalse(StringUtilities.equals((String) null, "a"));
        assertFalse(StringUtilities.equals("a", null));
        assertTrue(StringUtilities.equals("", ""));
        assertTrue(StringUtilities.equals("abc", "abc"));
        assertFalse(StringUtilities.equals("abc", "ABC"));
        // CharSequence implementation (StringBuilder)
        assertTrue(StringUtilities.equals(new StringBuilder("xyz"), new StringBuilder("xyz")));
        assertFalse(StringUtilities.equals(new StringBuilder("xyz"), new StringBuilder("XYz")));
    }

    @Test
    public void testEqualsIgnoreCaseBasic() {
        assertTrue(StringUtilities.equalsIgnoreCase((String) null, null));
        assertFalse(StringUtilities.equalsIgnoreCase(null, "a"));
        assertFalse(StringUtilities.equalsIgnoreCase("a", null));
        assertTrue(StringUtilities.equalsIgnoreCase("", ""));
        assertTrue(StringUtilities.equalsIgnoreCase("abc", "ABC"));
        assertTrue(StringUtilities.equalsIgnoreCase("AbC", "aBc"));
        // CharSequence implementation
        assertTrue(StringUtilities.equalsIgnoreCase(new StringBuilder("TeSt"), new StringBuilder("tEsT")));
    }

    // ----------------------------------------------------------------------
    // equalsWithTrim / equalsIgnoreCaseWithTrim
    // ----------------------------------------------------------------------
    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  hello ", "hello"));
        assertFalse(StringUtilities.equalsWithTrim("hello", "hello "));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim(null, "a"));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  HeLLo ", "hello"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("\tWorld\n", "world"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("abc", "def"));
    }

    // ----------------------------------------------------------------------
    // isEmpty / isWhitespace / hasContent
    // ----------------------------------------------------------------------
    @Test
    public void testIsEmptyAndWhitespace() {
        assertTrue(StringUtilities.isEmpty((String) null));
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty("   "));
        assertTrue(StringUtilities.isWhitespace("\t\n"));
        assertFalse(StringUtilities.isEmpty(" x "));
        assertFalse(StringUtilities.isWhitespace("a"));
    }

    @Test
    public void testHasContent() {
        assertFalse(StringUtilities.hasContent(null));
        assertFalse(StringUtilities.hasContent("   "));
        assertTrue(StringUtilities.hasContent(" x "));
    }

    // ----------------------------------------------------------------------
    // length / trimLength
    // ----------------------------------------------------------------------
    @Test
    public void testLength() {
        assertEquals(0, StringUtilities.length((String) null));
        assertEquals(0, StringUtilities.length(""));
        assertEquals(5, StringUtilities.length("hello"));
    }

    @Test
    public void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(0, StringUtilities.trimLength("   "));
        assertEquals(5, StringUtilities.trimLength("  hello  "));
    }

    // ----------------------------------------------------------------------
    // lastIndexOf
    // ----------------------------------------------------------------------
    @Test
    public void testLastIndexOf() {
        assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        assertEquals(3, StringUtilities.lastIndexOf("a/b/c", '/'));
        assertEquals(0, StringUtilities.lastIndexOf("x", 'x'));
    }

    // ----------------------------------------------------------------------
    // encode / decode
    // ----------------------------------------------------------------------
    @Test
    public void testEncodeDecodeHex() {
        byte[] data = {0x0F, (byte)0xA5, 0x00, 0x7B};
        String encoded = StringUtilities.encode(data);
        assertEquals("0FA5007B", encoded);
        byte[] decoded = StringUtilities.decode(encoded);
        assertArrayEquals(data, decoded);
        // odd length should return null
        assertNull(StringUtilities.decode("ABC"));
    }

    // ----------------------------------------------------------------------
    // count
    // ----------------------------------------------------------------------
    @Test
    public void testCount() {
        assertEquals(0, StringUtilities.count(null, "a"));
        assertEquals(0, StringUtilities.count("abc", null));
        assertEquals(0, StringUtilities.count("", "a"));
        assertEquals(0, StringUtilities.count("abc", ""));
        assertEquals(2, StringUtilities.count("ababab", "ab"));
        assertEquals(3, StringUtilities.count("aaa", 'a'));
    }

    // ----------------------------------------------------------------------
    // wildcardToRegexString
    // ----------------------------------------------------------------------
    @Test
    public void testWildcardToRegexString() {
        String regex = StringUtilities.wildcardToRegexString("file*.txt");
        assertTrue("file123.txt".matches(regex));
        assertFalse("file123.tx".matches(regex));

        String regex2 = StringUtilities.wildcardToRegexString("data??.csv");
        assertTrue("data12.csv".matches(regex2));
        assertFalse("data1.csv".matches(regex2));
    }

    // ----------------------------------------------------------------------
    // levenshteinDistance
    // ----------------------------------------------------------------------
    @Test
    public void testLevenshteinDistance() {
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        assertEquals(2, StringUtilities.levenshteinDistance(null, "ab"));
        assertEquals(2, StringUtilities.levenshteinDistance("ab", null));
        assertEquals(0, StringUtilities.levenshteinDistance(null, null));
    }

    // ----------------------------------------------------------------------
    // damerauLevenshteinDistance
    // ----------------------------------------------------------------------
    @Test
    public void testDamerauLevenshteinDistance() {
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("book", "back")); // transposition
        assertEquals(3, StringUtilities.damerauLevenshteinDistance("abcd", "acbd")); // one transposition + insert
    }

    // ----------------------------------------------------------------------
    // Random string generation
    // ----------------------------------------------------------------------
    @Test
    public void testRandomStringGeneration() {
        Random rnd = new Random(12345L);
        String s = StringUtilities.getRandomString(rnd, 5, 5);
        assertEquals(5, s.length());
        assertTrue(Character.isUpperCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            assertTrue(Character.isLowerCase(s.charAt(i)));
        }
    }

    // ----------------------------------------------------------------------
    // Encoding/decoding bytes with charset
    // ----------------------------------------------------------------------
    @Test
    public void testGetBytesAndCreateString() {
        String original = "café";
        byte[] utf8 = StringUtilities.getBytes(original, StandardCharsets.UTF_8.name());
        assertNotNull(utf8);
        String roundTrip = StringUtilities.createString(utf8, StandardCharsets.UTF_8.name());
        assertEquals(original, roundTrip);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBytesUnsupportedEncoding() {
        StringUtilities.getBytes("test", "UNSUPPORTED-ENCODING");
    }

    // ----------------------------------------------------------------------
    // UTF‑8 helpers
    // ----------------------------------------------------------------------
    @Test
    public void testUtf8Helpers() {
        String txt = "Hello 🌍";
        byte[] bytes = StringUtilities.getUTF8Bytes(txt);
        assertArrayEquals(txt.getBytes(StandardCharsets.UTF_8), bytes);
        assertEquals(txt, StringUtilities.createUTF8String(bytes));
        // deprecated method should behave the same
        assertEquals(txt, StringUtilities.createUtf8String(bytes));
    }

    // ----------------------------------------------------------------------
    // hashCodeIgnoreCase
    // ----------------------------------------------------------------------
    @Test
    public void testHashCodeIgnoreCase() {
        String a = "TestString";
        String b = "teststring";
        assertEquals(StringUtilities.hashCodeIgnoreCase(a), StringUtilities.hashCodeIgnoreCase(b));

        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    // ----------------------------------------------------------------------
    // trim / trimToEmpty / trimToNull / trimEmptyToDefault
    // ----------------------------------------------------------------------
    @Test
    public void testTrimMethods() {
        assertNull(StringUtilities.trim(null));
        assertEquals("abc", StringUtilities.trim("  abc  "));

        assertEquals("", StringUtilities.trimToEmpty(null));
        assertEquals("xyz", StringUtilities.trimToEmpty("  xyz "));

        assertNull(StringUtilities.trimToNull("   "));
        assertEquals("data", StringUtilities.trimToNull(" data "));

        String def = "default";
        assertEquals(def, StringUtilities.trimEmptyToDefault(null, def));
        assertEquals(def, StringUtilities.trimEmptyToDefault("   ", def));
        assertEquals("val", StringUtilities.trimEmptyToDefault("  val ", def));
    }

    // ----------------------------------------------------------------------
    // removeLeadingAndTrailingQuotes
    // ----------------------------------------------------------------------
    @Test
    public void testRemoveLeadingAndTrailingQuotes() {
        assertNull(StringUtilities.removeLeadingAndTrailingQuotes(null));
        assertEquals("", StringUtilities.removeLeadingAndTrailingQuotes(""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"text\""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"text\"\""));
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"\"text\"\"\""));
        assertEquals("text \"inside\" quotes", StringUtilities.removeLeadingAndTrailingQuotes("\"text \"inside\" quotes\""));
        assertEquals("noquotes", StringUtilities.removeLeadingAndTrailingQuotes("noquotes"));
    }

    // ----------------------------------------------------------------------
    // commaSeparatedStringToSet
    // ----------------------------------------------------------------------
    @Test
    public void testCommaSeparatedStringToSet() {
        Set<String> empty = StringUtilities.commaSeparatedStringToSet(null);
        assertTrue(empty.isEmpty());

        Set<String> empty2 = StringUtilities.commaSeparatedStringToSet("   ");
        assertTrue(empty2.isEmpty());

        Set<String> result = StringUtilities.commaSeparatedStringToSet("a, b, ,c, a ,");
        // order is not guaranteed, just check content
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), result);
    }
}
