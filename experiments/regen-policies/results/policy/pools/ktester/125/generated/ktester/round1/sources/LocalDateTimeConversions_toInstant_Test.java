package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toInstant_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; use the default constructor
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference
        converter = null;
    }

    private static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of(null, new Converter(new ConverterOptions())), Arguments.of(Instant.now(), null));
    }

    /**
     * Verify that a {@link LocalDateTime} is correctly converted to an {@link Instant}
     * using the system default zone offset.
     */
    @Test
    void toInstant_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MARCH, 15, 12, 30, 45);
        // The implementation uses toZonedDateTime(...).toInstant()
        // toZonedDateTime treats a plain LocalDateTime as being in the system default zone.
        // For reproducibility we force the default zone to UTC for the expectation.
        ZoneId original = ZoneId.systemDefault();
        try {
            // Ensure the test runs as if the default zone were UTC
            System.setProperty("user.timezone", "UTC");
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            Instant expected = ldt.toInstant(ZoneOffset.UTC);
            Instant actual = LocalDateTimeConversions.toInstant(ldt, converter);
            assertEquals(expected, actual, "Instant conversion from LocalDateTime should match UTC conversion");
        } finally {
            // Restore original default zone
            System.setProperty("user.timezone", original.getId());
            TimeZone.setDefault(TimeZone.getTimeZone(original));
        }
    }

    /**
     * Verify that a {@link java.util.Date} is correctly converted.
     */
    @Test
    void toInstant_fromDate() {
        // 2022-01-01T00:00:00Z
        Date date = new Date(1_640_995_200_000L);
        Instant expected = date.toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(date, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify that a {@link java.sql.Timestamp} is correctly converted.
     */
    @Test
    void toInstant_fromTimestamp() {
        Timestamp ts = Timestamp.valueOf("2021-12-31 23:59:59.123456789");
        Instant expected = ts.toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(ts, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify that a {@link java.time.ZonedDateTime} is correctly converted.
     */
    @Test
    void toInstant_fromZonedDateTime() {
        ZonedDateTime zdt = ZonedDateTime.of(2020, 5, 20, 10, 15, 30, 0, ZoneId.of("America/New_York"));
        Instant expected = zdt.toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(zdt, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify that a {@link java.time.OffsetDateTime} is correctly converted.
     */
    @Test
    void toInstant_fromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2020, 5, 20, 10, 15, 30, 0, ZoneOffset.ofHours(-4));
        Instant expected = odt.toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(odt, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify that a {@link java.time.LocalDate} (date without time) is converted to the start of the day UTC.
     */
    @Test
    void toInstant_fromLocalDate() {
        LocalDate ld = LocalDate.of(2022, 7, 4);
        Instant expected = ld.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(ld, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify that a {@link java.time.LocalTime} (time without date) is converted to today’s date at that time in UTC.
     */
    @Test
    void toInstant_fromLocalTime() {
        LocalTime lt = LocalTime.of(13, 45, 30);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        Instant expected = LocalDateTime.of(today, lt).toInstant(ZoneOffset.UTC);
        Instant actual = LocalDateTimeConversions.toInstant(lt, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify that a numeric epoch‑millisecond value (Long) is converted to an {@link Instant}.
     */
    @Test
    void toInstant_fromLong() {
        // 2020-09-13T12:26:40Z
        long epochMilli = 1_600_000_000_000L;
        Instant expected = Instant.ofEpochMilli(epochMilli);
        Instant actual = LocalDateTimeConversions.toInstant(epochMilli, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify that a numeric epoch‑second value (Integer) is converted to an {@link Instant}.
     */
    @Test
    void toInstant_fromInteger() {
        // 2020-09-13T12:26:40Z
        int epochSec = 1_600_000_000;
        Instant expected = Instant.ofEpochSecond(epochSec);
        Instant actual = LocalDateTimeConversions.toInstant(epochSec, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify that a {@link String} containing an ISO‑8601 instant is correctly parsed.
     */
    @Test
    void toInstant_fromIsoString() {
        String iso = "2021-08-15T10:20:30.123Z";
        Instant expected = Instant.parse(iso);
        Instant actual = LocalDateTimeConversions.toInstant(iso, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify that a {@link Map} with a "time" key (epoch‑millis) is converted.
     */
    @Test
    void toInstant_fromMapWithTimeKey() {
        Map<String, Object> map = new HashMap<>();
        map.put("time", 1_600_000_000_000L);
        Instant expected = Instant.ofEpochMilli(1_600_000_000_000L);
        Instant actual = LocalDateTimeConversions.toInstant(map, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify that passing {@code null} returns {@code null} (Converter’s null handling contract).
     */
    @Test
    void toInstant_fromNull() {
        assertNull(LocalDateTimeConversions.toInstant(null, converter));
    }

    /**
     * Null handling – either the source object or the converter is null.
     */
    @ParameterizedTest(name = "{index}: from={0}, converter={1}")
    @MethodSource("nullInputProvider")
    void toInstant_nullInputs(Object from, Converter conv) {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toInstant(from, conv), "Expected NullPointerException when either argument is null");
    }

    /**
     * When the source is already an {@link Instant}, the same instance should be returned.
     */
    @Test
    void toInstant_fromInstant() {
        // 2021-06-01T12:00:00Z
        Instant src = Instant.ofEpochSecond(1_622_548_800L);
        Instant result = LocalDateTimeConversions.toInstant(src, converter);
        assertSame(src, result, "Instant source should be returned unchanged");
    }

    /**
     * Non‑ISO string should cause a parsing exception.
     */
    @Test
    void toInstant_invalidStringFormat() {
        // missing 'T' and zone
        String bad = "2021-06-01 12:00:00";
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toInstant(bad, converter), "Parsing a non‑ISO string must throw DateTimeParseException");
    }

    /**
     * Unsupported source type should result in IllegalArgumentException.
     */
    @Test
    void toInstant_unsupportedType() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toInstant(unsupported, converter), "Unsupported source type must throw IllegalArgumentException");
    }

    /**
     * Epoch‑millis overflow – value larger than Instant.MAX.
     */
    @Test
    void toInstant_epochMillisOverflow() {
        // far beyond Instant.MAX
        long overflow = Long.MAX_VALUE;
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toInstant(overflow, converter), "Overflowing epoch‑millis should throw DateTimeException");
    }

    /**
     * Epoch‑millis underflow – value smaller than Instant.MIN.
     */
    @Test
    void toInstant_epochMillisUnderflow() {
        // far below Instant.MIN
        long underflow = Long.MIN_VALUE;
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toInstant(underflow, converter), "Underflowing epoch‑millis should throw DateTimeException");
    }

    /**
     * Converting a {@link LocalDate} without a time component is ambiguous and should
     * raise a DateTimeException (as defined by the library contract).
     */
    @Test
    void toInstant_localDateUnsupported() {
        LocalDate ld = LocalDate.of(2021, 6, 1);
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toInstant(ld, converter), "Converting LocalDate to Instant should throw DateTimeException");
    }

    /**
     * Converting a {@link LocalTime} without a date component should also raise a
     * DateTimeException.
     */
    @Test
    void toInstant_localTimeUnsupported() {
        LocalTime lt = LocalTime.of(12, 0, 0);
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toInstant(lt, converter), "Converting LocalTime to Instant should throw DateTimeException");
    }

    /**
     * OffsetDateTime conversion – verifies correct UTC adjustment.
     */
    @Test
    void toInstant_fromOffsetDateTime_offset() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneOffset.ofHours(-5));
        Instant expected = Instant.parse("2021-06-01T17:00:00Z");
        Instant actual = LocalDateTimeConversions.toInstant(odt, converter);
        assertEquals(expected, actual);
    }

    /**
     * ZonedDateTime conversion – verifies correct UTC adjustment for a non‑UTC zone.
     */
    @Test
    void toInstant_fromZonedDateTime_zone() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("Asia/Tokyo"));
        Instant expected = Instant.parse("2021-06-01T03:00:00Z");
        Instant actual = LocalDateTimeConversions.toInstant(zdt, converter);
        assertEquals(expected, actual);
    }

    /**
     * Calendar conversion – a Calendar set to UTC should map directly to the same Instant.
     */
    @Test
    void toInstant_fromCalendarUtc() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // 2021-06-01T12:00:00Z
        cal.setTimeInMillis(1_622_548_800_000L);
        Instant expected = Instant.ofEpochMilli(1_622_548_800_000L);
        Instant actual = LocalDateTimeConversions.toInstant(cal, converter);
        assertEquals(expected, actual);
    }
}
