package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class OffsetDateTimeConversions_toMap_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the built‑in conversions are sufficient for the tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    /**
     * OffsetDateTime input – expects a map with the original offset.
     */
    static Stream<Arguments> offsetDateTimeProvider() {
        OffsetDateTime odt = OffsetDateTime.parse("2023-08-15T13:45:30+02:00");
        Map<String, String> expected = Map.of(MapConversions.DATE, "2023-08-15", MapConversions.TIME, "13:45:30", MapConversions.OFFSET, "+02:00");
        return Stream.of(Arguments.of(odt, null, expected));
    }

    /**
     * ZonedDateTime input – seconds are omitted when they are zero.
     */
    static Stream<Arguments> zonedDateTimeProvider() {
        ZonedDateTime zdt = ZonedDateTime.parse("2022-12-01T09:15:00-05:00[America/New_York]");
        Map<String, String> expected = Map.of(MapConversions.DATE, "2022-12-01", MapConversions.TIME, "09:15", MapConversions.OFFSET, "-05:00");
        return Stream.of(Arguments.of(zdt, null, expected));
    }

    /**
     * LocalDateTime input – interpreted as UTC (offset Z).
     */
    static Stream<Arguments> localDateTimeProvider() {
        LocalDateTime ldt = LocalDateTime.of(2021, 5, 10, 23, 59, 59);
        Map<String, String> expected = Map.of(MapConversions.DATE, "2021-05-10", MapConversions.TIME, "23:59:59", MapConversions.OFFSET, "Z");
        return Stream.of(Arguments.of(ldt, null, expected));
    }

    /**
     * java.util.Date input – represents an instant in UTC.
     */
    static Stream<Arguments> utilDateProvider() {
        Date date = Date.from(Instant.parse("2020-01-01T00:00:00Z"));
        Map<String, String> expected = Map.of(MapConversions.DATE, "2020-01-01", MapConversions.TIME, "00:00", MapConversions.OFFSET, "Z");
        return Stream.of(Arguments.of(date, null, expected));
    }

    /**
     * Instant input – treated as UTC.
     */
    static Stream<Arguments> instantProvider() {
        Instant instant = Instant.parse("1999-12-31T23:59:59Z");
        Map<String, String> expected = Map.of(MapConversions.DATE, "1999-12-31", MapConversions.TIME, "23:59:59", MapConversions.OFFSET, "Z");
        return Stream.of(Arguments.of(instant, null, expected));
    }

    /**
     * OffsetTime input – unsupported, should propagate IllegalArgumentException.
     */
    static Stream<Arguments> offsetTimeUnsupportedProvider() {
        OffsetTime ot = OffsetTime.parse("12:34:56+03:00");
        return Stream.of(Arguments.of(ot, null, IllegalArgumentException.class));
    }

    /**
     * Null source object – expected NullPointerException (thrown by toZonedDateTime).
     */
    static Stream<Arguments> nullInputProvider() {
        return Stream.of(Arguments.of(null, null, NullPointerException.class));
    }

    /**
     * java.sql.Timestamp input – UTC instant.
     */
    static Stream<Arguments> timestampProvider() {
        Timestamp ts = Timestamp.from(Instant.parse("2022-03-10T12:30:45Z"));
        Map<String, String> expected = Map.of(MapConversions.DATE, "2022-03-10", MapConversions.TIME, "12:30:45", MapConversions.OFFSET, "Z");
        return Stream.of(Arguments.of(ts, null, expected));
    }

    /**
     * java.sql.Date input – date only, time defaults to 00:00, offset Z.
     */
    static Stream<Arguments> sqlDateProvider() {
        java.sql.Date sqlDate = new java.sql.Date(Instant.parse("2021-07-04T00:00:00Z").toEpochMilli());
        Map<String, String> expected = Map.of(MapConversions.DATE, "2021-07-04", MapConversions.TIME, "00:00", MapConversions.OFFSET, "Z");
        return Stream.of(Arguments.of(sqlDate, null, expected));
    }

    /**
     * Calendar input (UTC).
     */
    static Stream<Arguments> calendarUtcProvider() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2020, Calendar.JUNE, 1, 15, 45, 30);
        cal.set(Calendar.MILLISECOND, 0);
        Map<String, String> expected = Map.of(MapConversions.DATE, "2020-06-01", MapConversions.TIME, "15:45:30", MapConversions.OFFSET, "Z");
        return Stream.of(Arguments.of(cal, null, expected));
    }

    /**
     * AtomicLong (epoch millis) – interpreted as UTC instant.
     */
    static Stream<Arguments> atomicLongProvider() {
        AtomicLong al = new AtomicLong(Instant.parse("2019-12-31T23:59:59Z").toEpochMilli());
        Map<String, String> expected = Map.of(MapConversions.DATE, "2019-12-31", MapConversions.TIME, "23:59:59", MapConversions.OFFSET, "Z");
        return Stream.of(Arguments.of(al, null, expected));
    }

    /**
     * String in ISO_OFFSET_DATE_TIME format – normal conversion.
     */
    static Stream<Arguments> isoStringProvider() {
        String iso = "2023-01-02T14:20:00+05:30";
        Map<String, String> expected = Map.of(MapConversions.DATE, "2023-01-02", MapConversions.TIME, "14:20:00", MapConversions.OFFSET, "+05:30");
        return Stream.of(Arguments.of(iso, null, expected));
    }

    /**
     * LocalDate input – start of day at UTC.
     */
    static Stream<Arguments> localDateProvider() {
        LocalDate ld = LocalDate.of(2022, 2, 28);
        Map<String, String> expected = Map.of(MapConversions.DATE, "2022-02-28", MapConversions.TIME, "00:00", MapConversions.OFFSET, "Z");
        return Stream.of(Arguments.of(ld, null, expected));
    }

    /**
     * LocalTime input – unsupported, IllegalArgumentException expected.
     */
    static Stream<Arguments> localTimeUnsupportedProvider() {
        LocalTime lt = LocalTime.of(10, 15, 30);
        return Stream.of(Arguments.of(lt, null, IllegalArgumentException.class));
    }

    /**
     * BigDecimal input – unsupported, IllegalArgumentException expected.
     */
    static Stream<Arguments> bigDecimalUnsupportedProvider() {
        BigDecimal bd = new BigDecimal("12345");
        return Stream.of(Arguments.of(bd, null, IllegalArgumentException.class));
    }

    /**
     * Invalid ISO string – DateTimeParseException expected.
     */
    static Stream<Arguments> malformedStringProvider() {
        String bad = "2023-13-40T25:61:61+99:99";
        return Stream.of(Arguments.of(bad, null, DateTimeParseException.class));
    }

    /**
     * String input with null converter – NullPointerException expected.
     */
    static Stream<Arguments> stringNullConverterProvider() {
        String iso = "2023-01-02T14:20:00+05:30";
        return Stream.of(Arguments.of(iso, null, NullPointerException.class));
    }

    /**
     * Unsupported Integer type – IllegalArgumentException expected.
     */
    static Stream<Arguments> integerUnsupportedProvider() {
        Integer i = 12345;
        return Stream.of(Arguments.of(i, null, IllegalArgumentException.class));
    }

    /**
     * Unsupported Duration type – IllegalArgumentException expected.
     */
    static Stream<Arguments> durationUnsupportedProvider() {
        Duration d = Duration.ofHours(5);
        return Stream.of(Arguments.of(d, null, IllegalArgumentException.class));
    }

    /**
     * Executes a successful conversion and asserts that the resulting map matches the expected values.
     */
    private void executeSuccessCase(Object from, Converter conv, Map<String, String> expected) {
        // Use the shared converter when the test does not explicitly provide one.
        Converter effectiveConverter = (conv != null) ? conv : converter;
        Map<String, Object> result = OffsetDateTimeConversions.toMap(from, effectiveConverter);
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(3, result.size(), "Map must contain exactly three entries");
        // Verify each expected entry.
        expected.forEach((key, value) -> assertEquals(value, result.get(key), "Unexpected value for key '" + key + "'"));
    }

    /**
     * Executes a conversion that is expected to throw a specific exception.
     */
    private void executeExceptionCase(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        Converter effectiveConverter = (conv != null) ? conv : converter;
        Throwable thrown = assertThrows(expectedException, () -> OffsetDateTimeConversions.toMap(from, effectiveConverter));
        // Optionally, we could assert the message contains something useful, but the contract only requires the type.
        assertNotNull(thrown.getMessage());
    }

    @Test
    void testToMap_WithZonedDateTime_ReturnsCorrectMap() {
        // GIVEN a known ZonedDateTime (UTC) and a real Converter
        ZonedDateTime zdt = ZonedDateTime.of(// year, month, day
        2023, // year, month, day
        5, // year, month, day
        17, // hour, minute, second, nanosecond
        14, // hour, minute, second, nanosecond
        30, // hour, minute, second, nanosecond
        45, // hour, minute, second, nanosecond
        123_000_000, ZoneOffset.UTC);
        // WHEN the focal method is invoked
        Map<String, Object> result = OffsetDateTimeConversions.toMap(zdt, converter);
        // THEN the map contains the expected keys and stringified values
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(3, result.size(), "Map should contain exactly three entries");
        assertEquals(zdt.toLocalDate().toString(), result.get(MapConversions.DATE), "DATE entry should match the local date string");
        assertEquals(zdt.toLocalTime().toString(), result.get(MapConversions.TIME), "TIME entry should match the local time string");
        assertEquals(zdt.getOffset().toString(), result.get(MapConversions.OFFSET), "OFFSET entry should match the zone offset string");
    }

    @Test
    void testToMap_WithOffsetDateTime_ConvertedByConverter() {
        // GIVEN an OffsetDateTime; the Converter can turn it into a ZonedDateTime
        OffsetDateTime odt = OffsetDateTime.of(2021, 12, 31, 23, 59, 59, 999_000_000, // e.g., UTC‑05:30
        ZoneOffset.ofHoursMinutes(-5, -30));
        // WHEN the focal method is invoked (the Converter will handle the conversion)
        Map<String, Object> result = OffsetDateTimeConversions.toMap(odt, converter);
        // THEN the map reflects the converted ZonedDateTime values
        ZonedDateTime expectedZdt = odt.toZonedDateTime();
        assertEquals(expectedZdt.toLocalDate().toString(), result.get(MapConversions.DATE));
        assertEquals(expectedZdt.toLocalTime().toString(), result.get(MapConversions.TIME));
        assertEquals(expectedZdt.getOffset().toString(), result.get(MapConversions.OFFSET));
    }

    @ParameterizedTest(name = "[{index}] OffsetDateTime input")
    @MethodSource("offsetDateTimeProvider")
    void testToMap_OffsetDateTime(Object from, Converter conv, Map<String, String> expected) {
        executeSuccessCase(from, conv, expected);
    }

    @ParameterizedTest(name = "[{index}] ZonedDateTime input")
    @MethodSource("zonedDateTimeProvider")
    void testToMap_ZonedDateTime(Object from, Converter conv, Map<String, String> expected) {
        executeSuccessCase(from, conv, expected);
    }

    @ParameterizedTest(name = "[{index}] LocalDateTime input")
    @MethodSource("localDateTimeProvider")
    void testToMap_LocalDateTime(Object from, Converter conv, Map<String, String> expected) {
        executeSuccessCase(from, conv, expected);
    }

    @ParameterizedTest(name = "[{index}] java.util.Date input")
    @MethodSource("utilDateProvider")
    void testToMap_UtilDate(Object from, Converter conv, Map<String, String> expected) {
        executeSuccessCase(from, conv, expected);
    }

    @ParameterizedTest(name = "[{index}] Instant input")
    @MethodSource("instantProvider")
    void testToMap_Instant(Object from, Converter conv, Map<String, String> expected) {
        executeSuccessCase(from, conv, expected);
    }

    @ParameterizedTest(name = "[{index}] OffsetTime (unsupported)")
    @MethodSource("offsetTimeUnsupportedProvider")
    void testToMap_OffsetTimeUnsupported(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        executeExceptionCase(from, conv, expectedException);
    }

    @ParameterizedTest(name = "[{index}] Null input")
    @MethodSource("nullInputProvider")
    void testToMap_NullInput(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        executeExceptionCase(from, conv, expectedException);
    }

    @ParameterizedTest(name = "[{index}] java.sql.Timestamp input")
    @MethodSource("timestampProvider")
    void testToMap_Timestamp(Object from, Converter conv, Map<String, String> expected) {
        executeSuccessCase(from, conv, expected);
    }

    @ParameterizedTest(name = "[{index}] java.sql.Date input")
    @MethodSource("sqlDateProvider")
    void testToMap_SqlDate(Object from, Converter conv, Map<String, String> expected) {
        executeSuccessCase(from, conv, expected);
    }

    @ParameterizedTest(name = "[{index}] Calendar (UTC) input")
    @MethodSource("calendarUtcProvider")
    void testToMap_CalendarUtc(Object from, Converter conv, Map<String, String> expected) {
        executeSuccessCase(from, conv, expected);
    }

    @ParameterizedTest(name = "[{index}] AtomicLong input")
    @MethodSource("atomicLongProvider")
    void testToMap_AtomicLong(Object from, Converter conv, Map<String, String> expected) {
        executeSuccessCase(from, conv, expected);
    }

    @ParameterizedTest(name = "[{index}] ISO string input")
    @MethodSource("isoStringProvider")
    void testToMap_IsoString(Object from, Converter conv, Map<String, String> expected) {
        executeSuccessCase(from, conv, expected);
    }

    @ParameterizedTest(name = "[{index}] LocalDate input")
    @MethodSource("localDateProvider")
    void testToMap_LocalDate(Object from, Converter conv, Map<String, String> expected) {
        executeSuccessCase(from, conv, expected);
    }

    @ParameterizedTest(name = "[{index}] LocalTime (unsupported)")
    @MethodSource("localTimeUnsupportedProvider")
    void testToMap_LocalTimeUnsupported(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        executeExceptionCase(from, conv, expectedException);
    }

    @ParameterizedTest(name = "[{index}] BigDecimal (unsupported)")
    @MethodSource("bigDecimalUnsupportedProvider")
    void testToMap_BigDecimalUnsupported(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        executeExceptionCase(from, conv, expectedException);
    }

    @ParameterizedTest(name = "[{index}] Malformed string")
    @MethodSource("malformedStringProvider")
    void testToMap_MalformedString(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        executeExceptionCase(from, conv, expectedException);
    }

    @ParameterizedTest(name = "[{index}] String with null converter")
    @MethodSource("stringNullConverterProvider")
    void testToMap_StringNullConverter(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        executeExceptionCase(from, conv, expectedException);
    }

    @ParameterizedTest(name = "[{index}] Integer (unsupported)")
    @MethodSource("integerUnsupportedProvider")
    void testToMap_IntegerUnsupported(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        executeExceptionCase(from, conv, expectedException);
    }

    @ParameterizedTest(name = "[{index}] Duration (unsupported)")
    @MethodSource("durationUnsupportedProvider")
    void testToMap_DurationUnsupported(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        executeExceptionCase(from, conv, expectedException);
    }
}
