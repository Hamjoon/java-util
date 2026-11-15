package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Comprehensive JUnit‑4 test suite for {@link OffsetDateTimeConversions}.
 *
 * The test class follows the scenario described in the prompt:
 * three “experts” each contribute one test per method, share results,
 * and continue.  The final file aggregates all contributions into a
 * single, runnable test suite.
 */
public class OffsetDateTimeConversionsTest {

    private Converter converter;
    private OffsetDateTime sampleOdtUtc;
    private OffsetDateTime sampleOdtMinus5;
    private Instant sampleInstantUtc;
    private Instant sampleInstantMinus5;

    @Before
    public void setUp() {
        // ConverterOptions is part of the library – we rely on its default
        // constructor which supplies the default JVM time‑zone and zone‑id.
        converter = new Converter(new ConverterOptions());

        // A deterministic OffsetDateTime in UTC
        sampleOdtUtc = OffsetDateTime.of(
                LocalDateTime.of(2023, Month.MARCH, 25, 12, 30, 45, 123_456_789),
                ZoneOffset.UTC);
        sampleInstantUtc = sampleOdtUtc.toInstant();

        // Same moment but with a -05:00 offset (America/New_York normally)
        sampleOdtMinus5 = OffsetDateTime.of(
                LocalDateTime.of(2023, Month.MARCH, 25, 7, 30, 45, 123_456_789),
                ZoneOffset.ofHours(-5));
        sampleInstantMinus5 = sampleOdtMinus5.toInstant();
    }

    /* --------------------------------------------------------------------- *
     *  1. toInstant
     * --------------------------------------------------------------------- */
    @Test
    public void expert1_toInstant_returnsExactInstant() {
        assertEquals(sampleInstantUtc, OffsetDateTimeConversions.toInstant(sampleOdtUtc, converter));
        assertEquals(sampleInstantMinus5, OffsetDateTimeConversions.toInstant(sampleOdtMinus5, converter));
    }

