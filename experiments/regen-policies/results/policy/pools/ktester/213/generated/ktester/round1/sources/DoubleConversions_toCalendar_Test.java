package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toCalendar_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (handled by MockitoExtension, but kept for clarity)
        mocks = MockitoAnnotations.openMocks(this);
        // Configure the mock hierarchy so that DoubleConversions can obtain a deterministic TimeZone.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // Use UTC to make the test independent of the machine's default zone.
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito session and release any resources.
        mocks.close();
    }

    private static Stream<Arguments> conversionTestProvider() {
        return Stream.of(// whole‑second Unix timestamp (2021‑01‑01 00:00:00 UTC)
        Arguments.of(1609459200.0, 1609459200000L, "UTC"), // timestamp with fractional seconds (truncates)
        Arguments.of(1609459200.123, 1609459200123L, "UTC"), // zero seconds
        Arguments.of(0.0, 0L, "UTC"), // negative timestamp
        Arguments.of(-1.5, -1500L, "UTC"), // overflow – larger than Long.MAX_VALUE/1000, should saturate to Long.MAX_VALUE
        Arguments.of(9223372036854776.0, Long.MAX_VALUE, "UTC"), // custom time‑zone America/New_York
        Arguments.of(1609459200.0, 1609459200000L, "America/New_York"));
    }

    private static Stream<Arguments> specialDoubleProvider() {
        return Stream.of(// NaN -> cast to long yields 0
        Arguments.of(Double.NaN, 0L), // Positive infinity -> Long.MAX_VALUE
        Arguments.of(Double.POSITIVE_INFINITY, Long.MAX_VALUE), // Negative infinity -> Long.MIN_VALUE
        Arguments.of(Double.NEGATIVE_INFINITY, Long.MIN_VALUE));
    }

    private static Stream<Arguments> overflowUnderflowProvider() {
        return Stream.of(// large negative double – underflows to Long.MIN_VALUE
        Arguments.of(-9223372036854776.0, Long.MIN_VALUE), // fractional seconds that truncate
        Arguments.of(1.9999, 1999L));
    }

    private static Stream<Arguments> customTimeZoneProvider() {
        return Stream.of(// GMT+02:00 zone
        Arguments.of(12345.678, "GMT+02:00", 12345678L), // null zone – expect default
        Arguments.of(1609459200.0, "DEFAULT", 1609459200000L));
    }

    @Test
    @DisplayName("toCalendar converts positive seconds correctly")
    void testToCalendarPositiveSeconds() {
        // given
        // corresponds to 2021‑06‑01T12:00:00.123Z
        double seconds = 1622548800.123;
        long expectedMillis = (long) (seconds * 1000L);
        // when
        Calendar result = DoubleConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Calendar should use the TimeZone supplied by ConverterOptions");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar time in millis must match the converted value");
    }

    @Test
    @DisplayName("toCalendar truncates fractional milliseconds")
    void testToCalendarTruncatesFraction() {
        // given
        // 1999 ms -> truncates to 1999 ms
        double seconds = 1.999;
        // 1999
        long expectedMillis = (long) (seconds * 1000L);
        // when
        Calendar result = DoubleConversions.toCalendar(seconds, mockConverter);
        // then
        assertEquals(expectedMillis, result.getTimeInMillis(), "Fractional part of milliseconds must be truncated (long cast semantics)");
    }

    @Test
    @DisplayName("toCalendar handles negative epoch seconds")
    void testToCalendarNegativeSeconds() {
        // given
        // some time before the epoch
        double seconds = -12345.678;
        long expectedMillis = (long) (seconds * 1000L);
        // when
        Calendar result = DoubleConversions.toCalendar(seconds, mockConverter);
        // then
        assertEquals(expectedMillis, result.getTimeInMillis(), "Negative epoch seconds must be converted correctly");
    }

    /* -------------------------------------------------------------
     *  Group: toCalendar conversion tests
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, tz={2}")
    @MethodSource("conversionTestProvider")
    @DisplayName("toCalendar conversion tests")
    void testToCalendarConversions(double from, long expectedEpochMillis, String tzId) {
        // Arrange – configure the mock time‑zone for this case
        TimeZone tz = tzId == null ? null : TimeZone.getTimeZone(tzId);
        when(mockOptions.getTimeZone()).thenReturn(tz);
        // Act
        Calendar result = DoubleConversions.toCalendar(from, mockConverter);
        // Assert
        assertNotNull(result, "Resulting Calendar must not be null");
        // Time‑zone handling
        if (tz == null) {
            // Calendar.getInstance(null) falls back to the JVM default zone
            assertEquals(TimeZone.getDefault(), result.getTimeZone(), "When converter supplies null TimeZone, Calendar should use the default zone");
        } else {
            assertEquals(tz, result.getTimeZone(), "Calendar must use the TimeZone supplied by ConverterOptions");
        }
        // Epoch millis
        assertEquals(expectedEpochMillis, result.getTimeInMillis(), "Calendar timeInMillis must match the expected conversion");
    }

    /* -------------------------------------------------------------
     *  Group: toCalendar special double values
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("specialDoubleProvider")
    @DisplayName("toCalendar special double values")
    void testToCalendarSpecialDoubleValues(Object fromObj, long expectedEpochMillis) {
        // Arrange – the mock already returns UTC
        // NaN, POSITIVE_INFINITY, NEGATIVE_INFINITY are Numbers
        double from = ((Number) fromObj).doubleValue();
        // Act
        Calendar result = DoubleConversions.toCalendar(from, mockConverter);
        // Assert
        assertEquals(expectedEpochMillis, result.getTimeInMillis(), "Special double value should convert to the expected epoch millis");
        assertEquals(TimeZone.getTimeZone("UTC"), result.getTimeZone(), "Time zone must be UTC for these cases");
    }

    /* -------------------------------------------------------------
     *  Group: toCalendar overflow and underflow
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("overflowUnderflowProvider")
    @DisplayName("toCalendar overflow and underflow")
    void testToCalendarOverflowUnderflow(double from, long expectedEpochMillis) {
        Calendar result = DoubleConversions.toCalendar(from, mockConverter);
        assertEquals(expectedEpochMillis, result.getTimeInMillis(), "Overflow/underflow conversion must match expected saturated value");
    }

    /* -------------------------------------------------------------
     *  Group: toCalendar custom time‑zone handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] tz={1}")
    @MethodSource("customTimeZoneProvider")
    @DisplayName("toCalendar custom time‑zone handling")
    void testToCalendarCustomTimeZone(double from, String tzId, long expectedEpochMillis) {
        TimeZone tz = tzId.equals("DEFAULT") ? null : TimeZone.getTimeZone(tzId);
        when(mockOptions.getTimeZone()).thenReturn(tz);
        Calendar result = DoubleConversions.toCalendar(from, mockConverter);
        // Verify epoch millis
        assertEquals(expectedEpochMillis, result.getTimeInMillis(), "Epoch millis must be calculated correctly regardless of zone");
        // Verify zone handling
        if (tz == null) {
            assertEquals(TimeZone.getDefault(), result.getTimeZone(), "Null TimeZone should result in default JVM zone");
        } else {
            assertEquals(tz, result.getTimeZone(), "Calendar must use the supplied custom TimeZone");
        }
    }

    /* -------------------------------------------------------------
     *  Group: toCalendar invalid input handling
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("toCalendar null 'from' triggers NullPointerException")
    void testToCalendarNullFrom() {
        assertThrows(NullPointerException.class, () -> DoubleConversions.toCalendar(null, mockConverter), "Casting null to double should throw NullPointerException");
    }

    @Test
    @DisplayName("toCalendar Integer 'from' triggers ClassCastException")
    void testToCalendarIntegerFrom() {
        assertThrows(ClassCastException.class, () -> DoubleConversions.toCalendar(Integer.valueOf(12345), mockConverter), "Integer cannot be cast to double via ((double) from)");
    }

    @Test
    @DisplayName("toCalendar String 'from' triggers ClassCastException")
    void testToCalendarStringFrom() {
        assertThrows(ClassCastException.class, () -> DoubleConversions.toCalendar("2021-01-01T00:00:00Z", mockConverter), "String cannot be cast to double via ((double) from)");
    }

    @Test
    @DisplayName("toCalendar Boolean 'from' triggers ClassCastException")
    void testToCalendarBooleanFrom() {
        assertThrows(ClassCastException.class, () -> DoubleConversions.toCalendar(Boolean.TRUE, mockConverter), "Boolean cannot be cast to double via ((double) from)");
    }

    /* -------------------------------------------------------------
     *  Group: toCalendar null converter handling
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("toCalendar null converter triggers NullPointerException")
    void testToCalendarNullConverter() {
        assertThrows(NullPointerException.class, () -> DoubleConversions.toCalendar(0.0, null), "Calling with a null Converter should throw NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Group: toCalendar null options handling
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("toCalendar null options triggers NullPointerException")
    void testToCalendarNullOptions() {
        // Configure mockConverter to return null options
        when(mockConverter.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> DoubleConversions.toCalendar(12345.0, mockConverter), "When getOptions() returns null, a NullPointerException is expected");
    }
}
