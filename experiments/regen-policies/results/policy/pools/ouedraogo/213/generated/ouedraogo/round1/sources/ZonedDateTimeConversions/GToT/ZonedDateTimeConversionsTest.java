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

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ZonedDateTimeConversions}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic happy‑path conversion for every public static method.</li>
 *   <li>Edge‑case handling such as {@code null} arguments (expected NPE) and extreme epoch values.</li>
 *   <li>Correctness when the {@link Converter} is configured with a different target {@link ZoneId}.</li>
 *   <li>Verification that the {@code toMap} representation contains the expected key/value.</li>
 * </ul>
 *
 * The {@link Converter} class is part of the library under test; it provides a
 * {@link ConverterOptions} instance that supplies the target {@link ZoneId}
 * and {@link java.util.TimeZone}.  The default constructor yields the system
 * default zone – sufficient for most tests – but a helper method creates a
 * {@code Converter} whose target zone is forced to UTC so that zone‑adjustment
 * logic can be exercised.
 */
public class ZonedDateTimeConversionsTest {

    /** Sample instant used by many tests – 2022‑03‑14T15:09:26.535Z */
    private static final Instant SAMPLE_INSTANT = Instant.parse("2022-03-14T15:09:26.535Z");

    /** A {@link ZonedDateTime} in the system default zone based on {@link #SAMPLE_INSTANT}. */
    private ZonedDateTime sampleZdtDefault;

    /** A {@link Converter} that uses the system default zone (the simplest case). */
    private Converter defaultConverter;

    /** A {@link Converter} that forces the target zone to UTC – used to test zone conversion. */
    private Converter utcConverter;

    @Before
    public void setUp() {
        // System‑default zone version of the sample instant
        sampleZdtDefault = ZonedDateTime.ofInstant(SAMPLE_INSTANT, ZoneId.systemDefault());

        // Default Converter (uses whatever options the library supplies)
        defaultConverter = new Converter(new ConverterOptions());

        // Converter that explicitly targets UTC
        utcConverter = new Converter(createUtcOptions());
    }

    /**
     * Helper that creates a {@link ConverterOptions} instance whose {@code zoneId}
     * is {@link ZoneId#of(String) "UTC"} and whose {@code timeZone} is the matching
     * {@link java.util.TimeZone}.  The library does not expose a public constructor
     * for these fields, so reflection is used – this is acceptable in test code.
     */
    private static ConverterOptions createUtcOptions() {
        try {
            ConverterOptions opts = new ConverterOptions();
            // set the private field "zoneId"
            java.lang.reflect.Field zoneIdField = ConverterOptions.class.getDeclaredField("zoneId");
            zoneIdField.setAccessible(true);
            zoneIdField.set(opts, ZoneId.of("UTC"));

            // set the private field "timeZone"
            java.lang.reflect.Field timeZoneField = ConverterOptions.class.getDeclaredField("timeZone");
            timeZoneField.setAccessible(true);
            timeZoneField.set(opts, java.util.TimeZone.getTimeZone("UTC"));

            return opts;
        } catch (Exception e) {
            throw new RuntimeException("Unable to create UTC ConverterOptions via reflection", e);
        }
    }

    /* -------------------------------------------------------------
     *  Basic happy‑path tests for each conversion method
     * ------------------------------------------------------------- */

