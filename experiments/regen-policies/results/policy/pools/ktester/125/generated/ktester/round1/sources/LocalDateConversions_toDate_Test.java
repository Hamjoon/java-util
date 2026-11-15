package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toDate_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long test suites.
        converter = null;
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (parameterised)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> successfulCases() {
        // Helper objects that are reused across several cases
        // 2021‑06‑01T12:00:00Z
        Date dateFromLong = new Date(1_622_548_800_000L);
        Timestamp ts = Timestamp.valueOf("2021-06-01 12:00:00");
        Instant instant = Instant.ofEpochMilli(1_622_548_800_000L);
        LocalDate localDate = LocalDate.of(2021, 6, 1);
        long expectedLocalDateUtc = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        LocalDateTime ldt = LocalDateTime.of(2021, 6, 1, 12, 0, 0);
        long expectedLdtUtc = ldt.toInstant(ZoneOffset.UTC);
        Calendar calendarUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendarUtc.setTimeInMillis(1_622_548_800_000L);
        String isoString = "2021-06-01T12:00:00Z";
        return Stream.of(// 1. Long literal
        Arguments.of(1622548800000L, 1622548800000L, "Long value is interpreted directly as milliseconds since epoch."), // 2. Integer (widened to long)
        Arguments.of(Integer.valueOf(1622548800), 1622548800L, "Integer is widened to long and used as milliseconds."), // 3. java.util.Date (already a Date)
        Arguments.of(dateFromLong, dateFromLong.getTime(), "Date input should be converted to a Date with the identical epoch milliseconds."), // 4. java.sql.Timestamp
        Arguments.of(ts, ts.getTime(), "Timestamp is converted to its millisecond value."), // 5. java.time.Instant
        Arguments.of(instant, instant.toEpochMilli(), "Instant is converted using its epoch milli."), // 6. java.time.LocalDate (UTC start‑of‑day)
        Arguments.of(localDate, expectedLocalDateUtc, "LocalDate is interpreted as start‑of‑day at UTC."), // 7. java.time.LocalDateTime (UTC)
        Arguments.of(ldt, expectedLdtUtc, "LocalDateTime is interpreted as UTC‑based date‑time."), // 8. Calendar (UTC)
        Arguments.of(calendarUtc, calendarUtc.getTimeInMillis(), "Calendar’s millisecond value is used."), // 9. Numeric String
        Arguments.of("1622548800000", 1622548800000L, "Numeric String is parsed as a long representing milliseconds."), // 10. ISO‑8601 instant string
        Arguments.of(isoString, 1622548800000L, "ISO‑8601 instant string is parsed via DateTimeFormatter.ISO_INSTANT."), // 11. BigDecimal
        Arguments.of(new BigDecimal("1622548800000"), 1622548800000L, "BigDecimal is converted to long milliseconds."), // 12. BigInteger
        Arguments.of(new BigInteger("1622548800000"), 1622548800000L, "BigInteger is converted to long milliseconds."), // 13. AtomicLong
        Arguments.of(new AtomicLong(1622548800000L), 1622548800000L, "AtomicLong’s value is used as milliseconds."));
    }

    /* --------------------------------------------------------------
     *  Error handling test cases (parameterised)
     * -------------------------------------------------------------- */
    private static Stream<Arguments> errorCases() {
        return Stream.of(// 1. Null source with null converter -> NPE
        Arguments.of(null, null, NullPointerException.class, "Passing a null source with a null converter should throw NPE."), // 2. Unsupported Boolean type
        Arguments.of(Boolean.TRUE, null, IllegalArgumentException.class, "Unsupported Boolean type should cause IllegalArgumentException."), // 3. Non‑numeric String
        Arguments.of("not-a-number", null, NumberFormatException.class, "String that cannot be parsed as a number should raise NumberFormatException."), // 4. Invalid ISO date string
        Arguments.of("2021-13-01", null, DateTimeParseException.class, "Invalid date string format should cause DateTimeParseException."), // 5. Arbitrary Object
        Arguments.of(new Object(), null, IllegalArgumentException.class, "Arbitrary object not recognized should trigger IllegalArgumentException."), // 6. Number larger than Long.MAX_VALUE
        Arguments.of("9223372036854775808", null, NumberFormatException.class, "String > Long.MAX_VALUE should cause NumberFormatException."), // 7. Short (not supported)
        Arguments.of(Short.valueOf((short) 123), null, IllegalArgumentException.class, "Short is not among supported numeric types; expect IllegalArgumentException."), // 8. ZonedDateTime (unsupported)
        Arguments.of(ZonedDateTime.now(ZoneOffset.UTC), null, IllegalArgumentException.class, "ZonedDateTime is not handled; expect IllegalArgumentException."), // 9. Float (unsupported)
        Arguments.of(Float.valueOf(1.23f), null, IllegalArgumentException.class, "Floating‑point Float is not supported; expect IllegalArgumentException."), //10. AtomicInteger (unsupported)
        Arguments.of(new AtomicInteger(5), null, IllegalArgumentException.class, "AtomicInteger is not a supported atomic type; expect IllegalArgumentException."));
    }

    /**
     * When the source object is {@code null}, the {@link Converter} returns {@code 0}
     * for numeric primitives. Consequently {@code toDate} should produce a {@link Date}
     * representing the epoch (1970‑01‑01T00:00:00Z).
     */
    @Test
    void toDate_nullInput_returnsEpochDate() {
        Date result = LocalDateConversions.toDate(null, converter);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(0L, result.getTime(), "Null input should be converted to epoch (0 ms)");
    }

    /**
     * Passing a {@link Date} instance should yield a new {@link Date} with the same epoch value.
     */
    @Test
    void toDate_givenDate_returnsSameInstant() {
        // 2021‑07‑01T00:00:00Z
        Date source = new Date(1_622_505_600_000L);
        Date result = LocalDateConversions.toDate(source, converter);
        assertNotNull(result);
        assertEquals(source.getTime(), result.getTime(), "Date input should be converted to a Date with the identical epoch milliseconds");
    }

    /**
     * A {@link Long} (or {@code long}) representing epoch milliseconds should be converted directly.
     */
    @Test
    void toDate_givenLong_returnsCorrespondingDate() {
        // 2021‑07‑01T00:00:00Z
        long epochMillis = 1_622_505_600_000L;
        Date result = LocalDateConversions.toDate(Long.valueOf(epochMillis), converter);
        assertNotNull(result);
        assertEquals(epochMillis, result.getTime(), "Long input should be converted to a Date with the same epoch milliseconds");
    }

    /**
     * A numeric {@link String} should be parsed to a {@code long} and then to a {@link Date}.
     */
    @Test
    void toDate_givenNumericString_returnsCorrespondingDate() {
        // 2021‑06‑01T00:00:00Z
        String source = "1622505600000";
        Date result = LocalDateConversions.toDate(source, converter);
        assertNotNull(result);
        assertEquals(1_622_505_600_000L, result.getTime(), "Numeric String should be parsed and converted to the correct Date");
    }

    /**
     * A {@link java.time.LocalDate} should be converted to the start of that day in the
     * system default time‑zone (the conversion performed by {@code Converter}).
     * The test verifies that the resulting {@link Date} represents the same instant.
     */
    @Test
    void toDate_givenLocalDate_returnsStartOfDayDate() {
        java.time.LocalDate localDate = java.time.LocalDate.of(2021, 6, 1);
        // Expected epoch millis: start of the day in the default zone.
        long expected = localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        Date result = LocalDateConversions.toDate(localDate, converter);
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "LocalDate should be converted to a Date representing the start of that day in the default zone");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulCases")
    void toDate_successfulConversions(Object from, long expectedEpochMillis, String description) {
        Date result = LocalDateConversions.toDate(from, converter);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(expectedEpochMillis, result.getTime(), () -> "Conversion failed for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("errorCases")
    void toDate_errorHandling(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // For the error scenarios the specification requires a null converter.
        // will be null for all cases above
        Converter converterToUse = conv;
        assertThrows(expectedException, () -> LocalDateConversions.toDate(from, converterToUse), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }

    /* --------------------------------------------------------------
     *  Additional explicit tests for null‑converter behaviour
     * -------------------------------------------------------------- */
    @Test
    void toDate_nullSource_withValidConverter_returnsEpochDate() {
        // This mirrors the behaviour already exercised in the framework’s first test.
        Date result = LocalDateConversions.toDate(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "When source is null and a valid Converter is supplied, the result should be the epoch date.");
    }
}
