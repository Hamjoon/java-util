package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toByte_Test {

    /**
     * Shared {@code Converter} used by all test methods.
     */
    private static Converter converter;

    /**
     * Fresh map instance for each test.
     */
    private Map<String, Object> sourceMap;

    @BeforeAll
    static void setupBeforeAll() {
        // IdentityConverter simply returns the supplied value without any
        // transformation – it is appropriate for these unit tests.
        // If the library provides a singleton, we use it; otherwise we fall back
        // to a regular Converter with default options.
        converter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        sourceMap = new HashMap<>();
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Direct Byte instance – should be returned unchanged
        Arguments.of(Byte.valueOf((byte) 5), IdentityConverter.INSTANCE, Byte.valueOf((byte) 5), "Direct Byte instance should be returned unchanged."), // Integer → Byte (narrowing cast)
        Arguments.of(Integer.valueOf(10), IdentityConverter.INSTANCE, Byte.valueOf((byte) 10), "Numeric types other than Byte are converted via narrowing cast."), // String containing a valid byte literal
        Arguments.of("127", IdentityConverter.INSTANCE, Byte.valueOf((byte) 127), "String containing a valid byte literal is parsed."), // Map with key "value"
        Arguments.of(Map.of("value", "64"), IdentityConverter.INSTANCE, Byte.valueOf((byte) 64), "Map with key \"value\" should be looked up and parsed."), // Map with short‑hand key "_v"
        Arguments.of(Map.of("_v", Byte.valueOf((byte) 2)), IdentityConverter.INSTANCE, Byte.valueOf((byte) 2), "Map with the short‑hand key defined by constant V (\"_v\") is handled."));
    }

    private static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(Arguments.of(null, IdentityConverter.INSTANCE, null, "When the source object is null the conversion should return null rather than throwing."), Arguments.of(Map.of(), IdentityConverter.INSTANCE, null, "An empty map contains no recognised keys (\"value\" or \"_v\"), so the method should return null."));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Unparseable string
        Arguments.of("abc", IdentityConverter.INSTANCE, NumberFormatException.class, "String that cannot be parsed as a byte should cause NumberFormatException."), // Out‑of‑range numeric string
        Arguments.of("128", IdentityConverter.INSTANCE, NumberFormatException.class, "String representing a numeric value outside the byte range (‑128..127) should trigger NumberFormatException."), // Boolean inside map
        Arguments.of(Map.of("value", true), IdentityConverter.INSTANCE, IllegalArgumentException.class, "Map with key \"value\" whose associated value is a Boolean cannot be converted to Byte, resulting in IllegalArgumentException."), // Unsupported source type (List)
        Arguments.of(List.of(1, 2, 3), IdentityConverter.INSTANCE, IllegalArgumentException.class, "Unsupported source type (List) should cause IllegalArgumentException."), // Null converter
        Arguments.of(Byte.valueOf((byte) 10), null, NullPointerException.class, "Passing a null Converter should result in NullPointerException."), // Arbitrary object not recognised
        Arguments.of(new Object(), IdentityConverter.INSTANCE, IllegalArgumentException.class, "An arbitrary object that is not a recognized conversion source should cause IllegalArgumentException."));
    }

    @Test
    void toByte_FromMapWithDefaultValueKey_StringNumber() {
        // given
        // value as String
        sourceMap.put(MapConversions.VALUE, "123");
        // when
        Byte result = MapConversions.toByte(sourceMap, converter);
        // then
        assertNotNull(result);
        assertEquals((byte) 123, result.byteValue());
    }

    @Test
    void toByte_FromMapWithDefaultValueKey_Integer() {
        // given
        // value as Integer
        sourceMap.put(MapConversions.VALUE, 45);
        // when
        Byte result = MapConversions.toByte(sourceMap, converter);
        // then
        assertNotNull(result);
        assertEquals((byte) 45, result.byteValue());
    }

    @Test
    void toByte_FromMapWithSpecialKey_V() {
        // given
        // value already a Byte
        sourceMap.put(MapConversions.V, (byte) 7);
        // when
        Byte result = MapConversions.toByte(sourceMap, converter);
        // then
        assertNotNull(result);
        assertEquals((byte) 7, result.byteValue());
    }

    @Test
    void toByte_FromDirectByte() {
        // given
        Byte source = (byte) 9;
        // when
        Byte result = MapConversions.toByte(source, converter);
        // then
        assertNotNull(result);
        assertEquals((byte) 9, result.byteValue());
    }

    @Test
    void toByte_NullSourceReturnsNull() {
        // when
        Byte result = MapConversions.toByte(null, converter);
        // then
        assertNull(result);
    }

    @Test
    void toByte_EmptyMapReturnsNull() {
        // when
        Byte result = MapConversions.toByte(sourceMap, converter);
        // then
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void toByte_ValidConversions(Object from, Converter conv, Byte expected, String description) {
        // when
        Byte result = MapConversions.toByte(from, conv);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("nullAndEmptyProvider")
    void toByte_NullAndEmptyInputs(Object from, Converter conv, Byte expected, String description) {
        // when
        Byte result = MapConversions.toByte(from, conv);
        // then
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionProvider")
    void toByte_ExceptionScenarios(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // then
        assertThrows(expectedException, () -> MapConversions.toByte(from, conv), description);
    }
}
