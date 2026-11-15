package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.mockito.Mockito;
import java.lang.reflect.Field;

class LRUCache_putAll_Test {

    private LRUCache<Integer, String> cache;

    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.shutdown();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Ensure any threaded background executors are stopped.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Provide the two supported strategy types.
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Create a cache with a small capacity for the given strategy.
     */
    void initCache(LRUCache.StrategyType type) {
        // capacity 5 is enough for the tests and keeps eviction out of the way
        cache = new LRUCache<>(5, type);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putAllCopiesAllEntries(LRUCache.StrategyType strategy) {
        initCache(strategy);
        Map<Integer, String> source = new LinkedHashMap<>();
        source.put(1, "one");
        source.put(2, "two");
        source.put(3, "three");
        cache.putAll(source);
        assertEquals(3, cache.size(), "Cache should contain three entries after putAll");
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putAllOverridesExistingEntries(LRUCache.StrategyType strategy) {
        initCache(strategy);
        cache.put(1, "old");
        cache.put(2, "old2");
        Map<Integer, String> update = new HashMap<>();
        update.put(1, "new");
        update.put(3, "new3");
        cache.putAll(update);
        assertEquals(3, cache.size());
        assertEquals("new", cache.get(1));
        assertEquals("old2", cache.get(2));
        assertEquals("new3", cache.get(3));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putAllAcceptsNullKeyAndValue(LRUCache.StrategyType strategy) {
        initCache(strategy);
        Map<Integer, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put(null, null);
        mapWithNulls.put(5, null);
        mapWithNulls.put(null, "valueForNullKey");
        cache.putAll(mapWithNulls);
        // The last value for the null key should win.
        assertTrue(cache.containsKey(null));
        assertEquals("valueForNullKey", cache.get(null));
        // Null value for a non‑null key is allowed.
        assertTrue(cache.containsKey(5));
        assertNull(cache.get(5));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putAllDoesNotAffectOtherCacheState(LRUCache.StrategyType strategy) {
        initCache(strategy);
        cache.put(10, "ten");
        cache.put(20, "twenty");
        Map<Integer, String> extra = Collections.singletonMap(30, "thirty");
        cache.putAll(extra);
        // Existing entries must still be present.
        assertEquals("ten", cache.get(10));
        assertEquals("twenty", cache.get(20));
        assertEquals("thirty", cache.get(30));
        assertEquals(3, cache.size());
    }

    /**
     * putAll(null) must throw NullPointerException as per Map contract.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void putAllNullArgumentThrowsNPE(LRUCache.StrategyType strategy) {
        initCache(strategy);
        assertThrows(NullPointerException.class, () -> cache.putAll(null), "Calling putAll with null should throw NullPointerException");
    }

    /**
     * Verify that an empty map is delegated to the underlying strategy exactly once
     * and does not change the cache size.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void putAllEmptyMapDelegatesAndLeavesCacheUnchanged(LRUCache.StrategyType strategy) throws Exception {
        // Create a real cache then replace its internal strategy with a Mockito mock.
        initCache(strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        // make the mock behave like a normal map for size() queries
        when(mockStrategy.size()).thenReturn(0);
        // inject the mock
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, mockStrategy);
        Map<Integer, String> empty = Collections.emptyMap();
        cache.putAll(empty);
        // Verify delegation
        verify(mockStrategy, times(1)).putAll(empty);
        // The cache should still report size 0 (the mock returns 0)
        assertEquals(0, cache.size(), "Cache size must stay 0 after putting an empty map");
    }

    /**
     * Verify delegation of a non‑empty map and that entries become visible through the cache.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void putAllDelegatesAndAddsEntries(LRUCache.StrategyType strategy) throws Exception {
        initCache(strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        // Let the mock store entries in a real HashMap so that subsequent size()/get() work.
        Map<Integer, String> backing = new HashMap<>();
        doAnswer(invocation -> {
            Map<Integer, String> m = invocation.getArgument(0);
            backing.putAll(m);
            return null;
        }).when(mockStrategy).putAll(anyMap());
        when(mockStrategy.size()).thenAnswer(inv -> backing.size());
        when(mockStrategy.get(any())).thenAnswer(inv -> backing.get(inv.getArgument(0)));
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, mockStrategy);
        Map<Integer, String> source = Map.of(1, "one", 2, "two", 3, "three");
        cache.putAll(source);
        verify(mockStrategy, times(1)).putAll(source);
        assertEquals(3, cache.size());
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
    }

    /**
     * When the supplied map contains keys that already exist,
     * putAll must replace the old values with the new ones.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void putAllOverridesExistingEntriesViaDelegation(LRUCache.StrategyType strategy) throws Exception {
        initCache(strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        Map<Integer, String> backing = new HashMap<>();
        // mimic normal putAll behaviour
        doAnswer(inv -> {
            Map<Integer, String> m = inv.getArgument(0);
            backing.putAll(m);
            return null;
        }).when(mockStrategy).putAll(anyMap());
        when(mockStrategy.size()).thenAnswer(inv -> backing.size());
        when(mockStrategy.get(any())).thenAnswer(inv -> backing.get(inv.getArgument(0)));
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, mockStrategy);
        // pre‑populate cache
        cache.put(1, "old");
        cache.put(2, "old2");
        Map<Integer, String> update = Map.of(1, "new", 3, "new3");
        cache.putAll(update);
        verify(mockStrategy, times(1)).putAll(update);
        assertEquals(3, cache.size());
        assertEquals("new", cache.get(1));
        assertEquals("old2", cache.get(2));
        assertEquals("new3", cache.get(3));
    }

    /**
     * LRU eviction test: capacity = 3, cache pre‑populated with three entries,
     * then putAll three new entries. The original three must be evicted.
     */
    @Test
    void putAllEvictsWhenCapacityExceeded_LockingStrategy() {
        // Use the locking strategy for deterministic eviction.
        LRUCache<String, Integer> evictCache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        try {
            evictCache.put("a", 1);
            evictCache.put("b", 2);
            evictCache.put("c", 3);
            // New entries – should cause eviction of a,b,c (LRU order)
            Map<String, Integer> newEntries = new LinkedHashMap<>();
            newEntries.put("d", 4);
            newEntries.put("e", 5);
            newEntries.put("f", 6);
            evictCache.putAll(newEntries);
            // Verify only the new keys remain.
            assertEquals(3, evictCache.size(), "Cache size must stay at its capacity");
            assertNull(evictCache.get("a"));
            assertNull(evictCache.get("b"));
            assertNull(evictCache.get("c"));
            assertEquals(4, evictCache.get("d"));
            assertEquals(5, evictCache.get("e"));
            assertEquals(6, evictCache.get("f"));
        } finally {
            evictCache.shutdown();
        }
    }

    /**
     * LRU eviction with overlapping key: capacity = 2, pre‑populate with x and y,
     * then putAll containing y (updated) and z (new). x should be evicted.
     */
    @Test
    void putAllEvictsWhenCapacityExceeded_WithOverwrite_LockingStrategy() {
        LRUCache<String, Integer> evictCache = new LRUCache<>(2, LRUCache.StrategyType.LOCKING);
        try {
            evictCache.put("x", 10);
            evictCache.put("y", 20);
            Map<String, Integer> update = Map.of("y", 30, "z", 40);
            evictCache.putAll(update);
            assertEquals(2, evictCache.size());
            // evicted
            assertNull(evictCache.get("x"));
            // updated
            assertEquals(30, evictCache.get("y"));
            // new entry
            assertEquals(40, evictCache.get("z"));
        } finally {
            evictCache.shutdown();
        }
    }

    /**
     * Type‑mismatch: cache expects {@code String} keys but the supplied map uses {@code Integer} keys.
     * The underlying strategy should throw {@link ClassCastException}.
     */
    @Test
    void putAllTypeMismatchKeyThrowsClassCastException() {
        LRUCache<String, Integer> cache = new LRUCache<>(10, LRUCache.StrategyType.THREADED);
        try {
            Map<Integer, Integer> wrongMap = Map.of(1, 100);
            assertThrows(ClassCastException.class, () -> cache.putAll(wrongMap), "Putting a map with incompatible key type must throw ClassCastException");
        } finally {
            cache.shutdown();
        }
    }

    /**
     * Type‑mismatch: cache expects {@code Integer} values but the supplied map provides {@code String} values.
     */
    @Test
    void putAllTypeMismatchValueThrowsClassCastException() {
        LRUCache<String, Integer> cache = new LRUCache<>(10, LRUCache.StrategyType.THREADED);
        try {
            Map<String, String> wrongMap = Map.of("key", "not-an-integer");
            assertThrows(ClassCastException.class, () -> cache.putAll(wrongMap), "Putting a map with incompatible value type must throw ClassCastException");
        } finally {
            cache.shutdown();
        }
    }

    /**
     * Verify that a map containing a null key and a null value is correctly forwarded.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void putAllForwardsNullKeyAndValue(LRUCache.StrategyType strategy) throws Exception {
        initCache(strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        Map<Integer, String> backing = new HashMap<>();
        doAnswer(inv -> {
            Map<Integer, String> m = inv.getArgument(0);
            backing.putAll(m);
            return null;
        }).when(mockStrategy).putAll(anyMap());
        when(mockStrategy.get(any())).thenAnswer(inv -> backing.get(inv.getArgument(0)));
        when(mockStrategy.size()).thenAnswer(inv -> backing.size());
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, mockStrategy);
        Map<Integer, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put(null, "nullKeyValue");
        mapWithNulls.put(7, null);
        cache.putAll(mapWithNulls);
        verify(mockStrategy, times(1)).putAll(mapWithNulls);
        assertTrue(cache.containsKey(null));
        assertEquals("nullKeyValue", cache.get(null));
        assertTrue(cache.containsKey(7));
        assertNull(cache.get(7));
    }
}
