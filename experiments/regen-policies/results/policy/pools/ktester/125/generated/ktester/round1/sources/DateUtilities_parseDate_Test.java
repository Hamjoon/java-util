package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateUtilities_parseDate_Test {

    /**
     * Helper to temporarily override the JVM default zone. **
     */
    private static ZoneId originalDefaultZone;

    private static void setSystemDefaultZone(ZoneId zone) {
        originalDefaultZone = ZoneId.systemDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
    }

    private static void restoreSystemDefaultZone() {
        if (originalDefaultZone != null) {
            TimeZone.setDefault(TimeZone.getTimeZone(originalDefaultZone));
            originalDefaultZone = null;
        }
    }

    private static Stream<Arguments> validDateParsingProvider() {
        return Stream.of(// Standard ISO‑8601 UTC timestamp
        Arguments.of("2023-03-15T12:30:45Z", Instant.parse("2023-03-15T12:30:45Z")), // ISO‑8601 with positive offset (+02:00) – should be converted to UTC
        Arguments.of("2023-03-15T12:30:45+02:00", Instant.parse("2023-03-15T10:30:45Z")), // Date‑only string – interpreted as midnight in the system default zone
        Arguments.of("2023-03-15", // Expected instant will be computed inside the test using the current default zone
        null));
    }

    private static Stream<Arguments> invalidFormatProvider() {
        return Stream.of(Arguments.of("2023/03/15"), Arguments.of("12:30:45"), // missing colon in offset
        Arguments.of("2023-03-15T12:30:45+0200"));
    }

    @Test
    void parseDate_nullOrEmpty_returnsNull() {
        assertNull(DateUtilities.parseDate(null), "null input should yield null");
        assertNull(DateUtilities.parseDate(""), "empty string should yield null");
        assertNull(DateUtilities.parseDate("   "), "blank string should yield null");
    }

    @Test
    void parseDate_isoUtcWithZ_equalsInstantParse() {
        String iso = "2023-08-30T22:00Z";
        Date date = DateUtilities.parseDate(iso);
        assertNotNull(date, "Date should not be null for a valid ISO string");
        Instant expected = Instant.parse(iso);
        assertEquals(Date.from(expected), date, "Parsed date should match Instant.parse result");
    }

    @Test
    void parseDate_isoUtcWithVariousOffsets_areEqual() {
        String base = "2013-08-30T22:00";
        String[] variants = { "2013-08-30T22:00Z", "2013-08-30T22:00+00:00", "2013-08-30T22:00-00:00", "2013-08-30T22:00+0000", "2013-08-30T22:00-0000" };
        Date reference = DateUtilities.parseDate(variants[0]);
        assertNotNull(reference, "Reference date must not be null");
        for (int i = 1; i < variants.length; i++) {
            Date d = DateUtilities.parseDate(variants[i]);
            assertEquals(reference, d, "Variant [" + variants[i] + "] should be equal to the reference date");
        }
        // A variant with a non‑zero offset should be different
        Date different = DateUtilities.parseDate("2013-08-30T22:00+00:00:01");
        assertNotEquals(reference, different, "Date with a non‑zero offset seconds should differ from the reference");
    }

    @Test
    void parseDate_isoWithFractionalSeconds_areEqual() {
        String[] inputs = { "2023-08-30T22:00:00.0Z", "2023-08-30T22:00:00.000000000Z", "2023-08-30T22:00:00.123456789Z" };
        // Use the first entry as the reference
        Date reference = DateUtilities.parseDate(inputs[0]);
        assertNotNull(reference, "Reference date must not be null");
        for (int i = 1; i < inputs.length; i++) {
            Date d = DateUtilities.parseDate(inputs[i]);
            assertEquals(reference, d, "Input [" + inputs[i] + "] should resolve to the same instant as the reference");
        }
    }

    @Test
    void parseDate_unixEpochMillis_isHandled() {
        // 2022‑01‑01T00:00:00Z in epoch milliseconds
        String epochMillis = "1640995200000";
        Date date = DateUtilities.parseDate(epochMillis);
        assertNotNull(date, "Date should be parsed from epoch milliseconds");
        Instant expected = Instant.ofEpochMilli(Long.parseLong(epochMillis));
        assertEquals(Date.from(expected), date, "Parsed date must match the instant derived from the epoch milliseconds");
    }

    @Test
    void parseDate_invalidFormat_throwsException() {
        String bad = "not-a-date";
        // The internal implementation throws an IllegalArgumentException for unparsable strings.
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(bad), "Parsing an invalid date string should throw IllegalArgumentException");
    }

    @Test
    void parseDate_usesSystemDefaultZoneWhenNoZoneSpecified() {
        // The string contains no explicit zone; the result should be interpreted in the system default zone.
        String input = "2024-01-15 14:30:00";
        Date date = DateUtilities.parseDate(input);
        assertNotNull(date, "Date must not be null for a valid local‑time string");
        // Build the expected ZonedDateTime manually using the system default zone.
        ZonedDateTime expectedZdt = ZonedDateTime.of(2024, 1, 15, 14, 30, 0, 0, ZoneId.systemDefault());
        Instant expectedInstant = expectedZdt.toInstant();
        assertEquals(Date.from(expectedInstant), date, "Parsed date should correspond to the system default time‑zone interpretation");
    }

    @ParameterizedTest(name = "[{index}] parseDate({0})")
    @MethodSource("validDateParsingProvider")
    void parseDate_validInputs(String input, Instant expectedInstant) {
        // For the date‑only case we compute the expected instant on‑the‑fly.
        if (expectedInstant == null) {
            ZonedDateTime zdt = ZonedDateTime.of(2023, 3, 15, 0, 0, 0, 0, ZoneId.systemDefault());
            expectedInstant = zdt.toInstant();
        }
        Date result = DateUtilities.parseDate(input);
        assertNotNull(result, "Result should not be null for input: " + input);
        assertEquals(Date.from(expectedInstant), result, "Parsed date does not match expected instant for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] parseDate_invalid({0})")
    @MethodSource("invalidFormatProvider")
    void parseDate_invalidFormats(String input) {
        // The implementation throws IllegalArgumentException for malformed inputs.
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(input), "Expected IllegalArgumentException for malformed input: " + input);
    }

    @Test
    void parseDate_negativeOffset_isConvertedToUtc() {
        String input = "2023-03-15T12:30:45-05:00";
        // Expected UTC instant = 2023-03-15T17:30:45Z
        Instant expected = Instant.parse("2023-03-15T17:30:45Z");
        Date result = DateUtilities.parseDate(input);
        assertNotNull(result);
        assertEquals(Date.from(expected), result, "Negative offset should be added to obtain the UTC instant");
    }

    @Test
    void parseDate_fractionalSeconds_preserved() {
        String input = "2023-03-15T12:30:45.123Z";
        Instant expected = Instant.parse("2023-03-15T12:30:45.123Z");
        Date result = DateUtilities.parseDate(input);
        assertNotNull(result);
        assertEquals(Date.from(expected), result, "Fractional seconds must be preserved in the resulting Date");
    }

    @Test
    void parseDate_leapYearDate_parsedAsUtcMidnight() {
        // Force the JVM default zone to UTC for deterministic expectations.
        setSystemDefaultZone(ZoneOffset.UTC);
        try {
            String input = "2020-02-29";
            Instant expected = Instant.parse("2020-02-29T00:00:00Z");
            Date result = DateUtilities.parseDate(input);
            assertNotNull(result);
            assertEquals(Date.from(expected), result, "Leap‑day should be parsed as midnight UTC when default zone is UTC");
        } finally {
            restoreSystemDefaultZone();
        }
    }

    @Test
    void parseDate_systemDefaultZone_AmericaNewYork_interpretsDateOnlyCorrectly() {
        // Set default zone to America/New_York (UTC‑5)
        setSystemDefaultZone(ZoneId.of("America/New_York"));
        try {
            String input = "2023-03-15";
            // Midnight in New York => 2023‑03‑15T05:00:00Z
            Instant expected = Instant.parse("2023-03-15T05:00:00Z");
            Date result = DateUtilities.parseDate(input);
            assertNotNull(result);
            assertEquals(Date.from(expected), result, "Date‑only string should be interpreted as midnight in the default zone");
        } finally {
            restoreSystemDefaultZone();
        }
    }

    @Test
    void parseDate_overloaded_nullDateStr_throwsNPE() {
        assertThrows(NullPointerException.class, () -> DateUtilities.parseDate(null, ZoneId.systemDefault(), true), "Calling overloaded parseDate with null dateStr should throw NPE");
    }

    @Test
    void parseDate_overloaded_nullZoneId_throwsNPE() {
        assertThrows(NullPointerException.class, () -> DateUtilities.parseDate("2023-03-15", null, true), "Calling overloaded parseDate with null ZoneId should throw NPE");
    }
}
