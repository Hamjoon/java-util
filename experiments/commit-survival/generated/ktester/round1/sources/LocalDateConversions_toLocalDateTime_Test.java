package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toLocalDateTime_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    /**
     * Original default zone – restored in {@link #teardownAfterAll()}.
     */
    private static ZoneId originalDefaultZone;

    @BeforeAll
    static void setupBeforeAll() {
        // Force UTC to make the expected values independent of the machine running the tests.
        originalDefaultZone = ZoneId.systemDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // The library’s Converter needs a ConverterOptions instance; the default constructor is sufficient.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original default zone.
        TimeZone.setDefault(TimeZone.getTimeZone(originalDefaultZone));
        converter = null;
    }

    private static Stream<Arguments> provideDateEpochCases() {
        // The JVM default zone is forced to UTC in @BeforeAll.
        return Stream.of(Arguments.of("Epoch Date (0 ms) → 1970‑01‑01T00:00", new Date(0L), LocalDateTime.of(1970, 1, 1, 0, 0)));
    }

    private static Stream<Arguments> provideTimestampCases() {
        return Stream.of(Arguments.of("Timestamp with nanosecond precision", Timestamp.valueOf("2023-08-15 12:34:56.123456789"), LocalDateTime.of(2023, 8, 15, 12, 34, 56, 123_456_789)));
    }

    private static Stream<Arguments> provideLocalDateTimeIdentity() {
        return Stream.of(Arguments.of("Identity conversion", LocalDateTime.of(2021, 12, 31, 23, 59, 59)));
    }

    private static Stream<Arguments> provideLocalDateCases() {
        return Stream.of(Arguments.of("LocalDate → start of day", LocalDate.of(2022, 2, 28), LocalDateTime.of(2022, 2, 28, 0, 0)));
    }

    private static Stream<Arguments> provideInstantCases() {
        return Stream.of(Arguments.of("Instant → LocalDateTime using UTC", Instant.parse("2020-01-01T10:15:30Z"), LocalDateTime.of(2020, 1, 1, 10, 15, 30)));
    }

    private static Stream<Arguments> provideCalendarCases() {
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(0L);
        return Stream.of(Arguments.of("Calendar epoch (UTC) → 1970‑01‑01T00:00", utcCal, LocalDateTime.of(1970, 1, 1, 0, 0)));
    }

    private static Stream<Arguments> provideLongEpochCases() {
        return Stream.of(Arguments.of("Long 0 → epoch", 0L, LocalDateTime.of(1970, 1, 1, 0, 0)), Arguments.of("Long 1622505600000 → 2021‑06‑01T00:00", 1622505600000L, LocalDateTime.of(2021, 6, 1, 0, 0)));
    }

    private static Stream<Arguments> provideIsoStringCases() {
        return Stream.of(Arguments.of("ISO string without offset", "2023-08-15T12:34:56", LocalDateTime.of(2023, 8, 15, 12, 34, 56)));
    }

    private static Stream<Arguments> provideIsoStringWithOffsetCases() {
        // Input is +02:00, system default is UTC → subtract 2 hours.
        return Stream.of(Arguments.of("ISO string with +02:00 offset normalized to UTC", "2023-08-15T12:34:56+02:00", LocalDateTime.of(2023, 8, 15, 10, 34, 56)));
    }

    @Test
    void shouldReturnSameLocalDateTimeWhenInputIsLocalDateTime() {
        // GIVEN
        LocalDateTime now = LocalDateTime.now();
        // WHEN
        LocalDateTime result = LocalDateConversions.toLocalDateTime(now, converter);
        // THEN
        assertEquals(now, result, "Conversion of a LocalDateTime should return the same value");
    }

    @Test
    void shouldConvertJavaUtilDateToLocalDateTime() {
        // GIVEN
        // Epoch start – 1970‑01‑01T00:00:00Z
        Date epochDate = new Date(0L);
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(Instant.ofEpochMilli(0L), zone);
        // WHEN
        LocalDateTime result = LocalDateConversions.toLocalDateTime(epochDate, converter);
        // THEN
        assertEquals(expected, result, "Conversion of java.util.Date should produce the equivalent LocalDateTime in the system default zone");
    }

    @Test
    void shouldConvertSqlTimestampToLocalDateTime() {
        // GIVEN
        // Timestamp representing 2023‑03‑15T10:20:30.123
        Timestamp ts = Timestamp.valueOf("2023-03-15 10:20:30.123");
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(ts.toInstant(), zone);
        // WHEN
        LocalDateTime result = LocalDateConversions.toLocalDateTime(ts, converter);
        // THEN
        assertEquals(expected, result, "Conversion of java.sql.Timestamp should produce the equivalent LocalDateTime");
    }

    @Test
    void shouldConvertInstantToLocalDateTime() {
        // GIVEN
        Instant instant = Instant.parse("2022-12-31T23:59:59Z");
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime expected = LocalDateTime.ofInstant(instant, zone);
        // WHEN
        LocalDateTime result = LocalDateConversions.toLocalDateTime(instant, converter);
        // THEN
        assertEquals(expected, result, "Conversion of Instant should produce the equivalent LocalDateTime in the system default zone");
    }

    @Test
    void shouldParseIsoStringToLocalDateTime() {
        // GIVEN
        String iso = "2021-07-20T15:45:30";
        LocalDateTime expected = LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        // WHEN
        LocalDateTime result = LocalDateConversions.toLocalDateTime(iso, converter);
        // THEN
        assertEquals(expected, result, "Conversion of an ISO‑8601 string should be parsed into the correct LocalDateTime");
    }

    @Test
    void shouldReturnNullWhenInputIsNull() {
        // GIVEN
        Object nullInput = null;
        // WHEN
        LocalDateTime result = LocalDateConversions.toLocalDateTime(nullInput, converter);
        // THEN
        assertNull(result, "Conversion of null should result in null");
    }

    /**
     * Date → LocalDateTime (epoch, UTC)
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideDateEpochCases")
    void shouldConvertDateEpoch(String description, Object input, LocalDateTime expected) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(input, null);
        assertEquals(expected, result, description);
    }

    /**
     * Timestamp → LocalDateTime (preserve nanoseconds)
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideTimestampCases")
    void shouldConvertTimestamp(String description, Timestamp ts, LocalDateTime expected) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(ts, null);
        assertEquals(expected, result, description);
    }

    /**
     * LocalDateTime identity
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideLocalDateTimeIdentity")
    void shouldReturnSameInstanceForLocalDateTime(String description, LocalDateTime src) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(src, null);
        assertEquals(src, result, description);
    }

    /**
     * LocalDate → LocalDateTime (midnight)
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideLocalDateCases")
    void shouldConvertLocalDateToMidnight(String description, LocalDate date, LocalDateTime expected) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(date, null);
        assertEquals(expected, result, description);
    }

    /**
     * Calendar → LocalDateTime (epoch, UTC)
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideCalendarCases")
    void shouldConvertCalendarToLocalDateTime(String description, Calendar cal, LocalDateTime expected) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(cal, null);
        assertEquals(expected, result, description);
    }

    /**
     * Long epoch milliseconds → LocalDateTime
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideLongEpochCases")
    void shouldConvertLongEpochToLocalDateTime(String description, Long epochMs, LocalDateTime expected) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(epochMs, null);
        assertEquals(expected, result, description);
    }

    /**
     * ISO‑8601 strings without offset
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideIsoStringCases")
    void shouldParseIsoStringWithoutOffset(String description, String iso, LocalDateTime expected) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(iso, null);
        assertEquals(expected, result, description);
    }

    /**
     * ISO‑8601 strings with zone offset – should be normalized to UTC
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideIsoStringWithOffsetCases")
    void shouldParseIsoStringWithOffset(String description, String isoWithOffset, LocalDateTime expected) {
        LocalDateTime result = LocalDateConversions.toLocalDateTime(isoWithOffset, null);
        assertEquals(expected, result, description);
    }

    /**
     * Custom formatted string using a supplied Converter
     */
    @Test
    void shouldParseCustomFormattedStringUsingCustomConverter() {
        // GIVEN
        String source = "15/08/2023 12:34";
        // a Converter that knows how to parse the custom pattern
        Converter custom = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                // The target is expected to be LocalDateTime (the library will call this)
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return fmt.parse((String) src, LocalDateTime::from);
            }
        };
        LocalDateTime expected = LocalDateTime.of(2023, 8, 15, 12, 34);
        // WHEN
        LocalDateTime result = LocalDateConversions.toLocalDateTime(source, custom);
        // THEN
        assertEquals(expected, result, "Custom converter should parse the supplied pattern into the correct LocalDateTime");
    }

    /**
     * Null source value
     */
    @Test
    void shouldReturnNullWhenSourceIsNullEvenIfConverterIsNull() {
        // GIVEN
        Object src = null;
        // WHEN
        LocalDateTime result = LocalDateConversions.toLocalDateTime(src, null);
        // THEN
        assertNull(result, "Null source should yield null result");
    }

    /**
     * Unsupported source type – expect ConversionException
     */
    @Test
    void shouldThrowConversionExceptionForUnsupportedType() {
        // GIVEN
        Boolean unsupported = Boolean.TRUE;
        // WHEN / THEN
        assertThrows(ConversionException.class, () -> LocalDateConversions.toLocalDateTime(unsupported, null), "Converting an unsupported type should throw ConversionException");
    }

    /**
     * LocalTime alone – should fail
     */
    @Test
    void shouldThrowConversionExceptionForLocalTimeOnly() {
        // GIVEN
        LocalTime timeOnly = LocalTime.of(14, 30, 15);
        // WHEN / THEN
        assertThrows(ConversionException.class, () -> LocalDateConversions.toLocalDateTime(timeOnly, null), "Converting a LocalTime without a date component should throw ConversionException");
    }
}
