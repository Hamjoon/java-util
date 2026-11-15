package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicLong;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toLong_Test {

    /**
     * The {@link Converter} instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long‑running test suites.
        converter = null;
    }

    /**
     * Verify that a {@link java.util.Date} is converted to its epoch‑millisecond value.
     */
    @Test
    void toLong_givenDate_returnsEpochMilli() {
        // GIVEN
        // a fixed point in time
        long expected = 1_620_000_000_000L;
        Date source = new Date(expected);
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual, "The long value returned for a Date should match its epoch milli.");
    }

    /**
     * Verify that a {@link Long} value is returned unchanged.
     */
    @Test
    void toLong_givenLong_returnsSameValue() {
        // GIVEN
        Long source = 9_876_543_210L;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(source.longValue(), actual, "When the source is already a Long, toLong should return the same value.");
    }

    @Test
    void toLong_givenInstant_returnsEpochMilli() {
        // GIVEN
        Instant source = Instant.ofEpochMilli(1_622_548_800_000L);
        long expected = 1_622_548_800_000L;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenCalendar_returnsEpochMilli() {
        // GIVEN
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(1_622_548_800_000L);
        long expected = 1_622_548_800_000L;
        // WHEN
        long actual = LocalDateConversions.toLong(calendar, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenAtomicLong_returnsValue() {
        // GIVEN
        AtomicLong source = new AtomicLong(1_622_548_800_000L);
        long expected = 1_622_548_800_000L;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenBigDecimal_returnsValue() {
        // GIVEN
        BigDecimal source = new BigDecimal("1622548800000");
        long expected = 1_622_548_800_000L;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenBigInteger_returnsValue() {
        // GIVEN
        BigInteger source = new BigInteger("1622548800000");
        long expected = 1_622_548_800_000L;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenIsoString_returnsEpochMilli() {
        // GIVEN
        String source = "2021-06-01T12:00:00Z";
        // 2021‑06‑01T12:00:00Z in epoch ms
        long expected = 1_622_558_400_000L;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenLocalDateTimeAssumedUtc_returnsEpochMilli() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2021, 6, 1, 12, 0, 0);
        // interpreted as UTC
        long expected = 1_622_558_400_000L;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenZonedDateTime_returnsEpochMilli() {
        // GIVEN
        ZonedDateTime source = ZonedDateTime.of(2021, 6, 1, 8, 0, 0, 0, ZoneId.of("America/New_York"));
        // 2021‑06‑01T12:00:00Z
        long expected = 1_622_558_400_000L;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenOffsetDateTime_returnsEpochMilli() {
        // GIVEN
        OffsetDateTime source = OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, // +02:00
        ZoneOffset.ofHours(2));
        // 2021‑06‑01T10:00:00Z
        long expected = 1_622_551_200_000L;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenTimestamp_returnsEpochMilli() {
        // GIVEN
        Timestamp source = new Timestamp(1_622_548_800_000L);
        long expected = 1_622_548_800_000L;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenNegativeInstant_returnsNegativeEpochMilli() {
        // GIVEN
        Instant source = Instant.ofEpochMilli(-1L);
        long expected = -1L;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenMaximumLongInstant_returnsMaximumLong() {
        // GIVEN
        Instant source = Instant.ofEpochMilli(Long.MAX_VALUE);
        long expected = Long.MAX_VALUE;
        // WHEN
        long actual = LocalDateConversions.toLong(source, converter);
        // THEN
        assertEquals(expected, actual);
    }

    @Test
    void toLong_givenNullSource_throwsIllegalArgumentException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toLong(source, converter));
    }

    @Test
    void toLong_givenNullConverter_throwsIllegalArgumentException() {
        // GIVEN
        Instant source = Instant.ofEpochMilli(1_622_548_800_000L);
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toLong(source, nullConverter));
    }

    @Test
    void toLong_givenUnsupportedType_throwsIllegalArgumentException() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toLong(source, converter));
    }

    @Test
    void toLong_givenMalformedIsoString_throwsIllegalArgumentException() {
        // GIVEN
        // missing 'T' and zone
        String source = "2021-06-01 12:00:00";
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toLong(source, converter));
    }

    @Test
    void toLong_givenInvalidDateString_throwsIllegalArgumentException() {
        // GIVEN
        // February 30 does not exist
        String source = "2021-02-30T00:00:00Z";
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toLong(source, converter));
    }
}
