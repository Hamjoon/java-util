package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_keySet_Test {

    /**
     * Cache instance used in each test.
     */
    private ThreadedLRUCacheStrategy<String, Integer> cache;

    /**
     * Capacity and cleanup delay used for the cache – values are arbitrary but valid.
     */
    private static final int CAPACITY = 10;

    private static final int CLEANUP_DELAY_MILLIS = 50;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for every test to avoid cross‑test interference.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so the scheduler does not retain references.
        if (cache != null) {
            cache.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
        ThreadedLRUCacheStrategy.shutdown();
    }

    private static Stream<Arguments> basicKeySetsProvider() {
        return Stream.of(Arguments.of(Collections.emptyList(), Collections.emptySet()), Arguments.of(List.of("alpha"), Set.of("alpha")), Arguments.of(List.of("alpha", "beta", "gamma"), Set.of("alpha", "beta", "gamma")));
    }

    @Test
    void keySetReturnsAllKeysPresentInCache() {
        // given – populate the cache with a known set of entries
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        // when – retrieve the key set
        Set<String> keys = cache.keySet();
        // then – the set should contain exactly the inserted keys
        assertEquals(3, keys.size(), "Key set size should match number of inserted entries");
        assertTrue(keys.containsAll(Set.of("one", "two", "three")), "Key set should contain all keys that were put into the cache");
        // the returned set must be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> keys.add("four"));
    }

    @Test
    void keySetOnEmptyCacheReturnsEmptyUnmodifiableSet() {
        // given – an empty cache (no puts)
        // when
        Set<String> keys = cache.keySet();
        // then
        assertTrue(keys.isEmpty(), "Key set of an empty cache should be empty");
        assertSame(Collections.emptySet(), Collections.unmodifiableSet(keys), "The returned set should be an unmodifiable view");
        assertThrows(UnsupportedOperationException.class, () -> keys.remove("any"));
    }

    @Test
    void keySetReflectsSubsequentModificationsViaCacheMethods() {
        // given – put a single entry
        cache.put("alpha", 100);
        Set<String> keys = cache.keySet();
        // when – add another entry directly to the cache
        cache.put("beta", 200);
        // then – the previously obtained set should NOT reflect the new entry because
        // keySet() creates a snapshot each call. Verify by checking size.
        assertEquals(1, keys.size(), "Previously obtained key set should remain unchanged");
        assertTrue(keys.contains("alpha"));
        assertFalse(keys.contains("beta"));
        // a fresh call should see both keys
        Set<String> refreshed = cache.keySet();
        assertEquals(2, refreshed.size());
        assertTrue(refreshed.containsAll(Set.of("alpha", "beta")));
    }

    @Test
    void keySetReflectsSnapshotSemantics() {
        // given – put a single entry and obtain a snapshot
        cache.put("alpha", 100);
        Set<String> snapshot = cache.keySet();
        // when – modify the cache after the snapshot was taken
        cache.put("beta", 200);
        cache.remove("alpha");
        // then – snapshot must stay unchanged
        assertEquals(1, snapshot.size(), "Snapshot size must remain unchanged");
        assertTrue(snapshot.contains("alpha"));
        assertFalse(snapshot.contains("beta"));
        // a fresh call must see the current state
        Set<String> refreshed = cache.keySet();
        assertEquals(1, refreshed.size());
        assertTrue(refreshed.contains("beta"));
    }

    @Test
    void keySetDoesNotAllowAddRemoveClearOperations() {
        cache.put("k1", 1);
        Set<String> keys = cache.keySet();
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> keys.add("new")), () -> assertThrows(UnsupportedOperationException.class, () -> keys.remove("k1")), () -> assertThrows(UnsupportedOperationException.class, keys::clear));
    }

    @Test
    void iteratorRemoveOnReturnedSetThrowsUnsupportedOperationException() {
        cache.put("a", 1);
        cache.put("b", 2);
        Set<String> keys = cache.keySet();
        Iterator<String> it = keys.iterator();
        assertTrue(it.hasNext());
        // move to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void keySetIgnoresNullKeyInsertedViaPut() {
        // According to the API the cache allows null keys, but the
        // ConcurrentSet used inside keySet does not. The expected behaviour
        // is that the null key is simply omitted from the returned set.
        cache.put(null, 42);
        cache.put("valid", 1);
        Set<String> keys = cache.keySet();
        assertEquals(1, keys.size(), "Null key should be ignored");
        assertTrue(keys.contains("valid"));
        assertFalse(keys.contains(null));
    }

    @Test
    void keySetThrowsNullPointerExceptionWhenCacheContainsNullNode() throws Exception {
        // Simulate a corrupt internal map entry where the value (Node) is null.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Integer>> internalMap = (ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Integer>>) cacheField.get(cache);
        // insert a null Node
        internalMap.put("bad", null);
        // The for‑each loop should encounter the null and throw NPE.
        assertThrows(NullPointerException.class, cache::keySet);
    }

    @Test
    void keySetThrowsNullPointerExceptionWhenNodeKeyIsNull() throws Exception {
        // Insert a normal entry, then corrupt its key via reflection.
        cache.put("temp", 0);
        // Access the internal map and the Node instance.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Integer>> internalMap = (ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Integer>>) cacheField.get(cache);
        ThreadedLRUCacheStrategy.Node<String, Integer> node = internalMap.get("temp");
        // Set the key field of the node to null.
        Field keyField = ThreadedLRUCacheStrategy.Node.class.getDeclaredField("key");
        keyField.setAccessible(true);
        keyField.set(node, null);
        // Now keySet should attempt to add a null to ConcurrentSet and throw NPE.
        assertThrows(NullPointerException.class, cache::keySet);
    }

    @Test
    void keySetContainsAllKeysAfterConcurrentPuts() throws Exception {
        int threadCount = 5;
        int keysPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // wait for the go‑signal
                    startLatch.await();
                    for (int j = 0; j < keysPerThread; j++) {
                        String key = "t" + threadId + "-k" + j;
                        cache.put(key, j);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Release all threads simultaneously
        startLatch.countDown();
        // Wait for all puts to finish
        doneLatch.await();
        executor.shutdownNow();
        Set<String> keys = cache.keySet();
        assertEquals(threadCount * keysPerThread, keys.size(), "keySet must contain every key inserted concurrently");
    }

    @Test
    void keySetReflectsSnapshotWhenConcurrentRemovalsOccur() throws Exception {
        // Populate cache with a known set of keys
        List<String> allKeys = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            String k = "key-" + i;
            cache.put(k, i);
            allKeys.add(k);
        }
        // Obtain a snapshot before removal threads start
        Set<String> snapshot = cache.keySet();
        // Start concurrent removals
        ExecutorService executor = Executors.newFixedThreadPool(4);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(4);
        for (int t = 0; t < 4; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int i = threadId; i < allKeys.size(); i += 4) {
                        cache.remove(allKeys.get(i));
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        startLatch.countDown();
        doneLatch.await();
        executor.shutdownNow();
        // Snapshot must stay unchanged despite removals
        assertEquals(50, snapshot.size(), "Snapshot taken before removals must retain original size");
        // Fresh call reflects the removals
        Set<String> refreshed = cache.keySet();
        assertTrue(refreshed.size() < 50, "After removals the fresh key set should be smaller");
    }

    @Test
    void keySetThrowsIllegalStateExceptionWhenInternalMapIsNull() throws Exception {
        // Force the private 'cache' field to null.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        // The method should detect the illegal state and throw IllegalStateException.
        assertThrows(IllegalStateException.class, cache::keySet);
    }

    @ParameterizedTest(name = "{index} => keys={0}")
    @MethodSource("basicKeySetsProvider")
    void keySetBasicScenarios(List<String> keysToInsert, Set<String> expected) {
        // given
        for (String k : keysToInsert) {
            cache.put(k, k.length());
        }
        // when
        Set<String> actual = cache.keySet();
        // then
        assertEquals(expected, actual);
    }
}
