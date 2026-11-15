package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

@ExtendWith(MockitoExtension.class)
class Converter_convertToBigDecimal_Test {

    static Stream<Arguments> numberInputProvider() {
        return Stream.of(Arguments.of(123, "123"), Arguments.of(45.67, "45.67"), Arguments.of("-9876543210", "-9876543210"));
    }

    static Stream<Arguments> stringNumericProvider() {
        return Stream.of(Arguments.of("123.456", "123.456"), Arguments.of("   789   ", "789"));
    }

    static Stream<Arguments> stringNonNumericProvider() {
        return Stream.of(Arguments.of("abc123"));
    }

    static Stream<Arguments> booleanInputProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, "1"), Arguments.of(Boolean.FALSE, "0"));
    }

    static Stream<Arguments> atomicInputProvider() {
        return Stream.of(Arguments.of(new AtomicInteger(42), "42"), Arguments.of(new AtomicLong(9876543210L), "9876543210"), Arguments.of(new AtomicBoolean(true), "1"), Arguments.of(new AtomicBoolean(false), "0"));
    }

    static Stream<Arguments> temporalInputProvider() {
        return Stream.of(// java.util.Date at epoch
        Arguments.of(new Date(0L), "0"), // java.sql.Timestamp for 2021‑01‑01T00:00:00Z
        Arguments.of(new Timestamp(1609459200000L), "1609459200000"), // LocalDateTime at epoch (treated as epoch millis)
        Arguments.of(LocalDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), "0"), // ZonedDateTime at epoch UTC
        Arguments.of(ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC")), "0"), // LocalDate one day after epoch (24 h = 86 400 000 ms)
        Arguments.of(LocalDate.ofEpochDay(1), "86400000"));
    }

    static Stream<Arguments> edgeNumericStringProvider() {
        return Stream.of(Arguments.of("+12345", "12345"), Arguments.of("-0", "0"), Arguments.of("0.000", "0.000"), Arguments.of("1e3", "1000"), Arguments.of("1E-2", "0.01"));
    }

    static Stream<Arguments> emptyWhitespaceStringProvider() {
        return Stream.of(Arguments.of(""), Arguments.of("    "), Arguments.of("   42   "));
    }

    static Stream<Arguments> largeNumberProvider() {
        return Stream.of(Arguments.of("123456789012345678901234567890", "123456789012345678901234567890"), Arguments.of(new BigInteger("99999999999999999999999999999999999999"), "99999999999999999999999999999999999999"));
    }

    static Stream<Arguments> unsupportedCollectionProvider() {
        return Stream.of(Arguments.of(Arrays.asList(1, 2, 3)), Arguments.of(Map.of("key", "value")));
    }

    static Stream<Arguments> invalidNumericStringProvider() {
        return Stream.of(Arguments.of("1e"), Arguments.of("e10"), Arguments.of("1.2.3"), Arguments.of("++123"), Arguments.of("--5"), Arguments.of("12-34"), Arguments.of("0x10"), Arguments.of("NaN"), Arguments.of("Infinity"));
    }

    static Stream<Arguments> specialFloatingPointProvider() {
        return Stream.of(Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Double.NEGATIVE_INFINITY), Arguments.of(Float.NaN), Arguments.of(Float.POSITIVE_INFINITY), Arguments.of(Float.NEGATIVE_INFINITY));
    }

    static Stream<Arguments> unsupportedTemporalProvider() {
        return Stream.of(Arguments.of(Instant.ofEpochMilli(1609459200000L)), Arguments.of(OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1))), Arguments.of(LocalTime.of(12, 34, 56)));
    }

    static Stream<Arguments> unsupportedObjectProvider() {
        return Stream.of(Arguments.of(new AtomicReference<>(123)), Arguments.of(new StringBuilder("12345")), Arguments.of(TestEnum.VALUE), Arguments.of(new int[] { 1, 2, 3, 4 }), Arguments.of(java.sql.Date.valueOf(LocalDate.of(2021, 6, 1))));
    }

    @Test
    @DisplayName("convertToBigDecimal returns null when source is null")
    void shouldReturnNullWhenInputIsNull() {
        // given
        Object source = null;
        // when
        BigDecimal result = Converter.convertToBigDecimal(source);
        // then
        assertNull(result, "Expected null when converting a null source");
    }

    @Test
    @DisplayName("convertToBigDecimal converts a BigDecimal instance unchanged")
    void shouldReturnSameBigDecimalInstance() {
        // given
        BigDecimal source = new BigDecimal("12345.6789");
        // when
        BigDecimal result = Converter.convertToBigDecimal(source);
        // then
        assertEquals(source, result, "BigDecimal should be returned unchanged");
    }

    @Test
    @DisplayName("convertToBigDecimal converts a numeric String")
    void shouldConvertStringToBigDecimal() {
        // given
        String source = "98765.4321";
        // when
        BigDecimal result = Converter.convertToBigDecimal(source);
        // then
        assertEquals(new BigDecimal("98765.4321"), result, "String numeric value should be converted to equivalent BigDecimal");
    }

    @Test
    @DisplayName("convertToBigDecimal converts an Integer")
    void shouldConvertIntegerToBigDecimal() {
        // given
        Integer source = 42;
        // when
        BigDecimal result = Converter.convertToBigDecimal(source);
        // then
        assertEquals(BigDecimal.valueOf(42), result, "Integer should be converted to a BigDecimal with the same value");
    }

    @Test
    @DisplayName("convertToBigDecimal converts a Long")
    void shouldConvertLongToBigDecimal() {
        // given
        Long source = 123456789L;
        // when
        BigDecimal result = Converter.convertToBigDecimal(source);
        // then
        assertEquals(BigDecimal.valueOf(123456789L), result, "Long should be converted to a BigDecimal with the same value");
    }

    @Test
    @DisplayName("convertToBigDecimal converts a Double")
    void shouldConvertDoubleToBigDecimal() {
        // given
        Double source = 3.14159;
        // when
        BigDecimal result = Converter.convertToBigDecimal(source);
        // then
        assertEquals(BigDecimal.valueOf(3.14159), result, "Double should be converted to a BigDecimal with the same numeric value");
    }

    @Test
    @DisplayName("convertToBigDecimal converts a BigInteger")
    void shouldConvertBigIntegerToBigDecimal() {
        // given
        BigInteger source = new BigInteger("999999999999999999");
        // when
        BigDecimal result = Converter.convertToBigDecimal(source);
        // then
        assertEquals(new BigDecimal(source), result, "BigInteger should be converted to an equivalent BigDecimal");
    }

    @Test
    @DisplayName("convertToBigDecimal throws IllegalArgumentException for unsupported type")
    void shouldThrowForUnsupportedSourceType() {
        // given
        // not a supported conversion source
        Object source = new Object();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigDecimal(source), "Unsupported source types should cause IllegalArgumentException");
    }

    // ---------- Number Input (Integer, Double, numeric String) ----------
    @ParameterizedTest(name = "{index} => source={0}, expected={1}")
    @MethodSource("numberInputProvider")
    @DisplayName("Number Input conversions")
    void shouldConvertNumberInputs(Object source, String expected) {
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(new BigDecimal(expected), result);
    }

    // ---------- String Numeric Input (including whitespace trimming) ----------
    @ParameterizedTest(name = "{index} => source=\"{0}\", expected={1}")
    @MethodSource("stringNumericProvider")
    @DisplayName("String Numeric Input conversions")
    void shouldConvertStringNumericInputs(String source, String expected) {
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(new BigDecimal(expected), result);
    }

    // ---------- String Non‑Numeric Input ----------
    @ParameterizedTest(name = "{index} => source=\"{0}\" throws NumberFormatException")
    @MethodSource("stringNonNumericProvider")
    @DisplayName("String Non‑Numeric Input throws NumberFormatException")
    void shouldThrowForStringNonNumeric(String source) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToBigDecimal(source));
    }

    // ---------- Boolean Input ----------
    @ParameterizedTest(name = "{index} => source={0}, expected={1}")
    @MethodSource("booleanInputProvider")
    @DisplayName("Boolean Input conversions")
    void shouldConvertBooleanInputs(Boolean source, String expected) {
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(new BigDecimal(expected), result);
    }

    // ---------- Date Input ----------
    @Test
    @DisplayName("Date Input (ISO‑8601 string) converts to epoch millis")
    void shouldConvertIsoStringDateToBigDecimal() {
        // Using java.util.Date constructed from epoch millis
        Date date = Date.from(Instant.parse("2023-01-01T00:00:00Z"));
        BigDecimal result = Converter.convertToBigDecimal(date);
        assertEquals(new BigDecimal("1672531200000"), result);
    }

    // ---------- Calendar Input ----------
    @Test
    @DisplayName("Calendar Input converts to epoch millis")
    void shouldConvertCalendarToBigDecimal() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1672531200000L);
        BigDecimal result = Converter.convertToBigDecimal(cal);
        assertEquals(new BigDecimal("1672531200000"), result);
    }

    // ---------- Atomic Types ----------
    @ParameterizedTest(name = "{index} => source={0}, expected={1}")
    @MethodSource("atomicInputProvider")
    @DisplayName("Atomic Types conversions")
    void shouldConvertAtomicTypes(Object source, String expected) {
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(new BigDecimal(expected), result);
    }

    // ---------- Temporal Types ----------
    @ParameterizedTest(name = "{index} => source={0}, expected={1}")
    @MethodSource("temporalInputProvider")
    @DisplayName("Temporal Types conversions")
    void shouldConvertTemporalTypes(Object source, String expected) {
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(new BigDecimal(expected), result);
    }

    // ---------- Edge Numeric Strings ----------
    @ParameterizedTest(name = "{index} => source=\"{0}\", expected={1}")
    @MethodSource("edgeNumericStringProvider")
    @DisplayName("Edge Numeric Strings conversions")
    void shouldParseEdgeNumericStrings(String source, String expected) {
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(new BigDecimal(expected), result);
    }

    // ---------- Empty and Whitespace Strings ----------
    @ParameterizedTest(name = "{index} => source=\"{0}\" throws NumberFormatException")
    @MethodSource("emptyWhitespaceStringProvider")
    @DisplayName("Empty / Whitespace Strings throw NumberFormatException")
    void shouldThrowForEmptyOrWhitespaceStrings(String source) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToBigDecimal(source));
    }

    // separately:
    @Test
    @DisplayName("Whitespace‑trimmed numeric string converts correctly")
    void shouldTrimAndConvertWhitespaceString() {
        BigDecimal result = Converter.convertToBigDecimal("   42   ");
        assertEquals(new BigDecimal("42"), result);
    }

    // ---------- Large Number Handling ----------
    @ParameterizedTest(name = "{index} => source=\"{0}\", expected={1}")
    @MethodSource("largeNumberProvider")
    @DisplayName("Large Number handling")
    void shouldHandleVeryLargeNumbers(String source, String expected) {
        BigDecimal result = Converter.convertToBigDecimal(source);
        assertEquals(new BigDecimal(expected), result);
    }

    // ---------- Unsupported Collection Types ----------
    @ParameterizedTest(name = "{index} => source={0} throws IllegalArgumentException")
    @MethodSource("unsupportedCollectionProvider")
    @DisplayName("Unsupported Collection Types throw IllegalArgumentException")
    void shouldThrowForUnsupportedCollections(Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigDecimal(source));
    }

    // ---------- Invalid Numeric Strings ----------
    @ParameterizedTest(name = "{index} => source=\"{0}\" throws NumberFormatException")
    @MethodSource("invalidNumericStringProvider")
    @DisplayName("Invalid Numeric Strings throw NumberFormatException")
    void shouldThrowForInvalidNumericStrings(String source) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToBigDecimal(source));
    }

    // ---------- Special Floating Point Values ----------
    @ParameterizedTest(name = "{index} => source={0} throws NumberFormatException")
    @MethodSource("specialFloatingPointProvider")
    @DisplayName("Special Floating Point Values throw NumberFormatException")
    void shouldThrowForSpecialFloatingPointValues(Object source) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToBigDecimal(source));
    }

    // ---------- Unsupported Temporal Types ----------
    @ParameterizedTest(name = "{index} => source={0} throws IllegalArgumentException")
    @MethodSource("unsupportedTemporalProvider")
    @DisplayName("Unsupported Temporal Types throw IllegalArgumentException")
    void shouldThrowForUnsupportedTemporalTypes(Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigDecimal(source));
    }

    // ---------- Unsupported Object Types ----------
    @ParameterizedTest(name = "{index} => source={0} throws IllegalArgumentException")
    @MethodSource("unsupportedObjectProvider")
    @DisplayName("Unsupported Object Types throw IllegalArgumentException")
    void shouldThrowForUnsupportedObjectTypes(Object source) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToBigDecimal(source));
    }
}
