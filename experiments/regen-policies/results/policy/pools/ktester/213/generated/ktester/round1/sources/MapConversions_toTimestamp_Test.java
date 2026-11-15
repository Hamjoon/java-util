package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.sql.Timestamp;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toTimestamp_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter class requires a ConverterOptions instance.
        // Using the default options (empty constructor) provides the standard behaviour.
//         converter = new Converter(new ConverterOptions());
    }

    /**
     * Provides a stream of arguments for direct conversion tests.
     * Each argument consists of:
     *   - the source object {@code from}
     *   - the expected {@link Timestamp} (epoch millis)
     */
    private static Stream<Arguments> directConversionProvider() {
        // 2021‑09‑01T00:00:00Z
        long epoch = 1_630_454_400_000L;
        return Stream.of(// already a Timestamp – should be returned unchanged (or equal)
        Arguments.of(new Timestamp(epoch), new Timestamp(epoch)), // java.util.Date
        Arguments.of(new Date(epoch), new Timestamp(epoch)), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(epoch), new Timestamp(epoch)), // Long (epoch millis)
        Arguments.of(Long.valueOf(epoch), new Timestamp(epoch)), // String in ISO‑8601 format
        Arguments.of("2021-09-01T00:00:00Z", new Timestamp(epoch)), // Calendar (UTC)
        Arguments.of(createUtcCalendar(epoch), new Timestamp(epoch)));
    }

    private static Calendar createUtcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    private static Stream<Arguments> mapKeyConversionProvider() {
        // 2021‑09‑01T00:00:00Z
        long epoch = 1_630_454_400_000L;
        Timestamp ts = new Timestamp(epoch);
        return Stream.of(// key "timestamp" with a Timestamp value
        Arguments.of(Map.of("timestamp", ts), ts), // key "value" with a numeric epoch millis
        Arguments.of(Map.of("value", epoch), ts), // key "_v" with an ISO‑8601 string
        Arguments.of(Map.of("_v", "2021-09-01T00:00:00Z"), ts), // key "epochMillis" with a Long
        Arguments.of(Map.of("epochMillis", epoch), ts));
    }

    @Test
    void toTimestamp_givenTimestamp_returnsSameInstance() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp result = MapConversions.toTimestamp(now, converter);
        Assertions.assertSame(now, result, "When the source is already a Timestamp the same instance should be returned.");
    }

    @Test
    void toTimestamp_givenMapWithTimestampKey_returnsTimestamp() {
        Timestamp expected = new Timestamp(1_600_000_000_000L);
        Map<String, Object> map = new HashMap<>();
        // key defined in TIMESTAMP_KEYS
        map.put("timestamp", expected);
        Timestamp result = MapConversions.toTimestamp(map, converter);
        Assertions.assertEquals(expected, result, "Map entry with key \"timestamp\" should be used as the source value.");
    }

    @Test
    void toTimestamp_givenMapWithValueKey_convertsLongToTimestamp() {
        // 2021‑05‑05T00:00:00Z
        long epochMillis = 1_620_000_000_000L;
        Timestamp expected = new Timestamp(epochMillis);
        Map<String, Object> map = new HashMap<>();
        // another key from TIMESTAMP_KEYS
        map.put("value", epochMillis);
        Timestamp result = MapConversions.toTimestamp(map, converter);
        Assertions.assertEquals(expected, result, "Map entry with key \"value\" containing a Long should be converted to Timestamp.");
    }

    @Test
    void toTimestamp_givenMapWithVKey_convertsLongToTimestamp() {
        // 2021‑08‑15T06:13:20Z
        long epochMillis = 1_630_000_000_000L;
        Timestamp expected = new Timestamp(epochMillis);
        Map<String, Object> map = new HashMap<>();
        // the short form key
        map.put("_v", epochMillis);
        Timestamp result = MapConversions.toTimestamp(map, converter);
        Assertions.assertEquals(expected, result, "Map entry with key \"_v\" containing a Long should be converted to Timestamp.");
    }

    @Test
    void toTimestamp_givenMapWithEpochMillisKey_convertsLongToTimestamp() {
        // 2022‑01‑01T04:53:20Z
        long epochMillis = 1_640_000_000_000L;
        Timestamp expected = new Timestamp(epochMillis);
        Map<String, Object> map = new HashMap<>();
        // the last key in TIMESTAMP_KEYS
        map.put("epochMillis", epochMillis);
        Timestamp result = MapConversions.toTimestamp(map, converter);
        Assertions.assertEquals(expected, result, "Map entry with key \"epochMillis\" should be converted to Timestamp.");
    }

    @Test
    void toTimestamp_givenNull_returnsNull() {
        Timestamp result = MapConversions.toTimestamp(null, converter);
        Assertions.assertNull(result, "Converting a null source should yield null.");
    }

    @Test
    void toTimestamp_givenUnsupportedMap_returnsNull() {
        Map<String, Object> map = new HashMap<>();
        map.put("unrelatedKey", "someValue");
        Timestamp result = MapConversions.toTimestamp(map, converter);
        Assertions.assertNull(result, "Map without any of the recognised timestamp keys should result in null.");
    }

    @ParameterizedTest(name = "[{index}] Convert {0} to Timestamp")
    @MethodSource("directConversionProvider")
    void toTimestamp_directConversions(Object from, Timestamp expected) {
        Timestamp result = MapConversions.toTimestamp(from, converter);
        Assertions.assertNotNull(result, "Result should never be null for a supported source");
        Assertions.assertEquals(expected.getTime(), result.getTime(), () -> "Converted Timestamp does not match expected epoch millis for source: " + from);
    }

    @ParameterizedTest(name = "[{index}] Map conversion with key {0}")
    @MethodSource("mapKeyConversionProvider")
    void toTimestamp_mapBasedConversions(Map<String, Object> map, Timestamp expected) {
        Timestamp result = MapConversions.toTimestamp(map, converter);
        Assertions.assertNotNull(result, "Result should not be null when map contains a supported key");
        Assertions.assertEquals(expected.getTime(), result.getTime(), () -> "Timestamp derived from map does not match expected value for map: " + map);
    }

    @Test
    void toTimestamp_mapWithoutSupportedKeys_fallbackToStringParsing() {
        Map<String, Object> map = new HashMap<>();
        map.put("someOther", "2021-09-01T00:00:00Z");
        Timestamp result = MapConversions.toTimestamp(map, converter);
        Timestamp expected = new Timestamp(Instant.parse("2021-09-01T00:00:00Z").toEpochMilli());
        Assertions.assertNotNull(result, "Fallback conversion should produce a Timestamp");
        Assertions.assertEquals(expected.getTime(), result.getTime(), "Fallback parsing of string value failed to produce the correct Timestamp");
    }

    @Test
    void toTimestamp_emptyMap_throwsException() {
        Map<String, Object> empty = Collections.emptyMap();
        Assertions.assertThrows(RuntimeException.class, () -> MapConversions.toTimestamp(empty, converter), "Converting an empty map should result in an exception");
    }

    @Test
    void toTimestamp_givenBoolean_throwsException() {
        Assertions.assertThrows(RuntimeException.class, () -> MapConversions.toTimestamp(Boolean.TRUE, converter), "Unsupported source type (Boolean) must cause a conversion exception");
    }

    @Test
    void toTimestamp_givenCollection_throwsException() {
        List<String> list = List.of("2021-09-01T00:00:00Z");
        Assertions.assertThrows(RuntimeException.class, () -> MapConversions.toTimestamp(list, converter), "Passing a collection (non‑Map) should trigger a conversion exception");
    }

    @Test
    void toTimestamp_mapWithSupportedKeyButUnsupportedValue_throwsException() {
        Map<String, Object> map = Map.of("timestamp", new Object());
        Assertions.assertThrows(RuntimeException.class, () -> MapConversions.toTimestamp(map, converter), "Map with supported key but non‑convertible value should cause an exception");
    }

    @Test
    void toTimestamp_mapWithInvalidDateString_throwsException() {
        Map<String, Object> map = Map.of("timestamp", "invalid-date-string");
        Assertions.assertThrows(RuntimeException.class, () -> MapConversions.toTimestamp(map, converter), "Unparsable date string should result in a parsing exception");
    }

    @Test
    void toTimestamp_mapWithNonNumericEpochMillis_throwsException() {
        Map<String, Object> map = Map.of("epochMillis", "not-a-number");
        Assertions.assertThrows(RuntimeException.class, () -> MapConversions.toTimestamp(map, converter), "Non‑numeric epochMillis value should cause a NumberFormatException or similar");
    }

    @Test
    void toTimestamp_mapWithNullValue_throwsException() {
        Map<String, Object> map = Map.of("value", null);
        Assertions.assertThrows(RuntimeException.class, () -> MapConversions.toTimestamp(map, converter), "Null value for a supported key must raise an exception");
    }

    @Test
    void toTimestamp_stringBuilderUnsupported_throwsException() {
        StringBuilder sb = new StringBuilder("2021-09-01T00:00:00Z");
        Assertions.assertThrows(RuntimeException.class, () -> MapConversions.toTimestamp(sb, converter), "StringBuilder is not a supported source type");
    }

    @Test
    void toTimestamp_mapWithNullKey_throwsException() {
        Map<String, Object> map = new HashMap<>();
        map.put(null, 1_630_454_400_000L);
        Assertions.assertThrows(RuntimeException.class, () -> MapConversions.toTimestamp(map, converter), "Map containing a null key should result in a conversion exception");
    }

    @Test
    void toTimestamp_timestampOverflow_throwsException() {
        // Long.MAX_VALUE exceeds the range of java.sql.Timestamp (which uses milliseconds since epoch).
        Timestamp overflowTs = new Timestamp(Long.MAX_VALUE);
        Map<String, Object> map = Map.of("timestamp", overflowTs);
        Assertions.assertThrows(RuntimeException.class, () -> MapConversions.toTimestamp(map, converter), "Overflowing Timestamp value should trigger an exception");
    }

    @Test
    void toTimestamp_localDateTimeAssumedUtc() {
        LocalDateTime ldt = LocalDateTime.of(2021, 9, 1, 0, 0, 0);
        Timestamp expected = Timestamp.from(ldt.toInstant(ZoneOffset.UTC));
        Timestamp result = MapConversions.toTimestamp(ldt, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "LocalDateTime should be interpreted as UTC");
    }

    @Test
    void toTimestamp_zonedDateTimeWithOffset() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 9, 1, 0, 0, 0, 0, ZoneId.of("America/New_York"));
        Timestamp expected = Timestamp.from(zdt.toInstant());
        Timestamp result = MapConversions.toTimestamp(zdt, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "ZonedDateTime should be converted to the corresponding UTC instant");
    }

    @Test
    void toTimestamp_offsetDateTimeWithNegativeOffset() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 9, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-4));
        Timestamp expected = Timestamp.from(odt.toInstant());
        Timestamp result = MapConversions.toTimestamp(odt, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "OffsetDateTime with –04:00 offset should be converted to the same instant in UTC");
    }

    @Test
    void toTimestamp_sqlDateTreatedAsMidnightUtc() {
        java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.of(2021, 9, 1));
        Timestamp expected = Timestamp.from(sqlDate.toInstant());
        Timestamp result = MapConversions.toTimestamp(sqlDate, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "java.sql.Date should be treated as midnight UTC of the given date");
    }

    @Test
    void toTimestamp_localDateStartOfDayUtc() {
        LocalDate ld = LocalDate.of(2021, 9, 1);
        Timestamp expected = Timestamp.from(ld.atStartOfDay(ZoneOffset.UTC).toInstant());
        Timestamp result = MapConversions.toTimestamp(ld, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "LocalDate should be interpreted as start of day UTC");
    }

    @Test
    void toTimestamp_localTimeOnEpochDate() {
        LocalTime lt = LocalTime.of(12, 34, 56);
        long expectedMillis = lt.toSecondOfDay() * 1000L;
        Timestamp expected = new Timestamp(expectedMillis);
        Timestamp result = MapConversions.toTimestamp(lt, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "LocalTime should be interpreted as time‑of‑day on the epoch date");
    }

    @Test
    void toTimestamp_yearMonthFirstDayUtc() {
        YearMonth ym = YearMonth.of(2021, 9);
        Timestamp expected = Timestamp.from(ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        Timestamp result = MapConversions.toTimestamp(ym, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "YearMonth should map to the first day of the month at midnight UTC");
    }

    @Test
    void toTimestamp_yearFirstDayUtc() {
        Year y = Year.of(2021);
        Timestamp expected = Timestamp.from(y.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant());
        Timestamp result = MapConversions.toTimestamp(y, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "Year should map to Jan 1st at midnight UTC");
    }

    @Test
    void toTimestamp_monthDayCurrentYearUtc() {
        MonthDay md = MonthDay.of(9, 1);
        LocalDate assumed = LocalDate.of(Year.now().getValue(), md.getMonth(), md.getDayOfMonth());
        Timestamp expected = Timestamp.from(assumed.atStartOfDay(ZoneOffset.UTC).toInstant());
        Timestamp result = MapConversions.toTimestamp(md, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "MonthDay should be interpreted as the given month‑day in the current year at midnight UTC");
    }

    @Test
    void toTimestamp_offsetTimeOnEpochDate() {
        OffsetTime ot = OffsetTime.of(12, 0, 0, 0, ZoneOffset.ofHours(2));
        long expectedMillis = LocalTime.of(10, 0).toSecondOfDay() * 1000L;
        Timestamp expected = new Timestamp(expectedMillis);
        Timestamp result = MapConversions.toTimestamp(ot, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "OffsetTime should be applied to the epoch date with its offset");
    }

    @Test
    void toTimestamp_zoneOffsetConvertedToInstant() {
        ZoneOffset zo = ZoneOffset.ofHoursMinutes(5, 30);
        long expectedMillis = -((5 * 60 + 30) * 60 * 1000L);
        Timestamp expected = new Timestamp(expectedMillis);
        Timestamp result = MapConversions.toTimestamp(zo, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "ZoneOffset should be converted to the instant representing that offset on the epoch date");
    }

    @Test
    void toTimestamp_zoneIdUtc() {
        ZoneId zid = ZoneId.of("UTC");
        Timestamp expected = new Timestamp(0L);
        Timestamp result = MapConversions.toTimestamp(zid, converter);
        Assertions.assertEquals(expected.getTime(), result.getTime(), "ZoneId \"UTC\" should map to the epoch instant (0 ms)");
    }

    @Test
    void toTimestamp_nullInput_returnsNull() {
        Assertions.assertNull(MapConversions.toTimestamp(null, converter), "Converting a null source should yield null");
    }
}