package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toBigDecimal_Test {

    /**
     * A mock {@link Converter}. It is not used by the method under test but must be supplied.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Common {@link Instant} instance used by several tests.
     */
    private static final Instant SAMPLE_INSTANT = Instant.ofEpochSecond(1_600_000_000L, 123_456_789);

    /**
     * Expected value that the mocked {@code secondsAndNanosToDouble} will return.
     */
    private static final BigDecimal EXPECTED_BIG_DECIMAL = new BigDecimal("1600000000.123456789");

    /**
     * Parameter provider for valid Instant conversions.
     * Each argument consists of:
     *   - the Instant to convert,
     *   - the (optional) Converter instance (may be null),
     *   - the expected BigDecimal result.
     */
    private static Stream<Arguments> validInstantProvider() {
        return Stream.of(// epoch zero
        Arguments.of(Instant.ofEpochSecond(0L, 0), null, BigDecimal.valueOf(0.0)), // positive with nanos
        Arguments.of(Instant.ofEpochSecond(1_625_097_600L, 123_456_789), null, new BigDecimal("1625097600.123456789")), // negative seconds with positive nanos -> -0.5
        Arguments.of(Instant.ofEpochSecond(-1L, 500_000_000), null, BigDecimal.valueOf(-0.5)), // max long seconds with max nanos
        Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE, 999_999_999), null, new BigDecimal(Long.MAX_VALUE).add(new BigDecimal("0.999999999"))), // min long seconds, zero nanos
        Arguments.of(Instant.ofEpochSecond(Long.MIN_VALUE, 0), null, new BigDecimal(Long.MIN_VALUE)), // additional cases
        Arguments.of(Instant.ofEpochSecond(1000L, 0), null, BigDecimal.valueOf(1000.0)), Arguments.of(Instant.ofEpochSecond(0L, 999_999_999), null, new BigDecimal("0.999999999")), Arguments.of(Instant.ofEpochSecond(-1L, 0), null, BigDecimal.valueOf(-1.0)), Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE, 0), null, new BigDecimal(Long.MAX_VALUE)), // non‑null converter (unused)
        Arguments.of(Instant.ofEpochSecond(1_625_097_600L, 123_456_789), // anonymous instance
        new Converter() {
        }, new BigDecimal("1625097600.123456789")));
    }

    /**
     * Parameter provider for invalid inputs that should result in exceptions.
     */
    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// null source
        Arguments.of(null, null, NullPointerException.class), // non‑Instant String
        Arguments.of("not an instant", null, ClassCastException.class), // java.util.Date (not Instant)
        Arguments.of(new java.util.Date(), null, ClassCastException.class), // ZonedDateTime (not Instant)
        Arguments.of(java.time.ZonedDateTime.now(), null, ClassCastException.class));
    }

    /**
     * Verifies that a valid {@link Instant} is correctly forwarded to
     * {@code BigDecimalConversions.secondsAndNanosToDouble} and that the
     * resulting {@link BigDecimal} is returned unchanged.
     */
    @Test
    void toBigDecimal_WithValidInstant_DelegatesToBigDecimalConversions() {
        // Arrange – mock the static utility method.
        try (MockedStatic<BigDecimalConversions> mocked = mockStatic(BigDecimalConversions.class)) {
            mocked.when(() -> BigDecimalConversions.secondsAndNanosToDouble(SAMPLE_INSTANT.getEpochSecond(), SAMPLE_INSTANT.getNano())).thenReturn(EXPECTED_BIG_DECIMAL);
            // Act
            BigDecimal result = InstantConversions.toBigDecimal(SAMPLE_INSTANT, mockConverter);
            // Assert
            assertEquals(EXPECTED_BIG_DECIMAL, result, "The method should return the value supplied by BigDecimalConversions");
            mocked.verify(() -> BigDecimalConversions.secondsAndNanosToDouble(SAMPLE_INSTANT.getEpochSecond(), SAMPLE_INSTANT.getNano()), times(1));
        }
    }

    /**
     * Ensures that a {@code ClassCastException} is thrown when the supplied
     * {@code from} argument is not an {@link Instant}.
     */
    @Test
    void toBigDecimal_WithNonInstant_ThrowsClassCastException() {
        // Arrange
        Object notAnInstant = "2021-01-01T00:00:00Z";
        // Act & Assert
        assertThrows(ClassCastException.class, () -> InstantConversions.toBigDecimal(notAnInstant, mockConverter), "Casting a non‑Instant object should result in ClassCastException");
    }

    /**
     * Checks that passing {@code null} as the source object results in a
     * {@code NullPointerException} (the method performs an unchecked cast).
     */
    @Test
    void toBigDecimal_WithNullSource_ThrowsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> InstantConversions.toBigDecimal(null, mockConverter), "Calling the method with a null source should throw NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validInstantProvider")
    void toBigDecimal_ValidInstants_ReturnsExpectedBigDecimal(Instant instant, Converter converter, BigDecimal expected) {
        // Mock the static conversion method to return the expected value.
        try (MockedStatic<BigDecimalConversions> mocked = mockStatic(BigDecimalConversions.class)) {
            mocked.when(() -> BigDecimalConversions.secondsAndNanosToDouble(instant.getEpochSecond(), instant.getNano())).thenReturn(expected);
            // Use the supplied converter (may be null) – the method ignores it.
            BigDecimal result = InstantConversions.toBigDecimal(instant, converter);
            assertEquals(expected, result, () -> "Expected conversion of " + instant + " to be " + expected);
            mocked.verify(() -> BigDecimalConversions.secondsAndNanosToDouble(instant.getEpochSecond(), instant.getNano()), times(1));
        }
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("invalidInputProvider")
    void toBigDecimal_InvalidInputs_ThrowExpectedException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> InstantConversions.toBigDecimal(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
