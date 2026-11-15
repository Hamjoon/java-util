package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toSqlDate_Test {

    /**
     * Mocked Converter used by all test cases.
     */
    @Mock
    private Converter mockConverter;

    // 2022‑01‑01T00:00:00Z
    private static final long SAMPLE_EPOCH_MILLIS = 1_640_000_000_000L;

    @BeforeEach
    void setupBeforeEach() {
        // Reset mock before each test to avoid cross‑test interference.
        reset(mockConverter);
    }

    private static Stream<Arguments> numericInputs() {
        return Stream.of(// Long value – should be used directly as epoch millis.
        Arguments.of(123_456_789L, null, 123_456_789L, "Long value should be interpreted directly as milliseconds since epoch."), // Integer value – widened to long.
        Arguments.of(42, null, 42L, "Integer value should be widened to long and used as epoch milliseconds."), // Float value – truncated to long.
        Arguments.of(3.14f, null, 3L, "Floating‑point Number should be truncated to long before constructing the Date."));
    }

    private static Stream<Arguments> stringInputs() {
        return Stream.of(// Numeric string – parsed to long.
        Arguments.of("1625097600000", null, 1_625_097_600_000L, "String containing a numeric epoch millisecond value should be parsed to long and used to construct the Date."), // ISO‑8601 UTC timestamp – parsed to the same epoch millis as above.
        Arguments.of("2021-07-01T00:00:00Z", null, 1_625_097_600_000L, "ISO‑8601 UTC timestamp string should be parsed (via internal conversion) to the corresponding epoch milliseconds."), // Non‑numeric string – should cause IllegalArgumentException.
        Arguments.of("not-a-number", null, null, "A non‑numeric, non‑date string should cause an IllegalArgumentException during conversion."));
    }

    /**
     * Verify that a {@code String} representing a numeric epoch‑millisecond value is correctly
     * converted to a {@link Date} instance.
     */
    @Test
    void toSqlDate_fromString_returnsDateWithCorrectEpoch() {
        // given
        String source = String.valueOf(SAMPLE_EPOCH_MILLIS);
        // The static {@code toLong} method eventually asks the converter to produce a {@code Long}.
        when(mockConverter.convert(eq(source), eq(Long.class))).thenReturn(SAMPLE_EPOCH_MILLIS);
        // when
        Date result = LocalDateConversions.toSqlDate(source, mockConverter);
        // then
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(SAMPLE_EPOCH_MILLIS, result.getTime(), "Date should contain the epoch milliseconds supplied by the mock converter");
        // Verify that the converter was asked exactly once for the conversion.
        verify(mockConverter, times(1)).convert(eq(source), eq(Long.class));
    }

    /**
     * Verify that a {@code Long} value is passed through unchanged.
     */
    @Test
    void toSqlDate_fromLong_returnsDateWithSameEpoch() {
        // given
        Long source = SAMPLE_EPOCH_MILLIS;
        when(mockConverter.convert(eq(source), eq(Long.class))).thenReturn(SAMPLE_EPOCH_MILLIS);
        // when
        Date result = LocalDateConversions.toSqlDate(source, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(SAMPLE_EPOCH_MILLIS, result.getTime());
        verify(mockConverter, times(1)).convert(eq(source), eq(Long.class));
    }

    /**
     * Verify that {@code null} input yields a {@link Date} representing epoch {@code 0}.
     * The mock converter is configured to return {@code 0L} for a {@code null} source.
     */
    @Test
    void toSqlDate_fromNull_returnsEpochZeroDate() {
        // given
        when(mockConverter.convert(isNull(), eq(Long.class))).thenReturn(0L);
        // when
        Date result = LocalDateConversions.toSqlDate(null, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "A null source should be converted to a Date at epoch 0");
        verify(mockConverter, times(1)).convert(isNull(), eq(Long.class));
    }

    /**
     * Verify that an unsupported source type still results in a {@link Date}
     * with the epoch supplied by the mock converter.
     */
    @Test
    void toSqlDate_fromUnsupportedType_usesConverterResult() {
        // given
        // some arbitrary object
        Object source = new Object();
        when(mockConverter.convert(eq(source), eq(Long.class))).thenReturn(SAMPLE_EPOCH_MILLIS);
        // when
        Date result = LocalDateConversions.toSqlDate(source, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(SAMPLE_EPOCH_MILLIS, result.getTime());
        verify(mockConverter, times(1)).convert(eq(source), eq(Long.class));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("numericInputs")
    void toSqlDate_numericInputs(Object from, Converter converter, long expectedMillis, String description) {
        // given
        // If a custom converter is requested (not the case for the JSON data), we would mock it.
        // Here the JSON always uses a null converter, so we pass null.
        // when
        Date result = LocalDateConversions.toSqlDate(from, converter);
        // then
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(expectedMillis, result.getTime(), "Date epoch millis should match the expected value for numeric input.");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("stringInputs")
    void toSqlDate_stringInputs(Object from, Converter converter, Long expectedMillis, String description) {
        if (expectedMillis != null) {
            // happy path – expect a valid Date
            Date result = LocalDateConversions.toSqlDate(from, converter);
            assertNotNull(result, "Resulting Date must not be null");
            assertEquals(expectedMillis.longValue(), result.getTime(), "Date epoch millis should match the parsed value from the string.");
        } else {
            // error path – expect IllegalArgumentException
            assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toSqlDate(from, converter), "Parsing an invalid string should throw IllegalArgumentException.");
        }
    }

    @Test
    void toSqlDate_nullFrom_throwsNullPointerException() {
        // According to the JSON spec, a null 'from' should cause NPE because toLong cannot handle null.
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toSqlDate(null, null), "Passing a null 'from' value should cause NullPointerException.");
    }

    @Test
    void toSqlDate_booleanFrom_throwsIllegalArgumentException() {
        Boolean source = Boolean.TRUE;
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toSqlDate(source, null), "A Boolean value is not a supported source type for date conversion.");
    }

    @Test
    void toSqlDate_emptyString_throwsIllegalArgumentException() {
        String source = "";
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toSqlDate(source, null), "An empty string cannot be parsed to a numeric epoch millisecond value.");
    }

    @Test
    void toSqlDate_whitespaceString_throwsIllegalArgumentException() {
        String source = "   ";
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toSqlDate(source, null), "A whitespace‑only string is not a valid numeric representation.");
    }

    @Test
    void toSqlDate_decimalString_throwsIllegalArgumentException() {
        String source = "123.45";
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toSqlDate(source, null), "A decimal string cannot be converted to a long epoch millisecond value.");
    }

    @Test
    void toSqlDate_arbitraryObject_throwsIllegalArgumentException() {
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toSqlDate(source, null), "An arbitrary object that does not represent a date or number should cause IllegalArgumentException.");
    }

    @Test
    void toSqlDate_converterThrowsRuntimeException_propagatesException() {
        // given
        String source = "1625097600000";
        when(mockConverter.convert(eq(source), eq(Long.class))).thenThrow(new RuntimeException("Converter failure"));
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> LocalDateConversions.toSqlDate(source, mockConverter), "Exception thrown by the converter should propagate out of toSqlDate.");
        assertEquals("Converter failure", ex.getMessage(), "Propagated exception should retain the original message.");
        verify(mockConverter, times(1)).convert(eq(source), eq(Long.class));
    }
}
