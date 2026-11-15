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
class ConcurrentNavigableSetNullSafe_first_Test {

    private ConcurrentNavigableSetNullSafe<String> naturalSet;

    private ConcurrentNavigableSetNullSafe<String> lengthComparatorSet;

    @BeforeEach
    void setupBeforeEach() {
        // Set used for natural ordering tests
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        naturalSet.add("apple");
        naturalSet.add("banana");
        naturalSet.add("cherry");
        // null is allowed and should be masked internally
        naturalSet.add(null);
        // Set used for comparator‑based ordering tests (by string length)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthComparatorSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        lengthComparatorSet.add("kiwi");
        // length 5
        lengthComparatorSet.add("apple");
        // length 6
        lengthComparatorSet.add("banana");
        // length 0, should be first according to comparator
        lengthComparatorSet.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination
        if (naturalSet != null) {
            naturalSet.clear();
        }
        if (lengthComparatorSet != null) {
            lengthComparatorSet.clear();
        }
    }

    /**
     * Supplies natural‑ordering test data for {@code first()}.
     * Each entry consists of a set factory, elements to add and the expected first element.
     */
    private static Stream<Arguments> naturalOrderProvider() {
        return Stream.of(// Integer set: [1,2,3] → first = 1
        Arguments.of((Supplier<ConcurrentNavigableSetNullSafe<Integer>>) ConcurrentNavigableSetNullSafe::new, List.of(1, 2, 3), 1), // String set: ["banana","apple","cherry"] → first = "apple"
        Arguments.of((Supplier<ConcurrentNavigableSetNullSafe<String>>) ConcurrentNavigableSetNullSafe::new, List.of("banana", "apple", "cherry"), "apple"));
    }

    @Test
    void testFirstNaturalOrder() {
        // In natural ordering null is considered greater than any non‑null element
        // (as demonstrated by iterator order in existing tests)
        String first = naturalSet.first();
        assertEquals("apple", first, "First element should be the lowest natural order element");
    }

    @Test
    void testFirstWithComparatorWhereNullIsLowest() {
        // Comparator orders by string length; null is treated as length 0 and therefore lowest
        String first = lengthComparatorSet.first();
        assertNull(first, "First element should be null when comparator treats null as the smallest");
    }

    @Test
    void testFirstOnEmptySetThrowsException() {
        ConcurrentNavigableSetNullSafe<Integer> emptySet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(emptySet.isEmpty(), "Set should be empty");
        assertThrows(NoSuchElementException.class, emptySet::first, "Calling first() on an empty set must throw NoSuchElementException");
    }

    @ParameterizedTest(name = "[{index}] natural order first() should return {2}")
    @MethodSource("naturalOrderProvider")
    <E> void testFirstNaturalOrderParameterized(Supplier<ConcurrentNavigableSetNullSafe<E>> setSupplier, List<E> elements, E expectedFirst) {
        ConcurrentNavigableSetNullSafe<E> set = setSupplier.get();
        elements.forEach(set::add);
        assertEquals(expectedFirst, set.first(), () -> "Expected first element to be " + expectedFirst);
    }

    @Test
    void testFirstSetContainingOnlyNullReturnsNull() {
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(null);
        assertNull(set.first(), "first() must return null when the set contains only a null element");
    }

    @Test
    void testFirstWithNullAndNonNullWhenNullIsLowest() {
        // Comparator that places null before any non‑null value
        Comparator<String> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(nullsFirst);
        set.add("beta");
        set.add(null);
        assertNull(set.first(), "When nullsFirst comparator is used, first() should return null");
    }

    @Test
    void testFirstWithDescendingComparator() {
        // Reverse natural order – largest element becomes the “first”
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>(reverse);
        set.add(1);
        set.add(2);
        set.add(3);
        assertEquals(3, set.first(), "With a descending comparator, first() should return the maximum element");
    }

    @Test
    void testFirstWithNullsLastComparator() {
        // Nulls are placed after all non‑null values
        Comparator<Integer> nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
        ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>(nullsLast);
        set.add(null);
        set.add(5);
        assertEquals(5, set.first(), "With nullsLast comparator, first() should ignore null and return 5");
    }

    @Test
    void testFirstThrowsClassCastExceptionWithIncompatibleNaturalElements() {
        ConcurrentNavigableSetNullSafe<Object> set = new ConcurrentNavigableSetNullSafe<>();
        // Integer
        set.add(1);
        // String – not comparable with Integer under natural ordering
        set.add("a");
        assertThrows(ClassCastException.class, set::first, "first() should propagate ClassCastException when elements are not mutually comparable");
    }

    @Test
    void testFirstThrowsClassCastExceptionWithFaultyCustomComparator() {
        // Comparator that blindly casts to Integer – will fail for non‑Integer objects
        Comparator<Object> unsafeComparator = (o1, o2) -> Integer.compare((Integer) o1, (Integer) o2);
        ConcurrentNavigableSetNullSafe<Object> set = new ConcurrentNavigableSetNullSafe<>(unsafeComparator);
        // Integer
        set.add(1);
        // String – cast will fail
        set.add("b");
        assertThrows(ClassCastException.class, set::first, "first() should propagate ClassCastException when custom comparator cannot handle element types");
    }
}
