package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DeepEquals_isContainerType_Test {

    /**
     * Helper class that implements both {@link Collection} and {@link Map}.
     * The methods are stubbed because the test only cares about the type
     * relationship (i.e., instanceof checks) performed by {@code isContainerType}.
     */
    private static class BothContainer implements Collection<Object>, Map<Object, Object> {

        // ----------- Collection implementation (stubbed) -----------
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<Object> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return a;
        }

        @Override
        public boolean add(Object e) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends Object> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            /* no‑op */
        }

        // ----------- Map implementation (stubbed) ---------------
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
        public Object get(Object key) {
            return null;
        }

        @Override
        public Object put(Object key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends Object, ? extends Object> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            /* no‑op */
        }

        @Override
        public Set<Object> keySet() {
            return Collections.emptySet();
        }

        @Override
        public Collection<Object> values() {
            return Collections.emptyList();
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return Collections.emptySet();
        }
    }

    static Stream<Arguments> positiveCases() {
        return Stream.of(// Simple Collection and Map implementations
        Arguments.of(new ArrayList<>(), true, "ArrayList implements Collection"), Arguments.of(new HashMap<>(), true, "HashMap implements Map"), // Object that implements both interfaces
        Arguments.of(new BothContainer(), true, "Object implements both Collection and Map"));
    }

    static Stream<Arguments> negativeCases() {
        return Stream.of(Arguments.of(null, false, "null reference"), Arguments.of(Integer.valueOf(42), false, "Integer is not a container"), Arguments.of("some string", false, "String does not implement Collection or Map"), Arguments.of(new AbstractMap.SimpleEntry<>("k", "v"), false, "SimpleEntry implements Map.Entry but not Map"));
    }

    static Stream<Arguments> edgeCases() {
        return Stream.of(// Immutable collections/maps
        Arguments.of(Collections.emptyList(), true, "Collections.emptyList() implements Collection"), Arguments.of(Collections.emptyMap(), true, "Collections.emptyMap() implements Map"), // Other standard container implementations
        Arguments.of(new LinkedList<>(), true, "LinkedList implements Deque and Collection"), Arguments.of(new ConcurrentHashMap<>(), true, "ConcurrentHashMap implements Map"), Arguments.of(new HashSet<>(), true, "HashSet implements Set → Collection"), Arguments.of(new IdentityHashMap<>(), true, "IdentityHashMap implements Map"), // Non‑container objects
        Arguments.of(new AtomicBoolean(true), false, "AtomicBoolean is not a container"), Arguments.of(new int[] { 1, 2, 3 }, false, "Primitive int[] does not implement Collection or Map"), Arguments.of(new Object[0], false, "Object[] does not implement Collection or Map"));
    }

    @Test
    void shouldReturnTrueForCollectionImplementation() {
        List<Integer> list = new ArrayList<>();
        assertTrue(DeepEquals.isContainerType(list), "ArrayList should be recognised as a container type");
        Set<String> set = new HashSet<>();
        assertTrue(DeepEquals.isContainerType(set), "HashSet should be recognised as a container type");
        Deque<Double> deque = new LinkedList<>();
        assertTrue(DeepEquals.isContainerType(deque), "LinkedList used as Deque should be recognised as a container type");
    }

    @Test
    void shouldReturnTrueForMapImplementation() {
        Map<String, Integer> map = new HashMap<>();
        assertTrue(DeepEquals.isContainerType(map), "HashMap should be recognised as a container type");
        Map<Integer, String> concurrent = new ConcurrentHashMap<>();
        assertTrue(DeepEquals.isContainerType(concurrent), "ConcurrentHashMap should be recognised as a container type");
        Map<Object, Object> identity = new IdentityHashMap<>();
        assertTrue(DeepEquals.isContainerType(identity), "IdentityHashMap should be recognised as a container type");
    }

    @Test
    void shouldReturnFalseForNonContainerObjects() {
        String str = "not a collection";
        assertFalse(DeepEquals.isContainerType(str), "String is not a container type");
        Integer number = 42;
        assertFalse(DeepEquals.isContainerType(number), "Integer is not a container type");
        Object obj = new Object();
        assertFalse(DeepEquals.isContainerType(obj), "Plain Object is not a container type");
    }

    @Test
    void shouldReturnFalseForNull() {
        assertFalse(DeepEquals.isContainerType(null), "null should not be considered a container type");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("positiveCases")
    void shouldIdentifyPositiveContainerTypes(Object input, boolean expected, String description) {
        Assertions.assertEquals(expected, DeepEquals.isContainerType(input), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("negativeCases")
    void shouldIdentifyNegativeNonContainerTypes(Object input, boolean expected, String description) {
        Assertions.assertEquals(expected, DeepEquals.isContainerType(input), description);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("edgeCases")
    void shouldHandleEdgeCases(Object input, boolean expected, String description) {
        Assertions.assertEquals(expected, DeepEquals.isContainerType(input), description);
    }
}
