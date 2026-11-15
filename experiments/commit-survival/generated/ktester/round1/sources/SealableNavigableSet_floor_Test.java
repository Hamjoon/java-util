package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_floor_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    /* --------------------------------------------------------------
     *  Edge cases – single element, null argument, type mismatch
     * -------------------------------------------------------------- */
    @Nested
    class EdgeCases {

        @Test
        void floorSingleElementExactMatch() {
            SealableNavigableSet<Integer> single = new SealableNavigableSet<>(sealedSupplier);
            single.add(1);
            assertEquals(1, single.floor(1));
        }

        @Test
        void floorSingleElementGreaterThanElement() {
            SealableNavigableSet<Integer> single = new SealableNavigableSet<>(sealedSupplier);
            single.add(1);
            assertEquals(1, single.floor(2));
        }

        @Test
        void floorSingleElementLessThanElement() {
            SealableNavigableSet<Integer> single = new SealableNavigableSet<>(sealedSupplier);
            single.add(1);
            assertNull(single.floor(0));
        }

        @Test
        void floorNullArgumentThrowsNPE() {
            // The backing TreeSet does not permit null keys for natural ordering
            assertThrows(NullPointerException.class, () -> set.floor(null));
        }

        @Test
        void floorWrongTypeThrowsClassCastException() {
            // The set contains Integers, passing a String should cause ClassCastException
            assertThrows(ClassCastException.class, () -> set.floor("b"));
        }
    }

    /* --------------------------------------------------------------
     *  String set with natural ordering
     * -------------------------------------------------------------- */
    @Nested
    class StringSetTests {

        private SealableNavigableSet<String> stringSet;

        @BeforeEach
        void initStringSet() {
            // Supplier not used for read‑only checks in these tests
            Supplier<Boolean> dummySupplier = () -> false;
            stringSet = new SealableNavigableSet<>(dummySupplier);
            stringSet.add("a");
            stringSet.add("c");
            stringSet.add("k");
            stringSet.add("z");
        }

        @Test
        void floorStringExactMatch() {
            assertEquals("a", stringSet.floor("a"));
        }

        @Test
        void floorStringBetweenElements() {
            assertEquals("k", stringSet.floor("m"));
        }

        @Test
        void floorStringBelowSmallest() {
            assertNull(stringSet.floor("0"));
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set for each test and populate it with a known ordering
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the sealed flag is reset for the next test
        sealedState = false;
    }

    @Test
    void floorExactMatch() {
        Integer result = set.floor(20);
        assertEquals(20, result, "floor should return the element itself when it exists");
    }

    @Test
    void floorNonExactMatch() {
        // 25 is not present; the greatest element <= 25 is 20
        Integer result = set.floor(25);
        assertEquals(20, result, "floor should return the greatest element less than the argument");
    }

    @Test
    void floorBelowLowestElement() {
        // No element <= 5, expect null
        Integer result = set.floor(5);
        assertNull(result, "floor should return null when no element is less than or equal to the argument");
    }

    @Test
    void floorWhenSealed() {
        // The set is sealed (read‑only) but floor is a read operation and must succeed
        sealedState = true;
        Integer result = set.floor(25);
        assertEquals(20, result, "floor should work even when the set is sealed");
    }

    @Test
    void floorWithCustomComparator() {
        // Reverse order comparator: ordering is 30,20,10
        Comparator<Integer> reverse = Comparator.reverseOrder();
        SealableNavigableSet<Integer> revSet = new SealableNavigableSet<>(reverse, sealedSupplier);
        revSet.add(10);
        revSet.add(20);
        revSet.add(30);
        // In reverse order, the greatest element <= 25 is 30
        Integer result = revSet.floor(25);
        assertEquals(30, result, "floor should respect the custom comparator ordering");
    }

    @Test
    void floorWithNullArgument() {
        // According to NavigableSet contract, floor(null) returns null for natural order sets
        // (or throws NPE for sets that do not permit null). Our underlying set permits null,
        // but we have not added any null element, so result should be null.
        Integer result = set.floor(null);
        assertNull(result, "floor(null) should return null when no null element is present");
    }

    /* --------------------------------------------------------------
     *  Basic natural‑ordering behavior
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "floor({0}) should return {1}")
    @CsvSource({ // exact match
    "5,5", // lower element exists
    "4,3", // below smallest element
    "0,null" })
    void floorBasicBehavior(Integer input, String expectedStr) {
        Integer expected = "null".equals(expectedStr) ? null : Integer.valueOf(expectedStr);
        Integer result = set.floor(input);
        assertEquals(expected, result);
    }

    @Test
    void floorWhenSetIsEmpty() {
        // Create an empty SealableNavigableSet
        SealableNavigableSet<Integer> empty = new SealableNavigableSet<>(sealedSupplier);
        assertNull(empty.floor(10), "floor on an empty set must return null");
    }

    /* --------------------------------------------------------------
     *  Negative numbers set
     * -------------------------------------------------------------- */
    @Test
    void floorWithNegativeNumbers() {
        SealableNavigableSet<Integer> negatives = new SealableNavigableSet<>(sealedSupplier);
        negatives.add(-5);
        negatives.add(-3);
        negatives.add(-1);
        assertEquals(-3, negatives.floor(-2));
    }

    /* --------------------------------------------------------------
     *  Reverse order comparator tests
     * -------------------------------------------------------------- */
    @Test
    void floorWithReverseComparatorExactMatch() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        SealableNavigableSet<Integer> revSet = new SealableNavigableSet<>(reverse, sealedSupplier);
        revSet.add(5);
        revSet.add(3);
        revSet.add(1);
        // In reverse order, floor(5) returns 5 (greatest ≤ 5 in reverse order)
        assertEquals(5, revSet.floor(5));
    }

    @Test
    void floorWithReverseComparatorNonExact() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        SealableNavigableSet<Integer> revSet = new SealableNavigableSet<>(reverse, sealedSupplier);
        revSet.add(5);
        revSet.add(3);
        revSet.add(1);
        // In reverse order, floor(2) should return 3 (the smallest element that is >= 2 in natural order)
        assertEquals(3, revSet.floor(2));
    }

    /* --------------------------------------------------------------
     *  Null underlying navSet – use reflection to inject null
     * -------------------------------------------------------------- */
    @Test
    void floorWhenUnderlyingNavSetIsNull() throws Exception {
        SealableNavigableSet<Integer> faulty = new SealableNavigableSet<>(sealedSupplier);
        // Force the private field navSet to null via reflection
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        navSetField.set(faulty, null);
        assertThrows(NullPointerException.class, () -> faulty.floor(1));
    }

    /* --------------------------------------------------------------
     *  Comparator that deliberately throws IllegalArgumentException
     * -------------------------------------------------------------- */
    @Test
    void floorComparatorThrowsIllegalArgumentException() {
        Comparator<Integer> throwingComparator = (a, b) -> {
            if (Objects.equals(a, 0) || Objects.equals(b, 0)) {
                throw new IllegalArgumentException("Zero not allowed");
            }
            return Integer.compare(a, b);
        };
        SealableNavigableSet<Integer> setWithBadComparator = new SealableNavigableSet<>(throwingComparator, sealedSupplier);
        setWithBadComparator.add(1);
        setWithBadComparator.add(2);
        // floor(0) should propagate the IllegalArgumentException from the comparator
        assertThrows(IllegalArgumentException.class, () -> setWithBadComparator.floor(0));
    }

    /* --------------------------------------------------------------
     *  Underlying NavigableSet that throws UnsupportedOperationException
     * -------------------------------------------------------------- */
    @Test
    void floorUnderlyingSetThrowsUnsupportedOperationException() throws Exception {
        // Mock a NavigableSet whose floor method throws UOE
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> mockNav = mock(NavigableSet.class);
        when(mockNav.floor(anyInt())).thenThrow(new UnsupportedOperationException("not supported"));
        // Create a SealableNavigableSet wrapping the mock via reflection
        SealableNavigableSet<Integer> wrapper = new SealableNavigableSet<>(sealedSupplier);
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        navSetField.set(wrapper, mockNav);
        // The call must propagate the UnsupportedOperationException unchanged
        assertThrows(UnsupportedOperationException.class, () -> wrapper.floor(5));
    }
}
