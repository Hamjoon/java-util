package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static com.cedarsoftware.util.DeepEquals.deepEquals;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_addAll_2_Test {

    private ConcurrentList<Integer> list;

    /**
     * A List implementation that throws a RuntimeException from addAll.
     */
    private static class ExplodingList<E> extends ArrayList<E> {

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            throw new RuntimeException("boom");
        }
    }

    /**
     * List that throws ConcurrentModificationException on addAll.
     */
    private static class CmeList<E> extends ArrayList<E> {

        @Override
        public boolean addAll(int index, Collection<? extends E> c) {
            throw new ConcurrentModificationException("simulated");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for each test to guarantee isolation
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        list = null;
    }

    private static Stream<Arguments> validInsertionProvider() {
        return Stream.of(Arguments.of(2, Arrays.asList("x", "y"), Arrays.asList("a", "b", "x", "y", "c", "d")), Arguments.of(0, Arrays.asList("first", "second"), Arrays.asList("first", "second", "a", "b", "c")), Arguments.of(5, Arrays.asList("end1", "end2"), Arrays.asList("a", "b", "c", "d", "e", "end1", "end2")), Arguments.of(3, Arrays.asList(null, "notNull"), Arrays.asList("a", "b", "c", null, "notNull", "d", "e")));
    }

    @Test
    void testAddAllAtIndex_StartMiddleEnd() {
        // initial list: [1, 5]
        list.addAll(Arrays.asList(1, 5));
        // add at start
        boolean changedStart = list.addAll(0, Arrays.asList(-1, 0));
        assertTrue(changedStart, "addAll at start should report a change");
        deepEquals(Arrays.asList(-1, 0, 1, 5), list);
        // add at middle (index 2)
        boolean changedMiddle = list.addAll(2, Arrays.asList(2, 3, 4));
        assertTrue(changedMiddle, "addAll in the middle should report a change");
        deepEquals(Arrays.asList(-1, 0, 2, 3, 4, 1, 5), list);
        // add at end (current size is 7, add after last element)
        boolean changedEnd = list.addAll(7, Arrays.asList(6, 7));
        assertTrue(changedEnd, "addAll at end should report a change");
        deepEquals(Arrays.asList(-1, 0, 2, 3, 4, 1, 5, 6, 7), list);
    }

    @Test
    void testAddAllAtIndex_EmptyCollection() {
        list.addAll(Arrays.asList(10, 20));
        boolean changed = list.addAll(1, Collections.emptyList());
        assertFalse(changed, "addAll with an empty collection should return false");
        deepEquals(Arrays.asList(10, 20), list);
    }

    @Test
    void testAddAllAtIndex_NullCollection() {
        list.addAll(Arrays.asList(1, 2, 3));
        assertThrows(NullPointerException.class, () -> list.addAll(1, null), "addAll with a null collection should throw NullPointerException");
        // Ensure original list is unchanged
        deepEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    void testAddAllAtIndex_IndexOutOfBounds_Negative() {
        list.addAll(Arrays.asList(1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, Arrays.asList(9, 9)), "Negative index should throw IndexOutOfBoundsException");
    }

    @Test
    void testAddAllAtIndex_IndexOutOfBounds_TooLarge() {
        list.addAll(Arrays.asList(1, 2));
        // size is 2, valid indices for addAll are 0..2 inclusive.
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(3, Arrays.asList(9, 9)), "Index greater than size should throw IndexOutOfBoundsException");
    }

    @Test
    void testAddAllAtIndex_WithNullElements() {
        // Underlying list (ArrayList) permits nulls, so ConcurrentList should as well
        list.addAll(Arrays.asList(1, 2));
        boolean changed = list.addAll(1, Arrays.asList(null, null));
        assertTrue(changed, "addAll with null elements should report a change");
        deepEquals(Arrays.asList(1, null, null, 2), list);
        assertNull(list.get(1));
        assertNull(list.get(2));
    }

    @ParameterizedTest(name = "Valid insertion at index {0} with collection {1}")
    @MethodSource("validInsertionProvider")
    void testValidInsertions(Integer index, List<String> collection, List<String> expected) {
        // Prepare a list with initial elements a‑e
        ConcurrentList<String> strList = new ConcurrentList<>();
        strList.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        boolean changed = strList.addAll(index, collection);
        assertTrue(changed, "addAll should report a change for non‑empty collection");
        deepEquals(expected, strList);
    }

    @Test
    void testEmptyCollectionHandling() {
        list.addAll(Arrays.asList(1, 2, 3));
        boolean changed = list.addAll(1, Collections.emptyList());
        assertFalse(changed, "Empty collection must not modify the list");
        deepEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    void testWriteOperation_PropagatesRuntimeException() {
        ConcurrentList<Integer> exploding = new ConcurrentList<>(new ExplodingList<>());
        assertThrows(RuntimeException.class, () -> exploding.addAll(0, Arrays.asList(1, 2)), "RuntimeException from underlying list must propagate unchanged");
    }

    @Test
    void testInsertionBeforeLastElement() {
        ConcurrentList<String> strList = new ConcurrentList<>();
        strList.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        boolean changed = strList.addAll(4, Arrays.asList("x", "y"));
        assertTrue(changed);
        deepEquals(Arrays.asList("a", "b", "c", "d", "x", "y", "e"), strList);
    }

    @Test
    void testInsertionAtSizeBoundary() {
        ConcurrentList<String> strList = new ConcurrentList<>();
        strList.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        boolean changed = strList.addAll(5, Collections.singletonList("z"));
        assertTrue(changed);
        deepEquals(Arrays.asList("a", "b", "c", "d", "e", "z"), strList);
    }

    @Test
    void testSelfReferentialAddAtZero() {
        ConcurrentList<String> strList = new ConcurrentList<>();
        strList.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        // add the list to itself at index 0
        boolean changed = strList.addAll(0, strList);
        assertTrue(changed);
        deepEquals(Arrays.asList("a", "b", "c", "d", "e", "a", "b", "c", "d", "e"), strList);
    }

    @Test
    void testSelfReferentialAddAtMiddle() {
        ConcurrentList<String> strList = new ConcurrentList<>();
        strList.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        boolean changed = strList.addAll(3, strList);
        assertTrue(changed);
        deepEquals(Arrays.asList("a", "b", "c", "a", "b", "c", "d", "e", "d", "e"), strList);
    }

    @Test
    void testAddAllFromUnmodifiableCollection() {
        ConcurrentList<String> strList = new ConcurrentList<>();
        strList.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> source = Collections.unmodifiableList(Arrays.asList("x", "y"));
        boolean changed = strList.addAll(2, source);
        assertTrue(changed);
        deepEquals(Arrays.asList("a", "b", "x", "y", "c", "d", "e"), strList);
    }

    @Test
    void testAddAllFromEmptyUnmodifiableCollection() {
        ConcurrentList<String> strList = new ConcurrentList<>();
        strList.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> empty = Collections.unmodifiableList(Collections.emptyList());
        boolean changed = strList.addAll(0, empty);
        assertFalse(changed);
        deepEquals(Arrays.asList("a", "b", "c", "d", "e"), strList);
    }

    @Test
    void testLargeCollectionInsertion() {
        ConcurrentList<String> strList = new ConcurrentList<>();
        strList.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        List<String> many = Arrays.asList("i0", "i1", "i2", "i3", "i4", "i5", "i6", "i7", "i8", "i9");
        boolean changed = strList.addAll(1, many);
        assertTrue(changed);
        List<String> expected = new ArrayList<>();
        expected.add("a");
        expected.addAll(many);
        expected.addAll(Arrays.asList("b", "c", "d", "e"));
        deepEquals(expected, strList);
    }

    @Test
    void testConcurrentModificationExceptionPropagation() {
        ConcurrentList<Integer> cme = new ConcurrentList<>(new CmeList<>());
        assertThrows(ConcurrentModificationException.class, () -> cme.addAll(0, Collections.singletonList(1)), "ConcurrentModificationException must propagate unchanged");
    }

    @Test
    void testUnderlyingUnmodifiableListThrowsUnsupportedOperationException() {
        List<Integer> unmodifiable = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(1, 2, 3)));
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> wrapper.addAll(0, Collections.singletonList(99)), "Underlying unmodifiable list should cause UnsupportedOperationException");
    }
}
