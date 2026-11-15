package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_iterator_Test {

    private List<Integer> concurrentList;

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor so we have a fresh, empty list for each test
        concurrentList = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required
        concurrentList = null;
    }

    private static Stream<Arguments> basicIteratorProvider() {
        return Stream.of(Arguments.of("Empty list – iterator has no elements", Collections.emptyList(), Collections.emptyList()), Arguments.of("Single element list – iterator yields that element", Collections.singletonList(42), Collections.singletonList(42)), Arguments.of("Multiple elements – iterator yields them in order", Arrays.asList(1, 2, 3, 4), Arrays.asList(1, 2, 3, 4)));
    }

    /* --------------------------------------------------------------
     *  Helper utilities
     * -------------------------------------------------------------- */
    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static <T> List<T> collectAll(Iterator<T> it) {
        List<T> result = new ArrayList<>();
        while (it.hasNext()) {
            result.add(it.next());
        }
        return result;
    }

    @Test
    void iteratorReturnsSnapshotOfCurrentElements() {
        // given
        concurrentList.add(1);
        concurrentList.add(2);
        concurrentList.add(3);
        // when
        Iterator<Integer> iterator = concurrentList.iterator();
        // then – iterator should contain the three elements in order
        assertTrue(iterator.hasNext());
        assertEquals(1, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(2, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(3, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void iteratorIsIndependentOfSubsequentModifications() {
        // given
        concurrentList.add(10);
        concurrentList.add(20);
        // snapshot
        Iterator<Integer> iterator = concurrentList.iterator();
        // when – modify underlying list
        concurrentList.add(30);
        concurrentList.remove(Integer.valueOf(10));
        // then – iterator still reflects the original snapshot (10, 20)
        assertTrue(iterator.hasNext());
        assertEquals(10, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(20, iterator.next());
        assertFalse(iterator.hasNext());
        // also verify underlying list reflects the modifications
        assertEquals(Arrays.asList(20, 30), concurrentList);
    }

    @Test
    void iteratorOnEmptyListHasNoElements() {
        // given an empty ConcurrentList (setupBeforeEach already provides this)
        // when
        Iterator<Integer> iterator = concurrentList.iterator();
        // then
        assertFalse(iterator.hasNext());
    }

    @Test
    void iteratorDoesNotSupportRemoveOperation() {
        // given
        concurrentList.add(5);
        Iterator<Integer> iterator = concurrentList.iterator();
        // when / then – remove() should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @Test
    void iteratorWorksCorrectlyUnderConcurrentAdds() throws InterruptedException {
        // given
        int threads = 5;
        int perThread = 200;
        List<Thread> workers = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            final int base = t * perThread;
            Thread worker = new Thread(() -> {
                for (int i = 0; i < perThread; i++) {
                    concurrentList.add(base + i);
                }
            });
            workers.add(worker);
            worker.start();
        }
        // wait for all threads to finish
        for (Thread w : workers) {
            w.join();
        }
        // when
        Iterator<Integer> iterator = concurrentList.iterator();
        // then – iterator should contain exactly threads * perThread elements,
        // though order is not guaranteed because of concurrency; we just verify count.
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        assertEquals(threads * perThread, count, "Iterator should reflect all elements added concurrently");
    }

    /* --------------------------------------------------------------
     *  Basic iterator functionality – empty, single, multiple elements
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicIteratorProvider")
    void iteratorBasicFunctionality(String description, List<Integer> initialData, List<Integer> expected) {
        // given – populate the list
        initialData.forEach(concurrentList::add);
        // when
        Iterator<Integer> it = concurrentList.iterator();
        // then – collect iterator output and compare with expected snapshot
        List<Integer> actual = new ArrayList<>();
        while (it.hasNext()) {
            actual.add(it.next());
        }
        assertEquals(expected, actual, description);
    }

    @Test
    void iteratorDoesNotReflectNewElementsAddedAfterCreation() {
        // given
        concurrentList.add(1);
        // snapshot contains only 1
        Iterator<Integer> iterator = concurrentList.iterator();
        // when
        concurrentList.add(2);
        // then – iterator should not see the newly added element
        assertTrue(iterator.hasNext());
        assertEquals(1, iterator.next());
        assertFalse(iterator.hasNext());
    }

    @Test
    void iteratorDoesNotReflectRemovalsAfterCreation() {
        // given
        concurrentList.addAll(Arrays.asList(5, 6, 7));
        // snapshot (5,6,7)
        Iterator<Integer> iterator = concurrentList.iterator();
        // when
        concurrentList.remove(Integer.valueOf(6));
        // then – iterator still yields the original three elements
        assertEquals(Arrays.asList(5, 6, 7), collectAll(iterator));
    }

    /* --------------------------------------------------------------
     *  Null handling
     * -------------------------------------------------------------- */
    @Test
    void iteratorHandlesNullElements() {
        // given a list containing nulls
        ConcurrentList<String> stringList = new ConcurrentList<>();
        stringList.add(null);
        stringList.add("A");
        stringList.add(null);
        // when
        Iterator<String> it = stringList.iterator();
        // then – iterator returns null entries in the correct positions
        assertTrue(it.hasNext());
        assertNull(it.next());
        assertTrue(it.hasNext());
        assertEquals("A", it.next());
        assertTrue(it.hasNext());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    /* --------------------------------------------------------------
     *  Iterator remove() contract
     * -------------------------------------------------------------- */
    @Test
    void iteratorRemoveThrowsIllegalStateIfCalledBeforeNext() {
        // given
        concurrentList.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = concurrentList.iterator();
        // when / then – IllegalStateException because next() has not been called yet
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void iteratorRemoveRemovesFromSnapshotOnly() {
        // given
        concurrentList.addAll(Arrays.asList(100, 200, 300));
        Iterator<Integer> it = concurrentList.iterator();
        // when – advance to first element and remove it via iterator
        assertEquals(100, it.next());
        // should remove from snapshot, not from underlying list
        it.remove();
        // then – iterator continues without the removed element
        List<Integer> remaining = collectAll(it);
        assertEquals(Arrays.asList(200, 300), remaining);
        // underlying list must stay unchanged
        assertEquals(Arrays.asList(100, 200, 300), concurrentList);
    }

    /* --------------------------------------------------------------
     *  Exhaustion behaviour
     * -------------------------------------------------------------- */
    @Test
    void iteratorThrowsNoSuchElementExceptionWhenExhausted() {
        // given
        concurrentList.addAll(Arrays.asList(9, 8));
        Iterator<Integer> it = concurrentList.iterator();
        // exhaust the iterator
        assertEquals(9, it.next());
        assertEquals(8, it.next());
        assertFalse(it.hasNext());
        // then – calling next() again must throw NoSuchElementException
        assertThrows(NoSuchElementException.class, it::next);
    }

    /* --------------------------------------------------------------
     *  Exception handling – internal list set to null
     * -------------------------------------------------------------- */
    @Test
    void iteratorThrowsNullPointerExceptionWhenBackingListIsNull() throws Exception {
        // given a normal list, then force its private field 'list' to null via reflection
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.add(1);
        setPrivateField(list, "list", null);
        // when / then – invoking iterator() should throw NPE because of `new ArrayList<>(list)`
        assertThrows(NullPointerException.class, list::iterator);
    }

    /* --------------------------------------------------------------
     *  Exception handling – readOperation propagates RuntimeException
     * -------------------------------------------------------------- */
    @Test
    void iteratorPropagatesRuntimeExceptionFromReadOperation() {
        // The readOperation method is private, but we can simulate a failure by
        // providing a supplier that throws. The iterator() method delegates to
        // readOperation(() -> new ArrayList<>(list).iterator()).
        // We'll create a subclass that overrides iterator() to call the
        // original implementation with a supplier that throws.
        ConcurrentList<Integer> faulty = new ConcurrentList<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                // Simulate failure inside the supplier
                return readOperation(() -> {
                    throw new RuntimeException("simulated failure");
                });
            }

            // expose the private readOperation via a protected bridge for the test
            @SuppressWarnings("unchecked")
            protected <T> T readOperation(java.util.function.Supplier<T> operation) {
                try {
                    // Use reflection to invoke the original private method
                    java.lang.reflect.Method m = ConcurrentList.class.getDeclaredMethod("readOperation", java.util.function.Supplier.class);
                    m.setAccessible(true);
                    return (T) m.invoke(this, operation);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        // when / then – iterator() should propagate the RuntimeException
        RuntimeException ex = assertThrows(RuntimeException.class, faulty::iterator);
        assertTrue(ex.getMessage().contains("simulated failure"));
    }
}
