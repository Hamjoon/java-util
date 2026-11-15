package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toDate_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter sharedConverter;

    /**
     * Holds the original default time‑zone so we can restore it after the tests.
     */
    private static TimeZone originalTimeZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original default time‑zone – the conversion logic may depend on it.
        originalTimeZone = TimeZone.getDefault();
        // Use a deterministic default time‑zone for reproducible results.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Initialise a Converter with default options (no overrides).
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default time‑zone.
        TimeZone.setDefault(originalTimeZone);
    }

    private static Stream<Arguments> branchCoverageProvider() {
        Converter mockConverter = new Converter(new ConverterOptions());
        return Stream.of(// both arguments null → NPE (or whatever the implementation throws)
        Arguments.of(null, null, NullPointerException.class, "Both arguments null should raise NPE"), // java.util.Date instance
        Arguments.of(new Date(0L), mockConverter, new Date(0L), "Date epoch should be returned unchanged"), // Instant
        Arguments.of(Instant.ofEpochMilli(123456789L), mockConverter, Date.from(Instant.ofEpochMilli(123456789L)), "Instant should be converted to Date via epoch millis"), // LocalDate (start of day UTC)
        Arguments.of(LocalDate.of(2023, 5, 15), mockConverter, new Date(LocalDate.of(2023, 5, 15).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()), "LocalDate should become midnight UTC"), // LocalDateTime (default zone = UTC)
        Arguments.of(LocalDateTime.of(2023, 5, 15, 13, 45, 30), mockConverter, new Date(LocalDateTime.of(2023, 5, 15, 13, 45, 30).toInstant(ZoneOffset.UTC).toEpochMilli()), "LocalDateTime should be interpreted in UTC"), // ZonedDateTime (UTC)
        Arguments.of(ZonedDateTime.of(2023, 5, 15, 13, 45, 30, 0, ZoneId.of("UTC")), mockConverter, new Date(ZonedDateTime.of(2023, 5, 15, 13, 45, 30, 0, ZoneId.of("UTC")).toInstant().toEpochMilli()), "ZonedDateTime UTC should map to its instant"), // OffsetDateTime (+02)
        Arguments.of(OffsetDateTime.of(2023, 5, 15, 13, 45, 30, 0, ZoneOffset.ofHours(2)), mockConverter, new Date(OffsetDateTime.of(2023, 5, 15, 13, 45, 30, 0, ZoneOffset.ofHours(2)).toInstant().toEpochMilli()), "OffsetDateTime +02 should be converted to UTC instant"), // java.sql.Timestamp
        Arguments.of(Timestamp.valueOf("2023-05-15 13:45:30.123"), mockConverter, new Date(Timestamp.valueOf("2023-05-15 13:45:30.123").getTime()), "Timestamp should use its millisecond value"), // Long value
        Arguments.of(Long.valueOf(1609459200000L), mockConverter, new Date(1609459200000L), "Long epoch millis should be wrapped in Date"), // ISO‑like String (no zone, parsed as LocalDateTime in default UTC)
        Arguments.of("2023-05-15T13:45:30", mockConverter, new Date(LocalDateTime.parse("2023-05-15T13:45:30").toInstant(ZoneOffset.UTC).toEpochMilli()), "String without zone should be parsed as LocalDateTime UTC"), // BigDecimal
        Arguments.of(new BigDecimal("1622505600000"), mockConverter, new Date(1622505600000L), "BigDecimal should be converted via longValueExact()"), // AtomicLong
        Arguments.of(new AtomicLong(1622505600123L), mockConverter, new Date(1622505600123L), "AtomicLong should yield its long value"), // Calendar instance
        Arguments.of(new Calendar.Builder().setInstant(1622505600000L).build(), mockConverter, new Date(1622505600000L), "Calendar.getTimeInMillis() should be used"), // Unsupported object → RuntimeException (fallback conversion failure)
        Arguments.of(new Object(), mockConverter, RuntimeException.class, "Unsupported type should cause a conversion failure"));
    }

    private static Stream<Arguments> additionalNumericProvider() {
        Converter conv = new Converter(new ConverterOptions());
        return Stream.of(// Integer (treated as milliseconds)
        Arguments.of(Integer.valueOf(1622505600), conv, new Date(1622505600L), "Integer should be taken as milliseconds directly"), // Double (fractional part truncated)
        Arguments.of(Double.valueOf(1622505600.5), conv, new Date(1622505600L), "Double should be truncated to long milliseconds"), // BigInteger
        Arguments.of(new BigInteger("1622505600000"), conv, new Date(1622505600000L), "BigInteger should be converted to long"));
    }

    private static Stream<Arguments> timeOnlyProvider() {
        Converter conv = new Converter(new ConverterOptions());
        return Stream.of(Arguments.of(LocalTime.of(13, 45, 30), conv, "LocalTime lacks a date component and should fail"), Arguments.of(OffsetTime.of(13, 45, 30, 0, ZoneOffset.ofHours(2)), conv, "OffsetTime lacks a date component and should fail"));
    }

    private static Stream<Arguments> stringParsingProvider() {
        Converter conv = new Converter(new ConverterOptions());
        return Stream.of(// Plain numeric string → epoch millis
        Arguments.of("1622505600000", conv, new Date(1622505600000L), "Numeric string should be parsed as epoch millis"), // Non‑ISO format → conversion failure
        Arguments.of("2023/05/15 13:45:30", conv, RuntimeException.class, "Non‑ISO string should cause a conversion exception"), // Empty string → conversion failure
        Arguments.of("", conv, RuntimeException.class, "Empty string cannot be parsed"));
    }

    private static Stream<Arguments> unsupportedWrapperProvider() {
        Converter conv = new Converter(new ConverterOptions());
        return Stream.of(Arguments.of(Boolean.TRUE, conv, "Boolean should not be convertible to Date"), Arguments.of(Character.valueOf('A'), conv, "Character should not be convertible to Date"));
    }

    private static Stream<Arguments> nanInfinityProvider() {
        Converter conv = new Converter(new ConverterOptions());
        return Stream.of(Arguments.of(Double.NaN, conv, "NaN is not a valid epoch millisecond"), Arguments.of(Double.POSITIVE_INFINITY, conv, "Infinity is not a valid epoch millisecond"));
    }

    @Test
    void toDate_fromLong_returnsCorrectDate() {
        // given
        // 2022‑01‑01T00:00:00Z
        long epochMilli = 1_640_995_200_000L;
        // when
        Date result = LocalDateConversions.toDate(epochMilli, sharedConverter);
        // then
        assertEquals(new Date(epochMilli), result);
    }

    @Test
    void toDate_fromInstant_returnsCorrectDate() {
        // given
        Instant instant = Instant.parse("2022-03-15T12:34:56Z");
        // when
        Date result = LocalDateConversions.toDate(instant, sharedConverter);
        // then
        assertEquals(Date.from(instant), result);
    }

    @Test
    void toDate_fromLocalDate_returnsStartOfDayInUTC() {
        // given
        LocalDate localDate = LocalDate.of(2021, 12, 31);
        // when
        Date result = LocalDateConversions.toDate(localDate, sharedConverter);
        // then
        // The conversion should treat the LocalDate as midnight UTC.
        long expectedEpochMilli = localDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
        assertEquals(new Date(expectedEpochMilli), result);
    }

    @Test
    void toDate_fromLocalDateTime_returnsCorrectDate() {
        // given
        LocalDateTime ldt = LocalDateTime.of(2022, 5, 10, 15, 30, 45);
        // when
        Date result = LocalDateConversions.toDate(ldt, sharedConverter);
        // then
        long expected = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
        assertEquals(new Date(expected), result);
    }

    @Test
    void toDate_fromZonedDateTime_returnsCorrectDate() {
        // given
        ZonedDateTime zdt = ZonedDateTime.of(2022, 6, 1, 8, 0, 0, 0, ZoneId.of("America/New_York"));
        // when
        Date result = LocalDateConversions.toDate(zdt, sharedConverter);
        // then
        long expected = zdt.toInstant().toEpochMilli();
        assertEquals(new Date(expected), result);
    }

    @Test
    void toDate_fromString_parsesUsingDefaultConverter() {
        // given
        String iso = "2022-07-04T10:15:30Z";
        // when
        Date result = LocalDateConversions.toDate(iso, sharedConverter);
        // then
        assertEquals(Date.from(Instant.parse(iso)), result);
    }

    @Test
    void toDate_fromNull_returnsNull() {
        // given
        Object source = null;
        // when
        Date result = LocalDateConversions.toDate(source, sharedConverter);
        // then
        assertNull(result);
    }

    @Test
    void toDate_fromUnsupportedType_throwsException() {
        // given
        Object unsupported = new Object();
        // when / then
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(unsupported, sharedConverter));
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("branchCoverageProvider")
    void toDate_branchCoverage(Object from, Converter converter, Object expected, String description) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception
            assertThrows((Class<? extends Throwable>) expected, () -> LocalDateConversions.toDate(from, converter), description);
        } else {
            // Expected a Date instance
            Date result = LocalDateConversions.toDate(from, converter);
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("additionalNumericProvider")
    void toDate_additionalNumericTypes(Object from, Converter converter, Date expected, String description) {
        Date result = LocalDateConversions.toDate(from, converter);
        assertEquals(expected, result, description);
    }

    @Test
    void toDate_sqlDate_isHandledLikeUtilDate() {
        java.sql.Date sqlDate = java.sql.Date.valueOf("2022-12-31");
        Date result = LocalDateConversions.toDate(sqlDate, sharedConverter);
        assertEquals(new Date(sqlDate.getTime()), result, "java.sql.Date should be treated the same as java.util.Date");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("timeOnlyProvider")
    void toDate_timeOnlyTypesShouldFail(Object from, Converter converter, String description) {
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(from, converter), description);
    }

    @Test
    void toDate_zonedDateTimeWithNonUtcZone() {
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 15, 13, 45, 30, 0, ZoneId.of("America/New_York"));
        Date result = LocalDateConversions.toDate(zdt, sharedConverter);
        // New York is UTC‑5 (or UTC‑4 with DST); on 2023‑05‑15 it is UTC‑4
        long expected = zdt.toInstant().toEpochMilli();
        assertEquals(new Date(expected), result, "ZonedDateTime with America/New_York should be converted to its instant");
    }

    @Test
    void toDate_offsetDateTimeNegativeOffset() {
        OffsetDateTime odt = OffsetDateTime.of(2023, 5, 15, 13, 45, 30, 0, ZoneOffset.ofHours(-5));
        Date result = LocalDateConversions.toDate(odt, sharedConverter);
        // should be 2023‑05‑15T18:45:30Z
        long expected = odt.toInstant().toEpochMilli();
        assertEquals(new Date(expected), result, "Negative offset should be adjusted forward to UTC");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("stringParsingProvider")
    void toDate_stringParsing(Object from, Converter converter, Object expected, String description) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<? extends Throwable>) expected, () -> LocalDateConversions.toDate(from, converter), description);
        } else {
            Date result = LocalDateConversions.toDate(from, converter);
            assertEquals(expected, result, description);
        }
    }

    @Test
    void toDate_dateWithNullConverterSucceeds() {
        Date source = new Date(0L);
        Date result = LocalDateConversions.toDate(source, null);
        assertEquals(source, result, "When source is already a Date, a null converter must not cause a failure");
    }

    @Test
    void toDate_stringWithNullConverterFails() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toDate("2023-05-15T13:45:30", null), "String conversion requires a non‑null Converter");
    }

    @Test
    void toDate_negativeEpochMillisHandledCorrectly() {
        // one day before epoch
        Date source = new Date(-86_400_000L);
        Date result = LocalDateConversions.toDate(source, sharedConverter);
        assertEquals(source, result, "Negative epoch millis should be preserved");
    }

    @Test
    void toDate_longMaxValueDoesNotOverflow() {
        long max = Long.MAX_VALUE;
        Date result = LocalDateConversions.toDate(Long.valueOf(max), sharedConverter);
        assertEquals(new Date(max), result, "Long.MAX_VALUE should be wrapped in a Date without overflow");
    }

    @Test
    void toDate_calendarWithNonDefaultZone() {
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("Asia/Tokyo"));
        cal.setTimeInMillis(1622505600000L);
        Date result = LocalDateConversions.toDate(cal, sharedConverter);
        assertEquals(new Date(1622505600000L), result, "Calendar instant should be independent of its zone");
    }

    @Test
    void toDate_nullSourceThrowsNPE() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toDate(null, sharedConverter), "Null source with a non‑null converter should raise NPE");
    }

    @Test
    void toDate_numericSourceWithNullConverterThrowsNPE() {
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toDate(Integer.valueOf(1622505600), null), "Numeric source requires a non‑null Converter");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("unsupportedWrapperProvider")
    void toDate_unsupportedWrapperTypes(Object from, Converter converter, String description) {
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(from, converter), description);
    }

    @Test
    void toDate_sqlTimeShouldFail() {
        java.sql.Time time = java.sql.Time.valueOf("13:45:30");
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(time, sharedConverter), "java.sql.Time lacks a date component and should fail");
    }

    @Test
    void toDate_byteArrayShouldFail() {
        byte[] bytes = new byte[] { 49, 50, 51 };
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(bytes, sharedConverter), "Raw byte[] cannot be interpreted as a date");
    }

    @Test
    void toDate_bigDecimalOverflowThrowsArithmeticException() {
        // Long.MAX_VALUE + 1
        BigDecimal tooBig = new BigDecimal("9223372036854775808");
        assertThrows(ArithmeticException.class, () -> LocalDateConversions.toDate(tooBig, sharedConverter), "Overflowing BigDecimal should raise ArithmeticException");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("nanInfinityProvider")
    void toDate_nanInfinityShouldThrowIllegalArgument(Object from, Converter converter, String description) {
        assertThrows(IllegalArgumentException.class, () -> LocalDateConversions.toDate(from, converter), description);
    }

    @Test
    void toDate_mapWithoutDateInfoFails() {
        CompactLinkedMap<String, Object> map = new CompactLinkedMap<>();
        assertThrows(RuntimeException.class, () -> LocalDateConversions.toDate(map, sharedConverter), "A generic map without date information should cause conversion failure");
    }
}
