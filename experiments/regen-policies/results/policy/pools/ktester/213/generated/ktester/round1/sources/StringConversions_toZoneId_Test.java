package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toZoneId_Test {

    /**
     * Converter is required by the signature but not used by the method under test.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a ConverterOptions instance; use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<Arguments> provideOffsetAndGmtStyleIds() {
        return Stream.of(Arguments.of("+02:00"), Arguments.of("GMT+02:00"), Arguments.of("UTC"));
    }

    private static Stream<Arguments> provideNonStringInputs() {
        return Stream.of(// Integer
        Arguments.of(123), // Generic Object
        Arguments.of(new Object()));
    }

    @Test
    void toZoneId_emptyString_returnsNull() {
        ZoneId result = StringConversions.toZoneId("", converter);
        assertNull(result, "Empty string should result in null ZoneId");
    }

    @Test
    void toZoneId_validZoneId_returnsZoneId() {
        String zone = "America/New_York";
        ZoneId result = StringConversions.toZoneId(zone, converter);
        assertNotNull(result);
        assertEquals(ZoneId.of(zone), result);
    }

    @Test
    void toZoneId_unknownButTimeZoneKnown_returnsTimeZoneDerivedZoneId() {
        // "EST" is not a standard ZoneId identifier, but TimeZone.getTimeZone("EST") returns a valid TimeZone.
        String unknown = "EST";
        ZoneId result = StringConversions.toZoneId(unknown, converter);
        TimeZone tz = TimeZone.getTimeZone(unknown);
        assertEquals(tz.toZoneId(), result, "Should fall back to TimeZone-derived ZoneId");
    }

    @Test
    void toZoneId_unknownAndGmtFallback_throwsIllegalArgumentException() {
        // Any nonsense string causes TimeZone.getTimeZone to return GMT.
        String bogus = "this.is.not.a.zone";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toZoneId(bogus, converter));
        assertTrue(ex.getMessage().contains("Unknown time-zone ID"));
    }

    @Test
    void toZoneId_nullString_returnsNull() {
        ZoneId result = StringConversions.toZoneId(null, null);
        assertNull(result, "Null input should be treated as empty and return null");
    }

    @Test
    void toZoneId_shortId_fallbackToTimeZone() {
        // "PST" is not a standard ZoneId identifier, but TimeZone.getTimeZone("PST") returns a valid zone.
        String shortId = "PST";
        ZoneId result = StringConversions.toZoneId(shortId, converter);
        TimeZone tz = TimeZone.getTimeZone(shortId);
        assertEquals(tz.toZoneId(), result, "Should fall back to TimeZone-derived ZoneId");
    }

    @Test
    void toZoneId_invalidId_leadsToGmt_throwsIllegalArgumentException() {
        String bogus = "Invalid/Zone";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toZoneId(bogus, converter));
        assertTrue(ex.getMessage().contains("Unknown time-zone ID"), "Exception message should contain the unknown ID text");
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\"")
    @MethodSource("provideOffsetAndGmtStyleIds")
    void toZoneId_offsetOrGmtStyleIds_direct(String input) {
        ZoneId result = StringConversions.toZoneId(input, converter);
        assertNotNull(result);
        assertEquals(ZoneId.of(input), result, "Offset/GMT style ID should be parsed directly by ZoneId.of");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("provideNonStringInputs")
    void toZoneId_nonStringInput_throwsClassCastException(Object input) {
        assertThrows(ClassCastException.class, () -> StringConversions.toZoneId(input, null), "Passing a non‑String should result in ClassCastException");
    }
}
