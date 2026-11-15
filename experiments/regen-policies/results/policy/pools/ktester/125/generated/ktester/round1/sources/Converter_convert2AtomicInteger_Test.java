package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2AtomicInteger_Test {

    /**
     * --------------------------------------------------------------------- *
     */
    @Nested
    @DisplayName("Valid conversion scenarios")
    class ValidConversions {

        /**
         * Provides a stream of arguments: {@code (source, expectedIntValue)}.
         *
         * <p>The expected value is derived using the same Java conversion rules that the
         * {@code Converter} follows (e.g. {@code intValue()} for {@link BigInteger},
         * truncation for floating‑point numbers, etc.).</p>
         */
        static Stream<Arguments> validConversionProvider() {
            return Stream.of(// Integer → same value
            Arguments.of(5, 5), // String containing a valid integer → parsed value
            Arguments.of("10", 10), // Long within int range → truncated to int
            Arguments.of(7L, 7), // Double → fractional part discarded (truncation)
            Arguments.of(3.14, 3), // Float → fractional part discarded
            Arguments.of(2.718f, 2), // BigInteger → lower 32 bits (intValue())
            Arguments.of(new BigInteger("12345678901234567890"), new BigInteger("12345678901234567890").intValue()), // BigDecimal → fractional part discarded (intValue())
            Arguments.of(new BigDecimal("98765.4321"), new BigDecimal("98765.4321").intValue()), // Existing AtomicInteger → same numeric value (instance may be new or same)
            Arguments.of(new AtomicInteger(7), 7));
        }

        @ParameterizedTest(name = "convert2AtomicInteger({0}) → AtomicInteger({1})")
        @MethodSource("validConversionProvider")
        void testValidConversions(Object source, int expected) {
            // WHEN converting
            AtomicInteger result = Converter.convert2AtomicInteger(source);
            // THEN the result contains the expected integer value
            assertNotNull(result, "Result must not be null");
            assertEquals(expected, result.get(), () -> "Unexpected conversion result for source: " + source);
        }
    }

    /**
     * --------------------------------------------------------------------- *
     */
    @Nested
    @DisplayName("Unsupported conversion scenarios")
    class UnsupportedConversions {

        /**
         * Sources that are not convertible to {@link AtomicInteger}.
         * The expected exception type is {@link IllegalArgumentException},
         * but the public contract only guarantees a {@link RuntimeException},
         * therefore we assert that a subclass of {@link RuntimeException} is thrown.
         */
        static Stream<Arguments> unsupportedConversionProvider() {
            return Stream.of(// Boolean is not a numeric source for AtomicInteger
            Arguments.of(Boolean.TRUE, IllegalArgumentException.class), // Arbitrary Object instance
            Arguments.of(new Object(), IllegalArgumentException.class));
        }

        @ParameterizedTest(name = "convert2AtomicInteger({0}) throws {1}")
        @MethodSource("unsupportedConversionProvider")
        void testUnsupportedConversions(Object source, Class<? extends Throwable> expectedException) {
            // WHEN/THEN an exception is thrown
            assertThrows(expectedException, () -> Converter.convert2AtomicInteger(source), () -> "Expected conversion of " + source + " to fail with " + expectedException.getSimpleName());
        }

        @Test
        @DisplayName("Non‑numeric String throws NumberFormatException")
        void testStringNotANumberThrowsNumberFormatException() {
            // GIVEN a String that cannot be parsed as a number
            String source = "notANumber";
            // WHEN/THEN the conversion propagates NumberFormatException
            assertThrows(NumberFormatException.class, () -> Converter.convert2AtomicInteger(source), "Conversion of a non‑numeric String should throw NumberFormatException");
        }
    }

    @Test
    void convertNullReturnsZeroAtomicInteger() {
        // GIVEN a null source
        Object source = null;
        // WHEN converting to AtomicInteger
        AtomicInteger result = Converter.convert2AtomicInteger(source);
        // THEN a non‑null AtomicInteger with value 0 is returned
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.get(), "AtomicInteger value should be 0 for null input");
    }

    @Test
    void convertIntegerReturnsSameValue() {
        // GIVEN an Integer source
        Integer source = 42;
        // WHEN converting to AtomicInteger
        AtomicInteger result = Converter.convert2AtomicInteger(source);
        // THEN the AtomicInteger holds the same numeric value
        assertNotNull(result);
        assertEquals(42, result.get());
    }

    @Test
    void convertStringNumberReturnsParsedAtomicInteger() {
        // GIVEN a numeric String source
        String source = "123";
        // WHEN converting to AtomicInteger
        AtomicInteger result = Converter.convert2AtomicInteger(source);
        // THEN the AtomicInteger holds the parsed integer value
        assertNotNull(result);
        assertEquals(123, result.get());
    }

    @Test
    void convertLongWithinIntRangeReturnsCorrectValue() {
        // GIVEN a Long source that fits into an int
        Long source = 7L;
        // WHEN converting to AtomicInteger
        AtomicInteger result = Converter.convert2AtomicInteger(source);
        // THEN the value is correctly truncated to the int value
        assertNotNull(result);
        assertEquals(7, result.get());
    }

    @Test
    void convertUnsupportedTypeThrowsException() {
        // GIVEN an object that cannot be converted to an Integer (e.g., a Boolean)
        Boolean source = Boolean.TRUE;
        // WHEN/THEN converting should throw an IllegalArgumentException (or a
        // RuntimeException thrown by the underlying Converter). The exact exception
        // type is not part of the public contract, so we assert that an exception is thrown.
        assertThrows(RuntimeException.class, () -> Converter.convert2AtomicInteger(source));
    }
}
