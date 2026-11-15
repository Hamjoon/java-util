package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toOffsetTime_Test {

    @Mock
    Converter // mocked conversion utility
    converter;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock state before each test
        reset(converter);
    }

    /**
     * Configures the mock {@code converter} so that {@code convert(..., int.class)}
     * works with Numbers, numeric Strings and {@code null} (treated as 0).
     */
    private void stubIntConversion() {
        when(converter.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg == null) {
                return 0;
            }
            if (arg instanceof Number) {
                return ((Number) arg).intValue();
            }
            if (arg instanceof String) {
                return Integer.parseInt(((String) arg).trim());
            }
            throw new IllegalArgumentException("Unsupported type for int conversion: " + arg);
        });
    }

    /**
     * Configures the mock {@code converter} so that {@code convert(..., OffsetTime.class)}
     * parses a String using {@link OffsetTime#parse(CharSequence)} or returns the
     * supplied {@code OffsetTime} unchanged.
     */
    private void stubOffsetTimeConversion() {
        when(converter.convert(any(), eq(OffsetTime.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof String) {
                return OffsetTime.parse((String) arg);
            }
            return (OffsetTime) arg;
        });
    }

    private static Stream<Arguments> fullTimeAndOffsetProvider() {
        return Stream.of(// numeric values as integers
        Arguments.of(mapOf(MapConversions.HOUR, 13, MapConversions.MINUTE, 45, MapConversions.SECOND, 30, MapConversions.NANOS, 123_456_789, MapConversions.OFFSET_HOUR, -4, MapConversions.OFFSET_MINUTE, 0), OffsetTime.of(13, 45, 30, 123_456_789, ZoneOffset.ofHoursMinutes(-4, 0)), "All components as integers with a negative offset"), // numeric values supplied as strings (including a leading '+')
        Arguments.of(mapOf(MapConversions.HOUR, "9", MapConversions.MINUTE, "15", MapConversions.SECOND, "5", MapConversions.NANOS, "500000000", MapConversions.OFFSET_HOUR, "+2", MapConversions.OFFSET_MINUTE, "30"), OffsetTime.of(9, 15, 5, 500_000_000, ZoneOffset.ofHoursMinutes(2, 30)), "All components as numeric strings with a positive offset"), // null second/nanos – the mock converter treats null as 0
        Arguments.of(mapOf(MapConversions.HOUR, 14, MapConversions.MINUTE, 0, MapConversions.SECOND, null, MapConversions.NANOS, null, MapConversions.OFFSET_HOUR, -2, MapConversions.OFFSET_MINUTE, 15), OffsetTime.of(14, 0, 0, 0, ZoneOffset.ofHoursMinutes(-2, 15)), "Null second and nanos are interpreted as 0"));
    }

    /**
     * When the map contains hour, minute, second, nanos and a valid offset,
     * the method should return an {@link OffsetTime} built from those values.
     */
    @Test
    void shouldCreateOffsetTimeFromAllComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 10);
        map.put(MapConversions.MINUTE, 30);
        map.put(MapConversions.SECOND, 45);
        map.put(MapConversions.NANOS, 123_456_789);
        map.put(MapConversions.OFFSET_HOUR, -5);
        map.put(MapConversions.OFFSET_MINUTE, 0);
        // The converter is asked to convert each numeric entry to int.
        // A generic Answer converts the supplied Number to its int value.
        when(converter.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            return ((Number) arg).intValue();
        });
        OffsetTime result = MapConversions.toOffsetTime(map, converter);
        OffsetTime expected = OffsetTime.of(10, 30, 45, 123_456_789, ZoneOffset.ofHours(-5));
        assertEquals(expected, result);
    }

    /**
     * When the map does not contain hour/minute but does contain a {@code time} entry,
     * the method should delegate the conversion of that entry to the {@link Converter}.
     */
    @Test
    void shouldDelegateToConverterWhenTimeKeyPresent() {
        Map<String, Object> map = new HashMap<>();
        // the concrete type does not matter; we stub the conversion
        Object timeValue = "12:15:30+02:00";
        map.put(MapConversions.TIME, timeValue);
        OffsetTime expected = OffsetTime.of(12, 15, 30, 0, ZoneOffset.ofHours(2));
        when(converter.convert(timeValue, OffsetTime.class)).thenReturn(expected);
        OffsetTime result = MapConversions.toOffsetTime(map, converter);
        assertEquals(expected, result);
        // Verify that no integer conversion was attempted (hour/minute path not taken)
        verify(converter, never()).convert(any(), eq(int.class));
    }

    /**
     * If hour/minute are present but the offset values are invalid, the method must
     * throw an {@link IllegalArgumentException}.
     */
    @Test
    void shouldThrowWhenOffsetIsInvalid() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 9);
        map.put(MapConversions.MINUTE, 15);
        map.put(MapConversions.SECOND, 0);
        map.put(MapConversions.NANOS, 0);
        // Invalid offset: hour = 25 (outside the -18..+18 range allowed by ZoneOffset)
        map.put(MapConversions.OFFSET_HOUR, 25);
        map.put(MapConversions.OFFSET_MINUTE, 0);
        when(converter.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            return ((Number) arg).intValue();
        });
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetTime(map, converter));
        assertTrue(ex.getMessage().contains("Offset 'hour' and 'minute' are not correct"));
    }

    @Test
    void shouldThrowWhenOffsetHourIsInvalid() {
        Map<String, Object> map = mapOf(MapConversions.HOUR, 10, MapConversions.MINUTE, 20, MapConversions.SECOND, 0, MapConversions.NANOS, 0, // invalid hour (> +18)
        MapConversions.OFFSET_HOUR, // invalid hour (> +18)
        25, MapConversions.OFFSET_MINUTE, 0);
        stubIntConversion();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetTime(map, converter));
        assertTrue(ex.getMessage().contains("Offset 'hour' and 'minute' are not correct"));
    }

    @Test
    void shouldThrowWhenOffsetMinuteIsInvalid() {
        Map<String, Object> map = mapOf(MapConversions.HOUR, 12, MapConversions.MINUTE, 30, MapConversions.SECOND, 0, MapConversions.NANOS, 0, MapConversions.OFFSET_HOUR, 3, MapConversions.OFFSET_MINUTE, // invalid minute (> 59)
        75);
        stubIntConversion();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetTime(map, converter));
        assertTrue(ex.getMessage().contains("Offset 'hour' and 'minute' are not correct"));
    }

    @Test
    void shouldThrowWhenHourOrMinuteMissingAndNoTimeKey() {
        Map<String, Object> map = mapOf(MapConversions.SECOND, 10, MapConversions.NANOS, 0, MapConversions.OFFSET_HOUR, 1, MapConversions.OFFSET_MINUTE, 0);
        // No special stubbing needed – the method will reach fromMap and fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetTime(map, converter));
        // The exact message comes from fromMap; we only assert that an exception is thrown
        assertNotNull(ex.getMessage());
    }

    @Test
    void shouldThrowWhenHourMinutePresentButOffsetMissing() {
        Map<String, Object> map = mapOf(MapConversions.HOUR, 8, MapConversions.MINUTE, 30);
        // No offset entries – should fall back to fromMap and fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetTime(map, converter));
        assertNotNull(ex.getMessage());
    }

    @SafeVarargs
    private static Map<String, Object> mapOf(Object... keyVals) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            map.put((String) keyVals[i], keyVals[i + 1]);
        }
        return map;
    }
}