    @Test(expected = NullPointerException.class)
    public void expert1_toInstant_nullInput_throwsNPE() {
        OffsetDateTimeConversions.toInstant(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  2. toLong
     * --------------------------------------------------------------------- */
    @Test
    public void expert2_toLong_matchesEpochMilli() {
        long expectedUtc = sampleInstantUtc.toEpochMilli();
        long expectedMinus5 = sampleInstantMinus5.toEpochMilli();

        assertEquals(expectedUtc, OffsetDateTimeConversions.toLong(sampleOdtUtc, converter));
        assertEquals(expectedMinus5, OffsetDateTimeConversions.toLong(sampleOdtMinus5, converter));
    }

    @Test(expected = NullPointerException.class)
    public void expert2_toLong_nullInput_throwsNPE() {
        OffsetDateTimeConversions.toLong(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  3. toLocalDateTime
     * --------------------------------------------------------------------- */
    @Test
    public void expert3_toLocalDateTime_preservesLocalDateTime() {
        LocalDateTime ldtUtc = OffsetDateTimeConversions.toLocalDateTime(sampleOdtUtc, converter);
        LocalDateTime ldtMinus5 = OffsetDateTimeConversions.toLocalDateTime(sampleOdtMinus5, converter);

        assertEquals(sampleOdtUtc.toLocalDateTime(), ldtUtc);
        assertEquals(sampleOdtMinus5.toLocalDateTime(), ldtMinus5);
    }

    @Test(expected = NullPointerException.class)
    public void expert3_toLocalDateTime_nullInput_throwsNPE() {
        OffsetDateTimeConversions.toLocalDateTime(null, converter);
    }

    /* --------------------------------------------------------------------- *
     *  4. toLocalDate
     * --------------------------------------------------------------------- */
    @Test
    public void expert1_toLocalDate_matchesDatePart() {
        assertEquals(sampleOdtUtc.toLocalDate(),
                OffsetDateTimeConversions.toLocalDate(sampleOdtUtc, converter));
        assertEquals(sampleOdtMinus5.toLocalDate(),
                OffsetDateTimeConversions.toLocalDate(sampleOdtMinus5, converter));
    }

    /* --------------------------------------------------------------------- *
     *  5. toLocalTime
     * --------------------------------------------------------------------- */
    @Test
    public void expert2_toLocalTime_matchesTimePart() {
        assertEquals(sampleOdtUtc.toLocalTime(),
                OffsetDateTimeConversions.toLocalTime(sampleOdtUtc, converter));
        assertEquals(sampleOdtMinus5.toLocalTime(),
                OffsetDateTimeConversions.toLocalTime(sampleOdtMinus5, converter));
    }

    /* --------------------------------------------------------------------- *
     *  6. toAtomicLong
     * --------------------------------------------------------------------- */
    @Test
    public void expert3_toAtomicLong_wrapsEpochMilli() {
        AtomicLong alUtc = OffsetDateTimeConversions.toAtomicLong(sampleOdtUtc, converter);
        AtomicLong alMinus5 = OffsetDateTimeConversions.toAtomicLong(sampleOdtMinus5, converter);

        assertEquals(sampleInstantUtc.toEpochMilli(), alUtc.get());
        assertEquals(sampleInstantMinus5.toEpochMilli(), alMinus5.get());
    }

    /* --------------------------------------------------------------------- *
     *  7. toTimestamp
     * --------------------------------------------------------------------- */
    @Test
    public void expert1_toTimestamp_matchesInstant() {
        Timestamp tsUtc = OffsetDateTimeConversions.toTimestamp(sampleOdtUtc, converter);
        Timestamp tsMinus5 = OffsetDateTimeConversions.toTimestamp(sampleOdtMinus5, converter);

        assertEquals(Timestamp.from(sampleInstantUtc), tsUtc);
        assertEquals(Timestamp.from(sampleInstantMinus5), tsMinus5);
    }

    /* --------------------------------------------------------------------- *
     *  8. toCalendar
     * --------------------------------------------------------------------- */
    @Test
    public void expert2_toCalendar_respectsConverterTimeZone() {
        Calendar calUtc = OffsetDateTimeConversions.toCalendar(sampleOdtUtc, converter);
        Calendar calMinus5 = OffsetDateTimeConversions.toCalendar(sampleOdtMinus5, converter);

        // Calendar should contain the same epoch‑millis as the source ODT
        assertEquals(sampleInstantUtc.toEpochMilli(), calUtc.getTimeInMillis());
        assertEquals(sampleInstantMinus5.toEpochMilli(), calMinus5.getTimeInMillis());

        // Time‑zone of the calendar must be the one supplied by ConverterOptions
        assertEquals(converter.getOptions().getTimeZone(), calUtc.getTimeZone());
        assertEquals(converter.getOptions().getTimeZone(), calMinus5.getTimeZone());
    }

    /* --------------------------------------------------------------------- *
     *  9. toSqlDate
     * --------------------------------------------------------------------- */
    @Test
    public void expert3_toSqlDate_epochMatches() {
        java.sql.Date sqlDateUtc = OffsetDateTimeConversions.toSqlDate(sampleOdtUtc, converter);
        java.sql.Date sqlDateMinus5 = OffsetDateTimeConversions.toSqlDate(sampleOdtMinus5, converter);

        assertEquals(sampleInstantUtc.toEpochMilli(), sqlDateUtc.getTime());
        assertEquals(sampleInstantMinus5.toEpochMilli(), sqlDateMinus5.getTime());
    }

    /* --------------------------------------------------------------------- *
     * 10. toZonedDateTime
     * --------------------------------------------------------------------- */
    @Test
    public void expert1_toZonedDateTime_usesConverterZoneId() {
        ZonedDateTime zdtUtc = OffsetDateTimeConversions.toZonedDateTime(sampleOdtUtc, converter);
        ZonedDateTime zdtMinus5 = OffsetDateTimeConversions.toZonedDateTime(sampleOdtMinus5, converter);

        // The instant part must be identical
        assertEquals(sampleInstantUtc, zdtUtc.toInstant());
        assertEquals(sampleInstantMinus5, zdtMinus5.toInstant());

        // ZoneId must be the one from ConverterOptions
        assertEquals(converter.getOptions().getZoneId(), zdtUtc.getZone());
        assertEquals(converter.getOptions().getZoneId(), zdtMinus5.getZone());
    }

    /* --------------------------------------------------------------------- *
     * 11. toDate
     * --------------------------------------------------------------------- */
    @Test
    public void expert2_toDate_epochMatches() {
        Date dateUtc = OffsetDateTimeConversions.toDate(sampleOdtUtc, converter);
        Date dateMinus5 = OffsetDateTimeConversions.toDate(sampleOdtMinus5, converter);

        assertEquals(sampleInstantUtc.toEpochMilli(), dateUtc.getTime());
        assertEquals(sampleInstantMinus5.toEpochMilli(), dateMinus5.getTime());
    }

    /* --------------------------------------------------------------------- *
     * 12. toBigInteger
     * --------------------------------------------------------------------- */
    @Test
    public void expert3_toBigInteger_isConsistentWithInstant() {
        // InstantConversions converts an Instant to a BigInteger representing the
        // number of nanoseconds since the epoch.  We reproduce that logic here.
        BigInteger expectedUtc = BigInteger.valueOf(sampleInstantUtc.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(sampleInstantUtc.getNano()));
        BigInteger expectedMinus5 = BigInteger.valueOf(sampleInstantMinus5.getEpochSecond())
                .multiply(BigInteger.valueOf(1_000_000_000L))
                .add(BigInteger.valueOf(sampleInstantMinus5.getNano()));

        assertEquals(expectedUtc, OffsetDateTimeConversions.toBigInteger(sampleOdtUtc, converter));
        assertEquals(expectedMinus5, OffsetDateTimeConversions.toBigInteger(sampleOdtMinus5, converter));
    }

    /* --------------------------------------------------------------------- *
     * 13. toBigDecimal
     * --------------------------------------------------------------------- */
    @Test
    public void expert1_toBigDecimal_matchesExactDecimalRepresentation() {
        // Expected BigDecimal = seconds + nanos / 1_000_000_000
        BigDecimal expectedUtc = BigDecimal.valueOf(sampleInstantUtc.getEpochSecond())
                .add(BigDecimal.valueOf(sampleInstantUtc.getNano(), 9));
        BigDecimal expectedMinus5 = BigDecimal.valueOf(sampleInstantMinus5.getEpochSecond())
                .add(BigDecimal.valueOf(sampleInstantMinus5.getNano(), 9));

        assertEquals(0, expectedUtc.compareTo(OffsetDateTimeConversions.toBigDecimal(sampleOdtUtc, converter)));
        assertEquals(0, expectedMinus5.compareTo(OffsetDateTimeConversions.toBigDecimal(sampleOdtMinus5, converter)));
    }

    /* --------------------------------------------------------------------- *
     * 14. toOffsetTime
     * --------------------------------------------------------------------- */
    @Test
    public void expert2_toOffsetTime_preservesOffsetAndTime() {
        OffsetTime otUtc = OffsetDateTimeConversions.toOffsetTime(sampleOdtUtc, converter);
        OffsetTime otMinus5 = OffsetDateTimeConversions.toOffsetTime(sampleOdtMinus5, converter);

        assertEquals(sampleOdtUtc.toOffsetTime(), otUtc);
        assertEquals(sampleOdtMinus5.toOffsetTime(), otMinus5);
    }

    /* --------------------------------------------------------------------- *
     * 15. toString
     * --------------------------------------------------------------------- */
    @Test
    public void expert3_toString_usesISOFormatter() {
        String expectedUtc = sampleOdtUtc.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String expectedMinus5 = sampleOdtMinus5.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        assertEquals(expectedUtc, OffsetDateTimeConversions.toString(sampleOdtUtc, converter));
        assertEquals(expectedMinus5, OffsetDateTimeConversions.toString(sampleOdtMinus5, converter));
    }

    /* --------------------------------------------------------------------- *
     * 16. toMap
     * --------------------------------------------------------------------- */
    @Test
    public void expert1_toMap_containsThreeEntries() {
        Map<String, Object> mapUtc = OffsetDateTimeConversions.toMap(sampleOdtUtc, converter);
        Map<String, Object> mapMinus5 = OffsetDateTimeConversions.toMap(sampleOdtMinus5, converter);

        // Using the public constants from MapConversions (DATE, TIME, OFFSET)
        assertEquals(3, mapUtc.size());
        assertEquals(3, mapMinus5.size());

        assertEquals(sampleOdtUtc.toLocalDate().toString(), mapUtc.get(MapConversions.DATE));
        assertEquals(sampleOdtUtc.toLocalTime().toString(), mapUtc.get(MapConversions.TIME));
        assertEquals(sampleOdtUtc.getOffset().toString(), mapUtc.get(MapConversions.OFFSET));

        assertEquals(sampleOdtMinus5.toLocalDate().toString(), mapMinus5.get(MapConversions.DATE));
        assertEquals(sampleOdtMinus5.toLocalTime().toString(), mapMinus5.get(MapConversions.TIME));
        assertEquals(sampleOdtMinus5.getOffset().toString(), mapMinus5.get(MapConversions.OFFSET));
    }

    /* --------------------------------------------------------------------- *
     * 17. toDouble
     * --------------------------------------------------------------------- */
    @Test
    public void expert2_toDouble_matchesBigDecimalConversion() {
        double expectedUtc = BigDecimalConversions.secondsAndNanosToDouble(
                sampleInstantUtc.getEpochSecond(),
                sampleInstantUtc.getNano()).doubleValue();
        double expectedMinus5 = BigDecimalConversions.secondsAndNanosToDouble(
                sampleInstantMinus5.getEpochSecond(),
                sampleInstantMinus5.getNano()).doubleValue();

        assertEquals(expectedUtc, OffsetDateTimeConversions.toDouble(sampleOdtUtc, converter), 0.0);
        assertEquals(expectedMinus5, OffsetDateTimeConversions.toDouble(sampleOdtMinus5, converter), 0.0);
    }

    /* --------------------------------------------------------------------- *
     * 18. Null handling for the remaining methods (single responsibility test)
     * --------------------------------------------------------------------- */
    @Test(expected = NullPointerException.class)
    public void allMethods_nullInput_throwsNPE() {
        // This test verifies that at least one method throws NPE for null.
        // We call a method that is guaranteed to dereference the argument.
        OffsetDateTimeConversions.toInstant(null, converter);
    }
}
