package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

class Converter_convertToInteger_Test {

    private static Stream<Arguments> provideNullInput() {
        return Stream.of(Arguments.of((Object) null));
    }

    private static Stream<Arguments> provideIntegerInput() {
        return Stream.of(Arguments.of(42, 42));
    }

    private static Stream<Arguments> provideLongWithinRange() {
        return Stream.of(Arguments.of(1234567890L, 1234567890));
    }

    private static Stream<Arguments> provideLongOutOfRange() {
        return Stream.of(Arguments.of(Long.MAX_VALUE, ArithmeticException.class), Arguments.of(Long.MIN_VALUE, ArithmeticException.class));
    }

    private static Stream<Arguments> provideShortInput() {
        return Stream.of(Arguments.of((short) 12345, 12345));
    }

    private static Stream<Arguments> provideByteInput() {
        return Stream.of(Arguments.of((byte) 127, 127));
    }

    private static Stream<Arguments> provideDoubleTruncation() {
        return Stream.of(Arguments.of(123.987, 123), Arguments.of(-45.67, -45));
    }

    private static Stream<Arguments> provideFloatTruncation() {
        return Stream.of(Arguments.of(99.99f, 99));
    }

    private static Stream<Arguments> provideBigIntegerCases() {
        return Stream.of(Arguments.of(new BigInteger("1234567890123456789"), ArithmeticException.class), Arguments.of(new BigInteger("12345"), 12345));
    }

    private static Stream<Arguments> provideBigDecimalCases() {
        return Stream.of(Arguments.of(new BigDecimal("12345.67"), 12345), Arguments.of(new BigDecimal("-9876.54"), -9876), Arguments.of(new BigDecimal("2147483648"), ArithmeticException.class));
    }

    private static Stream<Arguments> provideStringNumeric() {
        return Stream.of(Arguments.of("1234", 1234));
    }

    private static Stream<Arguments> provideStringWithWhitespace() {
        return Stream.of(Arguments.of("   5678   ", 5678));
    }

    private static Stream<Arguments> provideStringNonNumeric() {
        return Stream.of(Arguments.of("abc123", NumberFormatException.class));
    }

