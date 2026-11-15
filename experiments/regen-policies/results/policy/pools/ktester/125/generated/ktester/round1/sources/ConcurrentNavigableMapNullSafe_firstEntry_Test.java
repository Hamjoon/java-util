package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.ConcurrentSkipListMap;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_firstEntry_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map for each test
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to aid garbage collection (not strictly required)
        map.clear();
    }

    /**
     * Verify that {@code firstEntry()} returns the entry with the smallest (natural) key.
     */
    @Test
    void testFirstEntryNormal() {
        // Insert entries in non‑sorted order
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        // null should be ignored for firstEntry (per API contract)
        map.put(null, 99);
        Map.Entry<String, Integer> first = map.firstEntry();
        assertNotNull(first, "firstEntry() should not be null when the map contains non‑null keys");
        assertEquals("a", first.getKey(), "The smallest key should be returned");
        assertEquals(1, first.getValue(), "The value associated with the smallest key should be returned");
    }

    /**
     * Verify that {@code firstEntry()} returns {@code null} when the map only contains a null key.
     * Null keys are considered last in the ascending view.
     */
    @Test
    void testFirstEntryOnlyNullKey() {
        map.put(null, 42);
        Map.Entry<String, Integer> first = map.firstEntry();
        assertNull(first, "firstEntry() should be null when the map only contains a null key");
    }

    /**
     * Verify that {@code firstEntry()} returns {@code null} for an empty map.
     */
    @Test
    void testFirstEntryEmptyMap() {
        Map.Entry<String, Integer> first = map.firstEntry();
        assertNull(first, "firstEntry() should be null for an empty map");
    }

    /**
     * When the map contains a {@code null} value, {@code firstEntry()} must still return the correct entry.
     */
    @Test
    void testFirstEntryWithNullValue() {
        map.put("b", null);
        map.put("c", 3);
        Map.Entry<String, Integer> first = map.firstEntry();
        assertNotNull(first, "firstEntry() should not be null when a null value is present");
        assertEquals("b", first.getKey(), "The smallest key should be returned even if its value is null");
        assertNull(first.getValue(), "The value associated with the smallest key should be null");
    }

    /**
     * When a null key is stored, the map masks it internally.
     * {@code firstEntry()} must un‑mask the key and return an entry whose key is {@code null}.
     */
    @Test
    void testFirstEntryWithNullKey() {
        map.put(null, 42);
        map.put("x", 99);
        Map.Entry<String, Integer> first = map.firstEntry();
        assertNotNull(first, "firstEntry() should not be null when a null key is present");
        assertNull(first.getKey(), "The returned entry's key must be null after un‑masking");
        assertEquals(42, first.getValue(), "The value associated with the null key should be returned");
    }

    /**
     * Simulate a corrupted internal state where {@code internalMap} is {@code null}.
     * The method should throw a {@link NullPointerException}.
     */
    @Test
    void testFirstEntryInternalMapNullPointerException() throws Exception {
        // Use reflection to set the private field 'internalMap' (declared in the superclass) to null
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, null);
        assertThrows(NullPointerException.class, () -> map.firstEntry(), "firstEntry() should throw NullPointerException when internalMap is null");
    }

    /**
     * Replace the {@code internalMap} with an incompatible implementation (e.g., {@link HashMap}).
     * The cast to {@link ConcurrentSkipListMap} performed inside {@code firstEntry()} should cause a {@link ClassCastException}.
     */
    @Test
    void testFirstEntryClassCastException() throws Exception {
        // Use reflection to replace internalMap with a plain HashMap (which does not implement ConcurrentSkipListMap)
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(map, new HashMap<>());
        assertThrows(ClassCastException.class, () -> map.firstEntry(), "firstEntry() should throw ClassCastException when internalMap is not a ConcurrentSkipListMap");
    }
}
