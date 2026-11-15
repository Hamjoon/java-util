package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.TimeZone;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toLocalDateTime_Test {

    private static TimeZone originalDefaultZone;

    private Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Remember the original default zone and force UTC for deterministic results
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // Converter has a public constructor that requires ConverterOptions.
        // An empty options instance is sufficient for the built‑in conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone
        TimeZone.setDefault(originalDefaultZone);
    }

    static Stream<Arguments> dateConversionProvider() {
        return Stream.of(Arguments.of(new Date(0L), "1970-01-01T00:00"), Arguments.of(new Date(-86_400_000L), "1969-12-31T00:00"));
    }

    @Test
    void shouldConvertBigDecimalEpochSecondsToLocalDateTime() {
        // GIVEN
        // 1622548800 seconds = 2021‑06‑01T00:00:00Z
        BigDecimal epochSeconds = new BigDecimal("1622548800");
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochSecond(1622548800L), ZoneOffset.UTC);
        // WHEN
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(epochSeconds, converter);
        // THEN
        assertEquals(expected, actual, "BigDecimal representing epoch seconds should be converted to the correct LocalDateTime");
    }

    @Test
    void shouldConvertBigDecimalWithFractionalNanosToLocalDateTime() {
        // GIVEN
        // 1622548800.123456789 seconds = 2021‑06‑01T00:00:00.123456789Z
        BigDecimal epochWithNanos = new BigDecimal("1622548800.123456789");
        long secondsPart = 1622548800L;
        int nanosPart = 123_456_789;
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochSecond(secondsPart, nanosPart), ZoneOffset.UTC);
        // WHEN
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(epochWithNanos, converter);
        // THEN
        assertEquals(expected, actual, "BigDecimal with fractional part should be converted preserving nanos");
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        LocalDateTime result = BigDecimalConversions.toLocalDateTime(source, converter);
        // THEN
        assertNull(result, "Conversion of null should return null");
    }

    /**
     * Parameterized test for converting {@link java.util.Date} instances.
     * The expected {@link LocalDateTime} is derived from the epoch‑millisecond value.
     */
    @ParameterizedTest(name = "[{index}] Date({0}) → {1}")
    @MethodSource("dateConversionProvider")
    void shouldConvertDateToLocalDateTime(Date input, String expectedIso) {
        // GIVEN
        // No custom converter is required for Date → LocalDateTime conversion.
        // The test framework supplies a null converter as per the JSON spec.
        // WHEN
        LocalDateTime actual = BigDecimalConversions.toLocalDateTime(input, null);
        // THEN
        LocalDateTime expected = LocalDateTime.parse(expectedIso);
        assertEquals(expected, actual, () -> "Date " + input + " should be converted to LocalDateTime " + expectedIso);
    }

    /**
     * Converting a {@code null} source should raise a {@link NullPointerException}
     * because the underlying {@code toZonedDateTime} implementation dereferences the input.
     */
    @Test
    void shouldThrowNPEWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toLocalDateTime(source, null), "Passing a null source should result in NullPointerException");
    }

    /**
     * An unsupported source type (plain {@link Object}) must trigger an
     * {@link IllegalArgumentException}.
     */
    @Test
    void shouldThrowIllegalArgumentExceptionForUnsupportedType() {
        // GIVEN
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toLocalDateTime(unsupported, null), "Unsupported source type should cause IllegalArgumentException");
    }

    /**
     * A non‑parseable {@link String} should cause a {@link DateTimeParseException}
     * during the internal parsing performed by {@code toZonedDateTime}.
     */
    @Test
    void shouldThrowDateTimeParseExceptionForInvalidString() {
        // GIVEN
        String invalid = "not-a-date";
        // WHEN / THEN
        assertThrows(DateTimeParseException.class, () -> BigDecimalConversions.toLocalDateTime(invalid, null), "Invalid date‑time string should cause DateTimeParseException");
    }

    /**
     * Converting a {@link LocalTime} (which lacks a date component) to a
     * {@link ZonedDateTime} is ambiguous and must result in a {@link DateTimeException}.
     */
    @Test
    void shouldThrowDateTimeExceptionForLocalTime() {
        // GIVEN
        LocalTime timeOnly = LocalTime.now();
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> BigDecimalConversions.toLocalDateTime(timeOnly, null), "Converting a LocalTime without a date should throw DateTimeException");
    }

    /**
     * A {@link BigDecimal} whose numeric value exceeds the range supported by
     * {@link java.time.Instant} (i.e., larger than {@link Long#MAX_VALUE} seconds)
     * must cause a {@link DateTimeException}.
     */
    @Test
    void shouldThrowDateTimeExceptionForOutOfRangeBigDecimal() {
        // GIVEN
        BigDecimal outOfRange = new BigDecimal(Long.MAX_VALUE).add(BigDecimal.ONE);
        // WHEN / THEN
        assertThrows(DateTimeException.class, () -> BigDecimalConversions.toLocalDateTime(outOfRange, null), "BigDecimal exceeding Instant range should cause DateTimeException");
    }
}
