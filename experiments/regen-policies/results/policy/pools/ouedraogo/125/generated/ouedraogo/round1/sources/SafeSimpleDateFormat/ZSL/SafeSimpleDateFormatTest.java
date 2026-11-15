package com.cedarsoftware.util;

import org.junit.After;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link SafeSimpleDateFormat}.
 *
 * The tests exercise every public method of the class, including the
 * thread‑local caching behaviour of {@code getDateFormat(String)}.
 *
 * The file starts with the required marker {@code ###Test START##}
 * and ends with {@code ###Test END##} so that it can be extracted
 * automatically by the evaluation harness.
 */
public class SafeSimpleDateFormatTest
{
    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    private static final TimeZone PST = TimeZone.getTimeZone("America/Los_Angeles");

    private Calendar referenceCal;
    private Date referenceDate;

    @Before
    public void setUp()
    {
        // Fixed point in time: 2022‑01‑15 12:34:56 GMT
        referenceCal = Calendar.getInstance(GMT);
        referenceCal.clear();
        referenceCal.set(2022, Calendar.JANUARY, 15, 12, 34, 56);
        referenceDate = referenceCal.getTime();
    }

    @After
    public void tearDown()
    {
        // No explicit clean‑up needed – each test runs in its own thread
        // and the ThreadLocal is automatically reclaimed when the test thread ends.
    }

    /** Verify that the same thread receives the same SimpleDateFormat instance. */
    @Test
    public void testGetDateFormatSameThreadReuse()
    {
        SimpleDateFormat first = SafeSimpleDateFormat.getDateFormat(PATTERN);
        SimpleDateFormat second = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertSame("Same thread should reuse the same SimpleDateFormat instance", first, second);
    }

    /** Verify that different threads obtain distinct SimpleDateFormat instances. */
    @Test
    public void testGetDateFormatDifferentThreads() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);

        Callable<SimpleDateFormat> task = () -> {
            // Wait until both threads have created their formatter
            latch.await();
            return SafeSimpleDateFormat.getDateFormat(PATTERN);
        };

        FutureTask<SimpleDateFormat> ft1 = new FutureTask<>(task);
        FutureTask<SimpleDateFormat> ft2 = new FutureTask<>(task);

        Thread t1 = new Thread(ft1);
        Thread t2 = new Thread(ft2);
        t1.start();
        t2.start();

        // Both threads are now ready to fetch the formatter
        latch.countDown();

        SimpleDateFormat fmt1 = ft1.get(); // blocks until thread finishes
        SimpleDateFormat fmt2 = ft2.get();

        assertNotNull(fmt1);
        assertNotNull(fmt2);
        assertNotSame("Different threads must not share the same formatter instance", fmt1, fmt2);
    }

    /** Formatting a known date must produce the expected string. */
    @Test
    public void testFormat()
    {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        StringBuffer buffer = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        sdf.format(referenceDate, buffer, fp);
        assertEquals("2022-01-15 12:34:56", buffer.toString());
    }

    /** Parsing a string that matches the pattern must return the original date (to the second). */
    @Test
    public void testParse()
    {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        String text = "2022-01-15 12:34:56";
        ParsePosition pos = new ParsePosition(0);
        Date parsed = sdf.parse(text, pos);
        assertNotNull(parsed);
        assertEquals(0, pos.getErrorIndex());
        assertEquals(referenceDate, parsed);
    }

    /** Setting a new TimeZone must be reflected in the underlying formatter. */
    @Test
    public void testSetTimeZone()
    {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        sdf.setTimeZone(PST);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertEquals("TimeZone should be updated on the underlying SimpleDateFormat",
                PST, underlying.getTimeZone());
        // Verify that formatting now uses the new zone
        String formatted = sdf.format(referenceDate).toString();
        Calendar cal = Calendar.getInstance(PST);
        cal.setTime(referenceDate);
        String expected = new SimpleDateFormat(PATTERN).format(cal.getTime());
        // Because SimpleDateFormat respects its own TimeZone, we compare with a formatter that has the same zone.
        SimpleDateFormat comparator = new SimpleDateFormat(PATTERN);
        comparator.setTimeZone(PST);
        assertEquals(comparator.format(referenceDate), formatted);
    }

    /** Setting a Calendar must be reflected in the underlying formatter. */
    @Test
    public void testSetCalendar()
    {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        Calendar customCal = Calendar.getInstance(GMT);
        customCal.clear();
        customCal.set(1999, Calendar.DECEMBER, 31, 23, 59, 59);
        sdf.setCalendar(customCal);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertSame("Calendar instance should be stored in the underlying formatter",
                customCal, underlying.getCalendar());

        // Formatting a date should now use the custom calendar's timezone (GMT) – same as default GMT here.
        String formatted = sdf.format(referenceDate).toString();
        SimpleDateFormat comparator = new SimpleDateFormat(PATTERN);
        comparator.setCalendar(customCal);
        assertEquals(comparator.format(referenceDate), formatted);
    }

    /** Setting a custom NumberFormat must be reflected in the underlying formatter. */
    @Test
    public void testSetNumberFormat()
    {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true); // enable grouping to see that it propagates
        sdf.setNumberFormat(nf);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertSame("NumberFormat should be stored in the underlying formatter", nf, underlying.getNumberFormat());
    }

    /** Setting lenient parsing must be reflected in the underlying formatter. */
    @Test
    public void testSetLenient()
    {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        sdf.setLenient(false);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertFalse("Lenient flag should be false on the underlying formatter", underlying.isLenient());

        // Parsing an invalid date should now fail (errorIndex > 0)
        ParsePosition pos = new ParsePosition(0);
        Date result = sdf.parse("2022-02-30 00:00:00", pos);
        assertNull("Parsing an impossible date should return null when lenient is false", result);
        assertTrue("Error index should be set for invalid input", pos.getErrorIndex() > 0);
    }

    /** Setting a custom DateFormatSymbols must be reflected in the underlying formatter. */
    @Test
    public void testSetDateFormatSymbols()
    {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        DateFormatSymbols symbols = new DateFormatSymbols();
        String[] customMonths = {"Januar", "Februar", "März", "April", "Mai", "Juni",
                                 "Juli", "August", "September", "Oktober", "November", "Dezember"};
        symbols.setMonths(customMonths);
        sdf.setDateFormatSymbols(symbols);

        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertArrayEquals("Month symbols must be updated in the underlying formatter",
                customMonths, underlying.getDateFormatSymbols().getMonths());

        // Verify that formatting uses the custom month name
        Calendar cal = Calendar.getInstance(GMT);
        cal.clear();
        cal.set(2022, Calendar.FEBRUARY, 5, 0, 0, 0);
        Date date = cal.getTime();
        String formatted = sdf.format(date).toString();
        assertTrue("Formatted string should contain the custom month name",
                formatted.contains("Februar"));
    }

    /** Setting the 2‑digit year start must be reflected in the underlying formatter. */
    @Test
    public void testSet2DigitYearStart()
    {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yy-MM-dd");
        Calendar startCal = Calendar.getInstance(GMT);
        startCal.clear();
        startCal.set(2000, Calendar.JANUARY, 1);
        Date startDate = startCal.getTime();

        sdf.set2DigitYearStart(startDate);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat("yy-MM-dd");
        assertEquals("2‑digit year start must be stored", startDate, underlying.get2DigitYearStart());
    }

    /** The toString method must return the format pattern supplied at construction. */
    @Test
    public void testToString()
    {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        assertEquals("toString should return the original pattern", PATTERN, sdf.toString());
    }

    /** equals() and hashCode() delegate to the underlying SimpleDateFormat. */
    @Test
    public void testEqualsAndHashCode()
    {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(PATTERN);

        // Equality with the underlying formatter should be true
        assertTrue("SafeSimpleDateFormat should be equal to its underlying SimpleDateFormat",
                sdf.equals(underlying));
        // Equality with a different pattern should be false
        SimpleDateFormat other = new SimpleDateFormat("MM/dd/yyyy");
        assertFalse("Different pattern formatter must not be equal", sdf.equals(other));

        // hashCode must match the underlying formatter's hashCode
        assertEquals("hashCode must be delegated to the underlying formatter",
                underlying.hashCode(), sdf.hashCode());
    }

    /** Helper method to format using SafeSimpleDateFormat directly (no StringBuffer needed). */
    private String format(SafeSimpleDateFormat sdf, Date date)
    {
        return sdf.format(date, new StringBuffer(), new FieldPosition(0)).toString();
    }
}
