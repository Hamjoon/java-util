package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toZonedDateTime_Test {

    /**
     * Converter used for all tests – created once for the whole test class.
     */
    private static Converter converter;

    /**
     * The zone that the {@link Converter} will apply when building a {@link ZonedDateTime}.
     */
    private static final ZoneId TEST_ZONE = ZoneId.of("UTC");

    /**
     * Simple container for the parameters of a single test case.
     * For successful conversions we store the expected {@link ZonedDateTime};
     * for failure cases we store the expected exception class.
     */
    private static class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final ZonedDateTime expectedZdt;

        final Class<? extends Throwable> expectedException;

        final String description;

        // Success case constructor
        ArgumentsHolder(Object from, Converter converter, ZonedDateTime expectedZdt, String description) {
            this.from = from;
            this.converter = converter;
            this.expectedZdt = expectedZdt;
            this.expectedException = null;
            this.description = description;
        }

        // Failure case constructor
        ArgumentsHolder(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
            this.from = from;
            this.converter = converter;
            this.expectedZdt = null;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    /**
     * Minimal stub for {@link Converter} used in the tests.
     *
     * The real {@code Converter} class is fairly heavyweight; for unit testing
     * {@code BigDecimalConversions#toZonedDateTime} we only need the ability to
     * return a {@link ConverterOptions} instance that supplies a {@link ZoneId}.
     */
    class ConverterStub extends Converter {

        private ConverterStub(ConverterOptions options) {
            super(options);
        }

        static ConverterStub withZone(ZoneId zone) {
            ConverterOptions opts = new ConverterOptions();
            opts.setZoneId(zone);
            return new ConverterStub(opts);
        }

        static ConverterStub withNullZone() {
            ConverterOptions opts = new ConverterOptions();
            opts.setZoneId(null);
            return new ConverterStub(opts);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Build ConverterOptions with a deterministic ZoneId so the expected results are reproducible.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(TEST_ZONE);
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up.
        converter = null;
    }

    private static Stream<ArgumentsHolder> validSupportedSources() {
        return Stream.of(// Date (epoch millis)
        new ArgumentsHolder(new Date(1_600_000_000_000L), ConverterStub.withZone(ZoneId.of("UTC")), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1_600_000_000_000L), ZoneId.of("UTC")), "Convert a java.util.Date (epoch milliseconds) to ZonedDateTime using UTC zone."), // Timestamp with nanos
        new ArgumentsHolder(Timestamp.valueOf("2023-03-15 12:34:56.123456789"), ConverterStub.withZone(ZoneId.of("America/New_York")), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1678880096L, 123_456_789L), ZoneId.of("America/New_York")), "Convert a java.sql.Timestamp (with nanos) to ZonedDateTime using New York zone."), // Instant
        new ArgumentsHolder(Instant.parse("2022-12-31T23:59:59Z"), ConverterStub.withZone(ZoneId.of("Asia/Tokyo")), ZonedDateTime.ofInstant(Instant.parse("2022-12-31T23:59:59Z"), ZoneId.of("Asia/Tokyo")), "Pass an Instant directly; the method should return the same instant in the specified Tokyo zone."), // Calendar (zone aware)
        new ArgumentsHolder(Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris")), ConverterStub.withZone(ZoneId.of("Europe/Paris")), ZonedDateTime.ofInstant(((Calendar) Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"))).toInstant(), ZoneId.of("Europe/Paris")), "Convert a Calendar instance (with its own time‑zone) to ZonedDateTime using the same zone."), // Long (epoch seconds)
        new ArgumentsHolder(Long.valueOf(1_600_000_000L), ConverterStub.withZone(ZoneId.of("UTC")), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1_600_000_000L), ZoneId.of("UTC")), "Convert a Long representing epoch seconds to ZonedDateTime."), // String with zone
        new ArgumentsHolder("2021-07-20T15:45:30+02:00[Europe/Paris]", ConverterStub.withZone(ZoneId.of("Europe/Paris")), ZonedDateTime.parse("2021-07-20T15:45:30+02:00[Europe/Paris]"), "Convert an ISO‑8601 string that already contains zone information."));
    }

    private static Stream<ArgumentsHolder> validOtherTemporalSources() {
        return Stream.of(// LocalDateTime
        new ArgumentsHolder(LocalDateTime.of(2022, 5, 10, 12, 30, 45), ConverterStub.withZone(ZoneId.of("UTC")), ZonedDateTime.of(2022, 5, 10, 12, 30, 45, 0, ZoneId.of("UTC")), "Convert a LocalDateTime to ZonedDateTime using UTC."), // LocalDate
        new ArgumentsHolder(LocalDate.of(2021, 12, 31), ConverterStub.withZone(ZoneId.of("Asia/Kolkata")), ZonedDateTime.of(LocalDate.of(2021, 12, 31).atStartOfDay(ZoneId.of("Asia/Kolkata")), ZoneId.of("Asia/Kolkata")), "Convert a LocalDate (no time) to ZonedDateTime; start of day in the converter's zone."), // LocalTime
        new ArgumentsHolder(LocalTime.of(23, 59, 59, 123_000_000), ConverterStub.withZone(ZoneId.of("Europe/London")), ZonedDateTime.of(LocalDate.of(1970, 1, 1), LocalTime.of(23, 59, 59, 123_000_000), ZoneId.of("Europe/London")), "Convert a LocalTime to ZonedDateTime; date defaults to epoch."), // OffsetDateTime
        new ArgumentsHolder(OffsetDateTime.parse("2023-01-01T10:15:30+02:00"), ConverterStub.withZone(ZoneId.of("UTC")), ZonedDateTime.ofInstant(OffsetDateTime.parse("2023-01-01T10:15:30+02:00").toInstant(), ZoneId.of("UTC")), "Convert an OffsetDateTime to ZonedDateTime in UTC."), // OffsetTime
        new ArgumentsHolder(OffsetTime.parse("13:45:30+05:30"), ConverterStub.withZone(ZoneId.of("America/Los_Angeles")), ZonedDateTime.ofInstant(OffsetTime.parse("13:45:30+05:30").atDate(LocalDate.of(1970, 1, 1)).toInstant(), ZoneId.of("America/Los_Angeles")), "Convert an OffsetTime (no date) to ZonedDateTime; date defaults to epoch."), // BigDecimal epoch seconds with nanos
        new ArgumentsHolder(new BigDecimal("1600000000.123456789"), ConverterStub.withZone(ZoneId.of("UTC")), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1_600_000_000L, 123_456_789L), ZoneId.of("UTC")), "Convert a BigDecimal representing seconds with fractional nanos."), // Double epoch seconds with nanos
        new ArgumentsHolder(Double.valueOf(1600000000.1234567), ConverterStub.withZone(ZoneId.of("UTC")), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1_600_000_000L, 123_456_700L), ZoneId.of("UTC")), "Convert a Double representing epoch seconds with fractional part."), // String without zone (LocalDateTime interpretation)
        new ArgumentsHolder("2022-08-15T12:34:56", ConverterStub.withZone(ZoneId.of("Europe/Paris")), ZonedDateTime.of(LocalDateTime.parse("2022-08-15T12:34:56"), ZoneId.of("Europe/Paris")), "Convert an ISO‑8601 string *without* zone information."));
    }

    private static Stream<ArgumentsHolder> invalidConversions() {
        return Stream.of(// null source
        new ArgumentsHolder(null, ConverterStub.withZone(ZoneId.of("UTC")), NullPointerException.class, "Passing a null source object should result in a NullPointerException."), // unsupported object
        new ArgumentsHolder(new Object(), ConverterStub.withZone(ZoneId.of("UTC")), IllegalArgumentException.class, "An unsupported source type (plain Object) must cause an IllegalArgumentException."), // malformed string
        new ArgumentsHolder("not-a-date-time", ConverterStub.withZone(ZoneId.of("UTC")), DateTimeParseException.class, "A malformed date‑time string should propagate a DateTimeParseException."), // BigDecimal overflow
        new ArgumentsHolder(BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE), ConverterStub.withZone(ZoneId.of("UTC")), DateTimeException.class, "A BigDecimal value that exceeds the range of Instant should cause a DateTimeException."), // null converter
        new ArgumentsHolder(Instant.now(), null, NullPointerException.class, "Passing a null Converter should cause a NullPointerException."), // converter with null zone
        new ArgumentsHolder(Instant.parse("2022-01-01T00:00:00Z"), ConverterStub.withNullZone(), NullPointerException.class, "A Converter whose getOptions().getZoneId() returns null leads to a NullPointerException."), // unsupported numeric type (Float)
        new ArgumentsHolder(Float.valueOf(123.45f), ConverterStub.withZone(ZoneId.of("UTC")), IllegalArgumentException.class, "Float is not a supported numeric source for toInstant."), // Double.NaN
        new ArgumentsHolder(Double.NaN, ConverterStub.withZone(ZoneId.of("UTC")), DateTimeException.class, "NaN cannot be interpreted as epoch seconds; conversion should raise DateTimeException."), // Double.POSITIVE_INFINITY
        new ArgumentsHolder(Double.POSITIVE_INFINITY, ConverterStub.withZone(ZoneId.of("UTC")), DateTimeException.class, "Infinity is out of the valid range for epoch seconds; conversion must fail."), // empty string
        new ArgumentsHolder("", ConverterStub.withZone(ZoneId.of("UTC")), DateTimeParseException.class, "An empty string cannot be parsed as a date‑time."), // BigInteger overflow
        new ArgumentsHolder(BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN), ConverterStub.withZone(ZoneId.of("UTC")), DateTimeException.class, "A BigInteger far beyond the Instant range should cause a DateTimeException."));
    }

    @Test
    void toZonedDateTime_fromDate_returnsCorrectZonedDateTime() {
        // GIVEN
        // 1970‑01‑01T00:00:00Z
        Date source = new Date(0L);
        ZonedDateTime expected = ZonedDateTime.ofInstant(source.toInstant(), TEST_ZONE);
        // WHEN
        ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(source, converter);
        // THEN
        assertEquals(expected, actual, "ZonedDateTime derived from java.util.Date should match expected value");
    }

    @Test
    void toZonedDateTime_fromInstant_returnsCorrectZonedDateTime() {
        // GIVEN
        Instant source = Instant.parse("2021-06-01T12:34:56.789Z");
        ZonedDateTime expected = ZonedDateTime.ofInstant(source, TEST_ZONE);
        // WHEN
        ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(source, converter);
        // THEN
        assertEquals(expected, actual, "ZonedDateTime derived from java.time.Instant should match expected value");
    }

    @Test
    void toZonedDateTime_fromLocalDateTime_returnsCorrectZonedDateTime() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2022, Month.MARCH, 15, 10, 20, 30, 123_456_789);
        // The Converter first turns the LocalDateTime into an Instant using the zone from options.
        ZonedDateTime expected = source.atZone(TEST_ZONE);
        // WHEN
        ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(source, converter);
        // THEN
        assertEquals(expected, actual, "ZonedDateTime derived from LocalDateTime should respect the Converter's zone");
    }

    @Test
    void toZonedDateTime_fromBigDecimalEpochSeconds_returnsCorrectZonedDateTime() {
        // GIVEN
        // BigDecimal representing epoch seconds with fractional part (e.g., 1622548800.123456789)
        // 2021‑06‑01T12:00:00.123456789Z
        BigDecimal source = new BigDecimal("1622548800.123456789");
        Instant instant = Instant.ofEpochSecond(1622548800L, 123_456_789L);
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, TEST_ZONE);
        // WHEN
        ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(source, converter);
        // THEN
        assertEquals(expected, actual, "ZonedDateTime derived from BigDecimal epoch seconds should be correct");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validSupportedSources")
    void toZonedDateTime_validSupportedSources(ArgumentsHolder holder) {
        // WHEN
        ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(holder.from, holder.converter);
        // THEN
        assertEquals(holder.expectedZdt, actual, holder.description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validOtherTemporalSources")
    void toZonedDateTime_validOtherTemporalSources(ArgumentsHolder holder) {
        ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(holder.from, holder.converter);
        assertEquals(holder.expectedZdt, actual, holder.description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidConversions")
    void toZonedDateTime_invalidConversions(ArgumentsHolder holder) {
        if (holder.expectedException != null) {
            assertThrows(holder.expectedException, () -> BigDecimalConversions.toZonedDateTime(holder.from, holder.converter), holder.description);
        } else {
            fail("Test data error: expectedException must be non‑null for invalid cases.");
        }
    }
}
