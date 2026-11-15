package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_equals_Test {

    private Map<String, Integer> backingMap;

    private TrackingMap<String, Integer> trackingMap;

    private static class BadEqualsObject {

        @Override
        public boolean equals(Object obj) {
            throw new IllegalStateException("Deliberate exception from value.equals()");
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }

    private static class EqualsThrowingMap<K, V> implements Map<K, V> {

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
            throw new IllegalArgumentException("Deliberate exception from internalMap.equals()");
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    private static class BadEntrySetMap<K, V> implements Map<K, V> {

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
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
            return new AbstractSet<Entry<K, V>>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return new Iterator<Entry<K, V>>() {

                        @Override
                        public boolean hasNext() {
                            return true;
                        }

                        @Override
                        public Entry<K, V> next() {
                            throw new ConcurrentModificationException("Iterator deliberately fails");
                        }

                        @Override
                        public void remove() {
                        }
                    };
                }

                @Override
                public int size() {
                    return 1;
                }
            };
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        backingMap.clear();
    }

    private static Stream<Arguments> nonMapArgumentsProvider() {
        return Stream.of(Arguments.of(null, false, "Passing null should return false because null is not an instance of Map."), Arguments.of(Integer.valueOf(42), false, "A non‑Map object (Integer) must cause equals() to return false."), Arguments.of("some string", false, "A non‑Map object (String) must cause equals() to return false."));
    }

    private static Stream<Arguments> sameEntriesProvider() {
        // Prepare a TrackingMap instance with entries a=1, b=2
        Map<String, Integer> internal = new HashMap<>();
        internal.put("a", 1);
        internal.put("b", 2);
        TrackingMap<String, Integer> sameTrackingMap = new TrackingMap<>(internal);
        // Plain HashMap with same entries
        Map<String, Integer> sameHashMap = new HashMap<>();
        sameHashMap.put("a", 1);
        sameHashMap.put("b", 2);
        return Stream.of(Arguments.of(sameHashMap, true, "A HashMap with exactly the same key‑value pairs as the internal map should make equals() return true."), Arguments.of(sameTrackingMap, true, "Another TrackingMap containing the same entries should be considered equal."), // will be overridden in test body
        Arguments.of(sameTrackingMap, true, "Passing the same TrackingMap instance (self‑reference) must return true."));
    }

    private static Stream<Arguments> differentEntriesProvider() {
        // Map with a different key/value
        Map<String, Integer> diffKeyMap = new HashMap<>();
        diffKeyMap.put("a", 1);
        diffKeyMap.put("c", 3);
        // TrackingMap with different entry
        Map<String, Integer> internal = new HashMap<>();
        internal.put("a", 1);
        internal.put("c", 3);
        TrackingMap<String, Integer> diffTrackingMap = new TrackingMap<>(internal);
        // Superset map
        Map<String, Integer> superset = new HashMap<>();
        superset.put("a", 1);
        superset.put("b", 2);
        superset.put("c", 3);
        return Stream.of(Arguments.of(diffKeyMap, false, "A Map with a different key/value set should cause equals() to return false."), Arguments.of(diffTrackingMap, false, "Another TrackingMap with differing entries must not be equal."), Arguments.of(superset, false, "A superset Map (contains all internal entries plus extra) should return false."));
    }

    private static Stream<Arguments> emptyMapProvider() {
        return Stream.of(Arguments.of(Collections.emptyMap(), true, "When the internal map is empty, comparing with an empty map must return true."), Arguments.of(new HashMap<String, Integer>() {

            {
                put("x", 99);
            }
        }, false, "Empty internal map compared to a non‑empty map must return false."));
    }

    private static Stream<Arguments> nullKeyValueProvider() {
        Map<String, Integer> internal = new HashMap<>();
        internal.put(null, 0);
        internal.put("k", null);
        TrackingMap<String, Integer> mapWithNulls = new TrackingMap<>(internal);
        Map<String, Integer> equalMap = new HashMap<>();
        equalMap.put(null, 0);
        equalMap.put("k", null);
        Map<String, Integer> differentMap = new HashMap<>();
        // different value for null key
        differentMap.put(null, 1);
        differentMap.put("k", null);
        return Stream.of(Arguments.of(mapWithNulls, equalMap, true, "If the internal map contains a null key and a null value, a Map with the same null entries must be equal."), Arguments.of(mapWithNulls, differentMap, false, "When the null key maps to a different value, equals() must return false."));
    }

    @Test
    void equalsSameInstance() {
        assertTrue(trackingMap.equals(trackingMap), "A TrackingMap should be equal to itself");
    }

    @Test
    void equalsNullReturnsFalse() {
        assertFalse(trackingMap.equals(null), "equals(null) must return false");
    }

    @Test
    void equalsNonMapObjectReturnsFalse() {
        Object notAMap = "I am not a map";
        assertFalse(trackingMap.equals(notAMap), "equals(Object) should return false for non‑Map objects");
    }

    @Test
    void equalsOtherTrackingMapWithSameBackingReference() {
        TrackingMap<String, Integer> other = new TrackingMap<>(backingMap);
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        // Both TrackingMap instances share the same internalMap reference
        assertEquals(trackingMap, other, "TrackingMaps sharing the same backing map should be equal");
        assertEquals(other, trackingMap, "Equality should be symmetric");
    }

    @Test
    void equalsOtherTrackingMapWithSeparateBackingButSameEntries() {
        Map<String, Integer> map1 = new HashMap<>();
        Map<String, Integer> map2 = new HashMap<>();
        map1.put("x", 10);
        map1.put("y", 20);
        map2.put("x", 10);
        map2.put("y", 20);
        TrackingMap<String, Integer> tm1 = new TrackingMap<>(map1);
        TrackingMap<String, Integer> tm2 = new TrackingMap<>(map2);
        assertEquals(tm1, tm2, "TrackingMaps with equal content should be equal even if backing maps differ");
        assertEquals(tm2, tm1, "Equality must be symmetric");
    }

    @Test
    void equalsOtherTrackingMapWithDifferentContent() {
        Map<String, Integer> map1 = new HashMap<>();
        Map<String, Integer> map2 = new HashMap<>();
        map1.put("k1", 100);
        // different value
        map2.put("k1", 200);
        TrackingMap<String, Integer> tm1 = new TrackingMap<>(map1);
        TrackingMap<String, Integer> tm2 = new TrackingMap<>(map2);
        assertNotEquals(tm1, tm2, "TrackingMaps with different entries must not be equal");
    }

    @Test
    void equalsPlainMapWithSameEntries() {
        backingMap.put("alpha", 1);
        backingMap.put("beta", 2);
        Map<String, Integer> plainMap = new HashMap<>();
        plainMap.put("alpha", 1);
        plainMap.put("beta", 2);
        assertEquals(trackingMap, plainMap, "TrackingMap should be equal to any Map with the same entries");
        assertEquals(plainMap, trackingMap, "Equality should be symmetric with plain Map implementations");
    }

    @Test
    void equalsPlainMapWithDifferentEntries() {
        backingMap.put("one", 1);
        Map<String, Integer> plainMap = new HashMap<>();
        // different value
        plainMap.put("one", 2);
        assertNotEquals(trackingMap, plainMap, "TrackingMap must not equal a Map with differing entries");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("nonMapArgumentsProvider")
    void equalsNonMapArguments(Object other, boolean expected, String description) {
        // No need to populate trackingMap – it is empty by default
        boolean result = trackingMap.equals(other);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("sameEntriesProvider")
    void equalsMapSameEntries(Object other, boolean expected, String description) {
        // Populate the trackingMap under test
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        // Special handling for the "self‑reference" case
        if (other instanceof TrackingMap) {
            // The provider gave a different instance; we need the self‑reference case separately
            // We'll handle it below with a dedicated test.
        }
        boolean result = trackingMap.equals(other);
        assertEquals(expected, result, description);
    }

    @Test
    void equalsSelfReference() {
        // Populate map first
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        assertTrue(trackingMap.equals(trackingMap), "Passing the same TrackingMap instance (self‑reference) must return true.");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("differentEntriesProvider")
    void equalsMapDifferentEntries(Object other, boolean expected, String description) {
        // Populate trackingMap with a=1, b=2
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        boolean result = trackingMap.equals(other);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("emptyMapProvider")
    void equalsEmptyMapCases(Map<String, Integer> other, boolean expected, String description) {
        // trackingMap is empty by default
        boolean result = trackingMap.equals(other);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("nullKeyValueProvider")
    void equalsNullKeyValueHandling(TrackingMap<String, Integer> mapUnderTest, Map<String, Integer> other, boolean expected, String description) {
        boolean result = mapUnderTest.equals(other);
        assertEquals(expected, result, description);
    }

    @Test
    void equalsDifferentMapImplementation() {
        // Populate trackingMap with a=1, b=2
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        // TreeMap with same entries
        Map<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("a", 1);
        treeMap.put("b", 2);
        assertTrue(trackingMap.equals(treeMap), "Equality must work across different Map implementations (TreeMap vs internal HashMap) when entries match.");
    }

    @Test
    void equalsValueEqualsThrowsException() {
        // internal map contains a value whose equals throws
        trackingMap.put("k", new BadEqualsObject());
        Map<String, Object> other = new HashMap<>();
        // the other map's value is harmless
        other.put("k", "normal");
        // When TrackingMap.equals(other) delegates to internalMap.equals(other),
        // the iteration will call BadEqualsObject.equals(...), which throws.
        assertThrows(IllegalStateException.class, () -> trackingMap.equals(other), "The exception from the value's equals() should propagate.");
    }

    @Test
    void equalsInternalMapEqualsThrowsException() throws Exception {
        // Replace the private internalMap field via reflection
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(trackingMap, new EqualsThrowingMap<>());
        // Any map argument should cause the exception to bubble up
        Map<String, Integer> other = Collections.emptyMap();
        assertThrows(IllegalArgumentException.class, () -> trackingMap.equals(other), "Exception thrown by internalMap.equals() must propagate.");
    }

    @Test
    void equalsOtherMapEntrySetIteratorThrows() {
        Map<String, Integer> badMap = new BadEntrySetMap<>();
        // trackingMap is empty; internalMap.equals(badMap) will iterate over badMap.entrySet()
        assertThrows(ConcurrentModificationException.class, () -> trackingMap.equals(badMap), "Exception from other map's entrySet iterator must propagate.");
    }
}
