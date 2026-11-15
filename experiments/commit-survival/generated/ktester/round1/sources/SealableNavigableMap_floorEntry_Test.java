package com.cedarsoftware.util;

import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_floorEntry_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    /**
     * Custom comparable key used in a few test cases.
     */
    private static final class CustomKey implements Comparable<CustomKey> {

        // may be null to trigger NPE in comparator
        final Integer value;

        CustomKey(Integer value) {
            this.value = value;
        }

        @Override
        public int compareTo(CustomKey o) {
            return Objects.compare(this.value, o.value, Integer::compareTo);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomKey))
                return false;
            return Objects.equals(value, ((CustomKey) o).value);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        @Override
        public String toString() {
            return "CustomKey(" + value + ")";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        // Populate a map that includes a null key/value and a few normal entries.
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put(null, null);
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    /**
     * Helper to build a simple integer → String map used by many test cases.
     */
    private static NavigableMap<Integer, String> buildIntegerStringMap() {
        NavigableMap<Integer, String> map = new ConcurrentSkipListMap<>();
        map.put(1, "one");
        map.put(5, "five");
        map.put(10, "ten");
        return map;
    }

    /**
     * Helper to build a simple string → String map used by string‑key cases.
     */
    private static NavigableMap<String, String> buildStringStringMap() {
        NavigableMap<String, String> map = new ConcurrentSkipListMap<>();
        map.put("a", "alpha");
        map.put("y", "yankee");
        map.put("z", "zulu");
        return map;
    }

    private static Stream<Arguments> provideNormalFloorEntryCases() {
        return Stream.of(// Null key – expect NPE (handled separately)
        // Exact match
        Arguments.of(5, entry(5, "five")), // Greater than existing keys, not present
        Arguments.of(7, entry(5, "five")), // Smaller than smallest key
        Arguments.of(0, null), // String natural ordering
        Arguments.of("b", entry("a", "alpha")), Arguments.of("z", entry("y", "yankee")), Arguments.of("a", entry("a", "alpha")), // case‑sensitive comparator
        Arguments.of("A", null), Arguments.of(Integer.MAX_VALUE, entry(5, "five")), Arguments.of(-1, null), Arguments.of("", null), Arguments.of("aa", entry("a", "alpha")), // Custom comparable key
        Arguments.of(new CustomKey(3), entry(new CustomKey(2), "two")), // Custom key with null internal value – NPE handled separately
        // placeholder; will be tested in exception group
        Arguments.of(new CustomKey(null), null));
    }

    private static Stream<Arguments> provideExceptionFloorEntryCases() {
        return Stream.of(// Null key on a map that does not support nulls
        Arguments.of(null, NullPointerException.class), // ClassCastException due to wrong key type
        Arguments.of("string", ClassCastException.class), Arguments.of(new Object(), ClassCastException.class), // IllegalArgumentException from custom comparator
        Arguments.of(5, IllegalArgumentException.class), // CustomKey with null internal value – comparator will NPE
        Arguments.of(new CustomKey(null), NullPointerException.class));
    }

    /**
     * Simple factory for {@link Map.Entry} used in expected values.
     */
    private static <K, V> Map.Entry<K, V> entry(K key, V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    @Test
    void floorEntryReturnsExactMatchWhenKeyExists() {
        Map.Entry<String, Integer> entry = sealableMap.floorEntry("two");
        assertNotNull(entry, "floorEntry should not be null for an existing key");
        assertEquals("two", entry.getKey());
        assertEquals(Integer.valueOf(2), entry.getValue());
        // Mutate the entry while the map is unsealed – this should succeed.
        entry.setValue(20);
        assertEquals(Integer.valueOf(20), sealableMap.get("two"));
    }

    @Test
    void floorEntryReturnsClosestLowerKeyWhenKeyMissing() {
        // "two-and-half" is not present; the greatest key ≤ it is "two".
        Map.Entry<String, Integer> entry = sealableMap.floorEntry("two-and-half");
        assertNotNull(entry);
        assertEquals("two", entry.getKey());
        assertEquals(Integer.valueOf(2), entry.getValue());
    }

    @Test
    void floorEntryHandlesNullKey() {
        // The backing map contains a null key with a null value.
        Map.Entry<String, Integer> entry = sealableMap.floorEntry(null);
        assertNotNull(entry, "floorEntry should return the null entry when key is null");
        assertNull(entry.getKey());
        assertNull(entry.getValue());
        // Changing the null entry while unsealed should be permitted.
        entry.setValue(99);
        assertEquals(Integer.valueOf(99), sealableMap.get(null));
    }

    @Test
    void floorEntryEntryIsImmutableWhenSealed() {
        Map.Entry<String, Integer> entry = sealableMap.floorEntry("three");
        assertNotNull(entry);
        // switch map to sealed state
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(999), "Modifying an entry from floorEntry should throw when map is sealed");
    }

    @ParameterizedTest(name = "{index}: floorEntry({0}) → {1}")
    @MethodSource("provideNormalFloorEntryCases")
    void floorEntryNormalBehaviour(Object key, Map.Entry<?, ?> expected) {
        // Choose backing map based on key type
        NavigableMap<?, ?> backing;
        if (key instanceof Integer) {
            backing = buildIntegerStringMap();
        } else if (key instanceof String) {
            backing = buildStringStringMap();
        } else if (key instanceof CustomKey) {
            NavigableMap<CustomKey, String> map = new ConcurrentSkipListMap<>();
            map.put(new CustomKey(2), "two");
            map.put(new CustomKey(4), "four");
            backing = map;
        } else {
            fail("Unsupported key type in test data");
            return;
        }
        SealableNavigableMap<?, ?> map = new SealableNavigableMap<>(backing, () -> false);
        @SuppressWarnings("unchecked")
        Map.Entry<Object, Object> result = (Map.Entry<Object, Object>) map.floorEntry(key);
        if (expected == null) {
            assertNull(result, "Expected null floorEntry for key: " + key);
        } else {
            assertNotNull(result, "Expected non‑null floorEntry for key: " + key);
            assertEquals(expected.getKey(), result.getKey(), "Key mismatch for floorEntry(" + key + ")");
            assertEquals(expected.getValue(), result.getValue(), "Value mismatch for floorEntry(" + key + ")");
        }
    }

    @ParameterizedTest(name = "{index}: floorEntry({0}) throws {1}")
    @MethodSource("provideExceptionFloorEntryCases")
    void floorEntryExceptionBehaviour(Object key, Class<? extends Throwable> expectedException) {
        // Build a map that will cause the expected exception
        NavigableMap<?, ?> backing;
        if (expectedException == ClassCastException.class) {
            // Map with Integer keys, key is of wrong type
            backing = buildIntegerStringMap();
        } else if (expectedException == IllegalArgumentException.class) {
            // Map with custom comparator that rejects odd numbers
            Comparator<Integer> rejectOdd = (a, b) -> {
                if (a % 2 != 0 || b % 2 != 0) {
                    throw new IllegalArgumentException("odd numbers not allowed");
                }
                return Integer.compare(a, b);
            };
            NavigableMap<Integer, String> map = new ConcurrentSkipListMap<>(rejectOdd);
            map.put(2, "two");
            map.put(4, "four");
            backing = map;
        } else {
            // For NullPointerException we need a map that does not allow null keys
            backing = new ConcurrentSkipListMap<>();
        }
        SealableNavigableMap<?, ?> map = new SealableNavigableMap<>(backing, () -> false);
        assertThrows(expectedException, () -> map.floorEntry(key), "Expected " + expectedException.getSimpleName() + " for key: " + key);
    }
}
