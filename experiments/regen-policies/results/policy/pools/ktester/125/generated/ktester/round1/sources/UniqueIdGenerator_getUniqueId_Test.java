package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import static com.cedarsoftware.util.UniqueIdGenerator.getUniqueId;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

// kept from the template; no mocks are actually used
@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getUniqueId_Test {

    /**
     * Number of IDs generated per thread in the concurrency test.
     */
    private static final int IDS_PER_THREAD = 50_000;

    /**
     * Number of threads used in the concurrency test.
     */
    private static final int THREAD_COUNT = 8;

    /**
     * Verify that the ID produced by {@code getUniqueId()} is exactly 18 digits long.
     */
    @Test
    void testIdLength() {
        long id = getUniqueId();
        assertEquals(18, String.valueOf(id).length(), "Unique ID should be exactly 18 digits long");
    }

    /**
     * Verify that successive calls return strictly increasing values.
     */
    @Test
    void testMonotonicIncreasing() {
        long previous = getUniqueId();
        // Generate a few thousand IDs to increase the chance of hitting the same millisecond.
        for (int i = 0; i < 5_000; i++) {
            long current = getUniqueId();
            assertTrue(current > previous, () -> "ID not increasing: previous=" + previous + " current=" + current);
            previous = current;
        }
    }

    /**
     * Verify that concurrent generation never yields duplicate IDs.
     *
     * <p>The test creates a pool of threads, each generating a large number of IDs.
     * All IDs are collected into a thread‑safe set; at the end we assert that the
     * set size equals the total number of generated IDs.
     */
    @Test
    void testConcurrentGenerationProducesUniqueIds() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        Set<Long> allIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        for (int t = 0; t < THREAD_COUNT; t++) {
            executor.submit(() -> {
                try {
                    // Wait until all threads are ready to start together.
                    startLatch.await();
                    for (int i = 0; i < IDS_PER_THREAD; i++) {
                        long id = getUniqueId();
                        // add returns false if the element was already present
                        boolean added = allIds.add(id);
                        assertTrue(added, () -> "Duplicate ID detected: " + id);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Thread interrupted", e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Release all worker threads simultaneously.
        startLatch.countDown();
        // Wait for all workers to finish.
        assertTrue(doneLatch.await(30, TimeUnit.SECONDS), "Timed out waiting for worker threads to finish");
        executor.shutdownNow();
        long expectedTotal = (long) THREAD_COUNT * IDS_PER_THREAD;
        assertEquals(expectedTotal, allIds.size(), "Expected " + expectedTotal + " unique IDs, but got " + allIds.size());
    }

    /**
     * Normal unique ID generation (first call after a "cold" JVM start).
     *
     * <p>We reset the internal static state to its defaults (count = 0,
     * lastTimeMillis = 0, lastGeneratedId = 0) via reflection, invoke
     * {@code getUniqueId()}, and verify that a positive ID is returned.
     * The exact numeric value depends on the current system time and the
     * serverId, but it must be greater than zero.
     */
    @Test
    void testNormalFirstCallGeneration() throws Exception {
        // ---- Save original static field values so we can restore them later ----
        Field countField = UniqueIdGenerator.class.getDeclaredField("count");
        Field lastTimeField = UniqueIdGenerator.class.getDeclaredField("lastTimeMillis");
        Field lastGenField = UniqueIdGenerator.class.getDeclaredField("lastGeneratedId");
        countField.setAccessible(true);
        lastTimeField.setAccessible(true);
        lastGenField.setAccessible(true);
        int originalCount = (int) countField.get(null);
        long originalLastTime = (long) lastTimeField.get(null);
        long originalLastGen = (long) lastGenField.get(null);
        try {
            // ---- Reset internal state ----
            countField.setInt(null, 0);
            lastTimeField.setLong(null, 0L);
            lastGenField.setLong(null, 0L);
            // ---- Invoke the method under test ----
            long id = getUniqueId();
            // ---- Verify contract ----
            assertTrue(id > 0, "First generated ID should be a positive long");
            // The ID must be 18 digits (already covered by testIdLength, but we keep a quick sanity check)
            assertEquals(18, String.valueOf(id).length(), "First generated ID should have exactly 18 digits");
        } finally {
            // ---- Restore original static state ----
            countField.setInt(null, originalCount);
            lastTimeField.setLong(null, originalLastTime);
            lastGenField.setLong(null, originalLastGen);
        }
    }

    /**
     * Clock regression handling.
     *
     * <p>We simulate the system clock moving backwards by forcing
     * {@code lastTimeMillis} to a value in the future (now + 5 seconds).
     * The method should detect the regression and continue to use the stored
     * {@code lastTimeMillis} for the timestamp component of the generated ID.
     * The test extracts the timestamp part from the returned ID and asserts
     * that it equals the artificially injected {@code lastTimeMillis}.
     */
    @Test
    void testClockRegressionUsesLastTimeMillis() throws Exception {
        // ---- Access private static fields via reflection ----
        Field lastTimeField = UniqueIdGenerator.class.getDeclaredField("lastTimeMillis");
        Field lastGenField = UniqueIdGenerator.class.getDeclaredField("lastGeneratedId");
        Field countField = UniqueIdGenerator.class.getDeclaredField("count");
        lastTimeField.setAccessible(true);
        lastGenField.setAccessible(true);
        countField.setAccessible(true);
        // Preserve original values
        long originalLastTime = (long) lastTimeField.get(null);
        long originalLastGen = (long) lastGenField.get(null);
        int originalCount = (int) countField.get(null);
        try {
            // ---- Simulate a clock that has moved backwards ----
            // +5 seconds
            long futureMillis = System.currentTimeMillis() + 5_000;
            lastTimeField.setLong(null, futureMillis);
            // Ensure count is zero so the generated ID uses the same timestamp
            countField.setInt(null, 0);
            // Set lastGeneratedId to a value smaller than what we expect,
            // otherwise the method might bump the ID by +1.
            lastGenField.setLong(null, 0L);
            // ---- Generate an ID ----
            long id = getUniqueId();
            // ---- Extract timestamp component ----
            // ID format: timestamp * 100_000 + sequence * 100 + serverId
            long extractedTimestamp = id / 100_000L;
            assertEquals(futureMillis, extractedTimestamp, "When the system clock moves backwards, the generator must reuse the stored lastTimeMillis");
            // Also verify that the ID is still greater than the previous (which was 0)
            assertTrue(id > 0, "Generated ID must be positive even after clock regression");
        } finally {
            // ---- Restore original static state ----
            lastTimeField.setLong(null, originalLastTime);
            lastGenField.setLong(null, originalLastGen);
            countField.setInt(null, originalCount);
        }
    }
}
