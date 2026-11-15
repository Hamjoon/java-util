package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toCalendar_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock Converter to return the mock options.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions to keep each test isolated.
        clearInvocations(mockConverter, mockOptions);
    }

    /**
     * Provides arguments for the "Valid conversions" and "Boundary and special cases"
     * groups. Each argument contains:
     * <ul>
     *   <li>the {@link BigDecimal} input</li>
     *   <li>the {@link TimeZone} that the mock Converter should expose</li>
     *   <li>the expected {@code long} value for {@link Calendar#getTimeInMillis()}</li>
     * </ul>
     */
    private static Stream<Arguments> validAndBoundaryProvider() {
        return Stream.of(// ---- Valid conversions -------------------------------------------------
        Arguments.of(new BigDecimal("10"), TimeZone.getTimeZone("UTC"), 10_000L), Arguments.of(new BigDecimal("0"), TimeZone.getTimeZone("UTC"), 0L), Arguments.of(new BigDecimal("-5"), TimeZone.getTimeZone("UTC"), -5_000L), Arguments.of(new BigDecimal("1.5"), TimeZone.getTimeZone("UTC"), 1_500L), Arguments.of(new BigDecimal("2147483647"), TimeZone.getTimeZone("UTC"), 2_147_483_647_000L), // ---- Boundary & special cases -----------------------------------------
        // Long.MAX_VALUE seconds -> overflow, longValue() caps at Long.MAX_VALUE
        Arguments.of(new BigDecimal("9223372036854775807"), TimeZone.getTimeZone("UTC"), Long.MAX_VALUE), // Long.MIN_VALUE seconds -> overflow, caps at Long.MIN_VALUE
        Arguments.of(new BigDecimal("-9223372036854775808"), TimeZone.getTimeZone("UTC"), Long.MIN_VALUE), // Fractional seconds < 1 ms truncate to 0
        Arguments.of(new BigDecimal("0.0001"), TimeZone.getTimeZone("UTC"), 0L), // Negative fractional < 1 ms truncate toward zero -> 0
        Arguments.of(new BigDecimal("-0.0009"), TimeZone.getTimeZone("UTC"), 0L), // Large decimal with fraction, New York zone
        Arguments.of(new BigDecimal("123456789.987654321"), TimeZone.getTimeZone("America/New_York"), 123_456_789_987L), // Negative decimal with fraction, Tokyo zone
        Arguments.of(new BigDecimal("-1.23456789"), TimeZone.getTimeZone("Asia/Tokyo"), -1_234L), // Zero seconds with GMT+05:30 offset
        Arguments.of(new BigDecimal("0"), TimeZone.getTimeZone("GMT+05:30"), 0L), // Scientific notation 1E3 -> 1000 seconds
        Arguments.of(new BigDecimal("1E3"), TimeZone.getTimeZone("UTC"), 1_000_000L), // Scientific notation tiny negative fraction -> 0 ms
        Arguments.of(new BigDecimal("-1E-3"), TimeZone.getTimeZone("UTC"), 0L), // Very large number that overflows after multiplication (behaviour defined by longValue())
        Arguments.of(new BigDecimal("12345678901234567890"), TimeZone.getTimeZone("UTC"), new BigDecimal("12345678901234567890").multiply(BigDecimal.valueOf(1000)).longValue()));
    }

    /**
     * Provides arguments for the "Exception scenarios" group.
     * Each argument contains:
     * <ul>
     *   <li>the {@code Object} to pass as {@code from}</li>
     *   <li>a {@link Converter} (may be {@code null})</li>
     *   <li>the expected exception {@link Class}</li>
     * </ul>
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// null 'from' leads to NPE on cast
        Arguments.of(null, mock(Converter.class), NullPointerException.class), // non‑BigDecimal object leads to ClassCastException
        Arguments.of(new Object(), mock(Converter.class), ClassCastException.class), // null converter leads to NPE when accessing getOptions()
        Arguments.of(new BigDecimal("1"), null, NullPointerException.class), // converter with null TimeZone leads to NPE inside Calendar.getInstance(null)
        Arguments.of(new BigDecimal("1"), mock(Converter.class), NullPointerException.class));
    }

    @Test
    void toCalendar_WithPositiveSeconds_ConvertsToCorrectMillis() {
        // given
        // 2.5 seconds = 2500 ms
        BigDecimal seconds = new BigDecimal("2.5");
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(mockOptions.getTimeZone()).thenReturn(utc);
        // when
        Calendar result = BigDecimalConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(2500L, result.getTimeInMillis(), "Calendar time should be seconds * 1000 (GRAND) milliseconds");
        assertEquals(utc, result.getTimeZone(), "Calendar should be created with the Converter's time zone");
    }

    @Test
    void toCalendar_WithZeroSeconds_ReturnsEpoch() {
        // given
        BigDecimal seconds = BigDecimal.ZERO;
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(mockOptions.getTimeZone()).thenReturn(utc);
        // when
        Calendar result = BigDecimalConversions.toCalendar(seconds, mockConverter);
        // then
        assertEquals(0L, result.getTimeInMillis(), "Zero seconds should produce epoch (0 ms) time");
        assertEquals(utc, result.getTimeZone(), "Calendar should use the Converter's time zone");
    }

    @Test
    void toCalendar_WithNegativeSeconds_HandlesCorrectly() {
        // given
        // -3 seconds = -3000 ms
        BigDecimal seconds = new BigDecimal("-3");
        TimeZone utc = TimeZone.getTimeZone("UTC");
        when(mockOptions.getTimeZone()).thenReturn(utc);
        // when
        Calendar result = BigDecimalConversions.toCalendar(seconds, mockConverter);
        // then
        assertEquals(-3000L, result.getTimeInMillis(), "Negative seconds should produce negative milliseconds");
        assertEquals(utc, result.getTimeZone(), "Calendar should use the Converter's time zone");
    }

    @Test
    void toCalendar_UsesConverterProvidedTimeZone() {
        // given
        // 1 second = 1000 ms
        BigDecimal seconds = BigDecimal.ONE;
        TimeZone customTz = TimeZone.getTimeZone("GMT+02:00");
        when(mockOptions.getTimeZone()).thenReturn(customTz);
        // when
        Calendar result = BigDecimalConversions.toCalendar(seconds, mockConverter);
        // then
        assertEquals(1000L, result.getTimeInMillis(), "Milliseconds calculation should still be correct");
        assertEquals(customTz, result.getTimeZone(), "Calendar must be created with the time zone supplied by ConverterOptions");
    }

    @ParameterizedTest(name = "[{index}] from={0}, tz={1}")
    @MethodSource("validAndBoundaryProvider")
    void toCalendar_ValidAndBoundaryCases(BigDecimal from, TimeZone tz, long expectedMillis) {
        // Arrange
        when(mockOptions.getTimeZone()).thenReturn(tz);
        // Act
        Calendar result = BigDecimalConversions.toCalendar(from, mockConverter);
        // Assert
        assertNotNull(result, "Resulting Calendar must not be null");
        assertEquals(expectedMillis, result.getTimeInMillis(), () -> String.format("Expected %d ms for input %s", expectedMillis, from));
        assertEquals(tz, result.getTimeZone(), "Calendar must use the time zone supplied by ConverterOptions");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("exceptionProvider")
    void toCalendar_ExceptionScenarios(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // Special handling for the last case where the converter exists but its options return null timezone
        if (converter != null && expectedException == NullPointerException.class && from instanceof BigDecimal) {
            // Configure the mock to return a mock options whose getTimeZone() returns null
            ConverterOptions nullTzOptions = mock(ConverterOptions.class);
            when(converter.getOptions()).thenReturn(nullTzOptions);
            when(nullTzOptions.getTimeZone()).thenReturn(null);
        } else if (converter != null) {
            // For other non‑null converters, just return a valid options (timezone irrelevant because we expect an exception earlier)
            when(converter.getOptions()).thenReturn(mockOptions);
        }
        // Act & Assert
        assertThrows(expectedException, () -> BigDecimalConversions.toCalendar(from, converter), () -> String.format("Expected %s for from=%s, converter=%s", expectedException.getSimpleName(), from, converter));
    }
}
