package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toOffsetDateTime_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Simple holder for successful test arguments.
     */
    private static class ArgumentsHolder {

        final Object from;

        final OffsetDateTime expected;

        final String description;

        ArgumentsHolder(Object from, OffsetDateTime expected, String description) {
            this.from = from;
            this.expected = expected;
            this.description = description;
        }

        // JUnit ParameterizedTest expects a single argument, so we unpack via a method source.
        static Stream<org.junit.jupiter.params.provider.Arguments> stream() {
            // not used
            return Stream.empty();
        }

        // Provide a method to convert to JUnit Arguments
        org.junit.jupiter.params.provider.Arguments toArguments() {
            return org.junit.jupiter.params.provider.Arguments.of(from, expected, description);
        }
    }

    /**
     * Simple holder for exception test arguments.
     */
    private static class ExceptionHolder {

        final Object from;

        final Class<? extends Throwable> expectedException;

        final String description;

        final boolean useNullConverter;

        ExceptionHolder(Object from, Class<? extends Throwable> expectedException, String description) {
            this(from, expectedException, description, false);
        }

        ExceptionHolder(Object from, Class<? extends Throwable> expectedException, String description, boolean useNullConverter) {
            this.from = from;
            this.expectedException = expectedException;
            this.description = description;
            this.useNullConverter = useNullConverter;
        }

        org.junit.jupiter.params.provider.Arguments toArguments() {
            return org.junit.jupiter.params.provider.Arguments.of(this);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared converter reference.
        converter = null;
    }

    /**
     * Source for successful conversions that should return an OffsetDateTime.
     */
    static Stream<ArgumentsHolder> successfulConversions() {
        return Stream.of(// String ISO8601 with explicit offset
        new ArgumentsHolder("2022-03-15T10:15:30+01:00", OffsetDateTime.parse("2022-03-15T10:15:30+01:00"), "String ISO8601 with explicit offset"), // java.util.Date (epoch 0)
        new ArgumentsHolder(new Date(0L), OffsetDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), "java.util.Date (epoch 0)"), // java.sql.Timestamp with nanoseconds
        new ArgumentsHolder(Timestamp.valueOf("2021-12-31 23:59:59.123456789"), OffsetDateTime.ofInstant(Instant.ofEpochSecond(Timestamp.valueOf("2021-12-31 23:59:59.123456789").getTime() / 1000, Timestamp.valueOf("2021-12-31 23:59:59.123456789").getNanos()), ZoneOffset.UTC), "java.sql.Timestamp with nanoseconds"), // java.time.Instant
        new ArgumentsHolder(Instant.parse("2020-06-01T12:00:00Z"), OffsetDateTime.ofInstant(Instant.parse("2020-06-01T12:00:00Z"), ZoneOffset.UTC), "java.time.Instant"), // ZonedDateTime with specific zone
        new ArgumentsHolder(ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.of("Europe/Paris")), OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(1)), "ZonedDateTime with specific zone"), // OffsetDateTime input (identity)
        new ArgumentsHolder(OffsetDateTime.parse("2024-04-04T04:04:04+05:30"), OffsetDateTime.parse("2024-04-04T04:04:04+05:30"), "OffsetDateTime input (identity)"));
    }

    /**
     * Source for cases that must throw an exception.
     */
    static Stream<ExceptionHolder> exceptionConversions() {
        return Stream.of(// null input -> NullPointerException (as per JSON expectation)
        new ExceptionHolder(null, NullPointerException.class, "null input"), // Unsupported type (Integer)
        new ExceptionHolder(12345, IllegalArgumentException.class, "Unsupported type (Integer)"), // Malformed ISO8601 string
        new ExceptionHolder("not-a-date", DateTimeParseException.class, "Malformed ISO8601 string"), // String without offset
        new ExceptionHolder("2022-03-15T10:15:30", DateTimeParseException.class, "String without offset"), // Unsupported type (Boolean)
        new ExceptionHolder(true, IllegalArgumentException.class, "Unsupported type (Boolean)"), // Unsupported type (BigDecimal)
        new ExceptionHolder(new BigDecimal("123.45"), IllegalArgumentException.class, "Unsupported type (BigDecimal)"), // LocalTime input (no date)
        new ExceptionHolder(LocalTime.of(12, 30, 45), DateTimeException.class, "LocalTime input (no date)"), // LocalDate input (no time)
        new ExceptionHolder(LocalDate.of(2022, 3, 15), DateTimeException.class, "LocalDate input (no time)"), // null converter
        new ExceptionHolder("2022-03-15T10:15:30+01:00", NullPointerException.class, "null converter", true));
    }

    // JUnit 5 does not automatically unpack custom holder objects, so we need a bridging method.
    static Stream<org.junit.jupiter.params.provider.Arguments> successfulConversions() {
        return successfulConversions().map(ArgumentsHolder::toArguments);
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> exceptionConversions() {
        return exceptionConversions().map(ExceptionHolder::toArguments);
    }

    @Test
    void toOffsetDateTime_fromBigIntegerEpochZero_returns1970Z() {
        // given
        // 1970‑01‑01T00:00:00Z
        BigInteger epochSeconds = BigInteger.ZERO;
        // when
        OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(epochSeconds, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(OffsetDateTime.ofInstant(java.time.Instant.EPOCH, ZoneOffset.UTC), result, "OffsetDateTime for epoch 0 should be 1970‑01‑01T00:00Z");
    }

    @Test
    void toOffsetDateTime_fromBigIntegerPositiveEpoch_returnsCorrectOffsetDateTime() {
        // given
        // 2023‑03‑15T12:34:56Z  => epoch seconds = 1678886096
        BigInteger epochSeconds = BigInteger.valueOf(1_678_886_096L);
        // when
        OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(epochSeconds, converter);
        // then
        OffsetDateTime expected = OffsetDateTime.ofInstant(java.time.Instant.ofEpochSecond(1_678_886_096L), ZoneOffset.UTC);
        assertEquals(expected, result, "OffsetDateTime should match the instant represented by the given epoch seconds");
    }

    @Test
    void toOffsetDateTime_fromNull_returnsNull() {
        // given
        Object from = null;
        // when
        OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(from, converter);
        // then
        assertNull(result, "Converting a null value should yield null");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversions")
    void toOffsetDateTime_successful(Object from, OffsetDateTime expected, String description) {
        // when
        OffsetDateTime result = BigIntegerConversions.toOffsetDateTime(from, converter);
        // then
        assertNotNull(result, description + " – result should not be null");
        assertEquals(expected, result, description + " – OffsetDateTime mismatch");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionConversions")
    void toOffsetDateTime_exceptions(ExceptionHolder holder) {
        Object from = holder.from;
        Class<? extends Throwable> expectedException = holder.expectedException;
        String description = holder.description;
        Converter usedConverter = holder.useNullConverter ? null : converter;
        // when / then
        assertThrows(expectedException, () -> BigIntegerConversions.toOffsetDateTime(from, usedConverter), description + " – expected exception");
    }
}
