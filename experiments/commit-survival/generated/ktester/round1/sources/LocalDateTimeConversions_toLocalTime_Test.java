package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toLocalTime_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance.  We can supply a mock that
        // returns an empty map for overrides – this mimics the default configuration.
        ConverterOptions options = mock(ConverterOptions.class);
        when(options.getConverterOverrides()).thenReturn(Collections.emptyMap());
        converter = new Converter(options);
    }

    /* ----------------------------------------------------------------------
       Helper to temporarily change the JVM default time‑zone for deterministic
       testing of conversions that rely on ZoneId.systemDefault().
       ---------------------------------------------------------------------- */
    private static void setSystemDefaultZone(ZoneId zone) {
        System.setProperty("user.timezone", zone.getId());
        // Reset cached default zone in java.time
        try {
            java.lang.reflect.Field field = ZoneId.class.getDeclaredField("systemDefaultZone");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception ignored) {
            // If reflection fails, the test may still pass depending on the JDK implementation.
        }
    }

    private static Stream<Arguments> successfulConversionProvider() {
        // Calendar instances need to be created once per argument to capture their fields.
        Calendar calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calUtc.set(2023, Calendar.JULY, 4, 12, 34, 56);
        calUtc.set(Calendar.MILLISECOND, 0);
        Calendar calNy = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        calNy.set(2023, Calendar.MAY, 17, 10, 30, 45);
        calNy.set(Calendar.MILLISECOND, 0);
        Calendar calParis = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
        calParis.set(2023, Calendar.MAY, 17, 12, 45, 30);
        calParis.set(Calendar.MILLISECOND, 0);
        return Stream.of(// ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 0, ZoneId.of("UTC")), "ZonedDateTime UTC", LocalTime.of(14, 30, 45)), // OffsetDateTime with -05:00 offset
        Arguments.of(OffsetDateTime.of(2023, 5, 17, 9, 15, 0, 0, ZoneOffset.ofHours(-5)), "OffsetDateTime -05:00", LocalTime.of(9, 15)), // LocalDateTime (interpreted in system default zone – we force UTC)
        Arguments.of(LocalDateTime.of(2023, 12, 31, 23, 59, 59), "LocalDateTime interpreted in UTC", LocalTime.of(23, 59, 59)), // LocalDate → midnight
        Arguments.of(LocalDate.of(2023, 1, 1), "LocalDate → MIDNIGHT", LocalTime.MIDNIGHT), // java.util.Date (instant) → UTC
        Arguments.of(Date.from(Instant.parse("2023-07-04T12:34:56Z")), "java.util.Date → UTC", LocalTime.of(12, 34, 56)), // java.sql.Timestamp → UTC
        Arguments.of(new Timestamp(Instant.parse("2023-07-04T08:00:00Z").toEpochMilli()), "java.sql.Timestamp → UTC", LocalTime.of(8, 0)), // Calendar (UTC) → fields
        Arguments.of(calUtc, "Calendar UTC → fields", LocalTime.of(calUtc.get(Calendar.HOUR_OF_DAY), calUtc.get(Calendar.MINUTE), calUtc.get(Calendar.SECOND))), // ISO‑8601 string with offset +02:00
        Arguments.of("2023-08-15T16:45:30+02:00", "String with +02:00 offset", LocalTime.of(16, 45, 30)), // Custom formatted string (space separator)
        Arguments.of("2023-08-15 16:45:30", "String with space separator", LocalTime.of(16, 45, 30)), // Long epoch millis
        Arguments.of(123_456_789L, "Long epoch millis", LocalTime.ofInstant(Instant.ofEpochMilli(123_456_789L), ZoneId.systemDefault()).toLocalTime()), // BigDecimal epoch millis
        Arguments.of(new BigDecimal("1622548800000"), "BigDecimal epoch millis", LocalTime.ofInstant(Instant.ofEpochMilli(1_622_548_800_000L), ZoneId.systemDefault()).toLocalTime()), // AtomicLong epoch millis
        Arguments.of(new AtomicLong(1_622_548_800_000L), "AtomicLong epoch millis", LocalTime.ofInstant(Instant.ofEpochMilli(1_622_548_800_000L), ZoneId.systemDefault()).toLocalTime()), // ZonedDateTime America/Los_Angeles (time component unchanged)
        Arguments.of(ZonedDateTime.of(2023, 5, 17, 14, 30, 45, 0, ZoneId.of("America/Los_Angeles")), "ZonedDateTime America/Los_Angeles", LocalTime.of(14, 30, 45)), // Same instant expressed in New York (UTC‑4)
        Arguments.of(ZonedDateTime.ofInstant(Instant.parse("2023-05-17T14:30:45Z"), ZoneId.of("America/New_York")), "ZonedDateTime same instant New York", LocalTime.of(10, 30, 45)), // java.sql.Date → midnight
        Arguments.of(new java.sql.Date(LocalDate.of(2023, 5, 17).toEpochDay() * 86_400_000L), "java.sql.Date → MIDNIGHT", LocalTime.MIDNIGHT), // Instant → system default zone (forced to UTC)
        Arguments.of(Instant.parse("2023-08-15T12:34:56Z"), "Instant → UTC", LocalTime.ofInstant(Instant.parse("2023-08-15T12:34:56Z"), ZoneId.systemDefault()).toLocalTime()), // ISO‑8601 string with positive offset +03:00
        Arguments.of("2023-08-15T18:45:30+03:00", "String with +03:00 offset", LocalTime.of(18, 45, 30)), // Calendar America/New_York
        Arguments.of(calNy, "Calendar America/New_York → fields", LocalTime.of(calNy.get(Calendar.HOUR_OF_DAY), calNy.get(Calendar.MINUTE), calNy.get(Calendar.SECOND))), // Calendar Europe/Paris
        Arguments.of(calParis, "Calendar Europe/Paris → fields", LocalTime.of(calParis.get(Calendar.HOUR_OF_DAY), calParis.get(Calendar.MINUTE), calParis.get(Calendar.SECOND))), // Negative epoch millis
        Arguments.of(-86_400_000L, "Negative epoch millis (one day before epoch)", LocalTime.ofInstant(Instant.ofEpochMilli(-86_400_000L), ZoneId.systemDefault()).toLocalTime()));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// Unsupported type: Object
        Arguments.of(new Object(), "Unsupported type Object → IllegalArgumentException", IllegalArgumentException.class), // Unsupported numeric type: BigInteger
        Arguments.of(new BigInteger("1622548800000"), "BigInteger not supported → IllegalArgumentException", IllegalArgumentException.class), // Unsupported temporal type: Year
        Arguments.of(Year.of(2023), "Year not supported → IllegalArgumentException", IllegalArgumentException.class), // Unsupported temporal type: YearMonth
        Arguments.of(YearMonth.of(2023, 5), "YearMonth not supported → IllegalArgumentException", IllegalArgumentException.class), // String containing only a date part
        Arguments.of("2023-08-15", "Date‑only string → DateTimeParseException", DateTimeParseException.class), // Time‑only string
        Arguments.of("12:34:56", "Time‑only string → DateTimeParseException", DateTimeParseException.class), // Malformed date‑time string
        Arguments.of("2023-13-40T99:99:99", "Malformed string → DateTimeParseException", DateTimeParseException.class), // Empty string
        Arguments.of("", "Empty string → IllegalArgumentException", IllegalArgumentException.class), // Invalid offset (+25:00)
        Arguments.of("2023-08-15T12:34:56+25:00", "Invalid offset → DateTimeParseException", DateTimeParseException.class), // ZoneId alone
        Arguments.of(ZoneId.of("UTC"), "ZoneId alone → IllegalArgumentException", IllegalArgumentException.class), // Map without temporal fields
        Arguments.of(Collections.singletonMap("hour", 12), "Map without temporal fields → IllegalArgumentException", IllegalArgumentException.class), // Null Converter
        Arguments.of(Instant.now(), "Null Converter → NullPointerException", NullPointerException.class), // Both arguments null
        Arguments.of(null, "Both arguments null → NullPointerException", NullPointerException.class), // LocalTime alone (unsupported)
        Arguments.of(LocalTime.of(13, 45), "LocalTime alone → IllegalArgumentException", IllegalArgumentException.class), // OffsetTime alone (unsupported)
        Arguments.of(OffsetTime.of(13, 45, 0, 0, ZoneOffset.ofHours(2)), "OffsetTime alone → IllegalArgumentException", IllegalArgumentException.class));
    }

    @Test
    void toLocalTime_fromZonedDateTime_returnsSameLocalTime() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 15, 30, 45, 0, ZoneOffset.UTC);
        // WHEN
        LocalTime result = LocalDateTimeConversions.toLocalTime(zdt, converter);
        // THEN
        assertEquals(LocalTime.of(15, 30, 45), result);
    }

    @Test
    void toLocalTime_fromInstant_returnsLocalTimeInSystemDefaultZone() {
        // GIVEN
        Instant instant = Instant.parse("2023-05-10T15:30:45Z");
        // The conversion will use the system default zone when turning an Instant into a ZonedDateTime.
        // To make the test deterministic we force the default zone to UTC for the duration of the test.
        ZoneId originalZone = ZoneId.systemDefault();
        try {
            setSystemDefaultZone(ZoneOffset.UTC);
            // WHEN
            LocalTime result = LocalDateTimeConversions.toLocalTime(instant, converter);
            // THEN
            assertEquals(LocalTime.of(15, 30, 45), result);
        } finally {
            setSystemDefaultZone(originalZone);
        }
    }

    @Test
    void toLocalTime_fromDate_returnsLocalTimeInSystemDefaultZone() {
        // GIVEN
        Date date = Date.from(Instant.parse("2023-05-10T08:15:20Z"));
        ZoneId originalZone = ZoneId.systemDefault();
        try {
            setSystemDefaultZone(ZoneOffset.UTC);
            // WHEN
            LocalTime result = LocalDateTimeConversions.toLocalTime(date, converter);
            // THEN
            assertEquals(LocalTime.of(8, 15, 20), result);
        } finally {
            setSystemDefaultZone(originalZone);
        }
    }

    @Test
    void toLocalTime_fromLongEpochMillis_returnsCorrectLocalTime() {
        // GIVEN
        long epochMillis = Instant.parse("2023-05-10T22:45:10Z").toEpochMilli();
        ZoneId originalZone = ZoneId.systemDefault();
        try {
            setSystemDefaultZone(ZoneOffset.UTC);
            // WHEN
            LocalTime result = LocalDateTimeConversions.toLocalTime(epochMillis, converter);
            // THEN
            assertEquals(LocalTime.of(22, 45, 10), result);
        } finally {
            setSystemDefaultZone(originalZone);
        }
    }

    @Test
    void toLocalTime_fromStringIsoFormat_returnsParsedLocalTime() {
        // GIVEN
        String isoString = "2023-05-10T13:05:55Z";
        ZoneId originalZone = ZoneId.systemDefault();
        try {
            setSystemDefaultZone(ZoneOffset.UTC);
            // WHEN
            LocalTime result = LocalDateTimeConversions.toLocalTime(isoString, converter);
            // THEN
            assertEquals(LocalTime.of(13, 5, 55), result);
        } finally {
            setSystemDefaultZone(originalZone);
        }
    }

    @Test
    void toLocalTime_nullInput_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toLocalTime(nullObj, converter));
    }

    /* ----------------------------------------------------------------------
       Successful conversion cases – each entry provides the source object,
       a short description (useful when a test fails) and the expected LocalTime.
       ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("successfulConversionProvider")
    void toLocalTime_successful(Object source, String description, LocalTime expected) {
        // Many conversions depend on the JVM default zone – enforce UTC for consistency.
        ZoneId original = ZoneId.systemDefault();
        try {
            setSystemDefaultZone(ZoneOffset.UTC);
            LocalTime actual = LocalDateTimeConversions.toLocalTime(source, converter);
            assertEquals(expected, actual, description);
        } finally {
            setSystemDefaultZone(original);
        }
    }

    /* ----------------------------------------------------------------------
       Exception‑expecting cases – each entry provides the source object,
       a description and the expected exception class.
       ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("exceptionConversionProvider")
    void toLocalTime_exceptions(Object source, String description, Class<? extends Throwable> expectedException) {
        ZoneId original = ZoneId.systemDefault();
        try {
            setSystemDefaultZone(ZoneOffset.UTC);
            assertThrows(expectedException, () -> LocalDateTimeConversions.toLocalTime(source, converter), description);
        } finally {
            setSystemDefaultZone(original);
        }
    }
}
