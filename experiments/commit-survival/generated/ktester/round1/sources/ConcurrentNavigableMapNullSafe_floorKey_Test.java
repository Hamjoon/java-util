package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Comparator;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@DisplayName("ConcurrentNavigableMapNullSafe.floorKey(K) Tests")
class ConcurrentNavigableMapNullSafe_floorKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test to guarantee isolation
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map – not strictly necessary because a new instance is created per test,
        // but it documents the intent of cleanup.
        map.clear();
    }

    private static Stream<Arguments> exactMatchProvider() {
        return Stream.of(Arguments.of(5, 5));
    }

    @Test
    @DisplayName("floorKey returns the exact key when it exists")
    void floorKeyExactMatch() {
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("gamma", 3);
        assertEquals("alpha", map.floorKey("alpha"));
        assertEquals("beta", map.floorKey("beta"));
        assertEquals("gamma", map.floorKey("gamma"));
    }

    @Test
    @DisplayName("floorKey returns the greatest key less than the supplied key")
    void floorKeyGreatestLessThan() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // "b" is the greatest key <= "bb"
        assertEquals("b", map.floorKey("bb"));
        // "a" is the greatest key <= "aa"
        assertEquals("a", map.floorKey("aa"));
    }

    @Test
    @DisplayName("floorKey returns null when no key is less than or equal to the supplied key")
    void floorKeyNoMatch() {
        map.put("m", 1);
        map.put("n", 2);
        map.put("o", 3);
        // No key <= "a"
        assertNull(map.floorKey("a"));
    }

    @Test
    @DisplayName("floorKey with null key when null key is present")
    void floorKeyWithNullKeyPresent() {
        map.put("x", 10);
        map.put("y", 20);
        // null key is stored using the internal sentinel
        map.put(null, 99);
        // Because null is considered the greatest key, floorKey(null) should return null itself
        assertNull(map.floorKey(null));
    }

    @Test
    @DisplayName("floorKey with null key when null key is absent")
    void floorKeyWithNullKeyAbsent() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // No null key inserted
        // Null is treated as greater than any real key, so the floor should be the highest real key
        assertEquals("c", map.floorKey(null));
    }

    @Test
    @DisplayName("floorKey works with a custom comparator")
    void floorKeyWithCustomComparator() {
        // Comparator that orders strings in reverse (descending) order
        Comparator<String> reverse = Comparator.reverseOrder();
        map = new ConcurrentNavigableMapNullSafe<>(reverse);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // In reverse order the sequence is c < b < a.
        // floorKey("b") should be "b" (the greatest key <= "b")
        assertEquals("b", map.floorKey("b"));
        // floorKey("bb") – keys <= "bb" are "c" and "b"; greatest is "b"
        assertEquals("b", map.floorKey("bb"));
        // floorKey(null) when null key is absent – null is still considered after all real keys
        assertEquals("a", map.floorKey(null));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Null key handling – empty map returns null")
    void nullKeyEmptyMap() {
        // map is freshly created and empty
        assertNull(map.floorKey(null), "When the map is empty, floorKey(null) must return null");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] floorKey({0}) → {1}")
    @MethodSource("exactMatchProvider")
    @DisplayName("Exact match key – floorKey returns the key itself")
    void floorKeyExactMatchInteger(Integer key, Integer expected) {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(5, 5);
        intMap.put(10, 10);
        assertEquals(expected, intMap.floorKey(key));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Key not present but lower key exists – floorKey returns greatest lower key")
    void floorKeyLowerKeyExists() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(2, 2);
        intMap.put(4, 4);
        intMap.put(6, 6);
        assertEquals(6, intMap.floorKey(7));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Key lower than any entry – floorKey returns null")
    void floorKeyBelowAll() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(2, 2);
        intMap.put(4, 4);
        intMap.put(6, 6);
        assertNull(intMap.floorKey(1));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Empty map – floorKey returns null for any key")
    void floorKeyEmptyMapAnyKey() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        assertNull(intMap.floorKey(10));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Incompatible key type – floorKey throws ClassCastException")
    void floorKeyIncompatibleKeyType() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, 1);
        intMap.put(2, 2);
        assertThrows(ClassCastException.class, () -> intMap.floorKey((Integer) (Object) "string-key"));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Key greater than maximum key – floorKey returns the maximum key")
    void floorKeyGreaterThanMax() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(2, 2);
        intMap.put(4, 4);
        intMap.put(6, 6);
        assertEquals(6, intMap.floorKey(10));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Key equal to the lowest key – floorKey returns the key itself")
    void floorKeyEqualToLowest() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(2, 2);
        intMap.put(4, 4);
        intMap.put(6, 6);
        assertEquals(2, intMap.floorKey(2));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Map containing only a null key – floorKey(null) returns null")
    void floorKeyOnlyNullKey_nullKey() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(null, 99);
        assertNull(intMap.floorKey(null));
    }

    @Test
    @DisplayName("Map containing only a null key – floorKey(non‑null) returns null key")
    void floorKeyOnlyNullKey_nonNullKey() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(null, 99);
        assertNull(intMap.floorKey(5));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Null key together with non‑null keys – exact match takes precedence")
    void floorKeyNullAndNonNull_exactMatch() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(null, 0);
        intMap.put(5, 5);
        assertEquals(5, intMap.floorKey(5));
    }

    @Test
    @DisplayName("Null key together with non‑null keys – floorKey below existing non‑null keys returns null key")
    void floorKeyNullAndNonNull_noLowerNonNull() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(null, 0);
        intMap.put(5, 5);
        // 4 is less than the only non‑null key (5); the null key is considered greater,
        // so the greatest key ≤ 4 is the stored null key (which is returned as null)
        assertNull(intMap.floorKey(4));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Custom comparator (reverse order) – floorKey follows reverse ordering")
    void floorKeyCustomComparatorReverse() {
        Comparator<Integer> reverse = Comparator.reverseOrder();
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>(reverse);
        intMap.put(1, 1);
        intMap.put(3, 3);
        intMap.put(5, 5);
        // In reverse order the logical floor of 2 is 3
        assertEquals(3, intMap.floorKey(2));
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Floor key after removal of higher entries – map updates correctly")
    void floorKeyAfterRemoval() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(2, 2);
        intMap.put(4, 4);
        intMap.put(6, 6);
        // Remove the highest key
        intMap.remove(6);
        // Now the greatest key ≤ 7 should be 4
        assertEquals(4, intMap.floorKey(7));
    }
}
