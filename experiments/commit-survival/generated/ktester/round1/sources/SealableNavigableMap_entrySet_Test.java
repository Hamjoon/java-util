package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.util.AbstractMap;
import java.util.NoSuchElementException;
import java.util.TreeMap;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_entrySet_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        // Use the same concrete implementation that the library provides
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("alpha", 1);
        backingMap.put("beta", 2);
        backingMap.put("gamma", 3);
        // also test a null key/value
        backingMap.put(null, null);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        backingMap = null;
        sealableMap = null;
        sealedSupplier = null;
    }

    @Test
    void entrySetReflectsBackingMapContents() {
        Set<Map.Entry<String, Integer>> entrySet = sealableMap.entrySet();
        assertEquals(backingMap.size(), entrySet.size(), "entrySet size should match backing map size");
        // Verify each expected entry is present
        assertTrue(entrySet.stream().anyMatch(e -> "alpha".equals(e.getKey()) && Integer.valueOf(1).equals(e.getValue())));
        assertTrue(entrySet.stream().anyMatch(e -> "beta".equals(e.getKey()) && Integer.valueOf(2).equals(e.getValue())));
        assertTrue(entrySet.stream().anyMatch(e -> "gamma".equals(e.getKey()) && Integer.valueOf(3).equals(e.getValue())));
        assertTrue(entrySet.stream().anyMatch(e -> e.getKey() == null && e.getValue() == null));
    }

    @Test
    void iteratorRemoveWorksWhenUnsealed() {
        Iterator<Map.Entry<String, Integer>> it = sealableMap.entrySet().iterator();
        // advance to first element
        assertTrue(it.hasNext());
        it.next();
        // removal should succeed because the map is currently unsealed
        it.remove();
        assertEquals(3, sealableMap.size(), "Size should decrease after successful iterator.remove()");
        // The removed key is the first key in natural order ("alpha")
        assertFalse(sealableMap.containsKey("alpha"));
    }

    @Test
    void iteratorRemoveThrowsWhenSealed() {
        Iterator<Map.Entry<String, Integer>> it = sealableMap.entrySet().iterator();
        assertTrue(it.hasNext());
        it.next();
        // seal the map before attempting removal
        sealed = true;
        assertThrows(UnsupportedOperationException.class, it::remove, "Iterator.remove() must throw when the map is sealed");
    }

    @Test
    void entrySetValueModificationWorksWhenUnsealed() {
        // firstEntry returns a live entry
        Map.Entry<String, Integer> entry = sealableMap.firstEntry();
        assertNotNull(entry, "firstEntry should not be null");
        // modify the value via setValue
        Integer old = entry.setValue(42);
        assertEquals(Integer.valueOf(1), old, "setValue should return the previous value");
        assertEquals(Integer.valueOf(42), sealableMap.get(entry.getKey()), "The map should reflect the new value after setValue");
    }

    @Test
    void entrySetValueModificationThrowsWhenSealed() {
        Map.Entry<String, Integer> entry = sealableMap.firstEntry();
        assertNotNull(entry, "firstEntry should not be null");
        // seal the map
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(99), "Calling setValue on an entry should throw when the map is sealed");
    }

    @Test
    void entrySetOnEmptyBackingMapReturnsEmptySet() {
        // Create a SealableNavigableMap backed by an empty TreeMap
        NavigableMap<Integer, String> emptyMap = new TreeMap<>();
        SealableNavigableMap<Integer, String> emptySealable = new SealableNavigableMap<>(emptyMap, () -> false);
        Set<Map.Entry<Integer, String>> set = emptySealable.entrySet();
        assertEquals(0, set.size(), "Empty map should produce a set of size 0");
        assertFalse(set.iterator().hasNext(), "Iterator should have no elements");
    }

    @Test
    void entrySetAddWhenUnsealedAddsToBackingMap() {
        Set<Map.Entry<String, Integer>> set = sealableMap.entrySet();
        // add a new entry – should succeed because the map is unsealed
        boolean added = set.add(new AbstractMap.SimpleEntry<>("delta", 4));
        assertTrue(added, "add() must return true when entry is inserted");
        assertEquals(5, sealableMap.size(), "Backing map size must increase");
        assertTrue(sealableMap.containsKey("delta"));
        assertEquals(Integer.valueOf(4), sealableMap.get("delta"));
    }

    @Test
    void entrySetAddWhenSealedThrowsIllegalStateException() {
        // map is sealed before we obtain the set
        sealed = true;
        Set<Map.Entry<String, Integer>> set = sealableMap.entrySet();
        assertThrows(IllegalStateException.class, () -> set.add(new AbstractMap.SimpleEntry<>("epsilon", 5)), "add() must throw IllegalStateException when the map is sealed");
    }

    @Test
    void entrySetAddRespectsDynamicSealingChanges() {
        // Obtain the set while the map is unsealed
        Set<Map.Entry<String, Integer>> set = sealableMap.entrySet();
        // First addition should succeed
        assertTrue(set.add(new AbstractMap.SimpleEntry<>("zeta", 6)), "First add should succeed while unsealed");
        // Change sealing state to true
        sealed = true;
        // Subsequent addition must fail
        assertThrows(IllegalStateException.class, () -> set.add(new AbstractMap.SimpleEntry<>("eta", 7)), "add() after sealing must throw IllegalStateException");
    }

    @Test
    void iteratorOnEmptySetBehavesCorrectly() {
        NavigableMap<Integer, String> emptyMap = new TreeMap<>();
        SealableNavigableMap<Integer, String> emptySealable = new SealableNavigableMap<>(emptyMap, () -> false);
        Iterator<Map.Entry<Integer, String>> it = emptySealable.entrySet().iterator();
        assertFalse(it.hasNext(), "Iterator should report no next element for empty set");
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    @Test
    void entrySetConstructionWithNullBackingMapThrowsNPE() {
        // Supplier is valid; backing map is null
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(null, () -> false);
        assertThrows(NullPointerException.class, map::entrySet, "Calling entrySet() with a null backing map should throw NullPointerException");
    }

    @Test
    void entrySetConstructionWithNullSupplierThrowsNPE() {
        // Backing map is valid; supplier is null
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(new TreeMap<>(), null);
        assertThrows(NullPointerException.class, map::entrySet, "Calling entrySet() with a null sealedSupplier should throw NullPointerException");
    }

    @Test
    void iteratorRemoveWhenSealedAfterObtainingSetThrowsIllegalStateException() {
        // Obtain set while unsealed
        Set<Map.Entry<String, Integer>> set = sealableMap.entrySet();
        Iterator<Map.Entry<String, Integer>> it = set.iterator();
        assertTrue(it.hasNext());
        it.next();
        // Seal the map now
        sealed = true;
        // According to the library contract, iterator.remove() should now throw
        assertThrows(IllegalStateException.class, it::remove, "Iterator.remove() must throw IllegalStateException after the map becomes sealed");
    }
}
