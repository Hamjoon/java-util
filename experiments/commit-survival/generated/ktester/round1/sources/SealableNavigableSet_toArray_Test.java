package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

class SealableNavigableSet_toArray_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState;

    private Supplier<Boolean> sealedSupplier;

    /**
     * Simple immutable holder for a test scenario.
     */
    private static final class TestScenario {

        final String description;

        final List<?> elements;

        // may be null for natural ordering
        final Comparator<?> comparator;

        final Object[] expectedArray;

        TestScenario(String description, List<?> elements, Comparator<?> comparator, Object[] expectedArray) {
            this.description = description;
            this.elements = Collections.unmodifiableList(new ArrayList<>(elements));
            this.comparator = comparator;
            this.expectedArray = expectedArray;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        sealedSupplier = () -> sealedState;
        set = new SealableNavigableSet<>(sealedSupplier);
        // Populate with a deterministic ordering (natural order for Integer)
        set.add(10);
        set.add(20);
        set.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        set = null;
    }

    /**
     * Provides a stream of arguments for {@link #toArrayVariousScenarios(TestScenario)}.
     */
    private static Stream<TestScenario> toArrayScenariosProvider() {
        // 1. Empty set
        TestScenario empty = new TestScenario("Empty set", Collections.emptyList(), null, new Object[] {});
        // 2. Single element
        TestScenario single = new TestScenario("Single element", Collections.singletonList(42), null, new Object[] { 42 });
        // 3. Multiple comparable elements (natural order)
        TestScenario multiple = new TestScenario("Multiple elements – natural order", Arrays.asList(5, 1, 3, 2, 4), null, new Object[] { 1, 2, 3, 4, 5 });
        // 4. Custom comparator (order by string length)
        TestScenario customComparator = new TestScenario("Custom comparator – length order", Arrays.asList("bbbb", "a", "ccc", "dd"), Comparator.comparingInt(String::length), new Object[] { "a", "dd", "ccc", "bbbb" });
        // 5. Set containing null (comparator that permits null)
        Comparator<String> nullFriendly = (s1, s2) -> {
            if (s1 == null && s2 == null)
                return 0;
            if (s1 == null)
                return -1;
            if (s2 == null)
                return 1;
            return s1.compareTo(s2);
        };
        TestScenario nullElement = new TestScenario("Null element with null‑friendly comparator", Arrays.asList(null, "alpha"), nullFriendly, new Object[] { null, "alpha" });
        // 6. Large set (10 integers)
        List<Integer> tenInts = new ArrayList<>();
        for (int i = 0; i < 10; i++) tenInts.add(i);
        TestScenario largeSet = new TestScenario("Large set of 10 integers", tenInts, null, tenInts.toArray());
        // 7. Descending view (reverse order)
        TestScenario descendingView = new TestScenario("Descending view", Arrays.asList(1, 2, 3), Comparator.reverseOrder(), new Object[] { 3, 2, 1 });
        return Stream.of(empty, single, multiple, customComparator, nullElement, largeSet, descendingView);
    }

    @Test
    void toArrayOnEmptySetReturnsEmptyArray() {
        SealableNavigableSet<Integer> empty = new SealableNavigableSet<>(sealedSupplier);
        Object[] result = empty.toArray();
        assertNotNull(result, "Resulting array should not be null");
        assertEquals(0, result.length, "Empty set should produce an empty array");
    }

    @Test
    void toArrayReturnsElementsInSortedOrder() {
        // natural order: 10, 20, 30
        set.add(10);
        set.add(20);
        set.add(30);
        Object[] result = set.toArray();
        assertNotNull(result);
        assertEquals(3, result.length);
        assertArrayEquals(new Object[] { 10, 20, 30 }, result);
    }

    @Test
    void toArrayDefensiveCopyDoesNotAffectOriginalSet() {
        set.add(10);
        set.add(20);
        set.add(30);
        Object[] result = set.toArray();
        // Mutate the returned array
        result[0] = 999;
        // The set must remain unchanged
        assertEquals(10, set.first(), "First element of the set should still be 10");
        assertEquals(3, set.size(), "Set size must stay the same after mutating the returned array");
    }

    @Test
    void toArrayWorksWhenSealed() {
        // simulate sealed (read‑only) state
        sealedState = true;
        set.add(10);
        set.add(20);
        set.add(30);
        assertDoesNotThrow(() -> {
            Object[] result = set.toArray();
            assertEquals(3, result.length);
        });
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("toArrayScenariosProvider")
    void toArrayVariousScenarios(TestScenario scenario) throws Exception {
        // Build the underlying NavigableSet according to the scenario description.
        NavigableSet<?> underlying;
        if (scenario.comparator != null) {
            // Comparator supplied – use a TreeSet with that comparator.
            underlying = new TreeSet<>(scenario.comparator);
        } else {
            // Natural ordering – TreeSet without explicit comparator.
            underlying = new TreeSet<>();
        }
        // Populate the underlying set.
        underlying.addAll(scenario.elements);
        // Replace the private navSet field via reflection.
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        navSetField.set(set, underlying);
        // Act
        Object[] result = set.toArray();
        // Assert: array content matches expected
        assertArrayEquals(scenario.expectedArray, result, () -> "Scenario '" + scenario.description + "' produced unexpected array content.");
        // Assert: the returned array is a fresh instance (defensive copy)
        assertNotSame(result, underlying.toArray(), "toArray() should return a new array instance, not the underlying set's internal array.");
        // Additional generic checks for all scenarios
        assertNotNull(result, "Resulting array must not be null");
        assertEquals(scenario.expectedArray.length, result.length, "Array length mismatch for scenario: " + scenario.description);
        assertEquals(Object[].class, result.getClass(), "Runtime type of the returned array must be exactly Object[]");
    }

    /**
     * Verifies that successive calls to {@code toArray()} return distinct array instances
     * and reflect modifications performed on the underlying set.
     */
    @Test
    void toArrayReflectsModificationsAndReturnsDistinctInstances() throws Exception {
        // Populate with three elements.
        set.add(1);
        set.add(2);
        set.add(3);
        Object[] first = set.toArray();
        assertArrayEquals(new Object[] { 1, 2, 3 }, first);
        // Remove an element directly via the underlying set.
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        @SuppressWarnings("unchecked")
        NavigableSet<Integer> underlying = (NavigableSet<Integer>) navSetField.get(set);
        underlying.remove(2);
        Object[] second = set.toArray();
        assertArrayEquals(new Object[] { 1, 3 }, second);
        // Ensure the two arrays are different objects.
        assertNotSame(first, second, "Each call to toArray() must produce a new array instance");
    }

    /**
     * Demonstrates the type‑safety boundary: the underlying raw set may contain an
     * element that is not assignable to the generic type {@code E}. The method still
     * returns an {@code Object[]} but casting the element to {@code E} triggers a
     * {@link ClassCastException}.
     */
    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void toArrayWithIncompatibleElementCausesClassCastExceptionOnCast() throws Exception {
        // Construct a raw SealableNavigableSet (no generic type information).
        SealableNavigableSet rawSet = new SealableNavigableSet(sealedSupplier);
        // Replace its navSet with a raw TreeSet containing a String.
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        Set rawUnderlying = new TreeSet();
        rawUnderlying.add("not-an-integer");
        navSetField.set(rawSet, rawUnderlying);
        // The method itself must succeed and return an Object[].
        Object[] result = rawSet.toArray();
        assertEquals(1, result.length);
        assertEquals("not-an-integer", result[0]);
        // Attempting to treat the element as Integer should fail.
        assertThrows(ClassCastException.class, () -> {
            // deliberate bad cast
            Integer i = (Integer) result[0];
        });
    }
}
