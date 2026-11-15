package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toZonedDateTime_Test {

    /**
     * The Converter used for the tests.
     */
    private Converter converter;

    /**
     * The ZoneId that the Converter is configured with (defaults to the system zone).
     */
    private ZoneId zoneId;

    /**
     * A sample {@link Date} instance used as input.
     */
    private Date sampleDate;

    /**
     * A sample {@link Calendar} instance used as input.
     */
    private Calendar sampleCalendar;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a real Converter with default options.
        // ConverterOptions has a no‑arg constructor that supplies sensible defaults
        // (including the system default ZoneId).
        this.converter = new Converter(new ConverterOptions());
        this.zoneId = converter.getOptions().getZoneId();
        // Prepare deterministic input values.
        // 2022‑01‑01T00:00:00Z
        this.sampleDate = new Date(1_640_995_200_000L);
        this.sampleCalendar = new GregorianCalendar();
        this.sampleCalendar.setTimeInMillis(sampleDate.getTime());
    }

    /**
     * Helper that creates a mock {@link Converter} returning the supplied {@link ZoneId}.
     */
    private static Converter mockConverter(ZoneId zoneId) {
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(zoneId);
        return converter;
    }

    private static Stream<Arguments> validDateToZonedDateTime() {
        return Stream.of(Arguments.of("Converts a java.util.Date representing 2021‑10‑01T00:00:00Z to a ZonedDateTime in UTC.", new Date(1633072800000L), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1633072800000L), ZoneId.of("UTC"))));
    }

    private static Stream<Arguments> validInstantToZonedDateTime() {
        return Stream.of(Arguments.of("Converts an Instant to ZonedDateTime using the New York time‑zone, verifying offset handling.", Instant.parse("2022-05-15T12:30:45Z"), ZoneId.of("America/New_York"), ZonedDateTime.ofInstant(Instant.parse("2022-05-15T12:30:45Z"), ZoneId.of("America/New_York"))));
    }

    private static Stream<Arguments> validLocalDateTimeToZonedDateTime() {
        return Stream.of(Arguments.of("Converts a LocalDateTime (no zone) to ZonedDateTime using the converter's zone (Paris).", LocalDateTime.of(2023, 3, 10, 14, 45, 30), ZoneId.of("Europe/Paris"), ZonedDateTime.of(2023, 3, 10, 14, 45, 30, 0, ZoneId.of("Europe/Paris"))));
    }

    private static Stream<Arguments> validOffsetDateTimeToZonedDateTime() {
        return Stream.of(Arguments.of("Ensures an OffsetDateTime is first turned into an Instant and then placed in the converter's zone (Tokyo).", OffsetDateTime.parse("2021-12-31T23:00:00+02:00"), ZoneId.of("Asia/Tokyo"), ZonedDateTime.ofInstant(OffsetDateTime.parse("2021-12-31T23:00:00+02:00").toInstant(), ZoneId.of("Asia/Tokyo"))));
    }

    private static Stream<Arguments> validTimestampToZonedDateTime() {
        return Stream.of(Arguments.of("Converts a java.sql.Timestamp to ZonedDateTime in UTC.", new Timestamp(1650000000000L), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochMilli(1650000000000L), ZoneId.of("UTC"))));
    }

    private static Stream<Arguments> validBigDecimalSecondsToZonedDateTime() {
        return Stream.of(Arguments.of("When the source is a BigDecimal representing epoch seconds, it should be converted to an Instant and then to ZonedDateTime.", new BigDecimal("1622505600"), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1622505600L), ZoneId.of("UTC"))));
    }

    private static Stream<Arguments> validBigDecimalFractionalToZonedDateTime() {
        return Stream.of(Arguments.of("Converts a BigDecimal that contains seconds and nanoseconds to an Instant and then to a ZonedDateTime in UTC.", new BigDecimal("1622505600.123456789"), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1622505600L, 123456789L), ZoneId.of("UTC"))));
    }

    private static Stream<Arguments> validBigIntegerToZonedDateTime() {
        return Stream.of(Arguments.of("Converts a BigInteger representing epoch seconds to a ZonedDateTime in UTC.", new BigInteger("1622505600"), ZoneId.of("UTC"), ZonedDateTime.ofInstant(Instant.ofEpochSecond(1622505600L), ZoneId.of("UTC"))));
    }

    private static Stream<Arguments> validZonedDateTimeSourceToDifferentZone() {
        return Stream.of(Arguments.of("Ensures a source ZonedDateTime is first turned into an Instant and then re‑zoned to the converter's zone (UTC).", ZonedDateTime.of(2022, 5, 20, 15, 30, 0, 0, ZoneId.of("America/Los_Angeles")), ZoneId.of("UTC"), ZonedDateTime.ofInstant(ZonedDateTime.of(2022, 5, 20, 15, 30, 0, 0, ZoneId.of("America/Los_Angeles")).toInstant(), ZoneId.of("UTC"))));
    }

    private static Stream<Arguments> validCalendarToZonedDateTime() {
        // Build a GregorianCalendar set to UTC 2022‑01‑01T12:00:00Z
        GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.set(2022, Calendar.JANUARY, 1, 12, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return Stream.of(Arguments.of("Converts a java.util.Calendar (UTC) representing 2022‑01‑01T12:00:00Z to a ZonedDateTime in the Europe/Berlin zone.", cal, ZoneId.of("Europe/Berlin"), ZonedDateTime.ofInstant(cal.toInstant(), ZoneId.of("Europe/Berlin"))));
    }

    private static Stream<Arguments> validSqlDateToZonedDateTime() {
        return Stream.of(Arguments.of("Converts a java.sql.Date (midnight of 2021‑12‑31) to a ZonedDateTime in UTC.", java.sql.Date.valueOf(LocalDate.of(2021, 12, 31)), ZoneId.of("UTC"), ZonedDateTime.ofInstant(((java.sql.Date) java.sql.Date.valueOf(LocalDate.of(2021, 12, 31))).toInstant(), ZoneId.of("UTC"))));
    }

    private static Stream<Arguments> unsupportedSourceTypes() {
        return Stream.of(Arguments.of("Passing a String that is not a supported conversion source should result in an IllegalArgumentException.", "2022-01-01T00:00:00Z"), Arguments.of("Passing a UUID, which is not a supported conversion source, should result in an IllegalArgumentException.", UUID.randomUUID()), Arguments.of("Passing an Integer (or any generic Number not explicitly handled) should trigger an IllegalArgumentException.", Integer.valueOf(12345)));
    }

    private static Stream<Arguments> dateTimeExceptionSources() {
        return Stream.of(Arguments.of("Converting a LocalTime (which lacks a date) to an Instant is undefined and should throw a DateTimeException.", LocalTime.of(12, 34, 56)), Arguments.of("Converting a LocalDate (which lacks a time component) to an Instant is undefined and should throw a DateTimeException.", LocalDate.of(2022, 1, 1)));
    }

    /**
     * Verify that {@code BigDecimalConversions.toZonedDateTime} correctly converts a {@link Date}
     * into a {@link ZonedDateTime} using the zone configured in the {@link Converter}.
     */
    @Test
    void toZonedDateTime_fromDate_matchesInstantAtConfiguredZone() {
        // GIVEN – a Date and a Converter with a known ZoneId (captured in the test setup)
        // WHEN
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(sampleDate, converter);
        // THEN
        ZonedDateTime expected = sampleDate.toInstant().atZone(zoneId);
        assertEquals(expected, result, "ZonedDateTime derived from Date should match Instant.atZone(zoneId)");
    }

    /**
     * Verify that {@code BigDecimalConversions.toZonedDateTime} correctly converts a {@link Calendar}
     * into a {@link ZonedDateTime} using the zone configured in the {@link Converter}.
     */
    @Test
    void toZonedDateTime_fromCalendar_matchesInstantAtConfiguredZone() {
        // GIVEN – a Calendar instance representing the same instant as {@code sampleDate}
        // WHEN
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(sampleCalendar, converter);
        // THEN
        ZonedDateTime expected = sampleCalendar.toInstant().atZone(zoneId);
        assertEquals(expected, result, "ZonedDateTime derived from Calendar should match Instant.atZone(zoneId)");
    }

    /**
     * Verify that a {@link java.math.BigDecimal} representing epoch seconds is converted correctly.
     * The {@code toInstant} implementation treats a {@code BigDecimal} as seconds‑and‑nanoseconds,
     * so we provide a value with a fractional part.
     */
    @Test
    void toZonedDateTime_fromBigDecimal_secondsAndNanos() {
        // GIVEN – a BigDecimal representing 2022‑01‑01T12:30:15.250Z
        // 1641034215.250 seconds since the epoch
        java.math.BigDecimal bd = new java.math.BigDecimal("1641034215.250");
        // WHEN
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(bd, converter);
        // THEN
        ZonedDateTime expected = java.time.Instant.ofEpochSecond(1641034215L, 250_000_000L).atZone(zoneId);
        assertEquals(expected, result, "ZonedDateTime derived from BigDecimal should reflect seconds and nanos");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validDateToZonedDateTime")
    void validConversions_dateToZonedDateTime(String description, Date from, ZoneId zoneId, ZonedDateTime expected) {
        Converter converter = mockConverter(zoneId);
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validInstantToZonedDateTime")
    void validConversions_instantToZonedDateTime(String description, Instant from, ZoneId zoneId, ZonedDateTime expected) {
        Converter converter = mockConverter(zoneId);
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validLocalDateTimeToZonedDateTime")
    void validConversions_localDateTimeToZonedDateTime(String description, LocalDateTime from, ZoneId zoneId, ZonedDateTime expected) {
        Converter converter = mockConverter(zoneId);
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validOffsetDateTimeToZonedDateTime")
    void validConversions_offsetDateTimeToZonedDateTime(String description, OffsetDateTime from, ZoneId zoneId, ZonedDateTime expected) {
        Converter converter = mockConverter(zoneId);
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validTimestampToZonedDateTime")
    void validConversions_timestampToZonedDateTime(String description, Timestamp from, ZoneId zoneId, ZonedDateTime expected) {
        Converter converter = mockConverter(zoneId);
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validBigDecimalSecondsToZonedDateTime")
    void validConversions_bigDecimalSecondsToZonedDateTime(String description, BigDecimal from, ZoneId zoneId, ZonedDateTime expected) {
        Converter converter = mockConverter(zoneId);
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validBigDecimalFractionalToZonedDateTime")
    void validConversions_bigDecimalFractionalToZonedDateTime(String description, BigDecimal from, ZoneId zoneId, ZonedDateTime expected) {
        Converter converter = mockConverter(zoneId);
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validBigIntegerToZonedDateTime")
    void validConversions_bigIntegerToZonedDateTime(String description, BigInteger from, ZoneId zoneId, ZonedDateTime expected) {
        Converter converter = mockConverter(zoneId);
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validZonedDateTimeSourceToDifferentZone")
    void validConversions_zonedDateTimeSourceToDifferentZone(String description, ZonedDateTime from, ZoneId targetZone, ZonedDateTime expected) {
        Converter converter = mockConverter(targetZone);
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validCalendarToZonedDateTime")
    void validConversions_calendarToZonedDateTime(String description, Calendar from, ZoneId zoneId, ZonedDateTime expected) {
        Converter converter = mockConverter(zoneId);
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validSqlDateToZonedDateTime")
    void validConversions_sqlDateToZonedDateTime(String description, java.sql.Date from, ZoneId zoneId, ZonedDateTime expected) {
        Converter converter = mockConverter(zoneId);
        ZonedDateTime result = BigDecimalConversions.toZonedDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @Test
    void error_nullSource_throwsNullPointerException() {
        Converter converter = mockConverter(ZoneId.of("UTC"));
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toZonedDateTime(null, converter), "Passing a null 'from' argument should result in a NullPointerException");
    }

    @Test
    void error_nullConverter_throwsNullPointerException() {
        Date date = new Date();
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toZonedDateTime(date, null), "A null Converter should cause a NullPointerException when its options are accessed");
    }

    @Test
    void error_converterWithNullZoneId_throwsNullPointerException() {
        // Mock a Converter whose options return a null ZoneId
        Converter converter = mock(Converter.class);
        ConverterOptions options = mock(ConverterOptions.class);
        when(converter.getOptions()).thenReturn(options);
        when(options.getZoneId()).thenReturn(null);
        Date date = new Date();
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toZonedDateTime(date, converter), "If the Converter's Options return a null ZoneId, atZone(null) should throw NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedSourceTypes")
    void error_unsupportedSourceTypes_throwIllegalArgumentException(String description, Object from) {
        Converter converter = mockConverter(ZoneId.of("UTC"));
        assertThrows(IllegalArgumentException.class, () -> BigDecimalConversions.toZonedDateTime(from, converter), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("dateTimeExceptionSources")
    void error_dateTimeExceptionSources_throwDateTimeException(String description, Object from) {
        Converter converter = mockConverter(ZoneId.of("UTC"));
        assertThrows(DateTimeException.class, () -> BigDecimalConversions.toZonedDateTime(from, converter), description);
    }

    @Test
    void error_dateEpochOverflow_throwsDateTimeException() {
        Converter converter = mockConverter(ZoneId.of("UTC"));
        // beyond Instant range
        Date overflowDate = new Date(Long.MAX_VALUE);
        assertThrows(DateTimeException.class, () -> BigDecimalConversions.toZonedDateTime(overflowDate, converter), "A java.util.Date whose epoch milliseconds exceed the range of java.time.Instant should cause a DateTimeException");
    }

    @Test
    void error_bigIntegerOverflow_throwsDateTimeException() {
        Converter converter = mockConverter(ZoneId.of("UTC"));
        // 2^63 is one more than Long.MAX_VALUE
        BigInteger overflow = new BigInteger("9223372036854775808");
        assertThrows(DateTimeException.class, () -> BigDecimalConversions.toZonedDateTime(overflow, converter), "A BigInteger representing epoch seconds larger than Long.MAX_VALUE is out of range for Instant and should throw a DateTimeException");
    }
}
