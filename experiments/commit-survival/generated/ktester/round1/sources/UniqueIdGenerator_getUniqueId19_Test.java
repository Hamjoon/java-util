package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

class UniqueIdGenerator_getUniqueId19_Test {

    /**
     * Holds IDs generated during a test to check for duplicates.
     */
    private Set<Long> generatedIds;

    /**
     * Reflection handles – cached for reuse.
     */
    private static Method getFullUniqueId19Method;

    private static Field lock19Field;

    private static Field lastIdsFullField;

    @BeforeAll
    static void initReflection() throws Exception {
        // Private static method getFullUniqueId19()
        getFullUniqueId19Method = UniqueIdGenerator.class.getDeclaredMethod("getFullUniqueId19");
        getFullUniqueId19Method.setAccessible(true);
        // Private static fields
        lock19Field = UniqueIdGenerator.class.getDeclaredField("lock19");
        lock19Field.setAccessible(true);
        lastIdsFullField = UniqueIdGenerator.class.getDeclaredField("lastIdsFull");
        lastIdsFullField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() {
        generatedIds = new HashSet<>();
        generatedIds = new HashSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the local set – the static state inside UniqueIdGenerator
        // (the map that remembers recent IDs) is left untouched on purpose,
        // because the class under test does not expose any clean‑up API.
        generatedIds.clear();
        // Restore original lock and map if they were set to null.
        if (lock19Field.get(null) == null) {
            lock19Field.set(null, new java.util.concurrent.locks.ReentrantLock());
        }
        if (lastIdsFullField.get(null) == null) {
            // Re‑create the original LinkedHashMap with the eviction rule.
            @SuppressWarnings("unchecked")
            Map<Long, Long> freshMap = new LinkedHashMap<Long, Long>() {

                protected boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > 10_000;
                }
            };
            lastIdsFullField.set(null, freshMap);
        }
        generatedIds.clear();
    }

    @Test
    void getUniqueId19_returnsStrictlyIncreasingUniqueValues() {
        // GIVEN – a fresh test context (setupBeforeEach has run)
        // WHEN – we request a series of IDs
        long first = UniqueIdGenerator.getUniqueId19();
        long second = UniqueIdGenerator.getUniqueId19();
        long third = UniqueIdGenerator.getUniqueId19();
        // THEN – each ID must be greater than the previous one
        assertTrue(first >= 0, "First ID should be non‑negative");
        assertTrue(second > first, "Second ID should be greater than the first");
        assertTrue(third > second, "Third ID should be greater than the second");
        // AND – all IDs must be unique
        assertTrue(generatedIds.add(first), "First ID should be unique");
        assertTrue(generatedIds.add(second), "Second ID should be unique");
        assertTrue(generatedIds.add(third), "Third ID should be unique");
    }

    @Test
    void getUniqueId19_producesManyUniqueIdsWithoutCollision() {
        // GIVEN – we will generate a batch of IDs
        // modest size to keep the test fast
        final int batchSize = 1_000;
        // WHEN – we collect a batch of IDs
        for (int i = 0; i < batchSize; i++) {
            long id = UniqueIdGenerator.getUniqueId19();
            // THEN – each ID must be unique within this batch
            assertTrue(generatedIds.add(id), "Duplicate ID detected at iteration " + i);
        }
        // Additional sanity check: the set size must equal the batch size
        assertEquals(batchSize, generatedIds.size(), "The number of unique IDs should match the number generated");
    }

    @Test
    void getUniqueId19_avoidsCollisionWithPrepopulatedId() throws Exception {
        // Obtain a deterministic ID from the private generator.
        long collidingId = (Long) getFullUniqueId19Method.invoke(null);
        // Insert it into the map so the while‑loop will see a collision.
        @SuppressWarnings("unchecked")
        Map<Long, Long> map = (Map<Long, Long>) lastIdsFullField.get(null);
        map.put(collidingId, null);
        // The next call must not return the colliding ID.
        long result = UniqueIdGenerator.getUniqueId19();
        assertNotEquals(collidingId, result, "Returned ID must differ from the pre‑populated colliding ID");
        assertTrue(result >= 0, "Returned ID should still be non‑negative");
    }

