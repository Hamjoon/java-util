package com.cedarsoftware.util;

import java.util.*;
import static com.cedarsoftware.util.CompactMap.*;
import java.lang.reflect.Method;
import java.util.stream.Stream;
// import org.junit.*;
import org.junit.jupiter.api.Test;
// import org.junit.runner.RunWith;
import org.junit.jupiter.api.function.Executable;
// import org.junit.runners.Parameterized;
import org.junit.jupiter.params.ParameterizedTest;
// import static org.junit.Assert.*;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;

class CompactMap_newMap_Test {

    //     @RunWith(Parameterized.class)
    public static class TreeMapOrderingVariants {

        //         @Parameterized.Parameters(name = "{index}: type={0}, ordering={1}")
        //         public static Collection<Object[]> data() {
        //             return Arrays.asList(new Object[][] { { TreeMap.class, SORTED, Arrays.asList("a", "b", "c") }, { TreeMap.class, REVERSE, Arrays.asList("c", "b", "a") } });
        //         }
        private final Class<?> mapType;

        private final String ordering;

        private final List<String> expectedOrder;

        public TreeMapOrderingVariants(Class<?> mapType, String ordering, List<String> expectedOrder) {
            this.mapType = mapType;
            this.ordering = ordering;
            this.expectedOrder = expectedOrder;
        }
        //         @Test
        //         public void testTreeMapOrderingVariant() {
        //             Map<String, Object> source = mapOf("c", 3, "a", 1, "b", 2);
        //             Map<String, Object> options = new HashMap<>();
        //             options.put(MAP_TYPE, mapType);
        //             options.put(ORDERING, ordering);
        //             options.put(SOURCE_MAP, source);
        //             CompactMap<String, Object> map = CompactMap.newMap(options);
        //             List<String> actual = new ArrayList<>(map.keySet());
        //             assertEquals(expectedOrder, actual);
        //         }
    }

    /**
     * A {@link Map} implementation with a private no‑argument constructor.
     * Used to provoke an {@link IllegalAccessException} when the template
     * class tries to instantiate it via {@code Class#newInstance()}.
     */
    private static class PrivateMap<K, V> extends HashMap<K, V> {

        private PrivateMap() {
            super();
        }
    }

