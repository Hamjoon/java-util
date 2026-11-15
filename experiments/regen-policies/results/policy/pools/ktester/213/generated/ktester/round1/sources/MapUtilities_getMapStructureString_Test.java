package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapUtilities_getMapStructureString_Test {

    /* ---------------------------------------------------------------------- */
    @Mock
    CaseInsensitiveMap<Object, Object> caseInsensitiveMapMock;

    @Mock
    TrackingMap<Object, Object> trackingMapMock;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock behaviour before each test
        reset(caseInsensitiveMapMock, trackingMapMock);
    }

    @Test
    void shouldReturnNullStringWhenMapIsNull() {
        assertEquals("null", MapUtilities.getMapStructureString(null));
    }

    @Test
    void shouldIdentifyPlainHashMap() {
        Map<String, String> map = new HashMap<>();
        assertEquals("HashMap", MapUtilities.getMapStructureString(map));
    }

    @Test
    void shouldIdentifyNavigableMapWithSuffix() {
        NavigableMap<Integer, Integer> map = new TreeMap<>();
        assertEquals("TreeMap(NavigableMap)", MapUtilities.getMapStructureString(map));
    }

    @Test
    void shouldHandleCaseInsensitiveMapWrappingPlainMap() {
        Map<String, String> inner = new HashMap<>();
        when(caseInsensitiveMapMock.getWrappedMap()).thenReturn(inner);
        // First element: "CaseInsensitiveMap"
        // Second element: simple name of the wrapped map ("HashMap")
        String result = MapUtilities.getMapStructureString(caseInsensitiveMapMock);
        assertEquals("CaseInsensitiveMap -> HashMap", result);
    }

    @Test
    void shouldHandleTrackingMapWrappingPlainMap() {
        Map<String, String> inner = new LinkedHashMap<>();
        when(trackingMapMock.getWrappedMap()).thenReturn(inner);
        String result = MapUtilities.getMapStructureString(trackingMapMock);
        assertEquals("TrackingMap -> LinkedHashMap", result);
    }

    @Test
    void shouldDetectCycleInCaseInsensitiveMap() {
        // The mock returns itself, creating a cycle
        when(caseInsensitiveMapMock.getWrappedMap()).thenReturn(caseInsensitiveMapMock);
        String result = MapUtilities.getMapStructureString(caseInsensitiveMapMock);
        // Expected: first the wrapper name, then the cycle detection entry
        assertEquals("CaseInsensitiveMap -> CYCLE -> CaseInsensitiveMap", result);
    }

    @Test
    void shouldDetectCycleInTrackingMap() {
        when(trackingMapMock.getWrappedMap()).thenReturn(trackingMapMock);
        String result = MapUtilities.getMapStructureString(trackingMapMock);
        assertEquals("TrackingMap -> CYCLE -> TrackingMap", result);
    }

    @Test
    void shouldProcessMixedWrappersWithoutCycle() {
        // CaseInsensitiveMap wraps a TrackingMap which wraps a plain HashMap
        Map<String, String> leaf = new HashMap<>();
        when(trackingMapMock.getWrappedMap()).thenReturn(leaf);
        when(caseInsensitiveMapMock.getWrappedMap()).thenReturn(trackingMapMock);
        String result = MapUtilities.getMapStructureString(caseInsensitiveMapMock);
        // Expected order: CaseInsensitiveMap -> TrackingMap -> HashMap
        assertEquals("CaseInsensitiveMap -> TrackingMap -> HashMap", result);
    }

    @Test
    void shouldDetectCycleAcrossDifferentWrappers() {
        // caseInsensitiveMapMock wraps trackingMapMock, which wraps caseInsensitiveMapMock again
        when(caseInsensitiveMapMock.getWrappedMap()).thenReturn(trackingMapMock);
        when(trackingMapMock.getWrappedMap()).thenReturn(caseInsensitiveMapMock);
        String result = MapUtilities.getMapStructureString(caseInsensitiveMapMock);
        // Expected: CaseInsensitiveMap -> TrackingMap -> CYCLE -> CaseInsensitiveMap
        assertEquals("CaseInsensitiveMap -> TrackingMap -> CYCLE -> CaseInsensitiveMap", result);
    }

    @Test
    void shouldHandleCompactMapWithMapValueAndNestedHashMap() {
        // CompactMap(INSERTION, MAP) wrapping a HashMap
        CompactMap<String, String> innerMap = new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.MAP, new HashMap<>());
        String result = MapUtilities.getMapStructureString(innerMap);
        assertEquals("CompactMap(INSERTION) -> HashMap", result);
    }

    @Test
    void shouldHandleCompactMapWithListValue() {
        // CompactMap(INSERTION, LIST) – value type is not MAP, so only enum name is added
        CompactMap<String, String> compact = new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.LIST);
        String result = MapUtilities.getMapStructureString(compact);
        assertEquals("CompactMap(INSERTION) -> [LIST]", result);
    }

    @Test
    void shouldHandleCompactMapOrderingVariations_NaturalSet() {
        // CompactMap(NATURAL, SET) – non‑MAP value type
        CompactMap<String, String> compact = new CompactMap<>(CompactMap.Ordering.NATURAL, CompactMap.LogicalValueType.SET);
        String result = MapUtilities.getMapStructureString(compact);
        assertEquals("CompactMap(NATURAL) -> [SET]", result);
    }

    @Test
    void shouldHandleCompactMapOrderingVariations_AccessMap() {
        // CompactMap(ACCESS, MAP) wrapping a LinkedHashMap
        CompactMap<String, String> compact = new CompactMap<>(CompactMap.Ordering.ACCESS, CompactMap.LogicalValueType.MAP, new LinkedHashMap<>());
        String result = MapUtilities.getMapStructureString(compact);
        assertEquals("CompactMap(ACCESS) -> LinkedHashMap", result);
    }

    @Test
    void shouldThrowNPEWhenCompactMapMapValueHasNullInternalMap() {
        // CompactMap(INSERTION, MAP, null) – internal map is null, leading to NPE
        CompactMap<String, String> compact = new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.MAP, null);
        assertThrows(NullPointerException.class, () -> MapUtilities.getMapStructureString(compact));
    }

    @Test
    void shouldThrowNPEWhenWrapperHasNullWrappedMap_CaseInsensitiveMap() {
        // Real CaseInsensitiveMap wrapping null
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(null);
        assertThrows(NullPointerException.class, () -> MapUtilities.getMapStructureString(ciMap));
    }

    @Test
    void shouldThrowNPEWhenWrapperHasNullWrappedMap_TrackingMap() {
        // Real TrackingMap wrapping null
        TrackingMap<String, String> trMap = new TrackingMap<>(null);
        assertThrows(NullPointerException.class, () -> MapUtilities.getMapStructureString(trMap));
    }

    @Test
    void shouldThrowNPEWhenCompactMapInsideWrapperHasNullInternalMap() {
        // CaseInsensitiveMap -> CompactMap(MAP) with null internal map
        CompactMap<String, String> compact = new CompactMap<>(CompactMap.Ordering.NATURAL, CompactMap.LogicalValueType.MAP, null);
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(compact);
        assertThrows(NullPointerException.class, () -> MapUtilities.getMapStructureString(ciMap));
    }

    @Test
    void shouldHandleCaseInsensitiveMapWrappingCompactMapWithListValue() {
        // CaseInsensitiveMap -> CompactMap(LIST)
        CompactMap<String, String> compact = new CompactMap<>(CompactMap.Ordering.INSERTION, CompactMap.LogicalValueType.LIST);
        CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>(compact);
        String result = MapUtilities.getMapStructureString(ciMap);
        assertEquals("CaseInsensitiveMap -> CompactMap(INSERTION) -> [LIST]", result);
    }

    @Test
    void shouldHandleTrackingMapWrappingNavigableTreeMap() {
        // TrackingMap -> TreeMap (which is NavigableMap)
        TrackingMap<String, Integer> trMap = new TrackingMap<>(new TreeMap<>());
        String result = MapUtilities.getMapStructureString(trMap);
        assertEquals("TrackingMap -> TreeMap(NavigableMap)", result);
    }
}
