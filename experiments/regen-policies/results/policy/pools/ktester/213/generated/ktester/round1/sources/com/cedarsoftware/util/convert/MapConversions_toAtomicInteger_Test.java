package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.Arrays;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mockito;

/**
 * Unit tests for {@link MapConversions#toAtomicInteger(Object, Converter)}.
 */
@ExtendWith(MockitoExtension.class)
class MapConversions_toAtomicInteger_Test {

    /**
     * Shared Converter mock instance used by all tests.
     */
    private static Converter converter;

    /**
     * Keys that may hold the numeric value inside a {@link Map}.
     */
    static final String[] VALUE_KEYS = { "value", "_v" };

    @BeforeAll
    static void setupBeforeAll() {
        // Use a Mockito mock – the converter is not actually used by the conversion logic.
        converter = Mockito.mock(Converter.class);
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(
                Arguments.of(Integer.valueOf(7), 7),
                Arguments.of(Long.valueOf(123L), 123),
                Arguments.of(Short.valueOf((short) 4), 4),
                Arguments.of(Byte.valueOf((byte) 9), 9)
        );
    }

    private static Stream<Arguments> stringNumericProvider() {
        return Stream.of(
                Arguments.of("56", 56),
                Arguments.of("   -12  ", -12)
        );
    }

    private static Stream<Arguments> mapValueKeyProvider() {
        return Stream.of(
                Arguments.of(Map.of("value", 33), 33),
                Arguments.of(Map.of("_v", "44"), 44)
        );
    }

    private static Stream<Arguments> boundaryValuesProvider() {
        return Stream.of(
                Arguments.of(Integer.valueOf(Integer.MAX_VALUE), Integer.MAX_VALUE),
                Arguments.of(Integer.valueOf(Integer.MIN_VALUE), Integer.MIN_VALUE)
        );
    }

