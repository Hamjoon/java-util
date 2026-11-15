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
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigIntegerConversions_toBigDecimal_Test {

    @Mock
    Converter // Converter is not used by the method, but we provide a mock.
    mockConverter;

    /* -----------------------------------------------------------------------
     *  Helper holder class to keep the parameterised test signatures tidy.
     * ----------------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Object from;

        final Converter converter;

        // Either BigDecimal or Class<? extends Throwable>
        final Object expected;

        final Class<? extends Throwable> exceptionClass;

        final String description;

        // Constructor for successful conversion expectations
        ArgumentsHolder(Object from, Converter converter, BigDecimal expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.exceptionClass = null;
            this.description = description;
        }

        // Constructor for exception expectations
        ArgumentsHolder(Object from, Converter converter, Class<? extends Throwable> exceptionClass, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = null;
            this.exceptionClass = exceptionClass;
            this.description = description;
        }
    }

    /* -----------------------------------------------------------------------
     *  Parameterised tests for the "Valid BigInteger inputs" group
     * ----------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> validBigIntegerProvider() {
        return Stream.of(// Zero
        new ArgumentsHolder(BigInteger.ZERO, null, BigDecimal.ZERO, "Convert a zero BigInteger to BigDecimal; should return BigDecimal.ZERO."), // ONE
        new ArgumentsHolder(BigInteger.ONE, null, BigDecimal.ONE, "Convert a positive small BigInteger (1) to BigDecimal; should return BigDecimal.ONE."), // Negative value
        new ArgumentsHolder(BigInteger.valueOf(-12345), null, new BigDecimal("-12345"), "Convert a negative BigInteger to BigDecimal; verifies sign is preserved."), // Constant BILLION (1_000_000_000)
        new ArgumentsHolder(BigInteger.valueOf(1_000_000_000L), null, new BigDecimal("1000000000"), "Convert a large positive BigInteger (1 billion) to BigDecimal; tests handling of values larger than int range."), // Very large (30+ digits)
        new ArgumentsHolder(new BigInteger("123456789012345678901234567890"), null, new BigDecimal("123456789012345678901234567890"), "Convert a very large BigInteger (more than 30 digits) to BigDecimal; ensures arbitrary‑precision conversion works."), // Non‑null Converter (dummy implementation)
        new ArgumentsHolder(BigInteger.TEN, new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                // Should never be called.
                throw new AssertionError("Converter should not be invoked");
            }
        }, new BigDecimal("10"), "Pass a non‑null Converter instance to verify that the method does not depend on the Converter argument."));
    }

    /* -----------------------------------------------------------------------
     *  Parameterised tests for the "Invalid inputs / error handling" group
     * ----------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> invalidInputProvider() {
        return Stream.of(// Null source
        new ArgumentsHolder(null, null, NullPointerException.class, "Passing null as the source object results in a NullPointerException."), // Wrong type: Integer
        new ArgumentsHolder(Integer.valueOf(42), null, ClassCastException.class, "Passing an Integer instead of BigInteger triggers a ClassCastException."), // Wrong type: String
        new ArgumentsHolder("12345", null, ClassCastException.class, "Passing a String instead of BigInteger also results in a ClassCastException."));
    }

    /* -----------------------------------------------------------------------
     *  Parameterised tests for the "Boundary and extreme BigInteger values" group
     * ----------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> boundaryProvider() {
        return Stream.of(// Long.MAX_VALUE
        new ArgumentsHolder(BigInteger.valueOf(Long.MAX_VALUE), null, BigDecimal.valueOf(Long.MAX_VALUE), "Convert the maximum long value wrapped in a BigInteger; verifies handling of the upper 64‑bit boundary."), // Long.MIN_VALUE
        new ArgumentsHolder(BigInteger.valueOf(Long.MIN_VALUE), null, BigDecimal.valueOf(Long.MIN_VALUE), "Convert the minimum long value wrapped in a BigInteger; checks correct handling of the lower 64‑bit boundary."), // Very large negative (100 digits)
        new ArgumentsHolder(new BigInteger("-1000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"), null, new BigDecimal("-1000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000"), "Convert a very large negative BigInteger (100 digits) to ensure arbitrary‑precision conversion works for negative numbers of extreme magnitude."), // 80‑digit positive
        new ArgumentsHolder(new BigInteger("12345678901234567890123456789012345678901234567890123456789012345678901234567890"), null, new BigDecimal("12345678901234567890123456789012345678901234567890123456789012345678901234567890"), "Convert an 80‑digit positive BigInteger to verify that all digits are preserved."), // Byte array with leading zero
        new ArgumentsHolder(new BigInteger(new byte[] { 0, 1, 0, -1, 127 }), null, new BigDecimal(new BigInteger(new byte[] { 0, 1, 0, -1, 127 })), "Convert a BigInteger created from a byte array containing a leading zero byte; ensures that leading zeros are ignored and the numeric value is correct."));
    }

    /* -----------------------------------------------------------------------
     *  Parameterised tests for the "Converter side‑effects verification" group
     * ----------------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> converterSideEffectProvider() {
        return Stream.of(// Converter that throws if invoked
        new ArgumentsHolder(BigInteger.TEN, new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                throw new RuntimeException("Converter should not be invoked");
            }
        }, new BigDecimal("10"), "Pass a non‑null Converter that throws if any of its methods are called; the method must not use the Converter."), // Converter that returns null
        new ArgumentsHolder(BigInteger.ZERO, new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                // ignored by the method
                return null;
            }
        }, BigDecimal.ZERO, "Even when a Converter instance implements convert() to return null, the method must ignore it and correctly return BigDecimal.ZERO."));
    }

    @Test
    void toBigDecimal_WithPositiveBigInteger_ReturnsEquivalentBigDecimal() {
        // given
        BigInteger source = new BigInteger("12345678901234567890");
        // when
        BigDecimal result = BigIntegerConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(new BigDecimal(source), result);
    }

    @Test
    void toBigDecimal_WithZero_ReturnsZeroBigDecimal() {
        // given
        BigInteger source = BigInteger.ZERO;
        // when
        BigDecimal result = BigIntegerConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void toBigDecimal_WithNegativeBigInteger_ReturnsNegativeBigDecimal() {
        // given
        BigInteger source = new BigInteger("-98765432109876543210");
        // when
        BigDecimal result = BigIntegerConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(new BigDecimal(source), result);
    }

    @Test
    void toBigDecimal_WithVeryLargeBigInteger_ReturnsCorrespondingBigDecimal() {
        // given
        BigInteger source = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN).add(BigInteger.ONE);
        // when
        BigDecimal result = BigIntegerConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(new BigDecimal(source), result);
    }

    @Test
    void toBigDecimal_WithNonBigInteger_ThrowsClassCastException() {
        // given
        Object notABigInteger = "not a big integer";
        // when / then
        assertThrows(ClassCastException.class, () -> BigIntegerConversions.toBigDecimal(notABigInteger, mockConverter));
    }

    @Test
    void toBigDecimal_WithNull_ThrowsNullPointerException() {
        // given
        Object nullValue = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toBigDecimal(nullValue, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validBigIntegerProvider")
    void toBigDecimal_ValidInputs_ReturnsExpected(ArgumentsHolder holder) {
        // when
        BigDecimal result = BigIntegerConversions.toBigDecimal(holder.from, holder.converter);
        // then
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidInputProvider")
    void toBigDecimal_InvalidInputs_ThrowsExpectedException(ArgumentsHolder holder) {
        // when / then
        assertThrows(holder.exceptionClass, () -> BigIntegerConversions.toBigDecimal(holder.from, holder.converter));
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("boundaryProvider")
    void toBigDecimal_BoundaryValues_ReturnsExpected(ArgumentsHolder holder) {
        // when
        BigDecimal result = BigIntegerConversions.toBigDecimal(holder.from, holder.converter);
        // then
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("converterSideEffectProvider")
    void toBigDecimal_ConverterIgnored_ReturnsExpected(ArgumentsHolder holder) {
        // when
        BigDecimal result = BigIntegerConversions.toBigDecimal(holder.from, holder.converter);
        // then
        assertEquals(holder.expected, result);
    }
}
