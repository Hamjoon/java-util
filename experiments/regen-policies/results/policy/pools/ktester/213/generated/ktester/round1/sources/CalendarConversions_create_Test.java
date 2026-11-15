package com.cedarsoftware.util.convert;

import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_create_Test {

    /**
     * Mocked {@link Converter} that will be injected into the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Mocked {@link ConverterOptions} returned by {@code mockConverter.getOptions()}.
     */
    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Wire the default mock hierarchy: Converter -> ConverterOptions.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions to keep each test independent.
        reset(mockConverter, mockOptions);
    }

    /**
     * Helper that builds a mock {@link Converter} returning the supplied {@link TimeZone}.
     */
    private static Converter mockConverterWith(TimeZone tz) {
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getTimeZone()).thenReturn(tz);
        return converter;
    }

    private static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(// epochMilli = 0, UTC
        Arguments.of(0L, "UTC", "Calendar instance with TimeZone UTC, cleared fields, time set to 1970‑01‑01T00:00:00Z (epochMilli 0)."), // epochMilli = 1622548800000 (2021‑06‑01T12:00:00‑07:00), America/Los_Angeles
        Arguments.of(1_622_548_800_000L, "America/Los_Angeles", "Calendar instance with TimeZone America/Los_Angeles, cleared fields, time set to 2021‑06‑01T12:00:00‑07:00."));
    }

    private static Stream<Arguments> epochBoundaryProvider() {
        return Stream.of(// One millisecond before epoch, UTC
        Arguments.of(-1L, "UTC", "Calendar instance with TimeZone UTC, cleared fields, time set to 1969‑12‑31T23:59:59.999Z."), // Long.MAX_VALUE milliseconds, UTC
        Arguments.of(Long.MAX_VALUE, "UTC", "Calendar instance with TimeZone UTC, cleared fields, time set to the far‑future date."));
    }

    @Test
    void create_usesConverterTimeZone_andSetsExactEpochMillis() {
        // Arrange – choose a distinct time‑zone and a known epoch‑millis value.
        // UTC+9
        TimeZone tz = TimeZone.getTimeZone("Asia/Tokyo");
        // 2022‑01‑01T00:00:00.123Z
        long epochMilli = 1_640_995_200_123L;
        when(mockOptions.getTimeZone()).thenReturn(tz);
        // Act – invoke the focal method.
        Calendar result = CalendarConversions.create(epochMilli, mockConverter);
        // Assert – the calendar must use the supplied time‑zone and hold the exact millis.
        assertNotNull(result, "Calendar should not be null");
        assertEquals(tz, result.getTimeZone(), "Calendar should be created with the Converter's TimeZone");
        assertEquals(epochMilli, result.getTimeInMillis(), "Calendar should contain the exact epoch milli supplied to create()");
    }

    @Test
    void create_withRealConverter_matchesConverterOptionsTimeZone() {
        // This test exercises the method with a real Converter instance (no mocking).
        // It verifies that the Calendar's time‑zone matches the one configured in the options.
        // Arrange – a real Converter with default options (which default to the JVM default zone).
        Converter realConverter = new Converter(new DefaultConverterOptions());
        TimeZone expectedTz = realConverter.getOptions().getTimeZone();
        long epochMilli = System.currentTimeMillis();
        // Act
        Calendar cal = CalendarConversions.create(epochMilli, realConverter);
        // Assert
        assertEquals(expectedTz, cal.getTimeZone(), "Calendar should be created with the time‑zone from the real Converter's options");
        assertEquals(epochMilli, cal.getTimeInMillis(), "Calendar should contain the epoch milli that was passed in");
    }

    @ParameterizedTest(name = "[{index}] epochMilli={0}, zone={1}")
    @MethodSource("normalBehaviorProvider")
    void create_normalBehavior(long epochMilli, String zoneId, String _description) {
        // Arrange
        TimeZone tz = TimeZone.getTimeZone(zoneId);
        Converter converter = mockConverterWith(tz);
        // Act
        Calendar cal = CalendarConversions.create(epochMilli, converter);
        // Assert
        assertNotNull(cal, "Resulting Calendar must not be null");
        assertEquals(tz, cal.getTimeZone(), "Calendar should be created with the TimeZone supplied by the Converter");
        assertEquals(epochMilli, cal.getTimeInMillis(), "Calendar must contain the exact epoch milliseconds passed to create()");
    }

    @ParameterizedTest(name = "[{index}] epochMilli={0}, zone={1}")
    @MethodSource("epochBoundaryProvider")
    void create_epochBoundaries(long epochMilli, String zoneId, String _description) {
        // Arrange
        TimeZone tz = TimeZone.getTimeZone(zoneId);
        Converter converter = mockConverterWith(tz);
        // Act
        Calendar cal = CalendarConversions.create(epochMilli, converter);
        // Assert
        assertNotNull(cal, "Resulting Calendar must not be null");
        assertEquals(tz, cal.getTimeZone(), "Calendar should use the Converter's TimeZone even for extreme epoch values");
        assertEquals(epochMilli, cal.getTimeInMillis(), "Calendar must store the extreme epoch milli value unchanged");
    }

    @Test
    void create_nullConverter_throwsNullPointerException() {
        // Arrange
        long epochMilli = 0L;
        Converter nullConverter = null;
        // Act & Assert
        assertThrows(NullPointerException.class, () -> CalendarConversions.create(epochMilli, nullConverter), "Passing a null Converter should result in NullPointerException");
    }

    @Test
    void create_converterWithNullOptions_throwsNullPointerException() {
        // Arrange
        long epochMilli = 123_456_789L;
        Converter converter = mock(Converter.class);
        // Simulate getOptions() returning null
        when(converter.getOptions()).thenReturn(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> CalendarConversions.create(epochMilli, converter), "If Converter.getOptions() returns null, a NullPointerException is expected");
    }
}
