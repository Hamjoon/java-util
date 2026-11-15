package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_addAll_Test {

    /**
     * Instance under test
     */
    private ConcurrentList<Integer> list;

    /**
     * Collection used in the "given" phase of the tests
     */
    private Collection<Integer> source;

    /**
     * A custom collection whose {@code iterator()} method always throws a {@link RuntimeException}.
     */
    private static class ThrowingCollection<E> extends AbstractCollection<E> {

        static final ThrowingCollection<Object> INSTANCE = new ThrowingCollection<>();

        @Override
        public Iterator<E> iterator() {
            throw new RuntimeException("Iterator deliberately fails");
        }

        @Override
        public int size() {
            // size is irrelevant; iterator is never reached in size‑based shortcuts
            return 1;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for each test to guarantee isolation
        list = new ConcurrentList<>();
        // A reusable source collection – individual tests may replace it
        source = new ArrayList<>(Arrays.asList(10, 20, 30));
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required but keeps the pattern consistent
        list = null;
        source = null;
    }

    /**
     * Helper to create a list of integers from a JSON‑like string representation.
     */
    private static List<Integer> ints(String jsonArray) {
        if (jsonArray.trim().equals("[]"))
            return Collections.emptyList();
        String[] parts = jsonArray.replaceAll("[\\[\\]\\s]", "").split(",");
        List<Integer> result = new ArrayList<>();
        for (String p : parts) {
            if (p.equals("null")) {
                result.add(null);
            } else {
                result.add(Integer.valueOf(p));
            }
        }
        return result;
    }

    /**
     * Helper to create a list of strings from a JSON‑like string representation.
     */
    private static List<String> strs(String jsonArray) {
        if (jsonArray.trim().equals("[]"))
            return Collections.emptyList();
        String[] parts = jsonArray.replaceAll("[\\[\\]\\s\"]", "").split(",");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            if (p.equals("null")) {
                result.add(null);
            } else {
                result.add(p);
            }
        }
        return result;
    }

    private static Stream<Arguments> nonEmptyCollectionProvider() {
        return Stream.of(// case 1 – strings, empty initial list
        Arguments.of(strs("[\"a\", \"b\", \"c\"]"), Collections.emptyList(), true, Arrays.asList("a", "b", "c")), // case 2 – integers, list already contains [0]
        Arguments.of(ints("[1, 2, 3]"), Arrays.asList(0), true, Arrays.asList(0, 1, 2, 3)), // case 3 – collection containing null and a regular element
        Arguments.of(Arrays.asList((String) null, "x"), Collections.emptyList(), true, Arrays.asList(null, "x")));
    }

    private static Stream<Arguments> emptyCollectionProvider() {
        return Stream.of(// empty collection on empty list
        Arguments.of(Collections.emptyList(), Collections.emptyList(), false, Collections.emptyList()), // empty collection on non‑empty list (initial ["a"])
        Arguments.of(Collections.emptyList(), Arrays.asList("a"), false, Arrays.asList("a")));
    }

    private static Stream<Arguments> duplicateElementsProvider() {
        return Stream.of(// duplicate strings on empty list
        Arguments.of(strs("[\"a\", \"a\", \"b\"]"), Collections.emptyList(), true, Arrays.asList("a", "a", "b")), // duplicate ints on list already containing [1]
        Arguments.of(ints("[1, 1, 1]"), Arrays.asList(1), true, Arrays.asList(1, 1, 1, 1)));
    }

    @Test
    void addAllAddsAllElementsAndReturnsTrue() {
        // given – list is empty, source contains three elements (setup in @BeforeEach)
        // when
        boolean changed = list.addAll(source);
        // then
        assertTrue(changed, "addAll should return true when the list is modified");
        assertEquals(3, list.size(), "List size must match the number of added elements");
        assertIterableEquals(source, list, "List contents must be exactly the source collection");
    }

    @Test
    void addAllWithEmptyCollectionReturnsFalseAndLeavesListUnchanged() {
        // given
        Collection<Integer> empty = Collections.emptyList();
        // when
        boolean changed = list.addAll(empty);
        // then
        assertFalse(changed, "addAll should return false when the collection is empty");
        assertTrue(list.isEmpty(), "List should remain empty after adding an empty collection");
    }

    @Test
    void addAllOnNonEmptyListAppendsElementsAndReturnsTrue() {
        // given
        list.addAll(Arrays.asList(1, 2));
        Collection<Integer> more = Arrays.asList(3, 4, 5);
        // when
        boolean changed = list.addAll(more);
        // then
        assertTrue(changed, "addAll should return true when new elements are appended");
        assertEquals(5, list.size(), "List size must reflect all elements after the second addAll");
        assertIterableEquals(Arrays.asList(1, 2, 3, 4, 5), list);
    }

    @Test
    void addAllWithNullCollectionThrowsNullPointerException() {
        // given
        Collection<Integer> nullCollection = null;
        // when / then
        assertThrows(NullPointerException.class, () -> list.addAll(nullCollection), "addAll should propagate NullPointerException when the argument is null");
    }

    @Test
    void addAllOnListWrappingExistingArrayListReflectsChangesInUnderlyingList() {
        // given – wrap a pre‑populated ArrayList
        List<Integer> backing = new ArrayList<>(Arrays.asList(100, 200));
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backing);
        Collection<Integer> toAdd = Arrays.asList(300, 400);
        // when
        boolean changed = wrapped.addAll(toAdd);
        // then
        assertTrue(changed);
        assertEquals(4, wrapped.size());
        assertIterableEquals(Arrays.asList(100, 200, 300, 400), wrapped);
        // The underlying list must have been updated as well
        assertIterableEquals(wrapped, backing, "Underlying list should contain the same elements");
    }

    @Test
    void addAllIsThreadSafeWhenMultipleThreadsAddConcurrently() throws InterruptedException {
        // given
        int threadCount = 8;
        int perThreadAdds = 250;
        List<Integer> source = new ArrayList<>(perThreadAdds);
        for (int i = 0; i < perThreadAdds; i++) source.add(i);
        // when
        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            threads[t] = new Thread(() -> list.addAll(source));
            threads[t].start();
        }
        for (Thread t : threads) {
            t.join();
        }
        // then
        // Each thread adds the same collection; total expected size = threadCount * perThreadAdds
        assertEquals(threadCount * perThreadAdds, list.size(), "After concurrent addAll calls, size must equal total number of added elements");
        // Verify that every element that could have been added appears the expected number of times
        Map<Integer, Long> frequencies = new HashMap<>();
        for (Integer i : list) {
            frequencies.merge(i, 1L, Long::sum);
        }
        for (int i = 0; i < perThreadAdds; i++) {
            assertEquals(threadCount, frequencies.getOrDefault(i, 0L), "Element " + i + " should appear exactly " + threadCount + " times");
        }
    }

    @ParameterizedTest(name = "[{index}] addAll non‑empty collection – expectedChanged={2}")
    @MethodSource("nonEmptyCollectionProvider")
    void addAllWithNonEmptyCollection(Object collection, List<?> initial, boolean expectedChanged, List<?> expectedResult) {
        // Setup a ConcurrentList with the appropriate generic type
        ConcurrentList listUnderTest;
        if (initial.isEmpty() && collection instanceof Collection && ((Collection<?>) collection).stream().allMatch(o -> o instanceof String)) {
            listUnderTest = new ConcurrentList<String>();
        } else {
            listUnderTest = new ConcurrentList<Integer>();
        }
        // Populate initial state
        for (Object o : initial) {
            //noinspection unchecked
            listUnderTest.add(o);
        }
        // when
        //noinspection unchecked
        boolean changed = listUnderTest.addAll((Collection) collection);
        // then
        assertEquals(expectedChanged, changed, "Returned boolean should match expectation");
        assertEquals(expectedResult.size(), listUnderTest.size(), "Size after addAll must match expected result size");
        assertIterableEquals(expectedResult, listUnderTest, "List contents must exactly match the expected order");
    }

    @ParameterizedTest(name = "[{index}] addAll empty collection – expectedChanged={2}")
    @MethodSource("emptyCollectionProvider")
    void addAllWithEmptyCollection(Collection<?> toAdd, List<?> initial, boolean expectedChanged, List<?> expectedResult) {
        ConcurrentList listUnderTest = new ConcurrentList<>();
        for (Object o : initial) {
            //noinspection unchecked
            listUnderTest.add(o);
        }
        // when
        //noinspection unchecked
        boolean changed = listUnderTest.addAll((Collection) toAdd);
        // then
        assertEquals(expectedChanged, changed);
        assertEquals(expectedResult.size(), listUnderTest.size());
        assertIterableEquals(expectedResult, listUnderTest);
    }

    @Test
    void addAllWithNullCollectionThrowsException() {
        assertThrows(NullPointerException.class, () -> list.addAll(null), "Passing null should result in NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] addAll duplicate elements – expectedChanged={2}")
    @MethodSource("duplicateElementsProvider")
    void addAllWithDuplicateElements(Object collection, List<?> initial, boolean expectedChanged, List<?> expectedResult) {
        ConcurrentList listUnderTest;
        if (initial.isEmpty() && collection instanceof Collection && ((Collection<?>) collection).stream().allMatch(o -> o instanceof String)) {
            listUnderTest = new ConcurrentList<String>();
        } else {
            listUnderTest = new ConcurrentList<Integer>();
        }
        for (Object o : initial) {
            //noinspection unchecked
            listUnderTest.add(o);
        }
        // when
        //noinspection unchecked
        boolean changed = listUnderTest.addAll((Collection) collection);
        // then
        assertEquals(expectedChanged, changed);
        assertEquals(expectedResult.size(), listUnderTest.size());
        assertIterableEquals(expectedResult, listUnderTest);
    }

    @Test
    void addAllWithOnlyNullsSucceedsIfUnderlyingListAllowsNulls() {
        // given – a fresh ConcurrentList (ArrayList permits nulls)
        List<Integer> nulls = Arrays.asList(null, null);
        // when
        boolean changed = list.addAll(nulls);
        // then
        assertTrue(changed, "addAll should return true when null elements are added");
        assertEquals(2, list.size(), "Size must reflect the two null entries");
        assertEquals(Arrays.asList(null, null), list);
    }

    @Test
    void addAllSizeChangeVerified() {
        // non‑empty addition
        boolean changed = list.addAll(Arrays.asList(1, 2, 3));
        assertTrue(changed);
        assertEquals(3, list.size());
        // empty addition – size must stay the same
        boolean unchanged = list.addAll(Collections.emptyList());
        assertFalse(unchanged);
        assertEquals(3, list.size());
    }

    @Test
    void addAllWithUnmodifiableUnderlyingListPropagatesUnsupportedOperationException() {
        // given – wrap an unmodifiable list
        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        ConcurrentList<String> wrapped = new ConcurrentList<>(unmodifiable);
        Collection<String> toAdd = Arrays.asList("a", "b");
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> wrapped.addAll(toAdd), "addAll should propagate UnsupportedOperationException from the delegate list");
    }

    @Test
    void addAllWithNullInternalDelegateListThrowsNullPointerException() throws Exception {
        // given – a normal ConcurrentList, then force its private 'list' field to null via reflection
        ConcurrentList<Integer> broken = new ConcurrentList<>();
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(broken, null);
        // when / then
        assertThrows(NullPointerException.class, () -> broken.addAll(Collections.singletonList(1)), "If internal delegate list is null, addAll should throw NullPointerException");
    }

    @Test
    void addAllWithThrowingCollectionPropagatesRuntimeException() {
        // given
        Collection<?> throwing = ThrowingCollection.INSTANCE;
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> list.addAll((Collection) throwing), "Exception from collection iterator must propagate");
        assertEquals("Iterator deliberately fails", ex.getMessage());
    }
}
