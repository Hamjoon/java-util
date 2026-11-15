package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_descendingSet_Test {

    private NavigableSet<String> originalSet;

    private NavigableSet<String> descendingSet;

    class UnsupportedNavigableSet<E> implements NavigableSet<E> {

        private final NavigableSet<E> delegate = new ConcurrentSkipListSet<>();

        @Override
        public E lower(E e) {
            return delegate.lower(e);
        }

        @Override
        public E floor(E e) {
            return delegate.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return delegate.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return delegate.higher(e);
        }

        @Override
        public E pollFirst() {
            return delegate.pollFirst();
        }

        @Override
        public E pollLast() {
            return delegate.pollLast();
        }

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
        public Iterator<E> iterator() {
            return delegate.iterator();
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

        @Override
        public Comparator<? super E> comparator() {
            return delegate.comparator();
        }

        @Override
        public E first() {
            return delegate.first();
        }

        @Override
        public E last() {
            return delegate.last();
        }

        @Override
        public NavigableSet<E> descendingSet() {
            throw new UnsupportedOperationException("descendingSet not supported");
        }

        @Override
        public Iterator<E> descendingIterator() {
            return delegate.descendingIterator();
        }

        @Override
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return delegate.subSet(fromElement, fromInclusive, toElement, toInclusive);
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return delegate.headSet(toElement, inclusive);
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return delegate.tailSet(fromElement, inclusive);
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return delegate.subSet(fromElement, toElement);
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return delegate.headSet(toElement);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return delegate.tailSet(fromElement);
        }
    }

    class UnsupportedNavigableSet<E> implements NavigableSet<E> {

        private final NavigableSet<E> delegate = new ConcurrentSkipListSet<>();

        @Override
        public E lower(E e) {
            return delegate.lower(e);
        }

        @Override
        public E floor(E e) {
            return delegate.floor(e);
        }

        @Override
        public E ceiling(E e) {
            return delegate.ceiling(e);
        }

        @Override
        public E higher(E e) {
            return delegate.higher(e);
        }

        @Override
        public E pollFirst() {
            return delegate.pollFirst();
        }

        @Override
        public E pollLast() {
            return delegate.pollLast();
        }

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
        public Iterator<E> iterator() {
            return delegate.iterator();
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

        @Override
        public Comparator<? super E> comparator() {
            return delegate.comparator();
        }

        @Override
        public E first() {
            return delegate.first();
        }

        @Override
        public E last() {
            return delegate.last();
        }

        @Override
        public NavigableSet<E> descendingSet() {
            throw new UnsupportedOperationException("descendingSet not supported");
        }

        @Override
        public Iterator<E> descendingIterator() {
            return delegate.descendingIterator();
        }

        @Override
        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
            return delegate.subSet(fromElement, fromInclusive, toElement, toInclusive);
        }

        @Override
        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
            return delegate.headSet(toElement, inclusive);
        }

        @Override
        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
            return delegate.tailSet(fromElement, inclusive);
        }

        @Override
        public SortedSet<E> subSet(E fromElement, E toElement) {
            return delegate.subSet(fromElement, toElement);
        }

        @Override
        public SortedSet<E> headSet(E toElement) {
            return delegate.headSet(toElement);
        }

        @Override
        public SortedSet<E> tailSet(E fromElement) {
            return delegate.tailSet(fromElement);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a set with natural ordering and a few elements, including null.
        originalSet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(originalSet.add("apple"));
        assertTrue(originalSet.add("banana"));
        assertTrue(originalSet.add("cherry"));
        assertTrue(originalSet.add(null));
        // Obtain the descending view that we are going to test.
        descendingSet = originalSet.descendingSet();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination.
        originalSet.clear();
        descendingSet.clear();
    }

    @Test
    void descendingSetIteratesInReverseOrder() {
        // In the natural ordering implementation null is the greatest element,
        // therefore it should appear first when iterating the descending set.
        Iterator<String> it = descendingSet.iterator();
        assertEquals(null, it.next(), "First element should be null");
        assertEquals("cherry", it.next(), "Second element should be 'cherry'");
        assertEquals("banana", it.next(), "Third element should be 'banana'");
        assertEquals("apple", it.next(), "Fourth element should be 'apple'");
        assertFalse(it.hasNext(), "No more elements expected");
    }

    @Test
    void descendingSetReflectsModificationsInOriginal() {
        // Add a new element to the original set after the descending view was created.
        assertTrue(originalSet.add("date"));
        // The descending view must now contain the new element as the smallest one.
        Iterator<String> it = descendingSet.iterator();
        assertEquals(null, it.next());
        assertEquals("date", it.next(), "New element should appear after null in descending order");
        assertEquals("cherry", it.next());
        assertEquals("banana", it.next());
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
        // Remove an element via the descending view and verify it disappears from the original.
        assertTrue(descendingSet.remove("banana"));
        assertFalse(originalSet.contains("banana"));
        assertEquals(4, originalSet.size());
    }

    @Test
    void descendingSetPreservesOriginalComparator() {
        // Build a set with a custom comparator (by string length).
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> customSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        customSet.add("kiwi");
        customSet.add("apple");
        customSet.add(null);
        // Obtain its descending view.
        NavigableSet<String> customDesc = customSet.descendingSet();
        // The comparator returned by the view must be the same instance that was supplied
        // to the original set (the originalComparator is stored unchanged).
        assertSame(lengthComparator, customDesc.comparator(), "Descending set should retain the original comparator instance");
    }

    @Test
    void descendingSetEmptySet() {
        NavigableSet<Integer> empty = new ConcurrentNavigableSetNullSafe<>();
        NavigableSet<Integer> desc = empty.descendingSet();
        assertTrue(desc.isEmpty(), "Descending set of an empty set must be empty");
        assertEquals(empty.comparator(), desc.comparator(), "Comparator of descending empty set must equal original comparator");
        assertFalse(desc.iterator().hasNext(), "Iterator of descending empty set must have no elements");
    }

    @Test
    void descendingSetNonEmptyOrder() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();
        Iterator<Integer> it = desc.iterator();
        assertEquals(3, it.next());
        assertEquals(2, it.next());
        assertEquals(1, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingSetFirstAndLastSwapped() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();
        assertEquals(3, desc.first(), "first() of descending set should be highest element of original");
        assertEquals(1, desc.last(), "last() of descending set should be lowest element of original");
    }

    @Test
    void descendingSetAddReflectsOriginal() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();
        assertTrue(desc.add(4));
        assertTrue(set.contains(4));
        assertTrue(desc.contains(4));
    }

    @Test
    void descendingSetRemoveReflectsOriginal() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();
        assertTrue(desc.remove(2));
        assertFalse(set.contains(2));
        assertFalse(desc.contains(2));
    }

    @Test
    void descendingSetReflectsOriginalAdditions() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();
        set.add(5);
        assertTrue(desc.contains(5));
        // iterator order: 5,3,2,1
        Iterator<Integer> it = desc.iterator();
        assertEquals(5, it.next());
        assertEquals(3, it.next());
        assertEquals(2, it.next());
        assertEquals(1, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingSetReflectsOriginalRemovals() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();
        set.remove(2);
        assertFalse(desc.contains(2));
        // iterator order: 3,1
        Iterator<Integer> it = desc.iterator();
        assertEquals(3, it.next());
        assertEquals(1, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingSetDoubleInversionRestoresOriginalOrder() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> doubleDesc = set.descendingSet().descendingSet();
        assertEquals(set.size(), doubleDesc.size());
        Iterator<Integer> it = doubleDesc.iterator();
        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingSetIteratorConsistency() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();
        Iterator<Integer> forward = set.iterator();
        Iterator<Integer> reverseFromDesc = desc.descendingIterator();
        while (forward.hasNext() && reverseFromDesc.hasNext()) {
            assertEquals(forward.next(), reverseFromDesc.next(), "descendingSet().descendingIterator() must match original iterator order");
        }
        assertFalse(forward.hasNext());
        assertFalse(reverseFromDesc.hasNext());
    }

    @Test
    void descendingSetPollOperations() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList(1, 2, 3));
        NavigableSet<Integer> desc = set.descendingSet();
        // pollFirst on descending view should behave like pollLast on original (return 3)
        Integer first = desc.pollFirst();
        assertEquals(3, first);
        assertFalse(set.contains(3));
        assertFalse(desc.contains(3));
        assertEquals(2, set.size());
        // pollLast on descending view should behave like pollFirst on original (return 1)
        Integer last = desc.pollLast();
        assertEquals(1, last);
        assertFalse(set.contains(1));
        assertFalse(desc.contains(1));
        assertEquals(1, set.size());
    }

    @Test
    void descendingSetSizeConsistency() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        NavigableSet<Integer> desc = set.descendingSet();
        assertEquals(set.size(), desc.size());
        set.add(10);
        assertEquals(set.size(), desc.size());
        desc.add(20);
        assertEquals(set.size(), desc.size());
        desc.remove(10);
        assertEquals(set.size(), desc.size());
    }

    @Test
    void descendingSetNullElementHandling() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("a");
        set.add(null);
        set.add("b");
        NavigableSet<String> desc = set.descendingSet();
        assertTrue(desc.contains(null), "Descending set must contain null if original does");
        Iterator<String> it = desc.iterator();
        // natural order treats null as greatest, so it appears first in descending view
        assertNull(it.next(), "Null should be first element in descending iterator");
        assertEquals("b", it.next());
        assertEquals("a", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingSetCustomComparatorOrdering() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 1
        set.add("a");
        // length 3
        set.add("bbb");
        // length 2
        set.add("cc");
        // length 0
        set.add(null);
        NavigableSet<String> desc = set.descendingSet();
        // Comparator instance must be the same
        assertSame(lengthComparator, desc.comparator(), "Descending set must retain the original comparator instance");
        // Expected order: longest to shortest (3,2,1,0)
        Iterator<String> it = desc.iterator();
        assertEquals("bbb", it.next());
        assertEquals("cc", it.next());
        assertEquals("a", it.next());
        assertNull(it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void descendingSetNullInternalSetThrowsNPE() throws Exception {
        // Create an instance via the public constructor then inject null into internalSet
        ConcurrentNavigableSetNullSafe<String> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        // force null internal set
        internalField.set(wrapper, null);
        assertThrows(NullPointerException.class, wrapper::descendingSet, "descendingSet() should throw NPE when internalSet is null");
    }

    @Test
    void descendingSetUnsupportedOperationPropagates() throws Exception {
        // Minimal NavigableSet that throws UnsupportedOperationException on descendingSet()
        class UnsupportedNavigableSet<E> implements NavigableSet<E> {

            private final NavigableSet<E> delegate = new ConcurrentSkipListSet<>();

            @Override
            public E lower(E e) {
                return delegate.lower(e);
            }

            @Override
            public E floor(E e) {
                return delegate.floor(e);
            }

            @Override
            public E ceiling(E e) {
                return delegate.ceiling(e);
            }

            @Override
            public E higher(E e) {
                return delegate.higher(e);
            }

            @Override
            public E pollFirst() {
                return delegate.pollFirst();
            }

            @Override
            public E pollLast() {
                return delegate.pollLast();
            }

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
            public Iterator<E> iterator() {
                return delegate.iterator();
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

            @Override
            public Comparator<? super E> comparator() {
                return delegate.comparator();
            }

            @Override
            public E first() {
                return delegate.first();
            }

            @Override
            public E last() {
                return delegate.last();
            }

            @Override
            public NavigableSet<E> descendingSet() {
                throw new UnsupportedOperationException("descendingSet not supported");
            }

            @Override
            public Iterator<E> descendingIterator() {
                return delegate.descendingIterator();
            }

            @Override
            public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
                return delegate.subSet(fromElement, fromInclusive, toElement, toInclusive);
            }

            @Override
            public NavigableSet<E> headSet(E toElement, boolean inclusive) {
                return delegate.headSet(toElement, inclusive);
            }

            @Override
            public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
                return delegate.tailSet(fromElement, inclusive);
            }

            @Override
            public SortedSet<E> subSet(E fromElement, E toElement) {
                return delegate.subSet(fromElement, toElement);
            }

            @Override
            public SortedSet<E> headSet(E toElement) {
                return delegate.headSet(toElement);
            }

            @Override
            public SortedSet<E> tailSet(E fromElement) {
                return delegate.tailSet(fromElement);
            }
        }
        // Wrap the unsupported set using reflection to set internalSet directly
        UnsupportedNavigableSet<Integer> unsupported = new UnsupportedNavigableSet<>();
        ConcurrentNavigableSetNullSafe<Integer> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(wrapper, unsupported);
        assertThrows(UnsupportedOperationException.class, wrapper::descendingSet, "descendingSet() should propagate UnsupportedOperationException from internal set");
    }
}
