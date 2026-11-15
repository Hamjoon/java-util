package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableSet_size_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed = false;

    private Supplier<Boolean> sealedSupplier = () -> sealed;

    @BeforeEach
    void setupBeforeEach() {
        // ensure mutable state for each test
        sealed = false;
        set = new SealableSet<>(sealedSupplier);
        // Populate with a few elements – the exact content is not important for most tests
        set.add(10);
        set.add(20);
        set.add(null);
    }

    @AfterEach
    void teardownAfterEach() {
        // help GC
        set = null;
    }

    private static Stream<Arguments> basicSizeScenarios() {
        return Stream.of(Arguments.of(new HashSet<Integer>(), 0, "Empty HashSet"), Arguments.of(new HashSet<>(Collections.singleton(99)), 1, "Single element"), Arguments.of(new HashSet<>(Arrays.asList(1, 2, 3)), 3, "Three distinct elements"));
    }

    @Test
    void testInitialSize() {
        assertEquals(3, set.size(), "Initial size should reflect three added elements");
    }

    @Test
    void testSizeAfterAdd() {
        SealableSet<String> ss = new SealableSet<>(sealedSupplier);
        ss.add("A");
        assertEquals(1, ss.size(), "Initial size after adding one element should be 1.");
        ss.add("B");
        assertEquals(2, ss.size(), "Size after adding a new element to a set that initially contains one element should be 2.");
    }

    @Test
    void testSizeAfterRemove() {
        SealableSet<String> ss = new SealableSet<>(sealedSupplier);
        ss.add("X");
        ss.add("Y");
        assertEquals(2, ss.size(), "Pre‑condition: set should contain two elements.");
        ss.remove("X");
        assertEquals(1, ss.size(), "After removing an existing element from a set that initially contains two elements, size should be 1.");
    }

    @Test
    void testSizeWhenSealedAddThrowsAndUnchanged() {
        // make the set immutable
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> set.add(40));
        assertEquals(3, set.size(), "Size must remain unchanged when add is prohibited by sealing");
    }

    @Test
    void testSizeWhenSealedRemoveThrowsAndUnchanged() {
        // make the set immutable
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> set.remove(10));
        assertEquals(3, set.size(), "Size must remain unchanged when remove is prohibited by sealing");
    }

    @Test
    void testSizeAfterClear() {
        set.clear();
        assertEquals(0, set.size(), "Size should be zero after clear on a set that contains several elements.");
    }

    @Test
    void testSizeWithCollectionConstructor() {
        SealableSet<Integer> fromCollection = new SealableSet<>(Arrays.asList(1, 2, 3), sealedSupplier);
        assertEquals(3, fromCollection.size(), "Constructor with collection should reflect collection size");
    }

    @Test
    void testSizeWithSetConstructor() {
        Set<Integer> backing = new HashSet<>(Arrays.asList(5, 6));
        SealableSet<Integer> fromSet = new SealableSet<>(backing, sealedSupplier);
        assertEquals(2, fromSet.size(), "Constructor with explicit set should reflect set size");
    }

    @Test
    void testSizeWithNullElementSupport() {
        int before = set.size();
        // Adding another null should not change size because null already present
        set.add(null);
        assertEquals(before, set.size(), "Adding duplicate null should not affect size");
    }

    @Test
    void testInitialSize_EmptyBackingSet() {
        SealableSet<Integer> empty = new SealableSet<>(Collections.emptySet(), sealedSupplier);
        assertEquals(0, empty.size(), "Size of a newly created SealableSet wrapping an empty HashSet should be 0.");
    }

    @Test
    void testInitialSize_SingleElement() {
        Set<Integer> backing = new HashSet<>(Collections.singleton(42));
        SealableSet<Integer> single = new SealableSet<>(backing, sealedSupplier);
        assertEquals(1, single.size(), "Size of a SealableSet wrapping a Set containing a single element should be 1.");
    }

    @Test
    void testInitialSize_ThreeDistinctElements() {
        Set<Integer> backing = new HashSet<>(Arrays.asList(1, 2, 3));
        SealableSet<Integer> three = new SealableSet<>(backing, sealedSupplier);
        assertEquals(3, three.size(), "Size of a SealableSet wrapping a Set with three distinct elements should be 3.");
    }

    @Test
    void testSizeReflectsExternalMutationViaIteratorRemove() {
        SealableSet<String> ss = new SealableSet<>(sealedSupplier);
        ss.addAll(Arrays.asList("a", "b", "c", "d", "e"));
        assertEquals(5, ss.size(), "Pre‑condition: set should contain five elements.");
        Iterator<String> it = ss.iterator();
        while (it.hasNext()) {
            it.next();
            // remove via iterator
            it.remove();
        }
        assertEquals(0, ss.size(), "Removing all elements via iterator.remove() should result in size 0.");
    }

    @Test
    void testAddDuplicateDoesNotChangeSize() {
        SealableSet<String> ss = new SealableSet<>(sealedSupplier);
        ss.add("A");
        assertEquals(1, ss.size());
        // duplicate
        ss.add("A");
        assertEquals(1, ss.size(), "Adding a duplicate element does not change the size.");
    }

    @Test
    void testAddNullToEmptySetIncreasesSize() {
        SealableSet<String> ss = new SealableSet<>(sealedSupplier);
        assertEquals(0, ss.size());
        ss.add(null);
        assertEquals(1, ss.size(), "Adding a null element to an initially empty SealableSet should increase size to 1.");
    }

    @Test
    void testAddNullTwiceCountsOnce() {
        SealableSet<String> ss = new SealableSet<>(sealedSupplier);
        ss.add(null);
        assertEquals(1, ss.size());
        // second null
        ss.add(null);
        assertEquals(1, ss.size(), "Adding null twice should only count once.");
    }

    @Test
    void testAddAllWithDuplicatesAndNewElements() {
        SealableSet<Integer> ss = new SealableSet<>(sealedSupplier);
        ss.addAll(Arrays.asList(1, 2));
        assertEquals(2, ss.size());
        ss.addAll(Arrays.asList(2, 3, 3));
        assertEquals(3, ss.size(), "addAll with a collection containing new and duplicate elements results in correct size.");
    }

    @Test
    void testAddAllEmptyCollectionDoesNotAffectSize() {
        SealableSet<String> ss = new SealableSet<>(sealedSupplier);
        ss.addAll(Arrays.asList("x", "y"));
        assertEquals(2, ss.size());
        ss.addAll(Collections.emptyList());
        assertEquals(2, ss.size(), "addAll with an empty collection does not affect size.");
    }

    @Test
    void testRemoveNonExistentElementLeavesSizeUnchanged() {
        SealableSet<String> ss = new SealableSet<>(sealedSupplier);
        ss.addAll(Arrays.asList("a", "b", "c", "d"));
        assertEquals(4, ss.size());
        // non‑existent
        ss.remove("z");
        assertEquals(4, ss.size(), "Removing a non‑existent element leaves size unchanged.");
    }

    @Test
    void testRemoveAllPartialMatch() {
        SealableSet<Integer> ss = new SealableSet<>(sealedSupplier);
        ss.addAll(Arrays.asList(10, 20, 30, 40));
        assertEquals(4, ss.size());
        ss.removeAll(Arrays.asList(20, 99));
        assertEquals(3, ss.size(), "removeAll with a partially matching collection reduces size accordingly.");
    }

    @Test
    void testRetainAllRemovesAllElements() {
        SealableSet<String> ss = new SealableSet<>(sealedSupplier);
        ss.addAll(Arrays.asList("p", "q"));
        assertEquals(2, ss.size());
        ss.retainAll(Collections.emptySet());
        assertEquals(0, ss.size(), "retainAll that removes all elements results in size 0.");
    }

    @Test
    void testExternalMutationAfterCreationIsReflected() {
        Set<Integer> backing = new HashSet<>(Arrays.asList(1, 2));
        SealableSet<Integer> ss = new SealableSet<>(backing, sealedSupplier);
        assertEquals(2, ss.size());
        // Mutate the original collection directly
        backing.add(3);
        assertEquals(3, ss.size(), "External mutation of the underlying set after SealableSet creation is reflected in size().");
    }

    @Test
    void testUnderlyingTreeSetWorksSame() {
        Set<Integer> treeSet = new java.util.TreeSet<>(Arrays.asList(5, 6, 7, 8, 9));
        SealableSet<Integer> ss = new SealableSet<>(treeSet, sealedSupplier);
        assertEquals(5, ss.size(), "Underlying TreeSet implementation works the same: size should be 5.");
    }

    @Test
    void testSizeThrowsWhenUnderlyingSetIsNull() throws Exception {
        // Build a SealableSet with a non‑null backing set, then replace it with null via reflection.
        SealableSet<Integer> ss = new SealableSet<>(sealedSupplier);
        setInternalBackingSet(ss, null);
        assertThrows(NullPointerException.class, ss::size, "Calling size() when the SealableSet was constructed with a null underlying Set should throw NullPointerException.");
    }

    @Test
    void testSizePropagatesIllegalStateExceptionFromBackingSet(@Mock Set<Integer> mockSet) {
        when(mockSet.size()).thenThrow(new IllegalStateException("illegal state"));
        SealableSet<Integer> ss = new SealableSet<>(sealedSupplier);
        setInternalBackingSet(ss, mockSet);
        IllegalStateException ex = assertThrows(IllegalStateException.class, ss::size, "If the underlying Set throws IllegalStateException from its size(), size() must propagate that exception.");
        assertEquals("illegal state", ex.getMessage());
    }

    @Test
    void testSizePropagatesUnsupportedOperationExceptionFromBackingSet(@Mock Set<Integer> mockSet) {
        when(mockSet.size()).thenThrow(new UnsupportedOperationException("not supported"));
        SealableSet<Integer> ss = new SealableSet<>(sealedSupplier);
        setInternalBackingSet(ss, mockSet);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, ss::size, "When the wrapped Set does not support the size() operation, size() should let the exception propagate.");
        assertEquals("not supported", ex.getMessage());
    }

    /**
     * Uses reflection to set the private {@code set} field of a {@link SealableSet}.
     *
     * @param ss   the SealableSet instance whose backing set should be replaced
     * @param backing the new backing Set (may be {@code null})
     */
    @SuppressWarnings("unchecked")
    private static void setInternalBackingSet(SealableSet<?> ss, Set<?> backing) {
        try {
            Field f = SealableSet.class.getDeclaredField("set");
            f.setAccessible(true);
            f.set(ss, backing);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject mock backing set", e);
        }
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicSizeScenarios")
    void testSizeWithVariousBackingSets(Set<Integer> backing, int expectedSize, String description) {
        SealableSet<Integer> ss = new SealableSet<>(backing, sealedSupplier);
        assertEquals(expectedSize, ss.size(), description);
    }
}
