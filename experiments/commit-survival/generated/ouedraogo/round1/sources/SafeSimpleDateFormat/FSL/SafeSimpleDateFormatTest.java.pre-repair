package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.text.ParsePosition;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.*;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link SafeSimpleDateFormat}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct creation and caching of {@link SimpleDateFormat} instances via the thread‑local map.</li>
 *   <li>Formatting and parsing behaviour (including edge cases such as lenient parsing).</li>
 *   <li>Propagation of configuration changes (time‑zone, calendar, number format, leniency, symbols, 2‑digit year start).</li>
 *   <li>Object contract methods {@code toString()}, {@code equals(Object)} and {@code hashCode()}.</li>
 *   <li>Thread‑safety – different threads obtain independent caches while the same thread re‑uses the cached formatter.</li>
 * </ul>
 */
public class SafeSimpleDateFormatTest {

    /** Helper that forces the creation of a SafeSimpleDateFormat and returns the underlying SimpleDateFormat. */
    private SimpleDateFormat underlyingFormatter(String pattern) {
        SafeSimpleDateFormat ssdf = new SafeSimpleDateFormat(pattern);
        // Use reflection to reach the private static ThreadLocal map.
        // The public API gives us the same instance via getDateFormat().
        return SafeSimpleDateFormat.getDateFormat(pattern);
    }

    @Test
    public void testGetDateFormatCachingWithinSameThread() {
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat fmt1 = SafeSimpleDateFormat.getDateFormat(pattern);
        SimpleDateFormat fmt2 = SafeSimpleDateFormat.getDateFormat(pattern);

        // Same thread must receive the same SimpleDateFormat instance
        assertSame("Formatter should be cached per thread", fmt1, fmt2);
    }

    @Test
    public void testGetDateFormatIsolationBetweenThreads() throws Exception {
        final String pattern = "dd/MM/yyyy";
        Callable<SimpleDateFormat> task = () -> SafeSimpleDateFormat.getDateFormat(pattern);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<SimpleDateFormat> f1 = executor.submit(task);
        Future<SimpleDateFormat> f2 = executor.submit(task);

        SimpleDateFormat fmt1 = f1.get();
        SimpleDateFormat fmt2 = f2.get();

        // Different threads must have distinct formatter instances
        assertNotSame("Different threads must not share the same formatter instance", fmt1, fmt2);
        executor.shutdownNow();
    }

    @Test
    public void testFormatAndParseRoundTrip() {
        String pattern = "yyyy/MM/dd HH:mm:ss.SSS";
        SafeSimpleDateFormat ssdf = new SafeSimpleDateFormat(pattern);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.MARCH, 15, 12, 30, 45);
        cal.set(Calendar.MILLISECOND, 123);
        Date original = cal.getTime();

        StringBuffer sb = new StringBuffer();
        ssdf.format(original, sb, new FieldPosition(0));
        String formatted = sb.toString();

