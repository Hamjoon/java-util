package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_toString_Test {

    @Mock
    private List<String> // used in the delegation test
    mockedList;

    /**
     * Simple POJO used for custom‑object tests.
     */
    static class Person {

        private final String name;

        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + '}';
        }
    }

    /**
     * List implementation that throws a RuntimeException from toString().
     */
    static class FaultyList<E> extends ArrayList<E> {

        @Override
        public String toString() {
            throw new RuntimeException("Faulty toString");
        }
    }

    /**
     * Custom list that decorates the default toString().
     */
    static class FancyList<E> extends ArrayList<E> {

        @Override
        public String toString() {
            return "<fancy>" + super.toString() + "</fancy>";
        }
    }

    /**
     * List whose toString() deliberately returns null.
     */
    static class NullStringList<E> extends ArrayList<E> {

        @Override
        public String toString() {
            return null;
        }
    }

    /**
     * List that throws an Error from toString().
     */
    static class ErrorList<E> extends ArrayList<E> {

        @Override
        public String toString() {
            throw new Error("Fatal error in toString");
        }
    }

    /**
     * List that throws IllegalStateException from toString().
     */
    static class IllegalStateList<E> extends ArrayList<E> {

        @Override
        public String toString() {
            throw new IllegalStateException("illegal state");
        }
    }

    // for future extensibility.
    static Stream<Arguments> emptyListProvider() {
        return Stream.of(Arguments.of(new ArrayList<>()), Arguments.of(new LinkedList<>()));
    }

    @Test
    void toStringOnEmptyListReturnsEmptyBrackets() {
        List<Integer> list = new ConcurrentList<>();
        assertTrue(list.isEmpty(), "List should start empty");
        assertEquals("[]", list.toString(), "toString() of an empty list must be []");
    }

    @Test
    void toStringReflectsCurrentContents() {
        List<String> list = new ConcurrentList<>();
        list.add("alpha");
        list.add("beta");
        list.add("gamma");
        assertEquals("[alpha, beta, gamma]", list.toString(), "toString() should reflect the elements in insertion order");
    }

    @Test
    void toStringDelegatesToWrappedList() {
        // Arrange – the mock will return a specific string when its toString() is invoked
        when(mockedList.toString()).thenReturn("[mocked list]");
        // Act – wrap the mock with ConcurrentList and call toString()
        ConcurrentList<String> concurrentList = new ConcurrentList<>(mockedList);
        String result = concurrentList.toString();
        // Assert – the result must be exactly what the wrapped list returned
        assertEquals("[mocked list]", result, "ConcurrentList.toString() should return the wrapped list's toString() result");
        // Verify that the wrapped list's toString() was called exactly once
        verify(mockedList, times(1)).toString();
    }

    @Test
    void constructorRejectsNullBackingList() {
        assertThrows(IllegalArgumentException.class, () -> new ConcurrentList<>(null), "Constructor should throw IllegalArgumentException when a null list is supplied");
    }

    @Test
    void toStringSingleElementList() {
        List<String> list = new ConcurrentList<>();
        list.add("alpha");
        assertEquals("[alpha]", list.toString(), "Single element list should be represented as [alpha]");
    }

    @Test
    void toStringMultipleElementsList() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3, 4));
        assertEquals("[1, 2, 3, 4]", list.toString(), "Multiple elements list should match the underlying list's toString()");
    }

    @Test
    void toStringListContainingNullElements() {
        List<String> list = new ConcurrentList<>();
        list.add(null);
        list.add("test");
        list.add(null);
        assertEquals("[null, test, null]", list.toString(), "Null entries must be rendered exactly as the underlying list does");
    }

    @Test
    void toStringListWithCustomObjects() {
        List<Person> list = new ConcurrentList<>();
        list.add(new Person("John", 30));
        list.add(new Person("Jane", 25));
        assertEquals("[Person{name='John', age=30}, Person{name='Jane', age=25}]", list.toString(), "Custom objects' toString() should be propagated unchanged");
    }

    @Test
    void toStringConcurrentModificationDuringToString() throws Exception {
        // Prepare list with initial elements
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("a", "b", "c"));
        // Writer thread that adds "d" after a short pause
        Thread writer = new Thread(() -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
            list.add("d");
        });
        writer.start();
        // Main thread calls toString while writer may be running
        String result = list.toString();
        // ensure writer finishes before assertions
        writer.join();
        // The result must be either the snapshot before or after the addition,
        // but must never be corrupted or cause CME.
        assertTrue("[a, b, c]".equals(result) || "[a, b, c, d]".equals(result), "toString() must return a consistent snapshot without throwing");
    }

    @Test
    void toStringExceptionPropagationFromUnderlyingList() {
        List<Integer> faulty = new FaultyList<>();
        faulty.addAll(Arrays.asList(1, 2, 3));
        ConcurrentList<Integer> cl = new ConcurrentList<>(faulty);
        RuntimeException ex = assertThrows(RuntimeException.class, cl::toString, "RuntimeException from underlying list must propagate");
        assertEquals("Faulty toString", ex.getMessage());
    }

    @Test
    void toStringAfterMutations() {
        List<Integer> list = new ConcurrentList<>();
        list.addAll(Arrays.asList(1, 2, 3));
        // Perform mutations
        // [1,10,2,3]
        list.add(1, 10);
        // [1,10,20,3]
        list.set(2, 20);
        // [10,20,3]
        list.remove(0);
        assertEquals("[10, 20, 3]", list.toString(), "toString() must reflect the final state after mutations");
    }

    @Test
    void toStringAfterClear() {
        List<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("x", "y"));
        list.clear();
        assertEquals("[]", list.toString(), "After clear() the list should be empty");
    }

    @Test
    void toStringDifferentUnderlyingListImplementation() {
        List<String> linked = new LinkedList<>(Arrays.asList("L1", "L2"));
        ConcurrentList<String> cl = new ConcurrentList<>(linked);
        assertEquals("[L1, L2]", cl.toString(), "ConcurrentList should forward the LinkedList's toString()");
    }

    @Test
    void toStringConcurrentReaders() throws Exception {
        List<String> list = new ConcurrentList<>(new ArrayList<>(Arrays.asList("r1", "r2")));
        int threadCount = 5;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(exec.submit(() -> {
                // signal this thread is ready
                ready.countDown();
                // wait for the go‑signal
                start.await();
                // invoke toString concurrently
                return list.toString();
            }));
        }
        // Wait until all threads are ready, then release them simultaneously
        ready.await();
        start.countDown();
        for (Future<String> f : futures) {
            assertEquals("[r1, r2]", f.get(), "All concurrent readers must obtain the same snapshot");
        }
        exec.shutdownNow();
    }

    @Test
    void toStringLargeList() {
        List<Integer> list = new ConcurrentList<>();
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        StringBuilder expected = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            expected.append(i);
            if (i < 99)
                expected.append(", ");
        }
        expected.append("]");
        assertEquals(expected.toString(), list.toString(), "Large list representation must be correct");
    }

    @Test
    void toStringCustomUnderlyingListWithOverriddenToString() {
        List<String> fancy = new FancyList<>(Arrays.asList("a", "b"));
        ConcurrentList<String> cl = new ConcurrentList<>(fancy);
        assertEquals("<fancy>[a, b]</fancy>", cl.toString(), "Custom toString implementation must be forwarded unchanged");
    }

    @Test
    void toStringListContainingNestedCollections() {
        List<Object> list = new ConcurrentList<>();
        list.add(Arrays.asList(1, 2));
        list.add(Collections.singletonList(3));
        assertEquals("[[1, 2], [3]]", list.toString(), "Nested collections must be rendered using their own toString()");
    }

    @Test
    void toStringUnderlyingListToStringReturnsNull() {
        List<String> nullStr = new NullStringList<>();
        ConcurrentList<String> cl = new ConcurrentList<>(nullStr);
        assertNull(cl.toString(), "If the wrapped list returns null, ConcurrentList.toString() must return null");
    }

    @Test
    void toStringErrorPropagationFromUnderlyingList() {
        List<Integer> errList = new ErrorList<>();
        errList.addAll(Arrays.asList(1, 2, 3));
        ConcurrentList<Integer> cl = new ConcurrentList<>(errList);
        Error err = assertThrows(Error.class, cl::toString, "Error thrown by underlying list must propagate");
        assertEquals("Fatal error in toString", err.getMessage());
    }

    @Test
    void toStringIllegalStateExceptionPropagation() {
        List<String> illegal = new IllegalStateList<>();
        illegal.add("x");
        ConcurrentList<String> cl = new ConcurrentList<>(illegal);
        IllegalStateException ise = assertThrows(IllegalStateException.class, cl::toString, "IllegalStateException must propagate unchanged");
        assertEquals("illegal state", ise.getMessage());
    }
}
