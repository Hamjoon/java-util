package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CollectionUtilities;
import com.cedarsoftware.util.CompactMap;
import com.cedarsoftware.util.DateUtilities;
import com.cedarsoftware.util.ReflectionUtils;
import com.cedarsoftware.util.StringUtilities;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import static org.junit.Assert.*;

public class MapConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is part of the library; using default constructor
        converter = new Converter(new ConverterOptions());
    }

    /** Helper to create a mutable CompactMap with insertion order */
    private static <K, V> Map<K, V> mapOf(Object... kv) {
        CompactMap<K, V> map = CompactMap.<K, V>builder().insertionOrder().build();
        for (int i = 0; i < kv.length; i += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) kv[i];
            @SuppressWarnings("unchecked")
            V value = (V) kv[i + 1];
            map.put(key, value);
        }
        return map;
    }

    /* ---------- UUID ---------- */

    @Test
    public void testToUUIDFromString() {
        Map<String, Object> m = mapOf("UUID", "123e4567-e89b-12d3-a456-426655440000");
        Object result = MapConversions.toUUID(m, converter);
        assertTrue(result instanceof UUID);
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), result);
    }

    @Test
    public void testToUUIDFromMostLeastSigBits() {
        Map<String, Object> m = mapOf("mostSigBits", 0x12345678L, "leastSigBits", 0x9abcdef0L);
        Object result = MapConversions.toUUID(m, converter);
        assertEquals(new UUID(0x12345678L, 0x9abcdef0L), result);
    }

    @Test
    public void testToUUIDFromValue() {
        Map<String, Object> m = mapOf("_v", "123e4567-e89b-12d3-a456-426655440000");
        Object result = MapConversions.toUUID(m, converter);
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), result);
    }

    /* ---------- Primitive Wrappers ---------- */

    @Test
    public void testToByte() {
        assertEquals(Byte.valueOf((byte) 7), MapConversions.toByte(mapOf("value", 7), converter));
    }

    @Test
    public void testToShort() {
        assertEquals(Short.valueOf((short) 12), MapConversions.toShort(mapOf("value", 12), converter));
    }

    @Test
    public void testToInt() {
        assertEquals(Integer.valueOf(12345), MapConversions.toInt(mapOf("value", 12345), converter));
    }

    @Test
    public void testToLong() {
        assertEquals(Long.valueOf(123456789L), MapConversions.toLong(mapOf("value", 123456789L), converter));
    }

    @Test
    public void testToFloat() {
        assertEquals(Float.valueOf(1.5f), MapConversions.toFloat(mapOf("value", 1.5f), converter));
    }

    @Test
    public void testToDouble() {
        assertEquals(Double.valueOf(2.71828), MapConversions.toDouble(mapOf("value", 2.71828), converter));
    }

    @Test
    public void testToBoolean() {
        assertEquals(Boolean.TRUE, MapConversions.toBoolean(mapOf("value", true), converter));
    }

    @Test
    public void testToBigDecimal() {
        assertEquals(new BigDecimal("123.45"), MapConversions.toBigDecimal(mapOf("value", "123.45"), converter));
    }

    @Test
    public void testToBigInteger() {
        assertEquals(new BigInteger("9876543210"), MapConversions.toBigInteger(mapOf("value", "9876543210"), converter));
    }

    @Test
    public void testToString() {
        assertEquals("hello", MapConversions.toString(mapOf("value", "hello"), converter));
    }

    @Test
    public void testToStringBuffer() {
        assertEquals(new StringBuffer("buf"), MapConversions.toStringBuffer(mapOf("value", "buf"), converter));
    }

    @Test
    public void testToStringBuilder() {
        assertEquals(new StringBuilder("builder"), MapConversions.toStringBuilder(mapOf("value", "builder"), converter));
    }

    @Test
    public void testToCharacter() {
        assertEquals(Character.valueOf('x'), MapConversions.toCharacter(mapOf("value", 'x'), converter));
    }

    @Test
    public void testToAtomicInteger() {
        assertEquals(new AtomicInteger(42), MapConversions.toAtomicInteger(mapOf("value", 42), converter));
    }

    @Test
    public void testToAtomicLong() {
        assertEquals(new AtomicLong(999L), MapConversions.toAtomicLong(mapOf("value", 999L), converter));
    }

    @Test
    public void testToAtomicBoolean() {
        assertEquals(new AtomicBoolean(true), MapConversions.toAtomicBoolean(mapOf("value", true), converter));
    }

    /* ---------- Date / Timestamp ---------- */

    @Test
    public void testToSqlDateFromEpochMillis() {
        long epoch = 1609459200000L; // 2021-01-01T00:00:00Z
        java.sql.Date date = MapConversions.toSqlDate(mapOf("epochMillis", epoch), converter);
        assertEquals(epoch, date.getTime());
    }

    @Test
    public void testToDateFromComponents() {
        Map<String, Object> m = mapOf(
                "date", "2021-01-01",
                "time", "12:34:56",
                "zone", "UTC"
        );
        Date d = MapConversions.toDate(m, converter);
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2021, Calendar.JANUARY, 1, 12, 34, 56);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(cal.getTimeInMillis(), d.getTime());
    }

    @Test
    public void testToTimestampFromEpochAndNanos() {
        Map<String, Object> m = mapOf(
                "epochMillis", 1609459200123L,
                "nanos", 456789012
        );
        Timestamp ts = MapConversions.toTimestamp(m, converter);
        assertEquals(1609459200123L, ts.getTime());
        assertEquals(456789012, ts.getNanos());
    }

    @Test
    public void testToTimestampFromValueNumber() {
        Map<String, Object> m = mapOf("value", 1609459200456L);
        Timestamp ts = MapConversions.toTimestamp(m, converter);
        assertEquals(1609459200456L, ts.getTime());
    }

    @Test
    public void testToTimestampFromValueStringNumber() {
        Map<String, Object> m = mapOf("value", "1609459200789");
        Timestamp ts = MapConversions.toTimestamp(m, converter);
        assertEquals(1609459200789L, ts.getTime());
    }

    @Test
    public void testToTimestampFromDateTimeComponents() {
        Map<String, Object> m = mapOf(
                "date", "2021-02-03",
                "time", "04:05:06.123456789",
                "zone", "UTC",
                "nanos", 0
        );
        Timestamp ts = MapConversions.toTimestamp(m, converter);
        // Expected epoch millis for 2021-02-03T04:05:06.123Z
        ZonedDateTime zdt = ZonedDateTime.of(
                LocalDate.of(2021, 2, 3),
                LocalTime.of(4, 5, 6, 123_456_789),
                ZoneOffset.UTC);
        assertEquals(zdt.toInstant().toEpochMilli(), ts.getTime());
        // nanos should be taken from the time component (123456789)
        assertEquals(123456789, ts.getNanos());
    }

    /* ---------- TimeZone & Calendar ---------- */

    @Test
    public void testToTimeZone() {
        TimeZone tz = MapConversions.toTimeZone(mapOf("zone", "America/New_York"), converter);
        assertEquals(TimeZone.getTimeZone("America/New_York").getID(), tz.getID());
    }

    @Test
    public void testToCalendarFromDateTimeZone() {
        Map<String, Object> m = mapOf(
                "date", "2022-03-04",
                "time", "05:06:07.001",
                "zone", "UTC"
        );
        Calendar cal = MapConversions.toCalendar(m, converter);
        ZonedDateTime zdt = ZonedDateTime.of(
                LocalDate.of(2022, 3, 4),
                LocalTime.of(5, 6, 7, 1_000_000),
                ZoneOffset.UTC);
        assertEquals(zdt.getYear(), cal.get(Calendar.YEAR));
        assertEquals(zdt.getMonthValue() - 1, cal.get(Calendar.MONTH));
        assertEquals(zdt.getDayOfMonth(), cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(zdt.getHour(), cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(zdt.getMinute(), cal.get(Calendar.MINUTE));
        assertEquals(zdt.getSecond(), cal.get(Calendar.SECOND));
        assertEquals(zdt.getNano() / 1_000_000, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void testToCalendarFromTimeOnly() {
        Map<String, Object> m = mapOf(
                "time", "2022-03-04T12:34:56Z"
        );
        Calendar cal = MapConversions.toCalendar(m, converter);
        ZonedDateTime zdt = DateUtilities.parseDate("2022-03-04T12:34:56Z", ZoneId.systemDefault(), true);
        assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    /* ---------- Locale ---------- */

    @Test
    public void testToLocaleFull() {
        Map<String, String> m = mapOf(
                "language", "en",
                "country", "US",
                "script", "Latn",
                "variant", "POSIX"
        );
        Locale locale = MapConversions.toLocale(m, converter);
        assertEquals("en", locale.getLanguage());
        assertEquals("US", locale.getCountry());
        assertEquals("Latn", locale.getScript());
        assertEquals("POSIX", locale.getVariant());
    }

    @Test
    public void testToLocaleFallback() {
        Map<String, String> m = mapOf("value", Locale.GERMAN);
        Locale locale = MapConversions.toLocale(m, converter);
        assertEquals(Locale.GERMAN, locale);
    }

    /* ---------- LocalDate / LocalTime / OffsetTime ---------- */

    @Test
    public void testToLocalDateFromComponents() {
        Map<String, Object> m = mapOf("year", 2020, "month", 2, "day", 29);
        LocalDate ld = MapConversions.toLocalDate(m, converter);
        assertEquals(LocalDate.of(2020, 2, 29), ld);
    }

    @Test
    public void testToLocalDateFromString() {
        Map<String, Object> m = mapOf("date", "2021-07-15");
        LocalDate ld = MapConversions.toLocalDate(m, converter);
        assertEquals(LocalDate.of(2021, 7, 15), ld);
    }

    @Test
    public void testToLocalTimeFromComponents() {
        Map<String, Object> m = mapOf(
                "hour", 10,
                "minute", 20,
                "second", 30,
                "nanos", 123456789
        );
        LocalTime lt = MapConversions.toLocalTime(m, converter);
        assertEquals(LocalTime.of(10, 20, 30, 123456789), lt);
    }

    @Test
    public void testToLocalTimeFromString() {
        Map<String, Object> m = mapOf("time", "11:22:33.444");
        LocalTime lt = MapConversions.toLocalTime(m, converter);
        assertEquals(LocalTime.of(11, 22, 33, 444_000_000), lt);
    }

    @Test
    public void testToOffsetTimeWithOffset() {
        Map<String, Object> m = mapOf(
                "hour", 9,
                "minute", 15,
                "second", 0,
                "nanos", 0,
                "offsetHour", -5,
                "offsetMinute", 0
        );
        OffsetTime ot = MapConversions.toOffsetTime(m, converter);
        assertEquals(OffsetTime.of(9, 15, 0, 0, ZoneOffset.ofHours(-5)), ot);
    }

    @Test
    public void testToOffsetTimeFromString() {
        Map<String, Object> m = mapOf("time", "13:45:30+02:00");
        OffsetTime ot = MapConversions.toOffsetTime(m, converter);
        assertEquals(OffsetTime.parse("13:45:30+02:00"), ot);
    }

    /* ---------- OffsetDateTime ---------- */

    @Test
    public void testToOffsetDateTimeWithDateTimeOffset() {
        Map<String, Object> m = mapOf(
                "date", "2022-08-01",
                "time", "12:34:56",
                "offset", "+03:00"
        );
        OffsetDateTime odt = MapConversions.toOffsetDateTime(m, converter);
        assertEquals(OffsetDateTime.of(
                LocalDate.of(2022, 8, 1),
                LocalTime.of(12, 34, 56),
                ZoneOffset.ofHours(3)), odt);
    }

    @Test
    public void testToOffsetDateTimeFromTimeOffsetOnly() {
        Map<String, Object> m = mapOf(
                "time", "2022-08-01T12:34:56",
                "offset", "-04:00"
        );
        OffsetDateTime odt = MapConversions.toOffsetDateTime(m, converter);
        assertEquals(OffsetDateTime.of(
                LocalDateTime.of(2022, 8, 1, 12, 34, 56),
                ZoneOffset.ofHours(-4)), odt);
    }

    /* ---------- LocalDateTime ---------- */

    @Test
    public void testToLocalDateTimeFromDateAndTime() {
        Map<String, Object> m = mapOf(
                "date", "2023-01-02",
                "time", "03:04:05"
        );
        LocalDateTime ldt = MapConversions.toLocalDateTime(m, converter);
        assertEquals(LocalDateTime.of(2023, 1, 2, 3, 4, 5), ldt);
    }

    @Test
    public void testToLocalDateTimeFromDateOnly() {
        Map<String, Object> m = mapOf("date", "2023-01-02");
        LocalDateTime ldt = MapConversions.toLocalDateTime(m, converter);
        assertEquals(LocalDateTime.of(2023, 1, 2, 0, 0), ldt);
    }

    /* ---------- ZonedDateTime ---------- */

    @Test
    public void testToZonedDateTimeFromEpoch() {
        long epoch = 1672531199000L; // 2022-12-31T23:59:59Z
        Map<String, Object> m = mapOf("epochMillis", epoch);
        ZonedDateTime zdt = MapConversions.toZonedDateTime(m, converter);
        assertEquals(Instant.ofEpochMilli(epoch).atZone(ZoneOffset.UTC), zdt);
    }

    @Test
    public void testToZonedDateTimeFromComponents() {
        Map<String, Object> m = mapOf(
                "date", "2022-12-31",
                "time", "23:59:59",
                "zone", "UTC"
        );
        ZonedDateTime zdt = MapConversions.toZonedDateTime(m, converter);
        assertEquals(ZonedDateTime.of(
                LocalDate.of(2022, 12, 31),
                LocalTime.of(23, 59, 59),
                ZoneOffset.UTC), zdt);
    }

    /* ---------- Class ---------- */

    @Test
    public void testToClass() {
        Map<String, Object> m = mapOf("value", "java.lang.String");
        Class<?> clazz = MapConversions.toClass(m, converter);
        assertEquals(String.class, clazz);
    }

    /* ---------- Duration & Instant ---------- */

    @Test
    public void testToDuration() {
        Map<String, Object> m = mapOf("seconds", 5L, "nanos", 250);
        Duration d = MapConversions.toDuration(m, converter);
        assertEquals(Duration.ofSeconds(5, 250), d);
    }

    @Test
    public void testToInstant() {
        Map<String, Object> m = mapOf("seconds", 10L, "nanos", 500L);
        Instant i = MapConversions.toInstant(m, converter);
        assertEquals(Instant.ofEpochSecond(10, 500), i);
    }

    /* ---------- MonthDay & YearMonth ---------- */

    @Test
    public void testToMonthDay() {
        Map<String, Object> m = mapOf("month", 2, "day", 29);
        MonthDay md = MapConversions.toMonthDay(m, converter);
        assertEquals(MonthDay.of(2, 29), md);
    }

    @Test
    public void testToYearMonth() {
        Map<String, Object> m = mapOf("year", 2022, "month", 12);
        YearMonth ym = MapConversions.toYearMonth(m, converter);
        assertEquals(YearMonth.of(2022, 12), ym);
    }

    /* ---------- Period ---------- */

    @Test
    public void testToPeriodFromComponents() {
        Map<String, Object> m = mapOf("years", 1, "months", 2, "days", 3);
        Period p = MapConversions.toPeriod(m, converter);
        assertEquals(Period.of(1, 2, 3), p);
    }

    @Test
    public void testToPeriodFromValue() {
        Map<String, Object> m = mapOf("value", "P4Y5M6D");
        Period p = MapConversions.toPeriod(m, converter);
        assertEquals(Period.of(4, 5, 6), p);
    }

    /* ---------- ZoneId & ZoneOffset ---------- */

    @Test
    public void testToZoneIdFromZone() {
        Map<String, Object> m = mapOf("zone", "Europe/Paris");
        ZoneId zid = MapConversions.toZoneId(m, converter);
        assertEquals(ZoneId.of("Europe/Paris"), zid);
    }

    @Test
    public void testToZoneIdFromId() {
        Map<String, Object> m = mapOf("id", "America/Los_Angeles");
        ZoneId zid = MapConversions.toZoneId(m, converter);
        assertEquals(ZoneId.of("America/Los_Angeles"), zid);
    }

    @Test
    public void testToZoneOffsetFromHours() {
        Map<String, Object> m = mapOf("hours", -2, "minutes", 30);
        ZoneOffset zo = MapConversions.toZoneOffset(m, converter);
        assertEquals(ZoneOffset.ofHoursMinutes(-2, 30), zo);
    }

    /* ---------- Year ---------- */

    @Test
    public void testToYear() {
        Map<String, Object> m = mapOf("year", 1999);
        Year y = MapConversions.toYear(m, converter);
        assertEquals(Year.of(1999), y);
    }

    /* ---------- URL ---------- */

    @Test
    public void testToURL() throws Exception {
        Map<String, Object> m = mapOf("URL", "http://example.com");
        URL url = MapConversions.toURL(m, converter);
        assertEquals(new URL("http://example.com"), url);
    }

    /* ---------- Throwable ---------- */

    @Test
    public void testToThrowableWithCause() {
        Map<String, Object> m = mapOf(
                "class", "java.lang.IllegalArgumentException",
                "message", "bad argument",
                "cause", "java.lang.NullPointerException",
                "causeMessage", "null"
        );
        Throwable t = MapConversions.toThrowable(m, converter, RuntimeException.class);
        assertTrue(t instanceof IllegalArgumentException);
        assertEquals("bad argument", t.getMessage());
        assertNotNull(t.getCause());
        assertTrue(t.getCause() instanceof NullPointerException);
        assertEquals("null", t.getCause().getMessage());
        // Stack trace should be cleared
        assertEquals(0, t.getStackTrace().length);
    }

    /* ---------- URI ---------- */

    @Test
    public void testToURI() {
        Map<String, Object> m = mapOf("URI", "http://example.com/path");
        URI uri = MapConversions.toURI(m, converter);
        assertEquals(URI.create("http://example.com/path"), uri);
    }

    /* ---------- initMap ---------- */

    @Test
    public void testInitMap() {
        Map<String, ?> result = MapConversions.initMap("payload", converter);
        assertTrue(result.containsKey("_v"));
        assertEquals("payload", result.get("_v"));
    }

    /* ---------- fromMap error handling ---------- */

    @Test(expected = IllegalArgumentException.class)
    public void testFromMapError() {
        // No recognizable keys – should trigger IllegalArgumentException
        MapConversions.toByte(new CompactMap<>(), converter);
    }
}
