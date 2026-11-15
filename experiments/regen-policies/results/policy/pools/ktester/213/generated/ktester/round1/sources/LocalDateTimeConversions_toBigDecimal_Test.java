package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

class LocalDateTimeConversions_toBigDecimal_Test {

    private Converter converter;

    /**
     * Simple holder for a single parameterised test case.
     */
    private static class ArgumentsHolder {

        final Object from;

        // may be null to test NPE
        final Converter converter;

        // either BigDecimal or Class<? extends Throwable>
        final Object expected;

        final String description;

        final Class<? extends Throwable> expectedException;

        ArgumentsHolder(Object from, Converter converter, Object expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
            this.expectedException = (expected instanceof Class) ? (Class<? extends Throwable>) expected : null;
        }

        // Overload for expected exception cases
        ArgumentsHolder(Object from, Converter converter, Class<? extends Throwable> exceptionClass, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = exceptionClass;
            this.description = description;
            this.expectedException = exceptionClass;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a plain Converter – it behaves like the “IdentityConverter” described in the spec.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Helper methods for building expected BigDecimal values.
     * -------------------------------------------------------------- */
    private static BigDecimal instantToBigDecimal(Instant i) {
        // epoch seconds + nanos / 1_000_000_000
        return BigDecimal.valueOf(i.getEpochSecond()).add(BigDecimal.valueOf(i.getNano()).divide(BigDecimal.valueOf(1_000_000_000L)));
    }

    /**
     * Group: conversion from various source types (including success & failure).
     */
    static Stream<ArgumentsHolder> sourceTypeProvider() {
        return Stream.of(// null source → NPE
        new ArgumentsHolder(null, new Converter(new ConverterOptions()), NullPointerException.class, "null source"), // Date
        new ArgumentsHolder(new Date(1_600_000_000_000L), null, instantToBigDecimal(Instant.ofEpochMilli(1_600_000_000_000L)), "java.util.Date"), // Instant
        new ArgumentsHolder(Instant.ofEpochSecond(1623L, 123_456_789L), null, new BigDecimal("1623.123456789"), "java.time.Instant"), // LocalDateTime (UTC)
        new ArgumentsHolder(LocalDateTime.of(2023, 5, 15, 12, 30, 45, 987_654_321), null, instantToBigDecimal(LocalDateTime.of(2023, 5, 15, 12, 30, 45, 987_654_321).toInstant(ZoneOffset.UTC)), "java.time.LocalDateTime"), // ZonedDateTime (offset -05:00)
        new ArgumentsHolder(ZonedDateTime.of(2023, 5, 15, 12, 30, 45, 0, ZoneOffset.ofHours(-5)), null, instantToBigDecimal(ZonedDateTime.of(2023, 5, 15, 12, 30, 45, 0, ZoneOffset.ofHours(-5)).toInstant()), "java.time.ZonedDateTime"), // Calendar (UTC)
        new ArgumentsHolder(createUtcCalendar(1_600_000_123_456L), null, instantToBigDecimal(Instant.ofEpochMilli(1_600_000_123_456L)), "java.util.Calendar"), // Long (epoch millis)
        new ArgumentsHolder(Long.valueOf(1_600_000_000_000L), null, instantToBigDecimal(Instant.ofEpochMilli(1_600_000_000_000L)), "java.lang.Long"), // String (unsupported) → ConversionException
        new ArgumentsHolder("not a date", null, ConversionException.class, "unsupported String"));
    }

    /**
     * Group: java.sql.Timestamp
     */
    static Stream<ArgumentsHolder> timestampProvider() {
        return Stream.of(new ArgumentsHolder(Timestamp.from(Instant.ofEpochSecond(1623L, 123_456_789L)), null, new BigDecimal("1623.123456789"), "java.sql.Timestamp"));
    }

    /**
     * Group: java.sql.Date
     */
    static Stream<ArgumentsHolder> sqlDateProvider() {
        return Stream.of(new ArgumentsHolder(java.sql.Date.valueOf("2023-05-15"), null, instantToBigDecimal(LocalDate.of(2023, 5, 15).atStartOfDay(ZoneOffset.UTC).toInstant()), "java.sql.Date"));
    }

    /**
     * Group: LocalDate
     */
    static Stream<ArgumentsHolder> localDateProvider() {
        return Stream.of(new ArgumentsHolder(LocalDate.of(2023, 5, 15), null, instantToBigDecimal(LocalDate.of(2023, 5, 15).atStartOfDay(ZoneOffset.UTC).toInstant()), "java.time.LocalDate"));
    }

    /**
     * Group: LocalTime
     */
    static Stream<ArgumentsHolder> localTimeProvider() {
        return Stream.of(new ArgumentsHolder(LocalTime.of(12, 30, 45, 123_456_789), null, new BigDecimal("45045.123456789"), "java.time.LocalTime"));
    }

    /**
     * Group: OffsetDateTime
     */
    static Stream<ArgumentsHolder> offsetDateTimeProvider() {
        return Stream.of(new ArgumentsHolder(OffsetDateTime.of(2023, 5, 15, 12, 30, 45, 987_654_321, ZoneOffset.ofHours(-5)), null, instantToBigDecimal(OffsetDateTime.of(2023, 5, 15, 12, 30, 45, 987_654_321, ZoneOffset.ofHours(-5)).toInstant()), "java.time.OffsetDateTime"));
    }

    /**
     * Group: YearMonth
     */
    static Stream<ArgumentsHolder> yearMonthProvider() {
        return Stream.of(new ArgumentsHolder(YearMonth.of(2023, 5), null, instantToBigDecimal(YearMonth.of(2023, 5).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant()), "java.time.YearMonth"));
    }

    /**
     * Group: Year (1970)
     */
    static Stream<ArgumentsHolder> yearProvider() {
        return Stream.of(new ArgumentsHolder(Year.of(1970), null, BigDecimal.ZERO, "java.time.Year (epoch)"));
    }

    /**
     * Group: AtomicLong
     */
    static Stream<ArgumentsHolder> atomicLongProvider() {
        return Stream.of(new ArgumentsHolder(new AtomicLong(1_600_000_123_456L), null, instantToBigDecimal(Instant.ofEpochMilli(1_600_000_123_456L)), "java.util.concurrent.atomic.AtomicLong"));
    }

    /**
     * Group: BigInteger (epoch‑millis)
     */
    static Stream<ArgumentsHolder> bigIntegerProvider() {
        return Stream.of(new ArgumentsHolder(new BigInteger("1600000123456"), null, instantToBigDecimal(Instant.ofEpochMilli(1_600_000_123_456L)), "java.math.BigInteger"));
    }

    /**
     * Group: negative Instant
     */
    static Stream<ArgumentsHolder> negativeInstantProvider() {
        return Stream.of(new ArgumentsHolder(Instant.ofEpochSecond(-1L, 500_000_000L), null, new BigDecimal("-0.5"), "negative Instant"));
    }

    /**
     * Group: zero Instant
     */
    static Stream<ArgumentsHolder> zeroInstantProvider() {
        return Stream.of(new ArgumentsHolder(Instant.ofEpochSecond(0L, 0L), null, BigDecimal.ZERO, "zero Instant"));
    }

    /**
     * Group: unsupported MonthDay
     */
    static Stream<ArgumentsHolder> monthDayProvider() {
        return Stream.of(new ArgumentsHolder(MonthDay.of(5, 15), null, ConversionException.class, "unsupported MonthDay"));
    }

    /**
     * Group: null converter
     */
    static Stream<ArgumentsHolder> nullConverterProvider() {
        return Stream.of(new ArgumentsHolder(Instant.now(), null, NullPointerException.class, "null converter"));
    }

    /**
     * Group: overflow epoch‑millis (Long)
     */
    static Stream<ArgumentsHolder> overflowLongProvider() {
        return Stream.of(new ArgumentsHolder(Long.valueOf(Long.MAX_VALUE), null, DateTimeException.class, "overflow Long.MAX_VALUE"));
    }

    /**
     * Group: overflow epoch‑millis (BigInteger)
     */
    static Stream<ArgumentsHolder> overflowBigIntegerProvider() {
        return Stream.of(new ArgumentsHolder(// Long.MAX_VALUE + 1
        new BigInteger("9223372036854775808"), null, DateTimeException.class, "overflow BigInteger"));
    }

    /**
     * Group: unsupported numeric type Double
     */
    static Stream<ArgumentsHolder> unsupportedDoubleProvider() {
        return Stream.of(new ArgumentsHolder(Double.valueOf(123.45), null, ConversionException.class, "unsupported Double"));
    }

    /* --------------------------------------------------------------
     *  Core execution helper.
     * -------------------------------------------------------------- */
    private void executeTestCase(ArgumentsHolder ah) {
        // Resolve the converter to use – if the holder supplies a non‑null one, use it,
        // otherwise fall back to the default set‑up converter.
        Converter usedConverter = ah.converter != null ? ah.converter : this.converter;
        if (ah.expectedException != null) {
            // Expect an exception.
            assertThrows(ah.expectedException, () -> LocalDateTimeConversions.toBigDecimal(ah.from, usedConverter), () -> "Expected exception " + ah.expectedException.getSimpleName() + " for case: " + ah.description);
        } else {
            // Normal conversion – compare using compareTo to ignore scale differences.
            BigDecimal actual = LocalDateTimeConversions.toBigDecimal(ah.from, usedConverter);
            assertNotNull(actual, "Result should not be null for case: " + ah.description);
            assertEquals(0, ((BigDecimal) ah.expected).compareTo(actual), () -> "Incorrect conversion for case: " + ah.description + ". Expected " + ah.expected + " but got " + actual);
        }
    }

    /**
     * Creates a UTC {@link Calendar} with the supplied epoch‑millis.
     */
    private static Calendar createUtcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Verify that a plain {@link Instant} is converted to the expected {@link BigDecimal}
     * representation (epoch seconds with fractional nanos).
     */
    @Test
    void toBigDecimal_fromInstant() {
        // 1,000,000.5 seconds
        Instant instant = Instant.ofEpochSecond(1_000_000L, 500_000_000L);
        BigDecimal expected = new BigDecimal("1000000.5");
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(instant, converter);
        assertNotNull(actual);
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but got " + actual);
    }

    /**
     * Verify that a {@link LocalDateTime} (interpreted as UTC) is converted correctly.
     */
    @Test
    void toBigDecimal_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MARCH, 15, 12, 30, 45, 250_000_000);
        // Convert to Instant assuming UTC
        Instant instant = ldt.toInstant(ZoneOffset.UTC);
        BigDecimal expected = new BigDecimal(instant.getEpochSecond()).add(new BigDecimal(instant.getNano()).divide(BigDecimal.valueOf(1_000_000_000L)));
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(ldt, converter);
        assertNotNull(actual);
        assertEquals(0, expected.compareTo(actual), () -> "Expected " + expected + " but got " + actual);
    }

