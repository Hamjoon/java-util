package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_floor_Test {

    private NavigableSet<String> set;

    private NavigableSet<Integer> intSet;

    private NavigableSet<String> strSet;

    class Uncomparable {

        final int id;

        Uncomparable(int id) {
            this.id = id;
        }
    }

    class Uncomparable {

        final int id;

        Uncomparable(int id) {
            this.id = id;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Default (natural‑ordering) constructors are used for most tests.
        intSet = new ConcurrentNavigableSetNullSafe<>();
        strSet = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared between tests to avoid cross‑test pollution.
        if (intSet != null) {
            intSet.clear();
        }
        if (strSet != null) {
            strSet.clear();
        }
    }

    private static Stream<Arguments> emptySetProvider() {
        return Stream.of(// floor(null) on empty set
        Arguments.of(null, null), // floor(non‑null) on empty set
        Arguments.of(42, null));
    }

    private static Stream<Arguments> nullAndElementsProvider() {
        return Stream.of(// floor(null) -> null
        Arguments.of(null, null), // 2 < 3, greatest ≤2 is masked null -> null
        Arguments.of(2, null), // between 3 and 5
        Arguments.of(4, 3), // greater than all non‑null
        Arguments.of(6, 5));
    }

    /* --------------------------------------------------------------
     *  Tests for floor(E) – natural ordering
     * -------------------------------------------------------------- */
    @Test
    void floorReturnsExactMatchWhenElementExists() {
        // given
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        // null is allowed
        set.add(null);
        // when
        String result = set.floor("banana");
        // then
        assertEquals("banana", result);
    }

    @Test
    void floorReturnsGreatestElementLessThanOrEqualToKey() {
        // given
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        set.add(null);
        // when
        // "blue" is between "banana" and "cherry"
        String result = set.floor("blue");
        // then
        assertEquals("banana", result);
    }

    @Test
    void floorReturnsNullWhenKeyIsSmallerThanAllNonNullElements() {
        // given
        set.add("apple");
        set.add("banana");
        set.add(null);
        // when
        // smaller than "apple"
        String result = set.floor("aardvark");
        // then
        // The sentinel representing null is considered the smallest element.
        assertNull(result);
    }

    @Test
    void floorHandlesNullKeyCorrectly() {
        // given
        set.add("apple");
        set.add(null);
        // when
        String result = set.floor(null);
        // then
        assertNull(result);
    }

    @Test
    void floorOnEmptySetReturnsNull() {
        // given – set is empty (created in @BeforeEach)
        // when
        String result = set.floor("any");
        // then
        assertNull(result);
    }

    /* --------------------------------------------------------------
     *  Tests for floor(E) – custom comparator (by string length)
     * -------------------------------------------------------------- */
    @Test
    void floorWithLengthComparator() {
        // given – a set that orders by string length, null is treated as length 0
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        lengthSet.add("kiwi");
        // length 5
        lengthSet.add("apple");
        // length 6
        lengthSet.add("banana");
        // length 0
        lengthSet.add(null);
        // when – exact match
        String exact = lengthSet.floor("apple");
        // when – same length as an existing element
        // length 5, should return "apple"
        String sameLength = lengthSet.floor("peach");
        // when – smaller than any non‑null element
        // length 1, should return null
        String smaller = lengthSet.floor("a");
        // when – null key
        String nullKey = lengthSet.floor(null);
        // then
        assertEquals("apple", exact);
        assertEquals("apple", sameLength);
        assertNull(smaller);
        assertNull(nullKey);
    }

    /* --------------------------------------------------------------
     *  Group: floor - empty set
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "floor(emptySet, e={0}) => {1}")
    @MethodSource("emptySetProvider")
    void floorEmptySet(Integer e, Integer expected) {
        // set is empty (created in @BeforeEach)
        Integer result = intSet.floor(e);
        assertEquals(expected, result);
    }

    /* --------------------------------------------------------------
     *  Group: floor - set contains null element
     * -------------------------------------------------------------- */
    @Test
    void floorSetContainsOnlyNull() {
        // only null element
        intSet.add(null);
        // floor(null) should return null (unmasked)
        assertNull(intSet.floor(null));
        // floor(10) should also return null because null is considered less than any non‑null
        assertNull(intSet.floor(10));
    }

    /* --------------------------------------------------------------
     *  Group: floor - normal ordering with existing element
     * -------------------------------------------------------------- */
    @Test
    void floorExactMatch() {
        intSet.addAll(Arrays.asList(1, 3, 5));
        assertEquals(3, intSet.floor(3));
    }

    /* --------------------------------------------------------------
     *  Group: floor - normal ordering with lower element
     * -------------------------------------------------------------- */
    @Test
    void floorLowerElement() {
        intSet.addAll(Arrays.asList(1, 3, 5));
        assertEquals(3, intSet.floor(4));
    }

    /* --------------------------------------------------------------
     *  Group: floor - normal ordering with no lower element
     * -------------------------------------------------------------- */
    @Test
    void floorNoLowerElement() {
        intSet.addAll(Arrays.asList(1, 3, 5));
        assertNull(intSet.floor(0));
    }

    /* --------------------------------------------------------------
     *  Group: floor - custom comparator (reverse order)
     * -------------------------------------------------------------- */
    @Test
    void floorReverseOrderComparator() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableSet<Integer> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        // natural order is 1,3,5 but comparator reverses it
        revSet.addAll(Arrays.asList(5, 3, 1));
        // In reversed ordering, floor(4) should be 5 (the greatest element according to the comparator)
        assertEquals(5, revSet.floor(4));
        // floor(null) must return null after masking/unmasking
        assertNull(revSet.floor(null));
    }

    /* --------------------------------------------------------------
     *  Group: floor - set contains null and other elements
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "floor({0}) on set{null,3,5} => {1}")
    @MethodSource("nullAndElementsProvider")
    void floorSetWithNullAndElements(Integer e, Integer expected) {
        intSet.addAll(Arrays.asList(null, 3, 5));
        assertEquals(expected, intSet.floor(e));
    }

    /* --------------------------------------------------------------
     *  Group: floor - element greater than all elements (no null)
     * -------------------------------------------------------------- */
    @Test
    void floorGreaterThanAllNoNull() {
        intSet.addAll(Arrays.asList(1, 3, 5));
        assertEquals(5, intSet.floor(10));
    }

    /* --------------------------------------------------------------
     *  Group: floor - element equal to smallest non‑null with null present
     * -------------------------------------------------------------- */
    @Test
    void floorEqualToSmallestNonNullWithNullPresent() {
        intSet.addAll(Arrays.asList(null, 3, 5));
        assertEquals(3, intSet.floor(3));
    }

    /* --------------------------------------------------------------
     *  Group: floor - custom comparator (case‑insensitive string ordering)
     * -------------------------------------------------------------- */
    @Test
    void floorCaseInsensitiveStringComparator() {
        Comparator<String> ci = String.CASE_INSENSITIVE_ORDER;
        NavigableSet<String> ciSet = new ConcurrentNavigableSetNullSafe<>(ci);
        ciSet.addAll(Arrays.asList("Apple", "banana", "Cherry"));
        ciSet.add(null);
        // "blue" (case‑insensitive) falls between "banana" and "Cherry"
        assertEquals("banana", ciSet.floor("blue"));
        // floor(null) should return null
        assertNull(ciSet.floor(null));
    }

    /* --------------------------------------------------------------
     *  Group: floor - reverse order comparator with null present
     * -------------------------------------------------------------- */
    @Test
    void floorReverseOrderWithNullPresent() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        NavigableSet<Integer> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        revSet.addAll(Arrays.asList(null, 5, 3, 1));
        // In reversed order the logical sequence is 5 > 3 > 1 > null
        assertEquals(1, revSet.floor(2));
        assertNull(revSet.floor(null));
    }

    /* --------------------------------------------------------------
     *  Group: floor - incompatible element type
     * -------------------------------------------------------------- */
    @Test
    void floorIncompatibleElementType() {
        NavigableSet<Integer> intOnly = new ConcurrentNavigableSetNullSafe<>();
        intOnly.addAll(Arrays.asList(1, 2, 3));
        // Passing a String should cause ClassCastException from the underlying set
        assertThrows(ClassCastException.class, () -> intOnly.floor((Integer) (Object) "string"));
    }

    /* --------------------------------------------------------------
     *  Group: floor - custom comparator that throws
     * -------------------------------------------------------------- */
    @Test
    void floorComparatorThrowsRuntimeException() {
        Comparator<Integer> throwingComparator = (a, b) -> {
            throw new RuntimeException("Comparator failure");
        };
        NavigableSet<Integer> throwingSet = new ConcurrentNavigableSetNullSafe<>(throwingComparator);
        throwingSet.addAll(Arrays.asList(1, 2, 3));
        // floor triggers a comparison, which should propagate the RuntimeException
        RuntimeException ex = assertThrows(RuntimeException.class, () -> throwingSet.floor(5));
        assertEquals("Comparator failure", ex.getMessage());
    }

    /* --------------------------------------------------------------
     *  Group: floor - internal set is null (defensive check)
     * -------------------------------------------------------------- */
    @Test
    void floorInternalSetNullThrowsNPE() throws Exception {
        // Create a normal set and then use reflection to nullify the private field.
        ConcurrentNavigableSetNullSafe<Integer> unsafe = new ConcurrentNavigableSetNullSafe<>();
        Field internal = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internal.setAccessible(true);
        internal.set(unsafe, null);
        // The call should now throw NullPointerException.
        assertThrows(NullPointerException.class, () -> unsafe.floor(null));
    }

    /* --------------------------------------------------------------
     *  Group: floor - element not comparable and no comparator
     * -------------------------------------------------------------- */
    @Test
    void floorNonComparableElementWithoutComparator() {
        // Use raw type to avoid compile‑time generic checks.
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe<Object> rawSet = new ConcurrentNavigableSetNullSafe<>();
        // Custom class without Comparable
        class Uncomparable {

            final int id;

            Uncomparable(int id) {
                this.id = id;
            }
        }
        Uncomparable a = new Uncomparable(1);
        Uncomparable b = new Uncomparable(2);
        rawSet.add(a);
        // floor on a different uncomparable instance should trigger ClassCastException
        assertThrows(ClassCastException.class, () -> rawSet.floor(b));
    }
}
