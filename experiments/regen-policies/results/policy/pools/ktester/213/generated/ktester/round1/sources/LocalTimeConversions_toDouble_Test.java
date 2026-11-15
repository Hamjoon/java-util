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
import java.time.LocalDate;
import java.util.stream.Stream;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toDouble_Test {

    /**
     * Provides a stream of valid {@code LocalTime} inputs together with the
     * expected double representation (seconds since midnight).  The {@code Converter}
     * argument is exercised both as a real instance and as {@code null}
     * to prove that the method does not depend on it.
     */
    private static Stream<Arguments> validConversionProvider() {
        Converter realConverter = new Converter(new ConverterOptions());
        return Stream.of(// Midnight – 0 seconds
        Arguments.of(LocalTime.of(0, 0, 0, 0), null, 0.0), // Noon – 12 hours = 43200 seconds
        Arguments.of(LocalTime.of(12, 0, 0, 0), realConverter, 43200.0), // One nanosecond before midnight
        Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), null, 86_399.999_999_999d), // Random time 06:30:15.250 (250 ms = 250 000 000 ns)
        Arguments.of(LocalTime.of(6, 30, 15, 250_000_000), null, 6 * 3600 + 30 * 60 + 15 + 0.250));
    }

    private static Stream<Arguments> invalidFromProvider() {
        return Stream.of(// Null source – NPE expected
        Arguments.of(null, null, NullPointerException.class), // Wrong type – String
        Arguments.of("some string", null, ClassCastException.class), // Wrong type – LocalDate (not a LocalTime)
        Arguments.of(LocalDate.now(), null, ClassCastException.class));
    }

    @Test
    void toDouble_midnight_returnsZero() {
        // given
        LocalTime midnight = LocalTime.MIDNIGHT;
        Converter dummyConverter = new Converter(new ConverterOptions());
        // when
        double result = LocalTimeConversions.toDouble(midnight, dummyConverter);
        // then
        assertEquals(0.0, result, 1e-12, "Midnight should be converted to 0.0 seconds");
    }

    @Test
    void toDouble_specificTime_returnsCorrectSeconds() {
        // given
        // 01:02:03.123456789  -> 1 hour, 2 minutes, 3 seconds + 123,456,789 nanoseconds
        LocalTime time = LocalTime.of(1, 2, 3, 123_456_789);
        Converter dummyConverter = new Converter(new ConverterOptions());
        // expected seconds = 1*3600 + 2*60 + 3 + 123_456_789 / 1_000_000_000
        double expected = 1 * 3600 + 2 * 60 + 3 + 123_456_789 / 1_000_000_000.0;
        // when
        double result = LocalTimeConversions.toDouble(time, dummyConverter);
        // then
        assertEquals(expected, result, 1e-12, "The conversion should yield the exact number of seconds as a double");
    }

    @Test
    void toDouble_usesConverterParameterButDoesNotDependOnIt(@Mock Converter mockConverter) {
        // given
        // exactly 12 hours = 43200 seconds
        LocalTime time = LocalTime.of(12, 0, 0);
        // 43200.0
        double expected = 12 * 3600;
        // when
        double result = LocalTimeConversions.toDouble(time, mockConverter);
        // then
        assertEquals(expected, result, 1e-12, "Conversion result must be independent of the Converter implementation");
        // Verify that no interaction with the mock occurred (method does not call it)
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toDouble_nullSource_throwsNullPointerException() {
        // given
        Converter dummyConverter = new Converter(new ConverterOptions());
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toDouble(null, dummyConverter), "Passing a null source should result in NPE when toNanoOfDay() is invoked");
    }

    @ParameterizedTest(name = "[{index}] from={0} converter={1}")
    @MethodSource("validConversionProvider")
    void toDouble_validConversions(LocalTime from, Converter converter, double expected) {
        // when
        double result = LocalTimeConversions.toDouble(from, converter);
        // then
        assertEquals(expected, result, 1e-12, () -> "Conversion of " + from + " should equal " + expected + " seconds");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("invalidFromProvider")
    void toDouble_invalidFromParameter(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> LocalTimeConversions.toDouble(from, converter), () -> "Expected " + expectedException.getSimpleName() + " when converting from " + (from == null ? "null" : from.getClass()));
    }
}
