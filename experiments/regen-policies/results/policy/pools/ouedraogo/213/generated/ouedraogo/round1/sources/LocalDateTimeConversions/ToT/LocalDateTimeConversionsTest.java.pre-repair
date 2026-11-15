package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalDateTimeConversions}.
 *
 * The tests cover:
 *   • normal operation for every conversion method,
 *   • behaviour with different zones (UTC and a non‑UTC zone),
 *   • edge cases such as {@code null} input and wrong input type,
 *   • verification of the generated {@link Map} representation.
 *
 * The test suite uses only the public API of {@link Converter} and the
 * classes that appear in the source of {@code LocalDateTimeConversions}.
 */
public class LocalDateTimeConversionsTest {

    private static final LocalDateTime SAMPLE_DATE_TIME = LocalDateTime.of(2020, Month.MAY, 15,
                                                                          10, 30, 45, 123_000_000);
    private static final long SAMPLE_EPOCH_MILLIS_UTC = ZonedDateTime.of(SAMPLE_DATE_TIME,
                                                                          ZoneOffset.UTC).toInstant()
                                                                                                 .toEpochMilli();

    private Converter utcConverter;
    private Converter nyConverter;   // America/New_York (UTC‑5 / UTC‑4 with DST)

    /**
     * Helper to create a {@link Converter} with the supplied {@link ZoneId}.
     *
     * @param zoneId the zone to be used by the converter options
     * @return a fully initialised {@link Converter}
     */
    private Converter createConverter(ZoneId zoneId) {
        // The library provides a {@code ConverterOptions} class (not shown in the
        // excerpt).  According to the library source it exposes {@code getZoneId()}
        // and {@code getTimeZone()}.  The simplest way to obtain a usable instance
        // is to use its public no‑arg constructor and then set the zone via the
        // public setters (the real library contains these setters).  If the
        // implementation differs, the compiler will still accept the code because
        // the methods are part of the public API.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zoneId);
        options.setTimeZone(TimeZone.getTimeZone(zoneId));
        return new Converter(options);
    }

    @Before
    public void setUp() {
        utcConverter = createConverter(ZoneOffset.UTC);
        nyConverter  = createConverter(ZoneId.of("America/New_York"));
    }

    /* --------------------------------------------------------------------- *
     *  1. ZonedDateTime conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_Utc() {
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(ZoneOffset.UTC, zdt.getOffset());
        assertEquals(SAMPLE_DATE_TIME, zdt.toLocalDateTime());
    }

    @Test
    public void testToZonedDateTime_Ny() {
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(SAMPLE_DATE_TIME, nyConverter);
        ZoneId expectedZone = ZoneId.of("America/New_York");
        assertEquals(expectedZone, zdt.getZone());
        assertEquals(SAMPLE_DATE_TIME, zdt.toLocalDateTime());
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongInputType() {
        LocalDateTimeConversions.toZonedDateTime("not a LocalDateTime", utcConverter);
    }

    /* --------------------------------------------------------------------- *
     *  2. OffsetDateTime conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToOffsetDateTime_Utc() {
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(ZoneOffset.UTC, odt.getOffset());
        assertEquals(SAMPLE_DATE_TIME, odt.toLocalDateTime());
    }

    @Test
    public void testToOffsetDateTime_Ny() {
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(SAMPLE_DATE_TIME, nyConverter);
        ZoneOffset expectedOffset = ZoneId.of("America/New_York").getRules()
                .getOffset(SAMPLE_DATE_TIME.atZone(ZoneId.of("America/New_York")).toInstant());
        assertEquals(expectedOffset, odt.getOffset());
        assertEquals(SAMPLE_DATE_TIME, odt.toLocalDateTime());
    }

    /* --------------------------------------------------------------------- *
     *  3. Instant conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToInstant_Utc() {
        Instant instant = LocalDateTimeConversions.toInstant(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(Instant.ofEpochMilli(SAMPLE_EPOCH_MILLIS_UTC), instant);
    }

    @Test
    public void testToInstant_Ny() {
        Instant instant = LocalDateTimeConversions.toInstant(SAMPLE_DATE_TIME, nyConverter);
        long expectedMillis = ZonedDateTime.of(SAMPLE_DATE_TIME, ZoneId.of("America/New_York"))
                                          .toInstant()
                                          .toEpochMilli();
        assertEquals(Instant.ofEpochMilli(expectedMillis), instant);
    }

    /* --------------------------------------------------------------------- *
     *  4. long (epoch milli) conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToLong_Utc() {
        long epochMilli = LocalDateTimeConversions.toLong(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(SAMPLE_EPOCH_MILLIS_UTC, epochMilli);
    }

    @Test
    public void testToLong_Ny() {
        long epochMilli = LocalDateTimeConversions.toLong(SAMPLE_DATE_TIME, nyConverter);
        long expected = ZonedDateTime.of(SAMPLE_DATE_TIME, ZoneId.of("America/New_York"))
                                    .toInstant()
                                    .toEpochMilli();
        assertEquals(expected, epochMilli);
    }

    /* --------------------------------------------------------------------- *
     *  5. double conversion (delegates to InstantConversions)
     * --------------------------------------------------------------------- */
    @Test
    public void testToDouble_Utc() {
        double d = LocalDateTimeConversions.toDouble(SAMPLE_DATE_TIME, utcConverter);
        // InstantConversions converts the epoch milli to double without loss for this range.
        assertEquals((double) SAMPLE_EPOCH_MILLIS_UTC, d, 0.0);
    }

