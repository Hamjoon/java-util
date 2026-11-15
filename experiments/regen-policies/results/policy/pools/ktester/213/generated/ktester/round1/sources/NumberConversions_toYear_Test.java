package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.time.Year;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toYear_Test {

    /**
     * The Converter instance required by the method signature.
     *  The implementation does not use it, but a real instance is supplied
     *  to avoid dealing with null‑checks inside the method.
     */
    private static Converter converter;

    /**
     * Simple {@link Number} subclass that returns a predefined short value.
     */
    private static final class MyNumber extends Number {

        private final int value;

        MyNumber(int value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return value;
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
        public short shortValue() {
            return (short) value;
        }

        @Override
        public byte byteValue() {
            return (byte) value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter does not affect NumberConversions.toYear
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> provideValidInputs() {
        return Stream.of(// Primitive wrapper types
        Arguments.of(Short.valueOf((short) 2021), Year.of(2021)), Arguments.of(Integer.valueOf(1999), Year.of(1999)), Arguments.of(Long.valueOf(3000L), Year.of(3000)), Arguments.of(Byte.valueOf((byte) 85), Year.of(85)), // Floating point types – shortValue() truncates the fraction
        Arguments.of(Double.valueOf(2022.99), Year.of(2022)), Arguments.of(Float.valueOf(1995.5f), Year.of(1995)), // BigDecimal / BigInteger (both implement Number)
        Arguments.of(new BigDecimal("2023"), Year.of(2023)), Arguments.of(new BigInteger("1980"), Year.of(1980)), // Edge values for short range
        Arguments.of(Short.valueOf(Short.MAX_VALUE), Year.of(Short.MAX_VALUE)), Arguments.of(Short.valueOf(Short.MIN_VALUE), Year.of(Short.MIN_VALUE)), // Zero and negative values
        Arguments.of(Short.valueOf((short) 0), Year.of(0)), Arguments.of(Short.valueOf((short) -500), Year.of(-500)), // Atomic types (implement Number)
        Arguments.of(new AtomicInteger(2025), Year.of(2025)), // Values that overflow short – behaviour follows casting to short
        Arguments.of(Integer.valueOf(40000), Year.of((short) 40000)), Arguments.of(new AtomicLong(40000L), Year.of((short) 40000)), Arguments.of(new BigDecimal("40000"), Year.of((short) 40000)), // NaN and infinities – shortValue() yields defined constants
        Arguments.of(Double.valueOf(Double.NaN), Year.of((short) 0)), Arguments.of(Double.valueOf(Double.POSITIVE_INFINITY), Year.of(Short.MAX_VALUE)), Arguments.of(Double.valueOf(Double.NEGATIVE_INFINITY), Year.of(Short.MIN_VALUE)), // Byte extremes
        Arguments.of(Byte.valueOf((byte) -128), Year.of(-128)), Arguments.of(Byte.valueOf((byte) 127), Year.of(127)), // Integer.MIN_VALUE – shortValue() wraps to 0
        Arguments.of(Integer.valueOf(Integer.MIN_VALUE), Year.of((short) Integer.MIN_VALUE)), // Custom Number subclass
        Arguments.of(new MyNumber(2022), Year.of(2022)), // Non‑null converter (still ignored)
        Arguments.of(Integer.valueOf(2000), Year.of(2000)));
    }

    private static Stream<Arguments> provideExceptionInputs() {
        return Stream.of(// Null source – NPE from cast to Number
        Arguments.of(null, NullPointerException.class), // Non‑Number source – ClassCastException from cast
        Arguments.of("2021", ClassCastException.class), // AtomicBoolean does not extend Number – ClassCastException
        Arguments.of(new AtomicBoolean(true), ClassCastException.class));
    }

    @Test
    void toYear_withInteger_returnsCorrectYear() {
        Integer source = 2023;
        Year result = NumberConversions.toYear(source, converter);
        assertEquals(Year.of(2023), result);
    }

    @Test
    void toYear_withShort_returnsCorrectYear() {
        Short source = 1999;
        Year result = NumberConversions.toYear(source, converter);
        assertEquals(Year.of(1999), result);
    }

    @Test
    void toYear_withLong_returnsCorrectYear() {
        Long source = 2000L;
        Year result = NumberConversions.toYear(source, converter);
        assertEquals(Year.of(2000), result);
    }

    @Test
    void toYear_withDouble_truncatesFraction() {
        Double source = 2025.9;
        Year result = NumberConversions.toYear(source, converter);
        // shortValue() drops the fractional part
        assertEquals(Year.of(2025), result);
    }

    @Test
    void toYear_withBigDecimal_returnsCorrectYear() {
        BigDecimal source = new BigDecimal("2022");
        Year result = NumberConversions.toYear(source, converter);
        assertEquals(Year.of(2022), result);
    }

    @Test
    void toYear_withNegativeNumber_returnsNegativeYear() {
        Integer source = -5;
        Year result = NumberConversions.toYear(source, converter);
        assertEquals(Year.of(-5), result);
    }

    @Test
    void toYear_withOverflowShortValue_behavesAccordingToShortCasting() {
        // 40000 exceeds Short.MAX_VALUE (32767) → wraps around
        Integer source = 40000;
        Year result = NumberConversions.toYear(source, converter);
        // = -25536
        short expectedShort = source.shortValue();
        assertEquals(Year.of(expectedShort), result);
    }

    @Test
    void toYear_withNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toYear(null, converter));
    }

    @ParameterizedTest(name = "[{index}] toYear({0}) ⇒ {1}")
    @MethodSource("provideValidInputs")
    void toYear_validInputs_returnsExpectedYear(Object from, Year expected) {
        Year result = NumberConversions.toYear(from, converter);
        assertEquals(expected, result, () -> "Expected Year.of(" + expected.getValue() + ") but got " + result);
    }

    @ParameterizedTest(name = "[{index}] toYear({0}) throws {1}")
    @MethodSource("provideExceptionInputs")
    void toYear_invalidInputs_throwsExpectedException(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.toYear(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }
}
