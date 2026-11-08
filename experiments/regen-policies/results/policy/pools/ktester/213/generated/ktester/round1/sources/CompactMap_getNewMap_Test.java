package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_getNewMap_Test {

    /**
     * Simple subclass that only exposes the protected {@code getNewMap()} method.
     */
    private static class ExposedCompactMap<K, V> extends CompactMap<K, V> {

        /**
         * Calls the protected {@code getNewMap()} from the superclass.
         */
        public Map<K, V> callGetNewMap() {
            return super.getNewMap();
        }
    }

    /**
     * Subclass that overrides {@code getNewMap()} to return a {@link LinkedHashMap}.
     */
    private static class OverriddenCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected Map<K, V> getNewMap() {
            return new LinkedHashMap<>();
        }

        /**
         * Exposes the overridden method for testing.
         */
        public Map<K, V> callGetNewMap() {
            return super.getNewMap();
        }
    }

    private static Stream<ExposedCompactMap<String, Integer>> provideMapInstancePairs() {
        // we simply provide several fresh instances to demonstrate the parameterized approach
        return Stream.of(new ExposedCompactMap<>(), new ExposedCompactMap<>(), new ExposedCompactMap<>());
    }

    @Test
    void defaultGetNewMapReturnsHashMap() {
        // Arrange
        ExposedCompactMap<String, Integer> map = new ExposedCompactMap<>();
        // Act
        Map<String, Integer> newMap = map.callGetNewMap();
        // Assert
        assertNotNull(newMap, "getNewMap() must not return null");
        assertInstanceOf(HashMap.class, newMap, "Default implementation of getNewMap() should return a HashMap");
        assertTrue(newMap.isEmpty(), "Returned map should be empty");
        // Verify that the map is mutable
        newMap.put("key", 1);
        assertEquals(1, newMap.size(), "Returned map should be mutable");
    }

    @Test
    void overriddenGetNewMapReturnsLinkedHashMap() {
        // Arrange
        OverriddenCompactMap<String, Integer> map = new OverriddenCompactMap<>();
        // Act
        Map<String, Integer> newMap = map.callGetNewMap();
        // Assert
        assertNotNull(newMap, "Overridden getNewMap() must not return null");
        assertInstanceOf(LinkedHashMap.class, newMap, "Overridden implementation should return a LinkedHashMap");
        assertTrue(newMap.isEmpty(), "Returned map should be empty");
        // Verify insertion order is preserved (a property of LinkedHashMap)
        newMap.put("first", 1);
        newMap.put("second", 2);
        assertEquals("first", newMap.keySet().iterator().next(), "LinkedHashMap should preserve insertion order");
    }

    @Test
    void getNewMapReturnsNonNullEmptyHashMapInstance() {
        // Verify that a freshly created map is non‑null, empty and a HashMap.
        ExposedCompactMap<String, Integer> map = new ExposedCompactMap<>();
        Map<String, Integer> newMap = map.callGetNewMap();
        assertNotNull(newMap);
        assertTrue(newMap.isEmpty());
        assertInstanceOf(HashMap.class, newMap);
    }

    @Test
    void getNewMapIsMutableAndReflectsPutOperation() {
        ExposedCompactMap<String, Integer> map = new ExposedCompactMap<>();
        Map<String, Integer> newMap = map.callGetNewMap();
        newMap.put("a", 10);
        assertEquals(1, newMap.size());
        assertEquals(10, newMap.get("a"));
    }

    @Test
    void getNewMapProducesDistinctInstancesOnEachCall() {
        ExposedCompactMap<String, Integer> map = new ExposedCompactMap<>();
        Map<String, Integer> first = map.callGetNewMap();
        Map<String, Integer> second = map.callGetNewMap();
        assertNotSame(first, second, "Each call to getNewMap() should return a new instance");
    }

    @Test
    void modificationsToOneReturnedMapDoNotAffectAnother() {
        ExposedCompactMap<String, Integer> map = new ExposedCompactMap<>();
        Map<String, Integer> first = map.callGetNewMap();
        Map<String, Integer> second = map.callGetNewMap();
        first.put("x", 99);
        assertEquals(1, first.size());
        assertTrue(second.isEmpty(), "Second map must remain untouched");
    }

    @Test
    void hashMapAcceptsNullKeyAndNullValue() {
        ExposedCompactMap<String, String> map = new ExposedCompactMap<>();
        Map<String, String> newMap = map.callGetNewMap();
        newMap.put(null, null);
        assertEquals(1, newMap.size());
        assertTrue(newMap.containsKey(null));
        assertNull(newMap.get(null));
    }

    @Test
    void putReturnValueFollowsHashMapSemantics() {
        ExposedCompactMap<String, Integer> map = new ExposedCompactMap<>();
        Map<String, Integer> newMap = map.callGetNewMap();
        // first insertion – no previous value
        Integer previous = newMap.put("a", 1);
        assertNull(previous, "First put should return null");
        // second insertion with same key – should return previous value (1)
        previous = newMap.put("a", 2);
        assertEquals(1, previous, "Second put should return the previous value");
        assertEquals(2, newMap.get("a"));
    }

    @Test
    void removeReturnsPreviousValueAndShrinksMap() {
        ExposedCompactMap<String, String> map = new ExposedCompactMap<>();
        Map<String, String> newMap = map.callGetNewMap();
        newMap.put("k", "v");
        assertEquals(1, newMap.size());
        String removed = newMap.remove("k");
        assertEquals("v", removed, "remove should return the associated value");
        assertTrue(newMap.isEmpty(), "Map should be empty after removal");
    }

    @Test
    void entrySetIteratorYieldsAllInsertedEntries() {
        ExposedCompactMap<Integer, String> map = new ExposedCompactMap<>();
        Map<Integer, String> newMap = map.callGetNewMap();
        newMap.put(1, "one");
        newMap.put(2, "two");
        newMap.put(3, "three");
        Set<Map.Entry<Integer, String>> entrySet = newMap.entrySet();
        Iterator<Map.Entry<Integer, String>> it = entrySet.iterator();
        Set<Map.Entry<Integer, String>> collected = new HashSet<>();
        while (it.hasNext()) {
            collected.add(it.next());
        }
        assertEquals(entrySet, collected, "Iterator should return exactly the entries in the set");
        assertFalse(it.hasNext(), "Iterator must be exhausted after all entries are consumed");
    }

    @Test
    void equalsAndHashCodeForEmptyAndIdenticalMaps() {
        ExposedCompactMap<String, Integer> map = new ExposedCompactMap<>();
        Map<String, Integer> m1 = map.callGetNewMap();
        Map<String, Integer> m2 = map.callGetNewMap();
        // both empty
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
        // add identical entry to both
        m1.put("key", 42);
        m2.put("key", 42);
        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
    }

    @Test
    void toStringRepresentationMatchesHashMapContract() {
        ExposedCompactMap<Integer, String> map = new ExposedCompactMap<>();
        Map<Integer, String> newMap = map.callGetNewMap();
        // empty map
        assertEquals("{}", newMap.toString(), "Empty HashMap should render as {}");
        // after adding entries – order is unspecified for HashMap, so we check content
        newMap.put(1, "a");
        newMap.put(2, "b");
        String s = newMap.toString();
        // The string must start with '{' and end with '}' and contain both key/value pairs.
        assertTrue(s.startsWith("{") && s.endsWith("}"));
        assertTrue(s.contains("1=a"));
        assertTrue(s.contains("2=b"));
    }

    @ParameterizedTest
    @MethodSource("provideMapInstancePairs")
    void distinctMapInstancesAreIndependent(ExposedCompactMap<String, Integer> map) {
        Map<String, Integer> first = map.callGetNewMap();
        Map<String, Integer> second = map.callGetNewMap();
        first.put("alpha", 100);
        assertEquals(1, first.size());
        assertTrue(second.isEmpty(), "Second map must stay empty");
        assertNotSame(first, second);
    }
}
