package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toDate_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    /**
     * Helper holder for parameterized test arguments.
     */
    private static class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final long expectedEpochMillis;

        final String description;

        ArgumentsHolder(Object from, Converter converter, long expectedEpochMillis, String description) {
            this.from = from;
            this.converter = converter;
            this.expectedEpochMillis = expectedEpochMillis;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Force UTC for conversions that rely on the system default zone.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
        // Restore the default time‑zone to avoid side‑effects for other test suites.
        TimeZone.setDefault(null);
    }

    /**
     * Provides simple value‑type test cases (Date, Instant, Timestamp, …).
     */
    private static List<ArgumentsHolder> simpleCases() {
        List<ArgumentsHolder> list = new ArrayList<>();
        // Direct java.util.Date input
        list.add(new ArgumentsHolder(new Date(0L), null, 0L, "Date epoch 0"));
        // java.time.Instant input
        Instant instant = Instant.ofEpochSecond(1622505600L);
        list.add(new ArgumentsHolder(instant, null, 1622505600000L, "Instant 2021‑06‑01T00:00:00Z"));
        // java.sql.Timestamp input (nanoseconds truncated)
        Timestamp ts = Timestamp.valueOf("2022-03-15 12:34:56.123456789");
        list.add(new ArgumentsHolder(ts, null, 1647340496123L, "Timestamp 2022‑03‑15T12:34:56.123Z"));
        // java.util.Calendar (UTC) input
        Calendar calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calUtc.setTimeInMillis(1609459199999L);
        list.add(new ArgumentsHolder(calUtc, null, 1609459199999L, "Calendar UTC 2020‑12‑31T23:59:59.999Z"));
        // Long epoch‑millis input
        list.add(new ArgumentsHolder(1625097600000L, null, 1625097600000L, "Long epoch‑millis 2021‑07‑01T00:00:00Z"));
        // BigDecimal epoch‑seconds (integral)
        list.add(new ArgumentsHolder(new BigDecimal("1625097600.0"), null, 1625097600000L, "BigDecimal seconds integral"));
        // java.sql.Date input (midnight UTC)
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-01-15");
        list.add(new ArgumentsHolder(sqlDate, null, 1673740800000L, "java.sql.Date 2023‑01‑15T00:00:00Z"));
        // ZonedDateTime input (America/New_York)
        ZonedDateTime zdt = ZonedDateTime.of(2021, 5, 20, 15, 30, 45, 0, ZoneId.of("America/New_York"));
        list.add(new ArgumentsHolder(zdt, null, 1621552245000L, "ZonedDateTime America/New_York"));
        // OffsetDateTime with +03:00 offset
        OffsetDateTime odt = OffsetDateTime.of(2021, 8, 10, 14, 45, 30, 0, ZoneOffset.ofHours(3));
        list.add(new ArgumentsHolder(odt, null, 1628591130000L, "OffsetDateTime +03:00"));
        // BigDecimal with fractional seconds
        list.add(new ArgumentsHolder(new BigDecimal("1625097600.123456789"), null, 1625097600123L, "BigDecimal fractional seconds"));
        // BigInteger epoch‑seconds (negative)
        list.add(new ArgumentsHolder(new BigInteger("-31536000"), null, -31536000000L, "BigInteger negative seconds"));
        // Long negative epoch‑millis
        list.add(new ArgumentsHolder(-86400000L, null, -86400000L, "Long negative epoch‑millis"));
        // Calendar with non‑UTC zone (Los Angeles) – instant must stay the same
        Calendar calLa = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        calLa.setTimeInMillis(1609459200000L);
        list.add(new ArgumentsHolder(calLa, null, 1609459200000L, "Calendar Los_Angeles"));
        // Timestamp with nanoseconds beyond milliseconds (same as earlier)
        Timestamp ts2 = Timestamp.valueOf("2022-03-15 12:34:56.123456789");
        list.add(new ArgumentsHolder(ts2, null, 1647340496123L, "Timestamp nanoseconds truncation"));
        return list;
    }

    /**
     * Test cases that require a non‑null Converter for zone resolution.
     */
    private static List<ArgumentsHolder> zoneAwareCases() {
        List<ArgumentsHolder> list = new ArrayList<>();
        // LocalDateTime assumed UTC
        LocalDateTime ldt = LocalDateTime.of(2021, 5, 20, 15, 30, 45);
        list.add(new ArgumentsHolder(ldt, converter, 1621521045000L, "LocalDateTime assumed UTC"));
        // LocalDate assumed UTC start‑of‑day
        LocalDate ld = LocalDate.of(2022, 12, 31);
        list.add(new ArgumentsHolder(ld, converter, 1672444800000L, "LocalDate start‑of‑day UTC"));
        return list;
    }

    /**
     * Test cases that must fail with IllegalArgumentException.
     */
    private static List<Object> illegalArgumentInputs() {
        return List.of(// Unsupported type UUID
        UUID.randomUUID(), // String representation of an instant
        "2021-05-20T15:30:45Z", // Integer (epoch‑millis) – unsupported numeric type
        Integer.valueOf(1625097600), // Float
        Float.valueOf(1625097600.5f), // Double
        Double.valueOf(1625097600.5), // LocalDateTime with null converter
        LocalDateTime.of(2021, 5, 20, 15, 30, 45), // LocalDate with null converter
        LocalDate.of(2022, 1, 1), // LocalTime with null converter
        LocalTime.of(12, 34, 56), // Boolean
        Boolean.TRUE, // Character
        'A');
    }

    @Test
    void toDate_givenInstant_returnsCorrespondingDate() {
        // GIVEN
        Instant now = Instant.now();
        // WHEN
        Date result = BigDecimalConversions.toDate(now, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(Date.from(now), result, "Date should represent the same instant as the input");
    }

    @Test
    void toDate_givenDate_returnsEqualDate() {
        // GIVEN
        Date original = new Date();
        // WHEN
        Date result = BigDecimalConversions.toDate(original, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        // The conversion goes through Instant, so the millisecond value must be identical.
        assertEquals(original.getTime(), result.getTime(), "Converted Date should have the same epoch millisecond as the original");
    }

    @Test
    void toDate_givenNull_throwsNullPointerException() {
        // GIVEN
        Object nullValue = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toDate(nullValue, converter), "Calling toDate with a null source should throw NPE because Date.from(null) is illegal");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("simpleCases")
    void toDate_simpleConversions(ArgumentsHolder holder) {
        Date result = BigDecimalConversions.toDate(holder.from, holder.converter);
        assertNotNull(result, "Resulting Date must not be null");
        assertEquals(holder.expectedEpochMillis, result.getTime(), "Converted Date does not match expected epoch milliseconds");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("zoneAwareCases")
    void toDate_zoneAwareConversions(ArgumentsHolder holder) {
        Date result = BigDecimalConversions.toDate(holder.from, holder.converter);
        assertNotNull(result);
        assertEquals(holder.expectedEpochMillis, result.getTime(), "Zone‑aware conversion produced wrong instant");
    }

    @ParameterizedTest(name = "{index}: IllegalArgument for {0}")
    @MethodSource("illegalArgumentInputs")
    void toDate_illegalArgument(Object source) {
        // For the two Local* cases we need to pass a null converter explicitly.
        Converter conv = (source instanceof LocalDateTime || source instanceof LocalDate || source instanceof LocalTime) ? null : converter;
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toDate(source, conv), "Expected IllegalArgumentException for input: " + source);
    }
}
