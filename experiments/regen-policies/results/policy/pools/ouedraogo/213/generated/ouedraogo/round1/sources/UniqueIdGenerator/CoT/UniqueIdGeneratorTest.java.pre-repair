package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link UniqueIdGenerator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic functionality of all public methods</li>
 *   <li>Monotonicity guarantees</li>
 *   <li>Correct extraction of timestamps (Date / Instant)</li>
 *   <li>Edge cases: clock regression and sequence overflow</li>
 *   <li>Exception handling for negative IDs</li>
 * </ul>
 *
 * The static initializer of {@code UniqueIdGenerator} determines the server id
 * from environment variables.  To keep the tests deterministic we read the
 * private {@code serverId} field via reflection and use it for verification.
 */
public class UniqueIdGeneratorTest {

    /** Cached server identifier (last two digits of every generated id). */
    private static int serverId;

    /** Helper to read a private static field. */
    private static Object getStaticField(String name) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    /** Helper to set a private static field. */
    private static void setStaticField(String name, Object value) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    @Before
    public void captureServerId() throws Exception {
        // The server id is final, but we can read it via reflection.
        serverId = (int) getStaticField("serverId");
    }

    /* --------------------------------------------------------------
     *  Basic public API tests
     * -------------------------------------------------------------- */

    @Test
    public void testGetUniqueId_basicMonotonicAndStructure() {
        long id1 = UniqueIdGenerator.getUniqueId();
        long id2 = UniqueIdGenerator.getUniqueId();

        // monotonic increase
        assertTrue("Second ID should be greater than first", id2 > id1);

        // server id must be the last two digits
        assertEquals("Server id should match the last two digits", serverId, (int) (id2 % 100));

        // date extraction must be consistent
        Date date = UniqueIdGenerator.getDate(id2);
        long expectedMillis = id2 / 100_000L;
        assertEquals("Date extracted from ID must match timestamp part", expectedMillis, date.getTime());

        // Instant extraction must be consistent
        Instant instant = UniqueIdGenerator.getInstant(id2);
        assertEquals("Instant extracted from ID must match timestamp part", expectedMillis, instant.toEpochMilli());
    }

    @Test
    public void testGetUniqueId19_basicMonotonicAndStructure() {
        long id1 = UniqueIdGenerator.getUniqueId19();
        long id2 = UniqueIdGenerator.getUniqueId19();

        // monotonic increase
        assertTrue("Second 19‑digit ID should be greater than first", id2 > id1);

        // server id must be the last two digits
        assertEquals("Server id should match the last two digits", serverId, (int) (id2 % 100));

        // date extraction must be consistent
        Date date = UniqueIdGenerator.getDate19(id2);
        long expectedMillis = id2 / 1_000_000L;
        assertEquals("Date extracted from 19‑digit ID must match timestamp part", expectedMillis, date.getTime());

        // Instant extraction must be consistent
        Instant instant = UniqueIdGenerator.getInstant19(id2);
        assertEquals("Instant extracted from 19‑digit ID must match timestamp part", expectedMillis, instant.toEpochMilli());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant_negativeId_throws() {
        UniqueIdGenerator.getInstant(-1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant19_negativeId_throws() {
        UniqueIdGenerator.getInstant19(-1L);
    }

    /* --------------------------------------------------------------
     *  Edge‑case tests
     * -------------------------------------------------------------- */

    @Test
    public void testClockRegression_handledGracefully() throws Exception {
        // Simulate a previous successful call to set lastTimeMillis in the future
        long future = System.currentTimeMillis() + 5_000L; // 5 seconds ahead
        setStaticField("lastTimeMillis", future);
        setStaticField("lastGeneratedId", 0L); // reset to avoid interference

        long id = UniqueIdGenerator.getUniqueId();

        long timestampPart = id / 100_000L;
        assertEquals("When clock moves backwards the generator must use the stored future time",
                future, timestampPart);
    }

    @Test
    public void testClockRegression19_handledGracefully() throws Exception {
        long future = System.currentTimeMillis() + 5_000L;
        setStaticField("lastTimeMillis19", future);
        setStaticField("lastGeneratedId19", 0L);

        long id = UniqueIdGenerator.getUniqueId19();

        long timestampPart = id / 1_000_000L;
        assertEquals("Clock regression for getUniqueId19 must be handled", future, timestampPart);
    }

    @Test
    public void testSequenceOverflow_getUniqueId() throws Exception {
        // Force the generator into the state where the next call would exceed the 1000‑per‑ms limit
        long now = System.currentTimeMillis();
        setStaticField("lastTimeMillis", now);
        setStaticField("count", 999); // next call will increment to 1000

        // The call should wait for the next millisecond and then produce a valid id
        long id = UniqueIdGenerator.getUniqueId();

        long timestampPart = id / 100_000L;
        // Because we forced an overflow, the timestamp part must be >= now+1
        assertTrue("After sequence overflow the timestamp must have advanced",
                timestampPart >= now + 1);
    }

    @Test
    public void testSequenceOverflow_getUniqueId19() throws Exception {
        long now = System.currentTimeMillis();
        setStaticField("lastTimeMillis19", now);
        setStaticField("count2", 9_999); // next call will overflow

        long id = UniqueIdGenerator.getUniqueId19();

        long timestampPart = id / 1_000_000L;
        assertTrue("After sequence overflow (19‑digit) the timestamp must have advanced",
                timestampPart >= now + 1);
    }

    /* --------------------------------------------------------------
     *  Helper sanity checks (not required by the assignment but useful)
     * -------------------------------------------------------------- */

    @Test
    public void testGeneratedIdsContainServerId() {
        for (int i = 0; i < 20; i++) {
            long id = UniqueIdGenerator.getUniqueId();
            assertEquals("Server id must be encoded as the last two digits", serverId, (int) (id % 100));

            long id19 = UniqueIdGenerator.getUniqueId19();
            assertEquals("Server id must be encoded as the last two digits (19‑digit)", serverId, (int) (id19 % 100));
        }
    }
}
