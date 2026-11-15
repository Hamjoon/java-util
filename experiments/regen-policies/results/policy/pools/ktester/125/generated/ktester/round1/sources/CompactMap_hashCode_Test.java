package com.cedarsoftware.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.lang.reflect.Field;
// import org.junit.*;
// import static org.junit.Assert.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompactMap_hashCode_Test {

    /**
     * A thin concrete subclass that exposes the protected hooks used by
     * {@code CompactMap} to decide which internal storage to employ.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        private final int compactSize;

        private final Map<K, V> backingMap;

        private final K singleValueKey;

        TestCompactMap(int compactSize, Map<K, V> backingMap, K singleValueKey) {
            this.compactSize = compactSize;
            this.backingMap = backingMap;
            this.singleValueKey = singleValueKey;
        }

        @Override
        protected int compactSize() {
            return compactSize;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return backingMap;
        }

        @Override
        protected K getSingleValueKey() {
            return singleValueKey;
        }
    }

    /**
     * Utility that forces the private {@code val} field of {@link CompactMap}
     * to a given value using reflection.  This is required for the test that
     * directly injects a {@link Map} instance to hit the {@code instanceof Map}
     * branch.
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, newVal);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to set internal val field", e);
        }
    }

    @Test
    void hashCode_emptyMap_returnsZero() {
        // The map is newly created → internal val == EMPTY_MAP
        CompactMap<String, Integer> map = new TestCompactMap<>(3, new HashMap<>(), "value");
        assertEquals(0, map.hashCode(), "Empty CompactMap should have hashCode 0 (sentinel EMPTY_MAP case).");
    }

    @Test
    void hashCode_singleEntry_matchesComputedValue() {
        CompactMap<String, Integer> map = new TestCompactMap<>(3, new HashMap<>(), "value");
        map.put("one", 1);
        int expected = Objects.hashCode("one") ^ Objects.hashCode(1);
        assertEquals(expected, map.hashCode(), "hashCode for a single‑entry CompactMap must be keyHash ^ valueHash");
    }

    @Test
    void hashCode_compactArray_twoEntries_matchesSumOfXors() {
        // compactSize > 2 ensures we stay in the Object[] representation
        CompactMap<String, Integer> map = new TestCompactMap<>(5, new HashMap<>(), "value");
        map.put("a", 1);
        map.put("b", 2);
        int expected = (Objects.hashCode("a") ^ Objects.hashCode(1)) + (Objects.hashCode("b") ^ Objects.hashCode(2));
        assertEquals(expected, map.hashCode(), "Compact array state should sum (keyHash ^ valueHash) for each pair.");
    }

    @Test
    void hashCode_backingMap_exceedsCompactSize_delegatesToMapHashCode() {
        // compactSize = 2 → after three puts the internal storage becomes a Map
        CompactMap<String, Integer> map = new TestCompactMap<>(2, new HashMap<>(), "value");
        map.put("x", 100);
        map.put("y", 200);
        // triggers transition to backing map
        map.put("z", 300);
        Map<String, Integer> expectedMap = new HashMap<>();
        expectedMap.put("x", 100);
        expectedMap.put("y", 200);
        expectedMap.put("z", 300);
        assertEquals(expectedMap.hashCode(), map.hashCode(), "When CompactMap uses a backing Map, hashCode should equal that Map's hashCode");
    }

    @Test
    void hashCode_singleEntry_matchesComputedXor() {
        CompactMap<String, Integer> map = new TestCompactMap<>(3, new HashMap<>(), "value");
        map.put("c", 3);
        int expected = Objects.hashCode("c") ^ Objects.hashCode(3);
        assertEquals(expected, map.hashCode(), "Single‑entry CompactMap must return keyHash ^ valueHash.");
    }

    @Test
    void hashCode_backingMap_directMapInstance_delegatesToMapHashCode() {
        // Create a standard map with the desired entries
        Map<String, Integer> backing = new HashMap<>();
        backing.put("a", 1);
        backing.put("b", 2);
        // Build a CompactMap and inject the map directly into its private 'val' field
        CompactMap<String, Integer> map = new TestCompactMap<>(3, new HashMap<>(), "value");
        setInternalVal(map, backing);
        assertEquals(backing.hashCode(), map.hashCode(), "When internal val is a Map, hashCode should delegate to that Map's hashCode.");
    }
}
