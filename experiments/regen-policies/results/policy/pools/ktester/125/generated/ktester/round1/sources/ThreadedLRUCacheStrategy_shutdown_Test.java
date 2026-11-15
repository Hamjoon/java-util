package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_shutdown_Test {

    /**
     * The original scheduler that belongs to the class under test – saved so we can restore it after all tests.
     */
    private static ScheduledExecutorService originalScheduler;

    /**
     * Mocked scheduler that will replace the static final field during each test.
     */
    @Mock
    private ScheduledExecutorService mockScheduler;

    /**
     * Save the original scheduler before any tests run.
     */
    @BeforeAll
    static void setupBeforeAll() throws Exception {
        Field schedulerField = ThreadedLRUCacheStrategy.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        originalScheduler = (ScheduledExecutorService) schedulerField.get(null);
        Field schedulerField = ThreadedLRUCacheStrategy.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        originalScheduler = (ScheduledExecutorService) schedulerField.get(null);
    }

    /**
     * Replace the static scheduler with a Mockito mock before each test.
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        Field schedulerField = ThreadedLRUCacheStrategy.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(null, mockScheduler);
        Field schedulerField = ThreadedLRUCacheStrategy.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(null, mockScheduler);
    }

    /**
     * Clear the interrupt flag on the current thread after each test to avoid cross‑test contamination.
     */
    @AfterEach
    void teardownAfterEach() {
        // Clears the interrupt status if it was set during a test.
        Thread.interrupted();
        // Ensure the thread's interrupt status does not leak between tests.
        Thread.interrupted();
    }

    /**
     * Restore the original scheduler after all tests have finished.
     */
    @AfterAll
    static void teardownAfterAll() throws Exception {
        Field schedulerField = ThreadedLRUCacheStrategy.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(null, originalScheduler);
        Field schedulerField = ThreadedLRUCacheStrategy.class.getDeclaredField("scheduler");
        schedulerField.setAccessible(true);
        schedulerField.set(null, originalScheduler);
    }

    @Test
    void shutdown_normalTermination_callsShutdownOnly() throws Exception {
        // Arrange – executor terminates gracefully within the timeout.
        when(mockScheduler.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(true);
        // Act
        ThreadedLRUCacheStrategy.shutdown();
        // Assert – shutdown() is invoked, shutdownNow() never invoked.
        verify(mockScheduler).shutdown();
        verify(mockScheduler, never()).shutdownNow();
    }

    @Test
    void shutdown_timeout_callsShutdownNow() throws Exception {
        // Arrange – executor does not terminate within the timeout.
        when(mockScheduler.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(false);
        // Act
        ThreadedLRUCacheStrategy.shutdown();
        // Assert – both shutdown() and forced shutdownNow() are called.
        verify(mockScheduler).shutdown();
        verify(mockScheduler).shutdownNow();
    }

    @Test
    void shutdown_interruptedException_callsShutdownNowAndInterruptsThread() throws Exception {
        // Arrange – awaitTermination throws InterruptedException.
        when(mockScheduler.awaitTermination(5, TimeUnit.SECONDS)).thenThrow(new InterruptedException());
        // Act
        ThreadedLRUCacheStrategy.shutdown();
        // Assert – shutdownNow() is called and the thread's interrupt flag is set.
        verify(mockScheduler).shutdown();
        verify(mockScheduler).shutdownNow();
        assertTrue(Thread.currentThread().isInterrupted(), "Thread should be interrupted when awaitTermination throws InterruptedException");
    }

    @Test
    void shutdown_idempotentCalls_firstTimeoutSecondSuccess() throws Exception {
        // Arrange – first call returns false (forcing shutdownNow), second call returns true.
        when(mockScheduler.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(// first invocation
        false).thenReturn(// second invocation
        true);
        // Act – invoke shutdown twice.
        // first call
        ThreadedLRUCacheStrategy.shutdown();
        // second call
        ThreadedLRUCacheStrategy.shutdown();
        // Assert – shutdown() called twice, shutdownNow() only once (first call).
        verify(mockScheduler, times(2)).shutdown();
        verify(mockScheduler, times(1)).shutdownNow();
    }

    @Test
    void shutdown_alreadyTerminated_noShutdownNow() throws Exception {
        // Arrange – simulate that the scheduler is already terminated; awaitTermination returns true.
        when(mockScheduler.awaitTermination(5, TimeUnit.SECONDS)).thenReturn(true);
        // Act – call shutdown twice to emulate repeated shutdown after termination.
        ThreadedLRUCacheStrategy.shutdown();
        ThreadedLRUCacheStrategy.shutdown();
        // Assert – shutdown() called twice, shutdownNow() never called.
        verify(mockScheduler, times(2)).shutdown();
        verify(mockScheduler, never()).shutdownNow();
    }
}
