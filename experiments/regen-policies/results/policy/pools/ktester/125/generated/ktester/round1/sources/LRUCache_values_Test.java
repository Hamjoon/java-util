package com.cedarsoftware.util;

import com.cedarsoftware.util.cache.ThreadedLRUCacheStrategy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class LRUCache_values_Test {

    private LRUCache<Integer, String> cache;

    @AfterEach
    void teardownAfterEach() {
        if (cache != null) {
            // graceful shutdown for both strategies
            cache.shutdown();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // The threaded strategy creates a shared scheduler that must be shut down.
        ThreadedLRUCacheStrategy.shutdown();
    }

    static Collection<LRUCache.StrategyType> strategies() {
        return Arrays.asList(LRUCache.StrategyType.LOCKING, LRUCache.StrategyType.THREADED);
    }

    /* --------------------------------------------------------------------- */
    private void injectStrategy(Map<Integer, String> customStrategy) {
        try {
            Field f = LRUCache.class.getDeclaredField("strategy");
            f.setAccessible(true);
            f.set(cache, customStrategy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void valuesReflectsAllEntries(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        Collection<String> vals = cache.values();
        // basic size & content checks
        assertEquals(3, vals.size(), "values() size must match number of entries");
        assertTrue(vals.containsAll(Arrays.asList("one", "two", "three")));
        // adding a new entry must be reflected instantly
        cache.put(4, "four");
        assertEquals(4, vals.size());
        assertTrue(vals.contains("four"));
        // removing via map must be reflected in the collection
        cache.remove(2);
        assertEquals(3, vals.size());
        assertFalse(vals.contains("two"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void valuesIteratorRemoveUpdatesCache(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(10, "ten");
        cache.put(20, "twenty");
        cache.put(30, "thirty");
        Collection<String> vals = cache.values();
        Iterator<String> it = vals.iterator();
        // remove the first element encountered
        assertTrue(it.hasNext());
        String removed = it.next();
        it.remove();
        // the underlying cache must no longer contain the removed entry
        assertNull(cache.values().stream().filter(v -> v.equals(removed)).findAny().orElse(null));
        assertEquals(2, cache.size());
        // ensure the remaining values are still present
        assertTrue(cache.containsValue("ten") ^ cache.containsValue("twenty") ^ cache.containsValue("thirty"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void valuesReflectsEviction(LRUCache.StrategyType strategy) {
        // capacity = 3, we will force an eviction
        cache = new LRUCache<>(3, strategy);
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        // Access 1 to make it recently used, then add 4 to trigger eviction
        cache.get(1);
        cache.put(4, "D");
        // Wait a short time for the threaded strategy to clean up if needed
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline && cache.containsKey(2) && cache.size() > 3) {
            // busy‑wait – the test will still succeed if eviction happens instantly
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        }
        Collection<String> vals = cache.values();
        // key 2 should have been evicted; its value must not appear in the collection
        assertFalse(vals.contains("B"));
        // values for the remaining keys must be present
        assertTrue(vals.containsAll(Arrays.asList("A", "D")));
        // size must be 3 (or less if eviction already happened)
        assertTrue(vals.size() <= 3);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_emptyCache(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        Collection<String> vals = cache.values();
        assertNotNull(vals, "values() must never return null");
        assertEquals(0, vals.size(), "Empty cache must yield empty collection");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_multipleEntries(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        Collection<String> vals = cache.values();
        assertEquals(3, vals.size());
        // Order is unspecified – we only require presence of each element
        assertTrue(vals.containsAll(Arrays.asList("one", "two", "three")));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_immutableValuesCollection(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        // replace the internal strategy with an unmodifiable map
        Map<Integer, String> unmod = Collections.unmodifiableMap(new HashMap<>());
        injectStrategy(unmod);
        // populate via reflection (since put() would try to modify the unmodifiable map)
        // this will actually throw, so we fill before wrapping
        unmod.put(1, "one");
        // Instead, create a mutable map, fill it, then wrap:
        Map<Integer, String> mutable = new HashMap<>();
        mutable.put(1, "one");
        mutable.put(2, "two");
        injectStrategy(Collections.unmodifiableMap(mutable));
        Collection<String> vals = cache.values();
        Iterator<String> it = vals.iterator();
        assertTrue(it.hasNext());
        it.next();
        // The iterator's remove must propagate UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, it::remove, "Removing from an immutable values view must throw");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_underlyingExceptionPropagation(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        // custom map whose values() throws RuntimeException
        Map<Integer, String> throwingMap = new HashMap<Integer, String>() {

            @Override
            public Collection<String> values() {
                throw new RuntimeException("boom");
            }
        };
        injectStrategy(throwingMap);
        RuntimeException ex = assertThrows(RuntimeException.class, cache::values, "Exception from underlying map must be re‑thrown");
        assertEquals("boom", ex.getMessage());
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_modificationReflectsInCache(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "alpha");
        cache.put(2, "beta");
        Collection<String> vals = cache.values();
        assertTrue(vals.remove("alpha"), "Removal via collection should succeed");
        assertEquals(1, cache.size());
        assertFalse(cache.containsKey(1));
        assertFalse(cache.containsValue("alpha"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_reflectsPutAfterRetrieval(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "x");
        cache.put(2, "y");
        Collection<String> vals = cache.values();
        int before = vals.size();
        cache.put(3, "z");
        assertEquals(before + 1, vals.size(), "Live view must reflect new entry");
        assertTrue(vals.contains("z"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_reflectsRemoveViaCache(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c");
        Collection<String> vals = cache.values();
        cache.remove(2);
        assertEquals(2, vals.size());
        assertFalse(vals.contains("b"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_containsNullValues(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, null);
        cache.put(2, "notnull");
        Collection<String> vals = cache.values();
        assertEquals(2, vals.size());
        assertTrue(vals.contains(null), "Collection must contain the stored null value");
        assertTrue(vals.contains("notnull"));
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_concurrentModificationDuringIteration(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c");
        Iterator<String> it = cache.values().iterator();
        // structural change before next()
        cache.put(4, "d");
        // The iterator should detect the change and throw CME on next()
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must fail fast on concurrent modification");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_threadedStrategyConcurrentAccess(LRUCache.StrategyType strategy) throws Exception {
        // This test is only meaningful for the threaded strategy but we run both for completeness
        cache = new LRUCache<>(10, strategy);
        for (int i = 0; i < 10; i++) {
            cache.put(i, "v" + i);
        }
        int threadCount = 8;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Collection<String>>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(exec.submit(() -> {
                // synchronize start
                start.await();
                // each thread obtains its own view
                return cache.values();
            }));
        }
        // let all threads run
        start.countDown();
        for (Future<Collection<String>> f : futures) {
            Collection<String> vals = f.get(2, TimeUnit.SECONDS);
            assertEquals(10, vals.size(), "Each thread must see a consistent snapshot");
        }
        exec.shutdownNow();
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_afterShutdownReturnsEmpty(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "x");
        cache.put(2, "y");
        cache.shutdown();
        Collection<String> vals = cache.values();
        assertNotNull(vals);
        assertEquals(0, vals.size(), "After shutdown the cache should behave as empty");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_addUnsupported(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        Map<Integer, String> mutable = new HashMap<>();
        mutable.put(1, "one");
        injectStrategy(Collections.unmodifiableMap(mutable));
        Collection<String> vals = cache.values();
        assertThrows(UnsupportedOperationException.class, () -> vals.add("new"), "Adding to an immutable values collection must throw");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_iteratorRemoveUnsupported(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        Map<Integer, String> mutable = new HashMap<>();
        mutable.put(1, "one");
        injectStrategy(Collections.unmodifiableMap(mutable));
        Iterator<String> it = cache.values().iterator();
        assertTrue(it.hasNext());
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove on immutable view must throw");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_nullStrategyAfterShutdown(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "a");
        cache.shutdown();
        // The implementation may null‑ify the strategy; any call should now fail
        assertThrows(RuntimeException.class, cache::values, "Calling values() after shutdown should throw an exception");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_nullReturnedCollection(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        // custom map whose values() returns null
        Map<Integer, String> nullMap = new HashMap<Integer, String>() {

            @Override
            public Collection<String> values() {
                return null;
            }
        };
        injectStrategy(nullMap);
        Collection<String> vals = cache.values();
        assertNull(vals, "Our custom strategy returns null");
        // Any operation on the null reference must throw NPE
        assertThrows(NullPointerException.class, () -> vals.size(), "Calling a method on a null collection must throw NPE");
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void values_concurrentModificationDuringIterationAfterClear(LRUCache.StrategyType strategy) {
        cache = new LRUCache<>(5, strategy);
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c");
        Iterator<String> it = cache.values().iterator();
        // Clear the cache before using the iterator
        cache.clear();
        // The iterator must detect the structural change
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must fail fast after underlying map is cleared");
    }
}
