package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.junit.jupiter.params.provider.Arguments;

@ExtendWith(MockitoExtension.class)
class MapConversions_toSqlDate_Test {

    private Converter converter;

    /**
     * Simple POJO used to verify that unsupported types raise an exception.
     */
    static class MyPojo {
        // empty custom class for testing unsupported conversion
    }

    @BeforeEach
    void setupBeforeEach() {
        // The Converter requires a ConverterOptions instance; use the default options.
//         converter = new Converter(new ConverterOptions());
    }

    private static List<Arguments> mapKeyProvider() {
        return List.of(// sqlDate key – should return the same instance
        Arguments.of(Map.of("sqlDate", new Date(1672531200000L)), new Date(1672531200000L), true, "sqlDate key"), // value key – legacy handling
        Arguments.of(Map.of("value", new Date(1672444800000L)), new Date(1672444800000L), true, "value key"), // short key _v
        Arguments.of(Map.of("_v", new Date(1623715200000L)), new Date(1623715200000L), true, "_v key"), // epochMillis numeric
        Arguments.of(Map.of("epochMillis", 1672531200000L), new Date(1672531200000L), true, "epochMillis numeric"));
    }

    @Test
    void toSqlDate_givenSqlDate_returnsSameInstance() {
        // 2020‑09‑13T12:26:40Z
        Date sqlDate = new Date(1_600_000_000_000L);
        Date result = (Date) MapConversions.toSqlDate(sqlDate, converter);
        assertNotNull(result);
        // The conversion should preserve the exact instant.
        assertEquals(sqlDate.getTime(), result.getTime());
        // The implementation returns a java.sql.Date (sub‑class of java.util.Date)
        assertTrue(result instanceof java.sql.Date);
    }

    @Test
    void toSqlDate_givenUtilDate_returnsSqlDateWithSameInstant() {
        java.util.Date utilDate = new java.util.Date(1_600_000_000_123L);
        Date result = (Date) MapConversions.toSqlDate(utilDate, converter);
        assertNotNull(result);
        assertEquals(utilDate.getTime(), result.getTime());
        assertTrue(result instanceof java.sql.Date);
    }

    @Test
    void toSqlDate_givenMapWithEpochMillis_returnsCorrespondingSqlDate() {
        long epoch = 1_600_000_000_456L;
        Map<String, Object> map = new HashMap<>();
        // one of the recognised keys
        map.put("epochMillis", epoch);
        Date result = (Date) MapConversions.toSqlDate(map, converter);
        assertNotNull(result);
        assertEquals(epoch, result.getTime());
        assertTrue(result instanceof java.sql.Date);
    }

    @Test
    void toSqlDate_givenMapWithSqlDateKey_returnsSameSqlDate() {
        Date sqlDate = new Date(1_600_000_000_789L);
        Map<String, Object> map = new HashMap<>();
        map.put("sqlDate", sqlDate);
        Date result = (Date) MapConversions.toSqlDate(map, converter);
        assertNotNull(result);
        assertEquals(sqlDate.getTime(), result.getTime());
        assertTrue(result instanceof java.sql.Date);
    }

    @Test
    void toSqlDate_givenNull_returnsNull() {
        Date result = (Date) MapConversions.toSqlDate(null, converter);
        assertNull(result);
    }

    @ParameterizedTest(name = "Map key handling – {3}")
    @MethodSource("mapKeyProvider")
    void toSqlDate_mapKeyHandling(Map<String, Object> sourceMap, Date expectedDate, boolean sameInstanceExpected, String description) {
        Date result = (Date) MapConversions.toSqlDate(sourceMap, converter);
        assertNotNull(result, "Result should not be null for " + description);
        assertEquals(expectedDate.getTime(), result.getTime(), "Incorrect epoch millis for " + description);
        if (sameInstanceExpected) {
            // When the map already contains a java.sql.Date, the same instance is expected.
            assertSame(sourceMap.get(MapConversions.SQL_DATE), result, "Expected same instance for " + description);
        }
    }

