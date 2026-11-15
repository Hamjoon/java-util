package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_hashCode_Test {

    private TestMap<String, String> map;

    /**
     * Concrete implementation of the abstract class for testing purposes.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    private static class ConstantHash {

        private final int value;

        ConstantHash(int v) {
            this.value = v;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    private static class ConstantHash2 {

        private final int value;

        ConstantHash2(int v) {
            this.value = v;
        }

        @Override
        public int hashCode() {
            return value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    private static Stream<Arguments> basicHashCodeProvider() {
        return Stream.of(// empty map
        Arguments.of(Collections.emptyMap(), 0, "Empty map should return hash code 0 because the loop does not execute."), // single non‑null entry ("a","b")
        Arguments.of(Map.of("a", "b"), ("a".hashCode() ^ "b".hashCode()), "Map with a single entry where both key and value are non‑null."), // null key, non‑null value
        Arguments.of(Collections.singletonMap(null, "b"), (0 ^ "b".hashCode()), "Map with a single entry where key is null and value is non‑null."), // non‑null key, null value
        Arguments.of(Collections.singletonMap("a", null), ("a".hashCode() ^ 0), "Map with a single entry where key is non‑null and value is null."), // both null
        Arguments.of(Collections.singletonMap(null, null), (0 ^ 0), "Map with a single entry where both key and value are null."), // mixed multiple entries
        Arguments.of(mapOf(entry("a", "b"), entry(null, "c"), entry("d", null), entry(null, null)), (("a".hashCode() ^ "b".hashCode()) + (0 ^ "c".hashCode()) + ("d".hashCode() ^ 0) + (0 ^ 0)), "Multiple entries mixing null and non‑null keys/values."));
    }

    private static <K, V> Map.Entry<K, V> entry(K k, V v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    @Test
    @DisplayName("hashCode of an empty map should be 0")
    void hashCodeEmptyMap() {
        assertEquals(0, map.hashCode(), "Empty map must have hashCode 0");
    }

    @Test
    @DisplayName("hashCode with only non‑null entries")
    void hashCodeNonNullEntries() {
        map.put("a", "1");
        map.put("b", "2");
        // Expected calculation:
        // entry ("a","1"): keyHash ^ valueHash
        // entry ("b","2"): keyHash ^ valueHash
        int expected = ("a".hashCode() ^ "1".hashCode()) + ("b".hashCode() ^ "2".hashCode());
        assertEquals(expected, map.hashCode(), "hashCode should equal the sum of (keyHash ^ valueHash) for each entry");
    }

    @Test
    @DisplayName("hashCode with null key and null value")
    void hashCodeNullKeyAndValue() {
        map.put(null, null);
        // Both key and value hash to 0, contribution is 0 ^ 0 = 0
        assertEquals(0, map.hashCode(), "Map containing only a null key/value pair must have hashCode 0");
    }

    @Test
    @DisplayName("hashCode with mixed null and non‑null entries")
    void hashCodeMixedEntries() {
        // key non‑null, value null
        map.put("a", null);
        // key null, value non‑null
        map.put(null, "b");
        // value null => 0
        int contribution1 = "a".hashCode() ^ 0;
        // key null => 0
        int contribution2 = 0 ^ "b".hashCode();
        int expected = contribution1 + contribution2;
        assertEquals(expected, map.hashCode(), "hashCode must correctly handle null keys and values");
    }

    @Test
    @DisplayName("hashCode updates after modifications")
    void hashCodeUpdatesAfterChanges() {
        map.put("x", "y");
        int first = map.hashCode();
        // replace value for existing key
        map.put("x", "z");
        int second = map.hashCode();
        assertNotEquals(first, second, "hashCode should change when map contents change");
        map.remove("x");
        assertEquals(0, map.hashCode(), "After removing the only entry, hashCode should be 0");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicHashCodeProvider")
    void hashCode_basicScenarios(Map<String, String> data, int expected, String description) {
        // Populate the map with the supplied data (null keys/values are allowed)
        data.forEach(map::put);
        assertEquals(expected, map.hashCode(), description);
    }

    @Test
    @DisplayName("Repeated calls on a populated map must return the same value")
    void hashCode_idempotence() {
        map.put("x", "y");
        map.put(null, "z");
        int first = map.hashCode();
        int second = map.hashCode();
        assertEquals(first, second, "hashCode must be idempotent for unchanged map");
    }

    @Test
    @DisplayName("Multiple entries where each key hash equals its value hash result in total hash 0")
    void hashCode_multipleZeroXor() {
        // Integer hash codes are equal
        map.put(1, 1);
        // String hash codes are equal
        map.put("x", "x");
        assertEquals(0, map.hashCode(), "When each entry contributes 0 (keyHash ^ valueHash), total hash must be 0");
    }

    @Test
    @DisplayName("Negative key hash (Integer.MIN_VALUE) is handled correctly")
    void hashCode_negativeKeyHash() {
        // key hash = -2147483648, value hash = 1
        map.put(Integer.MIN_VALUE, 1);
        // equals -2147483647
        int expected = Integer.MIN_VALUE ^ 1;
        assertEquals(expected, map.hashCode(), "hashCode must correctly XOR negative hash codes");
    }

    @Test
    @DisplayName("Sum overflow wraps around according to 32‑bit signed int semantics")
    void hashCode_overflow() {
        // Two entries with large positive key hash (2000000000) and null value (hash 0)
        map.put(2_000_000_000, null);
        map.put(2_000_000_000, null);
        // Each contributes 2_000_000_000; sum = 4_000_000_000 -> overflow to -294967296
        // cast triggers overflow
        int expected = (int) (4_000_000_000L);
        assertEquals(expected, map.hashCode(), "hashCode must overflow correctly when the sum exceeds Integer.MAX_VALUE");
    }

    @Test
    @DisplayName("Custom objects with known hash codes")
    void hashCode_customObject() {
        map.put(new ConstantHash(42), new ConstantHash2(99));
        // 73
        int expected = 42 ^ 99;
        assertEquals(expected, map.hashCode(), "hashCode must use the objects' hashCode implementations");
    }

    @Test
    @DisplayName("Hash code is independent of insertion order")
    void hashCode_orderIndependence() {
        TestMap<Integer, Integer> map1 = new TestMap<>();
        TestMap<Integer, Integer> map2 = new TestMap<>();
        // Insert in opposite order
        map1.put(1, 2);
        map1.put(3, 4);
        map2.put(3, 4);
        map2.put(1, 2);
        // 3 + 7 = 10
        int expected = (1 ^ 2) + (3 ^ 4);
        assertAll(() -> assertEquals(expected, map1.hashCode(), "First map hash"), () -> assertEquals(expected, map2.hashCode(), "Second map hash"));
    }

    @Test
    @DisplayName("Hash code updates correctly after removing an entry")
    void hashCode_afterRemove() {
        map.put(1, 2);
        map.put(3, 4);
        // 10
        int full = (1 ^ 2) + (3 ^ 4);
        assertEquals(full, map.hashCode(), "Initial hash");
        map.remove(1);
        // 7
        int afterRemove = (3 ^ 4);
        assertEquals(afterRemove, map.hashCode(), "Hash after removing entry (1→2) must reflect remaining entries");
    }

    @Test
    @DisplayName("Clearing the map resets hash code to 0")
    void hashCode_afterClear() {
        map.put("a", "b");
        map.put(null, "c");
        assertNotEquals(0, map.hashCode(), "Pre‑condition: hash should be non‑zero");
        map.clear();
        assertEquals(0, map.hashCode(), "After clear, hash must be 0");
    }

    @Test
    @DisplayName("Null entrySet leads to NullPointerException")
    void hashCode_entrySetNull() {
        // Spy the map to stub entrySet()
        TestMap<String, String> spy = spy(map);
        when(spy.entrySet()).thenReturn(null);
        assertThrows(NullPointerException.class, spy::hashCode, "hashCode should throw NPE when entrySet() returns null");
    }

    @Test
    @DisplayName("entrySet containing a null entry leads to NullPointerException")
    void hashCode_entrySetContainsNullEntry() {
        TestMap<String, String> spy = spy(map);
        // Create a Set with a single null element
        Set<Map.Entry<String, String>> badSet = new HashSet<>(Collections.singleton(null));
        when(spy.entrySet()).thenReturn(badSet);
        assertThrows(NullPointerException.class, spy::hashCode, "Iterating over a null entry should cause NPE");
    }

    @Test
    @DisplayName("Exception thrown from key.hashCode() propagates")
    void hashCode_keyHashException() {
        // Key whose hashCode throws RuntimeException
        Object badKey = new Object() {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad key");
            }
        };
        map.put(badKey, "value");
        RuntimeException ex = assertThrows(RuntimeException.class, map::hashCode, "Exception from key.hashCode() must propagate");
        assertEquals("bad key", ex.getMessage());
    }

    @Test
    @DisplayName("Exception thrown from value.hashCode() propagates")
    void hashCode_valueHashException() {
        // Value whose hashCode throws RuntimeException
        Object badValue = new Object() {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad value");
            }
        };
        map.put("key", badValue);
        RuntimeException ex = assertThrows(RuntimeException.class, map::hashCode, "Exception from value.hashCode() must propagate");
        assertEquals("bad value", ex.getMessage());
    }

    @Test
    @DisplayName("ConcurrentModificationException from iterator propagates")
    void hashCode_iteratorConcurrentModification() {
        TestMap<String, String> spy = spy(map);
        // Custom Set whose iterator throws CME on next()
        Set<Map.Entry<String, String>> badSet = new AbstractSet<>() {

            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                return new Iterator<>() {

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Map.Entry<String, String> next() {
                        throw new ConcurrentModificationException("CM");
                    }
                };
            }

            @Override
            public int size() {
                return 1;
            }
        };
        when(spy.entrySet()).thenReturn(badSet);
        assertThrows(ConcurrentModificationException.class, spy::hashCode, "Iterator throwing CME must propagate");
    }

    @Test
    @DisplayName("RuntimeException from iterator.next() propagates")
    void hashCode_iteratorRuntimeException() {
        TestMap<String, String> spy = spy(map);
        // Custom Set whose iterator throws IllegalStateException on next()
        Set<Map.Entry<String, String>> badSet = new AbstractSet<>() {

            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                return new Iterator<>() {

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Map.Entry<String, String> next() {
                        throw new IllegalStateException("illegal state");
                    }
                };
            }

            @Override
            public int size() {
                return 1;
            }
        };
        when(spy.entrySet()).thenReturn(badSet);
        assertThrows(IllegalStateException.class, spy::hashCode, "Iterator throwing IllegalStateException must propagate");
    }

    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        Map<K, V> m = new HashMap<>();
        for (Map.Entry<K, V> e : entries) {
            m.put(e.getKey(), e.getValue());
        }
        return m;
    }
}