    /**
     * Creates a fresh {@code HashMap} with the given entries.
     */
    private static Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return m;
    }

    private static Stream<Arguments> orderingProvider() {
        return Stream.of(Arguments.of(TreeMap.class, SORTED, Arrays.asList("a", "b", "c")), Arguments.of(TreeMap.class, REVERSE, Arrays.asList("c", "b", "a")));
    }

    /**
     * Returns the protected {@code getSingleValueKey()} via reflection.
     */
    @SuppressWarnings("unchecked")
    private static <K> K getSingleValueKey(CompactMap<K, ?> map) throws Exception {
        Method m = CompactMap.class.getDeclaredMethod("getSingleValueKey");
        m.setAccessible(true);
        return (K) m.invoke(map);
    }

    @Test
    void testNewMapWithSimpleOptionsAndSourceMap() {
        Map<String, Object> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        source.put("three", 3);
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, HashMap.class);
        options.put(COMPACT_SIZE, 2);
        options.put(CASE_SENSITIVE, true);
        options.put(SINGLE_KEY, "id");
        options.put(ORDERING, UNORDERED);
        options.put(SOURCE_MAP, source);
        CompactMap<String, Object> map = CompactMap.newMap(options);
        assertNotNull(map);
        assertEquals(source.size(), map.size());
        assertTrue(map.containsKey("one") && map.containsKey("two") && map.containsKey("three"));
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
        assertEquals(3, map.get("three"));
    }

    @Test
    void testNewMapSortedCaseInsensitiveOrdering() {
        Map<String, Object> source = new HashMap<>();
        source.put("bKey", "B");
        source.put("aKey", "A");
        source.put("CKey", "C");
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, TreeMap.class);
        options.put(ORDERING, SORTED);
        options.put(CASE_SENSITIVE, false);
        options.put(SINGLE_KEY, "id");
        options.put(SOURCE_MAP, source);
        CompactMap<String, Object> map = CompactMap.newMap(options);
        assertNotNull(map);
        // case‑insensitive look‑ups
        assertEquals("A", map.get("AKEY"));
        assertEquals("B", map.get("bkey"));
        assertEquals("C", map.get("ckey"));
        // iteration order must be case‑insensitive sorted
        List<String> keysInOrder = new ArrayList<>(map.keySet());
        List<String> expectedOrder = Arrays.asList("aKey", "bKey", "CKey");
        assertEquals(expectedOrder, keysInOrder);
    }

    @Test
    void testNewMapWithNullOptionsThrowsException() {
        Executable exec = () -> CompactMap.newMap(null);
        assertThrows(NullPointerException.class, exec);
    }

    @Test
    void testNewMapWithoutSourceMap_EmptyResult() {
        Map<String, Object> options = new HashMap<>();
        CompactMap<String, Object> map = CompactMap.newMap(options);
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
    }

    @Test
    void testNewMapWithSourceMap_DefaultHashMap() {
        Map<String, Object> source = mapOf("one", 1, "two", 2);
        Map<String, Object> options = new HashMap<>();
        options.put(SOURCE_MAP, source);
        CompactMap<String, Object> map = CompactMap.newMap(options);
        assertEquals(2, map.size());
        assertEquals(1, map.get("one"));
        assertEquals(2, map.get("two"));
    }

    @Test
    void testNewMapExplicitHashMapType() {
        Map<String, Object> source = mapOf("a", "A");
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, HashMap.class);
        options.put(SOURCE_MAP, source);
        CompactMap<String, Object> map = CompactMap.newMap(options);
        assertEquals(1, map.size());
        assertEquals("A", map.get("a"));
        assertTrue(map.keySet().contains("a"));
    }

    @Test
    void testNewMapTreeMapSortedOrdering() {
        Map<String, Object> source = mapOf("c", 3, "a", 1, "b", 2);
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, TreeMap.class);
        options.put(ORDERING, SORTED);
        options.put(SOURCE_MAP, source);
        CompactMap<String, Object> map = CompactMap.newMap(options);
        List<String> expected = Arrays.asList("a", "b", "c");
        List<String> actual = new ArrayList<>(map.keySet());
        assertEquals(expected, actual);
    }

    @Test
    void testNewMapTreeMapReverseOrdering() {
        Map<String, Object> source = mapOf("c", 3, "a", 1, "b", 2);
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, TreeMap.class);
        options.put(ORDERING, REVERSE);
        options.put(SOURCE_MAP, source);
        CompactMap<String, Object> map = CompactMap.newMap(options);
        List<String> expected = Arrays.asList("c", "b", "a");
        List<String> actual = new ArrayList<>(map.keySet());
        assertEquals(expected, actual);
    }

    @Test
    void testNewMapCaseInsensitiveMap() {
        Map<String, Object> source = mapOf("Key", "value");
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, CaseInsensitiveMap.class);
        options.put(CASE_SENSITIVE, false);
        options.put(SOURCE_MAP, source);
        CompactMap<String, Object> map = CompactMap.newMap(options);
        assertEquals("value", map.get("key"));
        assertEquals("value", map.get("KEY"));
        assertEquals(1, map.size());
    }

    @Test
    void testCaseInsensitiveMapWithInnerLinkedHashMap() {
        Map<String, Object> source = mapOf("First", 1, "second", 2);
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, CaseInsensitiveMap.class);
        // constant not public – use raw key name
        options.put("innerMapType", LinkedHashMap.class);
        options.put(CASE_SENSITIVE, false);
        options.put(SOURCE_MAP, source);
        CompactMap<String, Object> map = CompactMap.newMap(options);
        assertEquals(1, map.get("first"));
        assertEquals(2, map.get("SECOND"));
        List<String> expectedOrder = Arrays.asList("First", "second");
        List<String> actualOrder = new ArrayList<>(map.keySet());
        assertEquals(expectedOrder, actualOrder);
    }

    @Test
    void testNewMapCustomCompactSize() {
        Map<String, Object> source = mapOf("x", "X", "y", "Y", "z", "Z");
        Map<String, Object> options = new HashMap<>();
        options.put(COMPACT_SIZE, 5);
        options.put(SOURCE_MAP, source);
        CompactMap<String, Object> map = CompactMap.newMap(options);
        assertEquals(3, map.size());
        assertEquals("X", map.get("x"));
        assertEquals("Y", map.get("y"));
        assertEquals("Z", map.get("z"));
    }

    @Test
    void testNewMapSingleKeyOptimization() throws Exception {
        Map<String, Object> source = mapOf("id", 42);
        Map<String, Object> options = new HashMap<>();
        options.put(SINGLE_KEY, "id");
        options.put(SOURCE_MAP, source);
        CompactMap<String, Object> map = CompactMap.newMap(options);
        assertEquals(1, map.size());
        assertEquals(42, map.get("id"));
        assertEquals("id", getSingleValueKey(map));
    }

    @Test
    void testNewMapNonExistentMapTypeClassName() {
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, "com.example.DoesNotExist");
        Executable exec = () -> CompactMap.newMap(options);
        assertThrows(IllegalArgumentException.class, exec);
    }

    @Test
    void testNewMapMapInterfaceAsMapType() {
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, Map.class);
        Executable exec = () -> CompactMap.newMap(options);
        assertThrows(IllegalArgumentException.class, exec);
    }

    @Test
    void testNewMapSourceMapNotAMapThrowsClassCastException() {
        Map<String, Object> options = new HashMap<>();
        options.put(SOURCE_MAP, 12345);
        Executable exec = () -> CompactMap.newMap(options);
        assertThrows(ClassCastException.class, exec);
    }

    @Test
    void testNewMapTemplateClassWithPrivateConstructor() {
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, PrivateMap.class);
        options.put(SINGLE_KEY, "id");
        Executable exec = () -> CompactMap.newMap(options);
        // The implementation wraps reflective failures in IllegalStateException
        assertThrows(IllegalStateException.class, exec);
    }

    @ParameterizedTest
    @MethodSource("orderingProvider")
    void testTreeMapOrderingVariants(Class<?> mapType, String ordering, List<String> expectedOrder) {
        // GIVEN
        Map<String, Object> source = mapOf("c", 3, "a", 1, "b", 2);
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, mapType);
        options.put(ORDERING, ordering);
        options.put(SOURCE_MAP, source);
        // WHEN
        CompactMap<String, Object> map = CompactMap.newMap(options);
        // THEN
        List<String> actual = new ArrayList<>(map.keySet());
        assertEquals(expectedOrder, actual, "Ordering [" + ordering + "] must produce expected key sequence");
    }
}
