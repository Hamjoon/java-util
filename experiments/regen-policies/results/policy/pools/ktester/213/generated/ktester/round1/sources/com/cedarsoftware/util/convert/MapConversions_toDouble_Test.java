package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toDouble_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use the no‑arg constructor of Converter which supplies default options.
//         converter = new Converter();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> numberInputs() {
        return Stream.of(Arguments.of("Integer should be converted to Double via Number.doubleValue()", 42, 42.0), Arguments.of("Float should be converted to Double preserving the decimal part", 3.1415f, 3.1415), Arguments.of("BigDecimal should be converted to Double using its doubleValue()", new BigDecimal("123.456"), 123.456), Arguments.of("Direct Double instance should be returned unchanged", 5.5, 5.5), Arguments.of("Very large Float value should be converted to its double representation without overflow", Float.MAX_VALUE, (double) Float.MAX_VALUE), Arguments.of("BigInteger should be converted via its doubleValue()", new BigInteger("9876543210123456789"), 9.876543210123456E18), Arguments.of("AtomicInteger implements Number; its int value should be converted to Double", new AtomicInteger(42), 42.0));
    }

    private static Stream<Arguments> validStringInputs() {
        return Stream.of(Arguments.of("String containing a valid double literal should be parsed", "-99.99", -99.99), Arguments.of("String with whitespace and scientific notation should be trimmed and parsed", "   1.23e2   ", 123.0), Arguments.of("String \"NaN\" should be converted to Double.NaN", "NaN", Double.NaN));
    }

    private static Stream<Arguments> invalidStringInputs() {
        return Stream.of(Arguments.of("Empty string cannot be parsed as a double and should trigger NumberFormatException", ""), Arguments.of("String containing only whitespace should be trimmed to empty and cause NumberFormatException", "   "), Arguments.of("String with non‑numeric characters embedded should not be parsable and must raise NumberFormatException", "12a34"), Arguments.of("String containing more than one decimal point is malformed and should result in NumberFormatException", "1.2.3"));
    }

    private static Stream<Arguments> mapValueKeyInputs() {
        return Stream.of(Arguments.of("Map containing key \"value\" with a Number should return that number as Double", Collections.singletonMap("value", 7.89), 7.89), Arguments.of("Map containing key \"value\" with a parsable String should be converted to Double", Collections.singletonMap("value", "12.34"), 12.34));
    }

    private static Stream<Arguments> mapUnderscoreVInputs() {
        return Stream.of(Arguments.of("Map containing key \"_v\" with a Number should be converted", Collections.singletonMap("_v", 0.001), 0.001), Arguments.of("Map containing key \"_v\" with a parsable String should be converted", Collections.singletonMap("_v", "-0.5"), -0.5));
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(Arguments.of("String that cannot be parsed as a double should cause NumberFormatException", "not-a-number", NumberFormatException.class), Arguments.of("Map without any of the recognized value keys should return null (no conversion possible)", Collections.singletonMap("unexpected", 5), null), Arguments.of("Unsupported object type should result in null rather than throwing", new Object(), null));
    }

    private static Stream<Arguments> mapProblematicValueTypes() {
        return Stream.of(Arguments.of("Map with recognized key but Boolean value cannot be cast to Number; conversion should throw ClassCastException", Collections.singletonMap("value", true), ClassCastException.class), Arguments.of("Map contains the alternative value key but the associated value is null; attempting conversion should raise NullPointerException", Collections.singletonMap("_v", null), NullPointerException.class), Arguments.of("Map with key \"value\" whose value is an array (non‑Number) should cause ClassCastException during conversion", Collections.singletonMap("value", new int[] { 1, 2, 3 }), ClassCastException.class));
    }

    @Test
    void toDouble_WithMapContainingUnderscoreVKey_ShouldConvertStringToDouble() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.V, "123.45");
        // WHEN
        Double result = MapConversions.toDouble(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(123.45, result);
    }

    @Test
    void toDouble_WithMapContainingValueKey_ShouldConvertNumberToDouble() {
        // GIVEN
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.VALUE, 67.89);
        // WHEN
        Double result = MapConversions.toDouble(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(67.89, result);
    }

    @Test
    void toDouble_WithAlreadyDouble_ShouldReturnSameInstance() {
        // GIVEN
        Double original = 42.0;
        // WHEN
        Double result = MapConversions.toDouble(original, converter);
        // THEN
        assertEquals(original, result);
    }

    @Test
    void toDouble_WithNullSource_ShouldReturnNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Double result = MapConversions.toDouble(source, converter);
        // THEN
        assertNull(result);
    }

    /* -------------------------------------------------
     *  Number inputs
     * ------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numberInputs")
    void toDouble_NumberInputs(String description, Object from, Double expected) {
        Double result = MapConversions.toDouble(from, converter);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    /* -------------------------------------------------
     *  String inputs – valid
     * ------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validStringInputs")
    void toDouble_ValidStringInputs(String description, String from, Double expected) {
        Double result = MapConversions.toDouble(from, converter);
        assertNotNull(result);
        if (expected.isNaN()) {
            assertTrue(result.isNaN(), "Result should be NaN");
        } else {
            assertEquals(expected, result);
        }
    }

    /* -------------------------------------------------
     *  String inputs – invalid formats
     * ------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidStringInputs")
    void toDouble_InvalidStringInputs(String description, String from) {
        assertThrows(NumberFormatException.class, () -> MapConversions.toDouble(from, converter), "Expected NumberFormatException for input: " + from);
    }

    /* -------------------------------------------------
     *  Map inputs – value key
     * ------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapValueKeyInputs")
    void toDouble_MapValueKey(String description, Map<String, Object> from, Double expected) {
        Double result = MapConversions.toDouble(from, converter);
        assertEquals(expected, result);
    }

    /* -------------------------------------------------
     *  Map inputs – _v key
     * ------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapUnderscoreVInputs")
    void toDouble_MapUnderscoreVKey(String description, Map<String, Object> from, Double expected) {
        Double result = MapConversions.toDouble(from, converter);
        assertEquals(expected, result);
    }

    /* -------------------------------------------------
     *  Invalid inputs – exception handling
     * ------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputs")
    void toDouble_InvalidInputs(String description, Object from, Class<? extends Throwable> expectedException) {
        // If the expected exception is null, we simply assert that the result is null.
        if (expectedException == null) {
            Double result = MapConversions.toDouble(from, converter);
            assertNull(result);
        } else {
            assertThrows(expectedException, () -> MapConversions.toDouble(from, converter), "Expected " + expectedException.getSimpleName() + " for input: " + from);
        }
    }

    @Test
    void toDouble_MapWithoutRecognizedKeys_ShouldReturnNull() {
        Map<String, Object> source = Collections.singletonMap("unexpected", 5);
        Double result = MapConversions.toDouble(source, converter);
        assertNull(result);
    }

    @Test
    void toDouble_UnsupportedObject_ShouldReturnNull() {
        Double result = MapConversions.toDouble(new Object(), converter);
        assertNull(result);
    }

    /* -------------------------------------------------
     *  Null input handling
     * ------------------------------------------------- */
    @Test
    void toDouble_NullSource_ShouldReturnNull() {
        Double result = MapConversions.toDouble(null, converter);
        assertNull(result);
    }

    @Test
    void toDouble_NullConverter_ShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toDouble("123", null), "Both arguments null should trigger NPE because the method expects a non‑null Converter.");
    }

    /* -------------------------------------------------
     *  Map inputs – problematic value types
     * ------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapProblematicValueTypes")
    void toDouble_MapProblematicValueTypes(String description, Map<String, Object> from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapConversions.toDouble(from, converter), "Expected " + expectedException.getSimpleName() + " for map: " + from);
    }

    /* -------------------------------------------------
     *  Map inputs – both recognized keys present
     * ------------------------------------------------- */
    @Test
    void toDouble_MapBothKeysPresent_ShouldThrowIllegalArgumentException() {
        Map<String, Object> source = Map.of("value", "10.0", "_v", "20.0");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toDouble(source, converter), "When a map contains both recognized value keys, the conversion logic should be ambiguous and throw IllegalArgumentException.");
    }
}