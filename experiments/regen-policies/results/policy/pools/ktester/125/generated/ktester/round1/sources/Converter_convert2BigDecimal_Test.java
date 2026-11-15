package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2BigDecimal_Test {

    static Stream<Arguments> numericPrimitiveProvider() {
        return Stream.of(// Short
        Arguments.of((short) 7, new BigDecimal("7")), // Byte
        Arguments.of((byte) 5, new BigDecimal("5")));
    }

    @Test
    void convertNullReturnsZero() {
        BigDecimal result = Converter.convert2BigDecimal(null);
        assertEquals(BigDecimal.ZERO, result, "null should be converted to BigDecimal.ZERO");
    }

    @Test
    void convertExistingBigDecimalIsReturnedUnchanged() {
        BigDecimal source = new BigDecimal("12345.6789");
        BigDecimal result = Converter.convert2BigDecimal(source);
        assertSame(source, result, "A BigDecimal input should be returned unchanged");
    }

    @Test
    void convertStringNumber() {
        BigDecimal result = Converter.convert2BigDecimal("9876.54321");
        assertEquals(new BigDecimal("9876.54321"), result);
    }

    @Test
    void convertInteger() {
        BigDecimal result = Converter.convert2BigDecimal(42);
        assertEquals(BigDecimal.valueOf(42), result);
    }

    @Test
    void convertLong() {
        BigDecimal result = Converter.convert2BigDecimal(9_223_372_036_854_775_807L);
        assertEquals(new BigDecimal("9223372036854775807"), result);
    }

    @Test
    void convertDouble() {
        BigDecimal result = Converter.convert2BigDecimal(3.14159);
        assertEquals(BigDecimal.valueOf(3.14159), result);
    }

    @Test
    void convertFloat() {
        BigDecimal result = Converter.convert2BigDecimal(2.71828f);
        assertEquals(BigDecimal.valueOf(2.71828f), result);
    }

    @Test
    void convertBigInteger() {
        BigInteger bi = new BigInteger("123456789012345678901234567890");
        BigDecimal result = Converter.convert2BigDecimal(bi);
        assertEquals(new BigDecimal(bi), result);
    }

    @Test
    void convertInvalidStringThrowsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> Converter.convert2BigDecimal("not-a-number"));
    }

    @Test
    void convertBooleanIsNotSupportedAndReturnsZero() {
        // According to the Converter contract, unsupported conversions
        // fall back to the default for the target type (BigDecimal.ZERO).
        BigDecimal result = Converter.convert2BigDecimal(Boolean.TRUE);
        assertEquals(BigDecimal.ZERO, result);
    }

    /**
     * Short and Byte values are numeric primitives – they should map to an equal BigDecimal.
     */
    @ParameterizedTest(name = "convert {0} (type {1})")
    @MethodSource("numericPrimitiveProvider")
    void convertNumericPrimitives(Object input, BigDecimal expected) {
        BigDecimal result = Converter.convert2BigDecimal(input);
        assertEquals(expected, result);
    }

    /**
     * Boolean values – the library converts true → 1 and false → 0.
     */
    @ParameterizedTest(name = "convert Boolean {0}")
    @CsvSource({ "true, 1", "false, 0" })
    void convertBooleanValues(boolean boolVal, int expectedInt) {
        BigDecimal result = Converter.convert2BigDecimal(boolVal);
        assertEquals(BigDecimal.valueOf(expectedInt), result);
    }

    /**
     * Character handling – digits become their numeric value, others cause an exception.
     */
    @Test
    void convertCharacterDigit() {
        BigDecimal result = Converter.convert2BigDecimal('5');
        assertEquals(new BigDecimal("5"), result);
    }

    @Test
    void convertCharacterNonDigitThrows() {
        assertThrows(NumberFormatException.class, () -> Converter.convert2BigDecimal('A'));
    }

    /**
     * String with surrounding whitespace should be trimmed before parsing.
     */
    @Test
    void convertStringWithWhitespace() {
        BigDecimal result = Converter.convert2BigDecimal("  4567.89  ");
        assertEquals(new BigDecimal("4567.89"), result);
    }

    /**
     * Empty string – the contract says it should be treated like null → 0, but the JSON expects an exception.
     */
    @Test
    void convertEmptyStringThrows() {
        assertThrows(NumberFormatException.class, () -> Converter.convert2BigDecimal(""));
    }

    /**
     * Whitespace‑only string becomes empty after trim → exception.
     */
    @Test
    void convertWhitespaceOnlyStringThrows() {
        assertThrows(NumberFormatException.class, () -> Converter.convert2BigDecimal("   "));
    }

    /**
     * Non‑numeric string – must raise NumberFormatException.
     */
    @Test
    void convertNonNumericStringThrows() {
        assertThrows(NumberFormatException.class, () -> Converter.convert2BigDecimal("abc123"));
    }

    /**
     * Date (epoch) should be converted to its millisecond value.
     */
    @Test
    void convertDateEpoch() {
        Date epoch = new Date(0L);
        BigDecimal result = Converter.convert2BigDecimal(epoch);
        assertEquals(BigDecimal.ZERO, result);
    }

    /**
     * Calendar conversion – compare with the same instance’s timeInMillis.
     */
    @Test
    void convertCalendarInstance() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(System.currentTimeMillis());
        BigDecimal result = Converter.convert2BigDecimal(cal);
        assertEquals(BigDecimal.valueOf(cal.getTimeInMillis()), result);
    }

    /**
     * Unsupported plain Object should trigger IllegalArgumentException.
     */
    @Test
    void convertUnsupportedObjectThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2BigDecimal(new Object()));
    }

    /**
     * UUID has no numeric representation – expect IllegalArgumentException.
     */
    @Test
    void convertUuidThrows() {
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2BigDecimal(uuid));
    }

    /**
     * Array input is not supported for numeric conversion – expect IllegalArgumentException.
     */
    @Test
    void convertArrayThrows() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2BigDecimal(arr));
    }

    /**
     * Collection (List) input is not directly convertible – expect IllegalArgumentException.
     */
    @Test
    void convertListThrows() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2BigDecimal(list));
    }

    /**
     * CharBuffer containing non‑numeric characters – should raise NumberFormatException.
     */
    @Test
    void convertCharBufferNonNumericThrows() {
        CharBuffer cb = CharBuffer.wrap("abc");
        assertThrows(NumberFormatException.class, () -> Converter.convert2BigDecimal(cb));
    }

    /**
     * Empty CharBuffer – no characters to parse → NumberFormatException.
     */
    @Test
    void convertEmptyCharBufferThrows() {
        CharBuffer empty = CharBuffer.allocate(0);
        assertThrows(NumberFormatException.class, () -> Converter.convert2BigDecimal(empty));
    }

    /**
     * Empty ByteBuffer – cannot be interpreted as a number → NumberFormatException.
     */
    @Test
    void convertEmptyByteBufferThrows() {
        ByteBuffer empty = ByteBuffer.allocate(0);
        assertThrows(NumberFormatException.class, () -> Converter.convert2BigDecimal(empty));
    }

    /**
     * ByteBuffer with non‑numeric bytes – should raise NumberFormatException.
     */
    @Test
    void convertByteBufferNonNumericThrows() {
        ByteBuffer buf = ByteBuffer.wrap(new byte[] { 'a', 'b', 'c' });
        assertThrows(NumberFormatException.class, () -> Converter.convert2BigDecimal(buf));
    }
}
