package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_get_Test {

    static Stream<Arguments> standardBehaviorProvider() {
        return Stream.of(// key exists, non‑null value
        Arguments.of(new HashMap<String, Integer>() {

            {
                put("a", 1);
            }
        }, "a", 0, 1, "Key exists in the map and the associated value is non‑null; the method should return that value."), // key exists, value null -> default
        Arguments.of(new HashMap<String, Integer>() {

            {
                put("a", null);
            }
        }, "a", 0, 0, "Key exists but the stored value is null; the method must fall back to the default value."), // key absent -> default
        Arguments.of(new HashMap<String, Integer>() {

            {
                put("b", 2);
            }
        }, "a", 5, 5, "Key is absent; the method should return the supplied default value."), // key absent, default is null -> null
        Arguments.of(new HashMap<String, Integer>() {

            {
                put("b", 2);
            }
        }, "a", null, null, "Key is absent and the default value is null; the method should return null."), // map is null -> NPE
        Arguments.of(null, "a", 0, NullPointerException.class, "The map argument is null; invoking map.get(...) throws NullPointerException."), // null key present in map
        Arguments.of(new HashMap<Object, String>() {

            {
                put(null, "value");
            }
        }, null, "default", "value", "Key is null and a mapping for null exists; the method should return the stored value."), // null key absent -> default
        Arguments.of(new HashMap<String, String>() {

            {
                put("a", "v");
            }
        }, null, "default", "default", "Key is null but the map contains no null entry; the method should return the default."));
    }

    static Stream<Arguments> differentKeyValueTypesProvider() {
        return Stream.of(// Integer key, existing
        Arguments.of(new HashMap<Integer, String>() {

            {
                put(1, "one");
            }
        }, 1, "def", "one", "Map with Integer keys; existing key returns its value."), // Integer key, value null -> default
        Arguments.of(new HashMap<Integer, String>() {

            {
                put(1, null);
            }
        }, 1, "def", "def", "Integer key present but mapped to null; default is returned."), // Complex value type (List)
        Arguments.of(new LinkedHashMap<String, List<Integer>>() {

            {
                put("list", Arrays.asList(1, 2, 3));
            }
        }, "list", Collections.emptyList(), Arrays.asList(1, 2, 3), "Map with complex value type (List); existing key returns the list."));
    }

    static Stream<Arguments> boundaryConditionsProvider() {
        return Stream.of(// Empty map -> default
        Arguments.of(new HashMap<String, Integer>(), "any", 42, 42, "Empty map should always return the default value."), // Singleton map, matching key
        Arguments.of(Collections.singletonMap("only", 99), "only", 0, 99, "Singleton map with matching key returns its value."), // Singleton map, null value -> default
        Arguments.of(Collections.singletonMap("only", null), "only", 0, 0, "Singleton map with null value returns default."));
    }

    static Stream<Arguments> defaultValueVariationsProvider() {
        return Stream.of(// Boolean default when key missing
        Arguments.of(new HashMap<String, Boolean>() {

            {
                put("flag", true);
            }
        }, "missing", false, false, "Boolean default value is returned when key missing."), // String default when key present with null value
        Arguments.of(new HashMap<String, String>() {

            {
                put("k", null);
            }
        }, "k", "fallback", "fallback", "Key present with null value; default string is returned."), // Null default when key missing
        Arguments.of(new HashMap<String, Object>() {

            {
                put("obj", new Date(0));
            }
        }, "missing", null, null, "Missing key with null default returns null."));
    }

    static Stream<Arguments> mapImplementationsProvider() {
        return Stream.of(// Unmodifiable map
        Arguments.of(Collections.unmodifiableMap(new HashMap<String, Integer>() {

            {
                put("a", 10);
            }
        }), "a", 0, 10, "Unmodifiable map behaves like a normal map for get operation."), // ConcurrentHashMap with existing key
        Arguments.of(new ConcurrentHashMap<String, String>() {

            {
                put("c", "conc");
            }
        }, "c", "def", "conc", "ConcurrentHashMap with existing key returns its value."));
    }

    static Stream<Arguments> incompatibleKeyTypeProvider() {
        return Stream.of(// TreeMap<String, Integer> with Integer key
        Arguments.of(new TreeMap<String, Integer>() {

            {
                put("a", 1);
            }
        }, 1, 0, ClassCastException.class, "TreeMap expects keys of type String; passing an Integer key causes a ClassCastException during map.get."), // TreeMap<Integer, String> with String key (reverse order comparator)
        Arguments.of(new TreeMap<Integer, String>(Collections.reverseOrder()) {

            {
                put(2, "two");
            }
        }, "2", "def", ClassCastException.class, "TreeMap with Integer keys receives a String key; the internal comparison fails and a ClassCastException is thrown."));
    }

    static Stream<Arguments> nullKeyNotAllowedProvider() {
        return Stream.of(// ConcurrentHashMap does not allow null keys
        Arguments.of(new ConcurrentHashMap<String, Integer>() {

            {
                put("a", 1);
            }
        }, null, 0, NullPointerException.class, "ConcurrentHashMap does not permit null keys; invoking get(null) throws NullPointerException."), // Another ConcurrentHashMap example
        Arguments.of(new ConcurrentHashMap<String, String>() {

            {
                put("c", "conc");
            }
        }, null, "def", NullPointerException.class, "Even when the map contains entries, a null key triggers NullPointerException in ConcurrentHashMap."));
    }

    @Test
    void get_ReturnsExistingValue_WhenKeyIsPresent() {
        // given
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Integer defaultValue = -1;
        // when
        Integer result = MapUtilities.get(map, "one", defaultValue);
        // then
        assertEquals(1, result);
    }

    @Test
    void get_ReturnsDefaultValue_WhenKeyIsAbsent() {
        // given
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        Integer defaultValue = -1;
        // when
        Integer result = MapUtilities.get(map, "two", defaultValue);
        // then
        assertEquals(defaultValue, result);
    }

    @Test
    void get_ReturnsDefaultValue_WhenMapContainsNullValue() {
        // given
        Map<String, Integer> map = new HashMap<>();
        map.put("nullKey", null);
        Integer defaultValue = 42;
        // when
        Integer result = MapUtilities.get(map, "nullKey", defaultValue);
        // then
        assertEquals(defaultValue, result);
    }

    @Test
    void get_HandlesNullKeyGracefully() {
        // given
        Map<Object, String> map = new HashMap<>();
        map.put(null, "nullValue");
        String defaultValue = "default";
        // when
        String result = MapUtilities.get(map, null, defaultValue);
        // then
        assertEquals("nullValue", result);
    }

    @Test
    void get_UsesDefaultWhenMapIsEmpty() {
        // given
        Map<String, String> emptyMap = new HashMap<>();
        String defaultValue = "fallback";
        // when
        String result = MapUtilities.get(emptyMap, "anyKey", defaultValue);
        // then
        assertEquals(defaultValue, result);
    }

    @Test
    void get_ReturnsDefaultWhenMapIsNull() {
        // given
        Map<String, String> nullMap = null;
        String defaultValue = "default";
        // when / then
        // The utility method does not guard against a null map, so a NullPointerException is expected.
        assertThrows(NullPointerException.class, () -> MapUtilities.get(nullMap, "key", defaultValue));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("standardBehaviorProvider")
    void get_StandardBehavior(Map<?, ?> map, Object key, Object def, Object expected, String description) {
        if (expected instanceof Class) {
            // Expected an exception
            assertThrows((Class<? extends Throwable>) expected, () -> MapUtilities.get(map, key, def), description);
        } else {
            Object actual = MapUtilities.get(map, key, def);
            assertEquals(expected, actual, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("differentKeyValueTypesProvider")
    void get_DifferentKeyValueTypes(Map<?, ?> map, Object key, Object def, Object expected, String description) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapUtilities.get(map, key, def), description);
        } else {
            Object actual = MapUtilities.get(map, key, def);
            assertEquals(expected, actual, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("boundaryConditionsProvider")
    void get_BoundaryConditions(Map<?, ?> map, Object key, Object def, Object expected, String description) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapUtilities.get(map, key, def), description);
        } else {
            Object actual = MapUtilities.get(map, key, def);
            assertEquals(expected, actual, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("defaultValueVariationsProvider")
    void get_DefaultValueVariations(Map<?, ?> map, Object key, Object def, Object expected, String description) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapUtilities.get(map, key, def), description);
        } else {
            Object actual = MapUtilities.get(map, key, def);
            assertEquals(expected, actual, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("mapImplementationsProvider")
    void get_MapImplementations(Map<?, ?> map, Object key, Object def, Object expected, String description) {
        if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapUtilities.get(map, key, def), description);
        } else {
            Object actual = MapUtilities.get(map, key, def);
            assertEquals(expected, actual, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("incompatibleKeyTypeProvider")
    void get_IncompatibleKeyType(Map<?, ?> map, Object key, Object def, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> MapUtilities.get(map, key, def), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("nullKeyNotAllowedProvider")
    void get_NullKeyNotAllowed(Map<?, ?> map, Object key, Object def, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> MapUtilities.get(map, key, def), description);
    }
}
