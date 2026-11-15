package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_size_Test {

    /**
     * The list under test – created fresh for each test method.
     */
    private ConcurrentList<Integer> list;

    /**
     * A backing list used for the “wrapper mode” tests.
     */
    private List<Integer> backingList;

    /**
     * Helper class to transport the list instance created inside the lambda
     * to the test method. This avoids having to use a mutable field inside the
     * stream provider, which JUnit does not allow.
     */
    static class TestHelper {

        private static ConcurrentList<Integer> list;

        static void setList(ConcurrentList<Integer> l) {
            list = l;
        }

        static ConcurrentList<Integer> getList() {
            return list;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default (stand‑alone) mode – a brand‑new thread‑safe list.
        list = new ConcurrentList<>();
        // Backing list for wrapper‑mode tests – created lazily in the test that needs it.
        backingList = null;
        // If a test has stored a list in TestHelper, use it as the test fixture.
        ConcurrentList<Integer> helper = TestHelper.getList();
        if (helper != null) {
            this.list = helper;
            // reset for next test
            TestHelper.setList(null);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references to help the GC; not strictly required but keeps the pattern clean.
        list = null;
        backingList = null;
    }

    private static Stream<Arguments> sequentialModificationProvider() {
        return Stream.of(Arguments.of("Add two elements, size should be 2", (Runnable) () -> {
            ConcurrentList<Integer> l = new ConcurrentList<>();
            l.add(1);
            l.add(2);
            // expose to test method via static field (or recreate inside test)
            TestHelper.setList(l);
        }, 2), Arguments.of("Add three elements, remove one, size should be 2", (Runnable) () -> {
            ConcurrentList<Integer> l = new ConcurrentList<>();
            l.add(1);
            l.add(2);
            l.add(3);
            l.remove(Integer.valueOf(2));
            TestHelper.setList(l);
        }, 2), Arguments.of("Add elements then clear, size should be 0", (Runnable) () -> {
            ConcurrentList<Integer> l = new ConcurrentList<>();
            l.add(10);
            l.add(20);
            l.clear();
            TestHelper.setList(l);
        }, 0));
    }

    /* -------------------------------------------------------------
     *  Basic size behaviour – empty list
     * ------------------------------------------------------------- */
    @Test
    void sizeShouldBeZeroForNewList() {
        // given
        // (list already created in @BeforeEach)
        // when
        int size = list.size();
        // then
        assertEquals(0, size, "A newly created ConcurrentList must report size 0");
    }

    /* -------------------------------------------------------------
     *  Size after a sequence of adds
     * ------------------------------------------------------------- */
    @Test
    void sizeReflectsNumberOfAddedElements() {
        // given
        assertTrue(list.isEmpty(), "Pre‑condition: list starts empty");
        // when
        list.add(10);
        list.add(20);
        list.add(30);
        // then
        assertEquals(3, list.size(), "Size must equal the number of elements added");
    }

    /* -------------------------------------------------------------
     *  Wrapper mode – size should mirror the supplied backing list
     * ------------------------------------------------------------- */
    @Test
    void sizeWorksWhenWrappingAnExistingList() {
        // given
        backingList = new ArrayList<>(List.of(1, 2, 3, 4));
        list = new ConcurrentList<>(backingList);
        // when
        int initialSize = list.size();
        // then
        assertEquals(4, initialSize, "Size must match the initial backing list size");
        // when – modify through the wrapper
        list.add(5);
        list.remove(Integer.valueOf(2));
        // then – size reflects the modifications
        assertEquals(4, list.size(), "Size must reflect additions and removals performed via the wrapper");
    }

    /* -------------------------------------------------------------
     *  Concurrency test – many parallel adds followed by size check
     * ------------------------------------------------------------- */
    @Test
    void sizeIsCorrectAfterConcurrentAdds() throws InterruptedException, ExecutionException {
        // given
        final int taskCount = 1_000;
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        // when – submit a batch of add‑tasks
        List<Callable<Void>> tasks = new ArrayList<>(taskCount);
        for (int i = 0; i < taskCount; i++) {
            final int value = i;
            tasks.add(() -> {
                list.add(value);
                return null;
            });
        }
        List<Future<Void>> futures = executor.invokeAll(tasks);
        // ensure all tasks completed without exception
        for (Future<Void> f : futures) {
            // will re‑throw any exception that occurred in a task
            f.get();
        }
        // shutdown the executor cleanly
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, TimeUnit.MINUTES), "Executor did not terminate in the allotted time");
        // then – size must equal the number of successful adds
        assertEquals(taskCount, list.size(), "After all concurrent adds, size should equal the number of added elements");
    }

    /* -------------------------------------------------------------
     *  Wrapper mode – size on a pre‑populated backing list (3 elements)
     * ------------------------------------------------------------- */
    @Test
    void sizeReflectsInitialBackingListSize() {
        // given
        backingList = new ArrayList<>(List.of(7, 8, 9));
        list = new ConcurrentList<>(backingList);
        // when
        int size = list.size();
        // then
        assertEquals(3, size, "Size must reflect the three elements present in the backing list");
    }

    /* -------------------------------------------------------------
     *  Sequential modifications – parameterised scenarios
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("sequentialModificationProvider")
    void sizeAfterSequentialModifications(String description, Runnable setup, int expectedSize) {
        // given
        setup.run();
        // when
        int actual = list.size();
        // then
        assertEquals(expectedSize, actual, description);
    }

    /* -------------------------------------------------------------
     *  NullPointerException when internal list is null (reflection)
     * ------------------------------------------------------------- */
    @Test
    void sizeThrowsNPEWhenInternalListIsNull() throws Exception {
        // given – replace the private field 'list' with null via reflection
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(list, null);
        // when / then
        assertThrows(NullPointerException.class, list::size, "Calling size() when the internal list reference is null must throw NPE");
    }

    /* -------------------------------------------------------------
     *  Propagation of RuntimeException from underlying list
     * ------------------------------------------------------------- */
    @Test
    void sizePropagatesIllegalStateExceptionFromUnderlyingList() throws Exception {
        // given – a mock list whose size() always throws IllegalStateException
        @SuppressWarnings("unchecked")
        List<Integer> mockList = mock(List.class);
        when(mockList.size()).thenThrow(new IllegalStateException("boom"));
        // inject the mock via reflection
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(list, mockList);
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, list::size);
        assertEquals("boom", ex.getMessage(), "size() must propagate the IllegalStateException thrown by the underlying list");
    }

    /* -------------------------------------------------------------
     *  Propagation of ConcurrentModificationException from underlying list
     * ------------------------------------------------------------- */
    @Test
    void sizePropagatesConcurrentModificationExceptionFromUnderlyingList() throws Exception {
        // given – a mock list whose size() throws CME
        @SuppressWarnings("unchecked")
        List<Integer> mockList = mock(List.class);
        when(mockList.size()).thenThrow(new ConcurrentModificationException("concurrent change"));
        // inject the mock via reflection
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(list, mockList);
        // when / then
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, list::size);
        assertEquals("concurrent change", ex.getMessage(), "size() must propagate the ConcurrentModificationException thrown by the underlying list");
    }

    /* -------------------------------------------------------------
     *  Propagation of exception thrown by overridden readOperation
     * ------------------------------------------------------------- */
    @Test
    void sizePropagatesExceptionFromReadOperationOverride() {
        // given – subclass that overrides readOperation to throw IllegalStateException
        ConcurrentList<Integer> faultyList = new ConcurrentList<Integer>() {

            @Override
            protected <T> T readOperation(java.util.function.Supplier<T> operation) {
                throw new IllegalStateException("readOperation failure");
            }
        };
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, faultyList::size);
        assertEquals("readOperation failure", ex.getMessage(), "size() must surface the exception thrown by the overridden readOperation");
    }
}
