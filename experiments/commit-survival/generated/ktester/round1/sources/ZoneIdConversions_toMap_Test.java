package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZoneIdConversions_toMap_Test {

    /**
     * A mock Converter – the method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> normalCases() {
        return Stream.of(Arguments.of("Standard case: UTC", ZoneId.of("UTC"), Map.of("zone", "UTC")), Arguments.of("Non‑UTC region ID", ZoneId.of("America/New_York"), Map.of("zone", "America/New_York")), Arguments.of("Fixed offset +02:00", ZoneId.of("+02:00"), Map.of("zone", "+02:00")), Arguments.of("System default zone", ZoneId.systemDefault(), Map.of("zone", ZoneId.systemDefault().toString())), Arguments.of("Legacy GMT identifier", ZoneId.of("GMT"), Map.of("zone", "GMT")), Arguments.of("Another region‑based ID", ZoneId.of("Europe/Paris"), Map.of("zone", "Europe/Paris")), Arguments.of("Offset with minutes +05:30", ZoneId.of("+05:30"), Map.of("zone", "+05:30")), Arguments.of("Non‑null Converter that would throw if used", ZoneId.of("UTC"), Map.of("zone", "UTC")));
    }

    @Test
    void toMap_returnsMapWithZoneString() {
        // GIVEN
        ZoneId zoneId = ZoneId.of("America/New_York");
        // WHEN
        Map<String, Object> result = ZoneIdConversions.toMap(zoneId, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey("zone"), "Map should contain the key 'zone'");
        assertEquals(zoneId.toString(), result.get("zone"), "The value associated with 'zone' should be the ZoneId's string representation");
    }

    /* ------------------------------------------------------------
     *  Parameterized tests for the normal (non‑exception) cases
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalCases")
    void toMap_normalCases(String description, ZoneId input, Map<String, Object> expected) {
        // WHEN
        Map<String, Object> result = ZoneIdConversions.toMap(input, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(expected, result, description);
    }

    /* ------------------------------------------------------------
     *  Tests for exceptional scenarios
     * ------------------------------------------------------------ */
    @Test
    void toMap_nullFrom_throwsClassCastException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toMap(from, mockConverter), "Passing null as the source should result in ClassCastException");
    }

    @Test
    void toMap_incompatibleType_throwsClassCastException() {
        // GIVEN
        Object from = "not a ZoneId";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toMap(from, mockConverter), "Passing an object of an incompatible type should result in ClassCastException");
    }

    @Test
    void toMap_ignoresConverter_evenIfItThrows() {
        // GIVEN: a Converter implementation that throws on any call
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object source, Class<?> targetClass) {
                throw new RuntimeException("Should not be called");
            }
        };
        ZoneId zoneId = ZoneId.of("UTC");
        // WHEN
        Map<String, Object> result = ZoneIdConversions.toMap(zoneId, throwingConverter);
        // THEN
        assertNotNull(result);
        assertEquals(Map.of("zone", "UTC"), result, "Method must ignore the Converter argument entirely");
    }
}
