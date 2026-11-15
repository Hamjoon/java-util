package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toTimestamp_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Simple holder for the arguments of the parameterized test.
     */
    private static final class ArgumentsHolder {

        final Map<String, Object> map;

        final long expectedEpochMillis;

        final int expectedNanos;

        final String description;

        ArgumentsHolder(Map<String, Object> map, long expectedEpochMillis, int expectedNanos, String description) {
            this.map = map;
            this.expectedEpochMillis = expectedEpochMillis;
            this.expectedNanos = expectedNanos;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared converter reference.
        converter = null;
    }

    /**
     * Provides arguments for {@link #toTimestamp_mapWithEpochMillisAndNanos}.
     *
     * Each argument consists of:
     * <ul>
     *   <li>a map containing the test data</li>
     *   <li>the expected epoch‑millis value</li>
     *   <li>the expected nanos value</li>
     *   <li>a description used only for readability</li>
     * </ul>
     */
    private static Stream<ArgumentsHolder> mapWithEpochMillisAndNanosProvider() {
        return Stream.of(// both epochMillis and nanos as numbers
        new ArgumentsHolder(mapOf("epochMillis", 1622548800000L, "nanos", 123456789), 1622548800000L, 123456789, "Map contains both epochMillis and nanos; Timestamp should be created with nanos set."), // epochMillis only
        new ArgumentsHolder(mapOf("epochMillis", 1622548800000L), 1622548800000L, 0, "Map contains epochMillis only; nanos default to 0."), // epochMillis with nanos = 0
        new ArgumentsHolder(mapOf("epochMillis", 1622548800000L, "nanos", 0), 1622548800000L, 0, "Map contains epochMillis and nanos set to 0; Timestamp nanos should remain 0."), // epochMillis and nanos as strings
        new ArgumentsHolder(mapOf("epochMillis", "1622548800000", "nanos", "123"), 1622548800000L, 123, "String values are converted to long and int; nanos applied."), // negative epochMillis with nanos
        new ArgumentsHolder(mapOf("epochMillis", -1L, "nanos", 500), -1L, 500, "Negative epochMillis with nanos; verifies handling of dates before epoch."));
    }

    @Test
    void toTimestamp_WithEpochMillisAndNanos_ReturnsCorrectTimestamp() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // 2021‑06‑01T12:00:00.123Z
        long epochMillis = 1_622_548_800_123L;
        // additional nanos within the same second
        int nanos = 456_789;
        map.put(MapConversions.EPOCH_MILLIS, epochMillis);
        map.put(MapConversions.NANOS, nanos);
        // WHEN
        Timestamp result = MapConversions.toTimestamp(map, converter);
        // THEN
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(epochMillis, result.getTime(), "Timestamp time (millis) does not match");
        assertEquals(nanos, result.getNanos(), "Timestamp nanos do not match");
    }

    @Test
    void toTimestamp_WithEpochMillisOnly_ReturnsTimestampWithZeroNanos() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        long epochMillis = 1_622_548_800_123L;
        map.put(MapConversions.EPOCH_MILLIS, epochMillis);
        // No NANOS entry – should default to 0
        // WHEN
        Timestamp result = MapConversions.toTimestamp(map, converter);
        // THEN
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(epochMillis, result.getTime(), "Timestamp time (millis) does not match");
        assertEquals(0, result.getNanos(), "Timestamp nanos should be 0 when not supplied");
    }

    @Test
    void toTimestamp_WithEpochMillisAsString_IsConvertedCorrectly() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // same value as a String
        String epochMillisStr = "1622548800123";
        int nanos = 0;
        map.put(MapConversions.EPOCH_MILLIS, epochMillisStr);
        map.put(MapConversions.NANOS, nanos);
        // WHEN
        Timestamp result = MapConversions.toTimestamp(map, converter);
        // THEN
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(Long.parseLong(epochMillisStr), result.getTime(), "Timestamp time (millis) should be parsed from String");
        assertEquals(nanos, result.getNanos(), "Timestamp nanos should match supplied value");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("mapWithEpochMillisAndNanosProvider")
    void toTimestamp_mapWithEpochMillisAndNanos(ArgumentsHolder args) {
        // WHEN
        Timestamp result = MapConversions.toTimestamp(args.map, converter);
        // THEN
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(args.expectedEpochMillis, result.getTime(), "Timestamp millis do not match expected value");
        assertEquals(args.expectedNanos, result.getNanos(), "Timestamp nanos do not match expected value");
    }

    @Test
    void toTimestamp_fromNotMap_throwsClassCastException() {
        // GIVEN a non‑map object
        Object notAMap = "just a string";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MapConversions.toTimestamp(notAMap, converter), "`from` is not a Map; the cast to Map<String,Object> should fail.");
    }

    @Test
    void toTimestamp_nullFrom_throwsNullPointerException() {
        // GIVEN a null reference for `from`
        Object nullFrom = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toTimestamp(nullFrom, converter), "`from` is null; attempting to call map.get should trigger NPE.");
    }

    @Test
    void toTimestamp_nullConverter_throwsNullPointerException() {
        // GIVEN a valid map but a null converter
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, 0L);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toTimestamp(map, null), "`converter` is null; any call to converter.convert should trigger NPE.");
    }

    @Test
    void toTimestamp_invalidEpochMillisConversion_throwsNumberFormatException() {
        // GIVEN a map where epochMillis is a non‑numeric string
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, "notANumber");
        map.put(MapConversions.NANOS, 0);
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toTimestamp(map, converter), "`epochMillis` cannot be converted to long; Converter should throw NumberFormatException.");
    }

    @Test
    void toTimestamp_invalidNanosConversion_throwsNumberFormatException() {
        // GIVEN a map where nanos is a non‑numeric string
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, 0L);
        map.put(MapConversions.NANOS, "abc");
        // WHEN / THEN
        assertThrows(NumberFormatException.class, () -> MapConversions.toTimestamp(map, converter), "`nanos` cannot be converted to int; Converter should throw NumberFormatException.");
    }

    @Test
    void toTimestamp_missingAllKeys_throwsIllegalArgumentException() {
        // GIVEN an empty map – no epochMillis, no value, no date/time keys
        Map<String, Object> emptyMap = new HashMap<>();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toTimestamp(emptyMap, converter), "When no recognizable keys are present, fromMap should throw IllegalArgumentException.");
    }

    /**
     * Utility method to create a {@link Map} from an even number of key/value pairs.
     */
    @SafeVarargs
    private static Map<String, Object> mapOf(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of arguments supplied to mapOf");
        }
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = (String) keyValues[i];
            Object value = keyValues[i + 1];
            // Translate the JSON‑style keys used in the test description to the actual constants.
            switch(key) {
                case "epochMillis":
                    map.put(MapConversions.EPOCH_MILLIS, value);
                    break;
                case "nanos":
                    map.put(MapConversions.NANOS, value);
                    break;
                default:
                    map.put(key, value);
            }
        }
        return map;
    }
}
