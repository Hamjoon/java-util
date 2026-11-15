package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toCalendar_Test {

    /**
     * Converter used for all tests – created once with a deterministic time‑zone.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a fixed time‑zone so that the conversion result is predictable.
        ConverterOptions options = new ConverterOptions();
        options.setTimeZone(TimeZone.getTimeZone("UTC"));
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /**
     * Creates a mock {@link Converter} that returns a {@link ConverterOptions}
     *  whose {@code getTimeZone()} returns the supplied {@code tz}.
     */
    private static Converter mockConverter(TimeZone tz) {
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getTimeZone()).thenReturn(tz);
        return converter;
    }

    /**
     * Asserts that a {@link Calendar} contains the expected epoch millis and time‑zone.
     */
    private static void assertCalendar(long expectedEpochMillis, TimeZone expectedTZ, Calendar actual) {
        assertEquals(expectedEpochMillis, actual.getTimeInMillis(), "Calendar epoch millis must match the expected value");
        assertEquals(expectedTZ, actual.getTimeZone(), "Calendar time‑zone must be the one supplied by the Converter");
    }

    // ---------- Valid conversions with default UTC time zone ----------
    private static Stream<Arguments> utcValidConversions() {
        return Stream.of(// Date (already covered above, but kept for completeness)
        Arguments.of(new Date(1_600_000_000_000L), "UTC", 1_600_000_000_000L), // LocalDateTime – treated as system default zone, then forced to UTC
        Arguments.of(LocalDateTime.of(2020, 9, 13, 12, 30, 45), "UTC", LocalDateTime.of(2020, 9, 13, 12, 30, 45).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()), // ZonedDateTime – instant must be preserved
        Arguments.of(ZonedDateTime.of(2021, 3, 14, 2, 30, 0, 0, ZoneId.of("America/New_York")), "UTC", ZonedDateTime.of(2021, 3, 14, 2, 30, 0, 0, ZoneId.of("America/New_York")).toInstant().toEpochMilli()));
    }

    @Test
    void toCalendar_fromDate_returnsSameInstantAndTimeZone() {
        // given
        // 2021‑10‑01T00:00:00Z
        long epochMillis = 1633072800000L;
        Date date = new Date(epochMillis);
        // when
        Calendar calendar = LocalDateTimeConversions.toCalendar(date, converter);
        // then
        assertEquals(epochMillis, calendar.getTimeInMillis(), "Calendar should contain the same epoch milliseconds as the source Date");
        assertEquals(TimeZone.getTimeZone("UTC"), calendar.getTimeZone(), "Calendar should use the Converter's configured time‑zone (UTC)");
    }

    @Test
    void toCalendar_fromLocalDateTime_convertsUsingConfiguredZone() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2021, Month.OCTOBER, 1, 12, 30, 15);
        ZonedDateTime expectedZdt = ldt.atZone(ZoneId.of("UTC"));
        long expectedEpoch = expectedZdt.toInstant().toEpochMilli();
        // when
        Calendar calendar = LocalDateTimeConversions.toCalendar(ldt, converter);
        // then
        assertEquals(expectedEpoch, calendar.getTimeInMillis(), "Calendar epoch should match the ZonedDateTime derived from the LocalDateTime in UTC");
        assertEquals(TimeZone.getTimeZone("UTC"), calendar.getTimeZone(), "Calendar should use the Converter's configured time‑zone (UTC)");
    }

    @Test
    void toCalendar_fromZonedDateTime_preservesInstant() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(2022, 3, 15, 8, 45, 0, 0, ZoneId.of("America/New_York"));
        long expectedEpoch = zdt.toInstant().toEpochMilli();
        // when
        Calendar calendar = LocalDateTimeConversions.toCalendar(zdt, converter);
        // then
        assertEquals(expectedEpoch, calendar.getTimeInMillis(), "Calendar should contain the same instant as the source ZonedDateTime");
        // The calendar's zone is taken from the Converter (UTC), not from the source.
        assertEquals(TimeZone.getTimeZone("UTC"), calendar.getTimeZone(), "Calendar should use the Converter's configured time‑zone (UTC)");
    }

    @Test
    void toCalendar_fromInstant_convertsCorrectly() {
        // given
        Instant instant = Instant.parse("2023-01-01T00:00:00Z");
        long expectedEpoch = instant.toEpochMilli();
        // when
        Calendar calendar = LocalDateTimeConversions.toCalendar(instant, converter);
        // then
        assertEquals(expectedEpoch, calendar.getTimeInMillis(), "Calendar epoch should match the source Instant");
        assertEquals(TimeZone.getTimeZone("UTC"), calendar.getTimeZone(), "Calendar should use the Converter's configured time‑zone (UTC)");
    }

    @Test
    void toCalendar_fromSqlTimestamp_convertsToSameInstant() {
        // given
        Timestamp ts = Timestamp.valueOf("2024-05-20 14:22:33");
        long expectedEpoch = ts.toInstant().toEpochMilli();
        // when
        Calendar calendar = LocalDateTimeConversions.toCalendar(ts, converter);
        // then
        assertEquals(expectedEpoch, calendar.getTimeInMillis(), "Calendar epoch should match the Timestamp's instant");
        assertEquals(TimeZone.getTimeZone("UTC"), calendar.getTimeZone(), "Calendar should use the Converter's configured time‑zone (UTC)");
    }

    @Test
    void toCalendar_fromMapWithTimeKey_convertsCorrectly() {
        // given – a map representation that the Converter understands for date/time types
        Map<String, Object> map = new LinkedHashMap<>();
        // 2023‑01‑01T00:00:00Z
        long epochMillis = 1672531200000L;
        map.put("time", epochMillis);
        // when
        Calendar calendar = LocalDateTimeConversions.toCalendar(map, converter);
        // then
        assertEquals(epochMillis, calendar.getTimeInMillis(), "Calendar should reflect the 'time' value supplied in the map");
        assertEquals(TimeZone.getTimeZone("UTC"), calendar.getTimeZone(), "Calendar should use the Converter's configured time‑zone (UTC)");
    }

    @ParameterizedTest
    @MethodSource("utcValidConversions")
    void toCalendar_validConversionsWithUtc(Object source, String tzId, long expectedEpoch) {
        Converter converter = mockConverter(TimeZone.getTimeZone(tzId));
        Calendar calendar = LocalDateTimeConversions.toCalendar(source, converter);
        assertCalendar(expectedEpoch, TimeZone.getTimeZone(tzId), calendar);
    }

    // ---------- Conversion from Instant with custom zone ----------
    @Test
    void toCalendar_fromInstant_customTimeZone() {
        Instant instant = Instant.ofEpochMilli(1_600_000_000_000L);
        TimeZone tz = TimeZone.getTimeZone("America/Los_Angeles");
        Converter converter = mockConverter(tz);
        Calendar calendar = LocalDateTimeConversions.toCalendar(instant, converter);
        assertCalendar(1_600_000_000_000L, tz, calendar);
    }

    // ---------- Conversion from OffsetDateTime ----------
    @Test
    void toCalendar_fromOffsetDateTime() {
        OffsetDateTime odt = OffsetDateTime.of(2020, 9, 13, 7, 26, 40, 0, ZoneOffset.ofHours(-5));
        // 1600000000000L
        long expectedEpoch = odt.toInstant().toEpochMilli();
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        Calendar calendar = LocalDateTimeConversions.toCalendar(odt, converter);
        assertCalendar(expectedEpoch, TimeZone.getTimeZone("UTC"), calendar);
    }

    // ---------- Conversion from LocalDate ----------
    @Test
    void toCalendar_fromLocalDate() {
        LocalDate ld = LocalDate.of(2020, 9, 13);
        // start of day in UTC
        long expectedEpoch = ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        Calendar calendar = LocalDateTimeConversions.toCalendar(ld, converter);
        assertCalendar(expectedEpoch, TimeZone.getTimeZone("UTC"), calendar);
    }

    // ---------- Conversion from YearMonth ----------
    @Test
    void toCalendar_fromYearMonth() {
        YearMonth ym = YearMonth.of(2020, 9);
        long expectedEpoch = ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        Calendar calendar = LocalDateTimeConversions.toCalendar(ym, converter);
        assertCalendar(expectedEpoch, TimeZone.getTimeZone("UTC"), calendar);
    }

    // ---------- Conversion from Year ----------
    @Test
    void toCalendar_fromYear() {
        Year y = Year.of(2020);
        long expectedEpoch = y.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        Calendar calendar = LocalDateTimeConversions.toCalendar(y, converter);
        assertCalendar(expectedEpoch, TimeZone.getTimeZone("UTC"), calendar);
    }

    // ---------- Conversion from AtomicLong ----------
    @Test
    void toCalendar_fromAtomicLong() {
        AtomicLong al = new AtomicLong(1_600_000_000_000L);
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        Calendar calendar = LocalDateTimeConversions.toCalendar(al, converter);
        assertCalendar(1_600_000_000_000L, TimeZone.getTimeZone("UTC"), calendar);
    }

    // ---------- Conversion from BigDecimal ----------
    @Test
    void toCalendar_fromBigDecimal() {
        BigDecimal bd = new BigDecimal("1600000000000");
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        Calendar calendar = LocalDateTimeConversions.toCalendar(bd, converter);
        assertCalendar(1_600_000_000_000L, TimeZone.getTimeZone("UTC"), calendar);
    }

    // ---------- Conversion from BigInteger ----------
    @Test
    void toCalendar_fromBigInteger() {
        BigInteger bi = new BigInteger("1600000000000");
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        Calendar calendar = LocalDateTimeConversions.toCalendar(bi, converter);
        assertCalendar(1_600_000_000_000L, TimeZone.getTimeZone("UTC"), calendar);
    }

    // ---------- Custom time zone conversion ----------
    @Test
    void toCalendar_customTimeZoneConversion() {
        ZonedDateTime source = ZonedDateTime.of(2021, 3, 14, 2, 30, 0, 0, ZoneId.of("America/New_York"));
        // 1615708200000L
        long expectedEpoch = source.toInstant().toEpochMilli();
        TimeZone targetTZ = TimeZone.getTimeZone("America/Los_Angeles");
        Converter converter = mockConverter(targetTZ);
        Calendar calendar = LocalDateTimeConversions.toCalendar(source, converter);
        assertCalendar(expectedEpoch, targetTZ, calendar);
    }

    // ---------- Null source handling ----------
    @Test
    void toCalendar_nullSource_throwsNPE() {
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toCalendar(null, converter), "Null source should result in NullPointerException");
    }

    // ---------- Null converter handling ----------
    @Test
    void toCalendar_nullConverter_throwsNPE() {
        Date date = new Date(0L);
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toCalendar(date, null), "Null converter should result in NullPointerException");
    }

    // ---------- Converter with null options ----------
    @Test
    void toCalendar_converterWithNullOptions_throwsNPE() {
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(null);
        Date date = new Date(0L);
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toCalendar(date, converter), "Converter returning null options should cause NPE");
    }

    // ---------- Converter with null time zone ----------
    @Test
    void toCalendar_converterWithNullTimeZone_throwsNPE() {
        ConverterOptions options = mock(ConverterOptions.class);
        when(options.getTimeZone()).thenReturn(null);
        Converter converter = mock(Converter.class);
        when(converter.getOptions()).thenReturn(options);
        Date date = new Date(0L);
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toCalendar(date, converter), "Converter with null TimeZone should cause NPE when Calendar.getInstance(null) is called");
    }

    // ---------- Unsupported source type ----------
    @Test
    void toCalendar_unsupportedSource_throwsIllegalArgumentException() {
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toCalendar(Boolean.TRUE, converter), "Unsupported source type should result in IllegalArgumentException");
    }

    // ---------- Invalid string format ----------
    @Test
    void toCalendar_invalidString_throwsDateTimeParseException() {
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        // month 13 is illegal
        String badIso = "2020-13-01T00:00:00";
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toCalendar(badIso, converter), "Malformed ISO‑8601 string should cause DateTimeParseException");
    }

    // ---------- Non‑integral BigDecimal ----------
    @Test
    void toCalendar_nonIntegralBigDecimal_throwsIllegalArgumentException() {
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        BigDecimal fractional = new BigDecimal("1600.5");
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toCalendar(fractional, converter), "BigDecimal with fractional milliseconds should cause IllegalArgumentException");
    }

    // ---------- BigInteger out of long range ----------
    @Test
    void toCalendar_bigIntegerOutOfRange_throwsIllegalArgumentException() {
        Converter converter = mockConverter(TimeZone.getTimeZone("UTC"));
        // Long.MAX_VALUE + 1
        BigInteger tooBig = new BigInteger("9223372036854775808");
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toCalendar(tooBig, converter), "BigInteger exceeding Long.MAX_VALUE should cause IllegalArgumentException");
    }
}
