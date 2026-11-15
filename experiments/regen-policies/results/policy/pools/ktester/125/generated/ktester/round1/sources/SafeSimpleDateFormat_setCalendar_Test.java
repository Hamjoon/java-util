package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.text.SimpleDateFormat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import java.text.ParsePosition;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setCalendar_Test {

    /**
     * The format string used for the tests
     */
    private static final String TEST_FORMAT = "yyyy/MM/dd";

    /**
     * Mocked SimpleDateFormat that will be returned by the static factory
     */
    private SimpleDateFormat mockedSdf;

    /**
     * Mocked static scope for {@link SafeSimpleDateFormat#getDateFormat(String)}
     */
    private MockedStatic<SafeSimpleDateFormat> safeSdfStaticMock;

    /**
     * Instance under test
     */
    private SafeSimpleDateFormat safeSdf;

    @BeforeEach
    void setupBeforeEach() {
        // Create a mock SimpleDateFormat that will record interactions for the basic test
        mockedSdf = mock(SimpleDateFormat.class);
        // Mock the static method getDateFormat(String)
        safeSdfStaticMock = Mockito.mockStatic(SafeSimpleDateFormat.class);
        /* -------------------------------------------------------------
         * Stubbing strategy:
         *   – For the specific format used by the first test we return the mock.
         *   – For any other format we call the real static method so that the
         *     real caching logic of SafeSimpleDateFormat is exercised.
         * ------------------------------------------------------------- */
        safeSdfStaticMock.when(() -> SafeSimpleDateFormat.getDateFormat(anyString())).thenAnswer((Answer<SimpleDateFormat>) invocation -> {
            String fmt = invocation.getArgument(0);
            if (TEST_FORMAT.equals(fmt)) {
                return mockedSdf;
            }
            return invocation.callRealMethod();
        });
        // Construct the object under test – the constructor will call the mocked static method
        safeSdf = new SafeSimpleDateFormat(TEST_FORMAT);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the static mock so that other tests are not affected
        safeSdfStaticMock.close();
    }

    @Test
    void setCalendar_delegatesToUnderlyingSimpleDateFormat() {
        // GIVEN – a specific Calendar instance
        Calendar calendar = Calendar.getInstance();
        // make the calendar deterministic
        calendar.clear();
        calendar.set(2023, Calendar.JANUARY, 15, 10, 30, 45);
        calendar.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        // WHEN – the method under test is invoked
        safeSdf.setCalendar(calendar);
        // THEN – the underlying SimpleDateFormat should receive the same Calendar object
        verify(mockedSdf, times(1)).setCalendar(same(calendar));
    }

    /* -------------------------------------------------------------
     * 2. Cache miss – first use of a format string
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_cacheMissFirstUse() {
        String fmt = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(fmt);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.clear();
        cal.set(2022, Calendar.FEBRUARY, 20);
        // first call – cache is empty for this format
        sdf.setCalendar(cal);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(fmt);
        assertSame(cal, underlying.getCalendar(), "The SimpleDateFormat's calendar should be the exact instance supplied.");
    }

    /* -------------------------------------------------------------
     * 3. Cache hit – format string already cached
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_cacheHitReusesSimpleDateFormat() {
        String fmt = "dd/MM/yyyy";
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat(fmt);
        Calendar cal1 = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal1.clear();
        cal1.set(2021, Calendar.MARCH, 10);
        sdf1.setCalendar(cal1);
        // second call with a different calendar but same format string
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat(fmt);
        Calendar cal2 = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        cal2.clear();
        cal2.set(2021, Calendar.MARCH, 10);
        sdf2.setCalendar(cal2);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(fmt);
        assertSame(cal2, underlying.getCalendar(), "After the second setCalendar, the cached SimpleDateFormat should hold the second Calendar.");
    }

    /* -------------------------------------------------------------
     * 4. Null calendar argument – expect NPE
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_nullCalendarThrowsNPE() {
        assertThrows(NullPointerException.class, () -> safeSdf.setCalendar(null), "Passing a null Calendar should result in NullPointerException.");
    }

    /* -------------------------------------------------------------
     * 5. Different TimeZone handling – the TimeZone is preserved
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_preservesTimeZone() {
        String fmt = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(fmt);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        cal.clear();
        cal.set(2020, Calendar.JUNE, 1);
        sdf.setCalendar(cal);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(fmt);
        assertEquals(TimeZone.getTimeZone("Asia/Tokyo"), underlying.getCalendar().getTimeZone(), "The TimeZone of the SimpleDateFormat's calendar must match the supplied Calendar.");
    }

    /* -------------------------------------------------------------
     * 6. Repeated setCalendar calls with the same Calendar instance
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_repeatedCallsWithSameInstance() {
        String fmt = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(fmt);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(2019, Calendar.DECEMBER, 31);
        sdf.setCalendar(cal);
        // second call with the same reference
        sdf.setCalendar(cal);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(fmt);
        assertSame(cal, underlying.getCalendar(), "The SimpleDateFormat should still hold the original Calendar instance after repeated calls.");
    }

    /* -------------------------------------------------------------
     * 7. Cache isolation – different format strings must not share calendars
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_cacheIsolationDifferentFormats() {
        String fmt1 = "yyyy-MM-dd";
        String fmt2 = "dd MMM yyyy";
        // First format – set UTC calendar
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat(fmt1);
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.clear();
        utcCal.set(2022, Calendar.JANUARY, 1);
        sdf1.setCalendar(utcCal);
        // Second format – do NOT set any calendar
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat(fmt2);
        SimpleDateFormat underlying2 = SafeSimpleDateFormat.getDateFormat(fmt2);
        Calendar cal2 = underlying2.getCalendar();
        // The second SimpleDateFormat must NOT reference the UTC calendar
        assertNotSame(utcCal, cal2, "Calendars must be isolated per format string; the second format should not have the UTC calendar.");
    }

    /* -------------------------------------------------------------
     * 8. Formatting reflects calendar's timezone
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_formattingRespectsCalendarTimeZone() {
        String fmt = "yyyy-MM-dd HH:mm:ss Z";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(fmt);
        Calendar tokyoCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        // epoch start
        tokyoCal.clear();
        tokyoCal.setTimeInMillis(0L);
        sdf.setCalendar(tokyoCal);
        String result = sdf.format(new Date(0L));
        assertEquals("1970-01-01 09:00:00 +0900", result, "Formatting should use the timezone of the Calendar supplied via setCalendar.");
    }

    /* -------------------------------------------------------------
     * 9. setCalendar overrides previously set TimeZone
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_overwritesPreviouslySetTimeZone() {
        String fmt = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(fmt);
        // Set an explicit UTC timezone first
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Now set a Calendar with a different timezone
        Calendar kolkataCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kolkata"));
        kolkataCal.clear();
        kolkataCal.set(2021, Calendar.APRIL, 5);
        sdf.setCalendar(kolkataCal);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(fmt);
        assertEquals(TimeZone.getTimeZone("Asia/Kolkata"), underlying.getCalendar().getTimeZone(), "The Calendar's timezone must overwrite any timezone previously set via setTimeZone.");
    }

    /* -------------------------------------------------------------
     * 10. Subclass Calendar handling (GregorianCalendar)
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_acceptsGregorianCalendarSubclass() {
        String fmt = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(fmt);
        GregorianCalendar gregCal = new GregorianCalendar(TimeZone.getTimeZone("Europe/Paris"));
        gregCal.clear();
        gregCal.set(2023, Calendar.JULY, 14);
        sdf.setCalendar(gregCal);
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(fmt);
        assertSame(gregCal, underlying.getCalendar(), "The exact GregorianCalendar instance should be stored.");
        assertTrue(underlying.getCalendar() instanceof GregorianCalendar, "The stored calendar must be a GregorianCalendar.");
    }

    /* -------------------------------------------------------------
     * 11. Concurrent setCalendar calls – thread safety
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_concurrentCallsDoNotThrow() throws InterruptedException, ExecutionException {
        String fmt = "yyyy-MM-dd";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(fmt);
        Calendar nyCal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        nyCal.clear();
        nyCal.set(2022, Calendar.MAY, 1);
        Calendar tokyoCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        tokyoCal.clear();
        tokyoCal.set(2022, Calendar.MAY, 1);
        ExecutorService exec = Executors.newFixedThreadPool(2);
        Callable<Void> task1 = () -> {
            sdf.setCalendar(nyCal);
            return null;
        };
        Callable<Void> task2 = () -> {
            sdf.setCalendar(tokyoCal);
            return null;
        };
        Future<Void> f1 = exec.submit(task1);
        Future<Void> f2 = exec.submit(task2);
        // Await both tasks – any exception will be re‑thrown here
        f1.get();
        f2.get();
        exec.shutdownNow();
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(fmt);
        TimeZone tz = underlying.getCalendar().getTimeZone();
        // The final timezone can be either of the two – both are acceptable
        boolean isNy = tz.equals(TimeZone.getTimeZone("America/New_York"));
        boolean isTokyo = tz.equals(TimeZone.getTimeZone("Asia/Tokyo"));
        assertTrue(isNy || isTokyo, "After concurrent calls the calendar's timezone should be either New York or Tokyo, and no exception should have been thrown.");
    }

    /* -------------------------------------------------------------
     * 12. Invalid format string handling – null or malformed pattern
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_invalidFormatString_nullPatternThrows() {
        Calendar cal = Calendar.getInstance();
        assertThrows(IllegalArgumentException.class, () -> new SafeSimpleDateFormat(null).setCalendar(cal), "Creating SafeSimpleDateFormat with null pattern should cause IllegalArgumentException on setCalendar.");
    }

    @Test
    void setCalendar_invalidFormatString_malformedPatternThrows() {
        Calendar cal = Calendar.getInstance();
        // "@@@": not a valid date pattern – SimpleDateFormat constructor throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> new SafeSimpleDateFormat("@@@").setCalendar(cal), "A malformed pattern should cause IllegalArgumentException when setCalendar triggers getDateFormat.");
    }
}
