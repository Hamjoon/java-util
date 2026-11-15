package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_comparator_Test {

    // created with the no‑arg constructor
    private NavigableSet<String> defaultSet;

    // created with a user supplied comparator
    private NavigableSet<String> customSet;

    // comparator used for the custom set
    private Comparator<String> lengthComparator;

    /**
     * Simple comparator that reverses natural order of strings.
     */
    static class ReverseStringComparator implements Comparator<String> {

        @Override
        public int compare(String o1, String o2) {
            return o2.compareTo(o1);
        }
    }

    /**
     * Comparator that also implements {@link Serializable}.
     */
    static class SerializableComparator implements Comparator<Integer>, Serializable {

        @Override
        public int compare(Integer o1, Integer o2) {
            return o1.compareTo(o2);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // defaultSet uses natural ordering (originalComparator == null)
        defaultSet = new ConcurrentNavigableSetNullSafe<>();
        // comparator that orders strings by length, treating null as length 0
        lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        customSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination
        defaultSet.clear();
        customSet.clear();
    }

    private static Stream<Arguments> provideMutatingScenarios() {
        return Stream.of(Arguments.of("adding elements with reverse order comparator", Comparator.reverseOrder(), (Consumer<NavigableSet<?>>) s -> {
            s.add("c");
            s.add("a");
            s.add("b");
        }), Arguments.of("removing an existing element with natural order comparator", Comparator.naturalOrder(), (Consumer<NavigableSet<?>>) s -> {
            s.add(1);
            s.add(2);
            s.remove(1);
        }), Arguments.of("clearing the set with case‑insensitive string comparator", String.CASE_INSENSITIVE_ORDER, (Consumer<NavigableSet<?>>) s -> {
            s.add("Hello");
            s.add("world");
            s.clear();
        }));
    }

    @Test
    void defaultConstructor_returnsNullComparator() {
        // The no‑arg constructor should expose a null comparator
        assertNull(defaultSet.comparator(), "Comparator returned by the default constructor must be null");
    }

    @Test
    void comparatorConstructor_exposesSuppliedComparator() {
        // The comparator supplied to the constructor must be returned unchanged
        assertSame(lengthComparator, customSet.comparator(), "Comparator returned should be the same instance that was supplied");
    }

    @Test
    void comparatorAffectsOrdering() {
        // Verify that the comparator influences the iteration order
        // length 4
        customSet.add("kiwi");
        // length 5
        customSet.add("apple");
        // length 6
        customSet.add("banana");
        // length 0
        customSet.add(null);
        Iterator<String> it = customSet.iterator();
        // Expected order based on lengthComparator (null first, then by length)
        assertEquals(null, it.next(), "Null (length 0) should be first");
        assertEquals("kiwi", it.next(), "Shortest non‑null string should follow");
        assertEquals("apple", it.next(), "Next shortest string");
        assertEquals("banana", it.next(), "Longest string last");
        assertFalse(it.hasNext(), "No more elements expected");
    }

    @Test
    void comparatorReturnedIsConsistentAfterModifications() {
        // Ensure that after adding/removing elements the comparator reference does not change
        assertSame(lengthComparator, customSet.comparator(), "Comparator should remain the same before any modifications");
        customSet.add("a");
        customSet.add(null);
        customSet.remove("a");
        assertSame(lengthComparator, customSet.comparator(), "Comparator should still be the same after modifications");
    }

    @Test
    void comparatorRetrieval_withCustomNonNullComparator_returnsSameInstance() {
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(reverse);
        assertSame(reverse, set.comparator(), "The comparator() method must return the exact instance supplied to the constructor");
    }

    @Test
    void comparatorRetrieval_withNullComparator_returnsNull() {
        // Using the constructor that accepts a null comparator (behaves like default)
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>(null);
        assertNull(set.comparator(), "When a null comparator is supplied, comparator() must return null");
    }

    @Test
    void comparatorIdentity_lambdaComparator_isSameInstance() {
        Comparator<String> lambda = (s1, s2) -> {
            int len1 = s1 == null ? 0 : s1.length();
            int len2 = s2 == null ? 0 : s2.length();
            return Integer.compare(len1, len2);
        };
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(lambda);
        assertSame(lambda, set.comparator(), "Lambda comparator instance must be returned unchanged");
    }

    @Test
    void comparatorIdentity_methodReferenceComparator_isSameInstance() {
        Comparator<Integer> methodRef = Integer::compare;
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>(methodRef);
        assertSame(methodRef, set.comparator(), "Method‑reference comparator instance must be returned unchanged");
    }

    @Test
    void comparatorIdentity_customClassComparator_isSameInstance() {
        Comparator<String> reverse = new ReverseStringComparator();
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(reverse);
        assertSame(reverse, set.comparator(), "Custom class comparator instance must be returned unchanged");
    }

    @Test
    void comparatorIdentity_serializableComparator_isSameInstance() {
        Comparator<Integer> serializable = new SerializableComparator();
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>(serializable);
        assertSame(serializable, set.comparator(), "Serializable comparator instance must be returned unchanged");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideMutatingScenarios")
    void comparatorRemainsSameAfterMutations(String description, Comparator<?> comparator, Consumer<NavigableSet<?>> mutator) {
        NavigableSet<?> set = new ConcurrentNavigableSetNullSafe<>(comparator);
        // Apply the supplied mutation (add, remove, clear, etc.)
        mutator.accept(set);
        // The comparator reference must stay identical
        assertSame(comparator, set.comparator(), "Comparator reference must remain unchanged after " + description);
    }

    @Test
    void comparatorIsNullWhenSetIsEmptyAndNaturalOrdering() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        assertNull(set.comparator(), "Empty set with natural ordering should report a null comparator");
    }

    @Test
    void comparatorRemainsNullWhenSetIsPopulatedAndNaturalOrdering() {
        NavigableSet<Integer> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(10);
        set.add(5);
        set.add(20);
        assertNull(set.comparator(), "Populated set with natural ordering must still report a null comparator");
    }
}
