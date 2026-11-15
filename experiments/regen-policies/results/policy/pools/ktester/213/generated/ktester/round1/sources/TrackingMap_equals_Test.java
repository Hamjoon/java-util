package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_equals_Test {

    /**
     * The TrackingMap instance under test.
     */
    private TrackingMap<String, String> trackingMap;

    /**
     * A mock backing map used to verify delegation of equals().
     */
    @Mock
    private Map<String, String> mockBackingMap;

    /**
     * Simple Map implementation whose {@code equals} always throws a {@link RuntimeException}.
     */
    private static class ThrowingMap<K, V> implements Map<K, V> {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public V get(Object key) {
            return null;
        }

        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("Intentional exception from ThrowingMap.equals");
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The mockBackingMap is injected by Mockito; create the TrackingMap with it.
        trackingMap = new TrackingMap<>(mockBackingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions with the mock to keep each test isolated.
        clearInvocations(mockBackingMap);
    }

    private static Stream<Arguments> typeCheckingProvider() {
        return Stream.of(Arguments.of("a plain String", false, "The argument is not a Map (String). The method should return false because the instanceof check fails."), Arguments.of(null, false, "The argument is null. instanceof returns false, so equals should return false."));
    }

    private static Stream<Arguments> functionalProvider() {
        // Helper to build maps from a simple var‑args representation.
        Map<String, Integer> internal = new HashMap<>();
        internal.put("key1", 1);
        internal.put("key2", 2);
        return Stream.of(// same entries, HashMap
        Arguments.of(internal, mapOf("key1", 1, "key2", 2), true, "The internal map contains {\"key1\"=1,\"key2\"=2} and the argument is a HashMap with the same entries."), // same entries, TreeMap (different order)
        Arguments.of(internal, treeMapOf("key2", 2, "key1", 1), true, "The internal map contains {\"key1\"=1,\"key2\"=2}. The argument is a TreeMap with the same entries in a different order."), // different value for key2
        Arguments.of(internal, mapOf("key1", 1, "key2", 3), false, "The internal map contains {\"key1\"=1,\"key2\"=2} but the argument map differs in the value for \"key2\"."), // argument is the exact internal map instance
        Arguments.of(internal, internal, true, "The argument is the exact same Map instance that backs the TrackingMap (the internalMap)."), // extra entry
        Arguments.of(internal, mapOf("key1", 1, "key2", 2, "extra", 99), false, "The argument map contains all entries of the internal map plus an additional entry."), // both empty
        Arguments.of(Collections.emptyMap(), Collections.emptyMap(), true, "Both the internal map and the argument map are empty."), // internal empty, argument non‑empty
        Arguments.of(Collections.emptyMap(), mapOf("only", 42), false, "The internal map is empty while the argument map contains one entry."), // LinkedHashMap with same entries
        Arguments.of(internal, linkedHashMapOf("key1", 1, "key2", 2), true, "The argument is a LinkedHashMap (a subclass of Map) with the same entries as the internal map."), // null key/value pair equal
        Arguments.of(mapOf(null, null), mapOf(null, null), true, "Both the internal map and the argument map contain a single entry with a null key and a null value."), // null key with different value
        Arguments.of(mapOf(null, null), mapOf(null, "value"), false, "The internal map has a null key with a null value, while the argument map has the same null key but a non‑null value."));
    }

    @Test
    void equals_DelegatesToBackingMap_WhenOtherIsMap_AndBackingMapReturnsTrue() {
        Map<String, String> other = new HashMap<>();
        when(mockBackingMap.equals(other)).thenReturn(true);
        assertTrue(trackingMap.equals(other), "equals() should return true when backing map returns true");
        verify(mockBackingMap, times(1)).equals(other);
    }

    @Test
    void equals_DelegatesToBackingMap_WhenOtherIsMap_AndBackingMapReturnsFalse() {
        Map<String, String> other = new HashMap<>();
        when(mockBackingMap.equals(other)).thenReturn(false);
        assertFalse(trackingMap.equals(other), "equals() should return false when backing map returns false");
        verify(mockBackingMap, times(1)).equals(other);
    }

    @Test
    void equals_ReturnsFalse_WhenOtherIsNotAMap() {
        String notAMap = "I am not a map";
        assertFalse(trackingMap.equals(notAMap), "equals() must return false for non‑Map arguments");
        // The backing map's equals should never be invoked.
        verify(mockBackingMap, never()).equals(any());
    }

    @Test
    void equals_ReturnsFalse_WhenOtherIsNull() {
        assertFalse(trackingMap.equals(null), "equals() must return false for null argument");
        // The backing map's equals should never be invoked.
        verify(mockBackingMap, never()).equals(any());
    }

    @Test
    void equals_True_ForTwoTrackingMapsWithSameBackingInstance() {
        Map<String, String> shared = new HashMap<>();
        shared.put("a", "1");
        shared.put("b", "2");
        TrackingMap<String, String> map1 = new TrackingMap<>(shared);
        TrackingMap<String, String> map2 = new TrackingMap<>(shared);
        assertTrue(map1.equals(map2), "Two TrackingMap instances wrapping the same backing map should be equal");
        assertTrue(map2.equals(map1), "Equality should be symmetric");
    }

    @Test
    void equals_True_ForTwoTrackingMapsWithDifferentBackingsButSameEntries() {
        Map<String, String> backing1 = new HashMap<>();
        backing1.put("x", "10");
        backing1.put("y", "20");
        Map<String, String> backing2 = new HashMap<>();
        // identical content, different instance
        backing2.putAll(backing1);
        TrackingMap<String, String> map1 = new TrackingMap<>(backing1);
        TrackingMap<String, String> map2 = new TrackingMap<>(backing2);
        assertTrue(map1.equals(map2), "TrackingMaps with equal entry sets should be equal");
        assertTrue(map2.equals(map1), "Equality should be symmetric");
    }

    @Test
    void equals_False_WhenOtherMapHasDifferentEntries() {
        Map<String, String> backing = new HashMap<>();
        backing.put("k1", "v1");
        TrackingMap<String, String> map = new TrackingMap<>(backing);
        Map<String, String> other = new HashMap<>();
        other.put("k1", "different");
        assertFalse(map.equals(other), "TrackingMap should not be equal to a Map with different entries");
    }

    @ParameterizedTest(name = "[{index}] other={0} → expected={1}")
    @MethodSource("typeCheckingProvider")
    void equals_TypeChecking(Object other, boolean expected, String description) {
        // Use a real backing map so that the test does not rely on the mock.
        TrackingMap<String, Integer> map = new TrackingMap<>(new HashMap<>());
        boolean result = map.equals(other);
        assertEquals(expected, result, description);
        // No interaction with mockBackingMap should happen – we created a fresh instance.
    }

    @ParameterizedTest(name = "[{index}] expected={3}")
    @MethodSource("functionalProvider")
    void equals_FunctionalCorrectness(Map<String, Integer> internalMap, Map<String, Integer> other, boolean expected, String description) {
        // Build a TrackingMap that wraps a *real* mutable map populated with the supplied internalMap.
        Map<String, Integer> backing = new HashMap<>(internalMap);
        TrackingMap<String, Integer> map = new TrackingMap<>(backing);
        boolean result = map.equals(other);
        assertEquals(expected, result, description);
    }

    @Test
    void equals_PropagatesException_FromOtherMapEquals() {
        Map<String, String> other = new ThrowingMap<>();
        // Use a real backing map for this test – the internal map itself is not throwing.
        TrackingMap<String, String> map = new TrackingMap<>(new HashMap<>());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.equals(other), "TrackingMap.equals should propagate RuntimeException thrown by the other map's equals");
        assertEquals("Intentional exception from ThrowingMap.equals", ex.getMessage());
    }

    @Test
    void equals_PropagatesException_FromInternalMapEquals() throws Exception {
        // Prepare a ThrowingMap to become the internalMap via reflection.
        ThrowingMap<String, String> throwingInternal = new ThrowingMap<>();
        // Create a TrackingMap with a normal backing map first.
        TrackingMap<String, String> map = new TrackingMap<>(new HashMap<>());
        // Replace the private internalMap field.
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(map, throwingInternal);
        // any map – we only care about internalMap.equals
        Map<String, String> other = new HashMap<>();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> map.equals(other), "TrackingMap.equals should propagate RuntimeException thrown by its internal map's equals");
        assertEquals("Intentional exception from ThrowingMap.equals", ex.getMessage());
    }

    @Test
    void equals_ThrowsNullPointerException_WhenInternalMapIsNull() throws Exception {
        TrackingMap<String, String> map = new TrackingMap<>(new HashMap<>());
        // Nullify the internalMap via reflection.
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(map, null);
        Map<String, String> other = new HashMap<>();
        assertThrows(NullPointerException.class, () -> map.equals(other), "When internalMap is null, invoking equals should throw NullPointerException");
    }

    @SafeVarargs
    private static <K, V> Map<K, V> mapOf(Object... keyVals) {
        Map<K, V> m = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            @SuppressWarnings("unchecked")
            K k = (K) keyVals[i];
            @SuppressWarnings("unchecked")
            V v = (V) keyVals[i + 1];
            m.put(k, v);
        }
        return m;
    }

    @SafeVarargs
    private static <K, V> Map<K, V> linkedHashMapOf(Object... keyVals) {
        Map<K, V> m = new LinkedHashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            @SuppressWarnings("unchecked")
            K k = (K) keyVals[i];
            @SuppressWarnings("unchecked")
            V v = (V) keyVals[i + 1];
            m.put(k, v);
        }
        return m;
    }

    @SafeVarargs
    private static <K, V extends Comparable<V>> Map<K, V> treeMapOf(Object... keyVals) {
        Map<K, V> m = new TreeMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            @SuppressWarnings("unchecked")
            K k = (K) keyVals[i];
            @SuppressWarnings("unchecked")
            V v = (V) keyVals[i + 1];
            m.put(k, v);
        }
        return m;
    }
}
