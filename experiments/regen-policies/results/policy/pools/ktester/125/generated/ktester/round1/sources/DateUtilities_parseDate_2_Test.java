package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateUtilities_parseDate_2_Test {

    /**
     * shared ZoneId used by most tests
     */
    private static ZoneId utcZone;

    @BeforeAll
    static void setupBeforeAll() {
        // initialise a well‑known zone that will be used as the default in the tests
        utcZone = ZoneId.of("UTC");
    }

    private static Stream<Arguments> epochMillisProvider() {
        return Stream.of(Arguments.of("0", ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), utcZone)), Arguments.of("-86400000", ZonedDateTime.ofInstant(Instant.ofEpochMilli(-86_400_000L), utcZone)));
    }

    /**
     * Verify that a null input string returns {@code null}.
     */
    @Test
    void parseDate_nullString_returnsNull() {
        // given
        String input = null;
        // when
        ZonedDateTime result = DateUtilities.parseDate(input, utcZone, true);
        // then
        assertNull(result, "Parsing a null string should return null");
    }

    /**
     * Verify that an epoch‑millisecond string is correctly interpreted.
     */
    @Test
    void parseDate_epochMillis_parsesCorrectly() {
        // given
        // 1640995200000 ms = 2022‑01‑01T00:00:00Z
        String epoch = "1640995200000";
        // when
        ZonedDateTime zdt = DateUtilities.parseDate(epoch, utcZone, true);
        // then
        assertNotNull(zdt);
        Instant expected = Instant.ofEpochMilli(1640995200000L);
        assertEquals(expected, zdt.toInstant());
        assertEquals(utcZone, zdt.getZone());
    }

    /**
     * Verify that a standard ISO‑8601 date/time string is parsed correctly.
     */
    @Test
    void parseDate_isoString_parsesCorrectly() {
        // given
        String iso = "2024-01-19T22:30:00+01:00";
        // when
        ZonedDateTime zdt = DateUtilities.parseDate(iso, utcZone, true);
        // then
        assertNotNull(zdt);
        // The parsed zone should be the offset supplied in the string, not the default zone
        assertEquals(ZoneId.of("+01:00"), zdt.getZone());
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(19, zdt.getDayOfMonth());
        assertEquals(22, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(0, zdt.getSecond());
    }

    /**
     * When {@code ensureDateTimeAlone} is {@code true} any leftover characters should cause an
     * {@link IllegalArgumentException}.
     */
    @Test
    void parseDate_extraGarbageWithEnsureTrue_throws() {
        // given
        String bad = "2024-01-19T22:30:00 extra text";
        // when / then
        assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(bad, utcZone, true), "Garbage after a valid date/time should trigger an exception when ensureDateTimeAlone is true");
    }

    /**
     * When {@code ensureDateTimeAlone} is {@code false} the parser should ignore trailing garbage.
     */
    @Test
    void parseDate_extraGarbageWithEnsureFalse_parsesIgnoringGarbage() {
        // given
        String input = "2024-01-19T22:30:00 some junk";
        // when
        ZonedDateTime zdt = DateUtilities.parseDate(input, utcZone, false);
        // then
        assertNotNull(zdt);
        assertEquals(2024, zdt.getYear());
        assertEquals(1, zdt.getMonthValue());
        assertEquals(19, zdt.getDayOfMonth());
        assertEquals(22, zdt.getHour());
        assertEquals(30, zdt.getMinute());
        assertEquals(0, zdt.getSecond());
    }

    /**
     * Passing a {@code null} {@link ZoneId} must result in an exception (the utility validates the argument).
     */
    @Test
    void parseDate_nullZoneId_throws() {
        // given
        String iso = "2024-01-19T22:30:00";
        // when / then
        assertThrows(NullPointerException.class, () -> DateUtilities.parseDate(iso, null, true), "A null ZoneId should cause a NullPointerException");
    }

    /**
     * The class has a private no‑arg constructor; verify that it is indeed private and can be
     * instantiated via reflection (a common pattern for utility classes).
     */
    @Test
    void privateConstructor_isPrivateAndInstantiable() throws Exception {
        // given
        Constructor<DateUtilities> ctor = DateUtilities.class.getDeclaredConstructor();
        // then
        assertTrue(Modifier.isPrivate(ctor.getModifiers()), "Constructor should be private");
        // when
        ctor.setAccessible(true);
        DateUtilities instance = ctor.newInstance();
        // then – the instance should not be null (even though the class only contains static methods)
        assertNotNull(instance);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] blank input")
    @ValueSource(strings = { "   ", "\t\n" })
    void parseDate_blankString_returnsNull(String input) {
        ZonedDateTime result = DateUtilities.parseDate(input, utcZone, false);
        assertNull(result, "Blank (after trimming) input should return null");
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_defaultZoneIdNull_throwsIllegalArgumentException() {
        String date = "2024-01-01";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(date, null, false));
        assertTrue(ex.getMessage().contains("ZoneId cannot be null"), "Exception message should mention ZoneId cannot be null");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] epochMillis={0}")
    @MethodSource("epochMillisProvider")
    void parseDate_allDigitsEpoch(String epochStr, ZonedDateTime expected) {
        ZonedDateTime actual = DateUtilities.parseDate(epochStr, utcZone, false);
        assertEquals(expected, actual);
    }

    @Test
    void parseDate_allDigitsOverflow_throwsNumberFormatException() {
        // Long.MAX_VALUE + 1
        String overflow = "9223372036854775808";
        assertThrows(NumberFormatException.class, () -> DateUtilities.parseDate(overflow, utcZone, false));
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_isoYearFirst_withoutTime() {
        String input = "2024-07-21";
        ZonedDateTime expected = ZonedDateTime.of(2024, 7, 21, 0, 0, 0, 0, utcZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_isoDayFirst_slashSeparator() {
        String input = "21/07/2024";
        ZonedDateTime expected = ZonedDateTime.of(2024, 7, 21, 0, 0, 0, 0, utcZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_alphaMonth_monthFirst() {
        String input = "Jan 21 2024";
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 21, 0, 0, 0, 0, utcZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_alphaMonth_dayFirst() {
        String input = "21 Jan 2024";
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 21, 0, 0, 0, 0, utcZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_alphaMonth_yearFirst() {
        String input = "2024 Jan 21";
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 21, 0, 0, 0, 0, utcZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_alphaMonth_ordinalAndComma() {
        String input = "Jan 21st, 2024";
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 21, 0, 0, 0, 0, utcZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_alphaMonth_lowerCaseMonth() {
        String input = "feb 5 2024";
        ZonedDateTime expected = ZonedDateTime.of(2024, 2, 5, 0, 0, 0, 0, utcZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_alphaMonth_unknownMonth_throwsNPE() {
        String input = "Foo 21 2024";
        assertThrows(NullPointerException.class, () -> DateUtilities.parseDate(input, utcZone, false));
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_unixStyle_withExplicitTZ() {
        String input = "Fri Mar 21 14:30:00 GMT 2024";
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 21, 14, 30, 0, 0, ZoneId.of("GMT"));
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_unixStyle_noTZ_usesDefault() {
        String input = "Fri Mar 21 14:30:00 2024";
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 21, 14, 30, 0, 0, utcZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_unixStyle_gmtOffset() {
        String input = "Fri Mar 21 14:30:00 GMT+02:00 2024";
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 21, 14, 30, 0, 0, ZoneId.of("+02:00"));
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_unixStyle_unknownMonth_throwsNPE() {
        String input = "Fri Foo 21 14:30:00 2024";
        assertThrows(NullPointerException.class, () -> DateUtilities.parseDate(input, utcZone, false));
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_unixStyle_invalidString_throwsIllegalArgumentException() {
        String input = "Invalid Unix Date String";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(input, utcZone, false));
        assertTrue(ex.getMessage().contains("Unable to parse"));
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_timeWithFractionalAndOffset() {
        String input = "2024-03-15 14:05:07.123+02:00";
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 15, 14, 5, 7, 123_000_000, ZoneId.of("+02:00"));
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_timeWithTrailingZ() {
        String input = "2024-03-15T14:05Z";
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 15, 14, 5, 0, 0, ZoneId.of("Z"));
        ZonedDateTime actual = DateUtilities.parseDate(input, ZoneId.of("America/New_York"), false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_timeOnlyString_throwsIllegalArgumentException() {
        String input = "14:05:07";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(input, utcZone, false));
        assertTrue(ex.getMessage().contains("Unable to parse"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] input={0}")
    @CsvSource({ "'2024-03-15 14:05+02', '+02:00'", "'2024-03-15 14:05+0230', '+02:30'" })
    void parseDate_timeOffsetWithoutColon(String input, String expectedOffset) {
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(ZoneId.of(expectedOffset), actual.getZone(), "Offset should be normalized to proper ZoneId");
        assertEquals(14, actual.getHour());
        assertEquals(5, actual.getMinute());
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_namedTimezonePST() {
        String input = "2024-03-15 12:00 PST";
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        // PST maps to America/Los_Angeles
        assertEquals(ZoneId.of("America/Los_Angeles"), actual.getZone());
        assertEquals(12, actual.getHour());
    }

    @Test
    void parseDate_namedTimezoneInBrackets() {
        String input = "2024-03-15 12:00 [America/New_York]";
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(ZoneId.of("America/New_York"), actual.getZone());
        assertEquals(12, actual.getHour());
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_invalidTimezoneName_throwsIllegalArgumentException() {
        String input = "2024-03-15 12:00 XYZ";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(input, utcZone, false));
        assertTrue(ex.getMessage().contains("Invalid time zone"));
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_isoDotSeparatorWithTime() {
        String input = "2024.07.21 08:30:15";
        ZonedDateTime expected = ZonedDateTime.of(2024, 7, 21, 8, 30, 15, 0, utcZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_timeBeforeIsoDate() {
        String input = "08:30:15 2024-07-21";
        ZonedDateTime expected = ZonedDateTime.of(2024, 7, 21, 8, 30, 15, 0, utcZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, false);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_garbageWithEnsureTrue_throwsIllegalArgumentException() {
        String input = "2024-03-15 abc";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> DateUtilities.parseDate(input, utcZone, true));
        assertTrue(ex.getMessage().contains("Garbage"));
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_trailingWhitespaceWithEnsureTrue_parses() {
        String input = "2024-03-15   ";
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 15, 0, 0, 0, 0, utcZone);
        ZonedDateTime actual = DateUtilities.parseDate(input, utcZone, true);
        assertEquals(expected, actual);
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_isoInvalidMonth_throwsDateTimeException() {
        String input = "2024-13-01";
        assertThrows(DateTimeException.class, () -> DateUtilities.parseDate(input, utcZone, false));
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_isoInvalidDay_throwsDateTimeException() {
        String input = "2024-02-30";
        assertThrows(DateTimeException.class, () -> DateUtilities.parseDate(input, utcZone, false));
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_invalidTimezoneOffset_throwsDateTimeException() {
        String input = "2024-03-15 12:00 +25:00";
        assertThrows(DateTimeException.class, () -> DateUtilities.parseDate(input, utcZone, false));
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_invalidHour_throwsDateTimeException() {
        String input = "2024-03-15 25:00";
        assertThrows(DateTimeException.class, () -> DateUtilities.parseDate(input, utcZone, false));
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_invalidMinute_throwsDateTimeException() {
        String input = "2024-03-15 12:61";
        assertThrows(DateTimeException.class, () -> DateUtilities.parseDate(input, utcZone, false));
    }

    // -----------------------------------------------------------------
    @Test
    void parseDate_invalidSecond_throwsDateTimeException() {
        String input = "2024-03-15 12:00:61";
        assertThrows(DateTimeException.class, () -> DateUtilities.parseDate(input, utcZone, false));
    }
}
