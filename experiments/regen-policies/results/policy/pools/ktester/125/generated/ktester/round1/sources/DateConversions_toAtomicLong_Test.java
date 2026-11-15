package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toAtomicLong_Test {

    /**
     * The Converter used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – they provide all built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies the data for {@link #toAtomicLong_validConversions(Object, long, String)}.
     *
     * @return a stream of arguments: source object, expected long value, description
     */
    private static Stream<Arguments> validConversionProvider() {
        // Helper values used by several test cases
        // 2021‑01‑01T00:00:00Z
        long epoch2021_01_01_00_00_00Z = 1609459200000L;
        // 2021‑01‑01T00:00+02:00 → UTC
        long offsetMinus2Hours = epoch2021_01_01_00_00_00Z - 2 * 3600_000L;
        // DST transition in New York (2021‑03‑14 02:30 local time does not exist; it is interpreted as 03:30 EDT)
        // verified epoch for the given ZonedDateTime
        long dstTransitionMillis = 1615703400000L;
        // Calendar instance set to the current instant in UTC
        Calendar utcNow = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long nowMillis = utcNow.getTimeInMillis();
        return Stream.of(// 1. null input, null converter → 0
        Arguments.of(null, 0L, "null input, null converter"), // 2. Long value
        Arguments.of(123L, 123L, "Long input"), // 3. String containing a number
        Arguments.of("456", 456L, "String numeric"), // 4. java.util.Date
        Arguments.of(new Date(epoch2021_01_01_00_00_00Z), epoch2021_01_01_00_00_00Z, "Date instance"), // 5. java.sql.Timestamp
        Arguments.of(new Timestamp(epoch2021_01_01_00_00_00Z), epoch2021_01_01_00_00_00Z, "Timestamp instance"), // 6. java.time.Instant
        Arguments.of(Instant.ofEpochMilli(epoch2021_01_01_00_00_00Z), epoch2021_01_01_00_00_00Z, "Instant instance"), // 7. LocalDate (interpreted as start of day UTC)
        Arguments.of(LocalDate.of(2021, 1, 1), epoch2021_01_01_00_00_00Z, "LocalDate start of day UTC"), // 8. LocalDateTime (interpreted as start of day UTC)
        Arguments.of(LocalDateTime.of(2021, 1, 1, 0, 0, 0), epoch2021_01_01_00_00_00Z, "LocalDateTime start of day UTC"), // 9. ZonedDateTime UTC
        Arguments.of(ZonedDateTime.parse("2021-01-01T00:00:00Z"), epoch2021_01_01_00_00_00Z, "ZonedDateTime UTC"), // 10. BigDecimal with fractional part (truncated)
        Arguments.of(new BigDecimal("789.123"), 789L, "BigDecimal with fraction"), // 11. BigInteger
        Arguments.of(new BigInteger("9876543210"), 9876543210L, "BigInteger positive"), // 12. Unsupported Map → 0
        Arguments.of(new HashMap<>(), 0L, "Empty HashMap (unsupported)"), // 13. Non‑numeric String → 0
        Arguments.of("not a number", 0L, "Non‑numeric String"), // 14. Negative BigDecimal with fraction (truncates toward zero)
        Arguments.of(new BigDecimal("-123.999"), -123L, "Negative BigDecimal with fraction"), // 15. String with surrounding whitespace
        Arguments.of("   42   ", 42L, "String with whitespace"), // 16. Hexadecimal String (not decimal) → 0
        Arguments.of("0x1A", 0L, "Hexadecimal String"), // 17. OffsetDateTime with +02:00 offset
        Arguments.of(OffsetDateTime.parse("2021-01-01T00:00:00+02:00"), offsetMinus2Hours, "OffsetDateTime +02:00"), // 18. LocalTime (no date) → 0
        Arguments.of(LocalTime.of(12, 34, 56), 0L, "LocalTime only"), // 19. Calendar instance set to current UTC time
        Arguments.of(utcNow, nowMillis, "Calendar (UTC now)"), // 20. BigInteger equal to Long.MIN_VALUE
        Arguments.of(new BigInteger("-9223372036854775808"), Long.MIN_VALUE, "BigInteger Long.MIN_VALUE"), // 21. BigInteger equal to Long.MAX_VALUE
        Arguments.of(new BigInteger("9223372036854775807"), Long.MAX_VALUE, "BigInteger Long.MAX_VALUE"), // 22. Very large BigDecimal (truncated to Long.MAX_VALUE)
        Arguments.of(new BigDecimal("12345678901234567890.999"), Long.MAX_VALUE, "BigDecimal exceeding Long.MAX_VALUE"), // 23. ZonedDateTime during DST transition (America/New_York)
        Arguments.of(ZonedDateTime.parse("2021-03-14T02:30:00-05:00[America/New_York]"), dstTransitionMillis, "ZonedDateTime DST transition"));
    }

    /**
     * Supplies arguments for {@link #toAtomicLong_exceptions(Object, Converter, Class, String)}.
     *
     * @return a stream of arguments: source object, custom converter (or null), expected exception class, description
     */
    private static Stream<Arguments> exceptionProvider() {
        // 1. Converter that throws RuntimeException
        Converter throwingConverter = new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("converter failure");
            }
        };
        // 2. Converter that returns null (causing NPE later)
        Converter nullReturningConverter = new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object src, Class<?> target) {
                return null;
            }
        };
        return Stream.of(Arguments.of("any", throwingConverter, RuntimeException.class, "Converter throws RuntimeException"), Arguments.of("123", nullReturningConverter, NullPointerException.class, "Converter returns null → NPE"), Arguments.of(new int[] { 1, 2, 3 }, null, IllegalArgumentException.class, "Unsupported array type without custom converter"), Arguments.of(Boolean.TRUE, null, IllegalArgumentException.class, "Boolean value without custom converter"));
    }

    @Test
    void toAtomicLong_givenNull_returnsZero() {
        AtomicLong result = DateConversions.toAtomicLong(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @Test
    void toAtomicLong_givenLong_returnsSameValue() {
        Long source = 12345L;
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(source.longValue(), result.get());
    }

    @Test
    void toAtomicLong_givenInteger_returnsConvertedLong() {
        Integer source = 42;
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(42L, result.get());
    }

    @Test
    void toAtomicLong_givenStringNumber_returnsParsedLong() {
        String source = "9876543210";
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(9876543210L, result.get());
    }

    @Test
    void toAtomicLong_givenDate_returnsEpochMillis() {
        // 2020‑09‑13T12:26:40Z
        Date source = new Date(1_600_000_000_000L);
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(source.getTime(), result.get());
    }

    @Test
    void toAtomicLong_givenCalendar_returnsEpochMillis() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(1_600_000_000_123L);
        AtomicLong result = DateConversions.toAtomicLong(cal, converter);
        assertEquals(cal.getTimeInMillis(), result.get());
    }

    @Test
    void toAtomicLong_givenBigDecimal_returnsLongValue() {
        java.math.BigDecimal source = new java.math.BigDecimal("12345.678");
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        // Converter truncates toward zero for numeric → long conversion.
        assertEquals(12345L, result.get());
    }

    @Test
    void toAtomicLong_givenUnsupportedType_returnsZero() {
        // An object that the Converter cannot turn into a number yields 0.
        Object source = new Object();
        AtomicLong result = DateConversions.toAtomicLong(source, converter);
        assertEquals(0L, result.get());
    }

    /**
     * Executes a wide range of successful conversion scenarios.
     *
     * @param from        the source value passed to {@code toAtomicLong}
     * @param expected    the expected long value stored in the resulting {@link AtomicLong}
     * @param description a short description used as the test name
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toAtomicLong_validConversions(Object from, long expected, String description) {
        // The method under test creates a new AtomicLong based on the long conversion.
        AtomicLong result = DateConversions.toAtomicLong(from, converter);
        assertNotNull(result, "Resulting AtomicLong must not be null");
        assertEquals(expected, result.get(), () -> "Conversion failed for case: " + description + " (input: " + from + ")");
    }

    /**
     * Verifies that exceptional situations are propagated exactly as specified.
     *
     * @param from             the source value
     * @param customConverter  a converter that may throw or return null; may be {@code null}
     * @param expectedException the type of exception expected
     * @param description      description used for the test name
     */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionProvider")
    void toAtomicLong_exceptions(Object from, Converter customConverter, Class<? extends Throwable> expectedException, String description) {
        Converter usedConverter = (customConverter != null) ? customConverter : converter;
        assertThrows(expectedException, () -> DateConversions.toAtomicLong(from, usedConverter), () -> "Expected exception not thrown for case: " + description);
    }
}
