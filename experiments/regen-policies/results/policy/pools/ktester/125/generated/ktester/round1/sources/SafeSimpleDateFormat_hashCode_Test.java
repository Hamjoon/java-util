package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

class SafeSimpleDateFormat_hashCode_Test {

    private static final String FORMAT_A = "yyyy-MM-dd";

    private static final String FORMAT_B = "dd/MM/yyyy";

    private SafeSimpleDateFormat safeSdfA;

    private SafeSimpleDateFormat safeSdfB;

    @BeforeEach
    void setupBeforeEach() {
        // Create fresh instances before each test to avoid cross‑test contamination
        safeSdfA = new SafeSimpleDateFormat(FORMAT_A);
        safeSdfB = new SafeSimpleDateFormat(FORMAT_B);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly nullify references – helps the ThreadLocal cache to be cleared
        safeSdfA = null;
        safeSdfB = null;
    }

    /**
     * Group: hashCode equality for identical format strings (including empty)
     */
    static Stream<Arguments> identicalFormatProvider() {
        return Stream.of(Arguments.of("dd/MM/yyyy"), // empty pattern
        Arguments.of(""));
    }

    /**
     * Group: hashCode variance for different format strings
     */
    static Stream<Arguments> differentFormatProvider() {
        return Stream.of(Arguments.of("yyyy-MM-dd", "MM/dd/yyyy"), Arguments.of("yyyyMMddHHmmss", "yyyy-MM-dd"));
    }

    /**
     * Group: hashCode with malformed pattern (exception handling)
     */
    static Stream<Arguments> malformedPatternProvider() {
        return Stream.of(// trailing unmatched quote
        Arguments.of("yyyy-MM-dd'T'HH:mm:ss.SSSZ'"), // unterminated quote
        Arguments.of("yyyy-MM-dd'"));
    }

    @Test
    void hashCodeMatchesUnderlyingSimpleDateFormat() {
        // GIVEN a format string and a SafeSimpleDateFormat instance
        // WHEN hashCode() is invoked
        int actualHash = safeSdfA.hashCode();
        // THEN it must be equal to the hashCode of the underlying SimpleDateFormat
        int expectedHash = SafeSimpleDateFormat.getDateFormat(FORMAT_A).hashCode();
        assertEquals(expectedHash, actualHash, "hashCode() should delegate to the underlying SimpleDateFormat's hashCode");
    }

    @Test
    void hashCodeIsConsistentAcrossMultipleInstancesWithSameFormat() {
        // GIVEN two SafeSimpleDateFormat instances with the same format
        SafeSimpleDateFormat anotherInstance = new SafeSimpleDateFormat(FORMAT_A);
        // WHEN hashCode() is called on both
        int hash1 = safeSdfA.hashCode();
        int hash2 = anotherInstance.hashCode();
        // THEN the hash codes must be identical
        assertEquals(hash1, hash2, "Different SafeSimpleDateFormat instances with the same format should produce identical hash codes");
    }

    @Test
    void hashCodeDiffersForDifferentFormats() {
        // GIVEN two SafeSimpleDateFormat instances with different formats
        // WHEN hashCode() is called
        int hashA = safeSdfA.hashCode();
        int hashB = safeSdfB.hashCode();
        // THEN the hash codes should generally differ (not guaranteed but highly likely)
        assertNotEquals(hashA, hashB, "SafeSimpleDateFormat instances with different formats should produce different hash codes");
    }

    /**
     * Group: hashCode consistency (same instance, repeated calls)
     */
    @Test
    void hashCodeConsistentOnRepeatedCalls() {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("yyyy-MM-dd");
        int first = sdf.hashCode();
        int second = sdf.hashCode();
        int expected = SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd").hashCode();
        assertEquals(expected, first, "First hash must match SimpleDateFormat hash");
        assertEquals(first, second, "Repeated hashCode calls must return the same value");
    }

    @ParameterizedTest
    @MethodSource("identicalFormatProvider")
    void hashCodeEqualityForIdenticalFormats(String pattern) {
        SafeSimpleDateFormat s1 = new SafeSimpleDateFormat(pattern);
        SafeSimpleDateFormat s2 = new SafeSimpleDateFormat(pattern);
        assertEquals(s1.hashCode(), s2.hashCode(), "Two instances with pattern [" + pattern + "] must have equal hash codes");
    }

    @ParameterizedTest
    @MethodSource("differentFormatProvider")
    void hashCodeVarianceForDifferentFormats(String fmt1, String fmt2) {
        SafeSimpleDateFormat s1 = new SafeSimpleDateFormat(fmt1);
        SafeSimpleDateFormat s2 = new SafeSimpleDateFormat(fmt2);
        assertNotEquals(s1.hashCode(), s2.hashCode(), "Patterns [" + fmt1 + "] and [" + fmt2 + "] should usually yield different hash codes");
    }

