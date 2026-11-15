package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toLocalDateTime_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance – the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /**
     * Verify that a map containing both {@code "date"} and {@code "time"} keys is correctly converted
     * to a {@link LocalDateTime} composed of the supplied date and time.
     */
    @Test
    void toLocalDateTime_WithDateAndTime_ReturnsCombinedLocalDateTime() {
        // given
        Map<String, Object> map = new HashMap<>();
        LocalDate date = LocalDate.of(2023, 5, 10);
        LocalTime time = LocalTime.of(14, 30, 15);
        map.put(MapConversions.DATE, date);
        map.put(MapConversions.TIME, time);
        // when
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(LocalDateTime.of(date, time), result, "The LocalDateTime should combine the supplied date and time");
    }

    /**
     * Verify that when the {@code "time"} key is absent the method defaults the time component
     * to {@link LocalTime#MIDNIGHT}.
     */
    @Test
    void toLocalDateTime_WithDateOnly_UsesMidnight() {
        // given
        Map<String, Object> map = new HashMap<>();
        LocalDate date = LocalDate.of(2021, 12, 31);
        map.put(MapConversions.DATE, date);
        // No TIME entry – should default to MIDNIGHT
        // when
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(LocalDateTime.of(date, LocalTime.MIDNIGHT), result, "When time is missing the method should use LocalTime.MIDNIGHT");
    }

    /**
     * Verify that a map containing both {@code "date"} and {@code "time"} keys is
     * correctly converted to a {@link LocalDateTime} composed of the supplied date
     * and time.
     */
    @Test
    void datePresentWithExplicitTime() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2023-05-10");
        map.put(MapConversions.TIME, "14:30:15");
        Converter converter = mock(Converter.class);
        when(converter.convert("2023-05-10", LocalDate.class)).thenReturn(LocalDate.of(2023, 5, 10));
        when(converter.convert("14:30:15", LocalTime.class)).thenReturn(LocalTime.of(14, 30, 15));
        // when
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // then
        assertEquals(LocalDateTime.of(2023, 5, 10, 14, 30, 15), result);
    }

    /**
     * Verify that when the {@code "time"} key is absent the method defaults the
     * time component to {@link LocalTime#MIDNIGHT}.
     */
    @Test
    void datePresentWithoutTime_UsesMidnight() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2021-12-01");
        Converter converter = mock(Converter.class);
        when(converter.convert("2021-12-01", LocalDate.class)).thenReturn(LocalDate.of(2021, 12, 1));
        // when
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // then
        assertEquals(LocalDateTime.of(2021, 12, 1, 0, 0), result);
    }

    /**
     * Verify that when the map does not contain a {@code "date"} entry the method
     * delegates to {@code fromMap} and uses the optional keys.
     */
    @Test
    void dateAbsent_UsesOptionalKeysViaFromMap() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put("date (optional)", "2022-07-15");
        map.put("time (optional)", "09:45:00");
        Converter converter = mock(Converter.class);
        when(converter.convert("2022-07-15", LocalDate.class)).thenReturn(LocalDate.of(2022, 7, 15));
        when(converter.convert("09:45:00", LocalTime.class)).thenReturn(LocalTime.of(9, 45, 0));
        // when
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // then
        assertEquals(LocalDateTime.of(2022, 7, 15, 9, 45, 0), result);
    }

    /**
     * Verify that a {@code ClassCastException} is thrown when the {@code from}
     * argument is not a {@link Map}.
     */
    @Test
    void fromIsNotMap_ThrowsClassCastException() {
        // given
        String notAMap = "not a map";
        Converter converter = mock(Converter.class);
        // when / then
        assertThrows(ClassCastException.class, () -> MapConversions.toLocalDateTime(notAMap, converter));
    }

    /**
     * Verify that a {@code null} {@code time} entry is treated as missing and
     * defaults to {@link LocalTime#MIDNIGHT}.
     */
    @Test
    void datePresentWithExplicitNullTime_UsesMidnight() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2022-03-15");
        map.put(MapConversions.TIME, null);
        Converter converter = mock(Converter.class);
        when(converter.convert("2022-03-15", LocalDate.class)).thenReturn(LocalDate.of(2022, 3, 15));
        // when
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // then
        assertEquals(LocalDateTime.of(2022, 3, 15, 0, 0), result);
    }

    /**
     * Verify that a {@code NullPointerException} is thrown when the converter
     * returns {@code null} for the date component.
     */
    @Test
    void converterReturnsNullForDate_ThrowsNPE() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "any");
        map.put(MapConversions.TIME, "12:00:00");
        Converter converter = mock(Converter.class);
        when(converter.convert("any", LocalDate.class)).thenReturn(null);
        when(converter.convert("12:00:00", LocalTime.class)).thenReturn(LocalTime.of(12, 0, 0));
        // when / then
        assertThrows(NullPointerException.class, () -> MapConversions.toLocalDateTime(map, converter));
    }

    /**
     * Verify that a {@code NullPointerException} is thrown when the converter
     * returns {@code null} for the time component.
     */
    @Test
    void converterReturnsNullForTime_ThrowsNPE() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2020-01-01");
        map.put(MapConversions.TIME, "any");
        Converter converter = mock(Converter.class);
        when(converter.convert("2020-01-01", LocalDate.class)).thenReturn(LocalDate.of(2020, 1, 1));
        when(converter.convert("any", LocalTime.class)).thenReturn(null);
        // when / then
        assertThrows(NullPointerException.class, () -> MapConversions.toLocalDateTime(map, converter));
    }

    /**
     * Verify that an exception thrown by the converter while converting the
     * {@code "date"} entry propagates unchanged.
     */
    @Test
    void converterThrowsDuringDateConversion_PropagatesException() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "bad-date");
        map.put(MapConversions.TIME, "10:00:00");
        Converter converter = mock(Converter.class);
        when(converter.convert("bad-date", LocalDate.class)).thenThrow(DateTimeParseException.class);
        when(converter.convert("10:00:00", LocalTime.class)).thenReturn(LocalTime.of(10, 0, 0));
        // when / then
        assertThrows(DateTimeParseException.class, () -> MapConversions.toLocalDateTime(map, converter));
    }

    /**
     * Verify that when the map already contains {@link LocalDate} and
     * {@link LocalTime} instances, the converter simply returns them unchanged.
     */
    @Test
    void dateAndTimeAlreadyLocalInstances_NoConversionNeeded() {
        // given
        LocalDate date = LocalDate.of(2024, 2, 29);
        LocalTime time = LocalTime.of(23, 59, 59);
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, date);
        map.put(MapConversions.TIME, time);
        Converter converter = mock(Converter.class);
        // Echo back the same instance for both conversions
        when(converter.convert(date, LocalDate.class)).thenReturn(date);
        when(converter.convert(time, LocalTime.class)).thenReturn(time);
        // when
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // then
        assertEquals(LocalDateTime.of(date, time), result);
    }

    /**
     * Verify that a {@code null} {@code "date"} entry triggers the fallback to
     * {@code fromMap} and that optional keys are honoured.
     */
    @Test
    void nullDateTriggersFallbackToFromMap() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, null);
        map.put("date (optional)", "2021-11-30");
        map.put("time (optional)", "08:15:00");
        Converter converter = mock(Converter.class);
        when(converter.convert("2021-11-30", LocalDate.class)).thenReturn(LocalDate.of(2021, 11, 30));
        when(converter.convert("08:15:00", LocalTime.class)).thenReturn(LocalTime.of(8, 15, 0));
        // when
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // then
        assertEquals(LocalDateTime.of(2021, 11, 30, 8, 15, 0), result);
    }

    /**
     * Verify that extra unrelated keys in the map do not affect conversion.
     */
    @Test
    void mapContainsUnrelatedKeys_ConversionSucceeds() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2023-07-04");
        map.put(MapConversions.TIME, "16:20:05");
        map.put("extra", "ignored");
        map.put("number", 42);
        Converter converter = mock(Converter.class);
        when(converter.convert("2023-07-04", LocalDate.class)).thenReturn(LocalDate.of(2023, 7, 4));
        when(converter.convert("16:20:05", LocalTime.class)).thenReturn(LocalTime.of(16, 20, 5));
        // when
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // then
        assertEquals(LocalDateTime.of(2023, 7, 4, 16, 20, 5), result);
    }

    /**
     * Verify that a non‑String {@code time} value (e.g., {@link Integer}) is
     * forwarded to the converter unchanged.
     */
    @Test
    void nonStringTimeValueHandledByConverter() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2025-12-31");
        // arbitrary integer
        map.put(MapConversions.TIME, 12345);
        Converter converter = mock(Converter.class);
        when(converter.convert("2025-12-31", LocalDate.class)).thenReturn(LocalDate.of(2025, 12, 31));
        when(converter.convert(12345, LocalTime.class)).thenReturn(LocalTime.of(3, 25, 45));
        // when
        LocalDateTime result = MapConversions.toLocalDateTime(map, converter);
        // then
        assertEquals(LocalDateTime.of(2025, 12, 31, 3, 25, 45), result);
    }

    /**
     * Verify that passing a {@code null} {@code from} argument results in a
     * {@link NullPointerException}.
     */
    @Test
    void nullFromArgument_ThrowsNPE() {
        // given
        Converter converter = mock(Converter.class);
        // when / then
        assertThrows(NullPointerException.class, () -> MapConversions.toLocalDateTime(null, converter));
    }

    /**
     * Verify that passing a {@code null} {@code converter} argument results in a
     * {@link NullPointerException}.
     */
    @Test
    void nullConverterArgument_ThrowsNPE() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2023-01-01");
        // when / then
        assertThrows(NullPointerException.class, () -> MapConversions.toLocalDateTime(map, null));
    }

    /**
     * Verify that a map with non‑String keys causes a {@code ClassCastException}
     * when the method attempts to cast it to {@code Map<String,Object>}.
     */
    @Test
    void mapWithNonStringKeys_ThrowsClassCastException() {
        // given
        Map<Integer, Object> badMap = new HashMap<>();
        badMap.put(1, "2023-01-01");
        badMap.put(2, "12:00:00");
        Converter converter = mock(Converter.class);
        // when / then
        assertThrows(ClassCastException.class, () -> MapConversions.toLocalDateTime(badMap, converter));
    }

    /**
     * Verify that a converter returning the wrong type for the date entry causes a
     * {@link ClassCastException}.
     */
    @Test
    void converterReturnsWrongTypeForDate_ThrowsClassCastException() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2023-01-01");
        map.put(MapConversions.TIME, "12:00:00");
        Converter converter = mock(Converter.class);
        when(converter.convert("2023-01-01", LocalDate.class)).thenReturn(// wrong type
        "not a LocalDate");
        when(converter.convert("12:00:00", LocalTime.class)).thenReturn(LocalTime.of(12, 0, 0));
        // when / then
        assertThrows(ClassCastException.class, () -> MapConversions.toLocalDateTime(map, converter));
    }

    /**
     * Verify that a converter returning the wrong type for the time entry causes a
     * {@link ClassCastException}.
     */
    @Test
    void converterReturnsWrongTypeForTime_ThrowsClassCastException() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2023-01-01");
        map.put(MapConversions.TIME, "12:00:00");
        Converter converter = mock(Converter.class);
        when(converter.convert("2023-01-01", LocalDate.class)).thenReturn(LocalDate.of(2023, 1, 1));
        when(converter.convert("12:00:00", LocalTime.class)).thenReturn(// wrong type
        12345);
        // when / then
        assertThrows(ClassCastException.class, () -> MapConversions.toLocalDateTime(map, converter));
    }

    /**
     * Verify that an exception thrown while converting the {@code "time"} entry
     * propagates unchanged.
     */
    @Test
    void converterThrowsDuringTimeConversion_PropagatesException() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, "2023-01-01");
        map.put(MapConversions.TIME, "bad-time");
        Converter converter = mock(Converter.class);
        when(converter.convert("2023-01-01", LocalDate.class)).thenReturn(LocalDate.of(2023, 1, 1));
        when(converter.convert("bad-time", LocalTime.class)).thenThrow(new IllegalArgumentException("Invalid time format"));
        // when / then
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalDateTime(map, converter));
    }

    /**
     * Verify that when required keys are missing and {@code fromMap} throws an
     * {@link IllegalArgumentException}, the exception propagates.
     *
     * <p>This test relies on the actual implementation of {@code fromMap} to
     * validate required keys. An empty map should trigger the exception.
     */
    @Test
    void fromMapThrowsWhenRequiredKeysMissing_PropagatesException() {
        // given
        Map<String, Object> emptyMap = new HashMap<>();
        // not used
        Converter converter = mock(Converter.class);
        // when / then
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalDateTime(emptyMap, converter));
    }
}
