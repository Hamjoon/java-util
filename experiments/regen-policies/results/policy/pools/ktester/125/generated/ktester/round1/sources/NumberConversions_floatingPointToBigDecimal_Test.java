package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class NumberConversions_floatingPointToBigDecimal_Test {

    /**
     * The Converter used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance – the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> successfulPrimitiveAndWrapperProvider() {
        return Stream.of(arguments("Double value should be converted directly to a BigDecimal with the same numeric value.", 123.45d, BigDecimal.valueOf(123.45)), arguments("Float value is widened to double and then wrapped in a BigDecimal.", 12.3f, BigDecimal.valueOf(12.3)), arguments("Integer is converted to double (5.0) and then to BigDecimal.", 5, BigDecimal.valueOf(5.0)), arguments("Long is converted to double (100.0) and then to BigDecimal.", 100L, BigDecimal.valueOf(100.0)));
    }

    private static Stream<Arguments> successfulBigNumberAndStringProvider() {
        return Stream.of(arguments("BigDecimal is first turned into a double (loss of exact scale) and then wrapped.", new BigDecimal("123.456"), BigDecimal.valueOf(123.456)), arguments("BigInteger is converted to double (may lose precision) and then to BigDecimal.", new BigInteger("12345678901234567890"), BigDecimal.valueOf(1.2345678901234568E19)), arguments("String containing a valid floating‑point number is parsed to double and then to BigDecimal.", "45.67", BigDecimal.valueOf(45.67)));
    }

    private static Stream<Arguments> boundaryValuesProvider() {
        return Stream.of(arguments("Maximum finite double value should be converted preserving the exact double representation.", Double.MAX_VALUE, BigDecimal.valueOf(Double.MAX_VALUE)), arguments("Smallest positive non‑zero double (subnormal) should be converted preserving the exact value.", Double.MIN_VALUE, BigDecimal.valueOf(Double.MIN_VALUE)), arguments("A negative double should be correctly wrapped preserving sign.", -123.456d, BigDecimal.valueOf(-123.456)));
    }

    private static Stream<Arguments> specialFloatingPointProvider() {
        return Stream.of(arguments("NaN cannot be represented as a BigDecimal; should throw IllegalArgumentException.", Double.NaN), arguments("Positive infinity is not a finite double; conversion should raise IllegalArgumentException.", Double.POSITIVE_INFINITY), arguments("Negative infinity is not a finite double; conversion should raise IllegalArgumentException.", Double.NEGATIVE_INFINITY));
    }

    private static Stream<Arguments> atomicNumericProvider() {
        return Stream.of(arguments("AtomicInteger implements Number; its int value should be widened to double and wrapped.", new AtomicInteger(9), BigDecimal.valueOf(9.0)), arguments("AtomicLong implements Number; its long value should be converted to double then to a BigDecimal.", new AtomicLong(123456789L), BigDecimal.valueOf(123456789.0)));
    }

    private static Stream<Arguments> stringVariationsProvider() {
        return Stream.of(arguments("String with leading/trailing whitespace should be trimmed and parsed.", "  7.89  ", BigDecimal.valueOf(7.89)), arguments("String with explicit plus sign should be parsed correctly.", "+15.0", BigDecimal.valueOf(15.0)), arguments("Scientific notation string should be parsed to a double and then to a BigDecimal.", "6.022e23", BigDecimal.valueOf(6.022e23)), arguments("String representing negative zero should preserve the sign of zero.", "-0.0", BigDecimal.valueOf(-0.0)));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(arguments("java.util.Date is not a numeric source; conversion should fail.", new java.util.Date()), arguments("Character does not represent a numeric value; conversion should raise IllegalArgumentException.", new Character('A')));
    }

    private static Stream<Arguments> additionalExceptionProvider() {
        return Stream.of(arguments("BigDecimal representing NaN cannot be converted to a finite double; should throw IllegalArgumentException.", new BigDecimal("NaN"), IllegalArgumentException.class), arguments("BigDecimal representing Infinity results in Double.POSITIVE_INFINITY; should throw IllegalArgumentException.", new BigDecimal("Infinity"), IllegalArgumentException.class), arguments("BigDecimal representing -Infinity leads to Double.NEGATIVE_INFINITY; should throw IllegalArgumentException.", new BigDecimal("-Infinity"), IllegalArgumentException.class), arguments("Negative zero BigInteger is treated as zero; conversion should succeed.", new BigInteger("-0"), // null indicates no exception expected
        null), arguments("Empty string cannot be parsed to double; should throw NumberFormatException.", "", NumberFormatException.class), arguments("String with only whitespace is not a valid numeric literal; should throw NumberFormatException.", "   ", NumberFormatException.class), arguments("Arbitrary Object without numeric conversion should cause IllegalArgumentException.", new Object(), IllegalArgumentException.class), arguments("AtomicBoolean is not a numeric type; conversion should raise IllegalArgumentException.", new java.util.concurrent.atomic.AtomicBoolean(true), IllegalArgumentException.class), arguments("AtomicReference is unsupported for numeric conversion; should throw IllegalArgumentException.", new java.util.concurrent.atomic.AtomicReference<>(123), IllegalArgumentException.class), arguments("Temporal type LocalDate is not numeric; conversion must fail with IllegalArgumentException.", java.time.LocalDate.now(), IllegalArgumentException.class));
    }

    @Test
    void floatingPointToBigDecimal_WithDouble_ReturnsExactBigDecimal() {
        // given
        Double source = 123.456;
        // when
        BigDecimal result = NumberConversions.floatingPointToBigDecimal(source, converter);
        // then
        assertEquals(BigDecimal.valueOf(123.456), result, "Conversion of Double should produce a matching BigDecimal");
    }

    @Test
    void floatingPointToBigDecimal_WithFloat_ReturnsExactBigDecimal() {
        // given
        Float source = 78.9f;
        // when
        BigDecimal result = NumberConversions.floatingPointToBigDecimal(source, converter);
        // then
        // Float is widened to double before BigDecimal.valueOf, so we compare with the double value.
        assertEquals(BigDecimal.valueOf(78.9d), result, "Conversion of Float should produce a matching BigDecimal");
    }

    @Test
    void floatingPointToBigDecimal_WithNull_ReturnsZeroBigDecimal() {
        // given
        Object source = null;
        // when
        BigDecimal result = NumberConversions.floatingPointToBigDecimal(source, converter);
        // then
        assertEquals(BigDecimal.valueOf(0.0), result, "Null input should be treated as 0.0 and converted to a zero BigDecimal");
    }

    @Test
    void floatingPointToBigDecimal_WithStringNumber_ReturnsBigDecimal() {
        // given
        String source = "42.42";
        // when
        BigDecimal result = NumberConversions.floatingPointToBigDecimal(source, converter);
        // then
        assertEquals(BigDecimal.valueOf(42.42), result, "String representing a number should be converted via the Converter to the correct BigDecimal");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests – successful conversions (primitive & wrappers)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulPrimitiveAndWrapperProvider")
    void floatingPointToBigDecimal_SuccessfulPrimitiveAndWrapper(String description, Object from, BigDecimal expected) {
        BigDecimal result = NumberConversions.floatingPointToBigDecimal(from, converter);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests – successful conversions (BigDecimal, BigInteger, strings)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulBigNumberAndStringProvider")
    void floatingPointToBigDecimal_SuccessfulBigNumberAndString(String description, Object from, BigDecimal expected) {
        BigDecimal result = NumberConversions.floatingPointToBigDecimal(from, converter);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Edge cases – null and unsupported types
     * -------------------------------------------------------------- */
    @Test
    void floatingPointToBigDecimal_NullInput_ReturnsZero() {
        BigDecimal result = NumberConversions.floatingPointToBigDecimal(null, converter);
        assertEquals(BigDecimal.valueOf(0.0), result, "When the source object is null, toDouble returns 0.0; the method therefore returns BigDecimal zero.");
    }

    @Test
    void floatingPointToBigDecimal_BooleanUnsupported_ThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.floatingPointToBigDecimal(true, converter), "Boolean is not a supported numeric source; should throw IllegalArgumentException.");
    }

    /* --------------------------------------------------------------
     *  Malformed numeric strings
     * -------------------------------------------------------------- */
    @Test
    void floatingPointToBigDecimal_MalformedString_ThrowsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> NumberConversions.floatingPointToBigDecimal("not-a-number", converter), "A non‑numeric string cannot be parsed to double; should throw NumberFormatException.");
    }

    /* --------------------------------------------------------------
     *  Custom Converter influence (mocked)
     * -------------------------------------------------------------- */
    @Test
    void floatingPointToBigDecimal_CustomConverterReturns42_ReturnsBigDecimal42() {
        // Arrange – a mock Converter that knows how to turn the literal "custom" into 42.0
        Converter mockConverter = Mockito.mock(Converter.class);
        when(mockConverter.convert(eq("custom"), eq(Double.class))).thenReturn(42.0);
        // Act
        BigDecimal result = NumberConversions.floatingPointToBigDecimal("custom", mockConverter);
        // Assert
        assertEquals(BigDecimal.valueOf(42.0), result, "When a non‑numeric object is supplied but a Converter can convert it, the result should be 42.0.");
    }

    /* --------------------------------------------------------------
     *  Boundary numeric values
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryValuesProvider")
    void floatingPointToBigDecimal_BoundaryValues(String description, double from, BigDecimal expected) {
        BigDecimal result = NumberConversions.floatingPointToBigDecimal(from, converter);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Special floating‑point values (NaN, infinities)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("specialFloatingPointProvider")
    void floatingPointToBigDecimal_SpecialFloatingPointValues(String description, double from) {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.floatingPointToBigDecimal(from, converter), description);
    }

    /* --------------------------------------------------------------
     *  Atomic numeric types
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("atomicNumericProvider")
    void floatingPointToBigDecimal_AtomicNumericTypes(String description, Object from, BigDecimal expected) {
        BigDecimal result = NumberConversions.floatingPointToBigDecimal(from, converter);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  String variations (whitespace, signs, scientific notation, negative zero)
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringVariationsProvider")
    void floatingPointToBigDecimal_StringVariations(String description, String from, BigDecimal expected) {
        BigDecimal result = NumberConversions.floatingPointToBigDecimal(from, converter);
        assertEquals(expected, result, description);
    }

    /* --------------------------------------------------------------
     *  Unsupported non‑numeric types
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedTypesProvider")
    void floatingPointToBigDecimal_UnsupportedTypes(String description, Object from) {
        assertThrows(IllegalArgumentException.class, () -> NumberConversions.floatingPointToBigDecimal(from, converter), description);
    }

    /* --------------------------------------------------------------
     *  Additional exception scenarios for floatingPointToBigDecimal
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("additionalExceptionProvider")
    void floatingPointToBigDecimal_AdditionalExceptions(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.floatingPointToBigDecimal(from, converter), description);
    }

    /**
     * Special handling for the case where no exception is expected (negative‑zero BigInteger).
     * The parameterized test above supplies {@code null} as the expected exception class.
     */
    @Test
    void floatingPointToBigDecimal_NegativeZeroBigInteger_ReturnsZero() {
        BigInteger negZero = new BigInteger("-0");
        BigDecimal result = NumberConversions.floatingPointToBigDecimal(negZero, converter);
        assertEquals(BigDecimal.valueOf(0.0), result, "Negative zero BigInteger should be treated as zero and produce a zero BigDecimal.");
    }
}
