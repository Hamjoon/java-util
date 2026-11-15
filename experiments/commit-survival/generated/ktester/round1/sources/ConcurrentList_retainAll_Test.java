package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_retainAll_Test {

    private ConcurrentList<Integer> list;

    private ConcurrentList<String> stringList;

    private ConcurrentList<Integer> intList;

    /**
     * Helper utilities for tests.
     */
    class TestUtils {

        /**
         * A collection that throws a specified exception when its iterator is accessed.
         */
        static class ExceptionThrowingCollection<E> implements Collection<E> {

            private final Collection<E> delegate;

            private final Class<? extends RuntimeException> exceptionClass;

            ExceptionThrowingCollection(Collection<E> delegate, Class<? extends RuntimeException> exceptionClass) {
                this.delegate = delegate;
                this.exceptionClass = exceptionClass;
            }

            @Override
            public Iterator<E> iterator() {
                return new Iterator<E>() {

                    private final Iterator<E> it = delegate.iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public E next() {
                        try {
                            return it.next();
                        } catch (RuntimeException e) {
                            // ignore and throw the configured one instead
                            try {
                                throw exceptionClass.getDeclaredConstructor().newInstance();
                            } catch (Exception ex) {
                                throw new RuntimeException("Failed to instantiate exception", ex);
                            }
                        }
                    }

                    @Override
                    public void remove() {
                        it.remove();
                    }
                };
            }

            // The rest of the Collection contract is delegated (not used in tests)
            @Override
            public int size() {
                return delegate.size();
            }

            @Override
            public boolean isEmpty() {
                return delegate.isEmpty();
            }

            @Override
            public boolean contains(Object o) {
                return delegate.contains(o);
            }

            @Override
            public Object[] toArray() {
                return delegate.toArray();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return delegate.toArray(a);
            }

            @Override
            public boolean add(E e) {
                return delegate.add(e);
            }

            @Override
            public boolean remove(Object o) {
                return delegate.remove(o);
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return delegate.containsAll(c);
            }

            @Override
            public boolean addAll(Collection<? extends E> c) {
                return delegate.addAll(c);
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return delegate.removeAll(c);
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return delegate.retainAll(c);
            }

            @Override
            public void clear() {
                delegate.clear();
            }
        }
    }

    /**
     * A collection that throws a specified exception when its iterator is accessed.
     */
    static class ExceptionThrowingCollection<E> implements Collection<E> {

        private final Collection<E> delegate;

        private final Class<? extends RuntimeException> exceptionClass;

        ExceptionThrowingCollection(Collection<E> delegate, Class<? extends RuntimeException> exceptionClass) {
            this.delegate = delegate;
            this.exceptionClass = exceptionClass;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private final Iterator<E> it = delegate.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    try {
                        return it.next();
                    } catch (RuntimeException e) {
                        // ignore and throw the configured one instead
                        try {
                            throw exceptionClass.getDeclaredConstructor().newInstance();
                        } catch (Exception ex) {
                            throw new RuntimeException("Failed to instantiate exception", ex);
                        }
                    }
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        // The rest of the Collection contract is delegated (not used in tests)
        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return delegate.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return delegate.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return delegate.retainAll(c);
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }

    /**
     * A collection that throws a specified exception when its iterator is accessed.
     */
    static class ExceptionThrowingCollection<E> implements Collection<E> {

        private final Collection<E> delegate;

        private final Class<? extends RuntimeException> exceptionClass;

        ExceptionThrowingCollection(Collection<E> delegate, Class<? extends RuntimeException> exceptionClass) {
            this.delegate = delegate;
            this.exceptionClass = exceptionClass;
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private final Iterator<E> it = delegate.iterator();

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public E next() {
                    try {
                        return it.next();
                    } catch (RuntimeException e) {
                        // ignore and throw the configured one instead
                        try {
                            throw exceptionClass.getDeclaredConstructor().newInstance();
                        } catch (Exception ex) {
                            throw new RuntimeException("Failed to instantiate exception", ex);
                        }
                    }
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        // The rest of the Collection contract is delegated (not used in tests)
        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public Object[] toArray() {
            return delegate.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return delegate.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return delegate.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return delegate.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return delegate.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return delegate.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return delegate.retainAll(c);
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh lists for each test.
        stringList = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c", null, null)));
        intList = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 2, 3, 4)));
    }

    @AfterEach
    void teardownAfterEach() {
        // clear reference to help GC; not strictly required
        list = null;
    }

    /* --------------------------------------------------------------
     *  Parameterized test covering a subset of JSON cases for brevity
     * -------------------------------------------------------------- */
    private static Stream<Arguments> basicCases() {
        return Stream.of(Arguments.of(Arrays.asList("a", "b"), true, Arrays.asList("a", "b")), Arguments.of(Collections.emptyList(), true, Collections.emptyList()), Arguments.of(Collections.singletonList("nonexistent"), true, Collections.emptyList()), Arguments.of(Arrays.asList("a", "b", "c", "d"), false, Arrays.asList("a", "b", "c", "d")));
    }

    @Test
    void retainAllRemovesElementsAndReturnsTrue() {
        // given
        Collection<Integer> toRetain = Arrays.asList(2, 4, 5);
        // when
        boolean changed = list.retainAll(toRetain);
        // then
        assertTrue(changed, "retainAll should return true when the list is modified");
        assertEquals(Arrays.asList(2, 4), list, "List should contain only the retained elements");
    }

    @Test
    void retainAllNoChangeReturnsFalse() {
        // given
        Collection<Integer> toRetain = Arrays.asList(1, 2, 3, 4, 5);
        // when
        boolean changed = list.retainAll(toRetain);
        // then
        assertFalse(changed, "retainAll should return false when the list is not modified");
        assertEquals(Arrays.asList(1, 2, 3, 4), list, "List should remain unchanged");
    }

    @Test
    void retainAllWithEmptyCollectionEmptiesList() {
        // given
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
        Collection<Integer> nullCollection = null;
        // when / then
        assertThrows(NullPointerException.class, () -> list.retainAll(nullCollection), "retainAll should throw NullPointerException for a null argument");
    }

    @Test
    void retainAllDelegatesToUnderlyingList() {
        // given: a mock list that we wrap with ConcurrentList
        @SuppressWarnings("unchecked")
        List<Integer> mockList = mock(List.class);
        Collection<Integer> toRetain = Arrays.asList(1, 2);
        when(mockList.retainAll(toRetain)).thenReturn(true);
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(mockList);
        // when
        boolean result = wrapper.retainAll(toRetain);
        // then
        assertTrue(result, "Result should be whatever the underlying list returns");
        verify(mockList, times(1)).retainAll(toRetain);
    }

    /* --------------------------------------------------------------
     *  Basic functionality tests (subset, empty, non‑existent)
     * -------------------------------------------------------------- */
    @Test
    void retainAllSubsetReturnsTrueAndKeepsOrder() {
        // given
        Collection<String> toRetain = Arrays.asList("a", "b");
        // when
        boolean changed = stringList.retainAll(toRetain);
        // then
        assertTrue(changed, "retainAll should return true when elements are removed");
        assertEquals(Arrays.asList("a", "b"), stringList, "List must contain only the retained elements in original order");
    }

    @Test
    void retainAllEmptyCollectionEmptiesList() {
        // given
        Collection<String> empty = Collections.emptyList();
        // when
        boolean changed = stringList.retainAll(empty);
        // then
        assertTrue(changed, "retainAll should return true when list becomes empty");
        assertTrue(stringList.isEmpty(), "List should be empty after retaining an empty collection");
    }

    @Test
    void retainAllNonExistentElementsEmptiesList() {
        // given
        Collection<String> toRetain = Collections.singletonList("nonexistent");
        // when
        boolean changed = stringList.retainAll(toRetain);
        // then
        assertTrue(changed, "retainAll should return true when all elements are removed");
        assertTrue(stringList.isEmpty(), "List should be empty because none of the original elements were retained");
    }

    /* --------------------------------------------------------------
     *  Unchanged list scenarios
     * -------------------------------------------------------------- */
    @Test
    void retainAllExactMatchReturnsFalse() {
        // given
        Collection<String> same = Arrays.asList("a", "b", "c", null, null);
        // when
        boolean changed = stringList.retainAll(same);
        // then
        assertFalse(changed, "retainAll should return false when no modification occurs");
        assertEquals(Arrays.asList("a", "b", "c", null, null), stringList, "List must remain unchanged");
    }

    @Test
    void retainAllSupersetReturnsFalse() {
        // given
        Collection<String> superset = Arrays.asList("a", "b", "c", null, null, "extra");
        // when
        boolean changed = stringList.retainAll(superset);
        // then
        assertFalse(changed, "retainAll should return false when collection is a superset");
        assertEquals(Arrays.asList("a", "b", "c", null, null), stringList, "List must remain unchanged");
    }

    /* --------------------------------------------------------------
     *  Empty list scenarios
     * -------------------------------------------------------------- */
    @Test
    void retainAllOnEmptyListWithNonEmptyCollectionReturnsFalse() {
        // given
        ConcurrentList<String> emptyList = new ConcurrentList<>(new ArrayList<>());
        Collection<String> coll = Collections.singleton("any");
        // when
        boolean changed = emptyList.retainAll(coll);
        // then
        assertFalse(changed, "retainAll on an already empty list should return false");
        assertTrue(emptyList.isEmpty(), "List should stay empty");
    }

    @Test
    void retainAllOnEmptyListWithEmptyCollectionReturnsFalse() {
        // given
        ConcurrentList<String> emptyList = new ConcurrentList<>(new ArrayList<>());
        Collection<String> empty = Collections.emptyList();
        // when
        boolean changed = emptyList.retainAll(empty);
        // then
        assertFalse(changed, "retainAll on empty list with empty collection should return false");
        assertTrue(emptyList.isEmpty(), "List should stay empty");
    }

    /* --------------------------------------------------------------
     *  Null element handling
     * -------------------------------------------------------------- */
    @Test
    void retainAllKeepsOnlyNulls() {
        // given
        Collection<String> onlyNull = Collections.singletonList(null);
        // when
        boolean changed = stringList.retainAll(onlyNull);
        // then
        assertTrue(changed, "retainAll should return true when non‑null elements are removed");
        assertEquals(Arrays.asList(null, null), stringList, "List must contain only the null elements preserving order");
    }

    @Test
    void retainAllWithNoNullsInCollectionRemovesAllNulls() {
        // given
        Collection<String> noNull = Collections.singletonList("no-null");
        // when
        boolean changed = stringList.retainAll(noNull);
        // then
        assertTrue(changed, "retainAll should return true when null elements are removed");
        assertTrue(stringList.isEmpty(), "List should become empty because none of the original elements match");
    }

    /* --------------------------------------------------------------
     *  Collection type variations
     * -------------------------------------------------------------- */
    @Test
    void retainAllWithHashSetKeepsOrder() {
        // given
        Collection<String> set = new HashSet<>(Arrays.asList("a", "c"));
        // when
        boolean changed = stringList.retainAll(set);
        // then
        assertTrue(changed, "retainAll should return true when elements are removed");
        assertEquals(Arrays.asList("a", "c"), stringList, "Order of retained elements must follow original list order");
    }

    @Test
    void retainAllWithSelfReferenceDoesNotModify() {
        // given
        // same instance
        Collection<String> self = stringList;
        // when
        boolean changed = stringList.retainAll(self);
        // then
        assertFalse(changed, "retainAll should detect no change when passing the same list instance");
        assertEquals(Arrays.asList("a", "b", "c", null, null), stringList, "List must remain unchanged");
    }

    @Test
    void retainAllWithDuplicateElementsInCollection() {
        // given
        Collection<String> dup = Arrays.asList("a", "a", "b");
        // when
        boolean changed = stringList.retainAll(dup);
        // then
        assertTrue(changed, "retainAll should return true when elements are removed");
        assertEquals(Arrays.asList("a", "b"), stringList, "Duplicates in the collection must be treated as a set of allowed elements");
    }

    /* --------------------------------------------------------------
     *  Exception propagation from collection
     * -------------------------------------------------------------- */
    @Test
    void retainAllPropagatesExceptionFromCollection_NullPointerException() {
        // given
        Collection<String> throwing = new TestUtils.ExceptionThrowingCollection<>(Arrays.asList("a", "b"), NullPointerException.class);
        // when / then
        assertThrows(NullPointerException.class, () -> stringList.retainAll(throwing), "Exception from the collection should be propagated");
    }

    @Test
    void retainAllPropagatesExceptionFromCollection_IllegalStateException() {
        // given
        Collection<String> throwing = new TestUtils.ExceptionThrowingCollection<>(Arrays.asList("a", "b"), IllegalStateException.class);
        // when / then
        assertThrows(IllegalStateException.class, () -> stringList.retainAll(throwing), "Exception from the collection should be propagated");
    }

    /* --------------------------------------------------------------
     *  Unsupported operation / null internal list handling
     * -------------------------------------------------------------- */
    @Test
    void retainAllOnUnmodifiableUnderlyingListThrowsUnsupportedOperationException() {
        // given
        List<String> unmodifiable = Collections.unmodifiableList(new ArrayList<>(Arrays.asList("x", "y")));
        ConcurrentList<String> wrapper = new ConcurrentList<>(unmodifiable);
        Collection<String> toRetain = Collections.singletonList("x");
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> wrapper.retainAll(toRetain), "retainAll must forward the UnsupportedOperationException from the underlying list");
    }

    @Test
    void retainAllWithNullInternalListThrowsNPE() {
        // given
        ConcurrentList<String> broken = new ConcurrentList<>(null);
        Collection<String> any = Collections.singletonList("any");
        // when / then
        assertThrows(NullPointerException.class, () -> broken.retainAll(any), "If the internal list is null, any operation should result in NPE");
    }

    @ParameterizedTest(name = "retainAll {0} => changed={1}, result={2}")
    @MethodSource("basicCases")
    void retainAllParameterized(Collection<String> retain, boolean expectedChanged, List<String> expectedList) {
        // given a fresh list for each invocation
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c", "d")));
        // when
        boolean changed = list.retainAll(retain);
        // then
        assertEquals(expectedChanged, changed, "Unexpected change flag");
        assertEquals(expectedList, list, "List content after retainAll does not match expectation");
    }
}
