package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toOffsetDateTime_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter instance.  The library supplies a no‑arg
        // constructor; if a different constructor is required, adjust accordingly.
//         converter = new Converter();
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// already an OffsetDateTime
        Arguments.of("When the source object is already an OffsetDateTime, return the same instance", OffsetDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneOffset.ofHours(2)), OffsetDateTime.of(2023, 5, 10, 14, 30, 0, 0, ZoneOffset.ofHours(2))), // map with "offsetDateTime" key holding an OffsetDateTime
        Arguments.of("Map contains the key \"offsetDateTime\" with an OffsetDateTime value", mapOf("offsetDateTime", OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)), // map with generic "value" key holding an OffsetDateTime
        Arguments.of("Map contains the generic key \"value\" with an OffsetDateTime value", mapOf("value", OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5))), OffsetDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-5))), // map with short "v" key holding an OffsetDateTime
        Arguments.of("Map contains the short key \"v\" with an OffsetDateTime value", mapOf("v", OffsetDateTime.of(2020, 6, 15, 12, 0, 0, 0, ZoneOffset.ofHours(3))), OffsetDateTime.of(2020, 6, 15, 12, 0, 0, 0, ZoneOffset.ofHours(3))), // map with epochMillis as long
        Arguments.of("Map contains the key \"epochMillis\" with a long epoch value", mapOf("epochMillis", 1672531199000L), OffsetDateTime.ofInstant(Instant.ofEpochMilli(1672531199000L), ZoneOffset.UTC)), // raw long epoch millis
        Arguments.of("Raw long epoch milliseconds should be treated as \"epochMillis\"", 1672531199000L, OffsetDateTime.ofInstant(Instant.ofEpochMilli(1672531199000L), ZoneOffset.UTC)), // ISO‑8601 string
        Arguments.of("ISO‑8601 string should be parsed to OffsetDateTime", "2023-08-01T10:15:30+01:00", OffsetDateTime.parse("2023-08-01T10:15:30+01:00")), // map precedence: value vs epochMillis
        Arguments.of("When both \"value\" and \"epochMillis\" are present, \"value\" wins", mapOf("value", OffsetDateTime.of(2022, 3, 15, 9, 45, 0, 0, ZoneOffset.ofHours(1)), "epochMillis", 1672531199000L), OffsetDateTime.of(2022, 3, 15, 9, 45, 0, 0, ZoneOffset.ofHours(1))), // map precedence: short key "v" (string) vs epochMillis
        Arguments.of("Short key \"v\" with ISO string wins over \"epochMillis\"", mapOf("v", "2023-04-01T12:00:00+02:00", "epochMillis", 1680345600000L), OffsetDateTime.parse("2023-04-01T12:00:00+02:00")), // map with offsetDateTime key holding a string
        Arguments.of("String value under \"offsetDateTime\" key should be parsed", mapOf("offsetDateTime", "2021-12-31T23:59:59+00:00"), OffsetDateTime.parse("2021-12-31T23:59:59+00:00")), // map with epochMillis as numeric string
        Arguments.of("String numeric epochMillis should be coerced to long", mapOf("epochMillis", "1672531199000"), OffsetDateTime.ofInstant(Instant.ofEpochMilli(1672531199000L), ZoneOffset.UTC)), // Instant source
        Arguments.of("Instant should be converted using UTC offset", Instant.parse("2022-10-05T14:30:00Z"), OffsetDateTime.ofInstant(Instant.parse("2022-10-05T14:30:00Z"), ZoneOffset.UTC)), // java.util.Date source
        Arguments.of("java.util.Date should be converted via its epoch millis (UTC)", new Date(1672531199000L), OffsetDateTime.ofInstant(Instant.ofEpochMilli(1672531199000L), ZoneOffset.UTC)), // java.sql.Timestamp source
        Arguments.of("Timestamp should be treated like Date (UTC)", Timestamp.from(Instant.parse("2023-01-01T00:00:00Z")), OffsetDateTime.ofInstant(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC)), // ZonedDateTime source
        Arguments.of("ZonedDateTime should preserve its original offset", ZonedDateTime.of(2022, 7, 20, 15, 45, 0, 0, ZoneId.of("Europe/Paris")), ZonedDateTime.of(2022, 7, 20, 15, 45, 0, 0, ZoneId.of("Europe/Paris")).toOffsetDateTime()), // LocalDateTime source (UTC assumed)
        Arguments.of("LocalDateTime should be attached to UTC offset", LocalDateTime.of(2022, 12, 31, 23, 59, 59), LocalDateTime.of(2022, 12, 31, 23, 59, 59).atOffset(ZoneOffset.UTC)), // Negative epoch millis
        Arguments.of("Negative epoch millis (pre‑1970) should be handled correctly", -86400000L, OffsetDateTime.ofInstant(Instant.ofEpochMilli(-86400000L), ZoneOffset.UTC)));
    }

    private static Stream<Arguments> nullAndEmptyProvider() {
        return Stream.of(Arguments.of("Null source should return null", null), Arguments.of("Empty map should return null", Collections.emptyMap()));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// unsupported source type
        Arguments.of("Unsupported source type (Boolean) should throw IllegalArgumentException", Boolean.TRUE, IllegalArgumentException.class), // invalid ISO‑8601 string (bad month)
        Arguments.of("Invalid month in ISO string should throw DateTimeParseException", "2023-13-01T10:15:30+01:00", java.time.format.DateTimeParseException.class), // empty string
        Arguments.of("Empty string should throw DateTimeParseException", "", java.time.format.DateTimeParseException.class), // non‑numeric epochMillis string
        Arguments.of("Non‑numeric epochMillis string should throw NumberFormatException", mapOf("epochMillis", "notANumber"), NumberFormatException.class), // decimal epochMillis string
        Arguments.of("Decimal epochMillis string should throw NumberFormatException", mapOf("epochMillis", "1234.56"), NumberFormatException.class), // null converter argument
        Arguments.of("Null converter should cause NullPointerException", OffsetDateTime.now(), NullPointerException.class), // unsupported value type inside map (Object)
        Arguments.of("Unsupported value type inside map should throw IllegalArgumentException", mapOf("value", new Object()), IllegalArgumentException.class), // Boolean under offsetDateTime key
        Arguments.of("Boolean under \"offsetDateTime\" key should throw IllegalArgumentException", mapOf("offsetDateTime", Boolean.FALSE), IllegalArgumentException.class), // raw Double where epochMillis expected
        Arguments.of("Raw Double should be rejected with IllegalArgumentException", 12345.67D, IllegalArgumentException.class), // BigDecimal source (unsupported)
        Arguments.of("BigDecimal source should throw IllegalArgumentException", new BigDecimal("1234567890"), IllegalArgumentException.class));
    }

    /* --------------------------------------------------------------------- */
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    void shouldConvertOffsetDateTimeDirectly() {
        // GIVEN
        OffsetDateTime now = OffsetDateTime.now();
        // WHEN
        OffsetDateTime result = MapConversions.toOffsetDateTime(now, converter);
        // THEN
        assertEquals(now, result, "Direct OffsetDateTime should be returned unchanged");
    }

    @Test
    void shouldConvertFromMapWithValueKey() {
        // GIVEN
        OffsetDateTime expected = OffsetDateTime.of(2023, 5, 17, 12, 30, 45, 0, ZoneOffset.UTC);
        Map<String, Object> map = new HashMap<>();
        map.put("value", expected.toString());
        // WHEN
        OffsetDateTime result = MapConversions.toOffsetDateTime(map, converter);
        // THEN
        assertEquals(expected, result, "Map with 'value' key should be parsed to OffsetDateTime");
    }

    @Test
    void shouldConvertFromMapWithVKey() {
        // GIVEN
        OffsetDateTime expected = OffsetDateTime.of(2022, 12, 31, 23, 59, 59, 0, ZoneOffset.ofHours(-5));
        Map<String, Object> map = new HashMap<>();
        map.put("v", expected.toString());
        // WHEN
        OffsetDateTime result = MapConversions.toOffsetDateTime(map, converter);
        // THEN
        assertEquals(expected, result, "Map with 'v' key should be parsed to OffsetDateTime");
    }

    @Test
    void shouldConvertFromMapWithEpochMillisKey() {
        // GIVEN
        // 2022-01-01T00:00:00Z
        long epochMillis = 1_640_995_200_000L;
        OffsetDateTime expected = OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC);
        Map<String, Object> map = new HashMap<>();
        map.put("epochMillis", epochMillis);
        // WHEN
        OffsetDateTime result = MapConversions.toOffsetDateTime(map, converter);
        // THEN
        assertEquals(expected, result, "Map with 'epochMillis' key should be converted to OffsetDateTime");
    }

    @Test
    void shouldReturnNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        OffsetDateTime result = MapConversions.toOffsetDateTime(source, converter);
        // THEN
        assertNull(result, "Null source should result in null OffsetDateTime");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionsProvider")
    void validConversions(String description, Object from, OffsetDateTime expected) {
        OffsetDateTime result = MapConversions.toOffsetDateTime(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullAndEmptyProvider")
    void nullAndEmptyInputs(String description, Object from) {
        assertNull(MapConversions.toOffsetDateTime(from, converter), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void exceptionScenarios(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> MapConversions.toOffsetDateTime(from, converter), description);
    }
}