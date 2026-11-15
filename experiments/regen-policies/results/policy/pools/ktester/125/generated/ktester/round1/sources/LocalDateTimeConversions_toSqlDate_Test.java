package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.sql.Date;
import java.time.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toSqlDate_Test {

    /**
     * Real Converter used for integration‑style tests.
     */
    private Converter realConverter;

    /**
     * Mocked Converter used for isolated tests.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a real Converter with default options.
        realConverter = new Converter(new ConverterOptions());
        // Mockito will initialise {@code mockConverter} automatically because of @ExtendWith(MockitoExtension.class)
    }

    /**
     * Supplies the valid conversion scenarios described in the JSON test data.
     */
    private static Stream<Arguments> validConversionProvider() {
        // 2021‑06‑01T00:00:00Z (UTC)
        long epochMillis = 1_622_505_600_000L;
        return Stream.of(// 1. Source is already a java.sql.Date
        Arguments.of(new java.sql.Date(epochMillis), epochMillis, "java.sql.Date should be returned unchanged (same epoch)"), // 2. LocalDateTime – time part is discarded, date part kept
        Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 34, 56), epochMillis, "LocalDateTime should be converted to a SQL Date at midnight UTC"), // 3. Long representing epoch milliseconds
        Arguments.of(epochMillis, epochMillis, "Long epoch milliseconds should be turned directly into a SQL Date"), // 4. ISO‑8601 date string
        Arguments.of("2021-06-01", epochMillis, "ISO‑8601 date string should be parsed into a SQL Date"), // 5. LocalDate – midnight of that date
        Arguments.of(LocalDate.of(2021, 6, 1), epochMillis, "LocalDate should be converted to a SQL Date at midnight"));
    }

    /**
     * Supplies the exception scenarios described in the JSON test data.
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source object – should cause NPE inside toLong
        Arguments.of(null, NullPointerException.class, "Null source should trigger NullPointerException"), // Boolean not supported
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "Boolean value should trigger IllegalArgumentException"), // Invalid date string – parsing fails
        Arguments.of("not-a-date", DateTimeParseException.class, "Unparseable date string should trigger DateTimeParseException"), // Numeric‑looking but invalid string – NumberFormatException
        Arguments.of("123abc", NumberFormatException.class, "Invalid numeric string should trigger NumberFormatException"), // Arbitrary object – no conversion path
        Arguments.of(new Object(), IllegalArgumentException.class, "Unsupported object type should trigger IllegalArgumentException"));
    }

    /**
     * Verify that a {@link Long} value is correctly turned into a {@link java.sql.Date}
     * (which is a subclass of {@link java.util.Date}) by delegating to the real
     * {@link Converter}. The resulting {@code Date} should have the same epoch
     * millisecond value as the supplied {@code Long}.
     */
    @Test
    void toSqlDate_WithLong_usingRealConverter_returnsDateWithSameTime() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        long epochMillis = 1_642_505_600_000L;
        // WHEN
        Date result = (Date) LocalDateTimeConversions.toSqlDate(epochMillis, realConverter);
        // THEN
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(epochMillis, result.getTime(), "The Date returned by toSqlDate should contain the same epoch milliseconds as the input Long");
    }

    /**
     * Verify that when the {@link Converter} is mocked to return a specific
     * {@code long} value for any input, {@code toSqlDate} creates a {@code Date}
     * with that exact value. This isolates the test from the internal conversion
     * logic of {@code LocalDateTimeConversions#toLong}.
     */
    @Test
    void toSqlDate_WithMockedConverter_returnsDateFromStubbedLong() {
        // GIVEN
        // the actual value does not matter – the mock will ignore it
        Object anyInput = new Object();
        long stubbedMillis = 9_876_543_210L;
        // Stub the mock Converter so that any call to convert(_, Long.class) returns the stubbed value.
        // The static helper method {@code toLong} eventually calls {@code converter.convert(...)}.
        when(mockConverter.convert(any(), eq(Long.class))).thenReturn(stubbedMillis);
        // WHEN
        Date result = (Date) LocalDateTimeConversions.toSqlDate(anyInput, mockConverter);
        // THEN
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(stubbedMillis, result.getTime(), "Date time should match the long value supplied by the mocked Converter");
        // Verify that the mock was indeed used.
        verify(mockConverter, atLeastOnce()).convert(any(), eq(Long.class));
    }

    /**
     * Parameterised test that checks a variety of supported source types.
     * All cases use the real {@link Converter} to exercise the actual conversion
     * logic of {@code LocalDateTimeConversions#toLong}.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validConversionProvider")
    void toSqlDate_ValidConversions(Object from, long expectedEpochMillis, String description) {
        // WHEN
        Date result = (Date) LocalDateTimeConversions.toSqlDate(from, realConverter);
        // THEN
        assertNotNull(result, "Result must not be null");
        assertTrue(result instanceof java.sql.Date, "Result should be an instance of java.sql.Date");
        assertEquals(expectedEpochMillis, result.getTime(), "Epoch milliseconds should match the expected value");
    }

    /**
     * Parameterised test that verifies the method throws the expected exception
     * for unsupported or malformed inputs.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("exceptionProvider")
    void toSqlDate_ExceptionScenarios(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> LocalDateTimeConversions.toSqlDate(from, realConverter), description);
    }
}
