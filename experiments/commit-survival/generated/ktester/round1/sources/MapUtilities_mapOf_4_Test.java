package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapOf_4_Test {

    @Test
    void mapOfCreatesMapWithGivenEntries() {
        // given
        String k1 = "first";
        Integer v1 = 1;
        String k2 = "second";
        Integer v2 = 2;
        String k3 = "third";
        Integer v3 = 3;
        // when
        Map<String, Integer> result = MapUtilities.mapOf(k1, v1, k2, v2, k3, v3);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(3, result.size(), "Map should contain exactly three entries");
        assertEquals(v1, result.get(k1), "Value for first key is incorrect");
        assertEquals(v2, result.get(k2), "Value for second key is incorrect");
        assertEquals(v3, result.get(k3), "Value for third key is incorrect");
    }

    @Test
    void mapOfPreservesInsertionOrder() {
        // given
        String k1 = "alpha";
        String k2 = "beta";
        String k3 = "gamma";
        // when
        Map<String, String> map = MapUtilities.mapOf(k1, "A", k2, "B", k3, "C");
        // then – LinkedHashMap preserves insertion order, and the returned map
        // is a view of that LinkedHashMap, so iteration order must match.
        List<String> observedOrder = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            observedOrder.add(entry.getKey());
        }
        assertEquals(Arrays.asList(k1, k2, k3), observedOrder, "Map iteration order should follow the order of arguments");
    }

    @Test
    void mapOfReturnsUnmodifiableMap() {
        // given
        Map<String, Integer> map = MapUtilities.mapOf("a", 10, "b", 20, "c", 30);
        // then – any mutating operation must throw UnsupportedOperationException
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> map.put("d", 40), "put should not be allowed on the returned map"), () -> assertThrows(UnsupportedOperationException.class, () -> map.remove("a"), "remove should not be allowed on the returned map"), () -> assertThrows(UnsupportedOperationException.class, () -> map.clear(), "clear should not be allowed on the returned map"));
    }

    @Test
    void mapOfAllowsNullKeysAndValues() {
        // given
        String k1 = null;
        String v1 = null;
        String k2 = "key";
        String v2 = null;
        String k3 = null;
        String v3 = "value";
        // when
        Map<String, String> map = MapUtilities.mapOf(k1, v1, k2, v2, k3, v3);
        // then
        assertEquals(3, map.size(), "Map should contain three entries even with nulls");
        assertTrue(map.containsKey(null), "Map should contain a null key");
        assertTrue(map.containsValue(null), "Map should contain a null value");
        assertEquals(v1, map.get(k1));
        assertEquals(v2, map.get(k2));
        assertEquals(v3, map.get(k3));
        // and the map must still be immutable
        assertThrows(UnsupportedOperationException.class, () -> map.put("new", "val"));
    }

    @Test
    void duplicateKeyOverwritesValueButKeepsOriginalOrder() {
        // given duplicate key "A"
        Map<String, Integer> map = MapUtilities.mapOf("A", 1, "B", 2, "A", 3);
        // then
        assertEquals(2, map.size(), "Map should contain only two distinct keys");
        assertEquals(3, map.get("A"), "Value for duplicate key should be the last one supplied");
        assertEquals(2, map.get("B"), "Value for B should remain unchanged");
        // order must be A then B (original insertion order of first occurrence)
        List<String> order = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            order.add(e.getKey());
        }
        assertEquals(Arrays.asList("A", "B"), order, "Insertion order should reflect first occurrence of each key");
    }

    @Test
    void mapOfWorksWithNonStringKeyTypes() {
        // given Integer keys and String values
        Map<Integer, String> map = MapUtilities.mapOf(10, "ten", 20, "twenty", 30, "thirty");
        // then
        assertEquals(3, map.size(), "Map should contain three entries");
        assertEquals("ten", map.get(10));
        assertEquals("twenty", map.get(20));
        assertEquals("thirty", map.get(30));
        // verify immutability
        assertThrows(UnsupportedOperationException.class, () -> map.put(40, "forty"));
    }

    @Test
    void mapOfPutOperationIsProhibited() {
        Map<String, Integer> map = MapUtilities.mapOf("X", 10, "Y", 20, "Z", 30);
        assertThrows(UnsupportedOperationException.class, () -> map.put("newKey", 99), "put should throw UnsupportedOperationException on unmodifiable map");
    }

    @Test
    void mapOfRemoveOperationIsProhibited() {
        Map<String, Integer> map = MapUtilities.mapOf("A", 1, "B", 2, "C", 3);
        assertThrows(UnsupportedOperationException.class, () -> map.remove("A"), "remove should throw UnsupportedOperationException on unmodifiable map");
    }

    @Test
    void mapOfClearOperationIsProhibited() {
        Map<String, Integer> map = MapUtilities.mapOf("X", 10, "Y", 20, "Z", 30);
        assertThrows(UnsupportedOperationException.class, map::clear, "clear should throw UnsupportedOperationException on unmodifiable map");
    }

    @Test
    void mapOfPutAllOperationIsProhibited() {
        Map<String, Integer> map = MapUtilities.mapOf("K1", 100, "K2", 200, "K3", 300);
        Map<String, Integer> another = new HashMap<>();
        another.put("extra", 400);
        assertThrows(UnsupportedOperationException.class, () -> map.putAll(another), "putAll should throw UnsupportedOperationException on unmodifiable map");
    }

    @Test
    void entrySetIteratorRemoveIsProhibited() {
        Map<String, Integer> map = MapUtilities.mapOf("alpha", 1, "beta", 2, "gamma", 3);
        Iterator<Map.Entry<String, Integer>> iterator = map.entrySet().iterator();
        assertTrue(iterator.hasNext(), "Iterator should have at least one element");
        // advance to first entry
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove, "Iterator.remove should throw UnsupportedOperationException on unmodifiable map");
    }

    @Test
    void entrySetValueMutationIsProhibited() {
        Map<String, Integer> map = MapUtilities.mapOf("one", 1, "two", 2, "three", 3);
        Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(999), "Map.Entry.setValue should throw UnsupportedOperationException on unmodifiable map");
    }
}
