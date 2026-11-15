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
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link SafeSimpleDateFormat}.
 *
 * The tests cover:
 *   • basic contract of each public method,
 *   • typical usage (format/parse round‑trip),
 *   • edge‑cases and expected exceptions,
 *   • thread‑local isolation of the cached {@link SimpleDateFormat} instances,
 *   • delegation of {@code equals}, {@code hashCode} and {@code toString}.
 */
public class SafeSimpleDateFormatTest {

    /* -------------------------------------------------
       1.  Public static factory – caching behaviour
       ------------------------------------------------- */
    @Test
    public void testGetDateFormatCaching() {
        SimpleDateFormat f1 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat f2 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        assertSame("Same pattern must return the same cached instance", f1, f2);
    }

    @Test(expected = NullPointerException.class)
    public void testGetDateFormatNullPattern() {
        SafeSimpleDateFormat.getDateFormat(null);
    }

    /* -------------------------------------------------
       2.  Constructor
       ------------------------------------------------- */
    @Test
    public void testConstructorInitialisesWrapper() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        Date epoch = new Date(0L);                // 1970‑01‑01 00:00:00 UTC
        String out = sdf.format(epoch, new StringBuffer(), new FieldPosition(0)).toString();
        // The default timezone of the JVM is used – we only assert the pattern is applied
        assertEquals("1970-01-01", out);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullPattern() {
        new SafeSimpleDateFormat(null);
    }

