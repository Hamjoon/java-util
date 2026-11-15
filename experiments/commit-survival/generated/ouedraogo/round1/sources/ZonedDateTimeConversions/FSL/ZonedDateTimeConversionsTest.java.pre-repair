package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

/**
 * Comprehensive unit tests for {@link ZonedDateTimeConversions}.
 *
 * The tests cover every public static conversion method, exercising
 * normal behaviour as well as zone‑offset handling.
 *
 * The {@link Converter} class is part of the library under test.
 * A minimal {@link ConverterOptions} instance is created for each test
 * with the desired target zone and time‑zone.  All objects are created
 * via their public constructors – no mocking or subclassing is used.
 */
public class ZonedDateTimeConversionsTest {

    /** Helper to build a Converter that targets the supplied {@code zoneId}. */
    private static Converter converterFor(ZoneId zoneId) {
        // ConverterOptions is part of the library; it provides setters for zone and time‑zone.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zoneId);
        options.setTimeZone(TimeZone.getTimeZone(zoneId));
        return new Converter(options);
    }

    /** A fixed source ZonedDateTime used by most tests (UTC). */
    private static final ZonedDateTime SOURCE_UTC =
            ZonedDateTime.of(2023, 3, 25, 15, 30, 45, 123_000_000, ZoneId.of("UTC"));

    /** Target zone used for conversion tests – a zone with a non‑zero offset. */
    private static final ZoneId TARGET_ZONE = ZoneId.of("America/New_York");

    @Test
    public void testToLong() {
        Converter converter = converterFor(TARGET_ZONE);
        long expected = SOURCE_UTC.toInstant().toEpochMilli();
        assertEquals(expected, ZonedDateTimeConversions.toLong(SOURCE_UTC, converter));
    }

    @Test
    public void testToDouble() {
        Converter converter = converterFor(TARGET_ZONE);
        double expected = InstantConversions.toDouble(SOURCE_UTC.toInstant(), converter);
        assertEquals(expected, ZonedDateTimeConversions.toDouble(SOURCE_UTC, converter), 0.0);
    }

    @Test
    public void testToInstant() {
        Converter converter = converterFor(TARGET_ZONE);
        Instant expected = SOURCE_UTC.toInstant();
        assertEquals(expected, ZonedDateTimeConversions.toInstant(SOURCE_UTC, converter));
    }

    @Test
    public void testToLocalDateTime_WithZoneAdjustment() {
        // Source is UTC, target zone is America/New_York (UTC‑05:00 at the test date)
        Converter converter = converterFor(TARGET_ZONE);
        LocalDateTime result = ZonedDateTimeConversions.toLocalDateTime(SOURCE_UTC, converter);
        ZonedDateTime expectedZdt = SOURCE_UTC.withZoneSameInstant(TARGET_ZONE);
        assertEquals(expectedZdt.toLocalDateTime(), result);
    }

    @Test
    public void testToLocalDate() {
        Converter converter = converterFor(TARGET_ZONE);
        LocalDate result = ZonedDateTimeConversions.toLocalDate(SOURCE_UTC, converter);
        ZonedDateTime adjusted = SOURCE_UTC.withZoneSameInstant(TARGET_ZONE);
        assertEquals(adjusted.toLocalDate(), result);
    }

    @Test
    public void testToLocalTime() {
        Converter converter = converterFor(TARGET_ZONE);
        LocalTime result = ZonedDateTimeConversions.toLocalTime(SOURCE_UTC, converter);
        ZonedDateTime adjusted = SOURCE_UTC.withZoneSameInstant(TARGET_ZONE);
        assertEquals(adjusted.toLocalTime(), result);
    }

    @Test
    public void testToOffsetDateTime() {
        Converter converter = converterFor(TARGET_ZONE);
        OffsetDateTime result = ZonedDateTimeConversions.toOffsetDateTime(SOURCE_UTC, converter);
        assertEquals(SOURCE_UTC.toOffsetDateTime(), result);
    }

