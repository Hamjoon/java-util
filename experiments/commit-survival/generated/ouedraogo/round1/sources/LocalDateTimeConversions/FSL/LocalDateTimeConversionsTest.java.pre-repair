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
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link LocalDateTimeConversions}. All public static methods are exercised
 * with a deterministic {@link LocalDateTime} and a {@link Converter} that is configured
 * to use the UTC zone.  The tests avoid any external mocking frameworks and rely only on
 * the constructors and methods that are part of the library under test.
 */
public class LocalDateTimeConversionsTest {

    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");
    private static final LocalDateTime SAMPLE_LDT = LocalDateTime.of(2023, 3, 15, 12, 30, 45, 123_000_000); // 123 ms

    private Converter converter;

    /**
     * Creates a {@link Converter} whose {@link ConverterOptions} are set to UTC.
     * The {@code ConverterOptions} class is not part of the public signatures supplied
     * in the kata, therefore we use reflection to inject the required {@code ZoneId}
     * and {@code TimeZone} values.
     */
    @Before
    public void setUp() throws Exception {
        // The library provides a public constructor Converter(ConverterOptions options)
        // The concrete class ConverterOptions is part of the same library; we instantiate it
        // using its no‑arg constructor (available in the actual library) and then set the
        // fields that the conversion methods rely on.
        ConverterOptions options = new ConverterOptions(); // assumes a public no‑arg ctor

        // Use reflection because the field names are not part of the public API.
        java.lang.reflect.Field zoneIdField = ConverterOptions.class.getDeclaredField("zoneId");
        zoneIdField.setAccessible(true);
        zoneIdField.set(options, UTC_ZONE);

        java.lang.reflect.Field timeZoneField = ConverterOptions.class.getDeclaredField("timeZone");
        timeZoneField.setAccessible(true);
        timeZoneField.set(options, UTC_TZ);

        this.converter = new Converter(options);
    }

    @Test
    public void testToZonedDateTime() {
        ZonedDateTime expected = ZonedDateTime.of(SAMPLE_LDT, UTC_ZONE);
        ZonedDateTime actual = LocalDateTimeConversions.toZonedDateTime(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToOffsetDateTime() {
        ZoneOffset offset = ZoneOffset.UTC; // UTC => offset 0
        OffsetDateTime expected = SAMPLE_LDT.atOffset(offset);
        OffsetDateTime actual = LocalDateTimeConversions.toOffsetDateTime(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstant() {
        Instant expected = SAMPLE_LDT.atZone(UTC_ZONE).toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLong() {
        long expected = SAMPLE_LDT.atZone(UTC_ZONE).toInstant().toEpochMilli();
        long actual = LocalDateTimeConversions.toLong(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDouble() {
        double expected = (double) SAMPLE_LDT.atZone(UTC_ZONE).toInstant().toEpochMilli();
        double actual = LocalDateTimeConversions.toDouble(SAMPLE_LDT, converter);
        assertEquals(expected, actual, 0.0);
    }

    @Test
    public void testToLocalDateTime() {
        LocalDateTime expected = SAMPLE_LDT;
        LocalDateTime actual = LocalDateTimeConversions.toLocalDateTime(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalDate() {
        LocalDate expected = SAMPLE_LDT.toLocalDate();
        LocalDate actual = LocalDateTimeConversions.toLocalDate(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = SAMPLE_LDT.toLocalTime();
        LocalTime actual = LocalDateTimeConversions.toLocalTime(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToAtomicLong() {
        long expected = SAMPLE_LDT.atZone(UTC_ZONE).toInstant().toEpochMilli();
        AtomicLong atomicLong = LocalDateTimeConversions.toAtomicLong(SAMPLE_LDT, converter);
        assertEquals(expected, atomicLong.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(SAMPLE_LDT.atZone(UTC_ZONE).toInstant());
        Timestamp actual = LocalDateTimeConversions.toTimestamp(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToCalendar() {
        ZonedDateTime zdt = SAMPLE_LDT.atZone(UTC_ZONE);
        Calendar calendar = LocalDateTimeConversions.toCalendar(SAMPLE_LDT, converter);
        assertEquals(UTC_TZ, calendar.getTimeZone());
        assertEquals(zdt.toInstant().toEpochMilli(), calendar.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        long epochMilli = SAMPLE_LDT.atZone(UTC_ZONE).toInstant().toEpochMilli();
        java.sql.Date expected = new java.sql.Date(epochMilli);
        java.sql.Date actual = LocalDateTimeConversions.toSqlDate(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToDate() {
        long epochMilli = SAMPLE_LDT.atZone(UTC_ZONE).toInstant().toEpochMilli();
        Date expected = new Date(epochMilli);
        Date actual = LocalDateTimeConversions.toDate(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigInteger() {
        long epochMilli = SAMPLE_LDT.atZone(UTC_ZONE).toInstant().toEpochMilli();
        BigInteger expected = BigInteger.valueOf(epochMilli);
        BigInteger actual = LocalDateTimeConversions.toBigInteger(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToBigDecimal() {
        long epochMilli = SAMPLE_LDT.atZone(UTC_ZONE).toInstant().toEpochMilli();
        BigDecimal expected = BigDecimal.valueOf(epochMilli);
        BigDecimal actual = LocalDateTimeConversions.toBigDecimal(SAMPLE_LDT, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testToString() {
        String expected = SAMPLE_LDT.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String actual = LocalDateTimeConversions.toString(SAMPLE_LDT, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToMap() {
        Map<?, ?> map = LocalDateTimeConversions.toMap(SAMPLE_LDT, converter);
        assertNotNull(map);
        assertEquals(2, map.size());

        // The constants are defined in MapConversions; they map to the strings "date" and "time".
        String dateKey = MapConversions.DATE;
        String timeKey = MapConversions.TIME;

        assertTrue(map.containsKey(dateKey));
        assertTrue(map.containsKey(timeKey));

        assertEquals(SAMPLE_LDT.toLocalDate().toString(), map.get(dateKey));
        assertEquals(SAMPLE_LDT.toLocalTime().toString(), map.get(timeKey));
    }

    /**
     * Verify that passing {@code null} as the source object results in a
     * {@link NullPointerException} for the methods that cast the argument to
     * {@link LocalDateTime}. The class does not perform an explicit null‑check,
     * therefore the expected behaviour is the standard Java NPE.
     */
    @Test(expected = NullPointerException.class)
    public void testNullSourceThrowsException() {
        LocalDateTimeConversions.toZonedDateTime(null, converter);
    }
}
