package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateUtilities_parseDate_2_Test {

    private static Stream<Arguments> isoZoneWithoutTimeProvider() {
        return Stream.of(Arguments.of("2024-08-20+05:00", ZoneId.of("UTC"), false), Arguments.of("2024/12/31 PST", ZoneId.of("UTC"), true));
    }

    /**
     * -----------------------------------------------------------------
     *  1. Private constructor – ensure the utility class cannot be
     *  instantiated.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<DateUtilities> ctor = DateUtilities.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(ctor.getModifiers()), "Constructor should be private");
        ctor.setAccessible(true);
        assertDoesNotThrow(() -> ctor.newInstance(), "Instantiating via reflection should not throw");
    }

    /**
     * -----------------------------------------------------------------
     *  2. Epoch milliseconds – pure‑digit string should be interpreted as
     *  epoch millis and returned in the supplied default zone.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testParseEpochMillis() {
        // 2023‑01‑01T00:59:59Z
        String epochStr = "1672531199000";
        ZoneId defaultZone = ZoneId.of("America/New_York");
        ZonedDateTime zdt = DateUtilities.parseDate(epochStr, defaultZone, true);
        assertNotNull(zdt);
        assertEquals(Instant.ofEpochMilli(1672531199000L), zdt.toInstant());
        assertEquals(defaultZone, zdt.getZone());
    }

    /**
     * -----------------------------------------------------------------
     *  3. ISO‑style string with an explicit zone identifier – the zone
     *  supplied in the string must win over the default zone.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testParseIsoWithExplicitZone() {
        String input = "2024-02-05T22:31:17.409[Asia/Tokyo]";
        // should be ignored
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, true);
        assertNotNull(zdt);
        assertEquals(ZoneId.of("Asia/Tokyo"), zdt.getZone());
        assertEquals(22, zdt.getHour());
        assertEquals(31, zdt.getMinute());
        assertEquals(17, zdt.getSecond());
        // Verify the instant corresponds to UTC‑9 hours
        Instant expected = Instant.parse("2024-02-05T13:31:17.409Z");
        assertEquals(expected, zdt.toInstant());
    }

    /**
     * -----------------------------------------------------------------
     *  4. Strict mode – leftover characters after a valid date/time
     *  should cause an IllegalArgumentException.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testStrictModeGarbageThrows() {
        String input = "2024-01-01abc";
        ZoneId zone = ZoneId.systemDefault();
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(input, zone, true), "Garbage after a valid date/time should trigger an exception in strict mode");
    }

    /**
     * -----------------------------------------------------------------
     *  5. Non‑strict mode – garbage is ignored and the default zone is
     *  used when the zone part cannot be resolved.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testNonStrictModeIgnoresGarbageAndBadZone() {
        // XYZ is not a known zone
        String input = "2024-01-01T12:00:00 XYZ";
        ZoneId defaultZone = ZoneId.of("Europe/Paris");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, false);
        assertNotNull(zdt);
        // Because the zone is invalid, the default zone should be applied
        assertEquals(defaultZone, zdt.getZone());
        assertEquals(12, zdt.getHour());
        assertEquals(0, zdt.getMinute());
        assertEquals(0, zdt.getSecond());
    }

    /**
     * -----------------------------------------------------------------
     *  6. Time‑zone without a time component – should raise an exception.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testZoneWithoutTimeThrows() {
        // The string contains a zone after the date but no time part.
        String input = "2024-01-01TUTC";
        ZoneId defaultZone = ZoneId.of("UTC");
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(input, defaultZone, true), "A zone without an accompanying time must be rejected");
    }

    /**
     * -----------------------------------------------------------------
     *  7. Alpha‑month format – month name should be resolved correctly.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testParseAlphaMonthFormat() {
        String input = "January 6th, 2024 15:45:30";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, true);
        assertNotNull(zdt);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
        assertEquals(15, zdt.getHour());
        assertEquals(45, zdt.getMinute());
        assertEquals(30, zdt.getSecond());
        assertEquals(defaultZone, zdt.getZone());
    }

    /**
     * -----------------------------------------------------------------
     *  8. Unix‑style format – includes day‑of‑week and abbreviation.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testParseUnixStyleFormat() {
        String input = "Sat Jan 6 11:06:10 EST 2024";
        // should be overridden by EST
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, true);
        assertNotNull(zdt);
        // EST is UTC‑5
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone(), "Abbreviation EST should resolve to America/New_York");
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(6, zdt.getDayOfMonth());
        assertEquals(11, zdt.getHour());
        assertEquals(6, zdt.getMinute());
        assertEquals(10, zdt.getSecond());
    }

    /**
     * -----------------------------------------------------------------
     *  9. Null / whitespace input should return null.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testWhitespaceInputReturnsNull() {
        String input = "   ";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, false);
        assertNull(zdt, "Whitespace-only input should result in null");
    }

    /**
     * -----------------------------------------------------------------
     * 10. Null default ZoneId must cause IllegalArgumentException.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testNullDefaultZoneThrows() {
        String input = "2024-01-01";
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(input, null, false), "Passing a null defaultZoneId must raise IllegalArgumentException");
    }

    /**
     * -----------------------------------------------------------------
     * 11. Epoch millis parsing – specific epoch value from JSON test case.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testEpochMillisParsingSpecific() {
        // 2021‑01‑01T00:00:00Z
        String epoch = "1609459200000";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(epoch, defaultZone, false);
        assertNotNull(zdt);
        assertEquals(ZonedDateTime.parse("2021-01-01T00:00:00Z[UTC]"), zdt);
    }

    /**
     * -----------------------------------------------------------------
     * 12. ISO date without time – should default to midnight in the
     *     supplied default zone.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testIsoDateWithoutTime() {
        String input = "2024-07-15";
        ZoneId defaultZone = ZoneId.of("America/New_York");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, false);
        assertNotNull(zdt);
        assertEquals(ZonedDateTime.parse("2024-07-15T00:00:00-04:00[America/New_York]"), zdt);
    }

    /**
     * -----------------------------------------------------------------
     * 13. ISO date with Zulu timezone – zone must be UTC regardless of default.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testIsoDateWithZulu() {
        String input = "2024-01-01T12:34:56Z";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, false);
        assertNotNull(zdt);
        assertEquals(ZonedDateTime.parse("2024-01-01T12:34:56Z[UTC]"), zdt);
    }

    /**
     * -----------------------------------------------------------------
     * 14. ISO date with numeric offset timezone.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testIsoDateWithOffset() {
        String input = "2024-03-10T08:15:30+02:00";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, false);
        assertNotNull(zdt);
        assertEquals(ZonedDateTime.parse("2024-03-10T08:15:30+02:00[+02:00]"), zdt);
    }

    /**
     * -----------------------------------------------------------------
     * 15. ISO date with named timezone in strict mode.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testIsoDateWithNamedTimezoneStrict() {
        String input = "2024-05-20T14:00:00 America/Los_Angeles";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, true);
        assertNotNull(zdt);
        assertEquals(ZonedDateTime.parse("2024-05-20T14:00:00-07:00[America/Los_Angeles]"), zdt);
    }

    /**
     * -----------------------------------------------------------------
     * 16. ISO date with timezone but no time – must throw.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testIsoDateWithTimezoneButNoTimeThrows() {
        String input = "2024-12-31 PST";
        ZoneId defaultZone = ZoneId.of("UTC");
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(input, defaultZone, false), "Timezone without a time component should be rejected");
    }

    /**
     * -----------------------------------------------------------------
     * 17. Alpha‑month format – month first, with minutes only.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testAlphaMonthMonthFirst() {
        String input = "Jan 2, 2024 15:04";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, false);
        assertNotNull(zdt);
        assertEquals(ZonedDateTime.parse("2024-01-02T15:04:00Z[UTC]"), zdt);
    }

    /**
     * -----------------------------------------------------------------
     * 18. Alpha‑month format – day first with ordinal.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testAlphaMonthDayFirstOrdinal() {
        String input = "21st Feb 2023";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, false);
        assertNotNull(zdt);
        assertEquals(ZonedDateTime.parse("2023-02-21T00:00:00Z[UTC]"), zdt);
    }

    /**
     * -----------------------------------------------------------------
     * 19. Unix‑style with named timezone (EST) – should resolve to America/New_York.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testUnixStyleWithNamedTZ() {
        String input = "Tue Mar 5 14:30:00 EST 2024";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, false);
        assertNotNull(zdt);
        assertEquals(ZoneId.of("America/New_York"), zdt.getZone(), "EST should map to America/New_York");
        assertEquals(ZonedDateTime.parse("2024-03-05T14:30:00-05:00[America/New_York]"), zdt);
    }

    /**
     * -----------------------------------------------------------------
     * 20. Fractional seconds – nanosecond precision must be kept.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testFractionalSeconds() {
        String input = "2024-03-01T12:34:56.789Z";
        ZoneId defaultZone = ZoneId.of("UTC");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, false);
        assertNotNull(zdt);
        assertEquals(ZonedDateTime.parse("2024-03-01T12:34:56.789Z[UTC]"), zdt);
    }

    /**
     * -----------------------------------------------------------------
     * 21. Invalid timezone ignored in non‑strict mode – default zone used.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testInvalidTimezoneIgnoredNonStrict() {
        String input = "2024-06-01T10:00:00 XYZ";
        ZoneId defaultZone = ZoneId.of("America/New_York");
        ZonedDateTime zdt = DateUtilities.parseDate(input, defaultZone, false);
        assertNotNull(zdt);
        assertEquals(defaultZone, zdt.getZone(), "Invalid timezone should be ignored in non‑strict mode");
        assertEquals(ZonedDateTime.parse("2024-06-01T10:00:00-04:00[America/New_York]"), zdt);
    }

    /**
     * -----------------------------------------------------------------
     * 22. Invalid timezone in strict mode – must throw.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testInvalidTimezoneStrictThrows() {
        String input = "2024-06-01T10:00:00 XYZ";
        ZoneId defaultZone = ZoneId.of("America/New_York");
        assertThrows(Exception.class, () -> DateUtilities.parseDate(input, defaultZone, true), "Unknown timezone should cause an exception in strict mode");
    }

    /**
     * -----------------------------------------------------------------
     * 23. Garbage after a valid ISO date‑time in strict mode.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testGarbageAfterDateTimeStrictThrows() {
        String input = "2024-01-01T00:00:00Z extra";
        ZoneId defaultZone = ZoneId.of("UTC");
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(input, defaultZone, true), "Extra text after a valid date‑time must trigger IllegalArgumentException in strict mode");
    }

    /**
     * -----------------------------------------------------------------
     * 24. ISO pattern with zone but no time – two variations.
     *  ----------------------------------------------------------------- **
     */
    @ParameterizedTest
    @MethodSource("isoZoneWithoutTimeProvider")
    void testIsoPatternZoneWithoutTime(String dateStr, ZoneId defaultZone, boolean ensureAlone) {
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(dateStr, defaultZone, ensureAlone), "Zone without accompanying time must be rejected");
    }

    /**
     * -----------------------------------------------------------------
     * 25. Ensure that the utility correctly handles null input (already
     *     covered by whitespace test) – added for completeness.
     *  ----------------------------------------------------------------- **
     */
    @Test
    void testNullInputReturnsNull() {
        ZonedDateTime zdt = DateUtilities.parseDate(null, ZoneId.of("UTC"), false);
        assertNull(zdt, "Null input should result in null");
    }
}
