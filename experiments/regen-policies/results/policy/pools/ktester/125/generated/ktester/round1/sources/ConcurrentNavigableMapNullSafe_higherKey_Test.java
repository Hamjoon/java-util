package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.Field;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_higherKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test to avoid cross‑test contamination.
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map explicitly; not strictly required because a new instance
        // is created for each test, but it demonstrates good cleanup practice.
        map.clear();
    }

    static Stream<Arguments> higherKeyNormalCases() {
        return Stream.of(// 1. null key → smallest non‑null key ("A")
        Arguments.of("null key returns the smallest non‑null key", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) m -> {
            m.put("A", 1);
            m.put("B", 2);
        }, null, "A"), // 2. null key on an empty map → null
        Arguments.of("null key on empty map returns null", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) ConcurrentNavigableMapNullSafe::clear, null, null), // 3. key "B" → "C"
        Arguments.of("key \"B\" returns \"C\"", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) m -> {
            m.put("A", 1);
            m.put("B", 2);
            m.put("C", 3);
        }, "B", "C"), // 4. key "Z" (greatest) → null
        Arguments.of("key \"Z\" is greatest and returns null", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) m -> {
            m.put("A", 1);
            m.put("Z", 26);
        }, "Z", null), // 5. missing key "D" between "C" and "E" → "E"
        Arguments.of("missing key \"D\" returns next higher \"E\"", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) m -> {
            m.put("C", 3);
            m.put("E", 5);
        }, "D", "E"), // 6. key "Y" greater than any key → null
        Arguments.of("key \"Y\" greater than any stored key returns null", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) m -> {
            m.put("A", 1);
            m.put("Z", 26);
        }, "Y", null), // 7. key "A" → "B"
        Arguments.of("key \"A\" returns \"B\"", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) m -> {
            m.put("A", 1);
            m.put("B", 2);
        }, "A", "B"), // 8. null key when map also contains an explicit null entry → "A"
        Arguments.of("null key skips explicit null entry and returns first real key", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) m -> {
            m.put(null, 0);
            m.put("A", 1);
        }, // masked null key
        null, "A"));
    }

    static Stream<Arguments> higherKeyEdgeCases() {
        return Stream.of(// Map contains only a masked null key → higherKey(null) == null
        Arguments.of("only a null key present, higherKey(null) returns null", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) m -> m.put(null, 1), null, null), // Map contains only a masked null key → higherKey("A") == null
        Arguments.of("only a null key present, higherKey(\"A\") returns null", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) m -> m.put(null, 1), "A", null), // Key "0" less than smallest real key → returns "A"
        Arguments.of("key \"0\" less than smallest key returns first key \"A\"", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) m -> {
            m.put("A", 1);
            m.put("B", 2);
            m.put("C", 3);
        }, "0", "A"), // Key "ZZZ" greater than any key → null
        Arguments.of("key \"ZZZ\" greater than any key returns null", (Consumer<ConcurrentNavigableMapNullSafe<String, Integer>>) m -> {
            m.put("A", 1);
            m.put("Z", 26);
        }, "ZZZ", null));
    }

    @Test
    void higherKey_normalKeys() {
        // Given
        map.put("a", 1);
        map.put("c", 3);
        map.put("e", 5);
        // When / Then
        assertEquals("c", map.higherKey("a"), "higherKey should return the next key in order");
        assertEquals("e", map.higherKey("c"));
        assertNull(map.higherKey("e"), "No higher key after the greatest element");
        assertEquals("a", map.higherKey(null), "null is considered the greatest key, so higherKey(null) is null");
    }

    @Test
    void higherKey_withNullKeyPresent() {
        // Given a map that contains a null key (treated as the greatest key)
        map.put(null, 100);
        map.put("x", 10);
        map.put("y", 20);
        // When
        String higherAfterX = map.higherKey("x");
        String higherAfterY = map.higherKey("y");
        String higherAfterNull = map.higherKey(null);
        // Then
        assertEquals("y", higherAfterX, "higherKey should skip to the next non‑null key");
        assertEquals(null, higherAfterY, "null key is the greatest, so no higher key after it");
        assertNull(higherAfterNull, "higherKey(null) must be null because null is already the greatest");
    }

    @Test
    void higherKey_whenKeyNotPresent() {
        // Given a map with several entries but the queried key is absent
        map.put("b", 2);
        map.put("d", 4);
        map.put("f", 6);
        // When
        // "c" is not in the map, but lies between "b" and "d"
        String result = map.higherKey("c");
        // Then
        assertEquals("d", result, "higherKey should return the least key greater than the supplied key even if it is absent");
    }

    @Test
    void higherKey_allNullsAndEmptyMap() {
        // Empty map case
        assertNull(map.higherKey("any"), "higherKey on an empty map must return null");
        // Map only contains null key
        map.put(null, 1);
        assertNull(map.higherKey(null), "higherKey(null) should be null when null is the only key");
        assertNull(map.higherKey("anything"), "Any non‑null key has null as its higher key, which is considered last, so result is null");
    }

    @Test
    void higherKey_consistencyWithNavigableSet() {
        // Given a map populated with a mixture of keys
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("gamma", 3);
        map.put(null, 99);
        // The map's keySet is a NavigableSet; we can compare higherKey results.
        for (String key : new String[] { null, "alpha", "beta", "gamma" }) {
            String expected = ((java.util.NavigableSet<String>) map.navigableKeySet()).higher(key);
            assertEquals(expected, map.higherKey(key), () -> "higherKey should match NavigableSet.higher for key: " + key);
        }
    }

    /**
     * Normal behaviour test cases (group: higherKey).
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("higherKeyNormalCases")
    void higherKey_parameterised(String description, Consumer<ConcurrentNavigableMapNullSafe<String, Integer>> setup, String key, String expected) {
        setup.accept(map);
        assertEquals(expected, map.higherKey(key), description);
    }

    /**
     * Edge‑case test cases (group: higherKey_edgeCases).
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("higherKeyEdgeCases")
    void higherKey_edgeCases(String description, Consumer<ConcurrentNavigableMapNullSafe<String, Integer>> setup, String key, String expected) {
        setup.accept(map);
        assertEquals(expected, map.higherKey(key), description);
    }

    @Test
    void higherKey_incompatibleKeyClassCast() {
        // Raw map to bypass compile‑time generic checks
        @SuppressWarnings("rawtypes")
        ConcurrentNavigableMapNullSafe rawMap = new ConcurrentNavigableMapNullSafe();
        rawMap.put("one", 1);
        rawMap.put("two", 2);
        // Passing an Integer where a String is expected should provoke ClassCastException
        assertThrows(ClassCastException.class, () -> rawMap.higherKey(123), "Calling higherKey with an incompatible key type must throw ClassCastException");
    }

    @Test
    void higherKey_nonComparableKeyClassCast() {
        @SuppressWarnings("rawtypes")
        ConcurrentNavigableMapNullSafe rawMap = new ConcurrentNavigableMapNullSafe();
        rawMap.put("alpha", 1);
        rawMap.put("beta", 2);
        // Object does not implement Comparable – underlying skip list will reject it
        Object nonComparable = new Object();
        assertThrows(ClassCastException.class, () -> rawMap.higherKey(nonComparable), "Calling higherKey with a non‑Comparable key must throw ClassCastException");
    }

    @Test
    void higherKey_internalMapNullPointer() throws Exception {
        // Use reflection to null‑out the internal map field
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        // The cast inside higherKey will now throw NPE
        assertThrows(NullPointerException.class, () -> map.higherKey("any"), "If internalMap is null, higherKey must throw NullPointerException");
    }

    @Test
    void higherKey_customComparatorIllegalArgument() throws Exception {
        // Comparator that rejects empty strings
        Comparator<String> rejectingComparator = (s1, s2) -> {
            if (s1 == null || s2 == null)
                return 0;
            if (s1.isEmpty() || s2.isEmpty())
                throw new IllegalArgumentException("empty strings are not allowed");
            return s1.compareTo(s2);
        };
        // Replace the internal ConcurrentSkipListMap with one that uses the bad comparator
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        ConcurrentSkipListMap<Object, Object> badMap = new ConcurrentSkipListMap<>(rejectingComparator);
        internalMapField.set(map, badMap);
        // Populate with a couple of valid entries
        map.put("A", 1);
        map.put("B", 2);
        // Request higherKey for an empty string – the comparator should throw
        assertThrows(IllegalArgumentException.class, () -> map.higherKey(""), "A custom comparator that rejects empty strings must cause higherKey(\"\") to throw IllegalArgumentException");
    }
}
