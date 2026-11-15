package com.cedarsoftware.util;

import org.junit.*;
import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;

import static org.junit.Assert.*;

public class UniqueIdGeneratorTest {

    /* --------------------------------------------------------------------- *
     *  Reflection helpers to manipulate the private static fields of the class *
     * --------------------------------------------------------------------- */
    private static void setStaticField(String fieldName, Object value) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, value);
    }

    private static long extractCountFromId(long id) {
        // (time * 100_000 + count * 100 + serverId)  -> count = (id % 100_000) / 100
        return (id % 100_000) / 100;
    }

    private static long extractCountFromId19(long id) {
        // (time * 1_000_000 + count2 * 100 + serverId) -> count2 = (id % 1_000_000) / 100
        return (id % 1_000_000) / 100;
    }

    /* --------------------------------------------------------------------- *
     *  Basic happy‑path tests                                               *
     * --------------------------------------------------------------------- */
    @Test
    public void testGetUniqueIdBasicIncreasing() {
        long first = UniqueIdGenerator.getUniqueId();
        long second = UniqueIdGenerator.getUniqueId();
        assertTrue("Second ID should be greater than first", second > first);
    }

    @Test
    public void testGetUniqueId19BasicIncreasing() {
        long first = UniqueIdGenerator.getUniqueId19();
        long second = UniqueIdGenerator.getUniqueId19();
        assertTrue("Second 19‑digit ID should be greater than first", second > first);
    }

    @Test
    public void testGetDateAccuracy() {
        long id = UniqueIdGenerator.getUniqueId();
        Date d = UniqueIdGenerator.getDate(id);
        long expectedMillis = id / 100_000;
        assertEquals("Date milliseconds derived from ID must match", expectedMillis, d.getTime());
    }

    @Test
    public void testGetDate19Accuracy() {
        long id = UniqueIdGenerator.getUniqueId19();
        Date d = UniqueIdGenerator.getDate19(id);
        long expectedMillis = id / 1_000_000;
        assertEquals("Date19 milliseconds derived from ID must match", expectedMillis, d.getTime());
    }

    /* --------------------------------------------------------------------- *
     *  Edge‑case tests                                                      *
     * --------------------------------------------------------------------- */
    @Test
    public void testGetUniqueIdCountRollover() throws Exception {
        // Force the internal counter to 999 so the next increment wraps to 0
        setStaticField("count", 999);
        long id = UniqueIdGenerator.getUniqueId();
        assertEquals("Count should have rolled over to 0", 0L, extractCountFromId(id));
    }

    @Test
    public void testGetUniqueId19CountRollover() throws Exception {
        // Force the internal counter to 9 999 so the next increment wraps to 0
        setStaticField("count2", 9_999);
        long id = UniqueIdGenerator.getUniqueId19();
        assertEquals("Count2 should have rolled over to 0", 0L, extractCountFromId19(id));
    }

    @Test
    public void testGetUniqueIdManyInSameMillisecond() throws Exception {
        // Freeze the “previousTimeMilliseconds” to the current time so the clock does not advance
        long now = System.currentTimeMillis();
        setStaticField("previousTimeMilliseconds", now);
        setStaticField("count", 0);

        // Generate several IDs without the system clock moving
        long[] ids = new long[5];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = UniqueIdGenerator.getUniqueId();
        }

        // Verify that the count part increments from 0‑4
        for (int i = 0; i < ids.length; i++) {
            assertEquals("Count part should be " + i, i, extractCountFromId(ids[i]));
        }
    }

    @Test
    public void testDuplicateAvoidanceGetUniqueId() throws Exception {
        // Create a fabricated ID that we will inject into the internal map
        long fakeId = UniqueIdGenerator.getUniqueId();
        // Access the private map via reflection
        Field mapField = UniqueIdGenerator.class.getDeclaredField("lastIds");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, Long> map = (Map<Long, Long>) mapField.get(null);
        map.put(fakeId, null);   // now the generator will think this ID was already used

        // Next call must return a *different* value
        long newId = UniqueIdGenerator.getUniqueId();
        assertNotEquals("Generator should skip the duplicated ID", fakeId, newId);
    }

    @Test
    public void testDuplicateAvoidanceGetUniqueId19() throws Exception {
        long fakeId = UniqueIdGenerator.getUniqueId19();
        Field mapField = UniqueIdGenerator.class.getDeclaredField("lastIdsFull");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, Long> map = (Map<Long, Long>) mapField.get(null);
        map.put(fakeId, null);

        long newId = UniqueIdGenerator.getUniqueId19();
        assertNotEquals("Generator19 should skip the duplicated ID", fakeId, newId);
    }

    @Test
    public void testThreadSafety() throws Exception {
        final int threads = 10;
        final int idsPerThread = 100;
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        Set<Long> results = new ConcurrentSkipListSet<>();

        Callable<Void> task = () -> {
            for (int i = 0; i < idsPerThread; i++) {
                results.add(UniqueIdGenerator.getUniqueId());
            }
            return null;
        };

        List<Callable<Void>> tasks = Collections.nCopies(threads, task);
        exec.invokeAll(tasks);
        exec.shutdownNow();

        assertEquals("All generated IDs must be unique",
                threads * idsPerThread, results.size());

        // Verify monotonic increase (the set is sorted, so just check each element > previous)
        Long previous = null;
        for (Long cur : results) {
            if (previous != null) {
                assertTrue("IDs must be strictly increasing", cur > previous);
            }
            previous = cur;
        }
    }
}
