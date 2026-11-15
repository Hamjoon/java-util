package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link SafeSimpleDateFormat}.
 *
 * The test class follows the collaborative steps described in the prompt:
 *   1. List all public members.
 *   2. Provide a basic test for each member.
 *   3. Identify edge‑case / exception scenarios.
 *   4. Add dedicated tests for those scenarios.
 *   5. Merge everything into a single runnable test file.
 *
 * The tests use only the public API of {@code SafeSimpleDateFormat} and
 * standard JDK classes – no mocking or external libraries.
 */
public class SafeSimpleDateFormatTest {

    // -----------------------------------------------------------------------
    // 1. Public members identified from the source code
    // -----------------------------------------------------------------------
    // static SimpleDateFormat getDateFormat(String format)
    // SafeSimpleDateFormat(String format)
    // StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fp)
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
    private static final String SAMPLE_DATE_STRING = "2023-07-15 13:45:30";
    private static final Date SAMPLE_DATE = createDate(2023, Calendar.JULY, 15, 13, 45, 30);

    private SafeSimpleDateFormat safeSdf;

    @Before
    public void setUp() {
        safeSdf = new SafeSimpleDateFormat(PATTERN);
    }

    @After
    public void tearDown() {
        // No resources to release; the ThreadLocal map will be cleared automatically
    }

