package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicLong;

@ExtendWith(MockitoExtension.class)
class Converter_convert2AtomicInteger_Test {

    private static Stream<Arguments> numericStringProvider() {
        return Stream.of(Arguments.of("0", 0), Arguments.of("42", 42), Arguments.of("-7", -7));
    }

    @Test
    void convert2AtomicInteger_nullInput_returnsZeroAtomicInteger() {
        // given
        Object input = null;
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.get(), "Null input must produce AtomicInteger with value 0");
    }

    @Test
    void convert2AtomicInteger_integerInput_returnsSameValue() {
        // given
        Integer input = 42;
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        assertNotNull(result);
        assertEquals(42, result.get(), "AtomicInteger should contain the same int value as the source Integer");
    }

    @Test
    void convert2AtomicInteger_stringInput_returnsParsedValue() {
        // given
        String input = "123";
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        assertNotNull(result);
        assertEquals(123, result.get(), "AtomicInteger should contain the integer parsed from the source String");
    }

    @Test
    void convert2AtomicInteger_booleanTrue_returnsOne() {
        // given
        Boolean input = Boolean.TRUE;
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        assertNotNull(result);
        assertEquals(1, result.get(), "Boolean true should be converted to AtomicInteger with value 1");
    }

    @Test
    void convert2AtomicInteger_booleanFalse_returnsZero() {
        // given
        Boolean input = Boolean.FALSE;
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        assertNotNull(result);
        assertEquals(0, result.get(), "Boolean false should be converted to AtomicInteger with value 0");
    }

    @Test
    void convert2AtomicInteger_doubleInput_returnsTruncatedValue() {
        // given
        Double input = 7.89;
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        assertNotNull(result);
        assertEquals(7, result.get(), "Double should be converted to AtomicInteger by truncating the fractional part");
    }

    @Test
    void convert2AtomicInteger_fromLongWithinRange() {
        // given
        Long input = 987_654_321L;
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        assertEquals(987_654_321, result.get(), "Long value that fits in an int should be converted without alteration");
    }

    @Test
    void convert2AtomicInteger_fromLongOverflow() {
        // given
        // Integer.MAX_VALUE + 1
        Long input = 2_147_483_648L;
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        // Expected overflow according to Java's narrowing conversion semantics
        assertEquals(-2_147_483_648, result.get(), "Long larger than Integer.MAX_VALUE must overflow to -2147483648");
    }

    @Test
    void convert2AtomicInteger_fromStringNonNumeric_throwsNumberFormatException() {
        // given
        String input = "abc";
        // when / then
        assertThrows(NumberFormatException.class, () -> Converter.convert2AtomicInteger(input), "Non‑numeric String should cause NumberFormatException");
    }

    @Test
    void convert2AtomicInteger_fromBigInteger_overflowHandled() {
        // given
        BigInteger input = new BigInteger("1234567890123");
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        // uses Java's narrowing conversion
        int expected = input.intValue();
        assertEquals(expected, result.get(), "BigInteger should be narrowed to int using intValue()");
    }

    @Test
    void convert2AtomicInteger_fromBigDecimal_truncatesFraction() {
        // given
        BigDecimal input = new BigDecimal("-9876.54321");
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        // truncates toward zero
        int expected = input.intValue();
        assertEquals(expected, result.get(), "BigDecimal should be truncated to its integer part before conversion");
    }

    @Test
    void convert2AtomicInteger_fromAtomicInteger_returnsCopy() {
        // given
        AtomicInteger source = new AtomicInteger(55);
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(source);
        // then
        assertNotSame(source, result, "A new AtomicInteger instance should be created");
        assertEquals(55, result.get(), "The value must be copied from the source AtomicInteger");
    }

    @Test
    void convert2AtomicInteger_fromAtomicLong_overflowHandled() {
        // given
        AtomicLong source = new AtomicLong(7_777_777_777L);
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(source);
        // then
        // narrowing conversion
        int expected = (int) source.get();
        assertEquals(expected, result.get(), "AtomicLong value must be narrowed to int, potentially overflowing");
    }

    @Test
    void convert2AtomicInteger_fromBooleanTrue_returnsOne() {
        // given
        Boolean input = true;
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        assertEquals(1, result.get(), "Boolean true should be interpreted as integer 1");
    }

    @Test
    void convert2AtomicInteger_fromBooleanFalse_returnsZero() {
        // given
        Boolean input = false;
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        assertEquals(0, result.get(), "Boolean false should be interpreted as integer 0");
    }

    @Test
    void convert2AtomicInteger_fromCharacterDigit() {
        // given
        Character input = '8';
        // when
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // then
        assertEquals(8, result.get(), "Numeric character should be converted to its digit value");
    }

    @Test
    void convert2AtomicInteger_fromCharacterLetter_throwsNumberFormatException() {
        // given
        Character input = 'A';
        // when / then
        assertThrows(NumberFormatException.class, () -> Converter.convert2AtomicInteger(input), "Non‑numeric character must cause NumberFormatException");
    }

    @Test
    void convert2AtomicInteger_fromUnsupportedDate_throwsIllegalArgumentException() {
        // given
        java.util.Date input = new java.util.Date();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicInteger(input), "Unsupported source type (Date) must cause IllegalArgumentException");
    }

    @Test
    void convert2AtomicInteger_fromStringOverflow_throwsNumberFormatException() {
        // given
        // Integer.MAX_VALUE + 1
        String input = "2147483648";
        // when / then
        assertThrows(NumberFormatException.class, () -> Converter.convert2AtomicInteger(input), "String representing a value beyond int range must cause NumberFormatException");
    }

    @Test
    void convert2AtomicInteger_fromStringEmpty_throwsNumberFormatException() {
        // given
        String input = "";
        // when / then
        assertThrows(NumberFormatException.class, () -> Converter.convert2AtomicInteger(input), "Empty String cannot be parsed to an int");
    }

    @Test
    void convert2AtomicInteger_fromStringWhitespace_throwsNumberFormatException() {
        // given
        String input = "  42  ";
        // when / then
        assertThrows(NumberFormatException.class, () -> Converter.convert2AtomicInteger(input), "String with surrounding whitespace is not a valid numeric literal");
    }

    @Test
    void convert2AtomicInteger_fromDoubleUnsupported_throwsIllegalArgumentException() {
        // given
        Double input = 123.456;
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicInteger(input), "Double is not a supported source type for AtomicInteger conversion");
    }

    @Test
    void convert2AtomicInteger_fromUUIDUnsupported_throwsIllegalArgumentException() {
        // given
        UUID input = UUID.randomUUID();
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicInteger(input), "UUID has no defined conversion to AtomicInteger");
    }

    @Test
    void convert2AtomicInteger_fromByteArrayUnsupported_throwsIllegalArgumentException() {
        // given
        byte[] input = new byte[] { 1, 2, 3 };
        // when / then
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicInteger(input), "Byte array is not convertible to AtomicInteger");
    }

    @ParameterizedTest(name = "String \"{0}\" should be parsed to {1}")
    @MethodSource("numericStringProvider")
    void convert2AtomicInteger_fromValidNumericString(String input, int expected) {
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        assertEquals(expected, result.get(), () -> "Parsing numeric string \"" + input + "\" should yield " + expected);
    }
}
