package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toString_Test {

    /**
     * Shared Converter instance – the method under test does not use it, but it must be supplied.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter implementation is not needed for the tests,
        // but we create a default instance to satisfy the method signature.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference
        converter = null;
    }

    /**
     * Supplies a range of {@link LocalTime} values together with the
     *  expected ISO‑8601 string representation.
     */
    private static Stream<Arguments> validLocalTimeProvider() {
        return Stream.of(// Simple time without seconds or nanos
        Arguments.of(LocalTime.of(13, 45, 30), "13:45:30"), // Midnight – seconds are zero, so ISO_LOCAL_TIME omits the seconds part
        Arguments.of(LocalTime.of(0, 0, 0, 0), "00:00"), // Maximum time with full nanosecond precision
        Arguments.of(LocalTime.of(23, 59, 59, 999_999_999), "23:59:59.999999999"), // Time with seconds but no fraction
        Arguments.of(LocalTime.of(12, 30, 45), "12:30:45"), // Fraction that is a clean multiple of 1,000,000 (3 decimal places)
        Arguments.of(LocalTime.of(12, 30, 45, 123_000_000), "12:30:45.123"), // Fraction with trailing zeros that should be trimmed
        Arguments.of(LocalTime.of(12, 30, 45, 120_000_000), "12:30:45.12"), // Single‑digit fractional second
        Arguments.of(LocalTime.of(12, 30, 45, 100_000_000), "12:30:45.1"), // Smallest non‑zero nanosecond – leading zeros in the fraction must be kept
        Arguments.of(LocalTime.of(0, 0, 0, 1), "00:00:00.000000001"));
    }

    @Test
    void toString_standardTime() {
        // given
        // 13:45:30
        LocalTime time = LocalTime.of(13, 45, 30);
        // when
        String result = LocalTimeConversions.toString(time, converter);
        // then
        assertEquals(time.format(DateTimeFormatter.ISO_LOCAL_TIME), result, "The formatted string should match ISO_LOCAL_TIME output");
    }

    @Test
    void toString_midnight() {
        // given
        // 00:00
        LocalTime midnight = LocalTime.MIDNIGHT;
        // when
        String result = LocalTimeConversions.toString(midnight, converter);
        // then
        assertEquals("00:00", result, "Midnight should be formatted without seconds when they are zero");
    }

    @Test
    void toString_withNanos() {
        // given
        // 09:15:05.123456789
        LocalTime timeWithNanos = LocalTime.of(9, 15, 5, 123_456_789);
        // when
        String result = LocalTimeConversions.toString(timeWithNanos, converter);
        // then
        assertEquals(timeWithNanos.format(DateTimeFormatter.ISO_LOCAL_TIME), result, "The formatted string should include fractional seconds when nanos are present");
    }

    @Test
    void toString_nullInput_throwsNPE() {
        // given
        Object nullObj = null;
        // when / then
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toString(nullObj, converter), "Passing null should result in a NullPointerException because of the cast");
    }

    @ParameterizedTest(name = "[{index}] toString({0}) → \"{1}\"")
    @MethodSource("validLocalTimeProvider")
    void toString_formatsCorrectly(LocalTime input, String expected) {
        // The method ignores the converter, so we can safely pass null here.
        String result = LocalTimeConversions.toString(input, null);
        assertEquals(expected, result, () -> "Formatted string does not match expected ISO_LOCAL_TIME output");
    }

    @Test
    void toString_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalTimeConversions.toString(null, converter), "Passing null as the 'from' argument should throw NullPointerException");
    }

    @Test
    void toString_nonLocalTime_throwsClassCastException() {
        Object notALocalTime = "I am not a LocalTime";
        assertThrows(ClassCastException.class, () -> LocalTimeConversions.toString(notALocalTime, converter), "Passing an object that is not a LocalTime should result in ClassCastException");
    }

    @Test
    void toString_converterArgumentIsIgnored() {
        // Even when a real Converter instance is supplied, the method must still format correctly.
        LocalTime time = LocalTime.of(12, 30);
        String result = LocalTimeConversions.toString(time, converter);
        // Expected string is produced by the ISO formatter (seconds are omitted because they are zero)
        assertEquals(time.format(DateTimeFormatter.ISO_LOCAL_TIME), result, "The Converter argument should be ignored; formatting must rely solely on LocalTime");
    }

    @Test
    void toString_converterThatThrows_isIgnored() {
        // Create a Converter implementation that throws if its convert method is ever called.
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should never be invoked by LocalTimeConversions.toString");
            }
        };
        // 15:20:10.5
        LocalTime time = LocalTime.of(15, 20, 10, 500_000_000);
        String result = LocalTimeConversions.toString(time, throwingConverter);
        assertEquals("15:20:10.5", result, "Even a Converter that throws must be ignored by the toString method");
    }
}
