package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_putAll_Test {

    /**
     * Cache instance used in the tests.
     */
    private ThreadedLRUCacheStrategy<Integer, String> cache;

    /**
     * A map that will be passed to {@code putAll}.
     */
    private Map<Integer, String> sourceMap;

    /**
     * Initialise a fresh cache and source map before each test.
     * The cleanup delay is set to a large value (10 seconds) so that the
     * background purge task does not interfere with the deterministic
     * behaviour we are testing.
     */
    @BeforeEach
    void setupBeforeEach() {
        cache = new ThreadedLRUCacheStrategy<>(10, 10_000);
        sourceMap = new HashMap<>();
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        cache.clear();
    }

    private static Stream<Arguments> normalMapsProvider() {
        return Stream.of(Arguments.of(Map.of("key1", "value1", "key2", "42", "key3", "true"), 3, Map.of("key1", "value1", "key2", "42", "key3", "true")), Arguments.of(Collections.emptyMap(), 0, Collections.emptyMap()));
    }

    @Test
    void putAllAddsAllEntries() {
        // given
        sourceMap.put(1, "one");
        sourceMap.put(2, "two");
        sourceMap.put(3, "three");
        // when
        cache.putAll(sourceMap);
        // then
        assertEquals(3, cache.size(), "Cache size should match number of inserted entries");
        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
    }

    @Test
    void putAllOverridesExistingKeys() {
        // given
        cache.put(1, "old");
        sourceMap.put(1, "new");
        sourceMap.put(2, "two");
        // when
        cache.putAll(sourceMap);
        // then
        assertEquals(2, cache.size(), "Cache should contain two distinct keys after override");
        assertEquals("new", cache.get(1), "Existing key should be overwritten by putAll");
        assertEquals("two", cache.get(2));
    }

    @Test
    void putAllAcceptsNullKeyAndNullValue() {
        // given
        sourceMap.put(null, "nullKey");
        sourceMap.put(5, null);
        // when
        cache.putAll(sourceMap);
        // then
        assertEquals(2, cache.size());
        assertEquals("nullKey", cache.get(null));
        assertNull(cache.get(5));
    }

    @Test
    void putAllWithEmptyMapLeavesCacheUnchanged() {
        // given
        cache.put(10, "ten");
        assertEquals(1, cache.size());
        // when
        // Java 9+ immutable empty map
        cache.putAll(Map.of());
        // then
        assertEquals(1, cache.size(), "Cache size should remain unchanged after putting an empty map");
        assertEquals("ten", cache.get(10));
    }

    @Test
    void putAllThrowsNPEWhenArgumentIsNull() {
        // given
        Map<Integer, String> nullMap = null;
        // when / then
        assertThrows(NullPointerException.class, () -> cache.putAll(nullMap));
    }

    @ParameterizedTest(name = "putAll with map of size {1}")
    @MethodSource("normalMapsProvider")
    void putAllParameterized(Map<String, String> map, int expectedSize, Map<String, String> expectedContent) {
        // when
        //noinspection unchecked
        cache.putAll((Map) map);
        // then
        assertEquals(expectedSize, cache.size());
        expectedContent.forEach((k, v) -> assertEquals(v, cache.get(k)));
    }

    @Test
    void putAllRespectsCapacityAndEvictsLeastRecentlyUsed() {
        // capacity = 3, cleanup delay large enough to avoid async eviction
        ThreadedLRUCacheStrategy<String, String> smallCache = new ThreadedLRUCacheStrategy<>(3, 10_000);
        Map<String, String> many = Map.of(":1", "A", ":2", "B", ":3", "C", ":4", "D", ":5", "E");
        // when
        smallCache.putAll(many);
        // then
        assertEquals(3, smallCache.size(), "Cache should contain exactly the capacity number of entries");
        assertNull(smallCache.get(":1"));
        assertNull(smallCache.get(":2"));
        assertEquals("C", smallCache.get(":3"));
        assertEquals("D", smallCache.get(":4"));
        assertEquals("E", smallCache.get(":5"));
    }

    @Test
    void putAllUpdatesExistingKeys() {
        // given
        cache.put(100, "oldValue");
        Map<Integer, String> update = Map.of(100, "newValue", 200, "otherValue");
        // when
        cache.putAll(update);
        // then
        assertEquals(2, cache.size());
        assertEquals("newValue", cache.get(100));
        assertEquals("otherValue", cache.get(200));
    }

    @Test
    void putAllDoesNotHoldReferenceToSourceMap() {
        // given
        Map<String, Integer> src = new HashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        // when
        //noinspection unchecked
        cache.putAll((Map) src);
        // mutate source after putAll
        src.remove("a");
        // then
        assertEquals(2, cache.size());
        assertEquals(1, cache.get("a"));
        assertEquals(2, cache.get("b"));
    }

    @Test
    void putAllWorksWithUnmodifiableMap() {
        // given
        Map<String, Integer> modifiable = Map.of("x", 10, "y", 20);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(modifiable);
        // when
        //noinspection unchecked
        cache.putAll((Map) unmodifiable);
        // then
        assertEquals(2, cache.size());
        assertEquals(10, cache.get("x"));
        assertEquals(20, cache.get("y"));
    }

    @Test
    void putAllPropagatesConcurrentModificationException() {
        // given a map that will be modified while putAll iterates over it
        Map<String, String> mutable = new HashMap<>();
        for (int i = 0; i < 1_000; i++) {
            mutable.put("k" + i, "v" + i);
        }
        // Start a thread that clears the map after a tiny delay
        Thread modifier = new Thread(() -> {
            try {
                Thread.sleep(5);
                mutable.clear();
            } catch (InterruptedException ignored) {
            }
        });
        modifier.start();
        // when / then
        assertThrows(ConcurrentModificationException.class, () -> {
            //noinspection unchecked
            cache.putAll((Map) mutable);
        });
        // ensure modifier thread has finished
        try {
            modifier.join();
        } catch (InterruptedException ignored) {
        }
    }

    @Test
    void putAllHandlesLargeMapsEfficiently() {
        // given
        int size = 10_000;
        Map<Integer, Integer> bigMap = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            bigMap.put(i, i);
        }
        // when
        //noinspection unchecked
        cache.putAll((Map) bigMap);
        // then
        assertEquals(size, cache.size());
        // spot‑check a few entries
        assertEquals(0, cache.get(0));
        assertEquals(size / 2, cache.get(size / 2));
        assertEquals(size - 1, cache.get(size - 1));
    }

    @Test
    void putAllIsThreadSafeWhenOtherThreadsModifyCache() throws Exception {
        // given a cache with generous capacity
        ThreadedLRUCacheStrategy<Integer, Integer> concurrentCache = new ThreadedLRUCacheStrategy<>(1000, 10_000);
        Map<Integer, Integer> toAdd = new HashMap<>();
        for (int i = 0; i < 500; i++) {
            toAdd.put(i, i);
        }
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService exec = Executors.newFixedThreadPool(2);
        // Thread 1: performs random puts/removes while putAll is running
        exec.submit(() -> {
            try {
                startLatch.await();
                Random rnd = new Random();
                for (int i = 500; i < 800; i++) {
                    concurrentCache.put(i, i);
                    if (rnd.nextBoolean()) {
                        concurrentCache.remove(rnd.nextInt(800));
                    }
                }
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // Thread 2: executes putAll
        exec.submit(() -> {
            try {
                startLatch.await();
                //noinspection unchecked
                concurrentCache.putAll((Map) toAdd);
            } finally {
                doneLatch.countDown();
            }
        });
        // start both threads
        startLatch.countDown();
        // wait for completion
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Threads did not finish in time");
        exec.shutdownNow();
        // then: all entries from toAdd must be present (they may have been evicted only if capacity were exceeded,
        // which it is not)
        for (Map.Entry<Integer, Integer> e : toAdd.entrySet()) {
            assertEquals(e.getValue(), concurrentCache.get(e.getKey()), "Entry from putAll missing after concurrent activity");
        }
    }

    @Test
    void putAllPropagatesExceptionThrownByPut() {
        // Create a spy so we can force put() to throw for a specific key
        ThreadedLRUCacheStrategy<String, String> spyCache = Mockito.spy(new ThreadedLRUCacheStrategy<>(10, 10_000));
        doThrow(new IllegalArgumentException("bad key")).when(spyCache).put(eq("badKey"), anyString());
        Map<String, String> badMap = Map.of("badKey", "someValue", "goodKey", "goodValue");
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> spyCache.putAll(badMap));
        assertEquals("bad key", ex.getMessage());
        // Verify that put was invoked for the bad key and that processing stopped afterwards
        verify(spyCache, times(1)).put(eq("badKey"), anyString());
        verify(spyCache, never()).put(eq("goodKey"), anyString());
    }
}
