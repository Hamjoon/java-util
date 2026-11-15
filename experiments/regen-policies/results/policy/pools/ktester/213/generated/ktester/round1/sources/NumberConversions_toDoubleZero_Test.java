package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDoubleZero_Test {

    @Mock
    private Converter // mock is only used to verify that it is not touched
    mockConverter;

    /**
     * Simple placeholder class to emulate a user‑defined type.
     */
    static class MyClass {
        // intentionally empty
    }

    /**
     * Supplies all test scenarios described in the JSON test case list.
     * Includes primitive wrappers, strings, dates, temporal objects, collections,
     * custom objects, and different mock behaviours.
     */
    static Stream<Arguments> toDoubleZeroProvider() {
        // A regular mock that does nothing (default Mockito behaviour)
        Converter defaultMock = mock(Converter.class);
        // A mock that throws on any method invocation – useful to ensure the method never touches it
        Converter throwingMock = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        return Stream.of(// Both arguments null
        Arguments.of(null, null), // Primitive wrapper, no converter
        Arguments.of(123, null), // String representation of a number, no converter
        Arguments.of("456.78", null), // java.util.Date instance, no converter
        Arguments.of(Date.from(Instant.parse("2023-01-01T00:00:00Z")), null), // Arbitrary object with a normal mock converter
        Arguments.of("any object", defaultMock), // Double wrapper containing zero, no converter
        Arguments.of(0.0, null), // BigDecimal instance, no converter
        Arguments.of(new BigDecimal("123.45"), null), // BigInteger instance, no converter
        Arguments.of(new BigInteger("9876543210"), null), // AtomicInteger with a non‑zero value, no converter
        Arguments.of(new AtomicInteger(5), null), // Boolean true, no converter
        Arguments.of(Boolean.TRUE, null), // LocalDateTime instance, no converter
        Arguments.of(LocalDateTime.parse("2022-12-31T23:59:59"), null), // Duration instance, no converter
        Arguments.of(Duration.parse("PT5S"), null), // Primitive int array, no converter
        Arguments.of(new int[] { 1, 2, 3 }, null), // Null source with a converter that throws on any call
        Arguments.of(null, throwingMock), // Custom user‑defined object, no converter
        Arguments.of(new MyClass(), null), // Null source with a regular mock converter (default behaviour)
        Arguments.of(null, defaultMock));
    }

    @Test
    @DisplayName("toDoubleZero should always return Double zero irrespective of input")
    void testToDoubleZeroReturnsZeroForVariousInputs() {
        Object[] testValues = { null, "123.45", 42, 3.14, new Object(), Boolean.TRUE };
        for (Object value : testValues) {
            Double result = NumberConversions.toDoubleZero(value, mockConverter);
            // The method is defined to return CommonValues.DOUBLE_ZERO, which equals 0.0
            assertEquals(0.0, result, "Result should be zero for input: " + value);
            // Ensure the Converter is never consulted
            verifyNoInteractions(mockConverter);
        }
    }

    /**
     * Parameterised test that feeds a large collection of {@code from} objects
     * and various {@code Converter} instances into {@code NumberConversions.toDoubleZero}.
     * The expected result is always {@code 0.0} and the converter must remain untouched.
     */
    @ParameterizedTest(name = "{index}: from={0}, converter={1}")
    @MethodSource("toDoubleZeroProvider")
    @DisplayName("toDoubleZero should always return Double zero irrespective of input")
    void testToDoubleZero(Object from, Converter converter) {
        Double result = NumberConversions.toDoubleZero(from, converter);
        assertEquals(0.0, result, "Result should be zero for input: " + from);
        // The method must never call any method on the supplied converter.
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }
}
