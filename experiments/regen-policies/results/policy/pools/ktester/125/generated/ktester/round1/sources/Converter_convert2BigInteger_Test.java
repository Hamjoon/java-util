package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2BigInteger_Test {

    private static Stream<Arguments> numericWrapperProvider() {
        return Stream.of(Arguments.of("Integer wrapper 123 -> BigInteger 123", 123, BigInteger.valueOf(123)), Arguments.of("Long wrapper 9876543210L -> BigInteger 9876543210", 9876543210L, BigInteger.valueOf(9876543210L)), Arguments.of("Double 12.34 -> truncated BigInteger 12", 12.34, BigInteger.valueOf(12)), Arguments.of("Negative Double -45.67 -> truncated BigInteger -45", -45.67, BigInteger.valueOf(-45)), Arguments.of("Zero Double 0.0 -> BigInteger.ZERO", 0.0, BigInteger.ZERO));
    }

    private static Stream<Arguments> bigDecimalAndBigIntegerProvider() {
        return Stream.of(Arguments.of("BigDecimal 12345.678 -> truncated BigInteger 12345", new BigDecimal("12345.678"), BigInteger.valueOf(12345)), Arguments.of("BigDecimal -9876.543 -> truncated BigInteger -9876", new BigDecimal("-9876.543"), BigInteger.valueOf(-9876)), Arguments.of("BigInteger identity conversion", new BigInteger("999999999999999999"), new BigInteger("999999999999999999")));
    }

    private static Stream<Arguments> booleanAndCharacterProvider() {
        return Stream.of(Arguments.of("Boolean true -> BigInteger.ONE", true, BigInteger.ONE, null), Arguments.of("Boolean false -> BigInteger.ZERO", false, BigInteger.ZERO, null), Arguments.of("Character '5' -> BigInteger 5", '5', BigInteger.valueOf(5), null), Arguments.of("Character 'a' -> NumberFormatException", 'a', null, NumberFormatException.class), Arguments.of("Character whitespace -> NumberFormatException", ' ', null, NumberFormatException.class));
    }

    private static Stream<Arguments> stringParsingProvider() {
        return Stream.of(Arguments.of("Valid integer string \"123456\"", "123456", BigInteger.valueOf(123456), null), Arguments.of("String with whitespace \"   42   \"", "   42   ", BigInteger.valueOf(42), null), Arguments.of("Negative numeric string \"-999\"", "-999", BigInteger.valueOf(-999), null), Arguments.of("Decimal string \"12.34\" -> NumberFormatException", "12.34", null, NumberFormatException.class), Arguments.of("Non‑numeric string \"abc123\" -> NumberFormatException", "abc123", null, NumberFormatException.class), Arguments.of("Empty string \"\" -> NumberFormatException", "", null, NumberFormatException.class), Arguments.of("String with only whitespace \"   \" -> NumberFormatException", "   ", null, NumberFormatException.class));
    }

    private static Stream<Arguments> atomicWrapperProvider() {
        return Stream.of(Arguments.of("AtomicInteger(7) -> BigInteger 7", new AtomicInteger(7), BigInteger.valueOf(7)), Arguments.of("AtomicLong(1234567890123L) -> BigInteger 1234567890123", new AtomicLong(1234567890123L), BigInteger.valueOf(1234567890123L)), Arguments.of("AtomicBoolean(true) -> BigInteger.ONE", new AtomicBoolean(true), BigInteger.ONE));
    }

    private static Stream<Arguments> bufferProvider() {
        return Stream.of(// CharBuffer numeric
        Arguments.of("CharBuffer \"12345\" -> BigInteger 12345", CharBuffer.wrap("12345"), BigInteger.valueOf(12345), null), // CharBuffer non‑numeric
        Arguments.of("CharBuffer \"12a3\" -> NumberFormatException", CharBuffer.wrap("12a3"), null, NumberFormatException.class), // CharBuffer empty
        Arguments.of("Empty CharBuffer -> NumberFormatException", CharBuffer.wrap(""), null, NumberFormatException.class), // ByteBuffer numeric ASCII digits
        Arguments.of("ByteBuffer with ASCII digits \"987\" -> BigInteger 987", ByteBuffer.wrap(new byte[] { '9', '8', '7' }), BigInteger.valueOf(987), null), // ByteBuffer non‑numeric ASCII letters
        Arguments.of("ByteBuffer with letters \"abc\" -> NumberFormatException", ByteBuffer.wrap(new byte[] { 'a', 'b', 'c' }), null, NumberFormatException.class), // ByteBuffer non‑ASCII numeric data (0x01,0x02)
        Arguments.of("ByteBuffer with non‑ASCII bytes -> NumberFormatException", ByteBuffer.wrap(new byte[] { 0x01, 0x02 }), null, NumberFormatException.class), // Empty ByteBuffer
        Arguments.of("Empty ByteBuffer -> NumberFormatException", ByteBuffer.allocate(0), null, NumberFormatException.class));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of("Date instance is unsupported", new Date()), Arguments.of("UUID instance is unsupported", UUID.randomUUID()), Arguments.of("Plain Object is unsupported", new Object()), Arguments.of("LocalDate is unsupported", LocalDate.now()), Arguments.of("LocalDateTime is unsupported", LocalDateTime.now()), Arguments.of("ZonedDateTime is unsupported", ZonedDateTime.now()), Arguments.of("ArrayList collection is unsupported", new ArrayList<>()), Arguments.of("HashMap collection is unsupported", new HashMap<>()));
    }

    @Test
    void whenSourceIsNull_thenReturnsZero() {
        // given
        Object source = null;
        // when
        BigInteger result = Converter.convert2BigInteger(source);
        // then
        assertEquals(BigInteger.ZERO, result, "Converter.convert2BigInteger(null) should return BigInteger.ZERO");
    }

    @Test
    void whenSourceIsBigInteger_thenReturnsSameInstance() {
        // given
        BigInteger source = new BigInteger("12345678901234567890");
        // when
        BigInteger result = Converter.convert2BigInteger(source);
        // then
        assertEquals(source, result, "Converter should return the original BigInteger when source is already a BigInteger");
    }

    @Test
    void whenSourceIsStringRepresentingNumber_thenParsesCorrectly() {
        // given
        String source = "9876543210";
        // when
        BigInteger result = Converter.convert2BigInteger(source);
        // then
        assertEquals(new BigInteger("9876543210"), result);
    }

    @Test
    void whenSourceIsNumber_thenConvertsUsingNumberLongValue() {
        // given
        Long source = 42L;
        // when
        BigInteger result = Converter.convert2BigInteger(source);
        // then
        assertEquals(BigInteger.valueOf(42L), result);
    }

    @Test
    void whenSourceIsByteArray_thenConvertsFromStringRepresentation() {
        // given
        // Converter treats byte[] as a String
        byte[] source = "12345".getBytes();
        // when
        BigInteger result = Converter.convert2BigInteger(source);
        // then
        assertEquals(new BigInteger("12345"), result);
    }

    @Test
    void whenSourceIsMapWithValueKey_thenExtractsAndConverts() {
        // given
        Map<String, Object> source = new HashMap<>();
        source.put("value", "7777777777777777777777777777");
        // when
        BigInteger result = Converter.convert2BigInteger(source);
        // then
        assertEquals(new BigInteger("7777777777777777777777777777"), result);
    }

    @Test
    void whenSourceIsUnsupportedType_thenThrowsClassCastException() {
        // given
        // no conversion defined for plain Object -> BigInteger
        Object source = new Object();
        // when / then
        assertThrows(ClassCastException.class, () -> Converter.convert2BigInteger(source), "Converting an unsupported type should result in a ClassCastException");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericWrapperProvider")
    void testNumericPrimitiveWrappers(String description, Object input, BigInteger expected) {
        BigInteger result = Converter.convert2BigInteger(input);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("bigDecimalAndBigIntegerProvider")
    void testBigDecimalAndBigInteger(String description, Object input, BigInteger expected) {
        BigInteger result = Converter.convert2BigInteger(input);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("booleanAndCharacterProvider")
    void testBooleanAndCharacter(String description, Object input, Object expected, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> Converter.convert2BigInteger(input), description);
        } else {
            BigInteger result = Converter.convert2BigInteger(input);
            assertEquals(expected, result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringParsingProvider")
    void testStringParsing(String description, String input, BigInteger expected, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> Converter.convert2BigInteger(input), description);
        } else {
            BigInteger result = Converter.convert2BigInteger(input);
            assertEquals(expected, result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("atomicWrapperProvider")
    void testAtomicWrappers(String description, Object input, BigInteger expected) {
        BigInteger result = Converter.convert2BigInteger(input);
        assertEquals(expected, result, description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("bufferProvider")
    void testBufferInputs(String description, Object input, Object expected, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> Converter.convert2BigInteger(input), description);
        } else {
            BigInteger result = Converter.convert2BigInteger(input);
            assertEquals(expected, result, description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedTypesProvider")
    void testUnsupportedTypes(String description, Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2BigInteger(input), description);
    }
}
