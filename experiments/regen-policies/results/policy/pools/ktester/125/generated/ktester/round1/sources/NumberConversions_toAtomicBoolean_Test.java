package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toAtomicBoolean_Test {

    @Mock
    private Converter // injected by Mockito
    mockConverter;

    /**
     * Supplies all test scenarios defined in the JSON document.
     *
     * <p>Each {@link Arguments} contains:
     * <ul>
     *   <li>the {@code from} value to pass to {@code toAtomicBoolean}</li>
     *   <li>a {@link ConverterBehavior} describing the mock's behaviour</li>
     *   <li>the {@link Long} value the mock should return (or {@code null})</li>
     *   <li>the {@link Throwable} class the mock should throw (or {@code null})</li>
     *   <li>the expected {@link Boolean} result (or {@code null} when an exception is expected)</li>
     *   <li>a description (used only for readability)</li>
     * </ul>
     * </p>
     */
    private static Stream<Arguments> toAtomicBooleanProvider() {
        return Stream.of(// ----------- NumberConversions.toAtomicBoolean tests -----------------
        // Numeric Long zero → false
        Arguments.of(0L, ConverterBehavior.RETURN_LONG, 0L, null, false, "Numeric Long zero should be converted to false"), // Positive integer → true
        Arguments.of(10, ConverterBehavior.RETURN_LONG, 10L, null, true, "Positive integer value should be converted to true"), // String "0" → false
        Arguments.of("0", ConverterBehavior.RETURN_LONG, 0L, null, false, "String \"0\" is parsed to long 0, resulting in false"), // String "123" → true
        Arguments.of("123", ConverterBehavior.RETURN_LONG, 123L, null, true, "String \"123\" is parsed to long 123, resulting in true"), // null source → NullPointerException (no interaction)
        Arguments.of(null, ConverterBehavior.NO_INTERACTION, null, NullPointerException.class, null, "Passing null as the source object should cause a NullPointerException"), // null Converter → NullPointerException (no interaction)
        Arguments.of(5, ConverterBehavior.NO_INTERACTION, null, NullPointerException.class, null, "Providing a null Converter should result in a NullPointerException"), // Custom zero‑returning converter → false
        Arguments.of("any", ConverterBehavior.RETURN_LONG, 0L, null, false, "Custom Converter that always returns 0 forces false"), // Custom non‑zero‑returning converter → true
        Arguments.of("any", ConverterBehavior.RETURN_LONG, 42L, null, true, "Custom Converter that always returns non‑zero forces true"), // ----------- NumberConversions.toAtomicBoolean additional tests -----
        // Negative integer → true
        Arguments.of(-5, ConverterBehavior.RETURN_LONG, -5L, null, true, "Negative integer should be treated as non‑zero, resulting in true"), // Double zero → false
        Arguments.of(0.0d, ConverterBehavior.RETURN_LONG, 0L, null, false, "Double zero converts to long 0, therefore false"), // Small non‑zero double → true (simulated by returning 1)
        Arguments.of(0.0001d, ConverterBehavior.RETURN_LONG, 1L, null, true, "A small non‑zero double is converted to a non‑zero long, yielding true"), // String with whitespace → true
        Arguments.of("   42   ", ConverterBehavior.RETURN_LONG, 42L, null, true, "String with surrounding whitespace is trimmed and parsed to long 42 → true"), // Empty string → NumberFormatException
        Arguments.of("", ConverterBehavior.THROW_EXCEPTION, null, NumberFormatException.class, null, "Empty string cannot be parsed to a number"), // Boolean true → true
        Arguments.of(true, ConverterBehavior.RETURN_LONG, 1L, null, true, "Boolean true is interpreted as 1 (non‑zero) → true"), // Boolean false → false
        Arguments.of(false, ConverterBehavior.RETURN_LONG, 0L, null, false, "Boolean false is interpreted as 0 → false"), // AtomicInteger(0) → false
        Arguments.of(new AtomicInteger(0), ConverterBehavior.RETURN_LONG, 0L, null, false, "AtomicInteger holding 0 converts to long 0 → false"), // AtomicInteger(8) → true
        Arguments.of(new AtomicInteger(8), ConverterBehavior.RETURN_LONG, 8L, null, true, "AtomicInteger holding a non‑zero value converts to a non‑zero long → true"), // BigDecimal zero → false
        Arguments.of(new BigDecimal("0.0"), ConverterBehavior.RETURN_LONG, 0L, null, false, "BigDecimal zero is converted to long 0 → false"), // BigDecimal non‑zero → true
        Arguments.of(new BigDecimal("123.456"), ConverterBehavior.RETURN_LONG, 123L, null, true, "Non‑zero BigDecimal converts to a non‑zero long → true"), // Converter throws RuntimeException → RuntimeException propagates
        Arguments.of("any", ConverterBehavior.THROW_EXCEPTION, null, RuntimeException.class, null, "Custom converter throws a RuntimeException; the exception should propagate"), // ----------- NumberConversions.toAtomicBoolean exception edge cases -----
        // Unsupported object type → IllegalArgumentException
        Arguments.of(new Object(), ConverterBehavior.THROW_EXCEPTION, null, IllegalArgumentException.class, null, "Unsupported type should cause IllegalArgumentException"), // Whitespace‑only string → NumberFormatException
        Arguments.of("   ", ConverterBehavior.THROW_EXCEPTION, null, NumberFormatException.class, null, "String containing only whitespace cannot be parsed to a number"), // Malformed numeric string → NumberFormatException
        Arguments.of("12a3", ConverterBehavior.THROW_EXCEPTION, null, NumberFormatException.class, null, "Malformed numeric string should trigger NumberFormatException"), // Converter returns null → NullPointerException during unboxing
        Arguments.of("any", ConverterBehavior.RETURN_LONG, null, NullPointerException.class, null, "Converter returns null for the conversion causes NullPointerException"), // Converter returns wrong type → ClassCastException
        Arguments.of("any", ConverterBehavior.THROW_EXCEPTION, null, ClassCastException.class, null, "Converter returns incompatible type leading to ClassCastException"), // BigDecimal overflow → ArithmeticException
        Arguments.of(new BigDecimal("9223372036854775808"), ConverterBehavior.THROW_EXCEPTION, null, ArithmeticException.class, null, "BigDecimal larger than Long.MAX_VALUE should cause ArithmeticException"));
    }

    @Test
    void toAtomicBoolean_nonZeroLong_returnsTrue() {
        // given
        Object source = "doesn't matter";
        when(mockConverter.convert(any(), eq(Long.class))).thenReturn(42L);
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertTrue(result.get(), "AtomicBoolean should be true when the converted long is non‑zero");
        verify(mockConverter).convert(any(), eq(Long.class));
    }

    @Test
    void toAtomicBoolean_zeroLong_returnsFalse() {
        // given
        Object source = "doesn't matter";
        when(mockConverter.convert(any(), eq(Long.class))).thenReturn(0L);
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertFalse(result.get(), "AtomicBoolean should be false when the converted long is zero");
        verify(mockConverter).convert(any(), eq(Long.class));
    }

    @Test
    void toAtomicBoolean_nullSource_returnsFalseAndDoesNotUseConverter() {
        // given
        // null input
        Object source = null;
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertFalse(result.get(), "AtomicBoolean should be false for a null source (treated as 0)");
        // No conversion should be attempted because the source is null
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicBoolean_stringConvertedToLong_nonZero_returnsTrue() {
        // given
        Object source = "123";
        when(mockConverter.convert(eq("123"), eq(Long.class))).thenReturn(123L);
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertTrue(result.get(), "AtomicBoolean should be true when string converts to non‑zero long");
        verify(mockConverter).convert(eq("123"), eq(Long.class));
    }

    @Test
    void toAtomicBoolean_stringConvertedToLong_zero_returnsFalse() {
        // given
        Object source = "0";
        when(mockConverter.convert(eq("0"), eq(Long.class))).thenReturn(0L);
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, mockConverter);
        // then
        assertFalse(result.get(), "AtomicBoolean should be false when string converts to zero long");
        verify(mockConverter).convert(eq("0"), eq(Long.class));
    }

    /**
     * Parameterised test that drives all scenarios from {@link #toAtomicBooleanProvider()}.
     *
     * @param from               the source object passed to {@code toAtomicBoolean}
     * @param behavior           how the mock {@link Converter} should behave
     * @param longReturn         the {@link Long} value the mock should return (if applicable)
     * @param exceptionToThrow   the {@link Throwable} class the mock should throw (if applicable)
     * @param expectedResult     the expected boolean value wrapped in {@link AtomicBoolean}
     * @param description        textual description of the test case (used only for readability)
     */
    @ParameterizedTest(name = "{index}: {5}")
    @MethodSource("toAtomicBooleanProvider")
    void toAtomicBoolean_parameterised(Object from, ConverterBehavior behavior, Long longReturn, Class<? extends Throwable> exceptionToThrow, Boolean expectedResult, String description) {
        // -----------------------------------------------------------------
        // Arrange – create a fresh mock for each parameterised execution
        // -----------------------------------------------------------------
        Converter converter = null;
        if (behavior != ConverterBehavior.NO_INTERACTION) {
            converter = mock(Converter.class);
            if (behavior == ConverterBehavior.RETURN_LONG) {
                // The mock returns the supplied Long (which may be null to trigger NPE)
                when(converter.convert(any(), eq(Long.class))).thenReturn(longReturn);
            } else if (behavior == ConverterBehavior.THROW_EXCEPTION) {
                // The mock throws the supplied exception when convert() is invoked
                when(converter.convert(any(), eq(Long.class))).thenThrow(exceptionToThrow);
            }
        }
        // --------------------------------------------------------------
        // Act & Assert
        // --------------------------------------------------------------
        if (exceptionToThrow != null) {
            // Expect the exception to bubble out of toAtomicBoolean
            Throwable thrown = assertThrows(exceptionToThrow, () -> NumberConversions.toAtomicBoolean(from, converter), "Expected exception " + exceptionToThrow.getSimpleName());
            // For the special case where the method should fail before calling the mock
            // (null source or null converter), we also verify no interaction.
            if (behavior == ConverterBehavior.NO_INTERACTION) {
                if (converter != null) {
                    verifyNoInteractions(converter);
                }
            }
        } else {
            // Normal path – we expect a concrete AtomicBoolean result
            AtomicBoolean result = NumberConversions.toAtomicBoolean(from, converter);
            assertNotNull(result, "Resulting AtomicBoolean must not be null");
            assertEquals(expectedResult, result.get(), "AtomicBoolean value mismatch for case: " + description);
            // Verify that the mock was used exactly once when interaction is expected
            if (behavior != ConverterBehavior.NO_INTERACTION) {
                verify(converter).convert(any(), eq(Long.class));
            }
        }
    }
}
