package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collections;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toCharacter_Test {

    /**
     * Shared {@link Converter} used by all test cases.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The library provides a no‑arg constructor for ConverterOptions that yields the default configuration.
        // No custom overrides are required for the simple numeric conversions exercised in these tests.
        ConverterOptions options = new ConverterOptions();
        // explicit, but the default is also empty
        options.setConverterOverrides(Collections.emptyMap());
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
    }

    /**
     * Supplies all successful conversion scenarios.
     */
    Stream<Arguments> successProvider() {
        return Stream.of(// plain numeric types using the shared default converter
        // zero → null character
        Arguments.of(0, converter, '\u0000'), // ASCII 'A'
        Arguments.of(65, converter, 'A'), // max char
        Arguments.of(65535, converter, '\uffff'), // 12345 → '〹'
        Arguments.of(new BigInteger("12345"), converter, '\u3039'), // primitive wrappers
        Arguments.of(Byte.valueOf((byte) 0), converter, '\u0000'), // 32767 → '翿'
        Arguments.of(Short.valueOf((short) 32767), converter, '\u7FFF'), Arguments.of(Integer.valueOf(65535), converter, '\uffff'), Arguments.of(Long.valueOf(65535L), converter, '\uffff'), // BigDecimal / BigInteger variants
        Arguments.of(new BigDecimal("65535"), converter, '\uffff'), Arguments.of(new BigInteger("0"), converter, '\u0000'), Arguments.of(new BigInteger("65535"), converter, '\uffff'), Arguments.of(new BigInteger("-0"), converter, '\u0000'), // atomic types
        Arguments.of(new AtomicInteger(65), converter, 'A'), // custom converters that translate a Date into a long value
        Arguments.of(new Date(0L), customConverterReturningLong(0L), '\u0000'), Arguments.of(new Date(12345L), customConverterReturningLong(12345L), '\u3039'));
    }

    /**
     * Supplies all failure conversion scenarios.
     */
    Stream<Arguments> failureProvider() {
        return Stream.of(// numeric values outside the char range
        Arguments.of(-1, converter, IllegalArgumentException.class), Arguments.of(65536, converter, IllegalArgumentException.class), Arguments.of(new BigInteger("999999999999999999999"), converter, IllegalArgumentException.class), // non‑numeric string
        Arguments.of("notANumber", converter, IllegalArgumentException.class), // custom converter returning an out‑of‑range long
        Arguments.of(new Date(70000L), customConverterReturningLong(70000L), IllegalArgumentException.class), // atomic types with illegal values
        Arguments.of(new AtomicLong(70000L), converter, IllegalArgumentException.class), Arguments.of(new AtomicBoolean(true), converter, IllegalArgumentException.class), // null input
        Arguments.of(null, converter, IllegalArgumentException.class), // numeric string out of range
        Arguments.of("70000", converter, IllegalArgumentException.class), // floating‑point special values
        Arguments.of(Double.NaN, converter, IllegalArgumentException.class), Arguments.of(Double.POSITIVE_INFINITY, converter, IllegalArgumentException.class), // BigDecimal edge cases
        Arguments.of(new BigDecimal("65536"), converter, IllegalArgumentException.class), Arguments.of(new BigDecimal("123.45"), converter, IllegalArgumentException.class), // completely unsupported type
        Arguments.of(new Object(), converter, IllegalArgumentException.class), // a converter that throws a RuntimeException during conversion
        Arguments.of("any", throwingConverter(), RuntimeException.class));
    }

    /**
     * Creates a Mockito mock of {@link Converter} that, when asked to convert
     * anything to {@link Long}, returns the supplied {@code value}.
     *
     * @param value the long value the mock should return
     * @return a mocked {@link Converter}
     */
    private static Converter customConverterReturningLong(long value) {
        Converter mock = mock(Converter.class);
        // The real Converter likely has a method like <T> T convert(Object, Class<T>);
        // We stub any invocation that asks for a Long (or long) to return the desired value.
        when(mock.convert(any(), eq(Long.class))).thenReturn(value);
        when(mock.convert(any(), eq(Long.TYPE))).thenReturn(value);
        // Fallback for any other generic conversion request – return the long as is.
        when(mock.convert(any(), any())).thenReturn(value);
        return mock;
    }

    /**
     * Creates a Mockito mock of {@link Converter} that throws a {@link RuntimeException}
     * for any conversion attempt. This matches the JSON test case “Converter throws exception”.
     *
     * @return a mocked {@link Converter} that always throws
     */
    private static Converter throwingConverter() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), any())).thenThrow(new RuntimeException("Converter failure"));
        return mock;
    }

    /**
     * Utility method to produce a readable description of the test input.
     *
     * @param obj the object under test
     * @return a short string representation
     */
    private static String describeObject(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String) {
            return "\"" + obj + "\"";
        }
        return obj.getClass().getSimpleName() + "(" + obj.toString() + ")";
    }

    @Test
    void toCharacter_fromIntegerWithinRange_returnsExpectedChar() {
        // given
        // ASCII code for 'A'
        Integer source = 65;
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals('A', result, "Integer 65 should be converted to character 'A'");
    }

    @Test
    void toCharacter_fromStringWithinRange_returnsExpectedChar() {
        // given
        // ASCII code for 'B'
        String source = "66";
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals('B', result, "String \"66\" should be converted to character 'B'");
    }

    @Test
    void toCharacter_fromLongWithinRange_returnsExpectedChar() {
        // given
        // ASCII code for 'C'
        Long source = 67L;
        // when
        char result = NumberConversions.toCharacter(source, converter);
        // then
        assertEquals('C', result, "Long 67 should be converted to character 'C'");
    }

    @Test
    void toCharacter_fromLongOutOfRange_throwsIllegalArgumentException() {
        // given
        // greater than Character.MAX_VALUE (65535)
        Long source = 70_000L;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCharacter(source, converter));
        assertTrue(ex.getMessage().contains("out of range"), "Exception message should indicate the value is out of range");
    }

    @Test
    void toCharacter_fromNegativeNumber_throwsIllegalArgumentException() {
        // given
        Integer source = -5;
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> NumberConversions.toCharacter(source, converter));
        assertTrue(ex.getMessage().contains("out of range"), "Exception message should indicate the value is out of range");
    }

    /**
     * Parameterized test that verifies successful conversion of values that are
     * within the {@code char} range. The test data originates from the JSON
     * group “Valid character conversions (value within range)” and the
     * “Boundary and type‑mix conversions” group where the expected result is a
     * character.
     */
    @ParameterizedTest(name = "[{index}] from={0} → expected=''{2}''")
    @MethodSource("successProvider")
    void toCharacter_successfulConversion(Object from, Converter conv, char expected) {
        // when
        char result = NumberConversions.toCharacter(from, conv);
        // then
        assertEquals(expected, result, () -> "Conversion of " + describeObject(from) + " should yield '" + expected + "'");
    }

    /**
     * Parameterized test that verifies that illegal arguments result in the
     * expected exception type. The test data comes from the JSON groups
     * “Out‑of‑range conversions (exception path)”, “Boundary and type‑mix
     * conversions” (negative / out‑of‑range cases), “String numeric out‑of‑range”,
     * “Floating‑point special values”, “BigDecimal with fractional part”,
     * “Unsupported object type”, and “Converter throws exception”.
     */
    @ParameterizedTest(name = "[{index}] from={0} → throws {2}")
    @MethodSource("failureProvider")
    void toCharacter_failureConversion(Object from, Converter conv, Class<? extends Throwable> expectedException) {
        // when / then
        Throwable thrown = assertThrows(expectedException, () -> NumberConversions.toCharacter(from, conv), () -> "Expected " + expectedException.getSimpleName() + " for input " + describeObject(from));
        // For IllegalArgumentException we also assert that the message mentions "out of range"
        if (expectedException == IllegalArgumentException.class && thrown != null) {
            assertTrue(thrown.getMessage().contains("out of range") || thrown.getMessage().contains("cannot be converted") || thrown.getMessage().contains("NaN") || thrown.getMessage().contains("Infinity"), "Exception message should hint at the conversion problem");
        }
    }
}
