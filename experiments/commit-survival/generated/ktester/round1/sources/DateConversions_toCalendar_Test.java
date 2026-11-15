package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toCalendar_Test {

    /**
     * The Converter used by the methods under test.
     */
    @Mock
    private Converter // a mock is sufficient – the conversion path we test does not need real behaviour
    converter;

    /**
     * The mock is used for the majority of tests where the converter does not affect the outcome.
     */
    @org.mockito.Mock
    private Converter mockConverter;

    private static Stream<Arguments> numericInputProvider() {
        return Stream.of(Arguments.of(123456789L, 123456789L), Arguments.of("987654321", 987654321L));
    }

    private static Stream<Arguments> dateTimeObjectProvider() {
        return Stream.of(// 2021‑01‑01T00:00:00Z
        Arguments.of(new Date(1609459200000L), 1609459200000L), Arguments.of(Timestamp.valueOf("2022-12-31 23:59:59.123"), Timestamp.valueOf("2022-12-31 23:59:59.123").getTime()));
    }

    private static Stream<Arguments> bigNumberProvider() {
        return Stream.of(// fractional part truncated
        Arguments.of(new BigDecimal("123456789.987"), 123456789L), Arguments.of(new BigInteger("9876543210"), 9876543210L), Arguments.of(new AtomicLong(555555555L), 555555555L));
    }

    private static Stream<Arguments> javaTimeProvider() {
        // Helper to compute epoch millis for the expected values
        long localDateEpoch = LocalDate.of(2020, 2, 29).atStartOfDay(ZoneOffset.UTC).toInstant().// 1582934400000L
        toEpochMilli();
        long localDateTimeEpoch = LocalDateTime.of(2021, 12, 31, 23, 59, 59, 123_000_000).atZone(ZoneId.systemDefault()).toInstant().// depends on default zone – compute dynamically
        toEpochMilli();
        long localTimeEpoch = LocalTime.of(12, 34, 56, 789_000_000).toSecondOfDay() * 1000L + // 45296000L
        789L;
        long instantEpoch = 1609459200123L;
        long zonedDateTimeEpoch = ZonedDateTime.of(2022, 3, 15, 10, 20, 30, 0, ZoneId.of("America/New_York")).toInstant().// 1647352830000L
        toEpochMilli();
        long offsetDateTimeEpoch = OffsetDateTime.of(2022, 3, 15, 10, 20, 30, 0, ZoneOffset.ofHours(-5)).toInstant().// same as above
        toEpochMilli();
        // 1622505600000L
        long sqlDateEpoch = new java.sql.Date(1622505600000L).getTime();
        return Stream.of(Arguments.of(LocalDate.of(2020, 2, 29), localDateEpoch), Arguments.of(LocalDateTime.of(2021, 12, 31, 23, 59, 59, 123_000_000), localDateTimeEpoch), Arguments.of(LocalTime.of(12, 34, 56, 789_000_000), localTimeEpoch), Arguments.of(Instant.ofEpochMilli(1609459200123L), instantEpoch), Arguments.of(ZonedDateTime.of(2022, 3, 15, 10, 20, 30, 0, ZoneId.of("America/New_York")), zonedDateTimeEpoch), Arguments.of(OffsetDateTime.of(2022, 3, 15, 10, 20, 30, 0, ZoneOffset.ofHours(-5)), offsetDateTimeEpoch), Arguments.of(new java.sql.Date(1622505600000L), sqlDateEpoch));
    }

    private static Stream<Arguments> edgeNumericProvider() {
        return Stream.of(Arguments.of(0L, 0L), Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE), Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE), Arguments.of("-123456789", -123456789L));
    }

    private static Stream<Arguments> malformedStringProvider() {
        return Stream.of(Arguments.of("not-a-number", NumberFormatException.class), Arguments.of("1234abc", NumberFormatException.class), // whitespace‑padded numeric string should succeed – not an exception case
        Arguments.of("   987654321   ", null));
    }

    /**
     * Verify that a {@link Long} value is correctly transformed into a {@link Calendar}
     * containing the same epoch‑millisecond value.
     */
    @Test
    void toCalendar_fromLong_returnsCorrectCalendar() {
        // given
        // 2021‑06‑01T00:00:00Z
        Long epochMillis = 1_622_548_800_000L;
        // when
        Calendar result = DateConversions.toCalendar(epochMillis, converter);
        // then
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(epochMillis.longValue(), result.getTimeInMillis(), "Calendar should contain the same epoch milliseconds as the input Long");
    }

    /**
     * Verify that a {@link Date} instance is correctly transformed into a {@link Calendar}
     * containing the same epoch‑millisecond value.
     */
    @Test
    void toCalendar_fromDate_returnsCorrectCalendar() {
        // given
        // same instant as above
        Date date = new Date(1_622_548_800_000L);
        // when
        Calendar result = DateConversions.toCalendar(date, converter);
        // then
        assertNotNull(result, "Resulting Calendar should not be null");
        assertEquals(date.getTime(), result.getTimeInMillis(), "Calendar should contain the same epoch milliseconds as the input Date");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("numericInputProvider")
    void toCalendar_numericInputs(Object from, long expectedMillis) {
        // Use a real IdentityConverter – it simply returns the supplied object.
        Converter identity = new IdentityConverter();
        Calendar cal = DateConversions.toCalendar(from, identity);
        assertNotNull(cal);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar should contain the expected epoch milliseconds");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("dateTimeObjectProvider")
    void toCalendar_dateTimeObjects(Object from, long expectedMillis) {
        Calendar cal = DateConversions.toCalendar(from, new IdentityConverter());
        assertNotNull(cal);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar should reflect the epoch millis of the source date/time object");
    }

    @Test
    void toCalendar_nullSource_throwsNPE() {
        assertThrows(NullPointerException.class, () -> DateConversions.toCalendar(null, new IdentityConverter()), "Null source should cause NullPointerException");
    }

    @Test
    void toCalendar_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> DateConversions.toCalendar(123L, null), "Null converter should cause NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("bigNumberProvider")
    void toCalendar_bigNumberTypes(Object from, long expectedMillis) {
        Calendar cal = DateConversions.toCalendar(from, new IdentityConverter());
        assertNotNull(cal);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar should contain the truncated/converted long value");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("javaTimeProvider")
    void toCalendar_javaTimeObjects(Object from, long expectedMillis) {
        Calendar cal = DateConversions.toCalendar(from, new IdentityConverter());
        assertNotNull(cal);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar should represent the correct UTC epoch millis");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("edgeNumericProvider")
    void toCalendar_edgeNumericValues(Object from, long expectedMillis) {
        Calendar cal = DateConversions.toCalendar(from, new IdentityConverter());
        assertNotNull(cal);
        assertEquals(expectedMillis, cal.getTimeInMillis(), "Calendar should correctly handle edge numeric values");
    }

    @ParameterizedTest(name = "[{index}] from={0}")
    @MethodSource("malformedStringProvider")
    void toCalendar_malformedStringInputs(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> DateConversions.toCalendar(from, new IdentityConverter()), "Expected exception for malformed string input");
    }

    @Test
    void toCalendar_whitespacePaddedString_parsesSuccessfully() {
        Calendar cal = DateConversions.toCalendar("   987654321   ", new IdentityConverter());
        assertNotNull(cal);
        assertEquals(987654321L, cal.getTimeInMillis(), "Whitespace‑padded numeric string should be trimmed and parsed");
    }

    @Test
    void toCalendar_customEpochAddingConverter() {
        // Converter that parses ISO‑8601 string to epoch millis and adds 1000 ms
        Converter adding = new EpochMillisAddingConverter(1000L);
        Calendar cal = DateConversions.toCalendar("2023-01-01T00:00:00Z", adding);
        assertNotNull(cal);
        assertEquals(1672531201000L, cal.getTimeInMillis(), "Custom converter should add 1000 ms to the parsed epoch value");
    }

    @Test
    void toCalendar_customMultiplierConverter() {
        // Converter that multiplies the long value by 2
        Converter multiplier = new MultiplierConverter(2);
        Calendar cal = DateConversions.toCalendar(new Date(2000L), multiplier);
        assertNotNull(cal);
        assertEquals(4000L, cal.getTimeInMillis(), "Custom multiplier converter should double the epoch millis");
    }

    @Test
    void toCalendar_unsupportedSource_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> DateConversions.toCalendar(new Object(), new IdentityConverter()), "Unsupported source type should result in IllegalArgumentException");
    }
}
