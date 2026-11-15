package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateUtilities_parseDate_Test {

    private static Stream<Arguments> validIsoProvider() {
        return Stream.of(// UTC timestamp
        Arguments.of("2023-01-02T03:04:05Z", Instant.parse("2023-01-02T03:04:05Z")), // +02:00 offset → UTC minus 2h
        Arguments.of("2023-01-02T03:04:05+02:00", Instant.parse("2023-01-02T01:04:05Z")), // -05:00 offset → UTC plus 5h
        Arguments.of("2023-12-31T23:59:59-05:00", Instant.parse("2024-01-01T04:59:59Z")));
    }

    private static Stream<String> invalidProvider() {
        return Stream.of("not-a-date", // impossible calendar date
        "2023-02-30T00:00:00Z", // missing 'T'
        "2023-01-02 03:04:05Z", // offset without colon
        "2023-01-02T03:04:05+0200", // trailing space
        "2023-01-02T03:04:05Z ", // month out of range
        "2023-13-01T00:00:00Z");
    }

    private static Stream<Arguments> dateOnlyProvider() {
        return Stream.of(// System default set to UTC → midnight UTC
        Arguments.of(ZoneId.of("UTC"), "2023-05-10", Instant.parse("2023-05-10T00:00:00Z")), // System default set to America/Los_Angeles (UTC‑07 on 2023‑05‑10)
        Arguments.of(ZoneId.of("America/Los_Angeles"), "2023-05-10", Instant.parse("2023-05-10T07:00:00Z")));
    }

    private static Stream<Arguments> fractionalProvider() {
        return Stream.of(// Milliseconds preserved in UTC
        Arguments.of("2023-01-02T03:04:05.123Z", Instant.parse("2023-01-02T03:04:05.123Z")), // Fraction with +02:00 offset → shift to UTC
        Arguments.of("2023-01-02T03:04:05.987+02:00", Instant.parse("2023-01-02T01:04:05.987Z")));
    }

    private static Stream<Arguments> leapAndExtremeProvider() {
        return Stream.of(Arguments.of("2020-02-29T12:30:00Z", Instant.parse("2020-02-29T12:30:00Z")), Arguments.of("9999-12-31T23:59:59Z", Instant.parse("9999-12-31T23:59:59Z")));
    }

    private static Stream<Arguments> defaultZoneInfluenceProvider() {
        return Stream.of(// Default zone America/New_York (UTC‑04 on 2023‑08‑15)
        Arguments.of(ZoneId.of("America/New_York"), "2023-08-15T00:00:00", Instant.parse("2023-08-15T04:00:00Z")), // Default zone UTC – instant should be identical
        Arguments.of(ZoneId.of("UTC"), "2023-08-15T00:00:00", Instant.parse("2023-08-15T00:00:00Z")));
    }

    @Test
    @DisplayName("parseDate returns null for null input")
    void parseDate_nullInput_returnsNull() {
        assertNull(DateUtilities.parseDate(null));
    }

    @Test
    @DisplayName("parseDate returns null for empty string")
    void parseDate_emptyString_returnsNull() {
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
    }

    @Test
    @DisplayName("parseDate parses a simple ISO date (yyyy-MM-dd) at start of day")
    void parseDate_simpleIsoDate_correctDate() {
        // Input string without an explicit time – should be interpreted as midnight
        // in the system default zone.
        String input = "2023-12-31";
        // Expected instant: 2023‑12‑31T00:00:00 in the system default zone.
        ZonedDateTime expectedZdt = ZonedDateTime.of(2023, 12, 31, 0, 0, 0, 0, ZoneId.systemDefault());
        Date expected = Date.from(expectedZdt.toInstant());
        Date actual = DateUtilities.parseDate(input);
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "The parsed date should represent midnight of the given day in the system default zone.");
    }

    @Test
    @DisplayName("parseDate parses a date‑time string with UTC offset")
    void parseDate_isoDateTimeWithOffset_correctInstant() {
        // Input includes a time and a UTC offset.
        String input = "2023-12-31T15:30:45+02:00";
        // Expected instant: the same moment expressed in UTC.
        OffsetDateTime odt = OffsetDateTime.of(2023, 12, 31, 15, 30, 45, 0, ZoneOffset.ofHours(2));
        Instant expectedInstant = odt.toInstant();
        Date expected = Date.from(expectedInstant);
        Date actual = DateUtilities.parseDate(input);
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "The parsed instant should match the UTC instant represented by the input string.");
    }

    @Test
    @DisplayName("parseDate parses an epoch‑millis string")
    void parseDate_epochMillisString_correctDate() {
        // The string "0" represents the Unix epoch.
        String input = "0";
        Date expected = Date.from(Instant.EPOCH);
        Date actual = DateUtilities.parseDate(input);
        assertNotNull(actual);
        assertEquals(expected.getTime(), actual.getTime(), "Parsing \"0\" should yield the Unix epoch (1970‑01‑01T00:00:00Z).");
    }

    @Test
    @DisplayName("parseDate returns null for empty or whitespace input")
    void parseDate_emptyOrWhitespace_returnsNull() {
        assertNull(DateUtilities.parseDate(""));
        assertNull(DateUtilities.parseDate("   "));
    }

    @ParameterizedTest(name = "[{index}] parseDate(\"{0}\") → {1}")
    @MethodSource("validIsoProvider")
    void parseDate_validIsoStrings(String input, Instant expectedInstant) {
        Date actual = DateUtilities.parseDate(input);
        assertNotNull(actual, "Resulting Date should not be null");
        assertEquals(expectedInstant.toEpochMilli(), actual.getTime(), () -> "Parsed instant does not match expected for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] parseDate(\"{0}\") throws DateTimeParseException")
    @MethodSource("invalidProvider")
    void parseDate_invalidInputs_throwsException(String input) {
        assertThrows(DateTimeParseException.class, () -> DateUtilities.parseDate(input), "Expected DateTimeParseException for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] defaultZone={0}, input={1} → {2}")
    @MethodSource("dateOnlyProvider")
    void parseDate_dateOnlyString_respectsSystemDefaultZone(ZoneId defaultZone, String input, Instant expectedInstant) {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(defaultZone));
            Date actual = DateUtilities.parseDate(input);
            assertNotNull(actual);
            assertEquals(expectedInstant.toEpochMilli(), actual.getTime(), () -> "Date-only parsing failed for zone " + defaultZone);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @ParameterizedTest(name = "[{index}] parseDate(\"{0}\") → {1}")
    @MethodSource("fractionalProvider")
    void parseDate_fractionalSeconds(String input, Instant expectedInstant) {
        Date actual = DateUtilities.parseDate(input);
        assertNotNull(actual);
        assertEquals(expectedInstant.toEpochMilli(), actual.getTime(), () -> "Fractional seconds not parsed correctly for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] parseDate(\"{0}\") → {1}")
    @MethodSource("leapAndExtremeProvider")
    void parseDate_leapAndExtremeYears(String input, Instant expectedInstant) {
        Date actual = DateUtilities.parseDate(input);
        assertNotNull(actual);
        assertEquals(expectedInstant.toEpochMilli(), actual.getTime(), () -> "Failed to parse leap or extreme year for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] defaultZone={0}, input={1} → {2}")
    @MethodSource("defaultZoneInfluenceProvider")
    void parseDate_timestampWithoutZone_respectsDefaultZone(ZoneId defaultZone, String input, Instant expectedInstant) {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(defaultZone));
            Date actual = DateUtilities.parseDate(input);
            assertNotNull(actual);
            assertEquals(expectedInstant.toEpochMilli(), actual.getTime(), () -> "Default zone influence failed for zone " + defaultZone);
        } finally {
            TimeZone.setDefault(original);
        }
    }
}
