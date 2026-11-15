package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LRUCache_putAll_Test {

    private LRUCache<Integer, String> lruCache;

    @BeforeEach
    void setupBeforeEach() {
        // Each test will initialise its own cache via the helper `setUp` method.
        lruCache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to aid GC and to avoid cross‑test interference.
        if (lruCache != null) {
            lruCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the thread‑pooled strategy used by the threaded implementation.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    private void setUp(LRUCache.StrategyType strategy) {
        // Capacity 3 is enough to test normal behaviour and eviction.
        lruCache = new LRUCache<>(3, strategy);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllAddsAllEntries(LRUCache.StrategyType strategy) {
        setUp(strategy);
        Map<Integer, String> source = new HashMap<>();
        source.put(1, "A");
        source.put(2, "B");
        source.put(3, "C");
        lruCache.putAll(source);
        assertEquals(3, lruCache.size(), "Cache size should match source map size");
        assertEquals("A", lruCache.get(1));
        assertEquals("B", lruCache.get(2));
        assertEquals("C", lruCache.get(3));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllOverridesExistingKeys(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // Populate the cache with some initial values.
        lruCache.put(1, "old");
        lruCache.put(2, "old2");
        // New map contains a key that already exists and a new key.
        Map<Integer, String> source = new HashMap<>();
        // should overwrite
        source.put(1, "new");
        source.put(3, "brandNew");
        lruCache.putAll(source);
        assertEquals(3, lruCache.size());
        assertEquals("new", lruCache.get(1), "Existing key should be overwritten");
        assertEquals("old2", lruCache.get(2), "Unchanged key should retain its value");
        assertEquals("brandNew", lruCache.get(3));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllWithNullKeyAndValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        Map<Integer, String> source = new HashMap<>();
        source.put(null, null);
        source.put(1, null);
        source.put(null, "valueForNullKey");
        lruCache.putAll(source);
        assertTrue(lruCache.containsKey(null), "Cache should contain the null key");
        assertTrue(lruCache.containsValue("valueForNullKey"));
        assertTrue(lruCache.containsKey(1));
        assertTrue(lruCache.containsValue(null));
        assertEquals("valueForNullKey", lruCache.get(null));
        assertNull(lruCache.get(1));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllTriggersEvictionWhenCapacityExceeded(LRUCache.StrategyType strategy) {
        // Capacity is 3; we will put 2 entries, then putAll 3 more -> total 5.
        setUp(strategy);
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        Map<Integer, String> source = new HashMap<>();
        source.put(3, "C");
        source.put(4, "D");
        source.put(5, "E");
        lruCache.putAll(source);
        // Wait a little for the threaded strategy to perform cleanup.
        if (strategy == LRUCache.StrategyType.THREADED) {
            long deadline = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < deadline && lruCache.size() > 3) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
        }
        assertEquals(3, lruCache.size(), "Cache size must respect the configured capacity after putAll");
        // The two oldest entries (1 and 2) should have been evicted.
        assertFalse(lruCache.containsKey(1));
        assertFalse(lruCache.containsKey(2));
        // The three most recent entries must be present.
        assertTrue(lruCache.containsKey(3));
        assertTrue(lruCache.containsKey(4));
        assertTrue(lruCache.containsKey(5));
    }

    /**
     * putAll(null) must throw NullPointerException as required by the Map contract.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllNullArgumentThrowsNPE(LRUCache.StrategyType strategy) {
        setUp(strategy);
        assertThrows(NullPointerException.class, () -> lruCache.putAll(null));
    }

    /**
     * Putting an empty map must not modify the cache.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllEmptyMapNoChange(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put("a", 10);
        lruCache.put("b", 20);
        int originalSize = lruCache.size();
        lruCache.putAll(Collections.emptyMap());
        assertEquals(originalSize, lruCache.size(), "Size should stay unchanged after putting an empty map");
        assertEquals(10, lruCache.get("a"));
        assertEquals(20, lruCache.get("b"));
    }

    /**
     * Simple map with two entries – both should be added.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllSimpleTwoEntries(LRUCache.StrategyType strategy) {
        setUp(strategy);
        Map<String, Integer> source = Map.of("a", 1, "b", 2);
        lruCache.putAll(source);
        assertEquals(2, lruCache.size());
        assertEquals(1, lruCache.get("a"));
        assertEquals(2, lruCache.get("b"));
    }

    /**
     * When the source map contains duplicate keys (the later entry overwrites the former),
     * the cache must reflect the last value.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllDuplicateKeysLastWins(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // LinkedHashMap preserves insertion order – the second put for "k1" overwrites the first.
        Map<String, Integer> source = new LinkedHashMap<>() {

            {
                put("k1", 100);
                put("k2", 200);
                // duplicate key
                put("k1", 300);
            }
        };
        lruCache.putAll(source);
        assertEquals(2, lruCache.size());
        assertEquals(300, lruCache.get("k1"));
        assertEquals(200, lruCache.get("k2"));
    }

    /**
     * Very large map (1000 entries) – ensure bulk insertion works without data loss.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllVeryLargeMap(LRUCache.StrategyType strategy) {
        // Use a larger capacity so eviction does not interfere with the assertion.
        setUp(strategy, 2000);
        Map<String, Integer> source = new HashMap<>();
        for (int i = 0; i < 1000; i++) {
            source.put("k" + i, i);
        }
        lruCache.putAll(source);
        assertEquals(1000, lruCache.size());
        // Spot‑check a few entries.
        assertEquals(0, lruCache.get("k0"));
        assertEquals(500, lruCache.get("k500"));
        assertEquals(999, lruCache.get("k999"));
    }

    /**
     * The source map may be unmodifiable – putAll must read it without trying to modify it.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllUnmodifiableMap(LRUCache.StrategyType strategy) {
        setUp(strategy);
        Map<String, Integer> source = Collections.unmodifiableMap(Map.of("u1", 100, "u2", 200));
        lruCache.putAll(source);
        assertEquals(2, lruCache.size());
        assertEquals(100, lruCache.get("u1"));
        assertEquals(200, lruCache.get("u2"));
    }

    /**
     * Self‑reference: putting the cache into itself should be a no‑op and must not throw.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllSelfReferenceNoChange(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put("a", 1);
        lruCache.put("b", 2);
        int originalSize = lruCache.size();
        // The cache implements Map, so we can safely pass it to putAll.
        lruCache.putAll(lruCache);
        assertEquals(originalSize, lruCache.size(), "Self‑putAll must not change the size");
        assertEquals(1, lruCache.get("a"));
        assertEquals(2, lruCache.get("b"));
    }

    /**
     * TreeMap source – ensures putAll works with a sorted map implementation.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllTreeMap(LRUCache.StrategyType strategy) {
        setUp(strategy);
        Map<String, Integer> source = new TreeMap<>() {

            {
                put("a", 1);
                put("b", 2);
                put("c", 3);
            }
        };
        lruCache.putAll(source);
        assertEquals(3, lruCache.size());
        assertEquals(1, lruCache.get("a"));
        assertEquals(2, lruCache.get("b"));
        assertEquals(3, lruCache.get("c"));
    }

    /**
     * LinkedHashMap source – verifies that insertion order does not affect the final content.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllLinkedHashMapOrderPreserved(LRUCache.StrategyType strategy) {
        setUp(strategy);
        Map<String, Integer> source = new LinkedHashMap<>() {

            {
                put("first", 1);
                put("second", 2);
                put("third", 3);
            }
        };
        lruCache.putAll(source);
        assertEquals(3, lruCache.size());
        assertEquals(1, lruCache.get("first"));
        assertEquals(2, lruCache.get("second"));
        assertEquals(3, lruCache.get("third"));
    }

    /**
     * Mixed null keys/values using a mutable map (Map.ofEntries does not allow nulls).
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllMixedNulls(LRUCache.StrategyType strategy) {
        setUp(strategy);
        Map<String, Integer> source = new HashMap<>();
        source.put("k1", 1);
        // null key
        source.put(null, 2);
        // null value
        source.put("k2", null);
        lruCache.putAll(source);
        assertEquals(3, lruCache.size());
        assertEquals(1, lruCache.get("k1"));
        assertEquals(2, lruCache.get(null));
        assertNull(lruCache.get("k2"));
    }

    /**
     * Capacity‑limited eviction after putAll with more entries than the cache can hold.
     * Only the most recent entries (according to LRU policy) should remain.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllCapacityLimitedEviction(LRUCache.StrategyType strategy) {
        // Capacity 3, source map contains 5 entries.
        setUp(strategy, 3);
        Map<String, Integer> source = new LinkedHashMap<>() {

            {
                put("a", 1);
                put("b", 2);
                put("c", 3);
                put("d", 4);
                put("e", 5);
            }
        };
        lruCache.putAll(source);
        // Allow threaded strategy to finish cleanup.
        if (strategy == LRUCache.StrategyType.THREADED) {
            long deadline = System.currentTimeMillis() + 3000;
            while (System.currentTimeMillis() < deadline && lruCache.size() > 3) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            }
        }
        assertEquals(3, lruCache.size(), "Cache size must respect capacity after bulk insertion");
        // The three most recently added keys are "c","d","e".
        assertFalse(lruCache.containsKey("a"));
        assertFalse(lruCache.containsKey("b"));
        assertTrue(lruCache.containsKey("c"));
        assertTrue(lruCache.containsKey("d"));
        assertTrue(lruCache.containsKey("e"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAllPropagatesExceptionFromSourceMap(LRUCache.StrategyType strategy) {
        setUp(strategy);
        @SuppressWarnings("unchecked")
        Map<String, Integer> badMap = (Map<String, Integer>) org.mockito.Mockito.mock(Map.class);
        org.mockito.Mockito.when(badMap.entrySet()).thenThrow(new IllegalStateException("boom"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> lruCache.putAll(badMap));
        assertEquals("boom", ex.getMessage());
    }
}
