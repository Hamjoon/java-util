package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toInt_Test {

    /**
     * Mocked {@link Converter} – not used by the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple {@link Number} subclass used for custom tests.
     * It stores a long value and delegates the numeric conversions to it.
     */
    private static class CustomNumber extends Number {

        private final long value;

        CustomNumber(long value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public double doubleValue() {
            return value;
        }
    }

    /**
     * Provides a stream of arguments where the conversion is expected to succeed.
     * Each argument consists of:
     * <ul>
     *   <li>the source object (Number implementation)</li>
     *   <li>the expected int result after {@code intValue()}</li>
     *   <li>a description for debugging purposes</li>
     * </ul>
     */
    static Stream<org.junit.jupiter.params.provider.Arguments> successfulCases() {
        return Stream.of(// Plain Integer
        org.junit.jupiter.params.provider.Arguments.of(42, 42, "Integer 42"), // Long exceeding int range – truncates to low 32 bits
        org.junit.jupiter.params.provider.Arguments.of(1234567890123L, (int) 1234567890123L, "Long 1234567890123L overflow"), // Double with fraction – truncates toward zero
        org.junit.jupiter.params.provider.Arguments.of(3.9, 3, "Double 3.9"), // BigDecimal with fraction – truncates
        org.junit.jupiter.params.provider.Arguments.of(new BigDecimal("123.456"), 123, "BigDecimal 123.456"), // Byte value
        org.junit.jupiter.params.provider.Arguments.of((byte) 7, 7, "Byte 7"), // Short negative
        org.junit.jupiter.params.provider.Arguments.of((short) -5, -5, "Short -5"), // Float with fraction – truncates
        org.junit.jupiter.params.provider.Arguments.of(2.5f, 2, "Float 2.5f"), // Long within int range
        org.junit.jupiter.params.provider.Arguments.of(123L, 123, "Long 123L"), // Long.MAX_VALUE – low 32 bits are all 1s => -1
        org.junit.jupiter.params.provider.Arguments.of(Long.MAX_VALUE, -1, "Long.MAX_VALUE"), // BigInteger 2^32+1 => low 32 bits = 1
        org.junit.jupiter.params.provider.Arguments.of(new BigInteger("4294967297"), 1, "BigInteger 2^32+1"), // BigInteger 2^32-1 => low 32 bits = -1
        org.junit.jupiter.params.provider.Arguments.of(new BigInteger("4294967295"), -1, "BigInteger 2^32-1"), // AtomicInteger
        org.junit.jupiter.params.provider.Arguments.of(AtomicInteger.valueOf(56), 56, "AtomicInteger 56"), // AtomicLong overflow – same truncation as Long
        org.junit.jupiter.params.provider.Arguments.of(AtomicLong.valueOf(1234567890123L), (int) 1234567890123L, "AtomicLong overflow"), // Float.NaN → 0
        org.junit.jupiter.params.provider.Arguments.of(Float.NaN, 0, "Float.NaN"), // Float.POSITIVE_INFINITY → Integer.MAX_VALUE
        org.junit.jupiter.params.provider.Arguments.of(Float.POSITIVE_INFINITY, Integer.MAX_VALUE, "Float.POSITIVE_INFINITY"), // Float.NEGATIVE_INFINITY → Integer.MIN_VALUE
        org.junit.jupiter.params.provider.Arguments.of(Float.NEGATIVE_INFINITY, Integer.MIN_VALUE, "Float.NEGATIVE_INFINITY"), // Double.NaN → 0
        org.junit.jupiter.params.provider.Arguments.of(Double.NaN, 0, "Double.NaN"), // Double.POSITIVE_INFINITY → Integer.MAX_VALUE
        org.junit.jupiter.params.provider.Arguments.of(Double.POSITIVE_INFINITY, Integer.MAX_VALUE, "Double.POSITIVE_INFINITY"), // Double.NEGATIVE_INFINITY → Integer.MIN_VALUE
        org.junit.jupiter.params.provider.Arguments.of(Double.NEGATIVE_INFINITY, Integer.MIN_VALUE, "Double.NEGATIVE_INFINITY"), // Short.MAX_VALUE
        org.junit.jupiter.params.provider.Arguments.of(Short.MAX_VALUE, 32767, "Short.MAX_VALUE"), // Short.MIN_VALUE
        org.junit.jupiter.params.provider.Arguments.of(Short.MIN_VALUE, -32768, "Short.MIN_VALUE"), // Byte.MIN_VALUE
        org.junit.jupiter.params.provider.Arguments.of(Byte.valueOf((byte) -128), -128, "Byte.MIN_VALUE"), // Custom Number subclass overriding intValue()
        org.junit.jupiter.params.provider.Arguments.of(new CustomNumber(0) {

            @Override
            public int intValue() {
                return 42;
            }
        }, 42, "CustomNumber overriding intValue() → 42"), // Custom Number implementation that returns a constant via intValue()
        org.junit.jupiter.params.provider.Arguments.of(new CustomNumber(99), 99, "CustomNumber with stored value 99"));
    }

    /**
     * Supplies cases where the method is expected to throw an exception.
     * Each argument consists of:
     * <ul>
     *   <li>the source object (may be {@code null} or non‑Number)</li>
     *   <li>the expected exception class</li>
     *   <li>a description for debugging</li>
     * </ul>
     */
    static Stream<org.junit.jupiter.params.provider.Arguments> exceptionCases() {
        return Stream.of(// Null source → NullPointerException
        org.junit.jupiter.params.provider.Arguments.of(null, NullPointerException.class, "null source"), // Non‑Number (String) → ClassCastException
        org.junit.jupiter.params.provider.Arguments.of("123", ClassCastException.class, "String source"), // Custom Number whose intValue() throws IllegalStateException
        org.junit.jupiter.params.provider.Arguments.of(new Number() {

            @Override
            public int intValue() {
                throw new IllegalStateException("intValue failure");
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
        }, IllegalStateException.class, "Number throwing from intValue()"));
    }

    @Test
    void toInt_withInteger_returnsExactValue() {
        Integer src = 42;
        int result = NumberConversions.toInt(src, mockConverter);
        assertEquals(42, result);
    }

    @Test
    void toInt_withLongTruncatesToInt() {
        // larger than int range
        Long src = 1234567890123L;
        int result = NumberConversions.toInt(src, mockConverter);
        assertEquals((int) (long) src, result);
    }

    @Test
    void toInt_withShortReturnsCorrectInt() {
        Short src = 7;
        int result = NumberConversions.toInt(src, mockConverter);
        assertEquals(7, result);
    }

    @Test
    void toInt_withByteReturnsCorrectInt() {
        Byte src = 9;
        int result = NumberConversions.toInt(src, mockConverter);
        assertEquals(9, result);
    }

    @Test
    void toInt_withDoubleTruncatesFraction() {
        Double src = 42.99;
        int result = NumberConversions.toInt(src, mockConverter);
        // intValue() truncates toward zero
        assertEquals(42, result);
    }

    @Test
    void toInt_withFloatTruncatesFraction() {
        Float src = -13.7f;
        int result = NumberConversions.toInt(src, mockConverter);
        assertEquals(-13, result);
    }

    @Test
    void toInt_withBigDecimalTruncatesFraction() {
        BigDecimal src = new BigDecimal("123.456");
        int result = NumberConversions.toInt(src, mockConverter);
        assertEquals(123, result);
    }

    @Test
    void toInt_withBigIntegerReturnsExactIntWhenInRange() {
        // Integer.MAX_VALUE
        BigInteger src = new BigInteger("2147483647");
        int result = NumberConversions.toInt(src, mockConverter);
        assertEquals(Integer.MAX_VALUE, result);
    }

    @Test
    void toInt_withNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toInt(null, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulCases")
    void toInt_successfulConversions(Object source, int expected, String description) {
        int result = NumberConversions.toInt(source, mockConverter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("exceptionCases")
    void toInt_exceptionalConversions(Object source, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> NumberConversions.toInt(source, mockConverter), description);
    }
}
