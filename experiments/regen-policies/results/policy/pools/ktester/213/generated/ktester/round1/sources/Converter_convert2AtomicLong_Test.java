package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2AtomicLong_Test {

    private static Stream<Arguments> primitiveNumberProvider() {
        return Stream.of(// Integer
        Arguments.of(12345, 12345L), // Long
        Arguments.of(9876543210L, 9876543210L), // Double (truncates)
        Arguments.of(3.14, 3L), // Byte max
        Arguments.of((byte) 127, 127L), // Short min
        Arguments.of((short) -32768, -32768L), // Float truncates
        Arguments.of(12.99f, 12L), // Negative double truncates toward zero
        Arguments.of(-45.6, -45L));
    }

    private static Stream<Arguments> stringNumberProvider() {
        return Stream.of(Arguments.of("4567", 4567L), Arguments.of("   89   ", 89L), // hex – supported by library
        Arguments.of("0xFF", 255L), Arguments.of("+42", 42L), Arguments.of(" -13 ", -13L));
    }

    private static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, 1L), Arguments.of(Boolean.FALSE, 0L));
    }

    private static Stream<Arguments> stringEdgeProvider() {
        return Stream.of(Arguments.of(" +42 ", 42L), Arguments.of(new StringBuilder("9876").toString(), 9876L), Arguments.of(CharBuffer.wrap("54321").toString(), 54321L));
    }

    private static Stream<Arguments> malformedStringProvider() {
        return Stream.of(// scientific notation
        Arguments.of("1e3"), // invalid hex
        Arguments.of("0xGHI"), // embedded minus
        Arguments.of("12-34"), // internal whitespace
        Arguments.of("00 123"), // plus in middle
        Arguments.of("1+2"), // thousands separator
        Arguments.of("123,456"), // binary literal not supported
        Arguments.of("0b1010"));
    }

    private static Stream<Arguments> nonNumericCharSeqProvider() {
        return Stream.of(Arguments.of(CharBuffer.wrap("abc123")), Arguments.of(new StringBuilder("xyz")));
    }

    @Test
    void convertNullReturnsZeroAtomicLong() {
        AtomicLong result = Converter.convert2AtomicLong(null);
        assertNotNull(result, "Result should never be null");
        assertEquals(0L, result.get(), "Null input must produce AtomicLong with value 0");
    }

    @Test
    void convertFromLong() {
        Long source = 123L;
        AtomicLong result = Converter.convert2AtomicLong(source);
        assertEquals(123L, result.get(), "Long should be converted to AtomicLong with same value");
    }

    @Test
    void convertFromInteger() {
        Integer source = 456;
        AtomicLong result = Converter.convert2AtomicLong(source);
        assertEquals(456L, result.get(), "Integer should be converted to AtomicLong with same numeric value");
    }

    @Test
    void convertFromStringNumeric() {
        String source = "789";
        AtomicLong result = Converter.convert2AtomicLong(source);
        assertEquals(789L, result.get(), "Numeric String should be parsed and converted to AtomicLong");
    }

    @Test
    void convertFromBigInteger() {
        BigInteger source = new BigInteger("12345678901234567890");
        AtomicLong result = Converter.convert2AtomicLong(source);
        // Converter follows the standard Java conversion rules – it takes the low 64 bits.
        long expected = source.longValue();
        assertEquals(expected, result.get(), "BigInteger should be converted to AtomicLong using longValue()");
    }

    @Test
    void convertFromBigDecimal() {
        BigDecimal source = new BigDecimal("98765.4321");
        AtomicLong result = Converter.convert2AtomicLong(source);
        // truncates fractional part
        long expected = source.longValue();
        assertEquals(expected, result.get(), "BigDecimal should be converted to AtomicLong using longValue()");
    }

    /* -------------------------------------------------------------
     *  Primitive / Wrapper Numbers
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0} -> {1}")
    @MethodSource("primitiveNumberProvider")
    void convertPrimitiveNumbers(Object input, long expected) {
        AtomicLong result = Converter.convert2AtomicLong(input);
        assertEquals(expected, result.get(), () -> "Conversion of " + input.getClass().getSimpleName() + " should yield " + expected);
    }

    /* -------------------------------------------------------------
     *  String Representations
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: \"{0}\" -> {1}")
    @MethodSource("stringNumberProvider")
    void convertFromStringRepresentations(String input, long expected) {
        AtomicLong result = Converter.convert2AtomicLong(input);
        assertEquals(expected, result.get(), () -> "String \"" + input + "\" should be parsed to " + expected);
    }

    /* -------------------------------------------------------------
     *  BigNumber Types
     * ------------------------------------------------------------- */
    @Test
    void convertBigIntegerWithinLongRange() {
        // fits in long
        BigInteger src = new BigInteger("1234567890123456789");
        AtomicLong result = Converter.convert2AtomicLong(src);
        assertEquals(src.longValue(), result.get());
    }

    @Test
    void convertBigDecimalTruncates() {
        BigDecimal src = new BigDecimal("42.99");
        AtomicLong result = Converter.convert2AtomicLong(src);
        assertEquals(42L, result.get());
    }

    @Test
    void convertBigIntegerOverflowThrows() {
        // Long.MAX_VALUE + 1
        BigInteger src = new BigInteger("9223372036854775808");
        assertThrows(RuntimeException.class, () -> Converter.convert2AtomicLong(src), "Overflowing BigInteger should cause a RuntimeException");
    }

    @Test
    void convertBigDecimalOverflowThrows() {
        // far beyond Long.MAX_VALUE
        BigDecimal src = new BigDecimal("123456789012345678901234567890");
        assertThrows(RuntimeException.class, () -> Converter.convert2AtomicLong(src), "Overflowing BigDecimal should cause a RuntimeException");
    }

    /* -------------------------------------------------------------
     *  Date / Time Conversions
     * ------------------------------------------------------------- */
    @Test
    void convertInstantToEpochMillis() {
        Instant instant = Instant.ofEpochMilli(1609459200000L);
        AtomicLong result = Converter.convert2AtomicLong(instant);
        assertEquals(1609459200000L, result.get());
    }

    @Test
    void convertDateToEpochMillis() {
        Date date = Date.from(Instant.ofEpochMilli(123456789L));
        AtomicLong result = Converter.convert2AtomicLong(date);
        assertEquals(123456789L, result.get());
    }

    @Test
    void convertTimestampToEpochMillis() {
        Timestamp ts = Timestamp.valueOf("2023-01-01 12:34:56.789");
        AtomicLong result = Converter.convert2AtomicLong(ts);
        assertEquals(ts.getTime(), result.get());
    }

    @Test
    void convertLocalDateTimeToEpochMillis() {
        LocalDateTime ldt = LocalDateTime.of(2022, 5, 20, 15, 30, 45);
        long expected = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        AtomicLong result = Converter.convert2AtomicLong(ldt);
        assertEquals(expected, result.get());
    }

    @Test
    void convertZonedDateTimeToEpochMillis() {
        ZonedDateTime zdt = ZonedDateTime.of(LocalDateTime.of(2022, 5, 20, 15, 30, 45), ZoneId.of("UTC"));
        long expected = zdt.toInstant().toEpochMilli();
        AtomicLong result = Converter.convert2AtomicLong(zdt);
        assertEquals(expected, result.get());
    }

    @Test
    void convertCalendarToEpochMillis() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(1653064245000L);
        AtomicLong result = Converter.convert2AtomicLong(cal);
        assertEquals(1653064245000L, result.get());
    }

    /* -------------------------------------------------------------
     *  Boolean & Character
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "Boolean {0} -> {1}")
    @MethodSource("booleanProvider")
    void convertBoolean(Boolean input, long expected) {
        AtomicLong result = Converter.convert2AtomicLong(input);
        assertEquals(expected, result.get());
    }

    @Test
    void convertNumericChar() {
        char ch = '5';
        AtomicLong result = Converter.convert2AtomicLong(ch);
        assertEquals(5L, result.get(), "Character digit should be converted to its numeric value");
    }

    /* -------------------------------------------------------------
     *  Atomic Types
     * ------------------------------------------------------------- */
    @Test
    void convertAtomicLong() {
        AtomicLong src = new AtomicLong(555L);
        AtomicLong result = Converter.convert2AtomicLong(src);
        assertEquals(555L, result.get());
    }

    @Test
    void convertAtomicInteger() {
        AtomicInteger src = new AtomicInteger(777);
        AtomicLong result = Converter.convert2AtomicLong(src);
        assertEquals(777L, result.get());
    }

    @Test
    void convertAtomicBooleanTrue() {
        AtomicBoolean src = new AtomicBoolean(true);
        AtomicLong result = Converter.convert2AtomicLong(src);
        assertEquals(1L, result.get());
    }

    @Test
    void convertAtomicBooleanFalse() {
        AtomicBoolean src = new AtomicBoolean(false);
        AtomicLong result = Converter.convert2AtomicLong(src);
        assertEquals(0L, result.get());
    }

    /* -------------------------------------------------------------
     *  String Edge Cases
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "Edge String \"{0}\" -> {1}")
    @MethodSource("stringEdgeProvider")
    void convertStringEdgeCases(String input, long expected) {
        AtomicLong result = Converter.convert2AtomicLong(input);
        assertEquals(expected, result.get(), () -> "Edge case string \"" + input + "\" should be parsed to " + expected);
    }

    @Test
    void convertEmptyStringThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(""), "Empty string should not be convertible");
    }

    @Test
    void convertWhitespaceOnlyStringThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong("   "), "Whitespace‑only string should not be convertible");
    }

    @Test
    void convertAlphanumericStringThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong("123abc"), "Alphanumeric string should cause conversion failure");
    }

    /* -------------------------------------------------------------
     *  Invalid / Unsupported Conversions
     * ------------------------------------------------------------- */
    @Test
    void convertUnsupportedObjectThrows() {
        Object src = new Object();
        assertThrows(RuntimeException.class, () -> Converter.convert2AtomicLong(src), "Unsupported object type should trigger a RuntimeException");
    }

    @Test
    void convertUnsupportedCollectionThrows() {
        assertThrows(RuntimeException.class, () -> Converter.convert2AtomicLong(java.util.Collections.emptyList()), "Unsupported collection should trigger a RuntimeException");
    }

    @Test
    void convertByteBufferThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(java.nio.ByteBuffer.wrap(new byte[] { 0, 1, 2 })), "ByteBuffer is not a supported source type");
    }

    @Test
    void convertByteArrayThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(new byte[] { 10, 20, 30 }), "Raw byte[] should not be convertible");
    }

    @Test
    void convertUuidThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(UUID.randomUUID()), "UUID is not convertible to AtomicLong");
    }

    @Test
    void convertDurationThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(Duration.ofHours(5)), "Duration is not convertible to AtomicLong");
    }

    @Test
    void convertMapThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(java.util.Collections.emptyMap()), "Map is not convertible to AtomicLong");
    }

    @Test
    void convertSetThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(java.util.Collections.emptySet()), "Set is not convertible to AtomicLong");
    }

    @Test
    void convertEnumThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(Thread.State.NEW), "Enum constants are not convertible to AtomicLong");
    }

    @Test
    void convertClassThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(String.class), "Class objects are not convertible to AtomicLong");
    }

    /* -------------------------------------------------------------
     *  NaN / Infinity
     * ------------------------------------------------------------- */
    @Test
    void convertDoubleNaNThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(Double.NaN), "NaN cannot be converted to a long");
    }

    @Test
    void convertDoublePositiveInfinityThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(Double.POSITIVE_INFINITY), "Positive infinity cannot be converted to a long");
    }

    @Test
    void convertDoubleNegativeInfinityThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(Double.NEGATIVE_INFINITY), "Negative infinity cannot be converted to a long");
    }

    @Test
    void convertFloatNaNThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(Float.NaN), "Float NaN cannot be converted to a long");
    }

    @Test
    void convertFloatPositiveInfinityThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(Float.POSITIVE_INFINITY), "Float positive infinity cannot be converted to a long");
    }

    @Test
    void convertFloatNegativeInfinityThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(Float.NEGATIVE_INFINITY), "Float negative infinity cannot be converted to a long");
    }

    /* -------------------------------------------------------------
     *  Malformed Numeric Strings
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "Malformed \"{0}\" throws")
    @MethodSource("malformedStringProvider")
    void convertMalformedNumericStrings(String input) {
        assertThrows(NumberFormatException.class, () -> Converter.convert2AtomicLong(input), "Malformed numeric string should cause NumberFormatException");
    }

    /* -------------------------------------------------------------
     *  Non‑numeric CharSequence
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "Non‑numeric CharSequence \"{0}\" throws")
    @MethodSource("nonNumericCharSeqProvider")
    void convertNonNumericCharSequences(CharSequence input) {
        assertThrows(NumberFormatException.class, () -> Converter.convert2AtomicLong(input), "Non‑numeric CharSequence should cause NumberFormatException");
    }

    @Test
    void convertEmptyCharSequenceThrows() {
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(new StringBuilder()), "Empty CharSequence should not be convertible");
    }
}
