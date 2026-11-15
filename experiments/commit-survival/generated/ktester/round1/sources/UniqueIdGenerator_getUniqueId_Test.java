package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getUniqueId_Test {

    /**
     * Reference to the private static {@code lastIds} map.
     */
    private static Map<Long, Long> lastIdsMap;

    /**
     * Reference to the private static {@code lock}.
     */
    private static ReentrantLock internalLock;

    /**
     * Private method that actually creates a candidate ID.
     */
    private static Method getUniqueIdAttemptMethod;

    @BeforeAll
    static void initReflection() throws Exception {
        // Access the private static fields/methods once for the whole test class.
        Field mapField = UniqueIdGenerator.class.getDeclaredField("lastIds");
        mapField.setAccessible(true);
        //noinspection unchecked
        lastIdsMap = (Map<Long, Long>) mapField.get(null);
        Field lockField = UniqueIdGenerator.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        internalLock = (ReentrantLock) lockField.get(null);
        getUniqueIdAttemptMethod = UniqueIdGenerator.class.getDeclaredMethod("getUniqueIdAttempt");
        getUniqueIdAttemptMethod.setAccessible(true);
    }

    @BeforeEach
    void clearState() {
        // Ensure a clean map before each test.
        synchronized (lastIdsMap) {
            lastIdsMap.clear();
        }
    }

    /**
     * Verify that two successive calls produce a strictly increasing value.
     */
    @Test
    void testUniqueIdIsIncreasing() {
        long first = UniqueIdGenerator.getUniqueId();
        long second = UniqueIdGenerator.getUniqueId();
        assertTrue(second > first, () -> "Second ID (" + second + ") should be greater than first ID (" + first + ")");
    }

    /**
     * Generate a batch of IDs and ensure none are duplicated.
     */
    @Test
    void testUniqueIdIsUniqueAcrossMultipleCalls() {
        int count = 1_000;
        Set<Long> ids = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            long id = UniqueIdGenerator.getUniqueId();
            boolean added = ids.add(id);
            assertTrue(added, () -> "Duplicate ID detected: " + id);
        }
        assertEquals(count, ids.size(), () -> "Expected " + count + " unique IDs, but got " + ids.size());
    }

    /**
     * Stress test the method with concurrent callers to verify thread‑safety and uniqueness.
     */
    @Test
    void testThreadSafetyAndUniqueness() throws InterruptedException, ExecutionException {
        int threads = 10;
        int idsPerThread = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<List<Long>>> tasks = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            tasks.add(() -> {
                List<Long> local = new ArrayList<>(idsPerThread);
                for (int i = 0; i < idsPerThread; i++) {
                    local.add(UniqueIdGenerator.getUniqueId());
                }
                return local;
            });
        }
        List<Future<List<Long>>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate in the expected time");
        Set<Long> allIds = new HashSet<>(threads * idsPerThread);
        for (Future<List<Long>> f : futures) {
            List<Long> list = f.get();
            for (Long id : list) {
                boolean added = allIds.add(id);
                assertTrue(added, () -> "Duplicate ID found in concurrent execution: " + id);
            }
        }
        assertEquals(threads * idsPerThread, allIds.size(), "Total number of unique IDs does not match expected count");
    }

    @Test
    void testBasicFunctionality_normalPath() throws Exception {
        // Ensure the map is empty.
        assertTrue(lastIdsMap.isEmpty(), "Map should be empty before the test");
        // Call the method once.
        long id = UniqueIdGenerator.getUniqueId();
        // The returned id must be present as a key in the map.
        assertTrue(lastIdsMap.containsKey(id), () -> "Map should contain the generated id " + id);
        assertEquals(1, lastIdsMap.size(), "Map should contain exactly one entry after a single call");
    }

    @Test
    void testBasicFunctionality_collisionPath() throws Exception {
        // Obtain an id that the private attempt method would generate.
        long collidingId = (Long) getUniqueIdAttemptMethod.invoke(null);
        // Pre‑populate the map with that id to force a collision.
        lastIdsMap.put(collidingId, null);
        assertTrue(lastIdsMap.containsKey(collidingId), "Pre‑populated colliding id must be present");
        // Now invoke getUniqueId(); it should detect the collision and generate a different id.
        long newId = UniqueIdGenerator.getUniqueId();
        assertNotEquals(collidingId, newId, "Generated id must differ from the colliding id");
        assertTrue(lastIdsMap.containsKey(newId), "Map must contain the newly generated id");
        assertTrue(lastIdsMap.containsKey(collidingId), "Original colliding id must still be present");
        assertEquals(2, lastIdsMap.size(), "Map should contain exactly two distinct keys after handling the collision");
    }

    @Test
    void testMapEviction_after1001Calls() throws Exception {
        final int totalCalls = 1_001;
        List<Long> generated = new ArrayList<>(totalCalls);
        for (int i = 0; i < totalCalls; i++) {
            generated.add(UniqueIdGenerator.getUniqueId());
        }
        // The eldest entry (the first generated id) should have been evicted.
        long firstId = generated.get(0);
        long lastId = generated.get(generated.size() - 1);
        assertFalse(lastIdsMap.containsKey(firstId), "The first generated id should have been evicted from the map");
        assertTrue(lastIdsMap.containsKey(lastId), "The most recent id must be present in the map");
        assertEquals(1_000, lastIdsMap.size(), "Map size must be capped at 1,000 entries");
    }

    @Test
    void testMapEviction_stressBeyondCapacity() throws Exception {
        final int totalCalls = 2_500;
        // keep the last 1 000 ids
        Deque<Long> recent = new ArrayDeque<>(1_000);
        for (int i = 0; i < totalCalls; i++) {
            long id = UniqueIdGenerator.getUniqueId();
            recent.addLast(id);
            if (recent.size() > 1_000) {
                recent.removeFirst();
            }
        }
        assertTrue(lastIdsMap.size() <= 1_000, "Map size must never exceed 1,000");
        // Verify that every key present in the map belongs to the last 1,000 generated ids.
        for (Long key : lastIdsMap.keySet()) {
            assertTrue(recent.contains(key), () -> "Map contains an unexpected id " + key + " that is not among the most recent 1,000");
        }
    }

    @Test
    void testHighConcurrencyUniqueness_50Threads() throws Exception {
        int threads = 50;
        int idsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<List<Long>>> tasks = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            tasks.add(() -> {
                List<Long> local = new ArrayList<>(idsPerThread);
                for (int j = 0; j < idsPerThread; j++) {
                    local.add(UniqueIdGenerator.getUniqueId());
                }
                return local;
            });
        }
        List<Future<List<Long>>> futures = executor.invokeAll(tasks);
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate in time");
        Set<Long> allIds = new HashSet<>(threads * idsPerThread);
        for (Future<List<Long>> f : futures) {
            for (Long id : f.get()) {
                assertTrue(allIds.add(id), () -> "Duplicate id detected: " + id);
            }
        }
        assertEquals(threads * idsPerThread, allIds.size(), "All generated ids must be unique");
        // After many concurrent calls the internal map must still respect the 1 000‑entry limit.
        assertEquals(1_000, lastIdsMap.size(), "Internal lastIds map should be capped at 1,000 entries after high concurrency");
    }

    @Test
    void testIdsAreNeverNegative() {
        for (int i = 0; i < 1_000; i++) {
            long id = UniqueIdGenerator.getUniqueId();
            assertTrue(id >= 0, () -> "Generated id should be non‑negative, but got " + id);
        }
    }

    @Test
    void testLockReleaseWhenInterrupted() throws Exception {
        // Acquire the lock in the main thread, so the worker will block.
        internalLock.lock();
        try {
            // Worker thread that will call getUniqueId() while the lock is held.
            Callable<Long> worker = () -> UniqueIdGenerator.getUniqueId();
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<Long> future = executor.submit(worker);
            // Give the worker a moment to start and block on the lock.
            Thread.sleep(100);
            // Interrupt the waiting thread.
            // this sends an interrupt
            future.cancel(true);
            // Release the lock so the worker can proceed.
            internalLock.unlock();
            // The future should still complete normally (cancel does not abort the task
            // because the lock acquisition is not interruptible).
            Long result = future.get(2, TimeUnit.SECONDS);
            assertNotNull(result, "Result should not be null after lock is released");
            assertTrue(result >= 0, "Resulting id must be non‑negative");
            executor.shutdownNow();
        } finally {
            // Ensure the lock is released even if the test fails.
            if (internalLock.isHeldByCurrentThread()) {
                internalLock.unlock();
            }
        }
    }

    @Test
    void testLockReleasedWhenGetUniqueIdAttemptThrows() throws Exception {
        // Save original method reference (cannot actually replace it, but we can invoke it
        // via reflection and force an exception by making it inaccessible – this is a
        // best‑effort simulation because the real implementation is not replaceable
        // without byte‑code manipulation.  We therefore simulate the scenario by
        // invoking the method on a null target, which throws a NullPointerException.
        Method original = getUniqueIdAttemptMethod;
        // Create a proxy that throws a RuntimeException when invoked.
        Method throwingMethod = UniqueIdGenerator.class.getDeclaredMethod("getUniqueIdAttempt");
        throwingMethod.setAccessible(true);
        // Use a custom SecurityManager to force an exception is over‑kill; instead we
        // simply invoke the method via reflection with an illegal argument to cause
        // an exception inside getUniqueId().
        // The goal is to ensure that even when an exception propagates, the lock is
        // released.  We achieve this by calling getUniqueId() inside a try‑catch where
        // we deliberately cause a NullPointerException by passing a malformed state.
        // Corrupt the internal map to a state that will cause getUniqueIdAttempt()
        // to throw (e.g., make the map null). This forces a NullPointerException
        // inside getUniqueId().
        Field mapField = UniqueIdGenerator.class.getDeclaredField("lastIds");
        mapField.setAccessible(true);
        Object originalMap = mapField.get(null);
        try {
            // Set the map to null to trigger NPE inside getUniqueId().
            mapField.set(null, null);
            assertThrows(NullPointerException.class, UniqueIdGenerator::getUniqueId, "Expected NullPointerException due to corrupted internal state");
            // After the exception the lock must be free; we can acquire it again.
            assertDoesNotThrow(() -> {
                internalLock.lock();
                internalLock.unlock();
            }, "Lock should be released even after an exception");
        } finally {
            // Restore original map to avoid side effects for other tests.
            mapField.set(null, originalMap);
        }
    }
}
