package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.ZONED_DATE_TIME;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toLocalDate_Test {

    /**
     * Shared {@link Converter} used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a ConverterOptions instance – the default constructor gives us sensible defaults.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> provideLongEpochMillis() {
        return Stream.of(Arguments.of(0L, Instant.ofEpochMilli(0L).atZone(ZoneId.systemDefault()).toLocalDate()), // 2021‑06‑01T00:00:00Z
        Arguments.// 2021‑06‑01T00:00:00Z
        of(// 2021‑06‑01T00:00:00Z
        1622505600000L, Instant.ofEpochMilli(1622505600000L).atZone(ZoneId.systemDefault()).toLocalDate()));
    }

    /**
     * Verify conversion when the source object is already a {@link ZonedDateTime}.
     */
    @Test
    void toLocalDate_fromZonedDateTime() {
        // given
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 15, 10, 30, 0, 0, // +02:00 in summer
        ZoneId.of("Europe/Paris"));
        // when
        LocalDate result = ZonedDateTimeConversions.toLocalDate(source, converter);
        // then
        assertEquals(LocalDate.of(2023, 5, 15), result, "The LocalDate extracted from a ZonedDateTime should match its date component.");
    }

    /**
     * Verify conversion when the source object is an {@link OffsetDateTime}.
     */
    @Test
    void toLocalDate_fromOffsetDateTime() {
        // given
        OffsetDateTime source = OffsetDateTime.of(2021, 12, 3, 22, 45, 0, 0, // UTC‑05:00
        ZoneOffset.ofHours(-5));
        // when
        LocalDate result = ZonedDateTimeConversions.toLocalDate(source, converter);
        // then
        // OffsetDateTime is first turned into a ZonedDateTime (using its offset) and then to a LocalDate.
        assertEquals(LocalDate.of(2021, 12, 3), result, "The LocalDate extracted from an OffsetDateTime should match its date component.");
    }

    /**
     * Verify conversion when the source object is a legacy {@link java.util.Date}.
     */
    @Test
    void toLocalDate_fromDate() {
        // given
        // Create a Date representing an exact instant.
        Instant instant = Instant.parse("2022-08-19T14:15:30.00Z");
        Date source = Date.from(instant);
        // Expected LocalDate is the date of that instant in the JVM's default time‑zone.
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        // when
        LocalDate result = ZonedDateTimeConversions.toLocalDate(source, converter);
        // then
        assertEquals(expected, result, "Conversion from java.util.Date should yield the correct LocalDate in the default zone.");
    }

    /**
     * Verify that a {@code null} source returns {@code null}.
     */
    @Test
    void toLocalDate_fromNull() {
        // given
        Object source = null;
        // when
        LocalDate result = ZonedDateTimeConversions.toLocalDate(source, converter);
        // then
        assertNull(result, "Converting a null source should produce a null LocalDate.");
    }

    /**
     * ISO‑8601 string with zone offset should be parsed correctly.
     */
    @Test
    void toLocalDate_fromStringIsoZonedDateTime() {
        String source = "2023-08-15T13:45:30+02:00";
        LocalDate expected = LocalDate.of(2023, 8, 15);
        LocalDate result = ZonedDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result, "ISO‑8601 string with offset must yield the correct LocalDate.");
    }

    /**
     * Plain ISO‑8601 date string (no time) should be interpreted as midnight in the default zone.
     */
    @Test
    void toLocalDate_fromStringIsoLocalDate() {
        String source = "2021-04-01";
        LocalDate expected = LocalDate.of(2021, 4, 1);
        LocalDate result = ZonedDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result, "ISO‑8601 date string should yield the same LocalDate.");
    }

    /**
     * java.util.Date representing epoch (0 ms) should convert to the epoch date in the default zone.
     */
    @Test
    void toLocalDate_fromDateEpoch() {
        Date source = new Date(0L);
        LocalDate expected = Instant.ofEpochMilli(0L).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate result = ZonedDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result, "Epoch Date should map to epoch LocalDate in the default zone.");
    }

    /**
     * SQL Timestamp should be treated like an instant; the date part is extracted.
     */
    @Test
    void toLocalDate_fromSqlTimestamp() {
        Timestamp source = Timestamp.valueOf("1999-12-31 23:59:59");
        // Timestamp has no zone; conversion uses the default zone.
        LocalDate expected = source.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate result = ZonedDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result, "SQL Timestamp should yield the correct LocalDate.");
    }

    /**
     * Long epoch‑millis values – parameterized to cover multiple cases.
     */
    @ParameterizedTest(name = "{index}: epochMillis={0} → {1}")
    @MethodSource("provideLongEpochMillis")
    void toLocalDate_fromLongEpochMillis(Long epochMillis, LocalDate expected) {
        LocalDate result = ZonedDateTimeConversions.toLocalDate(epochMillis, converter);
        assertEquals(expected, result, "Long epoch‑millis should be converted to the correct LocalDate.");
    }

    /**
     * LocalDateTime is interpreted in the default zone; only the date component is returned.
     */
    @Test
    void toLocalDate_fromLocalDateTime() {
        LocalDateTime source = LocalDateTime.of(2022, 2, 28, 23, 59, 59);
        LocalDate expected = source.toLocalDate();
        LocalDate result = ZonedDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result, "LocalDateTime should yield its date component.");
    }

    /**
     * ZonedDateTime with a specific zone should keep its own date when converted.
     */
    @Test
    void toLocalDate_fromZonedDateTimeInput() {
        ZonedDateTime source = ZonedDateTime.of(2020, 12, 31, 23, 0, 0, 0, ZoneId.of("America/New_York"));
        LocalDate expected = LocalDate.of(2020, 12, 31);
        LocalDate result = ZonedDateTimeConversions.toLocalDate(source, converter);
        assertEquals(expected, result, "ZonedDateTime's own date component must be preserved.");
    }

    /**
     * Calendar set to a specific UTC date should be converted correctly.
     */
    @Test
    void toLocalDate_fromCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2005, Calendar.JULY, 4, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        LocalDate expected = LocalDate.of(2005, 7, 4);
        LocalDate result = ZonedDateTimeConversions.toLocalDate(cal, converter);
        assertEquals(expected, result, "Calendar date in UTC should be converted to the same LocalDate.");
    }

    /**
     * Map containing a ZONED_DATE_TIME entry should use that ZonedDateTime for conversion.
     */
    @Test
    void toLocalDate_fromMapWithZonedDateTimeKey() {
        Map<String, Object> map = new LinkedHashMap<>();
        ZonedDateTime zdt = ZonedDateTime.of(2019, 11, 30, 10, 0, 0, 0, ZoneId.of("Europe/Paris"));
        map.put(ZONED_DATE_TIME, zdt);
        LocalDate expected = LocalDate.of(2019, 11, 30);
        LocalDate result = ZonedDateTimeConversions.toLocalDate(map, converter);
        assertEquals(expected, result, "Map with ZONED_DATE_TIME key should yield the date from that ZonedDateTime.");
    }

    /**
     * Passing an unsupported type (Boolean) should throw IllegalArgumentException.
     */
    @Test
    void toLocalDate_fromUnsupportedType() {
        Boolean source = Boolean.TRUE;
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toLocalDate(source, converter), "Unsupported source types must cause IllegalArgumentException.");
    }

    /**
     * Malformed ISO‑8601 string should cause DateTimeParseException.
     */
    @Test
    void toLocalDate_fromInvalidStringFormat() {
        String source = "2023-13-40T25:61:61+99:99";
        assertThrows(DateTimeParseException.class, () -> ZonedDateTimeConversions.toLocalDate(source, converter), "Invalid date‑time string must trigger DateTimeParseException.");
    }

    /**
     * Empty string cannot be parsed and must result in DateTimeParseException.
     */
    @Test
    void toLocalDate_fromEmptyString() {
        String source = "";
        assertThrows(DateTimeParseException.class, () -> ZonedDateTimeConversions.toLocalDate(source, converter), "Empty string should cause DateTimeParseException.");
    }

    /**
     * Passing a null Converter reference should trigger NullPointerException.
     */
    @Test
    void toLocalDate_nullConverter() {
        String source = "2023-08-15";
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toLocalDate(source, null), "Null Converter must cause NullPointerException.");
    }

    /**
     * Map with ZONED_DATE_TIME key but non‑ZonedDateTime value should throw ClassCastException.
     */
    @Test
    void toLocalDate_mapWithNonZonedDateTimeValue() {
        Map<String, Object> map = new LinkedHashMap<>();
        // wrong type
        map.put(ZONED_DATE_TIME, "2023-08-15T00:00:00Z");
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toLocalDate(map, converter), "Non‑ZonedDateTime value for ZONED_DATE_TIME key must cause ClassCastException.");
    }

    /**
     * Map without the ZONED_DATE_TIME entry should cause IllegalArgumentException.
     */
    @Test
    void toLocalDate_mapWithoutZonedDateTimeKey() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("someKey", "2023-08-15");
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toLocalDate(map, converter), "Map lacking ZONED_DATE_TIME key must cause IllegalArgumentException.");
    }

    /**
     * Floating‑point number (Double) is not a valid epoch‑millis representation.
     */
    @Test
    void toLocalDate_unsupportedNumberType() {
        Double source = 12.34;
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toLocalDate(source, converter), "Floating‑point numbers should trigger IllegalArgumentException.");
    }

    /**
     * java.sql.Date is not directly supported; conversion should throw IllegalArgumentException.
     */
    @Test
    void toLocalDate_unsupportedSqlDateType() {
        java.sql.Date source = java.sql.Date.valueOf("2023-08-15");
        assertThrows(IllegalArgumentException.class, () -> ZonedDateTimeConversions.toLocalDate(source, converter), "java.sql.Date should cause IllegalArgumentException.");
    }
}
