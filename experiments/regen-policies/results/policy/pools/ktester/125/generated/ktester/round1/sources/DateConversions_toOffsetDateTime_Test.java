package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toOffsetDateTime_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * The {@link ZoneId} that the {@link Converter} will use – fixed to UTC for reproducibility.
     */
    private static final ZoneId TEST_ZONE = ZoneOffset.UTC;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with deterministic options (UTC zone)
        ConverterOptions options = new ConverterOptions();
        // ensure a known zone for all conversions
        options.setZoneId(TEST_ZONE);
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference
        converter = null;
    }

    /**
     * Helper that computes the expected {@link OffsetDateTime} for a given {@link Instant}
     * using the same zone that the {@link Converter} is configured with.
     */
    private static OffsetDateTime expectedFromInstant(Instant instant) {
        return instant.atZone(TEST_ZONE).toOffsetDateTime();
    }

    /**
     * Data provider for {@link #toOffsetDateTime_successful(Object, ZoneId, OffsetDateTime)}.
     */
    private static Stream<Arguments> provideSuccessfulConversions() {
        // 1. Instant
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        // 2. java.util.Date
        Date date = Date.from(instant);
        // 3. java.sql.Timestamp
        Timestamp ts = Timestamp.valueOf("2023-01-01 12:34:56");
        // 4. String with explicit offset
        String isoWithOffset = "2023-01-01T12:34:56+02:00";
        // 5. String without offset (custom pattern)
        String isoWithoutOffset = "2023-01-01 12:34:56";
        DateTimeFormatter customFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 6. LocalDateTime
        LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 34, 56);
        // 7. ZonedDateTime (different zone)
        ZonedDateTime zdt = ZonedDateTime.of(2023, 1, 1, 12, 34, 56, 0, ZoneId.of("Europe/London"));
        // Zone variations used in the matrix
        ZoneId utc = ZoneOffset.UTC;
        ZoneId newYork = ZoneId.of("America/New_York");
        ZoneId paris = ZoneId.of("Europe/Paris");
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");
        return Stream.of(// Instant → uses converter zone (UTC)
        Arguments.of(instant, utc, expectedFromInstant(instant, utc)), // Date → same instant as above, uses New York zone
        Arguments.of(date, newYork, expectedFromInstant(instant, newYork)), // Timestamp → uses Paris zone
        Arguments.of(ts, paris, expectedFromInstant(ts.toInstant(), paris)), // String with offset → parsed directly, zone ignored
        Arguments.of(isoWithOffset, utc, OffsetDateTime.parse(isoWithOffset)), // String without offset → parsed as LocalDateTime then combined with Tokyo zone
        Arguments.of(isoWithoutOffset, tokyo, LocalDateTime.parse(isoWithoutOffset, customFormatter).atZone(tokyo).toOffsetDateTime()), // LocalDateTime → combined with UTC zone
        Arguments.of(ldt, utc, ldt.atZone(utc).toOffsetDateTime()), // ZonedDateTime → first to Instant, then applied UTC zone
        Arguments.of(zdt, utc, expectedFromInstant(zdt.toInstant(), utc)));
    }

    @Test
    void toOffsetDateTime_fromInstant() {
        Instant instant = Instant.parse("2023-01-02T03:04:05Z");
        OffsetDateTime result = DateConversions.toOffsetDateTime(instant, converter);
        assertEquals(expectedFromInstant(instant), result);
    }

    @Test
    void toOffsetDateTime_fromDate() {
        Instant instant = Instant.parse("2023-06-15T12:30:45Z");
        Date date = Date.from(instant);
        OffsetDateTime result = DateConversions.toOffsetDateTime(date, converter);
        assertEquals(expectedFromInstant(instant), result);
    }

    @Test
    void toOffsetDateTime_fromLongEpochMilli() {
        Instant instant = Instant.parse("2022-12-31T23:59:59Z");
        Long epochMilli = instant.toEpochMilli();
        OffsetDateTime result = DateConversions.toOffsetDateTime(epochMilli, converter);
        assertEquals(expectedFromInstant(instant), result);
    }

    @Test
    void toOffsetDateTime_fromStringIso8601() {
        String iso = "2021-07-04T14:20:00Z";
        Instant instant = Instant.parse(iso);
        OffsetDateTime result = DateConversions.toOffsetDateTime(iso, converter);
        assertEquals(expectedFromInstant(instant), result);
    }

    @Test
    void toOffsetDateTime_fromLocalDateTime() {
        // LocalDateTime has no zone information – the Converter treats it as a
        // date‑time in the converter's zone.
        LocalDateTime ldt = LocalDateTime.of(2020, 5, 10, 8, 15, 30);
        // The expected instant is the same local date‑time interpreted in TEST_ZONE
        Instant expectedInstant = ldt.atZone(TEST_ZONE).toInstant();
        OffsetDateTime result = DateConversions.toOffsetDateTime(ldt, converter);
        assertEquals(expectedFromInstant(expectedInstant), result);
    }

    @Test
    void toOffsetDateTime_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2019, 11, 30, 22, 45, 0, 0, ZoneId.of("America/New_York"));
        // Conversion first normalises to an Instant, then re‑applies TEST_ZONE
        Instant instant = zdt.toInstant();
        OffsetDateTime result = DateConversions.toOffsetDateTime(zdt, converter);
        assertEquals(expectedFromInstant(instant), result);
    }

    @Test
    void toOffsetDateTime_nullInputReturnsNull() {
        assertNull(DateConversions.toOffsetDateTime(null, converter));
    }

    @ParameterizedTest(name = "{index}: from={0}, zone={1}")
    @MethodSource("provideSuccessfulConversions")
    void toOffsetDateTime_successful(Object from, ZoneId zone, OffsetDateTime expected) {
        // Build a Converter that uses the supplied zone
        ConverterOptions opts = new ConverterOptions();
        opts.setZoneId(zone);
        Converter conv = new Converter(opts);
        OffsetDateTime result = DateConversions.toOffsetDateTime(from, conv);
        assertEquals(expected, result);
    }

    @Test
    void toOffsetDateTime_nullFrom_throwsNullPointerException() {
        // The converter itself is valid; the source object is null.
        assertThrows(NullPointerException.class, () -> DateConversions.toOffsetDateTime(null, converter));
    }

    @Test
    void toOffsetDateTime_nullConverter_throwsNullPointerException() {
        // Passing a null converter should cause an NPE when the method tries to
        // access converter.getOptions().
        Instant instant = Instant.now();
        assertThrows(NullPointerException.class, () -> DateConversions.toOffsetDateTime(instant, null));
    }

    @Test
    void toOffsetDateTime_unsupportedType_throwsConversionException() {
        Object unsupported = new Object();
        assertThrows(ConversionException.class, () -> DateConversions.toOffsetDateTime(unsupported, converter));
    }

    @Test
    void toOffsetDateTime_invalidIsoString_throwsDateTimeParseException() {
        // February 30 does not exist
        String badDate = "2023-02-30T12:34:56";
        assertThrows(DateTimeParseException.class, () -> DateConversions.toOffsetDateTime(badDate, converter));
    }

    @Test
    void toOffsetDateTime_malformedString_throwsDateTimeParseException() {
        String malformed = "not-a-date";
        assertThrows(DateTimeParseException.class, () -> DateConversions.toOffsetDateTime(malformed, converter));
    }

    @Test
    void toOffsetDateTime_localDate_throwsConversionException() {
        LocalDate ld = LocalDate.of(2023, 1, 1);
        assertThrows(ConversionException.class, () -> DateConversions.toOffsetDateTime(ld, converter));
    }

    @Test
    void toOffsetDateTime_localTime_throwsConversionException() {
        LocalTime lt = LocalTime.of(12, 34, 56);
        assertThrows(ConversionException.class, () -> DateConversions.toOffsetDateTime(lt, converter));
    }

    @Test
    void toOffsetDateTime_invalidZoneId_throwsDateTimeException() {
        // The exception is thrown while constructing the ZonedDateTime,
        // not inside toOffsetDateTime.
        assertThrows(DateTimeException.class, () -> {
            ZonedDateTime invalid = ZonedDateTime.ofInstant(Instant.parse("2023-01-01T12:34:56Z"), ZoneId.of("Invalid/Zone"));
            // The method is never reached, but we keep the call for completeness.
            DateConversions.toOffsetDateTime(invalid, converter);
        });
    }
}
