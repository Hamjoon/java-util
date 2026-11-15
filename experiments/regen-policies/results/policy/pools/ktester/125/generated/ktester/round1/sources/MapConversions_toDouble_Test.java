package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toDouble_Test {

    /**
     * The Converter instance used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter needs a ConverterOptions instance – the default constructor
        // supplies a sensible set of defaults for the built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly release the reference – helps the GC in long test suites.
        converter = null;
    }

    private static Stream<Arguments> numberInputProvider() {
        return Stream.of(Arguments.of("Convert an Integer value to Double.", 42, 42.0), Arguments.of("Convert a Long value to Double.", 1_234_567_890_123L, 1_234_567_890_123.0), Arguments.of("Convert a Double value (already a Double) – should be returned unchanged.", 3.14159, 3.14159), Arguments.of("Convert a BigDecimal to Double.", new BigDecimal("12345.6789"), 12345.6789), Arguments.of("Convert a BigInteger to Double.", new BigInteger("9876543210"), 9876543210.0));
    }

    private static Stream<Arguments> stringInputProvider() {
        return Stream.of(Arguments.of("Convert a plain numeric string to Double.", "42.5", 42.5), Arguments.of("Convert a numeric string with leading/trailing whitespace to Double.", "  -17.25  ", -17.25), Arguments.of("Convert a scientific‑notation string to Double.", "1.23e4", 12300.0), Arguments.of("Convert the string \"NaN\" to Double.NaN.", "NaN", Double.NaN), Arguments.of("Convert the string \"Infinity\" to Double.POSITIVE_INFINITY.", "Infinity", Double.POSITIVE_INFINITY), Arguments.of("Attempt to convert a non‑numeric string; expect a NumberFormatException.", "not-a-number", NumberFormatException.class));
    }

    private static Stream<Arguments> mapInputProvider() {
        // Helper to create maps
        Map<String, Object> mapValueDouble = new HashMap<>();
        mapValueDouble.put(MapConversions.VALUE, 3.14);
        Map<String, Object> mapUnderscoreV = new HashMap<>();
        mapUnderscoreV.put(MapConversions.V, "2.718");
        Map<String, Object> mapValueScientific = new HashMap<>();
        mapValueScientific.put(MapConversions.VALUE, "6.022e23");
        Map<String, Object> mapOtherKey = new HashMap<>();
        mapOtherKey.put("other", 1.23);
        Map<String, Object> mapValueNull = new HashMap<>();
        mapValueNull.put(MapConversions.VALUE, null);
        return Stream.of(Arguments.of("Map containing key \"value\" with a numeric Double; should be extracted and returned.", mapValueDouble, 3.14), Arguments.of("Map containing the alternative key \"_v\" with a numeric string; should be extracted and converted.", mapUnderscoreV, 2.718), Arguments.of("Map with key \"value\" holding a scientific‑notation string; should be parsed to Double.", mapValueScientific, 6.022e23), Arguments.of("Map does not contain any of the recognized keys; conversion should return null.", mapOtherKey, null), Arguments.of("Map contains the key \"value\" but its associated value is null; result should be null.", mapValueNull, null));
    }

    @Test
    void toDouble_fromMapWithUnderscoreVKey_stringNumber_returnsDouble() {
        // GIVEN a map that contains the special key "_v" with a numeric string
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.V, "123.45");
        // WHEN the conversion is performed
        Double result = MapConversions.toDouble(source, converter);
        // THEN the string is parsed to a Double
        assertEquals(123.45, result);
    }

    @Test
    void toDouble_fromMapWithValueKey_numberObject_returnsDouble() {
        // GIVEN a map that contains the generic "value" key with a Number
        Map<String, Object> source = new HashMap<>();
        source.put(MapConversions.VALUE, 99.9d);
        // WHEN the conversion is performed
        Double result = MapConversions.toDouble(source, converter);
        // THEN the Number is returned unchanged (as a Double)
        assertEquals(99.9, result);
    }

    @Test
    void toDouble_fromPlainDouble_returnsSameInstance() {
        // GIVEN a plain Double object (no map involved)
        Double original = 42.0;
        // WHEN the conversion is performed
        Double result = MapConversions.toDouble(original, converter);
        // THEN the same Double value is returned (identity is not required,
        // only equality – the implementation creates a new Double via fromMap)
        assertEquals(original, result);
    }

    @Test
    void toDouble_fromNull_returnsNull() {
        // GIVEN a null source
        Object source = null;
        // WHEN the conversion is performed
        Double result = MapConversions.toDouble(source, converter);
        // THEN a null result is expected (object conversion semantics)
        assertNull(result);
    }

    @Test
    void toDouble_fromMapWithUnsupportedKey_returnsNull() {
        // GIVEN a map that does not contain any of the recognised keys
        Map<String, Object> source = new HashMap<>();
        source.put("someOtherKey", "55.5");
        // WHEN the conversion is performed
        Double result = MapConversions.toDouble(source, converter);
        // THEN the conversion cannot locate a value and returns null
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numberInputProvider")
    void toDouble_numberInput(String description, Object input, Double expected) {
        Double result = MapConversions.toDouble(input, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringInputProvider")
    void toDouble_stringInput(String description, String input, Object expected) {
        if (expected instanceof Class<?>) {
            // Expected an exception type
            Class<?> exClass = (Class<?>) expected;
            assertThrows(exClass, () -> MapConversions.toDouble(input, converter));
        } else if (expected instanceof Double) {
            Double result = MapConversions.toDouble(input, converter);
            Double exp = (Double) expected;
            if (exp.isNaN()) {
                assertTrue(result.isNaN(), description);
            } else if (exp.isInfinite()) {
                assertTrue(result.isInfinite() && result > 0, description);
            } else {
                assertEquals(exp, result, description);
            }
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapInputProvider")
    void toDouble_mapInput(String description, Map<String, Object> map, Object expected) {
        if (expected == null) {
            assertNull(MapConversions.toDouble(map, converter), description);
        } else if (expected instanceof Double) {
            Double result = MapConversions.toDouble(map, converter);
            assertEquals((Double) expected, result, description);
        } else if (expected instanceof Class<?>) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapConversions.toDouble(map, converter), description);
        }
    }

    @Test
    void toDouble_nullSource_returnsNull() {
        assertNull(MapConversions.toDouble(null, converter), "Passing a null source object should result in a null Double.");
    }

    @Test
    void toDouble_emptyString_returnsNull() {
        assertNull(MapConversions.toDouble("", converter), "Empty string is treated as missing value; conversion returns null.");
    }

    @Test
    void toDouble_booleanSource_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toDouble(Boolean.TRUE, converter), "Boolean value cannot be converted to Double; expect IllegalArgumentException.");
    }

    @Test
    void toDouble_mapWithBooleanValue_throwsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, Boolean.FALSE);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toDouble(map, converter), "Map contains a Boolean under a recognized key; conversion should fail with IllegalArgumentException.");
    }

    @Test
    void toDouble_arraySource_throwsClassCastException() {
        int[] array = { 1, 2, 3 };
        assertThrows(ClassCastException.class, () -> MapConversions.toDouble(array, converter), "Array input is not supported; conversion should throw ClassCastException.");
    }

    @Test
    void toDouble_mapWithUnknownKey_returnsNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("unknownKey", "123.45");
        assertNull(MapConversions.toDouble(map, converter), "Map contains no recognized keys (value, _v, etc.); conversion should return null.");
    }
}
