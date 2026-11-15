package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toOffsetDateTime_Test {

    @Mock
    private Converter // mock supplied by MockitoExtension
    converter;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock interactions before each test
        reset(converter);
    }

    @Test
    void testToOffsetDateTime_WithTimeAndOffsetOnly_ReturnsOffsetDateTime() {
        // GIVEN – a map that contains only TIME and OFFSET keys
        LocalDateTime expectedLdt = LocalDateTime.of(2023, Month.MARCH, 15, 10, 30, 45);
        ZoneOffset expectedOffset = ZoneOffset.ofHoursMinutes(2, 0);
        OffsetDateTime expected = OffsetDateTime.of(expectedLdt, expectedOffset);
        Map<String, Object> map = new HashMap<>();
        // any placeholder – the mock will handle conversion
        Object timeKey = new Object();
        Object offsetKey = new Object();
        map.put(MapConversions.TIME, timeKey);
        map.put(MapConversions.OFFSET, offsetKey);
        // DATE is intentionally omitted
        // AND – the converter mock returns the prepared objects
        when(converter.convert(timeKey, LocalDateTime.class)).thenReturn(expectedLdt);
        when(converter.convert(offsetKey, ZoneOffset.class)).thenReturn(expectedOffset);
        // WHEN
        OffsetDateTime actual = MapConversions.toOffsetDateTime(map, converter);
        // THEN
        assertEquals(expected, actual, "OffsetDateTime should be built from LocalDateTime and ZoneOffset");
        verify(converter).convert(timeKey, LocalDateTime.class);
        verify(converter).convert(offsetKey, ZoneOffset.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_WithDateTimeAndOffset_ReturnsOffsetDateTime() {
        // GIVEN – a map that contains DATE, TIME and OFFSET keys
        LocalDate expectedDate = LocalDate.of(2021, Month.DECEMBER, 5);
        LocalTime expectedTime = LocalTime.of(14, 20, 10, 123_456_789);
        ZoneOffset expectedOffset = ZoneOffset.ofHours(-5);
        OffsetDateTime expected = OffsetDateTime.of(expectedDate, expectedTime, expectedOffset);
        Map<String, Object> map = new HashMap<>();
        Object dateKey = new Object();
        Object timeKey = new Object();
        Object offsetKey = new Object();
        map.put(MapConversions.DATE, dateKey);
        map.put(MapConversions.TIME, timeKey);
        map.put(MapConversions.OFFSET, offsetKey);
        // AND – the converter mock returns the prepared objects
        when(converter.convert(dateKey, LocalDate.class)).thenReturn(expectedDate);
        when(converter.convert(timeKey, LocalTime.class)).thenReturn(expectedTime);
        when(converter.convert(offsetKey, ZoneOffset.class)).thenReturn(expectedOffset);
        // WHEN
        OffsetDateTime actual = MapConversions.toOffsetDateTime(map, converter);
        // THEN
        assertEquals(expected, actual, "OffsetDateTime should be built from LocalDate, LocalTime and ZoneOffset");
        verify(converter).convert(dateKey, LocalDate.class);
        verify(converter).convert(timeKey, LocalTime.class);
        verify(converter).convert(offsetKey, ZoneOffset.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_WithMissingKeys_DelegatesToFromMap() {
        // GIVEN – a map that does not satisfy the two fast‑path conditions
        Map<String, Object> map = new HashMap<>();
        map.put("someOtherKey", "value");
        // The private fromMap method ultimately ends up calling the public Converter.convert(...)
        // for the target type. We can let the mock behave like the real Converter for this case.
        // When the fallback path is taken, MapConversions will call:
        //   converter.convert(map, OffsetDateTime.class)
        // So we stub that conversion.
        OffsetDateTime expected = OffsetDateTime.now();
        when(converter.convert(map, OffsetDateTime.class)).thenReturn(expected);
        // WHEN
        OffsetDateTime actual = MapConversions.toOffsetDateTime(map, converter);
        // THEN
        assertEquals(expected, actual, "When fast‑path conditions are not met, conversion should be delegated to the generic map conversion");
        verify(converter).convert(map, OffsetDateTime.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_WithExplicitNullDate_TreatedLikeMissingDate() {
        // GIVEN – map contains DATE key with a null value, plus TIME and OFFSET
        LocalDateTime expectedLdt = LocalDateTime.of(2023, 8, 15, 12, 34, 56);
        ZoneOffset expectedOffset = ZoneOffset.ofHoursMinutes(2, 0);
        OffsetDateTime expected = OffsetDateTime.of(expectedLdt, expectedOffset);
        Map<String, Object> map = new HashMap<>();
        Object timeKey = new Object();
        Object offsetKey = new Object();
        // explicit null
        map.put(MapConversions.DATE, null);
        map.put(MapConversions.TIME, timeKey);
        map.put(MapConversions.OFFSET, offsetKey);
        when(converter.convert(timeKey, LocalDateTime.class)).thenReturn(expectedLdt);
        when(converter.convert(offsetKey, ZoneOffset.class)).thenReturn(expectedOffset);
        // WHEN
        OffsetDateTime actual = MapConversions.toOffsetDateTime(map, converter);
        // THEN
        assertEquals(expected, actual, "Null DATE should be ignored and behave like the TIME+OFFSET branch");
        verify(converter).convert(timeKey, LocalDateTime.class);
        verify(converter).convert(offsetKey, ZoneOffset.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_WithPreConvertedObjects_NoConversionNeeded() {
        // GIVEN – map already holds a LocalDateTime and a ZoneOffset
        LocalDateTime ldt = LocalDateTime.of(2023, 8, 15, 12, 34, 56);
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(2, 0);
        OffsetDateTime expected = OffsetDateTime.of(ldt, offset);
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, ldt);
        map.put(MapConversions.OFFSET, offset);
        // Converter simply returns the supplied instance (identity)
        when(converter.convert(ldt, LocalDateTime.class)).thenReturn(ldt);
        when(converter.convert(offset, ZoneOffset.class)).thenReturn(offset);
        // WHEN
        OffsetDateTime actual = MapConversions.toOffsetDateTime(map, converter);
        // THEN
        assertEquals(expected, actual, "Method should work when map already contains target types");
        verify(converter).convert(ldt, LocalDateTime.class);
        verify(converter).convert(offset, ZoneOffset.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_FallbackEmptyMap_ReturnsNull() {
        // GIVEN – an empty map that does not satisfy any fast‑path condition
        Map<String, Object> map = new HashMap<>();
        // The private fromMap ultimately calls converter.convert(map, OffsetDateTime.class)
        when(converter.convert(map, OffsetDateTime.class)).thenReturn(null);
        // WHEN
        OffsetDateTime actual = MapConversions.toOffsetDateTime(map, converter);
        // THEN
        assertNull(actual, "Empty map should result in null from the fallback conversion");
        verify(converter).convert(map, OffsetDateTime.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_FallbackMissingTime_ReturnsNull() {
        // GIVEN – map contains OFFSET (and optionally DATE) but no TIME
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.OFFSET, "+02:00");
        map.put(MapConversions.DATE, "2023-08-15");
        when(converter.convert(map, OffsetDateTime.class)).thenReturn(null);
        // WHEN
        OffsetDateTime actual = MapConversions.toOffsetDateTime(map, converter);
        // THEN
        assertNull(actual, "Missing TIME should cause fallback to return null");
        verify(converter).convert(map, OffsetDateTime.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_FallbackMissingOffset_ReturnsNull() {
        // GIVEN – map contains TIME (and DATE) but no OFFSET
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, "12:34:56");
        map.put(MapConversions.DATE, "2023-08-15");
        when(converter.convert(map, OffsetDateTime.class)).thenReturn(null);
        // WHEN
        OffsetDateTime actual = MapConversions.toOffsetDateTime(map, converter);
        // THEN
        assertNull(actual, "Missing OFFSET should cause fallback to return null");
        verify(converter).convert(map, OffsetDateTime.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_FallbackFromMapThrowsRuntimeException() {
        // GIVEN – map with unrelated keys; fromMap will delegate to converter.convert(...)
        Map<String, Object> map = new HashMap<>();
        map.put("someKey", "someValue");
        RuntimeException expected = new RuntimeException("Unsupported key set");
        when(converter.convert(map, OffsetDateTime.class)).thenThrow(expected);
        // WHEN / THEN
        RuntimeException actual = assertThrows(RuntimeException.class, () -> MapConversions.toOffsetDateTime(map, converter), "Exception from fallback conversion should propagate");
        assertSame(expected, actual, "Propagated exception should be the one thrown by the converter");
        verify(converter).convert(map, OffsetDateTime.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_NullMap_ThrowsNullPointerException() {
        // GIVEN a null reference as the 'from' argument
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toOffsetDateTime(null, converter), "Passing null should result in NPE when the method attempts to cast and access the map");
    }

    @Test
    void testToOffsetDateTime_NonMapInput_ThrowsClassCastException() {
        // GIVEN a non‑map object
        String notAMap = "just a string";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toOffsetDateTime(notAMap, converter), "Supplying a non‑Map argument should cause a ClassCastException");
    }

    @Test
    void testToOffsetDateTime_InvalidTimeValue_ConversionExceptionPropagates() {
        // GIVEN – TIME value cannot be converted to LocalDateTime/LocalTime
        Map<String, Object> map = new HashMap<>();
        // not a String
        Object badTime = 12345;
        Object offsetKey = new Object();
        map.put(MapConversions.TIME, badTime);
        map.put(MapConversions.OFFSET, offsetKey);
        RuntimeException conversionException = new RuntimeException("ConversionException for time");
        when(converter.convert(badTime, LocalDateTime.class)).thenThrow(conversionException);
        // WHEN / THEN
        RuntimeException actual = assertThrows(RuntimeException.class, () -> MapConversions.toOffsetDateTime(map, converter), "ConversionException from time conversion should propagate");
        assertSame(conversionException, actual);
        verify(converter).convert(badTime, LocalDateTime.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_InvalidOffsetValue_ConversionExceptionPropagates() {
        // GIVEN – OFFSET value cannot be converted to ZoneOffset
        Map<String, Object> map = new HashMap<>();
        Object timeKey = new Object();
        // not a String
        Object badOffset = 999;
        map.put(MapConversions.TIME, timeKey);
        map.put(MapConversions.OFFSET, badOffset);
        LocalDateTime ldt = LocalDateTime.of(2023, 8, 15, 12, 34, 56);
        when(converter.convert(timeKey, LocalDateTime.class)).thenReturn(ldt);
        RuntimeException conversionException = new RuntimeException("ConversionException for offset");
        when(converter.convert(badOffset, ZoneOffset.class)).thenThrow(conversionException);
        // WHEN / THEN
        RuntimeException actual = assertThrows(RuntimeException.class, () -> MapConversions.toOffsetDateTime(map, converter), "ConversionException from offset conversion should propagate");
        assertSame(conversionException, actual);
        verify(converter).convert(timeKey, LocalDateTime.class);
        verify(converter).convert(badOffset, ZoneOffset.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_InvalidDateValue_ConversionExceptionPropagates() {
        // GIVEN – DATE value cannot be converted to LocalDate
        Map<String, Object> map = new HashMap<>();
        // invalid type
        Object dateKey = true;
        Object timeKey = new Object();
        Object offsetKey = new Object();
        map.put(MapConversions.DATE, dateKey);
        map.put(MapConversions.TIME, timeKey);
        map.put(MapConversions.OFFSET, offsetKey);
        RuntimeException conversionException = new RuntimeException("ConversionException for date");
        when(converter.convert(dateKey, LocalDate.class)).thenThrow(conversionException);
        // WHEN / THEN
        RuntimeException actual = assertThrows(RuntimeException.class, () -> MapConversions.toOffsetDateTime(map, converter), "ConversionException from date conversion should propagate");
        assertSame(conversionException, actual);
        verify(converter).convert(dateKey, LocalDate.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void testToOffsetDateTime_ConverterReturnsNull_ResultsInNullPointerException() {
        // GIVEN – converter returns null for one of the required components
        Map<String, Object> map = new HashMap<>();
        Object timeKey = new Object();
        Object offsetKey = new Object();
        map.put(MapConversions.TIME, timeKey);
        map.put(MapConversions.OFFSET, offsetKey);
        // null LDT
        when(converter.convert(timeKey, LocalDateTime.class)).thenReturn(null);
        when(converter.convert(offsetKey, ZoneOffset.class)).thenReturn(ZoneOffset.UTC);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toOffsetDateTime(map, converter), "Null result from converter should cause NPE inside OffsetDateTime.of");
        verify(converter).convert(timeKey, LocalDateTime.class);
        verify(converter).convert(offsetKey, ZoneOffset.class);
        verifyNoMoreInteractions(converter);
    }
}