    @Test
    void toSqlDate_givenLocalDate_returnsCorrespondingSqlDate() {
        LocalDate localDate = LocalDate.of(2021, 12, 31);
        Date result = (Date) MapConversions.toSqlDate(localDate, converter);
        assertNotNull(result);
        assertEquals(Date.valueOf(localDate).getTime(), result.getTime());
    }

    @Test
    void toSqlDate_givenInstant_returnsSqlDateInUTC() {
        Instant instant = Instant.parse("2023-03-15T12:34:56Z");
        Date result = (Date) MapConversions.toSqlDate(instant, converter);
        assertNotNull(result);
        // The date part of the instant in UTC is 2023‑03‑15
        assertEquals(Date.valueOf(LocalDate.ofInstant(instant, ZoneOffset.UTC)).getTime(), result.getTime());
    }

    @Test
    void toSqlDate_givenTimestamp_returnsSqlDateTruncated() {
        Timestamp ts = Timestamp.valueOf("2020-02-29 23:59:59.123");
        Date result = (Date) MapConversions.toSqlDate(ts, converter);
        assertNotNull(result);
        assertEquals(Date.valueOf(LocalDate.of(2020, 2, 29)).getTime(), result.getTime());
    }

    @Test
    void toSqlDate_givenCalendar_returnsSqlDateBasedOnInstant() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(2019, Calendar.JULY, 4, 8, 15, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date result = (Date) MapConversions.toSqlDate(cal, converter);
        assertNotNull(result);
        assertEquals(Date.valueOf(LocalDate.of(2019, 7, 4)).getTime(), result.getTime());
    }

    @Test
    void toSqlDate_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> MapConversions.toSqlDate(null, converter), "Expected NPE when source is null");
    }

    @Test
    void toSqlDate_nullConverter_throwsNullPointerException() {
        java.util.Date utilDate = new java.util.Date();
        assertThrows(NullPointerException.class, () -> MapConversions.toSqlDate(utilDate, null), "Expected NPE when converter is null");
    }

    @Test
    void toSqlDate_nullConverterWithValidMap_throwsNullPointerException() {
        Map<String, Object> map = Map.of("sqlDate", new Date(0L));
        assertThrows(NullPointerException.class, () -> MapConversions.toSqlDate(map, null), "Expected NPE when converter is null even if map contains a valid value");
    }

    @Test
    void toSqlDate_givenBoolean_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toSqlDate(Boolean.TRUE, converter), "Boolean should not be convertible to java.sql.Date");
    }

    @Test
    void toSqlDate_givenCustomObject_throwsIllegalArgumentException() {
        MyPojo pojo = new MyPojo();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toSqlDate(pojo, converter), "Custom objects without conversion path should cause IllegalArgumentException");
    }

    @Test
    void toSqlDate_mapWithSqlDateKeyAndStringValue_throwsIllegalArgumentException() {
        Map<String, Object> map = new HashMap<>();
        map.put("sqlDate", "not-a-date");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toSqlDate(map, converter), "String value for sqlDate key should cause IllegalArgumentException");
    }

    @Test
    void toSqlDate_mapWithEpochMillisStringValue_throwsNumberFormatException() {
        Map<String, Object> map = new HashMap<>();
        // non‑numeric string
        map.put("epochMillis", "12345");
        assertThrows(NumberFormatException.class, () -> MapConversions.toSqlDate(map, converter), "Non‑numeric epochMillis string should cause NumberFormatException");
    }

    @Test
    void toSqlDate_invalidDateString_throwsDateTimeParseException() {
        // February 30 does not exist
        String invalidDate = "2021-02-30";
        assertThrows(DateTimeParseException.class, () -> MapConversions.toSqlDate(invalidDate, converter), "Invalid calendar date string should cause DateTimeParseException");
    }

    @Test
    void toSqlDate_emptyString_throwsDateTimeParseException() {
        String empty = "";
        assertThrows(DateTimeParseException.class, () -> MapConversions.toSqlDate(empty, converter), "Empty string should cause DateTimeParseException");
    }
}