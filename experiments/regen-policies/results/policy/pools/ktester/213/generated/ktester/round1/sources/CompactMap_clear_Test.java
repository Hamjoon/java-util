package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_clear_Test {

    /**
     * Creates a {@link CompactMap} with a small {@code compactSize} and a
     * {@link LinkedHashMap} backing store.  The {@code getSingleValueKey()}
     * method is overridden so that the map can take advantage of the
     * single‑entry optimisation when only one entry is present.
     */
    private CompactMap<String, Object> newTestMap() {
        return new CompactMap<String, Object>() {

            @Override
            protected int compactSize() {
                // small size to force state changes quickly
                return 3;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<String, Object> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
    }

    /**
     * Returns the current value of the private {@code val} field via reflection.
     */
    private static Object getInternalVal(CompactMap<?, ?> map) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        return f.get(map);
    }

    /**
     * Returns the sentinel {@code EMPTY_MAP} constant via reflection.
     */
    private static String getEmptySentinel() throws Exception {
        Field f = CompactMap.class.getDeclaredField("EMPTY_MAP");
        f.setAccessible(true);
        return (String) f.get(null);
    }

    @Test
    void clearOnEmptyMapLeavesItEmpty() {
        CompactMap<String, Object> map = newTestMap();
        // pre‑condition: map is empty
        assertTrue(map.isEmpty(), "Map should start empty");
        assertEquals(0, map.size());
        // act
        map.clear();
        // post‑condition: still empty
        assertTrue(map.isEmpty(), "Map should remain empty after clear()");
        assertEquals(0, map.size());
    }

    @Test
    void clearRemovesSingleEntryAndResetsState() {
        CompactMap<String, Object> map = newTestMap();
        // put a single entry – this triggers the single‑value optimisation
        assertNull(map.put("value", 42), "First put should return null");
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
        // verify the entry is present
        assertEquals(42, map.get("value"));
        // act
        map.clear();
        // map must be empty again
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, map.size());
        assertNull(map.get("value"));
    }

    @Test
    void clearWorksWhenMapHasBackedByStandardMap() {
        CompactMap<String, Object> map = newTestMap();
        // Fill the map beyond the compact size to force a backing map
        for (int i = 0; i < 5; i++) {
            map.put("k" + i, i);
        }
        assertEquals(5, map.size());
        assertFalse(map.isEmpty());
        // act
        map.clear();
        // map must be empty and usable again
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, map.size());
        // ensure we can add new entries after clear()
        assertNull(map.put("newKey", "newValue"));
        assertEquals(1, map.size());
        assertEquals("newValue", map.get("newKey"));
    }

    @Test
    void clearDoesNotThrowWhenIteratingAndRemovingViaIterator() {
        CompactMap<String, Object> map = newTestMap();
        // Populate map
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());
        // Remove all entries via iterator
        var it = map.keySet().iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        // At this point the map is already empty, but call clear() explicitly
        assertDoesNotThrow(map::clear, "clear() should not throw on an already empty map");
        // Verify emptiness
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    void clearOnNewlyCreatedMapLeavesItEmptyAndResetsSentinel() throws Exception {
        CompactMap<String, Object> map = newTestMap();
        // pre‑condition
        assertTrue(map.isEmpty(), "Map should start empty");
        assertEquals(0, map.size());
        // act
        map.clear();
        // post‑condition
        assertTrue(map.isEmpty(), "Map should remain empty after clear()");
        assertEquals(0, map.size());
        // internal sentinel check
        assertEquals(getEmptySentinel(), getInternalVal(map), "Internal 'val' must be reset to EMPTY_MAP");
    }

    @Test
    void clearAfterInsertingEntriesRemovesAllAndResetsSentinel() throws Exception {
        CompactMap<String, Object> map = newTestMap();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("a"));
        assertFalse(map.containsKey("b"));
        assertEquals(getEmptySentinel(), getInternalVal(map));
    }

    @Test
    void clearMultipleTimesIsIdempotent() throws Exception {
        CompactMap<String, Object> map = newTestMap();
        map.put("x", 100);
        map.put("y", 200);
        // first clear
        map.clear();
        // first clear should have left the map empty
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(getEmptySentinel(), getInternalVal(map));
        // second clear – should be a no‑op but also safe
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(getEmptySentinel(), getInternalVal(map));
    }

    @Test
    void clearDoesNotAlterConfiguration() throws Exception {
        // Build a map with non‑default configuration
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().caseSensitive(false).sortedOrder().compactSize(30).mapType(TreeMap.class).singleValueKey("uuid").build();
        // sanity check that configuration is indeed custom
        Map<String, Object> cfgBefore = map.getConfig();
        assertFalse((Boolean) cfgBefore.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, cfgBefore.get(CompactMap.ORDERING));
        assertEquals(TreeMap.class, cfgBefore.get(CompactMap.MAP_TYPE));
        // populate and clear
        map.put("a", 1);
        map.put("b", 2);
        map.clear();
        // configuration must be unchanged
        Map<String, Object> cfgAfter = map.getConfig();
        assertEquals(cfgBefore, cfgAfter, "Configuration should survive clear()");
        assertTrue(map.isEmpty());
        assertEquals(getEmptySentinel(), getInternalVal(map));
    }

    @Test
    void clearAfterIndividualRemovalsLeavesMapEmptyAndSentinelIntact() throws Exception {
        CompactMap<String, Object> map = newTestMap();
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");
        // remove each entry via the public remove method
        map.remove("k1");
        map.remove("k2");
        map.remove("k3");
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        // now call clear() on an already‑empty map
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(getEmptySentinel(), getInternalVal(map));
    }

    @Test
    void clearHandlesNullKeyAndNullValueEntries() throws Exception {
        CompactMap<String, Object> map = newTestMap();
        // CompactMap permits a null key/value (behaviour mirrors HashMap)
        map.put(null, "nullKey");
        map.put("nullValue", null);
        assertEquals(2, map.size());
        assertEquals("nullKey", map.get(null));
        assertTrue(map.containsValue(null));
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get(null));
        assertFalse(map.containsKey(null));
        assertFalse(map.containsValue(null));
        assertEquals(getEmptySentinel(), getInternalVal(map));
    }

    @Test
    void clearAfterPutAllRemovesAllEntries() throws Exception {
        CompactMap<String, Object> map = newTestMap();
        Map<String, Object> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        source.put("three", 3);
        map.putAll(source);
        assertEquals(3, map.size());
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertNull(map.get("one"));
        assertFalse(map.containsKey("two"));
        assertEquals(getEmptySentinel(), getInternalVal(map));
    }

    @Test
    void clearAfterEntrySetIteratorRemovalLeavesMapEmpty() throws Exception {
        CompactMap<String, Object> map = newTestMap();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        // map is empty via iterator removal
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        // explicit clear() should be safe
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(getEmptySentinel(), getInternalVal(map));
    }

    @Test
    void clearOnCustomTreeMapSortedOrderingPreservesConfiguration() throws Exception {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().mapType(TreeMap.class).sortedOrder().build();
        // verify configuration before clear
        Map<String, Object> cfgBefore = map.getConfig();
        assertEquals(TreeMap.class, cfgBefore.get(CompactMap.MAP_TYPE));
        assertEquals(CompactMap.SORTED, cfgBefore.get(CompactMap.ORDERING));
        // add a few entries
        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);
        assertEquals(3, map.size());
        map.clear();
        // after clear the map must be empty and config unchanged
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(getEmptySentinel(), getInternalVal(map));
        assertEquals(cfgBefore, map.getConfig());
    }

    @Test
    void clearAfterExceedingDefaultCompactSizeRestoresSentinel() throws Exception {
        // Use the default compact size (50) – we insert 55 entries
        CompactMap<Integer, Integer> map = new CompactMap<>();
        for (int i = 0; i < 55; i++) {
            map.put(i, i * 10);
        }
        assertEquals(55, map.size());
        // internal representation should now be a backing map (not the sentinel)
        assertNotEquals(getEmptySentinel(), getInternalVal(map));
        map.clear();
        // after clear we must be back to the sentinel state
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(getEmptySentinel(), getInternalVal(map));
    }

    @Test
    void clearPreservesWithConfigChanges() throws Exception {
        CompactMap<String, Object> map = new CompactMap<>();
        // change configuration via withConfig()
        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put(CompactMap.CASE_SENSITIVE, false);
        newConfig.put(CompactMap.COMPACT_SIZE, 20);
        map = map.withConfig(newConfig);
        // sanity check config
        Map<String, Object> cfgBefore = map.getConfig();
        assertFalse((Boolean) cfgBefore.get(CompactMap.CASE_SENSITIVE));
        assertEquals(20, cfgBefore.get(CompactMap.COMPACT_SIZE));
        // add some entries and clear
        map.put("Alpha", 1);
        map.put("Beta", 2);
        map.clear();
        // configuration must survive clear
        Map<String, Object> cfgAfter = map.getConfig();
        assertEquals(cfgBefore, cfgAfter);
        assertTrue(map.isEmpty());
        assertEquals(getEmptySentinel(), getInternalVal(map));
    }
}
