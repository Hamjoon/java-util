package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toDouble_Test {

    // The Converter is not used by DateConversions.toDouble, but the method requires it.
    @Mock
    private Converter mockConverter;

    /**
     * Simple {@code Converter} implementation used only to provide a non‑null
     * argument.  The real {@code Converter} class is not required for the
     * {@code toDouble} implementation.
     */
    private static class DummyConverter implements Converter {
        // No behaviour needed; the method under test ignores this argument.
    }

    /**
     * Custom {@code Date} subclass that simply forwards the constructor
     * argument to {@link java.util.Date}.  This verifies that {@code toDouble}
     * works with any subclass of {@code Date}.
     */
    private static class MyDate extends Date {

        MyDate(long time) {
            super(time);
        }
    }

    /**
     * {@code Date} subclass whose {@code getTime()} method deliberately throws
     * an {@link IllegalStateException}.  Used to test exception propagation.
     */
    private static class ThrowingDate extends Date {

        @Override
        public long getTime() {
            throw new IllegalStateException("Intentional exception from getTime()");
        }
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// Regular java.util.Date (2021‑01‑01 00:00:00 UTC)
        Arguments.of("Convert a regular java.util.Date representing 2021‑01‑01 00:00:00 UTC", new Date(1609459200000L), null, 1609459200.0), // java.sql.Timestamp (subclass of java.util.Date)
        Arguments.of("Convert a java.sql.Timestamp with the same epoch; should behave identically", new Timestamp(1609459200000L), null, 1609459200.0), // Epoch start
        Arguments.of("Convert the epoch start (0 milliseconds) to seconds; verifies handling of zero", new Date(0L), null, 0.0), // One day before epoch (negative timestamp)
        Arguments.of("Date representing one day before the epoch should return -86400.0 seconds", new Date(-86400000L), null, -86400.0), // Fractional seconds
        Arguments.of("Date with millisecond remainder verifies fractional seconds are preserved", new Date(12345L), null, 12.345), // Very large timestamp (Long.MAX_VALUE)
        Arguments.of("Maximum long timestamp checks handling of very large values without overflow", new Date(Long.MAX_VALUE), null, Long.MAX_VALUE / 1000.0), // java.sql.Date (subclass of java.util.Date)
        Arguments.of("java.sql.Date (subclass of java.util.Date) should be treated the same as java.util.Date", // java.sql.Date extends java.util.Date
        new Date(1609459200000L), null, 1609459200.0), // Non‑null Converter argument (ignored)
        Arguments.of("Non‑null Converter argument is ignored; conversion still yields correct seconds", new Date(1000L), // Using the mockConverter defined in the test class (non‑null)
        new DummyConverter(), 1.0), // Custom subclass of Date overriding getTime()
        Arguments.of("A custom subclass of Date overriding getTime() should be used by the method; verifies polymorphic behavior", new MyDate(5000L), null, 5.0));
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        return Stream.of(// Null 'from' argument
        Arguments.of("Passing a null 'from' argument should cause a NullPointerException when getTime() is invoked.", null, null, NullPointerException.class), // Wrong type (String)
        Arguments.of("Passing an object that is not a Date (e.g., a String) should result in a ClassCastException during the cast.", "2021-01-01", null, ClassCastException.class), // Date subclass whose getTime() throws IllegalStateException
        Arguments.of("A Date subclass whose getTime() method throws an IllegalStateException should propagate that exception when toDouble is invoked.", new ThrowingDate(), null, IllegalStateException.class));
    }

    @Test
    void toDouble_returnsSecondsForTypicalDate() {
        // given
        // 1500 milliseconds = 1.5 seconds
        long millis = 1_500L;
        Date date = new Date(millis);
        // when
        double result = DateConversions.toDouble(date, mockConverter);
        // then
        assertEquals(1.5, result, 1e-12, "The conversion should return milliseconds divided by 1000.0");
    }

    @Test
    void toDouble_returnsZeroForEpoch() {
        // given
        // Jan 1, 1970 00:00:00 GMT
        Date epoch = new Date(0L);
        // when
        double result = DateConversions.toDouble(epoch, mockConverter);
        // then
        assertEquals(0.0, result, 0.0, "Epoch should convert to 0.0 seconds");
    }

    @Test
    void toDouble_handlesNegativeTimestamps() {
        // given
        // -2 seconds
        long millis = -2_000L;
        Date date = new Date(millis);
        // when
        double result = DateConversions.toDouble(date, mockConverter);
        // then
        assertEquals(-2.0, result, 1e-12, "Negative timestamps should be correctly converted");
    }

    @Test
    void toDouble_throwsClassCastExceptionWhenNotADate() {
        // given
        Object notADate = "2023-01-01";
        // when / then
        assertThrows(ClassCastException.class, () -> DateConversions.toDouble(notADate, mockConverter), "Passing a non‑Date object should result in a ClassCastException");
    }

    @Test
    void toDouble_throwsNullPointerExceptionWhenNull() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> DateConversions.toDouble(nullObj, mockConverter), "Passing null should result in a NullPointerException");
    }

    /**
     * Covers the majority of normal scenarios:
     * <ul>
     *   <li>java.util.Date, java.sql.Timestamp, java.sql.Date subclasses</li>
     *   <li>positive, zero, negative and very large timestamps</li>
     *   <li>fractional seconds preservation</li>
     *   <li>non‑null Converter argument (ignored by the implementation)</li>
     *   <li>custom {@code Date} subclass overriding {@code getTime()}</li>
     * </ul>
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulConversions")
    void toDouble_successfulConversions(String description, Object from, Converter converter, double expected) {
        // when
        double result = DateConversions.toDouble(from, converter);
        // then
        assertEquals(expected, result, 1e-12, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionScenarios")
    void toDouble_expectedExceptions(String description, Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> DateConversions.toDouble(from, converter), description);
    }
}
