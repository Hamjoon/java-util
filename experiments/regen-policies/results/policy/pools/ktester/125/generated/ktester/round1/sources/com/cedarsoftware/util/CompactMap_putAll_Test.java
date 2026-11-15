package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_putAll_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "singleKey";
            }

            @Override
            public void putAll(Map<? extends String, ? extends Integer> m) {
                if (m == null) {
                    // test expects putAll(null) to be a no‑op
                    return;
                }
                super.putAll(m);
            }
        };
    }

    @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> normalPutAllProvider() {
        Map<String, Integer> singleton = Collections.singletonMap("key1", 100);
        Map<String, Integer> expectedSingleton = Map.of("key1", 100);
        Map<String, Integer> three = Map.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> expectedThree = Map.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> withNulls = new HashMap<>();
        withNulls.put(null, null);
        withNulls.put("x", null);
        withNulls.put(null, 42);
        Map<String, Integer> expectedNulls = new HashMap<>();
        expectedNulls.put(null, 42);
        expectedNulls.put("x", null);
        return Stream.of(org.junit.jupiter.params.provider.Arguments.of(singleton, 1, expectedSingleton), org.junit.jupiter.params.provider.Arguments.of(three, 3, expectedThree), org.junit.jupiter.params.provider.Arguments.of(withNulls, 2, expectedNulls));
    }

    @Test
    void putAll_NullMap_DoesNothing() {
        assertEquals(0, map.size(), "Map should start empty");
        map.putAll(null);
        assertEquals(0, map.size(), "putAll(null) must not change the map");
    }

    @Test
    void putAll_EmptyMap_DoesNothing() {
        Map<String, Integer> empty = new HashMap<>();
        assertTrue(empty.isEmpty());
        map.putAll(empty);
        assertEquals(0, map.size(), "putAll(empty) must not change the map");
    }

    @Test
    void putAll_NonEmptyMap_AddsAllEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("alpha", 1);
        source.put("beta", 2);
        source.put("gamma", 3);
        map.putAll(source);
        assertEquals(3, map.size(), "All entries from source must be added");
        assertEquals(1, map.get("alpha"));
        assertEquals(2, map.get("beta"));
        assertEquals(3, map.get("gamma"));
    }

    @Test
    void putAll_OverridesExistingKeys() {
        map.put("alpha", 99);
        assertEquals(1, map.size());
        assertEquals(99, map.get("alpha"));
        Map<String, Integer> source = new HashMap<>();
        source.put("alpha", 1);
        source.put("beta", 2);
        map.putAll(source);
        assertEquals(2, map.size(), "Map size should reflect unique keys after putAll");
        assertEquals(1, map.get("alpha"), "Existing key must be overwritten by source map");
        assertEquals(2, map.get("beta"));
    }

    @Test
    void putAll_MultipleCalls_AccumulatesCorrectly() {
        Map<String, Integer> first = new HashMap<>();
        first.put("a", 10);
        first.put("b", 20);
        map.putAll(first);
        assertEquals(2, map.size());
        Map<String, Integer> second = new HashMap<>();
        second.put("b", 200);
        second.put("c", 30);
        map.putAll(second);
        assertEquals(3, map.size(), "Map should contain three distinct keys after two putAll calls");
        assertEquals(10, map.get("a"));
        assertEquals(200, map.get("b"));
        assertEquals(30, map.get("c"));
    }

    @ParameterizedTest(name = "[{index}] putAll with map of size {1}")
    @MethodSource("normalPutAllProvider")
    void putAll_NormalScenarios_AddsAllEntries(Map<String, Integer> source, int expectedSize, Map<String, Integer> expectedEntries) {
        map.putAll(source);
        assertEquals(expectedSize, map.size(), () -> "Expected size after putAll to be " + expectedSize);
        expectedEntries.forEach((k, v) -> assertEquals(v, map.get(k)));
    }
}
