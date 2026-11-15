package com.cedarsoftware.util;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link SafeSimpleDateFormat}.
 *
 * The tests are divided into three sections:
 *
 * 1. **Public API extraction** – a comment that lists all public members.
 * 2. **Basic functional tests** – one test per public method that verifies the
 *    normal (happy‑path) behaviour.
 * 3. **Edge‑case / exception tests** – tests that exercise boundary conditions,
 *    null handling, thread‑local isolation and other corner‑cases.
 *
 * The test class is completely self‑contained and does not rely on any
 * external mocking frameworks – it creates real {@link SimpleDateFormat},
 * {@link Calendar}, {@link TimeZone}, etc., exactly as the production code does.
 */
@RunWith(JUnit4.class)
public class SafeSimpleDateFormatTest {

    /* ----------------------------------------------------------------------
     * 1. PUBLIC METHODS OF SafeSimpleDateFormat (extracted from the source)
     * ----------------------------------------------------------------------
     *
     * public static SimpleDateFormat getDateFormat(String format)
     *
     * public SafeSimpleDateFormat(String format)                // constructor
     *
     * public StringBuffer format(Date date,
     *                            StringBuffer toAppendTo,
     *                            FieldPosition fieldPosition)
     *
     * public Date parse(String source, ParsePosition pos)
     *
     * public void setTimeZone(TimeZone tz)
     *
     * public void setCalendar(Calendar cal)
     *
     * public void setNumberFormat(NumberFormat format)
     *
     * public void setLenient(boolean lenient)
     *
     * public void setDateFormatSymbols(DateFormatSymbols symbols)
     *
     * public void set2DigitYearStart(Date date)
     *
     * public String toString()
     *
     * public boolean equals(Object other)
     *
     * public int hashCode()
     */

    /* ----------------------------------------------------------------------
     * 2. BASIC FUNCTIONAL TESTS
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetDateFormatSameInstanceSameThread() {
        SimpleDateFormat fmt1 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat fmt2 = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        // Same thread → same instance (cached)
        assertSame("Same format string should return the same SimpleDateFormat instance", fmt1, fmt2);
    }

    @Test
    public void testGetDateFormatDifferentThreads() throws Exception {
        final SimpleDateFormat[] threadResult = new SimpleDateFormat[1];
        SimpleDateFormat mainThreadFormatter = SafeSimpleDateFormat.getDateFormat("yyyy/MM/dd");

        Thread t = new Thread(() -> {
            threadResult[0] = SafeSimpleDateFormat.getDateFormat("yyyy/MM/dd");
        });
        t.start();
        t.join();

        // ThreadLocal → different instances
        assertNotSame("Different threads should have distinct SimpleDateFormat instances",
                mainThreadFormatter, threadResult[0]);
    }

    @Test(expected = NullPointerException.class)
    public void testGetDateFormatNullPattern() {
        // SimpleDateFormat constructor throws NPE when pattern is null
        SafeSimpleDateFormat.getDateFormat(null);
    }

    @Test
    public void testConstructorStoresFormat() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("dd-MMM-yyyy");
        assertEquals("Constructor should store the format string", "dd-MMM-yyyy", sdf.toString());
    }

    @Test
    public void testFormatProducesExpectedString() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2023, Calendar.MARCH, 15, 0, 0, 0);
        Date date = cal.getTime();

        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        sdf.format(date, sb, fp);
        assertEquals("2023-03-15", sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testFormatNullDate() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        sdf.format(null, new StringBuffer(), new FieldPosition(0));
    }

    @Test
    public void testParseValidString() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy/MM/dd");
        ParsePosition pos = new ParsePosition(0);
        Date d = sdf.parse("2022/12/31", pos);
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        assertEquals(2022, cal.get(Calendar.YEAR));
        assertEquals(Calendar.DECEMBER, cal.get(Calendar.MONTH));
        assertEquals(31, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(0, pos.getIndex()); // SimpleDateFormat does not advance pos on success
    }

    @Test
    public void testParseInvalidStringReturnsNull() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy/MM/dd");
        ParsePosition pos = new ParsePosition(0);
        Date d = sdf.parse("invalid-date", pos);
        assertNull("Parsing an invalid string should return null", d);
        assertEquals(0, pos.getErrorIndex());
    }

    @Test(expected = NullPointerException.class)
    public void testParseNullSource() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        sdf.parse(null, new ParsePosition(0));
    }

    @Test
    public void testSetTimeZoneAffectsFormatting() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm Z");
        // Force UTC timezone first
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2021, Calendar.JANUARY, 1, 12, 0, 0);
        Date date = cal.getTime();

        StringBuffer sb = new StringBuffer();
        sdf.format(date, sb, new FieldPosition(0));
        assertTrue("UTC should be represented as +0000", sb.toString().endsWith(" +0000"));

        // Change to PST (UTC‑8) and verify the offset changes
        sdf.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        sb.setLength(0);
        sdf.format(date, sb, new FieldPosition(0));
        assertTrue("PST should be represented as -0800 or -0700 (DST) depending on date",
                sb.toString().endsWith("-0800") || sb.toString().endsWith("-0700"));
    }

    @Test
    public void testSetCalendarAffectsParsing() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        Calendar customCal = Calendar.getInstance();
        customCal.clear();
        customCal.setLenient(false);
        sdf.setCalendar(customCal);

        // Parsing a non‑existent date should now fail (lenient = false)
        ParsePosition pos = new ParsePosition(0);
        Date d = sdf.parse("2021-02-30", pos);
        assertNull("Lenient false should reject impossible dates", d);
        assertEquals(0, pos.getErrorIndex());
    }

    @Test
    public void testSetNumberFormatDoesNotBreakFormatting() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setGroupingUsed(true); // introduce grouping, which SimpleDateFormat should ignore
        sdf.setNumberFormat(nf);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2020, Calendar.JUNE, 5);
        Date date = cal.getTime();

        String result = sdf.format(date, new StringBuffer(), new FieldPosition(0)).toString();
        assertEquals("2020-06-05", result);
    }

    @Test
    public void testSetLenientChangesParsingBehaviour() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        // Default lenient is true → "2021-02-30" rolls over to March 2nd
        ParsePosition pos1 = new ParsePosition(0);
        Date d1 = sdf.parse("2021-02-30", pos1);
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(d1);
        assertEquals(2021, cal1.get(Calendar.YEAR));
        assertEquals(Calendar.MARCH, cal1.get(Calendar.MONTH));
        assertEquals(2, cal1.get(Calendar.DAY_OF_MONTH));

        // Now set strict parsing
        sdf.setLenient(false);
        ParsePosition pos2 = new ParsePosition(0);
        Date d2 = sdf.parse("2021-02-30", pos2);
        assertNull("Strict parsing should reject non‑existent dates", d2);
    }

    @Test
    public void testSetDateFormatSymbolsChangesMonthNames() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("dd-MMM-yyyy");
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        String[] customMonths = symbols.getShortMonths();
        customMonths[0] = "JanX"; // modify January
        symbols.setShortMonths(customMonths);
        sdf.setDateFormatSymbols(symbols);

        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2022, Calendar.JANUARY, 15);
        Date date = cal.getTime();

        String formatted = sdf.format(date, new StringBuffer(), new FieldPosition(0)).toString();
        assertTrue("Month abbreviation should reflect custom symbols", formatted.startsWith("15-JanX-2022"));
    }

    @Test
    public void testSet2DigitYearStartAffectsParsing() {
        // Pattern with two‑digit year
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yy-MM-dd");
        // Set the start of the 2‑digit window to 1950 → 49 maps to 2049, 50 maps to 1950
        Calendar start = Calendar.getInstance();
        start.clear();
        start.set(1950, Calendar.JANUARY, 1);
        sdf.set2DigitYearStart(start.getTime());

        // Parse "49-01-01" → should become 2049‑01‑01
        ParsePosition pos = new ParsePosition(0);
        Date d = sdf.parse("49-01-01", pos);
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        assertEquals(2049, cal.get(Calendar.YEAR));

        // Parse "50-01-01" → should become 1950‑01‑01
        pos.setIndex(0);
        d = sdf.parse("50-01-01", pos);
        cal.setTime(d);
        assertEquals(1950, cal.get(Calendar.YEAR));
    }

    @Test
    public void testToStringReturnsPattern() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("EEE, dd MMM yyyy");
        assertEquals("EEE, dd MMM yyyy", sdf.toString());
    }

    @Test
    public void testEqualsAndHashCodeDelegateToUnderlyingFormatter() {
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat("yyyyMMdd");
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat("yyyyMMdd");
        SimpleDateFormat plain = new SimpleDateFormat("yyyyMMdd");

        // Equality with another SafeSimpleDateFormat having same pattern
        assertTrue("Two SafeSimpleDateFormat with same pattern should be equal",
                sdf1.equals(sdf2));

        // Equality with a plain SimpleDateFormat (delegated equals)
        assertTrue("SafeSimpleDateFormat should be equal to a SimpleDateFormat with same pattern",
                sdf1.equals(plain));

        // Hash codes must match the underlying SimpleDateFormat hash code
        assertEquals("Hash code should match underlying formatter's hash code",
                plain.hashCode(), sdf1.hashCode());
    }

    /* ----------------------------------------------------------------------
     * 3. EDGE‑CASE / EXCEPTION TESTS (additional focus)
     * ---------------------------------------------------------------------- */

