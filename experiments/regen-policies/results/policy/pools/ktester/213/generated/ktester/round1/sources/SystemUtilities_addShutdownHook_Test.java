package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class SystemUtilities_addShutdownHook_Test {

    @Test
    void addShutdownHook_RegistersHookAndExecutesIt() {
        // Arrange – a simple runnable that flips a flag
        AtomicBoolean ran = new AtomicBoolean(false);
        Runnable hook = () -> ran.set(true);
        // Mock Runtime and its static accessor
        Runtime mockRuntime = mock(Runtime.class);
        try (MockedStatic<Runtime> mockedRuntime = mockStatic(Runtime.class)) {
            mockedRuntime.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // Act – call the method under test
            SystemUtilities.addShutdownHook(hook);
            // Assert – verify that Runtime.addShutdownHook was called with a Thread
            ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime, times(1)).addShutdownHook(threadCaptor.capture());
            Thread shutdownThread = threadCaptor.getValue();
            assertNotNull(shutdownThread, "Shutdown hook thread should not be null");
            // The thread should not have been started yet; running it manually must invoke the hook
            // simulate JVM shutdown
            shutdownThread.run();
            assertTrue(ran.get(), "The provided hook must have been executed");
        }
    }

    @Test
    void addShutdownHook_HandlesExceptionFromHook() {
        // Arrange – a runnable that throws an exception
        Runnable faultyHook = mock(Runnable.class);
        doThrow(new RuntimeException("boom")).when(faultyHook).run();
        // Mock Runtime
        Runtime mockRuntime = mock(Runtime.class);
        try (MockedStatic<Runtime> mockedRuntime = mockStatic(Runtime.class)) {
            mockedRuntime.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // Act – invoke the method; no exception should propagate
            assertDoesNotThrow(() -> SystemUtilities.addShutdownHook(faultyHook));
            // Capture the thread that was registered
            ArgumentCaptor<Thread> threadCaptor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime, times(1)).addShutdownHook(threadCaptor.capture());
            Thread shutdownThread = threadCaptor.getValue();
            assertNotNull(shutdownThread, "Shutdown hook thread should not be null");
            // Execute the thread – the exception should be caught internally
            assertDoesNotThrow(shutdownThread::run, "Exception from hook must be caught inside the shutdown thread");
            // Verify that the hook's run method was indeed called
            verify(faultyHook, times(1)).run();
        }
    }

    @Test
    void addShutdownHook_NullRunnable_IsCaughtAsException() {
        // Arrange – null hook
        Runnable nullHook = null;
        // Mock Runtime
        Runtime mockRuntime = mock(Runtime.class);
        try (MockedStatic<Runtime> mockedRuntime = mockStatic(Runtime.class)) {
            mockedRuntime.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // Act – register the null hook (no exception should be thrown here)
            SystemUtilities.addShutdownHook(nullHook);
            // Capture the registered thread
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime).addShutdownHook(captor.capture());
            Thread shutdownThread = captor.getValue();
            // Capture printed stack trace
            ByteArrayOutputStream errContent = new ByteArrayOutputStream();
            PrintStream originalErr = System.err;
            System.setErr(new PrintStream(errContent));
            try {
                // Run the thread – NullPointerException should be caught and printed
                assertDoesNotThrow(shutdownThread::run);
            } finally {
                System.setErr(originalErr);
            }
            String printed = errContent.toString();
            assertTrue(printed.contains("NullPointerException"), "NullPointerException stack trace should be printed");
        }
    }

    @Test
    void addShutdownHook_AssertionError_IsNotCaught() {
        // Arrange – a hook that throws AssertionError
        Runnable errorHook = () -> {
            throw new AssertionError("assertion failed");
        };
        // Mock Runtime
        Runtime mockRuntime = mock(Runtime.class);
        try (MockedStatic<Runtime> mockedRuntime = mockStatic(Runtime.class)) {
            mockedRuntime.when(Runtime::getRuntime).thenReturn(mockRuntime);
            SystemUtilities.addShutdownHook(errorHook);
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime).addShutdownHook(captor.capture());
            Thread shutdownThread = captor.getValue();
            // When we invoke run() directly, the AssertionError propagates out
            AssertionError thrown = assertThrows(AssertionError.class, shutdownThread::run, "AssertionError should not be caught by the wrapper");
            assertEquals("assertion failed", thrown.getMessage());
        }
    }

    @Test
    void addShutdownHook_RunnableAddsToList_SideEffectObserved() {
        // Arrange – shared mutable list
        List<String> shared = new ArrayList<>();
        Runnable hook = () -> shared.add("executed");
        // Mock Runtime
        Runtime mockRuntime = mock(Runtime.class);
        try (MockedStatic<Runtime> mockedRuntime = mockStatic(Runtime.class)) {
            mockedRuntime.when(Runtime::getRuntime).thenReturn(mockRuntime);
            SystemUtilities.addShutdownHook(hook);
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime).addShutdownHook(captor.capture());
            Thread shutdownThread = captor.getValue();
            // Act
            shutdownThread.run();
            // Assert
            assertEquals(1, shared.size());
            assertEquals("executed", shared.get(0));
        }
    }

    @Test
    void addShutdownHook_SleepsAndSetsFlag_NonDaemonThreadCompletes() {
        // Arrange – runnable that sleeps then sets flag
        AtomicBoolean flag = new AtomicBoolean(false);
        Runnable hook = () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            flag.set(true);
        };
        Runtime mockRuntime = mock(Runtime.class);
        try (MockedStatic<Runtime> mockedRuntime = mockStatic(Runtime.class)) {
            mockedRuntime.when(Runtime::getRuntime).thenReturn(mockRuntime);
            SystemUtilities.addShutdownHook(hook);
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime).addShutdownHook(captor.capture());
            Thread shutdownThread = captor.getValue();
            // Ensure the thread is not a daemon (shutdown hooks must be non‑daemon)
            assertFalse(shutdownThread.isDaemon(), "Shutdown hook thread must be non‑daemon");
            // Act
            shutdownThread.run();
            // Assert
            assertTrue(flag.get(), "Flag should be true after hook execution");
        }
    }

    @Test
    void addShutdownHook_ErrorIsNotCaughtByWrapper() {
        // Arrange – a hook that throws a plain Error
        Runnable errorHook = () -> {
            throw new Error("critical");
        };
        Runtime mockRuntime = mock(Runtime.class);
        try (MockedStatic<Runtime> mockedRuntime = mockStatic(Runtime.class)) {
            mockedRuntime.when(Runtime::getRuntime).thenReturn(mockRuntime);
            SystemUtilities.addShutdownHook(errorHook);
            ArgumentCaptor<Thread> captor = ArgumentCaptor.forClass(Thread.class);
            verify(mockRuntime).addShutdownHook(captor.capture());
            Thread shutdownThread = captor.getValue();
            // When invoking run() directly, the Error propagates out (not caught)
            Error thrown = assertThrows(Error.class, shutdownThread::run, "Error should not be caught by the wrapper");
            assertEquals("critical", thrown.getMessage());
        }
    }

    @Test
    void addShutdownHook_RuntimeThrowsSecurityException_Propagates() {
        // Arrange – mock Runtime.addShutdownHook to throw SecurityException
        Runtime mockRuntime = mock(Runtime.class);
        doThrow(new SecurityException("no permission")).when(mockRuntime).addShutdownHook(any(Thread.class));
        try (MockedStatic<Runtime> mockedRuntime = mockStatic(Runtime.class)) {
            mockedRuntime.when(Runtime::getRuntime).thenReturn(mockRuntime);
            // Act & Assert – the SecurityException should propagate out of the method
            SecurityException se = assertThrows(SecurityException.class, () -> SystemUtilities.addShutdownHook(() -> {
            }), "SecurityException should propagate from addShutdownHook");
            assertEquals("no permission", se.getMessage());
        }
    }
}
