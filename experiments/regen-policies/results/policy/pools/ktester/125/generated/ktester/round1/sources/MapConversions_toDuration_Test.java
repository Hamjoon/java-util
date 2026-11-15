package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toDuration_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise with a dummy real instance – it will be overwritten by a mock
        // in the tests that need fine‑grained control.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies the fast‑path test data.
     */
    private static Stream<Arguments> fastPathProvider() {
        return Stream.of(// 1. seconds and nanos as strings
        Arguments.of("Map contains both SECONDS and NANOS as strings", mapOf(MapConversions.SECONDS, "5", MapConversions.NANOS, "200"), (Consumer<Converter>) c -> {
            when(c.convert("5", long.class)).thenReturn(5L);
            when(c.convert("200", int.class)).thenReturn(200);
        }, "PT5.000000200S"), // 2. seconds only (nanos missing → null → 0)
        Arguments.of("Map contains SECONDS only, NANOS absent", mapOf(MapConversions.SECONDS, 10L), (Consumer<Converter>) c -> {
            when(c.convert(10L, long.class)).thenReturn(10L);
            when(c.convert(null, int.class)).thenReturn(0);
        }, "PT10S"), // 3. negative seconds with positive nanos
        Arguments.of("Negative seconds with positive nanos", mapOf(MapConversions.SECONDS, -3, MapConversions.NANOS, 500_000_000), (Consumer<Converter>) c -> {
            when(c.convert(-3, long.class)).thenReturn(-3L);
            when(c.convert(500_000_000, int.class)).thenReturn(500_000_000);
        }, "PT-2.5S"), // 4. zero seconds with nanos
        Arguments.of("Zero seconds with nanos only", mapOf(MapConversions.SECONDS, 0, MapConversions.NANOS, 123_456_789), (Consumer<Converter>) c -> {
            when(c.convert(0, long.class)).thenReturn(0L);
            when(c.convert(123_456_789, int.class)).thenReturn(123_456_789);
        }, "PT0.123456789S"), // 5. conversion exception on SECONDS
        Arguments.of("Converter throws NumberFormatException while converting SECONDS", mapOf(MapConversions.SECONDS, "notANumber", MapConversions.NANOS, 0), (Consumer<Converter>) c -> {
            when(c.convert("notANumber", long.class)).thenThrow(new NumberFormatException("bad number"));
        }, NumberFormatException.class), // 6. conversion exception on NANOS
        Arguments.of("Converter throws NumberFormatException while converting NANOS", mapOf(MapConversions.SECONDS, 3, MapConversions.NANOS, "bad"), (Consumer<Converter>) c -> {
            when(c.convert(3, long.class)).thenReturn(3L);
            when(c.convert("bad", int.class)).thenThrow(new NumberFormatException("bad nanos"));
        }, NumberFormatException.class), // 7. overflow – Duration.ofSeconds throws ArithmeticException
        Arguments.of("Overflow when seconds = Long.MAX_VALUE and nanos = 1_000_000_000", mapOf(MapConversions.SECONDS, Long.MAX_VALUE, MapConversions.NANOS, 1_000_000_000), (Consumer<Converter>) c -> {
            when(c.convert(Long.MAX_VALUE, long.class)).thenReturn(Long.MAX_VALUE);
            when(c.convert(1_000_000_000, int.class)).thenReturn(1_000_000_000);
        }, ArithmeticException.class));
    }

    /**
     * Small utility to build a map from an even number of arguments (key, value, …).
     */
    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void toDuration_WithSecondsAndNanos_ReturnsCorrectDuration() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // seconds as Long
        map.put(MapConversions.SECONDS, 5L);
        // nanos as Integer
        map.put(MapConversions.NANOS, 123_456_789);
        // WHEN
        Duration result = MapConversions.toDuration(map, converter);
        // THEN
        Duration expected = Duration.ofSeconds(5L, 123_456_789);
        assertEquals(expected, result, "Duration should be built from provided seconds and nanos");
    }

    @Test
    void toDuration_WithoutSeconds_ThrowsIllegalArgumentException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // Only nanos – no "seconds" entry, forcing the method to take the fallback path.
        map.put(MapConversions.NANOS, 500_000_000);
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toDuration(map, converter), "Missing 'seconds' key should cause an IllegalArgumentException");
    }

    /**
     * Parameterized test covering all cases where the source map contains a
     * {@code "seconds"} entry. The {@link Converter} is mocked to return the
     * values described in each test case.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("fastPathProvider")
    void toDuration_FastPath(String description, Map<String, Object> map, Consumer<Converter> converterStubbing, Object expected) {
        // Arrange – replace the real converter with a mock and apply the stubbing.
        converter = mock(Converter.class);
        converterStubbing.accept(converter);
        // Act
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception type.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> MapConversions.toDuration(map, converter), description);
        } else {
            // Expected a Duration.
            Duration result = MapConversions.toDuration(map, converter);
            Duration expectedDuration = Duration.parse((String) expected);
            assertEquals(expectedDuration, result, description);
        }
    }

    @Test
    void toDuration_Fallback_EpochMillis_ReturnsDuration() {
        // GIVEN – map with only epochMillis
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, 1500L);
        // Mock converter to return the same long value.
        converter = mock(Converter.class);
        when(converter.convert(1500L, long.class)).thenReturn(1500L);
        // WHEN
        Duration result = MapConversions.toDuration(map, converter);
        // THEN – 1500 ms = 1.5 seconds
        assertEquals(Duration.ofMillis(1500), result, "Fallback path should convert epochMillis to a Duration of 1.5 seconds");
    }

    @Test
    void toDuration_Fallback_NanosOnly_ThrowsIllegalArgumentException() {
        // GIVEN – map with only nanos (no seconds)
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.NANOS, 123_456);
        // No special stubbing needed – the converter is not used because the
        // fallback logic will fail before any conversion.
        // Use a real Converter to avoid NullPointerException.
        converter = new Converter(new ConverterOptions());
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toDuration(map, converter), "Fallback should fail when required SECONDS key is missing");
    }

    @Test
    void toDuration_NullMap_ThrowsNullPointerException() {
        // GIVEN
        Object nullMap = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toDuration(nullMap, converter), "Passing null as the source map should throw NullPointerException");
    }

    @Test
    void toDuration_NonMapInput_ThrowsClassCastException() {
        // GIVEN – a plain String instead of a Map
        Object notAMap = "I am not a map";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toDuration(notAMap, converter), "Passing a non‑Map object should cause a ClassCastException");
    }

    @Test
    void toDuration_NullConverter_ThrowsNullPointerException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, 5L);
        // No mock – converter is null
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toDuration(map, nullConverter), "A null Converter should cause a NullPointerException on first use");
    }
}
