package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LockingLRUCacheStrategy_equals_Test {

    private static final int CAPACITY = 10;

    private LockingLRUCacheStrategy<String, Integer> cache;

    // ---------- Helper data structures for parameterized tests ----------
    private static class TestCase {

        final String description;

        final Map<?, ?> otherMap;

        // Boolean or Class<? extends Throwable>
        final Object expected;

        TestCase(String description, Map<?, ?> otherMap, Object expected) {
            this.description = description;
            this.otherMap = otherMap;
            this.expected = expected;
        }
    }

    /**
     * Map whose entrySet() returns null
     */
    private static class MapWithNullEntrySet<K, V> implements Map<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return null;
        }

        // All other methods can be no‑ops or throw UnsupportedOperationException
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
        public V get(Object key) {
            return null;
        }

        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }
    }

    /**
     * Map whose entrySet() throws IllegalStateException
     */
    private static class MapWithExceptionOnEntrySet<K, V> implements Map<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new IllegalStateException("boom");
        }

        // Minimal stub implementations
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
        public V get(Object key) {
            return null;
        }

        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }
    }

    /**
     * Subclass of the cache that throws from entrySet()
     */
    private static class ThrowingEntrySetCache<K, V> extends LockingLRUCacheStrategy<K, V> {

        ThrowingEntrySetCache(int capacity) {
            super(capacity);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new IllegalArgumentException("entrySet broken");
        }
    }

    /**
     * Map whose entrySet() returns a Set whose equals() throws
     */
    private static class MapWithFaultyEntrySetEquals<K, V> implements Map<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<K, V>> iterator() {
                    return Collections.emptyIterator();
                }

                @Override
                public int size() {
                    return 0;
                }

                @Override
                public boolean equals(Object o) {
                    throw new UnsupportedOperationException("equals broken");
                }
            };
        }

        // Minimal stub implementations
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
        public V get(Object key) {
            return null;
        }

        @Override
        public V put(K key, V value) {
            return null;
        }

        @Override
        public V remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<K> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<V> values() {
            return Collections.emptyList();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        cache = new LockingLRUCacheStrategy<>(CAPACITY);
        // Populate the cache with a deterministic set of entries
        cache.put("one", 1);
        cache.put("two", 2);
        cache.put("three", 3);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the cache to avoid cross‑test contamination
        cache.clear();
    }

    // ---------- 1. Entry‑set equality – same content (including nulls) ----------
    private static Stream<TestCase> sameContentCases() {
        return Stream.of(new TestCase("Single entry with null value", mapOf(entry(null, "nullValue")), true), new TestCase("Single entry null→null", mapOf(entry(null, null)), true), new TestCase("LinkedHashMap with two entries (order differs)", linkedMapOf(entry("x", 1), entry("y", 2)), true), new TestCase("Another LockingLRUCacheStrategy with identical entries", new LockingLRUCacheStrategy<>(CAPACITY) {

            {
                put("one", 1);
                put("two", 2);
                put("three", 3);
            }
        }, true), new TestCase("TreeMap with same entries", new TreeMap<>(mapOf(entry("one", 1), entry("two", 2), entry("three", 3))), true), new TestCase("ConcurrentHashMap with same entry", new ConcurrentHashMap<>(mapOf(entry("c", 3))), true), new TestCase("HashMap with distinct but equal String value", mapOf(entry("key", new String("val"))), true));
    }

    // ---------- 2. Entry‑set equality – different content ----------
    private static Stream<TestCase> differentContentCases() {
        return Stream.of(new TestCase("Cache empty vs map with one entry", mapOf(entry("key1", "value1")), false), new TestCase("Both have entries but values differ", mapOf(entry("one", 10), entry("two", 2), entry("three", 3)), false), new TestCase("Same key, different value", mapOf(entry("one", 999)), false));
    }

    private static <K, V> Map.Entry<K, V> entry(K k, V v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    private static <K, V> Map<K, V> mapOf(Map.Entry<K, V>... entries) {
        Map<K, V> m = new HashMap<>();
        for (Map.Entry<K, V> e : entries) {
            m.put(e.getKey(), e.getValue());
        }
        return m;
    }

    private static <K, V> Map<K, V> linkedMapOf(Map.Entry<K, V>... entries) {
        Map<K, V> m = new LinkedHashMap<>();
        for (Map.Entry<K, V> e : entries) {
            m.put(e.getKey(), e.getValue());
        }
        return m;
    }

    @Test
    void equals_isReflexive() {
        assertTrue(cache.equals(cache), "Cache should be equal to itself (reflexivity)");
    }

    @Test
    void equals_nullReturnsFalse() {
        assertFalse(cache.equals(null), "Cache should not be equal to null");
    }

    @Test
    void equals_nonMapReturnsFalse() {
        assertFalse(cache.equals("not a map"), "Cache should not be equal to an object that is not a Map");
    }

    @Test
    void equals_sameEntriesDifferentMapImplementation_returnsTrue() {
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        other.put("three", 3);
        assertTrue(cache.equals(other), "Cache should be equal to another Map with the same entries");
    }

    @Test
    void equals_sameEntriesDifferentOrder_returnsTrue() {
        // HashMap does not guarantee order; we already test order‑independence above.
        // Here we create another LockingLRUCacheStrategy and insert entries in a different order.
        LockingLRUCacheStrategy<String, Integer> other = new LockingLRUCacheStrategy<>(CAPACITY);
        other.put("three", 3);
        other.put("one", 1);
        other.put("two", 2);
        assertTrue(cache.equals(other), "Cache should be equal to another LockingLRUCacheStrategy with same entries regardless of insertion order");
    }

    @Test
    void equals_differentEntries_returnsFalse() {
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        // "three" is missing, and an extra entry is added
        other.put("four", 4);
        assertFalse(cache.equals(other), "Cache should not be equal when entry sets differ");
    }

    @Test
    void equals_sameReferenceButDifferentContentAfterModification_returnsTrueBeforeModification() {
        // Verify that equality is based on the *current* entry set.
        Map<String, Integer> other = new HashMap<>(cache);
        assertTrue(cache.equals(other), "Initially both maps contain the same entries");
        // Mutate the cache; equality should now reflect the new state.
        cache.put("four", 4);
        other.put("four", 4);
        assertTrue(cache.equals(other), "After identical mutations, maps should still be equal");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("sameContentCases")
    void equals_sameContent(TestCase tc) {
        if (tc.expected instanceof Boolean) {
            assertEquals(tc.expected, cache.equals(tc.otherMap), tc.description);
        } else {
            fail("Unexpected expected type");
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("differentContentCases")
    void equals_differentContent(TestCase tc) {
        // Ensure cache is empty for the first case
        if (tc.description.contains("Cache empty")) {
            cache.clear();
        }
        assertEquals(tc.expected, cache.equals(tc.otherMap), tc.description);
    }

    // ---------- 3. Both maps empty ----------
    @Test
    void equals_bothEmptyMaps_returnsTrue() {
        cache.clear();
        Map<String, Integer> empty = new HashMap<>();
        assertTrue(cache.equals(empty), "Both the LockingLRUCacheStrategy instance and the other map are empty");
    }

    // ---------- 4. Other map entrySet returns null (NPE) ----------
    @Test
    void equals_otherEntrySetNull_throwsNPE() {
        Map<String, Integer> faulty = new MapWithNullEntrySet<>();
        assertThrows(NullPointerException.class, () -> cache.equals(faulty), "Calling equals with a map whose entrySet() returns null should throw NPE");
    }

    // ---------- 5. Other map entrySet throws IllegalStateException ----------
    @Test
    void equals_otherEntrySetThrowsException_propagates() {
        Map<String, Integer> faulty = new MapWithExceptionOnEntrySet<>();
        assertThrows(IllegalStateException.class, () -> cache.equals(faulty), "Exception thrown while obtaining other.entrySet() must propagate");
    }

    // ---------- 6. This entrySet throws IllegalArgumentException ----------
    @Test
    void equals_thisEntrySetThrowsException_propagates() {
        LockingLRUCacheStrategy<String, Integer> broken = new ThrowingEntrySetCache<>(CAPACITY);
        Map<String, Integer> other = new HashMap<>();
        other.put("one", 1);
        other.put("two", 2);
        other.put("three", 3);
        assertThrows(IllegalArgumentException.class, () -> broken.equals(other), "Exception from this.entrySet() must propagate");
    }

    // ---------- 7. Other entrySet equals throws UnsupportedOperationException ----------
    @Test
    void equals_otherEntrySetEqualsThrowsException_propagates() {
        Map<String, Integer> faulty = new MapWithFaultyEntrySetEquals<>();
        assertThrows(UnsupportedOperationException.class, () -> cache.equals(faulty), "Exception from other.entrySet().equals(...) must propagate");
    }
}
