package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_floatingPointToBigDecimal_Test {

    /**
     * A real Converter instance that uses the library’s default conversion rules.
     */
    private static Converter realConverter;

    /**
     * A mock Converter used for the “mocked‑converter” test case.
     */
    @Mock
    private Converter mockedConverter;

    /**
     * Simple POJO used to test custom {@link Converter} behaviour.
     */
    private static final class CustomNumber {

        final double value;

        CustomNumber(double value) {
            this.value = value;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor
        // creates an empty options set which is sufficient for the tests.
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        realConverter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// primitive double (autoboxed)
        Arguments.of(123.456d, BigDecimal.valueOf(123.456)), // Float object
        Arguments.of(new Float(78.9f), BigDecimal.valueOf(78.9)), // String containing a floating‑point number
        Arguments.of("-45.67", BigDecimal.valueOf(-45.67)), // Already a BigDecimal – conversion goes through double, so we mimic that
        Arguments.of(new BigDecimal("0.001"), BigDecimal.valueOf(new BigDecimal("0.001").doubleValue())), // AtomicInteger
        Arguments.of(new AtomicInteger(5), BigDecimal.valueOf(5.0)), // Large BigInteger (precision loss is acceptable – we compare the double‑based value)
        Arguments.of(new BigInteger("12345678901234567890"), BigDecimal.valueOf(new BigInteger("12345678901234567890").doubleValue())), // java.util.Date (epoch 0) – treated as 0.0 by toDouble
        Arguments.of(new Date(0L), BigDecimal.valueOf(0.0)), // Double.NaN – should cause NumberFormatException, handled in exception test
        // Double.POSITIVE_INFINITY – same, handled in exception test
        // AtomicLong
        Arguments.of(new AtomicLong(123456789L), BigDecimal.valueOf(123456789.0)), // primitive short (autoboxed to Short)
        Arguments.of((short) -12, BigDecimal.valueOf(-12.0)), // primitive byte (autoboxed to Byte)
        Arguments.of((byte) 7, BigDecimal.valueOf(7.0)), // Integer object
        Arguments.of(new Integer(42), BigDecimal.valueOf(42.0)), // Long object
        Arguments.of(new Long(9876543210L), BigDecimal.valueOf(9876543210.0)));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// null source + null converter → NPE inside toDouble
        Arguments.of(null, NullPointerException.class, "Null source with null converter should cause NullPointerException"), // Invalid numeric string
        Arguments.of("not a number", NumberFormatException.class, "Non‑numeric string should trigger NumberFormatException"), // NaN – BigDecimal.valueOf throws NumberFormatException
        Arguments.of(Double.NaN, NumberFormatException.class, "NaN cannot be represented as a BigDecimal"), // Positive infinity – same as NaN
        Arguments.of(Double.POSITIVE_INFINITY, NumberFormatException.class, "Infinity cannot be represented as a BigDecimal"), // Boolean unsupported
        Arguments.of(true, IllegalArgumentException.class, "Boolean without a Converter should be rejected"), // Character unsupported
        Arguments.of('a', IllegalArgumentException.class, "Character without a Converter should be rejected"), // Empty string
        Arguments.of("", NumberFormatException.class, "Empty string cannot be parsed to double"), // Whitespace‑only string
        Arguments.of("   ", NumberFormatException.class, "Whitespace‑only string cannot be parsed to double"), // Array type
        Arguments.of(new int[] { 1, 2, 3 }, IllegalArgumentException.class, "Array without a Converter should be rejected"), // Map type
        Arguments.of(new HashMap<>(), IllegalArgumentException.class, "Map without a Converter should be rejected"));
    }

    @Test
    void shouldConvertDoubleToBigDecimal() {
        // given
        Double source = 123.456;
        BigDecimal expected = BigDecimal.valueOf(source);
        // when
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(source, realConverter);
        // then
        assertEquals(expected, actual, "Double value should be converted to an equivalent BigDecimal");
    }

    @Test
    void shouldConvertFloatToBigDecimal() {
        // given
        Float source = 78.9f;
        BigDecimal expected = BigDecimal.valueOf(source.doubleValue());
        // when
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(source, realConverter);
        // then
        assertEquals(expected, actual, "Float value should be converted to an equivalent BigDecimal");
    }

    @Test
    void shouldConvertIntegerToBigDecimal() {
        // given
        Integer source = 42;
        BigDecimal expected = BigDecimal.valueOf(source.doubleValue());
        // when
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(source, realConverter);
        // then
        assertEquals(expected, actual, "Integer value should be converted to an equivalent BigDecimal");
    }

    @Test
    void shouldConvertStringNumericToBigDecimal() {
        // given
        String source = "9876.54321";
        double parsed = Double.parseDouble(source);
        BigDecimal expected = BigDecimal.valueOf(parsed);
        // when
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(source, realConverter);
        // then
        assertEquals(expected, actual, "Numeric String should be parsed and converted to a BigDecimal");
    }

    @Test
    void shouldUseMockedConverterWhenProvided() {
        // given
        // any object – the mock will dictate the result
        Object customSource = new Object();
        double mockedResult = 5.55;
        BigDecimal expected = BigDecimal.valueOf(mockedResult);
        // Stub the mock: whenever the converter is asked to produce a Double,
        // return the predefined mockedResult.
        // The exact signature of Converter.convert is not part of the public contract
        // we are testing, so we use a lenient stub that matches any arguments.
        when(mockedConverter.convert(any(), eq(Double.class))).thenReturn(mockedResult);
        // when
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(customSource, mockedConverter);
        // then
        assertEquals(expected, actual, "Method should forward the double supplied by the mocked Converter");
        // Verify that the mock was indeed consulted.
        verify(mockedConverter, atLeastOnce()).convert(any(), eq(Double.class));
    }

    @Test
    void shouldReturnZeroBigDecimalWhenNullIsProvided() {
        // given
        Object source = null;
        // According to NumberConversions.toDouble implementation, a null source yields 0.0.
        BigDecimal expected = BigDecimal.valueOf(0.0);
        // when
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(source, realConverter);
        // then
        assertEquals(expected, actual, "Null input should be converted to a BigDecimal representing 0.0");
    }

    /**
     * Successful conversions when the supplied {@code Converter} is {@code null}.
     * The method should rely on {@link NumberConversions#toDouble(Object, Converter)}.
     */
    @ParameterizedTest(name = "[{index}] from={0} => {1}")
    @MethodSource("validConversionProvider")
    void shouldConvertVariousSourcesToBigDecimal(Object from, BigDecimal expected) {
        // when
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(from, null);
        // then
        assertEquals(expected, actual, () -> "Conversion of " + (from == null ? "null" : from.getClass().getSimpleName()) + " should yield " + expected);
    }

    /**
     * Scenarios where {@code floatingPointToBigDecimal} is expected to throw
     * an exception (either from {@code toDouble} or from {@code BigDecimal.valueOf}).
     */
    @ParameterizedTest(name = "[{index}] from={0} ⇒ {2}")
    @MethodSource("exceptionProvider")
    void shouldThrowWhenConversionIsInvalid(Object from, Class<? extends Throwable> expectedException, String description) {
        // when / then
        assertThrows(expectedException, () -> NumberConversions.floatingPointToBigDecimal(from, null), description);
    }

    @Test
    void shouldUseCustomConverterReturningDouble() {
        // given
        CustomNumber source = new CustomNumber(5.5);
        when(mockedConverter.convert(any(), eq(Double.class))).thenReturn(5.5);
        BigDecimal expected = BigDecimal.valueOf(5.5);
        // when
        BigDecimal actual = NumberConversions.floatingPointToBigDecimal(source, mockedConverter);
        // then
        assertEquals(expected, actual, "Result should reflect the double supplied by the custom Converter");
        verify(mockedConverter, atLeastOnce()).convert(any(), eq(Double.class));
    }

    @Test
    void shouldPropagateExceptionFromCustomConverter() {
        // given
        CustomNumber source = new CustomNumber(0);
        when(mockedConverter.convert(any(), eq(Double.class))).thenThrow(new IllegalArgumentException("cannot convert"));
        // when / then
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> NumberConversions.floatingPointToBigDecimal(source, mockedConverter), "Exception from the supplied Converter must propagate");
        assertEquals("cannot convert", thrown.getMessage());
        verify(mockedConverter, atLeastOnce()).convert(any(), eq(Double.class));
    }

    @Test
    void shouldThrowWhenCustomConverterReturnsNaN() {
        // given
        CustomNumber source = new CustomNumber(0);
        when(mockedConverter.convert(any(), eq(Double.class))).thenReturn(Double.NaN);
        // when / then
        assertThrows(NumberFormatException.class, () -> NumberConversions.floatingPointToBigDecimal(source, mockedConverter), "BigDecimal.valueOf should reject NaN");
        verify(mockedConverter, atLeastOnce()).convert(any(), eq(Double.class));
    }

    @Test
    void shouldThrowWhenCustomConverterReturnsInfinity() {
        // given
        CustomNumber source = new CustomNumber(0);
        when(mockedConverter.convert(any(), eq(Double.class))).thenReturn(Double.POSITIVE_INFINITY);
        // when / then
        assertThrows(NumberFormatException.class, () -> NumberConversions.floatingPointToBigDecimal(source, mockedConverter), "BigDecimal.valueOf should reject infinite values");
        verify(mockedConverter, atLeastOnce()).convert(any(), eq(Double.class));
    }

    @Test
    void shouldPropagateNullPointerExceptionFromCustomConverter() {
        // given
        CustomNumber source = new CustomNumber(0);
        when(mockedConverter.convert(any(), eq(Double.class))).thenThrow(new NullPointerException("converter NPE"));
        // when / then
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> NumberConversions.floatingPointToBigDecimal(source, mockedConverter), "NullPointerException from Converter must propagate");
        assertEquals("converter NPE", thrown.getMessage());
        verify(mockedConverter, atLeastOnce()).convert(any(), eq(Double.class));
    }
}
