package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

class StringConversions_toShort_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs a non‑null options instance; the default options are sufficient.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test.
        converter = null;
    }

    static Stream<Arguments> emptyAndNullInputProvider() {
        return Stream.of(// null string (converter null)
        Arguments.of(null, null, "0"), // empty string (converter null)
        Arguments.of("", null, "0"));
    }

    static Stream<Arguments> validShortParsingProvider() {
        return Stream.of(Arguments.of("0", "0"), Arguments.of("-32768", "-32768"), Arguments.of("32767", "32767"));
    }

    static Stream<Arguments> nonNumericOrOutOfRangeProvider() {
        return Stream.of(Arguments.of("abc", IllegalArgumentException.class), Arguments.of("40000", IllegalArgumentException.class));
    }

    static Stream<Arguments> whitespaceFormattingProvider() {
        return Stream.of(Arguments.of("   "), Arguments.of(" 123 "), Arguments.of("\n456"));
    }

    static Stream<Arguments> positiveSignAndLeadingZerosProvider() {
        return Stream.of(Arguments.of("+123", (short) 123), Arguments.of("00123", (short) 123));
    }

    static Stream<Arguments> exponentNotationProvider() {
        return Stream.of(Arguments.of("1e2", (short) 100), Arguments.of("-2E1", (short) -20));
    }

    static Stream<Arguments> outOfRangeEdgeCasesProvider() {
        return Stream.of(Arguments.of("32768"), Arguments.of("-32769"));
    }

    @Test
    void emptyStringReturnsZero() {
        // given
        String empty = "";
        // when
        Short result = StringConversions.toShort(empty, converter);
        // then
        assertEquals((short) 0, result);
    }

    @Test
    void validShortStringIsConverted() {
        // given
        String value = "123";
        // when
        Short result = StringConversions.toShort(value, converter);
        // then
        assertEquals(Short.valueOf((short) 123), result);
    }

    @Test
    void outOfRangeNumberThrowsIllegalArgumentException() {
        // given – a number larger than Short.MAX_VALUE (32767)
        String tooBig = "40000";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort(tooBig, converter));
        // The message should contain the original value and the short bounds.
        assertTrue(ex.getMessage().contains("Value '40000'"));
        assertTrue(ex.getMessage().contains(String.valueOf(Short.MIN_VALUE)));
        assertTrue(ex.getMessage().contains(String.valueOf(Short.MAX_VALUE)));
    }

    @Test
    void nonNumericStringThrowsIllegalArgumentException() {
        // given
        String notANumber = "abc";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort(notANumber, converter));
        assertTrue(ex.getMessage().contains("Value 'abc'"));
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("emptyAndNullInputProvider")
    void emptyAndNullInput(Object from, Converter conv, String expected) {
        Short result = StringConversions.toShort(from, conv);
        assertEquals(Short.valueOf(expected), result);
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("validShortParsingProvider")
    void validShortParsing(String from, String expected) {
        Short result = StringConversions.toShort(from, converter);
        assertEquals(Short.valueOf(expected), result);
    }

    @Test
    void decimalStringIsRoundedDown() {
        Short result = StringConversions.toShort("123.0", converter);
        assertEquals(Short.valueOf((short) 123), result);
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("nonNumericOrOutOfRangeProvider")
    void nonNumericOrOutOfRangeThrows(String from, Class<? extends Throwable> expected) {
        Throwable ex = assertThrows(expected, () -> StringConversions.toShort(from, converter));
        // For IllegalArgumentException we also check that the message mentions the original value.
        if (ex instanceof IllegalArgumentException) {
            assertTrue(ex.getMessage().contains("Value '" + from + "'"));
        }
    }

    @Test
    void nonStringInputThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> StringConversions.toShort(5, converter));
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("whitespaceFormattingProvider")
    void whitespaceFormattingThrows(String from) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort(from, converter));
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("positiveSignAndLeadingZerosProvider")
    void positiveSignAndLeadingZeros(String from, short expected) {
        Short result = StringConversions.toShort(from, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("exponentNotationProvider")
    void exponentNotation(String from, short expected) {
        Short result = StringConversions.toShort(from, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("outOfRangeEdgeCasesProvider")
    void outOfRangeEdgeCases(String from) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort(from, converter));
    }

    @Test
    void fractionalNumberThrows() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort("123.45", converter));
    }

    @Test
    void hexadecimalNotationThrows() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort("0x7F", converter));
    }

    @Test
    void literalNullStringThrows() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort("null", converter));
    }

    @Test
    void nonNullConverterIsIgnored() {
        // Use a mock to prove that the method does not interact with it.
        Converter dummy = Mockito.mock(Converter.class);
        Short result = StringConversions.toShort("123", dummy);
        assertEquals(Short.valueOf((short) 123), result);
    }

    @Test
    void tabWhitespaceThrows() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort("\t123", converter));
    }

    @Test
    void plusSignWithLeadingZeros() {
        Short result = StringConversions.toShort("+00123", converter);
        assertEquals(Short.valueOf((short) 123), result);
    }

    @Test
    void negativeZeroIsZero() {
        Short result = StringConversions.toShort("-0", converter);
        assertEquals(Short.valueOf((short) 0), result);
    }

    @Test
    void longSuffixNotationThrows() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort("123L", converter));
    }

    @Test
    void binaryNotationThrows() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort("0b1010", converter));
    }

    @Test
    void underscoreNumericLiteralThrows() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort("1_234", converter));
    }

    @Test
    void unicodeFullWidthDigitsThrow() {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toShort("１２3", converter));
    }

    @Test
    void overflowBeyondLongRangeThrowsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> StringConversions.toShort("9223372036854775808", converter));
    }
}