    @Test
    public void testToAtomicLong() {
        Converter converter = converterFor(TARGET_ZONE);
        long expected = SOURCE_UTC.toInstant().toEpochMilli();
        AtomicLong atomic = ZonedDateTimeConversions.toAtomicLong(SOURCE_UTC, converter);
        assertEquals(expected, atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Converter converter = converterFor(TARGET_ZONE);
        Timestamp ts = ZonedDateTimeConversions.toTimestamp(SOURCE_UTC, converter);
        assertEquals(Timestamp.from(SOURCE_UTC.toInstant()), ts);
    }

    @Test
    public void testToCalendar() {
        Converter converter = converterFor(TARGET_ZONE);
        Calendar cal = ZonedDateTimeConversions.toCalendar(SOURCE_UTC, converter);
        assertEquals(TARGET_ZONE.getId(), cal.getTimeZone().getID());
        assertEquals(SOURCE_UTC.toInstant().toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        Converter converter = converterFor(TARGET_ZONE);
        java.sql.Date sqlDate = ZonedDateTimeConversions.toSqlDate(SOURCE_UTC, converter);
        assertEquals(SOURCE_UTC.toInstant().toEpochMilli(), sqlDate.getTime());
    }

    @Test
    public void testToDate() {
        Converter converter = converterFor(TARGET_ZONE);
        Date date = ZonedDateTimeConversions.toDate(SOURCE_UTC, converter);
        assertEquals(SOURCE_UTC.toInstant().toEpochMilli(), date.getTime());
    }

    @Test
    public void testToBigInteger() {
        Converter converter = converterFor(TARGET_ZONE);
        BigInteger bi = ZonedDateTimeConversions.toBigInteger(SOURCE_UTC, converter);
        BigInteger expected = InstantConversions.toBigInteger(SOURCE_UTC.toInstant(), converter);
        assertEquals(expected, bi);
    }

    @Test
    public void testToBigDecimal() {
        Converter converter = converterFor(TARGET_ZONE);
        BigDecimal bd = ZonedDateTimeConversions.toBigDecimal(SOURCE_UTC, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(SOURCE_UTC.toInstant(), converter);
        assertEquals(0, expected.compareTo(bd));
    }

    @Test
    public void testToString() {
        Converter converter = converterFor(TARGET_ZONE);
        String result = ZonedDateTimeConversions.toString(SOURCE_UTC, converter);
        String expected = SOURCE_UTC.format(DateTimeFormatter.ISO_DATE_TIME);
        assertEquals(expected, result);
    }

    @Test
    public void testToMap() {
        Converter converter = converterFor(TARGET_ZONE);
        Map<String, Object> map = ZonedDateTimeConversions.toMap(SOURCE_UTC, converter);

        // Expected map entries
        String expectedDate = SOURCE_UTC.toLocalDate().toString();
        String expectedTime = SOURCE_UTC.toLocalTime().toString();
        String expectedZone = SOURCE_UTC.getZone().toString();

        assertEquals(3, map.size());
        assertEquals(expectedDate, map.get(MapConversions.DATE));
        assertEquals(expectedTime, map.get(MapConversions.TIME));
        assertEquals(expectedZone, map.get(MapConversions.ZONE));
    }

    @Test
    public void testZoneConversionBoundary() {
        // Verify that a ZonedDateTime that crosses a day boundary when converted
        // still yields correct LocalDate and LocalTime.
        ZonedDateTime utcMidnight = ZonedDateTime.of(2023, 12, 31, 23, 30, 0, 0, ZoneId.of("UTC"));
        // Target zone: Pacific/Kiritimati (UTC+14) – moves the date forward.
        ZoneId farEast = ZoneId.of("Pacific/Kiritimati");
        Converter converter = converterFor(farEast);

        LocalDate date = ZonedDateTimeConversions.toLocalDate(utcMidnight, converter);
        LocalTime time = ZonedDateTimeConversions.toLocalTime(utcMidnight, converter);

        ZonedDateTime adjusted = utcMidnight.withZoneSameInstant(farEast);
        assertEquals(adjusted.toLocalDate(), date);
        assertEquals(adjusted.toLocalTime(), time);
    }

    @Test
    public void testNullInputBehaviour() {
        // All conversion methods assume a non‑null source; passing null should
        // result in a NullPointerException.  The test verifies that the
        // exception is thrown consistently.
        Converter converter = converterFor(TARGET_ZONE);
        Object nullObj = null;

        try { ZonedDateTimeConversions.toLong(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toDouble(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toInstant(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toLocalDateTime(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toLocalDate(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toLocalTime(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toOffsetDateTime(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toAtomicLong(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toTimestamp(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toCalendar(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toSqlDate(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toDate(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toBigInteger(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toBigDecimal(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toString(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
        try { ZonedDateTimeConversions.toMap(nullObj, converter); fail("NPE expected"); } catch (NullPointerException ignored) {}
    }
}
