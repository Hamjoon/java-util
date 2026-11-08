package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_putAll_Test {

    /**
     * Subclass that forces a tiny {@code compactSize()} so we can test the
     *  boundary and map‑branch logic without creating huge maps.
     */
    private static class TinyCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // tiny threshold for testing
            return 2;
        }
    }

    /**
     * Subclass that returns a non‑Map object from {@code getNewMap} to provoke
     *  a {@link ClassCastException} when the map‑branch is taken.
     */
    private static class BadMapCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            // deliberately wrong type
            return (Map<K, V>) new Object();
        }

        @Override
        protected Map<K, V> getNewMap(int size) {
            // deliberately wrong type
            return (Map<K, V>) new Object();
        }
    }

    /**
     * Helper key whose {@code hashCode()} throws an exception.
     */
    private static final class BadKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("bad key hashCode");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    /**
     * Helper value whose {@code hashCode()} throws an exception.
     */
    private static final class BadValue {

        @Override
        public int hashCode() {
            throw new IllegalArgumentException("bad value hashCode");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    /**
     * Helper to create a map with the given number of entries. Keys/values are simple strings.
     */
    private static Map<String, String> mapOfSize(int size) {
        Map<String, String> m = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            m.put("k" + i, "v" + i);
        }
        return m;
    }

    /**
     * Utility to read the private {@code val} field of a CompactMap instance.
     */
    private static Object getInternalVal(CompactMap<?, ?> cm) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        return f.get(cm);
    }

    @Test
    void putAll_NullMap_DoesNothing() {
        CompactMap<String, String> cm = new CompactMap<>();
        cm.put("existingKey", "existingValue");
        // should be a no‑op
        cm.putAll(null);
        assertEquals(1, cm.size(), "Size should remain unchanged when null is passed");
        assertEquals("existingValue", cm.get("existingKey"));
    }

    @Test
    void putAll_SmallMap_AddsEntriesViaPutLoop() {
        CompactMap<String, String> cm = new CompactMap<>();
        // a map smaller than the default compactSize (80)
        Map<String, String> small = mapOfSize(5);
        cm.putAll(small);
        assertEquals(5, cm.size(), "All entries from the small map must be added");
        for (int i = 0; i < 5; i++) {
            assertEquals("v" + i, cm.get("k" + i));
        }
    }

    @Test
    void putAll_LargeMap_TriggersUnderlyingMapConversion() {
        CompactMap<String, String> cm = new CompactMap<>();
        // default compactSize is 80, so we use 81 entries to exceed it
        Map<String, String> large = mapOfSize(81);
        cm.putAll(large);
        assertEquals(81, cm.size(), "All entries from the large map must be present after conversion");
        // spot‑check a few entries
        assertEquals("v0", cm.get("k0"));
        assertEquals("v40", cm.get("k40"));
        assertEquals("v80", cm.get("k80"));
    }

    @Test
    void putAll_WhenAlreadyUsingUnderlyingMap_MergesCorrectly() {
        CompactMap<String, String> cm = new CompactMap<>();
        // First, add enough entries to force the internal representation to become a Map
        // > compactSize, conversion occurs
        cm.putAll(mapOfSize(85));
        // Verify conversion happened indirectly by checking size
        assertEquals(85, cm.size());
        // Now putAll with a second map (both small and large) and verify merge
        // keys k0‑k9 will overwrite first 10 entries
        Map<String, String> additional = mapOfSize(10);
        cm.putAll(additional);
        // Size should be 85 (original) + 10 (new distinct keys) = 95 because keys k0‑k9 are duplicated
        assertEquals(95, cm.size());
        // Verify that overwritten keys have values from the second map
        for (int i = 0; i < 10; i++) {
            assertEquals("v" + i, cm.get("k" + i), "Value should be overwritten by the second map");
        }
        // Verify a key that wasn't overwritten retains its original value
        assertEquals("v50", cm.get("k50"));
    }

    @Test
    void putAll_EmptyArgumentMap_NoChangeAndKeepsCompactRepresentation() throws Exception {
        CompactMap<String, String> cm = new CompactMap<>();
        cm.putAll(Collections.emptyMap());
        assertEquals(0, cm.size(), "Size must stay 0 for empty argument map");
        assertEquals(CompactMap.EMPTY_MAP, getInternalVal(cm), "Internal val should remain the sentinel EMPTY_MAP");
    }

    @Test
    void putAll_SingleEntry_FitsCompactSize_StaysCompact() throws Exception {
        TinyCompactMap<String, String> cm = new TinyCompactMap<>();
        Map<String, String> one = Collections.singletonMap("key1", "value1");
        cm.putAll(one);
        assertEquals(1, cm.size());
        assertEquals("value1", cm.get("key1"));
        assertFalse(getInternalVal(cm) instanceof Map, "Internal representation must stay compact (not a Map)");
    }

    @Test
    void putAll_BoundarySize_EqualsCompactSize_StaysCompact() throws Exception {
        TinyCompactMap<String, Integer> cm = new TinyCompactMap<>();
        Map<String, Integer> two = new LinkedHashMap<>();
        two.put("k1", 1);
        two.put("k2", 2);
        cm.putAll(two);
        assertEquals(2, cm.size());
        assertEquals(1, cm.get("k1"));
        assertEquals(2, cm.get("k2"));
        assertFalse(getInternalVal(cm) instanceof Map, "When size == compactSize the map branch must NOT be taken");
    }

    @Test
    void putAll_SizeGreaterThanCompactSize_TakesMapBranch() throws Exception {
        TinyCompactMap<String, Integer> cm = new TinyCompactMap<>();
        Map<String, Integer> three = new LinkedHashMap<>();
        three.put("a", 10);
        three.put("b", 20);
        three.put("c", 30);
        cm.putAll(three);
        assertEquals(3, cm.size());
        assertEquals(10, cm.get("a"));
        assertEquals(20, cm.get("b"));
        assertEquals(30, cm.get("c"));
        assertTrue(getInternalVal(cm) instanceof Map, "Internal representation must be a Map after exceeding compactSize");
    }

    @Test
    void putAll_NullKeyAndNullValue_Accepted() {
        CompactMap<Object, Object> cm = new CompactMap<>();
        Map<Object, Object> map = new HashMap<>();
        map.put(null, null);
        cm.putAll(map);
        assertEquals(1, cm.size());
        assertTrue(cm.containsKey(null), "Map must contain the null key");
        assertNull(cm.get(null), "Value for null key must be null");
    }

    @Test
    void putAll_OverwriteExistingKey_ValueReplacedSizeUnchanged() {
        CompactMap<String, String> cm = new CompactMap<>();
        cm.put("dup", "oldValue");
        assertEquals(1, cm.size());
        Map<String, String> overwriter = Collections.singletonMap("dup", "newValue");
        cm.putAll(overwriter);
        assertEquals(1, cm.size(), "Size must stay unchanged when overwriting");
        assertEquals("newValue", cm.get("dup"), "Value should be replaced by the new one");
    }

    @Test
    void putAll_SortedMapArgument_EntriesStoredCorrectly() {
        CompactMap<String, Integer> cm = new CompactMap<>();
        TreeMap<String, Integer> tree = new TreeMap<>();
        tree.put("b", 2);
        tree.put("a", 1);
        cm.putAll(tree);
        assertEquals(2, cm.size());
        assertEquals(1, cm.get("a"));
        assertEquals(2, cm.get("b"));
        // Order is irrelevant for correctness; just ensure both are present
    }

    @Test
    void putAll_MapSizeThrowsRuntimeException_Propagates() {
        @SuppressWarnings("unchecked")
        Map<String, String> badMap = mock(Map.class);
        when(badMap.size()).thenThrow(new RuntimeException("size error"));
        CompactMap<String, String> cm = new CompactMap<>();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cm.putAll(badMap), "Exception from map.size() must be propagated");
        assertEquals("size error", ex.getMessage());
    }

    @Test
    void putAll_MapEntrySetThrowsRuntimeException_Propagates() {
        @SuppressWarnings("unchecked")
        Map<String, String> badMap = mock(Map.class);
        when(badMap.size()).thenReturn(1);
        when(badMap.entrySet()).thenThrow(new RuntimeException("entrySet error"));
        CompactMap<String, String> cm = new CompactMap<>();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cm.putAll(badMap), "Exception from map.entrySet() must be propagated");
        assertEquals("entrySet error", ex.getMessage());
    }

    @Test
    void putAll_ConcurrentModificationDuringIteration_ThrowsCME() {
        // Create a map whose iterator throws CME on the second call
        Set<Map.Entry<String, String>> entrySet = new AbstractSet<>() {

            private boolean first = true;

            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                return new Iterator<>() {

                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Map.Entry<String, String> next() {
                        if (first) {
                            first = false;
                            return new AbstractMap.SimpleEntry<>("k", "v");
                        } else {
                            throw new ConcurrentModificationException("modified during iteration");
                        }
                    }
                };
            }

            @Override
            public int size() {
                // pretend there are two entries
                return 2;
            }
        };
        @SuppressWarnings("unchecked")
        Map<String, String> badMap = mock(Map.class);
        when(badMap.size()).thenReturn(2);
        when(badMap.entrySet()).thenReturn(entrySet);
        CompactMap<String, String> cm = new CompactMap<>();
        assertThrows(ConcurrentModificationException.class, () -> cm.putAll(badMap), "ConcurrentModificationException should propagate");
    }

    @Test
    void putAll_KeyHashCodeThrowsException_Propagates() {
        Map<BadKey, String> map = new HashMap<>();
        map.put(new BadKey(), "value");
        CompactMap<BadKey, String> cm = new CompactMap<>();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cm.putAll(map), "Exception from key.hashCode() must propagate");
        assertEquals("bad key hashCode", ex.getMessage());
    }

    @Test
    void putAll_ValueHashCodeThrowsException_Propagates() {
        Map<String, BadValue> map = new HashMap<>();
        map.put("k", new BadValue());
        CompactMap<String, BadValue> cm = new CompactMap<>();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> cm.putAll(map), "Exception from value.hashCode() must propagate");
        assertEquals("bad value hashCode", ex.getMessage());
    }

    @Test
    void putAll_InvalidInternalMapCausesClassCastException() {
        // This CompactMap subclass returns a non‑Map from getNewMap()
        BadMapCompactMap<String, String> cm = new BadMapCompactMap<>();
        // size > default compactSize (2) to trigger map branch
        Map<String, String> large = mapOfSize(3);
        assertThrows(ClassCastException.class, () -> cm.putAll(large), "Casting a non‑Map internal value should throw ClassCastException");
    }

    @Test
    void putAll_SubsequentPutAllOnAlreadyMapBacked_UsesMapBranch() throws Exception {
        // Force conversion with a first large putAll
        TinyCompactMap<String, String> cm = new TinyCompactMap<>();
        // > compactSize => internal Map
        cm.putAll(mapOfSize(3));
        // Verify internal representation is a Map
        assertTrue(getInternalVal(cm) instanceof Map, "After first large putAll, internal val must be a Map");
        // Second putAll with a single entry – should merge via map branch
        Map<String, String> one = Collections.singletonMap("extra", "value");
        cm.putAll(one);
        assertEquals(4, cm.size());
        assertEquals("value", cm.get("extra"));
        assertTrue(getInternalVal(cm) instanceof Map, "Internal val must stay a Map after subsequent putAll");
    }
}
