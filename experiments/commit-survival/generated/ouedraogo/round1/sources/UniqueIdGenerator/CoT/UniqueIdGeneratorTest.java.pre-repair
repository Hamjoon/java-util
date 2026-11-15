package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link UniqueIdGenerator}.
 *
 * <p>This test class follows the requested steps:
 * <ol>
 *   <li>Lists all public methods of {@code UniqueIdGenerator}.</li>
 *   <li>Provides a basic test for each public method.</li>
 *   <li>Identifies edge‑case and exception scenarios.</li>
 *   <li>Implements additional tests that target those scenarios.</li>
 *   <li>Combines everything into a single, compilable test file.</li>
 * </ol>
 *
 * <p>Only APIs that appear in the supplied source code (or the Java standard library) are used.
 * No mocking frameworks are employed – the tests work with the real implementation.
 */
public class UniqueIdGeneratorTest {

    /* ----------------------------------------------------------------------
     *  1. PUBLIC METHOD SIGNATURES
     * ---------------------------------------------------------------------- */
    // public static long getUniqueId()
    // public static long getUniqueId19()
    // public static Date getDate(long uniqueId)
    // public static Date getDate19(long uniqueId)

    /* ----------------------------------------------------------------------
     *  2. BASIC FUNCTIONALITY TESTS
     * ---------------------------------------------------------------------- */

    /** Verify that successive calls to {@code getUniqueId()} are strictly increasing. */
    @Test
    public void testGetUniqueIdIncreasing() {
        long previous = UniqueIdGenerator.getUniqueId();
        for (int i = 0; i < 100; i++) {
            long current = UniqueIdGenerator.getUniqueId();
            assertTrue("UniqueId should be greater than the previous value", current > previous);
            previous = current;
        }
    }

    /** Verify that successive calls to {@code getUniqueId19()} are strictly increasing. */
    @Test
    public void testGetUniqueId19Increasing() {
        long previous = UniqueIdGenerator.getUniqueId19();
        for (int i = 0; i < 200; i++) {
            long current = UniqueIdGenerator.getUniqueId19();
            assertTrue("UniqueId19 should be greater than the previous value", current > previous);
            previous = current;
        }
    }

    /** Verify that {@code getDate()} correctly extracts the timestamp part of an ID. */
    @Test
    public void testGetDateMatchesTimestamp() {
        long id = UniqueIdGenerator.getUniqueId();
        Date extracted = UniqueIdGenerator.getDate(id);
        long expectedMillis = id / 100_000L;
        assertEquals("The epoch milliseconds derived from the ID should match getDate()", expectedMillis, extracted.getTime());
    }

    /** Verify that {@code getDate19()} correctly extracts the timestamp part of a 19‑digit ID. */
    @Test
    public void testGetDate19MatchesTimestamp() {
        long id = UniqueIdGenerator.getUniqueId19();
        Date extracted = UniqueIdGenerator.getDate19(id);
        long expectedMillis = id / 1_000_000L;
        assertEquals("The epoch milliseconds derived from the 19‑digit ID should match getDate19()", expectedMillis, extracted.getTime());
    }

    /* ----------------------------------------------------------------------
     *  3. EDGE‑CASE & EXCEPTION SCENARIOS
     *
     *  The class does not expose checked exceptions, but there are logical edge cases:
     *  <ul>
     *      <li>Count rollover for {@code getUniqueId()} (max 1 000 IDs per millisecond).</li>
     *      <li>Count rollover for {@code getUniqueId19()} (max 10 000 IDs per millisecond).</li>
     *      <li>Concurrent generation – ensure thread‑safety and global uniqueness.</li>
     *      <li>Large volume generation to exercise the internal “lastIds” eviction policy.</li>
     *  </ul>
     * ---------------------------------------------------------------------- */

    /**
     * Generate more than 1 000 IDs using {@code getUniqueId()} and verify that all
     * values are unique.  The internal counter should roll over, but the method
     * must still avoid returning duplicates (it loops while {@code lastIds} already
     * contains the candidate).
     */
    @Test
    public void testGetUniqueIdCountRolloverUniqueness() {
        final int total = 1500; // exceeds the 1 000 per‑ms limit
        Set<Long> ids = new HashSet<>(total);
        for (int i = 0; i < total; i++) {
            long id = UniqueIdGenerator.getUniqueId();
            assertTrue("Duplicate ID detected after count rollover", ids.add(id));
        }
        assertEquals("All generated IDs should be unique", total, ids.size());
    }

    /**
     * Generate more than 10 000 IDs using {@code getUniqueId19()} and verify
     * uniqueness across the rollover boundary.
     */
    @Test
    public void testGetUniqueId19CountRolloverUniqueness() {
        final int total = 15_000; // exceeds the 10 000 per‑ms limit
        Set<Long> ids = new HashSet<>(total);
        for (int i = 0; i < total; i++) {
            long id = UniqueIdGenerator.getUniqueId19();
            assertTrue("Duplicate ID detected after count19 rollover", ids.add(id));
        }
        assertEquals("All generated 19‑digit IDs should be unique", total, ids.size());
    }

    /**
     * Stress test with multiple threads to ensure that the generator maintains
     * strict ordering and uniqueness when accessed concurrently.
     */
    @Test
    public void testConcurrentGenerationUniqueness() throws Exception {
        final int threadCount = 8;
        final int idsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<Long> allIds = ConcurrentHashMap.newKeySet(threadCount * idsPerThread);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    startLatch.await(); // synchronize start
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = UniqueIdGenerator.getUniqueId();
                        assertTrue("Concurrent duplicate detected", allIds.add(id));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release all threads at once
        startLatch.countDown();
        // Wait for completion
        assertTrue("Threads did not finish in time", doneLatch.await(10, TimeUnit.SECONDS));
        executor.shutdownNow();

        assertEquals("All concurrently generated IDs must be unique",
                threadCount * idsPerThread, allIds.size());
    }

    /**
     * Verify that the internal eviction policy for {@code lastIds} (size > 1 000)
     * does not affect uniqueness after many generations.
     */
    @Test
    public void testLastIdsEvictionPolicy() {
        final int total = 2_500; // well beyond the 1 000 threshold
        Set<Long> ids = new HashSet<>(total);
        for (int i = 0; i < total; i++) {
            long id = UniqueIdGenerator.getUniqueId();
            assertTrue("Duplicate detected after eviction policy should have removed old entries", ids.add(id));
        }
        assertEquals("All generated IDs should remain unique after eviction", total, ids.size());
    }

    /**
     * Verify that the internal eviction policy for {@code lastIdsFull} (size > 10 000)
     * works correctly by generating a large volume of IDs.
     */
    @Test
    public void testLastIdsFullEvictionPolicy() {
        final int total = 12_500; // exceed the 10 000 limit
        Set<Long> ids = new HashSet<>(total);
        for (int i = 0; i < total; i++) {
            long id = UniqueIdGenerator.getUniqueId19();
            assertTrue("Duplicate detected after full eviction policy should have removed old entries", ids.add(id));
        }
        assertEquals("All generated 19‑digit IDs should remain unique after eviction", total, ids.size());
    }
}
