package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toSqlDate_Test {

    /**
     * Converter used for all tests – created once per test method.
     */
    private Converter converter;

    /* -------------------------------------------------------------
     *  Parameterised tests for the large set of valid conversions
     * ------------------------------------------------------------- */
    private static class ValidCase {

        final Object from;

        final long expectedEpochMilli;

        final String description;

        ValidCase(Object from, long expectedEpochMilli, String description) {
            this.from = from;
            this.expectedEpochMilli = expectedEpochMilli;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // ConverterOptions has a public no‑arg constructor in the library.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // The real Converter works for the built‑in types; no special configuration needed.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<ValidCase> validConversionProvider() {
        return Stream.of(// direct java.util.Date
        new ValidCase(new Date(1622548800000L), 1622548800000L, "java.util.Date instance"), // java.sql.Timestamp
        new ValidCase(Timestamp.valueOf("2021-06-01 12:00:00"), 1622548800000L, "java.sql.Timestamp"), // Instant
        new ValidCase(Instant.ofEpochMilli(1622548800000L), 1622548800000L, "java.time.Instant"), // LocalDateTime (treated as system default zone – tests assume UTC for reproducibility)
        new ValidCase(LocalDateTime.of(2021, 6, 1, 12, 0, 0), 1622548800000L, "java.time.LocalDateTime"), // LocalDate (midnight UTC)
        new ValidCase(LocalDate.of(2021, 6, 1), 1622505600000L, "java.time.LocalDate"), // ZonedDateTime with +02:00 offset
        new ValidCase(ZonedDateTime.parse("2021-06-01T12:00:00+02:00[Europe/Paris]"), 1622541600000L, "java.time.ZonedDateTime +02:00"), // OffsetDateTime with -04:00 offset
        new ValidCase(OffsetDateTime.parse("2021-06-01T12:00:00-04:00"), 1622556000000L, "java.time.OffsetDateTime -04:00"), // Long epoch millis
        new ValidCase(Long.valueOf(1622548800000L), 1622548800000L, "Long epoch millis"), // BigInteger epoch millis
        new ValidCase(new BigInteger("1622548800000"), 1622548800000L, "BigInteger epoch millis"), // Edge numeric cases
        new ValidCase(Long.MAX_VALUE, Long.MAX_VALUE, "Long.MAX_VALUE"), new ValidCase(Long.MIN_VALUE, Long.MIN_VALUE, "Long.MIN_VALUE"), new ValidCase(BigInteger.ZERO, 0L, "BigInteger zero"), new ValidCase(new BigInteger("-1"), -1L, "BigInteger negative one"));
    }

    @Test
    void toSqlDate_givenInstant_returnsDateWithSameEpochMilli() {
        // GIVEN
        // a deterministic timestamp
        long epochMilli = 1_617_283_945_123L;
        Instant instant = Instant.ofEpochMilli(epochMilli);
        // WHEN
        Date result = BigIntegerConversions.toSqlDate(instant, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(epochMilli, result.getTime(), "Date.getTime() must match the Instant's epoch milli value");
    }

    @Test
    void toSqlDate_givenLongTimestamp_returnsDateWithSameEpochMilli() {
        // GIVEN
        long epochMilli = 2_000_000_000L;
        // The library can convert a Long to an Instant via the Converter.
        Long source = Long.valueOf(epochMilli);
        // WHEN
        Date result = BigIntegerConversions.toSqlDate(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(epochMilli, result.getTime(), "Conversion from Long should yield a Date with the same epoch milliseconds");
    }

    @Test
    void toSqlDate_givenNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        Date result = BigIntegerConversions.toSqlDate(source, converter);
        // THEN
        assertNull(result, "Converting a null source should return null");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validConversionProvider")
    void toSqlDate_validConversions(ValidCase vc) {
        // WHEN
        Date result = BigIntegerConversions.toSqlDate(vc.from, converter);
        // THEN
        assertNotNull(result, "Result must not be null for " + vc.description);
        assertEquals(vc.expectedEpochMilli, result.getTime(), "Epoch milli mismatch for " + vc.description);
    }

    /* -------------------------------------------------------------
     *  Custom Converter scenarios
     * ------------------------------------------------------------- */
    @Test
    void toSqlDate_customConverterStringToInstant() {
        // Arrange: a mock Converter that turns a specific ISO‑8601 string into an Instant
        Converter mockConverter = mock(Converter.class);
        String iso = "2021-06-01T12:00:00Z";
        when(mockConverter.convert(eq(iso), eq(Instant.class))).thenReturn(Instant.parse(iso));
        // Act
        Date result = BigIntegerConversions.toSqlDate(iso, mockConverter);
        // Assert
        assertNotNull(result);
        assertEquals(Instant.parse(iso).toEpochMilli(), result.getTime());
        // Verify that the mock was actually invoked
        verify(mockConverter, times(1)).convert(eq(iso), eq(Instant.class));
    }

    @Test
    void toSqlDate_customConverterThrowsConversionException() {
        // Arrange: mock that throws a RuntimeException (simulating ConversionException)
        Converter badConverter = mock(Converter.class);
        String badInput = "not-a-date";
        when(badConverter.convert(eq(badInput), eq(Instant.class))).thenThrow(new RuntimeException("Conversion failed"));
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, () -> BigIntegerConversions.toSqlDate(badInput, badConverter));
        assertEquals("Conversion failed", ex.getMessage());
    }

    @Test
    void toSqlDate_customConverterReturnsNull() {
        // Arrange: mock that returns null for a valid string
        Converter nullReturning = mock(Converter.class);
        String input = "2021-01-01T00:00:00Z";
        when(nullReturning.convert(eq(input), eq(Instant.class))).thenReturn(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toSqlDate(input, nullReturning));
    }

    /* -------------------------------------------------------------
     *  Error handling for unsupported / null converter cases
     * ------------------------------------------------------------- */
    @Test
    void toSqlDate_nullSourceAndNullConverter_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toSqlDate(null, null));
    }

    @Test
    void toSqlDate_unsupportedObject_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toSqlDate(unsupported, converter));
    }

    @Test
    void toSqlDate_stringWithoutConverter_throwsNullPointerException() {
        // Strings are not directly handled; passing null converter should NPE inside toInstant
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toSqlDate("2021-01-01T00:00:00Z", null));
    }

    @Test
    void toSqlDate_bigDecimalUnsupported_throwsIllegalArgumentException() {
        BigDecimal bd = new BigDecimal("12345.67");
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toSqlDate(bd, converter));
    }

    @Test
    void toSqlDate_durationUnsupported_throwsIllegalArgumentException() {
        Duration dur = Duration.ofHours(1);
        assertThrows(IllegalArgumentException.class, () -> BigIntegerConversions.toSqlDate(dur, converter));
    }

    @Test
    void toSqlDate_instantMax_overflows_throwsArithmeticException() {
        // Instant.MAX has epoch second > Long.MAX_VALUE, so toEpochMilli overflows.
        assertThrows(ArithmeticException.class, () -> BigIntegerConversions.toSqlDate(Instant.MAX, converter));
    }
}
