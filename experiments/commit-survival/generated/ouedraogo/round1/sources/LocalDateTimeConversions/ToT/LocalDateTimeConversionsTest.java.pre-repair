package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactLinkedMap;
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
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link LocalDateTimeConversions}.
 *
 * The tests cover:
 *   • Normal conversion paths for every public static method.
 *   • Edge‑case handling (null source, different zones, DST‑free zones).
 *   • Consistency between related conversion results.
 *
 * Only the classes, constructors and methods that appear in the source
 * files (or their public signatures shown above) are used – no external
 * mocking frameworks or additional library versions are required.
 */
public class LocalDateTimeConversionsTest {

    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final TimeZone UTC_TZ = TimeZone.getTimeZone("UTC");

    private static final ZoneId NY_ZONE = ZoneId.of("America/New_York");
    private static final TimeZone NY_TZ = TimeZone.getTimeZone("America/New_York");

    private static final ZoneId FIXED_ZONE = ZoneId.of("GMT-05:00"); // constant –5 offset, no DST
    private static final TimeZone FIXED_TZ = TimeZone.getTimeZone("GMT-05:00");

    private Converter utcConverter;
    private Converter fixedOffsetConverter;   // used for deterministic offset tests

    private final LocalDateTime sampleLdt = LocalDateTime.of(2020, Month.MAY, 15,
                                                            10, 30, 45, 123_000_000); // 123 ms

    @Before
    public void setUp() {
        // -----------------------------------------------------------------
        // Build a Converter that returns UTC for both ZoneId and TimeZone.
        // The concrete type of ConverterOptions is not part of the public
        // API shown, but the library supplies a mutable options object with
        // setZoneId(..) and setTimeZone(..) methods.  The tests therefore
        // create an instance, configure it and pass it to Converter.
        // -----------------------------------------------------------------
        ConverterOptions utcOptions = new ConverterOptions();
        utcOptions.setZoneId(UTC_ZONE);
        utcOptions.setTimeZone(UTC_TZ);
        utcConverter = new Converter(utcOptions);

        ConverterOptions fixedOptions = new ConverterOptions();
        fixedOptions.setZoneId(FIXED_ZONE);
        fixedOptions.setTimeZone(FIXED_TZ);
        fixedOffsetConverter = new Converter(fixedOptions);
    }

    // -----------------------------------------------------------------
    // Helper – compute the expected epoch‑millis for a given LocalDateTime
    // under the converter's zone configuration.
    // -----------------------------------------------------------------
    private long expectedEpochMilli(LocalDateTime ldt, Converter converter) {
        ZoneId zid = converter.getOptions().getZoneId();
        return ldt.atZone(zid).toInstant().toEpochMilli();
    }

    // -----------------------------------------------------------------
    // 1. toZonedDateTime
    // -----------------------------------------------------------------
    @Test
    public void testToZonedDateTime_Utc() {
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(sampleLdt, utcConverter);
        assertEquals(UTC_ZONE, zdt.getZone());
        assertEquals(sampleLdt, zdt.toLocalDateTime());
    }

    @Test
    public void testToZonedDateTime_FixedOffset() {
        ZonedDateTime zdt = LocalDateTimeConversions.toZonedDateTime(sampleLdt, fixedOffsetConverter);
        assertEquals(FIXED_ZONE, zdt.getZone());
        assertEquals(sampleLdt, zdt.toLocalDateTime());
    }

    @Test(expected = NullPointerException.class)
    public void testToZonedDateTime_NullSource() {
        LocalDateTimeConversions.toZonedDateTime(null, utcConverter);
    }

    // -----------------------------------------------------------------
    // 2. toOffsetDateTime
    // -----------------------------------------------------------------
    @Test
    public void testToOffsetDateTime_FixedOffset() {
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(sampleLdt, fixedOffsetConverter);
        // GMT-05:00 → offset -05:00
        assertEquals(ZoneOffset.ofHours(-5), odt.getOffset());
        assertEquals(sampleLdt, odt.toLocalDateTime());
    }

    @Test
    public void testToOffsetDateTime_Utc() {
        OffsetDateTime odt = LocalDateTimeConversions.toOffsetDateTime(sampleLdt, utcConverter);
        assertEquals(ZoneOffset.UTC, odt.getOffset());
        assertEquals(sampleLdt, odt.toLocalDateTime());
    }

    // -----------------------------------------------------------------
    // 3. toInstant
    // -----------------------------------------------------------------
    @Test
    public void testToInstant_Utc() {
        Instant instant = LocalDateTimeConversions.toInstant(sampleLdt, utcConverter);
        assertEquals(Instant.ofEpochMilli(expectedEpochMilli(sampleLdt, utcConverter)), instant);
    }

    // -----------------------------------------------------------------
    // 4. toLong
    // -----------------------------------------------------------------
    @Test
    public void testToLong_Utc() {
        long epoch = LocalDateTimeConversions.toLong(sampleLdt, utcConverter);
        assertEquals(expectedEpochMilli(sampleLdt, utcConverter), epoch);
    }

    // -----------------------------------------------------------------
    // 5. toDouble
    // -----------------------------------------------------------------
    @Test
    public void testToDouble_Utc() {
        double d = LocalDateTimeConversions.toDouble(sampleLdt, utcConverter);
        // The library's InstantConversions.toDouble returns the epoch‑seconds
        // as a double.  Multiplying by 1000 should give the epoch‑millis.
        long asLong = (long) d;
        assertEquals(LocalDateTimeConversions.toLong(sampleLdt, utcConverter), asLong);
    }