    /* -------------------------------------------------
       3.  format(Date,…)
       ------------------------------------------------- */
    @Test
    public void testFormat() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy/MM/dd");
        Date d = new Date(1234567890000L); // 2009‑02‑13 23:31:30 UTC
        String out = sdf.format(d, new StringBuffer(), new FieldPosition(0)).toString();
        assertEquals("2009/02/13", out);
    }

    @Test(expected = NullPointerException.class)
    public void testFormatNullDate() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        sdf.format(null, new StringBuffer(), new FieldPosition(0));
    }

    /* -------------------------------------------------
       4.  parse(String,ParsePosition)
       ------------------------------------------------- */
    @Test
    public void testParseValidString() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyyMMdd");
        ParsePosition pp = new ParsePosition(0);
        Date d = sdf.parse("20201231", pp);
        assertNotNull("Parsing a valid string must return a Date", d);
        // The underlying SimpleDateFormat leaves the index at 0 when a ParsePosition is supplied.
        assertEquals(0, pp.getIndex());
    }

    @Test(expected = NullPointerException.class)
    public void testParseNullSource() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        sdf.parse(null, new ParsePosition(0));
    }

    @Test
    public void testParseEmptyString() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        ParsePosition pp = new ParsePosition(0);
        Date d = sdf.parse("", pp);
        assertNull("Empty source must yield null", d);
        assertEquals(0, pp.getIndex());
    }

    @Test
    public void testParseInvalidDateLenientFalse() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        ParsePosition pp = new ParsePosition(0);
        Date d = sdf.parse("2023-02-30", pp);
        assertNull("Invalid date with lenient=false must not be parsed", d);
    }

    /* -------------------------------------------------
       5.  setTimeZone(TimeZone)
       ------------------------------------------------- */
    @Test
    public void testSetTimeZone() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date d = new Date(0L);
        String out = sdf.format(d, new StringBuffer(), new FieldPosition(0)).toString();
        assertEquals("1970-01-01 00:00", out);
    }

    @Test(expected = NullPointerException.class)
    public void testSetTimeZoneNull() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(null);
    }

    /* -------------------------------------------------
       6.  setCalendar(Calendar)
       ------------------------------------------------- */
    @Test
    public void testSetCalendar() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(1999, Calendar.DECEMBER, 31);
        sdf.setCalendar(cal);
        String out = sdf.format(cal.getTime(), new StringBuffer(), new FieldPosition(0)).toString();
        assertEquals("1999-12-31", out);
    }

    @Test(expected = NullPointerException.class)
    public void testSetCalendarNull() {
        new SafeSimpleDateFormat("yyyy-MM-dd").setCalendar(null);
    }

    /* -------------------------------------------------
       7.  setNumberFormat(NumberFormat)
       ------------------------------------------------- */
    @Test
    public void testSetNumberFormat() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true);
        sdf.setNumberFormat(nf);
        // No direct observable effect on date formatting – test that no exception is thrown.
    }

    @Test(expected = NullPointerException.class)
    public void testSetNumberFormatNull() {
        new SafeSimpleDateFormat("yyyy-MM-dd").setNumberFormat(null);
    }

    /* -------------------------------------------------
       8.  setLenient(boolean)
       ------------------------------------------------- */
    @Test
    public void testSetLenientEffect() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        ParsePosition pp = new ParsePosition(0);
        Date d = sdf.parse("2023-02-30", pp);
        assertNull(d);
    }

    /* -------------------------------------------------
       9.  setDateFormatSymbols(DateFormatSymbols)
       ------------------------------------------------- */
    @Test
    public void testSetDateFormatSymbols() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("MMM dd, yyyy");
        DateFormatSymbols symbols = new DateFormatSymbols();
        String[] shortMonths = symbols.getShortMonths();
        shortMonths[0] = "JANUARY"; // replace Jan abbreviation
        symbols.setShortMonths(shortMonths);
        sdf.setDateFormatSymbols(symbols);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2022, Calendar.JANUARY, 5);
        String out = sdf.format(cal.getTime(), new StringBuffer(), new FieldPosition(0)).toString();
        assertTrue("Custom month abbreviation must be used", out.startsWith("JANUARY"));
    }

    @Test(expected = NullPointerException.class)
    public void testSetDateFormatSymbolsNull() {
        new SafeSimpleDateFormat("yyyy-MM-dd").setDateFormatSymbols(null);
    }

    /* -------------------------------------------------
       10. set2DigitYearStart(Date)
       ------------------------------------------------- */
    @Test
    public void testSet2DigitYearStart() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yy-MM-dd");
        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(2000, Calendar.JANUARY, 1);
        sdf.set2DigitYearStart(start.getTime());

        ParsePosition pp = new ParsePosition(0);
        Date d = sdf.parse("99-12-31", pp);
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        assertEquals(1999, cal.get(Calendar.YEAR));
    }

    @Test(expected = NullPointerException.class)
    public void testSet2DigitYearStartNull() {
        new SafeSimpleDateFormat("yy-MM-dd").set2DigitYearStart(null);
    }

    /* -------------------------------------------------
       11. toString()
       ------------------------------------------------- */
    @Test
    public void testToStringReturnsPattern() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("dd/MM/yyyy");
        assertEquals("dd/MM/yyyy", sdf.toString());
    }

    /* -------------------------------------------------
       12. equals(Object) & hashCode()
       ------------------------------------------------- */
    @Test
    public void testEqualsAndHashCodeDelegate() {
        SafeSimpleDateFormat wrapper = new SafeSimpleDateFormat("yyyyMMdd");
        SimpleDateFormat plain = new SimpleDateFormat("yyyyMMdd");
        assertTrue(wrapper.equals(plain));
        assertEquals(wrapper.hashCode(), plain.hashCode());

        // Different pattern → not equal
        SimpleDateFormat other = new SimpleDateFormat("yyyy-MM-dd");
        assertFalse(wrapper.equals(other));
    }

    @Test
    public void testEqualsWithDifferentType() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyyMMdd");
        assertFalse(sdf.equals("a string"));
    }

    /* -------------------------------------------------
       13. Thread‑local isolation of cached formatters
       ------------------------------------------------- */
    @Test
    public void testThreadLocalIsolation() throws Exception {
        final String pattern = "yyyy-MM-dd";
        final SafeSimpleDateFormat[] wrappers = new SafeSimpleDateFormat[2];
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch go = new CountDownLatch(1);

        Runnable task = () -> {
            wrappers[Integer.parseInt(Thread.currentThread().getName())] = new SafeSimpleDateFormat(pattern);
            ready.countDown();
            try { go.await(); } catch (InterruptedException ignored) {}
        };

        Thread t0 = new Thread(task, "0");
        Thread t1 = new Thread(task, "1");
        t0.start(); t1.start();

        // Wait until both threads have created their wrappers
        ready.await();
        // Release them – they will finish shortly
        go.countDown();
        t0.join(); t1.join();

        // The two wrapper instances are distinct objects (different thread‑local caches)
        assertNotSame("Wrappers created in different threads must be distinct objects", wrappers[0], wrappers[1]);

        // Their underlying SimpleDateFormat instances must also be distinct (thread‑local map)
        SimpleDateFormat f0 = SafeSimpleDateFormat.getDateFormat(pattern); // from main thread
        SimpleDateFormat f1 = wrappers[0].getDateFormat(pattern);
        SimpleDateFormat f2 = wrappers[1].getDateFormat(pattern);
        assertNotSame(f0, f1);
        assertNotSame(f0, f2);
        // f1 and f2 may be the same if the JVM re‑uses the same SimpleDateFormat instance inside each thread,
        // but they are **not** visible to the main thread – the important contract is that the main thread does
        // not see cross‑thread contamination.
    }

    // Helper to expose the protected getDateFormat method for the thread‑local test.
    // (The method is package‑private, so we can call it directly from this test class.)
    private SimpleDateFormat getDateFormat(String pattern) {
        return SafeSimpleDateFormat.getDateFormat(pattern);
    }
}
