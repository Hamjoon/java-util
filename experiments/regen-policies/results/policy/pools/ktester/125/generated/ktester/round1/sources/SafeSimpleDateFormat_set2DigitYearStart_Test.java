package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Calendar;
import java.util.Date;
import java.text.ParseException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.stream.Stream;

class SafeSimpleDateFormat_set2DigitYearStart_Test {

    /**
     * Supplies valid dates for {@link #set2DigitYearStart_ValidDate_PropagatesToUnderlying(Date)}.
     *
     * <ul>
     *   <li>Epoch (new Date(0L)) – a simple, deterministic value.</li>
     *   <li>31‑Dec‑1999 – obtained by parsing a two‑digit year string with a
     *   regular {@link SimpleDateFormat}.</li>
     * </ul>
     */
    private static Stream<Arguments> validStartDateProvider() throws ParseException {
        // Epoch
        Date epoch = new Date(0L);
        // Realistic date: parse "991231" (31‑Dec‑1999) using a plain SimpleDateFormat
        SimpleDateFormat parser = new SimpleDateFormat("yyMMdd");
        Date parsed = parser.parse("991231", new ParsePosition(0));
        return Stream.of(Arguments.of(epoch), Arguments.of(parsed));
    }

    /**
     * When the two‑digit year start is set to 1 Jan 2000, a two‑digit year
     * “01” should be interpreted as 2001 (rather than 1901 or the default
     * window based on the current date).
     */
    @Test
    void set2DigitYearStart_AffectsParsingOfTwoDigitYears() throws ParseException {
        // given: a SafeSimpleDateFormat for a pattern with a two‑digit year
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("yyMMdd");
        // and: a start date of 1 Jan 2000
        Calendar startCal = Calendar.getInstance();
        startCal.clear();
        startCal.set(2000, Calendar.JANUARY, 1);
        Date startDate = startCal.getTime();
        // when: the start date is applied
        safeSdf.set2DigitYearStart(startDate);
        // and: a two‑digit year string is parsed
        // expects 1 Jan 2001
        Date parsed = safeSdf.parse("010101");
        // then: the resulting year should be 2001
        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(parsed);
        assertEquals(2001, resultCal.get(Calendar.YEAR), "Year should be 2001");
        assertEquals(Calendar.JANUARY, resultCal.get(Calendar.MONTH), "Month should be January");
        assertEquals(1, resultCal.get(Calendar.DAY_OF_MONTH), "Day should be 1");
    }

    /**
     * Parameterized test that verifies the supplied start date is correctly
     * stored inside the underlying {@link SimpleDateFormat}.
     *
     * The test iterates over a set of valid dates (epoch and a realistic
     * parsed date) and asserts that {@code get2DigitYearStart()} returns the
     * same instance that was passed to {@code set2DigitYearStart}.
     */
    @ParameterizedTest(name = "{index}: startDate = {0}")
    @MethodSource("validStartDateProvider")
    void set2DigitYearStart_ValidDate_PropagatesToUnderlying(Date startDate) {
        // given: a SafeSimpleDateFormat for a pattern with a two‑digit year
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("yyMMdd");
        // when: the start date is applied
        safeSdf.set2DigitYearStart(startDate);
        // then: the underlying SimpleDateFormat should hold the same start date
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat("yyMMdd");
        Date actualStart = underlying.get2DigitYearStart();
        assertNotNull(actualStart, "Underlying SimpleDateFormat should have a non‑null start date");
        assertEquals(startDate, actualStart, "Underlying SimpleDateFormat's 2‑digit year start must equal the date supplied to SafeSimpleDateFormat");
    }

    /**
     * Verifies that passing {@code null} to {@code set2DigitYearStart} results
     * in an {@link IllegalArgumentException}. The underlying {@link SimpleDateFormat}
     * throws the same exception, and the wrapper must not swallow it.
     */
    @Test
    void set2DigitYearStart_NullDate_ThrowsIllegalArgumentException() {
        // given: a SafeSimpleDateFormat instance
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat("yyMMdd");
        // when/then: calling with null must throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> safeSdf.set2DigitYearStart(null), "Calling set2DigitYearStart(null) should throw IllegalArgumentException");
    }
}
