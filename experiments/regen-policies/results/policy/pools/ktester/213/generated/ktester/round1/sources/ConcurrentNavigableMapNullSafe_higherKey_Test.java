package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListMap;

class ConcurrentNavigableMapNullSafe_higherKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test and populate it with a deterministic set of keys
        map = new ConcurrentNavigableMapNullSafe<>();
        // Normal (non‑null) keys – natural ordering of String
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Null key – according to the class contract null is considered *last*
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the map to aid garbage collection; not strictly required
        map.clear();
    }

    @Test
    void higherKey_returnsNextHigherKey_forExistingNonNullKey() {
        // "a" -> next higher key should be "b"
        assertEquals("b", map.higherKey("a"));
        // "b" -> next higher key should be "c"
        assertEquals("c", map.higherKey("b"));
    }

    @Test
    void higherKey_returnsNullKey_whenHigherKeyIsNull() {
        // The map treats null as the greatest key, so the higher key of the
        // greatest non‑null key ("c") must be the null key.
        // higherKey returns the *actual* key, which is null
        assertNull(map.higherKey("c"));
        // Verify that the method indeed returns null (the sentinel is unmasked internally)
        // null is already the greatest key -> no higher key
        assertNull(map.higherKey(null));
    }

    @Test
    void higherKey_returnsNull_whenNoHigherKeyExists() {
        // The map contains only "a", "b", "c", null.
        // Asking for a key greater than null should yield null.
        assertNull(map.higherKey(null));
        // Asking for a key greater than a key that is not present but would be after "c"
        // (e.g., "d") also returns null because there is no entry after "c" (null is last).
        assertNull(map.higherKey("d"));
    }

    @Test
    void higherKey_returnsNull_onEmptyMap() {
        // Create an empty map instance
        ConcurrentNavigableMapNullSafe<String, Integer> emptyMap = new ConcurrentNavigableMapNullSafe<>();
        assertNull(emptyMap.higherKey("any"));
        assertNull(emptyMap.higherKey(null));
    }

    @Test
    void higherKey_behavesConsistentlyAfterModifications() {
        // Remove the null key and verify higherKey behavior changes accordingly
        map.remove(null);
        // Now "c" is the highest key; its higherKey should be null (no entry)
        assertNull(map.higherKey("c"));
        // Add a new key that sorts after "c"
        map.put("d", 4);
        assertEquals("d", map.higherKey("c"));
    }

    /**
     * Parameterized test for the typical case where the map holds {@code Integer}
     * keys and we ask for the next higher key.
     */
    @ParameterizedTest(name = "[{index}] higherKey({0}) = {1}")
    @CsvSource({ // inputKey, expectedHigherKey
    "1,2", "2,3" })
    void higherKey_withNonNullExistingIntegerKeys(int inputKey, int expectedHigherKey) {
        // Arrange: a map with integer keys 1,2,3 (null key is not added here)
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        // Act & Assert
        assertEquals(expectedHigherKey, intMap.higherKey(inputKey));
    }

    /**
     * Verify that providing a key of an incompatible type results in a
     * {@link ClassCastException}. The underlying {@code ConcurrentSkipListMap}
     * performs the type check during {@code higherKey}.
     */
    @Test
    void higherKey_withIncompatibleKeyType_throwsClassCastException() {
        // Arrange: map expects Integer keys
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        intMap.put(2, "two");
        intMap.put(3, "three");
        // Act & Assert
        assertThrows(ClassCastException.class, () -> intMap.higherKey((Integer) (Object) "string-key"));
    }

    /**
     * Simulate a corrupted internal state where {@code internalMap} is {@code null}.
     * The implementation casts {@code internalMap} to {@code ConcurrentSkipListMap}
     * and then calls {@code higherKey}, which should throw a {@link NullPointerException}.
     */
    @Test
    void higherKey_withNullInternalMap_throwsNullPointerException() throws Exception {
        // Arrange: create a map and then nullify its internalMap via reflection
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        // Access the protected field declared in AbstractConcurrentNullSafeMap
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        // break the internal state
        internalMapField.set(intMap, null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> intMap.higherKey(1));
    }

    /**
     * Simulate a corrupted internal state where {@code internalMap} is of the wrong
     * type (e.g., a plain {@code HashMap}). The cast to {@code ConcurrentSkipListMap}
     * inside {@code higherKey} should fail with a {@link ClassCastException}.
     */
    @Test
    void higherKey_withWrongInternalMapType_throwsClassCastException() throws Exception {
        // Arrange: create a map and replace its internalMap with a HashMap
        ConcurrentNavigableMapNullSafe<Integer, String> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, "one");
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        // wrong type
        internalMapField.set(intMap, new HashMap<>());
        // Act & Assert
        assertThrows(ClassCastException.class, () -> intMap.higherKey(1));
    }
}
