package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toAtomicBoolean_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor gives a
        // clean configuration suitable for unit testing.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long test runs.
        converter = null;
    }

    /**
     * Direct Boolean input – true / false
     */
    private static Stream<Arguments> directBooleanProvider() {
        return Stream.of(Arguments.of(true, new AtomicBoolean(true)), Arguments.of(false, new AtomicBoolean(false)));
    }

    /**
     * Numeric input – any non‑zero number should be true, zero false
     */
    private static Stream<Arguments> numericProvider() {
        return Stream.of(Arguments.of(1, new AtomicBoolean(true)), Arguments.of(0, new AtomicBoolean(false)), Arguments.of(-42, new AtomicBoolean(true)));
    }

    /**
     * String input – case‑insensitive, whitespace trimmed
     */
    private static Stream<Arguments> stringProvider() {
        return Stream.of(Arguments.of("true", new AtomicBoolean(true)), Arguments.of("FALSE", new AtomicBoolean(false)), // Boolean.valueOf("yes") → true
        Arguments.of("yes", new AtomicBoolean(true)), Arguments.of("TrUe", new AtomicBoolean(true)), Arguments.of(" false ", new AtomicBoolean(false)), Arguments.of("", new AtomicBoolean(false)), Arguments.of("no", new AtomicBoolean(false)), Arguments.of("0", new AtomicBoolean(false)), Arguments.of("   ", new AtomicBoolean(false)), Arguments.of("null", new AtomicBoolean(false)));
    }

    /**
     * Already an AtomicBoolean – should be returned unchanged
     */
    private static Stream<Arguments> atomicBooleanProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), new AtomicBoolean(true)), Arguments.of(new AtomicBoolean(false), new AtomicBoolean(false)));
    }

    /**
     * Map input – primary key “value”
     */
    private static Stream<Arguments> mapValueKeyProvider() {
        return Stream.of(Arguments.of(mapOf("value", true), new AtomicBoolean(true)), Arguments.of(mapOf("value", 0), new AtomicBoolean(false)), Arguments.of(mapOf("value", "false"), new AtomicBoolean(false)), Arguments.of(mapOf("value", null), new AtomicBoolean(false)), // prioritize "value"
        Arguments.of(mapOf("value", false, "_v", true), new AtomicBoolean(false)));
    }

    /**
     * Map input – alternative key “_v”
     */
    private static Stream<Arguments> mapAltKeyProvider() {
        return Stream.of(Arguments.of(mapOf("_v", true), new AtomicBoolean(true)), Arguments.of(mapOf("_v", "true"), new AtomicBoolean(true)));
    }

    /**
     * Map input – missing recognised keys → default false
     */
    private static Stream<Arguments> mapMissingKeyProvider() {
        return Stream.of(Arguments.of(mapOf("unknown", "data"), new AtomicBoolean(false)), Arguments.of(Collections.emptyMap(), new AtomicBoolean(false)));
    }

    /**
     * Numeric type variations (byte, short, long, BigInteger, BigDecimal)
     */
    private static Stream<Arguments> numericTypesProvider() {
        return Stream.of(Arguments.of((byte) 1, new AtomicBoolean(true)), Arguments.of((short) 0, new AtomicBoolean(false)), Arguments.of(-5L, new AtomicBoolean(true)), Arguments.of(BigInteger.ONE, new AtomicBoolean(true)), Arguments.of(BigDecimal.ZERO, new AtomicBoolean(false)));
    }

    /**
     * Unsupported type that should trigger IllegalArgumentException
     */
    private static Stream<Arguments> unsupportedExceptionProvider() {
        return Stream.of(// Double not explicitly handled
        Arguments.of(3.14));
    }

    /**
     * Null source → null result
     */
    private static Stream<Arguments> nullSourceProvider() {
        return Stream.of(Arguments.of(null));
    }

    /**
     * Null converter → NullPointerException
     */
    private static Stream<Arguments> nullConverterProvider() {
        return Stream.of(Arguments.of(true), Arguments.of("true"), Arguments.of(mapOf("value", true)));
    }

    /* --------------------------------------------------------------------- *
     *  Helper to build maps fluently
     * --------------------------------------------------------------------- */
    private static Map<String, Object> mapOf(Object... keyVals) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            map.put(String.valueOf(keyVals[i]), keyVals[i + 1]);
        }
        return map;
    }

    @Test
    @DisplayName("Convert Boolean true to AtomicBoolean")
    void convertBooleanTrue() {
        Boolean source = Boolean.TRUE;
        AtomicBoolean result = MapConversions.toAtomicBoolean(source, converter);
        assertNotNull(result, "Result should not be null");
        assertTrue(result.get(), "AtomicBoolean should contain true");
    }

    @Test
    @DisplayName("Convert Boolean false to AtomicBoolean")
    void convertBooleanFalse() {
        Boolean source = Boolean.FALSE;
        AtomicBoolean result = MapConversions.toAtomicBoolean(source, converter);
        assertNotNull(result, "Result should not be null");
        assertFalse(result.get(), "AtomicBoolean should contain false");
    }

    @Test
    @DisplayName("Convert String \"true\" to AtomicBoolean")
    void convertStringTrue() {
        String source = "true";
        AtomicBoolean result = MapConversions.toAtomicBoolean(source, converter);
        assertNotNull(result);
        assertTrue(result.get());
    }

    @Test
    @DisplayName("Convert String \"false\" to AtomicBoolean")
    void convertStringFalse() {
        String source = "false";
        AtomicBoolean result = MapConversions.toAtomicBoolean(source, converter);
        assertNotNull(result);
        assertFalse(result.get());
    }

    @Test
    @DisplayName("Convert Map with key \"value\" (Boolean) to AtomicBoolean")
    void convertMapWithBooleanValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", Boolean.TRUE);
        AtomicBoolean result = MapConversions.toAtomicBoolean(map, converter);
        assertNotNull(result);
        assertTrue(result.get());
    }

    @Test
    @DisplayName("Convert Map with key \"value\" (String) to AtomicBoolean")
    void convertMapWithStringValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "false");
        AtomicBoolean result = MapConversions.toAtomicBoolean(map, converter);
        assertNotNull(result);
        assertFalse(result.get());
    }

    @Test
    @DisplayName("Null source returns null")
    void nullSourceReturnsNull() {
        AtomicBoolean result = MapConversions.toAtomicBoolean(null, converter);
        assertNull(result, "Conversion of null should yield null");
    }

    @Test
    @DisplayName("Unsupported type (e.g., Integer) returns null")
    void unsupportedTypeReturnsNull() {
        Integer source = 42;
        AtomicBoolean result = MapConversions.toAtomicBoolean(source, converter);
        assertNull(result, "Conversion of an unsupported type should yield null");
    }

    @ParameterizedTest(name = "[{index}] Boolean {0} → {1}")
    @MethodSource("directBooleanProvider")
    void testDirectBooleanInput(Boolean from, AtomicBoolean expected) {
        AtomicBoolean result = MapConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get(), () -> "Expected AtomicBoolean(" + expected.get() + ") but got " + result.get());
    }

    @ParameterizedTest(name = "[{index}] Numeric {0} → {1}")
    @MethodSource("numericProvider")
    void testNumericInput(Number from, AtomicBoolean expected) {
        AtomicBoolean result = MapConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get());
    }

    @ParameterizedTest(name = "[{index}] String \"{0}\" → {1}")
    @MethodSource("stringProvider")
    void testStringInput(String from, AtomicBoolean expected) {
        AtomicBoolean result = MapConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get(), () -> "String \"" + from + "\" should map to " + expected.get());
    }

    @ParameterizedTest(name = "[{index}] AtomicBoolean {0} → same instance")
    @MethodSource("atomicBooleanProvider")
    void testAtomicBooleanInput(AtomicBoolean from, AtomicBoolean expected) {
        AtomicBoolean result = MapConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        // The implementation may return the same instance or a new one with same value.
        // We only assert the logical value.
        assertEquals(expected.get(), result.get());
    }

    @ParameterizedTest(name = "[{index}] Map {0} → {1}")
    @MethodSource("mapValueKeyProvider")
    void testMapValueKeyInput(Map<String, Object> from, AtomicBoolean expected) {
        AtomicBoolean result = MapConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get());
    }

    @ParameterizedTest(name = "[{index}] Map alt key {0} → {1}")
    @MethodSource("mapAltKeyProvider")
    void testMapAlternativeKeyInput(Map<String, Object> from, AtomicBoolean expected) {
        AtomicBoolean result = MapConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get());
    }

    @ParameterizedTest(name = "[{index}] Map missing keys {0} → false")
    @MethodSource("mapMissingKeyProvider")
    void testMapMissingKeys(Map<String, Object> from, AtomicBoolean expected) {
        AtomicBoolean result = MapConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get());
    }

    @ParameterizedTest(name = "[{index}] Numeric type {0} → {1}")
    @MethodSource("numericTypesProvider")
    void testNumericTypesVariations(Object from, AtomicBoolean expected) {
        AtomicBoolean result = MapConversions.toAtomicBoolean(from, converter);
        assertNotNull(result);
        assertEquals(expected.get(), result.get(), () -> "Numeric type " + from.getClass().getSimpleName() + " should map to " + expected.get());
    }

    @ParameterizedTest(name = "[{index}] Null source")
    @MethodSource("nullSourceProvider")
    void testNullSource(Object from) {
        AtomicBoolean result = MapConversions.toAtomicBoolean(from, converter);
        assertNull(result, "Conversion of null should return null");
    }

    @ParameterizedTest(name = "[{index}] Unsupported type throws IAE")
    @MethodSource("unsupportedExceptionProvider")
    void testUnsupportedTypeThrows(Object from) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicBoolean(from, converter), "Unsupported type should cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] Null converter throws NPE")
    @MethodSource("nullConverterProvider")
    void testNullConverter(Object from) {
        assertThrows(NullPointerException.class, () -> MapConversions.toAtomicBoolean(from, null), "Passing a null Converter must throw NullPointerException");
    }
}
