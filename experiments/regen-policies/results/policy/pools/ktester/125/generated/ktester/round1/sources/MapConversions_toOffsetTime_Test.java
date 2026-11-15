package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toOffsetTime_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    /**
     * The real Converter used for most positive tests.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the real Converter is lightweight and provides the needed conversions.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validOffsetProvider() {
        return Stream.of(// 14:30:15.123456789 with -05:00 offset
        arguments(mapOf(MapConversions.HOUR, 14, MapConversions.MINUTE, 30, MapConversions.SECOND, 15, MapConversions.NANOS, 123456789, MapConversions.OFFSET_HOUR, -5, MapConversions.OFFSET_MINUTE, 0), OffsetTime.of(14, 30, 15, 123456789, ZoneOffset.ofHoursMinutes(-5, 0))), // Midnight with +02:45 offset
        arguments(mapOf(MapConversions.HOUR, 0, MapConversions.MINUTE, 0, MapConversions.SECOND, 0, MapConversions.NANOS, 0, MapConversions.OFFSET_HOUR, 2, MapConversions.OFFSET_MINUTE, 45), OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHoursMinutes(2, 45))));
    }

    /**
     * Small convenience for creating a map literal.
     */
    private static Map<String, Object> mapOf(Object... keyVals) {
        if (keyVals.length % 2 != 0) {
            throw new IllegalArgumentException("Even number of arguments required");
        }
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            map.put((String) keyVals[i], keyVals[i + 1]);
        }
        return map;
    }

    /**
     * Verifies that when a map contains the hour/minute (and optional second, nanos,
     * offsetHour, offsetMinute) keys, {@code MapConversions.toOffsetTime} builds the
     * correct {@link OffsetTime} instance.
     */
    @Test
    void toOffsetTime_WithHourMinuteSecondNanosAndOffset_ReturnsCorrectOffsetTime() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 10);
        map.put(MapConversions.MINUTE, 20);
        map.put(MapConversions.SECOND, 30);
        map.put(MapConversions.NANOS, 0);
        map.put(MapConversions.OFFSET_HOUR, 2);
        map.put(MapConversions.OFFSET_MINUTE, 30);
        OffsetTime result = MapConversions.toOffsetTime(map, converter);
        OffsetTime expected = OffsetTime.of(10, 20, 30, 0, ZoneOffset.ofHoursMinutes(2, 30));
        assertEquals(expected, result, "OffsetTime should be built from map entries");
    }

    /**
     * Verifies that when a map contains the {@code "time"} key, the method delegates
     * to {@link Converter#convert(Object, Class)} which can parse a string representation
     * of an {@link OffsetTime}.
     */
    @Test
    void toOffsetTime_WithTimeKey_DelegatesToConverter() {
        Map<String, Object> map = new HashMap<>();
        String timeString = "12:34:56+02:00";
        map.put(MapConversions.TIME, timeString);
        OffsetTime result = MapConversions.toOffsetTime(map, converter);
        OffsetTime expected = OffsetTime.parse(timeString);
        assertEquals(expected, result, "OffsetTime should be parsed from the 'time' entry");
    }

    @ParameterizedTest(name = "[{index}] hour/minute/second/nanos + offset → {1}")
    @MethodSource("validOffsetProvider")
    void toOffsetTime_WithValidFields_ReturnsCorrectOffsetTime(Map<String, Object> map, OffsetTime expected) {
        OffsetTime result = MapConversions.toOffsetTime(map, realConverter);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetTime_WithNegativeOffset_ReturnsCorrectOffsetTime() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 22);
        map.put(MapConversions.MINUTE, 10);
        map.put(MapConversions.SECOND, 5);
        map.put(MapConversions.NANOS, 500);
        map.put(MapConversions.OFFSET_HOUR, -3);
        map.put(MapConversions.OFFSET_MINUTE, -45);
        OffsetTime result = MapConversions.toOffsetTime(map, realConverter);
        OffsetTime expected = OffsetTime.of(22, 10, 5, 500, ZoneOffset.ofHoursMinutes(-3, -45));
        assertEquals(expected, result);
    }

    @Test
    void toOffsetTime_WithZeroOffset_ReturnsUTC() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 6);
        map.put(MapConversions.MINUTE, 30);
        map.put(MapConversions.SECOND, 0);
        map.put(MapConversions.NANOS, 0);
        map.put(MapConversions.OFFSET_HOUR, 0);
        map.put(MapConversions.OFFSET_MINUTE, 0);
        OffsetTime result = MapConversions.toOffsetTime(map, realConverter);
        OffsetTime expected = OffsetTime.of(6, 30, 0, 0, ZoneOffset.UTC);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetTime_WithTimeKey_StringIsParsedByConverter() {
        Map<String, Object> map = new HashMap<>();
        String timeString = "12:34:56+02:00";
        map.put(MapConversions.TIME, timeString);
        OffsetTime result = MapConversions.toOffsetTime(map, realConverter);
        OffsetTime expected = OffsetTime.parse(timeString);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetTime_WithTimeKey_AlreadyOffsetTimeInstance() {
        Map<String, Object> map = new HashMap<>();
        OffsetTime original = OffsetTime.of(13, 45, 0, 0, ZoneOffset.ofHours(2));
        map.put(MapConversions.TIME, original);
        OffsetTime result = MapConversions.toOffsetTime(map, realConverter);
        assertSame(original, result, "When the value is already an OffsetTime the same instance should be returned");
    }

    @Test
    void toOffsetTime_MissingOffsetFields_ThrowsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 10);
        map.put(MapConversions.MINUTE, 20);
        map.put(MapConversions.SECOND, 30);
        map.put(MapConversions.NANOS, 0);
        // offsetHour / offsetMinute omitted
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetTime(map, realConverter));
        // The message comes from fromMap – we only assert the type.
        assertNotNull(ex.getMessage());
    }

    @Test
    void toOffsetTime_InvalidOffsetHour_ThrowsIllegalArgumentExceptionWithMessage() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 12);
        map.put(MapConversions.MINUTE, 0);
        map.put(MapConversions.SECOND, 0);
        map.put(MapConversions.NANOS, 0);
        // invalid
        map.put(MapConversions.OFFSET_HOUR, 25);
        map.put(MapConversions.OFFSET_MINUTE, 0);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetTime(map, realConverter));
        assertTrue(ex.getMessage().contains("Offset 'hour' and 'minute' are not correct"));
    }

    @Test
    void toOffsetTime_MismatchedOffsetSign_ThrowsIllegalArgumentExceptionWithMessage() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 9);
        map.put(MapConversions.MINUTE, 15);
        map.put(MapConversions.SECOND, 0);
        map.put(MapConversions.NANOS, 0);
        map.put(MapConversions.OFFSET_HOUR, 3);
        // illegal combination
        map.put(MapConversions.OFFSET_MINUTE, -30);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetTime(map, realConverter));
        assertTrue(ex.getMessage().contains("Offset 'hour' and 'minute' are not correct"));
    }

    @Test
    void toOffsetTime_EmptyMap_ThrowsIllegalArgumentException() {
        Map<String, Object> empty = new HashMap<>();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetTime(empty, realConverter));
        assertNotNull(ex.getMessage());
    }

    @Test
    void toOffsetTime_NullMap_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toOffsetTime(null, realConverter));
    }

    @Test
    void toOffsetTime_NonMapFrom_ThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> MapConversions.toOffsetTime("just a string", realConverter));
    }

    @Test
    void toOffsetTime_NullConverter_ThrowsNullPointerException() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 10);
        map.put(MapConversions.MINUTE, 20);
        map.put(MapConversions.SECOND, 30);
        map.put(MapConversions.NANOS, 0);
        assertThrows(NullPointerException.class, () -> MapConversions.toOffsetTime(map, null));
    }

    @Test
    void toOffsetTime_MissingSecondAndNanos_ConverterThrowsOnNull() {
        // Prepare a spy that throws NPE when converting a null value to int
        Converter spy = spy(realConverter);
        doThrow(new NullPointerException("null int conversion")).when(spy).convert(isNull(), eq(int.class));
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 12);
        map.put(MapConversions.MINUTE, 15);
        // second and nanos are omitted → null
        assertThrows(NullPointerException.class, () -> MapConversions.toOffsetTime(map, spy));
    }

    @Test
    void toOffsetTime_PartialOffsetOnlyHour_ThrowsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 9);
        map.put(MapConversions.MINUTE, 45);
        map.put(MapConversions.SECOND, 0);
        map.put(MapConversions.NANOS, 0);
        // offsetMinute missing
        map.put(MapConversions.OFFSET_HOUR, 2);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetTime(map, realConverter));
        assertNotNull(ex.getMessage());
    }

    @Test
    void toOffsetTime_NonIntegerOffsetFields_ConverterThrowsNumberFormatException() {
        // Mock a converter that attempts Integer.parseInt on strings and therefore
        // throws NumberFormatException for the offset fields.
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(int.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            if (arg instanceof String) {
                // will throw NumberFormatException
                return Integer.parseInt((String) arg);
            }
            // delegate to real converter for everything else
            return realConverter.convert(arg, int.class);
        });
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 8);
        map.put(MapConversions.MINUTE, 20);
        map.put(MapConversions.SECOND, 0);
        map.put(MapConversions.NANOS, 0);
        // invalid string
        map.put(MapConversions.OFFSET_HOUR, "+3");
        // invalid string
        map.put(MapConversions.OFFSET_MINUTE, "15");
        assertThrows(NumberFormatException.class, () -> MapConversions.toOffsetTime(map, mock));
    }

    @Test
    void toOffsetTime_InvalidHourValue_ThrowsDateTimeException() {
        Map<String, Object> map = new HashMap<>();
        // invalid hour
        map.put(MapConversions.HOUR, 25);
        map.put(MapConversions.MINUTE, 10);
        map.put(MapConversions.SECOND, 0);
        map.put(MapConversions.NANOS, 0);
        map.put(MapConversions.OFFSET_HOUR, 0);
        map.put(MapConversions.OFFSET_MINUTE, 0);
        assertThrows(DateTimeException.class, () -> MapConversions.toOffsetTime(map, realConverter));
    }

    @Test
    void toOffsetTime_InvalidMinuteValue_ThrowsDateTimeException() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 10);
        // invalid minute
        map.put(MapConversions.MINUTE, 61);
        map.put(MapConversions.SECOND, 0);
        map.put(MapConversions.NANOS, 0);
        map.put(MapConversions.OFFSET_HOUR, 0);
        map.put(MapConversions.OFFSET_MINUTE, 0);
        assertThrows(DateTimeException.class, () -> MapConversions.toOffsetTime(map, realConverter));
    }

    @Test
    void toOffsetTime_InvalidSecondValue_ThrowsDateTimeException() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 12);
        map.put(MapConversions.MINUTE, 30);
        // invalid second
        map.put(MapConversions.SECOND, 70);
        map.put(MapConversions.NANOS, 0);
        map.put(MapConversions.OFFSET_HOUR, 0);
        map.put(MapConversions.OFFSET_MINUTE, 0);
        assertThrows(DateTimeException.class, () -> MapConversions.toOffsetTime(map, realConverter));
    }

    @Test
    void toOffsetTime_TimeKeyConversionFails_PropagatesIllegalArgumentException() {
        // Mock a converter that throws IllegalArgumentException when asked to convert
        // to OffsetTime.class.
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(OffsetTime.class))).thenThrow(new IllegalArgumentException("cannot convert"));
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, "not-an-offset-time");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapConversions.toOffsetTime(map, mock));
        assertEquals("cannot convert", ex.getMessage());
    }
}
