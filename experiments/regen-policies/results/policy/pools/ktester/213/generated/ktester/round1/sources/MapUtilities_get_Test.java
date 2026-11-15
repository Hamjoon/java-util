package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_get_Test {

    /**
     * A map that will be (re)initialised before each test.
     */
    private Map<Object, Object> map;

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple HashMap for most tests; a TreeMap is used where ordering is relevant.
        map = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help the garbage collector and ensure no test leaks state.
        map = null;
    }

    private static Stream<Arguments> standardCases() {
        return Stream.of(// Key exists, non‑null value
        Arguments.of("Key exists in the map with a non‑null value; the method should return that value, ignoring the default.", mapOf("a", 1), "a", 0, 1), // Missing key
        Arguments.of("Key is absent; map.get returns null, so the method should return the supplied default value.", mapOf("a", 1), "b", 0, 0), // Present key but null value → default
        Arguments.of("Key is present but its associated value is null; the method must fall back to the default.", mapOf("a", null), "a", 5, 5), // Both stored value and default are null
        Arguments.of("Both the stored value and the default are null; the method should return null.", mapOf("a", null), "a", null, null), // Empty map
        Arguments.of("Empty map; any key lookup yields null, so the default is returned.", Collections.emptyMap(), "any", "default", "default"));
    }

    private static Stream<Arguments> nullKeyHandling() {
        return Stream.of(// Map contains a null key with a non‑null value
        Arguments.of("Map contains a null key with a non‑null value; passing null as the lookup key should return the stored value, not the default.", mapOf(null, 42), null, 0, 42), // Map does not contain a null key
        Arguments.of("Map does not contain a null key; a null lookup should behave like a missing key and return the supplied default.", mapOf("a", 1), null, -1, -1));
    }

    private static Stream<Arguments> keyTypeMismatch() {
        // Map<Integer,String> but we query with a String key → map.get returns null
        Map<Integer, String> inner = new HashMap<>();
        inner.put(1, "one");
        // raw view
        Map<Object, Object> map = new HashMap<>(inner);
        return Stream.of(Arguments.of("Map expects Integer keys but a String key is supplied; map.get returns null, so the default value must be returned.", map, "1", "default", "default"));
    }

    private static Stream<Arguments> unmodifiableMap() {
        Map<String, String> mutable = new HashMap<>();
        mutable.put("x", "y");
        Map<Object, Object> unmod = Collections.unmodifiableMap(mutable);
        return Stream.of(// existing key
        Arguments.of("An unmodifiable map still supports read operations; existing key should return its value.", unmod, "x", "def", "y"), // missing key
        Arguments.of("Lookup of a missing key in an unmodifiable map must return the supplied default.", unmod, "missing", "fallback", "fallback"));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// null map reference
        Arguments.of("Passing a null map reference should cause a NullPointerException when the method attempts map.get(key).", null, "any", 0, NullPointerException.class), // TreeMap does not allow null keys
        Arguments.of("TreeMap does not allow null keys; calling get(null) throws NullPointerException, which should propagate.", new TreeMap<String, Integer>(), null, 5, NullPointerException.class));
    }

    private static Stream<Arguments> additionalExceptionCases() {
        // ConcurrentHashMap null key
        Map<String, Integer> chm = new ConcurrentHashMap<>();
        chm.put("a", 1);
        // Hashtable null key
        Map<String, String> ht = new Hashtable<>();
        ht.put("x", "y");
        // TreeMap<Integer,String> with String key → ClassCastException
        Map<Integer, String> tm = new TreeMap<>();
        tm.put(1, "one");
        // EnumMap<DayOfWeek,Integer> with String key → ClassCastException
        EnumMap<DayOfWeek, Integer> em = new EnumMap<>(DayOfWeek.class);
        em.put(DayOfWeek.MONDAY, 10);
        return Stream.of(Arguments.of("ConcurrentHashMap does not permit null keys; calling get(null) throws NullPointerException which should propagate.", chm, null, 0, NullPointerException.class), Arguments.of("Hashtable rejects null keys; get(null) throws NullPointerException.", ht, null, "fallback", NullPointerException.class), Arguments.of("TreeMap expects keys that are Comparable of the map's key type; passing a String when the map expects Integer causes a ClassCastException.", tm, "a", "default", ClassCastException.class), Arguments.of("EnumMap requires keys of the specific enum type; providing a String key results in a ClassCastException.", em, "MONDAY", 0, ClassCastException.class));
    }

    @Test
    void getReturnsDefaultWhenKeyMissing() {
        assertEquals("default", MapUtilities.get(map, "missing", "default"));
        assertEquals(42, (int) MapUtilities.get(map, "missingInt", 42));
        assertNull(MapUtilities.get(map, "missingObj", null));
    }

    @Test
    void getReturnsStoredValueWhenPresent() {
        map.put("stringKey", "value");
        map.put("intKey", 7L);
        map.put("boolKey", Boolean.TRUE);
        assertEquals("value", MapUtilities.get(map, "stringKey", "default"));
        assertEquals(7L, MapUtilities.get(map, "intKey", 0L));
        assertEquals(Boolean.TRUE, MapUtilities.get(map, "boolKey", Boolean.FALSE));
    }

    @Test
    void getHandlesAutoBoxingCorrectly() {
        // default supplied as primitive, stored value as wrapper and vice‑versa
        map.put("autoBool", true);
        map.put("autoLong", 99L);
        // default is wrapper, return should be primitive auto‑unboxed
        assertTrue((Boolean) MapUtilities.get(map, "autoBool", Boolean.FALSE));
        // default is primitive, return should be wrapper
        assertEquals(99L, (long) MapUtilities.get(map, "autoLong", 0L));
    }

    @Test
    void getReturnsNullWhenValueIsExplicitlyNull() {
        map.put("nullKey", null);
        assertNull(MapUtilities.get(map, "nullKey", "default"));
    }

    @Test
    void getDoesNotPerformImplicitCasting() {
        map.put("foo", Boolean.TRUE);
        // The generic signature allows any T, but a wrong cast will cause a
        // ClassCastException at the call site – this is the expected behaviour.
        assertThrows(ClassCastException.class, () -> {
            // The compiler permits the cast; the runtime will fail.
            String s = (String) MapUtilities.get(map, "foo", null);
        });
    }

    @Test
    void getWorksWithSortedMapImplementation() {
        // Use a TreeMap to prove that the method works with any Map implementation.
        Map<Object, Object> sortedMap = new TreeMap<>();
        sortedMap.put("a", "alpha");
        assertEquals("alpha", MapUtilities.get(sortedMap, "a", "default"));
        assertEquals("missing", MapUtilities.get(sortedMap, "missing", "missing"));
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("standardCases")
    void standardCases(String description, Map<Object, Object> testMap, Object key, Object def, Object expected) {
        assertEquals(expected, MapUtilities.get(testMap, key, def));
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullKeyHandling")
    void nullKeyHandling(String description, Map<Object, Object> testMap, Object key, Object def, Object expected) {
        assertEquals(expected, MapUtilities.get(testMap, key, def));
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("keyTypeMismatch")
    void keyTypeMismatch(String description, Map<Object, Object> testMap, Object key, Object def, Object expected) {
        assertEquals(expected, MapUtilities.get(testMap, key, def));
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unmodifiableMap")
    void unmodifiableMap(String description, Map<Object, Object> testMap, Object key, Object def, Object expected) {
        assertEquals(expected, MapUtilities.get(testMap, key, def));
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionCases")
    void exceptionCases(String description, Map<Object, Object> testMap, Object key, Object def, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapUtilities.get(testMap, key, def));
    }

    // ----------------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalExceptionCases")
    void additionalExceptionCases(String description, Map<Object, Object> testMap, Object key, Object def, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapUtilities.get(testMap, key, def));
    }

    /**
     * Simple var‑args helper to build a {@link HashMap} from alternating key/value objects.
     * The method is deliberately generic (raw types) because the tests treat the map as
     * {@code Map<Object,Object>}.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Object> mapOf(Object... keyValues) {
        Map<Object, Object> m = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put(keyValues[i], keyValues[i + 1]);
        }
        return m;
    }
}
