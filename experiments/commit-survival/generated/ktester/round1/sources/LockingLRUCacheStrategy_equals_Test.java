package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

class LockingLRUCacheStrategy_equals_Test {

    private static final int CAPACITY = 5;

    private LockingLRUCacheStrategy<String, Integer> cache;

    /**
     * A subclass that deliberately returns {@code null} from {@code entrySet()}.
     */
    static class NullEntrySetCache<K, V> extends LockingLRUCacheStrategy<K, V> {

        NullEntrySetCache(int capacity) {
            super(capacity);
        }

        @Override
        public Set<Map.Entry<K, V>> entrySet() {
            // force NPE in equals()
            return null;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache for each test case.
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid side‑effects, although a new instance is created for each test.
        cache.clear();
    }

    static Stream<Arguments> mapComparisonProvider() {
        // Helper cache used only to build the expected entry sets.
        LockingLRUCacheStrategy<String, Integer> baseCache = new LockingLRUCacheStrategy<>(CAPACITY);
        // 1. emptyMap – both empty
        baseCache.clear();
        Map<String, Integer> emptyMap = new HashMap<>();
        // 2. mapWithSameEntries – identical entries
        baseCache.clear();
        baseCache.put("one", 1);
        baseCache.put("two", 2);
        baseCache.put("three", 3);
        Map<String, Integer> sameEntries = new HashMap<>(baseCache);
        // 3. mapWithDifferentEntries – one differing value
        baseCache.clear();
        baseCache.put("a", 10);
        baseCache.put("b", 20);
        Map<String, Integer> diffEntries = new HashMap<>();
        diffEntries.put("a", 10);
        // different value
        diffEntries.put("b", 999);
        // 4. mapWithSameKeysDifferentValues – same keys, different values
        baseCache.clear();
        baseCache.put("k1", 100);
        baseCache.put("k2", 200);
        Map<String, Integer> sameKeysDiffVals = new HashMap<>();
        sameKeysDiffVals.put("k1", 101);
        sameKeysDiffVals.put("k2", 200);
        return Stream.of(Arguments.of("Two empty caches have identical entrySet, should be equal.", emptyMap, true), Arguments.of("A regular HashMap containing the same key/value pairs as the cache should be equal.", sameEntries, true), Arguments.of("A Map with at least one differing entry should cause equals to return false.", diffEntries, false), Arguments.of("Same keys but different values lead to different entrySet, equals should be false.", sameKeysDiffVals, false));
    }

    @Test
    void equalsSameInstance_returnsTrue() {
        assertTrue(cache.equals(cache), "Cache should be equal to itself");
    }

    @Test
    void equalsNull_returnsFalse() {
        assertFalse(cache.equals(null), "Cache should not be equal to null");
    }

    @Test
    void equalsDifferentType_returnsFalse() {
        assertFalse(cache.equals("a string"), "Cache should not be equal to an object of a different type");
    }

    @Test
    void equalsEmptyMapWithSameEntries_returnsTrue() {
        // Populate cache and a regular HashMap with identical entries.
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        other.put("three", 3);
        assertTrue(cache.equals(other), "Cache should be equal to another map with the same entries");
        // Symmetry check
        assertTrue(other.equals(cache), "Other map should be equal to the cache (via Map.equals)");
    }

    @Test
    void equalsMapWithDifferentEntries_returnsFalse() {
        cache.put("a", 10);
        cache.put("b", 20);
        Map<String, Integer> other = new HashMap<>();
        other.put("a", 10);
        // different value
        other.put("b", 999);
        assertFalse(cache.equals(other), "Cache should not be equal when values differ");
    }

    @Test
    void equalsMapWithAdditionalEntry_returnsFalse() {
        cache.put("key", 100);
        Map<String, Integer> other = new HashMap<>(cache);
        // extra entry
        other.put("extra", 200);
        assertFalse(cache.equals(other), "Cache should not be equal when the other map has extra entries");
    }

