package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

class OffsetDateTimeConversions_toLocalDateTime_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – the default constructor creates an empty options set.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// String in ISO_LOCAL_DATE_TIME format
        Arguments.of("String ISO_LOCAL_DATE_TIME", "2023-01-15T10:20:30", LocalDateTime.parse("2023-01-15T10:20:30")), // java.util.Date (epoch millis for 2022‑12‑31T23:59:59Z)
        Arguments.of("java.util.Date (epoch millis)", new Date(1672531199000L), Instant.ofEpochMilli(1672531199000L).atZone(ZoneId.systemDefault()).toLocalDateTime()), // Instant
        Arguments.of("Instant", Instant.parse("2021-06-01T08:15:00Z"), Instant.parse("2021-06-01T08:15:00Z").atZone(ZoneId.systemDefault()).toLocalDateTime()), // OffsetDateTime (offset is dropped)
        Arguments.of("OffsetDateTime", OffsetDateTime.parse("2020-03-10T14:30:00+02:00"), OffsetDateTime.parse("2020-03-10T14:30:00+02:00").toLocalDateTime()), // ZonedDateTime
        Arguments.of("ZonedDateTime", ZonedDateTime.parse("2019-07-04T09:00:00+01:00[Europe/Paris]"), ZonedDateTime.parse("2019-07-04T09:00:00+01:00[Europe/Paris]").toLocalDateTime()), // LocalDateTime (should be returned unchanged)
        Arguments.of("LocalDateTime (identity)", LocalDateTime.of(2018, 11, 5, 16, 45, 0), LocalDateTime.of(2018, 11, 5, 16, 45, 0)), // java.sql.Timestamp
        Arguments.of("java.sql.Timestamp", Timestamp.valueOf("2017-05-20 12:00:00"), LocalDateTime.of(2017, 5, 20, 12, 0, 0)));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of("Boolean value is unsupported", true), Arguments.of("Arbitrary Object is unsupported", new Object()), Arguments.of("Integer numeric type is unsupported", 12345), Arguments.of("Long numeric type is unsupported", 1234567890123L), Arguments.of("java.sql.Date is unsupported", java.sql.Date.valueOf("2022-04-01")));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("Slash‑separated date/time", "2023/01/15 10:20:30"), Arguments.of("Completely unparsable string", "not-a-date"));
    }

    @Test
    void toLocalDateTime_fromOffsetDateTime() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 10, 15, 30, 45, 123_000_000, ZoneOffset.UTC);
        // WHEN
        LocalDateTime result = OffsetDateTimeConversions.toLocalDateTime(odt, converter);
        // THEN
        LocalDateTime expected = odt.toLocalDateTime();
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromZonedDateTime() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("Europe/Paris"));
        // WHEN
        LocalDateTime result = OffsetDateTimeConversions.toLocalDateTime(zdt, converter);
        // THEN
        LocalDateTime expected = zdt.toLocalDateTime();
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromUtilDate() {
        // GIVEN
        // Create a java.util.Date representing a known instant.
        Instant instant = Instant.parse("2021-08-15T10:20:30.400Z");
        Date date = Date.from(instant);
        // WHEN
        LocalDateTime result = OffsetDateTimeConversions.toLocalDateTime(date, converter);
        // THEN
        // The conversion uses the system default time‑zone.
        LocalDateTime expected = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromString() {
        // GIVEN
        String isoString = "2020-01-02T03:04:05";
        // WHEN
        LocalDateTime result = OffsetDateTimeConversions.toLocalDateTime(isoString, converter);
        // THEN
        LocalDateTime expected = LocalDateTime.parse(isoString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDateTime_fromNullReturnsNull() {
        // GIVEN
        Object nullObj = null;
        // WHEN
        LocalDateTime result = OffsetDateTimeConversions.toLocalDateTime(nullObj, converter);
        // THEN
        assertNull(result);
    }

    // -------------------- Valid Conversions --------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toLocalDateTime_validConversions(String description, Object input, LocalDateTime expected) {
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(input, converter);
        assertEquals(expected, actual, description);
    }

    // -------------------- Null Input (both args null) --------------------
    @Test
    void toLocalDateTime_nullInputBothNull() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toLocalDateTime(null, null), "Both parameters null should throw NullPointerException");
    }

    // -------------------- Null 'from' Parameter --------------------
    @Test
    void toLocalDateTime_nullFromParameter() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toLocalDateTime(null, converter), "Null source object with non‑null converter should throw NullPointerException");
    }

    // -------------------- Null 'converter' Parameter --------------------
    @Test
    void toLocalDateTime_nullConverterParameter() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toLocalDateTime("2023-01-15T10:20:30", null), "Null converter argument should throw NullPointerException");
    }

    // -------------------- Invalid Input Types --------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputProvider")
    void toLocalDateTime_invalidInputTypes(String description, Object input) {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalDateTime(input, converter), description);
    }

    // -------------------- Invalid String Formats --------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidStringProvider")
    void toLocalDateTime_invalidStringFormats(String description, String input) {
        assertThrows(DateTimeParseException.class, () -> OffsetDateTimeConversions.toLocalDateTime(input, converter), description);
    }
}
