package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toBigInteger_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter CONVERTER;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter works out‑of‑the‑box for the
        // date/time types we need in the tests.
        CONVERTER = new Converter(new ConverterOptions());
        // The real Converter works for all date‑time types we need.
        CONVERTER = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // test class tidy.
        CONVERTER = null;
        // Release the static reference – not strictly necessary but keeps the
        CONVERTER = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Direct Instant instance
        Arguments.of(Instant.parse("2022-01-15T10:20:30.123Z"), Instant.parse("2022-01-15T10:20:30.123Z")), // java.util.Date
        Arguments.of(new Date(1633072800000L), Instant.ofEpochMilli(1633072800000L)), // LocalDateTime – interpreted in the system default zone
        Arguments.of(LocalDateTime.of(2021, 10, 1, 12, 30, 45), LocalDateTime.of(2021, 10, 1, 12, 30, 45).atZone(ZoneId.systemDefault()).toInstant()), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2021-10-01 12:30:45.123"), Timestamp.valueOf("2021-10-01 12:30:45.123").toInstant()), // Long epoch‑millis
        Arguments.of(Long.valueOf(1633072800123L), Instant.ofEpochMilli(1633072800123L)), // ISO‑8601 String
        Arguments.of("2021-10-01T12:30:45.123Z", Instant.parse("2021-10-01T12:30:45.123Z")), // Zero epoch millis (boundary)
        Arguments.of(Long.valueOf(0L), Instant.EPOCH), // Negative epoch millis (boundary)
        Arguments.of(Long.valueOf(-1000L), Instant.ofEpochMilli(-1000L)), // Instant.EPOCH directly
        Arguments.of(Instant.EPOCH, Instant.EPOCH), // GregorianCalendar set to UTC
        Arguments.of(gregorianCalendarUtc(2022, Calendar.JANUARY, 15, 10, 20, 30, 123), Instant.parse("2022-01-15T10:20:30.123Z")), // ZonedDateTime with explicit zone
        Arguments.of(ZonedDateTime.of(2022, 3, 10, 5, 45, 30, 500_000_000, ZoneId.of("America/New_York")), ZonedDateTime.of(2022, 3, 10, 5, 45, 30, 500_000_000, ZoneId.of("America/New_York")).toInstant()), // OffsetDateTime with offset
        Arguments.of(OffsetDateTime.of(2022, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-4)), OffsetDateTime.of(2022, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-4)).toInstant()), // AtomicLong wrapping epoch‑millis
        Arguments.of(new AtomicLong(1633072800123L), Instant.ofEpochMilli(1633072800123L)));
    }

    /**
     * Helper that creates a {@link GregorianCalendar} in UTC and sets the supplied fields.
     */
    private static GregorianCalendar gregorianCalendarUtc(int year, int month, int day, int hour, int minute, int second, int milli) {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setLenient(false);
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, milli);
        return cal;
    }

    private static Stream<Arguments> unsupportedConversionProvider() {
        return Stream.of(// Non‑ISO string
        Arguments.of("not-a-date"), // LocalTime (no date component)
        Arguments.of(LocalTime.of(14, 30, 15, 250_000_000)), // Empty map
        Arguments.of(new HashMap<>()), // LocalDate (no time component)
        Arguments.of(LocalDate.of(2022, 1, 15)), // Duration
        Arguments.of(Duration.ofHours(5)), // UUID
        Arguments.of(UUID.randomUUID()), // Integer epoch millis (unsupported numeric type)
        Arguments.of(Integer.valueOf(1633072800123)), // Empty string
        Arguments.of(""), // Whitespace‑only string
        Arguments.of("   "), // ZoneId alone
        Arguments.of(ZoneId.of("Europe/Paris")), // Period
        Arguments.of(Period.ofDays(10)));
    }

    @Test
    void toBigInteger_fromInstant_returnsSameAsInstantConversions() {
        // GIVEN
        Instant instant = Instant.ofEpochSecond(1_234_567L, 890_123_456L);
        // WHEN
        BigInteger actual = LocalDateTimeConversions.toBigInteger(instant, CONVERTER);
        // THEN
        BigInteger expected = InstantConversions.toBigInteger(instant, CONVERTER);
        assertEquals(expected, actual, "Conversion from Instant should delegate to InstantConversions");
    }

    @Test
    void toBigInteger_fromDate_returnsSameAsInstantConversions() {
        // GIVEN
        Instant instant = Instant.ofEpochSecond(9_876_543L, 210_987_654L);
        Date date = Date.from(instant);
        // WHEN
        BigInteger actual = LocalDateTimeConversions.toBigInteger(date, CONVERTER);
        // THEN
        BigInteger expected = InstantConversions.toBigInteger(instant, CONVERTER);
        assertEquals(expected, actual, "Conversion from java.util.Date should be based on its Instant value");
    }

    @Test
    void toBigInteger_fromNull_returnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        BigInteger result = LocalDateTimeConversions.toBigInteger(source, CONVERTER);
        // THEN
        assertNull(result, "Null source should result in null BigInteger");
    }

    @Test
    void toBigInteger_fromLongEpochMilli_returnsSameAsInstantConversions() {
        // GIVEN
        // arbitrary epoch milliseconds
        long epochMilli = 1_600_000_000_000L;
        // WHEN
        BigInteger actual = LocalDateTimeConversions.toBigInteger(epochMilli, CONVERTER);
        // THEN
        Instant instant = Instant.ofEpochMilli(epochMilli);
        BigInteger expected = InstantConversions.toBigInteger(instant, CONVERTER);
        assertEquals(expected, actual, "Conversion from long epoch milli should match Instant conversion");
    }

    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("validConversionProvider")
    void toBigInteger_validConversions(Object from, Instant expectedInstant) {
        // WHEN
        BigInteger actual = LocalDateTimeConversions.toBigInteger(from, CONVERTER);
        // THEN
        BigInteger expected = InstantConversions.toBigInteger(expectedInstant, CONVERTER);
        assertEquals(expected, actual, () -> "Conversion of " + from.getClass().getSimpleName() + " should delegate to InstantConversions");
    }

    @Test
    void toBigInteger_fromNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toBigInteger(null, CONVERTER), "A null source should trigger NullPointerException");
    }

    @Test
    void toBigInteger_converterNull_throwsNullPointerException() {
        Instant instant = Instant.now();
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toBigInteger(instant, null), "A null Converter should trigger NullPointerException");
    }

    @ParameterizedTest(name = "{index}: unsupported from={0}")
    @MethodSource("unsupportedConversionProvider")
    void toBigInteger_unsupportedTypes_throwIllegalArgumentException(Object from) {
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toBigInteger(from, CONVERTER), () -> "Conversion of type " + from.getClass().getSimpleName() + " must fail with IllegalArgumentException");
    }
}
