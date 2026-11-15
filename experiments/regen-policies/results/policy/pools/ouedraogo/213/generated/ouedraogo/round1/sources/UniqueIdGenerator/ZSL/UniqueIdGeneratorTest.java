package com.cedarsoftware.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

public class UniqueIdGeneratorTest {

    // Reflection handles for the private static fields
    private static Field countField;
    private static Field count2Field;
    private static Field lastTimeMillisField;
    private static Field lastTimeMillis19Field;
    private static Field lastGeneratedIdField;
    private static Field lastGeneratedId19Field;
    private static Field serverIdField;

    @Before
    public void setUp() throws Exception {
        // Obtain references to the private static fields once
        countField = UniqueIdGenerator.class.getDeclaredField("count");
        countField.setAccessible(true);
        count2Field = UniqueIdGenerator.class.getDeclaredField("count2");
        count2Field.setAccessible(true);
        lastTimeMillisField = UniqueIdGenerator.class.getDeclaredField("lastTimeMillis");
        lastTimeMillisField.setAccessible(true);
        lastTimeMillis19Field = UniqueIdGenerator.class.getDeclaredField("lastTimeMillis19");
        lastTimeMillis19Field.setAccessible(true);
        lastGeneratedIdField = UniqueIdGenerator.class.getDeclaredField("lastGeneratedId");
        lastGeneratedIdField.setAccessible(true);
        lastGeneratedId19Field = UniqueIdGenerator.class.getDeclaredField("lastGeneratedId19");
        lastGeneratedId19Field.setAccessible(true);
        serverIdField = UniqueIdGenerator.class.getDeclaredField("serverId");
        serverIdField.setAccessible(true);

        // Reset static state before each test
        resetStaticState();
    }

    @After
    public void tearDown() throws Exception {
        // Ensure state is clean for any subsequent test class execution
        resetStaticState();
    }

    private void resetStaticState() throws IllegalAccessException {
        countField.setInt(null, 0);
        count2Field.setInt(null, 0);
        lastTimeMillisField.setLong(null, 0L);
        lastTimeMillis19Field.setLong(null, 0L);
        lastGeneratedIdField.setLong(null, 0L);
        lastGeneratedId19Field.setLong(null, 0L);
    }

    private int getServerId() throws IllegalAccessException {
        return serverIdField.getInt(null);
    }

    @Test
    public void testUniqueIdStructureAndMonotonicity() throws Exception {
        long id1 = UniqueIdGenerator.getUniqueId();
        long id2 = UniqueIdGenerator.getUniqueId();

        assertTrue("Second ID should be greater than first ID", id2 > id1);

        // Verify serverId occupies the last two digits
        int serverId = getServerId();
        assertEquals("Server ID should match the last two digits", serverId, (int) (id2 % 100));

        // Verify the timestamp part (divide by 100_000) matches a plausible current time
        long timestampMs = id2 / 100_000;
        long now = System.currentTimeMillis();
        assertTrue("Timestamp should be recent", Math.abs(now - timestampMs) < 5000);
    }

    @Test
    public void testUniqueIdDateAndInstantExtraction() throws Exception {
        long id = UniqueIdGenerator.getUniqueId();

        Date extractedDate = UniqueIdGenerator.getDate(id);
        Instant extractedInstant = UniqueIdGenerator.getInstant(id);

        long expectedMillis = id / 100_000;
        assertEquals("Date extraction should match timestamp portion", expectedMillis, extractedDate.getTime());
        assertEquals("Instant extraction should match timestamp portion", expectedMillis, extractedInstant.toEpochMilli());
    }

    @Test
    public void testClockRegressionHandledGracefully() throws Exception {
        // Simulate a future lastTimeMillis (clock moved backwards)
        long future = System.currentTimeMillis() + 2000L;
        lastTimeMillisField.setLong(null, future);
        // Also set a known lastGeneratedId to avoid the auto‑increment adjustment
        lastGeneratedIdField.setLong(null, future * 100_000 + 0 * 100 + getServerId());

        long id = UniqueIdGenerator.getUniqueId();

        long timestampFromId = id / 100_000;
        assertEquals("When clock regresses, generator should use last known time", future, timestampFromId);
        // Sequence should have advanced (count was 0, now becomes 1)
        assertTrue("Sequence part should be >= 1", (id / 100) % 1000 >= 1);
    }

