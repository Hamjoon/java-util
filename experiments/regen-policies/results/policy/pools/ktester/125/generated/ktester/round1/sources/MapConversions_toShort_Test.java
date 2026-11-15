package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class MapConversions_toShort_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a ConverterOptions instance; the default constructor gives us a
        // fully functional converter with all built‑in conversions registered.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the test tidy.
        converter = null;
    }

    /**
     * Helper that invokes the focal method.
     */
    private static Short callToShort(Object source) {
        return MapConversions.toShort(source, converter);
    }

    private static Stream<Arguments> directSuccessProvider() {
        return Stream.of(// already a Short
        arguments((short) 123, Short.valueOf((short) 123)), // Integer within Short range
        arguments(Integer.valueOf(456), Short.valueOf((short) 456)), // Long within Short range
        arguments(Long.valueOf(789L), Short.valueOf((short) 789)), // numeric String
        arguments("321", Short.valueOf((short) 321)));
    }

    private static Stream<Arguments> mapSuccessProvider() {
        return Stream.of(// key "value" with Short
        arguments(Collections.singletonMap("value", Short.valueOf((short) 55)), Short.valueOf((short) 55)), // key "_v" with numeric String
        arguments(Collections.singletonMap("_v", "-12"), Short.valueOf((short) -12)), // key "value" with Integer
        arguments(Map.of("value", Integer.valueOf(200)), Short.valueOf((short) 200)), // key "value" with String representing Short.MAX_VALUE
        arguments(Map.of("value", "32767"), Short.valueOf(Short.MAX_VALUE)), // key "value" with Boolean.TRUE (interpreted as 1)
        arguments(Map.of("value", Boolean.TRUE), Short.valueOf((short) 1)), // key "value" with Character 'A' (Unicode 65)
        arguments(Map.of("value", Character.valueOf('A')), Short.valueOf((short) 65)), // key "value" with BigDecimal within range
        arguments(Map.of("value", new BigDecimal("12345")), Short.valueOf((short) 12345)), // key "value" with BigInteger equal to Short.MIN_VALUE
        arguments(Map.of("value", new BigInteger("-32768")), Short.valueOf(Short.MIN_VALUE)));
    }

    private static Stream<Arguments> boundaryProvider() {
        return Stream.of(arguments(Short.MAX_VALUE, Short.valueOf(Short.MAX_VALUE)), arguments(Short.MIN_VALUE, Short.valueOf(Short.MIN_VALUE)), arguments(Integer.valueOf(32767), Short.valueOf(Short.MAX_VALUE)), arguments(Integer.valueOf(-32768), Short.valueOf(Short.MIN_VALUE)));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// non‑numeric String
        arguments("notANumber", NumberFormatException.class), // map with floating‑point String
        arguments(Map.of("value", "12.34"), NumberFormatException.class), // overflow beyond Short.MAX_VALUE
        arguments(Map.of("value", "32768"), ArithmeticException.class), // map with unknown key (should not throw, already covered by null case)
        // Null converter – should throw NPE before any conversion
        // placeholder, handled separately
        arguments(new Object[] { null, null }, NullPointerException.class));
    }

    @Test
    void toShort_FromMapWithVKey_StringNumber() {
        Map<String, Object> map = new HashMap<>();
        // value stored as a String
        map.put(MapConversions.V, "123");
        Short result = callToShort(map);
        assertNotNull(result);
        assertEquals(Short.valueOf((short) 123), result);
    }

    @Test
    void toShort_FromMapWithVKey_ShortObject() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.V, Short.valueOf((short) 45));
        Short result = callToShort(map);
        assertNotNull(result);
        assertEquals(Short.valueOf((short) 45), result);
    }

    @Test
    void toShort_FromMapWithValueKey_Integer() {
        Map<String, Object> map = new HashMap<>();
        // numeric type other than Short
        map.put(MapConversions.VALUE, Integer.valueOf(77));
        Short result = callToShort(map);
        assertNotNull(result);
        // Converter should truncate/convert the integer to short
        assertEquals(Short.valueOf((short) 77), result);
    }

    @Test
    void toShort_FromMapWithValueKey_String() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "255");
        Short result = callToShort(map);
        assertNotNull(result);
        assertEquals(Short.valueOf((short) 255), result);
    }

    @Test
    void toShort_FromMapMissingKeys_ReturnsNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("someOtherKey", "999");
        Short result = callToShort(map);
        // No recognised key – the conversion pipeline cannot locate a source value,
        // so the expected result is null.
        assertNull(result);
    }

    @Test
    void toShort_FromNonMap_ReturnsNull() {
        // Passing a non‑map object should result in a null return rather than an exception.
        Short result = callToShort("not a map");
        assertNull(result);
    }

    @Test
    void toShort_FromNull_ReturnsNull() {
        Short result = callToShort(null);
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("directSuccessProvider")
    void toShort_DirectSuccess(Object from, Short expected) {
        Short result = callToShort(from);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] map={0} → expected={1}")
    @MethodSource("mapSuccessProvider")
    void toShort_MapSuccess(Object fromMap, Short expected) {
        Short result = callToShort(fromMap);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("boundaryProvider")
    void toShort_BoundaryValues(Object from, Short expected) {
        Short result = callToShort(from);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void toShort_NullSource_ReturnsNull() {
        assertNull(callToShort(null));
    }

    @Test
    void toShort_EmptyMap_ReturnsNull() {
        assertNull(callToShort(Collections.emptyMap()));
    }

    @Test
    void toShort_MapWithKnownKeyButNullValue_ReturnsNull() {
        assertNull(callToShort(Collections.singletonMap("value", null)));
    }

    @Test
    void toShort_MapMissingAllKnownKeys_ReturnsNull() {
        assertNull(callToShort(Collections.singletonMap("unknownKey", "123")));
    }

    @Test
    void toShort_NonMapObject_ReturnsNull() {
        assertNull(callToShort("not a map"));
    }

    @ParameterizedTest(name = "[{index}] from={0} → throws={1}")
    @MethodSource("exceptionProvider")
    void toShort_InvalidInputs_Throws(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> callToShort(from));
    }

    /**
     * Specific test for a {@code null} {@link Converter}. The helper method cannot be used because
     * {@link MapConversions#toShort(Object, Converter)} will dereference the converter before any
     * other logic.
     */
    @Test
    void toShort_NullConverter_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toShort("123", null));
    }
}
