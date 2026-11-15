package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigDecimal;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;

class YearConversions_toShort_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter requires a ConverterOptions instance; the default options are sufficient.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// Integer within short range
        Arguments.of(123, (short) 123), // Integer overflow – Java truncates high‑order bits
        Arguments.of(40_000, (short) 40_000), // String representing a number
        Arguments.of("456", (short) 456), // BigInteger
        Arguments.of(new BigInteger("789"), (short) 789), // Year instance
        Arguments.of(Year.of(2021), (short) 2021), // String equal to Short.MIN_VALUE
        Arguments.of("-32768", (short) -32768), // Maximum short value
        Arguments.of(32767, (short) 32767), // BigDecimal (fractional part is truncated)
        Arguments.of(new BigDecimal("12345.67"), (short) 12345), // AtomicInteger
        Arguments.of(new AtomicInteger(200), (short) 200), // AtomicLong overflow – results in truncation
        Arguments.of(new AtomicLong(50_000L), (short) 50_000), // AtomicBoolean true → 1
        Arguments.of(new AtomicBoolean(true), (short) 1), // AtomicBoolean false → 0
        Arguments.of(new AtomicBoolean(false), (short) 0));
    }

    private static Stream<Arguments> provideExceptionScenarios() {
        return Stream.of(// Both parameters null – NPE when toInt tries to use the converter
        Arguments.of(null, NullPointerException.class), // Non‑numeric string – NumberFormatException from parsing
        Arguments.of("not a number", NumberFormatException.class), // Unsupported object type – ClassCastException from toInt
        Arguments.of(new Object(), ClassCastException.class), // Empty map – YearConversions expects a YEAR entry, NPE when missing
        Arguments.of(new HashMap<>(), NullPointerException.class), // Map with YEAR key but non‑numeric string – NumberFormatException
        Arguments.of(Map.of(YEAR, "not a number"), NumberFormatException.class), // Map with YEAR key but an Object value – ClassCastException
        Arguments.of(Map.of(YEAR, new Object()), ClassCastException.class), // Boolean (unsupported source) – ClassCastException
        Arguments.of(Boolean.TRUE, ClassCastException.class), // AtomicReference (unsupported) – ClassCastException
        Arguments.of(new AtomicReference<>(123), ClassCastException.class));
    }

    @Test
    void toShort_fromYear_returnsYearValueAsShort() {
        Year year = Year.of(2023);
        short result = YearConversions.toShort(year, converter);
        assertEquals((short) 2023, result);
    }

    @Test
    void toShort_fromStringNumber_returnsParsedShort() {
        String source = "12345";
        short result = YearConversions.toShort(source, converter);
        assertEquals((short) 12345, result);
    }

    @Test
    void toShort_fromInteger_returnsSameShortValue() {
        Integer source = 100;
        short result = YearConversions.toShort(source, converter);
        assertEquals((short) 100, result);
    }

    @Test
    void toShort_fromLong_returnsCastedShortValue() {
        Long source = 5_000L;
        short result = YearConversions.toShort(source, converter);
        assertEquals((short) 5_000, result);
    }

    @Test
    void toShort_fromBigDecimal_returnsCastedShortValue() {
        BigDecimal source = new BigDecimal("321");
        short result = YearConversions.toShort(source, converter);
        assertEquals((short) 321, result);
    }

    @Test
    void toShort_fromNull_returnsZero() {
        short result = YearConversions.toShort(null, converter);
        assertEquals((short) 0, result);
    }

    @Test
    void toShort_fromMapWithValueKey_returnsCastedShort() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", 250);
        short result = YearConversions.toShort(map, converter);
        assertEquals((short) 250, result);
    }

    @Test
    void toShort_overflowValue_isCastedAccordingToJavaRules() {
        // greater than Short.MAX_VALUE (32767)
        int overflow = 40_000;
        short result = YearConversions.toShort(overflow, converter);
        // Java casts int to short by discarding high-order bits
        short expected = (short) overflow;
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected short={1}")
    @MethodSource("provideSuccessfulConversions")
    void toShort_successfulConversions(Object from, short expected) {
        // All happy‑path cases use the real converter created in @BeforeEach
        short actual = YearConversions.toShort(from, converter);
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "[{index}] from={0} with null converter → expect {1}")
    @MethodSource("provideExceptionScenarios")
    void toShort_expectedExceptions(Object from, Class<? extends Throwable> expectedException) {
        // Pass a null Converter to trigger NPE inside toInt (as defined by the spec)
        assertThrows(expectedException, () -> YearConversions.toShort(from, null));
    }

    @Test
    void toShort_fromMapWithYearKey_parsesCorrectly() {
        Map<String, Object> map = new HashMap<>();
        // YEAR constant is a String key
        map.put(YEAR, "2022");
        short result = YearConversions.toShort(map, converter);
        assertEquals((short) 2022, result);
    }
}
