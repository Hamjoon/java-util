package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_integerTypeToBigInteger_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use a real Converter with default options – this gives us the full conversion
        // machinery that NumberConversions relies on.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Provides a stream of valid conversion arguments.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>the source object (the {@code from} parameter)</li>
     *   <li>the expected {@link BigInteger} result</li>
     *   <li>a textual description (used as the test name)</li>
     * </ul>
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(Arguments.of(Long.valueOf(123L), BigInteger.valueOf(123L), "Convert a Long instance to BigInteger"), Arguments.of(Integer.valueOf(456), BigInteger.valueOf(456L), "Convert an Integer instance to BigInteger"), Arguments.of(Short.valueOf((short) 7), BigInteger.valueOf(7L), "Convert a Short instance to BigInteger"), Arguments.of(Byte.valueOf((byte) 9), BigInteger.valueOf(9L), "Convert a Byte instance to BigInteger"), Arguments.of(BigInteger.valueOf(999), BigInteger.valueOf(999L), "Pass a BigInteger directly; should be returned unchanged"), Arguments.of(BigDecimal.valueOf(12345.67), BigInteger.valueOf(12345L), "Convert a BigDecimal; fractional part is truncated as per toLong conversion"), Arguments.of("321", BigInteger.valueOf(321L), "Convert a numeric String to BigInteger"), Arguments.of(Character.valueOf('8'), BigInteger.valueOf(8L), "Convert a Character representing a digit to BigInteger"), Arguments.of(Boolean.TRUE, BigInteger.valueOf(1L), "Convert Boolean true to numeric 1"), Arguments.of(new AtomicLong(555L), BigInteger.valueOf(555L), "Convert an AtomicLong to BigInteger"), Arguments.of(new AtomicInteger(444), BigInteger.valueOf(444L), "Convert an AtomicInteger to BigInteger"), Arguments.of(new AtomicBoolean(true), BigInteger.valueOf(1L), "Convert an AtomicBoolean true to numeric 1"));
    }

    /**
     * Provides a stream of arguments that are expected to cause exceptions.
     *
     * Each {@link Arguments} consists of:
     * <ul>
     *   <li>the source object (the {@code from} parameter)</li>
     *   <li>the {@link Converter} to use (may be {@code null})</li>
     *   <li>the expected exception class</li>
     *   <li>a description for readability</li>
     * </ul>
     */
    private static Stream<Arguments> exceptionScenarioProvider() {
        return Stream.of(Arguments.of(null, converter, NullPointerException.class, "Passing a null 'from' argument should cause a NullPointerException when toLong attempts to process it."), Arguments.of("notANumber", converter, NumberFormatException.class, "A non‑numeric String cannot be parsed to a long, leading to NumberFormatException."), Arguments.of(new Object(), converter, IllegalArgumentException.class, "An unsupported object type should trigger IllegalArgumentException from toLong."), Arguments.of(Double.valueOf(Double.MAX_VALUE), converter, ArithmeticException.class, "A double value exceeding the range of long causes overflow, resulting in ArithmeticException when converting to BigInteger."), Arguments.of(BigDecimal.valueOf(Long.MAX_VALUE).add(BigDecimal.ONE), converter, ArithmeticException.class, "A BigDecimal larger than Long.MAX_VALUE overflows the long conversion, leading to ArithmeticException."), Arguments.of(Character.valueOf('A'), converter, NumberFormatException.class, "A non‑digit character cannot be interpreted as a numeric value, causing NumberFormatException."), Arguments.of(Boolean.FALSE, null, NullPointerException.class, "Passing a null converter should result in NullPointerException if the method dereferences the converter."));
    }

    @Test
    void convertsLong() {
        Long source = 123L;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(123L), result);
    }

    @Test
    void convertsInteger() {
        Integer source = 456;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(456L), result);
    }

    @Test
    void convertsShort() {
        Short source = 78;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(78L), result);
    }

    @Test
    void convertsByte() {
        Byte source = 9;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(9L), result);
    }

    @Test
    void convertsAtomicInteger() {
        AtomicInteger source = new AtomicInteger(321);
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(321L), result);
    }

    @Test
    void convertsAtomicLong() {
        AtomicLong source = new AtomicLong(654L);
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(654L), result);
    }

    @Test
    void convertsStringNumber() {
        String source = "987";
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(987L), result);
    }

    @Test
    void convertsNegativeNumber() {
        Integer source = -42;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(-42L), result);
    }

    @Test
    void nullInputReturnsZero() {
        // According to Converter's null‑handling contract, numeric primitives become 0.
        BigInteger result = NumberConversions.integerTypeToBigInteger(null, converter);
        assertEquals(BigInteger.ZERO, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("validConversionProvider")
    void testValidNumericConversions(Object from, BigInteger expected, String description) {
        // Act
        BigInteger result = NumberConversions.integerTypeToBigInteger(from, converter);
        // Assert
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("exceptionScenarioProvider")
    void testExceptionScenarios(Object from, Converter conv, Class<? extends Throwable> expectedException, String description) {
        // Act & Assert
        assertThrows(expectedException, () -> NumberConversions.integerTypeToBigInteger(from, conv), description);
    }
}
