package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.time.DateTimeException;
import java.util.stream.Stream;

class TimestampConversions_toBigDecimal_Test {

    private Converter converter;

    private Timestamp timestamp;

    @BeforeEach
    void setupBeforeEach() {
        // Create a Converter with default options
        converter = new Converter(new ConverterOptions());
        // Create a deterministic Timestamp (e.g., 2023‑01‑01T12:34:56.789Z)
        Instant instant = Instant.parse("2023-01-01T12:34:56.789Z");
        timestamp = Timestamp.from(instant);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the pattern consistent
        converter = null;
        timestamp = null;
    }

    private static Stream<Arguments> validTimestampProvider() {
        return Stream.of(Arguments.of("Normal timestamp created from epoch millis", // 2021‑06‑01T12:00:00Z
        new Timestamp(1622548800000L), new Converter(new ConverterOptions())), Arguments.of("Timestamp created from a date‑time string", Timestamp.valueOf("2023-01-01 12:34:56.789"), new Converter(new ConverterOptions())));
    }

    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of("String is not a Timestamp", "2022-01-01"), Arguments.of("java.util.Date is not a java.sql.Timestamp", new java.util.Date()));
    }

    private static Stream<Arguments> edgeTimestampProvider() {
        return Stream.of(Arguments.of("Timestamp with Long.MAX_VALUE should overflow", new Timestamp(Long.MAX_VALUE)), Arguments.of("Timestamp with Long.MIN_VALUE should underflow", new Timestamp(Long.MIN_VALUE)));
    }

    @Test
    void toBigDecimal_ShouldReturnSameAsConverterDirectConversion() {
        // GIVEN – a Timestamp and a Converter (already prepared in @BeforeEach)
        // WHEN – we invoke the method under test
        BigDecimal result = TimestampConversions.toBigDecimal(timestamp, converter);
        // THEN – the result must be equal to the direct conversion of the Instant
        BigDecimal expected = converter.convert(timestamp.toInstant(), BigDecimal.class);
        assertEquals(expected, result, "TimestampConversions.toBigDecimal should delegate to InstantConversions");
    }

    @Test
    void toBigDecimal_NullTimestamp_ShouldThrowNullPointerException() {
        // GIVEN
        Timestamp nullTimestamp = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toBigDecimal(nullTimestamp, converter), "Passing a null Timestamp should result in NPE because toInstant() is invoked on null");
    }

    @Test
    void toBigDecimal_NonTimestampObject_ShouldThrowClassCastException() {
        // GIVEN
        Object notATimestamp = "2023-01-01T12:34:56.789Z";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimestampConversions.toBigDecimal(notATimestamp, converter), "Passing an object that is not a Timestamp should cause a ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validTimestampProvider")
    void toBigDecimal_ValidTimestamp_ShouldDelegateToInstantConversions(String description, Timestamp input, Converter conv) {
        // WHEN
        BigDecimal result = TimestampConversions.toBigDecimal(input, conv);
        // THEN – result must be exactly the same as the direct Instant conversion.
        BigDecimal expected = InstantConversions.toBigDecimal(input.toInstant(), conv);
        assertEquals(expected, result, "TimestampConversions.toBigDecimal should delegate to InstantConversions");
    }

    @Test
    void toBigDecimal_NullConverter_ShouldThrowNullPointerException() {
        // GIVEN
        Timestamp ts = new Timestamp(0L);
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> TimestampConversions.toBigDecimal(ts, nullConverter), "A null Converter should cause a NPE when InstantConversions attempts to use it");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidTypeProvider")
    void toBigDecimal_InvalidSourceType_ShouldThrowClassCastException(String description, Object input) {
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> TimestampConversions.toBigDecimal(input, new Converter(new ConverterOptions())), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeTimestampProvider")
    void toBigDecimal_EdgeTimestamp_ShouldThrowDateTimeException(String description, Timestamp edgeTs) {
        // WHEN / THEN – Instant.ofEpochMilli for these values throws DateTimeException,
        // so the method under test should propagate that exception.
        assertThrows(DateTimeException.class, () -> TimestampConversions.toBigDecimal(edgeTs, new Converter(new ConverterOptions())), description);
    }

    @Test
    void toBigDecimal_ConverterThrowsException_ShouldPropagate() {
        // GIVEN – a normal timestamp
        Timestamp ts = new Timestamp(1622548800000L);
        // AND – a mock Converter that throws IllegalArgumentException on any convert call
        Converter failingConverter = mock(Converter.class);
        when(failingConverter.convert(any(), eq(BigDecimal.class))).thenThrow(new IllegalArgumentException("Converter failure"));
        // WHEN / THEN – the IllegalArgumentException must bubble up unchanged
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> TimestampConversions.toBigDecimal(ts, failingConverter), "Exception from the Converter should propagate");
        assertEquals("Converter failure", thrown.getMessage());
    }
}
