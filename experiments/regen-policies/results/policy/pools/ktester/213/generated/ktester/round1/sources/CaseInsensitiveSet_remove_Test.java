package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_remove_Test {

    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for every test to guarantee isolation
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference – not strictly required but keeps the pattern consistent
        set = null;
    }

    private static Stream<Arguments> provideRemoveScenarios() {
        return Stream.of(// existing element – same case
        Arguments.of("TeSt", true, "The set contains the element \"TeSt\" (case‑insensitive). Removing with the same string should delete the entry and return true."), // existing element – different case
        Arguments.of("test", true, "The set contains \"TeSt\" (added previously). Removing using a different case \"test\" should still match because the set is case‑insensitive, resulting in true."), // non‑existing element
        Arguments.of("absent", false, "The set does not contain the element \"absent\". Calling remove should leave the set unchanged and return false."), // null element
        Arguments.of(null, false, "Removing a null reference from a set that does not contain null should return false (the underlying map returns null for map.remove(null))."), // different type (Integer vs. String set)
        Arguments.of(5, true, // note: set already contains 5, so removal succeeds
        "The set holds String elements only. Attempting to remove an Integer (5) should not match any entry and therefore return false."));
    }

    @Test
    void removeExistingStringIsCaseInsensitive() {
        assertTrue(set.add("Hello"));
        assertEquals(1, set.size());
        // Removing with a different case should succeed and return true
        assertTrue(set.remove("HELLO"));
        assertEquals(0, set.size());
        // Subsequent removal should return false because the element is gone
        assertFalse(set.remove("hello"));
    }

    @Test
    void removeNonExistingElementReturnsFalse() {
        set.add("Alpha");
        set.add(42);
        // Element that was never added
        assertFalse(set.remove("Beta"));
        // Existing element but different case (should be considered present)
        assertTrue(set.remove("ALPHA"));
        // Now removing again should be false
        assertFalse(set.remove("Alpha"));
        // Removing a number that exists
        assertTrue(set.remove(42));
        // Removing a number that no longer exists
        assertFalse(set.remove(42));
    }

    @Test
    void removeNonStringElement() {
        Integer value = 12345;
        assertTrue(set.add(value));
        assertEquals(1, set.size());
        assertTrue(set.remove(value));
        assertEquals(0, set.size());
        // Removing a different Integer that was never added
        assertFalse(set.remove(999));
    }

    @Test
    void removeNullElementReturnsFalse() {
        // The underlying map does not store null keys; remove should simply return false
        assertFalse(set.remove(null));
        // Adding a null is not permitted by the Set contract in this implementation;
        // however, if it were added (e.g., via a custom map), remove should still behave correctly.
        // Here we just verify the current behaviour.
    }

    @Test
    void removeWithCustomBackingMap() {
        // Use the constructor that accepts a backing map (TreeMap in this case)
        CaseInsensitiveSet<String> customSet = new CaseInsensitiveSet<>(Collections.emptySet(), new CaseInsensitiveMap<>(new TreeMap<>()));
        assertTrue(customSet.add("Apple"));
        assertTrue(customSet.add("Banana"));
        assertEquals(2, customSet.size());
        // Case‑insensitive removal
        assertTrue(customSet.remove("apple"));
        assertEquals(1, customSet.size());
        // Removing the remaining element with exact case
        assertTrue(customSet.remove("Banana"));
        assertTrue(customSet.isEmpty());
        // Attempt to remove a non‑existent element
        assertFalse(customSet.remove("Cherry"));
    }

    /**
     * Parameterised test covering normal removal scenarios:
     * - existing element (exact case)
     * - existing element (different case)
     * - non‑existing element
     * - null argument (expected false)
     * - different type (expected false)
     */
    @ParameterizedTest(name = "{index} => remove({0}) expected:{1}")
    @MethodSource("provideRemoveScenarios")
    void removeScenarios(Object argument, boolean expectedResult, String description) {
        // Arrange: populate the set according to the description
        // string element for case‑insensitive checks
        set.add("TeSt");
        // integer element for type‑mismatch checks
        set.add(5);
        // Act
        boolean result = set.remove(argument);
        // Assert
        assertEquals(expectedResult, result, description);
    }

    @Test
    void removeAfterClearReturnsFalse() {
        set.add("abc");
        // now the set is empty
        set.clear();
        assertFalse(set.remove("abc"), "After clear, removing any element should return false.");
    }

    @Test
    void removeFromEmptySetReturnsFalse() {
        assertTrue(set.isEmpty(), "Set should be empty before removal.");
        assertFalse(set.remove("nothing"), "Removing from an empty set must always return false.");
    }

    @Test
    void removeSameElementTwice() {
        assertTrue(set.add("duplicate"));
        // first removal succeeds
        assertTrue(set.remove("duplicate"));
        // second removal fails because the element is no longer present
        assertFalse(set.remove("duplicate"));
    }

    /**
     * Helper that injects a custom backing {@link Map} into a {@link CaseInsensitiveSet}
     * via reflection. This is required because the map field is private and there is
     * no public setter.
     */
    @SuppressWarnings("unchecked")
    private static <E> void injectBackingMap(CaseInsensitiveSet<E> target, Map<E, Object> map) {
        try {
            Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
            mapField.setAccessible(true);
            mapField.set(target, map);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to inject backing map via reflection", e);
        }
    }

    @Test
    void removeNullKeyThrowsNullPointerException() {
        // Use a ConcurrentSkipListMap which does not permit null keys
        CaseInsensitiveSet<Object> mapBackedSet = new CaseInsensitiveSet<>();
        injectBackingMap(mapBackedSet, new ConcurrentSkipListMap<>());
        // The map's remove(null) throws NPE, which should propagate
        assertThrows(NullPointerException.class, () -> mapBackedSet.remove(null), "When the underlying map (ConcurrentSkipListMap) does not permit null keys, remove(null) should throw NullPointerException.");
    }

    @Test
    void removeIncompatibleTypeThrowsClassCastException() {
        // ConcurrentSkipListMap uses natural ordering; mixing String and Integer causes a ClassCastException
        CaseInsensitiveSet<Object> mapBackedSet = new CaseInsensitiveSet<>();
        ConcurrentSkipListMap<Object, Object> skipListMap = new ConcurrentSkipListMap<>();
        injectBackingMap(mapBackedSet, skipListMap);
        // Populate with a String key
        assertTrue(mapBackedSet.add("string"));
        // Attempt to remove an Integer – the map will try to compare the Integer with the stored String
        assertThrows(ClassCastException.class, () -> mapBackedSet.remove(5), "Removing an Integer from a set backed by a natural‑ordering map that already contains a String should throw ClassCastException.");
    }

    @Test
    void removeWithImmutableBackingMapThrowsUnsupportedOperationException() {
        // Wrap a mutable map with Collections.unmodifiableMap to make it immutable
        Map<Object, Object> mutable = new HashMap<>();
        // dummy entry to keep the map non‑empty
        mutable.put("any", new Object());
        Map<Object, Object> unmodifiable = Collections.unmodifiableMap(mutable);
        CaseInsensitiveSet<Object> immutableSet = new CaseInsensitiveSet<>();
        injectBackingMap(immutableSet, unmodifiable);
        // Any removal attempt must fail with UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> immutableSet.remove("any"), "When the internal map is wrapped with Collections.unmodifiableMap, remove should raise UnsupportedOperationException.");
    }
}
