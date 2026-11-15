package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link UniqueIdGenerator}.
 *
 * The tests cover:
 * <ul>
 *   <li>Correct structure of IDs produced by {@code getUniqueId()} and {@code getUniqueId19()}.</li>
 *   <li>Strictly increasing order of generated IDs.</li>
 *   <li>Proper handling of the internal counters (reset on millisecond change, wrap‑around at max).</li>
 *   <li>Correct date extraction via {@code getDate(...)} and {@code getDate19(...)}.</li>
 *   <li>Size limits of the internal {@code lastIds} and {@code lastIdsFull} caches.</li>
 *   <li>Thread‑safety – concurrent generation yields a monotonic, duplicate‑free sequence.</li>
 * </ul>
 *
 * The test class uses reflection to manipulate the private static fields of
 * {@link UniqueIdGenerator} so that edge‑cases (counter wrap, millisecond rollover)
 * can be exercised without relying on time‑travel or mocking frameworks.
 */
public class UniqueIdGeneratorTest {

    /* --------------------------------------------------------------------- */
    /*  Reflection helpers – used to read/write private static members.     */
    /* --------------------------------------------------------------------- */

    private static void setStaticField(String fieldName, Object value) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getStaticField(String fieldName) throws Exception {
        Field f = UniqueIdGenerator.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (T) f.get(null);
    }

    /* --------------------------------------------------------------------- */
    /*  Test lifecycle – reset static state after every test to avoid bleed‑ */
    /*  over between test methods.                                          */
    /* --------------------------------------------------------------------- */

    @Before
    public void resetCounters() throws Exception {
        setStaticField("count", 0);
        setStaticField("count2", 0);
        setStaticField("previousTimeMilliseconds", 0L);
        setStaticField("previousTimeMilliseconds2", 0L);
        // clear the caches
        ((Map<?, ?>) getStaticField("lastIds")).clear();
        ((Map<?, ?>) getStaticField("lastIdsFull")).clear();
    }

    @After
    public void cleanUp() throws Exception {
        resetCounters();
    }

    /* --------------------------------------------------------------------- */
    /*  1. Basic structure & uniqueness of getUniqueId()                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetUniqueIdStructureAndMonotonicity() {
        long previous = -1L;
        int serverId = (int) (UniqueIdGenerator.getUniqueId() % 100); // extract once for reference
        // generate a sequence
        for (int i = 0; i < 20; i++) {
            long id = UniqueIdGenerator.getUniqueId();
            // 1) strictly increasing
            assertTrue("IDs must be strictly increasing", id > previous);
            previous = id;

            // 2) last two digits are the server id (0‑99)
            assertEquals("Server id must occupy the last two digits", serverId, (int) (id % 100));

            // 3) the count part (the three digits before server id) is between 0‑999
            long countPart = (id % 100_000) / 100;
            assertTrue("Count part must be in [0,999]", countPart >= 0 && countPart <= 999);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  2. Basic structure & uniqueness of getUniqueId19()                  */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetUniqueId19StructureAndMonotonicity() {
        long previous = -1L;
        int serverId = (int) (UniqueIdGenerator.getUniqueId19() % 100);
        for (int i = 0; i < 30; i++) {
            long id = UniqueIdGenerator.getUniqueId19();
            assertTrue("IDs must be strictly increasing", id > previous);
            previous = id;

            assertEquals("Server id must occupy the last two digits", serverId, (int) (id % 100));

            long countPart = (id % 1_000_000) / 100;
            assertTrue("Count part must be in [0,9999]", countPart >= 0 && countPart <= 9_999);
        }
    }

