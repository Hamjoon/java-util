package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class InstantConversions_toBigInteger_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter is required by the method signature, but the implementation does not use it.
        // We create a default instance with default options.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> standardConversionsProvider() {
        // Helper to compute expected value
        java.util.function.BiFunction<Long, Integer, BigInteger> calc = (sec, nano) -> BigInteger.valueOf(sec).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(nano));
        // Normal (real) Instants
        Arguments epoch = Arguments.of("Instant at the Unix epoch (0 seconds, 0 nanos) → 0", Instant.ofEpochSecond(0, 0), BigInteger.ZERO);
        Arguments oneSecond = Arguments.of("1 second, 0 nanos → 1 000 000 000", Instant.ofEpochSecond(1, 0), calc.apply(1L, 0));
        Arguments typicalPositive = Arguments.of("Typical positive instant (12345 s, 678 901 234 ns)", Instant.ofEpochSecond(12_345L, 678_901_234), calc.apply(12_345L, 678_901_234));
        Arguments negativeSecondZeroNanos = Arguments.of("Negative second with zero nanos → -1 000 000 000", Instant.ofEpochSecond(-1, 0), calc.apply(-1L, 0));
        Arguments negativeSecondMaxNanos = Arguments.of("Negative second with max positive nanos → -1 000 000 000 + 999 999 999", Instant.ofEpochSecond(-1, 999_999_999), calc.apply(-1L, 999_999_999));
        // Extreme values – cannot be created with the public factory, so we use a mock Instant.
        Instant maxInstantMock = mock(Instant.class);
        when(maxInstantMock.getEpochSecond()).thenReturn(Long.MAX_VALUE);
        when(maxInstantMock.getNano()).thenReturn(999_999_999);
        Arguments maxInstant = Arguments.of("Upper‑bound Instant (Long.MAX_VALUE s, 999 999 999 ns)", maxInstantMock, calc.apply(Long.MAX_VALUE, 999_999_999));
        Instant minInstantMock = mock(Instant.class);
        when(minInstantMock.getEpochSecond()).thenReturn(Long.MIN_VALUE);
        when(minInstantMock.getNano()).thenReturn(0);
        Arguments minInstant = Arguments.of("Lower‑bound Instant (Long.MIN_VALUE s, 0 ns)", minInstantMock, calc.apply(Long.MIN_VALUE, 0));
        return Stream.of(epoch, oneSecond, typicalPositive, negativeSecondZeroNanos, negativeSecondMaxNanos, maxInstant, minInstant);
    }

    private static Stream<Arguments> exceptionHandlingProvider() {
        return Stream.of(Arguments.of("Null source should throw NullPointerException", null, NullPointerException.class), Arguments.of("String source should throw ClassCastException", "2021-01-01T00:00:00Z", ClassCastException.class), Arguments.of("Date source should throw ClassCastException", new java.util.Date(), ClassCastException.class));
    }

    private static Stream<Arguments> normalizationProvider() {
        java.util.function.BiFunction<Long, Integer, BigInteger> calc = (sec, nano) -> BigInteger.valueOf(sec).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(nano));
        return Stream.of(Arguments.of("Negative seconds with positive nanos (‑2, 500 000 000) → -1 500 000 000", Instant.ofEpochSecond(-2, 500_000_000), // -1_500_000_000
        calc.apply(-1L, 500_000_000)), Arguments.of("Zero seconds with max nanos (0, 999 999 999) → 999 999 999", Instant.ofEpochSecond(0, 999_999_999), calc.apply(0L, 999_999_999)), Arguments.of("Positive seconds with negative nanos (1, -500 000 000) → 500 000 000", Instant.ofEpochSecond(1, -500_000_000), calc.apply(0L, 500_000_000)), Arguments.of("Upper‑bound seconds with zero nanos (Long.MAX_VALUE, 0)", // Use a mock to avoid DateTimeException
        mockInstant(Long.MAX_VALUE, 0), calc.apply(Long.MAX_VALUE, 0)), Arguments.of("Negative nanosecond adjustment (0, -1) → -1", Instant.ofEpochSecond(0, -1), calc.apply(0L, -1L)));
    }

    // Helper to create a mocked Instant with given second/nano values
    private static Instant mockInstant(long seconds, int nanos) {
        Instant mock = mock(Instant.class);
        when(mock.getEpochSecond()).thenReturn(seconds);
        when(mock.getNano()).thenReturn(nanos);
        return mock;
    }

    private static Stream<Arguments> instantConstantsProvider() {
        java.util.function.BiFunction<Long, Integer, BigInteger> calc = (sec, nano) -> BigInteger.valueOf(sec).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(nano));
        return Stream.of(Arguments.of("Instant.EPOCH → 0", Instant.EPOCH, BigInteger.ZERO), Arguments.of("Instant.MIN → Long.MIN_VALUE * 1_000_000_000", Instant.MIN, calc.apply(Instant.MIN.getEpochSecond(), Instant.MIN.getNano())), Arguments.of("Instant.MAX → Long.MAX_VALUE * 1_000_000_000 + 999_999_999", Instant.MAX, calc.apply(Instant.MAX.getEpochSecond(), Instant.MAX.getNano())));
    }

    @Test
    @DisplayName("Convert a positive Instant with seconds and nanos to BigInteger")
    void testPositiveInstant() {
        // given
        long seconds = 12_345L;
        int nanos = 6_789;
        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        BigInteger expected = BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(nanos));
        // when
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        // then
        assertEquals(expected, result, "BigInteger conversion should match calculated nanoseconds");
    }

    @Test
    @DisplayName("Convert the epoch Instant (0 seconds, 0 nanos) to BigInteger.ZERO")
    void testEpochInstant() {
        // given
        Instant instant = Instant.EPOCH;
        BigInteger expected = BigInteger.ZERO;
        // when
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        // then
        assertEquals(expected, result, "Epoch instant should convert to BigInteger.ZERO");
    }

    @Test
    @DisplayName("Convert a negative Instant (before epoch) with nanos to BigInteger")
    void testNegativeInstant() {
        // given
        long seconds = -10L;
        // positive nanos are allowed even with negative seconds
        int nanos = 500;
        Instant instant = Instant.ofEpochSecond(seconds, nanos);
        // Expected = seconds * 1_000_000_000 + nanos
        BigInteger expected = BigInteger.valueOf(seconds).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(nanos));
        // when
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        // then
        assertEquals(expected, result, "Negative instant conversion should correctly handle sign and nanos");
    }

    // -------------------- Standard conversions (including extreme long values) --------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("standardConversionsProvider")
    void testStandardConversions(String description, Instant instant, BigInteger expected) {
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, result, description);
    }

    // -------------------- Exception handling --------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionHandlingProvider")
    void testExceptionHandling(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> InstantConversions.toBigInteger(from, converter), description);
    }

    // -------------------- Normalization handling --------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalizationProvider")
    void testNormalization(String description, Instant instant, BigInteger expected) {
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, result, description);
    }

    // -------------------- Converter parameter effect --------------------
    @Test
    @DisplayName("Non‑null Converter instance is ignored")
    void testConverterParameterIgnored() {
        // Arrange
        Instant instant = Instant.ofEpochSecond(2, 250_000_000);
        BigInteger expected = BigInteger.valueOf(2_250_000_000L);
        // any mock is fine
        Converter mockConverter = mock(Converter.class);
        // Act
        BigInteger result = InstantConversions.toBigInteger(instant, mockConverter);
        // Assert
        assertEquals(expected, result, "Result must be independent of the Converter instance");
    }

    // -------------------- Instant constant edge cases --------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("instantConstantsProvider")
    void testInstantConstants(String description, Instant instant, BigInteger expected) {
        BigInteger result = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, result, description);
    }
}
