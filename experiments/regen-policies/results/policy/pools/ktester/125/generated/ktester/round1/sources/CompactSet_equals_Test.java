package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactSet_equals_Test {

    private CompactSet<String> caseSensitiveSet;

    private CompactSet<String> caseInsensitiveSortedSet;

    private Set<String> hashSet;

    private Set<String> treeSet;

    /**
     * Set that always returns false from {@code equals(Object)} regardless of content.
     */
    private static class CustomSetAlwaysFalseEquals<E> extends AbstractSet<E> {

        private final Set<E> delegate;

        CustomSetAlwaysFalseEquals(Collection<? extends E> c) {
            this.delegate = new HashSet<>(c);
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean equals(Object o) {
            // deliberately break equality contract
            return false;
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    /**
     * Set that uses reference equality (==) in its {@code equals(Object)} implementation.
     */
    private static class CustomSetReferenceEquality<E> extends AbstractSet<E> {

        private final Set<E> delegate;

        CustomSetReferenceEquality(Collection<? extends E> c) {
            this.delegate = new HashSet<>(c);
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean equals(Object o) {
            // only true for the same instance
            return this == o;
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    /**
     * Set whose {@code size()} method throws an IllegalStateException.
     */
    private static class CustomSetThrowingOnSize<E> extends AbstractSet<E> {

        private final Set<E> delegate;

        CustomSetThrowingOnSize(Collection<? extends E> c) {
            this.delegate = new HashSet<>(c);
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            throw new IllegalStateException("size error");
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }
    }

    /**
     * Set whose {@code iterator()} method throws a ConcurrentModificationException.
     */
    private static class CustomSetThrowingOnIterator<E> extends AbstractSet<E> {

        private final Set<E> delegate;

        CustomSetThrowingOnIterator(Collection<? extends E> c) {
            this.delegate = new HashSet<>(c);
        }

        @Override
        public Iterator<E> iterator() {
            throw new ConcurrentModificationException("iterator error");
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }
    }

    /**
     * Set whose {@code contains(Object)} throws a NullPointerException.
     */
    private static class CustomSetThrowingOnContains<E> extends AbstractSet<E> {

        private final Set<E> delegate;

        CustomSetThrowingOnContains(Collection<? extends E> c) {
            this.delegate = new HashSet<>(c);
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            throw new NullPointerException("contains error");
        }
    }

    /**
     * Set whose {@code containsAll(Collection)} throws an UnsupportedOperationException.
     */
    private static class CustomSetThrowingOnContainsAll<E> extends AbstractSet<E> {

        private final Set<E> delegate;

        CustomSetThrowingOnContainsAll(Collection<? extends E> c) {
            this.delegate = new HashSet<>(c);
        }

        @Override
        public Iterator<E> iterator() {
            return delegate.iterator();
        }

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean contains(Object o) {
            return delegate.contains(o);
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException("containsAll error");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default case‑sensitive CompactSet (insertion order)
        caseSensitiveSet = new CompactSet<>();
        // Case‑insensitive, sorted CompactSet built via the builder
        caseInsensitiveSortedSet = CompactSet.<String>builder().caseSensitive(false).sortedOrder().compactSize(// any value >= 2 is fine
        50).build();
        // Regular Java sets for cross‑type equality checks
        hashSet = new HashSet<>();
        treeSet = new TreeSet<>();
        // Populate all sets with the same elements
        List<String> elements = Arrays.asList("apple", "banana", "cherry");
        caseSensitiveSet.addAll(elements);
        caseInsensitiveSortedSet.addAll(elements);
        hashSet.addAll(elements);
        treeSet.addAll(elements);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        caseSensitiveSet.clear();
        caseInsensitiveSortedSet.clear();
        hashSet.clear();
        treeSet.clear();
    }

    static Stream<Arguments> simpleEqualityProvider() {
        CompactSet<String> set = new CompactSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        CompactSet<String> otherSame = new CompactSet<>();
        otherSame.addAll(Arrays.asList("a", "b", "c"));
        CompactSet<String> otherDiff = new CompactSet<>();
        otherDiff.addAll(Arrays.asList("a", "b", "x"));
        Set<String> hashSame = new HashSet<>(Arrays.asList("a", "b", "c"));
        Set<String> hashDiff = new HashSet<>(Arrays.asList("a", "b", "x"));
        Set<String> linkedHash = new LinkedHashSet<>(Arrays.asList("c", "b", "a"));
        Set<String> emptyHash = new HashSet<>();
        return Stream.of(Arguments.of("Reflexive – same instance", set, set, true), Arguments.of("Other CompactSet with same elements", set, otherSame, true), Arguments.of("Other CompactSet with different elements", set, otherDiff, false), Arguments.of("HashSet with same elements", set, hashSame, true), Arguments.of("HashSet with different elements", set, hashDiff, false), Arguments.of("null argument", set, null, false), Arguments.of("Non‑Set argument (String)", set, "some string", false), Arguments.of("LinkedHashSet with same elements, different order", set, linkedHash, true), Arguments.of("Empty HashSet vs empty CompactSet", new CompactSet<>(), emptyHash, true), Arguments.of("Empty CompactSet vs non‑empty HashSet", new CompactSet<>(), Collections.singleton("only"), false));
    }

    @Test
    void equalsSameInstance() {
        assertTrue(caseSensitiveSet.equals(caseSensitiveSet), "A set must be equal to itself");
    }

    @Test
    void equalsAnotherCompactSetSameElements() {
        CompactSet<String> other = new CompactSet<>();
        other.addAll(Arrays.asList("apple", "banana", "cherry"));
        assertTrue(caseSensitiveSet.equals(other), "Two CompactSet instances with identical elements should be equal");
        assertTrue(other.equals(caseSensitiveSet), "Equality must be symmetric");
    }

    @Test
    void equalsDifferentCompactSetConfigurationSameElements() {
        // caseInsensitiveSortedSet has the same elements but a different internal configuration
        assertTrue(caseSensitiveSet.equals(caseInsensitiveSortedSet), "CompactSet equality should depend only on element content, not configuration");
        assertTrue(caseInsensitiveSortedSet.equals(caseSensitiveSet), "Equality must be symmetric regardless of configuration");
    }

    @Test
    void equalsStandardJavaSetSameElements() {
        assertTrue(caseSensitiveSet.equals(hashSet), "CompactSet should be equal to a HashSet with the same elements");
        assertTrue(hashSet.equals(caseSensitiveSet), "Equality must be symmetric with standard Set implementations");
        assertTrue(caseSensitiveSet.equals(treeSet), "CompactSet should be equal to a TreeSet with the same elements");
        assertTrue(treeSet.equals(caseSensitiveSet), "Equality must be symmetric with TreeSet");
    }

    @Test
    void equalsDifferentElements() {
        Set<String> different = new HashSet<>(Arrays.asList("apple", "banana"));
        assertFalse(caseSensitiveSet.equals(different), "Sets with different element counts must not be equal");
        different.add("date");
        assertFalse(caseSensitiveSet.equals(different), "Sets with different element values must not be equal");
    }

    @Test
    void equalsNullAndNonSetObjects() {
        assertFalse(caseSensitiveSet.equals(null), "A set must not be equal to null");
        assertFalse(caseSensitiveSet.equals("not a set"), "A set must not be equal to an object of another type");
    }

    @Test
    void equalsWhenOtherSetHasDifferentOrdering() {
        // TreeSet naturally orders elements; CompactSet preserves insertion order.
        // Ordering should not affect equality.
        Set<String> shuffled = new LinkedHashSet<>(Arrays.asList("cherry", "banana", "apple"));
        assertTrue(caseSensitiveSet.equals(shuffled), "Equality must ignore ordering of elements");
    }

    @Test
    void equalsWhenBothSetsAreEmpty() {
        CompactSet<Integer> empty1 = new CompactSet<>();
        CompactSet<Integer> empty2 = new CompactSet<>();
        assertTrue(empty1.equals(empty2), "Two empty CompactSet instances should be equal");
        assertTrue(empty1.equals(new HashSet<>()), "Empty CompactSet should be equal to empty HashSet");
    }

    /**
     * Parameterized test covering the simple equality matrix (same elements,
     * different elements, null argument, non‑Set argument, order‑independent sets).
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("simpleEqualityProvider")
    void simpleEqualityTests(String description, CompactSet<String> subject, Object other, boolean expected) {
        assertEquals(expected, subject.equals(other), description);
    }

    @Test
    void equalsSetContainingNullElement() {
        CompactSet<String> setWithNull = new CompactSet<>();
        setWithNull.add(null);
        Set<String> hashWithNull = new HashSet<>();
        hashWithNull.add(null);
        assertTrue(setWithNull.equals(hashWithNull), "A CompactSet containing a single null should be equal to a HashSet with the same null element");
    }

    @Test
    void equalsSetContainingNullAndNonNullElements() {
        CompactSet<String> set = new CompactSet<>();
        set.add(null);
        set.add("a");
        Set<String> linked = new LinkedHashSet<>(Arrays.asList("a", null));
        assertTrue(set.equals(linked), "CompactSet with null and non‑null elements must be equal to a Set with the same mixture regardless of order");
    }

    @Test
    void equalsEmptySetAgainstCollectionsEmptySet() {
        CompactSet<String> empty = new CompactSet<>();
        assertTrue(empty.equals(Collections.emptySet()), "Empty CompactSet should be equal to Collections.emptySet()");
    }

    @Test
    void equalsLargeIntegerSet() {
        CompactSet<Integer> largeSet = new CompactSet<>();
        Set<Integer> hashSet = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            largeSet.add(i);
            hashSet.add(i);
        }
        assertTrue(largeSet.equals(hashSet), "CompactSet with 0‑99 must equal HashSet with same 100 integers");
    }

    @Test
    void equalsLargeIntegerSetMissingOneElement() {
        CompactSet<Integer> largeSet = new CompactSet<>();
        Set<Integer> hashSet = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            largeSet.add(i);
            if (i < 99) {
                // omit 99 from the HashSet
                hashSet.add(i);
            }
        }
        assertFalse(largeSet.equals(hashSet), "CompactSet with 100 elements must not equal HashSet missing one element");
    }

    @Test
    void equalsAgainstCustomSetThatAlwaysReturnsFalseFromEquals() {
        CompactSet<String> subject = new CompactSet<>();
        subject.addAll(Arrays.asList("a", "b", "c"));
        Set<String> alwaysFalse = new CustomSetAlwaysFalseEquals<>(Arrays.asList("a", "b", "c"));
        assertFalse(subject.equals(alwaysFalse), "When the other Set's equals() always returns false, CompactSet.equals() must also return false");
    }

    @Test
    void equalsAgainstCustomSetUsingReferenceEquality() {
        CompactSet<String> subject = new CompactSet<>();
        subject.addAll(Arrays.asList("a", "b", "c"));
        Set<String> refEquality = new CustomSetReferenceEquality<>(Arrays.asList("a", "b", "c"));
        // The custom set will only be equal to itself, not to the CompactSet
        assertFalse(subject.equals(refEquality), "If the other Set uses reference equality, CompactSet.equals() should return false");
    }

    @Test
    void equalsAgainstNonSetCollectionArrayList() {
        CompactSet<String> subject = new CompactSet<>();
        subject.addAll(Arrays.asList("a", "b", "c"));
        List<String> arrayList = Arrays.asList("a", "b", "c");
        assertFalse(subject.equals(arrayList), "A non‑Set collection (ArrayList) must not be considered equal to a CompactSet");
    }

    @Test
    void equalsAgainstTreeSetWithDifferentIterationOrder() {
        CompactSet<String> subject = new CompactSet<>();
        subject.addAll(Arrays.asList("a", "b", "c"));
        Set<String> tree = new TreeSet<>(Arrays.asList("c", "b", "a"));
        assertTrue(subject.equals(tree), "CompactSet equality is order‑independent; it should be equal to a TreeSet with same elements");
    }

    @Test
    void equalsPropagatesExceptionFromSize() {
        CompactSet<String> subject = new CompactSet<>();
        subject.addAll(Arrays.asList("a", "b", "c"));
        Set<String> throwing = new CustomSetThrowingOnSize<>(Arrays.asList("a", "b", "c"));
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> subject.equals(throwing), "Exception from the other Set's size() must propagate");
        assertEquals("size error", ex.getMessage());
    }

    @Test
    void equalsPropagatesExceptionFromIterator() {
        CompactSet<String> subject = new CompactSet<>();
        subject.addAll(Arrays.asList("a", "b", "c"));
        Set<String> throwing = new CustomSetThrowingOnIterator<>(Arrays.asList("a", "b", "c"));
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, () -> subject.equals(throwing), "Exception from the other Set's iterator() must propagate");
        assertEquals("iterator error", ex.getMessage());
    }

    @Test
    void equalsPropagatesExceptionFromContains() {
        CompactSet<String> subject = new CompactSet<>();
        subject.addAll(Arrays.asList("a", "b", "c"));
        Set<String> throwing = new CustomSetThrowingOnContains<>(Arrays.asList("a", "b", "c"));
        NullPointerException ex = assertThrows(NullPointerException.class, () -> subject.equals(throwing), "Exception from the other Set's contains() must propagate");
        assertEquals("contains error", ex.getMessage());
    }

    @Test
    void equalsPropagatesExceptionFromContainsAll() {
        CompactSet<String> subject = new CompactSet<>();
        subject.addAll(Arrays.asList("a", "b", "c"));
        Set<String> throwing = new CustomSetThrowingOnContainsAll<>(Arrays.asList("a", "b", "c"));
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> subject.equals(throwing), "Exception from the other Set's containsAll() must propagate");
        assertEquals("containsAll error", ex.getMessage());
    }
}
