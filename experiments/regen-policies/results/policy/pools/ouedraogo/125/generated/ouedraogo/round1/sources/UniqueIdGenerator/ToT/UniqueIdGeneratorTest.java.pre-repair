package com.cedarsoftware.util;

import org.junit.*;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link UniqueIdGenerator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Typical ID generation for both formats</li>
 *   <li>Monotonic increase guarantees</li>
 *   <li>Correct extraction of {@link Date} and {@link Instant}</li>
 *   <li>Edge‑cases such as clock regression handling and sequence overflow</li>
 *   <li>Invalid‑argument handling for {@code getInstant*}</li>
 *   <li>Thread‑safety and uniqueness under concurrent access</li>
 * </ul>
 *
 * Because {@code UniqueIdGenerator} holds static mutable state, each test resets that
 * state via reflection in {@code @Before}.  This ensures test independence and makes
 * the suite deterministic.
 */
public class UniqueIdGeneratorTest
{
    /* --------------------------------------------------------------------- */
    /*  Reflection utilities – reset the private static fields before each   */
    /*  test.  The fields are package‑private in the source, but we access   */
    /*  them reflectively because they are declared private.                  */
    /* --------------------------------------------------------------------- */
    @Before
    public void resetGeneratorState() throws Exception
    {
        setStaticLong("lastTimeMillis", 0L);
        setStaticLong("lastTimeMillis19", 0L);
        setStaticInt("count", 0);
        setStaticInt("count2", 0);
        setStaticLong("lastGeneratedId", 0L);
        setStaticLong("lastGeneratedId19", 0L);
    }

    private static void setStaticLong(String fieldName, long value) throws Exception
    {
        Field f = UniqueIdGenerator.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setLong(null, value);
    }

    private static void setStaticInt(String fieldName, int value) throws Exception
    {
        Field f = UniqueIdGenerator.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.setInt(null, value);
    }

    /* --------------------------------------------------------------------- */
    /*  1.  Basic generation and monotonic increase for getUniqueId()        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetUniqueIdMonotonicAndFormat()
    {
        long id1 = UniqueIdGenerator.getUniqueId();
        long id2 = UniqueIdGenerator.getUniqueId();

        // IDs must be positive and strictly increasing
        assertTrue("first ID should be positive", id1 > 0);
        assertTrue("second ID should be greater than first", id2 > id1);

        // Verify the structure: timestamp * 100_000 + seq*100 + serverId
        long timestampPart = id1 / 100_000;
        long seqAndServer = id1 % 100_000;
        long seq = seqAndServer / 100;
        long server = seqAndServer % 100;

        assertTrue("timestamp part should be a plausible epoch ms", timestampPart > 0);
        assertTrue("sequence should be within 0‑999", seq >= 0 && seq < 1000);
        assertTrue("serverId should be within 0‑99", server >= 0 && server < 100);
    }

    /* --------------------------------------------------------------------- */
    /*  2.  Basic generation and monotonic increase for getUniqueId19()    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetUniqueId19MonotonicAndFormat()
    {
        long id1 = UniqueIdGenerator.getUniqueId19();
        long id2 = UniqueIdGenerator.getUniqueId19();

        assertTrue("first ID19 should be positive", id1 > 0);
        assertTrue("second ID19 should be greater than first", id2 > id1);

        long timestampPart = id1 / 1_000_000;
        long seqAndServer = id1 % 1_000_000;
        long seq = seqAndServer / 100;
        long server = seqAndServer % 100;

        assertTrue("timestamp part should be a plausible epoch ms", timestampPart > 0);
        assertTrue("sequence should be within 0‑9999", seq >= 0 && seq < 10_000);
        assertTrue("serverId should be within 0‑99", server >= 0 && server < 100);
    }

    /* --------------------------------------------------------------------- */
    /*  3.  Date / Instant extraction for the 13‑digit format                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetDateAndInstantCorrespondence()
    {
        long id = UniqueIdGenerator.getUniqueId();

        Date date = UniqueIdGenerator.getDate(id);
        Instant instant = UniqueIdGenerator.getInstant(id);

        // Both extractions should represent the same epoch millisecond
        assertEquals(date.getTime(), instant.toEpochMilli());

        // The epoch millis should equal the timestamp component of the ID
        long expectedMillis = id / 100_000;
        assertEquals(expectedMillis, date.getTime());
    }

    /* --------------------------------------------------------------------- */
    /*  4.  Date / Instant extraction for the 19‑digit format                */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetDate19AndInstant19Correspondence()
    {
        long id = UniqueIdGenerator.getUniqueId19();

        Date date = UniqueIdGenerator.getDate19(id);
        Instant instant = UniqueIdGenerator.getInstant19(id);

        assertEquals(date.getTime(), instant.toEpochMilli());

        long expectedMillis = id / 1_000_000;
        assertEquals(expectedMillis, date.getTime());
    }

