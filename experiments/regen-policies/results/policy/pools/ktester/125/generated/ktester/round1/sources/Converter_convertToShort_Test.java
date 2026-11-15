package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToShort_Test {

    static Stream<Arguments> validConversionsProvider() {
        return Stream.of(// Short instance (should return the same value)
        Arguments.of((short) 5, (short) 5), // Integer within range
        Arguments.of(5, (short) 5), // Long within range
        Arguments.of(123L, (short) 123), // Numeric String
        Arguments.of("42", (short) 42), // BigDecimal with integral value
        Arguments.of(new BigDecimal("7"), (short) 7), // BigInteger with integral value
        Arguments.of(new BigInteger("9"), (short) 9), // Boolean true / false
        Arguments.of(true, (short) 1), Arguments.of(false, (short) 0), // Byte value
        Arguments.of((byte) 12, (short) 12), // Character (Unicode code point)
        Arguments.of('A', (short) 65));
    }

    static Stream<Arguments> outOfRangeProvider() {
        return Stream.of(// Integer overflow
        Arguments.of(40000, ArithmeticException.class), Arguments.of(-40000, ArithmeticException.class), // String overflow
        Arguments.of("123456", NumberFormatException.class), // Floating‑point overflow
        Arguments.of(40000.0d, ArithmeticException.class), Arguments.of(Float.MAX_VALUE, ArithmeticException.class), // BigDecimal overflow
        Arguments.of(new BigDecimal("40000"), ArithmeticException.class), // BigInteger overflow
        Arguments.of(new BigInteger("40000"), ArithmeticException.class), // AtomicLong overflow
        Arguments.of(new AtomicLong(40000L), ArithmeticException.class));
    }

    static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(Arguments.of(new Date(), IllegalArgumentException.class), Arguments.of(UUID.randomUUID(), IllegalArgumentException.class), Arguments.of(Timestamp.valueOf("2023-01-01 00:00:00"), IllegalArgumentException.class));
    }

    static Stream<Arguments> boundaryValuesProvider() {
        return Stream.of(Arguments.of(32767, (short) 32767), Arguments.of(-32768, (short) -32768), Arguments.of((short) 0, (short) 0));
    }

    static Stream<Arguments> floatingPointProvider() {
        return Stream.of(Arguments.of(12.0d, (short) 12), Arguments.of(12.9d, (short) 12), Arguments.of(new Float("12.5"), (short) 12));
    }

    static Stream<Arguments> bigNumberProvider() {
        return Stream.of(Arguments.of(new BigDecimal("12345.67"), (short) 12345), Arguments.of(new BigInteger("32767"), (short) 32767));
    }

    static Stream<Arguments> atomicProvider() {
        return Stream.of(Arguments.of(new AtomicInteger(123), (short) 123), Arguments.of(new AtomicLong(123L), (short) 123), Arguments.of(new AtomicBoolean(true), (short) 1), Arguments.of(new AtomicBoolean(false), (short) 0));
    }

    static Stream<Arguments> stringParsingProvider() {
        return Stream.of(Arguments.of("   42  ", (short) 42), Arguments.of("+42", (short) 42), Arguments.of("-42", (short) -42));
    }

    static Stream<Arguments> stringErrorProvider() {
        return Stream.of(Arguments.of("", NumberFormatException.class), Arguments.of("abc", NumberFormatException.class), Arguments.of("0x10", NumberFormatException.class), Arguments.of("32768", NumberFormatException.class), Arguments.of("-32769", NumberFormatException.class), Arguments.of("12.34", NumberFormatException.class), Arguments.of("1e2", NumberFormatException.class), Arguments.of("4 2", NumberFormatException.class), Arguments.of("+", NumberFormatException.class), Arguments.of("-", NumberFormatException.class));
    }

    @Test
    void convertNull_returnsNull() {
        assertNull(Converter.convertToShort(null));
    }

    @Test
    void convertFromInteger() {
        Short result = Converter.convertToShort(123);
        assertEquals(Short.valueOf((short) 123), result);
    }

    @Test
    void convertFromLong() {
        Short result = Converter.convertToShort(123L);
        assertEquals(Short.valueOf((short) 123), result);
    }

    @Test
    void convertFromDouble() {
        Short result = Converter.convertToShort(123.9);
        // conversion uses shortValue() which truncates the fractional part
        assertEquals(Short.valueOf((short) 123), result);
    }

    @Test
    void convertFromStringValidNumber() {
        Short result = Converter.convertToShort("456");
        assertEquals(Short.valueOf((short) 456), result);
    }

    @Test
    void convertFromStringInvalidNumber_throwsException() {
        assertThrows(RuntimeException.class, () -> Converter.convertToShort("not-a-number"));
    }

    @Test
    void convertFromBooleanTrue() {
        Short result = Converter.convertToShort(true);
        assertEquals(Short.valueOf((short) 1), result);
    }

    @Test
    void convertFromBooleanFalse() {
        Short result = Converter.convertToShort(false);
        assertEquals(Short.valueOf((short) 0), result);
    }

    @Test
    void convertFromBigDecimal() {
        Short result = Converter.convertToShort(new BigDecimal("12.34"));
        assertEquals(Short.valueOf((short) 12), result);
    }

    @Test
    void convertFromBigInteger() {
        Short result = Converter.convertToShort(new BigInteger("100"));
        assertEquals(Short.valueOf((short) 100), result);
    }

    @Test
    void convertFromByte() {
        Short result = Converter.convertToShort((byte) 7);
        assertEquals(Short.valueOf((short) 7), result);
    }

    @Test
    void convertFromShort() {
        Short original = 55;
        Short result = Converter.convertToShort(original);
        assertSame(original, result);
    }

    @Test
    void convertFromChar() {
        // 'A' == 65
        Short result = Converter.convertToShort('A');
        assertEquals(Short.valueOf((short) 65), result);
    }

    @Test
    void convertNumberOutOfRange_wrapsAccordingToShortCasting() {
        // 40000 exceeds Short.MAX_VALUE (32767). Casting to short yields -25536.
        Short result = Converter.convertToShort(40000);
        assertEquals(Short.valueOf((short) 40000), result);
    }

    @ParameterizedTest(name = "[{index}] convert {0} → {1}")
    @MethodSource("validConversionsProvider")
    void convertValid(Object input, short expected) {
        Short result = Converter.convertToShort(input);
        assertEquals(Short.valueOf(expected), result);
    }

    @Test
    void convertNull_returnsNull_parameterized() {
        assertNull(Converter.convertToShort(null));
    }

    @ParameterizedTest(name = "[{index}] overflow {0}")
    @MethodSource("outOfRangeProvider")
    void convertOutOfRangeThrows(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToShort(input));
    }

    @ParameterizedTest(name = "[{index}] unsupported type {0}")
    @MethodSource("invalidTypeProvider")
    void convertInvalidTypeThrows(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToShort(input));
    }

    @ParameterizedTest(name = "[{index}] boundary {0}")
    @MethodSource("boundaryValuesProvider")
    void convertBoundaryValues(Object input, short expected) {
        Short result = Converter.convertToShort(input);
        assertEquals(Short.valueOf(expected), result);
    }

    @ParameterizedTest(name = "[{index}] floating {0}")
    @MethodSource("floatingPointProvider")
    void convertFloatingPoint(Object input, short expected) {
        Short result = Converter.convertToShort(input);
        assertEquals(Short.valueOf(expected), result);
    }

    @ParameterizedTest(name = "[{index}] big number {0}")
    @MethodSource("bigNumberProvider")
    void convertBigNumber(Object input, short expected) {
        Short result = Converter.convertToShort(input);
        assertEquals(Short.valueOf(expected), result);
    }

    @ParameterizedTest(name = "[{index}] atomic {0}")
    @MethodSource("atomicProvider")
    void convertAtomic(Object input, short expected) {
        Short result = Converter.convertToShort(input);
        assertEquals(Short.valueOf(expected), result);
    }

    @ParameterizedTest(name = "[{index}] string \"{0}\" → {1}")
    @MethodSource("stringParsingProvider")
    void convertStringParsing(Object input, short expected) {
        Short result = Converter.convertToShort(input);
        assertEquals(Short.valueOf(expected), result);
    }

    @ParameterizedTest(name = "[{index}] string error \"{0}\"")
    @MethodSource("stringErrorProvider")
    void convertStringParsingThrows(Object input, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> Converter.convertToShort(input));
    }
}
