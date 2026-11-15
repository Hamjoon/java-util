package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toLocalDate_Test {

    /**
     * Preserve the original default time‑zone so we can restore it after the suite runs.
     */
    private static TimeZone originalDefaultZone;

    /**
     * The Converter instance used for all tests.
     */
    private Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Force a deterministic zone for the whole test suite.
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs an options object – the default options are sufficient for our tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // No external resources to clean up, but the method is kept for symmetry.
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone to avoid side‑effects on other tests.
        TimeZone.setDefault(originalDefaultZone);
    }

    /**
     * Helper that mirrors the conversion logic used inside {@code InstantConversions.toLocalDate}.
     */
    private static LocalDate expectedFromInstant(Instant instant) {
        // {@code InstantConversions.toZonedDateTime} ultimately uses the system default zone.
        // Because the test suite forces the default zone to UTC, we can safely use UTC here.
        return instant.atZone(ZoneOffset.UTC).toLocalDate();
    }

    /**
     * Supplies all valid conversion cases.
     */
    static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Instant at epoch
        Arguments.of(Instant.ofEpochSecond(0), LocalDate.of(1970, 1, 1)), // java.util.Date epoch
        Arguments.of(new Date(0L), LocalDate.of(1970, 1, 1)), // Calendar UTC 2022‑03‑15 10:20:00
        Arguments.of(createUtcCalendar(2022, Calendar.MARCH, 15, 10, 20, 0), LocalDate.of(2022, 3, 15)), // Long epoch millis -> 2023‑01‑01
        Arguments.of(Long.valueOf(1672531199000L), LocalDate.of(2023, 1, 1)), // ISO‑8601 UTC string
        Arguments.of("2021-12-31T23:59:59Z", LocalDate.of(2021, 12, 31)), // Map with INSTANT key
        Arguments.of(Map.of(MapConversions.INSTANT, Instant.ofEpochSecond(1609459200L)), LocalDate.of(2021, 1, 1)), // Calendar non‑UTC America/New_York 2022‑12‑31 23:00:00
        Arguments.of(createNyCalendar(2022, Calendar.DECEMBER, 31, 23, 0, 0), LocalDate.of(2023, 1, 1)), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2022-06-30 12:34:56.789"), LocalDate.of(2022, 6, 30)), // ZonedDateTime Europe/Paris 2021‑12‑31T23:00+01:00
        Arguments.of(ZonedDateTime.of(2021, 12, 31, 23, 0, 0, 0, ZoneId.of("Europe/Paris")), LocalDate.of(2021, 12, 31)), // OffsetDateTime 2022‑03‑15T01:30+03:00 -> UTC 2022‑03‑14
        Arguments.of(OffsetDateTime.parse("2022-03-15T01:30:00+03:00"), LocalDate.of(2022, 3, 14)), // String with -04:00 offset -> UTC 2022‑03‑16
        Arguments.of("2022-03-15T23:00:00-04:00", LocalDate.of(2022, 3, 16)));
    }

    /**
     * Supplies all exception‑producing cases.
     */
    static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source
        Arguments.of(null, NullPointerException.class, "null source should NPE"), // Null converter
        Arguments.of(Instant.ofEpochSecond(0), NullPointerException.class, "null converter should NPE", true), // Invalid string
        Arguments.of("not-a-date", DateTimeParseException.class, "malformed string"), // Unsupported Boolean
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "unsupported Boolean"), // Map without INSTANT key
        Arguments.of(Map.of("someKey", "2022-01-01T00:00:00Z"), IllegalArgumentException.class, "map missing INSTANT key"), // Map with INSTANT key but wrong value type
        Arguments.of(Map.of(MapConversions.INSTANT, "not-an-instant"), IllegalArgumentException.class, "map INSTANT value wrong type"), // Unsupported numeric type (Double)
        Arguments.of(Double.valueOf(12345.67), IllegalArgumentException.class, "unsupported Double"), // Arbitrary Object
        Arguments.of(new Object(), IllegalArgumentException.class, "unsupported Object"));
    }

    /**
     * Creates a UTC Calendar set to the given date‑time components.
     */
    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    /**
     * Creates an America/New_York Calendar set to the given date‑time components.
     */
    private static Calendar createNyCalendar(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("America/New_York"));
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void toLocalDate_fromInstant() {
        // GIVEN
        Instant instant = Instant.parse("2023-03-15T10:15:30.00Z");
        // WHEN
        LocalDate result = InstantConversions.toLocalDate(instant, converter);
        // THEN
        assertEquals(expectedFromInstant(instant), result, "Conversion from Instant should yield the same LocalDate as the UTC ZonedDateTime.");
    }

    @Test
    void toLocalDate_fromDate() {
        // GIVEN
        Date date = Date.from(Instant.parse("2022-12-01T00:00:00Z"));
        // WHEN
        LocalDate result = InstantConversions.toLocalDate(date, converter);
        // THEN
        assertEquals(LocalDate.of(2022, 12, 1), result, "Conversion from java.util.Date should produce the correct LocalDate.");
    }

    @Test
    void toLocalDate_fromLongEpochMillis() {
        // GIVEN
        // 2023-01-01T00:59:59Z
        long epochMillis = 1672531199000L;
        // The expected date in UTC for the above epoch millis.
        LocalDate expected = Instant.ofEpochMilli(epochMillis).atZone(ZoneOffset.UTC).toLocalDate();
        // WHEN
        LocalDate result = InstantConversions.toLocalDate(epochMillis, converter);
        // THEN
        assertEquals(expected, result, "Conversion from a long representing epoch milliseconds should yield the correct LocalDate.");
    }

    @Test
    void toLocalDate_fromZonedDateTime() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2021, 5, 20, 13, 45, 0, 0, ZoneId.of("UTC"));
        // WHEN
        LocalDate result = InstantConversions.toLocalDate(zdt, converter);
        // THEN
        assertEquals(zdt.toLocalDate(), result, "Conversion from ZonedDateTime should return its embedded LocalDate.");
    }

    @Test
    void toLocalDate_fromOffsetDateTime() {
        // GIVEN
        OffsetDateTime odt = OffsetDateTime.of(2020, 8, 10, 9, 30, 0, 0, ZoneOffset.ofHours(-5));
        // WHEN
        LocalDate result = InstantConversions.toLocalDate(odt, converter);
        // THEN
        assertEquals(odt.toLocalDate(), result, "Conversion from OffsetDateTime should return its embedded LocalDate.");
    }

    @ParameterizedTest(name = "[{index}] Convert {0} to {1}")
    @MethodSource("validConversionProvider")
    void toLocalDate_validConversions(Object from, LocalDate expected) {
        LocalDate result = InstantConversions.toLocalDate(from, converter);
        assertEquals(expected, result, () -> "Conversion of " + from + " should yield " + expected);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionProvider")
    void toLocalDate_exceptionCases(Object from, Class<? extends Throwable> expectedException, String description) {
        // When the converter itself is null we need to pass it explicitly.
        Converter usedConverter = (description.contains("null converter")) ? null : converter;
        assertThrows(expectedException, () -> InstantConversions.toLocalDate(from, usedConverter), () -> "Expected " + expectedException.getSimpleName() + " for case: " + description);
    }
}
