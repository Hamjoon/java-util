package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toZonedDateTime_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock Converter to return the mock options
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        // Use a deterministic ZoneId for reproducible results
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("America/New_York"));
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test interference
        clearInvocations(mockConverter, mockOptions);
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (parameterised)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> successfulConversions() {
        return Stream.of(// source LocalDateTime, expected ZoneId, expected ZonedDateTime
        Arguments.of(LocalDateTime.of(2023, 5, 15, 10, 30, 45), ZoneId.of("UTC"), ZonedDateTime.of(LocalDateTime.of(2023, 5, 15, 10, 30, 45), ZoneId.of("UTC"))), Arguments.of(LocalDateTime.of(2021, 12, 31, 23, 59, 59), ZoneId.of("America/New_York"), ZonedDateTime.of(LocalDateTime.of(2021, 12, 31, 23, 59, 59), ZoneId.of("America/New_York"))));
    }

    @Test
    void toZonedDateTime_WithLocalDateTime_ReturnsZonedDateTimeInConfiguredZone() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2023, Month.MAY, 10, 15, 30, 45, 123_456_789);
        // WHEN
        ZonedDateTime result = LocalDateTimeConversions.toZonedDateTime(source, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(source, result.toLocalDateTime(), "The LocalDateTime part of the result must match the source");
        assertEquals(ZoneId.of("America/New_York"), result.getZone(), "The ZoneId of the result must be the one supplied by ConverterOptions");
    }

    @Test
    void toZonedDateTime_WithDifferentZone_ReturnsZonedDateTimeInThatZone() {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        LocalDateTime source = LocalDateTime.of(2021, 12, 31, 23, 59, 59);
        // WHEN
        ZonedDateTime result = LocalDateTimeConversions.toZonedDateTime(source, mockConverter);
        // THEN
        assertEquals(ZoneId.of("UTC"), result.getZone(), "The ZoneId should reflect the updated configuration");
        assertEquals(source, result.toLocalDateTime(), "LocalDateTime component must remain unchanged");
    }

    @Test
    void toZonedDateTime_WithInvalidSource_ThrowsClassCastException() {
        // GIVEN
        Object notALocalDateTime = "2023-01-01T12:00:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toZonedDateTime(notALocalDateTime, mockConverter), "Passing a non‑LocalDateTime object should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] source={0}, zone={1}")
    @MethodSource("successfulConversions")
    void toZonedDateTime_Successful(LocalDateTime source, ZoneId zone, ZonedDateTime expected) {
        // GIVEN – configure the mock to return the zone required for this case
        when(mockOptions.getZoneId()).thenReturn(zone);
        // WHEN
        ZonedDateTime result = LocalDateTimeConversions.toZonedDateTime(source, mockConverter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result, "The produced ZonedDateTime must match the expected value");
    }

    @Test
    void toZonedDateTime_NullSource_ThrowsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(nullSource, mockConverter), "Passing null as the source should result in NullPointerException");
    }

    @Test
    void toZonedDateTime_InvalidSource_ThrowsClassCastException() {
        // GIVEN
        Object notALocalDateTime = "2023-01-01T00:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toZonedDateTime(notALocalDateTime, mockConverter), "Providing a non‑LocalDateTime source must trigger ClassCastException");
    }

    @Test
    void toZonedDateTime_ConverterReturnsNullOptions_ThrowsNullPointerException() {
        // GIVEN
        when(mockConverter.getOptions()).thenReturn(null);
        LocalDateTime source = LocalDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(source, mockConverter), "If Converter#getOptions() returns null, a NullPointerException is expected");
    }

    @Test
    void toZonedDateTime_OptionsReturnNullZoneId_ThrowsNullPointerException() {
        // GIVEN
        when(mockOptions.getZoneId()).thenReturn(null);
        LocalDateTime source = LocalDateTime.now();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(source, mockConverter), "A null ZoneId supplied by ConverterOptions should cause NullPointerException");
    }

    @Test
    void toZonedDateTime_NullConverter_ThrowsNullPointerException() {
        // GIVEN
        Converter nullConverter = null;
        LocalDateTime source = LocalDateTime.of(2022, 1, 1, 12, 0);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toZonedDateTime(source, nullConverter), "Passing a null Converter must result in NullPointerException");
    }

    @Test
    void toZonedDateTime_ConverterOptionsThrowsIllegalStateException_Propagates() {
        // GIVEN
        when(mockConverter.getOptions()).thenThrow(new IllegalStateException("options unavailable"));
        LocalDateTime source = LocalDateTime.of(2022, 1, 1, 12, 0);
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> LocalDateTimeConversions.toZonedDateTime(source, mockConverter), "Exception from Converter#getOptions() should propagate");
        assertEquals("options unavailable", ex.getMessage());
    }

    @Test
    void toZonedDateTime_OptionsGetZoneIdThrowsIllegalArgumentException_Propagates() {
        // GIVEN
        when(mockOptions.getZoneId()).thenThrow(new IllegalArgumentException("bad zone"));
        LocalDateTime source = LocalDateTime.of(2022, 1, 1, 12, 0);
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toZonedDateTime(source, mockConverter), "Exception from ConverterOptions#getZoneId() should propagate");
        assertEquals("bad zone", ex.getMessage());
    }
}
