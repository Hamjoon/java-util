package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

class Converter_convertToFloat_Test {

    static Stream<Arguments> validNumericConversions() {
        return Stream.of(Arguments.of("Integer input should be converted to Float with same numeric value.", 123, 123.0f), Arguments.of("Long input should be converted to Float.", 123L, 123.0f), Arguments.of("Double input should be converted to Float, truncating to float precision.", 123.45, 123.45f), Arguments.of("BigDecimal input should be converted to Float.", new BigDecimal("123.456"), 123.456f), Arguments.of("String representing a decimal number should be parsed to Float.", "78.9", 78.9f), Arguments.of("AtomicInteger should be converted to Float.", new AtomicInteger(55), 55.0f), Arguments.of("AtomicLong should be converted to Float.", new AtomicLong(77L), 77.0f));
    }

    static Stream<Arguments> numericEdgeValues() {
        return Stream.of(Arguments.of("Conversion of Double.NaN should produce Float.NaN.", Double.NaN, Float.NaN), Arguments.of("Conversion of Double.POSITIVE_INFINITY should produce Float.POSITIVE_INFINITY.", Double.POSITIVE_INFINITY, Float.POSITIVE_INFINITY), Arguments.of("Conversion of Double.NEGATIVE_INFINITY should produce Float.NEGATIVE_INFINITY.", Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY));
    }

    static Stream<Arguments> primitiveAndOtherNumbers() {
        return Stream.of(Arguments.of("Byte wrapper should be converted to Float preserving the numeric value.", new Byte((byte) 7), 7.0f), Arguments.of("Short wrapper should be converted to Float.", new Short((short) 9), 9.0f), Arguments.of("BigInteger should be converted to Float (possible loss of precision).", new BigInteger("123456"), 123456.0f));
    }

    static Stream<Arguments> stringParsingEdgeCases() {
        return Stream.of(Arguments.of("An empty string cannot be parsed to a Float and should raise a NumberFormatException.", "", "exception: NumberFormatException"), Arguments.of("A whitespace‑only string should also fail parsing.", "   ", "exception: NumberFormatException"), Arguments.of("A non‑numeric string should cause a NumberFormatException.", "abc123", "exception: NumberFormatException"), Arguments.of("Scientific notation string should be parsed correctly to Float.", "1.2e3", 1200.0f));
    }

    static Stream<Arguments> booleanAndCharHandling() {
        return Stream.of(Arguments.of("Boolean values are not convertible to Float; the method should reject them.", true, "IllegalArgumentException"), Arguments.of("Boolean values are not convertible to Float; the method should reject them.", false, "IllegalArgumentException"), Arguments.of("A Character is not interpreted as its numeric code point for Float conversion.", '5', "IllegalArgumentException"));
    }

    static Stream<Arguments> nioBufferConversions() {
        return Stream.of(Arguments.of("CharBuffer containing a numeric string should be parsed to Float.", CharBuffer.wrap("42.5"), 42.5f), Arguments.of("ByteBuffer with UTF‑8 bytes of a numeric string should be parsed to Float.", ByteBuffer.wrap("-12.3".getBytes()), -12.3f));
    }

    static Stream<Arguments> unsupportedComplexTypes() {
        return Stream.of(Arguments.of("UUID does not have a sensible Float representation; conversion should fail.", UUID.randomUUID()), Arguments.of("A Collection cannot be converted to Float directly.", Arrays.asList(1, 2, 3)), Arguments.of("An array of primitives is not convertible to Float.", new int[] { 1, 2, 3 }), Arguments.of("Arbitrary objects without a defined conversion should trigger an IllegalArgumentException.", new Object()));
    }

    static Stream<Arguments> unsupportedDateTimeTypes() {
        return Stream.of(Arguments.of("java.util.Date has no sensible Float representation; conversion should fail.", new Date()), Arguments.of("java.util.Calendar cannot be converted to Float.", Calendar.getInstance()), Arguments.of("java.time.LocalDate is not convertible to Float.", LocalDate.now()), Arguments.of("java.time.LocalDateTime is not convertible to Float.", LocalDateTime.now()), Arguments.of("java.time.ZonedDateTime is not convertible to Float.", ZonedDateTime.now()), Arguments.of("java.sql.Timestamp cannot be converted to Float.", Timestamp.from(Instant.now())));
    }

    static Stream<Arguments> malformedNumericStrings() {
        return Stream.of(Arguments.of("Comma‑separated numeric string is not a valid Float literal.", "1,234"), Arguments.of("String with multiple decimal points cannot be parsed to Float.", "12.3.4"), Arguments.of("Exponent without digits is malformed and should raise NumberFormatException.", "12e"), Arguments.of("Hexadecimal notation is not accepted by Float.parseFloat.", "0x1A"), Arguments.of("Leading/trailing whitespace causes parsing to fail (Float.parseFloat does not trim).", "  12.3  "));
    }

