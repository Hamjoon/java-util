package com.cedarsoftware.util.convert;

import java.time.MonthDay;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.LocalDate;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import java.time.format.DateTimeParseException;

class MapConversions_toMonthDay_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

//     @BeforeAll
    static void setupBeforeAll() {
        // The Converter class requires a ConverterOptions instance.
        // Using the default options is sufficient for the built‑in date/time
        // conversions exercised by these tests.
//         converter = new Converter(new ConverterOptions());
    }

//     @AfterAll
    static void teardownAfterAll() {
        converter = null;
    }

    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(Arguments.of("--02-14", MonthDay.of(2, 14)), Arguments.of("03-01", MonthDay.of(3, 1)));
    }

    private static Stream<Arguments> mapWithKeyProvider() {
        return Stream.of(// monthDay key with a MonthDay instance
        Arguments.of(Map.of("monthDay", MonthDay.of(7, 4)), MonthDay.of(7, 4)), // value key with a prefixed string
        Arguments.of(Map.of("value", "--12-31"), MonthDay.of(12, 31)), // _v key with a LocalDate
        Arguments.of(Map.of("_v", LocalDate.of(2023, 8, 15)), MonthDay.of(8, 15)));
    }

//     @Test
//     void toMonthDay_givenMonthDayInstance_returnsSameInstance() {
//         MonthDay source = MonthDay.of(3, 10);
//         MonthDay result = MapConversions.toMonthDay(source, converter);
//         assertSame(source, result, "When the source is already a MonthDay it should be returned unchanged");
//     }

//     @Test
//     void toMonthDay_givenNull_returnsNull() {
//         MonthDay result = MapConversions.toMonthDay(null, converter);
//         assertNull(result, "Null input should result in null output");
//     }

//     @Test
//     void toMonthDay_givenMapWithMonthDayKey_convertsCorrectly() {
//         Map<String, Object> map = new HashMap<>();
//         // ISO‑8601 month‑day format
//         map.put("monthDay", "12-25");
//         MonthDay result = MapConversions.toMonthDay(map, converter);
//         assertEquals(MonthDay.of(12, 25), result);
//     }

//     @Test
//     void toMonthDay_givenMapWithValueKey_convertsCorrectly() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("value", "02-14");
//         MonthDay result = MapConversions.toMonthDay(map, converter);
//         assertEquals(MonthDay.of(2, 14), result);
//     }

//     @Test
//     void toMonthDay_givenMapWithVKey_convertsCorrectly() {
//         Map<String, Object> map = new HashMap<>();
//         map.put("_v", "07-04");
//         MonthDay result = MapConversions.toMonthDay(map, converter);
//         assertEquals(MonthDay.of(7, 4), result);
//     }

    @ParameterizedTest(name = "String \"{0}\" should be parsed to {1}")
    @MethodSource("stringParsingProvider")
    void toMonthDay_givenString_parsesCorrectly(String input, MonthDay expected) {
        MonthDay result = MapConversions.toMonthDay(input, converter);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "Map {0} should produce {1}")
    @MethodSource("mapWithKeyProvider")
    void toMonthDay_givenMapWithRecognisedKey_convertsCorrectly(Map<String, Object> map, MonthDay expected) {
        MonthDay result = MapConversions.toMonthDay(map, converter);
        assertEquals(expected, result);
    }

//     @Test
//     void toMonthDay_givenUnsupportedType_throwsIllegalArgumentException() {
//         assertThrows(IllegalArgumentException.class, () -> MapConversions.toMonthDay(12345, converter), "Unsupported source type should cause IllegalArgumentException");
//     }

//     @Test
//     void toMonthDay_givenMapWithoutRecognisedKeys_throwsIllegalArgumentException() {
//         Map<String, Object> map = Map.of("other", "data");
//         assertThrows(IllegalArgumentException.class, () -> MapConversions.toMonthDay(map, converter), "Map without any recognised keys should cause IllegalArgumentException");
//     }

//     @Test
//     void toMonthDay_givenInvalidString_throwsDateTimeParseException() {
//         assertThrows(DateTimeParseException.class, () -> MapConversions.toMonthDay("not-a-monthday", converter), "Invalid month‑day string should cause DateTimeParseException");
//     }

//     @Test
//     void toMonthDay_givenMapWithRecognisedKeyButNullValue_throwsIllegalArgumentException() {
//         Map<String, Object> map = Map.of("value", null);
//         assertThrows(IllegalArgumentException.class, () -> MapConversions.toMonthDay(map, converter), "Null value for a recognised key should cause IllegalArgumentException");
//     }

//     @Test
//     void toMonthDay_givenMapWithRecognisedKeyButUnsupportedType_throwsIllegalArgumentException() {
//         Map<String, Object> map = Map.of("monthDay", Boolean.TRUE);
//         assertThrows(IllegalArgumentException.class, () -> MapConversions.toMonthDay(map, converter), "Unsupported value type for a recognised key should cause IllegalArgumentException");
//     }

//     @Test
//     void toMonthDay_givenMapWithMalformedString_throwsDateTimeParseException() {
//         // invalid month and day
//         Map<String, Object> map = Map.of("_v", "13-40");
//         assertThrows(DateTimeParseException.class, () -> MapConversions.toMonthDay(map, converter), "Malformed month‑day string should cause DateTimeParseException");
//     }

//     @Test
//     void toMonthDay_givenNullConverter_throwsNullPointerException() {
//         MonthDay source = MonthDay.of(1, 1);
//         assertThrows(NullPointerException.class, () -> MapConversions.toMonthDay(source, null), "Null Converter should result in NullPointerException");
//     }
}