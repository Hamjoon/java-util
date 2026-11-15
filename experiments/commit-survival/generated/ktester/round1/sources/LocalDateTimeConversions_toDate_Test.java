package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toDate_Test {

    /**
     * Converter instance used by the tests – created once for the whole test suite.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Default ConverterOptions are sufficient.
        converter = new Converter(new ConverterOptions());
        // Force the JVM default time‑zone to UTC so that conversions that rely on the system default
        // zone produce deterministic results.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    private static Date dateFromEpoch(long epochMilli) {
        return new Date(epochMilli);
    }

    private static Stream<Arguments> primitiveNumberProvider() {
        return Stream.of(Arguments.of(1622548800000L, dateFromEpoch(1622548800000L)), Arguments.of(Integer.valueOf((int) 1622548800000L), dateFromEpoch(1622548800000L)));
    }

    private static Stream<Arguments> stringNumberProvider() {
        return Stream.of(Arguments.of("1622548800000", dateFromEpoch(1622548800000L)), Arguments.of("-86400000", dateFromEpoch(-86400000L)));
    }

    private static Stream<Arguments> invalidNumericStringProvider() {
        return Stream.of(Arguments.of("notANumber", NumberFormatException.class), Arguments.of("", NumberFormatException.class), Arguments.of("123abc456", NumberFormatException.class), Arguments.of("1622548800.123", NumberFormatException.class));
    }

    @Test
    void toDate_givenDate_returnsSameInstant() {
        // GIVEN
        Date now = new Date();
        // WHEN
        Date result = LocalDateTimeConversions.toDate(now, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(now.getTime(), result.getTime(), "The returned Date must represent the same instant as the input Date");
    }

    @Test
    void toDate_givenLong_returnsCorrespondingDate() {
        // GIVEN
        // arbitrary epoch milliseconds
        long epochMilli = 1_642_000_000_000L;
        // WHEN
        Date result = LocalDateTimeConversions.toDate(epochMilli, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(epochMilli, result.getTime(), "The returned Date must represent the instant supplied by the Long value");
    }

    @Test
    void toDate_givenStringNumber_returnsCorrespondingDate() {
        // GIVEN
        String epochString = "1642000000000";
        // WHEN
        Date result = LocalDateTimeConversions.toDate(epochString, converter);
        // THEN
        assertNotNull(result, "Resulting Date should not be null");
        assertEquals(Long.parseLong(epochString), result.getTime(), "The returned Date must represent the instant parsed from the String");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("primitiveNumberProvider")
    void toDate_givenPrimitiveOrWrapperNumber_returnsDate(Object from, Date expected) {
        Date result = LocalDateTimeConversions.toDate(from, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "Date should represent the same epoch milliseconds as the input number");
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\"")
    @MethodSource("stringNumberProvider")
    void toDate_givenStringNumber_returnsDate(String from, Date expected) {
        Date result = LocalDateTimeConversions.toDate(from, converter);
        assertNotNull(result);
        assertEquals(expected.getTime(), result.getTime(), "String should be parsed to a long and wrapped in a Date");
    }

    @Test
    void toDate_givenCalendar_returnsCorrespondingDate() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(1622548800000L);
        Date result = LocalDateTimeConversions.toDate(calendar, converter);
        assertNotNull(result);
        assertEquals(1622548800000L, result.getTime(), "Calendar set to UTC should be converted to the same epoch instant");
    }

    @Test
    void toDate_givenLocalDateTime_systemDefaultZone() {
        LocalDateTime ldt = LocalDateTime.of(2021, 6, 1, 12, 0, 0);
        long expected = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Date result = LocalDateTimeConversions.toDate(ldt, converter);
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "LocalDateTime should be interpreted using the system default zone (UTC in tests)");
    }

    @Test
    void toDate_givenLocalDate_startOfDay() {
        LocalDate ld = LocalDate.of(2021, 6, 1);
        long expected = ld.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Date result = LocalDateTimeConversions.toDate(ld, converter);
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "LocalDate should be treated as start‑of‑day in the system default zone");
    }

    @Test
    void toDate_givenInstant_returnsCorrespondingDate() {
        Instant instant = Instant.ofEpochMilli(1622548800000L);
        Date result = LocalDateTimeConversions.toDate(instant, converter);
        assertNotNull(result);
        assertEquals(instant.toEpochMilli(), result.getTime(), "Instant maps directly to epoch milliseconds");
    }

    @Test
    void toDate_givenZonedDateTimeUTC_returnsDate() {
        ZonedDateTime zdt = ZonedDateTime.of(2021, 6, 1, 12, 0, 0, 0, ZoneId.of("UTC"));
        long expected = zdt.toInstant().toEpochMilli();
        Date result = LocalDateTimeConversions.toDate(zdt, converter);
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "ZonedDateTime with UTC zone must be converted to the same instant");
    }

    @Test
    void toDate_givenOffsetDateTime_returnsDate() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 6, 1, 8, 0, 0, 0, ZoneOffset.ofHours(-4));
        // 12:00 UTC
        long expected = odt.toInstant().toEpochMilli();
        Date result = LocalDateTimeConversions.toDate(odt, converter);
        assertNotNull(result);
        assertEquals(expected, result.getTime(), "OffsetDateTime should be normalised to UTC before conversion");
    }

    @Test
    void toDate_givenTimestamp_returnsDate() {
        Timestamp ts = new Timestamp(1622548800000L);
        Date result = LocalDateTimeConversions.toDate(ts, converter);
        assertNotNull(result);
        assertEquals(ts.getTime(), result.getTime(), "Timestamp should be treated as a millisecond instant");
    }

    @Test
    void toDate_givenBigDecimal_returnsDate() {
        BigDecimal bd = new BigDecimal("1622548800000");
        Date result = LocalDateTimeConversions.toDate(bd, converter);
        assertNotNull(result);
        assertEquals(bd.longValueExact(), result.getTime(), "BigDecimal containing a whole number of milliseconds should be converted");
    }

    @Test
    void toDate_givenBigInteger_returnsDate() {
        BigInteger bi = new BigInteger("1622548800000");
        Date result = LocalDateTimeConversions.toDate(bi, converter);
        assertNotNull(result);
        assertEquals(bi.longValueExact(), result.getTime(), "BigInteger representing epoch millis should be converted correctly");
    }

    @Test
    void toDate_givenAtomicLong_returnsDate() {
        AtomicLong al = new AtomicLong(1622548800000L);
        Date result = LocalDateTimeConversions.toDate(al, converter);
        assertNotNull(result);
        assertEquals(al.get(), result.getTime(), "AtomicLong's current value should be used as epoch millis");
    }

    @Test
    void toDate_givenNullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toDate(null, converter), "Passing null as the source object should result in NPE");
    }

    @Test
    void toDate_givenUnsupportedType_throwsIllegalArgumentException() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toDate(unsupported, converter), "Unsupported source type must trigger IllegalArgumentException");
    }

    @Test
    void toDate_givenZeroLong_returnsEpochStart() {
        Date result = LocalDateTimeConversions.toDate(0L, converter);
        assertNotNull(result);
        assertEquals(0L, result.getTime(), "Zero epoch should produce the Unix epoch start date");
    }

    @Test
    void toDate_givenLongMaxValue_returnsCorrespondingDate() {
        Date result = LocalDateTimeConversions.toDate(Long.MAX_VALUE, converter);
        assertNotNull(result);
        assertEquals(Long.MAX_VALUE, result.getTime(), "Long.MAX_VALUE should be wrapped in a Date (no overflow for java.util.Date)");
    }

    @Test
    void toDate_givenLongMinValue_returnsCorrespondingDate() {
        Date result = LocalDateTimeConversions.toDate(Long.MIN_VALUE, converter);
        assertNotNull(result);
        assertEquals(Long.MIN_VALUE, result.getTime(), "Long.MIN_VALUE should be wrapped in a Date (no overflow for java.util.Date)");
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → {1}")
    @MethodSource("invalidNumericStringProvider")
    void toDate_givenInvalidNumericString_throwsException(String from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalDateTimeConversions.toDate(from, converter), "Invalid numeric string should trigger " + expectedException.getSimpleName());
    }

    @Test
    void toDate_givenBoolean_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> LocalDateTimeConversions.toDate(Boolean.TRUE, converter), "Boolean values are not supported for epoch‑millisecond conversion");
    }

    @Test
    void toDate_givenOutOfRangeLocalDateTime_throwsDateTimeException() {
        LocalDateTime ldt = LocalDateTime.of(10_000, 1, 1, 0, 0);
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toDate(ldt, converter), "LocalDateTime beyond the supported range should throw DateTimeException");
    }

    @Test
    void toDate_givenInstantMax_throwsDateTimeException() {
        // far beyond java.util.Date range
        Instant instant = Instant.MAX;
        assertThrows(DateTimeException.class, () -> LocalDateTimeConversions.toDate(instant, converter), "Instant.MAX should cause DateTimeException when converting to Date");
    }

    @Test
    void toDate_givenBigDecimalOverflow_throwsArithmeticException() {
        // Long.MAX_VALUE + 1
        BigDecimal overflow = new BigDecimal("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> LocalDateTimeConversions.toDate(overflow, converter), "BigDecimal larger than Long.MAX_VALUE should cause ArithmeticException");
    }

    @Test
    void toDate_givenBigIntegerOverflow_throwsArithmeticException() {
        // Long.MAX_VALUE + 1
        BigInteger overflow = new BigInteger("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> LocalDateTimeConversions.toDate(overflow, converter), "BigInteger larger than Long.MAX_VALUE should cause ArithmeticException");
    }

    @Test
    void toDate_givenIsoStringAndNullConverter_throwsNullPointerException() {
        String iso = "2021-06-01T12:00:00Z";
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toDate(iso, null), "ISO‑8601 string requires a non‑null Converter; passing null should NPE");
    }
}
