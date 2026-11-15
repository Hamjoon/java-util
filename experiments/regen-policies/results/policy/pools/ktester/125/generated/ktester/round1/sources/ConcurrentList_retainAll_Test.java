package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_retainAll_Test {

    private ConcurrentList<Integer> list;

    // used for wrapper‑mode tests
    private List<Integer> backingList;

    class ModifyingCollection extends AbstractCollection<String> {

        private final List<String> internal = new ArrayList<>(Arrays.asList("a", "b"));

        @Override
        public Iterator<String> iterator() {
            return new Iterator<>() {

                private final Iterator<String> it = internal.iterator();

                private boolean first = true;

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public String next() {
                    if (first) {
                        // Structural modification while iterating
                        internal.add("c");
                        first = false;
                    }
                    return it.next();
                }
            };
        }

        @Override
        public int size() {
            return internal.size();
        }
    }

    class ModifyingCollection extends AbstractCollection<String> {

        private final List<String> internal = new ArrayList<>(Arrays.asList("a", "b"));

        @Override
        public Iterator<String> iterator() {
            return new Iterator<>() {

                private final Iterator<String> it = internal.iterator();

                private boolean first = true;

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public String next() {
                    if (first) {
                        // Structural modification while iterating
                        internal.add("c");
                        first = false;
                    }
                    return it.next();
                }
            };
        }

        @Override
        public int size() {
            return internal.size();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default tests use a fresh standalone ConcurrentList
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        list = null;
        backingList = null;
    }

    private static Stream<Arguments> functionalBehaviorProvider() {
        return Stream.of(// null collection → NPE
        Arguments.of(Arrays.asList("a", "b", "c"), null, false, null, "Null collection should cause NullPointerException"), // empty collection on non‑empty list → true, list becomes empty
        Arguments.of(Arrays.asList("a", "b", "c"), Collections.emptyList(), true, Collections.emptyList(), "Empty collection removes all elements"), // identical collection → false, unchanged
        Arguments.of(Arrays.asList("a", "b", "c"), Arrays.asList("a", "b", "c"), false, Arrays.asList("a", "b", "c"), "Exact match results in no modification"), // proper subset → true, non‑matching removed
        Arguments.of(Arrays.asList("a", "b", "c"), Arrays.asList("a", "c"), true, Arrays.asList("a", "c"), "Subset retains matching elements only"), // disjoint collection → true, list cleared
        Arguments.of(Arrays.asList("a", "b", "c"), Arrays.asList("x", "y"), true, Collections.emptyList(), "No common elements clears the list"), // superset collection → false, unchanged
        Arguments.of(Arrays.asList("a", "b", "c"), Arrays.asList("a", "b", "c", "d"), false, Arrays.asList("a", "b", "c"), "Superset does not modify the list"), // empty list, non‑empty collection → false, still empty
        Arguments.of(Collections.emptyList(), Arrays.asList("a"), false, Collections.emptyList(), "Empty list stays empty regardless of collection"), // both empty → false, unchanged
        Arguments.of(Collections.emptyList(), Collections.emptyList(), false, Collections.emptyList(), "Both empty results in no change"));
    }

    private static Stream<Arguments> duplicatesAndNullsProvider() {
        return Stream.of(// duplicate elements, retain "a" only
        Arguments.of(Arrays.asList("a", "a", "b"), Arrays.asList("a"), true, Arrays.asList("a", "a"), "Duplicates are preserved when retained"), // superset with duplicate in collection, no change
        Arguments.of(Arrays.asList("a", "b"), Arrays.asList("a", "b", "b"), false, Arrays.asList("a", "b"), "Duplicate in collection does not affect result"), // retain only null
        Arguments.of(Arrays.asList("a", null, "b"), Arrays.asList((String) null), true, Collections.singletonList(null), "Retaining null discards other elements"), // empty collection on list containing null
        Arguments.of(Arrays.asList("a", null, "b"), Collections.emptyList(), true, Collections.emptyList(), "Empty collection clears list even with nulls present"), // empty list, retain collection containing null → false
        Arguments.of(Collections.emptyList(), Arrays.asList((String) null), false, Collections.emptyList(), "Empty list stays empty when retaining null collection"), // retain using the same underlying list instance
        Arguments.of(Arrays.asList("a", "b", "c"), null, false, null, "Self‑reference collection (c == list) should result in no change"));
    }

    @Test
    void retainAllRemovesElementsNotInCollection() {
        // given
        list.addAll(Arrays.asList(1, 2, 3, 4));
        // 5 is not present
        Collection<Integer> toRetain = Arrays.asList(2, 4, 5);
        // when
        boolean changed = list.retainAll(toRetain);
        // then
        assertTrue(changed, "retainAll should return true when the list is modified");
        assertEquals(Arrays.asList(2, 4), list, "List should contain only the retained elements");
    }

    @Test
    void retainAllNoChangeWhenAllElementsAlreadyPresent() {
        // given
        list.addAll(Arrays.asList(1, 2));
        // 3 is extra, but does not affect existing elements
        Collection<Integer> toRetain = Arrays.asList(1, 2, 3);
        // when
        boolean changed = list.retainAll(toRetain);
        // then
        assertFalse(changed, "retainAll should return false when the list is not modified");
        assertEquals(Arrays.asList(1, 2), list, "List should remain unchanged");
    }

    @Test
    void retainAllWithEmptyCollectionEmptiesList() {
        // given
        list.addAll(Arrays.asList(10, 20, 30));
        Collection<Integer> empty = Collections.emptyList();
        // when
        boolean changed = list.retainAll(empty);
        // then
        assertTrue(changed, "retainAll should return true when the list becomes empty");
        assertTrue(list.isEmpty(), "List should be empty after retaining an empty collection");
    }

    @Test
    void retainAllNullCollectionThrowsNPE() {
        // given
        list.addAll(Arrays.asList(1, 2, 3));
        // when / then
        assertThrows(NullPointerException.class, () -> list.retainAll(null), "retainAll should throw NullPointerException when the argument is null");
    }

    @Test
    void retainAllWorksInWrapperModeAndAffectsBackingList() {
        // given – create a backing list and wrap it
        backingList = new ArrayList<>(Arrays.asList(5, 6, 7, 8));
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backingList);
        Collection<Integer> toRetain = Arrays.asList(6, 8, 9);
        // when
        boolean changed = wrapped.retainAll(toRetain);
        // then
        assertTrue(changed, "retainAll should report a change");
        assertEquals(Arrays.asList(6, 8), wrapped, "Wrapped list should contain only retained elements");
        assertEquals(Arrays.asList(6, 8), backingList, "Backing list must reflect the same changes");
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("functionalBehaviorProvider")
    void retainAllFunctionalBehavior(List<String> initial, Collection<String> toRetain, boolean expectedChanged, List<String> expectedResult, String description) {
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.addAll(initial);
        boolean changed = cl.retainAll(toRetain);
        assertEquals(expectedChanged, changed, "Unexpected change flag");
        assertEquals(expectedResult, cl, "List content mismatch");
    }

    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("duplicatesAndNullsProvider")
    void retainAllDuplicatesAndNulls(List<String> initial, Collection<String> toRetain, boolean expectedChanged, List<String> expectedResult, String description) {
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.addAll(initial);
        boolean changed = cl.retainAll(toRetain);
        assertEquals(expectedChanged, changed, "Unexpected change flag");
        assertEquals(expectedResult, cl, "List content mismatch");
    }

    @Test
    void retainAllSelfReferenceCollection() {
        // The collection argument is the same instance as the underlying list.
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.addAll(Arrays.asList("a", "b", "c"));
        // Use the underlying list via reflection (the private field 'list')
        List<String> underlying = getUnderlyingList(cl);
        boolean changed = cl.retainAll(underlying);
        assertFalse(changed, "Self‑reference should not modify the list");
        assertEquals(Arrays.asList("a", "b", "c"), cl, "List must remain unchanged");
    }

    @Test
    void retainAllSubListView() {
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.addAll(Arrays.asList("a", "b", "c"));
        // ["a","b"]
        Collection<String> sub = cl.subList(0, 2);
        boolean changed = cl.retainAll(sub);
        assertTrue(changed, "retainAll should remove element not present in subList");
        assertEquals(Arrays.asList("a", "b"), cl, "Result should match the subList view");
    }

    @Test
    void retainAllWithUnmodifiableBackingListThrowsUnsupportedOperationException() {
        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a")));
        ConcurrentList<String> wrapped = new ConcurrentList<>(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> wrapped.retainAll(Arrays.asList("a")), "retainAll must propagate UnsupportedOperationException from the delegate");
    }

    @Test
    void retainAllCollectionIteratorThrowsRuntimeException() {
        Collection<String> badCollection = new Collection<>() {

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
            public Iterator<String> iterator() {
                return new Iterator<>() {

                    @Override
                    public boolean hasNext() {
                        throw new RuntimeException("iterator failure");
                    }

                    @Override
                    public String next() {
                        return null;
                    }
                };
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
            public boolean add(String s) {
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
            public boolean addAll(Collection<? extends String> c) {
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
        };
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.addAll(Arrays.asList("x"));
        assertThrows(RuntimeException.class, () -> cl.retainAll(badCollection), "Exception from collection iterator must propagate");
    }

    @Test
    void retainAllCollectionModifiesDuringIterationThrowsConcurrentModificationException() {
        // A collection whose iterator adds an element to the collection on the first call to next()
        class ModifyingCollection extends AbstractCollection<String> {

            private final List<String> internal = new ArrayList<>(Arrays.asList("a", "b"));

            @Override
            public Iterator<String> iterator() {
                return new Iterator<>() {

                    private final Iterator<String> it = internal.iterator();

                    private boolean first = true;

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public String next() {
                        if (first) {
                            // Structural modification while iterating
                            internal.add("c");
                            first = false;
                        }
                        return it.next();
                    }
                };
            }

            @Override
            public int size() {
                return internal.size();
            }
        }
        ConcurrentList<String> cl = new ConcurrentList<>();
        cl.addAll(Arrays.asList("a", "b", "c"));
        ModifyingCollection coll = new ModifyingCollection();
        assertThrows(ConcurrentModificationException.class, () -> cl.retainAll(coll), "Structural change during iteration should result in CME");
    }

    /**
     * Retrieves the private {@code list} field from a {@link ConcurrentList}
     * instance using reflection. This is required for the self‑reference test.
     */
    @SuppressWarnings("unchecked")
    private static <E> List<E> getUnderlyingList(ConcurrentList<E> cl) {
        try {
            var field = ConcurrentList.class.getDeclaredField("list");
            field.setAccessible(true);
            return (List<E>) field.get(cl);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to access underlying list", e);
        }
    }
}
