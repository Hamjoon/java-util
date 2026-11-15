package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.io.UnsupportedEncodingException;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

public class StringUtilitiesTest {

    /* ---------- equals & equalsIgnoreCase ---------- */

    @Test
    public void testEqualsBothNull() {
        Assert.assertTrue(StringUtilities.equals((String) null, null));
    }

    @Test
    public void testEqualsOneNull() {
        Assert.assertFalse(StringUtilities.equals("a", null));
        Assert.assertFalse(StringUtilities.equals(null, "b"));
    }

    @Test
    public void testEqualsSameString() {
        Assert.assertTrue(StringUtilities.equals("hello", "hello"));
    }

    @Test
    public void testEqualsDifferentString() {
        Assert.assertFalse(StringUtilities.equals("hello", "world"));
    }

    @Test
    public void testEqualsCharSequenceImplementation() {
        StringBuilder sb1 = new StringBuilder("test");
        StringBuilder sb2 = new StringBuilder("test");
        Assert.assertTrue(StringUtilities.equals(sb1, sb2));
        sb2.append("X");
        Assert.assertFalse(StringUtilities.equals(sb1, sb2));
    }

    @Test
    public void testEqualsIgnoreCase() {
        Assert.assertTrue(StringUtilities.equalsIgnoreCase("AbC", "aBc"));
        Assert.assertFalse(StringUtilities.equalsIgnoreCase("AbC", "ab"));
        Assert.assertTrue(StringUtilities.equalsIgnoreCase(null, null));
        Assert.assertFalse(StringUtilities.equalsIgnoreCase(null, "a"));
    }

    @Test
    public void testEqualsIgnoreCaseWithStringBuilder() {
        StringBuilder sb1 = new StringBuilder("Case");
        StringBuilder sb2 = new StringBuilder("cASe");
        Assert.assertTrue(StringUtilities.equalsIgnoreCase(sb1, sb2));
    }

    /* ---------- equalsWithTrim & equalsIgnoreCaseWithTrim ---------- */

    @Test
    public void testEqualsWithTrim() {
        Assert.assertTrue(StringUtilities.equalsWithTrim("  foo ", "foo"));
        Assert.assertFalse(StringUtilities.equalsWithTrim(" foo", "bar"));
        Assert.assertTrue(StringUtilities.equalsWithTrim(null, null));
        Assert.assertFalse(StringUtilities.equalsWithTrim(null, " "));
    }

    @Test
    public void testEqualsIgnoreCaseWithTrim() {
        Assert.assertTrue(StringUtilities.equalsIgnoreCaseWithTrim("  Foo ", "foo"));
        Assert.assertFalse(StringUtilities.equalsIgnoreCaseWithTrim(" Foo", "Bar"));
    }

    /* ---------- isEmpty / isWhitespace / hasContent ---------- */

    @Test
    public void testIsEmpty() {
        Assert.assertTrue(StringUtilities.isEmpty((String) null));
        Assert.assertTrue(StringUtilities.isEmpty(""));
        Assert.assertTrue(StringUtilities.isEmpty("   "));
        Assert.assertFalse(StringUtilities.isEmpty(" x "));
    }

    @Test
    public void testIsWhitespace() {
        Assert.assertTrue(StringUtilities.isWhitespace(null));
        Assert.assertTrue(StringUtilities.isWhitespace(""));
        Assert.assertTrue(StringUtilities.isWhitespace("\t\n "));
        Assert.assertFalse(StringUtilities.isWhitespace("a"));
    }

    @Test
    public void testHasContent() {
        Assert.assertFalse(StringUtilities.hasContent(null));
        Assert.assertFalse(StringUtilities.hasContent("   "));
        Assert.assertTrue(StringUtilities.hasContent("  x "));
    }

    /* ---------- length & trimLength ---------- */

    @Test
    public void testLength() {
        Assert.assertEquals(0, StringUtilities.length((String) null));
        Assert.assertEquals(5, StringUtilities.length("hello"));
    }

    @Test
    public void testTrimLength() {
        Assert.assertEquals(0, StringUtilities.trimLength(null));
        Assert.assertEquals(0, StringUtilities.trimLength("   "));
        Assert.assertEquals(3, StringUtilities.trimLength("  abc  "));
    }

    /* ---------- lastIndexOf ---------- */

    @Test
    public void testLastIndexOf() {
        Assert.assertEquals(-1, StringUtilities.lastIndexOf(null, '/'));
        Assert.assertEquals(4, StringUtilities.lastIndexOf("a/b/c/d", '/'));
        Assert.assertEquals(0, StringUtilities.lastIndexOf("/", '/'));
    }

    /* ---------- decode & encode ---------- */

    @Test
    public void testEncodeDecodeRoundTrip() {
        byte[] original = new byte[]{0x00, 0x0F, (byte) 0xA5, (byte) 0xFF};
        String encoded = StringUtilities.encode(original);
        byte[] decoded = StringUtilities.decode(encoded);
        Assert.assertArrayEquals(original, decoded);
    }

