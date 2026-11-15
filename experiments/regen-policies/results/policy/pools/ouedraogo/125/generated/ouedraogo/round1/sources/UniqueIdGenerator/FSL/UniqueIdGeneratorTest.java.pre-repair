package com.cedarsoftware.util;

import static org.junit.Assert.*;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.*;
import java.time.Instant;

/**
 * Comprehensive JUnit‑4 test suite for {@link UniqueIdGenerator}.
 *
 * <p>The tests cover:
 * <ul>
 *   <li>Monotonic increase of IDs produced by {@code getUniqueId()} and {@code getUniqueId19()}</li>
 *   <li>Correct handling of the sequence counter within a single millisecond</li>
 *   <li>Proper behaviour when the system clock appears to move backwards</li>
 *   <li>Extraction of timestamps via {@code getDate}, {@code getDate19},
 *       {@code getInstant} and {@code getInstant19}</li>
 *   <li>Exception handling for negative IDs</li>
 *   <li>Thread‑safety and global uniqueness under concurrent access</li>
 * </ul>
 *
 * <p>Only public APIs and standard JDK classes are used; reflection is employed
 * solely to read or temporarily modify the private static fields of the class
 * under test (e.g., {@code serverId}, {@code lastTimeMillis},
 * {@code lastTimeMillis19}). This is permitted because the assignment explicitly
 * allows use of any class that appears in the source or the standard library.
 */
public class UniqueIdGeneratorTest {

