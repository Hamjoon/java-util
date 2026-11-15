package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class TrackingMap_keySet_Test {

    /**
     * The TrackingMap under test
     */
    private TrackingMap<String, Integer> trackingMap;

    /**
     * Mocked internal map that the TrackingMap delegates to
     */
    @Mock
    private Map<String, Integer> mockInternalMap;

    /**
     * A real map used for integration‑style verification
     */
    private Map<String, Integer> realMap;

    /**
     * A real TrackingMap that wraps the realMap
     */
    private TrackingMap<String, Integer> realTrackingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (needed because we use @Mock fields)
        MockitoAnnotations.openMocks(this);
        // ---------- Mock based scenario ----------
        // Prepare a deterministic keySet that the mock will return.
        Set<String> mockedKeySet = new HashSet<>(Arrays.asList("alpha", "beta", "gamma"));
        when(mockInternalMap.keySet()).thenReturn(mockedKeySet);
        // Construct the TrackingMap with the mocked internal map.
        trackingMap = new TrackingMap<>(mockInternalMap);
        // ---------- Real map scenario ----------
        // Use a real HashMap to verify that the returned Set is backed by the map.
        realMap = new HashMap<>();
        realMap.put("one", 1);
        realMap.put("two", 2);
        realTrackingMap = new TrackingMap<>(realMap);
    }

    /**
     * Verify that {@code keySet()} returns exactly the Set provided by the wrapped map.
     */
    @Test
    void keySet_ReturnsInternalMapKeySet() {
        Set<String> result = trackingMap.keySet();
        // The TrackingMap should delegate directly to the internal map.
        assertSame(mockInternalMap.keySet(), result, "keySet() should return the same Set instance as the internal map's keySet()");
        // Verify that the internal map's keySet() method was indeed invoked.
        verify(mockInternalMap, times(1)).keySet();
    }

    /**
     * Verify that the Set returned by {@code keySet()} is backed by the underlying map.
     * Adding an element to the Set should affect the map's size.
     */
    @Test
    void keySet_BackedByUnderlyingMap() {
        Set<String> keySet = realTrackingMap.keySet();
        // Initial state: map contains two entries.
        assertEquals(2, realMap.size());
        assertEquals(2, keySet.size());
        // Add a new key via the Set view.
        keySet.add("three");
        // The underlying map should now contain the new key with a null value.
        assertTrue(realMap.containsKey("three"));
        assertNull(realMap.get("three"));
        // Sizes should stay in sync.
        assertEquals(3, realMap.size());
        assertEquals(3, keySet.size());
    }

    /**
     * Verify that removing a key via the Set view removes the entry from the underlying map.
     */
    @Test
    void keySet_RemoveKeyReflectsInMap() {
        Set<String> keySet = realTrackingMap.keySet();
        // Ensure the key we are about to remove exists.
        assertTrue(keySet.contains("one"));
        assertTrue(realMap.containsKey("one"));
        // Remove through the Set view.
        boolean removed = keySet.remove("one");
        assertTrue(removed, "The key should be removed via the Set view");
        // The underlying map must no longer contain the key.
        assertFalse(realMap.containsKey("one"));
        assertFalse(keySet.contains("one"));
        // Size consistency check.
        assertEquals(1, realMap.size());
        assertEquals(1, keySet.size());
    }

    @Test
    void keySet_EmptyMapReturnsEmptySet() {
        TrackingMap<String, Integer> emptyMap = new TrackingMap<>(new HashMap<>());
        Set<String> ks = emptyMap.keySet();
        assertNotNull(ks, "keySet() should never return null");
        assertTrue(ks.isEmpty(), "An empty TrackingMap must yield an empty key set");
        assertEquals(0, ks.size());
    }

    @Test
    void keySet_PopulatedMapReturnsExactKeys() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("alpha", 1);
        tm.put("beta", 2);
        Set<String> ks = tm.keySet();
        assertEquals(2, ks.size());
        assertTrue(ks.containsAll(Arrays.asList("alpha", "beta")));
    }

    @Test
    void keySet_RemoveViaSetUpdatesMapAndSize() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("x", 10);
        tm.put("y", 20);
        Set<String> ks = tm.keySet();
        assertTrue(ks.remove("x"));
        assertFalse(tm.containsKey("x"));
        assertEquals(1, tm.size());
        assertEquals(1, ks.size());
    }

    @Test
    void keySet_AddOnReturnedSetThrowsUnsupportedOperationException() {
        // The Set view of a Map is not modifiable for addition.
        Set<String> ks = realTrackingMap.keySet();
        assertThrows(UnsupportedOperationException.class, () -> ks.add("newKey"));
    }

    @Test
    void keySet_ReturnsExactSameInstanceAsInternalMap() throws Exception {
        // Use reflection to obtain the internal map from the realTrackingMap.
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Integer> internal = (Map<String, Integer>) field.get(realTrackingMap);
        Set<String> internalKeySet = internal.keySet();
        Set<String> returnedKeySet = realTrackingMap.keySet();
        assertSame(internalKeySet, returnedKeySet, "keySet() must return the exact Set instance from the wrapped map");
    }

    @Test
    void keySet_LiveViewReflectsMapAdditions() {
        Set<String> ks = realTrackingMap.keySet();
        realTrackingMap.put("gamma", 3);
        assertTrue(ks.contains("gamma"));
        assertEquals(3, ks.size());
    }

    @Test
    void keySet_LiveViewReflectsClear() {
        Set<String> ks = realTrackingMap.keySet();
        realTrackingMap.clear();
        assertTrue(ks.isEmpty());
        assertEquals(0, ks.size());
    }

    @Test
    void keySet_RemoveNonexistentKeyReturnsFalseAndSizeUnchanged() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("a", 1);
        Set<String> ks = tm.keySet();
        assertFalse(ks.remove("nonexistent"));
        assertEquals(1, tm.size());
        assertEquals(1, ks.size());
    }

    @Test
    void keySet_NullKeyHandledCorrectly() {
        // HashMap permits null keys.
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put(null, 0);
        tm.put("notNull", 1);
        Set<String> ks = tm.keySet();
        assertTrue(ks.contains(null));
        assertTrue(ks.remove(null));
        assertFalse(tm.containsKey(null));
        assertEquals(1, tm.size());
    }

    @Test
    void keySet_IteratorRemoveAllEntries() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("a", 1);
        tm.put("b", 2);
        tm.put("c", 3);
        Set<String> ks = tm.keySet();
        Iterator<String> it = ks.iterator();
        while (it.hasNext()) {
            it.next();
            it.remove();
        }
        assertTrue(ks.isEmpty());
        assertTrue(tm.isEmpty());
    }

    @Test
    void keySet_IteratorConcurrentModificationThrows() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("a", 1);
        Set<String> ks = tm.keySet();
        Iterator<String> it = ks.iterator();
        // Structural modification outside iterator.
        tm.put("b", 2);
        assertThrows(ConcurrentModificationException.class, it::next);
    }

    @Test
    void keySet_RemoveAllBulkOperation() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("a", 1);
        tm.put("b", 2);
        tm.put("c", 3);
        Set<String> ks = tm.keySet();
        ks.removeAll(Arrays.asList("a", "c"));
        assertFalse(ks.contains("a"));
        assertFalse(ks.contains("c"));
        assertTrue(ks.contains("b"));
        assertEquals(1, tm.size());
    }

    @Test
    void keySet_RetainAllBulkOperation() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("only", 42);
        tm.put("discard1", 1);
        tm.put("discard2", 2);
        Set<String> ks = tm.keySet();
        ks.retainAll(Collections.singleton("only"));
        assertTrue(ks.contains("only"));
        assertEquals(1, ks.size());
        assertEquals(1, tm.size());
        assertTrue(tm.containsKey("only"));
    }

    @Test
    void keySet_ClearOnSetEmptiesUnderlyingMap() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("x", 10);
        tm.put("y", 20);
        Set<String> ks = tm.keySet();
        ks.clear();
        assertTrue(ks.isEmpty());
        assertTrue(tm.isEmpty());
        assertEquals(0, tm.size());
    }

    @Test
    void keySet_ToArrayReflectsCurrentKeys() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("a", 1);
        tm.put("b", 2);
        Set<String> ks = tm.keySet();
        Object[] arr = ks.toArray();
        assertEquals(2, arr.length);
        List<Object> asList = Arrays.asList(arr);
        assertTrue(asList.containsAll(Arrays.asList("a", "b")));
    }

    @Test
    void keySet_ToTypedArrayReflectsCurrentKeys() {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.put("x", 100);
        tm.put("y", 200);
        Set<String> ks = tm.keySet();
        String[] arr = ks.toArray(new String[0]);
        assertEquals(2, arr.length);
        List<String> asList = Arrays.asList(arr);
        assertTrue(asList.containsAll(Arrays.asList("x", "y")));
    }

    @Test
    void keySet_InternalMapIsNull_ThrowsNullPointerException() throws Exception {
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        // Force internalMap to null via reflection.
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(tm, null);
        assertThrows(NullPointerException.class, tm::keySet);
    }

    @Test
    void keySet_UnderlyingMapThrowsRuntimeException_Propagates() {
        // Custom map that throws on keySet()
        Map<String, Integer> explodingMap = new AbstractMap<String, Integer>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public Set<String> keySet() {
                throw new RuntimeException("keySet failure");
            }
        };
        TrackingMap<String, Integer> tm = new TrackingMap<>(explodingMap);
        RuntimeException ex = assertThrows(RuntimeException.class, tm::keySet);
        assertEquals("keySet failure", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 5, 10 })
    void keySet_SizeMatchesMapSize(int entryCount) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < entryCount; i++) {
            map.put("k" + i, i);
        }
        TrackingMap<String, Integer> tm = new TrackingMap<>(map);
        assertEquals(entryCount, tm.keySet().size(), "keySet size should equal the number of entries in the map");
    }
}
