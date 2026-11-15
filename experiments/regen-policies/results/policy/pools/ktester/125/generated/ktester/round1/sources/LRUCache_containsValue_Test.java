package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;

class LRUCache_containsValue_Test {

    private LRUCache<Integer, String> cache;

    /**
     * Simple POJO with custom {@code equals}/{@code hashCode} used to verify that
     * {@code containsValue} relies on the value's equality contract.
     */
    static class Foo {

        private final int id;

        Foo(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Foo))
                return false;
            Foo foo = (Foo) o;
            return id == foo.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default we use the LOCKING strategy; individual tests can
        // replace the cache with a different strategy via the parameterized
        // test method.
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid cross‑test contamination.
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // The threaded strategy uses a static executor that must be stopped.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    private void injectMockStrategy(Map<Integer, String> mockStrategy) throws Exception {
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, mockStrategy);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_returnsTrueWhenValueExists(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "alpha");
        cache.put(2, "beta");
        cache.put(3, "gamma");
        assertTrue(cache.containsValue("beta"), "containsValue should return true for a value that is present");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_returnsFalseWhenValueDoesNotExist(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "alpha");
        cache.put(2, "beta");
        assertFalse(cache.containsValue("delta"), "containsValue should return false for a value that is not present");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_handlesNullValue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(3, strategy);
        cache.put(1, null);
        cache.put(2, "notNull");
        assertTrue(cache.containsValue(null), "containsValue should correctly detect a stored null value");
        assertFalse(cache.containsValue("missing"), "containsValue should still return false for a non‑existent value");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_reflectsEvictionAfterCapacityExceeded(LRUCache.StrategyType strategy) {
        // Use a small capacity to force eviction.
        cache = new LRUCache<>(2, strategy);
        cache.put(1, "first");
        cache.put(2, "second");
        // Access key 1 to make it recently used.
        cache.get(1);
        // Adding a third entry should evict key 2 (the least recently used).
        cache.put(3, "third");
        // Allow the threaded strategy a moment to clean up if needed.
        if (strategy == LRUCache.StrategyType.THREADED) {
            try {
                // short pause; the cleanup thread runs quickly
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
        assertFalse(cache.containsValue("second"), "The evicted entry's value should no longer be reported by containsValue");
        assertTrue(cache.containsValue("first"), "The retained entry's value should still be reported");
        assertTrue(cache.containsValue("third"));
    }

    @Test
    void containsValue_onEmptyCacheReturnsFalse() {
        // cache is created empty in @BeforeEach
        assertFalse(cache.containsValue("any"), "An empty cache must never report that it contains a value");
    }

    @Test
    void containsValue_afterRemovalReturnsFalse() {
        cache.put(1, "one");
        cache.put(2, "two");
        // remove the entry that holds "two"
        cache.remove(2);
        assertFalse(cache.containsValue("two"), "After removal the value should no longer be reported");
        assertTrue(cache.containsValue("one"));
    }

    @Test
    void containsValue_withDuplicateValuesReturnsTrue() {
        cache.put(1, "dup");
        cache.put(2, "dup");
        assertTrue(cache.containsValue("dup"), "When multiple keys map to the same value, containsValue must still be true");
    }

    @Test
    void containsValue_afterClearReturnsFalse() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.clear();
        assertFalse(cache.containsValue("one"));
        assertFalse(cache.containsValue("two"));
    }

    @Test
    void containsValue_afterShutdownThrowsNullPointerException() {
        cache.put(1, "one");
        // expected to null out the underlying strategy
        cache.shutdown();
        assertThrows(NullPointerException.class, () -> cache.containsValue("one"), "After shutdown the underlying strategy is null, so containsValue should NPE");
    }

    @Test
    void containsValue_withCustomObjectEqualityReturnsTrue() {
        LRUCache<Integer, Foo> fooCache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        fooCache.put(1, new Foo(1));
        fooCache.put(2, new Foo(2));
        // A different instance but equal according to Foo.equals()
        Foo lookup = new Foo(1);
        assertTrue(fooCache.containsValue(lookup), "Foo objects with same internal state should be considered equal");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_propagatesRuntimeException(LRUCache.StrategyType strategy) throws Exception {
        cache = new LRUCache<>(3, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mock = mock(Map.class);
        when(mock.containsValue(any())).thenThrow(new RuntimeException("boom"));
        injectMockStrategy(mock);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.containsValue("any"), "RuntimeException from the strategy must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_propagatesNullPointerException(LRUCache.StrategyType strategy) throws Exception {
        cache = new LRUCache<>(3, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mock = mock(Map.class);
        when(mock.containsValue(any())).thenThrow(new NullPointerException("np"));
        injectMockStrategy(mock);
        assertThrows(NullPointerException.class, () -> cache.containsValue("any"), "NullPointerException from the strategy must propagate");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_propagatesUnsupportedOperationException(LRUCache.StrategyType strategy) throws Exception {
        cache = new LRUCache<>(3, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mock = mock(Map.class);
        when(mock.containsValue(any())).thenThrow(new UnsupportedOperationException("unsupported"));
        injectMockStrategy(mock);
        assertThrows(UnsupportedOperationException.class, () -> cache.containsValue("any"), "UnsupportedOperationException must propagate");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_propagatesClassCastException(LRUCache.StrategyType strategy) throws Exception {
        cache = new LRUCache<>(3, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mock = mock(Map.class);
        when(mock.containsValue(any())).thenThrow(new ClassCastException("bad cast"));
        injectMockStrategy(mock);
        assertThrows(ClassCastException.class, () -> cache.containsValue(new Object()), "ClassCastException must propagate");
    }
}
