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
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToFloat_Test {

    static Stream<Arguments> numberTypesProvider() {
        return Stream.of(Arguments.of("Integer → Float", 42, 42.0f), Arguments.of("Double → Float", 3.14, 3.14f), Arguments.of("Long → Float (large value)", 1234567890123456789L, 1.2345679E18f), Arguments.of("Float instance unchanged", 0.00789f, 0.00789f), Arguments.of("BigDecimal → Float (high‑precision)", new BigDecimal("123.45678901234568"), 123.45679f), Arguments.of("Numeric String → Float", "12345", 12345.0f));
    }

    static Stream<Arguments> stringParsingEdgeCasesProvider() {
        return Stream.of(Arguments.of("Whitespace trimmed", "   56.78   ", "56.78f"), Arguments.of("Negative numeric string", "-12.34", "-12.34f"), Arguments.of("String NaN", "NaN", "NaN"), Arguments.of("String Infinity", "Infinity", "Infinity"), Arguments.of("String -Infinity", "-Infinity", "-Infinity"), Arguments.of("Non‑numeric string throws", "not-a-number", "NumberFormatException"));
    }

    static Stream<Arguments> booleanConversionsProvider() {
        return Stream.of(Arguments.of("Boolean true → 1.0f", true, 1.0f), Arguments.of("Boolean false → 0.0f", false, 0.0f));
    }

    static Stream<Arguments> characterConversionsProvider() {
        return Stream.of(Arguments.of("String 'A' → Unicode 65", "A", 65.0f), Arguments.of("Character null (code point 0)", '\0', 0.0f), Arguments.of("Character 'Z' → 90", Character.valueOf('Z'), 90.0f));
    }

    static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of("Arbitrary Object", new Object()), Arguments.of("List is unsupported", Arrays.asList(1, 2, 3)), Arguments.of("UUID unsupported", UUID.randomUUID()), Arguments.of("StringBuilder unsupported", new StringBuilder("456")), Arguments.of("Optional unsupported", Optional.of(123)), Arguments.of("Enum unsupported", Month.JANUARY), Arguments.of("LocalTime unsupported", LocalTime.of(12, 34, 56)), Arguments.of("OffsetDateTime unsupported", OffsetDateTime.now()));
    }

    static Stream<Arguments> temporalTypesProvider() {
        // epoch millis for 2023‑01‑01T12:34:56Z
        long epochMillis = 1672572896000L;
        return Stream.of(Arguments.of("ISO‑8601 LocalDateTime string", "2023-01-01T12:34:56", epochMillis * 1.0f), Arguments.of("Long epoch millis", epochMillis, epochMillis * 1.0f));
    }

    static Stream<Arguments> byteShortProvider() {
        return Stream.of(Arguments.of("Byte value 7", Byte.valueOf((byte) 7), 7.0f), Arguments.of("Short value -123", Short.valueOf((short) -123), -123.0f));
    }

    static Stream<Arguments> bigIntegerProvider() {
        return Stream.of(Arguments.of("Large BigInteger within range", new BigInteger("123456789012345678901234567890"), 1.2345679E29f), Arguments.of("BigInteger exceeding Float.MAX_VALUE → Infinity", new BigInteger("10000000000000000000000000000000000000000"), Float.POSITIVE_INFINITY));
    }

    static Stream<Arguments> bufferNumericProvider() {
        return Stream.of(Arguments.of("CharBuffer \"98.6\"", CharBuffer.wrap("98.6"), 98.6f), Arguments.of("ByteBuffer \"3.2\"", ByteBuffer.wrap(new byte[] { 51, 46, 50 }), 3.2f));
    }

    static Stream<Arguments> bufferNonNumericProvider() {
        return Stream.of(Arguments.of("CharBuffer non‑numeric", CharBuffer.wrap("abc")), Arguments.of("ByteBuffer non‑numeric", ByteBuffer.wrap(new byte[] { 97, 98, 99 })));
    }

    static Stream<Arguments> temporalObjectsProvider() {
        long epoch = 1672572896000L;
        return Stream.of(Arguments.of("java.util.Date", new Date(epoch), epoch * 1.0f), Arguments.of("java.sql.Timestamp", new Timestamp(epoch), epoch * 1.0f), Arguments.of("java.time.Instant", Instant.ofEpochMilli(epoch), epoch * 1.0f), // LocalDate start of day UTC → epoch millis
        Arguments.of("java.time.LocalDate", LocalDate.of(2023, 1, 1), 1672531200000L * 1.0f), // ZonedDateTime UTC
        Arguments.of("ZonedDateTime UTC", ZonedDateTime.of(LocalDateTime.of(2023, 1, 1, 12, 34, 56), ZoneId.of("UTC")), epoch * 1.0f));
    }

    static Stream<Arguments> atomicTypesProvider() {
        return Stream.of(Arguments.of("AtomicInteger 42", new AtomicInteger(42), 42.0f), Arguments.of("AtomicLong 1234567890123", new AtomicLong(1234567890123L), 1234567890123.0f), Arguments.of("AtomicBoolean true", new AtomicBoolean(true), 1.0f), Arguments.of("AtomicBoolean false", new AtomicBoolean(false), 0.0f));
    }

    static Stream<Arguments> stringEdgeCasesProvider() {
        return Stream.of(Arguments.of("Empty string throws", "", "NumberFormatException"), Arguments.of("Explicit plus sign", "+12.34", "12.34f"), Arguments.of("Scientific notation 1e3", "1e3", "1000.0f"), Arguments.of("Scientific notation 1E-3", "1E-3", "0.001f"), Arguments.of("Whitespace only throws", "   ", "NumberFormatException"));
    }

    static Stream<Arguments> overflowUnderflowProvider() {
        return Stream.of(Arguments.of("Double.MAX_VALUE → Infinity", Double.MAX_VALUE, Float.POSITIVE_INFINITY), Arguments.of("Double.MIN_VALUE (sub‑normal) → 0.0f", Double.MIN_VALUE, 0.0f), Arguments.of("Float.MAX_VALUE unchanged", Float.MAX_VALUE, 3.4028235E38f), Arguments.of("Float.MIN_NORMAL unchanged", Float.MIN_NORMAL, 1.17549435E-38f));
    }

    static Stream<Arguments> stringLocaleHexProvider() {
        return Stream.of(Arguments.of("Comma as decimal separator", "12,34", "NumberFormatException"), Arguments.of("Hex notation 0xFF", "0xFF", "NumberFormatException"));
    }

    @Test
    void convertNullReturnsNull() {
        Float result = Converter.convertToFloat(null);
        assertNull(result, "Converting null should return null for object target types");
    }

    @Test
    void convertFloatReturnsSameInstance() {
        Float source = 12.34f;
        Float result = Converter.convertToFloat(source);
        assertEquals(source, result, "Converting a Float should return the same value");
    }

    @Test
    void convertDoubleToFloat() {
        Double source = 56.78;
        Float expected = source.floatValue();
        Float result = Converter.convertToFloat(source);
        assertEquals(expected, result, "Double should be converted to Float via narrowing");
    }

    @Test
    void convertIntegerToFloat() {
        Integer source = 42;
        Float expected = source.floatValue();
        Float result = Converter.convertToFloat(source);
        assertEquals(expected, result, "Integer should be converted to Float");
    }

    @Test
    void convertStringNumericToFloat() {
        String source = "123.45";
        Float expected = Float.valueOf(source);
        Float result = Converter.convertToFloat(source);
        assertEquals(expected, result, "Numeric String should be parsed as Float");
    }

    @Test
    void convertStringNonNumericToFloat() {
        String source = "not-a-number";
        Float result = Converter.convertToFloat(source);
        assertNull(result, "Non‑numeric String conversion should yield null");
    }

    @Test
    void convertBooleanTrueToFloat() {
        Boolean source = Boolean.TRUE;
        // true → 1
        Float expected = 1.0f;
        Float result = Converter.convertToFloat(source);
        assertEquals(expected, result, "Boolean true should convert to Float 1.0");
    }

    @Test
    void convertBooleanFalseToFloat() {
        Boolean source = Boolean.FALSE;
        // false → 0
        Float expected = 0.0f;
        Float result = Converter.convertToFloat(source);
        assertEquals(expected, result, "Boolean false should convert to Float 0.0");
    }

    @Test
    void convertBigDecimalToFloat() {
        BigDecimal source = new BigDecimal("987.65");
        Float expected = source.floatValue();
        Float result = Converter.convertToFloat(source);
        assertEquals(expected, result, "BigDecimal should be converted to Float");
    }

    @Test
    void convertBigIntegerToFloat() {
        BigInteger source = new BigInteger("123456789");
        Float expected = source.floatValue();
        Float result = Converter.convertToFloat(source);
        assertEquals(expected, result, "BigInteger should be converted to Float");
    }

    @Test
    void convertEmptyStringToFloat() {
        String source = "";
        Float result = Converter.convertToFloat(source);
        assertNull(result, "Empty String should not be convertible and must return null");
    }

    @Test
    void convertStringWithWhitespaceToFloat() {
        String source = "  3.14  ";
        Float expected = 3.14f;
        Float result = Converter.convertToFloat(source);
        assertEquals(expected, result, "String with surrounding whitespace should be trimmed and parsed");
    }

    // -------------------- Number Types --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numberTypesProvider")
    void testNumberTypes(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertEquals(expected, result, description);
    }

    // -------------------- String Parsing Edge Cases --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringParsingEdgeCasesProvider")
    void testStringParsingEdgeCases(String description, String input, Object expected) {
        if ("NumberFormatException".equals(expected)) {
            assertThrows(NumberFormatException.class, () -> Converter.convertToFloat(input), description);
        } else if ("NaN".equals(expected)) {
            Float result = Converter.convertToFloat(input);
            assertTrue(Float.isNaN(result), description);
        } else if ("Infinity".equals(expected)) {
            Float result = Converter.convertToFloat(input);
            assertEquals(Float.POSITIVE_INFINITY, result, description);
        } else if ("-Infinity".equals(expected)) {
            Float result = Converter.convertToFloat(input);
            assertEquals(Float.NEGATIVE_INFINITY, result, description);
        } else {
            Float expectedFloat = Float.valueOf(expected.toString());
            Float result = Converter.convertToFloat(input);
            assertEquals(expectedFloat, result, description);
        }
    }

    // -------------------- Boolean Conversions --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("booleanConversionsProvider")
    void testBooleanConversions(String description, Boolean input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertEquals(expected, result, description);
    }

    // -------------------- Character Conversions --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("characterConversionsProvider")
    void testCharacterConversions(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertEquals(expected, result, description);
    }

    // -------------------- Unsupported Types --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedTypesProvider")
    void testUnsupportedTypes(String description, Object input) {
        assertThrows(RuntimeException.class, () -> Converter.convertToFloat(input), description);
    }

    // -------------------- Temporal Types (String & Long) --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("temporalTypesProvider")
    void testTemporalTypes(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertEquals(expected, result, description);
    }

    // -------------------- Byte and Short Types --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("byteShortProvider")
    void testByteAndShort(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertEquals(expected, result, description);
    }

    // -------------------- BigInteger Conversion --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bigIntegerProvider")
    void testBigIntegerConversion(String description, BigInteger input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertEquals(expected, result, description);
    }

    // -------------------- CharBuffer & ByteBuffer (numeric) --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bufferNumericProvider")
    void testBufferNumeric(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertEquals(expected, result, description);
    }

    // -------------------- CharBuffer & ByteBuffer (non‑numeric) --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("bufferNonNumericProvider")
    void testBufferNonNumeric(String description, Object input) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToFloat(input), description);
    }

    // -------------------- Temporal Objects --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("temporalObjectsProvider")
    void testTemporalObjects(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertEquals(expected, result, description);
    }

    // -------------------- Atomic Types --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("atomicTypesProvider")
    void testAtomicTypes(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertEquals(expected, result, description);
    }

    // -------------------- String Edge Cases (NumberFormatException, signs, scientific) --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringEdgeCasesProvider")
    void testStringEdgeCases(String description, String input, Object expected) {
        if ("NumberFormatException".equals(expected)) {
            assertThrows(NumberFormatException.class, () -> Converter.convertToFloat(input), description);
        } else {
            Float expectedFloat = Float.valueOf(expected.toString());
            Float result = Converter.convertToFloat(input);
            assertEquals(expectedFloat, result, description);
        }
    }

    // -------------------- Overflow and Underflow --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("overflowUnderflowProvider")
    void testOverflowUnderflow(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertEquals(expected, result, description);
    }

    // -------------------- String Locale / Hex Representation --------------------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringLocaleHexProvider")
    void testStringLocaleHex(String description, String input, String expected) {
        if ("NumberFormatException".equals(expected)) {
            assertThrows(NumberFormatException.class, () -> Converter.convertToFloat(input), description);
        }
    }
}
