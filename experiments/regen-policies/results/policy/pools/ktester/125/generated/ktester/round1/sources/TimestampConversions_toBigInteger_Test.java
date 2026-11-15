package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class TimestampConversions_toBigInteger_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a no‑arg constructor in the library; use default options.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release resources if needed (none for this simple test).
        converter = null;
    }

    /**
     * Supplies the arguments for {@link #toBigInteger_validConversions}.
     */
    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// epoch (1970‑01‑01T00:00:00Z) → 0
        Arguments.of("Epoch timestamp", new Timestamp(0L), InstantConversions.toBigInteger(new Timestamp(0L).toInstant(), converter)), // 1,500,000 ms after epoch → 1,500 seconds (BigInteger value 1500)
        Arguments.of("Timestamp 1_500_000 ms after epoch", new Timestamp(1_500_000L), InstantConversions.toBigInteger(new Timestamp(1_500_000L).toInstant(), converter)), // Timestamp with nanosecond precision
        Arguments.of("Timestamp with nanoseconds", Timestamp.valueOf("2023-03-15 12:34:56.123456789"), InstantConversions.toBigInteger(Timestamp.valueOf("2023-03-15 12:34:56.123456789").toInstant(), converter)));
    }

    @Test
    void toBigInteger_givenTimestamp_returnsExpectedBigInteger() {
        // GIVEN
        // 2021‑10‑01T00:00:00Z
        long epochMilli = 1_638_307_280_000L;
        Timestamp timestamp = new Timestamp(epochMilli);
        Instant instant = timestamp.toInstant();
        // Expected value computed via the same conversion logic used internally.
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        // WHEN
        BigInteger actual = TimestampConversions.toBigInteger(timestamp, converter);
        // THEN
        assertEquals(expected, actual, "BigInteger conversion of Timestamp should match Instant conversion");
    }

    @Test
    void toBigInteger_givenTimestampWithNanos_returnsExpectedBigInteger() {
        // GIVEN a timestamp that includes nanosecond precision
        Timestamp timestamp = Timestamp.valueOf("2022-03-15 12:34:56.123456789");
        Instant instant = timestamp.toInstant();
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        // WHEN
        BigInteger actual = TimestampConversions.toBigInteger(timestamp, converter);
        // THEN
        assertEquals(expected, actual);
    }

    /**
     * Parameterized test covering the "Valid conversions" group from the JSON spec.
     *
     * <p>Each argument supplies a {@link Timestamp} instance and the expected {@link BigInteger}
     * result, which is calculated via the same delegate method used inside the
     * implementation ( {@link InstantConversions#toBigInteger(Instant, Converter)} ).</p>
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionsProvider")
    void toBigInteger_validConversions(String description, Timestamp timestamp, BigInteger expected) {
        // WHEN
        BigInteger actual = TimestampConversions.toBigInteger(timestamp, converter);
        // THEN
        assertEquals(expected, actual, () -> "Conversion of " + timestamp + " should match Instant conversion");
    }

    /**
     * Tests that a {@code null} source object results in a {@link NullPointerException}.
     *
     * <p>The method first casts the argument to {@link Timestamp}; casting {@code null}
     * yields {@code null}, but the subsequent call to {@code timestamp.toInstant()}
     * triggers the NPE.</p>
     */
    @Test
    void toBigInteger_nullFrom_throwsNullPointerException() {
        // GIVEN
        Timestamp from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toBigInteger(from, converter), "Passing a null 'from' should raise NullPointerException");
    }

    /**
     * Tests that providing an object that is not a {@link Timestamp}
     * causes a {@link ClassCastException}.
     */
    @Test
    void toBigInteger_nonTimestampFrom_throwsClassCastException() {
        // GIVEN
        // not a java.sql.Timestamp
        Object from = new java.util.Date();
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimestampConversions.toBigInteger(from, converter), "Passing a non‑Timestamp should raise ClassCastException");
    }

    /**
     * Tests that a {@code null} {@link Converter} argument propagates a
     * {@link NullPointerException} from the delegated call.
     */
    @Test
    void toBigInteger_nullConverter_throwsNullPointerException() {
        // GIVEN
        Timestamp timestamp = new Timestamp(0L);
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toBigInteger(timestamp, nullConverter), "Passing a null Converter should raise NullPointerException");
    }

    /**
     * Verifies that if the supplied {@link Converter} throws an unchecked exception
     * during the delegated conversion, the exception is propagated unchanged.
     */
    @Test
    void toBigInteger_converterThrowsIllegalArgumentException_propagates() {
        // GIVEN
        Timestamp timestamp = new Timestamp(0L);
        Converter badConverter = Mockito.mock(Converter.class);
        Mockito.when(badConverter.toBigInteger(any(Instant.class))).thenThrow(new IllegalArgumentException("converter failure"));
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toBigInteger(timestamp, badConverter), "Converter's IllegalArgumentException must be propagated");
        assertEquals("converter failure", ex.getMessage());
    }

    /**
     * Verifies that a {@link NullPointerException} thrown by the {@link Converter}
     * is also propagated without being wrapped.
     */
    @Test
    void toBigInteger_converterThrowsNullPointerException_propagates() {
        // GIVEN
        Timestamp timestamp = new Timestamp(12_345L);
        Converter badConverter = Mockito.mock(Converter.class);
        Mockito.when(badConverter.toBigInteger(any(Instant.class))).thenThrow(new NullPointerException("converter NPE"));
        // WHEN / THEN
        NullPointerException ex = assertThrows(NullPointerException.class, () -> TimestampConversions.toBigInteger(timestamp, badConverter), "Converter's NullPointerException must be propagated");
        assertEquals("converter NPE", ex.getMessage());
    }
}