    private static Stream<Arguments> overflowProvider() {
        return Stream.of(
                Arguments.of("2147483648"),               // > Integer.MAX_VALUE
                Arguments.of("-2147483649"),              // < Integer.MIN_VALUE
                Arguments.of(Long.valueOf(Long.MAX_VALUE)),
                Arguments.of(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)),
                Arguments.of(BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE))
        );
    }

    private static Stream<Arguments> invalidMapProvider() {
        return Stream.of(
                Arguments.of(Map.of("value", null)),
                Arguments.of(Map.of("_v", true)),
                Arguments.of(Map.of("value", "12.34"))
        );
    }

    private static Stream<Arguments> floatingEdgeProvider() {
        return Stream.of(
                Arguments.of(Double.NaN),
                Arguments.of(Float.POSITIVE_INFINITY)
        );
    }

    private static Stream<Arguments> unsupportedCollectionProvider() {
        return Stream.of(
                Arguments.of(Arrays.asList(1, 2, 3)),
                Arguments.of(new Object[]{5})
        );
    }

    /**
     * Convert the supplied {@code source} object to an {@link AtomicInteger}.
     *
     * @param source    the object to convert; may be {@code null}
     * @param converter a {@link Converter} instance (not used by this implementation)
     * @return an {@link AtomicInteger} representing the numeric value of {@code source},
     *         or {@code null} if {@code source} is {@code null}
     * @throws IllegalArgumentException if the source cannot be interpreted as an {@code int}
     */
    public static AtomicInteger toAtomicInteger(Object source, Converter converter) {
        if (source == null) {
            return null;
        }
        if (source instanceof AtomicInteger) {
            return (AtomicInteger) source;
        }
        if (source instanceof Number) {
            return atomicFromNumber((Number) source);
        }
        if (source instanceof String) {
            return atomicFromString((String) source);
        }
        if (source instanceof Map<?, ?>) {
            return atomicFromMap((Map<?, ?>) source);
        }
        if (source instanceof Collection<?> || source != null && source.getClass().isArray()) {
            throw new IllegalArgumentException("Collections and arrays cannot be converted to AtomicInteger");
        }
        throw new IllegalArgumentException("Unsupported source type: " + source.getClass());
    }

    private static AtomicInteger atomicFromNumber(Number number) {
        if (number instanceof Float || number instanceof Double) {
            double d = number.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("NaN or infinite values are not allowed");
            }
            if (d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Floating point value out of int range");
            }
            return new AtomicInteger((int) d);
        }
        if (number instanceof BigInteger) {
            BigInteger bi = (BigInteger) number;
            if (bi.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0 ||
                bi.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new IllegalArgumentException("BigInteger value out of int range");
            }
            return new AtomicInteger(bi.intValue());
        }
        if (number instanceof BigDecimal) {
            BigDecimal bd = (BigDecimal) number;
            try {
                int i = bd.intValueExact();
                return new AtomicInteger(i);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException("BigDecimal value out of int range or has fraction", e);
            }
        }
        long l = number.longValue();
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Numeric value out of int range");
        }
        return new AtomicInteger((int) l);
    }

    private static AtomicInteger atomicFromString(String text) {
        String trimmed = text.trim();
        try {
            int i = Integer.parseInt(trimmed);
            return new AtomicInteger(i);
        } catch (NumberFormatException e) {
            try {
                double d = Double.parseDouble(trimmed);
                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    throw new IllegalArgumentException("NaN or infinite values are not allowed");
                }
                if (d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
                    throw new IllegalArgumentException("Floating point value out of int range");
                }
                return new AtomicInteger((int) d);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("String value is not a valid number: " + text, ex);
            }
        }
    }

    private static AtomicInteger atomicFromMap(Map<?, ?> map) {
        Object value = null;
        for (String key : VALUE_KEYS) {
            if (map.containsKey(key)) {
                value = map.get(key);
                break;
            }
        }
        if (value == null) {
            throw new IllegalArgumentException("Map does not contain a recognised value key");
        }
        return toAtomicInteger(value, null);
    }

    @Test
    void toAtomicInteger_fromMapWithValueKey_stringNumber() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "42");
        AtomicInteger result = MapConversions.toAtomicInteger(map, converter);
        assertNotNull(result, "Result should not be null");
        assertEquals(42, result.get(), "AtomicInteger should contain the converted int value");
    }

    @Test
    void toAtomicInteger_fromMapWithVKey_numeric() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 123);
        AtomicInteger result = MapConversions.toAtomicInteger(map, converter);
        assertNotNull(result, "Result should not be null");
        assertEquals(123, result.get(), "AtomicInteger should contain the converted int value");
    }

    @Test
    void toAtomicInteger_fromMapWithValueKey_directInteger() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", 7);
        AtomicInteger result = MapConversions.toAtomicInteger(map, converter);
        assertNotNull(result, "Result should not be null");
        assertEquals(7, result.get(), "AtomicInteger should contain the same integer value");
    }

    @Test
    void toAtomicInteger_fromNull_returnsNull() {
        AtomicInteger result = MapConversions.toAtomicInteger(null, converter);
        assertNull(result, "Converting a null source should return null");
    }

    @Test
    void toAtomicInteger_directAtomicInteger_input() {
        AtomicInteger source = new AtomicInteger(42);
        AtomicInteger result = MapConversions.toAtomicInteger(source, null);
        assertSame(source, result, "When the source is already an AtomicInteger the same instance should be returned");
    }

    @ParameterizedTest(name = "{index}: from={0} expected={1}")
    @MethodSource("numericWrapperProvider")
    void toAtomicInteger_numericWrappers(Object from, int expected) {
        AtomicInteger result = MapConversions.toAtomicInteger(from, null);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{index}: from=\"{0}\" expected={1}")
    @MethodSource("stringNumericProvider")
    void toAtomicInteger_stringValues(Object from, int expected) {
        AtomicInteger result = MapConversions.toAtomicInteger(from, null);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void toAtomicInteger_stringNotANumber_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toAtomicInteger("notANumber", null),
                "Non‑numeric strings must cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: map={0} expected={1}")
    @MethodSource("mapValueKeyProvider")
    void toAtomicInteger_mapWithValueKeys(Map<String, Object> map, int expected) {
        AtomicInteger result = MapConversions.toAtomicInteger(map, null);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void toAtomicInteger_mapMissingKey_throws() {
        Map<String, Object> map = Map.of("other", 5);
        assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toAtomicInteger(map, null),
                "Map without recognised value keys must cause IllegalArgumentException");
    }

    @Test
    void toAtomicInteger_nullInput_returnsNull() {
        assertNull(MapConversions.toAtomicInteger(null, null));
    }

    @Test
    void toAtomicInteger_unsupportedObject_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toAtomicInteger(new Object(), null),
                "Unsupported source types must raise IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: from={0} expected={1}")
    @MethodSource("boundaryValuesProvider")
    void toAtomicInteger_boundaryValues(Object from, int expected) {
        AtomicInteger result = MapConversions.toAtomicInteger(from, null);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @ParameterizedTest(name = "{index}: overflow input={0}")
    @MethodSource("overflowProvider")
    void toAtomicInteger_overflowScenarios(Object from) {
        assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toAtomicInteger(from, null),
                "Values outside int range must cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: map={0}")
    @MethodSource("invalidMapProvider")
    void toAtomicInteger_invalidMapValues(Map<String, Object> map) {
        assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toAtomicInteger(map, null),
                "Maps with null or non‑numeric values must raise IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("floatingEdgeProvider")
    void toAtomicInteger_floatingEdgeValues(Object from) {
        assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toAtomicInteger(from, null),
                "NaN or infinite floating‑point values must cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("unsupportedCollectionProvider")
    void toAtomicInteger_unsupportedCollections(Object from) {
        assertThrows(IllegalArgumentException.class,
                () -> MapConversions.toAtomicInteger(from, null),
                "Collections and arrays are not supported sources for AtomicInteger conversion");
    }
}