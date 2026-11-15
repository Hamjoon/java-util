package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

class TrackingMap_informAdditionalUsage_Test {

    private TrackingMap<String, Integer> trackingMap;

    private Map<String, Integer> internalMap;

    /**
     * Collection that throws a RuntimeException on the second call to next().
     */
    private static class ThrowingCollection<E> extends AbstractCollection<E> {

        private final List<E> data;

        ThrowingCollection(Collection<E> src) {
            this.data = new ArrayList<>(src);
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
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    if (index == 1) {
                        // after first element, fail
                        throw new RuntimeException("Iterator forced failure");
                    }
                    return data.get(index++);
                }
            };
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    /**
     * Collection whose iterator() method throws IllegalStateException immediately.
     */
    private static class ImmediateThrowingCollection<E> extends AbstractCollection<E> {

        @Override
        public Iterator<E> iterator() {
            throw new IllegalStateException("Iterator creation failed");
        }

        @Override
        public int size() {
            return 0;
        }
    }

    /**
     * Collection whose iterator throws ConcurrentModificationException on first next().
     */
    private static class ConcurrentModifiableCollection<E> extends AbstractCollection<E> {

        private final List<E> data;

        ConcurrentModifiableCollection(Collection<E> src) {
            this.data = new ArrayList<>(src);
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private boolean first = true;

                @Override
                public boolean hasNext() {
                    return !data.isEmpty();
                }

                @Override
                public E next() {
                    if (first) {
                        first = false;
                        throw new ConcurrentModificationException("Concurrent modification simulated");
                    }
                    return data.get(0);
                }
            };
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    /**
     * Collection whose iterator() returns null.
     */
    private static class NullIteratorCollection<E> extends AbstractCollection<E> {

        private final List<E> data;

        NullIteratorCollection(Collection<E> src) {
            this.data = new ArrayList<>(src);
        }

        @Override
        public Iterator<E> iterator() {
            // deliberately broken
            return null;
        }

