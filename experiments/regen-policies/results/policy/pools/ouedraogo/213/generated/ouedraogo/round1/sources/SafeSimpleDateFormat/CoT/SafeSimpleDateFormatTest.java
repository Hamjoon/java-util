package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.Assert.*;

public class SafeSimpleDateFormatTest {

    /* -----------------------------------------------------------------
     *  List of public methods (including signatures) in SafeSimpleDateFormat
     * -----------------------------------------------------------------
     *
     *  public static SimpleDateFormat getDateFormat(String format)
     *  public SafeSimpleDateFormat(String format)
     *  public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition)
     *  public Date parse(String source, ParsePosition pos)
     *  public void setTimeZone(TimeZone tz)
     *  public void setCalendar(Calendar cal)
     *  public void setNumberFormat(NumberFormat format)
     *  public void setLenient(boolean lenient)
     *  public void setDateFormatSymbols(DateFormatSymbols symbols)
     *  public void set2DigitYearStart(Date date)
     *  public String toString()
     *  public boolean equals(Object other)
     *  public int hashCode()
     */

    private static final String PATTERN = "yyyy/MM/dd HH:mm:ss";
    private SafeSimpleDateFormat safeFormatter;

    @Before
    public void setUp() {
        safeFormatter = new SafeSimpleDateFormat(PATTERN);
    }

    /* ------------------------------- *
     *  Basic functionality tests     *
     * ------------------------------- */

    @Test
    public void testGetDateFormatReturnsSameInstanceWithinThread() {
        SimpleDateFormat f1 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        SimpleDateFormat f2 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        // Same thread, same format string → same instance (by reference)
        assertSame("Formatter instances should be identical within the same thread", f1, f2);
    }

    @Test
    public void testConstructorInitialisesInternalState() {
        // The constructor should not throw and the toString must return the pattern
        assertEquals("toString should return the original pattern", PATTERN, safeFormatter.toString());

        // Internal SimpleDateFormat should be lenient (default of Calendar) and have grouping disabled
        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertFalse("NumberFormat grouping should be disabled", internal.getNumberFormat().isGroupingUsed());
        assertEquals("Calendar leniency should match default", Calendar.getInstance().isLenient(),
                internal.isLenient());
    }

