package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

class BigDecimalConversions_toZonedDateTime_Test {

    private Converter converter;

    private Converter converterUTC;

    private Converter converterAmericaNewYork;

    private Converter converterEuropeBerlin;

    private Converter converterAmericaLosAngeles;

    private Converter converterEuropeParis;

    /**
     * Simple holder for parameterized test arguments.
     */
    private static class ArgumentsHolder {

        final Object input;

        final String converterName;

        final ZonedDateTime expectedZonedDateTime;

        final Class<? extends Throwable> expectedException;

        final String description;

        // Constructor for successful conversions
        ArgumentsHolder(Object input, String converterName, ZonedDateTime expected, String description) {
            this.input = input;
            this.converterName = converterName;
            this.expectedZonedDateTime = expected;
            this.expectedException = null;
            this.description = description;
        }

        // Constructor for exception‑expecting cases
        ArgumentsHolder(Object input, String converterName, Class<? extends Throwable> exception, String description) {
            this.input = input;
            this.converterName = converterName;
            this.expectedZonedDateTime = null;
            this.expectedException = exception;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Helper to create a Converter whose options contain the required ZoneId.
        converterUTC = createConverterWithZone(ZoneId.of("UTC"));
        converterAmericaNewYork = createConverterWithZone(ZoneId.of("America/New_York"));
        converterEuropeBerlin = createConverterWithZone(ZoneId.of("Europe/Berlin"));
        converterAmericaLosAngeles = createConverterWithZone(ZoneId.of("America/Los_Angeles"));
        converterEuropeParis = createConverterWithZone(ZoneId.of("Europe/Paris"));
    }

