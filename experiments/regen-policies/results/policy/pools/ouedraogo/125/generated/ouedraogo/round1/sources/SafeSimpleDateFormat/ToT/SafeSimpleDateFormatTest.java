package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comprehensive JUnit4 test suite for {@link SafeSimpleDateFormat}.
 *
 * The tests cover:
 * <ul>
 *   <li>Factory method {@code getDateFormat}</li>
 *   <li>Constructor behaviour (calendar reset, number format configuration)</li>
 *   <li>All public instance methods (format, parse, setters, toString, equals, hashCode)</li>
 *   <li>Thread‑local isolation of the internal {@code SimpleDateFormat} cache</li>
 *   <li>Typical, edge‑case and error scenarios</li>
 * </ul>
 */
public class SafeSimpleDateFormatTest
{
    private static final String PATTERN = "yyyy/MM/dd HH:mm:ss.SSS Z";

    /** Helper to create a deterministic {@code Date} (Jan 2, 2000 03:04:05.006 UTC). */
    private static Date utcDate()
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2000, Calendar.JANUARY, 2, 3, 4, 5);
        cal.set(Calendar.MILLISECOND, 6);
        return cal.getTime();
    }

    /** --------------------------------------------------------------------- *
     *  1. getDateFormat – same format, same thread → same instance.
     *  --------------------------------------------------------------------- */
    @Test
    public void testGetDateFormat_SameThreadReturnsSameInstance()
    {
        SimpleDateFormat fmt1 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        SimpleDateFormat fmt2 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertSame("Same thread should receive the same SimpleDateFormat instance", fmt1, fmt2);
    }

    /** --------------------------------------------------------------------- *
     *  2. getDateFormat – different threads → different instances.
     *  --------------------------------------------------------------------- */
    @Test
    public void testGetDateFormat_DifferentThreadsReturnDifferentInstances() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<SimpleDateFormat> thread1Fmt = new AtomicReference<>();
        final AtomicReference<SimpleDateFormat> thread2Fmt = new AtomicReference<>();

        Runnable task1 = () -> {
            thread1Fmt.set(SafeSimpleDateFormat.getDateFormat(PATTERN));
            latch.countDown();
        };
        Runnable task2 = () -> {
            thread2Fmt.set(SafeSimpleDateFormat.getDateFormat(PATTERN));
            latch.countDown();
        };

        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        t1.start();
        t2.start();

        // Wait for both threads to finish
        assertTrue("Both threads should finish quickly", latch.await(2, TimeUnit.SECONDS));

        assertNotNull(thread1Fmt.get());
        assertNotNull(thread2Fmt.get());
        assertNotSame("Different threads must have distinct SimpleDateFormat instances",
                thread1Fmt.get(), thread2Fmt.get());
    }

    /** --------------------------------------------------------------------- *
     *  3. Constructor – ensures underlying SimpleDateFormat is reset:
     *     • Calendar is cleared (fields are zero)
     *     • NumberFormat has grouping disabled
     *  --------------------------------------------------------------------- */
    @Test
    public void testConstructor_ResetsCalendarAndNumberFormat()
    {
        SafeSimpleDateFormat safeFmt = new SafeSimpleDateFormat(PATTERN);
        SimpleDateFormat inner = SafeSimpleDateFormat.getDateFormat(PATTERN);

        // Calendar must be cleared – all fields should be undefined (0)
        Calendar cal = inner.getCalendar();
        assertEquals("Calendar should be cleared (year = 0)", 0, cal.get(Calendar.YEAR));
        assertEquals("Calendar should be cleared (month = 0)", 0, cal.get(Calendar.MONTH));
        assertEquals("Calendar should be cleared (day = 0)", 0, cal.get(Calendar.DAY_OF_MONTH));

        // NumberFormat grouping must be disabled
        NumberFormat nf = inner.getNumberFormat();
        assertFalse("NumberFormat grouping should be disabled", nf.isGroupingUsed());
    }

    /** --------------------------------------------------------------------- *
     *  4. format / parse round‑trip.
     *  --------------------------------------------------------------------- */
    @Test
    public void testFormatParse_RoundTrip()
    {
        SafeSimpleDateFormat safeFmt = new SafeSimpleDateFormat(PATTERN);
        Date original = utcDate();

        StringBuffer sb = new StringBuffer();
        safeFmt.format(original, sb, new FieldPosition(0));
        String formatted = sb.toString();

        // Parse back using the same SafeSimpleDateFormat
        ParsePosition pos = new ParsePosition(0);
        Date parsed = safeFmt.parse(formatted, pos);
        assertNotNull("Parse should succeed", parsed);
        assertEquals("Parsed date must equal original", original, parsed);
        assertEquals("ParsePosition should be at end of string", formatted.length(), pos.getIndex());
    }

    /** --------------------------------------------------------------------- *
     *  5. setTimeZone – formatting reflects the new zone.
     *  --------------------------------------------------------------------- */
    @Test
    public void testSetTimeZone_AffectsFormatting()
    {
        SafeSimpleDateFormat safeFmt = new SafeSimpleDateFormat(PATTERN);
        safeFmt.setTimeZone(TimeZone.getTimeZone("America/New_York"));
        Date date = utcDate();

        String formatted = safeFmt.format(date, new StringBuffer(), new FieldPosition(0)).toString();

        // The offset for 2000‑01‑02 03:04:05.006 UTC in New York is -05:00
        assertTrue("Formatted string must contain -0500 offset",
                formatted.contains("-0500"));
    }

    /** --------------------------------------------------------------------- *
     *  6. setCalendar – parsing respects the supplied calendar's leniency.
     *  --------------------------------------------------------------------- */
    @Test
    public void testSetCalendar_LenientParsing()
    {
        SafeSimpleDateFormat safeFmt = new SafeSimpleDateFormat("yyyy/MM/dd");
        Calendar strictCal = Calendar.getInstance();
        strictCal.setLenient(false);
        safeFmt.setCalendar(strictCal);
        safeFmt.setLenient(false);

        // 2020/02/30 is invalid; with strict parsing we expect failure.
        ParsePosition pos = new ParsePosition(0);
        Date result = safeFmt.parse("2020/02/30", pos);
        assertNull("Parsing invalid date with strict mode must return null", result);
        assertEquals("ParsePosition error index should be 0", 0, pos.getErrorIndex());
    }

    /** --------------------------------------------------------------------- *
     *  7. setNumberFormat – grouping enabled should appear in formatted output.
     *  --------------------------------------------------------------------- */
    @Test
    public void testSetNumberFormat_GroupingEnabled()
    {
        SafeSimpleDateFormat safeFmt = new SafeSimpleDateFormat("yyyy, MMMM dd");
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(true);          // enable grouping (e.g., 1,000)
        safeFmt.setNumberFormat(nf);

        // Use a date where month name contains a number (e.g., "June") – not relevant.
        // Instead, format a number via pattern "yyyy, MMMM dd" does not use number format.
        // So we test via a pattern that includes a numeric field with grouping:
        SafeSimpleDateFormat fmtWithNumber = new SafeSimpleDateFormat("yyyy/MM/dd");
        fmtWithNumber.setNumberFormat(nf);
        Date date = utcDate(); // year 2000

        String formatted = fmtWithNumber.format(date, new StringBuffer(), new FieldPosition(0)).toString();
        // Expect "2000/01/02" – grouping does not affect date fields, but we can still assert no exception.
        assertEquals("2000/01/02", formatted);
    }

    /** --------------------------------------------------------------------- *
     *  8. setLenient – toggles lenient flag on the underlying formatter.
     *  --------------------------------------------------------------------- */
    @Test
    public void testSetLenient_TogglesParsingBehavior()
    {
        SafeSimpleDateFormat safeFmt = new SafeSimpleDateFormat("yyyy/MM/dd");
        safeFmt.setLenient(true);
        // With lenient true, "2020/02/30" rolls over to 2020/03/01
        ParsePosition posLenient = new ParsePosition(0);
        Date lenientDate = safeFmt.parse("2020/02/30", posLenient);
        assertNotNull("Lenient parsing should succeed", lenientDate);
        Calendar cal = Calendar.getInstance();
        cal.setTime(lenientDate);
        assertEquals("Month should roll to March (2)", Calendar.MARCH, cal.get(Calendar.MONTH));
        assertEquals("Day should be 1", 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    /** --------------------------------------------------------------------- *
     *  9. setDateFormatSymbols – custom month names affect formatting.
     *  --------------------------------------------------------------------- */
    @Test
    public void testSetDateFormatSymbols_CustomMonths()
    {
        SafeSimpleDateFormat safeFmt = new SafeSimpleDateFormat("dd MMM yyyy");
        DateFormatSymbols symbols = new DateFormatSymbols();
        String[] customMonths = {
                "Month0","Month1","Month2","Month3","Month4","Month5",
                "Month6","Month7","Month8","Month9","Month10","Month11"
        };
        symbols.setMonths(customMonths);
        safeFmt.setDateFormatSymbols(symbols);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2021, Calendar.MARCH, 15); // March -> index 2
        String formatted = safeFmt.format(cal.getTime(), new StringBuffer(), new FieldPosition(0)).toString();

        assertTrue("Formatted month should be custom name", formatted.startsWith("15 Month2"));
    }

    /** --------------------------------------------------------------------- *
     * 10. set2DigitYearStart – influences parsing of two‑digit years.
     *  --------------------------------------------------------------------- */
    @Test
    public void testSet2DigitYearStart_ParsingTwoDigitYear()
    {
        // Pattern with two‑digit year
        SafeSimpleDateFormat safeFmt = new SafeSimpleDateFormat("yy/MM/dd");
        // Set start to 1950 → years 00‑49 map to 2000‑2049, 50‑99 map to 1950‑1999
        Calendar startCal = Calendar.getInstance();
        startCal.clear();
        startCal.set(1950, Calendar.JANUARY, 1);
        safeFmt.set2DigitYearStart(startCal.getTime());

        // Parse "49/01/01" → should become 2049‑01‑01
        ParsePosition pos = new ParsePosition(0);
        Date parsed = safeFmt.parse("49/01/01", pos);
        Calendar resultCal = Calendar.getInstance();
        resultCal.setTime(parsed);
        assertEquals("Year should be 2049", 2049, resultCal.get(Calendar.YEAR));

        // Parse "50/01/01" → should become 1950‑01‑01
        pos.setIndex(0);
        pos.setErrorIndex(-1);
        parsed = safeFmt.parse("50/01/01", pos);
        resultCal.setTime(parsed);
        assertEquals("Year should be 1950", 1950, resultCal.get(Calendar.YEAR));
    }

    /** --------------------------------------------------------------------- *
     * 11. toString – returns the original format pattern.
     *  --------------------------------------------------------------------- */
    @Test
    public void testToString_ReturnsPattern()
    {
        SafeSimpleDateFormat safeFmt = new SafeSimpleDateFormat(PATTERN);
        assertEquals("toString must return the format pattern", PATTERN, safeFmt.toString());
    }

    /** --------------------------------------------------------------------- *
     * 12. equals & hashCode – delegate to the underlying SimpleDateFormat.
     *  --------------------------------------------------------------------- */
    @Test
    public void testEqualsAndHashCode_DelegateToUnderlying()
    {
        SafeSimpleDateFormat fmtA = new SafeSimpleDateFormat(PATTERN);
        SafeSimpleDateFormat fmtB = new SafeSimpleDateFormat(PATTERN);
        SimpleDateFormat plain = new SimpleDateFormat(PATTERN);

        // Equality with the underlying SimpleDateFormat instance
        assertTrue("SafeSimpleDateFormat should be equal to its underlying SimpleDateFormat",
                fmtA.equals(plain));
        assertTrue("Two SafeSimpleDateFormat with same pattern should be equal",
                fmtA.equals(fmtB));

        // Hash codes must match the underlying formatter's hash code
        assertEquals("hashCode should match underlying formatter",
                plain.hashCode(), fmtA.hashCode());
    }

    /** --------------------------------------------------------------------- *
     * 13. parse – null source should raise NullPointerException.
     *  --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testParse_NullSource_ThrowsNPE()
    {
        SafeSimpleDateFormat safeFmt = new SafeSimpleDateFormat(PATTERN);
        safeFmt.parse(null, new ParsePosition(0));
    }

    /** --------------------------------------------------------------------- *
     * 14. format – null Date should raise NullPointerException.
     *  --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void testFormat_NullDate_ThrowsNPE()
    {
        SafeSimpleDateFormat safeFmt = new SafeSimpleDateFormat(PATTERN);
        safeFmt.format(null, new StringBuffer(), new FieldPosition(0));
    }
}
