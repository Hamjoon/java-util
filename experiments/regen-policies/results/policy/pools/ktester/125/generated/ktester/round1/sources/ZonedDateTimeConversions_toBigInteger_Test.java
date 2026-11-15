package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toBigInteger_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter needs a non‑null options instance; the default constructor works for tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        // Date (epoch 0)
        Date epochDate = new Date(0L);
        // Date (negative epoch)
        Date negativeDate = new Date(-123_456_789L);
        // Instant (arbitrary)
        Instant instant12345 = Instant.ofEpochMilli(12_345L);
        // Instant (Long.MAX_VALUE)
        Instant instantMax = Instant.ofEpochMilli(Long.MAX_VALUE);
        // ZonedDateTime UTC
        ZonedDateTime zdtUtc = ZonedDateTime.of(2023, 3, 15, 12, 30, 45, 0, ZoneOffset.UTC);
        // ZonedDateTime non‑UTC (America/New_York, UTC‑5)
        ZonedDateTime zdtNy = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.of("America/New_York"));
        // OffsetDateTime negative offset
        OffsetDateTime odtNeg = OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5));
        // OffsetDateTime positive offset
        OffsetDateTime odtPos = OffsetDateTime.of(2022, 6, 30, 15, 45, 0, 0, ZoneOffset.ofHours(2));
        // Calendar UTC with explicit millis
        Calendar calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calUtc.setTimeInMillis(987_654_321L);
        // Calendar America/Los_Angeles (2023‑03‑01T12:00:00‑08:00)
        Calendar calLa = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        calLa.set(2023, Calendar.MARCH, 1, 12, 0, 0);
        calLa.set(Calendar.MILLISECOND, 0);
        // java.sql.Timestamp
        Timestamp ts = new Timestamp(5_555_555L);
        return Stream.of(// Primitive date/time types
        Arguments.of(epochDate, BigInteger.ZERO, "Date epoch 0 → BigInteger.ZERO"), Arguments.of(instant12345, BigInteger.valueOf(12_345L), "Instant.ofEpochMilli(12345) → same epoch millis as BigInteger"), Arguments.of(zdtUtc, BigInteger.valueOf(zdtUtc.toInstant().toEpochMilli()), "ZonedDateTime UTC → epoch‑millis BigInteger"), Arguments.of(odtNeg, BigInteger.valueOf(odtNeg.toInstant().toEpochMilli()), "OffsetDateTime with negative offset → epoch‑millis BigInteger"), // Calendar and SQL types
        Arguments.of(calUtc, BigInteger.valueOf(987_654_321L), "Calendar UTC with millis 987654321 → same BigInteger"), Arguments.of(ts, BigInteger.valueOf(5_555_555L), "Timestamp → epoch‑millis BigInteger"), // Additional temporal types
        Arguments.of(zdtNy, BigInteger.valueOf(zdtNy.toInstant().toEpochMilli()), "ZonedDateTime America/New_York → correct epoch‑millis"), Arguments.of(odtPos, BigInteger.valueOf(odtPos.toInstant().toEpochMilli()), "OffsetDateTime with +02:00 offset → correct epoch‑millis"), Arguments.of(instantMax, BigInteger.valueOf(Long.MAX_VALUE), "Instant at Long.MAX_VALUE epoch millis → same BigInteger"), // Calendar with non‑UTC timezone
        Arguments.of(calLa, BigInteger.valueOf(1_677_700_800_000L), "Calendar America/Los_Angeles 2023‑03‑01T12:00:00 → correct epoch‑millis"), // Negative epoch milliseconds
        Arguments.of(negativeDate, BigInteger.valueOf(-123_456_789L), "Date with negative epoch millis → same negative BigInteger"));
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        // Map missing required keys
        Map<String, String> mapMissingDate = Collections.singletonMap("TIME", "12:34:56");
        // Map with unparsable date string
        Map<String, String> mapBadDate = Map.of("DATE", "not-a-date", "TIME", "12:00:00", "ZONE", "UTC");
        return Stream.of(// Null source object
        Arguments.of(null, converter, NullPointerException.class, "null source should throw NullPointerException"), // Null converter
        Arguments.of(new Date(0L), null, NullPointerException.class, "null converter should throw NullPointerException"), // Unsupported plain String
        Arguments.of("not a date", converter, IllegalArgumentException.class, "String that cannot be parsed as date/time → IllegalArgumentException"), // Arbitrary Object
        Arguments.of(new Object(), converter, IllegalArgumentException.class, "Unsupported object type → IllegalArgumentException"), // LocalDate (no time/zone)
        Arguments.of(LocalDate.of(2021, 12, 31), converter, IllegalArgumentException.class, "LocalDate lacks zone → IllegalArgumentException"), // LocalTime (no date/zone)
        Arguments.of(LocalTime.of(14, 30, 15), converter, IllegalArgumentException.class, "LocalTime lacks date/zone → IllegalArgumentException"), // BigDecimal (non‑temporal)
        Arguments.of(new BigDecimal("12345.678"), converter, IllegalArgumentException.class, "BigDecimal is not a temporal source → IllegalArgumentException"), // BigInteger (non‑temporal)
        Arguments.of(new BigInteger("12345"), converter, IllegalArgumentException.class, "BigInteger is not a temporal source → IllegalArgumentException"), // Long (epoch millis but not supported by toInstant)
        Arguments.of(Long.valueOf(12_345L), converter, IllegalArgumentException.class, "Long epoch millis not supported → IllegalArgumentException"), // Array source
        Arguments.of(new int[] { 1, 2, 3 }, converter, IllegalArgumentException.class, "Array source is unsupported → IllegalArgumentException"), // Map missing DATE key
        Arguments.of(mapMissingDate, converter, IllegalArgumentException.class, "Map without DATE key → IllegalArgumentException"), // Map with unparsable DATE value
        Arguments.of(mapBadDate, converter, IllegalArgumentException.class, "Map with unparsable DATE string → IllegalArgumentException"));
    }

    @Test
    void toBigInteger_fromZonedDateTime_returnsExpectedValue() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 12, 34, 56, 123_456_789, ZoneId.of("UTC"));
        Instant instant = zdt.toInstant();
        // WHEN
        BigInteger result = ZonedDateTimeConversions.toBigInteger(zdt, converter);
        // THEN
        // The conversion delegates to InstantConversions.toBigInteger, so we compute the expected value the same way.
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, result, "BigInteger conversion of ZonedDateTime should match Instant conversion");
    }

    @Test
    void toBigInteger_fromInstant_returnsSameAsInstantConversion() {
        // GIVEN
        Instant instant = Instant.now();
        // WHEN
        BigInteger result = ZonedDateTimeConversions.toBigInteger(instant, converter);
        // THEN
        BigInteger expected = InstantConversions.toBigInteger(instant, converter);
        assertEquals(expected, result);
    }

    @Test
    void toBigInteger_nullInput_returnsNull() {
        // GIVEN
        Object nullObj = null;
        // WHEN
        BigInteger result = ZonedDateTimeConversions.toBigInteger(nullObj, converter);
        // THEN
        assertNull(result, "Conversion of null should return null");
    }

    /* --------------------------------------------------------------
     *  Successful conversion tests (parameterized)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideSuccessfulConversions")
    void toBigInteger_successfulConversions(Object from, BigInteger expected, String description) {
        // WHEN
        BigInteger result = ZonedDateTimeConversions.toBigInteger(from, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Exception / edge‑case tests (parameterized)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideExceptionScenarios")
    void toBigInteger_exceptionScenarios(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException, () -> ZonedDateTimeConversions.toBigInteger(from, conv), description);
    }
}
