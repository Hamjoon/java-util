package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toSqlDate_Test {

    /**
     * Shared {@link Converter} instance used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – the default options are sufficient for these tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long‑running test suites.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        ZoneId defaultZone = ZoneId.systemDefault();
        ???;
        utcCal.clear();
        utcCal.set(2023, Calendar.MAY, 5, 0, 0, 0);
        utcCal.set(Calendar.MILLISECOND, 0);
        long utcCalMillis = utcCal.getTimeInMillis();
        return Stream.of(Arguments.of(123456789L, 123456789L, "Long value should be interpreted as epoch milliseconds"), Arguments.of(42, 42L, "Integer value is auto‑boxed to Long and used as epoch milliseconds"), Arguments.of("1609459200000", 1_609_459_200_000L, "String containing a numeric epoch‑millis should be parsed to long"), Arguments.of(new Date(1_622_505_600_000L), 1_622_505_600_000L, "java.util.Date instance should keep its internal time"), Arguments.of(LocalDate.of(2022, 3, 15), epochFromLocalDate(LocalDate.of(2022, 3, 15)), "LocalDate is treated as start‑of‑day in the system default zone"), Arguments.of(LocalDateTime.of(2022, 3, 15, 12, 30, 45), epochFromLocalDateTime(LocalDateTime.of(2022, 3, 15, 12, 30, 45)), "LocalDateTime interpreted in the system default zone"), Arguments.of(Instant.ofEpochMilli(1_672_531_199_000L), 1_672_531_199_000L, "Instant directly provides epoch milliseconds"), Arguments.of(Timestamp.valueOf("2023-04-01 12:34:56.789"), 1_680_348_896_789L, "Timestamp should yield the same instant"), Arguments.of(utcCal, utcCalMillis, "Calendar in UTC should be converted correctly"), Arguments.of(ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York")), 1_672_540_799_000L, "ZonedDateTime converted to UTC instant"), Arguments.of(OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5)), 1_672_540_799_000L, "OffsetDateTime with -05:00 offset yields same instant as previous test"), Arguments.of(new AtomicLong(123456789L), 123456789L, "AtomicLong's value is used as epoch milliseconds"), Arguments.of(new BigDecimal("9876543210"), 9_876_543_210L, "BigDecimal with integer value is converted to long"), Arguments.of(new BigInteger("5555555555"), 5_555_555_555L, "BigInteger is converted to long epoch milliseconds"), Arguments.of("   +42  ", 42L, "String with whitespace and plus sign is trimmed and parsed"), Arguments.of(123.0d, 123L, "Double representing a whole number is cast to long"), Arguments.of(LocalTime.of(12, 34, 56), epochFromLocalTime(LocalTime.of(12, 34, 56)), "LocalTime interpreted as time‑of‑day on epoch date"));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Null source should cause NullPointerException"), Arguments.of("notANumber", NumberFormatException.class, "Non‑numeric string cannot be parsed to a long"), Arguments.of("123.45", NumberFormatException.class, "Decimal string should trigger NumberFormatException"), Arguments.of("   12a34   ", NumberFormatException.class, "Alphanumeric string after trimming cannot be parsed to a long"), Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "Boolean values are not supported for date conversion"), Arguments.of(new Object(), IllegalArgumentException.class, "Arbitrary Object should cause IllegalArgumentException"), Arguments.of(Double.NaN, IllegalArgumentException.class, "NaN cannot be interpreted as epoch milliseconds"), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class, "Infinite double values are invalid for epoch milliseconds"), Arguments.of(new BigDecimal("123.456"), ArithmeticException.class, "BigDecimal with fractional part cannot be losslessly converted to long"), Arguments.of(new BigInteger("9223372036854775808"), ArithmeticException.class, "BigInteger larger than Long.MAX_VALUE should overflow"), Arguments.of(new BigDecimal("9223372036854775808"), ArithmeticException.class, "BigDecimal exceeding long range should cause ArithmeticException"), Arguments.of(LocalDate.of(292_278_994, 8, 17), DateTimeException.class, "LocalDate beyond the maximum Instant cannot be represented"), Arguments.of(ZonedDateTime.of(292_278_994, 8, 17, 0, 0, 0, 0, ZoneOffset.UTC), DateTimeException.class, "ZonedDateTime at extreme upper bound overflows epoch‑millis"));
    }

    @Test
    void testToSqlDateFromDate() {
        // GIVEN
        Date now = new Date();
        // WHEN
        Date result = LocalDateConversions.toSqlDate(now, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(now.getTime(), result.getTime(), "Date produced from a java.util.Date should have the same epoch milliseconds");
    }

    @Test
    void testToSqlDateFromLong() {
        // GIVEN
        // 2021‑01‑01T00:00:00Z
        long epochMillis = 1_609_459_200_000L;
        // WHEN
        Date result = LocalDateConversions.toSqlDate(epochMillis, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(epochMillis, result.getTime(), "Date produced from a Long should represent the same epoch milliseconds");
    }

    @Test
    void testToSqlDateFromNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Date result = LocalDateConversions.toSqlDate(source, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null even when source is null");
        assertEquals(0L, result.getTime(), "When the source is null, the conversion should yield epoch 0 (midnight 1970‑01‑01 UTC)");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void testValidConversions(Object from, long expectedEpochMillis, String description) {
        Date result = LocalDateConversions.toSqlDate(from, converter);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(expectedEpochMillis, result.getTime(), () -> "Failed conversion: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionConversionProvider")
    void testExceptionConversions(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> LocalDateConversions.toSqlDate(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for: " + description);
    }
}
