package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.text.SimpleDateFormat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.text.ParsePosition;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_setCalendar_Test {

    private static final String FORMAT = "yyyy/MM/dd";

    private SafeSimpleDateFormat safeSdf;

    private static final String DEFAULT_FORMAT = "yyyy/MM/dd";

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test so the underlying SimpleDateFormat
        // is initialized with a clean Calendar.
        safeSdf = new SafeSimpleDateFormat(FORMAT);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the thread‑local cache used by SafeSimpleDateFormat to avoid
        // interference between tests. The cache is stored in a private static
        // ThreadLocal field named "threadLocal". Using reflection keeps the test
        // decoupled from internal implementation details while still providing
        // a clean state.
        try {
            java.lang.reflect.Field tlField = SafeSimpleDateFormat.class.getDeclaredField("threadLocal");
            tlField.setAccessible(true);
            ThreadLocal<?> tl = (ThreadLocal<?>) tlField.get(null);
            if (tl != null) {
                tl.remove();
            }
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
            // If the internal field changes, the test will still be valid; we just
            // skip the cleanup.
        }
    }

    /* -------------------------------------------------------------
     *  Normal operation – valid format and calendar
     * ------------------------------------------------------------- */
    private static Stream<Arguments> normalCalendarProvider() {
        return Stream.of(// 2020‑01‑01 00:00:00 GMT+02:00 → formatted as 2020‑01‑01
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("GMT+02:00")) {

            {
                clear();
                set(2020, Calendar.JANUARY, 1, 0, 0, 0);
            }
        }, "2020/01/01"), // 2021‑12‑31 23:00:00 GMT‑05:00 → same instant as 2022‑01‑01 04:00:00 UTC
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("GMT-05:00")) {

            {
                clear();
                set(2021, Calendar.DECEMBER, 31, 23, 0, 0);
            }
        }, "2021/12/31"));
    }

    @Test
    void setCalendar_delegatesToUnderlyingSimpleDateFormat() {
        // GIVEN: a Calendar with a specific time zone and a known instant
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(2023, Calendar.JANUARY, 15, 10, 30, 45);
        long expectedMillis = calendar.getTimeInMillis();
        // WHEN: setCalendar is invoked on the SafeSimpleDateFormat instance
        safeSdf.setCalendar(calendar);
        // THEN: the underlying SimpleDateFormat obtained via getDateFormat should
        // have its calendar set to the same instance (or at least an equivalent state)
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(FORMAT);
        Calendar actualCalendar = underlying.getCalendar();
        assertNotNull(actualCalendar, "Underlying SimpleDateFormat should have a non‑null Calendar");
        assertEquals(expectedMillis, actualCalendar.getTimeInMillis(), "Calendar milliseconds should match the one passed to setCalendar");
        // Also verify that the time zone is preserved
        assertEquals(calendar.getTimeZone(), actualCalendar.getTimeZone(), "TimeZone should be identical after delegation");
    }

    @ParameterizedTest(name = "[{index}] Calendar {0.timeZone.id}")
    @MethodSource("normalCalendarProvider")
    void setCalendar_normalOperation(Calendar cal, String expectedFormatted) {
        // GIVEN a calendar with a specific time‑zone and instant
        safeSdf.setCalendar(cal);
        // WHEN formatting a Date that represents the same instant as the calendar fields
        Date date = cal.getTime();
        String result = safeSdf.format(date);
        // THEN the formatted string must match the expected date part (format is yyyy/MM/dd)
        assertEquals(expectedFormatted, result);
    }

    /* -------------------------------------------------------------
     *  Null calendar handling
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_nullCalendar_throwsNPE() {
        assertThrows(NullPointerException.class, () -> safeSdf.setCalendar(null));
    }

    /* -------------------------------------------------------------
     *  Calendar replacement effect
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_replacesPreviousCalendar() {
        // first calendar (GMT+01)
        Calendar first = new GregorianCalendar(TimeZone.getTimeZone("GMT+01:00")) {

            {
                clear();
                set(2022, Calendar.MARCH, 15, 12, 30, 0);
            }
        };
        safeSdf.setCalendar(first);
        assertEquals("2022/03/15", safeSdf.format(first.getTime()));
        // second calendar (GMT‑03) – same instant different zone
        Calendar second = new GregorianCalendar(TimeZone.getTimeZone("GMT-03:00")) {

            {
                clear();
                set(2022, Calendar.MARCH, 15, 8, 30, 0);
            }
        };
        safeSdf.setCalendar(second);
        assertEquals("2022/03/15", safeSdf.format(second.getTime()));
    }

    /* -------------------------------------------------------------
     *  Different format patterns – date and time
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_differentPatternWithTime() {
        String pattern = "yyyy-MM-dd HH:mm:ss";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT+02:00")) {

            {
                clear();
                set(2021, Calendar.JULY, 4, 15, 45, 30);
            }
        };
        sdf.setCalendar(cal);
        // The instant is 2021‑07‑04 13:45:30 UTC, but the formatter uses the calendar's TZ
        // same instant
        Date date = new Date(cal.getTimeInMillis());
        String formatted = sdf.format(date);
        assertEquals("2021-07-04 15:45:30", formatted);
    }

    /* -------------------------------------------------------------
     *  Calendar with custom TimeZone and timezone pattern
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_patternWithZoneOffset() {
        String pattern = "yyyy-MM-dd HH:mm:ss Z";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT-05:00")) {

            {
                clear();
                set(2020, Calendar.DECEMBER, 31, 23, 0, 0);
            }
        };
        sdf.setCalendar(cal);
        // same instant
        Date date = cal.getTime();
        String formatted = sdf.format(date);
        // Expected UTC offset +0000 because formatter prints the offset of the calendar's zone
        assertEquals("2020-12-31 23:00:00 -0500", formatted);
    }

    /* -------------------------------------------------------------
     *  Multiple sequential setCalendar calls
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_multipleSequentialCalls() {
        // first calendar GMT+09
        Calendar cal1 = new GregorianCalendar(TimeZone.getTimeZone("GMT+09:00")) {

            {
                clear();
                set(2019, Calendar.MAY, 1, 0, 0, 0);
            }
        };
        safeSdf.setCalendar(cal1);
        Date instant = new GregorianCalendar(TimeZone.getTimeZone("UTC")) {

            {
                clear();
                set(2019, Calendar.APRIL, 30, 15, 0, 0);
            }
        }.// same instant as cal1
        getTime();
        assertEquals("2019/04/30", safeSdf.format(instant));
        // second calendar GMT‑04
        Calendar cal2 = new GregorianCalendar(TimeZone.getTimeZone("GMT-04:00")) {

            {
                clear();
                set(2019, Calendar.MAY, 1, 0, 0, 0);
            }
        };
        safeSdf.setCalendar(cal2);
        assertEquals("2019/05/01", safeSdf.format(instant));
    }

    /* -------------------------------------------------------------
     *  Concurrent modification of calendar
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_concurrentModification_lastWriteWins() throws Exception {
        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Calendar calA = new GregorianCalendar(TimeZone.getTimeZone("GMT+00:00")) {

            {
                clear();
                set(2022, Calendar.JANUARY, 1, 0, 0, 0);
            }
        };
        Calendar calB = new GregorianCalendar(TimeZone.getTimeZone("GMT+05:00")) {

            {
                clear();
                set(2022, Calendar.JANUARY, 1, 0, 0, 0);
            }
        };
        Runnable taskA = () -> {
            ready.countDown();
            try {
                start.await();
            } catch (InterruptedException ignored) {
            }
            safeSdf.setCalendar(calA);
        };
        Runnable taskB = () -> {
            ready.countDown();
            try {
                start.await();
            } catch (InterruptedException ignored) {
            }
            safeSdf.setCalendar(calB);
        };
        exec.submit(taskA);
        exec.submit(taskB);
        // wait for both threads to be ready then release them simultaneously
        ready.await();
        start.countDown();
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);
        // The last thread that performed setCalendar wins – we cannot know which,
        // but the result must match one of the two possible formatted strings.
        // epoch (1970‑01‑01 00:00:00 UTC)
        Date instant = new Date(0L);
        String result = safeSdf.format(instant);
        // Build the two possible expected strings
        SimpleDateFormat fmt = new SimpleDateFormat(DEFAULT_FORMAT);
        fmt.setTimeZone(calA.getTimeZone());
        String expectedA = fmt.format(instant);
        fmt.setTimeZone(calB.getTimeZone());
        String expectedB = fmt.format(instant);
        assertTrue(result.equals(expectedA) || result.equals(expectedB), "Result should correspond to the calendar set by the thread that executed last");
    }

    /* -------------------------------------------------------------
     *  Exception when format string is null
     * ------------------------------------------------------------- */
    @Test
    void constructor_nullFormat_setCalendar_propagatesNPE() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(null);
        Calendar cal = Calendar.getInstance();
        assertThrows(NullPointerException.class, () -> sdf.setCalendar(cal));
    }

    /* -------------------------------------------------------------
     *  Exception when format string is invalid
     * ------------------------------------------------------------- */
    @Test
    void constructor_invalidFormat_setCalendar_propagatesIAE() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-??-dd");
        Calendar cal = Calendar.getInstance();
        assertThrows(IllegalArgumentException.class, () -> sdf.setCalendar(cal));
    }

    /* -------------------------------------------------------------
     *  Exception when Calendar's TimeZone is null
     * ------------------------------------------------------------- */
    @Test
    void setCalendar_calendarWithNullTimeZone_throwsNPE() {
        Calendar badCal = new Calendar() {

            @Override
            public void add(int field, int amount) {
            }

            @Override
            public void roll(int field, boolean up) {
            }

            @Override
            public int getMinimum(int field) {
                return 0;
            }

            @Override
            public int getMaximum(int field) {
                return 0;
            }

            @Override
            public int getGreatestMinimum(int field) {
                return 0;
            }

            @Override
            public int getLeastMaximum(int field) {
                return 0;
            }

            @Override
            public int getActualMinimum(int field) {
                return 0;
            }

            @Override
            public int getActualMaximum(int field) {
                return 0;
            }

            @Override
            public Date getTime() {
                return new Date();
            }

            @Override
            public void setTime(Date date) {
            }

            @Override
            public void setTimeInMillis(long millis) {
            }

            @Override
            public long getTimeInMillis() {
                return System.currentTimeMillis();
            }

            @Override
            public void clear() {
            }

            @Override
            public void clear(int field) {
            }

            @Override
            public void set(int year, int month, int date) {
            }

            @Override
            public void set(int year, int month, int date, int hourOfDay, int minute) {
            }

            @Override
            public void set(int year, int month, int date, int hourOfDay, int minute, int second) {
            }

            @Override
            public void setTimeZone(TimeZone value) {
            }

            // <-- null TZ
            @Override
            public TimeZone getTimeZone() {
                return null;
            }

            @Override
            public boolean isSet(int field) {
                return false;
            }

            @Override
            public boolean isLenient() {
                return true;
            }

            @Override
            public void setLenient(boolean lenient) {
            }

            @Override
            public int get(int field) {
                return 0;
            }

            @Override
            public void set(int field, int value) {
            }

            @Override
            public int getActualMaximum(int field) {
                return 0;
            }
        };
        assertThrows(NullPointerException.class, () -> safeSdf.setCalendar(badCal));
    }
}
