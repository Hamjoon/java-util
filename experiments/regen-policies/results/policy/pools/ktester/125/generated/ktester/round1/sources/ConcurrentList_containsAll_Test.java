package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class ConcurrentList_containsAll_Test {

    private ConcurrentList<Integer> list;

    // ---------- Exception from element equals ----------
    private static class BadEquals {

        @Override
        public boolean equals(Object obj) {
            throw new RuntimeException("boom from equals");
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor (stand‑alone mode)
        list = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the list to avoid cross‑test contamination
        list.clear();
    }

    // ---------- Basic functionality ----------
    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// list = [1,2,3]; query = [1,2] -> true
        Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(1, 2), true), // list = [1,2,3]; query = [4] -> false
        Arguments.of(Arrays.asList(1, 2, 3), Collections.singletonList(4), false));
    }

    // ---------- Empty collections ----------
    private static Stream<Arguments> emptyCollectionsProvider() {
        return Stream.of(// both empty -> true
        Arguments.of(Collections.emptyList(), Collections.emptyList(), true));
    }

    // ---------- Duplicate elements in collection ----------
    private static Stream<Arguments> duplicateElementsProvider() {
        return Stream.of(// list = [1,2,3]; query = [1,1] -> true
        Arguments.of(Arrays.asList(1, 2, 3), Arrays.asList(1, 1), true));
    }

    // ---------- Collection type variations ----------
    private static Stream<Arguments> collectionTypeProvider() {
        return Stream.of(// Set version, all present -> true
        Arguments.of(Arrays.asList(1, 2, 3), new HashSet<>(Arrays.asList(2, 3)), true));
    }

    // ---------- Non‑empty list vs empty collection ----------
    private static Stream<Arguments> nonEmptyListEmptyCollectionProvider() {
        return Stream.of(Arguments.of(Arrays.asList(1, 2, 3), Collections.emptyList(), true));
    }

    // ---------- Empty list vs non‑empty collection ----------
    private static Stream<Arguments> emptyListNonEmptyCollectionProvider() {
        return Stream.of(Arguments.of(Collections.emptyList(), Collections.singletonList(1), false));
    }

    // ---------- List with duplicate elements ----------
    private static Stream<Arguments> listWithDuplicatesProvider() {
        return Stream.of(// list = [1,1,2]; query = [1] -> true
        Arguments.of(Arrays.asList(1, 1, 2), Collections.singletonList(1), true), // list = [1,1,2]; query = [3] -> false
        Arguments.of(Arrays.asList(1, 1, 2), Collections.singletonList(3), false));
    }

    // ---------- Different collection implementation ----------
    private static Stream<Arguments> differentImplementationProvider() {
        return Stream.of(// LinkedList with present elements
        Arguments.of(Arrays.asList(1, 2, 3), new LinkedList<>(Arrays.asList(2, 3)), true), // LinkedList with missing element
        Arguments.of(Arrays.asList(1, 2, 3), new LinkedList<>(Collections.singletonList(4)), false));
    }

    @Test
    void testContainsAll_AllElementsPresent() {
        list.addAll(Arrays.asList(1, 2, 3, 4, 5));
        Collection<Integer> query = Arrays.asList(2, 4);
        assertTrue(list.containsAll(query), "containsAll should return true when all queried elements are present");
    }

    @Test
    void testContainsAll_MissingElements() {
        list.addAll(Arrays.asList(10, 20, 30));
        // 40 is not in the list
        Collection<Integer> query = Arrays.asList(20, 40);
        assertFalse(list.containsAll(query), "containsAll should return false when at least one queried element is missing");
    }

    @Test
    void testContainsAll_EmptyCollection() {
        list.addAll(Arrays.asList(7, 8, 9));
        Collection<Integer> empty = Collections.emptyList();
        assertTrue(list.containsAll(empty), "containsAll should return true for an empty collection regardless of list contents");
    }

    @Test
    void testContainsAll_EmptyList() {
        // list is empty at this point
        Collection<Integer> query = Arrays.asList(1, 2);
        assertFalse(list.containsAll(query), "containsAll should return false when the list is empty but the collection is not");
    }

    @Test
    void testContainsAll_EmptyListAndEmptyCollection() {
        Collection<Integer> empty = Collections.emptyList();
        assertTrue(list.containsAll(empty), "containsAll should return true when both the list and the collection are empty");
    }

    @Test
    void testContainsAll_NullCollectionThrowsNPE() {
        list.addAll(Arrays.asList(1, 2, 3));
        assertThrows(NullPointerException.class, () -> list.containsAll(null), "containsAll should throw NullPointerException when the argument collection is null");
    }

    @Test
    void testContainsAll_WrapperMode() {
        // Create a backing list and wrap it
        List<Integer> backing = new ArrayList<>(Arrays.asList(5, 6, 7));
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backing);
        Collection<Integer> query = Arrays.asList(5, 7);
        assertTrue(wrapped.containsAll(query), "containsAll should work correctly when ConcurrentList wraps an existing list");
    }

    @ParameterizedTest(name = "[Basic] list={0}, query={1} => {2}")
    @MethodSource("basicFunctionalityProvider")
    void testContainsAll_BasicFunctionality(List<Integer> initial, Collection<Integer> query, boolean expected) {
        list.addAll(initial);
        assertEquals(expected, list.containsAll(query));
    }

    @ParameterizedTest(name = "[Empty] list={0}, query={1} => {2}")
    @MethodSource("emptyCollectionsProvider")
    void testContainsAll_EmptyCollections(List<Integer> initial, Collection<Integer> query, boolean expected) {
        list.addAll(initial);
        assertEquals(expected, list.containsAll(query));
    }

    // ---------- Null elements handling ----------
    @Test
    void testContainsAll_NullElements_Present() {
        // List contains null and "a"
        ConcurrentList<Object> objList = new ConcurrentList<>();
        objList.add(null);
        objList.add("a");
        Collection<Object> query = Collections.singletonList(null);
        assertTrue(objList.containsAll(query), "null element present in list should be detected");
    }

    @Test
    void testContainsAll_NullElements_Absent() {
        ConcurrentList<Object> objList = new ConcurrentList<>();
        objList.add(null);
        objList.add("a");
        Collection<Object> query = Arrays.asList(null, "b");
        assertFalse(objList.containsAll(query), "collection contains a non‑present element (\"b\") -> false");
    }

    @ParameterizedTest(name = "[Duplicates] list={0}, query={1} => {2}")
    @MethodSource("duplicateElementsProvider")
    void testContainsAll_DuplicateElementsInCollection(List<Integer> initial, Collection<Integer> query, boolean expected) {
        list.addAll(initial);
        assertEquals(expected, list.containsAll(query));
    }

    @ParameterizedTest(name = "[TypeVar] list={0}, query={1} (Set) => {2}")
    @MethodSource("collectionTypeProvider")
    void testContainsAll_CollectionTypeVariations(List<Integer> initial, Collection<Integer> query, boolean expected) {
        list.addAll(initial);
        assertEquals(expected, list.containsAll(query));
    }

    @ParameterizedTest(name = "[NonEmptyList/EmptyQuery] list={0}, query={1} => {2}")
    @MethodSource("nonEmptyListEmptyCollectionProvider")
    void testContainsAll_NonEmptyListEmptyCollection(List<Integer> initial, Collection<Integer> query, boolean expected) {
        list.addAll(initial);
        assertEquals(expected, list.containsAll(query));
    }

    @ParameterizedTest(name = "[EmptyList/NonEmptyQuery] list={0}, query={1} => {2}")
    @MethodSource("emptyListNonEmptyCollectionProvider")
    void testContainsAll_EmptyListNonEmptyCollection(List<Integer> initial, Collection<Integer> query, boolean expected) {
        list.addAll(initial);
        assertEquals(expected, list.containsAll(query));
    }

    @ParameterizedTest(name = "[ListDupes] list={0}, query={1} => {2}")
    @MethodSource("listWithDuplicatesProvider")
    void testContainsAll_ListWithDuplicateElements(List<Integer> initial, Collection<Integer> query, boolean expected) {
        list.addAll(initial);
        assertEquals(expected, list.containsAll(query));
    }

    // ---------- Null element absent in list ----------
    @Test
    void testContainsAll_NullElementAbsent() {
        list.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> query = Collections.singletonList(null);
        assertFalse(list.containsAll(query));
    }

    // ---------- Mixed type elements ----------
    @Test
    void testContainsAll_MixedTypeFailure() {
        list.addAll(Arrays.asList(1, 2, 3));
        // Query contains a String "1" – should not be considered equal to Integer 1
        Collection<Object> query = Collections.singletonList("1");
        // Need a raw ConcurrentList to allow mixed types
        @SuppressWarnings("unchecked")
        ConcurrentList rawList = list;
        assertFalse(rawList.containsAll(query));
    }

    // ---------- Large collection with duplicates (all present) ----------
    @Test
    void testContainsAll_LargeCollectionWithDuplicatesAllPresent() {
        list.addAll(Arrays.asList(1, 2, 3));
        Collection<Integer> query = Arrays.asList(1, 1, 2, 2, 3, 3);
        assertTrue(list.containsAll(query));
    }

    @ParameterizedTest(name = "[DiffImpl] list={0}, query={1} => {2}")
    @MethodSource("differentImplementationProvider")
    void testContainsAll_DifferentCollectionImplementation(List<Integer> initial, Collection<Integer> query, boolean expected) {
        list.addAll(initial);
        assertEquals(expected, list.containsAll(query));
    }

    @Test
    void testContainsAll_ElementEqualsThrows() {
        list.addAll(Arrays.asList(1, 2, 3));
        BadEquals bad = new BadEquals();
        Collection<Object> query = Collections.singletonList(bad);
        // Use raw list to allow Object type
        @SuppressWarnings("unchecked")
        ConcurrentList rawList = list;
        RuntimeException ex = assertThrows(RuntimeException.class, () -> rawList.containsAll(query));
        assertEquals("boom from equals", ex.getMessage());
    }

    // ---------- Exception from collection iterator ----------
    @Test
    void testContainsAll_CollectionIteratorThrows() {
        list.addAll(Arrays.asList(1, 2, 3));
        @SuppressWarnings("unchecked")
        Collection<Integer> badCollection = mock(Collection.class);
        // iterator() will throw when called
        when(badCollection.iterator()).thenThrow(new ConcurrentModificationException("iterator broken"));
        // Use raw list to avoid generic mismatch
        @SuppressWarnings("unchecked")
        ConcurrentList rawList = list;
        assertThrows(ConcurrentModificationException.class, () -> rawList.containsAll(badCollection));
    }
}
