package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toAtomicLong_Test {

    private static Converter converter;

    /**
     * Shared default converter used for the majority of test cases.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – the Converter has a no‑arg constructor via options.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /**
     * Returns a {@link Converter} instance according to the specification.
     *
     * @param spec the string identifier from the JSON test case.
     * @return a concrete {@code Converter} (either the shared default or a mock).
     */
    private static Converter buildConverter(String spec) {
        if (spec == null || "null".equals(spec)) {
            return defaultConverter;
        }
        // All custom behaviours are expressed via Mockito mocks.
        Converter mock = mock(Converter.class);
        switch(spec) {
            case "customConverter":
                // Simulate a converter that knows how to extract a long from CustomNumber.
                when(mock.convert(any(), eq(Long.class))).thenAnswer(invocation -> {
                    Object from = invocation.getArgument(0);
                    // In our test data the value is a Number wrapped inside the map.
                    if (from instanceof Number) {
                        return ((Number) from).longValue();
                    }
                    // For the purpose of the test we just return the value directly.
                    return ((Number) from).longValue();
                });
                break;
            case "customConverterReturningNull":
                // Returns null for any conversion request.
                when(mock.convert(any(), eq(Long.class))).thenReturn(null);
                break;
            case "customConverterThrowing":
                // Throws IllegalArgumentException when asked to convert.
                when(mock.convert(any(), eq(Long.class))).thenThrow(new IllegalArgumentException("bad conversion"));
                break;
            case "customConverterReturningWrongType":
                // Returns a String instead of a Number, leading to ClassCastException inside NumberConversions.
                when(mock.convert(any(), eq(Long.class))).thenReturn("not-a-number");
                break;
            default:
                throw new IllegalArgumentException("Unknown converter spec: " + spec);
        }
        return mock;
    }

    /**
     * Provides all test cases that are expected to succeed and produce an {@link AtomicLong}.
     */
    private static List<Arguments> validConversionProvider() {
        // Each entry: fromSpec, converterSpec, expectedLong, description
        return List.of(// Simple numeric types
        Arguments.of(123, "null", 123L, "Long value"), Arguments.of(45, "null", 45L, "Integer value"), Arguments.of("67", "null", 67L, "Numeric String"), Arguments.of("   89  ", "null", 89L, "String with whitespace"), Arguments.of(true, "null", 1L, "Boolean true"), Arguments.of(false, "null", 0L, "Boolean false"), Arguments.of(123.45, "null", 123L, "Double truncation"), Arguments.of(Map.of("type", "BigDecimal", "value", "987.65"), "null", 987L, "BigDecimal truncation"), Arguments.of(Map.of("type", "BigInteger", "value", "1234567890123456789"), "null", 1234567890123456789L, "BigInteger within long range"), Arguments.of("0x1A", "null", 26L, "Hexadecimal string"), Arguments.of("0b1010", "null", 10L, "Binary string"), Arguments.of("0755", "null", 493L, "Octal string"), // Custom converter handling a user‑defined type
        Arguments.of(Map.of("type", "CustomNumber", "value", 555), "customConverter", 555L, "Custom converter extracts long"), // Signed numeric strings
        Arguments.of("-42", "null", -42L, "Negative string"), Arguments.of("+15", "null", 15L, "Positive sign string"), Arguments.of("1e3", "null", 1000L, "Scientific notation"), // Primitive wrappers
        Arguments.of(12.9, "null", 12L, "Double literal truncation"), Arguments.of(Map.of("type", "Float", "value", 7.8), "null", 7L, "Float wrapper"), Arguments.of(Map.of("type", "Short", "value", 9), "null", 9L, "Short value"), Arguments.of(Map.of("type", "Byte", "value", -8), "null", -8L, "Byte value"), Arguments.of(Map.of("type", "Character", "value", "5"), "null", 53L, "Character code point"), // Atomic and date/time sources
        Arguments.of(Map.of("type", "AtomicLong", "value", 12345), "null", 12345L, "AtomicLong source"), Arguments.of(Map.of("type", "Date", "value", 1609459200000L), "null", 1609459200000L, "Date epoch millis"), // Boundary values
        Arguments.of(9223372036854775807L, "null", 9223372036854775807L, "Long.MAX_VALUE"), Arguments.of(-9223372036854775808L, "null", -9223372036854775808L, "Long.MIN_VALUE"), // AtomicInteger source
        Arguments.of(Map.of("type", "AtomicInteger", "value", 42), "null", 42L, "AtomicInteger source"));
    }

    /**
     * Provides all test cases that are expected to throw an exception.
     */
    private static List<Arguments> exceptionProvider() {
        // Each entry: fromSpec, converterSpec, expectedExceptionClass, description
        return List.of(// Null source
        Arguments.of(null, "null", NullPointerException.class, "Null source should cause NPE"), // Empty or whitespace strings
        Arguments.of("", "null", NumberFormatException.class, "Empty string"), Arguments.of("   ", "null", NumberFormatException.class, "Whitespace‑only string"), // Malformed numeric strings
        Arguments.of("0xG1", "null", NumberFormatException.class, "Invalid hexadecimal"), Arguments.of("0x", "null", NumberFormatException.class, "Hex prefix without digits"), Arguments.of("0b2", "null", NumberFormatException.class, "Invalid binary digit"), Arguments.of("123abc", "null", NumberFormatException.class, "Alphanumeric string"), // Overflow cases
        Arguments.of(Map.of("type", "BigInteger", "value", "9223372036854775808"), "null", ArithmeticException.class, "BigInteger > Long.MAX_VALUE"), Arguments.of(Map.of("type", "BigInteger", "value", "-9223372036854775809"), "null", ArithmeticException.class, "BigInteger < Long.MIN_VALUE"), Arguments.of(Map.of("type", "BigDecimal", "value", "1E309"), "null", ArithmeticException.class, "BigDecimal overflow"), // Custom converter returning null
        Arguments.of(Map.of("type", "CustomNumber", "value", 999), "customConverterReturningNull", NullPointerException.class, "Custom converter returns null"), // Custom converter throwing IllegalArgumentException
        Arguments.of(Map.of("type", "CustomNumber", "value", 777), "customConverterThrowing", IllegalArgumentException.class, "Custom converter throws IAE"), // Both parameters null
        Arguments.of(null, null, NullPointerException.class, "Both parameters null"), // Unsupported type (empty map)
        Arguments.of(new HashMap<>(), "null", NumberFormatException.class, "Unsupported empty map"));
    }

    @Test
    void toAtomicLong_fromLongReturnsSameValue() {
        Long source = 123L;
        AtomicLong result = NumberConversions.toAtomicLong(source, defaultConverter);
        assertNotNull(result);
        assertEquals(123L, result.get());
    }

    @Test
    void toAtomicLong_fromStringParsesNumber() {
        String source = "456";
        AtomicLong result = NumberConversions.toAtomicLong(source, defaultConverter);
        assertNotNull(result);
        assertEquals(456L, result.get());
    }

    @Test
    void toAtomicLong_fromNullReturnsZero() {
        // According to Converter contract, null numeric conversion yields 0.
        AtomicLong result = NumberConversions.toAtomicLong(null, converter);
        assertNotNull(result);
        assertEquals(0L, result.get());
    }

    @Test
    void toAtomicLong_fromAtomicLongCopiesValue() {
        AtomicLong source = new AtomicLong(789L);
        AtomicLong result = NumberConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(789L, result.get());
        // Ensure a new instance is created (different reference)
        assertNotSame(source, result);
    }

    @Test
    void toAtomicLong_fromDoubleRoundsDown() {
        // Converter will convert to long via truncation
        Double source = 12.9;
        AtomicLong result = NumberConversions.toAtomicLong(source, converter);
        assertNotNull(result);
        assertEquals(12L, result.get());
    }

    /**
     * Builds the {@code from} argument according to the JSON specification.
     *
     * @param spec either a primitive wrapper, a String, a Number, or a Map
     *             containing {@code type} and {@code value} keys.
     * @return the concrete object to be passed to {@code toAtomicLong}.
     */
    @SuppressWarnings("unchecked")
    private static Object buildSource(Object spec) {
        if (spec == null) {
            return null;
        }
        if (spec instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) spec;
            String type = (String) map.get("type");
            Object value = map.get("value");
            switch(type) {
                case "BigDecimal":
                    return new BigDecimal((String) value);
                case "BigInteger":
                    return new BigInteger((String) value);
                case "CustomNumber":
                    // Custom type – just return the raw value; the custom Converter will handle it.
                    return value;
                case "AtomicLong":
                    return new AtomicLong(((Number) value).longValue());
                case "AtomicInteger":
                    return new AtomicInteger(((Number) value).intValue());
                case "Date":
                    return new Date(((Number) value).longValue());
                case "ZonedDateTime":
                    return ZonedDateTime.parse((String) value);
                case "Float":
                    return Float.valueOf(((Number) value).floatValue());
                case "Short":
                    return Short.valueOf(((Number) value).shortValue());
                case "Byte":
                    return Byte.valueOf(((Number) value).byteValue());
                case "Character":
                    return ((String) value).charAt(0);
                default:
                    throw new IllegalArgumentException("Unsupported custom type: " + type);
            }
        }
        // Primitive wrappers, String, Boolean, etc. are returned as‑is.
        return spec;
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validConversionProvider")
    void testToAtomicLong_ValidConversions(Object fromSpec, String converterSpec, long expected, String description) {
        Object source = buildSource(fromSpec);
        Converter conv = buildConverter(converterSpec);
        AtomicLong result = NumberConversions.toAtomicLong(source, conv);
        assertNotNull(result, "Resulting AtomicLong must not be null");
        assertEquals(expected, result.get(), () -> "Conversion failed for case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("exceptionProvider")
    void testToAtomicLong_Exceptions(Object fromSpec, String converterSpec, Class<? extends Throwable> expectedException, String description) {
        Object source = buildSource(fromSpec);
        Converter conv = buildConverter(converterSpec);
        Throwable thrown = assertThrows(expectedException, () -> NumberConversions.toAtomicLong(source, conv), () -> "Expected exception not thrown for case: " + description);
        // Optional: verify exception message contains something useful
        assertNotNull(thrown.getMessage());
    }
}
