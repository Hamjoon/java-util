package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.awt.Point;
import java.util.TreeMap;
import java.util.EnumMap;

@ExtendWith(MockitoExtension.class)
class MapUtilities_mapToString_Test {

    @Test
    void testEmptyMap() {
        Map<String, String> empty = new HashMap<>();
        assertEquals("{}", MapUtilities.mapToString(empty));
    }

    @Test
    void testSimpleMapPreservesInsertionOrder() {
        // LinkedHashMap guarantees iteration order, making the expected string deterministic.
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals("{a=1, b=2, c=3}", MapUtilities.mapToString(map));
    }

    @Test
    void testMapWithSelfReferenceAsKey() {
        Map<Object, String> map = new HashMap<>();
        map.put(map, "value");
        assertEquals("{(this Map)=value}", MapUtilities.mapToString(map));
    }

    @Test
    void testMapWithSelfReferenceAsValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", map);
        assertEquals("{key=(this Map)}", MapUtilities.mapToString(map));
    }

    @Test
    void testMapWithSelfReferenceBothKeyAndValue() {
        Map<Object, Object> map = new HashMap<>();
        map.put(map, map);
        assertEquals("{(this Map)=(this Map)}", MapUtilities.mapToString(map));
    }

    @Test
    void testMapWithMixedTypes() {
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put("string", 123);
        map.put(456, true);
        map.put(null, "nullKey");
        assertEquals("{string=123, 456=true, null=nullKey}", MapUtilities.mapToString(map));
    }

    @Test
    void testSingleEntryMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertEquals("{a=1}", MapUtilities.mapToString(map));
    }

    @Test
    void testTreeMapSortedOrder() {
        // TreeMap sorts keys according to their natural ordering.
        Map<String, Integer> map = new TreeMap<>();
        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);
        assertEquals("{a=1, b=2, c=3}", MapUtilities.mapToString(map));
    }

    @Test
    void testEnumMapOrdering() {
        // EnumMap iterates in the order of enum constants.
        EnumMap<Color, String> map = new EnumMap<>(Color.class);
        map.put(Color.RED, "R");
        map.put(Color.BLUE, "B");
        assertEquals("{RED=R, BLUE=B}", MapUtilities.mapToString(map));
    }

    @Test
    void testNullKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put(null, 5);
        assertEquals("{null=5}", MapUtilities.mapToString(map));
    }

    @Test
    void testNullValue() {
        Map<String, Integer> map = new HashMap<>();
        map.put("k", null);
        assertEquals("{k=null}", MapUtilities.mapToString(map));
    }

    @Test
    void testNullKeyAndValue() {
        Map<String, String> map = new HashMap<>();
        map.put(null, null);
        assertEquals("{null=null}", MapUtilities.mapToString(map));
    }

    @Test
    void testCustomObjectToStringWithCommasAndBrackets() {
        Map<Integer, Point> map = new HashMap<>();
        map.put(1, new Point(2, 3));
        // Point's toString is "java.awt.Point[x=2,y=3]"
        assertEquals("{1=java.awt.Point[x=2,y=3]}", MapUtilities.mapToString(map));
    }
}
