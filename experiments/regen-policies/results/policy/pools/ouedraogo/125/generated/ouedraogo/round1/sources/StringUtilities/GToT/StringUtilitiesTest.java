package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * Comprehensive JUnit‑4 test suite for {@link StringUtilities}.
 *
 * The test class covers:
 * <ul>
 *   <li>All public static methods (basic functionality)</li>
 *   <li>Edge‑case handling (null, empty, whitespace, odd length, invalid encoding, etc.)</li>
 *   <li>Exception scenarios (UnsupportedEncodingException wrapped in IllegalArgumentException)</li>
 * </ul>
 *
 * The tests are deliberately deterministic – e.g. a {@link Random} instance with a fixed seed is used
 * for the random‑string generators.
 */
public class StringUtilitiesTest {

    /* ----------------------------------------------------------------------
     *  equals / equalsIgnoreCase (CharSequence & String overloads)
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsCharSequenceBasic() {
        Assert.assertTrue(StringUtilities.equals("abc", "abc"));
        Assert.assertFalse(StringUtilities.equals("abc", "ABC"));
        Assert.assertFalse(StringUtilities.equals("abc", "abcd"));
    }

    @Test
    public void testEqualsCharSequenceNulls() {
        Assert.assertTrue(StringUtilities.equals(null, null));
        Assert.assertFalse(StringUtilities.equals(null, "abc"));
        Assert.assertFalse(StringUtilities.equals("abc", null));
    }

    @Test
    public void testEqualsStringOverload() {
        Assert.assertTrue(StringUtilities.equals((String) null, (String) null));
        Assert.assertFalse(StringUtilities.equals((String) null, "x"));
        Assert.assertTrue(StringUtilities.equals("same", "same"));
    }

    @Test
    public void testEqualsIgnoreCaseCharSequence() {
        Assert.assertTrue(StringUtilities.equalsIgnoreCase("abc", "ABC"));
        Assert.assertFalse(StringUtilities.equalsIgnoreCase("abc", "AB"));
        Assert.assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        Assert.assertFalse(StringUtilities.equalsIgnoreCase(null, "a"));
    }

    @Test
    public void testEqualsIgnoreCaseStringOverload() {
        Assert.assertTrue(StringUtilities.equalsIgnoreCase((String) null, (String) null));
        Assert.assertTrue(StringUtilities.equalsIgnoreCase("TeSt", "test"));
        Assert.assertFalse(StringUtilities.equalsIgnoreCase("Test", "Tes"));
    }

    @Test
    public void testEqualsWithTrim() {
        Assert.assertTrue(StringUtilities.equalsWithTrim("  abc  ", "abc"));
        Assert.assertFalse(StringUtilities.equalsWithTrim("abc ", "ab"));
        Assert.assertTrue(StringUtilities.equalsWithTrim(null, null));
        Assert.assertFalse(StringUtilities.equalsWithTrim(null, "x"));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  aBc  ", "ABC"));
        Assert.assertFalse(StringUtilities.equalsIgnoreCaseWithTrim("a", "b"));
        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim(null, null));
    }

    /* ----------------------------------------------------------------------
     *  isEmpty / isWhitespace / hasContent
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsEmptyAndIsWhitespace() {
        Assert.assertTrue(StringUtilities.isEmpty((String) null));
        Assert.assertTrue(StringUtilities.isEmpty(""));
        Assert.assertTrue(StringUtilities.isEmpty("   "));
        Assert.assertFalse(StringUtilities.isEmpty(" x "));
        Assert.assertTrue(StringUtilities.isWhitespace(null));
        Assert.assertTrue(StringUtilities.isWhitespace("\t\n"));
        Assert.assertFalse(StringUtilities.isWhitespace("a"));
    }

    @Test
    public void testHasContent() {
        Assert.assertFalse(StringUtilities.hasContent(null));
        Assert.assertFalse(StringUtilities.hasContent(""));
        Assert.assertFalse(StringUtilities.hasContent("   "));
        Assert.assertTrue(StringUtilities.hasContent("x"));
    }

    /* ----------------------------------------------------------------------
     *  length / trimLength
     * ---------------------------------------------------------------------- */

    @Test
    public void testLength() {
        Assert.assertEquals(0, StringUtilities.length((String) null));
        Assert.assertEquals(0, StringUtilities.length(""));
        Assert.assertEquals(3, StringUtilities.length("abc"));
    }

    @Test
    public void testTrimLength() {
        Assert.assertEquals(0, StringUtilities.trimLength(null));
        Assert.assertEquals(0, StringUtilities.trimLength("   "));
        Assert.assertEquals(3, StringUtilities.trimLength("  abc  "));
    }

