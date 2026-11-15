package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

// Mockito not used but kept from template
@ExtendWith(MockitoExtension.class)
class TrackingMap_get_Test {

    private TrackingMap<String, String> trackingMap;

    private Map<String, String> backingMap;

    /**
     * Simple custom key class used for equality‑based tests.
     */
    static final class CustomKey {

        private final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomKey{" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC (not strictly required)
        trackingMap = null;
        backingMap = null;
    }

    @Test
    @DisplayName("get() returns the stored value and records the accessed key")
    void testGetExistingKey() {
        backingMap.put("alpha", "valueAlpha");
        backingMap.put("beta", "valueBeta");
        String value = trackingMap.get("alpha");
        assertEquals("valueAlpha", value, "get should return the correct value");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("alpha"), "keysUsed should contain the accessed key");
        assertEquals(1, used.size(), "Only the accessed key should be recorded");
    }

    @Test
    @DisplayName("get() on a missing key returns null and still records the key")
    void testGetNonExistingKey() {
        backingMap.put("x", "X");
        // "beta" not present
        String value = trackingMap.get("beta");
        assertNull(value, "get should return null for a missing key");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("beta"), "Missing key should still be recorded as used");
        assertEquals(1, used.size(), "Only the missing key should be recorded");
        assertEquals(1, trackingMap.size(), "Map size should remain unchanged");
    }

    @Test
    @DisplayName("get() with a null key works with HashMap backing and records null")
    void testGetNullKey() {
        backingMap.put(null, "nullKeyValue");
        backingMap.put("present", "value");
        String value = trackingMap.get(null);
        assertEquals("nullKeyValue", value, "get should return the value associated with null key");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains(null), "keysUsed should contain null after accessing null key");
        assertEquals(1, used.size(), "Only the null key should be recorded");
    }

    @Test
    @DisplayName("get() of a key whose value is null still records the key")
    void testGetKeyWithNullValue() {
        // key with explicit null value
        backingMap.put("delta", null);
        String value = trackingMap.get("delta");
        assertNull(value, "Value is null as stored");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("delta"), "Key with null value should be recorded");
        assertEquals(1, used.size(), "Only the accessed key should be recorded");
    }

    @Test
    @DisplayName("Repeated get() calls for the same key record it only once")
    void testGetMultipleCallsSameKey() {
        backingMap.put("dup", "first");
        trackingMap.get("dup");
        trackingMap.get("dup");
        trackingMap.get("dup");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.contains("dup"), "Key should be recorded");
        assertEquals(1, used.size(), "Key should appear only once in keysUsed");
    }

    @Test
    @DisplayName("get() does not modify the underlying map size when key is absent")
    void testGetDoesNotAffectSizeWhenKeyAbsent() {
        backingMap.put("a", "1");
        backingMap.put("b", "2");
        int originalSize = trackingMap.size();
        trackingMap.get("nonexistent");
        assertEquals(originalSize, trackingMap.size(), "Size should stay the same after getting a non‑existent key");
        assertTrue(trackingMap.keysUsed().contains("nonexistent"), "Non‑existent key should still be recorded as used");
    }

    @Test
    @DisplayName("get() with a key of wrong type throws ClassCastException and does not modify readKeys")
    void testGetWithWrongKeyType() {
        backingMap.put("alpha", "valueAlpha");
        // Attempt to retrieve using an Integer key – the internal cast ((K) key) should fail.
        Integer wrongKey = 123;
        ClassCastException thrown = assertThrows(ClassCastException.class, () -> trackingMap.get(wrongKey), "Expected ClassCastException for wrong key type");
        assertNotNull(thrown);
        // readKeys must stay empty because the exception occurs before add()
        assertTrue(trackingMap.keysUsed().isEmpty(), "readKeys set must remain unchanged after ClassCastException");
    }

    @Test
    @DisplayName("get() when internalMap is null throws NullPointerException")
    void testGetWhenInternalMapIsNull() throws Exception {
        // Use reflection to set the private field internalMap to null
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(trackingMap, null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> trackingMap.get("anyKey"), "Expected NullPointerException when internalMap is null");
        assertNotNull(npe);
    }

    @Test
    @DisplayName("get() with CharSequence key (StringBuilder) works and records the exact instance")
    void testGetWithCharSequenceSubclassKey() {
        // TrackingMap<CharSequence, String>
        Map<CharSequence, String> csMap = new HashMap<>();
        csMap.put("epsilon", "valueEpsilon");
        TrackingMap<CharSequence, String> csTrackingMap = new TrackingMap<>(csMap);
        StringBuilder sbKey = new StringBuilder("epsilon");
        String value = csTrackingMap.get(sbKey);
        assertEquals("valueEpsilon", value, "Should retrieve value for CharSequence key");
        Set<CharSequence> used = csTrackingMap.keysUsed();
        assertTrue(used.contains(sbKey), "The exact StringBuilder instance must be recorded");
        assertEquals(1, used.size(), "Only one key should be recorded");
    }

    @Test
    @DisplayName("get() with custom key objects respects equals() and records the supplied instance")
    void testGetWithCustomKey() {
        Map<CustomKey, String> customMap = new HashMap<>();
        CustomKey storedKey = new CustomKey(42);
        customMap.put(storedKey, "customValue");
        TrackingMap<CustomKey, String> customTrackingMap = new TrackingMap<>(customMap);
        // different instance but equal
        CustomKey lookupKey = new CustomKey(42);
        String value = customTrackingMap.get(lookupKey);
        assertEquals("customValue", value, "Should retrieve value for equal custom key");
        Set<CustomKey> used = customTrackingMap.keysUsed();
        assertTrue(used.contains(lookupKey), "The lookup instance must be recorded");
        assertEquals(1, used.size(), "Only one key should be recorded");
    }

    @Test
    @DisplayName("keysUsed accumulates multiple distinct keys")
    void testKeysUsedAccumulatesMultipleKeys() {
        backingMap.put("alpha", "valueAlpha");
        backingMap.put("zeta", "valueZeta");
        trackingMap.get("alpha");
        trackingMap.get("zeta");
        Set<String> used = trackingMap.keysUsed();
        assertTrue(used.containsAll(List.of("alpha", "zeta")), "Both accessed keys must be present");
        assertEquals(2, used.size(), "Exactly two distinct keys should be recorded");
    }

    @Test
    @DisplayName("get() throws NullPointerException when readKeys is null")
    void testGetWhenReadKeysIsNull() throws Exception {
        // Set readKeys to null via reflection
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        readKeysField.set(trackingMap, null);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> trackingMap.get("anyKey"), "Expected NullPointerException because readKeys is null");
        assertNotNull(npe);
    }

    @Test
    @DisplayName("get() throws UnsupportedOperationException when readKeys is unmodifiable")
    void testGetWhenReadKeysIsUnmodifiable() throws Exception {
        // Wrap the current readKeys set with Collections.unmodifiableSet()
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<String> original = (Set<String>) readKeysField.get(trackingMap);
        Set<String> unmodifiable = Collections.unmodifiableSet(original);
        readKeysField.set(trackingMap, unmodifiable);
        UnsupportedOperationException uoe = assertThrows(UnsupportedOperationException.class, () -> trackingMap.get("anyKey"), "Expected UnsupportedOperationException due to unmodifiable readKeys");
        assertNotNull(uoe);
    }

    @Test
    @DisplayName("TreeMap internalMap causes ClassCastException before explicit cast")
    void testGetWithTreeMapWrongKeyType() {
        // TreeMap<Integer, String> will throw ClassCastException when a String key is used
        Map<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "one");
        TrackingMap<Integer, String> tm = new TrackingMap<>(treeMap);
        // The exception originates from TreeMap.get, not from the ((K) key) cast
        ClassCastException cce = assertThrows(ClassCastException.class, () -> tm.get("stringKey"), "TreeMap should throw ClassCastException for incompatible key type");
        assertNotNull(cce);
    }

    @Test
    @DisplayName("TreeMap does not allow null keys – NullPointerException is thrown")
    void testGetWithTreeMapNullKey() {
        // TreeMap<String, String> does not permit null keys
        Map<String, String> treeMap = new TreeMap<>();
        treeMap.put("a", "A");
        TrackingMap<String, String> tm = new TrackingMap<>(treeMap);
        NullPointerException npe = assertThrows(NullPointerException.class, () -> tm.get(null), "TreeMap should throw NullPointerException for null key");
        assertNotNull(npe);
    }
}
