package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class MapConversions_toInt_Test {

    /**
     * Shared Converter used by all test methods.
     */
    private static Converter converter;

    /**
     * Shared ConverterOptions used to construct the Converter.
     */
    private static ConverterOptions options;

    /**
     * Shared {@link Converter} used for the majority of test cases.
     */
    private static Converter sharedConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a default ConverterOptions and the Converter that will be reused.
        // uses the default configuration
        options = new ConverterOptions();
        converter = new Converter(options);
        // The library provides a default configuration via {@link ConverterOptions}.
        sharedConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references (helps GC when tests are run in a large suite).
        converter = null;
        options = null;
    }

    /**
     * Supplies all test cases as a {@link Stream} of {@link Arguments}.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>the source object {@code from}</li>
     *   <li>the {@link Converter} instance (or {@code null} when the case specifies it)</li>
     *   <li>the expected outcome – either an {@link Integer}, {@code null},
     *       or a {@link Class} representing an expected exception</li>
     *   <li>a short description (used as the test name)</li>
     * </ul>
     * </p>
     */
    private static Stream<Arguments> intConversionProvider() {
        // may be {@code null} for some cases
        Converter defaultConv = sharedConverter;
        return Stream.of(// -----------------------------------------------------------------
        // Group: MapConversions.toInt
        // -----------------------------------------------------------------
        Arguments.of(null, null, null, "When the source object is null, toInt should return null (no conversion performed)."), Arguments.of(Integer.valueOf(42), null, 42, "If the source is already an Integer, the method should return the same value."), Arguments.of("123", defaultConv, 123, "String representing a valid integer should be parsed and returned as Integer."), Arguments.of("-7", defaultConv, -7, "Negative numeric string should be correctly parsed."), Arguments.of(Long.valueOf(99L), defaultConv, 99, "Long value within Integer range should be converted to Integer."), Arguments.of(Double.valueOf(12.34), defaultConv, 12, "Floating‑point number should be truncated to its integer part."), Arguments.of(new BigDecimal("456"), defaultConv, 456, "BigDecimal with integer value should be converted to Integer."), Arguments.of(new BigInteger("789"), defaultConv, 789, "BigInteger within range should be converted to Integer."), Arguments.of(new AtomicInteger(55), defaultConv, 55, "AtomicInteger should be unwrapped to its int value."), Arguments.of(new AtomicLong(66L), defaultConv, 66, "AtomicLong within Integer range should be converted."), Arguments.of(new AtomicBoolean(true), defaultConv, 1, "AtomicBoolean true should map to Integer 1, false to 0."), Arguments.of(Boolean.TRUE, defaultConv, 1, "Boolean true should be converted to Integer 1."), Arguments.of(Boolean.FALSE, defaultConv, 0, "Boolean false should be converted to Integer 0."), Arguments.of(new Date(0L), defaultConv, 0, "Date epoch milliseconds should be converted to the epoch seconds integer (truncated)."), Arguments.of(new Timestamp(123456789L), defaultConv, 123456789, "Timestamp milliseconds should be converted to Integer."), Arguments.of(mapOf("value", 77), defaultConv, 77, "Map containing key \"value\" with integer should be extracted."), Arguments.of(mapOf("_v", 88), defaultConv, 88, "Map containing key \"_v\" (alias for value) should be extracted."), Arguments.of(mapOf("value", "99"), defaultConv, 99, "Map with string value should be parsed to Integer."), Arguments.of(mapOf("foo", 1, "bar", 2), defaultConv, null, "Map without recognized keys should result in null."), Arguments.of("notANumber", defaultConv, NumberFormatException.class, "Invalid numeric string should cause NumberFormatException."), Arguments.of(Long.valueOf((long) Integer.MAX_VALUE + 1L), defaultConv, ArithmeticException.class, "Value exceeding Integer.MAX_VALUE should trigger overflow handling (exception)."), // -----------------------------------------------------------------
        // Group: MapConversions.toInt_additional
        // -----------------------------------------------------------------
        Arguments.of(Float.valueOf(23.99f), defaultConv, 23, "Float value should be truncated to its integer part."), Arguments.of(Short.valueOf((short) -12), defaultConv, -12, "Short value within range should be converted directly to Integer."), Arguments.of(Byte.valueOf((byte) 7), defaultConv, 7, "Byte value should be promoted to Integer without loss."), Arguments.of(Character.valueOf('9'), defaultConv, 57, "Character representing a digit is converted to its Unicode code point (57)."), Arguments.of(mapOf("value", null), defaultConv, null, "Map contains recognized key \"value\" but the associated value is null; conversion yields null."), Arguments.of(mapOf("value", Boolean.FALSE), defaultConv, 0, "Boolean false extracted from map should map to Integer 0."), Arguments.of(mapOf("value", "  42  "), defaultConv, 42, "String with surrounding whitespace is trimmed and parsed to Integer."), Arguments.of(mapOf("_v", " -15 "), defaultConv, -15, "Alias key \"_v\" with a spaced numeric string should be trimmed and converted."), Arguments.of(mapOf("value", "notANumber"), defaultConv, NumberFormatException.class, "Map value is a non‑numeric string; conversion must fail with NumberFormatException."), Arguments.of(Long.valueOf((long) Integer.MIN_VALUE - 1L), defaultConv, ArithmeticException.class, "Long value below Integer.MIN_VALUE should trigger overflow handling."), Arguments.of(Double.NaN, defaultConv, NumberFormatException.class, "NaN cannot be represented as an integer; conversion should raise NumberFormatException."), Arguments.of(calendarAtEpoch(), defaultConv, 0, "Calendar set to epoch (default when instantiated) should convert to integer 0 (epoch seconds)."), Arguments.of(Instant.ofEpochSecond(12345L), defaultConv, 12345, "Instant representing epoch seconds should be converted directly to that integer value."), Arguments.of(mapOf("value", 100, "_v", 200), defaultConv, 100, "When both \"value\" and \"_v\" keys exist, \"value\" takes precedence for extraction."));
    }

    /**
     * Returns a {@link Calendar} instance positioned at the epoch (0 ms).
     */
    private static Calendar calendarAtEpoch() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(0L);
        return cal;
    }

    @Test
    void toInt_givenInteger_returnsSameInteger() {
        Integer source = 12;
        Integer result = MapConversions.toInt(source, sharedConverter);
        assertEquals(source, result, "Conversion of an Integer should return the same value");
    }

    @Test
    void toInt_givenStringNumber_returnsParsedInteger() {
        String source = "12345";
        Integer result = MapConversions.toInt(source, sharedConverter);
        assertEquals(12345, result, "String containing a numeric value should be parsed to Integer");
    }

    @Test
    void toInt_givenMapWithValueKey_returnsConvertedInteger() {
        // The MapConversions implementation expects a key named "value" when the source is a Map.
        Map<String, Object> source = new CompactLinkedMap<>();
        // value stored as a String to test the internal conversion path
        source.put("value", "77");
        Integer result = MapConversions.toInt(source, converter);
        assertEquals(77, result, "Map with a 'value' entry should be converted to Integer");
    }

    @Test
    void toInt_givenNull_returnsNull() {
        Integer result = MapConversions.toInt(null, sharedConverter);
        assertNull(result, "Converting a null source should yield null for wrapper types");
    }

    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("intConversionProvider")
    void toInt_various(Object from, Converter converter, Object expected, String description) {
        if (expected instanceof Class) {
            // Expected an exception – verify that it is thrown.
            Class<? extends Throwable> ex = (Class<? extends Throwable>) expected;
            assertThrows(ex, () -> MapConversions.toInt(from, converter), description);
        } else {
            // Expected a normal result (including {@code null}).
            Integer result = MapConversions.toInt(from, converter);
            assertEquals(expected, result, description);
        }
    }

    /**
     * Utility to create a {@link Map} from an even number of arguments (key, value, …).
     */
    @SafeVarargs
    private static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        return map;
    }
}
