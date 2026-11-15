package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.text.SimpleDateFormat;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.text.NumberFormat;
import java.util.TimeZone;
import java.util.stream.Stream;

class SafeSimpleDateFormat_hashCode_Test {

    /**
     * Provides pairs of identical format strings for the “same format” group.
     */
    private static Stream<String> sameFormatProvider() {
        return Stream.of("yyyy-MM-dd", "dd/MM/yyyy HH:mm:ss");
    }

    /**
     * Provides a single format string for the deterministic‑multiple‑calls test.
     */
    private static Stream<String> deterministicProvider() {
        return Stream.of("yyyy-MM-dd");
    }

    /**
     * Provides distinct format strings for the “different patterns” group.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> differentFormatProvider() {
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of("yyyy-MM-dd", "dd/MM/yyyy"), org.junit.jupiter.params.provider.Arguments.of("yyyy-MM-dd", "yyyy-MM-dd "));
    }

    /**
     * Provides a format string for the empty‑pattern test.
     */
    private static Stream<String> emptyFormatProvider() {
        return Stream.of("");
    }

    /**
     * Provides a null format for the NPE test.
     */
    private static Stream<String> nullFormatProvider() {
        return Stream.of((String) null);
    }

    /**
     * Verifies that {@code SafeSimpleDateFormat#hashCode()} returns the same
     * value as the underlying {@link SimpleDateFormat} for the same format
     * string.
     */
    @Test
    void hashCodeMatchesUnderlyingSimpleDateFormat() {
        // given
        String pattern = "yyyy/MM/dd HH:mm:ss";
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(pattern);
        SimpleDateFormat simpleSdf = SafeSimpleDateFormat.getDateFormat(pattern);
        // when
        int safeHash = safeSdf.hashCode();
        int simpleHash = simpleSdf.hashCode();
        // then
        assertEquals(simpleHash, safeHash, "SafeSimpleDateFormat.hashCode() should delegate to the underlying SimpleDateFormat");
    }

    /**
     * Two {@code SafeSimpleDateFormat} instances created with the same pattern
     * must produce identical hash codes because they delegate to the same
     * underlying {@link SimpleDateFormat} instance (per thread).
     */
    @Test
    void hashCodeIsConsistentForSamePattern() {
        // given
        String pattern = "dd-MMM-yyyy";
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat(pattern);
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat(pattern);
        // when
        int hash1 = sdf1.hashCode();
        int hash2 = sdf2.hashCode();
        // then
        assertEquals(hash1, hash2, "Hash codes for SafeSimpleDateFormat instances with the same pattern should be equal");
    }

    /**
     * Different format strings should result in different hash codes because the
     * underlying {@link SimpleDateFormat} instances differ.
     */
    @Test
    void hashCodeDiffersForDifferentPatterns() {
        // given
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat("yyyy-MM-dd");
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat("MM/dd/yyyy");
        // when
        int hash1 = sdf1.hashCode();
        int hash2 = sdf2.hashCode();
        // then
        assertNotEquals(hash1, hash2, "Hash codes for different patterns should not be equal");
    }

    @ParameterizedTest(name = "hashCode consistency with same format \"{0}\"")
    @MethodSource("sameFormatProvider")
    void hashCodeConsistencyWithSameFormat(String pattern) {
        // given two separate instances with identical pattern
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat(pattern);
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat(pattern);
        // when
        int hash1 = sdf1.hashCode();
        int hash2 = sdf2.hashCode();
        // then
        assertEquals(hash1, hash2, "Two SafeSimpleDateFormat instances constructed with the same format string must produce identical hashCode values.");
    }

    @ParameterizedTest(name = "hashCode deterministic across multiple invocations for \"{0}\"")
    @MethodSource("deterministicProvider")
    void hashCodeDeterministicAcrossMultipleInvocations(String pattern) {
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        int first = sdf.hashCode();
        int second = sdf.hashCode();
        int third = sdf.hashCode();
        assertAll(() -> assertEquals(first, second, "Second invocation should match first"), () -> assertEquals(first, third, "Third invocation should match first"));
    }

    @ParameterizedTest(name = "hashCode differs for distinct patterns \"{0}\" vs \"{1}\"")
    @MethodSource("differentFormatProvider")
    void hashCodeDiffersForDistinctPatterns(String patternA, String patternB) {
        SafeSimpleDateFormat sdfA = new SafeSimpleDateFormat(patternA);
        SafeSimpleDateFormat sdfB = new SafeSimpleDateFormat(patternB);
        int hashA = sdfA.hashCode();
        int hashB = sdfB.hashCode();
        assertNotEquals(hashA, hashB, String.format("Hash codes for patterns \"%s\" and \"%s\" should differ.", patternA, patternB));
    }

    @Test
    void hashCodeUnchangedAfterModifyingMutableSettings() {
        // given
        String pattern = "yyyy-MM-dd HH:mm:ss";
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        int originalHash = sdf.hashCode();
        // mutate various mutable properties
        sdf.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        sdf.setLenient(!sdf.isLenient());
        sdf.setNumberFormat(NumberFormat.getCurrencyInstance());
        // when
        int afterMutationHash = sdf.hashCode();
        // then
        assertEquals(originalHash, afterMutationHash, "hashCode should remain unchanged after mutating time zone, leniency, or number format.");
    }

    @ParameterizedTest(name = "hashCode with empty format string")
    @MethodSource("emptyFormatProvider")
    void hashCodeWithEmptyFormatString(String pattern) {
        // Empty pattern is legal for SimpleDateFormat; ensure no exception and deterministic result
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        int hash = sdf.hashCode();
        // Compare with a direct SimpleDateFormat created with the same empty pattern
        SimpleDateFormat simple = SafeSimpleDateFormat.getDateFormat(pattern);
        assertEquals(simple.hashCode(), hash, "hashCode should match the underlying SimpleDateFormat for an empty pattern.");
    }

    @ParameterizedTest(name = "hashCode throws NullPointerException for null format")
    @MethodSource("nullFormatProvider")
    void hashCodeThrowsNullPointerExceptionForNullFormat(String pattern) {
        // The constructor stores the pattern; passing null should cause NPE when hashCode() accesses it.
        SafeSimpleDateFormat sdf = new SafeSimpleDateFormat(pattern);
        assertThrows(NullPointerException.class, sdf::hashCode, "Calling hashCode() on a SafeSimpleDateFormat constructed with null should throw NullPointerException.");
    }
}
