package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class TrackingMap_entrySet_Test {

    /**
     * The map that will be wrapped by {@link TrackingMap}.
     */
    private Map<String, Integer> backingMap;

    /**
     * The {@link TrackingMap} instance under test.
     */
    private TrackingMap<String, Integer> trackingMap;

    /**
     * A mock map used to verify delegation without relying on a concrete implementation.
     */
    @Mock
    private Map<String, Integer> mockMap;

    /**
     * Custom runtime exception used for the next test.
     */
    static class CustomRuntimeException extends RuntimeException {

        CustomRuntimeException(String msg) {
            super(msg);
        }
    }

    /**
     * A Map implementation whose {@code entrySet()} deliberately throws a {@link CustomRuntimeException}.
     */
    static class ThrowingMap<K, V> implements Map<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new CustomRuntimeException("Deliberate exception from entrySet()");
        }

        // --- Minimal stub implementations for the remaining Map methods ---
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
        // Initialise a real backing map for the majority of tests.
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help the garbage collector; not strictly required.
        backingMap = null;
        trackingMap = null;
    }

    /**
     * Verify that {@code entrySet()} on an empty map returns an empty set.
     */
    @Test
    void entrySet_EmptyMap_ReturnsEmptySet() {
        // Given: a freshly constructed TrackingMap (already set up in @BeforeEach)
        // When
        Set<Map.Entry<String, Integer>> result = trackingMap.entrySet();
        // Then
        assertNotNull(result, "entrySet() should never return null");
        assertTrue(result.isEmpty(), "entrySet() of an empty map should be empty");
    }

    /**
     * Verify that {@code entrySet()} returns the exact same Set instance
     * that the wrapped map provides.
     */
    @Test
    void entrySet_NonEmptyMap_ReturnsSameSetInstance() {
        // Given
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        Set<Map.Entry<String, Integer>> expectedSet = backingMap.entrySet();
        // When
        Set<Map.Entry<String, Integer>> actualSet = trackingMap.entrySet();
        // Then
        assertSame(expectedSet, actualSet, "entrySet() should return the exact Set instance from the wrapped map");
        assertEquals(2, actualSet.size(), "The returned set should contain all map entries");
    }

    /**
     * Verify delegation when the wrapped map is a Mockito mock.
     * This ensures that {@code entrySet()} does not perform any extra processing.
     */
    @Test
    void entrySet_WithMockMap_DelegatesDirectly() {
        // Given
        // Configure the mock to return a specific Set instance.
        Set<Map.Entry<String, Integer>> mockSet = mock(Set.class);
        when(mockMap.entrySet()).thenReturn(mockSet);
        // Construct the TrackingMap with the mock.
        TrackingMap<String, Integer> mapWithMock = new TrackingMap<>(mockMap);
        // When
        Set<Map.Entry<String, Integer>> result = mapWithMock.entrySet();
        // Then
        assertSame(mockSet, result, "entrySet() should delegate directly to the wrapped map");
        // ensure the method was called exactly once
        verify(mockMap, times(1)).entrySet();
    }

    /**
     * Verify that {@code entrySet()} reflects removals performed via the Set view.
     */
    @Test
    void entrySet_RemoveViaSet_UpdatesUnderlyingMap() {
        // Populate the map with two entries.
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        Set<Map.Entry<String, Integer>> entrySet = trackingMap.entrySet();
        // Remove the entry for key "a" using the Set's remove method.
        Map.Entry<String, Integer> entryForA = null;
        for (Map.Entry<String, Integer> e : entrySet) {
            if ("a".equals(e.getKey())) {
                entryForA = e;
                break;
            }
        }
        assertNotNull(entryForA, "Entry for key 'a' must be present before removal");
        assertTrue(entrySet.remove(entryForA), "remove(entry) should return true");
        // The underlying map must now have size 1 and no entry for "a".
        assertEquals(1, trackingMap.size(), "Map size should be reduced after removal via entrySet");
        assertNull(trackingMap.get("a"), "Key 'a' should no longer be present in the map");
        assertTrue(trackingMap.containsKey("b"));
    }

    /**
     * Verify that modifying an entry's value via {@link Map.Entry#setValue}
     * updates the underlying map.
     */
    @Test
    void entrySet_ModifyEntryValue_UpdatesMap() {
        trackingMap.put("x", 10);
        Set<Map.Entry<String, Integer>> entrySet = trackingMap.entrySet();
        // Find the entry for key "x".
        Map.Entry<String, Integer> entry = entrySet.iterator().next();
        assertEquals("x", entry.getKey());
        // Change the value.
        entry.setValue(99);
        // The map must reflect the new value.
        assertEquals(99, trackingMap.get("x"));
    }

    /**
     * Verify that a newly added entry appears in a subsequent {@code entrySet()} call.
     */
    @Test
    void entrySet_AddEntryViaMap_SeenInEntrySet() {
        // Initially empty.
        assertTrue(trackingMap.entrySet().isEmpty());
        // Add a new entry.
        trackingMap.put("newKey", 123);
        // Retrieve a fresh entrySet view.
        Set<Map.Entry<String, Integer>> entrySet = trackingMap.entrySet();
        assertEquals(1, entrySet.size());
        Map.Entry<String, Integer> e = entrySet.iterator().next();
        assertEquals("newKey", e.getKey());
        assertEquals(123, e.getValue());
    }

    /**
     * Verify that two consecutive calls to {@code entrySet()} return the same Set instance.
     */
    @Test
    void entrySet_TwoCalls_ReturnSameInstance() {
        Set<Map.Entry<String, Integer>> first = trackingMap.entrySet();
        Set<Map.Entry<String, Integer>> second = trackingMap.entrySet();
        assertSame(first, second, "Consecutive entrySet() calls should return the same Set instance");
    }

    /**
     * Verify that removing a key directly from the map is reflected in the live entrySet view.
     */
    @Test
    void entrySet_ReflectsMapRemove() {
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        Set<Map.Entry<String, Integer>> entrySet = trackingMap.entrySet();
        // Remove key "a" via the map API.
        trackingMap.remove("a");
        // The entrySet should now have size 1 and not contain the removed entry.
        assertEquals(1, entrySet.size(), "entrySet size should shrink after map.remove()");
        assertFalse(entrySet.stream().anyMatch(e -> "a".equals(e.getKey())), "entrySet should not contain the removed key");
    }

    /**
     * Verify that {@code clear()} empties both the map and the entrySet view.
     */
    @Test
    void entrySet_ReflectsClear() {
        trackingMap.put("k1", 10);
        trackingMap.put("k2", 20);
        assertFalse(trackingMap.entrySet().isEmpty());
        trackingMap.clear();
        assertTrue(trackingMap.isEmpty(), "Map should be empty after clear()");
        assertTrue(trackingMap.entrySet().isEmpty(), "entrySet should be empty after clear()");
        assertEquals(0, trackingMap.entrySet().size());
    }

    /**
     * Simulate a corrupted TrackingMap where the internal map reference is {@code null}.
     * The method should throw a {@link NullPointerException}.
     */
    @Test
    void entrySet_InternalMapIsNull_ThrowsNPE() throws Exception {
        // Use reflection to set the private field 'internalMap' to null.
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(trackingMap, null);
        // Expect NPE when calling entrySet().
        assertThrows(NullPointerException.class, trackingMap::entrySet, "Calling entrySet() when internalMap is null should throw NullPointerException");
    }

    /**
     * Verify that a custom wrapped map that throws an exception from {@code entrySet()}
     * propagates that exception through {@link TrackingMap#entrySet()}.
     */
    @Test
    void entrySet_WrappedMapThrowsCustomException_Propagates() throws Exception {
        // Create a ThrowingMap instance.
        ThrowingMap<String, Integer> throwingMap = new ThrowingMap<>();
        // Inject it into the TrackingMap via reflection.
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(trackingMap, throwingMap);
        // The call must propagate the custom runtime exception.
        CustomRuntimeException ex = assertThrows(CustomRuntimeException.class, trackingMap::entrySet, "Exception from the wrapped map's entrySet() should be propagated");
        assertEquals("Deliberate exception from entrySet()", ex.getMessage());
    }
}
