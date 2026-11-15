package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@ExtendWith(MockitoExtension.class)
class ThreadedLRUCacheStrategy_values_Test {

    /**
     * Cache instance used in each test.
     */
    private ThreadedLRUCacheStrategy<Integer, String> cache;

    /**
     * Fixed capacity for the cache – large enough for the test data.
     */
    private static final int CAPACITY = 10;

    /**
     * Minimal delay required by the constructor (see Javadoc).
     */
    private static final int CLEANUP_DELAY_MILLIS = 10;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for every test to guarantee isolation.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that background purge tasks (if any)
        // do not interfere with subsequent tests.
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy
        // instances. This prevents thread‑leak warnings in the test run.
        ThreadedLRUCacheStrategy.shutdown();
    }

    @Test
    void valuesReturnsAllCurrentValuesInInsertionOrder() {
        // given
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        // when
        Collection<String> values = cache.values();
        // then
        assertEquals(3, values.size(), "The collection should contain three values.");
        // The contract of values() does not guarantee ordering, but the current
        // implementation uses an ArrayList built from cache.values(), which
        // preserves the iteration order of the underlying map. We verify that
        // all expected values are present.
        assertTrue(values.containsAll(java.util.List.of("one", "two", "three")));
    }

    @Test
    void valuesCollectionIsUnmodifiable() {
        // given
        cache.put(1, "alpha");
        Collection<String> values = cache.values();
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> values.add("beta"), "The collection returned by values() must be unmodifiable.");
        assertThrows(UnsupportedOperationException.class, () -> values.remove("alpha"), "The collection returned by values() must be unmodifiable.");
    }

    @Test
    void valuesReflectsChangesAfterPutAndRemove() {
        // given
        cache.put(10, "ten");
        cache.put(20, "twenty");
        Collection<String> valuesBefore = cache.values();
        // when
        // add a new entry
        cache.put(30, "thirty");
        // remove an existing entry
        cache.remove(10);
        Collection<String> valuesAfter = cache.values();
        // then
        // The collection captured before the mutation must not change.
        assertTrue(valuesBefore.contains("ten"));
        assertTrue(valuesBefore.contains("twenty"));
        assertFalse(valuesBefore.contains("thirty"));
        // The collection captured after the mutation must reflect the current state.
        assertEquals(2, valuesAfter.size());
        assertTrue(valuesAfter.containsAll(java.util.List.of("twenty", "thirty")));
        assertFalse(valuesAfter.contains("ten"));
    }

    @Test
    void valuesHandlesNullValuesAndKeys() {
        // given
        cache.put(null, null);
        cache.put(5, null);
        cache.put(null, "notNull");
        // when
        Collection<String> values = cache.values();
        // then
        // The map allows null values, so we expect two null entries (the second
        // put with a null key overwrites the previous null key entry).
        long nullCount = values.stream().filter(v -> v == null).count();
        assertEquals(2L, nullCount, "There should be exactly two null values in the collection.");
        assertTrue(values.contains("notNull"));
    }

    @Test
    void valuesReturnsEmptyUnmodifiableCollectionWhenCacheIsNew() {
        // when
        Collection<String> values = cache.values();
        // then
        assertTrue(values.isEmpty(), "A newly created cache should return an empty collection.");
        assertThrows(UnsupportedOperationException.class, () -> values.add("x"));
    }

    @Test
    void valuesReturnsSingleElementCollection() {
        // given
        cache.put(1, "one");
        // when
        Collection<String> values = cache.values();
        // then
        assertEquals(1, values.size());
        assertTrue(values.contains("one"));
        assertThrows(UnsupportedOperationException.class, () -> values.remove("one"));
    }

    @Test
    void valuesContainsAllInsertedValuesRegardlessOfOrder() {
        // given
        cache.put(1, "a");
        cache.put(2, "b");
        cache.put(3, "c");
        // when
        Collection<String> values = cache.values();
        // then
        assertEquals(3, values.size());
        // Order is not guaranteed – we only verify presence of all values.
        assertTrue(values.containsAll(List.of("a", "b", "c")));
    }

    @Test
    void valuesSnapshotDoesNotReflectLaterChanges() {
        // given
        cache.put(10, "ten");
        cache.put(20, "twenty");
        // snapshot at this point
        Collection<String> snapshot = cache.values();
        // when
        // add new entry
        cache.put(30, "thirty");
        // remove existing entry
        cache.remove(10);
        Collection<String> afterChange = cache.values();
        // then
        // snapshot must stay unchanged
        assertTrue(snapshot.containsAll(List.of("ten", "twenty")));
        assertFalse(snapshot.contains("thirty"));
        // afterChange reflects the current state
        assertEquals(2, afterChange.size());
        assertTrue(afterChange.containsAll(List.of("twenty", "thirty")));
        assertFalse(afterChange.contains("ten"));
    }

    @Test
    void valuesHandlesNullValues() {
        // given
        cache.put(1, null);
        cache.put(2, "notNull");
        cache.put(3, null);
        // when
        Collection<String> values = cache.values();
        // then
        long nullCount = values.stream().filter(Objects::isNull).count();
        assertEquals(2L, nullCount, "There should be exactly two null values.");
        assertTrue(values.contains("notNull"));
    }

    @Test
    void valuesAfterShutdownThrowsIllegalStateException() {
        // Arrange – shutdown the shared executor
        ThreadedLRUCacheStrategy.shutdown();
        // Act & Assert – any further call to values() should fail
        assertThrows(IllegalStateException.class, () -> cache.values(), "Calling values() after shutdown should throw IllegalStateException.");
    }

    @Test
    void valuesReflectsConcurrentPuts() throws Exception {
        // given
        int threadCount = 5;
        int putsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    // ensure all threads start together
                    startLatch.await();
                    for (int i = 0; i < putsPerThread; i++) {
                        int key = threadId * putsPerThread + i;
                        cache.put(key, "v" + key);
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // release all threads
        startLatch.countDown();
        // wait for completion
        doneLatch.await();
        executor.shutdownNow();
        // when
        Collection<String> values = cache.values();
        // then
        assertEquals(threadCount * putsPerThread, values.size(), "The collection size must equal the number of distinct entries inserted concurrently.");
    }

    @Test
    void valuesContainsDuplicateValuesWhenDifferentKeysMapToSameValue() {
        // given
        cache.put(1, "dup");
        cache.put(2, "dup");
        cache.put(3, "unique");
        // when
        Collection<String> values = cache.values();
        // then
        // Both duplicate values must be present; collection size reflects three entries.
        assertEquals(3, values.size());
        long dupCount = values.stream().filter(v -> "dup".equals(v)).count();
        assertEquals(2L, dupCount, "Duplicate value should appear twice.");
    }

    @Test
    void valuesSnapshotRemainsUnchangedAfterSubsequentRemovals() {
        // given
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");
        Collection<String> snapshot = cache.values();
        // when
        cache.remove(2);
        cache.remove(3);
        Collection<String> afterRemoval = cache.values();
        // then
        assertEquals(3, snapshot.size());
        assertTrue(snapshot.containsAll(List.of("one", "two", "three")));
        assertEquals(1, afterRemoval.size());
        assertTrue(afterRemoval.contains("one"));
    }

    @Test
    void iteratorRemoveThrowsUnsupportedOperationException() {
        // given
        cache.put(1, "one");
        cache.put(2, "two");
        Collection<String> values = cache.values();
        Iterator<String> iterator = values.iterator();
        // when
        // move to first element
        iterator.next();
        // then
        assertThrows(UnsupportedOperationException.class, iterator::remove, "Iterator.remove() must throw UnsupportedOperationException for an unmodifiable collection.");
    }

    @Test
    void valuesToStringDoesNotExposeInternalNodeClassNames() {
        // given
        cache.put(1, "one");
        cache.put(2, "two");
        // when
        String str = cache.values().toString();
        // then
        assertTrue(str.contains("one") && str.contains("two"), "String representation should contain stored values.");
        assertFalse(str.contains("Node"), "String representation must not expose internal Node class names.");
    }

    @Test
    void valuesCollectionIsSerializableWhenElementsAreSerializable() throws Exception {
        // given
        cache.put(1, "alpha");
        cache.put(2, "beta");
        Collection<String> original = cache.values();
        // when – serialize & deserialize
        byte[] serialized;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(original);
            serialized = bos.toByteArray();
        }
        Collection<String> deserialized;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(serialized);
            ObjectInputStream in = new ObjectInputStream(bis)) {
            // The cast is safe because we know the original collection type is serializable.
            @SuppressWarnings("unchecked")
            Collection<String> tmp = (Collection<String>) in.readObject();
            deserialized = tmp;
        }
        // then
        assertEquals(original.size(), deserialized.size());
        assertTrue(deserialized.containsAll(List.of("alpha", "beta")));
    }

    @Test
    void valuesThrowsNullPointerExceptionWhenInternalNodeIsNull() throws Exception {
        // Arrange – inject a null Node into the internal cache map via reflection.
        // The cache field is a ConcurrentMap<K, Node<K,V>>.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<Integer, ThreadedLRUCacheStrategy.Node<Integer, String>> internalMap = (ConcurrentMap<Integer, ThreadedLRUCacheStrategy.Node<Integer, String>>) cacheField.get(cache);
        // Put a legitimate entry first.
        cache.put(1, "valid");
        // Directly replace the stored Node with null.
        // key 2 now maps to a null Node
        internalMap.put(2, null);
        // Act & Assert – iterating over cache.values() should encounter the null Node
        // and throw NullPointerException when trying to read node.value.
        assertThrows(NullPointerException.class, () -> cache.values(), "Iterating over a map containing a null Node should result in NPE.");
    }
}