    private static Stream<Arguments> provideBooleanInput() {
        return Stream.of(Arguments.of(Boolean.TRUE, IllegalArgumentException.class), Arguments.of(Boolean.FALSE, IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideCharacterDigit() {
        return Stream.of(Arguments.of('5', 5));
    }

    private static Stream<Arguments> provideCharacterNonDigit() {
        return Stream.of(Arguments.of('A', IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideDateInput() {
        return Stream.of(Arguments.of(new Date(0), IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideCalendarInput() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        return Stream.of(Arguments.of(cal, IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideLocalDateInput() {
        return Stream.of(Arguments.of(LocalDate.of(2023, 1, 1), IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideLocalDateTimeInput() {
        return Stream.of(Arguments.of(LocalDateTime.of(2023, 1, 1, 12, 0), IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideZonedDateTimeInput() {
        return Stream.of(Arguments.of(ZonedDateTime.now(), IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideTimestampInput() {
        return Stream.of(Arguments.of(new Timestamp(0), IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideAtomicIntegerInput() {
        return Stream.of(Arguments.of(new AtomicInteger(123), 123));
    }

    private static Stream<Arguments> provideAtomicLongWithinRange() {
        return Stream.of(Arguments.of(new AtomicLong(5000L), 5000));
    }

    private static Stream<Arguments> provideAtomicLongOutOfRange() {
        return Stream.of(Arguments.of(new AtomicLong(2147483648L), ArithmeticException.class));
    }

    private static Stream<Arguments> provideDoubleSpecialValues() {
        return Stream.of(Arguments.of(Double.NaN, IllegalArgumentException.class), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of(Double.NEGATIVE_INFINITY, IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideFloatSpecialValues() {
        return Stream.of(Arguments.of(Float.NaN, IllegalArgumentException.class), Arguments.of(Float.POSITIVE_INFINITY, IllegalArgumentException.class), Arguments.of(Float.NEGATIVE_INFINITY, IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideStringEmpty() {
        return Stream.of(Arguments.of("", NumberFormatException.class));
    }

    private static Stream<Arguments> provideStringWhitespaceOnly() {
        return Stream.of(Arguments.of("   ", NumberFormatException.class));
    }

    private static Stream<Arguments> provideStringDecimalFraction() {
        return Stream.of(Arguments.of("123.45", NumberFormatException.class));
    }

    private static Stream<Arguments> provideStringScientificNotation() {
        return Stream.of(Arguments.of("1e3", NumberFormatException.class));
    }

    private static Stream<Arguments> provideStringOutOfRange() {
        return Stream.of(Arguments.of("2147483648", ArithmeticException.class), Arguments.of("-2147483649", ArithmeticException.class));
    }

    private static Stream<Arguments> provideStringOnlySign() {
        return Stream.of(Arguments.of("+", NumberFormatException.class), Arguments.of("-", NumberFormatException.class));
    }

    private static Stream<Arguments> provideUnsupportedArray() {
        return Stream.of(Arguments.of(new int[] { 1, 2, 3 }, IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideUnsupportedList() {
        return Stream.of(Arguments.of(Arrays.asList(1, 2, 3), IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideUnsupportedEnum() {
        return Stream.of(Arguments.of(DayOfWeek.MONDAY, IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideSqlDateInput() {
        return Stream.of(Arguments.of(new java.sql.Date(0), IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideInstantInput() {
        return Stream.of(Arguments.of(Instant.now(), IllegalArgumentException.class));
    }

    private static Stream<Arguments> provideAtomicBooleanInput() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), IllegalArgumentException.class));
    }

    @Test
    void shouldConvertIntegerToInteger() {
        Integer source = 42;
        Integer result = Converter.convertToInteger(source);
        assertEquals(source, result, "Integer should be returned unchanged");
    }

    @Test
    void shouldConvertStringNumberToInteger() {
        String source = "123";
        Integer result = Converter.convertToInteger(source);
        assertEquals(123, result);
    }

    @Test
    void shouldConvertLongToInteger() {
        Long source = 7L;
        Integer result = Converter.convertToInteger(source);
        assertEquals(7, result);
    }

    @Test
    void shouldConvertDoubleToInteger() {
        Double source = 9.99;
        Integer result = Converter.convertToInteger(source);
        // Conversion truncates toward zero (same as casting to int)
        assertEquals(9, result);
    }

    @Test
    void shouldConvertBigDecimalToInteger() {
        java.math.BigDecimal source = new java.math.BigDecimal("56.78");
        Integer result = Converter.convertToInteger(source);
        assertEquals(56, result);
    }

    @Test
    void shouldConvertBigIntegerToInteger() {
        java.math.BigInteger source = new java.math.BigInteger("321");
        Integer result = Converter.convertToInteger(source);
        assertEquals(321, result);
    }

    @Test
    void shouldConvertBooleanTrueToInteger() {
        Boolean source = Boolean.TRUE;
        Integer result = Converter.convertToInteger(source);
        assertEquals(1, result);
    }

    @Test
    void shouldConvertBooleanFalseToInteger() {
        Boolean source = Boolean.FALSE;
        Integer result = Converter.convertToInteger(source);
        assertEquals(0, result);
    }

    @Test
    void shouldConvertAtomicIntegerToInteger() {
        java.util.concurrent.atomic.AtomicInteger source = new java.util.concurrent.atomic.AtomicInteger(55);
        Integer result = Converter.convertToInteger(source);
        assertEquals(55, result);
    }

    @Test
    void shouldConvertCharDigitToInteger() {
        Character source = '8';
        Integer result = Converter.convertToInteger(source);
        assertEquals(8, result);
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertNull(Converter.convertToInteger(null), "Null source must yield null");
    }

    @Test
    void shouldReturnNullWhenConversionNotSupported() {
        // An object that has no defined conversion to Integer (e.g., a plain Object)
        Object source = new Object();
        assertNull(Converter.convertToInteger(source), "Unsupported conversion should return null");
    }

    @Test
    void shouldNotThrowWhenConvertingUnsupportedType() {
        // Date is not directly convertible to Integer
        Object source = new java.util.Date();
        assertDoesNotThrow(() -> {
            Integer result = Converter.convertToInteger(source);
            assertNull(result);
        });
    }

    @ParameterizedTest(name = "{0} (Long within range) → {1}")
    @MethodSource("provideLongWithinRange")
    void shouldConvertLongWithinRange(Object source, Integer expected) {
        assertEquals(expected, Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Long out of range) → throws {1}")
    @MethodSource("provideLongOutOfRange")
    void shouldThrowWhenLongOutOfRange(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Short) → {1}")
    @MethodSource("provideShortInput")
    void shouldConvertShortToInteger(Object source, Integer expected) {
        assertEquals(expected, Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Byte) → {1}")
    @MethodSource("provideByteInput")
    void shouldConvertByteToInteger(Object source, Integer expected) {
        assertEquals(expected, Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Double) → {1}")
    @MethodSource("provideDoubleTruncation")
    void shouldTruncateDoubleToInteger(Object source, Integer expected) {
        assertEquals(expected, Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Float) → {1}")
    @MethodSource("provideFloatTruncation")
    void shouldTruncateFloatToInteger(Object source, Integer expected) {
        assertEquals(expected, Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (BigInteger) → {1}")
    @MethodSource("provideBigIntegerCases")
    void shouldHandleBigInteger(Object source, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convertToInteger(source));
        } else {
            assertEquals(expected, Converter.convertToInteger(source));
        }
    }

    @ParameterizedTest(name = "{0} (BigDecimal) → {1}")
    @MethodSource("provideBigDecimalCases")
    void shouldHandleBigDecimal(Object source, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convertToInteger(source));
        } else {
            assertEquals(expected, Converter.convertToInteger(source));
        }
    }

    @ParameterizedTest(name = "{0} (String numeric) → {1}")
    @MethodSource("provideStringNumeric")
    void shouldParseNumericString(Object source, Integer expected) {
        assertEquals(expected, Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (String with whitespace) → {1}")
    @MethodSource("provideStringWithWhitespace")
    void shouldParseWhitespaceTrimmedString(Object source, Integer expected) {
        assertEquals(expected, Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (String non‑numeric) → throws {1}")
    @MethodSource("provideStringNonNumeric")
    void shouldThrowWhenStringNonNumeric(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Boolean) → throws {1}")
    @MethodSource("provideBooleanInput")
    void shouldThrowWhenBooleanInput(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Character digit) → {1}")
    @MethodSource("provideCharacterDigit")
    void shouldConvertCharacterDigit(Object source, Integer expected) {
        assertEquals(expected, Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Character non‑digit) → throws {1}")
    @MethodSource("provideCharacterNonDigit")
    void shouldThrowWhenCharacterNonDigit(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Date) → throws {1}")
    @MethodSource("provideDateInput")
    void shouldThrowWhenDateInput(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Calendar) → throws {1}")
    @MethodSource("provideCalendarInput")
    void shouldThrowWhenCalendarInput(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (LocalDate) → throws {1}")
    @MethodSource("provideLocalDateInput")
    void shouldThrowWhenLocalDateInput(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (LocalDateTime) → throws {1}")
    @MethodSource("provideLocalDateTimeInput")
    void shouldThrowWhenLocalDateTimeInput(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (ZonedDateTime) → throws {1}")
    @MethodSource("provideZonedDateTimeInput")
    void shouldThrowWhenZonedDateTimeInput(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Timestamp) → throws {1}")
    @MethodSource("provideTimestampInput")
    void shouldThrowWhenTimestampInput(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (AtomicInteger) → {1}")
    @MethodSource("provideAtomicIntegerInput")
    void shouldUnwrapAtomicInteger(Object source, Integer expected) {
        assertEquals(expected, Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (AtomicLong within range) → {1}")
    @MethodSource("provideAtomicLongWithinRange")
    void shouldConvertAtomicLongWithinRange(Object source, Integer expected) {
        assertEquals(expected, Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (AtomicLong out of range) → throws {1}")
    @MethodSource("provideAtomicLongOutOfRange")
    void shouldThrowWhenAtomicLongOutOfRange(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Double special) → throws {1}")
    @MethodSource("provideDoubleSpecialValues")
    void shouldThrowWhenDoubleSpecial(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Float special) → throws {1}")
    @MethodSource("provideFloatSpecialValues")
    void shouldThrowWhenFloatSpecial(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (String empty) → throws {1}")
    @MethodSource("provideStringEmpty")
    void shouldThrowWhenStringEmpty(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (String whitespace only) → throws {1}")
    @MethodSource("provideStringWhitespaceOnly")
    void shouldThrowWhenStringWhitespaceOnly(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (String decimal fraction) → throws {1}")
    @MethodSource("provideStringDecimalFraction")
    void shouldThrowWhenStringDecimalFraction(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (String scientific notation) → throws {1}")
    @MethodSource("provideStringScientificNotation")
    void shouldThrowWhenStringScientificNotation(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (String out of range) → throws {1}")
    @MethodSource("provideStringOutOfRange")
    void shouldThrowWhenStringOutOfRange(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (String only sign) → throws {1}")
    @MethodSource("provideStringOnlySign")
    void shouldThrowWhenStringOnlySign(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Unsupported array) → throws {1}")
    @MethodSource("provideUnsupportedArray")
    void shouldThrowWhenArrayUnsupported(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Unsupported list) → throws {1}")
    @MethodSource("provideUnsupportedList")
    void shouldThrowWhenListUnsupported(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Unsupported enum) → throws {1}")
    @MethodSource("provideUnsupportedEnum")
    void shouldThrowWhenEnumUnsupported(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (java.sql.Date) → throws {1}")
    @MethodSource("provideSqlDateInput")
    void shouldThrowWhenSqlDateInput(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (Instant) → throws {1}")
    @MethodSource("provideInstantInput")
    void shouldThrowWhenInstantInput(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }

    @ParameterizedTest(name = "{0} (AtomicBoolean) → throws {1}")
    @MethodSource("provideAtomicBooleanInput")
    void shouldThrowWhenAtomicBooleanInput(Object source, Class<? extends Throwable> exception) {
        assertThrows(exception, () -> Converter.convertToInteger(source));
    }
}
