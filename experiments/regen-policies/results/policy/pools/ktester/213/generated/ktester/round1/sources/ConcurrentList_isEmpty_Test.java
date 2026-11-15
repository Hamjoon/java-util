package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_isEmpty_Test {

    /**
     * Instance used for the majority of the tests.
     */
    private ConcurrentList<Integer> list;

    /**
     * A backing list used to test the wrapper‑mode constructor.
     */
    private List<Integer> backingList;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ConcurrentList for each test to guarantee isolation.
        list = new ConcurrentList<>();
        // Prepare a mutable backing list for the wrapper‑mode tests.
        backingList = new ArrayList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references – helps the GC when many tests run.
        list = null;
        backingList = null;
    }

    @Test
    void emptyListShouldReportIsEmpty() {
        assertTrue(list.isEmpty(), "A newly created ConcurrentList must be empty");
    }

    @Test
    void nonEmptyListShouldReportNotEmpty() {
        list.add(42);
        assertFalse(list.isEmpty(), "List containing an element must report not empty");
    }

    @Test
    void afterClearListShouldBeEmptyAgain() {
        list.add(1);
        list.add(2);
        assertFalse(list.isEmpty(), "Pre‑condition: list is not empty");
        list.clear();
        assertTrue(list.isEmpty(), "List must be empty after clear()");
    }

    @Test
    void wrapperConstructorWithEmptyBackingListReportsEmpty() {
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backingList);
        assertTrue(wrapped.isEmpty(), "Wrapped list with empty backing list must be empty");
    }

    @Test
    void wrapperConstructorWithNonEmptyBackingListReportsNotEmpty() {
        backingList.add(7);
        backingList.add(8);
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backingList);
        assertFalse(wrapped.isEmpty(), "Wrapped list should reflect non‑empty backing list");
    }

    @Test
    void wrapperConstructorReflectsChangesToBackingList() {
        // The wrapper shares the same underlying list instance.
        backingList.add(10);
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backingList);
        assertFalse(wrapped.isEmpty(), "Wrapped list must see element added to backing list");
        // Remove via the wrapper and verify the backing list is also empty.
        wrapped.remove(Integer.valueOf(10));
        assertTrue(wrapped.isEmpty(), "Wrapped list should be empty after removal");
        assertTrue(backingList.isEmpty(), "Backing list must also be empty after removal through wrapper");
    }

    @Test
    void concurrentAddsMakeListNonEmpty() throws InterruptedException, ExecutionException {
        final int threadCount = 20;
        final int addsPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();
        // Submit tasks that add a known range of numbers.
        for (int t = 0; t < threadCount; t++) {
            final int base = t * addsPerThread;
            futures.add(executor.submit(() -> {
                for (int i = 0; i < addsPerThread; i++) {
                    list.add(base + i);
                }
            }));
        }
        // Wait for all tasks to finish.
        for (Future<?> f : futures) {
            // re‑throws any exception from the worker thread
            f.get();
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.MINUTES), "Executor did not terminate in time");
        // At this point the list must not be empty.
        assertFalse(list.isEmpty(), "List should not be empty after concurrent additions");
        assertEquals(threadCount * addsPerThread, list.size(), "List size must match total number of added elements");
    }

    @Test
    void removeByIndexMakesListEmpty() {
        list.add(99);
        assertFalse(list.isEmpty(), "Pre‑condition: list not empty before removal");
        list.remove(0);
        assertTrue(list.isEmpty(), "List should be empty after removing the sole element by index");
    }

    @Test
    void removeByObjectMakesListEmpty() {
        list.add(123);
        assertFalse(list.isEmpty(), "Pre‑condition: list not empty before removal");
        list.remove(Integer.valueOf(123));
        assertTrue(list.isEmpty(), "List should be empty after removing the sole element by object");
    }

    @Test
    void addAllEmptyCollectionLeavesListEmpty() {
        Collection<Integer> empty = Collections.emptyList();
        list.addAll(empty);
        assertTrue(list.isEmpty(), "Adding an empty collection to an empty list must keep it empty");
    }

    @Test
    void addAllNonEmptyCollectionMakesListNonEmpty() {
        Collection<Integer> data = Arrays.asList(1, 2, 3);
        list.addAll(data);
        assertFalse(list.isEmpty(), "Adding a non‑empty collection must make the list non‑empty");
        assertEquals(3, list.size(), "Size must reflect number of added elements");
    }

    @Test
    void clearOnAlreadyEmptyListIsIdempotent() {
        assertTrue(list.isEmpty(), "Pre‑condition: list is empty");
        // first clear – no effect
        list.clear();
        // second clear – should still be fine
        list.clear();
        assertTrue(list.isEmpty(), "List must remain empty after repeated clear() calls");
    }

    @Test
    void wrappedListRuntimeExceptionPropagates() {
        @SuppressWarnings("unchecked")
        List<Integer> mock = mock(List.class);
        when(mock.isEmpty()).thenThrow(new RuntimeException("boom"));
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(mock);
        RuntimeException ex = assertThrows(RuntimeException.class, wrapped::isEmpty, "RuntimeException from underlying list must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void wrappedListIllegalArgumentExceptionPropagates() {
        @SuppressWarnings("unchecked")
        List<Integer> mock = mock(List.class);
        when(mock.isEmpty()).thenThrow(new IllegalArgumentException("bad arg"));
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(mock);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, wrapped::isEmpty, "IllegalArgumentException from underlying list must propagate");
        assertEquals("bad arg", ex.getMessage());
    }

    @Test
    void wrappedListErrorPropagates() {
        @SuppressWarnings("unchecked")
        List<Integer> mock = mock(List.class);
        when(mock.isEmpty()).thenThrow(new AssertionError("assertion"));
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(mock);
        AssertionError err = assertThrows(AssertionError.class, wrapped::isEmpty, "Error from underlying list must propagate unchanged");
        assertEquals("assertion", err.getMessage());
    }

    @Test
    void nullUnderlyingListThrowsNPE() throws Exception {
        // Use reflection to set the private 'list' field to null.
        Field field = ConcurrentList.class.getDeclaredField("list");
        field.setAccessible(true);
        field.set(list, null);
        assertThrows(NullPointerException.class, list::isEmpty, "Calling isEmpty() when the internal list is null must throw NPE");
    }

    @Test
    void concurrentReadersDuringSingleAdd() throws Exception {
        final int readerThreads = 10;
        final int readsPerThread = 1000;
        ExecutorService exec = Executors.newFixedThreadPool(readerThreads + 1);
        List<Future<?>> futures = new ArrayList<>();
        // Reader tasks: repeatedly call isEmpty()
        for (int i = 0; i < readerThreads; i++) {
            futures.add(exec.submit(() -> {
                for (int j = 0; j < readsPerThread; j++) {
                    // We only assert that no exception is thrown; the result may be true or false.
                    list.isEmpty();
                }
            }));
        }
        // Writer task: add a single element after a short pause.
        futures.add(exec.submit(() -> {
            try {
                // give readers a chance to start
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            list.add(777);
        }));
        // Await completion
        for (Future<?> f : futures) {
            // re‑throw any exception from the task
            f.get();
        }
        exec.shutdownNow();
        assertTrue(exec.awaitTermination(1, TimeUnit.MINUTES), "Executor did not terminate in time");
        // After all threads finish the list must contain the element.
        assertFalse(list.isEmpty(), "List must be non‑empty after the writer added an element");
        assertEquals(1, list.size(), "Exactly one element should have been added");
    }
}
