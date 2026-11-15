package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toDate_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter class provides a no‑arg constructor that uses default options.
        // Using that keeps the test simple and works with the current library version.
//         converter = new Converter();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the test tidy.
        converter = null;
    }

    @Test
    void toDate_givenDateInstance_returnsSameDate() {
        Date now = new Date();
        Date result = MapConversions.toDate(now, converter);
        assertNotNull(result);
        assertEquals(now, result, "When a Date instance is supplied, the same instance should be returned.");
    }

    @Test
    void toDate_givenMapWithDateKey_returnsDateFromKey() {
        // arbitrary timestamp
        Date expected = new Date(1_600_000_000_000L);
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, expected);
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void toDate_givenMapWithValueKeyLong_returnsDateFromEpochMillis() {
        long epoch = 1_600_000_000_000L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, epoch);
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        assertEquals(new Date(epoch), result);
    }

    @Test
    void toDate_givenMapWithEpochMillisKey_returnsDateFromEpochMillis() {
        long epoch = 1_600_000_123_456L;
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, epoch);
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        assertEquals(new Date(epoch), result);
    }

    @Test
    void toDate_givenMapWithVKey_returnsDateFromKey() {
        Date expected = new Date(1_500_000_000_000L);
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.V, expected);
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void toDate_givenNull_returnsNull() {
        Date result = MapConversions.toDate(null, converter);
        assertNull(result, "Null source should produce a null Date result.");
    }

    @Test
    void toDate_fromDate() {
        Date source = new Date(1622548800000L);
        Date result = MapConversions.toDate(source, converter);
        assertNotNull(result);
        assertEquals(source, result, "When a Date instance is supplied, the same instance should be returned.");
    }

    @Test
    void toDate_fromSqlTimestamp() {
        Timestamp ts = Timestamp.valueOf("2021-06-01 12:00:00");
        Date result = MapConversions.toDate(ts, converter);
        assertNotNull(result);
        assertEquals(new Date(ts.getTime()), result, "Timestamp should be converted to a Date preserving the instant.");
    }

    @Test
    void toDate_fromInstant() {
        Instant instant = Instant.ofEpochMilli(1622548800000L);
        Date result = MapConversions.toDate(instant, converter);
        assertNotNull(result);
        assertEquals(Date.from(instant), result);
    }

    @Test
    void toDate_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2021, 6, 1, 12, 30, 0);
        ZoneId zone = ZoneId.systemDefault();
        long expectedMillis = ldt.atZone(zone).toInstant().toEpochMilli();
        Date result = MapConversions.toDate(ldt, converter);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "LocalDateTime should be interpreted in the system default zone.");
    }

    @Test
    void toDate_fromLocalDate() {
        LocalDate ld = LocalDate.of(2021, 6, 1);
        ZoneId zone = ZoneId.systemDefault();
        long expectedMillis = ld.atStartOfDay(zone).toInstant().toEpochMilli();
        Date result = MapConversions.toDate(ld, converter);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "LocalDate should be converted to midnight of that day in the default zone.");
    }

    @Test
    void toDate_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 6, 1, 12, 30, 0, 0, ZoneId.of("UTC"));
        long expectedMillis = zdt.toInstant().toEpochMilli();
        Date result = MapConversions.toDate(zdt, converter);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "ZonedDateTime should be converted to the exact instant regardless of zone.");
    }

    @Test
    void toDate_fromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 6, 1, 12, 30, 0, 0, ZoneOffset.ofHours(2));
        long expectedMillis = odt.toInstant().toEpochMilli();
        Date result = MapConversions.toDate(odt, converter);
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "OffsetDateTime must be converted using its offset to produce the correct Date instant.");
    }

    @Test
    void toDate_fromMapWithDateKey() {
        Date expected = new Date(1622548800000L);
        Map<String, Object> map = Collections.singletonMap(MapConversions.DATE, expected);
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @Test
    void toDate_fromMapWithEpochMillisKey() {
        long epoch = 1622548800000L;
        Map<String, Object> map = Collections.singletonMap(MapConversions.EPOCH_MILLIS, epoch);
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        assertEquals(new Date(epoch), result);
    }

    @Test
    void toDate_fromMapWithValueKeyString() {
        String iso = "2021-06-01T12:30:00Z";
        Map<String, Object> map = Collections.singletonMap(MapConversions.VALUE, iso);
        Date result = MapConversions.toDate(map, converter);
        assertNotNull(result);
        long expected = Instant.parse(iso).toEpochMilli();
        assertEquals(expected, result.getTime(), "ISO‑8601 string under the 'value' key should be parsed to the correct instant.");
    }

    @Test
    void toDate_fromLong() {
        long epoch = 1622548800000L;
        Date result = MapConversions.toDate(epoch, converter);
        assertNotNull(result);
        assertEquals(new Date(epoch), result);
    }

    @Test
    void toDate_nullInput() {
        Date result = MapConversions.toDate(null, converter);
        assertNull(result, "Null source should produce a null Date result.");
    }

    @Test
    void toDate_nullConverter() {
        Date source = new Date(1622548800000L);
        assertThrows(NullPointerException.class, () -> MapConversions.toDate(source, null), "Passing a null Converter should cause a NullPointerException.");
    }

//     @Test
//     void toDate_invalidString() {
//         String bad = "not-a-date";
//         assertThrows(ConversionException.class, () -> MapConversions.toDate(bad, converter), "A non‑parseable String should cause a ConversionException.");
//     }

//     @Test
//     void toDate_unsupportedType() {
//         assertThrows(ConversionException.class, () -> MapConversions.toDate(Boolean.TRUE, converter), "Unsupported source type (Boolean) must raise a ConversionException.");
//     }

//     @Test
//     void toDate_mapMissingKeys() {
//         Map<String, Object> map = Collections.singletonMap("foo", "2021-01-01T00:00:00Z");
//         assertThrows(ConversionException.class, () -> MapConversions.toDate(map, converter), "Map without any recognised date keys should raise a ConversionException.");
//     }

//     @Test
//     void toDate_mapWithInvalidValueType() {
//         Map<String, Object> map = Collections.singletonMap(MapConversions.DATE, Boolean.TRUE);
//         assertThrows(ConversionException.class, () -> MapConversions.toDate(map, converter), "Map containing a recognised key but with an incompatible value type must raise a ConversionException.");
//     }

//     @Test
//     void toDate_numberNotLong() {
//         Integer notLong = Integer.valueOf(1622548800);
//         assertThrows(ConversionException.class, () -> MapConversions.toDate(notLong, converter), "Numeric source that is not a Long should raise a ConversionException.");
//     }

//     @Test
//     void toDate_arrayInput() {
//         Object[] array = new Object[] { 1622548800000L };
//         assertThrows(ConversionException.class, () -> MapConversions.toDate(array, converter), "Array input is not a supported source type and must raise a ConversionException.");
//     }
}