package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_keySet_Test {

    private static final int CAPACITY = 10;

    private LockingLRUCacheStrategy<Integer, String> cache;

    class BadHashKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("Bad hash!");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class BadHashKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("Bad hash!");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test case
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that a failed test does not affect the next one
        cache.clear();
    }

    private static Stream<Arguments> snapshotScenarioProvider() {
        return Stream.of(Arguments.of(List.of(List.of("alpha", "1"), List.of("beta", "2")), "remove \"alpha\""));
    }

    @Test
    void keySetReturnsAllInsertedKeys() {
        // given
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // when
        Set<Integer> keys = cache.keySet();
        // then
        assertEquals(3, keys.size(), "keySet should contain exactly three keys");
        assertTrue(keys.containsAll(Arrays.asList(1, 2, 3)), "keySet should contain the keys that were put into the cache");
        // Verify iteration order matches insertion order (LinkedHashMap preserves it)
        Iterator<Integer> it = keys.iterator();
        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertEquals(3, it.next());
    }

    @Test
    void keySetOnEmptyCacheReturnsEmptySet() {
        // given – an empty cache (created in @BeforeEach)
        // when
        Set<Integer> keys = cache.keySet();
        // then
        assertNotNull(keys, "keySet should never be null");
        assertTrue(keys.isEmpty(), "keySet of an empty cache must be empty");
    }

    @Test
    void modificationsToReturnedKeySetDoNotAffectCache() {
        // given
        cache.put(10, "ten");
        cache.put(20, "twenty");
        // when
        Set<Integer> keys = cache.keySet();
        // Attempt to remove a key from the returned set – this should only affect the temporary
        // LinkedHashMap created inside keySet(), not the cache itself.
        assertTrue(keys.remove(10), "Removal from the returned keySet should succeed");
        // then
        // The cache must still contain both entries.
        assertEquals(2, cache.size(), "Cache size must remain unchanged after mutating the returned keySet");
        assertTrue(cache.containsKey(10), "Cache must still contain key 10");
        assertTrue(cache.containsKey(20), "Cache must still contain key 20");
        // Adding to the returned set is not supported (keySet of a Map is not add‑able)
        assertThrows(UnsupportedOperationException.class, () -> keys.add(30), "Adding to the returned keySet should throw UnsupportedOperationException");
    }

    @Test
    void keySetHandlesNullKey() {
        // given
        cache.put(null, "nullValue");
        cache.put(5, "five");
        // when
        Set<Integer> keys = cache.keySet();
        // then
        assertEquals(2, keys.size(), "keySet should contain two keys (null and 5)");
        assertTrue(keys.contains(null), "keySet must contain the null key");
        assertTrue(keys.contains(5), "keySet must contain the key 5");
    }

    @Test
    void keySetSingleEntry() {
        // single entry scenario
        cache.put(100, "hundred");
        Set<Integer> keys = cache.keySet();
        assertEquals(Set.of(100), keys, "keySet should contain exactly the inserted key");
    }

    @Test
    void keySetMultipleEntriesPreserveInsertionOrder() {
        cache.put(1, "A");
        cache.put(2, "B");
        cache.put(3, "C");
        Set<Integer> keys = cache.keySet();
        List<Integer> expected = List.of(1, 2, 3);
        assertEquals(expected, new ArrayList<>(keys), "keySet iterator must follow insertion order");
    }

    @Test
    void keySetDuplicateKeyReplacement() {
        cache.put(42, "first");
        // replace value
        cache.put(42, "second");
        Set<Integer> keys = cache.keySet();
        assertEquals(Set.of(42), keys, "Duplicate key should appear only once in keySet");
    }

    @Test
    void keySetAfterRemoval() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        cache.remove(2);
        Set<Integer> keys = cache.keySet();
        assertEquals(List.of(1, 3), new ArrayList<>(keys), "Removed key must not appear in keySet");
    }

    @Test
    void keySetAfterClear() {
        cache.put(1, "one");
        cache.put(2, "two");
        cache.clear();
        Set<Integer> keys = cache.keySet();
        assertTrue(keys.isEmpty(), "keySet must be empty after clear");
    }

    @Test
    void keySetLockIsReleasedAfterExecution() throws Exception {
        // Acquire the lock via reflection before calling keySet to ensure it's free.
        Field lockField = LockingLRUCacheStrategy.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        Lock lock = (Lock) lockField.get(cache);
        // Ensure we can acquire the lock now.
        assertTrue(lock.tryLock(), "Pre‑condition: lock must be free before test");
        lock.unlock();
        // Call keySet – it should lock and unlock internally.
        cache.keySet();
        // After keySet returns, we must be able to acquire the lock again.
        assertTrue(lock.tryLock(), "Lock must be released after keySet execution");
        lock.unlock();
    }

    @Test
    void keySetConcurrentAccessSafety() throws Exception {
        // Background thread continuously puts entries.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch stopLatch = new CountDownLatch(1);
        Runnable writer = () -> {
            try {
                startLatch.await();
                int i = 0;
                while (!Thread.currentThread().isInterrupted()) {
                    cache.put(i, "v" + i);
                    i++;
                }
            } catch (InterruptedException ignored) {
            } finally {
                stopLatch.countDown();
            }
        };
        executor.submit(writer);
        // start writer
        startLatch.countDown();
        // Main thread calls keySet many times.
        for (int i = 0; i < 100; i++) {
            // should never throw CME
            Set<Integer> keys = cache.keySet();
            assertNotNull(keys);
            // No further assertions – we only care that no exception is thrown.
        }
        // Stop background thread.
        executor.shutdownNow();
        stopLatch.await(2, TimeUnit.SECONDS);
    }

    @Test
    void keySetNullHeadViaReflectionThrowsNPE() throws Exception {
        // Corrupt internal head reference.
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.keySet(), "When head is null, keySet() should propagate NullPointerException");
    }

    @Test
    void keySetHeadNextNullViaReflectionThrowsNPE() throws Exception {
        // Create a dummy head node whose next field is null.
        Class<?> nodeClass = Class.forName("com.cedarsoftware.util.cache.LockingLRUCacheStrategy$Node");
        Object dummyHead = nodeClass.getDeclaredConstructor().newInstance();
        // Ensure dummyHead.next == null (default) and tail is a valid sentinel.
        Field tailField = LockingLRUCacheStrategy.class.getDeclaredField("tail");
        tailField.setAccessible(true);
        // existing valid tail
        Object tail = tailField.get(cache);
        // Set the private 'head' field to dummyHead.
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(cache, dummyHead);
        // The loop will evaluate node = head.next (null) and then try to read node.key -> NPE.
        assertThrows(NullPointerException.class, () -> cache.keySet(), "When head.next is null, keySet() should propagate NullPointerException");
    }

    @Test
    void keySetRuntimeExceptionFromBadHashKey() {
        // Define a key whose hashCode throws an exception.
        class BadHashKey {

            @Override
            public int hashCode() {
                throw new IllegalStateException("Bad hash!");
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }
        }
        BadHashKey badKey = new BadHashKey();
        // Put the bad key – the cache itself stores it without invoking hashCode.
        cache.put(badKey, "value");
        // keySet builds a LinkedHashMap and calls map.put(badKey, value) which triggers hashCode().
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cache.keySet(), "keySet() must propagate IllegalStateException from key.hashCode()");
        assertEquals("Bad hash!", ex.getMessage());
    }

    @ParameterizedTest(name = "{index} => putOps={0}, modify={1}")
    @MethodSource("snapshotScenarioProvider")
    void keySetSnapshotSemantics(List<List<Object>> putOps, String modify) {
        // Populate cache
        for (List<Object> pair : putOps) {
            @SuppressWarnings("unchecked")
            K key = (K) pair.get(0);
            @SuppressWarnings("unchecked")
            V value = (V) pair.get(1);
            cache.put((Integer) key, (String) value);
        }
        Set<Integer> keys = cache.keySet();
        // Apply modification described by the test case
        if ("remove \"alpha\"".equals(modify)) {
            assertTrue(keys.remove("alpha".hashCode()), "Removal from snapshot set should succeed");
        }
        // Verify cache unchanged
        assertEquals(putOps.size(), cache.size(), "Cache size must stay unchanged after mutating the returned keySet");
    }
}
