package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.YearMonth;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.YEAR_MONTH;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class YearMonthConversions_toMap_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    converter;

    @Mock
    private Converter // not used by the method but required by the signature
    mockConverter;

    /**
     * Provider for all “Valid conversion” cases (including boundary values).
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// regular values
        Arguments.of(YearMonth.of(2023, 5), Mockito.mock(Converter.class), "2023-05"), Arguments.of(YearMonth.of(1999, 12), null, "1999-12"), // boundary years
        Arguments.of(YearMonth.of(-999_999_999, 1), null, "-999999999-01"), Arguments.of(YearMonth.of(999_999_999, 12), null, "999999999-12"), // zero and negative years
        Arguments.of(YearMonth.of(0, 1), null, "0000-01"), Arguments.of(YearMonth.of(-5, 3), null, "-0005-03"));
    }

    /**
     * Provider for the “Converter argument ignored” group.
     */
    private static Stream<Arguments> converterIgnoredProvider() {
        // a mock that throws on any method invocation
        Converter throwingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("Should not be called");
        });
        // an arbitrary non‑null object cast to Converter (unchecked)
        @SuppressWarnings("unchecked")
        Converter arbitraryObject = (Converter) (Object) new Object();
        return Stream.of(Arguments.of(YearMonth.of(2022, 11), throwingConverter, "2022-11"), Arguments.of(YearMonth.of(2021, 7), arbitraryObject, "2021-07"));
    }

    /**
     * Provider for the “Map structure verification” case.
     */
    private static Stream<Arguments> mapStructureProvider() {
        return Stream.of(Arguments.of(YearMonth.of(2024, 2), 1, YEAR_MONTH, "2024-02"));
    }

    @Test
    void toMapReturnsCorrectMapForValidYearMonth() {
        // given
        YearMonth yearMonth = YearMonth.of(2023, 5);
        // when
        Map<String, Object> result = YearMonthConversions.toMap(yearMonth, mockConverter);
        // then
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertEquals(yearMonth.toString(), result.get(YEAR_MONTH), "Map value must be the string representation of the YearMonth");
    }

    @Test
    void toMapThrowsNullPointerExceptionWhenFromIsNull() {
        // given: a null source object
        // when / then
        assertThrows(NullPointerException.class, () -> YearMonthConversions.toMap(null, mockConverter), "Calling toMap with a null YearMonth should throw NPE");
    }

    @Test
    void toMapThrowsClassCastExceptionWhenFromIsNotYearMonth() {
        // given: an object of an incompatible type
        String notAYearMonth = "2023-05";
        // when / then
        assertThrows(ClassCastException.class, () -> YearMonthConversions.toMap(notAYearMonth, mockConverter), "Calling toMap with a non‑YearMonth should throw ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] toMap converts {0} -> \"{2}\"")
    @MethodSource("validConversionProvider")
    void toMapValidConversions(YearMonth from, Converter converter, String expectedString) {
        // when
        Map<String, Object> result = YearMonthConversions.toMap(from, converter);
        // then
        assertNotNull(result, "Result map must not be null");
        assertEquals(1, result.size(), "Map must contain exactly one entry");
        assertTrue(result.containsKey(YEAR_MONTH), "Map must contain the YEAR_MONTH key");
        assertEquals(expectedString, result.get(YEAR_MONTH), "Map value must match the expected YearMonth string");
    }

    @ParameterizedTest(name = "[{index}] Converter argument ignored for {0}")
    @MethodSource("converterIgnoredProvider")
    void toMapIgnoresConverterArgument(YearMonth from, Converter converter, String expectedString) {
        // when
        Map<String, Object> result = YearMonthConversions.toMap(from, converter);
        // then
        assertNotNull(result);
        assertEquals(expectedString, result.get(YEAR_MONTH));
    }

    @ParameterizedTest(name = "[{index}] Map structure verification for {0}")
    @MethodSource("mapStructureProvider")
    void toMapStructureVerification(YearMonth from, int expectedSize, String expectedKey, String expectedValue) {
        // when
        Map<String, Object> result = YearMonthConversions.toMap(from, null);
        // then
        assertNotNull(result, "Result map must not be null");
        assertEquals(expectedSize, result.size(), "Map should contain exactly " + expectedSize + " entry/entries");
        assertTrue(result.containsKey(expectedKey), "Map must contain the expected key: " + expectedKey);
        assertEquals(expectedValue, result.get(expectedKey), "Map value must match the expected YearMonth string");
    }
}
