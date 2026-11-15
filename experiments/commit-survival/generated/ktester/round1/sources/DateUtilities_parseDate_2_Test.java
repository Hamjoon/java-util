package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateUtilities_parseDate_2_Test {

    /**
     * Provides inputs for the “Epoch Milliseconds Parsing” group that are not covered
     * by the earlier explicit test.
     */
    private static Stream<Arguments> epochMillisProvider() {
        return Stream.of(Arguments.of("0", ZoneId.of("UTC"), Instant.EPOCH), Arguments.of("1600000000000", ZoneId.of("UTC"), Instant.ofEpochMilli(1_600_000_000_000L)), Arguments.of("1609459200000", ZoneId.of("UTC"), Instant.ofEpochMilli(1_609_459_200_000L)));
    }

    /**
     * -----------------------------------------------------------------
     *  Test 1 – null or empty input should return null.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_nullOrEmpty_returnsNull() {
        assertNull(DateUtilities.parseDate(null, ZoneId.systemDefault(), true));
        assertNull(DateUtilities.parseDate("   ", ZoneId.systemDefault(), true));
    }

    /**
     * -----------------------------------------------------------------
     *  Test 2 – numeric epoch string is interpreted as milliseconds.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_numericEpoch_isParsedCorrectly() {
        // 0 milliseconds since the epoch → 1970‑01‑01T00:00:00Z
        ZonedDateTime zdt = DateUtilities.parseDate("0", ZoneId.of("UTC"), true);
        assertNotNull(zdt);
        assertEquals(Instant.EPOCH, zdt.toInstant());
        // A known epoch value (e.g. 1 600 000 000 000 ms → 2020‑09‑13T12:26:40Z)
        ZonedDateTime expected = Instant.ofEpochMilli(1_600_000_000_000L).atZone(ZoneId.of("UTC"));
        ZonedDateTime actual = DateUtilities.parseDate("1600000000000", ZoneId.of("UTC"), true);
        assertEquals(expected, actual);
    }

    /**
     * -----------------------------------------------------------------
     *  Test 3 – ISO‑style date without time.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_isoDate_withoutTime() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-21", ZoneId.of("America/New_York"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(21, zdt.getDayOfMonth());
        // No time component → midnight in the supplied zone
        assertEquals(LocalTime.MIDNIGHT, zdt.toLocalTime());
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone());
    }

    /**
     * -----------------------------------------------------------------
     *  Test 4 – ISO‑style date with time and offset.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_isoDate_withTimeAndOffset() {
        // Input contains a +02:00 offset; the resulting ZonedDateTime should keep that offset
        ZonedDateTime zdt = DateUtilities.parseDate("2024-01-21T15:30:45+02:00", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(21, zdt.getDayOfMonth());
        assertEquals(LocalTime.of(15, 30, 45), zdt.toLocalTime());
        assertEquals(ZoneOffset.ofHours(2), zdt.getOffset());
    }

    /**
     * -----------------------------------------------------------------
     *  Test 5 – Alpha‑month format (e.g. “Jan 6th, 2024”).
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_alphaMonthFormat() {
        ZonedDateTime zdt = DateUtilities.parseDate("Jan 6th, 2024", ZoneId.of("Europe/London"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
        assertEquals(ZoneId.of("Europe/London"), zdt.getZone());
    }

    /**
     * -----------------------------------------------------------------
     *  Test 6 – Unix‑style format with day‑of‑week and zone abbreviation.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_unixStyleWithZone() {
        // “Sat Jan 6 11:06:10 EST 2024”
        ZonedDateTime zdt = DateUtilities.parseDate("Sat Jan 6 11:06:10 EST 2024", ZoneId.of("UTC"), true);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
        assertEquals(LocalTime.of(11, 6, 10), zdt.toLocalTime());
        // EST is UTC‑05:00 (ignoring daylight‑saving for the test)
        assertEquals(ZoneId.of("EST", ZoneId.SHORT_IDS), zdt.getZone());
    }

    /**
     * -----------------------------------------------------------------
     *  Test 7 – ensureDateTimeAlone = true forces garbage detection.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_ensureDateTimeAloneDetectsGarbage() {
        // The string contains extra characters after a valid time/date.
        String bad = "2024-01-21 12:34:56XYZ";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(bad, ZoneId.of("UTC"), true));
        assertTrue(ex.getMessage().contains("Unable to parse"));
    }

    /**
     * -----------------------------------------------------------------
     *  Test 8 – ensureDateTimeAlone = false tolerates trailing garbage.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_ensureDateTimeAloneFalseIgnoresGarbage() {
        // Same input as previous test but with the flag disabled.
        String input = "2024-01-21 12:34:56XYZ";
        ZonedDateTime zdt = DateUtilities.parseDate(input, ZoneId.of("UTC"), false);
        // The date part must still be parsed correctly.
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(21, zdt.getDayOfMonth());
        assertEquals(LocalTime.of(12, 34, 56), zdt.toLocalTime());
    }

    /**
     * -----------------------------------------------------------------
     *  Test 9 – default zone is honoured when no zone is supplied in the string.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_usesDefaultZoneWhenNoZoneInString() {
        ZoneId defaultZone = ZoneId.of("America/Los_Angeles");
        ZonedDateTime zdt = DateUtilities.parseDate("2024-03-15T08:00", defaultZone, true);
        assertEquals(defaultZone, zdt.getZone());
    }

    /**
     * -----------------------------------------------------------------
     *  Epoch milliseconds – 2021‑01‑01T00:00Z (numeric string larger than previous test)
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_epochMilliseconds_2021Jan01() {
        ZonedDateTime expected = Instant.ofEpochMilli(1_609_459_200_000L).atZone(ZoneId.of("UTC"));
        ZonedDateTime actual = DateUtilities.parseDate("1609459200000", ZoneId.of("UTC"), false);
        assertEquals(expected, actual);
    }

    /**
     * -----------------------------------------------------------------
     *  ISO date (leap‑year) – 2024‑02‑29 should be parsed as midnight in the supplied zone.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_isoLeapYearDate() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-02-29", ZoneId.of("America/New_York"), false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 2, 29, 0, 0, 0, 0, ZoneId.of("America/New_York"));
        assertEquals(expected, zdt);
    }

    /**
     * -----------------------------------------------------------------
     *  ISO date with time (space separator) – no explicit zone, default zone applied.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_isoDateWithTimeNoZone() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-03-10 15:30:45", ZoneId.of("UTC"), false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 10, 15, 30, 45, 0, ZoneId.of("UTC"));
        assertEquals(expected, zdt);
    }

    /**
     * -----------------------------------------------------------------
     *  ISO date with time and offset – offset overrides default zone.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_isoDateWithTimeAndOffsetOverridesDefault() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-03-10T15:30:45+02:00", ZoneId.of("UTC"), false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 10, 15, 30, 45, 0, ZoneOffset.ofHours(2));
        assertEquals(expected, zdt);
    }

    /**
     * -----------------------------------------------------------------
     *  Alpha‑month “Month Day, Year” format.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_alphaMonth_MonthDayYear() {
        ZonedDateTime zdt = DateUtilities.parseDate("Jan 2, 2024", ZoneId.of("UTC"), false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"));
        assertEquals(expected, zdt);
    }

    /**
     * -----------------------------------------------------------------
     *  Alpha‑month “Day Month Year” format.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_alphaMonth_DayMonthYear() {
        ZonedDateTime zdt = DateUtilities.parseDate("2 Jan 2024", ZoneId.of("UTC"), false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 2, 0, 0, 0, 0, ZoneId.of("UTC"));
        assertEquals(expected, zdt);
    }

    /**
     * -----------------------------------------------------------------
     *  Alpha‑month with ordinal suffix.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_alphaMonth_OrdinalDay() {
        ZonedDateTime zdt = DateUtilities.parseDate("21st Feb 2024", ZoneId.of("UTC"), false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 2, 21, 0, 0, 0, 0, ZoneId.of("UTC"));
        assertEquals(expected, zdt);
    }

    /**
     * -----------------------------------------------------------------
     *  Unix‑style date with GMT zone.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_unixStyle_GMT() {
        ZonedDateTime zdt = DateUtilities.parseDate("Tue Jan 02 13:45:30 GMT 2024", ZoneId.of("UTC"), false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 2, 13, 45, 30, 0, ZoneId.of("GMT"));
        assertEquals(expected, zdt);
    }

    /**
     * -----------------------------------------------------------------
     *  Time component after date – missing seconds default to “00”.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_timeAfterDate_NoSeconds() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-04-05 08:15", ZoneId.of("UTC"), false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 4, 5, 8, 15, 0, 0, ZoneId.of("UTC"));
        assertEquals(expected, zdt);
    }

    /**
     * -----------------------------------------------------------------
     *  Fractional seconds – milliseconds must be preserved.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_fractionalSeconds() {
        ZonedDateTime zdt = DateUtilities.parseDate("2024-04-05T08:15:30.123", ZoneId.of("UTC"), false);
        ZonedDateTime expected = ZonedDateTime.of(2024, 4, 5, 8, 15, 30, 123_000_000, ZoneId.of("UTC"));
        assertEquals(expected, zdt);
    }

    /**
     * -----------------------------------------------------------------
     *  Invalid format – completely unparsable string.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_invalidFormat_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate("not-a-date", ZoneId.of("UTC"), false));
    }

    /**
     * -----------------------------------------------------------------
     *  Null defaultZoneId – should trigger the null‑check.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_nullDefaultZoneId_throwsException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate("2024-01-01", null, false));
        assertTrue(ex.getMessage().contains("ZoneId cannot be null"));
    }

    /**
     * -----------------------------------------------------------------
     *  Numeric overflow – value exceeds Long.MAX_VALUE.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_numericOverflow_throwsNumberFormatException() {
        // 9223372036854775808 = Long.MAX_VALUE + 1
        assertThrows(NumberFormatException.class, () -> DateUtilities.parseDate("9223372036854775808", ZoneId.of("UTC"), false));
    }

    /**
     * -----------------------------------------------------------------
     *  Invalid zone identifier – should cause IllegalArgumentException from getTimeZone().
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_invalidZoneIdentifier_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate("2024-03-10T15:30:45 XYZ", ZoneId.of("UTC"), false));
    }

    /**
     * -----------------------------------------------------------------
     *  Invalid month value – month 13 is out of range.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_invalidMonth_throwsDateTimeException() {
        assertThrows(DateTimeException.class, () -> DateUtilities.parseDate("2024-13-01", ZoneId.of("UTC"), false));
    }

    /**
     * -----------------------------------------------------------------
     *  Invalid day value – February 30 does not exist.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_invalidDay_throwsDateTimeException() {
        assertThrows(DateTimeException.class, () -> DateUtilities.parseDate("2024-02-30", ZoneId.of("UTC"), false));
    }

    /**
     * -----------------------------------------------------------------
     *  Invalid numeric prefix – leading '+' prevents all‑digits match.
     *  -----------------------------------------------------------------
     */
    @Test
    void parseDate_invalidNumericPrefix_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate("+1609459200000", ZoneId.of("UTC"), false));
    }

    @ParameterizedTest(name = "epochMillis: {0}")
    @MethodSource("epochMillisProvider")
    void parseDate_epochMillisParameterized(String epochStr, ZoneId zone, Instant expectedInstant) {
        ZonedDateTime result = DateUtilities.parseDate(epochStr, zone, true);
        assertNotNull(result);
        assertEquals(expectedInstant, result.toInstant());
        assertEquals(zone, result.getZone());
    }
}
