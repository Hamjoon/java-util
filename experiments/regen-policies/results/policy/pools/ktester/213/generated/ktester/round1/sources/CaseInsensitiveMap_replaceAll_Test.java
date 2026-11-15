package com.cedarsoftware.util;

import java.lang.reflect.Method;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CaseInsensitiveMap_replaceAll_Test {

    /**
     * The map instance used by the tests.
     */
    private CaseInsensitiveMap<String, String> stringMap;

    /**
     * A map that uses a non‑String key – used to verify that replaceAll works unchanged for those keys.
     */
    private CaseInsensitiveMap<Integer, String> intKeyMap;

    //     @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test so that state does not leak.
        stringMap = new CaseInsensitiveMap<>();
        stringMap.put("One", "Two");
        stringMap.put("Three", "Four");
        stringMap.put("Five", "Six");
        intKeyMap = new CaseInsensitiveMap<>();
        intKeyMap.put(1, "One");
        intKeyMap.put(2, "Two");
    }

    //     @AfterEach
    void teardownAfterEach() {
        // Reset the static cache to the default state – this mirrors the behaviour of the original test suite.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    /**
     * Determines whether the given method returns {@code void}.
     *
     * @param method the method to inspect; may be {@code null}
     * @return {@code true} if the method is non‑null and its return type is {@code void};
     *         {@code false} otherwise
     */
    public static boolean returnsVoid(Method method) {
        return method != null && void.class.equals(method.getReturnType());
    }

    @Test
    void replaceAll_updatesValuesAndPreservesCaseInsensitiveLookup() {
        // GIVEN – a BiFunction that builds a new value from the original key and the old value
        BiFunction<String, String, String> updater = (k, v) -> k.toUpperCase() + ":" + v;
        // WHEN
        stringMap.replaceAll(updater);
        // THEN – values are updated according to the function
        assertEquals("ONE:Two", stringMap.get("One"));
        assertEquals("ONE:Two", stringMap.get("one"));
        assertEquals("THREE:Four", stringMap.get("THREE"));
        assertEquals("FIVE:Six", stringMap.get("fIvE"));
    }

    @Test
    void replaceAll_passesOriginalKey_notWrappedInstance() {
        // GIVEN – a BiFunction that records the class of the key it receives
        final Class<?>[] keyClass = new Class<?>[1];
        BiFunction<String, String, String> recorder = (k, v) -> {
            // should be java.lang.String
            keyClass[0] = k.getClass();
            return v + "-changed";
        };
        // WHEN
        stringMap.replaceAll(recorder);
        // THEN – the key seen by the function is a plain String, never the internal wrapper
        assertEquals(String.class, keyClass[0], "replaceAll should unwrap CaseInsensitiveString before invoking the supplied function");
        // also verify that the map values were indeed changed
        assertEquals("Two-changed", stringMap.get("ONE"));
        assertEquals("Four-changed", stringMap.get("three"));
    }

    @Test
    void replaceAll_worksForNonStringKeys() {
        // GIVEN – a map with Integer keys and a simple updater that appends the key to the value
        BiFunction<Integer, String, String> updater = (k, v) -> v + "-" + k;
        // WHEN
        intKeyMap.replaceAll(updater);
        // THEN – each entry is updated correctly
        assertEquals("One-1", intKeyMap.get(1));
        assertEquals("Two-2", intKeyMap.get(2));
    }

    @Test
    void replaceAll_nullFunction_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> stringMap.replaceAll(null));
    }

    @Test
    void replaceAll_unwrapsCaseInsensitiveStringKey_andAddsTen() {
        // GIVEN – map with a single entry using a String key
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("Key", 1);
        BiFunction<String, Integer, Integer> addTen = (k, v) -> v + 10;
        // WHEN
        map.replaceAll(addTen);
        // THEN – value is updated and the function received the original key string
        assertEquals(11, map.get("Key"));
        assertEquals(11, map.get("key"));
    }

    @Test
    void replaceAll_nonStringKeyIsForwardedUnchanged() {
        // GIVEN – map with Integer key
        Map<Integer, String> source = new HashMap<>();
        source.put(42, "value");
        CaseInsensitiveMap<Integer, String> map = new CaseInsensitiveMap<>(source);
        BiFunction<Integer, String, String> concat = (k, v) -> k + "-" + v;
        // WHEN
        map.replaceAll(concat);
        // THEN
        assertEquals("42-value", map.get(42));
    }

    @Test
    void replaceAll_allowsFunctionToReturnNullValues() {
        // GIVEN – map with two entries
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("A", "alpha");
        map.put("B", "beta");
        BiFunction<String, String, String> alwaysNull = (k, v) -> null;
        // WHEN
        map.replaceAll(alwaysNull);
        // THEN – map now contains null values for both keys
        assertNull(map.get("A"));
        assertNull(map.get("B"));
        assertEquals(2, map.size());
    }

    @Test
    void replaceAll_propagatesExceptionAndLeavesMapUnchanged() {
        // GIVEN – map with a single entry
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("X", 5);
        BiFunction<String, Integer, Integer> explode = (k, v) -> {
            throw new IllegalStateException("boom");
        };
        // WHEN / THEN – exception propagates and map stays as before
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.replaceAll(explode));
        assertEquals("boom", ex.getMessage());
        assertEquals(5, map.get("X"));
        assertEquals(1, map.size());
    }

    @Test
    void replaceAll_duplicateCaseInsensitiveKeys_collapseAndUpdateValue() {
        // GIVEN – map where two case‑insensitive keys collide
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("a", 1);
        // replaces the previous entry
        map.put("A", 2);
        BiFunction<String, Integer, Integer> doubleValue = (k, v) -> v * 2;
        // WHEN
        map.replaceAll(doubleValue);
        // THEN – only one entry remains, key retains the case of the first insertion ("a")
        assertEquals(4, map.get("a"));
        assertEquals(4, map.get("A"));
        assertEquals(1, map.size());
    }

    @Test
    void replaceAll_originalKeyCasePreservedInFunctionArgument() {
        // GIVEN – map with mixed‑case key
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("MiXeD", "value");
        BiFunction<String, String, String> upperKey = (k, v) -> k.toUpperCase() + ":" + v;
        // WHEN
        map.replaceAll(upperKey);
        // THEN – function received the original mixed‑case key and produced expected value
        assertEquals("MIXED:value", map.get("mixed"));
        // Verify that the stored key still reports its original case via entrySet
        Map.Entry<String, String> entry = map.entrySet().iterator().next();
        assertEquals("MiXeD", entry.getKey());
    }

    @Test
    void replaceAll_handlesNullExistingValueGracefully() {
        // GIVEN – map with a null value
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("nullKey", null);
        BiFunction<String, Integer, Integer> addFive = (k, v) -> (v == null ? 0 : v) + 5;
        // WHEN
        map.replaceAll(addFive);
        // THEN – null is treated as 0 and replaced with 5
        assertEquals(5, map.get("nullKey"));
    }

    @Test
    void replaceAll_storesNullWhenFunctionReturnsNull() {
        // GIVEN – map with a non‑null value
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("Key", 10);
        BiFunction<String, Integer, Integer> returnNull = (k, v) -> null;
        // WHEN
        map.replaceAll(returnNull);
        // THEN – the entry now holds a null value (Map.replaceAll does not remove the entry)
        assertNull(map.get("Key"));
        assertEquals(1, map.size());
    }

    @Test
    void replaceAll_concurrentHashMap_nullValueNotAllowed_throwsNullPointerException() {
        // GIVEN – a CaseInsensitiveMap backed by a ConcurrentHashMap
        Map<String, Integer> backing = new ConcurrentHashMap<>();
        backing.put("Key", 1);
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>(backing);
        BiFunction<String, Integer, Integer> returnNull = (k, v) -> null;
        // WHEN / THEN – NullPointerException is propagated from the underlying map
        assertThrows(NullPointerException.class, () -> map.replaceAll(returnNull));
        // Verify original entry is still present (the exception occurs before any mutation)
        assertEquals(1, map.get("Key"));
    }

    @Test
    void replaceAll_incompatibleReturnType_throwsClassCastException() {
        // GIVEN – map expecting Integer values
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
        map.put("Key", 5);
        // Function returns a String, which cannot be stored as Integer.
        @SuppressWarnings({ "unchecked", "rawtypes" })
        BiFunction badReturn = (BiFunction<String, Integer, String>) (k, v) -> k + "-" + v;
        // WHEN / THEN – ClassCastException is thrown by the underlying map
        assertThrows(ClassCastException.class, () -> map.replaceAll(badReturn));
        // Ensure original value is unchanged
        assertEquals(5, map.get("Key"));
    }

    @Test
    void replaceAll_unmodifiableMap_throwsUnsupportedOperationException() {
        // GIVEN – an unmodifiable backing map
        Map<String, Integer> source = new HashMap<>();
        source.put("Key", 2);
        Map<String, Integer> unmodifiable = Collections.unmodifiableMap(source);
        CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>(unmodifiable);
        BiFunction<String, Integer, Integer> inc = (k, v) -> v + 1;
        // WHEN / THEN – operation is not allowed
        assertThrows(UnsupportedOperationException.class, () -> map.replaceAll(inc));
        // Original entry remains unchanged
        assertEquals(2, map.get("Key"));
    }

    @Test
    void replaceAll_keyUnwrapCastMismatch_throwsClassCastException() {
        // GIVEN – a map with generic type <Integer, String> but the source map contains a CaseInsensitiveString key
        @SuppressWarnings("unchecked")
        Map raw = new HashMap<>();
        raw.put(CaseInsensitiveMap.CaseInsensitiveString.of("10"), "value");
        CaseInsensitiveMap<Integer, String> map = new CaseInsensitiveMap<>(raw);
        BiFunction<Integer, String, String> upper = (k, v) -> v.toUpperCase();
        // WHEN / THEN – the internal cast from String to Integer fails
        assertThrows(ClassCastException.class, () -> map.replaceAll(upper));
        // Verify that the original entry is still present (the exception occurs before mutation)
        assertEquals("value", map.get(10));
    }
}
