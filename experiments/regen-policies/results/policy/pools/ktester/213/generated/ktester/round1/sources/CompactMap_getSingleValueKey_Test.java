package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class CompactMap_getSingleValueKey_Test {

    /**
     * Simple subclass exposing the protected {@code getSingleValueKey()}
     * method for testing.
     */
    private static class ExposedMap<K, V> extends CompactMap<K, V> {

        public K exposeSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    /**
     * Subclass that overrides the single‑value key.
     */
    private static class CustomKeyMap<K, V> extends CompactMap<K, V> {

        private final K customKey;

        CustomKeyMap(K customKey) {
            this.customKey = customKey;
        }

        @Override
        protected K getSingleValueKey() {
            return customKey;
        }

        public K exposeSingleValueKey() {
            return super.getSingleValueKey();
        }
    }

    private static Stream<Arguments> defaultKeyProvider() {
        return Stream.of(Arguments.of("Default key should be \"id\"", CompactMap.DEFAULT_SINGLE_KEY));
    }

    @Test
    void defaultSingleValueKey_isId() {
        // GIVEN a plain CompactMap instance that does not override the method
        ExposedMap<String, Object> map = new ExposedMap<>();
        // WHEN we retrieve the single‑value key via the exposed method
        String key = map.exposeSingleValueKey();
        // THEN the key must be the default constant ("id")
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, key);
    }

    @Test
    void overriddenSingleValueKey_isReturnedAndReflectedInConfig() {
        // GIVEN a CompactMap that overrides the single‑value key to "uuid"
        String custom = "uuid";
        CustomKeyMap<String, Object> map = new CustomKeyMap<>(custom);
        // WHEN we call the exposed method
        String keyFromMethod = map.exposeSingleValueKey();
        // THEN the overridden key is returned
        assertEquals(custom, keyFromMethod);
        // AND WHEN we ask the map for its configuration
        // (the config map includes the SINGLE_KEY entry)
        java.util.Map<String, Object> config = map.getConfig();
        // THEN the config must contain the custom key under the SINGLE_KEY name
        assertTrue(config.containsKey(CompactMap.SINGLE_KEY));
        assertEquals(custom, config.get(CompactMap.SINGLE_KEY));
    }

    @Test
    void defaultSingleValueKey_multipleCalls_returnSameInstance() {
        // GIVEN a map with default configuration
        ExposedMap<String, Object> map = new ExposedMap<>();
        // WHEN we call the method twice
        String first = map.exposeSingleValueKey();
        String second = map.exposeSingleValueKey();
        // THEN both calls return the same object reference (the constant)
        assertSame(first, second);
    }

    @Test
    void defaultSingleValueKey_unchangedAfterMutations() {
        // GIVEN a map that undergoes mutating operations
        ExposedMap<String, Object> map = new ExposedMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.remove("a");
        map.clear();
        // WHEN we request the single‑value key after the mutations
        String key = map.exposeSingleValueKey();
        // THEN the key is still the default constant
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, key);
    }

    @Test
    void typeSafety_classCastException_whenKeyTypeIsInteger() {
        // GIVEN a map whose generic key type is Integer
        ExposedMap<Integer, Object> map = new ExposedMap<>();
        // WHEN we invoke the method we expect a ClassCastException because
        // the implementation returns a String ("id") cast to Integer
        assertThrows(ClassCastException.class, map::exposeSingleValueKey);
    }

    @Test
    void typeSafety_rawGeneric_returnsStringWithoutException() {
        // GIVEN a map with raw generic types (Object, Object)
        ExposedMap<Object, Object> map = new ExposedMap<>();
        // WHEN we invoke the method
        Object key = map.exposeSingleValueKey();
        // THEN the returned object is the default string constant
        assertTrue(key instanceof String);
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, key);
    }

    /**
     * Demonstrates how additional scenarios could be added via a parameterised test.
     * Currently it supplies the default key for a {@code CompactMap<String,?>}.
     */
    @ParameterizedTest
    @MethodSource("defaultKeyProvider")
    void parameterised_defaultKey(String description, String expected) {
        ExposedMap<String, Object> map = new ExposedMap<>();
        assertEquals(expected, map.exposeSingleValueKey(), description);
    }
}