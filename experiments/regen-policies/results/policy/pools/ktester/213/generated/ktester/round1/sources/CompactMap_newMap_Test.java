package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static com.cedarsoftware.util.CompactMap.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_newMap_Test {

    /**
     * Mocks {@link ReflectionUtils#isJavaCompilerAvailable()} to return {@code true}
     * for the duration of the supplied {@code executable}.
     */
    private static void withCompilerAvailable(Runnable executable) {
        try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class)) {
            mocked.when(ReflectionUtils::isJavaCompilerAvailable).thenReturn(true);
            executable.run();
        }
    }

    private static Stream<Arguments> provideSimpleSuccessCases() {
        // 1) default map, empty
        Map<String, Object> optEmpty = new HashMap<>();
        // 2) source map supplied
        Map<String, Object> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        Map<String, Object> optSource = new HashMap<>();
        optSource.put(SOURCE_MAP, source);
        // 3) sorted ordering with explicit entries
        Map<String, Object> optSorted = new HashMap<>();
        optSorted.put(ORDERING, SORTED);
        Map<String, Integer> expectedSorted = Map.of("a", 1, "b", 2, "c", 3);
        List<String> sortedOrder = List.of("a", "b", "c");
        // 4) reverse ordering
        Map<String, Object> optReverse = new HashMap<>();
        optReverse.put(ORDERING, REVERSE);
        List<String> reverseOrder = List.of("c", "b", "a");
        // 5) insertion ordering
        Map<String, Object> optInsertion = new HashMap<>();
        optInsertion.put(ORDERING, INSERTION);
        List<String> insertionOrder = List.of("a", "b", "c");
        return Stream.of(Arguments.of(optEmpty, Collections.emptyMap(), null), Arguments.of(optSource, Map.of("a", 1, "b", 2), null), Arguments.of(optSorted, expectedSorted, sortedOrder), Arguments.of(optReverse, expectedSorted, reverseOrder), Arguments.of(optInsertion, expectedSorted, insertionOrder));
    }

    @Test
    void newMap_throwsWhenJavaCompilerIsUnavailable() {
        // Arrange – force ReflectionUtils.isJavaCompilerAvailable() to return false.
        try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class)) {
            mocked.when(ReflectionUtils::isJavaCompilerAvailable).thenReturn(false);
            Map<String, Object> options = new HashMap<>();
            // Act & Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
            String expectedMessagePart = "CompactMap dynamic subclassing requires the Java Compiler";
            assertTrue(ex.getMessage().contains(expectedMessagePart), "Exception message should mention the missing Java compiler");
        }
    }

    @Test
    void newMap_createsCompactMapWithSourceEntries_whenCompilerIsAvailable() {
        // Arrange – ensure the compiler check passes and provide a source map.
        try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class)) {
            mocked.when(ReflectionUtils::isJavaCompilerAvailable).thenReturn(true);
            Map<String, Object> source = new HashMap<>();
            source.put("alpha", 1);
            source.put("beta", 2);
            Map<String, Object> options = new HashMap<>();
            // initialise with source map
            options.put(SOURCE_MAP, source);
            // Use default map type (HashMap) and default configuration – no need to set them explicitly.
            // Act
            CompactMap<String, Integer> result = CompactMap.newMap(options);
            // Assert – result is a CompactMap and contains the source entries.
            assertNotNull(result, "newMap should never return null when the compiler is available");
            assertInstanceOf(CompactMap.class, result, "Result should be a CompactMap instance");
            assertEquals(2, result.size(), "Resulting map should contain exactly the source entries");
            assertEquals(1, result.get("alpha"));
            assertEquals(2, result.get("beta"));
        }
    }

    @Test
    void newMap_returnsEmptyCompactMapWhenNoSourceProvided() {
        // Arrange – compiler is available but no SOURCE_MAP entry.
        try (MockedStatic<ReflectionUtils> mocked = mockStatic(ReflectionUtils.class)) {
            mocked.when(ReflectionUtils::isJavaCompilerAvailable).thenReturn(true);
            Map<String, Object> options = new HashMap<>();
            // Act
            CompactMap<String, String> map = CompactMap.newMap(options);
            // Assert
            assertNotNull(map);
            assertTrue(map.isEmpty(), "Map should be empty when no source map is supplied");
            assertEquals(0, map.size());
        }
    }

    @Test
    void newMap_templateInstantiationFailure_invalidMapTypeClass() {
        // MAP_TYPE set to a class that does not implement Map (String.class)
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // invalid map type
            options.put(MAP_TYPE, String.class);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options), "Expected IllegalStateException when MAP_TYPE is not a Map implementation");
            assertTrue(ex.getMessage().contains("Failed to create map instance"), "Exception message should indicate failure to create map instance");
        });
    }

    @Test
    void newMap_orderingSorted_keyIterationOrderIsNatural() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // natural order
            options.put(ORDERING, SORTED);
            CompactMap<String, Integer> map = CompactMap.newMap(options);
            map.put("c", 3);
            map.put("a", 1);
            map.put("b", 2);
            List<String> keys = new ArrayList<>(map.keySet());
            assertEquals(List.of("a", "b", "c"), keys, "Keys should be iterated in natural sorted order");
        });
    }

    @Test
    void newMap_orderingReverse_keyIterationOrderIsDescending() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // reverse order
            options.put(ORDERING, REVERSE);
            CompactMap<String, Integer> map = CompactMap.newMap(options);
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 3);
            List<String> keys = new ArrayList<>(map.keySet());
            assertEquals(List.of("c", "b", "a"), keys, "Keys should be iterated in descending natural order");
        });
    }

    @Test
    void newMap_orderingInsertion_preservesInsertionOrder() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // insertion order
            options.put(ORDERING, INSERTION);
            CompactMap<String, Integer> map = CompactMap.newMap(options);
            map.put("a", 1);
            map.put("c", 3);
            map.put("b", 2);
            List<String> keys = new ArrayList<>(map.keySet());
            assertEquals(List.of("a", "c", "b"), keys, "Keys should be iterated in the order they were inserted");
        });
    }

    @Test
    void newMap_mapTypeTreeMap_impliesSortedOrdering() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // explicit TreeMap
            options.put(MAP_TYPE, TreeMap.class);
            CompactMap<String, Integer> map = CompactMap.newMap(options);
            map.put("c", 3);
            map.put("a", 1);
            map.put("b", 2);
            List<String> keys = new ArrayList<>(map.keySet());
            assertEquals(List.of("a", "b", "c"), keys, "TreeMap backing should yield natural sorted iteration order");
        });
    }

    @Test
    void newMap_mapTypeConcurrentHashMap_storesAllEntries() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            options.put(MAP_TYPE, ConcurrentHashMap.class);
            CompactMap<String, Integer> map = CompactMap.newMap(options);
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 3);
            assertEquals(3, map.size(), "All entries should be stored");
            assertTrue(map.containsKey("a"));
            assertTrue(map.containsKey("b"));
            assertTrue(map.containsKey("c"));
        });
    }

    @Test
    void newMap_caseInsensitive_lookupIgnoresCase() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // case‑insensitive
            options.put(CASE_SENSITIVE, false);
            CompactMap<String, String> map = CompactMap.newMap(options);
            map.put("Key", "value");
            // lookup with different case
            assertEquals("value", map.get("key"), "Value stored under 'Key' must be retrievable with lower‑case 'key'");
        });
    }

    @Test
    void newMap_customCompactSizeAndSingleKey_createsEmptyMap() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            options.put(COMPACT_SIZE, 10);
            options.put(SINGLE_KEY, "uid");
            CompactMap<String, Integer> map = CompactMap.newMap(options);
            assertTrue(map.isEmpty(), "Map should start empty even with custom compactSize/singleKey");
            assertEquals(0, map.size());
        });
    }

    @Test
    void newMap_invalidMapType_nonMapClass_throwsIllegalStateException() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // not a Map
            options.put(MAP_TYPE, String.class);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
            assertTrue(ex.getMessage().contains("Failed to create map instance"));
        });
    }

    @Test
    void newMap_invalidMapType_EnumMapWithoutKey_throwsIllegalStateException() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // EnumMap needs a key type
            options.put(MAP_TYPE, EnumMap.class);
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CompactMap.newMap(options));
            assertTrue(ex.getMessage().contains("Failed to create map instance"));
        });
    }

    @Test
    void newMap_caseInsensitiveMapWithInnerHashMapAndSortedOrdering() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            options.put(MAP_TYPE, CaseInsensitiveMap.class);
            // inner map type key expected by generator
            options.put("innerMapType", HashMap.class);
            options.put(ORDERING, SORTED);
            CompactMap<String, Integer> map = CompactMap.newMap(options);
            map.put("b", 2);
            map.put("A", 1);
            map.put("c", 3);
            List<String> keys = new ArrayList<>(map.keySet());
            // case‑insensitive natural order: "A", "b", "c"
            assertEquals(List.of("A", "b", "c"), keys);
        });
    }

    @Test
    void newMap_nullOptions_throwsNullPointerException() {
        withCompilerAvailable(() -> {
            assertThrows(NullPointerException.class, () -> CompactMap.newMap(null), "Passing null options should raise NPE");
        });
    }

    @Test
    void newMap_invalidOption_mapTypeNotClass_throwsClassCastException() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // invalid type
            options.put(MAP_TYPE, 123);
            ClassCastException ex = assertThrows(ClassCastException.class, () -> CompactMap.newMap(options));
            assertTrue(ex.getMessage().contains("java.lang.Integer cannot be cast"));
        });
    }

    @Test
    void newMap_invalidOption_compactSizeNotInteger_throwsClassCastException() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // invalid type
            options.put(COMPACT_SIZE, "big");
            ClassCastException ex = assertThrows(ClassCastException.class, () -> CompactMap.newMap(options));
            assertTrue(ex.getMessage().contains("java.lang.String cannot be cast"));
        });
    }

    @Test
    void newMap_invalidOption_caseSensitiveNotBoolean_throwsClassCastException() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // invalid type
            options.put(CASE_SENSITIVE, "yes");
            ClassCastException ex = assertThrows(ClassCastException.class, () -> CompactMap.newMap(options));
            assertTrue(ex.getMessage().contains("java.lang.String cannot be cast"));
        });
    }

    @Test
    void newMap_invalidOption_orderingNotString_throwsClassCastException() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // invalid type
            options.put(ORDERING, true);
            ClassCastException ex = assertThrows(ClassCastException.class, () -> CompactMap.newMap(options));
            assertTrue(ex.getMessage().contains("java.lang.Boolean cannot be cast"));
        });
    }

    @Test
    void newMap_sourceMapNotAMap_throwsClassCastException() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            // invalid type
            options.put(SOURCE_MAP, "not a map");
            ClassCastException ex = assertThrows(ClassCastException.class, () -> CompactMap.newMap(options));
            assertTrue(ex.getMessage().contains("java.lang.String cannot be cast"));
        });
    }

    @Test
    void newMap_invalidInnerMapTypeForCaseInsensitiveMap_throwsClassCastException() {
        withCompilerAvailable(() -> {
            Map<String, Object> options = new HashMap<>();
            options.put(MAP_TYPE, CaseInsensitiveMap.class);
            // not a Class
            options.put("innerMapType", 456);
            options.put(ORDERING, SORTED);
            ClassCastException ex = assertThrows(ClassCastException.class, () -> CompactMap.newMap(options));
            assertTrue(ex.getMessage().contains("java.lang.Integer cannot be cast"));
        });
    }

    @ParameterizedTest(name = "[{index}] options={0}")
    @MethodSource("provideSimpleSuccessCases")
    void newMap_simpleSuccessScenarios(Map<String, Object> options, Map<String, Integer> expectedEntries, List<String> expectedOrder) {
        withCompilerAvailable(() -> {
            CompactMap<String, Integer> map = CompactMap.newMap(options);
            assertEquals(expectedEntries.size(), map.size(), "Map size should match expected number of entries");
            // verify entries
            expectedEntries.forEach((k, v) -> assertEquals(v, map.get(k)));
            // if an order is supplied, verify iteration order
            if (expectedOrder != null) {
                List<String> actual = new ArrayList<>(map.keySet());
                assertEquals(expectedOrder, actual, "Key iteration order does not match expected order");
            }
        });
    }
}
