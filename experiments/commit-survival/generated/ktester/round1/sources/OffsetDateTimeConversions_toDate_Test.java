package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toDate_Test {

    /**
     * Shared Converter instance used by all test methods.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The library provides a default‑options constructor; this gives us a fully‑functional converter.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the pattern tidy.
        converter = null;
    }

    /**
     * Provides the successful conversion test data.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        // 2021‑06‑01T12:00:00Z
        long epoch = 1622548800000L;
        // Calendar instance configured for UTC
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(epoch);
        // java.sql.Date (midnight UTC)
        java.sql.Date sqlDate = java.sql.Date.valueOf("2021-06-01");
        return Stream.of(// Direct Long input
        Arguments.of(1622548800000L, epoch, "Direct Long input should be used as epoch milliseconds"), // Integer (auto‑boxed) input
        Arguments.of(123456789, 123456789L, "Integer input is coerced to long and used as epoch milliseconds"), // Numeric String
        Arguments.of("1622548800000", epoch, "String containing a numeric value should be parsed to long and converted to Date"), // java.util.Date
        Arguments.of(new Date(epoch), epoch, "When a java.util.Date is supplied, its time value is used directly"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2021-06-01 12:00:00.0"), epoch, "java.sql.Timestamp should be converted using its underlying milliseconds"), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(epoch), epoch, "Instant is converted to epoch milliseconds for the Date"), // LocalDateTime (interpreted as UTC)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 0), epoch, "LocalDateTime is interpreted as UTC and converted to Date"), // OffsetDateTime (UTC)
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC), epoch, "OffsetDateTime's instant is used to obtain epoch milliseconds"), // Calendar (UTC)
        Arguments.of(utcCal, epoch, "Calendar's time in milliseconds is used for the Date"), // BigDecimal
        Arguments.of(new BigDecimal("1622548800000"), epoch, "BigDecimal numeric value is converted to long epoch milliseconds"), // BigInteger
        Arguments.of(new BigInteger("1622548800000"), epoch, "BigInteger numeric value should be converted to long epoch milliseconds"), // AtomicLong
        Arguments.of(new AtomicLong(epoch), epoch, "AtomicLong holds a long epoch value; toDate must extract the long and create the corresponding Date"), // ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC")), epoch, "ZonedDateTime with UTC zone should be converted via its instant"), // java.sql.Date (midnight UTC)
        Arguments.of(sqlDate, 1622505600000L, "java.sql.Date represents a date without time; conversion treats it as midnight UTC"));
    }

    @Test
    void toDate_fromDate_returnsSameInstant() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        Date source = new Date(1_640_995_200_000L);
        // WHEN
        Date result = OffsetDateTimeConversions.toDate(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "Converting a java.util.Date should preserve the epoch millisecond value.");
    }

    @Test
    void toDate_fromOffsetDateTime_returnsEquivalentDate() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2022, 1, 1, 12, 30, 45, 123_000_000, ZoneOffset.UTC);
        long expectedMillis = odt.toInstant().toEpochMilli();
        // WHEN
        Date result = OffsetDateTimeConversions.toDate(odt, converter);
        // THEN
        assertNotNull(result);
        assertEquals(expectedMillis, result.getTime(), "Converting an OffsetDateTime should produce a Date with the same instant.");
    }

    @Test
    void toDate_fromLong_returnsDateWithThatEpochMillis() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        Long epochMillis = 1_640_995_200_000L;
        // WHEN
        Date result = OffsetDateTimeConversions.toDate(epochMillis, converter);
        // THEN
        assertNotNull(result);
        assertEquals(epochMillis.longValue(), result.getTime(), "Converting a Long representing epoch milliseconds should yield a matching Date.");
    }

    @Test
    void toDate_fromStringNumeric_returnsDateWithParsedMillis() {
        // GIVEN
        // 2022‑01‑01T00:00:00Z
        String epochString = "1640995200000";
        // WHEN
        Date result = OffsetDateTimeConversions.toDate(epochString, converter);
        // THEN
        assertNotNull(result);
        assertEquals(Long.parseLong(epochString), result.getTime(), "Converting a numeric String should be parsed to the corresponding epoch milliseconds.");
    }

    /**
     * Parameterized test covering every successful conversion scenario.
     *
     * @param from          the source object to convert
     * @param expectedEpoch the expected epoch‑millisecond value of the resulting {@link Date}
     * @param description   a short description of the scenario (used only for readability)
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successfulConversionProvider")
    void toDate_successful(Object from, long expectedEpoch, String description) {
        // WHEN
        Date result = OffsetDateTimeConversions.toDate(from, converter);
        // THEN
        assertNotNull(result, "Resulting Date should never be null");
        assertEquals(expectedEpoch, result.getTime(), () -> "Incorrect epoch millis for case: " + description);
    }

    @Test
    void toDate_negativeEpoch() {
        // 1969‑12‑31T00:00:00Z
        long epoch = -86_400_000L;
        Date result = OffsetDateTimeConversions.toDate(epoch, converter);
        assertEquals(epoch, result.getTime(), "Negative epoch milliseconds should be accepted.");
    }

    @Test
    void toDate_zeroEpoch() {
        Date result = OffsetDateTimeConversions.toDate(0L, converter);
        assertEquals(0L, result.getTime(), "Zero epoch milliseconds corresponds to the Unix epoch start.");
    }

    @Test
    void toDate_maxLongEpoch() {
        long max = Long.MAX_VALUE;
        Date result = OffsetDateTimeConversions.toDate(max, converter);
        assertEquals(max, result.getTime(), "Method should handle Long.MAX_VALUE without overflow.");
    }

    @Test
    void toDate_nullFrom_throwsNPE() {
        assertThrows(NullPointerException.class, () -> OffsetDateTimeConversions.toDate(null, converter), "Null 'from' argument should cause a NullPointerException.");
    }

    @Test
    void toDate_unsupportedTypeWithNullConverter_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate(new Object(), null), "Unsupported source type with a null Converter should cause an IllegalArgumentException.");
    }

    @Test
    void toDate_unsupportedTypeWithConverterReturningLong() {
        // Mock converter that returns a valid long for an unknown source object.
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any(), eq(Long.class))).thenReturn(1000L);
        Date result = OffsetDateTimeConversions.toDate(new Object(), mock);
        assertEquals(1000L, result.getTime(), "Mock converter returning 1000L should produce a Date at epoch+1s.");
    }

    @Test
    void toDate_unsupportedTypeWithConverterReturningNull_throwsIllegalArgument() {
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any(), eq(Long.class))).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate(new Object(), mock), "Converter returning null should cause IllegalArgumentException.");
    }

    @Test
    void toDate_unsupportedTypeWithConverterReturningString_throwsIllegalArgument() {
        Converter mock = Mockito.mock(Converter.class);
        Mockito.when(mock.convert(any(), eq(Long.class))).thenReturn("not-a-number");
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate(new Object(), mock), "Converter returning a non‑numeric type should cause IllegalArgumentException.");
    }

    @Test
    void toDate_unsupportedTypeWithConverterThrowingRuntimeException_propagates() {
        Converter mock = Mockito.mock(Converter.class);
        RuntimeException ex = new RuntimeException("converter failure");
        Mockito.when(mock.convert(any(), eq(Long.class))).thenThrow(ex);
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> OffsetDateTimeConversions.toDate(new Object(), mock), "RuntimeException from Converter must propagate.");
        assertSame(ex, thrown, "Propagated exception should be the original one.");
    }

    @Test
    void toDate_localDateUnsupported_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate(LocalDate.of(2021, 6, 1), converter), "LocalDate lacks time‑zone information and must cause IllegalArgumentException.");
    }

    @Test
    void toDate_nonNumericString_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate("not-a-number", converter), "Non‑numeric String cannot be parsed to a long.");
    }

    @Test
    void toDate_stringWithWhitespace_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate(" 1622548800000 ", converter), "String with surrounding whitespace is not a valid numeric representation.");
    }

    @Test
    void toDate_emptyString_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate("", converter), "Empty string cannot be parsed to a numeric epoch value.");
    }

    @Test
    void toDate_whitespaceOnlyString_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate("   ", converter), "String containing only whitespace should trigger IllegalArgumentException.");
    }

    @Test
    void toDate_stringOverflow_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate("9223372036854775808", converter), "String numeric value greater than Long.MAX_VALUE must cause IllegalArgumentException.");
    }

    @Test
    void toDate_stringUnderflow_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate("-9223372036854775809", converter), "String numeric value less than Long.MIN_VALUE must cause IllegalArgumentException.");
    }

    @Test
    void toDate_floatingPointNumber_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate(12345.67, converter), "Floating‑point numbers are not accepted as epoch milliseconds.");
    }

    @Test
    void toDate_booleanValue_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate(true, converter), "Boolean values cannot be converted to epoch milliseconds.");
    }

    @Test
    void toDate_bigDecimalWithFraction_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate(new BigDecimal("123.45"), converter), "BigDecimal with a fractional part cannot be interpreted as an integral epoch value.");
    }

    @Test
    void toDate_mapInstance_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate(new HashMap<>(), converter), "Map instances are not supported as a source for epoch conversion.");
    }

    @Test
    void toDate_atomicInteger_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toDate(new java.util.concurrent.atomic.AtomicInteger(5), converter), "AtomicInteger is not a recognized numeric type for epoch conversion.");
    }

    @Test
    void toDate_longInputDoesNotInvokeConverter() {
        // The mock should never be called because the source type is already a Long.
        Converter mock = Mockito.mock(Converter.class);
        Date result = OffsetDateTimeConversions.toDate(123L, mock);
        assertEquals(123L, result.getTime(), "Long input should be used directly without consulting the Converter.");
        Mockito.verifyNoInteractions(mock);
    }
}