    static Stream<Arguments> nioBufferMalformed() {
        return Stream.of(Arguments.of("ByteBuffer containing non‑numeric characters cannot be parsed to Float.", ByteBuffer.wrap("abc".getBytes())), Arguments.of("CharBuffer with an invalid numeric format should raise NumberFormatException.", CharBuffer.wrap("12.3.4")), Arguments.of("Empty ByteBuffer provides no data to parse, resulting in NumberFormatException.", ByteBuffer.allocate(0)));
    }

    @Test
    void convertNullReturnsNull() {
        // given
        Object source = null;
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNull(result, "Converting null should return null for object target types");
    }

    @Test
    void convertStringNumber() {
        // given
        String source = "123.45";
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        assertEquals(123.45f, result);
    }

    @Test
    void convertInteger() {
        // given
        Integer source = 42;
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        assertEquals(42.0f, result);
    }

    @Test
    void convertDouble() {
        // given
        Double source = 3.14;
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        assertEquals(3.14f, result);
    }

    @Test
    void convertBooleanTrue() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        assertEquals(1.0f, result);
    }

    @Test
    void convertBooleanFalse() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        assertEquals(0.0f, result);
    }

    @Test
    void convertInvalidStringThrowsException() {
        // given
        String source = "not-a-number";
        // when / then
        assertThrows(NumberFormatException.class, () -> Converter.convertToFloat(source));
    }

    @Test
    void convertBigDecimal() {
        // given
        java.math.BigDecimal source = new java.math.BigDecimal("99.99");
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        assertEquals(99.99f, result);
    }

    @Test
    void convertCharDigit() {
        // given
        Character source = '7';
        // when
        Float result = Converter.convertToFloat(source);
        // then
        assertNotNull(result);
        // Character '7' is numeric value 7
        assertEquals(7.0f, result);
    }

    // ---------- valid numeric conversions ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validNumericConversions")
    void convertToFloat_validNumeric(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertNotNull(result, description);
        assertEquals(expected, result, description);
    }

    // ---------- null handling ----------
    @Test
    void convertToFloat_nullReturnsNull() {
        assertNull(Converter.convertToFloat(null), "A null source should return null rather than throwing an exception.");
    }

    // ---------- identity conversion ----------
    @Test
    void convertToFloat_identityFloat() {
        Float source = 123.45f;
        Float result = Converter.convertToFloat(source);
        assertSame(source, result, "When the source is already a Float, the same instance/value should be returned.");
    }

    // ---------- numeric edge values ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("numericEdgeValues")
    void convertToFloat_edgeValues(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertNotNull(result, description);
        if (expected.isNaN()) {
            assertTrue(result.isNaN(), description);
        } else if (expected.isInfinite()) {
            assertEquals(expected, result, description);
        } else {
            assertEquals(expected, result, description);
        }
    }

    // ---------- primitive wrappers and other number types ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("primitiveAndOtherNumbers")
    void convertToFloat_primitiveWrappers(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertNotNull(result, description);
        assertEquals(expected, result, description);
    }

    // ---------- string parsing edge cases ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringParsingEdgeCases")
    void convertToFloat_stringParsing(String description, String input, Object expected) {
        if (expected instanceof String && ((String) expected).startsWith("exception:")) {
            Class<? extends Throwable> ex = expected.equals("exception: NumberFormatException") ? NumberFormatException.class : // fallback
            IllegalArgumentException.class;
            assertThrows(ex, () -> Converter.convertToFloat(input), description);
        } else {
            Float result = Converter.convertToFloat(input);
            assertNotNull(result, description);
            assertEquals((Float) expected, result, description);
        }
    }

    // ---------- boolean and character handling ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("booleanAndCharHandling")
    void convertToFloat_booleanChar(String description, Object input, String expectedException) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToFloat(input), description);
    }

    // ---------- NIO buffers ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nioBufferConversions")
    void convertToFloat_nioBuffers(String description, Object input, Float expected) {
        Float result = Converter.convertToFloat(input);
        assertNotNull(result, description);
        assertEquals(expected, result, description);
    }

    // ---------- unsupported complex types ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedComplexTypes")
    void convertToFloat_unsupportedComplex(String description, Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToFloat(input), description);
    }

    // ---------- unsupported date/time types ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("unsupportedDateTimeTypes")
    void convertToFloat_unsupportedDateTime(String description, Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToFloat(input), description);
    }

    // ---------- malformed numeric strings ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("malformedNumericStrings")
    void convertToFloat_malformedStrings(String description, String input) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToFloat(input), description);
    }

    // ---------- NIO buffer malformed content ----------
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nioBufferMalformed")
    void convertToFloat_nioBufferMalformed(String description, Object input) {
        assertThrows(NumberFormatException.class, () -> Converter.convertToFloat(input), description);
    }
}
