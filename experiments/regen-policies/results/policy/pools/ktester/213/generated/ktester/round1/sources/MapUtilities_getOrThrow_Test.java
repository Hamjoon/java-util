package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_getOrThrow_Test {

    static Stream<Arguments> keyAbsentProvider() {
        return Stream.of(Arguments.of("When the key is not present, the supplied throwable should be thrown.", new HashMap<String, Integer>() {

            {
                put("x", 100);
            }
        }, "y", new IllegalArgumentException("key not present"), IllegalArgumentException.class, "key not present"), Arguments.of("Even if map contains null values, absence of the key triggers the throwable.", Map.of(1, (Object) null), 2, new NoSuchElementException("absent"), NoSuchElementException.class, "absent"));
    }

    static Stream<Arguments> keyPresentProvider() {
        return Stream.of(Arguments.of("When the map contains the requested key, the associated value is returned.", new HashMap<String, Integer>() {

            {
                put("alpha", 42);
            }
        }, "alpha", new IllegalArgumentException("should not be thrown"), 42), Arguments.of("A key that is present with a null value should cause the method to return null (not throw).", new HashMap<String, Object>() {

            {
                put("beta", null);
            }
        }, "beta", new RuntimeException("unexpected"), null));
    }

    static Stream<Arguments> nullMapProvider() {
        return Stream.of(Arguments.of("Passing a null map must result in a NullPointerException with the documented message.", null, "any", new IllegalStateException("map is null"), "Map parameter cannot be null"));
    }

    static Stream<Arguments> nullThrowableProvider() {
        return Stream.of(Arguments.of("A null throwable argument should cause a NullPointerException before any map lookup.", Map.of("k", 1), "k", null, "Throwable object cannot be null"));
    }

    static Stream<Arguments> nullKeyHandlingProvider() {
        return Stream.of(// HashMap permits null keys – value should be returned
        Arguments.of("HashMap permits null keys; when the supplied key is null and present, the corresponding value is returned.", new HashMap<Object, String>() {

            {
                put(null, "nullKeyValue");
            }
        }, null, new IllegalArgumentException("missing null key"), "nullKeyValue", null), // Null key absent – supplied throwable should be thrown
        Arguments.of("When the map does not contain a null key, the supplied throwable is thrown.", new HashMap<String, Integer>() {

            {
                put("a", 1);
            }
        }, null, new IllegalArgumentException("null key absent"), new IllegalArgumentException("null key absent"), IllegalArgumentException.class));
    }

    static Stream<Arguments> emptyMapProvider() {
        return Stream.of(Arguments.of("An empty map never contains any key, so the throwable must be thrown.", Collections.emptyMap(), "nothing", new IllegalStateException("empty map"), IllegalStateException.class, "empty map"));
    }

    static Stream<Arguments> immutableMapProvider() {
        Map<String, String> mutable = new HashMap<>() {

            {
                put("a", "alpha");
            }
        };
        Map<String, String> unmodifiable = Collections.unmodifiableMap(mutable);
        return Stream.of(Arguments.of("The method works with unmodifiable maps; a present key returns its value.", unmodifiable, "a", new RuntimeException("should not happen"), "alpha", null), Arguments.of("Even for immutable maps, a missing key triggers the supplied throwable.", unmodifiable, "b", new IllegalArgumentException("key missing"), new IllegalArgumentException("key missing"), IllegalArgumentException.class));
    }

    static Stream<Arguments> differentMapImplProvider() {
        // TreeMap example
        TreeMap<Integer, String> treeMap = new TreeMap<>() {

            {
                put(10, "ten");
                put(20, "twenty");
            }
        };
        // EnumMap example
        EnumMap<java.time.DayOfWeek, String> enumMap = new EnumMap<>(java.time.DayOfWeek.class);
        enumMap.put(java.time.DayOfWeek.MONDAY, "Mon");
        return Stream.of(Arguments.of("TreeMap (a SortedMap) should behave like any other map; present key returns its value.", treeMap, 20, new IllegalArgumentException("not found"), "twenty", null), Arguments.of("EnumMap works correctly; a present enum key returns the associated value.", enumMap, java.time.DayOfWeek.MONDAY, new IllegalArgumentException("missing"), "Mon", null));
    }

    static Stream<Arguments> nullKeyNonNullableProvider() {
        TreeMap<Integer, String> treeMap = new TreeMap<>() {

            {
                put(1, "one");
            }
        };
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>() {

            {
                put("a", 1);
            }
        };
        return Stream.of(Arguments.of("TreeMap does not permit null keys; calling containsKey(null) throws NullPointerException before the supplied throwable can be used.", treeMap, null, new IllegalArgumentException("null not allowed"), NullPointerException.class), Arguments.of("ConcurrentHashMap rejects null keys; the method should propagate the NullPointerException thrown by containsKey.", concurrentMap, null, new IllegalStateException("null key"), NullPointerException.class));
    }

    static Stream<Arguments> incompatibleKeyTypeProvider() {
        TreeMap<Integer, String> treeMap = new TreeMap<>() {

            {
                put(10, "ten");
            }
        };
        EnumMap<java.time.DayOfWeek, String> enumMap = new EnumMap<>(java.time.DayOfWeek.class);
        enumMap.put(java.time.DayOfWeek.MONDAY, "Mon");
        return Stream.of(Arguments.of("TreeMap requires keys that are mutually comparable. Supplying a String when the map expects Integer causes containsKey to throw ClassCastException.", treeMap, "stringKey", new IllegalArgumentException("type mismatch"), ClassCastException.class), Arguments.of("EnumMap only accepts enum constants as keys. Passing a String results in a ClassCastException from containsKey.", enumMap, "MONDAY", new IllegalArgumentException("type mismatch"), ClassCastException.class));
    }

    @Test
    void testGetOrThrowReturnsExistingValue() {
        Map<String, Object> map = new TreeMap<>();
        map.put("foo", Boolean.TRUE);
        Object result = MapUtilities.getOrThrow(map, "foo", new RuntimeException("should not be thrown"));
        assertTrue(result instanceof Boolean);
        assertTrue((Boolean) result);
    }

    @Test
    void testGetOrThrowReturnsNullWhenValueIsNull() {
        Map<String, Object> map = new TreeMap<>();
        map.put("bar", null);
        Object result = MapUtilities.getOrThrow(map, "bar", new RuntimeException("should not be thrown"));
        assertNull(result);
    }

    @Test
    void testGetOrThrowThrowsProvidedThrowableWhenKeyMissing() {
        Map<String, Object> map = new TreeMap<>();
        map.put("foo", Boolean.TRUE);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> MapUtilities.getOrThrow(map, "missingKey", new RuntimeException("key not found")));
        assertEquals("key not found", thrown.getMessage());
    }

    @Test
    void testGetOrThrowNullMapThrowsNPE() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.getOrThrow(null, "anyKey", new RuntimeException("any")));
        assertEquals("Map parameter cannot be null", npe.getMessage());
    }

    @Test
    void testGetOrThrowNullThrowableThrowsNPE() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.getOrThrow(map, "key", null));
        assertEquals("Throwable object cannot be null", npe.getMessage());
    }

    @Test
    void testMapUtilitiesConstructorIsPrivate() throws Exception {
        // Verify that the utility class cannot be instantiated publicly
        java.lang.reflect.Constructor<MapUtilities> con = MapUtilities.class.getDeclaredConstructor();
        assertEquals(java.lang.reflect.Modifier.PRIVATE, con.getModifiers() & java.lang.reflect.Modifier.PRIVATE);
        con.setAccessible(true);
        assertNotNull(con.newInstance());
    }

    // ---------- Group: getOrThrow - key absent ----------
    @ParameterizedTest(name = "[{index}] key absent – {0}")
    @MethodSource("keyAbsentProvider")
    void testKeyAbsent(String description, Map<?, ?> map, Object key, Throwable throwable, Class<? extends Throwable> expectedException, String expectedMessage) {
        Throwable thrown = assertThrows(expectedException, () -> MapUtilities.getOrThrow(map, key, throwable));
        assertEquals(expectedMessage, thrown.getMessage(), description);
    }

    // ---------- Group: getOrThrow - key present returns value ----------
    @ParameterizedTest(name = "[{index}] key present – {0}")
    @MethodSource("keyPresentProvider")
    void testKeyPresentReturnsValue(String description, Map<?, ?> map, Object key, Throwable throwable, Object expectedValue) {
        Object result = MapUtilities.getOrThrow(map, key, throwable);
        assertEquals(expectedValue, result, description);
    }

    // ---------- Group: getOrThrow - null map parameter ----------
    @ParameterizedTest(name = "[{index}] null map – {0}")
    @MethodSource("nullMapProvider")
    void testNullMapParameter(String description, Map<?, ?> map, Object key, Throwable throwable, String expectedMessage) {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.getOrThrow(map, key, throwable));
        assertEquals(expectedMessage, npe.getMessage(), description);
    }

    // ---------- Group: getOrThrow - null throwable parameter ----------
    @ParameterizedTest(name = "[{index}] null throwable – {0}")
    @MethodSource("nullThrowableProvider")
    void testNullThrowableParameter(String description, Map<?, ?> map, Object key, Throwable throwable, String expectedMessage) {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.getOrThrow(map, key, throwable));
        assertEquals(expectedMessage, npe.getMessage(), description);
    }

    // ---------- Group: getOrThrow - null key handling ----------
    @ParameterizedTest(name = "[{index}] null key handling – {0}")
    @MethodSource("nullKeyHandlingProvider")
    void testNullKeyHandling(String description, Map<?, ?> map, Object key, Throwable throwable, Object expectedResultOrException, Class<? extends Throwable> expectedException) {
        if (expectedException == null) {
            // expecting a value
            Object result = MapUtilities.getOrThrow(map, key, throwable);
            assertEquals(expectedResultOrException, result, description);
        } else {
            // expecting an exception
            Throwable thrown = assertThrows(expectedException, () -> MapUtilities.getOrThrow(map, key, throwable));
            assertEquals(((Throwable) expectedResultOrException).getMessage(), thrown.getMessage(), description);
        }
    }

    // ---------- Group: getOrThrow - empty map ----------
    @ParameterizedTest(name = "[{index}] empty map – {0}")
    @MethodSource("emptyMapProvider")
    void testEmptyMap(String description, Map<?, ?> map, Object key, Throwable throwable, Class<? extends Throwable> expectedException, String expectedMessage) {
        Throwable thrown = assertThrows(expectedException, () -> MapUtilities.getOrThrow(map, key, throwable));
        assertEquals(expectedMessage, thrown.getMessage(), description);
    }

    // ---------- Group: getOrThrow - immutable map implementation ----------
    @ParameterizedTest(name = "[{index}] immutable map – {0}")
    @MethodSource("immutableMapProvider")
    void testImmutableMap(String description, Map<?, ?> map, Object key, Throwable throwable, Object expectedResultOrException, Class<? extends Throwable> expectedException) {
        if (expectedException == null) {
            Object result = MapUtilities.getOrThrow(map, key, throwable);
            assertEquals(expectedResultOrException, result, description);
        } else {
            Throwable thrown = assertThrows(expectedException, () -> MapUtilities.getOrThrow(map, key, throwable));
            assertEquals(((Throwable) expectedResultOrException).getMessage(), thrown.getMessage(), description);
        }
    }

    // ---------- Group: getOrThrow - different map implementations ----------
    @ParameterizedTest(name = "[{index}] different map impl – {0}")
    @MethodSource("differentMapImplProvider")
    void testDifferentMapImplementations(String description, Map<?, ?> map, Object key, Throwable throwable, Object expectedResultOrException, Class<? extends Throwable> expectedException) {
        if (expectedException == null) {
            Object result = MapUtilities.getOrThrow(map, key, throwable);
            assertEquals(expectedResultOrException, result, description);
        } else {
            Throwable thrown = assertThrows(expectedException, () -> MapUtilities.getOrThrow(map, key, throwable));
            assertEquals(((Throwable) expectedResultOrException).getMessage(), thrown.getMessage(), description);
        }
    }

    // ---------- Group: getOrThrow - null key with non-nullable map implementations ----------
    @ParameterizedTest(name = "[{index}] null key non‑nullable map – {0}")
    @MethodSource("nullKeyNonNullableProvider")
    void testNullKeyNonNullableMaps(String description, Map<?, ?> map, Object key, Throwable throwable, Class<? extends Throwable> expectedException) {
        // The underlying map implementation throws NPE before we get a chance to use the supplied throwable.
        assertThrows(expectedException, () -> MapUtilities.getOrThrow(map, key, throwable));
    }

    // ---------- Group: getOrThrow - incompatible key type ----------
    @ParameterizedTest(name = "[{index}] incompatible key type – {0}")
    @MethodSource("incompatibleKeyTypeProvider")
    void testIncompatibleKeyType(String description, Map<?, ?> map, Object key, Throwable throwable, Class<? extends Throwable> expectedException) {
        // For TreeMap/EnumMap a wrong key type causes ClassCastException from containsKey.
        assertThrows(expectedException, () -> MapUtilities.getOrThrow(map, key, throwable));
    }
}
