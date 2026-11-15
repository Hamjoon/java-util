package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.*;

@ExtendWith(MockitoExtension.class)
class YearConversions_toLong_Test {

    /**
     * Converter used for all test cases.
     */
    private Converter converter;

    /* --------------------------------------------------------------------- */
    private static final class ThrowingNumber extends Number {

        @Override
        public int intValue() {
            throw new RuntimeException("intValue broken");
        }

        @Override
        public long longValue() {
            throw new RuntimeException("longValue broken");
        }

        @Override
        public float floatValue() {
            throw new RuntimeException("floatValue broken");
        }

        @Override
        public double doubleValue() {
            throw new RuntimeException("doubleValue broken");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // A fresh Converter for each test guarantees a clean state.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse.
        converter = null;
    }

    private static List<Arguments> numericSourceProvider() {
        return List.of(// Integer object
        Arguments.of("Integer object should be converted directly to long via toInt.", Integer.valueOf(42), "42L"), // Long object – note the implementation routes through toInt, causing truncation.
        Arguments.of("Long object should be down‑cast to int (truncating) and then returned as long.", Long.valueOf(1234567890123L), "1234567890123L"), // Double object – fractional part dropped by intValue()
        Arguments.of("Floating‑point Number should be converted using Number.intValue() (dropping fraction).", Double.valueOf(99.9), "99L"), // BigInteger larger than int range – overflow via intValue()
        Arguments.of("BigInteger larger than int range should be converted via intValue() (overflow) then returned as long.", new BigInteger("2147483648"), "2147483648L"));
    }

    private static List<Arguments> stringSourceProvider() {
        return List.of(Arguments.of("A numeric string within int range should be parsed to int and returned as long.", "12345", "12345L"), Arguments.of("A negative numeric string should be parsed correctly and returned as long.", "-987", "-987L"), Arguments.of("String representing a value just above Integer.MAX_VALUE overflows when converted to int, resulting in Integer.MIN_VALUE which is then returned as long.", "2147483648", "-2147483648L"), Arguments.of("A non‑numeric string should cause a NumberFormatException during parsing.", "notANumber", "NumberFormatException"));
    }

    private static List<Arguments> booleanSourceProvider() {
        return List.of(Arguments.of("Boolean true is converted to int 1 and then to long.", Boolean.TRUE, 1L), Arguments.of("Boolean false is converted to int 0 and then to long.", Boolean.FALSE, 0L));
    }

    private static List<Arguments> atomicSourceProvider() {
        return List.of(Arguments.of("AtomicInteger#get() provides an int value which is returned as long.", new AtomicInteger(55), 55L), // 9999999999L truncated to int => (int)9999999999 = 1410065407
        Arguments.of("AtomicLong value exceeds int range; conversion uses (int) long, causing overflow, then cast to long.", new AtomicLong(9999999999L), 1410065407L), Arguments.of("AtomicBoolean true maps to 1, false maps to 0.", new AtomicBoolean(true), 1L));
    }

    private static List<Arguments> primitiveWrapperProvider() {
        return List.of(Arguments.of("Maximum short value converts correctly to long.", Short.valueOf((short) 32767), 32767L), Arguments.of("Minimum byte value converts correctly to long.", Byte.valueOf((byte) -128), -128L));
    }

    private static List<Arguments> nullUnsupportedProvider() {
        return List.of(// null -> 0L
        Arguments.of("Null source should be treated as zero rather than throwing an exception.", null, null, 0L), // unsupported object -> IllegalArgumentException
        Arguments.of("An object of an unsupported type should cause an IllegalArgumentException.", new Object(), IllegalArgumentException.class, null));
    }

    private static List<Arguments> stringEdgeProvider() {
        return List.of(Arguments.of("An empty string cannot be parsed as an integer and should cause NumberFormatException.", "", NumberFormatException.class), Arguments.of("A string containing only whitespace is not a valid numeric representation and must trigger NumberFormatException.", "   ", NumberFormatException.class));
    }

    private static List<Arguments> mapErrorProvider() {
        return List.of(// Empty map – missing YEAR key
        Arguments.of("A map without the required YEAR key cannot be converted and should raise IllegalArgumentException.", new LinkedHashMap<>(), IllegalArgumentException.class), // YEAR key with non‑numeric string
        Arguments.of("Map contains YEAR key but the associated value is a non‑numeric string; parsing must fail with NumberFormatException.", Map.of(YEAR, "notANumber"), NumberFormatException.class), // YEAR key with unsupported object
        Arguments.of("Map contains YEAR key with a value of an unsupported type; conversion should throw IllegalArgumentException.", Map.of(YEAR, new Object()), IllegalArgumentException.class));
    }

    @Test
    void toLong_fromYear_returnsYearValue() {
        Year year = Year.of(2021);
        long result = YearConversions.toLong(year, converter);
        assertEquals(2021L, result, "Year should be converted to its numeric value");
    }

    @Test
    void toLong_fromMapWithYearString_returnsParsedYear() {
        Map<String, Object> map = new LinkedHashMap<>();
        // MapConversions.YEAR is the expected key
        map.put(YEAR, "2022");
        long result = YearConversions.toLong(map, converter);
        assertEquals(2022L, result, "Map with YEAR key containing a numeric string should be parsed");
    }