    @Test
    public void testSequenceOverflowWaitsForNextMillisecond() throws Exception {
        long now = System.currentTimeMillis();
        // Force internal state to be at the edge of the allowed sequence (999)
        lastTimeMillisField.setLong(null, now);
        countField.setInt(null, 999);
        // Set lastGeneratedId so that newId will be just below the overflow point
        long baseId = now * 100_000 + 999 * 100 + getServerId();
        lastGeneratedIdField.setLong(null, baseId - 1);

        long id = UniqueIdGenerator.getUniqueId();

        long timestamp = id / 100_000;
        assertTrue("ID timestamp should have advanced to next millisecond", timestamp > now);
        // After overflow, count should be reset to 0, so sequence part must be 0
        assertEquals("Sequence part should reset after overflow", 0, (int) ((id / 100) % 1000));
    }

    @Test
    public void testUniqueId19StructureAndMonotonicity() throws Exception {
        long id1 = UniqueIdGenerator.getUniqueId19();
        long id2 = UniqueIdGenerator.getUniqueId19();

        assertTrue("Second 19‑digit ID should be greater than first", id2 > id1);

        // Verify serverId occupies the last two digits
        int serverId = getServerId();
        assertEquals("Server ID should match the last two digits (19‑digit)", serverId, (int) (id2 % 100));

        // Verify timestamp part (divide by 1_000_000) matches current time
        long timestampMs = id2 / 1_000_000;
        long now = System.currentTimeMillis();
        assertTrue("Timestamp should be recent (19‑digit)", Math.abs(now - timestampMs) < 5000);
    }

    @Test
    public void testUniqueId19DateAndInstantExtraction() throws Exception {
        long id = UniqueIdGenerator.getUniqueId19();

        Date extractedDate = UniqueIdGenerator.getDate19(id);
        Instant extractedInstant = UniqueIdGenerator.getInstant19(id);

        long expectedMillis = id / 1_000_000;
        assertEquals("Date19 extraction should match timestamp portion", expectedMillis, extractedDate.getTime());
        assertEquals("Instant19 extraction should match timestamp portion", expectedMillis, extractedInstant.toEpochMilli());
    }

    @Test
    public void testUniqueId19SequenceOverflowWaitsForNextMillisecond() throws Exception {
        long now = System.currentTimeMillis();
        // Force internal state to just before the 10,000 limit
        lastTimeMillis19Field.setLong(null, now);
        count2Field.setInt(null, 9_999);
        // Adjust lastGeneratedId19 to a safe base
        long baseId = now * 1_000_000 + 9_999 * 100 + getServerId();
        lastGeneratedId19Field.setLong(null, baseId - 1);

        long id = UniqueIdGenerator.getUniqueId19();

        long timestamp = id / 1_000_000;
        assertTrue("ID19 timestamp should have advanced after overflow", timestamp > now);
        // Sequence part should reset to 0 after overflow
        assertEquals("Sequence part should reset after overflow (19‑digit)", 0, (int) ((id / 100) % 10_000));
    }

    @Test
    public void testConcurrentGenerationProducesUniqueMonotonicIds() throws Exception {
        int threadCount = 8;
        int idsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        Set<Long> uniqueIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
        List<Future<?>> futures = new ArrayList<>();

        CountDownLatch startLatch = new CountDownLatch(1);
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < idsPerThread; j++) {
                        long id = UniqueIdGenerator.getUniqueId();
                        assertTrue("All generated IDs must be positive", id > 0);
                        // add returns false if duplicate is found
                        assertTrue("Duplicate ID detected", uniqueIds.add(id));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        // Release all threads at once
        startLatch.countDown();

        // Wait for completion
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }
        executor.shutdownNow();

        // Verify total number of IDs generated matches expectation
        assertEquals("Total unique IDs generated", threadCount * idsPerThread, uniqueIds.size());

        // Verify monotonicity when IDs are sorted
        List<Long> sorted = new ArrayList<>(uniqueIds);
        Collections.sort(sorted);
        for (int i = 1; i < sorted.size(); i++) {
            assertTrue("Sorted IDs must be strictly increasing", sorted.get(i) > sorted.get(i - 1));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstantRejectsNegativeId() {
        UniqueIdGenerator.getInstant(-1L);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetInstant19RejectsNegativeId() {
        UniqueIdGenerator.getInstant19(-123L);
    }
}
