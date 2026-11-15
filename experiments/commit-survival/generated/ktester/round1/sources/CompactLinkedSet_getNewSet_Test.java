package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CompactLinkedSet_getNewSet_Test {

    private CompactLinkedSet<Integer> compactLinkedSet;

    /**
     * Helper subclass that allows us to dictate the result of {@code compactSize()}.
     */
    private static class CompactLinkedSetWithSize<E> extends CompactLinkedSet<E> {

        private final int size;

        CompactLinkedSetWithSize(int size) {
            this.size = size;
        }

        @Override
        protected int compactSize() {
            return size;
        }
    }

    private static class CompactLinkedSetException<E> extends CompactLinkedSet<E> {

        @Override
        protected int compactSize() {
            throw new IllegalStateException("compactSize failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance for each test.
        compactLinkedSet = new CompactLinkedSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC; not strictly required.
        compactLinkedSet = null;
    }

    /**
     * Inspects the internal HashMap of a LinkedHashSet via reflection and returns the
     * length of its table array (the underlying bucket array). This reveals the
     * initial capacity chosen by the LinkedHashSet constructor.
     */
    private static int getHashMapTableLength(Set<?> linkedHashSet) throws Exception {
        // LinkedHashSet extends HashSet which has a private field "map"
        Field mapField = HashSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<?, ?> map = (HashMap<?, ?>) mapField.get(linkedHashSet);
        // HashMap has a private field "table" (the bucket array)
        Field tableField = HashMap.class.getDeclaredField("table");
        tableField.setAccessible(true);
        Object[] table = (Object[]) tableField.get(map);
        return table == null ? 0 : table.length;
    }

    @Test
    void getNewSet_returnsEmptyLinkedHashSet() {
        // Act
        Set<Integer> newSet = compactLinkedSet.getNewSet();
        // Assert
        assertNotNull(newSet, "The returned set should not be null.");
        assertTrue(newSet instanceof LinkedHashSet, "The returned set should be an instance of LinkedHashSet.");
        assertTrue(newSet.isEmpty(), "The newly created set should be empty.");
    }

    @Test
    void getNewSet_returnsDistinctInstancesOnEachCall() {
        // Act
        Set<Integer> firstSet = compactLinkedSet.getNewSet();
        Set<Integer> secondSet = compactLinkedSet.getNewSet();
        // Assert
        assertNotSame(firstSet, secondSet, "Each invocation of getNewSet() should return a new distinct Set instance.");
    }

    @Test
    void getNewSet_behavesLikeLinkedHashSetInsertionOrder() {
        // Arrange
        Set<Integer> newSet = compactLinkedSet.getNewSet();
        // Act
        newSet.add(5);
        newSet.add(3);
        newSet.add(9);
        // Assert
        // LinkedHashSet preserves insertion order; we verify by iterating.
        Integer[] expectedOrder = { 5, 3, 9 };
        int index = 0;
        for (Integer value : newSet) {
            assertEquals(expectedOrder[index++], value, "LinkedHashSet should preserve insertion order.");
        }
    }

    @Test
    void getNewSet_initialCapacityWhenCompactSizeZero() throws Exception {
        CompactLinkedSetWithSize<Integer> set = new CompactLinkedSetWithSize<>(0);
        Set<Integer> newSet = set.getNewSet();
        assertTrue(newSet instanceof LinkedHashSet);
        assertTrue(newSet.isEmpty());
        // Expected initial capacity = compactSize() + 1 = 1
        int tableLen = getHashMapTableLength(newSet);
        assertTrue(tableLen >= 1, "Internal table length should be at least 1 when compactSize() is 0.");
    }

    @Test
    void getNewSet_initialCapacityWhenCompactSizeFive() throws Exception {
        CompactLinkedSetWithSize<Integer> set = new CompactLinkedSetWithSize<>(5);
        Set<Integer> newSet = set.getNewSet();
        assertTrue(newSet instanceof LinkedHashSet);
        assertTrue(newSet.isEmpty());
        // Expected initial capacity = 5 + 1 = 6
        int tableLen = getHashMapTableLength(newSet);
        assertTrue(tableLen >= 6, "Internal table length should be at least 6 when compactSize() is 5.");
    }

    @Test
    void getNewSet_initialCapacityWhenCompactSizeOne() throws Exception {
        CompactLinkedSetWithSize<Integer> set = new CompactLinkedSetWithSize<>(1);
        Set<Integer> newSet = set.getNewSet();
        int tableLen = getHashMapTableLength(newSet);
        assertTrue(tableLen >= 2, "Internal table length should be at least 2 when compactSize() is 1.");
    }

    @Test
    void getNewSet_initialCapacityWhenCompactSizeTen() throws Exception {
        CompactLinkedSetWithSize<Integer> set = new CompactLinkedSetWithSize<>(10);
        Set<Integer> newSet = set.getNewSet();
        int tableLen = getHashMapTableLength(newSet);
        assertTrue(tableLen >= 11, "Internal table length should be at least 11 when compactSize() is 10.");
    }

    @Test
    void getNewSet_throwsWhenCompactSizeMaxValue() {
        CompactLinkedSetWithSize<Integer> set = new CompactLinkedSetWithSize<>(Integer.MAX_VALUE);
        // The LinkedHashSet constructor will overflow the capacity argument.
        assertThrows(IllegalArgumentException.class, set::getNewSet, "Expect IllegalArgumentException when compactSize() is Integer.MAX_VALUE.");
    }

    @Test
    void getNewSet_throwsWhenCompactSizeNegative() {
        CompactLinkedSetWithSize<Integer> set = new CompactLinkedSetWithSize<>(-5);
        assertThrows(IllegalArgumentException.class, set::getNewSet, "Expect IllegalArgumentException when compactSize() is negative.");
    }

    @Test
    void getNewSet_propagatesExceptionFromCompactSize() {
        CompactLinkedSetException<Integer> set = new CompactLinkedSetException<>();
        IllegalStateException ex = assertThrows(IllegalStateException.class, set::getNewSet);
        assertEquals("compactSize failure", ex.getMessage(), "Exception from compactSize() should propagate unchanged.");
    }

    @Test
    void getNewSet_twoSetsAreIndependent() {
        Set<Integer> first = compactLinkedSet.getNewSet();
        Set<Integer> second = compactLinkedSet.getNewSet();
        first.add(1);
        first.add(2);
        assertEquals(2, first.size());
        assertTrue(second.isEmpty(), "Second set must remain empty after mutating the first.");
    }

    @Test
    void getNewSet_originalCompactSetUnaffectedByReturnedSet() {
        // Populate the CompactLinkedSet itself
        compactLinkedSet.add(10);
        compactLinkedSet.add(20);
        // should be empty
        Set<Integer> fresh = compactLinkedSet.getNewSet();
        assertTrue(fresh.isEmpty());
        // Mutate the original CompactLinkedSet
        compactLinkedSet.add(30);
        assertEquals(3, compactLinkedSet.size(), "Original CompactLinkedSet should reflect its own modifications.");
        assertTrue(fresh.isEmpty(), "Set obtained earlier must stay empty regardless of later changes to the original.");
    }

    @Test
    void getNewSet_modifyingReturnedSetDoesNotAffectOriginal() {
        Set<Integer> fresh = compactLinkedSet.getNewSet();
        fresh.add(99);
        assertEquals(0, compactLinkedSet.size(), "Original CompactLinkedSet should stay unchanged when the returned set is modified.");
    }

    @Test
    void getNewSet_supportsRemoval() {
        Set<String> set = compactLinkedSet.getNewSet();
        set.add("item1");
        set.add("item2");
        assertTrue(set.remove("item1"), "Removal should succeed.");
        assertEquals(Set.of("item2"), set);
    }

    @Test
    void getNewSet_addNullThrowsNPE() {
        Set<String> set = compactLinkedSet.getNewSet();
        assertThrows(NullPointerException.class, () -> set.add(null), "Adding null to a LinkedHashSet should throw NullPointerException.");
    }

    @Test
    void getNewSet_preservesInsertionOrderForStrings() {
        Set<String> set = compactLinkedSet.getNewSet();
        set.add("a");
        set.add("b");
        set.add("c");
        List<String> iteration = new ArrayList<>(set);
        assertEquals(List.of("a", "b", "c"), iteration, "Iteration order must match insertion order.");
    }
}
