package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_remove_Test {

    private ConcurrentMap<Object, Object> internalMap;

    private AbstractConcurrentNullSafeMap<String, String> map;

    /**
     * Simple concrete subclass used only for testing.
     */
    private static final class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
        // The abstract class does not provide any additional abstract methods,
        // so no further implementation is required.
    }

    /**
     * Subclass that deliberately passes a null internal map to provoke NPE.
     */
    private static final class NullInternalMapMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        NullInternalMapMap() {
            super(null);
        }
    }

    private static class ArgumentsHolder {

        final Object key;

        final String expected;

        final String description;

        ArgumentsHolder(Object key, String expected, String description) {
            this.key = key;
            this.expected = expected;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap as the backing store.
        internalMap = new ConcurrentHashMap<>();
        map = new TestMap<>(internalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared between tests to avoid cross‑test contamination.
        internalMap.clear();
    }

    private static Stream<ArgumentsHolder> removalScenarios() {
        return Stream.of(// remove existing key with non‑null value
        new ArgumentsHolder("alpha", "firstValue", "remove existing key with non‑null value"), // remove existing key with null value
        new ArgumentsHolder("beta", null, "remove existing key with null value"), // remove null key (present)
        new ArgumentsHolder(null, "nullKeyValue", "remove null key"), // remove non‑existent key
        new ArgumentsHolder("missing", null, "remove non‑existent key"), // remove key equal but not identical
        new ArgumentsHolder(new String("alpha"), "firstValue", "remove key that is equal but not identical"), // remove one of multiple entries (beta -> null)
        new ArgumentsHolder("beta", null, "remove one of multiple entries"));
    }

    @Test
    void removeExistingKey_returnsPreviousValueAndRemovesEntry() {
        // given
        String key = "alpha";
        String value = "first";
        // uses the default put implementation from the abstract class
        map.put(key, value);
        // when
        String removed = map.remove(key);
        // then
        assertEquals(value, removed, "remove should return the previous value");
        assertFalse(map.containsKey(key), "key should no longer be present after removal");
        assertNull(map.get(key), "get should return null for a removed key");
    }

    @Test
    void removeNonExistingKey_returnsNull() {
        // given
        String key = "nonexistent";
        // when
        String removed = map.remove(key);
        // then
        assertNull(removed, "remove of a missing key should return null");
        assertFalse(map.containsKey(key), "map should not contain the key");
    }

    @Test
    void removeNullKey_handlesNullKeyCorrectly() {
        // given
        String value = "nullKeyValue";
        // null key is allowed via maskNullKey
        map.put(null, value);
        // when
        String removed = map.remove(null);
        // then
        assertEquals(value, removed, "remove should return the value associated with the null key");
        assertFalse(map.containsKey(null), "null key should be removed");
        assertNull(map.get(null), "get should return null after null key removal");
    }

    @Test
    void removeNullKeyWhenAbsent_returnsNull() {
        // given: map is empty, no null key present
        // when
        String removed = map.remove(null);
        // then
        assertNull(removed, "remove should return null when null key is absent");
        assertFalse(map.containsKey(null));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("removalScenarios")
    void removeVariousScenarios(ArgumentsHolder holder) {
        // -----------------------------------------------------------------
        // Arrange – set up the map according to the scenario description
        // -----------------------------------------------------------------
        switch(holder.description) {
            case "remove existing key with non‑null value":
                map.put("alpha", "firstValue");
                break;
            case "remove existing key with null value":
                map.put("beta", null);
                break;
            case "remove null key":
                map.put(null, "nullKeyValue");
                break;
            case "remove non‑existent key":
                // map stays empty
                break;
            case "remove key that is equal but not identical":
                map.put("alpha", "firstValue");
                break;
            case "remove one of multiple entries":
                map.put("alpha", "firstValue");
                map.put("beta", null);
                map.put("gamma", "thirdValue");
                break;
            default:
                fail("Unexpected description: " + holder.description);
        }
        // -----------------------------------------------------------------
        // Act
        // -----------------------------------------------------------------
        String removed = map.remove(holder.key);
        // -----------------------------------------------------------------
        // Assert
        // -----------------------------------------------------------------
        assertEquals(holder.expected, removed, "Returned value does not match expectation for scenario: " + holder.description);
        // Verify that the key is no longer present
        assertFalse(map.containsKey(holder.key), "Key should be absent after removal in scenario: " + holder.description);
        // Additional size checks for multi‑entry scenario
        if ("remove one of multiple entries".equals(holder.description)) {
            assertEquals(2, map.size(), "Map size should decrease by one after removing 'beta'");
            assertTrue(map.containsKey("alpha"));
            assertTrue(map.containsKey("gamma"));
        }
    }

    @Test
    void removeAfterPreviousRemoval_isIdempotent() {
        // given
        map.put("gamma", "someValue");
        // first removal
        map.remove("gamma");
        // when
        String secondRemoval = map.remove("gamma");
        // then
        assertNull(secondRemoval, "Second removal should return null");
        assertFalse(map.containsKey("gamma"));
    }

    @Test
    void removeFromEmptyMap_returnsNull() {
        // when
        String removed = map.remove("anyKey");
        // then
        assertNull(removed);
        assertTrue(map.isEmpty(), "Map should remain empty");
    }

    @Test
    void removeWithIncompatibleKey_throwsClassCastException() {
        // given: map is declared with K = String
        // when / then
        assertThrows(ClassCastException.class, () -> map.remove(123), "Removing with an Integer key should throw ClassCastException");
    }

    @Test
    void removeWithMismatchedUnderlyingValue_throwsClassCastException() {
        // given: directly insert a mismatched value into internalMap
        // Integer instead of String
        internalMap.put("alpha", 42);
        // when / then
        assertThrows(ClassCastException.class, () -> map.remove("alpha"), "Unmasking a non‑String value should throw ClassCastException");
    }

    @Test
    void removeWhenInternalMapIsNull_throwsNullPointerException() {
        // given: map with null internalMap
        AbstractConcurrentNullSafeMap<String, String> brokenMap = new NullInternalMapMap<>();
        // when / then
        assertThrows(NullPointerException.class, () -> brokenMap.remove("anyKey"), "Calling remove on a map with null internalMap should throw NPE");
    }
}
