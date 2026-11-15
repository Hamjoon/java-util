package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AbstractConcurrentNullSafeMap_values_Test {

    private ConcurrentMap<Object, Object> internalMap;

    private TestConcurrentNullSafeMap<String, String> map;

    /**
     * A concrete implementation that does **no** masking – it simply returns the
     *  supplied keys/values unchanged. This lets us focus on the behaviour of the
     *  {@code values()} view without dealing with the internal sentinel objects.
     */
    private static class TestConcurrentNullSafeMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestConcurrentNullSafeMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }

        @Override
        protected Object maskNullKey(K key) {
            return key;
        }

        @Override
        protected Object maskNullValue(V value) {
            return value;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected K unmaskNullKey(Object key) {
            return (K) key;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected V unmaskNullValue(Object value) {
            return (V) value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        internalMap = new ConcurrentHashMap<>();
        // Populate with a few entries – including a null value to ensure the view works
        internalMap.put("one", "1");
        internalMap.put("two", "2");
        internalMap.put("nullVal", null);
        map = new TestConcurrentNullSafeMap<>(internalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        internalMap.clear();
    }

    // -------------------- valuesContains --------------------
    static Stream<Arguments> provideContainsCases() {
        return Stream.of(// not present initially
        Arguments.of("x", false), // null is present in default setup
        Arguments.of(null, true), Arguments.of("missing", false));
    }

    @Test
    void valuesCollectionReflectsInternalMapSize() {
        Collection<String> values = map.values();
        // internalMap has three entries (including a null value)
        assertEquals(3, values.size(), "values() size should match internal map size");
    }

    @Test
    void iteratorReturnsUnmaskedValuesInCorrectOrder() {
        Collection<String> values = map.values();
        List<String> iterated = new ArrayList<>();
        for (String v : values) {
            iterated.add(v);
        }
        // The order of values from ConcurrentHashMap is not guaranteed;
        // we only assert that the expected elements are present.
        assertTrue(iterated.containsAll(Arrays.asList("1", "2", null)), "Iterator should return all values, including null");
    }

    @Test
    void containsWorksUsingInternalMap() {
        Collection<String> values = map.values();
        assertTrue(values.contains("1"), "Collection should contain existing value");
        assertTrue(values.contains(null), "Collection should contain null value");
        assertFalse(values.contains("non‑existent"), "Collection should not contain unknown value");
    }

    @Test
    void clearDelegatesToInternalMap() {
        Collection<String> values = map.values();
        assertFalse(values.isEmpty(), "Pre‑condition: values collection should not be empty");
        values.clear();
        assertTrue(values.isEmpty(), "After clear, values collection should be empty");
        assertTrue(internalMap.isEmpty(), "Internal map must also be empty after clear");
    }

    @Test
    void iteratorRemoveUpdatesUnderlyingMap() {
        Collection<String> values = map.values();
        Iterator<String> it = values.iterator();
        // Find and remove the entry with value "2"
        while (it.hasNext()) {
            String v = it.next();
            if ("2".equals(v)) {
                it.remove();
                break;
            }
        }
        // Verify that the underlying map no longer contains the removed value
        assertFalse(internalMap.containsValue("2"), "Underlying map should no longer contain the removed value");
        // Size should have decreased by one
        assertEquals(2, values.size(), "Values collection size should reflect removal");
    }

    // -------------------- valuesSize --------------------
    @Test
    void valuesSize_EmptyMap() {
        // make map empty
        internalMap.clear();
        Collection<String> values = map.values();
        assertEquals(0, values.size(), "Empty map must report size 0");
    }

    @Test
    void valuesSize_AfterThreePuts() {
        internalMap.clear();
        internalMap.put("k1", "v1");
        internalMap.put("k2", "v2");
        internalMap.put("k3", "v3");
        Collection<String> values = map.values();
        assertEquals(3, values.size(), "Map with three entries must report size 3");
    }

    // -------------------- valuesIterator --------------------
    @Test
    void valuesIterator_OrderAndContent() {
        // Use a sorted map to guarantee iteration order.
        internalMap = new ConcurrentSkipListMap<>();
        internalMap.put("a", "a");
        internalMap.put("b", "b");
        internalMap.put("c", null);
        map = new TestConcurrentNullSafeMap<>(internalMap);
        Iterator<String> it = map.values().iterator();
        assertEquals("a", it.next(), "First element should be \"a\"");
        assertEquals("b", it.next(), "Second element should be \"b\"");
        assertNull(it.next(), "Third element should be null");
        assertFalse(it.hasNext(), "Iterator should be exhausted");
    }

    @Test
    void valuesIterator_HasNextAfterExhaustion() {
        internalMap.clear();
        map = new TestConcurrentNullSafeMap<>(internalMap);
        Iterator<String> it = map.values().iterator();
        assertFalse(it.hasNext(), "hasNext() must be false for empty collection");
    }

    // -------------------- valuesIteratorRemove --------------------
    @Test
    void valuesIteratorRemove_SingleRemoval() {
        Iterator<String> it = map.values().iterator();
        while (it.hasNext()) {
            String v = it.next();
            if ("1".equals(v)) {
                it.remove();
                break;
            }
        }
        assertFalse(internalMap.containsValue("1"));
        assertEquals(2, map.values().size());
    }

    @ParameterizedTest(name = "contains({0}) → {1}")
    @MethodSource("provideContainsCases")
    void valuesContains_VariousValues(Object query, boolean expected) {
        // Ensure "x" is present for the first case
        if ("x".equals(query)) {
            internalMap.put("xKey", "x");
        }
        Collection<String> values = map.values();
        assertEquals(expected, values.contains(query));
    }

    // -------------------- valuesClear --------------------
    @Test
    void valuesClear_EmptiesMap() {
        Collection<String> values = map.values();
        values.clear();
        assertTrue(values.isEmpty(), "After clear, collection must be empty");
        assertTrue(internalMap.isEmpty(), "Underlying map must be empty after clear");
    }

    // -------------------- valuesIteratorNoSuchElement --------------------
    @Test
    void valuesIteratorNoSuchElement_EmptyMap() {
        internalMap.clear();
        map = new TestConcurrentNullSafeMap<>(internalMap);
        Iterator<String> it = map.values().iterator();
        assertThrows(NoSuchElementException.class, it::next, "Calling next() on empty iterator must throw NoSuchElementException");
    }

    // -------------------- valuesIteratorIllegalStateRemove --------------------
    @Test
    void valuesIteratorIllegalStateRemove_BeforeNext() {
        Iterator<String> it = map.values().iterator();
        assertThrows(IllegalStateException.class, it::remove, "remove() before next() must throw IllegalStateException");
    }

    @Test
    void valuesIteratorIllegalStateRemove_DoubleRemove() {
        Iterator<String> it = map.values().iterator();
        if (it.hasNext()) {
            it.next();
            // first removal – OK
            it.remove();
        }
        assertThrows(IllegalStateException.class, it::remove, "Second remove() without intervening next() must throw IllegalStateException");
    }

    // -------------------- valuesReflectsMapChanges --------------------
    @Test
    void valuesReflectsMapChanges_SizeAfterDirectModifications() {
        map.put("new1", "v1");
        map.put("new2", "v2");
        // remove one of the default entries
        map.remove("one");
        assertEquals(4, map.values().size(), "After two puts and one remove, size should be 4 (original 3 +2 -1)");
    }

    @Test
    void valuesReflectsMapChanges_ContainsAfterDirectPut() {
        // obtain view early
        Collection<String> values = map.values();
        map.put("freshKey", "newVal");
        assertTrue(values.contains("newVal"), "Live view must reflect a newly added value via map.put()");
    }

    // -------------------- valuesContainsWithEquality --------------------
    @Test
    void valuesContainsWithEquality_StringEquality() {
        // literal stored
        internalMap.put("eqKey", "eq");
        Collection<String> values = map.values();
        assertTrue(values.contains(new String("eq")), "contains() must rely on equals() for distinct but equal String objects");
    }

    @Test
    void valuesContainsWithEquality_IntegerEquality() {
        // Use a map of <String, Integer> to test integer equality
        ConcurrentMap<Object, Object> intMap = new ConcurrentHashMap<>();
        TestConcurrentNullSafeMap<String, Integer> intMapWrapper = new TestConcurrentNullSafeMap<>(intMap);
        intMap.put("intKey", 42);
        Collection<Integer> values = intMapWrapper.values();
        assertTrue(values.contains(new Integer(42)), "contains() must rely on equals() for distinct Integer objects");
    }

    // -------------------- valuesSizeAfterClear --------------------
    @Test
    void valuesSizeAfterClear_NonEmptyMap() {
        // Populate with several entries
        internalMap.put("extra1", "e1");
        internalMap.put("extra2", "e2");
        Collection<String> values = map.values();
        values.clear();
        assertEquals(0, values.size(), "After clear, collection size must be 0");
        assertEquals(0, internalMap.size(), "Underlying map size must also be 0");
    }

    // -------------------- valuesIteratorRemoveMultiple --------------------
    @Test
    void valuesIteratorRemoveMultiple_TwoRemovals() {
        // Ensure deterministic order using a sorted map
        internalMap = new ConcurrentSkipListMap<>();
        internalMap.put("a", "a");
        internalMap.put("b", "b");
        internalMap.put("c", "c");
        internalMap.put("d", "d");
        map = new TestConcurrentNullSafeMap<>(internalMap);
        Iterator<String> it = map.values().iterator();
        // Remove first two elements
        assertEquals("a", it.next());
        it.remove();
        assertEquals("b", it.next());
        it.remove();
        // Expected remaining values: "c", "d"
        Collection<String> remaining = map.values();
        assertEquals(2, remaining.size());
        assertTrue(remaining.containsAll(Arrays.asList("c", "d")));
        // Verify underlying map keys have been removed
        assertFalse(internalMap.containsKey("a"));
        assertFalse(internalMap.containsKey("b"));
    }

    // -------------------- valuesContainsClassCast --------------------
    @Test
    void valuesContainsClassCast_Integer() {
        Collection<String> values = map.values();
        assertThrows(ClassCastException.class, () -> values.contains(123), "contains(123) on <String,String> map must throw ClassCastException");
    }

    @Test
    void valuesContainsClassCast_Object() {
        Collection<String> values = map.values();
        assertThrows(ClassCastException.class, () -> values.contains(new Object()), "contains(Object) on <String,String> map must throw ClassCastException");
    }
}
