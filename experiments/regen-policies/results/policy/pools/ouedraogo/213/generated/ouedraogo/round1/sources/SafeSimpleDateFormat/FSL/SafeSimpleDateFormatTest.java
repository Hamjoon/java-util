package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Assert;

import java.text.ParsePosition;
import java.text.FieldPosition;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Comprehensive JUnit‑4 test suite for {@link SafeSimpleDateFormat}.
 *
 * The tests cover:
 * <ul>
 *   <li>Static factory {@code getDateFormat}</li>
 *   <li>Constructor side‑effects (calendar, lenient, timezone, number format)</li>
 *   <li>Formatting and parsing</li>
 *   <li>All mutator methods (setTimeZone, setCalendar, setNumberFormat,
 *       setLenient, setDateFormatSymbols, set2DigitYearStart)</li>
 *   <li>Object contract methods {@code toString}, {@code equals} and {@code hashCode}</li>
 *   <li>Thread‑local isolation of the internal {@code SimpleDateFormat} map</li>
 * </ul>
 */
public class SafeSimpleDateFormatTest {

    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** Helper to create a deterministic Calendar instance (UTC, cleared fields). */
    private static Calendar utcClearedCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        return cal;
    }

    @Test
    public void testGetDateFormatCreatesAndCachesPerThread() {
        // First call creates the formatter for this thread
        SimpleDateFormat fmt1 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        SimpleDateFormat fmt2 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        Assert.assertSame("Same thread should receive the same SimpleDateFormat instance", fmt1, fmt2);

        // In a different thread a distinct SimpleDateFormat instance must be created
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<SimpleDateFormat> otherThreadFormatter = new AtomicReference<>();

        Thread t = new Thread(() -> {
            otherThreadFormatter.set(SafeSimpleDateFormat.getDateFormat(PATTERN));
            latch.countDown();
        });
        t.start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Assert.fail("Interrupted while waiting for other thread");
        }

        SimpleDateFormat fmtFromOtherThread = otherThreadFormatter.get();
        Assert.assertNotNull("Formatter from other thread must not be null", fmtFromOtherThread);
        Assert.assertNotSame("Different threads must not share the same SimpleDateFormat instance",
                fmt1, fmtFromOtherThread);
    }

    @Test
    public void testConstructorInitialisesUnderlyingFormatter() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        // The underlying SimpleDateFormat should have the same calendar, timezone and leniency as a fresh calendar
        Calendar expectedCal = utcClearedCalendar();
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(PATTERN);
        Assert.assertEquals("Calendar should be cleared", expectedCal, underlying.getCalendar());
        Assert.assertEquals("Lenient flag should match cleared calendar", expectedCal.isLenient(),
                underlying.isLenient());
        Assert.assertEquals("TimeZone should be UTC (default of cleared calendar)", expectedCal.getTimeZone(),
                underlying.getTimeZone());

        // NumberFormat must have grouping disabled
        NumberFormat nf = underlying.getNumberFormat();
        Assert.assertFalse("NumberFormat grouping must be disabled", nf.isGroupingUsed());
    }

    @Test
    public void testFormatProducesExpectedString() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2023, Calendar.MARCH, 15, 12, 30, 45);
        cal.set(Calendar.MILLISECOND, 0);
        Date date = cal.getTime();

        String expected = new SimpleDateFormat(PATTERN).format(date);
        String actual = sdf.format(date, new StringBuffer(), new FieldPosition(0)).toString();
        Assert.assertEquals("Formatted string must match SimpleDateFormat output", expected, actual);
    }

    @Test
    public void testParseWithParsePosition() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        String text = "2023-07-04 09:15:00 extra";
        ParsePosition pos = new ParsePosition(0);
        Date parsed = sdf.parse(text, pos);
        Assert.assertNotNull("Parsed date must not be null", parsed);
        Assert.assertEquals("ParsePosition index should point after the parsed segment",
                19, pos.getIndex());

        // Verify the parsed date matches the expected instant
        SimpleDateFormat reference = new SimpleDateFormat(PATTERN);
        Date expected = reference.parse("2023-07-04 09:15:00");
        Assert.assertEquals("Parsed date must equal reference parsing", expected, parsed);
    }

    @Test
    public void testMutatorsAffectAllInstancesSharingSamePattern() {
        SafeSimpleDateFormat sdfA = new SafeSimpleDateFormat(PATTERN);
        SafeSimpleDateFormat sdfB = new SafeSimpleDateFormat(PATTERN);

        // Change timezone via one instance
        TimeZone tz = TimeZone.getTimeZone("GMT+2");
        sdfA.setTimeZone(tz);
        Assert.assertEquals("Timezone change must be visible through second instance",
                tz, SafeSimpleDateFormat.getDateFormat(PATTERN).getTimeZone());

        // Change leniency
        sdfB.setLenient(false);
        Assert.assertFalse("Lenient flag must be false after mutation",
                SafeSimpleDateFormat.getDateFormat(PATTERN).isLenient());

        // Change calendar
        Calendar customCal = Calendar.getInstance();
        customCal.set(2000, Calendar.JANUARY, 1);
        sdfA.setCalendar(customCal);
        Assert.assertEquals("Calendar must be the one set via mutator",
                customCal, SafeSimpleDateFormat.getDateFormat(PATTERN).getCalendar());

        // Change NumberFormat
        NumberFormat nf = NumberFormat.getInstance();
        nf.setGroupingUsed(true);
        sdfB.setNumberFormat(nf);
        Assert.assertTrue("NumberFormat grouping must be enabled after mutation",
                SafeSimpleDateFormat.getDateFormat(PATTERN).getNumberFormat().isGroupingUsed());

        // Change DateFormatSymbols
        DateFormatSymbols symbols = new DateFormatSymbols();
        symbols.setMonths(new String[]{"JanX","FebX","MarX","AprX","MayX","JunX","JulX","AugX","SepX","OctX","NovX","DecX"});
        sdfA.setDateFormatSymbols(symbols);
        Assert.assertArrayEquals("Month symbols must be replaced",
                symbols.getMonths(),
                SafeSimpleDateFormat.getDateFormat(PATTERN).getDateFormatSymbols().getMonths());

        // Change 2‑digit year start
        Calendar startCal = Calendar.getInstance();
        startCal.set(1950, Calendar.JANUARY, 1);
        Date startDate = startCal.getTime();
        sdfB.set2DigitYearStart(startDate);
        Assert.assertEquals("2‑digit year start must be set",
                startDate,
                SafeSimpleDateFormat.getDateFormat(PATTERN).get2DigitYearStart());
    }

    @Test
    public void testToStringReturnsPattern() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("dd/MM/yyyy");
        Assert.assertEquals("toString must return the original pattern", "dd/MM/yyyy", sdf.toString());
    }

    @Test
    public void testEqualsAndHashCodeDelegateToUnderlyingFormatter() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        SimpleDateFormat simple = new SimpleDateFormat(PATTERN);
        SimpleDateFormat otherPattern = new SimpleDateFormat("MM-dd-yyyy");

        // equals should be true when compared to an equivalent SimpleDateFormat
        Assert.assertTrue("SafeSimpleDateFormat should be equal to SimpleDateFormat with same pattern",
                sdf.equals(simple));

        // equals should be false for different pattern
        Assert.assertFalse("SafeSimpleDateFormat should not be equal to SimpleDateFormat with different pattern",
                sdf.equals(otherPattern));

        // equals to another SafeSimpleDateFormat returns false (different class)
        SafeSimpleDateFormat anotherSdf = new SafeSimpleDateFormat(PATTERN);
        Assert.assertFalse("SafeSimpleDateFormat should not be equal to another SafeSimpleDateFormat",
                sdf.equals(anotherSdf));

        // hashCode must match the underlying formatter's hashCode
        Assert.assertEquals("hashCode must delegate to underlying SimpleDateFormat",
                simple.hashCode(),
                sdf.hashCode());
    }

    @Test
    public void testSet2DigitYearStartAffectsParsingOfTwoDigitYear() throws Exception {
        // Pattern with two‑digit year
        String twoDigitPattern = "yy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(twoDigitPattern);

        // Set start date to 1950 -> years 00‑49 map to 2000‑2049, 50‑99 map to 1950‑1999
        Calendar start = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        start.clear();
        start.set(1950, Calendar.JANUARY, 1);
        sdf.set2DigitYearStart(start.getTime());

        // Parse "49-12-31" -> should be 2049-12-31
        Date parsed = sdf.parse("49-12-31", new ParsePosition(0));
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        expected.clear();
        expected.set(2049, Calendar.DECEMBER, 31);
        Assert.assertEquals("Two‑digit year 49 must resolve to 2049", expected.getTime(), parsed);

        // Parse "50-01-01" -> should be 1950-01-01
        parsed = sdf.parse("50-01-01", new ParsePosition(0));
        expected.clear();
        expected.set(1950, Calendar.JANUARY, 1);
        Assert.assertEquals("Two‑digit year 50 must resolve to 1950", expected.getTime(), parsed);
    }
}