    @Test
    void equalsCacheWithSameEntriesButDifferentOrder_returnsTrue() {
        // Populate cache
        cache.put("first", 1);
        cache.put("second", 2);
        cache.put("third", 3);
        // Access entries in a different order to mutate internal LRU ordering.
        cache.get("second");
        cache.get("first");
        // Build a regular map with the same entries but inserted in a different order.
        Map<String, Integer> other = new LinkedHashMap<>();
        other.put("third", 3);
        other.put("first", 1);
        other.put("second", 2);
        assertTrue(cache.equals(other), "Equals must depend only on entrySet, not on internal LRU order");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mapComparisonProvider")
    void equalsMapComparison(String description, Map<String, Integer> other, boolean expected) {
        // Populate the cache for the cases that need entries.
        // The provider builds the cache according to the scenario.
        assertEquals(expected, cache.equals(other), description);
    }

    @Test
    void equalsMapWithNullKeyAndValue_returnsTrue() {
        // Cache contains a null key mapping to null value
        cache.put(null, null);
        cache.put("present", 42);
        Map<String, Integer> other = new HashMap<>();
        other.put(null, null);
        other.put("present", 42);
        assertTrue(cache.equals(other), "Cache should be equal to a map with identical null key/value entries");
    }

    @Test
    void equalsMapMissingNullKey_returnsFalse() {
        cache.put(null, 1);
        cache.put("x", 2);
        Map<String, Integer> other = new HashMap<>();
        // null key missing
        other.put("x", 2);
        assertFalse(cache.equals(other), "Cache with a null‑key entry should not equal a map that lacks that entry");
    }

    @Test
    void equalsLinkedHashMapReversedOrder_returnsTrue() {
        cache.put("first", 1);
        cache.put("second", 2);
        cache.put("third", 3);
        // Insert in reverse order into a LinkedHashMap (order is preserved by insertion)
        LinkedHashMap<String, Integer> reversed = new LinkedHashMap<>();
        reversed.put("third", 3);
        reversed.put("second", 2);
        reversed.put("first", 1);
        assertTrue(cache.equals(reversed), "EntrySet equality must be order‑agnostic");
    }

    @Test
    void equalsEmptyMapWhenCacheHasOneEntry_returnsFalse() {
        cache.put("only", 1);
        Map<String, Integer> empty = new HashMap<>();
        assertFalse(cache.equals(empty), "Cache with one entry must not equal an empty map");
    }

    @Test
    void equalsMapWithExtraEntry_returnsFalse() {
        cache.put("a", 10);
        cache.put("b", 20);
        Map<String, Integer> extra = new HashMap<>(cache);
        // extra entry
        extra.put("c", 30);
        assertFalse(cache.equals(extra), "Cache should not equal a map that contains additional entries");
    }

    @Test
    void equalsOtherMapEntrySetThrowsUnsupportedOperationException() {
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.entrySet()).thenThrow(new UnsupportedOperationException("entrySet not supported"));
        assertThrows(UnsupportedOperationException.class, () -> cache.equals(mockMap), "UnsupportedOperationException from other.map.entrySet() must propagate");
    }

    @Test
    void equalsOtherMapEntrySetEqualsThrowsRuntimeException() {
        @SuppressWarnings("unchecked")
        Set<Map.Entry<String, Integer>> faultySet = mock(Set.class);
        when(faultySet.equals(any())).thenThrow(new RuntimeException("faulty equals"));
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.entrySet()).thenReturn(faultySet);
        assertThrows(RuntimeException.class, () -> cache.equals(mockMap), "RuntimeException from entrySet.equals must propagate");
    }

    @Test
    void equalsOtherMapEntrySetThrowsConcurrentModificationException() {
        @SuppressWarnings("unchecked")
        Set<Map.Entry<String, Integer>> cmeSet = mock(Set.class);
        when(cmeSet.equals(any())).thenThrow(new ConcurrentModificationException("CME"));
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.entrySet()).thenReturn(cmeSet);
        assertThrows(ConcurrentModificationException.class, () -> cache.equals(mockMap), "ConcurrentModificationException from entrySet.equals must propagate");
    }

    @Test
    void equalsWhenThisEntrySetIsNull_throwsNullPointerException() {
        NullEntrySetCache<String, Integer> brokenCache = new NullEntrySetCache<>(CAPACITY);
        brokenCache.put("a", 1);
        Map<String, Integer> regular = new HashMap<>();
        regular.put("a", 1);
        assertThrows(NullPointerException.class, () -> brokenCache.equals(regular), "Calling equals on a cache whose entrySet() returns null should throw NPE");
    }
}
