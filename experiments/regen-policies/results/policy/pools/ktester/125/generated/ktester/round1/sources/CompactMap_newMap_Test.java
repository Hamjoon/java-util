package com.cedarsoftware.util;

import java.util.*;
import static com.cedarsoftware.util.CompactMap.*;
// import org.junit.Test;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CompactMap_newMap_Test {

    /**
     * Verify that {@code CompactMap.newMap} works with an empty options map.
     * The method should fall back to all default configuration values and return a usable
     * {@link CompactMap} instance.
     */
//     @Test
//     void newMap_WithEmptyOptions_ReturnsDefaultCompactMap() {
//         // GIVEN – an empty options map (all defaults will be used)
//         Map<String, Object> options = new HashMap<>();
//         // WHEN – a CompactMap is created via the static factory
//         CompactMap<String, Object> map = CompactMap.newMap(options);
//         // THEN – the map is a non‑null CompactMap and behaves like a normal map
//         assertNotNull("newMap should never return null", map);
//         assertTrue("Returned instance should be a CompactMap", map instanceof CompactMap);
//         assertTrue("Newly created map should be empty", map.isEmpty());
//         // Adding a couple of entries must work and affect size correctly
//         map.put("first", 1);
//         map.put("second", 2);
//         assertEquals("Map size should reflect added entries", 2, map.size());
//         assertEquals(Integer.valueOf(1), map.get("first"));
//         assertEquals(Integer.valueOf(2), map.get("second"));
//     }

    /**
     * Verify that a source map supplied via the {@code SOURCE_MAP} option is copied into the
     * newly created CompactMap.
     */
//     @Test
//     void newMap_WithSourceMap_CopiesEntries() {
//         // GIVEN – a source map with a few entries
//         Map<String, Integer> source = new HashMap<>();
//         source.put("alpha", 10);
//         source.put("beta", 20);
//         source.put("gamma", 30);
//         // Options include the source map; all other options are left to defaults
//         Map<String, Object> options = new HashMap<>();
//         options.put(SOURCE_MAP, source);
//         // WHEN – the CompactMap is created
//         CompactMap<String, Integer> map = CompactMap.newMap(options);
//         // THEN – the map contains exactly the entries from the source map
//         assertEquals("Map should contain the same number of entries as source", source.size(), map.size());
//         for (Map.Entry<String, Integer> e : source.entrySet()) {
//             assertTrue("Map should contain key: " + e.getKey(), map.containsKey(e.getKey()));
//             assertEquals("Value for key " + e.getKey() + " should match source", e.getValue(), map.get(e.getKey()));
//         }
//         // Modifying the source after creation must NOT affect the CompactMap (defensive copy)
//         source.put("delta", 40);
//         assertFalse("Map should not reflect modifications to the original source map", map.containsKey("delta"));
//     }

    /**
     * Verify that explicit configuration options are honoured:
     * <ul>
     *   <li>Ordering set to {@code SORTED}</li>
     *   <li>Backing map type set to {@code TreeMap.class}</li>
     * </ul>
     * The resulting map must implement {@link SortedMap}.
     */
//     @Test
//     void newMap_WithSortedOrderingAndTreeMap_Backings_ReturnsSortedMap() {
//         // GIVEN – options that request a sorted map backed by TreeMap
//         Map<String, Object> options = new HashMap<>();
//         options.put(MAP_TYPE, TreeMap.class);
//         options.put(ORDERING, SORTED);
//         options.put(CASE_SENSITIVE, true);
//         options.put(COMPACT_SIZE, 5);
//         options.put(SINGLE_KEY, "id");
//         // WHEN – a CompactMap is created
//         CompactMap<String, String> map = CompactMap.newMap(options);
//         // THEN – the returned map must be a SortedMap (TreeMap semantics)
//         assertTrue("Map should be a SortedMap when SORTED ordering is requested", map instanceof SortedMap);
//         SortedMap<String, String> sortedMap = (SortedMap<String, String>) map;
//         // Populate with out‑of‑order keys and verify natural ordering
//         sortedMap.put("c", "C");
//         sortedMap.put("a", "A");
//         sortedMap.put("b", "B");
//         Iterator<String> keyIter = sortedMap.keySet().iterator();
//         assertEquals("a", keyIter.next());
//         assertEquals("b", keyIter.next());
//         assertEquals("c", keyIter.next());
//     }

//     @Test
//     void newMap_CaseInsensitiveMap_WrapsInnerHashMap_KeysAreCaseInsensitive() {
//         // Configure a CaseInsensitiveMap wrapper around a plain HashMap and disable case sensitivity.
//         Map<String, Object> options = new HashMap<>();
//         options.put(MAP_TYPE, "com.cedarsoftware.util.CaseInsensitiveMap");
//         // option name used internally
//         options.put("INNER_MAP_TYPE", "java.util.HashMap");
//         options.put(CASE_SENSITIVE, false);
//         CompactMap<String, Integer> map = CompactMap.newMap(options);
//         // Insert the same logical key with different capitalisation.
//         map.put("Key", 1);
//         map.put("key", 2);
//         assertEquals("Case‑insensitive map should treat \"Key\" and \"key\" as the same entry", 1, map.size());
//         assertEquals(Integer.valueOf(2), map.get("Key"));
//         assertEquals(Integer.valueOf(2), map.get("key"));
//     }

