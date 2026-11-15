package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

class NumberConversions_toCharacter_Test {

    private static ConverterOptions options;

    private Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // ConverterOptions has a no‑arg constructor that creates a default options set.
        options = new ConverterOptions();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for each test to avoid cross‑test state.
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no stray state leaks between tests.
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nothing special to clean up for the static options.
        options = null;
    }

    /**
     * Provides a stream of valid inputs together with the expected character.
     */
    private static Stream<Arguments> provideValidConversions() {
        return Stream.of(// primitive / wrapper numbers
        // zero
        Arguments.of(0, '\u0000'), // int
        Arguments.of(65, 'A'), // max char value
        Arguments.of(65535, '\uffff'), // long
        Arguments.of(66L, 'B'), // short
        Arguments.of((short) 67, 'C'), // double (truncates)
        Arguments.of(68.0, 'D'), // float (truncates)
        Arguments.of(69.9f, 'E'), // atomic types
        Arguments.of(new AtomicInteger(70), 'F'), Arguments.of(new AtomicLong(71L), 'G'), // big number types
        Arguments.of(new BigInteger("72"), 'H'), Arguments.of(new BigDecimal("73"), 'I'), // strings that represent numbers
        // 123 → '{'
        Arguments.of("123", '{'), // string max value
        Arguments.of("65535", '\uffff'), // single‑character string – the library treats it as the character itself
        Arguments.of("K", 'K'), // Character object
        Arguments.of(Character.valueOf('L'), 'L'));
    }

    /**
     * Provides inputs that are expected to cause an exception.
     * The third argument is an optional part of the exception message to verify.
     */
    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// unsupported object type
        Arguments.of(new Object(), IllegalArgumentException.class, null), // boolean – not a numeric conversion
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class, null), // non‑numeric string
        Arguments.of("abc", IllegalArgumentException.class, null), // numeric string with decimal part (cannot be parsed to long)
        Arguments.of("12.34", IllegalArgumentException.class, null), // NaN and Infinity – illegal for long conversion
        Arguments.of(Double.NaN, IllegalArgumentException.class, null), Arguments.of(Double.POSITIVE_INFINITY, IllegalArgumentException.class, null), // Date whose epoch millis exceed the char range
        Arguments.of(new Date(100_000L), IllegalArgumentException.class, "out of range"), // Values clearly out of range
        Arguments.of(70_000, IllegalArgumentException.class, "out of range"), Arguments.of(-5_000, IllegalArgumentException.class, "out of range"), // AtomicBoolean – not a supported numeric type
        Arguments.of(new AtomicBoolean(true), IllegalArgumentException.class, null));
    }

    @Test
    void toCharacter_fromIntegerWithinRange_returnsCorrectChar() {
        // given
        // ASCII for 'A'
        Integer source = 65;
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals('A', result);
    }

    @Test
    void toCharacter_fromStringWithinRange_returnsCorrectChar() {
        // given
        // ASCII for 'B'
        String source = "66";
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals('B', result);
    }

    @Test
    void toCharacter_fromLongZero_returnsNullChar() {
        // given
        // should map to '\u0000'
        Long source = 0L;
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals('\u0000', result);
    }

    @Test
    void toCharacter_fromNull_returnsNullChar() {
        // given
        // Converter returns 0 for null numeric conversion
        Object source = null;
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals('\u0000', result);
    }

    @Test
    void toCharacter_valueOutOfRange_throwsIllegalArgumentException() {
        // given
        // greater than Character.MAX_VALUE (65535)
        Integer source = 70_000;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCharacter(source, converter));
        assertTrue(ex.getMessage().contains("out of range"));
    }

    @ParameterizedTest(name = "toCharacter({0}) → ''{1}''")
    @MethodSource("provideValidConversions")
    void toCharacter_validConversions(Object from, char expected) {
        // when
        char result = NumberConversions.toCharacter(from, converter);
        // then
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "toCharacter({0}) throws {1}")
    @MethodSource("provideExceptionCases")
    void toCharacter_exceptions(Object from, Class<? extends Throwable> expectedException, String expectedMessagePart) {
        Throwable thrown = assertThrows(expectedException, () -> NumberConversions.toCharacter(from, converter));
        if (expectedMessagePart != null) {
            assertTrue(thrown.getMessage().contains(expectedMessagePart), () -> "Expected message to contain [" + expectedMessagePart + "] but was [" + thrown.getMessage() + "]");
        }
    }
}
