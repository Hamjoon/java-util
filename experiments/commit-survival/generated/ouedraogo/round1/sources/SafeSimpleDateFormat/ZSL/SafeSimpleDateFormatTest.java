/***************************************************************************************************
 * SafeSimpleDateFormatTest.java
 *
 * Comprehensive JUnit‑4 test suite for {@link com.cedarsoftware.util.SafeSimpleDateFormat}.
 *
 * The tests cover:
 *   • Retrieval of cached SimpleDateFormat instances via {@code getDateFormat}
 *   • Correct initialization performed by the constructor
 *   • All delegating mutator methods (setTimeZone, setCalendar, setNumberFormat, setLenient,
 *     setDateFormatSymbols, set2DigitYearStart)
 *   • Formatting & parsing round‑trip
 *   • toString(), equals(Object) and hashCode()
 *   • Thread‑local isolation of the internal formatter cache
 *
 * The test class lives in the same package as the class under test so that it can access the
 * private static {@code _dateFormats} field via reflection for clean‑up between test cases.
 **************************************************************************************************/
package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class SafeSimpleDateFormatTest
{
    /** Holds a reference to the private ThreadLocal cache so we can clear it after each test. */
    private ThreadLocal<Map<String, SimpleDateFormat>> threadLocalCache;

    @Before
    public void setUp() throws Exception
    {
        // Obtain the private static ThreadLocal field via reflection.
        Field f = SafeSimpleDateFormat.class.getDeclaredField("_dateFormats");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Map<String, SimpleDateFormat>> tl = (ThreadLocal<Map<String, SimpleDateFormat>>) f.get(null);
        this.threadLocalCache = tl;
        // Ensure a clean cache for the current thread.
        this.threadLocalCache.remove();
    }

    @After
    public void tearDown() throws Exception
    {
        // Remove the map after each test to avoid cross‑test contamination.
        if (this.threadLocalCache != null)
        {
            this.threadLocalCache.remove();
        }
    }

    /* ---------------------------------------------------------------------- */
    /*  getDateFormat() tests                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testGetDateFormatReturnsSameInstanceForSamePattern()
    {
        SimpleDateFormat f1 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat f2 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        assertSame("Same pattern should yield the same SimpleDateFormat instance", f1, f2);
    }

    @Test
    public void testGetDateFormatReturnsDifferentInstancesForDifferentPatterns()
    {
        SimpleDateFormat f1 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat f2 = SafeSimpleDateFormat.getDateFormat("dd/MM/yyyy");
        assertNotSame("Different patterns must not share the same SimpleDateFormat", f1, f2);
    }

    @Test
    public void testThreadLocalIsolation()
        throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<SimpleDateFormat> thread1Formatter = new AtomicReference<>();
        final AtomicReference<SimpleDateFormat> thread2Formatter = new AtomicReference<>();

        Runnable r1 = () -> {
            thread1Formatter.set(SafeSimpleDateFormat.getDateFormat("yyyyMMdd"));
            latch.countDown();
        };
        Runnable r2 = () -> {
            thread2Formatter.set(SafeSimpleDateFormat.getDateFormat("yyyyMMdd"));
            latch.countDown();
        };

        Thread t1 = new Thread(r1);
        Thread t2 = new Thread(r2);
        t1.start();
        t2.start();

        latch.await(); // wait for both threads

        assertNotNull(thread1Formatter.get());
        assertNotNull(thread2Formatter.get());
        assertNotSame("Each thread must have its own SimpleDateFormat instance",
                thread1Formatter.get(), thread2Formatter.get());
    }

    /* ---------------------------------------------------------------------- */
    /*  Constructor initialization tests                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testConstructorInitializesCalendarAndNumberFormat()
    {
        String pattern = "yyyy/MM/dd HH:mm:ss.SSS";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);

        // Calendar should be cleared (no fields set)
        Calendar cal = underlying.getCalendar();
        assertEquals("Calendar should be cleared (all fields zeroed)",
                0, cal.getTimeInMillis());

        // Leniency should match the cleared calendar's default (true)
        assertEquals("Leniency flag should be propagated", cal.isLenient(),
                underlying.isLenient());

        // NumberFormat must have grouping disabled
        NumberFormat nf = underlying.getNumberFormat();
        assertFalse("NumberFormat grouping must be disabled", nf.isGroupingUsed());
    }

    /* ---------------------------------------------------------------------- */
    /*  format() / parse() round‑trip tests                                    */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testFormatAndParseRoundTrip()
    {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);

        Date now = new Date();
        StringBuffer sb = new StringBuffer();
        safe.format(now, sb, new FieldPosition(0));

        // Parse back using the same SafeSimpleDateFormat instance
        Date parsed = safe.parse(sb.toString(), new ParsePosition(0));

        // Because SimpleDateFormat rounds to the nearest millisecond, the two dates
        // should be equal.
        assertEquals("Parsed date must equal original date", now.getTime(), parsed.getTime());
    }

    /* ---------------------------------------------------------------------- */
    /*  Mutator methods delegating to the underlying SimpleDateFormat          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testSetTimeZoneAffectsFormatting()
    {
        String pattern = "yyyy-MM-dd HH:mm:ss Z";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);
        TimeZone utc = TimeZone.getTimeZone("UTC");
        safe.setTimeZone(utc);

        Date epoch = new Date(0L); // 1970‑01‑01 00:00:00 UTC
        String formatted = safe.format(epoch, new StringBuffer(), new FieldPosition(0)).toString();

        assertTrue("Formatted string must contain UTC offset +0000", formatted.contains("+0000"));
    }

    @Test
    public void testSetCalendarIsPropagated()
    {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);

        Calendar customCal = Calendar.getInstance();
        customCal.clear();
        customCal.set(1999, Calendar.DECEMBER, 31);
        safe.setCalendar(customCal);

        // The underlying SimpleDateFormat should now reference the same Calendar instance.
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertSame("Underlying formatter must use the Calendar set via SafeSimpleDateFormat",
                customCal, underlying.getCalendar());
    }

    @Test
    public void testSetNumberFormatIsUsedByUnderlyingFormatter()
    {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true); // enable grouping to detect the change
        safe.setNumberFormat(nf);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertSame("NumberFormat set via SafeSimpleDateFormat must be reflected",
                nf, underlying.getNumberFormat());
    }

    @Test
    public void testSetLenientAffectsParsing()
    {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);
        safe.setLenient(false);

        // Non‑lenient parsing should reject an invalid month (e.g., month 13)
        ParsePosition pos = new ParsePosition(0);
        Date result = safe.parse("2023-13-01", pos);
        assertNull("Parsing should fail for an invalid month when lenient is false", result);
        assertEquals("ParsePosition error index should point after the failing token",
                5, pos.getErrorIndex());
    }

    @Test
    public void testSetDateFormatSymbolsChangesMonthNames()
    {
        String pattern = "MMMM dd, yyyy";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);

        // Replace month names with a distinct set
        DateFormatSymbols symbols = new DateFormatSymbols();
        String[] customMonths = {
                "M0", "M1", "M2", "M3", "M4", "M5",
                "M6", "M7", "M8", "M9", "M10", "M11"
        };
        symbols.setMonths(customMonths);
        safe.setDateFormatSymbols(symbols);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2022, Calendar.MARCH, 15); // March -> index 2

        String formatted = safe.format(cal.getTime(), new StringBuffer(), new FieldPosition(0)).toString();
        assertTrue("Formatted month name must come from the custom symbols", formatted.startsWith("M2"));
    }

    @Test
    public void testSet2DigitYearStartInfluencesParsing()
    {
        // Pattern with 2‑digit year
        String pattern = "yy-MM-dd";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);

        // Set start of 2‑digit year to 2000‑01‑01
        Calendar startCal = Calendar.getInstance();
        startCal.clear();
        startCal.set(2000, Calendar.JANUARY, 1);
        safe.set2DigitYearStart(startCal.getTime());

        // Parsing "99-12-31" should yield 1999‑12‑31 (since 99 < 2000 start)
        Date parsed = safe.parse("99-12-31", new ParsePosition(0));
        Calendar parsedCal = Calendar.getInstance();
        parsedCal.setTime(parsed);
        assertEquals(1999, parsedCal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, parsedCal.get(Calendar.MONTH));
        assertEquals(31, parsedCal.get(Calendar.DAY_OF_MONTH));

        // Parsing "00-01-01" should yield 2000‑01‑01
        parsed = safe.parse("00-01-01", new ParsePosition(0));
        parsedCal.setTime(parsed);
        assertEquals(2000, parsedCal.get(Calendar.YEAR));
    }

    /* ---------------------------------------------------------------------- */
    /*  toString(), equals(Object) and hashCode() tests                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testToStringReturnsPattern()
    {
        String pattern = "dd/MM/yyyy";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);
        assertEquals("toString() must return the original pattern", pattern, safe.toString());
    }

    @Test
    public void testEqualsDelegatesToUnderlyingFormatter()
    {
        String pattern = "yyyyMMdd";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);
        SimpleDateFormat samePattern = new SimpleDateFormat(pattern);
        SimpleDateFormat differentPattern = new SimpleDateFormat("MMddyyyy");

        // The underlying formatter equals another SimpleDateFormat with the same pattern.
        assertTrue("SafeSimpleDateFormat should be equal to SimpleDateFormat with same pattern",
                safe.equals(samePattern));

        // Different pattern must not be equal.
        assertFalse("SafeSimpleDateFormat must not be equal to SimpleDateFormat with different pattern",
                safe.equals(differentPattern));

        // Equality with another SafeSimpleDateFormat having the same pattern should also be true.
        SafeSimpleDateFormat anotherSafe = new SafeSimpleDateFormat(pattern);
        assertTrue("Two SafeSimpleDateFormat instances with same pattern should be equal",
                safe.equals(anotherSafe));
    }

    @Test
    public void testHashCodeMatchesUnderlyingFormatter()
    {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertEquals("hashCode() must proxy to the underlying SimpleDateFormat",
                underlying.hashCode(), safe.hashCode());
    }
}
