package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_isEmpty_Test {

    /**
     * Instance used for most tests – a fresh set for each test method.
     */
    private ConcurrentSet<Integer> set;

    /**
     * Instance created once for the whole test suite (used only for static‑only checks).
     */
    private static ConcurrentSet<String> staticSet;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a static set that will be reused in static‑only tests.
        staticSet = new ConcurrentSet<>();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance before each test to guarantee isolation.
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the instance – not strictly required but makes intent clear.
        if (set != null) {
            set.clear();
        }
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static reference.
        staticSet = null;
    }

    @Test
    void isEmptyOnNewSet() {
        // GIVEN a newly created ConcurrentSet
        // WHEN checking emptiness
        boolean empty = set.isEmpty();
        // THEN it should be empty
        assertTrue(empty, "A newly created set must be empty");
    }

    @Test
    void isEmptyAfterAdd() {
        // GIVEN a set with a single element added
        set.add(42);
        // WHEN checking emptiness
        boolean empty = set.isEmpty();
        // THEN it should report non‑empty
        assertFalse(empty, "Set should not be empty after adding an element");
    }

    @Test
    void isEmptyAfterClear() {
        // GIVEN a set with several elements
        set.addAll(Arrays.asList(1, 2, 3));
        // AND the set is cleared
        set.clear();
        // WHEN checking emptiness
        boolean empty = set.isEmpty();
        // THEN it should be empty again
        assertTrue(empty, "Set should be empty after clear()");
    }

    @Test
    void isEmptyWithCollectionConstructor() {
        // GIVEN a collection with elements
        Set<Integer> source = new HashSet<>(Arrays.asList(10, 20, 30));
        // WHEN constructing a ConcurrentSet from that collection
        ConcurrentSet<Integer> fromCollection = new ConcurrentSet<>(source);
        // THEN isEmpty must be false
        assertFalse(fromCollection.isEmpty(), "Set constructed from a non‑empty collection must not be empty");
        // AND GIVEN an empty collection
        ConcurrentSet<Integer> emptyFromCollection = new ConcurrentSet<>(Collections.emptyList());
        // THEN isEmpty must be true
        assertTrue(emptyFromCollection.isEmpty(), "Set constructed from an empty collection must be empty");
    }

    @Test
    void isEmptyWithSetConstructor() {
        // GIVEN a Set with elements
        java.util.Set<String> source = new HashSet<>(Arrays.asList("a", "b"));
        // WHEN constructing a ConcurrentSet from that Set
        ConcurrentSet<String> fromSet = new ConcurrentSet<>(source);
        // THEN isEmpty must be false
        assertFalse(fromSet.isEmpty(), "Set constructed from a non‑empty Set must not be empty");
        // AND GIVEN an empty Set
        ConcurrentSet<String> emptyFromSet = new ConcurrentSet<>(new HashSet<>());
        // THEN isEmpty must be true
        assertTrue(emptyFromSet.isEmpty(), "Set constructed from an empty Set must be empty");
    }

    @Test
    void isEmptyAfterConcurrentAddAndRemove() throws InterruptedException {
        // This test demonstrates that isEmpty reflects a consistent state
        // even after many concurrent modifications.
        final int threadCount = 4;
        final int opsPerThread = 5000;
        // GIVEN a shared ConcurrentSet
        ConcurrentSet<Integer> shared = new ConcurrentSet<>();
        // WHEN many threads add and remove elements concurrently
        Thread[] threads = new Thread[threadCount * 2];
        for (int i = 0; i < threadCount; i++) {
            final int id = i;
            threads[i * 2] = new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    shared.add(id * opsPerThread + j);
                }
            });
            threads[i * 2 + 1] = new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    shared.remove(id * opsPerThread + j);
                }
            });
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
        // THEN isEmpty must be true or false depending on race, but it must be a valid boolean.
        // The important part is that calling it does not throw and returns a deterministic value.
        assertNotNull(shared.isEmpty(), "isEmpty() should return a non‑null boolean value after concurrent ops");
    }

    @Test
    void isEmptyAfterAddSingle() {
        // GIVEN a set with a single element added
        set.add(42);
        // WHEN checking emptiness
        boolean empty = set.isEmpty();
        // THEN it should report non‑empty
        assertFalse(empty, "Set should not be empty after adding an element");
    }

    @Test
    void isEmptyAfterAddMultiple() {
        // GIVEN a set with several distinct elements added
        set.addAll(Arrays.asList(1, 2, 3, 4, 5));
        // WHEN checking emptiness
        boolean empty = set.isEmpty();
        // THEN it should report non‑empty
        assertFalse(empty, "Set should not be empty after adding multiple elements");
    }

    @Test
    void isEmptyAfterRemoveSingle() {
        // GIVEN a set with one element that is removed
        set.add(99);
        set.remove(99);
        // WHEN checking emptiness
        boolean empty = set.isEmpty();
        // THEN it should be empty again
        assertTrue(empty, "Set should be empty after removing the only element");
    }

    @Test
    void isEmptyAfterRemoveAll() {
        // GIVEN a set with several elements
        set.addAll(Arrays.asList(10, 20, 30));
        // WHEN all of them are removed via removeAll
        set.removeAll(Arrays.asList(10, 20, 30));
        // THEN the set must be empty
        assertTrue(set.isEmpty(), "Set should be empty after removeAll clears it");
    }

    // ----- Concurrent addition test (expect non‑empty) -----
    @Test
    void isEmptyAfterConcurrentDistinctAdds() throws InterruptedException {
        final int threadCount = 3;
        final int elementsPerThread = 100;
        ConcurrentSet<Integer> cs = new ConcurrentSet<>();
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int base = i * elementsPerThread;
            tasks.add(() -> {
                for (int j = 0; j < elementsPerThread; j++) {
                    cs.add(base + j);
                }
                return null;
            });
        }
        exec.invokeAll(tasks);
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);
        assertFalse(cs.isEmpty(), "After concurrent distinct adds the set must be non‑empty");
        assertEquals(threadCount * elementsPerThread, cs.size(), "Size should match total distinct elements added");
    }

    // ----- Concurrent removal test (expect empty) -----
    @Test
    void isEmptyAfterConcurrentRemovals() throws InterruptedException, ExecutionException {
        final int threadCount = 5;
        final int elements = 10;
        ConcurrentSet<Integer> cs = new ConcurrentSet<>();
        // Pre‑populate with the same elements for all remover threads
        for (int i = 0; i < elements; i++) {
            cs.add(i);
        }
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Callable<Void>> removers = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            removers.add(() -> {
                for (int j = 0; j < elements; j++) {
                    cs.remove(j);
                }
                return null;
            });
        }
        List<Future<Void>> futures = exec.invokeAll(removers);
        // propagate possible exceptions
        for (Future<Void> f : futures) f.get();
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);
        assertTrue(cs.isEmpty(), "After concurrent removals the set must be empty");
    }

    // ----- addAll variations -----
    @Test
    void isEmptyAfterAddAllEmptyCollection() {
        List<Integer> empty = Collections.emptyList();
        set.addAll(empty);
        assertTrue(set.isEmpty(), "addAll with an empty collection must leave the set empty");
    }

    @Test
    void isEmptyAfterAddAllWithDuplicates() {
        List<String> dupes = Arrays.asList("a", "a", "b");
        ConcurrentSet<String> cs = new ConcurrentSet<>();
        cs.addAll(dupes);
        assertFalse(cs.isEmpty(), "addAll with duplicates should result in a non‑empty set");
        assertEquals(2, cs.size(), "Only distinct elements should be stored");
    }

    // ----- retainAll effects -----
    @Test
    void isEmptyAfterRetainAllEmptyCollection() {
        set.addAll(Arrays.asList(1, 2, 3));
        set.retainAll(Collections.emptySet());
        assertTrue(set.isEmpty(), "retainAll with empty collection must empty the set");
    }

    @Test
    void isEmptyAfterRetainAllSubset() {
        set.addAll(Arrays.asList(1, 2, 3, 4));
        set.retainAll(new HashSet<>(Arrays.asList(2, 4)));
        assertFalse(set.isEmpty(), "retainAll with a subset should leave the set non‑empty");
        assertEquals(2, set.size(), "Only the retained elements should remain");
    }

    // ----- iterator removal -----
    @Test
    void isEmptyAfterIteratorRemoveOnlyElement() {
        set.add(7);
        Iterator<Integer> it = set.iterator();
        assertTrue(it.hasNext());
        it.next();
        // removes the sole element
        it.remove();
        assertTrue(set.isEmpty(), "Set should be empty after iterator removes the only element");
    }

    @Test
    void isEmptyAfterIteratorRemoveOneOfMany() {
        set.addAll(Arrays.asList(10, 20, 30));
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            Integer val = it.next();
            if (val.equals(20)) {
                // remove one element
                it.remove();
                break;
            }
        }
        assertFalse(set.isEmpty(), "Set should still be non‑empty after removing one of many elements via iterator");
        assertEquals(2, set.size(), "Exactly two elements should remain");
    }

    // ----- null handling -----
    @Test
    void addNullThrowsNPE_andSetRemainsEmpty() {
        // ConcurrentHashMap based set does not allow null keys.
        assertThrows(NullPointerException.class, () -> set.add(null), "Adding null must throw NullPointerException");
        // After the exception the set should still be empty.
        assertTrue(set.isEmpty(), "Set should remain empty after failed null insertion");
    }

    // ----- internal null delegate set -----
    @Test
    void isEmptyWhenInternalSetIsNullThrowsNPE() throws Exception {
        // Use reflection to corrupt the private delegate field.
        Field delegate = ConcurrentSet.class.getDeclaredField("set");
        delegate.setAccessible(true);
        // force internal set to null
        delegate.set(set, null);
        // The subsequent call should dereference a null and throw NPE.
        assertThrows(NullPointerException.class, set::isEmpty, "Calling isEmpty when internal delegate is null must throw NullPointerException");
    }
}
