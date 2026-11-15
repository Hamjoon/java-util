package com.cedarsoftware.util;

import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_descendingMap_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @Nested
    @DisplayName("Empty map behavior")
    class EmptyMapTests {

        @Test
        void descendingMapOnEmptyMapIsEmpty() {
            SealableNavigableMap<Integer, String> emptyMap = new SealableNavigableMap<>(new java.util.TreeMap<>(), () -> false);
            NavigableMap<Integer, String> descending = emptyMap.descendingMap();
            assertEquals(0, descending.size());
            assertTrue(descending.isEmpty());
            assertNull(safeFirstKey(descending));
            assertNull(safeLastKey(descending));
            assertTrue(descending.keySet().isEmpty());
        }

        // Helper that swallows NPE from firstKey()/lastKey() on empty maps.
        private <K, V> K safeFirstKey(NavigableMap<K, V> map) {
            try {
                return map.firstKey();
            } catch (Exception e) {
                return null;
            }
        }

        private <K, V> K safeLastKey(NavigableMap<K, V> map) {
            try {
                return map.lastKey();
            } catch (Exception e) {
                return null;
            }
        }
    }

    @Nested
    @DisplayName("Descending order correctness")
    class DescendingOrderTests {

        private SealableNavigableMap<Integer, String> map;

        @BeforeEach
        void init() {
            NavigableMap<Integer, String> base = new java.util.TreeMap<>();
            base.put(1, "A");
            base.put(2, "B");
            base.put(3, "C");
            base.put(4, "D");
            base.put(5, "E");
            map = new SealableNavigableMap<>(base, () -> false);
        }

        @Test
        void viewHasReverseOrderAndCorrectFirstKey() {
            NavigableMap<Integer, String> descending = map.descendingMap();
            // Verify size and key order
            assertEquals(5, descending.size());
            assertEquals(Integer.valueOf(5), descending.firstKey());
            assertEquals(Integer.valueOf(1), descending.lastKey());
            // Check specific entry
            assertEquals("C", descending.get(3));
            // Verify the ordering of keySet()
            List<Integer> expected = List.of(5, 4, 3, 2, 1);
            List<Integer> actual = new ArrayList<>(descending.keySet());
            assertEquals(expected, actual);
        }

        @Test
        void pollFirstAndLastEntryRemoveCorrectElements() {
            NavigableMap<Integer, String> descending = map.descendingMap();
            // pollFirstEntry should remove the highest key (5)
            Map.Entry<Integer, String> firstPolled = descending.pollFirstEntry();
            assertEquals(Integer.valueOf(5), firstPolled.getKey());
            // pollLastEntry should remove the lowest key (1)
            Map.Entry<Integer, String> lastPolled = descending.pollLastEntry();
            assertEquals(Integer.valueOf(1), lastPolled.getKey());
            // Remaining size and order
            assertEquals(3, descending.size());
            List<Integer> expected = List.of(4, 3, 2);
            List<Integer> actual = new ArrayList<>(descending.keySet());
            assertEquals(expected, actual);
        }
    }

    @Nested
    @DisplayName("Sealed behavior propagation")
    class SealedPropagationTests {

        @Test
        void descendingMapIsSealedWhenSupplierAlwaysTrue() {
            Supplier<Boolean> alwaysSealed = () -> true;
            SealableNavigableMap<Integer, String> sealedMap = new SealableNavigableMap<>(new java.util.TreeMap<>(), alwaysSealed);
            NavigableMap<Integer, String> descending = sealedMap.descendingMap();
            // Any mutating operation must throw IllegalStateException (as per implementation)
            IllegalStateException ise = assertThrows(IllegalStateException.class, () -> descending.put(10, "X"));
            assertTrue(ise.getMessage().toLowerCase().contains("sealed"));
        }

        @Test
        void directModificationOnSealedOriginalThrows() {
            Supplier<Boolean> alwaysSealed = () -> true;
            SealableNavigableMap<Integer, String> sealedMap = new SealableNavigableMap<>(new java.util.TreeMap<>(), alwaysSealed);
            // The original map itself should also enforce the sealed contract.
            IllegalStateException ise = assertThrows(IllegalStateException.class, () -> sealedMap.put(10, "X"));
            assertTrue(ise.getMessage().toLowerCase().contains("sealed"));
        }

        @Test
        void descendingMapMutationMethodsThrowWhenSealed() {
            Supplier<Boolean> alwaysSealed = () -> true;
            SealableNavigableMap<Integer, String> sealedMap = new SealableNavigableMap<>(new java.util.TreeMap<>(), alwaysSealed);
            NavigableMap<Integer, String> descending = sealedMap.descendingMap();
            IllegalStateException clearEx = assertThrows(IllegalStateException.class, descending::clear);
            assertTrue(clearEx.getMessage().toLowerCase().contains("sealed"));
            IllegalStateException putAllEx = assertThrows(IllegalStateException.class, () -> descending.putAll(Map.of(1, "A", 2, "B")));
            assertTrue(putAllEx.getMessage().toLowerCase().contains("sealed"));
        }
    }

    @Nested
    @DisplayName("Multiple descendingMap instances consistency")
    class MultipleDescendingInstancesTests {

        @Test
        void twoDescendingViewsShareUnderlyingMap() throws Exception {
            // original map with keys 1‑5
            NavigableMap<Integer, String> base = new java.util.TreeMap<>();
            for (int i = 1; i <= 5; i++) base.put(i, String.valueOf(i));
            SealableNavigableMap<Integer, String> original = new SealableNavigableMap<>(base, () -> false);
            NavigableMap<Integer, String> d1 = original.descendingMap();
            NavigableMap<Integer, String> d2 = original.descendingMap();
            // Verify both wrappers expose the same underlying NavigableMap instance
            NavigableMap<?, ?> underlying1 = getUnderlyingNavMap(d1);
            NavigableMap<?, ?> underlying2 = getUnderlyingNavMap(d2);
            assertSame(underlying1, underlying2, "Both descending maps should share the same underlying map");
            // Mutate via first wrapper and verify visibility in second
            d1.put(6, "6");
            assertEquals(6, d2.size());
            assertTrue(d2.containsKey(6));
        }

        // Reflection helper to fetch the private 'navMap' field.
        private NavigableMap<?, ?> getUnderlyingNavMap(NavigableMap<?, ?> map) throws Exception {
            Field f = SealableNavigableMap.class.getDeclaredField("navMap");
            f.setAccessible(true);
            return (NavigableMap<?, ?>) f.get(map);
        }
    }

    @Nested
    @DisplayName("Comparator reversal verification")
    class ComparatorReversalTests {

        @Test
        void comparatorIsReversedInDescendingView() {
            // natural order map with keys 1 and 2
            NavigableMap<Integer, String> base = new java.util.TreeMap<>();
            base.put(1, "A");
            base.put(2, "B");
            SealableNavigableMap<Integer, String> original = new SealableNavigableMap<>(base, () -> false);
            Comparator<Integer> originalComp = original.comparator();
            // TreeMap returns null for natural ordering; SealableNavigableMap likely mirrors that.
            // For the purpose of the test we treat null as natural order.
            assertNull(originalComp, "Original comparator should be null for natural ordering");
            NavigableMap<Integer, String> descending = original.descendingMap();
            Comparator<Integer> descendingComp = descending.comparator();
            // The descending comparator must order 1 after 2 (i.e., positive result for compare(1,2))
            assertNotNull(descendingComp, "Descending comparator must be non‑null");
            assertTrue(descendingComp.compare(1, 2) > 0, "Comparator of descending map should return positive when 1 < 2");
            assertTrue(descendingComp.compare(2, 1) < 0, "Comparator of descending map should return negative when 2 > 1");
        }
    }

    @Nested
    @DisplayName("Double descendingMap identity")
    class DoubleDescendingIdentityTests {

        @Test
        void doubleDescendingRestoresOriginalOrderingAndInstance() throws Exception {
            NavigableMap<Integer, String> base = new java.util.TreeMap<>();
            for (int i = 1; i <= 5; i++) base.put(i, String.valueOf(i));
            SealableNavigableMap<Integer, String> original = new SealableNavigableMap<>(base, () -> false);
            NavigableMap<Integer, String> doubleDesc = original.descendingMap().descendingMap();
            // Size and ordering must match the original
            assertEquals(5, doubleDesc.size());
            assertEquals(Integer.valueOf(1), doubleDesc.firstKey());
            assertEquals(Integer.valueOf(5), doubleDesc.lastKey());
            List<Integer> expected = List.of(1, 2, 3, 4, 5);
            List<Integer> actual = new ArrayList<>(doubleDesc.keySet());
            assertEquals(expected, actual);
            // Verify that the underlying navMap of doubleDesc is the same instance as the original's navMap
            Field f = SealableNavigableMap.class.getDeclaredField("navMap");
            f.setAccessible(true);
            NavigableMap<?, ?> originalUnderlying = (NavigableMap<?, ?>) f.get(original);
            NavigableMap<?, ?> doubleDescUnderlying = (NavigableMap<?, ?>) f.get(doubleDesc);
            assertSame(originalUnderlying, doubleDescUnderlying, "Double descending map should wrap the same underlying NavigableMap as the original");
        }
    }

    @Nested
    @DisplayName("SubMap view on descending map")
    class SubMapOnDescendingTests {

        @Test
        void subMapOnDescendingViewReturnsCorrectRange() {
            NavigableMap<Integer, String> base = new java.util.TreeMap<>();
            for (int i = 1; i <= 5; i++) base.put(i, String.valueOf(i));
            SealableNavigableMap<Integer, String> original = new SealableNavigableMap<>(base, () -> false);
            NavigableMap<Integer, String> descending = original.descendingMap();
            NavigableMap<Integer, String> sub = descending.subMap(4, true, 2, false);
            // Expected keys: 4 and 3 (descending order)
            assertEquals(2, sub.size());
            List<Integer> expected = List.of(4, 3);
            List<Integer> actual = new ArrayList<>(sub.keySet());
            assertEquals(expected, actual);
            assertEquals(Integer.valueOf(4), sub.firstKey());
            assertEquals(Integer.valueOf(3), sub.lastKey());
            assertTrue(sub.containsKey(4));
            assertFalse(sub.containsKey(2));
        }
    }

    @Nested
    @DisplayName("KeySet and EntrySet order")
    class KeySetEntrySetOrderTests {

        @Test
        void descendingKeySetAndEntrySetIterateInReverseOrder() {
            NavigableMap<Integer, String> base = new java.util.TreeMap<>();
            for (int i = 1; i <= 5; i++) base.put(i, String.valueOf(i));
            SealableNavigableMap<Integer, String> original = new SealableNavigableMap<>(base, () -> false);
            NavigableMap<Integer, String> descending = original.descendingMap();
            // keySet order
            List<Integer> keySetOrder = new ArrayList<>(descending.keySet());
            List<Integer> expected = List.of(5, 4, 3, 2, 1);
            assertEquals(expected, keySetOrder);
            assertEquals(5, descending.keySet().size());
            // entrySet order (iterate over entries and collect keys)
            List<Integer> entrySetOrder = new ArrayList<>();
            for (Map.Entry<Integer, String> e : descending.entrySet()) {
                entrySetOrder.add(e.getKey());
            }
            assertEquals(expected, entrySetOrder);
            assertEquals(5, descending.entrySet().size());
        }
    }

    @Nested
    @DisplayName("Single element map behavior")
    class SingleElementMapTests {

        @Test
        void singleEntryMapDescendingViewBehavesCorrectly() {
            NavigableMap<Integer, String> base = new java.util.TreeMap<>();
            base.put(42, "X");
            SealableNavigableMap<Integer, String> original = new SealableNavigableMap<>(base, () -> false);
            NavigableMap<Integer, String> descending = original.descendingMap();
            assertEquals(1, descending.size());
            assertEquals(Integer.valueOf(42), descending.firstKey());
            assertEquals(Integer.valueOf(42), descending.lastKey());
            assertEquals("X", descending.get(42));
            // pollFirstEntry should remove the only entry
            Map.Entry<Integer, String> polledFirst = descending.pollFirstEntry();
            assertEquals(Integer.valueOf(42), polledFirst.getKey());
            assertTrue(descending.isEmpty());
            // pollLastEntry on an empty map returns null
            assertNull(descending.pollLastEntry());
        }
    }

    @Nested
    @DisplayName("Modification visibility between original and descending map")
    class ModificationVisibilityTests {

        @Test
        void putOnDescendingReflectsInOriginalAndRemoveOnOriginalReflectsInDescending() {
            // Original map with keys 1‑3
            NavigableMap<Integer, String> base = new java.util.TreeMap<>();
            base.put(1, "A");
            base.put(2, "B");
            base.put(3, "C");
            SealableNavigableMap<Integer, String> original = new SealableNavigableMap<>(base, () -> false);
            NavigableMap<Integer, String> descending = original.descendingMap();
            // Put via descending view
            descending.put(4, "D");
            assertEquals(4, original.size());
            assertTrue(original.containsKey(4));
            List<Integer> expectedAfterPut = List.of(4, 3, 2, 1);
            assertEquals(expectedAfterPut, new ArrayList<>(descending.keySet()));
            // Remove via original map
            original.remove(2);
            assertEquals(3, descending.size());
            List<Integer> expectedAfterRemove = List.of(4, 3, 1);
            assertEquals(expectedAfterRemove, new ArrayList<>(descending.keySet()));
        }
    }

    @Nested
    @DisplayName("Null underlying map handling")
    class NullUnderlyingMapTests {

        @Test
        void descendingMapThrowsNPEWhenUnderlyingMapIsNull() {
            // Construct with a null underlying map – the constructor itself does not guard against null.
            SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(null, () -> false);
            assertThrows(NullPointerException.class, map::descendingMap);
        }
    }

    @Nested
    @DisplayName("Null sealed supplier handling")
    class NullSealedSupplierTests {

        @Test
        void descendingMapThrowsNPEWhenSealedSupplierIsNull() {
            NavigableMap<Integer, String> base = new java.util.TreeMap<>();
            base.put(1, "A");
            // Pass null supplier; the constructor may accept it, but descendingMap should fail when it tries to use it.
            SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(base, null);
            assertThrows(NullPointerException.class, map::descendingMap);
        }
    }

    @Nested
    @DisplayName("Comparator reversal edge case when original comparator is non‑null")
    class NonNullComparatorReversalTests {

        @Test
        void descendingComparatorReversesCustomComparator() {
            // Custom comparator that orders integers in reverse already.
            Comparator<Integer> reverse = Collections.reverseOrder();
            NavigableMap<Integer, String> base = new java.util.TreeMap<>(reverse);
            base.put(1, "A");
            base.put(2, "B");
            base.put(3, "C");
            SealableNavigableMap<Integer, String> original = new SealableNavigableMap<>(base, () -> false);
            // Original comparator should be the reverse order comparator.
            assertSame(reverse, original.comparator());
            // Descending view should flip it back to natural order.
            Comparator<Integer> descComp = original.descendingMap().comparator();
            assertNotNull(descComp);
            assertEquals(0, descComp.compare(2, 2));
            assertTrue(descComp.compare(1, 2) < 0, "Descending comparator should now order 1 before 2 (natural order)");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The map starts unsealed.
        sealed = false;
        sealedSupplier = () -> sealed;
        // Populate a simple backing map.
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("alpha", 1);
        backingMap.put("beta", 2);
        backingMap.put("gamma", 3);
        // exercise null handling – the original class permits it
        backingMap.put(null, null);
        // Wrap it with the sealable implementation.
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC and avoid cross‑test contamination.
        backingMap = null;
        sealableMap = null;
        sealedSupplier = null;
    }

    @Test
    void descendingMapContainsAllEntriesAndReflectsChanges() {
        // GIVEN a sealable map with several entries
        sealableMap.put("delta", 4);
        // WHEN we obtain the descending view
        NavigableMap<String, Integer> descending = sealableMap.descendingMap();
        // THEN the view must contain the same keys (including null) and in reverse order.
        assertTrue(descending.containsKey("gamma"));
        assertTrue(descending.containsKey("beta"));
        assertTrue(descending.containsKey("alpha"));
        assertTrue(descending.containsKey("delta"));
        assertTrue(descending.containsKey(null));
        // The firstKey of the descending map should be the greatest key according to natural order.
        assertEquals("gamma", descending.firstKey());
        // WHEN we modify the original map after the view has been created
        sealableMap.put("epsilon", 5);
        // THEN the change must be visible in the descending view.
        assertTrue(descending.containsKey("epsilon"));
        assertEquals(Integer.valueOf(5), descending.get("epsilon"));
    }

    @Test
    void descendingMapRespectsSealedState() {
        // GIVEN an unsealed map and its descending view
        NavigableMap<String, Integer> descending = sealableMap.descendingMap();
        assertFalse(sealedSupplier.get());
        // WHEN the map is sealed
        sealed = true;
        // THEN any mutating operation on the descending view must throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> descending.put("zeta", 26));
        assertThrows(UnsupportedOperationException.class, () -> descending.remove("alpha"));
        // Also test iterator‑based removal.
        Iterator<String> it = descending.keySet().iterator();
        assertTrue(it.hasNext());
        // advance to first element
        it.next();
        assertThrows(UnsupportedOperationException.class, it::remove);
    }
}
