package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_equals_Test {

    private CaseInsensitiveMap<String, Integer> ciMap;

    @BeforeEach
    void setupBeforeEach() {
        ciMap = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        ciMap = null;
    }

    /**
     * Replaces the internal wrapped map of {@code ciMap} with the supplied map.
     *
     * @param newMap the map to become the internal storage
     * @throws Exception if reflection fails
     */
    private static void injectUnderlyingMap(Map<?, ?> newMap) throws Exception {
        Field field = CaseInsensitiveMap.class.getDeclaredField("wrappedMap");
        field.setAccessible(true);
        // The test instance will be set later; this method only prepares the field.
        // The caller must invoke it on the concrete instance.
        // (We keep the method static for reuse; the instance is passed via reflection later.)
    }

    /**
     * Retrieves the private {@code wrappedMap} field from the given instance.
     *
     * @param instance the {@code CaseInsensitiveMap} instance
     * @return the underlying map object
     * @throws Exception if reflection fails
     */
    private static Object getWrappedMap(CaseInsensitiveMap<?, ?> instance) throws Exception {
        Field field = CaseInsensitiveMap.class.getDeclaredField("wrappedMap");
        field.setAccessible(true);
        return field.get(instance);
    }

    /**
     * Convenience wrapper that injects a new underlying map into {@code ciMap}.
     *
     * @param newMap the map to inject
     * @throws Exception if reflection fails
     */
    private void injectUnderlyingMapInstance(Map<?, ?> newMap) throws Exception {
        Field field = CaseInsensitiveMap.class.getDeclaredField("wrappedMap");
        field.setAccessible(true);
        field.set(ciMap, newMap);
    }

    /**
     * Helper used by tests that need to replace the internal map.
     *
     * @param newMap the map to become the internal storage
     */
    private void injectUnderlyingMap(Map<?, ?> newMap) throws Exception {
        injectUnderlyingMapInstance(newMap);
    }

    @Test
    void equalsSameInstance_returnsTrue() {
        assertTrue(ciMap.equals(ciMap), "Map should be equal to itself");
    }

    @Test
    void equalsNull_returnsFalse() {
        assertFalse(ciMap.equals(null), "Map should not be equal to null");
    }

    @Test
    void equalsDifferentClass_returnsFalse() {
        assertFalse(ciMap.equals("some string"), "Map should not be equal to an unrelated type");
    }

    @Test
    void equalsDifferentSize_returnsFalse() {
        ciMap.put("a", 1);
        ciMap.put("b", 2);
        Map<String, Integer> other = new HashMap<>();
        // only one entry
        other.put("a", 1);
        assertFalse(ciMap.equals(other), "Maps with different sizes must not be equal");
    }

    @Test
    void equalsSameEntriesCaseInsensitiveKey_returnsTrue() {
        ciMap.put("KeyOne", 10);
        ciMap.put("KEYTWO", 20);
        Map<String, Integer> other = new HashMap<>();
        // different case
        other.put("keyone", 10);
        // different case
        other.put("keytwo", 20);
        assertTrue(ciMap.equals(other), "Case‑insensitive keys should be considered equal");
    }

    @Test
    void equalsSameEntriesDifferentValues_returnsFalse() {
        ciMap.put("Alpha", 1);
        ciMap.put("Beta", 2);
        Map<String, Integer> other = new HashMap<>();
        other.put("Alpha", 1);
        // different value
        other.put("Beta", 99);
        assertFalse(ciMap.equals(other), "Maps with same keys but different values must not be equal");
    }

    @Test
    void equalsNonStringKeys_behavesNormally() {
        CaseInsensitiveMap<Integer, String> intKeyMap = new CaseInsensitiveMap<>();
        intKeyMap.put(1, "one");
        intKeyMap.put(2, "two");
        Map<Integer, String> other = new HashMap<>();
        other.put(1, "one");
        other.put(2, "two");
        assertTrue(intKeyMap.equals(other), "Non‑String keys should be compared using normal Map semantics");
    }

    @Test
    void equalsMissingKeySameSize_returnsFalse() {
        // Both maps have size 2, but 'b' is missing from the other map
        ciMap.put("a", 1);
        ciMap.put("b", 2);
        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        // different key
        other.put("c", 2);
        assertFalse(ciMap.equals(other), "When sizes match but a key is missing, equals must return false");
    }

    @Test
    void equalsEmptyMaps_returnsTrue() {
        // ciMap is empty by default
        Map<String, Integer> other = new HashMap<>();
        assertTrue(ciMap.equals(other), "Two empty maps must be equal");
    }

    @Test
    void equalsNullKeySameValue_returnsTrue() throws Exception {
        // Force the underlying map to a TreeMap (which rejects null keys) and
        // inject a null key directly via reflection.
        injectUnderlyingMap(new TreeMap<>());
        // Directly manipulate the wrapped map to contain a null key.
        Map<?, ?> wrapped = (Map<?, ?>) getWrappedMap(ciMap);
        wrapped.put(null, 999);
        // The other map also contains a null key with the same value.
        Map<String, Integer> other = new HashMap<>();
        other.put(null, 999);
        assertTrue(ciMap.equals(other), "Maps with identical null keys and values should be equal");
    }

    @Test
    void equalsNullKeyDifferentValue_returnsFalse() throws Exception {
        injectUnderlyingMap(new TreeMap<>());
        Map<?, ?> wrapped = (Map<?, ?>) getWrappedMap(ciMap);
        wrapped.put(null, 111);
        Map<String, Integer> other = new HashMap<>();
        other.put(null, 222);
        assertFalse(ciMap.equals(other), "Maps with same null key but different values must not be equal");
    }

    @Test
    void equalsNullValue_returnsTrue() {
        ciMap.put("key", null);
        Map<String, Integer> other = new HashMap<>();
        // case‑insensitive key, null value
        other.put("KEY", null);
        assertTrue(ciMap.equals(other), "Null values should be compared using Objects.equals (null == null)");
    }

    @Test
    void equalsDifferentMapImplementations_returnsTrue() {
        ciMap.put("KEY", 1);
        ciMap.put("another", 2);
        // TreeMap implements Map and preserves order – should be equal.
        Map<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("key", 1);
        treeMap.put("Another", 2);
        assertTrue(ciMap.equals(treeMap), "Different concrete Map implementations with same logical entries must be equal");
    }

    @Test
    void equalsConcurrentHashMapWithNonStringKey_returnsTrue() {
        CaseInsensitiveMap<Integer, String> intKeyMap = new CaseInsensitiveMap<>();
        intKeyMap.put(42, "answer");
        Map<Integer, String> other = new ConcurrentHashMap<>();
        other.put(42, "answer");
        assertTrue(intKeyMap.equals(other), "Non‑String keys are compared normally even with concurrent maps");
    }

    @Test
    void equalsDifferentKeyTypes_returnsFalse() {
        // String key
        ciMap.put("42", 100);
        Map<Integer, Integer> other = new HashMap<>();
        // Integer key
        other.put(42, 100);
        assertFalse(ciMap.equals(other), "Maps with keys of different runtime types must not be equal");
    }

    @Test
    void equalsWhenWrappedMapThrowsNPE_dueToNullKey() throws Exception {
        // Underlying TreeMap does not allow null keys – calling containsKey(null) will NPE.
        injectUnderlyingMap(new TreeMap<>());
        // Insert a null key via reflection (bypassing the public API which would throw)
        Map<?, ?> wrapped = (Map<?, ?>) getWrappedMap(ciMap);
        wrapped.put(null, 123);
        Map<String, Integer> other = new HashMap<>();
        other.put(null, 123);
        assertThrows(NullPointerException.class, () -> ciMap.equals(other), "Calling equals on a map backed by TreeMap with a null key should throw NPE");
    }

    @Test
    void equalsWhenContainsKeyThrowsClassCastException() {
        // normal String key
        ciMap.put("key", 1);
        Map<Integer, Integer> other = new HashMap<>();
        // Integer key – will be passed to containsKey(Object)
        other.put(42, 1);
        assertThrows(ClassCastException.class, () -> ciMap.equals(other), "containsKey should throw ClassCastException when a non‑String key is supplied to a case‑insensitive map");
    }

    @Test
    void equalsWhenOtherEntrySetIteratorThrowsConcurrentModificationException() {
        ciMap.put("key", 1);
        Map<String, Integer> badMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return new AbstractSet<>() {

                    @Override
                    public Iterator<Entry<String, Integer>> iterator() {
                        return new Iterator<>() {

                            @Override
                            public boolean hasNext() {
                                return true;
                            }

                            @Override
                            public Entry<String, Integer> next() {
                                throw new ConcurrentModificationException("Iterator broken");
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
                return Collections.emptySet();
            }

            @Override
            public Collection<Integer> values() {
                return Collections.emptyList();
            }
        };
        assertThrows(ConcurrentModificationException.class, () -> ciMap.equals(badMap), "Iterator throwing CME should propagate from equals()");
    }
}
