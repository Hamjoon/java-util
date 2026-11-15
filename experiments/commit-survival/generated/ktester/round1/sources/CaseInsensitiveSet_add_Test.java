package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_add_Test {

    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the no‑arg constructor which creates an internal CaseInsensitiveMap.
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to guarantee isolation between tests (not strictly required because a new
        // instance is created for each test, but it documents the intent).
        set.clear();
    }

    /**
     * Clears the set (if any elements are present) and injects a new map instance
     * into the private {@code map} field using reflection.
     *
     * @param newMap the map to be used as the backing store
     */
    private void setClearAndInjectMap(Map<?, ?> newMap) throws Exception {
        // Ensure the set is empty before swapping the map.
        set.clear();
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, newMap);
    }

    @Test
    @DisplayName("add(E) returns true and increases size when element is new")
    void addNewElement() {
        // Given
        String element = "Alpha";
        // When
        boolean added = set.add(element);
        // Then
        assertTrue(added, "add should return true for a new element");
        assertEquals(1, set.size(), "size should be 1 after adding a new element");
        assertTrue(set.contains(element), "set should contain the newly added element");
    }

    @Test
    @DisplayName("add(E) returns false and does not change size when element already exists")
    void addDuplicateElement() {
        // Given
        String element = "Beta";
        // first insertion
        set.add(element);
        // When
        // duplicate insertion
        boolean addedAgain = set.add(element);
        // Then
        assertFalse(addedAgain, "add should return false for a duplicate element");
        assertEquals(1, set.size(), "size should remain unchanged after attempting to add duplicate");
    }

    @Test
    @DisplayName("String elements are compared case‑insensitively")
    void addCaseInsensitiveStrings() {
        // Given
        String upper = "Gamma";
        String lower = "gamma";
        // When
        boolean firstAdd = set.add(upper);
        boolean secondAdd = set.add(lower);
        // Then
        assertTrue(firstAdd, "first insertion should succeed");
        assertFalse(secondAdd, "second insertion with different case should be considered duplicate");
        assertEquals(1, set.size(), "set size should be 1 after case‑insensitive duplicate insertion");
        // The original case should be preserved when iterating
        Iterator<Object> it = set.iterator();
        assertTrue(it.hasNext(), "iterator should have an element");
        assertEquals(upper, it.next(), "original case of the stored element should be preserved");
    }

    @Test
    @DisplayName("Non‑String elements are handled normally (case‑insensitivity does not apply)")
    void addNonStringElements() {
        // Given
        Integer intOne = 1;
        Integer intTwo = 2;
        // When
        boolean addedOne = set.add(intOne);
        boolean addedTwo = set.add(intTwo);
        boolean addedOneAgain = set.add(intOne);
        // Then
        assertTrue(addedOne, "first addition of Integer should succeed");
        assertTrue(addedTwo, "addition of a different Integer should succeed");
        assertFalse(addedOneAgain, "adding the same Integer again should be duplicate");
        assertEquals(2, set.size(), "set should contain exactly two distinct non‑String elements");
        assertTrue(set.containsAll(Arrays.asList(intOne, intTwo)));
    }

    @Test
    @DisplayName("Adding null element behaves like a normal Set (allowed if underlying map permits)")
    void addNullElement() {
        // Given
        // (no pre‑condition)
        // When
        boolean added = set.add(null);
        boolean addedAgain = set.add(null);
        // Then
        // The behavior depends on the underlying map implementation; CaseInsensitiveMap
        // delegates to a regular HashMap which permits a single null key.
        assertTrue(added, "first insertion of null should succeed");
        assertFalse(addedAgain, "second insertion of null should be considered duplicate");
        assertEquals(1, set.size(), "set should contain exactly one null element");
        assertTrue(set.contains(null), "set should report containing null");
    }

    @Test
    @DisplayName("add(E) – case‑insensitivity and mixed type handling sequence")
    void addCaseInsensitiveAndMixedTypesSequence() {
        // 1. Add a new element "Hello"
        assertTrue(set.add("Hello"));
        // 2. Duplicate with different case
        assertFalse(set.add("hello"));
        // 3. Another case variant
        assertFalse(set.add("HELLO"));
        // 4. Distinct string "World"
        assertTrue(set.add("World"));
        // 5. Integer 5
        assertTrue(set.add(5));
        // 6. Duplicate Integer 5
        assertFalse(set.add(5));
        // 7. Different Integer instance with same value
        assertFalse(set.add(new Integer(5)));
        // 8. Empty string
        assertTrue(set.add(""));
        // 9. Whitespace‑only string
        assertTrue(set.add("   "));
        // 10. Very long string
        String longString = "A very long string that exceeds typical buffer sizes to test handling of large inputs in the set implementation";
        assertTrue(set.add(longString));
        // 11. Re‑add "Hello" after many insertions
        assertFalse(set.add("Hello"));
        // 12. Add null (first time)
        assertTrue(set.add(null));
        // 13. Add "world" (case‑insensitive duplicate of "World")
        assertFalse(set.add("world"));
        // Final size should reflect the unique keys:
        // "Hello", "World", 5, "", "   ", longString, null  => 7 entries
        assertEquals(7, set.size(), "final set size should be 7 unique elements");
        assertTrue(set.containsAll(Arrays.asList("Hello", "World", 5, "", "   ", longString, null)));
    }

    @Test
    @DisplayName("add(E) throws NullPointerException when underlying map disallows null keys")
    void addNullWhenMapDisallowsNull() throws Exception {
        // Replace internal map with a ConcurrentSkipListMap (null keys not permitted)
        set = new CaseInsensitiveSet<>();
        setClearAndInjectMap(new ConcurrentSkipListMap<>());
        assertThrows(NullPointerException.class, () -> set.add(null), "Adding null to a ConcurrentSkipListMap should throw NullPointerException");
    }

    @Test
    @DisplayName("add(E) throws ClassCastException when map comparator cannot compare mixed key types")
    void addIncompatibleKeyTypes() throws Exception {
        // Use a ConcurrentSkipListMap with case‑insensitive String comparator.
        // This comparator will throw ClassCastException when comparing a String with a non‑String.
        ConcurrentSkipListMap<Object, Object> map = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);
        set = new CaseInsensitiveSet<>();
        setClearAndInjectMap(map);
        // First insertion with a String works
        assertTrue(set.add("FirstString"));
        // Second insertion with an Integer should fail
        assertThrows(ClassCastException.class, () -> set.add(123), "Adding an Integer after a String should cause ClassCastException due to incomparable types");
    }

    @Test
    @DisplayName("add(E) throws UnsupportedOperationException when underlying map is unmodifiable")
    void addWithUnmodifiableMap() throws Exception {
        Map<Object, Object> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        set = new CaseInsensitiveSet<>();
        setClearAndInjectMap(unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> set.add("any"), "Adding to an unmodifiable map should throw UnsupportedOperationException");
    }

    @Test
    @DisplayName("add(E) throws NullPointerException when internal map reference is null")
    void addWhenInternalMapIsNull() throws Exception {
        // Force the private 'map' field to null via reflection.
        set = new CaseInsensitiveSet<>();
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        assertThrows(NullPointerException.class, () -> set.add("test"), "Invoking add when internal map is null should throw NullPointerException");
    }
}
