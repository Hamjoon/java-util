package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_getOrDefault_Test {

    /**
     * Sentinel used by the test subclass to represent a null key or value.
     */
    private static final Object SENTINEL = new Object();

    private TestMap map;

    private ConcurrentMap<Object, Object> internalMap;

    /**
     * Concrete test implementation of the abstract map.
     */
    private static class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        @Override
        protected Object maskNullKey(String key) {
            return key == null ? SENTINEL : key;
        }

        @Override
        protected String unmaskNullKey(Object key) {
            return key == SENTINEL ? null : (String) key;
        }

        @Override
        protected Object maskNullValue(String value) {
            return value == null ? SENTINEL : value;
        }

        @Override
        protected String unmaskNullValue(Object value) {
            return value == SENTINEL ? null : (String) value;
        }

        // The remaining abstract methods are not needed for these tests.
        // They can be left with default (no‑op) implementations because the tests
        // only interact with getOrDefault().
        @Override
        public void clear() {
        }

        @Override
        public String compute(String key, java.util.function.BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
            return null;
        }

        @Override
        public String computeIfAbsent(String key, java.util.function.Function<? super String, ? extends String> mappingFunction) {
            return null;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public java.util.Set<Entry<String, String>> entrySet() {
            return null;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public String get(Object key) {
            return null;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public java.util.Set<String> keySet() {
            return null;
        }

        @Override
        public String merge(String key, String value, java.util.function.BiFunction<? super String, ? super String, ? extends String> remappingFunction) {
            return null;
        }

        @Override
        public String put(String key, String value) {
            return null;
        }

        @Override
        public void putAll(java.util.Map<? extends String, ? extends String> m) {
        }

        @Override
        public String putIfAbsent(String key, String value) {
            return null;
        }

        @Override
        public String remove(Object key) {
            return null;
        }

        @Override
        public boolean remove(Object key, Object value) {
            return false;
        }

        @Override
        public String replace(String key, String value) {
            return null;
        }

        @Override
        public boolean replace(String key, String oldValue, String newValue) {
            return false;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public java.util.Collection<String> values() {
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        internalMap = new ConcurrentHashMap<>();
        map = new TestMap(internalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        internalMap.clear();
    }

    /**
     * Parameter source for the "Key present – non‑null value" group.
     * Each element is an array: {key, defaultValue, expectedResult, description}
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> keyPresentProvider() {
        return Stream.of(// case 1 – normal non‑null key/value
        org.junit.jupiter.params.provider.Arguments.of(// key
        "alpha", // defaultValue
        "default", // expected result
        "alphaValue", "Map contains the key \"alpha\" with a non‑null value \"alphaValue\"."), // case 2 – null key with non‑null value
        org.junit.jupiter.params.provider.Arguments.of(// key
        null, // defaultValue
        "defaultForNull", // expected result
        "nullKeyValue", "Map contains a mapping for a null key with a non‑null value \"nullKeyValue\"."));
    }

    @Test
    void getOrDefault_returnsExistingValue() {
        // Arrange
        // normal (non‑null) entry
        internalMap.put("key1", "value1");
        // Act
        String result = map.getOrDefault("key1", "default");
        // Assert
        assertEquals("value1", result, "Should return the stored value when key is present");
    }

    @Test
    void getOrDefault_returnsNullWhenStoredValueIsNull() {
        // Arrange
        // Store a null value using the maskNullValue logic (sentinel)
        internalMap.put("key2", SENTINEL);
        // Act
        String result = map.getOrDefault("key2", "default");
        // Assert
        assertNull(result, "Should return null (the stored value) rather than the default");
    }

    @Test
    void getOrDefault_returnsDefaultWhenKeyAbsent() {
        // Arrange: internalMap is empty
        // Act
        String result = map.getOrDefault("missing", "default");
        // Assert
        assertEquals("default", result, "Should return the supplied default when key is absent");
    }

    @Test
    void getOrDefault_handlesNullKeyPresent() {
        // Arrange
        // entry for a null key
        internalMap.put(SENTINEL, "nullKeyValue");
        // Act
        String result = map.getOrDefault(null, "default");
        // Assert
        assertEquals("nullKeyValue", result, "Should correctly retrieve value associated with a null key");
    }

    @Test
    void getOrDefault_handlesNullKeyAbsent() {
        // Arrange: no entry for the sentinel key
        // Act
        String result = map.getOrDefault(null, "default");
        // Assert
        assertEquals("default", result, "Should return default when null key is not present");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("keyPresentProvider")
    void getOrDefault_keyPresentNonNullValue(String key, String defaultValue, String expected, String description) {
        // Arrange
        if (key == null) {
            // store entry for a null key using the sentinel
            internalMap.put(SENTINEL, "nullKeyValue");
        } else {
            // store the expected value under the provided key
            internalMap.put(key, expected);
        }
        // Act
        String result = map.getOrDefault(key, defaultValue);
        // Assert
        assertEquals(expected, result, description);
    }

    @Test
    void getOrDefault_invalidKeyType_throwsClassCastException() {
        // Arrange
        Object invalidKey = Integer.valueOf(123);
        String defaultValue = "default";
        // Act & Assert
        Executable call = () -> map.getOrDefault(invalidKey, defaultValue);
        assertThrows(ClassCastException.class, call, "Providing a key of the wrong type (Integer) should cause a ClassCastException.");
    }

    @Test
    void getOrDefault_nullInternalMap_throwsNullPointerException() {
        // Arrange: create a map whose internalMap field is deliberately null
        TestMap mapWithNullInternal = new TestMap(null);
        // Act & Assert
        Executable call = () -> mapWithNullInternal.getOrDefault("anyKey", "default");
        assertThrows(NullPointerException.class, call, "Calling getOrDefault when internalMap is null should throw NullPointerException.");
    }
}
