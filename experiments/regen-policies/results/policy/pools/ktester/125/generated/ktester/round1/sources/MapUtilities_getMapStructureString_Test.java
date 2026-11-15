package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapUtilities_getMapStructureString_Test {

    /* --------------------------------------------------------------------- */
    private static String invokeGetMapStructureString(Map<?, ?> map) {
        try {
            java.lang.reflect.Method m = MapUtilities.class.getDeclaredMethod("getMapStructureString", Map.class);
            m.setAccessible(true);
            return (String) m.invoke(null, map);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* --------------------------------------------------------------------- */
    private static Stream<Arguments> nullAndSimpleMaps() {
        return Stream.of(Arguments.of(null, "null"), Arguments.of(new HashMap<>(), "HashMap"), Arguments.of(new TreeMap<>(), "TreeMap(NavigableMap)"), Arguments.of(new EnumMap<>(String.class), "EnumMap"), Arguments.of(new LinkedHashMap<>(), "LinkedHashMap"), Arguments.of(new ConcurrentSkipListMap<>(), "ConcurrentSkipListMap(NavigableMap)"));
    }

    private static Stream<Arguments> wrapperChains() {
        return Stream.of(// single wrappers
        Arguments.of(new CaseInsensitiveMap<>(new HashMap<>()), "CaseInsensitiveMap -> HashMap"), Arguments.of(new TrackingMap<>(new HashMap<>()), "TrackingMap -> HashMap"), // multiple wrappers
        Arguments.of(new CaseInsensitiveMap<>(new TrackingMap<>(new HashMap<>())), "CaseInsensitiveMap -> TrackingMap -> HashMap"), Arguments.of(new TrackingMap<>(new CaseInsensitiveMap<>(new TreeMap<>())), "TrackingMap -> CaseInsensitiveMap -> TreeMap(NavigableMap)"), // wrapper chain ending with NavigableMap
        Arguments.of(new TrackingMap<>(new CaseInsensitiveMap<>(new TreeMap<>())), "TrackingMap -> CaseInsensitiveMap -> TreeMap(NavigableMap)"));
    }

    private static Stream<Arguments> compactMapVariants() {
        return Stream.of(// MAP logical type with inner HashMap
        Arguments.of(new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.MAP, new HashMap<>()), "CompactMap(INSERTION) -> HashMap"), // LIST logical type – stops after reporting type
        Arguments.of(new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.LIST, null), "CompactMap(INSERTION) -> [LIST]"), // MAP logical type with inner NavigableMap
        Arguments.of(new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.MAP, new TreeMap<>()), "CompactMap(INSERTION) -> TreeMap(NavigableMap)"), // MAP logical type with inner wrapper
        Arguments.of(new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.MAP, new CaseInsensitiveMap<>(new HashMap<>())), "CompactMap(INSERTION) -> CaseInsensitiveMap -> HashMap"), // Nested CompactMaps
        Arguments.of(new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.MAP, new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.MAP, new HashMap<>())), "CompactMap(INSERTION) -> CompactMap(INSERTION) -> HashMap"));
    }

    private static Stream<Arguments> wrapperCycles() {
        return Stream.of(// CaseInsensitiveMap that wraps itself
        Arguments.of(createSelfWrappingCaseInsensitiveMap(), "CaseInsensitiveMap -> CYCLE -> CaseInsensitiveMap"), // TrackingMap that wraps itself
        Arguments.of(createSelfWrappingTrackingMap(), "TrackingMap -> CYCLE -> TrackingMap"));
    }

    private static Stream<Arguments> compactMapCycles() {
        // Build a CompactMap whose internal map is itself (cycle)
        CompactMap<String, String> cm = new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.MAP, null);
        // create the cycle via reflection
        setCompactMapVal(cm, cm);
        return Stream.of(Arguments.of(cm, "CompactMap(INSERTION) -> CYCLE -> CompactMap"));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// CompactMap MAP logical type but internal map is null → NPE
        Arguments.of(new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.MAP, null), NullPointerException.class), // CaseInsensitiveMap wrapping null → NPE
        Arguments.of(new CaseInsensitiveMap<>(null), NullPointerException.class), // TrackingMap wrapping null → NPE
        Arguments.of(new TrackingMap<>(null), NullPointerException.class), // CompactMap MAP logical type but internal value is not a Map → CCE
        Arguments.of(createCompactMapWithInvalidInner(), ClassCastException.class));
    }

    /**
     * Creates a {@code CaseInsensitiveMap} that wraps itself to form a cycle.
     */
    private static CaseInsensitiveMap<String, String> createSelfWrappingCaseInsensitiveMap() {
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ci = mock(CaseInsensitiveMap.class);
        when(ci.getWrappedMap()).thenReturn(ci);
        return ci;
    }

    /**
     * Creates a {@code TrackingMap} that wraps itself to form a cycle.
     */
    private static TrackingMap<String, String> createSelfWrappingTrackingMap() {
        @SuppressWarnings("unchecked")
        TrackingMap<String, String> tr = mock(TrackingMap.class);
        when(tr.getWrappedMap()).thenReturn(tr);
        return tr;
    }

    /**
     * Uses reflection to set the private {@code val} field of a CompactMap.
     */
    private static void setCompactMapVal(CompactMap<?, ?> compactMap, Object value) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(compactMap, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates a CompactMap whose internal {@code val} is a non‑Map object,
     *  causing a {@code ClassCastException} when the utility method tries to cast.
     */
    private static CompactMap<String, String> createCompactMapWithInvalidInner() {
        CompactMap<String, String> cm = new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.MAP, null);
        setCompactMapVal(cm, "not a map");
        return cm;
    }

    @Test
    void testNullMap() {
        assertEquals("null", invokeGetMapStructureString(null));
    }

    @Test
    void testSimpleHashMap() {
        Map<String, String> map = new HashMap<>();
        assertEquals("HashMap", invokeGetMapStructureString(map));
    }

    @Test
    void testNavigableMap() {
        // TreeMap implements NavigableMap
        Map<Integer, String> map = new TreeMap<>();
        assertEquals("TreeMap (NavigableMap)", invokeGetMapStructureString(map));
    }

    @Test
    void testCaseInsensitiveMapWrapper() {
        // Mock a CaseInsensitiveMap that wraps a plain HashMap
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ciMap = mock(CaseInsensitiveMap.class);
        Map<String, String> inner = new HashMap<>();
        when(ciMap.getWrappedMap()).thenReturn(inner);
        String result = invokeGetMapStructureString(ciMap);
        assertEquals("CaseInsensitiveMap -> HashMap", result);
    }

    @Test
    void testTrackingMapWrapper() {
        // Mock a TrackingMap that wraps a plain HashMap
        @SuppressWarnings("unchecked")
        TrackingMap<String, String> trMap = mock(TrackingMap.class);
        Map<String, String> inner = new HashMap<>();
        when(trMap.getWrappedMap()).thenReturn(inner);
        String result = invokeGetMapStructureString(trMap);
        assertEquals("TrackingMap -> HashMap", result);
    }

    @Test
    void testCompactMapWithNonMapValue() {
        // Mock a CompactMap that reports an ordering and a non‑MAP logical value type
        @SuppressWarnings("unchecked")
        CompactMap<String, String> compactMap = mock(CompactMap.class);
        when(compactMap.getOrdering()).thenReturn("INSERTION");
        // Use any enum constant that is *not* MAP – LIST is present in the library
        when(compactMap.getLogicalValueType()).thenReturn(CompactMap.LogicalValueType.LIST);
        String result = invokeGetMapStructureString(compactMap);
        assertEquals("CompactMap(INSERTION) -> [LIST]", result);
    }

    @Test
    void testCaseInsensitiveMapCycleDetection() {
        // Create a mock that returns itself as the wrapped map – this creates a cycle
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<String, String> ciMap = mock(CaseInsensitiveMap.class);
        when(ciMap.getWrappedMap()).thenReturn(ciMap);
        String result = invokeGetMapStructureString(ciMap);
        assertEquals("CaseInsensitiveMap -> CYCLE -> CaseInsensitiveMap", result);
    }

    @Test
    void testTrackingMapCycleDetection() {
        // Same idea as the previous test but with TrackingMap
        @SuppressWarnings("unchecked")
        TrackingMap<String, String> trMap = mock(TrackingMap.class);
        when(trMap.getWrappedMap()).thenReturn(trMap);
        String result = invokeGetMapStructureString(trMap);
        assertEquals("TrackingMap -> CYCLE -> TrackingMap", result);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] map={0}")
    @MethodSource("nullAndSimpleMaps")
    void testNullAndSimpleMaps(Map<?, ?> map, String expected) {
        assertEquals(expected, invokeGetMapStructureString(map));
    }

    @ParameterizedTest(name = "[{index}] wrapper chain")
    @MethodSource("wrapperChains")
    void testWrapperChains(Map<?, ?> map, String expected) {
        assertEquals(expected, invokeGetMapStructureString(map));
    }

    @ParameterizedTest(name = "[{index}] compact map variant")
    @MethodSource("compactMapVariants")
    void testCompactMapVariants(Map<?, ?> map, String expected) {
        assertEquals(expected, invokeGetMapStructureString(map));
    }

    @ParameterizedTest(name = "[{index}] wrapper cycle")
    @MethodSource("wrapperCycles")
    void testWrapperCycles(Map<?, ?> map, String expected) {
        assertEquals(expected, invokeGetMapStructureString(map));
    }

    @ParameterizedTest(name = "[{index}] compact map cycle")
    @MethodSource("compactMapCycles")
    void testCompactMapCycle(Map<?, ?> map, String expected) {
        assertEquals(expected, invokeGetMapStructureString(map));
    }

    @ParameterizedTest(name = "[{index}] exception case")
    @MethodSource("exceptionCases")
    void testExceptionPropagation(Map<?, ?> map, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> invokeGetMapStructureString(map));
    }
}
