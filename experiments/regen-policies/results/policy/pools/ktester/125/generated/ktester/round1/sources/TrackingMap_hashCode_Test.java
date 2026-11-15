package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_hashCode_Test {

    private TrackingMap<String, Integer> trackingMap;

    private Map<String, Integer> backingMap;

    /**
     * Simple mutable value whose hashCode depends on an internal integer state.
     */
    private static class MutableValue {

        private int state;

        MutableValue(int state) {
            this.state = state;
        }

        void setState(int newState) {
            this.state = newState;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(state);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MutableValue))
                return false;
            return state == ((MutableValue) o).state;
        }
    }

    /**
     * Map implementation that always returns a constant hash code (42) regardless of its contents.
     */
    private static class ConstantHashMap<K, V> implements Map<K, V> {

        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<K> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<V> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return delegate.entrySet();
        }

        @Override
        public int hashCode() {
            // constant value as required by the test case
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            return delegate.equals(o);
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
    void hashCodeDelegatesToInternalMap_usingMock() {
        // Arrange
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.hashCode()).thenReturn(42);
        TrackingMap<String, Integer> mapUnderTest = new TrackingMap<>(mockMap);
        // Act
        int hash = mapUnderTest.hashCode();
        // Assert
        assertEquals(42, hash, "TrackingMap.hashCode() should return the hashCode of the wrapped map");
        verify(mockMap, times(1)).hashCode();
    }

    @Test
    void hashCodeReflectsBackingMapContents() {
        // Arrange
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        int expectedHash = backingMap.hashCode();
        // Act & Assert
        assertEquals(expectedHash, trackingMap.hashCode(), "hashCode should match the hashCode of the internal map after initial population");
        // Modify the backing map and verify hashCode changes accordingly
        backingMap.put("c", 3);
        expectedHash = backingMap.hashCode();
        assertEquals(expectedHash, trackingMap.hashCode(), "hashCode should reflect changes made to the internal map");
    }

    @Test
    void hashCodeEqualityForMapsWithEqualEntries() {
        // Arrange
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("x", 10);
        map1.put("y", 20);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("x", 10);
        map2.put("y", 20);
        TrackingMap<String, Integer> tm1 = new TrackingMap<>(map1);
        TrackingMap<String, Integer> tm2 = new TrackingMap<>(map2);
        // Act & Assert
        assertEquals(map1.hashCode(), map2.hashCode(), "Two separate maps with equal entries must have equal hashCodes");
        assertEquals(tm1.hashCode(), tm2.hashCode(), "TrackingMap instances wrapping equal maps must produce equal hashCodes");
    }

    @Test
    void hashCodeForEmptyMap() {
        // An empty HashMap has a well‑defined hashCode (0)
        assertTrue(backingMap.isEmpty(), "Backing map should be empty at test start");
        assertEquals(backingMap.hashCode(), trackingMap.hashCode(), "hashCode of an empty TrackingMap should match the empty backing map's hashCode");
    }

    @Test
    void hashCodeWithSingleEntry() {
        // Arrange
        backingMap.put("key1", 1);
        int expected = backingMap.hashCode();
        // Act & Assert
        assertEquals(expected, trackingMap.hashCode(), "hashCode must reflect a map containing a single entry");
    }

    @Test
    void hashCodeWithMultipleEntries() {
        // Arrange
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        backingMap.put("c", 3);
        int expected = backingMap.hashCode();
        // Act & Assert
        assertEquals(expected, trackingMap.hashCode(), "hashCode should equal the hash code computed by the underlying map implementation");
    }

    @Test
    void hashCodeAfterRemoval() {
        // Arrange
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        backingMap.put("c", 3);
        // map now {"a"=1, "c"=3}
        backingMap.remove("b");
        int expected = backingMap.hashCode();
        // Act & Assert
        assertEquals(expected, trackingMap.hashCode(), "After removing an entry, hashCode must change accordingly");
    }

    @Test
    void hashCodeAfterPutAll() {
        // Arrange
        Map<String, Integer> toPutAll = new HashMap<>();
        toPutAll.put("x", 100);
        toPutAll.put("y", 200);
        trackingMap.putAll(toPutAll);
        int expected = backingMap.hashCode();
        // Act & Assert
        assertEquals(expected, trackingMap.hashCode(), "hashCode must reflect the new combined content after putAll");
    }

    @Test
    void hashCodeWithNullKeyAndValue() {
        // Arrange
        backingMap.put(null, null);
        int expected = backingMap.hashCode();
        // Act & Assert
        assertEquals(expected, trackingMap.hashCode(), "If the underlying map permits null keys/values, hashCode should delegate correctly");
    }

    @Test
    void hashCodeStabilityAcrossRepeatedCalls() {
        // Arrange
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        int first = trackingMap.hashCode();
        // Act & Assert
        assertEquals(first, trackingMap.hashCode(), "Repeated calls without mutation must return identical values");
        assertEquals(first, trackingMap.hashCode(), "Repeated calls without mutation must return identical values");
    }

    @Test
    void hashCodeAfterClear() {
        // Arrange
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        // should clear internal map
        trackingMap.clear();
        // both are empty now
        int expected = backingMap.hashCode();
        // Act & Assert
        assertEquals(expected, trackingMap.hashCode(), "After clear(), hashCode should match that of an empty underlying map");
    }

    @Test
    void hashCodeWithLinkedHashMapOrderIndependence() {
        // Arrange
        LinkedHashMap<String, Integer> linked = new LinkedHashMap<>();
        linked.put("first", 1);
        linked.put("second", 2);
        linked.put("third", 3);
        TrackingMap<String, Integer> tm = new TrackingMap<>(linked);
        int expected = linked.hashCode();
        // Act & Assert
        assertEquals(expected, tm.hashCode(), "hashCode must be independent of insertion order (LinkedHashMap's hashCode is order‑independent)");
    }

    @Test
    void hashCodeWithCustomMapConstantHash() {
        // Arrange
        Map<String, Integer> constantMap = new ConstantHashMap<>();
        constantMap.put("a", 1);
        constantMap.put("b", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(constantMap);
        // Act
        int hash = tm.hashCode();
        // Assert
        assertEquals(42, hash, "TrackingMap should return the constant hash code supplied by the custom map");
    }

    @Test
    void hashCodeAfterMutatingStoredMutableValue() {
        // Arrange
        MutableValue mutable = new MutableValue(10);
        trackingMap.put("key", mutable);
        int hashBefore = trackingMap.hashCode();
        // Mutate the value (affects its hashCode)
        mutable.setState(20);
        int hashAfter = trackingMap.hashCode();
        // Act & Assert
        assertNotEquals(hashBefore, hashAfter, "hashCode must change after the mutable value's state changes");
        // Verify that the new hash matches the underlying map's hashCode
        assertEquals(backingMap.hashCode(), hashAfter, "hashCode after mutation should equal the underlying map's hashCode");
    }

    @Test
    void hashCodeWithLargeNumberOfEntries() {
        // Arrange
        for (int i = 0; i < 1000; i++) {
            backingMap.put("k" + i, i);
        }
        int expected = backingMap.hashCode();
        // Act & Assert
        assertEquals(expected, trackingMap.hashCode(), "hashCode should be consistent with internalMap.hashCode() for 1000 entries");
    }

    @Test
    void hashCodeWithNullKeyOnly() {
        // Arrange
        backingMap.put(null, 123);
        int expected = backingMap.hashCode();
        // Act & Assert
        assertEquals(expected, trackingMap.hashCode(), "hashCode must match that of a map containing a single null key");
    }

    @Test
    void hashCodeWithNullValueOnly() {
        // Arrange
        backingMap.put("key", null);
        int expected = backingMap.hashCode();
        // Act & Assert
        assertEquals(expected, trackingMap.hashCode(), "hashCode must match that of a map containing a non‑null key mapped to null");
    }

    @Test
    void hashCodeEqualityAcrossDifferentMapImplementations() {
        // Arrange
        Map<String, Integer> hashMap = new HashMap<>();
        Map<String, Integer> treeMap = new TreeMap<>();
        hashMap.put("a", 1);
        hashMap.put("b", 2);
        treeMap.put("a", 1);
        treeMap.put("b", 2);
        TrackingMap<String, Integer> tmHash = new TrackingMap<>(hashMap);
        TrackingMap<String, Integer> tmTree = new TrackingMap<>(treeMap);
        // Act & Assert
        assertEquals(tmHash.hashCode(), tmTree.hashCode(), "Hash codes must be identical for different internal map implementations with the same entries");
    }

    @Test
    void hashCodeChangesAfterEachMutation() {
        // Arrange
        assertEquals(backingMap.hashCode(), trackingMap.hashCode(), "Initial hashCode on empty map");
        // Put A
        trackingMap.put("A", 1);
        int afterA = trackingMap.hashCode();
        assertNotEquals(backingMap.hashCode(), afterA, "HashCode should change after adding entry A");
        // Put B
        trackingMap.put("B", 2);
        int afterB = trackingMap.hashCode();
        assertNotEquals(afterA, afterB, "HashCode should change after adding entry B");
        // Remove A
        trackingMap.remove("A");
        int afterRemoveA = trackingMap.hashCode();
        // Build expected map manually
        Map<String, Integer> expectedMap = new HashMap<>();
        expectedMap.put("B", 2);
        assertEquals(expectedMap.hashCode(), afterRemoveA, "HashCode after removing A should match hashCode of map containing only B");
    }

    @Test
    void hashCodeWhenInternalMapIsNull_throwsNPE() throws Exception {
        // Arrange: create a normal TrackingMap then null out its internalMap via reflection
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(tm, null);
        // Act & Assert
        assertThrows(NullPointerException.class, tm::hashCode, "Calling hashCode() when internalMap is null should throw NullPointerException");
    }
}
