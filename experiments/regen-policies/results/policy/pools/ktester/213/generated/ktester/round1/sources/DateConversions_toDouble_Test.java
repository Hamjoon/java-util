package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.sql.Timestamp;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.*;

class DateConversions_toDouble_Test {

    /**
     * A Converter instance – the method under test does not use it, but it must be supplied.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the pattern tidy.
        converter = null;
    }

    static Stream<Arguments> normalConversionProvider() {
        return Stream.of(arguments("Epoch start date should convert to 0.0 seconds.", new Date(0L), null, 0.0), arguments("Positive millisecond value is correctly divided by 1000.0.", new Date(12_345L), null, 12.345), arguments("Negative millisecond value (pre‑epoch) is correctly converted.", new Date(-5_000L), null, -5.0), arguments("A realistic timestamp with milliseconds should retain the fractional part after division.", new Date(1_622_548_800_123L), null, 1_622_548_800.123));
    }

    static Stream<Arguments> errorHandlingProvider() {
        return Stream.of(arguments("Passing a null 'from' leads to a NullPointerException when getTime() is invoked.", null, null, NullPointerException.class), arguments("If 'from' is not a java.util.Date, a ClassCastException is thrown during casting.", "2021-01-01", null, ClassCastException.class), arguments("Passing a null 'from' with a non‑null Converter still triggers NullPointerException.", null, new Converter(new ConverterOptions()), NullPointerException.class), arguments("Non‑Date 'from' with a non‑null Converter results in ClassCastException during casting.", "NotADate", new Converter(new ConverterOptions()), ClassCastException.class));
    }

    static Stream<Arguments> boundaryValuesProvider() {
        return Stream.of(arguments("Maximum long millisecond value should be converted to a double without overflow.", new Date(Long.MAX_VALUE), Long.MAX_VALUE / 1000.0), arguments("Minimum long millisecond value should be converted to a double preserving sign.", new Date(Long.MIN_VALUE), Long.MIN_VALUE / 1000.0), arguments("A single millisecond should produce a fractional second value of 0.001.", new Date(1L), 0.001));
    }

    static Stream<Arguments> timestampProvider() {
        return Stream.of(arguments("java.sql.Timestamp extends Date; conversion should use the underlying millisecond value.", new Timestamp(123_456_789L), 123_456.789), arguments("Negative timestamp subclass should be handled identically to java.util.Date.", new Timestamp(-987_654_321L), -987_654.321));
    }

    static Stream<Arguments> precisionEdgeCaseProvider() {
        return Stream.of(arguments("Negative millisecond value not divisible by 1000 should retain three decimal places.", new Date(-1_234L), -1.234), arguments("Just under one second should be represented with three decimal digits.", new Date(999L), 0.999));
    }

    @Test
    void toDouble_convertsDateToSeconds_correctly() {
        // Given: a Date representing a known epoch millisecond value.
        // 2022‑01‑01T00:00:00.123Z
        long epochMillis = 1_640_995_200_123L;
        Date date = new Date(epochMillis);
        // When: converting the Date to double seconds.
        double result = DateConversions.toDouble(date, converter);
        // Then: the result should be epochMillis divided by 1000.0.
        double expected = epochMillis / 1000.0;
        assertEquals(expected, result, 1e-12, "toDouble should return the epoch time in seconds with millisecond precision");
    }

    @Test
    void toDouble_handlesZeroMillis() {
        // Given: a Date at the epoch (0 milliseconds).
        Date epoch = new Date(0L);
        // When:
        double result = DateConversions.toDouble(epoch, converter);
        // Then:
        assertEquals(0.0, result, 0.0, "Zero‑epoch Date should convert to 0.0 seconds");
    }

    @Test
    void toDouble_nullDate_throwsNullPointerException() {
        // Given: a null reference.
        Date nullDate = null;
        // When / Then: a NullPointerException is expected because the method casts directly.
        assertThrows(NullPointerException.class, () -> DateConversions.toDouble(nullDate, converter), "Passing null as the source Date should throw NullPointerException");
    }

    @Test
    void toDouble_ignoresConverterArgument() {
        // Given: a Date and a mocked Converter (to prove the method does not interact with it).
        Date date = new Date(5_000L);
        Converter mockConverter = mock(Converter.class);
        // When:
        double result = DateConversions.toDouble(date, mockConverter);
        // Then: conversion is correct and no interactions with the mock occurred.
        assertEquals(5.0, result, 0.0);
        verifyNoInteractions(mockConverter);
    }

    /**
     * Normal conversions – positive, negative and realistic timestamps.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalConversionProvider")
    void toDouble_normalConversions(String description, Date from, Converter conv, double expected) {
        double result = DateConversions.toDouble(from, conv);
        assertEquals(expected, result, 1e-12, description);
    }

    /**
     * Converter argument handling – the method must ignore a non‑null Converter.
     */
    @Test
    void toDouble_converterArgumentIsIgnored() {
        Date date = new Date(2_000L);
        Converter nonNullConverter = new Converter(new ConverterOptions());
        double result = DateConversions.toDouble(date, nonNullConverter);
        assertEquals(2.0, result, 1e-12, "Method should ignore the provided Converter and still return correct seconds.");
    }

    /**
     * Error handling – null source or non‑Date source should throw appropriate exceptions.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("errorHandlingProvider")
    void toDouble_errorHandling(String description, Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DateConversions.toDouble(from, conv), description);
    }

    /**
     * Boundary values – conversion of Long.MAX_VALUE and Long.MIN_VALUE milliseconds.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryValuesProvider")
    void toDouble_boundaryValues(String description, Date from, double expected) {
        double result = DateConversions.toDouble(from, null);
        assertEquals(expected, result, 1e-6, description);
    }

    /**
     * Subclass handling – java.sql.Timestamp extends java.util.Date.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("timestampProvider")
    void toDouble_timestampSubclass(String description, Timestamp timestamp, double expected) {
        double result = DateConversions.toDouble(timestamp, null);
        assertEquals(expected, result, 1e-12, description);
    }

    /**
     * Precision edge cases – values that are not clean multiples of 1000.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("precisionEdgeCaseProvider")
    void toDouble_precisionEdgeCases(String description, Date from, double expected) {
        double result = DateConversions.toDouble(from, null);
        assertEquals(expected, result, 1e-12, description);
    }
}