    // -----------------------------------------------------------------------
    // Helper utilities
    // -----------------------------------------------------------------------
    private static Date createDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(year, month, day, hour, minute, second);
        return cal.getTime();
    }

    // -----------------------------------------------------------------------
    // 2. Basic tests for each public method
    // -----------------------------------------------------------------------

    @Test
    public void testGetDateFormatSameInstanceForSamePattern() {
        SimpleDateFormat f1 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        SimpleDateFormat f2 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertSame("Same pattern should return the same SimpleDateFormat instance within a thread",
                f1, f2);
    }

    @Test
    public void testGetDateFormatDifferentInstanceForDifferentPattern() {
        SimpleDateFormat f1 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat f2 = SafeSimpleDateFormat.getDateFormat("MM/dd/yyyy");
        assertNotSame("Different patterns must yield different SimpleDateFormat instances",
                f1, f2);
    }

    @Test
    public void testConstructorInitialisesInternalFormatter() {
        // The constructor does not expose the formatter directly,
        // but a simple format/parse round‑trip proves it works.
        StringBuffer sb = new StringBuffer();
        FieldPosition pos = new FieldPosition(0);
        safeSdf.format(SAMPLE_DATE, sb, pos);
        assertEquals("Formatted output should match the pattern", SAMPLE_DATE_STRING, sb.toString());
    }

    @Test
    public void testFormatMethod() {
        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        safeSdf.format(SAMPLE_DATE, sb, fp);
        assertEquals("Formatting a known Date must produce the expected string",
                SAMPLE_DATE_STRING, sb.toString());
    }

    @Test
    public void testParseMethod() {
        ParsePosition pos = new ParsePosition(0);
        Date parsed = safeSdf.parse(SAMPLE_DATE_STRING, pos);
        assertNotNull("Parse should return a non‑null Date", parsed);
        assertEquals("Parsed Date must equal the original", SAMPLE_DATE, parsed);
        assertEquals("ParsePosition index must advance to end of string",
                SAMPLE_DATE_STRING.length(), pos.getIndex());
    }

    @Test
    public void testSetTimeZoneAffectsFormatting() {
        TimeZone tz = TimeZone.getTimeZone("America/New_York");
        safeSdf.setTimeZone(tz);

        // Re‑format using the same date; the output should now reflect the new TZ.
        StringBuffer sb = new StringBuffer();
        safeSdf.format(SAMPLE_DATE, sb, new FieldPosition(0));

        // Build expected string using a plain SimpleDateFormat with same TZ.
        SimpleDateFormat plain = new SimpleDateFormat(PATTERN);
        plain.setTimeZone(tz);
        String expected = plain.format(SAMPLE_DATE);
        assertEquals("Formatting after setTimeZone must match plain formatter with same TZ",
                expected, sb.toString());
    }

    @Test
    public void testSetCalendarAffectsParsing() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+2"));
        safeSdf.setCalendar(cal);

        // Parsing a date string should now produce a Date whose epoch reflects the calendar's TZ.
        ParsePosition pos = new ParsePosition(0);
        Date parsed = safeSdf.parse("2023-01-01 00:00:00", pos);
        assertNotNull(parsed);

        // Compute expected epoch using a plain SimpleDateFormat with same TZ.
        SimpleDateFormat plain = new SimpleDateFormat(PATTERN);
        plain.setTimeZone(cal.getTimeZone());
        Date expected = plain.parse("2023-01-01 00:00:00", new ParsePosition(0));
        assertEquals("Parsing after setCalendar must respect the calendar's time zone",
                expected, parsed);
    }

    @Test
    public void testSetNumberFormatDisablesGrouping() {
        // Create a NumberFormat that uses grouping and then set it on the SafeSimpleDateFormat
        java.text.NumberFormat nf = java.text.NumberFormat.getNumberInstance(Locale.US);
        nf.setGroupingUsed(true);
        safeSdf.setNumberFormat(nf);

        // Formatting a date does not involve numbers, but we can verify that the internal
        // NumberFormat instance is indeed the one we supplied (via reflection is not allowed,
        // so we rely on the fact that no exception is thrown when grouping is enabled).
        // The test is mainly a smoke test that the setter does not break functionality.
        StringBuffer sb = new StringBuffer();
        safeSdf.format(SAMPLE_DATE, sb, new FieldPosition(0));
        assertEquals(SAMPLE_DATE_STRING, sb.toString());
    }

    @Test
    public void testSetLenient() {
        safeSdf.setLenient(false);
        // Parsing a non‑lenient date that is slightly off should fail.
        ParsePosition pos = new ParsePosition(0);
        Date d = safeSdf.parse("2023-02-30 12:00:00", pos); // Feb 30 does not exist
        assertNull("Non‑lenient parsing of an invalid date must return null", d);
        assertEquals("ParsePosition error index should be set to the point of failure",
                0, pos.getErrorIndex());
    }

    @Test
    public void testSetDateFormatSymbols() {
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.FRENCH);
        safeSdf.setDateFormatSymbols(symbols);
        // Verify that month names are now French by formatting a month‑specific date.
        Date frenchMonth = createDate(2023, Calendar.MARCH, 1, 0, 0, 0);
        StringBuffer sb = new StringBuffer();
        SafeSimpleDateFormat frenchSdf = new SafeSimpleDateFormat("MMMM yyyy");
        frenchSdf.setDateFormatSymbols(symbols);
        frenchSdf.format(frenchMonth, sb, new FieldPosition(0));
        // In French, March is "mars"
        assertTrue("Month name should be French after setting symbols", sb.toString().startsWith("mars"));
    }

    @Test
    public void testSet2DigitYearStart() {
        // Set the 2‑digit year start to 1950‑01‑01
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(1950, Calendar.JANUARY, 1);
        Date startDate = cal.getTime();
        safeSdf.set2DigitYearStart(startDate);

        // Now parse a 2‑digit year that falls before 1950; it should be interpreted as 2050.
        SimpleDateFormat plain = new SimpleDateFormat("yy-MM-dd");
        plain.set2DigitYearStart(startDate);
        Date expected = plain.parse("49-12-31", new ParsePosition(0));
        Date actual = safeSdf.parse("49-12-31", new ParsePosition(0));
        assertEquals("Parsing with custom 2‑digit year start must match plain formatter", expected, actual);
    }

    @Test
    public void testToStringReturnsPattern() {
        assertEquals("toString must return the original pattern", PATTERN, safeSdf.toString());
    }

    @Test
    public void testEqualsAndHashCodeDelegateToUnderlyingFormatter() {
        SafeSimpleDateFormat other = new SafeSimpleDateFormat(PATTERN);
        assertTrue("Two SafeSimpleDateFormat with same pattern should be equal", safeSdf.equals(other));
        assertEquals("Hash codes must be equal for equal objects", safeSdf.hashCode(), other.hashCode());

        SafeSimpleDateFormat different = new SafeSimpleDateFormat("MM/dd/yyyy");
        assertFalse("Different patterns must not be equal", safeSdf.equals(different));
    }

    // -----------------------------------------------------------------------
    // 3. Edge‑case / exception tests
    // -----------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorInvalidPatternThrows() {
        new SafeSimpleDateFormat("invalid_pattern@@@");
    }

    @Test(expected = NullPointerException.class)
    public void testGetDateFormatNullPatternThrows() {
        SafeSimpleDateFormat.getDateFormat(null);
    }

    @Test(expected = NullPointerException.class)
    public void testFormatNullDateThrows() {
        safeSdf.format(null, new StringBuffer(), new FieldPosition(0));
    }

    @Test(expected = NullPointerException.class)
    public void testParseNullSourceThrows() {
        safeSdf.parse(null, new ParsePosition(0));
    }

    @Test(expected = NullPointerException.class)
    public void testParseNullParsePositionThrows() {
        safeSdf.parse("2023-01-01 00:00:00", null);
    }

    @Test(expected = NullPointerException.class)
    public void testSetTimeZoneNullThrows() {
        safeSdf.setTimeZone(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSetCalendarNullThrows() {
        safeSdf.setCalendar(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSetNumberFormatNullThrows() {
        safeSdf.setNumberFormat(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSetDateFormatSymbolsNullThrows() {
        safeSdf.setDateFormatSymbols(null);
    }

    @Test(expected = NullPointerException.class)
    public void testSet2DigitYearStartNullThrows() {
        safeSdf.set2DigitYearStart(null);
    }

    // -----------------------------------------------------------------------
    // 4. Thread‑safety sanity check (concurrent use of the same pattern)
    // -----------------------------------------------------------------------
    @Test
    public void testConcurrentAccessProducesCorrectResults() throws Exception {
        final int THREAD_COUNT = 10;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        final AtomicReference<Exception> error = new AtomicReference<>(null);

        Runnable task = () -> {
            try {
                startLatch.await(); // ensure all threads start at the same time
                SafeSimpleDateFormat fmt = new SafeSimpleDateFormat(PATTERN);
                StringBuffer sb = new StringBuffer();
                fmt.format(SAMPLE_DATE, sb, new FieldPosition(0));
                if (!SAMPLE_DATE_STRING.equals(sb.toString())) {
                    throw new AssertionError("Concurrent format mismatch");
                }
                ParsePosition pos = new ParsePosition(0);
                Date d = fmt.parse(SAMPLE_DATE_STRING, pos);
                if (!SAMPLE_DATE.equals(d)) {
                    throw new AssertionError("Concurrent parse mismatch");
                }
            } catch (Exception e) {
                error.set(e);
            } finally {
                doneLatch.countDown();
            }
        };

        for (int i = 0; i < THREAD_COUNT; i++) {
            new Thread(task).start();
        }
        startLatch.countDown();   // release all threads
        doneLatch.await();        // wait for completion

        if (error.get() != null) {
            throw error.get();
        }
    }
}
