package com.cedarsoftware.util.convert;

import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapConversions_toDuration_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter converter;

//     @BeforeAll
    static void setupBeforeAll() {
        // Initialise the Converter with default options – this is required for the
        // MapConversions methods that delegate to the Converter.
//         converter = new Converter(new ConverterOptions());
    }

//     @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the
        // test class tidy.
        converter = null;
    }

    static Stream<Arguments> numericToDurationProvider() {
        return Stream.of(Arguments.of("Long seconds", Long.valueOf(3600L), Duration.ofSeconds(3600)), Arguments.of("Integer seconds", Integer.valueOf(75), Duration.ofSeconds(75)), Arguments.of("Short seconds", Short.valueOf((short) 15), Duration.ofSeconds(15)), Arguments.of("Double with fraction", Double.valueOf(90.5), Duration.ofSeconds(90).plusNanos(500_000_000)), Arguments.of("Float with fraction", Float.valueOf(3.2f), Duration.ofSeconds(3).plusNanos(200_000_000)), Arguments.of("BigDecimal with fraction", new BigDecimal("5.125"), Duration.ofSeconds(5).plusNanos(125_000_000)), Arguments.of("BigInteger", new BigInteger("12345678901234567890"), Duration.ofSeconds(new BigInteger("12345678901234567890").longValue())), Arguments.of("AtomicLong", new AtomicLong(250L), Duration.ofSeconds(250)), Arguments.of("AtomicInteger", new AtomicInteger(99), Duration.ofSeconds(99)));
    }

    static Stream<Arguments> isoStringProvider() {
        return Stream.of(Arguments.of("PT2H30M15.250S", Duration.parse("PT2H30M15.250S")), Arguments.of("PT3M45.5S", Duration.parse("PT3M45.5S")));
    }

    /**
     * When the source object is already a {@link Duration}, the same instance should be returned.
     */
//     @Test
//     void toDuration_givenDuration_returnsSameInstance() {
//         Duration source = Duration.ofHours(2).plusMinutes(30);
//         Duration result = MapConversions.toDuration(source, converter);
//         assertSame(source, result, "Expected the original Duration instance to be returned unchanged");
//     }

    /**
     * A map containing a {@code "duration"} key with an ISO‑8601 string should be converted.
     */
//     @Test
//     void toDuration_givenMapWithDurationKey_convertsString() {
//         Map<String, Object> map = new HashMap<>();
//         // 1 hour 15 minutes
//         map.put("duration", "PT1H15M");
//         Duration result = MapConversions.toDuration(map, converter);
//         assertNotNull(result, "Resulting Duration should not be null");
//         assertEquals(Duration.ofHours(1).plusMinutes(15), result, "Duration parsed from ISO‑8601 string did not match expected value");
//     }

    /**
     * A map containing a {@code "value"} key with an ISO‑8601 string should also be converted.
     */
//     @Test
//     void toDuration_givenMapWithValueKey_convertsString() {
//         Map<String, Object> map = new HashMap<>();
//         // 45 seconds
//         map.put("value", "PT45S");
//         Duration result = MapConversions.toDuration(map, converter);
//         assertNotNull(result, "Resulting Duration should not be null");
//         assertEquals(Duration.ofSeconds(45), result, "Duration parsed from ISO‑8601 string did not match expected value");
//     }

    /**
     * A map containing the short key {@code "_v"} with a numeric value (seconds) should be converted.
     */
//     @Test
//     void toDuration_givenMapWithShortKey_convertsNumber() {
//         Map<String, Object> map = new HashMap<>();
//         // 120 seconds
//         map.put("_v", 120L);
//         Duration result = MapConversions.toDuration(map, converter);
//         assertNotNull(result, "Resulting Duration should not be null");
//         assertEquals(Duration.ofSeconds(120), result, "Duration created from numeric seconds did not match expected value");
//     }

    /**
     * Passing {@code null} as the source should result in {@code null}.
     */
//     @Test
//     void toDuration_givenNull_returnsNull() {
//         Duration result = MapConversions.toDuration(null, converter);
//         assertNull(result, "Converting a null source should yield null");
//     }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @MethodSource("numericToDurationProvider")
    void toDuration_numericValues(String description, Object input, Duration expected) {
        Duration result = MapConversions.toDuration(input, converter);
        assertNotNull(result);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] parse \"{0}\"")
    @MethodSource("isoStringProvider")
    void toDuration_isoString(String iso, Duration expected) {
        Duration result = MapConversions.toDuration(iso, converter);
        assertNotNull(result);
        assertEquals(expected, result);
    }

