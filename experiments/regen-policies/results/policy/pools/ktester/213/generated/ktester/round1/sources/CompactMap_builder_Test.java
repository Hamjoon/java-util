package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
class CompactMap_builder_Test {

    @Test
    void testBuilderReturnsBuilderInstance() {
        // The static builder() method must return a non‑null Builder object.
        CompactMap.Builder<String, Object> builder = CompactMap.builder();
        assertNotNull(builder, "builder() should not return null");
        // The returned object must be an instance of the inner Builder class.
        assertInstanceOf(CompactMap.Builder.class, builder, "builder() should return an instance of CompactMap.Builder");
    }

    @Test
    void testDefaultBuilderProducesMapWithDefaultConfig() {
        // Build a map using the default configuration (no method chaining).
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
        assertNotNull(map, "Built map must not be null");
        // The map should be empty initially.
        assertTrue(map.isEmpty(), "Newly built map should be empty");
        assertEquals(0, map.size(), "Size of a new map must be 0");
        // Verify that the configuration map reflects the defaults defined in CompactMap.
        Map<String, Object> cfg = map.getConfig();
        // Default values as documented in the API.
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE), "Default compactSize should match the constant");
        assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, cfg.get(CompactMap.CASE_SENSITIVE), "Default caseSensitive should match the constant");
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, cfg.get(CompactMap.MAP_TYPE), "Default mapType should match the constant");
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY), "Default singleKey should match the constant");
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING), "Default ordering should be 'unordered'");
    }

    @Test
    void testCustomBuilderConfigurationAndResultingMapBehavior() {
        // Prepare a source map with a few entries.
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        source.put("three", 3);
        // Build a CompactMap with a non‑default configuration.
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(// case‑insensitive keys
        false).// sorted ordering
        sortedOrder().compactSize(// larger compact threshold
        80).mapType(// explicit backing map type
        LinkedHashMap.class).singleValueKey(// custom single‑value key
        "uuid").sourceMap(// initialise with source entries
        source).build();
        assertNotNull(map, "Built map must not be null");
        // The map must contain all entries from the source map.
        assertEquals(source.size(), map.size(), "Map size should equal source size");
        source.forEach((k, v) -> assertEquals(v, map.get(k), "Value for key '" + k + "' must match source"));
        // Verify configuration values.
        Map<String, Object> cfg = map.getConfig();
        assertEquals(false, cfg.get(CompactMap.CASE_SENSITIVE), "caseSensitive should be false");
        assertEquals(80, cfg.get(CompactMap.COMPACT_SIZE), "compactSize should be 80");
        assertEquals(LinkedHashMap.class, cfg.get(CompactMap.MAP_TYPE), "mapType should be LinkedHashMap");
        assertEquals("uuid", cfg.get(CompactMap.SINGLE_KEY), "singleKey should be 'uuid'");
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING), "ordering should be 'sorted'");
        // Because ordering is sorted, the keySet iterator should return keys in natural order.
        List<String> expectedOrder = new ArrayList<>(source.keySet());
        Collections.sort(expectedOrder);
        List<String> actualOrder = new ArrayList<>();
        for (String key : map.keySet()) {
            actualOrder.add(key);
        }
        assertEquals(expectedOrder, actualOrder, "Keys should be iterated in sorted order when sortedOrder() is used");
        // Verify that case‑insensitivity works: different case of an existing key should resolve to the same entry.
        assertEquals(1, map.get("ONE"), "Case‑insensitive lookup should return the correct value");
        assertEquals(2, map.get("Two"), "Case‑insensitive lookup should return the correct value");
    }

    @Test
    void testBuilderReturnsDistinctInstances() {
        CompactMap.Builder<String, Object> b1 = CompactMap.builder();
        CompactMap.Builder<String, Object> b2 = CompactMap.builder();
        assertNotNull(b1);
        assertNotNull(b2);
        assertNotSame(b1, b2, "Each call to builder() must return a new Builder instance");
    }

    @Test
    void testBuilderWithCustomConfigViaWithConfig() {
        Map<String, Object> custom = new HashMap<>();
        custom.put(CompactMap.COMPACT_SIZE, 10);
        custom.put(CompactMap.CASE_SENSITIVE, false);
        custom.put(CompactMap.ORDERING, CompactMap.SORTED);
        custom.put(CompactMap.SINGLE_KEY, "uuid");
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().withConfig(custom).build();
        assertNotNull(map);
        assertTrue(map.isEmpty(), "Map should be empty after building with custom config");
        Map<String, Object> cfg = map.getConfig();
        assertEquals(10, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(false, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
        assertEquals("uuid", cfg.get(CompactMap.SINGLE_KEY));
    }

    @Test
    void testBuilderEmptyConfigProducesDefaults() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().withConfig(Collections.emptyMap()).build();
        Map<String, Object> cfg = map.getConfig();
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.DEFAULT_MAP_TYPE, cfg.get(CompactMap.MAP_TYPE));
        assertEquals(CompactMap.DEFAULT_SINGLE_KEY, cfg.get(CompactMap.SINGLE_KEY));
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
    }

    @Test
    void testBuilderNegativeCompactSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            CompactMap.<String, Object>builder().compactSize(-5).build();
        }, "Negative compactSize should cause IllegalArgumentException");
    }

    @Test
    void testBuilderZeroCompactSizeThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            CompactMap.<String, Object>builder().compactSize(0).build();
        }, "Zero compactSize should cause IllegalArgumentException");
    }

    @Test
    void testBuilderMapTypeTreeMapCreatesSortedMap() {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().mapType(TreeMap.class).build();
        // The builder should automatically switch ordering to sorted when a sorted map type is used.
        Map<String, Object> cfg = map.getConfig();
        assertEquals(TreeMap.class, cfg.get(CompactMap.MAP_TYPE));
        assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
    }

    @Test
    void testBuilderReverseOrderIteration() {
        // Build a map with reverse ordering and a known insertion order.
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        src.put("c", 3);
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().reverseOrder().sourceMap(src).build();
        // Collect keys from entrySet – they must appear in reverse insertion order.
        List<String> actual = new ArrayList<>();
        for (Map.Entry<String, Integer> e : map.entrySet()) {
            actual.add(e.getKey());
        }
        List<String> expected = Arrays.asList("c", "b", "a");
        assertEquals(expected, actual, "entrySet iteration should follow reverse insertion order");
    }

    @Test
    void testBuilderCaseInsensitiveLookup() {
        CompactMap<String, String> map = CompactMap.<String, String>builder().caseSensitive(false).build();
        map.put("Key", "value");
        assertEquals("value", map.get("key"), "Lookup should be case‑insensitive");
        assertEquals("value", map.get("KEY"), "Lookup should be case‑insensitive");
    }

    @Test
    void testBuilderSingleKeyConfiguration() throws Exception {
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().singleValueKey("uid").build();
        // Use reflection to invoke the protected getSingleValueKey() method.
        Method m = CompactMap.class.getDeclaredMethod("getSingleValueKey");
        m.setAccessible(true);
        Object singleKey = m.invoke(map);
        assertEquals("uid", singleKey, "getSingleValueKey() should return the configured key");
        assertEquals("uid", map.getConfig().get(CompactMap.SINGLE_KEY));
    }

    @Test
    void testBuilderUnknownOptionThrows() {
        Map<String, Object> bad = new HashMap<>();
        bad.put("unknownOption", "value");
        assertThrows(IllegalArgumentException.class, () -> {
            CompactMap.<String, Object>builder().withConfig(bad).build();
        }, "Providing an unknown configuration key must throw IllegalArgumentException");
    }

    @Test
    void testBuilderNullConfigThrows() {
        assertThrows(NullPointerException.class, () -> {
            CompactMap.<String, Object>builder().withConfig(null).build();
        }, "withConfig(null) should throw NullPointerException");
    }
}
