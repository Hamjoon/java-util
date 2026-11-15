package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toTimestamp_Test {

    /**
     * Mocked Converter that will be injected into the static method under test.
     */
    @Mock
    private Converter converter;

    /**
     * Initializes Mockito annotations before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        MockitoAnnotations.openMocks(this);
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Supplies the valid conversion test data.
     */
    private static Stream<Arguments> validConversionProvider() {
        // 2021‑06‑01T00:00:00Z
        long epoch = 1_622_548_800_000L;
        return Stream.of(Arguments.of("Direct conversion from a Long value", 1622548800000L, epoch), Arguments.of("Conversion from an Integer (seconds‑based value)", Integer.valueOf(1622548800), // Integer interpreted as millis directly
        1_622_548_800L), Arguments.of("Conversion from a java.util.Date", new Date(epoch), epoch), Arguments.of("Conversion from a Calendar set to the epoch instant", calendarAt(epoch), epoch), Arguments.of("Conversion from an Instant", Instant.ofEpochMilli(epoch), epoch), Arguments.of("Conversion from a numeric String", "1622548800000", epoch), Arguments.of("Conversion from a LocalDateTime (system default zone assumed UTC)", LocalDateTime.of(2021, 6, 1, 0, 0), epoch));
    }

    /**
     * Helper that creates a Calendar instance positioned at the supplied epoch‑millis (UTC).
     */
    private static Calendar calendarAt(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(Arguments.of("Boolean source is not supported", Boolean.TRUE), Arguments.of("Arbitrary Object is not supported", new Object()));
    }

    private static Stream<Arguments> malformedStringProvider() {
        return Stream.of(Arguments.of("String with non‑numeric characters", "123abc"), Arguments.of("Empty string", ""));
    }

    private static Stream<Arguments> overflowProvider() {
        return Stream.of(Arguments.of("BigInteger exceeding Long.MAX_VALUE", new BigInteger("9223372036854775808")), Arguments.of("BigDecimal outside long range", new BigDecimal("12345678901234567890")), Arguments.of("BigDecimal with fractional part", new BigDecimal("123.456")));
    }

    private static Stream<Arguments> temporalInsufficientInfoProvider() {
        return Stream.of(Arguments.of("LocalDate without time or zone", LocalDate.of(2021, 6, 1)), Arguments.of("LocalTime without date", LocalTime.of(12, 30, 45)));
    }

    private static Stream<Arguments> unsupportedStringFormatProvider() {
        return Stream.of(Arguments.of("Invalid month in ISO‑8601 string", "2021-13-01T00:00:00"), Arguments.of("Non‑ISO textual date format", "June 1st, 2021"));
    }

    /**
     * Verify that a numeric {@code Long} value is correctly turned into a {@link Timestamp}.
     */
    @Test
    void toTimestamp_fromLong_returnsTimestampWithSameEpochMillis() {
        // Arrange
        // some arbitrary epoch‑millis value
        Long source = 1_642_000_000_123L;
        // The internal {@code toLong} implementation ultimately asks the converter to convert
        // the source to a {@code Long}.  We stub that conversion.
        when(converter.convert(eq(source), eq(Long.class))).thenReturn(source);
        // Act
        Timestamp result = DateConversions.toTimestamp(source, converter);
        // Assert
        assertNotNull(result, "Resulting Timestamp should not be null");
        assertEquals(source.longValue(), result.getTime(), "Timestamp should contain the exact epoch‑millis supplied by the converter");
        verify(converter, times(1)).convert(eq(source), eq(Long.class));
    }

    /**
     * Verify that a {@link String} representing a number is converted correctly.
     */
    @Test
    void toTimestamp_fromStringNumber_returnsTimestampWithParsedValue() {
        // Arrange
        String source = "1642000000123";
        Long expected = 1_642_000_000_123L;
        when(converter.convert(eq(source), eq(Long.class))).thenReturn(expected);
        // Act
        Timestamp result = DateConversions.toTimestamp(source, converter);
        // Assert
        assertEquals(expected.longValue(), result.getTime(), "Timestamp should reflect the long value parsed from the string");
        verify(converter, times(1)).convert(eq(source), eq(Long.class));
    }

    /**
     * Verify that a {@link Date} instance is converted to a {@link Timestamp}
     * with the same millisecond value.
     */
    @Test
    void toTimestamp_fromDate_returnsTimestampWithSameTime() {
        // Arrange
        Date source = new Date(1_642_000_000_123L);
        when(converter.convert(eq(source), eq(Long.class))).thenReturn(source.getTime());
        // Act
        Timestamp result = DateConversions.toTimestamp(source, converter);
        // Assert
        assertEquals(source.getTime(), result.getTime(), "Timestamp should have the same epoch‑millis as the original Date");
        verify(converter, times(1)).convert(eq(source), eq(Long.class));
    }

    /**
     * Verify that {@code null} input yields a {@link Timestamp} representing epoch zero
     * (the typical behaviour of the underlying {@code toLong} conversion when the source is {@code null}).
     */
    @Test
    void toTimestamp_fromNull_returnsEpochZeroTimestamp() {
        // Arrange
        when(converter.convert(isNull(), eq(Long.class))).thenReturn(0L);
        // Act
        Timestamp result = DateConversions.toTimestamp(null, converter);
        // Assert
        assertEquals(0L, result.getTime(), "When the source is null the resulting Timestamp should represent epoch 0");
        verify(converter, times(1)).convert(isNull(), eq(Long.class));
    }

    /* -------------------------------------------------------------
     *  Valid conversion scenarios – the mock returns the expected
     *  epoch‑millis value for each supplied source object.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void toTimestamp_validConversions(String description, Object source, long expectedEpochMillis) {
        // Arrange – stub the converter to return the expected long for the given source
        when(converter.convert(eq(source), eq(Long.class))).thenReturn(expectedEpochMillis);
        // Act
        Timestamp ts = DateConversions.toTimestamp(source, converter);
        // Assert
        assertNotNull(ts, "Timestamp must not be null");
        assertEquals(expectedEpochMillis, ts.getTime(), "Timestamp should contain the exact epoch‑millis supplied by the converter");
        verify(converter, times(1)).convert(eq(source), eq(Long.class));
    }

    /* -------------------------------------------------------------
     *  Null source – the underlying conversion returns 0L, resulting
     *  in the epoch‑zero Timestamp.
     * ------------------------------------------------------------- */
    @Test
    void toTimestamp_nullSource_returnsEpochZeroTimestamp() {
        when(converter.convert(isNull(), eq(Long.class))).thenReturn(0L);
        Timestamp ts = DateConversions.toTimestamp(null, converter);
        assertNotNull(ts);
        assertEquals(0L, ts.getTime(), "A null source should yield a Timestamp representing epoch 0");
        verify(converter, times(1)).convert(isNull(), eq(Long.class));
    }

    /* -------------------------------------------------------------
     *  Invalid source types – we expect IllegalArgumentException.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidSourceProvider")
    void toTimestamp_invalidSourceTypes(String description, Object source) {
        // No stubbing – the real implementation should throw
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toTimestamp(source, converter), description);
    }

    /* -------------------------------------------------------------
     *  Malformed numeric strings – NumberFormatException expected.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("malformedStringProvider")
    void toTimestamp_malformedNumericStrings(String description, String source) {
        // The real conversion will attempt to parse the string and fail
        assertThrows(NumberFormatException.class, () -> DateConversions.toTimestamp(source, converter), description);
    }

    /* -------------------------------------------------------------
     *  Overflow numeric types – ArithmeticException expected.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("overflowProvider")
    void toTimestamp_overflowNumericTypes(String description, Object source) {
        assertThrows(ArithmeticException.class, () -> DateConversions.toTimestamp(source, converter), description);
    }

    /* -------------------------------------------------------------
     *  Temporal types lacking sufficient information – DateTimeException.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("temporalInsufficientInfoProvider")
    void toTimestamp_temporalInsufficientInfo(String description, Object source) {
        assertThrows(DateTimeException.class, () -> DateConversions.toTimestamp(source, converter), description);
    }

    /* -------------------------------------------------------------
     *  Unsupported string date formats – DateTimeParseException.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedStringFormatProvider")
    void toTimestamp_unsupportedStringFormats(String description, String source) {
        assertThrows(DateTimeParseException.class, () -> DateConversions.toTimestamp(source, converter), description);
    }
}
