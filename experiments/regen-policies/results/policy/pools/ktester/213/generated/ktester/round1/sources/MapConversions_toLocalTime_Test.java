package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.junit.jupiter.params.provider.Arguments;
import java.sql.Timestamp;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toLocalTime_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure deterministic zone handling for epoch‑millis based conversions.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        // Initialise the converter – the library provides a no‑arg constructor.
//         converter = new Converter();
    }

    @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    /* -------------------- Direct LocalTime instances -------------------- */
    private static Stream<Arguments> directLocalTimeProvider() {
        return Stream.of(Arguments.of(LocalTime.of(13, 45, 30), LocalTime.of(13, 45, 30)), Arguments.of(LocalTime.of(0, 0), LocalTime.of(0, 0)));
    }

    /* ----------------------- String representations ---------------------- */
    private static Stream<Arguments> stringRepresentationProvider() {
        return Stream.of(Arguments.of("13:45:30", LocalTime.of(13, 45, 30)), Arguments.of("13:45", LocalTime.of(13, 45)), Arguments.of("13:45:30.123", LocalTime.of(13, 45, 30, 123_000_000)), Arguments.of("invalid-time", DateTimeParseException.class));
    }

    /* ----------------------- Java 8 date‑time types ---------------------- */
    private static Stream<Arguments> java8DateTimeProvider() {
        return Stream.of(Arguments.of(LocalDateTime.of(2021, 5, 20, 13, 45, 30), LocalTime.of(13, 45, 30)), Arguments.of(OffsetTime.of(13, 45, 30, 0, ZoneOffset.ofHours(2)), LocalTime.of(13, 45, 30)), Arguments.of(ZonedDateTime.of(2021, 5, 20, 13, 45, 30, 0, ZoneId.of("Europe/Paris")), LocalTime.of(13, 45, 30)));
    }

    /* ----------------------- Legacy java.util types ---------------------- */
    private static Stream<Arguments> legacyUtilProvider() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.setTimeInMillis(1621517130000L);
        return Stream.of(Arguments.of(new Date(1621517130000L), LocalTime.of(13, 45, 30)), Arguments.of(new Timestamp(1621517130000L), LocalTime.of(13, 45, 30)), Arguments.of(calendar, LocalTime.of(13, 45, 30)));
    }

    /* -------------------------- Numeric epoch millis -------------------- */
    private static Stream<Arguments> numericEpochProvider() {
        return Stream.of(// interpreted as millis
        Arguments.of(1621517130000L, LocalTime.of(13, 45, 30)), // interpreted as seconds
        Arguments.of(1621517130, LocalTime.of(13, 45, 30)));
    }

    /* -------------------------- Map based conversion -------------------- */
    private static Stream<Arguments> mapConversionProvider() {
        Map<String, Object> map1 = Map.of("localTime", "13:45:30");
        Map<String, Object> map2 = Map.of("value", LocalTime.of(9, 15));
        Map<String, Object> map3 = Map.of("_v", new Date(1621517130000L));
        Map<String, Object> map4 = Map.of("unknown", "13:45:30");
        return Stream.of(Arguments.of(map1, LocalTime.of(13, 45, 30)), Arguments.of(map2, LocalTime.of(9, 15)), Arguments.of(map3, LocalTime.of(13, 45, 30)), Arguments.of(map4, DateTimeParseException.class));
    }

    /**
     * Verify that a map containing the key {@code "localTime"} with a string value
     * is correctly converted to {@link LocalTime}.
     */
    @Test
    void shouldConvertMapWithLocalTimeKeyFromString() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.LOCAL_TIME, "13:45:30");
        LocalTime result = MapConversions.toLocalTime(map, converter);
        assertNotNull(result);
        assertEquals(LocalTime.of(13, 45, 30), result);
    }

    /**
     * Verify that a map containing the generic {@code "value"} key with a {@link LocalTime}
     * instance returns the same instance (no conversion needed).
     */
    @Test
    void shouldReturnLocalTimeWhenValueKeyHoldsLocalTimeInstance() {
        LocalTime expected = LocalTime.of(9, 15, 0);
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.VALUE, expected);
        LocalTime result = MapConversions.toLocalTime(map, converter);
        assertSame(expected, result);
    }

    /**
     * Verify that a map containing the short key {@code "_v"} with a numeric value
     * is interpreted as seconds‑of‑day and converted to {@link LocalTime}.
     */
    @Test
    void shouldConvertMapWithShortKeyFromIntegerSeconds() {
        Map<String, Object> map = new HashMap<>();
        // 12:34:56 (12*3600 + 34*60 + 56)
        map.put(MapConversions.V, 45296);
        LocalTime result = MapConversions.toLocalTime(map, converter);
        assertNotNull(result);
        assertEquals(LocalTime.of(12, 34, 56), result);
    }

    /**
     * When the source object cannot be interpreted as a {@link LocalTime},
     * the method should return {@code null} rather than throwing.
     */
    @Test
    void shouldReturnNullForUnconvertibleSource() {
        Map<String, Object> map = new HashMap<>();
        map.put("someOtherKey", "not a time");
        LocalTime result = MapConversions.toLocalTime(map, converter);
        assertNull(result);
    }

    /**
     * Directly passing a {@link LocalTime} (not wrapped in a map) should also work
     * because {@code dispatch} falls back to the converter which knows how to handle it.
     */
    @Test
    void shouldReturnSameLocalTimeWhenSourceIsAlreadyLocalTime() {
        LocalTime source = LocalTime.now();
        LocalTime result = MapConversions.toLocalTime(source, converter);
        assertSame(source, result);
    }

    @ParameterizedTest(name = "[{index}] Direct LocalTime instance {0}")
    @MethodSource("directLocalTimeProvider")
    void shouldReturnSameLocalTimeWhenSourceIsAlreadyLocalTimeParameterized(LocalTime source, LocalTime expected) {
        LocalTime result = MapConversions.toLocalTime(source, converter);
        assertSame(source, result);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] String ''{0}''")
    @MethodSource("stringRepresentationProvider")
    void shouldParseStringRepresentations(Object input, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapConversions.toLocalTime(input, converter));
        } else {
            LocalTime result = MapConversions.toLocalTime(input, converter);
            assertEquals(expected, result);
        }
    }

    @ParameterizedTest(name = "[{index}] Java8 type {0}")
    @MethodSource("java8DateTimeProvider")
    void shouldConvertJava8DateTimeTypes(Object source, LocalTime expected) {
        LocalTime result = MapConversions.toLocalTime(source, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Legacy util type {0}")
    @MethodSource("legacyUtilProvider")
    void shouldConvertLegacyUtilTypes(Object source, LocalTime expected) {
        LocalTime result = MapConversions.toLocalTime(source, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Numeric epoch {0}")
    @MethodSource("numericEpochProvider")
    void shouldConvertNumericEpochValues(Number source, LocalTime expected) {
        LocalTime result = MapConversions.toLocalTime(source, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] Map conversion")
    @MethodSource("mapConversionProvider")
    void shouldConvertMapBasedScenarios(Map<String, Object> source, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<? extends Throwable>) expected, () -> MapConversions.toLocalTime(source, converter));
        } else {
            LocalTime result = MapConversions.toLocalTime(source, converter);
            assertEquals(expected, result);
        }
    }

    /* -------------------------- Null & unsupported ---------------------- */
    @Test
    void shouldReturnNullWhenSourceIsNull() {
        assertNull(MapConversions.toLocalTime(null, converter));
    }

    @Test
    void shouldThrowOnUnsupportedBooleanSource() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalTime(Boolean.TRUE, converter));
    }

    /* -------------------------- LocalDate failure ---------------------- */
    @Test
    void shouldThrowWhenSourceIsLocalDate() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalTime(LocalDate.of(2021, 5, 20), converter));
    }

    /* -------------------------- Instant conversion ---------------------- */
    @Test
    void shouldConvertInstantToLocalTime() {
        Instant instant = Instant.ofEpochMilli(1621517130000L);
        LocalTime result = MapConversions.toLocalTime(instant, converter);
        assertEquals(LocalTime.of(13, 45, 30), result);
    }

    /* -------------------------- OffsetDateTime conversion --------------- */
    @Test
    void shouldConvertOffsetDateTimeToLocalTime() {
        OffsetDateTime odt = OffsetDateTime.of(2021, 5, 20, 13, 45, 30, 0, ZoneOffset.ofHours(-5));
        LocalTime result = MapConversions.toLocalTime(odt, converter);
        assertEquals(LocalTime.of(13, 45, 30), result);
    }

    /* -------------------------- Map key precedence ---------------------- */
    @Test
    void shouldRespectKeyPrecedenceInMap() {
        Map<String, Object> map = Map.of("localTime", "09:15:00", "value", "10:30:00", "_v", "11:45:00");
        LocalTime result = MapConversions.toLocalTime(map, converter);
        assertEquals(LocalTime.of(9, 15), result);
    }

    /* -------------------------- Map with null value ---------------------- */
    @Test
    void shouldThrowWhenMapContainsNullForRecognisedKey() {
        Map<String, Object> map = Map.of("localTime", null);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalTime(map, converter));
    }

    /* -------------------------- Map with unsupported value type ---------- */
    @Test
    void shouldThrowWhenMapValueCannotBeConverted() {
        Map<String, Object> map = Map.of("value", Boolean.TRUE);
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalTime(map, converter));
    }

    /* -------------------------- StringBuilder input ---------------------- */
    @Test
    void shouldParseStringBuilderContainingTime() {
        StringBuilder sb = new StringBuilder("13:45:30");
        LocalTime result = MapConversions.toLocalTime(sb, converter);
        assertEquals(LocalTime.of(13, 45, 30), result);
    }

    /* -------------------------- Whitespace tolerant parsing ------------- */
    @Test
    void shouldParseStringWithSurroundingWhitespace() {
        String input = " 13:45:30 ";
        LocalTime result = MapConversions.toLocalTime(input, converter);
        assertEquals(LocalTime.of(13, 45, 30), result);
    }

    /* -------------------------- Empty map conversion --------------------- */
    @Test
    void shouldThrowWhenMapIsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toLocalTime(Collections.emptyMap(), converter));
    }

    /* -------------------------- BigInteger epoch millis ----------------- */
    @Test
    void shouldConvertBigIntegerEpochMillis() {
        BigInteger big = new BigInteger("1621517130000");
        LocalTime result = MapConversions.toLocalTime(big, converter);
        assertEquals(LocalTime.of(13, 45, 30), result);
    }
}