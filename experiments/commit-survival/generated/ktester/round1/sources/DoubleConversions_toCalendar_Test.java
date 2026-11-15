package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Calendar;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DoubleConversions_toCalendar_Test {

    @Mock
    private Converter mockConverter;

    @Mock
    private ConverterOptions mockOptions;

    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    @BeforeEach
    void setupBeforeEach() {
        // Default mock hierarchy: converter returns options, options returns UTC.
        when(mockConverter.getOptions()).thenReturn(mockOptions);
        when(mockOptions.getTimeZone()).thenReturn(UTC);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that no unexpected interactions occurred.
        verifyNoMoreInteractions(mockConverter, mockOptions);
    }

    private static Stream<Arguments> validDoubleProvider() {
        return Stream.of(// seconds, expected epoch millis
        Arguments.of(0.0d, 0L), Arguments.of(1.5d, 1500L), Arguments.of(-10.0d, -10000L));
    }

    private static Stream<Arguments> edgeDoubleProvider() {
        return Stream.of(// seconds, expected epoch millis, description
        Arguments.of(Double.NaN, 0L, "NaN casts to 0L"), Arguments.of(Double.POSITIVE_INFINITY, Long.MAX_VALUE, "Positive infinity casts to Long.MAX_VALUE"), Arguments.of(Double.NEGATIVE_INFINITY, Long.MIN_VALUE, "Negative infinity casts to Long.MIN_VALUE"), Arguments.of(Double.MAX_VALUE, Long.MAX_VALUE, "Double.MAX_VALUE exceeds Long.MAX_VALUE, casts to Long.MAX_VALUE"), Arguments.of(Double.MIN_VALUE, 0L, "Double.MIN_VALUE (sub‑nanosecond) casts to 0L"));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of(null, ClassCastException.class, "null source should cause ClassCastException"), Arguments.of(Integer.valueOf(10), ClassCastException.class, "Integer cannot be cast to double"), Arguments.of("123.45", ClassCastException.class, "String cannot be cast to double"), Arguments.of(Float.valueOf(1.23f), ClassCastException.class, "Float cannot be cast to double"), Arguments.of(Long.valueOf(123L), ClassCastException.class, "Long cannot be cast to double"), Arguments.of(Boolean.TRUE, ClassCastException.class, "Boolean cannot be cast to double"), Arguments.of(new BigDecimal("123.45"), ClassCastException.class, "BigDecimal cannot be cast to double"), Arguments.of(new BigInteger("12345"), ClassCastException.class, "BigInteger cannot be cast to double"));
    }

    @Test
    void toCalendar_WithPositiveSeconds_ReturnsCorrectCalendar() {
        // given
        // corresponds to 2022-01-01T00:00:00.123Z
        double seconds = 1_640_995_200.123;
        long expectedMillis = (long) (seconds * 1000L);
        // when
        Calendar result = DoubleConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(UTC, result.getTimeZone(), "Calendar should use the UTC time zone");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar time (ms) should match the converted seconds");
        // Ensure the calendar is cleared before the time is set (hour/minute/second fields are zeroed)
        assertEquals(0, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND) % 1000);
    }

    @Test
    void toCalendar_WithZeroSeconds_ReturnsEpochStart() {
        // given
        double seconds = 0.0;
        long expectedMillis = 0L;
        // when
        Calendar result = DoubleConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(UTC, result.getTimeZone());
        assertEquals(expectedMillis, result.getTimeInMillis(), "Zero seconds should map to epoch start (0 ms)");
    }

    @Test
    void toCalendar_WithNegativeSeconds_ReturnsPastDate() {
        // given
        // one day and 750 ms before epoch
        double seconds = -86_400.75;
        long expectedMillis = (long) (seconds * 1000L);
        // when
        Calendar result = DoubleConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(UTC, result.getTimeZone());
        assertEquals(expectedMillis, result.getTimeInMillis(), "Negative seconds should produce a Calendar before the epoch");
    }

    @ParameterizedTest(name = "seconds={0} → expectedMillis={1}")
    @MethodSource("validDoubleProvider")
    void toCalendar_WithValidDouble_ReturnsCorrectCalendar(double seconds, long expectedMillis) {
        // when
        Calendar result = DoubleConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(UTC, result.getTimeZone(), "Calendar should use the UTC time zone");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar time (ms) should match the converted seconds");
        // The calendar is cleared before setting the time, so fields like hour/minute/second are zeroed.
        assertEquals(0, result.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, result.get(Calendar.MINUTE));
        assertEquals(0, result.get(Calendar.SECOND));
        assertEquals(0, result.get(Calendar.MILLISECOND) % 1000);
    }

    @Test
    void toCalendar_WithLargeDoubleAndNonUtcTimeZone_ReturnsCorrectCalendar() {
        // given
        // large positive value
        double seconds = 1_234_567_890d;
        long expectedMillis = (long) (seconds * 1000L);
        TimeZone ny = TimeZone.getTimeZone("America/New_York");
        when(mockOptions.getTimeZone()).thenReturn(ny);
        // when
        Calendar result = DoubleConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(ny, result.getTimeZone(), "Calendar should use the New York time zone");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar time (ms) should match the converted seconds");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("edgeDoubleProvider")
    void toCalendar_WithEdgeDoubleValues_ReturnsExpectedCalendar(double seconds, long expectedMillis, String description) {
        // when
        Calendar result = DoubleConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(UTC, result.getTimeZone(), "Time zone should be UTC");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Edge case '" + description + "' should produce expected epoch milliseconds");
    }

    @Test
    void toCalendar_WithDoubleAndAsiaKolkataTimeZone_ReturnsCorrectCalendar() {
        // given
        // 1234.5 ms → truncates to 1234 ms
        double seconds = 1.2345d;
        long expectedMillis = (long) (seconds * 1000L);
        TimeZone kolkata = TimeZone.getTimeZone("Asia/Kolkata");
        when(mockOptions.getTimeZone()).thenReturn(kolkata);
        // when
        Calendar result = DoubleConversions.toCalendar(seconds, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(kolkata, result.getTimeZone(), "Calendar should use Asia/Kolkata time zone");
        assertEquals(expectedMillis, result.getTimeInMillis(), "Calendar time (ms) should match the truncated milliseconds");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("invalidInputProvider")
    void toCalendar_WithInvalidSource_ThrowsException(Object source, Class<? extends Throwable> exceptionClass, String description) {
        // when / then
        assertThrows(exceptionClass, () -> DoubleConversions.toCalendar(source, mockConverter), "Expected " + exceptionClass.getSimpleName() + " for case: " + description);
    }

    @Test
    void toCalendar_WithNullTimeZone_ThrowsNullPointerException() {
        // given
        when(mockOptions.getTimeZone()).thenReturn(null);
        double seconds = 2.0d;
        // when / then
        assertThrows(NullPointerException.class, () -> DoubleConversions.toCalendar(seconds, mockConverter), "Null time zone should cause NullPointerException from Calendar.getInstance(null)");
    }

    @Test
    void toCalendar_WithNullConverter_ThrowsNullPointerException() {
        // given
        double seconds = 0.0d;
        // when / then
        assertThrows(NullPointerException.class, () -> DoubleConversions.toCalendar(seconds, null), "Null converter should cause NullPointerException when calling getOptions()");
    }

    @Test
    void toCalendar_WithNullOptions_ThrowsNullPointerException() {
        // given
        when(mockConverter.getOptions()).thenReturn(null);
        double seconds = 0.0d;
        // when / then
        assertThrows(NullPointerException.class, () -> DoubleConversions.toCalendar(seconds, mockConverter), "Null options should cause NullPointerException when calling getTimeZone()");
    }
}
