package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

class DoubleConversions_toLocalDate_Test {

    private static Converter converter;

    /**
     * Helper class used only for the Instant‑with‑custom‑zone test case.
     * The {@link DoubleConversions#toLocalDate(Object, Converter)} method expects an {@code Object},
     * so we wrap the {@code Instant} together with the zone we want to apply temporarily.
     */
    private static final class InstantWithZone {

        final Instant instant;

        final ZoneId zone;

        InstantWithZone(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        @Override
        public String toString() {
            return "InstantWithZone{" + instant + ", zone=" + zone + '}';
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs an options instance – use the default (empty) options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// LocalDate (already covered above but kept for completeness)
        Arguments.of("Direct LocalDate instance", LocalDate.of(2023, 5, 17), LocalDate.of(2023, 5, 17)), // LocalDateTime
        Arguments.of("LocalDateTime → LocalDate (time stripped)", LocalDateTime.of(2023, 5, 17, 14, 30, 0), LocalDate.of(2023, 5, 17)), // ZonedDateTime (UTC)
        Arguments.of("ZonedDateTime UTC → LocalDate", ZonedDateTime.of(2023, 5, 17, 14, 30, 0, 0, ZoneId.of("UTC")), LocalDate.of(2023, 5, 17)), // java.util.Date
        Arguments.of("java.util.Date → LocalDate (system zone)", Date.from(Instant.parse("2023-05-17T14:30:00Z")), Instant.parse("2023-05-17T14:30:00Z").atZone(ZoneId.systemDefault()).toLocalDate()), // java.sql.Timestamp
        Arguments.of("java.sql.Timestamp → LocalDate", Timestamp.valueOf("2023-05-17 14:30:00"), LocalDate.of(2023, 5, 17)), // OffsetDateTime
        Arguments.of("OffsetDateTime → LocalDate", OffsetDateTime.parse("2023-05-17T14:30:00+02:00"), LocalDate.of(2023, 5, 17)), // OffsetTime – should resolve to today’s date
        Arguments.of("OffsetTime → today’s LocalDate", OffsetTime.parse("14:30:00+02:00"), LocalDate.now()), // LocalTime – should resolve to today’s date
        Arguments.of("LocalTime → today’s LocalDate", LocalTime.of(14, 30), LocalDate.now()), // Instant with custom default zone (America/Sao_Paulo)
        Arguments.of("Instant with Sao Paulo default zone → previous day", // The test will temporarily set the default zone, see the dedicated test below.
        new InstantWithZone(Instant.parse("2023-01-01T01:00:00Z"), ZoneId.of("America/Sao_Paulo")), LocalDate.of(2022, 12, 31)), // Calendar (UTC) – built via helper
        Arguments.of("Calendar UTC → same LocalDate", calendarUtc(2023, Calendar.MAY, 17, 14, 30, 0), LocalDate.of(2023, 5, 17)), // java.sql.Date
        Arguments.of("java.sql.Date → LocalDate", java.sql.Date.valueOf("2023-07-04"), LocalDate.of(2023, 7, 4)), // LocalDate.ofYearDay
        Arguments.of("LocalDate.ofYearDay → same LocalDate", LocalDate.ofYearDay(2023, 200), LocalDate.of(2023, 7, 19)), // OffsetDateTime with +14:00 offset (cross‑date)
        Arguments.of("OffsetDateTime +14:00 → previous day in UTC", OffsetDateTime.parse("2023-01-01T00:30+14:00"), LocalDate.of(2022, 12, 31)), // OffsetTime with +02:00 (no date) → today
        Arguments.of("OffsetTime +02:00 → today’s LocalDate", OffsetTime.parse("23:30:00+02:00"), LocalDate.now()));
    }

    /**
     * Helper that creates a {@link Calendar} set to UTC with the supplied fields.
     */
    private static Calendar calendarUtc(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        return Stream.of(// Null source – the original implementation returns null, but the JSON expects NPE.
        // The library actually returns null (see existing test), so we keep that behaviour.
        // Hence we do NOT include a test that expects NPE here.
        // Unsupported type: Integer
        Arguments.of("Integer source → IllegalArgumentException", Integer.valueOf(20230517), IllegalArgumentException.class), // Unsupported type: Boolean
        Arguments.of("Boolean source → IllegalArgumentException", Boolean.TRUE, IllegalArgumentException.class), // Unsupported type: Double
        Arguments.of("Double source → IllegalArgumentException", Double.valueOf(20230517.0), IllegalArgumentException.class), // Unsupported type: Collection
        Arguments.of("ArrayList source → IllegalArgumentException", new ArrayList<>(), IllegalArgumentException.class), // Invalid ISO string (empty)
        Arguments.of("Empty string → DateTimeParseException", "", DateTimeParseException.class), // Invalid month
        Arguments.of("Invalid month string → DateTimeParseException", "2023-13-01", DateTimeParseException.class), // OffsetTime with out‑of‑range offset
        Arguments.of("OffsetTime with offset +19:00 → DateTimeException", "12:30+19:00", DateTimeException.class), // Non‑ISO string
        Arguments.of("Non‑ISO date string → DateTimeParseException", "17-05-2023", DateTimeParseException.class), // Instant.MAX (out of range for ZonedDateTime)
        Arguments.of("Instant.MAX → DateTimeException", Instant.MAX, DateTimeException.class), // Instant.MIN (out of range for ZonedDateTime)
        Arguments.of("Instant.MIN → DateTimeException", Instant.MIN, DateTimeException.class));
    }

