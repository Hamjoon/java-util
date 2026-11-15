package com.cedarsoftware.util;

import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class SealableList_contains_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    // ---------- custom equality logic ----------
    private static final class Point {

        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Point))
                return false;
            Point p = (Point) o;
            return this.x == p.x && this.y == p.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    // ---------- element equals throws ----------
    private static final class FaultyEquals {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("Faulty equals invoked");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the sealed flag and create a fresh list for each test.
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // Populate with a few integers for the generic tests.
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent.
        list = null;
    }

    // ---------- basic functionality ----------
    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of("apple", true, "The underlying list contains the string \"apple\"; contains should return true."), Arguments.of("banana", false, "The underlying list does not contain \"banana\"; contains should return false."), Arguments.of(null, true, "The underlying list contains a null element; contains(null) should return true."), Arguments.of(null, false, "The underlying list does not contain null; contains(null) should return false."));
    }

    // ---------- empty list behavior ----------
    private static Stream<Arguments> emptyListProvider() {
        return Stream.of(Arguments.of("anyValue", false, "When the underlying list is empty, contains should always return false regardless of the searched element."), Arguments.of(null, false, "When the underlying list is empty, contains(null) should return false."));
    }

    @Test
    void containsExistingElement_returnsTrue() {
        assertTrue(list.contains(20), "List should contain the element that was added");
    }

    @Test
    void containsNonExistingElement_returnsFalse() {
        assertFalse(list.contains(99), "List should not contain an element that was never added");
    }

    @Test
    void containsNullWhenListHasNoNull_returnsFalse() {
        assertFalse(list.contains(null), "List without null should return false for contains(null)");
    }

    @Test
    void containsNullWhenListHasNull_returnsTrue() {
        list.add(null);
        assertTrue(list.contains(null), "List containing a null element should return true for contains(null)");
    }

    @Test
    void containsWorksWhenSealed() {
        // make the list immutable
        sealedState = true;
        // read‑only operations must still work
        assertTrue(list.contains(10), "contains should work even when the list is sealed");
        assertFalse(list.contains(999), "contains should correctly report missing elements when sealed");
    }

    @Test
    void containsDelegatesToUnderlyingList() {
        // Create a mock List and stub its contains method.
        @SuppressWarnings("unchecked")
        List<Integer> mockList = mock(List.class);
        when(mockList.contains(42)).thenReturn(true);
        when(mockList.contains(7)).thenReturn(false);
        SealableList<Integer> delegatingList = new SealableList<>(mockList, sealedSupplier);
        // Verify delegation for a value that the mock reports as present.
        assertTrue(delegatingList.contains(42));
        verify(mockList, times(1)).contains(42);
        // Verify delegation for a value that the mock reports as absent.
        assertFalse(delegatingList.contains(7));
        verify(mockList, times(1)).contains(7);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicFunctionalityProvider")
    void contains_basicFunctionality(String element, boolean expected, String description) {
        // Prepare a fresh SealableList<String> for each iteration.
        SealableList<String> sl = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // Populate according to the expectation:
        if (expected) {
            // Ensure the element is present.
            sl.add(element);
        } else {
            // Populate with a different value (if element is null we add a non‑null sentinel).
            sl.add(element == null ? "sentinel" : element + "_other");
        }
        assertEquals(expected, sl.contains(element), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("emptyListProvider")
    void contains_emptyList(String element, boolean expected, String description) {
        SealableList<Object> empty = new SealableList<>(new ArrayList<>(), sealedSupplier);
        assertEquals(expected, empty.contains(element), description);
    }

    // ---------- duplicate elements ----------
    @Test
    void contains_duplicateElements_returnsTrue() {
        SealableList<String> dupList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        dupList.add("dup");
        dupList.add("dup");
        dupList.add("dup");
        assertTrue(dupList.contains("dup"), "The list contains the element \"dup\" multiple times; contains should still return true.");
    }

    // ---------- type sensitivity ----------
    @Test
    void contains_typeSensitivity_integerTrue() {
        SealableList<Integer> intList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        intList.add(42);
        assertTrue(intList.contains(42), "The list stores an Integer 42; searching for the same Integer value should succeed.");
    }

    @Test
    void contains_typeSensitivity_longFalse() {
        SealableList<Integer> intList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        intList.add(42);
        assertFalse(intList.contains(42L), "The list stores an Integer 42; searching for a Long 42L should return false because equals is type‑sensitive.");
    }

    // ---------- reference‑based equals ----------
    @Test
    void contains_referenceBasedEquals_returnsFalse() {
        StringBuilder sb = new StringBuilder("test");
        SealableList<StringBuilder> sbList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        sbList.add(sb);
        // A different instance with the same content.
        StringBuilder other = new StringBuilder("test");
        assertFalse(sbList.contains(other), "StringBuilder does not override equals; a different instance is not considered equal.");
    }

    @Test
    void contains_customEqualityLogic_returnsTrue() {
        Point p1 = new Point(5, 10);
        Point p2 = new Point(5, 10);
        SealableList<Point> pointList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        pointList.add(p1);
        assertTrue(pointList.contains(p2), "A custom class Point overrides equals; searching with an equal instance should return true.");
    }

    // ---------- mutable element state ----------
    @Test
    void contains_mutableElementState_returnsTrueAfterMutation() {
        StringBuilder mutable = new StringBuilder("original");
        SealableList<StringBuilder> mutableList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        mutableList.add(mutable);
        // Mutate the stored object.
        mutable.append("changed");
        assertTrue(mutableList.contains(new StringBuilder("originalchanged")), "After mutating the stored StringBuilder, contains should reflect the new state and return true.");
    }

    // ---------- collection element equality ----------
    @Test
    void contains_collectionSameInstance_returnsTrue() {
        List<Integer> inner = Arrays.asList(1, 2, 3);
        SealableList<List<Integer>> listOfLists = new SealableList<>(new ArrayList<>(), sealedSupplier);
        listOfLists.add(inner);
        assertTrue(listOfLists.contains(inner), "The list stores a specific List<Integer> instance; searching with the same instance should return true.");
    }

    @Test
    void contains_collectionEqualInstance_returnsTrue() {
        List<Integer> stored = Arrays.asList(1, 2, 3);
        List<Integer> query = new ArrayList<>(Arrays.asList(1, 2, 3));
        SealableList<List<Integer>> listOfLists = new SealableList<>(new ArrayList<>(), sealedSupplier);
        listOfLists.add(stored);
        assertTrue(listOfLists.contains(query), "Searching with a different List instance that has equal contents should also return true.");
    }

    // ---------- size immutability check ----------
    @Test
    void contains_doesNotModifyListSize() {
        SealableList<String> sizeList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        assertEquals(0, sizeList.size(), "Pre‑condition: list is empty.");
        assertFalse(sizeList.contains("any"), "contains on empty list returns false.");
        assertEquals(0, sizeList.size(), "Post‑condition: size must remain unchanged after contains.");
    }

    // ---------- null internal list ----------
    @Test
    void contains_nullBackingList_throwsNPE() {
        SealableList<Integer> nullBacked = new SealableList<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, () -> nullBacked.contains(1), "Constructing with a null backing list should cause contains to throw NullPointerException.");
    }

    @Test
    void contains_elementEqualsThrows_propagatesException() {
        FaultyEquals faulty = new FaultyEquals();
        SealableList<FaultyEquals> listWithFaulty = new SealableList<>(new ArrayList<>(), sealedSupplier);
        listWithFaulty.add(faulty);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> listWithFaulty.contains(new FaultyEquals()), "The argument's equals method throws; contains should propagate the exception.");
        assertEquals("Faulty equals invoked", ex.getMessage());
    }

    // ---------- concurrent modification ----------
    @Test
    void contains_concurrentModification_throwsCME() {
        // Mock a backing list whose contains method throws CME.
        @SuppressWarnings("unchecked")
        List<String> mock = mock(List.class);
        when(mock.contains(any())).thenThrow(new ConcurrentModificationException("Concurrent change"));
        SealableList<String> cmList = new SealableList<>(mock, sealedSupplier);
        assertThrows(ConcurrentModificationException.class, () -> cmList.contains("test"), "If the underlying list throws ConcurrentModificationException during contains, it should be propagated.");
    }

    // ---------- unsupported operation ----------
    @Test
    void contains_unsupportedOperation_throwsUOE() {
        // Mock a backing list whose contains throws UnsupportedOperationException.
        @SuppressWarnings("unchecked")
        List<String> mock = mock(List.class);
        when(mock.contains(any())).thenThrow(new UnsupportedOperationException("Not supported"));
        SealableList<String> uoeList = new SealableList<>(mock, sealedSupplier);
        assertThrows(UnsupportedOperationException.class, () -> uoeList.contains("value"), "If the backing list does not support contains, the exception should propagate.");
    }
}
