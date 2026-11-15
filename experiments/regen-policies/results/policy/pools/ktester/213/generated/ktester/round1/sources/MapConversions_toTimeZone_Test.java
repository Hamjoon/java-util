package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;

// @ExtendWith(MockitoExtension.class)
class MapConversions_toTimeZone_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter now requires a ConverterOptions instance.
        // Using the no‑arg constructor which internally creates default options.
//         converter = new Converter();
    }

    @Test
    void toTimeZone_givenNull_returnsNull() {
        TimeZone tz = MapConversions.toTimeZone(null, converter);
        assertNull(tz, "Converting a null source should yield null");
    }

    @Test
    void toTimeZone_givenTimeZoneInstance_returnsSameInstance() {
        TimeZone original = TimeZone.getTimeZone("UTC");
        TimeZone result = MapConversions.toTimeZone(original, converter);
        assertSame(original, result, "When the source is already a TimeZone, the same instance should be returned");
    }

    @Test
    void toTimeZone_givenMapWithIdKey_returnsCorrespondingTimeZone() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "America/New_York");
        TimeZone tz = MapConversions.toTimeZone(map, converter);
        assertNotNull(tz, "Resulting TimeZone must not be null");
        assertEquals("America/New_York", tz.getID(), "TimeZone ID should match the 'id' map entry");
    }

    @Test
    void toTimeZone_givenMapWithValueKey_returnsCorrespondingTimeZone() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "GMT");
        TimeZone tz = MapConversions.toTimeZone(map, converter);
        assertNotNull(tz, "Resulting TimeZone must not be null");
        assertEquals("GMT", tz.getID(), "TimeZone ID should match the 'value' map entry");
    }

    @Test
    void toTimeZone_givenMapWithUnderscoreVKey_returnsCorrespondingTimeZone() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "UTC");
        TimeZone tz = MapConversions.toTimeZone(map, converter);
        assertNotNull(tz, "Resulting TimeZone must not be null");
        assertEquals("UTC", tz.getID(), "TimeZone ID should match the '_v' map entry");
    }

    @Test
    void toTimeZone_givenMapWithZoneKey_returnsCorrespondingTimeZone() {
        Map<String, Object> map = new HashMap<>();
        map.put("zone", "Europe/Paris");
        TimeZone tz = MapConversions.toTimeZone(map, converter);
        assertNotNull(tz, "Resulting TimeZone must not be null");
        assertEquals("Europe/Paris", tz.getID(), "TimeZone ID should match the 'zone' map entry");
    }

    @Test
    void toTimeZone_givenMapWithoutSupportedKeys_returnsNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("unknown", "someValue");
        TimeZone tz = MapConversions.toTimeZone(map, converter);
        assertNull(tz, "Map without any of the recognised keys should result in null");
    }

//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("normalCases")
//     void toTimeZone_normalCases(Object from, Converter conv, String expectedId, boolean expectSameInstance, String description) {
//         TimeZone result = MapConversions.toTimeZone(from, conv);
//         if (expectedId == null) {
//             assertNull(result, description);
//         } else {
//             assertNotNull(result, description);
//             assertEquals(expectedId, result.getID(), description);
//             if (expectSameInstance) {
//                 // When the source is already a TimeZone we expect identity.
//                 assertSame(from, result, description);
//             }
//         }
//     }

//     @ParameterizedTest(name = "[{index}] {4}")
//     @MethodSource("exceptionCases")
//     void toTimeZone_exceptionCases(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
//         assertThrows(expectedException, () -> MapConversions.toTimeZone(from, conv), description);
//     }

    @Test
    void toTimeZone_givenStringZoneId_returnsTimeZone() {
        TimeZone tz = MapConversions.toTimeZone("America/Los_Angeles", converter);
        assertNotNull(tz);
        assertEquals("America/Los_Angeles", tz.getID());
    }

    @Test
    void toTimeZone_givenMapWithMultipleKeys_usesZoneKeyFirst() {
        Map<String, Object> map = new HashMap<>();
        map.put("zone", "Europe/Berlin");
        map.put("id", "America/New_York");
        map.put("value", "GMT");
        map.put("_v", "UTC");
        TimeZone tz = MapConversions.toTimeZone(map, converter);
        assertNotNull(tz);
        assertEquals("Europe/Berlin", tz.getID(), "The 'zone' key should win according to ZONE_KEYS order.");
    }

    @Test
    void toTimeZone_givenMapWithTimeZoneObject_returnsSameInstance() {
        TimeZone chicago = TimeZone.getTimeZone("America/Chicago");
        Map<String, Object> map = new HashMap<>();
        map.put("zone", chicago);
        TimeZone tz = MapConversions.toTimeZone(map, converter);
        assertSame(chicago, tz, "When the map value is already a TimeZone, the same instance should be returned.");
    }

    @Test
    void toTimeZone_givenUnsupportedType_returnsNull() {
        Integer unsupported = 12345;
        TimeZone tz = MapConversions.toTimeZone(unsupported, converter);
        assertNull(tz, "Unsupported input type should result in null.");
    }
}