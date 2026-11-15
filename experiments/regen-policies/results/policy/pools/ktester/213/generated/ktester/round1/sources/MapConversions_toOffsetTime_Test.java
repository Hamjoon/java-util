package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Method;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;
import java.nio.ByteBuffer;

class MapConversions_toOffsetTime_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter class now requires a ConverterOptions instance.
//         converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// identity conversion
        Arguments.of("Identity conversion when source is already OffsetTime", OffsetTime.of(10, 15, 30, 0, ZoneOffset.ofHours(2)), OffsetTime.of(10, 15, 30, 0, ZoneOffset.ofHours(2))), // string parsing
        Arguments.of("String representation should be parsed", "10:15:30+02:00", OffsetTime.of(10, 15, 30, 0, ZoneOffset.ofHours(2))), // map with "offsetTime" key (string value)
        Arguments.of("Map with key 'offsetTime' containing a string", mapOf("offsetTime", "12:00:00Z"), OffsetTime.of(12, 0, 0, 0, ZoneOffset.UTC)), // map with "value" key (already an OffsetTime)
        Arguments.of("Map with key 'value' containing an OffsetTime instance", mapOf("value", OffsetTime.of(8, 30, 15, 0, ZoneOffset.ofHours(-5))), OffsetTime.of(8, 30, 15, 0, ZoneOffset.ofHours(-5))), // map with legacy "_v" key (string)
        Arguments.of("Map with legacy key '_v' containing a string", mapOf("_v", "23:59:59+00:00"), OffsetTime.of(23, 59, 59, 0, ZoneOffset.UTC)), // OffsetDateTime conversion
        Arguments.of("OffsetDateTime should be converted to its OffsetTime component", OffsetDateTime.of(2023, 5, 10, 14, 30, 45, 123_000_000, ZoneOffset.ofHoursMinutes(5, 30)), OffsetTime.of(14, 30, 45, 123_000_000, ZoneOffset.ofHoursMinutes(5, 30))), // ZonedDateTime conversion – offset derived from zone at the instant
        Arguments.of("ZonedDateTime should be converted using the zone's current offset", ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("Europe/Paris")), OffsetTime.of(23, 59, 59, 0, ZoneId.of("Europe/Paris").getRules().getOffset(Instant.parse("2022-12-31T23:59:59Z")))), // Minimum allowed offset (-18:00)
        Arguments.of("String with minimum allowed offset -18:00", "00:00:00-18:00", OffsetTime.of(0, 0, 0, 0, ZoneOffset.ofHours(-18))), // Maximum allowed offset (+18:00) with nanosecond precision
        Arguments.of("String with maximum offset +18:00 and full nanoseconds", "23:59:59.999999999+18:00", OffsetTime.of(23, 59, 59, 999_999_999, ZoneOffset.ofHours(18))), // Map with "offsetTime" key whose value is null → null result
        Arguments.of("Map with key 'offsetTime' but null value should yield null", mapOf("offsetTime", null), null), // Ambiguous keys – first non‑null according to OFFSET_TIME_KEYS order
        Arguments.of("Map with null 'value' and valid '_v' – should use '_v'", mapOf("value", null, "_v", "10:00:00+02:00"), OffsetTime.of(10, 0, 0, 0, ZoneOffset.ofHours(2))), // Both "value" and "offsetTime" present – offsetTime wins per key order
        Arguments.of("Map with both 'value' and 'offsetTime' – offsetTime takes precedence", mapOf("value", "11:11:11+01:00", "offsetTime", "12:12:12+02:00"), OffsetTime.of(12, 12, 12, 0, ZoneOffset.ofHours(2))));
    }

    private static Stream<Arguments> provideConversionExceptionCases() {
        return Stream.of(// Unsupported source type (Integer)
        Arguments.of("Unsupported source type Integer should throw ConversionException", 12345), // Map without any OFFSET_TIME_KEYS
        Arguments.of("Empty map (no keys) should throw ConversionException", Collections.emptyMap()), // Map with supported key but value of unsupported type (Integer)
        Arguments.of("Map with key 'offsetTime' whose value is Integer should throw ConversionException", mapOf("offsetTime", 12345)), // CharSequence other than String (StringBuilder)
        Arguments.of("StringBuilder source should throw ConversionException", new StringBuilder("12:00:00+01:00")), // ByteBuffer source
        Arguments.of("ByteBuffer source should throw ConversionException", ByteBuffer.wrap("12:00:00+01:00".getBytes())), // LocalTime (no offset) unsupported
        Arguments.of("LocalTime without offset should throw ConversionException", LocalTime.of(15, 45, 30)), // java.util.Date lacks offset
        Arguments.of("java.util.Date should throw ConversionException", Date.from(Instant.parse("2023-01-01T12:00:00Z"))), // Year object unsupported
        Arguments.of("Year object should throw ConversionException", Year.of(2025)), // Map with unknown key only
        Arguments.of("Map with unknown key only should throw ConversionException", mapOf("unknownKey", "12:00:00+01:00")));
    }

    private static Stream<Arguments> provideDateTimeParseExceptionCases() {
        return Stream.of(// malformed time string
        Arguments.of("Malformed time string should cause DateTimeParseException", "invalid-time-string"), // Invalid hour 24
        Arguments.of("Hour value 24 is invalid and should cause DateTimeParseException", "24:00:00+00:00"), // Invalid minute 60
        Arguments.of("Minute value 60 is invalid and should cause DateTimeParseException", "12:60:00+01:00"), // Invalid second 60 (leap‑second not supported)
        Arguments.of("Second value 60 is invalid and should cause DateTimeParseException", "12:30:60+01:00"), // Offset minutes > 59
        Arguments.of("Offset minutes > 59 should cause DateTimeParseException", "10:15:30+02:60"), // Offset hour > +18
        Arguments.of("Offset hour > +18 should cause DateTimeParseException", "10:15:30+19:00"), // Missing offset
        Arguments.of("String without offset should cause DateTimeParseException", "10:15:30"), // Empty string
        Arguments.of("Empty string should cause DateTimeParseException", ""), // Map with key but unparsable string value
        Arguments.of("Map with unparsable string value should cause DateTimeParseException", mapOf("offsetTime", "not-a-time")));
    }

    @Test
    void toOffsetTime_givenOffsetTime_returnsSameInstance() {
        OffsetTime now = OffsetTime.now();
        OffsetTime result = MapConversions.toOffsetTime(now, converter);
        assertEquals(now, result, "When the source is already an OffsetTime it should be returned unchanged");
    }

    @Test
    void toOffsetTime_givenString_parsesCorrectly() {
        String source = "10:15:30+02:00";
        OffsetTime expected = OffsetTime.parse(source);
        OffsetTime result = MapConversions.toOffsetTime(source, converter);
        assertEquals(expected, result, "String representation of an OffsetTime should be parsed");
    }

    @Test
    void toOffsetTime_givenMapWithOffsetTimeKey_parsesValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("offsetTime", "12:30:45+03:00");
        OffsetTime expected = OffsetTime.parse("12:30:45+03:00");
        OffsetTime result = MapConversions.toOffsetTime(map, converter);
        assertEquals(expected, result, "Map with key 'offsetTime' should be used for conversion");
    }

    @Test
    void toOffsetTime_givenMapWithValueKey_parsesValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", "08:00:00Z");
        OffsetTime expected = OffsetTime.parse("08:00:00Z");
        OffsetTime result = MapConversions.toOffsetTime(map, converter);
        assertEquals(expected, result, "Map with key 'value' should be used for conversion");
    }

    @Test
    void toOffsetTime_givenMapWithVKey_parsesValue() {
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "09:15:00-05:00");
        OffsetTime expected = OffsetTime.parse("09:15:00-05:00");
        OffsetTime result = MapConversions.toOffsetTime(map, converter);
        assertEquals(expected, result, "Map with key '_v' should be used for conversion");
    }

    @Test
    void toOffsetTime_givenNull_returnsNull() {
        assertNull(MapConversions.toOffsetTime(null, converter), "Null source should result in null OffsetTime");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSuccessfulConversions")
    void toOffsetTime_successfulConversions(String description, Object from, OffsetTime expected) {
        OffsetTime result = MapConversions.toOffsetTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideDateTimeParseExceptionCases")
    void toOffsetTime_throwsDateTimeParseException(String description, Object from) {
        assertThrows(DateTimeParseException.class, () -> MapConversions.toOffsetTime(from, converter), description);
    }

    @Test
    void toOffsetTime_nullConverter_throwsNullPointerException() {
        OffsetTime source = OffsetTime.now();
        assertThrows(NullPointerException.class, () -> MapConversions.toOffsetTime(source, null), "Passing a null Converter must result in NullPointerException");
    }

    @Test
    void toOffsetTime_nullSource_returnsNull() {
        assertNull(MapConversions.toOffsetTime(null, converter), "Null source should result in null OffsetTime");
    }

    /**
     * Utility method to create a {@link Map} from alternating key/value arguments.
     *
     * @param entries key/value pairs (must be an even number of arguments)
     * @return a mutable {@link HashMap} containing the supplied entries
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            String key = (String) entries[i];
            Object value = entries[i + 1];
            map.put(key, value);
        }
        return map;
    }
}