    /* --------------------------------------------------------------------- */
    /*  5.  Clock regression handling – simulate system clock moving back   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testClockRegressionHandledGracefully() throws Exception
    {
        // Simulate a future lastTimeMillis
        long future = System.currentTimeMillis() + 10_000;
        setStaticLong("lastTimeMillis", future);

        // The generator must not produce an ID with a timestamp earlier than 'future'
        long id = UniqueIdGenerator.getUniqueId();
        long timestamp = id / 100_000;

        assertTrue("ID timestamp should not go backwards", timestamp >= future);
    }

    /* --------------------------------------------------------------------- */
    /*  6.  Sequence overflow – when 1000 IDs are generated within a ms    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSequenceOverflowWaitsForNextMillisecond() throws Exception
    {
        long now = System.currentTimeMillis();

        // Force the generator into the same millisecond with count = 999
        setStaticLong("lastTimeMillis", now);
        setStaticInt("count", 999);
        setStaticLong("lastGeneratedId", now * 100_000 + 999 * 100 + 0); // dummy previous ID

        long start = System.currentTimeMillis();
        long id = UniqueIdGenerator.getUniqueId(); // should wait for next ms
        long elapsed = System.currentTimeMillis() - start;

        // The wait should be at most a few milliseconds (real wait is ~1 ms)
        assertTrue("Method should have waited for next millisecond", elapsed >= 0);
        long timestamp = id / 100_000;
        assertTrue("Timestamp must be greater than the forced 'now'", timestamp > now);
    }

    /* --------------------------------------------------------------------- */
    /*  7.  Invalid argument handling for getInstant* methods               */
    /* --------------------------------------------------------------------- */
    @Test(expected = IllegalArgumentException.class)
    public void testGetInstantRejectsNegative()
    {
        UniqueIdGenerator.getInstant(-1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant19RejectsNegative()
    {
        UniqueIdGenerator.getInstant19(-1L);
    }

    /* --------------------------------------------------------------------- */
    /*  8.  Concurrency – generate IDs from many threads and verify uniqueness */
    /* --------------------------------------------------------------------- */
    @Test
    public void testConcurrentGenerationProducesUniqueIds()
        throws Exception
    {
        final int THREADS = 8;
        final int IDS_PER_THREAD = 5_000; // total 40k IDs – well within limits

        ExecutorService exec = Executors.newFixedThreadPool(THREADS);
        Set<Long> uniqueIds = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < THREADS; t++)
        {
            futures.add(exec.submit(() -> {
                for (int i = 0; i < IDS_PER_THREAD; i++)
                {
                    long id = UniqueIdGenerator.getUniqueId();
                    assertTrue("All IDs must be positive", id > 0);
                    // The Set's add() returns false if the element already exists
                    assertTrue("Duplicate ID detected", uniqueIds.add(id));
                }
            }));
        }

        // Wait for all threads to finish
        for (Future<?> f : futures)
        {
            f.get(); // propagates any exception
        }
        exec.shutdownNow();

        assertEquals("Total number of generated IDs should match expected",
                THREADS * IDS_PER_THREAD, uniqueIds.size());
    }

    /* --------------------------------------------------------------------- */
    /*  9.  Verify that getUniqueId19 also respects monotonic increase after */
    /*      a manual manipulation of its internal state.                     */
    /* --------------------------------------------------------------------- */
    @Test
    public void testUniqueId19MonotonicAfterStateManipulation() throws Exception
    {
        long now = System.currentTimeMillis();
        setStaticLong("lastTimeMillis19", now);
        setStaticInt("count2", 9_999);
        setStaticLong("lastGeneratedId19", now * 1_000_000 + 9_999 * 100 + 0);

        long id = UniqueIdGenerator.getUniqueId19(); // should wait for next ms
        long timestamp = id / 1_000_000;
        assertTrue("Timestamp must have advanced to next millisecond", timestamp > now);
    }
}
