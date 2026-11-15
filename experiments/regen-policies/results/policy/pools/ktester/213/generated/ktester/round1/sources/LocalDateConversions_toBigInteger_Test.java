package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.Calendar;
import java.util.stream.Stream;

class LocalDateConversions_toBigInteger_Test {

    /**
     * Converter used for all tests – created once for the whole test suite.
     */
    private static Converter converter;

    /**
     * Converter used for all tests – the library’s default, fully‑functional instance.
     */
    private static final Converter CONVERTER = DefaultConverter.INSTANCE;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter is fully functional out‑of‑the‑box.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Helper that mirrors the logic used by {@code InstantConversions.toBigInteger}.
     */
    private static BigInteger instantToBigInteger(Instant i) {
        if (i == null) {
            return null;
        }
        // total nanoseconds = seconds * 1_000_000_000 + nanos
        return BigInteger.valueOf(i.getEpochSecond()).multiply(BigInteger.valueOf(1_000_000_000L)).add(BigInteger.valueOf(i.getNano()));
    }

    /* -------------------------------------------------------
     *  Parameterized tests for the “Valid conversions to BigInteger” group
     * ------------------------------------------------------- */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Instant → same epoch‑millis
        Arguments.of(Instant.ofEpochMilli(123_456_789L), BigInteger.valueOf(123_456_789L), "Instant input should be passed through to InstantConversions.toBigInteger"), // java.util.Date → same epoch‑millis
        Arguments.of(new Date(987_654_321L), BigInteger.valueOf(987_654_321L), "Date is converted to an Instant representing the same epoch‑millis"), // LocalDateTime interpreted as UTC
        Arguments.of(LocalDateTime.of(2023, 1, 1, 0, 0, 0), BigInteger.valueOf(1_672_531_200_000L), "LocalDateTime interpreted as UTC midnight 2023‑01‑01"), // Long epoch‑millis
        Arguments.of(Long.valueOf(5_555_555_555L), BigInteger.valueOf(5_555_555_555L), "Long epoch‑millis is treated as an Instant"), // Numeric String
        Arguments.of("1234567890123", BigInteger.valueOf(1_234_567_890_123L), "Numeric String parsed to Long epoch‑millis"));
    }

    /* -------------------------------------------------------
     *  Conversions from LocalDate
     * ------------------------------------------------------- */
    private static Stream<Arguments> localDateProvider() {
        return Stream.of(Arguments.of(LocalDate.of(1970, 1, 1), BigInteger.valueOf(0L), "Epoch day → 0"), Arguments.of(LocalDate.of(1969, 12, 31), BigInteger.valueOf(-86_400_000L), "Day before epoch → -86 400 000"));
    }

    /* -------------------------------------------------------
     *  Conversions from ZonedDateTime and OffsetDateTime
     * ------------------------------------------------------- */
    private static Stream<Arguments> zonedAndOffsetProvider() {
        return Stream.of(// ZonedDateTime with -05:00 offset
        Arguments.of(ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5)), BigInteger.valueOf(1_672_549_200_000L), "ZonedDateTime -05:00 → UTC epoch‑millis"), // OffsetDateTime with +02:00 offset
        Arguments.of(OffsetDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2)), BigInteger.valueOf(1_672_524_000_000L), "OffsetDateTime +02:00 → UTC epoch‑millis"));
    }

    /* -------------------------------------------------------
     *  Conversions from Year, YearMonth and MonthDay
     * ------------------------------------------------------- */
    private static Stream<Arguments> yearRelatedProvider() {
        return Stream.of(// Year 1970 → 1970‑01‑01T00:00:00Z
        Arguments.of(Year.of(1970), BigInteger.valueOf(0L), "Year 1970 → epoch start"), // YearMonth 2023‑01 → first day of month at midnight UTC
        Arguments.of(YearMonth.of(2023, 1), BigInteger.valueOf(1_672_531_200_000L), "YearMonth 2023‑01 → start of month"), // MonthDay 12‑31 → most recent occurrence (chosen as 2023‑12‑31)
        Arguments.of(MonthDay.of(12, 31), // 2023‑12‑31T00:00:00Z epoch‑millis
        BigInteger.valueOf(Instant.parse("2023-12-31T00:00:00Z").toEpochMilli()), "MonthDay 12‑31 → 2023‑12‑31"));
    }

    /* -------------------------------------------------------
     *  Conversions from Timestamp and Calendar
     * ------------------------------------------------------- */
    private static Stream<Arguments> timestampCalendarProvider() {
        return Stream.of(// java.sql.Timestamp
        Arguments.of(new java.sql.Timestamp(123_456_789L), BigInteger.valueOf(123_456_789L), "Timestamp → same epoch‑millis"), // Calendar in UTC with specific millis
        Arguments.of(createUtcCalendar(5_555_555_555L), BigInteger.valueOf(5_555_555_555L), "Calendar (UTC) → same epoch‑millis"));
    }

    private static Calendar createUtcCalendar(long epochMilli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(epochMilli);
        return cal;
    }

    /* -------------------------------------------------------
     *  Boundary epoch values
     * ------------------------------------------------------- */
    private static Stream<Arguments> boundaryProvider() {
        return Stream.of(Arguments.of(Instant.ofEpochMilli(Long.MAX_VALUE), BigInteger.valueOf(Long.MAX_VALUE), "Maximum long epoch‑millis preserved"), Arguments.of(Instant.ofEpochMilli(Long.MIN_VALUE), BigInteger.valueOf(Long.MIN_VALUE), "Minimum long epoch‑millis preserved"), Arguments.of("-123456789", BigInteger.valueOf(-123_456_789L), "Negative numeric string parsed correctly"));
    }

    /* -------------------------------------------------------
     *  Invalid and unsupported inputs – expect exceptions
     * ------------------------------------------------------- */
    private static Stream<Arguments> invalidProvider() {
        return Stream.of(Arguments.of("not-a-number", NumberFormatException.class, "Non‑numeric string should cause NumberFormatException"), Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "Unsupported Boolean should trigger IllegalArgumentException"), Arguments.of(new Object(), IllegalArgumentException.class, "Arbitrary Object should trigger IllegalArgumentException"), Arguments.of(LocalTime.of(12, 34, 56), IllegalArgumentException.class, "LocalTime lacks date information – IllegalArgumentException"));
    }

    @Test
    void toBigIntegerFromInstant() {
        Instant now = Instant.now();
        BigInteger expected = instantToBigInteger(now);
        BigInteger actual = LocalDateConversions.toBigInteger(now, converter);
        assertEquals(expected, actual, "Conversion from Instant should produce correct nanosecond value");
    }

    @Test
    void toBigIntegerFromLocalDateTime() {
        LocalDateTime ldt = LocalDateTime.of(2023, 3, 15, 12, 30, 45, 123_456_789);
        // Converter will treat the LocalDateTime as being in the system default zone;
        // for a deterministic test we convert it to an Instant using UTC.
        Instant expectedInstant = ldt.toInstant(ZoneOffset.UTC);
        BigInteger expected = instantToBigInteger(expectedInstant);
        // Force the converter to interpret the LocalDateTime as UTC by supplying a
        // ConverterOptions with a fixed default zone (optional – here we rely on the
        // default behavior of the library which uses UTC for LocalDateTime conversion).
        BigInteger actual = LocalDateConversions.toBigInteger(ldt, converter);
        assertEquals(expected, actual, "Conversion from LocalDateTime should match expected nanoseconds");
    }

    @Test
    void toBigIntegerFromLocalDate() {
        LocalDate ld = LocalDate.of(2023, 3, 15);
        // LocalDate is interpreted as start of day at UTC.
        Instant expectedInstant = ld.atStartOfDay(ZoneOffset.UTC).toInstant();
        BigInteger expected = instantToBigInteger(expectedInstant);
        BigInteger actual = LocalDateConversions.toBigInteger(ld, converter);
        assertEquals(expected, actual, "Conversion from LocalDate should use start‑of‑day UTC");
    }

    @Test
    void toBigIntegerFromDate() {
        // 2022‑01‑01T00:00:00Z
        Date date = new Date(1_640_995_200_000L);
        Instant expectedInstant = date.toInstant();
        BigInteger expected = instantToBigInteger(expectedInstant);
        BigInteger actual = LocalDateConversions.toBigInteger(date, converter);
        assertEquals(expected, actual, "Conversion from java.util.Date should produce correct nanoseconds");
    }

    @Test
    void toBigIntegerFromLongEpochMilli() {
        // 2022‑01‑01T00:00:00.123Z
        long epochMilli = 1_640_995_200_123L;
        Instant expectedInstant = Instant.ofEpochMilli(epochMilli);
        BigInteger expected = instantToBigInteger(expectedInstant);
        BigInteger actual = LocalDateConversions.toBigInteger(epochMilli, converter);
        assertEquals(expected, actual, "Conversion from epoch‑millis long should be correct");
    }

    @Test
    void toBigIntegerFromStringIso8601() {
        String iso = "2022-01-01T12:34:56.789Z";
        Instant expectedInstant = Instant.parse(iso);
        BigInteger expected = instantToBigInteger(expectedInstant);
        BigInteger actual = LocalDateConversions.toBigInteger(iso, converter);
        assertEquals(expected, actual, "Conversion from ISO‑8601 string should be correct");
    }

    @Test
    void toBigIntegerFromNull() {
        assertNull(LocalDateConversions.toBigInteger(null, converter), "Conversion of null should return null");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toBigInteger_ValidConversions(Object from, BigInteger expected, String description) {
        BigInteger actual = LocalDateConversions.toBigInteger(from, CONVERTER);
        assertEquals(expected, actual, description);
    }

    /* -------------------------------------------------------
     *  Null input – should return null
     * ------------------------------------------------------- */
    @Test
    void toBigInteger_NullInput() {
        assertNull(LocalDateConversions.toBigInteger(null, CONVERTER), "Null input should result in null");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("localDateProvider")
    void toBigInteger_FromLocalDate(LocalDate ld, BigInteger expected, String description) {
        Instant instant = ld.atStartOfDay(ZoneOffset.UTC).toInstant();
        assertEquals(expected, instantToBigInteger(instant), "Pre‑condition check");
        BigInteger actual = LocalDateConversions.toBigInteger(ld, CONVERTER);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("zonedAndOffsetProvider")
    void toBigInteger_FromZonedOrOffset(Object temporal, BigInteger expected, String description) {
        // The conversion routine ultimately calls toInstant(...), which normalises to UTC.
        BigInteger actual = LocalDateConversions.toBigInteger(temporal, CONVERTER);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("yearRelatedProvider")
    void toBigInteger_FromYearRelated(Object from, BigInteger expected, String description) {
        BigInteger actual = LocalDateConversions.toBigInteger(from, CONVERTER);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("timestampCalendarProvider")
    void toBigInteger_FromTimestampOrCalendar(Object from, BigInteger expected, String description) {
        BigInteger actual = LocalDateConversions.toBigInteger(from, CONVERTER);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("boundaryProvider")
    void toBigInteger_BoundaryValues(Object from, BigInteger expected, String description) {
        BigInteger actual = LocalDateConversions.toBigInteger(from, CONVERTER);
        assertEquals(expected, actual, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidProvider")
    void toBigInteger_InvalidInputs(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> LocalDateConversions.toBigInteger(from, CONVERTER), description);
    }
}
