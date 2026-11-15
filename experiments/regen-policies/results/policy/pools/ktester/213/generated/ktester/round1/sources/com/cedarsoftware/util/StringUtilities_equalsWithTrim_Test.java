package com.cedarsoftware.util;

import static com.cedarsoftware.util.StringUtilities.equalsWithTrim;
// import org.junit.BeforeClass;
// import org.junit.runner.RunWith;
// import org.junit.runners.Parameterized;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import java.util.stream.Stream;
// import org.junit.AfterClass;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringUtilities_equalsWithTrim_Test {

    //     @RunWith(Parameterized.class)
    public static class TrimmedEqualPairs {

        private final String s1;

        private final String s2;

        public TrimmedEqualPairs(String s1, String s2) {
            this.s1 = s1;
            this.s2 = s2;
        }
        //         @Parameters(name = "\"{0}\" vs \"{1}\"")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][]{
        //                     {"abc", "abc"},
        //                     {" abc", "abc"},
        //                     {"abc ", "abc"},
        //                     {"  abc  ", "abc"},
        //                     {"\tabc\n", "abc"},
        //                     {"\n\t  abc  \r", "abc"},
        //                     {"   ", ""},
        //                     {"\t\n\r", ""},
        //                     {"", "   "},
        //                     {"", "\t"}
        //             });
        //         }
        //         @Test
        //         public void trimmedValuesAreEqual() {
        //             assertTrue(equalsWithTrim(s1, s2));
        //         }
    }

    //     @RunWith(Parameterized.class)
    public static class TrimmedNonEqualPairs {

        private final String s1;

        private final String s2;

        public TrimmedNonEqualPairs(String s1, String s2) {
            this.s1 = s1;
            this.s2 = s2;
        }
        //         @Parameters(name = "\"{0}\" vs \"{1}\"")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][]{
        //                     {"abc", "def"},
        //                     {" abc", "def"},
        //                     {"abc ", "def "},
        //                     {"  abc  ", "  def  "},
        //                     {"aBc", "abc "},
        //                     {"abc", "ab"},
        //                     {"", "a"},
        //                     {" ", "a"}
        //             });
        //         }
        //         @Test
        //         public void trimmedValuesAreNotEqual() {
        //             assertFalse(equalsWithTrim(s1, s2));
        //         }
    }

    //     @RunWith(Parameterized.class)
    public static class BasicEqualityPairs {

        private final String s1;

        private final String s2;

        private final boolean expected;

        public BasicEqualityPairs(String s1, String s2, boolean expected) {
            this.s1 = s1;
            this.s2 = s2;
            this.expected = expected;
        }
        //         @Parameters(name = "\"{0}\" vs \"{1}\" → {2}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][]{
        //                     {"apple", "apple", true},
        //                     {"apple", "orange", false}
        //             });
        //         }
        //         @Test
        //         public void basicEquality() {
        //             assertEquals(expected, equalsWithTrim(s1, s2));
        //         }
    }

    //     @RunWith(Parameterized.class)
    public static class WhitespaceVariationPairs {

        private final String s1;

        private final String s2;

        private final boolean expected;

        public WhitespaceVariationPairs(String s1, String s2, boolean expected) {
            this.s1 = s1;
            this.s2 = s2;
            this.expected = expected;
        }
        //         @Parameters(name = "\"{0}\" vs \"{1}\" → {2}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][]{
        //                     {"  hello  ", "hello", true},
        //                     {"\tworld\n", "world", true},
        //                     {"foo", "  foo  ", true},
        //                     {"  bar", "bar  ", true},
        //                     {"  baz", "qux", false}
        //             });
        //         }
        //         @Test
        //         public void whitespaceVariations() {
        //             assertEquals(expected, equalsWithTrim(s1, s2));
        //         }
    }

    //     @RunWith(Parameterized.class)
    public static class EmptyBlankPairs {

        private final String s1;

        private final String s2;

        private final boolean expected;

        public EmptyBlankPairs(String s1, String s2, boolean expected) {
            this.s1 = s1;
            this.s2 = s2;
            this.expected = expected;
        }
        //         @Parameters(name = "\"{0}\" vs \"{1}\" → {2}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][]{
        //                     {"", "", true},
        //                     {"   ", "", true},
        //                     {"\n\t", " ", true},
        //                     {"", "non-empty", false},
        //                     {"   ", "non-empty", false}
        //             });
        //         }
        //         @Test
        //         public void emptyAndBlank() {
        //             assertEquals(expected, equalsWithTrim(s1, s2));
        //         }
    }

    //     @RunWith(Parameterized.class)
    public static class UnicodeWhitespacePairs {

        private final String s1;

        private final String s2;

        private final boolean expected;

        public UnicodeWhitespacePairs(String s1, String s2, boolean expected) {
            this.s1 = s1;
            this.s2 = s2;
            this.expected = expected;
        }
        //         @Parameters(name = "\"{0}\" vs \"{1}\" → {2}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][]{
        //                     {"\u2002\u2003hello\u2004", "hello", false},
        //                     {"\u2002hello\u2002", "hello", false}
        //             });
        //         }
        //         @Test
        //         public void unicodeWhitespace() {
        //             assertEquals(expected, equalsWithTrim(s1, s2));
        //         }
    }

    //     @RunWith(Parameterized.class)
    public static class MixedCasePairs {

        private final String s1;

        private final String s2;

        private final boolean expected;

        public MixedCasePairs(String s1, String s2, boolean expected) {
            this.s1 = s1;
            this.s2 = s2;
            this.expected = expected;
        }
        //         @Parameters(name = "\"{0}\" vs \"{1}\" → {2}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][]{
        //                     {"Hello", "hello", false},
        //                     {"  Hello  ", "hello", false}
        //             });
        //         }
        //         @Test
        //         public void mixedCase() {
        //             assertEquals(expected, equalsWithTrim(s1, s2));
        //         }
    }

    private static Stream<Arguments> trimmedEqualPairs() {
        return Stream.of(Arguments.of("abc", "abc"), Arguments.of(" abc", "abc"), Arguments.of("abc ", "abc"), Arguments.of("  abc  ", "abc"), Arguments.of("\tabc\n", "abc"), Arguments.of("\n\t  abc  \r", "abc"), Arguments.of("   ", ""), Arguments.of("\t\n\r", ""), Arguments.of("", "   "), Arguments.of("", "\t"));
    }

    private static Stream<Arguments> trimmedNonEqualPairs() {
        return Stream.of(Arguments.of("abc", "def"), Arguments.of(" abc", "def"), Arguments.of("abc ", "def "), Arguments.of("  abc  ", "  def  "), Arguments.of("aBc", "abc "), Arguments.of("abc", "ab"), Arguments.of("", "a"), Arguments.of(" ", "a"));
    }

    private static Stream<Arguments> basicEqualityPairs() {
        return Stream.of(Arguments.of("apple", "apple", true), Arguments.of("apple", "orange", false));
    }

    private static Stream<Arguments> whitespaceVariationPairs() {
        return Stream.of(Arguments.of("  hello  ", "hello", true), Arguments.of("\tworld\n", "world", true), Arguments.of("foo", "  foo  ", true), Arguments.of("  bar", "bar  ", true), Arguments.of("  baz", "qux", false));
    }

    private static Stream<Arguments> emptyBlankPairs() {
        return Stream.of(Arguments.of("", "", true), Arguments.of("   ", "", true), Arguments.of("\n\t", " ", true), Arguments.of("", "non-empty", false), Arguments.of("   ", "non-empty", false));
    }

    private static Stream<Arguments> unicodeWhitespacePairs() {
        return Stream.of(Arguments.of("\u2002\u2003hello\u2004", "hello", false), Arguments.of("\u2002hello\u2002", "hello", false));
    }

    private static Stream<Arguments> mixedCasePairs() {
        return Stream.of(Arguments.of("Hello", "hello", false), Arguments.of("  Hello  ", "hello", false));
    }

    @Test
    void bothNull_returnsTrue() {
        assertTrue(equalsWithTrim(null, null));
    }

    @Test
    void oneNull_returnsFalse() {
        assertFalse(equalsWithTrim(null, "abc"));
        assertFalse(equalsWithTrim("abc", null));
    }

    @ParameterizedTest
    @MethodSource("trimmedEqualPairs")
    void trimmedValuesAreEqual(String s1, String s2) {
        assertTrue(equalsWithTrim(s1, s2), () -> "Expected true for trimmed equality of [" + s1 + "] and [" + s2 + "]");
    }

    @ParameterizedTest
    @MethodSource("trimmedNonEqualPairs")
    void trimmedValuesAreNotEqual(String s1, String s2) {
        assertFalse(equalsWithTrim(s1, s2), () -> "Expected false for trimmed inequality of [" + s1 + "] and [" + s2 + "]");
    }

    @ParameterizedTest
    @MethodSource("basicEqualityPairs")
    void basicEquality(String s1, String s2, boolean expected) {
        if (expected) {
            assertTrue(equalsWithTrim(s1, s2));
        } else {
            assertFalse(equalsWithTrim(s1, s2));
        }
    }

    @ParameterizedTest
    @MethodSource("whitespaceVariationPairs")
    void whitespaceVariations(String s1, String s2, boolean expected) {
        if (expected) {
            assertTrue(equalsWithTrim(s1, s2));
        } else {
            assertFalse(equalsWithTrim(s1, s2));
        }
    }

    @ParameterizedTest
    @MethodSource("emptyBlankPairs")
    void emptyAndBlank(String s1, String s2, boolean expected) {
        if (expected) {
            assertTrue(equalsWithTrim(s1, s2));
        } else {
            assertFalse(equalsWithTrim(s1, s2));
        }
    }

    @ParameterizedTest
    @MethodSource("unicodeWhitespacePairs")
    void unicodeWhitespace(String s1, String s2, boolean expected) {
        if (expected) {
            assertTrue(equalsWithTrim(s1, s2));
        } else {
            assertFalse(equalsWithTrim(s1, s2));
        }
    }

    @ParameterizedTest
    @MethodSource("mixedCasePairs")
    void mixedCase(String s1, String s2, boolean expected) {
        if (expected) {
            assertTrue(equalsWithTrim(s1, s2));
        } else {
            assertFalse(equalsWithTrim(s1, s2));
        }
    }

    @Test
    void unicodeCharacters() {
        // EM SPACE before/after
        String s1 = "\u2003\u2003こんにちは\u2003";
        String s2 = "こんにちは";
        // The EM SPACE (U+2003) is > 32, so it is NOT trimmed by equalsWithTrim.
        // Therefore the strings are NOT considered equal.
        assertFalse(equalsWithTrim(s1, s2));
    }

    @Test
    void surrogatePairs() {
        // Emoji (U+1F600) represented by a surrogate pair, surrounded by spaces.
        String s1 = " \uD83D\uDE00 ";
        String s2 = "\uD83D\uDE00";
        assertTrue(equalsWithTrim(s1, s2));
    }
}
