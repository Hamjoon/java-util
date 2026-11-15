package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

class SealableNavigableSet_addAll_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    /* -------------------------------------------------------------
     *  Helper holder class for parameterised arguments
     * ------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final Collection<Integer> collection;

        final boolean expectedResult;

        final String message;

        ArgumentsHolder(Collection<Integer> collection, boolean expectedResult, String message) {
            this.collection = collection;
            this.expectedResult = expectedResult;
            this.message = message;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // start each test with an un‑sealed set containing a few elements
        sealedState = false;
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the sealed flag does not leak between tests
        sealedState = false;
    }

    /* -------------------------------------------------------------
     *  Normal‑operation tests – parameterised from the JSON description
     * ------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> normalOperationProvider() {
        return Stream.of(// Adding brand‑new elements – should modify the set and return true
        new ArgumentsHolder(Arrays.asList(1, 2, 3), true, "addAll should return true when new elements are added"), // Adding an empty collection – no modification, false
        new ArgumentsHolder(Collections.emptyList(), false, "addAll should return false for an empty collection"), // Adding only elements already present – no modification, false
        new ArgumentsHolder(Arrays.asList(20, 30), false, "addAll should return false when collection contains only existing elements"));
    }

    /**
     * Helper to count how many elements of the collection are already present in the set.
     */
    private int countExisting(Collection<Integer> col) {
        int cnt = 0;
        for (Integer i : col) {
            if (set.contains(i))
                cnt++;
        }
        return cnt;
    }

    @Test
    void addAllWhenNotSealedAddsElementsAndReturnsTrue() {
        Collection<Integer> toAdd = Arrays.asList(40, 50, 60);
        boolean result = set.addAll(toAdd);
        assertTrue(result, "addAll should return true when the set changes");
        assertEquals(6, set.size(), "Set size should reflect added elements");
        assertTrue(set.containsAll(toAdd), "Set should contain all newly added elements");
    }

    @Test
    void addAllWhenNotSealedWithExistingElementsReturnsFalse() {
        // 20 and 30 are already present
        Collection<Integer> toAdd = Arrays.asList(20, 30);
        boolean result = set.addAll(toAdd);
        assertFalse(result, "addAll should return false when nothing is added");
        assertEquals(3, set.size(), "Set size should remain unchanged");
    }

    @Test
    void addAllWhenNotSealedWithEmptyCollectionReturnsFalse() {
        Collection<Integer> empty = Collections.emptyList();
        boolean result = set.addAll(empty);
        assertFalse(result, "addAll should return false for an empty collection");
        assertEquals(3, set.size(), "Set size should remain unchanged");
    }

    @Test
    void addAllWhenNotSealedContainsNull() {
        Collection<Integer> withNull = Arrays.asList(null, 70);
        boolean result = set.addAll(withNull);
        assertTrue(result, "addAll should return true when null is added");
        assertTrue(set.contains(null), "Set should contain null after addAll");
        assertTrue(set.contains(70), "Set should contain newly added non‑null element");
        assertEquals(5, set.size());
    }

    @Test
    void addAllWhenSealedThrowsUnsupportedOperationException() {
        // seal the set before invoking addAll
        sealedState = true;
        Collection<Integer> toAdd = Arrays.asList(80, 90);
        assertThrows(UnsupportedOperationException.class, () -> set.addAll(toAdd), "addAll must throw when the set is sealed");
    }

    @Test
    void addAllPartialOverlapWhenSealedThrowsAndLeavesSetUnchanged() {
        sealedState = true;
        Collection<Integer> toAdd = Arrays.asList(20, 100);
        // Expect exception before any modification
        assertThrows(UnsupportedOperationException.class, () -> set.addAll(toAdd));
        // Verify original content is untouched
        assertEquals(3, set.size());
        assertFalse(set.contains(100));
        assertTrue(set.contains(20));
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("normalOperationProvider")
    void addAllNormalOperation(ArgumentsHolder holder) {
        Collection<Integer> toAdd = holder.collection;
        boolean result = set.addAll(toAdd);
        assertEquals(holder.expectedResult, result, holder.message);
        // Verify the final size matches expectations
        int expectedSize = holder.expectedResult ? 3 + toAdd.size() - countExisting(toAdd) : 3;
        assertEquals(expectedSize, set.size(), "Set size should be " + expectedSize + " after addAll");
    }

    @Test
    void addAllWithNullCollectionPropagatesNullPointerException() {
        // The underlying NavigableSet.addAll(null) throws NPE – we expect it to bubble up.
        assertThrows(NullPointerException.class, () -> set.addAll(null), "Passing a null collection should result in NullPointerException");
    }

    @Test
    void addAllWithWrongElementTypeThrowsClassCastException() {
        // Create a normal TreeSet<Integer> and replace the private navSet via reflection.
        NavigableSet<String> stringSet = new TreeSet<>(Arrays.asList("a", "b"));
        // Use reflection to inject the mismatched set.
        replaceNavSet(set, stringSet);
        // Adding a collection of Integers to a set that expects Strings should cause CCE.
        Collection<Integer> wrongType = Arrays.asList(1, 2);
        assertThrows(ClassCastException.class, () -> set.addAll(wrongType), "Adding elements of the wrong type must throw ClassCastException");
    }

    @Test
    void addAllWithNullElementWhenUnderlyingSetDisallowsNullThrowsNPE() {
        // TreeSet (natural ordering) does not allow null – replace navSet with such a set.
        NavigableSet<Integer> treeSet = new TreeSet<>(Arrays.asList(10, 20, 30));
        replaceNavSet(set, treeSet);
        Collection<Integer> withNull = Arrays.asList(null, 40);
        assertThrows(NullPointerException.class, () -> set.addAll(withNull), "Adding a null element to a TreeSet should throw NullPointerException");
    }

    @Test
    void addAllWhenUnderlyingSetIsUnmodifiablePropagatesUnsupportedOperationException() {
        // Wrap a mutable TreeSet with Collections.unmodifiableNavigableSet
        NavigableSet<Integer> modifiable = new TreeSet<>(Arrays.asList(10, 20, 30));
        NavigableSet<Integer> unmodifiable = Collections.unmodifiableNavigableSet(modifiable);
        replaceNavSet(set, unmodifiable);
        Collection<Integer> toAdd = Arrays.asList(40, 50);
        assertThrows(UnsupportedOperationException.class, () -> set.addAll(toAdd), "addAll should propagate UnsupportedOperationException from an unmodifiable underlying set");
    }

    /* -------------------------------------------------------------
     *  Utility method to replace the private 'navSet' field via reflection.
     * ------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private <T> void replaceNavSet(SealableNavigableSet<T> target, NavigableSet<T> newNavSet) {
        try {
            Field f = SealableNavigableSet.class.getDeclaredField("navSet");
            f.setAccessible(true);
            f.set(target, newNavSet);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to inject mock NavigableSet", e);
        }
    }
}
