package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toAtomicLong_Test {

    /**
     * Converter used for all test cases – created once for the whole test suite.
     */
    private static Converter converter;

    /**
     * Converter used for the majority of test cases – created once for the whole test suite.
     */
    private static Converter defaultConverter;

    /**
     * Simple holder for parameterized success test arguments.
     */
    private static final class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final long expected;

        final String description;

        ArgumentsHolder(Object from, Converter converter, long expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        // JUnit 5 requires a static method returning a Stream of Arguments.
        // We expose the fields via getters for the MethodSource.
        Object getFrom() {
            return from;
        }

        Converter getConverter() {
            return converter;
        }

        long getExpected() {
            return expected;
        }

        String getDescription() {
            return description;
        }
    }

    /**
     * Simple holder for parameterized failure test arguments.
     */
    private static final class ExceptionCase {

        final Object from;

        final Converter converter;

        final Class<? extends Throwable> expectedException;

        final String description;

        ExceptionCase(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
            this.from = from;
            this.converter = converter;
            this.expectedException = expectedException;
            this.description = description;
        }

        Object getFrom() {
            return from;
        }

        Converter getConverter() {
            return converter;
        }

        Class<? extends Throwable> getExpectedException() {
            return expectedException;
        }

        String getDescription() {
            return description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a public no‑arg constructor; it provides the default conversion map.
        ConverterOptions options = new ConverterOptions();
        defaultConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        defaultConverter = null;
    }

    /* --------------------------------------------------- *
     *  Successful conversion test data (expected AtomicLong)
     * --------------------------------------------------- */
    private static Stream<ArgumentsHolder> successCases() {
        return Stream.of(// null -> 0
        new ArgumentsHolder(null, null, 0L, "null input should become 0"), // Long instance
        new ArgumentsHolder(123L, null, 123L, "Long instance"), // String numeric
        new ArgumentsHolder("456", null, 456L, "String numeric"), // BigDecimal (truncates)
        new ArgumentsHolder(new BigDecimal("789.01"), null, 789L, "BigDecimal truncates"), // BigInteger
        new ArgumentsHolder(new BigInteger("9876543210"), null, 9876543210L, "BigInteger"), // Instant
        new ArgumentsHolder(Instant.ofEpochMilli(123456789L), null, 123456789L, "Instant epoch milli"), // OffsetDateTime
        new ArgumentsHolder(OffsetDateTime.parse("2023-01-01T12:34:56+02:00"), null, OffsetDateTime.parse("2023-01-01T12:34:56+02:00").toInstant().toEpochMilli(), "OffsetDateTime to epoch milli"), // java.util.Date
        new ArgumentsHolder(new Date(1609459200000L), null, 1609459200000L, "java.util.Date"), // java.sql.Timestamp
        new ArgumentsHolder(new Timestamp(1610000000000L), null, 1610000000000L, "Timestamp"), // Calendar
        new ArgumentsHolder(createCalendar(1620000000000L), null, 1620000000000L, "Calendar"), // Object with toString returning numeric string
        new ArgumentsHolder(new Object() {

            @Override
            public String toString() {
                return "321";
            }
        }, null, 321L, "Object with numeric toString"), // Integer
        new ArgumentsHolder(Integer.valueOf(42), null, 42L, "Integer"), // Short
        new ArgumentsHolder(Short.valueOf((short) 7), null, 7L, "Short"), // Byte
        new ArgumentsHolder(Byte.valueOf((byte) 3), null, 3L, "Byte"), // Float (truncated)
        new ArgumentsHolder(Float.valueOf(12.34f), null, 12L, "Float truncates"), // Double (truncated)
        new ArgumentsHolder(Double.valueOf(56.78), null, 56L, "Double truncates"), // java.sql.Date
        new ArgumentsHolder(java.sql.Date.valueOf("2022-05-01"), null, java.sql.Date.valueOf("2022-05-01").toInstant().toEpochMilli(), "java.sql.Date"), // Custom converter overrides default conversion
        new ArgumentsHolder("12345", new Converter() {

            @Override
            public long toLong(Object o) {
                return 999L;
            }
        }, 999L, "Custom converter returns 999"));
    }

    /* --------------------------------------------------- *
     *  Failure conversion test data (expected exception)
     * --------------------------------------------------- */
    private static Stream<ExceptionCase> failureCases() {
        return Stream.of(// Invalid numeric string
        new ExceptionCase("notANumber", null, NumberFormatException.class, "Invalid numeric string"), // Object without numeric toString
        new ExceptionCase(new Object() {

            @Override
            public String toString() {
                return "nonNumeric";
            }
        }, null, NumberFormatException.class, "Object with non‑numeric toString"), // Object with null toString
        new ExceptionCase(new Object() {

            @Override
            public String toString() {
                return null;
            }
        }, null, NumberFormatException.class, "Object with null toString"), // Empty string
        new ExceptionCase("", null, NumberFormatException.class, "Empty string"), // Whitespace only
        new ExceptionCase("   ", null, NumberFormatException.class, "Whitespace only string"), // OffsetTime (no epoch milli)
        new ExceptionCase(OffsetTime.parse("12:34:56+02:00"), null, NumberFormatException.class, "OffsetTime unsupported"), // Collection
        new ExceptionCase(new ArrayList<>(), null, NumberFormatException.class, "Collection object"), // Character
        new ExceptionCase(Character.valueOf('9'), null, NumberFormatException.class, "Character"), // ZonedDateTime
        new ExceptionCase(ZonedDateTime.parse("2023-01-01T12:34:56+02:00[Europe/Paris]"), null, NumberFormatException.class, "ZonedDateTime unsupported"), // LocalDateTime
        new ExceptionCase(LocalDateTime.parse("2023-01-01T12:34:56"), null, NumberFormatException.class, "LocalDateTime unsupported"), // BigDecimal NaN
        new ExceptionCase(new BigDecimal("NaN"), null, NumberFormatException.class, "BigDecimal NaN"), // BigInteger overflow
        new ExceptionCase(new BigInteger("-9223372036854775809"), null, ArithmeticException.class, "BigInteger underflow"), // BigDecimal overflow (Long.MAX_VALUE + 1)
        new ExceptionCase(BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE), null, ArithmeticException.class, "BigDecimal overflow"), // Custom converter throws IllegalArgumentException
        new ExceptionCase("any", new Converter() {

            @Override
            public long toLong(Object o) {
                throw new IllegalArgumentException("conversion failed");
            }
        }, IllegalArgumentException.class, "Custom converter throws IllegalArgumentException"), // Custom converter throws RuntimeException on null input
        new ExceptionCase(null, new Converter() {

            @Override
            public long toLong(Object o) {
                throw new RuntimeException("converter failure");
            }
        }, RuntimeException.class, "Custom converter throws RuntimeException for null"));
    }

    /**
     * Creates a {@link Calendar} instance set to the supplied epoch‑millis.
     */
    private static Calendar createCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    @Test
    void toAtomicLong_nullInput_returnsZeroAtomicLong() {
        // GIVEN
        Object from = null;
        // WHEN
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // THEN
        assertNotNull(result, "Resulting AtomicLong must not be null");
        assertEquals(0L, result.get(), "Null input should be converted to 0");
    }

    @Test
    void toAtomicLong_numberInput_returnsAtomicLongWithSameValue() {
        // GIVEN
        Long from = 123L;
        // WHEN
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // THEN
        assertNotNull(result);
        assertEquals(123L, result.get(), "Long input should be preserved");
    }

    @Test
    void toAtomicLong_offsetDateTimeInput_returnsEpochMilliAtomicLong() {
        // GIVEN
        // 2023‑01‑01T12:34:56.789+02:00
        OffsetDateTime from = OffsetDateTime.of(2023, 1, 1, 12, 34, 56, 789_000_000, ZoneOffset.ofHours(2));
        // Expected epoch milliseconds (Instant is always UTC)
        long expected = from.toInstant().toEpochMilli();
        // WHEN
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.get(), "OffsetDateTime should be converted to its epoch‑millisecond representation");
    }

    @Test
    void toAtomicLong_stringRepresentingNumber_returnsParsedAtomicLong() {
        // GIVEN
        String from = "456789";
        // WHEN
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, converter);
        // THEN
        assertNotNull(result);
        assertEquals(456_789L, result.get(), "String containing a numeric value should be parsed to the corresponding long");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("successCases")
    void toAtomicLong_successfulConversions(Object from, Converter converter, long expected, String description) {
        // GIVEN
        Converter used = (converter != null) ? converter : defaultConverter;
        // WHEN
        AtomicLong result = OffsetDateTimeConversions.toAtomicLong(from, used);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), "Unexpected conversion result for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("failureCases")
    void toAtomicLong_failureScenarios(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // GIVEN
        Converter used = (converter != null) ? converter : defaultConverter;
        // WHEN / THEN
        Throwable thrown = assertThrows(expectedException, () -> OffsetDateTimeConversions.toAtomicLong(from, used), "Expected exception for case: " + description);
        // ensure exception is not silently empty
        assertNotNull(thrown.getMessage());
    }
}
