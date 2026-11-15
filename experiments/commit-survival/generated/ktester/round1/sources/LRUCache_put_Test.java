package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_put_Test {

    /**
     * The cache instance used by each test.
     */
    private LRUCache<Integer, String> cache;

    /**
     * Clean up after each test – ensure the cache reference is cleared.
     */
    @AfterEach
    void teardownAfterEach() {
        cache = null;
    }

    /**
     * Shut down the thread‑pooled strategy after all tests have run.
     */
    @AfterAll
    static void teardownAfterAll() {
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Provide the two supported strategy types for the parameterized tests.
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Basic put / get verification for both strategies.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAndGet(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        assertTrue(cache.isEmpty());
        assertNull(cache.put(1, "A"));
        assertNull(cache.put(2, "B"));
        assertNull(cache.put(3, "C"));
        assertEquals("A", cache.get(1));
        assertEquals("B", cache.get(2));
        assertEquals("C", cache.get(3));
        assertEquals(3, cache.size());
    }

    /**
     * Verify that put returns the previous value associated with the key.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutReturnsPreviousValue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(2, strategy);
        assertNull(cache.put(10, "first"));
        assertEquals("first", cache.get(10));
        // Overwrite the same key – should return the old value.
        String previous = cache.put(10, "second");
        assertEquals("first", previous);
        assertEquals("second", cache.get(10));
    }

    /**
     * Ensure that null keys and null values are accepted and stored correctly.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutNullKeyAndValue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        // null key, non‑null value
        assertNull(cache.put(null, "value"));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue("value"));
        assertEquals("value", cache.get(null));
        // non‑null key, null value
        assertNull(cache.put(1, null));
        assertTrue(cache.containsKey(1));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(1));
        // null key, null value
        assertNull(cache.put(null, null));
        assertTrue(cache.containsKey(null));
        assertTrue(cache.containsValue(null));
        assertNull(cache.get(null));
    }

    /**
     * Simple eviction check – when capacity is exceeded the least‑recently‑used entry disappears.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testEvictionOnPut(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(2, strategy);
        // cache: 1
        cache.put(1, "A");
        // cache: 1,2
        cache.put(2, "B");
        // access 1 → makes 2 LRU
        cache.get(1);
        // should evict key 2
        cache.put(3, "C");
        // Because the threaded strategy evicts asynchronously, we wait a short moment.
        // The locking strategy evicts immediately.
        if (strategy == LRUCache.StrategyType.THREADED) {
            try {
                // give cleanup thread time to run
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
        assertNull(cache.get(2), "Key 2 should have been evicted");
        assertEquals("A", cache.get(1));
        assertEquals("C", cache.get(3));
    }

    /**
     * Strategy exception handling – the underlying map throws UnsupportedOperationException.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testStrategyThrowsUnsupportedOperationException(LRUCache.StrategyType strategy) throws Exception {
        cache = new LRUCache<>(3, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.put(anyInt(), anyString())).thenThrow(new UnsupportedOperationException("mock"));
        // Inject the mock map into the private final field 'strategy' via reflection.
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, mockMap);
        assertThrows(UnsupportedOperationException.class, () -> cache.put(1, "X"), "UnsupportedOperationException should be propagated");
    }

    /**
     * ClassCastException when an incompatible key type is supplied.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testClassCastExceptionIncompatibleKey() {
        // Use raw type to bypass compile‑time generics.
        LRUCache rawCache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        assertThrows(ClassCastException.class, () -> rawCache.put(123, "valid"), "Putting an Integer key into a LRUCache<String,?> should throw ClassCastException");
    }

    /**
     * ClassCastException when an incompatible value type is supplied.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void testClassCastExceptionIncompatibleValue() {
        LRUCache rawCache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        assertThrows(ClassCastException.class, () -> rawCache.put("validKey", true), "Putting a Boolean value into a LRUCache<?,String> should throw ClassCastException");
    }

    /**
     * Put after cache shutdown should throw IllegalStateException.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testPutAfterShutdownThrows(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.shutdown();
        assertThrows(IllegalStateException.class, () -> cache.put(99, "afterShutdown"), "Putting after shutdown must raise IllegalStateException");
    }

    /**
     * Duplicate null key handling – second put replaces the previous value.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testDuplicateNullKeyHandling(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        assertNull(cache.put(null, 7));
        // second insertion with same null key
        String previous = cache.put(null, 8);
        assertEquals("7", previous, "Previous value for null key should be returned");
        assertEquals("8", cache.get(null));
    }

    /**
     * Large number of sequential puts – verify update of an existing entry under load.
     */
    @ParameterizedTest
    @MethodSource("strategies")
    void testLargeSequentialPuts(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(1000, strategy);
        // Populate 0..998
        for (int i = 0; i < 999; i++) {
            cache.put(i, String.valueOf(i));
        }
        // key500 already exists with value "500"
        String previous = cache.put(500, "5000");
        assertEquals("500", previous);
        assertEquals("5000", cache.get(500));
        assertEquals(1000, cache.size());
    }

    /**
     * NullPointerException when underlying strategy is a ConcurrentHashMap (null key).
     */
    @Test
    void testNullKeyWithConcurrentHashMapStrategy() throws Exception {
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        // Replace strategy with a real ConcurrentHashMap (which rejects nulls)
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> cache.put(null, 42), "ConcurrentHashMap does not allow null keys");
    }

    /**
     * NullPointerException when underlying strategy is a ConcurrentHashMap (null value).
     */
    @Test
    void testNullValueWithConcurrentHashMapStrategy() throws Exception {
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, new ConcurrentHashMap<>());
        assertThrows(NullPointerException.class, () -> cache.put(1, null), "ConcurrentHashMap does not allow null values");
    }

    /**
     * IllegalArgumentException from a custom strategy that throws for a specific key.
     */
    @Test
    void testCustomStrategyThrowsIllegalArgumentException() throws Exception {
        cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        // Custom map that throws IllegalArgumentException when key equals "badKey"
        Map<String, String> customMap = new HashMap<String, String>() {

            @Override
            public String put(String key, String value) {
                if ("badKey".equals(key)) {
                    throw new IllegalArgumentException("bad key");
                }
                return super.put(key, value);
            }
        };
        // Inject the custom map
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, customMap);
        assertThrows(IllegalArgumentException.class, () -> cache.put("badKey", "trigger"), "Custom strategy should propagate IllegalArgumentException");
    }
}
