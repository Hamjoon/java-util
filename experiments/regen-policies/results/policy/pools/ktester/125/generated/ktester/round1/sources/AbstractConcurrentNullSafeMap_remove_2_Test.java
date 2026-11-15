package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AbstractConcurrentNullSafeMap_remove_2_Test {

    private TestMap<String, String> map;

    /**
     * Simple concrete implementation that uses a {@link ConcurrentHashMap} as the backing store.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// Remove an existing entry with matching key and value; should return true.
        arguments("a", "b", true), // Key exists but value does not match; remove should return false and entry remains.
        arguments("a", "c", false), // Remove entry where key is null (masked internally); should succeed and return true.
        arguments(null, "b", true), // Remove entry where value is null (masked internally); should succeed and return true.
        arguments("a", null, true), // Remove entry where both key and value are null; should succeed and return true.
        arguments(null, null, true));
    }

    private static Stream<Arguments> nonExistentOrMismatchedProvider() {
        return Stream.of(// Key does not exist in the map; remove should return false.
        arguments("nonexistent", "any", false), // Key "a" exists with a non‑null value (e.g., "b"); attempting to remove with a null value should fail.
        arguments("a", null, false), // Map contains a null key associated with a different non‑null value; removal with a mismatching value should return false.
        arguments(null, "x", false), // Map contains a null key with a non‑null value; attempting to remove with both key and value null should return false.
        arguments(null, null, false));
    }

    private static Stream<Arguments> typeMismatchProvider() {
        return Stream.of(// Key is Integer, value is String
        arguments(123, Integer.class, "b", String.class), // Key is String, value is Integer
        arguments("a", String.class, 123, Integer.class), // Both key and value are Integer
        arguments(123, Integer.class, 456, Integer.class));
    }

    @Test
    void removeExistingEntryReturnsTrueAndRemovesIt() {
        // given
        map.put("key1", "value1");
        map.put("key2", "value2");
        // when
        boolean removed = map.remove("key1", "value1");
        // then
        assertTrue(removed, "remove should return true for an existing key/value pair");
        assertFalse(map.containsKey("key1"), "key1 should no longer be present after removal");
        assertTrue(map.containsKey("key2"), "other entries must remain untouched");
    }

    @Test
    void removeWithMismatchedValueReturnsFalseAndLeavesEntry() {
        // given
        map.put("key", "original");
        // when
        boolean removed = map.remove("key", "different");
        // then
        assertFalse(removed, "remove should return false when the value does not match");
        assertTrue(map.containsKey("key"), "the entry must remain because removal failed");
        assertEquals("original", map.get("key"), "original value must be unchanged");
    }

    @Test
    void removeNullKeyAndNullValueWorksCorrectly() {
        // given
        map.put(null, null);
        map.put("nonNullKey", null);
        map.put(null, "nonNullValue");
        // when & then – null key / null value pair
        assertTrue(map.remove(null, null), "remove should succeed for null key/null value pair");
        assertFalse(map.containsKey(null), "null key should be removed");
        // when & then – non‑null key / null value pair
        assertTrue(map.remove("nonNullKey", null), "remove should succeed for non‑null key with null value");
        assertFalse(map.containsKey("nonNullKey"), "nonNullKey should be removed");
        // when & then – null key / non‑null value pair
        assertTrue(map.remove(null, "nonNullValue"), "remove should succeed for null key with non‑null value");
        assertFalse(map.containsKey(null), "null key should be removed again");
    }

    @Test
    void removeNonExistingKeyReturnsFalse() {
        // given: map is empty
        // when
        boolean removed = map.remove("absent", "any");
        // then
        assertFalse(removed, "remove must return false when the key is not present");
    }

    /**
     * Group: basic functionality
     */
    @ParameterizedTest(name = "{index}: remove(key={0}, value={1}) → expected={2}")
    @MethodSource("basicFunctionalityProvider")
    void removeBasicFunctionality(Object key, Object value, boolean expected) {
        // Populate the map according to the scenario
        // The map is cleared before each test by @AfterEach, so we only need to add the relevant entry.
        if ("a".equals(key) && "b".equals(value)) {
            map.put("a", "b");
        } else if ("a".equals(key) && "c".equals(value)) {
            // mismatched value case
            map.put("a", "b");
        } else if (key == null && "b".equals(value)) {
            map.put(null, "b");
        } else if ("a".equals(key) && value == null) {
            map.put("a", null);
        } else if (key == null && value == null) {
            map.put(null, null);
        }
        boolean result = map.remove(key, value);
        assertEquals(expected, result, "remove returned unexpected result");
        // Verify side‑effects when removal succeeded
        if (expected) {
            assertFalse(map.containsKey(key), "Key should be absent after successful removal");
        } else {
            // entry should still be present
            assertTrue(map.containsKey(key), "Key should remain after failed removal");
        }
    }

    /**
     * Group: non‑existent key and mismatched values
     */
    @ParameterizedTest(name = "{index}: remove(key={0}, value={1}) → expected={2}")
    @MethodSource("nonExistentOrMismatchedProvider")
    void removeNonExistentOrMismatched(Object key, Object value, boolean expected) {
        // Set up map according to each case
        if ("a".equals(key) && !"null".equals(String.valueOf(value))) {
            // map contains "a" -> "b"
            map.put("a", "b");
        } else if (key == null && "x".equals(value)) {
            // map contains null -> "y"
            map.put(null, "y");
        } else if (key == null && value == null) {
            // map contains null -> "something"
            map.put(null, "something");
        }
        // For the "nonexistent" case we leave the map empty.
        boolean result = map.remove(key, value);
        assertEquals(expected, result, "remove returned unexpected result");
        // Ensure map state is unchanged when removal fails
        if (!expected) {
            if ("a".equals(key)) {
                assertTrue(map.containsKey("a"));
                assertEquals("b", map.get("a"));
            }
            if (key == null) {
                assertTrue(map.containsKey(null));
            }
        }
    }

    /**
     * Group: type mismatch exceptions
     */
    @ParameterizedTest(name = "{index}: remove(key={0} ({1}), value={2} ({3})) → expect ClassCastException")
    @MethodSource("typeMismatchProvider")
    void removeTypeMismatch(Object key, Class<?> keyClass, Object value, Class<?> valueClass) {
        // No need to pre‑populate the map; the cast exception occurs before any map access.
        assertThrows(ClassCastException.class, () -> map.remove(key, value), () -> String.format("Expected ClassCastException for key type %s and value type %s", keyClass.getSimpleName(), valueClass.getSimpleName()));
    }
}
