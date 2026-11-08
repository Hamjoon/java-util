package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

class CompactMap_getLogicalValueType_Test {

    /**
     * Default behavior – uses the library's default compactSize (80).
     */
    private static class DefaultCompactMap extends CompactMap<String, Integer> {

        LogicalValueType logicalValueType() {
            return super.getLogicalValueType();
        }
    }

    /**
     * Overrides compactSize() to a very small value so that a Map is used.
     */
    private static class MapTriggerCompactMap extends CompactMap<String, Integer> {

        @Override
        protected int compactSize() {
            // any size >2 will force MAP representation
            return 2;
        }

        LogicalValueType logicalValueType() {
            return super.getLogicalValueType();
        }
    }

    /**
     * Overrides getSingleValueKey() so that a matching key stores the value
     * directly (OBJECT) and a non‑matching key stores a CompactMapEntry (ENTRY).
     */
    private static class SingleKeyCompactMap extends CompactMap<String, Integer> {

        @Override
        protected String getSingleValueKey() {
            return "singleKey";
        }

        LogicalValueType logicalValueType() {
            return super.getLogicalValueType();
        }
    }

    /**
     * Simple subclass that only exposes the protected method.
     */
    private static class ExposedCompactMap extends CompactMap<String, Integer> {

        LogicalValueType logicalValueType() {
            return super.getLogicalValueType();
        }
    }

    /**
     * Sets the private {@code val} field of a CompactMap instance.
     *
     * @param map   the map whose internal state will be altered
     * @param value the value to assign to {@code val}
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object value) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to set internal val field", e);
        }
    }

    /**
     * Retrieves the sentinel constant {@code EMPTY_MAP} from CompactMap.
     *
     * @return the sentinel object used for an empty CompactMap
     */
    private static Object getEmptyMapSentinel() {
        try {
            Field f = CompactMap.class.getDeclaredField("EMPTY_MAP");
            f.setAccessible(true);
            return f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to read EMPTY_MAP sentinel", e);
        }
    }

    @Test
    void getLogicalValueType_returnsEMPTY_forNewMap() {
        DefaultCompactMap map = new DefaultCompactMap();
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.logicalValueType(), "A freshly constructed CompactMap should be EMPTY");
    }

    @Test
    void getLogicalValueType_returnsOBJECT_whenSingleKeyMatches() {
        SingleKeyCompactMap map = new SingleKeyCompactMap();
        // matches getSingleValueKey()
        map.put("singleKey", 42);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.logicalValueType(), "When the sole entry's key matches getSingleValueKey(), the logical type should be OBJECT");
    }

    @Test
    void getLogicalValueType_returnsENTRY_whenSingleKeyDoesNotMatch() {
        SingleKeyCompactMap map = new SingleKeyCompactMap();
        // does NOT match getSingleValueKey()
        map.put("otherKey", 99);
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.logicalValueType(), "When the sole entry's key does not match getSingleValueKey(), the logical type should be ENTRY");
    }

    @Test
    void getLogicalValueType_returnsARRAY_whenTwoElements() {
        DefaultCompactMap map = new DefaultCompactMap();
        map.put("a", 1);
        // size == 2, still below default compactSize (80)
        map.put("b", 2);
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType(), "Two entries should be stored in an internal Object[] resulting in ARRAY type");
    }

    @Test
    void getLogicalValueType_returnsMAP_whenSizeExceedsCompactSize() {
        MapTriggerCompactMap map = new MapTriggerCompactMap();
        map.put("k1", 10);
        map.put("k2", 20);
        // size == 3 > compactSize (2) → internal Map
        map.put("k3", 30);
        assertEquals(CompactMap.LogicalValueType.MAP, map.logicalValueType(), "When size exceeds compactSize, the internal representation should be a Map");
    }

    @Test
    void getLogicalValueType_returnsARRAY_whenValIsObjectArray() {
        ExposedCompactMap map = new ExposedCompactMap();
        // any Object[] should be recognised as ARRAY
        setInternalVal(map, new Object[] { "key", 1, "another", 2 });
        assertEquals(CompactMap.LogicalValueType.ARRAY, map.logicalValueType(), "Object[] stored in val must yield LogicalValueType.ARRAY");
    }

    @Test
    void getLogicalValueType_returnsMAP_whenValIsMapInstance() {
        ExposedCompactMap map = new ExposedCompactMap();
        Map<String, Integer> backing = new HashMap<>();
        backing.put("a", 10);
        setInternalVal(map, backing);
        assertEquals(CompactMap.LogicalValueType.MAP, map.logicalValueType(), "A Map instance stored in val must yield LogicalValueType.MAP");
    }

    @Test
    void getLogicalValueType_returnsEMPTY_whenValIsSentinel() {
        ExposedCompactMap map = new ExposedCompactMap();
        setInternalVal(map, getEmptyMapSentinel());
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.logicalValueType(), "The EMPTY_MAP sentinel must be recognised as LogicalValueType.EMPTY");
    }

    @Test
    void getLogicalValueType_returnsENTRY_whenValIsCompactMapEntry() {
        ExposedCompactMap map = new ExposedCompactMap();
        // Create a proper CompactMapEntry instance via the outer class
        CompactMap<String, Integer>.CompactMapEntry entry = map.new CompactMapEntry("k", 5);
        setInternalVal(map, entry);
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.logicalValueType(), "A CompactMapEntry stored in val must yield LogicalValueType.ENTRY");
    }

    @Test
    void getLogicalValueType_returnsENTRY_forAnonymousSubclassOfCompactMapEntry() {
        ExposedCompactMap map = new ExposedCompactMap();
        // Anonymous subclass – still should be recognised via isInstance()
        CompactMap<String, Integer>.CompactMapEntry anon = map.new CompactMapEntry("k", 5) {
        };
        setInternalVal(map, anon);
        assertEquals(CompactMap.LogicalValueType.ENTRY, map.logicalValueType(), "An anonymous subclass of CompactMapEntry must still be recognised as ENTRY");
    }

    @Test
    void getLogicalValueType_returnsOBJECT_forArbitraryObject() {
        ExposedCompactMap map = new ExposedCompactMap();
        setInternalVal(map, "just a plain String");
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.logicalValueType(), "Any non‑array, non‑Map, non‑sentinel, non‑entry object should be OBJECT");
    }

    @Test
    void getLogicalValueType_returnsOBJECT_whenValIsNull() {
        ExposedCompactMap map = new ExposedCompactMap();
        setInternalVal(map, null);
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.logicalValueType(), "A null val is treated as a regular object and must yield OBJECT");
    }

    @Test
    void getLogicalValueType_returnsOBJECT_forPrimitiveArray() {
        ExposedCompactMap map = new ExposedCompactMap();
        // int[] is NOT an instance of Object[]
        setInternalVal(map, new int[] { 1, 2, 3 });
        assertEquals(CompactMap.LogicalValueType.OBJECT, map.logicalValueType(), "Primitive arrays are not Object[]; they must be reported as OBJECT");
    }

    @Test
    void getLogicalValueType_returnsEMPTY_afterClear() {
        // Use the public API to verify that clear() resets the internal representation
        ExposedCompactMap map = new ExposedCompactMap();
        map.put("x", 1);
        map.put("y", 2);
        assertNotEquals(CompactMap.LogicalValueType.EMPTY, map.logicalValueType(), "Pre‑condition: map should not be EMPTY before clear");
        map.clear();
        assertEquals(CompactMap.LogicalValueType.EMPTY, map.logicalValueType(), "After clear() the internal val must be reset to EMPTY_MAP");
    }
}
