package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toByte_Test {

    @Mock
    Converter // not used by NumberConversions.toByte but required by signature
    mockConverter;

    /**
     * Supplies all test cases defined in the JSON specification.
     *
     * @return a stream of arguments for the parameterized test
     */
    static Stream<Arguments> provideToByteCases() {
        // Custom Converter used in one test case (the converter is ignored by toByte)
        Converter dummyConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                return null;
            }
        };
        // Custom Number subclass that returns a fixed byte value
        Number customNumberReturning42 = new Number() {

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
                return (byte) 42;
            }
        };
        // Custom Number subclass whose byteValue() throws an exception
        Number customNumberThrowing = new Number() {

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
                throw new IllegalStateException("byteValue error");
            }
        };
        return Stream.of(Arguments.of(Byte.valueOf((byte) 5), (byte) 5, null, "Convert a Byte instance to primitive byte, expecting the same value."), Arguments.of(new Integer(127), (byte) 127, null, "Convert an Integer within byte range; should return its low-order byte."), Arguments.of(new Long(128L), (byte) -128, null, "Convert a Long value that exceeds byte max (127); byteValue() wraps to -128."), Arguments.of(new Double(12.34), (byte) 12, null, "Convert a Double; fractional part is truncated by byteValue()."), Arguments.of(new BigDecimal("-45.6"), (byte) -45, null, "Convert a BigDecimal; truncates toward zero."), Arguments.of(new AtomicInteger(10), (byte) 10, null, "Convert an AtomicInteger (which implements Number) to byte."), Arguments.of(null, NullPointerException.class, null, "Passing null should cause a NullPointerException when casting to Number."), Arguments.of("123", ClassCastException.class, null, "A non‑Number object (String) should cause a ClassCastException."), Arguments.of(Byte.valueOf((byte) -128), (byte) -128, null, "Convert the minimum byte value; should return -128 unchanged."), Arguments.of(new Integer(-129), (byte) 127, null, "Integer less than Byte.MIN_VALUE wraps around; -129 becomes 127 after truncation."), Arguments.of(Short.valueOf((short) -200), (byte) 56, null, "Short value -200 overflows the byte range; low‑order byte is 56."), Arguments.of(Float.valueOf(-12.9f), (byte) -12, null, "Float with fractional part is truncated toward zero; -12.9 becomes -12."), Arguments.of(Double.valueOf(Double.NaN), (byte) 0, null, "NaN converted to byte yields 0 according to JLS casting rules."), Arguments.of(Double.valueOf(Double.POSITIVE_INFINITY), (byte) -1, null, "Positive infinity truncates to Integer.MAX_VALUE, whose low‑order byte is -1."), Arguments.of(new BigInteger("200"), (byte) -56, null, "BigInteger 200 overflows byte range; low‑order byte is -56."), Arguments.of(new AtomicLong(300L), (byte) 44, null, "AtomicLong 300 wraps to 44 (300‑256)."), Arguments.of(customNumberReturning42, (byte) 42, null, "Custom Number subclass overrides byteValue() to return 42; conversion should use that value."), Arguments.of(Boolean.TRUE, ClassCastException.class, null, "Boolean does not implement Number; casting should throw ClassCastException."), Arguments.of(new Integer(100), (byte) 100, dummyConverter, "A non‑null Converter is ignored by toByte; conversion of Integer 100 should still return 100."), Arguments.of(customNumberThrowing, IllegalStateException.class, null, "Custom Number subclass whose byteValue() throws IllegalStateException; toByte should propagate the exception."));
    }

    @Test
    void toByte_FromByte_ReturnsSameValue() {
        Byte source = (byte) 5;
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) 5, result);
    }

    @Test
    void toByte_FromShort_ReturnsTruncatedValue() {
        // 200 -> 0xC8 -> -56 as a signed byte
        Short source = (short) 200;
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) 200, result);
    }

    @Test
    void toByte_FromInteger_ReturnsTruncatedValue() {
        Integer source = 123;
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) 123, result);
    }

    @Test
    void toByte_FromLong_ReturnsTruncatedValue() {
        // 300 -> 0x12C -> 44 as a signed byte
        Long source = 300L;
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) 300, result);
    }

    @Test
    void toByte_FromFloat_ReturnsTruncatedValue() {
        // truncates toward zero
        Float source = 12.9f;
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) 12, result);
    }

    @Test
    void toByte_FromDouble_ReturnsTruncatedValue() {
        Double source = -5.7;
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) -5, result);
    }

    @Test
    void toByte_FromBigInteger_ReturnsTruncatedValue() {
        BigInteger source = new BigInteger("127");
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) 127, result);
    }

    @Test
    void toByte_FromBigDecimal_ReturnsTruncatedValue() {
        BigDecimal source = new BigDecimal("123.456");
        byte result = NumberConversions.toByte(source, mockConverter);
        assertEquals((byte) 123, result);
    }

    @Test
    void toByte_NullSource_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toByte(null, mockConverter));
    }

    @Test
    void toByte_NonNumberSource_ThrowsClassCastException() {
        String source = "not a number";
        assertThrows(ClassCastException.class, () -> NumberConversions.toByte(source, mockConverter));
    }

    /**
     * Executes the exhaustive set of conversion scenarios for {@code toByte}.
     *
     * @param from        the source object to convert
     * @param expected    either a {@link Byte}/{@link Number} representing the expected result,
     *                    or a {@link Class} of an exception that should be thrown
     * @param converter   the {@link Converter} instance to pass (may be {@code null})
     * @param description textual description of the test case (used as the display name)
     */
    @ParameterizedTest(name = "{3}")
    @MethodSource("provideToByteCases")
    void testToByte(Object from, Object expected, Converter converter, String description) {
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> NumberConversions.toByte(from, converter));
        } else {
            // Expected a numeric result
            byte result = NumberConversions.toByte(from, converter);
            byte expectedByte = ((Number) expected).byteValue();
            assertEquals(expectedByte, result, description);
        }
    }
}
