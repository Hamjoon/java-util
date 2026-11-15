package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class VoidConversions_toBoolean_Test {

    /**
     * A mock {@link Converter} used for all test cases.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Provider for the primary "toBoolean" group.
     */
    private static Stream<Arguments> toBooleanProvider() {
        return Stream.of(// Both arguments null
        Arguments.of(null, null, false, "Both arguments are null; the method should ignore them and always return Boolean.FALSE."), // Non‑null String, null converter
        Arguments.of("some string", null, false, "The 'from' argument is a non‑null String and converter is null; method must still return Boolean.FALSE."), // Integer with mock converter
        Arguments.of(42, getMockConverterPlaceholder(), false, "The 'from' argument is an Integer and a mock Converter is supplied; method should not use the converter and return Boolean.FALSE."), // Boolean true with mock converter
        Arguments.of(Boolean.TRUE, getMockConverterPlaceholder(), false, "The 'from' argument is a Boolean true; regardless of its value the method always returns Boolean.FALSE."), // Custom object (represented as a Map) with mock converter
        Arguments.of(customMap("customObject", "value"), getMockConverterPlaceholder(), false, "The 'from' argument is a custom object (represented as a JSON object); method must still return Boolean.FALSE."));
    }

    /**
     * Provider for the "toBoolean_additional" group.
     */
    private static Stream<Arguments> toBooleanAdditionalProvider() {
        return Stream.of(// Null from, non‑null mock converter
        Arguments.of(null, getMockConverterPlaceholder(), false, "Both arguments are null for 'from' and a non‑null Converter is supplied; method must still ignore them and return Boolean.FALSE."), // Boolean.FALSE with null converter
        Arguments.of(Boolean.FALSE, null, false, "The 'from' argument is Boolean.FALSE; regardless of its value the method always returns Boolean.FALSE."), // Empty String with null converter
        Arguments.of("", null, false, "The 'from' argument is an empty String; method must still return Boolean.FALSE."), // int array wrapped in a Map with mock converter
        Arguments.of(customMap("array", new int[] { 1, 2, 3 }), getMockConverterPlaceholder(), false, "The 'from' argument is an int array wrapped in a JSON object; method should ignore the content and return Boolean.FALSE."), // List wrapped in a Map with null converter
        Arguments.of(customMap("list", Arrays.asList("a", "b")), null, false, "The 'from' argument is a List represented as a JSON object; method must still return Boolean.FALSE."), // Double value with mock converter
        Arguments.of(3.14, getMockConverterPlaceholder(), false, "The 'from' argument is a Double value; method should ignore it and return Boolean.FALSE."), // Character represented as a single‑character String with mock converter
        Arguments.of("c", getMockConverterPlaceholder(), false, "The 'from' argument is a Character (represented as a single‑character String); method must still return Boolean.FALSE."));
    }

    /**
     * Returns the {@link Converter} mock that will be injected by Mockito.
     * The placeholder is used in the static data providers where a direct reference
     * to the instance field is not possible.
     */
    private static Converter getMockConverterPlaceholder() {
        // The actual mock instance will be injected by Mockito at runtime.
        // In the data provider we cannot reference the non‑static field, so we
        // return {@code null} as a sentinel. The test method will replace it with
        // the real mock if necessary.
        return null;
    }

    /**
     * Convenience factory for a simple {@link Map} representing a JSON‑style object.
     *
     * @param key   the map key
     * @param value the map value (may be another collection, array, etc.)
     * @return a mutable {@link HashMap} containing the given entry
     */
    private static Map<String, Object> customMap(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }

    @Test
    void toBoolean_returnsFalse_whenFromIsNull() {
        // GIVEN
        Object from = null;
        // WHEN
        Boolean result = VoidConversions.toBoolean(from, mockConverter);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(Boolean.FALSE, result, "toBoolean should always return Boolean.FALSE");
    }

    @Test
    void toBoolean_returnsFalse_whenFromIsNonNull() {
        // GIVEN
        Object from = "any non‑null value";
        // WHEN
        Boolean result = VoidConversions.toBoolean(from, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void toBoolean_returnsFalse_whenConverterIsNull() {
        // GIVEN
        Object from = 123;
        Converter nullConverter = null;
        // WHEN
        Boolean result = VoidConversions.toBoolean(from, nullConverter);
        // THEN
        assertNotNull(result);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void toBoolean_isIdempotent_forMultipleCalls() {
        // GIVEN
        Object from = new Object();
        // WHEN / THEN
        for (int i = 0; i < 5; i++) {
            Boolean result = VoidConversions.toBoolean(from, mockConverter);
            assertEquals(Boolean.FALSE, result, "Each call must return Boolean.FALSE");
        }
    }

    /**
     * Parameterized test covering all cases from the "toBoolean" group.
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("toBooleanProvider")
    void toBoolean_parameterized(Object from, Converter converter, boolean expected, String description) {
        // WHEN
        Boolean result = VoidConversions.toBoolean(from, converter);
        // THEN
        assertNotNull(result, description);
        assertEquals(Boolean.valueOf(expected), result, description);
    }

    /**
     * Parameterized test covering all cases from the "toBoolean_additional" group.
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("toBooleanAdditionalProvider")
    void toBooleanAdditional_parameterized(Object from, Converter converter, boolean expected, String description) {
        // WHEN
        Boolean result = VoidConversions.toBoolean(from, converter);
        // THEN
        assertNotNull(result, description);
        assertEquals(Boolean.valueOf(expected), result, description);
    }
}