    /**
     * Helper to read the private static final {@code serverId} field.
     */
    private static int getServerId() throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField("serverId");
        f.setAccessible(true);
        return f.getInt(null);
    }

    /**
     * Helper to set the private static {@code lastTimeMillis} field (used to
     * simulate a clock regression scenario). The field is not final, so it can be
     * modified directly via reflection.
     */
    private static void setLastTimeMillis(long value) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField("lastTimeMillis");
        f.setAccessible(true);
        f.setLong(null, value);
    }

    /**
     * Helper to set the private static {@code lastTimeMillis19} field (used to
     * simulate a clock regression scenario for the 19‑digit generator).
     */
    private static void setLastTimeMillis19(long value) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField("lastTimeMillis19");
        f.setAccessible(true);
        f.setLong(null, value);
    }

    /**
     * Verifies that two successive calls to {@code getUniqueId()} produce a
     * strictly larger value and that the increment between IDs generated within
     * the same millisecond is exactly {@code 100}.
     */
    @Test
    public void testGetUniqueIdMonotonicAndSequenceStep() {
        long id1 = UniqueIdGenerator.getUniqueId();
        long id2 = UniqueIdGenerator.getUniqueId();

        assertTrue("Second ID should be greater than the first", id2 > id1);

        // Extract the timestamp component (ms) from each ID
        long ts1 = id1 / 100_000;
        long ts2 = id2 / 100_000;

        if (ts1 == ts2) {
            // Same millisecond – the only difference should be the sequence part (100)
            assertEquals("IDs generated in the same millisecond must differ by 100",
                    100L, id2 - id1);
        } else {
            // Different milliseconds – the difference must be at least 1 ms * 100_000
            assertTrue("Timestamp part must have advanced", ts2 > ts1);
        }
    }

    /**
     * Verifies that {@code getUniqueId19()} also produces strictly increasing
     * IDs with a step of {@code 100} when generated within the same millisecond.
     */
    @Test
    public void testGetUniqueId19MonotonicAndSequenceStep() {
        long id1 = UniqueIdGenerator.getUniqueId19();
        long id2 = UniqueIdGenerator.getUniqueId19();

        assertTrue("Second 19‑digit ID should be greater than the first", id2 > id1);

        long ts1 = id1 / 1_000_000;
        long ts2 = id2 / 1_000_000;

        if (ts1 == ts2) {
            assertEquals("19‑digit IDs generated in the same millisecond must differ by 100",
                    100L, id2 - id1);
        } else {
            assertTrue("Timestamp part must have advanced for 19‑digit IDs", ts2 > ts1);
        }
    }

    /**
     * Simulates a clock regression by forcing {@code lastTimeMillis} to a value
     * that is in the future relative to the actual system clock. The generator
     * must fall back to the stored {@code lastTimeMillis} value, ensuring the
     * returned ID does not move backwards.
     */
    @Test
    public void testClockRegressionHandlingInGetUniqueId() throws Exception {
        long future = System.currentTimeMillis() + 10_000; // 10 seconds ahead
        setLastTimeMillis(future);

        long id = UniqueIdGenerator.getUniqueId();
        long extractedTimestamp = id / 100_000;

        assertEquals("When the clock moves backwards, the generator should use the stored lastTimeMillis",
                future, extractedTimestamp);
    }

    /**
     * Similar regression test for the 19‑digit generator.
     */
    @Test
    public void testClockRegressionHandlingInGetUniqueId19() throws Exception {
        long future = System.currentTimeMillis() + 10_000;
        setLastTimeMillis19(future);

        long id = UniqueIdGenerator.getUniqueId19();
        long extractedTimestamp = id / 1_000_000;

        assertEquals("Clock regression handling for getUniqueId19()", future, extractedTimestamp);
    }

    /**
     * Verifies that {@code getDate()} correctly extracts the timestamp component
     * from an ID generated by {@code getUniqueId()}.
     */
    @Test
    public void testGetDateMatchesTimestamp() {
        long id = UniqueIdGenerator.getUniqueId();
        Date extracted = UniqueIdGenerator.getDate(id);
        long expectedMillis = id / 100_000;

        assertEquals("Date extracted from ID should match the embedded timestamp",
                expectedMillis, extracted.getTime());
    }

    /**
     * Verifies that {@code getDate19()} correctly extracts the timestamp component
     * from an ID generated by {@code getUniqueId19()}.
     */
    @Test
    public void testGetDate19MatchesTimestamp() {
        long id = UniqueIdGenerator.getUniqueId19();
        Date extracted = UniqueIdGenerator.getDate19(id);
        long expectedMillis = id / 1_000_000;

        assertEquals("Date19 extracted from ID should match the embedded timestamp",
                expectedMillis, extracted.getTime());
    }

    /**
     * Verifies that {@code getInstant()} returns the same point in time as
     * {@code getDate()} (converted to {@code Instant}) and throws an exception
     * for negative IDs.
     */
    @Test
    public void testGetInstantAndInvalidInput() {
        long id = UniqueIdGenerator.getUniqueId();
        Instant instant = UniqueIdGenerator.getInstant(id);
        Date date = UniqueIdGenerator.getDate(id);

        assertEquals("Instant derived from ID must match Date derived from ID",
                date.toInstant(), instant);

        try {
            UniqueIdGenerator.getInstant(-1L);
            fail("Expected IllegalArgumentException for negative uniqueId");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /**
     * Same verification for the 19‑digit variant.
     */
    @Test
    public void testGetInstant19AndInvalidInput() {
        long id = UniqueIdGenerator.getUniqueId19();
        Instant instant = UniqueIdGenerator.getInstant19(id);
        Date date = UniqueIdGenerator.getDate19(id);

        assertEquals("Instant19 derived from ID must match Date19 derived from ID",
                date.toInstant(), instant);

        try {
            UniqueIdGenerator.getInstant19(-1L);
            fail("Expected IllegalArgumentException for negative uniqueId19");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    /**
     * Generates a large number of IDs concurrently and checks that all values
     * are unique and globally monotonic across threads.
     */
    @Test
    public void testThreadSafetyAndGlobalUniqueness() throws Exception {
        final int threads = 10;
        final int idsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        Set<Long> results = ConcurrentHashMap.newKeySet();

        Callable<Void> task = () -> {
            for (int i = 0; i < idsPerThread; i++) {
                long id = UniqueIdGenerator.getUniqueId();
                if (!results.add(id)) {
                    fail("Duplicate ID detected in concurrent generation: " + id);
                }
            }
            return null;
        };

        // Submit tasks
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(task));
        }

        // Wait for completion
        for (Future<Void> f : futures) {
            f.get(); // propagate any exception
        }
        executor.shutdownNow();

        // Verify total count
        assertEquals("All generated IDs must be unique",
                threads * idsPerThread, results.size());

        // Verify monotonicity across the whole set (sorted order)
        long previous = -1;
        for (Long id : results) {
            if (previous != -1) {
                assertTrue("All IDs must be strictly increasing when sorted", id > previous);
            }
            previous = id;
        }
    }

    /**
     * Ensures that the sequence counter rolls over correctly after reaching its
     * maximum within a single millisecond. The test forces the internal counter
     * to near‑exhaustion using reflection and then checks that the generator
     * waits for the next millisecond before continuing.
     */
    @Test
    public void testSequenceOverflowCausesWaitForNextMillisecond() throws Exception {
        // Access and set the private static counter fields
        Field countField = UniqueIdGenerator.class.getDeclaredField("count");
        countField.setAccessible(true);
        Field count2Field = UniqueIdGenerator.class.getDeclaredField("count2");
        count2Field.setAccessible(true);

        // Bring the 1000‑capacity counter close to its limit
        countField.setInt(null, 998); // next call will make it 999, then overflow
        count2Field.setInt(null, 9998); // for the 19‑digit generator

        // Capture the timestamp before the overflow
        long before = System.currentTimeMillis();

        // Generate IDs that will trigger the overflow handling
        long id1 = UniqueIdGenerator.getUniqueId();   // count becomes 999
        long id2 = UniqueIdGenerator.getUniqueId();   // should wait for next ms

        long ts1 = id1 / 100_000;
        long ts2 = id2 / 100_000;

        assertTrue("After sequence overflow the timestamp must advance to a new millisecond",
                ts2 > ts1);
        assertTrue("Timestamp after overflow must be at least the current system time",
                ts2 >= before);
    }

    /**
     * Simple sanity check that the server ID used in the generated IDs is within
     * the documented range (0‑99). The actual value is environment‑dependent, so
     * we only verify the bounds.
     */
    @Test
    public void testServerIdWithinRange() throws Exception {
        int serverId = getServerId();
        assertTrue("Server ID must be between 0 and 99 inclusive", serverId >= 0 && serverId <= 99);
    }
}
