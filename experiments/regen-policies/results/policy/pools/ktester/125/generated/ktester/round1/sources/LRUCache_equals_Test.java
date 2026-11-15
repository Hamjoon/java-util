package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_equals_Test {

    private LRUCache<Integer, String> cache;

    /**
     * Runs once before all tests – shuts down any background threads used by the threaded strategy.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // No global resources needed for the locking strategy,
        // but we must ensure the threaded strategy can be cleaned up after the whole suite.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Creates a fresh cache instance before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Use a small capacity; the capacity is irrelevant for equals tests.
        cache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
    }

    /**
     * No per‑test teardown required – the cache will be garbage‑collected.
     */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to avoid any lingering references.
        cache.clear();
    }

    /**
     * No per‑suite teardown required – already handled in {@link #setupBeforeAll()}.
     */
    @AfterAll
    static void teardownAfterAll() {
        // Ensure any threaded resources are released (idempotent if already shut down).
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void equalsIsReflexive() {
        assertTrue(cache.equals(cache), "Cache should be equal to itself");
    }

    @Test
    void equalsWithNullIsFalse() {
        assertFalse(cache.equals(null), "Cache compared with null must return false");
    }

    @Test
    void equalsWithNonMapIsFalse() {
        assertFalse(cache.equals("not a map"), "Cache compared with a non‑Map object must return false");
    }

    @Test
    void equalsWhenContentsMatch() {
        // Populate first cache
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // Create a second cache with the same entries (different instance)
        LRUCache<Integer, String> other = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        other.put(1, "one");
        other.put(2, "two");
        other.put(3, "three");
        assertTrue(cache.equals(other), "Caches with identical contents should be equal");
        assertTrue(other.equals(cache), "Equality must be symmetric");
    }

    @Test
    void equalsWhenContentsDiffer() {
        cache.put(1, "one");
        cache.put(2, "two");
        LRUCache<Integer, String> other = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        other.put(1, "one");
        // different value
        other.put(2, "different");
        assertFalse(cache.equals(other), "Caches with different values should not be equal");
        assertFalse(other.equals(cache), "Equality must be symmetric even when false");
    }

    @Test
    void equalsIgnoresStrategyImplementation() {
        // One cache using LOCKING, another using THREADED – same content.
        LRUCache<Integer, String> lockingCache = new LRUCache<>(3, LRUCache.StrategyType.LOCKING);
        LRUCache<Integer, String> threadedCache = new LRUCache<>(3, LRUCache.StrategyType.THREADED);
        lockingCache.put(10, "ten");
        threadedCache.put(10, "ten");
        // The equals method delegates to the underlying strategy's Map.equals,
        // which only cares about the map entries, not the concrete strategy class.
        assertTrue(lockingCache.equals(threadedCache), "Caches with identical entries should be equal regardless of strategy type");
    }

    /**
     * Verify that a {@code NullPointerException} is thrown when the internal
     * {@code strategy} map is {@code null}. The method attempts to call
     * {@code strategy.equals(other)} and therefore fails.
     */
    @Test
    void equalsWithNullStrategyThrowsNPE() throws Exception {
        // Use reflection to set the private 'strategy' field to null.
        Field strategyField = LRUCache.class.getDeclaredField("strategy");
        strategyField.setAccessible(true);
        strategyField.set(cache, null);
        Map<String, String> other = new HashMap<>();
        other.put("key", "value");
        // The contract of LRUCache.equals does not guard against a null strategy,
        // so a NPE is expected.
        assertThrows(NullPointerException.class, () -> cache.equals(other), "When internal strategy is null, equals should throw NullPointerException");
    }

    /**
     * Ensure that if the underlying strategy's {@code equals} method throws an
     * {@link IllegalStateException}, the exception propagates out of
     * {@link LRUCache#equals(Object)} unchanged.
     */
    @Test
    void equalsWhenStrategyEqualsThrowsPropagatesException(@Mock Map<Integer, String> mockStrategy) {
        // Stub the mock to throw IllegalStateException on any equals call.
        when(mockStrategy.equals(any())).thenThrow(new IllegalStateException("strategy failure"));
        // Replace the private 'strategy' field with the mock.
        try {
            Field strategyField = LRUCache.class.getDeclaredField("strategy");
            strategyField.setAccessible(true);
            strategyField.set(cache, mockStrategy);
        } catch (ReflectiveOperationException e) {
            fail("Reflection failed while injecting mock strategy: " + e);
        }
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "one");
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> cache.equals(other), "Exception from strategy.equals should be propagated");
        assertEquals("strategy failure", thrown.getMessage(), "Propagated exception should retain the original message");
    }
}
