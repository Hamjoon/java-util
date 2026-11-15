package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NumberConversions_toFloat_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires options; use default options
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none for this test)
        converter = null;
    }

    static Stream<Arguments> validNumberTypesProvider() {
        return Stream.of(arguments("Convert an Integer to float; expect exact float representation.", 42, 42.0f), arguments("Convert a large Long to float; verify possible precision loss.", 1234567890123L, 1.2345679E12f), arguments("Convert a Double to float; expect the float representation.", 3.14d, 3.14f), arguments("Convert a Float to float; should return the same value.", 2.71f, 2.71f), arguments("Convert a BigDecimal to float; verify correct conversion.", new BigDecimal("1.2345"), 1.2345f), arguments("Convert a BigInteger to float; expect exact float representation.", new BigInteger("1000"), 1000.0f), arguments("Convert a Short to float; should return the corresponding float value.", (short) 5, 5.0f), arguments("Convert a Byte to float; verify negative value handling.", (byte) -8, -8.0f));
    }

    static Stream<Arguments> zeroAndNegativeProvider() {
        return Stream.of(arguments("Convert an Integer zero to float; should return 0.0f.", 0, 0.0f), arguments("Convert a negative Long to float; verify sign is preserved.", -123L, -123.0f), arguments("Convert a negative BigDecimal with fractional part to float.", new BigDecimal("-0.001"), -0.001f), arguments("Convert a negative BigInteger to float; expect exact representation.", new BigInteger("-500"), -500.0f));
    }

    static Stream<Arguments> specialFloatDoubleProvider() {
        return Stream.of(arguments("Convert Float.NaN; result should be NaN.", Float.NaN, "NaN"), arguments("Convert Float.POSITIVE_INFINITY; result should be Infinity.", Float.POSITIVE_INFINITY, "Infinity"), arguments("Convert Float.NEGATIVE_INFINITY; result should be -Infinity.", Float.NEGATIVE_INFINITY, "-Infinity"), arguments("Convert Double.NaN; float conversion yields NaN.", Double.NaN, "NaN"), arguments("Convert Double.POSITIVE_INFINITY; result should be Infinity.", Double.POSITIVE_INFINITY, "Infinity"), arguments("Convert Double.NEGATIVE_INFINITY; result should be -Infinity.", Double.NEGATIVE_INFINITY, "-Infinity"));
    }

    static Stream<Arguments> atomicNumberProvider() {
        return Stream.of(arguments("Convert an AtomicInteger to float; should return the integer value as float.", new AtomicInteger(10), 10.0f), arguments("Convert an AtomicLong with a negative value to float.", new AtomicLong(-20L), -20.0f));
    }

    static Stream<Arguments> overflowProvider() {
        return Stream.of(arguments("Convert a very large BigDecimal that exceeds float max; result should be Infinity.", new BigDecimal("1e40"), "Infinity"), arguments("Convert a very small (large negative) BigDecimal; result should be -Infinity.", new BigDecimal("-1e40"), "-Infinity"));
    }

    @Test
    void toFloat_WithInteger_returnsFloatValue() {
        // given
        Integer source = 42;
        // when
        float result = NumberConversions.toFloat(source, converter);
        // then
        assertEquals(42.0f, result);
    }

    @Test
    void toFloat_WithLong_returnsFloatValue() {
        // given
        Long source = 123456789L;
        // when
        float result = NumberConversions.toFloat(source, converter);
        // then
        assertEquals(123456789L, (long) result);
    }

    @Test
    void toFloat_WithDouble_returnsFloatValue() {
        // given
        Double source = 3.1415926535;
        // when
        float result = NumberConversions.toFloat(source, converter);
        // then
        assertEquals(3.1415927f, result, 0.000001f);
    }

    @Test
    void toFloat_WithFloat_returnsSameValue() {
        // given
        Float source = 7.5f;
        // when
        float result = NumberConversions.toFloat(source, converter);
        // then
        assertEquals(7.5f, result);
    }

    @Test
    void toFloat_WithBigDecimal_returnsFloatValue() {
        // given
        java.math.BigDecimal source = new java.math.BigDecimal("12.34");
        // when
        float result = NumberConversions.toFloat(source, converter);
        // then
        assertEquals(12.34f, result, 0.0001f);
    }

    @Test
    void toFloat_WithNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> NumberConversions.toFloat(source, converter));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validNumberTypesProvider")
    void toFloat_ValidNumberTypes(String description, Object from, float expected) {
        float actual = NumberConversions.toFloat(from, converter);
        assertEquals(expected, actual, 0.000001f, description);
    }

    // -----------------------------------------------------------------
    @Test
    void toFloat_NullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toFloat(null, converter));
    }

    @Test
    void toFloat_StringSource_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> NumberConversions.toFloat("123", converter));
    }

    @Test
    void toFloat_ArbitraryObject_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> NumberConversions.toFloat(new Object(), converter));
    }

    @Test
    void toFloat_WithMockConverter_ignoresConverter() {
        Converter mockConverter = Mockito.mock(Converter.class);
        float result = NumberConversions.toFloat(42, mockConverter);
        assertEquals(42.0f, result);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("zeroAndNegativeProvider")
    void toFloat_ZeroAndNegative(String description, Object from, float expected) {
        float actual = NumberConversions.toFloat(from, converter);
        assertEquals(expected, actual, 0.000001f, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("specialFloatDoubleProvider")
    void toFloat_SpecialFloatDoubleValues(String description, Object from, String expectedSpecial) {
        float actual = NumberConversions.toFloat(from, converter);
        switch(expectedSpecial) {
            case "NaN":
                assertTrue(Float.isNaN(actual), description);
                break;
            case "Infinity":
                assertTrue(Float.isInfinite(actual) && actual > 0, description);
                break;
            case "-Infinity":
                assertTrue(Float.isInfinite(actual) && actual < 0, description);
                break;
            default:
                fail("Unexpected expectedSpecial value: " + expectedSpecial);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("atomicNumberProvider")
    void toFloat_AtomicNumberTypes(String description, Object from, float expected) {
        float actual = NumberConversions.toFloat(from, converter);
        assertEquals(expected, actual, 0.000001f, description);
    }

    @Test
    void toFloat_AtomicBoolean_throwsClassCastException() {
        assertThrows(ClassCastException.class, () -> NumberConversions.toFloat(new AtomicBoolean(true), converter));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("overflowProvider")
    void toFloat_OverflowCases(String description, Object from, String expectedSpecial) {
        float actual = NumberConversions.toFloat(from, converter);
        switch(expectedSpecial) {
            case "Infinity":
                assertTrue(Float.isInfinite(actual) && actual > 0, description);
                break;
            case "-Infinity":
                assertTrue(Float.isInfinite(actual) && actual < 0, description);
                break;
            default:
                fail("Unexpected expectedSpecial: " + expectedSpecial);
        }
    }

    // -----------------------------------------------------------------
    @Test
    void toFloat_NumberSubclassThrows_ArithmeticException() {
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
                throw new ArithmeticException("bad float");
            }

            @Override
            public double doubleValue() {
                return 0.0;
            }
        };
        assertThrows(ArithmeticException.class, () -> NumberConversions.toFloat(badNumber, converter));
    }

    @Test
    void toFloat_NumberSubclassThrows_IllegalStateException_WithMockConverter() {
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
                throw new IllegalStateException("state error");
            }

            @Override
            public double doubleValue() {
                return 0.0;
            }
        };
        Converter mockConverter = Mockito.mock(Converter.class);
        assertThrows(IllegalStateException.class, () -> NumberConversions.toFloat(badNumber, mockConverter));
    }
}
