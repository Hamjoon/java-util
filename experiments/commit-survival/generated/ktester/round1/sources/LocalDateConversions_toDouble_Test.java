package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toDouble_Test {

    /**
     * Converter used for all tests – created once for the whole test run.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        // If the library provides a no‑arg constructor for ConverterOptions, use it.
        // Otherwise, create an empty options object.
        // assumes a public no‑arg ctor
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> provideValidConversions() {
        ???;
        double baseSeconds = baseInstant.toEpochMilli() / 1000.0;
        return Stream.of(Arguments.of(Instant.ofEpochSecond(1672531200L), baseSeconds), Arguments.of(Long.valueOf(1672531200000L), baseSeconds), Arguments.of(new Date(1672531200000L), baseSeconds), Arguments.of(utcCalendar(baseInstant), baseSeconds), Arguments.of(LocalDateTime.of(2023, 1, 1, 0, 0), baseSeconds), Arguments.of(LocalDate.of(2023, 1, 1), baseSeconds), Arguments.of(ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), baseSeconds), Arguments.of(OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), baseSeconds), Arguments.of("2023-01-01T00:00:00Z", baseSeconds), Arguments.of(new Timestamp(1672531200000L), baseSeconds), Arguments.of("2023-01-01T00:00:00+02:00", 1672524000.0), Arguments.of(new Date(Long.MIN_VALUE), Long.MIN_VALUE / 1000.0), Arguments.of(Long.valueOf(Long.MAX_VALUE), Long.MAX_VALUE / 1000.0), Arguments.of(new AtomicLong(1672531200000L), baseSeconds));
    }

    private static Stream<Arguments> provideInvalidConversions() {
        return Stream.of(Arguments.of(Boolean.TRUE, IllegalArgumentException.class), Arguments.of(null, IllegalArgumentException.class), Arguments.of(LocalTime.of(12, 34, 56), IllegalArgumentException.class), Arguments.of(BigDecimal.valueOf(1672531200.5), IllegalArgumentException.class), Arguments.of("2023-01-01 00:00:00", IllegalArgumentException.class), Arguments.of("", IllegalArgumentException.class), Arguments.of(new Object(), IllegalArgumentException.class), Arguments.of(Integer.valueOf(1234567890), IllegalArgumentException.class), Arguments.of(Double.valueOf(1672531200.0), IllegalArgumentException.class), Arguments.of(BigInteger.valueOf(1672531200000L), IllegalArgumentException.class), Arguments.of("2023-02-30T00:00:00Z", IllegalArgumentException.class), Arguments.of("2023-13-01T00:00:00Z", IllegalArgumentException.class), Arguments.of("1672531200000", IllegalArgumentException.class), Arguments.of(new StringBuilder("2023-01-01T00:00:00Z"), IllegalArgumentException.class));
    }

    /**
     * Verify that an {@link Instant} is converted to the correct epoch‑second double.
     */
    @Test
    void toDouble_fromInstant_returnsSecondsSinceEpoch() {
        // Arrange – use a fixed instant truncated to milliseconds for deterministic math.
        Instant instant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        double expected = instant.toEpochMilli() / 1000.0;
        // Act
        double actual = LocalDateConversions.toDouble(instant, converter);
        // Assert
        assertEquals(expected, actual, 1e-9, "Conversion from Instant should yield epoch seconds as double");
    }

    /**
     * Verify that a {@link Date} is converted to the same epoch‑second double as its underlying instant.
     */
    @Test
    void toDouble_fromDate_returnsSecondsSinceEpoch() {
        // Arrange – reuse the same instant as the previous test for consistency.
        Instant instant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Date date = new Date(instant.toEpochMilli());
        double expected = instant.toEpochMilli() / 1000.0;
        // Act
        double actual = LocalDateConversions.toDouble(date, converter);
        // Assert
        assertEquals(expected, actual, 1e-9, "Conversion from java.util.Date should yield epoch seconds as double");
    }

    /**
     * Verify that a {@link LocalDateTime} (interpreted as UTC) is converted correctly.
     * The library treats a LocalDateTime as UTC when converting to an {@link Instant}.
     */
    @Test
    void toDouble_fromLocalDateTime_returnsSecondsSinceEpoch() {
        // Arrange – create a LocalDateTime that represents the same instant as above, using UTC.
        Instant instant = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        double expected = instant.toEpochMilli() / 1000.0;
        // Act
        double actual = LocalDateConversions.toDouble(ldt, converter);
        // Assert
        assertEquals(expected, actual, 1e-9, "Conversion from LocalDateTime (treated as UTC) should yield epoch seconds as double");
    }

    /**
     * Verify that {@code null} input yields {@code 0.0}.
     * The conversion chain ultimately falls back to {@code Instant.EPOCH} for null values.
     */
    @Test
    void toDouble_fromNull_returnsZero() {
        // Act
        double actual = LocalDateConversions.toDouble(null, converter);
        // Assert
        assertEquals(0.0, actual, 0.0, "Conversion from null should return 0.0 (epoch start)");
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("provideValidConversions")
    void toDouble_validInputs(Object from, double expected) {
        double actual = LocalDateConversions.toDouble(from, converter);
        double delta = Math.abs(expected) > 1e12 ? Math.abs(expected) * 1e-12 : 1e-9;
        assertEquals(expected, actual, delta, () -> "Conversion of " + from.getClass().getSimpleName() + " should match expected epoch seconds");
    }

    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("provideInvalidConversions")
    void toDouble_invalidInputs(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateConversions.toDouble(from, converter), () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should throw " + expectedException.getSimpleName());
    }

    @Test
    void toDouble_nullConverter_throwsNullPointerException() {
        Instant instant = Instant.ofEpochSecond(1672531200L);
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toDouble(instant, null), "Passing a null Converter should cause NullPointerException");
    }
}
