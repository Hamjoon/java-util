package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toLocalTime_Test {

    /**
     * Converter instance used for all tests.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a Converter with default options.
        // ConverterOptions has a public no‑arg constructor in the library.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    static Stream<Arguments> validConversionProvider() {
        return Stream.of(// LocalTime – should be returned unchanged
        Arguments.of(LocalTime.of(13, 45, 30), LocalTime.of(13, 45, 30)), // LocalDateTime – system default zone
        Arguments.of(LocalDateTime.of(2023, 5, 10, 9, 15, 0), LocalDateTime.of(2023, 5, 10, 9, 15, 0).atZone(ZoneId.systemDefault()).toLocalTime()), // java.util.Date – UTC is used internally for Date → ZonedDateTime in the library
        Arguments.of(Date.from(Instant.parse("2023-07-01T22:30:15Z")), LocalTime.of(22, 30, 15)), // java.sql.Timestamp – behaves like java.util.Date
        Arguments.of(Timestamp.valueOf("2022-12-31 23:59:59"), LocalTime.of(23, 59, 59)), // Instant – UTC conversion
        Arguments.of(Instant.parse("2021-01-01T05:00:00Z"), LocalTime.of(5, 0, 0)), // Calendar with America/New_York zone
        Arguments.of(createCalendar("America/New_York", 12, 34, 56), LocalTime.of(12, 34, 56)), // Long epoch‑millis
        Arguments.of(1234567890123L, LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567890123L), ZoneId.systemDefault()).toLocalTime()), // ISO‑local‑time string
        Arguments.of("15:20:45", LocalTime.of(15, 20, 45)), // ZonedDateTime with non‑default zone
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneId.of("Europe/Paris")), LocalTime.of(14, 30, 0)), // OffsetTime – offset ignored
        Arguments.of(OffsetTime.of(11, 22, 33, 0, ZoneOffset.ofHours(-5)), LocalTime.of(11, 22, 33)), // GregorianCalendar from a ZonedDateTime (America/New_York)
        Arguments.of(GregorianCalendar.from(ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("America/New_York"))), LocalTime.of(23, 59, 59)), // AtomicLong – epoch‑millis
        Arguments.of(new AtomicLong(1234567890123L), LocalDateTime.ofInstant(Instant.ofEpochMilli(1234567890123L), ZoneId.systemDefault()).toLocalTime()), // String with nanosecond fraction
        Arguments.of("13:45:30.123456789", LocalTime.of(13, 45, 30, 123_456_789)), // OffsetTime expressed as a string (offset ignored)
        Arguments.of("10:15:30+02:00", LocalTime.of(10, 15, 30)), // BigInteger – epoch‑seconds
        Arguments.of(BigInteger.valueOf(1609459200L), LocalDateTime.ofInstant(Instant.ofEpochSecond(1609459200L), ZoneId.systemDefault()).toLocalTime()), // OffsetDateTime – offset ignored
        Arguments.of(OffsetDateTime.parse("2023-03-15T12:34:56+02:00"), LocalTime.of(12, 34, 56)), // ZonedDateTime already covered above (duplicate for clarity)
        Arguments.of(ZonedDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneId.of("Europe/Paris")), LocalTime.of(14, 30, 0)));
    }

    static Stream<Arguments> nullAndErrorProvider() {
        return Stream.of(// null source
        Arguments.of(null, NullPointerException.class), // unsupported plain Object
        Arguments.of(new Object(), IllegalArgumentException.class), // malformed time string
        Arguments.of("not-a-time", DateTimeParseException.class), // LocalDate – no time component
        Arguments.of(LocalDate.of(2023, 1, 1), DateTimeException.class), // java.sql.Date – date only
        Arguments.of(java.sql.Date.valueOf("2023-04-01"), DateTimeException.class), // YearMonth – unsupported
        Arguments.of(YearMonth.of(2023, 5), IllegalArgumentException.class), // Invalid month in string
        Arguments.of("2023-13-01T10:00:00", DateTimeParseException.class), // Empty string
        Arguments.of("", DateTimeParseException.class), // ZoneId – not a temporal value
        Arguments.of(ZoneId.of("UTC"), IllegalArgumentException.class), // Duration – unsupported
        Arguments.of(Duration.ofHours(5), IllegalArgumentException.class), // AtomicInteger – unsupported numeric wrapper
        Arguments.of(new java.util.concurrent.atomic.AtomicInteger(123), IllegalArgumentException.class), // Date‑only string
        Arguments.of("2023-01-01", DateTimeParseException.class), // Too many fractional digits
        Arguments.of("10:15:30.1234567890", DateTimeParseException.class));
    }

    /**
     * Creates a {@link Calendar} instance in the supplied time‑zone with the given
     * hour, minute and second values. Milliseconds are set to zero.
     */
    private static Calendar createCalendar(String zoneId, int hour, int minute, int second) {
        TimeZone tz = TimeZone.getTimeZone(zoneId);
        Calendar cal = Calendar.getInstance(tz);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void testToLocalTimeFromZonedDateTime() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 14, 25, 30, 0, ZoneOffset.UTC);
        LocalTime expected = zdt.toLocalTime();
        // WHEN
        LocalTime actual = CalendarConversions.toLocalTime(zdt, converter);
        // THEN
        assertEquals(expected, actual, "LocalTime extracted from a ZonedDateTime should match the original time component");
    }

    @Test
    void testToLocalTimeFromDate() {
        // GIVEN
        Instant instant = Instant.parse("2023-05-10T14:25:30Z");
        Date date = Date.from(instant);
        // CalendarConversions uses the system default zone when converting a Date to a ZonedDateTime.
        ZoneId systemZone = ZoneId.systemDefault();
        LocalTime expected = instant.atZone(systemZone).toLocalTime();
        // WHEN
        LocalTime actual = CalendarConversions.toLocalTime(date, converter);
        // THEN
        assertEquals(expected, actual, "LocalTime derived from a java.util.Date should reflect the system default time‑zone");
    }

    @Test
    void testToLocalTimeFromInstant() {
        // GIVEN
        Instant instant = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ZoneId systemZone = ZoneId.systemDefault();
        LocalTime expected = instant.atZone(systemZone).toLocalTime();
        // WHEN
        LocalTime actual = CalendarConversions.toLocalTime(instant, converter);
        // THEN
        assertEquals(expected, actual, "LocalTime derived from an Instant should match the instant converted to the system default zone");
    }

    @Test
    void testToLocalTimeFromLocalDateTime() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, 12, 31, 23, 59, 59);
        // The conversion will treat the LocalDateTime as being in the system default zone.
        ZoneId systemZone = ZoneId.systemDefault();
        LocalTime expected = ldt.atZone(systemZone).toLocalTime();
        // WHEN
        LocalTime actual = CalendarConversions.toLocalTime(ldt, converter);
        // THEN
        assertEquals(expected, actual, "LocalTime extracted from a LocalDateTime should be based on the system default zone");
    }

    /**
     * --------------------------------------------------------------------
     *  Valid conversions – types handled by toZonedDateTime
     * --------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validConversionProvider")
    void testValidConversions(Object from, LocalTime expected) {
        LocalTime actual = CalendarConversions.toLocalTime(from, converter);
        assertEquals(expected, actual);
    }

    /**
     * --------------------------------------------------------------------
     *  Null and error handling
     * --------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nullAndErrorProvider")
    void testNullAndErrorHandling(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> CalendarConversions.toLocalTime(from, converter));
    }

    /**
     * --------------------------------------------------------------------
     *  Conversion using a custom Converter implementation
     * --------------------------------------------------------------------
     */
    @Test
    void testCustomConverterWithBigDecimal() {
        // Custom converter that treats numeric values as epoch seconds.
        Converter custom = mock(Converter.class);
        BigDecimal epochSeconds = BigDecimal.valueOf(1609459200L);
        // When the library asks the converter to convert the BigDecimal to an Instant,
        // we return the appropriate Instant.
        when(custom.convert(eq(epochSeconds), eq(Instant.class))).thenReturn(Instant.ofEpochSecond(1609459200L));
        // The toLocalTime method will eventually call toZonedDateTime which will use the
        // converter to obtain an Instant (or ZonedDateTime). By stubbing the convert call
        // we can control the outcome.
        LocalTime expected = LocalDateTime.ofInstant(Instant.ofEpochSecond(1609459200L), ZoneId.systemDefault()).toLocalTime();
        LocalTime actual = CalendarConversions.toLocalTime(epochSeconds, custom);
        assertEquals(expected, actual);
    }

    @Test
    void testCustomConverterWithOffsetDateTimeString() {
        // Custom converter that parses ISO‑8601 date‑time strings to OffsetDateTime.
        Converter custom = mock(Converter.class);
        String isoString = "2023-08-01T10:15:30+01:00";
        OffsetDateTime odt = OffsetDateTime.parse(isoString);
        when(custom.convert(eq(isoString), eq(OffsetDateTime.class))).thenReturn(odt);
        LocalTime expected = LocalTime.of(10, 15, 30);
        LocalTime actual = CalendarConversions.toLocalTime(isoString, custom);
        assertEquals(expected, actual);
    }
}
