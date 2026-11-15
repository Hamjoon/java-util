/***************************************************************************************************
 * JUnit‑4 test suite for {@link com.cedarsoftware.util.UniqueIdGenerator}.
 *
 * The tests cover:
 *   • Correct structure of the generated IDs (timestamp, sequence, serverId)
 *   • Monotonic increase guarantees
 *   • Proper handling of clock regression
 *   • Sequence overflow handling (wait for next millisecond)
 *   • Fallback when the calculated ID would not be larger than the previous one
 *   • Extraction helpers {@code getDate}, {@code getDate19}, {@code getInstant},
 *     {@code getInstant19}
 *   • Validation of the serverId range (0‑99)
 *
 * The test class lives in the same package as the class under test, allowing direct access to
 * the private static fields via reflection.  Each test restores the static state to a clean
 * baseline in {@code @Before} so that tests are independent.
 *
 * Note: The real environment‑dependent logic that determines the serverId is exercised only
 * indirectly – we merely assert that the resolved value is within the expected range.
 *
 * @author  OpenAI ChatGPT – generated for the exercise
 **************************************************************************************************/
package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class UniqueIdGeneratorTest {

    /* --------------------------------------------------------------------- *
     *  Reflection helpers – allow us to reset the static state before each *
     *  test.  All fields are private static, so we need to make them       *
     *  accessible.                                                          *
     * --------------------------------------------------------------------- */
    private static void setStaticInt(String name, int value) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(null, value);
    }

    private static void setStaticLong(String name, long value) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setLong(null, value);
    }

    private static int getStaticInt(String name) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(null);
    }

    private static long getStaticLong(String name) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getLong(null);
    }

    private static int getServerId() throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField("serverId");
        f.setAccessible(true);
        return f.getInt(null);
    }

    @Before
    public void resetGeneratorState() throws Exception {
        // Reset all mutable static fields to their initial values.
        setStaticInt("count", 0);
        setStaticInt("count2", 0);
        setStaticLong("lastTimeMillis", 0L);
        setStaticLong("lastTimeMillis19", 0L);
        setStaticLong("lastGeneratedId", 0L);
        setStaticLong("lastGeneratedId19", 0L);
    }

    /* --------------------------------------------------------------------- *
     *  1. Basic structural and monotonic tests for getUniqueId()            *
     * --------------------------------------------------------------------- */
    @Test
    public void testUniqueIdStructureAndMonotonicity() {
        long id1 = UniqueIdGenerator.getUniqueId();
        long id2 = UniqueIdGenerator.getUniqueId();

        assertTrue("IDs must be strictly increasing", id2 > id1);

        int serverId = getServerId();

        // ---- serverId (last two digits) ----
        assertEquals("ServerId should occupy the last two digits", serverId, (int) (id2 % 100));

        // ---- timestamp part ----
        long ts1 = id1 / 100_000L;
        long ts2 = id2 / 100_000L;
        long now = System.currentTimeMillis();

        // Timestamp should be close to the current time (allow a small drift)
        assertTrue("Timestamp should not be in the future", ts1 <= now);
        assertTrue("Timestamp should not be in the future", ts2 <= now);
        assertTrue("Timestamp should be non‑negative", ts1 >= 0);
        assertTrue("Timestamp should be non‑negative", ts2 >= 0);

        // ---- sequence part (0‑999) ----
        long seq1 = (id1 / 100L) % 1_000L;
        long seq2 = (id2 / 100L) % 1_000L;

        // If both IDs were generated within the same millisecond, the sequence must increase.
        if (ts1 == ts2) {
            assertEquals("Sequence should increment by 1 within the same millisecond", seq1 + 1, seq2);
        } else {
            // otherwise the sequence resets to 0 for the new millisecond
            assertEquals("Sequence should start at 0 for a new millisecond", 0, seq2);
        }
    }

    /* --------------------------------------------------------------------- *
     *  2. Basic structural and monotonic tests for getUniqueId19()          *
     * --------------------------------------------------------------------- */
    @Test
    public void testUniqueId19StructureAndMonotonicity() {
        long id1 = UniqueIdGenerator.getUniqueId19();
        long id2 = UniqueIdGenerator.getUniqueId19();

        assertTrue("IDs must be strictly increasing", id2 > id1);

        int serverId = getServerId();

        // ---- serverId (last two digits) ----
        assertEquals("ServerId should occupy the last two digits", serverId, (int) (id2 % 100));

        // ---- timestamp part (13 digits) ----
        long ts1 = id1 / 1_000_000L;
        long ts2 = id2 / 1_000_000L;
        long now = System.currentTimeMillis();

        assertTrue("Timestamp should not be in the future", ts1 <= now);
        assertTrue("Timestamp should not be in the future", ts2 <= now);
        assertTrue("Timestamp should be non‑negative", ts1 >= 0);
        assertTrue("Timestamp should be non‑negative", ts2 >= 0);

        // ---- sequence part (0‑9999) ----
        long seq1 = (id1 / 100L) % 10_000L;
        long seq2 = (id2 / 100L) % 10_000L;

        if (ts1 == ts2) {
            assertEquals("Sequence should increment by 1 within the same millisecond", seq1 + 1, seq2);
        } else {
            assertEquals("Sequence should start at 0 for a new millisecond", 0, seq2);
        }
    }

    /* --------------------------------------------------------------------- *
     *  3. Clock regression handling – when the system clock moves backwards *
     * --------------------------------------------------------------------- */
    @Test
    public void testClockRegressionIsHandledGracefully() throws Exception {
        long future = System.currentTimeMillis() + 200L; // 200 ms ahead of real time
        setStaticLong("lastTimeMillis", future);

        long id = UniqueIdGenerator.getUniqueId();

        long extractedTs = id / 100_000L;
        assertEquals("When clock goes backwards, generator must use lastTimeMillis", future, extractedTs);
    }

    @Test
    public void testClockRegressionIsHandledGracefullyForId19() throws Exception {
        long future = System.currentTimeMillis() + 200L;
        setStaticLong("lastTimeMillis19", future);

        long id = UniqueIdGenerator.getUniqueId19();

        long extractedTs = id / 1_000_000L;
        assertEquals("When clock goes backwards, generator must use lastTimeMillis19", future, extractedTs);
    }

    /* --------------------------------------------------------------------- *
     *  4. Sequence overflow – the generator should wait for the next millisecond *
     * --------------------------------------------------------------------- */
    @Test
    public void testSequenceOverflowWaitsForNextMillisecond() throws Exception {
        long now = System.currentTimeMillis();

        // Simulate that we are already at the limit for the current millisecond.
        setStaticLong("lastTimeMillis", now);
        setStaticInt("count", 999); // next call will increment to 1000 and trigger wait

        long id = UniqueIdGenerator.getUniqueId();

        long ts = id / 100_000L;
        assertTrue("Timestamp must have advanced after sequence overflow", ts > now);
        // After overflow the sequence must have been reset to 0
        long seq = (id / 100L) % 1_000L;
        assertEquals("Sequence must reset to 0 after waiting for next millisecond", 0, seq);
    }

    @Test
    public void testSequenceOverflowWaitsForNextMillisecondForId19() throws Exception {
        long now = System.currentTimeMillis();

        setStaticLong("lastTimeMillis19", now);
        setStaticInt("count2", 9_999); // next call will overflow

        long id = UniqueIdGenerator.getUniqueId19();

        long ts = id / 1_000_000L;
        assertTrue("Timestamp must have advanced after sequence overflow (19‑digit)", ts > now);
        long seq = (id / 100L) % 10_000L;
        assertEquals("Sequence must reset to 0 after overflow for 19‑digit IDs", 0, seq);
    }

    /* --------------------------------------------------------------------- *
     *  5. Monotonic adjustment when calculated ID would not be larger than   *
     *     the previous one.                                                  *
     * --------------------------------------------------------------------- */
    @Test
    public void testMonotonicAdjustmentWhenIdWouldNotIncrease() throws Exception {
        // Force a very large previous ID.
        long hugePrev = Long.MAX_VALUE - 10;
        setStaticLong("lastGeneratedId", hugePrev);
        // Ensure the timestamp part is *smaller* than that previous value.
        long past = System.currentTimeMillis() - 10_000L; // 10 seconds ago
        setStaticLong("lastTimeMillis", past);
        setStaticInt("count", 0);

        long id = UniqueIdGenerator.getUniqueId();

        assertTrue("Returned ID must be greater than the previous one", id > hugePrev);
        assertEquals("Generator must have incremented by exactly 1", hugePrev + 1, id);
    }

    @Test
    public void testMonotonicAdjustmentWhenId19WouldNotIncrease() throws Exception {
        long hugePrev = Long.MAX_VALUE - 10;
        setStaticLong("lastGeneratedId19", hugePrev);
        long past = System.currentTimeMillis() - 10_000L;
        setStaticLong("lastTimeMillis19", past);
        setStaticInt("count2", 0);

        long id = UniqueIdGenerator.getUniqueId19();

        assertTrue("Returned ID must be greater than the previous one", id > hugePrev);
        assertEquals("Generator must have incremented by exactly 1", hugePrev + 1, id);
    }

    /* --------------------------------------------------------------------- *
     *  6. Date/Instant extraction helpers                                    *
     * --------------------------------------------------------------------- */
    @Test
    public void testGetDateAndInstantFromUniqueId() {
        long id = UniqueIdGenerator.getUniqueId();

        Date date = UniqueIdGenerator.getDate(id);
        Instant instant = UniqueIdGenerator.getInstant(id);

        long expectedMillis = id / 100_000L;
        assertEquals("Date should represent the timestamp part of the ID", expectedMillis, date.getTime());
        assertEquals("Instant should represent the timestamp part of the ID", expectedMillis, instant.toEpochMilli());
    }

    @Test
    public void testGetDateAndInstantFromUniqueId19() {
        long id = UniqueIdGenerator.getUniqueId19();

        Date date = UniqueIdGenerator.getDate19(id);
        Instant instant = UniqueIdGenerator.getInstant19(id);

        long expectedMillis = id / 1_000_000L;
        assertEquals("Date19 should represent the timestamp part of the ID", expectedMillis, date.getTime());
        assertEquals("Instant19 should represent the timestamp part of the ID", expectedMillis, instant.toEpochMilli());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstantRejectsNegativeId() {
        UniqueIdGenerator.getInstant(-1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant19RejectsNegativeId() {
        UniqueIdGenerator.getInstant19(-1L);
    }

    /* --------------------------------------------------------------------- *
     *  7. ServerId sanity check – must be in range 0‑99                     *
     * --------------------------------------------------------------------- */
    @Test
    public void testServerIdIsWithinValidRange() throws Exception {
        int serverId = getServerId();
        assertTrue("ServerId must be >= 0", serverId >= 0);
        assertTrue("ServerId must be < 100", serverId < 100);
    }
}
