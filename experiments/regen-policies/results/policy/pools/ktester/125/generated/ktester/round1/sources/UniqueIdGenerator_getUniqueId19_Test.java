package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import static com.cedarsoftware.util.UniqueIdGenerator.getUniqueId19;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Modifier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ExtendWith(MockitoExtension.class)
class UniqueIdGenerator_getUniqueId19_Test {

    /**
     * Reflection handles for the private static fields that influence generation.
     */
    private static Field countField;

    private static Field lastTimeField;

    private static Field lastGeneratedField;

    private static Field serverIdField;

    private static Field lockField;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain references to the private static fields once.
        countField = UniqueIdGenerator.class.getDeclaredField("count2");
        countField.setAccessible(true);
        lastTimeField = UniqueIdGenerator.class.getDeclaredField("lastTimeMillis19");
        lastTimeField.setAccessible(true);
        lastGeneratedField = UniqueIdGenerator.class.getDeclaredField("lastGeneratedId19");
        lastGeneratedField.setAccessible(true);
        serverIdField = UniqueIdGenerator.class.getDeclaredField("serverId");
        serverIdField.setAccessible(true);
        // serverId is final – remove the final modifier so we can inject a test value.
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(serverIdField, serverIdField.getModifiers() & ~Modifier.FINAL);
        lockField = UniqueIdGenerator.class.getDeclaredField("lock19");
        lockField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Reset the generator's internal state so each test starts from a clean slate.
        countField.setInt(null, 0);
        lastTimeField.setLong(null, 0L);
        lastGeneratedField.setLong(null, 0L);
        // Restore the default serverId (the real implementation derives it from the environment;
        // we simply set it to 0 for a deterministic baseline).
        serverIdField.setInt(null, 0);
    }

    /**
     * Helper that extracts the timestamp component from a 19‑digit unique id.
     */
    private static long timestampFromId19(long id) {
        // The format is: timestampMs * 1_000_000 + (sequence * 100) + serverId
        return id / 1_000_000L;
    }

    /**
     * Helper that extracts the low‑order part (sequence*100 + serverId).
     */
    private static long lowOrderFromId19(long id) {
        return id % 1_000_000L;
    }

    @Test
    void generatedIdHasCorrectLengthAndIsMonotonicallyIncreasing() {
        long first = getUniqueId19();
        long second = getUniqueId19();
        assertEquals(19, String.valueOf(first).length(), "First generated id should be 19 digits long");
        assertEquals(19, String.valueOf(second).length(), "Second generated id should be 19 digits long");
        assertTrue(second > first, "Second id must be greater than the first (monotonic increase)");
    }

    @Test
    void sequenceOverflowCausesWaitForNextMillisecond() throws Exception {
        // Arrange: set the internal counter to its maximum (9999) and the timestamp to "now".
        long now = System.currentTimeMillis();
        lastTimeField.setLong(null, now);
        // next call will trigger overflow handling
        countField.setInt(null, 9_999);
        // this consumes the 9 999th sequence
        long beforeOverflow = getUniqueId19();
        // Act: the next call should detect count >= 10 000, wait for the next ms and reset count.
        long afterOverflow = getUniqueId19();
        // Assert: the timestamp part must have advanced by at least one millisecond.
        long tsBefore = timestampFromId19(beforeOverflow);
        long tsAfter = timestampFromId19(afterOverflow);
        assertTrue(tsAfter > tsBefore, "Timestamp must advance after sequence overflow (waitForNextMillis should have been invoked)");
        // The difference in ids should be at least 1_000_000 (one millisecond * 1_000_000 factor).
        assertTrue(afterOverflow - beforeOverflow >= 1_000_000L, "Id difference after overflow should be at least 1_000_000");
    }

    @Test
    void clockRegressionIsHandledGracefully() throws Exception {
        // Arrange: simulate that the last generated timestamp was in the future.
        long future = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
        lastTimeField.setLong(null, future);
        countField.setInt(null, 0);
        lastGeneratedField.setLong(null, 0L);
        // Act: generate a new id; the generator should fall back to the stored future timestamp.
        long id = getUniqueId19();
        // Assert: the timestamp component of the id equals the stored future timestamp.
        long extractedTimestamp = timestampFromId19(id);
        assertEquals(future, extractedTimestamp, "When the system clock moves backwards, the generator must reuse the last known timestamp");
    }

    @Test
    void sameMillisecondIncrementAddsExactly100PerCall() throws Exception {
        // Arrange: force a known timestamp and reset the counter.
        long fixedTime = System.currentTimeMillis();
        lastTimeField.setLong(null, fixedTime);
        countField.setInt(null, 0);
        // Ensure serverId is 0 so we can see the pure sequence contribution.
        serverIdField.setInt(null, 0);
        // Act: generate two IDs that belong to the same millisecond.
        long first = getUniqueId19();
        // Keep generating until we hit the same millisecond (highly likely on fast hardware).
        long second;
        int attempts = 0;
        do {
            second = getUniqueId19();
            attempts++;
        } while (timestampFromId19(second) != timestampFromId19(first) && attempts < 10);
        // Guard against flaky environments.
        assertEquals(timestampFromId19(first), timestampFromId19(second), "Failed to obtain two IDs within the same millisecond after " + attempts + " attempts");
        // Assert: the low‑order part must differ by exactly 100 (sequence increment).
        long diff = lowOrderFromId19(second) - lowOrderFromId19(first);
        assertEquals(100L, diff, "Sequence increment within the same millisecond must be exactly 100");
    }

    @Test
    void monotonicIncreaseWhenComputedIdIsStale() throws Exception {
        // Arrange: set a large previously generated id and a much older timestamp.
        long staleLastGenerated = 9_999_999_999L;
        lastGeneratedField.setLong(null, staleLastGenerated);
        // a tiny timestamp (far in the past)
        lastTimeField.setLong(null, 1_000L);
        countField.setInt(null, 0);
        serverIdField.setInt(null, 0);
        // Act: generate a new id.
        long id = getUniqueId19();
        // Assert: the generator must have returned staleLastGenerated + 1.
        assertEquals(staleLastGenerated + 1, id, "When the computed newId would be <= lastGeneratedId19, the method must return lastGeneratedId19 + 1");
    }

    @Test
    void serverIdIsEmbeddedInLowOrderDigits() throws Exception {
        // Arrange: inject a known serverId (e.g., 57).
        int testServerId = 57;
        serverIdField.setInt(null, testServerId);
        // Ensure count2 = 0 so low order part equals serverId.
        countField.setInt(null, 0);
        lastTimeField.setLong(null, System.currentTimeMillis());
        // Act: generate an id.
        long id = getUniqueId19();
        // Assert: the two least‑significant decimal digits equal the injected serverId.
        assertEquals(testServerId, (int) (id % 100), "The generated id's low‑order two digits must match the injected serverId");
    }

    @Test
    void lockIsReleasedAfterNormalExecution() throws Exception {
        // Arrange: obtain the lock instance via reflection.
        Lock lock = (Lock) lockField.get(null);
        assertTrue(lock instanceof ReentrantLock, "lock19 should be a ReentrantLock");
        // Act: invoke the method once.
        getUniqueId19();
        // Assert: the lock must not be held by any thread after the method returns.
        ReentrantLock reLock = (ReentrantLock) lock;
        assertFalse(reLock.isLocked(), "lock19 must be released after normal method exit");
    }
}
