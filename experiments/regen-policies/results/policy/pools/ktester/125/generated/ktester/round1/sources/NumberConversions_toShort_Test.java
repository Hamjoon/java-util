package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toShort_Test {

    private static Converter converter;

    /**
     * Simple {@link Number} implementation that returns a fixed integer value.
     */
    private static final class CustomNumber extends Number {

        private final int value;

        CustomNumber(int value) {
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
    }

    /**
     * A class that does NOT extend {@link Number}. Used to verify that a
     * {@code ClassCastException} is thrown for non‑Number arguments.
     */
    private static final class CustomNonNumber {
    }

    /**
     * {@link Number} subclass whose {@code shortValue()} deliberately throws
     * an {@link IllegalStateException}. This tests exception propagation.
     */
    private static final class ThrowingNumber extends Number {

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
            throw new IllegalStateException("shortValue exception");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter has a public constructor that requires ConverterOptions.
        // Using the default options is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    /**
     * Supplies the arguments for {@link #toShort_validConversions(Object, short, String)}.
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Integer within short range
        Arguments.of(Integer.valueOf(123), (short) 123, "Integer within short range should be converted directly to short 123."), // Max positive short value
        Arguments.of(Integer.valueOf(32767), (short) 32767, "Maximum positive short value; Integer 32767 should return short 32767."), // Min short value
        Arguments.of(Integer.valueOf(-32768), (short) -32768, "Minimum short value; Integer -32768 should return short -32768."), // Long larger than short range – low 16 bits kept
        Arguments.of(Long.valueOf(1234567890123L), (short) 525, "Long value larger than short range; shortValue() keeps low 16 bits (1234567890123L → 525)."), // Double with fractional part – truncation
        Arguments.of(Double.valueOf(123.987), (short) 123, "Double with fractional part; shortValue() truncates to 123."), // Integer overflow test
        Arguments.of(Integer.valueOf(40000), (short) -25536, "Integer overflow test; 40000 exceeds short max, resulting in -25536 after truncation."), // Zero double
        Arguments.of(Double.valueOf(0.0), (short) 0, "Zero double should convert to short 0."), // Byte value (within range) – expressed as Integer literal but fits a byte
        Arguments.of(Integer.valueOf(127), (short) 127, "Byte value (within range) should be returned unchanged as short."), // Negative byte value
        Arguments.of(Integer.valueOf(-128), (short) -128, "Byte negative value should be returned unchanged as short."), // Float value – auto‑boxed
        Arguments.of(Float.valueOf(12.34f), (short) 12, "Float value (auto‑boxed) truncates fractional part."), // BigInteger larger than short range – low 16 bits
        Arguments.of(new BigInteger("12345678901234567890"), (short) -31070, "BigInteger larger than short range; shortValue() returns low 16 bits (-31070)."), // BigDecimal with many fractional digits – truncation
        Arguments.of(new BigDecimal("123.45678901234568"), (short) 123, "BigDecimal with many fractional digits; shortValue() truncates to 123."), // Custom Number subclass returning 42
        Arguments.of(new CustomNumber(42), (short) 42, "Custom subclass of Number returning 42; should be converted to short 42."));
    }

    private static Stream<Arguments> nullAndExceptionProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Passing null as the source object should result in a NullPointerException."), Arguments.of("not a number", ClassCastException.class, "Passing a String object should cause a ClassCastException."), Arguments.of(Boolean.TRUE, ClassCastException.class, "Passing a Boolean object should cause a ClassCastException."), Arguments.of(new Object(), ClassCastException.class, "Passing a plain Object (not a Number) should result in a ClassCastException."), Arguments.of(new CustomNonNumber(), ClassCastException.class, "Passing an instance of a user‑defined class that does not extend Number should trigger a ClassCastException."));
    }

    @Test
    void toShort_fromInteger_returnsCorrectShort() {
        // larger than Short.MAX_VALUE
        Integer source = 12345;
        // overflow is expected per Java semantics
        short expected = source.shortValue();
        short actual = NumberConversions.toShort(source, converter);
        assertEquals(expected, actual, "Integer should be converted via shortValue()");
    }

    @Test
    void toShort_fromLong_returnsCorrectShort() {
        // exactly Short.MAX_VALUE
        Long source = 32767L;
        short expected = source.shortValue();
        short actual = NumberConversions.toShort(source, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toShort_fromShort_returnsSameValue() {
        Short source = 42;
        short actual = NumberConversions.toShort(source, converter);
        assertEquals(source.shortValue(), actual);
    }

    @Test
    void toShort_fromByte_returnsCorrectShort() {
        Byte source = 7;
        short actual = NumberConversions.toShort(source, converter);
        assertEquals(source.shortValue(), actual);
    }

    @Test
    void toShort_fromBigDecimal_returnsCorrectShort() {
        BigDecimal source = new BigDecimal("1234.56");
        short expected = source.shortValue();
        short actual = NumberConversions.toShort(source, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toShort_fromBigInteger_returnsCorrectShort() {
        BigInteger source = new BigInteger("123456");
        short expected = source.shortValue();
        short actual = NumberConversions.toShort(source, converter);
        assertEquals(expected, actual);
    }

    @Test
    void toShort_fromNull_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> NumberConversions.toShort(null, converter));
    }

    @Test
    void toShort_fromNonNumber_throwsClassCastException() {
        Object source = "not a number";
        assertThrows(ClassCastException.class, () -> NumberConversions.toShort(source, converter));
    }

    /**
     * Parameterized test that covers all valid conversion scenarios described
     * in the JSON test data.  Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the source object (already boxed as the appropriate type),</li>
     *   <li>the expected {@code short} result,</li>
     *   <li>a description used as the test display name.</li>
     * </ul>
     */
    @ParameterizedTest(name = "{2}")
    @MethodSource("validConversionProvider")
    void toShort_validConversions(Object source, short expected, String description) {
        short actual = NumberConversions.toShort(source, converter);
        assertEquals(expected, actual, () -> "Conversion of " + source.getClass().getSimpleName() + " failed: " + description);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullAndExceptionProvider")
    void toShort_invalidInputs(Object source, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> NumberConversions.toShort(source, converter), description);
    }

    @Test
    void toShort_throwingNumber_propagatesException() {
        ThrowingNumber source = new ThrowingNumber();
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> NumberConversions.toShort(source, converter), "Exception thrown by shortValue() should propagate.");
        assertEquals("shortValue exception", ex.getMessage());
    }
}
