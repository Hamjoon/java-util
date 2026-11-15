package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_equals_Test {

    private CaseInsensitiveSet<String> ciSet;

    /**
     * A Set whose {@code size()} method throws a {@link RuntimeException}.
     * Used to verify that {@code CaseInsensitiveSet.equals} propagates the exception.
     */
    private static class SizeExceptionSet<E> implements Set<E> {

        @Override
        public int size() {
            throw new RuntimeException("size() failure");
        }

        // The remaining methods are not used by equals (they will never be called)
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
     * A Set whose iterator throws {@link ConcurrentModificationException}
     * when {@code next()} is called. This mimics a collection that is modified
     * during iteration.
     */
    private static class CmeSet<E> implements Set<E> {

        private final Set<E> delegate = new HashSet<>();

        CmeSet(E element) {
            delegate.add(element);
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
                    // Simulate concurrent modification on the first call.
                    throw new ConcurrentModificationException("simulated CME");
                }

                @Override
                public void remove() {
                    /* no‑op */
                }
            };
        }

        // All other methods delegate to the underlying set (they are not used by equals).
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
        public boolean retainAll(Collection<?> c) {
            return delegate.retainAll(c);
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return delegate.removeAll(c);
        }

        @Override
        public void clear() {
            delegate.clear();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        ciSet = new CaseInsensitiveSet<>();
        ciSet.add("Apple");
        ciSet.add("Banana");
        ciSet.add("Cherry");
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required.
        ciSet = null;
    }

    @Test
    void equalsSameReference_returnsTrue() {
        assertTrue(ciSet.equals(ciSet), "A set must be equal to itself");
    }

    @Test
    void equalsNull_returnsFalse() {
        assertFalse(ciSet.equals(null), "A set must not be equal to null");
    }

    @Test
    void equalsNonSetObject_returnsFalse() {
        Object notASet = "I am not a set";
        assertFalse(ciSet.equals(notASet), "A set must not be equal to a non‑Set object");
    }

    @Test
    void equalsDifferentSize_returnsFalse() {
        Set<String> other = new HashSet<>();
        other.add("Apple");
        // one element fewer
        other.add("Banana");
        assertFalse(ciSet.equals(other), "Sets of different size must not be equal");
    }

    @Test
    void equalsSameElementsDifferentOrder_returnsTrue() {
        Set<String> other = new HashSet<>();
        other.add("Cherry");
        other.add("Banana");
        // order does not matter
        other.add("Apple");
        assertTrue(ciSet.equals(other), "Sets with the same elements must be equal regardless of order");
    }

    @Test
    void equalsCaseInsensitiveStrings_returnsTrue() {
        Set<String> other = new HashSet<>();
        // lower‑case
        other.add("apple");
        // upper‑case
        other.add("BANANA");
        // mixed case
        other.add("cHeRrY");
        assertTrue(ciSet.equals(other), "CaseInsensitiveSet must compare String elements ignoring case");
    }

    @Test
    void equalsMixedTypeElements_returnsTrueWhenContentsMatch() {
        // CaseInsensitiveSet can hold heterogeneous elements; we only care about Set contract.
        CaseInsensitiveSet<Object> mixedSet = new CaseInsensitiveSet<>();
        mixedSet.add("Apple");
        mixedSet.add(42);
        mixedSet.add(true);
        Set<Object> other = new HashSet<>();
        other.add(42);
        other.add(true);
        // different case but same logical value
        other.add("apple");
        assertTrue(mixedSet.equals(other), "Equality must succeed when all elements match case‑insensitively");
    }

    @Test
    void equalsAgainstSortedSet_returnsTrue() {
        Set<String> sorted = new TreeSet<>();
        sorted.add("Banana");
        sorted.add("Cherry");
        sorted.add("Apple");
        assertTrue(ciSet.equals(sorted), "Equality must work with other Set implementations such as TreeSet");
    }

    @Test
    void equalsEmptySetSameInstance_returnsTrue() {
        // Both sets are empty and the same instance – shortcut should fire.
        CaseInsensitiveSet<String> empty = new CaseInsensitiveSet<>();
        assertTrue(empty.equals(empty), "Empty set must be equal to itself");
    }

    @Test
    void equalsEmptyHashSetDifferentInstance_returnsTrue() {
        // Both sets are empty but distinct instances.
        CaseInsensitiveSet<String> emptyCi = new CaseInsensitiveSet<>();
        Set<String> emptyHash = new HashSet<>();
        assertTrue(emptyCi.equals(emptyHash), "Two empty sets of any implementation must be equal");
    }

    @Test
    void equalsSetWithNullElement_returnsTrue() {
        // Both sets contain a single null element.
        CaseInsensitiveSet<String> withNull = new CaseInsensitiveSet<>();
        withNull.add(null);
        Set<String> other = new HashSet<>();
        other.add(null);
        assertTrue(withNull.equals(other), "Sets containing only null should be equal");
    }

    @Test
    void equalsSetWithNullAndString_vsSetWithOnlyNull_returnsFalse() {
        // Size differs because ciSet has null + "test".
        CaseInsensitiveSet<String> ci = new CaseInsensitiveSet<>();
        ci.add(null);
        ci.add("test");
        Set<String> other = new HashSet<>();
        other.add(null);
        assertFalse(ci.equals(other), "Different sizes (null+string vs only null) must yield false");
    }

    @Test
    void equalsConcurrentSkipListSetSameElements_returnsTrue() {
        // Verify that equality works with a concurrent set implementation.
        Set<String> concurrent = new ConcurrentSkipListSet<>();
        concurrent.add("Apple");
        concurrent.add("Banana");
        concurrent.add("Cherry");
        assertTrue(ciSet.equals(concurrent), "Equality should succeed with ConcurrentSkipListSet");
    }

    @Test
    void equalsHashSetDifferentElementsSameSize_returnsFalse() {
        // Same size but at least one element differs.
        Set<String> other = new HashSet<>();
        other.add("Apple");
        other.add("Banana");
        // different element
        other.add("Durian");
        assertFalse(ciSet.equals(other), "Same‑size sets with a mismatching element must not be equal");
    }

    @Test
    void equalsTreeSetSameElements_returnsTrue() {
        Set<String> tree = new TreeSet<>();
        tree.addAll(Arrays.asList("Apple", "Banana", "Cherry"));
        assertTrue(ciSet.equals(tree), "TreeSet with identical elements should be equal");
    }

    @Test
    void equalsSizeExceptionSet_propagatesRuntimeException() {
        Set<String> bad = new SizeExceptionSet<>();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> ciSet.equals(bad), "equals should propagate RuntimeException from size()");
        assertEquals("size() failure", ex.getMessage());
    }

    /**
     * A Set that contains an element of an incompatible type (Integer).
     * The {@code containsAll} call will invoke {@code CaseInsensitiveSet.contains(Integer)},
     * which internally uses {@code StringUtilities.hashCodeIgnoreCase} and throws
     * {@link ClassCastException}.
     */
    @Test
    void equalsSetWithIncompatibleElement_throwsClassCastException() {
        Set<Integer> intSet = new HashSet<>();
        // Integer, not a String
        intSet.add(123);
        // The equals implementation will call ciSet.containsAll(intSet)
        // which eventually calls ciSet.contains(Integer) leading to ClassCastException.
        assertThrows(ClassCastException.class, () -> ciSet.equals(intSet), "containsAll should throw ClassCastException for incompatible element types");
    }

    @Test
    void equalsConcurrentModificationSet_propagatesCME() {
        Set<String> cmeSet = new CmeSet<>("Apple");
        assertThrows(ConcurrentModificationException.class, () -> ciSet.equals(cmeSet), "equals should propagate ConcurrentModificationException from iterator");
    }
}
