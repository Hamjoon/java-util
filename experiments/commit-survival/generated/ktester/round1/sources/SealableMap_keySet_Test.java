package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableMap_keySet_Test {

    /**
     * The map under test.
     */
    private SealableMap<String, Integer> map;

    /**
     * Mutable flag used by the {@link #sealedSupplier} to indicate the sealed state.
     */
    private volatile boolean sealedState = false;

    /**
     * Supplier that returns the current sealed state.
     */
    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test and populate it with deterministic data.
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, null);
        // Ensure the map starts in an unsealed state.
        sealedState = false;
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help the garbage collector; not strictly necessary.
        map = null;
    }

    private static Stream<Arguments> basicBehaviorProvider() {
        return Stream.of(Arguments.of("Non‑empty map, unsealed", mapOf("a", 1, "b", 2), false), Arguments.of("Empty map, unsealed", Collections.emptyMap(), false));
    }

    private static Stream<Arguments> sealedBehaviorProvider() {
        return Stream.of(Arguments.of("Sealed map with entries", mapOf("x", 1, "y", 2), true), Arguments.of("Sealed empty map", Collections.emptyMap(), true));
    }

    /**
     * The keySet should never be {@code null} and must contain the keys that were put into the map.
     */
    @Test
    void testKeySetIsNotNullAndContainsExistingKeys() {
        Set<String> keys = map.keySet();
        assertNotNull(keys, "keySet() must not return null");
        assertTrue(keys.contains("one"));
        assertTrue(keys.contains("two"));
        assertTrue(keys.contains("three"));
        // The map also contains a null key, which should be present in the set.
        assertTrue(keys.contains(null));
    }

    /**
     * When the map is unsealed, the keySet should allow modifications that affect the underlying map.
     */
    @Test
    void testKeySetModificationWhenUnsealed() {
        Set<String> keys = map.keySet();
        // Remove an existing key.
        assertTrue(keys.remove("two"));
        assertNull(map.get("two"));
        assertFalse(keys.contains("two"));
        // Add a new key via the underlying map and verify the set reflects it.
        map.put("four", 4);
        assertTrue(keys.contains("four"));
    }

    /**
     * When the map is sealed, any mutating operation on the keySet must throw {@link UnsupportedOperationException}.
     */
    @Test
    void testKeySetModificationWhenSealed() {
        Set<String> keys = map.keySet();
        // Seal the map.
        sealedState = true;
        // Attempt to remove a key.
        assertThrows(UnsupportedOperationException.class, () -> keys.remove("one"));
        // Attempt to clear the set.
        assertThrows(UnsupportedOperationException.class, keys::clear);
        // Attempt to add a key (the set returned by SealableMap is unmodifiable, but we test the contract).
        assertThrows(UnsupportedOperationException.class, () -> keys.add("five"));
    }

    /**
     * The keySet view must stay in sync with the map after mutations performed directly on the map.
     */
    @Test
    void testKeySetReflectsDirectMapChanges() {
        Set<String> keys = map.keySet();
        // Directly modify the map.
        map.put("alpha", 100);
        map.remove("one");
        assertTrue(keys.contains("alpha"));
        assertFalse(keys.contains("one"));
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicBehaviorProvider")
    void testKeySetBasicBehavior(String description, Map<String, Integer> initialMap, boolean sealed) {
        // Prepare map with the supplied initial data
        map = new SealableMap<>(sealedSupplier);
        initialMap.forEach(map::put);
        // false for these cases
        sealedState = sealed;
        Set<String> keySet = map.keySet();
        // Verify the set reflects the initial keys
        assertEquals(initialMap.keySet(), keySet, "keySet should contain exactly the initial keys");
        // Mutate via the set (only allowed when unsealed)
        if (!sealed) {
            // remove first key (if any)
            if (!initialMap.isEmpty()) {
                String firstKey = initialMap.keySet().iterator().next();
                assertTrue(keySet.remove(firstKey), "remove should succeed when unsealed");
                assertNull(map.get(firstKey), "underlying map must no longer contain the removed key");
            }
        } else {
            assertThrows(UnsupportedOperationException.class, () -> keySet.remove("any"));
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("sealedBehaviorProvider")
    void testKeySetWhenSealedImmutable(String description, Map<String, Integer> initialMap, boolean sealed) {
        map = new SealableMap<>(sealedSupplier);
        initialMap.forEach(map::put);
        // true
        sealedState = sealed;
        Set<String> keySet = map.keySet();
        // The set must contain the expected keys
        assertEquals(initialMap.keySet(), keySet, "Sealed keySet should contain the initial keys");
        // All mutating operations must throw
        assertAll("mutating operations", () -> assertThrows(UnsupportedOperationException.class, () -> keySet.remove("x")), () -> assertThrows(UnsupportedOperationException.class, () -> keySet.add("z")), () -> assertThrows(UnsupportedOperationException.class, keySet::clear));
    }

    @Test
    void testKeySetReflectsMapChangesAfterSealing() {
        // Initial map with one entry, then seal it
        map = new SealableMap<>(sealedSupplier);
        map.put("k1", 1);
        // sealed
        sealedState = true;
        Set<String> keySet = map.keySet();
        assertTrue(keySet.contains("k1"));
        assertEquals(1, keySet.size());
        // Add a new entry directly to the underlying map
        map.put("k2", 2);
        assertTrue(keySet.contains("k2"));
        assertEquals(2, keySet.size());
        // Mutating via the set must still be prohibited
        assertThrows(UnsupportedOperationException.class, () -> keySet.remove("k1"));
    }

    @Test
    void testKeySetWithStringNullKey() {
        map = new SealableMap<>(sealedSupplier);
        // literal string "null"
        map.put("null", null);
        map.put("a", 1);
        sealedState = false;
        Set<String> keySet = map.keySet();
        assertTrue(keySet.contains("null"));
        assertTrue(keySet.contains("a"));
        // Remove the literal "null" key via the set
        assertTrue(keySet.remove("null"));
        assertFalse(keySet.contains("null"));
        assertNull(map.get("null"));
        assertEquals(1, map.size());
    }

    @Test
    void testKeySetWithoutInitialNullKeyAndAddLater() {
        map = new SealableMap<>(sealedSupplier);
        map.put("a", 1);
        sealedState = false;
        Set<String> keySet = map.keySet();
        assertFalse(keySet.contains(null));
        // Add a null key directly to the map (HashMap permits null keys)
        map.put(null, 42);
        assertTrue(keySet.contains(null));
    }

    @Test
    void testKeySetMultipleCallsConsistency() {
        map = new SealableMap<>(sealedSupplier);
        map.put("p", 10);
        map.put("q", 20);
        sealedState = false;
        Set<String> first = map.keySet();
        Set<String> second = map.keySet();
        // The two sets must be distinct wrapper instances
        assertNotSame(first, second, "Each call to keySet() should return a new SealableSet instance");
        // But they must reflect the same underlying data
        assertEquals(first, second);
        // Mutate via the first view
        assertTrue(first.remove("p"));
        assertFalse(first.contains("p"));
        assertFalse(second.contains("p"));
        assertNull(map.get("p"));
    }

    @Test
    void testKeySetIteratorRemoveWhenUnsealed() {
        map = new SealableMap<>(sealedSupplier);
        map.put("i", 1);
        map.put("j", 2);
        map.put("k", 3);
        sealedState = false;
        Set<String> keySet = map.keySet();
        Iterator<String> it = keySet.iterator();
        // Capture the first element
        assertTrue(it.hasNext());
        String first = it.next();
        // Remove via iterator
        it.remove();
        // The underlying map must no longer contain the removed key
        assertNull(map.get(first));
        assertFalse(keySet.contains(first));
        assertEquals(2, map.size());
    }

    @Test
    void testKeySetIteratorRemoveWhenSealed() {
        map = new SealableMap<>(sealedSupplier);
        map.put("i", 1);
        map.put("j", 2);
        // sealed
        sealedState = true;
        Set<String> keySet = map.keySet();
        Iterator<String> it = keySet.iterator();
        assertTrue(it.hasNext());
        // advance to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void testKeySetWhenUnderlyingMapIsNull() throws Exception {
        // Construct a normal SealableMap then force its internal map to null via reflection
        map = new SealableMap<>(sealedSupplier);
        Field mapField = SealableMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        // now the internal map reference is null
        mapField.set(map, null);
        assertThrows(NullPointerException.class, map::keySet, "Calling keySet() when the backing map is null should throw NPE");
    }

    @Test
    void testKeySetWhenSealedSupplierIsNull() throws Exception {
        // Build a SealableMap with a valid map first
        map = new SealableMap<>(sealedSupplier);
        map.put("a", 1);
        // Use reflection to replace the sealedSupplier with null
        Field supplierField = SealableMap.class.getDeclaredField("sealedSupplier");
        supplierField.setAccessible(true);
        supplierField.set(map, null);
        // The constructor itself does not access the supplier, but any mutating operation
        // will attempt to query it, causing a NPE. We verify that the exception is thrown
        // when we try to modify the returned set.
        // creation succeeds
        Set<String> keySet = map.keySet();
        assertThrows(NullPointerException.class, () -> keySet.remove("a"), "Mutating the keySet with a null sealedSupplier should result in NPE");
    }

    /**
     * Simple var‑args helper to build a map literal.
     */
    @SafeVarargs
    private static Map<String, Integer> mapOf(Object... entries) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            @SuppressWarnings("unchecked")
            String k = (String) entries[i];
            @SuppressWarnings("unchecked")
            Integer v = (Integer) entries[i + 1];
            m.put(k, v);
        }
        return m;
    }
}
