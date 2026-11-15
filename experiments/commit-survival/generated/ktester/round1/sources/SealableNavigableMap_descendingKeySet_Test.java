package com.cedarsoftware.util;

import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_descendingKeySet_Test {

    private NavigableMap<String, Integer> map;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        // Use a map implementation that permits null keys for the default tests.
        map = new ConcurrentNavigableMapNullSafe<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        // keep the same data as other tests
        map.put(null, null);
        sealableMap = new SealableNavigableMap<>(map, sealedSupplier);
    }

    @Test
    void testDescendingKeySetReflectsMapChanges() {
        // add a new entry after the set is obtained
        sealableMap.put("zero", 0);
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        // the set should contain the newly added key
        assertTrue(descendingSet.contains("zero"));
        // verify descending order – highest key first
        Iterator<String> it = descendingSet.iterator();
        // "three" > "two" > "one" > "zero"
        assertEquals("three", it.next());
        assertEquals("two", it.next());
        assertEquals("one", it.next());
        assertEquals("zero", it.next());
    }

    @Test
    void testRemovalWhenUnsealed() {
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        // removal should succeed while the map is unsealed
        assertTrue(descendingSet.remove("two"));
        assertFalse(sealableMap.containsKey("two"));
        assertFalse(descendingSet.contains("two"));
    }

    @Test
    void testRemovalWhenSealedThrows() {
        // seal the map
        sealed = true;
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.remove("two"));
    }

    @Test
    void testIteratorRemoveRespectsSealedState() {
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        Iterator<String> iterator = descendingSet.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if ("one".equals(key)) {
                // while sealed, iterator.remove() must fail
                sealed = true;
                assertThrows(UnsupportedOperationException.class, iterator::remove);
                // when unsealed, iterator.remove() must succeed
                sealed = false;
                iterator.remove();
                assertFalse(sealableMap.containsKey("one"));
                assertFalse(descendingSet.contains("one"));
                break;
            }
        }
    }

    @Test
    void descendingKeySet_returnsSealableNavigableSetWithSameElements() {
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        // Verify the returned set is a SealableNavigableSet (by class name)
        assertTrue(descendingSet.getClass().getSimpleName().contains("SealableNavigableSet"), "descendingKeySet should return a SealableNavigableSet");
        // The set should contain exactly the keys of the underlying map in descending order
        List<String> expected = new ArrayList<>(map.descendingKeySet());
        List<String> actual = new ArrayList<>(descendingSet);
        assertEquals(expected, actual, "Elements of descendingKeySet must match underlying map's descendingKeySet");
    }

    @Test
    void descendingKeySet_onEmptyMap_returnsEmptySealableNavigableSet() {
        // Create an empty underlying map
        NavigableMap<String, Integer> emptyMap = new TreeMap<>();
        SealableNavigableMap<String, Integer> emptySealable = new SealableNavigableMap<>(emptyMap, sealedSupplier);
        NavigableSet<String> descendingSet = emptySealable.descendingKeySet();
        assertTrue(descendingSet.isEmpty(), "descendingKeySet of an empty map should be empty");
    }

    @Test
    void descendingKeySet_iteratorTraversesInReverseNaturalOrder() {
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        // Expected order is the natural order reversed
        List<String> expected = new ArrayList<>(map.descendingKeySet());
        List<String> actual = new ArrayList<>();
        for (String key : descendingSet) {
            actual.add(key);
        }
        assertEquals(expected, actual, "Iterator should traverse keys in descending (reverse natural) order");
    }

    @Test
    void descendingKeySet_reflectsChangesMadeAfterObtainingSet() {
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        // Add a new entry after the set has been obtained
        sealableMap.put("zero", 0);
        // The new key must appear in the correct position (lowest key in descending order)
        assertTrue(descendingSet.contains("zero"));
        Iterator<String> it = descendingSet.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        List<String> expected = new ArrayList<>(map.descendingKeySet());
        assertEquals(expected, iterated, "Set should reflect modifications to the underlying map");
    }

    @Test
    void descendingKeySet_comparatorIsReverseOrderWhenUnderlyingUsesNaturalOrdering() {
        // Underlying map uses natural ordering (TreeMap with nulls not allowed)
        NavigableMap<String, Integer> naturalMap = new TreeMap<>();
        naturalMap.put("a", 1);
        naturalMap.put("b", 2);
        naturalMap.put("c", 3);
        SealableNavigableMap<String, Integer> naturalSealable = new SealableNavigableMap<>(naturalMap, sealedSupplier);
        NavigableSet<String> descendingSet = naturalSealable.descendingKeySet();
        Comparator<? super String> comparator = descendingSet.comparator();
        assertNotNull(comparator, "Comparator of descending set should not be null for natural ordering");
        // Verify that the comparator imposes the reverse of natural order
        assertTrue(comparator.compare("c", "b") < 0, "Comparator should order \"c\" before \"b\" (reverse order)");
        assertTrue(comparator.compare("b", "a") < 0, "Comparator should order \"b\" before \"a\" (reverse order)");
    }

    @Test
    void descendingKeySet_containsDelegatesToUnderlyingMap() {
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        assertTrue(descendingSet.contains("two"));
        assertFalse(descendingSet.contains("nonexistent"));
    }

    @Test
    void descendingKeySet_sizeMatchesUnderlyingMapSize() {
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        assertEquals(map.size(), descendingSet.size(), "Size of descendingKeySet must equal size of the underlying map");
    }

    @Test
    void descendingKeySet_descendingIteratorYieldsAscendingOrder() {
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        // The descending iterator of the descending set should iterate in the original (ascending) order
        Iterator<String> doubleDescIter = descendingSet.descendingIterator();
        // natural order
        List<String> expectedAscending = new ArrayList<>(map.navigableKeySet());
        List<String> actual = new ArrayList<>();
        doubleDescIter.forEachRemaining(actual::add);
        assertEquals(expectedAscending, actual, "descendingIterator on descendingKeySet should produce keys in ascending (original) order");
    }

    @Test
    void descendingKeySet_removeWhenUnsealedSucceeds() {
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        assertTrue(descendingSet.remove("two"));
        assertFalse(sealableMap.containsKey("two"));
        assertFalse(descendingSet.contains("two"));
    }

    @Test
    void descendingKeySet_removeWhenSealedThrows() {
        // seal the map
        sealed = true;
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.remove("two"));
    }

    @Test
    void descendingKeySet_addWhenSealedThrows() {
        sealed = true;
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        assertThrows(UnsupportedOperationException.class, () -> descendingSet.add("newKey"));
    }

    @Test
    void descendingKeySet_pollFirstWhenSealedThrows() {
        sealed = true;
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        assertThrows(UnsupportedOperationException.class, descendingSet::pollFirst);
    }

    @Test
    void iterator_removeRespectsSealedState() {
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        Iterator<String> iterator = descendingSet.iterator();
        while (iterator.hasNext()) {
            String key = iterator.next();
            if ("one".equals(key)) {
                // while sealed, iterator.remove() must fail
                sealed = true;
                assertThrows(UnsupportedOperationException.class, iterator::remove);
                // when unsealed, iterator.remove() must succeed
                sealed = false;
                iterator.remove();
                assertFalse(sealableMap.containsKey("one"));
                assertFalse(descendingSet.contains("one"));
                break;
            }
        }
    }

    @Test
    void descendingKeySet_iteratorNextOnEmptySetThrowsNoSuchElementException() {
        NavigableMap<String, Integer> emptyMap = new TreeMap<>();
        SealableNavigableMap<String, Integer> emptySealable = new SealableNavigableMap<>(emptyMap, sealedSupplier);
        NavigableSet<String> emptySet = emptySealable.descendingKeySet();
        Iterator<String> iterator = emptySet.iterator();
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    void descendingKeySet_addNullOnMapDisallowingNullKeysThrowsNullPointerException() {
        // TreeMap does not allow null keys with natural ordering
        NavigableMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("a", 1);
        SealableNavigableMap<String, Integer> sealable = new SealableNavigableMap<>(treeMap, sealedSupplier);
        NavigableSet<String> descendingSet = sealable.descendingKeySet();
        assertThrows(NullPointerException.class, () -> descendingSet.add(null));
    }

    @Test
    void descendingKeySet_addIncompatibleKeyTypeThrowsClassCastException() {
        // Underlying map uses a comparator that only accepts Strings
        NavigableMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("a", 1);
        SealableNavigableMap<String, Integer> sealable = new SealableNavigableMap<>(treeMap, sealedSupplier);
        NavigableSet<String> descendingSet = sealable.descendingKeySet();
        // Attempt to add an Integer which cannot be compared to existing String keys
        assertThrows(ClassCastException.class, () -> descendingSet.add((String) (Object) 123));
    }

    @Test
    void descendingKeySet_addWhenSealedThrowsIllegalStateExceptionOrUnsupportedOperationException() {
        sealed = true;
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        // The concrete exception type depends on SealableNavigableSet implementation.
        // Accept either IllegalStateException or UnsupportedOperationException.
        assertThrows(RuntimeException.class, () -> descendingSet.add("newKey"));
    }

    @Test
    void descendingKeySet_removeWhenSealedThrowsIllegalStateExceptionOrUnsupportedOperationException() {
        sealed = true;
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        assertThrows(RuntimeException.class, () -> descendingSet.remove("one"));
    }

    @Test
    void descendingKeySet_pollFirstWhenSealedThrowsIllegalStateExceptionOrUnsupportedOperationException() {
        sealed = true;
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        assertThrows(RuntimeException.class, descendingSet::pollFirst);
    }

    @ParameterizedTest
    @EnumSource(MutatingOperation.class)
    void mutatingOperationsWhenSealedThrow(MutatingOperation op) {
        sealed = true;
        NavigableSet<String> descendingSet = sealableMap.descendingKeySet();
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            switch(op) {
                case ADD ->
                    descendingSet.add("newKey");
                case REMOVE ->
                    descendingSet.remove("one");
                case POLL_FIRST ->
                    descendingSet.pollFirst();
            }
        });
        // Accept either IllegalStateException or UnsupportedOperationException
        assertTrue(thrown instanceof IllegalStateException || thrown instanceof UnsupportedOperationException, "Expected IllegalStateException or UnsupportedOperationException, got: " + thrown);
    }
}
