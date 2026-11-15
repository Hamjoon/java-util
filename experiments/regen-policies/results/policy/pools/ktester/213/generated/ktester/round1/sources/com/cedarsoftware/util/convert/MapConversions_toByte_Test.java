package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;
// <-- added
import org.mockito.Mockito;

@ExtendWith(MockitoExtension.class)
class MapConversions_toByte_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter class requires a ConverterOptions instance.
        // We create a mock of the abstract ConverterOptions so that a concrete instance is available.
        ConverterOptions options = Mockito.mock(ConverterOptions.class);
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// String that cannot be parsed as a number
        Arguments.of("String that cannot be parsed as a number should cause NumberFormatException.", "not-a-number", NumberFormatException.class), // Unsupported source type
        Arguments.of("Unsupported source type should result in IllegalArgumentException.", new Object(), IllegalArgumentException.class), // Map without expected keys
        Arguments.of("Map without the expected keys should trigger IllegalArgumentException.", Map.of("unknown", "5"), IllegalArgumentException.class), // Null converter
        Arguments.of("Null converter should cause NullPointerException.", Integer.valueOf(10), NullPointerException.class));
    }

    private static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(Arguments.of("Integer within byte range should be converted to Byte value 123.", Integer.valueOf(123), (byte) 123), Arguments.of("Long within range should be converted to Byte value -50.", Long.valueOf(-50L), (byte) -50), Arguments.of("Double is truncated to its integer part before conversion to Byte.", Double.valueOf(12.9), (byte) 12), Arguments.of("Byte instance is returned unchanged.", Byte.valueOf((byte) 7), (byte) 7), Arguments.of("BigDecimal that fits in a byte is converted correctly.", BigDecimal.valueOf(123), (byte) 123), Arguments.of("BigInteger within byte range converts to the corresponding Byte.", new BigInteger("100"), (byte) 100));
    }

    private static Stream<Arguments> stringRepresentationProvider() {
        return Stream.of(Arguments.of("Numeric string is parsed and converted to Byte.", "42", (byte) 42), Arguments.of("String representing the minimum byte value converts correctly.", "-128", (byte) -128), Arguments.of("String representing the maximum byte value converts correctly.", "127", (byte) 127), Arguments.of("String value exceeding byte range should cause NumberFormatException.", "128", NumberFormatException.class), Arguments.of("Empty string cannot be parsed as a number and must throw NumberFormatException.", "", NumberFormatException.class));
    }

    private static Stream<Arguments> booleanCharacterProvider() {
        return Stream.of(Arguments.of("Boolean true is mapped to byte value 1.", Boolean.TRUE, (byte) 1), Arguments.of("Boolean false is mapped to byte value 0.", Boolean.FALSE, (byte) 0), Arguments.of("Character 'A' (Unicode 65) converts to its numeric byte representation.", Character.valueOf('A'), (byte) 65));
    }

    private static Stream<Arguments> mapWrapperProvider() {
        return Stream.of(Arguments.of("Map containing key \"value\" with a numeric string is extracted and converted.", Map.of("value", "55"), (byte) 55), Arguments.of("Map containing alternative key \"_v\" with an Integer is extracted and converted.", Map.of("_v", 9), (byte) 9));
    }

    private static Stream<Arguments> boundaryOverflowProvider() {
        return Stream.of(Arguments.of("Integer value just above Byte.MAX_VALUE should trigger NumberFormatException.", Integer.valueOf(128), NumberFormatException.class), Arguments.of("Integer value just below Byte.MIN_VALUE should trigger NumberFormatException.", Integer.valueOf(-129), NumberFormatException.class));
    }

    @Test
    void toByte_fromMapWithValueKey_stringNumber() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "123");
        Byte result = MapConversions.toByte(map, converter);
        assertEquals(Byte.valueOf((byte) 123), result);
    }

    @Test
    void toByte_fromMapWithVKey_integer() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 45);
        Byte result = MapConversions.toByte(map, converter);
        assertEquals(Byte.valueOf((byte) 45), result);
    }

    @Test
    void toByte_fromNumber() {
        Integer source = 7;
        Byte result = MapConversions.toByte(source, converter);
        assertEquals(Byte.valueOf((byte) 7), result);
    }

    @Test
    void toByte_fromString() {
        String source = "9";
        Byte result = MapConversions.toByte(source, converter);
        assertEquals(Byte.valueOf((byte) 9), result);
    }

    @Test
    void toByte_fromNull_returnsNull() {
        Byte result = MapConversions.toByte(null, converter);
        assertNull(result);
    }

    @Test
    void toByte_invalidContent_throwsException() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "not-a-number");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toByte(map, converter));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidConversionProvider")
    void toByte_invalidConversions(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapConversions.toByte(from, converter), description);
    }

    @Test
    void toByte_mapWithNullValue_returnsNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", null);
        Byte result = MapConversions.toByte(map, converter);
        assertNull(result, "Map contains the key but the associated value is null → conversion returns null.");
    }

    @Test
    void toByte_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toByte(Integer.valueOf(10), null), "Passing a null Converter should cause a NullPointerException.");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericWrapperProvider")
    void toByte_numericWrappers(String description, Object from, byte expected) {
        Byte result = MapConversions.toByte(from, converter);
        assertEquals(Byte.valueOf(expected), result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringRepresentationProvider")
    void toByte_stringRepresentations(String description, String source, Object expected) {
        if (expected instanceof Class) {
            // Expected an exception
            assertThrows((Class<? extends Throwable>) expected, () -> MapConversions.toByte(source, converter), description);
        } else {
            Byte result = MapConversions.toByte(source, converter);
            assertEquals(Byte.valueOf(((Number) expected).byteValue()), result, description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("booleanCharacterProvider")
    void toByte_booleanAndCharacter(String description, Object source, byte expected) {
        Byte result = MapConversions.toByte(source, converter);
        assertEquals(Byte.valueOf(expected), result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapWrapperProvider")
    void toByte_mapWrappers(String description, Map<String, Object> map, byte expected) {
        Byte result = MapConversions.toByte(map, converter);
        assertEquals(Byte.valueOf(expected), result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("boundaryOverflowProvider")
    void toByte_boundaryOverflow(String description, Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapConversions.toByte(source, converter), description);
    }

    @Test
    void toByte_nullSource_returnsNull() {
        Byte result = MapConversions.toByte(null, converter);
        assertNull(result, "Null source should result in a null Byte return.");
    }
}
