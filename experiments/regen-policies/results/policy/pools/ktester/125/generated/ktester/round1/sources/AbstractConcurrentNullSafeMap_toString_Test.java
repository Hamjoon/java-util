package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_toString_Test {

    /**
     * Concrete instance of the abstract class used for testing.
     */
    private AbstractConcurrentNullSafeMap<Object, Object> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a simple concrete subclass that forwards to a ConcurrentHashMap.
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new AbstractConcurrentNullSafeMap<Object, Object>(backing) {
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        map.clear();
    }

    private static Stream<Arguments> basicScenarioProvider() {
        return Stream.of(Arguments.of("Single non‑null entry", (Runnable) () -> map.put("foo", "bar"), "{foo=bar}"), Arguments.of("Null key with non‑null value", (Runnable) () -> map.put(null, "bar"), "{null=bar}"), Arguments.of("Non‑null key with null value", (Runnable) () -> map.put("foo", null), "{foo=null}"), Arguments.of("Both key and value null", (Runnable) () -> map.put(null, null), "{null=null}"), Arguments.of("Multiple entries (order not significant)", (Runnable) () -> {
            map.put("a", "1");
            map.put("b", "2");
            map.put("c", "3");
        }, "contains:a=1;b=2;c=3"));
    }

    private static Stream<Arguments> nonStringScenarioProvider() {
        return Stream.of(Arguments.of("Integer key, Boolean value", (Runnable) () -> map.put(1, true), "{1=true}"), Arguments.of("Boolean key, Double value", (Runnable) () -> map.put(false, 3.14), "{false=3.14}"), Arguments.of("Mixed non‑String types", (Runnable) () -> {
            map.put(1, true);
            map.put(false, 3.14);
        }, "contains:1=true;false=3.14"));
    }

    private static Stream<Arguments> specialCharScenarioProvider() {
        return Stream.of(Arguments.of("Key/value with commas and equals", (Runnable) () -> map.put("a,b", "c=d"), "{a,b=c=d}"), Arguments.of("Braces in key, brackets in value", (Runnable) () -> map.put("{brace}", "[list]"), "{{brace}=[list]}"), Arguments.of("Multiple entries with special chars", (Runnable) () -> {
            map.put("a,b", "c=d");
            map.put("{brace}", "[list]");
        }, "contains:a,b=c=d;{brace}=[list]"));
    }

    @Test
    void toStringOnEmptyMapReturnsBraces() {
        // GIVEN an empty map
        // WHEN toString is invoked
        String result = map.toString();
        // THEN the result should be the empty‑map representation.
        assertEquals("{}", result, "Empty map should be represented as {}");
    }

    @Test
    void toStringContainsAllEntriesIncludingNullKeyAndValue() {
        // GIVEN a map populated with regular, null‑key and null‑value entries
        map.put("alpha", "A");
        map.put(null, "nullKeyValue");
        map.put("nullValueKey", null);
        map.put("beta", "B");
        // WHEN toString is called
        String result = map.toString();
        // THEN the resulting string must contain each entry.
        // The exact ordering is not guaranteed, so we check for the presence of substrings.
        assertTrue(result.contains("alpha=A"), "Result should contain the entry alpha=A");
        assertTrue(result.contains("null=nullKeyValue") || result.contains("nullKeyValue=null"), "Result should contain the null‑key entry");
        assertTrue(result.contains("nullValueKey=null") || result.contains("null=nullValueKey"), "Result should contain the null‑value entry");
        assertTrue(result.contains("beta=B"), "Result should contain the entry beta=B");
        // The string should start with '{' and end with '}' as per typical Map toString format.
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should be wrapped in curly braces");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicScenarioProvider")
    void toStringBasicScenarios(String description, Runnable population, String expected) {
        // GIVEN a map populated according to the test case
        population.run();
        // WHEN toString is called
        String result = map.toString();
        // THEN the string must match the expected representation.
        // For cases where order is not significant we compare token presence.
        if (expected.startsWith("contains")) {
            // format: "contains:key1=value1;key2=value2;..."
            String[] tokens = expected.substring("contains:".length()).split(";");
            for (String token : tokens) {
                assertTrue(result.contains(token), () -> "Result should contain token \"" + token + "\" but was: " + result);
            }
            assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should be wrapped in curly braces");
        } else {
            assertEquals(expected, result, () -> description + " – expected \"" + expected + "\" but got \"" + result + "\"");
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nonStringScenarioProvider")
    void toStringNonStringKeysValues(String description, Runnable population, String expected) {
        population.run();
        String result = map.toString();
        if (expected.startsWith("contains")) {
            String[] tokens = expected.substring("contains:".length()).split(";");
            for (String token : tokens) {
                assertTrue(result.contains(token), () -> "Result should contain token \"" + token + "\" but was: " + result);
            }
            assertTrue(result.startsWith("{") && result.endsWith("}"));
        } else {
            assertEquals(expected, result, () -> description + " – expected \"" + expected + "\" but got \"" + result + "\"");
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("specialCharScenarioProvider")
    void toStringSpecialCharacters(String description, Runnable population, String expected) {
        population.run();
        String result = map.toString();
        if (expected.startsWith("contains")) {
            String[] tokens = expected.substring("contains:".length()).split(";");
            for (String token : tokens) {
                assertTrue(result.contains(token), () -> "Result should contain token \"" + token + "\" but was: " + result);
            }
            assertTrue(result.startsWith("{") && result.endsWith("}"));
        } else {
            assertEquals(expected, result, () -> description + " – expected \"" + expected + "\" but got \"" + result + "\"");
        }
    }

    @Test
    void toStringAfterPutAllAndClear() {
        // GIVEN a map with some initial data
        map.put("old", "value");
        // ensure clean state
        map.clear();
        // WHEN putAll is called with a new map
        Map<String, Integer> other = new HashMap<>();
        other.put("x", 1);
        other.put("y", 2);
        map.putAll(other);
        // THEN toString reflects only the new entries
        String result = map.toString();
        assertTrue(result.startsWith("{") && result.endsWith("}"));
        assertTrue(result.contains("x=1"));
        assertTrue(result.contains("y=2"));
        assertFalse(result.contains("old"));
    }

    @Test
    void toStringAfterRemove() {
        map.put("x", 1);
        map.put("y", 2);
        map.remove("y");
        String result = map.toString();
        assertEquals("{x=1}", result);
    }

    @Test
    void toStringAfterClearOnNonEmptyMap() {
        map.put("a", "A");
        map.put("b", "B");
        map.clear();
        assertEquals("{}", map.toString());
    }

    @Test
    void toStringWithNestedMapValue() {
        Map<String, Integer> inner = new LinkedHashMap<>();
        inner.put("a", 1);
        inner.put("b", 2);
        map.put("inner", inner);
        String result = map.toString();
        // The inner map's own toString uses its own braces.
        assertEquals("{inner={a=1, b=2}}", result);
    }

    @Test
    void toStringWithMultipleNestedMaps() {
        Map<String, Integer> inner = new LinkedHashMap<>();
        inner.put("a", 1);
        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("inner", inner);
        map.put("outer", outer);
        String result = map.toString();
        assertEquals("{outer={inner={a=1}}}", result);
    }

    @Test
    void toStringLargeMapContainsAllEntries() {
        // Populate map with 100 distinct entries
        for (int i = 0; i < 100; i++) {
            map.put("k" + i, "v" + i);
        }
        String result = map.toString();
        // Basic format checks
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should start with '{' and end with '}'");
        // Remove surrounding braces and split on ", "
        String inner = result.substring(1, result.length() - 1);
        String[] tokens = inner.split(", ");
        assertEquals(100, tokens.length, "Result should contain exactly 100 key=value tokens");
        // Verify a few random entries are present
        assertTrue(inner.contains("k0=v0"));
        assertTrue(inner.contains("k50=v50"));
        assertTrue(inner.contains("k99=v99"));
    }
}
