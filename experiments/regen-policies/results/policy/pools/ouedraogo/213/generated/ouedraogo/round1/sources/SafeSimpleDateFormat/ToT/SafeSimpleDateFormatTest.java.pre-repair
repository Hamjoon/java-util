package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public class SafeSimpleDateFormatTest {

    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss.SSS Z";
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");
    private static final TimeZone PST = TimeZone.getTimeZone("America/Los_Angeles");

    private Date sampleDate;

    @Before
    public void setUp() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(2023, Calendar.MARCH, 15, 12, 34, 56);
        cal.set(Calendar.MILLISECOND, 789);
        sampleDate = cal.getTime();
    }

    /* --------------------------------------------------------------
     *  Static method getDateFormat()
     * -------------------------------------------------------------- */

    @Test
    public void testGetDateFormatCreatesAndCachesPerThread() {
        SimpleDateFormat fmt1 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        SimpleDateFormat fmt2 = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertSame("Same thread should receive same SimpleDateFormat instance", fmt1, fmt2);

        // Different pattern -> different instance
        SimpleDateFormat fmtOther = SafeSimpleDateFormat.getDateFormat("yyyy/MM/dd");
        assertNotSame("Different patterns must not share the same instance", fmt1, fmtOther);
    }

    @Test
    public void testGetDateFormatThreadIsolation() throws Exception {
        final SimpleDateFormat[] threadFmt = new SimpleDateFormat[2];
        CountDownLatch latch = new CountDownLatch(2);

        Runnable task = () -> {
            threadFmt[(int) Thread.currentThread().getId() % 2] = SafeSimpleDateFormat.getDateFormat(PATTERN);
            latch.countDown();
        };
        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        latch.await(2, TimeUnit.SECONDS);

        assertNotNull(threadFmt[0]);
        assertNotNull(threadFmt[1]);
        assertNotSame("Different threads must have distinct SimpleDateFormat instances",
                threadFmt[0], threadFmt[1]);
    }

    /* --------------------------------------------------------------
     *  Constructor
     * -------------------------------------------------------------- */

    @Test
    public void testConstructorInitializesCalendarClearedAndLenient() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(PATTERN);
        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        Calendar cal = internal.getCalendar();

        // Calendar should be cleared (no fields set)
        assertEquals("All calendar fields should be cleared", 0, cal.getTimeInMillis());

        // Lenient flag should mirror the cleared calendar (default false)
        assertFalse("Lenient flag should be false after constructor", internal.isLenient());

        // NumberFormat must have grouping disabled
        NumberFormat nf = internal.getNumberFormat();
        assertFalse("NumberFormat grouping must be disabled", nf.isGroupingUsed());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullPatternThrowsNPE() {
        new SafeSimpleDateFormat(null);
    }

    /* --------------------------------------------------------------
     *  format(Date, StringBuffer, FieldPosition)
     * -------------------------------------------------------------- */

    @Test
    public void testFormatProducesSameResultAsSimpleDateFormat() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        StringBuffer sb = new StringBuffer();
        FieldPosition fp = new FieldPosition(0);
        safe.format(sampleDate, sb, fp);

        SimpleDateFormat plain = new SimpleDateFormat(PATTERN);
        plain.setTimeZone(plain.getTimeZone()); // ensure default timezone
        String expected = plain.format(sampleDate);

        assertEquals("Formatted output must match SimpleDateFormat", expected, sb.toString());
    }

    @Test(expected = NullPointerException.class)
    public void testFormatNullDateThrowsNPE() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        safe.format(null, new StringBuffer(), new FieldPosition(0));
    }

    /* --------------------------------------------------------------
     *  parse(String, ParsePosition)
     * -------------------------------------------------------------- */

    @Test
    public void testParseParsesBackToOriginalDate() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        String text = safe.format(sampleDate, new StringBuffer(), new FieldPosition(0)).toString();

        ParsePosition pos = new ParsePosition(0);
        Date parsed = safe.parse(text, pos);
        assertNotNull("Parsed date must not be null", parsed);
        assertEquals("Parsed date must equal original", sampleDate, parsed);
        assertEquals("ParsePosition index must be at end of string", text.length(), pos.getIndex());
    }

    @Test
    public void testParseWithInvalidStringLeavesErrorIndexUnchanged() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        ParsePosition pos = new ParsePosition(5);
        Date result = safe.parse("invalid-date-string", pos);
        assertNull("Parsing invalid string should return null", result);
        assertEquals("Error index should remain unchanged", 5, pos.getErrorIndex());
    }

    @Test(expected = NullPointerException.class)
    public void testParseNullSourceThrowsNPE() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        safe.parse(null, new ParsePosition(0));
    }

    /* --------------------------------------------------------------
     *  setTimeZone(TimeZone)
     * -------------------------------------------------------------- */

    @Test
    public void testSetTimeZoneAffectsFormatting() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        safe.setTimeZone(UTC);
        String formatted = safe.format(sampleDate, new StringBuffer(), new FieldPosition(0)).toString();
        assertTrue("Formatted string must contain UTC offset +0000", formatted.endsWith(" +0000"));
    }

    @Test(expected = NullPointerException.class)
    public void testSetTimeZoneNullThrowsNPE() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        safe.setTimeZone(null);
    }

    /* --------------------------------------------------------------
     *  setCalendar(Calendar)
     * -------------------------------------------------------------- */

    @Test
    public void testSetCalendarChangesBaseCalendar() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        Calendar cal = Calendar.getInstance(PST);
        cal.set(1999, Calendar.DECEMBER, 31, 23, 59, 59);
        cal.set(Calendar.MILLISECOND, 0);
        safe.setCalendar(cal);

        // format a known instant and verify timezone offset matches PST
        Date date = new Date(946684799000L); // 1999-12-31 23:59:59 GMT
        String formatted = safe.format(date, new StringBuffer(), new FieldPosition(0)).toString();
        assertTrue("Offset should reflect PST (-0800 or -0700 depending on DST)",
                formatted.endsWith(" -0800") || formatted.endsWith(" -0700"));
    }

    @Test(expected = NullPointerException.class)
    public void testSetCalendarNullThrowsNPE() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        safe.setCalendar(null);
    }

    /* --------------------------------------------------------------
     *  setNumberFormat(NumberFormat)
     * -------------------------------------------------------------- */

    @Test
    public void testSetNumberFormatIsDelegated() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setGroupingUsed(true);
        safe.setNumberFormat(nf);

        SimpleDateFormat internal = SafeSimpleDateFormat.getDateFormat(PATTERN);
        assertSame("NumberFormat should be the one we set", nf, internal.getNumberFormat());
    }

    @Test(expected = NullPointerException.class)
    public void testSetNumberFormatNullThrowsNPE() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        safe.setNumberFormat(null);
    }

    /* --------------------------------------------------------------
     *  setLenient(boolean)
     * -------------------------------------------------------------- */

    @Test
    public void testSetLenientControlsParsingStrictness() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yyyy-MM-dd");
        safe.setLenient(false);

        // February 30th is illegal – parsing should fail with lenient=false
        ParsePosition pos = new ParsePosition(0);
        Date d = safe.parse("2023-02-30", pos);
        assertNull("Parsing illegal date with lenient=false must return null", d);
        assertEquals("Error index must be set to the point of failure", 0, pos.getErrorIndex());

        // With lenient=true parsing succeeds (rolls over to March 2)
        safe.setLenient(true);
        pos = new ParsePosition(0);
        d = safe.parse("2023-02-30", pos);
        assertNotNull("Parsing illegal date with lenient=true must succeed", d);
    }

    /* --------------------------------------------------------------
     *  setDateFormatSymbols(DateFormatSymbols)
     * -------------------------------------------------------------- */

    @Test
    public void testSetDateFormatSymbolsChangesMonthNames() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("MMMM dd, yyyy");
        DateFormatSymbols symbols = new DateFormatSymbols(Locale.US);
        String[] months = symbols.getMonths();
        months[0] = "Januar"; // replace January with a German spelling
        symbols.setMonths(months);
        safe.setDateFormatSymbols(symbols);

        Calendar cal = Calendar.getInstance();
        cal.set(2023, Calendar.JANUARY, 5);
        String result = safe.format(cal.getTime(), new StringBuffer(), new FieldPosition(0)).toString();
        assertTrue("Month name should reflect custom symbols", result.startsWith("Januar"));
    }

    @Test(expected = NullPointerException.class)
    public void testSetDateFormatSymbolsNullThrowsNPE() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        safe.setDateFormatSymbols(null);
    }

    /* --------------------------------------------------------------
     *  set2DigitYearStart(Date)
     * -------------------------------------------------------------- */

    @Test
    public void testSet2DigitYearStartAffectsTwoDigitParsing() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yy-MM-dd");
        // pivot year = 2000-01-01
        Calendar pivot = Calendar.getInstance();
        pivot.set(2000, Calendar.JANUARY, 1);
        safe.set2DigitYearStart(pivot.getTime());

        // "99-12-31" should be parsed as 1999-12-31
        ParsePosition pos = new ParsePosition(0);
        Date d = safe.parse("99-12-31", pos);
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        assertEquals(1999, cal.get(Calendar.YEAR));

        // "00-01-01" should be parsed as 2000-01-01
        pos = new ParsePosition(0);
        d = safe.parse("00-01-01", pos);
        cal.setTime(d);
        assertEquals(2000, cal.get(Calendar.YEAR));
    }

    @Test(expected = NullPointerException.class)
    public void testSet2DigitYearStartNullThrowsNPE() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        safe.set2DigitYearStart(null);
    }

    /* --------------------------------------------------------------
     *  toString()
     * -------------------------------------------------------------- */

    @Test
    public void testToStringReturnsOriginalPattern() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("dd/MM/yyyy");
        assertEquals("toString must return the pattern supplied to the constructor",
                "dd/MM/yyyy", safe.toString());
    }

    /* --------------------------------------------------------------
     *  equals(Object) and hashCode()
     * -------------------------------------------------------------- */

    @Test
    public void testEqualsDelegatesToUnderlyingSimpleDateFormat() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        SimpleDateFormat plain = new SimpleDateFormat(PATTERN);
        assertTrue("SafeSimpleDateFormat should be equal to equivalent SimpleDateFormat",
                safe.equals(plain));
        assertTrue("Equality must be symmetric", plain.equals(safe));

        SimpleDateFormat different = new SimpleDateFormat("yyyy/MM/dd");
        assertFalse("Different pattern must not be equal", safe.equals(different));
    }

    @Test
    public void testEqualsWithNonDateFormatObjectReturnsFalse() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        assertFalse("Equals with unrelated object must be false", safe.equals("some string"));
    }

    @Test
    public void testHashCodeMatchesUnderlyingSimpleDateFormat() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(PATTERN);
        SimpleDateFormat plain = new SimpleDateFormat(PATTERN);
        assertEquals("hashCode must be delegated", plain.hashCode(), safe.hashCode());
    }

    /* --------------------------------------------------------------
     *  Thread-safety sanity check – concurrent formatting
     * -------------------------------------------------------------- */

    @Test
    public void testConcurrentFormattingDoesNotCorruptState() throws Exception {
        final SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        final int threadCount = 10;
        final int iterations = 1000;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(exec.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    String formatted = safe.format(sampleDate, new StringBuffer(), new FieldPosition(0)).toString();
                    if (!formatted.contains("2023")) {
                        return false;
                    }
                }
                return true;
            }));
        }

        for (Future<Boolean> f : futures) {
            assertTrue("All threads must format correctly without interference", f.get());
        }
        exec.shutdownNow();
    }
}
