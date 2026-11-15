package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;

// No mocks are actually used, but kept to match the template
@ExtendWith(MockitoExtension.class)
class ConcurrentSet_containsAll_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    /**
     * An {@link Iterable} whose {@link Iterator} can be configured to throw a
     * {@link RuntimeException} either from {@code hasNext()} or {@code next()}.
     */
    private static class RuntimeExceptionIterable implements Iterable<Object> {

        private final boolean hasNextThrows;

        private final boolean nextThrows;

        RuntimeExceptionIterable(boolean hasNextThrows, boolean nextThrows) {
            this.hasNextThrows = hasNextThrows;
            this.nextThrows = nextThrows;
        }

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {

                private boolean first = true;

                @Override
                public boolean hasNext() {
                    if (hasNextThrows) {
                        throw new RuntimeException("hasNext() forced exception");
                    }
                    return first;
                }

                @Override
                public Object next() {
                    if (!first) {
                        throw new NoSuchElementException();
                    }
                    if (nextThrows) {
                        throw new RuntimeException("next() forced exception");
                    }
                    first = false;
                    // dummy element
                    return new Object();
                }
            };
        }
    }

    /**
     * An {@link Iterable} whose iterator's {@code hasNext()} throws a
     * {@link NullPointerException}.
     */
    private static class NullPointerExceptionIterable implements Iterable<Object> {

        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {

                @Override
                public boolean hasNext() {
                    throw new NullPointerException("hasNext() forced NPE");
                }

                @Override
                public Object next() {
                    // never reached
                    return null;
                }
            };
        }
    }

    /**
     * An {@link ArrayList} that modifies itself during iteration, causing a
     * {@link ConcurrentModificationException} to be thrown by the next call to
     * {@code hasNext()} or {@code next()}.
     */
    private static class ModifiableArrayList<E> extends ArrayList<E> {

        ModifiableArrayList(Collection<? extends E> c) {
            super(c);
        }

        @Override
        public Iterator<E> iterator() {
            final Iterator<E> delegate = super.iterator();
            return new Iterator<E>() {

                private boolean modified = false;

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public E next() {
                    // On first element, modify the underlying list to trigger CME
                    if (!modified) {
                        // structural change
                        ModifiableArrayList.this.add((E) new Object());
                        modified = true;
                    }
                    return delegate.next();
                }

                @Override
                public void remove() {
                    delegate.remove();
                }
            };
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for each test to guarantee isolation
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets in case a test forgets to do so (defensive)
        if (intSet != null)
            intSet.clear();
        if (stringSet != null)
            stringSet.clear();
    }

    @Test
    void containsAll_AllElementsPresent_ReturnsTrue() {
        intSet.addAll(Arrays.asList(1, 2, 3, 4, 5));
        Collection<Integer> query = Arrays.asList(2, 4, 5);
        assertTrue(intSet.containsAll(query), "containsAll should return true when every element of the collection is present");
    }

    @Test
    void containsAll_MissingElements_ReturnsFalse() {
        intSet.addAll(Arrays.asList(10, 20, 30));
        Collection<Integer> query = Arrays.asList(10, 40);
        assertFalse(intSet.containsAll(query), "containsAll should return false when at least one element of the collection is missing");
    }

    @Test
    void containsAll_EmptyCollection_ReturnsTrue() {
        // Even when the set itself is empty, containsAll on an empty collection must be true
        Collection<Integer> empty = Collections.emptyList();
        assertTrue(intSet.containsAll(empty), "containsAll should return true for an empty collection regardless of set contents");
    }

    @Test
    void containsAll_CollectionContainsNull_ElementNullInSet_ReturnsTrue() {
        stringSet.addAll(Arrays.asList("a", null, "b"));
        Collection<String> query = Arrays.asList(null, "a");
        assertTrue(stringSet.containsAll(query), "containsAll should correctly handle null elements when they are present in the set");
    }

    @Test
    void containsAll_CollectionContainsNull_ElementNullNotInSet_ReturnsFalse() {
        stringSet.addAll(Arrays.asList("x", "y"));
        Collection<String> query = Arrays.asList((String) null);
        assertFalse(stringSet.containsAll(query), "containsAll should return false when the collection contains null but the set does not");
    }

    @Test
    void containsAll_DuplicateElementsInCollection_IgnoresDuplicates() {
        intSet.addAll(Arrays.asList(1, 2, 3));
        // The collection has duplicate '2's; containsAll should still return true
        Collection<Integer> query = Arrays.asList(2, 2, 3);
        assertTrue(intSet.containsAll(query), "containsAll should treat duplicate entries in the argument collection as a single check");
    }

    @Test
    void containsAll_NullCollection_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> intSet.containsAll(null), "containsAll should throw NPE when the supplied collection is null");
    }

    @Test
    void containsAll_EmptySetWithNonEmptyCollection_ReturnsFalse() {
        // Set is empty, collection has one element
        Collection<String> query = Collections.singletonList("any");
        assertFalse(stringSet.containsAll(query), "An empty set must return false for any non‑empty collection");
    }

    @Test
    void containsAll_CollectionWithDifferentElementTypes_ReturnsFalse() {
        // Set holds String values "1" and "2"
        stringSet.addAll(Arrays.asList("1", "2"));
        // Collection contains Integer objects 1 and 2
        Collection<Integer> query = Arrays.asList(1, 2);
        assertFalse(stringSet.containsAll(query), "Type‑mismatched elements should not be considered equal, resulting in false");
    }

    @Test
    void containsAll_CollectionLargerThanSetAllElementsPresent_IncludingDuplicates_ReturnsTrue() {
        stringSet.addAll(Arrays.asList("a", "b"));
        Collection<String> query = Arrays.asList("a", "b", "a", "b");
        assertTrue(stringSet.containsAll(query), "Duplicates in the collection must not affect the result when all distinct elements are present");
    }

    @Test
    void containsAll_IteratorThrowsRuntimeExceptionOnHasNext_PropagatesException() {
        Iterable<Object> badIterable = new RuntimeExceptionIterable(true, false);
        assertThrows(RuntimeException.class, () -> intSet.containsAll(badIterable), "RuntimeException from iterator.hasNext() must be propagated");
    }

    @Test
    void containsAll_IteratorThrowsRuntimeExceptionOnNext_PropagatesException() {
        Iterable<Object> badIterable = new RuntimeExceptionIterable(false, true);
        assertThrows(RuntimeException.class, () -> intSet.containsAll(badIterable), "RuntimeException from iterator.next() must be propagated");
    }

    @Test
    void containsAll_IteratorThrowsNullPointerException_PropagatesException() {
        Iterable<Object> npeIterable = new NullPointerExceptionIterable();
        assertThrows(NullPointerException.class, () -> intSet.containsAll(npeIterable), "NullPointerException from iterator should be propagated");
    }

    @Test
    void containsAll_CollectionModifiedDuringIteration_ThrowsConcurrentModificationException() {
        ModifiableArrayList<String> modifiable = new ModifiableArrayList<>(Arrays.asList("a", "b"));
        // The iterator will modify the list on its first call to next()
        assertThrows(ConcurrentModificationException.class, () -> stringSet.addAll(Arrays.asList("a", "b")).containsAll(modifiable), "ConcurrentModificationException must be thrown when the collection is structurally modified during iteration");
    }
}
