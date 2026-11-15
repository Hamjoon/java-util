package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_entrySet_Test {

    /**
     * Cache instance used in each test.
     */
    private LockingLRUCacheStrategy<String, Integer> cache;

    /**
     * A key whose {@code hashCode()} throws an {@link IllegalStateException}.
     * Used to force an exception while {@link LinkedHashMap#put(Object, Object)} is building the entry set.
     */
    private static final class BadKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("bad hash");
        }

        @Override
        public boolean equals(Object obj) {
            return false;
        }

        @Override
        public String toString() {
            return "BadKey";
        }
    }

    /**
     * Create a fresh cache before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // capacity is arbitrary; it only limits the number of entries before eviction.
        cache = new LockingLRUCacheStrategy<>(10);
    }

    /**
     * Clear the cache after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        cache.clear();
    }

    /**
     * Verify that {@code entrySet()} returns a set containing all current mappings.
     */
    @Test
    void entrySetContainsAllInsertedEntries() {
        // given
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        // when
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // then
        assertEquals(3, entrySet.size(), "entrySet should contain exactly three entries");
        // Convert to a map for easier assertions
        Map<String, Integer> asMap = entrySet.stream().collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(Integer.valueOf(1), asMap.get("one"));
        assertEquals(Integer.valueOf(2), asMap.get("two"));
        assertEquals(Integer.valueOf(3), asMap.get("three"));
    }

    /**
     * Ensure that the set returned is a snapshot – subsequent modifications do not affect the previously obtained set.
     */
    @Test
    void entrySetIsSnapshotOfCurrentState() {
        // given
        cache.put("alpha", 10);
        cache.put("beta", 20);
        Set<Map.Entry<String, Integer>> firstSnapshot = cache.entrySet();
        // when – modify the cache after obtaining the snapshot
        cache.put("gamma", 30);
        cache.remove("alpha");
        Set<Map.Entry<String, Integer>> secondSnapshot = cache.entrySet();
        // then – first snapshot remains unchanged
        assertEquals(2, firstSnapshot.size(), "First snapshot should still contain two entries");
        assertTrue(firstSnapshot.stream().anyMatch(e -> e.getKey().equals("alpha") && e.getValue().equals(10)));
        assertTrue(firstSnapshot.stream().anyMatch(e -> e.getKey().equals("beta") && e.getValue().equals(20)));
        // and – second snapshot reflects the new state
        assertEquals(2, secondSnapshot.size(), "Second snapshot should contain the updated two entries");
        assertTrue(secondSnapshot.stream().anyMatch(e -> e.getKey().equals("beta") && e.getValue().equals(20)));
        assertTrue(secondSnapshot.stream().anyMatch(e -> e.getKey().equals("gamma") && e.getValue().equals(30)));
    }

    /**
     * Verify that {@code entrySet()} works correctly when the cache is empty.
     */
    @Test
    void entrySetOnEmptyCacheReturnsEmptySet() {
        // given – an empty cache (setup already provides this)
        // when
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // then
        assertNotNull(entrySet, "entrySet should never be null");
        assertTrue(entrySet.isEmpty(), "entrySet should be empty for an empty cache");
    }

    /**
     * Confirm that {@code entrySet()} does not expose internal mutable structures.
     */
    @Test
    void entrySetModificationDoesNotAffectCache() {
        // given
        cache.put("key", 99);
        Set<Map.Entry<String, Integer>> entrySet = cache.entrySet();
        // when – attempt to modify the returned set (should throw UnsupportedOperationException)
        assertThrows(UnsupportedOperationException.class, () -> entrySet.clear(), "The set returned by entrySet() should be unmodifiable");
        // then – cache still contains the original entry
        assertEquals(1, cache.size());
        assertEquals(Integer.valueOf(99), cache.get("key"));
    }

    /**
     * After inserting two entries, entrySet() must preserve insertion order.
     */
    @Test
    void entrySetPreservesInsertionOrder() {
        cache.put("A", 1);
        cache.put("B", 2);
        List<String> keys = cache.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.toList());
        assertEquals(Arrays.asList("A", "B"), keys, "Insertion order should be preserved in the entry set");
    }

    /**
     * When a key is re‑inserted, its value is updated and it moves to the most‑recent position.
     */
    @Test
    void entrySetReflectsUpdateAndReorder() {
        cache.put("X", 1);
        cache.put("Y", 2);
        // update – should become most‑recent
        cache.put("X", 99);
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(cache.entrySet());
        // Expect order: Y then X (X moved to the tail)
        assertEquals("Y", entries.get(0).getKey());
        assertEquals(Integer.valueOf(2), entries.get(0).getValue());
        assertEquals("X", entries.get(1).getKey());
        assertEquals(Integer.valueOf(99), entries.get(1).getValue());
    }

    /**
     * Cache must support null keys and null values; entrySet() should contain them correctly.
     */
    @Test
    void entrySetHandlesNullKeyAndValue() {
        cache.put(null, 123);
        cache.put("Z", null);
        Map<String, Integer> map = cache.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertTrue(map.containsKey(null));
        assertEquals(Integer.valueOf(123), map.get(null));
        assertTrue(map.containsKey("Z"));
        assertNull(map.get("Z"));
    }

    @Test
    void entrySetIsThreadSafeUnderConcurrentModification() throws Exception {
        final int writerThreads = 2;
        final int readerThreads = 3;
        final int iterations = 500;
        ExecutorService exec = Executors.newFixedThreadPool(writerThreads + readerThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean stop = new AtomicBoolean(false);
        List<Future<?>> futures = new ArrayList<>();
        // Writers: continuously put and remove entries
        for (int i = 0; i < writerThreads; i++) {
            final int id = i;
            futures.add(exec.submit(() -> {
                try {
                    startLatch.await();
                    Random rnd = new Random();
                    while (!stop.get()) {
                        String key = "K" + rnd.nextInt(20);
                        cache.put(key, rnd.nextInt(1000));
                        if (rnd.nextBoolean()) {
                            cache.remove(key);
                        }
                    }
                } catch (InterruptedException ignored) {
                }
            }));
        }
        // Readers: repeatedly call entrySet()
        for (int i = 0; i < readerThreads; i++) {
            futures.add(exec.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < iterations; j++) {
                        Set<Map.Entry<String, Integer>> set = cache.entrySet();
                        // The set must be a consistent snapshot – no ConcurrentModificationException should surface
                        assertNotNull(set);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }
        // Start all threads
        startLatch.countDown();
        // Let them run for a short while
        Thread.sleep(200);
        stop.set(true);
        // Await termination
        for (Future<?> f : futures) {
            f.get(2, TimeUnit.SECONDS);
        }
        exec.shutdownNow();
        // Final sanity check – entrySet should still be usable
        assertDoesNotThrow(() -> cache.entrySet(), "After concurrent activity, entrySet() must still be callable without exception");
    }

    /**
     * Simulate a RuntimeException thrown from a key's hashCode() during entrySet construction.
     */
    @Test
    void entrySetPropagatesExceptionFromKeyHashCodeAndReleasesLock() {
        // Insert a normal entry first
        cache.put("good", 1);
        // Replace the key of the internal node with a faulty one
        BadKey badKey = new BadKey();
        try {
            Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
            headField.setAccessible(true);
            Object head = headField.get(cache);
            Field nextField = head.getClass().getDeclaredField("next");
            nextField.setAccessible(true);
            Object firstNode = nextField.get(head);
            Field keyField = firstNode.getClass().getDeclaredField("key");
            keyField.setAccessible(true);
            keyField.set(firstNode, badKey);
        } catch (Exception e) {
            fail("Reflection failed while injecting bad key: " + e);
        }
        // entrySet() should now throw IllegalStateException from BadKey.hashCode()
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> cache.entrySet(), "Exception from key.hashCode() must propagate");
        assertEquals("bad hash", thrown.getMessage());
        // Verify that the lock was released – a subsequent put must succeed without deadlock.
        assertDoesNotThrow(() -> cache.put("after", 2), "Lock must be released even after entrySet throws");
    }

    /**
     * Corrupt the internal linked list (head == null) and verify that a NullPointerException propagates.
     */
    @Test
    void entrySetFailsWhenInternalHeadIsNull() throws Exception {
        // Use reflection to set the private 'head' field to null
        Field headField = LockingLRUCacheStrategy.class.getDeclaredField("head");
        headField.setAccessible(true);
        headField.set(cache, null);
        // Calling entrySet() should now hit a NPE when it tries to read head.next
        assertThrows(NullPointerException.class, () -> cache.entrySet(), "Corrupted internal state should result in NullPointerException");
    }
}
