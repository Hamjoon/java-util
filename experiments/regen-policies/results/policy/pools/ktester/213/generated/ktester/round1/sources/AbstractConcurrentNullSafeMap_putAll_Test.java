package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_putAll_Test {

    private TestMap<String, String> mapUnderTest;

    private ConcurrentMap<Object, Object> backingMap;

    /**
     * Concrete implementation used for the tests.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }

        /**
         * Expose the internal map for verification purposes.
         */
        ConcurrentMap<Object, Object> getInternalMap() {
            return internalMap;
        }

        /**
         * Helper to expose the protected mask‑null logic for keys.
         */
        Object maskKey(K key) {
            return maskNullKey(key);
        }

        /**
         * Helper to expose the protected mask‑null logic for values.
         */
        Object maskValue(V value) {
            return maskNullValue(value);
        }
    }

    /**
     * A map implementation whose {@code entrySet()} returns duplicate keys.
     */
    private static class DuplicateKeyMap<K, V> implements Map<K, V> {

        private final List<Entry<K, V>> entries = new ArrayList<>();

        /**
         * Adds an entry without any deduplication – used to simulate duplicate keys.
         */
        void putEntry(K k, V v) {
            entries.add(new AbstractMap.SimpleEntry<>(k, v));
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return entries.iterator();
                }

                @Override
                public int size() {
                    return entries.size();
                }
            };
        }

        // The remaining Map methods are either trivial or unsupported for this test.
        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public boolean isEmpty() {
            return entries.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return entries.stream().anyMatch(e -> Objects.equals(e.getKey(), key));
        }

        @Override
        public boolean containsValue(Object value) {
            return entries.stream().anyMatch(e -> Objects.equals(e.getValue(), value));
        }

        @Override
        public V get(Object key) {
            for (Entry<K, V> e : entries) {
                if (Objects.equals(e.getKey(), key))
                    return e.getValue();
            }
            return null;
        }

        @Override
        public V put(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public V remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            entries.clear();
        }

        @Override
        public Set<K> keySet() {
            return entries.stream().map(Entry::getKey).collect(Collectors.toSet());
        }

        @Override
        public Collection<V> values() {
            return entries.stream().map(Entry::getValue).collect(Collectors.toList());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap so that we can inspect the stored values.
        backingMap = new ConcurrentHashMap<>();
        mapUnderTest = new TestMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        backingMap.clear();
    }

    /**
     * Supplies the various {@code putAll} scenarios described in the JSON.
     */
    private static Stream<Arguments> putAllProvider() {
        return Stream.of(// 1. All non‑null entries
        Arguments.of(Map.of("a", 1, "b", 2, "c", 3), Map.of("a", 1, "b", 2, "c", 3), "All keys and values are non‑null; each entry should be forwarded unchanged."), // 2. Empty map
        Arguments.of(Collections.emptyMap(), Collections.emptyMap(), "Empty source map – internal map must stay untouched."), // 3. Null key only
        Arguments.of(new HashMap<String, Integer>() {

            {
                put(null, 10);
                put("x", 20);
            }
        }, // placeholder – will be built inside the test using maskKey()
        null, "Null key must be masked with the internal sentinel."), // 4. Null value only
        Arguments.of(new HashMap<String, Integer>() {

            {
                put("y", null);
                put("z", 30);
            }
        }, null, "Null value must be masked with the internal sentinel."), // 5. Both null key and null value
        Arguments.of(new HashMap<String, Integer>() {

            {
                put(null, null);
                put("k", 40);
            }
        }, null, "Both key and value nulls must be masked."), // 6. Mixed collection with overwrites
        Arguments.of(new LinkedHashMap<String, Integer>() {

            {
                put("p", 1);
                put(null, 2);
                put("q", null);
                // overwrites previous null‑key entry
                put(null, null);
                put("r", 5);
            }
        }, null, "Mixed entries – later null‑key entry overwrites earlier one."));
    }

    @Test
    void putAll_withOnlyNonNullEntries_storesAllEntries() {
        // GIVEN
        Map<String, String> source = new HashMap<>();
        source.put("a", "alpha");
        source.put("b", "bravo");
        source.put("c", "charlie");
        // WHEN
        mapUnderTest.putAll(source);
        // THEN
        assertEquals(3, mapUnderTest.size(), "Map size should match number of entries added");
        assertEquals("alpha", mapUnderTest.get("a"));
        assertEquals("bravo", mapUnderTest.get("b"));
        assertEquals("charlie", mapUnderTest.get("c"));
    }

    @Test
    void putAll_withNullKeyAndNullValue_handlesSentinelsCorrectly() {
        // GIVEN
        Map<String, String> source = new HashMap<>();
        // null key, non‑null value
        source.put(null, "nullKeyValue");
        // non‑null key, null value
        source.put("nullValue", null);
        // both null (overwrites previous null‑key entry)
        source.put(null, null);
        // WHEN
        mapUnderTest.putAll(source);
        // THEN
        // The map should contain three logical entries: null key → null, "nullValue" → null
        assertEquals(2, mapUnderTest.size(), "Map should contain two logical entries after overwriting null key");
        // Retrieval via the public API should give the original logical values.
        assertNull(mapUnderTest.get(null), "Value for null key should be null (the last entry overwrote previous)");
        assertNull(mapUnderTest.get("nullValue"), "Value for key \"nullValue\" should be null");
        // Verify that the internal map does not store actual nulls (sentinel objects are used).
        for (Object storedKey : backingMap.keySet()) {
            assertNotNull(storedKey, "Internal map must not contain a raw null key");
        }
        for (Object storedValue : backingMap.values()) {
            assertNotNull(storedValue, "Internal map must not contain a raw null value");
        }
    }

    @Test
    void putAll_withMixedEntries_preservesAllValues() {
        // GIVEN
        // preserve order for readability
        Map<String, String> source = new LinkedHashMap<>();
        source.put("first", "1");
        source.put(null, "nullKey");
        source.put("second", null);
        source.put("third", "3");
        // WHEN
        mapUnderTest.putAll(source);
        // THEN
        assertEquals(4, mapUnderTest.size(), "All four entries should be present");
        assertEquals("1", mapUnderTest.get("first"));
        assertEquals("nullKey", mapUnderTest.get(null));
        assertNull(mapUnderTest.get("second"));
        assertEquals("3", mapUnderTest.get("third"));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("putAllProvider")
    void putAll_parameterizedTests(Map<String, Integer> source, Map<Object, Object> expectedPlaceholder, String description) {
        // Build the expected internal map, taking sentinel objects into account.
        Map<Object, Object> expected = new HashMap<>();
        for (Map.Entry<String, Integer> e : source.entrySet()) {
            Object key = e.getKey();
            Object value = e.getValue();
            // Apply masking when necessary.
            if (key == null) {
                key = mapUnderTest.maskKey(null);
            }
            if (value == null) {
                value = mapUnderTest.maskValue(null);
            }
            expected.put(key, value);
        }
        // WHEN
        mapUnderTest.putAll(source);
        // THEN
        assertEquals(expected.size(), backingMap.size(), "Internal map size must match expected number of stored entries.");
        // Direct comparison of the internal representation (including sentinel objects).
        assertEquals(expected, backingMap, "Internal map contents differ from expected after putAll.");
    }

    /**
     * Specific test for the {@code DuplicateKeyMap} which yields duplicate keys in its entry set.
     */
    @Test
    void putAll_withDuplicateKeyMap_lastEntryWins() {
        // GIVEN – a map that pretends to have two entries with the same key.
        DuplicateKeyMap<String, Integer> dupMap = new DuplicateKeyMap<>();
        dupMap.putEntry("dup", 100);
        // later entry should overwrite the former
        dupMap.putEntry("dup", 200);
        // WHEN
        mapUnderTest.putAll(dupMap);
        // THEN – only one logical entry should exist with the value from the last entry.
        assertEquals(1, backingMap.size(), "Only one key should be present after processing duplicates.");
        Object expectedKey = "dup";
        Object expectedValue = 200;
        assertEquals(expectedValue, backingMap.get(expectedKey), "The value associated with the duplicate key must be the one from the last entry.");
    }
}