        // Parse back
        ParsePosition pos = new ParsePosition(0);
        Date parsed = ssdf.parse(formatted, pos);
        assertNotNull("Parsed date should not be null", parsed);
        assertEquals("Round‑trip date should be equal", original, parsed);
        assertEquals("Parse position should be at end of string", formatted.length(), pos.getIndex());
    }

    @Test
    public void testLenientParsingBehavior() {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat ssdf = new SafeSimpleDateFormat(pattern);
        ssdf.setLenient(false); // strict parsing

        ParsePosition pos = new ParsePosition(0);
        Date d = ssdf.parse("2023-02-30", pos); // invalid date
        assertNull("Strict parsing should fail for invalid date", d);
        assertTrue("Error index should be set after failure", pos.getErrorIndex() > 0);
    }

    @Test
    public void testTimeZonePropagation() {
        String pattern = "yyyy-MM-dd HH:mm";
        SafeSimpleDateFormat ssdf = new SafeSimpleDateFormat(pattern);

        TimeZone utc = TimeZone.getTimeZone("UTC");
        TimeZone pst = TimeZone.getTimeZone("America/Los_Angeles");
        ssdf.setTimeZone(pst);

        // Verify that the underlying formatter uses the new zone
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertEquals("Underlying formatter must have the PST zone", pst, underlying.getTimeZone());

        // Formatting a known instant should reflect the set zone
        Calendar cal = Calendar.getInstance(utc);
        cal.set(2023, Calendar.JANUARY, 1, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date instant = cal.getTime();

        String formatted = ssdf.format(instant, new StringBuffer(), new FieldPosition(0)).toString();
        // In PST (UTC‑8) the same instant is 2022‑12‑31 16:00
        assertTrue("Formatted string should contain PST offset date",
                formatted.startsWith("2022-12-31 16:00"));
    }

    @Test
    public void testCalendarPropagation() {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat ssdf = new SafeSimpleDateFormat(pattern);

        Calendar customCal = Calendar.getInstance();
        customCal.setFirstDayOfWeek(Calendar.SUNDAY);
        ssdf.setCalendar(customCal);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertSame("Underlying formatter must reference the same Calendar instance",
                customCal, underlying.getCalendar());
    }

    @Test
    public void testNumberFormatNoGrouping() {
        String pattern = "yyyyMMdd";
        SafeSimpleDateFormat ssdf = new SafeSimpleDateFormat(pattern);

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(true); // enable grouping deliberately
        ssdf.setNumberFormat(nf);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertFalse("Underlying formatter must have grouping disabled",
                underlying.getNumberFormat().isGroupingUsed());
    }

    @Test
    public void testDateFormatSymbolsPropagation() {
        String pattern = "EEE, MMM d, yyyy";
        SafeSimpleDateFormat ssdf = new SafeSimpleDateFormat(pattern);

        DateFormatSymbols symbols = new DateFormatSymbols();
        symbols.setMonths(new String[] {"Zero", "One", "Two", "Three", "Four", "Five",
                "Six", "Seven", "Eight", "Nine", "Ten", "Eleven"});
        ssdf.setDateFormatSymbols(symbols);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertArrayEquals("Month symbols must be propagated",
                symbols.getMonths(),
                underlying.getDateFormatSymbols().getMonths());
    }

    @Test
    public void testTwoDigitYearStart() {
        String pattern = "yyMMdd";
        SafeSimpleDateFormat ssdf = new SafeSimpleDateFormat(pattern);

        Calendar cal = Calendar.getInstance();
        cal.set(1950, Calendar.JANUARY, 1);
        Date startDate = cal.getTime();

        ssdf.set2DigitYearStart(startDate);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertEquals("Two‑digit year start must be set on underlying formatter",
                startDate, underlying.get2DigitYearStart());
    }

    @Test
    public void testToStringReturnsPattern() {
        String pattern = "dd-MMM-yyyy";
        SafeSimpleDateFormat ssdf = new SafeSimpleDateFormat(pattern);
        assertEquals("toString should return the original pattern", pattern, ssdf.toString());
    }

    @Test
    public void testEqualsAndHashCodeDelegateToUnderlyingFormatter() {
        String pattern = "yyyyMMdd";
        SafeSimpleDateFormat ssdf1 = new SafeSimpleDateFormat(pattern);
        SafeSimpleDateFormat ssdf2 = new SafeSimpleDateFormat(pattern);

        // The underlying SimpleDateFormat instances are equal, therefore equals() should be true
        assertTrue("Equals should delegate to underlying SimpleDateFormat", ssdf1.equals(ssdf2));

        // hashCode must match the underlying formatter's hashCode
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertEquals("hashCode must delegate to underlying SimpleDateFormat",
                underlying.hashCode(), ssdf1.hashCode());
    }

    @Test
    public void testEqualsWithNonFormatterObject() {
        SafeSimpleDateFormat ssdf = new SafeSimpleDateFormat("yyyy");
        assertFalse("Equals should return false for unrelated object", ssdf.equals("some string"));
    }

    @Test
    public void testConstructorResetsState() {
        String pattern = "yyyy/MM/dd";
        SafeSimpleDateFormat ssdf = new SafeSimpleDateFormat(pattern);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        Calendar cal = underlying.getCalendar();

        // Calendar should be cleared (all fields set to 0 or undefined)
        assertEquals("Calendar should be cleared", 0, cal.get(Calendar.YEAR));
        assertEquals("Calendar should be cleared", 0, cal.get(Calendar.MONTH));
        assertEquals("Calendar should be cleared", 0, cal.get(Calendar.DAY_OF_MONTH));

        // Leniency should match the cleared calendar's leniency (default true)
        assertEquals("Leniency must match cleared calendar", cal.isLenient(), underlying.isLenient());

        // TimeZone must be the default of the cleared calendar
        assertEquals("TimeZone must match cleared calendar", cal.getTimeZone(), underlying.getTimeZone());

        // NumberFormat must have grouping disabled
        assertFalse("NumberFormat grouping must be disabled", underlying.getNumberFormat().isGroupingUsed());
    }
}
