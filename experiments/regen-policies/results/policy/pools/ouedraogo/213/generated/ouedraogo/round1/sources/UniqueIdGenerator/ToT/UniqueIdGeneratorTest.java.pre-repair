package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link UniqueIdGenerator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct structure of the generated IDs (timestamp, sequence, serverId)</li>
 *   <li>Monotonic increase of IDs across successive calls</li>
 *   <li>Behaviour when the per‑millisecond sequence counter overflows</li>
 *   <li>Correct extraction of {@link Date} and {@link Instant} from the IDs</li>
 *   <li>Validation of error handling for negative IDs</li>
 * </ul>
 *
 * No external mocking frameworks are used – only reflection to reset the
 * internal static state between tests. All interactions are with the public
 * API of {@link UniqueIdGenerator}.
 */
public class UniqueIdGeneratorTest {

    // -------------------------------------------------------------------------
    // Reflection helpers – used to reset the static state before each test
    // -------------------------------------------------------------------------
    private static void setStaticField(String name, Object value) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    private static long getStaticFieldLong(String name) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getLong(null);
    }

    private static int getStaticFieldInt(String name) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(null);
    }

    private static int getServerId() throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField("serverId");
        f.setAccessible(true);
        return f.getInt(null);
    }

    // -------------------------------------------------------------------------
    // Reset static state before each test to guarantee independence
    // -------------------------------------------------------------------------
    @Before
    public void resetState() throws Exception {
        setStaticField("count", 0);
        setStaticField("count2", 0);
        setStaticField("lastTimeMillis", 0L);
        setStaticField("lastTimeMillis19", 0L);
        setStaticField("lastGeneratedId", 0L);
        setStaticField("lastGeneratedId19", 0L);
    }

    @After
    public void cleanUp() throws Exception {
        // Ensure the same reset after a test in case a test fails before @After
        resetState();
    }

    // -------------------------------------------------------------------------
    // 1. Basic structure & monotonicity of getUniqueId()
    // -------------------------------------------------------------------------
    @Test
    public void testUniqueIdStructureAndMonotonicity() throws Exception {
        long id1 = UniqueIdGenerator.getUniqueId();
        Thread.sleep(1); // guarantee next millisecond or at least a different timestamp
        long id2 = UniqueIdGenerator.getUniqueId();

        assertTrue("Second ID must be greater than the first", id2 > id1);

        int serverId = getServerId();

        // Verify serverId occupies the last two digits
        assertEquals("Server ID should be encoded in the last two digits", serverId, (int) (id2 % 100));

        // Verify timestamp component (id / 100_000) is close to the current system time
        long timestampPart = id2 / 100_000;
        long now = System.currentTimeMillis();
        assertTrue("Timestamp part should be within 5 ms of the current time",
                Math.abs(now - timestampPart) <= 5);
    }

    // -------------------------------------------------------------------------
    // 2. Sequence overflow handling for getUniqueId()
    // -------------------------------------------------------------------------
    @Test
    public void testUniqueIdSequenceOverflowCausesNextMillisecond() throws Exception {
        // Simulate that we are already at the 999th sequence of the current millisecond
        long current = System.currentTimeMillis();
        setStaticField("lastTimeMillis", current);
        setStaticField("count", 999); // next call will increment to 1000 and trigger wait

        long id = UniqueIdGenerator.getUniqueId();

        // The timestamp part must be greater than the original simulated millisecond
        long timestampPart = id / 100_000;
        assertTrue("Timestamp should have advanced after sequence overflow", timestampPart > current);

        // After overflow the sequence counter must have been reset to 0 (implicit via next call)
        int countAfter = getStaticFieldInt("count");
        assertEquals("Sequence counter must be reset after overflow", 0, countAfter);
    }

    // -------------------------------------------------------------------------
    // 3. Basic structure & monotonicity of getUniqueId19()
    // -------------------------------------------------------------------------
    @Test
    public void testUniqueId19StructureAndMonotonicity() throws Exception {
        long id1 = UniqueIdGenerator.getUniqueId19();
        Thread.sleep(1);
        long id2 = UniqueIdGenerator.getUniqueId19();

        assertTrue("Second 19‑digit ID must be greater than the first", id2 > id1);

        int serverId = getServerId();

        // Verify serverId occupies the last two digits
        assertEquals("Server ID should be encoded in the last two digits (19‑digit)", serverId,
                (int) (id2 % 100));

        // Verify timestamp component (id / 1_000_000) is close to the current system time
        long timestampPart = id2 / 1_000_000;
        long now = System.currentTimeMillis();
        assertTrue("Timestamp part should be within 5 ms of the current time (19‑digit)",
                Math.abs(now - timestampPart) <= 5);
    }

    // -------------------------------------------------------------------------
    // 4. Sequence overflow handling for getUniqueId19()
    // -------------------------------------------------------------------------
    @Test
    public void testUniqueId19SequenceOverflowCausesNextMillisecond() throws Exception {
        long current = System.currentTimeMillis();
        setStaticField("lastTimeMillis19", current);
        setStaticField("count2", 9_999); // next call will increment to 10_000 and trigger wait

        long id = UniqueIdGenerator.getUniqueId19();

        long timestampPart = id / 1_000_000;
        assertTrue("Timestamp should have advanced after 10,000‑ID overflow", timestampPart > current);

        int countAfter = getStaticFieldInt("count2");
        assertEquals("Sequence counter (19) must be reset after overflow", 0, countAfter);
    }

    // -------------------------------------------------------------------------
    // 5. Extraction of Date and Instant from getUniqueId()
    // -------------------------------------------------------------------------
    @Test
    public void testGetDateAndInstantFromUniqueId() {
        long id = UniqueIdGenerator.getUniqueId();

        Date date = UniqueIdGenerator.getDate(id);
        Instant instant = UniqueIdGenerator.getInstant(id);

        long expectedMillis = id / 100_000;
        assertEquals("Date epoch millis must match the timestamp part of the ID",
                expectedMillis, date.getTime());
        assertEquals("Instant epoch millis must match the timestamp part of the ID",
                expectedMillis, instant.toEpochMilli());
    }

    // -------------------------------------------------------------------------
    // 6. Extraction of Date and Instant from getUniqueId19()
    // -------------------------------------------------------------------------
    @Test
    public void testGetDateAndInstantFromUniqueId19() {
        long id = UniqueIdGenerator.getUniqueId19();

        Date date = UniqueIdGenerator.getDate19(id);
        Instant instant = UniqueIdGenerator.getInstant19(id);

        long expectedMillis = id / 1_000_000;
        assertEquals("Date19 epoch millis must match the timestamp part of the ID",
                expectedMillis, date.getTime());
        assertEquals("Instant19 epoch millis must match the timestamp part of the ID",
                expectedMillis, instant.toEpochMilli());
    }

    // -------------------------------------------------------------------------
    // 7. IllegalArgumentException for negative IDs (Instant APIs)
    // -------------------------------------------------------------------------
    @Test(expected = IllegalArgumentException.class)
    public void testGetInstantRejectsNegativeId() {
        UniqueIdGenerator.getInstant(-1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant19RejectsNegativeId() {
        UniqueIdGenerator.getInstant19(-123L);
    }

    // -------------------------------------------------------------------------
    // 8. Verify that IDs are strictly increasing even when generated rapidly
    // -------------------------------------------------------------------------
    @Test
    public void testMonotonicIncreaseUnderRapidCalls() {
        long previous = -1L;
        // Generate a batch of IDs; the loop is deliberately tight to stress the generator
        for (int i = 0; i < 5_000; i++) {
            long cur = UniqueIdGenerator.getUniqueId();
            assertTrue("Each generated ID must be greater than the previous one", cur > previous);
            previous = cur;
        }
    }

    // -------------------------------------------------------------------------
    // 9. Verify that getUniqueId19 also remains monotonic under rapid calls
    // -------------------------------------------------------------------------
    @Test
    public void testMonotonicIncreaseUnderRapidCalls19() {
        long previous = -1L;
        for (int i = 0; i < 20_000; i++) {
            long cur = UniqueIdGenerator.getUniqueId19();
            assertTrue("Each generated 19‑digit ID must be greater than the previous one", cur > previous);
            previous = cur;
        }
    }
}
