package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NumberConversions_integerTypeToBigInteger_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a public no‑arg constructor; it provides the default behaviour.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC when many test classes run.
        converter = null;
    }

    /**
     * Supplies all test cases.  The data mirrors the JSON structure, grouping them only for
     * readability – JUnit treats the stream as a flat list.
     */
    private static Stream<Arguments> provideTestCases() {
        return Stream.of(/* ---------- Valid integer‑type conversions ---------- */
        Arguments.of(Integer.valueOf(42), null, BigInteger.valueOf(42), "Convert a plain Integer to BigInteger; should return the same numeric value."), Arguments.of(Long.valueOf(1234567890123L), null, BigInteger.valueOf(1234567890123L), "Convert a Long instance; the long value is wrapped in a BigInteger."), Arguments.of(Short.valueOf((short) 7), null, BigInteger.valueOf(7), "Convert a Short; the short is widened to long then to BigInteger."), Arguments.of(Byte.valueOf((byte) -5), null, BigInteger.valueOf(-5), "Convert a Byte (negative value); sign is preserved."), Arguments.of(BigInteger.valueOf(999), null, BigInteger.valueOf(999), "Pass a BigInteger directly; toLong should extract its long value and wrap it again."), Arguments.of("12345", null, BigInteger.valueOf(12345), "String containing a decimal integer; should be parsed and converted."), Arguments.of(Double.valueOf(12.34), null, BigInteger.valueOf(12), "Floating‑point number; toLong truncates toward zero before wrapping."), /* ---------- Null and invalid inputs ---------- */
        Arguments.of(null, NullPointerException.class, null, "Passing a null source object should result in a NullPointerException because the method cannot determine a numeric value."), Arguments.of("abc", NumberFormatException.class, null, "A non‑numeric string cannot be parsed to a long, so toLong throws NumberFormatException which propagates."), Arguments.of(Boolean.TRUE, IllegalArgumentException.class, null, "Boolean values are not supported by integer‑type conversion; the method should reject them with an IllegalArgumentException."), Arguments.of(Character.valueOf('5'), IllegalArgumentException.class, null, "A Character is not an integer type; conversion should fail with IllegalArgumentException."), /* ---------- String edge cases ---------- */
        Arguments.of("  42  ", null, BigInteger.valueOf(42), "String with surrounding whitespace should be trimmed and parsed correctly."), Arguments.of("+17", null, BigInteger.valueOf(17), "Explicit plus sign is accepted and yields the positive value."), Arguments.of("-99", null, BigInteger.valueOf(-99), "Negative sign is parsed and the resulting BigInteger retains the sign."), /* ---------- Boundary numeric values ---------- */
        Arguments.of(Long.MAX_VALUE, null, BigInteger.valueOf(Long.MAX_VALUE), "Maximum long value should be converted without overflow."), Arguments.of(Long.MIN_VALUE, null, BigInteger.valueOf(Long.MIN_VALUE), "Minimum long value (negative) should be converted correctly."), Arguments.of(Integer.valueOf(0), null, BigInteger.ZERO, "Zero is a valid integer value and should map to BigInteger.ZERO."), /* ---------- Atomic numeric wrappers ---------- */
        Arguments.of(new AtomicInteger(123), null, BigInteger.valueOf(123), "AtomicInteger should be unwrapped to its int value before conversion."), Arguments.of(new AtomicLong(-456L), null, BigInteger.valueOf(-456), "AtomicLong should be unwrapped to its long value before conversion."), /* ---------- Floating‑point sources that are still integer‑type convertible ---------- */
        Arguments.of(BigDecimal.valueOf(123.99), null, BigInteger.valueOf(123), "BigDecimal is a floating‑point type; toLong truncates toward zero before wrapping."), Arguments.of(Float.valueOf(-7.89f), null, BigInteger.valueOf(-7), "Float value is truncated toward zero by toLong before conversion."), Arguments.of(Double.valueOf(Double.NaN), IllegalArgumentException.class, null, "NaN cannot be represented as a long; the conversion should fail."), Arguments.of(Double.valueOf(Double.POSITIVE_INFINITY), IllegalArgumentException.class, null, "Infinity cannot be converted to a finite long; expect an IllegalArgumentException."), /* ---------- BigInteger out of long range ---------- */
        Arguments.of(new BigInteger("9223372036854775808"), IllegalArgumentException.class, null, "BigInteger value larger than Long.MAX_VALUE cannot be represented as a long; toLong should reject it."), Arguments.of(new BigInteger("-9223372036854775809"), IllegalArgumentException.class, null, "BigInteger value smaller than Long.MIN_VALUE cannot be represented as a long; toLong should reject it."), /* ---------- Unsupported temporal types without converter ---------- */
        Arguments.of(LocalDate.of(2023, 1, 1), IllegalArgumentException.class, null, "LocalDate is not an integer‑type source; without a Converter the method should throw IllegalArgumentException."), Arguments.of(Instant.now(), IllegalArgumentException.class, null, "Instant cannot be directly converted to a long by integerTypeToBigInteger; expect IllegalArgumentException."), /* ---------- AtomicBoolean unsupported ---------- */
        Arguments.of(new AtomicBoolean(true), IllegalArgumentException.class, null, "AtomicBoolean does not represent a numeric value; conversion should fail with IllegalArgumentException."), /* ---------- Empty or whitespace‑only strings ---------- */
        Arguments.of("", NumberFormatException.class, null, "An empty string cannot be parsed to a number; toLong should throw NumberFormatException."), Arguments.of("   ", NumberFormatException.class, null, "A string containing only whitespace is not a valid numeric representation."), /* ---------- Invalid numeric string formats ---------- */
        Arguments.of("0xFF", NumberFormatException.class, null, "Hexadecimal notation is not accepted by the default integer parser; expect NumberFormatException."), Arguments.of("+", NumberFormatException.class, null, "A plus sign without digits is not a valid number."), Arguments.of("-", NumberFormatException.class, null, "A minus sign without digits is not a valid number."), /* ---------- Non‑numeric object types ---------- */
        Arguments.of(java.util.Collections.emptyList(), IllegalArgumentException.class, null, "A List does not represent a numeric value; conversion should be rejected."), Arguments.of(new Object(), IllegalArgumentException.class, null, "An arbitrary Object without numeric semantics should cause IllegalArgumentException."));
    }

    @Test
    void integerInput_isConvertedToBigInteger() {
        Integer source = 42;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(42L), result, "Integer value should be converted to an equivalent BigInteger");
    }

    @Test
    void longInput_isConvertedToBigInteger() {
        // Long.MAX_VALUE
        Long source = 9_223_372_036_854_775_807L;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE), result, "Long value should be converted to an equivalent BigInteger");
    }

    @Test
    void stringNumericInput_isConvertedToBigInteger() {
        String source = "123456789";
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(new BigInteger("123456789"), result, "Numeric String should be parsed and converted to a BigInteger");
    }

    @Test
    void nullInput_returnsZeroBigInteger() {
        // According to Converter's contract, a null source yields 0 for primitive numeric types.
        BigInteger result = NumberConversions.integerTypeToBigInteger(null, converter);
        assertEquals(BigInteger.ZERO, result, "Null source should be treated as 0 and converted to BigInteger.ZERO");
    }

    @Test
    void atomicIntegerInput_isConvertedToBigInteger() {
        AtomicInteger source = new AtomicInteger(-7);
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(-7L), result, "AtomicInteger value should be converted to an equivalent BigInteger");
    }

    @Test
    void shortInput_isConvertedToBigInteger() {
        Short source = 12;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(12L), result, "Short value should be converted to an equivalent BigInteger");
    }

    @Test
    void byteInput_isConvertedToBigInteger() {
        Byte source = (byte) 3;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(3L), result, "Byte value should be converted to an equivalent BigInteger");
    }

    /**
     * Parameterised test that covers every case described in the JSON document.
     *
     * @param from               the value to be converted
     * @param expectedException  the exception we expect (or {@code null} for a successful conversion)
     * @param expectedResult     the {@link BigInteger} we expect when no exception is thrown
     * @param description        a short description of the scenario (used as test name)
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideTestCases")
    void integer_type_to_big_integer(Object from, Class<? extends Throwable> expectedException, BigInteger expectedResult, String description) {
        if (expectedException != null) {
            // The method is expected to throw.
            assertThrows(expectedException, () -> NumberConversions.integerTypeToBigInteger(from, converter), description);
        } else {
            // Normal conversion – result must match.
            BigInteger actual = NumberConversions.integerTypeToBigInteger(from, converter);
            assertEquals(expectedResult, actual, description);
        }
    }
}
