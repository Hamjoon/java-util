package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toMap_Test {

    private static Stream<Arguments> provideNormalAndPrecisionCases() {
        return Stream.of(Arguments.of("Typical time without nanos", LocalTime.of(13, 45, 30), "13:45:30"), Arguments.of("Midnight", LocalTime.of(0, 0), "00:00"), Arguments.of("Maximum LocalTime", LocalTime.MAX, "23:59:59.999999999"), Arguments.of("Hour & minute only", LocalTime.of(9, 5), "09:05"), Arguments.of("With nanoseconds", LocalTime.of(12, 30, 15, 123_456_789), "12:30:15.123456789"), Arguments.of("Last second of day, zero nanos", LocalTime.of(23, 59, 59, 0), "23:59:59"));
    }

    /**
     * Verify that {@code toMap} returns a map containing a single entry
     * with the key defined in {@link MapConversions#TIME} and the string
     * representation of the supplied {@link LocalTime}.
     */
    @Test
    void toMap_returnsMapWithTimeKey() {
        // given
        LocalTime source = LocalTime.of(13, 45, 30, 123_456_789);
        // the method does not use the converter
        Converter converter = null;
        // when
        Map<String, Object> result = LocalTimeConversions.toMap(source, converter);
        // then
        assertNotNull(result, "Resulting map must not be null");
        // MapConversions.TIME is a public constant; if it is not accessible we fall back to the literal "time"
        String expectedKey = MapConversions.TIME != null ? MapConversions.TIME : "time";
        assertTrue(result.containsKey(expectedKey), () -> "Result map should contain the key '" + expectedKey + "'");
        assertEquals(source.toString(), result.get(expectedKey), "The value associated with the key should be the LocalTime's string representation");
        assertEquals(1, result.size(), "Result map should contain exactly one entry");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideNormalAndPrecisionCases")
    void toMap_normalAndPrecisionConversions(String description, LocalTime input, String expectedString) {
        // given
        Converter converter = null;
        // when
        Map<String, Object> map = LocalTimeConversions.toMap(input, converter);
        // then
        assertNotNull(map, "Map must not be null");
        String key = MapConversions.TIME != null ? MapConversions.TIME : "time";
        assertEquals(1, map.size(), "Map should contain exactly one entry");
        assertTrue(map.containsKey(key), "Map must contain the TIME key");
        assertEquals(expectedString, map.get(key), "Map value should be the expected string representation of the LocalTime");
    }

    @Test
    void toMap_nullSource_throwsNullPointerException() {
        // given
        Object nullSource = null;
        Converter converter = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toMap(nullSource, converter), "Passing a null source should throw NullPointerException");
    }

    @Test
    void toMap_wrongType_throwsClassCastException() {
        // given
        // a String, not a LocalTime
        Object notALocalTime = "12:00";
        Converter converter = null;
        // when / then
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toMap(notALocalTime, converter), "Passing a non‑LocalTime object should throw ClassCastException");
    }

    @Test
    void toMap_ignoresNonNullConverter(@Mock Converter mockConverter) {
        // given
        LocalTime source = LocalTime.of(15, 20, 45);
        // mockConverter is a Mockito mock; we do not set any behaviour because the method never calls it
        // when
        Map<String, Object> result = LocalTimeConversions.toMap(source, mockConverter);
        // then
        assertNotNull(result);
        String key = MapConversions.TIME != null ? MapConversions.TIME : "time";
        assertEquals(source.toString(), result.get(key), "Even with a non‑null Converter, the map value must be the LocalTime string");
    }

    @Test
    void toMap_mapIntegrity_singleEntry() {
        // given
        // 4 nanoseconds
        LocalTime source = LocalTime.of(1, 2, 3, 4);
        Converter converter = null;
        // should be "01:02:03.000000004"
        String expectedValue = source.toString();
        // when
        Map<String, Object> map = LocalTimeConversions.toMap(source, converter);
        // then
        assertNotNull(map);
        String key = MapConversions.TIME != null ? MapConversions.TIME : "time";
        assertEquals(1, map.size(), "Map must contain exactly one entry");
        assertTrue(map.containsKey(key), "Map must contain the TIME key");
        assertEquals(expectedValue, map.get(key), "Map value must match the LocalTime's string representation");
    }
}
