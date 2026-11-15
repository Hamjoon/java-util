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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toCalendar_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private static final TimeZone TEST_TIME_ZONE = TimeZone.getTimeZone("UTC");

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock hierarchy: converter -> options -> time zone
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(TEST_TIME_ZONE);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks after each test to avoid cross‑test interference
        reset(mockConverter, mockOptions);
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(Arguments.of("Positive 1.5 seconds (UTC)", new BigDecimal("1.5"), "UTC", 1500L), Arguments.of("Zero seconds (UTC)", BigDecimal.ZERO, "UTC", 0L), Arguments.of("Negative -2.25 seconds (UTC)", new BigDecimal("-2.25"), "UTC", -2250L), Arguments.of("Large value that fits in long (UTC)", new BigDecimal("1234567890123.456"), "UTC", 1234567890123456L));
    }

    private static Stream<Arguments> roundingProvider() {
        return Stream.of(Arguments.of("1.999 seconds → 1999 ms (truncates)", new BigDecimal("1.999"), 1999L), Arguments.of("0.0005 seconds → 0 ms (fraction < 1 ms)", new BigDecimal("0.0005"), 0L), Arguments.of("-0.0015 seconds → -1 ms (negative truncates toward zero)", new BigDecimal("-0.0015"), -1L));
    }

    @Test
    void toCalendar_convertsPositiveSecondsToMillis_andUsesConverterTimeZone() {
        // given
        // 123.456 seconds
        BigDecimal seconds = new BigDecimal("123.456");
        // 123456 ms
        long expectedMillis = seconds.multiply(BigDecimal.valueOf(1000)).longValue();
        // when
        Calendar result = BigDecimalConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar time should be seconds converted to milliseconds");
        assertEquals(TEST_TIME_ZONE, result.getTimeZone(), "Calendar should use the TimeZone supplied by ConverterOptions");
    }

    @Test
    void toCalendar_convertsZeroSecondsToZeroMillis() {
        // given
        BigDecimal seconds = BigDecimal.ZERO;
        long expectedMillis = 0L;
        // when
        Calendar result = BigDecimalConversions.toCalendar(seconds, mockConverter);
        // then
        assertEquals(expectedMillis, result.getTimeInMillis(), "Zero seconds must result in zero milliseconds");
        assertEquals(TEST_TIME_ZONE, result.getTimeZone(), "TimeZone must still be the one from ConverterOptions");
    }

    @Test
    void toCalendar_convertsNegativeSecondsToNegativeMillis() {
        // given
        // -5.5 seconds
        BigDecimal seconds = new BigDecimal("-5.5");
        // -5500 ms
        long expectedMillis = seconds.multiply(BigDecimal.valueOf(1000)).longValue();
        // when
        Calendar result = BigDecimalConversions.toCalendar(seconds, mockConverter);
        // then
        assertEquals(expectedMillis, result.getTimeInMillis(), "Negative seconds should produce negative milliseconds");
        assertEquals(TEST_TIME_ZONE, result.getTimeZone(), "TimeZone must still be the one from ConverterOptions");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for valid conversions (including large values)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void toCalendar_validConversions(String description, BigDecimal input, String tzId, long expectedMillis) {
        // Arrange – configure the mock to return the requested time‑zone
        TimeZone tz = TimeZone.getTimeZone(tzId);
        when(mockOptions.getTimeZone()).thenReturn(tz);
        // Act
        Calendar cal = BigDecimalConversions.toCalendar(input, mockConverter);
        // Assert
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Milliseconds value does not match expected");
        assertEquals(tz, cal.getTimeZone(), "TimeZone should be the one supplied by the ConverterOptions");
    }

    /* -------------------------------------------------------------
     *  Rounding / truncation behavior
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("roundingProvider")
    void toCalendar_roundingBehavior(String description, BigDecimal input, long expectedMillis) {
        // Act
        Calendar cal = BigDecimalConversions.toCalendar(input, mockConverter);
        // Assert
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Truncation toward zero is expected");
    }

    /* -------------------------------------------------------------
     *  Different time‑zone handling
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_usesConverterSuppliedTimeZone() {
        // Arrange – New York time zone
        TimeZone ny = TimeZone.getTimeZone("America/New_York");
        when(mockOptions.getTimeZone()).thenReturn(ny);
        // 2500 ms
        BigDecimal seconds = new BigDecimal("2.5");
        // Act
        Calendar cal = BigDecimalConversions.toCalendar(seconds, mockConverter);
        // Assert
        assertEquals(2500L, cal.getTimeInMillis(), "Millisecond conversion must be correct regardless of time‑zone");
        assertEquals(ny, cal.getTimeZone(), "Calendar must be created with the supplied time‑zone");
    }

    /* -------------------------------------------------------------
     *  Null handling tests
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toCalendar(null, mockConverter), "Passing null 'from' should cause NPE due to cast to BigDecimal");
    }

    @Test
    void toCalendar_nullConverter_throwsNullPointerException() {
        BigDecimal seconds = new BigDecimal("1.0");
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toCalendar(seconds, null), "Passing null Converter should cause NPE when accessing getOptions()");
    }

    @Test
    void toCalendar_converterReturnsNullOptions_throwsNullPointerException() {
        // Converter mock that returns null for getOptions()
        Converter converterWithNullOptions = mock(Converter.class);
        when(converterWithNullOptions.getOptions()).thenReturn(null);
        BigDecimal seconds = new BigDecimal("1.0");
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toCalendar(seconds, converterWithNullOptions), "Null options should cause NPE when getTimeZone() is invoked");
    }

    @Test
    void toCalendar_optionsReturnNullTimeZone_throwsNullPointerException() {
        // Options mock that returns null for getTimeZone()
        ConverterOptions optionsWithNullTZ = mock(ConverterOptions.class);
        when(optionsWithNullTZ.getTimeZone()).thenReturn(null);
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(optionsWithNullTZ);
        BigDecimal seconds = new BigDecimal("2.0");
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toCalendar(seconds, converter), "Null TimeZone should cause NPE inside Calendar.getInstance(null)");
    }

    /* -------------------------------------------------------------
     *  Overflow behavior
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_overflowWhenMultiplyingBeyondLongMaxValue() {
        // Value just above Long.MAX_VALUE / 1000
        // *1000 overflows
        BigDecimal seconds = new BigDecimal("9223372036854776");
        // wraps around
        long expected = seconds.multiply(BigDecimal.valueOf(1000)).longValue();
        Calendar cal = BigDecimalConversions.toCalendar(seconds, mockConverter);
        assertEquals(expected, cal.getTimeInMillis(), "Overflow should follow BigDecimal.longValue() wrap‑around semantics");
    }

    /* -------------------------------------------------------------
     *  Invalid source‑type handling
     * ------------------------------------------------------------- */
    @Test
    void toCalendar_nonBigDecimalFrom_throwsClassCastException() {
        String notABigDecimal = "123.45";
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toCalendar(notABigDecimal, mockConverter), "Passing a non‑BigDecimal should cause a ClassCastException");
    }
}
