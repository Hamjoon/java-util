package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToZonedDateTime_Test {

    private static Stream<Arguments> validConversionProvider() {
        ZoneId systemZone = ZoneId.systemDefault();
        return Stream.of(// Already a ZonedDateTime – should return the same instance (or an equal one)
        Arguments.of(ZonedDateTime.parse("2023-05-01T12:30:45+02:00[Europe/Paris]"), ZonedDateTime.of(2023, 5, 1, 12, 30, 45, 0, ZoneId.of("Europe/Paris")), "Input is already a ZonedDateTime; method should return the same instance (or an equal one)."), // java.util.Date
        Arguments.of(Date.from(Instant.parse("2023-05-01T10:30:45Z")), ZonedDateTime.ofInstant(Instant.parse("2023-05-01T10:30:45Z"), systemZone), "Convert a java.util.Date (which wraps an Instant) to ZonedDateTime using the system default zone."), // Instant
        Arguments.of(Instant.parse("2023-05-01T10:30:45Z"), ZonedDateTime.ofInstant(Instant.parse("2023-05-01T10:30:45Z"), systemZone), "Convert an Instant to ZonedDateTime using the system default zone."), // LocalDateTime (no zone)
        Arguments.of(LocalDateTime.of(2023, 5, 1, 12, 30, 45), ZonedDateTime.of(2023, 5, 1, 12, 30, 45, 0, systemZone), "Convert a LocalDateTime (no zone) to ZonedDateTime by attaching the system default zone."), // LocalDate (date only)
        Arguments.of(LocalDate.of(2023, 5, 1), ZonedDateTime.of(2023, 5, 1, 0, 0, 0, 0, systemZone), "Convert a LocalDate to ZonedDateTime at start‑of‑day in the system default zone."), // java.sql.Timestamp (subclass of Date)
        Arguments.of(Timestamp.valueOf("2023-05-01 12:30:45"), ZonedDateTime.of(2023, 5, 1, 12, 30, 45, 0, systemZone), "Convert a java.sql.Timestamp to ZonedDateTime."), // ISO‑8601 string with explicit zone
        Arguments.of("2023-05-01T12:30:45+02:00[Europe/Paris]", ZonedDateTime.parse("2023-05-01T12:30:45+02:00[Europe/Paris]"), "Convert an ISO‑8601 formatted String containing a zone to ZonedDateTime."), // ISO‑8601 instant string (no explicit zone)
        Arguments.of("2023-05-01T10:30:45Z", ZonedDateTime.ofInstant(Instant.parse("2023-05-01T10:30:45Z"), systemZone), "Convert an ISO‑8601 instant String (no explicit zone) to ZonedDateTime using system default zone."), // OffsetDateTime
        Arguments.of(OffsetDateTime.parse("2023-05-01T12:30:45+02:00"), OffsetDateTime.parse("2023-05-01T12:30:45+02:00").toZonedDateTime(), "Convert an OffsetDateTime to ZonedDateTime, preserving the offset as a zone."), // OffsetTime (time‑only) – combine with today’s date
        Arguments.of(OffsetTime.parse("12:30:45+02:00"), OffsetTime.parse("12:30:45+02:00").atDate(LocalDate.now()).atZone(systemZone), "Convert an OffsetTime to ZonedDateTime by combining with today’s date and system default zone."), // GregorianCalendar with specific zone
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("Europe/Paris")) {

            {
                set(2023, Calendar.MAY, 1, 12, 30, 45);
                set(Calendar.MILLISECOND, 0);
            }
        }, ZonedDateTime.of(2023, 5, 1, 12, 30, 45, 0, ZoneId.of("Europe/Paris")), "GregorianCalendar with a specific TimeZone should be converted preserving fields and zone."), // java.sql.Date (date‑only)
        Arguments.of(java.sql.Date.valueOf("2023-05-01"), ZonedDateTime.of(2023, 5, 1, 0, 0, 0, 0, systemZone), "java.sql.Date represents a date without time; conversion should produce start‑of‑day ZonedDateTime."), // CharBuffer containing ISO‑8601 string
        Arguments.of(CharBuffer.wrap("2023-05-01T12:30:45+02:00[Europe/Paris]"), ZonedDateTime.parse("2023-05-01T12:30:45+02:00[Europe/Paris]"), "CharBuffer with an ISO‑8601 ZonedDateTime string should be parsed correctly."), // ByteBuffer containing UTF‑8 bytes of ISO‑8601 string
        Arguments.of(ByteBuffer.wrap("2023-05-01T12:30:45+02:00[Europe/Paris]".getBytes(StandardCharsets.UTF_8)), ZonedDateTime.parse("2023-05-01T12:30:45+02:00[Europe/Paris]"), "ByteBuffer with UTF‑8 bytes of an ISO‑8601 ZonedDateTime string should be parsed correctly."), // Epoch instant
        Arguments.of(Instant.EPOCH, ZonedDateTime.ofInstant(Instant.EPOCH, systemZone), "Converting the epoch instant should yield a ZonedDateTime at 1970‑01‑01T00:00Z in the system default zone."), // Far‑future LocalDateTime
        Arguments.of(LocalDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999), ZonedDateTime.of(9999, 12, 31, 23, 59, 59, 999_999_999, systemZone), "A far‑future LocalDateTime should be converted preserving nanosecond precision."), // OffsetTime with negative offset
        Arguments.of(OffsetTime.parse("12:30:45-05:00"), OffsetTime.parse("12:30:45-05:00").atDate(LocalDate.now()).atZone(systemZone), "OffsetTime with a negative offset should be combined with today’s date and system default zone."));
    }

    private static Stream<Arguments> unsupportedSourceProvider() {
        return Stream.of(Arguments.of(123, "Integer is not a supported source type."), Arguments.of(true, "Boolean is not a supported source type."), Arguments.of(UUID.randomUUID(), "UUID is not a supported source type."), Arguments.of(new Object(), "Generic Object with no conversion rule should be rejected."), Arguments.of(List.of(1, 2, 3), "Collection type is not supported for ZonedDateTime conversion."), Arguments.of(Map.of("key", "value"), "Map type is unsupported for ZonedDateTime conversion."));
    }

    private static Stream<Arguments> malformedStringProvider() {
        return Stream.of(Arguments.of("2023-13-01T25:61:00Z", "String with invalid month/day/hour/minute should cause DateTimeParseException."), Arguments.of("", "Empty string cannot be parsed and should cause DateTimeParseException."));
    }

    @Test
    void givenZonedDateTime_returnsSameInstance() {
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime result = Converter.convertToZonedDateTime(now);
        assertSame(now, result, "When the source is already a ZonedDateTime the same instance should be returned");
    }

    @Test
    void givenStringWithISOFormat_isParsedCorrectly() {
        String iso = "2023-08-15T12:34:56+02:00[Europe/Paris]";
        ZonedDateTime expected = ZonedDateTime.parse(iso);
        ZonedDateTime actual = Converter.convertToZonedDateTime(iso);
        assertEquals(expected, actual, "ISO‑8601 string should be converted to the correct ZonedDateTime");
    }

    @Test
    void givenDate_isConvertedUsingSystemDefaultZone() {
        // java.util.Date stores an instant in UTC.
        Date date = Date.from(Instant.parse("2023-01-01T00:00:00Z"));
        ZonedDateTime expected = date.toInstant().atZone(ZoneId.systemDefault());
        ZonedDateTime actual = Converter.convertToZonedDateTime(date);
        assertEquals(expected, actual, "Date should be converted to ZonedDateTime using the system default zone");
    }

    @Test
    void givenInstant_isConvertedToSystemDefaultZone() {
        Instant instant = Instant.parse("2022-12-31T23:59:59Z");
        ZonedDateTime expected = instant.atZone(ZoneId.systemDefault());
        ZonedDateTime actual = Converter.convertToZonedDateTime(instant);
        assertEquals(expected, actual, "Instant should be converted to ZonedDateTime using the system default zone");
    }

    @Test
    void givenLocalDateTime_isConvertedUsingSystemDefaultZone() {
        LocalDateTime ldt = LocalDateTime.of(2021, Month.MARCH, 10, 15, 30, 45);
        ZonedDateTime expected = ldt.atZone(ZoneId.systemDefault());
        ZonedDateTime actual = Converter.convertToZonedDateTime(ldt);
        assertEquals(expected, actual, "LocalDateTime should be converted to ZonedDateTime using the system default zone");
    }

    @Test
    void givenCalendar_isConvertedToCorrespondingZonedDateTime() {
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("America/New_York"));
        cal.set(2020, Calendar.JULY, 4, 9, 15, 30);
        cal.set(Calendar.MILLISECOND, 0);
        ZonedDateTime expected = ZonedDateTime.ofInstant(cal.toInstant(), cal.getTimeZone().toZoneId());
        ZonedDateTime actual = Converter.convertToZonedDateTime(cal);
        assertEquals(expected, actual, "Calendar should be converted to the matching ZonedDateTime");
    }

    @Test
    void givenNull_returnsNull() {
        assertNull(Converter.convertToZonedDateTime(null), "Null input must result in null output");
    }

    /* --------------------------------------------------------------------- *
     *  Valid conversion cases – each entry is described in the JSON spec.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validConversionProvider")
    void givenValidSource_returnsExpectedZonedDateTime(Object fromInstance, ZonedDateTime expected, String description) {
        ZonedDateTime actual = Converter.convertToZonedDateTime(fromInstance);
        assertEquals(expected, actual, description);
    }

    /* --------------------------------------------------------------------- *
     *  Unsupported source types – expect IllegalArgumentException.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("unsupportedSourceProvider")
    void givenUnsupportedSource_throwsIllegalArgumentException(Object fromInstance, String description) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToZonedDateTime(fromInstance), description);
    }

    /* --------------------------------------------------------------------- *
     *  Malformed or empty strings – expect DateTimeParseException.
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {1}")
    @MethodSource("malformedStringProvider")
    void givenMalformedString_throwsDateTimeParseException(String input, String description) {
        assertThrows(DateTimeParseException.class, () -> Converter.convertToZonedDateTime(input), description);
    }
}
