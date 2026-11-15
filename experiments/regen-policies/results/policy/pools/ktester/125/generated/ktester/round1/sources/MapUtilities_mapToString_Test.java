package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapToString_Test {

    /**
     * Provides arguments for the {@code testEmptyMap} parameterized test.
     */
    static Stream<Arguments> emptyMapProvider() {
        return Stream.of(Arguments.of(new LinkedHashMap<>(), "{}"));
    }

    static Stream<Arguments> singleEntryProvider() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "b");
        return Stream.of(Arguments.of(map, "{a=b}"));
    }

    static Stream<Arguments> multipleOrderedProvider() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("x", 1);
        map.put("y", 2);
        return Stream.of(Arguments.of(map, "{x=1, y=2}"));
    }

    static Stream<Arguments> nullKeyValueProvider() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(null, "val");
        map.put("key", null);
        return Stream.of(Arguments.of(map, "{null=val, key=null}"));
    }

    static Stream<Arguments> selfReferentialProvider() {
        Map<Object, Object> map = selfReferentialMap();
        return Stream.of(Arguments.of(map, "{(this Map)=value, self=(this Map)}"));
    }

    static Stream<Arguments> sortedTreeMapProvider() {
        Map<String, Integer> map = new TreeMap<>();
        map.put("b", 2);
        map.put("a", 1);
        return Stream.of(Arguments.of(map, "{a=1, b=2}"));
    }

    static Stream<Arguments> enumMapProvider() {
        EnumMap<Color, String> map = new EnumMap<>(Color.class);
        map.put(Color.RED, "R");
        map.put(Color.GREEN, "G");
        return Stream.of(Arguments.of(map, "{RED=R, GREEN=G}"));
    }

    /**
     * Constructs a map that contains a self‑reference both as a key and as a value.
     * The insertion order matches the expected string in the JSON test case.
     *
     * @return a {@link LinkedHashMap} with two entries:
     *         1) the map itself as key, mapped to the string "value"
     *         2) the string "self" as key, mapped to the map itself
     */
    private static Map<Object, Object> selfReferentialMap() {
        Map<Object, Object> map = new LinkedHashMap<>();
        // first entry: key is the map itself
        map.put(map, "value");
        // second entry: value is the map itself
        map.put("self", map);
        return map;
    }

    @Test
    void testEmptyMapReturnsBraces() {
        Map<String, Integer> empty = new HashMap<>();
        assertEquals("{}", MapUtilities.mapToString(empty));
    }

    @Test
    void testSingleEntryFormatting() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("one", 1);
        assertEquals("{one=1}", MapUtilities.mapToString(map));
    }

    @Test
    void testMultipleEntriesFormatting() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "alpha");
        map.put("b", "beta");
        map.put("c", "gamma");
        // LinkedHashMap preserves insertion order, which MapUtilities respects
        assertEquals("{a=alpha, b=beta, c=gamma}", MapUtilities.mapToString(map));
    }

    @Test
    void testSelfReferenceAsKey() {
        Map<Object, String> map = new IdentityHashMap<>();
        map.put(map, "value");
        // key is the map itself, should be rendered as "(this Map)"
        assertEquals("{(this Map)=value}", MapUtilities.mapToString(map));
    }

    @Test
    void testSelfReferenceAsValue() {
        Map<String, Object> map = new IdentityHashMap<>();
        map.put("key", map);
        // value is the map itself, should be rendered as "(this Map)"
        assertEquals("{key=(this Map)}", MapUtilities.mapToString(map));
    }

    @Test
    void testSelfReferenceBothKeyAndValue() {
        Map<Object, Object> map = new IdentityHashMap<>();
        map.put(map, map);
        assertEquals("{(this Map)=(this Map)}", MapUtilities.mapToString(map));
    }

    @Test
    void testMixedNullAndNonNullValues() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("nullKey", null);
        map.put(null, "nullValue");
        // Nulls are printed as the literal "null"
        assertEquals("{nullKey=null, null=nullValue}", MapUtilities.mapToString(map));
    }

    @ParameterizedTest(name = "[{index}] Empty map should return \"{}\"")
    @MethodSource("emptyMapProvider")
    void testEmptyMap(Map<?, ?> map, String expected) {
        assertEquals(expected, MapUtilities.mapToString(map));
    }

    @ParameterizedTest(name = "[{index}] Single entry map")
    @MethodSource("singleEntryProvider")
    void testSingleEntryMap(Map<?, ?> map, String expected) {
        assertEquals(expected, MapUtilities.mapToString(map));
    }

    @ParameterizedTest(name = "[{index}] Multiple entries ordered map")
    @MethodSource("multipleOrderedProvider")
    void testMultipleEntriesOrderedMap(Map<?, ?> map, String expected) {
        assertEquals(expected, MapUtilities.mapToString(map));
    }

    @ParameterizedTest(name = "[{index}] Map with null key and value")
    @MethodSource("nullKeyValueProvider")
    void testMapWithNullKeyAndValue(Map<?, ?> map, String expected) {
        assertEquals(expected, MapUtilities.mapToString(map));
    }

    @ParameterizedTest(name = "[{index}] Self‑referential map")
    @MethodSource("selfReferentialProvider")
    void testSelfReferentialMap(Map<?, ?> map, String expected) {
        assertEquals(expected, MapUtilities.mapToString(map));
    }

    @ParameterizedTest(name = "[{index}] Sorted TreeMap")
    @MethodSource("sortedTreeMapProvider")
    void testSortedTreeMap(Map<?, ?> map, String expected) {
        assertEquals(expected, MapUtilities.mapToString(map));
    }

    @ParameterizedTest(name = "[{index}] EnumMap handling")
    @MethodSource("enumMapProvider")
    void testEnumMapHandling(Map<?, ?> map, String expected) {
        assertEquals(expected, MapUtilities.mapToString(map));
    }

    @Test
    void testNullMapArgumentThrowsNPE() {
        // The method should throw a NullPointerException when the map argument is null.
        assertThrows(NullPointerException.class, () -> MapUtilities.mapToString(null));
    }
}
