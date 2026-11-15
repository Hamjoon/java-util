package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Iterator;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactSet_clear_Test {

    /**
     * The CompactSet instance used in each test.
     */
    private CompactSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh instance before each test.
        set = new CompactSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set to help the GC, though not strictly necessary.
        if (set != null) {
            set.clear();
        }
    }

    /**
     * Supplies different element arrays for the parameterized test.
     */
    static Stream<String[]> elementsProvider() {
        return Stream.of(// already covered by clearOnEmptySet, but kept for completeness
        new String[] {}, new String[] { "a" }, new String[] { "a", "b", "c" }, IntStream.rangeClosed(1, 50).mapToObj(i -> "val-" + i).toArray(String[]::new));
    }

    /**
     * Verify that clearing an already‑empty set leaves it empty.
     */
    @Test
    void clearOnEmptySet() {
        assertTrue(set.isEmpty(), "Set should be empty initially");
        assertEquals(0, set.size(), "Initial size should be 0");
        assertFalse(set.iterator().hasNext(), "Iterator should have no elements");
        set.clear();
        assertTrue(set.isEmpty(), "Set should remain empty after clear");
        assertEquals(0, set.size(), "Size should still be 0 after clear");
        assertFalse(set.iterator().hasNext(), "Iterator should still have no elements");
    }

    /**
     * Verify that clear removes elements when the set is using the lightweight representation
     * (size below {@code compactSize()}).
     */
    @Test
    void clearAfterAddingFewElements() {
        set.add("alpha");
        set.add("beta");
        assertFalse(set.isEmpty(), "Set should not be empty after additions");
        assertEquals(2, set.size(), "Size should reflect added elements");
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("beta"));
        // iterator should see the two elements
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        // we don't care about order, just that two elements are present
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(2, count, "Iterator should have returned two elements");
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size(), "Size should be 0 after clear");
        assertFalse(set.contains("alpha"));
        assertFalse(set.contains("beta"));
        assertFalse(set.iterator().hasNext(), "Iterator should report no next element after clear");
    }

    /**
     * Verify that clear works when the set has grown beyond {@code compactSize()}
     * and therefore uses an internal {@link java.util.Set} implementation.
     */
    @Test
    void clearAfterExceedingCompactSize() {
        // Fill the set with more elements than the default compactSize (80).
        IntStream.rangeClosed(1, 85).forEach(i -> set.add("item-" + i));
        assertEquals(85, set.size(), "Set should contain 85 elements");
        assertFalse(set.isEmpty(), "Set should not be empty before clear");
        // Spot‑check a few elements.
        assertTrue(set.contains("item-1"));
        assertTrue(set.contains("item-42"));
        assertTrue(set.contains("item-85"));
        set.clear();
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size(), "Size should be 0 after clear");
        assertFalse(set.contains("item-1"));
        assertFalse(set.contains("item-42"));
        assertFalse(set.contains("item-85"));
        assertFalse(set.iterator().hasNext(), "Iterator should have no elements after clear");
    }

    /**
     * Verify that the set can be reused after a clear operation.
     */
    @Test
    void addAfterClear() {
        set.add("first");
        set.clear();
        // After clearing, the set should accept new elements normally.
        set.add("second");
        assertEquals(1, set.size(), "Size should be 1 after adding post‑clear");
        assertTrue(set.contains("second"));
        assertFalse(set.contains("first"));
    }

    /**
     * Parameterized test that adds a variable number of elements, clears, and checks iterator behaviour.
     */
    @ParameterizedTest(name = "{index}: clear after adding {0} elements")
    @MethodSource("elementsProvider")
    void clearAfterAddingVariableElements(String[] elements) {
        // Add each supplied element.
        for (String e : elements) {
            set.add(e);
        }
        assertEquals(elements.length, set.size(), "Size should match number of added elements");
        assertFalse(set.isEmpty(), "Set should not be empty before clear");
        // Verify iterator reports exactly the same number of elements.
        int iterCount = 0;
        for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
            it.next();
            iterCount++;
        }
        assertEquals(elements.length, iterCount, "Iterator should return the same number of elements as added");
        set.clear();
        // Post‑clear assertions.
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertEquals(0, set.size(), "Size should be 0 after clear");
        assertFalse(set.iterator().hasNext(), "Iterator should have no elements after clear");
        for (String e : elements) {
            assertFalse(set.contains(e), "Set should not contain element '" + e + "' after clear");
        }
    }

    /**
     * Verify that the set can be reused after a clear operation, including handling of a {@code null} element.
     */
    @Test
    void addAfterClearIncludingNull() {
        // First add a normal element and a null.
        set.add("first");
        set.add(null);
        assertEquals(2, set.size(), "Size should be 2 before clear (including null)");
        assertTrue(set.contains("first"));
        assertTrue(set.contains(null));
        set.clear();
        // After clearing, the set should accept new elements normally.
        set.add("second");
        set.add(null);
        assertEquals(2, set.size(), "Size should be 2 after adding two elements post‑clear");
        assertTrue(set.contains("second"));
        assertTrue(set.contains(null));
        assertFalse(set.contains("first"));
    }
}
