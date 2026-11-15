package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapUtilities_detectMapOrdering_Test {

    /**
     * Minimal concrete subclass of {@link CompactMap} that allows us to control
     * the value returned by {@code getOrdering()} (or to throw an exception).
     */
    private static class CompactMapMock<K, V> extends CompactMap<K, V> {

        private final Supplier<String> orderingSupplier;

        CompactMapMock(String ordering) {
            this.orderingSupplier = () -> ordering;
        }

        CompactMapMock(Supplier<String> orderingSupplier) {
            this.orderingSupplier = orderingSupplier;
        }

        @Override
        protected String getOrdering() {
            return orderingSupplier.get();
        }
    }

    /**
     * Wrapper that delegates to a real map but deliberately throws an
     * {@link IllegalArgumentException} from {@code entrySet()} – this is enough
     * to cause {@code MapUtilities.getUnderlyingMap()} (which inspects the map)
     * to propagate the exception.
     */
    private static class FaultyMapWrapper<K, V> implements Map<K, V> {

        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<K> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<V> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new IllegalArgumentException("simulated failure in entrySet");
        }
    }

    /**
     * Similar to {@link FaultyMapWrapper} but throws {@link NullPointerException}
     * from {@code entrySet()}.
     */
    private static class NullPointerMapWrapper<K, V> implements Map<K, V> {

        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<K> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<V> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new NullPointerException("simulated NPE in entrySet");
        }
    }

    /**
     * Similar to {@link FaultyMapWrapper} but throws {@link IllegalStateException}
     * from {@code entrySet()}.
     */
    private static class IllegalStateMapWrapper<K, V> implements Map<K, V> {

        private final Map<K, V> delegate = new HashMap<>();

        @Override
        public int size() {
            return delegate.size();
        }

        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return delegate.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return delegate.containsValue(value);
        }

        @Override
        public V get(Object key) {
            return delegate.get(key);
        }

        @Override
        public V put(K key, V value) {
            return delegate.put(key, value);
        }

        @Override
        public V remove(Object key) {
            return delegate.remove(key);
        }

        @Override
        public void putAll(Map<? extends K, ? extends V> m) {
            delegate.putAll(m);
        }

        @Override
        public void clear() {
            delegate.clear();
        }

        @Override
        public Set<K> keySet() {
            return delegate.keySet();
        }

        @Override
        public Collection<V> values() {
            return delegate.values();
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            throw new IllegalStateException("simulated ISE in entrySet");
        }
    }

    @Test
    void detectMapOrdering_nullMap_returnsUnordered() {
        String ordering = MapUtilities.detectMapOrdering(null);
        assertEquals(CompactMap.UNORDERED, ordering, "null map should be reported as UNORDERED");
    }

    @Test
    void detectMapOrdering_sortedMap_returnsSorted() {
        SortedMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("a", 1);
        treeMap.put("b", 2);
        String ordering = MapUtilities.detectMapOrdering(treeMap);
        assertEquals(CompactMap.SORTED, ordering, "SortedMap (TreeMap) should be reported as SORTED");
    }

    @Test
    void detectMapOrdering_linkedHashMap_returnsInsertion() {
        Map<String, Integer> linked = new LinkedHashMap<>();
        linked.put("first", 1);
        linked.put("second", 2);
        String ordering = MapUtilities.detectMapOrdering(linked);
        assertEquals(CompactMap.INSERTION, ordering, "LinkedHashMap should be reported as INSERTION");
    }

    @Test
    void detectMapOrdering_enumMap_returnsInsertion() {
        EnumMap<Day, String> enumMap = new EnumMap<>(Day.class);
        enumMap.put(Day.MONDAY, "Mon");
        enumMap.put(Day.TUESDAY, "Tue");
        String ordering = MapUtilities.detectMapOrdering(enumMap);
        assertEquals(CompactMap.INSERTION, ordering, "EnumMap should be reported as INSERTION");
    }

    @Test
    void detectMapOrdering_hashMap_returnsUnordered() {
        Map<String, Integer> hash = new HashMap<>();
        hash.put("x", 10);
        hash.put("y", 20);
        String ordering = MapUtilities.detectMapOrdering(hash);
        assertEquals(CompactMap.UNORDERED, ordering, "HashMap (no ordering guarantees) should be reported as UNORDERED");
    }

    @Test
    void detectMapOrdering_directCompactMap_sorted() {
        Map<String, Integer> map = new CompactMapMock<>(CompactMap.SORTED);
        String ordering = MapUtilities.detectMapOrdering(map);
        assertEquals(CompactMap.SORTED, ordering, "CompactMap reporting SORTED should be returned directly");
    }

    @Test
    void detectMapOrdering_directCompactMap_insertion() {
        Map<String, Integer> map = new CompactMapMock<>(CompactMap.INSERTION);
        String ordering = MapUtilities.detectMapOrdering(map);
        assertEquals(CompactMap.INSERTION, ordering, "CompactMap reporting INSERTION should be returned directly");
    }

    @Test
    void detectMapOrdering_directCompactMap_unordered() {
        Map<String, Integer> map = new CompactMapMock<>(CompactMap.UNORDERED);
        String ordering = MapUtilities.detectMapOrdering(map);
        assertEquals(CompactMap.UNORDERED, ordering, "CompactMap reporting UNORDERED should be returned directly");
    }

    @Test
    void detectMapOrdering_wrappedCompactMap_synchronized_returnsSorted() {
        Map<String, Integer> wrapped = Collections.synchronizedMap(new CompactMapMock<>(CompactMap.SORTED));
        String ordering = MapUtilities.detectMapOrdering(wrapped);
        assertEquals(CompactMap.SORTED, ordering, "A synchronized wrapper around a CompactMap should yield the underlying ordering");
    }

    @Test
    void detectMapOrdering_compactMap_getOrderingThrowsIllegalArgumentException() {
        Map<String, Integer> map = new CompactMapMock<>(() -> {
            throw new IllegalArgumentException("ordering error");
        });
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapUtilities.detectMapOrdering(map));
        assertTrue(ex.getMessage().startsWith("Cannot determine map ordering:"), "Exception message should be prefixed");
        assertTrue(ex.getMessage().contains("ordering error"), "Original exception message should be included");
    }

    @Test
    void detectMapOrdering_wrappedCompactMap_getOrderingThrowsIllegalArgumentException() {
        Map<String, Integer> wrapped = Collections.synchronizedMap(new CompactMapMock<>(() -> {
            throw new IllegalArgumentException("wrapped ordering error");
        }));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapUtilities.detectMapOrdering(wrapped));
        assertTrue(ex.getMessage().startsWith("Cannot determine map ordering:"), "Exception message should be prefixed");
        assertTrue(ex.getMessage().contains("wrapped ordering error"), "Original exception message should be included");
    }

    @Test
    void detectMapOrdering_faultyMapWrapper_throwsIllegalArgumentException() {
        Map<String, Integer> faulty = new FaultyMapWrapper<>();
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> MapUtilities.detectMapOrdering(faulty));
        assertTrue(ex.getMessage().startsWith("Cannot determine map ordering:"), "Exception should be re‑thrown with prefixed message");
    }

    @Test
    void detectMapOrdering_nullPointerMapWrapper_propagatesNullPointerException() {
        Map<String, Integer> faulty = new NullPointerMapWrapper<>();
        assertThrows(NullPointerException.class, () -> MapUtilities.detectMapOrdering(faulty), "NullPointerException should propagate unchanged");
    }

    @Test
    void detectMapOrdering_illegalStateMapWrapper_propagatesIllegalStateException() {
        Map<String, Integer> faulty = new IllegalStateMapWrapper<>();
        assertThrows(IllegalStateException.class, () -> MapUtilities.detectMapOrdering(faulty), "IllegalStateException should propagate unchanged");
    }
}
