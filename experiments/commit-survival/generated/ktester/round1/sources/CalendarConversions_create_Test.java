package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CalendarConversions_create_Test {

    @Mock
    private Converter // the Converter dependency
    converterMock;

    @Mock
    private ConverterOptions // the options returned by the converter
    optionsMock;

    @BeforeEach
    void setupBeforeEach() {
        // Every test expects the converter to return its options.
        when(converterMock.getOptions()).thenReturn(optionsMock);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test contamination.
        clearInvocations(converterMock, optionsMock);
    }

    private static Stream<Arguments> provideValidCreateParameters() {
        return Stream.of(// basic epoch 0
        Arguments.of(0L, "UTC"), // non‑UTC zone
        Arguments.of(1625097600000L, "America/New_York"), // negative epoch (pre‑1970)
        Arguments.of(-123456789L, "Asia/Tokyo"), // extreme long values
        Arguments.of(Long.MAX_VALUE, "UTC"), Arguments.of(Long.MIN_VALUE, "UTC"), // minimal positive / negative millisecond values
        Arguments.of(1L, "UTC"), Arguments.of(-1L, "UTC"), // extreme positive offset
        Arguments.of(0L, "GMT+14"), // extreme negative offset
        Arguments.of(0L, "GMT-12"), // typical modern timestamp with DST‑aware zone
        Arguments.of(1234567890123L, "Europe/Paris"), // historical date far before epoch
        Arguments.of(-2208988800000L, "UTC"));
    }

    /**
     * Verifies that a Calendar created with the UTC time‑zone contains the exact
     * epoch‑millisecond value supplied.
     */
    @Test
    void testCreate_WithUtcTimeZone() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_642_548_800_000L;
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(optionsMock.getTimeZone()).thenReturn(utc);
        // WHEN
        Calendar result = CalendarConversions.create(epochMilli, converterMock);
        // THEN
        assertNotNull(result, "Calendar should not be null");
        assertEquals(utc, result.getTimeZone(), "Calendar must use UTC time‑zone");
        assertEquals(epochMilli, result.getTimeInMillis(), "Calendar must contain the exact epoch milliseconds");
        // The calendar is cleared before setting the time, so all fields other than
        // the time should be at their default values.
        assertEquals(0, result.get(Calendar.MILLISECOND) % 1000, "Milliseconds field should reflect the cleared state");
    }

    /**
     * Verifies that a Calendar created with a non‑UTC time‑zone (e.g., America/New_York)
     * correctly reflects that zone while still preserving the supplied epoch‑millisecond.
     */
    @Test
    void testCreate_WithCustomTimeZone() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_642_548_800_000L;
        TimeZone ny = TimeZone.getTimeZone("America/New_York");
        when(optionsMock.getTimeZone()).thenReturn(ny);
        // WHEN
        Calendar result = CalendarConversions.create(epochMilli, converterMock);
        // THEN
        assertNotNull(result, "Calendar should not be null");
        assertEquals(ny, result.getTimeZone(), "Calendar must use the supplied New York time‑zone");
        assertEquals(epochMilli, result.getTimeInMillis(), "Calendar must retain the exact epoch milliseconds regardless of zone");
    }

    /**
     * Parameterized test covering normal and edge‑case epoch values together
     * with a variety of time‑zones (including extreme offsets and DST‑aware zones).
     */
    @ParameterizedTest(name = "[{index}] epoch={0}, zone={1}")
    @MethodSource("provideValidCreateParameters")
    void testCreate_ValidScenarios(long epochMilli, String zoneId) {
        // GIVEN
        TimeZone tz = TimeZone.getTimeZone(zoneId);
        when(optionsMock.getTimeZone()).thenReturn(tz);
        // WHEN
        Calendar cal = CalendarConversions.create(epochMilli, converterMock);
        // THEN
        assertNotNull(cal, "Calendar must not be null");
        assertEquals(tz, cal.getTimeZone(), () -> "Expected time‑zone " + zoneId + " but got " + cal.getTimeZone().getID());
        assertEquals(epochMilli, cal.getTimeInMillis(), () -> "Calendar should contain the supplied epoch millis");
    }

    @Test
    void testCreate_NullConverter_ShouldThrowNPE() {
        long epochMilli = 0L;
        assertThrows(NullPointerException.class, () -> CalendarConversions.create(epochMilli, null), "Passing a null Converter must result in NullPointerException");
    }

    @Test
    void testCreate_NullOptions_ShouldThrowNPE() {
        long epochMilli = 0L;
        when(converterMock.getOptions()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CalendarConversions.create(epochMilli, converterMock), "If Converter.getOptions() returns null, a NullPointerException is expected");
    }

    @Test
    void testCreate_NullTimeZone_ShouldThrowNPE() {
        long epochMilli = 0L;
        when(optionsMock.getTimeZone()).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CalendarConversions.create(epochMilli, converterMock), "If Options.getTimeZone() returns null, Calendar.getInstance(null) throws NPE");
    }

    @Test
    void testCreate_OptionsThrowsIllegalStateException_Propagates() {
        long epochMilli = 0L;
        when(converterMock.getOptions()).thenThrow(new IllegalStateException("options not available"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CalendarConversions.create(epochMilli, converterMock), "Exception from getOptions() must propagate");
        assertEquals("options not available", ex.getMessage());
    }

    @Test
    void testCreate_TimeZoneThrowsIllegalArgumentException_Propagates() {
        long epochMilli = 123L;
        when(optionsMock.getTimeZone()).thenThrow(new IllegalArgumentException("invalid zone"));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> CalendarConversions.create(epochMilli, converterMock), "Exception from getTimeZone() must propagate");
        assertEquals("invalid zone", ex.getMessage());
    }

    @Test
    void testCreate_ConverterConvertThrowsButCreateIgnoresIt() {
        // The create() method never calls converter.convert(...), therefore a
        // RuntimeException from that method must not affect the outcome.
        long epochMilli = 0L;
        when(optionsMock.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        // Configure the mock to throw on convert() – a method that create() never invokes.
        doThrow(new RuntimeException("conversion failure")).when(converterMock).convert(any(), any());
        // No exception should be thrown from create().
        Calendar cal = assertDoesNotThrow(() -> CalendarConversions.create(epochMilli, converterMock), "create() must ignore unrelated converter methods");
        assertNotNull(cal);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(epochMilli, cal.getTimeInMillis());
    }
}
