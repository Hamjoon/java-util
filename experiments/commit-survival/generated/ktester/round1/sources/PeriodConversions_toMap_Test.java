package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Period;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class PeriodConversions_toMap_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    /**
     * Supplies a stream of arguments covering:
     * <ul>
     *   <li>Normal period (5y,3m,12d)</li>
     *   <li>All‑zero period</li>
     *   <li>Negative components</li>
     *   <li>Mixed zero/non‑zero fields</li>
     *   <li>Maximum int values</li>
     *   <li>Weeks conversion (3 weeks → 21 days)</li>
     *   <li>Converter side‑effects ignored (custom impl that would throw)</li>
     * </ul>
     */
    private static Stream<Arguments> validInputProvider() {
        // a dummy converter implementation that throws if any method is called
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object from, Class<?> toClass) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        return Stream.of(Arguments.of("Normal period (5y,3m,12d) with mock converter", Period.of(5, 3, 12), // null converter is tolerated
        null, 5, 3, 12), Arguments.of("All zero period with null converter", Period.of(0, 0, 0), null, 0, 0, 0), Arguments.of("Negative components (-2y,-5m,-10d) with mock converter", Period.of(-2, -5, -10), null, -2, -5, -10), Arguments.of("Mixed fields (0y,7m,0d) with mock converter", Period.of(0, 7, 0), null, 0, 7, 0), Arguments.of("Mixed fields (3y,0m,15d) with mock converter", Period.of(3, 0, 15), null, 3, 0, 15), Arguments.of("Maximum int values for each component", Period.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), null, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE), Arguments.of("Weeks conversion (3 weeks → 21 days)", Period.ofWeeks(3), null, 0, 0, 21), Arguments.of("Converter side‑effects ignored (throwing implementation)", Period.of(1, 2, 3), throwingConverter, 1, 2, 3));
    }

    @Test
    void toMap_returnsMapWithCorrectYearMonthDayValues() {
        // given
        // 5 years, 3 months, 12 days
        Period period = Period.of(5, 3, 12);
        // when
        Map<String, Object> result = PeriodConversions.toMap(period, mockConverter);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(3, result.size(), "Map should contain exactly three entries");
        assertEquals(5, result.get(MapConversions.YEARS), "Years value should match the Period");
        assertEquals(3, result.get(MapConversions.MONTHS), "Months value should match the Period");
        assertEquals(12, result.get(MapConversions.DAYS), "Days value should match the Period");
    }

    @Test
    void toMap_handlesZeroValuesCorrectly() {
        // given
        Period period = Period.of(0, 0, 0);
        // when
        Map<String, Object> result = PeriodConversions.toMap(period, mockConverter);
        // then
        assertNotNull(result);
        assertEquals(0, result.get(MapConversions.YEARS));
        assertEquals(0, result.get(MapConversions.MONTHS));
        assertEquals(0, result.get(MapConversions.DAYS));
    }

    @Test
    void toMap_throwsClassCastExceptionWhenInputIsNotPeriod() {
        // given
        Object notAPeriod = "I am not a Period";
        // when / then
        assertThrows(ClassCastException.class, () -> PeriodConversions.toMap(notAPeriod, mockConverter));
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for all *valid* input scenarios.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validInputProvider")
    void toMap_validInputs(String description, Period period, Converter converter, int expectedYears, int expectedMonths, int expectedDays) {
        // when
        Map<String, Object> result = PeriodConversions.toMap(period, converter);
        // then
        assertNotNull(result, "Resulting map should never be null");
        assertEquals(3, result.size(), "Map must contain exactly three entries");
        assertEquals(expectedYears, result.get(MapConversions.YEARS), "Years value should match the Period");
        assertEquals(expectedMonths, result.get(MapConversions.MONTHS), "Months value should match the Period");
        assertEquals(expectedDays, result.get(MapConversions.DAYS), "Days value should match the Period");
    }

    /* -------------------------------------------------------------
     *  Tests for invalid inputs – null or wrong type.
     * ------------------------------------------------------------- */
    @Test
    void toMap_nullFrom_throwsNullPointerException() {
        // given
        Object nullFrom = null;
        // when / then
        assertThrows(NullPointerException.class, () -> PeriodConversions.toMap(nullFrom, mockConverter), "Passing a null 'from' should result in NPE when accessing period fields");
    }

    @Test
    void toMap_wrongTypeFrom_throwsClassCastException() {
        // given
        Object notAPeriod = "I am not a Period";
        // when / then
        assertThrows(ClassCastException.class, () -> PeriodConversions.toMap(notAPeriod, mockConverter), "Casting a non‑Period object should throw ClassCastException");
    }
}
