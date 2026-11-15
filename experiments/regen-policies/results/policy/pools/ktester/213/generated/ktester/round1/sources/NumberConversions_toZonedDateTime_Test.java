package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toZonedDateTime_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    /**
     * The ZoneId that the Converter is configured to use (defaults to the system zone).
     */
    private static ZoneId zoneId;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options.
        // The default ConverterOptions uses the JVM default time‑zone.
        converter = new Converter(new ConverterOptions());
        zoneId = converter.getOptions().getZoneId();
    }

    /**
     * Helper to create a Converter with a specific ZoneId.
     */
    private static Converter createConverter(ZoneId zone) {
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zone);
        return new Converter(options);
    }

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// ---- Date, Instant, Long, LocalDateTime (group: Valid conversions – Date, Instant, Long, LocalDateTime)
        Arguments.of(// 2021‑10‑01T00:00:00Z
        new Date(1633072800000L), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1633072800000L), ZoneId.of("UTC")), "Convert java.util.Date (2021‑10‑01T00:00:00Z) to ZonedDateTime with UTC"), Arguments.of(Instant.parse("2022-01-15T12:30:45Z"), ZoneId.of("Europe/Paris"), ZonedDateTime.ofInstant(Instant.parse("2022-01-15T12:30:45Z"), ZoneId.of("Europe/Paris")), "Convert Instant to ZonedDateTime with Europe/Paris"), Arguments.of(// 2022‑12‑31T23:59:59Z
        Long.valueOf(1672531199000L), ZoneId.of("America/New_York"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1672531199000L), ZoneId.of("America/New_York")), "Convert epoch‑millis Long to ZonedDateTime with America/New_York"), Arguments.of(LocalDateTime.of(2023, 3, 10, 14, 45, 30), ZoneId.of("Asia/Tokyo"), ZonedDateTime.of(LocalDateTime.of(2023, 3, 10, 14, 45, 30), ZoneId.of("Asia/Tokyo")), "Convert LocalDateTime to ZonedDateTime with Asia/Tokyo"), // ---- Calendar, Timestamp, LocalDate, Year (group: Valid conversions – Calendar, Timestamp, LocalDate, Year)
        Arguments.of(new GregorianCalendar(TimeZone.getTimeZone("UTC")) {

            {
                // 2021‑01‑01T00:00:00Z
                setTimeInMillis(1609459200000L);
            }
        }, ZoneId.of("Europe/Berlin"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1609459200000L), ZoneId.of("Europe/Berlin")), "Convert Calendar (2021‑01‑01T00:00:00Z) to ZonedDateTime with Europe/Berlin"), Arguments.of(Timestamp.valueOf("2022-06-15 12:34:56.789"), ZoneId.of("America/Chicago"), ZonedDateTime.ofInstant(Timestamp.valueOf("2022-06-15 12:34:56.789").toInstant(), ZoneId.of("America/Chicago")), "Convert Timestamp to ZonedDateTime with America/Chicago"), Arguments.of(LocalDate.of(2020, 2, 29), ZoneId.of("UTC"), ZonedDateTime.of(LocalDate.of(2020, 2, 29).atStartOfDay(ZoneId.of("UTC")), ZoneId.of("UTC")), "Convert leap‑year LocalDate to ZonedDateTime with UTC"), Arguments.of(Year.of(1999), ZoneId.of("Asia/Kolkata"), ZonedDateTime.of(LocalDate.of(1999, 1, 1).atStartOfDay(ZoneId.of("Asia/Kolkata")), ZoneId.of("Asia/Kolkata")), "Convert Year 1999 to ZonedDateTime with Asia/Kolkata"), // ---- Boundary epoch values (group: Boundary epoch values – zero and negative epoch)
        Arguments.of(Long.valueOf(0L), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.of("UTC")), "Convert epoch‑millis 0 to ZonedDateTime with UTC"), Arguments.of(// one day before epoch
        Long.valueOf(-86400000L), ZoneId.of("America/New_York"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(-86400000L), ZoneId.of("America/New_York")), "Convert negative epoch‑millis to ZonedDateTime with America/New_York"), // ---- Converter zone edge cases (group: Converter zone edge cases – ZoneOffset.UTC and custom offset zone)
        Arguments.of(Instant.parse("2023-12-31T23:00:00Z"), ZoneOffset.UTC, ZonedDateTime.ofInstant(Instant.parse("2023-12-31T23:00:00Z"), ZoneOffset.UTC), "Converter zone is fixed UTC offset"), Arguments.of(OffsetDateTime.parse("2023-07-01T12:00:00+05:30"), ZoneId.of("GMT+02:00"), ZonedDateTime.ofInstant(OffsetDateTime.parse("2023-07-01T12:00:00+05:30").toInstant(), ZoneId.of("GMT+02:00")), "OffsetDateTime source overridden by converter zone GMT+02:00"));
    }

    private static Stream<Arguments> exceptionScenariosProvider() {
        return Stream.of(// Null input (already covered above, but kept for completeness)
        Arguments.of(null, ZoneId.of("UTC"), NullPointerException.class, "Null source object should cause NullPointerException"), // Unsupported types – String, ArrayList, BigDecimal, BigInteger, OffsetTime
        Arguments.of("2021-01-01T00:00:00Z", ZoneId.of("UTC"), IllegalArgumentException.class, "String source is not supported"), Arguments.of(new ArrayList<>(), ZoneId.of("UTC"), IllegalArgumentException.class, "ArrayList source is not supported"), Arguments.of(new BigDecimal("12345.67"), ZoneId.of("UTC"), IllegalArgumentException.class, "BigDecimal source is not supported"), Arguments.of(BigInteger.valueOf(987654321L), ZoneId.of("UTC"), IllegalArgumentException.class, "BigInteger source is not supported"), Arguments.of(OffsetTime.parse("13:45:30+03:00"), ZoneId.of("Europe/Moscow"), IllegalArgumentException.class, "OffsetTime lacks date component and is unsupported"), // Zone handling – source ZonedDateTime should be re‑zoned
        Arguments.of(ZonedDateTime.parse("2022-12-25T15:30:00+01:00[Europe/Paris]"), ZoneId.of("Europe/London"), // no exception – this case belongs to successful conversions, but we test it here for clarity
        null, "ZonedDateTime source re‑zoned to converter's zone (London)")).filter(// filter out the non‑exception entry
        args -> args.get()[2] != null);
    }

    @Test
    void toZonedDateTime_fromInstant_returnsCorrectZonedDateTime() {
        // GIVEN
        Instant instant = Instant.now();
        converter = createConverter(ZoneId.systemDefault());
        // WHEN
        ZonedDateTime result = NumberConversions.toZonedDateTime(instant, converter);
        // THEN
        ZonedDateTime expected = instant.atZone(converter.getOptions().getZoneId());
        assertEquals(expected, result);
    }

    @Test
    void toZonedDateTime_fromDate_returnsCorrectZonedDateTime() {
        // GIVEN
        Date date = new Date();
        converter = createConverter(ZoneId.systemDefault());
        // WHEN
        ZonedDateTime result = NumberConversions.toZonedDateTime(date, converter);
        // THEN
        ZonedDateTime expected = date.toInstant().atZone(converter.getOptions().getZoneId());
        assertEquals(expected, result);
    }

    @Test
    void toZonedDateTime_fromLocalDateTime_returnsCorrectZonedDateTime() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MARCH, 15, 10, 45, 30);
        // WHEN
        ZonedDateTime result = NumberConversions.toZonedDateTime(ldt, converter);
        // THEN
        // The conversion path is: LocalDateTime -> Instant (using the converter's zone) -> ZonedDateTime
        ZonedDateTime expected = ldt.atZone(zoneId);
        assertEquals(expected, result);
    }

    @Test
    void toZonedDateTime_fromCalendar_returnsCorrectZonedDateTime() {
        // GIVEN
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(Instant.parse("2022-01-01T12:34:56Z").toEpochMilli());
        converter = createConverter(ZoneId.systemDefault());
        // WHEN
        ZonedDateTime result = NumberConversions.toZonedDateTime(cal, converter);
        // THEN
        ZonedDateTime expected = cal.toInstant().atZone(converter.getOptions().getZoneId());
        assertEquals(expected, result);
    }

    @Test
    void toZonedDateTime_fromLongEpochMilli_returnsCorrectZonedDateTime() {
        // GIVEN
        long epochMilli = System.currentTimeMillis();
        converter = createConverter(ZoneId.systemDefault());
        // WHEN
        ZonedDateTime result = NumberConversions.toZonedDateTime(epochMilli, converter);
        // THEN
        ZonedDateTime expected = Instant.ofEpochMilli(epochMilli).atZone(converter.getOptions().getZoneId());
        assertEquals(expected, result);
    }

    @Test
    void toZonedDateTime_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        converter = createConverter(ZoneId.systemDefault());
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> NumberConversions.toZonedDateTime(nullObj, converter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionsProvider")
    void toZonedDateTime_successfulConversions(Object from, ZoneId zone, ZonedDateTime expected, String description) {
        // GIVEN
        converter = createConverter(zone);
        // WHEN
        ZonedDateTime result = NumberConversions.toZonedDateTime(from, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionScenariosProvider")
    void toZonedDateTime_exceptionScenarios(Object from, ZoneId zone, Class<? extends Throwable> expectedException, String description) {
        // GIVEN
        converter = createConverter(zone);
        // WHEN / THEN
        assertThrows(expectedException, () -> NumberConversions.toZonedDateTime(from, converter), description);
    }

    @Test
    void toZonedDateTime_ZonedDateTimeSource_zoneIsOverriddenByConverter() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.parse("2022-12-25T15:30:00+01:00[Europe/Paris]");
        ZoneId targetZone = ZoneId.of("Europe/London");
        converter = createConverter(targetZone);
        ZonedDateTime expected = ZonedDateTime.ofInstant(source.toInstant(), targetZone);
        // WHEN
        ZonedDateTime result = NumberConversions.toZonedDateTime(source, converter);
        // THEN
        assertEquals(expected, result);
    }
}
