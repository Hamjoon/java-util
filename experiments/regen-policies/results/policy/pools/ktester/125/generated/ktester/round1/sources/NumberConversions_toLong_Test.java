package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLong_Test {

    /**
     * The {@link Converter} dependency – it is not used by the method under test, but must be provided.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple {@link Number} subclass that returns a predefined long value.
     */
    private static final class MyNumber extends Number {

        private final long value;

        MyNumber(long value) {
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
     * {@link Number} implementation whose {@code longValue()} deliberately throws an unchecked exception.
     */
    private static final class ThrowingNumber extends Number {

        @Override
        public int intValue() {
            throw new IllegalStateException("intValue not supported");
        }

        @Override
        public long longValue() {
            throw new IllegalStateException("longValue deliberately throws");
        }

        @Override
        public float floatValue() {
            throw new IllegalStateException("floatValue not supported");
        }

        @Override
        public double doubleValue() {
            throw new IllegalStateException("doubleValue not supported");
        }
    }

    private static Stream<Arguments> validNumberConversions() {
        return Stream.of(// Integer
        Arguments.of(Integer.valueOf(42), 42L), // Long
        Arguments.of(Long.valueOf(1234567890123L), 1234567890123L), // Double (fraction truncated)
        Arguments.of(Double.valueOf(42.9), 42L), // Float (fraction truncated)
        Arguments.of(Float.valueOf(123.45f), 123L), // BigDecimal (fraction discarded)
        Arguments.of(new BigDecimal("9876543210.99"), 9876543210L), // BigInteger (fits in long)
        Arguments.of(new BigInteger("999999999999"), 999999999999L), // AtomicLong (extends Number)
        Arguments.of(new AtomicLong(5555L), 5555L), // Custom Number subclass
        Arguments.of(new MyNumber(777L), 777L));
    }

    private static Stream<Arguments> boundaryAndSpecialValues() {
        return Stream.of(// Byte minimum
        Arguments.of(Byte.valueOf((byte) -128), -128L), // Short maximum
        Arguments.of(Short.valueOf((short) 32767), 32767L), // AtomicInteger (negative)
        Arguments.of(new AtomicInteger(-9999), -9999L), // Double.NaN → 0
        Arguments.of(Double.NaN, 0L), // Double.POSITIVE_INFINITY → Long.MAX_VALUE
        Arguments.of(Double.POSITIVE_INFINITY, Long.MAX_VALUE), // Double.NEGATIVE_INFINITY → Long.MIN_VALUE
        Arguments.of(Double.NEGATIVE_INFINITY, Long.MIN_VALUE), // BigDecimal just above Long.MAX_VALUE – expected saturation to Long.MAX_VALUE
        Arguments.of(new BigDecimal("9223372036854775808"), Long.MAX_VALUE), // BigDecimal just below Long.MIN_VALUE – expected saturation to Long.MIN_VALUE
        Arguments.of(new BigDecimal("-9223372036854775809"), Long.MIN_VALUE), // BigInteger exceeding Long.MAX_VALUE – wraps according to BigInteger.longValue()
        Arguments.of(new BigInteger("9223372036854775808"), Long.MIN_VALUE), // BigInteger below Long.MIN_VALUE – wraps
        Arguments.of(new BigInteger("-9223372036854775809"), Long.MAX_VALUE));
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// Null source
        Arguments.of(null, NullPointerException.class), // Non‑Number object
        Arguments.of("not a number", ClassCastException.class), // Number subclass whose longValue throws an unchecked exception
        Arguments.of(new ThrowingNumber(), IllegalStateException.class));
    }

    @Test
    void toLong_WithInteger_ReturnsLongValue() {
        // given
        Integer source = Integer.valueOf(42);
        // when
        long result = NumberConversions.toLong(source, mockConverter);
        // then
        assertEquals(42L, result);
    }

    @Test
    void toLong_WithLong_ReturnsSameLong() {
        // given
        Long source = Long.valueOf(123L);
        // when
        long result = NumberConversions.toLong(source, mockConverter);
        // then
        assertEquals(123L, result);
    }

    @Test
    void toLong_WithDouble_TruncatesFraction() {
        // given
        // doubleValue() -> 5.9, longValue() truncates to 5
        Double source = Double.valueOf(5.9);
        // when
        long result = NumberConversions.toLong(source, mockConverter);
        // then
        assertEquals(5L, result);
    }

    @Test
    void toLong_WithBigDecimal_ConvertsCorrectly() {
        // given
        BigDecimal source = new BigDecimal("12345");
        // when
        long result = NumberConversions.toLong(source, mockConverter);
        // then
        assertEquals(12345L, result);
    }

    @Test
    void toLong_WithNull_ThrowsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> NumberConversions.toLong(source, mockConverter));
    }

    @Test
    void toLong_WithNonNumber_ThrowsClassCastException() {
        // given
        String source = "not a number";
        // when / then
        assertThrows(ClassCastException.class, () -> NumberConversions.toLong(source, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] toLong({0}) => {1}")
    @MethodSource("validNumberConversions")
    void toLong_ValidNumberConversions(Object from, long expected) {
        long result = NumberConversions.toLong(from, null);
        assertEquals(expected, result, () -> "Expected conversion of " + from.getClass().getSimpleName() + " to be " + expected);
    }

    @ParameterizedTest(name = "[{index}] toLong({0}) => {1}")
    @MethodSource("boundaryAndSpecialValues")
    void toLong_BoundaryAndSpecialValues(Object from, long expected) {
        long result = NumberConversions.toLong(from, null);
        assertEquals(expected, result, () -> "Boundary conversion of " + from.getClass().getSimpleName() + " failed");
    }

    @ParameterizedTest(name = "[{index}] toLong({0}) throws {1}")
    @MethodSource("exceptionCases")
    void toLong_ExceptionHandling(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toLong(from, null), () -> "Expected exception " + expectedException.getSimpleName() + " was not thrown");
    }
}
