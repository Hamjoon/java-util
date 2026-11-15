package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.Date;
import java.sql.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class DateConversions_toInstant_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter is not used by the method under test, but we initialise it
        // to satisfy the method signature.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No resources to release for this test suite.
        converter = null;
    }

    private static Stream<Arguments> sqlDateProvider() {
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        Date sqlDate = new Date(instant.toEpochMilli());
        return Stream.of(arguments(sqlDate, instant));
    }

    private static Stream<Arguments> utilDateProvider() {
        Instant instant = Instant.parse("2023-06-15T08:00:00Z");
        Date utilDate = new Date(instant.toEpochMilli());
        return Stream.of(arguments(utilDate, instant));
    }

    private static Stream<Arguments> timestampProvider() {
        Instant instant = Instant.parse("2022-12-31T23:59:59Z");
        Timestamp ts = new Timestamp(instant.toEpochMilli());
        return Stream.of(arguments(ts, instant));
    }

    @Test
    void toInstant_withUtilDate_returnsSameInstant() {
        // GIVEN
        Date date = new Date(123_456_789L);
        Instant expected = date.toInstant();
        // WHEN
        Instant actual = DateConversions.toInstant(date, converter);
        // THEN
        assertEquals(expected, actual, "Instant derived from java.util.Date should match");
    }

    @Test
    void toInstant_withSqlDate_returnsSameInstantAsUtilDate() {
        // GIVEN
        java.sql.Date sqlDate = new java.sql.Date(123_456_789L);
        // The method creates a new java.util.Date from the sqlDate's time before calling toInstant()
        Instant expected = new Date(sqlDate.getTime()).toInstant();
        // WHEN
        Instant actual = DateConversions.toInstant(sqlDate, converter);
        // THEN
        assertEquals(expected, actual, "Instant derived from java.sql.Date should match the util Date conversion");
    }

    @Test
    void toInstant_withNull_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toInstant(nullObj, converter), "Calling toInstant with a null source should throw NPE");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] sqlDate={0}")
    @MethodSource("sqlDateProvider")
    void toInstant_sqlDate_returnsCorrectInstant(Date sqlDate, Instant expected) {
        // WHEN
        Instant actual = DateConversions.toInstant(sqlDate, null);
        // THEN
        assertEquals(expected, actual, "Instant from java.sql.Date must match the epoch‑millis of the original date");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] utilDate={0}")
    @MethodSource("utilDateProvider")
    void toInstant_utilDate_returnsSameInstant(Date utilDate, Instant expected) {
        // WHEN
        Instant actual = DateConversions.toInstant(utilDate, null);
        // THEN
        assertEquals(expected, actual, "Instant from java.util.Date (non‑sql) must be identical to date.toInstant()");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] timestamp={0}")
    @MethodSource("timestampProvider")
    void toInstant_timestamp_returnsInstant(Timestamp timestamp, Instant expected) {
        // WHEN
        Instant actual = DateConversions.toInstant(timestamp, null);
        // THEN
        assertEquals(expected, actual, "java.sql.Timestamp should be treated as a java.util.Date (false branch)");
    }

    // --------------------------------------------------------------------- //
    @Test
    void toInstant_nullInput_throwsNPE() {
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toInstant(null, null), "Null 'from' argument must cause NullPointerException");
    }

    // --------------------------------------------------------------------- //
    @Test
    void toInstant_nonDateInput_throwsClassCastException() {
        // GIVEN
        String notADate = "2023-01-01";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toInstant(notADate, null), "Passing a non‑Date object should result in ClassCastException");
    }

    // --------------------------------------------------------------------- //
    @Test
    void toInstant_epochZeroDate_returnsEpochInstant() {
        // GIVEN
        Date epochZero = new Date(0L);
        Instant expected = Instant.parse("1970-01-01T00:00:00Z");
        // WHEN
        Instant actual = DateConversions.toInstant(epochZero, null);
        // THEN
        assertEquals(expected, actual, "Date representing epoch zero must convert to Instant 1970-01-01T00:00:00Z");
    }

    // --------------------------------------------------------------------- //
    @Test
    void toInstant_maxLongDate_returnsMaxInstant() {
        // GIVEN
        Date maxDate = new Date(Long.MAX_VALUE);
        Instant expected = Instant.ofEpochMilli(Long.MAX_VALUE);
        // WHEN
        Instant actual = DateConversions.toInstant(maxDate, null);
        // THEN
        assertEquals(expected, actual, "Date constructed with Long.MAX_VALUE should map to the maximum representable Instant");
    }

    // --------------------------------------------------------------------- //
    @Test
    void toInstant_withNonNullConverter_ignoresConverter() {
        // GIVEN
        Instant instant = Instant.parse("2021-04-01T12:30:45Z");
        Date date = new Date(instant.toEpochMilli());
        // Create a mock Converter that would throw if used (it shouldn't be)
        Converter mockConverter = Mockito.mock(Converter.class);
        Mockito.doThrow(new AssertionError("Converter should not be invoked")).when(mockConverter).convert(Mockito.any(), Mockito.any());
        // WHEN
        Instant actual = DateConversions.toInstant(date, mockConverter);
        // THEN
        assertEquals(instant, actual, "Method should ignore the provided Converter and still return the correct Instant");
    }
}
