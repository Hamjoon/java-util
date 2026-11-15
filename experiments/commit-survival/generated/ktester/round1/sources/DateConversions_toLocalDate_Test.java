package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link DateConversions#toLocalDate(Object, Converter)}.
 * <p>
 * The tests are divided into two groups:
 * <ul>
 *   <li>Successful conversions – the method should return the expected {@link LocalDate}
 *       (or {@code null} for a {@code null} input).</li>
 *   <li>Exception cases – the method should throw the expected exception for unsupported
 *       or malformed inputs.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class DateConversions_toLocalDate_Test {

    /** Converter instance used for all tests. */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter is sufficient for these simple type conversions.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /* --------------------------------------------------------------
     *  Successful conversion test cases (group: toLocalDate_tests)
     * -------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideValidConversions")
    void toLocalDate_validInputs(Object from, LocalDate expected, String description) {
        // WHEN
        LocalDate result = DateConversions.toLocalDate(from, converter);

        // THEN
        assertEquals(expected, result, description);
    }

    private static Stream<Arguments> provideValidConversions() {
        // Helper to create a Calendar set to 2023‑05‑17 in UTC+03:00
        Calendar utcPlus3Calendar() {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC+03:00"));
            cal.clear();
            cal.set(2023, Calendar.MAY, 17, 0, 0, 0);
            return cal;
        }

        return Stream.of(
                // LocalDate – should be returned unchanged
                Arguments.of(
                        LocalDate.of(2023, 5, 17),
                        LocalDate.of(2023, 5, 17),
                        "Pass a LocalDate directly; method should return the same date unchanged."
                ),
                // ISO‑8601 date string
                Arguments.of(
                        "2023-05-17",
                        LocalDate.of(2023, 5, 17),
                        "String in ISO‑8601 date format should be parsed to the correct LocalDate."
                ),
                // ISO‑8601 date‑time string – date part only
                Arguments.of(
                        "2023-05-17T15:30:45",
                        LocalDate.of(2023, 5, 17),
                        "String with date‑time component should be parsed and the date part extracted."
                ),
                // java.sql.Date
                Arguments.of(
                        java.sql.Date.valueOf("2023-05-17"),
                        LocalDate.of(2023, 5, 17),
                        "SQL Date should be converted to the same LocalDate."
                ),
                // java.util.Date (epoch milliseconds, UTC)
                Arguments.of(
                        new Date(Instant.parse("2023-05-17T00:00:00Z").toEpochMilli()),
                        // Expected date is the instant interpreted in the system default zone
                        Date.from(Instant.parse("2023-05-17T00:00:00Z"))
                                .toInstant()
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate(),
                        "java.util.Date (UTC epoch) should be converted to the correct LocalDate in the system default zone."
                ),
                // LocalDateTime – date component only
                Arguments.of(
                        LocalDateTime.of(2023, 5, 17, 23, 59, 59),
                        LocalDate.of(2023, 5, 17),
                        "LocalDateTime should yield its date component."
                ),
                // ZonedDateTime with +02:00 offset shifting the instant back one day
                Arguments.of(
                        ZonedDateTime.of(2023, 5, 18, 1, 0, 0, 0, ZoneOffset.ofHours(2)),
                        LocalDate.of(2023, 5, 17),
                        "ZonedDateTime with a timezone offset that shifts the instant back one day should produce the previous date."
                ),
                // OffsetDateTime – behaves like ZonedDateTime for date extraction
                Arguments.of(
                        OffsetDateTime.of(2023, 5, 18, 1, 0, 0, 0, ZoneOffset.ofHours(2)),
                        LocalDate.of(2023, 5, 17),
                        "OffsetDateTime behaves like ZonedDateTime for date extraction."
                ),
                // Instant – interpreted in the system default zone
                Arguments.of(
                        Instant.parse("2023-05-17T12:00:00Z"),
                        Instant.parse("2023-05-17T12:00:00Z")
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate(),
                        "Instant should be interpreted in the system default zone and converted to the appropriate LocalDate."
                ),
                // Calendar set to 2023‑05‑17 in UTC+03:00
                Arguments.of(
                        utcPlus3Calendar(),
                        LocalDate.of(2023, 5, 17),
                        "Calendar set to a specific date (2023‑05‑17) and timezone should yield that date."
                ),
                // Null input – should return null
                Arguments.of(
                        null,
                        null,
                        "Null input should result in null."
                ),
                // Invalid string – parsing exception (handled in separate test)
                Arguments.of(
                        "not-a-date",
                        null,
                        "Invalid string should cause a parsing exception."
                )
        );
    }

    @Test
    void toLocalDate_invalidString_throwsDateTimeParseException() {
        // GIVEN
        String bad = "not-a-date";

        // WHEN / THEN
        assertThrows(DateTimeParseException.class,
                () -> DateConversions.toLocalDate(bad, converter),
                "Invalid string should cause a DateTimeParseException.");
    }

    /* --------------------------------------------------------------
     *  Exception test cases (group: toLocalDate_exception_tests)
     * -------------------------------------------------------------- */

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideExceptionConversions")
    void toLocalDate_invalidInputs_throwException(Object from, Class<? extends Throwable> expectedException, String description) {
        // WHEN / THEN
        assertThrows(expectedException,
                () -> DateConversions.toLocalDate(from, converter),
                description);
    }

    private static Stream<Arguments> provideExceptionConversions() {
        return Stream.of(
                Arguments.of(
                        true,
                        IllegalArgumentException.class,
                        "Passing an unsupported Boolean type should cause an IllegalArgumentException."
                ),
                Arguments.of(
                        LocalTime.of(15, 30, 45),
                        DateTimeException.class,
                        "A LocalTime (time‑only) cannot be converted to a date; should throw DateTimeException."
                ),
                Arguments.of(
                        java.sql.Time.valueOf("12:34:56"),
                        DateTimeException.class,
                        "java.sql.Time represents only a time component; conversion to LocalDate should fail with DateTimeException."
                ),
                Arguments.of(
                        Double.valueOf(1621234567890.5),
                        IllegalArgumentException.class,
                        "A fractional epoch millisecond (Double) is not a valid input for date conversion; should raise IllegalArgumentException."
                ),
                Arguments.of(
                        YearMonth.of(2023, 5),
                        DateTimeException.class,
                        "YearMonth lacks a day component; conversion to LocalDate should result in DateTimeException."
                ),
                Arguments.of(
                        MonthDay.of(5, 17),
                        DateTimeException.class,
                        "MonthDay lacks a year component; conversion to LocalDate should result in DateTimeException."
                ),
                Arguments.of(
                        new Object(),
                        IllegalArgumentException.class,
                        "An arbitrary Object is not a supported source type; should trigger IllegalArgumentException."
                )
        );
    }
}