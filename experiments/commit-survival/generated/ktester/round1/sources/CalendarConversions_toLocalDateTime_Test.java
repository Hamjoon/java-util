package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_toLocalDateTime_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a real Converter with default options.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validZonedDateTimeCases() {
        return Stream.of(// ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, ZoneId.of("UTC")), null, LocalDateTime.of(2023, 5, 15, 10, 30), "Direct ZonedDateTime should be converted to the same LocalDateTime"), // OffsetDateTime with -05:00 offset → UTC conversion
        Arguments.of(OffsetDateTime.of(2022, 12, 31, 23, 45, 0, 0, ZoneOffset.ofHours(-5)), null, LocalDateTime.of(2023, 1, 1, 4, 45), "OffsetDateTime is first converted to UTC then to LocalDateTime"), // Instant (UTC)
        Arguments.of(Instant.parse("2021-07-20T15:20:30Z"), null, LocalDateTime.of(2021, 7, 20, 15, 20, 30), "Instant interpreted as UTC"), // java.util.Date (UTC instant)
        Arguments.of(Date.from(Instant.parse("2020-01-01T00:00:00Z")), null, LocalDateTime.of(2020, 1, 1, 0, 0), "java.util.Date should be converted to UTC LocalDateTime"), // java.sql.Timestamp (nanosecond precision)
        Arguments.of(new Timestamp(Instant.parse("2019-03-10T12:34:56.789Z").toEpochMilli()), null, LocalDateTime.of(2019, 3, 10, 12, 34, 56, 789_000_000), "Timestamp conversion preserves nanoseconds"), // Calendar with America/New_York zone
        Arguments.of(Calendar.getInstance(TimeZone.getTimeZone("America/New_York")), null, // Compute expected value dynamically because the calendar's time is now
        // We use the same conversion logic as the implementation.
        LocalDateTime.ofInstant(((Calendar) Calendar.getInstance(TimeZone.getTimeZone("America/New_York"))).toInstant(), ZoneOffset.UTC).toLocalDateTime(), "Calendar instance with a specific time‑zone is converted via toInstant()"), // LocalDateTime (identity)
        Arguments.of(LocalDateTime.of(2022, 8, 5, 14, 0, 0), null, LocalDateTime.of(2022, 8, 5, 14, 0), "LocalDateTime is returned unchanged"), // LocalDate (start‑of‑day)
        Arguments.of(LocalDate.of(2021, 11, 30), null, LocalDateTime.of(2021, 11, 30, 0, 0), "LocalDate is treated as start‑of‑day"), // LocalTime (combined with current date)
        Arguments.of(LocalTime.of(9, 15, 30), null, LocalDateTime.of(LocalDate.now(), LocalTime.of(9, 15, 30)), "LocalTime combined with today's date"), // Long epoch‑millis
        Arguments.of(1_234_567_890_123L, null, LocalDateTime.ofInstant(Instant.ofEpochMilli(1_234_567_890_123L), ZoneOffset.UTC).toLocalDateTime(), "Long epoch‑millis conversion"), // String parsed via a real Converter
        Arguments.of("2024-02-29T12:00:00", new Converter(), LocalDateTime.of(2024, 2, 29, 12, 0), "String parsed via provided Converter"));
    }

    private static Stream<Arguments> numericCases() {
        return Stream.of(// Integer epoch‑millis
        Arguments.of(Integer.valueOf(123_456_789), LocalDateTime.ofInstant(Instant.ofEpochMilli(123_456_789L), ZoneOffset.UTC).toLocalDateTime(), "Integer epoch‑millis treated as Long"), // Double epoch‑millis (truncated)
        Arguments.of(Double.valueOf(12_345.678), LocalDateTime.ofInstant(Instant.ofEpochMilli(12_345L), ZoneOffset.UTC).toLocalDateTime(), "Double epoch‑millis truncated to long"), // BigDecimal epoch‑millis
        Arguments.of(new BigDecimal("1609459200123"), LocalDateTime.ofInstant(Instant.ofEpochMilli(1_609_459_200_123L), ZoneOffset.UTC).toLocalDateTime(), "BigDecimal epoch‑millis conversion"), // BigInteger epoch‑millis
        Arguments.of(new BigInteger("1609459200123"), LocalDateTime.ofInstant(Instant.ofEpochMilli(1_609_459_200_123L), ZoneOffset.UTC).toLocalDateTime(), "BigInteger epoch‑millis conversion"), // AtomicLong epoch‑millis
        Arguments.of(new AtomicLong(1_609_459_200_123L), LocalDateTime.ofInstant(Instant.ofEpochMilli(1_609_459_200_123L), ZoneOffset.UTC).toLocalDateTime(), "AtomicLong epoch‑millis conversion"));
    }

    private static Stream<Arguments> otherDateCases() {
        return Stream.of(// java.sql.Date (date only)
        Arguments.of(java.sql.Date.valueOf("2022-10-15"), LocalDateTime.of(2022, 10, 15, 0, 0), "java.sql.Date → start‑of‑day LocalDateTime"), // ZonedDateTime with Asia/Kolkata (+05:30) → UTC conversion
        Arguments.of(ZonedDateTime.of(2023, 3, 10, 15, 0, 0, 0, ZoneId.of("Asia/Kolkata")), LocalDateTime.of(2023, 3, 10, 9, 30), "ZonedDateTime with non‑UTC zone normalised to UTC"), // OffsetDateTime with +05:45 offset crossing day boundary
        Arguments.of(OffsetDateTime.of(2022, 12, 31, 22, 30, 0, 0, ZoneOffset.ofHoursMinutes(5, 45)), LocalDateTime.of(2022, 12, 31, 16, 45), "OffsetDateTime +05:45 → UTC crossing day boundary"), // Calendar in UTC
        Arguments.of(Calendar.getInstance(TimeZone.getTimeZone("UTC")), LocalDateTime.ofInstant(((Calendar) Calendar.getInstance(TimeZone.getTimeZone("UTC"))).toInstant(), ZoneOffset.UTC).toLocalDateTime(), "Calendar in UTC zone"));
    }

    @Test
    void toLocalDateTime_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5));
        LocalDateTime expected = zdt.toLocalDateTime();
        LocalDateTime actual = CalendarConversions.toLocalDateTime(zdt, converter);
        assertEquals(expected, actual, "LocalDateTime should be extracted from ZonedDateTime unchanged");
    }

    @Test
    void toLocalDateTime_fromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(2));
        LocalDateTime expected = odt.toLocalDateTime();
        LocalDateTime actual = CalendarConversions.toLocalDateTime(odt, converter);
        assertEquals(expected, actual, "LocalDateTime should be extracted from OffsetDateTime unchanged");
    }

    @Test
    void toLocalDateTime_fromInstant() {
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        // CalendarConversions likely converts an Instant to a ZonedDateTime in UTC.
        LocalDateTime expected = ZonedDateTime.ofInstant(instant, ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime actual = CalendarConversions.toLocalDateTime(instant, converter);
        assertEquals(expected, actual, "Instant should be converted to UTC LocalDateTime");
    }

    @Test
    void toLocalDateTime_fromDate() {
        Date date = Date.from(Instant.parse("2023-03-15T08:15:30Z"));
        // Expected conversion mirrors the Instant case (UTC).
        LocalDateTime expected = ZonedDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC).toLocalDateTime();
        LocalDateTime actual = CalendarConversions.toLocalDateTime(date, converter);
        assertEquals(expected, actual, "java.util.Date should be converted to UTC LocalDateTime");
    }

    @Test
    void toLocalDateTime_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2021, 7, 20, 14, 45, 30);
        // When the source is already a LocalDateTime, the conversion should be identity.
        LocalDateTime actual = CalendarConversions.toLocalDateTime(ldt, converter);
        assertEquals(ldt, actual, "LocalDateTime input should be returned unchanged");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validZonedDateTimeCases")
    void testValidConversions(Object from, Converter conv, LocalDateTime expected, String description) {
        // Use the supplied converter (may be null) or fall back to the field instance.
        Converter used = conv != null ? conv : converter;
        LocalDateTime actual = CalendarConversions.toLocalDateTime(from, used);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("numericCases")
    void testNumericConversions(Object from, LocalDateTime expected, String description) {
        LocalDateTime actual = CalendarConversions.toLocalDateTime(from, converter);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("otherDateCases")
    void testOtherDateTypes(Object from, LocalDateTime expected, String description) {
        LocalDateTime actual = CalendarConversions.toLocalDateTime(from, converter);
        assertEquals(expected, actual, description);
    }

    @Test
    void testNullSourceThrows() {
        assertThrows(IllegalArgumentException.class, () -> CalendarConversions.toLocalDateTime(null, null), "Null source should cause IllegalArgumentException");
    }

    @Test
    void testUnsupportedBooleanThrows() {
        assertThrows(ConversionException.class, () -> CalendarConversions.toLocalDateTime(true, null), "Unsupported Boolean should trigger ConversionException");
    }

    @Test
    void testMalformedStringThrows() {
        Converter conv = new Converter();
        assertThrows(DateTimeParseException.class, () -> CalendarConversions.toLocalDateTime("not-a-date", conv), "Malformed date string should cause DateTimeParseException");
    }

    @Test
    void testLongMaxValueThrows() {
        assertThrows(DateTimeException.class, () -> CalendarConversions.toLocalDateTime(Long.MAX_VALUE, null), "Epoch‑millis > Instant range should throw DateTimeException");
    }

    @Test
    void testLongMinValueThrows() {
        assertThrows(DateTimeException.class, () -> CalendarConversions.toLocalDateTime(Long.MIN_VALUE, null), "Epoch‑millis < Instant range should throw DateTimeException");
    }

    @Test
    void testUnsupportedListThrows() {
        assertThrows(ConversionException.class, () -> CalendarConversions.toLocalDateTime(Collections.emptyList(), null), "List is not a recognised date/time source");
    }

    @Test
    void testOffsetTimeThrows() {
        assertThrows(ConversionException.class, () -> CalendarConversions.toLocalDateTime(OffsetTime.of(13, 45, 30, 0, ZoneOffset.UTC), null), "OffsetTime should trigger ConversionException");
    }

    @Test
    void testYearMonthThrows() {
        assertThrows(ConversionException.class, () -> CalendarConversions.toLocalDateTime(YearMonth.of(2023, 5), null), "YearMonth cannot be converted to LocalDateTime");
    }

    @Test
    void testZoneIdThrows() {
        assertThrows(ConversionException.class, () -> CalendarConversions.toLocalDateTime(ZoneId.of("Europe/Paris"), null), "ZoneId is unrelated to a date/time value");
    }

    @Test
    void testZoneOffsetThrows() {
        assertThrows(ConversionException.class, () -> CalendarConversions.toLocalDateTime(ZoneOffset.ofHours(-4), null), "ZoneOffset alone cannot be converted to a LocalDateTime");
    }
}
