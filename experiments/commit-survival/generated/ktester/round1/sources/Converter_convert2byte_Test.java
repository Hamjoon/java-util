package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

class Converter_convert2byte_Test {

    static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of(null, (byte) 0));
    }

    static Stream<Arguments> numericExactRangeProvider() {
        return Stream.of(Arguments.of((byte) 5, (byte) 5), Arguments.of((short) 120, (byte) 120), Arguments.of(100, (byte) 100), // overflow wraps
        Arguments.of(255L, (byte) -1), // truncation
        Arguments.of(12.7f, (byte) 12), // truncation
        Arguments.of(-3.14, (byte) -3), Arguments.of(new BigDecimal("45"), (byte) 45), // truncation
        Arguments.of(new BigDecimal("45.9"), (byte) 45), Arguments.of(new BigInteger("127"), (byte) 127));
    }

    static Stream<Arguments> numericOverflowProvider() {
        return Stream.of(Arguments.of((short) 200, (byte) -56), Arguments.of(130, (byte) -126), Arguments.of(new BigInteger("128"), (byte) -128));
    }

    static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of(true, (byte) 1), Arguments.of(false, (byte) 0));
    }

    static Stream<Arguments> stringConversionProvider() {
        return Stream.of(Arguments.of("123", (byte) 123), Arguments.of("-12", (byte) -12), // overflow wraps
        Arguments.of("200", (byte) -56), Arguments.of("abc", IllegalArgumentException.class), // whitespace not allowed
        Arguments.of(" 7 ", IllegalArgumentException.class), // hex not allowed
        Arguments.of("0x10", IllegalArgumentException.class));
    }

    static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of(LocalDate.now()), Arguments.of(new Object()), Arguments.of(DayOfWeek.MONDAY), Arguments.of(new StringBuilder("123")), Arguments.of(new byte[] { 1, 2, 3 }), Arguments.of(Optional.of(5)), Arguments.of(LocalDateTime.now()), Arguments.of(ZonedDateTime.now()));
    }

    @Test
    void convert2byte_NullInput_ReturnsZero() {
        // given
        Object input = null;
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 0, result, "Null input should yield logical zero for primitive byte");
    }

    @Test
    void convert2byte_FromInteger_ReturnsByteValue() {
        // given
        // within byte range
        Integer input = 123;
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 123, result);
    }

    @Test
    void convert2byte_FromString_ValidNumber_ReturnsParsedByte() {
        // given
        String input = "45";
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 45, result);
    }

    @Test
    void convert2byte_FromString_InvalidNumber_ThrowsNumberFormatException() {
        // given
        String input = "not-a-number";
        // when / then
        assertThrows(NumberFormatException.class, () -> Converter.convert2byte(input));
    }

    @Test
    void convert2byte_FromBoolean_TrueReturnsOne() {
        // given
        Boolean input = Boolean.TRUE;
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 1, result);
    }

    @Test
    void convert2byte_FromBoolean_FalseReturnsZero() {
        // given
        Boolean input = Boolean.FALSE;
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 0, result);
    }

    @Test
    void convert2byte_FromLong_WithinByteRange_ReturnsByte() {
        // given
        // max byte value
        Long input = 127L;
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 127, result);
    }

    @Test
    void convert2byte_FromBigDecimal_Valid_ReturnsByte() {
        // given
        java.math.BigDecimal input = new java.math.BigDecimal("12");
        // when
        byte result = Converter.convert2byte(input);
        // then
        assertEquals((byte) 12, result);
    }

    // ---- Null input handling ------------------------------------------------
    @ParameterizedTest(name = "{index}: null input => {1}")
    @MethodSource("nullInputProvider")
    void convert2byte_NullInput(Object input, byte expected) {
        assertEquals(expected, Converter.convert2byte(input));
    }

    // ---- Numeric conversions - exact range ---------------------------------
    @ParameterizedTest(name = "{index}: {0} => {1}")
    @MethodSource("numericExactRangeProvider")
    void convert2byte_NumericExactRange(Object input, byte expected) {
        assertEquals(expected, Converter.convert2byte(input));
    }

    // ---- Numeric conversions - overflow handling ---------------------------
    @ParameterizedTest(name = "{index}: overflow {0} => {1}")
    @MethodSource("numericOverflowProvider")
    void convert2byte_NumericOverflow(Object input, byte expected) {
        assertEquals(expected, Converter.convert2byte(input));
    }

    // ---- Boolean conversions ------------------------------------------------
    @ParameterizedTest(name = "{index}: boolean {0} => {1}")
    @MethodSource("booleanProvider")
    void convert2byte_Boolean(Object input, byte expected) {
        assertEquals(expected, Converter.convert2byte(input));
    }

    // ---- Character conversion ------------------------------------------------
    @Test
    void convert2byte_CharacterConversion() {
        char input = 'A';
        byte result = Converter.convert2byte(input);
        assertEquals((byte) 65, result, "Character 'A' should map to its Unicode value 65");
    }

    // ---- String conversions -------------------------------------------------
    @ParameterizedTest(name = "{index}: string \"{0}\" => {1}")
    @MethodSource("stringConversionProvider")
    void convert2byte_StringConversion(String input, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convert2byte(input));
        } else {
            assertEquals((byte) expected, Converter.convert2byte(input));
        }
    }

    // ---- Date / Calendar conversions ----------------------------------------
    @Test
    void convert2byte_DateConversion_Epoch() {
        Date epoch = new Date(0L);
        byte result = Converter.convert2byte(epoch);
        assertEquals((byte) 0, result, "Epoch date (0 ms) should convert to 0");
    }

    @Test
    void convert2byte_CalendarConversion_CurrentTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long millis = cal.getTimeInMillis();
        // casting follows Java's two's‑complement truncation
        byte expected = (byte) millis;
        byte result = Converter.convert2byte(cal);
        assertEquals(expected, result, "Calendar conversion should cast its millisecond value to byte");
    }

    // ---- Unsupported source types -------------------------------------------
    @ParameterizedTest(name = "{index}: unsupported {0}")
    @MethodSource("unsupportedProvider")
    void convert2byte_UnsupportedTypes(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2byte(input));
    }
}
