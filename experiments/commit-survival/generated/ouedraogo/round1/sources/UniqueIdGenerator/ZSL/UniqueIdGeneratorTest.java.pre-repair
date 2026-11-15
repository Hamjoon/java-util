package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link UniqueIdGenerator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct range of the internally generated {@code serverId}</li>
 *   <li>Strictly increasing IDs for both 18‑digit and 19‑digit generators</li>
 *   <li>Uniqueness of a large batch of IDs (exceeds the per‑millisecond count)</li>
 *   <li>Proper reset of the internal counters when they reach their limits</li>
 *   <li>Correct conversion of IDs back to {@link Date} values</li>
 * </ul>
 *
 * The class under test uses only static state, therefore the test suite manipulates that
 * state via reflection to simulate edge conditions (counter wrap‑around) without needing
 * to wait for real time to pass.
 */
public class UniqueIdGeneratorTest {

    /* --------------------------------------------------------------------- *
     *  Helper methods for reflective access to the private static fields   *
     * --------------------------------------------------------------------- */

    private static Object getStaticField(String name) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    private static void setStaticField(String name, Object value) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    /* --------------------------------------------------------------------- *
     *  Basic sanity check for the server identifier (00‑99)                *
     * --------------------------------------------------------------------- */

    @Test
    public void testServerIdIsWithinZeroToNinetyNine() throws Exception {
        int serverId = (int) getStaticField("serverId");
        assertTrue("serverId should be between 0 and 99 inclusive", serverId >= 0 && serverId < 100);
    }

    /* --------------------------------------------------------------------- *
     *  getUniqueId() – monotonic increase and uniqueness                  *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetUniqueIdIncreasesMonotonically() {
        long previous = UniqueIdGenerator.getUniqueId();
        for (int i = 0; i < 20; i++) {
            long current = UniqueIdGenerator.getUniqueId();
            assertTrue("IDs must be strictly increasing", current > previous);
            previous = current;
        }
    }

    @Test
    public void testGetUniqueIdProducesNoDuplicatesInLargeBatch() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 2_000; i++) {
            long id = UniqueIdGenerator.getUniqueId();
            assertTrue("Duplicate ID detected", ids.add(id));
        }
        assertEquals("All generated IDs should be unique", 2_000, ids.size());
    }

    @Test
    public void testGetUniqueIdCounterWrapAround() throws Exception {
        // Force the internal counter to be just before the wrap‑around point.
        setStaticField("count", 999);
        // Make sure the time‑stamp comparison does NOT reset the counter.
        long future = System.currentTimeMillis() + 10_000L;
        setStaticField("previousTimeMilliseconds", future);

        long id = UniqueIdGenerator.getUniqueId();

        // The count part of the ID is the middle three digits (count * 100)
        long countPart = (id % 100_000) / 100L;
        assertEquals("Counter should have wrapped to 0", 0L, countPart);
    }

    /* --------------------------------------------------------------------- *
     *  getUniqueId19() – monotonic increase and uniqueness                 *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetUniqueId19IncreasesMonotonically() {
        long previous = UniqueIdGenerator.getUniqueId19();
        for (int i = 0; i < 20; i++) {
            long current = UniqueIdGenerator.getUniqueId19();
            assertTrue("IDs must be strictly increasing", current > previous);
            previous = current;
        }
    }

    @Test
    public void testGetUniqueId19ProducesNoDuplicatesInLargeBatch() {
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 20_000; i++) {
            long id = UniqueIdGenerator.getUniqueId19();
            assertTrue("Duplicate ID detected", ids.add(id));
        }
        assertEquals("All generated IDs should be unique", 20_000, ids.size());
    }

    @Test
    public void testGetUniqueId19CounterWrapAround() throws Exception {
        // Set the 4‑digit counter just before its limit.
        setStaticField("count2", 9_999);
        // Prevent time‑based reset.
        long future = System.currentTimeMillis() + 10_000L;
        setStaticField("previousTimeMilliseconds2", future);

        long id = UniqueIdGenerator.getUniqueId19();

        // Extract the 4‑digit count: (id % 1_000_000) / 100
        long countPart = (id % 1_000_000) / 100L;
        assertEquals("Counter should have wrapped to 0", 0L, countPart);
    }

    /* --------------------------------------------------------------------- *
     *  Date conversion helpers                                            *
     * --------------------------------------------------------------------- */

    @Test
    public void testGetDateMatchesTimestampOfUniqueId() {
        long uid = UniqueIdGenerator.getUniqueId();
        Date expected = new Date(uid / 100_000);
        Date actual = UniqueIdGenerator.getDate(uid);
        assertEquals("Date derived from ID should match expected value", expected, actual);
    }

    @Test
    public void testGetDate19MatchesTimestampOfUniqueId19() {
        long uid = UniqueIdGenerator.getUniqueId19();
        Date expected = new Date(uid / 1_000_000);
        Date actual = UniqueIdGenerator.getDate19(uid);
        assertEquals("Date19 derived from ID should match expected value", expected, actual);
    }

    /* --------------------------------------------------------------------- *
     *  Sanity check that the static maps never exceed their size limits   *
     * --------------------------------------------------------------------- */

    @Test
    public void testLastIdsMapDoesNotExceedCapacity() throws Exception {
        // Generate more than 1,000 IDs; the map should automatically evict the eldest.
        for (int i = 0; i < 1_200; i++) {
            UniqueIdGenerator.getUniqueId();
        }
        @SuppressWarnings("unchecked")
        java.util.Map<Long, Long> map = (java.util.Map<Long, Long>) getStaticField("lastIds");
        assertTrue("lastIds map should have at most 1,000 entries", map.size() <= 1_000);
    }

    @Test
    public void testLastIdsFullMapDoesNotExceedCapacity() throws Exception {
        // Generate more than 10,000 IDs; the map should automatically evict the eldest.
        for (int i = 0; i < 10_500; i++) {
            UniqueIdGenerator.getUniqueId19();
        }
        @SuppressWarnings("unchecked")
        java.util.Map<Long, Long> map = (java.util.Map<Long, Long>) getStaticField("lastIdsFull");
        assertTrue("lastIdsFull map should have at most 10,000 entries", map.size() <= 10_000);
    }
}