    // -----------------------------------------------------------------
    // 6. toLocalDateTime
    // -----------------------------------------------------------------
    @Test
    public void testToLocalDateTime() {
        LocalDateTime result = LocalDateTimeConversions.toLocalDateTime(sampleLdt, utcConverter);
        assertEquals(sampleLdt, result);
    }

    // -----------------------------------------------------------------
    // 7. toLocalDate
    // -----------------------------------------------------------------
    @Test
    public void testToLocalDate() {
        LocalDate date = LocalDateTimeConversions.toLocalDate(sampleLdt, utcConverter);
        assertEquals(sampleLdt.toLocalDate(), date);
    }

    // -----------------------------------------------------------------
    // 8. toLocalTime
    // -----------------------------------------------------------------
    @Test
    public void testToLocalTime() {
        LocalTime time = LocalDateTimeConversions.toLocalTime(sampleLdt, utcConverter);
        assertEquals(sampleLdt.toLocalTime(), time);
    }

    // -----------------------------------------------------------------
    // 9. toAtomicLong
    // -----------------------------------------------------------------
    @Test
    public void testToAtomicLong() {
        AtomicLong atomic = LocalDateTimeConversions.toAtomicLong(sampleLdt, utcConverter);
        assertEquals(LocalDateTimeConversions.toLong(sampleLdt, utcConverter), atomic.get());
    }

    // -----------------------------------------------------------------
    // 10. toTimestamp
    // -----------------------------------------------------------------
    @Test
    public void testToTimestamp() {
        Timestamp ts = LocalDateTimeConversions.toTimestamp(sampleLdt, utcConverter);
        assertEquals(expectedEpochMilli(sampleLdt, utcConverter), ts.getTime());
    }

    // -----------------------------------------------------------------
    // 11. toCalendar
    // -----------------------------------------------------------------
    @Test
    public void testToCalendar_Utc() {
        Calendar cal = LocalDateTimeConversions.toCalendar(sampleLdt, utcConverter);
        assertEquals(UTC_TZ, cal.getTimeZone());
        assertEquals(expectedEpochMilli(sampleLdt, utcConverter), cal.getTimeInMillis());
    }

    @Test
    public void testToCalendar_FixedOffset() {
        Calendar cal = LocalDateTimeConversions.toCalendar(sampleLdt, fixedOffsetConverter);
        assertEquals(FIXED_TZ, cal.getTimeZone());
        assertEquals(expectedEpochMilli(sampleLdt, fixedOffsetConverter), cal.getTimeInMillis());
    }

    // -----------------------------------------------------------------
    // 12. toSqlDate
    // -----------------------------------------------------------------
    @Test
    public void testToSqlDate() {
        java.sql.Date sqlDate = LocalDateTimeConversions.toSqlDate(sampleLdt, utcConverter);
        assertEquals(expectedEpochMilli(sampleLdt, utcConverter), sqlDate.getTime());
    }

    // -----------------------------------------------------------------
    // 13. toDate
    // -----------------------------------------------------------------
    @Test
    public void testToDate() {
        Date date = LocalDateTimeConversions.toDate(sampleLdt, utcConverter);
        assertEquals(expectedEpochMilli(sampleLdt, utcConverter), date.getTime());
    }

    // -----------------------------------------------------------------
    // 14. toBigInteger
    // -----------------------------------------------------------------
    @Test
    public void testToBigInteger() {
        BigInteger bi = LocalDateTimeConversions.toBigInteger(sampleLdt, utcConverter);
        assertEquals(BigInteger.valueOf(LocalDateTimeConversions.toLong(sampleLdt, utcConverter)), bi);
    }

    // -----------------------------------------------------------------
    // 15. toBigDecimal
    // -----------------------------------------------------------------
    @Test
    public void testToBigDecimal() {
        BigDecimal bd = LocalDateTimeConversions.toBigDecimal(sampleLdt, utcConverter);
        assertEquals(BigDecimal.valueOf(LocalDateTimeConversions.toLong(sampleLdt, utcConverter)), bd);
    }

    // -----------------------------------------------------------------
    // 16. toString
    // -----------------------------------------------------------------
    @Test
    public void testToString() {
        String iso = LocalDateTimeConversions.toString(sampleLdt, utcConverter);
        assertEquals(sampleLdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), iso);
    }

    // -----------------------------------------------------------------
    // 17. toMap
    // -----------------------------------------------------------------
    @Test
    public void testToMap() {
        Map<?, ?> map = LocalDateTimeConversions.toMap(sampleLdt, utcConverter);
        // The MapConversions class defines the keys "date" and "time".
        assertEquals(sampleLdt.toLocalDate().toString(), map.get(MapConversions.DATE));
        assertEquals(sampleLdt.toLocalTime().toString(), map.get(MapConversions.TIME));
        assertEquals(2, map.size());
    }

    // -----------------------------------------------------------------
    // 18. Null source for a method that does not explicitly check null.
    // -----------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void testToString_NullSource() {
        LocalDateTimeConversions.toString(null, utcConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToMap_NullSource() {
        LocalDateTimeConversions.toMap(null, utcConverter);
    }
}