    /**
     * Group: hashCode with Unicode pattern
     */
    @Test
    void hashCodeWithUnicodePattern() {
        String pattern = "yyyy年MM月dd日";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        int expected = SafeSimpleDateFormat.getDateFormat(pattern).hashCode();
        assertEquals(expected, sdf.hashCode(), "Unicode pattern hash must match underlying SimpleDateFormat hash");
        // repeat to ensure stability
        assertEquals(expected, sdf.hashCode(), "Repeated calls must stay stable for Unicode pattern");
    }

    /**
     * Group: hashCode with leading/trailing whitespace
     */
    @Test
    void hashCodeWithWhitespaceIsSignificant() {
        String patternWithSpaces = "  yyyy-MM-dd  ";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(patternWithSpaces);
        int expected = SafeSimpleDateFormat.getDateFormat(patternWithSpaces).hashCode();
        assertEquals(expected, sdf.hashCode(), "Whitespace should be part of the pattern and affect hashCode");
        // Ensure it differs from trimmed version
        assertNotEquals(expected, SafeSimpleDateFormat.getDateFormat("yyyy-MM-dd").hashCode(), "Pattern with spaces must have a different hash than trimmed pattern");
    }

    /**
     * Group: very long pattern (stress test)
     */
    @Test
    void hashCodeWithVeryLongPattern() {
        String longPattern = "yyyy-MM-dd HH:mm:ss.SSS ZZZZ XXX yyy MMM dd HH:mm:ss.SSS ZZZZ";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(longPattern);
        int expected = SafeSimpleDateFormat.getDateFormat(longPattern).hashCode();
        assertEquals(expected, sdf.hashCode(), "Long pattern hash must match underlying SimpleDateFormat hash and not overflow");
    }

    /**
     * Group: hashCode invariance after lenient flag change
     */
    @Test
    void hashCodeInvariantAfterLenientChange() {
        String pattern = "dd-MMM-yyyy";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        int before = sdf.hashCode();
        sdf.setLenient(false);
        int after = sdf.hashCode();
        assertEquals(before, after, "Changing lenient flag must not affect hashCode");
    }

    /**
     * Group: hashCode invariance after time zone change
     */
    @Test
    void hashCodeInvariantAfterTimeZoneChange() {
        String pattern = "yyyy/MM/dd HH:mm:ss";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        int before = sdf.hashCode();
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdf.setTimeZone(TimeZone.getTimeZone("PST"));
        int after = sdf.hashCode();
        assertEquals(before, after, "Changing time zone must not affect hashCode");
    }

    /**
     * Group: hashCode after internal cache reuse
     */
    @Test
    void hashCodeAfterCacheReuse() {
        String patternA = "EEE, MMM d, ''yy";
        String patternB = "yyyy-MM-dd";
        // First instance (A)
        SafeSimpleDateFormat sdfA = new SafeSimpleDateFormat(patternA);
        int hashAFirst = sdfA.hashCode();
        // Second instance (B) – forces cache entry for a different pattern
        SafeSimpleDateFormat sdfB = new SafeSimpleDateFormat(patternB);
        // populate cache for B
        sdfB.hashCode();
        // Re‑invoke hashCode on A, it must still correspond to pattern A
        int hashASecond = sdfA.hashCode();
        int expectedA = SafeSimpleDateFormat.getDateFormat(patternA).hashCode();
        assertEquals(expectedA, hashASecond, "Hash after cache reuse must still match pattern A");
        assertEquals(hashAFirst, hashASecond, "Hash for A must be stable even after B was cached");
    }

    /**
     * Group: hashCode with null format (exception handling)
     */
    @Test
    void hashCodeWithNullFormatThrowsNPE() throws Exception {
        // Construct with a non‑null pattern, then force _format to null via reflection
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat("any");
        Field f = SafeSimpleDateFormat.class.getDeclaredField("_format");
        f.setAccessible(true);
        f.set(sdf, null);
        assertThrows(NullPointerException.class, sdf::hashCode, "Calling hashCode when internal format is null should throw NPE");
    }

    @ParameterizedTest
    @MethodSource("malformedPatternProvider")
    void hashCodeWithMalformedPatternThrowsIAE(String malformedPattern) {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(malformedPattern);
        assertThrows(IllegalArgumentException.class, sdf::hashCode, "Malformed pattern [" + malformedPattern + "] should cause IllegalArgumentException");
    }

    /**
     * Group: hashCode thread‑safety (concurrent access)
     */
    @Test
    void hashCodeThreadSafety() throws Exception {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        int expected = sdf.hashCode();
        Callable<Integer> task = sdf::hashCode;
        int threadCount = 20;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                futures.add(exec.submit(task));
            }
            for (Future<Integer> f : futures) {
                assertEquals(expected, f.get(), "All concurrent hashCode calls must return the same value");
            }
        } finally {
            exec.shutdownNow();
        }
    }
}
