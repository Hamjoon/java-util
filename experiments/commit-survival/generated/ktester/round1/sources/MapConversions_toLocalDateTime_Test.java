package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.*;
import java.time.format.DateTimeParseException;

@ExtendWith(MockitoExtension.class)
class MapConversions_toLocalDateTime_Test {

    /**
     * The mock Converter used for all test cases.
     */
    @Mock
    private Converter converter;

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions so each test starts with a clean mock state.
        reset(converter);
    }

    @Test
    void toLocalDateTime_WithDateAndTime_ReturnsCombinedDateTime() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        String dateKey = "date";
        String timeKey = "time";
        // Arbitrary objects that will be passed to the converter.
        Object dateObj = "2023-01-15";
        Object timeObj = "13:45:30";
        map.put(dateKey, dateObj);
        map.put(timeKey, timeObj);
        LocalDate expectedDate = LocalDate.of(2023, 1, 15);
        LocalTime expectedTime = LocalTime.of(13, 45, 30);
        when(converter.convert(dateObj, LocalDate.class)).thenReturn(expectedDate);
        when(converter.convert(timeObj, LocalTime.class)).thenReturn(expectedTime);
        // WHEN
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // THEN
        assertEquals(LocalDateTime.of(expectedDate, expectedTime), result);
        verify(converter).convert(dateObj, LocalDate.class);
        verify(converter).convert(timeObj, LocalTime.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void toLocalDateTime_WithDateOnly_ReturnsMidnightDateTime() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        String dateKey = "date";
        Object dateObj = "2023-02-01";
        map.put(dateKey, dateObj);
        // No "time" entry – the method should default to LocalTime.MIDNIGHT.
        LocalDate expectedDate = LocalDate.of(2023, 2, 1);
        when(converter.convert(dateObj, LocalDate.class)).thenReturn(expectedDate);
        // WHEN
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // THEN
        assertEquals(LocalDateTime.of(expectedDate, LocalTime.MIDNIGHT), result);
        verify(converter).convert(dateObj, LocalDate.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void datePresentWithTimePresent_ReturnsCombinedDateTime() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        Object dateObj = "2023-05-10";
        Object timeObj = "14:30:45";
        map.put("date", dateObj);
        map.put("time", timeObj);
        LocalDate expectedDate = LocalDate.of(2023, 5, 10);
        LocalTime expectedTime = LocalTime.of(14, 30, 45);
        when(converter.convert(dateObj, LocalDate.class)).thenReturn(expectedDate);
        when(converter.convert(timeObj, LocalTime.class)).thenReturn(expectedTime);
        // WHEN
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // THEN
        assertEquals(LocalDateTime.of(expectedDate, expectedTime), result);
        verify(converter).convert(dateObj, LocalDate.class);
        verify(converter).convert(timeObj, LocalTime.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void datePresentWithTimeAbsent_ReturnsMidnight() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        Object dateObj = "2022-12-01";
        // no "time" entry
        map.put("date", dateObj);
        LocalDate expectedDate = LocalDate.of(2022, 12, 1);
        when(converter.convert(dateObj, LocalDate.class)).thenReturn(expectedDate);
        // WHEN
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // THEN
        assertEquals(LocalDateTime.of(expectedDate, LocalTime.MIDNIGHT), result);
        verify(converter).convert(dateObj, LocalDate.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void timeKeyPresentWithNullValue_UsesMidnightAndDoesNotCallConverterForTime() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        Object dateObj = "2023-01-01";
        map.put("date", dateObj);
        // explicit null
        map.put("time", null);
        LocalDate expectedDate = LocalDate.of(2023, 1, 1);
        when(converter.convert(dateObj, LocalDate.class)).thenReturn(expectedDate);
        // WHEN
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // THEN
        assertEquals(LocalDateTime.of(expectedDate, LocalTime.MIDNIGHT), result);
        verify(converter).convert(dateObj, LocalDate.class);
        // No interaction for the null time value
        verifyNoMoreInteractions(converter);
    }

    @Test
    void datePresentWithLocalDateObject_NoTime_ReturnsMidnight() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        LocalDate dateValue = LocalDate.of(2022, 6, 30);
        // already a LocalDate
        map.put("date", dateValue);
        // The converter is expected to simply return the supplied LocalDate unchanged.
        when(converter.convert(dateValue, LocalDate.class)).thenReturn(dateValue);
        // WHEN
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // THEN
        assertEquals(LocalDateTime.of(dateValue, LocalTime.MIDNIGHT), result);
        verify(converter).convert(dateValue, LocalDate.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void timePresentWithLocalTimeObject_CombinesConvertedDateAndSuppliedTime() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        Object dateObj = "2022-07-04";
        LocalTime timeValue = LocalTime.of(9, 15, 30);
        map.put("date", dateObj);
        // already a LocalTime
        map.put("time", timeValue);
        LocalDate expectedDate = LocalDate.of(2022, 7, 4);
        when(converter.convert(dateObj, LocalDate.class)).thenReturn(expectedDate);
        // Time is already a LocalTime, converter should return it unchanged.
        when(converter.convert(timeValue, LocalTime.class)).thenReturn(timeValue);
        // WHEN
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // THEN
        assertEquals(LocalDateTime.of(expectedDate, timeValue), result);
        verify(converter).convert(dateObj, LocalDate.class);
        verify(converter).convert(timeValue, LocalTime.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void dateConversionReturnsNull_ThrowsNullPointerException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        Object dateObj = "invalid-date";
        Object timeObj = "12:00:00";
        map.put("date", dateObj);
        map.put("time", timeObj);
        when(converter.convert(dateObj, LocalDate.class)).thenReturn(null);
        when(converter.convert(timeObj, LocalTime.class)).thenReturn(LocalTime.NOON);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toLocalDateTime(map, converter));
        verify(converter).convert(dateObj, LocalDate.class);
        // Time conversion may still be called before the NPE occurs.
        verify(converter).convert(timeObj, LocalTime.class);
    }

    @Test
    void timeConversionReturnsNull_ThrowsNullPointerException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        Object dateObj = "2023-01-01";
        Object timeObj = "12:00:00";
        map.put("date", dateObj);
        map.put("time", timeObj);
        when(converter.convert(dateObj, LocalDate.class)).thenReturn(LocalDate.of(2023, 1, 1));
        when(converter.convert(timeObj, LocalTime.class)).thenReturn(null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toLocalDateTime(map, converter));
        verify(converter).convert(dateObj, LocalDate.class);
        verify(converter).convert(timeObj, LocalTime.class);
    }

    @Test
    void dateConversionThrowsDateTimeParseException_PropagatesException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        Object dateObj = "bad-date";
        Object timeObj = "12:00:00";
        map.put("date", dateObj);
        map.put("time", timeObj);
        when(converter.convert(dateObj, LocalDate.class)).thenThrow(DateTimeParseException.class);
        // Time conversion is irrelevant but we can stub it.
        when(converter.convert(timeObj, LocalTime.class)).thenReturn(LocalTime.NOON);
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> MapConversions.toLocalDateTime(map, converter));
        verify(converter).convert(dateObj, LocalDate.class);
        // No need to verify time conversion because exception occurs first.
    }

    @Test
    void timeConversionThrowsIllegalArgumentException_PropagatesException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        Object dateObj = "2023-03-15";
        Object timeObj = "bad-time";
        map.put("date", dateObj);
        map.put("time", timeObj);
        when(converter.convert(dateObj, LocalDate.class)).thenReturn(LocalDate.of(2023, 3, 15));
        when(converter.convert(timeObj, LocalTime.class)).thenThrow(IllegalArgumentException.class);
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalDateTime(map, converter));
        verify(converter).convert(dateObj, LocalDate.class);
        verify(converter).convert(timeObj, LocalTime.class);
    }

    @Test
    void dateConversionReturnsWrongType_ThrowsClassCastException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        Object dateObj = "2023-01-01";
        map.put("date", dateObj);
        // Converter returns an Integer instead of LocalDate.
        when(converter.convert(dateObj, LocalDate.class)).thenReturn(123);
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toLocalDateTime(map, converter));
        verify(converter).convert(dateObj, LocalDate.class);
    }

    @Test
    void timeConversionReturnsWrongType_ThrowsClassCastException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        Object dateObj = "2023-01-01";
        Object timeObj = "12:00:00";
        map.put("date", dateObj);
        map.put("time", timeObj);
        when(converter.convert(dateObj, LocalDate.class)).thenReturn(LocalDate.of(2023, 1, 1));
        // Converter returns a String instead of LocalTime.
        when(converter.convert(timeObj, LocalTime.class)).thenReturn("not-a-time");
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toLocalDateTime(map, converter));
        verify(converter).convert(dateObj, LocalDate.class);
        verify(converter).convert(timeObj, LocalTime.class);
    }

    @Test
    void dateAbsent_WithYearMonthDayKeys_DelegatesToFromMap_ReturnsMidnightDateTime() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("year", 2021);
        map.put("month", 7);
        map.put("day", 15);
        // No DATE key – the private fromMap should construct the LocalDateTime.
        // WHEN
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // THEN
        assertEquals(LocalDateTime.of(2021, 7, 15, 0, 0), result);
        // No interactions with the mock converter are expected because fromMap handles conversion internally.
        verifyNoInteractions(converter);
    }

    @Test
    void emptyMap_TriggersFromMapException_IllegalArgumentException() {
        // GIVEN
        Map<String, Object> emptyMap = new HashMap<>();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalDateTime(emptyMap, converter));
        verifyNoInteractions(converter);
    }

    @Test
    void dateKeyPresentButNullValue_DelegatesToFromMap_IllegalArgumentException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // explicit null value
        map.put("date", null);
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalDateTime(map, converter));
        verifyNoInteractions(converter);
    }

    @Test
    void nullMapInput_ThrowsNullPointerException() {
        // GIVEN
        Object nullMap = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toLocalDateTime(nullMap, converter));
        verifyNoInteractions(converter);
    }

    @Test
    void nonMapInput_ThrowsClassCastException() {
        // GIVEN
        String notAMap = "just a string";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toLocalDateTime(notAMap, converter));
        verifyNoInteractions(converter);
    }
}
