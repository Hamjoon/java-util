package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToAtomicLong_Test {

    static Stream<Arguments> validConversions() {
        return Stream.of(// primitive/wrapper numbers
        Arguments.of(123L, 123L), Arguments.of(Integer.valueOf(456), 456L), Arguments.of(new AtomicLong(789L), 789L), Arguments.of("321", 321L), Arguments.of(new BigInteger("654"), 654L), Arguments.of(new BigDecimal("987.00"), 987L), Arguments.of(new AtomicInteger(111), 111L), // booleans
        Arguments.of(Boolean.TRUE, 1L), Arguments.of(Boolean.FALSE, 0L));
    }

    static Stream<Arguments> additionalValidConversions() {
        // Calendar instance with a known epoch millisecond value
        Calendar utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utcCal.setTimeInMillis(1622548800000L);
        return Stream.of(Arguments.of((short) 12, 12L), Arguments.of((byte) -5, -5L), // double truncates
        Arguments.of(123.45d, 123L), // float truncates
        Arguments.of(67.89f, 67L), Arguments.of(CharBuffer.wrap("555"), 555L), Arguments.of(new StringBuilder("777"), 777L), // whitespace trimmed
        Arguments.of("  42 ", 42L), // explicit plus sign
        Arguments.of("+100", 100L), // negative sign
        Arguments.of("-55", -55L), Arguments.of(new AtomicBoolean(true), 1L), Arguments.of(new AtomicBoolean(false), 0L), Arguments.of(new Timestamp(1622548800000L), 1622548800000L), Arguments.of(Instant.ofEpochMilli(1622548800000L), 1622548800000L), Arguments.of(utcCal, 1622548800000L), // fractional part truncated
        Arguments.of(new BigDecimal("123.999"), 123L));
    }

    static Stream<Arguments> invalidConversions() {
        // Calendar used for overflow checks – not needed here
        return Stream.of(// non‑numeric strings
        Arguments.of("not-a-number"), // completely unrelated object
        Arguments.of(new Object()), // Date (no direct numeric representation)
        Arguments.of(new Date()), // Empty / whitespace‑only strings
        Arguments.of(""), Arguments.of("   "), // Numbers exceeding Long.MAX_VALUE
        // Long.MAX_VALUE + 1
        Arguments.of(new BigInteger("9223372036854775808")), Arguments.of(new BigDecimal("9223372036854775808.00")), // Temporal types without numeric conversion rule
        Arguments.of(LocalDate.of(2023, 1, 1)), Arguments.of(LocalDateTime.of(2023, 1, 1, 0, 0)), Arguments.of(ZonedDateTime.now()), // Unsupported collection/byte buffer types
        Arguments.of(ByteBuffer.wrap(new byte[] { 49, 50, 51 })), Arguments.of(UUID.randomUUID()), // Invalid numeric string formats
        Arguments.of("123abc"), Arguments.of("+"), Arguments.of("-"), Arguments.of("1e3"), Arguments.of("0xFF"), Arguments.of("1,000"));
    }

    @Test
    @DisplayName("Convert a Long value to AtomicLong")
    void convertFromLong() {
        Long source = 123L;
        AtomicLong result = Converter.convertToAtomicLong(source);
        assertNotNull(result);
        assertEquals(123L, result.get());
    }

    @Test
    @DisplayName("Convert a primitive long (boxed as Long) to AtomicLong")
    void convertFromPrimitiveLong() {
        long source = 456L;
        AtomicLong result = Converter.convertToAtomicLong(source);
        assertNotNull(result);
        assertEquals(456L, result.get());
    }

    @Test
    @DisplayName("Convert a String representation of a number to AtomicLong")
    void convertFromString() {
        String source = "789";
        AtomicLong result = Converter.convertToAtomicLong(source);
        assertNotNull(result);
        assertEquals(789L, result.get());
    }

    @Test
    @DisplayName("Convert a Double to AtomicLong (truncates the decimal part)")
    void convertFromDouble() {
        Double source = 12.34;
        AtomicLong result = Converter.convertToAtomicLong(source);
        assertNotNull(result);
        // The built‑in conversion follows the same rules as Long conversion,
        // i.e., it truncates toward zero.
        assertEquals(12L, result.get());
    }

    @Test
    @DisplayName("Convert an existing AtomicLong – should return the same value")
    void convertFromAtomicLong() {
        AtomicLong source = new AtomicLong(999L);
        AtomicLong result = Converter.convertToAtomicLong(source);
        assertNotNull(result);
        // The conversion returns a new AtomicLong with the same numeric value.
        assertEquals(999L, result.get());
        // It is not required to be the same instance.
        assertNotSame(source, result);
    }

    @Test
    @DisplayName("Convert null – should return null")
    void convertFromNull() {
        AtomicLong result = Converter.convertToAtomicLong(null);
        assertNull(result);
    }

    @Test
    @DisplayName("Convert an unsupported type – should return null")
    void convertFromUnsupportedType() {
        // No conversion rule for plain Object → AtomicLong
        Object source = new Object();
        AtomicLong result = Converter.convertToAtomicLong(source);
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] convert {0} → AtomicLong({1})")
    @MethodSource("validConversions")
    @DisplayName("Valid conversions to AtomicLong")
    void testValidConversions(Object input, long expected) {
        AtomicLong result = Converter.convertToAtomicLong(input);
        assertNotNull(result, "Result must not be null for valid input");
        assertEquals(expected, result.get(), "AtomicLong value mismatch");
    }

    @ParameterizedTest(name = "[{index}] additional valid conversion: {0} → {1}")
    @MethodSource("additionalValidConversions")
    @DisplayName("Additional valid conversions to AtomicLong")
    void testAdditionalValidConversions(Object input, long expected) {
        AtomicLong result = Converter.convertToAtomicLong(input);
        assertNotNull(result);
        assertEquals(expected, result.get());
    }

    @Test
    @DisplayName("Null input should return null")
    void testNullInput() {
        assertNull(Converter.convertToAtomicLong(null), "Null source must yield null result");
    }

    @ParameterizedTest(name = "[{index}] invalid conversion: {0}")
    @MethodSource("invalidConversions")
    @DisplayName("Invalid or unsupported conversions should throw IllegalArgumentException")
    void testInvalidConversions(Object input) {
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicLong(input), "Expected IllegalArgumentException for input: " + input);
    }

    @Test
    @DisplayName("Conversion from an existing AtomicLong should produce a new instance with same value")
    void testAtomicLongCopy() {
        AtomicLong source = new AtomicLong(999L);
        AtomicLong result = Converter.convertToAtomicLong(source);
        assertNotNull(result);
        assertEquals(999L, result.get());
        // The contract does not require identity preservation.
        assertNotSame(source, result, "Result may be a new instance");
    }
}
