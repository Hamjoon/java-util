package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_headMap_2_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test to guarantee isolation.
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // null is considered the greatest key.
        map.put(null, 99);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference – helps GC in long test suites.
        map = null;
    }

    private static Stream<Arguments> basicHeadMapProvider() {
        return Stream.of(// toKey "c" -> a,b
        Arguments.of("c", 2, List.of("a", "b")), // toKey "a" -> empty
        Arguments.of("a", 0, List.of()), // toKey "z" -> a,b,c,d (null excluded)
        Arguments.of("z", 4, List.of("a", "b", "c", "d")), // toKey null -> all non‑null keys
        Arguments.of(null, 4, List.of("a", "b", "c", "d")));
    }

    @Test
    void headMapExcludesToKey_nonInclusive() {
        // GIVEN a map with keys "a", "b", "c" and null.
        // WHEN we request a headMap up to "b" (exclusive)
        NavigableMap<String, Integer> head = map.headMap("b");
        // THEN the resulting view must contain only keys less than "b"
        assertEquals(1, head.size(), "Expected only one entry before key 'b'");
        assertTrue(head.containsKey("a"));
        assertFalse(head.containsKey("b"));
        assertFalse(head.containsKey("c"));
        assertFalse(head.containsKey(null));
        // The original map must remain unchanged.
        assertEquals(4, map.size());
    }

    @Test
    void headMapWithNullKey_usesNullAsGreatestKey() {
        // GIVEN a map where null is the greatest key.
        // WHEN we request a headMap up to null (exclusive)
        NavigableMap<String, Integer> head = map.headMap(null);
        // THEN the view should contain all non‑null keys, but not the null key itself.
        assertEquals(3, head.size(), "All non‑null keys should be present");
        assertTrue(head.containsKey("a"));
        assertTrue(head.containsKey("b"));
        assertTrue(head.containsKey("c"));
        assertFalse(head.containsKey(null));
        // The original map still contains the null entry.
        assertTrue(map.containsKey(null));
        assertEquals(99, map.get(null));
    }

    @Test
    void headMapIsLiveView_reflectsChangesBothWays() {
        // GIVEN a headMap view up to "c" (exclusive)
        NavigableMap<String, Integer> head = map.headMap("c");
        // WHEN we add a new entry via the view
        head.put("b2", 22);
        // THEN the original map must reflect the addition
        assertEquals(22, map.get("b2"));
        assertTrue(map.containsKey("b2"));
        assertEquals(5, map.size());
        // WHEN we remove an entry via the original map that is present in the view
        map.remove("a");
        // THEN the view must no longer contain that entry
        assertFalse(head.containsKey("a"));
        // should contain "b" and "b2"
        assertEquals(2, head.size());
    }

    @Test
    void headMapReturnedIsConcurrentNavigableMap() {
        // GIVEN a request for a headMap
        ConcurrentNavigableMap<String, Integer> head = map.headMap("c");
        // THEN the returned type should be a ConcurrentNavigableMap (live view)
        assertTrue(head instanceof ConcurrentNavigableMap, "headMap should return a ConcurrentNavigableMap implementation");
    }

    // ----------------------------------------------------------------------
    @Test
    void headMapOnEmptyMapReturnsEmpty() {
        // make the map empty
        map.clear();
        NavigableMap<String, Integer> head = map.headMap("c");
        assertTrue(head.isEmpty(), "headMap of an empty map must be empty");
    }

    @Test
    void headMapLessThanSmallestKeyReturnsEmpty() {
        map.clear();
        map.put("d", 4);
        map.put("e", 5);
        NavigableMap<String, Integer> head = map.headMap("c");
        assertTrue(head.isEmpty(), "No keys are less than the smallest key 'd'");
    }

    @Test
    void headMapEqualToSmallestKeyReturnsEmpty() {
        map.clear();
        map.put("d", 4);
        map.put("e", 5);
        NavigableMap<String, Integer> head = map.headMap("d");
        assertTrue(head.isEmpty(), "Exclusive view excludes the smallest key itself");
    }

    @Test
    void headMapBetweenTwoKeysReturnsOnlyLowerKey() {
        map.clear();
        map.put("d", 4);
        map.put("e", 5);
        NavigableMap<String, Integer> head = map.headMap("e");
        assertEquals(1, head.size());
        assertTrue(head.containsKey("d"));
        assertFalse(head.containsKey("e"));
    }

    @Test
    void headMapGreaterThanAllKeysReturnsAllEntries() {
        map.clear();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        NavigableMap<String, Integer> head = map.headMap("z");
        assertEquals(4, head.size());
        assertTrue(head.keySet().containsAll(List.of("a", "b", "c", "d")));
    }

    @Test
    void headMapExclusiveSingleStep() {
        map.clear();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        NavigableMap<String, Integer> head = map.headMap("b");
        assertEquals(1, head.size());
        assertTrue(head.containsKey("a"));
    }

    @Test
    void headMapToSmallestKeyReturnsEmpty() {
        map.clear();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        NavigableMap<String, Integer> head = map.headMap("a");
        assertTrue(head.isEmpty());
    }

    @Test
    void headMapAlphabetRange() {
        map.clear();
        for (char ch = 'a'; ch <= 'z'; ch++) {
            map.put(String.valueOf(ch), (int) ch);
        }
        NavigableMap<String, Integer> head = map.headMap("m");
        // Expected keys are a..l (12 keys)
        assertEquals(12, head.size());
        for (char ch = 'a'; ch < 'm'; ch++) {
            assertTrue(head.containsKey(String.valueOf(ch)), "Expected key '" + ch + "' in headMap");
        }
        assertFalse(head.containsKey("m"));
    }

    @Test
    void headMapLiveModificationReflectsBothWays() {
        map.clear();
        for (char ch = 'a'; ch <= 'z'; ch++) {
            map.put(String.valueOf(ch), (int) ch);
        }
        NavigableMap<String, Integer> head = map.headMap("m");
        // add via view
        head.put("l", 100);
        assertEquals(100, map.get("l"));
        // remove via original map
        map.remove("k");
        assertFalse(head.containsKey("k"));
        // l..a except k
        assertEquals(11, head.size());
    }

    // ----------------------------------------------------------------------
    @Test
    void headMapSingleElementMap_toKeyLessThanOnlyKey() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(10, 10);
        NavigableMap<Integer, Integer> head = intMap.headMap(5);
        assertTrue(head.isEmpty(), "toKey < only key should give empty headMap");
    }

    @Test
    void headMapSingleElementMap_toKeyEqualToOnlyKey() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(10, 10);
        NavigableMap<Integer, Integer> head = intMap.headMap(10);
        assertTrue(head.isEmpty(), "Exclusive view with toKey == only key must be empty");
    }

    @Test
    void headMapSingleElementMap_toKeyGreaterThanOnlyKey() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(10, 10);
        NavigableMap<Integer, Integer> head = intMap.headMap(15);
        assertEquals(1, head.size());
        assertTrue(head.containsKey(10));
    }

    // ----------------------------------------------------------------------
    @Test
    void headMapWithReverseComparator() {
        // Reverse order comparator: larger natural keys are considered "less"
        ConcurrentNavigableMapNullSafe<Integer, Integer> revMap = new ConcurrentNavigableMapNullSafe<>(Comparator.reverseOrder());
        revMap.put(1, 1);
        revMap.put(2, 2);
        revMap.put(3, 3);
        // In reverse order, keys "less than" 2 are 3
        NavigableMap<Integer, Integer> head = revMap.headMap(2);
        assertEquals(1, head.size());
        assertTrue(head.containsKey(3));
    }

    @Test
    void headMapWithReverseComparator_toKeySmallest() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> revMap = new ConcurrentNavigableMapNullSafe<>(Comparator.reverseOrder());
        revMap.put(1, 1);
        revMap.put(2, 2);
        revMap.put(3, 3);
        // No key is "less than" 1 in reverse order
        NavigableMap<Integer, Integer> head = revMap.headMap(1);
        assertTrue(head.isEmpty());
    }

    // ----------------------------------------------------------------------
    @Test
    void iteratorRemovalFromHeadMapReflectsInOriginal() {
        map.clear();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);
        NavigableMap<String, Integer> head = map.headMap("d");
        Iterator<String> it = head.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if ("b".equals(key)) {
                // remove via iterator
                it.remove();
                break;
            }
        }
        assertFalse(map.containsKey("b"));
        assertEquals(4, map.size());
    }

    // ----------------------------------------------------------------------
    @Test
    void headMapIncludesEntriesWithNullValues() {
        map.clear();
        map.put("x", null);
        map.put("y", 5);
        NavigableMap<String, Integer> head = map.headMap("y");
        assertEquals(1, head.size());
        assertTrue(head.containsKey("x"));
        assertNull(head.get("x"));
    }

    // ----------------------------------------------------------------------
    @Test
    void headMapReflectsRemovalsFromOriginalMap() {
        map.clear();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);
        NavigableMap<String, Integer> head = map.headMap("e");
        // a,b,c,d
        assertEquals(4, head.size());
        map.remove("c");
        assertFalse(head.containsKey("c"));
        assertEquals(3, head.size());
    }

    // ----------------------------------------------------------------------
    @Test
    void headMapWithIncompatibleKeyThrowsClassCastException() {
        // Map with String keys; passing an Object of unrelated type should fail.
        assertThrows(ClassCastException.class, () -> map.headMap(new Object()));
    }

    @Test
    void headMapWithKeyOutsideSubMapBoundsThrowsIllegalArgumentException() {
        // Create a bounded subMap (from "b" inclusive to "d" exclusive)
        ConcurrentNavigableMapNullSafe<String, Integer> sub = (ConcurrentNavigableMapNullSafe<String, Integer>) map.subMap("b", "d");
        // The subMap only knows keys "b" and "c". Asking for a headMap with "z" is out of range.
        assertThrows(IllegalArgumentException.class, () -> sub.headMap("z"));
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "{index}: toKey=''{0}'' expects size {1}")
    @MethodSource("basicHeadMapProvider")
    void headMapBasicParameterized(String toKey, int expectedSize, List<String> expectedKeys) {
        // Build a map with keys a through d (plus null)
        map.clear();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put(null, 99);
        NavigableMap<String, Integer> head = map.headMap(toKey);
        assertEquals(expectedSize, head.size(), () -> "Unexpected size for headMap(\"" + toKey + "\")");
        for (String k : expectedKeys) {
            assertTrue(head.containsKey(k), "Expected key '" + k + "' in headMap");
        }
    }
}
