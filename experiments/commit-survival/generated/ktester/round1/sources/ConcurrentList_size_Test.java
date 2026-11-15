package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Additional unit tests for {@link ConcurrentList#size()} that cover
 * the scenarios described in the JSON test‑case specification.
 *
 * <p>All existing @BeforeEach / @AfterEach logic from the original
 * test class is preserved – the {@code list} field is freshly created
 * before each test.</p>
 */
@ExtendWith(MockitoExtension.class)
class ConcurrentListSize_Test {

    /** The list instance used in each test. */
    private ConcurrentList<Integer> list;

    @BeforeEach
    void setupBeforeEach() {
        // default constructor creates an internal ArrayList wrapped with a lock
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        if (list != null) {
            list.clear();
        }
        list = null;
    }

    /* --------------------------------------------------------------------- */
    /*                     Basic functionality tests                         */
    /* --------------------------------------------------------------------- */

    @Test
    void size_isZeroWhenNew() {
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    void size_afterAddingThreeElements() {
        list.add(1);
        list.add(2);
        list.add(3);
        assertEquals(3, list.size());
    }

    @Test
    void size_afterAddThreeRemoveByIndex() {
        list.addAll(List.of(10, 20, 30));
        list.remove(0);               // remove first element
        assertEquals(2, list.size());
    }

    @Test
    void size_afterClear() {
        list.addAll(List.of(5, 6, 7));
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    void size_countsNullElements() {
        // Adding five nulls – List permits null entries.
        for (int i = 0; i < 5; i++) {
            list.add(null);
        }
        assertEquals(5, list.size());
    }

    /* --------------------------------------------------------------------- */
    /*                     Concurrency & boundary tests                     */
    /* --------------------------------------------------------------------- */

    @Test
    void size_duringConcurrentIteration() throws Exception {
        // Fill list with initial data
        list.addAll(List.of(1, 2, 3, 4, 5));

        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        // Thread 1 – iterates over the list
        exec.submit(() -> {
            try {
                startLatch.await(); // wait until both threads are ready
                for (Integer i : list) {
                    // just read – no modification
                }
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        // Thread 2 – adds elements concurrently
        exec.submit(() -> {
            try {
                startLatch.await();
                for (int i = 6; i <= 100; i++) {
                    list.add(i);
                }
            } finally {
                doneLatch.countDown();
            }
        });

        // Release both threads
        startLatch.countDown();
        // Wait for completion
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Threads should finish quickly");
        exec.shutdownNow();

        // Size must be the original 5 + 95 added = 100
        assertEquals(100, list.size());
    }

    @Test
    void size_afterConcurrentClearsNeverNegative() throws Exception {
        // Pre‑populate with some elements
        list.addAll(List.of(1, 2, 3, 4, 5));

        ExecutorService exec = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);

        // Four threads each clearing the list repeatedly
        for (int t = 0; t < 4; t++) {
            exec.submit(() -> {
                for (int i = 0; i < 1000; i++) {
                    list.clear();
                }
                latch.countDown();
            });
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Clearing threads should finish");
        exec.shutdownNow();

        // After all clears the size must be exactly 0 (never negative)
        assertEquals(0, list.size());
    }

    @Test
    void size_afterAddAtIndexZero() {
        list.add(0, 42);
        assertEquals(1, list.size());
        assertEquals(42, list.get(0));
    }

    @Test
    void size_returnsIntegerMaxValueWhenBackingListReportsIt() throws Exception {
        // Mock a List that pretends to contain Integer.MAX_VALUE elements
        @SuppressWarnings("unchecked")
        List<Integer> mockBacking = mock(List.class);
        when(mockBacking.size()).thenReturn(Integer.MAX_VALUE);
        // The mock should also behave as a proper List for other calls used by ConcurrentList
        when(mockBacking.iterator()).thenReturn(Collections.emptyIterator());

        // Use the constructor that wraps an existing list
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(mockBacking);
        assertEquals(Integer.MAX_VALUE, wrapped.size(),
                "size() must forward the backing list's Integer.MAX_VALUE without overflow");
    }

    @Test
    void size_becomesZeroAfterRemovingAllViaRemoveObject() {
        list.addAll(List.of(1, 2, 3, 4));
        // Remove each element individually using remove(Object)
        for (Integer v : List.of(1, 2, 3, 4)) {
            list.remove(v);
        }
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    /*                     Exception propagation tests                       */
    /* --------------------------------------------------------------------- */

    @Test
    void size_propagatesIllegalStateExceptionFromBackingList() throws Exception {
        @SuppressWarnings("unchecked")
        List<Integer> mock = mock(List.class);
        when(mock.size()).thenThrow(new IllegalStateException("forced"));
        ConcurrentList<Integer> cl = new ConcurrentList<>(mock);
        IllegalStateException ex = assertThrows(IllegalStateException.class, cl::size);
        assertEquals("forced", ex.getMessage());
    }

    @Test
    void size_propagatesUnsupportedOperationExceptionFromBackingList() throws Exception {
        @SuppressWarnings("unchecked")
        List<Integer> mock = mock(List.class);
        when(mock.size()).thenThrow(new UnsupportedOperationException("not supported"));
        ConcurrentList<Integer> cl = new ConcurrentList<>(mock);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, cl::size);
        assertEquals("not supported", ex.getMessage());
    }

    @Test
    void size_throwsNullPointerExceptionWhenInternalListIsNull() throws Exception {
        // Use reflection to set the private field 'list' to null
        Field f = ConcurrentList.class.getDeclaredField("list");
        f.setAccessible(true);
        f.set(list, null);
        assertThrows(NullPointerException.class, list::size);
    }

    @Test
    void size_throwsIllegalMonitorStateExceptionWhenLockFails() {
        // Create a subclass that overrides readOperation to simulate a lock failure
        class FaultyConcurrentList<E> extends ConcurrentList<E> {
            FaultyConcurrentList() { super(); }
            @Override
            protected <T> T readOperation(java.util.function.Supplier<T> operation) {
                // Simulate lock throwing IllegalMonitorStateException before delegating
                throw new IllegalMonitorStateException("lock failure");
            }
        }

        FaultyConcurrentList<Integer> faulty = new FaultyConcurrentList<>();
        IllegalMonitorStateException ex = assertThrows(IllegalMonitorStateException.class, faulty::size);
        assertEquals("lock failure", ex.getMessage());
    }

    @Test
    void size_surfacesRuntimeExceptionFromSupplier() {
        // Subclass that forces readOperation to throw a RuntimeException
        class RuntimeFailingConcurrentList<E> extends ConcurrentList<E> {
            RuntimeFailingConcurrentList() { super(); }
            @Override
            protected <T> T readOperation(java.util.function.Supplier<T> operation) {
                throw new RuntimeException("forced runtime");
            }
        }

        RuntimeFailingConcurrentList<Integer> failing = new RuntimeFailingConcurrentList<>();
        RuntimeException ex = assertThrows(RuntimeException.class, failing::size);
        assertEquals("forced runtime", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    /*                     Helper methods for parameterized tests            */
    /* --------------------------------------------------------------------- */

    // Example of a parameterized test for the basic functionality group.
    // Not required by the specification but demonstrates clean test design.
    private static Stream<Arguments> basicSizeScenarios() {
        return Stream.of(
                Arguments.of(List.of(), 0, "empty list"),
                Arguments.of(List.of(1, 2, 3), 3, "three elements"),
                Arguments.of(List.of(null, null, null, null, null), 5, "five nulls")
        );
    }

    @ParameterizedTest(name = "size of {2} should be {1}")
    @MethodSource("basicSizeScenarios")
    void size_parameterized_basic(List<Integer> initial, int expected, String description) {
        list.addAll(initial);
        assertEquals(expected, list.size(), description);
    }
}