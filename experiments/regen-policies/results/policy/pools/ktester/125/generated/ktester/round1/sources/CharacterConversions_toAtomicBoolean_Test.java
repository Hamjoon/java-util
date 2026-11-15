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
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toAtomicBoolean_Test {

    private static Converter converter;

    /**
     * Real converter used for the “happy‑path” tests that rely on the library’s built‑in logic.
     */
    private static Converter realConverter;

    @Mock
    private Converter // injected by MockitoExtension
    mockConverter;

    /**
     * Simple POJO used as a dummy source for custom‑converter tests.
     */
    private static final class CustomObj {
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter is used so that the built‑in conversion logic is exercised.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing to clean up for the static Converter instance.
        converter = null;
    }

    private static Stream<Arguments> numericInputs() {
        return Stream.of(Arguments.of(Integer.valueOf(1), true), Arguments.of(Long.valueOf(0L), false), Arguments.of(Double.valueOf(-0.5), true));
    }

    private static Stream<Arguments> unsupportedStringValues() {
        return Stream.of(Arguments.of("yes"), Arguments.of("maybe"));
    }

    @Test
    void toAtomicBoolean_fromBooleanTrue_returnsTrue() {
        Boolean source = Boolean.TRUE;
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, realConverter);
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should contain true for Boolean.TRUE input");
    }

    @Test
    void toAtomicBoolean_fromBooleanFalse_returnsFalse() {
        Boolean source = Boolean.FALSE;
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, realConverter);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should contain false for Boolean.FALSE input");
    }

    @Test
    void toAtomicBoolean_fromStringTrue_returnsTrue() {
        String source = "true";
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, realConverter);
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should contain true for string \"true\"");
    }

    @Test
    void toAtomicBoolean_fromStringFalse_returnsFalse() {
        String source = "false";
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, realConverter);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should contain false for string \"false\"");
    }

    @Test
    void toAtomicBoolean_fromIntegerOne_returnsTrue() {
        Integer source = 1;
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, realConverter);
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should contain true for integer 1");
    }

    @Test
    void toAtomicBoolean_fromIntegerZero_returnsFalse() {
        Integer source = 0;
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, realConverter);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should contain false for integer 0");
    }

    @Test
    void toAtomicBoolean_fromNull_returnsFalse() {
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(null, realConverter);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should contain false when source is null");
    }

    @ParameterizedTest(name = "Numeric {0} should be converted to {1}")
    @MethodSource("numericInputs")
    void toAtomicBoolean_numericValues(Object source, boolean expected) {
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(source, realConverter);
        assertNotNull(result);
        assertEquals(expected, result.get(), () -> "AtomicBoolean should contain " + expected + " for source " + source);
    }

    @ParameterizedTest(name = "Unsupported string \"{0}\" should cause IllegalArgumentException")
    @MethodSource("unsupportedStringValues")
    void toAtomicBoolean_unsupportedStringValues(String source) {
        assertThrows(IllegalArgumentException.class, () -> CharacterConversions.toAtomicBoolean(source, realConverter), "Expected IllegalArgumentException for unsupported string value");
    }

    @Test
    void toAtomicBoolean_customConverterReturnsTrue() {
        // Arrange – mock converter returns Boolean.TRUE irrespective of input
        when(mockConverter.convert(any(), eq(Boolean.class))).thenReturn(Boolean.TRUE);
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(new CustomObj(), mockConverter);
        assertNotNull(result);
        assertTrue(result.get(), "Result should be true when custom converter returns Boolean.TRUE");
    }

    @Test
    void toAtomicBoolean_customConverterReturnsFalse() {
        when(mockConverter.convert(any(), eq(Boolean.class))).thenReturn(Boolean.FALSE);
        AtomicBoolean result = CharacterConversions.toAtomicBoolean(new CustomObj(), mockConverter);
        assertNotNull(result);
        assertFalse(result.get(), "Result should be false when custom converter returns Boolean.FALSE");
    }

    @Test
    void toAtomicBoolean_customConverterThrowsConversionException() {
        // The library defines ConversionException – we assume it exists.
        ConversionException conversionException = new ConversionException("conversion failed");
        when(mockConverter.convert(any(), eq(Boolean.class))).thenThrow(conversionException);
        ConversionException thrown = assertThrows(ConversionException.class, () -> CharacterConversions.toAtomicBoolean(new CustomObj(), mockConverter), "ConversionException should be propagated");
        assertSame(conversionException, thrown, "Same exception instance should be re‑thrown");
    }

    @Test
    void toAtomicBoolean_customConverterThrowsUncheckedException() {
        IllegalStateException runtime = new IllegalStateException("illegal state");
        when(mockConverter.convert(any(), eq(Boolean.class))).thenThrow(runtime);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> CharacterConversions.toAtomicBoolean(new CustomObj(), mockConverter), "Unchecked exception should be propagated unchanged");
        assertSame(runtime, thrown, "Same runtime exception instance should be re‑thrown");
    }

    @Test
    void toAtomicBoolean_customConverterReturnsNonBoolean() {
        // Returning an Integer should cause a ClassCastException when the library casts to Boolean
        when(mockConverter.convert(any(), eq(Boolean.class))).thenReturn(Integer.valueOf(5));
        assertThrows(ClassCastException.class, () -> CharacterConversions.toAtomicBoolean(new CustomObj(), mockConverter), "ClassCastException expected when converter returns non‑Boolean");
    }

    @Test
    void toAtomicBoolean_customConverterReturnsNull() {
        // Returning null leads to NPE during unboxing to primitive boolean
        when(mockConverter.convert(any(), eq(Boolean.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> CharacterConversions.toAtomicBoolean(new CustomObj(), mockConverter), "NullPointerException expected when converter returns null");
    }

    @Test
    void toAtomicBoolean_unsupportedObjectWithoutConverter() {
        // no special handling in the library
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> CharacterConversions.toAtomicBoolean(source, realConverter), "IllegalArgumentException expected for completely unsupported source");
    }
}
