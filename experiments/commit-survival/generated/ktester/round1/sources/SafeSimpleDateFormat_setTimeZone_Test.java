package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

class SafeSimpleDateFormat_setTimeZone_Test {

    private static final String PATTERN = "yyyy-MM-dd HH:mm:ss";

    private SafeSimpleDateFormat safeSdf;

    // 1970‑01‑01 00:00:00 UTC
    private Date epochDate;

    private static final String PATTERN_WITH_ZONE = "yyyy-MM-dd HH:mm:ss Z";

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test to avoid cross‑test interference.
        // The default pattern does **not** contain a zone offset; tests that need it
        // will create their own instance.
        safeSdf = new SafeSimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // the epoch instant
        epochDate = new Date(0L);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no lingering state is kept.
        safeSdf = null;
        epochDate = null;
    }

    /**
     * Parameter provider for normal time‑zone handling tests.
     * Returns a pair of {@link TimeZone} and the expected offset string
     * (as produced by the pattern "... Z").
     */
    private static Stream<Arguments> timeZoneProvider() {
        return Stream.of(Arguments.of(TimeZone.getTimeZone("UTC"), "+0000"), Arguments.of(TimeZone.getTimeZone("GMT+02:00"), "+0200"));
    }

    @Test
    void setTimeZone_ShouldDelegateToUnderlyingSimpleDateFormat() {
        // GIVEN – a specific time zone different from the default
        TimeZone gmt = TimeZone.getTimeZone("GMT");
        // WHEN – we set the time zone on the SafeSimpleDateFormat instance
        safeSdf.setTimeZone(gmt);
        // THEN – formatting a known instant must use the supplied time zone
        String actual = safeSdf.format(epochDate);
        SimpleDateFormat reference = new SimpleDateFormat(PATTERN);
        reference.setTimeZone(gmt);
        String expected = reference.format(epochDate);
        assertEquals(expected, actual, "SafeSimpleDateFormat should format dates using the time zone supplied via setTimeZone");
    }

    @ParameterizedTest(name = "setTimeZone should format epoch with offset {1}")
    @MethodSource("timeZoneProvider")
    void setTimeZone_ShouldApplyTimeZoneAndFormatWithOffset(TimeZone tz, String expectedOffset) {
        // GIVEN a SafeSimpleDateFormat that includes a zone offset in its pattern
        SafeSimpleDateFormat sdfWithZone = new SafeSimpleDateFormat(PATTERN_WITH_ZONE);
        // WHEN we set the supplied time‑zone and format the epoch instant
        sdfWithZone.setTimeZone(tz);
        String actual = sdfWithZone.format(epochDate);
        // THEN the formatted string must end with the expected offset
        assertTrue(actual.endsWith(expectedOffset), () -> "Expected formatted date to end with '" + expectedOffset + "' but got: '" + actual + "'");
    }

    @Test
    void setTimeZone_NullTimeZone_ThrowsNullPointerException() {
        // GIVEN a non‑null SafeSimpleDateFormat instance (already created in @BeforeEach)
        // WHEN we call setTimeZone with null
        // THEN a NullPointerException is expected
        assertThrows(NullPointerException.class, () -> safeSdf.setTimeZone(null), "Calling setTimeZone(null) should throw NullPointerException");
    }

    @Test
    void setTimeZone_NullFormat_ThrowsNullPointerException() {
        // GIVEN a SafeSimpleDateFormat created with a null format string
        SafeSimpleDateFormat sdfNullFormat = new SafeSimpleDateFormat(null);
        // WHEN we attempt to set a time‑zone
        // THEN a NullPointerException should propagate from getDateFormat(null)
        assertThrows(NullPointerException.class, () -> sdfNullFormat.setTimeZone(TimeZone.getTimeZone("UTC")), "setTimeZone on an instance with null format should throw NullPointerException");
    }

    @Test
    void setTimeZone_InvalidPattern_ThrowsIllegalArgumentException() {
        // GIVEN a syntactically invalid pattern (unclosed quote)
        // missing closing quote
        String invalidPattern = "yyyy-MM-dd'";
        SafeSimpleDateFormat sdfInvalid = new SafeSimpleDateFormat(invalidPattern);
        // WHEN we call setTimeZone, the internal getDateFormat(...) will try to
        // create a SimpleDateFormat and throw IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> sdfInvalid.setTimeZone(TimeZone.getTimeZone("UTC")), "setTimeZone with an invalid pattern should throw IllegalArgumentException");
    }
}
