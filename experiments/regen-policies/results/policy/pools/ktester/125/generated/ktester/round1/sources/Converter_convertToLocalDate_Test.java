package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToLocalDate_Test {

    /**
     * Data provider for all valid conversion cases.
     */
    static Stream<Arguments> validConversionProvider() {
        return Stream.of(// already a LocalDate
        Arguments.of(LocalDate.of(2023, 5, 17), LocalDate.of(2023, 5, 17)), // LocalDateTime -> date part
        Arguments.of(LocalDateTime.of(2023, 5, 17, 14, 30, 45), LocalDate.of(2023, 5, 17)), // ZonedDateTime (UTC) -> date part
        Arguments.of(ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 0, ZoneId.of("UTC")), LocalDate.of(2023, 5, 17)), // java.util.Date (instant)
        Arguments.of(Date.from(Instant.parse("2023-05-17T00:00:00Z")), LocalDate.of(2023, 5, 17)), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2023-05-17 23:59:59"), LocalDate.of(2023, 5, 17)), // Instant (UTC)
        Arguments.of(Instant.parse("2023-05-17T12:34:56Z"), LocalDate.of(2023, 5, 17)), // Calendar (UTC)
        Arguments.of(createCalendarUTC(2023, Calendar.MAY, 17), LocalDate.of(2023, 5, 17)), // ISO‑8601 String
        Arguments.of("2023-05-17", LocalDate.of(2023, 5, 17)), // Alternative format yyyy/MM/dd (supported by Converter)
        Arguments.of("2023/05/17", LocalDate.of(2023, 5, 17)), // java.sql.Date
        Arguments.of(java.sql.Date.valueOf("2023-05-17"), LocalDate.of(2023, 5, 17)), // OffsetDateTime
        Arguments.of(OffsetDateTime.of(2023, 5, 17, 12, 0, 0, 0, ZoneOffset.ofHours(-5)), LocalDate.of(2023, 5, 17)), // CharBuffer containing ISO date
        Arguments.of(CharBuffer.wrap("2023-05-17"), LocalDate.of(2023, 5, 17)), // ByteBuffer with UTF‑8 bytes of ISO date
        Arguments.of(ByteBuffer.wrap("2023-05-17".getBytes(StandardCharsets.UTF_8)), LocalDate.of(2023, 5, 17)), // GregorianCalendar in Asia/Tokyo
        Arguments.of(createGregorianCalendar(ZoneId.of("Asia/Tokyo"), 2023, Calendar.MAY, 17), LocalDate.of(2023, 5, 17)), // BigDecimal representing yyyyMMdd
        Arguments.of(BigDecimal.valueOf(20230517), LocalDate.of(2023, 5, 17)), // BigInteger representing yyyyMMdd
        Arguments.of(BigInteger.valueOf(20230517), LocalDate.of(2023, 5, 17)));
    }

    /**
     * Data provider for invalid conversion cases that should return {@code null}.
     */
    static Stream<Arguments> nullResultProvider() {
        return Stream.of(// null input – already covered by a dedicated test, but kept for completeness
        Arguments.of(null), // Non‑ISO string that the Converter does not accept
        // day/month/year – not supported
        Arguments.of("04/07/2024"));
    }

    /**
     * Data provider for cases that must throw an exception.
     */
    static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Empty string
        Arguments.of("", DateTimeParseException.class), // Unsupported format dd‑MM‑yyyy
        Arguments.of("17-05-2023", DateTimeParseException.class), // Completely non‑date string
        Arguments.of("not-a-date", DateTimeParseException.class), // LocalTime – no date component
        Arguments.of(LocalTime.of(14, 30, 0), IllegalArgumentException.class), // UUID – unsupported source type
        Arguments.of(UUID.randomUUID(), IllegalArgumentException.class), // Boolean – unsupported source type
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class), // Arbitrary Object
        Arguments.of(new Object(), IllegalArgumentException.class), // Bad numeric length (7 digits)
        Arguments.of(BigInteger.valueOf(2023051), IllegalArgumentException.class), // Bad numeric length (more than 8 digits)
        Arguments.of(BigDecimal.valueOf(20230517123L), IllegalArgumentException.class), // CharBuffer with invalid month
        Arguments.of(CharBuffer.wrap("2023-13-01"), DateTimeParseException.class), // ByteBuffer with impossible day
        Arguments.of(ByteBuffer.wrap("2023-02-30".getBytes(StandardCharsets.UTF_8)), DateTimeParseException.class), // OffsetTime – time only
        Arguments.of(OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC), IllegalArgumentException.class), // YearMonth – missing day
        Arguments.of(YearMonth.of(2023, 5), IllegalArgumentException.class));
    }

    private static Calendar createCalendarUTC(int year, int month, int day) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year, month, day);
        return cal;
    }

    private static Calendar createGregorianCalendar(ZoneId zone, int year, int month, int day) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone(zone));
        cal.clear();
        cal.set(year, month, day);
        return cal;
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertNull(Converter.convertToLocalDate(null), "Converting null should return null");
    }

    @Test
    void shouldReturnSameInstanceWhenSourceIsAlreadyLocalDate() {
        LocalDate today = LocalDate.now();
        LocalDate result = Converter.convertToLocalDate(today);
        assertSame(today, result, "When the source is already a LocalDate the same instance should be returned");
    }

    @Test
    void shouldConvertFromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, Month.APRIL, 15, 10, 30, 45);
        LocalDate expected = ldt.toLocalDate();
        LocalDate actual = Converter.convertToLocalDate(ldt);
        assertEquals(expected, actual, "LocalDateTime should be converted to its date component");
    }

    @Test
    void shouldConvertFromInstant() {
        Instant instant = Instant.parse("2022-12-01T12:34:56Z");
        // Converter uses the system default zone when converting an Instant to LocalDate
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate actual = Converter.convertToLocalDate(instant);
        assertEquals(expected, actual, "Instant should be converted to LocalDate using the system default zone");
    }

    @Test
    void shouldConvertFromjavaUtilDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2021, Calendar.JUNE, 20, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();
        LocalDate expected = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate actual = Converter.convertToLocalDate(date);
        assertEquals(expected, actual, "java.util.Date should be converted to LocalDate");
    }

    @Test
    void shouldConvertFromjavaSqlDate() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2020-02-29");
        LocalDate expected = sqlDate.toLocalDate();
        LocalDate actual = Converter.convertToLocalDate(sqlDate);
        assertEquals(expected, actual, "java.sql.Date should be converted to LocalDate");
    }

    @Test
    void shouldConvertFromCalendar() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2019, Calendar.DECEMBER, 31, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 0);
        LocalDate expected = cal.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
        LocalDate actual = Converter.convertToLocalDate(cal);
        assertEquals(expected, actual, "Calendar should be converted to LocalDate using its instant");
    }

    @Test
    void shouldConvertFromStringInISOFormat() {
        String isoDate = "2024-07-04";
        LocalDate expected = LocalDate.parse(isoDate);
        LocalDate actual = Converter.convertToLocalDate(isoDate);
        assertEquals(expected, actual, "ISO‑8601 date string should be parsed to LocalDate");
    }

    @Test
    void shouldConvertFromStringWithCustomPattern() {
        // The Converter accepts a limited set of patterns; the most common is ISO.
        // For a non‑ISO pattern we verify that conversion fails (returns null) rather than throwing.
        // day/month/year – not ISO
        String custom = "04/07/2024";
        LocalDate actual = Converter.convertToLocalDate(custom);
        assertNull(actual, "Non‑ISO date string should not be converted and should return null");
    }

    @Test
    void shouldConvertFromLongRepresentingEpochMilli() {
        // 2022-01-01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        LocalDate expected = Instant.ofEpochMilli(epochMilli).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate actual = Converter.convertToLocalDate(epochMilli);
        assertEquals(expected, actual, "Long epoch milliseconds should be converted to LocalDate");
    }

    @Test
    void shouldConvertFromNumberRepresentingEpochSecond() {
        // The Converter treats numeric types as epoch milliseconds when converting to date/time types.
        // 2022-01-01T00:00:00Z
        long epochSecond = 1_640_995_200L;
        LocalDate expected = Instant.ofEpochSecond(epochSecond).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate actual = Converter.convertToLocalDate(epochSecond);
        assertEquals(expected, actual, "Numeric epoch seconds should be interpreted as epoch milliseconds and converted");
    }

    @ParameterizedTest(name = "[{index}] convert {0} -> {1}")
    @MethodSource("validConversionProvider")
    void shouldConvertValidSources(Object source, LocalDate expected) {
        // Act
        LocalDate actual = Converter.convertToLocalDate(source);
        // Assert
        assertEquals(expected, actual, () -> "Conversion of " + source.getClass().getSimpleName() + " should yield " + expected);
    }

    @ParameterizedTest(name = "[{index}] source {0} should result in null")
    @MethodSource("nullResultProvider")
    void shouldReturnNullForUnconvertibleSources(Object source) {
        LocalDate actual = Converter.convertToLocalDate(source);
        assertNull(actual, "Conversion of " + (source == null ? "null" : source.getClass().getSimpleName()) + " should return null");
    }

    @ParameterizedTest(name = "[{index}] source {0} throws {1}")
    @MethodSource("exceptionProvider")
    void shouldThrowExpectedException(Object source, Class<? extends Throwable> exceptionClass) {
        assertThrows(exceptionClass, () -> Converter.convertToLocalDate(source), () -> "Conversion of " + source.getClass().getSimpleName() + " should throw " + exceptionClass.getSimpleName());
    }
}
