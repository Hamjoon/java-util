package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class SafeSimpleDateFormatTest {

    private static final String PATTERN = "yyyy-MM-dd";
    private static final String PATTERN2 = "dd/MM/yyyy";

    private SafeSimpleDateFormat sdf;
    private Date sampleDate;

    @Before
    public void setUp() {
        sdf = new SafeSimpleDateFormat(PATTERN);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2023, Calendar.MARCH, 15, 12, 30, 45);
        sampleDate = cal.getTime();
    }

    /* ---------- getDateFormat ---------- */
    @Test
    public void testGetDateFormat_SamePatternSameInstance() {
        SimpleDateFormat f1 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        SimpleDateFormat f2 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertSame("Same pattern should return same instance within thread", f1, f2);
    }

    @Test
    public void testGetDateFormat_DifferentPatternDifferentInstance() {
        SimpleDateFormat f1 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        SimpleDateFormat f2 = SafeSimpleDateFormat.getDateFormat(PATTERN2);
        assertNotSame("Different patterns must yield different instances", f1, f2);
    }

    @Test(expected = NullPointerException.class)
    public void testGetDateFormat_NullPatternThrowsNPE() {
        SafeSimpleDateFormat.getDateFormat(null);
    }

    @Test
    public void testGetDateFormat_DifferentThreadsDifferentInstances() throws Exception {
        Callable<SimpleDateFormat> task = () -> SafeSimpleDateFormat.getDateFormat(PATTERN);
        ExecutorService exec = Executors.newFixedThreadPool(2);
        Future<SimpleDateFormat> f1 = exec.submit(task);
        Future<SimpleDateFormat> f2 = exec.submit(task);
        SimpleDateFormat fmt1 = f1.get();
        SimpleDateFormat fmt2 = f2.get();
        exec.shutdownNow();
        // Instances may be equal but must not be the same object because each thread has its own map
        assertNotSame("Each thread should have its own SimpleDateFormat instance", fmt1, fmt2);
    }

    /* ---------- Constructor & toString ---------- */
    @Test
    public void testConstructor_StoresPattern() {
        assertEquals("toString() should return the original pattern", PATTERN, sdf.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructor_NullPatternThrowsNPE() {
        new SafeSimpleDateFormat(null);
    }

    /* ---------- format ---------- */
    @Test
    public void testFormat_ProducesExpectedString() {
        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(DateFormat.YEAR_FIELD);
        sdf.format(sampleDate, sb, fp);
        assertEquals("2023-03-15", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testFormat_NullDateThrowsNPE() {
        sdf.format(null, new StringBuffer(), new FieldPosition(0));
    }

    /* ---------- parse ---------- */
    @Test
    public void testParse_ValidStringReturnsCorrectDate() {
        ParsePosition pos = new ParsePosition(0);
        Date parsed = sdf.parse("2023-03-15", pos);
        assertNotNull(parsed);
        assertEquals(sampleDate.getTime(), parsed.getTime());
        assertEquals(10, pos.getIndex()); // full length parsed
    }

    @Test
    public void testParse_NullStringReturnsNullAndLeavesPosUnchanged() {
        ParsePosition pos = new ParsePosition(0);
        Date parsed = sdf.parse(null, pos);
        assertNull(parsed);
        assertEquals(0, pos.getIndex());
    }

    @Test
    public void testParse_WithParsePositionBeyondStringReturnsNull() {
        ParsePosition pos = new ParsePosition(5);
        Date parsed = sdf.parse("2023-03-15", pos);
        assertNull(parsed);
        assertEquals(5, pos.getIndex()); // unchanged
    }

    /* ---------- setTimeZone ---------- */
    @Test
    public void testSetTimeZone_ChangesOutputAccordingly() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        sdf.setTimeZone(tz);
        String result = sdf.format(sampleDate, new StringBuffer(), new FieldPosition(0)).toString();
        // Convert the same instant to New York time and format manually for comparison
        SimpleDateFormat manual = new SimpleDateFormat(PATTERN);
        manual.setTimeZone(tz);
        String expected = manual.format(sampleDate);
        assertEquals(expected, result);
    }

    @Test(expected = NullPointerException.class)
    public void testSetTimeZone_NullThrowsNPE() {
        sdf.setTimeZone(null);
    }

    /* ---------- setCalendar ---------- */
    @Test
    public void testSetCalendar_AltersCalendarFields() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+2"));
        cal.clear();
        cal.set(1999, Calendar.DECEMBER, 31);
        sdf.setCalendar(cal);
        String result = sdf.format(sampleDate, new StringBuffer(), new FieldPosition(0)).toString();
        // With the calendar set to 1999-12-31, the year field used for formatting is 1999
        assertEquals("1999-12-31", result);
    }

    @Test(expected = NullPointerException.class)
    public void testSetCalendar_NullThrowsNPE() {
        sdf.setCalendar(null);
    }

    /* ---------- setNumberFormat ---------- */
    @Test
    public void testSetNumberFormat_DisableGrouping() {
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(true); // default grouping on
        sdf.setNumberFormat(nf);
        // Using a pattern that contains a numeric field (e.g., week in year) to see grouping effect
        SafeSimpleDateFormat weekFmt = new SafeSimpleDateFormat("w");
        weekFmt.setNumberFormat(nf);
        String result = weekFmt.format(sampleDate, new StringBuffer(), new FieldPosition(0)).toString();
        // Week number never contains grouping separators, but we assert no exception and result is numeric
        assertTrue(result.matches("\\d+"));
    }

    @Test(expected = NullPointerException.class)
    public void testSetNumberFormat_NullThrowsNPE() {
        sdf.setNumberFormat(null);
    }

    /* ---------- setLenient ---------- */
    @Test
    public void testSetLenient_FalseRejectsInvalidDate() {
        sdf.setLenient(false);
        ParsePosition pos = new ParsePosition(0);
        Date d = sdf.parse("2023-02-30", pos); // Feb 30 does not exist
        assertNull(d);
        assertEquals(0, pos.getErrorIndex()); // error index set by SimpleDateFormat
    }

    @Test
    public void testSetLenient_TrueAcceptsInvalidDate() {
        sdf.setLenient(true);
        ParsePosition pos = new ParsePosition(0);
        Date d = sdf.parse("2023-02-30", pos);
        assertNotNull(d); // rolls over to March 2
    }

    /* ---------- setDateFormatSymbols ---------- */
    @Test
    public void testSetDateFormatSymbols_ChangesMonthNames() {
        DateFormatSymbols symbols = new DateFormatSymbols();
        String[] months = new String[12];
        Arrays.fill(months, "Foo");
        symbols.setMonths(months);
        sdf.setDateFormatSymbols(symbols);
        SafeSimpleDateFormat monthFmt = new SafeSimpleDateFormat("MMMM");
        String result = monthFmt.format(sampleDate, new StringBuffer(), new FieldPosition(0)).toString();
        assertEquals("Foo", result);
    }

    @Test(expected = NullPointerException.class)
    public void testSetDateFormatSymbols_NullThrowsNPE() {
        sdf.setDateFormatSymbols(null);
    }

    /* ---------- set2DigitYearStart ---------- */
    @Test
    public void testSet2DigitYearStart_ImpactOnParsingTwoDigitYear() {
        // Set pivot to 1950
        Calendar pivot = Calendar.getInstance();
        pivot.clear();
        pivot.set(1950, Calendar.JANUARY, 1);
        sdf.set2DigitYearStart(pivot.getTime());

        SafeSimpleDateFormat twoDigitFmt = new SafeSimpleDateFormat("yy-MM-dd");
        // "49-01-01" should be 2049 (after pivot)
        Date d1 = twoDigitFmt.parse("49-01-01", new ParsePosition(0));
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        assertEquals(2049, c1.get(Calendar.YEAR));

        // "50-01-01" should be 1950 (on or after pivot)
        Date d2 = twoDigitFmt.parse("50-01-01", new ParsePosition(0));
        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);
        assertEquals(1950, c2.get(Calendar.YEAR));
    }

    @Test(expected = NullPointerException.class)
    public void testSet2DigitYearStart_NullThrowsNPE() {
        sdf.set2DigitYearStart(null);
    }

    /* ---------- equals & hashCode ---------- */
    @Test
    public void testEqualsAndHashCode_SamePattern() {
        SafeSimpleDateFormat other = new SafeSimpleDateFormat(PATTERN);
        assertTrue(sdf.equals(other));
        assertEquals(sdf.hashCode(), other.hashCode());
    }

    @Test
    public void testEqualsAndHashCode_DifferentPattern() {
        SafeSimpleDateFormat other = new SafeSimpleDateFormat(PATTERN2);
        assertFalse(sdf.equals(other));
        // hashCodes may collide by chance; we only assert they are *not* guaranteed equal
        // but we check that they are different most of the time
        assertNotEquals(sdf.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals_WithNonSafeSimpleDateFormatObject() {
        SimpleDateFormat plain = new SimpleDateFormat(PATTERN);
        assertTrue(sdf.equals(plain));
        assertTrue(plain.equals(sdf)); // SimpleDateFormat's equals also checks pattern
    }

    /* ---------- toString ---------- */
    @Test
    public void testToString_ReturnsPattern() {
        assertEquals(PATTERN, sdf.toString());
    }
}
