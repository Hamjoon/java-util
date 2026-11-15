package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.ConcurrentHashMap;

class MapUtilities_detectMapOrdering_Test {

    /**
     * Minimal {@link CompactMap} implementation that allows the ordering to be
     * injected via the constructor.  The real {@code CompactMap} class already
     * provides such a constructor; this subclass exists only to make the test
     * code self‑contained and readable.
     */
    private static class DummyCompactMap<K, V> extends CompactMap<K, V> {

        DummyCompactMap() {
            super();
        }

        DummyCompactMap(String ordering) {
            super(ordering);
        }
    }

    /**
     * Wrapper map that delegates all {@link Map} operations to an underlying map
     * and exposes a public {@code getUnderlyingMap()} method.  The utility method
     * {@code MapUtilities.getUnderlyingMap} (private) uses reflection to call
     * this method, so the wrapper mimics the behaviour of the library's own
     * wrappers (e.g., {@code Collections.synchronizedMap}).
     */
    private static class WrapperMap<K, V> implements Map<K, V> {

        private final Map<K, V> underlying;

        WrapperMap(Map<K, V> underlying) {
            this.underlying = underlying;
        }

        /**
         * Used by {@code MapUtilities.getUnderlyingMap} via reflection.
         */
        public Map<K, V> getUnderlyingMap() {
            return underlying;
        }

        /* -------------------------------------------------------
         *  Delegate all Map methods to the underlying map.
         * ------------------------------------------------------- */
        @Override
        public int size() {
            return underlying.size();
        }

        @Override
        public boolean isEmpty() {
            return underlying.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return underlying.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return underlying.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return underlying.get(key);
        }

        @Override
        public V put(K key, V value) {
            return underlying.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return underlying.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            underlying.putAll(m);
        }

        @Override
        public void clear() {
            underlying.clear();
        }

        @Override
        public Set<K> keySet() {
            return underlying.keySet();
        }

        @Override
        public Collection<V> values() {
            return underlying.values();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return underlying.entrySet();
        }
    }

