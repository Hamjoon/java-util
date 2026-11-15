package com.cedarsoftware.util;

import java.util.*;
// import java.util.concurrent.ConcurrentModificationException;
// import org.junit.*;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CompactMap_getNewMap_Test {

    /**
     * A plain CompactMap instance that uses the default {@code getNewMap()} implementation.
     */
    private CompactMap<String, Integer> defaultMap;

    /**
     * A CompactMap instance that overrides {@code getNewMap()} to return a LinkedHashMap.
     */
    private CompactMap<String, Integer> overriddenMap;

//     @BeforeEach
    void setupBeforeEach() {
        // Default behaviour – no overrides.
        defaultMap = new CompactMap<String, Integer>() {
        };
        // Override getNewMap() to return a LinkedHashMap.
        overriddenMap = new CompactMap<String, Integer>() {

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
    }

//     @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly necessary.
        defaultMap = null;
        overriddenMap = null;
    }

//     static Stream<Arguments> nullKeyAndValueProvider() {
//         return Stream.of(Arguments.of(null, 123, "null key"), Arguments.of("nullValue", null, "null value"));
//     }

//     @Test
//     void defaultGetNewMapReturnsHashMap() {
//         Map<String, Integer> newMap = defaultMap.getNewMap();
//         assertNotNull("getNewMap() must not return null", newMap);
//         assertTrue("Default implementation should return a HashMap", newMap instanceof HashMap);
//         assertTrue("Newly created map should be empty", newMap.isEmpty());
//     }

//     @Test
//     void overriddenGetNewMapReturnsLinkedHashMap() {
//         Map<String, Integer> newMap = overriddenMap.getNewMap();
//         assertNotNull("Overridden getNewMap() must not return null", newMap);
//         assertTrue("Override should return a LinkedHashMap", newMap instanceof LinkedHashMap);
//         assertTrue("Newly created overridden map should be empty", newMap.isEmpty());
//     }

//     @Test
//     void getNewMapProducesDistinctInstances() {
//         Map<String, Integer> first = defaultMap.getNewMap();
//         Map<String, Integer> second = defaultMap.getNewMap();
//         assertNotSame("Each call to getNewMap() must produce a new map instance", first, second);
//         first.put("a", 1);
//         assertTrue(first.containsKey("a"));
//         assertFalse(second.containsKey("a"));
//     }

//     @Test
//     void mapReturnedCanBeUsedIndependently() {
//         Map<String, Integer> external = defaultMap.getNewMap();
//         external.put("key", 42);
//         assertEquals(42, (int) external.get("key"));
//         assertTrue("CompactMap should remain empty after using external map", defaultMap.isEmpty());
//         assertFalse(defaultMap.containsKey("key"));
//     }

//     @Test
//     void mapReturnedIsMutableAndReflectsSizeChanges() {
//         Map<String, Integer> map = defaultMap.getNewMap();
//         assertEquals("Initially empty map should have size 0", 0, map.size());
//         map.put("x", 10);
//         assertEquals("After one put, size must be 1", 1, map.size());
//         assertEquals(Integer.valueOf(10), map.get("x"));
//     }

//     @Test
//     void mapClassIsHashMapOrSubclass() {
//         Map<String, Integer> map = defaultMap.getNewMap();
//         assertTrue("Returned map should be a HashMap (or subclass)", map.getClass() == HashMap.class || HashMap.class.isAssignableFrom(map.getClass()));
//     }

//     @Test
//     void mapAcceptsNullKey() {
//         Map<String, Integer> map = defaultMap.getNewMap();
//         map.put(null, 99);
//         assertEquals(Integer.valueOf(99), map.get(null));
//         assertEquals(1, map.size());
//     }

//     @Test
//     void mapAcceptsNullValue() {
//         Map<String, Integer> map = defaultMap.getNewMap();
//         map.put("nullable", null);
//         assertTrue(map.containsKey("nullable"));
//         assertNull(map.get("nullable"));
//         assertEquals(1, map.size());
//     }