        @Override
        public int size() {
            return data.size();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        internalMap = new HashMap<>();
        // Populate the internal map with some data – the content is not relevant for the
        // informAdditionalUsage tests but ensures the TrackingMap is fully functional.
        internalMap.put("a", 1);
        internalMap.put("b", 2);
        internalMap.put("c", 3);
        trackingMap = new TrackingMap<>(internalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required.
        trackingMap = null;
        internalMap = null;
    }

    private static Stream<Arguments> normalBehaviourProvider() {
        return Stream.of(// empty collection – should leave the set empty
        Arguments.of(Collections.emptyList(), Collections.emptySet()), // single element
        Arguments.of(Arrays.asList(1), Collections.singleton(1)), // multiple distinct elements
        Arguments.of(Arrays.asList(1, 2, 3), new HashSet<>(Arrays.asList(1, 2, 3))), // duplicate elements – Set removes duplicates
        Arguments.of(Arrays.asList(5, 5, 5), Collections.singleton(5)), // collection containing null (HashSet permits null)
        Arguments.of(Arrays.asList(null, 7), new HashSet<>(Arrays.asList(null, 7))));
    }

    @Test
    void informAdditionalUsage_AddsAllKeysToReadSet() {
        // given
        Collection<String> additionalKeys = Arrays.asList("x", "y", "z");
        // when
        trackingMap.informAdditionalUsage(additionalKeys);
        // then
        Set<String> usedKeys = trackingMap.keysUsed();
        assertEquals(3, usedKeys.size(), "keysUsed should contain exactly the added keys");
        assertTrue(usedKeys.containsAll(additionalKeys), "keysUsed should contain all keys passed to informAdditionalUsage");
    }

    @Test
    void informAdditionalUsage_WithEmptyCollectionLeavesReadSetUnchanged() {
        // given
        Collection<String> empty = Collections.emptyList();
        // when
        trackingMap.informAdditionalUsage(empty);
        // then
        Set<String> usedKeys = trackingMap.keysUsed();
        assertTrue(usedKeys.isEmpty(), "keysUsed should remain empty when an empty collection is supplied");
    }

    @Test
    void informAdditionalUsage_AddsOnlyUniqueKeys() {
        // given
        Collection<String> firstBatch = Arrays.asList("p", "q");
        Collection<String> secondBatch = Arrays.asList("q", "r");
        // when
        trackingMap.informAdditionalUsage(firstBatch);
        trackingMap.informAdditionalUsage(secondBatch);
        // then
        Set<String> usedKeys = trackingMap.keysUsed();
        assertEquals(3, usedKeys.size(), "keysUsed should contain unique keys only");
        assertTrue(usedKeys.containsAll(Arrays.asList("p", "q", "r")), "keysUsed should contain all distinct keys added");
    }

    @Test
    void informAdditionalUsage_NullCollectionThrowsNullPointerException() {
        // given
        Collection<String> nullCollection = null;
        // when / then
        assertThrows(NullPointerException.class, () -> trackingMap.informAdditionalUsage(nullCollection), "Calling informAdditionalUsage with null should throw NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Normal behaviour – parameterised tests
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: additional={0} => expected={1}")
    @MethodSource("normalBehaviourProvider")
    void informAdditionalUsage_NormalBehaviour(Collection<Integer> additional, Set<Integer> expected) {
        // when
        trackingMap.informAdditionalUsage(additional);
        // then
        Set<Integer> used = trackingMap.keysUsed();
        assertEquals(expected, used, () -> "keysUsed() should equal the expected set after adding " + additional);
    }

    /* --------------------------------------------------------------
     *  Unmodifiable collection – should be readable
     * -------------------------------------------------------------- */
    @Test
    void informAdditionalUsage_UnmodifiableCollection() {
        Collection<Integer> unmodifiable = Collections.unmodifiableCollection(Arrays.asList(10, 20));
        trackingMap.informAdditionalUsage(unmodifiable);
        Set<Integer> used = trackingMap.keysUsed();
        assertEquals(new HashSet<>(Arrays.asList(10, 20)), used, "keysUsed() should contain the elements from an unmodifiable collection");
    }

    /* --------------------------------------------------------------
     *  Collection whose iterator throws after the first element
     * -------------------------------------------------------------- */
    @Test
    void informAdditionalUsage_ThrowingCollectionPropagatesRuntimeException() {
        Collection<Integer> throwing = new ThrowingCollection<>(Arrays.asList(1, 2));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> trackingMap.informAdditionalUsage(throwing), "RuntimeException from the iterator should be propagated");
        assertEquals("Iterator forced failure", ex.getMessage());
        // readKeys must remain unchanged
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed() should stay empty after exception");
    }

    /* --------------------------------------------------------------
     *  Accumulation across multiple calls with overlapping collections
     * -------------------------------------------------------------- */
    @Test
    void informAdditionalUsage_AccumulationAcrossCalls() {
        trackingMap.informAdditionalUsage(Collections.singleton(100));
        trackingMap.informAdditionalUsage(Arrays.asList(200, 300));
        trackingMap.informAdditionalUsage(Arrays.asList(100, 400));
        Set<Integer> expected = new HashSet<>(Arrays.asList(100, 200, 300, 400));
        assertEquals(expected, trackingMap.keysUsed(), "keysUsed() should contain the union of all distinct keys added across calls");
    }

    /* --------------------------------------------------------------
     *  Self‑reference collection (the map's own keysUsed set)
     * -------------------------------------------------------------- */
    @Test
    void informAdditionalUsage_SelfReferenceCollectionNoChange() {
        // keysUsed is initially empty
        Set<Integer> self = trackingMap.keysUsed();
        // Adding the set to itself should be a no‑op (HashSet.addAll handles self‑addition)
        trackingMap.informAdditionalUsage(self);
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed() should remain unchanged when its own set is passed");
    }

    /* --------------------------------------------------------------
     *  Collection whose iterator() method throws immediately
     * -------------------------------------------------------------- */
    @Test
    void informAdditionalUsage_ImmediateThrowingCollectionPropagatesIllegalStateException() {
        Collection<Integer> immediate = new ImmediateThrowingCollection<>();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> trackingMap.informAdditionalUsage(immediate), "IllegalStateException from iterator() should be propagated");
        assertEquals("Iterator creation failed", ex.getMessage());
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed() should stay empty after exception");
    }

    /* --------------------------------------------------------------
     *  Collection whose iterator throws ConcurrentModificationException
     * -------------------------------------------------------------- */
    @Test
    void informAdditionalUsage_ConcurrentModificationCollectionPropagatesException() {
        Collection<Integer> concurrent = new ConcurrentModifiableCollection<>(Arrays.asList(1, 2, 3));
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, () -> trackingMap.informAdditionalUsage(concurrent), "ConcurrentModificationException should be propagated");
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed() should stay empty after exception");
    }

    /* --------------------------------------------------------------
     *  Collection whose iterator() returns null
     * -------------------------------------------------------------- */
    @Test
    void informAdditionalUsage_NullIteratorCollectionThrowsNullPointerException() {
        Collection<Integer> nullIter = new NullIteratorCollection<>(Arrays.asList(4, 5));
        assertThrows(NullPointerException.class, () -> trackingMap.informAdditionalUsage(nullIter), "NullPointerException should be thrown when iterator() returns null");
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed() should stay empty after exception");
    }
}