    @Test
    public void testGetDateFormatCacheIsolationBetweenFormats() {
        SimpleDateFormat fmtA = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd");
        SimpleDateFormat fmtB = SafeSimpleDateFormat.getDateFormat("dd/MM/yyyy");
        assertNotSame("Different format strings must yield different SimpleDateFormat instances", fmtA, fmtB);
    }

    @Test
    public void testConcurrentAccessDoesNotCorruptCache() throws Exception {
        final int THREADS = 10;
        final String PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
        ExecutorService exec = Executors.newFixedThreadPool(THREADS);
        List<Future<SimpleDateFormat>> futures = new ArrayList<>();

        for (int i = 0; i < THREADS; i++) {
            futures.add(exec.submit(() -> SafeSimpleDateFormat.getDateFormat(PATTERN)));
        }

        SimpleDateFormat first = futures.get(0).get();
        for (Future<SimpleDateFormat> f : futures) {
            assertSame("All threads should retrieve the same SimpleDateFormat instance for the same pattern",
                    first, f.get());
        }
        exec.shutdownNow();
    }

    @Test
    public void testSetCalendarWithNullThrowsNPE() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        try {
            sdf.setCalendar(null);
            fail("Setting a null Calendar should throw NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testSetNumberFormatWithNullThrowsNPE() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        try {
            sdf.setNumberFormat(null);
            fail("Setting a null NumberFormat should throw NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testSetDateFormatSymbolsWithNullThrowsNPE() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        try {
            sdf.setDateFormatSymbols(null);
            fail("Setting null DateFormatSymbols should throw NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    @Test
    public void testSet2DigitYearStartWithNullThrowsNPE() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yy-MM-dd");
        try {
            sdf.set2DigitYearStart(null);
            fail("Setting a null start date for 2‑digit year should throw NullPointerException");
        } catch (NullPointerException expected) {
            // expected
        }
    }
}
