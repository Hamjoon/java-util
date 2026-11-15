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

/**
 * Comprehensive JUnit‑4 test suite for {@link SafeSimpleDateFormat}.
 *
 * The tests cover:
 * <ul>
 *   <li>Thread‑local caching of {@link SimpleDateFormat} instances.</li>
 *   <li>Constructor side‑effects (calendar reset, time‑zone, number format).</li>
 *   <li>All public methods – format, parse, setters, toString, equals, hashCode.</li>
 *   <li>Behaviour of lenient parsing, custom {@link DateFormatSymbols},
 *       2‑digit‑year start handling and time‑zone handling.</li>
 * </ul>
 *
 * No mocking frameworks are used – the tests instantiate real objects only.
 */
public class SafeSimpleDateFormatTest
{
    // -----------------------------------------------------------------------
    // Helper – clears the thread‑local map after each test to keep tests isolated.
    // -----------------------------------------------------------------------
    @After
    public void clearThreadLocal() throws Exception
    {
        Field tlField = SafeSimpleDateFormat.class.getDeclaredField("_dateFormats");
        tlField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Map<String, SimpleDateFormat>> tl =
                (ThreadLocal<Map<String, SimpleDateFormat>>) tlField.get(null);
        tl.remove(); // clears the map for the current thread
    }

    // -----------------------------------------------------------------------
    // 1. getDateFormat – same instance within a thread, different across threads
    // -----------------------------------------------------------------------
    @Test
    public void testGetDateFormatSameInstanceWithinThread()
    {
        SimpleDateFormat f1 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat f2 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        assertSame("Two calls with the same pattern in the same thread must return the same instance",
                f1, f2);
    }

    @Test
    public void testGetDateFormatDifferentInstancesAcrossThreads() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<SimpleDateFormat> ref1 = new AtomicReference<>();
        final AtomicReference<SimpleDateFormat> ref2 = new AtomicReference<>();

        Runnable r1 = () -> {
            ref1.set(SafeSimpleDateFormat.getDateFormat("yyyy/MM/dd"));
            latch.countDown();
        };
        Runnable r2 = () -> {
            ref2.set(SafeSimpleDateFormat.getDateFormat("yyyy/MM/dd"));
            latch.countDown();
        };

        Thread t1 = new Thread(r1);
        Thread t2 = new Thread(r2);
        t1.start();
        t2.start();
        latch.await(); // wait for both threads

