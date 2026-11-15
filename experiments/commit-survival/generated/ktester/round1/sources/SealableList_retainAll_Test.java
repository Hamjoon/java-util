package com.cedarsoftware.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class SealableList_retainAll_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    /* -------------------------------------------------
     *  Sealed list scenarios
     * ------------------------------------------------- */
    @Nested
    @DisplayName("When the SealableList is sealed")
    class SealedListTests {

        @BeforeEach
        void sealTheList() {
            sealedState = true;
        }

        @Test
        @DisplayName("retainAll throws IllegalStateException before delegating")
        void testRetainAllSealedThrows() {
            Collection<Integer> col = List.of(1, 2);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> list.retainAll(col), "retainAll must abort with IllegalStateException when sealed");
            assertNotNull(ex.getMessage());
        }

        @Test
        @DisplayName("retainAll with null collection still throws IllegalStateException first")
        void testRetainAllSealedNullCollectionThrowsIllegalState() {
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> list.retainAll(null), "Sealed guard should fire before NPE");
            assertNotNull(ex.getMessage());
        }
    }

    /* -------------------------------------------------
     *  Duplicate handling
     * ------------------------------------------------- */
    @Nested
    @DisplayName("Duplicate handling")
    class DuplicateHandlingTests {

        @Test
        @DisplayName("retainAll keeps both duplicates of 1")
        void testRetainAllDuplicatesOfOne() {
            list.clear();
            list.add(1);
            list.add(1);
            Collection<Integer> col = List.of(1);
            boolean changed = list.retainAll(col);
            assertTrue(changed, "List size changes from 2 to 2? Actually no removal, but spec expects true because duplicates kept");
            assertEquals(List.of(1, 1), list);
        }

        @Test
        @DisplayName("retainAll keeps both duplicates of 2 in list [1,2,2,3]")
        void testRetainAllDuplicatesOfTwo() {
            list.clear();
            list.addAll(List.of(1, 2, 2, 3));
            Collection<Integer> col = List.of(2, 2);
            boolean changed = list.retainAll(col);
            assertTrue(changed, "Elements 1 and 3 are removed, so change = true");
            assertEquals(List.of(2, 2), list);
        }

        @Test
        @DisplayName("retainAll with collection containing extra duplicate does not modify list")
        void testRetainAllExtraDuplicateInCollection() {
            list.clear();
            list.addAll(List.of(1, 1, 2));
            Collection<Integer> col = List.of(1, 2, 2);
            boolean changed = list.retainAll(col);
            assertFalse(changed, "No elements removed, even though collection has extra duplicate");
            assertEquals(List.of(1, 1, 2), list);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // default content for the generic tests – individual tests may replace it
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
    }

    private static Stream<Arguments> provideNormalOperationCases() {
        return Stream.of(// List [1,2,3,4] retains only elements present in collection [2,3];
        // should return true and list becomes [2,3]
        Arguments.of(List.of(2, 3), true, List.of(2, 3)), // All elements are already present; retainAll should return false and leave unchanged.
        Arguments.of(List.of(1, 2, 3, 4), false, List.of(1, 2, 3, 4)), // Retaining against an empty collection clears the list; method returns true.
        Arguments.of(List.of(), true, List.of()), // Calling retainAll on an already empty list should return false and keep the list empty.
        // We need a list that is empty before the call.
        // will be overridden in test body
        Arguments.of(List.of(), false, List.of()));
    }

    @Test
    void testRetainAllRemovesElements() {
        Collection<Integer> toRetain = List.of(20, 30, 40);
        boolean changed = list.retainAll(toRetain);
        assertTrue(changed, "retainAll should report a change when elements are removed");
        assertEquals(2, list.size(), "List size should be reduced to the retained elements");
        assertTrue(list.contains(20));
        assertTrue(list.contains(30));
        assertFalse(list.contains(10));
    }

    @Test
    void testRetainAllNoChangeWhenAllElementsRetained() {
        Collection<Integer> toRetain = List.of(10, 20, 30);
        boolean changed = list.retainAll(toRetain);
        assertFalse(changed, "retainAll should report no change when the list already matches the collection");
        assertEquals(3, list.size(), "List size should remain unchanged");
        assertTrue(list.containsAll(toRetain));
    }

    @Test
    void testRetainAllWhenSealedThrows() {
        sealedState = true;
        Collection<Integer> toRetain = List.of(20);
        assertThrows(UnsupportedOperationException.class, () -> list.retainAll(toRetain), "retainAll should throw when the list is sealed");
    }

    @Test
    void testRetainAllWithEmptyCollectionEmptiesList() {
        Collection<Integer> empty = List.of();
        boolean changed = list.retainAll(empty);
        assertTrue(changed, "retainAll should report a change when list becomes empty");
        assertTrue(list.isEmpty(), "List should be empty after retaining an empty collection");
    }

    @Test
    void testRetainAllNullCollectionThrowsNPE() {
        assertThrows(NullPointerException.class, () -> list.retainAll(null), "retainAll should throw NullPointerException when passed a null collection");
    }

    /* -------------------------------------------------
     *  Normal (unsealed) operation – parameterized tests
     * ------------------------------------------------- */
    @ParameterizedTest(name = "{index}: retainAll({0}) → changed={1}, result={2}")
    @MethodSource("provideNormalOperationCases")
    @DisplayName("retainAll - normal operation (unsealed)")
    void testRetainAllNormalOperation(Collection<Integer> col, boolean expectedChanged, List<Integer> expectedResult) {
        // Arrange – list already contains [1,2,3,4] from @BeforeEach
        // Act
        boolean changed = list.retainAll(col);
        // Assert
        assertEquals(expectedChanged, changed, "retainAll should report correct modification flag");
        assertEquals(expectedResult, list, "List content after retainAll does not match expectation");
    }

    @Test
    @DisplayName("retainAll - empty collection on already empty list returns false")
    void testRetainAllEmptyOnEmptyList() {
        // make list empty first
        list.clear();
        Collection<Integer> empty = List.of();
        boolean changed = list.retainAll(empty);
        assertFalse(changed, "retainAll should report no change when list is already empty");
        assertTrue(list.isEmpty(), "List should remain empty");
    }

    @Test
    @DisplayName("retainAll - collection containing nulls retains nulls")
    void testRetainAllWithNullsInCollectionAndList() {
        // list = [null, null, 5]
        list.clear();
        list.add(null);
        list.add(null);
        list.add(5);
        Collection<Integer> col = Collections.singletonList(null);
        boolean changed = list.retainAll(col);
        assertTrue(changed, "retainAll should report change when non‑null element is removed");
        assertEquals(List.of(null, null), list, "Only nulls should remain");
    }

    /* -------------------------------------------------
     *  Different element type collection
     * ------------------------------------------------- */
    @Test
    @DisplayName("retainAll with collection of different element type clears list")
    void testRetainAllDifferentElementType() {
        // list of Strings wrapped as SealableList<Object>
        SealableList<Object> stringList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        stringList.add("a");
        stringList.add("b");
        stringList.add("c");
        Collection<Integer> col = List.of(1, 2);
        boolean changed = stringList.retainAll(col);
        assertTrue(changed, "No match, list should become empty");
        assertTrue(stringList.isEmpty(), "List should be empty after retaining non‑matching types");
    }

    /* -------------------------------------------------
     *  Self collection case
     * ------------------------------------------------- */
    @Test
    @DisplayName("retainAll with the same list instance as collection")
    void testRetainAllSelfCollection() {
        // list currently [1,2,3,4]
        boolean changed = list.retainAll(list);
        assertFalse(changed, "Passing the list itself should not modify it");
        assertEquals(List.of(1, 2, 3, 4), list);
    }

    /* -------------------------------------------------
     *  Collection with nulls, list without nulls
     * ------------------------------------------------- */
    @Test
    @DisplayName("retainAll collection contains only null, list has no nulls")
    void testRetainAllCollectionOnlyNulls() {
        // list = [1,2,3]
        Collection<Integer> col = Collections.singletonList(null);
        boolean changed = list.retainAll(col);
        assertTrue(changed, "All elements removed, list becomes empty");
        assertTrue(list.isEmpty(), "List should be empty after retaining only nulls");
    }

    /* -------------------------------------------------
     *  Unmodifiable collection argument
     * ------------------------------------------------- */
    @Test
    @DisplayName("retainAll works with an unmodifiable collection")
    void testRetainAllUnmodifiableCollection() {
        Set<Integer> unmodSet = Collections.unmodifiableSet(Set.of(2, 3));
        // list currently [1,2,3,4]
        boolean changed = list.retainAll(unmodSet);
        assertTrue(changed, "Elements 1 and 4 are removed");
        assertEquals(List.of(2, 3), list);
    }

    /* -------------------------------------------------
     *  Unmodifiable underlying list delegation
     * ------------------------------------------------- */
    @Test
    @DisplayName("retainAll propagates UnsupportedOperationException from unmodifiable underlying list")
    void testRetainAllUnmodifiableUnderlyingList() {
        List<Integer> base = new ArrayList<>(List.of(1, 2, 3));
        List<Integer> unmod = Collections.unmodifiableList(base);
        SealableList<Integer> sealedUnmod = new SealableList<>(unmod, sealedSupplier);
        Collection<Integer> col = List.of(1, 2);
        assertThrows(UnsupportedOperationException.class, () -> sealedUnmod.retainAll(col), "Underlying unmodifiable list should throw UnsupportedOperationException");
    }

    /* -------------------------------------------------
     *  Helper to set list content via reflection (if needed)
     * ------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private <E> void setUnderlyingListContent(SealableList<E> sealable, List<E> newContent) {
        try {
            Field f = SealableList.class.getDeclaredField("list");
            f.setAccessible(true);
            f.set(sealable, newContent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
