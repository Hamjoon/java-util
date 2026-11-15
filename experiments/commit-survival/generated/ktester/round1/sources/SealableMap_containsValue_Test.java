package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableMap_containsValue_Test {

    private SealableMap<String, Integer> map;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    /**
     * Helper data holder for each test case.
     */
    private static class TestCase {

        final Object value;

        final boolean expected;

        final String description;

        // null means use default map from @BeforeEach
        final Map<String, Object> backingMap;

        TestCase(Object value, boolean expected, String description, Map<String, Object> backingMap) {
            this.value = value;
            this.expected = expected;
            this.description = description;
            this.backingMap = backingMap;
        }
    }

    /**
     * Simple POJO with proper {@code equals} and {@code hashCode} implementations.
     */
    private static final class Person {

        private final String name;

        private final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person person = (Person) o;
            return age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public String toString() {
            return "Person{name='" + name + '\'' + ", age=" + age + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default initialise a map with a few entries – individual tests can replace it.
        map = new SealableMap<>(sealedSupplier);
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        // include a null key/value pair
        map.put(null, null);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mutable state so that next test starts clean
        sealedState = false;
        if (map != null) {
            map.clear();
        }
    }

    /**
     * Supplies test cases for the "basic functionality" group.
     */
    private static Stream<TestCase> basicFunctionalityCases() {
        // Map containing "apple" and a null value
        Map<String, Object> mapWithApple = new HashMap<>();
        mapWithApple.put("fruit", "apple");
        mapWithApple.put("empty", null);
        // Map with duplicate values
        Map<String, Object> mapWithDuplicates = new HashMap<>();
        mapWithDuplicates.put("a", "duplicate");
        mapWithDuplicates.put("b", "duplicate");
        // Empty map
        Map<String, Object> emptyMap = new HashMap<>();
        return Stream.of(new TestCase("apple", true, "The backing map contains the value \"apple\"; containsValue should return true.", mapWithApple), new TestCase("banana", false, "The backing map does not contain the value \"banana\"; containsValue should return false.", mapWithApple), new TestCase(null, true, "The backing map contains a null value (e.g., key \"k1\" → null); containsValue(null) should return true.", mapWithApple), new TestCase(null, false, "The backing map is empty and does not contain any null values; containsValue(null) should return false.", emptyMap), new TestCase("duplicate", true, "The backing map has multiple keys mapping to the same value \"duplicate\"; containsValue should still return true.", mapWithDuplicates), new TestCase("empty", false, "The backing map is empty; any value lookup should return false.", emptyMap), new TestCase(new Integer(42), true, "The backing map contains an Integer value 42; containsValue should correctly handle non‑String objects.", Collections.singletonMap("answer", new Integer(42))), new TestCase(new Integer(99), false, "The backing map does not contain the Integer value 99; containsValue should return false.", Collections.singletonMap("answer", new Integer(42))));
    }

    /**
     * Supplies test cases for the "custom object equality" group.
     */
    private static Stream<TestCase> customObjectEqualityCases() {
        // Map containing a Person instance
        Person alice = new Person("Alice", 30);
        Map<String, Object> mapWithPerson = new HashMap<>();
        mapWithPerson.put("person1", alice);
        // Map containing a plain String "test"
        Map<String, Object> mapWithString = new HashMap<>();
        mapWithString.put("s", "test");
        return Stream.of(new TestCase(new Person("Alice", 30), true, "The backing map contains a Person instance with name \"Alice\" and age 30; " + "containsValue should return true when passed a different Person instance that is equal according to Person.equals.", mapWithPerson), new TestCase(new Person("Bob", 25), false, "The backing map does not contain a Person equal to name \"Bob\" and age 25; containsValue should return false.", mapWithPerson), new TestCase(new StringBuilder("test"), false, "StringBuilder does not override equals, so even though the map contains the String \"test\", " + "containsValue(new StringBuilder(\"test\")) should return false.", mapWithString));
    }

    @Test
    void testContainsExistingValue() {
        assertTrue(map.containsValue(2), "Map should contain the value 2");
    }

    @Test
    void testDoesNotContainMissingValue() {
        assertFalse(map.containsValue(99), "Map should not contain the value 99");
    }

    @Test
    void testContainsNullValue() {
        // The map already contains a null value (key = null)
        assertTrue(map.containsValue(null), "Map should report that it contains a null value");
    }

    @Test
    void testContainsValueWhenSealed() {
        // switch to sealed (read‑only) mode
        sealedState = true;
        // containsValue is a read‑only operation, it must still work
        assertTrue(map.containsValue(1), "Even when sealed, containsValue should work");
        assertFalse(map.containsValue(100), "Even when sealed, containsValue should correctly report missing values");
    }

    @Test
    void testContainsValueDoesNotMutateMap() {
        int originalSize = map.size();
        boolean result = map.containsValue(3);
        assertTrue(result, "Value 3 should be present");
        assertEquals(originalSize, map.size(), "containsValue must not change the map size");
    }

    @Test
    void testContainsValueWithCustomUnderlyingMap() {
        Map<String, Integer> backing = new HashMap<>();
        backing.put("alpha", 10);
        backing.put("beta", 20);
        // Provide the custom backing map to the SealableMap constructor
        SealableMap<String, Integer> customMap = new SealableMap<>(backing, sealedSupplier);
        assertTrue(customMap.containsValue(10), "Custom map should contain value 10");
        assertFalse(customMap.containsValue(30), "Custom map should not contain value 30");
    }

    @ParameterizedTest(name = "{index}: {0.description}")
    @MethodSource("basicFunctionalityCases")
    void testContainsValue_BasicFunctionality(TestCase tc) {
        // If a custom backing map is supplied, create a SealableMap with it; otherwise use the default map.
        SealableMap<String, Object> testMap = (tc.backingMap != null) ? new SealableMap<>(tc.backingMap, sealedSupplier) : new SealableMap<>(sealedSupplier);
        // Populate the default map when no custom backing is provided (mirrors @BeforeEach)
        if (tc.backingMap == null) {
            testMap.put("one", 1);
            testMap.put("two", 2);
            testMap.put("three", 3);
            testMap.put(null, null);
        }
        boolean actual = testMap.containsValue(tc.value);
        assertEquals(tc.expected, actual, tc.description);
    }

    @ParameterizedTest(name = "{index}: {0.description}")
    @MethodSource("customObjectEqualityCases")
    void testContainsValue_CustomObjectEquality(TestCase tc) {
        SealableMap<String, Object> testMap = new SealableMap<>(tc.backingMap, sealedSupplier);
        boolean actual = testMap.containsValue(tc.value);
        assertEquals(tc.expected, actual, tc.description);
    }
}
