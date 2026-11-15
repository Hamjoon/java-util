package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableList_addAll_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        // Use a mutable backing list so we can verify contents easily.
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // Populate with a few elements to test additive behaviour.
        list.add(1);
        list.add(2);
        list.add(3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent.
        list = null;
    }

    private static Stream<Arguments> normalOperationProvider() {
        return Stream.of(// non‑empty collection
        Arguments.of(Arrays.asList(4, 5, 6), true), // empty collection
        Arguments.of(Collections.emptyList(), false));
    }

    private static Stream<Arguments> sealedOperationProvider() {
        return Stream.of(// non‑empty collection
        Arguments.of(Arrays.asList(7, 8)), // empty collection
        Arguments.of(Collections.emptyList()), // null collection
        Arguments.of(null));
    }

    @Test
    void testAddAllWhenNotSealedAddsElementsAndReturnsTrue() {
        Collection<Integer> toAdd = Arrays.asList(4, 5, 6);
        boolean changed = list.addAll(toAdd);
        assertTrue(changed, "addAll should report that the list changed");
        assertEquals(6, list.size(), "List size should reflect added elements");
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), new ArrayList<>(list), "Elements should be appended in the order provided by the collection");
    }

    @Test
    void testAddAllWhenNotSealedWithEmptyCollectionReturnsFalse() {
        Collection<Integer> empty = Collections.emptyList();
        boolean changed = list.addAll(empty);
        assertFalse(changed, "addAll on an empty collection should return false");
        assertEquals(3, list.size(), "List size must remain unchanged");
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(list));
    }

    @Test
    void testAddAllWhenSealedThrowsUnsupportedOperationException() {
        // seal the list
        sealedState = true;
        Collection<Integer> toAdd = Arrays.asList(7, 8);
        assertThrows(UnsupportedOperationException.class, () -> list.addAll(toAdd), "addAll should throw when the list is sealed");
    }

    @Test
    void testAddAllNullCollectionThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> list.addAll(null), "addAll must throw NPE when the supplied collection is null");
    }

    @Test
    void testAddAllPreservesIterationOrderFromDifferentCollectionTypes() {
        // LinkedHashSet preserves insertion order, but iteration order may differ from List.
        Set<Integer> linkedSet = new LinkedHashSet<>(Arrays.asList(9, 10, 11));
        boolean changed = list.addAll(linkedSet);
        assertTrue(changed);
        assertEquals(6, list.size());
        // Expected order: original [1,2,3] followed by the set's iteration order [9,10,11]
        assertEquals(Arrays.asList(1, 2, 3, 9, 10, 11), new ArrayList<>(list));
    }

    @Test
    void testAddAllWhenSealedThenUnsealedAllowsAddition() {
        // Seal, attempt (fails), then unseal and succeed.
        sealedState = true;
        Collection<Integer> coll = Collections.singletonList(12);
        assertThrows(UnsupportedOperationException.class, () -> list.addAll(coll));
        // unseal
        sealedState = false;
        boolean changed = list.addAll(coll);
        assertTrue(changed);
        assertEquals(4, list.size());
        assertTrue(list.contains(12));
    }

    /**
     * Verifies that {@code addAll} delegates to the backing list and returns the
     * correct boolean result for both non‑empty and empty collections.
     */
    @ParameterizedTest(name = "{index}: addAll({0}) → {1}")
    @MethodSource("normalOperationProvider")
    void testAddAllNormalOperation(Collection<Integer> toAdd, boolean expectedResult) {
        boolean changed = list.addAll(toAdd);
        assertEquals(expectedResult, changed, "addAll should return the value supplied by the backing list");
        // Verify the final size matches expectation.
        int expectedSize = expectedResult ? 3 + toAdd.size() : 3;
        assertEquals(expectedSize, list.size(), "List size should reflect whether the collection modified the list");
    }

    /**
     * A null collection must cause a {@link NullPointerException}.
     */
    @Test
    void testAddAllNullCollectionThrowsNPE() {
        assertThrows(NullPointerException.class, () -> list.addAll(null), "addAll must throw NPE when the supplied collection is null");
    }

    /**
     * When the list is sealed, any call to {@code addAll} must abort before
     * delegating to the backing list and throw {@link IllegalStateException},
     * regardless of the collection's contents.
     */
    @ParameterizedTest(name = "{index}: sealed addAll({0}) throws IllegalStateException")
    @MethodSource("sealedOperationProvider")
    void testAddAllWhenSealedThrowsIllegalStateException(Collection<Integer> toAdd) {
        // seal the list
        sealedState = true;
        assertThrows(IllegalStateException.class, () -> list.addAll(toAdd), "addAll should throw IllegalStateException when the list is sealed");
    }

    /**
     * If the backing list is an unmodifiable view, the {@code addAll}
     * operation should propagate the {@link UnsupportedOperationException}
     * thrown by the delegate.
     */
    @Test
    void testAddAllWithUnmodifiableBackingListPropagatesException() {
        // Construct a SealableList whose backing list cannot be modified.
        List<Integer> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        SealableList<Integer> unmodifiableSealable = new SealableList<>(unmodifiable, sealedSupplier);
        Collection<Integer> toAdd = Arrays.asList(1, 2);
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSealable.addAll(toAdd), "addAll should propagate UnsupportedOperationException from an unmodifiable backing list");
    }

    /**
     * A {@code SealableList<String>} receiving a collection of {@code Integer}
     * objects should let the underlying list throw a {@link ClassCastException},
     * which must be propagated.
     */
    @Test
    void testAddAllWithMismatchedElementTypePropagatesClassCastException() {
        // Use raw types to bypass compile‑time generic checks.
        SealableList rawList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // ensure the list is of type String at runtime
        rawList.add("a");
        @SuppressWarnings("unchecked")
        Collection<Integer> wrongType = Arrays.asList(1, 2);
        assertThrows(ClassCastException.class, () -> rawList.addAll(wrongType), "addAll should propagate ClassCastException when element types are incompatible");
    }
}
