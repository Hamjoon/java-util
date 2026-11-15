package com.cedarsoftware.util;

import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
// import java.util.concurrent.ConcurrentModificationException;

class CompactMap_keySet_Test {

    private CompactMap<String, Integer> map;

//     @BeforeEach
    void setupBeforeEach() {
        // Create a CompactMap with a very small compactSize so we can test
        // the transition between internal representations easily.
        map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                // small size to trigger backing map quickly
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // Use a deterministic map implementation for predictable ordering
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                // Use a custom single‑value key to avoid the default “id”
                return "value";
            }
        };
    }

//     @AfterEach
    void teardownAfterEach() {
        // Ensure map is cleared after each test to avoid cross‑test contamination
        map.clear();
    }

//     @Test
//     void keySetReflectsMapSizeAndContents() {
//         // initially empty
//         Set<String> ks = map.keySet();
//         assertTrue(ks.isEmpty());
//         assertEquals(0, ks.size());
//         // add entries
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         // keySet should now contain the three keys
//         assertEquals(3, ks.size());
//         assertTrue(ks.containsAll(Arrays.asList("a", "b", "c")));
//     }

//     @Test
//     void iteratorProvidesAllKeysAndSupportsRemove() {
//         map.put("x", 10);
//         map.put("y", 20);
//         map.put("z", 30);
//         Set<String> ks = map.keySet();
//         Iterator<String> it = ks.iterator();
//         List<String> iterated = new ArrayList<>();
//         while (it.hasNext()) {
//             String key = it.next();
//             iterated.add(key);
//             // remove the first key we encounter
//             if ("y".equals(key)) {
//                 it.remove();
//             }
//         }
//         // All keys were visited
//         assertTrue(iterated.containsAll(Arrays.asList("x", "y", "z")));
//         // "y" should have been removed from the map
//         assertFalse(map.containsKey("y"));
//         assertEquals(2, map.size());
//     }

//     @Test
//     void clearViaKeySetEmptiesMap() {
//         map.put("one", 1);
//         map.put("two", 2);
//         assertFalse(map.isEmpty());
//         // clear through the keySet view
//         map.keySet().clear();
//         assertTrue(map.isEmpty());
//         assertTrue(map.keySet().isEmpty());
//     }

//     @Test
//     void containsDelegatesToMapContainsKey() {
//         map.put("present", 42);
//         Set<String> ks = map.keySet();
//         assertTrue(ks.contains("present"));
//         assertFalse(ks.contains("absent"));
//     }

//     @Test
//     void removeViaKeySetUpdatesMap() {
//         map.put("alpha", 100);
//         map.put("beta", 200);
//         Set<String> ks = map.keySet();
//         // remove an existing key
//         boolean changed = ks.remove("alpha");
//         assertTrue(changed);
//         assertFalse(map.containsKey("alpha"));
//         assertEquals(1, map.size());
//         // attempt to remove a non‑existent key
//         boolean unchanged = ks.remove("gamma");
//         assertFalse(unchanged);
//         assertEquals(1, map.size());
//     }

//     @Test
//     void removeAllViaKeySetRemovesAllSpecifiedKeys() {
//         map.put("k1", 1);
//         map.put("k2", 2);
//         map.put("k3", 3);
//         map.put("k4", 4);
//         Set<String> ks = map.keySet();
//         Collection<String> toRemove = Arrays.asList("k2", "k4", "nonexistent");
//         boolean changed = ks.removeAll(toRemove);
//         assertTrue(changed);
//         assertFalse(map.containsKey("k2"));
//         assertFalse(map.containsKey("k4"));
//         assertTrue(map.containsKey("k1"));
//         assertTrue(map.containsKey("k3"));
//         assertEquals(2, map.size());
//     }

//     @Test
//     void retainAllViaKeySetKeepsOnlySpecifiedKeys() {
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         map.put("d", 4);
//         Set<String> ks = map.keySet();
//         Collection<String> toRetain = Arrays.asList("b", "c", "nonexistent");
//         boolean changed = ks.retainAll(toRetain);
//         assertTrue(changed);
//         assertFalse(map.containsKey("a"));
//         assertFalse(map.containsKey("d"));
//         assertTrue(map.containsKey("b"));
//         assertTrue(map.containsKey("c"));
//         assertEquals(2, map.size());
//     }

//     @Test
//     void removeIfViaKeySetWorksAsExpected() {
//         map.put("p", 5);
//         map.put("q", 6);
//         map.put("r", 7);
//         // remove keys that are lexicographically greater than "q"
//         boolean changed = map.keySet().removeIf(k -> k.compareTo("q") > 0);
//         assertTrue(changed);
//         assertTrue(map.containsKey("p"));
//         assertTrue(map.containsKey("q"));
//         assertFalse(map.containsKey("r"));
//         assertEquals(2, map.size());
//     }

    /* -------------------------------------------------------------
     *  Concurrent modification detection
     * ------------------------------------------------------------- */
//     @Test
//     void iteratorDetectsConcurrentModificationWhenMapIsModified() {
//         map.put("a", 1);
//         map.put("b", 2);
//         Set<String> ks = map.keySet();
//         Iterator<String> it = ks.iterator();
//         // modify the map after iterator creation
//         map.put("c", 3);
//         // the next call to the iterator must notice the structural change
//         assertThrows(ConcurrentModificationException.class, it::next, "Iterator should throw CME after map is modified");
//     }

//     @Test
//     void iteratorDetectsConcurrentModificationWhenKeySetRemoves() {
//         map.put("a", 1);
//         map.put("b", 2);
//         Set<String> ks = map.keySet();
//         Iterator<String> it = ks.iterator();
//         // remove via the keySet view (not via iterator)
//         ks.remove("a");
//         assertThrows(ConcurrentModificationException.class, it::next, "Iterator should throw CME after keySet.remove()");
//     }

