package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_putAll_Test {

    /**
     * Cache instance used by each test.
     */
    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Runs before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Capacity 10 and a short cleanup delay are sufficient for the tests.
        cache = new ThreadedLRUCacheStrategy<>(10, 50);
    }

    /**
     * Runs after each test method.
     */
    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that a fresh instance is used for the next test.
        cache.clear();
    }

    /**
     * Runs once after all test methods have completed.
     */
    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Provides a variety of source maps together with the expected state of the cache
     * after {@code putAll} is invoked.
     */
    private static Stream<Arguments> provideSimplePutAllCases() {
        // empty map
        Map<String, Integer> empty = Collections.emptyMap();
        Map<String, Integer> expectedEmpty = Collections.emptyMap();
        // single entry
        Map<String, Integer> single = new HashMap<>();
        single.put("one", 1);
        Map<String, Integer> expectedSingle = Map.of("one", 1);
        // multiple distinct entries
        Map<String, Integer> multiple = new HashMap<>();
        multiple.put("a", 10);
        multiple.put("b", 20);
        multiple.put("c", 30);
        Map<String, Integer> expectedMultiple = Map.of("a", 10, "b", 20, "c", 30);
        // map with a null key (value non‑null)
        Map<String, Integer> nullKey = new HashMap<>();
        nullKey.put(null, 99);
        Map<String, Integer> expectedNullKey = new HashMap<>();
        expectedNullKey.put(null, 99);
        // map with a null value
        Map<String, Integer> nullValue = new HashMap<>();
        nullValue.put("present", null);
        Map<String, Integer> expectedNullValue = new HashMap<>();
        expectedNullValue.put("present", null);
        // duplicate keys – later entry wins
        Map<String, Integer> duplicate = new LinkedHashMap<>();
        duplicate.put("dup", 1);
        // overwrite
        duplicate.put("dup", 2);
        Map<String, Integer> expectedDuplicate = Map.of("dup", 2);
        return Stream.of(Arguments.of(empty, expectedEmpty, "empty map"), Arguments.of(single, expectedSingle, "single entry map"), Arguments.of(multiple, expectedMultiple, "multiple distinct entries"), Arguments.of(nullKey, expectedNullKey, "map with null key"), Arguments.of(nullValue, expectedNullValue, "map with null value"), Arguments.of(duplicate, expectedDuplicate, "map with duplicate keys"));
    }

    @Test
    void putAll_InsertsAllEntries() {
        // given
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        source.put("three", 3);
        // when
        cache.putAll(source);
        // then
        assertEquals(3, cache.size(), "Cache should contain exactly three entries");
        assertEquals(1, cache.get("one"));
        assertEquals(2, cache.get("two"));
        assertEquals(3, cache.get("three"));
    }

    @Test
    void putAll_OverridesExistingKeys() {
        // given
        cache.put("alpha", 10);
        cache.put("beta", 20);
        Map<String, Integer> source = new HashMap<>();
        // existing key, new value
        source.put("beta", 200);
        // new key
        source.put("gamma", 30);
        // when
        cache.putAll(source);
        // then
        assertEquals(3, cache.size(), "Cache should have three distinct keys");
        assertEquals(10, cache.get("alpha"));
        assertEquals(200, cache.get("beta"), "Value for 'beta' should be overwritten");
        assertEquals(30, cache.get("gamma"));
    }

    @Test
    void putAll_AllowsNullKeyAndNullValue() {
        // given
        Map<String, Integer> source = new HashMap<>();
        // both key and value are null
        source.put(null, null);
        // null value
        source.put("present", null);
        // null key with non‑null value (overwrites previous null key)
        source.put(null, 42);
        // when
        cache.putAll(source);
        // then
        assertEquals(2, cache.size(), "Cache should contain two distinct keys (null and 'present')");
        assertEquals(42, cache.get(null), "Null key should map to the last supplied value (42)");
        assertNull(cache.get("present"), "Value for 'present' should be null");
    }

    @ParameterizedTest(name = "putAll with {2}")
    @MethodSource("provideSimplePutAllCases")
    void putAll_SimpleCases(Map<String, Integer> source, Map<String, Integer> expected, String description) {
        // when
        cache.putAll(source);
        // then
        assertEquals(expected.size(), cache.size(), "Cache size should match expected size for case: " + description);
        expected.forEach((k, v) -> assertEquals(v, cache.get(k), () -> "Value for key '" + k + "' should match expected for case: " + description));
    }

    @Test
    void putAll_ExceedsCapacityTriggersEviction() {
        // Fill cache to its capacity (10 entries)
        for (int i = 0; i < 10; i++) {
            cache.put("k" + i, i);
        }
        // Map that would exceed remaining capacity by 5 entries
        Map<String, Integer> overflow = new HashMap<>();
        for (int i = 10; i < 15; i++) {
            overflow.put("new" + i, i);
        }
        // when
        cache.putAll(overflow);
        // then
        assertEquals(10, cache.size(), "Cache size must stay at its maximum capacity");
        // The five oldest keys (k0‑k4) should have been evicted
        for (int i = 0; i < 5; i++) {
            assertNull(cache.get("k" + i), "Old key k" + i + " should have been evicted");
        }
        // New entries should be present
        for (int i = 10; i < 15; i++) {
            assertEquals(i, cache.get("new" + i), "New key new" + i + " should be present");
        }
        // Remaining older keys (k5‑k9) should still be present
        for (int i = 5; i < 10; i++) {
            assertEquals(i, cache.get("k" + i), "Key k" + i + " should still be present");
        }
    }

    @Test
    void putAll_LargeMapStressRespectsCapacity() {
        // Build a map with 1000 entries
        Map<String, Integer> large = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            large.put("key" + i, i);
        }
        // when
        cache.putAll(large);
        // then
        assertEquals(10, cache.size(), "Cache size must be limited to its capacity (10)");
        // The most recent 10 entries (keys 990‑999) should be retained
        for (int i = 990; i < 1000; i++) {
            assertEquals(i, cache.get("key" + i), "Most recent key key" + i + " should be present after bulk insertion");
        }
        // Older entries must have been evicted
        assertNull(cache.get("key0"), "Oldest entry should have been evicted");
    }

    @Test
    void putAll_WithExistingKeys_UpdatesValueAndRecency() {
        // Populate cache with some keys
        cache.put("x", 1);
        cache.put("y", 2);
        // least recently used after these puts
        cache.put("z", 3);
        // Map that updates "y" and adds a new key "w"
        Map<String, Integer> source = new HashMap<>();
        // overwrite
        source.put("y", 20);
        // new entry
        source.put("w", 4);
        // when
        cache.putAll(source);
        // then
        assertEquals(4, cache.size(), "Cache size should increase only for truly new keys");
        assertEquals(1, cache.get("x"));
        assertEquals(20, cache.get("y"), "Value for 'y' should be updated");
        assertEquals(3, cache.get("z"));
        assertEquals(4, cache.get("w"));
        // To verify recency, add enough entries to force eviction and ensure "y" is kept
        for (int i = 0; i < 7; i++) {
            cache.put("extra" + i, i);
        }
        // At this point the cache is full (10 entries). Adding one more should evict the LRU.
        cache.put("trigger", 999);
        // "z" was the LRU before the extra puts, so it should be evicted, while "y" stays.
        assertNull(cache.get("z"), "'z' should have been evicted as LRU");
        assertEquals(20, cache.get("y"), "'y' should still be present due to recent update");
    }

    @Test
    void putAll_UnmodifiableMap_IsAccepted() {
        Map<String, Integer> modifiable = new HashMap<>();
        modifiable.put("a", 1);
        modifiable.put("b", 2);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(modifiable);
        // when
        cache.putAll(unmodifiable);
        // then
        assertEquals(2, cache.size());
        assertEquals(1, cache.get("a"));
        assertEquals(2, cache.get("b"));
    }

    @Test
    void putAll_NullMap_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> cache.putAll(null), "putAll should throw NPE when the supplied map is null");
    }

    @Test
    void putAll_MapWithNullEntrySet_ThrowsNullPointerException() {
        Map<String, Integer> badMap = new Map<>() {

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
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
            public Integer get(Object key) {
                return null;
            }

            @Override
            public Integer put(String key, Integer value) {
                return null;
            }

            @Override
            public Integer remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends String, ? extends Integer> m) {
            }

            @Override
            public void clear() {
            }

            @Override
            public Set<String> keySet() {
                return Collections.emptySet();
            }

            @Override
            public Collection<Integer> values() {
                return Collections.emptyList();
            }

            // <-- null entry set
            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> cache.putAll(badMap), "putAll should throw NPE when entrySet() returns null");
    }

    @Test
    void putAll_MapThatModifiesDuringIteration_ThrowsConcurrentModificationException() {
        // A HashMap will throw CME if it is structurally modified after its iterator is created.
        Map<String, Integer> modMap = new HashMap<>();
        modMap.put("a", 1);
        modMap.put("b", 2);
        modMap.put("c", 3);
        // Wrap the map so that its entrySet iterator modifies the map on the first call to next()
        Map<String, Integer> wrapper = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                Set<Entry<String, Integer>> original = modMap.entrySet();
                return new AbstractSet<>() {

                    @Override
                    public Iterator<Entry<String, Integer>> iterator() {
                        Iterator<Entry<String, Integer>> it = original.iterator();
                        return new Iterator<>() {

                            private boolean first = true;

                            @Override
                            public boolean hasNext() {
                                return it.hasNext();
                            }

                            @Override
                            public Entry<String, Integer> next() {
                                if (first) {
                                    // Structural modification during iteration triggers CME
                                    modMap.put("newKey", 999);
                                    first = false;
                                }
                                return it.next();
                            }

                            @Override
                            public void remove() {
                                it.remove();
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return original.size();
                    }
                };
            }

            // delegate other Map methods to modMap
            @Override
            public int size() {
                return modMap.size();
            }

            @Override
            public boolean isEmpty() {
                return modMap.isEmpty();
            }

            @Override
            public boolean containsKey(Object key) {
                return modMap.containsKey(key);
            }

            @Override
            public boolean containsValue(Object value) {
                return modMap.containsValue(value);
            }

            @Override
            public Integer get(Object key) {
                return modMap.get(key);
            }

            @Override
            public Integer put(String key, Integer value) {
                return modMap.put(key, value);
            }

            @Override
            public Integer remove(Object key) {
                return modMap.remove(key);
            }

            @Override
            public void putAll(Map<? extends String, ? extends Integer> m) {
                modMap.putAll(m);
            }

            @Override
            public void clear() {
                modMap.clear();
            }

            @Override
            public Set<String> keySet() {
                return modMap.keySet();
            }

            @Override
            public Collection<Integer> values() {
                return modMap.values();
            }
        };
        assertThrows(ConcurrentModificationException.class, () -> cache.putAll(wrapper), "putAll should propagate ConcurrentModificationException when the source map is modified during iteration");
    }

    @Test
    void putAll_MapWithFaultyEntryKey_ThrowsIllegalStateException() {
        // Create a map with a single entry whose getKey() throws IllegalStateException
        Map<String, Integer> faulty = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                Entry<String, Integer> badEntry = new Entry<>() {

                    @Override
                    public String getKey() {
                        throw new IllegalStateException("bad key");
                    }

                    @Override
                    public Integer getValue() {
                        return 1;
                    }

                    @Override
                    public Integer setValue(Integer value) {
                        return null;
                    }
                };
                return Set.of(badEntry);
            }

            // delegate other methods to an empty map
            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
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
            public Integer get(Object key) {
                return null;
            }

            @Override
            public Integer put(String key, Integer value) {
                return null;
            }

            @Override
            public Integer remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends String, ? extends Integer> m) {
            }

            @Override
            public void clear() {
            }

            @Override
            public Set<String> keySet() {
                return Collections.emptySet();
            }

            @Override
            public Collection<Integer> values() {
                return Collections.emptyList();
            }
        };
        assertThrows(IllegalStateException.class, () -> cache.putAll(faulty), "putAll should propagate IllegalStateException thrown by a map entry's getKey()");
    }

    @Test
    void putAll_OverridesExistingKeys_Original() {
        // given
        cache.put("alpha", 10);
        cache.put("beta", 20);
        Map<String, Integer> source = new HashMap<>();
        // existing key, new value
        source.put("beta", 200);
        // new key
        source.put("gamma", 30);
        // when
        cache.putAll(source);
        // then
        assertEquals(3, cache.size(), "Cache should have three distinct keys");
        assertEquals(10, cache.get("alpha"));
        assertEquals(200, cache.get("beta"), "Value for 'beta' should be overwritten");
        assertEquals(30, cache.get("gamma"));
    }

    @Test
    void putAll_AllowsNullKeyAndNullValue_Original() {
        // given
        Map<String, Integer> source = new HashMap<>();
        // both key and value are null
        source.put(null, null);
        // null value
        source.put("present", null);
        // null key with non‑null value (overwrites previous null key)
        source.put(null, 42);
        // when
        cache.putAll(source);
        // then
        assertEquals(2, cache.size(), "Cache should contain two distinct keys (null and 'present')");
        assertEquals(42, cache.get(null), "Null key should map to the last supplied value (42)");
        assertNull(cache.get("present"), "Value for 'present' should be null");
    }
}
