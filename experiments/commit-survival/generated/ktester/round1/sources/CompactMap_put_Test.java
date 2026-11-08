package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.*;
import java.util.*;

class CompactMap_put_Test {

    private CompactMap<Integer, String> map;

    /**
     * CompactMap that switches to a {@link HashMap} after only two entries.
     */
    private static class SmallCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // transition after 2 entries
            return 2;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }

        @Override
        protected Map<K, V> getNewMap(int size) {
            return new HashMap<>(size);
        }
    }

    /**
     * CompactMap with a larger {@code compactSize()} so that a third entry
     *  still lives in the internal {@code Object[]} representation.
     */
    private static class LargeCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // stay in array for up to 3 entries
            return 3;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }

        @Override
        protected Map<K, V> getNewMap(int size) {
            return new HashMap<>(size);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use LargeCompactMap for the majority of tests; individual tests may replace it.
        map = new LargeCompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        map = null;
    }

    /**
     * Sets the private {@code val} field of a {@link CompactMap}.
     */
    private static void setInternalVal(CompactMap<?, ?> target, Object newVal) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(target, newVal);
    }

    /**
     * Retrieves the private {@code val} field (used for assertions).
     */
    private static Object getInternalVal(CompactMap<?, ?> target) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        return f.get(target);
    }

    /**
     * Creates an instance of the package‑private {@code CompactMapEntry}.
     */
    private static Object newCompactMapEntry(Object key, Object value) throws Exception {
        Class<?> entryCls = Class.forName("com.cedarsoftware.util.CompactMapEntry");
        Constructor<?> ctor = entryCls.getDeclaredConstructor(Object.class, Object.class);
        ctor.setAccessible(true);
        return ctor.newInstance(key, value);
    }

    /**
     * Verify that the first insertion works when the map is empty.
     */
    @Test
    void putFirstEntry() {
        String previous = map.put(1, "one");
        assertNull(previous, "First put should return null");
        assertEquals(1, map.size(), "Size should be 1 after first insertion");
        assertEquals("one", map.get(1), "Stored value should be retrievable");
    }

    /**
     * Verify that a second distinct key creates the internal Object[] representation.
     */
    @Test
    void putSecondEntryCreatesArray() {
        map.put(1, "one");
        String previous = map.put(2, "two");
        assertNull(previous, "Second distinct key should return null");
        assertEquals(2, map.size(), "Size should be 2 after two insertions");
        assertEquals("one", map.get(1));
        assertEquals("two", map.get(2));
    }

    /**
     * Verify that adding a third entry forces a transition to a backing Map.
     */
    @Test
    void putThirdEntrySwitchesToBackingMap() {
        // Fill to the compact size (2)
        map.put(1, "one");
        map.put(2, "two");
        // Third insertion triggers the switch
        String previous = map.put(3, "three");
        assertNull(previous, "Third insertion should return null");
        assertEquals(3, map.size(), "Size should be 3 after transition to backing map");
        assertEquals("one", map.get(1));
        assertEquals("two", map.get(2));
        assertEquals("three", map.get(3));
        // Ensure the backing map behaves like a normal map
        assertTrue(map.containsKey(2));
        assertTrue(map.containsValue("two"));
    }

    /**
     * Verify that putting a key that already exists overwrites the value and returns the old one.
     */
    @Test
    void putOverwriteReturnsOldValue() {
        map.put(1, "one");
        map.put(2, "two");
        // Overwrite key 1
        String old = map.put(1, "uno");
        assertEquals("one", old, "Overwrite should return the previous value");
        assertEquals(2, map.size(), "Size must stay unchanged after overwrite");
        assertEquals("uno", map.get(1), "New value should be stored");
    }

    /**
     * Verify that putting a null key/value works (the default backing map is a HashMap which permits nulls).
     */
    @Test
    void putNullKeyAndValue() {
        map.put(null, null);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(null));
        assertTrue(map.containsValue(null));
        assertNull(map.get(null));
    }

    /**
     * ObjectArrayOverwrite – overwrite an existing entry in the Object[] representation.
     */
    @Test
    void objectArrayOverwrite() throws Exception {
        // Force internal state to Object[] with two entries: ["alpha","oldValue","beta","val2"]
        Object[] arr = new Object[] { "alpha", "oldValue", "beta", "val2" };
        setInternalVal(map, arr);
        Object previous = map.put("alpha", "newValue");
        assertEquals("oldValue", previous, "Should return the overwritten value");
        assertEquals("newValue", map.get("alpha"));
        assertEquals("val2", map.get("beta"));
        assertEquals(2, map.size());
    }

    /**
     * ObjectArrayGrow – add a new key when size < compactSize, causing the array to grow.
     */
    @Test
    void objectArrayGrow() throws Exception {
        // Use LargeCompactMap (compactSize = 3) so size (2) < compactSize.
        map = new LargeCompactMap<>();
        Object[] arr = new Object[] { "alpha", "oldValue", "beta", "val2" };
        setInternalVal(map, arr);
        Object previous = map.put("gamma", "val3");
        assertNull(previous, "Adding a new key should return null");
        assertEquals("val3", map.get("gamma"));
        assertEquals(3, map.size());
        // Verify internal array length is now 6 (three pairs)
        Object internal = getInternalVal(map);
        assertTrue(internal instanceof Object[]);
        assertEquals(6, ((Object[]) internal).length);
    }

    /**
     * ObjectArraySwitchToMap – when size == compactSize, a new entry triggers conversion to a Map.
     */
    @Test
    void objectArraySwitchToMap() throws Exception {
        // compactSize = 2, size already 2 → switch on third insertion.
        map = new SmallCompactMap<>();
        Object[] arr = new Object[] { "alpha", "oldValue", "beta", "val2" };
        setInternalVal(map, arr);
        Object previous = map.put("delta", "val4");
        assertNull(previous, "Switching to Map should return null");
        assertEquals("val4", map.get("delta"));
        assertEquals(3, map.size());
        // Internal representation must now be a Map
        Object internal = getInternalVal(map);
        assertTrue(internal instanceof Map);
    }

    /**
     * MapDelegate – put delegates to an existing backing Map and returns the previous value.
     */
    @Test
    void mapDelegateOverwrite() throws Exception {
        // Prepare a backing HashMap with a single entry.
        map = new SmallCompactMap<>();
        Map<String, String> backing = new HashMap<>();
        backing.put("epsilon", "oldMapValue");
        setInternalVal(map, backing);
        Object previous = map.put("epsilon", "newValue");
        assertEquals("oldMapValue", previous, "Should return the old map value");
        assertEquals("newValue", map.get("epsilon"));
        assertEquals(1, map.size());
    }

    /**
     * EmptyMapSetValue – empty map, key matches logical single key (null) and value is a raw value.
     */
    @Test
    void emptyMapSetValue() throws Exception {
        // map is freshly created → EMPTY_MAP
        Object previous = map.put(null, "singleValue");
        assertNull(previous);
        assertEquals(1, map.size());
        assertEquals("singleValue", map.get(null));
        // Internal val should be the raw value, not a CompactMapEntry
        Object internal = getInternalVal(map);
        assertEquals("singleValue", internal);
    }

    /**
     * EmptyMapCreateEntry – empty map, key does not match logical single key, creates CompactMapEntry.
     */
    @Test
    void emptyMapCreateEntry() throws Exception {
        Object previous = map.put("zeta", "val6");
        assertNull(previous);
        assertEquals(1, map.size());
        assertEquals("val6", map.get("zeta"));
        Object internal = getInternalVal(map);
        assertTrue(internal.getClass().getSimpleName().contains("CompactMapEntry"));
    }

    /**
     * SingleEntryOverwriteReplaceValue – overwrite when single entry stores raw value (key matches).
     */
    @Test
    void singleEntryOverwriteReplaceValue() throws Exception {
        // Initialise with a raw value (key matches logical single key)
        // first insertion creates CompactMapEntry because key != logical single key (null)
        map.put("theta", "oldSingle");
        // Force the internal representation to be the raw value by setting it directly.
        setInternalVal(map, "oldSingle");
        Object previous = map.put("theta", "newSingle");
        assertEquals("oldSingle", previous);
        assertEquals("newSingle", map.get("theta"));
        assertEquals(1, map.size());
        // Internal representation should now be the raw new value.
        assertEquals("newSingle", getInternalVal(map));
    }

    /**
     * SingleEntryOverwriteCreateEntry – overwrite with a Map, causing a CompactMapEntry to wrap the Map.
     */
    @Test
    void singleEntryOverwriteCreateEntry() throws Exception {
        // Start with a raw value for key "theta".
        map.put("theta", "oldSingle");
        // ensure raw value
        setInternalVal(map, "oldSingle");
        Map<String, String> newMap = new HashMap<>();
        newMap.put("inner", "value");
        Object previous = map.put("theta", newMap);
        assertEquals("oldSingle", previous);
        assertEquals(newMap, map.get("theta"));
        assertEquals(1, map.size());
        Object internal = getInternalVal(map);
        assertTrue(internal.getClass().getSimpleName().contains("CompactMapEntry"));
    }

    /**
     * SingleEntryAddNewKey – adding a different key converts storage to an Object[].
     */
    @Test
    void singleEntryAddNewKey() throws Exception {
        // creates CompactMapEntry internally
        map.put("theta", "oldSingle");
        Object previous = map.put("iota", "val7");
        assertNull(previous);
        assertEquals(2, map.size());
        assertEquals("oldSingle", map.get("theta"));
        assertEquals("val7", map.get("iota"));
        Object internal = getInternalVal(map);
        assertTrue(internal instanceof Object[]);
        assertArrayEquals(new Object[] { "theta", "oldSingle", "iota", "val7" }, (Object[]) internal);
    }

    /**
     * ObjectArrayOverwriteWithNullValue – overwrite existing entry with null value.
     */
    @Test
    void objectArrayOverwriteWithNullValue() throws Exception {
        Object[] arr = new Object[] { "alpha", "oldValue", "beta", "val2" };
        setInternalVal(map, arr);
        Object previous = map.put("alpha", null);
        assertEquals("oldValue", previous);
        assertNull(map.get("alpha"));
        assertEquals("val2", map.get("beta"));
    }

    /**
     * ObjectArrayAddNewKeyWithNullValue – add a new key with null value, array grows.
     */
    @Test
    void objectArrayAddNewKeyWithNullValue() throws Exception {
        // Use LargeCompactMap so size < compactSize.
        map = new LargeCompactMap<>();
        Object[] arr = new Object[] { "alpha", "oldValue", "beta", "val2" };
        setInternalVal(map, arr);
        Object previous = map.put("gamma", null);
        assertNull(previous);
        assertNull(map.get("gamma"));
        assertEquals(3, map.size());
        Object internal = getInternalVal(map);
        assertTrue(internal instanceof Object[]);
        // Verify that the null value is stored at the correct position.
        Object[] internalArr = (Object[]) internal;
        // index 5 = value for "gamma"
        assertEquals(null, internalArr[5]);
    }

    /**
     * MapDelegateAddNewKey – adding a brand‑new key to an existing backing Map.
     */
    @Test
    void mapDelegateAddNewKey() throws Exception {
        map = new SmallCompactMap<>();
        Map<String, String> backing = new HashMap<>();
        backing.put("epsilon", "oldMapValue");
        setInternalVal(map, backing);
        Object previous = map.put("newKey", "newVal");
        assertNull(previous);
        assertEquals("newVal", map.get("newKey"));
        assertEquals(2, map.size());
    }

    /**
     * MapDelegateOverwriteWithNullValue – overwrite an existing map entry with null.
     */
    @Test
    void mapDelegateOverwriteWithNullValue() throws Exception {
        map = new SmallCompactMap<>();
        Map<String, String> backing = new HashMap<>();
        backing.put("epsilon", "oldMapValue");
        setInternalVal(map, backing);
        Object previous = map.put("epsilon", null);
        assertEquals("oldMapValue", previous);
        assertNull(map.get("epsilon"));
        assertEquals(1, map.size());
    }

    /**
     * EmptyMapSetValueArray – empty map, key matches logical single key but value is an Object[].
     */
    @Test
    void emptyMapSetValueArray() throws Exception {
        Object[] arrayValue = new Object[] { "a", "b" };
        Object previous = map.put(null, arrayValue);
        assertNull(previous);
        assertEquals(arrayValue, map.get(null));
        assertEquals(1, map.size());
        // Internal representation should be a CompactMapEntry (because value is an Object[])
        Object internal = getInternalVal(map);
        assertTrue(internal.getClass().getSimpleName().contains("CompactMapEntry"));
    }

    /**
     * EmptyMapSetRawNullValue – empty map, key matches logical single key and value is raw null.
     */
    @Test
    void emptyMapSetRawNullValue() throws Exception {
        Object previous = map.put(null, null);
        assertNull(previous);
        assertNull(map.get(null));
        assertEquals(1, map.size());
        // Internal val should be the raw null (i.e., still EMPTY_MAP sentinel is replaced by null)
        Object internal = getInternalVal(map);
        assertNull(internal);
    }

    /**
     * SingleEntryOverwriteWithNullValue – overwrite a single entry with null.
     */
    @Test
    void singleEntryOverwriteWithNullValue() throws Exception {
        map.put("theta", "oldSingle");
        // Ensure internal representation is a CompactMapEntry
        Object internalBefore = getInternalVal(map);
        assertTrue(internalBefore.getClass().getSimpleName().contains("CompactMapEntry"));
        Object previous = map.put("theta", null);
        assertEquals("oldSingle", previous);
        assertNull(map.get("theta"));
        assertEquals(1, map.size());
        // After overwrite the internal value should be raw null.
        assertNull(getInternalVal(map));
    }

    /**
     * SingleEntryAddNewKeyWithArrayValue – add a different key with an Object[] value.
     */
    @Test
    void singleEntryAddNewKeyWithArrayValue() throws Exception {
        map.put("theta", "oldSingle");
        Object[] newArray = new Object[] { "x", "y" };
        Object previous = map.put("iota", newArray);
        assertNull(previous);
        assertEquals(2, map.size());
        assertEquals("oldSingle", map.get("theta"));
        assertArrayEquals(newArray, (Object[]) map.get("iota"));
        Object internal = getInternalVal(map);
        assertTrue(internal instanceof Object[]);
    }

    /**
     * SingleEntryOverwriteWithArrayValue – overwrite existing entry with an Object[] value.
     */
    @Test
    void singleEntryOverwriteWithArrayValue() throws Exception {
        map.put("theta", "oldSingle");
        Object[] newArray = new Object[] { "x", "y" };
        Object previous = map.put("theta", newArray);
        assertEquals("oldSingle", previous);
        assertArrayEquals(newArray, (Object[]) map.get("theta"));
        assertEquals(1, map.size());
        // Internal representation should now be a CompactMapEntry wrapping the array.
        Object internal = getInternalVal(map);
        assertTrue(internal.getClass().getSimpleName().contains("CompactMapEntry"));
    }

    /**
     * ObjectArraySwitchToMapThenPutNewKey – after a previous switch, adding another key delegates to the Map.
     */
    @Test
    void objectArraySwitchToMapThenPutNewKey() throws Exception {
        // Start with SmallCompactMap so that after three insertions we are in Map mode.
        map = new SmallCompactMap<>();
        map.put("alpha", "oldValue");
        // now size == compactSize (2)
        map.put("beta", "val2");
        // triggers switch to Map
        map.put("gamma", "val3");
        // Verify we are indeed using a Map internally.
        assertTrue(getInternalVal(map) instanceof Map);
        // Now add a new key; should delegate to the map.
        Object previous = map.put("epsilon", "newAfterMap");
        assertNull(previous);
        assertEquals("newAfterMap", map.get("epsilon"));
        assertEquals(4, map.size());
    }
}
