package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toZoneId_Test {

    /**
     * Shared {@link Converter} instance used by all test methods.
     */
    private static Converter converter;

    /**
     * Mocked {@link Converter} used in all test cases.
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Creates a map containing the given key/value pairs.
     */
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    /**
     * Supplies all scenarios described in the JSON specification.
     */
    private static Stream<Scenario> scenarios() {
        ZoneId utc = ZoneId.of("UTC");
        ZoneId paris = ZoneId.of("Europe/Paris");
        ZoneId berlin = ZoneId.of("Europe/Berlin");
        ZoneId newYork = ZoneId.of("America/New_York");
        return Stream.of(// -----------------------------------------------------------------
        // zone key present – non‑null value, id ignored
        // -----------------------------------------------------------------
        new Scenario("Map contains a non‑null \"zone\" value; converter should be called with it", mapOf(MapConversions.ZONE, "UTC", MapConversions.ID, "America/Los_Angeles"), "UTC", utc, utc, null), // -----------------------------------------------------------------
        // zone key present but null → fall back to id
        // -----------------------------------------------------------------
        new Scenario("\"zone\" key is present but its value is null; should use \"id\"", mapOf(MapConversions.ZONE, null, MapConversions.ID, "Europe/Paris"), "Europe/Paris", paris, paris, null), // -----------------------------------------------------------------
        // id key present – zone missing
        // -----------------------------------------------------------------
        new Scenario("Map contains only a non‑null \"id\" entry", mapOf(MapConversions.ID, "Europe/Berlin"), "Europe/Berlin", berlin, berlin, null), // -----------------------------------------------------------------
        // both keys null → fallback (returns null)
        // -----------------------------------------------------------------
        new Scenario("Both \"zone\" and \"id\" are null; method should fall back and return null", mapOf(MapConversions.ZONE, null, MapConversions.ID, null), null, null, null, null), // -----------------------------------------------------------------
        // fallback when no relevant keys (empty map)
        // -----------------------------------------------------------------
        new Scenario("Empty map – no \"zone\" or \"id\" keys, expect null via fallback", mapOf(), null, null, null, null), // -----------------------------------------------------------------
        // zone value already a ZoneId instance
        // -----------------------------------------------------------------
        new Scenario("\"zone\" value is already a ZoneId instance; should be passed unchanged", mapOf(MapConversions.ZONE, paris), paris, paris, paris, null), // -----------------------------------------------------------------
        // zone instance present together with id – zone wins
        // -----------------------------------------------------------------
        new Scenario("\"zone\" (ZoneId) wins over \"id\" when both are present", mapOf(MapConversions.ZONE, utc, MapConversions.ID, "Europe/Paris"), utc, utc, utc, null), // -----------------------------------------------------------------
        // null from parameter → NPE
        // -----------------------------------------------------------------
        new Scenario("\"from\" argument is null, expect NullPointerException", null, null, null, null, NullPointerException.class));
    }

    @Test
    void toZoneId_usesZoneKeyWhenPresent() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ZONE, "UTC");
        when(mockConverter.convert("UTC", ZoneId.class)).thenReturn(ZoneId.of("UTC"));
        ZoneId actual = MapConversions.toZoneId(map, mockConverter);
        assertEquals(ZoneId.of("UTC"), actual);
        verify(mockConverter, times(1)).convert("UTC", ZoneId.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toZoneId_usesIdKeyWhenZoneMissing() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ID, "America/New_York");
        when(mockConverter.convert("America/New_York", ZoneId.class)).thenReturn(ZoneId.of("America/New_York"));
        ZoneId actual = MapConversions.toZoneId(map, mockConverter);
        assertEquals(ZoneId.of("America/New_York"), actual);
        verify(mockConverter, times(1)).convert("America/New_York", ZoneId.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toZoneId_zoneKeyTakesPrecedenceOverIdKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ZONE, "Europe/Paris");
        map.put(MapConversions.ID, "UTC");
        when(mockConverter.convert("Europe/Paris", ZoneId.class)).thenReturn(ZoneId.of("Europe/Paris"));
        ZoneId actual = MapConversions.toZoneId(map, mockConverter);
        assertEquals(ZoneId.of("Europe/Paris"), actual);
        verify(mockConverter, times(1)).convert("Europe/Paris", ZoneId.class);
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toZoneId_returnsNullWhenNoRelevantKey() {
        // empty map
        Map<String, Object> map = new HashMap<>();
        ZoneId actual = MapConversions.toZoneId(map, mockConverter);
        assertNull(actual, "Without \"zone\" or \"id\" keys the method should return null (via fromMap).");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("scenarios")
    void toZoneId_behaviour(Scenario s) {
        // Arrange ---------------------------------------------------------
        if (s.convertArgument != null && s.stubResult != null) {
            // Stub the mock to return the desired ZoneId for the expected argument.
            when(mockConverter.convert(s.convertArgument, ZoneId.class)).thenReturn(s.stubResult);
        }
        // Act -------------------------------------------------------------
        if (s.expectedException != null) {
            // Expect an exception (e.g., NPE when \"from\" is null)
            assertThrows(s.expectedException, () -> MapConversions.toZoneId(s.map, mockConverter), s.description);
            return;
        }
        ZoneId actual = MapConversions.toZoneId(s.map, mockConverter);
        // Assert -----------------------------------------------------------
        assertEquals(s.expectedResult, actual, s.description);
        // Verify interaction with the mock --------------------------------
        if (s.convertArgument != null) {
            // The converter should be called exactly once with the expected argument.
            verify(mockConverter, times(1)).convert(s.convertArgument, ZoneId.class);
        } else {
            // No conversion should occur when both keys are absent/null.
            verifyNoInteractions(mockConverter);
        }
        // Ensure no stray calls.
        verifyNoMoreInteractions(mockConverter);
    }

    @Test
    void toZoneId_nullFromParameterThrowsNPE() {
        assertThrows(NullPointerException.class, () -> MapConversions.toZoneId(null, mockConverter), "Passing null as the 'from' argument should result in NPE.");
    }
}
