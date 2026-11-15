package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toLocalDate_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using default options is sufficient for these tests
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference
        converter = null;
    }

    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// ISO‑8601 instant string (UTC)
        Arguments.of("2023-05-15T13:45:30Z", LocalDate.of(2023, 5, 15)), // java.util.Date (epoch milliseconds)
        Arguments.of(Date.from(Instant.parse("2022-12-01T00:00:00Z")), LocalDate.of(2022, 12, 1)), // java.sql.Timestamp (treated as LocalDateTime)
        Arguments.of(Timestamp.valueOf("2021-07-20 08:15:00"), LocalDate.of(2021, 7, 20)), // LocalDateTime (system default zone assumed UTC for deterministic test)
        Arguments.of(LocalDateTime.of(2020, 2, 29, 23, 59, 59), LocalDate.of(2020, 2, 29)), // OffsetDateTime (offset applied)
        Arguments.of(OffsetDateTime.parse("2019-11-30T12:00:00+02:00"), LocalDate.of(2019, 11, 30)), // ZonedDateTime (date component extracted)
        Arguments.of(ZonedDateTime.parse("2018-04-10T05:30:00-04:00[America/New_York]"), LocalDate.of(2018, 4, 10)), // LocalDate (should be returned unchanged)
        Arguments.of(LocalDate.of(2021, 12, 31), LocalDate.of(2021, 12, 31)), // Instant (converted via system default zone – UTC in CI)
        Arguments.of(Instant.parse("2022-06-15T23:30:00Z"), LocalDate.of(2022, 6, 15)), // Long epoch‑millis
        Arguments.of(Long.valueOf(Instant.parse("2020-01-02T12:00:00Z").toEpochMilli()), LocalDate.of(2020, 1, 2)), // BigInteger epoch‑millis
        Arguments.of(new BigInteger("1580515200000"), LocalDate.of(2020, 2, 1)), // BigDecimal epoch‑millis
        Arguments.of(new BigDecimal("1609459200000"), LocalDate.of(2021, 1, 1)), // YearMonth → first day of month
        Arguments.of(YearMonth.of(2023, 2), LocalDate.of(2023, 2, 1)), // Year → Jan 1st of that year
        Arguments.of(Year.of(2022), LocalDate.of(2022, 1, 1)), // java.sql.Date (already a date)
        Arguments.of(java.sql.Date.valueOf("2024-07-04"), LocalDate.of(2024, 7, 4)), // ZonedDateTime with negative offset
        Arguments.of(ZonedDateTime.parse("2023-03-31T23:30:00-04:00[America/New_York]"), LocalDate.of(2023, 3, 31)), // String containing only a date
        Arguments.of("2023-08-15", LocalDate.of(2023, 8, 15)));
    }

    private static Stream<Arguments> provideInvalidConversions() {
        return Stream.of(// Unsupported object type
        Arguments.of(new Object()), // OffsetTime (no date component)
        Arguments.of(OffsetTime.parse("12:34:56+02:00")), // LocalTime (no date component)
        Arguments.of(LocalTime.of(15, 30)), // MonthDay (no year)
        Arguments.of(MonthDay.of(2, 29)), // Invalid date string (non‑ISO)
        Arguments.of("2023/08/15"), // Impossible calendar date
        Arguments.of("2023-02-30T10:15:30Z"), // Numeric string that looks like epoch‑millis but is not parsed
        Arguments.of("1622505600000"), // Empty string
        Arguments.of(""), // Whitespace only string
        Arguments.of("   "), // Map instance (unsupported)
        Arguments.of(new LinkedHashMap<>()), // AtomicInteger (numeric type not handled)
        Arguments.of(new AtomicInteger(123456789)));
    }

    /**
     * Verify that an ISO‑8601 offset date‑time string is converted to the correct {@link LocalDate}.
     */
    @Test
    void toLocalDate_fromIsoOffsetDateTimeString() {
        // given
        String isoString = "2022-05-10T15:30:00+02:00";
        // when
        LocalDate result = OffsetDateTimeConversions.toLocalDate(isoString, converter);
        // then
        assertEquals(LocalDate.of(2022, 5, 10), result, "The LocalDate extracted from an ISO offset date‑time string should match the date component.");
    }

    /**
     * Verify that a {@link Date} instance is converted to the correct {@link LocalDate}.
     */
    @Test
    void toLocalDate_fromUtilDate() {
        // given
        // 2022‑05‑10T00:00:00Z
        Date date = Date.from(ZonedDateTime.of(2022, 5, 10, 0, 0, 0, 0, ZoneOffset.UTC).toInstant());
        // when
        LocalDate result = OffsetDateTimeConversions.toLocalDate(date, converter);
        // then
        assertEquals(LocalDate.of(2022, 5, 10), result, "Conversion from java.util.Date should yield the same calendar date in UTC.");
    }

    /**
     * Verify that a {@link ZonedDateTime} instance is converted correctly.
     */
    @Test
    void toLocalDate_fromZonedDateTime() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5));
        // when
        LocalDate result = OffsetDateTimeConversions.toLocalDate(zdt, converter);
        // then
        assertEquals(LocalDate.of(2021, 12, 31), result, "The date portion of the ZonedDateTime should be preserved.");
    }

    /**
     * Verify that passing {@code null} returns {@code null}.
     */
    @Test
    void toLocalDate_nullInput() {
        // given
        Object from = null;
        // when
        LocalDate result = OffsetDateTimeConversions.toLocalDate(from, converter);
        // then
        assertNull(result, "Converting a null source should return null.");
    }

    /* ------------------------------------------------------------
     *  Parameterized tests – successful conversions returning a LocalDate
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "[{index}] from {0} → {1}")
    @MethodSource("provideValidConversions")
    void toLocalDate_validConversions(Object from, LocalDate expected) {
        // when
        LocalDate result = OffsetDateTimeConversions.toLocalDate(from, converter);
        // then
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromUtcCalendar_returnsCurrentUtcDate() {
        // given – a Calendar in UTC set to the current instant
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTime(Date.from(Instant.now()));
        // when
        LocalDate result = OffsetDateTimeConversions.toLocalDate(utcCal, converter);
        // then – expected is the current UTC date at the moment of execution
        LocalDate expected = LocalDate.now(ZoneOffset.UTC);
        assertEquals(expected, result, "UTC Calendar should yield the current UTC date.");
    }

    @Test
    void toLocalDate_fromLosAngelesCalendar_returnsExpectedDate() {
        // given – Calendar in America/Los_Angeles set to the instant 2023‑05‑01T08:00:00Z
        ZoneId laZone = ZoneId.of("America/Los_Angeles");
        Calendar laCal = Calendar.getInstance(TimeZone.getTimeZone(laZone));
        Instant instant = Instant.parse("2023-05-01T08:00:00Z");
        laCal.setTime(Date.from(instant));
        // when
        LocalDate result = OffsetDateTimeConversions.toLocalDate(laCal, converter);
        // then – the calendar's zone is -07:00 (DST), so the local date is 2023‑05‑01
        LocalDate expected = LocalDate.of(2023, 5, 1);
        assertEquals(expected, result, "Calendar in Los Angeles at 2023-05-01T08:00:00Z should resolve to 2023-05-01 in that zone.");
    }

    /* ------------------------------------------------------------
     *  Parameterized tests – expected IllegalArgumentException
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "[{index}] from {0} should throw IAE")
    @MethodSource("provideInvalidConversions")
    void toLocalDate_invalidConversions(Object from) {
        // when / then
        assertThrows(IllegalArgumentException.class, () -> OffsetDateTimeConversions.toLocalDate(from, converter), "Conversion should fail with IllegalArgumentException for unsupported or malformed input.");
    }
}
