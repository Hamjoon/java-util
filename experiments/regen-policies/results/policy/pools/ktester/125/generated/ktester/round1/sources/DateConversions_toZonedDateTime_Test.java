package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Instant;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toZonedDateTime_Test {

    @Mock
    private Converter // mock of the Converter used by DateConversions
    converter;

    @Mock
    private ConverterOptions // mock of the options returned by the Converter
    options;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy: converter.getOptions() → options
        when(converter.getOptions()).thenReturn(options);
    }

    @Test
    void toZonedDateTime_WithUtcZone_ReturnsCorrectZonedDateTime() {
        // GIVEN
        // a fixed instant
        long epochMilli = 1_600_000_000_000L;
        Date date = new Date(epochMilli);
        ZoneId utc = ZoneId.of("UTC");
        when(options.getZoneId()).thenReturn(utc);
        // WHEN
        ZonedDateTime result = DateConversions.toZonedDateTime(date, converter);
        // THEN
        ZonedDateTime expected = Instant.ofEpochMilli(epochMilli).atZone(utc);
        assertEquals(expected, result, "ZonedDateTime should be calculated using the UTC zone");
    }

    @Test
    void toZonedDateTime_WithCustomZone_ReturnsCorrectZonedDateTime() {
        // GIVEN
        long epochMilli = 1_600_000_000_000L;
        Date date = new Date(epochMilli);
        ZoneId newYork = ZoneId.of("America/New_York");
        when(options.getZoneId()).thenReturn(newYork);
        // WHEN
        ZonedDateTime result = DateConversions.toZonedDateTime(date, converter);
        // THEN
        ZonedDateTime expected = Instant.ofEpochMilli(epochMilli).atZone(newYork);
        assertEquals(expected, result, "ZonedDateTime should be calculated using the America/New_York zone");
    }

    @Test
    void toZonedDateTime_NullDate_ThrowsNullPointerException() {
        // GIVEN
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toZonedDateTime(null, converter), "Passing a null Date should result in NullPointerException");
    }

    @Test
    @DisplayName("Passing a non‑Date object should throw ClassCastException")
    void toZonedDateTime_NonDateObject_ThrowsClassCastException() {
        // GIVEN
        // not a java.util.Date
        Object notADate = new Object();
        when(options.getZoneId()).thenReturn(ZoneId.of("UTC"));
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toZonedDateTime(notADate, converter), "Providing an object that is not a java.util.Date should trigger a ClassCastException");
    }

    @Test
    @DisplayName("Passing a null Converter should throw NullPointerException")
    void toZonedDateTime_NullConverter_ThrowsNullPointerException() {
        // GIVEN
        // any non‑null date
        Date date = new Date();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toZonedDateTime(date, null), "Passing a null Converter should cause a NullPointerException when accessing converter.getOptions()");
    }

    @Test
    @DisplayName("Converter returning null options should throw NullPointerException")
    void toZonedDateTime_ConverterReturnsNullOptions_ThrowsNullPointerException() {
        // GIVEN
        // any non‑null date
        Date date = new Date();
        Converter converterWithNullOptions = mock(Converter.class);
        // simulate null options
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toZonedDateTime(date, converterWithNullOptions), "If converter.getOptions() returns null, a NullPointerException should be thrown");
    }

    @Test
    @DisplayName("Options returning null ZoneId should throw NullPointerException")
    void toZonedDateTime_OptionsReturnNullZoneId_ThrowsNullPointerException() {
        // GIVEN
        // any non‑null date
        Date date = new Date();
        ConverterOptions optionsWithNullZone = mock(ConverterOptions.class);
        // simulate null ZoneId
        when(optionsWithNullZone.getZoneId()).thenReturn(null);
        Converter converterWithNullZone = mock(Converter.class);
        when(converterWithNullZone.getOptions()).thenReturn(optionsWithNullZone);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toZonedDateTime(date, converterWithNullZone), "If options.getZoneId() returns null, a NullPointerException should be thrown");
    }
}
