package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toAtomicLong_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter constructor requires a ConverterOptions instance.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validConversionProvider() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2021‑01‑01T00:00:00Z
        cal.setTimeInMillis(1_609_459_200_000L);
        return Stream.of(// Long input
        Arguments.of(123L, 123L), // Integer input (autoboxed)
        Arguments.of(45, 45L), // Numeric String
        Arguments.of("6789", 6_789L), // BigDecimal (fractional part truncated)
        Arguments.of(new BigDecimal("12345.67"), 12_345L), // java.util.Date
        Arguments.of(new Date(1_609_459_200_000L), 1_609_459_200_000L), // java.util.Calendar
        Arguments.of(cal, 1_609_459_200_000L), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1_000L), 1_000L));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("abc"), Arguments.of("  123  "), // Long.MAX_VALUE + 1
        Arguments.of("9223372036854775808"));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE), Arguments.of(LocalDate.of(2023, 1, 1)), Arguments.of(new int[] { 1, 2, 3 }));
    }

    @Test
    void toAtomicLong_fromLong_returnsSameValue() {
        Long source = 123L;
        AtomicLong result = LocalDateConversions.toAtomicLong(source, converter);
        assertNotNull(result, "Result should never be null");
        assertEquals(123L, result.get(), "AtomicLong should contain the original long value");
    }

    @Test
    void toAtomicLong_fromStringNumber_returnsParsedValue() {
        String source = "456";
        AtomicLong result = LocalDateConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(456L, result.get(), "String numeric value should be parsed to long");
    }

    @Test
    void toAtomicLong_fromNull_returnsZero() {
        AtomicLong result = LocalDateConversions.toAtomicLong(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.get(), "Null source should be converted to 0");
    }

    @Test
    void toAtomicLong_fromDate_returnsEpochMillis() {
        // Create a deterministic Date (e.g., 2023‑01‑01T00:00:00Z)
        // epoch milliseconds for 2023‑01‑01
        Date source = new Date(1_672_569_600_000L);
        AtomicLong result = LocalDateConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(source.getTime(), result.get(), "Date should be converted to its epoch millisecond representation");
    }

    @Test
    void toAtomicLong_fromCalendar_returnsEpochMillis() {
        // Calendar set to the same instant as the Date above
        java.util.Calendar cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1_672_569_600_000L);
        AtomicLong result = LocalDateConversions.toAtomicLong(cal, converter);
        assertNotNull(result);
        assertEquals(cal.getTimeInMillis(), result.get(), "Calendar should be converted to its epoch millisecond representation");
    }

    @ParameterizedTest(name = "toAtomicLong converts {0} to AtomicLong({1})")
    @MethodSource("validConversionProvider")
    void toAtomicLong_validConversions(Object source, long expected) {
        AtomicLong result = LocalDateConversions.toAtomicLong(source, converter);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), () -> "Conversion of " + source + " should yield AtomicLong with value " + expected);
    }

    @Test
    void toAtomicLong_nullSource_andNullConverter_throwsNPE() {
        // According to the specification, a null source with a null converter
        // should result in a NullPointerException.
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toAtomicLong(null, null), "Both arguments null should throw NullPointerException");
    }

    @Test
    void toAtomicLong_emptyString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> LocalDateConversions.toAtomicLong("", converter), "Empty string cannot be parsed to a number");
    }

    @ParameterizedTest(name = "toAtomicLong throws NumberFormatException for ''{0}''")
    @MethodSource("invalidStringProvider")
    void toAtomicLong_invalidStringInputs(String source) {
        assertThrows(NumberFormatException.class, () -> LocalDateConversions.toAtomicLong(source, converter), "String '" + source + "' should cause NumberFormatException");
    }

    @ParameterizedTest(name = "toAtomicLong throws IllegalArgumentException for {0}")
    @MethodSource("unsupportedTypesProvider")
    void toAtomicLong_unsupportedTypes(Object source) {
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toAtomicLong(source, converter), "Source type " + source.getClass().getSimpleName() + " should not be convertible");
    }

    @Test
    void toAtomicLong_converterThrowsConversionException_propagates() {
        // Create a mock Converter that always throws ConversionException
        Converter failingConverter = Mockito.mock(Converter.class);
        Mockito.doThrow(new ConversionException("forced failure")).when(failingConverter).convert(any(), any());
        // The source itself is valid; the exception should come from the converter.
        assertThrows(ConversionException.class, () -> LocalDateConversions.toAtomicLong("123", failingConverter), "A failing converter should propagate ConversionException");
    }
}
