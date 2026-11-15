package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toDouble_Test {

    /**
     * Mocked Converter – not used by the implementation but required by the signature.
     */
    private Converter mockConverter;

    /* --------------------------------------------------------------------- */
    private static class MockConverter implements Converter {
        // Empty stub – the real implementation is irrelevant for the test.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for each test to avoid cross‑test interference.
        mockConverter = mock(Converter.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the reference – helps the garbage collector and makes the test
        // intent explicit.
        mockConverter = null;
    }

    private static Stream<Arguments> validNumberConversions() {
        return Stream.of(// Integer
        Arguments.of(5, null, 5.0, "Convert Integer to double"), // Double
        Arguments.of(3.14, null, 3.14, "Convert Double to double"), // BigDecimal
        Arguments.of(new BigDecimal("123.456"), null, 123.456, "Convert BigDecimal to double"), // AtomicInteger
        Arguments.of(new AtomicInteger(7), null, 7.0, "Convert AtomicInteger to double"), // Long
        Arguments.of(new Long(1234567890123L), null, 1234567890123.0, "Convert Long to double"), // Short
        Arguments.of((short) 12, null, 12.0, "Convert Short to double"), // Byte
        Arguments.of((byte) -8, null, -8.0, "Convert Byte to double"), // Float (object)
        Arguments.of(new Float(2.5f), null, 2.5, "Convert Float to double"), // BigInteger
        Arguments.of(new BigInteger("98765432109876543210"), null, 9.876543210987654E19, "Convert BigInteger to double (precision loss acceptable)"), // AtomicLong
        Arguments.of(new AtomicLong(42L), null, 42.0, "Convert AtomicLong to double"));
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(Arguments.of("abc", null, ClassCastException.class, "Non-number string should cause ClassCastException"), Arguments.of(null, null, NullPointerException.class, "Null input should cause NullPointerException"));
    }

    private static Stream<Arguments> edgeCases() {
        return Stream.of(Arguments.of(Float.NaN, null, "NaN", "Convert Float.NaN to double results NaN"), Arguments.of(Float.POSITIVE_INFINITY, null, "Infinity", "Convert Float.POSITIVE_INFINITY to double"), Arguments.of(Double.NEGATIVE_INFINITY, null, "-Infinity", "Convert Double.NEGATIVE_INFINITY to double"));
    }

    private static Stream<Arguments> converterParameterEffect() {
        return Stream.of(Arguments.of(5, new MockConverter(), 5.0, "A non‑null Converter instance is ignored; conversion still returns the correct double"));
    }

    private static Stream<Arguments> extremeFloatingValues() {
        return Stream.of(Arguments.of(Float.MAX_VALUE, null, 3.4028234663852886E38, "Convert Float.MAX_VALUE to double"), Arguments.of(Float.MIN_VALUE, null, 1.4E-45, "Convert Float.MIN_VALUE (positive subnormal) to double"), Arguments.of(Double.MAX_VALUE, null, 1.7976931348623157E308, "Convert Double.MAX_VALUE to double"), Arguments.of(Double.MIN_VALUE, null, 4.9E-324, "Convert Double.MIN_VALUE (positive subnormal) to double"));
    }

    private static Stream<Arguments> nonNumberObjectInputs() {
        return Stream.of(Arguments.of(true, null, ClassCastException.class, "Passing a Boolean (which does not implement Number) should trigger ClassCastException"), Arguments.of(new Object(), null, ClassCastException.class, "Passing a plain Object should trigger ClassCastException"), Arguments.of(new Date(), null, ClassCastException.class, "Passing a java.util.Date (not a Number) should trigger ClassCastException"), Arguments.of('c', null, ClassCastException.class, "Passing a Character should trigger ClassCastException"), Arguments.of(new int[] { 1, 2, 3 }, null, ClassCastException.class, "Passing an int array should trigger ClassCastException"));
    }

    private static Stream<Arguments> numberSubclassExceptionPropagation() {
        Number throwsUnsupported = new Number() {

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
                throw new UnsupportedOperationException("doubleValue not supported");
            }
        };
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
                throw new ArithmeticException("illegal arithmetic");
            }
        };
        return Stream.of(Arguments.of(throwsUnsupported, UnsupportedOperationException.class, "A custom Number implementation whose doubleValue() throws UnsupportedOperationException should propagate that exception"), Arguments.of(throwsArithmetic, ArithmeticException.class, "A custom Number implementation whose doubleValue() throws ArithmeticException should propagate that exception"));
    }

    @Test
    void toDouble_WithInteger_ReturnsCorrectValue() {
        Integer input = 42;
        double result = NumberConversions.toDouble(input, mockConverter);
        assertEquals(42.0, result, 0.000001);
    }

    @Test
    void toDouble_WithLong_ReturnsCorrectValue() {
        Long input = 123456789L;
        double result = NumberConversions.toDouble(input, mockConverter);
        assertEquals(123456789.0, result, 0.000001);
    }

    @Test
    void toDouble_WithDouble_ReturnsExactValue() {
        Double input = 3.1415926535;
        double result = NumberConversions.toDouble(input, mockConverter);
        assertEquals(3.1415926535, result, 0.000000001);
    }

    @Test
    void toDouble_WithFloat_ReturnsCorrectValue() {
        Float input = 2.71828f;
        double result = NumberConversions.toDouble(input, mockConverter);
        assertEquals(2.71828, result, 0.000001);
    }

    @Test
    void toDouble_WithBigDecimal_ReturnsCorrectValue() {
        BigDecimal input = new BigDecimal("12345.6789");
        double result = NumberConversions.toDouble(input, mockConverter);
        assertEquals(12345.6789, result, 0.000001);
    }

    @Test
    void toDouble_WithBigInteger_ReturnsCorrectValue() {
        BigInteger input = new BigInteger("9876543210");
        double result = NumberConversions.toDouble(input, mockConverter);
        assertEquals(9.87654321E9, result, 0.0001);
    }

    @Test
    void toDouble_WithAtomicInteger_ReturnsCorrectValue() {
        AtomicInteger input = new AtomicInteger(7);
        double result = NumberConversions.toDouble(input, mockConverter);
        assertEquals(7.0, result, 0.000001);
    }

    @Test
    void toDouble_WithAtomicLong_ReturnsCorrectValue() {
        AtomicLong input = new AtomicLong(555L);
        double result = NumberConversions.toDouble(input, mockConverter);
        assertEquals(555.0, result, 0.000001);
    }

    @Test
    void toDouble_WithNonNumber_ThrowsClassCastException() {
        String nonNumber = "not a number";
        assertThrows(ClassCastException.class, () -> NumberConversions.toDouble(nonNumber, mockConverter));
    }

    @Test
    void toDouble_WithNull_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toDouble(null, mockConverter));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("validNumberConversions")
    void validNumberConversions(Object from, Converter converter, Object expected, String description) {
        double result = NumberConversions.toDouble(from, converter);
        double expectedDouble = ((Number) expected).doubleValue();
        assertEquals(expectedDouble, result, 1e-12, description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("invalidInputs")
    void invalidInputs(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> NumberConversions.toDouble(from, converter), description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("edgeCases")
    void edgeCases(Object from, Converter converter, String expectedSpecial, String description) {
        double result = NumberConversions.toDouble(from, converter);
        switch(expectedSpecial) {
            case "NaN":
                assertTrue(Double.isNaN(result), description);
                break;
            case "Infinity":
                assertEquals(Double.POSITIVE_INFINITY, result, description);
                break;
            case "-Infinity":
                assertEquals(Double.NEGATIVE_INFINITY, result, description);
                break;
            default:
                fail("Unexpected special expected value: " + expectedSpecial);
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("converterParameterEffect")
    void converterParameterEffect(Object from, Converter converter, double expected, String description) {
        double result = NumberConversions.toDouble(from, converter);
        assertEquals(expected, result, 1e-12, description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("extremeFloatingValues")
    void extremeFloatingValues(Object from, Converter converter, double expected, String description) {
        double result = NumberConversions.toDouble(from, converter);
        assertEquals(expected, result, 1e-12, description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("nonNumberObjectInputs")
    void nonNumberObjectInputs(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> NumberConversions.toDouble(from, converter), description);
    }

    /* --------------------------------------------------------------------- */
    @Test
    @DisplayName("Custom Number subclass that returns 99.99 via doubleValue()")
    void customNumberSubclass() {
        Number custom = new Number() {

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
                return 99.99;
            }
        };
        double result = NumberConversions.toDouble(custom, mockConverter);
        assertEquals(99.99, result, 1e-12);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("numberSubclassExceptionPropagation")
    void numberSubclassExceptionPropagation(Number from, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> NumberConversions.toDouble(from, mockConverter), description);
    }
}
