package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class CompactMap_getLogicalValueType_Test {

    /* --------------------------------------------------------------------- */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        private final int compactSize;

        private final String singleValueKey;

        private final Map<K, V> backingMap;

        TestCompactMap(int compactSize, String singleValueKey, Map<K, V> backingMap) {
            this.compactSize = compactSize;
            this.singleValueKey = singleValueKey;
            this.backingMap = backingMap;
        }

        @Override
        protected int compactSize() {
            return compactSize;
        }

        @Override
        protected String getSingleValueKey() {
            return singleValueKey;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return backingMap;
        }
    }

    /**
     * Utility to replace the private `val` field of a CompactMap instance.
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(map, newVal);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getLogicalValueType_Empty() {
        CompactMap<String, Integer> map = new TestCompactMap<>(5, "id", new HashMap<>());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType(), "A brand‑new CompactMap must report EMPTY");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getLogicalValueType_Object() {
        CompactMap<String, Double> map = new TestCompactMap<>(5, "value", new HashMap<>());
        // key equals the configured single‑value key → stored as raw OBJECT
        map.put("value", 42.0);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType(), "Single entry with the special key should be reported as OBJECT");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getLogicalValueType_Entry() {
        CompactMap<String, String> map = new TestCompactMap<>(5, "id", new HashMap<>());
        // not the special key
        map.put("alpha", "beta");
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType(), "Single entry with a non‑special key should be reported as ENTRY");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getLogicalValueType_Array() {
        // compactSize = 4 → 2 entries keep the compact array representation
        CompactMap<String, Integer> map = new TestCompactMap<>(4, "id", new HashMap<>());
        map.put("one", 1);
        map.put("two", 2);
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType(), "Two entries (size <= compactSize) should be reported as ARRAY");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getLogicalValueType_Map() {
        // compactSize = 2 → inserting 3 entries forces a backing map
        CompactMap<String, Integer> map = new TestCompactMap<>(2, "id", new LinkedHashMap<>());
        map.put("a", 1);
        map.put("b", 2);
        // size now 3 > compactSize
        map.put("c", 3);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType(), "More than compactSize entries should be reported as MAP");
    }

    /* --------------------------- ARRAY branch --------------------------- */
    @Test
    void getLogicalValueType_ValIsObjectArray() throws Exception {
        CompactMap<String, String> map = new TestCompactMap<>(10, "id", new HashMap<>());
        Object[] arr = new Object[] { "k1", "v1", "k2", "v2" };
        setInternalVal(map, arr);
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType(), "When `val` is an Object[] the method must return ARRAY");
    }

    /* ---------------------------- MAP branch ---------------------------- */
    @Test
    void getLogicalValueType_ValIsMap() throws Exception {
        CompactMap<String, String> map = new TestCompactMap<>(10, "id", new HashMap<>());
        Map<String, String> backing = new HashMap<>();
        backing.put("x", "y");
        setInternalVal(map, backing);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType(), "When `val` holds a Map implementation the method must return MAP");
    }

    /* --------------------------- EMPTY branch -------------------------- */
    @Test
    void getLogicalValueType_ValIsEmptySentinel() throws Exception {
        CompactMap<String, String> map = new TestCompactMap<>(10, "id", new HashMap<>());
        // By default a new map already has EMPTY sentinel, but we set it explicitly
        setInternalVal(map, CompactMap.EMPTY_MAP);
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType(), "When `val` equals EMPTY_MAP the method must return EMPTY");
    }

    /* --------------------------- ENTRY branch -------------------------- */
    @Test
    void getLogicalValueType_ValIsCompactMapEntry() throws Exception {
        CompactMap<String, String> map = new TestCompactMap<>(10, "id", new HashMap<>());
        // Create a proper CompactMapEntry instance via the outer instance
        CompactMap<String, String>.CompactMapEntry entry = map.new CompactMapEntry("k", "v");
        setInternalVal(map, entry);
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType(), "When `val` is a CompactMapEntry the method must return ENTRY");
    }

    @Test
    void getLogicalValueType_ValIsAnonymousCompactMapEntrySubclass() throws Exception {
        CompactMap<String, String> map = new TestCompactMap<>(10, "id", new HashMap<>());
        // Anonymous subclass of CompactMapEntry
        CompactMap<String, String>.CompactMapEntry anon = map.new CompactMapEntry("a", "b") {
        };
        setInternalVal(map, anon);
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType(), "Anonymous subclass of CompactMapEntry must still be recognised as ENTRY");
    }

    /* --------------------------- OBJECT branch -------------------------- */
    @Test
    void getLogicalValueType_ValIsPlainObject() throws Exception {
        CompactMap<String, String> map = new TestCompactMap<>(10, "id", new HashMap<>());
        setInternalVal(map, "just a string");
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType(), "Any non‑null, non‑special object should be reported as OBJECT");
    }

    @Test
    void getLogicalValueType_ValIsNull() throws Exception {
        CompactMap<String, String> map = new TestCompactMap<>(10, "id", new HashMap<>());
        setInternalVal(map, null);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType(), "When `val` is null the method falls through to OBJECT");
    }

    @Test
    void getLogicalValueType_ValIsPrimitiveArray() throws Exception {
        CompactMap<String, String> map = new TestCompactMap<>(10, "id", new HashMap<>());
        int[] primitive = new int[] { 1, 2, 3 };
        setInternalVal(map, primitive);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType(), "A primitive array (int[]) is not an Object[] → OBJECT");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void stateTransitions_EmptyToEntryToMapToEmpty() {
        CompactMap<String, Integer> map = new TestCompactMap<>(2, "id", new LinkedHashMap<>());
        // start EMPTY
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType(), "Initially the map must be EMPTY");
        // first put → ENTRY (key not equal to singleValueKey)
        map.put("alpha", 1);
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType(), "After first put the map should be ENTRY");
        // second distinct key → MAP (size exceeds compactSize of 2 after third put)
        map.put("beta", 2);
        // now size = 3 > compactSize
        map.put("gamma", 3);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType(), "After exceeding compactSize the map must be MAP");
        // clear → back to EMPTY
        map.clear();
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType(), "After clear the map must return to EMPTY");
    }
}
