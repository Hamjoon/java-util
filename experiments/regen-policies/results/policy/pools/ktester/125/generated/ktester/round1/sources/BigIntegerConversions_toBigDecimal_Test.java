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
    private Converter // not used by the method but required by signature
    mockConverter;

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// zero, converter null
        Arguments.of(BigInteger.ZERO, null, BigDecimal.ZERO), // positive small value, converter null
        Arguments.of(BigInteger.valueOf(12345), null, new BigDecimal("12345")), // negative small value, converter null
        Arguments.of(BigInteger.valueOf(-98765), null, new BigDecimal("-98765")), // very large positive value, converter null
        Arguments.of(new BigInteger("123456789012345678901234567890"), null, new BigDecimal("123456789012345678901234567890")), // very large negative value, converter null
        Arguments.of(new BigInteger("-123456789012345678901234567890"), null, new BigDecimal("-123456789012345678901234567890")), // Long.MAX_VALUE wrapped as BigInteger, converter null
        Arguments.of(BigInteger.valueOf(Long.MAX_VALUE), null, new BigDecimal("9223372036854775807")), // Long.MIN_VALUE wrapped as BigInteger, converter null
        Arguments.of(BigInteger.valueOf(Long.MIN_VALUE), null, new BigDecimal("-9223372036854775808")), // non‑null Converter (real instance) – the method should ignore it
        Arguments.of(BigInteger.valueOf(42), new Converter(), new BigDecimal("42")), // non‑null Converter with zero
        Arguments.of(BigInteger.ZERO, new Converter(), BigDecimal.ZERO));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(// null source – NPE because of cast
        Arguments.of(null, null, NullPointerException.class), // String instead of BigInteger – ClassCastException
        Arguments.of("not a big integer", null, ClassCastException.class), // Integer – ClassCastException
        Arguments.of(Integer.valueOf(42), null, ClassCastException.class), // BigDecimal – ClassCastException
        Arguments.of(BigDecimal.valueOf(10.5), null, ClassCastException.class), // Long – ClassCastException
        Arguments.of(Long.valueOf(100L), null, ClassCastException.class), // Date – ClassCastException
        Arguments.of(new java.util.Date(), null, ClassCastException.class));
    }

    @Test
    void toBigDecimal_convertsPositiveBigInteger() {
        // given
        BigInteger source = new BigInteger("123456789012345678901234567890");
        // when
        BigDecimal result = BigIntegerConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(new BigDecimal(source), result);
    }

    @Test
    void toBigDecimal_convertsZero() {
        // given
        BigInteger source = BigInteger.ZERO;
        // when
        BigDecimal result = BigIntegerConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void toBigDecimal_convertsNegativeBigInteger() {
        // given
        BigInteger source = new BigInteger("-98765432109876543210");
        // when
        BigDecimal result = BigIntegerConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(new BigDecimal(source), result);
    }

    @Test
    void toBigDecimal_nullInput_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toBigDecimal(source, mockConverter));
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("validConversionProvider")
    void toBigDecimal_convertsValidBigInteger(Object from, Converter converter, BigDecimal expected) {
        // when
        BigDecimal result = BigIntegerConversions.toBigDecimal(from, converter);
        // then
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("invalidConversionProvider")
    void toBigDecimal_invalidInputs_throwsException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> BigIntegerConversions.toBigDecimal(from, converter));
    }
}
