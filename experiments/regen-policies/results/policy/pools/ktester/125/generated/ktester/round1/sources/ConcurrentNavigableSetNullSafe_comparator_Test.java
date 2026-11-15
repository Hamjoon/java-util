package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_comparator_Test {

    private NavigableSet<String> defaultSet;

    private NavigableSet<String> customSet;

    private Comparator<String> lengthComparator;

    @BeforeEach
    void setupBeforeEach() {
        // Default constructor – natural ordering, comparator() should be null.
        defaultSet = new ConcurrentNavigableSetNullSafe<>();
        // Comparator that orders strings by length (null is treated as length 0).
        lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        // Constructor with explicit comparator – comparator() should return the same instance.
        customSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination.
        defaultSet.clear();
        customSet.clear();
    }

    /**
     * Supplies a stream of arguments for {@link #comparatorReturnsProvidedInstance}.
     *
     * Each entry contains:
     * <ul>
     *   <li>a human‑readable description,</li>
     *   <li>the comparator instance to pass to the constructor,</li>
     *   <li>the expected concrete class of that comparator.</li>
     * </ul>
     */
    private static Stream<Arguments> comparatorProvider() {
        Comparator<Integer> reverseOrder = Comparator.reverseOrder();
        Comparator<Integer> naturalOrder = Comparator.naturalOrder();
        Comparator<String> lengthLambda = Comparator.comparingInt(String::length);
        Comparator<String> methodRef = Comparator.comparing(String::toLowerCase);
        return Stream.of(Arguments.of("Reverse‑order comparator for Integer", reverseOrder, reverseOrder.getClass()), Arguments.of("Natural‑order comparator for Integer", naturalOrder, naturalOrder.getClass()), Arguments.of("Lambda comparator (by length) for String", lengthLambda, lengthLambda.getClass()), Arguments.of("Method‑reference comparator (case‑insensitive) for String", methodRef, methodRef.getClass()));
    }

    @Test
    void comparatorReturnsNullWhenDefaultConstructorUsed() {
        // GIVEN – a set created via the default constructor (already prepared in @BeforeEach)
        // WHEN
        Comparator<? super String> returned = defaultSet.comparator();
        // THEN
        assertNull(returned, "Comparator should be null for the default (natural ordering) constructor");
    }

    @Test
    void comparatorReturnsProvidedInstance() {
        // GIVEN – a set built with the supplied comparator (type inference via raw types)
        @SuppressWarnings("unchecked")
        NavigableSet<Object> set = new ConcurrentNavigableSetNullSafe<>(suppliedComparator);
        // WHEN
        Comparator<?> returned = set.comparator();
        // THEN
        assertNotNull(returned, "Comparator must not be null when supplied via constructor");
        assertSame(suppliedComparator, returned, "Returned comparator must be the exact instance supplied to the constructor");
        assertEquals(expectedComparatorClass, returned.getClass(), "Returned comparator should be of the expected runtime class");
    }

    @Test
    void comparatorAffectsOrderingAsExpected() {
        // GIVEN – a set using the length‑based comparator defined in @BeforeEach
        // length 4
        customSet.add("kiwi");
        // length 5
        customSet.add("apple");
        // length 6
        customSet.add("banana");
        // length 0 (treated specially)
        customSet.add(null);
        // WHEN – iterate over the set
        Iterator<String> it = customSet.iterator();
        // THEN – order must follow the length‑based comparator
        assertEquals(null, it.next(), "Null (length 0) should be first");
        assertEquals("kiwi", it.next(), "Shortest non‑null string should follow");
        assertEquals("apple", it.next(), "Next shortest string");
        assertEquals("banana", it.next(), "Longest string");
        assertFalse(it.hasNext(), "No more elements expected");
    }

    @Test
    void comparatorReturnedCanBeUsedDirectlyForComparison() {
        // GIVEN
        Comparator<? super String> comp = customSet.comparator();
        // WHEN / THEN – verify the comparator's contract independently of the set
        assertEquals(0, comp.compare(null, null), "Comparator must treat two nulls as equal");
        assertTrue(comp.compare(null, "a") < 0, "Null (length 0) should be less than any non‑null string");
        assertTrue(comp.compare("abc", "de") > 0, "Longer string should be greater than shorter string");
        assertEquals(0, comp.compare("same", "test"), "Strings of equal length should compare as equal");
    }

    @Test
    void comparatorUnaffectedBySetModifications() {
        // GIVEN – a set with a reverse‑order comparator for Integer values
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        // Populate the set
        intSet.addAll(Arrays.asList(1, 3, 2, 5, 4));
        // Clear the set
        intSet.clear();
        // WHEN
        Comparator<? super Integer> returned = intSet.comparator();
        // THEN – the comparator instance must be unchanged
        assertSame(reverse, returned, "Comparator instance must remain the same after add/clear operations");
    }
}
