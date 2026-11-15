package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class InstantConversions_toDate_Test {

    /**
     * The Converter used for all test cases.
     */
    private static Converter converter;

    /**
     * Preserve the original default time‑zone so we can restore it after the suite runs.
     */
    private static TimeZone originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Freeze the default zone to UTC – the conversion logic in InstantConversions relies on the default zone
        // for types that do not carry zone information (e.g., LocalDateTime).  Using a fixed zone makes the
        // expectations deterministic across environments.
        originalDefaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Initialise the Converter with default options (no overrides).
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default time‑zone so other test suites are not affected.
        TimeZone.setDefault(originalDefaultZone);
    }

    @Test
    void toDateFromLong() {
        // 2020‑09‑13T12:26:40Z
        Long epochMillis = 1_600_000_000_000L;
        Date expected = new Date(epochMillis);
        Date actual = InstantConversions.toDate(epochMillis, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Date created from a Long should match the supplied epoch milliseconds");
    }

    @Test
    void toDateFromInteger() {
        // 12.345 seconds after epoch
        Integer epochMillis = 12345;
        Date expected = new Date(epochMillis.longValue());
        Date actual = InstantConversions.toDate(epochMillis, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Date created from an Integer should be converted to long first");
    }

    @Test
    void toDateFromString() {
        // 2021‑01‑01T00:00:00Z
        String epochMillis = "1609459200000";
        Date expected = new Date(Long.parseLong(epochMillis));
        Date actual = InstantConversions.toDate(epochMillis, converter);
        assertEquals(expected.getTime(), actual.getTime(), "String containing a numeric epoch should be parsed as a long");
    }

    @Test
    void toDateFromDate() {
        Date now = new Date();
        Date actual = InstantConversions.toDate(now, converter);
        assertEquals(now.getTime(), actual.getTime(), "Passing a Date should result in an equivalent Date (same epoch millis)");
        assertNotSame(now, actual, "The returned Date must be a new instance, not the original object");
    }

    @Test
    void toDateFromTimestamp() {
        Timestamp ts = Timestamp.valueOf("2022-03-15 10:20:30.123456789");
        // Timestamp#getTime returns milliseconds
        Date expected = new Date(ts.getTime());
        Date actual = InstantConversions.toDate(ts, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Timestamp should be converted to Date preserving the millisecond value");
    }

    @Test
    void toDateFromInstant() {
        Instant instant = Instant.parse("2023-07-01T12:34:56.789Z");
        Date expected = Date.from(instant);
        Date actual = InstantConversions.toDate(instant, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Instant should be converted to Date with identical epoch milliseconds");
    }

    @Test
    void toDateFromLocalDateTime() {
        // LocalDateTime has no zone; InstantConversions uses the default zone (UTC in this test)
        LocalDateTime ldt = LocalDateTime.of(2024, Month.JANUARY, 2, 3, 4, 5, 0);
        long expectedMillis = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        Date expected = new Date(expectedMillis);
        Date actual = InstantConversions.toDate(ldt, converter);
        assertEquals(expected.getTime(), actual.getTime(), "LocalDateTime should be interpreted using the default zone (UTC) and converted to Date");
    }

    @Test
    void toDateFromLocalDate() {
        // LocalDate is interpreted as start of day at the default zone.
        LocalDate ld = LocalDate.of(2025, Month.FEBRUARY, 10);
        long expectedMillis = ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        Date expected = new Date(expectedMillis);
        Date actual = InstantConversions.toDate(ld, converter);
        assertEquals(expected.getTime(), actual.getTime(), "LocalDate should be converted to the start‑of‑day instant in the default zone");
    }

    @Test
    void toDateFromCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2022‑01‑01T00:00:00Z
        cal.setTimeInMillis(1_640_995_200_000L);
        Date expected = new Date(cal.getTimeInMillis());
        Date actual = InstantConversions.toDate(cal, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Calendar should be converted to Date preserving its millisecond value");
    }

    @Test
    void toDateFromNullReturnsEpochZeroDate() {
        // According to the conversion contract, a null source is treated as 0 for numeric targets.
        Date actual = InstantConversions.toDate(null, converter);
        assertNotNull(actual, "Result should never be null – a null source yields epoch‑zero Date");
        assertEquals(0L, actual.getTime(), "Null source should be interpreted as epoch millisecond 0");
    }

    @Test
    void toDateFromBigDecimalExact() {
        BigDecimal bd = BigDecimal.valueOf(1_622_505_600_000L);
        Date expected = new Date(bd.longValueExact());
        Date actual = InstantConversions.toDate(bd, converter);
        assertEquals(expected.getTime(), actual.getTime(), "BigDecimal with an integral value should be converted via longValueExact");
    }

    @Test
    void toDateFromBigInteger() {
        BigInteger bi = BigInteger.valueOf(1_622_505_600_000L);
        Date expected = new Date(bi.longValueExact());
        Date actual = InstantConversions.toDate(bi, converter);
        assertEquals(expected.getTime(), actual.getTime(), "BigInteger should be converted via longValueExact");
    }

    @Test
    void toDateFromAtomicLong() {
        AtomicLong al = new AtomicLong(1_622_505_600_000L);
        Date expected = new Date(al.get());
        Date actual = InstantConversions.toDate(al, converter);
        assertEquals(expected.getTime(), actual.getTime(), "AtomicLong should be converted using its long value");
    }

    @Test
    void toDateFromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.parse("2021-06-01T12:00:00+02:00");
        Date expected = Date.from(odt.toInstant());
        Date actual = InstantConversions.toDate(odt, converter);
        assertEquals(expected.getTime(), actual.getTime(), "OffsetDateTime should be converted via its instant");
    }

    @Test
    void toDateFromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.parse("2021-06-01T12:00:00+02:00[Europe/Paris]");
        Date expected = Date.from(zdt.toInstant());
        Date actual = InstantConversions.toDate(zdt, converter);
        assertEquals(expected.getTime(), actual.getTime(), "ZonedDateTime should be converted via its instant");
    }

    @Test
    void toDateFromSqlDate() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2021-06-01");
        Date expected = new Date(sqlDate.getTime());
        Date actual = InstantConversions.toDate(sqlDate, converter);
        assertEquals(expected.getTime(), actual.getTime(), "java.sql.Date should be treated as a normal Date (its epoch millis unchanged)");
    }

    @Test
    void toDateFromCalendarWithDifferentZone() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+05:30"));
        cal.setTimeInMillis(1_622_505_600_000L);
        Date expected = new Date(1_622_505_600_000L);
        Date actual = InstantConversions.toDate(cal, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Calendar's internal epoch millis are absolute; zone does not affect the result");
    }

    @Test
    void toDateFromOffsetTime() {
        // OffsetTime is combined with the *current* date in the default zone (UTC for this test)
        OffsetTime ot = OffsetTime.of(12, 30, 0, 0, ZoneOffset.ofHours(2));
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant expectedInstant = ot.atDate(today).toInstant();
        Date expected = Date.from(expectedInstant);
        Date actual = InstantConversions.toDate(ot, converter);
        assertEquals(expected.getTime(), actual.getTime(), "OffsetTime should be combined with today’s date and converted to an instant");
    }

    @Test
    void toDateFromAtomicInteger() {
        AtomicInteger ai = new AtomicInteger(123_456);
        Date expected = new Date(ai.get());
        Date actual = InstantConversions.toDate(ai, converter);
        assertEquals(expected.getTime(), actual.getTime(), "AtomicInteger should be converted using its int value as milliseconds");
    }

    @Test
    void toDateFromLongWrapper() {
        Long wrapper = Long.valueOf(1_622_505_600_000L);
        Date expected = new Date(wrapper);
        Date actual = InstantConversions.toDate(wrapper, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Long wrapper should be treated as a numeric millisecond value directly");
    }

    @Test
    void toDateFromIntegerWrapper() {
        Integer wrapper = Integer.valueOf(1_622_505_600);
        Date expected = new Date(wrapper.longValue());
        Date actual = InstantConversions.toDate(wrapper, converter);
        assertEquals(expected.getTime(), actual.getTime(), "Integer wrapper should be treated as a numeric millisecond value directly");
    }

    @Test
    void toDateFromBigDecimalWithFractionThrows() {
        BigDecimal bd = new BigDecimal("1622505600000.5");
        assertThrows(ArithmeticException.class, () -> InstantConversions.toDate(bd, converter), "Fractional BigDecimal should cause longValueExact to throw ArithmeticException");
    }

    @Test
    void toDateFromNonNumericStringThrows() {
        String bad = "notANumber";
        assertThrows(NumberFormatException.class, () -> InstantConversions.toDate(bad, converter), "Non‑numeric String should cause NumberFormatException");
    }

    @Test
    void toDateFromUnsupportedObjectFallsBackToZero() {
        Object unsupported = new Object();
        Date actual = InstantConversions.toDate(unsupported, converter);
        assertEquals(0L, actual.getTime(), "Unsupported type with no converter should result in epoch‑zero Date");
    }

    @Test
    void toDateWithCustomConverterReturningFixedValue(@Mock Converter custom) {
        // Simulate a converter that, for any input, returns the fixed epoch millis 1622505600000L.
        when(custom.convert(any(), eq(Long.class))).thenReturn(1622505600000L);
        Date actual = InstantConversions.toDate(new Object(), custom);
        assertEquals(1622505600000L, actual.getTime(), "Custom Converter should dictate the epoch millis used for the resulting Date");
    }

    @Test
    void toDateWithCustomConverterReturningForNull(@Mock Converter custom) {
        // Custom converter overrides the default null handling.
        when(custom.convert(isNull(), eq(Long.class))).thenReturn(12_345L);
        Date actual = InstantConversions.toDate(null, custom);
        assertEquals(12_345L, actual.getTime(), "Custom Converter should be consulted even when the source value is null");
    }

    @Test
    void toDateWithCustomConverterThrowingRuntimeException(@Mock Converter custom) {
        when(custom.convert(any(), eq(Long.class))).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> InstantConversions.toDate(new Object(), custom), "RuntimeException from the custom Converter must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toDateWithCustomConverterThrowingCheckedException(@Mock Converter custom) throws Exception {
        // Mockito cannot throw checked exceptions without declaring them; we wrap in RuntimeException.
        when(custom.convert(any(), eq(Long.class))).thenThrow(new Exception("checked"));
        Exception ex = assertThrows(Exception.class, () -> InstantConversions.toDate(null, custom), "Checked exception from the custom Converter must propagate");
        assertEquals("checked", ex.getMessage());
    }
}