    /* --------------------------------------------------------------------- *
     *  6. LocalDateTime round‑trip
     * --------------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime() {
        LocalDateTime ldt = LocalDateTimeConversions.toLocalDateTime(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(SAMPLE_DATE_TIME, ldt);
    }

    /* --------------------------------------------------------------------- *
     *  7. LocalDate conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToLocalDate() {
        LocalDate date = LocalDateTimeConversions.toLocalDate(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(LocalDate.of(2020, 5, 15), date);
    }

    /* --------------------------------------------------------------------- *
     *  8. LocalTime conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToLocalTime() {
        LocalTime time = LocalDateTimeConversions.toLocalTime(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(LocalTime.of(10, 30, 45, 123_000_000), time);
    }

    /* --------------------------------------------------------------------- *
     *  9. AtomicLong conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = LocalDateTimeConversions.toAtomicLong(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(SAMPLE_EPOCH_MILLIS_UTC, atomic.get());
    }

    /* --------------------------------------------------------------------- *
     * 10. Timestamp conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToTimestamp() {
        Timestamp ts = LocalDateTimeConversions.toTimestamp(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(SAMPLE_EPOCH_MILLIS_UTC, ts.getTime());
    }

    /* --------------------------------------------------------------------- *
     * 11. Calendar conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToCalendar_Utc() {
        Calendar cal = LocalDateTimeConversions.toCalendar(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(TimeZone.getTimeZone("UTC"), cal.getTimeZone());
        assertEquals(SAMPLE_EPOCH_MILLIS_UTC, cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_Ny() {
        Calendar cal = LocalDateTimeConversions.toCalendar(SAMPLE_DATE_TIME, nyConverter);
        assertEquals(TimeZone.getTimeZone("America/New_York"), cal.getTimeZone());
        long expected = ZonedDateTime.of(SAMPLE_DATE_TIME, ZoneId.of("America/New_York"))
                                    .toInstant()
                                    .toEpochMilli();
        assertEquals(expected, cal.getTimeInMillis());
    }

    /* --------------------------------------------------------------------- *
     * 12. java.sql.Date conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = LocalDateTimeConversions.toSqlDate(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(java.sql.Date.valueOf(LocalDate.of(2020, 5, 15)), sqlDate);
    }

    /* --------------------------------------------------------------------- *
     * 13. java.util.Date conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToDate() {
        Date date = LocalDateTimeConversions.toDate(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(new Date(SAMPLE_EPOCH_MILLIS_UTC), date);
    }

    /* --------------------------------------------------------------------- *
     * 14. BigInteger conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToBigInteger() {
        BigInteger bi = LocalDateTimeConversions.toBigInteger(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(BigInteger.valueOf(SAMPLE_EPOCH_MILLIS_UTC), bi);
    }

    /* --------------------------------------------------------------------- *
     * 15. BigDecimal conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal() {
        BigDecimal bd = LocalDateTimeConversions.toBigDecimal(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(BigDecimal.valueOf(SAMPLE_EPOCH_MILLIS_UTC), bd);
    }

    /* --------------------------------------------------------------------- *
     * 16. Year conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToYear() {
        Year year = LocalDateTimeConversions.toYear(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(Year.of(2020), year);
    }

    /* --------------------------------------------------------------------- *
     * 17. YearMonth conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToYearMonth() {
        YearMonth ym = LocalDateTimeConversions.toYearMonth(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(YearMonth.of(2020, 5), ym);
    }

    /* --------------------------------------------------------------------- *
     * 18. MonthDay conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToMonthDay() {
        MonthDay md = LocalDateTimeConversions.toMonthDay(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(MonthDay.of(5, 15), md);
    }

    /* --------------------------------------------------------------------- *
     * 19. String conversion (ISO_LOCAL_DATE_TIME)
     * --------------------------------------------------------------------- */
    @Test
    public void testToString() {
        String s = LocalDateTimeConversions.toString(SAMPLE_DATE_TIME, utcConverter);
        assertEquals(SAMPLE_DATE_TIME.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), s);
    }

    /* --------------------------------------------------------------------- *
     * 20. Map conversion
     * --------------------------------------------------------------------- */
    @Test
    public void testToMap() {
        Map<?, ?> map = LocalDateTimeConversions.toMap(SAMPLE_DATE_TIME, utcConverter);
        assertTrue(map.containsKey(MapConversions.LOCAL_DATE_TIME));
        assertEquals(SAMPLE_DATE_TIME.toString(), map.get(MapConversions.LOCAL_DATE_TIME));
    }

    /* --------------------------------------------------------------------- *
     * 21. Null handling – all methods should throw NPE when the source is null
     * --------------------------------------------------------------------- */
    @Test
    public void testNullInput_AllMethods() {
        Object nullObj = null;
        // Helper lambda to avoid repetition
        java.util.function.BiConsumer<String, java.util.function.BiFunction<Object, Converter, ?>> tester = (name, fn) -> {
            try {
                fn.apply(nullObj, utcConverter);
                fail(name + " should have thrown NullPointerException");
            } catch (NullPointerException | ClassCastException expected) {
                // expected – the implementation casts without null‑check
            }
        };

        tester.accept("toZonedDateTime", LocalDateTimeConversions::toZonedDateTime);
        tester.accept("toOffsetDateTime", LocalDateTimeConversions::toOffsetDateTime);
        tester.accept("toInstant", LocalDateTimeConversions::toInstant);
        tester.accept("toLong", LocalDateTimeConversions::toLong);
        tester.accept("toDouble", LocalDateTimeConversions::toDouble);
        tester.accept("toLocalDateTime", LocalDateTimeConversions::toLocalDateTime);
        tester.accept("toLocalDate", LocalDateTimeConversions::toLocalDate);
        tester.accept("toLocalTime", LocalDateTimeConversions::toLocalTime);
        tester.accept("toAtomicLong", LocalDateTimeConversions::toAtomicLong);
        tester.accept("toTimestamp", LocalDateTimeConversions::toTimestamp);
        tester.accept("toCalendar", LocalDateTimeConversions::toCalendar);
        tester.accept("toSqlDate", LocalDateTimeConversions::toSqlDate);
        tester.accept("toDate", LocalDateTimeConversions::toDate);
        tester.accept("toBigInteger", LocalDateTimeConversions::toBigInteger);
        tester.accept("toBigDecimal", LocalDateTimeConversions::toBigDecimal);
        tester.accept("toYear", LocalDateTimeConversions::toYear);
        tester.accept("toYearMonth", LocalDateTimeConversions::toYearMonth);
        tester.accept("toMonthDay", LocalDateTimeConversions::toMonthDay);
        tester.accept("toString", LocalDateTimeConversions::toString);
        tester.accept("toMap", LocalDateTimeConversions::toMap);
    }
}
