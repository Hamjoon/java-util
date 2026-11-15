package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToDouble_Test {

    private static Stream<Arguments> provideTestCases() {
        List<Arguments> args = new ArrayList<>();
        // Helper lambda to add a case
        java.util.function.BiConsumer<String, Object[]> adder = (desc, vals) -> args.add(Arguments.of(desc, vals[0], vals[1]));
        // ---------- Null and Direct Types ----------
        adder.accept("When the source object is null, convertToDouble should return null.", new Object[] { null, null });
        adder.accept("If the source is already a Double, the same value is returned unchanged.", new Object[] { 12.34, 12.34 });
        // ---------- Numeric Types ----------
        adder.accept("Convert from Integer to Double.", new Object[] { 42, 42.0 });
        adder.accept("Convert from Long to Double (may lose precision, but value is expected as double).", new Object[] { 1234567890123L, 1234567890123.0 });
        adder.accept("Convert from Float to Double.", new Object[] { 314.0f, 314.0 });
        adder.accept("Convert from BigDecimal to Double.", new Object[] { new BigDecimal("123.45"), 123.45 });
        adder.accept("Convert from BigInteger to Double.", new Object[] { new BigInteger("9876543210"), 9876543210.0 });
        // ---------- String Types ----------
        adder.accept("Parse a numeric string with a decimal point.", new Object[] { "3.1415", 3.1415 });
        adder.accept("Parse a numeric string that contains leading/trailing whitespace.", new Object[] { "   2.71828   ", 2.71828 });
        adder.accept("Parse an integer string.", new Object[] { "42", 42.0 });
        adder.accept("Non‑numeric string should cause NumberFormatException.", new Object[] { "not-a-number", NumberFormatException.class });
        // ---------- Boolean Types ----------
        adder.accept("Boolean true is converted to 1.0.", new Object[] { true, 1.0 });
        adder.accept("Boolean false is converted to 0.0.", new Object[] { false, 0.0 });
        // ---------- Character Types ----------
        adder.accept("A digit character is converted to its numeric value.", new Object[] { '7', 7.0 });
        adder.accept("A non‑digit character should cause NumberFormatException.", new Object[] { 'a', NumberFormatException.class });
        // ---------- Date/Time Types ----------
        adder.accept("Date is converted to its epoch milliseconds as a double.", new Object[] { new Date(1000L), 1000.0 });
        adder.accept("Calendar is converted to its epoch milliseconds as a double.", new Object[] { calendarFromEpoch(2000L), 2000.0 });
        adder.accept("Instant is converted to its epoch milliseconds as a double.", new Object[] { Instant.ofEpochMilli(3000L), 3000.0 });
        // ---------- Atomic Types ----------
        adder.accept("AtomicInteger is converted to its int value as a double.", new Object[] { new AtomicInteger(5), 5.0 });
        adder.accept("AtomicLong is converted to its long value as a double.", new Object[] { new AtomicLong(10L), 10.0 });
        adder.accept("AtomicBoolean true is converted to 1.0.", new Object[] { new AtomicBoolean(true), 1.0 });
        adder.accept("AtomicBoolean false is converted to 0.0.", new Object[] { new AtomicBoolean(false), 0.0 });
        // ---------- Unsupported Types ----------
        adder.accept("Conversion of an unsupported type (UUID) should throw IllegalArgumentException.", new Object[] { UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), IllegalArgumentException.class });
        adder.accept("Conversion of a collection should fail with IllegalArgumentException.", new Object[] { List.of(1, 2, 3), IllegalArgumentException.class });
        // ---------- Primitive Wrapper Types ----------
        adder.accept("Convert from Byte wrapper to Double.", new Object[] { Byte.valueOf((byte) 127), 127.0 });
        adder.accept("Convert from Short wrapper (negative) to Double.", new Object[] { Short.valueOf((short) -32768), -32768.0 });
        adder.accept("Convert from Character object representing a digit to Double.", new Object[] { Character.valueOf('5'), 5.0 });
        adder.accept("Character that is not a digit should cause NumberFormatException.", new Object[] { Character.valueOf('z'), NumberFormatException.class });
        // ---------- CharSequence Types ----------
        adder.accept("Parse a scientific‑notation string held in a StringBuilder.", new Object[] { new StringBuilder("6.022e23"), 6.022e23 });
        adder.accept("Empty string should cause NumberFormatException.", new Object[] { "", NumberFormatException.class });
        adder.accept("String containing only whitespace should cause NumberFormatException.", new Object[] { "   ", NumberFormatException.class });
        adder.accept("Negative numeric string should be parsed correctly.", new Object[] { "-42.5", -42.5 });
        adder.accept("Scientific notation with negative exponent should be parsed correctly.", new Object[] { "1e-3", 0.001 });
        // ---------- Buffer Types ----------
        adder.accept("Convert from CharBuffer containing a decimal number.", new Object[] { CharBuffer.wrap("8.5"), 8.5 });
        adder.accept("CharBuffer with non‑numeric content should cause NumberFormatException.", new Object[] { CharBuffer.wrap("abc"), NumberFormatException.class });
        adder.accept("Convert from ByteBuffer containing the ASCII characters of a decimal number.", new Object[] { ByteBuffer.wrap("9.75".getBytes(StandardCharsets.UTF_8)), 9.75 });
        adder.accept("Empty ByteBuffer should cause NumberFormatException.", new Object[] { ByteBuffer.wrap(new byte[0]), NumberFormatException.class });
        // ---------- Extended Date/Time Types ----------
        adder.accept("ZonedDateTime is converted to its epoch milliseconds as a double.", new Object[] { ZonedDateTime.ofInstant(Instant.ofEpochMilli(4000L), ZoneId.systemDefault()), 4000.0 });
        adder.accept("LocalDateTime is converted to epoch milliseconds (using the system default zone) as a double.", new Object[] { LocalDateTime.ofInstant(Instant.ofEpochMilli(5000L), ZoneId.systemDefault()), 5000.0 });
        adder.accept("Timestamp is converted to its epoch milliseconds as a double.", new Object[] { new Timestamp(6000L), 6000.0 });
        // ---------- Special Numeric Edge Cases ----------
        adder.accept("Very large BigInteger exceeds Double.MAX_VALUE; conversion should result in positive infinity.", new Object[] { new BigInteger("1234567890123456789012345678901234567890"), Double.POSITIVE_INFINITY });
        adder.accept("BigDecimal at Double.MAX_VALUE should convert to the same double value.", new Object[] { new BigDecimal("1.7976931348623157E308"), 1.7976931348623157e+308 });
        adder.accept("NaN Double input should be returned unchanged.", new Object[] { Double.NaN, Double.NaN });
        adder.accept("Positive Infinity Double input should be returned unchanged.", new Object[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY });
        // ---------- Additional Unsupported Types ----------
        adder.accept("Conversion of a Map should throw IllegalArgumentException.", new Object[] { Map.of("key", "value"), IllegalArgumentException.class });
        adder.accept("Conversion of a Set should throw IllegalArgumentException.", new Object[] { Set.of(1, 2, 3), IllegalArgumentException.class });
        // ---------- String Parsing Edge Cases ----------
        adder.accept("String containing numeric characters followed by non‑numeric characters should cause NumberFormatException.", new Object[] { "123abc", NumberFormatException.class });
        adder.accept("Hexadecimal notation is not supported; conversion should throw NumberFormatException.", new Object[] { "0x10", NumberFormatException.class });
        adder.accept("Locale‑specific decimal separator (comma) should cause NumberFormatException.", new Object[] { "3,14", NumberFormatException.class });
        // ---------- Unsupported Types Additional ----------
        adder.accept("LocalTime does not contain a date component; conversion to Double is unsupported and should throw IllegalArgumentException.", new Object[] { LocalTime.of(12, 34, 56), IllegalArgumentException.class });
        adder.accept("AtomicReference is not a supported source type; conversion should throw IllegalArgumentException.", new Object[] { new AtomicReference<>("test"), IllegalArgumentException.class });
        adder.accept("Enum values are not convertible to Double; should result in IllegalArgumentException.", new Object[] { SampleEnum.SAMPLE, IllegalArgumentException.class });
        // ---------- Array Types ----------
        adder.accept("Primitive arrays are not supported for conversion; should throw IllegalArgumentException.", new Object[] { new int[] { 1, 2, 3 }, IllegalArgumentException.class });
        adder.accept("Object arrays (e.g., Double[]) are not supported; conversion should throw IllegalArgumentException.", new Object[] { new Double[] { 1.1, 2.2 }, IllegalArgumentException.class });
        // ---------- Null CharSequence Types ----------
        adder.accept("A null CharSequence (e.g., StringBuilder with null value) should result in NullPointerException when the converter attempts to read its content.", new Object[] { nullStringBuilder(), NullPointerException.class });
        adder.accept("A null CharBuffer should cause NullPointerException during conversion.", new Object[] { nullCharBuffer(), NullPointerException.class });
        return args.stream();
    }

    private static Calendar calendarFromEpoch(long epochMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epochMillis);
        return cal;
    }

    /**
     * Returns a {@code StringBuilder} instance whose internal value is {@code null}.
     */
    private static StringBuilder nullStringBuilder() {
        // Java's StringBuilder cannot hold a null value directly; we simulate the situation
        // by passing a null reference to the test – the converter will attempt to treat it
        // as a CharSequence and consequently throw NPE.
        return null;
    }

    /**
     * Returns a {@code CharBuffer} that is {@code null}.
     */
    private static CharBuffer nullCharBuffer() {
        return null;
    }

    @Test
    void convert_Null_ReturnsNull() {
        Double result = Converter.convertToDouble(null);
        assertNull(result, "Converting a null source should return null for object target types");
    }

    @Test
    void convert_StringNumber_ReturnsParsedDouble() {
        Double result = Converter.convertToDouble("123.45");
        assertEquals(123.45, result);
    }

    @Test
    void convert_Integer_ReturnsDoubleValue() {
        Double result = Converter.convertToDouble(Integer.valueOf(42));
        assertEquals(42.0, result);
    }

    @Test
    void convert_BigDecimal_ReturnsDoubleValue() {
        Double result = Converter.convertToDouble(new BigDecimal("987.65"));
        assertEquals(987.65, result);
    }

    @Test
    void convert_BooleanTrue_ReturnsOne() {
        Double result = Converter.convertToDouble(Boolean.TRUE);
        assertEquals(1.0, result);
    }

    @Test
    void convert_BooleanFalse_ReturnsZero() {
        Double result = Converter.convertToDouble(Boolean.FALSE);
        assertEquals(0.0, result);
    }

    @Test
    void convert_Date_ReturnsEpochMillisAsDouble() {
        Date now = new Date();
        Double result = Converter.convertToDouble(now);
        assertEquals(now.getTime(), result);
    }

    @Test
    void convert_MapWithValueKey_ReturnsParsedDouble() {
        Map<String, Object> map = new HashMap<>();
        // the converter looks for a “value” or “_v” key
        map.put("_v", "321.99");
        Double result = Converter.convertToDouble(map);
        assertEquals(321.99, result);
    }

    @Test
    void convert_UnsupportedType_ReturnsNull() {
        // no built‑in conversion from plain Object to Double
        Object unsupported = new Object();
        Double result = Converter.convertToDouble(unsupported);
        assertNull(result, "Unsupported source types should yield null for object target types");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTestCases")
    void convertToDouble_ShouldBehaveAsSpecified(String description, Object input, Object expected) {
        // Expected is either a Double (including NaN/Infinity), null, or a Throwable class.
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Exception expected
            assertThrows((Class<? extends Throwable>) expected, () -> Converter.convertToDouble(input), description);
        } else if (expected == null) {
            // Null result expected
            assertNull(Converter.convertToDouble(input), description);
        } else {
            // Normal Double result expected
            Double result = Converter.convertToDouble(input);
            Double exp = (Double) expected;
            if (exp.isNaN()) {
                assertTrue(result.isNaN(), description);
            } else if (exp.isInfinite()) {
                assertTrue(result.isInfinite() && result > 0, description);
            } else {
                assertEquals(exp, result, 1e-12, description);
            }
        }
    }
}
