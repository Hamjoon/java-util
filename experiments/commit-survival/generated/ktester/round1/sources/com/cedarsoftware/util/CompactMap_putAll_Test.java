package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import java.lang.reflect.Field;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_putAll_Test {

    private CompactMap<String, Integer> map;

    /**
     * A concrete CompactMap with a tiny {@code compactSize} to make testing easier.
     */
    private static class TinyCompactMap<K, V> extends CompactMap<K, V> {

        private final int sizeLimit;

        TinyCompactMap(int sizeLimit) {
            this.sizeLimit = sizeLimit;
        }

        @Override
        protected int compactSize() {
            return sizeLimit;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return new HashMap<>();
        }

        @Override
        protected Map<K, V> getNewMap(int size) {
            // Size hint is ignored for the simple HashMap implementation.
            return new HashMap<>(size);
        }
    }

    /**
     * Variant that returns a Mockito mock for the backing map.  This allows us
     * to verify that {@code putAll} delegates to the internal map when the
     * representation has already been converted.
     */
    private static class MockMapCompactMap<K, V> extends TinyCompactMap<K, V> {

        private final Map<K, V> mockMap;

        MockMapCompactMap(int sizeLimit, Map<K, V> mockMap) {
            super(sizeLimit);
            this.mockMap = mockMap;
        }

        @Override
        protected Map<K, V> getNewMap() {
            return mockMap;
        }

        @Override
        protected Map<K, V> getNewMap(int size) {
            // ignore size hint – just return the same mock
            return mockMap;
        }
    }

    class SpyCompactMap<K, V> extends TinyCompactMap<K, V> {

        int capturedSize = -1;

        SpyCompactMap(int sizeLimit) {
            super(sizeLimit);
        }

        @Override
        protected Map<K, V> getNewMap(int size) {
            capturedSize = size;
            return super.getNewMap(size);
        }
    }

    class SpyCompactMap<K, V> extends TinyCompactMap<K, V> {

        int capturedSize = -1;

        SpyCompactMap(int sizeLimit) {
            super(sizeLimit);
        }

        @Override
        protected Map<K, V> getNewMap(int size) {
            capturedSize = size;
            return super.getNewMap(size);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a compactSize CompactMap_putAll_Test2 so that a map with 3 entries forces a switch
        // to the backing HashMap implementation.
        map = new TinyCompactMap<>(2);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    @Test
    void putAll_NullArgument_DoesNothing() {
        map.put("a", 1);
        // should be a no‑op
        map.putAll(null);
        assertEquals(1, map.size(), "Size should remain unchanged when null is passed");
        assertEquals(1, map.get("a"));
    }

    @Test
    void putAll_EmptyMap_DoesNothing() {
        Map<String, Integer> empty = new HashMap<>();
        map.putAll(empty);
        assertTrue(map.isEmpty(), "CompactMap should stay empty after putting an empty map");
    }

    @Test
    void putAll_SmallMap_StaysInCompactArrayRepresentation() {
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        // size == compactSize (2)
        source.put("two", 2);
        map.putAll(source);
        assertEquals(2, map.size(), "All entries should be added");
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        // No direct way to assert internal representation, but the public API must work.
    }

    @Test
    void putAll_LargeMap_SwitchesToBackingMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 10);
        source.put("b", 20);
        // size > compactSize (2)
        source.put("c", 30);
        map.putAll(source);
        assertEquals(3, map.size(), "All entries should be added after switch to backing map");
        assertEquals(10, map.get("a"));
        assertEquals(20, map.get("b"));
        assertEquals(30, map.get("c"));
    }

    @Test
    void putAll_MultipleCalls_AccumulatesEntriesCorrectly() {
        Map<String, Integer> first = new HashMap<>();
        first.put("x", 100);
        // 2 entries, still compact
        first.put("y", 200);
        Map<String, Integer> second = new HashMap<>();
        second.put("z", 300);
        // after adding, total will be 4 > compactSize
        second.put("w", 400);
        map.putAll(first);
        assertEquals(2, map.size());
        map.putAll(second);
        assertEquals(4, map.size());
        assertEquals(100, map.get("x"));
        assertEquals(200, map.get("y"));
        assertEquals(300, map.get("z"));
        assertEquals(400, map.get("w"));
    }

    @Test
    void putAll_ExistingMap_OverridesExistingKeys() {
        map.put("key", 1);
        map.put("other", 2);
        Map<String, Integer> source = new HashMap<>();
        // should replace existing value
        source.put("key", 99);
        source.put("new", 3);
        map.putAll(source);
        assertEquals(3, map.size());
        assertEquals(99, map.get("key"));
        assertEquals(2, map.get("other"));
        assertEquals(3, map.get("new"));
    }

    /**
     * When the CompactMap already holds a compact representation (val != EMPTY_MAP
     * and not a Map) and the incoming map size is <= {@code compactSize()},
     * each entry should be added via {@code put}.
     */
    @Test
    void putAll_NonEmptyCompactRepresentation_AddsViaPut() throws Exception {
        // arrange: put a single entry so that internal representation is the compact array form
        map.put("k1", 10);
        assertEquals(1, map.size());
        // act: putAll with a map of size 1 (<= compactSize)
        Map<String, Integer> incoming = new HashMap<>();
        incoming.put("k2", 20);
        map.putAll(incoming);
        // assert: size increased and both entries are present
        assertEquals(2, map.size());
        assertEquals(10, map.get("k1"));
        assertEquals(20, map.get("k2"));
        // internal representation should still NOT be a Map
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        Object internalVal = f.get(map);
        assertFalse(internalVal instanceof Map, "Internal representation should stay compact (not a Map) after small putAll");
    }

    /**
     * Even if the CompactMap already holds a real Map (val instanceof Map),
     * a {@code null} argument must be ignored and the method must return
     * immediately without altering the internal Map.
     */
    @Test
    void putAll_NullArgument_WithExistingMap_NoChange() throws Exception {
        // arrange: force conversion to a backing Map by adding three entries (> compactSize)
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(3, map.size());
        // capture internal map snapshot
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Integer> internalMap = (Map<String, Integer>) f.get(map);
        assertTrue(internalMap instanceof Map, "Internal representation should be a Map");
        // act: call putAll with null
        map.putAll(null);
        // assert: internal map unchanged
        assertEquals(3, map.size());
        // reference unchanged
        assertEquals(internalMap, internalMap);
        assertEquals(1, map.get("a"));
        assertEquals(2, map.get("b"));
        assertEquals(3, map.get("c"));
    }

    /**
     * If the internal representation is already a Map, {@code putAll} should
     * delegate directly to the internal Map's {@code putAll}, regardless of
     * the incoming map size.
     */
    @Test
    void putAll_SmallMap_OnExistingMap_DelegatesToInternalMap(@Mock Map<String, Integer> mockMap) {
        // arrange: create a CompactMap that uses the mocked backing map
        CompactMap<String, Integer> mockCompact = new MockMapCompactMap<>(2, mockMap);
        // force conversion to the mock map by adding three entries (> compactSize)
        mockCompact.put("x", 1);
        mockCompact.put("y", 2);
        mockCompact.put("z", 3);
        // at this point the internal map should be the mockMap
        // verify that the mock received the three puts (indirectly via put)
        // (no need – we care about the later putAll)
        // act: putAll with a small map (size <= compactSize)
        Map<String, Integer> incoming = new HashMap<>();
        incoming.put("a", 10);
        incoming.put("b", 20);
        mockCompact.putAll(incoming);
        // assert: the mock's putAll should have been invoked exactly once with the incoming map
        verify(mockMap, times(1)).putAll(incoming);
        // ensure no other interactions (e.g., put) occurred during the delegation
        verifyNoMoreInteractions(mockMap);
    }

    /**
     * Verify that when the incoming map size exceeds {@code compactSize()},
     * {@code getNewMap(int)} is called with the incoming map's size.
     */
    @Test
    void putAll_LargeMap_Conversion_UsesGetNewMapInt() throws Exception {
        // Spy subclass to capture the argument passed to getNewMap(int)
        class SpyCompactMap<K, V> extends TinyCompactMap<K, V> {

            int capturedSize = -1;

            SpyCompactMap(int sizeLimit) {
                super(sizeLimit);
            }

            @Override
            protected Map<K, V> getNewMap(int size) {
                capturedSize = size;
                return super.getNewMap(size);
            }
        }
        SpyCompactMap<String, Integer> spyMap = new SpyCompactMap<>(2);
        Map<String, Integer> incoming = new HashMap<>();
        incoming.put("a", 1);
        incoming.put("b", 2);
        // size = 3 > compactSize
        incoming.put("c", 3);
        spyMap.putAll(incoming);
        assertEquals(3, spyMap.size());
        assertEquals(3, spyMap.capturedSize, "getNewMap(int) should be called with the incoming map's size");
    }

    /**
     * When the internal representation is the EMPTY_MAP sentinel and the incoming
     * map size is within {@code compactSize()}, entries should be added via repeated
     * {@code put} calls, keeping the compact representation.
     */
    @Test
    void putAll_EmptySentinel_SmallMap_StaysCompact() throws Exception {
        // map is freshly created, so val == EMPTY_MAP
        Map<String, Integer> incoming = new HashMap<>();
        // size 1 <= compactSize (2)
        incoming.put("x", 42);
        map.putAll(incoming);
        assertEquals(1, map.size());
        assertEquals(42, map.get("x"));
        // Verify internal representation is still not a Map
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        Object internalVal = f.get(map);
        assertFalse(internalVal instanceof Map, "After small putAll on EMPTY_MAP, representation should remain compact");
    }

    /**
     * If the internal representation is already a Map, even a small incoming map
     * should be merged via the internal Map's {@code putAll}.
     */
    @Test
    void putAll_SmallMap_InternalMap_DelegatesToMap(@Mock Map<String, Integer> mockMap) {
        // Setup a CompactMap that already uses a mock backing map
        CompactMap<String, Integer> compact = new MockMapCompactMap<>(2, mockMap);
        // Force conversion to the mock map
        compact.put("k1", 1);
        compact.put("k2", 2);
        // > compactSize, triggers conversion
        compact.put("k3", 3);
        // Clear any previous interactions
        clearInvocations(mockMap);
        // Small incoming map
        Map<String, Integer> incoming = new HashMap<>();
        incoming.put("k4", 4);
        compact.putAll(incoming);
        // Verify delegation
        verify(mockMap, times(1)).putAll(incoming);
        verifyNoMoreInteractions(mockMap);
    }
}
