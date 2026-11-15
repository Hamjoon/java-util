package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URL;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link MapConversions}.
 *
 * The tests exercise the different conversion pathways present in the class,
 * covering:
 * <ul>
 *   <li>Direct map keys (e.g., {@code UUID}, {@code mostSigBits}/{@code leastSigBits})</li>
 *   <li>Fallback to the generic {@code fromMap} logic</li>
 *   <li>Special handling for date‑time objects, locales and offsets</li>
 *   <li>Exception handling for invalid data (e.g., bad locale language)</li>
 * </ul>
 *
 * All objects are created using the public constructors or factory methods that
 * appear in the supplied source code – no mocking or external libraries are
 * used.
 */
public class MapConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions is part of the public API of the library; a default instance is sufficient for tests.
        converter = new Converter(new ConverterOptions());
    }

    /* ------------------------------------------------------------
     *  UUID conversion tests
     * ------------------------------------------------------------ */
    @Test
    public void testToUuidFromString() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.UUID, "123e4567-e89b-12d3-a456-426655440000");
        Object result = MapConversions.toUUID(map, converter);
        assertTrue(result instanceof UUID);
        assertEquals(UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), result);
    }

    @Test
    public void testToUuidFromMostAndLeastSigBits() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.MOST_SIG_BITS, 0x12345678L);
        map.put(MapConversions.LEAST_SIG_BITS, 0x9abcdef0L);
        Object result = MapConversions.toUUID(map, converter);
        assertTrue(result instanceof UUID);
        assertEquals(new UUID(0x12345678L, 0x9abcdef0L), result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToUuidMissingKeysThrows() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        // No recognised keys – should fall back to fromMap and then throw
        MapConversions.toUUID(map, converter);
    }

    /* ------------------------------------------------------------
     *  Locale conversion tests
     * ------------------------------------------------------------ */
    @Test
    public void testToLocaleSimpleLanguage() {
        Map<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
        map.put(MapConversions.LANGUAGE, "en");
        Locale locale = MapConversions.toLocale(map, converter);
        assertEquals(Locale.ENGLISH, locale);
    }

    @Test
    public void testToLocaleFullSpecification() {
        Map<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
        map.put(MapConversions.LANGUAGE, "fr");
        map.put(MapConversions.COUNTRY, "CA");
        map.put(MapConversions.SCRIPT, "Latn");
        map.put(MapConversions.VARIANT, "POSIX");
        Locale locale = MapConversions.toLocale(map, converter);
        Locale expected = new Locale.Builder()
                .setLanguage("fr")
                .setRegion("CA")
                .setScript("Latn")
                .setVariant("POSIX")
                .build();
        assertEquals(expected, locale);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToLocaleInvalidLanguageThrows() {
        Map<String, String> map = CompactMap.<String, String>builder().insertionOrder().build();
        map.put(MapConversions.LANGUAGE, "!!!"); // illegal language code
        MapConversions.toLocale(map, converter);
    }

    /* ------------------------------------------------------------
     *  Timestamp conversion tests
     * ------------------------------------------------------------ */
    @Test
    public void testToTimestampFromEpochMillisAndNanos() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.EPOCH_MILLIS, 1609459200000L); // 2021‑01‑01T00:00:00Z
        map.put(MapConversions.NANOS, 123456789);
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(1609459200000L, ts.getTime());
        assertEquals(123456789, ts.getNanos());
    }

    @Test
    public void testToTimestampFromTimeString() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.TIME, "1609459200000"); // string that parses to long
        Timestamp ts = MapConversions.toTimestamp(map, converter);
        assertEquals(1609459200000L, ts.getTime());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToTimestampMissingAllKeysThrows() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        // No epochMillis, no time, no value – should trigger fromMap error
        MapConversions.toTimestamp(map, converter);
    }

    /* ------------------------------------------------------------
     *  Calendar conversion tests
     * ------------------------------------------------------------ */
    @Test
    public void testToCalendarFromEpochMillis() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.EPOCH_MILLIS, 1622505600000L); // 2021‑06‑01T00:00:00Z
        Calendar cal = MapConversions.toCalendar(map, converter);
        assertEquals(1622505600000L, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendarFromDateAndTimeAndZone() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.DATE, LocalDate.of(2022, 3, 15));
        map.put(MapConversions.TIME, LocalTime.of(13, 45, 30));
        map.put(MapConversions.ZONE, ZoneId.of("Europe/Paris"));
        Calendar cal = MapConversions.toCalendar(map, converter);
        ZonedDateTime zdt = ZonedDateTime.of(LocalDate.of(2022,3,15), LocalTime.of(13,45,30), ZoneId.of("Europe/Paris"));
        assertEquals(zdt.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    /* ------------------------------------------------------------
     *  OffsetTime conversion tests
     * ------------------------------------------------------------ */
    @Test
    public void testToOffsetTimeFromComponents() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.HOUR, 9);
        map.put(MapConversions.MINUTE, 30);
        map.put(MapConversions.SECOND, 15);
        map.put(MapConversions.NANOS, 500);
        map.put(MapConversions.OFFSET_HOUR, -5);
        map.put(MapConversions.OFFSET_MINUTE, 0);
        OffsetTime ot = MapConversions.toOffsetTime(map, converter);
        assertEquals(OffsetTime.of(9,30,15,500, ZoneOffset.ofHours(-5)), ot);
    }

    @Test
    public void testToOffsetTimeFromStringFallback() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.TIME, "10:15:30+02:00");
        OffsetTime ot = MapConversions.toOffsetTime(map, converter);
        assertEquals(OffsetTime.parse("10:15:30+02:00"), ot);
    }

    /* ------------------------------------------------------------
     *  ZoneId conversion tests
     * ------------------------------------------------------------ */
    @Test
    public void testToZoneIdFromZoneKey() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.ZONE, "America/New_York");
        ZoneId zone = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("America/New_York"), zone);
    }

    @Test
    public void testToZoneIdFromIdKey() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.ID, "UTC");
        ZoneId zone = MapConversions.toZoneId(map, converter);
        assertEquals(ZoneId.of("UTC"), zone);
    }

    /* ------------------------------------------------------------
     *  ZoneOffset conversion tests
     * ------------------------------------------------------------ */
    @Test
    public void testToZoneOffsetFromHoursOnly() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.HOURS, 3);
        ZoneOffset offset = MapConversions.toZoneOffset(map, converter);
        assertEquals(ZoneOffset.ofHours(3), offset);
    }

    @Test
    public void testToZoneOffsetFromFullSpecification() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.HOURS, -2);
        map.put(MapConversions.MINUTES, 30);
        map.put(MapConversions.SECONDS, 15);
        ZoneOffset offset = MapConversions.toZoneOffset(map, converter);
        assertEquals(ZoneOffset.ofHoursMinutesSeconds(-2, 30, 15), offset);
    }

    /* ------------------------------------------------------------
     *  URL and URI conversion tests
     * ------------------------------------------------------------ */
    @Test
    public void testToURLFromString() throws Exception {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.URL_KEY, "https://example.com/path");
        URL url = MapConversions.toURL(map, converter);
        assertEquals(new URL("https://example.com/path"), url);
    }

    @Test
    public void testToURIFromString() throws Exception {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.URI_KEY, "mailto:user@example.com");
        URI uri = MapConversions.toURI(map, converter);
        assertEquals(new URI("mailto:user@example.com"), uri);
    }

    /* ------------------------------------------------------------
     *  Period conversion tests
     * ------------------------------------------------------------ */
    @Test
    public void testToPeriodFromExplicitFields() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.YEARS, 2);
        map.put(MapConversions.MONTHS, 3);
        map.put(MapConversions.DAYS, 10);
        Period period = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(2,3,10), period);
    }

    @Test
    public void testToPeriodFromValueKeyFallsBack() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put(MapConversions.VALUE, "P1Y2M3D");
        // The fallback uses fromMap which will delegate to Converter; we expect it to parse via ISO‑8601.
        Period period = MapConversions.toPeriod(map, converter);
        assertEquals(Period.of(1,2,3), period);
    }

    /* ------------------------------------------------------------
     *  initMap helper test
     * ------------------------------------------------------------ */
    @Test
    public void testInitMapCreatesWrapper() {
        String original = "testValue";
        Map<String, ?> wrapper = MapConversions.initMap(original, converter);
        assertTrue(wrapper.containsKey(MapConversions.V));
        assertEquals(original, wrapper.get(MapConversions.V));
    }

    /* ------------------------------------------------------------
     *  Negative test for fromMap error message
     * ------------------------------------------------------------ */
    @Test
    public void testFromMapErrorMessageContainsExpectedKeys() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        map.put("unrelated", "value");
        try {
            // Directly invoke a private helper via reflection to verify the exception message.
            java.lang.reflect.Method m = MapConversions.class.getDeclaredMethod("fromMap", Object.class, Converter.class, Class.class, String[][].class);
            m.setAccessible(true);
            m.invoke(null, map, converter, UUID.class, new String[][]{{MapConversions.UUID}});
            fail("Expected IllegalArgumentException");
        } catch (Exception e) {
            Throwable cause = e.getCause();
            assertTrue(cause instanceof IllegalArgumentException);
            String msg = cause.getMessage();
            assertTrue(msg.contains("To convert from Map to 'UUID'"));
            assertTrue(msg.contains(MapConversions.UUID));
            assertTrue(msg.contains("[value]"));
        }
    }
}
