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
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_isBigIntegerNotZero_Test {

    /**
     * The Converter is a dependency of the method, but it is not used in the current implementation.
     *  It is mocked only to satisfy the method signature.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> zeroValuesProvider() {
        return Stream.of(// literal constant
        Arguments.of(BigInteger.ZERO), // constructed from string "0"
        Arguments.of(new BigInteger("0")), // subtraction that yields zero
        Arguments.of(new BigInteger("5").subtract(new BigInteger("5"))), // valueOf factory method
        Arguments.of(BigInteger.valueOf(0L)));
    }

    private static Stream<Arguments> nonZeroValuesProvider() {
        return Stream.of(// a normal positive value
        Arguments.of(new BigInteger("12345678901234567890")), // a normal negative value
        Arguments.of(new BigInteger("-9876543210987654321")), // very large positive value
        Arguments.of(new BigInteger("12345678901234567890123456789012345678901234567890")), // very large negative value
        Arguments.of(new BigInteger("-12345678901234567890123456789012345678901234567890")));
    }

    @Test
    void isBigIntegerNotZero_returnsFalseWhenZero() {
        // given
        BigInteger zero = BigInteger.ZERO;
        // when
        boolean result = NumberConversions.isBigIntegerNotZero(zero, mockConverter);
        // then
        assertFalse(result, "Zero should be reported as not non‑zero");
    }

    @Test
    void isBigIntegerNotZero_returnsTrueWhenPositiveNonZero() {
        // given
        BigInteger positive = new BigInteger("123456789");
        // when
        boolean result = NumberConversions.isBigIntegerNotZero(positive, mockConverter);
        // then
        assertTrue(result, "A positive non‑zero BigInteger should be reported as non‑zero");
    }

    @Test
    void isBigIntegerNotZero_returnsTrueWhenNegativeNonZero() {
        // given
        BigInteger negative = new BigInteger("-987654321");
        // when
        boolean result = NumberConversions.isBigIntegerNotZero(negative, mockConverter);
        // then
        assertTrue(result, "A negative non‑zero BigInteger should be reported as non‑zero");
    }

    @Test
    void isBigIntegerNotZero_throwsClassCastExceptionWhenNotBigInteger() {
        // given
        Object notABigInteger = "not a big integer";
        // when / then
        assertThrows(ClassCastException.class, () -> NumberConversions.isBigIntegerNotZero(notABigInteger, mockConverter), "Passing a non‑BigInteger object should result in a ClassCastException");
    }

    @ParameterizedTest(name = "{index} => from={0}")
    @MethodSource("nonZeroValuesProvider")
    void isBigIntegerNotZero_returnsTrueWhenNonZero(BigInteger from) {
        // when
        boolean result = NumberConversions.isBigIntegerNotZero(from, mockConverter);
        // then
        assertTrue(result, "A non‑zero BigInteger should be reported as non‑zero");
    }

    @Test
    void isBigIntegerNotZero_throwsNullPointerExceptionWhenFromIsNull() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> NumberConversions.isBigIntegerNotZero(from, mockConverter), "Passing null should cause a NullPointerException when the cast is attempted");
    }

    @Test
    void isBigIntegerNotZero_throwsClassCastExceptionWhenNotBigInteger_String() {
        // given
        Object from = "12345";
        // when / then
        assertThrows(ClassCastException.class, () -> NumberConversions.isBigIntegerNotZero(from, mockConverter), "A String cannot be cast to BigInteger, should throw ClassCastException");
    }

    @Test
    void isBigIntegerNotZero_throwsClassCastExceptionWhenNotBigInteger_Integer() {
        // given
        Object from = Integer.valueOf(10);
        // when / then
        assertThrows(ClassCastException.class, () -> NumberConversions.isBigIntegerNotZero(from, mockConverter), "An Integer cannot be cast to BigInteger, should throw ClassCastException");
    }

    @Test
    void isBigIntegerNotZero_ignoresNonNullConverter() {
        // given
        BigInteger from = BigInteger.ONE;
        Converter nonNullConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> tgt) {
                // This implementation should never be called.
                fail("Converter should not be invoked");
                return null;
            }
        };
        // when
        boolean result = NumberConversions.isBigIntegerNotZero(from, nonNullConverter);
        // then
        assertTrue(result, "Non‑null converter must be ignored and result should be true for ONE");
    }

    @Test
    void isBigIntegerNotZero_ignoresConverterThatThrowsIfUsed() {
        // given
        BigInteger from = BigInteger.ONE;
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> tgt) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // when
        boolean result = NumberConversions.isBigIntegerNotZero(from, throwingConverter);
        // then
        assertTrue(result, "Even a throwing Converter must be ignored; result should be true for ONE");
    }
}
