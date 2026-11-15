package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YearConversions_toAtomicInteger_Test {

    /**
     * Shared Converter instance used by all tests.
     */
    private static Converter converter;

    /**
     * Group: Valid conversions – using a custom Converter
     */
    @Mock
    Converter // injected by Mockito
    mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; using the default options is sufficient
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference
        converter = null;
    }

    private static Stream<Arguments> primitiveAndWrapperProvider() {
        return Stream.of(// Integer
        Arguments.of(42, 42), // numeric String
        Arguments.of("123", 123), // true → 1
        Arguments.of(Boolean.TRUE, 1), // false → 0
        Arguments.of(Boolean.FALSE, 0));
    }

    private static Stream<Arguments> numericAndTemporalProvider() {
        return Stream.of(// Double
        Arguments.of(99.9d, "Double (fraction truncated)", 99), // Long overflow
        Arguments.of(1234567890123L, "Long (overflow)", (int) 1234567890123L), // BigDecimal
        Arguments.of(new BigDecimal("99.9"), "BigDecimal (fraction discarded)", 99), // Year
        Arguments.of(Year.of(2020), "Year", 2020));
    }

    private static Stream<Arguments> yearEdgeCasesProvider() {
        return Stream.of(// minimum representable year
        Arguments.of(Year.of(1), 1), // typical maximum year
        Arguments.of(Year.of(9999), 9999));
    }

    @Test
    void toAtomicInteger_fromInteger_returnsSameValue() {
        // given
        Integer source = 42;
        // when
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(42, result.get());
    }

    @Test
    void toAtomicInteger_fromStringNumeric_returnsParsedValue() {
        // given
        String source = "123";
        // when
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(123, result.get());
    }

    @Test
    void toAtomicInteger_fromYear_returnsYearValue() {
        // given
        Year source = Year.of(2023);
        // when
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(2023, result.get());
    }

    @Test
    void toAtomicInteger_fromNull_returnsZero() {
        // given
        Object source = null;
        // when
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        // then
        assertNotNull(result);
        assertEquals(0, result.get());
    }

    /**
     * Group: Valid conversions – primitive and wrapper types
     */
    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("primitiveAndWrapperProvider")
    void toAtomicInteger_validPrimitiveAndWrapper(Object from, int expected) {
        // The method under test should treat a null converter as “use default conversion”.
        AtomicInteger result = YearConversions.toAtomicInteger(from, null);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    /**
     * Group: Valid conversions – numeric and temporal types
     */
    @ParameterizedTest(name = "[{index}] from={0} ({1}) → expected={2}")
    @MethodSource("numericAndTemporalProvider")
    void toAtomicInteger_validNumericAndTemporal(Object from, String typeDesc, int expected) {
        AtomicInteger result = YearConversions.toAtomicInteger(from, null);
        assertNotNull(result);
        assertEquals(expected, result.get(), "Conversion of " + typeDesc + " failed");
    }

    /**
     * Group: Valid conversions – Year edge cases
     */
    @ParameterizedTest(name = "[{index}] Year={0} → expected={1}")
    @MethodSource("yearEdgeCasesProvider")
    void toAtomicInteger_yearEdgeCases(Year year, int expected) {
        AtomicInteger result = YearConversions.toAtomicInteger(year, null);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    void toAtomicInteger_customConverterReturns7() {
        // Arrange – the mock converter always returns 7 for any int conversion request
        when(mockConverter.convert(any(), eq(Integer.class))).thenReturn(7);
        // source value is irrelevant – the mock decides the result
        Object source = new Object();
        // Act
        AtomicInteger result = YearConversions.toAtomicInteger(source, mockConverter);
        // Assert
        assertNotNull(result);
        assertEquals(7, result.get());
        verify(mockConverter).convert(eq(source), eq(Integer.class));
    }

    /**
     * Group: Invalid conversions – unsupported type (default converter)
     */
    @Test
    void toAtomicInteger_unsupportedType_throwsIllegalArgumentException() {
        // no conversion rule for plain Object
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toAtomicInteger(unsupported, null), "Expected IllegalArgumentException for unsupported source type");
    }

    /**
     * Group: Invalid conversions – null converter with non‑convertible source
     */
    @Test
    void toAtomicInteger_nullConverterWithUnsupportedSource_throwsIllegalArgumentException() {
        // still unsupported
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toAtomicInteger(unsupported, null), "When converter is null and source is unsupported, an exception should be thrown");
    }
}
