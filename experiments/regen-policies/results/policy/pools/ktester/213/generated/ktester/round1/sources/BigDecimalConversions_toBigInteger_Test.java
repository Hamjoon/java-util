package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BigDecimalConversions_toBigInteger_Test {

    @Mock
    private Converter // Converter is not used by the method, but we provide a mock
    mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock instance automatically via @Mock
        // Ensure no unexpected interactions are recorded
        clearInvocations(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the converter mock was never interacted with,
        // because the method under test does not use it.
        verifyNoInteractions(mockConverter);
    }

    // Successful conversion scenarios
    Stream<Arguments> successfulCases() {
        return Stream.of(// basic truncation
        Arguments.of(new BigDecimal("12345.678"), null, BigInteger.valueOf(12345)), Arguments.of(new BigDecimal("-9876.54321"), null, BigInteger.valueOf(-9876)), // zero
        Arguments.of(BigDecimal.ZERO, null, BigInteger.ZERO), // very large integer (positive)
        Arguments.of(new BigDecimal("9999999999999999999999999999"), null, new BigInteger("9999999999999999999999999999")), // high‑precision fractional part that should be truncated
        Arguments.of(new BigDecimal("1234567890.000000001"), null, BigInteger.valueOf(1234567890)), // scale handling
        Arguments.of(new BigDecimal("12345.0"), null, BigInteger.valueOf(12345)), Arguments.of(new BigDecimal("12345E2"), null, BigInteger.valueOf(1234500)), Arguments.of(new BigDecimal("-0.000"), null, BigInteger.ZERO), Arguments.of(new BigDecimal("0.9999999999999999999999999999"), null, BigInteger.ZERO), // extreme magnitude
        Arguments.of(new BigDecimal("-9999999999999999999999999999"), null, new BigInteger("-9999999999999999999999999999")), Arguments.of(new BigDecimal("1E+1000"), null, new BigInteger("1" + "0".repeat(1000))), Arguments.of(new BigDecimal("-1E+1000"), null, new BigInteger("-" + "1" + "0".repeat(1000))), // non‑null converter argument (method ignores it)
        Arguments.of(new BigDecimal("42"), mockConverter, BigInteger.valueOf(42)));
    }

    // Exception scenarios (null source or wrong type)
    Stream<Arguments> exceptionCases() {
        return Stream.of(// null source
        Arguments.of(null, null, NullPointerException.class), // wrong types
        Arguments.of(Integer.valueOf(5), null, ClassCastException.class), Arguments.of("123", null, ClassCastException.class), Arguments.of(Double.valueOf(3.14), null, ClassCastException.class), Arguments.of(Long.valueOf(123L), null, ClassCastException.class), Arguments.of(Boolean.TRUE, null, ClassCastException.class), Arguments.of(new int[] { 1, 2, 3 }, null, ClassCastException.class), Arguments.of(new Object(), null, ClassCastException.class));
    }

    @Test
    void toBigInteger_returnsExactIntegerPart_forPositiveBigDecimal() {
        // given
        BigDecimal source = new BigDecimal("12345.678");
        BigInteger expected = new BigInteger("12345");
        // when
        BigInteger actual = BigDecimalConversions.toBigInteger(source, mockConverter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toBigInteger_returnsExactIntegerPart_forNegativeBigDecimal() {
        // given
        BigDecimal source = new BigDecimal("-9876.543");
        BigInteger expected = new BigInteger("-9876");
        // when
        BigInteger actual = BigDecimalConversions.toBigInteger(source, mockConverter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toBigInteger_returnsZero_whenSourceIsZero() {
        // given
        BigDecimal source = BigDecimal.ZERO;
        BigInteger expected = BigInteger.ZERO;
        // when
        BigInteger actual = BigDecimalConversions.toBigInteger(source, mockConverter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toBigInteger_throwsClassCastException_whenSourceIsNotBigDecimal() {
        // given
        Object notABigDecimal = "12345";
        // when / then
        assertThrows(ClassCastException.class, () -> BigDecimalConversions.toBigInteger(notABigDecimal, mockConverter));
    }

    @Test
    void toBigInteger_throwsNullPointerException_whenSourceIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BigDecimalConversions.toBigInteger(nullSource, mockConverter));
    }

    /**
     * Successful conversion cases – covers positive/negative numbers,
     * scale handling, scientific notation, extreme magnitudes and the
     * situation where a non‑null Converter is supplied.
     */
    @ParameterizedTest(name = "{index}: from={0}, converter={1} => expected={2}")
    @MethodSource("successfulCases")
    void toBigInteger_convertsCorrectly(Object from, Converter converter, BigInteger expected) {
        BigInteger actual = BigDecimalConversions.toBigInteger(from, converter);
        assertEquals(expected, actual);
    }

    /**
     * Exception cases – null source or source of an unsupported type.
     * The method should throw the documented exception type.
     */
    @ParameterizedTest(name = "{index}: from={0} => throws {2}")
    @MethodSource("exceptionCases")
    void toBigInteger_throwsExpectedException(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> BigDecimalConversions.toBigInteger(from, converter));
    }
}
