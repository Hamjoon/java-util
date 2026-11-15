package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_informAdditionalUsage_Test {

    private TrackingMap<String, Integer> trackingMap;

    /**
     * A collection whose iterator immediately throws a RuntimeException.
     */
    private static class ExceptionThrowingCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    throw new RuntimeException("Iterator failure");
                }

                @Override
                public E next() {
                    throw new RuntimeException("Iterator failure");
                }

                @Override
                public void remove() {
                    /* no‑op */
                }
            };
        }

        @Override
        public int size() {
            return 0;
        }
    }

    /**
     * A collection that throws ConcurrentModificationException when its iterator
     * is used. The exception is triggered on the first call to {@code next()}.
     */
    private static class ConcurrentModificationCollection<E> extends AbstractCollection<E> {

        private final List<E> data;

        private boolean iterated = false;

        ConcurrentModificationCollection(Collection<? extends E> c) {
            this.data = new ArrayList<>(c);
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < data.size();
                }

                @Override
                public E next() {
                    if (iterated) {
                        throw new ConcurrentModificationException("Simulated concurrent modification");
                    }
                    // cause CME on the *next* call
                    iterated = true;
                    return data.get(index++);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple HashMap as the backing map – the behaviour of informAdditionalUsage
        // does not depend on the concrete Map implementation.
        trackingMap = new TrackingMap<>(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required.
        trackingMap = null;
    }

    @Test
    void informAdditionalUsageAddsAllKeysFromCollection() {
        // GIVEN
        Collection<String> additional = Arrays.asList("alpha", "beta", "gamma");
        // WHEN
        trackingMap.informAdditionalUsage(additional);
        // THEN
        Set<String> used = trackingMap.keysUsed();
        assertEquals(3, used.size(), "All keys from the collection should be recorded");
        assertTrue(used.containsAll(additional), "Recorded keys should match the supplied collection");
    }

    @Test
    void informAdditionalUsageMergesWithExistingKeys() {
        // GIVEN
        trackingMap.informAdditionalUsage(Collections.singleton("first"));
        Collection<String> more = Arrays.asList("second", "third");
        // WHEN
        trackingMap.informAdditionalUsage(more);
        // THEN
        Set<String> used = trackingMap.keysUsed();
        assertEquals(3, used.size(), "Keys should be merged, not replaced");
        assertTrue(used.containsAll(Arrays.asList("first", "second", "third")));
    }

    @Test
    void informAdditionalUsageWithEmptyCollectionDoesNotChangeKeysUsed() {
        // GIVEN
        trackingMap.informAdditionalUsage(Collections.singleton("only"));
        Collection<String> empty = Collections.emptyList();
        // WHEN
        trackingMap.informAdditionalUsage(empty);
        // THEN
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "Empty collection must not affect existing keys");
        assertTrue(used.contains("only"));
    }

    @Test
    void informAdditionalUsageNullCollectionThrowsNPE() {
        // GIVEN
        Collection<String> nullCollection = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> trackingMap.informAdditionalUsage(nullCollection), "Passing a null collection should result in NullPointerException");
    }

    @Test
    void informAdditionalUsageEmptyCollectionLeavesKeysUsedEmpty() {
        // GIVEN
        Collection<String> empty = Collections.emptyList();
        // WHEN
        trackingMap.informAdditionalUsage(empty);
        // THEN
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.isEmpty(), "keysUsed() should be empty after adding an empty collection");
    }

    @Test
    void informAdditionalUsageAddsNewKeysOnly() {
        // GIVEN
        trackingMap.informAdditionalUsage(Arrays.asList("a", "b"));
        // "a" already present
        Collection<String> additional = Arrays.asList("a", "c");
        // WHEN
        trackingMap.informAdditionalUsage(additional);
        // THEN
        Set<String> used = trackingMap.keysUsed();
        assertEquals(3, used.size(), "Distinct keys should be stored only once");
        assertTrue(used.containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    void informAdditionalUsageAcceptsNullElement() {
        // GIVEN
        Collection<String> withNull = Collections.singletonList(null);
        // WHEN
        trackingMap.informAdditionalUsage(withNull);
        // THEN
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "Null element should be stored as a key");
        assertTrue(used.contains(null));
    }

    @Test
    void informAdditionalUsageDeduplicatesElements() {
        // GIVEN
        Collection<String> duplicates = Arrays.asList("a", "a", "b");
        // WHEN
        trackingMap.informAdditionalUsage(duplicates);
        // THEN
        Set<String> used = trackingMap.keysUsed();
        assertEquals(2, used.size(), "Duplicate entries must result in a single key per distinct value");
        assertTrue(used.containsAll(Arrays.asList("a", "b")));
    }

    @Test
    void informAdditionalUsageDeduplicatesNullElements() {
        // GIVEN
        Collection<String> duplicateNulls = Arrays.asList(null, null);
        // WHEN
        trackingMap.informAdditionalUsage(duplicateNulls);
        // THEN
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "Multiple null entries should result in a single null key");
        assertTrue(used.contains(null));
    }

    @Test
    void informAdditionalUsageIsUnaffectedBySubsequentCollectionMutation() {
        // GIVEN
        List<String> mutable = new ArrayList<>(Collections.singletonList("x"));
        // WHEN
        trackingMap.informAdditionalUsage(mutable);
        // mutate after the call
        mutable.add("y");
        // THEN
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "Internal set must not reflect mutations after the call");
        assertTrue(used.contains("x"));
        assertFalse(used.contains("y"));
    }

    @Test
    void informAdditionalUsageAcceptsUnmodifiableCollection() {
        // GIVEN
        Collection<String> unmodifiable = Collections.unmodifiableList(Arrays.asList("u", "v"));
        // WHEN
        trackingMap.informAdditionalUsage(unmodifiable);
        // THEN
        Set<String> used = trackingMap.keysUsed();
        assertEquals(2, used.size(), "Unmodifiable collections should be read without error");
        assertTrue(used.containsAll(Arrays.asList("u", "v")));
    }

    @Test
    void informAdditionalUsagePropagatesRuntimeExceptionFromIterator() {
        // GIVEN
        Collection<String> bad = new ExceptionThrowingCollection<>();
        // WHEN / THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> trackingMap.informAdditionalUsage(bad), "Exception thrown by iterator should propagate");
        assertEquals("Iterator failure", ex.getMessage());
    }

    @Test
    void informAdditionalUsagePropagatesConcurrentModificationException() {
        // GIVEN
        Collection<String> concurrent = new ConcurrentModificationCollection<>(Arrays.asList("c1", "c2"));
        // WHEN / THEN
        assertThrows(ConcurrentModificationException.class, () -> trackingMap.informAdditionalUsage(concurrent), "Concurrent modification during iteration should propagate the exception");
    }

    @Test
    void informAdditionalUsageHandlesLargeCollectionEfficiently() {
        // GIVEN
        List<Integer> large = IntStream.rangeClosed(0, 999).boxed().collect(Collectors.toList());
        // Use a TrackingMap<Integer, Integer> for this test
        TrackingMap<Integer, Integer> intMap = new TrackingMap<>(new HashMap<>());
        // WHEN
        intMap.informAdditionalUsage(large);
        // THEN
        Set<Integer> used = intMap.keysUsed();
        assertEquals(1000, used.size(), "All 1000 distinct keys must be recorded");
        assertTrue(used.containsAll(large));
    }
}
