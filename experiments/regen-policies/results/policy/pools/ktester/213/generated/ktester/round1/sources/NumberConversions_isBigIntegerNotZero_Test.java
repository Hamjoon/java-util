package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import java.math.BigInteger;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class NumberConversions_isBigIntegerNotZero_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The production code expects a non‑null Converter, but the method under test
        // does not actually use it.  We initialise a default instance for the
        // regular test cases.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing to clean up per‑test
        converter = null;
    }

    /**
     * Provides a variety of {@link BigInteger} values together with the expected
     * boolean result.  The {@code converter} argument is deliberately set to
     * {@code null} for most cases to prove that the method does not depend on it.
     */
    private static Stream<Arguments> bigIntegerProvider() {
        return Stream.of(// Zero – should return false
        arguments(BigInteger.ZERO, false, null), // Small positive non‑zero values
        arguments(BigInteger.ONE, true, null), arguments(new BigInteger("12345678901234567890"), true, null), // Small negative non‑zero values
        arguments(BigInteger.valueOf(-1), true, null), arguments(new BigInteger("-98765432109876543210"), true, null), // Values that exceed the range of a long
        arguments(BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE), true, null), arguments(BigInteger.valueOf(Long.MIN_VALUE).subtract(BigInteger.ONE), true, null));
    }

    @Test
    void isBigIntegerNotZero_returnsFalseWhenZero() {
        // given
        BigInteger zero = BigInteger.ZERO;
        // when
        boolean result = NumberConversions.isBigIntegerNotZero(zero, converter);
        // then
        assertFalse(result, "Zero should be considered not‑non‑zero");
    }

    @Test
    void isBigIntegerNotZero_returnsTrueWhenPositiveNonZero() {
        // given
        BigInteger positive = new BigInteger("12345678901234567890");
        // when
        boolean result = NumberConversions.isBigIntegerNotZero(positive, converter);
        // then
        assertTrue(result, "Positive non‑zero BigInteger should return true");
    }

    @Test
    void isBigIntegerNotZero_returnsTrueWhenNegativeNonZero() {
        // given
        BigInteger negative = new BigInteger("-98765432109876543210");
        // when
        boolean result = NumberConversions.isBigIntegerNotZero(negative, converter);
        // then
        assertTrue(result, "Negative non‑zero BigInteger should return true");
    }

    @Test
    void isBigIntegerNotZero_throwsClassCastExceptionForInvalidType() {
        // given
        Object notABigInteger = "not a big integer";
        // when / then
        assertThrows(ClassCastException.class, () -> NumberConversions.isBigIntegerNotZero(notABigInteger, converter), "Passing a non‑BigInteger should result in ClassCastException");
    }

    @ParameterizedTest(name = "isBigIntegerNotZero({0}) → {1}")
    @MethodSource("bigIntegerProvider")
    void isBigIntegerNotZero_variousValues(BigInteger from, boolean expected, Converter suppliedConverter) {
        // When the suppliedConverter is null we pass the default instance created in @BeforeEach.
        Converter usedConverter = suppliedConverter != null ? suppliedConverter : converter;
        boolean result = NumberConversions.isBigIntegerNotZero(from, usedConverter);
        assertEquals(expected, result, () -> "BigInteger value " + from + " should produce " + expected);
    }

    @Test
    void isBigIntegerNotZero_nullFrom_throwsNullPointerException() {
        // The method casts the argument to BigInteger and then calls compareTo,
        // which results in a NPE when the argument itself is null.
        assertThrows(NullPointerException.class, () -> NumberConversions.isBigIntegerNotZero(null, converter), "Passing null as the 'from' argument should throw NullPointerException");
    }

    @Test
    void isBigIntegerNotZero_wrongType_throwsClassCastException() {
        // Passing an Integer (or any non‑BigInteger) should cause a ClassCastException.
        Object notABigInteger = Integer.valueOf(42);
        assertThrows(ClassCastException.class, () -> NumberConversions.isBigIntegerNotZero(notABigInteger, converter), "Providing a non‑BigInteger should result in ClassCastException");
    }

    @Test
    void isBigIntegerNotZero_ignoresNonNullConverter() {
        // Create a Mockito mock that would fail the test if any method were invoked.
        Converter mockConverter = Mockito.mock(Converter.class);
        // Use a non‑zero BigInteger; the method should return true regardless of the converter.
        boolean result = NumberConversions.isBigIntegerNotZero(BigInteger.TEN, mockConverter);
        assertTrue(result, "Non‑zero BigInteger should return true even when a mock Converter is supplied");
        // Verify that the mock was never interacted with.
        Mockito.verifyNoInteractions(mockConverter);
    }

    @Test
    void isBigIntegerNotZero_ignoresCustomConverterImplementation() {
        // Custom Converter that throws an AssertionError if its convert method is called.
        Converter customConverter = new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object from, Class<?> toClass) {
                throw new AssertionError("Converter.convert should never be invoked by isBigIntegerNotZero");
            }
        };
        // The method should simply return true for a non‑zero value.
        boolean result = NumberConversions.isBigIntegerNotZero(BigInteger.valueOf(12345), customConverter);
        assertTrue(result, "Method must ignore the custom Converter and return true for non‑zero value");
    }
}
