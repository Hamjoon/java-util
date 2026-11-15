package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toTimeZone_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter sharedConverter;

    /**
     * Simple holder with a public field named {@code zone}.
     */
    static class ZoneHolder {

        public String zone;

        ZoneHolder(String zone) {
            this.zone = zone;
        }
    }

    /**
     * Bean with a private field {@code zone} and a public getter.
     */
    static class ZoneBean {

        private final String zone;

        ZoneBean(String zone) {
            this.zone = zone;
        }

        public String getZone() {
            return zone;
        }
    }

    /**
     * Bean with a public field {@code zone} of type {@link ZoneId}.
     */
    static class ZoneFieldBean {

        public ZoneId zone;

        ZoneFieldBean(ZoneId zone) {
            this.zone = zone;
        }
    }

    /**
     * Bean whose getter deliberately throws a {@link RuntimeException}.
     */
    static class FaultyZoneBean {

        public String getZone() {
            throw new RuntimeException("Intentional failure in getter");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use a real Converter with default options – no need for a mock because
        // we are testing the conversion logic inside MapConversions, not the
        // Converter itself.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared converter reference.
        sharedConverter = null;
    }

    /**
     * Helper that invokes the package‑private method under test.
     */
    private static TimeZone invokeToTimeZone(Object source) {
        // Direct call because the test class is in the same package.
        return MapConversions.toTimeZone(source, sharedConverter);
    }

    private static Stream<Arguments> successCases() {
        return Stream.of(// Map with a valid zone string
        Arguments.of("Map contains the key 'zone' with a valid zone ID string", Map.of("zone", "UTC"), "UTC"), // Map where the value is already a TimeZone instance
        Arguments.of("Map contains the key 'zone' whose value is a TimeZone object", Map.of("zone", TimeZone.getTimeZone("America/New_York")), "America/New_York"), // Plain String input
        Arguments.of("Input is a plain String representing a zone ID", "Europe/Paris", "Europe/Paris"), // Input already a TimeZone instance
        Arguments.of("Input is already a TimeZone instance", TimeZone.getTimeZone("GMT"), "GMT"), // Null source
        Arguments.of("Null input should result in null", null, null), // Bean with a public field named 'zone' (String)
        Arguments.of("Object with a public field named 'zone' (String)", new ZoneHolder("Asia/Tokyo"), "Asia/Tokyo"), // Map with an invalid zone ID – should raise IllegalArgumentException,
        // handled in the exception test group (not here).
        // Map without the required key
        Arguments.of("Map does not contain the required 'zone' key", Map.of("other", "value"), null), // Map with a ZoneId instance
        Arguments.of("Map contains a ZoneId instance under the key 'zone'", Map.of("zone", ZoneId.of("Europe/London")), "Europe/London"), // Map with a ZoneOffset instance
        Arguments.of("Map contains a ZoneOffset instance", Map.of("zone", ZoneOffset.ofHoursMinutes(5, 30)), "GMT+05:30"), // Map with a null value for the required key
        Arguments.of("Map contains the required key but the associated value is null", Map.of("zone", (Object) null), null), // Bean with a private field 'zone' and a public getter
        Arguments.of("Bean with a private field 'zone' and a public getter", new ZoneBean("America/Los_Angeles"), "America/Los_Angeles"), // Bean with a public field of type ZoneId
        Arguments.of("Bean with a public field named 'zone' of type ZoneId", new ZoneFieldBean(ZoneId.of("Asia/Kolkata")), "Asia/Kolkata"), // Map with extra unrelated entries
        Arguments.of("Map contains the required 'zone' key plus additional unrelated entries", new HashMap<String, Object>() {

            {
                put("zone", "Europe/Paris");
                put("extra", 123);
                put("another", "ignored");
            }
        }, "Europe/Paris"), // Empty string as zone ID (defaults to GMT)
        Arguments.of("Empty string as zone ID should default to GMT", Map.of("zone", ""), "GMT"), // Whitespace‑only string (treated as invalid, defaults to GMT)
        Arguments.of("String containing only whitespace is treated as invalid and resolves to GMT", Map.of("zone", "   "), "GMT"), // Numeric offset string
        Arguments.of("Numeric offset string should be converted to a GMT‑based TimeZone", Map.of("zone", "+02:00"), "GMT+02:00"));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null converter argument
        Arguments.of("The converter argument is null; should throw NullPointerException", "Europe/Paris", null, NullPointerException.class), // Unsupported source type (List)
        Arguments.of("Source object is a List, which is not supported", Arrays.asList("GMT"), sharedConverter, IllegalArgumentException.class), // Map with an Integer value for 'zone'
        Arguments.of("Map contains the required 'zone' key but the value is an Integer", Map.of("zone", 12345), sharedConverter, IllegalArgumentException.class), // Map with an impossible offset string
        Arguments.of("Zone string represents an impossible offset (greater than +14:00)", Map.of("zone", "GMT+25:00"), sharedConverter, IllegalArgumentException.class), // Bean whose getter throws a RuntimeException
        Arguments.of("Bean whose getter getZone() throws a RuntimeException", new FaultyZoneBean(), sharedConverter, RuntimeException.class));
    }

    @Test
    void toTimeZone_WithValidZoneString_ReturnsCorrectTimeZone() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ZONE, "America/New_York");
        TimeZone tz = invokeToTimeZone(map);
        assertNotNull(tz, "TimeZone should not be null for a valid zone string");
        assertEquals("America/New_York", tz.getID(), "Returned TimeZone ID should match the supplied zone string");
    }

    @Test
    void toTimeZone_WithUtcZoneString_ReturnsUtcTimeZone() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ZONE, "UTC");
        TimeZone tz = invokeToTimeZone(map);
        assertNotNull(tz, "TimeZone should not be null for UTC");
        assertEquals("UTC", tz.getID(), "Returned TimeZone ID should be UTC");
    }

    @Test
    void toTimeZone_WithMissingZoneKey_ReturnsNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("someOtherKey", "value");
        TimeZone tz = invokeToTimeZone(map);
        assertNull(tz, "TimeZone should be null when the map does not contain a 'zone' key");
    }

    @Test
    void toTimeZone_WithNullSource_ReturnsNull() {
        TimeZone tz = invokeToTimeZone(null);
        assertNull(tz, "TimeZone should be null when the source object is null");
    }

    @Test
    void toTimeZone_WithNonMapSource_ReturnsNull() {
        // The conversion logic expects a Map; any other type should yield null.
        String notAMap = "America/Los_Angeles";
        TimeZone tz = invokeToTimeZone(notAMap);
        assertNull(tz, "TimeZone should be null when the source is not a Map");
    }

    @Test
    void toTimeZone_WithAdditionalIrrelevantEntries_IgnoresThem() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ZONE, "GMT");
        map.put("extra", 12345);
        map.put("another", "ignored");
        TimeZone tz = invokeToTimeZone(map);
        assertNotNull(tz, "TimeZone should be produced even when extra keys are present");
        assertEquals("GMT", tz.getID(), "Returned TimeZone ID should match the 'zone' entry");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successCases")
    void toTimeZone_SuccessfulConversions(String description, Object source, String expectedTimeZoneId) {
        TimeZone result = invokeToTimeZone(source);
        if (expectedTimeZoneId == null) {
            assertNull(result, description);
        } else {
            assertNotNull(result, description);
            assertEquals(expectedTimeZoneId, result.getID(), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionCases")
    void toTimeZone_ExceptionScenarios(String description, Object source, Converter converter, Class<? extends Throwable> expectedException) {
        // When the converter argument is null we cannot use the shared instance.
        Converter usedConverter = (converter == null) ? null : sharedConverter;
        assertThrows(expectedException, () -> MapConversions.toTimeZone(source, usedConverter), description);
    }
}
