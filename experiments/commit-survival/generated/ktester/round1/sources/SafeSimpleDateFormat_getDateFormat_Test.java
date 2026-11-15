package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_getDateFormat_Test {

    @AfterEach
    void teardownAfterEach() {
        // Ensure the thread‑local cache is cleared after each test to avoid cross‑test contamination.
        try {
            Field tlField = SafeSimpleDateFormat.class.getDeclaredField("_dateFormats");
            tlField.setAccessible(true);
            @SuppressWarnings("unchecked")
            ThreadLocal<Map<String, SimpleDateFormat>> tl = (ThreadLocal<Map<String, SimpleDateFormat>>) tlField.get(null);
            // clears the ThreadLocal for the current thread
            tl.remove();
        } catch (Exception e) {
            // If reflection fails we let the test fail – this should never happen.
            throw new RuntimeException(e);
        }
    }

    /**
     * Replaces the thread‑local map with the supplied map.
     */
    private static void setThreadLocalMap(Map<String, SimpleDateFormat> map) throws Exception {
        Field tlField = SafeSimpleDateFormat.class.getDeclaredField("_dateFormats");
        tlField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Map<String, SimpleDateFormat>> tl = (ThreadLocal<Map<String, SimpleDateFormat>>) tlField.get(null);
        tl.set(map);
    }

    /**
     * Clears the thread‑local cache for the current thread.
     */
    private static void clearThreadLocalMap() throws Exception {
        Field tlField = SafeSimpleDateFormat.class.getDeclaredField("_dateFormats");
        tlField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Map<String, SimpleDateFormat>> tl = (ThreadLocal<Map<String, SimpleDateFormat>>) tlField.get(null);
        tl.remove();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDateFormatReturnsUsableFormatter() throws Exception {
        // GIVEN
        String pattern = "yyyy-MM-dd HH:mm:ss";
        Date now = new Date();
        // WHEN
        SimpleDateFormat sdf = SafeSimpleDateFormat.getDateFormat(pattern);
        String formatted = sdf.format(now);
        // THEN
        assertNotNull(sdf, "Formatter should not be null");
        // Verify that the produced string matches the pattern using a fresh SimpleDateFormat.
        SimpleDateFormat verifier = new SimpleDateFormat(pattern);
        assertEquals(verifier.format(now), formatted, "Formatter should produce the same output as a regular SimpleDateFormat");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDateFormatCachesSameInstanceForSamePattern() {
        // GIVEN
        String pattern = "dd/MM/yyyy";
        // WHEN
        SimpleDateFormat first = SafeSimpleDateFormat.getDateFormat(pattern);
        SimpleDateFormat second = SafeSimpleDateFormat.getDateFormat(pattern);
        // THEN
        assertSame(first, second, "Subsequent calls with the same pattern should return the identical SimpleDateFormat instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDateFormatReturnsDifferentInstancesForDifferentPatterns() {
        // GIVEN
        String patternA = "yyyyMMdd";
        String patternB = "MM-dd-yyyy";
        // WHEN
        SimpleDateFormat fmtA = SafeSimpleDateFormat.getDateFormat(patternA);
        SimpleDateFormat fmtB = SafeSimpleDateFormat.getDateFormat(patternB);
        // THEN
        assertNotSame(fmtA, fmtB, "Different patterns must not share the same SimpleDateFormat instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDateFormatIsThreadLocal() throws Exception {
        // GIVEN
        String pattern = "yyyy-MM-dd";
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<SimpleDateFormat> task = () -> SafeSimpleDateFormat.getDateFormat(pattern);
        // WHEN
        Future<SimpleDateFormat> future1 = executor.submit(task);
        Future<SimpleDateFormat> future2 = executor.submit(task);
        SimpleDateFormat fmtInThread1 = future1.get();
        SimpleDateFormat fmtInThread2 = future2.get();
        // THEN
        // The two threads should have distinct SimpleDateFormat objects because each
        // thread owns its own ThreadLocal map.
        assertNotSame(fmtInThread1, fmtInThread2, "Each thread must receive its own SimpleDateFormat instance (ThreadLocal isolation)");
        executor.shutdownNow();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getDateFormatWithUniquePatternPerTest() {
        // GIVEN
        String uniquePattern = "yyyy-MM-dd-" + UUID.randomUUID();
        // WHEN
        SimpleDateFormat formatter = SafeSimpleDateFormat.getDateFormat(uniquePattern);
        // THEN
        assertNotNull(formatter, "Formatter created with a unique pattern should not be null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void cacheMissCreatesAndCachesFormatter() throws Exception {
        // GIVEN
        String pattern = "yyyy-MM-dd";
        // Ensure the thread‑local map is empty for this test.
        clearThreadLocalMap();
        // WHEN
        SimpleDateFormat sdf = SafeSimpleDateFormat.getDateFormat(pattern);
        // THEN
        assertNotNull(sdf, "Formatter should be created");
        assertEquals(pattern, sdf.toPattern(), "Formatter pattern must match the requested pattern");
        // Verify that the formatter is now present in the thread‑local map.
        Map<String, SimpleDateFormat> map = getThreadLocalMap();
        assertTrue(map.containsKey(pattern), "Thread‑local cache must contain the newly created formatter");
        assertSame(sdf, map.get(pattern), "Cached instance must be the same as the returned instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void cacheHitReturnsExistingFormatter() throws Exception {
        // GIVEN
        String pattern = "dd/MM/yyyy";
        SimpleDateFormat prepopulated = new SimpleDateFormat(pattern);
        Map<String, SimpleDateFormat> customMap = new ConcurrentHashMap<>();
        customMap.put(pattern, prepopulated);
        setThreadLocalMap(customMap);
        // WHEN
        SimpleDateFormat result = SafeSimpleDateFormat.getDateFormat(pattern);
        // THEN
        assertSame(prepopulated, result, "Method must return the formatter already stored in the cache");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void concurrentInsertionSimulationUsesExistingFormatter() throws Exception {
        // GIVEN
        String pattern = "MM-dd-yyyy";
        SimpleDateFormat existing = new SimpleDateFormat(pattern);
        @SuppressWarnings("unchecked")
        Map<String, SimpleDateFormat> mockMap = mock(Map.class);
        // get returns null to simulate a miss
        when(mockMap.get(pattern)).thenReturn(null);
        // putIfAbsent returns the existing formatter, simulating a race where another thread
        // already inserted the formatter.
        when(mockMap.putIfAbsent(eq(pattern), any(SimpleDateFormat.class))).thenReturn(existing);
        setThreadLocalMap(mockMap);
        // WHEN
        SimpleDateFormat result = SafeSimpleDateFormat.getDateFormat(pattern);
        // THEN
        assertSame(existing, result, "When putIfAbsent returns a non‑null value, that formatter must be used");
        // Verify that a new SimpleDateFormat was indeed constructed (the mock's putIfAbsent was called)
        verify(mockMap).putIfAbsent(eq(pattern), any(SimpleDateFormat.class));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullFormatThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> SafeSimpleDateFormat.getDateFormat(null), "Passing null should result in NullPointerException from SimpleDateFormat constructor");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void emptyPatternCreatesFormatter() {
        // GIVEN
        String pattern = "";
        // WHEN
        SimpleDateFormat sdf = SafeSimpleDateFormat.getDateFormat(pattern);
        String formatted = sdf.format(new Date());
        // THEN
        assertNotNull(sdf, "Formatter should not be null for empty pattern");
        assertEquals(pattern, sdf.toPattern(), "Pattern of the formatter must be empty string");
        assertEquals("", formatted, "Formatting with an empty pattern should yield an empty string");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void invalidPatternUnmatchedQuoteThrowsIllegalArgumentException() {
        // unmatched single quote
        String pattern = "yyyy-MM-dd'";
        assertThrows(IllegalArgumentException.class, () -> SafeSimpleDateFormat.getDateFormat(pattern), "Unmatched quote in pattern must cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void patternWithTimezoneIsAcceptedAndCached() throws Exception {
        // GIVEN
        String pattern = "yyyy-MM-dd'T'HH:mm:ssZ";
        // WHEN
        SimpleDateFormat sdf = SafeSimpleDateFormat.getDateFormat(pattern);
        // THEN
        assertNotNull(sdf, "Formatter should be created");
        assertEquals(pattern, sdf.toPattern(), "Formatter must retain the exact pattern including timezone literals");
        // Verify caching
        Map<String, SimpleDateFormat> map = getThreadLocalMap();
        assertTrue(map.containsKey(pattern), "Cache must contain the formatter for timezone pattern");
        assertSame(sdf, map.get(pattern), "Cached instance must be identical to the returned instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void threadLocalIsolationDistinctInstancesSamePattern() throws Exception {
        // GIVEN
        String pattern = "dd-MM-yyyy";
        ExecutorService exec = Executors.newFixedThreadPool(2);
        Callable<SimpleDateFormat> task = () -> SafeSimpleDateFormat.getDateFormat(pattern);
        // WHEN
        Future<SimpleDateFormat> f1 = exec.submit(task);
        Future<SimpleDateFormat> f2 = exec.submit(task);
        SimpleDateFormat fmt1 = f1.get();
        SimpleDateFormat fmt2 = f2.get();
        // THEN
        assertNotSame(fmt1, fmt2, "Different threads must receive distinct formatter instances for the same pattern");
        exec.shutdownNow();
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mutationPersistsAcrossCacheRetrievals() {
        // GIVEN
        String pattern = "MM/dd/yyyy";
        SimpleDateFormat first = SafeSimpleDateFormat.getDateFormat(pattern);
        // mutate
        first.setLenient(false);
        // WHEN
        SimpleDateFormat second = SafeSimpleDateFormat.getDateFormat(pattern);
        // THEN
        assertSame(first, second, "Cache must return the same instance");
        assertFalse(second.isLenient(), "Mutation (lenient = false) must be visible on subsequent retrievals");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mapGetThrowsRuntimeExceptionPropagates() throws Exception {
        // GIVEN
        String pattern = "yyyy-MM-dd";
        @SuppressWarnings("unchecked")
        Map<String, SimpleDateFormat> mockMap = mock(Map.class);
        when(mockMap.get(pattern)).thenThrow(new RuntimeException("simulated get failure"));
        setThreadLocalMap(mockMap);
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> SafeSimpleDateFormat.getDateFormat(pattern), "Exception from map.get should propagate unchanged");
        assertEquals("simulated get failure", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void mapPutIfAbsentThrowsRuntimeExceptionPropagates() throws Exception {
        // GIVEN
        String pattern = "dd/MM/yyyy";
        @SuppressWarnings("unchecked")
        Map<String, SimpleDateFormat> mockMap = mock(Map.class);
        // get returns null to force creation path
        when(mockMap.get(pattern)).thenReturn(null);
        // putIfAbsent throws
        when(mockMap.putIfAbsent(eq(pattern), any(SimpleDateFormat.class))).thenThrow(new RuntimeException("simulated putIfAbsent failure"));
        setThreadLocalMap(mockMap);
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> SafeSimpleDateFormat.getDateFormat(pattern), "Exception from putIfAbsent should propagate unchanged");
        assertEquals("simulated putIfAbsent failure", ex.getMessage());
    }

    /**
     * Retrieves the current thread‑local map used by {@link SafeSimpleDateFormat}.
     */
    @SuppressWarnings("unchecked")
    private static Map<String, SimpleDateFormat> getThreadLocalMap() throws Exception {
        Field tlField = SafeSimpleDateFormat.class.getDeclaredField("_dateFormats");
        tlField.setAccessible(true);
        ThreadLocal<Map<String, SimpleDateFormat>> tl = (ThreadLocal<Map<String, SimpleDateFormat>>) tlField.get(null);
        return tl.get();
    }
}
