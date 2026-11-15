package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static com.cedarsoftware.util.DeepEquals.deepEquals;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_add_2_Test {

    /**
     * The list under test – created fresh for each test method.
     */
    private List<Integer> list;

    class CustomObj {

        final int value;

        CustomObj(int v) {
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CustomObj && ((CustomObj) o).value == value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "CustomObj(" + value + ")";
        }
    }

    class CustomObj {

        final int value;

        CustomObj(int v) {
            this.value = v;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CustomObj && ((CustomObj) o).value == value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "CustomObj(" + value + ")";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor which creates an internal ArrayList.
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector and make the test intent explicit.
        list = null;
    }

    /**
     * Parameter source for valid index/element combinations.
     */
    private static Stream<Arguments> validAddArguments() {
        return Stream.of(// Add to empty list at index 0
        Arguments.of(0, "first", Arrays.asList("first")), // Append at current size (size = 1 after previous add)
        Arguments.of(1, "last", Arrays.asList("first", "last")), // Insert null at index 0 (list currently ["first","last"])
        Arguments.of(0, null, Arrays.asList(null, "first", "last")));
    }

    /**
     * Parameter source for illegal index values that must throw.
     */
    private static Stream<Arguments> illegalIndexArguments() {
        return Stream.of(// Negative index
        Arguments.of(-1, "neg"), // Index greater than size (size = 2)
        Arguments.of(3, "outOfRange"));
    }

    @Test
    void testAddAtStartMiddleAndEnd() {
        // start with a known list state
        list.addAll(Arrays.asList(1, 3, 4));
        // add at the beginning (index 0)
        list.add(0, 0);
        assertTrue(deepEquals(Arrays.asList(0, 1, 3, 4), list), "Adding at index 0 should prepend the element");
        // add in the middle (index 2)
        list.add(2, 2);
        assertTrue(deepEquals(Arrays.asList(0, 1, 2, 3, 4), list), "Adding at index 2 should insert the element between 1 and 3");
        // add at the end (index equal to current size)
        list.add(5, 5);
        assertTrue(deepEquals(Arrays.asList(0, 1, 2, 3, 4, 5), list), "Adding at index size should append the element");
    }

    @Test
    void testAddNullElementIsAllowed() {
        list.addAll(Arrays.asList(1, 2, 3));
        // Inserting a null value should be permitted (ArrayList permits nulls)
        list.add(1, null);
        assertTrue(deepEquals(Arrays.asList(1, null, 2, 3), list), "Null element should be inserted at the specified index");
    }

    @Test
    void testAddWithNegativeIndexThrows() {
        list.addAll(Arrays.asList(10, 20));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, 5), "Negative index should cause IndexOutOfBoundsException");
    }

    @Test
    void testAddWithIndexGreaterThanSizeThrows() {
        list.addAll(Arrays.asList(10, 20));
        assertThrows(IndexOutOfBoundsException.class, // current size is 2
        () -> list.add(3, 30), "Index greater than size should cause IndexOutOfBoundsException");
    }

    @Test
    void testAddOnEmptyListAtValidIndexZero() {
        // Adding at index 0 on an empty list is valid
        list.add(0, 42);
        assertTrue(deepEquals(Arrays.asList(42), list), "Adding at index 0 on an empty list should succeed");
    }

    @Test
    void testConcurrentAddsDoNotCorruptOrderWhenSynchronized() throws InterruptedException {
        // This test mirrors the existing concurrency test but focuses on the
        // add(int, E) overload.
        final int threadCount = 20;
        final int addsPerThread = 50;
        List<Integer> concurrentList = new ConcurrentList<>();
        var executor = java.util.concurrent.Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < addsPerThread; i++) {
                    // Insert at the end each time; index = size() is safe because
                    // size() is evaluated inside the synchronized writeOperation.
                    int index = concurrentList.size();
                    concurrentList.add(index, threadId * addsPerThread + i);
                }
            });
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES), "All add tasks should finish within the timeout");
        int expectedSize = threadCount * addsPerThread;
        assertEquals(expectedSize, concurrentList.size(), "List size should reflect all concurrent insertions");
        // Verify that every expected element is present.
        for (int i = 0; i < expectedSize; i++) {
            assertTrue(concurrentList.contains(i), "List should contain element " + i);
        }
    }

    @ParameterizedTest(name = "add({0}, {1}) results in {2}")
    @MethodSource("validAddArguments")
    void testAddValidIndicesAndElements(int index, String element, List<String> expected) {
        // Use a fresh list of Strings for each invocation
        List<String> stringList = new ConcurrentList<>();
        // Populate with the elements that precede the current test case when needed
        // (first case starts empty, second case needs one element, third case needs two)
        if (!expected.isEmpty()) {
            // Build the list step‑by‑step to match the expected final state
            // The test harness will call add() exactly once per case,
            // so we need to simulate the prior state manually.
            // For simplicity, we reconstruct the expected list by removing the last element.
            List<String> prior = new ArrayList<>(expected);
            prior.remove(prior.size() - 1);
            stringList.addAll(prior);
        }
        // Perform the operation under test
        stringList.add(index, element);
        // Verify final state matches expectation
        assertTrue(deepEquals(expected, stringList), () -> "After add(" + index + ", " + element + ") expected " + expected);
    }

    @ParameterizedTest(name = "add({0}, {1}) throws IndexOutOfBoundsException")
    @MethodSource("illegalIndexArguments")
    void testAddIllegalIndices(int index, String element) {
        // Prepare a list with two elements so that size == 2
        list.addAll(Arrays.asList(10, 20));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(index, element), "add with index " + index + " should throw IndexOutOfBoundsException");
    }

    @Test
    void testInsertionOrderAndShifting() {
        List<String> lst = new ConcurrentList<>();
        lst.addAll(Arrays.asList("a", "b"));
        // Insert in the middle
        lst.add(1, "middle");
        assertTrue(deepEquals(Arrays.asList("a", "middle", "b"), lst), "Element should be inserted at index 1 and shift subsequent elements");
        // Insert at head
        lst.add(0, "head");
        assertTrue(deepEquals(Arrays.asList("head", "a", "middle", "b"), lst), "Head insertion should shift all existing elements right");
        // Append at tail
        lst.add(lst.size(), "tail");
        assertTrue(deepEquals(Arrays.asList("head", "a", "middle", "b", "tail"), lst), "Appending at size should add element at the end");
    }

    @Test
    void testDuplicateAndEqualityHandling() {
        List<String> lst = new ConcurrentList<>();
        lst.addAll(Arrays.asList("dup", "x", "y"));
        // Insert duplicate value at index 2
        lst.add(2, "dup");
        assertTrue(deepEquals(Arrays.asList("dup", "x", "dup", "y"), lst), "Duplicate values should be allowed and inserted at the specified index");
        // Use a custom object with proper equals but distinct identity
        class CustomObj {

            final int value;

            CustomObj(int v) {
                this.value = v;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof CustomObj && ((CustomObj) o).value == value;
            }

            @Override
            public int hashCode() {
                return Objects.hash(value);
            }

            @Override
            public String toString() {
                return "CustomObj(" + value + ")";
            }
        }
        CustomObj objA = new CustomObj(5);
        CustomObj objB = new CustomObj(7);
        List<CustomObj> objList = new ConcurrentList<>();
        objList.addAll(Arrays.asList(objA, objB));
        // Insert another instance with the same internal state as objA
        objList.add(1, new CustomObj(5));
        assertEquals(3, objList.size(), "Size should increase after inserting another CustomObj");
        assertEquals(objA, objList.get(0));
        assertEquals(new CustomObj(5), objList.get(1));
        assertEquals(objB, objList.get(2));
    }

    @Test
    void testIndexNearIntegerMaxValueThrows() {
        // The list is tiny; attempting to add at a gigantic index must fail.
        List<Integer> tiny = new ConcurrentList<>();
        tiny.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        int hugeIndex = Integer.MAX_VALUE - 1;
        assertThrows(IndexOutOfBoundsException.class, () -> tiny.add(hugeIndex, 99), "Adding at an index far beyond the current size should throw IndexOutOfBoundsException");
    }

    @Test
    void testAddAtValidUpperBoundForModestList() {
        List<Integer> ten = new ConcurrentList<>();
        for (int i = 0; i < 10; i++) {
            ten.add(i);
        }
        // index == size()
        ten.add(10, 99);
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 10; i++) expected.add(i);
        expected.add(99);
        assertTrue(deepEquals(expected, ten), "Appending at index equal to current size should succeed");
    }

    @Test
    void testTwoThreadConcurrentAddsAtZero() throws InterruptedException, ExecutionException {
        // Start with an empty ConcurrentList
        List<String> shared = new ConcurrentList<>();
        Callable<Void> task1 = () -> {
            shared.add(0, "t1");
            return null;
        };
        Callable<Void> task2 = () -> {
            shared.add(0, "t2");
            return null;
        };
        ExecutorService exec = Executors.newFixedThreadPool(2);
        Future<Void> f1 = exec.submit(task1);
        Future<Void> f2 = exec.submit(task2);
        // wait for completion
        f1.get();
        f2.get();
        exec.shutdownNow();
        assertEquals(2, shared.size(), "After two concurrent adds at index 0, size should be 2");
        // Both elements must be present, order is nondeterministic due to race on index 0
        assertTrue(shared.contains("t1") && shared.contains("t2"), "List should contain both inserted elements");
    }

    @Test
    void testAddWithUnmodifiableBackingListThrows() {
        // Create an unmodifiable list and wrap it with ConcurrentList
        List<String> backing = Collections.unmodifiableList(new ArrayList<>());
        List<String> wrapper = new ConcurrentList<>(backing);
        // Any mutating operation should propagate the UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> wrapper.add(0, "any"), "Adding to a ConcurrentList that wraps an unmodifiable list must throw UnsupportedOperationException");
        // Also test append (index == size())
        assertThrows(UnsupportedOperationException.class, () -> wrapper.add(0, "append"), "Appending to an unmodifiable backing list must also throw UnsupportedOperationException");
    }
}
