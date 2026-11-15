package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class AbstractConcurrentNullSafeMap_getOrDefault_Test {

    private TestMap<String, String> map;

    /**
     * Concrete implementation used for testing.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    /**
     * Sub‑class that deliberately passes a {@code null} internal map to the super‑constructor.
     */
    private static class NullInternalMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        NullInternalMap() {
            super(null);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap as the backing store.
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new TestMap<>(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC; the backing map will be cleared automatically.
        map = null;
    }

    private static Stream<Arguments> provideNormalScenarios() {
        return Stream.of(// Key present with non‑null value
        Arguments.of("apple", "default", "fruit"), // Key absent returns default
        Arguments.of("banana", "default", "default"), // Key present with null value returns null
        Arguments.of("carrot", "default", null), // Key present with null value and default null returns null
        Arguments.of("carrot", null, null), // Null key present with non‑null value
        Arguments.of(null, "default", "empty"), // Null key absent returns default
        Arguments.of(null, "default", "default"), // Default value is null and key absent returns null
        Arguments.of("missing", null, null), // Key present with non‑null value and default null
        Arguments.of("apple", null, "fruit"));
    }

    @Test
    void getOrDefault_ReturnsExistingNonNullValue() {
        // given
        map.put("key1", "value1");
        // when
        String result = map.getOrDefault("key1", "default");
        // then
        assertEquals("value1", result, "Should return the stored non‑null value");
    }

    @Test
    void getOrDefault_ReturnsDefaultWhenKeyMissing() {
        // given: map is empty
        // when
        String result = map.getOrDefault("missing", "default");
        // then
        assertEquals("default", result, "Should return the supplied default for missing key");
    }

    @Test
    void getOrDefault_ReturnsNullWhenStoredValueIsNull() {
        // given
        map.put("nullValueKey", null);
        // when
        String result = map.getOrDefault("nullValueKey", "default");
        // then
        assertNull(result, "Should return the stored null value, not the default");
    }

    @Test
    void getOrDefault_AllowsNullKeyAndReturnsAssociatedValue() {
        // given
        map.put(null, "nullKeyValue");
        // when
        String result = map.getOrDefault(null, "default");
        // then
        assertEquals("nullKeyValue", result, "Should correctly handle a null key");
    }

    @Test
    void getOrDefault_ReturnsDefaultWhenNullKeyNotPresent() {
        // given: map does not contain a null key
        // when
        String result = map.getOrDefault(null, "default");
        // then
        assertEquals("default", result, "Should return default when null key is absent");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests covering the normal (non‑exception) cases.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] key={0}, default={1} => expected={2}")
    @MethodSource("provideNormalScenarios")
    void getOrDefault_NormalScenarios(Object key, String defaultValue, String expected) {
        // -----------------------------------------------------------------
        // Prepare the map according to the key that appears in the scenario.
        // The JSON matrix only uses a handful of explicit keys, so we
        // initialise the backing map only for those.
        // -----------------------------------------------------------------
        if ("apple".equals(key)) {
            map.put("apple", "fruit");
        } else if ("carrot".equals(key)) {
            map.put("carrot", null);
        } else if (key == null) {
            map.put(null, "empty");
        }
        // -----------------------------------------------------------------
        // Execute the method under test.
        // -----------------------------------------------------------------
        String result = map.getOrDefault(key, defaultValue);
        // -----------------------------------------------------------------
        // Verify the outcome.
        // -----------------------------------------------------------------
        assertEquals(expected, result, () -> "Unexpected result for key=" + key + " with default=" + defaultValue);
    }

    /* --------------------------------------------------------------
     *  Exception‑related test cases.
     * -------------------------------------------------------------- */
    @Test
    void getOrDefault_IncompatibleKeyType_ThrowsClassCastException() {
        // The map is declared as <String, String>. Supplying an Integer key should
        // cause a ClassCastException before maskNullKey is invoked.
        assertThrows(ClassCastException.class, () -> map.getOrDefault(123, "default"), "Expected ClassCastException for incompatible key type");
    }

    @Test
    void getOrDefault_InternalMapIsNull_ThrowsNullPointerException() {
        // Construct a map whose internalMap field is null.
        NullInternalMap<String, String> nullMap = new NullInternalMap<>();
        assertThrows(NullPointerException.class, () -> nullMap.getOrDefault("anyKey", "default"), "Expected NullPointerException when internalMap is null");
    }

    @Test
    void getOrDefault_StoredValueIncompatibleType_ThrowsClassCastException() throws Exception {
        // Insert a raw entry with an Integer value directly into the backing map.
        // This bypasses the type‑safe put() method and simulates corrupt data.
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> backing = (ConcurrentMap<Object, Object>) internalMapField.get(map);
        // Directly store a mismatched value (Integer) for a String key.
        backing.put("mismatchKey", 42);
        // Now invoke getOrDefault – unmaskNullValue will attempt to cast 42 to String.
        assertThrows(ClassCastException.class, () -> map.getOrDefault("mismatchKey", "default"), "Expected ClassCastException when stored value type does not match V");
    }
}
