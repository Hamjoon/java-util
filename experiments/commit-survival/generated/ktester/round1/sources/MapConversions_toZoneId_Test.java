package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toZoneId_Test {

    @Mock
    private Converter // mocked dependency
    converter;

    /* -------------------------------------------------------------
     *  Group: "Zone key takes precedence"
     * ------------------------------------------------------------- */
    private static Stream<Arguments> zonePrecedenceProvider() {
        // case 1: both "zone" and "id" present – "zone" wins (String value)
        Map<String, Object> mapBoth = new HashMap<>();
        mapBoth.put(MapConversions.ZONE, "America/New_York");
        mapBoth.put(MapConversions.ID, "Europe/Paris");
        // case 2: "zone" already holds a ZoneId instance
        Map<String, Object> mapZoneId = new HashMap<>();
        mapZoneId.put(MapConversions.ZONE, ZoneId.of("UTC"));
        return Stream.of(Arguments.of(mapBoth, ZoneId.of("America/New_York"), "String zone value takes precedence"), Arguments.of(mapZoneId, ZoneId.of("UTC"), "ZoneId instance is returned unchanged"));
    }

    /* -------------------------------------------------------------
     *  Group: "Id key used when zone missing"
     * ------------------------------------------------------------- */
    private static Stream<Arguments> idFallbackProvider() {
        // case 1: only "id" present – String value
        Map<String, Object> mapIdString = new HashMap<>();
        mapIdString.put(MapConversions.ID, "Europe/London");
        // case 2: "id" holds a ZoneId already
        Map<String, Object> mapIdZoneId = new HashMap<>();
        mapIdZoneId.put(MapConversions.ID, ZoneId.of("Asia/Tokyo"));
        return Stream.of(Arguments.of(mapIdString, ZoneId.of("Europe/London"), "String id value is converted"), Arguments.of(mapIdZoneId, ZoneId.of("Asia/Tokyo"), "ZoneId instance from id is returned unchanged"));
    }

    /* -------------------------------------------------------------
     *  Group: "Fallback to fromMap when neither zone nor id present"
     * ------------------------------------------------------------- */
    private static Stream<Arguments> fallbackProvider() {
        // case 1: arbitrary map – converter returns a concrete ZoneId (e.g., GMT)
        Map<String, Object> mapArbitrary = new HashMap<>();
        mapArbitrary.put("someKey", "someValue");
        // case 2: empty map – converter returns null to simulate a null fallback
        Map<String, Object> mapEmpty = new HashMap<>();
        return Stream.of(Arguments.of(mapArbitrary, ZoneId.of("GMT"), "Fallback uses converter on whole map and returns GMT"), Arguments.of(mapEmpty, null, "Fallback returns null when converter yields null"));
    }

    @Test
    void toZoneId_usesZoneKeyWhenPresent() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ZONE, "America/New_York");
        ZoneId expected = ZoneId.of("America/New_York");
        when(converter.convert("America/New_York", ZoneId.class)).thenReturn(expected);
        // WHEN
        ZoneId actual = MapConversions.toZoneId(map, converter);
        // THEN
        assertEquals(expected, actual);
        verify(converter, times(1)).convert("America/New_York", ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void toZoneId_usesIdKeyWhenZoneMissing() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ID, "Europe/Paris");
        ZoneId expected = ZoneId.of("Europe/Paris");
        when(converter.convert("Europe/Paris", ZoneId.class)).thenReturn(expected);
        // WHEN
        ZoneId actual = MapConversions.toZoneId(map, converter);
        // THEN
        assertEquals(expected, actual);
        verify(converter, times(1)).convert("Europe/Paris", ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    @Test
    void toZoneId_fallsBackToFromMapWhenNoZoneOrId() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        // The fallback path eventually calls converter.convert(sourceMap, ZoneId.class)
        ZoneId expected = ZoneId.of("UTC");
        when(converter.convert(map, ZoneId.class)).thenReturn(expected);
        // WHEN
        ZoneId actual = MapConversions.toZoneId(map, converter);
        // THEN
        assertEquals(expected, actual);
        verify(converter, times(1)).convert(map, ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("zonePrecedenceProvider")
    void toZoneId_zoneKeyTakesPrecedence(Map<String, Object> map, ZoneId expected, String description) {
        // Configure the mock converter:
        // – If the value is a String, convert it to ZoneId via ZoneId.of(...)
        // – If the value is already a ZoneId, return it unchanged.
        when(converter.convert(any(), eq(ZoneId.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            return (arg instanceof String) ? ZoneId.of((String) arg) : arg;
        });
        // Execute the method under test
        ZoneId actual = MapConversions.toZoneId(map, converter);
        // Verify result and interaction
        assertEquals(expected, actual, "Returned ZoneId does not match expectation");
        // The converter should have been invoked exactly once with the value from the "zone" key
        verify(converter, times(1)).convert(map.get(MapConversions.ZONE), ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("idFallbackProvider")
    void toZoneId_idKeyUsedWhenZoneMissing(Map<String, Object> map, ZoneId expected, String description) {
        // Same generic mock behaviour as the previous test
        when(converter.convert(any(), eq(ZoneId.class))).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            return (arg instanceof String) ? ZoneId.of((String) arg) : arg;
        });
        ZoneId actual = MapConversions.toZoneId(map, converter);
        assertEquals(expected, actual, "Returned ZoneId does not match expectation");
        // The converter must be called with the value from the "id" key
        verify(converter, times(1)).convert(map.get(MapConversions.ID), ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("fallbackProvider")
    void toZoneId_fallbackWhenNoZoneOrId(Map<String, Object> map, ZoneId expected, String description) {
        // Stub the converter to return the expected value when called with the whole map
        when(converter.convert(map, ZoneId.class)).thenReturn(expected);
        ZoneId actual = MapConversions.toZoneId(map, converter);
        assertEquals(expected, actual, "Fallback result does not match expectation");
        verify(converter, times(1)).convert(map, ZoneId.class);
        verifyNoMoreInteractions(converter);
    }

    /* -------------------------------------------------------------
     *  Group: "Null from argument"
     * ------------------------------------------------------------- */
    @Test
    void toZoneId_nullFromArgument_throwsNPE() {
        // No need to configure the mock – the NPE occurs before any interaction
        assertThrows(NullPointerException.class, () -> MapConversions.toZoneId(null, converter), "Method should propagate NullPointerException when 'from' is null");
        // Ensure the converter was never touched
        verifyNoInteractions(converter);
    }
}
