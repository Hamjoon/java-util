package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@ExtendWith(MockitoExtension.class)
class TTLCache_shutdown_Test {

    @BeforeEach
    void setupBeforeEach() {
        // Replace the static scheduler with a fresh executor before each test
        // so that tests are independent of one another.
        resetScheduler();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Ensure the scheduler is shut down after all tests have run.
        // This mirrors the behaviour of the existing TTLCacheTest suite.
        TTLCache.shutdown();
    }

    /**
     * Helper that injects a brand‑new {@link ScheduledExecutorService} into the
     * private static final {@code scheduler} field of {@link TTLCache}.
     *
     * @throws Exception if reflection fails
     */
    private static void resetScheduler() throws Exception {
        ScheduledExecutorService fresh = Executors.newSingleThreadScheduledExecutor();
        Field schedulerField = TTLCache.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        // Remove the {@code final} modifier so we can replace the instance.
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(schedulerField, schedulerField.getModifiers() & ~Modifier.FINAL);
        schedulerField.set(null, fresh);
    }

    /**
     * Retrieves the current scheduler instance via reflection.
     *
     * @return the internal {@link ScheduledExecutorService}
     * @throws Exception if reflection fails
     */
    private static ScheduledExecutorService getScheduler() throws Exception {
        Field schedulerField = TTLCache.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        return (ScheduledExecutorService) schedulerField.get(null);
    }

    @Test
    void testShutdownIsIdempotentAndDoesNotThrow() {
        // First call – should shut down the internal scheduler without throwing.
        assertDoesNotThrow(TTLCache::shutdown, "First shutdown call should not throw");
        // Second call – the method should be safe to invoke multiple times.
        assertDoesNotThrow(TTLCache::shutdown, "Second shutdown call should also not throw");
    }

    @Test
    void testSchedulerIsShutdownAfterFirstCall() throws Exception {
        // Ensure the scheduler is active before the call.
        ScheduledExecutorService before = getScheduler();
        assertFalse(before.isShutdown(), "Scheduler should be active before shutdown");
        // Invoke shutdown once.
        TTLCache.shutdown();
        // Verify via reflection that the scheduler is now shut down.
        ScheduledExecutorService after = getScheduler();
        assertTrue(after.isShutdown(), "Scheduler should be shut down after first call");
    }

    @Test
    void testSchedulerRemainsShutdownAfterSecondCall() throws Exception {
        // First shutdown – should succeed.
        TTLCache.shutdown();
        // Capture the scheduler reference after the first call.
        ScheduledExecutorService afterFirst = getScheduler();
        assertTrue(afterFirst.isShutdown(), "Scheduler should be shut down after first call");
        // Second shutdown – should be a no‑op and not throw.
        assertDoesNotThrow(TTLCache::shutdown, "Second shutdown call should not throw");
        // Verify the same scheduler instance is still shut down.
        ScheduledExecutorService afterSecond = getScheduler();
        assertTrue(afterSecond.isShutdown(), "Scheduler should remain shut down after second call");
        assertSame(afterFirst, afterSecond, "Scheduler instance should not change between calls");
    }

    @Test
    void testConcurrentShutdownCallsDoNotThrowAndResultInShutdown() throws Exception {
        int threadCount = 10;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        // Spawn several threads that will call TTLCache.shutdown() concurrently.
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                // signal that this thread is ready
                ready.countDown();
                try {
                    // wait for the start signal
                    start.await();
                    // invoke the method under test
                    TTLCache.shutdown();
                } catch (Throwable t) {
                    // Capture any unexpected exception and fail the test.
                    fail("Exception thrown from concurrent shutdown: " + t);
                } finally {
                    // signal completion
                    done.countDown();
                }
            }, "shutdown-thread-" + i).start();
        }
        // Ensure all threads are ready before triggering the concurrent execution.
        ready.await();
        // let all threads proceed at the same time
        start.countDown();
        // wait for all threads to finish
        done.await();
        // After all concurrent calls, the scheduler must be shut down.
        ScheduledExecutorService scheduler = getScheduler();
        assertTrue(scheduler.isShutdown(), "Scheduler should be shut down after concurrent calls");
    }
}
