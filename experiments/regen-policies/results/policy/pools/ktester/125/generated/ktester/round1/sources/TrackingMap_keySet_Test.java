package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

// retained from the template; Mockito not used here
@ExtendWith(MockitoExtension.class)
class TrackingMap_keySet_Test {

    /**
     * Backing map that will be wrapped by the TrackingMap under test.
     */
    private Map<String, Integer> backingMap;

    /**
     * TrackingMap instance used in each test.
     */
    private TrackingMap<String, Integer> trackingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use a plain HashMap as the backing map – it is simple and has no side‑effects.
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help the garbage collector; not strictly required.
        trackingMap = null;
        backingMap = null;
    }

    // Example of a parameter source – currently unused but kept for illustration
    static Stream<Arguments> emptyAndNonEmptyMaps() {
        return Stream.of(Arguments.of(new HashMap<String, Integer>(), Collections.emptySet()), Arguments.of(Map.of("a", 1, "b", 2), Set.of("a", "b")));
    }

    /**
     * Verify that {@code keySet()} returns the same Set instance that the backing map
     * would return and that it reflects the current keys in the map.
     */
    @Test
    void keySetReflectsCurrentKeys() {
        // initially empty
        Set<String> keySet = trackingMap.keySet();
        assertTrue(keySet.isEmpty(), "keySet should be empty for a newly created map");
        // add entries via TrackingMap
        trackingMap.put("one", 1);
        trackingMap.put("two", 2);
        trackingMap.put("three", 3);
        // keySet should now contain exactly the three keys
        assertEquals(3, keySet.size(), "keySet size should match number of inserted entries");
        assertTrue(keySet.containsAll(Set.of("one", "two", "three")));
        // The Set returned by keySet() must be the same instance on successive calls
        assertSame(keySet, trackingMap.keySet(), "keySet() should return a live view – the same Set instance each call");
    }

    /**
     * Ensure that modifications to the Set returned by {@code keySet()} affect the underlying map.
     */
    @Test
    void keySetModificationAffectsMap() {
        trackingMap.put("alpha", 10);
        trackingMap.put("beta", 20);
        trackingMap.put("gamma", 30);
        Set<String> keySet = trackingMap.keySet();
        // remove a key via the Set view (supported)
        assertTrue(keySet.remove("beta"), "Removing an existing key via keySet should succeed");
        assertFalse(keySet.contains("beta"));
        assertFalse(trackingMap.containsKey("beta"));
        assertEquals(2, trackingMap.size());
        // attempt to add a key via the Set view – should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> keySet.add("delta"), "Adding a new key via keySet should throw UnsupportedOperationException");
        // map must remain unchanged
        assertFalse(trackingMap.containsKey("delta"));
        assertEquals(2, trackingMap.size());
    }

    /**
     * After calling {@code expungeUnused()}, the {@code keySet()} should reflect the
     * removal of entries that were never accessed.
     */
    @Test
    void keySetReflectsExpungeUnused() {
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        trackingMap.put("c", 3);
        // Access only "a" and "b"
        trackingMap.get("a");
        trackingMap.containsKey("b");
        // At this point the internal tracking set contains "a" and "b"
        // should remove "c"
        trackingMap.expungeUnused();
        Set<String> keySet = trackingMap.keySet();
        assertEquals(2, keySet.size(), "After expungeUnused, only accessed keys should remain");
        assertTrue(keySet.containsAll(Set.of("a", "b")));
        assertFalse(keySet.contains("c"));
    }

    /**
     * Verify that the Set returned by {@code keySet()} is empty when the map is empty,
     * even after calling {@code expungeUnused()}.
     */
    @Test
    void emptyMapKeySetRemainsEmpty() {
        assertTrue(trackingMap.isEmpty());
        assertTrue(trackingMap.keySet().isEmpty(), "keySet should be empty for an empty map");
        // Calling expungeUnused on an empty map must not change anything
        trackingMap.expungeUnused();
        assertTrue(trackingMap.keySet().isEmpty(), "keySet should stay empty after expungeUnused");
    }

    @Test
    void keySetEmptyMap() {
        // Empty map – keySet should be empty
        assertTrue(trackingMap.keySet().isEmpty(), "When the internal map is empty, keySet() should return an empty Set.");
    }

    @Test
    void keySetTwoEntries() {
        trackingMap.put("alpha", 1);
        trackingMap.put("beta", 2);
        Set<String> ks = trackingMap.keySet();
        assertEquals(2, ks.size());
        assertTrue(ks.containsAll(Set.of("alpha", "beta")), "keySet should contain exactly \"alpha\" and \"beta\".");
    }

    @Test
    void keySetWithNullKey() {
        // HashMap permits a single null key
        trackingMap.put(null, 5);
        trackingMap.put("x", 10);
        Set<String> ks = trackingMap.keySet();
        assertEquals(2, ks.size());
        assertTrue(ks.contains(null));
        assertTrue(ks.contains("x"));
    }

    @Test
    void keySetRemovalViaSetDecreasesMapSize() {
        trackingMap.put("alpha", 1);
        trackingMap.put("beta", 2);
        Set<String> ks = trackingMap.keySet();
        assertTrue(ks.remove("alpha"));
        assertEquals(1, trackingMap.size());
        assertFalse(ks.contains("alpha"));
    }

    @Test
    void keySetReflectsPutAfterObtainingSet() {
        trackingMap.put("alpha", 1);
        Set<String> ks = trackingMap.keySet();
        trackingMap.put("gamma", 3);
        assertTrue(ks.contains("gamma"));
    }

    @Test
    void keySetAddDirectUnsupported() {
        Set<String> ks = trackingMap.keySet();
        assertThrows(UnsupportedOperationException.class, () -> ks.add("newKey"), "Adding directly to the keySet view must throw UnsupportedOperationException.");
    }

    @Test
    void keySetSameInstanceOnConsecutiveCalls() {
        Set<String> first = trackingMap.keySet();
        Set<String> second = trackingMap.keySet();
        assertSame(first, second, "Calling keySet() twice should return the exact same Set object.");
    }

    @Test
    void keySetEmptyAfterClear() {
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        Set<String> ks = trackingMap.keySet();
        trackingMap.clear();
        assertTrue(ks.isEmpty(), "After map.clear(), the previously obtained Set should become empty.");
    }

    @Test
    void keySetReflectsMapRemove() {
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        Set<String> ks = trackingMap.keySet();
        trackingMap.remove("a");
        assertFalse(ks.contains("a"));
        assertTrue(ks.contains("b"));
    }

    @Test
    void keySetReflectsPutAll() {
        Map<String, Integer> src = Map.of("x", 1, "y", 2, "z", 3);
        trackingMap.putAll(src);
        Set<String> ks = trackingMap.keySet();
        assertEquals(3, ks.size());
        assertTrue(ks.containsAll(Set.of("x", "y", "z")));
    }

    @Test
    void iteratorTraversesAllKeys() {
        trackingMap.put("k1", 1);
        trackingMap.put("k2", 2);
        trackingMap.put("k3", 3);
        Set<String> ks = trackingMap.keySet();
        Set<String> collected = new HashSet<>();
        for (String k : ks) {
            collected.add(k);
        }
        assertEquals(Set.of("k1", "k2", "k3"), collected);
    }

    @Test
    void iteratorRemoveDeletesEntryFromMap() {
        trackingMap.put("p", 1);
        trackingMap.put("q", 2);
        Iterator<String> it = trackingMap.keySet().iterator();
        assertEquals("p", it.next());
        // remove "p"
        it.remove();
        assertFalse(trackingMap.containsKey("p"));
        assertEquals(1, trackingMap.size());
        assertFalse(trackingMap.keySet().contains("p"));
    }

    @Test
    void iteratorRemoveBeforeNextThrowsIllegalStateException() {
        trackingMap.put("x", 1);
        Iterator<String> it = trackingMap.keySet().iterator();
        assertThrows(IllegalStateException.class, it::remove, "Calling iterator.remove() before next() must throw IllegalStateException.");
    }

    @Test
    void iteratorRemoveTwiceThrowsIllegalStateException() {
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        Iterator<String> it = trackingMap.keySet().iterator();
        // first element
        it.next();
        // first removal – ok
        it.remove();
        assertThrows(IllegalStateException.class, it::remove, "Calling iterator.remove() twice after a single next() must throw IllegalStateException.");
    }

    @Test
    void iteratorNextBeyondEndThrowsNoSuchElementException() {
        trackingMap.put("solo", 1);
        Iterator<String> it = trackingMap.keySet().iterator();
        assertEquals("solo", it.next());
        assertThrows(NoSuchElementException.class, it::next, "Calling next() beyond the last element must throw NoSuchElementException.");
    }

    @Test
    void iteratorDetectsConcurrentModification() {
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        Iterator<String> it = trackingMap.keySet().iterator();
        // modify the map after iterator creation
        trackingMap.put("c", 3);
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator must detect concurrent modification and throw ConcurrentModificationException.");
    }

    @Test
    void removeAllDeletesSpecifiedKeys() {
        trackingMap.put("one", 1);
        trackingMap.put("two", 2);
        trackingMap.put("three", 3);
        boolean changed = trackingMap.keySet().removeAll(Arrays.asList("one", "three"));
        assertTrue(changed);
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("two"));
        assertFalse(trackingMap.containsKey("one"));
        assertFalse(trackingMap.containsKey("three"));
    }

    @Test
    void retainAllKeepsOnlySpecifiedKeys() {
        trackingMap.put("alpha", 1);
        trackingMap.put("beta", 2);
        trackingMap.put("gamma", 3);
        boolean changed = trackingMap.keySet().retainAll(Collections.singleton("beta"));
        assertTrue(changed);
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("beta"));
        assertFalse(trackingMap.containsKey("alpha"));
        assertFalse(trackingMap.containsKey("gamma"));
    }

    @Test
    void toArrayReturnsCorrectSizedArray() {
        trackingMap.put("x", 1);
        trackingMap.put("y", 2);
        Object[] arr = trackingMap.keySet().toArray();
        assertEquals(2, arr.length);
        assertTrue(Arrays.asList(arr).containsAll(List.of("x", "y")));
    }

    @Test
    void toArrayWithLargerDestinationArray() {
        trackingMap.put("a", 1);
        trackingMap.put("b", 2);
        trackingMap.put("c", 3);
        String[] dest = new String[5];
        String[] result = trackingMap.keySet().toArray(dest);
        assertSame(dest, result, "toArray(T[]) should return the supplied array when it is large enough.");
        // first three positions must be filled, the rest null
        List<String> filled = Arrays.asList(result[0], result[1], result[2]);
        assertTrue(filled.containsAll(List.of("a", "b", "c")));
        assertNull(result[3]);
        assertNull(result[4]);
    }

    @Test
    void addAllThrowsUnsupportedOperationException() {
        Set<String> ks = trackingMap.keySet();
        assertThrows(UnsupportedOperationException.class, () -> ks.addAll(Arrays.asList("newKey")), "addAll on the keySet view must throw UnsupportedOperationException.");
    }

    @Test
    void removeAllNullThrowsNullPointerException() {
        Set<String> ks = trackingMap.keySet();
        assertThrows(NullPointerException.class, () -> ks.removeAll(null), "removeAll(null) must throw NullPointerException.");
    }

    @Test
    void retainAllNullThrowsNullPointerException() {
        Set<String> ks = trackingMap.keySet();
        assertThrows(NullPointerException.class, () -> ks.retainAll(null), "retainAll(null) must throw NullPointerException.");
    }

    @Test
    void toArrayIncompatibleArrayThrowsArrayStoreException() {
        trackingMap.put("s", 1);
        // Attempt to store String keys into an Integer array
        assertThrows(ArrayStoreException.class, () -> trackingMap.keySet().toArray(new Integer[0]), "Incompatible array type must cause ArrayStoreException.");
    }

    @Test
    void keySetWhenInternalMapIsNullThrowsNullPointerException() throws Exception {
        // Use reflection to set the private field `internalMap` to null
        Field field = TrackingMap.class.getDeclaredField("internalMap");
        field.setAccessible(true);
        field.set(trackingMap, null);
        // Now invoking keySet() should result in NPE
        Executable call = trackingMap::keySet;
        assertThrows(NullPointerException.class, call, "Calling keySet() when internalMap is null must throw NullPointerException.");
    }
}
