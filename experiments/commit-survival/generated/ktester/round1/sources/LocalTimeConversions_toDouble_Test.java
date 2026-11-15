package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toDouble_Test {

    /**
     * A mock Converter – the method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Provides all normal conversion cases (expected to return a double).
     * The {@code Converter} argument may be {@code null}, a Mockito mock,
     * or a mock that throws if used – the implementation never touches it.
     */
    private static Stream<Arguments> provideValidConversionCases() {
        // a mock that would throw on any interaction (should never be called)
        Converter throwingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new UnsupportedOperationException("Converter should not be used");
        });
        return Stream.of(// group: toDouble
        Arguments.of(LocalTime.MIDNIGHT, null, 0.0), Arguments.of(LocalTime.of(12, 0, 0), null, 43200.0), Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), null, 86_399.999_999_999), Arguments.of(LocalTime.of(0, 0, 0, 1), null, 1.0e-9), Arguments.of(LocalTime.of(6, 30, 15, 250_000_000), throwingConverter, 23_415.25), // group: toDoubleAdditional
        Arguments.of(LocalTime.of(1, 2, 3, 123_456_789), null, 3_723.123_456_789), Arguments.of(LocalTime.of(0, 0, 0, 500_000_000), null, 0.5), Arguments.of(LocalTime.of(23, 0, 0, 0), null, 82_800.0), Arguments.of(LocalTime.of(0, 30, 0, 0), null, 1_800.0), Arguments.of(LocalTime.of(12, 34, 56, 789_000_000), null, 45_296.789), Arguments.of(LocalTime.of(12, 34, 56, 789_000_000), throwingConverter, 45_296.789), Arguments.of(LocalTime.of(23, 59, 58, 0), null, 86_398.0), Arguments.of(LocalTime.of(0, 0, 0, 2), null, 2.0e-9));
    }

    /**
     * Provides cases that are expected to throw an exception.
     */
    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Null source object
        Arguments.of(null, null, NullPointerException.class), // Wrong type (String instead of LocalTime)
        Arguments.of("12:34:56", null, ClassCastException.class));
    }

    @Test
    void toDouble_zeroTime_returnsZero() {
        // given
        // midnight
        LocalTime lt = LocalTime.of(0, 0, 0, 0);
        // when
        double result = LocalTimeConversions.toDouble(lt, mockConverter);
        // then
        assertEquals(0.0, result, 1e-12, "Midnight should convert to 0.0 seconds");
    }

    @Test
    void toDouble_oneHour_returns3600() {
        // given
        // 01:00:00.000
        LocalTime lt = LocalTime.of(1, 0, 0, 0);
        // when
        double result = LocalTimeConversions.toDouble(lt, mockConverter);
        // then
        assertEquals(3600.0, result, 1e-12, "One hour should be 3600.0 seconds");
    }

    @Test
    void toDouble_complexTime_returnsCorrectFraction() {
        // given
        // 12:34:56.123456789 -> 12*3600 + 34*60 + 56 = 45296 seconds + 0.123456789 seconds
        LocalTime lt = LocalTime.of(12, 34, 56, 123_456_789);
        // when
        double result = LocalTimeConversions.toDouble(lt, mockConverter);
        // then
        double expected = 45296.123456789;
        assertEquals(expected, result, 1e-12, "Complex time should be converted accurately");
    }

    @Test
    void toDouble_nullInput_throwsNullPointerException() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toDouble(nullObj, mockConverter), "Passing null should result in NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] {2} seconds from {0}")
    @MethodSource("provideValidConversionCases")
    void toDouble_validConversions(LocalTime from, Converter converter, double expected) {
        // when
        double result = LocalTimeConversions.toDouble(from, converter);
        // then
        assertEquals(expected, result, 1e-12, () -> "Conversion of " + from + " should be " + expected + " seconds");
    }

    @ParameterizedTest(name = "[{index}] expecting {2} for input {0}")
    @MethodSource("provideExceptionCases")
    void toDouble_exceptionalCases(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalTimeConversions.toDouble(from, converter), () -> "Expected " + expectedException.getSimpleName() + " when converting " + from);
    }
}
