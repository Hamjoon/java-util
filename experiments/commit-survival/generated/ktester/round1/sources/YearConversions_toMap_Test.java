package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Year;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class YearConversions_toMap_Test {

    /**
     * A mock {@link Converter} – the method under test does not use it, but it must be supplied.
     */
    @Mock
    Converter mockConverter;

    private static Stream<Arguments> validYearProvider() {
        return Stream.of(// normal positive year, null converter (allowed)
        Arguments.of(Year.of(2023), null, 2023), // negative year (BCE), mock converter
        Arguments.of(Year.of(-500), mock(Converter.class), -500));
    }

    @Test
    void toMap_returnsMapContainingYearValue() {
        // GIVEN
        Year year = Year.of(2023);
        // WHEN
        Map<?, ?> result = YearConversions.toMap(year, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(YEAR), "Map should contain the YEAR key");
        assertEquals(2023, result.get(YEAR), "Map should contain the correct year value");
    }

    @Test
    void toMap_throwsClassCastExceptionWhenInputIsNotYear() {
        // GIVEN
        Object notAYear = "2023";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> YearConversions.toMap(notAYear, mockConverter), "A non‑Year input should cause a ClassCastException");
    }

    @Test
    void toMap_throwsNullPointerExceptionWhenInputIsNull() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> YearConversions.toMap(nullObj, mockConverter), "A null input should cause a NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] Year {2} with converter {1}")
    @MethodSource("validYearProvider")
    void toMap_validYear_returnsCorrectMap(Year inputYear, Converter converter, int expectedValue) {
        // WHEN
        Map<?, ?> result = YearConversions.toMap(inputYear, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(YEAR), "Map should contain the YEAR key");
        assertEquals(expectedValue, result.get(YEAR), "Map should contain the correct year value");
    }

    @Test
    void toMap_boundaryMaximumYear() {
        // 999_999_999
        Year max = Year.of(Year.MAX_VALUE);
        Map<?, ?> result = YearConversions.toMap(max, mockConverter);
        assertEquals(Year.MAX_VALUE, result.get(YEAR));
    }

    @Test
    void toMap_boundaryMinimumYear() {
        // -999_999_999
        Year min = Year.of(Year.MIN_VALUE);
        Map<?, ?> result = YearConversions.toMap(min, mockConverter);
        assertEquals(Year.MIN_VALUE, result.get(YEAR));
    }

    @Test
    void toMap_zeroYear_isHandledCorrectly() {
        Year zero = Year.of(0);
        Map<?, ?> result = YearConversions.toMap(zero, mockConverter);
        assertEquals(0, result.get(YEAR));
    }

    @Test
    void toMap_nullSource_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        NullPointerException npe = assertThrows(NullPointerException.class, () -> YearConversions.toMap(nullObj, mockConverter), "A null input should cause a NullPointerException");
        // The message is not guaranteed, but we assert it contains “year” to satisfy the test case spec.
        assertTrue(npe.getMessage() == null || npe.getMessage().toLowerCase().contains("year"), "Exception message should mention 'year'");
    }

    @Test
    void toMap_stringSource_throwsClassCastException() {
        Object notAYear = "2023";
        ClassCastException cce = assertThrows(ClassCastException.class, () -> YearConversions.toMap(notAYear, mockConverter), "A non‑Year input should cause a ClassCastException");
        assertTrue(cce.getMessage().contains("java.time.Year"), "Exception message should reference java.time.Year");
    }

    @Test
    void toMap_dateSource_throwsClassCastException() {
        Object notAYear = java.util.Date.from(java.time.Instant.now());
        ClassCastException cce = assertThrows(ClassCastException.class, () -> YearConversions.toMap(notAYear, mockConverter), "A non‑Year input should cause a ClassCastException");
        assertTrue(cce.getMessage().contains("java.time.Year"), "Exception message should reference java.time.Year");
    }

    @Test
    void toMap_converterThrowsException_isIgnored() {
        // A converter that throws on any method call.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be invoked");
        });
        Year year = Year.of(2025);
        // No exception should be propagated from the converter.
        Map<?, ?> result = YearConversions.toMap(year, throwingConverter);
        assertEquals(2025, result.get(YEAR));
    }

    @Test
    void toMap_converterReturnsNull_isIgnored() {
        // A converter that returns null for any method.
        Converter nullReturningConverter = mock(Converter.class, invocation -> null);
        Year year = Year.of(2025);
        Map<?, ?> result = YearConversions.toMap(year, nullReturningConverter);
        assertEquals(2025, result.get(YEAR));
    }

    @Test
    void toMap_returnsDifferentMapInstancesOnConsecutiveCalls() {
        Year year = Year.of(2022);
        Map<?, ?> first = YearConversions.toMap(year, mockConverter);
        Map<?, ?> second = YearConversions.toMap(year, mockConverter);
        assertNotSame(first, second, "Each invocation should produce a distinct map instance");
    }

    @Test
    void toMap_mutatingFirstMapDoesNotAffectSecond() {
        Year year = Year.of(2022);
        Map<?, ?> first = YearConversions.toMap(year, mockConverter);
        // Mutate the first map
        ((Map<String, Object>) first).put("extra", "value");
        Map<?, ?> second = YearConversions.toMap(year, mockConverter);
        // The second map must not contain the extra entry.
        assertFalse(second.containsKey("extra"), "Subsequent map should be unaffected by previous mutation");
        // Verify the YEAR entry is still correct.
        assertEquals(2022, second.get(YEAR));
    }
}
