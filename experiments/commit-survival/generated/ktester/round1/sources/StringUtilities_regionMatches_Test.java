package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_regionMatches_Test {

    /**
     * Supplies all test cases described in the JSON document.
     *
     * Each {@link Arguments} entry contains:
     *   cs, ignoreCase, thisStart, substring, start, length, expectedResult, description
     */
    private static Stream<Arguments> regionMatchesProvider() {
        return Stream.of(// 1. cs null – expect exception
        Arguments.of(null, false, 0, "test", 0, 4, "throws IllegalArgumentException", "cs argument is null – the method should fail fast via Convention.throwIfNull."), // 2. substring null – expect exception
        Arguments.of("test", false, 0, null, 0, 4, "throws IllegalArgumentException", "substring argument is null – the method should throw an exception."), // 3. Exact match, ignoreCase false
        Arguments.of("HelloWorld", false, 0, "HelloWorld", 0, 10, true, "Both arguments are String, exact match, ignoreCase false – should delegate to String.regionMatches and return true."), // 4. First 5 chars match, ignoreCase false
        Arguments.of("HelloWorld", false, 0, "HelloJava", 0, 5, true, "Both arguments are String, first 5 characters match, ignoreCase false – should return true."), // 5. Mismatch at position 5, ignoreCase false
        Arguments.of("HelloWorld", false, 0, "HelloJava", 0, 6, false, "Both arguments are String, mismatch at position 5, ignoreCase false – should return false."), // 6. Case‑insensitive full match
        Arguments.of("HelloWorld", true, 0, "helloworld", 0, 10, true, "Both arguments are String, case‑insensitive match, ignoreCase true – should return true."), // 7. Non‑String CharSequence exact match
        Arguments.of("abcde", false, 1, "bcd", 0, 3, true, "Non‑String CharSequence (StringBuilder) matches exactly – returns true."), // 8. Non‑String CharSequence case‑insensitive match
        Arguments.of("abCde", true, 2, "cde", 0, 3, true, "Non‑String CharSequence with case‑insensitive match – should succeed."), // 9. Case‑insensitive mismatch after conversion
        Arguments.of("abCde", true, 2, "cdf", 0, 3, false, "Case‑insensitive comparison but characters differ after upper/lower conversion – should return false."), // 10. Negative thisStart
        Arguments.of("abc", false, -1, "a", 0, 1, false, "Invalid negative thisStart – method should detect invalid parameters and return false."), // 11. Negative start
        Arguments.of("abc", false, 0, "a", -2, 1, false, "Invalid negative start – should return false."), // 12. Negative length
        Arguments.of("abc", false, 0, "a", 0, -1, false, "Negative length – should return false."), // 13. Length exceeds cs region
        Arguments.of("ab", false, 0, "abc", 0, 3, false, "Requested length exceeds cs region – should return false."), // 14. Length exceeds substring region
        Arguments.of("abc", false, 0, "ab", 0, 3, false, "Requested length exceeds substring region – should return false."), // 15. Zero length with thisStart beyond cs length
        Arguments.of("abc", false, 5, "xyz", 0, 0, false, "Zero length but thisStart beyond cs length – srcLen becomes negative, length check fails, returns false."), // 16. Zero length with valid starts – should be true
        Arguments.of("abc", false, 2, "xyz", 1, 0, true, "Zero length with valid start positions – should return true (no characters to compare)."), // 17. Mixed CharSequence types, ignoreCase false, case differs
        Arguments.of("HelloWorld", false, 0, "helloworld", 0, 10, false, "Mixed CharSequence types with ignoreCase=false; characters differ by case, so the method must return false."), // 18. Zero‑length at exact ends – true
        Arguments.of("abc", false, 3, "xyz", 3, 0, true, "Zero‑length comparison where both start indices are exactly at the end of their respective CharSequences; no characters are compared, so the result should be true."), // 19. Zero‑length but negative start for substring – false
        Arguments.of("abc", false, 1, "xyz", -1, 0, false, "Zero‑length comparison but the start index for the substring is negative; the method should detect the invalid parameter and return false."), // 20. Zero‑length with negative thisStart – false
        Arguments.of("abc", false, -1, "xyz", 0, 0, false, "Zero‑length comparison with a negative thisStart; the method should return false because the start index is invalid."), // 21. Exact match where length fits exactly
        Arguments.of("abcdef", false, 2, "cdefgh", 0, 4, true, "Exact match where the requested length exactly fits the remaining region of both CharSequences (cs[2..5] == substring[0..3])."), // 22. Full‑length case‑sensitive mismatch
        Arguments.of("abcdEF", false, 0, "ABCDef", 0, 6, false, "Full‑length comparison where characters differ only by case and ignoreCase is false; the method must return false."), // 23. Zero‑length with start inside cs but at end of substring – true
        Arguments.of("abc", false, 1, "def", 3, 0, true, "Zero‑length comparison where start equals the substring length (end of substring) while thisStart is within cs; should return true."), // 24. Both empty and length zero – true
        Arguments.of("", false, 0, "", 0, 0, true, "Both CharSequences are empty and length is zero; the method should return true."));
    }

    @Test
    void regionMatches_nullCs_throwsException() {
        assertThrows(RuntimeException.class, () -> StringUtilities.regionMatches(null, false, 0, "abc", 0, 3));
    }

    @Test
    void regionMatches_nullSubstring_throwsException() {
        assertThrows(RuntimeException.class, () -> StringUtilities.regionMatches("abc", false, 0, null, 0, 3));
    }

    @Test
    void regionMatches_bothString_delegatesToStringRegionMatches() {
        String cs = "HelloWorld";
        String sub = "World";
        boolean expected = cs.regionMatches(false, 5, sub, 0, sub.length());
        boolean actual = StringUtilities.regionMatches(cs, false, 5, sub, 0, sub.length());
        assertEquals(expected, actual);
    }

    @Test
    void regionMatches_bothString_ignoreCaseTrue() {
        String cs = "AbCdEfGh";
        String sub = "cde";
        boolean expected = cs.regionMatches(true, 2, sub, 0, sub.length());
        boolean actual = StringUtilities.regionMatches(cs, true, 2, sub, 0, sub.length());
        assertEquals(expected, actual);
    }

    @Test
    void regionMatches_nonStringCharSequences_match() {
        CharSequence cs = new StringBuilder("OpenAIChatGPT");
        CharSequence sub = new StringBuilder("AI");
        assertTrue(StringUtilities.regionMatches(cs, false, 4, sub, 0, 2));
    }

    @Test
    void regionMatches_nonStringCharSequences_ignoreCaseMatch() {
        CharSequence cs = new StringBuilder("OpenAIChatGPT");
        CharSequence sub = new StringBuilder("ai");
        assertTrue(StringUtilities.regionMatches(cs, true, 4, sub, 0, 2));
    }

    @Test
    void regionMatches_nonStringCharSequences_ignoreCaseMismatch() {
        CharSequence cs = new StringBuilder("OpenAIChatGPT");
        CharSequence sub = new StringBuilder("ai");
        assertFalse(StringUtilities.regionMatches(cs, false, 4, sub, 0, 2));
    }

    @Test
    void regionMatches_negativeStart_returnsFalse() {
        assertFalse(StringUtilities.regionMatches("abc", false, -1, "abc", 0, 3));
        assertFalse(StringUtilities.regionMatches("abc", false, 0, "abc", -1, 3));
    }

    @Test
    void regionMatches_negativeLength_returnsFalse() {
        assertFalse(StringUtilities.regionMatches("abc", false, 0, "abc", 0, -5));
    }

    @Test
    void regionMatches_lengthExceedsSource_returnsFalse() {
        // source region shorter than requested length
        assertFalse(StringUtilities.regionMatches("abc", false, 1, "abc", 0, 3));
    }

    @Test
    void regionMatches_lengthExceedsSubstring_returnsFalse() {
        // substring region shorter than requested length
        assertFalse(StringUtilities.regionMatches("abc", false, 0, "abc", 2, 2));
    }

    @Test
    void regionMatches_caseConversionMatchesStringImplementation() {
        // Characters that differ only by case and by Unicode case‑folding
        // German sharp s
        String cs = "ß";
        // Upper‑case representation
        String sub = "SS";
        // String.regionMatches with ignoreCase = true returns false for this pair
        assertFalse("ß".regionMatches(true, 0, "SS", 0, 1));
        // Our implementation should behave the same
        assertFalse(StringUtilities.regionMatches(cs, true, 0, sub, 0, 1));
    }

    @ParameterizedTest(name = "[{index}] {6}")
    @MethodSource("regionMatchesProvider")
    void regionMatches_parameterised(Object csObj, boolean ignoreCase, int thisStart, Object subObj, int start, int length, Object expected, String description) {
        CharSequence cs = (csObj == null) ? null : csObj.toString();
        CharSequence sub = (subObj == null) ? null : subObj.toString();
        if ("throws IllegalArgumentException".equals(expected)) {
            // The production code throws a RuntimeException (via Convention.throwIfNull)
            assertThrows(RuntimeException.class, () -> StringUtilities.regionMatches(cs, ignoreCase, thisStart, sub, start, length), description);
        } else {
            boolean expectedBool = (Boolean) expected;
            boolean actual = StringUtilities.regionMatches(cs, ignoreCase, thisStart, sub, start, length);
            assertEquals(expectedBool, actual, description);
        }
    }
}
