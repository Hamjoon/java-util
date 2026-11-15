package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToSqlDate_Test {

    private static Stream<Arguments> provideSuccessfulConversions() {
        // Helper to obtain epoch millis from a java.sql.Date (which stores midnight UTC)
        java.sql.Date sqlDate = java.sql.Date.valueOf("2021-10-01");
        long sqlDateMillis = sqlDate.getTime();
        // Calendar instance in UTC set to the same instant
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2021‑10‑01T00:00:00Z
        utcCal.setTimeInMillis(1633036800000L);
        // Calendar instance in New York (same absolute instant)
        Calendar nyCal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        nyCal.setTimeInMillis(1633036800000L);
        // ISO‑8601 instant string
        String isoInstant = "2021-10-01T12:34:56.789Z";
        long isoInstantMillis = Instant.parse(isoInstant).toEpochMilli();
        // Date‑only string (interpreted as LocalDate at start of day)
        String dateOnly = "2021-10-01";
        long dateOnlyMillis = LocalDate.parse(dateOnly).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        // ZonedDateTime with offset +02:00 (Paris)
        ZonedDateTime zdt = ZonedDateTime.parse("2021-10-01T12:34:56+02:00[Europe/Paris]");
        long zdtMillis = zdt.toInstant().toEpochMilli();
        // Instant at epoch (0)
        Instant epochInstant = Instant.ofEpochMilli(0);
        long epochMillis = epochInstant.toEpochMilli();
        // Negative epoch long
        // one day before epoch
        long negativeMillis = -86_400_000L;
        return Stream.of(Arguments.of("java.sql.Date should be converted preserving the same epoch millis", sqlDate, sqlDateMillis), Arguments.of("java.util.Calendar (UTC) should be converted using its internal millis", utcCal, 1633036800000L), Arguments.of("java.util.Calendar (America/New_York) should be converted using its internal millis", nyCal, 1633036800000L), Arguments.of("Long representing epoch millis should be converted directly", 1633036800000L, 1633036800000L), Arguments.of("String ISO‑8601 instant should be parsed and converted", isoInstant, isoInstantMillis), Arguments.of("String date‑only should be parsed as LocalDate at start of day", dateOnly, dateOnlyMillis), Arguments.of("java.time.ZonedDateTime with positive offset should be converted to the same instant", zdt, zdtMillis), Arguments.of("java.time.Instant at epoch should be converted to epoch Date", epochInstant, epochMillis), Arguments.of("Negative epoch millis (Long) should be converted correctly", negativeMillis, negativeMillis));
    }

    private static Stream<Arguments> provideUnsupportedConversions() {
        return Stream.of(Arguments.of("Boolean value is not convertible to Date", true, IllegalArgumentException.class), Arguments.of("Arbitrary Object with no conversion rule", new Object(), IllegalArgumentException.class), Arguments.of("java.sql.Time does not contain a date component", java.sql.Time.valueOf("12:34:56"), IllegalArgumentException.class), Arguments.of("BigDecimal is not a supported source type", new BigDecimal("123.45"), IllegalArgumentException.class), Arguments.of("BigInteger is not a supported source type", new BigInteger("12345"), IllegalArgumentException.class), Arguments.of("AtomicInteger cannot be converted to Date", new java.util.concurrent.atomic.AtomicInteger(5), IllegalArgumentException.class), Arguments.of("AtomicLong cannot be converted to Date", new java.util.concurrent.atomic.AtomicLong(10L), IllegalArgumentException.class), Arguments.of("AtomicBoolean cannot be converted to Date", new java.util.concurrent.atomic.AtomicBoolean(true), IllegalArgumentException.class), Arguments.of("LocalTime lacks a date component", java.time.LocalTime.of(12, 34, 56), IllegalArgumentException.class), Arguments.of("OffsetTime does not contain a date", java.time.OffsetTime.of(12, 34, 56, 0, ZoneOffset.UTC), IllegalArgumentException.class), Arguments.of("OffsetDateTime is not listed among supported source types", java.time.OffsetDateTime.of(2021, 10, 1, 12, 34, 56, 0, ZoneOffset.ofHours(2)), IllegalArgumentException.class), Arguments.of("YearMonth cannot be mapped to a specific instant", java.time.YearMonth.of(2021, 10), IllegalArgumentException.class), Arguments.of("Year lacks month and day information", java.time.Year.of(2021), IllegalArgumentException.class), Arguments.of("MonthDay does not contain a year", java.time.MonthDay.of(10, 1), IllegalArgumentException.class), Arguments.of("String with invalid hour component should cause parsing exception", "2021-10-01T25:00:00Z", DateTimeParseException.class), Arguments.of("String representing non‑existent calendar date should trigger parsing exception", "2021-02-30", DateTimeParseException.class), Arguments.of("Time‑only string lacks a date component", "12:34:56", IllegalArgumentException.class), Arguments.of("Whitespace‑only string is not a valid date representation", "   ", IllegalArgumentException.class), Arguments.of("Non‑ISO format string should cause unsupported format exception", "2021/10/01", IllegalArgumentException.class), Arguments.of("Array objects are not convertible to Date", new Object[] { 1, 2, 3 }, IllegalArgumentException.class));
    }

    /**
     * -----------------------------------------------------------------
     *  Test that a java.util.Date instance is returned unchanged.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void convertToSqlDate_givenDate_returnsSameInstance() {
        // given
        Date now = new Date();
        // when
        Date result = Converter.convertToSqlDate(now);
        // then
        assertSame(now, result, "When a java.util.Date is supplied, the same instance should be returned.");
    }

    /**
     * -----------------------------------------------------------------
     *  Test conversion from java.sql.Timestamp to java.util.Date.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void convertToSqlDate_givenTimestamp_returnsDateWithSameInstant() {
        // given
        long epochMillis = System.currentTimeMillis();
        Timestamp ts = new Timestamp(epochMillis);
        // when
        Date result = Converter.convertToSqlDate(ts);
        // then
        assertNotNull(result, "Result must not be null for a non‑null Timestamp.");
        assertEquals(epochMillis, result.getTime(), "The converted Date must represent the same instant as the Timestamp.");
    }

    /**
     * -----------------------------------------------------------------
     *  Test conversion from java.time.LocalDate to java.util.Date.
     *  The expected Date is at the start of the day in the default
     *  time‑zone (midnight).  The Converter follows the same rule as
     *  java.sql.Date.valueOf(LocalDate).
     *  ----------------------------------------------------------------- **
     */
    @Test
    void convertToSqlDate_givenLocalDate_returnsDateAtStartOfDay() {
        // given
        LocalDate localDate = LocalDate.of(2023, 3, 15);
        // when
        Date result = Converter.convertToSqlDate(localDate);
        // then
        assertNotNull(result, "Result must not be null for a non‑null LocalDate.");
        // Compute expected milliseconds at start of day in the default zone
        long expected = localDate.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(expected, result.getTime(), "The Date should represent the start of the given LocalDate.");
    }

    /**
     * -----------------------------------------------------------------
     *  Test conversion from java.time.LocalDateTime to java.util.Date.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void convertToSqlDate_givenLocalDateTime_returnsCorrespondingDate() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 12, 34, 56, 123_000_000);
        // when
        Date result = Converter.convertToSqlDate(ldt);
        // then
        assertNotNull(result, "Result must not be null for a non‑null LocalDateTime.");
        long expected = ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals(expected, result.getTime(), "The Date should represent the same instant as the LocalDateTime.");
    }

    /**
     * -----------------------------------------------------------------
     *  Test that a null input yields null output (convertTo* contract).
     *  ----------------------------------------------------------------- **
     */
    @Test
    void convertToSqlDate_givenNull_returnsNull() {
        // given
        Object input = null;
        // when
        Date result = Converter.convertToSqlDate(input);
        // then
        assertNull(result, "convertToSqlDate should return null when the input is null.");
    }

    /**
     * -----------------------------------------------------------------
     *  Parameterised tests – successful conversions
     *  ----------------------------------------------------------------- **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulConversions")
    void convertToSqlDate_successful(String description, Object input, long expectedEpochMillis) {
        Date result = Converter.convertToSqlDate(input);
        assertNotNull(result, description + " – result must not be null");
        assertEquals(expectedEpochMillis, result.getTime(), description + " – epoch milliseconds must match");
    }

    /**
     * -----------------------------------------------------------------
     *  Parameterised tests – unsupported conversions / expected exceptions
     *  ----------------------------------------------------------------- **
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideUnsupportedConversions")
    void convertToSqlDate_unsupported(String description, Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToSqlDate(input), description + " – expected exception");
    }
}
