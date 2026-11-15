package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class MapConversions_toBigInteger_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; using the default options is sufficient
        converter = new Converter(new ConverterOptions());
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Null and non‑map inputs
        arguments("null input returns null", null, null), arguments("String \"12345\" parsed to BigInteger", "12345", new BigInteger("12345")), arguments("Long 12345L converted to BigInteger", 12345L, BigInteger.valueOf(12345L)), arguments("BigDecimal with zero fraction truncated", new BigDecimal("9876543210.00"), new BigInteger("9876543210")), // Map inputs with recognized keys
        arguments("Map with key \"value\" as String", Map.of("value", "555"), new BigInteger("555")), arguments("Map with short key \"_v\" as Long", Map.of("_v", 777L), BigInteger.valueOf(777L)), arguments("Map with UUID components combined", mapOf("mostSigBits", 0x1L, "leastSigBits", 0x2L), new BigInteger("1").shiftLeft(64).add(BigInteger.valueOf(2))), // Map inputs with alternative key sets
        arguments("Date key set (year, month, day)", mapOf("year", 2023, "month", 5, "day", 17), new BigInteger("20230517")), arguments("Time key set (hour, minute, second)", mapOf("hour", 14, "minute", 30, "second", 45), new BigInteger("143045")), // String variations
        arguments("String with surrounding whitespace", "  42  ", new BigInteger("42")), arguments("String with explicit plus sign", "+123", new BigInteger("123")), arguments("Negative numeric string", "-987", new BigInteger("-987")), // Map variations – simple value keys
        arguments("Map \"value\" with Integer", Map.of("value", 555), BigInteger.valueOf(555L)), arguments("Map \"_v\" with numeric String", Map.of("_v", "777"), BigInteger.valueOf(777L)), arguments("Map \"value\" with null yields null", mapOf("value", null), null), arguments("Map with both \"value\" and \"_v\" – \"value\" wins", mapOf("value", "111", "_v", 222L), new BigInteger("111")), arguments("Map \"epochMillis\" converted", mapOf("epochMillis", 1609459200000L), new BigInteger("1609459200000")), arguments("Map \"nanos\" converted", mapOf("nanos", 123456789L), new BigInteger("123456789")), // Map variations – date and time key sets (zero‑padding)
        arguments("Date key set with single‑digit month/day (zero‑padded)", mapOf("year", 2023, "month", 3, "day", 5), new BigInteger("20230305")), arguments("Time key set with single‑digit components (zero‑padded)", mapOf("hour", 9, "minute", 5, "second", 7), new BigInteger("090507")), // Date/Time object conversions
        arguments("java.util.Date epoch → 0", new Date(0L), BigInteger.ZERO), arguments("Instant epoch‑millis → BigInteger", Instant.ofEpochSecond(12345L, 0), new BigInteger("12345000")), arguments("LocalDate → concatenated YYYYMMDD", LocalDate.of(2023, 5, 17), new BigInteger("20230517")), arguments("LocalDateTime → concatenated YYYYMMDDHHMMSS", LocalDateTime.of(2023, 5, 17, 14, 30, 45), new BigInteger("20230517143045")), // Atomic and numeric wrapper conversions
        arguments("AtomicInteger unwrapped", new AtomicInteger(123), BigInteger.valueOf(123L)), arguments("AtomicLong unwrapped", new AtomicLong(9876543210L), new BigInteger("9876543210")), arguments("BigDecimal with fractional part truncated", new BigDecimal("123.45"), new BigInteger("123")), // UUID conversion
        arguments("UUID with leastSigBits=1", UUID.fromString("00000000-0000-0000-0000-000000000001"), new BigInteger("1")), arguments("Typical UUID combined", UUID.fromString("123e4567-e89b-12d3-a456-426655440000"), new BigInteger("24197857161011715162171839636988778176")));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// Unsupported or malformed inputs
        arguments("Unsupported object type throws ConversionException", new Object(), ConversionException.class), arguments("Map \"value\" with non‑numeric String throws NumberFormatException", mapOf("value", "notANumber"), NumberFormatException.class), arguments("Map UUID components with non‑numeric type throws NumberFormatException", mapOf("mostSigBits", "abc", "leastSigBits", 123L), NumberFormatException.class), // Map with non‑numeric values
        arguments("Map \"value\" Boolean throws ConversionException", mapOf("value", true), ConversionException.class), arguments("Map \"epochMillis\" non‑numeric String throws NumberFormatException", mapOf("epochMillis", "notANumber"), NumberFormatException.class), arguments("Map \"nanos\" malformed String throws NumberFormatException", mapOf("nanos", "123abc"), NumberFormatException.class), arguments("Map \"value\" empty String throws NumberFormatException", mapOf("value", ""), NumberFormatException.class), arguments("Map date components as Strings throws NumberFormatException", mapOf("year", "2023", "month", "05", "day", "07"), NumberFormatException.class), // Partial or invalid UUID components
        arguments("Map missing leastSigBits throws ConversionException", mapOf("mostSigBits", 123L), ConversionException.class), arguments("Map missing mostSigBits throws ConversionException", mapOf("leastSigBits", 456L), ConversionException.class), arguments("Map UUID component null throws ConversionException", mapOf("mostSigBits", null, "leastSigBits", 2L), ConversionException.class), arguments("Map UUID components Boolean throws ConversionException", mapOf("mostSigBits", true, "leastSigBits", false), ConversionException.class), // Empty and incomplete map inputs
        arguments("Empty map throws ConversionException", Map.of(), ConversionException.class), arguments("Incomplete date key set throws ConversionException", mapOf("year", 2023, "month", 5), ConversionException.class), arguments("Incomplete time key set throws ConversionException", mapOf("hour", 14, "minute", 30), ConversionException.class), // Whitespace and special numeric edge cases
        arguments("String with only whitespace throws NumberFormatException", "   ", NumberFormatException.class), arguments("Double.NaN throws IllegalArgumentException", Double.NaN, IllegalArgumentException.class), arguments("Float.POSITIVE_INFINITY throws IllegalArgumentException", Float.POSITIVE_INFINITY, IllegalArgumentException.class));
    }

    /**
     * Utility method to build a mutable {@link HashMap} with the supplied key/value pairs.
     * This is required for test cases where {@code Map.of(...)} cannot be used
     * (e.g., null values or more than 10 entries).
     */
    private static Map<String, Object> mapOf(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Odd number of arguments supplied to mapOf");
        }
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    @Test
    void toBigInteger_FromMapWithValueKey_ReturnsBigInteger() {
        // GIVEN a map that contains the special key "_v" with a numeric string value
        Map<String, Object> map = new HashMap<>();
        map.put("_v", "12345678901234567890");
        // WHEN converting the map to a BigInteger
        BigInteger result = MapConversions.toBigInteger(map, converter);
        // THEN the result should be the correctly parsed BigInteger
        assertEquals(new BigInteger("12345678901234567890"), result);
    }

    @Test
    void toBigInteger_FromMapWithValueKey_NumericObject() {
        // GIVEN a map that contains the special key "_v" with a Long value
        Map<String, Object> map = new HashMap<>();
        map.put("_v", 9876543210L);
        // WHEN converting the map to a BigInteger
        BigInteger result = MapConversions.toBigInteger(map, converter);
        // THEN the result should represent the same numeric value
        assertEquals(BigInteger.valueOf(9876543210L), result);
    }

    @Test
    void toBigInteger_FromString_ReturnsBigInteger() {
        // GIVEN a plain String that can be parsed as a number
        String source = "42";
        // WHEN converting the String directly
        BigInteger result = MapConversions.toBigInteger(source, converter);
        // THEN the result should be the corresponding BigInteger
        assertEquals(BigInteger.valueOf(42), result);
    }

    @Test
    void toBigInteger_FromLong_ReturnsBigInteger() {
        // GIVEN a Long instance
        Long source = 123L;
        // WHEN converting the Long directly
        BigInteger result = MapConversions.toBigInteger(source, converter);
        // THEN the result should be the same numeric value as a BigInteger
        assertEquals(BigInteger.valueOf(123L), result);
    }

    @Test
    void toBigInteger_FromNull_ReturnsNull() {
        // GIVEN a null source
        Object source = null;
        // WHEN converting null
        BigInteger result = MapConversions.toBigInteger(source, converter);
        // THEN the conversion should yield null (object types return null on null input)
        assertNull(result);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulConversionProvider")
    void toBigInteger_SuccessfulConversions(String description, Object from, BigInteger expected) {
        // WHEN
        BigInteger result = MapConversions.toBigInteger(from, converter);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionConversionProvider")
    void toBigInteger_ExpectedExceptions(String description, Object from, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> MapConversions.toBigInteger(from, converter), description);
    }
}
