package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.util.Date;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.sql.*;
import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toBigDecimal_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – the Converter works out‑of‑the‑box for the
        // standard JDK types we are exercising.
        converter = new Converter(new ConverterOptions());
    }

    @Test
    void toBigDecimal_fromInstant_returnsSameAsInstantConversions() {
        // given
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        // when
        BigDecimal actual = LocalDateConversions.toBigDecimal(instant, converter);
        // then
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(expected, actual, "BigDecimal conversion from Instant should delegate to InstantConversions");
    }

    @Test
    void toBigDecimal_fromDate_returnsSameAsInstantConversions() {
        // given
        Instant instant = Instant.parse("2022-06-15T08:20:30Z");
        Date date = Date.from(instant);
        // when
        BigDecimal actual = LocalDateConversions.toBigDecimal(date, converter);
        // then
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(expected, actual, "BigDecimal conversion from Date should be based on the underlying Instant");
    }

    @Test
    void toBigDecimal_fromNull_returnsNull() {
        // given
        Object source = null;
        // when
        BigDecimal actual = LocalDateConversions.toBigDecimal(source, converter);
        // then
        assertNull(actual, "Converting a null source should result in null");
    }

    @Test
    void toBigDecimal_fromLocalDate_returnsSameAsInstantConversions() {
        // given
        LocalDate localDate = LocalDate.of(2021, 12, 31);
        // LocalDateConversions.toInstant will interpret the date at start of day UTC
        Instant expectedInstant = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        // when
        BigDecimal actual = LocalDateConversions.toBigDecimal(localDate, converter);
        // then
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        assertEquals(expected, actual, "BigDecimal conversion from LocalDate should use start‑of‑day UTC");
    }

    @Test
    void toBigDecimal_fromInstant_delegatesToInstantConversions() {
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        BigDecimal actual = LocalDateConversions.toBigDecimal(instant, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(expected, actual, "Instant should be delegated directly to InstantConversions");
    }

    @Test
    void toBigDecimal_fromDate_delegatesToInstantConversions() {
        Instant instant = Instant.parse("2022-06-15T08:20:30Z");
        Date date = Date.from(instant);
        BigDecimal actual = LocalDateConversions.toBigDecimal(date, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(instant, converter);
        assertEquals(expected, actual, "Date should be converted via its underlying Instant");
    }

    @Test
    void toBigDecimal_fromLocalDateTime_usesSystemDefaultZone() {
        // The library treats a LocalDateTime as being in the system default zone.
        // For reproducibility we set the default zone to UTC for this test.
        ZoneId original = ZoneId.systemDefault();
        try {
            System.setProperty("user.timezone", "UTC");
            LocalDateTime ldt = LocalDateTime.of(2023, 1, 1, 12, 34, 56);
            Instant expectedInstant = ldt.atZone(ZoneOffset.UTC).toInstant();
            BigDecimal actual = LocalDateConversions.toBigDecimal(ldt, converter);
            BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
            assertEquals(expected, actual, "LocalDateTime should be interpreted in UTC for the test");
        } finally {
            System.setProperty("user.timezone", original.getId());
        }
    }

    @Test
    void toBigDecimal_fromLocalDate_startOfDayUTC() {
        LocalDate localDate = LocalDate.of(2023, 1, 1);
        Instant expectedInstant = localDate.atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal actual = LocalDateConversions.toBigDecimal(localDate, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        assertEquals(expected, actual, "LocalDate should be converted to start‑of‑day UTC");
    }

    @Test
    void toBigDecimal_fromZonedDateTime_respectsOffset() {
        ZonedDateTime zdt = ZonedDateTime.parse("2023-01-01T12:34:56+02:00[Europe/Paris]");
        // 10:34:56Z
        Instant expectedInstant = zdt.toInstant();
        BigDecimal actual = LocalDateConversions.toBigDecimal(zdt, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        assertEquals(expected, actual, "ZonedDateTime conversion must honour its offset");
    }

    @Test
    void toBigDecimal_fromOffsetDateTime_respectsOffset() {
        OffsetDateTime odt = OffsetDateTime.parse("2023-01-01T12:34:56+05:30");
        // 07:04:56Z
        Instant expectedInstant = odt.toInstant();
        BigDecimal actual = LocalDateConversions.toBigDecimal(odt, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        assertEquals(expected, actual, "OffsetDateTime conversion must honour its offset");
    }

    @Test
    void toBigDecimal_fromTimestamp() {
        Timestamp ts = Timestamp.valueOf("2023-01-01 12:34:56");
        // same instant as the timestamp
        Instant expectedInstant = ts.toInstant();
        BigDecimal actual = LocalDateConversions.toBigDecimal(ts, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        assertEquals(expected, actual, "Timestamp should be converted via its Instant representation");
    }

    @Test
    void toBigDecimal_fromBigDecimal_isIdentity() {
        BigDecimal source = BigDecimal.valueOf(12345.6789);
        BigDecimal actual = LocalDateConversions.toBigDecimal(source, converter);
        // The conversion path treats a BigDecimal as a numeric epoch‑second value,
        // so the result should be the same number.
        assertEquals(source, actual, "Passing a BigDecimal should return the same value");
    }

    @Test
    void toBigDecimal_fromCalendarUTC() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.set(2023, Calendar.JANUARY, 1, 12, 34, 56);
        calendar.set(Calendar.MILLISECOND, 0);
        Instant expectedInstant = calendar.toInstant();
        BigDecimal actual = LocalDateConversions.toBigDecimal(calendar, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        assertEquals(expected, actual, "Calendar (UTC) should be converted via its Instant");
    }

    @Test
    void toBigDecimal_fromYearMonth_firstDayUTC() {
        YearMonth ym = YearMonth.of(2023, 1);
        Instant expectedInstant = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal actual = LocalDateConversions.toBigDecimal(ym, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        assertEquals(expected, actual, "YearMonth should be treated as first day of month at UTC");
    }

    @Test
    void toBigDecimal_fromYear_firstDayUTC() {
        Year y = Year.of(2023);
        Instant expectedInstant = y.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        BigDecimal actual = LocalDateConversions.toBigDecimal(y, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        assertEquals(expected, actual, "Year should be treated as Jan 1st at UTC");
    }

    @Test
    void toBigDecimal_fromBigInteger_isIdentity() {
        BigInteger source = BigInteger.valueOf(1672570496L);
        BigDecimal actual = LocalDateConversions.toBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(1672570496L), actual, "BigInteger representing epoch seconds should be returned unchanged");
    }

    @Test
    void toBigDecimal_fromAtomicLong_isIdentity() {
        AtomicLong source = new AtomicLong(1672570496L);
        BigDecimal actual = LocalDateConversions.toBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(1672570496L), actual, "AtomicLong holding epoch seconds should be returned unchanged");
    }

    @Test
    void toBigDecimal_fromLong_isIdentity() {
        Long source = Long.valueOf(1672570496L);
        BigDecimal actual = LocalDateConversions.toBigDecimal(source, converter);
        assertEquals(BigDecimal.valueOf(1672570496L), actual, "Long representing epoch seconds should be returned unchanged");
    }

    @Test
    void toBigDecimal_fromStringISO_isParsed() {
        String iso = "2023-01-01T12:34:56Z";
        Instant expectedInstant = Instant.parse(iso);
        BigDecimal actual = LocalDateConversions.toBigDecimal(iso, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        assertEquals(expected, actual, "ISO‑8601 string should be parsed to Instant");
    }

    @Test
    void toBigDecimal_fromSqlDate_startOfDayUTC() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2023-01-01");
        // midnight UTC
        Instant expectedInstant = sqlDate.toInstant();
        BigDecimal actual = LocalDateConversions.toBigDecimal(sqlDate, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        assertEquals(expected, actual, "java.sql.Date should become start‑of‑day UTC");
    }

    @Test
    void toBigDecimal_fromSqlTime_epochDay() {
        java.sql.Time sqlTime = java.sql.Time.valueOf("12:34:56");
        // The library treats a Time as being on the epoch day (1970‑01‑01)
        Instant expectedInstant = Instant.ofEpochSecond(0).plusSeconds(12 * 3600 + 34 * 60 + 56);
        BigDecimal actual = LocalDateConversions.toBigDecimal(sqlTime, converter);
        BigDecimal expected = InstantConversions.toBigDecimal(expectedInstant, converter);
        assertEquals(expected, actual, "java.sql.Time should be interpreted on the epoch day");
    }

    @Test
    void toBigDecimal_fromInstantEpoch_isZero() {
        Instant epoch = Instant.EPOCH;
        BigDecimal actual = LocalDateConversions.toBigDecimal(epoch, converter);
        assertEquals(BigDecimal.ZERO, actual, "Epoch instant must produce zero BigDecimal");
    }

    @Test
    void toBigDecimal_fromNegativeInstant() {
        Instant negative = Instant.ofEpochSecond(-1L);
        BigDecimal actual = LocalDateConversions.toBigDecimal(negative, converter);
        assertEquals(BigDecimal.valueOf(-1), actual, "Negative epoch second should be represented correctly");
    }

    @Test
    void toBigDecimal_nullConverter_throwsNPE() {
        Instant instant = Instant.parse("2023-01-01T12:34:56Z");
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toBigDecimal(instant, null), "Passing a null Converter should result in NPE");
    }

    @Test
    void toBigDecimal_invalidString_throwsException() {
        // month 13 does not exist
        String bad = "2023-13-01T12:34:56Z";
        // the library may wrap DateTimeParseException
        assertThrows(// the library may wrap DateTimeParseException
        RuntimeException.class, () -> LocalDateConversions.toBigDecimal(bad, converter), "Malformed ISO‑8601 string must cause a conversion exception");
    }

    @Test
    void toBigDecimal_bigIntegerOverflow_throwsException() {
        // Long.MAX_VALUE + 1 is out of range for Instant epoch seconds
        BigInteger overflow = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE);
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toBigDecimal(overflow, converter), "Overflowing epoch seconds should trigger an exception");
    }

    @Test
    void toBigDecimal_offsetTimeUnsupported_throwsException() {
        OffsetTime offsetTime = OffsetTime.of(12, 34, 56, 0, ZoneOffset.UTC);
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toBigDecimal(offsetTime, converter), "OffsetTime (time‑only) is not convertible to Instant");
    }

    @Test
    void toBigDecimal_localTimeUnsupported_throwsException() {
        LocalTime lt = LocalTime.of(12, 34, 56);
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toBigDecimal(lt, converter), "LocalTime (time‑only) cannot be converted to Instant");
    }

    @Test
    void toBigDecimal_calendarWithNullTimeZone_throwsNPE() {
        Calendar calendar = Calendar.getInstance();
        // deliberately break the zone information
        calendar.setTimeZone(null);
        calendar.set(2023, Calendar.JANUARY, 1, 12, 34, 56);
        calendar.set(Calendar.MILLISECOND, 0);
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toBigDecimal(calendar, converter), "Calendar with null TimeZone should cause NPE during conversion");
    }
}
