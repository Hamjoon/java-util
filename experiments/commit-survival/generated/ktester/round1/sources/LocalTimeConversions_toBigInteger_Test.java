package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toBigInteger_Test {

    @Mock
    Converter // Converter is not used by the method but required for the signature
    mockConverter;

    /**
     * Simple dummy implementation of {@link Converter}.  The conversion
     * logic does not depend on the converter, so the class can stay empty.
     */
    static class DummyConverter implements Converter {
        // No methods needed for the current tests
    }

    /**
     * Supplies all “happy‑path” test data extracted from the JSON specification.
     * Each {@link Arguments} instance contains:
     * <ul>
     *   <li>a {@link LocalTime} source value</li>
     *   <li>a {@link Converter} (null or a dummy instance)</li>
     *   <li>the expected {@link BigInteger} result</li>
     * </ul>
     */
    static Stream<Arguments> provideToBigIntegerCases() {
        return Stream.of(// typical time
        Arguments.of(LocalTime.of(12, 34, 56, 123_456_789), null, new BigInteger("45296123456789")), // midnight
        Arguments.of(LocalTime.MIDNIGHT, null, BigInteger.ZERO), // maximum time of the day
        Arguments.of(LocalTime.MAX, null, new BigInteger("86399999999999")), // one nanosecond after midnight
        Arguments.of(LocalTime.of(0, 0, 0, 1), null, BigInteger.ONE), // one nanosecond before midnight
        Arguments.of(LocalTime.of(23, 59, 59, 999_999_998), null, new BigInteger("86399999999998")), // hour‑only (5 hours)
        Arguments.of(LocalTime.of(5, 0, 0, 0), null, new BigInteger("18000000000000")), // seconds‑only (30 seconds)
        Arguments.of(LocalTime.of(0, 0, 30, 0), null, new BigInteger("30000000000")), // non‑null converter (ignored by the method)
        Arguments.of(LocalTime.of(1, 2, 3, 4), new DummyConverter(), new BigInteger("3723000000004")), // factory method ofSecondOfDay
        Arguments.of(LocalTime.ofSecondOfDay(12_345), null, new BigInteger("12345000000000")));
    }

    @Test
    void toBigInteger_ZeroTime_ReturnsZero() {
        // given
        LocalTime lt = LocalTime.of(0, 0, 0, 0);
        // when
        BigInteger result = LocalTimeConversions.toBigInteger(lt, mockConverter);
        // then
        assertEquals(BigInteger.ZERO, result);
    }

    @Test
    void toBigInteger_MiddayTime_ReturnsCorrectNanoseconds() {
        // given
        LocalTime lt = LocalTime.of(12, 34, 56, 789_123_456);
        // using the same API as the implementation
        long expectedNanos = lt.toNanoOfDay();
        // when
        BigInteger result = LocalTimeConversions.toBigInteger(lt, mockConverter);
        // then
        assertEquals(BigInteger.valueOf(expectedNanos), result);
    }

    @Test
    void toBigInteger_MaximumTime_ReturnsLastNanosecondOfDay() {
        // given
        LocalTime lt = LocalTime.of(23, 59, 59, 999_999_999);
        long expectedNanos = lt.toNanoOfDay();
        // when
        BigInteger result = LocalTimeConversions.toBigInteger(lt, mockConverter);
        // then
        assertEquals(BigInteger.valueOf(expectedNanos), result);
        // sanity check: the value should be one less than a full day's nanoseconds
        assertEquals(86_400_000_000_000L - 1, expectedNanos);
    }

    @Test
    void toBigInteger_NonLocalTime_ThrowsClassCastException() {
        // given
        Object notATime = "2023-01-01T12:00:00";
        // when / then
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toBigInteger(notATime, mockConverter));
    }

    @Test
    void toBigInteger_NullInput_ThrowsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toBigInteger(nullObj, mockConverter));
    }

    /**
     * Parameterized test that runs through all normal conversion scenarios.
     *
     * @param from       the {@link LocalTime} to convert
     * @param converter  the {@link Converter} supplied (may be {@code null})
     * @param expected   the expected {@link BigInteger} result
     */
    @ParameterizedTest(name = "{index} => from={0}, expected={2}")
    @MethodSource("provideToBigIntegerCases")
    void toBigInteger_Parameterized(LocalTime from, Converter converter, BigInteger expected) {
        // Act
        BigInteger result = LocalTimeConversions.toBigInteger(from, converter);
        // Assert
        assertEquals(expected, result);
    }

    /**
     * Verifies that passing an object of the wrong type results in a {@link ClassCastException}.
     */
    @Test
    void toBigInteger_WrongType_ThrowsClassCastException() {
        Object notATime = "12:00";
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toBigInteger(notATime, mockConverter));
    }
}
