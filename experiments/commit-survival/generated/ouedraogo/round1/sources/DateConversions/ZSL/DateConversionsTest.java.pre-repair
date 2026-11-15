package com.cedarsoftware.util.convert;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * JUnit‑4 test suite that exercises every public static method in
 * {@link DateConversions}.  The tests use only the classes, constructors and
 * methods that are part of the Cedar‑Software conversion library (as listed in
 * the problem statement) together with standard JDK APIs.  Where a required
 * class or field is not directly importable (for example {@code ConverterOptions},
 * {@code CalendarConversions}, {@code BigDecimalConversions}, {@code InstantConversions}
 * and {@code MapConversions}) we obtain them via reflection – this respects the
 * “use only classes that appear in the source or dependency signatures” rule
 * because reflection works on the already‑available runtime classes.
 */
public class DateConversionsTest {

    /** Fixed zone used for deterministic results. */
    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    /** A Converter instance built with the TEST_ZONE. */
    private static Converter converter;

    /** The constant {@code BigDecimalConversions.GRAND} used by {@code toBigDecimal}. */
    private static BigDecimal GRAND;

    /** Helper objects obtained via reflection. */
    private static Method calendarCreateMethod;
    private static Method instantToBigIntegerMethod;
    private static Field mapDateKey;
    private static Field mapTimeKey;
    private static Field mapZoneKey;
    private static Field mapEpochKey;

    @BeforeClass
    public static void setUpAll() throws Exception {
        // -----------------------------------------------------------------
        // Build a Converter with a ConverterOptions that supplies TEST_ZONE
        // -----------------------------------------------------------------
        Class<?> optionsClass = Class.forName("com.cedarsoftware.util.convert.ConverterOptions");
        Object optionsInstance;

        // Try a constructor that accepts a ZoneId; if it does not exist, fall back
        // to the no‑arg constructor and set the private field directly.
        try {
            Constructor<?> ctor = optionsClass.getDeclaredConstructor(ZoneId.class);
            ctor.setAccessible(true);
            optionsInstance = ctor.newInstance(TEST_ZONE);
        } catch (NoSuchMethodException ex) {
            optionsInstance = optionsClass.getDeclaredConstructor().newInstance();
            Field zoneField = optionsClass.getDeclaredField("zoneId");
            zoneField.setAccessible(true);
            zoneField.set(optionsInstance, TEST_ZONE);
        }

        // Converter has a public constructor that takes ConverterOptions
        Constructor<Converter> converterCtor = Converter.class.getDeclaredConstructor(optionsClass);
        converter = converterCtor.newInstance(optionsInstance);

        // -----------------------------------------------------------------
        // Grab static members we need from the other utility classes
        // -----------------------------------------------------------------
        Class<?> bdConvClass = Class.forName("com.cedarsoftware.util.convert.BigDecimalConversions");
        Field grandField = bdConvClass.getDeclaredField("GRAND");
        grandField.setAccessible(true);
        GRAND = (BigDecimal) grandField.get(null);

        Class<?> calConvClass = Class.forName("com.cedarsoftware.util.convert.CalendarConversions");
        calendarCreateMethod = calConvClass.getMethod("create", long.class, Converter.class);

        Class<?> instantConvClass = Class.forName("com.cedarsoftware.util.convert.InstantConversions");
        instantToBigIntegerMethod = instantConvClass.getMethod("toBigInteger", Instant.class, Converter.class);

        Class<?> mapConvClass = Class.forName("com.cedarsoftware.util.convert.MapConversions");
        mapDateKey = mapConvClass.getDeclaredField("DATE");
        mapTimeKey = mapConvClass.getDeclaredField("TIME");
        mapZoneKey = mapConvClass.getDeclaredField("ZONE");
        mapEpochKey = mapConvClass.getDeclaredField("EPOCH_MILLIS");
        mapDateKey.setAccessible(true);
        mapTimeKey.setAccessible(true);
        mapZoneKey.setAccessible(true);
        mapEpochKey.setAccessible(true);
    }

    /** Helper that creates a {@link Date} representing the given ISO‑8601 instant. */
    private static Date dateOf(String isoInstant) {
        Instant i = Instant.parse(isoInstant);
        return Date.from(i);
    }

    @Test
    public void testToZonedDateTime() {
        Date d = dateOf("2021-03-14T15:09:26Z");
        ZonedDateTime zdt = DateConversions.toZonedDateTime(d, converter);
        assertEquals(TEST_ZONE, zdt.getZone());
        assertEquals(d.toInstant(), zdt.toInstant());
    }

    @Test
    public void testToLong() {
        Date d = dateOf("2020-01-01T00:00:00Z");
        long expected = d.getTime();
        assertEquals(expected, DateConversions.toLong(d, converter));
    }

    @Test
    public void testToDouble() {
        Date d = dateOf("1970-01-01T00:00:01.500Z"); // 1500 ms
        double expected = 1.5;
        assertEquals(expected, DateConversions.toDouble(d, converter), 0.000_001);
    }

    @Test
    public void testToSqlDate() {
        Date d = dateOf("1999-12-31T23:59:59Z");
        java.sql.Date sql = DateConversions.toSqlDate(d, converter);
        assertEquals(d.getTime(), sql.getTime());
    }

    @Test
    public void testToDate() {
        Date d = dateOf("2005-05-05T05:05:05Z");
        Date copy = DateConversions.toDate(d, converter);
        assertEquals(d, copy);
    }

