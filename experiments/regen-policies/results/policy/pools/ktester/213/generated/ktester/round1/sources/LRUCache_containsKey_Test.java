package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.cache.LockingLRUCacheStrategy;
import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LRUCache_containsKey_Test {

    private LRUCache<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // The cache will be created in each test (or in the parameterized “given” phase)
        cache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the cache to help GC; not strictly required
        if (cache != null) {
            cache.clear();
        }
    }

    /* --------------------------------------------------------------------- *
     *  Parameter source – both supported strategies                         *
     * --------------------------------------------------------------------- */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Replaces the private final {@code strategy} field of {@link LRUCache}
     * with the supplied map. Used to inject mocks or alternative implementations.
     */
    private static void injectStrategy(LRUCache<?, ?> target, Map<?, ?> strategy) throws Exception {
        Field field = LRUCache.class.getDeclaredField("strategy");
        field.setAccessible(true);
        field.set(target, strategy);
    }

    /**
     * Sets the internal {@code strategy} field to a specific value (including {@code null}).
     */
    private static void setStrategyField(LRUCache<?, ?> target, Map<?, ?> strategy) throws Exception {
        injectStrategy(target, strategy);
    }

    /* --------------------------------------------------------------------- *
     *  Positive case – key is present                                         *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_returnsTrueWhenKeyExists(LRUCache.StrategyType strategy) {
        // given
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        // then
        assertTrue(cache.containsKey(1), "Cache should contain key 1");
        assertTrue(cache.containsKey(2), "Cache should contain key 2");
    }

    /* --------------------------------------------------------------------- *
     *  Negative case – key is absent                                          *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_returnsFalseWhenKeyMissing(LRUCache.StrategyType strategy) {
        // given
        cache = new LRUCache<>(5, strategy);
        cache.put(10, "ten");
        // then
        assertFalse(cache.containsKey(99), "Cache should not contain key 99");
    }

    /* --------------------------------------------------------------------- *
     *  Null‑key handling                                                       *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_handlesNullKey(LRUCache.StrategyType strategy) {
        // given
        cache = new LRUCache<>(5, strategy);
        cache.put(null, "nullValue");
        // then
        assertTrue(cache.containsKey(null), "Cache should report presence of a null key");
        assertEquals("nullValue", cache.get(null), "Value associated with null key should be retrievable");
    }

    /* --------------------------------------------------------------------- *
     *  Eviction – after capacity is exceeded, evicted key should be absent    *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_reflectsEvictionAfterCapacityExceeded(LRUCache.StrategyType strategy) {
        // given – capacity 3
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        // Access key 1 to make it recently used
        cache.get(1);
        // Insert fourth entry, causing eviction of the least‑recently‑used key (2)
        cache.put(4, "D");
        // Wait briefly for threaded strategy to perform cleanup (if needed)
        if (strategy == LRUCache.StrategyType.THREADED) {
            try {
                // small pause; the cleanup thread runs quickly
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
        // then
        assertFalse(cache.containsKey(2), "Key 2 should have been evicted");
        assertTrue(cache.containsKey(1), "Key 1 should still be present");
        assertTrue(cache.containsKey(4), "Key 4 should be present");
    }

    /* --------------------------------------------------------------------- *
     *  Delegation & edge‑case tests (mocked strategy)                         *
     * --------------------------------------------------------------------- */
    @Test
    void containsKey_delegatesToStrategy_true() {
        // Mock a Map that returns true for key "alpha"
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.containsKey("alpha")).thenReturn(true);
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        injectStrategy(cache, mockMap);
        assertTrue(cache.containsKey("alpha"));
        verify(mockMap, times(1)).containsKey("alpha");
    }

    @Test
    void containsKey_delegatesToStrategy_false() {
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.containsKey("beta")).thenReturn(false);
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        injectStrategy(cache, mockMap);
        assertFalse(cache.containsKey("beta"));
        verify(mockMap, times(1)).containsKey("beta");
    }

    @Test
    void containsKey_delegatesToStrategy_nullKey_present() {
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.containsKey(null)).thenReturn(true);
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        injectStrategy(cache, mockMap);
        assertTrue(cache.containsKey(null));
        verify(mockMap, times(1)).containsKey(null);
    }

    @Test
    void containsKey_delegatesToStrategy_nullKey_absent() {
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.containsKey(null)).thenReturn(false);
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        injectStrategy(cache, mockMap);
        assertFalse(cache.containsKey(null));
        verify(mockMap, times(1)).containsKey(null);
    }

    @Test
    void containsKey_propagatesRuntimeException() {
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.containsKey("gamma")).thenThrow(new RuntimeException("boom"));
        cache = new LRUCache<>(10, LRUCache.StrategyType.LOCKING);
        injectStrategy(cache, mockMap);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.containsKey("gamma"));
        assertEquals("boom", ex.getMessage());
    }

    /* --------------------------------------------------------------------- *
     *  State‑transition tests (real cache, no mocking)                        *
     * --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_stateTransition_sizeUnchangedAfterTrue(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "delta");
        int sizeAfterPut = cache.size();
        assertTrue(cache.containsKey(1), "Key \"delta\" must be reported as present");
        assertEquals(sizeAfterPut, cache.size(), "Cache size must stay unchanged after containsKey returns true");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_stateTransition_sizeUnchangedAfterFalse(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(2, "epsilon");
        cache.remove(2);
        int sizeAfterRemoval = cache.size();
        assertFalse(cache.containsKey(2), "Key \"epsilon\" must be reported as absent");
        assertEquals(sizeAfterRemoval, cache.size(), "Cache size must stay unchanged after containsKey returns false");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void containsKey_stateTransition_afterClear(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(3, "zeta");
        cache.put(4, "eta");
        cache.clear();
        assertFalse(cache.containsKey(3), "After clear, any key must be absent");
        assertEquals(0, cache.size(), "Cache size must be zero after clear");
    }

    @Test
    void containsKey_typeMismatch_returnsFalseAndSizeUnchanged() {
        // Cache typed with String keys
        LRUCache<String, Integer> stringCache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        stringCache.put("a", 1);
        int sizeBefore = stringCache.size();
        // Query with an Integer key – should safely return false
        assertFalse(stringCache.containsKey(123));
        assertEquals(sizeBefore, stringCache.size(), "Size must remain unchanged after type‑mismatched containsKey");
    }

    @Test
    void containsKey_withLockingStrategy_delegatesCorrectly() {
        cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        cache.put(7, "theta");
        assertTrue(cache.containsKey(7), "Locking strategy must report existing key");
    }

    /* --------------------------------------------------------------------- *
     *  Exception‑scenario tests                                               *
     * --------------------------------------------------------------------- */
    @Test
    void containsKey_nullStrategy_throwsNullPointerException() throws Exception {
        cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        // Force the internal strategy map to null via reflection
        setStrategyField(cache, null);
        assertThrows(NullPointerException.class, () -> cache.containsKey("anyKey"), "Calling containsKey with a null internal strategy must NPE");
    }

    @Test
    void containsKey_nullKeyNotAllowed_throwsNullPointerException() throws Exception {
        // Use a ConcurrentHashMap (does not allow null keys)
        Map<String, String> concurrentMap = new ConcurrentHashMap<>();
        cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        injectStrategy(cache, concurrentMap);
        assertThrows(NullPointerException.class, () -> cache.containsKey(null), "ConcurrentHashMap should throw NPE for null key");
    }

    @Test
    void containsKey_classCastExceptionPropagates() throws Exception {
        // TreeMap expects Comparable keys; we insert a String key then query with Integer
        TreeMap<String, String> treeMap = new TreeMap<>();
        treeMap.put("key", "value");
        cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        injectStrategy(cache, treeMap);
        assertThrows(ClassCastException.class, () -> cache.containsKey(123), "TreeMap should throw ClassCastException for mismatched key type");
    }

    @Test
    void containsKey_customMapThrowsIllegalStateException() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, String> mockMap = mock(Map.class);
        when(mockMap.containsKey("trigger")).thenThrow(new IllegalStateException("illegal state"));
        cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        injectStrategy(cache, mockMap);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cache.containsKey("trigger"));
        assertEquals("illegal state", ex.getMessage());
    }

    @Test
    void containsKey_securityProxyThrowsSecurityException() throws Exception {
        // Create a simple proxy that throws SecurityException on containsKey
        Map<String, String> proxy = (Map<String, String>) java.lang.reflect.Proxy.newProxyInstance(Map.class.getClassLoader(), new Class[] { Map.class }, (proxyObj, method, args) -> {
            if ("containsKey".equals(method.getName())) {
                throw new SecurityException("access denied");
            }
            // delegate other methods to a real map to keep the proxy functional
            return new HashMap<String, String>().getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(new HashMap<>(), args);
        });
        cache = new LRUCache<>(5, LRUCache.StrategyType.LOCKING);
        injectStrategy(cache, proxy);
        SecurityException ex = assertThrows(SecurityException.class, () -> cache.containsKey("security"));
        assertEquals("access denied", ex.getMessage());
    }
}
