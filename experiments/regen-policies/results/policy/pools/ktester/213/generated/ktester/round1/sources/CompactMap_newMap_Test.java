package com.cedarsoftware.util;

import org.mockito.MockedStatic;
import java.time.DayOfWeek;
import static com.cedarsoftware.util.CompactMap.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@ExtendWith(MockitoExtension.class)
class CompactMap_newMap_Test {

    /* --------------------------------------------------------------------- */
    private static <T> T invokeProtected(Object target, String methodName, Class<T> returnType, Object... args) {
        try {
            java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            @SuppressWarnings("unchecked")
            T result = (T) method.invoke(target, args);
            return result;
        } catch (Exception e) {
            throw new AssertionError("Failed to invoke protected method " + methodName, e);
        }
    }

    /**
     * Verify that {@code newMap} creates a CompactMap that respects the supplied
     * configuration options (case‑insensitivity, sorted ordering and a specific
     * backing map type).  The test inserts keys in reverse order and checks that
     * iteration follows the expected sorted order.
     */
    @Test
    void testNewMapCreatesSortedCaseInsensitiveMap() {
        // -------------------------------------------------
        // GIVEN: a configuration map with case‑insensitive, sorted order,
        //        a TreeMap backing, and a small compact size.
        // -------------------------------------------------
        Map<String, Object> options = new HashMap<>();
        // case‑insensitive
        options.put(CASE_SENSITIVE, Boolean.FALSE);
        // sorted order
        options.put(ORDERING, SORTED);
        // explicit backing map
        options.put(MAP_TYPE, TreeMap.class);
        // small compact size to force map usage
        options.put(COMPACT_SIZE, 2);
        // -------------------------------------------------
        // WHEN: we create a CompactMap via the static factory.
        // -------------------------------------------------
        CompactMap<String, Integer> map = CompactMap.newMap(options);
        // -------------------------------------------------
        // THEN: the returned object is a CompactMap and behaves as a sorted,
        //       case‑insensitive map.
        // -------------------------------------------------
        assertNotNull(map, "newMap should not return null");
        assertTrue(map instanceof CompactMap, "Returned instance must be a CompactMap");
        // Insert keys in reverse alphabetical order.
        map.put("b", 2);
        map.put("A", 1);
        map.put("c", 3);
        // The keySet iterator should present keys in case‑insensitive sorted order: A, b, c
        List<String> iterationOrder = new ArrayList<>(map.keySet());
        assertEquals(List.of("A", "b", "c"), iterationOrder, "Keys should be iterated in case‑insensitive sorted order");
        // Verify that look‑ups are case‑insensitive.
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("B"));
        assertEquals(Integer.valueOf(3), map.get("C"));
    }

    /**
     * Verify that when a source map is supplied in the options, its entries are
     * copied into the newly created CompactMap.
     */
    @Test
    void testNewMapCopiesSourceMap() {
        // -------------------------------------------------
        // GIVEN: a source map with a few entries and an options map that
        //        includes the source under the {@code SOURCE_MAP} key.
        // -------------------------------------------------
        Map<String, Integer> source = new HashMap<>();
        source.put("one", 1);
        source.put("two", 2);
        source.put("three", 3);
        Map<String, Object> options = new HashMap<>();
        options.put(SOURCE_MAP, source);
        // Use defaults for everything else (case‑sensitive, HashMap backing, etc.)
        // -------------------------------------------------
        // WHEN: we create a CompactMap via the static factory.
        // -------------------------------------------------
        CompactMap<String, Integer> map = CompactMap.newMap(options);
        // -------------------------------------------------
        // THEN: the map contains all entries from the source map.
        // -------------------------------------------------
        assertNotNull(map);
        assertEquals(source.size(), map.size(), "Map size should match source map size");
        assertTrue(map.keySet().containsAll(source.keySet()), "All source keys must be present");
        for (Map.Entry<String, Integer> e : source.entrySet()) {
            assertEquals(e.getValue(), map.get(e.getKey()), "Value for key '" + e.getKey() + "' must be copied from source map");
        }
        // Modifying the original source map must not affect the CompactMap (defensive copy).
        source.put("four", 4);
        assertNull(map.get("four"), "CompactMap should not reflect changes made to the source map after creation");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testNewMapFailsWhenCompilerUnavailable() {
        try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class)) {
            mocked.when(ReflectionUtils::isJavaCompilerAvailable).thenReturn(false);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(Collections.emptyMap()));
            assertTrue(ex.getMessage().contains("CompactMap dynamic subclassing requires the Java Compiler"), "Exception message must mention compiler requirement");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testNewMapWithConcurrentHashMap() {
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, ConcurrentHashMap.class);
        CompactMap<String, Integer> map = CompactMap.newMap(options);
        assertNotNull(map);
        // Verify that the underlying map is a ConcurrentHashMap by causing a
        // concurrent modification exception if it were a normal HashMap.
        map.put("a", 1);
        map.put("b", 2);
        // No exception means we have a concurrent map (the test is just a sanity check).
        assertEquals(2, map.size());
    }

    @Test
    void testNewMapWithEnumMap() {
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, EnumMap.class);
        // auxiliary option required by EnumMap
        options.put("enumKeyClass", DayOfWeek.class);
        CompactMap<DayOfWeek, String> map = CompactMap.newMap(options);
        map.put(DayOfWeek.MONDAY, "first");
        map.put(DayOfWeek.FRIDAY, "last");
        assertEquals("first", map.get(DayOfWeek.MONDAY));
        assertEquals(2, map.size());
    }

    @Test
    void testNewMapWithInvalidMapTypeThrows() {
        Map<String, Object> options = new HashMap<>();
        // not a Map
        options.put(MAP_TYPE, String.class);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
        assertTrue(ex.getMessage().contains("mapType must be a Map class"), "Exception message must mention map type validation");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testSortedOrdering() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, SORTED);
        CompactMap<String, Integer> map = CompactMap.newMap(options);
        map.put("c", 3);
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(List.of("a", "b", "c"), new ArrayList<>(map.keySet()));
    }

    @Test
    void testReverseOrdering() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, REVERSE);
        CompactMap<String, Integer> map = CompactMap.newMap(options);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals(List.of("c", "b", "a"), new ArrayList<>(map.keySet()));
    }

    @Test
    void testInsertionOrdering() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, INSERTION);
        CompactMap<String, Integer> map = CompactMap.newMap(options);
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);
        assertEquals(List.of("x", "y", "z"), new ArrayList<>(map.keySet()));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testSingleKeyCustomization() {
        Map<String, Object> options = new HashMap<>();
        options.put(SINGLE_KEY, "customId");
        CompactMap<String, Integer> map = CompactMap.newMap(options);
        // Use reflection to call the protected getSingleValueKey()
        Object singleKey = invokeProtected(map, "getSingleValueKey", Object.class);
        assertEquals("customId", singleKey);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testCaseInsensitiveMapType() {
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, CaseInsensitiveMap.class);
        options.put(CASE_SENSITIVE, Boolean.FALSE);
        CompactMap<String, Integer> map = CompactMap.newMap(options);
        map.put("Key", 42);
        assertEquals(Integer.valueOf(42), map.get("key"));
        assertEquals(Integer.valueOf(42), map.get("KEY"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testOptionsIsolation() {
        Map<String, Object> mutable = new HashMap<>();
        mutable.put(COMPACT_SIZE, 20);
        CompactMap<String, Integer> map = CompactMap.newMap(mutable);
        // Change the original map after creation
        mutable.put(COMPACT_SIZE, 5);
        // The map should still report compactSize() == 20
        int compactSize = invokeProtected(map, "compactSize", int.class);
        assertEquals(20, compactSize);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testCompilationFailureBadSingleKey() {
        Map<String, Object> options = new HashMap<>();
        // unescaped quote breaks generated source
        options.put(SINGLE_KEY, "bad\"key");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
        assertTrue(ex.getMessage().contains("Compilation failed"), "Message must contain compilation failure hint");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testEnumMapWithoutKeyClassFailsAtRuntime() {
        Map<String, Object> options = new HashMap<>();
        // missing enumKeyClass
        options.put(MAP_TYPE, EnumMap.class);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
        assertTrue(ex.getMessage().contains("Failed to create map instance"));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }

    @Test
    void testCaseInsensitiveMapWithoutInnerMapFails() {
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, CaseInsensitiveMap.class);
        options.put(CASE_SENSITIVE, Boolean.FALSE);
        // No INNER_MAP_TYPE supplied – the generated code will NPE
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
        assertTrue(ex.getMessage().contains("Failed to create map instance"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testSourceMapNullKeySortedOrdering() {
        Map<String, Object> options = new HashMap<>();
        options.put(ORDERING, SORTED);
        Map<String, Integer> source = new HashMap<>();
        // null key
        source.put(null, 0);
        options.put(SOURCE_MAP, source);
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CompactMap.newMap(options));
        // The exception originates from TreeMap.put(null)
        assertEquals("null key", ex.getMessage());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testNegativeCapacityLeadsToFailure() {
        Map<String, Object> options = new HashMap<>();
        options.put(MAP_TYPE, ConcurrentHashMap.class);
        // compactSize+1 = -1 -> invalid capacity
        options.put(COMPACT_SIZE, -2);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
        assertTrue(ex.getMessage().contains("Failed to create map instance"));
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
    }
}
