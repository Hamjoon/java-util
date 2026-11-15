package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_getDateFormat_Test {

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// First call with a new format string
        Arguments.of("yyyy-MM-dd", "yyyy-MM-dd", "new formatter cached"), // Complex ISO‑8601 pattern
        Arguments.of("yyyy-MM-dd'T'HH:mm:ss.SSSZ", "yyyy-MM-dd'T'HH:mm:ss.SSSZ", "complex pattern cached"), // Empty pattern (allowed by SimpleDateFormat)
        Arguments.of("", "", "empty pattern cached"), // Null pattern – handled separately in the test body
        Arguments.of(null, null, "null pattern throws NPE"));
    }

    /* --------------------------------------------------------------------- */
    private static void clearThreadLocalCache() throws Exception {
        Field tlField = SafeSimpleDateFormat.class.getDeclaredField("_dateFormats");
        tlField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ThreadLocal<Map<String, SimpleDateFormat>> tl = (ThreadLocal<Map<String, SimpleDateFormat>>) tlField.get(null);
        Map<String, SimpleDateFormat> map = tl.get();
        if (map != null) {
            map.clear();
        }
    }

    /* --------------------------------------------------------------------- */
    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void sameFormatReturnsSameFormatterInstance() {
        // unique per test
        String format = "yyyy/MM/dd-HH:mm:ss-" + System.nanoTime();
        SimpleDateFormat first = SafeSimpleDateFormat.getDateFormat(format);
        SimpleDateFormat second = SafeSimpleDateFormat.getDateFormat(format);
        assertNotNull(first, "First formatter should not be null");
        assertNotNull(second, "Second formatter should not be null");
        assertSame(first, second, "Formatters for the same pattern must be the same instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void differentFormatsReturnDifferentFormatterInstances() {
        String formatA = "yyyy/MM/dd-" + System.nanoTime();
        String formatB = "MM-dd-yyyy-" + System.nanoTime();
        SimpleDateFormat formatterA = SafeSimpleDateFormat.getDateFormat(formatA);
        SimpleDateFormat formatterB = SafeSimpleDateFormat.getDateFormat(formatB);
        assertNotNull(formatterA);
        assertNotNull(formatterB);
        assertNotSame(formatterA, formatterB, "Formatters for different patterns must not be the same instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void formatterIsThreadLocal() throws ExecutionException, InterruptedException {
        String format = "dd-MM-yyyy-" + System.nanoTime();
        // Formatter in the main (test) thread
        SimpleDateFormat mainThreadFormatter = SafeSimpleDateFormat.getDateFormat(format);
        assertNotNull(mainThreadFormatter);
        // Formatter in a separate thread
        CompletableFuture<SimpleDateFormat> future = CompletableFuture.supplyAsync(() -> SafeSimpleDateFormat.getDateFormat(format));
        SimpleDateFormat otherThreadFormatter = future.get();
        assertNotNull(otherThreadFormatter);
        // They must be different objects because each thread has its own map
        assertNotSame(mainThreadFormatter, otherThreadFormatter, "Each thread must have its own SimpleDateFormat instance for the same pattern");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void formatterCanFormatAndParseDate() throws Exception {
        String format = "yyyy-MM-dd HH:mm:ss.SSS-" + System.nanoTime();
        SimpleDateFormat sdf = SafeSimpleDateFormat.getDateFormat(format);
        assertNotNull(sdf);
        Date now = new Date();
        String formatted = sdf.format(now);
        assertNotNull(formatted);
        assertFalse(formatted.isEmpty(), "Formatted string should not be empty");
        // Parse the string back to a Date and compare (allowing a small delta)
        Date parsed = sdf.parse(formatted);
        assertNotNull(parsed);
        assertEquals(now.getTime(), parsed.getTime(), "Parsed date should match the original date (millisecond precision)");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] format=''{0}'' → expected={2}")
    @MethodSource("basicFunctionalityProvider")
    void basicFunctionality(String format, String expectedPattern, String expectation) throws Exception {
        if (format == null) {
            // Expect NPE from SimpleDateFormat constructor
            assertThrows(NullPointerException.class, () -> SafeSimpleDateFormat.getDateFormat(null), "Calling with null should throw NullPointerException");
            return;
        }
        SimpleDateFormat sdf = SafeSimpleDateFormat.getDateFormat(format);
        assertNotNull(sdf, "Formatter should never be null for non‑null pattern");
        // Verify the internal pattern via reflection (SimpleDateFormat stores it in a private field)
        String actualPattern = (String) getPrivateField(sdf, "pattern");
        assertEquals(expectedPattern, actualPattern, "The formatter's pattern must match the requested pattern");
        // Ensure the formatter is cached – a second call returns the same instance
        SimpleDateFormat second = SafeSimpleDateFormat.getDateFormat(format);
        assertSame(sdf, second, "Subsequent calls with the same pattern must return the cached instance");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void formatterCreatedAfterThreadLocalClearIsNewInstance() throws Exception {
        String format = "yyyy-MM-dd";
        // First formatter – cached in the current thread
        SimpleDateFormat first = SafeSimpleDateFormat.getDateFormat(format);
        assertNotNull(first);
        // Clear the ThreadLocal map via reflection
        clearThreadLocalCache();
        // Second formatter – should be a *different* instance because the cache was cleared
        SimpleDateFormat second = SafeSimpleDateFormat.getDateFormat(format);
        assertNotNull(second);
        assertNotSame(first, second, "After clearing the ThreadLocal cache a new instance must be created");
        // The pattern must still be correct
        String pattern = (String) getPrivateField(second, "pattern");
        assertEquals(format, pattern);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void threadIsolationWithDifferentPattern() throws ExecutionException, InterruptedException {
        String format = "dd/MM/yyyy-" + System.nanoTime();
        SimpleDateFormat main = SafeSimpleDateFormat.getDateFormat(format);
        assertNotNull(main);
        CompletableFuture<SimpleDateFormat> future = CompletableFuture.supplyAsync(() -> SafeSimpleDateFormat.getDateFormat(format));
        SimpleDateFormat other = future.get();
        assertNotNull(other);
        assertNotSame(main, other, "Different threads must have distinct formatter instances");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void malformedPatternThrowsIllegalArgumentException() {
        // unmatched quote
        String badPattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ'\"";
        assertThrows(IllegalArgumentException.class, () -> SafeSimpleDateFormat.getDateFormat(badPattern), "Malformed pattern should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void unsupportedTimezonePatternThrowsIllegalArgumentException() {
        // SimpleDateFormat does not support 5 Z's
        String badPattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ";
        assertThrows(IllegalArgumentException.class, () -> SafeSimpleDateFormat.getDateFormat(badPattern), "Unsupported timezone token should cause IllegalArgumentException");
    }
}
