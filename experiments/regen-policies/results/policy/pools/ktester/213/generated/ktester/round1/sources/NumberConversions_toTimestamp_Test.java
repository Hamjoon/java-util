package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toTimestamp_Test {

    /**
     * The Converter used for all tests – created once per test method.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter works out‑of‑the‑box for the
        // primitive and JDK types we exercise.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse.
        converter = null;
    }

    private static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(Arguments.of("Long value", 123L, 123L), Arguments.of("Integer value", 456, 456L), Arguments.of("Double value truncated", 78.9d, 78L), Arguments.of("BigDecimal value", new BigDecimal("12345"), 12345L), Arguments.of("BigInteger value", new BigInteger("9876543210"), 9876543210L), Arguments.of("Byte value", (byte) 7, 7L), Arguments.of("Short value", (short) 250, 250L), Arguments.of("Float value truncated", 12.34f, 12L));
    }

    private static Stream<Arguments> dateTimeProvider() {
        ZoneId systemZone = ZoneId.systemDefault();
        Instant now = Instant.now();
        ???;
    }

    private static Stream<Arguments> stringProvider() {
        return Stream.of(Arguments.of("Numeric epoch‑millis string", "1625097600000", 1625097600000L), Arguments.of("ISO‑8601 instant string", "2022-08-15T12:00:00Z", Instant.parse("2022-08-15T12:00:00Z").toEpochMilli()), Arguments.of("Date‑only string", "2023-03-10", LocalDate.parse("2023-03-10").atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()));
    }

    @Test
    void toTimestamp_fromLong() {
        // GIVEN
        // a specific epoch‑millis value
        Long source = 1_642_000_000_000L;
        // WHEN
        Timestamp ts = NumberConversions.toTimestamp(source, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(source.longValue(), ts.getTime(), "Timestamp should contain the same epoch millis as the source Long");
    }

    @Test
    void toTimestamp_fromString() {
        // GIVEN
        // same epoch value as a string
        String source = "1642000000123";
        // WHEN
        Timestamp ts = NumberConversions.toTimestamp(source, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(1_642_000_000_123L, ts.getTime(), "String representing a number should be converted to the matching Timestamp");
    }

    @Test
    void toTimestamp_fromDate() {
        // GIVEN
        Date date = new Date(1_642_000_000_456L);
        // WHEN
        Timestamp ts = NumberConversions.toTimestamp(date, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(date.getTime(), ts.getTime(), "Date#getTime() should be reflected in the resulting Timestamp");
    }

    @Test
    void toTimestamp_fromInstant() {
        // GIVEN
        Instant instant = Instant.ofEpochMilli(1_642_000_000_789L);
        // WHEN
        Timestamp ts = NumberConversions.toTimestamp(instant, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(instant.toEpochMilli(), ts.getTime(), "Instant should be converted to a Timestamp with the same epoch millis");
    }

    @Test
    void toTimestamp_fromNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Timestamp ts = NumberConversions.toTimestamp(source, converter);
        // THEN
        assertNotNull(ts);
        assertEquals(0L, ts.getTime(), "Null should be treated as 0 when converting to a primitive long, yielding epoch start");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericWrapperProvider")
    void toTimestamp_numericWrappers(String description, Object source, long expectedEpochMillis) {
        Timestamp ts = NumberConversions.toTimestamp(source, converter);
        assertNotNull(ts);
        assertEquals(expectedEpochMillis, ts.getTime(), "Timestamp epoch millis should match the expected value");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("dateTimeProvider")
    void toTimestamp_dateTimeObjects(String description, Object source, long expectedEpochMillis) {
        Timestamp ts = NumberConversions.toTimestamp(source, converter);
        assertNotNull(ts);
        assertEquals(expectedEpochMillis, ts.getTime(), "Timestamp epoch millis should match the expected value");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringProvider")
    void toTimestamp_stringRepresentations(String description, String source, long expectedEpochMillis) {
        Timestamp ts = NumberConversions.toTimestamp(source, converter);
        assertNotNull(ts);
        assertEquals(expectedEpochMillis, ts.getTime(), "Timestamp epoch millis should match the expected value");
    }

    @Test
    void toTimestamp_nullSource_throwsNPE() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toTimestamp(null, converter), "Null source should result in NullPointerException");
    }

    @Test
    void toTimestamp_unsupportedObject_throwsIAE() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toTimestamp(unsupported, converter), "Unsupported source type should cause IllegalArgumentException");
    }

    @Test
    void toTimestamp_nonNumericString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toTimestamp("not-a-number", converter), "Non‑numeric string should raise NumberFormatException");
    }

    @Test
    void toTimestamp_stringWithWhitespace_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toTimestamp("   12345   ", converter));
    }

    @Test
    void toTimestamp_stringWithPlusSign() {
        Timestamp ts = NumberConversions.toTimestamp("+1000", converter);
        assertEquals(1000L, ts.getTime(), "String with leading plus sign should be parsed as 1000 ms");
    }

    @Test
    void toTimestamp_stringWithNegativeNumber() {
        Timestamp ts = NumberConversions.toTimestamp("-500", converter);
        assertEquals(-500L, ts.getTime(), "Negative numeric string should produce a timestamp before the epoch");
    }

    @Test
    void toTimestamp_emptyString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toTimestamp("", converter));
    }

    @Test
    void toTimestamp_offsetTime() {
        OffsetTime ot = OffsetTime.parse("23:59:59+02:00");
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        long expected = ot.atDate(today).toInstant().toEpochMilli();
        Timestamp ts = NumberConversions.toTimestamp(ot, converter);
        assertEquals(expected, ts.getTime(), "OffsetTime combined with today’s date should be converted correctly");
    }

    @Test
    void toTimestamp_year() {
        Year year = Year.of(1970);
        long expected = year.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Timestamp ts = NumberConversions.toTimestamp(year, converter);
        assertEquals(expected, ts.getTime(), "Year should be interpreted as start of that year");
    }

    @Test
    void toTimestamp_leapDayLocalDate() {
        LocalDate leap = LocalDate.of(2000, 2, 29);
        long expected = leap.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Timestamp ts = NumberConversions.toTimestamp(leap, converter);
        assertEquals(expected, ts.getTime(), "Leap‑day LocalDate should be converted to start of that day");
    }

    @Test
    void toTimestamp_sqlDate() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2022-01-01");
        long expected = sqlDate.getTime();
        Timestamp ts = NumberConversions.toTimestamp(sqlDate, converter);
        assertEquals(expected, ts.getTime(), "java.sql.Date should be treated like java.util.Date");
    }

    @Test
    void toTimestamp_longMaxValue() {
        Timestamp ts = NumberConversions.toTimestamp(Long.MAX_VALUE, converter);
        assertEquals(Long.MAX_VALUE, ts.getTime(), "Maximum long value should be stored unchanged in Timestamp");
    }

    @Test
    void toTimestamp_longMinValue() {
        Timestamp ts = NumberConversions.toTimestamp(Long.MIN_VALUE, converter);
        assertEquals(Long.MIN_VALUE, ts.getTime(), "Minimum long value should be stored unchanged in Timestamp");
    }

    @Test
    void toTimestamp_zeroLong() {
        Timestamp ts = NumberConversions.toTimestamp(0L, converter);
        assertEquals(0L, ts.getTime(), "Zero epoch millis should produce the Unix epoch Timestamp");
    }

    @Test
    void toTimestamp_customConverterReturning123(@Mock Converter custom) {
        when(custom.convert(any(), eq(Long.class))).thenReturn(123L);
        Timestamp ts = NumberConversions.toTimestamp("custom", custom);
        assertEquals(123L, ts.getTime(), "Custom converter's result (123) should be used to build the Timestamp");
    }

    @Test
    void toTimestamp_customConverterReturningZero(@Mock Converter custom) {
        when(custom.convert(any(), eq(Long.class))).thenReturn(0L);
        Timestamp ts = NumberConversions.toTimestamp("ignored", custom);
        assertEquals(0L, ts.getTime(), "Custom converter returning 0 should yield the epoch Timestamp");
    }

    @Test
    void toTimestamp_customConverterReturningStringInstant(@Mock Converter custom) {
        when(custom.convert(any(), eq(Long.class))).thenReturn("2022-01-01T00:00:00Z");
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toTimestamp("ignored", custom), "Returning a non‑Number should cause IllegalArgumentException");
    }

    @Test
    void toTimestamp_customConverterThrowingRuntime(@Mock Converter custom) {
        when(custom.convert(any(), eq(Long.class))).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> NumberConversions.toTimestamp("any", custom));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toTimestamp_customConverterReturningNull(@Mock Converter custom) {
        when(custom.convert(any(), eq(Long.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> NumberConversions.toTimestamp("123", custom), "Null return from custom converter should lead to NPE in toLong");
    }

    @Test
    void toTimestamp_customConverterReturningBoolean(@Mock Converter custom) {
        when(custom.convert(any(), eq(Long.class))).thenReturn(Boolean.TRUE);
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toTimestamp("ignored", custom), "Returning a Boolean should cause IllegalArgumentException");
    }

    @Test
    void toTimestamp_stringWithDecimal_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toTimestamp("123.45", converter));
    }

    @Test
    void toTimestamp_stringScientificNotation_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.toTimestamp("1e3", converter));
    }

    @Test
    void toTimestamp_bigIntegerOverflow_throwsArithmeticException() {
        BigInteger tooBig = new BigInteger("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> NumberConversions.toTimestamp(tooBig, converter));
    }

    @Test
    void toTimestamp_booleanUnsupported_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.toTimestamp(Boolean.TRUE, converter));
    }

    @Test
    void toTimestamp_yearOutOfRange_throwsDateTimeException() {
        Year farFuture = Year.of(1_000_000);
        assertThrows(DateTimeException.class, () -> NumberConversions.toTimestamp(farFuture, converter));
    }
}
