package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.MonthDay;
import java.time.ZonedDateTime;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.DateTimeException;

@ExtendWith(MockitoExtension.class)
class StringConversions_toMonthDay_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – they provide a ZoneId that DateUtilities can use.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    void toMonthDay_ValidIsoString_ReturnsMonthDay() {
        // "--12-25" is the ISO‑8601 format accepted by MonthDay.parse
        String input = "--12-25";
        MonthDay result = StringConversions.toMonthDay(input, converter);
        assertNotNull(result);
        assertEquals(MonthDay.of(12, 25), result);
    }

    @Test
    void toMonthDay_MatchesRegex_ReturnsMonthDay() {
        // Does not parse as ISO, but matches the MM_DD pattern "12.25"
        String input = "12.25";
        MonthDay result = StringConversions.toMonthDay(input, converter);
        assertNotNull(result);
        assertEquals(MonthDay.of(12, 25), result);
    }

    @Test
    void toMonthDay_DateUtilitiesParses_ReturnsMonthDay() {
        // Not ISO and not the simple regex, but a full date string that DateUtilities can parse.
        // The string includes a time zone so that a ZonedDateTime is produced.
        String input = "2020-12-25T10:15:30Z";
        MonthDay result = StringConversions.toMonthDay(input, converter);
        assertNotNull(result);
        assertEquals(MonthDay.of(12, 25), result);
    }

    @Test
    void toMonthDay_UnparseableString_ThrowsIllegalArgumentException() {
        // Completely invalid string – none of the parsing strategies succeed.
        String input = "not-a-date";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toMonthDay(input, converter));
        assertTrue(ex.getMessage().contains("Unable to extract Month-Day from string"));
    }

    /**
     * ISO‑8601 string parsed directly – same as the first existing test but kept for the JSON group.
     */
    @Test
    void toMonthDay_successfulParse() {
        String input = "--12-25";
        MonthDay result = StringConversions.toMonthDay(input, converter);
        assertEquals(MonthDay.of(12, 25), result);
    }

    /**
     * Regex matches with single‑digit month and day (e.g. "3-7").
     */
    @ParameterizedTest(name = "[{index}] input=''{0}'' → {1}")
    @CsvSource({ "3-7,   3,7", "9/2,   9,2" })
    void toMonthDay_regexMatch_singleDigit(String input, int expectedMonth, int expectedDay) {
        MonthDay result = StringConversions.toMonthDay(input, converter);
        assertEquals(MonthDay.of(expectedMonth, expectedDay), result);
    }

    /**
     * Regex matches with leading zeros and a '/' separator (e.g. "03/07").
     */
    @Test
    void toMonthDay_regexMatch_leadingZero() {
        String input = "03/07";
        MonthDay result = StringConversions.toMonthDay(input, converter);
        assertEquals(MonthDay.of(3, 7), result);
    }

    /**
     * Regex matches but month value is out of range – MonthDay.of should throw DateTimeException.
     */
    @Test
    void toMonthDay_regexMatch_invalidMonth() {
        // month 13 is invalid
        String input = "13-01";
        assertThrows(DateTimeException.class, () -> StringConversions.toMonthDay(input, converter));
    }

    /**
     * Regex matches but day/month combination is impossible (e.g. "31/02").
     */
    @Test
    void toMonthDay_invalidFormatException() {
        // February 31 does not exist
        String input = "31/02";
        assertThrows(DateTimeException.class, () -> StringConversions.toMonthDay(input, converter));
    }

    /**
     * DateUtilities parses a full date‑time string and extracts the month‑day.
     */
    @Test
    void toMonthDay_dateUtilitiesParse_success() {
        String input = "2022-12-25T10:15:30Z";
        MonthDay result = StringConversions.toMonthDay(input, converter);
        assertEquals(MonthDay.of(12, 25), result);
    }

    /**
     * Empty string leads DateUtilities.parseDate to return null → method returns null.
     */
    @Test
    void toMonthDay_dateUtilitiesParse_nullResult() {
        // empty string
        String input = "";
        MonthDay result = StringConversions.toMonthDay(input, converter);
        assertNull(result);
    }

    /**
     * All parsing attempts fail and DateUtilities throws an exception → wrapped in IllegalArgumentException.
     */
    @Test
    void toMonthDay_dateUtilitiesParse_exception() {
        String input = "not-a-date";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toMonthDay(input, converter));
        assertTrue(ex.getMessage().contains("Unable to extract Month-Day from string"));
        assertNotNull(ex.getCause());
    }

    /**
     * Passing a null 'from' argument results in a NullPointerException from MonthDay.parse.
     */
    @Test
    void toMonthDay_nullInput() {
        assertThrows(NullPointerException.class, () -> StringConversions.toMonthDay(null, converter));
    }

    /**
     * Passing a null {@link Converter} forces a NullPointerException when the fallback
     * logic reaches {@code converter.getOptions()}.
     */
    @Test
    void toMonthDay_nullConverter() {
        // Choose a string that forces the fallback path (fails ISO parse and regex)
        // not ISO (needs leading '--'), not matching the regex
        String input = "01-01";
        assertThrows(NullPointerException.class, () -> StringConversions.toMonthDay(input, null));
    }
}