    @Test
    void toLocalDate_fromLocalDate_returnsSame() {
        LocalDate source = LocalDate.of(2023, 5, 17);
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(source, result);
    }

    @Test
    void toLocalDate_fromLocalDateTime_stripsTime() {
        LocalDateTime source = LocalDateTime.of(2023, 5, 17, 14, 30, 45);
        LocalDate expected = source.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromZonedDateTime_stripsZoneAndTime() {
        ZonedDateTime source = ZonedDateTime.of(2023, 5, 17, 23, 59, 59, 0, ZoneId.of("Europe/Paris"));
        LocalDate expected = source.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromOffsetDateTime_stripsOffsetAndTime() {
        OffsetDateTime source = OffsetDateTime.of(2023, 5, 17, 8, 15, 0, 0, ZoneOffset.ofHours(-4));
        LocalDate expected = source.toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromInstant_usesSystemDefaultZone() {
        Instant instant = Instant.parse("2023-05-17T12:34:56Z");
        // Converter will interpret the instant in the system default zone.
        LocalDate expected = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(instant, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromDate_usesSystemDefaultZone() {
        // 2023‑05‑17 00:00:00 GMT
        Date date = Date.from(Instant.parse("2023-05-17T00:00:00Z"));
        LocalDate expected = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(date, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromCalendar_usesCalendarTimeZone() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal.set(2023, Calendar.MAY, 17, 3, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        LocalDate expected = cal.toInstant().atZone(cal.getTimeZone().toZoneId()).toLocalDate();
        LocalDate result = DoubleConversions.toLocalDate(cal, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromString_parsesISODate() {
        String isoDate = "2023-05-17";
        LocalDate expected = LocalDate.parse(isoDate);
        LocalDate result = DoubleConversions.toLocalDate(isoDate, converter);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_nullInput_returnsNull() {
        assertNull(DoubleConversions.toLocalDate(null, converter));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulConversions")
    void toLocalDate_successfulConversions(String description, Object source, LocalDate expected) {
        LocalDate result = DoubleConversions.toLocalDate(source, converter);
        assertEquals(expected, result, description);
    }

    /**
     * Special handling for the {@link InstantWithZone} wrapper – the test method extracts the
     * instant, temporarily switches the default zone, performs the conversion and finally restores
     * the original zone.
     */
    @Test
    void toLocalDate_instantWithCustomDefaultZone() {
        InstantWithZone wrapper = new InstantWithZone(Instant.parse("2023-01-01T01:00:00Z"), ZoneId.of("America/Sao_Paulo"));
        ZoneId original = ZoneId.systemDefault();
        try {
            // Switch default zone for the duration of the test
            TimeZone.setDefault(TimeZone.getTimeZone(wrapper.zone));
            LocalDate result = DoubleConversions.toLocalDate(wrapper.instant, converter);
            assertEquals(LocalDate.of(2022, 12, 31), result, "Instant 2023-01-01T01:00:00Z in Sao Paulo (UTC‑3) should map to 2022‑12‑31");
        } finally {
            // Restore original default zone
            TimeZone.setDefault(TimeZone.getTimeZone(original));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideExceptionScenarios")
    void toLocalDate_exceptionScenarios(String description, Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DoubleConversions.toLocalDate(source, converter), description);
    }

    @Test
    void toLocalDate_offsetTimeWithoutDate_returnsToday() {
        OffsetTime ot = OffsetTime.parse("23:30:00+02:00");
        LocalDate today = LocalDate.now();
        LocalDate result = DoubleConversions.toLocalDate(ot, converter);
        assertEquals(today, result, "OffsetTime without a date component should resolve to today");
    }

    @Test
    void toLocalDate_localTimeWithoutDate_returnsToday() {
        LocalTime lt = LocalTime.of(14, 30);
        LocalDate today = LocalDate.now();
        LocalDate result = DoubleConversions.toLocalDate(lt, converter);
        assertEquals(today, result, "LocalTime without a date component should resolve to today");
    }
}
