package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toInstant_Test {

    private static Converter converter;

    /**
     * Simple {@link Converter} that always throws an {@link IllegalArgumentException}.
     */
    private static final class ThrowingConverter extends Converter {

        ThrowingConverter() {
            super(new ConverterOptions());
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T convert(Object src, Class<T> target) {
            throw new IllegalArgumentException("conversion failed");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter with default options (no special overrides)
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference
        converter = null;
    }

    private static Stream<Arguments> directInstantProvider() {
        return Stream.of(Arguments.of(Instant.parse("2023-05-01T12:34:56Z")));
    }

    private static Stream<Arguments> localDateTimeProvider() {
        return Stream.of(Arguments.of(LocalDateTime.of(2023, 5, 1, 12, 34, 56)));
    }

    private static Stream<Arguments> dateProvider() {
        return Stream.of(// 2023‑03‑01T00:00:00Z
        Arguments.of(new Date(1_680_000_000_000L)));
    }

    private static Stream<Arguments> calendarProvider() {
        return Stream.of(Arguments.of(createUtcCalendar(1_680_000_000_000L)));
    }

    private static Stream<Arguments> epochLongProvider() {
        return Stream.of(Arguments.of(1_680_000_000_000L));
    }

    private static Stream<Arguments> isoStringProvider() {
        return Stream.of(Arguments.of("2023-05-01T12:34:56Z"));
    }

    private static Stream<Arguments> zonedDateTimeProvider() {
        return Stream.of(Arguments.of(ZonedDateTime.of(2023, 5, 1, 12, 34, 56, 0, ZoneId.of("UTC"))));
    }

    private static Stream<Arguments> offsetDateTimeProvider() {
        return Stream.of(Arguments.of(OffsetDateTime.of(2023, 5, 1, 12, 34, 56, 0, ZoneOffset.ofHours(2))));
    }

    private static Stream<Arguments> timestampProvider() {
        return Stream.of(Arguments.of(Timestamp.from(Instant.parse("2023-05-01T12:34:56Z"))));
    }

    private static Stream<Arguments> sqlDateProvider() {
        return Stream.of(Arguments.of(java.sql.Date.valueOf(LocalDate.of(2023, 5, 1))));
    }

    private static Stream<Arguments> yearMonthProvider() {
        return Stream.of(Arguments.of(YearMonth.of(2023, 5)));
    }

    private static Stream<Arguments> yearProvider() {
        return Stream.of(Arguments.of(Year.of(2023)));
    }

    private static Stream<Arguments> atomicLongProvider() {
        return Stream.of(Arguments.of(new AtomicLong(1_680_000_000_000L)));
    }

    private static Stream<Arguments> bigDecimalProvider() {
        return Stream.of(Arguments.of(new BigDecimal("1680000000000")));
    }

    private static Stream<Arguments> bigIntegerProvider() {
        return Stream.of(Arguments.of(new BigInteger("1680000000000")));
    }

    private static Stream<Arguments> epochZeroProvider() {
        return Stream.of(Arguments.of(0L));
    }

    private static Stream<Arguments> negativeEpochProvider() {
        return Stream.of(Arguments.of(-1_000L));
    }

    private static Stream<Arguments> stringWithOffsetProvider() {
        return Stream.of(Arguments.of("2023-05-01T12:34:56+02:00"));
    }

    private static Stream<Arguments> mapWithTimeKeyProvider() {
        return Stream.of(Arguments.of(Collections.singletonMap("time", 1_680_000_000_000L)));
    }

    private static Calendar createUtcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Verify that a {@link LocalDateTime} is converted to an {@link Instant}
     * using the system default zone offset.
     */
    @Test
    void toInstant_fromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, Month.MARCH, 15, 10, 30, 45);
        Instant expected = ldt.toInstant(ZoneOffset.UTC);
        Instant actual = LocalDateTimeConversions.toInstant(ldt, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify conversion from a {@link Date} (java.util) to {@link Instant}.
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
     * Verify conversion from a {@link Long} representing epoch‑millis.
     */
    @Test
    void toInstant_fromLong() {
        long epochMillis = 1_640_995_200_000L;
        Instant expected = Instant.ofEpochMilli(epochMillis);
        Instant actual = LocalDateTimeConversions.toInstant(epochMillis, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify conversion from an ISO‑8601 {@link String}.
     */
    @Test
    void toInstant_fromString() {
        String iso = "2023-07-20T15:45:30Z";
        Instant expected = Instant.parse(iso);
        Instant actual = LocalDateTimeConversions.toInstant(iso, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify conversion from a {@link Map} with the keys used by the
     * {@code Converter} for date/time values.
     */
    @Test
    void toInstant_fromMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        // epoch millis
        map.put("time", 1_640_995_200_000L);
        Instant expected = Instant.ofEpochMilli(1_640_995_200_000L);
        Instant actual = LocalDateTimeConversions.toInstant(map, converter);
        assertEquals(expected, actual);
    }

    /**
     * Verify that a {@code null} source returns {@code null}.
     */
    @Test
    void toInstant_fromNull() {
        assertNull(LocalDateTimeConversions.toInstant(null, converter));
    }

    @ParameterizedTest(name = "Direct Instant should be returned unchanged")
    @MethodSource("directInstantProvider")
    void toInstant_directInstant(Instant source) {
        Instant actual = LocalDateTimeConversions.toInstant(source, null);
        assertSame(source, actual, "Instant instance should be returned unchanged");
    }

    @ParameterizedTest(name = "java.util.Calendar → Instant")
    @MethodSource("calendarProvider")
    void toInstant_fromCalendar(Calendar calendar) {
        Instant expected = Instant.ofEpochMilli(calendar.getTimeInMillis());
        Instant actual = LocalDateTimeConversions.toInstant(calendar, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "ZonedDateTime → Instant")
    @MethodSource("zonedDateTimeProvider")
    void toInstant_fromZonedDateTime(ZonedDateTime zdt) {
        Instant expected = zdt.toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(zdt, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "OffsetDateTime → Instant (normalised to UTC)")
    @MethodSource("offsetDateTimeProvider")
    void toInstant_fromOffsetDateTime(OffsetDateTime odt) {
        Instant expected = odt.toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(odt, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "java.sql.Timestamp → Instant")
    @MethodSource("timestampProvider")
    void toInstant_fromTimestamp(Timestamp ts) {
        Instant expected = ts.toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(ts, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "java.sql.Date → Instant (start of day in system default zone)")
    @MethodSource("sqlDateProvider")
    void toInstant_fromSqlDate(java.sql.Date sqlDate) {
        Instant expected = sqlDate.toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(sqlDate, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "YearMonth → Instant (first day at start of day)")
    @MethodSource("yearMonthProvider")
    void toInstant_fromYearMonth(YearMonth ym) {
        Instant expected = ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(ym, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "Year → Instant (first day at start of day)")
    @MethodSource("yearProvider")
    void toInstant_fromYear(Year year) {
        Instant expected = year.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant actual = LocalDateTimeConversions.toInstant(year, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "AtomicLong (epoch‑millis) → Instant")
    @MethodSource("atomicLongProvider")
    void toInstant_fromAtomicLong(AtomicLong atomic) {
        Instant expected = Instant.ofEpochMilli(atomic.get());
        Instant actual = LocalDateTimeConversions.toInstant(atomic, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "BigDecimal (epoch‑millis) → Instant")
    @MethodSource("bigDecimalProvider")
    void toInstant_fromBigDecimal(BigDecimal bd) {
        Instant expected = Instant.ofEpochMilli(bd.longValueExact());
        Instant actual = LocalDateTimeConversions.toInstant(bd, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "BigInteger (epoch‑millis) → Instant")
    @MethodSource("bigIntegerProvider")
    void toInstant_fromBigInteger(BigInteger bi) {
        Instant expected = Instant.ofEpochMilli(bi.longValueExact());
        Instant actual = LocalDateTimeConversions.toInstant(bi, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "Epoch zero → Instant.EPOCH")
    @MethodSource("epochZeroProvider")
    void toInstant_epochZero(Long zero) {
        Instant actual = LocalDateTimeConversions.toInstant(zero, null);
        assertEquals(Instant.EPOCH, actual);
    }

    @ParameterizedTest(name = "Negative epoch milliseconds")
    @MethodSource("negativeEpochProvider")
    void toInstant_negativeEpoch(Long negative) {
        Instant expected = Instant.ofEpochMilli(negative);
        Instant actual = LocalDateTimeConversions.toInstant(negative, null);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "String with explicit offset")
    @MethodSource("stringWithOffsetProvider")
    void toInstant_stringWithExplicitOffset(String isoWithOffset) {
        // normalised to UTC
        Instant expected = Instant.parse("2023-05-01T10:34:56Z");
        Instant actual = LocalDateTimeConversions.toInstant(isoWithOffset, null);
        assertEquals(expected, actual);
    }

    @Test
    void toInstant_invalidStringFormat() {
        String bad = "2023-05-01 12:34:56";
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toInstant(bad, null));
    }

    @Test
    void toInstant_emptyString() {
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toInstant("", null));
    }

    @Test
    void toInstant_whitespaceString() {
        assertThrows(DateTimeParseException.class, () -> LocalDateTimeConversions.toInstant("   ", null));
    }

    @Test
    void toInstant_unsupportedNumericDouble() {
        Double d = 12_345.67;
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toInstant(d, null));
    }

    @Test
    void toInstant_unsupportedBoolean() {
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toInstant(Boolean.TRUE, null));
    }

    @Test
    void toInstant_unsupportedList() {
        List<Integer> list = Collections.singletonList(1);
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toInstant(list, null));
    }

    @Test
    void toInstant_unsupportedSet() {
        Set<Integer> set = Collections.singleton(1);
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toInstant(set, null));
    }

    @Test
    void toInstant_unsupportedMap() {
        Map<String, String> map = Collections.singletonMap("key", "value");
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toInstant(map, null));
    }

    @ParameterizedTest(name = "Map with key \"time\" (epoch‑millis) → Instant")
    @MethodSource("mapWithTimeKeyProvider")
    void toInstant_fromMapWithTimeKey(Map<String, Object> map) {
        Instant expected = Instant.ofEpochMilli(((Number) map.get("time")).longValue());
        Instant actual = LocalDateTimeConversions.toInstant(map, null);
        assertEquals(expected, actual);
    }

    @Test
    void toInstant_nullSourceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toInstant(null, null));
    }

    @Test
    void toInstant_converterProvidedButIgnoredWhenSourceIsInstant() {
        Instant src = Instant.parse("2023-05-01T12:34:56Z");
        Converter throwing = new ThrowingConverter();
        // The method should return the source instantly without delegating to the converter.
        Instant actual = LocalDateTimeConversions.toInstant(src, throwing);
        assertEquals(src, actual);
    }

    @Test
    void toInstant_converterThrowsExceptionIsPropagated() {
        String unsupported = "unsupported";
        Converter throwing = new ThrowingConverter();
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toInstant(unsupported, throwing));
    }
}
