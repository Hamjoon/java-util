package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.LocalDate;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toBigInteger_Test {

    /**
     * The Converter is a dependency of the static method, but the implementation does not use it.
     *  We provide a mock to satisfy the method signature without pulling in any real conversion logic.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// midnight – 0 nanoseconds
        Arguments.of(LocalTime.of(0, 0), BigInteger.valueOf(0L)), // typical time with hour, minute, second, nanosecond
        Arguments.of(LocalTime.of(12, 34, 56, 123_456_789), BigInteger.valueOf(4_529_696_123_456_789L)), // maximum LocalTime (23:59:59.999999999)
        Arguments.of(LocalTime.MAX, BigInteger.valueOf(86_399_999_999_999L)));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(// a plain String
        Arguments.of("not a time"), // a LocalDate – a different temporal type
        Arguments.of(LocalDate.now()));
    }

    @Test
    void toBigInteger_givenLocalTime_returnsNanoOfDayAsBigInteger() {
        // GIVEN
        // 13:45:30.123456789
        LocalTime time = LocalTime.of(13, 45, 30, 123_456_789);
        long expectedNanos = time.toNanoOfDay();
        // WHEN
        BigInteger result = LocalTimeConversions.toBigInteger(time, mockConverter);
        // THEN
        assertNotNull(result, "Result should never be null for a valid LocalTime");
        assertEquals(BigInteger.valueOf(expectedNanos), result, "BigInteger value should represent the nanoseconds of the day");
    }

    @Test
    void toBigInteger_givenNull_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toBigInteger(nullObj, mockConverter), "Passing null as the source should result in NPE because of the cast to LocalTime");
    }

    @ParameterizedTest(name = "[{index}] toBigInteger({0}) == {1}")
    @MethodSource("validConversionProvider")
    void toBigInteger_givenValidLocalTime_returnsExpectedBigInteger(LocalTime from, BigInteger expected) {
        // WHEN – we deliberately pass {@code null} for the converter to prove it is ignored
        BigInteger result = LocalTimeConversions.toBigInteger(from, null);
        // THEN
        assertNotNull(result, "Result must never be null for a valid LocalTime");
        assertEquals(expected, result, () -> "Expected nanoseconds of day for " + from + " to be " + expected);
    }

    @Test
    void toBigInteger_givenValidLocalTime_andNonNullConverter_returnsExpectedBigInteger() {
        // GIVEN
        LocalTime time = LocalTime.of(23, 59, 59, 999_999_999);
        BigInteger expected = BigInteger.valueOf(86_399_999_999_999L);
        // using a real instance – method should ignore it
        Converter realConverter = new Converter();
        // WHEN
        BigInteger result = LocalTimeConversions.toBigInteger(time, realConverter);
        // THEN
        assertEquals(expected, result, "Method must ignore the Converter argument and return the correct nanoseconds");
    }

    @Test
    void toBigInteger_givenNullFrom_throwsNullPointerException() {
        // GIVEN
        Object nullFrom = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toBigInteger(nullFrom, mockConverter), "Passing null as the source should result in NPE because of the cast to LocalTime");
    }

    @ParameterizedTest(name = "[{index}] toBigInteger({0}) throws ClassCastException")
    @MethodSource("invalidTypeProvider")
    void toBigInteger_givenInvalidType_throwsClassCastException(Object invalidFrom) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toBigInteger(invalidFrom, mockConverter), "Passing a non‑LocalTime object should trigger a ClassCastException");
    }
}
