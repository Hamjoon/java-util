package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import java.util.*;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

public class StringUtilitiesTest {

    /* ---------- 1. equals / equalsIgnoreCase ---------- */

    @Test
    public void testEqualsCharSequence() {
        assertTrue(StringUtilities.equals("abc", new StringBuilder("abc")));
        assertFalse(StringUtilities.equals("abc", "ab"));
        assertTrue(StringUtilities.equals(null, null));
        assertFalse(StringUtilities.equals(null, "a"));
    }

    @Test
    public void testEqualsString() {
        assertTrue(StringUtilities.equals("test", "test"));
        assertFalse(StringUtilities.equals("test", "Test"));
        assertTrue(StringUtilities.equals(null, null));
        assertFalse(StringUtilities.equals("x", null));
    }

    @Test
    public void testEqualsIgnoreCaseCharSequence() {
        assertTrue(StringUtilities.equalsIgnoreCase("AbC", new StringBuilder("aBc")));
        assertFalse(StringUtilities.equalsIgnoreCase("abc", "ab"));
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        assertFalse(StringUtilities.equalsIgnoreCase(null, "a"));
    }

    @Test
    public void testEqualsIgnoreCaseString() {
        assertTrue(StringUtilities.equalsIgnoreCase("TeSt", "test"));
        assertFalse(StringUtilities.equalsIgnoreCase("abc", "def"));
        assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        assertFalse(StringUtilities.equalsIgnoreCase("x", null));
    }

