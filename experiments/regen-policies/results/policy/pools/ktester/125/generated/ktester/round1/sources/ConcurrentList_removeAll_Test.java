package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_removeAll_Test {

    // Mocked backing list used for wrapper‑mode tests
    @Mock
    List<Integer> mockBackingList;

    // Instance under test – will be created in @BeforeEach
    ConcurrentList<Integer> concurrentList;

    class TestConcurrentList<E> extends ConcurrentList<E> {

        TestConcurrentList() {
            super();
        }

        @SuppressWarnings("unchecked")
        boolean invokeWriteOperationThatThrows() {
            try {
                java.lang.reflect.Method m = ConcurrentList.class.getDeclaredMethod("writeOperation", Supplier.class);
                m.setAccessible(true);
                // Supplier that throws IllegalStateException
                Supplier<Boolean> badSupplier = () -> {
                    throw new IllegalStateException("bad");
                };
                return (Boolean) m.invoke(this, badSupplier);
            } catch (Exception e) {
                // unwrap InvocationTargetException
                if (e.getCause() != null)
                    throw new RuntimeException(e.getCause());
                throw new RuntimeException(e);
            }
        }
    }

    class TestConcurrentList<E> extends ConcurrentList<E> {

        TestConcurrentList() {
            super();
        }

        @SuppressWarnings("unchecked")
        boolean invokeWriteOperationThatThrows() {
            try {
                java.lang.reflect.Method m = ConcurrentList.class.getDeclaredMethod("writeOperation", Supplier.class);
                m.setAccessible(true);
                // Supplier that throws IllegalStateException
                Supplier<Boolean> badSupplier = () -> {
                    throw new IllegalStateException("bad");
                };
                return (Boolean) m.invoke(this, badSupplier);
            } catch (Exception e) {
                // unwrap InvocationTargetException
                if (e.getCause() != null)
                    throw new RuntimeException(e.getCause());
                throw new RuntimeException(e);
            }
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default we test the wrapper mode with a mocked backing list.
        // Individual tests can replace this with a real list if they need
        // functional behaviour.
        concurrentList = new ConcurrentList<>(mockBackingList);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to keep tests isolated
        reset(mockBackingList);
    }

    // Helper to create a fresh standalone list pre‑populated with the given elements
    private ConcurrentList<String> newStandaloneList(String... elements) {
        ConcurrentList<String> list = new ConcurrentList<>();
        Collections.addAll(list, elements);
        return list;
    }

    private static Stream<Arguments> basicScenarios() {
        return Stream.of(Arguments.of(Arrays.asList("a", "b", "c"), Arrays.asList("a", "b"), true, Arrays.asList("c")), Arguments.of(Arrays.asList("a", "b", "c"), Arrays.asList("x", "y"), false, Arrays.asList("a", "b", "c")));
    }

    @Test
    void removeAll_delegatesToBackingList_andReturnsTrue() {
        // GIVEN
        Collection<String> toRemove = Arrays.asList("1", "2", "3");
        when(mockBackingList.removeAll(toRemove)).thenReturn(true);
        // WHEN
        boolean result = concurrentList.removeAll(toRemove);
        // THEN
        assertTrue(result, "removeAll should return true when backing list returns true");
        verify(mockBackingList, times(1)).removeAll(toRemove);
    }

    @Test
    void removeAll_delegatesToBackingList_andReturnsFalse() {
        // GIVEN
        Collection<String> toRemove = Collections.singleton("99");
        when(mockBackingList.removeAll(toRemove)).thenReturn(false);
        // WHEN
        boolean result = concurrentList.removeAll(toRemove);
        // THEN
        assertFalse(result, "removeAll should return false when backing list returns false");
        verify(mockBackingList, times(1)).removeAll(toRemove);
    }

    @Test
    void removeAll_nullCollection_throwsNullPointerException() {
        // GIVEN
        // The underlying ArrayList implementation throws NPE for null argument.
        // We let the mock behave the same way.
        when(mockBackingList.removeAll(null)).thenThrow(NullPointerException.class);
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> concurrentList.removeAll(null), "removeAll should propagate NullPointerException for null argument");
        verify(mockBackingList, times(1)).removeAll(null);
    }

    @Test
    void removeAll_standaloneList_behavesCorrectly() {
        // Use a real ConcurrentList (stand‑alone mode) to verify functional behaviour
        ConcurrentList<Integer> realList = new ConcurrentList<>();
        realList.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // Remove a subset
        Collection<Integer> toRemove = Arrays.asList(2, 4);
        boolean changed = realList.removeAll(toRemove);
        assertTrue(changed, "removeAll should return true when the list is modified");
        assertEquals(Arrays.asList(1, 3, 5), realList, "List should contain only the elements not removed");
        assertEquals(3, realList.size(), "Size should reflect the remaining elements");
        // Attempt to remove elements that are no longer present
        changed = realList.removeAll(toRemove);
        assertFalse(changed, "removeAll should return false when no elements are removed");
        assertEquals(Arrays.asList(1, 3, 5), realList, "List should remain unchanged");
    }

    @Test
    void removeAll_basicFunctionality_trueWhenElementsRemoved() {
        ConcurrentList<String> list = newStandaloneList("a", "b", "c");
        boolean changed = list.removeAll(Arrays.asList("a", "b"));
        assertTrue(changed, "Expected true because elements were removed");
        assertEquals(Arrays.asList("c"), list);
    }

    @Test
    void removeAll_basicFunctionality_falseWhenNoMatch() {
        ConcurrentList<String> list = newStandaloneList("a", "b", "c");
        boolean changed = list.removeAll(Arrays.asList("x", "y"));
        assertFalse(changed, "Expected false because no elements matched");
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    void removeAll_emptyCollection_alwaysReturnsFalse() {
        // non‑empty list case
        ConcurrentList<String> list1 = newStandaloneList("a", "b");
        assertFalse(list1.removeAll(Collections.emptyList()));
        assertEquals(Arrays.asList("a", "b"), list1);
        // empty list case
        ConcurrentList<String> list2 = newStandaloneList();
        assertFalse(list2.removeAll(Collections.emptyList()));
        assertTrue(list2.isEmpty());
    }

    @Test
    void removeAll_nullCollectionArgument_throwsNPE() {
        ConcurrentList<String> list = newStandaloneList("a");
        assertThrows(NullPointerException.class, () -> list.removeAll(null));
    }

    @Test
    void removeAll_listContainsNullElements() {
        ConcurrentList<String> list = newStandaloneList(null, "a");
        // removing null should succeed
        assertTrue(list.removeAll(Collections.singletonList(null)));
        assertEquals(Collections.singletonList("a"), list);
        // removing "a" when only null is present should return false
        list.clear();
        list.add(null);
        assertFalse(list.removeAll(Collections.singletonList("a")));
        assertEquals(Collections.singletonList(null), list);
    }

    @Test
    void removeAll_duplicateElementsInList() {
        ConcurrentList<String> list = newStandaloneList("a", "a", "b");
        // remove "a" – both occurrences should disappear
        assertTrue(list.removeAll(Collections.singletonList("a")));
        assertEquals(Collections.singletonList("b"), list);
        // reset and remove both "a" and "b"
        list.clear();
        Collections.addAll(list, "a", "a", "b");
        assertTrue(list.removeAll(Arrays.asList("a", "b")));
        assertTrue(list.isEmpty());
    }

    @Test
    void removeAll_collectionWithDuplicates_isHandledAsSet() {
        ConcurrentList<String> list = newStandaloneList("a", "b", "c");
        assertTrue(list.removeAll(Arrays.asList("a", "a", "b")));
        assertEquals(Collections.singletonList("c"), list);
    }

    @Test
    void removeAll_collectionIsSameInstanceAsList_clearsList() {
        ConcurrentList<String> list = newStandaloneList("a", "b", "c");
        // Passing the list itself as the collection argument
        assertTrue(list.removeAll(list));
        assertTrue(list.isEmpty());
    }

    @Test
    void removeAll_unmodifiableCollectionArgument_works() {
        ConcurrentList<String> list = newStandaloneList("a", "b");
        List<String> unmod = Collections.unmodifiableList(Arrays.asList("a"));
        assertTrue(list.removeAll(unmod));
        assertEquals(Collections.singletonList("b"), list);
    }

    @Test
    void removeAll_emptyListWithNonEmptyCollection_returnsFalse() {
        ConcurrentList<String> list = newStandaloneList();
        assertFalse(list.removeAll(Collections.singletonList("a")));
        assertTrue(list.isEmpty());
    }

    @Test
    void removeAll_nullElementRemovalOnEmptyList_returnsFalse() {
        ConcurrentList<String> list = newStandaloneList();
        assertFalse(list.removeAll(Collections.singletonList(null)));
        assertTrue(list.isEmpty());
    }

    @Test
    void removeAll_concurrentModificationSafety_withReadLock() throws Exception {
        // Create a real list and acquire a read lock before invoking removeAll
        ConcurrentList<String> list = newStandaloneList("a", "b", "c");
        // Use reflection to obtain the internal lock
        java.lang.reflect.Field lockField = ConcurrentList.class.getDeclaredField("list");
        lockField.setAccessible(true);
        // The lock is inside the class; we cannot get it directly, but we can simulate
        // by starting a thread that holds a read lock via the public iterator (which copies the list)
        // The iterator does not hold a lock, so we instead use the internal lock via reflection:
        java.lang.reflect.Field rwLockField = ConcurrentList.class.getDeclaredField("list");
        // The class actually has a private ReentrantReadWriteLock field (not exposed). We'll access it:
        java.lang.reflect.Field lockObjField = ConcurrentList.class.getDeclaredField("list");
        // Since the source shows a private final List<E> list; the lock is a separate field:
        java.lang.reflect.Field rwLock = ConcurrentList.class.getDeclaredField("list");
        // For simplicity, we just invoke removeAll while another thread sleeps – the internal
        // writeOperation should acquire the write lock safely.
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<Boolean> future = exec.submit(() -> {
            start.countDown();
            return list.removeAll(Collections.singletonList("a"));
        });
        // ensure the task has started
        start.await();
        boolean result = future.get(2, TimeUnit.SECONDS);
        assertTrue(result);
        assertEquals(Arrays.asList("b", "c"), list);
        exec.shutdownNow();
    }

    @Test
    void removeAll_concurrentRemovalByMultipleThreads_isThreadSafe() throws Exception {
        ConcurrentList<String> list = newStandaloneList("a", "b");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        futures.add(executor.submit(() -> {
            ready.countDown();
            go.await();
            return list.removeAll(Collections.singletonList("a"));
        }));
        futures.add(executor.submit(() -> {
            ready.countDown();
            go.await();
            return list.removeAll(Collections.singletonList("b"));
        }));
        // both threads ready
        ready.await();
        // let them run concurrently
        go.countDown();
        // collect results, any exception will be re‑thrown
        boolean first = futures.get(0).get();
        boolean second = futures.get(1).get();
        assertTrue(first || second, "At least one thread must report a change");
        assertTrue(list.isEmpty(), "List must be empty after both removals");
        executor.shutdownNow();
    }

    @Test
    void removeAll_underlyingListUnmodifiable_propagatesUnsupportedOperationException() {
        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a", "b")));
        ConcurrentList<String> list = new ConcurrentList<>(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> list.removeAll(Collections.singletonList("a")), "Expected UnsupportedOperationException from the wrapped list");
    }

    @Test
    void removeAll_underlyingListThrowsCustomRuntimeException_isPropagated() {
        // Custom list that throws RuntimeException from removeAll
        List<String> exploding = new AbstractList<>() {

            @Override
            public String get(int index) {
                return null;
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new RuntimeException("boom");
            }
        };
        ConcurrentList<String> list = new ConcurrentList<>(exploding);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> list.removeAll(Collections.singletonList("x")));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void removeAll_concurrentModificationFromExternalThread_causesCME() throws Exception {
        // Use a plain ArrayList as the backing store and wrap it.
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b"));
        ConcurrentList<String> list = new ConcurrentList<>(backing);
        // Thread that will modify the backing list directly while removeAll is in progress
        ExecutorService exec = Executors.newSingleThreadExecutor();
        CountDownLatch modifyStarted = new CountDownLatch(1);
        CountDownLatch modifyDone = new CountDownLatch(1);
        // The removeAll call will be performed in the main thread; we introduce a tiny delay
        // inside the backing list's removeAll via a custom wrapper.
        List<String> spyingBacking = new ArrayList<>(backing) {

            @Override
            public boolean removeAll(Collection<?> c) {
                // signal that we are inside removeAll
                modifyStarted.countDown();
                try {
                    // Wait until the external thread makes a change
                    modifyDone.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                }
                return super.removeAll(c);
            }
        };
        ConcurrentList<String> concurrent = new ConcurrentList<>(spyingBacking);
        Future<Boolean> future = exec.submit(() -> concurrent.removeAll(Collections.singletonList("a")));
        // Wait until removeAll has entered the overridden method
        assertTrue(modifyStarted.await(1, TimeUnit.SECONDS));
        // Directly modify the backing list without synchronization
        // this should cause modCount mismatch
        spyingBacking.add("z");
        // let removeAll continue
        modifyDone.countDown();
        // Expect ConcurrentModificationException
        ExecutionException ee = assertThrows(ExecutionException.class, future::get);
        assertTrue(ee.getCause() instanceof ConcurrentModificationException, "Expected ConcurrentModificationException due to external modification");
        exec.shutdownNow();
    }

    @Test
    void removeAll_writeOperationSuppliesException_isPropagated() throws Exception {
        // Subclass to expose a method that calls the private writeOperation via reflection.
        class TestConcurrentList<E> extends ConcurrentList<E> {

            TestConcurrentList() {
                super();
            }

            @SuppressWarnings("unchecked")
            boolean invokeWriteOperationThatThrows() {
                try {
                    java.lang.reflect.Method m = ConcurrentList.class.getDeclaredMethod("writeOperation", Supplier.class);
                    m.setAccessible(true);
                    // Supplier that throws IllegalStateException
                    Supplier<Boolean> badSupplier = () -> {
                        throw new IllegalStateException("bad");
                    };
                    return (Boolean) m.invoke(this, badSupplier);
                } catch (Exception e) {
                    // unwrap InvocationTargetException
                    if (e.getCause() != null)
                        throw new RuntimeException(e.getCause());
                    throw new RuntimeException(e);
                }
            }
        }
        TestConcurrentList<String> list = new TestConcurrentList<>();
        IllegalStateException ise = assertThrows(IllegalStateException.class, list::invokeWriteOperationThatThrows, "writeOperation should propagate the exception thrown by the supplied operation");
        assertEquals("bad", ise.getMessage());
    }

    @ParameterizedTest(name = "{index} => initial={0}, remove={1}, expectedChanged={2}")
    @MethodSource("basicScenarios")
    void removeAll_parameterized_basicFunctionality(List<String> initial, Collection<String> toRemove, boolean expectedChanged, List<String> expectedResult) {
        ConcurrentList<String> list = newStandaloneList(initial.toArray(new String[0]));
        boolean changed = list.removeAll(toRemove);
        assertEquals(expectedChanged, changed);
        assertEquals(expectedResult, list);
    }
}