    @Test
    public void testToLong() {
        long expected = SAMPLE_INSTANT.toEpochMilli();
        assertEquals(expected, ZonedDateTimeConversions.toLong(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToDouble() {
        double expected = (double) SAMPLE_INSTANT.toEpochMilli();
        // InstantConversions.toDouble simply returns epoch milli as double – we verify equality
        assertEquals(expected, ZonedDateTimeConversions.toDouble(sampleZdtDefault, defaultConverter), 0.0);
    }

    @Test
    public void testToInstant() {
        assertEquals(SAMPLE_INSTANT, ZonedDateTimeConversions.toInstant(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToLocalDateTime_DefaultZone() {
        LocalDateTime expected = sampleZdtDefault.toLocalDateTime();
        assertEquals(expected, ZonedDateTimeConversions.toLocalDateTime(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToLocalDateTime_UtcAdjustment() {
        // The same instant, but converted to UTC first
        ZonedDateTime utcZdt = sampleZdtDefault.withZoneSameInstant(ZoneId.of("UTC"));
        LocalDateTime expected = utcZdt.toLocalDateTime();

        assertEquals(expected, ZonedDateTimeConversions.toLocalDateTime(sampleZdtDefault, utcConverter));
    }

    @Test
    public void testToLocalDate() {
        LocalDate expected = sampleZdtDefault.withZoneSameInstant(defaultConverter.getOptions().getZoneId()).toLocalDate();
        assertEquals(expected, ZonedDateTimeConversions.toLocalDate(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToLocalTime() {
        LocalTime expected = sampleZdtDefault.withZoneSameInstant(defaultConverter.getOptions().getZoneId()).toLocalTime();
        assertEquals(expected, ZonedDateTimeConversions.toLocalTime(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToOffsetDateTime() {
        OffsetDateTime expected = sampleZdtDefault.toOffsetDateTime();
        assertEquals(expected, ZonedDateTimeConversions.toOffsetDateTime(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToAtomicLong() {
        long expected = SAMPLE_INSTANT.toEpochMilli();
        AtomicLong atomic = ZonedDateTimeConversions.toAtomicLong(sampleZdtDefault, defaultConverter);
        assertEquals(expected, atomic.get());
    }

    @Test
    public void testToTimestamp() {
        Timestamp expected = Timestamp.from(SAMPLE_INSTANT);
        assertEquals(expected, ZonedDateTimeConversions.toTimestamp(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToCalendar() {
        Calendar cal = ZonedDateTimeConversions.toCalendar(sampleZdtDefault, defaultConverter);
        assertEquals(defaultConverter.getOptions().getTimeZone(), cal.getTimeZone());
        assertEquals(SAMPLE_INSTANT.toEpochMilli(), cal.getTimeInMillis());
    }

    @Test
    public void testToSqlDate() {
        java.sql.Date expected = java.sql.Date.valueOf(
                sampleZdtDefault.withZoneSameInstant(defaultConverter.getOptions().getZoneId()).toLocalDate()
        );
        assertEquals(expected, ZonedDateTimeConversions.toSqlDate(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToDate() {
        Date expected = new Date(SAMPLE_INSTANT.toEpochMilli());
        assertEquals(expected, ZonedDateTimeConversions.toDate(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToBigInteger() {
        BigInteger bi = ZonedDateTimeConversions.toBigInteger(sampleZdtDefault, defaultConverter);
        assertNotNull(bi);
        assertEquals(BigInteger.valueOf(SAMPLE_INSTANT.toEpochMilli()), bi);
    }

    @Test
    public void testToBigDecimal() {
        BigDecimal bd = ZonedDateTimeConversions.toBigDecimal(sampleZdtDefault, defaultConverter);
        assertNotNull(bd);
        assertEquals(BigDecimal.valueOf(SAMPLE_INSTANT.toEpochMilli()), bd);
    }

    @Test
    public void testToYear() {
        Year expected = Year.from(
                sampleZdtDefault.withZoneSameInstant(defaultConverter.getOptions().getZoneId()).toLocalDate()
        );
        assertEquals(expected, ZonedDateTimeConversions.toYear(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToYearMonth() {
        YearMonth expected = YearMonth.from(
                sampleZdtDefault.withZoneSameInstant(defaultConverter.getOptions().getZoneId()).toLocalDate()
        );
        assertEquals(expected, ZonedDateTimeConversions.toYearMonth(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToMonthDay() {
        MonthDay expected = MonthDay.from(
                sampleZdtDefault.withZoneSameInstant(defaultConverter.getOptions().getZoneId()).toLocalDate()
        );
        assertEquals(expected, ZonedDateTimeConversions.toMonthDay(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToString() {
        String expected = sampleZdtDefault.format(DateTimeFormatter.ISO_ZONED_DATE_TIME);
        assertEquals(expected, ZonedDateTimeConversions.toString(sampleZdtDefault, defaultConverter));
    }

    @Test
    public void testToMap() {
        String zdtStr = ZonedDateTimeConversions.toString(sampleZdtDefault, defaultConverter);
        Map<String, Object> map = ZonedDateTimeConversions.toMap(sampleZdtDefault, defaultConverter);
        assertNotNull(map);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.ZONED_DATE_TIME));
        assertEquals(zdtStr, map.get(MapConversions.ZONED_DATE_TIME));
    }

    /* -------------------------------------------------------------
     *  Edge‑case & exception tests
     * ------------------------------------------------------------- */

    @Test(expected = NullPointerException.class)
    public void testToLong_NullInput() {
        ZonedDateTimeConversions.toLong(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLong_NullConverter() {
        ZonedDateTimeConversions.toLong(sampleZdtDefault, null);
    }

    @Test(expected = NullPointerException.class)
    public void testToInstant_NullInput() {
        ZonedDateTimeConversions.toInstant(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDateTime_NullInput() {
        ZonedDateTimeConversions.toLocalDateTime(null, defaultConverter);
    }

    @Test(expected = NullPointerException.class)
    public void testToLocalDate_NullConverter() {
        ZonedDateTimeConversions.toLocalDate(sampleZdtDefault, null);
    }

    @Test
    public void testToLong_EpochBoundary() {
        ZonedDateTime epochZdt = ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"));
        Converter utc = new Converter(createUtcOptions());
        assertEquals(0L, ZonedDateTimeConversions.toLong(epochZdt, utc));
    }

    @Test
    public void testToLong_NearLongOverflow() {
        // 2^63 - 1 milliseconds is about year 292,277,026,596.
        // Use a value safely within range but large enough to prove handling.
        Instant farFuture = Instant.ofEpochMilli(Long.MAX_VALUE - 1);
        ZonedDateTime farZdt = ZonedDateTime.ofInstant(farFuture, ZoneId.of("UTC"));
        Converter utc = new Converter(createUtcOptions());
        assertEquals(Long.MAX_VALUE - 1, ZonedDateTimeConversions.toLong(farZdt, utc));
    }

    @Test
    public void testToString_ZoneAdjustment() {
        // Ensure that the string reflects the original zone, not the converter's target zone.
        ZonedDateTime original = ZonedDateTime.of(
                2021, 12, 31, 23, 59, 59, 0,
                ZoneId.of("America/New_York")
        );
        String formatted = ZonedDateTimeConversions.toString(original, utcConverter);
        // The string must contain the original offset "-05:00" (or "-04:00" if DST applies)
        assertTrue(formatted.contains(original.getOffset().getId()));
    }

    @Test
    public void testToMap_ContainsOnlyZonedDateTimeKey() {
        Map<String, Object> map = ZonedDateTimeConversions.toMap(sampleZdtDefault, defaultConverter);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(MapConversions.ZONED_DATE_TIME));
        assertFalse(map.containsKey("otherKey"));
    }
}