//     @Test
//     void newMap_WithReverseOrdering_ReturnsMapIteratingInDescendingOrder() {
//         Map<String, Object> options = new HashMap<>();
//         options.put(MAP_TYPE, TreeMap.class);
//         options.put(ORDERING, REVERSE);
//         options.put(CASE_SENSITIVE, true);
//         CompactMap<String, String> map = CompactMap.newMap(options);
//         assertTrue("Map should be a SortedMap when REVERSE ordering is requested", map instanceof SortedMap);
//         SortedMap<String, String> sortedMap = (SortedMap<String, String>) map;
//         sortedMap.put("a", "A");
//         sortedMap.put("b", "B");
//         sortedMap.put("c", "C");
//         Iterator<String> iter = sortedMap.keySet().iterator();
//         assertEquals("c", iter.next());
//         assertEquals("b", iter.next());
//         assertEquals("a", iter.next());
//     }

//     @Test
//     void newMap_WithSmallCompactSize_TransitionsToBackingMapWithoutError() {
//         Map<String, Object> options = new HashMap<>();
//         options.put(COMPACT_SIZE, 2);
//         options.put(MAP_TYPE, HashMap.class);
//         CompactMap<String, Integer> map = CompactMap.newMap(options);
//         map.put("one", 1);
//         map.put("two", 2);
//         // triggers transition to backing map
//         map.put("three", 3);
//         assertEquals("Map should hold all entries after exceeding compact size", 3, map.size());
//         assertEquals(Integer.valueOf(1), map.get("one"));
//         assertEquals(Integer.valueOf(2), map.get("two"));
//         assertEquals(Integer.valueOf(3), map.get("three"));
//     }

//     @Test
//     void newMap_WithInvalidMapTypeString_ThrowsIllegalStateException() {
//         Map<String, Object> options = new HashMap<>();
//         // not a Map implementation
//         options.put(MAP_TYPE, "java.lang.String");
//         IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
//         assertTrue("Exception message should mention map type validation", ex.getMessage().contains("mapType must be a Map class"));
//     }

//     @Test
//     void newMap_MissingInnerMapForCaseInsensitive_ThrowsException() {
//         Map<String, Object> options = new HashMap<>();
//         options.put(MAP_TYPE, "com.cedarsoftware.util.CaseInsensitiveMap");
//         // No INNER_MAP_TYPE supplied – generation should fail.
//         Exception ex = assertThrows(Exception.class, () -> CompactMap.newMap(options));
//         assertNotNull(ex);
//     }

//     @Test
//     void newMap_NullOptions_ThrowsNullPointerException() {
//         CompactMap.newMap(null);
//     }

//     @Test
//     void newMap_UnmodifiableOptionsMap_ThrowsUnsupportedOperationException() {
//         Map<String, Object> mutable = new HashMap<>();
//         Map<String, Object> options = Collections.unmodifiableMap(mutable);
//         CompactMap.newMap(options);
//     }

//     @Test
//     void newMap_MapTypeNotClassOrString_ThrowsClassCastException() {
//         Map<String, Object> options = new HashMap<>();
//         // Integer, not Class nor String
//         options.put(MAP_TYPE, 123);
//         CompactMap.newMap(options);
//     }

//     @Test
//     void newMap_MapTypeClassNotImplementingMap_ThrowsIllegalStateException() {
//         Map<String, Object> options = new HashMap<>();
//         // Integer does not implement Map
//         options.put(MAP_TYPE, Integer.class);
//         IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
//         assertTrue("Exception message should mention map type validation", ex.getMessage().contains("mapType must be a Map class"));
//     }

//     @Test
//     void newMap_MapTypeWithoutDefaultConstructor_ThrowsIllegalStateException() {
//         Map<String, Object> options = new HashMap<>();
//         options.put(MAP_TYPE, EnumMap.class);
//         // required for CaseInsensitiveMap path, not used here
//         options.put("INNER_MAP_TYPE", HashMap.class);
//         IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
//         assertTrue("Exception message should indicate map instantiation failure", ex.getMessage().contains("Failed to create map instance"));
//     }

//     @Test
//     void newMap_NegativeCompactSize_ThrowsIllegalStateException() {
//         Map<String, Object> options = new HashMap<>();
//         options.put(COMPACT_SIZE, -5);
//         options.put(MAP_TYPE, HashMap.class);
//         IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
//         assertTrue("Exception message should indicate map creation failure due to illegal capacity", ex.getMessage().contains("Failed to create map instance"));
//     }

//     @Test
//     void newMap_CaseSensitiveOptionWrongType_ThrowsClassCastException() {
//         Map<String, Object> options = new HashMap<>();
//         // should be Boolean
//         options.put(CASE_SENSITIVE, "yes");
//         options.put(MAP_TYPE, HashMap.class);
//         CompactMap.newMap(options);
//     }
}