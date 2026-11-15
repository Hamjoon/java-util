package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TTLCache_shutdown_Test {

    /**
     * Holds a reference to the private static scheduler so we can inspect its state.
     */
    private static ScheduledExecutorService scheduler;

    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Obtain the private static scheduler via reflection once for the whole test suite.
        Field f = TTLCache.class.getDeclaredField("scheduler");
        f.setAccessible(true);
        scheduler = (ScheduledExecutorService) f.get(null);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Ensure the scheduler is shut down after all tests have run.
        // This mirrors the behaviour of the original TTLCacheTest class.
        TTLCache.shutdown();
    }

    /**
     * Helper that (re)creates the static scheduler if it has been shut down.
     * The field is {@code private static final}, therefore we also clear the
     * {@code final} modifier via reflection before assigning a new instance.
     */
    private static void resetSchedulerIfNeeded() throws Exception {
        if (scheduler == null || scheduler.isShutdown() || scheduler.isTerminated()) {
            // create a fresh executor
            ScheduledExecutorService fresh = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TTLCache-Test-Purge-Thread");
                t.setDaemon(true);
                return t;
            });
            // replace the final static field
            Field f = TTLCache.class.getDeclaredField("scheduler");
            f.setAccessible(true);
            // remove final modifier (required on recent JVMs)
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
            f.set(null, fresh);
            scheduler = fresh;
        }
    }

    @Test
    void testShutdown() {
        // The scheduler should be alive before we invoke shutdown().
        assertFalse(scheduler.isShutdown(), "Scheduler should be active before calling TTLCache.shutdown()");
        // Invoke the method under test.
        TTLCache.shutdown();
        // After shutdown, the scheduler must report being shut down or terminated.
        assertTrue(scheduler.isShutdown() || scheduler.isTerminated(), "Scheduler should be shut down after calling TTLCache.shutdown()");
    }

    @Test
    @Order(1)
    void testShutdownBasic() throws Exception {
        resetSchedulerIfNeeded();
        assertFalse(scheduler.isShutdown(), "Scheduler should be active before calling TTLCache.shutdown()");
        TTLCache.shutdown();
        assertTrue(scheduler.isShutdown() || scheduler.isTerminated(), "Scheduler should be shut down after calling TTLCache.shutdown()");
    }

    @Test
    @Order(2)
    void testShutdownIdempotentMultipleCalls() throws Exception {
        resetSchedulerIfNeeded();
        // first call – should shut down
        TTLCache.shutdown();
        assertTrue(scheduler.isShutdown() || scheduler.isTerminated(), "Scheduler must be shut down after first call");
        // subsequent calls – must not throw and scheduler stays shut down
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(TTLCache::shutdown, "Calling shutdown() repeatedly must not throw");
            assertTrue(scheduler.isShutdown() || scheduler.isTerminated(), "Scheduler must remain shut down after repeated calls");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 1, 5, 10 })
    @Order(3)
    void testShutdownIdempotentParameterized(int repetitions) throws Exception {
        resetSchedulerIfNeeded();
        for (int i = 0; i < repetitions; i++) {
            assertDoesNotThrow(TTLCache::shutdown);
        }
        assertTrue(scheduler.isShutdown() || scheduler.isTerminated(), "Scheduler should be shut down after repeated calls");
    }

    @Test
    @Order(4)
    void testConcurrentShutdown() throws Exception {
        resetSchedulerIfNeeded();
        int threadCount = 10;
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        try {
            for (int i = 0; i < threadCount; i++) {
                exec.submit(() -> {
                    // signal ready
                    ready.countDown();
                    try {
                        // wait for the go‑signal
                        start.await();
                    } catch (InterruptedException ignored) {
                    }
                    // each thread calls shutdown()
                    TTLCache.shutdown();
                });
            }
            // Wait until all threads are ready
            assertTrue(ready.await(2, TimeUnit.SECONDS), "All threads should be ready");
            // Release them simultaneously
            start.countDown();
            // give a moment for all shutdown calls to complete
            exec.shutdown();
            assertTrue(exec.awaitTermination(2, TimeUnit.SECONDS), "Executor should finish");
            // Scheduler must be shut down and no thread should have thrown
            assertTrue(scheduler.isShutdown() || scheduler.isTerminated(), "Scheduler should be shut down after concurrent calls");
        } finally {
            exec.shutdownNow();
        }
    }

    @Test
    @Order(5)
    void testConcurrentShutdownWithCacheOperation() throws Exception {
        resetSchedulerIfNeeded();
        TTLCache<String, String> cache = new TTLCache<>();
        // Thread that performs a cache operation (put)
        Thread putThread = new Thread(() -> cache.put("k", "v"));
        // Thread that shuts down the scheduler
        Thread shutdownThread = new Thread(TTLCache::shutdown);
        putThread.start();
        shutdownThread.start();
        putThread.join();
        shutdownThread.join();
        // Scheduler must be shut down
        assertTrue(scheduler.isShutdown() || scheduler.isTerminated(), "Scheduler should be shut down after concurrent operation");
        // The put either succeeded before shutdown or was silently ignored.
        // In either case the cache must still contain the entry (TTLCache does not
        // discard existing data on shutdown).
        assertEquals("v", cache.get("k"), "Cache entry should be present after shutdown");
    }

    @Test
    @Order(6)
    void testCacheOperationsAfterShutdown() throws Exception {
        resetSchedulerIfNeeded();
        TTLCache<String, Integer> cache = new TTLCache<>();
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        // Verify entries before shutdown
        assertEquals(3, cache.size());
        assertEquals(2, cache.get("b"));
        // Shut down the scheduler
        TTLCache.shutdown();
        assertTrue(scheduler.isShutdown() || scheduler.isTerminated(), "Scheduler must be shut down");
        // Cache operations must continue to work
        cache.put("d", 4);
        assertEquals(4, cache.size());
        assertEquals(4, cache.get("d"));
        assertEquals(1, cache.get("a"));
    }

    @Test
    @Order(7)
    void testSchedulingAfterShutdownThrowsRejectedExecutionException() throws Exception {
        resetSchedulerIfNeeded();
        // Populate cache to ensure scheduler exists
        TTLCache<String, String> cache = new TTLCache<>();
        cache.put("x", "y");
        // Shut down the scheduler
        TTLCache.shutdown();
        assertTrue(scheduler.isShutdown() || scheduler.isTerminated(), "Scheduler must be shut down");
        // Attempt to schedule a dummy task directly via the scheduler reference
        assertThrows(RejectedExecutionException.class, () -> scheduler.schedule(() -> {
        }, 10, TimeUnit.MILLISECONDS), "Scheduling after shutdown must throw RejectedExecutionException");
    }

    @Test
    @Order(8)
    void testReflectionReplaceSchedulerAfterShutdown() throws Exception {
        resetSchedulerIfNeeded();
        // Shut down the original scheduler
        TTLCache.shutdown();
        assertTrue(scheduler.isShutdown() || scheduler.isTerminated(), "Original scheduler must be shut down");
        // Attempt to replace the static field via reflection
        Field f = TTLCache.class.getDeclaredField("scheduler");
        f.setAccessible(true);
        // remove final modifier to allow setting a new instance
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        ScheduledExecutorService replacement = Executors.newSingleThreadScheduledExecutor();
        // The replacement should be allowed to be set, but the contract of TTLCache
        // expects the original executor to stay shut down. We verify that the
        // replacement is indeed set, but that its shutdown state is independent.
        f.set(null, replacement);
        ScheduledExecutorService current = (ScheduledExecutorService) f.get(null);
        assertSame(replacement, current, "Scheduler field should now reference the new executor");
        // However, the original shutdown state of the *previous* executor is unchanged.
        // The new executor is alive – this demonstrates that replacing the field is
        // possible via reflection but not recommended. The test simply ensures no
        // IllegalAccessException is thrown.
        // clean up
        replacement.shutdownNow();
    }

    @Test
    @Order(9)
    void testNullSchedulerThrowsNullPointerException() throws Exception {
        // Set the static scheduler field to null via reflection
        Field f = TTLCache.class.getDeclaredField("scheduler");
        f.setAccessible(true);
        // remove final modifier
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        f.set(null, null);
        // keep local reference in sync
        scheduler = null;
        // Expect NPE when shutdown() tries to invoke shutdown() on a null reference
        assertThrows(NullPointerException.class, TTLCache::shutdown, "Calling shutdown() with a null scheduler must throw NullPointerException");
        // Restore a functional scheduler for subsequent tests
        resetSchedulerIfNeeded();
    }

    @Test
    @Order(10)
    void testSchedulerShutdownThrowsRuntimeException() throws Exception {
        // Create a mock ScheduledExecutorService that throws when shutdown() is called
        ScheduledExecutorService mock = Mockito.mock(ScheduledExecutorService.class);
        Mockito.doThrow(new IllegalStateException("forced failure")).when(mock).shutdown();
        // Replace the static scheduler with the mock
        Field f = TTLCache.class.getDeclaredField("scheduler");
        f.setAccessible(true);
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        f.set(null, mock);
        scheduler = mock;
        // The exception should propagate out of TTLCache.shutdown()
        IllegalStateException ex = assertThrows(IllegalStateException.class, TTLCache::shutdown, "Shutdown should propagate RuntimeException from the scheduler");
        assertEquals("forced failure", ex.getMessage());
        // Restore a real scheduler for the rest of the suite
        resetSchedulerIfNeeded();
    }

    @Test
    @Order(11)
    void testHelperResetSchedulerWorks() throws Exception {
        // Force shutdown first
        TTLCache.shutdown();
        assertTrue(scheduler.isShutdown() || scheduler.isTerminated());
        // Reset should give us a fresh, alive scheduler
        resetSchedulerIfNeeded();
        assertNotNull(scheduler);
        assertFalse(scheduler.isShutdown() || scheduler.isTerminated(), "Scheduler must be alive after resetSchedulerIfNeeded");
    }
}
