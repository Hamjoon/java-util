package com.cedarsoftware.util;

// import org.junit.*;
import java.util.*;
import java.util.stream.Collectors;

class StringUtilities_equalsIgnoreCase_Test {

    /* -------------------------------------------------------------
       Helper class to simulate CharSequence implementations that throw
       on length() or charAt().
       ------------------------------------------------------------- */
    static class ExplodingCharSequence implements CharSequence {

        private final boolean explodeOnLength;

        private final boolean explodeOnCharAt;

        private final String backing;

        ExplodingCharSequence(boolean explodeOnLength, boolean explodeOnCharAt) {
            this(explodeOnLength, explodeOnCharAt, "dummy");
        }

        ExplodingCharSequence(boolean explodeOnLength, boolean explodeOnCharAt, String backing) {
            this.explodeOnLength = explodeOnLength;
            this.explodeOnCharAt = explodeOnCharAt;
            this.backing = backing;
        }

        @Override
        public int length() {
            if (explodeOnLength) {
                throw new RuntimeException("length()");
            }
            return backing.length();
        }

        @Override
        public char charAt(int index) {
            if (explodeOnCharAt) {
                throw new RuntimeException("charAt()");
            }
            return backing.charAt(index);
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            return backing.subSequence(start, end);
        }

        @Override
        public String toString() {
            return backing;
        }
    }

//     private static Stream<CharSequence> nonNullCharSequences() {
//         return Arrays.asList("abc", new StringBuilder("abc"), new StringBuffer("abc"));
//     }

//     private static Stream<Arguments> unicodeCases() {
//         return Arrays.asList(// Capital I with dot vs i + combining dot (different length after case folding)
//         new Object[] { "İstanbul", "i\u0307stanbul", false }, // Fraktur capital U vs lowercase U (Unicode case conversion works)
//         new Object[] { "\uD835\uDD18\uD835\uDD1B\uD835\uDD1E\uD835\uDD20\uD835\uDD22\uD835\uDD24\uD835\uDD26", "\uD835\uDD38\uD835\uDD3B\uD835\uDD3E\uD835\uDD40\uD835\uDD42\uD835\uDD44\uD835\uDD46", true }, // Greek sigma vs standard sigma
//         new Object[] { "\u03A3", "\u03C3", true }, // Greek sigma vs final sigma (different lowercase forms)
//         new Object[] { "\u03A3", "\u03C2", false });
//     }

    /* -------------------------------------------------------------
       1. Same reference (including both null) → true
       ------------------------------------------------------------- */
//     @Test
//     void testEqualsIgnoreCase_sameReference_returnsTrue() {
//         CharSequence cs = "SomeText";
//         Assert.assertTrue(StringUtilities.equalsIgnoreCase(cs, cs));
//         Assert.assertTrue(StringUtilities.equalsIgnoreCase(null, null));
//     }

    /* -------------------------------------------------------------
       2. One argument null, the other non‑null → false
       ------------------------------------------------------------- */
//     @ParameterizedTest
//     @MethodSource("nonNullCharSequences")
//     void testEqualsIgnoreCase_oneNull_returnsFalse(CharSequence nonNull) {
//         for (CharSequence nonNull : nonNullCharSequences()) {
//             Assert.assertFalse(StringUtilities.equalsIgnoreCase(nonNull, null));
//             Assert.assertFalse(StringUtilities.equalsIgnoreCase(null, nonNull));
//         }
//     }

    /* -------------------------------------------------------------
       3. Different lengths → false
       ------------------------------------------------------------- */
//     @Test
//     void testEqualsIgnoreCase_differentLengths_returnsFalse() {
//         CharSequence cs1 = "short";
//         CharSequence cs2 = "longer";
//         Assert.assertFalse(StringUtilities.equalsIgnoreCase(cs1, cs2));
//     }

    /* -------------------------------------------------------------
       4. Same characters, different case → true
       ------------------------------------------------------------- */
//     @ParameterizedTest
//     @CsvSource({ "'Hello', 'hello'", "'WORLD', 'world'", "'Java', 'jAvA'", "'MiXeD CaSe', 'mixed case'" })
//     void testEqualsIgnoreCase_caseInsensitiveMatch_returnsTrue(String s1, String s2) {
//         String[][] data = { { "Hello", "hello" }, { "WORLD", "world" }, { "Java", "jAvA" }, { "MiXeD CaSe", "mixed case" } };
//         for (String[] pair : data) {
//             Assert.assertTrue(StringUtilities.equalsIgnoreCase(pair[0], pair[1]));
//         }
//     }

    /* -------------------------------------------------------------
       5. Same length but different characters → false
       ------------------------------------------------------------- */
//     @ParameterizedTest
//     @CsvSource({ "'abc', 'abd'", "'test', 'tost'", "'1234', '1235'", "'Java', 'Jaba'" })
//     void testEqualsIgnoreCase_differentContentSameLength_returnsFalse(String s1, String s2) {
//         String[][] data = { { "abc", "abd" }, { "test", "tost" }, { "1234", "1235" }, { "Java", "Jaba" } };
//         for (String[] pair : data) {
//             Assert.assertFalse(StringUtilities.equalsIgnoreCase(pair[0], pair[1]));
//         }
//     }

