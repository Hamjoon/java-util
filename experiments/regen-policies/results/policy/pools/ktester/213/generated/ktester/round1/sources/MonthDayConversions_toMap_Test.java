package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.MonthDay;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class MonthDayConversions_toMap_Test {

    /**
     * The mock Converter passed to the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * The MonthDay instance used in the positive test case.
     */
    private MonthDay monthDay;

    /**
     * Simple stub implementation of {@link Converter} used for the
     *  non‑null‑converter test.
     */
    private static class DummyConverter implements Converter {
        // No behaviour needed – the method under test does not call any methods.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a deterministic MonthDay value for each test.
        // Christmas – will render as "--12-25"
        monthDay = MonthDay.of(12, 25);
        // Mockito will initialise mockConverter automatically because of @Mock.
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method under test does not interact with the Converter.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Supplies a stream of arguments for {@link #toMap_convertsMonthDayToMap}.
     */
    private static Stream<Arguments> provideMonthDayConversions() {
        return Stream.of(arguments("Standard case – January 15", MonthDay.of(1, 15), "--01-15"), arguments("Edge case – last day of the year", MonthDay.of(12, 31), "--12-31"), arguments("Leap‑day – February 29", MonthDay.of(2, 29), "--02-29"), arguments("Single‑digit month and day – March 5", MonthDay.of(3, 5), "--03-05"), arguments("First day of the year – January 1", MonthDay.of(1, 1), "--01-01"), arguments("April 30 – month with 30 days", MonthDay.of(4, 30), "--04-30"), arguments("Non‑leap‑year February 28", MonthDay.of(2, 28), "--02-28"), arguments("June 15 with non‑null Converter", MonthDay.of(6, 15), "--06-15"));
    }

    @Test
    void toMap_returnsMapContainingMonthDayString() {
        // GIVEN – monthDay and a (mocked) Converter are already prepared by @BeforeEach
        // WHEN
        Map<String, Object> result = MonthDayConversions.toMap(monthDay, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.MONTH_DAY), "Map should contain the key defined in MapConversions.MONTH_DAY");
        assertEquals(monthDay.toString(), result.get(MapConversions.MONTH_DAY), "Map value should be the MonthDay's ISO‑8601 string representation");
    }

    @Test
    void toMap_nullMonthDay_throwsNullPointerException() {
        // GIVEN
        Object nullMonthDay = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MonthDayConversions.toMap(nullMonthDay, mockConverter), "Passing a null MonthDay should result in NullPointerException");
    }

    /* --------------------------------------------------------------
     *  Positive conversion tests – parameterised over many MonthDay values.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("provideMonthDayConversions")
    void toMap_convertsMonthDayToMap(String description, MonthDay input, String expectedString) {
        // GIVEN – a MonthDay instance (input) and a (mocked) Converter (ignored).
        // WHEN
        Map<String, Object> result = MonthDayConversions.toMap(input, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map must contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.MONTH_DAY), "Map must contain the key defined in MapConversions.MONTH_DAY");
        assertEquals(expectedString, result.get(MapConversions.MONTH_DAY), "Map value must match the MonthDay's ISO‑8601 string");
    }

    /* --------------------------------------------------------------
     *  Invalid type test – expects ClassCastException.
     * -------------------------------------------------------------- */
    @Test
    void toMap_invalidSourceType_throwsClassCastException() {
        // GIVEN
        Object notAMonthDay = "not a MonthDay";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> MonthDayConversions.toMap(notAMonthDay, mockConverter), "Passing an object of the wrong type should throw ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Non‑null Converter argument – method should ignore it.
     * -------------------------------------------------------------- */
    @Test
    void toMap_nonNullConverter_isIgnored() {
        // GIVEN
        MonthDay input = MonthDay.of(6, 15);
        // a concrete, non‑null Converter
        Converter dummyConverter = new DummyConverter();
        // WHEN
        Map<String, Object> result = MonthDayConversions.toMap(input, dummyConverter);
        // THEN
        assertNotNull(result);
        assertEquals("--06-15", result.get(MapConversions.MONTH_DAY));
        // The dummy converter is not a mock, so we cannot verify interactions,
        // but the contract of the method states it should be ignored.
    }
}
