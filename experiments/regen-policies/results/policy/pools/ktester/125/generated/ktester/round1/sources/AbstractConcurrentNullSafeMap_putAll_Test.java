package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_putAll_Test {

    private TestMap map;

    private TestMapInt mapInt;

    /**
     * Simple concrete implementation for {@code String → String} maps.
     */
    private static final class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Simple concrete implementation for {@code String → Integer} maps.
     */
    @SuppressWarnings("unused")
    private static final class TestMapInt extends AbstractConcurrentNullSafeMap<String, Integer> {

        TestMapInt() {
            super(new ConcurrentHashMap<>());
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestMap();
        mapInt = new TestMapInt();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the maps to avoid cross‑test interference.
        map.clear();
        mapInt.clear();
    }

    private static Stream<Arguments> basicPutAllProvider() {
        // 1. empty map
        Map<String, Integer> empty = Collections.emptyMap();
        // 2. single non‑null entry
        Map<String, Integer> single = Map.of("a", 1);
        // 3. null key
        Map<String, Integer> nullKey = new HashMap<>();
        nullKey.put(null, 2);
        // 4. null value
        Map<String, Integer> nullValue = new HashMap<>();
        nullValue.put("b", null);
        // 5. mixed entries (LinkedHashMap to preserve order)
        Map<String, Integer> mixed = new LinkedHashMap<>();
        mixed.put(null, null);
        mixed.put("c", 3);
        mixed.put("d", null);
        // 6. duplicate key – later entry should win
        Map<String, Integer> duplicate = new LinkedHashMap<>();
        duplicate.put("e", 5);
        // overwrite
        duplicate.put("e", 6);
        return Stream.of(Arguments.of("Putting an empty map should be a no‑op", empty, 0, Collections.emptyMap()), Arguments.of("Single non‑null key/value pair", single, 1, Map.of("a", 1)), Arguments.of("Null key with non‑null value", nullKey, 1, Map.of(null, 2)), Arguments.of("Non‑null key with null value", nullValue, 1, Map.of("b", null)), Arguments.of("Mixed null and non‑null keys/values", mixed, 3, Map.of(null, null, "c", 3, "d", null)), Arguments.of("Duplicate keys – last value wins", duplicate, 1, Map.of("e", 6)));
    }

    private static Stream<Arguments> overwritePutAllProvider() {
        // Scenario 1: existing non‑null key is overwritten
        Map<String, Integer> pre1 = Map.of("x", 10);
        Map<String, Integer> src1 = Map.of("x", 20, "y", 30);
        Map<String, Integer> expected1 = Map.of("x", 20, "y", 30);
        // Scenario 2: existing null key is overwritten
        Map<String, Integer> pre2 = new HashMap<>();
        pre2.put(null, 5);
        Map<String, Integer> src2 = new HashMap<>();
        src2.put(null, 7);
        Map<String, Integer> expected2 = Map.of(null, 7);
        return Stream.of(Arguments.of("Overwrite existing non‑null key and add new key", pre1, src1, 2, expected1), Arguments.of("Overwrite existing null key", pre2, src2, 1, expected2));
    }

    @Test
    void putAllCopiesAllEntriesIncludingNulls() {
        // GIVEN: a source map containing a mix of normal and null keys/values
        Map<String, String> source = new HashMap<>();
        source.put("a", "alpha");
        source.put("b", "beta");
        source.put(null, "nullKey");
        source.put("c", null);
        // overwrites previous null key entry
        source.put(null, null);
        // WHEN: putAll is invoked
        map.putAll(source);
        // THEN: all entries must be present and correctly unmasked
        assertEquals(3, map.size(), "Map should contain three distinct keys (null, a, b, c)");
        assertEquals("alpha", map.get("a"));
        assertEquals("beta", map.get("b"));
        assertNull(map.get("c"));
        // the last entry for null key stored null value
        assertNull(map.get(null));
    }

    @Test
    void putAllWithEmptyMapLeavesTargetUnchanged() {
        // GIVEN: an empty source map and a pre‑populated target map
        map.put("x", "xValue");
        map.put(null, "nullValue");
        Map<String, String> empty = new HashMap<>();
        // WHEN: putAll is called with an empty map
        map.putAll(empty);
        // THEN: target map should remain unchanged
        assertEquals(2, map.size());
        assertEquals("xValue", map.get("x"));
        assertEquals("nullValue", map.get(null));
    }

    @Test
    void putAllOverridesExistingEntries() {
        // GIVEN: target map already contains some keys
        map.put("key", "old");
        map.put(null, "oldNull");
        // AND: source map contains the same keys with new values (including null)
        Map<String, String> source = new HashMap<>();
        source.put("key", "new");
        source.put(null, null);
        // WHEN: putAll is executed
        map.putAll(source);
        // THEN: values are replaced by the source map's values
        assertEquals(2, map.size());
        assertEquals("new", map.get("key"));
        assertNull(map.get(null));
    }

    /**
     * Basic putAll scenarios covering empty maps, null keys/values,
     * mixed entries and duplicate keys.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicPutAllProvider")
    void putAllBasicScenarios(String description, Map<String, Integer> source, int expectedSize, Map<String, Integer> expectedContent) {
        // WHEN
        mapInt.putAll(source);
        // THEN
        assertEquals(expectedSize, mapInt.size(), "Unexpected map size");
        expectedContent.forEach((k, v) -> assertEquals(v, mapInt.get(k), () -> "Unexpected value for key [" + k + "]"));
    }

    /**
     * Verifies that passing {@code null} to {@code putAll} throws
     * {@link NullPointerException}, matching the contract of {@link Map#putAll}.
     */
    @Test
    void putAllRejectsNullArgument() {
        assertThrows(NullPointerException.class, () -> map.putAll(null), "putAll(null) should throw NullPointerException");
    }

    /**
     * Overwrite existing entries – both non‑null and null keys.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("overwritePutAllProvider")
    void putAllOverwriteExisting(String description, Map<String, Integer> prepopulate, Map<String, Integer> source, int expectedSize, Map<String, Integer> expectedContent) {
        // GIVEN: a map already containing some entries
        prepopulate.forEach(mapInt::put);
        // WHEN
        mapInt.putAll(source);
        // THEN
        assertEquals(expectedSize, mapInt.size(), "Unexpected size after overwrite");
        expectedContent.forEach((k, v) -> assertEquals(v, mapInt.get(k), () -> "Unexpected value for key [" + k + "] after overwrite"));
    }
}
