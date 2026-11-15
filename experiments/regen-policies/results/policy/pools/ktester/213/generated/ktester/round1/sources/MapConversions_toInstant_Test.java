package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toInstant_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter class requires a ConverterOptions instance.
        // Using the default options is sufficient for the conversion tests.
//         converter = new Converter(new ConverterOptions());
    }

    @Test
    void toInstant_returnsSameInstantWhenSourceIsInstant() {
        Instant now = Instant.now();
        Instant result = MapConversions.toInstant(now, converter);
        assertSame(now, result, "When the source object is already an Instant the same instance should be returned");
    }

    @Test
    void toInstant_extractsInstantFromMapUsingInstantKey() {
        Instant expected = Instant.parse("2023-01-02T03:04:05Z");
        Map<String, Object> map = new HashMap<>();
        // primary key defined in INSTANT_KEYS
        map.put("instant", expected);
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(expected, result, "The Instant stored under the 'instant' key should be returned");
    }

    @Test
    void toInstant_extractsInstantFromMapUsingValueKey() {
        Instant expected = Instant.parse("2021-12-31T23:59:59Z");
        Map<String, Object> map = new HashMap<>();
        // secondary key defined in INSTANT_KEYS
        map.put("value", expected);
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(expected, result, "The Instant stored under the 'value' key should be returned");
    }

    @Test
    void toInstant_extractsInstantFromMapUsingVKey() {
        Instant expected = Instant.parse("2000-01-01T00:00:00Z");
        Map<String, Object> map = new HashMap<>();
        // tertiary key defined in INSTANT_KEYS
        map.put("_v", expected);
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(expected, result, "The Instant stored under the '_v' key should be returned");
    }

    @Test
    void toInstant_returnsNullWhenSourceIsNull() {
        Instant result = MapConversions.toInstant(null, converter);
        assertNull(result, "A null source should result in a null Instant");
    }

    @Test
    void toInstant_convertsLongEpochMilliToInstantWhenMapContainsEpochMillis() {
        // The dispatch implementation also supports a map with the key "epochMillis".
        // This test confirms that behaviour by providing a long value.
        // corresponds to 2020‑09‑13T12:26:40Z
        long epochMillis = 1_600_000_000_000L;
        Instant expected = Instant.ofEpochMilli(epochMillis);
        Map<String, Object> map = new HashMap<>();
        map.put("epochMillis", epochMillis);
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(expected, result, "A map containing 'epochMillis' should be converted to the corresponding Instant");
    }

    @Test
    void directInstantInput_returnsSameInstance() {
        Instant src = Instant.parse("2023-01-01T12:34:56Z");
        Instant result = MapConversions.toInstant(src, converter);
        assertSame(src, result, "Direct Instant input must be returned unchanged");
    }

    @Test
    void instantFromDate() {
        // 2023-01-01T12:34:56Z
        Date date = new Date(1_672_572_896_000L);
        Instant expected = Instant.ofEpochMilli(date.getTime());
        Instant result = MapConversions.toInstant(date, converter);
        assertEquals(expected, result);
    }

    @Test
    void instantFromEpochDate() {
        Date epoch = new Date(0L);
        Instant result = MapConversions.toInstant(epoch, converter);
        assertEquals(Instant.EPOCH, result);
    }

    @Test
    void instantFromTimestamp() {
        Timestamp ts = Timestamp.from(Instant.parse("2023-01-01T12:34:56Z"));
        Instant result = MapConversions.toInstant(ts, converter);
        assertEquals(Instant.parse("2023-01-01T12:34:56Z"), result);
    }

    @Test
    void instantFromNegativeTimestamp() {
        // one day before epoch
        Timestamp ts = new Timestamp(-86_400_000L);
        Instant result = MapConversions.toInstant(ts, converter);
        assertEquals(Instant.ofEpochMilli(-86_400_000L), result);
    }

    @Test
    void instantFromLongEpochMillis() {
        Long epoch = 1_672_572_896_000L;
        Instant result = MapConversions.toInstant(epoch, converter);
        assertEquals(Instant.ofEpochMilli(epoch), result);
    }

    @Test
    void instantFromZeroLong() {
        Long zero = 0L;
        Instant result = MapConversions.toInstant(zero, converter);
        assertEquals(Instant.EPOCH, result);
    }

    @Test
    void instantFromIsoString() {
        String iso = "2023-01-01T12:34:56Z";
        Instant result = MapConversions.toInstant(iso, converter);
        assertEquals(Instant.parse(iso), result);
    }

    @Test
    void instantFromIsoEpochString() {
        String iso = "1970-01-01T00:00:00Z";
        Instant result = MapConversions.toInstant(iso, converter);
        assertEquals(Instant.EPOCH, result);
    }

    @Test
    void instantFromMapWithInstantKey() {
        Instant expected = Instant.parse("2023-01-01T12:34:56Z");
        Map<String, Object> map = Map.of("instant", expected);
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(expected, result);
    }

    @Test
    void instantFromMapWithValueKeyString() {
        String iso = "2023-01-01T12:34:56Z";
        Map<String, Object> map = Map.of("value", iso);
        Instant result = MapConversions.toInstant(map, converter);
        assertEquals(Instant.parse(iso), result);
    }

    @Test
    void instantFromCalendarUtc() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2023-01-01T12:34:56Z
        cal.setTimeInMillis(1_672_572_896_000L);
        Instant expected = Instant.ofEpochMilli(cal.getTimeInMillis());
        Instant result = MapConversions.toInstant(cal, converter);
        assertEquals(expected, result);
    }

    @Test
    void instantFromLocalDateTimeAssumedUtc() {
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 34, 56);
        Instant expected = ldt.toInstant(ZoneOffset.UTC);
        Instant result = MapConversions.toInstant(ldt, converter);
        assertEquals(expected, result);
    }

    @Test
    void instantFromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneId.of("America/New_York"));
        Instant expected = zdt.toInstant();
        Instant result = MapConversions.toInstant(zdt, converter);
        assertEquals(expected, result);
    }

    @Test
    void instantFromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneOffset.ofHours(-5));
        Instant expected = odt.toInstant();
        Instant result = MapConversions.toInstant(odt, converter);
        assertEquals(expected, result);
    }

    @Test
    void instantFromNullReturnsNull() {
        assertNull(MapConversions.toInstant(null, converter));
    }

    @Test
    void instantFromUnsupportedBooleanThrows() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(Boolean.TRUE, converter));
    }

    @Test
    void instantFromMalformedIsoStringThrows() {
        // month 13 is invalid
        String badIso = "2023-13-01T00:00:00Z";
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(badIso, converter));
    }

    @Test
    void instantFromMapWithInvalidInstantValueThrows() {
        Map<String, Object> map = Map.of("instant", Boolean.TRUE);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(map, converter));
    }

    @Test
    void instantFromMapWithInvalidValueStringThrows() {
        Map<String, Object> map = Map.of("value", "not-a-date");
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(map, converter));
    }

    @Test
    void instantFromMapWithNullValueThrows() {
        Map<String, Object> map = Map.of("instant", null);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(map, converter));
    }

    @Test
    void instantFromNonIntegralNumberThrows() {
        Double d = 12_345.678;
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(d, converter));
    }

    @Test
    void instantFromEpochMillisOutOfRangeThrows() {
        // Long.MAX_VALUE milliseconds is far beyond the range of Instant
        assertThrows(DateTimeException.class, () -> MapConversions.toInstant(Long.MAX_VALUE, converter));
    }

    @Test
    void instantFromOffsetTimeThrows() {
        OffsetTime ot = OffsetTime.of(12, 34, 56, 0, ZoneOffset.UTC);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(ot, converter));
    }

    @Test
    void instantFromLocalTimeThrows() {
        LocalTime lt = LocalTime.of(12, 34, 56);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(lt, converter));
    }

    @Test
    void instantFromYearMonthThrows() {
        YearMonth ym = YearMonth.of(2023, 1);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(ym, converter));
    }

    @Test
    void instantFromUuidThrows() {
        UUID uuid = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toInstant(uuid, converter));
    }
}