package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import java.time.Period;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.PERIOD;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class PeriodConversions_toMap_Test {

    private static Stream<Arguments> validPeriodProvider() {
        // a dummy (mocked) converter – the implementation is irrelevant because the method ignores it
        Converter dummyConverter = null;
        return Stream.of(Arguments.of("Converts a non‑null Period (1Y2M3D) to a map", Period.of(1, 2, 3), "P1Y2M3D", dummyConverter), Arguments.of("Converts Period.ZERO to a map", Period.ZERO, "P0D", dummyConverter), Arguments.of("Negative period values are preserved", Period.of(-1, -2, -3), "P-1Y-2M-3D", dummyConverter), Arguments.of("Maximum int value for years", Period.ofYears(Integer.MAX_VALUE), "P2147483647Y", dummyConverter), Arguments.of("Minimum int value for months (negative overflow)", Period.ofMonths(Integer.MIN_VALUE), "P-2147483648M", dummyConverter));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(Arguments.of("Passing a null `from` argument should throw NullPointerException", null, NullPointerException.class), Arguments.of("Passing a non‑Period object should throw ClassCastException", "not a period", ClassCastException.class));
    }

    @Test
    void toMap_returnsMapWithPeriodString(@Mock Converter converter) {
        // GIVEN
        // P1Y2M3D
        Period period = Period.of(1, 2, 3);
        // WHEN
        Map<String, Object> result = PeriodConversions.toMap(period, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(PERIOD), "Map should contain the key defined by MapConversions.PERIOD");
        assertEquals(period.toString(), result.get(PERIOD), "Map value should be the ISO‑8601 string representation of the Period");
    }

    @Test
    void toMap_nullPeriod_throwsNullPointerException(@Mock Converter converter) {
        // GIVEN
        Object nullPeriod = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> PeriodConversions.toMap(nullPeriod, converter), "Calling toMap with a null Period should throw NullPointerException");
    }

    /**
     * Valid Period conversion cases (including zero, negative and large values).
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validPeriodProvider")
    void toMap_validPeriods(String description, Period input, String expectedString, Converter converter) {
        // WHEN
        Map<String, Object> result = PeriodConversions.toMap(input, converter);
        // THEN
        assertNotNull(result, "Result map must not be null");
        assertEquals(1, result.size(), "Result map must contain exactly one entry");
        assertEquals(expectedString, result.get(PERIOD), "The map should contain the ISO‑8601 string of the supplied Period");
    }

    /**
     * Invalid input cases that should result in exceptions.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidInputProvider")
    void toMap_invalidInputs(String description, Object input, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> PeriodConversions.toMap(input, null), description);
    }

    /**
     * Test that a non‑null {@link Converter} does not affect the result.
     */
    @Test
    void toMap_withNonNullConverter_isIgnored(@Mock Converter converter) {
        // GIVEN
        // P5Y
        Period period = Period.of(5, 0, 0);
        // WHEN
        Map<String, Object> result = PeriodConversions.toMap(period, converter);
        // THEN
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("P5Y", result.get(PERIOD), "Converter parameter should be ignored; map must contain the correct period string");
    }
}
