package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convertToAtomicLong_Test {

    @Nested
    @DisplayName("Numeric conversions")
    class NumericConversions {

        @ParameterizedTest(name = "convert {0} -> AtomicLong({1})")
        @MethodSource("com.cedarsoftware.util.Converter_convertToAtomicLong_Test#numericConversions")
        void givenNumeric_whenConvertToAtomicLong_thenReturnsExpectedAtomicLong(Object source, long expected) {
            AtomicLong result = Converter.convertToAtomicLong(source);
            assertNotNull(result, "Result must not be null for numeric input");
            assertEquals(expected, result.get(), "AtomicLong value mismatch");
        }
    }

    @Nested
    @DisplayName("String conversion errors")
    class StringConversionErrors {

        @ParameterizedTest(name = "convert \"{0}\" -> IllegalArgumentException")
        @MethodSource("com.cedarsoftware.util.Converter_convertToAtomicLong_Test#illegalArgumentCases")
        void givenInvalidString_whenConvertToAtomicLong_thenThrows(Object source) {
            assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicLong(source), "Expected IllegalArgumentException for input: " + source);
        }
    }

    @Nested
    @DisplayName("Boolean conversions")
    class BooleanConversions {

        @ParameterizedTest(name = "convert {0} -> AtomicLong({1})")
        @MethodSource("com.cedarsoftware.util.Converter_convertToAtomicLong_Test#booleanAndAtomicWrappers")
        void givenBooleanOrAtomic_whenConvertToAtomicLong_thenReturnsOneOrZero(Object source, long expected) {
            AtomicLong result = Converter.convertToAtomicLong(source);
            assertNotNull(result);
            assertEquals(expected, result.get());
        }
    }

    @Nested
    @DisplayName("AtomicLong handling")
    class AtomicLongHandling {

        @Test
        void givenAtomicLongInstance_whenConvertToAtomicLong_thenReturnsEqualValue() {
            AtomicLong source = new AtomicLong(5555L);
            AtomicLong result = Converter.convertToAtomicLong(source);
            assertNotNull(result);
            assertEquals(5555L, result.get());
            // The contract does not guarantee the same instance
            assertNotSame(source, result, "Result may be a different instance but must have same value");
        }
    }

    @Nested
    @DisplayName("Null handling")
    class NullHandling {

        @Test
        void givenNull_whenConvertToAtomicLong_thenReturnsNull() {
            assertNull(Converter.convertToAtomicLong(null), "Null input should yield null output");
        }
    }

    @Nested
    @DisplayName("Unsupported type conversion")
    class UnsupportedTypes {

        @Test
        void givenUnsupportedObject_whenConvertToAtomicLong_thenThrows() {
            Object source = new Object();
            assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicLong(source), "Unsupported source type should cause IllegalArgumentException");
        }
    }

    @Nested
    @DisplayName("Buffer and CharSequence conversions")
    class BufferConversions {

        @ParameterizedTest(name = "convert {0} -> AtomicLong({1})")
        @MethodSource("com.cedarsoftware.util.Converter_convertToAtomicLong_Test#bufferConversions")
        void givenBuffer_whenConvertToAtomicLong_thenParsesValue(Object source, long expected) {
            AtomicLong result = Converter.convertToAtomicLong(source);
            assertNotNull(result);
            assertEquals(expected, result.get());
        }
    }

    @Nested
    @DisplayName("Map conversions")
    class MapConversions {

        @ParameterizedTest(name = "convert map {0} -> AtomicLong({1})")
        @MethodSource("com.cedarsoftware.util.Converter_convertToAtomicLong_Test#mapConversions")
        void givenMapWithValueKey_whenConvertToAtomicLong_thenExtractsAndConverts(Map<String, Object> source, long expected) {
            AtomicLong result = Converter.convertToAtomicLong(source);
            assertNotNull(result);
            assertEquals(expected, result.get());
        }
    }

    @Nested
    @DisplayName("Overflow handling")
    class OverflowHandling {

        @Test
        void givenBigIntegerOverflow_whenConvertToAtomicLong_thenThrows() {
            // Long.MAX_VALUE + 1
            BigInteger tooBig = new BigInteger("9223372036854775808");
            assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicLong(tooBig), "Overflowing BigInteger should cause IllegalArgumentException");
        }

        @Test
        void givenBigDecimalUnderflow_whenConvertToAtomicLong_thenThrows() {
            // Long.MIN_VALUE - 1
            BigDecimal tooSmall = new BigDecimal("-9223372036854775809.123");
            assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicLong(tooSmall), "Underflowing BigDecimal should cause IllegalArgumentException");
        }
    }

    /**
     * Provides numeric inputs that should be converted to the expected long value.
     */
    private static Stream<Arguments> numericConversions() {
        return Stream.of(// Long
        Arguments.of(123L, 123L), // Integer
        Arguments.of(45, 45L), // String with no whitespace
        Arguments.of("6789", 6789L), // String with surrounding whitespace
        Arguments.of("   10  ", 10L), // BigInteger within long range
        Arguments.of(new BigInteger("1234567890123"), 1234567890123L), // BigDecimal (fractional part truncated)
        Arguments.of(new BigDecimal("9876.543"), 9876L), // Short
        Arguments.of((short) 12, 12L), // Byte (negative)
        Arguments.of((byte) -5, -5L), // Float (fractional part truncated toward zero)
        Arguments.of(3.9f, 3L), // Double (negative, truncates toward zero)
        Arguments.of(-2.7d, -2L), // String with leading minus
        Arguments.of("-123", -123L), // String with explicit plus
        Arguments.of("+456", 456L));
    }

    /**
     * Provides inputs that should result in an IllegalArgumentException.
     */
    private static Stream<Arguments> illegalArgumentCases() {
        return Stream.of(// Non‑numeric string
        Arguments.of("abc"), // Empty string
        Arguments.of(""), // Whitespace‑only string
        Arguments.of("   "), // Scientific notation (not supported)
        Arguments.of("1e3"), // BigInteger overflow
        // Long.MAX_VALUE + 1
        Arguments.of(new BigInteger("9223372036854775808")), // BigDecimal overflow (integral part < Long.MIN_VALUE)
        Arguments.of(new BigDecimal("-9223372036854775809.123")), // Unsupported type: UUID
        Arguments.of(UUID.randomUUID()), // Unsupported type: LocalDate (requires import, but we can use a string to represent it)
        Arguments.of(java.time.LocalDate.now()), // Generic Object
        Arguments.of(new Object()));
    }

    /**
     * Provides Boolean and Atomic wrapper inputs that map to 0/1.
     */
    private static Stream<Arguments> booleanAndAtomicWrappers() {
        return Stream.of(// Primitive Boolean true/false
        Arguments.of(Boolean.TRUE, 1L), Arguments.of(Boolean.FALSE, 0L), // AtomicBoolean true/false
        Arguments.of(new AtomicBoolean(true), 1L), Arguments.of(new AtomicBoolean(false), 0L), // AtomicInteger
        Arguments.of(new AtomicInteger(777), 777L), // AtomicLong (should preserve value, may be a new instance)
        Arguments.of(new AtomicLong(5555L), 5555L));
    }

    /**
     * Provides CharBuffer and ByteBuffer inputs.
     */
    private static Stream<Arguments> bufferConversions() {
        return Stream.of(Arguments.of(CharBuffer.wrap("99"), 99L), // ByteBuffer containing ASCII digits '4' and '2' -> "42"
        Arguments.of(ByteBuffer.wrap(new byte[] { '4', '2' }), 42L));
    }

    /**
     * Provides Map conversion cases (value key).
     */
    private static Stream<Arguments> mapConversions() {
        return Stream.of(Arguments.of(Map.of("value", "5555"), 5555L));
    }

    @Test
    void givenNull_whenConvertToAtomicLong_thenReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        AtomicLong result = Converter.convertToAtomicLong(source);
        // THEN
        assertNull(result, "Converting null should return null");
    }

    @Test
    void givenLong_whenConvertToAtomicLong_thenReturnsAtomicLongWithSameValue() {
        // GIVEN
        Long source = 42L;
        // WHEN
        AtomicLong result = Converter.convertToAtomicLong(source);
        // THEN
        assertNotNull(result);
        assertEquals(42L, result.get());
    }

    @Test
    void givenInteger_whenConvertToAtomicLong_thenReturnsAtomicLongWithSameValue() {
        // GIVEN
        Integer source = 7;
        // WHEN
        AtomicLong result = Converter.convertToAtomicLong(source);
        // THEN
        assertNotNull(result);
        assertEquals(7L, result.get());
    }

    @Test
    void givenStringNumeric_whenConvertToAtomicLong_thenParsesAndReturnsAtomicLong() {
        // GIVEN
        String source = "123456";
        // WHEN
        AtomicLong result = Converter.convertToAtomicLong(source);
        // THEN
        assertNotNull(result);
        assertEquals(123456L, result.get());
    }

    @Test
    void givenAtomicLongInstance_whenConvertToAtomicLong_thenReturnsEqualAtomicLong() {
        // GIVEN
        AtomicLong source = new AtomicLong(999L);
        // WHEN
        AtomicLong result = Converter.convertToAtomicLong(source);
        // THEN
        assertNotNull(result);
        assertEquals(999L, result.get());
        // The converter may return a new instance; we only require value equality.
        assertNotSame(source, result, "Result may be a different instance but must have same value");
    }

    @Test
    void givenMapWithValueKey_whenConvertToAtomicLong_thenExtractsAndConverts() {
        // GIVEN
        Map<String, Object> source = Map.of("value", "5555");
        // WHEN
        AtomicLong result = Converter.convertToAtomicLong(source);
        // THEN
        assertNotNull(result);
        assertEquals(5555L, result.get());
    }

    @Test
    void givenUnsupportedType_whenConvertToAtomicLong_thenThrowsException() {
        // GIVEN
        // No conversion rule for plain Object -> AtomicLong
        Object source = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicLong(source), "Converting an unsupported type should throw IllegalArgumentException");
    }
}
