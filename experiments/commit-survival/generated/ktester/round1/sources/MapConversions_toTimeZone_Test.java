package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toTimeZone_Test {

    /**
     * Shared {@link Converter} instance used for all tests.
     */
    private static Converter converter;

    /**
     * A map that mimics the structure expected by {@code toTimeZone}.
     */
    private Map<String, Object> zoneMap;

    /**
     * Shared {@link Converter} instance used for the majority of tests.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor
        // provides an empty options set which is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        zoneMap = new HashMap<>();
        // The key name is defined in MapConversions as ZONE = "zone"
        zoneMap.put("zone", "GMT");
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable static state to clean up; method kept for symmetry.
        zoneMap.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Explicitly release the static converter reference.
        converter = null;
    }

    private static Stream<Arguments> stringZoneProvider() {
        return Stream.of(Arguments.of("Europe/Paris", "Europe/Paris"), Arguments.of("GMT+02:00", "GMT+02:00"), Arguments.of("  America/Chicago  ", "America/Chicago"), // case‑insensitive handling
        Arguments.of("utc", "UTC"));
    }

    private static Stream<Arguments> invalidZoneValueProvider() {
        return Stream.of(Arguments.of(true, ClassCastException.class), Arguments.of(new Object(), ClassCastException.class));
    }

    /**
     * Verify that a map containing a valid zone ID is converted to the correct {@link TimeZone}.
     */
    @Test
    void toTimeZone_fromValidMap_returnsExpectedTimeZone() {
        // given – zoneMap already contains {"zone":"GMT"} from @BeforeEach
        // when
        TimeZone tz = MapConversions.toTimeZone(zoneMap, converter);
        // then
        assertNotNull(tz, "Conversion should produce a non‑null TimeZone");
        assertEquals("GMT", tz.getID(), "TimeZone ID should match the supplied zone string");
    }

    /**
     * Verify that passing {@code null} as the source object yields {@code null}.
     */
    @Test
    void toTimeZone_fromNull_returnsNull() {
        // when
        TimeZone tz = MapConversions.toTimeZone(null, converter);
        // then
        assertNull(tz, "Conversion of null should result in null");
    }

    /**
     * Verify that a map missing the required key returns {@code null}.
     */
    @Test
    void toTimeZone_fromMapMissingKey_returnsNull() {
        // given – map without the required "zone" entry
        Map<String, Object> emptyMap = new HashMap<>();
        // when
        TimeZone tz = MapConversions.toTimeZone(emptyMap, converter);
        // then
        assertNull(tz, "Conversion should return null when the required key is absent");
    }

    /**
     * Verify that a map with a non‑String zone value is still handled (e.g., a TimeZone instance).
     */
    @Test
    void toTimeZone_fromMapWithTimeZoneInstance_returnsSameInstance() {
        // given
        TimeZone expected = TimeZone.getTimeZone("UTC");
        zoneMap.put("zone", expected);
        // when
        TimeZone actual = MapConversions.toTimeZone(zoneMap, converter);
        // then
        assertSame(expected, actual, "When the map already contains a TimeZone, the same instance should be returned");
    }

    @Test
    void toTimeZone_fromDirectTimeZoneInstance_returnsSameInstance() {
        // given
        TimeZone source = TimeZone.getTimeZone("America/New_York");
        Converter identity = new IdentityConverter();
        // when
        TimeZone result = MapConversions.toTimeZone(source, identity);
        // then
        assertSame(source, result, "When the source is already a TimeZone the same instance must be returned");
    }

    @ParameterizedTest(name = "{index} ⇒ from=''{0}'' expects ID=''{1}''")
    @MethodSource("stringZoneProvider")
    void toTimeZone_fromStringZone_returnsExpectedTimeZone(String source, String expectedId) {
        // given
        Converter identity = new IdentityConverter();
        // when
        TimeZone tz = MapConversions.toTimeZone(source, identity);
        // then
        assertNotNull(tz);
        assertEquals(expectedId, tz.getID());
    }

    @Test
    void toTimeZone_fromMapContainingTimeZone_returnsSameInstance() {
        // given
        TimeZone expected = TimeZone.getTimeZone("Asia/Tokyo");
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("zone", expected);
        Converter identity = new IdentityConverter();
        // when
        TimeZone actual = MapConversions.toTimeZone(map, identity);
        // then
        assertSame(expected, actual);
    }

    @Test
    void toTimeZone_fromMapContainingString_returnsConvertedTimeZone() {
        // given
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("zone", "America/Los_Angeles");
        Converter identity = new IdentityConverter();
        // when
        TimeZone tz = MapConversions.toTimeZone(map, identity);
        // then
        assertNotNull(tz);
        assertEquals("America/Los_Angeles", tz.getID());
    }

    @Test
    void toTimeZone_fromMapWithExtraKeys_ignoresIrrelevantData() {
        // given
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("zone", "Europe/Berlin");
        map.put("foo", 123);
        map.put("bar", "baz");
        Converter identity = new IdentityConverter();
        // when
        TimeZone tz = MapConversions.toTimeZone(map, identity);
        // then
        assertNotNull(tz);
        assertEquals("Europe/Berlin", tz.getID());
    }

    @Test
    void toTimeZone_fromZoneIdWithSimpleConverter_convertsCorrectly() {
        // given
        ZoneId zoneId = ZoneId.of("Europe/London");
        Converter simple = new SimpleConverter();
        // when
        TimeZone tz = MapConversions.toTimeZone(zoneId, simple);
        // then
        assertNotNull(tz);
        assertEquals("Europe/London", tz.getID());
    }

    @Test
    void toTimeZone_fromZoneOffsetWithSimpleConverter_convertsCorrectly() {
        // given
        ZoneOffset offset = ZoneOffset.ofHoursMinutes(5, 30);
        Converter simple = new SimpleConverter();
        // when
        TimeZone tz = MapConversions.toTimeZone(offset, simple);
        // then
        assertNotNull(tz);
        assertEquals("GMT+05:30", tz.getID());
    }

    @Test
    void toTimeZone_fromMapWithNumericOffsetMinutes_convertsToGMTOffset() {
        // given
        Map<String, Object> map = new CompactLinkedMap<>();
        // 120 minutes = GMT+02:00
        map.put("zone", 120);
        Converter simple = new SimpleConverter();
        // when
        TimeZone tz = MapConversions.toTimeZone(map, simple);
        // then
        assertNotNull(tz);
        assertEquals("GMT+02:00", tz.getID());
    }

    @Test
    void toTimeZone_fromMapMissingKey_throwsIllegalArgumentException() {
        // given
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("foo", "bar");
        Converter identity = new IdentityConverter();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toTimeZone(map, identity), "Missing required 'zone' key should cause IllegalArgumentException");
    }

    @Test
    void toTimeZone_fromUnsupportedSource_throwsClassCastException() {
        // given
        Integer unsupported = 12345;
        Converter identity = new IdentityConverter();
        // when / then
        assertThrows(ClassCastException.class, () -> MapConversions.toTimeZone(unsupported, identity), "Unsupported source type should result in ClassCastException");
    }

    @Test
    void toTimeZone_fromNullSource_throwsNullPointerException() {
        // given
        Converter identity = new IdentityConverter();
        // when / then
        assertThrows(NullPointerException.class, () -> MapConversions.toTimeZone(null, identity), "Null source should trigger NullPointerException");
    }

    @Test
    void toTimeZone_withNullConverter_throwsNullPointerException() {
        // given
        String source = "UTC";
        // when / then
        assertThrows(NullPointerException.class, () -> MapConversions.toTimeZone(source, null), "Null converter should trigger NullPointerException");
    }

    @Test
    void toTimeZone_fromMapWithNullZoneValue_throwsIllegalArgumentException() {
        // given
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("zone", null);
        Converter identity = new IdentityConverter();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toTimeZone(map, identity), "Null value for required 'zone' key should cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "{index} ⇒ map value type {0} expects {1}")
    @MethodSource("invalidZoneValueProvider")
    void toTimeZone_fromMapWithInvalidZoneValue_throwsClassCastException(Object invalidValue, Class<? extends Throwable> expected) {
        // given
        Map<String, Object> map = new CompactLinkedMap<>();
        map.put("zone", invalidValue);
        Converter identity = new IdentityConverter();
        // when / then
        assertThrows(expected, () -> MapConversions.toTimeZone(map, identity), "Invalid zone value type should result in " + expected.getSimpleName());
    }
}
