package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;

// No mocks are required but kept for consistency with the template
@ExtendWith(MockitoExtension.class)
class CompactSet_isEmpty_Test {

    /**
     * Instance used by the majority of tests.
     */
    private CompactSet<String> set;

    /**
     * Runs before each test – creates a fresh, empty CompactSet instance.
     */
    @BeforeEach
    void setupBeforeEach() {
        set = new CompactSet<>();
    }

    /**
     * Runs after each test – clears the reference to help GC and avoid accidental reuse.
     */
    @AfterEach
    void teardownAfterEach() {
        set = null;
    }

    @Test
    @DisplayName("Newly created CompactSet reports empty")
    void testIsEmptyOnNewSet() {
        assertTrue(set.isEmpty(), "A newly created set must be empty");
        assertEquals(0, set.size(), "Size of a newly created set must be 0");
    }

    @Test
    @DisplayName("isEmpty() returns false after adding an element")
    void testIsEmptyAfterAdd() {
        assertTrue(set.isEmpty(), "Pre‑condition: set should be empty");
        set.add("alpha");
        assertFalse(set.isEmpty(), "Set should not be empty after adding an element");
        assertEquals(1, set.size(), "Size must be 1 after a single add");
    }

    @Test
    @DisplayName("isEmpty() returns true after removing the only element")
    void testIsEmptyAfterRemove() {
        set.add("beta");
        assertFalse(set.isEmpty(), "Set should not be empty after add");
        set.remove("beta");
        assertTrue(set.isEmpty(), "Set must be empty after removing its sole element");
        assertEquals(0, set.size(), "Size must be 0 after removal");
    }

    @Test
    @DisplayName("isEmpty() returns true after clear()")
    void testIsEmptyAfterClear() {
        set.addAll(Arrays.asList("one", "two", "three"));
        assertFalse(set.isEmpty(), "Set should not be empty before clear");
        set.clear();
        assertTrue(set.isEmpty(), "Set must be empty after clear()");
        assertEquals(0, set.size(), "Size must be 0 after clear()");
    }

    @Test
    @DisplayName("isEmpty() on a CompactSet constructed from a non‑empty collection")
    void testIsEmptyWithCollectionConstructor() {
        Collection<String> source = Arrays.asList("x", "y", "z");
        CompactSet<String> constructed = new CompactSet<>(source);
        assertFalse(constructed.isEmpty(), "Constructed set should reflect the source collection's elements");
        assertEquals(source.size(), constructed.size(), "Size must match source collection size");
    }

    @Test
    @DisplayName("isEmpty() after removing all elements via iterator")
    void testIsEmptyAfterIteratorRemoval() {
        List<String> elements = Arrays.asList("a", "b", "c");
        set.addAll(elements);
        assertFalse(set.isEmpty(), "Set should contain elements before iterator removal");
        Iterator<String> iterator = set.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            // remove via iterator
            iterator.remove();
        }
        assertTrue(set.isEmpty(), "Set must be empty after all elements are removed via iterator");
        assertEquals(0, set.size(), "Size must be 0 after iterator removal");
    }

    @Test
    @DisplayName("isEmpty() remains true when duplicate adds are ignored")
    void testIsEmptyWithDuplicateAdds() {
        assertTrue(set.isEmpty(), "Pre‑condition: set must start empty");
        assertTrue(set.add("dup"), "First add of a new element must return true");
        assertFalse(set.isEmpty(), "Set should not be empty after first add");
        // second add of same element returns false and does not change emptiness
        assertFalse(set.add("dup"), "Duplicate add must return false");
        assertFalse(set.isEmpty(), "Set should still be non‑empty after duplicate add");
        assertEquals(1, set.size(), "Size must remain 1 after duplicate add");
    }

    @ParameterizedTest(name = "clear() on a set with {0} elements results in empty")
    @ValueSource(ints = { 0, 1, 5, 10 })
    @DisplayName("isEmpty() after clear() with varying initial sizes")
    void testIsEmptyAfterClearWithVariousSizes(int initialSize) {
        // Populate the set with the requested number of distinct elements
        for (int i = 0; i < initialSize; i++) {
            set.add("elem-" + i);
        }
        assertEquals(initialSize, set.size(), "Pre‑condition: set size should match the number of inserted elements");
        assertEquals(initialSize == 0, set.isEmpty(), "Pre‑condition: emptiness should reflect the initial size");
        // Act
        set.clear();
        // Assert
        assertTrue(set.isEmpty(), "Set must be empty after clear()");
        assertEquals(0, set.size(), "Size must be 0 after clear()");
    }

    @Test
    @DisplayName("isEmpty() on a set built via the builder API")
    void testIsEmptyOnBuilderCreatedSet() {
        // Builder can configure ordering, case‑sensitivity etc.; none of that affects emptiness.
        CompactSet<String> built = CompactSet.<String>builder().insertionOrder().caseSensitive(true).build();
        assertTrue(built.isEmpty(), "A freshly built set must be empty");
        assertEquals(0, built.size(), "Size of a freshly built set must be 0");
        built.add("one");
        assertFalse(built.isEmpty(), "Set should not be empty after adding an element via builder‑created instance");
    }
}