//     @Test
//     void toDuration_mapWithDurationKeyContainingDuration() {
//         Map<String, Object> map = Map.of("duration", Duration.ofMinutes(15));
//         Duration result = MapConversions.toDuration(map, converter);
//         assertEquals(Duration.ofMinutes(15), result);
//     }

//     @Test
//     void toDuration_mapWithDurationKeyContainingString() {
//         Map<String, Object> map = Map.of("duration", "PT3M45.5S");
//         Duration result = MapConversions.toDuration(map, converter);
//         assertEquals(Duration.parse("PT3M45.5S"), result);
//     }

//     @Test
//     void toDuration_mapWithValueKeyContainingNumber() {
//         Map<String, Object> map = Map.of("value", 45);
//         Duration result = MapConversions.toDuration(map, converter);
//         assertEquals(Duration.ofSeconds(45), result);
//     }

//     @Test
//     void toDuration_mapWithValueKeyContainingDouble() {
//         Map<String, Object> map = Map.of("value", Double.valueOf(12.75));
//         Duration result = MapConversions.toDuration(map, converter);
//         assertEquals(Duration.ofSeconds(12).plusNanos(750_000_000), result);
//     }

//     @Test
//     void toDuration_mapWithLegacyKeyVContainingNumber() {
//         Map<String, Object> map = Map.of("_v", Long.valueOf(42L));
//         Duration result = MapConversions.toDuration(map, converter);
//         assertEquals(Duration.ofSeconds(42), result);
//     }

//     @Test
//     void toDuration_mapWithLegacyKeyVContainingString() {
//         Map<String, Object> map = Map.of("_v", "PT10S");
//         Duration result = MapConversions.toDuration(map, converter);
//         assertEquals(Duration.ofSeconds(10), result);
//     }

//     @Test
//     void toDuration_mapWithBothDurationAndValue_keysDurationWins() {
//         Map<String, Object> map = Map.of("duration", Duration.ofSeconds(20), "value", 9999);
//         Duration result = MapConversions.toDuration(map, converter);
//         assertEquals(Duration.ofSeconds(20), result);
//     }

//     @Test
//     void toDuration_mapWithNullDurationValue_returnsNull() {
//         Map<String, Object> map = Map.of("duration", null);
//         Duration result = MapConversions.toDuration(map, converter);
//         assertNull(result);
//     }

//     @Test
//     void toDuration_mapWithNullValue_returnsNull() {
//         Map<String, Object> map = Map.of("value", null);
//         Duration result = MapConversions.toDuration(map, converter);
//         assertNull(result);
//     }

//     @Test
//     void toDuration_mapWithNullLegacyV_returnsNull() {
//         Map<String, Object> map = Map.of("_v", null);
//         Duration result = MapConversions.toDuration(map, converter);
//         assertNull(result);
//     }

//     @Test
//     void toDuration_mapWithNoRecognisedKeys_fallbackConversion() {
//         // The map itself does not have duration keys, so the dispatcher tries the generic conversion.
//         // In the current implementation the generic conversion treats the map as a bean and extracts
//         // the first value, which is the ISO‑8601 string.
//         Map<String, Object> map = Map.of("foo", "PT5M");
//         Duration result = MapConversions.toDuration(map, converter);
//         assertEquals(Duration.ofMinutes(5), result);
//     }

//     @Test
//     void toDuration_unsupportedString_throwsIllegalArgumentException() {
//         String bad = "not-a-duration";
//         assertThrows(IllegalArgumentException.class, () -> MapConversions.toDuration(bad, converter), "Expected IllegalArgumentException for unparsable string");
//     }

//     @Test
//     void toDuration_mapWithBadDurationString_throwsIllegalArgumentException() {
//         Map<String, Object> map = Map.of("duration", "not-a-duration");
//         assertThrows(IllegalArgumentException.class, () -> MapConversions.toDuration(map, converter), "Expected IllegalArgumentException for bad string under 'duration' key");
//     }

//     @Test
//     void toDuration_booleanUnsupported_throwsIllegalArgumentException() {
//         assertThrows(IllegalArgumentException.class, () -> MapConversions.toDuration(Boolean.TRUE, converter), "Boolean cannot be converted to Duration");
//     }

//     @Test
//     void toDuration_emptyMap_throwsIllegalArgumentException() {
//         assertThrows(IllegalArgumentException.class, () -> MapConversions.toDuration(Collections.emptyMap(), converter), "Empty map should cause IllegalArgumentException");
//     }

//     @Test
//     void toDuration_nullConverter_throwsNullPointerException() {
//         Duration source = Duration.ofSeconds(10);
//         assertThrows(NullPointerException.class, () -> MapConversions.toDuration(source, null), "Passing null Converter should result in NPE");
//     }
}