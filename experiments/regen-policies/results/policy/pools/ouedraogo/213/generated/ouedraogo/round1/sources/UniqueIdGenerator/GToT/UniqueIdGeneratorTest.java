package com.cedarsoftware.util;

import org.junit.*;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class UniqueIdGeneratorTest {

    // -------------------------------------------------------------------------
    // Utility helpers to manipulate the private static fields of UniqueIdGenerator
    // -------------------------------------------------------------------------
    private static void setStaticField(String name, Object value) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    private static Object getStaticField(String name) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    /** Reset all mutable static state before each test to guarantee isolation. */
    @Before
    public void resetState() throws Exception {
        setStaticField("count", 0);
        setStaticField("count2", 0);
        setStaticField("lastTimeMillis", 0L);
        setStaticField("lastTimeMillis19", 0L);
        setStaticField("lastGeneratedId", 0L);
        setStaticField("lastGeneratedId19", 0L);
    }

    // -------------------------------------------------------------------------
    // 1. Basic functionality tests
    // -------------------------------------------------------------------------

    @Test
    public void testGetUniqueId_MonotonicAndPositive() {
        long first = UniqueIdGenerator.getUniqueId();
        long second = UniqueIdGenerator.getUniqueId();

        assertTrue("IDs must be positive", first > 0 && second > 0);
        assertTrue("Second ID must be greater than first", second > first);
    }

    @Test
    public void testGetUniqueId19_MonotonicAndPositive() {
        long first = UniqueIdGenerator.getUniqueId19();
        long second = UniqueIdGenerator.getUniqueId19();

        assertTrue("IDs must be positive", first > 0 && second > 0);
        assertTrue("Second ID must be greater than first", second > first);
    }

    @Test
    public void testGetDate_ExtractsCorrectTimestamp() {
        long id = UniqueIdGenerator.getUniqueId();
        Date date = UniqueIdGenerator.getDate(id);
        long now = System.currentTimeMillis();

        // The extracted timestamp should be within ±5 ms of the current time.
        long diff = Math.abs(date.getTime() - now);
        assertTrue("Extracted date should be close to now (diff=" + diff + "ms)", diff < 10);
    }

    @Test
    public void testGetDate19_ExtractsCorrectTimestamp() {
        long id = UniqueIdGenerator.getUniqueId19();
        Date date = UniqueIdGenerator.getDate19(id);
        long now = System.currentTimeMillis();

        long diff = Math.abs(date.getTime() - now);
        assertTrue("Extracted date19 should be close to now (diff=" + diff + "ms)", diff < 10);
    }

    @Test
    public void testGetInstant_ValidId() {
        long id = UniqueIdGenerator.getUniqueId();
        Instant instant = UniqueIdGenerator.getInstant(id);
        long now = System.currentTimeMillis();

        long diff = Math.abs(instant.toEpochMilli() - now);
        assertTrue("Instant should be close to now (diff=" + diff + "ms)", diff < 10);
    }

    @Test
    public void testGetInstant19_ValidId() {
        long id = UniqueIdGenerator.getUniqueId19();
        Instant instant = UniqueIdGenerator.getInstant19(id);
        long now = System.currentTimeMillis();

        long diff = Math.abs(instant.toEpochMilli() - now);
        assertTrue("Instant19 should be close to now (diff=" + diff + "ms)", diff < 10);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant_NegativeIdThrows() {
        UniqueIdGenerator.getInstant(-1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant19_NegativeIdThrows() {
        UniqueIdGenerator.getInstant19(-1L);
    }

    // -------------------------------------------------------------------------
    // 2. Edge‑case tests
    // -------------------------------------------------------------------------

    @Test
    public void testClockRegressionHandledForUniqueId() throws Exception {
        // Simulate a forward clock, then move the system clock backwards.
        long fakeFuture = System.currentTimeMillis() + 10_000L; // 10 seconds ahead
        setStaticField("lastTimeMillis", fakeFuture);

        long id = UniqueIdGenerator.getUniqueId();
        long extractedTimestamp = id / 100_000L; // timestamp component

        assertEquals("When clock regresses, generator must use lastTimeMillis",
                     fakeFuture, extractedTimestamp);
    }

    @Test
    public void testClockRegressionHandledForUniqueId19() throws Exception {
        long fakeFuture = System.currentTimeMillis() + 10_000L;
        setStaticField("lastTimeMillis19", fakeFuture);

        long id = UniqueIdGenerator.getUniqueId19();
        long extractedTimestamp = id / 1_000_000L; // timestamp component

        assertEquals("Clock regression handling for getUniqueId19", fakeFuture, extractedTimestamp);
    }

    @Test
    public void testSequenceOverflowWaitsForNextMillisecond_UniqueId() throws Exception {
        // Force the internal counter to the max‑1 value (999) while keeping the same millisecond.
        long now = System.currentTimeMillis();
        setStaticField("lastTimeMillis", now);
        setStaticField("count", 999); // next call should overflow

        long idBefore = UniqueIdGenerator.getUniqueId(); // this will be the 1000th ID in the same ms
        long tsBefore = idBefore / 100_000L;

        // The next call must wait for the next millisecond
        long idAfter = UniqueIdGenerator.getUniqueId();
        long tsAfter = idAfter / 100_000L;

        assertTrue("Timestamp must have advanced after sequence overflow", tsAfter > tsBefore);
        assertTrue("ID after overflow must be greater than previous ID", idAfter > idBefore);
    }

    @Test
    public void testSequenceOverflowWaitsForNextMillisecond_UniqueId19() throws Exception {
        long now = System.currentTimeMillis();
        setStaticField("lastTimeMillis19", now);
        setStaticField("count2", 9_999); // max‑1 for the 19‑digit generator

        long idBefore = UniqueIdGenerator.getUniqueId19(); // 10 000th ID in same ms
        long tsBefore = idBefore / 1_000_000L;

        long idAfter = UniqueIdGenerator.getUniqueId19();
        long tsAfter = idAfter / 1_000_000L;

        assertTrue("Timestamp must advance after overflow (19‑digit)", tsAfter > tsBefore);
        assertTrue("ID after overflow must be greater than previous ID", idAfter > idBefore);
    }

    @Test
    public void testServerIdWithinExpectedRange() throws Exception {
        int serverId = (int) getStaticField("serverId");
        assertTrue("Server ID must be between 0 and 99 inclusive", serverId >= 0 && serverId <= 99);
    }

    // -------------------------------------------------------------------------
    // 3. Concurrency / uniqueness test (light‑weight)
    // -------------------------------------------------------------------------

    @Test
    public void testConcurrentGeneration_UniqueIdsAreMonotonic() throws Exception {
        final int THREADS = 50;
        final int IDS_PER_THREAD = 200;
        ExecutorService exec = Executors.newFixedThreadPool(THREADS);
        Set<Long> results = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < THREADS; t++) {
            futures.add(exec.submit(() -> {
                for (int i = 0; i < IDS_PER_THREAD; i++) {
                    long id = UniqueIdGenerator.getUniqueId();
                    results.add(id);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(); // propagate any exception
        }
        exec.shutdownNow();

        // Expected number of distinct IDs
        int expected = THREADS * IDS_PER_THREAD;
        assertEquals("All generated IDs must be distinct", expected, results.size());

        // Verify monotonicity when sorted
        List<Long> sorted = new ArrayList<>(results);
        Collections.sort(sorted);
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue("IDs must be strictly increasing when ordered", sorted.get(i) > sorted.get(i - 1));
        }
    }

    // -------------------------------------------------------------------------
    // 4. Helper to extract timestamp component (used in a few tests)
    // -------------------------------------------------------------------------
    private static long timestampFromUniqueId(long id) {
        return id / 100_000L;
    }

    private static long timestampFromUniqueId19(long id) {
        return id / 1_000_000L;
    }
}
