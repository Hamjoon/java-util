package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Map;
import java.util.TreeMap;

@ExtendWith(MockitoExtension.class)
class CompactMap_getLogicalValueType_Test {

    /**
     * Returns a fixed key so that a single entry can be stored as a plain value.
     */
    private static class SingleValueMap extends CompactMap<String, String> {

        @Override
        protected String getSingleValueKey() {
            return "singleKey";
        }
    }

    /**
     * Same as {@link SingleValueMap} but we will store a key that does NOT match the single‑value key.
     */
    private static class SingleEntryMap extends CompactMap<String, String> {

        @Override
        protected String getSingleValueKey() {
            return "singleKey";
        }
    }

    /**
     * Forces the map to switch to a backing {@link java.util.Map} after only two elements.
     */
    private static class SmallCompactMap extends CompactMap<String, String> {

        @Override
        protected int compactSize() {
            // >2 entries will be stored in a Map
            return 2;
        }

        @Override
        protected String getSingleValueKey() {
            return "singleKey";
        }
    }

    /* --------------------------------------------------------------------- */
    private static void setValField(CompactMap<?, ?> mapInstance, Object newVal) throws Exception {
        Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
        valField.set(mapInstance, newVal);
    }

    @Test
    void emptyLogicalValueType() {
        CompactMap<String, String> map = new CompactMap<>();
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.getLogicalValueType(), "A newly constructed CompactMap should be EMPTY");
    }

    @Test
    void objectLogicalValueType() {
        CompactMap<String, String> map = new SingleValueMap();
        // key matches getSingleValueKey()
        map.put("singleKey", "theValue");
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType(), "A single entry whose key matches getSingleValueKey() should be OBJECT");
    }

    @Test
    void entryLogicalValueType() {
        CompactMap<String, String> map = new SingleEntryMap();
        // key does NOT match getSingleValueKey()
        map.put("differentKey", "value");
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.getLogicalValueType(), "A single entry whose key does NOT match getSingleValueKey() should be ENTRY");
    }

    @Test
    void arrayLogicalValueType() {
        CompactMap<String, String> map = new SingleEntryMap();
        // Two distinct entries – size is between 2 and compactSize() (default 80)
        map.put("k1", "v1");
        map.put("k2", "v2");
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType(), "Two entries should be stored in an internal Object[] and be reported as ARRAY");
    }

    @Test
    void mapLogicalValueType() {
        CompactMap<String, String> map = new SmallCompactMap();
        // Insert three entries – exceeds compactSize() (2) → backing Map is used
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType(), "More than compactSize() entries should be stored in a backing Map and be reported as MAP");
    }

    /**
     * When the internal field {@code val} is explicitly set to {@code null}
     * the method should fall through all checks and return OBJECT.
     */
    @Test
    void nullInternalValueReturnsObject() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        setValField(map, null);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType(), "Null internal value should be treated as OBJECT");
    }

    /**
     * Primitive arrays are not instances of {@code Object[]}; they should be
     * reported as OBJECT.
     */
    @Test
    void primitiveArrayInternalValueReturnsObject() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        int[] primitiveArray = new int[] { 1, 2, 3 };
        setValField(map, primitiveArray);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType(), "Primitive array internal value should be reported as OBJECT");
    }

    /**
     * A {@link java.util.Map.Entry} that is NOT the inner {@code CompactMapEntry}
     * must be treated as a regular object.
     */
    @Test
    void nonCompactMapEntryInstanceReturnsObject() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        Map.Entry<String, String> simpleEntry = new AbstractMap.SimpleEntry<>("k", "v");
        setValField(map, simpleEntry);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.getLogicalValueType(), "Non‑CompactMapEntry Map.Entry should be reported as OBJECT");
    }

    /**
     * An array created via reflection (still an {@code Object[]}) must be
     * recognised as ARRAY.
     */
    @Test
    void reflectiveObjectArrayReturnsArray() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        // Create an Object[2] via reflection
        Object reflectiveArray = Array.newInstance(Object.class, 2);
        setValField(map, reflectiveArray);
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.getLogicalValueType(), "Reflectively created Object[] should be reported as ARRAY");
    }

    /**
     * An empty {@link java.util.Map} that is NOT the sentinel constant should
     * be reported as MAP (the sentinel {@code EMPTY_MAP} is the only EMPTY case).
     */
    @Test
    void emptyButNonSentinelMapReturnsMap() throws Exception {
        CompactMap<String, String> map = new CompactMap<>();
        Map<String, String> emptyTreeMap = new TreeMap<>();
        setValField(map, emptyTreeMap);
        assertEquals(CompactMap.LogicalValueType.MAP, map.getLogicalValueType(), "Empty non‑sentinel Map should be reported as MAP");
    }
}
