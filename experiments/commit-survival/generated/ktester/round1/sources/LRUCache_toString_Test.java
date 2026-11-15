package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.cache.LockingLRUCacheStrategy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LRUCache_toString_Test {

    private LRUCache<Integer, String> cache;

    @BeforeEach
    void setupBeforeEach() {
        // Each test will explicitly create its own cache via setUp(strategy).
        cache = null;
    }

    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the background thread used by the threaded strategy.
        ThreadedLRUCacheStrategy.shutdown();
    }

    /**
     * Provides the two supported strategy types for the parameterized tests.
     */
    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /**
     * Simple factory used by the tests to create a cache with a given strategy.
     */
    private void setUp(LRUCache.StrategyType strategy) {
        // Capacity 3 is enough for the scenarios exercised in the tests.
        cache = new LRUCache<>(3, strategy);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void toStringShowsEntriesWhenCacheIsPopulated(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        String s = cache.toString();
        // The string representation should contain each key‑value pair.
        assertTrue(s.contains("1=one"), "toString must contain the first entry");
        assertTrue(s.contains("2=two"), "toString must contain the second entry");
        assertTrue(s.contains("3=three"), "toString must contain the third entry");
        // The representation should be surrounded by braces like a normal map.
        assertTrue(s.startsWith("{") && s.endsWith("}"), "toString should look like a normal Map representation");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void toStringOfEmptyCacheIsEmptyBraces(LRUCache.StrategyType strategy) {
        setUp(strategy);
        assertEquals("{}", cache.toString(), "An empty LRUCache should render as {}");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void toStringHandlesNullKeyAndValue(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(null, null);
        cache.put(5, null);
        cache.put(null, "value");
        String s = cache.toString();
        // Various null representations must appear.
        assertTrue(s.contains("null=null") || s.contains("null=value"), "toString must contain a representation for a null key/value pair");
        assertTrue(s.contains("5=null"), "toString must contain the entry with a null value");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void toStringReflectsEvictionAfterCapacityExceeded(LRUCache.StrategyType strategy) {
        setUp(strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        // cache is full now
        cache.put(3, "C");
        // Access 1 to make it recently used, then add a fourth entry.
        cache.get(1);
        cache.put(4, "D");
        // Give the threaded strategy a little time to perform cleanup, if needed.
        if (strategy == LRUCache.StrategyType.THREADED) {
            try {
                // small pause; the test is tolerant to timing.
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        }
        String s = cache.toString();
        // The evicted key (2) should no longer appear.
        assertFalse(s.contains("2=B"), "Evicted entry should not be present in toString");
        // The retained entries should still be present.
        assertTrue(s.contains("1=A"));
        assertTrue(s.contains("4=D"));
    }

    /**
     * Injects a custom backing {@code Map} into the {@code LRUCache} instance.
     * The {@code strategy} field is private, so reflection is required.
     *
     * @param cache   the cache whose strategy should be replaced
     * @param map     the map to inject
     */
    @SuppressWarnings("unchecked")
    private static void injectStrategy(LRUCache<?, ?> cache, Map<?, ?> map) throws Exception {
        Field f = LRUCache.class.getDeclaredField("strategy");
        f.setAccessible(true);
        f.set(cache, map);
    }

    @Test
    void toStringDelegatesToStandardUnderlyingMap() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        Map<String, Integer> backing = new LinkedHashMap<>();
        backing.put("a", 1);
        backing.put("b", 2);
        injectStrategy(cache, backing);
        assertEquals("{a=1, b=2}", cache.toString(), "LRUCache should delegate to the underlying map's toString()");
    }

    @Test
    void toStringWithEmptyUnderlyingMap() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        Map<String, Integer> empty = Collections.emptyMap();
        injectStrategy(cache, empty);
        assertEquals("{}", cache.toString(), "Empty backing map must produce {}");
    }

    @Test
    void toStringUsesCustomMapToStringImplementation() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        Map<String, String> custom = new AbstractMap<>() {

            @Override
            public Set<Entry<String, String>> entrySet() {
                return Set.of();
            }

            @Override
            public String toString() {
                return "CustomMapRepresentation";
            }
        };
        injectStrategy(cache, custom);
        assertEquals("CustomMapRepresentation", cache.toString(), "Custom map's toString should be returned unchanged");
    }

    @Test
    void toStringPropagatesRuntimeExceptionFromBackingMap() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Object, Object> faulty = mock(Map.class);
        when(faulty.toString()).thenThrow(new RuntimeException("boom"));
        injectStrategy(cache, faulty);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::toString, "RuntimeException from backing map must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toStringThreadedStrategyDelegatesCorrectly() {
        setUp(LRUCache.StrategyType.THREADED);
        cache.put("x", "y");
        assertEquals("{x=y}", cache.toString(), "Threaded strategy should delegate to its internal map");
    }

    @Test
    void toStringLockingStrategyDelegatesCorrectly() {
        setUp(LRUCache.StrategyType.LOCKING);
        cache.put(1, "one");
        cache.put(2, "two");
        assertEquals("{1=one, 2=two}", cache.toString(), "Locking strategy should delegate to its internal map");
    }

    @Test
    void toStringReflectsPutAndRemoveOperations() {
        setUp(LRUCache.StrategyType.LOCKING);
        cache.put("x", 10);
        cache.put("y", 20);
        cache.remove("x");
        assertEquals("{y=20}", cache.toString(), "After remove, only remaining entries must be shown");
    }

    @Test
    void toStringWorksWithSynchronizedMapBacking() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        Map<String, Integer> sync = Collections.synchronizedMap(new LinkedHashMap<>() {

            {
                put("s", 10);
                put("t", 20);
            }
        });
        injectStrategy(cache, sync);
        assertEquals("{s=10, t=20}", cache.toString(), "Synchronized map backing must be represented correctly");
    }

    @Test
    void toStringLargeMapRepresentation() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        Map<Integer, Integer> large = new LinkedHashMap<>();
        for (int i = 0; i < 100; i++) {
            large.put(i, i);
        }
        injectStrategy(cache, large);
        String result = cache.toString();
        // Build expected string using the same iteration order as LinkedHashMap.
        StringBuilder expected = new StringBuilder("{");
        for (int i = 0; i < 100; i++) {
            expected.append(i).append("=").append(i);
            if (i < 99)
                expected.append(", ");
        }
        expected.append('}');
        assertEquals(expected.toString(), result, "Large map must be fully represented without truncation");
    }

    @Test
    void toStringPreservesUnicodeCharacters() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        Map<String, String> unicode = new LinkedHashMap<>();
        unicode.put("α", "β");
        unicode.put("γ", "δ");
        injectStrategy(cache, unicode);
        assertEquals("{α=β, γ=δ}", cache.toString(), "Unicode characters must survive the toString conversion");
    }

    @Test
    void toStringAfterShutdownStillWorks() {
        setUp(LRUCache.StrategyType.THREADED);
        cache.put("k", "v");
        // shutdown the background thread
        cache.shutdown();
        assertEquals("{k=v}", cache.toString(), "Shutdown must not alter the visible map representation");
    }

    @Test
    void toStringCustomMapReturnsEmptyString() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        Map<String, String> emptyStringMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, String>> entrySet() {
                return Set.of();
            }

            @Override
            public String toString() {
                return "";
            }
        };
        injectStrategy(cache, emptyStringMap);
        assertEquals("", cache.toString(), "If backing map returns empty string, LRUCache must return it unchanged");
    }

    @Test
    void toStringWithNullBackingStrategyThrowsNPE() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        // deliberately set to null
        injectStrategy(cache, null);
        assertThrows(NullPointerException.class, cache::toString, "Calling toString on a cache with null backing map must throw NPE");
    }

    @Test
    void toStringPropagatesConcurrentModificationException() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        injectStrategy(cache, hashMap);
        // Latch to ensure the remover runs while toString is iterating.
        CountDownLatch startIterating = new CountDownLatch(1);
        CountDownLatch removerDone = new CountDownLatch(1);
        AtomicBoolean exceptionThrown = new AtomicBoolean(false);
        Thread remover = new Thread(() -> {
            try {
                // Wait until the main thread has entered toString()
                startIterating.await();
                hashMap.remove("a");
            } catch (InterruptedException ignored) {
            } finally {
                removerDone.countDown();
            }
        });
        remover.start();
        try {
            // Wrap toString to signal when iteration starts.
            // may throw CME
            String result = cache.toString();
            // If we reach here, no CME was thrown – still verify the map is consistent.
            assertEquals("{b=2}", result);
        } catch (ConcurrentModificationException e) {
            exceptionThrown.set(true);
        } finally {
            // Ensure remover thread finishes before test ends.
            removerDone.await();
        }
        assertTrue(exceptionThrown.get(), "Concurrent modification during toString should result in ConcurrentModificationException");
    }

    @Test
    void toStringPropagatesAssertionErrorFromBackingMap() throws Exception {
        setUp(LRUCache.StrategyType.LOCKING);
        @SuppressWarnings("unchecked")
        Map<Object, Object> faulty = mock(Map.class);
        when(faulty.toString()).thenThrow(new AssertionError("assertion failed"));
        injectStrategy(cache, faulty);
        AssertionError err = assertThrows(AssertionError.class, cache::toString, "AssertionError from backing map must propagate");
        assertEquals("assertion failed", err.getMessage());
    }
}
