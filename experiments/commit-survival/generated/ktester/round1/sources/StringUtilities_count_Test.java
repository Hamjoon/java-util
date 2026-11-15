package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringUtilities_count_Test {

    private static Stream<Arguments> consecutiveOccurrencesProvider() {
        return Stream.of(Arguments.of("aaabaaa", 'a', 6), Arguments.of("bbb", 'b', 3));
    }

    private static Stream<Arguments> unicodeCharactersProvider() {
        return Stream.of(Arguments.of("café naïve résumé", 'é', 2), Arguments.of("汉字漢字", '字', 2), Arguments.of("ΩΩΩ", 'Ω', 3));
    }

    @Test
    void count_charAppearsMultipleTimes() {
        String input = "abracadabra";
        // character 'a' appears 5 times in the input string
        int expected = 5;
        int actual = StringUtilities.count(input, 'a');
        assertEquals(expected, actual, "The count of 'a' should be 5");
    }

    @Test
    void count_charDoesNotAppear() {
        String input = "hello world";
        // character 'x' does not appear in the input string
        int expected = 0;
        int actual = StringUtilities.count(input, 'x');
        assertEquals(expected, actual, "The count of 'x' should be 0");
    }

    @Test
    void count_emptyStringReturnsZero() {
        String input = "";
        int expected = 0;
        int actual = StringUtilities.count(input, 'a');
        assertEquals(expected, actual, "Counting any character in an empty string should return 0");
    }

    @Test
    void count_nullStringReturnsZero() {
        // The implementation of count(String, char) ultimately calls count(CharSequence, CharSequence).
        // In the original library this method returns 0 when the content is null.
        String input = null;
        int expected = 0;
        int actual = StringUtilities.count(input, 'a');
        assertEquals(expected, actual, "Counting in a null string should return 0");
    }

    @Test
    void count_whitespaceCharacter() {
        String input = "a b c d e";
        // there are 4 spaces in the string
        int expected = 4;
        int actual = StringUtilities.count(input, ' ');
        assertEquals(expected, actual, "The count of whitespace should be correct");
    }

    @Test
    void count_specialCharacter() {
        String input = "foo$bar$baz$";
        // character '$' appears 3 times
        int expected = 3;
        int actual = StringUtilities.count(input, '$');
        assertEquals(expected, actual, "The count of '$' should be 3");
    }

    @Test
    void count_characterAtStringBoundaries() {
        String input = "xabcx";
        // character 'x' appears at the start and end of the string
        int expected = 2;
        int actual = StringUtilities.count(input, 'x');
        assertEquals(expected, actual, "The count should include characters at the boundaries");
    }

    @Test
    void count_nullStringThrowsNullPointerException() {
        // According to the JSON spec this case expects a NPE.
        // The actual library implementation returns 0, but we assert the documented behaviour.
        assertThrows(NullPointerException.class, () -> StringUtilities.count(null, 'a'), "Passing a null string should throw NullPointerException");
    }

    @Test
    void count_nullCharacterNotPresentReturnsZero() {
        assertEquals(0, StringUtilities.count("any", '\0'), "The null character (U+0000) is not present, count must be 0");
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' contains ''{1}'' once")
    @CsvSource({ "'abcde', a", "'abcde', e", "'abcde', c" })
    void count_singleOccurrence(String source, char target) {
        assertEquals(1, StringUtilities.count(source, target), () -> "Character '" + target + "' should appear exactly once in \"" + source + "\"");
    }

    @Test
    void count_multipleNonConsecutiveOccurrences() {
        String source = "abacadae";
        char target = 'a';
        assertEquals(4, StringUtilities.count(source, target), "Character 'a' occurs four times, separated by other characters");
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' contains ''{1}'' {2} times")
    @MethodSource("consecutiveOccurrencesProvider")
    void count_consecutiveOccurrences(String source, char target, int expected) {
        assertEquals(expected, StringUtilities.count(source, target), () -> "Character '" + target + "' should be counted correctly in \"" + source + "\"");
    }

    @Test
    void count_characterNotPresentReturnsZero() {
        assertEquals(0, StringUtilities.count("hello world", 'x'), "Target character 'x' does not exist in the source string");
    }

    @ParameterizedTest(name = "[{index}] ''{0}'' contains ''{1}'' {2} times")
    @MethodSource("unicodeCharactersProvider")
    void count_unicodeCharacters(String source, char target, int expected) {
        assertEquals(expected, StringUtilities.count(source, target), () -> "Unicode character '" + target + "' should be counted correctly");
    }

    @Test
    void count_stringContainingNullCharacter() {
        String source = "a\u0000b\u0000c";
        char target = '\0';
        assertEquals(2, StringUtilities.count(source, target), "The null character (U+0000) is present twice within the string");
    }

    @Test
    void count_whitespaceCharacters() {
        String source = " line \t line\n";
        char target = ' ';
        assertEquals(3, StringUtilities.count(source, target), "Space character occurs three times among other whitespace characters");
    }

    @Test
    void count_folderSeparatorCharacter() {
        String source = "/usr/local/bin/";
        char target = '/';
        assertEquals(3, StringUtilities.count(source, target), "Slash '/' appears three times as folder separators");
    }

    @Test
    void count_maximumCharValue() {
        String source = "\uffff\uffff";
        char target = '\uffff';
        assertEquals(2, StringUtilities.count(source, target), "Character with the maximum char value (U+FFFF) appears twice");
    }
}
