package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_containsAll_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    /**
     * A collection whose iterator throws {@link ConcurrentModificationException}
     * on the second call to {@code next()}.
     */
    private static class CMECollection<E> implements Collection<E> {

        private final List<E> backing;

        CMECollection(Collection<E> source) {
            this.backing = new ArrayList<>(source);
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < backing.size();
                }

                @Override
                public E next() {
                    if (index == 1) {
                        throw new ConcurrentModificationException("simulated CME");
                    }
                    return backing.get(index++);
                }
            };
        }

        // The remaining methods are not needed for the test; delegate to the backing list.
        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public boolean isEmpty() {
            return backing.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return backing.contains(o);
        }

        @Override
        public Object[] toArray() {
            return backing.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return backing.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return backing.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return backing.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return backing.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return backing.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return backing.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return backing.retainAll(c);
        }

        @Override
        public void clear() {
            backing.clear();
        }
    }

    /**
     * A collection whose iterator throws a generic {@link RuntimeException}
     * on the first call to {@code next()}.
     */
    private static class RuntimeExceptionCollection<E> implements Collection<E> {

        private final List<E> backing;

        RuntimeExceptionCollection(Collection<E> source) {
            this.backing = new ArrayList<>(source);
        }

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                private int index = 0;

                @Override
                public boolean hasNext() {
                    return index < backing.size();
                }

                @Override
                public E next() {
                    throw new RuntimeException("simulated runtime exception");
                }
            };
        }

        // Delegate remaining methods to the backing list (not used in the test)
        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public boolean isEmpty() {
            return backing.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return backing.contains(o);
        }

        @Override
        public Object[] toArray() {
            return backing.toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return backing.toArray(a);
        }

        @Override
        public boolean add(E e) {
            return backing.add(e);
        }

        @Override
        public boolean remove(Object o) {
            return backing.remove(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return backing.containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return backing.addAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return backing.removeAll(c);
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return backing.retainAll(c);
        }

        @Override
        public void clear() {
            backing.clear();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid any accidental cross‑test contamination
        intSet.clear();
        stringSet.clear();
    }

    @Test
    void containsAll_AllElementsPresent_ReturnsTrue() {
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        Collection<Integer> query = Arrays.asList(2, 4, 5);
        assertTrue(intSet.containsAll(query), "containsAll should return true when every element of the collection is present");
    }

    @Test
    void containsAll_MissingElement_ReturnsFalse() {
        intSet.addAll(Arrays.asList(10, 20, 30));
        // 99 is not in the set
        Collection<Integer> query = Arrays.asList(10, 99);
        assertFalse(intSet.containsAll(query), "containsAll should return false when at least one element of the collection is missing");
    }

    @Test
    void containsAll_EmptyCollection_ReturnsTrue() {
        intSet.addAll(Arrays.asList(7, 8, 9));
        Collection<Integer> empty = Collections.emptyList();
        assertTrue(intSet.containsAll(empty), "containsAll should return true for an empty collection (vacuous truth)");
    }

    @Test
    void containsAll_WithNullElements_ReturnsTrueWhenNullIsPresent() {
        // ConcurrentSet supports null via a sentinel value
        stringSet.addAll(Arrays.asList("a", null, "b"));
        Collection<String> query = Arrays.asList(null, "a");
        assertTrue(stringSet.containsAll(query), "containsAll should correctly handle null elements when they are present in the set");
    }

    @Test
    void containsAll_WithNullElements_ReturnsFalseWhenNullIsAbsent() {
        stringSet.addAll(Arrays.asList("x", "y"));
        Collection<String> query = Arrays.asList(null);
        assertFalse(stringSet.containsAll(query), "containsAll should return false when the collection contains null but the set does not");
    }

    @Test
    void containsAll_MixedTypes_ReturnsTrueForObjectSet() {
        // Use a raw ConcurrentSet to store heterogeneous objects
        ConcurrentSet<Object> mixedSet = new ConcurrentSet<>();
        mixedSet.addAll(Arrays.asList("string", 123, true));
        Collection<Object> query = Arrays.asList(123, true);
        assertTrue(mixedSet.containsAll(query), "containsAll should work with a Set of Object and a collection of mixed types");
    }

    @Test
    void containsAll_MixedTypes_ReturnsFalseWhenTypeMismatch() {
        ConcurrentSet<Object> mixedSet = new ConcurrentSet<>();
        mixedSet.addAll(Arrays.asList("string", 123, true));
        // The collection contains a value that is not present (different type)
        // Long vs Integer
        Collection<Object> query = Arrays.asList(123L);
        assertFalse(mixedSet.containsAll(query), "containsAll should return false when the collection contains an element of a different type not present in the set");
    }

    @Test
    void containsAll_AllElementsPresent_FromJson() {
        // Set contains {1,2,3}
        intSet.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> query = Arrays.asList(1, 2);
        assertTrue(intSet.containsAll(query), "All elements present – should return true");
    }

    @Test
    void containsAll_MissingElementTriggersEarlyReturn_FromJson() {
        // Set contains {1,2,3}
        intSet.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> query = Arrays.asList(1, 4);
        assertFalse(intSet.containsAll(query), "Missing element – should return false early");
    }

    @Test
    void containsAll_EmptyCollection_FromJson() {
        intSet.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> empty = Collections.emptyList();
        assertTrue(intSet.containsAll(empty), "Empty collection – vacuous true");
    }

    @Test
    void containsAll_CollectionWithNullPresent_FromJson() {
        stringSet.addAll(Arrays.asList("x", null, "y"));
        Collection<String> query = Collections.singletonList(null);
        assertTrue(stringSet.containsAll(query), "Null present in set – should return true");
    }

    @Test
    void containsAll_CollectionWithNullAbsent_FromJson() {
        stringSet.addAll(Arrays.asList("x", "y"));
        Collection<String> query = Collections.singletonList(null);
        assertFalse(stringSet.containsAll(query), "Null absent in set – should return false");
    }

    @Test
    void containsAll_NullCollectionArgument_ShouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> intSet.containsAll(null), "Passing null as the collection argument must throw NullPointerException");
    }

    @Test
    void containsAll_DuplicateElementsInCollection_ReturnsTrue() {
        intSet.addAll(Arrays.asList(2, 3, 4));
        Collection<Integer> query = Arrays.asList(2, 2, 2);
        assertTrue(intSet.containsAll(query), "All duplicates are present – should return true");
    }

    @Test
    void containsAll_AbsentElementFirst_ReturnsFalseImmediately() {
        intSet.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> query = Arrays.asList(4, 1);
        assertFalse(intSet.containsAll(query), "First element absent – should return false without checking later elements");
    }

    @Test
    void containsAll_ClassCastException_WhenWrongGenericType() {
        // The set is typed as Integer; contains(Object) may cast internally.
        intSet.addAll(Arrays.asList(1, 2, 3));
        Collection<String> query = Arrays.asList("string");
        assertThrows(ClassCastException.class, () -> intSet.containsAll(query), "Supplying a collection with an incompatible type should result in ClassCastException");
    }

    @Test
    void containsAll_ConcurrentModificationException_Propagated() {
        intSet.addAll(Arrays.asList(1, 2));
        Collection<Integer> cmeCollection = new CMECollection<>(Arrays.asList(1, 2));
        assertThrows(ConcurrentModificationException.class, () -> intSet.containsAll(cmeCollection), "Iterator throwing CME should propagate the exception");
    }

    @Test
    void containsAll_RuntimeException_Propagated() {
        intSet.addAll(Arrays.asList(1, 2));
        Collection<Integer> runtimeCollection = new RuntimeExceptionCollection<>(Arrays.asList(1, 2));
        assertThrows(RuntimeException.class, () -> intSet.containsAll(runtimeCollection), "Iterator throwing RuntimeException should propagate the exception");
    }
}
