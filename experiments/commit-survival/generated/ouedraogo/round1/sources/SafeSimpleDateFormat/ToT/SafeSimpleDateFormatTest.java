package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class SafeSimpleDateFormatTest {

    private static final String PATTERN = "yyyy-MM-dd";
    private static final String PATTERN_TWO_DIGIT = "yy-MM-dd";

    @Test
    public void testGetDateFormatSameThreadReturnsSameInstance() {
        SimpleDateFormat f1 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        SimpleDateFormat f2 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertSame("Same thread should reuse SimpleDateFormat instance", f1, f2);
    }

    @Test
    public void testGetDateFormatDifferentThreadsDoNotShareInstance() throws Exception {
        Callable<SimpleDateFormat> task = () -> SafeSimpleDateFormat.getDateFormat(PATTERN);

        ExecutorService exec = Executors.newFixedThreadPool(2);
        Future<SimpleDateFormat> future1 = exec.submit(task);
        Future<SimpleDateFormat> future2 = exec.submit(task);

        SimpleDateFormat fmt1 = future1.get();
        SimpleDateFormat fmt2 = future2.get();

        exec.shutdownNow();

        assertNotSame("Different threads must have different SimpleDateFormat instances", fmt1, fmt2);
    }

    @Test(expected = NullPointerException.class)
    public void testGetDateFormatNullPatternThrowsException() {
        SafeSimpleDateFormat.getDateFormat(null);
    }

    @Test
    public void testConstructorInitializesFieldsCorrectly() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        assertEquals("toString() must return the original pattern", PATTERN, sdf.toString());

        // Verify that the underlying SimpleDateFormat has a cleared calendar (no time set)
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(PATTERN);
        Calendar cal = underlying.getCalendar();
        assertEquals("Calendar should be cleared (year = 1970)", 1970, cal.get(Calendar.YEAR));
        assertEquals("Calendar should be cleared (month = JANUARY)", Calendar.JANUARY, cal.get(Calendar.MONTH));
        assertEquals("Calendar should be cleared (day = 1)", 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testFormatProducesExpectedString() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2021, Calendar.MARCH, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        StringBuffer sb = new StringBuffer();
        FieldPosition pos = new FieldPosition(0);
        sdf.format(date, sb, pos);
        assertEquals("2021-03-15", sb.toString());
    }

    @Test
    public void testParseValidString() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        ParsePosition pos = new ParsePosition(0);
        Date date = sdf.parse("2022-12-31", pos);
        assertNotNull("Parse should succeed for a valid date string", date);
        assertEquals(0, pos.getIndex());

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTime(date);
        assertEquals(2022, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testParseInvalidStringLeavesParsePositionUnchanged() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        ParsePosition pos = new ParsePosition(0);
        Date date = sdf.parse("invalid-date", pos);
        assertNull("Parse should return null for invalid input", date);
        assertEquals("Parse position should not advance on failure", 0, pos.getErrorIndex());
    }

    @Test
    public void testSetTimeZoneAffectsFormatting() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        TimeZone tz = TimeZone.getTimeZone("GMT+5");
        sdf.setTimeZone(tz);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2021, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        String result = sdf.format(date, new StringBuffer(), new FieldPosition(0)).toString();
        // In GMT+5 the same instant is 2021-01-01 05:00, but the date part stays the same.
        assertEquals("2021-01-01", result);
    }

    @Test
    public void testSetCalendarIsPropagated() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        Calendar customCal = Calendar.getInstance(TimeZone.getTimeZone("GMT-3"));
        customCal.setFirstDayOfWeek(Calendar.SUNDAY);
        sdf.setCalendar(customCal);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertSame("Underlying SimpleDateFormat must hold the calendar we set", customCal, underlying.getCalendar());
    }

    @Test
    public void testSetNumberFormatIsPropagated() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(true);
        sdf.setNumberFormat(nf);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertTrue("NumberFormat should have grouping enabled after setNumberFormat",
                underlying.getNumberFormat().isGroupingUsed());
    }

    @Test
    public void testSetLenientControlsParsing() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        sdf.setLenient(false);
        ParsePosition pos = new ParsePosition(0);
        Date date = sdf.parse("2020-02-30", pos); // Invalid date (Feb 30)

        assertNull("Parsing should fail when lenient is false and date is invalid", date);
        assertEquals("Parse position error index should be set", 0, pos.getErrorIndex());

        // With lenient true, parsing rolls over to March 1
        sdf.setLenient(true);
        pos = new ParsePosition(0);
        date = sdf.parse("2020-02-30", pos);
        assertNotNull("Parsing should succeed when lenient is true", date);
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        assertEquals(2020, cal.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH));
        assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testSetDateFormatSymbolsChangesMonthNames() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("MMMM dd, yyyy");
        DateFormatSymbols symbols = new DateFormatSymbols();
        String[] customMonths = new String[]{
                "Month0","Month1","Month2","Month3","Month4","Month5",
                "Month6","Month7","Month8","Month9","Month10","Month11"
        };
        symbols.setMonths(customMonths);
        sdf.setDateFormatSymbols(symbols);

        Calendar cal = Calendar.getInstance();
        cal.set(2021, Calendar.AUGUST, 15); // August = index 7
        String formatted = sdf.format(cal.getTime(), new StringBuffer(), new FieldPosition(0)).toString();
        assertTrue("Formatted month should use custom month name", formatted.startsWith("Month7"));
    }

    @Test
    public void testSet2DigitYearStartAffectsParsing() throws ParseException {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN_TWO_DIGIT);
        // Set start to 1950-01-01
        Calendar startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        startCal.clear();
        startCal.set(1950, Calendar.JANUARY, 1);
        sdf.set2DigitYearStart(startCal.getTime());

        Date parsed = sdf.parse("49-01-01", new ParsePosition(0));
        Calendar resultCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        resultCal.setTime(parsed);
        assertEquals(2049, resultCal.get(Calendar.YEAR));
        assertEquals(Calendar.JANUARY, resultCal.get(Calendar.MONTH));
        assertEquals(1, resultCal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testToStringReturnsPattern() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        assertEquals(PATTERN, sdf.toString());
    }

    @Test
    public void testEqualsAndHashCodeDelegateToUnderlyingSimpleDateFormat() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        SimpleDateFormat plain = new SimpleDateFormat(PATTERN);

        assertTrue("SafeSimpleDateFormat should be equal to a SimpleDateFormat with same pattern",
                sdf.equals(plain));
        assertEquals("hashCode must match underlying SimpleDateFormat's hashCode",
                plain.hashCode(), sdf.hashCode());

        SafeSimpleDateFormat other = new SafeSimpleDateFormat("MM/dd/yyyy");
        assertFalse("Different patterns should not be equal", sdf.equals(other));
    }
}
