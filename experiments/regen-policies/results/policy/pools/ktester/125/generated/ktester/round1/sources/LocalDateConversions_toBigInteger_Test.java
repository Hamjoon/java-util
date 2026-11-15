package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toBigInteger_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default options – they contain the built‑in conversion map.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up for the real Converter instance.
        converter = null;
    }

    /**
     * Helper that delegates to the public Converter API to obtain the
     *  expected {@code BigInteger} value for a given source object.
     */
    private BigInteger expected(Object source) {
        // The Converter returns {@code null} for a null source when the target
        // type is an object (BigInteger).  This matches the contract of the
        // method under test.
        return converter.convert(source, BigInteger.class);
    }

    private static Stream<Arguments> successfulInstantSources() {
        // Date at epoch (0 ms)
        Date dateEpoch = new Date(0L);
        // Instant with a known epoch second
        Instant instant = Instant.ofEpochSecond(123_456_789L);
        // LocalDate → midnight UTC
        // 86400 seconds after epoch
        LocalDate localDate = LocalDate.of(1970, 1, 2);
        // LocalDateTime interpreted in the system default zone
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 12, 30, 45);
        // ZonedDateTime in UTC
        ZonedDateTime zdtUtc = ZonedDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneId.of("UTC"));
        // OffsetDateTime with a -5 hour offset (should become 2021‑06‑01T13:00:00Z)
        OffsetDateTime odtMinus5 = OffsetDateTime.of(2021, 6, 1, 8, 0, 0, 0, ZoneOffset.ofHours(-5));
        // Calendar set to 5 000 ms after epoch in UTC
        Calendar calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calUtc.setTimeInMillis(5_000L);
        // Long representing epoch‑millis
        Long longMillis = 9_876_543_210L;
        // Numeric String representing epoch‑millis
        String numericString = "1609459200000";
        return Stream.of(Arguments.of(dateEpoch, "java.util.Date at epoch (0 ms)"), Arguments.of(instant, "java.time.Instant"), Arguments.of(localDate, "java.time.LocalDate (midnight UTC)"), Arguments.of(ldt, "java.time.LocalDateTime (system default zone)"), Arguments.of(zdtUtc, "java.time.ZonedDateTime (UTC)"), Arguments.of(odtMinus5, "java.time.OffsetDateTime (offset -5)"), Arguments.of(calUtc, "java.util.Calendar (UTC)"), Arguments.of(longMillis, "Long epoch‑millis"), Arguments.of(numericString, "String numeric epoch‑millis"));
    }

    private static Stream<Arguments> additionalSuccessfulSources() {
        // ZonedDateTime in America/New_York (UTC‑5 on 2022‑12‑31)
        ZonedDateTime zdtNY = ZonedDateTime.of(2022, 12, 31, 23, 0, 0, 0, // corresponds to 2023‑01‑01T04:00:00Z
        ZoneId.of("America/New_York"));
        // OffsetDateTime at UTC
        OffsetDateTime odtUtc = OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        // java.sql.Timestamp (sub‑second precision)
        Timestamp timestamp = Timestamp.valueOf("1970-01-01 00:00:01.234");
        // java.sql.Date (date only, epoch‑millis = 86 400 000)
        java.sql.Date sqlDate = new java.sql.Date(86_400_000L);
        // Calendar in America/Los_Angeles set to its default fields (1970‑01‑01 00:00:00)
        Calendar calLA = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        calLA.set(1970, Calendar.JANUARY, 1, 0, 0, 0);
        calLA.set(Calendar.MILLISECOND, 0);
        // AtomicLong
        AtomicLong atomicLong = new AtomicLong(5_555_555L);
        // BigDecimal with whole number
        BigDecimal bigDecimalWhole = new BigDecimal("1234567890123");
        // String with leading/trailing whitespace
        String paddedString = "   1609459200000   ";
        return Stream.of(Arguments.of(zdtNY, "ZonedDateTime (America/New_York)"), Arguments.of(odtUtc, "OffsetDateTime (UTC)"), Arguments.of(timestamp, "java.sql.Timestamp"), Arguments.of(sqlDate, "java.sql.Date"), Arguments.of(calLA, "Calendar (America/Los_Angeles)"), Arguments.of(atomicLong, "AtomicLong"), Arguments.of(bigDecimalWhole, "BigDecimal (whole number)"), Arguments.of(paddedString, "String with surrounding whitespace"));
    }

    private static Stream<Arguments> errorCases() {
        // Unsupported object type
        Object unsupported = new Object();
        // Non‑numeric string
        String notANumber = "not-a-number";
        // Empty string
        String empty = "";
        // Malformed numeric string
        String malformed = "123abc456";
        // LocalTime (no date/zone)
        LocalTime localTime = LocalTime.of(12, 34, 56);
        // Whitespace‑only string
        String whitespaceOnly = "   ";
        // Decimal string (fractional part)
        String decimalString = "12345.67";
        // BigDecimal with fractional part
        BigDecimal bigDecimalFraction = new BigDecimal("12345.678");
        // Double value
        Double doubleValue = Double.valueOf(98_765.432);
        // Float value
        Float floatValue = Float.valueOf(12_345.0f);
        // AtomicInteger (unsupported)
        AtomicInteger atomicInt = new AtomicInteger(777_777);
        // Instant that overflows a long millisecond count
        Instant overflowInstant = Instant.ofEpochSecond(Long.MAX_VALUE, 999_999_999L);
        return Stream.of(Arguments.of(unsupported, IllegalArgumentException.class, "unsupported source type (Object)"), Arguments.of(notANumber, NumberFormatException.class, "non‑numeric String"), Arguments.of(empty, NumberFormatException.class, "empty String"), Arguments.of(malformed, NumberFormatException.class, "malformed numeric String"), Arguments.of(localTime, IllegalArgumentException.class, "LocalTime (no date/zone)"), Arguments.of(whitespaceOnly, NumberFormatException.class, "whitespace‑only String"), Arguments.of(decimalString, NumberFormatException.class, "decimal String (fractional)"), Arguments.of(bigDecimalFraction, ArithmeticException.class, "BigDecimal with fractional part"), Arguments.of(doubleValue, IllegalArgumentException.class, "Double value (unsupported)"), Arguments.of(floatValue, IllegalArgumentException.class, "Float value (unsupported)"), Arguments.of(atomicInt, IllegalArgumentException.class, "AtomicInteger (unsupported)"), Arguments.of(overflowInstant, DateTimeException.class, "Instant overflow beyond epoch‑millis range"));
    }

    @Test
    void toBigInteger_fromInstant() {
        Instant instant = Instant.ofEpochSecond(1_600_000_000L, 123_456_789);
        BigInteger actual = LocalDateConversions.toBigInteger(instant, converter);
        assertEquals(expected(instant), actual, "Conversion from Instant should match Converter's direct conversion");
    }

    @Test
    void toBigInteger_fromLocalDate() {
        LocalDate date = LocalDate.of(2020, 5, 15);
        BigInteger actual = LocalDateConversions.toBigInteger(date, converter);
        assertEquals(expected(date), actual, "Conversion from LocalDate should match Converter's direct conversion");
    }

    @Test
    void toBigInteger_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2020, 5, 15, 12, 34, 56, 789_000_000);
        BigInteger actual = LocalDateConversions.toBigInteger(ldt, converter);
        assertEquals(expected(ldt), actual, "Conversion from LocalDateTime should match Converter's direct conversion");
    }

    @Test
    void toBigInteger_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2020, 5, 15, 12, 34, 56, 789_000_000, ZoneId.of("America/New_York"));
        BigInteger actual = LocalDateConversions.toBigInteger(zdt, converter);
        assertEquals(expected(zdt), actual, "Conversion from ZonedDateTime should match Converter's direct conversion");
    }

    @Test
    void toBigInteger_fromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2020, 5, 15, 12, 34, 56, 789_000_000, ZoneOffset.ofHours(-4));
        BigInteger actual = LocalDateConversions.toBigInteger(odt, converter);
        assertEquals(expected(odt), actual, "Conversion from OffsetDateTime should match Converter's direct conversion");
    }

    @Test
    void toBigInteger_fromDate() {
        Date date = Date.from(Instant.now());
        BigInteger actual = LocalDateConversions.toBigInteger(date, converter);
        assertEquals(expected(date), actual, "Conversion from java.util.Date should match Converter's direct conversion");
    }

    @Test
    void toBigInteger_fromCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(Instant.now().toEpochMilli());
        BigInteger actual = LocalDateConversions.toBigInteger(cal, converter);
        assertEquals(expected(cal), actual, "Conversion from Calendar should match Converter's direct conversion");
    }

    @Test
    void toBigInteger_fromLong() {
        Long epochMilli = 1_600_000_000_123L;
        BigInteger actual = LocalDateConversions.toBigInteger(epochMilli, converter);
        assertEquals(expected(epochMilli), actual, "Conversion from a Long (epoch milliseconds) should match Converter's direct conversion");
    }

    @Test
    void toBigInteger_fromStringRepresentingNumber() {
        String number = "12345678901234567890";
        BigInteger actual = LocalDateConversions.toBigInteger(number, converter);
        assertEquals(expected(number), actual, "Conversion from numeric String should match Converter's direct conversion");
    }

    @Test
    void toBigInteger_fromNull() {
        assertNull(LocalDateConversions.toBigInteger(null, converter), "Conversion of null should return null for object target types");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("successfulInstantSources")
    void toBigInteger_successfulConversions(Object from, String description) {
        BigInteger actual = LocalDateConversions.toBigInteger(from, converter);
        assertEquals(expected(from), actual, () -> "Conversion from " + description + " should match Converter's direct conversion");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("additionalSuccessfulSources")
    void toBigInteger_additionalSuccesses(Object from, String description) {
        BigInteger actual = LocalDateConversions.toBigInteger(from, converter);
        assertEquals(expected(from), actual, () -> "Conversion from " + description + " should match Converter's direct conversion");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("errorCases")
    void toBigInteger_errorScenarios(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> LocalDateConversions.toBigInteger(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for " + description);
    }

    /**
     * Verify that passing a {@code null} {@link Converter} results in an NPE.
     */
    @Test
    void toBigInteger_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toBigInteger("12345", null), "Passing a null Converter should raise NullPointerException");
    }

    /**
     * The original test suite expects a {@code null} result when the source is {@code null}.
     */
    @Test
    void toBigInteger_fromNull_returnsNull() {
        assertNull(LocalDateConversions.toBigInteger(null, converter), "Conversion of null should return null for object target types");
    }
}
