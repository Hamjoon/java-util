package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_integerTypeToBigInteger_Test {

    /**
     * Shared {@link Converter} used for all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; using the default constructor.
        converter = new Converter(new ConverterOptions());
        // The default constructor creates a Converter with default options – sufficient for our tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
        converter = null;
    }

    private static Stream<Arguments> positiveCases() {
        return Stream.of(// primitive wrappers
        Arguments.of("Integer → BigInteger", Integer.valueOf(456), BigInteger.valueOf(456L)), Arguments.of("Long → BigInteger", Long.valueOf(123L), BigInteger.valueOf(123L)), Arguments.of("Short → BigInteger", Short.valueOf((short) 7), BigInteger.valueOf(7L)), Arguments.of("Byte → BigInteger", Byte.valueOf((byte) 9), BigInteger.valueOf(9L)), // atomic types
        Arguments.of("AtomicInteger → BigInteger", new AtomicInteger(321), BigInteger.valueOf(321L)), Arguments.of("AtomicLong → BigInteger", new AtomicLong(654L), BigInteger.valueOf(654L)), Arguments.of("Negative AtomicInteger → BigInteger", new AtomicInteger(-100), BigInteger.valueOf(-100L)), // numeric strings
        Arguments.of("Numeric String → BigInteger", "12345", BigInteger.valueOf(12345L)), Arguments.of("String with whitespace and sign → BigInteger", " -42 ", BigInteger.valueOf(-42L)), Arguments.of("Zero string → BigInteger.ZERO", "0", BigInteger.ZERO), // floating‑point numbers (truncation)
        Arguments.of("BigDecimal with fraction → truncated", new BigDecimal("123.456"), BigInteger.valueOf(123L)), Arguments.of("Double with fraction → truncated", Double.valueOf(789.999), BigInteger.valueOf(789L)), Arguments.of("Float negative → truncated", Float.valueOf(-12.34f), BigInteger.valueOf(-12L)), // date / time types (milliseconds since epoch)
        Arguments.of("java.util.Date → milliseconds", new Date(1_609_459_200_000L), BigInteger.valueOf(1_609_459_200_000L)), Arguments.of("java.util.Calendar → milliseconds", calendarWithMillis(1_609_459_200_000L), BigInteger.valueOf(1_609_459_200_000L)), Arguments.of("java.time.Instant → milliseconds", Instant.ofEpochMilli(123_456_789L), BigInteger.valueOf(123_456_789L)), // already a BigInteger (converted via longValue)
        Arguments.of("BigInteger within Long range → same value", new BigInteger("9876543210123456789"), BigInteger.valueOf(9_876_543_210_123_456_789L)));
    }

    private static Calendar calendarWithMillis(long millis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal;
    }

    private static Stream<Arguments> exceptionCases() {
        return Stream.of(// malformed strings
        Arguments.of("Empty string → NumberFormatException", "", NumberFormatException.class), Arguments.of("Non‑numeric string → NumberFormatException", "123abc", NumberFormatException.class), // unsupported type
        Arguments.of("Unsupported Object → ClassCastException", new Object(), ClassCastException.class), // overflow / out‑of‑range
        Arguments.of("BigDecimal exceeding Long.MAX_VALUE → ArithmeticException", new BigDecimal("1E400"), ArithmeticException.class), Arguments.of("BigInteger > Long.MAX_VALUE → ArithmeticException", new BigInteger("9223372036854775808"), ArithmeticException.class), Arguments.of("AtomicLong > Long.MAX_VALUE → ArithmeticException", new AtomicLong(Long.MAX_VALUE).incrementAndGet(), ArithmeticException.class), // null source – toLong throws NullPointerException according to existing behaviour
        Arguments.of("Null source → NullPointerException", null, NullPointerException.class));
    }

    @Test
    void integerTypeToBigInteger_fromInteger() {
        Integer source = 42;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(42L), result);
    }

    @Test
    void integerTypeToBigInteger_fromLong() {
        Long source = 1_234_567_890_123L;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(1_234_567_890_123L), result);
    }

    @Test
    void integerTypeToBigInteger_fromShort() {
        Short source = 7;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(7L), result);
    }

    @Test
    void integerTypeToBigInteger_fromByte() {
        Byte source = 9;
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(9L), result);
    }

    @Test
    void integerTypeToBigInteger_fromAtomicInteger() {
        AtomicInteger source = new AtomicInteger(15);
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(15L), result);
    }

    @Test
    void integerTypeToBigInteger_fromAtomicLong() {
        AtomicLong source = new AtomicLong(27L);
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(27L), result);
    }

    @Test
    void integerTypeToBigInteger_fromStringNumeric() {
        String source = "33";
        BigInteger result = NumberConversions.integerTypeToBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(33L), result);
    }

    @Test
    void integerTypeToBigInteger_fromNull_returnsZero() {
        // According to the Converter contract, a null source for a primitive conversion yields zero.
        BigInteger result = NumberConversions.integerTypeToBigInteger(null, converter);
        assertEquals(BigInteger.ZERO, result);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("positiveCases")
    void integerTypeToBigInteger_positive(String description, Object from, BigInteger expected) {
        BigInteger result = NumberConversions.integerTypeToBigInteger(from, converter);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionCases")
    void integerTypeToBigInteger_exceptions(String description, Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> NumberConversions.integerTypeToBigInteger(from, converter), description);
    }
}
