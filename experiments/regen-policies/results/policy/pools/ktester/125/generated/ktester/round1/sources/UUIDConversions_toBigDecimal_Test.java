package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class UUIDConversions_toBigDecimal_Test {

    private Converter converter;

    @Mock
    private Converter // a mock that always throws ConversionException
    mockConverterThatThrows;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter needs a ConverterOptions instance; using the default options.
        converter = new Converter(new ConverterOptions());
    }

    /* --------------------------------------------------------------
     *  Helper to stub the Converter for a given source object.
     *  The real Converter API is {@code <T> T convert(Object from, Class<T> toClass)}.
     * -------------------------------------------------------------- */
    private void stubConverter(Object from, BigInteger asBigInteger) {
        //noinspection unchecked
        when(converter.convert(eq(from), eq(BigInteger.class))).thenReturn(asBigInteger);
    }

    /* --------------------------------------------------------------
     *  VALID CONVERSIONS
     * -------------------------------------------------------------- */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// UUID with value 1
        Arguments.of(UUID.fromString("00000000-0000-0000-0000-000000000001"), new BigInteger("1")), // Direct BigInteger
        Arguments.of(new BigInteger("12345678901234567890"), new BigInteger("12345678901234567890")), // Numeric String
        Arguments.of("9876543210", new BigInteger("9876543210")), // UUID all‑zero
        Arguments.of(UUID.fromString("00000000-0000-0000-0000-000000000000"), BigInteger.ZERO), // UUID max value (2^128‑1)
        Arguments.of(UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"), new BigInteger("340282366920938463463374607431768211455")), // Integer
        Arguments.of(Integer.valueOf(123), new BigInteger("123")), // Long (negative)
        Arguments.of(Long.valueOf(-456L), new BigInteger("-456")), // Double (fractional part discarded)
        Arguments.of(Double.valueOf(12.34), new BigInteger("12")), // String with surrounding whitespace
        Arguments.of("   98765   ", new BigInteger("98765")), // BigDecimal (fractional part discarded)
        Arguments.of(new BigDecimal("12345.6789"), new BigInteger("12345")));
    }

    /* --------------------------------------------------------------
     *  UNSUPPORTED TYPES – should raise IllegalArgumentException
     * -------------------------------------------------------------- */
    private static Stream<Arguments> unsupportedTypeProvider() {
        return Stream.of(Arguments.of(new Object()), Arguments.of(new Date()));
    }

    /* --------------------------------------------------------------
     *  STRING PARSING ERRORS – NumberFormatException
     * -------------------------------------------------------------- */
    private static Stream<Arguments> numberFormatErrorProvider() {
        return Stream.of(Arguments.of("notANumber"), Arguments.of(""), Arguments.of("   "), Arguments.of("123abc"), Arguments.of("-"));
    }

    /* --------------------------------------------------------------
     *  NON‑FINITE FLOATING‑POINT VALUES – NumberFormatException
     * -------------------------------------------------------------- */
    private static Stream<Arguments> nonFiniteFloatingProvider() {
        return Stream.of(Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Float.NEGATIVE_INFINITY));
    }

    @Test
    void toBigDecimal_givenUuid_returnsBigDecimalRepresentingSameBigInteger() {
        // GIVEN
        UUID uuid = UUID.randomUUID();
        // WHEN
        BigDecimal actual = UUIDConversions.toBigDecimal(uuid, converter);
        // THEN
        // Compute the expected value using the same internal conversion that
        // toBigDecimal relies on.
        BigInteger expectedBigInt = UUIDConversions.toBigInteger(uuid, converter);
        BigDecimal expected = new BigDecimal(expectedBigInt);
        assertEquals(expected, actual, "toBigDecimal should produce a BigDecimal that wraps the BigInteger conversion of the UUID");
    }

    @Test
    void toBigDecimal_givenBigInteger_returnsEquivalentBigDecimal() {
        // GIVEN
        BigInteger bigInt = new BigInteger("123456789012345678901234567890");
        // The Converter can convert a BigInteger to a BigInteger (identity), so we can pass it directly.
        // This also exercises the code path where the source object is already a BigInteger.
        // WHEN
        BigDecimal actual = UUIDConversions.toBigDecimal(bigInt, converter);
        // THEN
        BigDecimal expected = new BigDecimal(bigInt);
        assertEquals(expected, actual);
    }

    @Test
    void toBigDecimal_givenNull_throwsNullPointerException() {
        // GIVEN
        Object nullObj = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigDecimal(nullObj, converter));
    }

    @ParameterizedTest(name = "[{index}] toBigDecimal converts {0} → {1}")
    @MethodSource("validConversionProvider")
    void toBigDecimal_validConversions(Object from, BigInteger expectedBigInt) {
        // Arrange – stub the converter for the given source object
        stubConverter(from, expectedBigInt);
        // Act
        BigDecimal result = UUIDConversions.toBigDecimal(from, converter);
        // Assert
        BigDecimal expected = new BigDecimal(expectedBigInt);
        assertEquals(expected, result, () -> "Expected BigDecimal " + expected + " for input " + from);
    }

    /* --------------------------------------------------------------
     *  NULL HANDLING
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_givenNullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigDecimal(null, converter));
    }

    @Test
    void toBigDecimal_givenNullConverter_throwsNullPointerException() {
        UUID uuid = UUID.randomUUID();
        assertThrows(NullPointerException.class, () -> UUIDConversions.toBigDecimal(uuid, null));
    }

    @ParameterizedTest(name = "[{index}] toBigDecimal unsupported type {0}")
    @MethodSource("unsupportedTypeProvider")
    void toBigDecimal_unsupportedTypes(Object from) {
        // No stubbing – the method should fail before delegating to the converter.
        assertThrows(IllegalArgumentException.class, () -> UUIDConversions.toBigDecimal(from, converter));
    }

    @ParameterizedTest(name = "[{index}] toBigDecimal malformed string \"{0}\"")
    @MethodSource("numberFormatErrorProvider")
    void toBigDecimal_malformedStringThrowsNumberFormatException(String from) {
        // Stub the converter to attempt conversion – it will internally try to parse the string.
        // The stub is not actually used because parsing fails inside toBigInteger.
        // Still, we provide a dummy return to keep the mock happy if it is called.
        when(converter.convert(eq(from), eq(BigInteger.class))).thenCallRealMethod();
        assertThrows(NumberFormatException.class, () -> UUIDConversions.toBigDecimal(from, converter));
    }

    @ParameterizedTest(name = "[{index}] toBigDecimal non‑finite floating value {0}")
    @MethodSource("nonFiniteFloatingProvider")
    void toBigDecimal_nonFiniteFloatingValuesThrowNumberFormatException(Object from) {
        // Stub the converter – same reasoning as the malformed string case.
        when(converter.convert(eq(from), eq(BigInteger.class))).thenCallRealMethod();
        assertThrows(NumberFormatException.class, () -> UUIDConversions.toBigDecimal(from, converter));
    }

    /* --------------------------------------------------------------
     *  CONVERTER EXCEPTION PROPAGATION
     * -------------------------------------------------------------- */
    @Test
    void toBigDecimal_converterThrowsConversionException_propagates() {
        // Arrange – configure the mock to throw when asked to convert the given input.
        String unsupported = "someUnsupportedType";
        when(mockConverterThatThrows.convert(eq(unsupported), eq(BigInteger.class))).thenThrow(new ConversionException("cannot convert"));
        // Act & Assert
        ConversionException ex = assertThrows(ConversionException.class, () -> UUIDConversions.toBigDecimal(unsupported, mockConverterThatThrows));
        assertEquals("cannot convert", ex.getMessage());
    }
}
