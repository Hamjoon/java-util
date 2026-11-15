package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.format.DateTimeParseException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toTimestamp_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter needs a ConverterOptions instance – the default constructor provides a sane default
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
        // default ConverterOptions is sufficient for these tests
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> numericInputs() {
        return Stream.of(// Long input
        Arguments.of(1622548800000L, new Timestamp(1622548800000L)), // Integer input (auto‑boxed to Integer)
        Arguments.of(12345, new Timestamp(12345L)), // String numeric input
        Arguments.of("1622548800000", new Timestamp(1622548800000L)));
    }

    private static Stream<Arguments> dateAndCalendarInputs() {
        return Stream.of(// java.util.Date
        Arguments.of(new Date(1622548800000L), new Timestamp(1622548800000L)), // java.sql.Date (subclass of java.util.Date)
        Arguments.of(new java.sql.Date(1622548800000L), new Timestamp(1622548800000L)), // Calendar in UTC
        Arguments.of(createUtcCalendar(1622548800000L), new Timestamp(1622548800000L)));
    }

    private static Calendar createUtcCalendar(long epochMillis) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    private static Stream<Arguments> instantAndTemporalInputs() {
        return Stream.of(// Instant
        Arguments.of(Instant.ofEpochMilli(1622548800000L), Timestamp.from(Instant.ofEpochMilli(1622548800000L))), // LocalDateTime interpreted as UTC (library treats it as epoch‑millis in UTC)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 12, 0, 0), Timestamp.valueOf(LocalDateTime.of(2021, 6, 1, 12, 0, 0))), // LocalDate → start of day UTC
        Arguments.of(LocalDate.of(2021, 6, 1), new Timestamp(1622505600000L)), // YearMonth → first day of month start of day UTC
        Arguments.of(YearMonth.of(2021, 6), new Timestamp(1622505600000L)), // Year → Jan 1st start of day UTC
        Arguments.of(Year.of(2021), new Timestamp(1609459200000L)), // MonthDay → current year at start of day UTC
        Arguments.of(MonthDay.of(6, 1), new Timestamp(Year.now().atMonth(6).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())), // ZonedDateTime with +02:00 offset
        Arguments.of(ZonedDateTime.parse("2021-06-01T12:00:00+02:00[Europe/Paris]"), new Timestamp(1622541600000L)), // OffsetDateTime with +02:00 offset
        Arguments.of(OffsetDateTime.parse("2021-06-01T12:00:00+02:00"), new Timestamp(1622541600000L)));
    }

    private static Stream<Arguments> stringDateFormats() {
        return Stream.of(// ISO‑8601 UTC
        Arguments.of("2021-06-01T12:00:00Z", new Timestamp(1622548800000L)), // Custom format "yyyy-MM-dd HH:mm:ss"
        Arguments.of("2021-06-01 12:00:00", new Timestamp(1622548800000L)), // Numeric string (already covered in numericInputs but kept for completeness)
        Arguments.of("1622548800000", new Timestamp(1622548800000L)));
    }

    private static Stream<Arguments> bigNumberInputs() {
        return Stream.of(Arguments.of(new BigDecimal("1622548800000"), new Timestamp(1622548800000L)), Arguments.of(new BigInteger("1622548800000"), new Timestamp(1622548800000L)));
    }

    private static Stream<Arguments> atomicLongInput() {
        return Stream.of(Arguments.of(new AtomicLong(1622548800000L), new Timestamp(1622548800000L)));
    }

    private static Stream<Arguments> nullAndInvalidInputs() {
        return Stream.of(// Null source – expect NullPointerException as per library contract
        Arguments.of(null, NullPointerException.class), // Unsupported type – expect IllegalArgumentException
        Arguments.of(new Object(), IllegalArgumentException.class));
    }

    private static Stream<Arguments> invalidStringFormats() {
        return Stream.of(Arguments.of("2021-13-01", DateTimeParseException.class), Arguments.of("not-a-date", DateTimeParseException.class));
    }

    private static Stream<Arguments> numericOverflowAndFormatErrors() {
        return Stream.of(// String larger than Long.MAX_VALUE
        Arguments.of("9223372036854775808", NumberFormatException.class), // BigDecimal overflow
        Arguments.of(new BigDecimal("9223372036854775808"), ArithmeticException.class), // BigInteger overflow
        Arguments.of(new BigInteger("9223372036854775808"), ArithmeticException.class), // BigDecimal with fractional part
        Arguments.of(new BigDecimal("12345.67"), ArithmeticException.class));
    }

    private static Stream<Arguments> invalidNumericValues() {
        return Stream.of(Arguments.of(Double.NaN, IllegalArgumentException.class), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class));
    }

    @Test
    void toTimestamp_fromLong() {
        // given
        // 2021‑09‑20T00:00:00Z
        Long epochMillis = 1_632_000_000_000L;
        // when
        Timestamp ts = DateConversions.toTimestamp(epochMillis, converter);
        // then
        assertEquals(new Timestamp(epochMillis), ts);
    }

    @Test
    void toTimestamp_fromStringNumber() {
        // given
        String epochString = "1632000000000";
        // when
        Timestamp ts = DateConversions.toTimestamp(epochString, converter);
        // then
        assertEquals(new Timestamp(1632000000000L), ts);
    }

    @Test
    void toTimestamp_fromDate() {
        // given
        Date date = new Date(1632000000000L);
        // when
        Timestamp ts = DateConversions.toTimestamp(date, converter);
        // then
        assertEquals(new Timestamp(date.getTime()), ts);
    }

    @Test
    void toTimestamp_fromSqlTimestamp() {
        // given
        Timestamp original = new Timestamp(1632000000000L);
        // when
        Timestamp ts = DateConversions.toTimestamp(original, converter);
        // then
        // The conversion should preserve the exact instant
        assertEquals(original, ts);
    }

    @Test
    void toTimestamp_fromInstant() {
        // given
        Instant instant = Instant.ofEpochMilli(1632000000000L);
        // when
        Timestamp ts = DateConversions.toTimestamp(instant, converter);
        // then
        assertEquals(Timestamp.from(instant), ts);
    }

    @Test
    void toTimestamp_fromLocalDateTime() {
        // given – use the system default zone for the expected conversion
        LocalDateTime ldt = LocalDateTime.of(2021, Month.SEPTEMBER, 20, 0, 0, 0, 0);
        // when
        Timestamp ts = DateConversions.toTimestamp(ldt, converter);
        // then
        // Timestamp.valueOf uses the same conversion rules as the JDK for LocalDateTime → Timestamp
        assertEquals(Timestamp.valueOf(ldt), ts);
    }

    @Test
    void toTimestamp_fromCalendar() {
        // given
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1632000000000L);
        // when
        Timestamp ts = DateConversions.toTimestamp(cal, converter);
        // then
        assertEquals(new Timestamp(1632000000000L), ts);
    }

    @Test
    void toTimestamp_fromMapWithTimeKey() {
        // given – map representation used by the Converter for date/time objects
        Map<String, Object> map = new HashMap<>();
        map.put("time", 1632000000000L);
        // when
        Timestamp ts = DateConversions.toTimestamp(map, converter);
        // then
        assertEquals(new Timestamp(1632000000000L), ts);
    }

    @Test
    void toTimestamp_fromNull() {
        // given
        Object nullValue = null;
        // when
        Timestamp ts = DateConversions.toTimestamp(nullValue, converter);
        // then
        // Converter returns 0 for numeric primitives, so Timestamp(0) is expected
        assertEquals(new Timestamp(0L), ts);
    }

    @ParameterizedTest(name = "[Numeric] from={0}")
    @MethodSource("numericInputs")
    void toTimestamp_numericInputs(Object from, Timestamp expected) {
        Timestamp actual = DateConversions.toTimestamp(from, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[Date/Calendar] from={0}")
    @MethodSource("dateAndCalendarInputs")
    void toTimestamp_dateAndCalendarInputs(Object from, Timestamp expected) {
        Timestamp actual = DateConversions.toTimestamp(from, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[Instant/Temporal] from={0}")
    @MethodSource("instantAndTemporalInputs")
    void toTimestamp_instantAndTemporalInputs(Object from, Timestamp expected) {
        Timestamp actual = DateConversions.toTimestamp(from, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[String formats] from=\"{0}\"")
    @MethodSource("stringDateFormats")
    void toTimestamp_stringDateFormats(String from, Timestamp expected) {
        Timestamp actual = DateConversions.toTimestamp(from, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[BigNumber] from={0}")
    @MethodSource("bigNumberInputs")
    void toTimestamp_bigNumberInputs(Object from, Timestamp expected) {
        Timestamp actual = DateConversions.toTimestamp(from, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[AtomicLong] from={0}")
    @MethodSource("atomicLongInput")
    void toTimestamp_atomicLongInput(AtomicLong from, Timestamp expected) {
        Timestamp actual = DateConversions.toTimestamp(from, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[Null/Invalid] from={0}")
    @MethodSource("nullAndInvalidInputs")
    void toTimestamp_nullAndInvalidInputs(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DateConversions.toTimestamp(from, converter));
    }

    @ParameterizedTest(name = "[Invalid string] from=\"{0}\"")
    @MethodSource("invalidStringFormats")
    void toTimestamp_invalidStringFormats(String from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DateConversions.toTimestamp(from, converter));
    }

    @ParameterizedTest(name = "[Numeric overflow] from={0}")
    @MethodSource("numericOverflowAndFormatErrors")
    void toTimestamp_numericOverflowAndFormatErrors(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DateConversions.toTimestamp(from, converter));
    }

    @ParameterizedTest(name = "[Invalid numeric] from={0}")
    @MethodSource("invalidNumericValues")
    void toTimestamp_invalidNumericValues(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DateConversions.toTimestamp(from, converter));
    }
}
