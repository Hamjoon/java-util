package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableList_addAll_2_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        // Use the constructor that accepts a backing List so we can start with a known collection
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(1);
        list.add(2);
        list.add(3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        list = null;
    }

    /**
     * Provides normal insertion scenarios (start, middle, end) together with
     * the expected list after the operation.
     */
    private static Stream<Arguments> normalInsertionProvider() {
        return Stream.of(// insert at start
        Arguments.of(0, Arrays.asList("alpha", "beta"), Arrays.asList("alpha", "beta", 1, 2, 3)), // insert in the middle (index 2, after elements 1 and 2)
        Arguments.of(2, Arrays.asList("mid1", "mid2"), Arrays.asList(1, 2, "mid1", "mid2", 3)), // insert at the end (index == size)
        Arguments.of(3, Collections.singletonList("end"), Arrays.asList(1, 2, 3, "end")));
    }

    @Test
    void testAddAllAtIndexWhenUnsealed() {
        // given
        Collection<Integer> toAdd = Arrays.asList(10, 20, 30);
        // insert after the first element (1)
        int index = 1;
        // when
        boolean changed = list.addAll(index, toAdd);
        // then
        assertTrue(changed, "addAll should report that the list was modified");
        // Expected order: 1, 10, 20, 30, 2, 3
        List<Integer> expected = Arrays.asList(1, 10, 20, 30, 2, 3);
        assertEquals(expected, list);
        assertEquals(expected.size(), list.size());
    }

    @Test
    void testAddAllAtIndexWhenSealedThrows() {
        // given
        // seal the list
        sealedState = true;
        Collection<Integer> toAdd = Arrays.asList(4, 5);
        int index = 2;
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> list.addAll(index, toAdd), "Adding to a sealed list should throw UnsupportedOperationException");
    }

    @Test
    void testAddAllAtEndIsEquivalentToAddAllCollection() {
        // given
        Collection<Integer> toAdd = Arrays.asList(7, 8, 9);
        int originalSize = list.size();
        // when
        boolean changed = list.addAll(originalSize, toAdd);
        // then
        assertTrue(changed);
        List<Integer> expected = new ArrayList<>(Arrays.asList(1, 2, 3));
        expected.addAll(toAdd);
        assertEquals(expected, list);
    }

    @Test
    void testAddAllWithEmptyCollectionDoesNotChangeList() {
        // given
        Collection<Integer> empty = new ArrayList<>();
        int sizeBefore = list.size();
        // when
        boolean changed = list.addAll(0, empty);
        // then
        assertFalse(changed, "addAll with an empty collection should return false");
        assertEquals(sizeBefore, list.size(), "List size should remain unchanged");
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    void testAddAllWithNullCollectionThrowsNPE() {
        // given
        Collection<Integer> nullCol = null;
        // when / then
        assertThrows(NullPointerException.class, () -> list.addAll(0, nullCol), "addAll with a null collection should throw NullPointerException");
    }

    @ParameterizedTest(name = "addAll at index {0} with collection {1}")
    @MethodSource("normalInsertionProvider")
    void testAddAllNormalInsertion(int index, Collection<String> toAdd, List<Object> expected) {
        // The list created in @BeforeEach holds Integer values; we need a fresh list of Strings.
        SealableList<String> stringList = new SealableList<>(new ArrayList<>(), () -> false);
        stringList.add("1");
        stringList.add("2");
        stringList.add("3");
        // when
        boolean changed = stringList.addAll(index, toAdd);
        // then
        assertTrue(changed, "addAll should return true for a non‑empty collection");
        assertEquals(expected, stringList, "List contents after insertion do not match expectation");
    }

    @Test
    void testAddAllNegativeIndexThrowsIndexOutOfBounds() {
        Collection<Integer> col = Collections.singletonList(99);
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, col), "Negative index must result in IndexOutOfBoundsException");
    }

    @Test
    void testAddAllIndexGreaterThanSizeThrowsIndexOutOfBounds() {
        Collection<Integer> col = Collections.singletonList(99);
        // one past the allowed range
        int illegalIndex = list.size() + 1;
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(illegalIndex, col), "Index greater than size must result in IndexOutOfBoundsException");
    }

    @Test
    void testAddAllCollectionContainingNullIsAccepted() {
        Collection<Integer> col = Arrays.asList(null, 42);
        boolean changed = list.addAll(0, col);
        assertTrue(changed, "addAll should return true when collection contains null");
        List<Integer> expected = Arrays.asList(null, 42, 1, 2, 3);
        assertEquals(expected, list);
    }

    @Test
    void testAddAllDuplicatesArePreserved() {
        Collection<String> col = Arrays.asList("dup", "dup");
        // Use a String list for clarity
        SealableList<String> stringList = new SealableList<>(new ArrayList<>(), () -> false);
        stringList.addAll(Arrays.asList("a", "b"));
        boolean changed = stringList.addAll(0, col);
        assertTrue(changed);
        List<String> expected = Arrays.asList("dup", "dup", "a", "b");
        assertEquals(expected, stringList);
    }

    @Test
    void testAddAllWhenSealedThrowsBeforeCheckingCollection_Empty() {
        // seal the list
        sealedState = true;
        Collection<Integer> empty = Collections.emptyList();
        assertThrows(IllegalStateException.class, () -> list.addAll(0, empty), "Sealed list must throw IllegalStateException before any collection processing");
    }

    @Test
    void testAddAllWhenSealedThrowsBeforeCheckingCollection_NonEmpty() {
        // seal the list
        sealedState = true;
        Collection<Integer> col = Collections.singletonList(123);
        assertThrows(IllegalStateException.class, () -> list.addAll(0, col), "Sealed list must throw IllegalStateException before any collection processing");
    }

    @Test
    void testAddAllMixedNullAndNonNullElements() {
        Collection<String> col = Arrays.asList(null, "x", null);
        // Use a String list for this scenario
        SealableList<String> stringList = new SealableList<>(new ArrayList<>(), () -> false);
        stringList.addAll(Arrays.asList("a", "b"));
        boolean changed = stringList.addAll(0, col);
        assertTrue(changed);
        List<String> expected = Arrays.asList(null, "x", null, "a", "b");
        assertEquals(expected, stringList);
    }
}