    /**
     * A {@link CompactMap} whose {@code getOrdering()} method deliberately throws
     * {@link IllegalArgumentException}.  Used to verify that {@code detectMapOrdering}
     * re‑wraps the exception with the expected prefix.
     */
    private static class ThrowingOrderingCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected String getOrdering() {
            throw new IllegalArgumentException("ordering broken");
        }
    }

    /**
     * Map that throws {@link IllegalArgumentException} from {@code entrySet()}.
     *  This triggers the catch block inside {@code detectMapOrdering}.
     */
    private static class ThrowingMap<K, V> extends HashMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new IllegalArgumentException("bad map");
        }
    }

    /**
     * Subclass of {@link LinkedHashMap} – should still be recognised as INSERTION ordered.
     */
    private static class MyLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    }

    /**
     * Subclass of {@link TreeMap} – should be recognised as SORTED ordered.
     */
    private static class MyTreeMap<K, V> extends TreeMap<K, V> {
    }

    @Test
    void detectMapOrdering_nullMap_returnsUnordered() {
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(null));
    }

    @Test
    void detectMapOrdering_hashMap_returnsUnordered() {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("a", "1");
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(hashMap));
    }

    @Test
    void detectMapOrdering_treeMap_returnsSorted() {
        Map<String, String> treeMap = new TreeMap<>();
        treeMap.put("b", "2");
        treeMap.put("a", "1");
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(treeMap));
    }

    @Test
    void detectMapOrdering_linkedHashMap_returnsInsertion() {
        Map<String, String> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("first", "1");
        linkedHashMap.put("second", "2");
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(linkedHashMap));
    }

    @Test
    void detectMapOrdering_enumMap_returnsInsertion() {
        EnumMap<TestEnum, String> enumMap = new EnumMap<>(TestEnum.class);
        enumMap.put(TestEnum.ONE, "1");
        enumMap.put(TestEnum.TWO, "2");
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(enumMap));
    }

    @Test
    void detectMapOrdering_compactMap_returnsItsOrdering() {
        // CompactMap is part of the library; we can use its concrete implementation.
        // The default ordering for a newly created CompactMap is UNORDERED.
        CompactMap<String, String> compactMap = new CompactMap<>();
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(compactMap));
        // Change ordering by creating a CompactMap with insertion ordering.
        CompactMap<String, String> insertionMap = new CompactMap<>(CompactMap.INSERTION);
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(insertionMap));
    }

    @Test
    void detectMapOrdering_wrappedCompactMap_returnsUnderlyingOrdering() {
        // Some map implementations wrap another map (e.g., Collections.synchronizedMap).
        // The utility should unwrap and detect the ordering of the underlying map.
        CompactMap<String, String> underlying = new CompactMap<>(CompactMap.SORTED);
        Map<String, String> wrapped = Collections.synchronizedMap(underlying);
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(wrapped));
    }

    @Test
    void detectMapOrdering_illegalArgument_isRethrownWithMessage() {
        // Simulate a map that throws IllegalArgumentException from getUnderlyingMap.
        Map<Object, Object> badMap = new HashMap<>() {

            @Override
            public Set<Entry<Object, Object>> entrySet() {
                throw new IllegalArgumentException("bad map");
            }
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapUtilities.detectMapOrdering(badMap));
        assertTrue(ex.getMessage().contains("Cannot determine map ordering"));
    }

    @Test
    void detectMapOrdering_directCompactMap_insertionOrdering() {
        DummyCompactMap<String, String> map = new DummyCompactMap<>(CompactMap.INSERTION);
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(map));
    }

    @Test
    void detectMapOrdering_directCompactMap_sortedOrdering() {
        DummyCompactMap<String, String> map = new DummyCompactMap<>(CompactMap.SORTED);
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(map));
    }

    @Test
    void detectMapOrdering_directCompactMap_unorderedOrdering() {
        DummyCompactMap<String, String> map = new DummyCompactMap<>(CompactMap.UNORDERED);
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(map));
    }

    @Test
    void detectMapOrdering_underlyingCompactMap_isDetected() {
        DummyCompactMap<String, String> underlying = new DummyCompactMap<>(CompactMap.UNORDERED);
        WrapperMap<String, String> wrapper = new WrapperMap<>(underlying);
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(wrapper));
    }

    @Test
    void detectMapOrdering_treeMap_isSorted() {
        Map<String, String> treeMap = new TreeMap<>();
        treeMap.put("b", "2");
        treeMap.put("a", "1");
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(treeMap));
    }

    @Test
    void detectMapOrdering_linkedHashMap_isInsertion() {
        Map<String, String> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("first", "1");
        linkedHashMap.put("second", "2");
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(linkedHashMap));
    }

    @Test
    void detectMapOrdering_enumMap_isInsertion() {
        EnumMap<SampleEnum, String> enumMap = new EnumMap<>(SampleEnum.class);
        enumMap.put(SampleEnum.ONE, "1");
        enumMap.put(SampleEnum.TWO, "2");
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(enumMap));
    }

    @Test
    void detectMapOrdering_hashMap_isUnordered() {
        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("a", "1");
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(hashMap));
    }

    @Test
    void detectMapOrdering_concurrentHashMap_isUnordered() {
        Map<String, String> concurrent = new ConcurrentHashMap<>();
        concurrent.put("x", "1");
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(concurrent));
    }

    @Test
    void detectMapOrdering_wrappedSortedMap_isSorted() {
        WrapperMap<String, String> wrapper = new WrapperMap<>(new TreeMap<>());
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(wrapper));
    }

    @Test
    void detectMapOrdering_wrappedLinkedHashMap_isInsertion() {
        WrapperMap<String, String> wrapper = new WrapperMap<>(new LinkedHashMap<>());
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(wrapper));
    }

    @Test
    void detectMapOrdering_wrappedEnumMap_isInsertion() {
        WrapperMap<SampleEnum, String> wrapper = new WrapperMap<>(new EnumMap<>(SampleEnum.class));
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(wrapper));
    }

    @Test
    void detectMapOrdering_wrapperWithNullUnderlying_isUnordered() {
        WrapperMap<String, String> wrapper = new WrapperMap<>(null);
        assertEquals(CompactMap.UNORDERED, MapUtilities.detectMapOrdering(wrapper));
    }

    @Test
    void detectMapOrdering_customLinkedHashMapSubclass_isInsertion() {
        MyLinkedHashMap<String, String> myMap = new MyLinkedHashMap<>();
        myMap.put("k1", "v1");
        assertEquals(CompactMap.INSERTION, MapUtilities.detectMapOrdering(myMap));
    }

    @Test
    void detectMapOrdering_customTreeMapSubclass_isSorted() {
        MyTreeMap<String, String> myMap = new MyTreeMap<>();
        myMap.put("b", "2");
        myMap.put("a", "1");
        assertEquals(CompactMap.SORTED, MapUtilities.detectMapOrdering(myMap));
    }

    @Test
    void detectMapOrdering_mapThatThrowsIllegalArgumentException_isRethrown() {
        Map<Object, Object> badMap = new ThrowingMap<>();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapUtilities.detectMapOrdering(badMap));
        assertTrue(ex.getMessage().startsWith("Cannot determine map ordering"));
    }

    @Test
    void detectMapOrdering_directCompactMapThrowsOrderingException_isRethrown() {
        ThrowingOrderingCompactMap<String, String> map = new ThrowingOrderingCompactMap<>();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapUtilities.detectMapOrdering(map));
        assertTrue(ex.getMessage().startsWith("Cannot determine map ordering"));
    }

    @Test
    void detectMapOrdering_underlyingCompactMapThrowsOrderingException_isRethrown() {
        ThrowingOrderingCompactMap<String, String> underlying = new ThrowingOrderingCompactMap<>();
        WrapperMap<String, String> wrapper = new WrapperMap<>(underlying);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapUtilities.detectMapOrdering(wrapper));
        assertTrue(ex.getMessage().startsWith("Cannot determine map ordering"));
    }
}
