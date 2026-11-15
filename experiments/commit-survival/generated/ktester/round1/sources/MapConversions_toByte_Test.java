package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.api.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

class MapConversions_toByte_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – use the default constructor.
        // If a different constructor is needed, adjust accordingly.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC when many tests run.
        converter = null;
    }

    /**
     * Supplies the valid conversion scenarios described in the JSON spec.
     */
    private static Stream<Arguments> validConversions() {
        return Stream.of(// Direct Byte instance – identity conversion
        Arguments.of(Byte.valueOf((byte) 7), createIdentityConverter(), Byte.valueOf((byte) 7)), // Integer within byte range
        Arguments.of(Integer.valueOf(42), createIdentityConverter(), Byte.valueOf((byte) 42)), // Long within byte range
        Arguments.of(Long.valueOf(120L), createIdentityConverter(), Byte.valueOf((byte) 120)), // String parsed by a simple string‑to‑number converter
        Arguments.of("15", createStringToNumberConverter(), Byte.valueOf((byte) 15)), // Map with key "value" holding a numeric value
        Arguments.of(mapWithEntry(MapConversions.VALUE, 9), createIdentityConverter(), Byte.valueOf((byte) 9)), // Map with key "_v" holding a numeric string
        Arguments.of(mapWithEntry(MapConversions.V, "-3"), createStringToNumberConverter(), Byte.valueOf((byte) -3)), // Null source should yield null
        Arguments.of(null, createIdentityConverter(), null));
    }

    /**
     * Returns a mock {@code Converter} that simply returns the supplied source unchanged.
     */
    private static Converter createIdentityConverter() {
        Converter mock = Mockito.mock(Converter.class);
        // The real Converter API is generic; we mock the most common signature.
        Mockito.when(mock.convert(any(), any(Class.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return mock;
    }

    /**
     * Returns a mock {@code Converter} that parses a {@link String} to a {@link Byte}
     * using {@link Byte#valueOf(String)}. For any other source type it behaves like
     * the identity converter.
     */
    private static Converter createStringToNumberConverter() {
        Converter mock = Mockito.mock(Converter.class);
        // String → Byte parsing
        Mockito.when(mock.convert(anyString(), eq(Byte.class))).thenAnswer(invocation -> Byte.valueOf((String) invocation.getArgument(0)));
        // Fallback for other types – just return the argument unchanged.
        Mockito.when(mock.convert(any(), any(Class.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return mock;
    }

    /**
     * Utility to build a {@code CompactLinkedMap} with a single entry.
     */
    private static Map<String, Object> mapWithEntry(String key, Object value) {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put(key, value);
        return map;
    }

    @Test
    void toByte_fromMapWithNumericValue_returnsByte() {
        // GIVEN
        Map<String, Object> map = new CompactLinkedMap<>();
        // an Integer that fits in a byte
        map.put(MapConversions.VALUE, 123);
        // WHEN
        Byte result = MapConversions.toByte(map, converter);
        // THEN
        assertEquals(Byte.valueOf((byte) 123), result);
    }

    @Test
    void toByte_fromMapWithStringValue_returnsByte() {
        // GIVEN
        Map<String, Object> map = new CompactLinkedMap<>();
        // a String representation of a number
        map.put(MapConversions.VALUE, "45");
        // WHEN
        Byte result = MapConversions.toByte(map, converter);
        // THEN
        assertEquals(Byte.valueOf((byte) 45), result);
    }

    @Test
    void toByte_fromMapWithByteValue_returnsSameByte() {
        // GIVEN
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put(MapConversions.VALUE, Byte.valueOf((byte) -12));
        // WHEN
        Byte result = MapConversions.toByte(map, converter);
        // THEN
        assertEquals(Byte.valueOf((byte) -12), result);
    }

    @Test
    void toByte_fromNullMap_returnsNull() {
        // GIVEN
        Map<String, Object> map = null;
        // WHEN
        Byte result = MapConversions.toByte(map, converter);
        // THEN
        assertNull(result);
    }

    @Test
    void toByte_fromMapMissingValueKey_returnsNull() {
        // GIVEN
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("someOtherKey", 99);
        // WHEN
        Byte result = MapConversions.toByte(map, converter);
        // THEN
        // The conversion logic treats a missing “value” key as null → result should be null.
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] from={0} expected={2}")
    @MethodSource("validConversions")
    void toByte_validConversions(Object from, Converter customConverter, Byte expected) {
        // WHEN
        Byte actual = MapConversions.toByte(from, customConverter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toByte_missingKeyInMap_throwsIllegalArgumentException() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("other", 5);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toByte(map, createIdentityConverter()));
    }

    @Test
    void toByte_invalidValueTypeInMap_throwsIllegalArgumentException() {
        Map<String, Object> map = new CompactLinkedMap<>();
        // Boolean is not a valid numeric source
        map.put(MapConversions.VALUE, true);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toByte(map, createIdentityConverter()));
    }

    @Test
    void toByte_nonNumericString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> MapConversions.toByte("abc", createStringToNumberConverter()));
    }

    @Test
    void toByte_decimalString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> MapConversions.toByte("12.34", createStringToNumberConverter()));
    }

    @Test
    void toByte_numberOutOfByteRange_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toByte(Integer.valueOf(200), createIdentityConverter()));
    }

    @Test
    void toByte_nullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toByte(Byte.valueOf((byte) 5), null));
    }

    @Test
    void toByte_unsupportedSourceType_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toByte(new Object(), createIdentityConverter()));
    }

    @Test
    void toByte_mapWithNullValue_throwsNullPointerException() {
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put(MapConversions.VALUE, null);
        assertThrows(NullPointerException.class, () -> MapConversions.toByte(map, createIdentityConverter()));
    }
}
