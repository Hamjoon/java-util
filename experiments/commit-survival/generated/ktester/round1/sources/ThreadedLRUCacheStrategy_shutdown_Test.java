package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_shutdown_Test {

    /**
     * Holds the original scheduler so we can restore it after the tests.
     */
    private static ScheduledExecutorService originalScheduler;

    /**
     * Mock that will replace the private static scheduler field.
     */
    @Mock
    private ScheduledExecutorService mockScheduler;

    /**
     * Replace the private static {@code scheduler} field with a Mockito mock before any test runs.
     */
    @BeforeAll
    static void setupBeforeAll() throws Exception {
        Field schedulerField = ThreadedLRUCacheStrategy.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        // Save the real scheduler so we can put it back later.
        originalScheduler = (ScheduledExecutorService) schedulerField.get(null);
    }

    /**
     * Inject the mock scheduler before each test and clear any thread‑interrupt flag.
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Clear possible interrupt status from previous tests.
        Thread.interrupted();
        // Install the mock into the static field.
        Field schedulerField = ThreadedLRUCacheStrategy.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(null, mockScheduler);
    }

    /**
     * After each test, reset the mock interactions to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        reset(mockScheduler);
    }

    /**
     * Restore the original scheduler after all tests have finished.
     */
    @AfterAll
    static void teardownAfterAll() throws Exception {
        Field schedulerField = ThreadedLRUCacheStrategy.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(null, originalScheduler);
    }

    @Test
    void shutdown_CompletesNormally_WhenAwaitTerminationReturnsTrue() throws Exception {
        // GIVEN – the scheduler terminates within the timeout
        when(mockScheduler.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        // WHEN
        ThreadedLRUCacheStrategy.shutdown();
        // THEN – shutdown was requested, awaitTermination was called, and shutdownNow was NOT called
        verify(mockScheduler, times(1)).shutdown();
        verify(mockScheduler, times(1)).awaitTermination(eq(5L), eq(TimeUnit.SECONDS));
        verify(mockScheduler, never()).shutdownNow();
        // No interruption should be set on the thread
        assertFalse(Thread.currentThread().isInterrupted(), "Thread should not be interrupted when awaitTermination succeeds");
    }

    @Test
    void shutdown_TriggersShutdownNow_WhenAwaitTerminationReturnsFalse() throws Exception {
        // GIVEN – the scheduler does NOT terminate within the timeout
        when(mockScheduler.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);
        // WHEN
        ThreadedLRUCacheStrategy.shutdown();
        // THEN – shutdownNow is invoked after the timeout
        verify(mockScheduler, times(1)).shutdown();
        verify(mockScheduler, times(1)).awaitTermination(eq(5L), eq(TimeUnit.SECONDS));
        verify(mockScheduler, times(1)).shutdownNow();
        assertFalse(Thread.currentThread().isInterrupted(), "Thread should not be interrupted when awaitTermination returns false");
    }

    @Test
    void shutdown_HandlesInterruptedException_AndReinterruptsThread() throws Exception {
        // GIVEN – awaitTermination throws InterruptedException
        when(mockScheduler.awaitTermination(anyLong(), any(TimeUnit.class))).thenThrow(new InterruptedException("simulated interruption"));
        // WHEN
        ThreadedLRUCacheStrategy.shutdown();
        // THEN – shutdownNow is called and the current thread is re‑interrupted
        verify(mockScheduler, times(1)).shutdown();
        verify(mockScheduler, times(1)).awaitTermination(eq(5L), eq(TimeUnit.SECONDS));
        verify(mockScheduler, times(1)).shutdownNow();
        assertTrue(Thread.interrupted(), "Thread should be re‑interrupted after catching InterruptedException");
    }

    @Test
    void shutdown_MultipleCalls_RespectSameLogicEachTime() throws Exception {
        // GIVEN – awaitTermination returns true each time (graceful shutdown)
        when(mockScheduler.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(true);
        // WHEN – call shutdown twice
        ThreadedLRUCacheStrategy.shutdown();
        ThreadedLRUCacheStrategy.shutdown();
        // THEN – each call should invoke shutdown() and awaitTermination() once,
        //        shutdownNow() never invoked.
        verify(mockScheduler, times(2)).shutdown();
        verify(mockScheduler, times(2)).awaitTermination(eq(5L), eq(TimeUnit.SECONDS));
        verify(mockScheduler, never()).shutdownNow();
    }

    @Test
    void shutdown_PropagatesSecurityException_FromShutdown() throws Exception {
        // GIVEN – scheduler.shutdown() throws a SecurityException
        doThrow(new SecurityException("shutdown denied")).when(mockScheduler).shutdown();
        // WHEN / THEN – the exception propagates out of the method
        SecurityException ex = assertThrows(SecurityException.class, ThreadedLRUCacheStrategy::shutdown, "Expected SecurityException to be thrown from shutdown()");
        assertEquals("shutdown denied", ex.getMessage());
        // Verify that awaitTermination and shutdownNow were never called
        verify(mockScheduler, never()).awaitTermination(anyLong(), any(TimeUnit.class));
        verify(mockScheduler, never()).shutdownNow();
    }

    @Test
    void shutdown_PropagatesSecurityException_FromAwaitTermination() throws Exception {
        // GIVEN – shutdown succeeds, but awaitTermination throws SecurityException
        when(mockScheduler.awaitTermination(anyLong(), any(TimeUnit.class))).thenThrow(new SecurityException("await denied"));
        // WHEN / THEN – the exception propagates
        SecurityException ex = assertThrows(SecurityException.class, ThreadedLRUCacheStrategy::shutdown, "Expected SecurityException to be thrown from awaitTermination()");
        assertEquals("await denied", ex.getMessage());
        // Verify that shutdownNow was not called after the exception
        verify(mockScheduler, times(1)).shutdown();
        verify(mockScheduler, times(1)).awaitTermination(eq(5L), eq(TimeUnit.SECONDS));
        verify(mockScheduler, never()).shutdownNow();
    }

    @Test
    void shutdown_PropagatesSecurityException_FromShutdownNow() throws Exception {
        // GIVEN – awaitTermination returns false, then shutdownNow throws SecurityException
        when(mockScheduler.awaitTermination(anyLong(), any(TimeUnit.class))).thenReturn(false);
        doThrow(new SecurityException("shutdownNow denied")).when(mockScheduler).shutdownNow();
        // WHEN / THEN – the exception propagates
        SecurityException ex = assertThrows(SecurityException.class, ThreadedLRUCacheStrategy::shutdown, "Expected SecurityException to be thrown from shutdownNow()");
        assertEquals("shutdownNow denied", ex.getMessage());
        // Verify the sequence of calls up to the point of failure
        verify(mockScheduler, times(1)).shutdown();
        verify(mockScheduler, times(1)).awaitTermination(eq(5L), eq(TimeUnit.SECONDS));
        verify(mockScheduler, times(1)).shutdownNow();
    }
}