    @Test
    void toLong_fromMapWithYearNumber_returnsNumber() {
        Map<String, Object> map = new LinkedHashMap<>();
        // direct numeric value
        map.put(YEAR, 2023);
        long result = YearConversions.toLong(map, converter);
        assertEquals(2023L, result, "Map with YEAR key containing a Number should be used directly");
    }

    @Test
    void toLong_fromStringNumber_returnsParsedLong() {
        long result = YearConversions.toLong("2024", converter);
        assertEquals(2024L, result, "String representing a number should be parsed to long");
    }

    @Test
    void toLong_fromInteger_returnsSameValue() {
        long result = YearConversions.toLong(Integer.valueOf(1999), converter);
        assertEquals(1999L, result, "Integer should be converted to long preserving the value");
    }

    @Test
    void toLong_null_returnsZero() {
        long result = YearConversions.toLong(null, converter);
        assertEquals(0L, result, "Null source should yield 0 for primitive numeric conversion");
    }

    /**
     * Numeric source types (Integer, Long, Double, BigInteger, etc.).
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericSourceProvider")
    void toLong_numericSources(String description, Object from, String expected) {
        if ("NumberFormatException".equals(expected) || "IllegalArgumentException".equals(expected) || "RuntimeException".equals(expected)) {
            // The JSON never expects an exception for numeric sources, but guard anyway.
            assertThrows(RuntimeException.class, () -> YearConversions.toLong(from, null));
        } else {
            long expectedLong = Long.parseLong(expected.replace("L", ""));
            long actual = YearConversions.toLong(from, null);
            assertEquals(expectedLong, actual, description);
        }
    }

    /**
     * String source types (numeric strings, negative numbers, overflow, non‑numeric).
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringSourceProvider")
    void toLong_stringSources(String description, Object from, String expected) {
        if ("NumberFormatException".equals(expected)) {
            assertThrows(NumberFormatException.class, () -> YearConversions.toLong(from, null), description);
        } else {
            long expectedLong = Long.parseLong(expected.replace("L", ""));
            long actual = YearConversions.toLong(from, null);
            assertEquals(expectedLong, actual, description);
        }
    }

    /**
     * Year source type.
     */
    @Test
    void toLong_yearSource() {
        Year year = Year.of(2023);
        long result = YearConversions.toLong(year, null);
        assertEquals(2023L, result, "java.time.Year should be converted using Year.getValue(), yielding the year as a long.");
    }

    /**
     * Boolean source types.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("booleanSourceProvider")
    void toLong_booleanSources(String description, Object from, long expected) {
        long actual = YearConversions.toLong(from, null);
        assertEquals(expected, actual, description);
    }

    /**
     * Atomic source types.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("atomicSourceProvider")
    void toLong_atomicSources(String description, Object from, long expected) {
        long actual = YearConversions.toLong(from, null);
        assertEquals(expected, actual, description);
    }

    /**
     * BigDecimal source type.
     */
    @Test
    void toLong_bigDecimalSource() {
        BigDecimal bd = new BigDecimal("12345.67");
        long result = YearConversions.toLong(bd, null);
        assertEquals(12345L, result, "BigDecimal.intValue() drops the fractional part; result is returned as long.");
    }

    /**
     * Primitive wrapper edge cases (Short, Byte).
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("primitiveWrapperProvider")
    void toLong_primitiveWrapperEdgeCases(String description, Object from, long expected) {
        long actual = YearConversions.toLong(from, null);
        assertEquals(expected, actual, description);
    }

    /**
     * Null and unsupported types.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullUnsupportedProvider")
    void toLong_nullAndUnsupported(String description, Object from, Class<? extends Throwable> expectedException, Long expectedValue) {
        if (expectedException != null) {
            assertThrows(expectedException, () -> YearConversions.toLong(from, null), description);
        } else {
            long actual = YearConversions.toLong(from, null);
            assertEquals(expectedValue.longValue(), actual, description);
        }
    }

    /**
     * String source edge cases (empty, whitespace).
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringEdgeProvider")
    void toLong_stringEdgeCases(String description, String from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> YearConversions.toLong(from, null), description);
    }

    /**
     * Map source error handling.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapErrorProvider")
    void toLong_mapErrorHandling(String description, Map<?, ?> map, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> YearConversions.toLong(map, null), description);
    }

    /**
     * Unsupported CharSequence source (StringBuilder).
     */
    @Test
    void toLong_unsupportedCharSequence() {
        StringBuilder sb = new StringBuilder("123");
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toLong(sb, null), "StringBuilder implements CharSequence but is not explicitly supported; the method must reject it with IllegalArgumentException.");
    }

    /**
     * Custom Number that throws from intValue().
     */
    @Test
    void toLong_customNumberExceptionPropagation() {
        ThrowingNumber tn = new ThrowingNumber();
        assertThrows(RuntimeException.class, () -> YearConversions.toLong(tn, null), "A Number subclass whose intValue() method throws a RuntimeException should cause that exception to propagate out of toLong.");
    }
}