//     @Test
//     void iteratorDetectsConcurrentModificationAfterClear() {
//         map.put("x", 10);
//         map.put("y", 20);
//         Set<String> ks = map.keySet();
//         Iterator<String> it = ks.iterator();
//         // clear the map via the view
//         ks.clear();
//         assertThrows(ConcurrentModificationException.class, it::next, "Iterator should throw CME after keySet.clear()");
//     }

//     @Test
//     void iteratorThrowsNoSuchElementExceptionWhenEmpty() {
//         Set<String> ks = map.keySet();
//         Iterator<String> it = ks.iterator();
//         assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
//     }

    /* -------------------------------------------------------------
     *  Null handling and contract violations
     * ------------------------------------------------------------- */
//     @Test
//     void removeNullKeyWhenAbsentReturnsFalse() {
//         // map does not contain null
//         boolean changed = map.keySet().remove(null);
//         assertFalse(changed, "Removing a non‑existent null key should return false");
//         assertEquals(0, map.size());
//     }

//     @Test
//     void removeNullKeyWhenPresentReturnsTrue() {
//         map.put(null, 999);
//         assertTrue(map.containsKey(null));
//         boolean changed = map.keySet().remove(null);
//         assertTrue(changed, "Removing an existing null key should return true");
//         assertFalse(map.containsKey(null));
//         assertEquals(0, map.size());
//     }

//     @Test
//     void removeAllWithNullCollectionThrowsNPE() {
//         assertThrows(NullPointerException.class, () -> map.keySet().removeAll(null), "removeAll(null) must throw NullPointerException per Collection contract");
//     }

//     @Test
//     void retainAllWithNullCollectionThrowsNPE() {
//         assertThrows(NullPointerException.class, () -> map.keySet().retainAll(null), "retainAll(null) must throw NullPointerException per Collection contract");
//     }

    /* -------------------------------------------------------------
     *  Type compatibility checks
     * ------------------------------------------------------------- */
//     @Test
//     void containsDifferentKeyTypeReturnsFalse() {
//         CompactMap<Integer, String> intMap = new CompactMap<Integer, String>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<Integer, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         intMap.put(1, "one");
//         intMap.put(2, "two");
//         intMap.put(3, "three");
//         Set<Integer> ks = intMap.keySet();
//         assertFalse(ks.contains("1"), "String key should not be considered present in Integer‑keyed map");
//         assertTrue(ks.contains(1), "Integer key 1 should be present");
//     }

    /* -------------------------------------------------------------
     *  Bulk operation size consistency
     * ------------------------------------------------------------- */
//     @Test
//     void sizeReflectsRemoveAllChanges() {
//         map.put("k1", 1);
//         map.put("k2", 2);
//         map.put("k3", 3);
//         map.put("k4", 4);
//         map.put("k5", 5);
//         Set<String> ks = map.keySet();
//         boolean changed = ks.removeAll(Arrays.asList("k1", "k2", "k3"));
//         assertTrue(changed);
//         assertEquals(2, ks.size());
//         assertEquals(2, map.size());
//     }

//     @Test
//     void sizeReflectsRetainAllChanges() {
//         map.put("a", 1);
//         map.put("b", 2);
//         map.put("c", 3);
//         map.put("d", 4);
//         Set<String> ks = map.keySet();
//         boolean changed = ks.retainAll(Arrays.asList("b", "d"));
//         assertTrue(changed);
//         assertEquals(2, ks.size());
//         assertEquals(2, map.size());
//     }

    /* -------------------------------------------------------------
     *  Ordering verification (insertion order)
     * ------------------------------------------------------------- */
//     @Test
//     void iteratorOrderMatchesInsertionOrder() {
//         // Create a map that preserves insertion order via the overridden getNewMap()
//         CompactMap<String, Integer> orderedMap = new CompactMap<String, Integer>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         orderedMap.put("first", 1);
//         orderedMap.put("second", 2);
//         orderedMap.put("third", 3);
//         List<String> iterated = new ArrayList<>();
//         for (String key : orderedMap.keySet()) {
//             iterated.add(key);
//         }
//         assertEquals(Arrays.asList("first", "second", "third"), iterated, "Iterator should follow insertion order");
//     }

//     @Test
//     void iteratorOrderMatchesReverseOrder() {
//         // Simulate reverse ordering by using a TreeMap with a reverse comparator
//         CompactMap<String, Integer> reverseMap = new CompactMap<String, Integer>() {
// 
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new TreeMap<>(Comparator.reverseOrder());
//             }
//         };
//         reverseMap.put("a", 1);
//         reverseMap.put("b", 2);
//         reverseMap.put("c", 3);
//         List<String> iterated = new ArrayList<>();
//         for (String key : reverseMap.keySet()) {
//             iterated.add(key);
//         }
//         assertEquals(Arrays.asList("c", "b", "a"), iterated, "Iterator should follow reverse order");
//     }

    /* -------------------------------------------------------------
     *  Clear after retainAll (no‑op clear)
     * ------------------------------------------------------------- */
//     @Test
//     void clearAfterRetainAllOnEmptyMapDoesNotThrow() {
//         map.put("x", 1);
//         map.put("y", 2);
//         // retainAll with empty collection empties the map
//         map.keySet().retainAll(Collections.emptyList());
//         assertTrue(map.isEmpty());
//         // subsequent clear should be a no‑op and not throw
//         assertDoesNotThrow(() -> map.keySet().clear());
//         assertTrue(map.isEmpty());
//         assertEquals(0, map.keySet().size());
//     }
}