package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toLong_Test {

    /**
     * The Converter instance used for all tests.
     */
    private Converter converter;

    /**
     * A concrete {@link Converter} that simply forwards to the real implementation.
     */
    private static class DummyConverter extends Converter {

        DummyConverter() {
            super(new ConverterOptions());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter can be created with default options – no overrides are needed.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Parameter source for all successful conversions.
     *
     * @return a stream of arguments: {@code from}, {@code expectedEpochMillis}, {@code description}
     */
    private static Stream<Arguments> successfulConversions() {
        // Helper objects reused across several cases
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(1672531200000L);
        Calendar pstCal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        pstCal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        pstCal.set(Calendar.MILLISECOND, 0);
        return Stream.of(// Instant
        Arguments.of(Instant.ofEpochMilli(1672531200000L), 1672531200000L, "Instant should yield its epoch‑millis directly"), // java.util.Date
        Arguments.of(new Date(1672531200000L), 1672531200000L, "Date is converted via Date#toInstant()"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2023-01-01 00:00:00"), 1672531200000L, "Timestamp behaves like a Date"), // Calendar (UTC)
        Arguments.of(utcCal, 1672531200000L, "UTC Calendar should return its timeInMillis"), // Long value
        Arguments.of(Long.valueOf(1672531200000L), 1672531200000L, "Long is interpreted as epoch‑millis"), // ISO‑8601 string with Z
        Arguments.of("2023-01-01T00:00:00Z", 1672531200000L, "ISO‑8601 Z‑designated string"), // LocalDateTime (treated as UTC)
        Arguments.of(LocalDateTime.of(2023, 1, 1, 0, 0, 0), 1672531200000L, "LocalDateTime assumed to be UTC"), // LocalDate (start‑of‑day UTC)
        Arguments.of(LocalDate.of(2023, 1, 1), 1672531200000L, "LocalDate interpreted as start‑of‑day UTC"), // LocalTime (midnight on epoch day)
        Arguments.of(LocalTime.of(0, 0, 0), 0L, "LocalTime midnight on 1970‑01‑01 yields 0"), // ZonedDateTime UTC
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 1672531200000L, "ZonedDateTime with UTC zone"), // OffsetDateTime UTC
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), 1672531200000L, "OffsetDateTime with UTC offset"), // Integer (treated as epoch‑millis)
        Arguments.of(Integer.valueOf(1672531200), 1672531200L, "Integer interpreted directly as epoch‑millis"), // Short
        Arguments.of(Short.valueOf((short) 12345), 12345L, "Short interpreted directly as epoch‑millis"), // BigDecimal
        Arguments.of(new BigDecimal("1672531200000"), 1672531200000L, "BigDecimal containing epoch‑millis"), // BigInteger
        Arguments.of(new BigInteger("1672531200000"), 1672531200000L, "BigInteger containing epoch‑millis"), // AtomicLong
        Arguments.of(new AtomicLong(1672531200000L), 1672531200000L, "AtomicLong unwrapped to epoch‑millis"), // ISO‑8601 string with positive offset (+02:00)
        Arguments.of("2023-01-01T02:00:00+02:00", 1672531200000L, "String with +02:00 offset resolves to same instant as UTC"), // ISO‑8601 string without zone (treated as UTC)
        Arguments.of("2023-01-01T00:00:00", 1672531200000L, "Zone‑less ISO string interpreted using default UTC"), // Calendar PST (UTC‑8)
        Arguments.of(pstCal, 1672550400000L, "PST Calendar 2023‑01‑01 00:00 corresponds to 2023‑01‑01T08:00Z"), // java.sql.Date (midnight UTC)
        Arguments.of(java.sql.Date.valueOf("2023-01-01"), 1672531200000L, "java.sql.Date represents start‑of‑day UTC"), // LocalTime 12:34:56 on epoch day
        Arguments.of(LocalTime.of(12, 34, 56), 45296000L, "LocalTime converted to millis since epoch day start"), // Negative Long (one day before epoch)
        Arguments.of(Long.valueOf(-86400000L), -86400000L, "Negative epoch‑millis should be returned unchanged"));
    }

    /**
     * Parameter source for conversion failures.
     *
     * @return a stream of arguments: {@code from}, {@code expectedException}, {@code description}
     */
    private static Stream<Arguments> failureConversions() {
        return Stream.of(// null
        Arguments.of(null, ConversionException.class, "null should trigger a conversion exception"), // non‑parseable string
        Arguments.of("not-a-date", ConversionException.class, "Invalid date string must raise a conversion exception"), // Empty string
        Arguments.of("", ConversionException.class, "Empty string cannot be parsed"), // Unsupported format
        Arguments.of("2023/01/01", ConversionException.class, "Unsupported date format should raise a conversion exception"), // Numeric string (not ISO‑8601)
        Arguments.of("1672531200000", ConversionException.class, "Pure numeric string is not a supported ISO‑8601 format"), // Boolean
        Arguments.of(Boolean.TRUE, ConversionException.class, "Boolean is an unsupported source type"), // Arbitrary Object
        Arguments.of(new Object(), ConversionException.class, "Random object cannot be converted"), // Double (floating‑point)
        Arguments.of(Double.valueOf(1672531200000.0), ConversionException.class, "Floating‑point numbers are not accepted as epoch‑millis"), // Instant.MAX – out of range for epoch‑millis
        Arguments.of(Instant.MAX, DateTimeException.class, "Instant.MAX should cause DateTimeException"), // Instant.MIN – out of range for epoch‑millis
        Arguments.of(Instant.MIN, DateTimeException.class, "Instant.MIN should cause DateTimeException"));
    }

    @Test
    void toLongFromInstant() {
        // GIVEN
        Instant now = Instant.now();
        // WHEN
        long result = LocalDateTimeConversions.toLong(now, converter);
        // THEN
        assertEquals(now.toEpochMilli(), result, "toLong should return the epoch‑millis of the supplied Instant");
    }

    @Test
    void toLongFromDate() {
        // GIVEN
        // current time in millis
        Date date = new Date();
        // WHEN
        long result = LocalDateTimeConversions.toLong(date, converter);
        // THEN
        assertEquals(date.getTime(), result, "toLong should return the same millis as Date#getTime()");
    }

    @Test
    void toLongFromTimestamp() {
        // GIVEN
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        // WHEN
        long result = LocalDateTimeConversions.toLong(ts, converter);
        // THEN
        assertEquals(ts.getTime(), result, "toLong should return the same millis as Timestamp#getTime()");
    }

    @Test
    void toLongFromLongValue() {
        // GIVEN
        // an arbitrary epoch‑millis value
        long epochMillis = 1_600_000_000_000L;
        // WHEN
        long result = LocalDateTimeConversions.toLong(epochMillis, converter);
        // THEN
        assertEquals(epochMillis, result, "toLong should return the same value when a Long (epoch millis) is supplied");
    }

    @Test
    void toLongFromLocalDateTimeAssumesUTC() {
        // GIVEN
        // LocalDateTime does not contain zone information; the conversion logic in
        // LocalDateTimeConversions treats it as UTC (the most common behaviour in
        // this library). We construct the expected epoch millis accordingly.
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 12, 30, 45, 123_000_000);
        long expected = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        // WHEN
        long result = LocalDateTimeConversions.toLong(ldt, converter);
        // THEN
        assertEquals(expected, result, "toLong should convert LocalDateTime to epoch‑millis assuming UTC");
    }

    @Test
    void toLongFromCalendar() {
        // GIVEN
        Calendar cal = Calendar.getInstance();
        // set a known epoch millis
        cal.setTimeInMillis(1_610_000_000_000L);
        // WHEN
        long result = LocalDateTimeConversions.toLong(cal, converter);
        // THEN
        assertEquals(cal.getTimeInMillis(), result, "toLong should return the same millis as Calendar#getTimeInMillis()");
    }

    @Test
    void toLongFromUnsupportedTypeThrowsException() {
        // GIVEN
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> LocalDateTimeConversions.toLong(unsupported, converter), "Conversion of an unsupported type should result in a RuntimeException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversions")
    void toLong_successful(Object from, long expected, String description) {
        // WHEN
        long result = LocalDateTimeConversions.toLong(from, converter);
        // THEN
        assertEquals(expected, result, () -> "Failed conversion for: " + description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("failureConversions")
    void toLong_failure(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> LocalDateTimeConversions.toLong(from, converter), () -> "Expected exception for: " + description);
    }
}
