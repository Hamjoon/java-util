package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_tailSet_2_Test {

    private ConcurrentNavigableSetNullSafe<String> naturalSet;

    private ConcurrentNavigableSetNullSafe<String> lengthComparatorSet;

    @BeforeEach
    void setupBeforeEach() {
        // Set that uses natural ordering (String implements Comparable)
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        // Set that orders strings by their length (null is treated as length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthComparatorSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a clean set
        if (naturalSet != null)
            naturalSet.clear();
        if (lengthComparatorSet != null)
            lengthComparatorSet.clear();
    }

    /**
     * Provides arguments for the basic‑behavior parameterized test.
     * Each argument consists of:
     *   - the {@code fromElement} to pass to {@code tailSet}
     *   - the expected view as a {@code List<Integer>}
     */
    private static Stream<Arguments> basicBehaviorProvider() {
        return Stream.of(Arguments.of(2, List.of(2, 3, 4, 5)), Arguments.of(3, List.of(3, 4, 5)), Arguments.of(0, List.of(1, 2, 3, 4, 5)), // greater than any element → empty view
        Arguments.of(6, List.of()));
    }

    @Test
    void tailSet_NaturalOrdering_Inclusive() {
        // given
        naturalSet.add("apple");
        naturalSet.add("banana");
        naturalSet.add("cherry");
        // null is considered greater than any non‑null element
        naturalSet.add(null);
        // when
        NavigableSet<String> tail = naturalSet.tailSet("banana");
        // then
        assertNotNull(tail);
        assertEquals(3, tail.size());
        Iterator<String> it = tail.iterator();
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertEquals(null, it.next());
        assertFalse(it.hasNext());
        // verify that the original set is unchanged
        assertEquals(4, naturalSet.size());
    }

    @Test
    void tailSet_NaturalOrdering_FromNull() {
        // given
        naturalSet.add("apple");
        naturalSet.add(null);
        // when
        NavigableSet<String> tail = naturalSet.tailSet(null);
        // then
        assertNotNull(tail);
        assertEquals(1, tail.size());
        assertTrue(tail.contains(null));
        assertFalse(tail.contains("apple"));
    }

    @Test
    void tailSet_NaturalOrdering_EmptySet() {
        // when
        NavigableSet<String> tail = naturalSet.tailSet("any");
        // then
        assertNotNull(tail);
        assertTrue(tail.isEmpty());
    }

    @Test
    void tailSet_WithLengthComparator_Inclusive() {
        // given
        // length 4
        lengthComparatorSet.add("kiwi");
        // length 5
        lengthComparatorSet.add("apple");
        // length 6
        lengthComparatorSet.add("banana");
        // length 0 (treated as smallest)
        lengthComparatorSet.add(null);
        // when
        NavigableSet<String> tail = lengthComparatorSet.tailSet("apple");
        // then
        assertNotNull(tail);
        assertEquals(2, tail.size());
        Iterator<String> it = tail.iterator();
        // length 5
        assertEquals("apple", it.next());
        // length 6
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
        // verify that null and shorter elements are not present
        assertFalse(tail.contains(null));
        assertFalse(tail.contains("kiwi"));
    }

    @Test
    void tailSet_WithLengthComparator_FromNull() {
        // given
        lengthComparatorSet.add(null);
        lengthComparatorSet.add("apple");
        // when
        NavigableSet<String> tail = lengthComparatorSet.tailSet(null);
        // then
        // null is the smallest element according to the length comparator,
        // therefore the tail set should contain *all* elements.
        assertNotNull(tail);
        assertEquals(2, tail.size());
        assertTrue(tail.contains(null));
        assertTrue(tail.contains("apple"));
    }

    @ParameterizedTest(name = "tailSet({0}) should return {1}")
    @MethodSource("basicBehaviorProvider")
    void tailSet_Integer_BasicBehavior(Integer fromElement, List<Integer> expected) {
        // given – a set containing 1..5
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 1; i <= 5; i++) {
            intSet.add(i);
        }
        // when
        SortedSet<Integer> tail = intSet.tailSet(fromElement);
        // then
        assertNotNull(tail);
        assertEquals(expected.size(), tail.size());
        assertEquals(expected, new ArrayList<>(tail));
        // original set must stay unchanged
        assertEquals(5, intSet.size());
    }

    @Test
    void tailSet_Integer_Edge_FirstElement() {
        // given
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 1; i <= 5; i++) intSet.add(i);
        // when
        SortedSet<Integer> tail = intSet.tailSet(1);
        // then – full set should be returned
        assertEquals(List.of(1, 2, 3, 4, 5), new ArrayList<>(tail));
    }

    @Test
    void tailSet_Integer_Edge_LastElement() {
        // given
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 1; i <= 5; i++) intSet.add(i);
        // when
        SortedSet<Integer> tail = intSet.tailSet(5);
        // then – only the greatest element should be present
        assertEquals(List.of(5), new ArrayList<>(tail));
    }

    @Test
    void tailSet_Integer_NullElement_ThrowsNPE() {
        // given
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        for (int i = 1; i <= 5; i++) intSet.add(i);
        // when / then
        assertThrows(NullPointerException.class, () -> intSet.tailSet(null));
    }

    @Test
    void tailSet_Integer_WrongType_String_ThrowsClassCastException() {
        // given – a set of Integers
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.add(1);
        intSet.add(2);
        intSet.add(3);
        // when / then
        assertThrows(ClassCastException.class, () -> intSet.tailSet("string"));
    }

    @Test
    void tailSet_Integer_WrongType_Object_ThrowsClassCastException() {
        // given – a set of Integers
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        intSet.add(1);
        intSet.add(2);
        intSet.add(3);
        // when / then
        assertThrows(ClassCastException.class, () -> intSet.tailSet(new Object()));
    }

    @Test
    void tailSet_Integer_CustomComparator_IllegalArgumentExceptionPropagated() {
        // Comparator that throws IllegalArgumentException when comparing the value 5
        Comparator<Integer> throwingComparator = (a, b) -> {
            if (a != null && a == 5)
                throw new IllegalArgumentException("value 5 not allowed");
            if (b != null && b == 5)
                throw new IllegalArgumentException("value 5 not allowed");
            return Integer.compare(a, b);
        };
        // given – set constructed with the throwing comparator and populated with values 1..5
        ConcurrentNavigableSetNullSafe<Integer> intSet = new ConcurrentNavigableSetNullSafe<>(throwingComparator);
        for (int i = 1; i <= 5; i++) {
            intSet.add(i);
        }
        // when / then – calling tailSet(5) should propagate the IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> intSet.tailSet(5));
    }
}
