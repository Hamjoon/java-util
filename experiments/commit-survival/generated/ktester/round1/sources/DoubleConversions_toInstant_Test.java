package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DateTimeException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toInstant_Test {

    /**
     * A mocked Converter – the implementation of {@code toInstant} does not use it,
     *  but the method signature requires it.
     */
    @Mock
    Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock for us; ensure it is clean before each test.
        reset(mockConverter);
    }

    Stream<Arguments> standardConversionProvider() {
        return Stream.of(// Zero value → epoch
        Arguments.of(0.0, mockConverter, "1970-01-01T00:00:00Z", "Zero value should produce the epoch instant."), // Whole‑second double
        Arguments.of(12345.0, mockConverter, "1970-01-01T03:25:45Z", "Whole number seconds are converted directly to an instant with zero nanoseconds."), // Positive fractional seconds
        Arguments.of(1.5, mockConverter, "1970-01-01T00:00:01.500000000Z", "Positive fractional part should be represented as nanoseconds."), // Negative fractional seconds
        Arguments.of(-1.5, mockConverter, "1969-12-31T23:59:58.500000000Z", "Negative value with fractional part; verifies correct handling of negative seconds and nanosecond normalization."), // Rounding edge – 1.9999999995 → 2 seconds
        Arguments.of(1.9999999995, mockConverter, "1970-01-01T00:00:02Z", "Fractional part rounds up to an extra whole second (nanoseconds become 1_000_000_000)."), // Very large positive seconds
        Arguments.of(1_000_000_000_000.0, mockConverter, "33658-09-27T01:46:40Z", "Very large positive seconds to test Instant's ability to represent far‑future dates."), // Double.NaN should be treated as 0 seconds / 0 nanos
        Arguments.of(Double.NaN, mockConverter, "1970-01-01T00:00:00Z", "Double.NaN is cast to 0 seconds and 0 nanoseconds, resulting in the epoch instant."), // Non‑null converter supplied (ignored by implementation)
        Arguments.of(2.0, mockConverter, "1970-01-01T00:00:02Z", "A non‑null Converter is supplied but ignored; conversion should still produce the correct instant."));
    }

    Stream<Arguments> exceptionProvider() {
        return Stream.of(// Positive infinity overflows Instant range
        Arguments.of(Double.POSITIVE_INFINITY, mockConverter, DateTimeException.class, "Positive infinity overflows the allowed range for Instant; Instant.ofEpochSecond throws DateTimeException."), // Non‑Double input (Integer) triggers ClassCastException
        Arguments.of(Integer.valueOf(5), mockConverter, ClassCastException.class, "Non‑Double input (Integer) causes a ClassCastException when casting to Double."), // Null 'from' argument → NullPointerException during cast
        Arguments.of(null, mockConverter, NullPointerException.class, "Null 'from' argument leads to NullPointerException during the cast to Double."));
    }

    @Test
    @DisplayName("toInstant converts a whole‑second double correctly")
    void testWholeNumberConversion() {
        // 2021‑05‑31T12:00:00Z
        Double source = 1_620_548_800.0;
        Instant expected = Instant.ofEpochSecond(1_620_548_800L, 0L);
        Instant actual = DoubleConversions.toInstant(source, mockConverter);
        assertEquals(expected, actual, "Instant should match whole‑second value");
    }

    @Test
    @DisplayName("toInstant converts a double with fractional seconds (nanosecond precision)")
    void testFractionalSecondConversion() {
        // 2021‑05‑31T12:00:00.123456789Z
        Double source = 1_620_548_800.123_456_789;
        // 1_620_548_800
        long seconds = source.longValue();
        long nanos = Math.round((source - seconds) * 1_000_000_000L);
        Instant expected = Instant.ofEpochSecond(seconds, nanos);
        Instant actual = DoubleConversions.toInstant(source, mockConverter);
        assertEquals(expected, actual, "Instant should contain correct seconds and nanoseconds");
    }

    @Test
    @DisplayName("toInstant handles negative epoch seconds")
    void testNegativeEpochConversion() {
        // before 1970‑01‑01
        Double source = -12345.678;
        // -12345
        long seconds = source.longValue();
        long nanos = Math.round((source - seconds) * 1_000_000_000L);
        Instant expected = Instant.ofEpochSecond(seconds, nanos);
        Instant actual = DoubleConversions.toInstant(source, mockConverter);
        assertEquals(expected, actual, "Instant should correctly represent negative epoch values");
    }

    @Test
    @DisplayName("toInstant rounds nanoseconds correctly when fractional part is near 1")
    void testRoundingEdgeCase() {
        // 1.9999999995 seconds → should round to 2 seconds, 0 nanos
        Double source = 1.999_999_999_5;
        // overflow handled by Instant
        Instant expected = Instant.ofEpochSecond(2L, 0L);
        Instant actual = DoubleConversions.toInstant(source, mockConverter);
        assertEquals(expected, actual, "Nanosecond rounding that overflows should increment the second part");
    }

    /* --------------------------------------------------------------
     *  Standard conversions – only successful Instant results.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("standardConversionProvider")
    void testStandardConversions(Object from, Converter converter, String expectedIso, String description) {
        // Act
        Instant actual = DoubleConversions.toInstant(from, converter);
        // Assert
        Instant expected = Instant.parse(expectedIso);
        assertEquals(expected, actual, description);
    }

    /* --------------------------------------------------------------
     *  Exception scenarios – the method is expected to throw.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionProvider")
    void testExceptionScenarios(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        // Act & Assert
        assertThrows(expectedException, () -> DoubleConversions.toInstant(from, converter), description);
    }
}
