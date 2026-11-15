package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toZonedDateTime_Test {

    /**
     * The Converter used for all tests – it is created once because its state is immutable.
     */
    private static Converter converter;

    /**
     * The ZoneId used for the conversion – UTC makes the expected values easy to reason about.
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    @BeforeAll
    static void setupBeforeAll() {
        // Build ConverterOptions with the desired ZoneId.
        // The real library provides a default constructor; if not, adjust accordingly.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(TEST_ZONE);
        converter = new Converter(options);
    }

    /**
     * Helper that builds a {@link Converter} with the required {@link ZoneId}.
     */
    private static Converter converterWithZone(ZoneId zone) {
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zone);
        return new Converter(options);
    }

    /**
     * Supplies the successful conversion scenarios described in the JSON spec.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// java.util.Date (epoch)
        Arguments.of("Date epoch → ZonedDateTime UTC", new Date(0L), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.EPOCH, ZoneId.of("UTC"))), // Instant with America/New_York
        Arguments.of("Instant → ZonedDateTime America/New_York", Instant.parse("2023-03-15T12:34:56Z"), ZoneId.of("America/New_York"), ZonedDateTime.ofInstant(Instant.parse("2023-03-15T12:34:56Z"), ZoneId.of("America/New_York"))), // java.sql.Timestamp → Europe/Paris
        Arguments.of("Timestamp → ZonedDateTime Europe/Paris", Timestamp.valueOf("2022-01-01 08:15:30"), ZoneId.of("Europe/Paris"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(Timestamp.valueOf("2022-01-01 08:15:30").getTime()), ZoneId.of("Europe/Paris"))), // LocalDateTime → Asia/Tokyo (treated as if in target zone)
        Arguments.of("LocalDateTime → ZonedDateTime Asia/Tokyo", LocalDateTime.of(2021, 5, 20, 14, 45, 10), ZoneId.of("Asia/Tokyo"), ZonedDateTime.of(LocalDateTime.of(2021, 5, 20, 14, 45, 10), ZoneId.of("Asia/Tokyo"))), // OffsetDateTime → UTC
        Arguments.of("OffsetDateTime → ZonedDateTime UTC", OffsetDateTime.parse("2020-12-31T23:00:00+02:00"), ZoneId.of("UTC"), ZonedDateTime.ofInstant(OffsetDateTime.parse("2020-12-31T23:00:00+02:00").toInstant(), ZoneId.of("UTC"))), // BigInteger epoch seconds → UTC
        Arguments.of("BigInteger epoch seconds → ZonedDateTime UTC", BigInteger.valueOf(1_640_995_200L), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1_640_995_200L), ZoneId.of("UTC"))), // ISO‑8601 String → UTC
        Arguments.of("ISO‑8601 String → ZonedDateTime UTC", "2024-02-29T10:20:30Z", ZoneId.of("UTC"), ZonedDateTime.parse("2024-02-29T10:20:30Z")));
    }

    @Test
    void toZonedDateTime_fromInstant_returnsZonedDateTimeInConfiguredZone() {
        // GIVEN
        Instant instant = Instant.parse("2023-03-25T12:34:56.789Z");
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(instant, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(instant, result.toInstant(), "Instant part must be unchanged");
        assertEquals(TEST_ZONE, result.getZone(), "Zone must be the one configured in ConverterOptions");
    }

    @Test
    void toZonedDateTime_fromEpochMillisLong_returnsCorrectZonedDateTime() {
        // GIVEN
        // corresponds to 2023-05-02T03:04:16.789Z
        long epochMillis = 1_684_123_456_789L;
        Instant expectedInstant = Instant.ofEpochMilli(epochMillis);
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(epochMillis, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedInstant, result.toInstant(), "The Instant derived from epoch‑millis must match the expected value");
        assertEquals(TEST_ZONE, result.getZone(), "Zone must be the one configured in ConverterOptions");
    }

    @Test
    void toZonedDateTime_fromDate_returnsCorrectZonedDateTime() {
        // GIVEN
        // same epoch as previous test
        Date date = new Date(1_684_123_456_789L);
        Instant expectedInstant = date.toInstant();
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(date, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedInstant, result.toInstant(), "The Instant derived from java.util.Date must match the expected value");
        assertEquals(TEST_ZONE, result.getZone(), "Zone must be the one configured in ConverterOptions");
    }

    /* -------------------------------------------------------------
     *  Successful conversion tests – parameterized for readability.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void toZonedDateTime_successfulConversions(String description, Object from, ZoneId zone, ZonedDateTime expected) {
        // GIVEN
        Converter converter = converterWithZone(zone);
        // WHEN
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(from, converter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expected.toInstant(), result.toInstant(), "Instant part should match the expected instant");
        assertEquals(expected.getZone(), result.getZone(), "Zone must be the one supplied by the Converter");
    }

    /* -------------------------------------------------------------
     *  ZoneId variation tests – same instant, different zones.
     * ------------------------------------------------------------- */
    @Test
    void toZonedDateTime_zoneVariations_london() {
        Instant instant = Instant.parse("2022-06-01T12:00:00Z");
        ZoneId zone = ZoneId.of("Europe/London");
        Converter converter = converterWithZone(zone);
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(instant, converter);
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, zone);
        assertEquals(expected, result);
    }

    @Test
    void toZonedDateTime_zoneVariations_losAngeles() {
        Instant instant = Instant.parse("2022-06-01T12:00:00Z");
        ZoneId zone = ZoneId.of("America/Los_Angeles");
        Converter converter = converterWithZone(zone);
        ZonedDateTime result = BigIntegerConversions.toZonedDateTime(instant, converter);
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, zone);
        assertEquals(expected, result);
    }

    /* -------------------------------------------------------------
     *  Error handling – null arguments and unsupported types.
     * ------------------------------------------------------------- */
    @Test
    void toZonedDateTime_nullSource_throwsNullPointerException() {
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toZonedDateTime(null, converter), "Null source should result in NullPointerException");
    }

    @Test
    void toZonedDateTime_nullConverter_throwsNullPointerException() {
        Date date = new Date(0L);
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toZonedDateTime(date, null), "Null converter should result in NullPointerException");
    }

    @Test
    void toZonedDateTime_unsupportedType_boolean_throwsConversionException() {
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        assertThrows(ConversionException.class, () -> BigIntegerConversions.toZonedDateTime(Boolean.TRUE, converter), "Unsupported source type should throw ConversionException");
    }

    @Test
    void toZonedDateTime_unsupportedTemporal_LocalDate_throwsConversionException() {
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        LocalDate ld = LocalDate.of(2023, 5, 20);
        assertThrows(ConversionException.class, () -> BigIntegerConversions.toZonedDateTime(ld, converter), "LocalDate lacks time‑of‑day and should cause ConversionException");
    }

    @Test
    void toZonedDateTime_unsupportedTemporal_LocalTime_throwsConversionException() {
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        LocalTime lt = LocalTime.of(12, 30, 45);
        assertThrows(ConversionException.class, () -> BigIntegerConversions.toZonedDateTime(lt, converter), "LocalTime lacks date component and should cause ConversionException");
    }

    @Test
    void toZonedDateTime_unsupportedSqlDate_throwsConversionException() {
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        java.sql.Date sqlDate = new java.sql.Date(0L);
        assertThrows(ConversionException.class, () -> BigIntegerConversions.toZonedDateTime(sqlDate, converter), "java.sql.Date is unsupported for ZonedDateTime conversion");
    }

    /* -------------------------------------------------------------
     *  Error handling – malformed string and numeric overflow.
     * ------------------------------------------------------------- */
    @Test
    void toZonedDateTime_malformedString_throwsDateTimeParseException() {
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        // month 13 is invalid
        String badIso = "2024-13-01T00:00:00Z";
        assertThrows(DateTimeParseException.class, () -> BigIntegerConversions.toZonedDateTime(badIso, converter), "Malformed ISO‑8601 string should cause DateTimeParseException");
    }

    @Test
    void toZonedDateTime_bigIntegerOverflow_throwsDateTimeException() {
        Converter converter = converterWithZone(ZoneId.of("UTC"));
        // Long.MAX_VALUE + 1 seconds exceeds Instant range
        BigInteger overflow = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        assertThrows(DateTimeException.class, () -> BigIntegerConversions.toZonedDateTime(overflow, converter), "Overflowing epoch seconds should cause DateTimeException");
    }

    /* -------------------------------------------------------------
     *  Error handling – null ZoneId in ConverterOptions.
     * ------------------------------------------------------------- */
    @Test
    void toZonedDateTime_nullZoneIdInOptions_throwsNullPointerException() {
        // Build a Converter whose options have a null ZoneId
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(null);
        Converter converter = new Converter(options);
        Instant instant = Instant.EPOCH;
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toZonedDateTime(instant, converter), "Null ZoneId in ConverterOptions should cause NullPointerException");
    }
}
