package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapOf_6_Test {

    // Simple POJOs used for the custom‑object test case
    private static final class Person {

        private final String name;

        Person(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Person) && Objects.equals(name, ((Person) o).name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public String toString() {
            return "Person[" + name + "]";
        }
    }

    private static final class Address {

        private final String city;

        Address(String city) {
            this.city = city;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Address) && Objects.equals(city, ((Address) o).city);
        }

        @Override
        public int hashCode() {
            return Objects.hash(city);
        }

        @Override
        public String toString() {
            return "Address[" + city + "]";
        }
    }

    private static Stream<Arguments> duplicateKeyProvider() {
        // Case 1 – duplicate key "A" appears as k1 and k3
        Map<String, Integer> expectedMap1 = new LinkedHashMap<>();
        // later value wins
        expectedMap1.put("A", 3);
        expectedMap1.put("B", 2);
        expectedMap1.put("C", 4);
        expectedMap1.put("D", 5);
        List<String> order1 = Arrays.asList("A", "B", "C", "D");
        // Case 2 – consecutive duplicate "X"
        Map<String, Integer> expectedMap2 = new LinkedHashMap<>();
        expectedMap2.put("X", 20);
        expectedMap2.put("Y", 30);
        expectedMap2.put("Z", 40);
        expectedMap2.put("W", 50);
        List<String> order2 = Arrays.asList("X", "Y", "Z", "W");
        return Stream.of(Arguments.of("Duplicate key 'A' later value overwrites but order stays from first occurrence", "A", 1, "B", 2, "A", 3, "C", 4, "D", 5, 4, order1, expectedMap1), Arguments.of("Consecutive duplicate key 'X' – later value wins, original position kept", "X", 10, "X", 20, "Y", 30, "Z", 40, "W", 50, 4, order2, expectedMap2));
    }

    @Test
    void mapOfCreatesOrderedUnmodifiableMap() {
        // given
        String k1 = "one";
        Integer v1 = 1;
        String k2 = "two";
        Integer v2 = 2;
        String k3 = "three";
        Integer v3 = 3;
        String k4 = "four";
        Integer v4 = 4;
        String k5 = "five";
        Integer v5 = 5;
        // when
        Map<String, Integer> result = MapUtilities.mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        // then
        assertEquals(5, result.size(), "Map should contain exactly five entries");
        assertEquals(v1, result.get(k1));
        assertEquals(v2, result.get(k2));
        assertEquals(v3, result.get(k3));
        assertEquals(v4, result.get(k4));
        assertEquals(v5, result.get(k5));
        // Verify iteration order matches insertion order (LinkedHashMap semantics)
        Iterator<String> keyIter = result.keySet().iterator();
        assertEquals(k1, keyIter.next());
        assertEquals(k2, keyIter.next());
        assertEquals(k3, keyIter.next());
        assertEquals(k4, keyIter.next());
        assertEquals(k5, keyIter.next());
        // Verify the map is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> result.put("six", 6));
        assertThrows(UnsupportedOperationException.class, () -> result.remove(k1));
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test
    void mapOfHandlesNullKeysAndValues() {
        // given
        String k1 = null;
        Integer v1 = null;
        String k2 = "key";
        Integer v2 = null;
        String k3 = null;
        Integer v3 = 3;
        String k4 = "four";
        Integer v4 = 4;
        String k5 = "five";
        Integer v5 = null;
        // when
        Map<String, Integer> result = MapUtilities.mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        // then
        assertEquals(5, result.size());
        assertTrue(result.containsKey(null));
        // the first null key maps to null value
        assertNull(result.get(null));
        assertEquals(v2, result.get("key"));
        // second null key overwrites first null key
        assertEquals(v3, result.get(null));
        assertEquals(v4, result.get("four"));
        assertNull(result.get("five"));
    }

    @Test
    void mapOfReturnsSameInstanceWhenCalledMultipleTimesWithSameData() {
        // given
        String k1 = "a", k2 = "b", k3 = "c", k4 = "d", k5 = "e";
        Integer v1 = 10, v2 = 20, v3 = 30, v4 = 40, v5 = 50;
        // when
        Map<String, Integer> first = MapUtilities.mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        Map<String, Integer> second = MapUtilities.mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        // then
        assertEquals(first, second, "Two maps created with identical arguments should be equal");
        // They are distinct objects (different references) because a new map is created each call
        assertNotSame(first, second);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("duplicateKeyProvider")
    void mapOfDuplicateKeyReplacement(String description, String k1, Integer v1, String k2, Integer v2, String k3, Integer v3, String k4, Integer v4, String k5, Integer v5, int expectedSize, List<String> expectedOrder, Map<String, Integer> expectedMap) {
        // when
        Map<String, Integer> result = MapUtilities.mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        // then
        assertEquals(expectedSize, result.size(), "Map size after duplicate handling");
        // verify order
        Iterator<String> it = result.keySet().iterator();
        for (String expectedKey : expectedOrder) {
            assertTrue(it.hasNext(), "Expected more keys in iteration");
            assertEquals(expectedKey, it.next());
        }
        assertFalse(it.hasNext(), "No extra keys expected");
        // verify values
        expectedMap.forEach((k, v) -> assertEquals(v, result.get(k), "Value for key " + k));
    }

    @Test
    void mapOfWorksWithIntegerKeysAndStringValues() {
        // given
        Integer k1 = 1, k2 = 2, k3 = 3, k4 = 4, k5 = 5;
        String v1 = "one", v2 = "two", v3 = "three", v4 = "four", v5 = "five";
        // when
        Map<Integer, String> result = MapUtilities.mapOf(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);
        // then
        assertEquals(5, result.size());
        assertEquals(v1, result.get(k1));
        assertEquals(v2, result.get(k2));
        assertEquals(v3, result.get(k3));
        assertEquals(v4, result.get(k4));
        assertEquals(v5, result.get(k5));
        // order check
        Iterator<Integer> it = result.keySet().iterator();
        assertEquals(k1, it.next());
        assertEquals(k2, it.next());
        assertEquals(k3, it.next());
        assertEquals(k4, it.next());
        assertEquals(k5, it.next());
        // immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put(6, "six"));
    }

    @Test
    void mapOfWorksWithUserDefinedObjectKeysAndValues() {
        // given
        Person p1 = new Person("Alice");
        Address a1 = new Address("NY");
        Person p2 = new Person("Bob");
        Address a2 = new Address("LA");
        Person p3 = new Person("Carol");
        Address a3 = new Address("SF");
        Person p4 = new Person("Dave");
        Address a4 = new Address("CHI");
        Person p5 = new Person("Eve");
        Address a5 = new Address("SEA");
        // when
        Map<Person, Address> result = MapUtilities.mapOf(p1, a1, p2, a2, p3, a3, p4, a4, p5, a5);
        // then
        assertEquals(5, result.size());
        assertEquals(a1, result.get(p1));
        assertEquals(a2, result.get(p2));
        assertEquals(a3, result.get(p3));
        assertEquals(a4, result.get(p4));
        assertEquals(a5, result.get(p5));
        // verify insertion order is preserved
        Iterator<Person> it = result.keySet().iterator();
        assertEquals(p1, it.next());
        assertEquals(p2, it.next());
        assertEquals(p3, it.next());
        assertEquals(p4, it.next());
        assertEquals(p5, it.next());
        // immutability of the map itself
        assertThrows(UnsupportedOperationException.class, () -> result.remove(p1));
    }

    @Test
    void mapOfReturnsUnmodifiableMapButValuesRemainMutable() {
        // given – mutable list values
        List<Integer> list1 = new ArrayList<>(Arrays.asList(1, 2));
        List<Integer> list2 = new ArrayList<>(Collections.singletonList(3));
        List<Integer> list3 = new ArrayList<>();
        List<Integer> list4 = new ArrayList<>(Arrays.asList(4, 5, 6));
        List<Integer> list5 = new ArrayList<>();
        // when
        Map<String, List<Integer>> result = MapUtilities.mapOf("list1", list1, "list2", list2, "list3", list3, "list4", list4, "list5", list5);
        // then – structural immutability
        assertThrows(UnsupportedOperationException.class, () -> result.put("new", new ArrayList<>()));
        assertThrows(UnsupportedOperationException.class, () -> result.remove("list1"));
        assertThrows(UnsupportedOperationException.class, result::clear);
        // mutate a value retrieved from the map
        List<Integer> retrieved = result.get("list1");
        retrieved.add(99);
        // the underlying list object is the same instance, so the change is visible through the map
        assertEquals(Arrays.asList(1, 2, 99), result.get("list1"));
        // other lists remain untouched
        assertEquals(Collections.singletonList(3), result.get("list2"));
        assertTrue(result.get("list3").isEmpty());
        assertEquals(Arrays.asList(4, 5, 6), result.get("list4"));
        assertTrue(result.get("list5").isEmpty());
    }
}