    @Test
    public void testEqualsWithTrim() {
        assertTrue(StringUtilities.equalsWithTrim("  hello ", "hello"));
        assertFalse(StringUtilities.equalsWithTrim("hello", "world"));
        assertTrue(StringUtilities.equalsWithTrim(null, null));
        assertFalse(StringUtilities.equalsWithTrim(null, "a"));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  HeLLo ", "hello"));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("abc", "def"));
        assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(null, null));
        assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("x", null));
    }

    /* ---------- 2. isEmpty / isWhitespace / hasContent ---------- */

    @Test
    public void testIsEmpty() {
        assertTrue(StringUtilities.isEmpty((CharSequence) null));
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty("   "));
        assertFalse(StringUtilities.isEmpty(" a "));
    }

    @Test
    public void testIsEmptyString() {
        assertTrue(StringUtilities.isEmpty((String) null));
        assertTrue(StringUtilities.isEmpty(""));
        assertTrue(StringUtilities.isEmpty(" \t\n"));
        assertFalse(StringUtilities.isEmpty("x"));
    }

    @Test
    public void testIsWhitespace() {
        assertTrue(StringUtilities.isWhitespace(null));
        assertTrue(StringUtilities.isWhitespace(""));
        assertTrue(StringUtilities.isWhitespace(" \t"));
        assertFalse(StringUtilities.isWhitespace("a "));
    }

    @Test
    public void testHasContent() {
        assertFalse(StringUtilities.hasContent(null));
        assertFalse(StringUtilities.hasContent("   "));
        assertTrue(StringUtilities.hasContent("  x "));
    }

    /* ---------- 3. length / trimLength ---------- */

    @Test
    public void testLength() {
        assertEquals(0, StringUtilities.length((CharSequence) null));
        assertEquals(0, StringUtilities.length(""));
        assertEquals(5, StringUtilities.length("hello"));
    }

    @Test
    public void testLengthString() {
        assertEquals(0, StringUtilities.length((String) null));
        assertEquals(3, StringUtilities.length("abc"));
    }

    @Test
    public void testTrimLength() {
        assertEquals(0, StringUtilities.trimLength(null));
        assertEquals(0, StringUtilities.trimLength("   "));
        assertEquals(5, StringUtilities.trimLength("  hello  "));
    }

    /* ---------- 4. lastIndexOf ---------- */

    @Test
    public void testLastIndexOf() {
        assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        assertEquals(4, StringUtilities.lastIndexOf("a/b/c/d", '/'));
        assertEquals(-1, StringUtilities.lastIndexOf("abc", 'z'));
    }

    /* ---------- 5. decode / encode ---------- */

    @Test
    public void testDecodeValid() {
        byte[] expected = new byte[]{ (byte)0x0F, (byte)0xA0, (byte)0x5C };
        assertArrayEquals(expected, StringUtilities.decode("0FA05C"));
    }

    @Test
    public void testDecodeOddLength() {
        assertNull(StringUtilities.decode("ABC")); // odd length → null
    }

    @Test
    public void testEncodeEmpty() {
        assertEquals("", StringUtilities.encode(new byte[0]));
    }

    @Test
    public void testEncodeNormal() {
        byte[] data = new byte[]{ (byte)0xDE, (byte)0xAD, (byte)0xBE, (byte)0xEF };
        // Expected hex string using the same HEX_ARRAY logic (uppercase)
        assertEquals("DEADBEEF", StringUtilities.encode(data));
    }

    /* ---------- 6. count ---------- */

    @Test
    public void testCountChar() {
        assertEquals(3, StringUtilities.count("banana", 'a'));
        assertEquals(0, StringUtilities.count("banana", 'z'));
    }

    @Test
    public void testCountCharSequence() {
        assertEquals(2, StringUtilities.count("ababa", "aba"));
        assertEquals(0, StringUtilities.count("abc", "d"));
        assertEquals(0, StringUtilities.count(null, "a"));
        assertEquals(0, StringUtilities.count("abc", null));
        assertEquals(0, StringUtilities.count("", "a"));
        assertEquals(0, StringUtilities.count("abc", ""));
    }

    /* ---------- 7. wildcardToRegexString ---------- */

    @Test
    public void testWildcardToRegexString() {
        String pattern = StringUtilities.wildcardToRegexString("file-?.*");
        // Should translate to "^file-..*$"
        assertEquals("^file-..*$", pattern);
    }

    @Test
    public void testWildcardSpecialCharsEscaped() {
        String pattern = StringUtilities.wildcardToRegexString("a(b)c[d]e$^.{|}\\");
        assertEquals("^a\\(b\\)c\\[d\\]e\\$\\^\\.\\{\\|\\}\\\\$", pattern);
    }

    /* ---------- 8. levenshteinDistance ---------- */

    @Test
    public void testLevenshteinBasic() {
        assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        assertEquals(5, StringUtilities.levenshteinDistance(null, "hello"));
        assertEquals(5, StringUtilities.levenshteinDistance("hello", null));
    }

    /* ---------- 9. damerauLevenshteinDistance ---------- */

    @Test
    public void testDamerauLevenshteinBasic() {
        assertEquals(2, StringUtilities.damerauLevenshteinDistance("book", "back")); // b o o k -> b a c k (o->a, o->c)
        assertEquals(1, StringUtilities.damerauLevenshteinDistance("ab", "ba")); // transposition
        assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        assertEquals(4, StringUtilities.damerauLevenshteinDistance(null, "test"));
    }

    /* ---------- 10. Random string utilities ---------- */

    private Random fixedRandom;

    @Before
    public void setUpRandom() {
        fixedRandom = new Random(12345L); // deterministic seed
    }

    @Test
    public void testGetRandomStringLengthWithinBounds() {
        String s = StringUtilities.getRandomString(fixedRandom, 5, 5);
        assertEquals(5, s.length());
        // First character must be uppercase, others lowercase
        assertTrue(Character.isUpperCase(s.charAt(0)));
        for (int i = 1; i < s.length(); i++) {
            assertTrue(Character.isLowerCase(s.charAt(i)));
        }
    }

    @Test
    public void testGetRandomStringVariableLength() {
        String s = StringUtilities.getRandomString(fixedRandom, 3, 7);
        assertTrue(s.length() >= 3 && s.length() <= 7);
    }

    @Test
    public void testGetRandomCharUpperAndLower() {
        String upper = StringUtilities.getRandomChar(new Random(1L), true);
        String lower = StringUtilities.getRandomChar(new Random(1L), false);
        assertEquals(1, upper.length());
        assertEquals(1, lower.length());
        assertTrue(Character.isUpperCase(upper.charAt(0)));
        assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    /* ---------- 11. Encoding / decoding byte arrays ---------- */

    @Test
    public void testGetBytesValidEncoding() {
        byte[] bytes = StringUtilities.getBytes("hello", "UTF-8");
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    public void testGetBytesUnsupportedEncoding() {
        try {
            StringUtilities.getBytes("test", "UNSUPPORTED-ENCODING");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Encoding (UNSUPPORTED-ENCODING)"));
        }
    }

    @Test
    public void testCreateStringValidEncoding() {
        byte[] data = "world".getBytes(StandardCharsets.UTF_8);
        String s = StringUtilities.createString(data, "UTF-8");
        assertEquals("world", s);
    }

    @Test
    public void testCreateStringUnsupportedEncoding() {
        try {
            StringUtilities.createString(new byte[]{0x61}, "BAD-ENC");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Encoding (BAD-ENC)"));
        }
    }

    @Test
    public void testCreateUtf8StringDeprecated() {
        byte[] data = "abc".getBytes(StandardCharsets.UTF_8);
        assertEquals("abc", StringUtilities.createUtf8String(data));
    }

    @Test
    public void testCreateUTF8String() {
        byte[] data = "xyz".getBytes(StandardCharsets.UTF_8);
        assertEquals("xyz", StringUtilities.createUTF8String(data));
    }

    @Test
    public void testGetUTF8Bytes() {
        byte[] expected = "sample".getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, StringUtilities.getUTF8Bytes("sample"));
    }

    /* ---------- 12. hashCodeIgnoreCase ---------- */

    @Test
    public void testHashCodeIgnoreCase() {
        String a = "AbCdeF";
        String b = "aBcDeF";
        assertEquals(StringUtilities.hashCodeIgnoreCase(a), StringUtilities.hashCodeIgnoreCase(b));
        assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    /* ---------- 13. trim family ---------- */

    @Test
    public void testTrim() {
        assertNull(StringUtilities.trim(null));
        assertEquals("abc", StringUtilities.trim("  abc  "));
    }

    @Test
    public void testTrimToEmpty() {
        assertEquals("", StringUtilities.trimToEmpty(null));
        assertEquals("", StringUtilities.trimToEmpty("   "));
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
        assertEquals("def", StringUtilities.trimEmptyToDefault(null, "def"));
        assertEquals("def", StringUtilities.trimEmptyToDefault("   ", "def"));
        assertEquals("value", StringUtilities.trimEmptyToDefault("  value  ", "def"));
    }

    /* ---------- 14. removeLeadingAndTrailingQuotes ---------- */

    @Test
    public void testRemoveQuotesSimple() {
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"text\""));
    }

    @Test
    public void testRemoveQuotesMultiple() {
        assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"\"text\"\"\""));
    }

    @Test
    public void testRemoveQuotesInternal() {
        assertEquals("text with \"quotes\" inside", 
            StringUtilities.removeLeadingAndTrailingQuotes("\"text with \"quotes\" inside\""));
    }

    @Test
    public void testRemoveQuotesNoQuotes() {
        assertEquals("plain", StringUtilities.removeLeadingAndTrailingQuotes("plain"));
    }

    @Test
    public void testRemoveQuotesEmptyOrNull() {
        assertNull(StringUtilities.removeLeadingAndTrailingQuotes(null));
        assertEquals("", StringUtilities.removeLeadingAndTrailingQuotes(""));
    }

    /* ---------- 15. commaSeparatedStringToSet ---------- */

    @Test
    public void testCommaSeparatedStringToSetNormal() {
        Set<String> set = StringUtilities.commaSeparatedStringToSet("a, b ,c, a , ");
        Set<String> expected = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertEquals(expected, set);
    }

    @Test
    public void testCommaSeparatedStringToSetEmptyOrNull() {
        assertTrue(StringUtilities.commaSeparatedStringToSet(null).isEmpty());
        assertTrue(StringUtilities.commaSeparatedStringToSet("   ").isEmpty());
    }

    @Test
    public void testCommaSeparatedStringToSetSingleElement() {
        Set<String> set = StringUtilities.commaSeparatedStringToSet("single");
        assertEquals(Collections.singleton("single"), set);
    }
}
