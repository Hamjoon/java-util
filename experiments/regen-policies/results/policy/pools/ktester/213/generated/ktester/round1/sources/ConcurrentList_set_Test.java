package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_set_Test {

    /**
     * List used for the majority of tests (stand‑alone mode).
     */
    private ConcurrentList<Integer> list;

    /**
     * List used for wrapper‑mode tests.
     */
    private List<Integer> backingList;

    /**
     * List wrapped around {@code backingList}.
     */
    private ConcurrentList<Integer> wrappedList;

    /**
     * List used for the majority of tests (stand‑alone mode).
     */
    private ConcurrentList<Integer> intList;

    /**
     * List used for wrapper‑mode tests.
     */
    private List<Integer> backingIntList;

    /**
     * List wrapped around {@code backingIntList}.
     */
    private ConcurrentList<Integer> wrappedIntList;

    /**
     * Generic list for non‑integer tests.
     */
    private ConcurrentList<Object> objList;

    // ---------- Backing list that rejects null elements ----------
    private static class NullRejectingList<E> extends ArrayList<E> {

        @Override
        public E set(int index, E element) {
            if (element == null) {
                throw new NullPointerException("null not allowed");
            }
            return super.set(index, element);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stand‑alone integer list
        intList = new ConcurrentList<>();
        // Wrapper‑mode integer list
        backingIntList = new ArrayList<>();
        wrappedIntList = new ConcurrentList<>(backingIntList);
        // Generic list for Object‑type tests
        objList = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the template tidy.
        list = null;
        backingList = null;
        wrappedList = null;
    }

    // ---------- Basic functionality (String/Object) ----------
    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// Replace element at index 0, old value "originalFirst"
        Arguments.of(0, "first", "originalFirst"), // Replace element at middle index 2 where original is null, expect null return
        Arguments.of(2, 123, null));
    }

    // ---------- Index out of bounds ----------
    private static Stream<Arguments> outOfBoundsProvider() {
        return Stream.of(Arguments.of(-1, "outOfBounds"), // size will be 3
        Arguments.of(3, "outOfBounds"), Arguments.of(5, "outOfBounds"));
    }

    @Test
    void setReplacesElementAndReturnsOldValue() {
        // Arrange
        list.add(10);
        list.add(20);
        list.add(30);
        // Act
        Integer old = list.set(1, 99);
        // Assert
        assertEquals(20, old, "set should return the element previously at the index");
        assertEquals(99, list.get(1), "element at index 1 should be replaced with the new value");
        assertEquals(3, list.size(), "size must remain unchanged after set");
    }

    @Test
    void setNullElementWhenAllowed() {
        // Arrange
        list.add(1);
        list.add(2);
        // Act
        Integer old = list.set(0, null);
        // Assert
        assertEquals(1, old);
        assertNull(list.get(0), "element should be replaced with null");
        assertEquals(2, list.size());
    }

    @Test
    void setWithInvalidIndexThrowsException() {
        list.add(5);
        list.add(6);
        // Negative index
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, 100));
        // Index equal to size (out of bounds for set)
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(2, 100));
    }

    @Test
    void setWorksInWrapperMode() {
        // Arrange – populate the backing list directly
        backingList.add(100);
        backingList.add(200);
        backingList.add(300);
        // Act – use the wrapped ConcurrentList
        Integer old = wrappedList.set(2, 999);
        // Assert
        assertEquals(300, old, "set should return the previous element from the backing list");
        assertEquals(999, backingList.get(2), "backing list must reflect the change");
        assertEquals(999, wrappedList.get(2), "wrapped list must reflect the change");
        assertEquals(3, wrappedList.size(), "size must stay the same");
    }

    @Test
    void concurrentSetOperationsMaintainConsistency() throws InterruptedException, ExecutionException {
        // Populate list with known values
        int size = 100;
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        ExecutorService executor = Executors.newFixedThreadPool(10);
        Callable<Void> task = () -> {
            for (int i = 0; i < size; i++) {
                // Replace each element with its negative value
                list.set(i, -list.get(i));
            }
            return null;
        };
        // Submit several tasks that all perform set operations
        int tasks = 5;
        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < tasks; i++) {
            futures.add(executor.submit(task));
        }
        // Wait for completion and propagate any exception
        for (Future<Void> f : futures) {
            f.get();
        }
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS), "Executor should terminate");
        // Verify that each position now holds the negative of the original value
        for (int i = 0; i < size; i++) {
            assertEquals(-i, list.get(i), "Element at index " + i + " should be the negative of its original value");
        }
    }

    @ParameterizedTest(name = "{index}: set({0}, {1}) returns {2}")
    @MethodSource("basicFunctionalityProvider")
    void setBasicFunctionality(int index, Object newElement, Object expectedOld) {
        // Prepare a list with known contents
        objList.add("originalFirst");
        objList.add("b");
        // index 2 is null
        objList.add(null);
        objList.add("d");
        Object old = objList.set(index, newElement);
        assertEquals(expectedOld, old, "Returned old value should match expectation");
        assertEquals(newElement, objList.get(index), "New element must be stored at the index");
    }

    @ParameterizedTest(name = "set({0}, ...) throws IndexOutOfBoundsException")
    @MethodSource("outOfBoundsProvider")
    void setIndexOutOfBounds(int index, String element) {
        // List of size 3
        intList.add(10);
        intList.add(20);
        intList.add(30);
        assertThrows(IndexOutOfBoundsException.class, () -> intList.set(index, element));
    }

    // ---------- Replace with null element ----------
    @Test
    void setReplaceWithNullElement() {
        intList.add(1);
        intList.add(2);
        intList.add(3);
        Integer old = intList.set(1, null);
        assertEquals(2, old);
        assertNull(intList.get(1));
        assertEquals(3, intList.size());
    }

    // ---------- Replace at last index ----------
    @Test
    void setReplaceAtLastIndex() {
        objList.add("x");
        objList.add("y");
        // index 2 is last
        objList.add("z");
        Object old = objList.set(2, "newZ");
        assertEquals("z", old);
        assertEquals("newZ", objList.get(2));
        assertEquals(3, objList.size());
    }

    // ---------- Single‑element list ----------
    @Test
    void setSingleElementList() {
        objList.add("only");
        Object old = objList.set(0, "changed");
        assertEquals("only", old);
        assertEquals("changed", objList.get(0));
        assertEquals(1, objList.size());
    }

    // ---------- Concurrent modification (two threads) ----------
    @Test
    void setConcurrentTwoThreads() throws InterruptedException {
        // Initialise with a single element
        objList.add("original");
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        List<Object> returned = Collections.synchronizedList(new ArrayList<>());
        Runnable task1 = () -> {
            try {
                startLatch.await();
                returned.add(objList.set(0, "thread1"));
            } catch (Exception e) {
                fail("Thread1 threw: " + e);
            } finally {
                doneLatch.countDown();
            }
        };
        Runnable task2 = () -> {
            try {
                startLatch.await();
                returned.add(objList.set(0, "thread2"));
            } catch (Exception e) {
                fail("Thread2 threw: " + e);
            } finally {
                doneLatch.countDown();
            }
        };
        Thread t1 = new Thread(task1);
        Thread t2 = new Thread(task2);
        t1.start();
        t2.start();
        // Release both threads simultaneously
        startLatch.countDown();
        // Wait for both to finish
        assertTrue(doneLatch.await(2, TimeUnit.SECONDS), "Both threads should finish");
        // Both return values must be either "original" or the other thread's value
        assertTrue(returned.contains("original") || returned.contains("thread1") || returned.contains("thread2"), "Returned values should contain a valid old element");
        // The final element must be one of the two new values
        Object finalValue = objList.get(0);
        assertTrue(finalValue.equals("thread1") || finalValue.equals("thread2"), "Final list element must be either thread1 or thread2");
    }

    // ---------- Unsupported operation (unmodifiable backing list) ----------
    @Test
    void setOnUnmodifiableBackingListThrows() {
        List<Integer> unmod = Collections.unmodifiableList(new ArrayList<>(List.of(1, 2, 3)));
        ConcurrentList<Integer> cl = new ConcurrentList<>(unmod);
        assertThrows(UnsupportedOperationException.class, () -> cl.set(1, 99));
    }

    // ---------- ClassCastException when element type mismatches ----------
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void setWithWrongGenericTypeThrowsClassCastException() {
        // Raw list that actually stores Integers
        List raw = new ArrayList<>();
        raw.add(10);
        raw.add(20);
        // Wrap it as a ConcurrentList<Integer>
        ConcurrentList<Integer> cl = new ConcurrentList<>(raw);
        // Force a String into the list via the generic method – runtime cast should fail
        assertThrows(ClassCastException.class, () -> cl.set(0, (Integer) (Object) "stringValue"));
    }

    // ---------- Null underlying list ----------
    @Test
    void setWithNullBackingListThrowsNullPointerException() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(null);
        assertThrows(NullPointerException.class, () -> cl.set(0, 1));
    }

    @Test
    void setNullElementRejectedByBackingList() {
        List<String> rejecting = new NullRejectingList<>();
        rejecting.add("a");
        rejecting.add("b");
        ConcurrentList<String> cl = new ConcurrentList<>(rejecting);
        assertThrows(NullPointerException.class, () -> cl.set(0, null));
    }
}
