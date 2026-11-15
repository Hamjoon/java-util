package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.Converter;
import com.cedarsoftware.util.convert.ConverterOptions;
import org.junit.jupiter.api.*;
import java.time.Year;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class YearConversions_toLong_Test {

    /**
     * Shared Converter instance used by all tests
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special overrides are required for the tests
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Null out the static reference – helps GC in long‑running suites
        converter = null;
    }

    /* -------------------------------------------------------------
     *  Successful conversion test cases (parameterized)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> successfulConversions() {
        return Stream.of(// Year instance
        Arguments.of(Year.of(2023), 2023L), // Primitive wrappers
        Arguments.of(Integer.valueOf(1999), 1999L), Arguments.of(Long.valueOf(2021L), 2021L), Arguments.of(Short.valueOf((short) 2022), 2022L), Arguments.of(Byte.valueOf((byte) 100), 100L), // Numeric strings (including whitespace and sign)
        Arguments.of("2005", 2005L), Arguments.of(" 2029 ", 2029L), Arguments.of("+2030", 2030L), Arguments.of("-1", -1L), // CharSequence (StringBuilder)
        Arguments.of(new StringBuilder("2033"), 2033L), // BigInteger / BigDecimal
        Arguments.of(new BigInteger("2024"), 2024L), Arguments.of(new BigDecimal("2025"), 2025L), // Atomic types
        Arguments.of(new AtomicInteger(2026), 2026L), Arguments.of(new AtomicLong(2027L), 2027L), // Map containing the YEAR key
        Arguments.of(Collections.singletonMap(YEAR, 2028), 2028L), // Floating‑point numbers that represent whole numbers
        Arguments.of(Double.valueOf(2031.0), 2031L), Arguments.of(Float.valueOf(2032.0f), 2032L));
    }

    /* -------------------------------------------------------------
     *  Exception‑throwing test cases (parameterized)
     * ------------------------------------------------------------- */
    private static Stream<Arguments> exceptionConversions() {
        return Stream.of(// Null source
        Arguments.of(null, IllegalArgumentException.class), // Non‑numeric string
        Arguments.of("abc", NumberFormatException.class), // Empty map (no YEAR key)
        Arguments.of(Collections.emptyMap(), IllegalArgumentException.class), // Map with YEAR key but non‑numeric value
        Arguments.of(Collections.singletonMap(YEAR, "notANumber"), NumberFormatException.class), // BigDecimal with fractional part
        Arguments.of(new BigDecimal("2025.5"), NumberFormatException.class), // Double with fractional part
        Arguments.of(Double.valueOf(2031.5), IllegalArgumentException.class), // Unsupported types
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class), Arguments.of(Character.valueOf('A'), IllegalArgumentException.class), Arguments.of(new int[] { 2020 }, IllegalArgumentException.class), // Values that exceed int range (toInt cannot represent them)
        // > Integer.MAX_VALUE
        Arguments.of(new BigInteger("2147483648"), IllegalArgumentException.class), // > Integer.MAX_VALUE
        Arguments.of(Long.valueOf(3000000000L), IllegalArgumentException.class));
    }

    @Test
    void toLong_fromYear_returnsYearValue() {
        Year year = Year.of(2023);
        long result = YearConversions.toLong(year, converter);
        assertEquals(2023L, result, "Year object should be converted to its numeric value");
    }

    @Test
    void toLong_fromMapWithYearKey_returnsMappedValue() {
        Map<String, Object> map = Map.of(YEAR, 2025);
        long result = YearConversions.toLong(map, converter);
        assertEquals(2025L, result, "Map containing YEAR key should yield the mapped integer value");
    }

    @Test
    void toLong_fromString_parsesNumericYear() {
        String yearString = "1999";
        long result = YearConversions.toLong(yearString, converter);
        assertEquals(1999L, result, "String representation of a year should be parsed to its numeric value");
    }

    @Test
    void toLong_fromLongNumber_returnsLongValue() {
        Long number = 2022L;
        long result = YearConversions.toLong(number, converter);
        assertEquals(2022L, result, "Long number should be returned unchanged (as long)");
    }

    @Test
    void toLong_fromNull_returnsZero() {
        long result = YearConversions.toLong(null, converter);
        assertEquals(0L, result, "Null source should convert to 0 for primitive long");
    }

    @Test
    void toLong_fromUnsupportedType_returnsZero() {
        // An object that the Converter does not know how to turn into a number
        Object unsupported = new Object();
        long result = YearConversions.toLong(unsupported, converter);
        assertEquals(0L, result, "Unsupported source types should fall back to 0 for primitive long");
    }

    @ParameterizedTest(name = "[{index}] toLong({0}) → {1}")
    @MethodSource("successfulConversions")
    void toLong_returnsExpectedLong(Object from, long expected) {
        // The method under test delegates to toInt, which ultimately returns a long.
        long result = YearConversions.toLong(from, null);
        assertEquals(expected, result, () -> "Conversion of [" + from + "] should yield " + expected);
    }

    @ParameterizedTest(name = "[{index}] toLong({0}) throws {1}")
    @MethodSource("exceptionConversions")
    void toLong_throwsExpectedException(Object from, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> YearConversions.toLong(from, null), () -> "Conversion of [" + from + "] should throw " + expectedException.getSimpleName());
    }
}
