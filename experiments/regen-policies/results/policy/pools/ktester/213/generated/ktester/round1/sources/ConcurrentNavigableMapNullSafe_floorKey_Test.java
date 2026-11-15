package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

class ConcurrentNavigableMapNullSafe_floorKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        map = null;
    }

    static Stream<Arguments> basicScenariosProvider() {
        return Stream.of(// Exact match
        Arguments.of("c", "c", "Exact match: floorKey(\"c\") → \"c\""), // Non‑existent key but lower key exists
        Arguments.of("c5", "c", "Non‑existent key: floorKey(\"c5\") → \"c\""), // Key smaller than any entry
        Arguments.of("0", null, "Key smaller than any entry: floorKey(\"0\") → null"), // Key greater than the greatest entry
        Arguments.of("z", "d", "Key greater than greatest entry: floorKey(\"z\") → \"d\""), // Null key handling (no null key stored)
        Arguments.of(null, null, "Null key handling without stored null: floorKey(null) → null"), // Empty map scenario (handled separately)
        // map will be empty after clearing
        Arguments.of("any", null, "Empty map: floorKey(\"any\") → null"));
    }

    /**
     * Basic verification of floorKey with normal (non‑null) keys.
     */
    @Test
    void testFloorKeyBasic() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Exact match
        assertEquals("b", map.floorKey("b"));
        // Key not present – should return greatest key less than the argument
        assertEquals("b", map.floorKey("bb"));
        assertEquals("a", map.floorKey("a"));
        assertEquals("a", map.floorKey("aa"));
        // No key less than the argument
        assertNull(map.floorKey("0"));
    }

    /**
     * Verify that null keys are treated as the greatest possible key.
     * According to the Javadoc, null keys are considered “last” in the
     * natural ordering, therefore floorKey(null) should return the highest
     * non‑null key if present, otherwise the null key itself.
     */
    @Test
    void testFloorKeyWithNullKey() {
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);
        // Null is the greatest key, so floorKey(null) returns the null key itself
        // the key returned is null
        assertNull(map.floorKey(null));
        // Removing the null key makes the highest non‑null key the floor of null
        map.remove(null);
        assertEquals("b", map.floorKey(null));
        // Normal keys still behave as expected
        assertEquals("a", map.floorKey("a"));
        assertEquals("b", map.floorKey("c"));
    }

    /**
     * Verify floorKey works when the map is constructed with a custom comparator.
     * The custom comparator orders strings by length first, then natural order.
     * Null is still considered greater than any non‑null key.
     */
    @Test
    void testFloorKeyWithCustomComparator() {
        Comparator<String> lengthThenNatural = (s1, s2) -> {
            if (s1 == null && s2 == null)
                return 0;
            // null is considered greater
            if (s1 == null)
                return 1;
            if (s2 == null)
                return -1;
            int lenDiff = Integer.compare(s1.length(), s2.length());
            return (lenDiff != 0) ? lenDiff : s1.compareTo(s2);
        };
        map = new ConcurrentNavigableMapNullSafe<>(lengthThenNatural);
        // length 2
        map.put("aa", 1);
        // length 4
        map.put("bbbb", 2);
        // length 1
        map.put("c", 3);
        // null key
        map.put(null, 99);
        // floorKey for a key of length 3 should be the longest key <= length 3, i.e., "aa"
        // "xyz" length 3
        assertEquals("aa", map.floorKey("xyz"));
        // Exact match
        assertEquals("bbbb", map.floorKey("bbbb"));
        // Key shorter than any existing non‑null key
        // empty string length 0, no floor
        assertNull(map.floorKey(""));
        // Null key – should return null itself because it is present
        assertNull(map.floorKey(null));
        // After removing null, floorKey(null) should return the longest key ("bbbb")
        map.remove(null);
        assertEquals("bbbb", map.floorKey(null));
    }

    /**
     * Verify floorKey on an empty map always returns {@code null}.
     */
    @Test
    void testFloorKeyEmptyMap() {
        assertNull(map.floorKey("any"));
        assertNull(map.floorKey(null));
    }

    /**
     * Parameterized basic scenarios extracted from the JSON test description.
     *
     * The map is populated with keys ["a", "b", "c", "d"] before each invocation.
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("basicScenariosProvider")
    void testFloorKeyBasicScenarios(String key, String expected) {
        // Populate the map with the required keys
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        // Act
        String actual = map.floorKey(key);
        // Assert
        assertEquals(expected, actual);
    }

    /**
     * Special case for the empty‑map scenario required by the JSON spec.
     */
    @Test
    @DisplayName("Empty map scenario – floorKey should return null for any key")
    void testFloorKeyEmptyMapJsonScenario() {
        // No entries added – map is empty
        assertNull(map.floorKey("any"));
        assertNull(map.floorKey(null));
    }

    /**
     * Verify that a {@link ClassCastException} is thrown when the key type does not match
     * the map's key type (String vs Integer).
     */
    @Test
    @DisplayName("Exception scenario – wrong key type triggers ClassCastException")
    void testFloorKeyWrongKeyType() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // The map expects String keys; passing an Integer forces the underlying
        // ConcurrentSkipListMap to attempt an illegal comparison.
        assertThrows(ClassCastException.class, () -> map.floorKey((String) (Object) 123));
    }

    /**
     * Verify that a {@link ClassCastException} is thrown when the internal map is
     * replaced with a plain {@link HashMap} (i.e., not a {@link ConcurrentSkipListMap}).
     */
    @Test
    @DisplayName("Exception scenario – internal map replaced with HashMap triggers ClassCastException")
    void testFloorKeyInternalMapWrongType() throws Exception {
        // Populate with a normal entry so the map is not empty
        map.put("a", 1);
        // Replace the protected internalMap field via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        // not a ConcurrentSkipListMap
        internalMapField.set(map, new HashMap<>());
        // The cast inside floorKey should now fail
        assertThrows(ClassCastException.class, () -> map.floorKey("any"));
    }

    /**
     * Verify that a {@link NullPointerException} is thrown when the internal map reference
     * itself is set to {@code null}.
     */
    @Test
    @DisplayName("Exception scenario – internalMap set to null triggers NullPointerException")
    void testFloorKeyInternalMapNull() throws Exception {
        // Populate to ensure map had a proper internal map before we null it
        map.put("a", 1);
        // Null out the internalMap field via reflection
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        // floorKey will attempt to invoke a method on a null reference
        assertThrows(NullPointerException.class, () -> map.floorKey("any"));
    }
}
