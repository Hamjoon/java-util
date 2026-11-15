package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.time.Period;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class PeriodConversions_toMap_Test {

    @Mock
    private Converter // not used by the method but required by signature
    mockConverter;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for @Mock fields.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Close the Mockito session to release resources.
        mocks.close();
        // Reset the mock to a clean state for the next test.
        reset(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Normal cases – positive, zero and negative period components
     * ------------------------------------------------------------- */
    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(// Typical positive values
        Arguments.of(Period.of(5, 3, 12), 5, 3, 12, "Typical Period with positive values should be converted to a map containing the same year, month and day values."), // All zeros
        Arguments.of(Period.of(0, 0, 0), 0, 0, 0, "A Period with all zero components should produce a map with zero values for each entry."), // Negative components (Period supports them)
        Arguments.of(Period.of(-2, -4, -7), -2, -4, -7, "Period can hold negative values; the method must copy those negatives unchanged into the resulting map."));
    }

    /* -------------------------------------------------------------
     *  Large value cases – boundary values of int
     * ------------------------------------------------------------- */
    private static Stream<Arguments> largeValueCasesProvider() {
        return Stream.of(Arguments.of(Period.ofYears(Integer.MAX_VALUE), Integer.MAX_VALUE, 0, 0, "Maximum positive int for years should be copied unchanged; months and days default to 0."), Arguments.of(Period.ofMonths(Integer.MAX_VALUE), 0, Integer.MAX_VALUE, 0, "Maximum positive int for months should be copied unchanged; years and days default to 0."), Arguments.of(Period.ofDays(Integer.MAX_VALUE), 0, 0, Integer.MAX_VALUE, "Maximum positive int for days should be copied unchanged; years and months default to 0."), Arguments.of(Period.ofYears(Integer.MIN_VALUE), Integer.MIN_VALUE, 0, 0, "Minimum (most negative) int for years should be copied unchanged; months and days default to 0."));
    }

    @Test
    void toMap_ShouldReturnMapWithCorrectPeriodComponents() {
        // GIVEN
        // 5 years, 3 months, 12 days
        Period period = Period.of(5, 3, 12);
        // WHEN
        Map<String, Object> result = PeriodConversions.toMap(period, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(3, result.size(), "Map should contain exactly three entries");
        assertEquals(5, result.get(MapConversions.YEARS), "Years value mismatch");
        assertEquals(3, result.get(MapConversions.MONTHS), "Months value mismatch");
        assertEquals(12, result.get(MapConversions.DAYS), "Days value mismatch");
        // Verify that the converter is not interacted with (method does not use it)
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toMap_WithZeroValues_ShouldHandleCorrectly() {
        // GIVEN
        Period period = Period.of(0, 0, 0);
        // WHEN
        Map<String, Object> result = PeriodConversions.toMap(period, mockConverter);
        // THEN
        assertEquals(0, result.get(MapConversions.YEARS));
        assertEquals(0, result.get(MapConversions.MONTHS));
        assertEquals(0, result.get(MapConversions.DAYS));
    }

    @Test
    void toMap_NullPeriod_ShouldThrowClassCastException() {
        // GIVEN
        Object nullPeriod = null;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> PeriodConversions.toMap(nullPeriod, mockConverter), "Passing null (or a non‑Period) should result in a ClassCastException");
    }

    @Test
    void toMap_NonPeriodObject_ShouldThrowClassCastException() {
        // GIVEN
        Object notAPeriod = "I am not a Period";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> PeriodConversions.toMap(notAPeriod, mockConverter), "If the supplied object is not a java.time.Period, the cast fails and a ClassCastException must be thrown.");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalCasesProvider")
    void toMap_NormalCases_ShouldReturnCorrectMap(Period period, int expectedYears, int expectedMonths, int expectedDays, String description) {
        // WHEN
        Map<String, Object> result = PeriodConversions.toMap(period, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(3, result.size(), "Map should contain exactly three entries");
        assertEquals(expectedYears, result.get(MapConversions.YEARS), "Years value mismatch");
        assertEquals(expectedMonths, result.get(MapConversions.MONTHS), "Months value mismatch");
        assertEquals(expectedDays, result.get(MapConversions.DAYS), "Days value mismatch");
        // The method does not interact with the converter.
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] {4}")
    @MethodSource("largeValueCasesProvider")
    void toMap_LargeValueCases_ShouldHandleBoundaryValues(Period period, int expectedYears, int expectedMonths, int expectedDays, String description) {
        // WHEN
        Map<String, Object> result = PeriodConversions.toMap(period, mockConverter);
        // THEN
        assertEquals(expectedYears, result.get(MapConversions.YEARS), "Years value mismatch");
        assertEquals(expectedMonths, result.get(MapConversions.MONTHS), "Months value mismatch");
        assertEquals(expectedDays, result.get(MapConversions.DAYS), "Days value mismatch");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Converter argument is ignored – even when non‑null
     * ------------------------------------------------------------- */
    @Test
    void toMap_ConverterIgnored_NonNullConverterShouldNotAffectResult() {
        // GIVEN
        Period period = Period.of(1, 1, 1);
        // Use a separate mock to emphasise that the method does not touch it.
        Converter anotherMock = mock(Converter.class);
        // WHEN
        Map<String, Object> result = PeriodConversions.toMap(period, anotherMock);
        // THEN
        assertEquals(1, result.get(MapConversions.YEARS));
        assertEquals(1, result.get(MapConversions.MONTHS));
        assertEquals(1, result.get(MapConversions.DAYS));
        // Verify that the supplied converter was never used.
        verifyNoInteractions(anotherMock);
    }

    /* -------------------------------------------------------------
     *  Exception cases – null input and non‑Period input
     * ------------------------------------------------------------- */
    @Test
    void toMap_NullPeriod_ShouldThrowNullPointerException() {
        // GIVEN
        Object nullPeriod = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> PeriodConversions.toMap(nullPeriod, mockConverter), "Casting a null reference to Period succeeds, but accessing its fields triggers a NullPointerException");
    }
}
