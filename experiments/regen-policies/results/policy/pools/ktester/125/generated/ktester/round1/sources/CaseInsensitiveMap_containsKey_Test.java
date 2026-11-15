package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_containsKey_Test {

    private CaseInsensitiveMap<String, Object> stringKeyMap;

    private CaseInsensitiveMap<Object, Object> mixedKeyMap;

    @BeforeEach
    void setupBeforeEach() {
        // Map that uses String keys – the focus of the case‑insensitive logic.
        stringKeyMap = new CaseInsensitiveMap<>();
        stringKeyMap.put("Alpha", "A");
        stringKeyMap.put("Beta", "B");
        stringKeyMap.put("Gamma", "C");
        // Map that mixes String and non‑String keys to verify the fallback path.
        mixedKeyMap = new CaseInsensitiveMap<>();
        mixedKeyMap.put(42, "Answer");
        mixedKeyMap.put("Delta", "D");
        mixedKeyMap.put(null, "nullValue");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the maps to avoid cross‑test contamination.
        stringKeyMap.clear();
        mixedKeyMap.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset any static configuration that other tests might rely on.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /**
     * Provides normal (non‑exception) test data for {@code containsKey}.
     */
    private static Stream<Arguments> provideBranchAndCaseCoverage() {
        // Map is pre‑populated in @BeforeEach:
        //  stringKeyMap: "Alpha","Beta","Gamma"
        //  mixedKeyMap: 42 -> "Answer", "Delta" -> "D", null -> "nullValue"
        return Stream.of(// String key – exact case
        Arguments.of(stringKeyMap, "Alpha", true, "Exact case match"), // String key – different case
        Arguments.of(stringKeyMap, "alpha", true, "Case‑insensitive match (lowercase)"), Arguments.of(stringKeyMap, "ALPHA", true, "Case‑insensitive match (uppercase)"), // Another key, mixed case
        Arguments.of(stringKeyMap, "bEtA", true, "Case‑insensitive match (mixed case)"), // Non‑existent string key
        Arguments.of(stringKeyMap, "Omega", false, "String not present"), // Non‑String key present
        Arguments.of(mixedKeyMap, 42, true, "Integer key present"), // Non‑String key absent
        Arguments.of(mixedKeyMap, 24, false, "Integer key absent"), // Null key present (mixedKeyMap contains a null entry)
        Arguments.of(mixedKeyMap, null, true, "Null key present"), // Null key absent (stringKeyMap has no null entry)
        Arguments.of(stringKeyMap, null, false, "Null key absent"));
    }

    @Test
    void containsKey_StringKey_IgnoresCase() {
        // Exact case
        assertTrue(stringKeyMap.containsKey("Alpha"));
        // Different capitalisation
        assertTrue(stringKeyMap.containsKey("alpha"));
        assertTrue(stringKeyMap.containsKey("ALPHA"));
        assertTrue(stringKeyMap.containsKey("AlPhA"));
        // Another key, different case
        assertTrue(stringKeyMap.containsKey("bEtA"));
        // Non‑existent key
        assertFalse(stringKeyMap.containsKey("Omega"));
    }

    @Test
    void containsKey_StringKey_WithNull() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put(null, "nullValue");
        assertTrue(map.containsKey(null));
        assertFalse(map.containsKey("null"));
    }

    @Test
    void containsKey_NonStringKey_DelegatesToBackingMap() {
        // Integer key
        assertTrue(mixedKeyMap.containsKey(42));
        assertFalse(mixedKeyMap.containsKey(24));
        // Object key that is not a String
        Object customKey = new Object();
        mixedKeyMap.put(customKey, "custom");
        assertTrue(mixedKeyMap.containsKey(customKey));
        assertFalse(mixedKeyMap.containsKey(new Object()));
    }

    @Test
    void containsKey_NonStringKeyWhenKeyIsStringInMap() {
        // The map contains a String key "Delta". Supplying a non‑String object
        // that equals the same textual value should NOT be considered present.
        assertTrue(mixedKeyMap.containsKey("Delta"));
        assertFalse(mixedKeyMap.containsKey(new StringBuilder("Delta")));
    }

    @Test
    void containsKey_NullKeyOnMapWithoutNullEntry() {
        // Map does not contain a null key – should return false.
        assertFalse(stringKeyMap.containsKey(null));
    }

    @Test
    void containsKey_EmptyMapAlwaysFalseExceptNullWhenPresent() {
        CaseInsensitiveMap<String, String> empty = new CaseInsensitiveMap<>();
        // Empty map – any non‑null key returns false
        assertFalse(empty.containsKey("any"));
        // Empty map – null key returns false
        assertFalse(empty.containsKey(null));
        // Add a null entry and verify the behaviour changes only for null
        empty.put(null, "value");
        assertTrue(empty.containsKey(null));
        assertFalse(empty.containsKey("any"));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideBranchAndCaseCoverage")
    void containsKey_BranchAndCaseCoverage(CaseInsensitiveMap<?, ?> map, Object key, boolean expected, String description) {
        assertEquals(expected, map.containsKey(key), description);
    }

    @Test
    void containsKey_EmptyStringKey() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("", "empty");
        // empty string is case‑insensitive by definition
        assertTrue(map.containsKey(""));
        // whitespace is different
        assertFalse(map.containsKey(" "));
    }

    @Test
    void containsKey_StringBuilderKey_NotTreatedAsString() {
        // Store a String key
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("TeSt", "value");
        // Supplying a StringBuilder should bypass the CI branch and be looked up directly
        StringBuilder sb = new StringBuilder("TeSt");
        assertFalse(map.containsKey(sb), "StringBuilder is not a String – should not be found");
    }

    @Test
    void containsKey_StringBuilderKey_ExactInstance() {
        // Store a StringBuilder as a key (non‑String)
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
        StringBuilder builder = new StringBuilder("builder");
        map.put(builder, "value");
        // The exact same instance must be found
        assertTrue(map.containsKey(builder));
        // A different builder with same content must not be found
        assertFalse(map.containsKey(new StringBuilder("builder")));
    }

    @Test
    void containsKey_KeyWithNullValue() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        // value is null, but key exists
        map.put("test", null);
        assertTrue(map.containsKey("test"));
        // also verify case‑insensitivity
        assertTrue(map.containsKey("TEST"));
    }

    @Test
    void containsKey_DoesNotModifyMapSize() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("test", "value");
        int sizeBefore = map.size();
        assertTrue(map.containsKey("test"));
        assertEquals(sizeBefore, map.size(), "containsKey must not change map size");
    }

    /**
     * Helper that injects a custom backing map into a freshly constructed {@link CaseInsensitiveMap}
     * via reflection. This is necessary because the public API does not expose a constructor that
     * directly accepts a backing map.
     */
    @SuppressWarnings("unchecked")
    private static <K, V> CaseInsensitiveMap<K, V> mapWithBacking(Map<K, V> backing) throws Exception {
        CaseInsensitiveMap<K, V> ciMap = new CaseInsensitiveMap<>();
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(ciMap, backing);
        return ciMap;
    }

    @Test
    void containsKey_NonStringKeyOnTreeMap_ThrowsClassCastException() throws Exception {
        // TreeMap with natural ordering only knows how to compare Strings.
        TreeMap<String, String> treeBacking = new TreeMap<>();
        treeBacking.put("a", "value");
        CaseInsensitiveMap<String, String> ciMap = mapWithBacking(treeBacking);
        // The TreeMap will try to compare the Integer key with existing String keys,
        // resulting in a ClassCastException.
        assertThrows(ClassCastException.class, () -> ciMap.containsKey(1));
    }

    @Test
    void containsKey_NullKeyOnConcurrentSkipListMap_ThrowsNullPointerException() throws Exception {
        // ConcurrentSkipListMap does not permit null keys.
        ConcurrentSkipListMap<String, String> skipListBacking = new ConcurrentSkipListMap<>();
        skipListBacking.put("present", "value");
        CaseInsensitiveMap<String, String> ciMap = mapWithBacking(skipListBacking);
        assertThrows(NullPointerException.class, () -> ciMap.containsKey(null));
    }

    @Test
    void containsKey_OnMapWithNullBacking_ThrowsNullPointerException() throws Exception {
        // Create a map and then force its internal 'map' field to null.
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        // simulate construction with a null backing map
        mapField.set(ciMap, null);
        assertThrows(NullPointerException.class, () -> ciMap.containsKey("any"));
    }
}
