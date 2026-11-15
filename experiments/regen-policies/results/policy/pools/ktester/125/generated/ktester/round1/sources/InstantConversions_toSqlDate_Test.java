package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

class InstantConversions_toSqlDate_Test {

    /**
     * Converter used for all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options – no special configuration required.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long‑running test suites.
        converter = null;
    }

    private static Stream<Arguments> provideSupportedSourceTypes() {
        return Stream.of(// java.util.Date(0)
        Arguments.of(new Date(0L), 0L, "java.util.Date epoch 0"), // java.sql.Timestamp
        Arguments.of(new Timestamp(1622505600123L), 1622505600123L, "java.sql.Timestamp retains its millisecond value"), // java.time.Instant
        Arguments.of(Instant.ofEpochMilli(1622505600000L), 1622505600000L, "Instant converted to epoch millis"), // java.time.LocalDate (midnight UTC)
        Arguments.of(LocalDate.of(2021, 6, 1), 1622505600000L, "LocalDate interpreted as midnight UTC"), // java.time.LocalDateTime (treated as UTC)
        Arguments.of(LocalDateTime.of(2021, 6, 1, 15, 30, 45), 1622555445000L, "LocalDateTime treated as UTC"), // ZonedDateTime America/New_York (UTC‑4)
        Arguments.of(ZonedDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneId.of("America/New_York")), 1622505600000L, "ZonedDateTime normalized to UTC"), // OffsetDateTime negative offset (‑5)
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5)), 1622512800000L, "OffsetDateTime with negative offset shifted to UTC"), // OffsetDateTime positive offset (+2) – expected date part only
        Arguments.of(OffsetDateTime.of(2021, 6, 1, 0, 0, 0, 0, ZoneOffset.ofHours(2)), 1622419200000L, "OffsetDateTime with positive offset normalized to UTC"), // BigInteger
        Arguments.of(new BigInteger("1622505600000"), 1622505600000L, "BigInteger directly used as epoch millis"), // BigDecimal (fractional part truncated)
        Arguments.of(new BigDecimal("1622505600123.45"), 1622505600123L, "BigDecimal truncated to long"), // AtomicLong
        Arguments.of(new AtomicLong(1622505600000L), 1622505600000L, "AtomicLong provides its long value"), // Calendar (UTC)
        Arguments.of(createUtcCalendar(2021, Calendar.JUNE, 1, 0, 0, 0, 0), 1622505600000L, "Calendar set to 2021‑06‑01 00:00:00.000 UTC"), // Long value
        Arguments.of(1622548800000L, 1622548800000L, "Long epoch millis"), // String representation of long
        Arguments.of("1622548800000", 1622548800000L, "String parsed to long"), // java.util.Date (current instant)
        Arguments.of(new Date(1622548800000L), 1622548800000L, "java.util.Date current instant"));
    }

    private static Calendar createUtcCalendar(int year, int month, int day, int hour, int minute, int second, int milli) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month, day, hour, minute, second);
        cal.set(Calendar.MILLISECOND, milli);
        return cal;
    }

    private static Stream<Arguments> provideOverflowCases() {
        return Stream.of(// BigInteger > Long.MAX_VALUE
        Arguments.of(new BigInteger("9223372036854775808"), ArithmeticException.class, "BigInteger larger than Long.MAX_VALUE should overflow"), // BigInteger < Long.MIN_VALUE
        Arguments.of(new BigInteger("-9223372036854775809"), ArithmeticException.class, "BigInteger smaller than Long.MIN_VALUE should overflow"), // BigDecimal > Long.MAX_VALUE
        Arguments.of(new BigDecimal("9223372036854775808"), ArithmeticException.class, "BigDecimal larger than Long.MAX_VALUE should overflow"), // BigDecimal < Long.MIN_VALUE
        Arguments.of(new BigDecimal("-9223372036854775809"), ArithmeticException.class, "BigDecimal smaller than Long.MIN_VALUE should overflow"), // String > Long.MAX_VALUE
        Arguments.of("9223372036854775808", NumberFormatException.class, "String representation exceeding Long.MAX_VALUE should fail parsing"), // String < Long.MIN_VALUE
        Arguments.of("-9223372036854775809", NumberFormatException.class, "String representation below Long.MIN_VALUE should fail parsing"), // Instant overflow (millis beyond long range)
        Arguments.of(Instant.ofEpochSecond(Long.MAX_VALUE / 1000 + 1, 0), ArithmeticException.class, "Instant whose epoch millis exceed Long range should overflow"));
    }

    private static Stream<Arguments> provideInvalidNumericValues() {
        return Stream.of(Arguments.of(Double.NaN, IllegalArgumentException.class, "NaN cannot be converted to a long"), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class, "Positive infinity cannot be converted to a long"), Arguments.of(Float.NEGATIVE_INFINITY, IllegalArgumentException.class, "Negative infinity cannot be converted to a long"));
    }

    private static Stream<Arguments> provideBoundaryValues() {
        return Stream.of(Arguments.of(new Date(Long.MAX_VALUE), Long.MAX_VALUE, "Maximum long epoch should be handled correctly"), Arguments.of(new Date(Long.MIN_VALUE), Long.MIN_VALUE, "Minimum long epoch should be handled correctly"), Arguments.of(Instant.EPOCH, 0L, "Instant.EPOCH should produce epoch 0"), Arguments.of(LocalDate.of(1970, 1, 1), 0L, "LocalDate representing Unix epoch start should yield 0"));
    }

    @Test
    void toSqlDate_fromLong_returnsDateWithSameEpoch() {
        // 2021‑06‑01T12:00:00Z
        Long epochMillis = 1_622_548_800_000L;
        Date result = InstantConversions.toSqlDate(epochMillis, converter);
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(epochMillis.longValue(), result.getTime(), "Date time should match the supplied long value");
    }

    @Test
    void toSqlDate_fromString_returnsDateWithParsedEpoch() {
        // same instant as above, expressed as a String
        String epochStr = "1622548800000";
        Date result = InstantConversions.toSqlDate(epochStr, converter);
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(Long.parseLong(epochStr), result.getTime(), "Date time should match the parsed long value from the String");
    }

    @Test
    void toSqlDate_fromDate_returnsDateWithSameInstant() {
        // current instant
        Date now = new Date();
        Date result = InstantConversions.toSqlDate(now, converter);
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(now.getTime(), result.getTime(), "Resulting Date should have the same epoch as the source Date");
    }

    @Test
    void toSqlDate_fromNull_returnsEpochZeroDate() {
        // According to Converter contract, a null source converted to a primitive long yields 0.
        Date result = InstantConversions.toSqlDate(null, converter);
        assertNotNull(result, "Resulting Date should not be null even when source is null");
        assertEquals(0L, result.getTime(), "Resulting Date should represent epoch 0 when source is null");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideSupportedSourceTypes")
    void toSqlDate_supportedSources(Object from, long expectedEpoch, String description) {
        Date result = InstantConversions.toSqlDate(from, null);
        assertNotNull(result, "Resulting Date should never be null");
        assertEquals(expectedEpoch, result.getTime(), "Epoch milliseconds should match expected value for: " + description);
    }

    @Test
    void toSqlDate_nullSource_throwsIllegalArgumentException() {
        // According to the JSON spec, a null source should cause an exception.
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toSqlDate(null, null), "Passing a null source should throw IllegalArgumentException");
    }

    @Test
    void toSqlDate_unsupportedObject_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toSqlDate(unsupported, null), "Unsupported source type should trigger IllegalArgumentException");
    }

    @Test
    void toSqlDate_nonNumericString_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toSqlDate("not-a-number", null), "Non‑numeric string must cause conversion failure");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideOverflowCases")
    void toSqlDate_overflowScenarios(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> InstantConversions.toSqlDate(from, null), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideInvalidNumericValues")
    void toSqlDate_invalidNumericValues(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> InstantConversions.toSqlDate(from, null), description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideBoundaryValues")
    void toSqlDate_boundaryValues(Object from, long expectedEpoch, String description) {
        Date result = InstantConversions.toSqlDate(from, null);
        assertNotNull(result);
        assertEquals(expectedEpoch, result.getTime(), description);
    }
}
