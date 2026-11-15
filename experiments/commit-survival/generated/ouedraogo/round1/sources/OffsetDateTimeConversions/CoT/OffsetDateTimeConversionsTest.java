package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@code OffsetDateTimeConversions}.
 *
 * The tests are written in the same package as the class under test
 * so that the package‑private static methods are accessible.
 *
 * Each public static method of {@code OffsetDateTimeConversions}
 * is exercised with a normal case, a set of edge cases and the
 * expected failure modes (null argument, wrong type, extreme dates).
 */
public class OffsetDateTimeConversionsTest {

    private Converter converter;

    @Before
    public void setUp() {
        // ConverterOptions has a public no‑arg constructor in the library.
        // The default options use the JVM default time‑zone and zone‑id,
        // which is sufficient for our tests.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------------- */
    /*  Helper to create a typical OffsetDateTime used by many tests          */
    /* --------------------------------------------------------------------- */
    private static OffsetDateTime sampleOffsetDateTime() {
        return OffsetDateTime.of(
                LocalDateTime.of(2020, Month.JANUARY, 2, 3, 4, 5, 123_456_789),
                ZoneOffset.ofHoursMinutes(5, 30));
    }

    /* --------------------------------------------------------------------- */
    /*  1. toInstant                                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToInstant_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        Instant expected = odt.toInstant();
        Instant actual = OffsetDateTimeConversions.toInstant(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullArgument() {
        OffsetDateTimeConversions.toInstant(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToInstant_WrongType() {
        OffsetDateTimeConversions.toInstant("not an OffsetDateTime", converter);
    }

    @Test
    public void testToInstant_ExtremeMaximum() {
        OffsetDateTime odt = OffsetDateTime.MAX;
        Instant expected = odt.toInstant();
        Instant actual = OffsetDateTimeConversions.toInstant(odt, converter);
        assertEquals(expected, actual);
    }

    @Test
    public void testToInstant_ExtremeMinimum() {
        OffsetDateTime odt = OffsetDateTime.MIN;
        Instant expected = odt.toInstant();
        Instant actual = OffsetDateTimeConversions.toInstant(odt, converter);
        assertEquals(expected, actual);
    }

    /* --------------------------------------------------------------------- */
    /*  2. toLong                                                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToLong_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        long expected = odt.toInstant().toEpochMilli();
        long actual = OffsetDateTimeConversions.toLong(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullArgument() {
        OffsetDateTimeConversions.toLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLong_WrongType() {
        OffsetDateTimeConversions.toLong(42, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  3. toLocalDateTime                                                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToLocalDateTime_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        LocalDateTime expected = odt.atZone(converter.getOptions().getZoneId()).toLocalDateTime();
        LocalDateTime actual = OffsetDateTimeConversions.toLocalDateTime(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullArgument() {
        OffsetDateTimeConversions.toLocalDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalDateTime_WrongType() {
        OffsetDateTimeConversions.toLocalDateTime(new Object(), converter);
    }

    /* --------------------------------------------------------------------- */
    /*  4. toLocalDate                                                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToLocalDate_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        LocalDate expected = odt.atZone(converter.getOptions().getZoneId()).toLocalDate();
        LocalDate actual = OffsetDateTimeConversions.toLocalDate(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullArgument() {
        OffsetDateTimeConversions.toLocalDate(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalDate_WrongType() {
        OffsetDateTimeConversions.toLocalDate("string", converter);
    }

    /* --------------------------------------------------------------------- */
    /*  5. toLocalTime                                                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToLocalTime_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        LocalTime expected = odt.atZone(converter.getOptions().getZoneId()).toLocalTime();
        LocalTime actual = OffsetDateTimeConversions.toLocalTime(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalTime_NullArgument() {
        OffsetDateTimeConversions.toLocalTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToLocalTime_WrongType() {
        OffsetDateTimeConversions.toLocalTime(Boolean.TRUE, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  6. toAtomicLong                                                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToAtomicLong_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        long expected = odt.toInstant().toEpochMilli();
        AtomicLong actual = OffsetDateTimeConversions.toAtomicLong(odt, converter);
        assertEquals(expected, actual.get());
    }

    @Test(expected = NullPointerException.class)
    public void testToAtomicLong_NullArgument() {
        OffsetDateTimeConversions.toAtomicLong(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToAtomicLong_WrongType() {
        OffsetDateTimeConversions.toAtomicLong(123L, converter);
    }

    /* --------------------------------------------------------------------- */
    /*  7. toTimestamp                                                        */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToTimestamp_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        Timestamp expected = Timestamp.from(odt.toInstant());
        Timestamp actual = OffsetDateTimeConversions.toTimestamp(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToTimestamp_NullArgument() {
        OffsetDateTimeConversions.toTimestamp(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToTimestamp_WrongType() {
        OffsetDateTimeConversions.toTimestamp(Instant.now(), converter);
    }

    /* --------------------------------------------------------------------- */
    /*  8. toCalendar                                                         */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToCalendar_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        Calendar cal = OffsetDateTimeConversions.toCalendar(odt, converter);
        assertEquals(odt.toInstant().toEpochMilli(), cal.getTimeInMillis());

        // Verify that the calendar’s time‑zone matches the one supplied by the converter.
        assertEquals(converter.getOptions().getTimeZone(), cal.getTimeZone());
    }

    @Test(expected = NullPointerException.class)
    public void testToCalendar_NullArgument() {
        OffsetDateTimeConversions.toCalendar(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToCalendar_WrongType() {
        OffsetDateTimeConversions.toCalendar(LocalDate.now(), converter);
    }

    /* --------------------------------------------------------------------- */
    /*  9. toSqlDate                                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToSqlDate_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        java.sql.Date expected = new java.sql.Date(odt.toInstant().toEpochMilli());
        java.sql.Date actual = OffsetDateTimeConversions.toSqlDate(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToSqlDate_NullArgument() {
        OffsetDateTimeConversions.toSqlDate(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToSqlDate_WrongType() {
        OffsetDateTimeConversions.toSqlDate(Boolean.FALSE, converter);
    }

    /* --------------------------------------------------------------------- */
    /* 10. toZonedDateTime                                                    */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToZonedDateTime_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        ZonedDateTime expected = odt.toInstant().atZone(converter.getOptions().getZoneId());
        ZonedDateTime actual = OffsetDateTimeConversions.toZonedDateTime(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullArgument() {
        OffsetDateTimeConversions.toZonedDateTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToZonedDateTime_WrongType() {
        OffsetDateTimeConversions.toZonedDateTime(12345, converter);
    }

    @Test
    public void testToZonedDateTime_WithDifferentOffset() {
        OffsetDateTime odt = OffsetDateTime.of(
                LocalDateTime.of(2021, 12, 31, 23, 59, 59),
                ZoneOffset.ofHours(-4));
        // The conversion must use the converter’s ZoneId, not the original offset.
        ZonedDateTime expected = odt.toInstant().atZone(converter.getOptions().getZoneId());
        ZonedDateTime actual = OffsetDateTimeConversions.toZonedDateTime(odt, converter);
        assertEquals(expected, actual);
        assertNotEquals(odt.getOffset(), actual.getOffset());
    }

    /* --------------------------------------------------------------------- */
    /* 11. toDate                                                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToDate_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        Date expected = new Date(odt.toInstant().toEpochMilli());
        Date actual = OffsetDateTimeConversions.toDate(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToDate_NullArgument() {
        OffsetDateTimeConversions.toDate(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDate_WrongType() {
        OffsetDateTimeConversions.toDate(new Object(), converter);
    }

    /* --------------------------------------------------------------------- */
    /* 12. toBigInteger                                                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToBigInteger_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        // InstantConversions converts an Instant to a BigInteger representing epoch‑millis.
        // We reproduce the same logic here (the library’s conversion uses the same value).
        BigInteger expected = BigInteger.valueOf(odt.toInstant().toEpochMilli());
        BigInteger actual = OffsetDateTimeConversions.toBigInteger(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToBigInteger_NullArgument() {
        OffsetDateTimeConversions.toBigInteger(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigInteger_WrongType() {
        OffsetDateTimeConversions.toBigInteger("oops", converter);
    }

    /* --------------------------------------------------------------------- */
    /* 13. toBigDecimal                                                       */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToBigDecimal_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        // The library converts the Instant to a BigDecimal representing epoch seconds with nanos fraction.
        Instant instant = odt.toInstant();
        BigDecimal expected = BigDecimal.valueOf(instant.getEpochSecond())
                .add(BigDecimal.valueOf(instant.getNano(), 9));
        BigDecimal actual = OffsetDateTimeConversions.toBigDecimal(odt, converter);
        assertEquals(0, expected.compareTo(actual));
    }

    @Test(expected = NullPointerException.class)
    public void testToBigDecimal_NullArgument() {
        OffsetDateTimeConversions.toBigDecimal(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToBigDecimal_WrongType() {
        OffsetDateTimeConversions.toBigDecimal(Instant.now(), converter);
    }

    /* --------------------------------------------------------------------- */
    /* 14. toOffsetTime                                                      */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToOffsetTime_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        OffsetTime expected = odt.toOffsetTime();
        OffsetTime actual = OffsetDateTimeConversions.toOffsetTime(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToOffsetTime_NullArgument() {
        OffsetDateTimeConversions.toOffsetTime(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToOffsetTime_WrongType() {
        OffsetDateTimeConversions.toOffsetTime(LocalDate.now(), converter);
    }

    /* --------------------------------------------------------------------- */
    /* 15. toString                                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToString_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        String expected = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String actual = OffsetDateTimeConversions.toString(odt, converter);
        assertEquals(expected, actual);
    }

    @Test(expected = NullPointerException.class)
    public void testToString_NullArgument() {
        OffsetDateTimeConversions.toString(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToString_WrongType() {
        OffsetDateTimeConversions.toString(123, converter);
    }

    /* --------------------------------------------------------------------- */
    /* 16. toMap                                                             */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToMap_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        Map<String, Object> map = OffsetDateTimeConversions.toMap(odt, converter);

        ZonedDateTime zdt = odt.toInstant().atZone(converter.getOptions().getZoneId());

        assertEquals(zdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(zdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(zdt.getOffset().toString(), map.get(MapConversions.OFFSET));
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullArgument() {
        OffsetDateTimeConversions.toMap(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToMap_WrongType() {
        OffsetDateTimeConversions.toMap(new java.util.Date(), converter);
    }

    /* --------------------------------------------------------------------- */
    /* 17. toDouble                                                          */
    /* --------------------------------------------------------------------- */
    @Test
    public void testToDouble_Normal() {
        OffsetDateTime odt = sampleOffsetDateTime();
        Instant instant = odt.toInstant();
        double expected = BigDecimalConversions.secondsAndNanosToDouble(
                instant.getEpochSecond(),
                instant.getNano()).doubleValue();

        double actual = OffsetDateTimeConversions.toDouble(odt, converter);
        assertEquals(expected, actual, 0.0);
    }

    @Test(expected = NullPointerException.class)
    public void testToDouble_NullArgument() {
        OffsetDateTimeConversions.toDouble(null, converter);
    }

    @Test(expected = ClassCastException.class)
    public void testToDouble_WrongType() {
        OffsetDateTimeConversions.toDouble(Boolean.FALSE, converter);
    }

    @Test
    public void testToDouble_EpochZero() {
        OffsetDateTime odt = OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC);
        double actual = OffsetDateTimeConversions.toDouble(odt, converter);
        assertEquals(0.0, actual, 0.0);
    }
}
