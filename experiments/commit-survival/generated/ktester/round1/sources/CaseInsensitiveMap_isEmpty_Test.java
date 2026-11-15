package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_isEmpty_Test {

    private CaseInsensitiveMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // GIVEN: a fresh CaseInsensitiveMap instance for each test
        map = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to ensure no leftover state (not strictly required)
        map.clear();
    }

    /**
     * Utility to set the private {@code map} field via reflection.
     * Used when a constructor that accepts a Map is not available.
     */
    private static <K, V> void setUnderlyingMap(CaseInsensitiveMap<K, V> wrapper, Map<K, V> underlying) {
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(wrapper, underlying);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void isEmpty_returnsTrueWhenMapHasNoEntries() {
        // WHEN: checking emptiness on a newly created map
        boolean result = map.isEmpty();
        // THEN: the map should be reported as empty
        assertTrue(result, "Expected isEmpty() to return true for an empty map");
    }

    @Test
    void isEmpty_returnsFalseWhenMapContainsEntries() {
        // GIVEN: a map with at least one entry
        map.put("KeyOne", 1);
        // WHEN: checking emptiness
        boolean result = map.isEmpty();
        // THEN: the map should not be reported as empty
        assertFalse(result, "Expected isEmpty() to return false when the map contains entries");
    }

    @Test
    void isEmpty_isCaseInsensitiveForStringKeys() {
        // GIVEN: a map with a key in mixed case
        map.put("TeSt", 42);
        // WHEN: checking emptiness after adding an entry
        boolean result = map.isEmpty();
        // THEN: the map should not be empty regardless of key case
        assertFalse(result, "Expected isEmpty() to return false after adding a case‑insensitive key");
    }

    @Test
    void isEmpty_returnsTrueAfterClearIncludingNullValue() {
        // GIVEN: map with a null value entry
        map.put("A", null);
        assertFalse(map.isEmpty(), "Pre‑condition: map should not be empty after put");
        // WHEN: clear the map
        map.clear();
        // THEN: map reports empty
        assertTrue(map.isEmpty(), "Expected isEmpty() to return true after clear");
    }

    @Test
    void isEmpty_reflectsPrepopulatedWrappedMap() {
        // GIVEN: underlying map already contains an entry
        Map<String, Integer> underlying = new HashMap<>(Map.of("x", 1));
        CaseInsensitiveMap<String, Integer> wrapped = new CaseInsensitiveMap<>(underlying);
        // THEN: isEmpty reflects underlying state
        assertFalse(wrapped.isEmpty(), "Expected isEmpty() to be false when underlying map is pre‑populated");
    }

    @Test
    void isEmpty_reflectsClearedWrappedMap() {
        // GIVEN: wrapper around a pre‑populated map
        Map<String, Integer> underlying = new HashMap<>(Map.of("x", 1));
        CaseInsensitiveMap<String, Integer> wrapped = new CaseInsensitiveMap<>(underlying);
        // WHEN: clear via the wrapped map reference
        wrapped.getWrappedMap().clear();
        // THEN: wrapper sees an empty map
        assertTrue(wrapped.isEmpty(), "Expected isEmpty() to be true after clearing underlying map");
    }

    @Test
    void isEmpty_afterCaseInsensitiveRemoval() {
        // GIVEN: entry added with mixed‑case key
        map.put("Key", 100);
        assertFalse(map.isEmpty());
        // WHEN: remove using a different case
        map.remove("key");
        // THEN: map becomes empty
        assertTrue(map.isEmpty(), "Expected isEmpty() to be true after case‑insensitive removal");
    }

    @Test
    void isEmpty_nullKeyHandling() {
        // GIVEN: put entry with null key
        map.put(null, 5);
        assertFalse(map.isEmpty(), "Map should not be empty after inserting null key");
        // WHEN: remove the null key
        map.remove(null);
        // THEN: map is empty again
        assertTrue(map.isEmpty(), "Expected isEmpty() to be true after removing null key");
    }

    @Test
    void isEmpty_reflectsExternalModificationViaWrappedMap() {
        // GIVEN: wrapper around a fresh HashMap
        CaseInsensitiveMap<String, Integer> wrapped = new CaseInsensitiveMap<>(new HashMap<>());
        // WHEN: add entry directly to underlying map
        wrapped.getWrappedMap().put("external", 42);
        // THEN: wrapper sees the entry
        assertFalse(wrapped.isEmpty(), "Expected isEmpty() to be false after external addition");
        // WHEN: remove entry directly from underlying map
        wrapped.getWrappedMap().remove("external");
        // THEN: wrapper sees empty map
        assertTrue(wrapped.isEmpty(), "Expected isEmpty() to be true after external removal");
    }

    @Test
    void isEmpty_multipleEntriesPartialRemoval() {
        // GIVEN: three distinct entries
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", 3);
        assertFalse(map.isEmpty());
        // WHEN: remove two entries using different cases
        map.remove("A");
        // case‑insensitive removal
        map.remove("b");
        // THEN: one entry remains
        assertFalse(map.isEmpty(), "Map should not be empty after removing only two of three entries");
        // WHEN: remove the last entry with different case
        map.remove("c");
        // THEN: map becomes empty
        assertTrue(map.isEmpty(), "Expected isEmpty() to be true after removing all entries");
    }

    @Test
    void isEmpty_concurrentUnderlyingMap() {
        // GIVEN: wrapper around a pre‑populated ConcurrentHashMap
        Map<String, Integer> concurrent = new ConcurrentHashMap<>(Map.of("x", 10));
        CaseInsensitiveMap<String, Integer> wrapped = new CaseInsensitiveMap<>(concurrent);
        assertFalse(wrapped.isEmpty(), "Concurrent underlying map should make isEmpty() return false");
        // WHEN: clear via wrapped map
        wrapped.getWrappedMap().clear();
        // THEN: wrapper reports empty
        assertTrue(wrapped.isEmpty(), "Expected isEmpty() to be true after clearing concurrent underlying map");
    }

    @Test
    void isEmpty_nullUnderlyingMap_throwsNullPointerException() {
        // GIVEN: a CaseInsensitiveMap constructed with a null underlying map
        CaseInsensitiveMap<String, Integer> wrapped = new CaseInsensitiveMap<>(null);
        // WHEN/THEN: calling isEmpty() should throw NPE
        assertThrows(NullPointerException.class, wrapped::isEmpty, "Expected NullPointerException when underlying map is null");
    }

    @Test
    void isEmpty_underlyingMapThrowsUnsupportedOperationException() {
        // GIVEN: custom map whose isEmpty() always throws UnsupportedOperationException
        Map<String, Integer> badMap = new Map<>() {

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                throw new UnsupportedOperationException("unsupported");
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
            public Integer get(Object key) {
                return null;
            }

            @Override
            public Integer put(String key, Integer value) {
                return null;
            }

            @Override
            public Integer remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends String, ? extends Integer> m) {
            }

            @Override
            public void clear() {
            }

            @Override
            public Set<String> keySet() {
                return Set.of();
            }

            @Override
            public Collection<Integer> values() {
                return List.of();
            }

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return Set.of();
            }
        };
        CaseInsensitiveMap<String, Integer> wrapped = new CaseInsensitiveMap<>(badMap);
        // WHEN/THEN: isEmpty propagates the exception
        assertThrows(UnsupportedOperationException.class, wrapped::isEmpty, "Expected UnsupportedOperationException to propagate from underlying map");
    }

    @Test
    void isEmpty_underlyingMapThrowsRuntimeException() {
        // GIVEN: custom map whose isEmpty() throws a generic RuntimeException
        Map<String, Integer> badMap = new Map<>() {

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                throw new RuntimeException("boom");
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
            public Integer get(Object key) {
                return null;
            }

            @Override
            public Integer put(String key, Integer value) {
                return null;
            }

            @Override
            public Integer remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends String, ? extends Integer> m) {
            }

            @Override
            public void clear() {
            }

            @Override
            public Set<String> keySet() {
                return Set.of();
            }

            @Override
            public Collection<Integer> values() {
                return List.of();
            }

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return Set.of();
            }
        };
        CaseInsensitiveMap<String, Integer> wrapped = new CaseInsensitiveMap<>(badMap);
        // WHEN/THEN: isEmpty propagates the RuntimeException
        assertThrows(RuntimeException.class, wrapped::isEmpty, "Expected RuntimeException to propagate from underlying map");
    }
}
