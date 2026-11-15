package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toDouble_Test {

    /**
     * Converter used for all tests – created once for the whole test class.
     */
    private static Converter converter;

    /**
     * Real converter used for the majority of test cases.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Force deterministic zone handling (many conversions use the system default zone)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        realConverter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that converts an {@link Instant} to the double representation used by
     * {@link InstantConversions#toDouble(Instant, Converter)} – seconds since the epoch
     * with the nanosecond fraction expressed as a decimal.
     */
    private static double instantToDouble(Instant instant) {
        return instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
    }

    /* --------------------------------------------------------------
     * Test data provider.
     * -------------------------------------------------------------- */
    private static Stream<Arguments> provideToDoubleCases() {
        // Helper objects reused across multiple cases
        // for the "Instant.now()" case
        Instant nowInstant = Instant.now();
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 15, 12, 30, 45);
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 15, 12, 30, 45, 0, ZoneOffset.ofHours(-5));
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 15, 12, 30, 45, 0, ZoneOffset.ofHours(2));
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(0L);
        return Stream.of(// 1. null for both parameters → NPE
        Arguments.of(null, null, null, NullPointerException.class, "Both parameters null should cause NullPointerException"), // 2. Instant input, real converter
        Arguments.of(nowInstant, realConverter, instantToDouble(nowInstant), null, "Instant input should be converted directly to double"), // 3. LocalDateTime → UTC instant
        Arguments.of(ldt, realConverter, instantToDouble(ldt.toInstant(ZoneOffset.UTC)), null, "LocalDateTime converted using UTC should yield correct double"), // 4. Date epoch start → 0.0
        Arguments.of(new Date(0L), realConverter, 0.0, null, "java.util.Date at epoch should produce 0.0"), // 5. Timestamp → seconds since epoch
        Arguments.of(new java.sql.Timestamp(1_600_000_000_000L), realConverter, 1_600_000_000.0, null, "Timestamp converted to seconds since epoch"), // 6. ISO_LOCAL_DATE_TIME string
        Arguments.of("2023-05-15T12:30:45", realConverter, instantToDouble(LocalDateTime.parse("2023-05-15T12:30:45").toInstant(ZoneOffset.UTC)), null, "ISO_LOCAL_DATE_TIME string parsed and converted"), // 7. ISO_LOCAL_DATE string (no time)
        Arguments.of("2023-05-15", realConverter, instantToDouble(LocalDate.parse("2023-05-15").atStartOfDay(ZoneOffset.UTC).toInstant()), null, "Date‑only string parsed to start‑of‑day instant"), // 8. Long epoch‑millis
        Arguments.of(12_345L, realConverter, 12.345, null, "Long interpreted as epoch milliseconds → seconds"), // 9. AtomicLong epoch‑millis
        Arguments.of(new AtomicLong(1_600_000_000_000L), realConverter, 1_600_000_000.0, null, "AtomicLong interpreted as epoch milliseconds → seconds"), // 10. BigDecimal with fractional millis
        Arguments.of(new BigDecimal("12345.678"), realConverter, 12.345678, null, "BigDecimal interpreted as epoch milliseconds with fraction"), // 11. LocalDate → start of day UTC
        Arguments.of(LocalDate.of(2023, 5, 15), realConverter, instantToDouble(LocalDate.of(2023, 5, 15).atStartOfDay(ZoneOffset.UTC).toInstant()), null, "LocalDate converted to start‑of‑day instant"), // 12. ZonedDateTime with -05:00 offset
        Arguments.of(zdt, realConverter, instantToDouble(zdt.toInstant()), null, "ZonedDateTime with non‑UTC offset normalized to instant"), // 13. OffsetDateTime with +02:00 offset
        Arguments.of(odt, realConverter, instantToDouble(odt.toInstant()), null, "OffsetDateTime converted to instant"), // 14. Calendar set to epoch
        Arguments.of(utcCal, realConverter, 0.0, null, "Calendar at epoch should produce 0.0"), // 15. BigInteger epoch‑millis
        Arguments.of(new BigInteger("1234567890123"), realConverter, 1_234_567_890.123, null, "BigInteger interpreted as epoch milliseconds → seconds with fraction"), // 16. Integer epoch‑millis
        Arguments.of(12_345, realConverter, 12.345, null, "Integer interpreted as epoch milliseconds → seconds"), // 17. Double epoch‑millis
        Arguments.of(12_345.678, realConverter, 12.345678, null, "Double interpreted as epoch milliseconds → seconds with fraction"), // 18. Float epoch‑millis
        Arguments.of(new Float(12_345.678f), realConverter, 12.345678, null, "Float interpreted as epoch milliseconds → seconds with fraction"), // 19. Numeric string epoch‑millis
        Arguments.of("1600000000000", realConverter, 1_600_000_000.0, null, "String containing numeric epoch‑millis parsed to seconds"), // 20. Invalid date‑time string → DateTimeParseException
        Arguments.of("invalid-date", realConverter, null, DateTimeParseException.class, "Non‑parseable date‑time string should throw DateTimeParseException"), // 21. Unsupported object type → IllegalArgumentException
        Arguments.of(new Object(), realConverter, null, IllegalArgumentException.class, "Unsupported source type should cause IllegalArgumentException"), // 22. Valid Instant but null converter → NullPointerException
        Arguments.of(Instant.now(), null, null, NullPointerException.class, "Null converter should cause NullPointerException when delegating to InstantConversions"), // 23. Null 'from' with valid converter → NullPointerException
        Arguments.of(null, realConverter, null, NullPointerException.class, "Null source argument should cause NullPointerException inside toInstant"), // 24. LocalTime (unsupported) → IllegalArgumentException
        Arguments.of(LocalTime.of(12, 30), realConverter, null, IllegalArgumentException.class, "LocalTime is not supported by toInstant, should raise IllegalArgumentException"), // 25. Long.MAX_VALUE epoch‑millis → DateTimeException (overflow)
        Arguments.of(Long.MAX_VALUE, realConverter, null, DateTimeException.class, "Epoch‑millis beyond Instant.MAX should throw DateTimeException"), // 26. Non‑numeric string → NumberFormatException
        Arguments.of("not-a-number", realConverter, null, NumberFormatException.class, "String that cannot be parsed to a number should cause NumberFormatException"), // 27. BigInteger exceeding long range → ArithmeticException
        Arguments.of(new BigInteger("9223372036854775808"), realConverter, null, ArithmeticException.class, "BigInteger larger than Long.MAX_VALUE should cause ArithmeticException"), // 28. Double.NaN → IllegalArgumentException
        Arguments.of(Double.NaN, realConverter, null, IllegalArgumentException.class, "NaN is not a valid epoch‑millisecond value, should raise IllegalArgumentException"));
    }

    @Test
    void toDouble_fromInstant_returnsCorrectDouble() {
        // GIVEN
        Instant instant = Instant.parse("2023-03-25T12:34:56.789Z");
        // WHEN
        double actual = LocalDateTimeConversions.toDouble(instant, converter);
        // THEN
        double expected = instantToDouble(instant);
        assertEquals(expected, actual, 1e-12, "Conversion from Instant should produce seconds with nanosecond fraction");
    }

    @Test
    void toDouble_fromLocalDateTime_returnsCorrectDouble_usingUTC() {
        // GIVEN
        LocalDateTime ldt = LocalDateTime.of(2020, Month.MAY, 15, 12, 30, 45, 123_000_000);
        // The conversion performed by LocalDateTimeConversions uses the system default zone.
        // To make the test deterministic we explicitly convert using UTC, which matches the
        // behaviour of the library when the default zone is UTC (the most common CI environment).
        Instant instant = ldt.toInstant(ZoneOffset.UTC);
        // WHEN
        double actual = LocalDateTimeConversions.toDouble(ldt, converter);
        // THEN
        double expected = instantToDouble(instant);
        assertEquals(expected, actual, 1e-12, "Conversion from LocalDateTime should be based on its UTC instant representation");
    }

    @Test
    void toDouble_fromDate_returnsCorrectDouble() {
        // GIVEN
        Date date = Date.from(Instant.parse("1975-10-04T16:30:00Z"));
        // WHEN
        double actual = LocalDateTimeConversions.toDouble(date, converter);
        // THEN
        double expected = instantToDouble(date.toInstant());
        assertEquals(expected, actual, 1e-12, "Conversion from java.util.Date should match its underlying instant");
    }

    @Test
    void toDouble_fromLongEpochMillis_returnsCorrectDouble() {
        // GIVEN
        // corresponds to 2020‑09‑13T12:26:40.123Z
        long epochMillis = 1_600_000_000_123L;
        Instant instant = Instant.ofEpochMilli(epochMillis);
        // WHEN
        double actual = LocalDateTimeConversions.toDouble(epochMillis, converter);
        // THEN
        double expected = instantToDouble(instant);
        assertEquals(expected, actual, 1e-12, "Conversion from a Long representing epoch milliseconds should be correct");
    }

    @Test
    void toDouble_fromNull_returnsZero() {
        // GIVEN
        Object from = null;
        // WHEN
        double actual = LocalDateTimeConversions.toDouble(from, converter);
        // THEN
        assertEquals(0.0d, actual, "Conversion of a null source should return 0.0 for primitive double");
    }

    /* --------------------------------------------------------------
     * Parameterised test covering the exhaustive JSON test matrix.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("provideToDoubleCases")
    void toDouble_variousInputs(Object from, Converter converter, Double expectedValue, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            // Expect an exception – the actual value is irrelevant.
            assertThrows(expectedException, () -> LocalDateTimeConversions.toDouble(from, converter), description);
        } else {
            double actual = LocalDateTimeConversions.toDouble(from, converter);
            assertEquals(expectedValue, actual, 1e-12, description);
        }
    }
}
