package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToInteger_Test {

    static Stream<Arguments> basicTypeConversionsProvider() {
        return Stream.of(Arguments.of("123", 123), Arguments.of("  456  ", 456), Arguments.of("12.34", NumberFormatException.class), Arguments.of(789, 789), Arguments.of(12345.0, 12345), Arguments.of(12345.67, 12345), Arguments.of(Boolean.TRUE, 1), Arguments.of(Boolean.FALSE, 0), Arguments.of(null, null));
    }

    static Stream<Arguments> numericEdgeCasesProvider() {
        return Stream.of(Arguments.of("+42", 42), Arguments.of("-42", -42), Arguments.of("00123", 123), Arguments.of("", NumberFormatException.class), Arguments.of("   ", NumberFormatException.class), Arguments.of("0x10", NumberFormatException.class));
    }

    static Stream<Arguments> otherNumberTypesProvider() {
        return Stream.of(// Byte
        Arguments.of(Byte.valueOf((byte) 7), 7), // Short
        Arguments.of(Short.valueOf((short) 8), 8), // Long within range
        Arguments.of(Long.valueOf(123L), 123), // Long overflow
        Arguments.of(Long.valueOf(2147483648L), ArithmeticException.class), // BigInteger within range
        Arguments.of(new BigInteger("12345"), 12345), // BigInteger overflow
        Arguments.of(new BigInteger("2147483648"), ArithmeticException.class), // BigDecimal with fractional part (truncates)
        Arguments.of(new BigDecimal("123.99"), 123), // BigDecimal outside Integer range
        Arguments.of(new BigDecimal("1E10"), ArithmeticException.class), // Float NaN
        Arguments.of(Float.NaN, NumberFormatException.class), // Float positive infinity
        Arguments.of(Float.POSITIVE_INFINITY, NumberFormatException.class));
    }

    static Stream<Arguments> characterAndBufferTypesProvider() {
        return Stream.of(// digit character
        Arguments.of('5', 5), // non‑numeric character
        Arguments.of('A', NumberFormatException.class), Arguments.of(CharBuffer.wrap("99"), 99), Arguments.of(CharBuffer.wrap(""), NumberFormatException.class));
    }

    static Stream<Arguments> atomicAndBooleanTypesProvider() {
        return Stream.of(Arguments.of(new AtomicInteger(10), 10), Arguments.of(new AtomicLong(20L), 20), Arguments.of(new AtomicLong(3000000000L), ArithmeticException.class), Arguments.of(new AtomicBoolean(true), 1), Arguments.of(new AtomicBoolean(false), 0));
    }

    static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of(new Date(123456789L)), Arguments.of(LocalDate.now()), Arguments.of(LocalDateTime.now()), Arguments.of(ZonedDateTime.now()), Arguments.of(Timestamp.from(Instant.now())), Arguments.of(UUID.randomUUID()), Arguments.of(new Object()));
    }

    static Stream<Arguments> nullAndWrapperTypesProvider() {
        return Stream.of(Arguments.of(null, null), Arguments.of(new Boolean(true), 1), Arguments.of(new Boolean(false), 0), Arguments.of(Character.valueOf('7'), 7), Arguments.of(Character.valueOf('b'), NumberFormatException.class));
    }

    /* --------------------------------------------------------------------- *
     *  Helper method that decides whether to assert a value, null or an   *
     *  exception based on the type of the expected argument.               *
     * --------------------------------------------------------------------- */
    private void executeConversionAndAssert(Object input, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> Converter.convertToInteger(input), () -> "Expected conversion to throw " + exClass.getSimpleName());
        } else if (expected == null) {
            assertNull(Converter.convertToInteger(input), "Expected conversion to return null");
        } else {
            Integer actual = Converter.convertToInteger(input);
            assertEquals(expected, actual, () -> "Conversion result mismatch for input: " + input);
        }
    }

    @Test
    @DisplayName("convert null → null")
    void convertNullReturnsNull() {
        Integer result = Converter.convertToInteger(null);
        assertNull(result, "Converting null should return null for wrapper types");
    }

    @Test
    @DisplayName("convert numeric String → Integer")
    void convertStringNumber() {
        Integer result = Converter.convertToInteger("12345");
        assertEquals(12345, result);
    }

    @Test
    @DisplayName("convert floating‑point Number → truncated Integer")
    void convertFloatingNumber() {
        // double literal
        Integer result = Converter.convertToInteger(45.9);
        assertEquals(45, result, "Conversion should truncate toward zero");
    }

    @Test
    @DisplayName("convert Boolean true → 1 and false → 0")
    void convertBoolean() {
        assertEquals(1, Converter.convertToInteger(Boolean.TRUE));
        assertEquals(0, Converter.convertToInteger(Boolean.FALSE));
    }

    @Test
    @DisplayName("convert Character → its Unicode code point")
    void convertCharacter() {
        Integer result = Converter.convertToInteger('A');
        assertEquals((int) 'A', result);
    }

    @Test
    @DisplayName("convert BigDecimal → truncated Integer")
    void convertBigDecimal() {
        BigDecimal bd = new BigDecimal("9876.543");
        Integer result = Converter.convertToInteger(bd);
        assertEquals(9876, result);
    }

    @Test
    @DisplayName("convert Map with key \"value\" → Integer")
    void convertMapWithValueKey() {
        Map<String, Object> map = Map.of("value", "42");
        Integer result = Converter.convertToInteger(map);
        assertEquals(42, result);
    }

    @Test
    @DisplayName("convert unsupported type throws IllegalArgumentException")
    void convertUnsupportedType() {
        Object unsupported = new Object();
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToInteger(unsupported), "Converting an unsupported type should raise an exception");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] basic conversion: {0} → {1}")
    @MethodSource("basicTypeConversionsProvider")
    void convertBasicTypeConversions(Object fromInstance, Object expected) {
        executeConversionAndAssert(fromInstance, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] edge case: {0} → {1}")
    @MethodSource("numericEdgeCasesProvider")
    void convertNumericEdgeCases(Object fromInstance, Object expected) {
        executeConversionAndAssert(fromInstance, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] other number: {0} → {1}")
    @MethodSource("otherNumberTypesProvider")
    void convertOtherNumberTypes(Object fromInstance, Object expected) {
        executeConversionAndAssert(fromInstance, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] char/buffer: {0} → {1}")
    @MethodSource("characterAndBufferTypesProvider")
    void convertCharacterAndBufferTypes(Object fromInstance, Object expected) {
        executeConversionAndAssert(fromInstance, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] atomic/boolean: {0} → {1}")
    @MethodSource("atomicAndBooleanTypesProvider")
    void convertAtomicAndBooleanTypes(Object fromInstance, Object expected) {
        executeConversionAndAssert(fromInstance, expected);
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] unsupported type → null: {0}")
    @MethodSource("unsupportedTypesProvider")
    void convertUnsupportedTypes(Object fromInstance) {
        Integer result = Converter.convertToInteger(fromInstance);
        assertNull(result, "Conversion of unsupported type should return null");
    }

    // --------------------------------------------------------------------- //
    @ParameterizedTest(name = "[{index}] wrapper/char edge: {0} → {1}")
    @MethodSource("nullAndWrapperTypesProvider")
    void convertNullAndWrapperTypes(Object fromInstance, Object expected) {
        executeConversionAndAssert(fromInstance, expected);
    }
}
