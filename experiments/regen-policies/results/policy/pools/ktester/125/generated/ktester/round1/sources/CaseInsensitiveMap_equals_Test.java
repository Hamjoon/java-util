package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_equals_Test {

    private CaseInsensitiveMap<String, String> ciMap;

    private Map<String, String> referenceMap;

    /**
     * Simple POJO with equality based on the {@code name} field.
     */
    private static final class Person {

        private final String name;

        Person(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Person))
                return false;
            Person person = (Person) o;
            return Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Populate a simple case‑insensitive map (used by the original tests)
        ciMap = new CaseInsensitiveMap<>();
        ciMap.put("One", "Alpha");
        ciMap.put("Two", "Beta");
        ciMap.put("Three", "Gamma");
        // Reference map with the same logical content (different key casing)
        referenceMap = new HashMap<>();
        referenceMap.put("one", "Alpha");
        referenceMap.put("TWO", "Beta");
        referenceMap.put("tHrEe", "Gamma");
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset static cache configuration to its default value for other tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /**
     * Provides a stream of arguments for the branch‑coverage tests.
     *
     * Each argument consists of:
     * <ul>
     *   <li>A description (used only for readability in the test output)</li>
     *   <li>A {@code Consumer<CaseInsensitiveMap>} that configures the map under test</li>
     *   <li>The "other" map to compare against</li>
     *   <li>The expected result of {@code equals}</li>
     * </ul>
     */
    private static Stream<Arguments> branchCoverageCases() {
        return Stream.of(// size mismatch – ciMap has 2 entries, other only 1
        arguments("Size mismatch", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("a", 1);
            map.put("b", 2);
        }, new HashMap<>(Map.of("a", 1)), false), // key not present – other contains a key that ciMap does not have
        arguments("Key not present", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("a", 1);
            map.put("b", 2);
        }, new HashMap<>(Map.of("b", 2, "c", 3)), false), // value mismatch – same keys, different value for "a"
        arguments("Value mismatch", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("a", 1);
            map.put("b", 2);
        }, new HashMap<>(Map.of("a", 99, "b", 2)), false), // exact match – identical map
        arguments("Exact match", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("a", 1);
            map.put("b", 2);
        }, new HashMap<>(Map.of("a", 1, "b", 2)), true), // case‑insensitive key match – other uses upper‑case keys
        arguments("Case‑insensitive key match", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("a", 1);
            map.put("b", 2);
        }, new HashMap<>(Map.of("A", 1, "B", 2)), true), // different map implementation – TreeMap
        arguments("Different Map implementation (TreeMap)", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("a", 1);
            map.put("b", 2);
        }, new TreeMap<>(Map.of("a", 1, "b", 2)), true), // null value handling – both maps contain a null value for "a"
        arguments("Null value handling", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("a", null);
            map.put("b", 2);
        }, new HashMap<>(Map.of("a", null, "b", 2)), true));
    }

    private static Stream<Arguments> additionalScenarios() {
        return Stream.of(// both maps empty
        arguments("Both empty maps", (Consumer<CaseInsensitiveMap>) CaseInsensitiveMap::clear, new HashMap<>(), true), // ciMap empty, other non‑empty
        arguments("Empty ciMap vs non‑empty other", (Consumer<CaseInsensitiveMap>) CaseInsensitiveMap::clear, new HashMap<>(Map.of("x", 1)), false), // null key with same value
        arguments("Null key with same value", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put(null, "val");
        }, new HashMap<>(Map.of((Object) null, "val")), true), // null key with different value
        arguments("Null key with different value", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put(null, "val");
        }, new HashMap<>(Map.of((Object) null, "other")), false), // ciMap has null key, other map missing it
        arguments("ciMap has null key, other missing it", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put(null, "val");
            map.put("a", 1);
        }, new HashMap<>(Map.of("a", 1)), false), // upper‑case keys in ciMap, lower‑case in other
        arguments("Upper‑case keys in ciMap vs lower‑case in other", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("A", 1);
            map.put("B", 2);
        }, new HashMap<>(Map.of("a", 1, "b", 2)), true), // custom Person objects (equals based on name)
        arguments("Custom Person objects with equal content", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("person", new Person("John"));
        }, new HashMap<>(Map.of("person", new Person("John"))), true), // array values – should be false because Arrays are compared by reference
        arguments("Array values (different instances)", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("arr", new int[] { 1, 2 });
        }, new HashMap<>(Map.of("arr", new int[] { 1, 2 })), false), // unmodifiable map – should be true
        arguments("Unmodifiable map implementation", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("a", 1);
            map.put("b", 2);
        }, Collections.unmodifiableMap(new HashMap<>(Map.of("a", 1, "b", 2))), true), // concurrent hash map – should be true
        arguments("ConcurrentHashMap implementation", (Consumer<CaseInsensitiveMap>) map -> {
            map.clear();
            map.put("a", 1);
            map.put("b", 2);
        }, new ConcurrentHashMap<>(Map.of("a", 1, "b", 2)), true));
    }

    @Test
    void equalsSameInstance() {
        assertTrue(ciMap.equals(ciMap), "Map should be equal to itself");
    }

    @Test
    void equalsNull() {
        assertFalse(ciMap.equals(null), "Map should not be equal to null");
    }

    @Test
    void equalsNonMapObject() {
        assertFalse(ciMap.equals("not a map"), "Map should not be equal to a non‑Map object");
    }

    @Test
    void equalsDifferentSize() {
        Map<String, String> smaller = new HashMap<>();
        smaller.put("one", "Alpha");
        // missing "three"
        smaller.put("two", "Beta");
        assertFalse(ciMap.equals(smaller), "Maps of different sizes must not be equal");
    }

    @Test
    void equalsMissingKey() {
        Map<String, String> missingKey = new HashMap<>(referenceMap);
        // remove one entry
        missingKey.remove("two");
        assertFalse(ciMap.equals(missingKey), "Maps missing a key must not be equal");
    }

    @Test
    void equalsDifferentValueForKey() {
        Map<String, String> differentValue = new HashMap<>(referenceMap);
        differentValue.put("two", "DIFFERENT");
        assertFalse(ciMap.equals(differentValue), "Maps with a different value for a matching key must not be equal");
    }

    @Test
    void equalsCaseInsensitiveKeys() {
        assertTrue(ciMap.equals(referenceMap), "Case‑insensitive map should be equal to a regular map with keys in different case");
    }

    @Test
    void equalsWithNonStringKeys() {
        // Build a map that contains non‑String keys alongside String keys
        CaseInsensitiveMap<Object, String> mixedCiMap = new CaseInsensitiveMap<>();
        mixedCiMap.put(1, "One");
        mixedCiMap.put("Two", "TwoValue");
        mixedCiMap.put(3L, "Three");
        Map<Object, String> mixedReference = new HashMap<>();
        mixedReference.put(1, "One");
        // different case for the String key
        mixedReference.put("two", "TwoValue");
        mixedReference.put(3L, "Three");
        assertTrue(mixedCiMap.equals(mixedReference), "Equality must work when maps contain a mixture of String and non‑String keys");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("branchCoverageCases")
    void equals_branchCoverage(String description, Consumer<CaseInsensitiveMap> ciMapConfigurer, Map<?, ?> other, boolean expected) {
        // Re‑configure the map under test according to the scenario
        ciMapConfigurer.accept(ciMap);
        assertEquals(expected, ciMap.equals(other), () -> "Branch coverage case failed: " + description);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("additionalScenarios")
    void equals_additionalScenarios(String description, Consumer<CaseInsensitiveMap> ciMapConfigurer, Map<?, ?> other, boolean expected) {
        ciMapConfigurer.accept(ciMap);
        assertEquals(expected, ciMap.equals(other), () -> "Additional scenario failed: " + description);
    }

    @Test
    void equals_nonStringKey_throwsClassCastException() {
        // ciMap is already populated with String keys from @BeforeEach
        Map<Integer, String> other = new HashMap<>(Map.of(123, "value"));
        assertThrows(ClassCastException.class, () -> ciMap.equals(other), "Providing a non‑String key should trigger ClassCastException");
    }
}
