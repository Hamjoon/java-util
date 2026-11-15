package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLong_Test {

    @Mock
    private Converter // Converter is not used by toLong, but required by signature
    mockConverter;

    /**
     * Simple subclass of {@link Number} that returns a predefined long value.
     */
    private static final class CustomNumber extends Number {

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

        @Override
        public byte byteValue() {
            return (byte) value;
        }

        @Override
        public short shortValue() {
            return (short) value;
        }
    }

    /**
     * Subclass of {@link Number} whose {@code longValue()} deliberately throws
     * a supplied runtime exception. Used to verify that {@code toLong} does not
     * swallow exceptions from the {@code Number} implementation.
     */
    private static final class ThrowingNumber extends Number {

        private final RuntimeException toThrow;

        ThrowingNumber(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public int intValue() {
            throw toThrow;
        }

        @Override
        public long longValue() {
            throw toThrow;
        }

        @Override
        public float floatValue() {
            throw toThrow;
        }

        @Override
        public double doubleValue() {
            throw toThrow;
        }

        @Override
        public byte byteValue() {
            throw toThrow;
        }

        @Override
        public short shortValue() {
            throw toThrow;
        }
    }

    private static Stream<Arguments> successProvider() {
        return Stream.of(// basic numeric types
        Arguments.of(Integer.valueOf(42), 42L), Arguments.of(Long.valueOf(1234567890123L), 1234567890123L), Arguments.of(Double.valueOf(3.14159), 3L), Arguments.of(Float.valueOf(271.828f), 271L), // BigInteger / BigDecimal handling
        Arguments.of(new BigInteger("9876543210987654321"), -5392229872364012159L), Arguments.of(new BigDecimal("12345.6789"), 12345L), // extreme long values
        Arguments.of(Long.MAX_VALUE, Long.MAX_VALUE), Arguments.of(Long.MIN_VALUE, Long.MIN_VALUE), // additional primitive wrappers
        Arguments.of(Short.valueOf((short) 123), 123L), Arguments.of(Byte.valueOf((byte) -45), -45L), // BigInteger near the limits
        Arguments.of(new BigInteger("9223372036854775806"), 9223372036854775806L), Arguments.of(new BigInteger("-9223372036854775809"), Long.MAX_VALUE), // BigDecimal with negative fraction
        Arguments.of(new BigDecimal("-12345.9876"), -12345L), // special double values
        Arguments.of(Double.NaN, 0L), Arguments.of(Double.POSITIVE_INFINITY, Long.MAX_VALUE), Arguments.of(Double.NEGATIVE_INFINITY, Long.MIN_VALUE), // negative double with fraction
        Arguments.of(Double.valueOf(-3.9), -3L), // custom Number subclass
        Arguments.of(new CustomNumber(777), 777L), // converter argument is non‑null but ignored
        Arguments.of(Integer.valueOf(0), 0L), Arguments.of(Long.valueOf(Long.MAX_VALUE), Long.MAX_VALUE));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// null source
        Arguments.of(null, NullPointerException.class), // non‑Number source
        Arguments.of("not a number", ClassCastException.class), // Atomic types are not Numbers
        Arguments.of(new AtomicInteger(12345), ClassCastException.class), Arguments.of(new AtomicLong(9876543210L), ClassCastException.class), // custom Number that throws from longValue()
        Arguments.of(new ThrowingNumber(new IllegalArgumentException("forced")), IllegalArgumentException.class), Arguments.of(new ThrowingNumber(new UnsupportedOperationException("unsupported")), UnsupportedOperationException.class));
    }

    @Test
    void toLong_withInteger_returnsCorrectLong() {
        Integer source = 42;
        long result = NumberConversions.toLong(source, mockConverter);
        assertEquals(42L, result);
    }

    @Test
    void toLong_withLong_returnsSameValue() {
        Long source = 123L;
        long result = NumberConversions.toLong(source, mockConverter);
        assertEquals(123L, result);
    }

    @Test
    void toLong_withDouble_truncatesFraction() {
        Double source = 5.9;
        long result = NumberConversions.toLong(source, mockConverter);
        // doubleValue() -> longValue() truncates toward zero
        assertEquals(5L, result);
    }

    @Test
    void toLong_withBigInteger_withinLongRange() {
        BigInteger source = new BigInteger("999999999");
        long result = NumberConversions.toLong(source, mockConverter);
        assertEquals(999_999_999L, result);
    }

    @Test
    void toLong_withNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toLong(null, mockConverter));
    }

    @Test
    void toLong_withNonNumber_throwsClassCastException() {
        String source = "not a number";
        assertThrows(ClassCastException.class, () -> NumberConversions.toLong(source, mockConverter));
    }

    /**
     * Successful conversion cases.
     */
    @ParameterizedTest(name = "{index}: toLong({0}) = {1}")
    @MethodSource("successProvider")
    void testToLongSuccess(Object from, long expected) {
        long result = NumberConversions.toLong(from, mockConverter);
        assertEquals(expected, result);
    }

    /**
     * Cases that are expected to throw an exception.
     */
    @ParameterizedTest(name = "{index}: toLong({0}) throws {1}")
    @MethodSource("exceptionProvider")
    void testToLongException(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toLong(from, mockConverter));
    }
}