    /**
     * Verify that a {@link Date} instance is converted correctly.
     */
    @Test
    void toBigDecimal_fromDate() {
        // Date stores milliseconds since epoch
        // 2020‑09‑13T12:26:40Z
        Date date = new Date(1_600_000_000_000L);
        Instant instant = date.toInstant();
        BigDecimal expected = new BigDecimal(instant.getEpochSecond()).add(new BigDecimal(instant.getNano()).divide(BigDecimal.valueOf(1_000_000_000L)));
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(date, converter);
        assertNotNull(actual);
        assertEquals(0, expected.compareTo(actual));
    }

    /**
     * Verify that a {@link Calendar} instance is converted correctly.
     */
    @Test
    void toBigDecimal_fromCalendar() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        // includes milliseconds part
        cal.setTimeInMillis(1_600_000_000_123L);
        Instant instant = cal.toInstant();
        BigDecimal expected = new BigDecimal(instant.getEpochSecond()).add(new BigDecimal(instant.getNano()).divide(BigDecimal.valueOf(1_000_000_000L)));
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(cal, converter);
        assertNotNull(actual);
        assertEquals(0, expected.compareTo(actual));
    }

    /**
     * Verify that a {@code Long} representing epoch milliseconds is converted correctly.
     */
    @Test
    void toBigDecimal_fromLongMillis() {
        Long epochMillis = 1_600_000_000_123L;
        Instant instant = Instant.ofEpochMilli(epochMillis);
        BigDecimal expected = new BigDecimal(instant.getEpochSecond()).add(new BigDecimal(instant.getNano()).divide(BigDecimal.valueOf(1_000_000_000L)));
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(epochMillis, converter);
        assertNotNull(actual);
        assertEquals(0, expected.compareTo(actual));
    }

    /**
     * Verify that a {@code null} source yields {@code null} result.
     */
    @Test
    void toBigDecimal_fromNull() {
        assertNull(LocalDateTimeConversions.toBigDecimal(null, converter));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("sourceTypeProvider")
    void testVariousSourceTypes(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] Timestamp")
    @MethodSource("timestampProvider")
    void testTimestamp(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] java.sql.Date")
    @MethodSource("sqlDateProvider")
    void testSqlDate(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] LocalDate")
    @MethodSource("localDateProvider")
    void testLocalDate(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] LocalTime")
    @MethodSource("localTimeProvider")
    void testLocalTime(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] OffsetDateTime")
    @MethodSource("offsetDateTimeProvider")
    void testOffsetDateTime(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] YearMonth")
    @MethodSource("yearMonthProvider")
    void testYearMonth(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] Year")
    @MethodSource("yearProvider")
    void testYear(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] AtomicLong")
    @MethodSource("atomicLongProvider")
    void testAtomicLong(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] BigInteger")
    @MethodSource("bigIntegerProvider")
    void testBigInteger(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] Negative Instant")
    @MethodSource("negativeInstantProvider")
    void testNegativeInstant(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] Zero Instant")
    @MethodSource("zeroInstantProvider")
    void testZeroInstant(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] MonthDay (unsupported)")
    @MethodSource("monthDayProvider")
    void testMonthDayUnsupported(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] Null Converter")
    @MethodSource("nullConverterProvider")
    void testNullConverter(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] Overflow Long")
    @MethodSource("overflowLongProvider")
    void testOverflowLong(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] Overflow BigInteger")
    @MethodSource("overflowBigIntegerProvider")
    void testOverflowBigInteger(ArgumentsHolder ah) {
        executeTestCase(ah);
    }

    @ParameterizedTest(name = "[{index}] Unsupported Double")
    @MethodSource("unsupportedDoubleProvider")
    void testUnsupportedDouble(ArgumentsHolder ah) {
        executeTestCase(ah);
    }
}
