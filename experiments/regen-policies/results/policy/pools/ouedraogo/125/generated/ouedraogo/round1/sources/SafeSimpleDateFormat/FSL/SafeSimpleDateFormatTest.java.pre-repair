package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.*;

import org.junit.Test;

public class SafeSimpleDateFormatTest {

    @Test
    public void testGetDateFormatSingletonPerThread() {
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat fmt1 = SafeSimpleDateFormat.getDateFormat(pattern);
        SimpleDateFormat fmt2 = SafeSimpleDateFormat.getDateFormat(pattern);
        assertSame("Same thread should receive same SimpleDateFormat instance", fmt1, fmt2);

        // Different pattern → different instance
        SimpleDateFormat fmtOther = SafeSimpleDateFormat.getDateFormat("dd/MM/yyyy");
        assertNotSame("Different pattern must yield different SimpleDateFormat", fmt1, fmtOther);
    }

    @Test
    public void testThreadLocalIsolation() throws Exception {
        final String pattern = "yyyy-MM-dd HH:mm:ss.SSS";
        Callable<SimpleDateFormat> task = () -> SafeSimpleDateFormat.getDateFormat(pattern);

        ExecutorService exec = Executors.newFixedThreadPool(2);
        Future<SimpleDateFormat> f1 = exec.submit(task);
        Future<SimpleDateFormat> f2 = exec.submit(task);
        SimpleDateFormat fmt1 = f1.get();
        SimpleDateFormat fmt2 = f2.get();

        // Two different threads must have distinct SimpleDateFormat objects
        assertNotSame("ThreadLocal should isolate SimpleDateFormat per thread", fmt1, fmt2);
        exec.shutdownNow();
    }

    @Test
    public void testFormatAndParseRoundTrip() {
        String pattern = "yyyy/MM/dd HH:mm:ss";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.MARCH, 15, 10, 30, 45);
        cal.set(Calendar.MILLISECOND, 0);
        Date original = cal.getTime();

        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        sdf.format(original, sb, fp);
        String formatted = sb.toString();

        // Parse back
        ParsePosition pos = new ParsePosition(0);
        Date parsed = sdf.parse(formatted, pos);
        assertNotNull(parsed);
        assertEquals("Parsed date must equal original", original, parsed);
        assertEquals("ParsePosition index should be at end of string", formatted.length(), pos.getIndex());
    }

    @Test
    public void testSetTimeZoneAffectsFormatting() {
        String pattern = "yyyy-MM-dd HH:mm:ss Z";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        // Force GMT+2
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        sdf.setTimeZone(tz);

        Calendar cal = Calendar.getInstance(tz);
        cal.set(2022, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        String formatted = sdf.format(date, new StringBuffer(), new FieldPosition(0)).toString();
        assertTrue("Formatted string must contain +0200 timezone offset", formatted.endsWith("+0200"));
    }

    @Test
    public void testSetLenientParsing() {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        sdf.setLenient(false);

        // Invalid date (Feb 30) should fail to parse when lenient is false
        ParsePosition pos = new ParsePosition(0);
        Date result = sdf.parse("2023-02-30", pos);
        assertNull("Parsing invalid date with lenient=false must return null", result);
        assertEquals("ParsePosition error index should be > 0", 8, pos.getErrorIndex());

        // Switch to lenient true – should succeed
        sdf.setLenient(true);
        pos = new ParsePosition(0);
        result = sdf.parse("2023-02-30", pos);
        assertNotNull("Parsing invalid date with lenient=true must succeed", result);
    }

    @Test
    public void testNumberFormatGroupingDisabled() {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        // Retrieve underlying SimpleDateFormat to inspect its NumberFormat
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        NumberFormat nf = underlying.getNumberFormat();
        assertFalse("NumberFormat grouping must be disabled by constructor", nf.isGroupingUsed());

        // Change NumberFormat to one that uses grouping and set back
        NumberFormat withGrouping = NumberFormat.getNumberInstance();
        withGrouping.setGroupingUsed(true);
        sdf.setNumberFormat(withGrouping);
        assertTrue("NumberFormat should now have grouping enabled", underlying.getNumberFormat().isGroupingUsed());
    }

    @Test
    public void testSetAndRetrieveCalendar() {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        Calendar customCal = Calendar.getInstance(TimeZone.getTimeZone("GMT-05:00"));
        customCal.set(1999, Calendar.DECEMBER, 31);
        sdf.setCalendar(customCal);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertEquals("Calendar time zone must be the one we set", customCal.getTimeZone(), underlying.getCalendar().getTimeZone());
        assertEquals("Calendar date must match the one we set", customCal.getTime(), underlying.getCalendar().getTime());
    }

    @Test
    public void testDateFormatSymbolsPropagation() {
        String pattern = "EEE, MMM d, yyyy";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        DateFormatSymbols symbols = new DateFormatSymbols();
        symbols.setMonths(new String[]{"Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven"});
        sdf.setDateFormatSymbols(symbols);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertArrayEquals("Months symbols should be propagated", symbols.getMonths(), underlying.getDateFormatSymbols().getMonths());
    }

    @Test
    public void testSet2DigitYearStart() {
        String pattern = "yy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        Calendar cal = Calendar.getInstance();
        cal.set(2000, Calendar.JANUARY, 1);
        Date start = cal.getTime();
        sdf.set2DigitYearStart(start);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertEquals("2-digit year start must be the date we set", start, underlying.get2DigitYearStart());
    }

    @Test
    public void testToStringEqualsHashCode() {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        assertEquals("toString should return the original pattern", pattern, sdf.toString());

        // equals should delegate to underlying SimpleDateFormat
        SimpleDateFormat other = new SimpleDateFormat(pattern);
        assertTrue("SafeSimpleDateFormat.equals should be true for equal underlying format", sdf.equals(other));

        SimpleDateFormat different = new SimpleDateFormat("dd/MM/yyyy");
        assertFalse("SafeSimpleDateFormat.equals should be false for different format", sdf.equals(different));

        // hashCode should match underlying
        assertEquals("hashCode must match underlying SimpleDateFormat", other.hashCode(), sdf.hashCode());
    }
}
