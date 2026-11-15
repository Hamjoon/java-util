package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toAtomicBoolean_Test {

    private Converter converter;

    private Converter defaultConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Use a real Converter with default options – this gives us the built‑in conversion logic.
        defaultConverter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies a comprehensive set of inputs together with the expected boolean
     * value or the expected exception type.
     */
    private static Stream<Arguments> basicConversionProvider() {
        return Stream.of(// ----- Primitive booleans & strings (case‑insensitive, trimmed) -----
        Arguments.of(true, true, null), Arguments.of(false, false, null), Arguments.of("true", true, null), Arguments.of("FALSE", false, null), Arguments.of("yes", true, null), Arguments.of("no", false, null), Arguments.of("   true   ", true, null), Arguments.of("   FALSE   ", false, null), Arguments.of("TrUe", true, null), Arguments.of("randomText", false, null), // ----- Numeric values (any non‑zero is true) -----
        Arguments.of(1, true, null), Arguments.of(0, false, null), Arguments.of(-5, true, null), Arguments.of(0L, false, null), Arguments.of(2L, true, null), Arguments.of(0.0d, false, null), Arguments.of(0.5d, true, null), // ----- BigDecimal / BigInteger -----
        Arguments.of(new BigDecimal("0"), false, null), Arguments.of(new BigDecimal("1.23"), true, null), Arguments.of(new BigInteger("0"), false, null), Arguments.of(new BigInteger("-10"), true, null), // ----- Other object types that expose a textual representation -----
        Arguments.of(new StringBuilder("true"), true, null), Arguments.of(new StringBuilder("FALSE"), false, null), Arguments.of(CharBuffer.wrap("yes"), true, null), Arguments.of(CharBuffer.wrap("no"), false, null), // ----- AtomicBoolean input (should be unwrapped) -----
        Arguments.of(new AtomicBoolean(true), true, null), // ----- Enum (no matching truthy literal) -----
        Arguments.of(DayOfWeek.MONDAY, false, null), // ----- Empty / whitespace strings -----
        Arguments.of("", false, null), Arguments.of("   ", false, null), // ----- Numeric strings -----
        Arguments.of("1", true, null), Arguments.of("0", false, null), // ----- Unrecognised truthy keyword "on" (treated as false) -----
        Arguments.of("on", false, null), // ----- Null source (default conversion yields false) -----
        Arguments.of(null, false, null), // ----- Unsupported source types (should raise IllegalArgumentException) -----
        Arguments.of(new int[] { 1, 2, 3 }, null, IllegalArgumentException.class), Arguments.of(Arrays.asList(true, false), null, IllegalArgumentException.class), Arguments.of(Collections.singletonMap("key", "value"), null, IllegalArgumentException.class), Arguments.of(java.time.LocalDate.now(), null, IllegalArgumentException.class));
    }

    @Test
    void shouldConvertBooleanTrue() {
        AtomicBoolean ab = StringConversions.toAtomicBoolean(true, converter);
        assertTrue(ab.get(), "AtomicBoolean should be true for Boolean true");
    }

    @Test
    void shouldConvertBooleanFalse() {
        AtomicBoolean ab = StringConversions.toAtomicBoolean(false, converter);
        assertFalse(ab.get(), "AtomicBoolean should be false for Boolean false");
    }

    @Test
    void shouldConvertStringTrue() {
        AtomicBoolean ab = StringConversions.toAtomicBoolean("true", converter);
        assertTrue(ab.get(), "AtomicBoolean should be true for string \"true\"");
    }

    @Test
    void shouldConvertStringFalse() {
        AtomicBoolean ab = StringConversions.toAtomicBoolean("false", converter);
        assertFalse(ab.get(), "AtomicBoolean should be false for string \"false\"");
    }

    @Test
    void shouldConvertNumericOne() {
        AtomicBoolean ab = StringConversions.toAtomicBoolean(1, converter);
        assertTrue(ab.get(), "AtomicBoolean should be true for numeric 1");
    }

    @Test
    void shouldConvertNumericZero() {
        AtomicBoolean ab = StringConversions.toAtomicBoolean(0, converter);
        assertFalse(ab.get(), "AtomicBoolean should be false for numeric 0");
    }

    @Test
    void shouldReturnFalseForNull() {
        AtomicBoolean ab = StringConversions.toAtomicBoolean(null, converter);
        assertFalse(ab.get(), "AtomicBoolean should be false when source is null");
    }

    @Test
    void shouldReturnFalseForUnconvertibleObject() {
        Object unknown = new Object();
        AtomicBoolean ab = StringConversions.toAtomicBoolean(unknown, converter);
        // The default conversion for an unknown object yields false
        assertFalse(ab.get(), "AtomicBoolean should be false for an unconvertible object");
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("basicConversionProvider")
    void shouldConvertVariousInputs(Object from, Boolean expectedValue, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // Expect an exception to be thrown
            assertThrows(expectedException, () -> StringConversions.toAtomicBoolean(from, defaultConverter), "Expected exception for input: " + from);
        } else {
            // Normal conversion – verify the boolean value wrapped in AtomicBoolean
            AtomicBoolean result = StringConversions.toAtomicBoolean(from, defaultConverter);
            assertEquals(expectedValue, result.get(), "Unexpected conversion result for input: " + from);
        }
    }

    @Test
    void shouldPropagateExceptionFromSourceToString() {
        Object badToString = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("toString failure");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toAtomicBoolean(badToString, defaultConverter));
        assertEquals("toString failure", ex.getMessage());
    }

    @Test
    void customConverterForcesFalseEvenWhenSourceIsTrue(@Mock Converter mockConverter) {
        // Mock converter returns Boolean.FALSE regardless of input
        when(mockConverter.convert(any(), eq(Boolean.class))).thenReturn(Boolean.FALSE);
        AtomicBoolean result = StringConversions.toAtomicBoolean(true, mockConverter);
        assertFalse(result.get(), "Custom converter forced false should be reflected in the result");
    }

    @Test
    void customConverterForcesTrueWhenSourceIsNull(@Mock Converter mockConverter) {
        // Mock converter returns Boolean.TRUE for a null source
        when(mockConverter.convert(isNull(), eq(Boolean.class))).thenReturn(Boolean.TRUE);
        AtomicBoolean result = StringConversions.toAtomicBoolean(null, mockConverter);
        assertTrue(result.get(), "Custom converter forced true for null source should be reflected");
    }

    @Test
    void customConverterThrowsRuntimeExceptionShouldPropagate(@Mock Converter mockConverter) {
        // Mock converter throws a RuntimeException
        when(mockConverter.convert(any(), eq(Boolean.class))).thenThrow(new RuntimeException("converter failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toAtomicBoolean("false", mockConverter));
        assertEquals("converter failure", ex.getMessage());
    }

    @Test
    void customConverterReturningNullLeadsToNullPointerException() {
        // Create a mock that returns null for Boolean conversion
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), eq(Boolean.class))).thenReturn(null);
        // The internal Boolean conversion will attempt to unbox the null and throw NPE
        assertThrows(NullPointerException.class, () -> StringConversions.toAtomicBoolean("true", mockConverter));
    }
}
