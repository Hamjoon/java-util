package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toLocalDate_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – they are sufficient for the built‑in conversions we need.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Allow the Converter to be garbage‑collected.
        converter = null;
    }

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// – already a LocalDate
        Arguments.of(LocalDate.of(2023, 8, 15), LocalDate.of(2023, 8, 15)), // – LocalDateTime
        Arguments.of(LocalDateTime.of(2023, 8, 15, 13, 45, 30), LocalDate.of(2023, 8, 15)), // – java.util.Date (instant)
        Arguments.of(Date.from(Instant.parse("2023-08-15T13:45:30Z")), LocalDate.of(2023, 8, 15)), // – java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2023-08-15 13:45:30"), LocalDate.of(2023, 8, 15)), // – Instant
        Arguments.of(Instant.parse("2023-08-15T13:45:30Z"), LocalDate.of(2023, 8, 15)), // – ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.of(2023, 8, 15, 13, 45, 30, 0, ZoneId.of("UTC")), LocalDate.of(2023, 8, 15)), // – OffsetDateTime
        Arguments.of(OffsetDateTime.of(2023, 8, 15, 13, 45, 30, 0, ZoneOffset.UTC), LocalDate.of(2023, 8, 15)), // – Calendar in UTC
        Arguments.of(utcCalendar(2023, Calendar.AUGUST, 15, 13, 45, 30), LocalDate.of(2023, 8, 15)), // – ISO‑8601 date string
        Arguments.of("2023-08-15", LocalDate.of(2023, 8, 15)), // – ISO‑8601 date‑time string (no zone)
        Arguments.of("2023-08-15T13:45:30", LocalDate.of(2023, 8, 15)), // – epoch‑millis as Long
        Arguments.of(Long.valueOf(1692105930000L), LocalDate.of(2023, 8, 15)), // – epoch‑millis as BigDecimal
        Arguments.of(new BigDecimal("1692105930000"), LocalDate.of(2023, 8, 15)), // – Leap‑day
        Arguments.of(LocalDate.of(2020, 2, 29), LocalDate.of(2020, 2, 29)), // – Epoch zero
        Arguments.of(Long.valueOf(0L), LocalDate.of(1970, 1, 1)), // – Negative epoch (one day before 1970)
        Arguments.of(Long.valueOf(-86400000L), LocalDate.of(1969, 12, 31)), // – Maximum representable date via epoch millis (year 9999‑12‑31)
        Arguments.of(Long.valueOf(253402300799000L), LocalDate.of(9999, 12, 31)), // – java.sql.Date (date‑only)
        Arguments.of(java.sql.Date.valueOf("2023-08-15"), LocalDate.of(2023, 8, 15)), // – Calendar in America/Los_Angeles (PST) – date after zone conversion stays the same
        Arguments.of(GregorianCalendar.from(ZonedDateTime.of(2023, 8, 15, 13, 45, 30, 0, ZoneId.of("America/Los_Angeles"))), LocalDate.of(2023, 8, 15)), // – ISO‑8601 string with positive offset
        Arguments.of("2023-08-15T13:45:30+02:00", LocalDate.of(2023, 8, 15)), // – BigInteger epoch millis
        Arguments.of(new BigInteger("1692105930000"), LocalDate.of(2023, 8, 15)), // – AtomicLong epoch millis
        Arguments.of(new AtomicLong(1692105930000L), LocalDate.of(2023, 8, 15)));
    }

    private static Stream<Arguments> exceptionConversionsProvider() {
        return Stream.of(// – LocalTime (no date component)
        Arguments.of(LocalTime.of(13, 45, 30), IllegalArgumentException.class), // – Non‑ISO date string
        Arguments.of("15-08-2023", DateTimeParseException.class), // – Empty string
        Arguments.of("", DateTimeParseException.class), // – Whitespace‑only string
        Arguments.of("   ", DateTimeParseException.class), // – Unsupported type Boolean
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class), // – Unsupported type UUID
        Arguments.of(UUID.randomUUID(), IllegalArgumentException.class), // – Unsupported collection type ArrayList
        Arguments.of(new ArrayList<>(), IllegalArgumentException.class), // – Unsupported map type HashMap
        Arguments.of(new HashMap<>(), IllegalArgumentException.class), // – Malformed month (13)
        Arguments.of("2023-13-01", DateTimeParseException.class), // – Invalid day (Feb 30)
        Arguments.of("2023-02-30", DateTimeParseException.class), // – Invalid hour (25)
        Arguments.of("2023-08-15T25:00:00", DateTimeParseException.class), // – Epoch millis far beyond range (Long.MAX_VALUE)
        Arguments.of(Long.valueOf(Long.MAX_VALUE), DateTimeException.class), // – BigDecimal epoch beyond Long.MAX_VALUE
        Arguments.of(new BigDecimal("9223372036854775808"), DateTimeException.class));
    }

    private static Stream<Arguments> nullConverterProvider() {
        return Stream.of(Arguments.of("2023-08-15"), Arguments.of(LocalDate.of(2023, 8, 15)));
    }

    /**
     * Creates a {@link Calendar} in UTC with the supplied date‑time fields.
     */
    private static Calendar utcCalendar(int year, int monthZeroBased, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setLenient(false);
        cal.clear();
        cal.set(year, monthZeroBased, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    @Test
    void toLocalDate_fromLocalDateTime_returnsDatePart() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 17, 14, 30, 45);
        // when
        LocalDate result = LocalDateTimeConversions.toLocalDate(ldt, converter);
        // then
        assertEquals(LocalDate.of(2023, 5, 17), result);
    }

    @Test
    void toLocalDate_fromZonedDateTime_returnsDatePart() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(2021, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC);
        // when
        LocalDate result = LocalDateTimeConversions.toLocalDate(zdt, converter);
        // then
        assertEquals(LocalDate.of(2021, 12, 31), result);
    }

    @Test
    void toLocalDate_fromStringIsoFormat_returnsParsedDate() {
        // given
        String isoDate = "2022-08-15";
        // when
        LocalDate result = LocalDateTimeConversions.toLocalDate(isoDate, converter);
        // then
        assertEquals(LocalDate.of(2022, 8, 15), result);
    }

    @Test
    void toLocalDate_fromLongEpochMilli_returnsDateInSystemDefaultZone() {
        // given
        // 2021‑06‑01T00:00:00Z
        long epochMilli = 1622505600000L;
        // when
        LocalDate result = LocalDateTimeConversions.toLocalDate(epochMilli, converter);
        // then – the conversion uses the system default zone; we compare using UTC to avoid
        // platform‑dependent failures.
        LocalDate expected = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(epochMilli), ZoneOffset.UTC);
        assertEquals(expected, result);
    }

    @Test
    void toLocalDate_fromNull_returnsNull() {
        // given
        Object source = null;
        // when
        LocalDate result = LocalDateTimeConversions.toLocalDate(source, converter);
        // then
        assertNull(result);
    }

    /**
     * Valid conversions that should yield a concrete {@link LocalDate}.
     */
    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("validConversionsProvider")
    void toLocalDate_validConversions(Object from, LocalDate expected) {
        assertEquals(expected, LocalDateTimeConversions.toLocalDate(from, converter));
    }

    /**
     * Conversions that are expected to throw a specific exception.
     */
    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("exceptionConversionsProvider")
    void toLocalDate_expectedExceptions(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateTimeConversions.toLocalDate(from, converter));
    }

    /**
     * Null {@link Converter} handling – should always throw NPE regardless of source.
     */
    @ParameterizedTest(name = "[{index}] null converter with source={0}")
    @MethodSource("nullConverterProvider")
    void toLocalDate_nullConverter(Object from) {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toLocalDate(from, null));
    }
}
