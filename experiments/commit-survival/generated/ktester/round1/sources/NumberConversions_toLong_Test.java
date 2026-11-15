package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toLong_Test {

    /**
     * The Converter is not used by {@code NumberConversions.toLong}, but the method signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Supplies the valid conversion scenarios.
     */
    private static Stream<Arguments> provideValidNumberCases() {
        // Anonymous Number subclass that returns 777L from longValue()
        Number anonNumber = new Number() {

            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                return 777L;
            }

            @Override
            public float floatValue() {
                return 0f;
            }

            @Override
            public double doubleValue() {
                return 0d;
            }
        };
        return Stream.of(// Integer
        Arguments.of(Integer.valueOf(42), 42L), // Long (Long.MAX_VALUE)
        Arguments.of(Long.valueOf(Long.MAX_VALUE), Long.MAX_VALUE), // Double (fraction should be truncated)
        Arguments.of(Double.valueOf(3.99), 3L), // BigDecimal (truncated via longValue())
        Arguments.of(new BigDecimal("12345"), 12345L), // BigInteger (may overflow – use longValue() result as expected)
        Arguments.of(new BigInteger("98765432109876543210"), new BigInteger("98765432109876543210").longValue()), // Anonymous Number subclass
        Arguments.of(anonNumber, 777L), // Negative int
        Arguments.of(Integer.valueOf(-12345), -12345L), // Negative double (truncates toward zero)
        Arguments.of(Double.valueOf(-9876543210.99), -9876543210L), // BigDecimal with fractional part
        Arguments.of(new BigDecimal("1234567890.123456789"), 1234567890L), // AtomicLong (implements Number)
        Arguments.of(new AtomicLong(55555L), 55555L), // AtomicInteger (implements Number)
        Arguments.of(new AtomicInteger(777), 777L));
    }

    /**
     * Supplies the exception scenarios.
     */
    private static Stream<Arguments> provideExceptionCases() {
        // Number subclass whose longValue() throws an unchecked exception
        Number throwingNumber = new Number() {

            @Override
            public int intValue() {
                return 0;
            }

            @Override
            public long longValue() {
                throw new IllegalStateException("forced");
            }

            @Override
            public float floatValue() {
                return 0f;
            }

            @Override
            public double doubleValue() {
                return 0d;
            }
        };
        return Stream.of(// AtomicBoolean does NOT implement Number → ClassCastException
        Arguments.of(new AtomicBoolean(true), ClassCastException.class), // Null source → NullPointerException when .longValue() is invoked
        Arguments.of(null, NullPointerException.class), // String input → ClassCastException
        Arguments.of("some string", ClassCastException.class), // Boolean (autoboxed) → ClassCastException
        Arguments.of(Boolean.TRUE, ClassCastException.class), // Arbitrary Object → ClassCastException
        Arguments.of(new Object(), ClassCastException.class), // Custom Number that throws IllegalStateException from longValue()
        Arguments.of(throwingNumber, IllegalStateException.class));
    }

    @Test
    void toLong_withInteger_returnsCorrectLong() {
        Integer source = 42;
        long result = NumberConversions.toLong(source, mockConverter);
        assertEquals(42L, result, "Integer should be converted to its long value");
    }

    @Test
    void toLong_withLong_returnsSameLong() {
        // Long.MAX_VALUE
        Long source = 9_223_372_036_854_775_807L;
        long result = NumberConversions.toLong(source, mockConverter);
        assertEquals(Long.MAX_VALUE, result, "Long should be returned unchanged");
    }

    @Test
    void toLong_withDouble_truncatesFraction() {
        Double source = 3.99;
        long result = NumberConversions.toLong(source, mockConverter);
        assertEquals(3L, result, "Double should be truncated toward zero");
    }

    @Test
    void toLong_withBigDecimal_convertsCorrectly() {
        BigDecimal source = new BigDecimal("12345");
        long result = NumberConversions.toLong(source, mockConverter);
        assertEquals(12_345L, result, "BigDecimal should be converted via its longValue()");
    }

    @Test
    void toLong_withNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toLong(null, mockConverter), "Passing null should result in NPE when the cast to Number is attempted");
    }

    @Test
    void toLong_withNonNumber_throwsClassCastException() {
        String source = "not a number";
        assertThrows(ClassCastException.class, () -> NumberConversions.toLong(source, mockConverter), "Passing a non‑Number should cause a ClassCastException");
    }

    /**
     * Parameterized test that verifies correct long values are returned
     * for a variety of {@link Number} implementations.
     */
    @ParameterizedTest(name = "[{index}] toLong({0}) = {1}")
    @MethodSource("provideValidNumberCases")
    void toLong_validConversions(Object from, long expected) {
        long result = NumberConversions.toLong(from, mockConverter);
        assertEquals(expected, result, () -> "Expected toLong(" + from + ") to be " + expected + " but was " + result);
    }

    /**
     * Parameterized test that verifies the method throws the expected
     * exception for invalid inputs.
     */
    @ParameterizedTest(name = "[{index}] toLong({0}) throws {1}")
    @MethodSource("provideExceptionCases")
    void toLong_expectedExceptions(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toLong(from, mockConverter), () -> "Expected toLong(" + from + ") to throw " + expectedException.getSimpleName());
    }
}
