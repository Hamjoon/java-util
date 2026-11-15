package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import static com.cedarsoftware.util.UniqueIdGenerator.getUniqueId;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.time.Instant;

@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getUniqueId_Test {

    /**
     * Names of the private static fields that hold generator state.
     */
    private static final String FIELD_COUNT = "count";

    private static final String FIELD_LAST_TIME = "lastTimeMillis";

    private static final String FIELD_LAST_ID = "lastGeneratedId";

    /**
     * The number of IDs that can be generated within a single millisecond.
     */
    private static final int MAX_SEQUENCE_PER_MILLIS = 1000;

    private static final String FIELD_SERVER_ID = "serverId";

    /**
     * Helper that clears the static state of {@link UniqueIdGenerator}.
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        resetStaticField(UniqueIdGenerator.class, FIELD_COUNT, 0);
        resetStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME, 0L);
        resetStaticField(UniqueIdGenerator.class, FIELD_LAST_ID, 0L);
        // serverId is initialised once by the class – we keep the original value,
        // but tests that need a custom value will overwrite it explicitly.
    }

    /**
     * Extracts the timestamp component from a generated ID.
     * The ID format is: {@code timestamp * 100_000 + (count * 100) + serverId}.
     *
     * @param id the generated unique identifier
     * @return the timestamp in milliseconds since the epoch
     */
    private static long extractTimestamp(long id) {
        return id / 100_000L;
    }

    /**
     * Resets a private static field via reflection.
     *
     * @param clazz      the class containing the field
     * @param fieldName  the name of the field
     * @param newValue   the value to set
     * @throws Exception if reflection fails
     */
    private static void resetStaticField(Class<?> clazz, String fieldName, Object newValue) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, newValue);
    }

    /**
     * Reads a private static field via reflection.
     *
     * @param clazz     the class containing the field
     * @param fieldName the field name
     * @return the current value of the field
     * @throws Exception if reflection fails
     */
    private static Object getStaticField(Class<?> clazz, String fieldName) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        return f.get(null);
    }

    /**
     * Writes a private static field via reflection.
     *
     * @param clazz     the class containing the field
     * @param fieldName the field name
     * @param value     the new value to set
     * @throws Exception if reflection fails
     */
    private static void setStaticField(Class<?> clazz, String fieldName, Object value) throws Exception {
        Field f = clazz.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(null, value);
    }

    /**
     * Retrieves the current serverId (used in expected‑value calculations).
     *
     * @return the server identifier stored in {@code UniqueIdGenerator}
     * @throws Exception if reflection fails
     */
    private static int getServerId() throws Exception {
        return (int) getStaticField(UniqueIdGenerator.class, FIELD_SERVER_ID);
    }

    /**
     * --------------------------------------------------------------------- *
     *  Test 1 – basic contract: length and monotonic increase.
     *  ---------------------------------------------------------------------
     */
    @Test
    void generatedIdHasCorrectLengthAndIsMonotonicallyIncreasing() {
        long first = getUniqueId();
        long second = getUniqueId();
        // Length must be exactly 18 digits.
        assertEquals(18, String.valueOf(first).length(), "First generated ID should be 18 digits long");
        assertEquals(18, String.valueOf(second).length(), "Second generated ID should be 18 digits long");
        // IDs must increase.
        assertTrue(second > first, "Second ID must be greater than the first");
    }

    /**
     * --------------------------------------------------------------------- *
     *  Test 2 – sequence component increments by 100 while the timestamp part
     *           stays the same.
     *  ---------------------------------------------------------------------
     */
    @Test
    void sequenceIncrementsWithinSameMillisecond() {
        // Capture the first ID and its timestamp component.
        long first = getUniqueId();
        long timestamp = extractTimestamp(first);
        // count*100 + serverId
        long remainder = first % 100_000L;
        // Keep generating until we encounter another ID that shares the same timestamp.
        long second = -1L;
        for (int i = 0; i < 100; i++) {
            // a reasonable upper bound
            long candidate = getUniqueId();
            if (extractTimestamp(candidate) == timestamp) {
                second = candidate;
                break;
            }
        }
        assertTrue(second > 0, "Could not obtain a second ID within the same millisecond");
        long secondRemainder = second % 100_000L;
        long diff = secondRemainder - remainder;
        // The serverId part is constant; the only thing that should change is count*100.
        // Therefore the difference must be exactly 100.
        assertEquals(100L, diff, "Sequence component should increase by 100 for each ID generated in the same millisecond");
    }

    /**
     * --------------------------------------------------------------------- *
     *  Test 3 – after 1 000 IDs in the same millisecond the generator must
     *           advance to the next millisecond.
     *  ---------------------------------------------------------------------
     */
    @Test
    void generatorAdvancesWhenSequenceExceedsLimit() {
        // Record the timestamp of the first generated ID.
        long first = getUniqueId();
        long firstTimestamp = extractTimestamp(first);
        // Generate MAX_SEQUENCE_PER_MILLIS IDs (the limit) and keep the last one.
        long lastInSameMs = first;
        for (int i = 1; i < MAX_SEQUENCE_PER_MILLIS; i++) {
            lastInSameMs = getUniqueId();
        }
        // The timestamp should still be the same for the first 1 000 IDs.
        assertEquals(firstTimestamp, extractTimestamp(lastInSameMs), "After generating up to the limit, timestamp should still be the original millisecond");
        // The next call must move to the next millisecond.
        long afterLimit = getUniqueId();
        long afterTimestamp = extractTimestamp(afterLimit);
        assertTrue(afterTimestamp > firstTimestamp, "When the sequence counter overflows, the generator must wait for the next millisecond");
    }

    /**
     * --------------------------------------------------------------------- *
     *  Test 4 – high‑concurrency generation produces only unique IDs.
     *  ---------------------------------------------------------------------
     */
    @Test
    void concurrentGenerationProducesUniqueValues() throws Exception {
        final int threads = 8;
        final int idsPerThread = 25_000;
        Set<Long> allIds = new HashSet<>(threads * idsPerThread);
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread(() -> {
                for (int i = 0; i < idsPerThread; i++) {
                    long v = getUniqueId();
                    synchronized (allIds) {
                        allIds.add(v);
                    }
                }
            });
            workers[t].start();
        }
        for (Thread w : workers) {
            w.join();
        }
        assertEquals(threads * idsPerThread, allIds.size(), "All generated IDs must be unique even under concurrent access");
    }

    /* --------------------------------------------------------------------- *
     *  2. Clock goes backwards (currentTime < lastTimeMillis).
     *  --------------------------------------------------------------------- */
    @Test
    void clockRegressionUsesLastTimeMillis() throws Exception {
        long fixedLastTime = 1_600_000_000_000L;
        // smaller than lastTimeMillis
        long regressionTime = 1_599_999_999_000L;
        // prime the generator with a previous ID so that lastGeneratedId is known
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME, fixedLastTime);
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_ID, 0L);
        setStaticField(UniqueIdGenerator.class, FIELD_COUNT, 0);
        try (MockedStatic<System> sys = Mockito.mockStatic(System.class)) {
            sys.when(System::currentTimeMillis).thenReturn(regressionTime);
            long id = getUniqueId();
            long usedTimestamp = extractTimestamp(id);
            assertEquals(fixedLastTime, usedTimestamp, "When the clock moves backwards the generator must use lastTimeMillis");
            long lastGenerated = (long) getStaticField(UniqueIdGenerator.class, FIELD_LAST_ID);
            assertTrue(id > lastGenerated - 1, "ID must be greater than the previous lastGeneratedId");
        }
    }

    /* --------------------------------------------------------------------- *
     *  3. Same millisecond without overflow (count < 1000).
     *  --------------------------------------------------------------------- */
    @Test
    void sameMillisecondIncrementsCount() throws Exception {
        long fixedTime = 1_600_000_000_000L;
        int initialCount = 5;
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME, fixedTime);
        setStaticField(UniqueIdGenerator.class, FIELD_COUNT, initialCount);
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_ID, 0L);
        try (MockedStatic<System> sys = Mockito.mockStatic(System.class)) {
            sys.when(System::currentTimeMillis).thenReturn(fixedTime);
            long id = getUniqueId();
            // expected count = initialCount + 1
            int expectedCount = initialCount + 1;
            int actualCount = (int) getStaticField(UniqueIdGenerator.class, FIELD_COUNT);
            assertEquals(expectedCount, actualCount, "Count should be incremented by one");
            long expectedId = ((fixedTime * 100_000L) + (expectedCount * 100L)) + getServerId();
            assertEquals(expectedId, id, "ID must follow the formula with the incremented count");
        }
    }

    /* --------------------------------------------------------------------- *
     *  4. Count overflow – wait for next millisecond.
     *  --------------------------------------------------------------------- */
    @Test
    void overflowTriggersWaitForNextMillis() throws Exception {
        long fixedTime = 1_600_000_000_000L;
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME, fixedTime);
        setStaticField(UniqueIdGenerator.class, FIELD_COUNT, 999);
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_ID, 0L);
        // System.currentTimeMillis() sequence:
        // 1️⃣ first call – same millisecond (causes count to become 1000)
        // 2️⃣ second call inside waitForNextMillis – still same (loop)
        // 3️⃣ third call – next millisecond
        try (MockedStatic<System> sys = Mockito.mockStatic(System.class)) {
            sys.when(System::currentTimeMillis).thenReturn(// first call
            fixedTime).thenReturn(// first loop iteration
            fixedTime).thenReturn(// exit loop
            fixedTime + 1);
            long id = getUniqueId();
            // After overflow count must be reset to 0
            int countAfter = (int) getStaticField(UniqueIdGenerator.class, FIELD_COUNT);
            assertEquals(0, countAfter, "Count must be reset to 0 after overflow");
            long lastTimeAfter = (long) getStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME);
            assertEquals(fixedTime + 1, lastTimeAfter, "lastTimeMillis must be updated to the timestamp returned by waitForNextMillis");
            // count = 0
            long expectedId = ((fixedTime + 1) * 100_000L) + getServerId();
            assertEquals(expectedId, id, "Generated ID must use the new timestamp and count=0");
        }
    }

    /* --------------------------------------------------------------------- *
     *  5. Time advances (currentTime > lastTimeMillis).
     *  --------------------------------------------------------------------- */
    @Test
    void timeAdvancesResetsCount() throws Exception {
        long oldTime = 1_600_000_000_000L;
        long newTime = oldTime + 123L;
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME, oldTime);
        setStaticField(UniqueIdGenerator.class, FIELD_COUNT, 42);
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_ID, 0L);
        try (MockedStatic<System> sys = Mockito.mockStatic(System.class)) {
            sys.when(System::currentTimeMillis).thenReturn(newTime);
            long id = getUniqueId();
            int countAfter = (int) getStaticField(UniqueIdGenerator.class, FIELD_COUNT);
            assertEquals(0, countAfter, "Count must be reset to 0 when time moves forward");
            long lastTimeAfter = (long) getStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME);
            assertEquals(newTime, lastTimeAfter, "lastTimeMillis must be updated to the new current time");
            // count = 0
            long expectedId = (newTime * 100_000L) + getServerId();
            assertEquals(expectedId, id, "ID must be based on the new timestamp with count 0");
        }
    }

    /* --------------------------------------------------------------------- *
     *  6. Monotonic ID enforcement (newId <= lastGeneratedId).
     *  --------------------------------------------------------------------- */
    @Test
    void monotonicEnforcementEnsuresIdIsGreaterThanLast() throws Exception {
        long highLastId = 9_999_999_999_999L;
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_ID, highLastId);
        // tiny timestamp
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME, 1L);
        setStaticField(UniqueIdGenerator.class, FIELD_COUNT, 0);
        // Mock a tiny current time so that the calculated newId would be far smaller
        try (MockedStatic<System> sys = Mockito.mockStatic(System.class)) {
            sys.when(System::currentTimeMillis).thenReturn(1L);
            long id = getUniqueId();
            assertEquals(highLastId + 1, id, "When calculated ID is not larger, generator must return lastGeneratedId+1");
            long lastIdAfter = (long) getStaticField(UniqueIdGenerator.class, FIELD_LAST_ID);
            assertEquals(id, lastIdAfter, "lastGeneratedId must be updated to the returned value");
        }
    }

    /* --------------------------------------------------------------------- *
     *  7. Server ID inclusion – positive value.
     *  --------------------------------------------------------------------- */
    @Test
    void serverIdPositiveIsAddedToId() throws Exception {
        int customServerId = 12_345;
        long fixedTime = 1_600_000_000_000L;
        setStaticField(UniqueIdGenerator.class, FIELD_SERVER_ID, customServerId);
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME, fixedTime);
        setStaticField(UniqueIdGenerator.class, FIELD_COUNT, 0);
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_ID, 0L);
        try (MockedStatic<System> sys = Mockito.mockStatic(System.class)) {
            sys.when(System::currentTimeMillis).thenReturn(fixedTime);
            long id = getUniqueId();
            long expected = (fixedTime * 100_000L) + customServerId;
            assertEquals(expected, id, "ID must contain the positive serverId");
        }
    }

    /* --------------------------------------------------------------------- *
     *  8. Server ID inclusion – negative value.
     *  --------------------------------------------------------------------- */
    @Test
    void serverIdNegativeIsSubtractedFromId() throws Exception {
        int customServerId = -7;
        long fixedTime = 1_600_000_000_001L;
        setStaticField(UniqueIdGenerator.class, FIELD_SERVER_ID, customServerId);
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME, fixedTime);
        setStaticField(UniqueIdGenerator.class, FIELD_COUNT, 0);
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_ID, 0L);
        try (MockedStatic<System> sys = Mockito.mockStatic(System.class)) {
            sys.when(System::currentTimeMillis).thenReturn(fixedTime);
            long id = getUniqueId();
            long expected = (fixedTime * 100_000L) + customServerId;
            assertEquals(expected, id, "ID must contain the negative serverId");
        }
    }

    /* --------------------------------------------------------------------- *
     *  9. waitForNextMillis loop handling (multiple attempts).
     *  --------------------------------------------------------------------- */
    @Test
    void waitForNextMillisLoopsUntilTimeAdvances() throws Exception {
        long baseTime = 1_600_000_000_000L;
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME, baseTime);
        setStaticField(UniqueIdGenerator.class, FIELD_COUNT, 999);
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_ID, 0L);
        // Sequence: same time (first call), same time again (first loop), finally +1 (exit loop)
        try (MockedStatic<System> sys = Mockito.mockStatic(System.class)) {
            sys.when(System::currentTimeMillis).thenReturn(// initial call – overflow
            baseTime).thenReturn(// first loop iteration – still not > lastTimestamp
            baseTime).thenReturn(// second loop iteration – progress
            baseTime + 1);
            long id = getUniqueId();
            long lastTimeAfter = (long) getStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME);
            assertEquals(baseTime + 1, lastTimeAfter, "lastTimeMillis must reflect the timestamp finally returned by waitForNextMillis");
            int countAfter = (int) getStaticField(UniqueIdGenerator.class, FIELD_COUNT);
            assertEquals(0, countAfter, "Count must be reset to 0 after overflow handling");
            long expected = ((baseTime + 1) * 100_000L) + getServerId();
            assertEquals(expected, id, "Generated ID must use the timestamp after the loop");
        }
    }

    /* --------------------------------------------------------------------- *
     * 10. Sequential real‑time monotonicity (no mocking).
     *  --------------------------------------------------------------------- */
    @Test
    void sequentialCallsWithoutMockingAreStrictlyIncreasing() {
        long[] ids = new long[5];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = getUniqueId();
        }
        for (int i = 0; i < ids.length - 1; i++) {
            assertTrue(ids[i] < ids[i + 1], "ID at index " + i + " must be less than ID at index " + (i + 1));
        }
    }

    /* --------------------------------------------------------------------- *
     * 11. Count reset after overflow and subsequent same‑millisecond call.
     *  --------------------------------------------------------------------- */
    @Test
    void countResetsAfterOverflowThenIncrementsOnNextCall() throws Exception {
        long baseTime = 1_600_000_000_000L;
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME, baseTime);
        setStaticField(UniqueIdGenerator.class, FIELD_COUNT, 999);
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_ID, 0L);
        // First call will overflow and move to baseTime+1
        try (MockedStatic<System> sys = Mockito.mockStatic(System.class)) {
            sys.when(System::currentTimeMillis).thenReturn(// initial call – overflow
            baseTime).thenReturn(// loop iteration
            baseTime).thenReturn(// exit loop
            baseTime + 1).thenReturn(// second call – same millisecond
            baseTime + 1);
            // overflow
            long firstId = getUniqueId();
            // same ms, count should become 1
            long secondId = getUniqueId();
            // Verify first ID uses new timestamp with count=0
            long expectedFirst = ((baseTime + 1) * 100_000L) + getServerId();
            assertEquals(expectedFirst, firstId, "First ID after overflow should have count=0");
            // Verify second ID uses same timestamp with count=1
            long expectedSecond = ((baseTime + 1) * 100_000L) + (1 * 100L) + getServerId();
            assertEquals(expectedSecond, secondId, "Second ID should have count=1 after overflow");
        }
    }

    /* --------------------------------------------------------------------- *
     * 12. Multiple overflow cycles within a single test.
     *  --------------------------------------------------------------------- */
    @Test
    void multipleOverflowCyclesProduceIncreasingIds() throws Exception {
        long baseTime = 1_600_000_000_000L;
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_TIME, baseTime);
        setStaticField(UniqueIdGenerator.class, FIELD_COUNT, 998);
        setStaticField(UniqueIdGenerator.class, FIELD_LAST_ID, 0L);
        // Sequence of System.currentTimeMillis():
        // Call1: same time (count -> 999)
        // Call2: same time (count -> 1000 -> overflow) -> wait loop returns base+1
        // Call3: same time (now base+1) (count -> 1)
        // We'll need enough returns for the internal wait loops.
        try (MockedStatic<System> sys = Mockito.mockStatic(System.class)) {
            sys.when(System::currentTimeMillis).// first call (count 998 -> 999)
            thenReturn(baseTime).// second call (count 999 -> 1000, overflow)
            thenReturn(// initial overflow detection
            baseTime).thenReturn(// first loop iteration (no progress)
            baseTime).thenReturn(// exit loop – timestamp 1
            baseTime + 1).// third call (same ms as previous, count becomes 1)
            thenReturn(baseTime + 1);
            // count=999
            long id1 = getUniqueId();
            // overflow -> timestamp base+1, count=0
            long id2 = getUniqueId();
            // same ms, count=1
            long id3 = getUniqueId();
            // Expected values
            long expectedId1 = (baseTime * 100_000L) + (999 * 100L) + getServerId();
            // count=0
            long expectedId2 = ((baseTime + 1) * 100_000L) + getServerId();
            long expectedId3 = ((baseTime + 1) * 100_000L) + (1 * 100L) + getServerId();
            assertEquals(expectedId1, id1, "First ID should have count=999");
            assertEquals(expectedId2, id2, "Second ID should be after first overflow (count=0)");
            assertEquals(expectedId3, id3, "Third ID should have count=1 in the new millisecond");
            assertTrue(id1 < id2 && id2 < id3, "All IDs must be strictly increasing");
        }
    }
}
