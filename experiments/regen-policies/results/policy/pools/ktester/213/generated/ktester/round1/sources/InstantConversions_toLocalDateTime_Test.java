package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.stream.Stream;

class InstantConversions_toLocalDateTime_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // use default options – no special overrides required for these tests
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // release the static reference
        converter = null;
    }

    private static Stream<Arguments> instantProvider() {
        return Stream.of(Arguments.of("Convert epoch Instant to LocalDateTime (UTC)", Instant.ofEpochSecond(0), LocalDateTime.of(1970, 1, 1, 0, 0)), Arguments.of("Convert non‑zero Instant with nanosecond precision", Instant.parse("2023-03-25T15:30:45.123Z"), LocalDateTime.of(2023, 3, 25, 15, 30, 45, 123_000_000)));
    }

    private static Stream<Arguments> dateProvider() {
        return Stream.of(Arguments.of("Date epoch (0 ms) → LocalDateTime", new Date(0L), LocalDateTime.of(1970, 1, 1, 0, 0)), Arguments.of("Date with millisecond precision", // 2023‑03‑25T15:30:45.123Z
        new Date(1679752245123L), LocalDateTime.of(2023, 3, 25, 15, 30, 45, 123_000_000)));
    }

    private static Stream<Arguments> timestampProvider() {
        return Stream.of(Arguments.of("Timestamp at epoch", Timestamp.from(Instant.ofEpochSecond(0)), LocalDateTime.of(1970, 1, 1, 0, 0)), Arguments.of("Timestamp with nanosecond precision", Timestamp.valueOf("2023-03-25 15:30:45.123"), LocalDateTime.of(2023, 3, 25, 15, 30, 45, 123_000_000)));
    }

    private static Stream<Arguments> longProvider() {
        return Stream.of(Arguments.of("Long epoch millis 0 → LocalDateTime", 0L, LocalDateTime.of(1970, 1, 1, 0, 0)), Arguments.of("Long epoch millis with fractional seconds", 1679752245123L, LocalDateTime.of(2023, 3, 25, 15, 30, 45, 123_000_000)));
    }

    private static Stream<Arguments> isoStringProvider() {
        return Stream.of(Arguments.of("ISO‑8601 UTC string without fractional seconds", "2023-03-25T15:30:45Z", LocalDateTime.of(2023, 3, 25, 15, 30, 45)), Arguments.of("ISO‑8601 string with +02:00 offset – converted to UTC", "2023-03-25T15:30:45.123+02:00", // 15:30:45.123+02:00 == 13:30:45.123Z
        LocalDateTime.of(2023, 3, 25, 13, 30, 45, 123_000_000)));
    }

    private static Stream<Arguments> zonedDateTimeProvider() {
        return Stream.of(Arguments.of("ZonedDateTime with Europe/Paris zone (UTC+01:00 in March)", ZonedDateTime.of(2023, 3, 25, 15, 30, 45, 0, ZoneId.of("Europe/Paris")), // The method discards the zone – it simply returns the local fields.
        LocalDateTime.of(2023, 3, 25, 15, 30, 45)));
    }

    private static Stream<Arguments> offsetDateTimeProvider() {
        return Stream.of(Arguments.of("OffsetDateTime with +01:00 offset – keep the local fields", OffsetDateTime.parse("2023-03-25T15:30:45.123+01:00"), LocalDateTime.of(2023, 3, 25, 15, 30, 45, 123_000_000)));
    }

    private static Stream<Arguments> mapWithInstantProvider() {
        Instant instant = Instant.parse("2023-03-25T15:30:45.123Z");
        Map<String, Object> map = Collections.singletonMap(MapConversions.INSTANT, instant);
        return Stream.of(Arguments.of("Map containing the special INSTANT key", map, LocalDateTime.of(2023, 3, 25, 15, 30, 45, 123_000_000)));
    }

    private static Stream<Arguments> unsupportedProvider() {
        return Stream.of(Arguments.of("Plain Object is not supported", new Object()), Arguments.of("BigDecimal is not supported", new BigDecimal("1234567890")), Arguments.of("LocalDate (date‑only) is not supported", LocalDate.of(2023, 3, 25)), Arguments.of("LocalTime (time‑only) is not supported", LocalTime.of(15, 30, 45)), Arguments.of("java.sql.Date (date‑only) is not supported", java.sql.Date.valueOf("2023-03-25")));
    }

    private static Stream<Arguments> invalidStringProvider() {
        return Stream.of(Arguments.of("Malformed date‑time string", "2023-13-40T99:99:99Z"), Arguments.of("Completely random non‑date string", "just-a-random-string"));
    }

    private static Stream<Arguments> invalidMapProvider() {
        Map<String, Object> missingKey = Collections.singletonMap("notInstantKey", Instant.now());
        Map<String, Object> wrongValue = Collections.singletonMap(MapConversions.INSTANT, "2023-03-25T15:30:45Z");
        return Stream.of(Arguments.of("Map does not contain the special INSTANT key", missingKey), Arguments.of("Map contains INSTANT key but value is a String", wrongValue));
    }

    @Test
    void toLocalDateTimeFromInstant() {
        Instant now = Instant.now();
        LocalDateTime expected = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        LocalDateTime actual = InstantConversions.toLocalDateTime(now, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDateTimeFromDate() {
        Date date = new Date();
        Instant instant = date.toInstant();
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime actual = InstantConversions.toLocalDateTime(date, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDateTimeFromTimestamp() {
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        Instant instant = ts.toInstant();
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime actual = InstantConversions.toLocalDateTime(ts, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDateTimeFromCalendar() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(System.currentTimeMillis());
        Instant instant = cal.toInstant();
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime actual = InstantConversions.toLocalDateTime(cal, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDateTimeFromLongEpochMillis() {
        long epochMillis = System.currentTimeMillis();
        Instant instant = Instant.ofEpochMilli(epochMillis);
        LocalDateTime expected = LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
        LocalDateTime actual = InstantConversions.toLocalDateTime(epochMillis, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDateTimeFromMapContainingInstant() {
        Instant now = Instant.now();
        Map<String, Object> map = new LinkedHashMap<>();
        // MapConversions.INSTANT is a constant key used by the library – its value is "instant"
        map.put("instant", now);
        LocalDateTime expected = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        LocalDateTime actual = InstantConversions.toLocalDateTime(map, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toLocalDateTimeFromNullReturnsNull() {
        assertNull(InstantConversions.toLocalDateTime(null, converter));
    }

    @Test
    void toLocalDateTimeFromUnsupportedTypeThrowsException() {
        String unsupported = "2023-01-01T12:00:00";
        assertThrows(RuntimeException.class, () -> InstantConversions.toLocalDateTime(unsupported, converter));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("instantProvider")
    void instant_toLocalDateTime(String description, Instant input, LocalDateTime expected) {
        LocalDateTime actual = InstantConversions.toLocalDateTime(input, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dateProvider")
    void date_toLocalDateTime(String description, Date input, LocalDateTime expected) {
        LocalDateTime actual = InstantConversions.toLocalDateTime(input, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("timestampProvider")
    void timestamp_toLocalDateTime(String description, Timestamp input, LocalDateTime expected) {
        LocalDateTime actual = InstantConversions.toLocalDateTime(input, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("longProvider")
    void longEpochMillis_toLocalDateTime(String description, Long input, LocalDateTime expected) {
        LocalDateTime actual = InstantConversions.toLocalDateTime(input, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("isoStringProvider")
    void stringISO8601_toLocalDateTime(String description, String input, LocalDateTime expected) {
        LocalDateTime actual = InstantConversions.toLocalDateTime(input, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zonedDateTimeProvider")
    void zonedDateTime_toLocalDateTime(String description, ZonedDateTime input, LocalDateTime expected) {
        LocalDateTime actual = InstantConversions.toLocalDateTime(input, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("offsetDateTimeProvider")
    void offsetDateTime_toLocalDateTime(String description, OffsetDateTime input, LocalDateTime expected) {
        LocalDateTime actual = InstantConversions.toLocalDateTime(input, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapWithInstantProvider")
    void mapWithInstantKey_toLocalDateTime(String description, Map<String, Object> input, LocalDateTime expected) {
        LocalDateTime actual = InstantConversions.toLocalDateTime(input, converter);
        assertEquals(expected, actual, description);
    }

    // -----------------------------------------------------------------
    @Test
    void nullInput_toLocalDateTime() {
        assertNull(InstantConversions.toLocalDateTime(null, converter), "When the source object is null the method should return null.");
    }

    // -----------------------------------------------------------------
    @Test
    void nullConverter_toLocalDateTime() {
        Instant instant = Instant.ofEpochSecond(0);
        assertThrows(NullPointerException.class, () -> InstantConversions.toLocalDateTime(instant, null), "Passing a null Converter must result in NullPointerException.");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedProvider")
    void unsupportedType_toLocalDateTime(String description, Object input) {
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toLocalDateTime(input, converter), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidStringProvider")
    void stringInvalid_toLocalDateTime(String description, String input) {
        assertThrows(DateTimeParseException.class, () -> InstantConversions.toLocalDateTime(input, converter), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidMapProvider")
    void mapInvalid_toLocalDateTime(String description, Map<String, Object> input) {
        assertThrows(IllegalArgumentException.class, () -> InstantConversions.toLocalDateTime(input, converter), description);
    }
}