    /* --------------------------------------------------------------------- */
    /*  3. Date extraction – getDate()                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetDateMatchesTimestamp() {
        long id = UniqueIdGenerator.getUniqueId();
        Date extracted = UniqueIdGenerator.getDate(id);
        long expectedMillis = id / 100_000L;
        assertEquals("Date milliseconds must match the ID's time component",
                expectedMillis, extracted.getTime());
    }

    /* --------------------------------------------------------------------- */
    /*  4. Date extraction – getDate19()                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGetDate19MatchesTimestamp() {
        long id = UniqueIdGenerator.getUniqueId19();
        Date extracted = UniqueIdGenerator.getDate19(id);
        long expectedMillis = id / 1_000_000L;
        assertEquals("Date milliseconds must match the 19‑digit ID's time component",
                expectedMillis, extracted.getTime());
    }

    /* --------------------------------------------------------------------- */
    /*  5. Counter reset when the millisecond advances (getUniqueId)        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCountResetOnMillisecondAdvance() throws Exception {
        // force a non‑zero count and a stale previous timestamp
        setStaticField("count", 555);
        setStaticField("previousTimeMilliseconds", System.currentTimeMillis() - 10L);

        long id = UniqueIdGenerator.getUniqueId(); // this call should reset count to 0
        long countPart = (id % 100_000) / 100;
        assertEquals("When the millisecond advances, count must reset to 0", 0, countPart);
    }

    /* --------------------------------------------------------------------- */
    /*  6. Counter wrap‑around at 1000 (getUniqueId)                        */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCountWrapAround() throws Exception {
        // bring the counter to 999 and keep the timestamp unchanged
        setStaticField("count", 999);
        long now = System.currentTimeMillis();
        setStaticField("previousTimeMilliseconds", now);

        // first call -> count 999
        long first = UniqueIdGenerator.getUniqueId();
        long firstCount = (first % 100_000) / 100;
        assertEquals("First generated count should be 999", 999, firstCount);

        // second call -> count wraps to 0 (still same millisecond)
        long second = UniqueIdGenerator.getUniqueId();
        long secondCount = (second % 100_000) / 100;
        assertEquals("Counter must wrap to 0 after 999", 0, secondCount);
    }

    /* --------------------------------------------------------------------- */
    /*  7. Counter wrap‑around at 10 000 (getUniqueId19)                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCount2WrapAround() throws Exception {
        setStaticField("count2", 9_999);
        long now = System.currentTimeMillis();
        setStaticField("previousTimeMilliseconds2", now);

        long first = UniqueIdGenerator.getUniqueId19();
        long firstCount = (first % 1_000_000) / 100;
        assertEquals("First generated count2 should be 9999", 9_999, firstCount);

        long second = UniqueIdGenerator.getUniqueId19();
        long secondCount = (second % 1_000_000) / 100;
        assertEquals("Counter2 must wrap to 0 after 9999", 0, secondCount);
    }

    /* --------------------------------------------------------------------- */
    /*  8. Cache size limits (lastIds and lastIdsFull)                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void testLastIdsCacheSizeLimit() throws Exception {
        // generate 1 100 IDs – cache should hold only the most recent 1 000
        for (int i = 0; i < 1_100; i++) {
            UniqueIdGenerator.getUniqueId();
        }
        Map<Long, Long> map = getStaticField("lastIds");
        assertEquals("lastIds cache must evict eldest entries, keeping 1000", 1_000, map.size());
    }

    @Test
    public void testLastIdsFullCacheSizeLimit() throws Exception {
        // generate 10 200 IDs – cache should hold only the most recent 10 000
        for (int i = 0; i < 10_200; i++) {
            UniqueIdGenerator.getUniqueId19();
        }
        Map<Long, Long> map = getStaticField("lastIdsFull");
        assertEquals("lastIdsFull cache must evict eldest entries, keeping 10000", 10_000, map.size());
    }

    /* --------------------------------------------------------------------- */
    /*  9. Concurrency – ensure monotonic increasing order under load       */
    /* --------------------------------------------------------------------- */

    @Test
    public void testConcurrentGenerationIsMonotonicAndUnique() throws Exception {
        final int threads = 8;
        final int idsPerThread = 250; // total 2000 IDs
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threads);
        final List<Long> allIds = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    startLatch.await(); // ensure all threads start together
                    for (int i = 0; i < idsPerThread; i++) {
                        allIds.add(UniqueIdGenerator.getUniqueId());
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startLatch.countDown(); // release threads
        doneLatch.await(); // wait for completion

        // verify total count
        assertEquals("All IDs must have been collected", threads * idsPerThread, allIds.size());

        // verify strict increasing order after sorting
        List<Long> sorted = new ArrayList<>(allIds);
        Collections.sort(sorted);
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue("Sorted IDs must be strictly increasing", sorted.get(i) > sorted.get(i - 1));
        }

        // verify no duplicates
        for (int i = 1; i < sorted.size(); i++) {
            assertNotEquals("No duplicate IDs should appear", sorted.get(i - 1), sorted.get(i));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  10. Defensive check – generated IDs are never negative (pre‑2286)   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testGeneratedIdsAreNonNegative() {
        for (int i = 0; i < 50; i++) {
            assertTrue("getUniqueId must never be negative", UniqueIdGenerator.getUniqueId() >= 0);
            assertTrue("getUniqueId19 must never be negative for current epoch", UniqueIdGenerator.getUniqueId19() >= 0);
        }
    }
}
