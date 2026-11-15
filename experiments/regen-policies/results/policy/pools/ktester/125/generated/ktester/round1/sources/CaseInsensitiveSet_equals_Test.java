package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_equals_Test {

    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test and populate it with a mixture of
        // case‑variant strings and a non‑String element.
        set = new CaseInsensitiveSet<>();
        set.add("One");
        set.add("Two");
        set.add("Three");
        // non‑String element
        set.add(42);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to help the garbage collector; not strictly required.
        set.clear();
    }

    @Test
    void equalsSameReference() {
        assertTrue(set.equals(set), "A set should be equal to itself");
    }

    @Test
    void equalsDifferentType() {
        assertFalse(set.equals("not a set"), "A set should not be equal to an object of another type");
    }

    @Test
    void equalsNull() {
        assertFalse(set.equals(null), "A set should not be equal to null");
    }

    @Test
    void equalsIdenticalContent() {
        Set<Object> other = new CaseInsensitiveSet<>();
        // different case, should match
        other.add("one");
        other.add("TWO");
        other.add("three");
        other.add(42);
        assertTrue(set.equals(other), "Sets with identical (case‑insensitive) contents must be equal");
    }

    @Test
    void equalsDifferentSize() {
        Set<Object> other = new CaseInsensitiveSet<>();
        other.add("One");
        other.add("Two");
        // missing "Three" and the integer element
        assertFalse(set.equals(other), "Sets with different sizes must not be equal");
    }

    @Test
    void equalsSameSizeDifferentContent() {
        Set<Object> other = new CaseInsensitiveSet<>();
        other.add("One");
        other.add("Two");
        other.add("Three");
        // different non‑String element
        other.add(99);
        assertFalse(set.equals(other), "Sets with same size but different elements must not be equal");
    }

    @Test
    void equalsWithMockSetMatchingBehavior() {
        // Create a mock Set that pretends to have the same size and contents as `set`.
        @SuppressWarnings("unchecked")
        Set<Object> mockSet = mock(Set.class);
        when(mockSet.size()).thenReturn(set.size());
        when(mockSet.containsAll(set)).thenReturn(true);
        // The equals implementation checks `containsAll` on the *other* set,
        // so we also need to make `set.containsAll(mockSet)` return true.
        // Since `set` is a real instance we can rely on its actual logic.
        // To make the call succeed we stub `mockSet.iterator()` (not used by equals)
        // but it is good practice to avoid UnexpectedStubbingException.
        when(mockSet.iterator()).thenReturn(Collections.emptyIterator());
        // The mock still implements Set, so the instanceof check passes.
        assertTrue(set.equals(mockSet), "A set should be equal to a mock Set that reports matching size and contents");
    }

    @Test
    void equalsWithMockSetMismatchingSize() {
        @SuppressWarnings("unchecked")
        Set<Object> mockSet = mock(Set.class);
        // deliberately wrong size
        when(mockSet.size()).thenReturn(set.size() - 1);
        when(mockSet.containsAll(set)).thenReturn(false);
        assertFalse(set.equals(mockSet), "A set should not be equal to a Set reporting a different size");
    }

    /**
     * Branch‑coverage style tests using plain {@link HashSet} as the
     * comparison argument.
     */
    @Test
    void equalsHashSetSameElements() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("a");
        ciSet.add("b");
        Set<String> other = new HashSet<>(Arrays.asList("a", "b"));
        assertTrue(ciSet.equals(other), "HashSet with identical elements should be equal");
    }

    @Test
    void equalsHashSetDifferentContent() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("a");
        ciSet.add("b");
        Set<String> other = new HashSet<>(Arrays.asList("a", "c"));
        assertFalse(ciSet.equals(other), "HashSet with a differing element should not be equal");
    }

    @Test
    void equalsHashSetLargerOther() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("a");
        ciSet.add("b");
        ciSet.add("c");
        Set<String> other = new HashSet<>(Arrays.asList("a", "b", "c", "d"));
        assertFalse(ciSet.equals(other), "Larger other set should cause size mismatch → false");
    }

    /**
     * Both sets empty – should be equal.
     */
    @Test
    void equalsBothEmptySets() {
        CaseInsensitiveSet<Object> emptyCi = new CaseInsensitiveSet<>();
        Set<Object> emptyHash = new HashSet<>();
        assertTrue(emptyCi.equals(emptyHash), "Two empty sets must be equal");
    }

    /**
     * Verify case‑insensitive matching for string elements.
     */
    @Test
    void equalsCaseInsensitiveMatching() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("a");
        // mixed case
        ciSet.add("B");
        Set<String> other = new HashSet<>(Arrays.asList("A", "b"));
        assertTrue(ciSet.equals(other), "Sets with case‑insensitively equal strings must be equal");
    }

    /**
     * Other set contains only {@code null}; size matches but {@code containsAll}
     * must fail, yielding {@code false}.
     */
    @Test
    void equalsOtherSetContainsOnlyNull() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("a");
        Set<String> other = new HashSet<>(Collections.singletonList(null));
        assertFalse(ciSet.equals(other), "ContainsAll should fail when other set only contains null");
    }

    /**
     * Two distinct {@link CaseInsensitiveSet} instances with the same elements.
     */
    @Test
    void equalsOtherCaseInsensitiveSetSameElements() {
        CaseInsensitiveSet<String> first = new CaseInsensitiveSet<>();
        first.add("x");
        first.add("y");
        CaseInsensitiveSet<String> second = new CaseInsensitiveSet<>();
        second.add("x");
        second.add("y");
        assertTrue(first.equals(second), "Two CaseInsensitiveSets with identical elements must be equal");
    }

    /**
     * The {@code size()} method of the *other* set throws an
     * {@link UnsupportedOperationException}. The exception should propagate
     * out of {@code equals}.
     */
    @Test
    void equalsOtherSetSizeThrowsUnsupportedOperationException() {
        Set<Object> badSet = new Set<Object>() {

            @Override
            public int size() {
                throw new UnsupportedOperationException("size not supported");
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
            public Iterator<Object> iterator() {
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
            public boolean add(Object e) {
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
            public boolean addAll(Collection<? extends Object> c) {
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
        };
        assertThrows(UnsupportedOperationException.class, () -> set.equals(badSet), "Exception from other.size() must propagate");
    }

    /**
     * The {@code iterator()} of the *other* set throws a
     * {@link ConcurrentModificationException} during the {@code containsAll}
     * evaluation.
     */
    @Test
    void equalsOtherSetIteratorThrowsConcurrentModificationException() {
        Set<String> badSet = new AbstractSet<String>() {

            @Override
            public int size() {
                return 1;
            }

            @Override
            public Iterator<String> iterator() {
                return new Iterator<String>() {

                    private boolean first = true;

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public String next() {
                        if (first) {
                            first = false;
                            return "a";
                        }
                        throw new ConcurrentModificationException("modification detected");
                    }

                    @Override
                    public void remove() {
                    }
                };
            }
        };
        assertThrows(ConcurrentModificationException.class, () -> set.equals(badSet), "Exception from other.iterator() during containsAll must propagate");
    }

    /**
     * Subclass of {@link CaseInsensitiveSet} whose {@code size()} method throws
     * a {@link NullPointerException}. The exception should propagate out of
     * {@code equals}.
     */
    @Test
    void equalsThisSetSizeThrowsNullPointerException() {
        CaseInsensitiveSet<Object> broken = new CaseInsensitiveSet<>() {

            @Override
            public int size() {
                throw new NullPointerException("size broken");
            }
        };
        Set<Object> other = new HashSet<>(Collections.singleton("a"));
        assertThrows(NullPointerException.class, () -> broken.equals(other), "Exception from this.size() must propagate");
    }

    /**
     * Subclass of {@link CaseInsensitiveSet} whose {@code containsAll}
     * throws an {@link IllegalStateException}. The exception should propagate.
     */
    @Test
    void equalsThisSetContainsAllThrowsIllegalStateException() {
        CaseInsensitiveSet<Object> broken = new CaseInsensitiveSet<>() {

            @Override
            public boolean containsAll(Collection<?> c) {
                throw new IllegalStateException("containsAll broken");
            }
        };
        Set<Object> other = new HashSet<>(Collections.singleton("a"));
        assertThrows(IllegalStateException.class, () -> broken.equals(other), "Exception from this.containsAll() must propagate");
    }

    /**
     * Verify that a {@link CaseInsensitiveSet} with three elements is not equal
     * to a {@link HashSet} containing only two of them (size mismatch where the
     * *this* set is larger).
     */
    @Test
    void equalsThisLargerThanOther() {
        CaseInsensitiveSet<String> ciSet = new CaseInsensitiveSet<>();
        ciSet.add("a");
        ciSet.add("b");
        ciSet.add("c");
        Set<String> other = new HashSet<>(Arrays.asList("a", "b"));
        assertFalse(ciSet.equals(other), "Size mismatch where this set is larger must return false");
    }
}
