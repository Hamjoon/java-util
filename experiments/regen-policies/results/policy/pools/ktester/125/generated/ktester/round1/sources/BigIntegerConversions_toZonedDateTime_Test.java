package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toZonedDateTime_Test {

    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    private static Converter converter;

    private static final ZoneId UTC = ZoneId.of("UTC");

    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");

    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    // UTC – used as the generic “default”
    private static Converter defaultConverter;

    private static Converter utcConverter;

    private static Converter nyConverter;

    private static Converter tokyoConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise converters with known zones for deterministic results.
        ConverterOptions defaultOpts = new ConverterOptions();
        defaultOpts.setZoneId(UTC);
        defaultConverter = new Converter(defaultOpts);
        ConverterOptions utcOpts = new ConverterOptions();
        utcOpts.setZoneId(UTC);
        utcConverter = new Converter(utcOpts);
        ConverterOptions nyOpts = new ConverterOptions();
        nyOpts.setZoneId(NEW_YORK);
        nyConverter = new Converter(nyOpts);
        ConverterOptions tokyoOpts = new ConverterOptions();
        tokyoOpts.setZoneId(TOKYO);
        tokyoConverter = new Converter(tokyoOpts);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the pattern tidy.
        converter = null;
    }

    @Test
    void toZonedDateTime_givenInstant_returnsZonedDateTimeWithSameInstantAndConfiguredZone() {
        // GIVEN
        Instant instant = Instant.parse("2023-01-02T03:04:05Z");
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(instant, defaultConverter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(instant, result.toInstant(), "The instant part must be unchanged");
        assertEquals(UTC, result.getZone(), "The zone must be the one configured in ConverterOptions");
    }

    @Test
    void toZonedDateTime_givenBigIntegerEpochSeconds_returnsCorrectZonedDateTime() {
        // GIVEN
        // 2023‑01‑02T03:04:05Z expressed as epoch seconds
        BigInteger epochSeconds = BigInteger.valueOf(1672628645L);
        Instant expectedInstant = Instant.ofEpochSecond(epochSeconds.longValue());
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(epochSeconds, defaultConverter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expectedInstant, result.toInstant(), "The conversion from BigInteger (epoch seconds) must produce the correct instant");
        assertEquals(UTC, result.getZone(), "The zone must be the one configured in ConverterOptions");
    }

    @Test
    void toZonedDateTime_givenNullSource_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toZonedDateTime(from, defaultConverter), "A null source should propagate NullPointerException from toInstant");
    }

    @Test
    void toZonedDateTime_givenDateEpochZero_returnsZonedDateTimeAtUTC() {
        // GIVEN
        // epoch start
        Date date = new Date(0L);
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(date, utcConverter);
        // THEN
        assertNotNull(result);
        assertEquals(Instant.ofEpochMilli(0L), result.toInstant(), "Date epoch 0 should map to the same instant");
        assertEquals(UTC, result.getZone(), "Zone must be UTC as configured in the converter");
    }

    @Test
    void toZonedDateTime_givenTimestamp_convertsViaInstantAndAppliesConverterZone() {
        // GIVEN
        Timestamp ts = Timestamp.valueOf("2000-01-01 00:00:00");
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(ts, defaultConverter);
        // THEN
        assertNotNull(result);
        assertEquals(ts.toInstant(), result.toInstant(), "Timestamp should be converted via its Instant representation");
        assertEquals(UTC, result.getZone(), "Zone must be the one configured in the converter (UTC)");
    }

    @Test
    void toZonedDateTime_givenLocalDateTime_interpretedInConverterZone() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2022, 12, 31, 23, 59, 59);
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(ldt, nyConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.of(ldt, NEW_YORK);
        assertEquals(expected, result, "LocalDateTime should be interpreted in the converter's zone (America/New_York)");
    }

    @Test
    void toZonedDateTime_givenZonedDateTimeWithDifferentTargetZone_preservesInstant() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2021, 6, 15, 10, 0, 0, 0, // source zone
        ZoneId.of("Europe/Paris"));
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(source, tokyoConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.ofInstant(source.toInstant(), TOKYO);
        assertEquals(expected, result, "Result should have the same instant but be expressed in the converter's zone (Asia/Tokyo)");
    }

    @Test
    void toZonedDateTime_givenOffsetDateTime_convertsToInstantThenAppliesConverterZone() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.parse("2020-05-20T15:30:00+02:00");
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(odt, utcConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.ofInstant(odt.toInstant(), UTC);
        assertEquals(expected, result, "OffsetDateTime should be converted to an Instant and then zoned to UTC");
    }

    @Test
    void toZonedDateTime_givenLongEpochMillis_convertsToInstantAndAppliesConverterZone() {
        // GIVEN
        // 2021-06-01T00:00:00Z
        long epochMillis = 1622505600000L;
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(epochMillis, utcConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), UTC);
        assertEquals(expected, result, "Long epoch-millis should be interpreted as an Instant and zoned to UTC");
    }

    @Test
    void toZonedDateTime_givenUnsupportedType_throwsIllegalArgumentException() {
        // GIVEN
        Boolean unsupported = Boolean.TRUE;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toZonedDateTime(unsupported, defaultConverter), "Unsupported source type should result in IllegalArgumentException");
    }

    @Test
    void toZonedDateTime_givenFarFutureInstant_handlesMaximumRepresentableInstant() {
        // GIVEN
        Instant farFuture = Instant.parse("9999-12-31T23:59:59.999999999Z");
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(farFuture, utcConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.ofInstant(farFuture, UTC);
        assertEquals(expected, result, "Method should correctly handle a far‑future instant");
    }

    @Test
    void toZonedDateTime_givenInstantMin_handlesFarPastInstant() {
        // GIVEN
        Instant minInstant = Instant.MIN;
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(minInstant, utcConverter);
        // THEN
        ZonedDateTime expected = ZonedDateTime.ofInstant(minInstant, UTC);
        assertEquals(expected, result, "Method should correctly handle Instant.MIN (far‑past instant)");
    }

    @Test
    void toZonedDateTime_givenBigIntegerOverflow_throwsDateTimeException() {
        // GIVEN
        // value > Long.MAX_VALUE seconds – will overflow Instant
        BigInteger overflow = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> BigIntegerConversions.toZonedDateTime(overflow, utcConverter), "Overflowing epoch‑second value should cause DateTimeException");
    }
}
