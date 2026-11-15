package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_putAll_Test {

    /**
     * The cache instance used in each test.
     */
    private ThreadedLRUCacheStrategy<Integer, String> cache;

    /**
     * Capacity is set high enough so that eviction does not interfere with the putAll tests.
     */
    private static final int CAPACITY = 10;

    /**
     * Small delay – the scheduled purge task will not affect the immediate assertions.
     */
    private static final int CLEANUP_DELAY_MILLIS = 50;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for every test case.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that one test does not affect another.
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Parameter source for normal‑behavior scenarios.
     */
    private static Stream<Arguments> normalBehaviorProvider() {
        return Stream.of(// { "a":1, "b":2 }
        Arguments.of(Map.of("a", 1, "b", 2), Map.of("a", 1, "b", 2), "putAll with a regular map containing two distinct entries"), // { "x":null, "y":5 }
        Arguments.of(new HashMap<String, Integer>() {

            {
                put("x", null);
                put("y", 5);
            }
        }, Map.of("x", null, "y", 5), "putAll should correctly handle entries whose value is null"), // { null:10, "z":20 }
        Arguments.of(new HashMap<String, Integer>() {

            {
                put(null, 10);
                put("z", 20);
            }
        }, Map.of(null, 10, "z", 20), "putAll should correctly handle an entry with a null key"), // duplicate key – later entry wins
        Arguments.of(new LinkedHashMap<String, Integer>() {

            {
                put("dup", 1);
                // overwrite
                put("dup", 2);
            }
        }, Map.of("dup", 2), "When the map contains duplicate keys, the later entry should overwrite the earlier one"), // empty map
        Arguments.of(Collections.emptyMap(), Collections.emptyMap(), "putAll with an empty map should result in no modifications to the cache"));
    }

    @Test
    void putAll_WithEmptyMap_DoesNotChangeSize() {
        // GIVEN
        assertEquals(0, cache.size(), "Cache should start empty");
        Map<Integer, String> emptyMap = Map.of();
        // WHEN
        cache.putAll(emptyMap);
        // THEN
        assertEquals(0, cache.size(), "Cache size must remain 0 after putting an empty map");
    }

    @Test
    void putAll_WithMultipleEntries_StoresAllEntries() {
        // GIVEN
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "one");
        source.put(2, "two");
        source.put(3, "three");
        // WHEN
        cache.putAll(source);
        // THEN
        assertEquals(source.size(), cache.size(), "Cache size should match the number of inserted entries");
        source.forEach((k, v) -> assertEquals(v, cache.get(k), "Value for key " + k + " must be stored"));
    }

    @Test
    void putAll_DelegatesToPut_ForEachEntry() {
        // GIVEN
        // Spy on a real cache instance so that we can verify internal calls to put(...)
        ThreadedLRUCacheStrategy<Integer, String> realCache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
        ThreadedLRUCacheStrategy<Integer, String> spyCache = Mockito.spy(realCache);
        Map<Integer, String> source = new HashMap<>();
        source.put(10, "ten");
        source.put(20, "twenty");
        // WHEN
        spyCache.putAll(source);
        // THEN
        // Verify that put(...) was invoked exactly once for each entry in the source map.
        source.forEach((k, v) -> verify(spyCache, times(1)).put(k, v));
        // Also ensure no extra invocations occurred.
        verifyNoMoreInteractions(spyCache);
    }

    @Test
    void putAll_ExceedsCapacity_EvictsLeastRecentlyUsed() {
        // GIVEN
        // Capacity of 3, put 3 items first.
        ThreadedLRUCacheStrategy<Integer, String> smallCache = new ThreadedLRUCacheStrategy<>(3, CLEANUP_DELAY_MILLIS);
        smallCache.put(1, "one");
        smallCache.put(2, "two");
        smallCache.put(3, "three");
        // Access key 1 to make it most-recently-used.
        assertEquals("one", smallCache.get(1));
        // WHEN: putAll adds two more entries, causing overflow.
        Map<Integer, String> overflow = Map.of(4, "four", 5, "five");
        smallCache.putAll(overflow);
        // THEN: cache size must stay at capacity (3) and the least-recently-used entry (key 2) should be evicted.
        assertEquals(3, smallCache.size(), "Cache size must respect the configured capacity");
        assertNull(smallCache.get(2), "Least-recently-used entry should have been evicted");
        assertNotNull(smallCache.get(1), "Most-recently-used entry must remain");
        assertNotNull(smallCache.get(4), "Newly added entry must be present");
        assertNotNull(smallCache.get(5), "Newly added entry must be present");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("normalBehaviorProvider")
    void putAll_NormalBehavior(Map<String, Integer> source, Map<String, Integer> expected, String description) {
        ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(100, CLEANUP_DELAY_MILLIS);
        c.putAll(source);
        assertEquals(expected.size(), c.size(), "Cache size must match expected size");
        expected.forEach((k, v) -> assertEquals(v, c.get(k), "Incorrect value for key: " + k));
    }

    /**
     * Test that a null argument results in NPE.
     */
    @Test
    void putAll_NullArgument_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> cache.putAll(null), "Calling putAll with a null argument should throw NullPointerException");
    }

    /**
     * Test propagation of a RuntimeException thrown from entrySet().
     */
    @Test
    void putAll_EntrySetThrowsRuntimeException_Propagates() {
        Map<String, Integer> badMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                throw new RuntimeException("fail");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.putAll(badMap));
        assertEquals("fail", ex.getMessage());
    }

    /**
     * Overwrite existing entries scenario.
     */
    @Test
    void putAll_OverwriteExistingEntries() {
        ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(100, CLEANUP_DELAY_MILLIS);
        // initial cache
        c.put("a", 10);
        Map<String, Integer> source = new HashMap<>();
        // should overwrite
        source.put("a", 5);
        // new entry
        source.put("b", 7);
        c.putAll(source);
        assertEquals(2, c.size());
        assertEquals(5, c.get("a"));
        assertEquals(7, c.get("b"));
    }

    /**
     * Large map handling – 100 entries.
     */
    @Test
    void putAll_LargeMap_HandlesAllEntries() {
        ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(200, CLEANUP_DELAY_MILLIS);
        Map<String, Integer> largeMap = IntStream.range(0, 100).boxed().collect(Collectors.toMap(i -> "k" + i, i -> i));
        c.putAll(largeMap);
        assertEquals(100, c.size());
        largeMap.forEach((k, v) -> assertEquals(v, c.get(k)));
    }

    /**
     * Unmodifiable source map.
     */
    @Test
    void putAll_UnmodifiableSourceMap() {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("u", 1);
        Map<String, Integer> source = Collections.unmodifiableMap(mutable);
        ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(10, CLEANUP_DELAY_MILLIS);
        c.putAll(source);
        assertEquals(1, c.size());
        assertEquals(1, c.get("u"));
    }

    /**
     * Null key and null value together.
     */
    @Test
    void putAll_NullKeyAndNullValue() {
        Map<String, Integer> source = new HashMap<>();
        source.put(null, null);
        ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(10, CLEANUP_DELAY_MILLIS);
        c.putAll(source);
        assertEquals(1, c.size());
        assertTrue(c.containsKey(null));
        assertNull(c.get(null));
    }

    /**
     * EntrySet containing a null entry should cause NPE when accessed.
     */
    @Test
    void putAll_EntrySetContainsNullEntry_ThrowsNullPointerException() {
        Map<String, Integer> badMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                Set<Entry<String, Integer>> s = new HashSet<>();
                // illegal null entry
                s.add(null);
                return s;
            }
        };
        assertThrows(NullPointerException.class, () -> cache.putAll(badMap));
    }

    /**
     * ConcurrentHashMap source – ensure no CME is thrown.
     */
    @Test
    void putAll_ConcurrentHashMapModifiedDuringIteration_NoException() throws InterruptedException {
        // Prepare a map with 50 entries.
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        for (int i = 0; i < 50; i++) {
            concurrentMap.put("c" + i, i);
        }
        // Start a background thread that adds more entries while we iterate.
        Thread modifier = new Thread(() -> {
            for (int i = 50; i < 100; i++) {
                concurrentMap.put("c" + i, i);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        });
        modifier.start();
        ThreadedLRUCacheStrategy<String, Integer> c = new ThreadedLRUCacheStrategy<>(200, CLEANUP_DELAY_MILLIS);
        // The call itself must not throw.
        assertDoesNotThrow(() -> c.putAll(concurrentMap));
        // Wait for the modifier to finish to make the test deterministic.
        modifier.join();
        // At a minimum, the first 50 entries must be present.
        for (int i = 0; i < 50; i++) {
            assertEquals(i, c.get("c" + i), "Missing expected entry c" + i);
        }
        // The cache may also contain some of the later entries – we don't assert exact count,
        // only that no exception occurred and the initial data is intact.
    }
}
