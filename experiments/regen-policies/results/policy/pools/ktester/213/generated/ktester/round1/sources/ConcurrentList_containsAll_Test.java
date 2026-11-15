package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.*;
import static org.mockito.Mockito.*;

class ConcurrentList_containsAll_Test {

    private ConcurrentList<Integer> list;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for each test (stand‑alone mode)
        list = new ConcurrentList<>();
    }

    private static Stream<Arguments> basicPositiveProvider() {
        return Stream.of(Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(2, 3), true, "ConcurrentList initialized with [1,2,3] should contain all elements of [2,3]"), Arguments.of(Arrays.asList(1, 2, 3), Collections.emptyList(), true, "Any list (including non‑empty) must return true when the argument collection is empty"), Arguments.of(Arrays.asList(1, null, 3), Arrays.asList((Object) null), true, "List containing a null element should report true when the argument collection contains only null"), Arguments.of(Arrays.asList(1, 2, 2, 3), Arrays.asList(2, 2), true, "Duplicates in the argument collection are ignored; list [1,2,2,3] should still return true"));
    }

    private static Stream<Arguments> basicNegativeProvider() {
        return Stream.of(Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(4), false, "List [1,2,3] does not contain element 4, therefore containsAll should return false."), Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(2, 4), false, "Even though 2 is present, 4 is missing, so the result must be false."), Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList("string"), false, "Different element type (String) that is not present in an Integer list results in false."));
    }

    private static Stream<Arguments> emptyListProvider() {
        return Stream.of(Arguments.of(Collections.emptyList(), Collections.emptyList(), true, "An empty list contains all elements of an empty collection (vacuously true)."), Arguments.of(Collections.emptyList(), Arrays.asList(1), false, "Empty list cannot contain any non‑null element; should return false."));
    }

    private static Stream<Arguments> nullElementEdgeProvider() {
        return Stream.of(Arguments.of(Arrays.asList(null, 1, 2), Arrays.asList(null, 2), true, "List initialized with [null,1,2] should contain all elements of [null,2]"), Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList((Object) null), false, "List without a null element must return false when the argument collection contains only null."), Arguments.of(Arrays.asList(null, 1, 2), Arrays.asList(null, null), true, "Duplicates of null in the argument collection are ignored; a list that contains a single null should still return true."));
    }

    private static Stream<Arguments> collectionTypeVariationsProvider() {
        return Stream.of(Arguments.of(Arrays.asList(1, 2, 3), new HashSet<>(Arrays.asList(1, 2)), true, "Using a HashSet as the argument collection with elements present in the list should return true."), Arguments.of(Arrays.asList(1, 2, 3), new LinkedList<>(Arrays.asList(2, 3)), true, "A LinkedList (produced by Arrays.asList) containing elements that exist in the list must return true."), Arguments.of(Arrays.asList(1, 2, 3), Collections.unmodifiableList(Arrays.asList(1, 4)), false, "An unmodifiable collection that contains a missing element (4) must cause containsAll to return false."));
    }

    private static Stream<Arguments> largeCollectionProvider() {
        List<Integer> range1to1000 = IntStream.rangeClosed(1, 1000).boxed().collect(Collectors.toList());
        List<Integer> range1to1001 = IntStream.rangeClosed(1, 1001).boxed().collect(Collectors.toList());
        return Stream.of(Arguments.of(range1to1000, range1to1000, true, "List initialized with integers 1..1000 should contain all elements of an identical range."), Arguments.of(range1to1000, range1to1001, false, "Same large list (1..1000) does NOT contain element 1001; containsAll must return false."));
    }

    @Test
    void containsAllReturnsTrueWhenAllElementsPresent() {
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));
        Collection<Integer> toCheck = Arrays.asList(1, 3, 5);
        assertTrue(list.containsAll(toCheck), "containsAll should return true when every element of the collection is present");
    }

    @Test
    void containsAllReturnsFalseWhenAnyElementMissing() {
        list.addAll(Arrays.asList(10, 20, 30));
        // 40 is not in the list
        Collection<Integer> toCheck = Arrays.asList(10, 40);
        assertFalse(list.containsAll(toCheck), "containsAll should return false when at least one element of the collection is missing");
    }

    @Test
    void containsAllWithEmptyCollectionIsTrue() {
        list.addAll(Arrays.asList(7, 8, 9));
        Collection<Integer> empty = Collections.emptyList();
        assertTrue(list.containsAll(empty), "containsAll should return true for an empty collection regardless of list contents");
    }

    @Test
    void containsAllHandlesNullElementsWhenBackingListAllowsNulls() {
        // Underlying ArrayList permits nulls, so the ConcurrentList should as well
        list.addAll(Arrays.asList(1, null, 3));
        Collection<Integer> withNull = Arrays.asList(null, 1);
        assertTrue(list.containsAll(withNull), "containsAll should correctly recognise null elements that are present in the list");
        Collection<Integer> missingNull = Arrays.asList(null, 2);
        assertFalse(list.containsAll(missingNull), "containsAll should return false when a null element is expected but not present");
    }

    @Test
    void containsAllWorksWithWrappedList() {
        // Prepare a backing list, then wrap it
        List<Integer> backing = new ArrayList<>(Arrays.asList(100, 200, 300));
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backing);
        // Verify normal behaviour
        assertTrue(wrapped.containsAll(Arrays.asList(100, 300)));
        assertFalse(wrapped.containsAll(Arrays.asList(400)));
        // Mutate the backing list directly to ensure the wrapper reflects changes
        backing.add(400);
        assertTrue(wrapped.containsAll(Arrays.asList(400)), "Wrapped ConcurrentList should reflect modifications made to the underlying list");
    }

    @Test
    void containsAllThrowsNPEWhenArgumentIsNull() {
        list.addAll(Arrays.asList(1, 2, 3));
        assertThrows(NullPointerException.class, () -> list.containsAll(null), "containsAll should throw NullPointerException when the supplied collection is null");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicPositiveProvider")
    void containsAllBasicPositive(Collection<Integer> init, Collection<?> arg, boolean expected, String description) {
        list.addAll(init);
        assertEquals(expected, list.containsAll(arg), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicNegativeProvider")
    void containsAllBasicNegative(Collection<Integer> init, Collection<?> arg, boolean expected, String description) {
        list.addAll(init);
        assertEquals(expected, list.containsAll(arg), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("emptyListProvider")
    void containsAllEmptyListScenarios(Collection<Integer> init, Collection<?> arg, boolean expected, String description) {
        // init is expected to be empty for these cases
        list.addAll(init);
        assertEquals(expected, list.containsAll(arg), description);
    }

    // -----------------------------------------------------------------
    @Test
    void containsAllNullArgumentThrowsNPE() {
        list.addAll(Arrays.asList(1, 2, 3));
        assertThrows(NullPointerException.class, () -> list.containsAll(null), "Passing null as the collection argument must throw NullPointerException, matching the List interface contract.");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nullElementEdgeProvider")
    void containsAllNullElementEdgeCases(Collection<Integer> init, Collection<?> arg, boolean expected, String description) {
        list.addAll(init);
        assertEquals(expected, list.containsAll(arg), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("collectionTypeVariationsProvider")
    void containsAllCollectionTypeVariations(Collection<Integer> init, Collection<?> arg, boolean expected, String description) {
        list.addAll(init);
        assertEquals(expected, list.containsAll(arg), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("largeCollectionProvider")
    void containsAllLargeCollectionScenarios(Collection<Integer> init, Collection<?> arg, boolean expected, String description) {
        list.addAll(init);
        assertEquals(expected, list.containsAll(arg), description);
    }

    // -----------------------------------------------------------------
    @Test
    void containsAllCollectionThatThrowsRuntimeException() {
        list.addAll(IntStream.rangeClosed(1, 5).boxed().collect(Collectors.toList()));
        Collection<?> badCollection = new AbstractCollection<Object>() {

            @Override
            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Object next() {
                        throw new RuntimeException("iteration failure");
                    }
                };
            }

            @Override
            public int size() {
                return 1;
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> list.containsAll(badCollection), "Exception from the argument collection should propagate.");
        assertEquals("iteration failure", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @Test
    void containsAllCollectionThrowsConcurrentModificationException() {
        list.addAll(Arrays.asList(1, 2, 3));
        Collection<?> cmExceptionCollection = new AbstractCollection<Object>() {

            @Override
            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    private int count = 0;

                    @Override
                    public boolean hasNext() {
                        return count < 1;
                    }

                    @Override
                    public Object next() {
                        count++;
                        throw new ConcurrentModificationException("iteration failure");
                    }
                };
            }

            @Override
            public int size() {
                return 1;
            }
        };
        assertThrows(ConcurrentModificationException.class, () -> list.containsAll(cmExceptionCollection), "ConcurrentModificationException should propagate from the argument collection.");
    }

    @Test
    void containsAllCollectionThrowsNoSuchElementException() {
        list.addAll(Arrays.asList(1, 2, 3));
        Collection<?> noSuchElementCollection = new AbstractCollection<Object>() {

            @Override
            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    @Override
                    public boolean hasNext() {
                        return false;
                    }

                    @Override
                    public Object next() {
                        throw new NoSuchElementException("no element");
                    }
                };
            }

            @Override
            public int size() {
                return 0;
            }
        };
        assertThrows(NoSuchElementException.class, () -> list.containsAll(noSuchElementCollection), "NoSuchElementException should propagate from the argument collection.");
    }

    @Test
    void containsAllCollectionThrowsUnsupportedOperationExceptionOnRemove() {
        list.addAll(Arrays.asList(1, 2, 3));
        // HashSet.containsAll never calls iterator.remove(), but we create a collection that does
        // to ensure any unexpected call would surface.
        Collection<?> unsupportedRemoveCollection = new AbstractCollection<Object>() {

            @Override
            public Iterator<Object> iterator() {
                return new Iterator<Object>() {

                    private boolean first = true;

                    @Override
                    public boolean hasNext() {
                        return first;
                    }

                    @Override
                    public Object next() {
                        first = false;
                        return 1;
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("remove not supported");
                    }
                };
            }

            @Override
            public int size() {
                return 1;
            }
        };
        // The expectation is that the exception propagates if remove() is invoked.
        // In practice HashSet.containsAll does not invoke remove(), so we simply assert that no exception is thrown.
        // This test documents the behaviour.
        assertDoesNotThrow(() -> list.containsAll(unsupportedRemoveCollection), "containsAll should not invoke iterator.remove(); no exception expected.");
    }

    // -----------------------------------------------------------------
    @Test
    void containsAllMockitoMockCollectionThrowsException() {
        list.addAll(Arrays.asList(1, 2, 3));
        @SuppressWarnings("unchecked")
        Collection<Integer> mockCollection = mock(Collection.class);
        when(mockCollection.iterator()).thenThrow(new RuntimeException("mocked iterator failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> list.containsAll(mockCollection), "Exception from mocked collection should propagate.");
        assertEquals("mocked iterator failure", ex.getMessage());
    }
}
