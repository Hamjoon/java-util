package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

class NumberConversions_isIntTypeNotZero_Test {

    /**
     * Converter used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the GC in long test suites.
        converter = null;
    }

    /**
     * Supplies the numeric test data used by {@link #isIntTypeNotZero_numericInputs(Object, boolean)}.
     */
    private static Stream<Arguments> numericSourceProvider() {
        return Stream.of(// Primitive wrapper types
        Arguments.of(0L, false), Arguments.of(123L, true), Arguments.of(Integer.valueOf(0), false), Arguments.of(Integer.valueOf(42), true), Arguments.of(Short.valueOf((short) 0), false), Arguments.of(Short.valueOf((short) 5), true), Arguments.of(Byte.valueOf((byte) 0), false), Arguments.of(Byte.valueOf((byte) 9), true), // Atomic numeric types
        Arguments.of(new AtomicInteger(0), false), Arguments.of(new AtomicInteger(7), true), Arguments.of(new AtomicLong(0L), false), Arguments.of(new AtomicLong(15L), true), // BigInteger / BigDecimal
        Arguments.of(new BigInteger("0"), false), Arguments.of(new BigInteger("12345678901234567890"), true), Arguments.of(new BigDecimal("0.0"), false), Arguments.of(new BigDecimal("3.14"), true), // Numeric strings
        Arguments.of("0", false), Arguments.of("-42", true));
    }

    /**
     * Supplies the exception‑expectation test data.
     */
    private static Stream<Arguments> exceptionSourceProvider() {
        // Mock converter that throws RuntimeException for any conversion attempt
        Converter throwingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("forced exception");
        });
        return Stream.of(// Invalid numeric strings
        Arguments.of("not a number", converter, NumberFormatException.class), // > Long.MAX_VALUE
        Arguments.of("9223372036854775808", converter, NumberFormatException.class), // < Long.MIN_VALUE
        Arguments.of("-9223372036854775809", converter, NumberFormatException.class), // Floating‑point special strings – toLong cannot parse them
        Arguments.of("NaN", converter, IllegalArgumentException.class), Arguments.of("Infinity", converter, IllegalArgumentException.class), // Unsupported object types
        Arguments.of(Boolean.TRUE, converter, IllegalArgumentException.class), Arguments.of('a', converter, IllegalArgumentException.class), Arguments.of(new java.util.Date(), converter, IllegalArgumentException.class), // Null handling – the real Converter treats null as 0, but passing a null
        // converter leads to a NullPointerException inside the static method.
        Arguments.of(null, null, NullPointerException.class), // Converter that throws a RuntimeException
        Arguments.of("123", throwingConverter, RuntimeException.class));
    }

    @Test
    void isIntTypeNotZero_withIntegerNonZero_returnsTrue() {
        Integer source = 42;
        assertTrue(NumberConversions.isIntTypeNotZero(source, converter), "Integer 42 should be considered non‑zero");
    }

    @Test
    void isIntTypeNotZero_withLongNonZero_returnsTrue() {
        Long source = 123L;
        assertTrue(NumberConversions.isIntTypeNotZero(source, converter));
    }

    @Test
    void isIntTypeNotZero_withStringNumberNonZero_returnsTrue() {
        String source = "999";
        assertTrue(NumberConversions.isIntTypeNotZero(source, converter));
    }

    @Test
    void isIntTypeNotZero_withDoubleNonZero_returnsTrue() {
        Double source = 3.14;
        assertTrue(NumberConversions.isIntTypeNotZero(source, converter));
    }

    @Test
    void isIntTypeNotZero_withAtomicLongNonZero_returnsTrue() {
        AtomicLong source = new AtomicLong(7L);
        assertTrue(NumberConversions.isIntTypeNotZero(source, converter));
    }

    @Test
    void isIntTypeNotZero_withIntegerZero_returnsFalse() {
        Integer source = 0;
        assertFalse(NumberConversions.isIntTypeNotZero(source, converter));
    }

    @Test
    void isIntTypeNotZero_withLongZero_returnsFalse() {
        Long source = 0L;
        assertFalse(NumberConversions.isIntTypeNotZero(source, converter));
    }

    @Test
    void isIntTypeNotZero_withStringZero_returnsFalse() {
        String source = "0";
        assertFalse(NumberConversions.isIntTypeNotZero(source, converter));
    }

    @Test
    void isIntTypeNotZero_withDoubleZero_returnsFalse() {
        Double source = 0.0;
        assertFalse(NumberConversions.isIntTypeNotZero(source, converter));
    }

    @Test
    void isIntTypeNotZero_withAtomicLongZero_returnsFalse() {
        AtomicLong source = new AtomicLong(0L);
        assertFalse(NumberConversions.isIntTypeNotZero(source, converter));
    }

    @Test
    void isIntTypeNotZero_withNull_returnsFalse() {
        // According to Converter's contract, null converts to 0 for numeric primitives.
        assertFalse(NumberConversions.isIntTypeNotZero(null, converter));
    }

    @Test
    void isIntTypeNotZero_withBooleanTrue_returnsTrue() {
        // This test reflects the historic behaviour where Boolean.TRUE is treated as 1.
        // It is retained for documentation purposes; the parameterized suite also covers it.
        assertTrue(NumberConversions.isIntTypeNotZero(Boolean.TRUE, converter), "Boolean.TRUE should be considered non‑zero");
    }

    @Test
    void isIntTypeNotZero_withBooleanFalse_returnsFalse() {
        assertFalse(NumberConversions.isIntTypeNotZero(Boolean.FALSE, converter), "Boolean.FALSE should be considered zero");
    }

    @Test
    void isIntTypeNotZero_withUnsupportedObject_returnsFalse() {
        // An object that has no numeric conversion should result in 0.
        Object source = new Object();
        assertFalse(NumberConversions.isIntTypeNotZero(source, converter));
    }

    /**
     * Parameterized test that verifies the boolean result for a wide range of numeric
     * source objects. The expected value is derived from the numeric value after it
     * is converted to a {@code long} by {@code NumberConversions.toLong}.
     */
    @ParameterizedTest(name = "[{index}] from={0} → expected={1}")
    @MethodSource("numericSourceProvider")
    void isIntTypeNotZero_numericInputs(Object from, boolean expected) {
        assertEquals(expected, NumberConversions.isIntTypeNotZero(from, converter), () -> "Unexpected result for source: " + from);
    }

    /**
     * Parameterized test that expects a specific exception to be thrown
     * when {@code isIntTypeNotZero} is invoked with unsupported or malformed inputs.
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1} → throws {2}")
    @MethodSource("exceptionSourceProvider")
    void isIntTypeNotZero_throwsException(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.isIntTypeNotZero(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for source: " + from);
    }
}
