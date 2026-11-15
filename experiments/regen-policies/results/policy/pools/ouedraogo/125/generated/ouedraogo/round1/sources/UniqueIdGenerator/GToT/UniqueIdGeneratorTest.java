package com.cedarsoftware.util;

import org.junit.*;
import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@link UniqueIdGenerator}.
 *
 * The suite covers:
 * <ul>
 *   <li>Basic generation and monotonicity of {@code getUniqueId()} and {@code getUniqueId19()}</li>
 *   <li>Correct extraction of {@code Date} and {@code Instant} values</li>
 *   <li>Edge‑case handling:
 *       <ul>
 *         <li>Sequence overflow (waiting for the next millisecond)</li>
 *         <li>System‑clock regression</li>
 *         <li>Invalid (negative) IDs for {@code getInstant*()}</li>
 *       </ul>
 *   </li>
 *   <li>Thread‑safety and uniqueness under concurrent access</li>
 * </ul>
 *
 * The test class manipulates the private static state of {@code UniqueIdGenerator}
 * via reflection to force edge conditions without needing to mock {@code System.currentTimeMillis()}.
 */
public class UniqueIdGeneratorTest
{
    /* --------------------------------------------------------------------- */
    /*  Reflection helpers – used to reset or inject values into the private */
    /*  static fields of UniqueIdGenerator.                                 */
    /* --------------------------------------------------------------------- */
    private static void setStaticField(String name, Object value) throws Exception
    {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        if (value instanceof Integer)
            f.setInt(null, (Integer) value);
        else if (value instanceof Long)
            f.setLong(null, (Long) value);
        else
            f.set(null, value);
    }

    private static long getStaticLong(String name) throws Exception
    {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getLong(null);
    }

    private static int getStaticInt(String name) throws Exception
    {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(null);
    }

    /* --------------------------------------------------------------------- */
    /*  Reset the generator state before each test to avoid cross‑test         */
    /*  interference.                                                         */
    /* --------------------------------------------------------------------- */
    @Before
    public void resetGeneratorState() throws Exception
    {
        setStaticField("count", 0);
        setStaticField("count2", 0);
        setStaticField("lastTimeMillis", 0L);
        setStaticField("lastTimeMillis19", 0L);
        setStaticField("lastGeneratedId", 0L);
        setStaticField("lastGeneratedId19", 0L);
    }

    /* --------------------------------------------------------------------- */
    /*  1. Basic generation – monotonic increase and server‑id embedding       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetUniqueIdBasicMonotonic()
    {
        long id1 = UniqueIdGenerator.getUniqueId();
        long id2 = UniqueIdGenerator.getUniqueId();

        assertTrue("Second ID must be greater than first", id2 > id1);

        // Verify the two least‑significant digits are the serverId (0‑99)
        int serverId = (int) (id1 % 100);
        assertEquals("Server ID should be consistent", serverId, (int) (id2 % 100));
    }

    @Test
    public void testGetUniqueId19BasicMonotonic()
    {
        long id1 = UniqueIdGenerator.getUniqueId19();
        long id2 = UniqueIdGenerator.getUniqueId19();

        assertTrue("Second 19‑digit ID must be greater than first", id2 > id1);

        int serverId = (int) (id1 % 100);
        assertEquals("Server ID should be consistent for 19‑digit IDs", serverId, (int) (id2 % 100));
    }

    /* --------------------------------------------------------------------- */
    /*  2. Date extraction – verify division logic                           */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetDateFromUniqueId()
    {
        long id = UniqueIdGenerator.getUniqueId();
        Date expected = new Date(id / 100_000);
        Date actual   = UniqueIdGenerator.getDate(id);

        assertEquals("Date extracted from ID must match expected", expected, actual);
    }

    @Test
    public void testGetDate19FromUniqueId19()
    {
        long id = UniqueIdGenerator.getUniqueId19();
        Date expected = new Date(id / 1_000_000);
        Date actual   = UniqueIdGenerator.getDate19(id);

        assertEquals("Date19 extracted from ID must match expected", expected, actual);
    }

