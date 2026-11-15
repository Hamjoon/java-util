package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class MapConversions_toBigDecimal_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs a ConverterOptions instance – the default constructor provides sensible defaults
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up after each test
        converter = null;
    }

    static Stream<Arguments> nullInputProvider() {
        return Stream.of(arguments(null));
    }

    static Stream<Arguments> directBigDecimalProvider() {
        return Stream.of(arguments(new BigDecimal("123.456")));
    }

    static Stream<Arguments> numericTypesProvider() {
        return Stream.of(arguments(Integer.valueOf(42), new BigDecimal("42")), arguments(Long.valueOf(1234567890123L), new BigDecimal("1234567890123")), arguments(Double.valueOf(3.14159), new BigDecimal("3.14159")), arguments(new BigInteger("98765432109876543210"), new BigDecimal(new BigInteger("98765432109876543210"))));
    }

    static Stream<Arguments> stringInputProvider() {
        return Stream.of(arguments("0.001", new BigDecimal("0.001")), arguments("   42   ", new BigDecimal("42")), arguments("invalid", NumberFormatException.class));
    }

    static Stream<Arguments> mapPreferredKeysProvider() {
        return Stream.of(arguments(Map.of("value", "123.45"), new BigDecimal("123.45")), arguments(Map.of("_v", new BigDecimal("9.99")), new BigDecimal("9.99")));
    }

    static Stream<Arguments> mapFallbackKeysProvider() {
        return Stream.of(arguments(Map.of("bigDecimal", "7.77"), new BigDecimal("7.77")), arguments(Map.of("amount", 55), new BigDecimal("55")));
    }

    static Stream<Arguments> mapNullOrEmptyProvider() {
        return Stream.of(arguments(Map.of("value", null), IllegalArgumentException.class), arguments(Map.of(), IllegalArgumentException.class), arguments(Map.of("value", "   "), NumberFormatException.class), arguments(Map.of("value", ""), NumberFormatException.class), arguments(Map.of("value", "1,234.56"), NumberFormatException.class));
    }

    static Stream<Arguments> mapMixedTypesProvider() {
        return Stream.of(arguments(Map.of("value", Integer.valueOf(99)), new BigDecimal("99")), arguments(Map.of("value", Long.valueOf(123456789L)), new BigDecimal("123456789")), arguments(Map.of("value", Double.valueOf(12.34)), new BigDecimal("12.34")), arguments(Map.of("value", new BigInteger("98765432109876543210")), new BigDecimal(new BigInteger("98765432109876543210"))), arguments(Map.of("value", new BigDecimal("55.55")), new BigDecimal("55.55")));
    }

    static Stream<Arguments> stringEdgeCasesProvider() {
        return Stream.of(arguments("+123.45", new BigDecimal("+123.45")), arguments("-0.001", new BigDecimal("-0.001")), arguments("1e3", new BigDecimal("1e3")), arguments("000123.4500", new BigDecimal("000123.4500")), arguments("   +42   ", new BigDecimal("42")));
    }

    static Stream<Arguments> numberEdgeCasesProvider() {
        return Stream.of(arguments(Double.NaN, NumberFormatException.class), arguments(Double.POSITIVE_INFINITY, NumberFormatException.class), arguments(new AtomicInteger(77), new BigDecimal("77")), arguments(new AtomicLong(123456789012345L), new BigDecimal("123456789012345")), arguments(new AtomicBoolean(true), ClassCastException.class));
    }

    static Stream<Arguments> charSequenceProvider() {
        return Stream.of(arguments(new StringBuilder("987.65"), new BigDecimal("987.65")), arguments(new StringBuffer("   321   "), new BigDecimal("321")));
    }

    static Stream<Arguments> converterInteractionProvider() {
        return Stream.of(arguments(Map.of("value", "123"), true), arguments("456.78", true));
    }

    static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(arguments(new Date()), arguments(Instant.now()), arguments(new Object[] { "123" }));
    }

    static Stream<Arguments> unsupportedTemporalProvider() {
        return Stream.of(arguments(LocalDate.now()), arguments(ZonedDateTime.now()));
    }

    @Test
    void toBigDecimal_FromMapWithDefaultValueKey() {
        Map<String, Object> source = new HashMap<>();
        // the generic "value" key
        source.put(MapConversions.VALUE, "123.45");
        BigDecimal result = MapConversions.toBigDecimal(source, converter);
        assertNotNull(result);
        assertEquals(new BigDecimal("123.45"), result);
    }

    @Test
    void toBigDecimal_FromMapWithSpecialVKey() {
        Map<String, Object> source = new HashMap<>();
        // the special "_v" key used by many map conversions
        source.put(MapConversions.V, "75.0");
        BigDecimal result = MapConversions.toBigDecimal(source, converter);
        assertNotNull(result);
        assertEquals(new BigDecimal("75.0"), result);
    }

    @Test
    void toBigDecimal_FromStringNumberAndFromBigDecimalDirectly() {
        // Case 1 – a plain String that can be parsed as a number
        BigDecimal fromString = MapConversions.toBigDecimal("9876.54321", converter);
        assertEquals(new BigDecimal("9876.54321"), fromString);
        // Case 2 – the source is already a BigDecimal; the method should return it unchanged
        BigDecimal original = new BigDecimal("0.001");
        BigDecimal fromBigDecimal = MapConversions.toBigDecimal(original, converter);
        assertSame(original, fromBigDecimal);
    }

    @Test
    void toBigDecimal_NullInputReturnsNull() {
        assertNull(MapConversions.toBigDecimal(null, converter));
    }

    // ---------- Null Input ----------
    @ParameterizedTest(name = "{index} => from={0}")
    @MethodSource("nullInputProvider")
    void testNullInput(Object from) {
        assertNull(MapConversions.toBigDecimal(from, converter));
    }

    // ---------- Direct BigDecimal Instance ----------
    @ParameterizedTest(name = "{index} => from={0}")
    @MethodSource("directBigDecimalProvider")
    void testDirectBigDecimalInstance(BigDecimal from) {
        BigDecimal result = MapConversions.toBigDecimal(from, converter);
        // The method may return the same instance or an equal one – both are acceptable.
        assertEquals(from, result);
    }

    // ---------- Numeric Types ----------
    @ParameterizedTest(name = "{index} => from={0}, expected={1}")
    @MethodSource("numericTypesProvider")
    void testNumericTypes(Object from, BigDecimal expected) {
        BigDecimal result = MapConversions.toBigDecimal(from, converter);
        assertEquals(expected, result);
    }

    // ---------- String Input ----------
    @ParameterizedTest(name = "{index} => from=\"{0}\", expected={1}")
    @MethodSource("stringInputProvider")
    void testStringInput(String from, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapConversions.toBigDecimal(from, converter));
        } else {
            BigDecimal result = MapConversions.toBigDecimal(from, converter);
            assertEquals(expected, result);
        }
    }

    // ---------- Map Input – Preferred Keys ----------
    @ParameterizedTest(name = "{index} => map={0}, expected={1}")
    @MethodSource("mapPreferredKeysProvider")
    void testMapPreferredKeys(Map<String, Object> map, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapConversions.toBigDecimal(map, converter));
        } else {
            BigDecimal result = MapConversions.toBigDecimal(map, converter);
            assertEquals(expected, result);
        }
    }

    // ---------- Map Input – Fallback Keys ----------
    @ParameterizedTest(name = "{index} => map={0}, expected={1}")
    @MethodSource("mapFallbackKeysProvider")
    void testMapFallbackKeys(Map<String, Object> map, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapConversions.toBigDecimal(map, converter));
        } else {
            BigDecimal result = MapConversions.toBigDecimal(map, converter);
            assertEquals(expected, result);
        }
    }

    // ---------- Map Input – Missing Convertible Value ----------
    @Test
    void testMapMissingConvertibleValue() {
        Map<String, Object> map = Map.of("foo", "bar");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toBigDecimal(map, converter));
    }

    // ---------- Unsupported Type ----------
    @Test
    void testUnsupportedType() {
        Object unsupported = new Object();
        assertThrows(ClassCastException.class, () -> MapConversions.toBigDecimal(unsupported, converter));
    }

    // ---------- Map Input – Null or Empty Values ----------
    @ParameterizedTest(name = "{index} => map={0}, expectedException={1}")
    @MethodSource("mapNullOrEmptyProvider")
    void testMapNullOrEmptyValues(Map<String, Object> map, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> MapConversions.toBigDecimal(map, converter));
    }

    // ---------- Map Input – Mixed Types for Preferred Key ----------
    @ParameterizedTest(name = "{index} => map={0}, expected={1}")
    @MethodSource("mapMixedTypesProvider")
    void testMapMixedTypesPreferredKey(Map<String, Object> map, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapConversions.toBigDecimal(map, converter));
        } else {
            BigDecimal result = MapConversions.toBigDecimal(map, converter);
            assertEquals(expected, result);
        }
    }

    // ---------- String Edge Cases ----------
    @ParameterizedTest(name = "{index} => from=\"{0}\", expected={1}")
    @MethodSource("stringEdgeCasesProvider")
    void testStringEdgeCases(String from, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapConversions.toBigDecimal(from, converter));
        } else {
            BigDecimal result = MapConversions.toBigDecimal(from, converter);
            assertEquals(expected, result);
        }
    }

    // ---------- Number Edge Cases ----------
    @ParameterizedTest(name = "{index} => from={0}, expected={1}")
    @MethodSource("numberEdgeCasesProvider")
    void testNumberEdgeCases(Object from, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapConversions.toBigDecimal(from, converter));
        } else {
            BigDecimal result = MapConversions.toBigDecimal(from, converter);
            assertEquals(expected, result);
        }
    }

    // ---------- CharSequence Implementations ----------
    @ParameterizedTest(name = "{index} => cs={0}, expected={1}")
    @MethodSource("charSequenceProvider")
    void testCharSequenceImplementations(CharSequence cs, Object expected) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapConversions.toBigDecimal(cs, converter));
        } else {
            BigDecimal result = MapConversions.toBigDecimal(cs, converter);
            assertEquals(expected, result);
        }
    }

    // ---------- Converter Interaction ----------
    @ParameterizedTest(name = "{index} => from={0}, useMock={1}")
    @MethodSource("converterInteractionProvider")
    void testConverterInteraction(Object from, boolean useMock) {
        Converter usedConverter = useMock ? mock(Converter.class) : converter;
        BigDecimal result = MapConversions.toBigDecimal(from, usedConverter);
        assertEquals(new BigDecimal("123"), result);
    }

    // ---------- Unsupported Types – Additional Coverage ----------
    @ParameterizedTest(name = "{index} => from={0}")
    @MethodSource("unsupportedTypesProvider")
    void testUnsupportedTypes(Object from) {
        assertThrows(ClassCastException.class, () -> MapConversions.toBigDecimal(from, converter));
    }

    // ---------- Direct Empty String ----------
    @Test
    void testDirectEmptyString() {
        assertThrows(NumberFormatException.class, () -> MapConversions.toBigDecimal("", converter));
    }

    // ---------- Direct Whitespace String ----------
    @Test
    void testDirectWhitespaceString() {
        assertThrows(NumberFormatException.class, () -> MapConversions.toBigDecimal("   ", converter));
    }

    // ---------- Map Boolean Value ----------
    @Test
    void testMapBooleanValue() {
        Map<String, Object> map = Map.of("value", true);
        assertThrows(ClassCastException.class, () -> MapConversions.toBigDecimal(map, converter));
    }

    // ---------- Map List Value ----------
    @Test
    void testMapListValue() {
        Map<String, Object> map = Map.of("value", Arrays.asList(1, 2, 3));
        assertThrows(ClassCastException.class, () -> MapConversions.toBigDecimal(map, converter));
    }

    // ---------- Map Nested Map Value ----------
    @Test
    void testMapNestedMapValue() {
        Map<String, Object> map = Map.of("value", Map.of("inner", "123"));
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toBigDecimal(map, converter));
    }

    // ---------- Unsupported Temporal Types ----------
    @ParameterizedTest(name = "{index} => from={0}")
    @MethodSource("unsupportedTemporalProvider")
    void testUnsupportedTemporalTypes(Object from) {
        assertThrows(ClassCastException.class, () -> MapConversions.toBigDecimal(from, converter));
    }

    // ---------- AtomicReference Input ----------
    @Test
    void testAtomicReferenceInput() {
        AtomicReference<BigDecimal> ref = new AtomicReference<>(new BigDecimal("5"));
        assertThrows(ClassCastException.class, () -> MapConversions.toBigDecimal(ref, converter));
    }

    // ---------- Invalid Scientific Notation String ----------
    @Test
    void testInvalidScientificNotationString() {
        assertThrows(NumberFormatException.class, () -> MapConversions.toBigDecimal("1e", converter));
    }
}
