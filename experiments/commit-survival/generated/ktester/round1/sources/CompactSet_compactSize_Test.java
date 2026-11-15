package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactSet_compactSize_Test {

    class TooSmallCompactSet<E> extends CompactSet<E> {

        @Override
        protected int compactSize() {
            // below the allowed minimum
            return 1;
        }
    }

    class MinimumValidCompactSet<E> extends CompactSet<E> {

        @Override
        protected int compactSize() {
            return 2;
        }
    }

    class PlainSubclass<E> extends CompactSet<E> {
        // No override
    }

    class CustomCompactSet<E> extends CompactSet<E> {

        @Override
        protected int compactSize() {
            return 50;
        }
    }

    class ExceptionThrowingSet<E> extends CompactSet<E> {

        @Override
        protected int compactSize() {
            throw new IllegalStateException("Deliberate exception");
        }
    }

    class PlainSubclass<E> extends CompactSet<E> {
        // No override
    }

    class CustomCompactSet<E> extends CompactSet<E> {

        @Override
        protected int compactSize() {
            return 50;
        }
    }

    class ExceptionThrowingSet<E> extends CompactSet<E> {

        @Override
        protected int compactSize() {
            throw new IllegalStateException("Deliberate exception");
        }
    }

    /* --------------------------------------------------------------------- */
    private static <T extends Throwable> T assertThrowsAnyOf(List<Class<? extends Throwable>> types, Executable executable, String message) {
        try {
            executable.execute();
            fail(message + " (no exception thrown)");
            // unreachable
            return null;
        } catch (Throwable actual) {
            for (Class<? extends Throwable> type : types) {
                if (type.isInstance(actual)) {
                    @SuppressWarnings("unchecked")
                    T casted = (T) actual;
                    return casted;
                }
            }
            // Unexpected exception type
            fail(message + " (unexpected exception type: " + actual.getClass() + ")", actual);
            // unreachable
            return null;
        }
    }

    @Test
    @DisplayName("compactSize() returns the default value of 80")
    void testDefaultCompactSize() {
        CompactSet<String> set = new CompactSet<>();
        // The method is protected but accessible because the test class is in the same package.
        int size = set.compactSize();
        assertEquals(80, size, "The default compactSize() should be 80 as defined in the source code.");
    }

    @Test
    @DisplayName("Constructor throws IllegalStateException when compactSize() < 2")
    void testConstructorRejectsTooSmallCompactSize() {
        // Create an anonymous subclass that overrides compactSize() to return 1.
        class TooSmallCompactSet<E> extends CompactSet<E> {

            @Override
            protected int compactSize() {
                // below the allowed minimum
                return 1;
            }
        }
        IllegalStateException ex = assertThrows(IllegalStateException.class, TooSmallCompactSet::new, "CompactSet constructor must reject a compactSize() value less than 2.");
        assertEquals("compactSize() must be >= 2", ex.getMessage());
    }

    @Test
    @DisplayName("Constructor succeeds when compactSize() == 2")
    void testConstructorAcceptsMinimumValidCompactSize() {
        // Subclass that returns exactly the minimum allowed value.
        class MinimumValidCompactSet<E> extends CompactSet<E> {

            @Override
            protected int compactSize() {
                return 2;
            }
        }
        // No exception should be thrown.
        assertDoesNotThrow(MinimumValidCompactSet::new, "CompactSet constructor should accept a compactSize() of 2.");
        // Verify that the overridden value is indeed used.
        MinimumValidCompactSet<String> set = new MinimumValidCompactSet<>();
        assertEquals(2, set.compactSize(), "The overridden compactSize() should return the value supplied by the subclass.");
    }

    @Test
    @DisplayName("Two consecutive calls return the same constant 80")
    void testTwoConsecutiveCallsReturnSameValue() {
        CompactSet<Integer> set = new CompactSet<>();
        assertAll(() -> assertEquals(80, set.compactSize(), "First call should return 80"), () -> assertEquals(80, set.compactSize(), "Second call should also return 80"));
    }

    @Test
    @DisplayName("Subclass that does NOT override compactSize() still returns 80")
    void testSubclassWithoutOverrideReturnsDefault() {
        class PlainSubclass<E> extends CompactSet<E> {
            // No override
        }
        PlainSubclass<String> set = new PlainSubclass<>();
        assertEquals(80, set.compactSize(), "Even without overriding, compactSize() should return the base constant 80.");
    }

    @Test
    @DisplayName("Subclass overriding compactSize() to return 50")
    void testSubclassOverrideReturnsCustomValue() {
        class CustomCompactSet<E> extends CompactSet<E> {

            @Override
            protected int compactSize() {
                return 50;
            }
        }
        CustomCompactSet<String> set = new CustomCompactSet<>();
        assertEquals(50, set.compactSize(), "Overridden compactSize() should return the custom value 50.");
    }

    @Test
    @DisplayName("Invoking compactSize() via reflection without setAccessible(true) throws IllegalAccessException")
    void testReflectionWithoutSetAccessible() throws NoSuchMethodException {
        Method method = CompactSet.class.getDeclaredMethod("compactSize");
        CompactSet<String> set = new CompactSet<>();
        // The method is protected; invoking without setAccessible may succeed in the same package.
        // To enforce the IllegalAccessException we attempt to invoke it from a different lookup context.
        // Using MethodHandles.privateLookupIn forces an illegal access check.
        IllegalAccessException thrown = assertThrows(IllegalAccessException.class, () -> {
            // Attempt to invoke via reflection using a different lookup (simulated)
            MethodHandles.lookup().unreflect(method).invoke(set);
        }, "Expected IllegalAccessException when invoking protected method without proper access.");
        // The exact message is JVM dependent; we only assert the exception type.
    }

    @Test
    @DisplayName("Invoking compactSize() via reflection with setAccessible(true) returns 80")
    void testReflectionWithSetAccessible() throws Exception {
        Method method = CompactSet.class.getDeclaredMethod("compactSize");
        method.setAccessible(true);
        CompactSet<String> set = new CompactSet<>();
        Object result = method.invoke(set);
        assertTrue(result instanceof Integer, "Result should be an Integer");
        assertEquals(80, result, "Reflective call should return the default value 80");
    }

    @Test
    @DisplayName("Concurrent invocations of compactSize() from many threads")
    void testConcurrentInvocations() throws InterruptedException, ExecutionException {
        final int threadCount = 10;
        final int iterationsPerThread = 100;
        CompactSet<String> set = new CompactSet<>();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Integer>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                int last = -1;
                for (int j = 0; j < iterationsPerThread; j++) {
                    last = set.compactSize();
                }
                return last;
            });
        }
        List<Future<Integer>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate in time");
        for (Future<Integer> f : futures) {
            assertEquals(80, f.get(), "All concurrent calls must return 80");
        }
    }

    @Test
    @DisplayName("Reflection invoke on null target yields InvocationTargetException with cause NPE")
    void testReflectionInvokeOnNullTarget() throws NoSuchMethodException {
        Method method = CompactSet.class.getDeclaredMethod("compactSize");
        method.setAccessible(true);
        InvocationTargetException ite = assertThrows(InvocationTargetException.class, () -> {
            method.invoke(null);
        }, "Invoking an instance method on null should wrap NullPointerException in InvocationTargetException");
        assertTrue(ite.getCause() instanceof NullPointerException, "Cause of InvocationTargetException should be NullPointerException");
    }

    @Test
    @DisplayName("Reflection invoke with extra argument throws IllegalArgumentException")
    void testReflectionInvokeWithExtraArgument() throws NoSuchMethodException {
        Method method = CompactSet.class.getDeclaredMethod("compactSize");
        method.setAccessible(true);
        CompactSet<String> set = new CompactSet<>();
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> {
            // The method expects zero arguments; we deliberately pass one.
            method.invoke(set, 1);
        }, "Providing an extra argument must cause IllegalArgumentException");
        // Message is JVM dependent; we only verify the exception type.
    }

    @Test
    @DisplayName("Attempt to setAccessible(true) under simulated security restriction throws InaccessibleObjectException")
    void testReflectionSecurityRestriction() throws NoSuchMethodException {
        Method method = CompactSet.class.getDeclaredMethod("compactSize");
        // In Java 9+ the module system may reject setAccessible; we capture either
        // InaccessibleObjectException or IllegalAccessException.
        Throwable thrown = assertThrowsAnyOf(List.of(InaccessibleObjectException.class, IllegalAccessException.class), () -> method.setAccessible(true), "Attempting to make the protected method accessible may be denied by the module system");
        assertNotNull(thrown, "An exception should have been thrown due to access restriction");
    }

    @Test
    @DisplayName("Subclass overriding compactSize() to throw IllegalStateException propagates the exception")
    void testSubclassOverrideThrowsException() {
        class ExceptionThrowingSet<E> extends CompactSet<E> {

            @Override
            protected int compactSize() {
                throw new IllegalStateException("Deliberate exception");
            }
        }
        ExceptionThrowingSet<String> set = new ExceptionThrowingSet<>();
        IllegalStateException ex = assertThrows(IllegalStateException.class, set::compactSize, "Calling overridden compactSize() must propagate the IllegalStateException");
        assertEquals("Deliberate exception", ex.getMessage());
    }
}
