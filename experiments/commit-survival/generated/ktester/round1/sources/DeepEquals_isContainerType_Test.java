package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

// No mocks needed, but kept from the template
@ExtendWith(MockitoExtension.class)
class DeepEquals_isContainerType_Test {

    class MyList extends ArrayList<String> {
    }

    class MyMap extends HashMap<String, String> {
    }

    /**
     * Implements both {@link Collection} and {@link Map}. The implementations are
     * minimal – only the methods required by the interfaces are stubbed out.
     * The test only relies on the {@code instanceof} checks performed inside
     * {@link DeepEquals#isContainerType(Object)}.
     */
    static class BothCollectionAndMap implements Collection<Object>, Map<Object, Object> {

        // ----- Collection methods (no‑op implementations) -----
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
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends Object> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }

        // ----- Map methods (no‑op implementations) -----
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
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends Object, ? extends Object> m) {
        }

        @Override
        public void clear() {
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

    /**
     * Simple custom collection – extends {@link AbstractCollection} to avoid
     * having to implement every method of {@link Collection}.
     */
    static class MyCustomCollection extends AbstractCollection<Object> {

        @Override
        public Iterator<Object> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    }

    /**
     * Simple custom map – extends {@link AbstractMap}.
     */
    static class MyCustomMap extends AbstractMap<Object, Object> {

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return Collections.emptySet();
        }
    }

    static Stream<Arguments> providerDetectionOfCollectionAndMap() {
        return Stream.of(// Collection instances
        Arguments.of(new ArrayList<>(), true), Arguments.of(new LinkedList<>(), true), // Map instances
        Arguments.of(new HashMap<>(), true), Arguments.of(new ConcurrentHashMap<>(), true), // Null
        Arguments.of(null, false), // Non‑container objects
        Arguments.of("plain string", false), Arguments.of(Integer.valueOf(42), false), // Custom class implementing both Collection and Map
        Arguments.of(new BothCollectionAndMap(), true));
    }

    static Stream<Arguments> providerAdditionalCollections() {
        return Stream.of(Arguments.of(new HashSet<>()), Arguments.of(new TreeSet<>()), Arguments.of(new ArrayDeque<>()), Arguments.of(new Vector<>()), Arguments.of(Collections.emptyList()));
    }

    static Stream<Arguments> providerAdditionalMaps() {
        return Stream.of(Arguments.of(new TreeMap<>()), Arguments.of(new LinkedHashMap<>()), Arguments.of(new Properties()), Arguments.of(Collections.emptyMap()), Arguments.of(new ConcurrentSkipListMap<>()));
    }

    static Stream<Arguments> providerNonContainerEdgeCases() {
        return Stream.of(Arguments.of(new Object()), Arguments.of(new int[] { 1, 2, 3 }), Arguments.of(new AtomicBoolean(true)), Arguments.of(new AbstractMap.SimpleEntry<>("k", "v")), Arguments.of(new AtomicInteger(5)));
    }

    @Test
    void returnsTrueForCollectionInstances() {
        List<Integer> list = new ArrayList<>();
        Set<String> set = new HashSet<>();
        Deque<Double> deque = new LinkedList<>();
        assertTrue(DeepEquals.isContainerType(list), "ArrayList should be recognised as a container");
        assertTrue(DeepEquals.isContainerType(set), "HashSet should be recognised as a container");
        assertTrue(DeepEquals.isContainerType(deque), "LinkedList (Deque) should be recognised as a container");
    }

    @Test
    void returnsTrueForMapInstances() {
        Map<String, Integer> map = new HashMap<>();
        Map<Integer, String> concurrent = new ConcurrentHashMap<>();
        assertTrue(DeepEquals.isContainerType(map), "HashMap should be recognised as a container");
        assertTrue(DeepEquals.isContainerType(concurrent), "ConcurrentHashMap should be recognised as a container");
    }

    @Test
    void returnsFalseForNonContainerObjects() {
        String text = "hello";
        Integer number = 42;
        Object plainObject = new Object();
        int[] primitiveArray = new int[] { 1, 2, 3 };
        assertFalse(DeepEquals.isContainerType(text), "String is not a container");
        assertFalse(DeepEquals.isContainerType(number), "Number is not a container");
        assertFalse(DeepEquals.isContainerType(plainObject), "Plain Object is not a container");
        assertFalse(DeepEquals.isContainerType(primitiveArray), "Array is not a container");
    }

    @Test
    void returnsFalseForNull() {
        assertFalse(DeepEquals.isContainerType(null), "null should not be considered a container");
    }

    @Test
    void customCollectionSubclassIsRecognised() {
        class MyList extends ArrayList<String> {
        }
        MyList myList = new MyList();
        assertTrue(DeepEquals.isContainerType(myList), "Subclass of Collection should be recognised as a container");
    }

    @Test
    void customMapSubclassIsRecognised() {
        class MyMap extends HashMap<String, String> {
        }
        MyMap myMap = new MyMap();
        assertTrue(DeepEquals.isContainerType(myMap), "Subclass of Map should be recognised as a container");
    }

    /* -----------------------------------------------------------
     *  Group: detection of collection and map types
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} → expected: {1}")
    @MethodSource("providerDetectionOfCollectionAndMap")
    void isContainerType_detectionOfCollectionAndMap(Object input, boolean expected) {
        assertEquals(expected, DeepEquals.isContainerType(input), () -> "Object: " + (input == null ? "null" : input.getClass().getSimpleName()));
    }

    /* -----------------------------------------------------------
     *  Group: additional collection implementations
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} → expected: true")
    @MethodSource("providerAdditionalCollections")
    void isContainerType_additionalCollections(Object input) {
        assertTrue(DeepEquals.isContainerType(input), () -> "Expected collection type but got false for: " + input.getClass().getSimpleName());
    }

    /* -----------------------------------------------------------
     *  Group: additional map implementations
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} → expected: true")
    @MethodSource("providerAdditionalMaps")
    void isContainerType_additionalMaps(Object input) {
        assertTrue(DeepEquals.isContainerType(input), () -> "Expected map type but got false for: " + input.getClass().getSimpleName());
    }

    /* -----------------------------------------------------------
     *  Group: custom collection only
     * ----------------------------------------------------------- */
    @Test
    void isContainerType_customCollectionOnly() {
        MyCustomCollection custom = new MyCustomCollection();
        assertTrue(DeepEquals.isContainerType(custom), "Custom class implementing only Collection should be recognised as a container");
    }

    /* -----------------------------------------------------------
     *  Group: custom map only
     * ----------------------------------------------------------- */
    @Test
    void isContainerType_customMapOnly() {
        MyCustomMap custom = new MyCustomMap();
        assertTrue(DeepEquals.isContainerType(custom), "Custom class implementing only Map should be recognised as a container");
    }

    /* -----------------------------------------------------------
     *  Group: non‑container edge cases
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} → expected: false")
    @MethodSource("providerNonContainerEdgeCases")
    void isContainerType_nonContainerEdgeCases(Object input) {
        assertFalse(DeepEquals.isContainerType(input), () -> "Expected non‑container but got true for: " + (input == null ? "null" : input.getClass().getSimpleName()));
    }
}
