package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_equals_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    /**
     * Set whose {@code size()} throws {@link UnsupportedOperationException}.
     */
    private static class SizeUnsupportedSet<E> implements Set<E> {

        @Override
        public int size() {
            throw new UnsupportedOperationException();
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
        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    /**
     * Set whose {@code size()} throws {@link IllegalStateException}.
     */
    private static class SizeIllegalStateSet<E> implements Set<E> {

        @Override
        public int size() {
            throw new IllegalStateException();
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
        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    /**
     * ConcurrentSet subclass whose iterator throws {@link ConcurrentModificationException}.
     */
    private static class BadIteratorConcurrentSet<E> extends ConcurrentSet<E> {

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
                    throw new IllegalStateException();
                }
            };
        }
    }

    /**
     * Set whose {@code contains(Object)} throws {@link UnsupportedOperationException}.
     */
    private static class ContainsUnsupportedSet<E> implements Set<E> {

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
            throw new UnsupportedOperationException();
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
        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    /**
     * Set whose {@code contains(Object)} throws a generic {@link RuntimeException}.
     */
    private static class ContainsRuntimeExceptionSet<E> implements Set<E> {

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
            throw new RuntimeException("boom");
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
        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references for GC
        intSet = null;
        stringSet = null;
    }

    @Test
    void equalsSameReference() {
        assertTrue(intSet.equals(intSet), "A set should be equal to itself");
    }

    @Test
    void equalsDifferentType() {
        assertFalse(intSet.equals("not a set"), "A set should not be equal to an object of another type");
    }

    @Test
    void equalsDifferentSize() {
        intSet.add(1);
        ConcurrentSet<Integer> other = new ConcurrentSet<>();
        other.add(1);
        other.add(2);
        assertFalse(intSet.equals(other), "Sets with different sizes must not be equal");
    }

    @Test
    void equalsSameElementsDifferentOrder() {
        intSet.addAll(Arrays.asList(1, 2, 3));
        ConcurrentSet<Integer> other = new ConcurrentSet<>();
        other.addAll(Arrays.asList(3, 2, 1));
        assertTrue(intSet.equals(other), "Sets containing the same elements (order irrelevant) must be equal");
    }

    @Test
    void equalsWithNullElements() {
        stringSet.addAll(Arrays.asList("a", null, "b"));
        ConcurrentSet<String> other = new ConcurrentSet<>();
        other.addAll(Arrays.asList(null, "b", "a"));
        assertTrue(stringSet.equals(other), "Sets that both contain null and the same non‑null elements must be equal");
    }

    @Test
    void equalsNullElementMissingInOther() {
        stringSet.addAll(Arrays.asList("a", null));
        ConcurrentSet<String> other = new ConcurrentSet<>();
        other.add("a");
        assertFalse(stringSet.equals(other), "If one set contains null and the other does not, they must not be equal");
    }

    @Test
    void equalsWhenOtherThrowsClassCastException() {
        // Create a mock Set that throws ClassCastException on contains()
        @SuppressWarnings("unchecked")
        Set<Integer> badSet = mock(Set.class);
        when(badSet.size()).thenReturn(1);
        when(badSet.iterator()).thenReturn(Collections.singleton(1).iterator());
        when(badSet.contains(any())).thenThrow(ClassCastException.class);
        intSet.add(1);
        assertFalse(intSet.equals(badSet), "If other.contains throws ClassCastException, equals must return false");
    }

    @Test
    void equalsWhenOtherThrowsNullPointerException() {
        // Create a mock Set that throws NullPointerException on contains()
        @SuppressWarnings("unchecked")
        Set<Integer> badSet = mock(Set.class);
        when(badSet.size()).thenReturn(1);
        when(badSet.iterator()).thenReturn(Collections.singleton(1).iterator());
        when(badSet.contains(any())).thenThrow(NullPointerException.class);
        intSet.add(1);
        assertFalse(intSet.equals(badSet), "If other.contains throws NullPointerException, equals must return false");
    }

    @Test
    void equalsSizeMismatchWithStandardSet() {
        // this set has 2 elements, the other Set.of(..) has 3
        intSet.addAll(Arrays.asList(1, 2));
        Set<Integer> other = Set.of(1, 2, 3);
        assertFalse(intSet.equals(other), "Different sizes should cause equality to be false");
    }

    @Test
    void equalsContentExactMatch() {
        intSet.addAll(Arrays.asList(1, 2));
        Set<Integer> other = Set.of(1, 2);
        assertTrue(intSet.equals(other), "Identical content should result in equality");
    }

    @Test
    void equalsContentMissingElement() {
        intSet.addAll(Arrays.asList(1, 2));
        Set<Integer> other = Set.of(1, 3);
        assertFalse(intSet.equals(other), "Missing element in other set must make equals return false");
    }

    @Test
    void equalsEmptySets() {
        // both sets are freshly created and therefore empty
        assertTrue(intSet.equals(new HashSet<>()), "Two empty sets must be equal");
    }

    @Test
    void equalsWhenOtherContainsNullAndThrowsNPE() {
        // ConcurrentSkipListSet does not allow null and throws NPE on contains(null)
        intSet.add(null);
        Set<Integer> skipListSet = new ConcurrentSkipListSet<>();
        assertFalse(intSet.equals(skipListSet), "Null element in this set with a NPE‑throwing other should result in false");
    }

    @Test
    void equalsWhenOtherIsTreeSetWithClassCastException() {
        // this set holds a String, other is a TreeSet<Integer>
        stringSet.add("a");
        Set<Integer> treeSet = new TreeSet<>(Set.of(1));
        assertFalse(stringSet.equals(treeSet), "ClassCastException from TreeSet.contains should be caught and return false");
    }

    @Test
    void equalsPropagatesIllegalArgumentExceptionFromContains() {
        @SuppressWarnings("unchecked")
        Set<Integer> badSet = mock(Set.class);
        when(badSet.size()).thenReturn(1);
        when(badSet.iterator()).thenReturn(Collections.singleton(1).iterator());
        when(badSet.contains(any())).thenThrow(IllegalArgumentException.class);
        intSet.add(1);
        assertThrows(IllegalArgumentException.class, () -> intSet.equals(badSet), "IllegalArgumentException should propagate out of equals");
    }

    @Test
    void equalsPropagatesUnsupportedOperationExceptionFromSize() {
        Set<Integer> badSet = new SizeUnsupportedSet<>();
        intSet.add(1);
        assertThrows(UnsupportedOperationException.class, () -> intSet.equals(badSet), "UnsupportedOperationException from size() should propagate");
    }

    @Test
    void equalsPropagatesIllegalStateExceptionFromSize() {
        Set<Integer> badSet = new SizeIllegalStateSet<>();
        intSet.add(1);
        assertThrows(IllegalStateException.class, () -> intSet.equals(badSet), "IllegalStateException from size() should propagate");
    }

    @Test
    void equalsPropagatesConcurrentModificationExceptionFromIterator() {
        ConcurrentSet<Integer> badIterSet = new BadIteratorConcurrentSet<>();
        badIterSet.add(1);
        Set<Integer> other = new HashSet<>(Set.of(1));
        assertThrows(ConcurrentModificationException.class, () -> badIterSet.equals(other), "Exception from iterator should propagate");
    }

    @Test
    void equalsPropagatesUnsupportedOperationExceptionFromContains() {
        Set<Integer> badSet = new ContainsUnsupportedSet<>();
        intSet.add(1);
        assertThrows(UnsupportedOperationException.class, () -> intSet.equals(badSet), "UnsupportedOperationException from contains should propagate");
    }

    @Test
    void equalsPropagatesRuntimeExceptionFromContains() {
        Set<Integer> badSet = new ContainsRuntimeExceptionSet<>();
        intSet.add(1);
        assertThrows(RuntimeException.class, () -> intSet.equals(badSet), "RuntimeException from contains should propagate");
    }
}
