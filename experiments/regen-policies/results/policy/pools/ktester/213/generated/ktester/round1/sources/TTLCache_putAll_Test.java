package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

@ExtendWith(MockitoExtension.class)
class TTLCache_putAll_Test {

    private TTLCache<Integer, String> cache;

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure any previously created caches are shut down before the suite runs
        TTLCache.shutdown();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a generous TTL so entries do not expire during the test
        cache = new TTLCache<>(60_000, -1);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache after each test to avoid cross‑test contamination
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the background purge thread once all tests are finished
        TTLCache.shutdown();
    }

    @Test
    void putAllCopiesAllEntries() {
        // given
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "one");
        source.put(2, "two");
        source.put(3, "three");
        // when
        cache.putAll(source);
        // then
        assertEquals(3, cache.size(), "Cache should contain three entries after putAll");
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
    }

    @Test
    void putAllOverridesExistingEntries() {
        // given
        cache.put(1, "old");
        cache.put(2, "old2");
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "new");
        source.put(3, "new3");
        // when
        cache.putAll(source);
        // then
        assertEquals(3, cache.size(), "Cache should have three distinct keys after putAll");
        assertEquals("new", cache.get(1), "Existing key should be overwritten");
        assertEquals("old2", cache.get(2), "Unchanged key should retain original value");
        assertEquals("new3", cache.get(3));
    }

    @Test
    void putAllHandlesEmptyMapGracefully() {
        // given
        cache.put(1, "A");
        Map<Integer, String> empty = new HashMap<>();
        // when
        cache.putAll(empty);
        // then
        assertEquals(1, cache.size(), "Cache size should remain unchanged when putting an empty map");
        assertEquals("A", cache.get(1));
    }

    @Test
    void putAllAcceptsNullValuesAndKeysWhenSupported() {
        // given
        Map<Integer, String> source = new HashMap<>();
        source.put(null, "nullKey");
        source.put(5, null);
        // when
        cache.putAll(source);
        // then
        assertEquals(2, cache.size());
        assertEquals("nullKey", cache.get(null));
        assertNull(cache.get(5));
    }

    @Test
    @DisplayName("putAll – null source map should throw NullPointerException")
    void putAllNullSourceMap() {
        assertThrows(NullPointerException.class, () -> cache.putAll(null), "Calling putAll with a null map must raise NPE");
    }

    @Test
    @DisplayName("putAll – map containing a duplicate key keeps the last value")
    void putAllDuplicateKeys() {
        // given
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("x", 1);
        // duplicate, later value wins
        source.put("x", 2);
        // when
        cache.putAll(source);
        // then
        assertEquals(1, cache.size());
        assertEquals(2, cache.get("x"));
    }

    @Test
    @DisplayName("putAll – map with mixed null and non‑null entries")
    void putAllMixedNullAndNonNull() {
        // given
        Map<String, Integer> source = new HashMap<>();
        source.put(null, 100);
        source.put("valid", 42);
        source.put("another", null);
        // when
        cache.putAll(source);
        // then
        assertEquals(3, cache.size());
        assertEquals(100, cache.get(null));
        assertEquals(42, cache.get("valid"));
        assertNull(cache.get("another"));
    }

    @Test
    @DisplayName("putAll – source map is a concurrent map with concurrent modifications")
    void putAllConcurrentMapModifications() throws Exception {
        // given
        ConcurrentHashMap<String, Integer> concurrent = new ConcurrentHashMap<>();
        concurrent.put("a", 1);
        concurrent.put("b", 2);
        Thread modifier = new Thread(() -> {
            // modify while the main thread may be iterating
            concurrent.put("c", 3);
            concurrent.remove("a");
        });
        modifier.start();
        // ensure modifications are done before we call putAll
        modifier.join();
        // when
        cache.putAll(concurrent);
        // then
        // The snapshot taken by entrySet() may contain 2 or 3 entries depending on timing.
        int expectedSize = concurrent.size();
        assertEquals(expectedSize, cache.size(), "Cache size should match the number of entries present in the source map at iteration time");
        // Verify that each key present in the source map is present in the cache with the same value
        for (Map.Entry<String, Integer> e : concurrent.entrySet()) {
            assertEquals(e.getValue(), cache.get(e.getKey()));
        }
    }

    @Test
    @DisplayName("putAll – source map with custom Entry implementation throwing on getKey")
    void putAllEntryThrowsOnGetKey() {
        // given a map whose entrySet returns a single entry that throws when getKey() is called
        Map<String, Integer> badMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return Collections.singleton(new Entry<>() {

                    @Override
                    public String getKey() {
                        throw new RuntimeException("key error");
                    }

                    @Override
                    public Integer getValue() {
                        return 5;
                    }

                    @Override
                    public Integer setValue(Integer value) {
                        return null;
                    }
                });
            }
        };
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.putAll(badMap), "Exception from entry.getKey() must propagate");
        assertEquals("key error", ex.getMessage());
        // cache must remain unchanged
        assertTrue(cache.isEmpty(), "Cache should stay empty after exception");
    }

    @Test
    @DisplayName("putAll – source map with self‑referencing entry (key equals value)")
    void putAllSelfReferencingEntry() {
        // given
        Map<String, String> source = Map.of("self", "self");
        // when
        TTLCache<String, String> stringCache = new TTLCache<>(60_000, -1);
        stringCache.putAll(source);
        // then
        assertEquals(1, stringCache.size());
        assertEquals("self", stringCache.get("self"));
    }

    @Test
    @DisplayName("putAll – source map with large number of entries (stress test)")
    void putAllLargeMapStressTest() {
        // given a map with 10,000 entries
        Map<String, Integer> largeMap = IntStream.range(0, 10_000).boxed().collect(Collectors.toMap(i -> "k" + i, i -> i));
        // when
        cache.putAll(largeMap);
        // then
        assertEquals(10_000, cache.size(), "Cache should contain all 10,000 entries");
        // Spot‑check a few random entries to avoid iterating the whole map in the assertion
        assertEquals(0, cache.get("k0"));
        assertEquals(5_000, cache.get("k5000"));
        assertEquals(9_999, cache.get("k9999"));
    }

    @Test
    @DisplayName("putAll – source map with immutable entry set (unmodifiableMap)")
    void putAllUnmodifiableMap() {
        // given
        Map<String, Integer> source = Collections.unmodifiableMap(Map.of("u1", 1, "u2", 2));
        // when
        cache.putAll(source);
        // then
        assertEquals(2, cache.size());
        assertEquals(1, cache.get("u1"));
        assertEquals(2, cache.get("u2"));
        // ensure source map is still unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> source.put("u3", 3));
    }
}
