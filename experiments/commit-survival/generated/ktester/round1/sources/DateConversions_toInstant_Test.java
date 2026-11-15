package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DateConversions_toInstant_Test {

    @Mock
    private Converter // not used by the method under test
    converter;

    @Mock
    private Converter // not used by the method under test
    mockConverter;

    /**
     * Source for the java.sql.Date branch tests.
     */
    private static Stream<Arguments> sqlDateBranchProvider() {
        return Stream.of(// epoch 0
        Arguments.of(new java.sql.Date(0L), Instant.ofEpochMilli(0L)), // typical non‑zero epoch (2021‑06‑01T00:00:00Z)
        Arguments.of(new java.sql.Date(1_622_505_600_000L), Instant.ofEpochMilli(1_622_505_600_000L)), // negative epoch (one day before 1970‑01‑01)
        Arguments.of(new java.sql.Date(-86_400_000L), Instant.ofEpochMilli(-86_400_000L)), // max epoch minus one day
        Arguments.of(new java.sql.Date(Long.MAX_VALUE - 86_400_000L), Instant.ofEpochMilli(Long.MAX_VALUE - 86_400_000L)));
    }

    /**
     * Source for the non‑java.sql.Date branch tests (java.util.Date, Timestamp).
     */
    private static Stream<Arguments> nonSqlDateBranchProvider() {
        return Stream.of(// plain java.util.Date
        Arguments.of(new java.util.Date(1_234_567_890L), Instant.ofEpochMilli(1_234_567_890L)), // java.sql.Timestamp (extends java.util.Date) – zero nanoseconds
        Arguments.of(new Timestamp(0L, 0), Instant.ofEpochSecond(0L, 0L)), // Timestamp with nanoseconds
        Arguments.of(new Timestamp(123_456_789L, 999_999_999), Instant.ofEpochSecond(123_456_789L, 999_999_999L)), // Timestamp with negative epoch but positive nanos
        Arguments.of(new Timestamp(-1_000L, 250_000_000), // Timestamp normalises: -1000 ms = -1 sec, remainder 0 ms,
        // nanoseconds are added -> -1 sec + 250_000_000 ns
        Instant.ofEpochSecond(-1L, 250_000_000L)), // java.util.Date with negative epoch
        Arguments.of(new java.util.Date(-1L), Instant.ofEpochMilli(-1L)), // java.util.Date with Long.MAX_VALUE
        Arguments.of(new java.util.Date(Long.MAX_VALUE), Instant.ofEpochMilli(Long.MAX_VALUE)));
    }

    /**
     * Source for exception‑expectation tests.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// null input should cause NPE during cast
        Arguments.of(null, NullPointerException.class), // wrong type (String) should cause ClassCastException
        Arguments.of("2021-01-01", ClassCastException.class));
    }

    /**
     * Source for tests where a non‑null converter is supplied.
     */
    private static Stream<Arguments> nonNullConverterProvider() {
        // Dummy Converter implementation that simply returns null for any conversion.
        Converter dummy = new Converter() {

            @Override
            public <T> T convert(Object o, Class<T> c) {
                return null;
            }
        };
        return Stream.of(// java.util.Date with dummy Converter
        Arguments.of(new java.util.Date(5_000L), dummy, Instant.ofEpochMilli(5_000L)), // java.sql.Date with a plain Object (not a Converter) – method accepts Object,
        // so we pass the Object directly; the method will ignore it.
        Arguments.of(new java.sql.Date(2_000L), new Object(), Instant.ofEpochMilli(2_000L)));
    }

    @Test
    void toInstant_withUtilDate_returnsInstantFromDate() {
        // GIVEN
        // a fixed epoch millisecond value
        Date utilDate = new Date(1_600_000_000_000L);
        Instant expected = utilDate.toInstant();
        // WHEN
        Instant actual = DateConversions.toInstant(utilDate, converter);
        // THEN
        assertEquals(expected, actual, "Instant returned should match Date.toInstant()");
    }

    @Test
    void toInstant_withSqlDate_returnsInstantFromUnderlyingTime() {
        // GIVEN
        java.sql.Date sqlDate = new java.sql.Date(1_600_000_000_000L);
        // DateConversions creates a new java.util.Date from the same epoch millis.
        Instant expected = new Date(sqlDate.getTime()).toInstant();
        // WHEN
        Instant actual = DateConversions.toInstant(sqlDate, converter);
        // THEN
        assertEquals(expected, actual, "Instant for java.sql.Date should be based on its millisecond value");
    }

    @ParameterizedTest(name = "[{index}] java.sql.Date -> {0}")
    @MethodSource("sqlDateBranchProvider")
    void toInstant_sqlDateBranch_returnsCorrectInstant(Date sqlDate, Instant expected) {
        // WHEN
        Instant actual = DateConversions.toInstant(sqlDate, null);
        // THEN
        assertEquals(expected, actual, () -> "Instant for java.sql.Date " + sqlDate + " should be " + expected);
    }

    @ParameterizedTest(name = "[{index}] Non‑sql Date -> {0}")
    @MethodSource("nonSqlDateBranchProvider")
    void toInstant_nonSqlDateBranch_returnsCorrectInstant(Date date, Instant expected) {
        // WHEN
        Instant actual = DateConversions.toInstant(date, mockConverter);
        // THEN
        assertEquals(expected, actual, () -> "Instant for " + date.getClass().getSimpleName() + " should be " + expected);
    }

    @ParameterizedTest(name = "[{index}] Exception case with input: {0}")
    @MethodSource("exceptionProvider")
    void toInstant_exceptionScenarios(Object input, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> DateConversions.toInstant(input, null), () -> "Expected " + expectedException.getSimpleName() + " for input: " + input);
    }

    @ParameterizedTest(name = "[{index}] Non‑null converter usage with input: {0}")
    @MethodSource("nonNullConverterProvider")
    void toInstant_nonNullConverter_isIgnored(Object from, Object converterObj, Instant expected) {
        // The method signature expects a Converter, but we can safely pass any Object
        // because the parameter type is Object at compile time (the method casts only the first arg).
        Converter conv = (converterObj instanceof Converter) ? (Converter) converterObj : null;
        Instant actual = DateConversions.toInstant(from, conv);
        assertEquals(expected, actual, () -> "Converter should be ignored; Instant should be " + expected);
    }
}