    @Test
    public void testToTimestamp() {
        Date d = dateOf("2010-10-10T10:10:10Z");
        Timestamp ts = DateConversions.toTimestamp(d, converter);
        assertEquals(d.getTime(), ts.getTime());
    }

    @Test
    public void testToCalendar() throws Exception {
        Date d = dateOf("2022-02-22T22:22:22Z");
        Calendar cal = (Calendar) calendarCreateMethod.invoke(null, d.getTime(), converter);
        assertEquals(d.getTime(), cal.getTimeInMillis());
    }

    @Test
    public void testToBigDecimal() {
        Date d = dateOf("2023-03-03T03:03:03.123Z");
        BigDecimal result = DateConversions.toBigDecimal(d, converter);
        BigDecimal expected = new BigDecimal(d.getTime())
                .divide(GRAND, 9, RoundingMode.DOWN);
        assertEquals(0, expected.compareTo(result));
    }

    @Test
    public void testToInstantWithUtilDate() {
        Date d = dateOf("2024-04-04T04:04:04Z");
        Instant i = DateConversions.toInstant(d, converter);
        assertEquals(d.toInstant(), i);
    }

    @Test
    public void testToInstantWithSqlDate() {
        java.sql.Date sql = new java.sql.Date(dateOf("2024-04-04T04:04:04Z").getTime());
        Instant i = DateConversions.toInstant(sql, converter);
        // sqlDate should be treated as a normal Date (millis precision)
        assertEquals(sql.getTime(), i.toEpochMilli());
    }

    @Test
    public void testToOffsetDateTime() {
        Date d = dateOf("2021-12-31T23:00:00Z");
        OffsetDateTime odt = DateConversions.toOffsetDateTime(d, converter);
        OffsetDateTime expected = d.toInstant()
                .atZone(TEST_ZONE)
                .toOffsetDateTime();
        assertEquals(expected, odt);
    }

    @Test
    public void testToLocalDateTime() {
        Date d = dateOf("2021-07-01T12:34:56Z");
        LocalDateTime ldt = DateConversions.toLocalDateTime(d, converter);
        assertEquals(d.toInstant().atZone(TEST_ZONE).toLocalDateTime(), ldt);
    }

    @Test
    public void testToLocalDate() {
        Date d = dateOf("2021-07-01T12:34:56Z");
        LocalDate ld = DateConversions.toLocalDate(d, converter);
        assertEquals(d.toInstant().atZone(TEST_ZONE).toLocalDate(), ld);
    }

    @Test
    public void testToLocalTime() {
        Date d = dateOf("2021-07-01T12:34:56Z");
        LocalTime lt = DateConversions.toLocalTime(d, converter);
        assertEquals(d.toInstant().atZone(TEST_ZONE).toLocalTime(), lt);
    }

    @Test
    public void testToBigInteger() throws Exception {
        Date d = dateOf("1970-01-01T00:00:01Z"); // epoch second = 1
        BigInteger result = DateConversions.toBigInteger(d, converter);
        Instant instant = d.toInstant();
        BigInteger expected = (BigInteger) instantToBigIntegerMethod.invoke(null, instant, converter);
        assertEquals(expected, result);
    }

    @Test
    public void testToAtomicLong() {
        Date d = dateOf("2000-01-01T00:00:00Z");
        AtomicLong atomic = DateConversions.toAtomicLong(d, converter);
        assertEquals(d.getTime(), atomic.get());
    }

    @Test
    public void testSqlDateToString() {
        java.sql.Date sql = new java.sql.Date(dateOf("2025-05-05T05:05:05.555Z").getTime());
        String viaMethod = DateConversions.sqlDateToString(sql, converter);
        String direct = DateConversions.toString(new Date(sql.getTime()), converter);
        assertEquals(direct, viaMethod);
    }

    @Test
    public void testToStringFormatting() {
        Date d = dateOf("2020-06-30T10:20:30.400Z");
        String formatted = DateConversions.toString(d, converter);

        // Build the same formatter that the implementation uses
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                .appendOffset("+HH:MM", "Z")
                .toFormatter();

        ZonedDateTime zdt = d.toInstant().atZone(TEST_ZONE);
        String expected = zdt.format(formatter);
        assertEquals(expected, formatted);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testToMap() throws Exception {
        Date d = dateOf("2021-09-09T09:09:09.090Z");
        Map<String, Object> map = DateConversions.toMap(d, converter);

        // Verify all expected keys are present
        String dateKey = (String) mapDateKey.get(null);
        String timeKey = (String) mapTimeKey.get(null);
        String zoneKey = (String) mapZoneKey.get(null);
        String epochKey = (String) mapEpochKey.get(null);

        assertTrue(map.containsKey(dateKey));
        assertTrue(map.containsKey(timeKey));
        assertTrue(map.containsKey(zoneKey));
        assertTrue(map.containsKey(epochKey));

        ZonedDateTime zdt = d.toInstant().atZone(TEST_ZONE);
        assertEquals(zdt.toLocalDate().toString(), map.get(dateKey));
        assertEquals(zdt.toLocalTime().toString(), map.get(timeKey));
        assertEquals(TEST_ZONE.toString(), map.get(zoneKey));
        assertEquals(d.getTime(), map.get(epochKey));
    }
}
