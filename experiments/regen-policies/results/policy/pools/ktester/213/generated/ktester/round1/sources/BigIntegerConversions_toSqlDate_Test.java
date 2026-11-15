package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import java.sql.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

class BigIntegerConversions_toSqlDate_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Default to UTC so that the “system default” used in the original test class
        // does not affect deterministic expectations.
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(ZoneId.of("UTC"));
        converter = new Converter(options);
    }

    /**
     * Creates a {@link Converter} whose {@link ConverterOptions} use the supplied {@link ZoneId}.
     */
    private static Converter converterWithZone(ZoneId zone) {
        ConverterOptions options = new ConverterOptions();
        options.setZoneId(zone);
        return new Converter(options);
    }

    /**
     * Computes the expected {@link java.sql.Date} for a given nanosecond count and zone.
     */
    private static Date expectedSqlDate(BigInteger nanos, ZoneId zone) {
        long epochMilli = nanos.divide(BigIntegerConversions.MILLION).longValue();
        LocalDate localDate = Instant.ofEpochMilli(epochMilli).atZone(zone).toLocalDate();
        return Date.valueOf(localDate);
    }

    /**
     * Supplies all test cases that should return a valid {@link java.sql.Date}.
     */
    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// group: toSqlDate conversion tests
        Arguments.of("Convert zero nanoseconds to SQL Date; epoch start date should be returned (UTC)", BigInteger.valueOf(0), converterWithZone(ZoneId.of("UTC")), expectedSqlDate(BigInteger.valueOf(0), ZoneId.of("UTC"))), Arguments.of("1.5 seconds (1,500,000,000 ns) still on epoch day (UTC)", BigInteger.valueOf(1_500_000_000L), converterWithZone(ZoneId.of("UTC")), expectedSqlDate(BigInteger.valueOf(1_500_000_000L), ZoneId.of("UTC"))), Arguments.of("Exactly one day in nanoseconds should yield 1970‑01‑02 (UTC)", new BigInteger("86400000000000"), converterWithZone(ZoneId.of("UTC")), expectedSqlDate(new BigInteger("86400000000000"), ZoneId.of("UTC"))), Arguments.of("Large nanosecond value with New York zone", new BigInteger("1234567890123456789"), converterWithZone(ZoneId.of("America/New_York")), expectedSqlDate(new BigInteger("1234567890123456789"), ZoneId.of("America/New_York"))), Arguments.of("Negative nanoseconds (one second before epoch) should produce previous day (UTC)", BigInteger.valueOf(-1_000_000_000L), converterWithZone(ZoneId.of("UTC")), expectedSqlDate(BigInteger.valueOf(-1_000_000_000L), ZoneId.of("UTC"))), // group: toSqlDate additional edge cases
        Arguments.of("Epoch nanoseconds with Los Angeles zone (UTC‑08) should shift to previous day", BigInteger.valueOf(0), converterWithZone(ZoneId.of("America/Los_Angeles")), expectedSqlDate(BigInteger.valueOf(0), ZoneId.of("America/Los_Angeles"))), Arguments.of("Exactly one year (365 days) expressed in nanoseconds (UTC)", new BigInteger("31536000000000000"), converterWithZone(ZoneId.of("UTC")), expectedSqlDate(new BigInteger("31536000000000000"), ZoneId.of("UTC"))), Arguments.of("Nanoseconds just before the end of the first day (23:59:59.999) – still epoch day (UTC)", new BigInteger("86399999999999"), converterWithZone(ZoneId.of("UTC")), expectedSqlDate(new BigInteger("86399999999999"), ZoneId.of("UTC"))), Arguments.of("Exactly one year before epoch (negative) – should be 1969‑01‑01 (UTC)", new BigInteger("-31536000000000000"), converterWithZone(ZoneId.of("UTC")), expectedSqlDate(new BigInteger("-31536000000000000"), ZoneId.of("UTC"))), Arguments.of("Exactly one millisecond (1,000,000 ns) – still epoch day (UTC)", BigInteger.valueOf(1_000_000L), converterWithZone(ZoneId.of("UTC")), expectedSqlDate(BigInteger.valueOf(1_000_000L), ZoneId.of("UTC"))), Arguments.of("23:59:59.999 UTC on epoch day with Tokyo (+09) – date remains 1970‑01‑01", new BigInteger("86399999999999"), converterWithZone(ZoneId.of("Asia/Tokyo")), expectedSqlDate(new BigInteger("86399999999999"), ZoneId.of("Asia/Tokyo"))));
    }

    /**
     * Supplies cases that should throw either NPE or ClassCastException.
     */
    private static Stream<Arguments> provideInvalidInputExceptions() {
        return Stream.of(// null 'from' -> NPE because of cast to BigInteger
        Arguments.of("Passing null as source should throw NullPointerException", null, converterWithZone(ZoneId.of("UTC")), NullPointerException.class), // wrong type -> ClassCastException
        Arguments.of("Passing a String instead of BigInteger should throw ClassCastException", "not a biginteger", converterWithZone(ZoneId.of("UTC")), ClassCastException.class));
    }

    /**
     * Supplies cases where the {@link Converter} itself is null or mis‑configured.
     */
    private static Stream<Arguments> provideNullConverterExceptions() {
        return Stream.of(Arguments.of("Null Converter should cause NullPointerException", BigInteger.valueOf(0), null, NullPointerException.class), Arguments.of("Converter constructed with null options should cause NPE when accessed", BigInteger.valueOf(0), // assuming such ctor exists
        new Converter(null), NullPointerException.class), Arguments.of("Converter with Options containing null ZoneId should cause NPE at atZone(null)", BigInteger.valueOf(0), new Converter(ConverterOptions.builder().zoneId(null).build()), NullPointerException.class));
    }

    @Test
    void toSqlDate_withZeroNanoseconds_returnsEpochDate() {
        // given
        // 0 nanoseconds = epoch start
        BigInteger nanos = BigInteger.ZERO;
        // when
        Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        // then
        Date expected = Date.valueOf(LocalDate.of(1970, 1, 1));
        assertEquals(expected, sqlDate);
    }

    @Test
    void toSqlDate_withNonZeroNanoseconds_returnsCorrespondingLocalDate() {
        // given
        // 1_600_000_000_000_000_000 nanoseconds = 1_600_000_000 milliseconds
        // which corresponds to 2020‑09‑13 (UTC). Using the system default ZoneId
        // yields the same LocalDate for this instant.
        BigInteger nanos = new BigInteger("1600000000000000000");
        // when
        Date sqlDate = BigIntegerConversions.toSqlDate(nanos, converter);
        // then
        // Compute the expected LocalDate using the same logic as the method under test
        long epochMilli = nanos.divide(BigIntegerConversions.MILLION).longValue();
        LocalDate expectedLocalDate = Instant.ofEpochMilli(epochMilli).atZone(converter.getOptions().getZoneId()).toLocalDate();
        Date expected = Date.valueOf(expectedLocalDate);
        assertEquals(expected, sqlDate);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulConversions")
    void toSqlDate_successfulConversions(String description, BigInteger from, Converter conv, Date expected) {
        // when
        Date actual = BigIntegerConversions.toSqlDate(from, conv);
        // then
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideInvalidInputExceptions")
    void toSqlDate_invalidInputs(String description, Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> BigIntegerConversions.toSqlDate(from, conv), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideNullConverterExceptions")
    void toSqlDate_nullConverterScenarios(String description, BigInteger from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> BigIntegerConversions.toSqlDate(from, conv), description);
    }
}
