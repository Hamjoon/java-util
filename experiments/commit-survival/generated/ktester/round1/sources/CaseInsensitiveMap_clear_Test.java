package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_clear_Test {

    private CaseInsensitiveMap<String, Integer> map;

    class ExplodingMap<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public Set<Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public void clear() {
            throw new IllegalStateException("Intentional failure from custom map");
        }
    }

    class ExplodingMap<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public Set<Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public void clear() {
            throw new IllegalStateException("Intentional failure from custom map");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor which creates an internal LinkedHashMap.
        map = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map reference is released after each test.
        map = null;
    }

    /* --------------------------------------------------------------------- */
    private static void setInternalMap(CaseInsensitiveMap<?, ?> target, Map<?, ?> newMap) throws Exception {
        Field f = CaseInsensitiveMap.class.getDeclaredField("map");
        f.setAccessible(true);
        // Remove final modifier if present (works on most JVMs for test purposes)
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        f.set(target, newMap);
    }

    @Test
    @DisplayName("clear() empties a populated map")
    void clearEmptiesPopulatedMap() {
        // given
        map.put("One", 1);
        map.put("Two", 2);
        map.put("Three", 3);
        assertEquals(3, map.size(), "Pre‑condition: map should contain three entries");
        // when
        map.clear();
        // then
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, map.size(), "Size should be zero after clear()");
        assertTrue(map.keySet().isEmpty(), "keySet should be empty after clear()");
        assertTrue(map.entrySet().isEmpty(), "entrySet should be empty after clear()");
        assertTrue(map.values().isEmpty(), "values collection should be empty after clear()");
    }

    @Test
    @DisplayName("clear() on an already empty map does not throw")
    void clearOnEmptyMap() {
        // given: map is already empty
        assertTrue(map.isEmpty(), "Pre‑condition: map should start empty");
        // when / then: clear should not throw and map remains empty
        assertDoesNotThrow(() -> map.clear(), "clear() on an empty map should not throw");
        assertTrue(map.isEmpty(), "Map should still be empty after clear()");
        assertEquals(0, map.size(), "Size should remain zero after clear()");
    }

    @Test
    @DisplayName("clear() removes case‑insensitive entries")
    void clearRemovesCaseInsensitiveEntries() {
        // given: add entries with varying case
        map.put("Key", 10);
        map.put("another", 20);
        // should replace the previous "Key" entry
        map.put("KEY", 30);
        assertEquals(2, map.size(), "Map should contain two distinct keys (case‑insensitive)");
        // when
        map.clear();
        // then
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
        assertNull(map.get("key"), "Getting any key after clear() should return null");
    }

    @Test
    @DisplayName("clear() after putAll() with null values")
    void clearAfterPutAllWithNullValues() {
        Map<String, Integer> source = new HashMap<>();
        source.put("A", null);
        source.put("B", null);
        source.put("C", 3);
        map.putAll(source);
        assertEquals(3, map.size(), "Map should contain three entries (null values count)");
        assertTrue(map.containsKey("A"));
        assertTrue(map.containsKey("B"));
        assertTrue(map.containsKey("C"));
        assertNull(map.get("A"));
        assertNull(map.get("B"));
        assertEquals(3, map.get("C"));
        map.clear();
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, map.size(), "Size should be zero after clear()");
        assertFalse(map.containsKey("A"));
        assertNull(map.get("A"));
    }

    @Test
    @DisplayName("clear() does not affect external reference to the wrapped map")
    void clearDoesNotAffectExternalReference() {
        Map<String, Integer> wrapped = map.getWrappedMap();
        // Directly modify the underlying map
        wrapped.put("x", 100);
        wrapped.put("y", 200);
        assertEquals(2, map.size(), "Map should reflect changes made directly to the wrapped map");
        map.clear();
        assertTrue(map.isEmpty(), "CaseInsensitiveMap should be empty after clear()");
        assertTrue(wrapped.isEmpty(), "Underlying wrapped map should also be empty after clear()");
    }

    @Test
    @DisplayName("clear() after partial removal")
    void clearAfterPartialRemoval() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);
        // Remove a subset
        map.remove("two");
        map.remove("four");
        assertEquals(2, map.size(), "Map should have two entries after removals");
        assertFalse(map.containsKey("two"));
        assertFalse(map.containsKey("four"));
        map.clear();
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, map.size(), "Size should be zero after clear()");
        assertFalse(map.containsKey("one"));
        assertFalse(map.containsKey("three"));
    }

    @Test
    @DisplayName("clear() is idempotent")
    void clearIsIdempotent() {
        map.put("alpha", 10);
        map.put("beta", 20);
        assertEquals(2, map.size());
        map.clear();
        int sizeAfterFirstClear = map.size();
        // second clear – should not throw and size stays zero
        assertDoesNotThrow(() -> map.clear(), "Second clear() should not throw");
        int sizeAfterSecondClear = map.size();
        assertEquals(0, sizeAfterFirstClear, "Size after first clear should be zero");
        assertEquals(0, sizeAfterSecondClear, "Size after second clear should still be zero");
        assertTrue(map.isEmpty(), "Map should be empty after second clear");
    }

    @Test
    @DisplayName("clear() on map with ConcurrentHashMap backing")
    void clearOnConcurrentHashMapBacking() throws Exception {
        // Replace internal map with a ConcurrentHashMap via reflection
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        setInternalMap(map, concurrentMap);
        map.put("c1", 1);
        map.put("c2", 2);
        assertEquals(2, map.size());
        assertTrue(concurrentMap instanceof ConcurrentHashMap, "Underlying map should be a ConcurrentHashMap");
        map.clear();
        assertTrue(map.isEmpty(), "CaseInsensitiveMap should be empty after clear()");
        assertTrue(concurrentMap.isEmpty(), "Underlying ConcurrentHashMap should be cleared");
    }

    @Test
    @DisplayName("clear() after iterating keySet does not throw")
    void clearAfterIteratingKeySet() {
        map.put("k1", 1);
        map.put("k2", 2);
        map.put("k3", 3);
        // Iterate without modifying
        for (String key : map.keySet()) {
            // just read
            assertNotNull(key);
        }
        // clear should succeed without ConcurrentModificationException
        assertDoesNotThrow(() -> map.clear(), "clear() after iteration should not throw");
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
    }

    @Test
    @DisplayName("clear() after inserting null key")
    void clearAfterInsertingNullKey() {
        map.put(null, 42);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(null));
        assertEquals(42, map.get(null));
        map.clear();
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
        assertNull(map.get(null), "get(null) should return null after clear()");
        assertFalse(map.containsKey(null), "containsKey(null) should be false after clear()");
    }

    @Test
    @DisplayName("clear() on unmodifiable backing map throws UnsupportedOperationException")
    void clearOnUnmodifiableBackingMap() throws Exception {
        // Prepare a modifiable map with entries, then wrap it as unmodifiable
        Map<String, Integer> modifiable = new HashMap<>();
        modifiable.put("u1", 1);
        modifiable.put("u2", 2);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(modifiable);
        // Inject the unmodifiable map as the internal backing store
        setInternalMap(map, unmodifiable);
        // clear() should delegate to the underlying map and therefore throw
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> map.clear(), "clear() on an unmodifiable backing map must propagate UnsupportedOperationException");
        assertNotNull(ex);
    }

    @Test
    @DisplayName("clear() when underlying map reference is null throws NullPointerException")
    void clearWhenUnderlyingMapIsNull() throws Exception {
        // Set the private field 'map' to null via reflection
        setInternalMap(map, null);
        assertThrows(NullPointerException.class, () -> map.clear(), "Calling clear() when internal map is null should throw NullPointerException");
    }

    @Test
    @DisplayName("clear() propagates runtime exception from custom backing map")
    void clearPropagatesExceptionFromCustomMap() throws Exception {
        // Custom map that throws IllegalStateException from clear()
        class ExplodingMap<K, V> extends AbstractMap<K, V> {

            private final Map<K, V> delegate = new HashMap<>();

            @Override
            public Set<Entry<K, V>> entrySet() {
                return delegate.entrySet();
            }

            @Override
            public V put(K key, V value) {
                return delegate.put(key, value);
            }

            @Override
            public void clear() {
                throw new IllegalStateException("Intentional failure from custom map");
            }
        }
        ExplodingMap<String, Integer> exploding = new ExplodingMap<>();
        setInternalMap(map, exploding);
        // Populate via the custom map (delegated put works)
        map.put("e1", 10);
        map.put("e2", 20);
        assertEquals(2, map.size());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.clear(), "clear() should propagate IllegalStateException from the backing map");
        assertEquals("Intentional failure from custom map", ex.getMessage());
    }
}