    @Test
    public void testDecodeOddLengthReturnsNull() {
        Assert.assertNull(StringUtilities.decode("ABC"));
    }

    @Test
    public void testDecodeValidHex() {
        byte[] expected = new byte[]{0x0A, 0x1B, (byte) 0xFF};
        byte[] actual = StringUtilities.decode("0A1BFF");
        Assert.assertArrayEquals(expected, actual);
    }

    /* ---------- count ---------- */

    @Test
    public void testCountChar() {
        Assert.assertEquals(3, StringUtilities.count("banana", 'a'));
        Assert.assertEquals(0, StringUtilities.count("banana", 'z'));
    }

    @Test
    public void testCountToken() {
        Assert.assertEquals(2, StringUtilities.count("ababa", "aba"));
        // overlapping occurrences: "aaa" contains "aa" twice
        Assert.assertEquals(2, StringUtilities.count("aaa", "aa"));
        Assert.assertEquals(0, StringUtilities.count(null, "a"));
        Assert.assertEquals(0, StringUtilities.count("abc", null));
        Assert.assertEquals(0, StringUtilities.count("", "a"));
        Assert.assertEquals(0, StringUtilities.count("abc", ""));
    }

    /* ---------- wildcardToRegexString ---------- */

    @Test
    public void testWildcardToRegexString() {
        String wildcard = "file-?.*";
        String regex = StringUtilities.wildcardToRegexString(wildcard);
        Pattern p = Pattern.compile(regex);
        Assert.assertTrue(p.matcher("file-a.txt").matches());
        Assert.assertFalse(p.matcher("file-abc.txt").matches());
    }

    @Test
    public void testWildcardEscapesSpecialChars() {
        String wildcard = "a(b)*c?d[ef]";
        String regex = StringUtilities.wildcardToRegexString(wildcard);
        Pattern p = Pattern.compile(regex);
        Assert.assertTrue(p.matcher("a(b)xyzcdXdef").matches());
    }

    /* ---------- levenshteinDistance ---------- */

    @Test
    public void testLevenshteinDistanceBasic() {
        Assert.assertEquals(3, StringUtilities.levenshteinDistance("kitten", "sitting"));
        Assert.assertEquals(0, StringUtilities.levenshteinDistance("", ""));
        Assert.assertEquals(5, StringUtilities.levenshteinDistance("", "hello"));
        Assert.assertEquals(5, StringUtilities.levenshteinDistance("hello", null));
    }

    @Test
    public void testLevenshteinDistanceNullAndEmpty() {
        Assert.assertEquals(0, StringUtilities.levenshteinDistance(null, null));
        Assert.assertEquals(0, StringUtilities.levenshteinDistance("", ""));
    }

    /* ---------- damerauLevenshteinDistance ---------- */

    @Test
    public void testDamerauLevenshteinDistanceTransposition() {
        // "ca" -> "ac" is a transposition, distance 1
        Assert.assertEquals(1, StringUtilities.damerauLevenshteinDistance("ca", "ac"));
        // No transposition
        Assert.assertEquals(2, StringUtilities.damerauLevenshteinDistance("abc", "cba"));
    }

    @Test
    public void testDamerauLevenshteinDistanceBasic() {
        Assert.assertEquals(3, StringUtilities.damerauLevenshteinDistance("kitten", "sitting"));
        Assert.assertEquals(0, StringUtilities.damerauLevenshteinDistance("", ""));
        Assert.assertEquals(4, StringUtilities.damerauLevenshteinDistance(null, "test"));
        Assert.assertEquals(4, StringUtilities.damerauLevenshteinDistance("test", null));
    }

    /* ---------- getRandomString & getRandomChar ---------- */

    @Test
    public void testGetRandomStringDeterministic() {
        Random rnd = new Random(12345L);
        String s = StringUtilities.getRandomString(rnd, 5, 5);
        // Length must be exactly 5 and first character uppercase
        Assert.assertEquals(5, s.length());
        Assert.assertTrue(Character.isUpperCase(s.charAt(0)));
        // Subsequent characters should be lowercase
        for (int i = 1; i < s.length(); i++) {
            Assert.assertTrue(Character.isLowerCase(s.charAt(i)));
        }
    }

    @Test
    public void testGetRandomCharUpperAndLower() {
        Random rnd = new Random(1L);
        String upper = StringUtilities.getRandomChar(rnd, true);
        String lower = StringUtilities.getRandomChar(rnd, false);
        Assert.assertEquals(1, upper.length());
        Assert.assertEquals(1, lower.length());
        Assert.assertTrue(Character.isUpperCase(upper.charAt(0)));
        Assert.assertTrue(Character.isLowerCase(lower.charAt(0)));
    }

