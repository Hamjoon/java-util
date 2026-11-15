package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toShort_Test {

    @Mock
    private Converter // Converter is not used by the method but required by the signature
    converter;

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// Short → same value
        Arguments.of(Short.valueOf((short) 123), (short) 123, "Convert a Short instance to primitive short; normal path."), // Integer within short range
        Arguments.of(Integer.valueOf(456), (short) 456, "Convert an Integer to short; shortValue() truncates to lower 16 bits."), // Long beyond Short.MAX_VALUE – overflow behaviour
        Arguments.of(Long.valueOf(32768L), (short) -32768, "Convert a Long whose value exceeds Short.MAX_VALUE; expect overflow behavior."), // BigDecimal (implements Number) – truncates fraction
        Arguments.of(BigDecimal.valueOf(12345), (short) 12345, "Convert a BigDecimal (which implements Number) to short."), // Null source – should throw NPE (handled in exception group)
        // Non‑Number source – should throw CCE (handled in exception group)
        // Byte within range
        Arguments.of(Byte.valueOf((byte) 100), (short) 100, "Convert a Byte instance within short range; should return the same numeric value."), // Short.MAX_VALUE
        Arguments.of(Short.valueOf(Short.MAX_VALUE), Short.MAX_VALUE, "Convert Short.MAX_VALUE; verifies upper bound of primitive short."), // Short.MIN_VALUE
        Arguments.of(Short.valueOf(Short.MIN_VALUE), Short.MIN_VALUE, "Convert Short.MIN_VALUE; verifies lower bound of primitive short."), // Integer outside range – overflow
        Arguments.of(Integer.valueOf(-40000), (short) 25536, "Integer value outside short range; shortValue() wraps according to two‑complement overflow."), // Float with fractional part – truncates
        Arguments.of(Float.valueOf(12345.67f), (short) 12345, "Float with fractional part; shortValue() truncates the decimal portion."), // Double negative with fractional part – truncates toward zero
        Arguments.of(Double.valueOf(-12345.99), (short) -12345, "Negative Double with fractional part; truncation toward zero."), // BigInteger larger than Short.MAX_VALUE – overflow
        Arguments.of(BigInteger.valueOf(40000L), (short) -25536, "BigInteger larger than Short.MAX_VALUE; overflow behavior matches (short)40000."), // AtomicInteger – returns its int value as short
        Arguments.of(new AtomicInteger(200), (short) 200, "AtomicInteger implements Number; conversion should return its int value as short."), // AtomicLong beyond range – overflow
        Arguments.of(new AtomicLong(50000L), (short) -15536, "AtomicLong value exceeding short range; verifies overflow handling for atomic numeric types."), // Float.NaN → 0
        Arguments.of(Float.NaN, (short) 0, "Float.NaN cast to short yields 0; tests special floating‑point value handling."), // Double.POSITIVE_INFINITY → -1 (0xFFFF)
        Arguments.of(Double.POSITIVE_INFINITY, (short) -1, "Positive infinity cast to short results in -1 (0xFFFF); confirms Java casting semantics for extreme values."), // Converter that throws if used – should be ignored
        Arguments.of(Integer.valueOf(123), (short) 123, "Non‑null Converter that throws if invoked; toShort should ignore the Converter argument entirely."));
    }

    private static Stream<Arguments> additionalConversionProvider() {
        // Custom Number subclass overriding shortValue()
        Number customNumber = new Number() {

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
            public short shortValue() {
                return (short) 999;
            }
        };
        return Stream.of(Arguments.of(customNumber, (short) 999, "Custom Number subclass overriding shortValue() to return 999; ensures method uses shortValue() of the actual runtime type."), // Custom Converter that throws – we pass it explicitly here
        Arguments.of(Integer.valueOf(123), (short) 123, "Non‑null Converter that throws if invoked; verifies that toShort ignores the converter."));
    }

    private static Stream<Arguments> exceptionProvider() {
        // Null source
        Object nullSource = null;
        // Non‑Number source
        Object nonNumber = "not a number";
        // Number subclass whose shortValue() throws ArithmeticException
        Number throwsArithmetic = new Number() {

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
            public short shortValue() {
                throw new ArithmeticException("shortValue failure");
            }
        };
        // Number subclass whose shortValue() throws NullPointerException
        Number throwsNPE = new Number() {

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
            public short shortValue() {
                throw new NullPointerException("shortValue NPE");
            }
        };
        return Stream.of(Arguments.of(nullSource, NullPointerException.class, "Passing null should cause a NullPointerException when casting to Number."), Arguments.of(nonNumber, ClassCastException.class, "Passing a non‑Number object should result in a ClassCastException."), Arguments.of(throwsArithmetic, ArithmeticException.class, "Custom Number implementation whose shortValue() throws an ArithmeticException; verifies that the exception propagates unchanged."), Arguments.of(throwsNPE, NullPointerException.class, "Custom Number implementation whose shortValue() throws a NullPointerException; ensures that such runtime exceptions are not masked."));
    }

    /* -------------------------------------------------------------
     *  Helper method to create a Converter that throws if its convert method is called.
     * ------------------------------------------------------------- */
    private static Converter throwingConverter() {
        return new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be called");
            }
        };
    }

    @Test
    void toShort_WithShortReturnsSameValue() {
        Short src = 123;
        short result = NumberConversions.toShort(src, converter);
        assertEquals(123, result);
    }

    @Test
    void toShort_WithIntegerWithinRange() {
        Integer src = 12_345;
        short result = NumberConversions.toShort(src, converter);
        assertEquals(12_345, result);
    }

    @Test
    void toShort_WithLongBeyondShortRangeWrapsCorrectly() {
        // 0x11170 -> low 16 bits 0x1170 = 4464
        Long src = 70_000L;
        short result = NumberConversions.toShort(src, converter);
        assertEquals(4_464, result);
    }

    @Test
    void toShort_WithDoubleTruncatesFraction() {
        Double src = 12.99;
        short result = NumberConversions.toShort(src, converter);
        assertEquals(12, result);
    }

    @Test
    void toShort_WithBigDecimalTruncatesFraction() {
        BigDecimal src = new BigDecimal("123.99");
        short result = NumberConversions.toShort(src, converter);
        assertEquals(123, result);
    }

    @Test
    void toShort_WithBigIntegerReturnsExactValue() {
        // max short value
        BigInteger src = new BigInteger("32767");
        short result = NumberConversions.toShort(src, converter);
        assertEquals(32767, result);
    }

    @Test
    void toShort_WithAtomicIntegerReturnsItsValue() {
        AtomicInteger src = new AtomicInteger(42);
        short result = NumberConversions.toShort(src, converter);
        assertEquals(42, result);
    }

    @Test
    void toShort_WithNullThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toShort(null, converter));
    }

    @Test
    void toShort_WithNonNumberThrowsClassCastException() {
        Object src = "not a number";
        assertThrows(ClassCastException.class, () -> NumberConversions.toShort(src, converter));
    }

    /* -------------------------------------------------------------
     *  Normal conversion scenarios (group: "NumberConversions.toShort tests")
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("normalConversionProvider")
    void toShort_NormalConversions(Object from, short expected, String description) {
        short result = NumberConversions.toShort(from, converter);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Additional conversion scenarios (group: "NumberConversions.toShort additional tests")
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("additionalConversionProvider")
    void toShort_AdditionalConversions(Object from, short expected, String description) {
        short result = NumberConversions.toShort(from, converter);
        assertEquals(expected, result, description);
    }

    /* -------------------------------------------------------------
     *  Exception scenarios (group: "NumberConversions.toShort exception tests")
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionProvider")
    void toShort_ExceptionScenarios(Object from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> NumberConversions.toShort(from, converter), description);
    }

    /**
     * Overridden test for the case where a non‑null, throwing {@link Converter}
     * is supplied. The method under test must ignore the converter argument.
     */
    @Test
    void toShort_IgnoresNonNullConverter() {
        Converter badConverter = throwingConverter();
        short result = NumberConversions.toShort(Integer.valueOf(123), badConverter);
        assertEquals(123, result, "Method must ignore the provided Converter and perform the conversion directly.");
    }
}