    /* ----------------------------------------------------------------------
     *  lastIndexOf
     * ---------------------------------------------------------------------- */

    @Test
    public void testLastIndexOf() {
        Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        Assert.assertEquals(5, StringUtilities.lastIndexOf("a/b/c/d", '/'));
        Assert.assertEquals(0, StringUtilities.lastIndexOf("/", '/'));
    }

    /* ----------------------------------------------------------------------
     *  encode / decode (hex conversion)
     * ---------------------------------------------------------------------- */

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] data = new byte[]{0x00, 0x0F, (byte) 0xA5, (byte) 0xFF};
        String encoded = StringUtilities.encode(data);
        byte[] decoded = StringUtilities.decode(encoded);
        Assert.assertArrayEquals(data, decoded);
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        Assert.assertNull(StringUtilities.decode("ABC")); // 3 chars → odd
    }

    @Test
    public void testDecodeInvalidHexDigits() {
        // Character.digit returns -1 for non‑hex; the method does not guard against it.
        // Expect resulting bytes to contain -1 values (as per conversion logic).
        byte[] result = StringUtilities.decode("GG");
        Assert.assertEquals(1, result.length);
        Assert.assertEquals((byte) ((-1 << 4) + -1), result[0]); // -17
    }

    /* ----------------------------------------------------------------------
     *  count (String & CharSequence overloads)
     * ---------------------------------------------------------------------- */

    @Test
    public void testCountCharAndString() {
        Assert.assertEquals(3, StringUtilities.count("banana", 'a'));
        Assert.assertEquals(2, StringUtilities.count("banana", "an"));
        Assert.assertEquals(0, StringUtilities.count(null, "a"));
        Assert.assertEquals(0, StringUtilities.count("abc", null));
        Assert.assertEquals(0, StringUtilities.count("", "a"));
        Assert.assertEquals(0, StringUtilities.count("abc", ""));
    }

    /* ----------------------------------------------------------------------
     *  wildcardToRegexString
     * ---------------------------------------------------------------------- */

    @Test
    public void testWildcardToRegexString() {
        String wildcard = "*file?.txt";
        String regex = StringUtilities.wildcardToRegexString(wildcard);
        Assert.assertEquals("^.*file.\\.txt$", regex);
        // Verify that the regex actually matches a sample string
        Assert.assertTrue("myfile1.txt".matches(regex));
        Assert.assertFalse("myfile12.txt".matches(regex));
    }

    /* ----------------------------------------------------------------------
     *  levenshteinDistance
     * ---------------------------------------------------------------------- */

    @Test
    public void testLevenshteinBasic() {
        Assert.assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        Assert.assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        Assert.assertEquals(5, StringUtilities.levenshteinDistance("", "hello"));
        Assert.assertEquals(5, StringUtilities.levenshteinDistance("hello", null));
    }

    /* ----------------------------------------------------------------------
     *  damerauLevenshteinDistance
     * ---------------------------------------------------------------------- */

    @Test
    public void testDamerauLevenshteinBasic() {
        Assert.assertEquals(2, StringUtilities.damerauLevenshteinDistance("book", "back"));
        Assert.assertEquals(0, StringUtilities.damerauLevenshteinDistance(null, null));
        Assert.assertEquals(4, StringUtilities.damerauLevenshteinDistance("", "test"));
        Assert.assertEquals(4, StringUtilities.damerauLevenshteinDistance("test", null));
    }

    /* ----------------------------------------------------------------------
     *  getRandomString / getRandomChar
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetRandomStringLengthAndCase() {
        Random rnd = new Random(12345L);
        String s = StringUtilities.getRandomString(rnd, 5, 10);
        Assert.assertTrue(s.length() >= 5 && s.length() <= 10);
        Assert.assertTrue(Character.isUpperCase(s.charAt(0)));
        // all other chars must be lowercase letters
        for (int i = 1; i < s.length(); i++) {
            Assert.assertTrue(Character.isLowerCase(s.charAt(i)));
        }
    }

    @Test
    public void testGetRandomCharUpperAndLower() {
        Random rnd = new Random(42L);
        String upper = StringUtilities.getRandomChar(rnd, true);
        String lower = StringUtilities.getRandomChar(rnd, false);
        Assert.assertEquals(1, upper.length());
        Assert.assertEquals(1, lower.length());
        Assert.assertTrue(Character.isUpperCase(upper.charAt(0)));
        Assert.assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    /* ----------------------------------------------------------------------
     *  getBytes / createString / UTF‑8 helpers
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetBytesAndCreateStringUtf8() throws UnsupportedEncodingException {
        String original = "café";
        byte[] utf8 = StringUtilities.getUTF8Bytes(original);
        String roundTrip = StringUtilities.createUTF8String(utf8);
        Assert.assertEquals(original, roundTrip);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBytesUnsupportedEncodingThrows() {
        // The method wraps UnsupportedEncodingException into IllegalArgumentException
        StringUtilities.getBytes("test", "UNSUPPORTED-ENCODING-XYZ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStringUnsupportedEncodingThrows() {
        StringUtilities.createString(new byte[]{0x00}, "UNSUPPORTED-ENCODING-XYZ");
    }

    @Test
    public void testCreateUtf8String() {
        byte[] bytes = { (byte)0x48, (byte)0x69 }; // "Hi"
        Assert.assertEquals("Hi", StringUtilities.createUtf8String(bytes));
    }

    /* ----------------------------------------------------------------------
     *  hashCodeIgnoreCase
     * ---------------------------------------------------------------------- */

    @Test
    public void testHashCodeIgnoreCase() {
        String a = "AbC";
        String b = "aBc";
        Assert.assertEquals(StringUtilities.hashCodeIgnoreCase(a),
                            StringUtilities.hashCodeIgnoreCase(b));
        Assert.assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    /* ----------------------------------------------------------------------
     *  trim / trimToEmpty / trimToNull / trimEmptyToDefault
     * ---------------------------------------------------------------------- */

    @Test
    public void testTrimMethods() {
        Assert.assertNull(StringUtilities.trim(null));
        Assert.assertEquals("abc", StringUtilities.trim("  abc  "));

        Assert.assertEquals("", StringUtilities.trimToEmpty(null));
        Assert.assertEquals("", StringUtilities.trimToEmpty("   "));

        Assert.assertNull(StringUtilities.trimToNull("   "));
        Assert.assertEquals("x", StringUtilities.trimToNull("  x  "));

        Assert.assertEquals("default", StringUtilities.trimEmptyToDefault(null, "default"));
        Assert.assertEquals("def", StringUtilities.trimEmptyToDefault("   ", "def"));
        Assert.assertEquals("value", StringUtilities.trimEmptyToDefault("  value ", "def"));
    }

    /* ----------------------------------------------------------------------
     *  removeLeadingAndTrailingQuotes
     * ---------------------------------------------------------------------- */

    @Test
    public void testRemoveLeadingAndTrailingQuotes() {
        Assert.assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"text\""));
        Assert.assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"text\"\""));
        Assert.assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"\"\"text\"\"\""));
        Assert.assertEquals("text with \"quotes\" inside", 
                StringUtilities.removeLeadingAndTrailingQuotes("\"text with \"quotes\" inside\""));
        Assert.assertNull(StringUtilities.removeLeadingAndTrailingQuotes(null));
        Assert.assertEquals("", StringUtilities.removeLeadingAndTrailingQuotes(""));
    }

    /* ----------------------------------------------------------------------
     *  commaSeparatedStringToSet
     * ---------------------------------------------------------------------- */

    @Test
    public void testCommaSeparatedStringToSetBasic() {
        Set<String> expected = new HashSet<>();
        expected.add("apple");
        expected.add("banana");
        expected.add("cherry");

        Set<String> actual = StringUtilities.commaSeparatedStringToSet("apple, banana, cherry, apple,  ");
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void testCommaSeparatedStringToSetEdgeCases() {
        Assert.assertEquals(Collections.emptySet(),
                StringUtilities.commaSeparatedStringToSet(null));

        Assert.assertEquals(Collections.emptySet(),
                StringUtilities.commaSeparatedStringToSet("   "));

        // All entries empty after trimming
        Assert.assertEquals(Collections.emptySet(),
                StringUtilities.commaSeparatedStringToSet(" , , , "));
    }

    /* ----------------------------------------------------------------------
     *  regionMatches (package‑private) – exercised indirectly via equalsIgnoreCase
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsIgnoreCaseUsesRegionMatchesCorrectly() {
        // regionMatches should handle out‑of‑range parameters gracefully (return false)
        Assert.assertFalse(StringUtilities.equalsIgnoreCase("a", "ab"));
        Assert.assertFalse(StringUtilities.equalsIgnoreCase("ab", "a"));
        // Normal case
        Assert.assertTrue(StringUtilities.equalsIgnoreCase("Test", "test"));
    }

}
