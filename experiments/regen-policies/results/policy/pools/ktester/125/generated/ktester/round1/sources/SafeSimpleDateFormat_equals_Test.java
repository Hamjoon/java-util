package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.text.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_equals_Test {

    private static final String FORMAT_A = "yyyy/MM/dd";

    private static final String FORMAT_B = "dd-MM-yyyy";

    /**
     * Provides a stream of arguments for the bulk of the equality tests.
     * Each argument consists of:
     *   - a {@link SafeSimpleDateFormat} instance (the "this" object)
     *   - an {@code Object} to compare with
     *   - the expected boolean result
     *   - a textual description (used only for readability)
     */
    private static Stream<Arguments> equalityTestProvider() {
        String pattern = "yyyy-MM-dd";
        // Base SafeSimpleDateFormat used for many cases
        SafeSimpleDateFormat baseSafe = new SafeSimpleDateFormat(pattern);
        // Plain SimpleDateFormat with default settings (same as baseSafe)
        SimpleDateFormat plainSame = SafeSimpleDateFormat.getDateFormat(pattern);
        // Plain SimpleDateFormat with a different time‑zone
        SimpleDateFormat plainDiffTZ = SafeSimpleDateFormat.getDateFormat(pattern);
        plainDiffTZ.setTimeZone(TimeZone.getTimeZone("UTC"));
        // Plain SimpleDateFormat with different leniency
        SimpleDateFormat plainDiffLenient = SafeSimpleDateFormat.getDateFormat(pattern);
        plainDiffLenient.setLenient(false);
        // Plain SimpleDateFormat with different NumberFormat
        SimpleDateFormat plainDiffNumber = SafeSimpleDateFormat.getDateFormat(pattern);
        plainDiffNumber.setNumberFormat(NumberFormat.getInstance());
        // Plain SimpleDateFormat with different Calendar (UTC)
        SimpleDateFormat plainDiffCalendar = SafeSimpleDateFormat.getDateFormat(pattern);
        plainDiffCalendar.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        // Plain SimpleDateFormat with different locale symbols (German)
        SimpleDateFormat plainDiffSymbols = SafeSimpleDateFormat.getDateFormat(pattern);
        plainDiffSymbols.setDateFormatSymbols(new DateFormatSymbols(Locale.GERMAN));
        // Plain SimpleDateFormat with same symbols (default)
        SimpleDateFormat plainSameSymbols = SafeSimpleDateFormat.getDateFormat(pattern);
        plainSameSymbols.setDateFormatSymbols(new DateFormatSymbols());
        // Plain SimpleDateFormat with different 2‑digit year start
        SimpleDateFormat plainDiffYearStart = SafeSimpleDateFormat.getDateFormat(pattern);
        plainDiffYearStart.set2DigitYearStart(new Date(0L));
        // SafeSimpleDateFormat with different time‑zone
        SafeSimpleDateFormat safeDiffTZ = new SafeSimpleDateFormat(pattern);
        safeDiffTZ.setTimeZone(TimeZone.getTimeZone("UTC"));
        // SafeSimpleDateFormat with different leniency
        SafeSimpleDateFormat safeDiffLenient = new SafeSimpleDateFormat(pattern);
        safeDiffLenient.setLenient(false);
        // SafeSimpleDateFormat with different calendar (UTC)
        SafeSimpleDateFormat safeDiffCalendar = new SafeSimpleDateFormat(pattern);
        safeDiffCalendar.setCalendar(Calendar.getInstance(TimeZone.getTimeZone("UTC")));
        // SafeSimpleDateFormat with different symbols (German)
        SafeSimpleDateFormat safeDiffSymbols = new SafeSimpleDateFormat(pattern);
        safeDiffSymbols.setDateFormatSymbols(new DateFormatSymbols(Locale.GERMAN));
        // SafeSimpleDateFormat with same default symbols
        SafeSimpleDateFormat safeSameSymbols = new SafeSimpleDateFormat(pattern);
        safeSameSymbols.setDateFormatSymbols(new DateFormatSymbols());
        // SafeSimpleDateFormat with different 2‑digit year start
        SafeSimpleDateFormat safeDiffYearStart = new SafeSimpleDateFormat(pattern);
        safeDiffYearStart.set2DigitYearStart(new Date(0L));
        return Stream.of(// --- branch coverage cases -------------------------------------------------
        Arguments.of(baseSafe, new SafeSimpleDateFormat(pattern), true, "Both SafeSimpleDateFormat with identical pattern"), Arguments.of(baseSafe, new SafeSimpleDateFormat("dd/MM/yyyy"), false, "SafeSimpleDateFormat with different pattern"), Arguments.of(baseSafe, plainSame, true, "SimpleDateFormat with same pattern and default settings"), Arguments.of(baseSafe, new SimpleDateFormat("MM-dd-yyyy"), false, "SimpleDateFormat with different pattern"), Arguments.of(baseSafe, null, false, "Comparing with null"), Arguments.of(baseSafe, "some random string", false, "Comparing with unrelated String"), Arguments.of(baseSafe, new Date(), false, "Comparing with a Date instance"), Arguments.of(baseSafe, plainDiffTZ, false, "SimpleDateFormat same pattern but different time‑zone"), Arguments.of(baseSafe, plainDiffLenient, false, "SimpleDateFormat same pattern but different leniency"), // --- additional coverage cases --------------------------------------------
        Arguments.of(baseSafe, safeDiffTZ, false, "SafeSimpleDateFormat same pattern but different time‑zone"), Arguments.of(baseSafe, safeDiffLenient, false, "SafeSimpleDateFormat same pattern but different leniency"), Arguments.of(baseSafe, new SafeSimpleDateFormat("dd/MM/yyyy"), false, "SafeSimpleDateFormat with different pattern"), Arguments.of(baseSafe, plainDiffNumber, false, "SimpleDateFormat same pattern but different NumberFormat"), Arguments.of(baseSafe, new Object(), false, "Comparing with generic Object"), Arguments.of(baseSafe, safeDiffCalendar, false, "SafeSimpleDateFormat same pattern but different calendar (UTC)"), Arguments.of(baseSafe, safeDiffSymbols, false, "SafeSimpleDateFormat same pattern but different locale symbols (German)"), Arguments.of(baseSafe, safeSameSymbols, true, "Both SafeSimpleDateFormat have identical default symbols and pattern"), Arguments.of(baseSafe, safeDiffYearStart, false, "SafeSimpleDateFormat same pattern but different 2‑digit year start"), Arguments.of(baseSafe, plainSame, true, "SafeSimpleDateFormat vs plain SimpleDateFormat with identical default settings"));
    }

    @Test
    void equalsSameFormatReturnsTrue() {
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat(FORMAT_A);
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat(FORMAT_A);
        assertTrue(sdf1.equals(sdf2), "SafeSimpleDateFormat instances with the same pattern should be equal");
        assertTrue(sdf2.equals(sdf1), "Equality should be symmetric");
    }

    @Test
    void equalsDifferentFormatReturnsFalse() {
        SafeSimpleDateFormat sdf1 = new SafeSimpleDateFormat(FORMAT_A);
        SafeSimpleDateFormat sdf2 = new SafeSimpleDateFormat(FORMAT_B);
        assertFalse(sdf1.equals(sdf2), "SafeSimpleDateFormat instances with different patterns should not be equal");
        assertFalse(sdf2.equals(sdf1), "Equality should be symmetric even when false");
    }

    @Test
    void equalsWithSimpleDateFormatSamePatternTrue() {
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(FORMAT_A);
        SimpleDateFormat plainSdf = SafeSimpleDateFormat.getDateFormat(FORMAT_A);
        assertTrue(safeSdf.equals(plainSdf), "SafeSimpleDateFormat should be equal to a SimpleDateFormat with the same pattern");
    }

    @Test
    void equalsWithSimpleDateFormatDifferentPatternFalse() {
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(FORMAT_A);
        SimpleDateFormat plainSdf = SafeSimpleDateFormat.getDateFormat(FORMAT_B);
        assertFalse(safeSdf.equals(plainSdf), "SafeSimpleDateFormat should not be equal to a SimpleDateFormat with a different pattern");
    }

    @Test
    void equalsWithUnrelatedObjectReturnsFalse() {
        SafeSimpleDateFormat safeSdf = new SafeSimpleDateFormat(FORMAT_A);
        Object unrelated = new Object();
        assertFalse(safeSdf.equals(unrelated), "SafeSimpleDateFormat should not be equal to an unrelated object");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("equalityTestProvider")
    void equalsVariousScenarios(SafeSimpleDateFormat underTest, Object other, boolean expected, String description) {
        // The assertion message includes the description for easier debugging.
        if (expected) {
            assertTrue(underTest.equals(other), description);
        } else {
            assertFalse(underTest.equals(other), description);
        }
    }

    @Test
    void equalsThrowsNullPointerExceptionWhenPatternIsNull() {
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat(null);
        // The NPE is expected to be thrown from SimpleDateFormat constructor inside getDateFormat(null)
        assertThrows(NullPointerException.class, () -> safe.equals(new Object()), "Calling equals on a SafeSimpleDateFormat created with a null pattern should throw NullPointerException");
    }

    @Test
    void equalsThrowsIllegalArgumentExceptionWhenPatternIsInvalid() {
        // The pattern "yyyy-@@@" is not parsable by SimpleDateFormat and should cause IllegalArgumentException
        SafeSimpleDateFormat safe = new SafeSimpleDateFormat("yyyy-@@@");
        assertThrows(IllegalArgumentException.class, () -> safe.equals(new Object()), "Calling equals on a SafeSimpleDateFormat created with an invalid pattern should throw IllegalArgumentException");
    }
}