    /* -------------------------------------------------------------
       6. Verify the String overload forwards correctly.
       ------------------------------------------------------------- */
//     @Test
//     void testEqualsIgnoreCase_stringOverload_behavesIdentically() {
//         String s1 = "CaseInsensitive";
//         String s2 = "caseinsensitive";
//         // Direct CharSequence call
//         boolean viaCharSeq = StringUtilities.equalsIgnoreCase((CharSequence) s1, (CharSequence) s2);
//         // String overload
//         boolean viaString = StringUtilities.equalsIgnoreCase(s1, s2);
//         Assert.assertEquals(viaCharSeq, viaString);
//         Assert.assertTrue(viaString);
//     }

    /* -------------------------------------------------------------
       7. Use other CharSequence implementations (StringBuilder,
          StringBuffer) to ensure method works with any CharSequence.
       ------------------------------------------------------------- */
//     @Test
//     void testEqualsIgnoreCase_variousCharSequenceImplementations() {
//         CharSequence cs1 = new StringBuilder("BuilderTest");
//         CharSequence cs2 = new StringBuffer("buildertest");
//         Assert.assertTrue(StringUtilities.equalsIgnoreCase(cs1, cs2));
//     }

    /* -------------------------------------------------------------
       8. Unicode case‑mapping edge cases.
       ------------------------------------------------------------- */
//     @ParameterizedTest
//     @MethodSource("unicodeCases")
//     void testEqualsIgnoreCase_unicodeEdgeCases(CharSequence cs1, CharSequence cs2, boolean expected) {
//         for (Object[] row : unicodeCases()) {
//             CharSequence cs1 = (CharSequence) row[0];
//             CharSequence cs2 = (CharSequence) row[1];
//             boolean expected = (Boolean) row[2];
//             Assert.assertEquals(expected, StringUtilities.equalsIgnoreCase(cs1, cs2));
//         }
//     }

    /* -------------------------------------------------------------
       9. Large repeated strings – ensure no overflow / performance issue.
       ------------------------------------------------------------- */
//     @Test
//     void testEqualsIgnoreCase_largeRepeatedStrings() {
//         String part1 = "aBc".repeat(1000);
//         String part2 = "Abc".repeat(1000);
//         Assert.assertTrue(StringUtilities.equalsIgnoreCase(part1, part2));
//     }

    /* -------------------------------------------------------------
       10. Exception propagation from length()
       ------------------------------------------------------------- */
//     @Test
//     void testEqualsIgnoreCase_lengthExceptionPropagated_cs1() {
//         CharSequence exploding = new ExplodingCharSequence(true, false);
//         CharSequence normal = "normal";
//         RuntimeException ex = Assert.assertThrows(RuntimeException.class, () -> StringUtilities.equalsIgnoreCase(exploding, normal));
//         Assert.assertEquals("length()", ex.getMessage());
//     }

//     @Test
//     void testEqualsIgnoreCase_lengthExceptionPropagated_cs2() {
//         CharSequence exploding = new ExplodingCharSequence(true, false);
//         CharSequence normal = "normal";
//         RuntimeException ex = Assert.assertThrows(RuntimeException.class, () -> StringUtilities.equalsIgnoreCase(normal, exploding));
//         Assert.assertEquals("length()", ex.getMessage());
//     }

    /* -------------------------------------------------------------
       11. Exception propagation from charAt() during regionMatches
       ------------------------------------------------------------- */
//     @Test
//     void testEqualsIgnoreCase_charAtException_cs1() {
//         CharSequence exploding = new ExplodingCharSequence(false, true, "AbC");
//         CharSequence normal = "abc";
//         RuntimeException ex = Assert.assertThrows(RuntimeException.class, () -> StringUtilities.equalsIgnoreCase(exploding, normal));
//         Assert.assertEquals("charAt()", ex.getMessage());
//     }

//     @Test
//     void testEqualsIgnoreCase_charAtException_cs2() {
//         CharSequence exploding = new ExplodingCharSequence(false, true, "AbC");
//         CharSequence normal = "abc";
//         RuntimeException ex = Assert.assertThrows(RuntimeException.class, () -> StringUtilities.equalsIgnoreCase(normal, exploding));
//         Assert.assertEquals("charAt()", ex.getMessage());
//     }

    /* -------------------------------------------------------------
       12. Reference equality short‑circuit avoids invoking methods.
       ------------------------------------------------------------- */
//     @Test
//     void testEqualsIgnoreCase_referenceEqualityShortCircuit() {
//         ExplodingCharSequence exploding = new ExplodingCharSequence(true, true, "same");
//         // cs2 references the same instance – should return true without any exception.
//         Assert.assertTrue(StringUtilities.equalsIgnoreCase(exploding, exploding));
//     }
}