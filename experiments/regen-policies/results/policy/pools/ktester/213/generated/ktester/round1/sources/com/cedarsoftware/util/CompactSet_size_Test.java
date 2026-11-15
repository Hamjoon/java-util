package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

// @ExtendWith(MockitoExtension.class)
class CompactSet_size_Test {

    /**
     * Instance under test – a fresh CompactSet for each test method.
     */
    private Set<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the public no‑arg constructor which creates an empty set.
        set = new CompactSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector; not strictly required but keeps the pattern clear.
        set = null;
    }

    /**
     * Removes all elements from the supplied set using its iterator.
     *
     * @param s the set to clear
     */
    private void clearViaIterator(Set<String> s) {
        Iterator<String> it = s.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertTrue(s.isEmpty(), "Set should be empty after iterator‑based clear");
    }

    @Test
    @DisplayName("size() on a newly created CompactSet returns 0")
    void testSizeEmpty() {
        assertTrue(set.isEmpty(), "New set should be empty");
        assertEquals(0, set.size(), "Size of an empty set must be 0");
    }

    @Test
    @DisplayName("size() reflects the number of added elements")
    void testSizeAfterAdd() {
        assertTrue(set.add("alpha"));
        assertEquals(1, set.size(), "Size should be 1 after adding one element");
        assertTrue(set.add("beta"));
        assertEquals(2, set.size(), "Size should be 2 after adding a second distinct element");
        // Adding a duplicate must not change the size.
        assertFalse(set.add("alpha"));
        assertEquals(2, set.size(), "Size must stay unchanged when adding a duplicate");
    }

    @Test
    @DisplayName("size() updates correctly after removals")
    void testSizeAfterRemove() {
        // Populate the set.
        set.add("one");
        set.add("two");
        set.add("three");
        assertEquals(3, set.size(), "Pre‑condition: size should be 3 after three distinct adds");
        // Remove an existing element.
        assertTrue(set.remove("two"));
        assertEquals(2, set.size(), "Size should decrement after successful removal");
        // Attempt to remove a non‑existent element – size must stay the same.
        assertFalse(set.remove("non‑existent"));
        assertEquals(2, set.size(), "Size must remain unchanged when removing a missing element");
        // Clear remaining elements via iterator and verify size becomes 0.
        clearViaIterator(set);
        assertEquals(0, set.size(), "Size must be 0 after clearing the set");
    }

    @Test
    @DisplayName("size() - basic behavior on empty set")
    void testSizeBasicEmpty() {
        // The set is freshly created in @BeforeEach, so it must be empty.
        assertEquals(0, set.size(), "A newly created CompactSet should report size 0.");
    }

    @Test
    @DisplayName("size() after adding distinct elements")
    void testSizeAfterAddingThreeDistinct() {
        assertTrue(set.add("one"));
        assertTrue(set.add("two"));
        assertTrue(set.add("three"));
        assertEquals(3, set.size(), "After adding three different elements size() should be 3.");
    }

    @Test
    @DisplayName("size() after adding duplicate elements")
    void testSizeAfterAddingDuplicate() {
        assertTrue(set.add("dup"));
        // second add returns false because the element already exists
        assertFalse(set.add("dup"));
        assertEquals(1, set.size(), "Adding the same element twice should result in size 1.");
    }

    @Test
    @DisplayName("size() after removing an element")
    void testSizeAfterRemovingOneOfTwo() {
        assertTrue(set.add("first"));
        assertTrue(set.add("second"));
        assertTrue(set.remove("first"));
        assertEquals(1, set.size(), "After adding two elements and removing one, size() should be 1.");
    }

    @Test
    @DisplayName("size() after clear()")
    void testSizeAfterClear() {
        set.add("a");
        set.add("b");
        set.add("c");
        set.clear();
        assertEquals(0, set.size(), "After clear() size() must be 0.");
    }

    @Test
    @DisplayName("size() after addAll(Collection) with distinct elements")
    void testSizeAfterAddAllDistinct() {
        Collection<String> src = List.of("one", "two", "three", "four");
        assertTrue(set.addAll(src));
        assertEquals(4, set.size(), "addAll() of four distinct elements should give size 4.");
    }

    @Test
    @DisplayName("size() after addAll(Collection) containing duplicates")
    void testSizeAfterAddAllWithDuplicates() {
        // collection contains three distinct values, one of them appears twice
        Collection<String> src = List.of("a", "b", "a", "c");
        assertTrue(set.addAll(src));
        assertEquals(3, set.size(), "addAll() with duplicates should store only unique elements (3).");
    }

    @Test
    @DisplayName("size() with case‑insensitive builder")
    void testSizeCaseInsensitiveBuilder() {
        // Build a case‑insensitive set
        CompactSet<String> ciSet = CompactSet.<String>builder().caseSensitive(false).build();
        assertTrue(ciSet.add("A"));
        // Adding the lower‑case version must be considered a duplicate
        assertFalse(ciSet.add("a"));
        assertEquals(1, ciSet.size(), "Case‑insensitive set should treat \"A\" and \"a\" as the same element.");
    }

    @Test
    @DisplayName("size() after withConfig changes ordering only")
    void testSizeAfterWithConfigOrderingChange() {
        set.add("first");
        set.add("second");
        assertEquals(2, set.size(), "Pre‑condition: size should be 2 before re‑configuring.");
        // Change ordering configuration – element count must stay the same
        Map<String, Object> cfg = new HashMap<>();
        cfg.put("ordering", "tree");
        ((CompactSet<String>) set).withConfig(cfg);
        assertEquals(2, set.size(), "Changing ordering via withConfig must not affect element count.");
    }

    @Test
    @DisplayName("size() respects initial capacity but not count")
    void testSizeWithLargeInitialCapacity() {
        // Builder with a large initial capacity (size hint)
        CompactSet<String> bigCapSet = CompactSet.<String>builder().compactSize(100).build();
        assertTrue(bigCapSet.add("x"));
        assertTrue(bigCapSet.add("y"));
        assertEquals(2, bigCapSet.size(), "Size should reflect actual elements, not the initial capacity.");
    }

    @Test
    @DisplayName("size() unchanged after removing non‑existent element")
    void testSizeUnchangedWhenRemovingMissing() {
        set.add("alpha");
        set.add("beta");
        assertEquals(2, set.size(), "Pre‑condition: size should be 2.");
        assertFalse(set.remove("gamma"));
        assertEquals(2, set.size(), "Removing a non‑existent element must leave size unchanged.");
    }

    @Test
    @DisplayName("size() when internal map is null throws NPE")
    void testSizeWhenInternalMapIsNull() throws Exception {
        // Obtain the private final field 'map' from CompactSet
        Field mapField = CompactSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        // Remove the final modifier (necessary on some JVMs)
        // Note: this hack works for testing purposes only.
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(mapField, mapField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        // Set the field to null on the current instance
        mapField.set(set, null);
        // The size() implementation directly delegates to map.size(),
        // so a NullPointerException is expected.
        assertThrows(NullPointerException.class, () -> set.size(), "Calling size() when internal map is null should throw NullPointerException.");
    }
}