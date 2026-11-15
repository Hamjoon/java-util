package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toLocalDateTime_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter has a public constructor that requires a ConverterOptions instance.
        // An empty options object gives us the default conversion behaviour.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to clean up, but we clear the reference for good measure.
        converter = null;
    }

    /**
     * Supplies the successful test data.
     */
    private static Stream<Arguments> provideValidConversions() {
        ZoneId utc = ZoneId.of("UTC");
        ZoneId system = ZoneId.systemDefault();
        // 1. ZonedDateTime (UTC) – zone stripped
        ZonedDateTime zdtUtc = ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 123_000_000, utc);
        LocalDateTime ldtFromZdt = zdtUtc.toLocalDateTime();
        // 2. LocalDateTime – identity
        LocalDateTime ldt = LocalDateTime.of(2022, 12, 31, 23, 59, 59, 999_000_000);
        // 3. java.util.Date (epoch millis) – interpreted as UTC
        Date date = new Date(Instant.parse("2021-01-01T10:20:30.400Z").toEpochMilli());
        LocalDateTime ldtFromDate = LocalDateTime.ofInstant(date.toInstant(), utc);
        // 4. java.sql.Timestamp – nanoseconds preserved
        Timestamp ts = Timestamp.valueOf("2020-06-15 08:05:06.789");
        LocalDateTime ldtFromTs = ts.toLocalDateTime();
        // 5. Instant (UTC) – same instant in UTC
        Instant instant = Instant.parse("2019-03-25T12:00:00Z");
        LocalDateTime ldtFromInstant = LocalDateTime.ofInstant(instant, utc);
        // 6. Calendar (UTC) – matching instant
        Calendar calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calUtc.setTimeInMillis(Instant.parse("2022-02-28T14:15:16.250Z").toEpochMilli());
        LocalDateTime ldtFromCalUtc = LocalDateTime.ofInstant(Instant.ofEpochMilli(calUtc.getTimeInMillis()), utc);
        // 7. LocalDate – start of day
        LocalDate ld = LocalDate.of(2021, 5, 4);
        LocalDateTime ldtFromLocalDate = ld.atStartOfDay();
        // 8. OffsetDateTime – offset applied, zone stripped
        OffsetDateTime odt = OffsetDateTime.of(2022, 11, 30, 9, 45, 12, 345_000_000, ZoneOffset.ofHours(-5));
        LocalDateTime ldtFromOffset = odt.toLocalDateTime();
        // 9. BigInteger (epoch seconds) – 0 -> 1970‑01‑01T00:00
        BigInteger biEpochSecZero = BigInteger.valueOf(0);
        LocalDateTime ldtFromBiSecZero = LocalDateTime.ofInstant(Instant.ofEpochSecond(0), utc);
        // 10. BigInteger (epoch seconds) – typical date
        BigInteger biEpochSecTypical = BigInteger.valueOf(1_640_995_200L);
        LocalDateTime ldtFromBiSecTypical = LocalDateTime.ofInstant(Instant.ofEpochSecond(1_640_995_200L), utc);
        // 11. BigInteger (epoch milliseconds) – fraction preserved
        BigInteger biEpochMilli = BigInteger.valueOf(1_640_995_200_123L);
        LocalDateTime ldtFromBiMilli = LocalDateTime.ofInstant(Instant.ofEpochMilli(1_640_995_200_123L), utc);
        // 12. java.sql.Date – midnight of that date
        java.sql.Date sqlDate = java.sql.Date.valueOf("1999-12-31");
        LocalDateTime ldtFromSqlDate = sqlDate.toLocalDate().atStartOfDay();
        // 13. Calendar (America/New_York) – convert to UTC equivalent
        Calendar calNy = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        calNy.set(2022, Calendar.MARCH, 14, 2, 30, 0);
        calNy.set(Calendar.MILLISECOND, 0);
        // The Calendar now represents 2022‑03‑14T06:30:00Z (NY is UTC‑4 at that date)
        LocalDateTime ldtFromCalNy = LocalDateTime.ofInstant(Instant.ofEpochMilli(calNy.getTimeInMillis()), utc);
        // 14. Negative epoch Date (pre‑1970)
        // 1969‑12‑31T00:00:00Z
        Date negativeDate = new Date(-86_400_000L);
        LocalDateTime ldtFromNegDate = LocalDateTime.ofInstant(negativeDate.toInstant(), utc);
        return Stream.of(arguments(zdtUtc, ldtFromZdt, "ZonedDateTime (UTC) → LocalDateTime (zone stripped)"), arguments(ldt, ldt, "LocalDateTime → identity"), arguments(date, ldtFromDate, "java.util.Date (epoch millis) → LocalDateTime (UTC)"), arguments(ts, ldtFromTs, "java.sql.Timestamp → LocalDateTime (nanoseconds preserved)"), arguments(instant, ldtFromInstant, "Instant (UTC) → LocalDateTime (UTC)"), arguments(calUtc, ldtFromCalUtc, "Calendar (UTC) → matching LocalDateTime"), arguments(ld, ldtFromLocalDate, "LocalDate → start‑of‑day LocalDateTime"), arguments(odt, ldtFromOffset, "OffsetDateTime → LocalDateTime (offset applied)"), arguments(biEpochSecZero, ldtFromBiSecZero, "BigInteger epoch seconds = 0 → 1970‑01‑01T00:00"), arguments(biEpochSecTypical, ldtFromBiSecTypical, "BigInteger epoch seconds → typical date"), arguments(biEpochMilli, ldtFromBiMilli, "BigInteger epoch milliseconds → fraction preserved"), arguments(sqlDate, ldtFromSqlDate, "java.sql.Date → midnight LocalDateTime"), arguments(calNy, ldtFromCalNy, "Calendar (America/New_York) → UTC equivalent LocalDateTime"), arguments(negativeDate, ldtFromNegDate, "Negative epoch Date (pre‑1970) → LocalDateTime"));
    }

    /**
     * Supplies the exception‑related test data.
     */
    private static Stream<Arguments> provideExceptionCases() {
        // 1. Null source → NPE
        // 2. Unsupported type (String) → IllegalArgumentException
        // 3. LocalTime alone → IllegalArgumentException
        // 4. Null converter → NPE
        // 5. Unsupported source type (Integer) → IllegalArgumentException
        // 6. BigInteger overflow (epoch seconds) → IllegalArgumentException
        // 7. Instant out of range → DateTimeException
        // Helper: a dummy converter (real one works for supported types)
        Converter dummy = converter;
        // 6. BigInteger overflow: Long.MAX_VALUE + 1 seconds
        BigInteger overflowBi = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        // 7. Instant out of range
        // triggers DateTimeException
        Instant outOfRangeInstant = Instant.ofEpochSecond(Long.MAX_VALUE).plusSeconds(1);
        return Stream.of(arguments(null, dummy, NullPointerException.class, "null source"), arguments("2022-01-01T12:00:00", dummy, IllegalArgumentException.class, "unsupported String source"), arguments(LocalTime.of(13, 45, 30), dummy, IllegalArgumentException.class, "LocalTime without date"), arguments(ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneId.of("UTC")), null, NullPointerException.class, "null converter"), arguments(Integer.valueOf(42), dummy, IllegalArgumentException.class, "unsupported Integer source"), arguments(overflowBi, dummy, IllegalArgumentException.class, "BigInteger overflow (epoch seconds)"), arguments(outOfRangeInstant, dummy, DateTimeException.class, "Instant out of range"));
    }

    /**
     * Verify that a {@link LocalDateTime} source is returned unchanged.
     */
    @Test
    void shouldReturnSameLocalDateTimeWhenSourceIsLocalDateTime() {
        // given
        LocalDateTime source = LocalDateTime.of(2022, Month.MAY, 4, 12, 30, 15, 123_456_789);
        // when
        LocalDateTime result = BigIntegerConversions.toLocalDateTime(source, converter);
        // then
        assertEquals(source, result, "Conversion of a LocalDateTime should be identity");
    }

    /**
     * Verify conversion of a {@link BigInteger} representing epoch milliseconds.
     * The expected {@link LocalDateTime} is derived from the same epoch value using
     * the system default time‑zone (the conversion logic in {@code toZonedDateTime}
     * follows the same rule).
     */
    @Test
    void shouldConvertBigIntegerEpochMillisToLocalDateTime() {
        // given
        // 2022‑01‑01T00:00:00Z
        long epochMillis = 1_640_995_200_000L;
        BigInteger source = BigInteger.valueOf(epochMillis);
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), zone);
        // when
        LocalDateTime result = BigIntegerConversions.toLocalDateTime(source, converter);
        // then
        assertEquals(expected, result, "BigInteger representing epoch milliseconds should be converted to the correct LocalDateTime");
    }

    /**
     * Verify conversion of a {@link Map} that contains the keys used by the
     * {@code Converter} for date‑time values (\"time\" and optional \"nanos\").
     */
    @Test
    void shouldConvertMapWithTimeAndNanosToLocalDateTime() {
        // given
        // 2022‑01‑01T00:00:00Z
        long epochMillis = 1_640_995_200_000L;
        int nanos = 123_456_789;
        Map<String, Object> source = Map.of("time", epochMillis, "nanos", nanos);
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = Instant.ofEpochMilli(epochMillis).plusNanos(nanos);
        LocalDateTime expected = LocalDateTime.ofInstant(instant, zone);
        // when
        LocalDateTime result = BigIntegerConversions.toLocalDateTime(source, converter);
        // then
        assertEquals(expected, result, "Map containing time and nanos should be converted to the correct LocalDateTime");
    }

    /**
     * Ensure that passing {@code null} as the source value results in a {@code NullPointerException}
     * (the implementation delegates to {@code toZonedDateTime} which returns {@code null} for a null
     * source, and the subsequent {@code .toLocalDateTime()} call throws NPE).
     */
    @Test
    void shouldThrowNpeWhenSourceIsNull() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toLocalDateTime(source, converter), "Calling toLocalDateTime with a null source should throw NullPointerException");
    }

    /**
     * Successful conversions from a variety of source objects to {@link LocalDateTime}.
     *
     * Each argument consists of:
     *   – the source object to be converted,
     *   – the expected {@link LocalDateTime} result,
     *   – a short description (used only for readability of the test data).
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideValidConversions")
    void shouldConvertVariousSourcesToLocalDateTime(Object source, LocalDateTime expected, String description) {
        LocalDateTime result = BigIntegerConversions.toLocalDateTime(source, converter);
        assertEquals(expected, result, () -> "Conversion failed for: " + description);
    }

    /**
     * Tests that invalid inputs result in the expected exception type.
     *
     * Each argument consists of:
     *   – the source object,
     *   – the {@link Converter} to use (may be {@code null}),
     *   – the expected exception class,
     *   – a description.
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideExceptionCases")
    void shouldThrowExpectedException(Object source, Converter conv, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigIntegerConversions.toLocalDateTime(source, conv), () -> "Expected " + expectedException.getSimpleName() + " for: " + description);
    }
}
