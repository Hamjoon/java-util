package com.cedarsoftware.util.convert;

import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.MonthDay;
import java.time.YearMonth;
import java.time.Period;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * Comprehensive tests for {@link MapConversions}.
 * The tests use the real {@link Converter} implementation provided by the library.
 * All conversion paths are exercised, including positive cases and expected
 * failure messages.
 */
public class MapConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // The library provides a default ConverterOptions class; we instantiate it with defaults.
        converter = new Converter(new ConverterOptions());
    }

    // ----------------------------------------------------------------------
    // UUID conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToUUID_WithUuidString() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.UUID, "123e4567-e89b-12d3-a456-426655440000");
        Object result = MapConversions.toUUID(map, converter);
        assertTrue(result instanceof UUID);
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), result);
    }

    @Test
    public void testToUUID_WithMostAndLeastSigBits() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.MOST_SIG_BITS, 0x1234567890ABCDEFL);
        map.put(MapConversions.LEAST_SIG_BITS, 0x0FEDCBA987654321L);
        Object result = MapConversions.toUUID(map, converter);
        assertTrue(result instanceof UUID);
        UUID expected = new UUID(0x1234567890ABCDEFL, 0x0FEDCBA987654321L);
        assertEquals(expected, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUUID_MissingKeys_Throws() {
        Map<String, Object> map = new HashMap<>();
        // No UUID, mostSigBits or leastSigBits
        MapConversions.toUUID(map, converter);
    }

    // ----------------------------------------------------------------------
    // Locale conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToLocale_FullSpecification() {
        Map<String, String> map = new HashMap<>();
        map.put(MapConversions.LANGUAGE, "en");
        map.put(MapConversions.COUNTRY, "US");
        map.put(MapConversions.SCRIPT, "Latn");
        map.put(MapConversions.VARIANT, "POSIX");
        Locale locale = MapConversions.toLocale(map, converter);
        assertEquals(Locale.forLanguageTag("en-US-Latn-POSIX"), locale);
    }

    @Test
    public void testToLocale_MissingLanguage_UsesFromMap() {
        Map<String, String> map = new HashMap<>();
        map.put(MapConversions.V, "fr-CA");
        Locale locale = MapConversions.toLocale(map, converter);
        assertEquals(Locale.forLanguageTag("fr-CA"), locale);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocale_InvalidLanguage_Throws() {
        Map<String, String> map = new HashMap<>();
        map.put(MapConversions.LANGUAGE, "invalid-language-code");
        MapConversions.toLocale(map, converter);
    }

    // ----------------------------------------------------------------------
    // LocalDate conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToLocalDate_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, 2023);
        map.put(MapConversions.MONTH, 5);
        map.put(MapConversions.DAY, 17);
        LocalDate date = MapConversions.toLocalDate(map, converter);
        assertEquals(LocalDate.of(2023, 5, 17), date);
    }

    @Test
    public void testToLocalDate_FromValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.V, LocalDate.of(2020, 1, 1));
        LocalDate date = MapConversions.toLocalDate(map, converter);
        assertEquals(LocalDate.of(2020, 1, 1), date);
    }

    // ----------------------------------------------------------------------
    // LocalTime conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToLocalTime_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 14);
        map.put(MapConversions.MINUTE, 30);
        map.put(MapConversions.SECOND, 45);
        map.put(MapConversions.NANOS, 123456789);
        LocalTime time = MapConversions.toLocalTime(map, converter);
        assertEquals(LocalTime.of(14, 30, 45, 123456789), time);
    }

    @Test
    public void testToLocalTime_FromValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.V, LocalTime.NOON);
        LocalTime time = MapConversions.toLocalTime(map, converter);
        assertEquals(LocalTime.NOON, time);
    }

    // ----------------------------------------------------------------------
    // OffsetTime conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToOffsetTime_WithOffsetComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOUR, 9);
        map.put(MapConversions.MINUTE, 15);
        map.put(MapConversions.SECOND, 0);
        map.put(MapConversions.NANOS, 0);
        map.put(MapConversions.OFFSET_HOUR, -5);
        map.put(MapConversions.OFFSET_MINUTE, 0);
        OffsetTime ot = MapConversions.toOffsetTime(map, converter);
        assertEquals(OffsetTime.of(9, 15, 0, 0, ZoneOffset.ofHours(-5)), ot);
    }

    @Test
    public void testToOffsetTime_FromTimeString() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, "10:20:30+02:00");
        OffsetTime ot = MapConversions.toOffsetTime(map, converter);
        assertEquals(OffsetTime.parse("10:20:30+02:00"), ot);
    }

    // ----------------------------------------------------------------------
    // OffsetDateTime conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToOffsetDateTime_WithDateAndOffset() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, LocalDate.of(2022, 12, 31));
        map.put(MapConversions.TIME, LocalTime.of(23, 59, 59));
        map.put(MapConversions.OFFSET, ZoneOffset.ofHoursMinutes(3, 30));
        OffsetDateTime odt = MapConversions.toOffsetDateTime(map, converter);
        assertEquals(OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHoursMinutes(3, 30)), odt);
    }

    @Test
    public void testToOffsetDateTime_WithTimeOnly() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.TIME, LocalDateTime.of(2021, 6, 15, 12, 0));
        map.put(MapConversions.OFFSET, ZoneOffset.UTC);
        OffsetDateTime odt = MapConversions.toOffsetDateTime(map, converter);
        assertEquals(OffsetDateTime.of(2021, 6, 15, 12, 0, 0, 0, ZoneOffset.UTC), odt);
    }

    // ----------------------------------------------------------------------
    // ZonedDateTime conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToZonedDateTime_FullMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, LocalDate.of(2020, 2, 29));
        map.put(MapConversions.TIME, LocalTime.of(6, 30));
        map.put(MapConversions.ZONE, ZoneId.of("Europe/Paris"));
        ZonedDateTime zdt = MapConversions.toZonedDateTime(map, converter);
        assertEquals(ZonedDateTime.of(2020, 2, 29, 6, 30, 0, 0, ZoneId.of("Europe/Paris")), zdt);
    }

    @Test
    public void testToZonedDateTime_FromEpochMillis() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, 1609459200000L); // 2021-01-01T00:00:00Z
        ZonedDateTime zdt = MapConversions.toZonedDateTime(map, converter);
        assertEquals(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1609459200000L), ZoneId.of("UTC")), zdt);
    }

    // ----------------------------------------------------------------------
    // Duration and Instant conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToDuration_FromSecondsAndNanos() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, 5L);
        map.put(MapConversions.NANOS, 500);
        Duration dur = MapConversions.toDuration(map, converter);
        assertEquals(Duration.ofSeconds(5, 500), dur);
    }

    @Test
    public void testToInstant_FromSecondsAndNanos() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.SECONDS, 1622559600L); // 2021-06-01T12:00:00Z
        map.put(MapConversions.NANOS, 250L);
        Instant instant = MapConversions.toInstant(map, converter);
        assertEquals(Instant.ofEpochSecond(1622559600L, 250L), instant);
    }

    // ----------------------------------------------------------------------
    // MonthDay and YearMonth conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToMonthDay_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.MONTH, 2);
        map.put(MapConversions.DAY, 29);
        MonthDay md = MapConversions.toMonthDay(map, converter);
        assertEquals(MonthDay.of(2, 29), md);
    }

    @Test
    public void testToYearMonth_FromComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEAR, 1999);
        map.put(MapConversions.MONTH, 12);
        YearMonth ym = MapConversions.toYearMonth(map, converter);
        assertEquals(YearMonth.of(1999, 12), ym);
    }

    // ----------------------------------------------------------------------
    // Period conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToPeriod_WithValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, "P2Y3M5D");
        Period p = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(2, 3, 5), p);
    }

    @Test
    public void testToPeriod_FromComponentMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.YEARS, 1);
        map.put(MapConversions.MONTHS, 2);
        map.put(MapConversions.DAYS, 3);
        Period p = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(1, 2, 3), p);
    }

    // ----------------------------------------------------------------------
    // ZoneId and ZoneOffset conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToZoneId_FromZoneKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ZONE, "America/New_York");
        ZoneId zid = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("America/New_York"), zid);
    }

    @Test
    public void testToZoneId_FromIdKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.ID, "UTC");
        ZoneId zid = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("UTC"), zid);
    }

    @Test
    public void testToZoneOffset_FromHoursMinutesSeconds() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.HOURS, -2);
        map.put(MapConversions.MINUTES, 30);
        map.put(MapConversions.SECONDS, 15);
        ZoneOffset zo = MapConversions.toZoneOffset(map, converter);
        assertEquals(ZoneOffset.ofHoursMinutesSeconds(-2, 30, 15), zo);
    }

    // ----------------------------------------------------------------------
    // URL and URI conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToURL_FromKey() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.URL_KEY, "https://example.com");
        URL url = MapConversions.toURL(map, converter);
        assertEquals(new URL("https://example.com"), url);
    }

    @Test
    public void testToURI_FromKey() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.URI_KEY, "mailto:test@example.com");
        URI uri = MapConversions.toURI(map, converter);
        assertEquals(new URI("mailto:test@example.com"), uri);
    }

    // ----------------------------------------------------------------------
    // Timestamp conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToTimestamp_FromEpochMillisAndNanos() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, 1622559600123L);
        map.put(MapConversions.NANOS, 456789);
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(1622559600123L, ts.getTime());
        assertEquals(456789, ts.getNanos());
    }

    @Test
    public void testToTimestamp_FromTimeComponents() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, LocalDate.of(2022, 1, 1));
        map.put(MapConversions.TIME, LocalTime.of(0, 0, 0, 123456789));
        map.put(MapConversions.ZONE, ZoneId.of("UTC"));
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(Instant.parse("2022-01-01T00:00:00.123456789Z").toEpochMilli(), ts.getTime());
        assertEquals(123456789, ts.getNanos());
    }

    // ----------------------------------------------------------------------
    // Calendar conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToCalendar_FromEpochMillis() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.EPOCH_MILLIS, 1609459200000L);
        Calendar cal = MapConversions.toCalendar(map, converter);
        assertEquals(1609459200000L, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_FromDateTimeAndZone() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.DATE, LocalDate.of(2021, 3, 14));
        map.put(MapConversions.TIME, LocalTime.of(2, 30));
        map.put(MapConversions.ZONE, ZoneId.of("America/New_York"));
        Calendar cal = MapConversions.toCalendar(map, converter);
        ZonedDateTime zdt = ZonedDateTime.of(LocalDate.of(2021, 3, 14), LocalTime.of(2,30), ZoneId.of("America/New_York"));
        assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    // ----------------------------------------------------------------------
    // Throwable conversion tests
    // ----------------------------------------------------------------------
    @Test
    public void testToThrowable_WithCause() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, IllegalArgumentException.class.getName());
        map.put(MapConversions.MESSAGE, "top level");
        map.put(MapConversions.CAUSE, NullPointerException.class.getName());
        map.put(MapConversions.CAUSE_MESSAGE, "inner cause");
        Throwable t = MapConversions.toThrowable(map, converter);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("top level", t.getMessage());
        assertNotNull(t.getCause());
        assertTrue(t.getCause() instanceof NullPointerException);
        assertEquals("inner cause", t.getCause().getMessage());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToThrowable_InvalidClass_Throws() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, "non.existent.ClassName");
        map.put(MapConversions.MESSAGE, "msg");
        MapConversions.toThrowable(map, converter);
    }

    // ----------------------------------------------------------------------
    // initMap utility test
    // ----------------------------------------------------------------------
    @Test
    public void testInitMap_PopulatesVKey() {
        String original = "testValue";
        Map<String, ?> result = MapConversions.initMap(original, converter);
        assertTrue(result.containsKey(MapConversions.V));
        assertEquals(original, result.get(MapConversions.V));
    }
}
