package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.ZoneId;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.DateTimeException;
import java.util.*;
import java.util.stream.Stream;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MapConversions_toZoneId_Test {

    // the Converter used by MapConversions
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter now requires a ConverterOptions instance.
//         converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> directZoneIdProvider() {
        return Stream.of(Arguments.of(ZoneId.of("America/New_York"), ZoneId.of("America/New_York")), Arguments.of(null, null));
    }

    private static Stream<Arguments> singleKeyMapProvider() {
        return Stream.of(Arguments.of("zone", "Asia/Tokyo"), Arguments.of("id", "Europe/London"), Arguments.of("value", "America/Los_Angeles"), Arguments.of("_v", "UTC"));
    }

    private static Stream<Arguments> zoneIdValueMapProvider() {
        return Stream.of(Arguments.of("zone", ZoneId.of("Europe/Berlin")), Arguments.of("id", ZoneId.of("America/Chicago")));
    }

    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(// plain Integer
        Arguments.of(12345), // generic Object
        Arguments.of(new Object()));
    }

    private static Stream<Arguments> invalidMapValueProvider() {
        return Stream.of(// numeric value
        Arguments.of(Map.of("zone", 123)), // arbitrary object
        Arguments.of(Map.of("zone", new Object())), // numeric id
        Arguments.of(Map.of("id", 12345)), // no recognised keys
        Arguments.of(Map.of("foo", "bar")));
    }

    private static Stream<Arguments> anySourceProvider() {
        return Stream.of(Arguments.of("Europe/Paris"), Arguments.of(Map.of("zone", "Europe/Paris")), Arguments.of(ZoneId.of("Europe/Paris")));
    }

    @Test
    void shouldReturnSameZoneIdWhenSourceIsZoneId() {
        ZoneId original = ZoneId.of("America/New_York");
        ZoneId result = MapConversions.toZoneId(original, converter);
        assertSame(original, result, "When the source object is already a ZoneId, the same instance should be returned.");
    }

    @Test
    void shouldConvertStringToZoneId() {
        String zoneString = "Europe/Paris";
        ZoneId result = MapConversions.toZoneId(zoneString, converter);
        assertEquals(ZoneId.of(zoneString), result);
    }

    @Test
    void shouldConvertMapWithZoneKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("zone", "Asia/Tokyo");
        ZoneId result = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("Asia/Tokyo"), result);
    }

    @Test
    void shouldConvertMapWithValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "UTC");
        ZoneId result = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("UTC"), result);
    }

    @Test
    void shouldConvertMapWithUnderscoreVKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "GMT+2");
        ZoneId result = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("GMT+2"), result);
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        ZoneId result = MapConversions.toZoneId(null, converter);
        assertNull(result, "Conversion of a null source should yield null.");
    }

    /* -------------------------------------------------------------
     *  Direct ZoneId instances
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}, expected={1}")
    @MethodSource("directZoneIdProvider")
    void shouldHandleDirectZoneIdInstances(Object from, ZoneId expected) {
        ZoneId result = MapConversions.toZoneId(from, converter);
        assertEquals(expected, result);
        if (from instanceof ZoneId) {
            assertSame(from, result);
        }
    }

    /* -------------------------------------------------------------
     *  String based conversion – valid strings
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => zoneString=''{0}''")
    @ValueSource(strings = { "Europe/Paris", "GMT+02:00" })
    void shouldConvertValidStringToZoneId(String zoneString) {
        ZoneId result = MapConversions.toZoneId(zoneString, converter);
        assertEquals(ZoneId.of(zoneString), result);
    }

    /* -------------------------------------------------------------
     *  String based conversion – invalid strings
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => invalidString=''{0}''")
    @ValueSource(strings = { "Invalid/Zone", "" })
    void shouldThrowDateTimeExceptionForInvalidString(String invalid) {
        assertThrows(DateTimeException.class, () -> MapConversions.toZoneId(invalid, converter), "Parsing an invalid zone string should throw DateTimeException");
    }

    /* -------------------------------------------------------------
     *  Map based conversion – single recognised key
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => key=''{0}'', value=''{1}''")
    @MethodSource("singleKeyMapProvider")
    void shouldConvertMapWithSingleKey(String key, String zoneIdString) {
        Map<String, Object> map = Map.of(key, zoneIdString);
        ZoneId result = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of(zoneIdString), result);
    }

    /* -------------------------------------------------------------
     *  Map based conversion – priority order when multiple keys exist
     * ------------------------------------------------------------- */
    @Test
    void shouldHonorPriorityOrderWhenMultipleZoneKeysPresent() {
        Map<String, Object> map = Map.of("zone", "Europe/Paris", "id", "America/New_York", "value", "Asia/Tokyo", "_v", "UTC");
        ZoneId result = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("Europe/Paris"), result, "The 'zone' key has the highest priority and should be used.");
    }

    /* -------------------------------------------------------------
     *  Map based conversion – values that are already ZoneId
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => key=''{0}'', zoneId={1}")
    @MethodSource("zoneIdValueMapProvider")
    void shouldReturnExistingZoneIdFromMap(String key, ZoneId zoneId) {
        Map<String, Object> map = Map.of(key, zoneId);
        ZoneId result = MapConversions.toZoneId(map, converter);
        assertSame(zoneId, result, "When the map value is already a ZoneId it must be returned unchanged.");
    }

    /* -------------------------------------------------------------
     *  Invalid input handling – unsupported source types
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => unsupportedSource={0}")
    @MethodSource("invalidSourceProvider")
    void shouldThrowIllegalArgumentExceptionForUnsupportedSource(Object unsupported) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toZoneId(unsupported, converter), "Unsupported source types must cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Invalid input handling – map with non‑String / non‑ZoneId values
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => map={0}")
    @MethodSource("invalidMapValueProvider")
    void shouldThrowIllegalArgumentExceptionForInvalidMapValue(Map<String, Object> map) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toZoneId(map, converter), "Map values that are not String or ZoneId must cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Invalid map values – empty or null String
     * ------------------------------------------------------------- */
    @Test
    void shouldThrowDateTimeExceptionForEmptyStringInMap() {
        Map<String, Object> map = Map.of("value", "");
        assertThrows(DateTimeException.class, () -> MapConversions.toZoneId(map, converter), "Empty string value should result in DateTimeException");
    }

    @Test
    void shouldReturnNullWhenMapKeyValueIsNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", null);
        ZoneId result = MapConversions.toZoneId(map, converter);
        assertNull(result, "Null value for a recognised key should yield null");
    }

    /* -------------------------------------------------------------
     *  Converter null handling – any source type
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => source={0}")
    @MethodSource("anySourceProvider")
    void shouldThrowIllegalArgumentExceptionWhenConverterIsNull(Object source) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toZoneId(source, null), "A null Converter must cause IllegalArgumentException");
    }

    /* -------------------------------------------------------------
     *  Unsupported collection source
     * ------------------------------------------------------------- */
    @Test
    void shouldThrowIllegalArgumentExceptionForCollectionSource() {
        List<String> list = Arrays.asList("Europe/Paris");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toZoneId(list, converter), "Collections are not valid sources for ZoneId conversion");
    }
}