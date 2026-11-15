package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_indexOf_Test {

    private List<String> list;

    private static class DummyKey {

        private final int value;

        DummyKey(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof DummyKey))
                return false;
            return value == ((DummyKey) o).value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor (stand‑alone mode) for a fresh list before each test
        list = new ConcurrentList<>();
        list.add("alpha");
        list.add("beta");
        list.add("gamma");
        // include a null element to verify null handling
        list.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required but keeps the pattern consistent
        list = null;
    }

    // Group: Basic functionality of indexOf(Object)
    private static Stream<Arguments> basicFunctionalityProvider() {
        List<String> basic = new ConcurrentList<>();
        // index 0
        basic.add("apple");
        basic.add("orange");
        // index 2
        basic.add("banana");
        // duplicate at index 3
        basic.add("banana");
        return Stream.of(Arguments.of(basic, "apple", 0), Arguments.of(basic, "banana", 2), Arguments.of(basic, "pear", -1));
    }

    // Group: Empty list behavior
    private static Stream<Arguments> emptyListProvider() {
        List<String> empty = new ConcurrentList<>();
        return Stream.of(Arguments.of(empty, "anyValue", -1));
    }

    // Group: Null handling (three scenarios)
    private static Stream<Arguments> nullHandlingProvider() {
        List<String> singleNull = new ConcurrentList<>();
        // index 0
        singleNull.add(null);
        List<String> mixed = new ConcurrentList<>();
        mixed.add("a");
        mixed.add("b");
        // index 2
        mixed.add(null);
        mixed.add("c");
        List<String> noNull = new ConcurrentList<>();
        noNull.add("a");
        noNull.add("b");
        noNull.add("c");
        return Stream.of(Arguments.of(singleNull, null, 0), Arguments.of(mixed, null, 2), Arguments.of(noNull, null, -1));
    }

    // Group: Equality without same instance (DummyKey already defined)
    private static Stream<Arguments> equalityWithoutSameInstanceProvider() {
        List<DummyKey> lst = new ConcurrentList<>();
        lst.add(new DummyKey(1));
        lst.add(new DummyKey(42));
        lst.add(new DummyKey(3));
        return Stream.of(Arguments.of(lst, new DummyKey(42), 1));
    }

    // Group: Different type non‑matching
    private static Stream<Arguments> differentTypeProvider() {
        List<String> strList = new ConcurrentList<>();
        strList.add("one");
        strList.add("two");
        return Stream.of(Arguments.of(strList, 5, -1));
    }

    @Test
    void testIndexOfExistingElement() {
        assertEquals(0, list.indexOf("alpha"), "indexOf should return the first index of the element");
        assertEquals(1, list.indexOf("beta"), "indexOf should return correct index for middle element");
        assertEquals(2, list.indexOf("gamma"), "indexOf should return correct index for last non‑null element");
    }

    @Test
    void testIndexOfNonExistingElement() {
        assertEquals(-1, list.indexOf("delta"), "indexOf should return -1 when element is not present");
    }

    @Test
    void testIndexOfNullElement() {
        // The list contains a single null at index 3
        assertEquals(3, list.indexOf(null), "indexOf should correctly locate a null element");
    }

    @Test
    void testIndexOfAfterModifications() {
        // Remove an element and add a new one, then verify indexOf reflects the current state
        // list now: [alpha, gamma, null]
        list.remove("beta");
        // list now: [alpha, gamma, null, delta]
        list.add("delta");
        assertEquals(-1, list.indexOf("beta"), "Removed element should no longer be found");
        assertEquals(3, list.indexOf("delta"), "Newly added element should be found at the correct index");
    }

    @Test
    void testIndexOfWithWrappedList() {
        // Verify that indexOf works when ConcurrentList wraps an existing list
        List<String> backing = new ArrayList<>();
        backing.add("x");
        backing.add("y");
        List<String> wrapped = new ConcurrentList<>(backing);
        assertEquals(0, wrapped.indexOf("x"));
        assertEquals(1, wrapped.indexOf("y"));
        assertEquals(-1, wrapped.indexOf("z"));
    }

    @Test
    void testIndexOfDuplicateElement() {
        // Add a duplicate value after the original occurrence
        // now at index 4
        list.add("beta");
        assertEquals(1, list.indexOf("beta"), "indexOf must return the index of the first occurrence even when duplicates exist");
    }

    @Test
    void testIndexOfOnEmptyList() {
        List<String> empty = new ConcurrentList<>();
        assertEquals(-1, empty.indexOf("anyValue"), "Empty list must always return -1");
    }

    @Test
    void testIndexOfNullWhenNullIsFirstElement() {
        List<String> l = new ConcurrentList<>();
        l.add(null);
        l.add("a");
        l.add("b");
        assertEquals(0, l.indexOf(null), "Should return 0 when null is the first element");
    }

    @Test
    void testIndexOfNullInMiddle() {
        List<String> l = new ConcurrentList<>();
        l.add("a");
        l.add("b");
        l.add(null);
        l.add("c");
        assertEquals(2, l.indexOf(null), "Should return the index of the first null occurrence");
    }

    @Test
    void testIndexOfNullWhenAbsent() {
        List<String> l = new ConcurrentList<>();
        l.add("a");
        l.add("b");
        l.add("c");
        assertEquals(-1, l.indexOf(null), "Should return -1 when list contains no nulls");
    }

    @Test
    void testIndexOfEqualityBasedSearch() {
        List<DummyKey> keyList = new ConcurrentList<>();
        // index 0
        keyList.add(new DummyKey(10));
        // index 1
        keyList.add(new DummyKey(42));
        // index 2
        keyList.add(new DummyKey(99));
        // Different instance but same logical value
        DummyKey searchKey = new DummyKey(42);
        assertEquals(1, keyList.indexOf(searchKey), "indexOf should rely on equals() and locate the element with same logical value");
    }

    @Test
    void testIndexOfDifferentType() {
        // List holds Strings, we search with an Integer
        assertEquals(-1, list.indexOf(5), "Searching with a different type must return -1");
    }

    @Test
    void testIndexOfDuringConcurrentModification() throws Exception {
        // Prepare a list that already contains the target element at index 0
        List<String> concurrentList = new ConcurrentList<>();
        concurrentList.add("concurrent");
        concurrentList.add("initial");
        // Background thread continuously adds elements
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            for (int i = 0; i < 1000; i++) {
                concurrentList.add("bg-" + i);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ignored) {
                }
            }
        });
        // While the background thread is running, repeatedly call indexOf
        for (int i = 0; i < 100; i++) {
            assertEquals(0, concurrentList.indexOf("concurrent"), "indexOf must consistently return the correct index despite concurrent adds");
        }
        // Clean up
        future.cancel(true);
        executor.shutdownNow();
    }

    @ParameterizedTest(name = "basic functionality: indexOf({1}) => {2}")
    @MethodSource("basicFunctionalityProvider")
    void testBasicFunctionality(List<String> source, Object search, int expected) {
        assertEquals(expected, source.indexOf(search));
    }

    @ParameterizedTest(name = "empty list: indexOf({1}) => {2}")
    @MethodSource("emptyListProvider")
    void testEmptyListBehavior(List<String> source, Object search, int expected) {
        assertEquals(expected, source.indexOf(search));
    }

    @ParameterizedTest(name = "null handling: indexOf(null) on list => {2}")
    @MethodSource("nullHandlingProvider")
    void testNullHandling(List<String> source, Object search, int expected) {
        assertEquals(expected, source.indexOf(search));
    }

    @ParameterizedTest(name = "equality test: indexOf({1}) => {2}")
    @MethodSource("equalityWithoutSameInstanceProvider")
    void testEqualityWithoutSameInstance(List<DummyKey> source, Object search, int expected) {
        assertEquals(expected, source.indexOf(search));
    }

    @ParameterizedTest(name = "different type: indexOf({1}) => {2}")
    @MethodSource("differentTypeProvider")
    void testDifferentTypeNonMatching(List<String> source, Object search, int expected) {
        assertEquals(expected, source.indexOf(search));
    }
}
