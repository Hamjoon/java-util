package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_equals_Test {

    /* --------------------------------------------------------------------- */
    private static class BadIteratorSet<T> extends ConcurrentSet<T> {

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public T next() {
                    throw new ConcurrentModificationException("bad iterator");
                }

                @Override
                public void remove() {
                    /* no‑op */
                }
            };
        }
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> sizeThrowingSetProvider() {
        return Stream.of(Arguments.of(new IllegalStateException("size broken")));
    }

    private static Stream<Arguments> containsThrowingSetProvider() {
        return Stream.of(Arguments.of(new IllegalArgumentException("contains broken")), Arguments.of(new UnsupportedOperationException("unsupported")));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsShouldReturnTrueWhenComparingWithItself() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        assertTrue(set.equals(set), "A set must be equal to itself");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsShouldReturnTrueForDifferentSetImplementationsWithSameElements() {
        ConcurrentSet<String> concurrentSet = new ConcurrentSet<>();
        concurrentSet.addAll(Arrays.asList("a", "b", null));
        Set<String> hashSet = new HashSet<>(Arrays.asList("b", null, "a"));
        // TreeSet cannot contain null
        Set<String> treeSet = new TreeSet<>(Arrays.asList("a", "b"));
        // Null is ignored for TreeSet – we compare only the non‑null part
        assertTrue(concurrentSet.equals(hashSet), "ConcurrentSet should be equal to a HashSet with same elements");
        assertTrue(concurrentSet.equals(new HashSet<>(concurrentSet)), "ConcurrentSet should be equal to another ConcurrentSet with same elements");
        assertTrue(concurrentSet.equals(new LinkedHashSet<>(concurrentSet)), "ConcurrentSet should be equal to a LinkedHashSet with same elements");
        // TreeSet comparison (null removed) – both contain "a" and "b"
        assertTrue(concurrentSet.equals(treeSet), "ConcurrentSet should be equal to a TreeSet when both contain the same non‑null elements");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsShouldReturnFalseWhenSizesDiffer() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3));
        Set<Integer> smaller = new HashSet<>(Arrays.asList(1, 2));
        assertFalse(set.equals(smaller), "Sets with different sizes must not be equal");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsShouldReturnFalseWhenElementsDiffer() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("x", "y", "z"));
        Set<String> other = new HashSet<>(Arrays.asList("x", "y", "w"));
        assertFalse(set.equals(other), "Sets with same size but different elements must not be equal");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsShouldReturnFalseWhenOtherIsNotASet() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(42);
        String notASet = "I am not a set";
        assertFalse(set.equals(notASet), "equals must return false when the other object is not a Set");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsShouldReturnFalseWhenOtherSetThrowsClassCastException() {
        // ConcurrentSet contains Strings
        ConcurrentSet<String> stringSet = new ConcurrentSet<>();
        stringSet.addAll(Arrays.asList("a", "b"));
        // TreeSet<Integer> will throw ClassCastException when contains receives a String
        Set<Integer> integerTreeSet = new TreeSet<>(Arrays.asList(1, 2, 3));
        assertFalse(stringSet.equals(integerTreeSet), "equals must catch ClassCastException from the other set and return false");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsShouldHandleNullElementsCorrectly() {
        ConcurrentSet<String> setWithNull = new ConcurrentSet<>();
        setWithNull.addAll(Arrays.asList(null, "alpha", "beta"));
        Set<String> otherWithNull = new HashSet<>(Arrays.asList("beta", null, "alpha"));
        assertTrue(setWithNull.equals(otherWithNull), "Sets that both contain null should be considered equal");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsShouldReturnFalseWhenOnlyOneSetContainsNull() {
        ConcurrentSet<String> setWithNull = new ConcurrentSet<>();
        setWithNull.addAll(Arrays.asList(null, "x"));
        Set<String> setWithoutNull = new HashSet<>(Arrays.asList("x"));
        assertFalse(setWithNull.equals(setWithoutNull), "If only one set contains null the sets must not be equal");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("sizeThrowingSetProvider")
    void equalsShouldPropagateExceptionWhenOtherSizeThrows(RuntimeException ex) {
        // Arrange: a normal ConcurrentSet with two elements
        ConcurrentSet<String> cs = new ConcurrentSet<>();
        cs.addAll(Arrays.asList("a", "b"));
        // Mock a Set that throws when size() is invoked
        @SuppressWarnings("unchecked")
        Set<String> badSet = mock(Set.class);
        when(badSet.size()).thenThrow(ex);
        // Act & Assert: the exception must bubble out of equals()
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> cs.equals(badSet), "equals should not catch exceptions from other.size()");
        assertSame(ex, thrown, "The same exception instance should be propagated");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("containsThrowingSetProvider")
    void equalsShouldPropagateExceptionWhenOtherContainsThrows(RuntimeException ex) {
        // Arrange: a ConcurrentSet with a single element
        ConcurrentSet<Integer> cs = new ConcurrentSet<>();
        cs.add(1);
        // Mock a Set that reports correct size but throws on contains()
        @SuppressWarnings("unchecked")
        Set<Integer> badSet = mock(Set.class);
        when(badSet.size()).thenReturn(1);
        when(badSet.contains(any())).thenThrow(ex);
        // Act & Assert: the unchecked exception must propagate
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> cs.equals(badSet), "equals should not catch exceptions from other.contains()");
        assertSame(ex, thrown, "The same exception instance should be propagated");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsShouldReturnFalseWhenOtherContainsThrowsNullPointerException() {
        // Arrange: ConcurrentSet with a null element
        ConcurrentSet<String> cs = new ConcurrentSet<>();
        cs.addAll(Arrays.asList(null, "x"));
        // Mock a Set that throws NPE on contains()
        @SuppressWarnings("unchecked")
        Set<String> npeSet = mock(Set.class);
        when(npeSet.size()).thenReturn(2);
        when(npeSet.contains(any())).thenThrow(new NullPointerException("NPE in contains"));
        // Act
        boolean result = cs.equals(npeSet);
        // Assert
        assertFalse(result, "NullPointerException from contains should be caught and result in false");
    }

    @Test
    void equalsShouldPropagateExceptionWhenIteratorThrows() {
        // Arrange: a subclass with a bad iterator
        BadIteratorSet<Integer> badIterSet = new BadIteratorSet<>();
        badIterSet.addAll(Arrays.asList(1, 2));
        Set<Integer> other = new HashSet<>(Arrays.asList(1, 2));
        // Act & Assert
        assertThrows(ConcurrentModificationException.class, () -> badIterSet.equals(other), "Exception from iterator.next() must propagate");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsShouldReturnTrueWhenComparingWithItselfAgain() {
        ConcurrentSet<Long> cs = new ConcurrentSet<>();
        cs.addAll(Arrays.asList(10L, 20L));
        assertTrue(cs.equals(cs), "A set must be equal to itself (reference equality)");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsShouldReturnFalseWhenOtherIsNotASetAgain() {
        ConcurrentSet<String> cs = new ConcurrentSet<>();
        cs.add("hello");
        assertFalse(cs.equals("not a set"), "Non‑Set argument must yield false");
    }
}