    /* --------------------------------------------------------------------- *
     *  Helper to build a Converter with a specific ZoneId.
     *  The real Converter class is not shown, so we use reflection to set the
     *  private field `options` (or the zone inside options) if necessary.
     * --------------------------------------------------------------------- */
    private static Converter createConverterWithZone(ZoneId zone) throws Exception {
        ConverterOptions options = new ConverterOptions();
        // Assume there is a setter; if not, use reflection.
        try {
            options.setZoneId(zone);
        } catch (NoSuchMethodError | NoSuchMethodException ignored) {
            // Fallback: reflectively set the private field `zoneId`
            Field f = ConverterOptions.class.getDeclaredField("zoneId");
            f.setAccessible(true);
            f.set(options, zone);
        }
        return new Converter(options);
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for the "Valid conversions – standard types" group
     * --------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> validStandardProvider() {
        return Stream.of(// java.util.Date
        new ArgumentsHolder(new Date(1672531200000L), "converterUTC", ZonedDateTime.ofInstant(Instant.ofEpochMilli(1672531200000L), ZoneId.of("UTC"))), // Instant
        new ArgumentsHolder(Instant.parse("2023-06-15T12:34:56Z"), "converterUTC", ZonedDateTime.ofInstant(Instant.parse("2023-06-15T12:34:56Z"), ZoneId.of("UTC"))), // java.sql.Timestamp
        new ArgumentsHolder(Timestamp.valueOf("2022-12-31 23:59:59.123"), "converterUTC", ZonedDateTime.ofInstant(Instant.ofEpochSecond(1672531199L, 123_000_000L), ZoneId.of("UTC"))), // LocalDateTime
        new ArgumentsHolder(LocalDateTime.of(2021, 5, 10, 14, 30, 15, 250_000_000), "converterUTC", ZonedDateTime.of(2021, 5, 10, 14, 30, 15, 250_000_000, ZoneId.of("UTC"))), // OffsetDateTime
        new ArgumentsHolder(OffsetDateTime.parse("2020-03-01T08:15:30+02:00"), "converterUTC", ZonedDateTime.ofInstant(OffsetDateTime.parse("2020-03-01T08:15:30+02:00").toInstant(), ZoneId.of("UTC"))), // Long seconds
        new ArgumentsHolder(Long.valueOf(1700000000L), "converterUTC", ZonedDateTime.ofInstant(Instant.ofEpochSecond(1700000000L), ZoneId.of("UTC"))), // BigDecimal with nanoseconds
        new ArgumentsHolder(new BigDecimal("1700000000.123456789"), "converterUTC", ZonedDateTime.ofInstant(Instant.ofEpochSecond(1700000000L, 123_456_789L), ZoneId.of("UTC"))), // ISO‑8601 string with Z
        new ArgumentsHolder("2022-11-30T10:20:30Z", "converterUTC", ZonedDateTime.parse("2022-11-30T10:20:30Z").withZoneSameInstant(ZoneId.of("UTC"))));
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for the "Invalid / edge cases" group
     * --------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> invalidProvider() {
        return Stream.of(new ArgumentsHolder(null, "converterUTC", NullPointerException.class, "Passing null source should throw NPE"), new ArgumentsHolder(new Object(), "converterUTC", ConversionException.class, "Unsupported source type should cause ConversionException"), new ArgumentsHolder("not-a-date", "converterUTC", DateTimeParseException.class, "Malformed date string should throw DateTimeParseException"), new ArgumentsHolder(new BigDecimal("-123.456"), "converterUTC", ZonedDateTime.ofInstant(Instant.ofEpochSecond(-123L, -456_000_000L), ZoneId.of("UTC")), "Negative epoch seconds should be handled correctly"));
    }

    /* --------------------------------------------------------------------- *
     *  Parameterized tests for the "Zone handling – non‑UTC zones" group
     * --------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> zoneHandlingProvider() {
        return Stream.of(new ArgumentsHolder(Date.from(Instant.parse("2023-08-01T12:00:00Z")), "converterAmericaNewYork", ZonedDateTime.ofInstant(Instant.parse("2023-08-01T12:00:00Z"), ZoneId.of("America/New_York")), "Converter zone America/New_York should be applied"), new ArgumentsHolder(LocalDateTime.of(2023, 3, 26, 2, 30), "converterEuropeBerlin", ZonedDateTime.of(2023, 3, 26, 2, 30, 0, 0, ZoneId.of("Europe/Berlin")), "DST transition – local fields must stay unchanged"));
    }

    /* --------------------------------------------------------------------- *
     *  Utility methods and holder class
     * --------------------------------------------------------------------- */
    private Converter resolveConverter(String name) {
        return switch(name) {
            case "converterUTC" ->
                converterUTC;
            case "converterAmericaNewYork" ->
                converterAmericaNewYork;
            case "converterEuropeBerlin" ->
                converterEuropeBerlin;
            case "converterAmericaLosAngeles" ->
                converterAmericaLosAngeles;
            case "converterEuropeParis" ->
                converterEuropeParis;
            default ->
                throw new IllegalArgumentException("Unknown converter name: " + name);
        };
    }

    @Test
    void toZonedDateTimeFromDate() {
        // 2021‑12‑01T00:00:00Z
        Date date = new Date(1638316800000L);
        ZonedDateTime expected = date.toInstant().atZone(ZoneId.systemDefault());
        ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(date, converter);
        assertEquals(expected, actual, "ZonedDateTime derived from java.util.Date should match expected value");
    }

    @Test
    void toZonedDateTimeFromInstant() {
        // 2022‑01‑15T12:34:56.789Z
        Instant instant = Instant.parse("2022-01-15T12:34:56.789Z");
        ZonedDateTime expected = instant.atZone(ZoneId.systemDefault());
        ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(instant, converter);
        assertEquals(expected, actual, "ZonedDateTime derived from java.time.Instant should match expected value");
    }

    @Test
    void toZonedDateTimeFromEpochMillisLong() {
        // Epoch milliseconds for 2020‑06‑30T08:15:30Z
        Long epochMillis = 1593516930000L;
        ZonedDateTime expected = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault());
        ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(epochMillis, converter);
        assertEquals(expected, actual, "ZonedDateTime derived from epoch‑millis Long should match expected value");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validStandardProvider")
    void testValidStandardConversions(ArgumentsHolder holder) {
        Converter conv = resolveConverter(holder.converterName);
        Object input = holder.input;
        ZonedDateTime expected = holder.expected;
        ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(input, conv);
        assertEquals(expected, actual, holder.description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidProvider")
    void testInvalidConversions(ArgumentsHolder holder) {
        Converter conv = resolveConverter(holder.converterName);
        if (holder.expectedException != null) {
            assertThrows(holder.expectedException, () -> BigDecimalConversions.toZonedDateTime(holder.input, conv), holder.description);
        } else {
            // Expected a valid ZonedDateTime (negative epoch case)
            ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(holder.input, conv);
            assertEquals(holder.expectedZonedDateTime, actual, holder.description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zoneHandlingProvider")
    void testZoneHandling(ArgumentsHolder holder) {
        Converter conv = resolveConverter(holder.converterName);
        ZonedDateTime actual = BigDecimalConversions.toZonedDateTime(holder.input, conv);
        assertEquals(holder.expectedZonedDateTime, actual, holder.description);
    }

    /* --------------------------------------------------------------------- *
     *  Additional valid conversion groups (date‑only, Calendar, numeric, etc.)
     * --------------------------------------------------------------------- */
    @Test
    void testLocalDateConversion() {
        Converter conv = converterUTC;
        LocalDate date = LocalDate.of(2022, 12, 31);
        ZonedDateTime expected = ZonedDateTime.of(2022, 12, 31, 0, 0, 0, 0, ZoneId.of("UTC"));
        assertEquals(expected, BigDecimalConversions.toZonedDateTime(date, conv));
    }

    @Test
    void testSqlDateConversion() {
        Converter conv = converterAmericaNewYork;
        java.sql.Date sqlDate = new java.sql.Date(LocalDate.of(2021, 7, 15).toEpochDay() * 24L * 60L * 60L * 1000L);
        ZonedDateTime expected = ZonedDateTime.of(2021, 7, 15, 0, 0, 0, 0, ZoneId.of("America/New_York"));
        assertEquals(expected, BigDecimalConversions.toZonedDateTime(sqlDate, conv));
    }

    @Test
    void testCalendarConversion() {
        Converter conv = converterEuropeBerlin;
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 20, 14, 45, 30, 0, ZoneId.of("Europe/Paris"));
        Calendar cal = GregorianCalendar.from(source);
        ZonedDateTime expected = ZonedDateTime.ofInstant(source.toInstant(), ZoneId.of("Europe/Berlin"));
        assertEquals(expected, BigDecimalConversions.toZonedDateTime(cal, conv));
    }

    @Test
    void testInstantWithNonUtcZone() {
        Converter conv = converterAmericaLosAngeles;
        Instant instant = Instant.parse("2038-01-19T03:14:07Z");
        ZonedDateTime expected = ZonedDateTime.ofInstant(instant, ZoneId.of("America/Los_Angeles"));
        assertEquals(expected, BigDecimalConversions.toZonedDateTime(instant, conv));
    }

    @Test
    void testLongEpochMilliLargeValue() {
        Converter conv = converterUTC;
        Long epochMilli = 1_700_000_000_123L;
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.of("UTC"));
        assertEquals(expected, BigDecimalConversions.toZonedDateTime(epochMilli, conv));
    }

    @Test
    void testBigDecimalExtraFractionDigits() {
        Converter conv = converterUTC;
        BigDecimal bd = new BigDecimal("1700000000.123456789123");
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1700000000L, 123_456_789L), ZoneId.of("UTC"));
        assertEquals(expected, BigDecimalConversions.toZonedDateTime(bd, conv));
    }

    @Test
    void testBigIntegerConversion() {
        Converter conv = converterUTC;
        BigInteger bi = BigInteger.valueOf(1700000000L);
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochSecond(1700000000L), ZoneId.of("UTC"));
        assertEquals(expected, BigDecimalConversions.toZonedDateTime(bi, conv));
    }

    @Test
    void testStringWithPositiveOffset() {
        Converter conv = converterUTC;
        String src = "2022-11-30T10:20:30+02:00";
        ZonedDateTime expected = ZonedDateTime.parse("2022-11-30T10:20:30+02:00").withZoneSameInstant(ZoneId.of("UTC"));
        assertEquals(expected, BigDecimalConversions.toZonedDateTime(src, conv));
    }

    @Test
    void testDateOnlyStringInZone() {
        Converter conv = converterAmericaNewYork;
        String src = "2022-11-30";
        ZonedDateTime expected = ZonedDateTime.of(2022, 11, 30, 0, 0, 0, 0, ZoneId.of("America/New_York"));
        assertEquals(expected, BigDecimalConversions.toZonedDateTime(src, conv));
    }

    @Test
    void testVeryLargeEpochSecond() {
        Converter conv = converterUTC;
        long large = Long.MAX_VALUE / 1_000_000_000L;
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochSecond(large), ZoneId.of("UTC"));
        assertEquals(expected, BigDecimalConversions.toZonedDateTime(Long.valueOf(large), conv));
    }

    @Test
    void testBigDecimalJustBelowOneSecond() {
        Converter conv = converterUTC;
        BigDecimal bd = new BigDecimal("0.9999999999");
        ZonedDateTime expected = ZonedDateTime.ofInstant(Instant.ofEpochSecond(0L, 999_999_999L), ZoneId.of("UTC"));
        assertEquals(expected, BigDecimalConversions.toZonedDateTime(bd, conv));
    }

    @Test
    void testUnsupportedNumericTypeFloat() {
        Converter conv = converterUTC;
        Float f = Float.valueOf(12345.67f);
        assertThrows(ConversionException.class, () -> BigDecimalConversions.toZonedDateTime(f, conv), "Float is not a supported numeric type");
    }

    @Test
    void testMalformedStringInvalidMonth() {
        Converter conv = converterUTC;
        String src = "2022-13-01T00:00:00Z";
        assertThrows(DateTimeParseException.class, () -> BigDecimalConversions.toZonedDateTime(src, conv), "Invalid month should cause DateTimeParseException");
    }

    @Test
    void testNullConverter() {
        Instant instant = Instant.parse("2023-01-01T00:00:00Z");
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toZonedDateTime(instant, null), "Null converter should cause NPE");
    }
}