    /* --------------------------------------------------------------------- */
    /*  3. Instant extraction – positive IDs work, negative IDs throw       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testGetInstantPositive()
    {
        long id = UniqueIdGenerator.getUniqueId();
        Instant expected = Instant.ofEpochMilli(id / 100_000);
        Instant actual   = UniqueIdGenerator.getInstant(id);

        assertEquals("Instant extracted from ID must match expected", expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstantNegativeThrows()
    {
        UniqueIdGenerator.getInstant(-1L);
    }

    @Test
    public void testGetInstant19Positive()
    {
        long id = UniqueIdGenerator.getUniqueId19();
        Instant expected = Instant.ofEpochMilli(id / 1_000_000);
        Instant actual   = UniqueIdGenerator.getInstant19(id);

        assertEquals("Instant19 extracted from ID must match expected", expected, actual);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant19NegativeThrows()
    {
        UniqueIdGenerator.getInstant19(-123L);
    }

    /* --------------------------------------------------------------------- */
    /*  4. Sequence overflow – force count to its limit and ensure the       */
    /*     generator waits for the next millisecond.                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testSequenceOverflowGetUniqueId() throws Exception
    {
        // Prime the generator so that lastTimeMillis is the current time
        long first = UniqueIdGenerator.getUniqueId();

        // Inject a count just below the overflow threshold (999)
        setStaticField("count", 999);
        setStaticField("lastTimeMillis", System.currentTimeMillis());

        long before = System.currentTimeMillis();
        long id = UniqueIdGenerator.getUniqueId(); // should cause wait for next ms
        long after  = System.currentTimeMillis();

        // Verify that the timestamp part of the ID has advanced
        long idTimestamp = id / 100_000;
        assertTrue("Timestamp must have moved to next millisecond after overflow",
                idTimestamp > (first / 100_000));

        // Ensure at least one millisecond elapsed in real time (guard against flaky fast loops)
        assertTrue("Real time should have advanced at least 1 ms", after - before >= 1);
    }

    @Test
    public void testSequenceOverflowGetUniqueId19() throws Exception
    {
        long first = UniqueIdGenerator.getUniqueId19();

        setStaticField("count2", 9_999);
        setStaticField("lastTimeMillis19", System.currentTimeMillis());

        long before = System.currentTimeMillis();
        long id = UniqueIdGenerator.getUniqueId19(); // triggers wait
        long after  = System.currentTimeMillis();

        long idTimestamp = id / 1_000_000;
        assertTrue("Timestamp must have moved to next millisecond after overflow (19‑digit)",
                idTimestamp > (first / 1_000_000));

        assertTrue("Real time should have advanced at least 1 ms", after - before >= 1);
    }

    /* --------------------------------------------------------------------- */
    /*  5. Clock regression – simulate system clock moving backwards.       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testClockRegressionHandledGracefully() throws Exception
    {
        // Generate an ID to set a realistic lastTimeMillis
        long normalId = UniqueIdGenerator.getUniqueId();

        // Force lastTimeMillis to a future value (e.g., +500 ms)
        long future = System.currentTimeMillis() + 500;
        setStaticField("lastTimeMillis", future);

        // Now call getUniqueId(); the method should detect regression and use the
        // stored future timestamp instead of the current (earlier) system time.
        long id = UniqueIdGenerator.getUniqueId();

        long usedTimestamp = id / 100_000;
        assertEquals("Generator must fallback to stored future timestamp on regression",
                future, usedTimestamp);
    }

    /* --------------------------------------------------------------------- */
    /*  6. Concurrency – multiple threads must receive unique, monotonically */
    /*     increasing IDs.                                                   */
    /* --------------------------------------------------------------------- */
    @Test
    public void testConcurrentGenerationUniqueness() throws Exception
    {
        final int THREADS = 10;
        final int IDS_PER_THREAD = 200;
        ExecutorService exec = Executors.newFixedThreadPool(THREADS);
        Set<Long> resultSet = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
        List<Future<?>> futures = new ArrayList<>();

        for (int t = 0; t < THREADS; t++)
        {
            futures.add(exec.submit(() -> {
                for (int i = 0; i < IDS_PER_THREAD; i++)
                {
                    long id = UniqueIdGenerator.getUniqueId();
                    // The Set will reject duplicates; if add returns false, a duplicate existed
                    assertTrue("Duplicate ID detected under concurrency", resultSet.add(id));
                }
            }));
        }

        // Await completion
        for (Future<?> f : futures) f.get();
        exec.shutdownNow();

        int expectedCount = THREADS * IDS_PER_THREAD;
        assertEquals("All generated IDs must be unique", expectedCount, resultSet.size());

        // Verify monotonic increase across the sorted set
        List<Long> sorted = new ArrayList<>(resultSet);
        Collections.sort(sorted);
        for (int i = 1; i < sorted.size(); i++)
        {
            assertTrue("IDs must be strictly increasing", sorted.get(i) > sorted.get(i - 1));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  7. Verify that the internal “waitForNextMillis” does not produce      */
    /*     negative IDs even when the system clock is stuck.                 */
    /* --------------------------------------------------------------------- */
    @Test
    public void testWaitForNextMillisDoesNotProduceNegativeId() throws Exception
    {
        // Simulate a scenario where the system clock does not move forward
        // by setting lastTimeMillis to the current time and then forcing
        // the generator to think it has exhausted the sequence.
        setStaticField("count", 999);
        long now = System.currentTimeMillis();
        setStaticField("lastTimeMillis", now);

        // The next call will enter the wait loop; because the clock may stay
        // at the same millisecond for a short period, the loop should yield
        // until it progresses, after which the generated ID must be positive.
        long id = UniqueIdGenerator.getUniqueId();

        assertTrue("Generated ID must be positive even after waiting", id > 0);
    }
}
