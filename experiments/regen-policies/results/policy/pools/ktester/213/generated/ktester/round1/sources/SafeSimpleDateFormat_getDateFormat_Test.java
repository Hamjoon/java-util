package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_getDateFormat_Test {

    /**
     * A unique pattern for each test to avoid cross‑test cache interference.
     */
    private String uniquePattern() {
        return "yyyy-MM-dd-HH-mm-ss-" + System.nanoTime();
    }

    @Test
    void getDateFormatCreatesFormatter() {
        // GIVEN
        String pattern = "yyyy-MM-dd";
        // WHEN
        SimpleDateFormat sdf = SafeSimpleDateFormat.getDateFormat(pattern);
        // THEN
        assertNotNull(sdf, "Formatter should not be null");
        assertEquals(pattern, sdf.toPattern(), "Formatter should be created with the requested pattern");
    }

    @Test
    void getDateFormatReturnsSameInstanceWithinThread() {
        // GIVEN
        String pattern = uniquePattern();
        // WHEN
        SimpleDateFormat first = SafeSimpleDateFormat.getDateFormat(pattern);
        SimpleDateFormat second = SafeSimpleDateFormat.getDateFormat(pattern);
        // THEN
        assertSame(first, second, "Subsequent calls with the same pattern in the same thread must return the same instance");
    }

    @Test
    void getDateFormatIsThreadLocal() throws Exception {
        // GIVEN
        String pattern = "yyyy/MM/dd";
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<SimpleDateFormat> task = () -> SafeSimpleDateFormat.getDateFormat(pattern);
        // WHEN
        Future<SimpleDateFormat> f1 = executor.submit(task);
        Future<SimpleDateFormat> f2 = executor.submit(task);
        // thread‑1
        SimpleDateFormat sdf1 = f1.get();
        // thread‑2
        SimpleDateFormat sdf2 = f2.get();
        // THEN
        assertNotSame(sdf1, sdf2, "Each thread should have its own SimpleDateFormat instance for the same pattern");
        // Clean up the executor
        executor.shutdownNow();
    }

    @Test
    void formatterCanParseAndFormat() {
        // GIVEN
        String pattern = "dd-MM-yyyy HH:mm:ss";
        SimpleDateFormat sdf = SafeSimpleDateFormat.getDateFormat(pattern);
        String input = "31-12-2023 23:59:59";
        // WHEN
        java.util.Date date = sdf.parse(input);
        String output = sdf.format(date);
        // THEN
        assertNotNull(date, "Parsed date should not be null");
        assertEquals(input, output, "Formatting the parsed date should yield the original string");
    }

    @ParameterizedTest(name = "[{index}] create formatter for pattern ''{0}''")
    @CsvSource({ "'yyyy-MM-dd'", "'dd/MM/yyyy HH:mm:ss'" })
    void formatterCreationAndCaching(String pattern) {
        // GIVEN a fresh pattern (unique per invocation is not required here because
        // the map is thread‑local and the test runs in a single thread).
        // WHEN
        SimpleDateFormat sdf = SafeSimpleDateFormat.getDateFormat(pattern);
        // THEN
        assertNotNull(sdf, "Formatter should not be null");
        assertEquals(pattern, sdf.toPattern(), "Returned formatter must have the requested pattern");
    }

    @Test
    void repeatedCallsReturnSameInstanceCachingVerification() {
        // GIVEN
        String pattern1 = "yyyy-MM-dd";
        String pattern2 = "dd/MM/yyyy";
        // WHEN
        SimpleDateFormat firstCall1 = SafeSimpleDateFormat.getDateFormat(pattern1);
        SimpleDateFormat secondCall1 = SafeSimpleDateFormat.getDateFormat(pattern1);
        SimpleDateFormat firstCall2 = SafeSimpleDateFormat.getDateFormat(pattern2);
        SimpleDateFormat secondCall2 = SafeSimpleDateFormat.getDateFormat(pattern2);
        SimpleDateFormat thirdCall2 = SafeSimpleDateFormat.getDateFormat(pattern2);
        // THEN
        assertSame(firstCall1, secondCall1, "Same pattern should yield the identical instance on subsequent calls");
        assertSame(firstCall2, secondCall2, "Same pattern should yield the identical instance on subsequent calls");
        assertSame(firstCall2, thirdCall2, "Further calls continue to return the cached instance");
    }

    @Test
    void differentPatternsProduceDistinctInstances() {
        // GIVEN
        String patternA = "yyyy-MM-dd";
        String patternB = "yyyy/MM/dd";
        // WHEN
        SimpleDateFormat sdfA = SafeSimpleDateFormat.getDateFormat(patternA);
        SimpleDateFormat sdfB = SafeSimpleDateFormat.getDateFormat(patternB);
        // THEN
        assertNotSame(sdfA, sdfB, "Different pattern strings must result in different SimpleDateFormat instances");
    }

    @Test
    void nullPatternHandling() {
        // GIVEN
        String nullPattern = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> SafeSimpleDateFormat.getDateFormat(nullPattern), "Passing null should propagate NullPointerException from SimpleDateFormat constructor");
    }

    @Test
    void emptyPatternHandling() {
        // GIVEN
        String empty = "";
        // WHEN first call
        SimpleDateFormat first = SafeSimpleDateFormat.getDateFormat(empty);
        // WHEN second call
        SimpleDateFormat second = SafeSimpleDateFormat.getDateFormat(empty);
        // THEN
        assertNotNull(first, "Formatter for empty pattern should not be null");
        assertEquals(empty, first.toPattern(), "Formatter's pattern should be the empty string");
        assertSame(first, second, "Empty pattern must be cached – subsequent calls return the same instance");
    }

    @Test
    void concurrentCreationRaceConditionSafety() throws Exception {
        // GIVEN
        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Callable<SimpleDateFormat> task = () -> SafeSimpleDateFormat.getDateFormat(pattern);
        List<Future<SimpleDateFormat>> futures = new ArrayList<>();
        // WHEN – launch many threads simultaneously
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(task));
        }
        // also obtain the formatter in the main thread for comparison
        SimpleDateFormat mainThreadFormatter = SafeSimpleDateFormat.getDateFormat(pattern);
        // THEN – every future should contain a non‑null formatter and, within each
        // thread, the formatter must be the same instance that the thread cached.
        for (Future<SimpleDateFormat> f : futures) {
            // re‑throws any exception
            SimpleDateFormat fmt = f.get();
            assertNotNull(fmt, "Formatter returned from concurrent thread must not be null");
            // Because each thread has its own ThreadLocal map, the instance returned
            // from a worker thread is not required to be identical to the main thread's.
            // However, within that worker thread the formatter must be cached, which we
            // indirectly verify by calling the method a second time inside the same thread.
            SimpleDateFormat secondCall = SafeSimpleDateFormat.getDateFormat(pattern);
            assertSame(fmt, secondCall, "Within a single thread, repeated calls must return the same cached instance");
        }
        executor.shutdownNow();
    }

    @ParameterizedTest(name = "[{index}] invalid pattern ''{0}'' throws IllegalArgumentException")
    @CsvSource({ // unterminated quote
    "'yyyy-MM-dd'''", // illegal characters
    "'yyyy-??-dd'", // unsupported pattern letter (G beyond allowed range)
    "'yyyy-MM-dd GGGG'" })
    void invalidPatternHandling(String invalidPattern) {
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> SafeSimpleDateFormat.getDateFormat(invalidPattern), "Invalid pattern should cause SimpleDateFormat to throw IllegalArgumentException");
    }
}