    /* ---------- getBytes / createString / UTF-8 helpers ---------- */

    @Test
    public void testGetBytesAndCreateStringUtf8() throws UnsupportedEncodingException {
        String original = "café";
        byte[] utf8 = StringUtilities.getBytes(original, "UTF-8");
        String roundTrip = StringUtilities.createString(utf8, "UTF-8");
        Assert.assertEquals(original, roundTrip);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetBytesUnsupportedEncoding() {
        StringUtilities.getBytes("test", "UNSUPPORTED-ENCODING");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateStringUnsupportedEncoding() {
        StringUtilities.createString(new byte[]{0x41}, "UNSUPPORTED-ENCODING");
    }

    @Test
    public void testCreateUtf8StringAndGetUTF8Bytes() {
        String text = "Hello 🌍";
        byte[] bytes = StringUtilities.getUTF8Bytes(text);
        String restored = StringUtilities.createUtf8String(bytes);
        Assert.assertEquals(text, restored);
    }

    @Test
    public void testCreateUTF8StringAlias() {
        String text = "binary";
        byte[] bytes = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Assert.assertEquals(text, StringUtilities.createUTF8String(bytes));
    }

    /* ---------- hashCodeIgnoreCase ---------- */

    @Test
    public void testHashCodeIgnoreCaseConsistency() {
        String s1 = "AbCdeF";
        String s2 = "aBcDeF";
        int h1 = StringUtilities.hashCodeIgnoreCase(s1);
        int h2 = StringUtilities.hashCodeIgnoreCase(s2);
        Assert.assertEquals(h1, h2);
    }

    @Test
    public void testHashCodeIgnoreCaseNull() {
        Assert.assertEquals(0, StringUtilities.hashCodeIgnoreCase(null));
    }

    /* ---------- trim family ---------- */

    @Test
    public void testTrim() {
        Assert.assertNull(StringUtilities.trim(null));
        Assert.assertEquals("abc", StringUtilities.trim("  abc  "));
    }

    @Test
    public void testTrimToEmpty() {
        Assert.assertEquals("", StringUtilities.trimToEmpty(null));
        Assert.assertEquals("", StringUtilities.trimToEmpty("   "));
        Assert.assertEquals("xyz", StringUtilities.trimToEmpty("  xyz "));
    }

    @Test
    public void testTrimToNull() {
        Assert.assertNull(StringUtilities.trimToNull(null));
        Assert.assertNull(StringUtilities.trimToNull("   "));
        Assert.assertEquals("val", StringUtilities.trimToNull("  val "));
    }

    @Test
    public void testTrimEmptyToDefault() {
        Assert.assertEquals("def", StringUtilities.trimEmptyToDefault(null, "def"));
        Assert.assertEquals("def", StringUtilities.trimEmptyToDefault("   ", "def"));
        Assert.assertEquals("ok", StringUtilities.trimEmptyToDefault("  ok  ", "def"));
    }

    /* ---------- removeLeadingAndTrailingQuotes ---------- */

    @Test
    public void testRemoveLeadingAndTrailingQuotesSimple() {
        Assert.assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("\"text\""));
        Assert.assertEquals("text", StringUtilities.removeLeadingAndTrailingQuotes("""" + "text" + """" ));
        Assert.assertEquals("text\"inside", StringUtilities.removeLeadingAndTrailingQuotes("\"text\"inside\""));
        Assert.assertEquals("", StringUtilities.removeLeadingAndTrailingQuotes("\"\""));
        Assert.assertNull(StringUtilities.removeLeadingAndTrailingQuotes(null));
    }

    @Test
    public void testRemoveLeadingAndTrailingQuotesNoQuotes() {
        Assert.assertEquals("noquotes", StringUtilities.removeLeadingAndTrailingQuotes("noquotes"));
    }

    /* ---------- commaSeparatedStringToSet ---------- */

    @Test
    public void testCommaSeparatedStringToSetBasic() {
        Set<String> set = StringUtilities.commaSeparatedStringToSet("a, b ,c, a,  ");
        Assert.assertEquals(3, set.size());
        Assert.assertTrue(set.contains("a"));
        Assert.assertTrue(set.contains("b"));
        Assert.assertTrue(set.contains("c"));
    }

    @Test
    public void testCommaSeparatedStringToSetEmptyAndNull() {
        Assert.assertTrue(StringUtilities.commaSeparatedStringToSet(null).isEmpty());
        Assert.assertTrue(StringUtilities.commaSeparatedStringToSet("").isEmpty());
        Assert.assertTrue(StringUtilities.commaSeparatedStringToSet("   ").isEmpty());
    }

    @Test
    public void testCommaSeparatedStringToSetTrimsElements() {
        Set<String> set = StringUtilities.commaSeparatedStringToSet("  foo  ,  bar");
        Assert.assertTrue(set.contains("foo"));
        Assert.assertTrue(set.contains("bar"));
    }
}
