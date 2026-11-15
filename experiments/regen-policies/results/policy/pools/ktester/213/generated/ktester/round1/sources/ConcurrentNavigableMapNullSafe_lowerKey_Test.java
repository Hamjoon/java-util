package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@org.junit.jupiter.api.extension.ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_lowerKey_Test {

    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * Simple POJO used for the custom‑object comparator test.
     */
    private static final class Person {

        private final int age;

        Person(int age) {
            this.age = age;
        }

        int getAge() {
            return age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person person = (Person) o;
            return age == person.age;
        }

        @Override
        public int hashCode() {
            return Objects.hash(age);
        }

        @Override
        public String toString() {
            return "Person(age=" + age + ')';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test (natural ordering)
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to help GC (not strictly required)
        map.clear();
    }

    static Stream<Arguments> basicStringCases() {
        return Stream.of(// lowerKey("c") → "b"
        Arguments.of("c", "b"), // lowerKey("a") → null (smallest key)
        Arguments.of("a", null), // lowerKey("z") → "d" (key beyond highest)
        Arguments.of("z", "d"), // lowerKey(null) → "d" (null is greatest, predecessor is highest real key)
        Arguments.of(null, "d"), // lowerKey("any") on empty map → null (handled separately)
        // The following case expects null because map will be cleared before use
        // We'll handle it in a dedicated test below.
        Arguments.of("any", null));
    }

    static Stream<Arguments> descendingComparatorCases() {
        return Stream.of(// predecessor in descending order
        Arguments.of("b", "c"), Arguments.of("a", "b"), // lowest in descending view
        Arguments.of("c", null));
    }

    static Stream<Arguments> singleElementCases() {
        return Stream.of(// same key → no lower key
        Arguments.of("x", null), // greater key → predecessor is "x"
        Arguments.of("y", "x"), // smaller key → no lower key
        Arguments.of("w", null));
    }

    static Stream<Arguments> onlyNullKeyCases() {
        return Stream.of(// lowerKey(null) → null (no lower key)
        Arguments.of(null, null), // any non‑null key → null (null key is the only lower key)
        Arguments.of("any", null));
    }

    static Stream<Arguments> integerKeyCases() {
        return Stream.of(Arguments.of(4, 3), Arguments.of(1, null), Arguments.of(6, 5));
    }

    /**
     * Verify lowerKey returns the greatest key strictly less than the given key.
     * Normal (non‑null) keys are ordered according to their natural ordering.
     */
    @Test
    void lowerKeyNormalKeys() {
        // given
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // when / then
        assertNull(map.lowerKey("a"), "No key is lower than the smallest key");
        assertEquals("a", map.lowerKey("b"));
        assertEquals("b", map.lowerKey("c"));
        // key not present, still returns greatest lower
        assertEquals("c", map.lowerKey("d"));
    }

    /**
     * Verify that a null key is considered *last* in the ascending view.
     * Therefore lowerKey(null) should return the greatest non‑null key.
     */
    @Test
    void lowerKeyWithNullKey() {
        // given
        map.put("x", 10);
        map.put("y", 20);
        // null is treated as the greatest key
        map.put(null, 99);
        // when
        String lower = map.lowerKey(null);
        // then
        // The greatest key before the null sentinel is "y"
        assertEquals("y", lower);
    }

    /**
     * Verify that lowerKey on a map that only contains a null key returns null,
     * because there is no key less than the null sentinel.
     */
    @Test
    void lowerKeyOnlyNullKey() {
        // given
        map.put(null, 1);
        // when
        String lower = map.lowerKey(null);
        // then
        assertNull(lower, "When the map only contains a null key, lowerKey(null) should be null");
    }

    /**
     * Verify that lowerKey works correctly when the supplied key is null but the map
     * does not contain a null entry. The method should still treat null as the greatest
     * possible key and return the current greatest non‑null key.
     */
    @Test
    void lowerKeyNullKeyWhenMapHasNoNullEntry() {
        // given
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("gamma", 3);
        // note: no null key inserted
        // when
        String lower = map.lowerKey(null);
        // then
        assertEquals("gamma", lower, "null is considered greater than any real key");
    }

    /**
     * Verify that lowerKey returns null when the map is empty, regardless of the key supplied.
     */
    @Test
    void lowerKeyEmptyMap() {
        // when
        assertNull(map.lowerKey("any"), "Empty map should return null for any key");
        assertNull(map.lowerKey(null), "Empty map should return null for null key");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: lowerKey({0}) → {1}")
    @MethodSource("basicStringCases")
    void lowerKeyBasicFunctionality(String key, String expected) {
        // Populate the map with the keys required for the group
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        // null sentinel – treated as greatest
        map.put(null, 99);
        // Act
        String result = map.lowerKey(key);
        // Assert
        assertEquals(expected, result);
    }

    @Test
    void lowerKeyBasicFunctionality_EmptyMap() {
        // map is empty by default
        assertNull(map.lowerKey("any"));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: descending lowerKey({0}) → {1}")
    @MethodSource("descendingComparatorCases")
    void lowerKeyWithDescendingComparator(String key, String expected) {
        // Comparator that reverses natural order
        ConcurrentNavigableMapNullSafe<String, Integer> descMap = new ConcurrentNavigableMapNullSafe<>(Comparator.reverseOrder());
        // Populate with a,b,c
        descMap.put("a", 1);
        descMap.put("b", 2);
        descMap.put("c", 3);
        // Act
        String result = descMap.lowerKey(key);
        // Assert
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: single‑element lowerKey({0}) → {1}")
    @MethodSource("singleElementCases")
    void lowerKeySingleElement(String key, String expected) {
        // only element
        map.put("x", 42);
        String result = map.lowerKey(key);
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: only‑null lowerKey({0}) → {1}")
    @MethodSource("onlyNullKeyCases")
    void lowerKeyOnlyNullKeyPresent(Object key, Object expected) {
        // only null key
        map.put(null, 123);
        // The map is typed as <String,Integer>, but we can pass null or a String.
        // For the generic version we just cast.
        @SuppressWarnings("unchecked")
        String result = map.lowerKey((String) key);
        assertEquals((String) expected, result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "{index}: int lowerKey({0}) → {1}")
    @MethodSource("integerKeyCases")
    void lowerKeyIntegerKeys(Integer key, Integer expected) {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, 10);
        intMap.put(3, 30);
        intMap.put(5, 50);
        Integer result = intMap.lowerKey(key);
        assertEquals(expected, result);
    }

    // -----------------------------------------------------------------
    @Test
    void lowerKeyTypeMismatchThrowsClassCastException() {
        ConcurrentNavigableMapNullSafe<Integer, Integer> intMap = new ConcurrentNavigableMapNullSafe<>();
        intMap.put(1, 10);
        intMap.put(2, 20);
        intMap.put(3, 30);
        // The map expects Integer keys; passing a String should trigger a ClassCastException
        assertThrows(ClassCastException.class, () -> intMap.lowerKey("2"));
    }

    // -----------------------------------------------------------------
    @Test
    void lowerKeyCustomObjectComparator() {
        // Comparator based on age
        Comparator<Person> ageComparator = Comparator.comparingInt(Person::getAge);
        ConcurrentNavigableMapNullSafe<Person, String> personMap = new ConcurrentNavigableMapNullSafe<>(ageComparator);
        Person p20 = new Person(20);
        Person p30 = new Person(30);
        personMap.put(p20, "young");
        personMap.put(p30, "old");
        Person query = new Person(25);
        Person result = personMap.lowerKey(query);
        assertEquals(p20, result, "Expected the Person with age 20 to be the greatest key lower than 25");
    }

    @Test
    void lowerKeyCustomObjectComparator_NoLowerKey() {
        Comparator<Person> ageComparator = Comparator.comparingInt(Person::getAge);
        ConcurrentNavigableMapNullSafe<Person, String> personMap = new ConcurrentNavigableMapNullSafe<>(ageComparator);
        Person p20 = new Person(20);
        personMap.put(p20, "young");
        Person query = new Person(20);
        Person result = personMap.lowerKey(query);
        assertNull(result, "No Person with age less than 20 exists");
    }

    // -----------------------------------------------------------------
    @Test
    void lowerKeyComparatorThrowsRuntimeException() {
        Comparator<String> explodingComparator = (a, b) -> {
            if ("trigger".equals(a) || "trigger".equals(b)) {
                throw new RuntimeException("Comparator forced failure");
            }
            return a.compareTo(b);
        };
        ConcurrentNavigableMapNullSafe<String, Integer> explodingMap = new ConcurrentNavigableMapNullSafe<>(explodingComparator);
        explodingMap.put("alpha", 1);
        explodingMap.put("beta", 2);
        explodingMap.put("gamma", 3);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> explodingMap.lowerKey("trigger"));
        assertEquals("Comparator forced failure", ex.getMessage());
    }
}
