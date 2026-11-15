package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class SafeSimpleDateFormatTest {

    /* ---------------------------------------------------------------------- */
    /* 1.  List of public methods (including the constructor)                    */
    /* ---------------------------------------------------------------------- */
    // static SimpleDateFormat getDateFormat(String format)
    // SafeSimpleDateFormat(String format)                // constructor
    // StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition)
    // Date parse(String source, ParsePosition pos)
    // void setTimeZone(TimeZone tz)
    // void setCalendar(Calendar cal)
    // void setNumberFormat(NumberFormat format)
    // void setLenient(boolean lenient)
    // void setDateFormatSymbols(DateFormatSymbols symbols)
    // void set2DigitYearStart(Date date)
    // String toString()
    // boolean equals(Object other)
    // int hashCode()

    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    /* ---------------------------------------------------------------------- */
    /* 2.  Basic functionality tests                                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDateFormatReturnsSameInstanceWithinThread() {
        SimpleDateFormat fmt1 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        SimpleDateFormat fmt2 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertSame("Subsequent calls in same thread should return the same SimpleDateFormat instance",
                fmt1, fmt2);
    }

    @Test
    public void testConstructorInitialisesInternalState() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        // The toString() method simply returns the pattern string.
        assertEquals(PATTERN, sdf.toString());
    }

    @Test
    public void testFormatProducesExpectedString() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.MARCH, 15, 10, 20, 30);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        sdf.format(date, sb, fp);

        SimpleDateFormat reference = new SimpleDateFormat(PATTERN);
        assertEquals(reference.format(date), sb.toString());
    }

    @Test
    public void testParseValidString() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        String text = "2023-03-15 10:20:30";
        ParsePosition pp = new ParsePosition(0);
        Date parsed = sdf.parse(text, pp);
        assertNotNull(parsed);
        assertEquals(0, pp.getIndex()); // SimpleDateFormat does not advance the index for parse(String,ParsePosition)
    }

    @Test
    public void testParseInvalidStringReturnsNull() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        String text = "invalid-date";
        ParsePosition pp = new ParsePosition(0);
        Date parsed = sdf.parse(text, pp);
        assertNull(parsed);
        assertEquals(0, pp.getErrorIndex());
    }

    @Test
    public void testSetTimeZoneChangesInternalFormatter() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        TimeZone tz = TimeZone.getTimeZone("GMT+2");
        sdf.setTimeZone(tz);

        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertEquals(tz, internal.getTimeZone());
    }

    @Test
    public void testSetCalendarChangesInternalFormatter() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-5"));
        sdf.setCalendar(cal);

        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertEquals(cal, internal.getCalendar());
    }

    @Test
    public void testSetNumberFormatChangesInternalFormatter() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        NumberFormat nf = NumberFormat.getIntegerInstance();
        nf.setGroupingUsed(true);
        sdf.setNumberFormat(nf);

        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertEquals(nf, internal.getNumberFormat());
    }

    @Test
    public void testSetLenient() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        sdf.setLenient(false);
        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertFalse(internal.isLenient());
    }

    @Test
    public void testSetDateFormatSymbols() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        DateFormatSymbols symbols = new DateFormatSymbols();
        symbols.setMonths(new String[]{"Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"});
        sdf.setDateFormatSymbols(symbols);

        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertArrayEquals(symbols.getMonths(), internal.getDateFormatSymbols().getMonths());
    }

    @Test
    public void testSet2DigitYearStart() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        Calendar cal = Calendar.getInstance();
        cal.set(2000, Calendar.JANUARY, 1);
        Date start = cal.getTime();
        sdf.set2DigitYearStart(start);

        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertEquals(start, internal.get2DigitYearStart());
    }

    @Test
    public void testToStringReturnsPattern() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        assertEquals(PATTERN, sdf.toString());
    }

    @Test
    public void testEqualsAndHashCodeConsistency() {
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat(PATTERN);
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat(PATTERN);
        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);

        // Equality with the underlying SimpleDateFormat should be true
        assertTrue(sdf1.equals(internal));
        assertTrue(internal.equals(sdf1));

        // Two SafeSimpleDateFormat instances are NOT equal because the delegate
        // SimpleDateFormat does not consider SafeSimpleDateFormat as a DateFormat.
        assertFalse(sdf1.equals(sdf2));

        // hashCode should delegate to the internal SimpleDateFormat
        assertEquals(internal.hashCode(), sdf1.hashCode());
    }

    /* ---------------------------------------------------------------------- */
    /* 3.  Edge‑case and exception handling tests                              */
    /* ---------------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testGetDateFormatNullPatternThrows() {
        SafeSimpleDateFormat.getDateFormat(null);
    }

    @Test(expected = NullPointerException.class)
    public void testFormatNullDateThrows() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        sdf.format(null, new StringBuffer(), new FieldPosition(0));
    }

    @Test(expected = NullPointerException.class)
    public void testParseNullSourceThrows() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        sdf.parse(null, new ParsePosition(0));
    }

    @Test(expected = NullPointerException.class)
    public void testSetTimeZoneNullThrows() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        sdf.setTimeZone(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSetCalendarNullThrows() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        sdf.setCalendar(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSetNumberFormatNullThrows() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        sdf.setNumberFormat(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSetDateFormatSymbolsNullThrows() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        sdf.setDateFormatSymbols(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSet2DigitYearStartNullThrows() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        sdf.set2DigitYearStart(null);
    }

    @Test
    public void testThreadLocalIsolation() throws Exception {
        final String pattern = "yyyyMMdd";
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(2);
        final AtomicReference<SimpleDateFormat> thread1Formatter = new AtomicReference<>();
        final AtomicReference<SimpleDateFormat> thread2Formatter = new AtomicReference<>();

        Runnable task1 = () -> {
            try {
                startLatch.await(); // wait for both threads to be ready
                thread1Formatter.set(SafeSimpleDateFormat.getDateFormat(pattern));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        Runnable task2 = () -> {
            try {
                startLatch.await();
                thread2Formatter.set(SafeSimpleDateFormat.getDateFormat(pattern));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        };

        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        t1.start();
        t2.start();

        // Release both threads
        startLatch.countDown();

        // Wait for both to finish
        doneLatch.await();

        assertNotNull(thread1Formatter.get());
        assertNotNull(thread2Formatter.get());

        // Because each thread has its own ThreadLocal map, the formatter instances must be different.
        assertNotSame("Formatter instances from different threads must not be the same object",
                thread1Formatter.get(), thread2Formatter.get());
    }

    /* ---------------------------------------------------------------------- */
    /* 4.  Clean‑up (optional) – reset ThreadLocal map for isolation between tests */
    /* ---------------------------------------------------------------------- */

    @After
    public void clearThreadLocal() throws Exception {
        // Use reflection to clear the ThreadLocal map between test methods.
        // This avoids cross‑test contamination when the same thread runs multiple tests.
        java.lang.reflect.Field field = SafeSimpleDateFormat.class.getDeclaredField("_dateFormats");
        field.setAccessible(true);
        ThreadLocal<?> tl = (ThreadLocal<?>) field.get(null);
        tl.remove();
    }
}
