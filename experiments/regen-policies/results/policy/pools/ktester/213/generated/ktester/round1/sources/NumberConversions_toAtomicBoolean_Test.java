package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toAtomicBoolean_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The real Converter works with the built‑in conversion logic.
        // An empty options instance gives us the default behaviour.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// zero long → false
        Arguments.of(0L, new Converter(new ConverterOptions()), false), // non‑zero integer → true
        Arguments.of(123, new Converter(new ConverterOptions()), true), // string "0" → false
        Arguments.of("0", new Converter(new ConverterOptions()), false), // string "42" → true
        Arguments.of("42", new Converter(new ConverterOptions()), true), // null → false (treated as 0 by the conversion logic)
        Arguments.of(null, new Converter(new ConverterOptions()), false), // Boolean true → true (converted to 1)
        Arguments.of(Boolean.TRUE, new Converter(new ConverterOptions()), true), // Boolean false → false (converted to 0)
        Arguments.of(Boolean.FALSE, new Converter(new ConverterOptions()), false), // BigInteger.ZERO → false
        Arguments.of(BigInteger.ZERO, new Converter(new ConverterOptions()), false), // large non‑zero BigInteger → true (any non‑zero becomes true)
        Arguments.of(new BigInteger("12345678901234567890"), new Converter(new ConverterOptions()), true), // Double.NaN → true (NumberConversions treats NaN as non‑zero for boolean conversion)
        Arguments.of(Double.NaN, new Converter(new ConverterOptions()), true), // Double.POSITIVE_INFINITY → true
        Arguments.of(Double.POSITIVE_INFINITY, new Converter(new ConverterOptions()), true), // trimmed string "   0   " → false
        Arguments.of("   0   ", new Converter(new ConverterOptions()), false), // trimmed negative string "   -5   " → true
        Arguments.of("   -5   ", new Converter(new ConverterOptions()), true), // AtomicInteger(0) → false
        Arguments.of(new AtomicInteger(0), new Converter(new ConverterOptions()), false), // AtomicLong(42L) → true
        Arguments.of(new AtomicLong(42L), new Converter(new ConverterOptions()), true));
    }

    private static Stream<Arguments> provideExceptionConversions() {
        // Helper to create a mock Converter that throws a given exception when asked to convert to Long
        Converter illegalArgMock = mock(Converter.class);
        when(illegalArgMock.convert(any(), eq(Long.class))).thenThrow(new IllegalArgumentException("cannot convert"));
        Converter nullPointerMock = mock(Converter.class);
        when(nullPointerMock.convert(any(), eq(Long.class))).thenThrow(new NullPointerException("converter is null"));
        Converter numberFormatMock = mock(Converter.class);
        when(numberFormatMock.convert(any(), eq(Long.class))).thenThrow(new NumberFormatException("bad number"));
        Converter classCastMock = mock(Converter.class);
        when(classCastMock.convert(any(), eq(Long.class))).thenThrow(new ClassCastException("unsupported type"));
        return Stream.of(// Converter that cannot handle the object → IllegalArgumentException
        Arguments.of(new Object(), illegalArgMock, IllegalArgumentException.class), // Null converter passed → NullPointerException (the real method will NPE when trying to use it)
        Arguments.of(5L, null, NullPointerException.class), // Both parameters null → NullPointerException
        Arguments.of(null, null, NullPointerException.class), // String that cannot be parsed → NumberFormatException
        Arguments.of("notANumber", numberFormatMock, NumberFormatException.class), // Unsupported type causing ClassCastException
        Arguments.of(new java.util.concurrent.atomic.AtomicReference<>(42L), classCastMock, ClassCastException.class));
    }

    @Test
    void toAtomicBoolean_nonZeroNumber_returnsTrue() {
        // given
        Integer source = 5;
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, converter);
        // then
        assertTrue(result.get(), "AtomicBoolean should be true for non‑zero numeric values");
    }

    @Test
    void toAtomicBoolean_zeroNumber_returnsFalse() {
        // given
        Integer source = 0;
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, converter);
        // then
        assertFalse(result.get(), "AtomicBoolean should be false for zero numeric values");
    }

    @Test
    void toAtomicBoolean_nonZeroString_returnsTrue() {
        // given
        String source = "123";
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, converter);
        // then
        assertTrue(result.get(), "AtomicBoolean should be true for non‑zero numeric strings");
    }

    @Test
    void toAtomicBoolean_zeroString_returnsFalse() {
        // given
        String source = "0";
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, converter);
        // then
        assertFalse(result.get(), "AtomicBoolean should be false for string representing zero");
    }

    @Test
    void toAtomicBoolean_null_returnsFalse() {
        // given
        Object source = null;
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, converter);
        // then
        assertFalse(result.get(), "AtomicBoolean should be false when the source is null (treated as 0)");
    }

    @Test
    void toAtomicBoolean_booleanTrue_returnsTrue() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, converter);
        // then
        assertTrue(result.get(), "AtomicBoolean should be true when source Boolean is true (converted to 1)");
    }

    @Test
    void toAtomicBoolean_booleanFalse_returnsFalse() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(source, converter);
        // then
        assertFalse(result.get(), "AtomicBoolean should be false when source Boolean is false (converted to 0)");
    }

    @ParameterizedTest(name = "{index}: from={0} → expected={2}")
    @MethodSource("provideSuccessfulConversions")
    void toAtomicBoolean_success(Object from, Converter conv, boolean expected) {
        // when
        AtomicBoolean result = NumberConversions.toAtomicBoolean(from, conv);
        // then
        assertEquals(expected, result.get(), () -> "AtomicBoolean should be " + expected + " for input: " + from);
    }

    @ParameterizedTest(name = "{index}: from={0} → throws {2}")
    @MethodSource("provideExceptionConversions")
    void toAtomicBoolean_exceptions(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toAtomicBoolean(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
