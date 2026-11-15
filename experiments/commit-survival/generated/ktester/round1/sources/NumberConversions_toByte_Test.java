package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toByte_Test {

    @Mock
    private Converter // injected by Mockito
    mockConverter;

    /**
     * Helper {@link Number} implementation whose {@code byteValue()} method
     * throws a supplied exception type.  Used to verify that {@code toByte}
     * does not catch or wrap exceptions from the underlying {@link Number}.
     */
    private static class ThrowingNumber extends Number {

        private final Class<? extends RuntimeException> exceptionClass;

        ThrowingNumber(Class<? extends RuntimeException> exceptionClass) {
            this.exceptionClass = exceptionClass;
        }

        @Override
        public int intValue() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long longValue() {
            throw new UnsupportedOperationException();
        }

        @Override
        public float floatValue() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double doubleValue() {
            throw new UnsupportedOperationException();
        }

        @Override
        public byte byteValue() {
            try {
                throw exceptionClass.getConstructor(String.class).newInstance("byteValue exception");
            } catch (ReflectiveOperationException e) {
                // Wrap reflection problems into a RuntimeException – this should never happen in the test.
                throw new RuntimeException(e);
            }
        }

        @Override
        public short shortValue() {
            throw new UnsupportedOperationException();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (required when not using constructor injection).
        MockitoAnnotations.openMocks(this);
    }

    private static Stream<Arguments> basicNumericConversions() {
        return Stream.of(// Byte within range
        Arguments.of(Byte.valueOf((byte) 10), (byte) 10, "Converts a Byte instance within the byte range; should return the same value."), // Short exceeding byte range
        Arguments.of(Short.valueOf((short) 200), (byte) -56, "Converts a Short whose value exceeds the signed byte range; byteValue() truncates to -56 (200‑256)."), // Integer overflow
        Arguments.of(Integer.valueOf(130), (byte) -126, "Converts an Integer larger than 127; overflow wraps around to -126 (130‑256)."), // Negative Long
        Arguments.of(Long.valueOf(-130L), (byte) 126, "Converts a negative Long; -130 modulo 256 yields 126 as a signed byte."), // Float with fraction
        Arguments.of(Float.valueOf(12.34f), (byte) 12, "Converts a Float; fractional part is discarded, leaving the integer part 12."), // Double with fraction
        Arguments.of(Double.valueOf(-5.99), (byte) -5, "Converts a Double; fractional part is discarded, resulting in -5."), // BigDecimal > byte range
        Arguments.of(new BigDecimal("123"), (byte) -133, "Converts a BigDecimal whose value exceeds the byte range; 123‑256 = -133 after truncation."), // BigInteger exactly 256
        Arguments.of(new BigInteger("256"), (byte) 0, "Converts a BigInteger that is exactly 256; byteValue() wraps to 0."));
    }

    private static Stream<Arguments> edgeNumericValues() {
        return Stream.of(// Minimum byte
        Arguments.of(Byte.valueOf((byte) -128), (byte) -128, "Minimum byte value; should remain unchanged."), // Maximum byte
        Arguments.of(Byte.valueOf((byte) 127), (byte) 127, "Maximum byte value; should remain unchanged."), // Negative short wraps to positive byte
        Arguments.of(Short.valueOf((short) -200), (byte) 56, "Negative short that wraps into the byte range (-200 → 56)."), // Integer multiple of 256
        Arguments.of(Integer.valueOf(1024), (byte) 0, "Integer that is a multiple of 256; byteValue() yields 0."), // Long exactly 256
        Arguments.of(Long.valueOf(256L), (byte) 0, "Long exactly equal to 256; wraps to 0."), // Float NaN
        Arguments.of(Float.valueOf(Float.NaN), (byte) 0, "NaN converted to byte; casting NaN to int yields 0, thus byte 0."), // Float +Infinity
        Arguments.of(Float.valueOf(Float.POSITIVE_INFINITY), (byte) 127, "Positive infinity casts to Integer.MAX_VALUE then to byte (127)."), // Float -Infinity
        Arguments.of(Float.valueOf(Float.NEGATIVE_INFINITY), (byte) 0, "Negative infinity casts to Integer.MIN_VALUE which modulo 256 is 0."), // Double NaN
        Arguments.of(Double.valueOf(Double.NaN), (byte) 0, "NaN double behaves like NaN float; result is 0."), // BigDecimal with fractional part (positive)
        Arguments.of(new BigDecimal("12.9"), (byte) 12, "BigDecimal with fractional part; truncates toward zero before byte conversion."), // BigDecimal with fractional part (negative)
        Arguments.of(new BigDecimal("-12.9"), (byte) -12, "Negative BigDecimal with fraction; truncates toward zero."), // Negative BigInteger wraps
        Arguments.of(new BigInteger("-300"), (byte) 212, "Negative BigInteger wraps into byte range (-300 → 212)."));
    }

    @Test
    @DisplayName("toByte returns the same value when the source is a Byte")
    void toByte_givenByte_returnsSameByte() {
        // given
        Byte source = Byte.valueOf((byte) 7);
        // when
        byte result = NumberConversions.toByte(source, mockConverter);
        // then
        assertEquals((byte) 7, result, "Byte value should be unchanged");
    }

    @Test
    @DisplayName("toByte returns the low‑order byte of an Integer")
    void toByte_givenInteger_returnsByteValue() {
        // given
        // 130 -> (byte)130 == -126
        Integer source = Integer.valueOf(130);
        // when
        byte result = NumberConversions.toByte(source, mockConverter);
        // then
        assertEquals((byte) -126, result, "Byte value should be the low‑order 8 bits of the integer");
    }

    @Test
    @DisplayName("toByte throws NullPointerException when source is null")
    void toByte_givenNull_throwsNPE() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> NumberConversions.toByte(source, mockConverter), "Calling toByte with null should throw NullPointerException");
    }

    @Test
    @DisplayName("toByte throws ClassCastException when source is not a Number")
    void toByte_givenNonNumber_throwsClassCastException() {
        // given
        String source = "not a number";
        // when / then
        assertThrows(ClassCastException.class, () -> NumberConversions.toByte(source, mockConverter), "Calling toByte with a non‑Number should throw ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for normal numeric conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("basicNumericConversions")
    void toByte_basicNumericConversions(Object from, byte expected, String description) {
        byte result = NumberConversions.toByte(from, mockConverter);
        assertEquals(expected, result, description);
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for edge numeric values
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("edgeNumericValues")
    void toByte_edgeNumericValues(Object from, byte expected, String description) {
        byte result = NumberConversions.toByte(from, mockConverter);
        assertEquals(expected, result, description);
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Tests for error handling (null, wrong type, unsupported Number)
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("toByte throws NullPointerException when source is null")
    void toByte_nullSource_throwsNPE() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toByte(null, mockConverter), "Null 'from' argument should cause NPE when cast to Number.");
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toByte throws ClassCastException for non‑Number source")
    void toByte_nonNumberSource_throwsCCE() {
        assertThrows(ClassCastException.class, () -> NumberConversions.toByte("123", mockConverter), "Non‑Number object (String) cannot be cast; should throw CCE.");
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toByte throws ClassCastException for AtomicInteger (does not implement Number)")
    void toByte_atomicIntegerSource_throwsCCE() {
        assertThrows(ClassCastException.class, () -> NumberConversions.toByte(new java.util.concurrent.atomic.AtomicInteger(5), mockConverter), "AtomicInteger does not implement Number; casting fails.");
        verifyNoInteractions(mockConverter);
    }

    /* -------------------------------------------------------------
     *  Tests for custom Number subclasses that throw exceptions
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("toByte propagates IllegalStateException from custom Number")
    void toByte_customNumber_throwsIllegalStateException() {
        Number throwing = new ThrowingNumber(IllegalStateException.class);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> NumberConversions.toByte(throwing, mockConverter), "Exception from byteValue() should propagate unchanged.");
        assertEquals("byteValue exception", ex.getMessage());
        verifyNoInteractions(mockConverter);
    }

    @Test
    @DisplayName("toByte propagates ArithmeticException from custom Number")
    void toByte_customNumber_throwsArithmeticException() {
        Number throwing = new ThrowingNumber(ArithmeticException.class);
        ArithmeticException ex = assertThrows(ArithmeticException.class, () -> NumberConversions.toByte(throwing, mockConverter), "Exception from byteValue() should propagate unchanged.");
        assertEquals("byteValue exception", ex.getMessage());
        verifyNoInteractions(mockConverter);
    }
}
