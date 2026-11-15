package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
class TTLCache_shutdown_Test {

    @AfterEach
    void teardownAfterEach() {
        // Gracefully shut down the scheduler after each test to avoid thread leaks.
        try {
            TTLCache.shutdown();
            // Await termination briefly; ignore result – the test will clean up anyway.
            getScheduler().awaitTermination(500, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Guarantee that the scheduler is shut down after all tests have run.
        TTLCache.shutdown();
    }

    /**
     * Retrieves the private static {@code scheduler} field from {@link TTLCache}
     * via reflection.
     */
    private static ScheduledExecutorService getScheduler() throws Exception {
        Field field = TTLCache.class.getDeclaredField("scheduler");
        field.setAccessible(true);
        return (ScheduledExecutorService) field.get(null);
    }

    /**
     * Replaces the private static {@code scheduler} with a fresh
     * {@link ScheduledExecutorService}. This is required because the
     * scheduler is a static final field that can be shut down only once.
     */
    private static void resetScheduler() throws Exception {
        Field field = TTLCache.class.getDeclaredField("scheduler");
        field.setAccessible(true);
        // Remove the final modifier (necessary for Java 8+ reflection hacks)
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        // Assign a new executor
        ScheduledExecutorService fresh = Executors.newSingleThreadScheduledExecutor();
        field.set(null, fresh);
    }

    /**
     * Verifies that invoking {@link TTLCache#shutdown()} prevents any further
     * scheduling of purge tasks. The {@link TTLCache} constructor schedules a
     * task using the internal scheduler; after shutdown this should result in a
     * {@link RejectedExecutionException}.
     */
    @Test
    void shutdownPreventsNewCacheScheduling() {
        // First, shut down the shared scheduler.
        TTLCache.shutdown();
        // Attempt to create a new cache – the constructor will try to schedule a
        // purge task and should therefore throw RejectedExecutionException.
        assertThrows(RejectedExecutionException.class, () -> new TTLCache<>(1000, -1, 100), "Creating a TTLCache after shutdown should be rejected");
    }

    /**
     * Ensures that calling {@link TTLCache#shutdown()} multiple times does not
     * throw an exception (the operation is idempotent).
     */
    @Test
    void shutdownIsIdempotent() {
        // First shutdown – may or may not have been called already by other tests.
        TTLCache.shutdown();
        // Second shutdown should be a no‑op and must not throw.
        assertDoesNotThrow(TTLCache::shutdown, "Calling shutdown a second time should not throw an exception");
    }

    /**
     * Verify that after a single call to {@code TTLCache.shutdown()} the
     * underlying scheduler reports {@code isShutdown() == true}.
     */
    @Test
    void schedulerIsShutdownAfterShutdown() throws Exception {
        TTLCache.shutdown();
        ScheduledExecutorService scheduler = getScheduler();
        assertTrue(scheduler.isShutdown(), "Scheduler should be shut down after TTLCache.shutdown()");
    }

    /**
     * Call {@code TTLCache.shutdown()} twice and ensure the second call does
     * not throw and the scheduler stays shut down.
     */
    @Test
    void shutdownIdempotentAndSchedulerRemainsShutdown() throws Exception {
        // first call
        TTLCache.shutdown();
        assertDoesNotThrow(TTLCache::shutdown, "Second shutdown call must not throw");
        ScheduledExecutorService scheduler = getScheduler();
        assertTrue(scheduler.isShutdown(), "Scheduler must remain shut down after repeated shutdown calls");
    }

    /**
     * After shutdown, any attempt to schedule a new task should be rejected
     * with {@link RejectedExecutionException}.
     */
    @Test
    void schedulingAfterShutdownThrowsRejectedExecutionException() throws Exception {
        TTLCache.shutdown();
        ScheduledExecutorService scheduler = getScheduler();
        Runnable dummy = () -> {
            /* no‑op */
        };
        assertThrows(RejectedExecutionException.class, () -> scheduler.schedule(dummy, 0, TimeUnit.MILLISECONDS), "Scheduling after shutdown must be rejected");
    }

    /**
     * Concurrently invoke {@code TTLCache.shutdown()} from multiple threads.
     * The method must be thread‑safe and the scheduler must end up shut down.
     */
    @Test
    void concurrentShutdownCallsAreThreadSafe() throws Exception {
        int threadCount = 10;
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(TTLCache::shutdown);
            threads.add(t);
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        ScheduledExecutorService scheduler = getScheduler();
        assertTrue(scheduler.isShutdown(), "Scheduler should be shut down after concurrent shutdown calls");
    }

    /**
     * If the scheduler is shut down directly via {@code shutdownNow()}, a
     * subsequent call to {@code TTLCache.shutdown()} must be harmless.
     */
    @Test
    void shutdownAfterExternalShutdownNowIsHarmless() throws Exception {
        ScheduledExecutorService scheduler = getScheduler();
        // Directly shut down the executor.
        scheduler.shutdownNow();
        // Now invoke the public shutdown method – it should not throw.
        assertDoesNotThrow(TTLCache::shutdown, "Calling TTLCache.shutdown() after external shutdownNow() must not throw");
        assertTrue(scheduler.isShutdown(), "Scheduler must remain shut down after calling TTLCache.shutdown()");
    }

    /**
     * After invoking {@code TTLCache.shutdown()}, {@code awaitTermination}
     * should return {@code true} within a short timeout and the executor must be
     * reported as terminated.
     */
    @Test
    void awaitTerminationCompletesSuccessfully() throws Exception {
        TTLCache.shutdown();
        ScheduledExecutorService scheduler = getScheduler();
        boolean terminated = scheduler.awaitTermination(1, TimeUnit.SECONDS);
        assertTrue(terminated, "awaitTermination should return true after shutdown");
        assertTrue(scheduler.isTerminated(), "Scheduler must report terminated after awaitTermination");
    }

    /**
     * Verify that {@code scheduleAtFixedRate} is rejected after shutdown.
     */
    @Test
    void scheduleAtFixedRateAfterShutdownThrowsRejectedExecutionException() throws Exception {
        TTLCache.shutdown();
        ScheduledExecutorService scheduler = getScheduler();
        Runnable dummy = () -> {
            /* no‑op */
        };
        assertThrows(RejectedExecutionException.class, () -> scheduler.scheduleAtFixedRate(dummy, 0, 10, TimeUnit.MILLISECONDS), "scheduleAtFixedRate after shutdown must be rejected");
    }
}
