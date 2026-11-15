package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toInt_Test {

    /**
     * The Converter instance required by the method under test. It is not used by the
     *  current implementation of {@code NumberConversions.toInt}, but the signature
     *  requires it, so we provide a mock.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple {@link Number} implementation used to verify that {@code toInt}
     * works with custom subclasses. It stores an {@code int} value and returns
     * double that value from {@code intValue()}.
     */
    private static final class CustomNumber extends Number {

        private final int value;

        CustomNumber(int value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            // deliberately double the stored value to make the behaviour visible
            return value * 2;
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
     * Provides all cases that should return a concrete {@code int} value.
     */
    static Stream<Arguments> successfulCases() {
        // Helper instances used in multiple cases
        Converter dummyConverter = new Converter() {

            @Override
            public Object convert(Object o) {
                // identity conversion – not used by toInt()
                return o;
            }
        };
        Converter mockedConverter = Mockito.mock(Converter.class);
        return Stream.of(// ---- Simple numeric types -------------------------------------------------
        Arguments.of(Integer.valueOf(42), null, 42), Arguments.of(Long.valueOf(1234567890123L), null, Long.valueOf(1234567890123L).intValue()), Arguments.of(Double.valueOf(3.14), null, Double.valueOf(3.14).intValue()), Arguments.of(BigDecimal.valueOf(12345.67), null, BigDecimal.valueOf(12345.67).intValue()), // ---- Custom Number subclass that doubles the stored value -----------------
        Arguments.of(new CustomNumber(7), null, 14), // ---- Boundary integer values ---------------------------------------------
        Arguments.of(Integer.MAX_VALUE, null, Integer.MAX_VALUE), Arguments.of(Integer.MIN_VALUE, null, Integer.MIN_VALUE), // ---- Short, Byte and Float ------------------------------------------------
        Arguments.of(Short.valueOf((short) 123), null, Short.valueOf((short) 123).intValue()), Arguments.of(Byte.valueOf((byte) -5), null, Byte.valueOf((byte) -5).intValue()), Arguments.of(Float.valueOf(2.9f), null, Float.valueOf(2.9f).intValue()), // ---- BigInteger / BigDecimal beyond int range ----------------------------
        Arguments.of(new BigInteger("12345678901234567890"), null, new BigInteger("12345678901234567890").intValue()), Arguments.of(new BigDecimal("-98765432109876543210"), null, new BigDecimal("-98765432109876543210").intValue()), // ---- Extreme long values -------------------------------------------------
        Arguments.of(Long.MAX_VALUE, null, Long.MAX_VALUE.intValue()), Arguments.of(Long.MIN_VALUE, null, Long.MIN_VALUE.intValue()), // ---- Floating‑point special values ----------------------------------------
        Arguments.of(Double.NaN, null, Double.NaN.intValue()), Arguments.of(Double.POSITIVE_INFINITY, null, Double.POSITIVE_INFINITY.intValue()), Arguments.of(Double.NEGATIVE_INFINITY, null, Double.NEGATIVE_INFINITY.intValue()), Arguments.of(Float.NaN, null, Float.NaN.intValue()), Arguments.of(Float.POSITIVE_INFINITY, null, Float.POSITIVE_INFINITY.intValue()), Arguments.of(Float.NEGATIVE_INFINITY, null, Float.NEGATIVE_INFINITY.intValue()), // ---- Non‑null Converter argument (ignored by the implementation) -------
        Arguments.of(Integer.valueOf(7), dummyConverter, 7), Arguments.of(new BigDecimal("42.99"), mockedConverter, new BigDecimal("42.99").intValue()));
    }

    /**
     * Provides all cases that should result in an exception being thrown.
     */
    static Stream<Arguments> exceptionCases() {
        return Stream.of(// ---- Null source ---------------------------------------------------------
        Arguments.of(null, null, NullPointerException.class), // ---- Non‑Number sources --------------------------------------------------
        Arguments.of("123", null, ClassCastException.class), Arguments.of(new Object(), null, ClassCastException.class), Arguments.of(new AtomicInteger(5), null, ClassCastException.class), // ---- Number subclass whose intValue() throws an exception ----------------
        Arguments.of(new Number() {

            @Override
            public int intValue() {
                throw new IllegalArgumentException("intValue failure");
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
        }, null, IllegalArgumentException.class), Arguments.of(new Number() {

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
        }, null, IllegalStateException.class));
    }

    @Test
    void toInt_WithInteger_ReturnsExactValue() {
        Integer source = 42;
        int result = NumberConversions.toInt(source, mockConverter);
        assertEquals(42, result);
    }

    @Test
    void toInt_WithLong_TruncatesToInt() {
        // larger than int range
        Long source = 1234567890123L;
        int result = NumberConversions.toInt(source, mockConverter);
        // Long.intValue() simply discards high-order bits
        assertEquals(source.intValue(), result);
    }

    @Test
    void toInt_WithDouble_TruncatesTowardsZero() {
        Double source = 12.987;
        int result = NumberConversions.toInt(source, mockConverter);
        assertEquals(source.intValue(), result);
    }

    @Test
    void toInt_WithBigDecimal_TruncatesTowardsZero() {
        BigDecimal source = new BigDecimal("99.99");
        int result = NumberConversions.toInt(source, mockConverter);
        assertEquals(source.intValue(), result);
    }

    @Test
    void toInt_WithBigInteger_TruncatesToInt() {
        BigInteger source = new BigInteger("12345678901234567890");
        int result = NumberConversions.toInt(source, mockConverter);
        assertEquals(source.intValue(), result);
    }

    @Test
    void toInt_WithNull_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toInt(null, mockConverter));
    }

    @Test
    void toInt_WithNonNumber_ThrowsClassCastException() {
        String notANumber = "not a number";
        assertThrows(ClassCastException.class, () -> NumberConversions.toInt(notANumber, mockConverter));
    }

    /**
     * Successful conversions – the method returns the expected {@code int} value.
     *
     * <p>The {@code expected} argument is the exact integer that {@code Number.intValue()}
     * would produce for the supplied {@code from} instance.</p>
     */
    @ParameterizedTest(name = "[{index}] toInt({0}) ⇒ {2}")
    @MethodSource("successfulCases")
    void toInt_ReturnsExpected(Object from, Converter converter, int expected) {
        int result = NumberConversions.toInt(from, converter);
        assertEquals(expected, result, () -> String.format("Expected %d but got %d for input %s", expected, result, from));
    }

    /**
     * Exceptional conversions – the method should propagate the expected exception.
     *
     * <p>Typical failure modes are {@link NullPointerException} for a {@code null}
     * source and {@link ClassCastException} when the source does not extend {@link Number}.
     * Custom {@link Number} subclasses that throw from {@code intValue()} are also verified.</p>
     */
    @ParameterizedTest(name = "[{index}] toInt({0}) throws {2}")
    @MethodSource("exceptionCases")
    void toInt_ThrowsExpected(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toInt(from, converter), () -> String.format("Expected %s to be thrown for input %s", expectedException.getSimpleName(), from));
    }
}
