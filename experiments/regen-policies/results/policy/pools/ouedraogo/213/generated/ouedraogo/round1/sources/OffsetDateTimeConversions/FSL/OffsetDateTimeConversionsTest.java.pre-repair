package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link OffsetDateTimeConversions}.
 * The tests construct real {@link OffsetDateTime} instances and a real {@link Converter}
 * (using the default {@link ConverterOptions}) and verify the result of each conversion
 * method against the expected Java‑time / legacy‑time values.
 */
public class OffsetDateTimeConversionsTest {

    private OffsetDateTime odt;
    private Converter converter;

    @Before
    public void setUp() {
        // 2021‑05‑15T12:30:45.123456789+02:00
        odt = OffsetDateTime.of(
                2021, 5, 15,
                12, 30, 45, 123_456_789,
                ZoneOffset.ofHoursMinutes(2, 0));

        // The library provides a public constructor that accepts ConverterOptions.
        // Using the default options (system default zone & time‑zone) is sufficient for our tests.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    public void testToInstant() {
        Instant expected = odt.toInstant();
        assertEquals(expected, OffsetDateTimeConversions.toInstant(odt, converter));
    }

    @Test
    public void testToLong_and_ToDate_and_ToTimestamp() {
        long expectedMillis = odt.toInstant().toEpochMilli();

        assertEquals(expectedMillis, OffsetDateTimeConversions.toLong(odt, converter));

        Date date = OffsetDateTimeConversions.toDate(odt, converter);
        assertEquals(expectedMillis, date.getTime());

        Timestamp ts = OffsetDateTimeConversions.toTimestamp(odt, converter);
        assertEquals(expectedMillis, ts.getTime());
    }

    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = OffsetDateTimeConversions.toAtomicLong(odt, converter);
        assertEquals(odt.toInstant().toEpochMilli(), atomic.get());
    }

    @Test
    public void testToDouble() {
        // double representation of seconds + nanos
        Instant instant = odt.toInstant();
        double expected = BigDecimalConversions.secondsAndNanosToDouble(
                instant.getEpochSecond(),
                instant.getNano()).doubleValue();

        assertEquals(expected, OffsetDateTimeConversions.toDouble(odt, converter), 0.0);
    }

    @Test
    public void testToBigInteger_and_ToBigDecimal() {
        Instant instant = odt.toInstant();

        BigInteger expectedBi = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expectedBi, OffsetDateTimeConversions.toBigInteger(odt, converter));

        BigDecimal expectedBd = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(expectedBd, OffsetDateTimeConversions.toBigDecimal(odt, converter));
    }

    @Test
    public void testToLocalDateTime_LocalDate_LocalTime() {
        ZonedDateTime zdt = OffsetDateTimeConversions.toZonedDateTime(odt, converter);

        assertEquals(zdt.toLocalDateTime(),
                OffsetDateTimeConversions.toLocalDateTime(odt, converter));

        assertEquals(zdt.toLocalDate(),
                OffsetDateTimeConversions.toLocalDate(odt, converter));

        assertEquals(zdt.toLocalTime(),
                OffsetDateTimeConversions.toLocalTime(odt, converter));
    }

    @Test
    public void testToCalendar() {
        Calendar cal = OffsetDateTimeConversions.toCalendar(odt, converter);
        assertEquals(odt.toInstant().toEpochMilli(), cal.getTimeInMillis());
        // The calendar should be created with the ConverterOptions time‑zone.
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = OffsetDateTimeConversions.toSqlDate(odt, converter);
        LocalDate expected = odt.atZoneSameInstant(converter.getOptions().getZoneId()).toLocalDate();
        assertEquals(java.sql.Date.valueOf(expected), sqlDate);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = odt.toInstant().atZone(converter.getOptions().getZoneId());
        assertEquals(expected, OffsetDateTimeConversions.toZonedDateTime(odt, converter));
    }

    @Test
    public void testToOffsetTime() {
        OffsetTime expected = odt.toOffsetTime();
        assertEquals(expected, OffsetDateTimeConversions.toOffsetTime(odt, converter));
    }

    @Test
    public void testToYear_YearMonth_MonthDay() {
        ZoneId zone = converter.getOptions().getZoneId();
        LocalDate localDate = odt.atZoneSameInstant(zone).toLocalDate();

        assertEquals(Year.from(localDate), OffsetDateTimeConversions.toYear(odt, converter));
        assertEquals(YearMonth.from(localDate), OffsetDateTimeConversions.toYearMonth(odt, converter));
        assertEquals(MonthDay.from(localDate), OffsetDateTimeConversions.toMonthDay(odt, converter));
    }

    @Test
    public void testToString() {
        String expected = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        assertEquals(expected, OffsetDateTimeConversions.toString(odt, converter));
    }

    @Test
    public void testToMap() {
        Map<String, Object> map = OffsetDateTimeConversions.toMap(odt, converter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.OFFSET_DATE_TIME));
        assertEquals(OffsetDateTimeConversions.toString(odt, converter), map.get(MapConversions.OFFSET_DATE_TIME));
    }

    @Test
    public void testEdgeCase_NullInput_ShouldThrowClassCastException() {
        // All conversion methods expect an OffsetDateTime; passing null should result in a
        // NullPointerException or ClassCastException as per the implementation.
        try {
            OffsetDateTimeConversions.toInstant(null, converter);
            fail("Expected NullPointerException or ClassCastException");
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof ClassCastException);
        }
    }

    @Test
    public void testDifferentOffsetBehaviour() {
        // Same instant, different offset
        OffsetDateTime differentOffset = odt.withOffsetSameInstant(ZoneOffset.UTC);
        assertEquals(odt.toInstant(), differentOffset.toInstant());

        // Conversions that depend on the zone (e.g., toLocalDate) must respect the converter's zone,
        // not the original offset.
        LocalDate expected = differentOffset.atZoneSameInstant(converter.getOptions().getZoneId()).toLocalDate();
        assertEquals(expected, OffsetDateTimeConversions.toLocalDate(differentOffset, converter));
    }
}
