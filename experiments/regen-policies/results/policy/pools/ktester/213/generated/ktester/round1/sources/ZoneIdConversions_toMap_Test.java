package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.ZoneOffset;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZoneIdConversions_toMap_Test {

    /**
     * Converter instance used for the tests – the method under test does not actually use it.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a real Converter with default options.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Provides valid {@link ZoneId} inputs together with the expected string
     * that should appear under the {@code "zone"} key in the returned map.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Group: Valid conversions
        Arguments.of(ZoneId.of("UTC"), null, "UTC", "Convert a standard ZoneId (UTC) to a map; converter is null."), Arguments.of(ZoneId.of("America/New_York"), new Converter(new ConverterOptions()), "America/New_York", "Convert a region‑based ZoneId to a map; non‑null Converter supplied."), Arguments.of(ZoneId.of("+02:00"), null, "+02:00", "Convert a ZoneOffset based ZoneId (+02:00) to a map."), // Group: Additional valid conversions
        Arguments.of(ZoneId.of("GMT"), null, "GMT", "Convert a ZoneId representing GMT to a map."), Arguments.of(ZoneId.of("UTC+01:00"), null, "UTC+01:00", "Convert a ZoneId with UTC+01:00 prefix to a map."), Arguments.of(ZoneOffset.ofHoursMinutesSeconds(5, 45, 30), null, "+05:45:30", "Convert a ZoneOffset with seconds precision to a map."), // System default – expected value is computed at runtime
        Arguments.of(ZoneId.systemDefault(), null, ZoneId.systemDefault().toString(), "Convert the JVM's default ZoneId to a map."));
    }

    /**
     * Provides invalid inputs together with the exception class that is expected
     * to be thrown by {@link ZoneIdConversions#toMap(Object, Converter)}.
     */
    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Null source
        Arguments.of(null, NullPointerException.class, "Passing null as the source should cause NullPointerException."), // Wrong type – String
        Arguments.of("UTC", ClassCastException.class, "Passing a String instead of ZoneId should cause ClassCastException."), // Wrong type – Date
        Arguments.of(new java.util.Date(), ClassCastException.class, "Passing a Date instead of ZoneId should cause ClassCastException."));
    }

    @Test
    void toMap_ShouldReturnMapContainingZoneString_ForStandardZoneId() {
        // GIVEN
        ZoneId zoneId = ZoneId.of("America/New_York");
        // WHEN
        Map<String, Object> result = ZoneIdConversions.toMap(zoneId, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey("zone"), "Map must contain the key 'zone'");
        assertEquals(zoneId.toString(), result.get("zone"), "The value associated with 'zone' must be the ZoneId's string representation");
    }

    @Test
    void toMap_ShouldReturnMapContainingZoneString_ForUtcOffset() {
        // GIVEN
        // equivalent to ZoneOffset.UTC
        ZoneId utc = ZoneId.of("Z");
        // WHEN
        Map<String, Object> result = ZoneIdConversions.toMap(utc, converter);
        // THEN
        assertNotNull(result);
        assertEquals("Z", result.get("zone"));
    }

    @Test
    void toMap_ShouldThrowClassCastException_WhenSourceIsNotZoneId() {
        // GIVEN
        Object notAZoneId = "not a zone";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZoneIdConversions.toMap(notAZoneId, converter), "Passing a non‑ZoneId object should result in a ClassCastException");
    }

    @Test
    void toMap_ShouldThrowNullPointerException_WhenSourceIsNull() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZoneIdConversions.toMap(nullSource, converter), "Passing null as the source should result in a NullPointerException");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("validConversionProvider")
    void toMap_ShouldReturnMapWithExpectedZoneString(ZoneId zoneId, Converter suppliedConverter, String expectedZoneString, String description) {
        // GIVEN
        Converter conv = (suppliedConverter != null) ? suppliedConverter : null;
        // WHEN
        Map<String, Object> result = ZoneIdConversions.toMap(zoneId, conv);
        // THEN
        assertNotNull(result, "Result map must not be null");
        assertEquals(1, result.size(), "Result map must contain exactly one entry");
        assertTrue(result.containsKey("zone"), "Result map must contain the key 'zone'");
        assertEquals(expectedZoneString, result.get("zone"), "The 'zone' value should match the expected string representation");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("invalidConversionProvider")
    void toMap_ShouldThrowExpectedException_ForInvalidInputs(Object source, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> ZoneIdConversions.toMap(source, null), description);
    }
}