        assertNotNull(ref1.get());
        assertNotNull(ref2.get());
        assertNotSame("Different threads must have different SimpleDateFormat instances",
                ref1.get(), ref2.get());
    }

    // -----------------------------------------------------------------------
    // 2. Constructor – verifies that the internal SimpleDateFormat is reset
    // -----------------------------------------------------------------------
    @Test
    public void testConstructorResetsCalendarAndNumberFormat()
    {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(pattern);

        // The underlying SimpleDateFormat instance is stored in the thread‑local map.
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);

        // Calendar must be cleared (all fields set to 0 or UNSET)
        Calendar cal = underlying.getCalendar();
        assertEquals("Calendar should be cleared – YEAR should be 0", 0, cal.get(Calendar.YEAR));
        assertEquals("Calendar should be cleared – MONTH should be 0", 0, cal.get(Calendar.MONTH));
        assertEquals("Calendar should be cleared – DAY_OF_MONTH should be 1", 1, cal.get(Calendar.DAY_OF_MONTH));

        // Time zone must be the JVM default (the constructor copies it from the cleared calendar)
        assertEquals(TimeZone.getDefault().getID(), underlying.getTimeZone().getID());

        // NumberFormat must have grouping disabled
        NumberFormat nf = underlying.getNumberFormat();
        assertFalse("NumberFormat grouping must be disabled", nf.isGroupingUsed());
    }

    // -----------------------------------------------------------------------
    // 3. format / parse – round‑trip test
    // -----------------------------------------------------------------------
    @Test
    public void testFormatAndParseRoundTrip()
    {
        String pattern = "yyyy-MM-dd HH:mm:ss.SSS Z";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2021, Calendar.MARCH, 15, 12, 34, 56);
        cal.set(Calendar.MILLISECOND, 123);
        Date original = cal.getTime();

        StringBuffer sb = new StringBuffer();
        sdf.format(original, sb, new FieldPosition(0));
        String formatted = sb.toString();

        // Parse back
        ParsePosition pos = new ParsePosition(0);
        Date parsed = sdf.parse(formatted, pos);
        assertNotNull("Parsed date must not be null", parsed);
        assertEquals("Round‑trip parsing must yield the original instant", original.getTime(), parsed.getTime());
        assertEquals("ParsePosition index must be at the end of the string", formatted.length(), pos.getIndex());
    }

    // -----------------------------------------------------------------------
    // 4. setTimeZone – the formatted string must contain the new offset
    // -----------------------------------------------------------------------
    @Test
    public void testSetTimeZoneAffectsFormatting()
    {
        String pattern = "yyyy-MM-dd HH:mm Z";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        TimeZone tz = TimeZone.getTimeZone("GMT+02:00");
        sdf.setTimeZone(tz);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2022, Calendar.JANUARY, 1, 0, 0, 0);
        Date date = cal.getTime();

        String formatted = sdf.format(date);
        // The offset for GMT+02:00 is +0200
        assertTrue("Formatted string must contain the new time‑zone offset +0200",
                formatted.endsWith(" +0200"));
    }

    // -----------------------------------------------------------------------
    // 5. setCalendar – ensure the new calendar is used for formatting
    // -----------------------------------------------------------------------
    @Test
    public void testSetCalendarChangesBehaviour()
    {
        String pattern = "yyyy-MM-dd HH:mm";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        Calendar customCal = Calendar.getInstance(TimeZone.getTimeZone("GMT-05:00"));
        customCal.clear();
        customCal.set(1999, Calendar.DECEMBER, 31, 23, 59);
        sdf.setCalendar(customCal);

        Date date = customCal.getTime();
        String formatted = sdf.format(date);
        // The formatted string should reflect the custom calendar’s time‑zone offset (-0500)
        assertTrue("Formatted output must contain -0500 offset when using custom calendar",
                formatted.endsWith(" -0500"));
    }

    // -----------------------------------------------------------------------
    // 6. setNumberFormat – verify that a custom NumberFormat is honoured
    // -----------------------------------------------------------------------
    @Test
    public void testSetNumberFormatIsHonoured()
    {
        String pattern = "yyyy-MM-dd G";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(true); // enable grouping for testing
        sdf.setNumberFormat(nf);

        // The pattern does not contain a numeric field, but we can still verify that the underlying
        // SimpleDateFormat now holds the custom NumberFormat.
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(pattern);
        assertTrue("Underlying NumberFormat must now have grouping enabled",
                underlying.getNumberFormat().isGroupingUsed());
    }

    // -----------------------------------------------------------------------
    // 7. setLenient – parsing an invalid date should fail when lenient is false
    // -----------------------------------------------------------------------
    @Test
    public void testSetLenientDisablesLenientParsing()
    {
        String pattern = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        sdf.setLenient(false);

        // February 30th is invalid; parsing should fail and leave position unchanged.
        ParsePosition pos = new ParsePosition(0);
        Date result = sdf.parse("2020-02-30", pos);
        assertNull("Parsing invalid date with lenient=false must return null", result);
        assertEquals("ParsePosition index must remain 0 on failure", 0, pos.getIndex());
    }

    // -----------------------------------------------------------------------
    // 8. setDateFormatSymbols – custom month names
    // -----------------------------------------------------------------------
    @Test
    public void testSetDateFormatSymbolsChangesMonthNames()
    {
        String pattern = "MMM";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        // Create a symbols object with a custom month name for March.
        String[] months = new String[12];
        for (int i = 0; i < months.length; i++) {
            months[i] = "M" + (i + 1);
        }
        months[2] = "FooMonth"; // March (zero‑based index 2)
        DateFormatSymbols symbols = new DateFormatSymbols();
        symbols.setMonths(months);
        sdf.setDateFormatSymbols(symbols);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2021, Calendar.MARCH, 1);
        String formatted = sdf.format(cal.getTime());

        assertEquals("Custom month name must be used after setting DateFormatSymbols",
                "FooMonth", formatted);
    }

    // -----------------------------------------------------------------------
    // 9. set2DigitYearStart – verify two‑digit year interpretation
    // -----------------------------------------------------------------------
    @Test
    public void testSet2DigitYearStartAffectsParsing()
    {
        String pattern = "yy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);

        // Set the start of the 2‑digit year range to 1950‑01‑01.
        Calendar startCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        startCal.clear();
        startCal.set(1950, Calendar.JANUARY, 1);
        sdf.set2DigitYearStart(startCal.getTime());

        // According to the rule, "49" maps to 2049 (because 49 >= (1950 % 100) ?).
        Date parsed = sdf.parse("49-01-01", new ParsePosition(0));
        Calendar resultCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        resultCal.setTime(parsed);
        assertEquals("Year should be 2049 for two‑digit year 49 with start 1950",
                2049, resultCal.get(Calendar.YEAR));
    }

    // -----------------------------------------------------------------------
    // 10. toString – must return the pattern supplied at construction
    // -----------------------------------------------------------------------
    @Test
    public void testToStringReturnsPattern()
    {
        String pattern = "dd/MM/yyyy";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        assertEquals("toString() must return the original pattern", pattern, sdf.toString());
    }

    // -----------------------------------------------------------------------
    // 11. equals & hashCode – delegate to underlying SimpleDateFormat
    // -----------------------------------------------------------------------
    @Test
    public void testEqualsAndHashCodeDelegateToUnderlying()
    {
        String pattern = "yyyyMMdd";
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat(pattern);
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat(pattern);
        SimpleDateFormat plain = SafeSimpleDateFormat.getDateFormat(pattern);

        // Equality with the underlying SimpleDateFormat
        assertTrue("SafeSimpleDateFormat should be equal to its underlying SimpleDateFormat",
                sdf1.equals(plain));

        // Equality between two SafeSimpleDateFormat objects with the same pattern
        // (delegates to SimpleDateFormat.equals which checks the pattern)
        assertTrue("Two SafeSimpleDateFormat instances with the same pattern must be equal",
                sdf1.equals(sdf2));

        // hashCode must match the underlying SimpleDateFormat's hashCode
        assertEquals("hashCode must be delegated to the underlying SimpleDateFormat",
                plain.hashCode(), sdf1.hashCode());
    }

    // -----------------------------------------------------------------------
    // 12. Additional sanity test – format method overload using String return
    // -----------------------------------------------------------------------
    @Test
    public void testConvenientFormatMethod()
    {
        // The class inherits the default DateFormat.format(Date) which calls the overridden
        // format(Date, StringBuffer, FieldPosition) method. This test simply ensures that
        // the convenience method works without throwing.
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        Date now = new Date();
        String result = sdf.format(now);
        assertNotNull("Convenient format(Date) should return a non‑null string", result);
        // Basic sanity check – length must be at least 10 (yyyy-MM-dd)
        assertTrue("Resulting string should have expected length", result.length() >= 10);
    }
}
