package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import java.util.stream.Stream;
import static com.cedarsoftware.util.StringUtilities.removeLeadingAndTrailingQuotes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringUtilities_removeLeadingAndTrailingQuotes_Test {

    private static Stream<String> emptyStrings() {
        // whitespace is not a quote, method returns same string
        return Stream.of("", "   ");
    }

    private static Stream<String> stringsWithoutQuotes() {
        return Stream.of("Foo", "Hello, World!", " \"Embedded\" quotes ", "12345", "   leadingSpace", "trailingSpace   ");
    }

    private static Stream<Arguments> quoteScenarios() {
        return Stream.of(// simple leading & trailing
        Arguments.of("\"Foo\"", "Foo"), // only leading
        Arguments.of("\"Foo", "Foo"), // only trailing
        Arguments.of("Foo\"", "Foo"), // multiple leading / trailing quotes
        Arguments.of("\"\"Foo\"\"", "Foo"), Arguments.of("\"\"\"Bar\"\"\"", "Bar"), // quotes interleaved with other characters
        Arguments.of("\"\"\"This is \"really\" weird.\"\"\"", "This is \"really\" weird."), // single quote character should become empty string
        Arguments.of("\"", ""), // string consisting solely of quotes (odd/even count)
        // four quotes -> all stripped
        Arguments.of("\"\"\"\"", ""), // three quotes -> all stripped
        Arguments.of("\"\"\"", ""), // leading/trailing quotes with whitespace inside
        Arguments.of("\"  abc  \"", "  abc  "), // quotes around whitespace
        Arguments.of("\"   \"", "   "), // mixed content with quotes at edges (from JSON)
        Arguments.of("\"\"abc\"def\"\"", "abc\"def"), Arguments.of("\"\"\"abc\"def\"\"", "abc\"def"), // mixed leading/trailing quote counts (three leading, two trailing)
        Arguments.of("\"\"\"abc\"def\"\"", "abc\"def"), // whitespace outside quotes must stay untouched
        Arguments.of("  \"Hello\"  ", "  \"Hello\"  "), // Unicode characters surrounded by quotes
        Arguments.of("\"☃©\"", "☃©"));
    }

    private static Stream<Arguments> internalQuoteScenarios() {
        return Stream.of(// escaped internal quotes (backslashes are part of the string literal)
        Arguments.of("\"He said, \\\"Hi\\\"\"", "He said, \\\"Hi\\\""), Arguments.of("\"\"He said, \"Hi\"\"\"", "He said, \"Hi\""), Arguments.of("\"\"\"A \"B\" C\"\"\"", "A \"B\" C"), // internal quotes only – should remain unchanged
        Arguments.of("a\"b\"c", "a\"b\"c"));
    }

    private static Stream<Arguments> edgeCaseScenarios() {
        return Stream.of(// String that is only a quote character
        Arguments.of("\"", ""), // String that is a quote followed by whitespace
        Arguments.of("\"   ", "   "), // Whitespace surrounded by quotes
        Arguments.of("\"   \"", "   "), // No quotes but contains quote‑like Unicode character (different code point)
        Arguments.of("\u201Cquoted\u201D", "\u201Cquoted\u201D"));
    }

    @ParameterizedTest
    @NullSource
    void removeLeadingAndTrailingQuotes_whenNull_returnsNull(String input) {
        assertNull(removeLeadingAndTrailingQuotes(input));
    }

    @ParameterizedTest
    @MethodSource("emptyStrings")
    void removeLeadingAndTrailingQuotes_whenEmpty_returnsEmpty(String input) {
        assertThat(removeLeadingAndTrailingQuotes(input)).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("stringsWithoutQuotes")
    void removeLeadingAndTrailingQuotes_whenNoQuotes_returnsOriginal(String input) {
        // The method should return the *same* reference when nothing is stripped.
        assertThat(removeLeadingAndTrailingQuotes(input)).isSameAs(input);
    }

    @ParameterizedTest
    @MethodSource("quoteScenarios")
    void removeLeadingAndTrailingQuotes_stripsOnlySurroundingQuotes(String input, String expected) {
        assertThat(removeLeadingAndTrailingQuotes(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("internalQuoteScenarios")
    void removeLeadingAndTrailingQuotes_preservesInternalQuotes(String input, String expected) {
        assertThat(removeLeadingAndTrailingQuotes(input)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("edgeCaseScenarios")
    void removeLeadingAndTrailingQuotes_edgeCases(String input, String expected) {
        assertThat(removeLeadingAndTrailingQuotes(input)).isEqualTo(expected);
    }
}
