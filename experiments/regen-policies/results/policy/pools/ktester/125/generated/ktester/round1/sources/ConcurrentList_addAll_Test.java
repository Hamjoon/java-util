package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_addAll_Test {

    /**
     * A mock backing list used for the wrapper‑mode tests.
     */
    @Mock
    private List<Integer> mockBackingList;

    /**
     * The ConcurrentList instance under test – either a real list or a wrapper around the mock.
     */
    private ConcurrentList<Integer> listUnderTest;

    /**
     * Collection whose {@code addAll} throws a RuntimeException.
     */
    private static class ThrowingCollection<E> implements Collection<E> {

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new RuntimeException("Intentional failure in addAll");
        }

        // Minimal stub implementations for the other methods
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<E> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return a;
        }

        @Override
        public boolean add(E e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    /**
     * Collection whose iterator throws {@link ConcurrentModificationException}.
     */
    private static class BadIteratorCollection<E> implements Collection<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    throw new ConcurrentModificationException();
                }

                @Override
                public E next() {
                    throw new ConcurrentModificationException();
                }

                @Override
                public void remove() {
                    throw new ConcurrentModificationException();
                }
            };
        }

        // Minimal stub implementations
        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return a;
        }

        @Override
        public boolean add(E e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default we work with a fresh, standalone ConcurrentList.
        listUnderTest = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly necessary but keeps the test tidy.
        listUnderTest = null;
    }

    private static Stream<Arguments> normalAddAllProvider() {
        return Stream.of(// non‑empty collection
        Arguments.of(Arrays.asList("alpha", "beta"), true, List.of("alpha", "beta")), // empty collection
        Arguments.of(Collections.emptyList(), false, Collections.emptyList()), // collection containing a null element
        Arguments.of(Arrays.asList(null, "gamma"), true, Arrays.asList(null, "gamma")), // collection with duplicate values
        Arguments.of(Arrays.asList("delta", "delta"), true, Arrays.asList("delta", "delta")));
    }

    /**
     * Verify that {@code addAll(Collection)} correctly adds all elements
     * and reports the proper return value for a non‑empty collection.
     */
    @Test
    void addAllAddsElementsAndReturnsTrue() {
        // given
        Collection<Integer> toAdd = Arrays.asList(10, 20, 30);
        // when
        boolean result = listUnderTest.addAll(toAdd);
        // then
        assertTrue(result, "addAll should return true when the list changes");
        assertEquals(3, listUnderTest.size(), "Size should reflect the added elements");
        assertEquals(toAdd, listUnderTest, "List contents should match the added collection");
    }

    /**
     * Adding an empty collection must not modify the list and must return {@code false}.
     */
    @Test
    void addAllEmptyCollectionReturnsFalseAndLeavesListUnchanged() {
        // given
        listUnderTest.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> empty = Collections.emptyList();
        // when
        boolean result = listUnderTest.addAll(empty);
        // then
        assertFalse(result, "addAll should return false when nothing is added");
        assertEquals(3, listUnderTest.size(), "Size should stay unchanged");
        assertEquals(Arrays.asList(1, 2, 3), listUnderTest, "Existing elements must remain");
    }

    /**
     * Passing {@code null} as the collection should result in a {@link NullPointerException},
     * mirroring the behaviour of the underlying {@code List#addAll}.
     */
    @Test
    void addAllNullCollectionThrowsNPE() {
        assertThrows(NullPointerException.class, () -> listUnderTest.addAll(null), "addAll(null) should throw NullPointerException");
    }

    /**
     * When the {@code ConcurrentList} wraps an existing list, the call to {@code addAll}
     * must be delegated to the backing list. This test uses a Mockito mock to verify
     * the delegation and the returned value.
     */
    @Test
    void addAllDelegatesToBackingListInWrapperMode() {
        // given – a mock backing list that returns true when addAll is called
        Collection<Integer> toAdd = Arrays.asList(5, 6);
        when(mockBackingList.addAll(toAdd)).thenReturn(true);
        // wrap the mock with ConcurrentList
        listUnderTest = new ConcurrentList<>(mockBackingList);
        // when
        boolean result = listUnderTest.addAll(toAdd);
        // then
        assertTrue(result, "Result should be whatever the backing list returned");
        verify(mockBackingList, times(1)).addAll(toAdd);
    }

    /**
     * The wrapper mode must also propagate a {@code false} result from the backing list.
     */
    @Test
    void addAllDelegatesAndPropagatesFalseResult() {
        // given – mock returns false (e.g., empty collection)
        Collection<Integer> toAdd = Collections.emptyList();
        when(mockBackingList.addAll(toAdd)).thenReturn(false);
        listUnderTest = new ConcurrentList<>(mockBackingList);
        // when
        boolean result = listUnderTest.addAll(toAdd);
        // then
        assertFalse(result, "Result should reflect the backing list's false return");
        verify(mockBackingList, times(1)).addAll(toAdd);
    }

    @ParameterizedTest(name = "addAll({0}) returns {1}")
    @MethodSource("normalAddAllProvider")
    void addAllNormalAndEdgeCases(Collection<String> input, boolean expectedResult, List<String> expectedContent) {
        // Use a fresh ConcurrentList<String>
        ConcurrentList<String> stringList = new ConcurrentList<>();
        // when
        boolean result = stringList.addAll(input);
        // then
        assertEquals(expectedResult, result, "Unexpected return value from addAll");
        assertEquals(expectedContent, stringList, "List contents do not match expectation");
    }

    @Test
    void addAllConcurrentMultipleThreads() throws Exception {
        // given
        ConcurrentList<String> concurrentList = new ConcurrentList<>();
        List<String> toAdd = Arrays.asList("t1", "t2", "t3");
        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();
        // when
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                // wait for the signal to start together
                startLatch.await();
                boolean r = concurrentList.addAll(toAdd);
                doneLatch.countDown();
                return r;
            }));
        }
        // let all threads proceed
        startLatch.countDown();
        // wait for all to finish
        doneLatch.await();
        executor.shutdownNow();
        // then
        // every thread should have returned true because each call adds three new elements
        for (Future<Boolean> f : futures) {
            assertTrue(f.get(), "Each concurrent addAll should return true");
        }
        assertEquals(threadCount * toAdd.size(), concurrentList.size(), "Final size must reflect all additions without loss");
        // Verify that the list actually contains the expected number of each element
        Map<String, Long> counts = new HashMap<>();
        for (String s : concurrentList) {
            counts.merge(s, 1L, Long::sum);
        }
        for (String s : toAdd) {
            assertEquals(threadCount, counts.getOrDefault(s, 0L).longValue(), "Each element should appear exactly " + threadCount + " times");
        }
    }

    @Test
    void addAllConcurrentEmptyCollections() throws Exception {
        // given
        ConcurrentList<Integer> concurrentList = new ConcurrentList<>();
        // seed with some data
        concurrentList.addAll(Arrays.asList(1, 2, 3));
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();
        // when
        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                boolean r = concurrentList.addAll(Collections.emptyList());
                doneLatch.countDown();
                return r;
            }));
        }
        startLatch.countDown();
        doneLatch.await();
        executor.shutdownNow();
        // then
        for (Future<Boolean> f : futures) {
            assertFalse(f.get(), "addAll(empty) should return false even under concurrency");
        }
        assertEquals(3, concurrentList.size(), "List size must stay unchanged");
        assertEquals(Arrays.asList(1, 2, 3), concurrentList, "Original elements must remain untouched");
    }

    @Test
    void addAllCollectionThatThrowsRuntimeException() {
        // given
        ThrowingCollection<String> bad = new ThrowingCollection<>();
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> listUnderTest.addAll(bad), "Exception from the collection should propagate");
        assertEquals("Intentional failure in addAll", ex.getMessage());
    }

    @Test
    void addAllCollectionWithFailingIteratorThrowsCME() {
        // given
        BadIteratorCollection<String> badIter = new BadIteratorCollection<>();
        // when / then
        assertThrows(ConcurrentModificationException.class, () -> listUnderTest.addAll(badIter), "Iterator exception should bubble up");
    }

    @Test
    void addAllSelfReferenceDoublesElements() {
        // given
        ConcurrentList<String> selfList = new ConcurrentList<>();
        selfList.addAll(Arrays.asList("a", "b", "c"));
        int originalSize = selfList.size();
        // when
        // add itself
        boolean result = selfList.addAll(selfList);
        // then
        assertTrue(result, "addAll(self) should return true because the list changes");
        assertEquals(originalSize * 2, selfList.size(), "Size should double after adding the list to itself");
        // Verify that the order is original elements followed by a copy of them
        List<String> expected = new ArrayList<>(Arrays.asList("a", "b", "c"));
        expected.addAll(Arrays.asList("a", "b", "c"));
        assertEquals(expected, selfList, "Contents should be original elements followed by a duplicate");
    }

    @Test
    void addAllSubListViewAppendsElements() {
        // given
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(Arrays.asList("x", "y", "z", "w"));
        // ["x","y"]
        List<String> sub = list.subList(0, 2);
        int originalSize = list.size();
        // when
        boolean result = list.addAll(sub);
        // then
        assertTrue(result, "addAll(subList) should return true");
        assertEquals(originalSize + sub.size(), list.size(), "Size should increase by the sub‑list size");
        List<String> expected = new ArrayList<>(Arrays.asList("x", "y", "z", "w"));
        expected.addAll(Arrays.asList("x", "y"));
        assertEquals(expected, list, "List should contain original elements plus the sub‑list elements");
    }

    @Test
    void addAllLargeCollection() {
        // given
        List<Integer> large = new ArrayList<>(10_000);
        for (int i = 0; i < 10_000; i++) {
            large.add(i);
        }
        // when
        boolean result = listUnderTest.addAll(large);
        // then
        assertTrue(result, "Adding a large collection should return true");
        assertEquals(10_000, listUnderTest.size(), "Size must reflect all added elements");
        // Spot‑check a few values to ensure order is preserved
        assertEquals(0, listUnderTest.get(0));
        assertEquals(5_000, listUnderTest.get(5_000));
        assertEquals(9_999, listUnderTest.get(9_999));
    }

    @Test
    void addAllOnUnmodifiableBackingListThrowsUOE() {
        // given – wrap an unmodifiable list (no modifications allowed)
        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        ConcurrentList<String> wrapped = new ConcurrentList<>(unmodifiable);
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> wrapped.addAll(Arrays.asList("x")), "Adding to an unmodifiable backing list must throw UOE");
    }

    @Test
    void addAllOnUnmodifiableBackingListWithNullThrowsUOE() {
        // given – same unmodifiable list, attempt to add a collection containing null
        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        ConcurrentList<String> wrapped = new ConcurrentList<>(unmodifiable);
        Collection<String> withNull = Collections.singletonList(null);
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> wrapped.addAll(withNull), "Even a null element cannot be added to an unmodifiable backing list");
    }
}
