package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigInteger;
import java.time.DateTimeException;
import java.time.format.DateTimeParseException;

@ExtendWith(MockitoExtension.class)
class MapConversions_toLocalDate_Test {

    @Mock
    private Converter // mocked Converter used in the tests
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates and injects the mock for us via @Mock/@ExtendWith
        // Reset mock to a clean state before each test
        reset(mockConverter);
    }

    /**
     * Verify that when the input map contains the keys YEAR, MONTH and DAY,
     * {@code MapConversions.toLocalDate} extracts the three values via the
     * {@link Converter#convert(Object, Class)} method and builds a LocalDate
     * directly (bypassing the generic {@code fromMap} fallback).
     */
    @Test
    void toLocalDate_usesYearMonthDayKeys() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // using String values to force conversion
        map.put(MapConversions.YEAR, "2023");
        map.put(MapConversions.MONTH, "7");
        map.put(MapConversions.DAY, "14");
        // Mock the Converter to return the expected integer values
        when(mockConverter.convert("2023", int.class)).thenReturn(2023);
        when(mockConverter.convert("7", int.class)).thenReturn(7);
        when(mockConverter.convert("14", int.class)).thenReturn(14);
        // WHEN
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // THEN
        assertEquals(LocalDate.of(2023, 7, 14), result, "The method should construct a LocalDate from the supplied year/month/day values.");
        // Verify that the converter was called exactly three times with the correct arguments
        verify(mockConverter, times(1)).convert("2023", int.class);
        verify(mockConverter, times(1)).convert("7", int.class);
        verify(mockConverter, times(1)).convert("14", int.class);
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * Verify that when the map does not contain YEAR/MONTH/DAY but does contain a
     * {@code DATE} entry, the method falls back to {@code fromMap}.  Because
     * {@code fromMap} is private, we test the fallback indirectly by mocking the
     * {@link Converter#convert(Object, Class)} call that would be performed for the
     * {@code DATE} key.
     */
    @Test
    void toLocalDate_usesDateKeyFallback() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // arbitrary object; the converter decides how to turn it into a LocalDate
        Object dateValue = "2022-12-31";
        map.put(MapConversions.DATE, dateValue);
        // The fallback path will call converter.convert(dateValue, LocalDate.class)
        LocalDate expected = LocalDate.of(2022, 12, 31);
        when(mockConverter.convert(dateValue, LocalDate.class)).thenReturn(expected);
        // WHEN
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // THEN
        assertEquals(expected, result, "When YEAR/MONTH/DAY are missing, the method should delegate to the converter using the DATE key.");
        verify(mockConverter, times(1)).convert(dateValue, LocalDate.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_directConstruction_intValues() {
        // GIVEN – map with integer values
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, 2023);
        map.put(MapConversions.MONTH, 5);
        map.put(MapConversions.DAY, 17);
        // Mock identity conversion for int.class
        when(mockConverter.convert(2023, int.class)).thenReturn(2023);
        when(mockConverter.convert(5, int.class)).thenReturn(5);
        when(mockConverter.convert(17, int.class)).thenReturn(17);
        // WHEN
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // THEN
        assertEquals(LocalDate.of(2023, 5, 17), result);
        verify(mockConverter, times(1)).convert(2023, int.class);
        verify(mockConverter, times(1)).convert(5, int.class);
        verify(mockConverter, times(1)).convert(17, int.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_directConstruction_stringValues() {
        // GIVEN – map with String representations
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, "1999");
        map.put(MapConversions.MONTH, "12");
        map.put(MapConversions.DAY, "31");
        when(mockConverter.convert("1999", int.class)).thenReturn(1999);
        when(mockConverter.convert("12", int.class)).thenReturn(12);
        when(mockConverter.convert("31", int.class)).thenReturn(31);
        // WHEN
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // THEN
        assertEquals(LocalDate.of(1999, 12, 31), result);
        verify(mockConverter, times(1)).convert("1999", int.class);
        verify(mockConverter, times(1)).convert("12", int.class);
        verify(mockConverter, times(1)).convert("31", int.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_directConstruction_verifyConversionOrderIrrelevant() {
        // GIVEN – map with integer values, but we verify that order does not matter
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, 1970);
        map.put(MapConversions.MONTH, 1);
        map.put(MapConversions.DAY, 1);
        when(mockConverter.convert(1970, int.class)).thenReturn(1970);
        // month
        when(mockConverter.convert(1, int.class)).thenReturn(1);
        // day (same value)
        when(mockConverter.convert(1, int.class)).thenReturn(1);
        // WHEN
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // THEN
        assertEquals(LocalDate.of(1970, 1, 1), result);
        // Verify three invocations – order is not asserted, only count
        verify(mockConverter, times(3)).convert(any(), eq(int.class));
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_fallbackUsesDateKey() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        Object dateValue = "2022-12-31";
        map.put(MapConversions.DATE, dateValue);
        LocalDate expected = LocalDate.of(2022, 12, 31);
        when(mockConverter.convert(dateValue, LocalDate.class)).thenReturn(expected);
        // WHEN
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // THEN
        assertEquals(expected, result);
        verify(mockConverter, times(1)).convert(dateValue, LocalDate.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_fallbackDateKeyReturnsNull() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, null);
        when(mockConverter.convert(null, LocalDate.class)).thenReturn(null);
        // WHEN
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // THEN
        assertNull(result);
        verify(mockConverter, times(1)).convert(null, LocalDate.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_fallbackMalformedDateString() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        String badDate = "invalid-date-format";
        map.put(MapConversions.DATE, badDate);
        when(mockConverter.convert(badDate, LocalDate.class)).thenThrow(DateTimeParseException.class);
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> MapConversions.toLocalDate(map, mockConverter));
        verify(mockConverter, times(1)).convert(badDate, LocalDate.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_missingDayReturnsNull() {
        // GIVEN – YEAR and MONTH present, DAY missing
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, 2021);
        map.put(MapConversions.MONTH, 4);
        // No DAY entry
        // WHEN
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // THEN
        assertNull(result);
        // No conversion should have happened because fallback does not find DATE key
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toLocalDate_emptyMapReturnsNull() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // WHEN
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // THEN
        assertNull(result);
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toLocalDate_mixedNumberTypes() {
        // GIVEN – Long, BigInteger, Double values
        Map<String, Object> map = new HashMap<>();
        // Long
        map.put(MapConversions.YEAR, 2022L);
        // BigInteger
        map.put(MapConversions.MONTH, new BigInteger("7"));
        // Double (fraction ignored)
        map.put(MapConversions.DAY, 15.9);
        when(mockConverter.convert(2022L, int.class)).thenReturn(2022);
        when(mockConverter.convert(new BigInteger("7"), int.class)).thenReturn(7);
        when(mockConverter.convert(15.9, int.class)).thenReturn(15);
        // WHEN
        LocalDate result = MapConversions.toLocalDate(map, mockConverter);
        // THEN
        assertEquals(LocalDate.of(2022, 7, 15), result);
        verify(mockConverter, times(1)).convert(2022L, int.class);
        verify(mockConverter, times(1)).convert(new BigInteger("7"), int.class);
        verify(mockConverter, times(1)).convert(15.9, int.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_nonNumericYearThrowsNumberFormatException() {
        // GIVEN – YEAR is a non‑numeric string
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, "notANumber");
        map.put(MapConversions.MONTH, "12");
        map.put(MapConversions.DAY, "01");
        when(mockConverter.convert("notANumber", int.class)).thenThrow(NumberFormatException.class);
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toLocalDate(map, mockConverter));
        verify(mockConverter, times(1)).convert("notANumber", int.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_monthOutOfRangeThrowsDateTimeException() {
        // GIVEN – month = 13
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, 2020);
        map.put(MapConversions.MONTH, 13);
        map.put(MapConversions.DAY, 1);
        when(mockConverter.convert(2020, int.class)).thenReturn(2020);
        when(mockConverter.convert(13, int.class)).thenReturn(13);
        when(mockConverter.convert(1, int.class)).thenReturn(1);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> MapConversions.toLocalDate(map, mockConverter));
        verify(mockConverter, times(3)).convert(any(), eq(int.class));
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_invalidDayForFebruaryThrowsDateTimeException() {
        // GIVEN – February 30th
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, 2020);
        map.put(MapConversions.MONTH, 2);
        map.put(MapConversions.DAY, 30);
        when(mockConverter.convert(2020, int.class)).thenReturn(2020);
        when(mockConverter.convert(2, int.class)).thenReturn(2);
        when(mockConverter.convert(30, int.class)).thenReturn(30);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> MapConversions.toLocalDate(map, mockConverter));
        verify(mockConverter, times(3)).convert(any(), eq(int.class));
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_converterThrowsRuntimeException() {
        // GIVEN – valid map but converter throws on first conversion
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, 2021);
        map.put(MapConversions.MONTH, 5);
        map.put(MapConversions.DAY, 10);
        when(mockConverter.convert(2021, int.class)).thenThrow(RuntimeException.class);
        // WHEN / THEN
        assertThrows(RuntimeException.class, () -> MapConversions.toLocalDate(map, mockConverter));
        verify(mockConverter, times(1)).convert(2021, int.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toLocalDate_nullMapThrowsNullPointerException() {
        // GIVEN
        Map<String, Object> nullMap = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toLocalDate(nullMap, mockConverter));
        // No interaction with the mock is expected because the cast fails first
        verifyNoInteractions(mockConverter);
    }
}
