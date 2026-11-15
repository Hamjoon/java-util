package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_addShutdownHook_Test {

    @Test
    void testAddShutdownHookExecutesProvidedRunnable() {
        // GIVEN
        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable hook = executed::set;
        // Mock the static Runtime.getRuntime() call
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // WHEN
            SystemUtilities.addShutdownHook(hook);
            // Capture the Thread passed to Runtime.addShutdownHook(...)
            ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime, times(1)).addShutdownHook(threadCaptor.capture());
            Thread shutdownThread = threadCaptor.getValue();
            assertNotNull(shutdownThread, "Shutdown hook thread should not be null");
            // Simulate JVM shutdown by invoking the thread's run method
            shutdownThread.run();
            // THEN
            assertTrue(executed.get(), "The provided hook should have been executed");
        }
    }

    @Test
    void testAddShutdownHookSwallowsExceptionsFromRunnable() {
        // GIVEN
        Runnable faultyHook = () -> {
            throw new RuntimeException("Intentional failure");
        };
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // WHEN – the method should not propagate the exception
            assertDoesNotThrow(() -> SystemUtilities.addShutdownHook(faultyHook));
            // Capture the thread
            ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime, times(1)).addShutdownHook(threadCaptor.capture());
            Thread shutdownThread = threadCaptor.getValue();
            assertNotNull(shutdownThread, "Shutdown hook thread should not be null");
            // THEN – running the thread must not throw
            assertDoesNotThrow(shutdownThread::run, "Exceptions thrown by the hook must be caught inside the shutdown hook");
        }
    }

    /* -------------------------------------------------------------
     *  Normal execution – side‑effects verification
     * ------------------------------------------------------------- */
    @Test
    void testAddShutdownHookExecutesRunnableSettingAtomicBoolean() {
        // GIVEN
        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable hook = executed::set;
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // WHEN
            SystemUtilities.addShutdownHook(hook);
            // Capture the Thread that was registered
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime, times(1)).addShutdownHook(captor.capture());
            Thread shutdownThread = captor.getValue();
            // Simulate JVM shutdown
            shutdownThread.run();
            // THEN
            assertTrue(executed.get(), "The provided hook should have been executed");
        }
    }

    @Test
    void testAddShutdownHookExecutesRunnableAppendingToList() {
        // GIVEN
        List<String> shared = new ArrayList<>();
        Runnable hook = () -> shared.add("hook‑executed");
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // WHEN
            SystemUtilities.addShutdownHook(hook);
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime).addShutdownHook(captor.capture());
            Thread shutdownThread = captor.getValue();
            shutdownThread.run();
            // THEN
            assertEquals(1, shared.size(), "The list should contain exactly one element");
            assertEquals("hook‑executed", shared.get(0), "The element should be the string added by the hook");
        }
    }

    /* -------------------------------------------------------------
     *  Null hook handling
     * ------------------------------------------------------------- */
    @Test
    void testAddShutdownHookWithNullRunnableDoesNotPropagateException() {
        // GIVEN
        Runnable nullHook = null;
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class);
            MockedStatic<System> systemMock = mockStatic(System.class)) {
            // Capture printed stack trace to avoid cluttering test output
            PrintStream originalErr = System.err;
            PrintStream mockErr = mock(PrintStream.class);
            systemMock.when(() -> System.setErr(any())).thenAnswer(i -> {
                // restore after test
                System.setErr(originalErr);
                return null;
            });
            System.setErr(mockErr);
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // WHEN – registration must succeed
            assertDoesNotThrow(() -> SystemUtilities.addShutdownHook(nullHook));
            // Capture the Thread
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime).addShutdownHook(captor.capture());
            Thread shutdownThread = captor.getValue();
            // THEN – running the thread must swallow NPE and not throw
            assertDoesNotThrow(shutdownThread::run, "NullPointerException from a null hook should be caught inside the shutdown thread");
            // Verify that a stack trace was attempted to be printed (best‑effort)
            verify(mockErr, atLeastOnce()).println(anyString());
        }
    }

    /* -------------------------------------------------------------
     *  Exception handling within the hook
     * ------------------------------------------------------------- */
    @Test
    void testAddShutdownHookSwallowsRuntimeExceptionFromHook() {
        // GIVEN
        Runnable faultyHook = () -> {
            throw new RuntimeException("boom");
        };
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class);
            MockedStatic<System> systemMock = mockStatic(System.class)) {
            PrintStream mockErr = mock(PrintStream.class);
            System.setErr(mockErr);
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // WHEN – registration must not throw
            assertDoesNotThrow(() -> SystemUtilities.addShutdownHook(faultyHook));
            // Capture the thread
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime).addShutdownHook(captor.capture());
            Thread shutdownThread = captor.getValue();
            // THEN – executing the thread must not propagate the exception
            assertDoesNotThrow(shutdownThread::run, "Exceptions thrown by the hook must be caught inside the shutdown thread");
            // Verify that the exception's stack trace was printed
            verify(mockErr, atLeastOnce()).println(anyString());
        }
    }

    /* -------------------------------------------------------------
     *  Multiple shutdown hooks registration
     * ------------------------------------------------------------- */
    @Test
    void testMultipleShutdownHooksAreRegisteredAndExecutedIndependently() {
        // GIVEN
        AtomicInteger counter = new AtomicInteger(0);
        Runnable first = counter::incrementAndGet;
        Runnable second = counter::incrementAndGet;
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // Register both hooks
            SystemUtilities.addShutdownHook(first);
            SystemUtilities.addShutdownHook(second);
            // Capture both registered threads
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime, times(2)).addShutdownHook(captor.capture());
            List<Thread> threads = captor.getAllValues();
            // Simulate shutdown by running each thread
            threads.forEach(Thread::run);
            // THEN
            assertEquals(2, counter.get(), "Both hooks should have been executed, incrementing the counter twice");
        }
    }

    /* -------------------------------------------------------------
     *  Hook execution does not affect original thread (ThreadLocal)
     * ------------------------------------------------------------- */
    @Test
    void testHookRunsInSeparateThreadLocalContext() throws Exception {
        // GIVEN
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        Runnable hook = () -> threadLocal.set("hook");
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            SystemUtilities.addShutdownHook(hook);
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime).addShutdownHook(captor.capture());
            Thread shutdownThread = captor.getValue();
            // Ensure the test thread's ThreadLocal is null before shutdown
            assertNull(threadLocal.get(), "ThreadLocal should be null in the registering thread");
            // WHEN – run the shutdown thread
            shutdownThread.run();
            // THEN – the value is set only in the shutdown thread, not in this thread
            assertNull(threadLocal.get(), "ThreadLocal in the original thread must remain unchanged");
        }
    }

    /* -------------------------------------------------------------
     *  Invalid hook type handling (Error not caught)
     * ------------------------------------------------------------- */
    @Test
    void testHookThrowingErrorIsNotCaughtByExceptionBlock() {
        // GIVEN
        Runnable errorHook = () -> {
            throw new AssertionError("error");
        };
        try (MockedStatic<Runtime> runtimeMock = mockStatic(Runtime.class)) {
            Runtime mockRuntime = mock(Runtime.class);
            runtimeMock.when(Runtime::getRuntime).thenReturn(mockRuntime);
            SystemUtilities.addShutdownHook(errorHook);
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime).addShutdownHook(captor.capture());
            Thread shutdownThread = captor.getValue();
            // WHEN – running the thread should propagate the AssertionError
            AssertionError thrown = assertThrows(AssertionError.class, shutdownThread::run, "Errors (subclass of Throwable but not Exception) are not caught by the generic catch block");
            // THEN
            assertEquals("error", thrown.getMessage(), "The original error message should be preserved");
        }
    }
}
