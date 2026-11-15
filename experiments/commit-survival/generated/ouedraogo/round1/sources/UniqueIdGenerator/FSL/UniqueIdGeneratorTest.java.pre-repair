package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.Lock;

import org.junit.Before;
import org.junit.Test;

/**
 * JUnit‑4 test suite that exercises all public APIs of {@link UniqueIdGenerator}
 * as well as the internal state‑reset and duplicate‑avoidance logic.
 *
 * The tests use reflection to manipulate the private static fields
 * (counters, previous‑time stamps and the internal maps) because the class
 * does not expose any injection points.  This is acceptable for a unit‑test
 * that validates the contract of the class without altering its source.
 */
public class UniqueIdGeneratorTest {

    // -----------------------------------------------------------------------
    // Reflection helpers
    // -----------------------------------------------------------------------
    private static void setStaticField(String name, Object value) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getStaticField(String name) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(null);
    }

    // -----------------------------------------------------------------------
    // Reset static state before each test to guarantee isolation
    // -----------------------------------------------------------------------
    @Before
    public void resetState() throws Exception {
        // counters
        setStaticField("count", 0);
        setStaticField("count2", 0);
        // previous timestamps
        setStaticField("previousTimeMilliseconds", 0L);
        setStaticField("previousTimeMilliseconds2", 0L);
        // clear the duplicate‑detection maps
        ((Map<Long, Long>) getStaticField("lastIds")).clear();
        ((Map<Long, Long>) getStaticField("lastIdsFull")).clear();
        // ensure locks are unlocked (they are ReentrantLock, unlocking a non‑owner throws IllegalMonitorStateException)
        // but the class always unlocks in finally, so we just leave them as‑is.
    }

    // -----------------------------------------------------------------------
    // Tests for getUniqueId()
    // -----------------------------------------------------------------------
    @Test
    public void testGetUniqueIdIsStrictlyIncreasing() {
        long id1 = UniqueIdGenerator.getUniqueId();
        long id2 = UniqueIdGenerator.getUniqueId();
        long id3 = UniqueIdGenerator.getUniqueId();

        assertTrue("IDs must be strictly increasing", id1 < id2);
        assertTrue("IDs must be strictly increasing", id2 < id3);
    }

    @Test
    public void testGetUniqueIdCountResetsWhenMillisecondAdvances() throws Exception {
        // Simulate a previous timestamp that is older than the current time
        long now = System.currentTimeMillis();
        setStaticField("previousTimeMilliseconds", now - 10); // older
        setStaticField("count", 42); // arbitrary non‑zero

        long id = UniqueIdGenerator.getUniqueId();

        // Because the time advanced, the internal count should have been reset to 0.
        // The low‑order two‑digit part (count*100) must therefore be 0.
        long countPart = (id / 100) % 1000; // extract the three‑digit count (0‑999)
        assertEquals("Count should reset to 0 after millisecond advance", 0, countPart);
    }

    @Test
    public void testGetUniqueIdCountWrapsAtThousand() throws Exception {
        // Force the internal count to 999 and make the previous timestamp equal to now
        long now = System.currentTimeMillis();
        setStaticField("previousTimeMilliseconds", now);
        setStaticField("count", 999);

        // First call: count becomes 1000 -> wraps to 0, then count is 0 for the ID
        long idFirst = UniqueIdGenerator.getUniqueId();

        long countFirst = (idFirst / 100) % 1000;
        assertEquals("Count should wrap to 0 after reaching 999", 0, countFirst);

        // Second call: count becomes 1
        long idSecond = UniqueIdGenerator.getUniqueId();
        long countSecond = (idSecond / 100) % 1000;
        assertEquals("Count should be 1 after wrap", 1, countSecond);
    }

    @Test
    public void testGetUniqueIdAvoidsDuplicateIds() throws Exception {
        // Generate a legitimate ID
        long duplicate = UniqueIdGenerator.getUniqueId();

        // Insert the same ID manually into the duplicate‑check map
        @SuppressWarnings("unchecked")
        Map<Long, Long> lastIds = (Map<Long, Long>) getStaticField("lastIds");
        lastIds.put(duplicate, null);

        // The next call must produce a different value (the while‑loop in getUniqueId will retry)
        long next = UniqueIdGenerator.getUniqueId();
        assertNotEquals("Generator must skip an ID that already exists in the map", duplicate, next);
    }

    // -----------------------------------------------------------------------
    // Tests for getUniqueId19()
    // -----------------------------------------------------------------------
    @Test
    public void testGetUniqueId19IsStrictlyIncreasing() {
        long id1 = UniqueIdGenerator.getUniqueId19();
        long id2 = UniqueIdGenerator.getUniqueId19();
        long id3 = UniqueIdGenerator.getUniqueId19();

        assertTrue(id1 < id2);
        assertTrue(id2 < id3);
    }

    @Test
    public void testGetUniqueId19CountWrapsAtTenThousand() throws Exception {
        long now = System.currentTimeMillis();
        setStaticField("previousTimeMilliseconds2", now);
        setStaticField("count2", 9_999);

        long first = UniqueIdGenerator.getUniqueId19();
        long countFirst = (first / 100) % 10_000;
        assertEquals("Count should wrap to 0 after reaching 9,999", 0, countFirst);

        long second = UniqueIdGenerator.getUniqueId19();
        long countSecond = (second / 100) % 10_000;
        assertEquals("Count should be 1 after wrap", 1, countSecond);
    }

    @Test
    public void testGetUniqueId19AvoidsDuplicateIds() throws Exception {
        long dup = UniqueIdGenerator.getUniqueId19();

        @SuppressWarnings("unchecked")
        Map<Long, Long> lastFull = (Map<Long, Long>) getStaticField("lastIdsFull");
        lastFull.put(dup, null);

        long next = UniqueIdGenerator.getUniqueId19();
        assertNotEquals("Generator must skip an ID that already exists in the full map", dup, next);
    }

    // -----------------------------------------------------------------------
    // Tests for date extraction helpers
    // -----------------------------------------------------------------------
    @Test
    public void testGetDateExtractsCorrectMilliseconds() {
        long id = UniqueIdGenerator.getUniqueId();
        Date extracted = UniqueIdGenerator.getDate(id);
        long expectedMillis = id / 100_000L;
        assertEquals("Date milliseconds should match ID division", expectedMillis, extracted.getTime());
    }

    @Test
    public void testGetDate19ExtractsCorrectMilliseconds() {
        long id = UniqueIdGenerator.getUniqueId19();
        Date extracted = UniqueIdGenerator.getDate19(id);
        long expectedMillis = id / 1_000_000L;
        assertEquals("Date19 milliseconds should match ID division", expectedMillis, extracted.getTime());
    }

    // -----------------------------------------------------------------------
    // Additional sanity checks (serverId is part of the low‑order digits)
    // -----------------------------------------------------------------------
    @Test
    public void testServerIdIsEmbeddedInGeneratedIds() throws Exception {
        // The serverId is stored in the static final field "serverId"
        int serverId = getStaticField("serverId");
        long id = UniqueIdGenerator.getUniqueId();
        long id19 = UniqueIdGenerator.getUniqueId19();

        // Both IDs end with the serverId (mod 100)
        assertEquals("Low‑order two digits of getUniqueId must equal serverId",
                serverId, (int) (id % 100));
        assertEquals("Low‑order two digits of getUniqueId19 must equal serverId",
                serverId, (int) (id19 % 100));
    }
}
