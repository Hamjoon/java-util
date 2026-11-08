package com.cedarsoftware.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.NoSuchElementException;
// import org.junit.Test;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CompactMap_clear_Test {

    /**
     * Creates a {@link CompactMap} with a small {@code compactSize} so we can
     *  trigger the transition to a backing {@link LinkedHashMap} after a few inserts.
     */
    private CompactMap<String, Object> newTestMap() {
        return new CompactMap<>() {

            // Use a small compact size to force a transition to a backing map.
            @Override
            protected int compactSize() {
                return 3;
            }

            // The key that enables the “single‑value” optimisation.
            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            // Backing map type when we exceed the compact size.
            @Override
            protected Map<String, Object> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
    }

//     @Test
//     @DisplayName("clear() on an already empty map")
//     void testClearOnEmptyMap() {
//         CompactMap<String, Object> map = newTestMap();
//         assertTrue("Map should start empty", map.isEmpty());
//         assertEquals("Size should be 0", 0, map.size());
//         // The operation under test
//         map.clear();
//         assertTrue("Map should remain empty after clear()", map.isEmpty());
//         assertEquals("Size should stay 0 after clear()", 0, map.size());
//     }

//     @Test
//     @DisplayName("clear() after a single entry (single‑value optimisation)")
//     void testClearAfterSingleEntry() {
//         CompactMap<String, Object> map = newTestMap();
//         // Insert the special single‑value key
//         map.put("value", 42);
//         assertEquals("Size should be 1 after one put", 1, map.size());
//         assertFalse("Map should not be empty after one put", map.isEmpty());
//         assertEquals(Integer.valueOf(42), map.get("value"));
//         // Clear the map
//         map.clear();
//         assertTrue("Map should be empty after clear()", map.isEmpty());
//         assertEquals("Size should be 0 after clear()", 0, map.size());
//         assertNull("Previously stored value must be gone", map.get("value"));
//     }

//     @Test
//     @DisplayName("clear() after several entries that stay in the compact array state")
//     void testClearAfterCompactArrayState() {
//         CompactMap<String, Object> map = newTestMap();
//         // Add two entries – still within compactSize (3)
//         map.put("a", 1);
//         map.put("b", 2);
//         assertEquals("Size should be 2 (compact array state)", 2, map.size());
//         assertFalse(map.isEmpty());
//         // Verify contents before clear
//         assertEquals(Integer.valueOf(1), map.get("a"));
//         assertEquals(Integer.valueOf(2), map.get("b"));
//         // Clear the map
//         map.clear();
//         assertTrue("Map should be empty after clear()", map.isEmpty());
//         assertEquals("Size should be 0 after clear()", 0, map.size());
//         assertNull(map.get("a"));
//         assertNull(map.get("b"));
//     }

//     @Test
//     @DisplayName("clear() after the map has transitioned to a backing LinkedHashMap")
//     void testClearAfterBackingMapState() {
//         CompactMap<String, Object> map = newTestMap();
//         // Insert entries to exceed compactSize (3) → backing map is used
//         map.put("k1", "v1");
//         map.put("k2", "v2");
//         map.put("k3", "v3");
//         // triggers transition to backing map
//         map.put("k4", "v4");
//         assertEquals("Size should be 4 after exceeding compactSize", 4, map.size());
//         assertFalse(map.isEmpty());
//         // Verify a few values before clear
//         assertEquals("v1", map.get("k1"));
//         assertEquals("v4", map.get("k4"));
//         // Clear the map
//         map.clear();
//         assertTrue("Map should be empty after clear()", map.isEmpty());
//         assertEquals("Size should be 0 after clear()", 0, map.size());
//         assertNull(map.get("k1"));
//         assertNull(map.get("k4"));
//         // Ensure map can be reused after clear()
//         map.put("newKey", "newVal");
//         assertEquals(1, map.size());
//         assertEquals("newVal", map.get("newKey"));
//     }

//     @Test
//     @DisplayName("consecutive clear() calls on an empty map are no‑ops")
//     void testConsecutiveClearsOnEmptyMap() {
//         CompactMap<String, Object> map = newTestMap();
//         // First clear – map already empty
//         map.clear();
//         assertTrue("Map should stay empty after first clear()", map.isEmpty());
//         assertEquals(0, map.size());
//         // Second clear – should also be a no‑op and not throw
//         map.clear();
//         assertTrue("Map should stay empty after second clear()", map.isEmpty());
//         assertEquals(0, map.size());
//     }

//     @Test
//     @DisplayName("map behaves normally after clear(): new entries can be added")
//     void testMapReusableAfterClear() {
//         CompactMap<String, Object> map = newTestMap();
//         // Populate map
//         map.put("x", 100);
//         map.put("y", 200);
//         assertEquals(2, map.size());
//         // Clear it
//         map.clear();
//         assertTrue(map.isEmpty());
//         // Add a new entry and verify normal behavior
//         map.put("z", 300);
//         assertEquals(1, map.size());
//         assertEquals(Integer.valueOf(300), map.get("z"));
//         assertNull(map.get("x"));
//     }

//     @Test
//     @DisplayName("iterator obtained before clear() reflects cleared state")
//     void testIteratorAfterClear() {
//         CompactMap<String, Object> map = newTestMap();
//         map.put("a", 1);
//         map.put("b", 2);
//         // Obtain iterator before clearing
//         Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
//         // Clear the map
//         map.clear();
//         // The iterator should now be exhausted – next() must throw NoSuchElementException
//         try {
//             it.next();
//             fail("Iterator should have no elements after map.clear()");
//         } catch (NoSuchElementException expected) {
//             // expected
//         }
//     }

//     @Test
//     @DisplayName("case‑insensitive map retains case‑insensitivity after clear()")
//     void testCaseInsensitiveBehaviorPersistsAfterClear() {
//         // Build a case‑insensitive CompactMap via the public builder API
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().caseSensitive(// turn off case sensitivity
//         false).compactSize(// small size, not relevant for this test
//         5).singleValueKey(// default single‑value key
//         "id").build();
//         // Insert keys with mixed case
//         map.put("KeyOne", "value1");
//         map.put("KEYTWO", "value2");
//         assertEquals(2, map.size());
//         // Verify case‑insensitive retrieval works before clear
//         assertEquals("value1", map.get("keyone"));
//         assertEquals("value2", map.get("keytwo"));
//         // Clear the map
//         map.clear();
//         assertTrue(map.isEmpty());
//         // Insert a new entry using a different case and verify retrieval is still case‑insensitive
//         map.put("NewKey", "newValue");
//         assertEquals(1, map.size());
//         assertEquals("newValue", map.get("newkey"));
//         assertEquals("newValue", map.get("NEWKEY"));
//     }
}