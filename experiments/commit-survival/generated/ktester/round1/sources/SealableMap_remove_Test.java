package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class SealableMap_remove_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh SealableMap for each test, initially unsealed
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset sealed flag for the next test.
        sealedState = false;
        map = null;
    }

    @Test
    void testRemoveWhenUnsealed_existingKey() {
        // verify key exists before removal
        assertEquals(1, map.get("one"));
        // remove should return the previous value
        Integer removed = map.remove("one");
        assertEquals(1, removed);
        // key should no longer be present
        assertNull(map.get("one"));
    }

    @Test
    void testRemoveWhenUnsealed_nonExistingKey() {
        // Removing a key that is not present should return null
        assertNull(map.remove("absent"));
        // Ensure map size unchanged
        // includes the null key/value entry
        assertEquals(4, map.size());
    }

    @Test
    void testRemoveWhenUnsealed_nullKey() {
        // map already contains a null key with null value
        assertNull(map.get(null));
        // put a non‑null value for the null key
        map.put(null, 99);
        assertEquals(99, map.get(null));
        // remove the null key
        Integer removed = map.remove(null);
        assertEquals(99, removed);
        // after removal the null key should be gone
        assertNull(map.get(null));
    }

    @Test
    void testRemoveWhenSealed() {
        // switch map to sealed (read‑only) mode
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> map.remove("one"));
    }

    @Test
    void testRemoveWhenSealed_nullKey() {
        sealedState = true;
        assertThrows(UnsupportedOperationException.class, () -> map.remove(null));
    }

    @Test
    void testRemove_normalOperation_existingKey() {
        // Setup a specific entry for this test
        map.put("alpha", "A");
        assertEquals("A", map.get("alpha"));
        String removed = map.remove("alpha");
        assertEquals("A", removed);
        assertNull(map.get("alpha"));
    }

    @Test
    void testRemove_normalOperation_missingKey() {
        // Map does not contain "missing"
        assertNull(map.remove("missing"));
        // Ensure map size unchanged (original 4 entries)
        assertEquals(4, map.size());
    }

    @Test
    void testRemove_normalOperation_nullKeyWithValue() {
        map.put(null, "nullValue");
        assertEquals("nullValue", map.get(null));
        String removed = map.remove(null);
        assertEquals("nullValue", removed);
        assertNull(map.get(null));
    }

    @Test
    void testRemove_sealedMap_existingKey_throwsIllegalStateException() {
        map.put("alpha", "A");
        sealedState = true;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.remove("alpha"));
        assertNotNull(ex.getMessage());
        // map must stay unchanged
        assertEquals("A", map.get("alpha"));
    }

    @Test
    void testRemove_sealedMap_missingKey_throwsIllegalStateException() {
        sealedState = true;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.remove("missing"));
        assertNotNull(ex.getMessage());
        // size must stay the same
        assertEquals(4, map.size());
    }

    @Test
    void testRemove_sealedMap_nullKey_throwsIllegalStateException() {
        sealedState = true;
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.remove(null));
        assertNotNull(ex.getMessage());
    }

    @Test
    void testRemove_onEmptyMap_anyKey_returnsNull() throws Exception {
        // Replace internal map with an empty HashMap via reflection
        Field f = SealableMap.class.getDeclaredField("map");
        f.setAccessible(true);
        f.set(map, new HashMap<>());
        assertNull(map.remove("anyKey"));
        assertTrue(map.isEmpty());
    }

    @Test
    void testRemove_onEmptyMap_nullKey_returnsNull() throws Exception {
        Field f = SealableMap.class.getDeclaredField("map");
        f.setAccessible(true);
        f.set(map, new HashMap<>());
        assertNull(map.remove(null));
        assertTrue(map.isEmpty());
    }

    @Test
    void testRemove_keyWithNullValue() {
        map.put("beta", null);
        assertTrue(map.containsKey("beta"));
        assertNull(map.get("beta"));
        // remove should return null (the stored value) and delete the entry
        assertNull(map.remove("beta"));
        assertFalse(map.containsKey("beta"));
    }

    @Test
    void testRemove_unmodifiableUnderlyingMap_throwsUnsupportedOperationException() throws Exception {
        // Build an unmodifiable map containing a single entry
        Map<String, String> backing = Collections.unmodifiableMap(new HashMap<>(Map.of("x", "X")));
        // Inject it via reflection
        Field f = SealableMap.class.getDeclaredField("map");
        f.setAccessible(true);
        f.set(map, backing);
        // Regardless of seal state, remove must propagate the UOE from the delegate
        assertThrows(UnsupportedOperationException.class, () -> map.remove("x"));
    }

    @Test
    void testRemove_incompatibleKeyType_throwsClassCastException() throws Exception {
        // TreeMap<Integer, String> expects Integer keys
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "one");
        // Inject the TreeMap (raw type to bypass generics)
        Field f = SealableMap.class.getDeclaredField("map");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map raw = treeMap;
        f.set(map, raw);
        // Attempt to remove with a String key -> ClassCastException from TreeMap
        assertThrows(ClassCastException.class, () -> map.remove("stringKey"));
    }

    @Test
    void testRemove_nullUnderlyingMap_throwsNullPointerException() throws Exception {
        // Force the internal map reference to null
        Field f = SealableMap.class.getDeclaredField("map");
        f.setAccessible(true);
        f.set(map, null);
        assertThrows(NullPointerException.class, () -> map.remove("anyKey"));
    }
}
