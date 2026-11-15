package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.MonthDay;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class MonthDayConversions_toMap_Test {

    /**
     * The Converter instance used for the tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not need any special configuration for this test.
        // An empty options object is sufficient.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validMonthDayProvider() {
        return Stream.of(// Group: Valid MonthDay conversion
        arguments(MonthDay.of(5, 15), null, 15, 5, "When a proper MonthDay instance is supplied and converter is null, the map contains day=15, month=5."), arguments(MonthDay.of(12, 31), new Converter(new ConverterOptions()), 31, 12, "Even with a non‑null Converter, the map must contain day=31, month=12."), // Group: Valid MonthDay edge values
        arguments(MonthDay.of(1, 1), null, 1, 1, "Minimum valid month/day (January 1st)."), arguments(MonthDay.of(6, 30), new Converter(new ConverterOptions()), 30, 6, "Mid‑year valid MonthDay (June 30) with a non‑null Converter."), // Additional valid cases for completeness
        arguments(MonthDay.of(2, 29), null, 29, 2, "Leap day (February 29) with null converter."), arguments(MonthDay.of(11, 30), new Converter(new ConverterOptions()), 30, 11, "Another typical MonthDay (November 30) with non‑null converter."));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidSourceProvider() {
        return Stream.of(// Group: Invalid source type – String
        arguments("2023-05-15", null, "Supplying a String instead of MonthDay should throw ClassCastException."), // Group: Invalid source type – LocalDate
        arguments(LocalDate.of(2023, 5, 15), null, "Supplying a LocalDate instead of MonthDay should throw ClassCastException."));
    }

    @Test
    void toMap_returnsMapWithCorrectDayAndMonth() {
        // GIVEN
        // Christmas: month=12, day=25
        MonthDay monthDay = MonthDay.of(12, 25);
        // WHEN
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) MonthDayConversions.toMap(monthDay, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(2, result.size(), "Map should contain exactly two entries");
        assertEquals(25, result.get("day"), "Day value should match the MonthDay's day");
        assertEquals(12, result.get("month"), "Month value should match the MonthDay's month");
    }

    @Test
    void toMap_handlesDifferentMonthDayValues() {
        // GIVEN
        // Leap day
        MonthDay monthDay = MonthDay.of(2, 29);
        // WHEN
        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) MonthDayConversions.toMap(monthDay, converter);
        // THEN
        assertEquals(29, result.get("day"));
        assertEquals(2, result.get("month"));
    }

    @Test
    void toMap_throwsClassCastExceptionWhenSourceIsNotMonthDay() {
        // GIVEN
        Object notMonthDay = "2023-01-01";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MonthDayConversions.toMap(notMonthDay, converter));
    }

    /* -------------------------------------------------------------
     *  Null source object
     * ------------------------------------------------------------- */
    @Test
    void toMap_throwsNullPointerExceptionWhenSourceIsNull() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MonthDayConversions.toMap(nullSource, null), "Casting a null reference to MonthDay should result in a NullPointerException when accessed.");
    }

    /* -------------------------------------------------------------
     *  Invalid source type (ClassCastException)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("invalidSourceProvider")
    void toMap_throwsClassCastExceptionWhenSourceIsInvalid(Object from, Converter conv, String description) {
        // GIVEN – parameters supplied by the method source
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MonthDayConversions.toMap(from, conv), description);
    }
}
