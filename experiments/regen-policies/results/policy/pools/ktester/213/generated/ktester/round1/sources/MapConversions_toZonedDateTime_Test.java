package com.cedarsoftware.util.convert;

import java.time.*;
import java.util.*;
import java.time.format.DateTimeParseException;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class MapConversions_toZonedDateTime_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    /**
     * Original JVM default zone – restored after the suite finishes.
     */
    private static ZoneId originalDefaultZone;

    /**
     * Original JVM default time zone – restored after the suite finishes.
     */
    private static TimeZone originalDefaultTimeZone;

//     @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original default time zone and force a deterministic zone for the tests.
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // The Converter class requires a ConverterOptions instance – the no‑arg constructor
        // creates a default instance internally.
//         converter = new Converter();
    }

//     @AfterAll
    static void teardownAfterAll() {
        // Restore the original default time zone so we do not affect other test suites.
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    /**
     * Verify that a ZonedDateTime passed directly is returned unchanged.
     */
//     @Test
//     void shouldReturnSameZonedDateTimeWhenSourceIsZonedDateTime() {
//         ZonedDateTime source = ZonedDateTime.of(2023, 5, 17, 12, 30, 45, 0, ZoneId.of("Europe/Paris"));
//         ZonedDateTime result = MapConversions.toZonedDateTime(source, converter);
//         assertEquals("The conversion should return the original ZonedDateTime instance", source, result);
//     }

    /**
     * Convert a map containing an ISO‑8601 string under the key "zonedDateTime".
     */
//     @Test
//     void shouldConvertMapWithIsoString() {
//         String iso = "2023-01-02T10:15:30+01:00[Europe/Paris]";
//         Map<String, Object> map = new HashMap<>();
//         map.put("zonedDateTime", iso);
//         map.put("value", iso);
//         map.put("_v", iso);
//         ZonedDateTime expected = ZonedDateTime.parse(iso);
//         ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * Convert a map that contains the epoch‑millis representation.
     */
//     @Test
//     void shouldConvertMapWithEpochMillis() {
//         // 2022-01-01T12:15:30Z
//         long epochMillis = 1_640_995_730_000L;
//         Map<String, Object> map = new HashMap<>();
//         map.put("epochMillis", epochMillis);
//         ZonedDateTime expected = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("UTC"));
//         ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * Convert a java.util.Date (uses the default zone which we forced to UTC).
     */
//     @Test
//     void shouldConvertDate() {
//         Date date = Date.from(Instant.parse("2021-12-31T23:59:59Z"));
//         ZonedDateTime expected = date.toInstant().atZone(ZoneId.of("UTC"));
//         ZonedDateTime actual = MapConversions.toZonedDateTime(date, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * Convert a map that supplies a LocalDateTime together with a zone identifier.
     */
//     @Test
//     void shouldConvertMapWithLocalDateTimeAndZone() {
//         LocalDateTime ldt = LocalDateTime.of(2023, Month.MARCH, 15, 9, 45, 0);
//         String zoneId = "America/New_York";
//         Map<String, Object> map = new HashMap<>();
//         map.put("localDateTime", ldt);
//         map.put("zone", zoneId);
//         ZonedDateTime expected = ldt.atZone(ZoneId.of(zoneId));
//         ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * Convert an Instant (should become a ZonedDateTime in UTC because of the default zone).
     */
//     @Test
//     void shouldConvertInstant() {
//         Instant instant = Instant.parse("2020-06-30T08:20:15Z");
//         ZonedDateTime expected = instant.atZone(ZoneId.of("UTC"));
//         ZonedDateTime actual = MapConversions.toZonedDateTime(instant, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * When the source object is already a ZonedDateTime the method should return the same value.
     */
//     @Test
//     void testToZonedDateTimeFromZonedDateTime() {
//         ZonedDateTime source = ZonedDateTime.parse("2023-03-25T10:15:30+01:00[Europe/Paris]");
//         ZonedDateTime result = MapConversions.toZonedDateTime(source, converter);
//         assertEquals(source, result);
//     }

    /**
     * ISO‑8601 string should be parsed into the corresponding ZonedDateTime.
     */
//     @Test
//     void testToZonedDateTimeFromStringISO() {
//         String iso = "2023-03-25T10:15:30+01:00[Europe/Paris]";
//         ZonedDateTime expected = ZonedDateTime.parse(iso);
//         ZonedDateTime actual = MapConversions.toZonedDateTime(iso, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * Map containing the key 'zonedDateTime' with a ZonedDateTime value should be converted correctly.
     */
//     @Test
//     void testToZonedDateTimeFromMapWithZonedDateTimeKey() {
//         String iso = "2023-03-25T10:15:30+01:00[Europe/Paris]";
//         Map<String, Object> map = new HashMap<>();
//         map.put("zonedDateTime", iso);
//         ZonedDateTime expected = ZonedDateTime.parse(iso);
//         ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * Map with key 'value' holding an ISO‑8601 string should be parsed to ZonedDateTime.
     */
//     @Test
//     void testToZonedDateTimeFromMapWithValueKeyString() {
//         String iso = "2023-03-25T10:15:30+01:00[Europe/Paris]";
//         Map<String, Object> map = new HashMap<>();
//         map.put("value", iso);
//         ZonedDateTime expected = ZonedDateTime.parse(iso);
//         ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * Map with key '_v' holding epoch‑millis should be converted to ZonedDateTime in UTC.
     */
//     @Test
//     void testToZonedDateTimeFromMapWithVKeyLong() {
//         // 2023-03-25T10:15:30Z
//         long epochMillis = 1_679_740_530_000L;
//         Map<String, Object> map = new HashMap<>();
//         map.put("_v", epochMillis);
//         ZonedDateTime expected = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("UTC"));
//         ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * Map with key 'epochMillis' holding epoch‑millis should be converted to ZonedDateTime in UTC.
     */
//     @Test
//     void testToZonedDateTimeFromMapWithEpochMillisKey() {
//         // 2023-03-25T10:15:30Z
//         long epochMillis = 1_679_740_530_000L;
//         Map<String, Object> map = new HashMap<>();
//         map.put("epochMillis", epochMillis);
//         ZonedDateTime expected = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("UTC"));
//         ZonedDateTime actual = MapConversions.toZonedDateTime(map, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * Long value representing epoch‑millis should be converted to ZonedDateTime in UTC.
     */
//     @Test
//     void testToZonedDateTimeFromLongEpochMillis() {
//         // 2023-03-25T10:15:30Z
//         long epochMillis = 1_679_740_530_000L;
//         ZonedDateTime expected = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.of("UTC"));
//         ZonedDateTime actual = MapConversions.toZonedDateTime(epochMillis, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * Instant should be converted to ZonedDateTime with UTC zone.
     */
//     @Test
//     void testToZonedDateTimeFromInstant() {
//         Instant instant = Instant.parse("2023-03-25T10:15:30Z");
//         ZonedDateTime expected = instant.atZone(ZoneId.of("UTC"));
//         ZonedDateTime actual = MapConversions.toZonedDateTime(instant, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * java.util.Date should be converted to ZonedDateTime in UTC.
     */
//     @Test
//     void testToZonedDateTimeFromDate() {
//         Date date = Date.from(Instant.parse("2023-03-25T10:15:30.000Z"));
//         ZonedDateTime expected = date.toInstant().atZone(ZoneId.of("UTC"));
//         ZonedDateTime actual = MapConversions.toZonedDateTime(date, converter);
//         assertEquals(expected, actual);
//     }

    /**
     * Null input should result in null output (no exception).
     */
//     @Test
//     void testToZonedDateTimeFromNull() {
//         assertNull("Converting a null source should return null", MapConversions.toZonedDateTime(null, converter));
//     }

    /**
     * A non‑parseable string should cause a DateTimeParseException.
     */
//     @Test
//     void testToZonedDateTimeFromInvalidString() {
//         String bad = "not-a-date";
//         try {
//             MapConversions.toZonedDateTime(bad, converter);
//             fail("Parsing an invalid date string must throw DateTimeParseException");
//         } catch (DateTimeParseException e) {
//             // expected
//         }
//     }

    /**
     * Map without any recognized keys should result in IllegalArgumentException.
     */
//     @Test
//     void testToZonedDateTimeFromInvalidMap() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("unknownKey", "12345");
//         try {
//             MapConversions.toZonedDateTime(map, converter);
//             fail("A map lacking known keys must trigger IllegalArgumentException");
//         } catch (IllegalArgumentException e) {
//             // expected
//         }
//     }

    /**
     * Passing a null Converter should cause a NullPointerException because the method dereferences the converter.
     */
//     @Test
//     void testToZonedDateTimeWithNullConverter() {
//         String iso = "2023-03-25T10:15:30+01:00[Europe/Paris]";
//         try {
//             MapConversions.toZonedDateTime(iso, null);
//             fail("Null Converter should raise NullPointerException");
//         } catch (NullPointerException e) {
//             // expected
//         }
//     }

    /**
     * A Boolean value is not a supported source type; the method should throw IllegalArgumentException.
     */
//     @Test
//     void testToZonedDateTimeUnsupportedType() {
//         try {
//             MapConversions.toZonedDateTime(Boolean.TRUE, converter);
//             fail("Unsupported source type must raise IllegalArgumentException");
//         } catch (IllegalArgumentException e) {
//             // expected
//         }
//     }

    /**
     * Map contains the recognized key 'epochMillis' but the associated value is a non‑numeric string; conversion should fail with NumberFormatException.
     */
//     @Test
//     void testToZonedDateTimeMapEpochMillisString() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("epochMillis", "not-a-number");
//         try {
//             MapConversions.toZonedDateTime(map, converter);
//             fail("Non‑numeric epochMillis should raise NumberFormatException");
//         } catch (NumberFormatException e) {
//             // expected
//         }
//     }

    /**
     * Map contains the key 'epochMillis' with a null value; the method should reject it and throw IllegalArgumentException.
     */
//     @Test
//     void testToZonedDateTimeMapEpochMillisNull() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("epochMillis", null);
//         try {
//             MapConversions.toZonedDateTime(map, converter);
//             fail("Null epochMillis value must raise IllegalArgumentException");
//         } catch (IllegalArgumentException e) {
//             // expected
//         }
//     }

    /**
     * ISO‑8601 string contains an unknown zone identifier; parsing should raise a DateTimeException.
     */
//     @Test
//     void testToZonedDateTimeStringInvalidZone() {
//         String badZone = "2023-03-25T10:15:30+01:00[Invalid/Zone]";
//         try {
//             MapConversions.toZonedDateTime(badZone, converter);
//             fail("Invalid zone identifier should raise DateTimeException");
//         } catch (DateTimeException e) {
//             // expected
//         }
//     }

    /**
     * A Double representing epoch‑millis is not an accepted numeric type; the conversion should throw IllegalArgumentException.
     */
//     @Test
//     void testToZonedDateTimeNumberWrongType() {
//         Double epochDouble = 1_679_740_530_000.0;
//         try {
//             MapConversions.toZonedDateTime(epochDouble, converter);
//             fail("Double epochMillis should raise IllegalArgumentException");
//         } catch (IllegalArgumentException e) {
//             // expected
//         }
//     }

    /**
     * Passing a Calendar instance (or a map mimicking one) that is not explicitly handled should result in IllegalArgumentException.
     */
//     @Test
//     void testToZonedDateTimeFromCalendarUnsupported() {
//         Map<String, Object> fakeCalendar = new HashMap<>();
//         fakeCalendar.put("java.util.Calendar", "2023-03-25T10:15:30Z");
//         try {
//             MapConversions.toZonedDateTime(fakeCalendar, converter);
//             fail("Unsupported Calendar representation must raise IllegalArgumentException");
//         } catch (IllegalArgumentException e) {
//             // expected
//         }
//     }
}