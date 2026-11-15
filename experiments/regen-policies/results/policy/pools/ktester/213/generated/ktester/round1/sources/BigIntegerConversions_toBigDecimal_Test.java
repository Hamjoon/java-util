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
class BigIntegerConversions_toBigDecimal_Test {

    @Mock
    private Converter // not used by the method but required by signature
    converter;

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(Arguments.of(BigInteger.ZERO, BigDecimal.ZERO, "Convert a zero BigInteger to BigDecimal; should return BigDecimal.ZERO."), Arguments.of(new BigInteger("12345678901234567890"), new BigDecimal("12345678901234567890"), "Convert a large positive BigInteger to BigDecimal; verifies handling of values beyond long range."), Arguments.of(BigInteger.valueOf(-42), new BigDecimal("-42"), "Convert a negative BigInteger to BigDecimal; ensures sign is preserved."));
    }

    private static Stream<Arguments> errorHandlingProvider() {
        return Stream.of(Arguments.of(null, NullPointerException.class, "Passing a null `from` argument should cause a NullPointerException because the method attempts to cast it to BigInteger."), Arguments.of(Integer.valueOf(123), ClassCastException.class, "Providing an object that is not a BigInteger (e.g., Integer) must result in a ClassCastException."), Arguments.of("not a number", ClassCastException.class, "Supplying a String instead of a BigInteger should also trigger a ClassCastException."));
    }

    private static Stream<Arguments> boundaryValuesProvider() {
        BigInteger twoPow1024 = BigInteger.ONE.shiftLeft(1024);
        BigInteger negTwoPow1024 = twoPow1024.negate();
        return Stream.of(Arguments.of(BigInteger.valueOf(Long.MAX_VALUE), new BigDecimal(Long.MAX_VALUE), "Convert the maximum long value wrapped in a BigInteger to ensure correct handling at the positive 64‑bit boundary."), Arguments.of(BigInteger.valueOf(Long.MIN_VALUE), new BigDecimal(Long.MIN_VALUE), "Convert the minimum long value (negative) wrapped in a BigInteger to verify sign handling at the negative 64‑bit boundary."), Arguments.of(twoPow1024, new BigDecimal(twoPow1024), "Convert a very large (2^1024) positive BigInteger to test the method's ability to handle numbers far beyond the range of primitive types."), Arguments.of(negTwoPow1024, new BigDecimal(negTwoPow1024), "Convert a very large negative BigInteger (‑2^1024) to ensure correct sign preservation for huge magnitudes."));
    }

    private static Stream<Arguments> converterUsageProvider() {
        // Create a dummy Converter instance – using a mock is sufficient because the method never calls it.
        Converter dummyConverter = mock(Converter.class);
        Converter mockedConverter = mock(Converter.class);
        return Stream.of(Arguments.of(BigInteger.valueOf(777), new BigDecimal("777"), dummyConverter, "Pass a non‑null Converter instance; the method should ignore it and still correctly convert the BigInteger."), Arguments.of(BigInteger.ZERO, BigDecimal.ZERO, mockedConverter, "Using a mocked Converter verifies that the conversion logic does not depend on the Converter argument."));
    }

    @Test
    void toBigDecimal_positiveBigInteger() {
        // given
        BigInteger input = new BigInteger("123456789012345678901234567890");
        BigDecimal expected = new BigDecimal(input);
        // when
        BigDecimal actual = BigIntegerConversions.toBigDecimal(input, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toBigDecimal_zeroBigInteger() {
        // given
        BigInteger input = BigInteger.ZERO;
        BigDecimal expected = BigDecimal.ZERO;
        // when
        BigDecimal actual = BigIntegerConversions.toBigDecimal(input, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toBigDecimal_negativeBigInteger() {
        // given
        BigInteger input = new BigInteger("-98765432109876543210");
        BigDecimal expected = new BigDecimal(input);
        // when
        BigDecimal actual = BigIntegerConversions.toBigDecimal(input, converter);
        // then
        assertEquals(expected, actual);
    }

    @Test
    void toBigDecimal_nullInput_throwsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BigIntegerConversions.toBigDecimal(input, converter));
    }

    /* -------------------------------------------------------------
     *  Successful conversions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("successfulConversionProvider")
    void toBigDecimal_successfulConversions(BigInteger input, BigDecimal expected, String description) {
        // when
        BigDecimal actual = BigIntegerConversions.toBigDecimal(input, null);
        // then
        assertEquals(expected, actual, description);
    }

    /* -------------------------------------------------------------
     *  Error handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("errorHandlingProvider")
    void toBigDecimal_errorHandling(Object input, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> BigIntegerConversions.toBigDecimal(input, null), description);
    }

    /* -------------------------------------------------------------
     *  Boundary values
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("boundaryValuesProvider")
    void toBigDecimal_boundaryValues(BigInteger input, BigDecimal expected, String description) {
        BigDecimal actual = BigIntegerConversions.toBigDecimal(input, null);
        assertEquals(expected, actual, description);
    }

    /* -------------------------------------------------------------
     *  Converter parameter usage
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("converterUsageProvider")
    void toBigDecimal_converterParameterUsage(BigInteger input, BigDecimal expected, Converter conv, String description) {
        // The method ignores the converter argument; we merely verify it does not affect the result.
        BigDecimal actual = BigIntegerConversions.toBigDecimal(input, conv);
        assertEquals(expected, actual, description);
    }
}
