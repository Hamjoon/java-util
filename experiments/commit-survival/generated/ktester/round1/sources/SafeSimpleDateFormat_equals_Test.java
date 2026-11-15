package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.text.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SafeSimpleDateFormat_equals_Test {

    /**
     * The format string used for all test instances.
     */
    private static final String TEST_PATTERN = "yyyy/MM/dd";

    /**
     * Instance under test, created before each test method.
     */
    private SafeSimpleDateFormat safeSdf;

    /**
     * The format string used for the majority of test instances.
     */
    private static final String VALID_PATTERN = "yyyy-MM-dd";

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh SafeSimpleDateFormat for every test to avoid cross‑test contamination.
        safeSdf = new SafeSimpleDateFormat(TEST_PATTERN);
        safeSdf = new SafeSimpleDateFormat(VALID_PATTERN);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
        safeSdf = null;
        safeSdf = null;
    }

    /* -------------------------------------------------------------
     *  Parameterised test data
     * ------------------------------------------------------------- */
    private static Stream<Arguments> equalityScenarios() {
        return Stream.of(// null → false
        Arguments.of(null, false, "null should return false"), // SimpleDateFormat with same pattern → true
        Arguments.of(new SimpleDateFormat(VALID_PATTERN), true, "SimpleDateFormat with same pattern should be equal"), // SimpleDateFormat with different pattern → false
        Arguments.of(new SimpleDateFormat("dd/MM/yyyy"), false, "SimpleDateFormat with different pattern should not be equal"), // Another SafeSimpleDateFormat with same pattern → false
        Arguments.of(new SafeSimpleDateFormat(VALID_PATTERN), false, "Other SafeSimpleDateFormat should not be equal (different class)"), // Unrelated type (String) → false
        Arguments.of("some string", false, "Unrelated type should return false"), // Same instance (self) → false (delegate sees SafeSimpleDateFormat)
        Arguments.of(safeSdf, false, "Self reference should return false"), // Generic Object → false
        Arguments.of(new Object(), false, "Generic Object should return false"), // The exact delegate SimpleDateFormat instance → true
        Arguments.of(SafeSimpleDateFormat.getDateFormat(VALID_PATTERN), true, "Delegate SimpleDateFormat instance should be equal"));
    }

    @Test
    void equalsReturnsTrueWhenOtherIsSimpleDateFormatWithSamePattern() {
        SimpleDateFormat samePattern = new SimpleDateFormat(TEST_PATTERN);
        assertTrue(safeSdf.equals(samePattern), "SafeSimpleDateFormat should be equal to a SimpleDateFormat with the same pattern");
    }

    @Test
    void equalsReturnsFalseWhenOtherIsSimpleDateFormatWithDifferentPattern() {
        SimpleDateFormat differentPattern = new SimpleDateFormat("dd-MM-yyyy");
        assertFalse(safeSdf.equals(differentPattern), "SafeSimpleDateFormat should not be equal to a SimpleDateFormat with a different pattern");
    }

    @Test
    void equalsReturnsFalseWhenOtherIsNotASimpleDateFormat() {
        String notADateFormat = "some string";
        assertFalse(safeSdf.equals(notADateFormat), "SafeSimpleDateFormat should not be equal to an object of a different type");
    }

    @Test
    void equalsReturnsFalseWhenOtherIsNull() {
        assertFalse(safeSdf.equals(null), "SafeSimpleDateFormat should not be equal to null");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("equalityScenarios")
    void testEqualsVariousObjects(Object other, boolean expected, String description) {
        // Act
        boolean result = safeSdf.equals(other);
        // Assert
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Tests for configuration differences that affect SimpleDateFormat.equals
     * ------------------------------------------------------------- */
    @Test
    void equalsFalseWhenTimeZoneDiffers() {
        SimpleDateFormat other = new SimpleDateFormat(VALID_PATTERN);
        // default of delegate is system zone
        other.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertFalse(safeSdf.equals(other), "Same pattern but different TimeZone should result in false");
    }

    @Test
    void equalsFalseWhenLenientDiffers() {
        SimpleDateFormat other = new SimpleDateFormat(VALID_PATTERN);
        other.setLenient(false);
        assertFalse(safeSdf.equals(other), "Same pattern but different lenient flag should result in false");
    }

    @Test
    void equalsFalseWhenLocaleDiffers() {
        SimpleDateFormat other = new SimpleDateFormat(VALID_PATTERN, Locale.FRENCH);
        assertFalse(safeSdf.equals(other), "Same pattern but different Locale should result in false");
    }

    @Test
    void equalsFalseWhenDateFormatSymbolsDiffer() {
        SimpleDateFormat other = new SimpleDateFormat(VALID_PATTERN);
        other.setDateFormatSymbols(new DateFormatSymbols(Locale.GERMAN));
        assertFalse(safeSdf.equals(other), "Same pattern but different DateFormatSymbols should result in false");
    }

    @Test
    void equalsFalseWhenNumberFormatDiffers() {
        SimpleDateFormat other = new SimpleDateFormat(VALID_PATTERN);
        other.setNumberFormat(NumberFormat.getInstance(Locale.FRENCH));
        assertFalse(safeSdf.equals(other), "Same pattern but different NumberFormat should result in false");
    }

    @Test
    void equalsFalseWhenCalendarDiffers() {
        SimpleDateFormat other = new SimpleDateFormat(VALID_PATTERN);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"), Locale.JAPANESE);
        other.setCalendar(cal);
        assertFalse(safeSdf.equals(other), "Same pattern but different Calendar (zone/locale) should result in false");
    }

    /* -------------------------------------------------------------
     *  Tests that trigger IllegalArgumentException during delegate creation
     * ------------------------------------------------------------- */
    @Test
    void equalsThrowsWhenSafeSimpleDateFormatHasInvalidPattern() {
        // Construct a SafeSimpleDateFormat with an illegal pattern.
        SafeSimpleDateFormat broken = new SafeSimpleDateFormat("y-M-!!");
        // The delegate is created lazily inside equals → expect IAE.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> broken.equals(new SimpleDateFormat("yyyy-MM-dd")), "Invalid pattern should cause IllegalArgumentException from getDateFormat");
        assertNotNull(ex.getMessage());
    }

    @Test
    void equalsThrowsWhenSafeSimpleDateFormatHasEmptyPattern() {
        SafeSimpleDateFormat broken = new SafeSimpleDateFormat("");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> broken.equals(new SimpleDateFormat("yyyy-MM-dd")), "Empty pattern should cause IllegalArgumentException from getDateFormat");
        assertNotNull(ex.getMessage());
    }
}
