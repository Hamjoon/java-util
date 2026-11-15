package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableList_subList_Test {

    /* --------------------------------------------------------------------- */
    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // start un‑sealed
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        list = null;
    }

    /**
     * Provider for all *valid* subList scenarios.
     */
    private static Stream<Arguments> validSubListProvider() {
        return Stream.of(// simple view checks
        Arguments.of(1, 3, "[\"b\",\"c\"]", "subList(1,3) returns [b,c]"), Arguments.of(0, 4, "[\"a\",\"b\",\"c\",\"d\"]", "subList(0,size) returns whole list"), Arguments.of(2, 2, "[]", "empty sublist when from == to"), // mutation reflected in original list
        Arguments.of(1, 3, "modification reflected in original list", "add \"x\" to sublist"), // removal reflected in original list
        Arguments.of(1, 3, "removal reflected in original list", "remove index 0 from sublist"), // empty sublist at start
        Arguments.of(0, 0, "[]", "empty sublist from start"), // last element only
        Arguments.of(3, 4, "[\"d\"]", "sublist containing last element"), // first two elements
        Arguments.of(0, 2, "[\"a\",\"b\"]", "sublist of first two elements"), // clear sublist then verify original
        Arguments.of(1, 4, "modification reflected in original list after clear", "clear sublist"));
    }

    /**
     * Provider for all *invalid* subList scenarios (exception handling).
     */
    private static Stream<Arguments> invalidSubListProvider() {
        return Stream.of(// negative fromIndex
        Arguments.of(-1, 2, IndexOutOfBoundsException.class, "negative fromIndex"), // toIndex larger than size
        Arguments.of(0, 5, IndexOutOfBoundsException.class, "toIndex > size"), // fromIndex > toIndex (IllegalArgumentException per List.subList contract)
        Arguments.of(3, 2, IllegalArgumentException.class, "fromIndex > toIndex"), // fromIndex == size (invalid for mutable lists)
        Arguments.of(4, 4, IndexOutOfBoundsException.class, "fromIndex == size on mutable list"), // both indices negative
        Arguments.of(-5, -1, IndexOutOfBoundsException.class, "both indices negative"), // fromIndex > toIndex (duplicate case from JSON)
        Arguments.of(2, 1, IllegalArgumentException.class, "fromIndex > toIndex (duplicate)"));
    }

    /**
     * Very small JSON‑array parser used only for the test data format
     * {@code ["a","b"]}. It strips the surrounding brackets and quotes,
     * then splits on commas.
     */
    private static List<String> parseJsonArray(String json) {
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Invalid JSON array: " + json);
        }
        String inner = trimmed.substring(1, trimmed.length() - 1).trim();
        if (inner.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = inner.split(",");
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            // remove surrounding quotes
            result.add(part.trim().replaceAll("^\"|\"$", ""));
        }
        return result;
    }

    @Test
    void testSubListBasicProperties() {
        // GIVEN a list with three elements
        // WHEN we request a sub‑list covering the first two elements
        List<Integer> sub = list.subList(0, 2);
        // THEN the sub‑list reflects the correct size and contents
        assertEquals(2, sub.size());
        assertEquals(Integer.valueOf(10), sub.get(0));
        assertEquals(Integer.valueOf(20), sub.get(1));
        // AND modifications on the sub‑list are reflected in the original list
        sub.add(40);
        assertEquals(4, list.size());
        assertEquals(Integer.valueOf(40), list.get(3));
        // AND removal works as expected
        sub.remove(Integer.valueOf(10));
        assertEquals(3, list.size());
        assertFalse(list.contains(10));
    }

    @Test
    void testSubListWhenSealedThrowsOnMutations() {
        // GIVEN a sealed list
        sealedState = true;
        List<Integer> sub = list.subList(0, 2);
        // THEN any mutating operation on the sub‑list must fail
        assertThrows(UnsupportedOperationException.class, () -> sub.add(99));
        assertThrows(UnsupportedOperationException.class, () -> sub.remove(Integer.valueOf(10)));
        assertThrows(UnsupportedOperationException.class, () -> sub.clear());
        // Non‑mutating operations still work
        assertEquals(2, sub.size());
        assertEquals(Integer.valueOf(10), sub.get(0));
    }

    @Test
    void testSubListIteratorRespectsSeal() {
        // GIVEN a sub‑list and its iterator
        List<Integer> sub = list.subList(0, 3);
        Iterator<Integer> it = sub.iterator();
        // WHEN the outer list becomes sealed
        sealedState = true;
        // THEN the iterator can still read existing elements
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(10), it.next());
        // BUT any attempt to remove via the iterator must fail
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void testSubListNestedSealingHonorsOuterSeal() {
        // GIVEN a chain of nested sub‑lists
        List<Integer> l2 = list.subList(0, list.size());
        List<Integer> l3 = l2.subList(0, l2.size());
        List<Integer> l4 = l3.subList(0, l3.size());
        List<Integer> l5 = l4.subList(0, l4.size());
        // WHEN we add through the deepest sub‑list while unsealed
        l5.add(40);
        assertEquals(4, list.size());
        assertEquals(Integer.valueOf(40), list.get(3));
        // AND then seal the outermost list
        sealedState = true;
        // THEN any further mutation through the deepest sub‑list is prohibited
        assertThrows(UnsupportedOperationException.class, () -> l5.add(50));
        // AND after un‑sealing it works again
        sealedState = false;
        l5.add(50);
        assertEquals(5, list.size());
        assertEquals(Integer.valueOf(50), list.get(4));
    }

    @Test
    void testSubListInvalidIndices() {
        // Negative fromIndex
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(-1, 2));
        // fromIndex greater than toIndex
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(2, 1));
        // toIndex beyond list size
        assertThrows(IndexOutOfBoundsException.class, () -> list.subList(0, 5));
    }

    @Test
    void testSubListIteratorBoundaries() {
        // GIVEN a sub‑list that is a view of the first two elements
        List<Integer> sub = list.subList(0, 2);
        ListIterator<Integer> it = sub.listIterator();
        // WHEN iterating forward
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(10), it.next());
        assertTrue(it.hasNext());
        assertEquals(Integer.valueOf(20), it.next());
        assertFalse(it.hasNext());
        // THEN attempting to go past the end throws NoSuchElementException
        assertThrows(NoSuchElementException.class, it::next);
        // AND moving backwards works as expected
        assertTrue(it.hasPrevious());
        assertEquals(Integer.valueOf(20), it.previous());
        assertEquals(Integer.valueOf(10), it.previous());
        assertFalse(it.hasPrevious());
        assertThrows(NoSuchElementException.class, it::previous);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validSubListProvider")
    void testValidSubListScenarios(int fromIndex, int toIndex, String expected, String description) {
        // Arrange: a fresh SealableList<String> with known contents
        List<String> backing = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        SealableList<String> stringList = new SealableList<>(backing, () -> false);
        // Act
        List<String> sub = stringList.subList(fromIndex, toIndex);
        // Assert based on the expected value
        if (expected.startsWith("[")) {
            // Expected a concrete list of values
            List<String> expectedList = parseJsonArray(expected);
            assertEquals(expectedList, sub, "Sublist contents must match expected");
        } else if ("modification reflected in original list".equals(expected)) {
            // Add an element to the sub‑list and verify the outer list sees it
            sub.add("x");
            List<String> expectedOuter = new ArrayList<>(Arrays.asList("a", "b", "x", "c", "d"));
            assertEquals(expectedOuter, stringList, "Original list must reflect addition via sublist");
        } else if ("removal reflected in original list".equals(expected)) {
            // Remove the first element of the sub‑list
            sub.remove(0);
            List<String> expectedOuter = new ArrayList<>(Arrays.asList("a", "c", "d"));
            assertEquals(expectedOuter, stringList, "Original list must reflect removal via sublist");
        } else if ("modification reflected in original list after clear".equals(expected)) {
            // Clear the sub‑list and verify the outer list now only contains the element before the sub‑list
            sub.clear();
            List<String> expectedOuter = Collections.singletonList("a");
            assertEquals(expectedOuter, stringList, "Original list must reflect clear via sublist");
        } else {
            fail("Unexpected expected value: " + expected);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidSubListProvider")
    void testInvalidSubListScenarios(int fromIndex, int toIndex, Class<? extends Throwable> expectedException, String description) {
        // Arrange a normal list of four strings
        SealableList<String> stringList = new SealableList<>(new ArrayList<>(Arrays.asList("a", "b", "c", "d")), () -> false);
        // Act & Assert
        assertThrows(expectedException, () -> stringList.subList(fromIndex, toIndex), "Expected " + expectedException.getSimpleName() + " for " + description);
    }

    @Test
    void testSubListWithNullBackingListThrowsNPE() {
        // Construct a SealableList with a null backing list
        SealableList<String> nullBacked = new SealableList<>(null, () -> false);
        // Any call that delegates to the underlying list should throw NPE
        assertThrows(NullPointerException.class, () -> nullBacked.subList(0, 1));
    }

    @Test
    void testSubListWithExtremeIndicesThrowsIOBE() {
        // Using Integer.MAX_VALUE as indices is guaranteed to be out of bounds
        SealableList<Integer> intList = new SealableList<>(new ArrayList<>(Arrays.asList(1, 2, 3)), () -> false);
        assertThrows(IndexOutOfBoundsException.class, () -> intList.subList(Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
