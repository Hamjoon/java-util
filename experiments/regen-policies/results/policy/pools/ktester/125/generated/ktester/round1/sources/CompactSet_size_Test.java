package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Set;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactSet_size_Test {

    /**
     * Instance used by most test methods.
     */
    private CompactSet<Integer> set;

    /**
     * Create a fresh {@link CompactSet} before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        set = new CompactSet<>();
    }

    /**
     * Clear the instance after each test – not strictly required but keeps the
     *  test class tidy if additional state is added later.
     */
    @AfterEach
    void teardownAfterEach() {
        // The set does not expose a clear method via the test field,
        // but we can simply discard the reference; the GC will reclaim it.
        set = null;
    }

    /**
     * Verify that a newly‑created set reports size 0.
     */
    @Test
    void size_isZeroOnNewSet() {
        assertTrue(set.isEmpty(), "New set should be empty");
        assertEquals(0, set.size(), "Size of a new set must be 0");
    }

    /**
     * Adding distinct elements should increase the size accordingly.
     */
    @Test
    void size_increasesWithDistinctAdds() {
        assertTrue(set.add(10));
        assertEquals(1, set.size());
        assertTrue(set.add(20));
        assertEquals(2, set.size());
        assertTrue(set.add(30));
        assertEquals(3, set.size());
    }

    /**
     * Adding a duplicate element must not change the size.
     */
    @Test
    void size_unchangedWhenAddingDuplicate() {
        assertTrue(set.add(42));
        assertEquals(1, set.size());
        // Duplicate add returns false and size stays the same
        assertFalse(set.add(42));
        assertEquals(1, set.size());
    }

    /**
     * Removing elements updates the size correctly.
     */
    @Test
    void size_decreasesAfterRemovals() {
        set.add(1);
        set.add(2);
        set.add(3);
        assertEquals(3, set.size());
        assertTrue(set.remove(2));
        assertEquals(2, set.size());
        assertTrue(set.remove(1));
        assertEquals(1, set.size());
        assertTrue(set.remove(3));
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }

    /**
     * Size works with the builder‑configured set (e.g., case‑insensitive, sorted).
     */
    @Test
    void size_withBuilderConfiguration() {
        CompactSet<String> ordered = CompactSet.<String>builder().caseSensitive(// case‑insensitive
        false).// sorted ordering
        sortedOrder().compactSize(// custom compact size
        50).build();
        assertEquals(0, ordered.size());
        ordered.add("Apple");
        ordered.add("Banana");
        // duplicate in case‑insensitive mode
        ordered.add("apple");
        // Only two distinct values should be present
        assertEquals(2, ordered.size());
        // Verify that removal also updates size
        // case‑insensitive removal
        assertTrue(ordered.remove("APPLE"));
        assertEquals(1, ordered.size());
    }

    /**
     * Size should remain consistent after many insertions, crossing the internal
     *  compact‑size threshold. This does not check internal structure, only the
     *  public contract.
     */
    @Test
    void size_afterManyInsertions() {
        // The CompactSet API does not expose the threshold directly, but we can
        // use a reasonable number to ensure we cross it.
        int totalElements = 100;
        for (int i = 0; i < totalElements; i++) {
            assertTrue(set.add(i));
        }
        assertEquals(totalElements, set.size());
        // Remove a few elements and verify size adjusts
        for (int i = 0; i < 5; i++) {
            assertTrue(set.remove(i));
        }
        assertEquals(totalElements - 5, set.size());
    }

    /**
     * Removing a non‑existent element must leave the size unchanged.
     */
    @Test
    void size_afterRemovingNonExistentElement() {
        set.add(100);
        set.add(200);
        assertEquals(2, set.size());
        // Attempt to remove an element that is not present
        assertFalse(set.remove(999));
        assertEquals(2, set.size(), "Size should stay 2 after trying to remove a missing element");
    }

    /**
     * Calling {@code clear()} must reset the size to zero.
     */
    @Test
    void size_afterClear() {
        set.add(1);
        set.add(2);
        set.add(3);
        assertEquals(3, set.size());
        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty(), "Set should be empty after clear()");
    }

    /**
     * {@code addAll} with a collection that contains duplicates should only
     *  count distinct elements.
     */
    @Test
    void size_afterAddAllWithDuplicates() {
        Collection<String> source = Arrays.asList("a", "b", "c", "a", "b");
        CompactSet<String> stringSet = new CompactSet<>();
        assertTrue(stringSet.addAll(source));
        assertEquals(3, stringSet.size(), "Only three distinct elements should be stored");
    }

    /**
     * Builder can pre‑populate a set; verify that the size reflects the initial elements.
     */
    @Test
    void size_afterBuilderWithInitialElements() {
        // The real builder API is not fully documented here; we assume it provides
        // an `add` method for initial population.
        CompactSet<String> built = CompactSet.<String>builder().add("one").add("two").add("three").add("four").add("five").build();
        assertEquals(5, built.size(), "Builder‑created set should contain five distinct elements");
    }

    /**
     * Adding a {@code null} element (if supported) should increase size by one.
     */
    @Test
    void size_afterAddingNull() {
        CompactSet<Object> nullableSet = new CompactSet<>();
        assertTrue(nullableSet.add(null), "Adding null should succeed if supported");
        assertEquals(1, nullableSet.size(), "Size should be 1 after adding null");
    }

    /**
     * When the internal map reference is {@code null}, {@code size()} must throw NPE.
     */
    @Test
    void size_whenInternalMapIsNull() throws Exception {
        // Use reflection to set the private final field 'map' to null.
        Field mapField = CompactSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        // Expect a NullPointerException when size() dereferences the null map.
        assertThrows(NullPointerException.class, set::size, "Calling size() with a null internal map should throw NPE");
    }

    /**
     * If the internal map's {@code size()} throws {@link UnsupportedOperationException},
     *  the exception must propagate.
     */
    @Test
    void size_whenInternalMapSizeThrowsUnsupportedOperationException() throws Exception {
        @SuppressWarnings("unchecked")
        CompactMap<Integer, Object> mockMap = mock(CompactMap.class);
        when(mockMap.size()).thenThrow(new UnsupportedOperationException("size not supported"));
        // Inject the mock map via reflection.
        Field mapField = CompactSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, mockMap);
        // Verify that the exception bubbles up.
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, set::size, "size() should propagate UnsupportedOperationException from the map");
        assertEquals("size not supported", ex.getMessage());
    }

    /**
     * If the internal map's {@code size()} throws {@link IllegalStateException},
     *  the exception must propagate.
     */
    @Test
    void size_whenInternalMapSizeThrowsIllegalStateException() throws Exception {
        @SuppressWarnings("unchecked")
        CompactMap<Integer, Object> mockMap = mock(CompactMap.class);
        when(mockMap.size()).thenThrow(new IllegalStateException("corrupted state"));
        // Inject the mock map via reflection.
        Field mapField = CompactSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, mockMap);
        // Verify propagation.
        IllegalStateException ex = assertThrows(IllegalStateException.class, set::size, "size() should propagate IllegalStateException from the map");
        assertEquals("corrupted state", ex.getMessage());
    }
}
