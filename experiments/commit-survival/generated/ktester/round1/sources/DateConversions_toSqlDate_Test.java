package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
class DateConversions_toSqlDate_Test {

    /**
     * Converter used by all test cases – created once for the whole suite.
     */
    private static Converter converter;

    /**
     * Shared default {@link Converter} used for the majority of test cases.
     */
    private static Converter defaultConverter;

    /**
     * Initialise the shared {@link Converter} before any test runs.
     */
    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a public no‑arg constructor (default options).
        // If the library evolves, this line can be adapted without touching the tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // default options
        ConverterOptions options = new ConverterOptions();
        defaultConverter = new Converter(options);
    }

    private static Stream<Arguments> provideVariousSourceTypes() {
        // Helper objects reused across several cases
        // 2021‑06‑01T12:34:56.789Z
        Date date = new Date(1622548800000L);
        Timestamp timestamp = Timestamp.valueOf("2021-06-01 12:34:56.789");
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2021‑01‑01T00:00:00Z
        utcCal.setTimeInMillis(1609459200000L);
        Instant instant = Instant.ofEpochMilli(1583020800123L);
        LocalDate localDate = LocalDate.of(2020, 2, 29);
        LocalDateTime localDateTime = LocalDateTime.of(2022, 12, 31, 23, 59, 59, 123_000_000);
        BigInteger bigInt = new BigInteger("1234567890123");
        BigDecimal bigDec = new BigDecimal("9876543210.0");
        AtomicLong atomicLong = new AtomicLong(5555555555L);
        String numericString = "1672531199123";
        String isoString = "2022-01-01T00:00:00Z";
        String dateOnlyString = "2021-12-31";
        String plusSignString = "+1234567890";
        // Custom converter that adds one hour to the parsed instant
        Converter addsOneHour = mock(Converter.class);
        when(addsOneHour.convert(eq("2021-07-15T12:34:56+02:00"), any())).thenReturn(// original instant + 1 hour
        1626344096000L);
        // Custom converter that returns null (simulating “no conversion”)
        Converter returnsNull = mock(Converter.class);
        when(returnsNull.convert(any(), any())).thenReturn(null);
        // Custom converter that throws a RuntimeException
        Converter throwsRuntime = mock(Converter.class);
        when(throwsRuntime.convert(any(), any())).thenThrow(new RuntimeException("custom failure"));
        return Stream.of(// null source, null converter
        Arguments.of(null, null, 0L, null, "null source & null converter → epoch 0"), // Long input
        Arguments.of(123456789L, null, 123456789L, null, "Long input → same epoch"), // java.util.Date input
        Arguments.of(date, null, 1622548800000L, null, "java.util.Date input → identical epoch"), // java.sql.Timestamp (subclass of Date)
        Arguments.of(timestamp, null, 1622548896789L, null, "Timestamp input → same millisecond value"), // Calendar (UTC)
        Arguments.of(utcCal, null, 1609459200000L, null, "UTC Calendar → timeInMillis used"), // Instant
        Arguments.of(instant, null, 1583020800123L, null, "Instant → epoch millis"), // LocalDate (midnight UTC)
        Arguments.of(localDate, null, 1582934400000L, null, "LocalDate → midnight UTC epoch"), // LocalDateTime (interpreted as UTC)
        Arguments.of(localDateTime, null, 1672531199123L, null, "LocalDateTime → UTC instant epoch"), // Numeric String
        Arguments.of(numericString, null, 1672531199123L, null, "Numeric String → parsed long"), // ISO‑8601 instant string
        Arguments.of(isoString, null, 1640995200000L, null, "ISO‑8601 string → parsed instant"), // BigInteger
        Arguments.of(bigInt, null, 1234567890123L, null, "BigInteger → longValue() used"), // BigDecimal (fractional part ignored)
        Arguments.of(bigDec, null, 9876543210L, null, "BigDecimal → longValue() used"), // AtomicLong
        Arguments.of(atomicLong, null, 5555555555L, null, "AtomicLong → current value used"), // Unsupported Object → IllegalArgumentException
        Arguments.of(new Object(), null, null, IllegalArgumentException.class, "Unsupported type → IllegalArgumentException"), // Non‑numeric String → NumberFormatException
        Arguments.of("not-a-number", null, null, NumberFormatException.class, "Non‑numeric String → NumberFormatException"), // Date‑only string
        Arguments.of(dateOnlyString, null, 1640908800000L, null, "Date‑only string → midnight UTC epoch"), // Null source, custom (non‑null) converter → epoch 0
        Arguments.of(null, defaultConverter, 0L, null, "null source with custom converter → epoch 0"), // Custom converter that adds one hour
        Arguments.of("2021-07-15T12:34:56+02:00", addsOneHour, 1626344096000L, null, "Custom converter adds one hour to instant"));
    }

    private static Stream<Arguments> provideAdditionalEdgeCases() {
        // Calendar with America/Los_Angeles timezone (PST/PDT)
        Calendar laCal = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        // arbitrary instant
        laCal.setTimeInMillis(1672500000000L);
        // Custom converter that returns null (already defined above)
        Converter returnsNull = mock(Converter.class);
        when(returnsNull.convert(any(), any())).thenReturn(null);
        // Custom converter that throws RuntimeException (already defined above)
        Converter throwsRuntime = mock(Converter.class);
        when(throwsRuntime.convert(any(), any())).thenThrow(new RuntimeException("custom failure"));
        return Stream.of(// Long.MAX_VALUE
        Arguments.of(Long.MAX_VALUE, null, Long.MAX_VALUE, null, "Long.MAX_VALUE → far‑future date"), // Long.MIN_VALUE
        Arguments.of(Long.MIN_VALUE, null, Long.MIN_VALUE, null, "Long.MIN_VALUE → far‑past date"), // Negative long
        Arguments.of(-123456789L, null, -123456789L, null, "Negative long → epoch before 1970"), // java.sql.Date
        Arguments.of(java.sql.Date.valueOf("2022-02-28"), null, 1646006400000L, null, "java.sql.Date → midnight UTC epoch"), // ZonedDateTime (America/New_York → UTC)
        Arguments.of(ZonedDateTime.of(2023, 3, 15, 10, 30, 0, 0, ZoneId.of("America/New_York")), null, 1678890600000L, null, "ZonedDateTime → instant in UTC"), // OffsetDateTime (+02:00)
        Arguments.of(OffsetDateTime.of(2022, 12, 31, 23, 0, 0, 0, ZoneOffset.ofHours(2)), null, 1672520400000L, null, "OffsetDateTime → instant UTC"), // LocalTime (unsupported) → IllegalArgumentException
        Arguments.of(LocalTime.of(12, 34, 56, 789_000_000), null, null, IllegalArgumentException.class, "LocalTime → IllegalArgumentException"), // BigDecimal with fraction (truncated)
        Arguments.of(new BigDecimal("12345.6789"), null, 12345L, null, "BigDecimal with fraction → truncated long"), // Numeric string with leading '+'
        Arguments.of("+1234567890", null, 1234567890L, null, "String with plus sign → positive long"), // Empty string → NumberFormatException
        Arguments.of("", null, null, NumberFormatException.class, "Empty string → NumberFormatException"), // Whitespace‑only string (no trimming) → NumberFormatException
        Arguments.of("   1622551199123   ", null, null, NumberFormatException.class, "Whitespace‑padded numeric string → NumberFormatException"), // Calendar with non‑UTC zone (Los Angeles)
        Arguments.of(laCal, null, 1672500000000L, null, "GregorianCalendar (America/Los_Angeles) → absolute millis"), // AtomicLong with negative value
        Arguments.of(new AtomicLong(-5555555555L), null, -5555555555L, null, "AtomicLong negative → epoch before 1970"), // Custom converter that returns null → epoch 0
        Arguments.of("2022-01-01T00:00:00+05:30", returnsNull, 0L, null, "Custom converter returns null → epoch 0"), // Custom converter that throws RuntimeException
        Arguments.of("2022-01-01T00:00:00Z", throwsRuntime, null, RuntimeException.class, "Custom converter throws RuntimeException → propagated"));
    }

    private static Stream<Arguments> provideOverflowAndParsingCases() {
        return Stream.of(// BigDecimal overflow
        Arguments.of(new BigDecimal("9223372036854775808"), null, null, ArithmeticException.class, "BigDecimal > Long.MAX_VALUE → ArithmeticException"), // BigInteger overflow
        Arguments.of(new BigInteger("9223372036854775808"), null, null, ArithmeticException.class, "BigInteger > Long.MAX_VALUE → ArithmeticException"), // Invalid month in ISO string
        Arguments.of("2021-13-01T00:00:00Z", null, null, DateTimeParseException.class, "Invalid month in ISO‑8601 string → DateTimeParseException"), // Non‑existent date (Feb 30)
        Arguments.of("2021-02-30", null, null, DateTimeParseException.class, "Invalid calendar date → DateTimeParseException"), // Invalid hour (24)
        Arguments.of("2021-02-28T24:01:00Z", null, null, DateTimeParseException.class, "Hour 24 in ISO‑8601 → DateTimeParseException"), // Boolean value → IllegalArgumentException
        Arguments.of(Boolean.TRUE, null, null, IllegalArgumentException.class, "Boolean source → IllegalArgumentException"), // Raw byte array → IllegalArgumentException
        Arguments.of(new byte[] { 1, 2, 3 }, null, null, IllegalArgumentException.class, "byte[] source → IllegalArgumentException"), // java.time.Year → IllegalArgumentException
        Arguments.of(Year.of(2022), null, null, IllegalArgumentException.class, "Year instance → IllegalArgumentException"), // java.time.MonthDay → IllegalArgumentException
        Arguments.of(MonthDay.of(2, 29), null, null, IllegalArgumentException.class, "MonthDay instance → IllegalArgumentException"), // Instant overflow (epoch millis > Long.MAX_VALUE)
        Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE / 1000 + 1), null, null, ArithmeticException.class, "Instant beyond Long.MAX_VALUE → ArithmeticException"), // LocalDate far in future (year 10000) → DateTimeException
        Arguments.of(LocalDate.of(10_000, 1, 1), null, null, DateTimeException.class, "LocalDate year 10000 → DateTimeException"));
    }

    /**
     * Verify that a {@link java.util.Date} is converted to a new {@link Date} with the same epoch time.
     */
    @Test
    void toSqlDate_givenDate_returnsEqualDate() {
        // given
        // 2022‑01‑01T00:00:00Z
        Date source = new Date(1_640_995_200_000L);
        // when
        Date result = DateConversions.toSqlDate(source, converter);
        // then
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(source.getTime(), result.getTime(), "The epoch milliseconds of the result must match the source");
        assertNotSame(source, result, "A new Date instance should be created (not the original)");
    }

    /**
     * Verify that a {@link Long} representing epoch milliseconds is converted correctly.
     */
    @Test
    void toSqlDate_givenLong_returnsDateWithSameInstant() {
        // given
        // 2022‑01‑01T00:00:00Z
        Long epochMillis = 1_640_995_200_000L;
        // when
        Date result = DateConversions.toSqlDate(epochMillis, converter);
        // then
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(epochMillis.longValue(), result.getTime(), "The Date should represent the supplied epoch milliseconds");
    }

    /**
     * Verify the library's behaviour when the source value is {@code null}.
     */
    @Test
    void toSqlDate_givenNull_returnsDateAtEpochZero() {
        // given
        Object source = null;
        // when
        Date result = DateConversions.toSqlDate(source, converter);
        // then
        assertNotNull(result, "Resulting Date must not be null even for null input");
        assertEquals(0L, result.getTime(), "When the source is null the conversion should fall back to epoch 0");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideVariousSourceTypes")
    void toSqlDate_variousSources(Object from, Converter converter, Long expectedEpoch, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> DateConversions.toSqlDate(from, converter), description);
        } else {
            Date result = DateConversions.toSqlDate(from, converter);
            assertNotNull(result, description);
            assertEquals(expectedEpoch.longValue(), result.getTime(), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideAdditionalEdgeCases")
    void toSqlDate_additionalEdgeCases(Object from, Converter converter, Long expectedEpoch, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> DateConversions.toSqlDate(from, converter), description);
        } else {
            Date result = DateConversions.toSqlDate(from, converter);
            assertNotNull(result, description);
            assertEquals(expectedEpoch.longValue(), result.getTime(), description);
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("provideOverflowAndParsingCases")
    void toSqlDate_overflowAndParsing(Object from, Converter converter, Long expectedEpoch, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> DateConversions.toSqlDate(from, converter), description);
        } else {
            Date result = DateConversions.toSqlDate(from, converter);
            assertNotNull(result, description);
            assertEquals(expectedEpoch.longValue(), result.getTime(), description);
        }
    }
}
