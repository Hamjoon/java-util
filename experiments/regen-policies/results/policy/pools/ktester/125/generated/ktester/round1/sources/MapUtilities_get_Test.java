package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_get_Test {

    /**
     * The map used in the majority of the tests.
     */
    private Map<Object, Object> map;

    @BeforeEach
    void setupBeforeEach() {
        // A fresh map for each test guarantees isolation.
        map = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the map – not strictly required but makes the intent clear.
        map.clear();
        map = null;
    }

    /**
     * Provides arguments for the parameterized {@link #getParameterizedTests}.
     *
     * <p>Each {@link Arguments} instance contains:
     * <ul>
     *   <li>a pre‑populated {@link Map}</li>
     *   <li>the lookup key</li>
     *   <li>the default value</li>
     *   <li>the expected result (or expected exception class)</li>
     *   <li>a textual description for readability</li>
     * </ul>
     * </p>
     */
    private static Stream<Arguments> getTestCases() {
        return Stream.of(// -----------------------------------------------------------------
        // Value present in map
        // -----------------------------------------------------------------
        Arguments.of(mapOf("apple", 10, "banana", 20), "banana", 0, 20, "The map contains the key \"banana\" with a non‑null value 20, so the method should return that value instead of the default."), // -----------------------------------------------------------------
        // Key absent returns default
        // -----------------------------------------------------------------
        Arguments.of(mapOf("apple", 10, "banana", 20), "orange", 0, 0, "The map does not contain the requested key, so the method should return the supplied default value."), // -----------------------------------------------------------------
        // Key present with null value returns default
        // -----------------------------------------------------------------
        Arguments.of(mapOf("apple", null, "banana", 20), "apple", 5, 5, "The key exists but its associated value is null; the method must fall back to the default value."), // -----------------------------------------------------------------
        // Empty map returns default
        // -----------------------------------------------------------------
        Arguments.of(Collections.emptyMap(), "anyKey", 42, 42, "When the map is empty, any lookup should yield the default value."), // -----------------------------------------------------------------
        // Default is null returns null when key missing
        // -----------------------------------------------------------------
        Arguments.of(mapOf("x", 1), "y", null, null, "If the default argument itself is null, the method should return null when the key is not present."), // -----------------------------------------------------------------
        // Null map throws NullPointerException (handled separately)
        // -----------------------------------------------------------------
        // -----------------------------------------------------------------
        // Null key lookup with null entry in map
        // -----------------------------------------------------------------
        Arguments.of(mapOf(null, 99, "a", 1), null, 0, 99, "The map contains a null key; looking up null should return its associated value, not the default."), // -----------------------------------------------------------------
        // Generic List values are returned correctly
        // -----------------------------------------------------------------
        Arguments.of(mapOf("list", Arrays.asList(1, 2, 3)), "list", Collections.emptyList(), Arrays.asList(1, 2, 3), "Ensures that the method works with non‑scalar generic types (e.g., List<Integer>) and returns the stored list."), // -----------------------------------------------------------------
        // Immutable map missing key returns default
        // -----------------------------------------------------------------
        Arguments.of(Collections.unmodifiableMap(Map.of("a", 10)), "b", 5, 5, "Even when the map is immutable, a missing key should cause the method to return the default value."), // -----------------------------------------------------------------
        // ConcurrentHashMap null key throws NullPointerException (handled separately)
        // -----------------------------------------------------------------
        // The two cases that expect exceptions are exercised in dedicated tests below.
        // They are not represented here because {@code Arguments} expects a concrete
        // result value, not an exception class.
        // -----------------------------------------------------------------
        // End of data
        // -----------------------------------------------------------------
        // placeholder to keep the stream non‑empty; will be filtered out.
        Arguments.empty()).filter(// remove the placeholder
        arg -> arg.get() != null);
    }

    @Test
    void getReturnsDefaultWhenKeyIsMissing() {
        // given – an empty map and a default value
        String defaultValue = "default";
        // when
        String result = MapUtilities.get(map, "missingKey", defaultValue);
        // then
        assertEquals(defaultValue, result, "When the key is absent, the supplied default should be returned.");
    }

    @Test
    void getReturnsStoredValueWhenKeyIsPresent() {
        // given
        map.put("existingKey", 42L);
        Long defaultValue = 7L;
        // when
        Long result = MapUtilities.get(map, "existingKey", defaultValue);
        // then
        assertEquals(42L, result, "When the key exists, its associated value should be returned, not the default.");
    }

    @Test
    void getHandlesNullMapGracefully() {
        // given
        Map<?, ?> nullMap = null;
        String defaultValue = "fallback";
        // when
        String result = MapUtilities.get(nullMap, "anyKey", defaultValue);
        // then
        assertEquals(defaultValue, result, "A null map should behave like an empty map and return the default.");
    }

    @Test
    void getWorksWithAutoBoxingAndUnboxing() {
        // given – store a primitive boolean as its wrapper type
        map.put("boolKey", Boolean.TRUE);
        // default is the primitive `false`
        boolean defaultBool = false;
        // when
        boolean result = MapUtilities.get(map, "boolKey", defaultBool);
        // then
        assertTrue(result, "Auto‑unboxing should convert Boolean.TRUE to primitive true.");
        // given – default is a primitive int, stored value is a Long
        map.put("intKey", 5L);
        int defaultInt = 9;
        // when
        int intResult = MapUtilities.get(map, "intKey", defaultInt);
        // then
        assertEquals(5, intResult, "The Long value should be auto‑unboxed/cast to int when possible.");
    }

    @Test
    void getDoesNotThrowWhenDefaultIsNull() {
        // given
        map.put("key", "value");
        // when
        String result = MapUtilities.get(map, "key", null);
        // then
        assertEquals("value", result, "When a value exists, a null default must be ignored and the stored value returned.");
    }

    @Test
    void getThrowsClassCastExceptionWhenReturnedValueCannotBeCast() {
        // given – map stores a Boolean but we request a String
        map.put("mixedKey", Boolean.TRUE);
        // when / then
        assertThrows(ClassCastException.class, () -> {
            // The generic type is erased at runtime, so the cast is performed by the caller.
            @SuppressWarnings("unchecked")
            String ignored = (String) MapUtilities.get(map, "mixedKey", null);
        }, "A ClassCastException should be propagated when the caller casts to an incompatible type.");
    }

    /**
     * The original scaffold expected a graceful fallback for a {@code null} map,
     * but {@code MapUtilities.get(...)} dereferences the map without a null‑check,
     * therefore a {@link NullPointerException} is the correct behaviour.
     */
    @Test
    void getThrowsNullPointerExceptionWhenMapIsNull() {
        // given
        Map<?, ?> nullMap = null;
        String defaultValue = "fallback";
        // when / then
        assertThrows(NullPointerException.class, () -> MapUtilities.get(nullMap, "anyKey", defaultValue), "A null map should cause a NullPointerException because the method does not guard against it.");
    }

    /**
     * Parameterized test that iterates over the JSON‑derived scenarios.
     *
     * @param testMap   the map to query
     * @param key       the lookup key
     * @param def       the default value
     * @param expected  the expected return value
     * @param descr     description of the scenario (used only for readability)
     */
    @ParameterizedTest(name = "{4}")
    @MethodSource("getTestCases")
    void getParameterizedTests(Map<Object, Object> testMap, Object key, Object def, Object expected, String descr) {
        // when
        Object result = MapUtilities.get(testMap, key, def);
        // then
        assertEquals(expected, result, () -> "Failed scenario: " + descr);
    }

    /**
     * Specific test for the two JSON cases that expect an exception:
     * <ul>
     *   <li>Null map throws {@link NullPointerException}</li>
     *   <li>{@link ConcurrentHashMap} with a {@code null} key throws {@link NullPointerException}</li>
     * </ul>
     */
    @Test
    void getThrowsWhenMapIsNull() {
        // given
        Map<?, ?> nullMap = null;
        String defaultValue = "def";
        // when / then
        assertThrows(NullPointerException.class, () -> MapUtilities.get(nullMap, "any", defaultValue), "Calling get with a null map should result in a NullPointerException.");
    }

    @Test
    void getThrowsWhenConcurrentHashMapKeyIsNull() {
        // given
        Map<Object, Object> concurrentMap = new ConcurrentHashMap<>();
        concurrentMap.put("present", 1);
        String defaultValue = "def";
        // when / then
        assertThrows(NullPointerException.class, () -> MapUtilities.get(concurrentMap, null, defaultValue), "ConcurrentHashMap does not permit null keys; a null key should cause NullPointerException.");
    }

    /**
     * Creates a mutable {@link HashMap} from a var‑args list of key/value pairs.
     *
     * @param keyValues alternating keys and values
     * @return a populated {@link HashMap}
     */
    @SafeVarargs
    private static Map<Object, Object> mapOf(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Even number of arguments required (key/value pairs).");
        }
        Map<Object, Object> m = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            m.put(keyValues[i], keyValues[i + 1]);
        }
        return m;
    }
}
