package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

class CalendarConversions_toLocalDate_Test {

    /**
     * Shared {@code Converter} instance used by all test methods.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special configuration is required for the
        // conversions exercised in these tests.
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// ZonedDateTime → LocalDate
        Arguments.of(ZonedDateTime.parse("2023-04-05T14:30:00+02:00[Europe/Paris]"), LocalDate.of(2023, 4, 5), "ZonedDateTime input should be converted to its LocalDate component (2023‑04‑05)."), // LocalDateTime → LocalDate
        Arguments.of(LocalDateTime.of(2022, 12, 31, 23, 59, 59), LocalDate.of(2022, 12, 31), "LocalDateTime input should be converted to the same date part (2022‑12‑31)."), // Instant → LocalDate (UTC)
        Arguments.of(// 2023‑01‑01T00:59:59Z
        Instant.ofEpochSecond(1672531199L), LocalDate.of(2023, 1, 1), "Instant input (UTC) should be converted to the corresponding LocalDate (2023‑01‑01)."), // java.util.Date → LocalDate (UTC)
        Arguments.of(Date.from(Instant.parse("2021-07-20T10:15:30Z")), LocalDate.of(2021, 7, 20), "java.util.Date should be interpreted as an instant in the default zone and yield the correct LocalDate."), // java.sql.Timestamp → LocalDate
        Arguments.of(Timestamp.valueOf("2020-02-29 12:00:00"), LocalDate.of(2020, 2, 29), "java.sql.Timestamp should be converted to its date component (leap‑year day)."), // Calendar (UTC) → current UTC date
        Arguments.of(Calendar.getInstance(TimeZone.getTimeZone("UTC")), LocalDate.now(ZoneOffset.UTC), "Calendar instance (UTC) should be converted to the current UTC date at the moment of test execution."), // Long epoch‑millis → LocalDate
        Arguments.of(// 2023‑03‑01T00:00:00Z
        Long.valueOf(1677628800000L), LocalDate.of(2023, 3, 1), "Long epoch‑millisecond value should be interpreted as an instant in the default zone and yield the correct LocalDate."), // ISO‑8601 date string → LocalDate
        Arguments.of("2022-11-15", LocalDate.of(2022, 11, 15), "ISO‑8601 date string should be parsed and converted to the matching LocalDate."), // LocalDate → unchanged
        Arguments.of(LocalDate.of(2019, 5, 10), LocalDate.of(2019, 5, 10), "LocalDate input should be returned unchanged."));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// Null input – should return null (handled separately above)
        // Boolean → IllegalArgumentException
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "Unsupported type Boolean should cause an IllegalArgumentException."), // Malformed string → DateTimeParseException
        Arguments.of("not-a-date", DateTimeParseException.class, "Malformed date string cannot be parsed and should throw a DateTimeParseException."), // Arbitrary Object → IllegalArgumentException
        Arguments.of(new Object(), IllegalArgumentException.class, "Arbitrary object that has no conversion rule should result in an IllegalArgumentException."), // LocalTime → IllegalArgumentException
        Arguments.of(LocalTime.of(12, 34, 56), IllegalArgumentException.class, "LocalTime cannot be converted to a ZonedDateTime, so toLocalDate should throw IllegalArgumentException."), // BigDecimal → IllegalArgumentException
        Arguments.of(new java.math.BigDecimal("12345.67"), IllegalArgumentException.class, "BigDecimal is not a supported source type for date conversion; expect IllegalArgumentException."), // ZoneId → IllegalArgumentException
        Arguments.of(ZoneId.of("UTC"), IllegalArgumentException.class, "ZoneId does not represent a point in time and cannot be converted to a LocalDate; should raise IllegalArgumentException."), // Empty string → DateTimeParseException
        Arguments.of("", DateTimeParseException.class, "Empty string cannot be parsed as a date, resulting in DateTimeParseException."), // Whitespace‑only string → DateTimeParseException
        Arguments.of("   ", DateTimeParseException.class, "String containing only whitespace is not a valid date format; parsing should throw DateTimeParseException."), // Map → IllegalArgumentException
        Arguments.of(Map.of("year", 2022, "month", 1, "day", 1), IllegalArgumentException.class, "Map is not a recognized source type for date conversion; conversion should fail with IllegalArgumentException."));
    }

    @Test
    void toLocalDate_fromLocalDate_returnsSameDate() {
        LocalDate source = LocalDate.of(2022, Month.MAY, 15);
        LocalDate result = CalendarConversions.toLocalDate(source, converter);
        assertEquals(source, result);
    }

    @Test
    void toLocalDate_fromLocalDateTime_returnsDatePart() {
        LocalDateTime source = LocalDateTime.of(2022, Month.DECEMBER, 3, 10, 30, 45);
        LocalDate expected = source.toLocalDate();
        LocalDate result = CalendarConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromZonedDateTime_returnsDatePart() {
        ZonedDateTime source = ZonedDateTime.of(2021, 8, 22, 14, 0, 0, 0, // any zone is fine – only the date matters
        ZoneOffset.ofHours(-4));
        LocalDate expected = source.toLocalDate();
        LocalDate result = CalendarConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromInstant_returnsDateInUTC() {
        // 2022-01-01T00:00:00Z
        Instant instant = Instant.ofEpochSecond(1_640_995_200L);
        // CalendarConversions ultimately uses ZonedDateTime; the default zone of the
        // Converter is the JVM default. To make the test deterministic we convert the
        // instant to UTC ourselves and compare the date part.
        LocalDate expected = instant.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate result = CalendarConversions.toLocalDate(instant, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromDate_returnsDateInUTC() {
        // Date holds an instant; we treat it as UTC for the expected result.
        Date source = Date.from(Instant.parse("2020-02-29T12:34:56Z"));
        LocalDate expected = source.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate result = CalendarConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromString_parsesISODate() {
        // The Converter supports ISO‑8601 date strings.
        String source = "1999-12-31";
        LocalDate expected = LocalDate.of(1999, Month.DECEMBER, 31);
        LocalDate result = CalendarConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromNull_returnsNull() {
        assertNull(CalendarConversions.toLocalDate(null, converter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toLocalDate_validConversions(Object from, LocalDate expected, String description) {
        LocalDate result = CalendarConversions.toLocalDate(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidConversionProvider")
    void toLocalDate_invalidConversions(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> CalendarConversions.toLocalDate(from, converter), description);
    }

    @Test
    void toLocalDate_nullInput_returnsNull() {
        assertNull(CalendarConversions.toLocalDate(null, converter), "Null source value should result in a null LocalDate (no conversion performed).");
    }
}
