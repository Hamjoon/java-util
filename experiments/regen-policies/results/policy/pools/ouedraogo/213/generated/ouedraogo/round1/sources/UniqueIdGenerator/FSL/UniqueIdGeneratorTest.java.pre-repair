package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

/**
 * Test suite for {@link UniqueIdGenerator}. The tests exercise all public
 * methods and also verify the internal state handling (sequence overflow,
 * clock regression, monotonicity, and date/instant extraction).  No external
 * libraries are mocked – the tests manipulate the private static fields via
 * reflection to simulate edge‑cases such as sequence exhaustion and clock
 * regression.
 */
public class UniqueIdGeneratorTest {

    /* --------------------------------------------------------------------- *
     *  Reflection helpers to reset and manipulate the private static fields   *
     * --------------------------------------------------------------------- */

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

    /** Reset all mutable static fields to a clean state before each test. */
    @Before
    public void resetGeneratorState() throws Exception {
        setStaticField("count", 0);
        setStaticField("count2", 0);
        setStaticField("lastTimeMillis", 0L);
        setStaticField("lastTimeMillis19", 0L);
        setStaticField("lastGeneratedId", 0L);
        setStaticField("lastGeneratedId19", 0L);
    }

    /* --------------------------------------------------------------------- *
     *  Helper methods for extracting the three components of an ID            *
     * --------------------------------------------------------------------- */

    /** Returns the server‑id (last two digits) of a generated ID. */
    private static int getServerIdFromId(long id) {
        return (int) (id % 100);
    }

    /** Returns the sequence component of a getUniqueId() value. */
    private static int getSequenceFromId(long id) {
        return (int) ((id / 100) % 1_000);
    }

    /** Returns the timestamp (milliseconds since epoch) encoded in a getUniqueId() value. */
    private static long getTimestampFromId(long id) {
        return id / 100_000;
    }

    /** Returns the sequence component of a getUniqueId19() value. */
    private static int getSequence19FromId(long id) {
        return (int) ((id / 100) % 10_000);
    }

    /** Returns the timestamp (milliseconds since epoch) encoded in a getUniqueId19() value. */
    private static long getTimestamp19FromId(long id) {
        return id / 1_000_000;
    }

