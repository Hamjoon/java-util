package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.cache.LockingLRUCacheStrategy;
import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_put_Test {

    private LRUCache<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // The cache is created in each test (or in the parameterized test's
        // given‑phase) so that the strategy can be varied per test case.
        cache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to aid GC; not strictly required.
        if (cache != null) {
            cache.clear();
        }
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Helper that injects a mock {@code Map} into the private {@code strategy}
     * field of the cache via reflection.
     */
    private void injectMockStrategy(LRUCache<?, ?> targetCache, Map<?, ?> mockStrategy) throws Exception {
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(targetCache, mockStrategy);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putShouldReturnPreviousValueAndStoreNewValue(LRUCache.StrategyType strategy) {
        // given
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        // when – inserting a new key
        String previousForNewKey = cache.put(3, "three");
        // then – previous value must be null and the new entry must be retrievable
        assertNull(previousForNewKey, "put should return null when the key was not present");
        assertEquals("three", cache.get(3), "The newly inserted value must be retrievable");
        // when – inserting an existing key
        String previousForExistingKey = cache.put(2, "TWO");
        // then – previous value must be the old one and the map must reflect the update
        assertEquals("two", previousForExistingKey, "put should return the previous value for an existing key");
        assertEquals("TWO", cache.get(2), "The value associated with the key must be updated");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putShouldAcceptNullKeyAndNullValue(LRUCache.StrategyType strategy) {
        // given
        cache = new LRUCache<>(5, strategy);
        // when – null key with a non‑null value
        cache.put(null, "nullKey");
        // then
        assertTrue(cache.containsKey(null), "Cache must contain the null key");
        assertEquals("nullKey", cache.get(null), "Value for null key must be stored correctly");
        // when – null key with null value (overwrite)
        cache.put(null, null);
        // then
        assertTrue(cache.containsKey(null), "Cache must still contain the null key after overwrite");
        assertNull(cache.get(null), "Value for null key must now be null");
        assertTrue(cache.containsValue(null), "Cache must report containing a null value");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putShouldRespectCapacityAndTriggerEviction(LRUCache.StrategyType strategy) {
        // given – a cache with capacity 3
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        // when – access key 1 to make it most‑recently used, then add a fourth entry
        cache.get(1);
        cache.put(4, "D");
        // then – key 2 should be evicted (LRU), others must remain
        // For the threaded strategy eviction may be asynchronous, so we wait briefly.
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            if (!cache.containsKey(2)) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        assertNull(cache.get(2), "Key 2 should have been evicted");
        assertEquals("A", cache.get(1), "Key 1 must still be present");
        assertEquals("D", cache.get(4), "Key 4 must be present");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putShouldPropagateIllegalStateException(LRUCache.StrategyType strategy) throws Exception {
        // Arrange – create a cache and replace its strategy with a mock that throws
        LRUCache<Integer, String> realCache = new LRUCache<>(5, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.put(anyInt(), anyString())).thenThrow(new IllegalStateException("cache full"));
        injectMockStrategy(realCache, mockStrategy);
        // for teardown
        this.cache = realCache;
        // Act & Assert
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cache.put(1, "one"));
        assertEquals("cache full", ex.getMessage());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putShouldPropagateUnsupportedOperationException(LRUCache.StrategyType strategy) throws Exception {
        LRUCache<Integer, String> realCache = new LRUCache<>(5, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.put(anyInt(), anyString())).thenThrow(new UnsupportedOperationException("unmodifiable"));
        injectMockStrategy(realCache, mockStrategy);
        this.cache = realCache;
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> cache.put(2, "two"));
        assertEquals("unmodifiable", ex.getMessage());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putShouldPropagateClassCastException(LRUCache.StrategyType strategy) throws Exception {
        // Use a raw LRUCache to bypass compile‑time generic checks.
        LRUCache rawCache = new LRUCache(5, strategy);
        @SuppressWarnings("unchecked")
        Map rawMock = mock(Map.class);
        when(rawMock.put(any(), any())).thenThrow(new ClassCastException("bad key type"));
        injectMockStrategy(rawCache, rawMock);
        // for cleanup
        this.cache = rawCache;
        ClassCastException ex = assertThrows(ClassCastException.class, () -> rawCache.put(123, "value"));
        assertEquals("bad key type", ex.getMessage());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putShouldPropagateIllegalArgumentException(LRUCache.StrategyType strategy) throws Exception {
        LRUCache<Integer, String> realCache = new LRUCache<>(5, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.put(anyInt(), anyString())).thenThrow(new IllegalArgumentException("illegal key"));
        injectMockStrategy(realCache, mockStrategy);
        this.cache = realCache;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cache.put(99, "bad"));
        assertEquals("illegal key", ex.getMessage());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void putShouldPropagateNullPointerExceptionFromStrategy(LRUCache.StrategyType strategy) throws Exception {
        LRUCache<Integer, String> realCache = new LRUCache<>(5, strategy);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockStrategy = mock(Map.class);
        when(mockStrategy.put(isNull(), anyString())).thenThrow(new NullPointerException("null keys not allowed"));
        injectMockStrategy(realCache, mockStrategy);
        this.cache = realCache;
        NullPointerException ex = assertThrows(NullPointerException.class, () -> cache.put(null, "value"));
        assertEquals("null keys not allowed", ex.getMessage());
    }

    /**
     * Simulates the situation where the LRUCache was constructed with a {@code null}
     * strategy field (e.g., via reflection or a broken constructor). The call to
     * {@code put} should result in a {@link NullPointerException}.
     */
    @Test
    void putShouldThrowNullPointerExceptionWhenStrategyIsNull() throws Exception {
        // Construct a normal cache first.
        LRUCache<Integer, String> realCache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        // Force the private strategy field to null.
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(realCache, null);
        // for teardown
        this.cache = realCache;
        NullPointerException ex = assertThrows(NullPointerException.class, () -> cache.put(1, "one"));
        // The exact message is implementation‑dependent; we only assert that NPE occurs.
        assertNotNull(ex.getMessage());
    }
}
