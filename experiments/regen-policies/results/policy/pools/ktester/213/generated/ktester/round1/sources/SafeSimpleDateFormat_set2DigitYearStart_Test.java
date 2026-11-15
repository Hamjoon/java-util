package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Date;
import java.text.SimpleDateFormat;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_set2DigitYearStart_Test {

    private static final String FORMAT = "yy/MM/dd";

    private SafeSimpleDateFormat safeSdf;

    private static final String VALID_FORMAT = "yy/MM/dd";

    private static final String INVALID_FORMAT = "invalid_format";

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test
        safeSdf = new SafeSimpleDateFormat(FORMAT);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the thread‑local cache to avoid cross‑test contamination.
        // The cache is stored in a private static ThreadLocal<Map<String,SimpleDateFormat>> field.
        Field cacheField = SafeSimpleDateFormat.class.getDeclaredField("THREAD_LOCAL");
        cacheField.setAccessible(true);
        ThreadLocal<?> threadLocal = (ThreadLocal<?>) cacheField.get(null);
        threadLocal.remove();
        safeSdf = null;
    }

    // -----------------------------------------------------------------
    static Stream<Date> validDateProvider() {
        return Stream.of(// Epoch
        new Date(0L), // One day after epoch
        new Date(86400000L), // One day before epoch
        new Date(-86400000L), // Current moment
        new Date(System.currentTimeMillis()));
    }

    @Test
    @DisplayName("set2DigitYearStart should delegate to the underlying SimpleDateFormat")
    void testSet2DigitYearStartUpdatesUnderlyingSimpleDateFormat() {
        // GIVEN
        // Epoch start
        Date expectedStart = new Date(0L);
        // WHEN
        safeSdf.set2DigitYearStart(expectedStart);
        // THEN
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(VALID_FORMAT);
        Date actualStart = underlying.get2DigitYearStart();
        assertEquals(expectedStart, actualStart, "The underlying SimpleDateFormat should have its 2‑digit year start set to the provided date");
    }

    /* --------------------------------------------------------------
     *  2. Exception handling – null Date argument.
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("set2DigitYearStart(null) should propagate IllegalArgumentException")
    void testSet2DigitYearStartNullDateThrows() {
        // GIVEN
        Date nullDate = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> safeSdf.set2DigitYearStart(nullDate), "Passing null to set2DigitYearStart must result in IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  3. Exception handling – unsupported/invalid format string.
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("set2DigitYearStart with an invalid format should propagate IllegalArgumentException")
    void testSet2DigitYearStartInvalidFormatThrows() {
        // GIVEN a SafeSimpleDateFormat created with a pattern SimpleDateFormat cannot parse
        SafeSimpleDateFormat sdfWithBadPattern = new SafeSimpleDateFormat(INVALID_FORMAT);
        Date anyDate = new Date();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> sdfWithBadPattern.set2DigitYearStart(anyDate), "An invalid pattern should cause SimpleDateFormat constructor to throw IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  4. Cache reuse – multiple calls affect the same SimpleDateFormat.
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Multiple set2DigitYearStart calls reuse the same SimpleDateFormat instance")
    void testCacheReuseMultipleCalls() {
        // GIVEN two distinct dates
        Date firstDate = new Date(1000L);
        Date secondDate = new Date(2000L);
        // Capture the underlying SimpleDateFormat instance before any call
        SimpleDateFormat firstUnderlying = SafeSimpleDateFormat.getDateFormat(VALID_FORMAT);
        // Ensure the cache is empty at the very beginning (firstUnderlying may be newly created)
        assertNotNull(firstUnderlying, "Underlying SimpleDateFormat should be created on first access");
        // WHEN – first call
        safeSdf.set2DigitYearStart(firstDate);
        Date afterFirst = firstUnderlying.get2DigitYearStart();
        // THEN – first call updates the start date
        assertEquals(firstDate, afterFirst, "First call should set the start date correctly");
        // WHEN – second call with a different date
        safeSdf.set2DigitYearStart(secondDate);
        SimpleDateFormat secondUnderlying = SafeSimpleDateFormat.getDateFormat(VALID_FORMAT);
        Date afterSecond = secondUnderlying.get2DigitYearStart();
        // THEN – same instance is reused and its start date is overwritten
        assertSame(firstUnderlying, secondUnderlying, "The SimpleDateFormat instance should be cached and reused across calls");
        assertEquals(secondDate, afterSecond, "Second call should overwrite the start date on the same underlying instance");
    }

    /* --------------------------------------------------------------
     *  5. Exception handling – null format string in constructor.
     * -------------------------------------------------------------- */
    @Test
    @DisplayName("Constructor with null format should cause NullPointerException on set2DigitYearStart")
    void testConstructorNullFormatThrows() {
        // GIVEN a SafeSimpleDateFormat with a null format string
        SafeSimpleDateFormat sdfNullFormat = new SafeSimpleDateFormat(null);
        Date anyDate = new Date();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> sdfNullFormat.set2DigitYearStart(anyDate), "A null format string should propagate NullPointerException when SimpleDateFormat is instantiated");
    }

    /* --------------------------------------------------------------
     *  6. Parameterized test for several valid dates (optional illustration).
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "set2DigitYearStart with date {0}")
    @MethodSource("validDateProvider")
    @DisplayName("set2DigitYearStart works for a range of valid dates")
    void testSet2DigitYearStartWithVariousDates(Date date) {
        // WHEN
        safeSdf.set2DigitYearStart(date);
        // THEN
        SimpleDateFormat underlying = SafeSimpleDateFormat.getDateFormat(VALID_FORMAT);
        assertEquals(date, underlying.get2DigitYearStart(), "Underlying SimpleDateFormat must reflect the date supplied to set2DigitYearStart");
    }
}