    /* --------------------------------------------------------------------- *
     *  Tests for the standard 17‑digit ID (getUniqueId)                      *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetUniqueId_MonotonicAndFormat() {
        long first = UniqueIdGenerator.getUniqueId();
        long second = UniqueIdGenerator.getUniqueId();

        // monotonic increase
        assertTrue("Second ID must be greater than first ID", second > first);

        // server id must be the same for both IDs
        int serverId = getServerIdFromId(first);
        assertEquals(serverId, getServerIdFromId(second));

        // sequence should increase (or wrap after 999)
        int seq1 = getSequenceFromId(first);
        int seq2 = getSequenceFromId(second);
        assertTrue("Sequence must increase or wrap", (seq2 == (seq1 + 1) % 1_000));

        // timestamp part must be non‑decreasing
        assertTrue(getTimestampFromId(second) >= getTimestampFromId(first));
    }

    @Test
    public void testGetUniqueId_DateAndInstantExtraction() {
        long id = UniqueIdGenerator.getUniqueId();
        long ts = getTimestampFromId(id);

        Date date = UniqueIdGenerator.getDate(id);
        assertEquals("Date epoch milli must match timestamp part", ts, date.getTime());

        Instant instant = UniqueIdGenerator.getInstant(id);
        assertEquals("Instant epoch milli must match timestamp part", ts, instant.toEpochMilli());
    }

    @Test
    public void testGetUniqueId_ClockRegressionHandled() throws Exception {
        // Simulate a previous generation at a future time
        long fakeFuture = System.currentTimeMillis() + 10_000; // 10 seconds ahead
        setStaticField("lastTimeMillis", fakeFuture);
        setStaticField("lastGeneratedId", fakeFuture * 100_000 + 99); // arbitrary high id

        // The generator should notice the regression and reuse the stored future time
        long id = UniqueIdGenerator.getUniqueId();
        long ts = getTimestampFromId(id);
        assertEquals("Timestamp must be the stored future time after regression", fakeFuture, ts);
    }

    @Test
    public void testGetUniqueId_SequenceOverflow_WaitsForNextMillisecond() throws Exception {
        long now = System.currentTimeMillis();

        // Force the generator to think we are already at 'now' with count = 999
        setStaticField("lastTimeMillis", now);
        setStaticField("count", 999);
        setStaticField("lastGeneratedId", now * 100_000 + 999 * 100 + getStaticField("serverId"));

        // The next call should cause the sequence to overflow, wait for the next ms,
        // reset count to 0 and produce a timestamp > now.
        long id = UniqueIdGenerator.getUniqueId();
        long ts = getTimestampFromId(id);
        assertTrue("Timestamp must have advanced to the next millisecond", ts > now);
        assertEquals("Sequence after overflow must be 0", 0, getSequenceFromId(id));
    }

    @Test
    public void testGetUniqueId_IdNeverDecreasesEvenIfComputedValueIsLower() throws Exception {
        // Set a high lastGeneratedId manually
        long highId = 9_999_999_999_999L; // arbitrary large value
        setStaticField("lastGeneratedId", highId);
        setStaticField("lastTimeMillis", System.currentTimeMillis());
        setStaticField("count", 0);

        // The computed newId will be smaller than highId, so the method must bump it
        long id = UniqueIdGenerator.getUniqueId();
        assertTrue("Generated ID must be greater than previously stored high ID", id > highId);
    }

    /* --------------------------------------------------------------------- *
     *  Tests for the 19‑digit ID (getUniqueId19)                             *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetUniqueId19_MonotonicAndFormat() {
        long first = UniqueIdGenerator.getUniqueId19();
        long second = UniqueIdGenerator.getUniqueId19();

        assertTrue("Second ID must be greater than first ID", second > first);

        int serverId = getServerIdFromId(first);
        assertEquals(serverId, getServerIdFromId(second));

        int seq1 = getSequence19FromId(first);
        int seq2 = getSequence19FromId(second);
        assertTrue("Sequence must increase or wrap", (seq2 == (seq1 + 1) % 10_000));

        assertTrue(getTimestamp19FromId(second) >= getTimestamp19FromId(first));
    }

    @Test
    public void testGetUniqueId19_DateAndInstantExtraction() {
        long id = UniqueIdGenerator.getUniqueId19();
        long ts = getTimestamp19FromId(id);

        Date date = UniqueIdGenerator.getDate19(id);
        assertEquals("Date epoch milli must match timestamp part", ts, date.getTime());

        Instant instant = UniqueIdGenerator.getInstant19(id);
        assertEquals("Instant epoch milli must match timestamp part", ts, instant.toEpochMilli());
    }

    @Test
    public void testGetUniqueId19_ClockRegressionHandled() throws Exception {
        long fakeFuture = System.currentTimeMillis() + 20_000;
        setStaticField("lastTimeMillis19", fakeFuture);
        setStaticField("lastGeneratedId19", fakeFuture * 1_000_000 + 99);

        long id = UniqueIdGenerator.getUniqueId19();
        long ts = getTimestamp19FromId(id);
        assertEquals("Timestamp must be the stored future time after regression", fakeFuture, ts);
    }

    @Test
    public void testGetUniqueId19_SequenceOverflow_WaitsForNextMillisecond() throws Exception {
        long now = System.currentTimeMillis();

        setStaticField("lastTimeMillis19", now);
        setStaticField("count2", 9_999);
        setStaticField("lastGeneratedId19", now * 1_000_000 + 9_999 * 100 + getStaticField("serverId"));

        long id = UniqueIdGenerator.getUniqueId19();
        long ts = getTimestamp19FromId(id);
        assertTrue("Timestamp must have advanced after sequence overflow", ts > now);
        assertEquals("Sequence after overflow must be 0", 0, getSequence19FromId(id));
    }

    @Test
    public void testGetUniqueId19_IdNeverDecreasesEvenIfComputedValueIsLower() throws Exception {
        long highId = 9_999_999_999_999L;
        setStaticField("lastGeneratedId19", highId);
        setStaticField("lastTimeMillis19", System.currentTimeMillis());
        setStaticField("count2", 0);

        long id = UniqueIdGenerator.getUniqueId19();
        assertTrue("Generated ID19 must be greater than previously stored high ID", id > highId);
    }

    /* --------------------------------------------------------------------- *
     *  Validation of argument checks for Instant extraction methods         *
     * --------------------------------------------------------------------- */

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant_NegativeIdThrows() {
        UniqueIdGenerator.getInstant(-1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant19_NegativeIdThrows() {
        UniqueIdGenerator.getInstant19(-1L);
    }
}
