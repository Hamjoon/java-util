package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;
// corrected import
// import java.util.concurrent.ConcurrentModificationException;

@ExtendWith(MockitoExtension.class)
class CompactMap_equals_Test {

    private static class ThrowingMap<K, V> extends LinkedHashMap<K, V> {

        @Override
        public boolean equals(Object o) {
            throw new UnsupportedOperationException("equals deliberately broken");
        }
    }

    private CompactMap<String, Integer> newCompactMap(final int compactSize) {
        return new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return compactSize;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "singleKey";
            }
        };
    }

    private static Stream<Arguments> compactArrayScenarios() {
        CompactMap<String, Integer> compact = new CompactMap_equals_Test().newCompactMap(5);
        compact.put("one", 1);
        compact.put("two", 2);
        Map<String, Integer> same = new HashMap<>();
        same.put("one", 1);
        same.put("two", 2);
        Map<String, Integer> missingKey = new HashMap<>();
        missingKey.put("one", 1);
        Map<String, Integer> nullVsNonNull = new HashMap<>();
        nullVsNonNull.put("one", null);
        nullVsNonNull.put("two", 2);
        CompactMap<String, Integer> compactWithNull = new CompactMap_equals_Test().newCompactMap(5);
        compactWithNull.put("one", null);
        compactWithNull.put("two", 2);
        Map<String, Integer> nullMatch = new HashMap<>();
        nullMatch.put("one", null);
        nullMatch.put("two", 2);
        Map<String, Integer> valueNotEqual = new HashMap<>();
        valueNotEqual.put("one", 1);
        valueNotEqual.put("two", 99);
        return Stream.of(Arguments.of("Compact array – matching entries", compact, same, true), Arguments.of("Compact array – missing key", compact, missingKey, false), Arguments.of("Compact array – null vs non‑null value", compact, nullVsNonNull, false), Arguments.of("Compact array – both null values", compactWithNull, nullMatch, true), Arguments.of("Compact array – value not equal", compact, valueNotEqual, false));
    }

    private static Stream<Arguments> backingMapScenarios() {
        CompactMap<String, Integer> backing = new CompactMap_equals_Test().newCompactMap(2);
        backing.put("a", 10);
        backing.put("b", 20);
        backing.put("c", 30);
        Map<String, Integer> equal = new LinkedHashMap<>();
        equal.put("a", 10);
        equal.put("b", 20);
        equal.put("c", 30);
        Map<String, Integer> notEqual = new LinkedHashMap<>();
        notEqual.put("a", 10);
        notEqual.put("b", 20);
        notEqual.put("c", 999);
        return Stream.of(Arguments.of("Backing map – identical content", backing, equal, true), Arguments.of("Backing map – different content", backing, notEqual, false));
    }

    private static Stream<Arguments> singleEntryScenarios() {
        CompactMap<String, Integer> single = new CompactMap_equals_Test().newCompactMap(5);
        single.put("key", 42);
        Map<String, Integer> same = new HashMap<>();
        same.put("key", 42);
        Map<String, Integer> different = new HashMap<>();
        different.put("key", 99);
        Map<String, Integer> treeSame = new TreeMap<>();
        treeSame.put("key", 42);
        return Stream.of(Arguments.of("Single entry – same entry", single, same, true), Arguments.of("Single entry – different value", single, different, false), Arguments.of("Single entry – TreeMap same entry", single, treeSame, true));
    }

    private static Stream<Arguments> nullKeyScenarios() {
        CompactMap<String, Integer> mapNullNull = new CompactMap_equals_Test().newCompactMap(5);
        mapNullNull.put(null, null);
        Map<String, Integer> otherNullNull = new HashMap<>();
        otherNullNull.put(null, null);
        CompactMap<String, Integer> mapNullNonNull = new CompactMap_equals_Test().newCompactMap(5);
        mapNullNonNull.put(null, 1);
        Map<String, Integer> otherNullNonNull = new HashMap<>();
        otherNullNonNull.put(null, 2);
        CompactMap<String, Integer> mapNullSameValue = new CompactMap_equals_Test().newCompactMap(5);
        mapNullSameValue.put(null, 7);
        Map<String, Integer> otherNullSameValue = new HashMap<>();
        otherNullSameValue.put(null, 7);
        return Stream.of(Arguments.of("Null key & null values equal", mapNullNull, otherNullNull, true), Arguments.of("Null key – null vs non‑null value", mapNullNonNull, otherNullNonNull, false), Arguments.of("Null key – same non‑null value", mapNullSameValue, otherNullSameValue, true));
    }

    private static Stream<Arguments> nestedMapScenarios() {
        CompactMap<String, Map<String, Integer>> compact = new CompactMap_equals_Test().newCompactMapGeneric(5);
        Map<String, Integer> inner1 = new HashMap<>();
        inner1.put("a", 1);
        compact.put("outer", inner1);
        Map<String, Map<String, Integer>> otherEqual = new HashMap<>();
        Map<String, Integer> innerEqual = new HashMap<>();
        innerEqual.put("a", 1);
        otherEqual.put("outer", innerEqual);
        Map<String, Map<String, Integer>> otherDiff = new HashMap<>();
        Map<String, Integer> innerDiff = new HashMap<>();
        innerDiff.put("a", 99);
        otherDiff.put("outer", innerDiff);
        return Stream.of(Arguments.of("Nested map values – equal", compact, otherEqual, true), Arguments.of("Nested map values – different", compact, otherDiff, false));
    }

    private <K, V> CompactMap<K, V> newCompactMapGeneric(final int compactSize) {
        @SuppressWarnings("unchecked")
        K singleKey = (K) "singleKey";
        return new CompactMap<K, V>() {

            @Override
            protected int compactSize() {
                return compactSize;
            }

            @Override
            protected Map<K, V> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected K getSingleValueKey() {
                return singleKey;
            }
        };
    }

    /**
     * Helper that matches the lowercase name used in many test methods.
     */
    private CompactMap<String, Integer> newcompactMap(final int compactSize) {
        return newCompactMap(compactSize);
    }

    @Test
    void equalsSameInstance() {
        CompactMap<String, Integer> map = newCompactMap(3);
        assertTrue(map.equals(map), "A map must be equal to itself");
    }

    @Test
    void equalsDifferentType() {
        CompactMap<String, Integer> map = newCompactMap(3);
        assertFalse(map.equals("not a map"), "A CompactMap must not be equal to an object of another type");
    }

    @Test
    void equalsEmptyMaps() {
        CompactMap<String, Integer> map1 = newCompactMap(3);
        CompactMap<String, Integer> map2 = newCompactMap(5);
        assertTrue(map1.equals(map2), "Two empty CompactMaps must be equal");
    }

    @Test
    void equalsEmptyAndNonEmpty() {
        CompactMap<String, Integer> empty = newcompactMap(3);
        CompactMap<String, Integer> nonEmpty = newcompactMap(3);
        nonEmpty.put("a", 1);
        assertFalse(empty.equals(nonEmpty), "Empty map must not equal a non‑empty map");
        assertFalse(nonEmpty.equals(empty), "Non‑empty map must not equal an empty map");
    }

    @Test
    void equalsSingleEntry() {
        CompactMap<String, Integer> map1 = newcompactMap(3);
        CompactMap<String, Integer> map2 = newcompactMap(4);
        map1.put("key", 42);
        map2.put("key", 42);
        assertTrue(map1.equals(map2), "Maps with the same single entry must be equal");
    }

    @Test
    void equalsSingleEntryDifferentValue() {
        CompactMap<String, Integer> map1 = newcompactMap(3);
        CompactMap<String, Integer> map2 = newcompactMap(3);
        map1.put("key", 1);
        map2.put("key", 2);
        assertFalse(map1.equals(map2), "Maps with the same key but different values must not be equal");
    }

    @Test
    void equalsCompactArrayState() {
        CompactMap<String, Integer> compact = newcompactMap(5);
        compact.put("one", 1);
        compact.put("two", 2);
        Map<String, Integer> regular = new HashMap<>();
        regular.put("one", 1);
        regular.put("two", 2);
        assertTrue(compact.equals(regular), "CompactMap in compact‑array state must equal a regular map with same entries");
        assertTrue(regular.equals(compact), "Regular map must equal CompactMap in compact‑array state");
    }

    @Test
    void equalsCompactArrayMissingEntry() {
        CompactMap<String, Integer> compact = newcompactMap(5);
        compact.put("one", 1);
        compact.put("two", 2);
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        assertFalse(compact.equals(other), "CompactMap must not equal a map that is missing entries");
    }

    @Test
    void equalsBackingMapState() {
        CompactMap<String, Integer> backing = newcompactMap(2);
        backing.put("a", 10);
        backing.put("b", 20);
        backing.put("c", 30);
        Map<String, Integer> reference = new LinkedHashMap<>();
        reference.put("a", 10);
        reference.put("b", 20);
        reference.put("c", 30);
        assertTrue(backing.equals(reference), "CompactMap in backing‑map state must equal a map with the same entries");
        assertTrue(reference.equals(backing), "Regular map must equal CompactMap in backing‑map state");
    }

    @Test
    void equalsWithNullValues() {
        CompactMap<String, Integer> mapWithNull = newcompactMap(3);
        mapWithNull.put("x", null);
        mapWithNull.put("y", 5);
        Map<String, Integer> other = new HashMap<>();
        other.put("x", null);
        other.put("y", 5);
        assertTrue(mapWithNull.equals(other), "Maps containing null values must compare correctly");
    }

    @Test
    void equalsDifferentOrderShouldStillBeTrue() {
        CompactMap<String, Integer> map = newcompactMap(3);
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);
        Map<String, Integer> other = new LinkedHashMap<>();
        other.put("third", 3);
        other.put("first", 1);
        other.put("second", 2);
        assertTrue(map.equals(other), "Equality must be based on entries, not on iteration order");
    }

    @Test
    void equalsAgainstSelfFromDifferentImplementation() {
        CompactMap<String, Integer> compact = newcompactMap(4);
        compact.put("k1", 100);
        compact.put("k2", 200);
        Map<String, Integer> hash = new HashMap<>();
        hash.put("k1", 100);
        hash.put("k2", 200);
        assertTrue(compact.equals(hash));
        assertTrue(hash.equals(compact));
    }

    @Test
    void equalsNullArgument() {
        CompactMap<String, Integer> map = newcompactMap(3);
        assertFalse(map.equals(null), "equals(null) must return false");
    }

    @Test
    void equalsSizeMismatch() {
        CompactMap<String, Integer> mapWithOneEntry = newcompactMap(3);
        mapWithOneEntry.put("k1", 10);
        Map<String, Integer> largerMap = new HashMap<>();
        largerMap.put("k1", 10);
        largerMap.put("k2", 20);
        assertFalse(mapWithOneEntry.equals(largerMap), "Maps of different size must not be equal");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("compactArrayScenarios")
    void compactArrayRepresentation(String description, CompactMap<String, Integer> compact, Map<String, Integer> other, boolean expected) {
        if (expected) {
            assertTrue(compact.equals(other), description);
            assertTrue(other.equals(compact), "Symmetric check: " + description);
        } else {
            assertFalse(compact.equals(other), description);
            assertFalse(other.equals(compact), "Symmetric check: " + description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("backingMapScenarios")
    void backingMapRepresentation(String description, CompactMap<String, Integer> compact, Map<String, Integer> other, boolean expected) {
        if (expected) {
            assertTrue(compact.equals(other), description);
            assertTrue(other.equals(compact), "Symmetric check: " + description);
        } else {
            assertFalse(compact.equals(other), description);
            assertFalse(other.equals(compact), "Symmetric check: " + description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("singleEntryScenarios")
    void singleEntryFallback(String description, CompactMap<String, Integer> compact, Map<String, Integer> other, boolean expected) {
        if (expected) {
            assertTrue(compact.equals(other), description);
            assertTrue(other.equals(compact), "Symmetric check: " + description);
        } else {
            assertFalse(compact.equals(other), description);
            assertFalse(other.equals(compact), "Symmetric check: " + description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullKeyScenarios")
    void nullKeyHandling(String description, CompactMap<String, Integer> compact, Map<String, Integer> other, boolean expected) {
        if (expected) {
            assertTrue(compact.equals(other), description);
        } else {
            assertFalse(compact.equals(other), description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nestedMapScenarios")
    void nestedMapValues(String description, CompactMap<String, Map<String, Integer>> compact, Map<String, Map<String, Integer>> other, boolean expected) {
        if (expected) {
            assertTrue(compact.equals(other), description);
        } else {
            assertFalse(compact.equals(other), description);
        }
    }

    @Test
    void concurrentModificationExceptionDuringIteration() {
        CompactMap<String, Integer> compact = newcompactMap(5);
        compact.put("k1", 1);
        compact.put("k2", 2);
        @SuppressWarnings("unchecked")
        Map<String, Integer> cmeMap = mock(Map.class);
        Set<Map.Entry<String, Integer>> entrySet = mock(Set.class);
        Iterator<Map.Entry<String, Integer>> iterator = mock(Iterator.class);
        when(cmeMap.entrySet()).thenReturn(entrySet);
        when(entrySet.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenThrow(new ConcurrentModificationException("mock CME"));
        when(cmeMap.size()).thenReturn(2);
        assertThrows(ConcurrentModificationException.class, () -> compact.equals(cmeMap), "CME from other.map's iterator must propagate");
    }

    @Test
    void exceptionFromSizeCall() {
        @SuppressWarnings("unchecked")
        Map<String, Integer> badSizeMap = mock(Map.class);
        when(badSizeMap.size()).thenThrow(new IllegalStateException("size broken"));
        CompactMap<String, Integer> compact = newcompactMap(5);
        compact.put("a", 1);
        compact.put("b", 2);
        assertThrows(IllegalStateException.class, () -> compact.equals(badSizeMap), "Exception from size() should propagate");
    }

    @Test
    void exceptionFromInternalMapDelegation() {
        CompactMap<String, Integer> compact = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new ThrowingMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "singleKey";
            }
        };
        compact.put("x", 1);
        compact.put("y", 2);
        compact.put("z", 3);
        Map<String, Integer> other = new HashMap<>();
        other.put("x", 1);
        other.put("y", 2);
        other.put("z", 3);
        assertThrows(UnsupportedOperationException.class, () -> compact.equals(other), "equals should delegate to internal map and propagate its exception");
    }

    @Test
    void exceptionFromEntrySetEqualsFallback() {
        CompactMap<String, Integer> compact = newcompactMap(5);
        compact.put("only", 1);
        @SuppressWarnings("unchecked")
        Map<String, Integer> badEntrySetMap = mock(Map.class);
        Set<Map.Entry<String, Integer>> badSet = mock(Set.class);
        when(badEntrySetMap.entrySet()).thenReturn(badSet);
        when(badSet.equals(any())).thenThrow(new RuntimeException("entrySet equals broken"));
        when(badEntrySetMap.size()).thenReturn(1);
        when(badEntrySetMap.isEmpty()).thenReturn(false);
        assertThrows(RuntimeException.class, () -> compact.equals(badEntrySetMap), "Exception from entrySet().equals must propagate");
    }
}