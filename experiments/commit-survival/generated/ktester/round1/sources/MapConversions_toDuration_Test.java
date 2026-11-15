package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toDuration_Test {

    @Mock
    Converter // mocked Converter used in the tests
    converter;

    // map that will be passed to toDuration
    private Map<String, Object> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test
        map = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions after each test
        reset(converter);
    }

    @Test
    void toDuration_WithSecondsAndNanos_ReturnsCorrectDuration() {
        // GIVEN
        long expectedSeconds = 5L;
        int expectedNanos = 123_456_789;
        map.put(MapConversions.SECONDS, expectedSeconds);
        map.put(MapConversions.NANOS, expectedNanos);
        // Stub the converter to return the supplied values
        when(converter.convert(expectedSeconds, long.class)).thenReturn(expectedSeconds);
        when(converter.convert(expectedNanos, int.class)).thenReturn(expectedNanos);
        // WHEN
        Duration result = MapConversions.toDuration(map, converter);
        // THEN
        assertEquals(Duration.ofSeconds(expectedSeconds, expectedNanos), result, "Duration should be built from seconds and nanos map entries");
        verify(converter).convert(expectedSeconds, long.class);
        verify(converter).convert(expectedNanos, int.class);
    }

    @Test
    void toDuration_WithSecondsOnly_NanosDefaultedToZero() {
        // GIVEN
        long expectedSeconds = 10L;
        map.put(MapConversions.SECONDS, expectedSeconds);
        // No NANOS entry – the map returns null for that key
        // Stub the converter: seconds conversion returns the value,
        // nanos conversion receives null and should yield 0
        when(converter.convert(expectedSeconds, long.class)).thenReturn(expectedSeconds);
        when(converter.convert(null, int.class)).thenReturn(0);
        // WHEN
        Duration result = MapConversions.toDuration(map, converter);
        // THEN
        assertEquals(Duration.ofSeconds(expectedSeconds, 0), result, "Duration should be built from seconds with nanos defaulted to zero");
        verify(converter).convert(expectedSeconds, long.class);
        verify(converter).convert(null, int.class);
    }

    @Test
    void testToDuration_NullFrom_ShouldThrowNullPointerException() {
        // GIVEN
        Map<String, Object> nullMap = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toDuration(nullMap, converter), "Passing a null 'from' should result in NullPointerException due to map.get call");
    }

    @Test
    void testToDuration_NonMapFrom_ShouldThrowClassCastException() {
        // GIVEN
        Object notAMap = "I am not a map";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toDuration(notAMap, converter), "Supplying a non‑Map object must cause a ClassCastException on the cast");
    }

    @Test
    void testToDuration_SecondsConversionFails_ShouldPropagateException() {
        // GIVEN
        map.put(MapConversions.SECONDS, "notANumber");
        map.put(MapConversions.NANOS, 0);
        // Simulate a conversion failure (e.g., NumberFormatException wrapped in RuntimeException)
        when(converter.convert("notANumber", long.class)).thenThrow(new NumberFormatException("Invalid number"));
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toDuration(map, converter), "Conversion exception for seconds must propagate");
        verify(converter).convert("notANumber", long.class);
    }

    @Test
    void testToDuration_NanosConversionFails_ShouldPropagateException() {
        // GIVEN
        long seconds = 5L;
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, "badNanos");
        when(converter.convert(seconds, long.class)).thenReturn(seconds);
        when(converter.convert("badNanos", int.class)).thenThrow(new IllegalArgumentException("Cannot convert nanos"));
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toDuration(map, converter), "Conversion exception for nanos must propagate");
        verify(converter).convert(seconds, long.class);
        verify(converter).convert("badNanos", int.class);
    }

    @Test
    void testToDuration_NanosIsNull_ConverterThrowsOnNull_ShouldPropagateException() {
        // GIVEN
        long seconds = 10L;
        map.put(MapConversions.SECONDS, seconds);
        map.put(MapConversions.NANOS, null);
        when(converter.convert(seconds, long.class)).thenReturn(seconds);
        when(converter.convert(null, int.class)).thenThrow(new NullPointerException("Nanos is null"));
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toDuration(map, converter), "Converter throwing on null nanos must propagate");
        verify(converter).convert(seconds, long.class);
        verify(converter).convert(null, int.class);
    }

    @Test
    void testToDuration_MissingSecondsKey_ShouldThrowIllegalArgumentException() {
        // GIVEN
        map.put(MapConversions.NANOS, 123);
        // No SECONDS entry – triggers the fallback to fromMap which validates required keys
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toDuration(map, converter), "Missing required 'seconds' key should cause IllegalArgumentException from fromMap");
    }

    @Test
    void testToDuration_SecondsKeyPresentButNull_ShouldThrowIllegalArgumentException() {
        // GIVEN
        map.put(MapConversions.SECONDS, null);
        map.put(MapConversions.NANOS, 0);
        // The presence of a null value leads the method to treat it as absent and delegate to fromMap
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toDuration(map, converter), "Null 'seconds' value should be considered missing and cause IllegalArgumentException");
    }
}
