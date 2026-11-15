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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_containsValue_Test {

    private LRUCache<Integer, String> lruCache;

    static final class CustomKey {

        private final String id;

        CustomKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomKey))
                return false;
            CustomKey that = (CustomKey) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    static class BaseValue {

        private final String data;

        BaseValue(String data) {
            this.data = data;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof BaseValue))
                return false;
            BaseValue that = (BaseValue) o;
            return Objects.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(data);
        }
    }

    static class SubValue extends BaseValue {

        SubValue(String data) {
            super(data);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The cache is created per‑parameterised test in the setUp method.
        lruCache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        if (lruCache != null) {
            lruCache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Ensure the background thread used by the threaded strategy is stopped.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /* --------------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    private void setUp(LRUCache.StrategyType strategy) {
        // Small capacity is enough for the tests; the strategy determines the internal implementation.
        lruCache = new LRUCache<>(3, strategy);
    }

    /**
     * Helper that injects a mock {@code Map} into the private {@code strategy}
     * field of an {@link LRUCache} instance using reflection.
     */
    private static <K, V> void injectStrategyMock(LRUCache<K, V> cache, Map<K, V> mockMap) {
        try {
            Field f = LRUCache.class.getDeclaredField("strategy");
            f.setAccessible(true);
            f.set(cache, mockMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject mock strategy map", e);
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_returnsTrueWhenValueExists(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "alpha");
        lruCache.put(2, "beta");
        lruCache.put(3, "gamma");
        assertTrue(lruCache.containsValue("beta"), "Cache should report that it contains the value \"beta\"");
        assertTrue(lruCache.containsValue("alpha"));
        assertTrue(lruCache.containsValue("gamma"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_returnsFalseWhenValueMissing(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, "one");
        lruCache.put(2, "two");
        assertFalse(lruCache.containsValue("three"), "Cache should report that it does NOT contain the value \"three\"");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_handlesNullValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(1, null);
        lruCache.put(2, "notnull");
        assertTrue(lruCache.containsValue(null), "Cache should report that it contains a null value");
        assertTrue(lruCache.containsValue("notnull"));
        assertFalse(lruCache.containsValue("missing"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_reflectsEviction(LRUCache.StrategyType strategy) {
        setUp(strategy);
        // Fill the cache to capacity
        lruCache.put(1, "A");
        lruCache.put(2, "B");
        lruCache.put(3, "C");
        // Access key 1 to make it most‑recently used
        lruCache.get(1);
        // Adding a fourth entry forces eviction of the least‑recently used entry (key 2)
        lruCache.put(4, "D");
        // For the threaded strategy eviction happens asynchronously; wait a short while.
        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            if (!lruCache.containsKey(2)) {
                break;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        assertFalse(lruCache.containsKey(2), "Key 2 should have been evicted");
        assertFalse(lruCache.containsValue("B"), "Value \"B\" should no longer be present after eviction");
        assertTrue(lruCache.containsValue("A"));
        assertTrue(lruCache.containsValue("D"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsValue_withNullKeyAndValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        lruCache.put(null, null);
        assertTrue(lruCache.containsKey(null), "Cache should contain the null key");
        assertTrue(lruCache.containsValue(null), "Cache should contain the null value");
    }

    @Test
    void containsValue_delegatesToStrategy_true() {
        // Arrange
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.containsValue("apple")).thenReturn(true);
        injectStrategyMock(cache, mockMap);
        // Act & Assert
        assertTrue(cache.containsValue("apple"));
        verify(mockMap).containsValue("apple");
    }

    @Test
    void containsValue_delegatesToStrategy_false() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.containsValue("banana")).thenReturn(false);
        injectStrategyMock(cache, mockMap);
        assertFalse(cache.containsValue("banana"));
        verify(mockMap).containsValue("banana");
    }

    @Test
    void containsValue_nullValuePresent_true() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.containsValue(null)).thenReturn(true);
        injectStrategyMock(cache, mockMap);
        assertTrue(cache.containsValue(null));
        verify(mockMap).containsValue(null);
    }

    @Test
    void containsValue_nullValueAbsent_false() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.containsValue(null)).thenReturn(false);
        injectStrategyMock(cache, mockMap);
        assertFalse(cache.containsValue(null));
        verify(mockMap).containsValue(null);
    }

    @Test
    void containsValue_propagatesRuntimeException() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.containsValue("exception")).thenThrow(new RuntimeException("boom"));
        injectStrategyMock(cache, mockMap);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.containsValue("exception"));
        assertEquals("boom", ex.getMessage());
        verify(mockMap).containsValue("exception");
    }

    @Test
    void containsValue_emptyCache_returnsFalse() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        // No entries added – real strategy map will be empty.
        assertFalse(cache.containsValue("anyValue"));
    }

    @Test
    void containsValue_duplicateValues_true() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put(1, "shared");
        cache.put(2, "shared");
        assertTrue(cache.containsValue("shared"));
    }

    @Test
    void containsValue_customKeyEqualInstance_true() {
        LRUCache<CustomKey, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put(new CustomKey("id1"), "value");
        // Query with a *different* instance that is equal according to equals()
        assertTrue(cache.containsValue(new CustomKey("id1")));
    }

    @Test
    void containsValue_customKeyDifferentInstance_false() {
        LRUCache<CustomKey, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put(new CustomKey("id1"), "value");
        assertFalse(cache.containsValue(new CustomKey("different")));
    }

    @Test
    void containsValue_removedEntry_false() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put(1, "toBeRemoved");
        cache.remove(1);
        assertFalse(cache.containsValue("toBeRemoved"));
    }

    @Test
    void containsValue_integerValue_true() {
        LRUCache<Integer, Integer> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put(1, 42);
        assertTrue(cache.containsValue(42));
    }

    @Test
    void containsValue_listEquality_true() {
        LRUCache<Integer, List<Integer>> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        List<Integer> stored = Arrays.asList(1, 2);
        cache.put(1, stored);
        // A new list instance with the same contents should be considered equal.
        assertTrue(cache.containsValue(Arrays.asList(1, 2)));
    }

    @Test
    void containsValue_subclassEquality_true() {
        LRUCache<Integer, BaseValue> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put(1, new SubValue("data"));
        // Query with a BaseValue reference that is equal (same data)
        assertTrue(cache.containsValue(new BaseValue("data")));
    }

    @Test
    void containsValue_nullStrategy_throwsNullPointerException() throws Exception {
        // Construct a cache and then null‑out its internal strategy field.
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        // deliberately break the cache
        f.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.containsValue("anyValue"), "Calling containsValue on a cache with a null strategy should throw NPE");
    }

    @Test
    void containsValue_propagatesUnsupportedOperationException() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.containsValue("unsupported")).thenThrow(new UnsupportedOperationException("not supported"));
        injectStrategyMock(cache, mockMap);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> cache.containsValue("unsupported"));
        assertEquals("not supported", ex.getMessage());
        verify(mockMap).containsValue("unsupported");
    }

    @Test
    void containsValue_propagatesClassCastException() {
        LRUCache<Integer, String> cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Integer, String> mockMap = mock(Map.class);
        when(mockMap.containsValue("badType")).thenThrow(new ClassCastException("bad type"));
        injectStrategyMock(cache, mockMap);
        ClassCastException ex = assertThrows(ClassCastException.class, () -> cache.containsValue("badType"));
        assertEquals("bad type", ex.getMessage());
        verify(mockMap).containsValue("badType");
    }
}
