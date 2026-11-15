package com.cedarsoftware.util.convert;

import java.time.Year;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;

@ExtendWith(MockitoExtension.class)
class YearConversions_toDouble_Test {

    @Mock
    private Converter // injected by MockitoExtension
    converter;

    @Mock
    private Converter // used only for the last test case
    customConverter;

    /**
     * Helper holder class to keep test data tidy.
     */
    private static class ArgumentsHolder {

        final Object from;

        // "custom" means use the Mockito mock, otherwise null
        final String converterTag;

        final double expectedDouble;

        final Class<? extends Throwable> expectedException;

        ArgumentsHolder(Object from, String converterTag, double expectedDouble) {
            this.from = from;
            this.converterTag = converterTag;
            this.expectedDouble = expectedDouble;
            this.expectedException = null;
        }

        ArgumentsHolder(Object from, String converterTag, Class<? extends Throwable> expectedException) {
            this.from = from;
            this.converterTag = converterTag;
            this.expectedDouble = Double.NaN;
            this.expectedException = expectedException;
        }
    }

    /**
     * Parameter provider that mirrors the JSON test cases.
     */
    private static Stream<ArgumentsHolder> toDoubleProvider() {
        return Stream.of(// Integer input
        new ArgumentsHolder(2023, null, 2023.0), // String representing an integer
        new ArgumentsHolder("2023", null, 2023.0), // Year object
        new ArgumentsHolder(Year.of(2023), null, 2023.0), // Null input
        new ArgumentsHolder(null, null, 0.0), // BigDecimal (truncated)
        new ArgumentsHolder(new BigDecimal("123.45"), null, 123.0), // AtomicInteger
        new ArgumentsHolder(new AtomicInteger(99), null, 99.0), // Unsupported type – expect ClassCastException
        new ArgumentsHolder(new Object(), null, ClassCastException.class), // Long within int range
        new ArgumentsHolder(Long.valueOf(2023L), null, 2023.0), // Long exceeding Integer.MAX_VALUE (overflow)
        new ArgumentsHolder(Long.valueOf(3000000000L), null, (double) ((int) 3000000000L)), // Short
        new ArgumentsHolder(Short.valueOf((short) 12), null, 12.0), // Byte
        new ArgumentsHolder(Byte.valueOf((byte) 7), null, 7.0), // Double (truncated)
        new ArgumentsHolder(Double.valueOf(45.67), null, 45.0), // Float (truncated)
        new ArgumentsHolder(Float.valueOf(89.99f), null, 89.0), // BigInteger within int range
        new ArgumentsHolder(new BigInteger("123456"), null, 123456.0), // BigInteger larger than Integer.MAX_VALUE (intValue() wraps)
        new ArgumentsHolder(new BigInteger("4294967296"), null, (double) new BigInteger("4294967296").intValue()), // AtomicLong
        new ArgumentsHolder(new AtomicLong(2025L), null, 2025.0), // Map with YEAR key
        new ArgumentsHolder(Collections.singletonMap(YEAR, 1999), null, 1999.0), // Boolean (unsupported)
        new ArgumentsHolder(Boolean.TRUE, null, ClassCastException.class), // Unknown type with a custom converter returning 2022
        new ArgumentsHolder(new Object(), "custom", 2022.0));
    }

    @Test
    void toDouble_WithYearObject_ReturnsYearAsDouble() {
        // GIVEN
        Year year = Year.of(2022);
        when(converter.convert(eq(year), eq(Integer.class))).thenReturn(2022);
        // WHEN
        double result = YearConversions.toDouble(year, converter);
        // THEN
        assertEquals(2022.0, result, "Year should be converted to its numeric value as double");
        verify(converter, times(1)).convert(eq(year), eq(Integer.class));
    }

    @Test
    void toDouble_WithIntegerObject_ReturnsSameValueAsDouble() {
        // GIVEN
        Integer value = 7;
        when(converter.convert(eq(value), eq(Integer.class))).thenReturn(7);
        // WHEN
        double result = YearConversions.toDouble(value, converter);
        // THEN
        assertEquals(7.0, result, "Integer should be converted to double with the same numeric value");
        verify(converter, times(1)).convert(eq(value), eq(Integer.class));
    }

    @Test
    void toDouble_WithNull_ReturnsZeroDouble() {
        // GIVEN
        when(converter.convert(isNull(), eq(Integer.class))).thenReturn(0);
        // WHEN
        double result = YearConversions.toDouble(null, converter);
        // THEN
        assertEquals(0.0, result, "Null input should be converted to 0.0");
        verify(converter, times(1)).convert(isNull(), eq(Integer.class));
    }

    @ParameterizedTest(name = "{index}: from={0}, converter={1}")
    @MethodSource("toDoubleProvider")
    void toDouble_VariousInputs(ArgumentsHolder holder) {
        // Arrange
        Object from = holder.from;
        Converter conv = null;
        if ("custom".equals(holder.converterTag)) {
            // Stub the custom converter to return Integer 2022 for any input.
            when(customConverter.convert(eq(from), eq(Integer.class))).thenReturn(2022);
            conv = customConverter;
        }
        // Act & Assert
        if (holder.expectedException != null) {
            assertThrows(holder.expectedException, () -> YearConversions.toDouble(from, conv), "Expected exception for input: " + from);
        } else {
            double result = YearConversions.toDouble(from, conv);
            assertEquals(holder.expectedDouble, result, 0.000001, "Unexpected conversion result for input: " + from);
        }
    }
}
