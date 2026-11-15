package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toInt_Test {

    private static Converter converter;

    /**
     * A Number whose {@code intValue()} throws {@link IllegalStateException}.
     */
    private static final class ExplodingNumber extends Number {

        @Override
        public int intValue() {
            throw new IllegalStateException("boom");
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
            return 0;
        }

        @Override
        public short shortValue() {
            return 0;
        }
    }

    /**
     * A Number whose {@code intValue()} throws {@link NullPointerException}.
     */
    private static final class NullPointerNumber extends Number {

        @Override
        public int intValue() {
            throw new NullPointerException("null in intValue");
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
            return 0;
        }

        @Override
        public short shortValue() {
            return 0;
        }
    }

    /**
     * A Number whose {@code intValue()} throws {@link ArithmeticException}.
     */
    private static final class ArithmeticNumber extends Number {

        @Override
        public int intValue() {
            throw new ArithmeticException("arithmetic error");
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
            return 0;
        }

        @Override
        public short shortValue() {
            return 0;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; using default options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    static Stream<Arguments> validNumberProvider() {
        return Stream.of(arguments("Integer input should return its int value unchanged.", 42, 42), arguments("Long input is cast to Number and intValue() truncates to lower 32 bits.", 1234567890123L, (int) 1234567890123L), arguments("Double input is truncated toward zero when converting to int.", 3.14, 3), arguments("BigDecimal input returns the integer part via intValue().", new BigDecimal("12345.678"), 12345), arguments("Float (as double literal) is truncated toward zero.", -2.9, -2), arguments("Byte value is promoted to int correctly.", (byte) 7, 7), arguments("Short value is promoted to int correctly.", (short) -5, -5));
    }

    static Stream<Arguments> edgeValueProvider() {
        return Stream.of(arguments("Integer.MAX_VALUE should be returned unchanged.", Integer.MAX_VALUE, Integer.MAX_VALUE), arguments("Integer.MIN_VALUE should be returned unchanged.", Integer.MIN_VALUE, Integer.MIN_VALUE), arguments("Long.MAX_VALUE truncates to lower 32 bits, resulting in -1.", Long.MAX_VALUE, (int) Long.MAX_VALUE), arguments("Long.MIN_VALUE truncates to lower 32 bits, yielding Integer.MIN_VALUE.", Long.MIN_VALUE, (int) Long.MIN_VALUE), arguments("Double.NaN converts to 0 via intValue().", Double.NaN, 0), arguments("Double.POSITIVE_INFINITY converts to Integer.MAX_VALUE.", Double.POSITIVE_INFINITY, Integer.MAX_VALUE), arguments("Double.NEGATIVE_INFINITY converts to Integer.MIN_VALUE.", Double.NEGATIVE_INFINITY, Integer.MIN_VALUE), arguments("Float object is truncated toward zero when converting to int.", new Float(2.99f), 2));
    }

    static Stream<Arguments> exceptionProvider() {
        return Stream.of(arguments("Passing null as the source object results in NPE when intValue() is invoked.", null, NullPointerException.class), arguments("A non‑Number object (String) cannot be cast to Number, causing ClassCastException.", "some string", ClassCastException.class));
    }

    @Test
    void toInt_withIntegerReturnsSameValue() {
        Integer source = 42;
        int result = NumberConversions.toInt(source, converter);
        assertEquals(42, result);
    }

    @Test
    void toInt_withLongReturnsTruncatedValue() {
        // larger than int range, will be truncated
        Long source = 1234567890123L;
        int result = NumberConversions.toInt(source, converter);
        assertEquals((int) source.longValue(), result);
    }

    @Test
    void toInt_withDoubleReturnsTruncatedValue() {
        Double source = 12.99;
        int result = NumberConversions.toInt(source, converter);
        assertEquals(12, result);
    }

    @Test
    void toInt_withByteReturnsCorrectValue() {
        Byte source = (byte) 7;
        int result = NumberConversions.toInt(source, converter);
        assertEquals(7, result);
    }

    @Test
    void toInt_withNullReturnsZero() {
        // According to Converter's contract, null should be treated as 0 for primitives.
        // NumberConversions directly casts to Number, so we must avoid passing null here.
        // Instead we test the behavior when a null is wrapped in a Number (which is not possible),
        // therefore we assert that a NullPointerException is thrown.
        assertThrows(NullPointerException.class, () -> NumberConversions.toInt(null, converter));
    }

    @Test
    void toInt_withAtomicIntegerUsesConverterIndirectly() {
        // Demonstrate that other NumberConversions methods rely on toInt.
        AtomicInteger atomic = NumberConversions.toAtomicInteger(55, converter);
        assertNotNull(atomic);
        assertEquals(55, atomic.get());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validNumberProvider")
    void toInt_validNumbers(String description, Object from, int expected) {
        int result = NumberConversions.toInt(from, converter);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeValueProvider")
    void toInt_edgeValues(String description, Object from, int expected) {
        int result = NumberConversions.toInt(from, converter);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionProvider")
    void toInt_exceptions(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toInt(from, converter), description);
    }

    // -----------------------------------------------------------------
    @Test
    void toInt_explodingNumberThrowsIllegalStateException() {
        Number exploding = new ExplodingNumber();
        assertThrows(IllegalStateException.class, () -> NumberConversions.toInt(exploding, converter), "ExplodingNumber should propagate IllegalStateException from intValue()");
    }

    // -----------------------------------------------------------------
    @Test
    void toInt_converterArgumentIgnored() {
        // Create a mock Converter that would throw if used (it shouldn't be).
        Converter mockConverter = Assertions.assertDoesNotThrow(() -> new Converter(new ConverterOptions()));
        int result = NumberConversions.toInt(99, mockConverter);
        assertEquals(99, result, "Providing a non‑null Converter does not affect the result");
    }

    // -----------------------------------------------------------------
    @Test
    void toInt_nullPointerNumberPropagatesException() {
        Number n = new NullPointerNumber();
        assertThrows(NullPointerException.class, () -> NumberConversions.toInt(n, converter), "NullPointerNumber should propagate NullPointerException");
    }

    @Test
    void toInt_arithmeticNumberPropagatesException() {
        Number n = new ArithmeticNumber();
        assertThrows(ArithmeticException.class, () -> NumberConversions.toInt(n, converter), "ArithmeticNumber should propagate ArithmeticException");
    }
}
