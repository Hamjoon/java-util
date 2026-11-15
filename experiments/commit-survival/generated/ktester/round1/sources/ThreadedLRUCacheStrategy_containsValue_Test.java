package com.cedarsoftware.util.cache;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

class ThreadedLRUCacheStrategy_containsValue_Test {

    private static final int CAPACITY = 5;

    private static final int CLEANUP_DELAY_MILLIS = 100;

    private ThreadedLRUCacheStrategy<String, String> cache;

    private static class BasicCase {

        final Object value;

        final boolean expected;

        final String description;

        BasicCase(Object value, boolean expected, String description) {
            this.value = value;
            this.expected = expected;
            this.description = description;
        }
    }

    static final class Person {

        final String name;

        final int age;

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
            Person p = (Person) o;
            return age == p.age && name.equals(p.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() * 31 + age;
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + '}';
        }
    }

    static final class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("equals failure");
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh cache instance for each test.
        cache = new ThreadedLRUCacheStrategy<>(CAPACITY, CLEANUP_DELAY_MILLIS);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the cache is cleared so that subsequent tests start clean.
        cache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared scheduler used by all ThreadedLRUCacheStrategy instances.
        ThreadedLRUCacheStrategy.shutdown();
    }

    private static Stream<BasicCase> basicCases() {
        return Stream.of(new BasicCase(null, false, "Cache is empty; containsValue(null) should return false."), new BasicCase("test", true, "Cache contains a node with value \"test\"; containsValue should return true."), new BasicCase("missing", false, "Cache does not contain the given value; method should return false."));
    }

    @Test
    void containsValueReturnsTrueWhenValueIsPresent() {
        // given
        cache.put("one", "apple");
        cache.put("two", "banana");
        cache.put("three", "cherry");
        // when
        boolean result = cache.containsValue("banana");
        // then
        assertTrue(result, "containsValue should return true for an existing value");
    }

    @Test
    void containsValueReturnsFalseWhenValueIsAbsent() {
        // given
        cache.put("a", "alpha");
        cache.put("b", "beta");
        // when
        boolean result = cache.containsValue("gamma");
        // then
        assertFalse(result, "containsValue should return false when the value is not in the cache");
    }

    @Test
    void containsValueHandlesNullValues() {
        // given
        cache.put("nullKey", null);
        // when
        boolean result = cache.containsValue(null);
        // then
        assertTrue(result, "containsValue should correctly identify a stored null value");
    }

    @Test
    void containsValueDoesNotModifyCacheSize() {
        // given
        cache.put("k1", "v1");
        cache.put("k2", "v2");
        int sizeBefore = cache.size();
        // when
        cache.containsValue("v2");
        // then
        assertEquals(sizeBefore, cache.size(), "containsValue must not change the size of the cache");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicCases")
    void containsValueBasicFunctionality(BasicCase c) {
        // Arrange
        if (c.value != null && c.expected) {
            // Only populate the cache when we expect a hit.
            cache.put("key", c.value);
        }
        // Act
        boolean result = cache.containsValue(c.value);
        // Assert
        assertEquals(c.expected, result, c.description);
    }

    @Test
    void containsValueWithNullPresent() {
        cache.put("nullKey", null);
        assertTrue(cache.containsValue(null), "Cache contains a node whose value is null; containsValue(null) should return true.");
    }

    @Test
    void containsValueWithDuplicateValues() {
        cache.put("k1", "dup");
        // duplicate value
        cache.put("k2", "dup");
        cache.put("k3", "other");
        assertTrue(cache.containsValue("dup"), "Cache has multiple entries with value \"dup\"; should return true.");
        assertFalse(cache.containsValue("unique"), "Cache contains several entries but none with value \"unique\"; should return false.");
    }

    @Test
    void containsValueWithCustomObjectEquality() {
        Person stored = new Person("Alice", 30);
        cache.put("personKey", stored);
        // Different instance but equal according to Person.equals()
        Person queryEqual = new Person("Alice", 30);
        Person queryNotEqual = new Person("Bob", 25);
        assertTrue(cache.containsValue(queryEqual), "Equal Person instance should be found.");
        assertFalse(cache.containsValue(queryNotEqual), "Non‑equal Person instance should not be found.");
    }

    @Test
    void containsValueWithNonMatchingType() {
        // Integer stored
        cache.put("intKey", 123);
        assertFalse(cache.containsValue(123L), "Long value should not match stored Integer even if numeric value is the same.");
    }

    @Test
    void containsValueAfterRemoval() {
        cache.put("temp", "temp");
        cache.remove("temp");
        assertFalse(cache.containsValue("temp"), "Value was removed; containsValue should return false.");
    }

    @Test
    void containsValueWhenInternalCacheIsNull() throws Exception {
        // Use reflection to set the private 'cache' field to null.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cacheField.set(cache, null);
        assertThrows(NullPointerException.class, () -> cache.containsValue("any"), "When internal cache map is null, containsValue should throw NullPointerException.");
    }

    @Test
    void containsValueWithNullNodeInCache() throws Exception {
        // Insert a null Node for a key directly into the internal map.
        Field cacheField = ThreadedLRUCacheStrategy.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Object>> internalMap = (ConcurrentMap<String, ThreadedLRUCacheStrategy.Node<String, Object>>) cacheField.get(cache);
        internalMap.put("nullNodeKey", null);
        assertThrows(NullPointerException.class, () -> cache.containsValue("any"), "Iterating over a null node should cause NullPointerException.");
    }

    @Test
    void containsValueWhenNodeValueEqualsThrows() {
        BadEquals bad = new BadEquals();
        cache.put("badKey", bad);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> cache.containsValue(null), "Exception from value.equals should be propagated.");
        assertEquals("equals failure", ex.getMessage());
    }
}
