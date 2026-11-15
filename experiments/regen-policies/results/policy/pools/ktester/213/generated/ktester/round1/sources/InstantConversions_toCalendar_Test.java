package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.sql.Date;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.INSTANT;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toCalendar_Test {

    private static Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter can be used as‑is; it does not require any special configuration for the
        // conversions exercised in this test class.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – helps the GC in large test suites.
        converter = null;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        ZoneId systemZone = ZoneId.systemDefault();
        return Stream.of(// ---- basic supported types -------------------------------------------------
        Arguments.of("Long epoch millis", 123456789L, new Converter(new ConverterOptions()), 123456789L), Arguments.of("ISO‑8601 string", "2023-01-01T12:34:56Z", new Converter(new ConverterOptions()), Instant.parse("2023-01-01T12:34:56Z").toEpochMilli()), Arguments.of("java.util.Date at epoch", new Date(0L), new Converter(new ConverterOptions()), 0L), Arguments.of("LocalDateTime (system default zone)", LocalDateTime.of(2022, 12, 31, 23, 59, 59), new Converter(new ConverterOptions()), LocalDateTime.of(2022, 12, 31, 23, 59, 59).atZone(systemZone).toInstant().toEpochMilli()), // ---- additional supported types --------------------------------------------
        Arguments.of("Instant at epoch", Instant.ofEpochMilli(0L), new Converter(new ConverterOptions()), 0L), Arguments.of("java.sql.Timestamp", Timestamp.valueOf("1970-01-01 00:00:01"), new Converter(new ConverterOptions()), 1_000L), Arguments.of("java.sql.Date (midnight)", Date.valueOf("2020-02-29"), new Converter(new ConverterOptions()), LocalDate.of(2020, 2, 29).atStartOfDay(systemZone).toInstant().toEpochMilli()), Arguments.of("LocalDate (midnight)", LocalDate.of(2021, 5, 15), new Converter(new ConverterOptions()), LocalDate.of(2021, 5, 15).atStartOfDay(systemZone).toInstant().toEpochMilli()), Arguments.of("LocalTime on epoch date", LocalTime.of(13, 45, 30), new Converter(new ConverterOptions()), LocalDate.of(1970, 1, 1).atTime(13, 45, 30).atZone(systemZone).toInstant().toEpochMilli()), Arguments.of("ZonedDateTime (America/New_York)", ZonedDateTime.of(2022, 3, 10, 5, 6, 7, 0, ZoneId.of("America/New_York")), new Converter(new ConverterOptions()), ZonedDateTime.of(2022, 3, 10, 5, 6, 7, 0, ZoneId.of("America/New_York")).toInstant().toEpochMilli()), Arguments.of("OffsetDateTime (UTC‑5)", OffsetDateTime.of(2022, 3, 10, 5, 6, 7, 0, ZoneOffset.ofHours(-5)), new Converter(new ConverterOptions()), OffsetDateTime.of(2022, 3, 10, 5, 6, 7, 0, ZoneOffset.ofHours(-5)).toInstant().toEpochMilli()), Arguments.of("AtomicLong", new AtomicLong(987_654_321L), new Converter(new ConverterOptions()), 987_654_321L), Arguments.of("BigDecimal (whole number)", new BigDecimal("123456789"), new Converter(new ConverterOptions()), 123_456_789L), Arguments.of("BigInteger", new BigInteger("123456789"), new Converter(new ConverterOptions()), 123_456_789L), Arguments.of("Map containing INSTANT key", Map.of(INSTANT, Instant.ofEpochMilli(555_555_555L)), new Converter(new ConverterOptions()), 555_555_555L), // ---- edge‑case values -------------------------------------------------------
        Arguments.of("Long.MAX_VALUE (far future)", Long.MAX_VALUE, new Converter(new ConverterOptions()), Long.MAX_VALUE), Arguments.of("Negative epoch (-1 ms)", -1L, new Converter(new ConverterOptions()), -1L), Arguments.of("UTC time‑zone handling", 123_456_789L, new Converter(new ConverterOptions()).setTimeZone(TimeZone.getTimeZone("UTC")), 123_456_789L), Arguments.of("Leap‑second string (normalized)", "2023-02-28T23:59:60Z", new Converter(new ConverterOptions()), Instant.parse("2023-03-01T00:00:00Z").toEpochMilli()));
    }

    private static Stream<Arguments> provideExceptionConversions() {
        return Stream.of(// ---- null source (as per JSON expectation) ---------------------------------
        Arguments.of("Null source should throw NPE", null, new Converter(new ConverterOptions()), NullPointerException.class), // ---- unsupported object ----------------------------------------------------
        Arguments.of("Unsupported object type should throw ClassCastException", new Object(), new Converter(new ConverterOptions()), ClassCastException.class), // ---- invalid string format -------------------------------------------------
        Arguments.of("Non‑ISO‑8601 string should throw DateTimeParseException", "not-a-date", new Converter(new ConverterOptions()), DateTimeParseException.class), // ---- numeric overflow ------------------------------------------------------
        Arguments.of("String larger than Long.MAX_VALUE should throw NumberFormatException", "9223372036854775808", new Converter(new ConverterOptions()), NumberFormatException.class), Arguments.of("BigInteger exceeding long range should throw ArithmeticException", new BigInteger("9223372036854775808"), new Converter(new ConverterOptions()), ArithmeticException.class), // ---- non‑integral numeric -------------------------------------------------
        Arguments.of("BigDecimal with fractional part should throw ArithmeticException", new BigDecimal("12345.678"), new Converter(new ConverterOptions()), ArithmeticException.class), // ---- map conversion errors ------------------------------------------------
        Arguments.of("Map without INSTANT key should throw RuntimeException (ConversionException)", Map.of("someKey", Instant.now()), new Converter(new ConverterOptions()), RuntimeException.class), Arguments.of("Map with INSTANT key but null value should throw NPE", Map.of(INSTANT, null), new Converter(new ConverterOptions()), NullPointerException.class), // ---- null converter --------------------------------------------------------
        Arguments.of("Null converter should throw NPE", "123456789", null, NullPointerException.class));
    }

    @Test
    void testToCalendarFromInstant() {
        // GIVEN
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        long expectedMillis = instant.toEpochMilli();
        // WHEN
        Calendar calendar = InstantConversions.toCalendar(instant, converter);
        // THEN
        assertNotNull(calendar, "Calendar should not be null");
        assertEquals(expectedMillis, calendar.getTimeInMillis(), "Calendar time should match the Instant epoch milliseconds");
    }

    @Test
    void testToCalendarFromLong() {
        // GIVEN
        // corresponds to 2023‑01‑01T12:34:56Z
        long epochMillis = 1_672_571_696_000L;
        // WHEN
        Calendar calendar = InstantConversions.toCalendar(epochMillis, converter);
        // THEN
        assertNotNull(calendar);
        assertEquals(epochMillis, calendar.getTimeInMillis(), "Calendar should represent the supplied epoch milliseconds");
    }

    @Test
    void testToCalendarFromString() {
        // GIVEN
        // same instant as above
        String epochString = "1672571696000";
        long expectedMillis = Long.parseLong(epochString);
        // WHEN
        Calendar calendar = InstantConversions.toCalendar(epochString, converter);
        // THEN
        assertNotNull(calendar);
        assertEquals(expectedMillis, calendar.getTimeInMillis(), "String should be converted to the correct epoch milliseconds");
    }

    @Test
    void testToCalendarFromNull() {
        // GIVEN
        // a null source value – according to Converter contract this yields 0 for numeric primitives
        // WHEN
        Calendar calendar = InstantConversions.toCalendar(null, converter);
        // THEN
        assertNotNull(calendar);
        assertEquals(0L, calendar.getTimeInMillis(), "Null source should be converted to epoch 0 (1970‑01‑01T00:00:00Z)");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulConversions")
    void testToCalendarSuccessful(String description, Object from, Converter conv, long expectedEpochMillis) {
        Calendar cal = InstantConversions.toCalendar(from, conv);
        assertNotNull(cal, description);
        assertEquals(expectedEpochMillis, cal.getTimeInMillis(), description + " – epoch milliseconds must match");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionConversions")
    void testToCalendarExceptions(String description, Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> InstantConversions.toCalendar(from, conv), description);
    }

    @Test
    void testToCalendarFromStringWithLeapSecondNormalization() {
        // GIVEN
        String leapSecond = "2023-02-28T23:59:60Z";
        long expected = Instant.parse("2023-03-01T00:00:00Z").toEpochMilli();
        // WHEN
        Calendar cal = InstantConversions.toCalendar(leapSecond, converter);
        // THEN
        assertEquals(expected, cal.getTimeInMillis(), "Leap‑second should be normalized to the next second");
    }

    @Test
    void testToCalendarUsesConverterTimeZoneWhenConfigured() {
        // GIVEN
        Converter utcConverter = new Converter(new ConverterOptions()).setTimeZone(TimeZone.getTimeZone("UTC"));
        long epoch = 123_456_789L;
        // WHEN
        Calendar cal = InstantConversions.toCalendar(epoch, utcConverter);
        // THEN
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone(), "Calendar should inherit the UTC time‑zone from the converter");
        assertEquals(epoch, cal.getTimeInMillis(), "Epoch milliseconds must be unchanged");
    }
}
