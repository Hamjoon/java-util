package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_keySet_Test {

    private Map<String, Integer> backingMap;

    private TrackingMap<String, Integer> trackingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh backing map and TrackingMap for each test
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy
        backingMap.clear();
        // trackingMap will be eligible for GC after the test method finishes
    }

    // Example of a parameter source – currently not used but kept for extensibility
    static Stream<Arguments> emptyAndNonEmptyProvider() {
        return Stream.of(Arguments.of(Collections.emptyMap(), Collections.emptySet()), Arguments.of(Map.of("a", 1, "b", 2), Set.of("a", "b")));
    }

    @Test
    void keySetReturnsAllKeys() {
        trackingMap.put("alpha", 1);
        trackingMap.put("beta", 2);
        Set<String> keys = trackingMap.keySet();
        assertEquals(2, keys.size(), "keySet should contain exactly two keys");
        assertTrue(keys.containsAll(Arrays.asList("alpha", "beta")), "keySet should contain the inserted keys");
    }

    @Test
    void keySetReflectsChangesInBackingMap() {
        // Initial entry
        trackingMap.put("one", 100);
        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.contains("one"));
        // Add a new entry directly to the backing map
        backingMap.put("two", 200);
        assertTrue(keys.contains("two"), "keySet should reflect additions to the backing map");
        // Remove an entry directly from the backing map
        backingMap.remove("one");
        assertFalse(keys.contains("one"), "keySet should reflect removals from the backing map");
    }

    @Test
    void keySetIsMutableThroughSetRemove() {
        trackingMap.put("removeMe", 42);
        Set<String> keys = trackingMap.keySet();
        // Remove via the Set view
        boolean removed = keys.remove("removeMe");
        assertTrue(removed, "Removing via keySet should succeed");
        assertFalse(trackingMap.containsKey("removeMe"), "Underlying map should no longer contain the key");
        assertEquals(0, trackingMap.size(), "TrackingMap should be empty after removal");
    }

    @Test
    void keySetOnEmptyMapIsEmpty() {
        Set<String> keys = trackingMap.keySet();
        assertNotNull(keys, "keySet should never be null");
        assertTrue(keys.isEmpty(), "keySet of an empty TrackingMap should be empty");
    }

    @Test
    void keySetReturnsLiveViewAfterExpungeUnused() {
        // Populate map and mark one key as accessed
        trackingMap.put("used", 1);
        trackingMap.put("unused", 2);
        // mark as used
        trackingMap.get("used");
        // should remove "unused"
        trackingMap.expungeUnused();
        Set<String> keys = trackingMap.keySet();
        assertEquals(1, keys.size());
        assertTrue(keys.contains("used"));
        assertFalse(keys.contains("unused"));
    }

    @Test
    void emptyMapKeySetIsEmpty() {
        // Group: Empty Map
        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.isEmpty(), "When the wrapped map is empty, keySet() should return an empty Set.");
    }

    @Test
    void nonEmptyMapKeySetContainsAllKeys() {
        // Group: Non‑Empty Map
        trackingMap.put("alpha", 1);
        trackingMap.put("beta", 2);
        Set<String> keys = trackingMap.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.containsAll(Arrays.asList("alpha", "beta")), "keySet must contain exactly the keys \"alpha\" and \"beta\" (order not significant).");
    }

    @Test
    void keySetModificationRemovalEmptiesMap() {
        // Group: KeySet Modification – removal
        trackingMap.put("k", 10);
        Set<String> keys = trackingMap.keySet();
        assertTrue(keys.remove("k"));
        assertTrue(keys.isEmpty(), "Returned Set should be empty after removal.");
        assertTrue(trackingMap.isEmpty(), "Underlying TrackingMap should become empty.");
    }

    @Test
    void keySetAddUnsupportedOperation() {
        // Group: KeySet Modification – unsupported add
        Set<String> keys = trackingMap.keySet();
        assertThrows(UnsupportedOperationException.class, () -> keys.add("newKey"), "Calling add(Object) on the Set returned by keySet() should throw UnsupportedOperationException.");
    }

    @Test
    void underlyingMapModificationAfterKeySetRetrievalIsReflected() {
        // Group: Underlying Map Modification after keySet retrieval
        trackingMap.put("k1", 1);
        Set<String> keys = trackingMap.keySet();
        // Directly modify the backing map
        backingMap.put("k2", 2);
        assertTrue(keys.containsAll(Arrays.asList("k1", "k2")), "Previously obtained Set must reflect the new key \"k2\" in addition to the original \"k1\".");
    }

    @Test
    void keySetInstanceConsistency() {
        // Group: KeySet Instance Consistency
        Set<String> first = trackingMap.keySet();
        Set<String> second = trackingMap.keySet();
        assertSame(first, second, "Calling keySet() twice on the same TrackingMap should return the identical Set object.");
    }

    @Test
    void keySetIteratorRemovalEmptiesMap() {
        // Group: KeySet Iterator Removal
        trackingMap.put("x", 1);
        Set<String> keys = trackingMap.keySet();
        Iterator<String> it = keys.iterator();
        assertTrue(it.hasNext());
        String key = it.next();
        // should remove from underlying map
        it.remove();
        assertEquals("x", key);
        assertTrue(trackingMap.isEmpty(), "Underlying TrackingMap must become empty after iterator.remove().");
        assertEquals(0, keys.size(), "Set size must be 0 after iterator.remove().");
    }

    @Test
    void keySetClearOperationEmptiesMap() {
        // Group: KeySet Clear Operation
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        Set<String> keys = trackingMap.keySet();
        keys.clear();
        assertTrue(trackingMap.isEmpty(), "Calling clear() on the Set must clear the underlying TrackingMap.");
        assertTrue(keys.isEmpty(), "Set must be empty after clear().");
    }

    @Test
    void keySetContainsPresentKey() {
        // Group: KeySet Contains Check – present key
        trackingMap.put("alpha", 100);
        assertTrue(trackingMap.keySet().contains("alpha"), "After populating the TrackingMap with key \"alpha\", keySet().contains(\"alpha\") must return true.");
    }

    @Test
    void keySetContainsAbsentKey() {
        // Group: KeySet Contains Check – absent key
        trackingMap.put("beta", 200);
        assertFalse(trackingMap.keySet().contains("gamma"), "For a TrackingMap that does not contain key \"gamma\", keySet().contains(\"gamma\") must return false.");
    }

    @Test
    void keySetToArrayContent() {
        // Group: KeySet toArray Content
        trackingMap.put("x", 1);
        trackingMap.put("y", 2);
        trackingMap.put("z", 3);
        Object[] array = trackingMap.keySet().toArray();
        assertEquals(3, array.length);
        List<Object> list = Arrays.asList(array);
        assertTrue(list.containsAll(Arrays.asList("x", "y", "z")), "toArray() must contain exactly the keys \"x\", \"y\", \"z\" (order not significant).");
    }

    @Test
    void keySetReflectsPutAll() {
        // Group: KeySet Reflects putAll
        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        other.put("b", 2);
        other.put("c", 3);
        // obtain before putAll
        Set<String> keys = trackingMap.keySet();
        trackingMap.putAll(other);
        assertTrue(keys.containsAll(Arrays.asList("a", "b", "c")), "After invoking map.putAll, the previously obtained Set must now contain \"a\", \"b\", \"c\".");
    }

    @Test
    void keySetReflectsMapRemove() {
        // Group: KeySet Reflects map.remove
        trackingMap.put("alpha", 1);
        trackingMap.put("beta", 2);
        Set<String> keys = trackingMap.keySet();
        trackingMap.remove("beta");
        assertFalse(keys.contains("beta"), "Set must no longer contain \"beta\" after map.remove(\"beta\").");
        assertTrue(keys.contains("alpha"), "Set must still contain \"alpha\".");
    }

    @Test
    void keySetWithUnmodifiableUnderlyingMapRemovalThrows() {
        // Group: KeySet with Unmodifiable Underlying Map – removal
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
        TrackingMap<String, Integer> tm = new TrackingMap<>(unmodifiable);
        // Attempt to remove via keySet should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> tm.keySet().remove("anyKey"), "When wrapped around an unmodifiable map, keySet().remove should throw UnsupportedOperationException.");
    }

    @Test
    void internalMapNullThrowsNullPointerException() throws Exception {
        // Group: Exception Cases – internalMap is null
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(trackingMap, null);
        assertThrows(NullPointerException.class, () -> trackingMap.keySet(), "If internalMap is null, calling keySet() should throw NullPointerException.");
    }

    @Test
    void internalMapKeySetThrowsIllegalStateException() {
        // Group: Exception Cases – internalMap.keySet() throws IllegalStateException
        Map<String, Integer> faultyMap = new Map<>() {

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
                throw new IllegalStateException("forced");
            }

            @Override
            public Collection<Integer> values() {
                return null;
            }

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return null;
            }
        };
        TrackingMap<String, Integer> tm = new TrackingMap<>(faultyMap);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> tm.keySet(), "keySet() should propagate IllegalStateException from internalMap.keySet().");
        assertEquals("forced", ex.getMessage());
    }

    @Test
    void internalMapKeySetThrowsUnsupportedOperationException() {
        // Group: Exception Cases – internalMap.keySet() throws UnsupportedOperationException
        Map<String, Integer> faultyMap = new Map<>() {

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
                throw new UnsupportedOperationException("forced");
            }

            @Override
            public Collection<Integer> values() {
                return null;
            }

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return null;
            }
        };
        TrackingMap<String, Integer> tm = new TrackingMap<>(faultyMap);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> tm.keySet(), "keySet() should propagate UnsupportedOperationException from internalMap.keySet().");
        assertEquals("forced", ex.getMessage());
    }
}
