package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Constructor;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_keySet_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start each test un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // use a simple concrete implementation that allows nulls
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        backingMap.put("c", 3);
        // also test null handling – the underlying map permits a null key/value
        backingMap.put(null, null);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @Test
    void keySetContainsAllKeysInitially() {
        Set<String> ks = sealableMap.keySet();
        assertTrue(ks.containsAll(Arrays.asList("a", "b", "c", null)), "keySet should contain every key present in the map, including null");
        assertEquals(4, ks.size(), "keySet size must match map size");
    }

    @Test
    void keySetReflectsAdditionsToMap() {
        sealableMap.put("d", 4);
        assertTrue(sealableMap.keySet().contains("d"), "keySet must reflect newly added keys");
    }

    @Test
    void keySetReflectsRemovalsFromMap() {
        sealableMap.remove("b");
        assertFalse(sealableMap.keySet().contains("b"), "keySet must reflect removed keys");
    }

    @Test
    void keySetAllowsRemovalWhenUnsealed() {
        Set<String> ks = sealableMap.keySet();
        assertTrue(ks.remove("a"), "remove should succeed while unsealed");
        assertFalse(sealableMap.containsKey("a"), "Underlying map must no longer contain the removed key");
        assertEquals(3, sealableMap.size(), "Map size must shrink after removal via keySet");
    }

    @Test
    void keySetRemovalThrowsWhenSealed() {
        // seal the map
        sealed = true;
        Set<String> ks = sealableMap.keySet();
        assertThrows(UnsupportedOperationException.class, () -> ks.remove("a"), "remove on keySet must throw when the map is sealed");
    }

    @Test
    void keySetIteratorRemoveRespectsSealState() {
        Iterator<String> it = sealableMap.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            if ("b".equals(key)) {
                // un‑sealed – removal should succeed
                it.remove();
                assertFalse(sealableMap.containsKey("b"));
                assertEquals(3, sealableMap.size());
                // now seal and attempt another removal
                sealed = true;
                assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove must throw when sealed");
                break;
            }
        }
    }

    @Test
    void keySetIsImmutableWhenSealedEvenForContainsAndSize() {
        sealed = true;
        Set<String> ks = sealableMap.keySet();
        // read‑only operations must still work
        assertTrue(ks.contains("c"));
        assertEquals(4, ks.size());
        // but mutating attempts must fail
        assertThrows(UnsupportedOperationException.class, () -> ks.add("x"));
        assertThrows(UnsupportedOperationException.class, () -> ks.clear());
    }

    @Test
    void keySetViewUpdatesWhenUnderlyingMapChangesDirectly() {
        // modify the backing map directly (simulating external changes)
        backingMap.put("external", 99);
        assertTrue(sealableMap.keySet().contains("external"), "keySet must reflect changes made directly to the underlying map");
        backingMap.remove("c");
        assertFalse(sealableMap.keySet().contains("c"), "keySet must reflect removals made directly to the underlying map");
    }

    @Test
    @DisplayName("keySet() returns a SealableSet containing exactly the map's keys")
    void keySetReturnsSealableSetWithExactContents() {
        // Use a deterministic backing map (TreeMap) to guarantee order
        NavigableMap<String, Integer> tree = new TreeMap<>();
        tree.put("a", 1);
        tree.put("b", 2);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(tree, () -> false);
        Set<String> ks = map.keySet();
        // Verify the concrete type
        assertTrue(ks instanceof SealableSet, "keySet() should return a SealableSet instance");
        // Verify contents
        assertEquals(Set.of("a", "b"), ks, "Returned set must contain exactly the keys of the backing map");
    }

    @Test
    @DisplayName("Modifications via the returned set are reflected in the underlying map when unsealed")
    void keySetModificationReflectedWhenUnsealed() {
        Set<String> ks = sealableMap.keySet();
        assertTrue(ks.remove("a"), "remove should return true for existing key");
        assertFalse(ks.contains("a"));
        assertFalse(sealableMap.containsKey("a"));
        assertEquals(3, sealableMap.size(), "Map size must shrink after removal via the set view");
    }

    @Test
    @DisplayName("Attempting to modify the set when sealed throws UnsupportedOperationException")
    void keySetModificationThrowsWhenSealed() {
        sealed = true;
        Set<String> ks = sealableMap.keySet();
        // remove
        assertThrows(UnsupportedOperationException.class, () -> ks.remove("b"), "remove must throw when sealed");
        // add (unsupported for Set view)
        assertThrows(UnsupportedOperationException.class, () -> ks.add("x"), "add must throw when sealed");
    }

    @Test
    @DisplayName("keySet view updates automatically after underlying map mutation")
    void keySetViewUpdatesAfterMapMutation() {
        Set<String> ks = sealableMap.keySet();
        // add directly to the backing map
        backingMap.put("newKey", 42);
        assertTrue(ks.contains("newKey"), "keySet must reflect newly added key without re‑calling keySet()");
        // remove directly from the backing map
        backingMap.remove("c");
        assertFalse(ks.contains("c"), "keySet must reflect removal of a key made directly on the map");
    }

    @Test
    @DisplayName("Multiple calls to keySet() return distinct SealableSet objects sharing the same backing set")
    void multipleKeySetCallsReturnDistinctButLinkedViews() {
        Set<String> set1 = sealableMap.keySet();
        Set<String> set2 = sealableMap.keySet();
        assertNotSame(set1, set2, "Each call to keySet() should return a distinct Set instance");
        assertTrue(set1 instanceof SealableSet && set2 instanceof SealableSet, "Both returned objects must be instances of SealableSet");
        // Mutate via set1 and verify visibility through set2
        assertTrue(set1.remove("a"));
        assertFalse(set2.contains("a"), "Modification through one view must be visible through the other");
    }

    @Test
    @DisplayName("Iterator.remove() works when unsealed and throws when sealed")
    void iteratorRemoveRespectsSealState() {
        // Unsealed – should succeed
        Iterator<String> it = sealableMap.keySet().iterator();
        assertTrue(it.hasNext());
        String first = it.next();
        it.remove();
        assertFalse(sealableMap.containsKey(first));
        assertEquals(3, sealableMap.size());
        // Seal and attempt another removal
        sealed = true;
        Iterator<String> it2 = sealableMap.keySet().iterator();
        assertTrue(it2.hasNext());
        // move to some element
        it2.next();
        assertThrows(UnsupportedOperationException.class, it2::remove, "Iterator.remove must throw when the map is sealed");
    }

    @Test
    @DisplayName("size() of SealableSet matches underlying map size after modifications")
    void sealableSetSizeMatchesMapSize() {
        Set<String> ks = sealableMap.keySet();
        // remove one key via the set
        ks.remove("a");
        assertEquals(sealableMap.size(), ks.size(), "Set size must equal map size after removal via the set");
        // add a new entry directly to the map
        backingMap.put("new", 5);
        assertEquals(sealableMap.size(), ks.size(), "Set size must stay in sync after direct map addition");
    }

    @ParameterizedTest(name = "[{index}] containsAll({0}) => {1}")
    @CsvSource({ "'a,b', true", "'a,x', false" })
    @DisplayName("containsAll works correctly for present and missing elements")
    void containsAllBehaviour(String csvKeys, boolean expected) {
        Set<String> ks = sealableMap.keySet();
        List<String> keys = Arrays.asList(csvKeys.split(","));
        assertEquals(expected, ks.containsAll(keys), "containsAll should return " + expected + " for collection " + keys);
    }

    @Test
    @DisplayName("toArray() returns an array with keys in iteration order")
    void toArrayReturnsOrderedArray() {
        NavigableMap<String, Integer> tree = new TreeMap<>();
        tree.put("a", 1);
        tree.put("b", 2);
        tree.put("c", 3);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(tree, () -> false);
        Object[] arr = map.keySet().toArray();
        assertArrayEquals(new Object[] { "a", "b", "c" }, arr, "toArray() must preserve the natural order of the TreeMap keys");
    }

    @Test
    @DisplayName("toArray(T[]) populates provided large enough array and null‑terminates")
    void toArrayTypedPopulatesSuppliedArray() {
        NavigableMap<Integer, String> tree = new TreeMap<>();
        tree.put(1, "one");
        tree.put(2, "two");
        tree.put(3, "three");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(tree, () -> false);
        Integer[] supplied = new Integer[5];
        Integer[] result = map.keySet().toArray(supplied);
        // The same array instance should be returned
        assertSame(supplied, result, "When the supplied array is large enough, it should be returned");
        assertArrayEquals(new Integer[] { 1, 2, 3, null, null }, result, "Array should contain the keys followed by nulls");
    }

    @Test
    @DisplayName("toArray(T[]) creates a new array of correct runtime type when supplied array is too small")
    void toArrayTypedCreatesNewArrayWhenTooSmall() {
        NavigableMap<String, Integer> tree = new TreeMap<>();
        tree.put("alpha", 1);
        tree.put("beta", 2);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(tree, () -> false);
        String[] result = map.keySet().toArray(new String[0]);
        assertEquals(2, result.length, "Resulting array length must match number of keys");
        assertArrayEquals(new String[] { "alpha", "beta" }, result, "Resulting array must contain exactly the map's keys in order");
    }

    @Test
    @DisplayName("clear() on the SealableSet removes all entries when unsealed")
    void clearRemovesAllWhenUnsealed() {
        Set<String> ks = sealableMap.keySet();
        ks.clear();
        assertTrue(sealableMap.isEmpty(), "clear() via the set view must empty the underlying map");
        assertTrue(ks.isEmpty(), "Set view must also be empty after clear()");
    }

    @Test
    @DisplayName("clear() on the SealableSet throws when sealed")
    void clearThrowsWhenSealed() {
        sealed = true;
        Set<String> ks = sealableMap.keySet();
        assertThrows(UnsupportedOperationException.class, ks::clear, "clear() must throw when the map is sealed");
        assertFalse(sealableMap.isEmpty(), "Map must remain unchanged after failed clear()");
    }

    @Test
    @DisplayName("Constructor with null sealedSupplier leads to NPE on keySet()")
    void nullSealedSupplierThrowsNPE() {
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(backingMap, null);
        assertThrows(NullPointerException.class, map::keySet, "Calling keySet() with a null sealedSupplier should throw NullPointerException");
    }

    @Test
    @DisplayName("Constructor with null underlying map leads to NPE on keySet()")
    void nullUnderlyingMapThrowsNPE() {
        // Directly pass null for the backing map; the constructor itself may accept it,
        // but keySet() will attempt to invoke navMap.keySet() and thus NPE.
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(null, () -> false);
        assertThrows(NullPointerException.class, map::keySet, "Calling keySet() when the underlying navMap is null should throw NullPointerException");
    }
}