    @Test
    void getUniqueId19_avoidsMultiplePrepopulatedIds() throws Exception {
        // Generate three distinct IDs using the private method.
        @SuppressWarnings("unchecked")
        Map<Long, Long> map = (Map<Long, Long>) lastIdsFullField.get(null);
        Set<Long> prepopulated = new HashSet<>();
        for (int i = 0; i < 3; i++) {
            long id = (Long) getFullUniqueId19Method.invoke(null);
            prepopulated.add(id);
            map.put(id, null);
        }
        // Call the public method – it must return a value not among the three.
        long result = UniqueIdGenerator.getUniqueId19();
        assertFalse(prepopulated.contains(result), "Returned ID must not match any of the pre‑populated IDs");
    }

    @Test
    void getUniqueId19_evictsEldestEntryWhenCapacityExceeded() throws Exception {
        final int totalCalls = 10_001;
        List<Long> generated = new ArrayList<>(totalCalls);
        for (int i = 0; i < totalCalls; i++) {
            generated.add(UniqueIdGenerator.getUniqueId19());
        }
        @SuppressWarnings("unchecked")
        Map<Long, Long> map = (Map<Long, Long>) lastIdsFullField.get(null);
        assertEquals(10_000, map.size(), "Map size must be capped at 10 000 after eviction");
        // The very first generated ID should have been evicted.
        Long firstId = generated.get(0);
        assertFalse(map.containsKey(firstId), "The eldest ID should have been removed from the map");
    }

    @Test
    void getUniqueId19_retainsOnlyMostRecentIdsAfterEviction() throws Exception {
        final int totalCalls = 10_001;
        Deque<Long> lastTenThousand = new ArrayDeque<>(10_000);
        for (int i = 0; i < totalCalls; i++) {
            long id = UniqueIdGenerator.getUniqueId19();
            if (lastTenThousand.size() == 10_000) {
                lastTenThousand.removeFirst();
            }
            lastTenThousand.addLast(id);
        }
        @SuppressWarnings("unchecked")
        Map<Long, Long> map = (Map<Long, Long>) lastIdsFullField.get(null);
        assertEquals(10_000, map.size(), "Map should contain exactly 10 000 entries after the test");
        for (Long id : lastTenThousand) {
            assertTrue(map.containsKey(id), "Map must contain the recent ID " + id);
        }
    }

    @Test
    void getUniqueId19_concurrentCallsYieldDistinctValues() throws Exception {
        final int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<Long> concurrentResults = ConcurrentHashMap.newKeySet();
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                long id = UniqueIdGenerator.getUniqueId19();
                assertTrue(concurrentResults.add(id), "Duplicate ID observed in concurrent execution");
                return null;
            });
        }
        // invokeAll blocks until all tasks finish
        executor.invokeAll(tasks);
        executor.shutdownNow();
        assertEquals(threadCount, concurrentResults.size(), "All threads should have produced distinct IDs");
    }

    @RepeatedTest(5)
    void getUniqueId19_concurrentCalls_doNotDeadlock() throws Exception {
        // Same scenario as above but repeated to increase confidence that no deadlock occurs.
        getUniqueId19_concurrentCallsYieldDistinctValues();
    }

    @Test
    void getUniqueId19_throwsNPEWhenLockIsNull() throws Exception {
        // Set lock19 to null via reflection.
        lock19Field.set(null, null);
        Executable call = () -> UniqueIdGenerator.getUniqueId19();
        assertThrows(NullPointerException.class, call, "Calling getUniqueId19 with a null lock should throw NPE");
    }

    @Test
    void getUniqueId19_throwsNPEWhenMapIsNull() throws Exception {
        // Set lastIdsFull to null via reflection.
        lastIdsFullField.set(null, null);
        Executable call = () -> UniqueIdGenerator.getUniqueId19();
        assertThrows(NullPointerException.class, call, "Calling getUniqueId19 with a null map should throw NPE");
    }
}
