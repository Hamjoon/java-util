package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.TimeZone;
import java.util.stream.Stream;

class SafeSimpleDateFormat_hashCode_Test {

    private SafeSimpleDateFormat safeDateFormat;

    private static final String FORMAT = "yyyy/MM/dd";

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test
        safeDateFormat = new SafeSimpleDateFormat(FORMAT);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
        safeDateFormat = null;
    }

    private static Stream<Arguments> differentFormatPairs() {
        return Stream.of(Arguments.of("yyyy-MM-dd", "dd/MM/yyyy"), Arguments.of("yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss"), Arguments.of("yyyyMMdd", "yyyy/MM/dd"));
    }

    @Test
    void hashCode_ShouldDelegateToUnderlyingSimpleDateFormat() {
        // given
        int expected = SafeSimpleDateFormat.getDateFormat(FORMAT).hashCode();
        // when
        int actual = safeDateFormat.hashCode();
        // then
        assertEquals(expected, actual, "hashCode() must return the hash code of the underlying SimpleDateFormat");
    }

    @Test
    void hashCode_DifferentFormatsYieldDifferentHashCodes() {
        // given
        SafeSimpleDateFormat other = new SafeSimpleDateFormat("dd-MM-yyyy");
        // when
        int hash1 = safeDateFormat.hashCode();
        int hash2 = other.hashCode();
        // then
        assertNotEquals(hash1, hash2, "Different format strings should result in different hash codes");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] format=''{0}''")
    @ValueSource(strings = { "yyyy-MM-dd", "dd/MM/yyyy", "yyyyMMdd" })
    void hashCode_IdenticalFormats_ProduceSameHash(String pattern) {
        // Two different instances with the same pattern must have equal hash codes
        SafeSimpleDateFormat a = new SafeSimpleDateFormat(pattern);
        SafeSimpleDateFormat b = new SafeSimpleDateFormat(pattern);
        assertEquals(a.hashCode(), b.hashCode(), "Instances created with identical format strings must produce identical hashCode values");
    }

    @Test
    void hashCode_SameInstance_StableAcrossMultipleCalls() {
        // Repeated calls on the same object must return the same value
        int first = safeDateFormat.hashCode();
        int second = safeDateFormat.hashCode();
        int third = safeDateFormat.hashCode();
        assertEquals(first, second, "hashCode must be stable across multiple invocations");
        assertEquals(second, third, "hashCode must be stable across multiple invocations");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] format1=''{0}'', format2=''{1}''")
    @MethodSource("differentFormatPairs")
    void hashCode_DifferentFormats_YieldDifferentHash(String fmt1, String fmt2) {
        SafeSimpleDateFormat a = new SafeSimpleDateFormat(fmt1);
        SafeSimpleDateFormat b = new SafeSimpleDateFormat(fmt2);
        assertNotEquals(a.hashCode(), b.hashCode(), () -> "Different format strings '" + fmt1 + "' and '" + fmt2 + "' should result in different hash codes");
    }

    // -----------------------------------------------------------------
    @Test
    void hashCode_EmptyFormatString_DelegatesCorrectly() {
        SafeSimpleDateFormat emptyFmt = new SafeSimpleDateFormat("");
        int expected = SafeSimpleDateFormat.getDateFormat("").hashCode();
        assertEquals(expected, emptyFmt.hashCode(), "An empty pattern string should delegate to SimpleDateFormat and produce matching hashCode");
    }

    // -----------------------------------------------------------------
    @Test
    void hashCode_NullFormatString_ThrowsNullPointerException() {
        SafeSimpleDateFormat nullFmt = new SafeSimpleDateFormat(null);
        assertThrows(NullPointerException.class, nullFmt::hashCode, "Calling hashCode on an instance constructed with null format should throw NPE");
    }

    // -----------------------------------------------------------------
    @Test
    void hashCode_AfterSetTimeZone_RemainsUnchanged() {
        SafeSimpleDateFormat fmt = new SafeSimpleDateFormat("yyyy-MM-dd");
        int before = fmt.hashCode();
        fmt.setTimeZone(TimeZone.getTimeZone("GMT+2"));
        int after = fmt.hashCode();
        assertEquals(before, after, "Changing the TimeZone must not affect the hashCode (hash based on pattern only)");
    }

    @Test
    void hashCode_AfterSetLenient_RemainsUnchanged() {
        SafeSimpleDateFormat fmt = new SafeSimpleDateFormat("yyyy-MM-dd");
        int before = fmt.hashCode();
        fmt.setLenient(false);
        int after = fmt.hashCode();
        assertEquals(before, after, "Toggling leniency must not affect the hashCode (hash based on pattern only)");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] format=''{0}''")
    @ValueSource(strings = { "dd-MMM-yyyy", "yyyy.MM.dd G 'at' HH:mm:ss z", "EEE, MMM d, ''yy" })
    void hashCode_EqualsStaticHelperHash(String pattern) {
        SafeSimpleDateFormat fmt = new SafeSimpleDateFormat(pattern);
        int expected = SafeSimpleDateFormat.getDateFormat(pattern).hashCode();
        assertEquals(expected, fmt.hashCode(), "hashCode must equal the hashCode of SimpleDateFormat obtained via getDateFormat()");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] invalidPattern=''{0}''")
    @ValueSource(strings = { "yyyy-MM-dd'", "yyyy-??-dd", "yyyy-MM-dd[HH]" })
    void hashCode_InvalidPattern_ThrowsIllegalArgumentException(String badPattern) {
        SafeSimpleDateFormat fmt = new SafeSimpleDateFormat(badPattern);
        assertThrows(IllegalArgumentException.class, fmt::hashCode, "An invalid pattern should cause SimpleDateFormat construction to throw IllegalArgumentException");
    }
}
