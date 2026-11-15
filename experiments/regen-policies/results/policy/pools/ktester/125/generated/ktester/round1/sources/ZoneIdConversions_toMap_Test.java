package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.ZoneOffset;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZoneIdConversions_toMap_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The toMap method does not actually use the Converter instance,
        // but providing a real instance keeps the test realistic.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none needed here).
        converter = null;
    }

    /**
     * Provides a stream of valid {@code from} arguments together with the expected
     * {@code zone} string that should appear in the resulting map.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> validZoneIdProvider() {
        return Stream.of(// Standard UTC ZoneId
        org.junit.jupiter.params.provider.Arguments.of(ZoneId.of("UTC"), "UTC"), // Region‑based ZoneId
        org.junit.jupiter.params.provider.Arguments.of(ZoneId.of("America/New_York"), "America/New_York"), // ZoneOffset (subclass of ZoneId)
        org.junit.jupiter.params.provider.Arguments.of(ZoneOffset.ofHoursMinutes(5, 30), "+05:30"), // Short GMT offset ID
        org.junit.jupiter.params.provider.Arguments.of(ZoneId.of("GMT+02:00"), "GMT+02:00"), // System default ZoneId – the expected value is computed at runtime
        org.junit.jupiter.params.provider.Arguments.of(ZoneId.systemDefault(), ZoneId.systemDefault().toString()));
    }

    @Test
    void testToMap_WithValidZoneId_ReturnsCorrectMap() {
        // given
        ZoneId zoneId = ZoneId.of("UTC");
        // when
        Map<String, Object> result = ZoneIdConversions.toMap(zoneId, converter);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey("zone"), "Map should contain the key 'zone'");
        assertEquals(zoneId.toString(), result.get("zone"), "Value should be the ZoneId string representation");
    }

    @Test
    void testToMap_WithInvalidSource_ThrowsClassCastException() {
        // given
        Object notAZoneId = "not-a-zone-id";
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toMap(notAZoneId, converter), "Providing a non‑ZoneId source should result in ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Test that a non‑null Converter instance is ignored
     * ------------------------------------------------------------- */
    @Test
    void testToMap_WithNonNullConverter_Ignored() {
        // given
        ZoneId zoneId = ZoneId.of("Europe/Paris");
        Converter nonNullConverter = new Converter(new ConverterOptions());
        // when
        Map<String, Object> result = ZoneIdConversions.toMap(zoneId, nonNullConverter);
        // then
        assertNotNull(result);
        assertEquals("Europe/Paris", result.get("zone"));
    }

    /* -------------------------------------------------------------
     *  Null `from` argument – should result in NPE when accessing toString()
     * ------------------------------------------------------------- */
    @Test
    void testToMap_NullFrom_ThrowsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> ZoneIdConversions.toMap(null, null), "Passing a null `from` argument should cause a NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Invalid type for `from` – expect ClassCastException
     * ------------------------------------------------------------- */
    @Test
    void testToMap_InvalidFromType_ThrowsClassCastException() {
        // given
        Object notAZoneId = "not a ZoneId";
        // when / then
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toMap(notAZoneId, null), "Providing a non‑ZoneId source should result in ClassCastException");
    }
}
