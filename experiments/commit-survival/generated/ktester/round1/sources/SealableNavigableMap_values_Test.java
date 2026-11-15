package com.cedarsoftware.util;

import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_values_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        backingMap.put("three", 3);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @Test
    void valuesReflectUnderlyingMapChanges() {
        Collection<Integer> values = sealableMap.values();
        // initial contents
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
        assertEquals(3, values.size());
        // add a new entry directly to the map – the values view must see it
        sealableMap.put("four", 4);
        assertTrue(values.contains(4));
        assertEquals(4, values.size());
    }

    @Test
    void valuesAllowRemovalWhenUnsealed() {
        Collection<Integer> values = sealableMap.values();
        // remove an existing value – should remove the corresponding entry from the map
        assertTrue(values.remove(Integer.valueOf(2)));
        assertFalse(sealableMap.containsKey("two"));
        assertEquals(2, sealableMap.size());
        // the values view must no longer contain the removed element
        assertFalse(values.contains(2));
        assertEquals(2, values.size());
    }

    @Test
    void valuesDisallowModificationWhenSealed() {
        // make the map read‑only
        sealed = true;
        Collection<Integer> values = sealableMap.values();
        // attempt to remove – should throw
        assertThrows(UnsupportedOperationException.class, () -> values.remove(Integer.valueOf(1)));
        // attempt to add – should also throw (the underlying SealableList checks the sealed supplier)
        assertThrows(UnsupportedOperationException.class, () -> values.add(99));
    }

    @Test
    void iteratorRemoveRespectsSealedState() {
        Collection<Integer> values = sealableMap.values();
        Iterator<Integer> it = values.iterator();
        // advance to first element
        assertTrue(it.hasNext());
        it.next();
        // while unsealed, removal should succeed
        it.remove();
        assertFalse(sealableMap.containsKey("one"));
        assertEquals(2, sealableMap.size());
        // now seal the map and verify iterator removal throws
        sealed = true;
        it = sealableMap.values().iterator();
        assertTrue(it.hasNext());
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    /**
     * values() – empty underlying map, unsealed
     */
    @Test
    void emptyMapUnsealedReturnsMutableEmptyCollection() {
        NavigableMap<Integer, String> emptyMap = new TreeMap<>();
        Supplier<Boolean> neverSealed = () -> false;
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(emptyMap, neverSealed);
        Collection<String> values = map.values();
        assertTrue(values.isEmpty(), "Collection should be empty");
        assertDoesNotThrow(() -> values.add("new"), "Adding to an unsealed empty collection must not throw");
        assertEquals(1, values.size());
        assertTrue(values.contains("new"));
    }

    /**
     * values() – empty underlying map, sealed
     */
    @Test
    void emptyMapSealedReturnsImmutableEmptyCollection() {
        NavigableMap<Integer, String> emptyMap = new TreeMap<>();
        Supplier<Boolean> alwaysSealed = () -> true;
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(emptyMap, alwaysSealed);
        Collection<String> values = map.values();
        assertTrue(values.isEmpty(), "Collection should be empty");
        assertThrows(UnsupportedOperationException.class, () -> values.add("new"), "Adding to a sealed collection must throw");
        assertThrows(UnsupportedOperationException.class, () -> values.remove("any"), "Removing from a sealed collection must throw");
    }

    /**
     * values() – non‑empty map, mutable
     */
    @Test
    void nonEmptyMapMutableAllMutationsAllowed() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "a");
        base.put(2, "b");
        base.put(3, "c");
        Supplier<Boolean> neverSealed = () -> false;
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(base, neverSealed);
        Collection<String> values = map.values();
        // snapshot must contain the three values in the map's value‑iteration order
        assertEquals(3, values.size());
        assertTrue(values.containsAll(Arrays.asList("a", "b", "c")));
        // add a new value – should succeed and be reflected in the collection
        assertTrue(values.add("d"));
        assertEquals(4, values.size());
        assertTrue(values.contains("d"));
        // remove an existing value – should succeed and also remove the entry from the map
        assertTrue(values.remove("b"));
        assertEquals(3, values.size());
        assertFalse(values.contains("b"));
        // key 2 should have been removed
        assertFalse(map.containsKey(2));
        // clear the collection – underlying map must become empty as well
        values.clear();
        assertTrue(values.isEmpty());
        assertTrue(map.isEmpty());
    }

    /**
     * values() – non‑empty map, immutable
     */
    @Test
    void nonEmptyMapImmutableAllMutationsProhibited() {
        NavigableMap<String, Integer> base = new TreeMap<>();
        base.put("x", 10);
        base.put("y", 20);
        Supplier<Boolean> alwaysSealed = () -> true;
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(base, alwaysSealed);
        Collection<Integer> values = map.values();
        assertEquals(2, values.size());
        assertTrue(values.containsAll(Arrays.asList(10, 20)));
        // any mutating operation must throw
        assertAll(() -> assertThrows(UnsupportedOperationException.class, () -> values.add(30)), () -> assertThrows(UnsupportedOperationException.class, () -> values.remove(10)), () -> assertThrows(UnsupportedOperationException.class, values::clear));
    }

    /**
     * values() – mutable then sealed (AtomicBoolean)
     */
    @Test
    void mutableThenSealedTransitionsAccordingToAtomicBoolean() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(5, "five");
        base.put(6, "six");
        AtomicBoolean sealFlag = new AtomicBoolean(false);
        Supplier<Boolean> supplier = sealFlag::get;
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(base, supplier);
        Collection<String> values = map.values();
        // initially mutable
        assertDoesNotThrow(() -> values.add("seven"));
        assertTrue(values.contains("seven"));
        assertEquals(3, values.size());
        // now seal the map
        sealFlag.set(true);
        assertThrows(UnsupportedOperationException.class, () -> values.add("eight"));
        assertThrows(UnsupportedOperationException.class, () -> values.remove("five"));
    }

    /**
     * values() – snapshot independence
     */
    @Test
    void snapshotIndependenceBetweenConsecutiveCalls() {
        NavigableMap<String, Integer> base = new TreeMap<>();
        base.put("a", 1);
        base.put("b", 2);
        Supplier<Boolean> neverSealed = () -> false;
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(base, neverSealed);
        // first snapshot
        Collection<Integer> first = map.values();
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(first));
        // mutate underlying map after the snapshot was taken
        map.put("c", 3);
        // second snapshot should see the new entry
        Collection<Integer> second = map.values();
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(second));
        // first snapshot must stay unchanged
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(first));
    }

    /**
     * values() – null underlying map
     */
    @Test
    void nullUnderlyingMapThrowsNPE() {
        Supplier<Boolean> neverSealed = () -> false;
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(null, neverSealed);
        assertThrows(NullPointerException.class, map::values);
    }

    /**
     * values() – null sealedSupplier
     */
    @Test
    void nullSealedSupplierThrowsNPE() {
        NavigableMap<String, Integer> base = new TreeMap<>();
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(base, null);
        assertThrows(NullPointerException.class, map::values);
    }

    /**
     * values() – supplier throws on mutation
     */
    @Test
    void supplierThrowsDuringMutationPropagatesException() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "one");
        Supplier<Boolean> explodingSupplier = () -> {
            throw new RuntimeException("supplier failure");
        };
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(base, explodingSupplier);
        Collection<String> values = map.values();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> values.add("two"));
        assertEquals("supplier failure", ex.getMessage());
    }
}
