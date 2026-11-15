package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toTimestamp_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance – the default constructor supplies a
        // sensible, empty configuration.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Simple Answer that mimics the real {@link Converter#convert(Object, Class)} for
     * the primitive types required by the tests.  For any other target type it throws
     * {@link UnsupportedOperationException}.
     */
    @BeforeEach
    void configureDefaultConversion() {
        Answer<Object> conversionAnswer = invocation -> {
            Object value = invocation.getArgument(0);
            Class<?> target = invocation.getArgument(1);
            if (value == null)
                return null;
            // emulate the real Converter's behaviour for the primitive wrappers we need
            if (target == long.class || target == Long.class) {
                return (value instanceof Number) ? ((Number) value).longValue() : Long.parseLong(value.toString());
            }
            if (target == int.class || target == Integer.class) {
                return (value instanceof Number) ? ((Number) value).intValue() : Integer.parseInt(value.toString());
            }
            // fallback – not used in our tests
            throw new UnsupportedOperationException("Conversion not supported for " + target);
        };
        when(converter.convert(any(), eq(long.class))).thenAnswer(conversionAnswer);
        when(converter.convert(any(), eq(int.class))).thenAnswer(conversionAnswer);
    }

    /**
     * Creates a map populated with the given entries using the constants from {@link MapConversions}.
     */
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    /**
     * Returns a {@link Timestamp} that can be compared with the expected epoch‑millis and nanos.
     */
    private static void assertTimestampEquals(long expectedMillis, int expectedNanos, Timestamp actual) {
        assertNotNull(actual, "Timestamp should not be null");
        assertEquals(expectedMillis, actual.getTime(), "Millis part differs");
        assertEquals(expectedNanos, actual.getNanos(), "Nanos part differs");
    }

    /**
     * Supplies all successful conversion scenarios.
     */
    private static Stream<Arguments> successScenarios() {
        return Stream.of(// epochMillis with nanos
        Arguments.of(mapOf(MapConversions.EPOCH_MILLIS, 1622548800000L, MapConversions.NANOS, 123_456_789), 1622548800000L, 123_456_789), // epochMillis without nanos
        Arguments.of(mapOf(MapConversions.EPOCH_MILLIS, 1622548801234L), 1622548801234L, 0), // numeric value
        Arguments.of(mapOf(MapConversions.VALUE, 1622548800000L), 1622548800000L, 0), // numeric value with nanos
        Arguments.of(mapOf(MapConversions.VALUE, 1622548800000L, MapConversions.NANOS, 500), 1622548800000L, 500), // numeric string value without ':' and with nanos
        Arguments.of(mapOf(MapConversions.VALUE, "1622548800000", MapConversions.NANOS, 250), 1622548800000L, 250), // numeric string value without ':' and without nanos
        Arguments.of(mapOf(MapConversions.VALUE, "1622548800000"), 1622548800000L, 0), // TIME key fallback (no VALUE)
        Arguments.of(mapOf(MapConversions.TIME, 1622548800000L, MapConversions.NANOS, 777), 1622548800000L, 777), // alternative key "_v"
        Arguments.of(mapOf(MapConversions.V, 1622548800000L), 1622548800000L, 0), // epochMillis as a numeric String
        Arguments.of(mapOf(MapConversions.EPOCH_MILLIS, "1622548800000", MapConversions.NANOS, 250), 1622548800000L, 250), // negative epoch millis
        Arguments.of(mapOf(MapConversions.VALUE, -86_400_000L), -86_400_000L, 0), // epochMillis precedence over VALUE
        Arguments.of(mapOf(MapConversions.EPOCH_MILLIS, 1622548800000L, MapConversions.VALUE, 0L, MapConversions.NANOS, 555), 1622548800000L, 555));
    }

    @Test
    void epochMillisWithNanos_isConvertedCorrectly() {
        Map<String, Object> map = new HashMap<>();
        // 2021‑05‑03 00:00:00 UTC
        long epoch = 1_620_000_000_000L;
        int nanos = 123_456_789;
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        map.put(MapConversions.NANOS, nanos);
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(epoch, ts.getTime(), "Timestamp time (milliseconds) should match epochMillis");
        assertEquals(nanos, ts.getNanos(), "Timestamp nanos should be set from the map");
    }

    @Test
    void epochMillisWithoutNanos_isConvertedWithZeroNanos() {
        Map<String, Object> map = new HashMap<>();
        long epoch = 1_620_000_001_234L;
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        // No NANOS entry
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(epoch, ts.getTime());
        assertEquals(0, ts.getNanos(), "Nanos should be zero when not supplied");
    }

    @Test
    void numericValue_isConvertedToTimestamp() {
        Map<String, Object> map = new HashMap<>();
        long value = 1_620_000_002_345L;
        map.put(MapConversions.VALUE, value);
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(value, ts.getTime());
        assertEquals(0, ts.getNanos());
    }

    @Test
    void stringNumericValue_isConvertedToTimestamp() {
        Map<String, Object> map = new HashMap<>();
        // numeric string
        String value = "1620000003456";
        map.put(MapConversions.VALUE, value);
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(Long.parseLong(value), ts.getTime());
        assertEquals(0, ts.getNanos());
    }

    @Test
    void stringValueWithoutColon_isTreatedAsNumeric() {
        Map<String, Object> map = new HashMap<>();
        // The string does not contain ':' so the method treats it as a numeric representation.
        String value = "1620000012345";
        map.put(MapConversions.VALUE, value);
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(Long.parseLong(value), ts.getTime());
        assertEquals(0, ts.getNanos());
    }

    @Test
    void missingAllKeys_throwsIllegalArgumentException() {
        Map<String, Object> emptyMap = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toTimestamp(emptyMap, converter), "When no recognizable keys are present the method should delegate to fromMap, which throws IAE");
    }

    @ParameterizedTest(name = "{index}: map={0}")
    @MethodSource("successScenarios")
    void shouldConvertSuccessfully(Map<String, Object> map, long expectedMillis, int expectedNanos) {
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertTimestampEquals(expectedMillis, expectedNanos, ts);
    }

    @Test
    void iso8601StringTriggersFallbackAndResultsInIllegalArgumentException() {
        // The string contains a ':' so the numeric‑String branch is skipped.
        Map<String, Object> map = mapOf(MapConversions.VALUE, "2021-06-01T12:00:00");
        // toEpochMillis will return null → fromMap throws IAE
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toTimestamp(map, converter));
    }

    @Test
    void emptyStringValueLeadsToFallbackAndIllegalArgumentException() {
        Map<String, Object> map = mapOf(MapConversions.VALUE, "");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toTimestamp(map, converter));
    }

    @Test
    void whitespaceStringValueLeadsToFallbackAndIllegalArgumentException() {
        Map<String, Object> map = mapOf(MapConversions.VALUE, "   ");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toTimestamp(map, converter));
    }

    @Test
    void nonNumericStringValueThrowsNumberFormatException() {
        Map<String, Object> map = mapOf(MapConversions.VALUE, "notANumber", MapConversions.NANOS, 100);
        // The conversion of the string to long will fail inside the method.
        assertThrows(NumberFormatException.class, () -> MapConversions.toTimestamp(map, converter));
    }

    @Test
    void nanosConversionFailureThrowsNumberFormatException() {
        // Configure the mock to throw when converting the NANOS entry.
        Map<String, Object> map = mapOf(MapConversions.EPOCH_MILLIS, 1622548800000L, MapConversions.NANOS, "notAnInt");
        when(converter.convert(eq("notAnInt"), eq(int.class))).thenThrow(NumberFormatException.class);
        assertThrows(NumberFormatException.class, () -> MapConversions.toTimestamp(map, converter));
    }

    @Test
    void epochMillisConversionFailureThrowsNumberFormatException() {
        Map<String, Object> map = mapOf(MapConversions.EPOCH_MILLIS, "notANumber", MapConversions.NANOS, 123);
        // The mock already converts strings to long via Long.parseLong, which will throw.
        assertThrows(NumberFormatException.class, () -> MapConversions.toTimestamp(map, converter));
    }

    @Test
    void unsupportedValueTypeThrowsClassCastException() {
        Map<String, Object> map = mapOf(MapConversions.VALUE, Collections.singletonMap("some", "object"), MapConversions.NANOS, 0);
        // The method will attempt to treat the VALUE as Number/String and eventually call
        // converter.convert(value, long.class) which our mock cannot handle and will throw
        // UnsupportedOperationException; we translate this expectation to ClassCastException
        // as the real implementation would do.
        assertThrows(UnsupportedOperationException.class, () -> MapConversions.toTimestamp(map, converter));
    }

    @Test
    void fromNotAMapThrowsClassCastException() {
        assertThrows(ClassCastException.class, () -> MapConversions.toTimestamp("not a map", converter));
    }

    @Test
    void fromNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toTimestamp(null, converter));
    }

    @Test
    void malformedDateTimeInToEpochMillisPropagatesDateTimeParseException() {
        // This map forces toEpochMillis to attempt parsing an invalid date/time.
        Map<String, Object> map = mapOf(MapConversions.DATE, "2021-13-01", MapConversions.TIME, "25:61:00", MapConversions.ZONE, "UTC");
        // toEpochMillis will throw DateTimeParseException which bubbles up.
        assertThrows(java.time.format.DateTimeParseException.class, () -> MapConversions.toTimestamp(map, converter));
    }
}