    @Test
    public void testFormatProducesExpectedString() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.JANUARY, 15, 12, 30, 45);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        safeFormatter.format(date, sb, fp);
        assertEquals("2023/01/15 12:30:45", sb.toString());
    }

    @Test
    public void testParseValidStringReturnsCorrectDate() {
        String source = "2023/07/04 09:15:00";
        ParsePosition pos = new ParsePosition(0);
        Date parsed = safeFormatter.parse(source, pos);
        assertNotNull("Parsed date should not be null for a valid string", parsed);
        assertEquals("Parse position should be advanced to end of string",
                source.length(), pos.getIndex());
    }

    @Test
    public void testParseInvalidStringReturnsNullAndLeavesPositionUnchanged() {
        String source = "invalid-date";
        ParsePosition pos = new ParsePosition(0);
        Date parsed = safeFormatter.parse(source, pos);
        assertNull("Parsed date should be null for an invalid string", parsed);
        assertEquals("Parse position index should remain at 0 on failure", 0, pos.getIndex());
    }

    @Test
    public void testSetTimeZoneAffectsFormatting() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        safeFormatter.setTimeZone(tz);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.MARCH, 10, 15, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        String formatted = safeFormatter.format(date, new StringBuffer(), new FieldPosition(0)).toString();
        // 15:00 UTC == 11:00 EDT (UTC-4) on that date
        assertTrue("Formatted string should reflect the new timezone offset",
                formatted.startsWith("2023/03/10 11:00:00"));
    }

    @Test
    public void testSetCalendarChangesInternalCalendar() {
        Calendar customCal = Calendar.getInstance();
        customCal.setFirstDayOfWeek(Calendar.MONDAY);
        safeFormatter.setCalendar(customCal);
        // Verify that the underlying SimpleDateFormat now uses the custom calendar
        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertEquals("First day of week should be updated in internal calendar",
                Calendar.MONDAY, internal.getCalendar().getFirstDayOfWeek());
    }

    @Test
    public void testSetNumberFormatDisablesGrouping() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(true); // start with grouping enabled
        safeFormatter.setNumberFormat(nf);
        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertFalse("NumberFormat grouping must be disabled by SafeSimpleDateFormat",
                internal.getNumberFormat().isGroupingUsed());
    }

    @Test
    public void testSetLenientAffectsParsing() {
        // Non‑lenient parsing should reject ambiguous dates like "2023/02/30"
        safeFormatter.setLenient(false);
        ParsePosition pos = new ParsePosition(0);
        Date d = safeFormatter.parse("2023/02/30 00:00:00", pos);
        assertNull("Non‑lenient parser should reject invalid date", d);
        assertEquals("Parse position should not advance on failure", 0, pos.getIndex());

        // Lenient parsing should accept it and roll over to March 2nd
        safeFormatter.setLenient(true);
        pos = new ParsePosition(0);
        d = safeFormatter.parse("2023/02/30 00:00:00", pos);
        assertNotNull("Lenient parser should accept overflow date", d);
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        assertEquals("Month should roll over to March (2 -> March)", Calendar.MARCH, cal.get(Calendar.MONTH));
        assertEquals("Day should become 2", 2, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void testSetDateFormatSymbolsChangesMonthNames() {
        DateFormatSymbols symbols = new DateFormatSymbols();
        String[] months = symbols.getMonths();
        months[0] = "JanTest";
        symbols.setMonths(months);
        safeFormatter.setDateFormatSymbols(symbols);

        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        // Format using pattern that includes month name (e.g., "MMM")
        SafeSimpleDateFormat monthFormatter = new SafeSimpleDateFormat("MMM dd, yyyy");
        monthFormatter.setDateFormatSymbols(symbols);
        String result = monthFormatter.format(date, new StringBuffer(), new FieldPosition(0)).toString();
        assertTrue("Month name should be replaced by custom symbol", result.startsWith("JanTest"));
    }

    @Test
    public void testSet2DigitYearStartAffectsParsing() {
        // Set the 2‑digit year start to 1950, so "49" becomes 2049, "50" becomes 1950
        Calendar startCal = Calendar.getInstance();
        startCal.set(1950, Calendar.JANUARY, 1);
        safeFormatter.set2DigitYearStart(startCal.getTime());

        SimpleDateFormat sdf = new SimpleDateFormat("yy-MM-dd");
        sdf.set2DigitYearStart(startCal.getTime());

        Date d = sdf.parse("49-01-01");
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        assertEquals("Year should be interpreted as 2049", 2049, cal.get(Calendar.YEAR));

        d = sdf.parse("50-01-01");
        cal.setTime(d);
        assertEquals("Year should be interpreted as 1950", 1950, cal.get(Calendar.YEAR));
    }

    @Test
    public void testEqualsDelegatesToUnderlyingSimpleDateFormat() {
        SimpleDateFormat other = new SimpleDateFormat(PATTERN);
        assertTrue("equals should delegate to underlying SimpleDateFormat", safeFormatter.equals(other));
        assertFalse("equals should return false for different format", safeFormatter.equals(new SimpleDateFormat("dd/MM/yyyy")));
    }

    @Test
    public void testHashCodeMatchesUnderlyingFormatter() {
        int expected = SafeSimpleDateFormat.getDateFormat(PATTERN).hashCode();
        assertEquals("hashCode should be delegated to underlying SimpleDateFormat", expected, safeFormatter.hashCode());
    }

    /* ------------------------------------------------------ *
     *  Edge‑case and exception handling tests                 *
     * ------------------------------------------------------ */

    @Test
    public void testGetDateFormatWithNullThrowsNPE() {
        try {
            SafeSimpleDateFormat.getDateFormat(null);
            fail("Expected NullPointerException when format is null");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testConstructorWithNullFormatThrowsNPE() {
        try {
            new SafeSimpleDateFormat(null);
            fail("Expected NullPointerException when constructing with null format");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testFormatWithNullDateThrowsNPE() {
        try {
            safeFormatter.format(null, new StringBuffer(), new FieldPosition(0));
            fail("Expected NullPointerException when formatting a null Date");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testParseWithNullSourceThrowsNPE() {
        try {
            safeFormatter.parse(null, new ParsePosition(0));
            fail("Expected NullPointerException when parsing a null source string");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testSetTimeZoneWithNullResetsToDefault() {
        // According to SimpleDateFormat contract, passing null uses GMT.
        safeFormatter.setTimeZone(null);
        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertEquals("TimeZone should be GMT when null is supplied", TimeZone.getTimeZone("GMT"), internal.getTimeZone());
    }

    @Test
    public void testSetCalendarWithNullThrowsNPE() {
        try {
            safeFormatter.setCalendar(null);
            fail("Expected NullPointerException when setting a null Calendar");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testSetNumberFormatWithNullThrowsNPE() {
        try {
            safeFormatter.setNumberFormat(null);
            fail("Expected NullPointerException when setting a null NumberFormat");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testSetDateFormatSymbolsWithNullThrowsNPE() {
        try {
            safeFormatter.setDateFormatSymbols(null);
            fail("Expected NullPointerException when setting null DateFormatSymbols");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testSet2DigitYearStartWithNullThrowsNPE() {
        try {
            safeFormatter.set2DigitYearStart(null);
            fail("Expected NullPointerException when setting null 2‑digit year start date");
        } catch (NullPointerException expected) {
            // expected
        }
    }
}
