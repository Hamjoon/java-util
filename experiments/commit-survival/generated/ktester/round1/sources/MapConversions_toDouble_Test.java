package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toDouble_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter constructor requires a ConverterOptions instance
        converter = new Converter(new ConverterOptions());
    }

    private static Map<String, Object> mapWithNullValue() {
        Map<String, Object> m = new HashMap<>();
        m.put("value", null);
        return m;
    }

    private static Map<String, Object> calendarMap() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 123456789 seconds -> millis
        cal.setTimeInMillis(123_456_789_000L);
        Map<String, Object> m = new HashMap<>();
        m.put("value", cal);
        return m;
    }

    private static Stream<Arguments> validDoubleProvider() {
        return Stream.of(Arguments.of("Direct Double value", Map.of("value", 123.45), 123.45), Arguments.of("Alternative key _v with numeric string", Map.of("_v", "678.9"), 678.9), Arguments.of("Integer value", Map.of("value", 42), 42.0), Arguments.of("BigDecimal value", Map.of("value", new BigDecimal("123.456")), 123.456), Arguments.of("Boolean true", Map.of("value", true), 1.0), Arguments.of("Boolean false", Map.of("value", false), 0.0), Arguments.of("AtomicInteger", Map.of("value", new AtomicInteger(7)), 7.0), Arguments.of("AtomicLong", Map.of("value", new AtomicLong(1_234_567_890_123L)), 1.234567890123E12), Arguments.of("AtomicBoolean true", Map.of("value", new AtomicBoolean(true)), 1.0), Arguments.of("AtomicBoolean false", Map.of("value", new AtomicBoolean(false)), 0.0), Arguments.of("Date epoch", Map.of("value", new Date(0L)), 0.0), Arguments.of("Timestamp", Map.of("value", new Timestamp(1_622_548_800_000L)), 1_622_548_800_000.0), Arguments.of("Map with null value", mapWithNullValue(), null), Arguments.of("Empty map", new HashMap<>(), null), Arguments.of("String with whitespace", Map.of("value", "  42.5  "), 42.5), Arguments.of("Scientific notation", Map.of("value", "1e3"), 1000.0), Arguments.of("String NaN", Map.of("value", "NaN"), Double.NaN), Arguments.of("String Infinity", Map.of("value", "Infinity"), Double.POSITIVE_INFINITY), Arguments.of("Float value", Map.of("value", 3.14f), 3.14), Arguments.of("Short value", Map.of("value", (short) 7), 7.0), Arguments.of("Byte value", Map.of("value", (byte) -5), -5.0), Arguments.of("BigInteger large", Map.of("value", new BigInteger("12345678901234567890")), 1.2345678901234568E19), Arguments.of("Instant epoch millis", Map.of("value", Instant.ofEpochMilli(123_456_789L)), 123_456_789.0), Arguments.of("Calendar UTC", calendarMap(), 123_456_789_000.0));
    }

    private static Stream<Arguments> mapExceptionProvider() {
        return Stream.of(Arguments.of("Invalid numeric string", Map.of("value", "not a number"), NumberFormatException.class), Arguments.of("Empty string", Map.of("value", ""), NumberFormatException.class), Arguments.of("String with non‑numeric characters", Map.of("value", "12abc34"), NumberFormatException.class), Arguments.of("Whitespace only", Map.of("value", "   "), NumberFormatException.class), Arguments.of("Grouping commas", Map.of("value", "1,234.56"), NumberFormatException.class), Arguments.of("Fallback key with non‑numeric", Map.of("_v", "abc"), NumberFormatException.class), Arguments.of("Unsupported object type", Map.of("value", new Object()), ClassCastException.class), Arguments.of("List as value", Map.of("value", Arrays.asList(1, 2, 3)), ClassCastException.class), Arguments.of("Duration as value", Map.of("value", Duration.ofHours(5)), ClassCastException.class), Arguments.of("Nested map as value", Map.of("value", new HashMap<>()), ClassCastException.class));
    }

    @Test
    void toDouble_fromMapWithNumericValue_returnsDouble() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // Integer value
        map.put(MapConversions.VALUE, 123);
        // WHEN
        Double result = MapConversions.toDouble(map, converter);
        // THEN
        assertNotNull(result);
        assertEquals(123.0, result);
    }

    @Test
    void toDouble_fromMapWithStringValue_returnsParsedDouble() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // String that can be parsed as double
        map.put(MapConversions.VALUE, "45.6");
        // WHEN
        Double result = MapConversions.toDouble(map, converter);
        // THEN
        assertNotNull(result);
        assertEquals(45.6, result);
    }

    @Test
    void toDouble_fromMapWithoutValueKey_returnsNull() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("someOtherKey", 999);
        // WHEN
        Double result = MapConversions.toDouble(map, converter);
        // THEN
        assertNull(result);
    }

    @Test
    void toDouble_fromNull_returnsNull() {
        // GIVEN
        Object from = null;
        // WHEN
        Double result = MapConversions.toDouble(from, converter);
        // THEN
        assertNull(result);
    }

    @Test
    void toDouble_fromAlreadyDouble_returnsSameInstance() {
        // GIVEN
        Double original = 12.34;
        // WHEN
        Double result = MapConversions.toDouble(original, converter);
        // THEN
        assertNotNull(result);
        assertEquals(original, result);
    }

    /**
     * Valid conversion scenarios – the method should return the expected Double
     * (or {@code null} when the map/value is null).  Special values such as
     * {@code NaN} and {@code Infinity} are verified with the appropriate
     * {@code Double} helpers.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validDoubleProvider")
    void toDouble_validInputs_returnsExpected(String description, Map<String, Object> map, Double expected) {
        Double result = MapConversions.toDouble(map, converter);
        if (expected == null) {
            assertNull(result, description);
        } else if (expected.isNaN()) {
            assertTrue(result.isNaN(), description);
        } else if (expected.isInfinite()) {
            assertTrue(result.isInfinite() && result > 0, description);
        } else {
            assertEquals(expected, result, description);
        }
    }

    /**
     * Scenarios that must raise an exception (NumberFormatException,
     * ClassCastException, or NullPointerException).
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapExceptionProvider")
    void toDouble_mapExceptions_throws(String description, Map<String, Object> map, Class<? extends Throwable> exceptionClass) {
        assertThrows(exceptionClass, () -> MapConversions.toDouble(map, converter), description);
    }

    /**
     * The source object is not a map – this should result in a ClassCastException.
     */
    @Test
    void toDouble_sourceNotMap_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> MapConversions.toDouble("just a string", converter), "Source object is a plain String, not a Map");
    }

    /**
     * Passing a {@code null} converter is illegal – the internal code dereferences it,
     * leading to a NullPointerException.
     */
    @Test
    void toDouble_nullConverter_throwsNullPointerException() {
        Map<String, Object> map = Map.of("value", "123.45");
        assertThrows(NullPointerException.class, () -> MapConversions.toDouble(map, null), "Converter argument is null");
    }
}
