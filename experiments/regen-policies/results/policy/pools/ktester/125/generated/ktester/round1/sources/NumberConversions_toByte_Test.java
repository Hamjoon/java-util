package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toByte_Test {

    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (required when not using constructor injection)
        MockitoAnnotations.openMocks(this);
        // The mockConverter is not used by NumberConversions.toByte, but we provide it to satisfy the method signature
        reset(mockConverter);
    }

    private static Stream<Arguments> validNumberConversions() {
        return Stream.of(// (byte)5 → 5
        Arguments.of((byte) 5, (byte) 5), // (short)200 → -56 (200 - 256)
        Arguments.of((short) 200, (byte) -56), // (int)12345 → 57 (12345 % 256)
        Arguments.of(12345, (byte) 57), // (long)256L → 0
        Arguments.of(256L, (byte) 0), // (float)12.9f → 12 (fraction truncated)
        Arguments.of(12.9f, (byte) 12), // (double)-3.7 → -3 (fraction truncated)
        Arguments.of(-3.7, (byte) -3), // new BigDecimal("127") → 127
        Arguments.of(new BigDecimal("127"), (byte) 127), // new BigInteger("-129") → 127 (-129 + 256)
        Arguments.of(new BigInteger("-129"), (byte) 127));
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(// null source → NullPointerException
        Arguments.of(null, NullPointerException.class), // non‑Number (String) → ClassCastException
        Arguments.of("string", ClassCastException.class));
    }

    private static Stream<Arguments> additionalEdgeCases() {
        return Stream.of(// (byte)-128 → -128 (minimum)
        Arguments.of((byte) -128, (byte) -128), // (byte)127 → 127 (maximum)
        Arguments.of((byte) 127, (byte) 127), // (short)-32768 → 0  (-32768 % 256 == 0)
        Arguments.of((short) -32768, (byte) 0), // (int)-1 → -1
        Arguments.of(-1, (byte) -1), // (long)Long.MAX_VALUE → -1 (mod 256)
        Arguments.of(Long.MAX_VALUE, (byte) -1), // (float)-0.0f → 0 (negative zero becomes 0)
        Arguments.of(-0.0f, (byte) 0), // (double)255.999 → -1 (truncates to 255 → -1 as byte)
        Arguments.of(255.999, (byte) -1), // new BigDecimal("-128") → -128
        Arguments.of(new BigDecimal("-128"), (byte) -128), // new BigDecimal("128") → -128 (wraps)
        Arguments.of(new BigDecimal("128"), (byte) -128), // new BigInteger("-256") → 0
        Arguments.of(new BigInteger("-256"), (byte) 0), // new BigInteger("256") → 0
        Arguments.of(new BigInteger("256"), (byte) 0), // large BigInteger reduced modulo 256 → 2
        Arguments.of(new BigInteger("12345678901234567890"), (byte) 2));
    }

    @Test
    void toByte_WithByteReturnsSameValue() {
        Byte source = (byte) 10;
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) 10, result);
    }

    @Test
    void toByte_WithIntegerTruncatesToByte() {
        // 130 -> -126 when cast to byte
        Integer source = 130;
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) -126, result);
    }

    @Test
    void toByte_WithDoubleTruncatesFraction() {
        // byteValue() drops the fraction, yielding 12
        Double source = 12.9;
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) 12, result);
    }

    @Test
    void toByte_WithBigIntegerWrapsAround() {
        // 255 -> -1 as a byte
        java.math.BigInteger source = new java.math.BigInteger("255");
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) -1, result);
    }

    @Test
    void toByte_WithBigDecimalTruncatesAndWraps() {
        // 300 -> 44 (300 - 256)
        java.math.BigDecimal source = new java.math.BigDecimal("300.75");
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) 44, result);
    }

    @Test
    void toByte_WithNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toByte(null, mockConverter));
    }

    /* -------------------------------------------------------------
     *  Group: Valid Number conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → expected byte={1}")
    @MethodSource("validNumberConversions")
    void toByte_ValidNumbers(Object from, byte expected) {
        byte result = NumberConversions.toByte(from, null);
        assertEquals(expected, result, () -> "Conversion of " + from + " should yield byte value " + expected);
    }

    /* -------------------------------------------------------------
     *  Group: Invalid inputs
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → throws {1}")
    @MethodSource("invalidInputs")
    void toByte_InvalidInputs(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toByte(from, null), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }

    /* -------------------------------------------------------------
     *  Group: Valid Number conversions - Additional edge cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → expected byte={1}")
    @MethodSource("additionalEdgeCases")
    void toByte_AdditionalEdgeCases(Object from, byte expected) {
        byte result = NumberConversions.toByte(from, null);
        assertEquals(expected, result, () -> "Edge case conversion of " + from + " should yield " + expected);
    }

    /* -------------------------------------------------------------
     *  Group: Custom Number implementation throwing exception
     * ------------------------------------------------------------- */
    @Test
    void toByte_CustomNumberThrowsException() {
        Number badNumber = new Number() {

            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 0L;
            }

            @Override
            public float floatValue() {
                return 0f;
            }

            @Override
            public double doubleValue() {
                return 0d;
            }

            @Override
            public byte byteValue() {
                throw new IllegalStateException("byteValue not supported");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> NumberConversions.toByte(badNumber, null), "Custom Number implementation should propagate IllegalStateException");
        assertEquals("byteValue not supported", ex.getMessage());
    }
}