//     @Test
//     void putReturnsPreviousValueAccordingToMapContract() {
//         Map<String, Integer> map = defaultMap.getNewMap();
//         assertNull(map.put("a", 1));
//         assertEquals(Integer.valueOf(1), map.put("a", 2));
//         assertEquals(Integer.valueOf(2), map.get("a"));
//     }

//     @Test
//     void removeReturnsPreviousValueOrNullWhenAbsent() {
//         Map<String, Integer> map = defaultMap.getNewMap();
//         assertNull(map.remove("b"));
//         map.put("b", 3);
//         assertEquals(Integer.valueOf(3), map.remove("b"));
//         assertEquals(0, map.size());
//     }

//     @Test
//     void iteratorRemoveUpdatesMapCorrectly() {
//         Map<String, Integer> map = defaultMap.getNewMap();
//         map.put("k1", 1);
//         map.put("k2", 2);
//         map.put("k3", 3);
//         Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
//         while (it.hasNext()) {
//             Map.Entry<String, Integer> e = it.next();
//             if ("k2".equals(e.getKey())) {
//                 it.remove();
//             }
//         }
//         assertFalse(map.containsKey("k2"));
//         assertEquals(2, map.size());
//     }

//     @Test
//     void iteratorFailsFastOnConcurrentModification() {
//         Map<String, Integer> map = defaultMap.getNewMap();
//         map.put("x", 10);
//         map.put("y", 20);
//         Iterator<String> keyIt = map.keySet().iterator();
//         // structural modification
//         map.put("z", 30);
//         try {
//             keyIt.next();
//             fail("Iterator should be fail‑fast after concurrent modification");
//         } catch (ConcurrentModificationException expected) {
//             // expected
//         }
//     }

//     @Test
//     void mapsWithIdenticalEntriesAreEqualAndHaveSameHashCode() {
//         Map<String, Integer> m1 = defaultMap.getNewMap();
//         Map<String, Integer> m2 = defaultMap.getNewMap();
//         m1.put("x", 10);
//         m1.put("y", 20);
//         m2.put("x", 10);
//         m2.put("y", 20);
//         assertEquals(m1, m2);
//         assertEquals(m1.hashCode(), m2.hashCode());
//     }

//     @Test
//     void mapsWithDifferentEntriesAreNotEqual() {
//         Map<String, Integer> m1 = defaultMap.getNewMap();
//         Map<String, Integer> m2 = defaultMap.getNewMap();
//         m1.put("x", 10);
//         m1.put("y", 20);
//         m2.put("x", 10);
//         m2.put("y", 99);
//         assertNotEquals(m1, m2);
//     }

//     @Test
//     void toStringProducesReadableRepresentation() {
//         Map<String, Integer> map = defaultMap.getNewMap();
//         map.put("a", 1);
//         map.put("b", 2);
//         String s = map.toString();
//         assertNotNull(s);
//         assertTrue(s.contains("a=1") && s.contains("b=2"));
//         assertTrue(s.startsWith("{") && s.endsWith("}"));
//     }

//     @Test
//     void collectionViewsAreLiveAndReflectSubsequentModifications() {
//         Map<String, Integer> map = defaultMap.getNewMap();
//         Set<String> keys = map.keySet();
//         Collection<Integer> values = map.values();
//         Set<Map.Entry<String, Integer>> entries = map.entrySet();
//         assertTrue(keys.isEmpty() && values.isEmpty() && entries.isEmpty());
//         map.put("k1", 100);
//         map.put("k2", 200);
//         assertEquals(Set.of("k1", "k2"), keys);
//         assertEquals(Set.of(100, 200), new HashSet<>(values));
//         assertEquals(2, entries.size());
//         Iterator<String> keyIter = keys.iterator();
//         while (keyIter.hasNext()) {
//             if ("k1".equals(keyIter.next())) {
//                 keyIter.remove();
//             }
//         }
//         assertFalse(map.containsKey("k1"));
//         assertEquals(1, map.size());
//     }
}