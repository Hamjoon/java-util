package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.Year;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cedarsoftware.util.CompactLinkedMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.*;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;

@ExtendWith(MockitoExtension.class)
class YearConversions_toBigDecimal_Test {

    @Mock
    private Converter // mocked dependency used by YearConversions
    mockConverter;

    @Mock
    private Converter // used for the “converter throws” scenario
    throwingConverter;

    @AfterEach
    void teardownAfterEach() {
        // Reset mock interactions to avoid cross‑test interference
        reset(mockConverter, throwingConverter);
    }

    private static List<Arguments> normalConversionProvider() {
        return List.of(// Integer input
        Arguments.of("Integer input should be converted to its int value and then to BigDecimal", 2023, BigDecimal.valueOf(2023)), // String representing a year
        Arguments.of("String representing a year should be parsed to int and converted to BigDecimal", "2025", BigDecimal.valueOf(2025)), // java.time.Year instance
        Arguments.of("Year instance should be converted via toInt and then to BigDecimal", Year.of(2022), BigDecimal.valueOf(2022)), // Null input
        Arguments.of("Null input should be treated as 0 by toInt and result in BigDecimal zero", null, BigDecimal.ZERO), // Date – use current date, extract year via Calendar
        Arguments.of("Date object should be converted to its year component via toInt and then to BigDecimal", new Date(), BigDecimal.valueOf(getYearFromDate())), // BigInteger
        Arguments.of("BigInteger input should be converted to int and then to BigDecimal", new BigInteger("1999"), BigDecimal.valueOf(1999)), // BigDecimal (truncates fraction)
        Arguments.of("BigDecimal input should be converted via toInt (truncating any fraction) and then to BigDecimal", new BigDecimal("2001.0"), BigDecimal.valueOf(2001)), // AtomicInteger
        Arguments.of("AtomicInteger should be unwrapped to its int value and converted to BigDecimal", new AtomicInteger(2024), BigDecimal.valueOf(2024)), // AtomicLong
        Arguments.of("AtomicLong should be unwrapped to its long value, cast to int, and converted to BigDecimal", new AtomicLong(2026L), BigDecimal.valueOf(2026)), // Short primitive (auto‑boxed)
        Arguments.of("Short primitive should be widened to int and converted to BigDecimal", (short) 2027, BigDecimal.valueOf(2027)), // Byte primitive (auto‑boxed)
        Arguments.of("Byte primitive should be widened to int and converted to BigDecimal", (byte) 127, BigDecimal.valueOf(127)), // Long within int range
        Arguments.of("Long value that fits into int range should be cast to int and converted", 2028L, BigDecimal.valueOf(2028)), // Long exceeding int range – low 32 bits
        Arguments.of("Long value exceeding int range is truncated by casting to int (low 32 bits) before conversion", 5000000000L, BigDecimal.valueOf((int) 5000000000L)), // Double (truncates)
        Arguments.of("Double value is truncated to its integer part before conversion", 2029.99, BigDecimal.valueOf(2029)), // Float (truncates)
        Arguments.of("Float value is truncated to its integer part before conversion", 2030.5f, BigDecimal.valueOf(2030)), // Boolean true
        Arguments.of("Boolean true is interpreted as 1 by toInt and then converted", true, BigDecimal.valueOf(1)), // Boolean false
        Arguments.of("Boolean false is interpreted as 0 by toInt and then converted", false, BigDecimal.valueOf(0)), // AtomicBoolean true
        Arguments.of("AtomicBoolean true unwraps to true → 1 → BigDecimal", new AtomicBoolean(true), BigDecimal.valueOf(1)), // Map with YEAR key (Integer)
        Arguments.of("Map containing the YEAR key with an Integer value should be extracted and converted", Collections.singletonMap(YEAR, 2031), BigDecimal.valueOf(2031)), // Map with YEAR key (String)
        Arguments.of("Map containing the YEAR key with a numeric String should be parsed and converted", Collections.singletonMap(YEAR, "2032"), BigDecimal.valueOf(2032)), // Empty map
        Arguments.of("Empty map has no YEAR entry; conversion defaults to 0", Collections.emptyMap(), BigDecimal.ZERO), // CompactLinkedMap with YEAR key
        Arguments.of("CompactLinkedMap with YEAR key should be handled like a regular map", new CompactLinkedMap<>().with(YEAR, 2033), BigDecimal.valueOf(2033)), // Negative Year
        Arguments.of("Negative Year instance should be converted preserving the sign", Year.of(-44), BigDecimal.valueOf(-44)), // String with surrounding whitespace
        Arguments.of("String with surrounding whitespace should be trimmed and parsed", " 2034 ", BigDecimal.valueOf(2034)), // String with explicit plus sign
        Arguments.of("String with explicit plus sign should be parsed as positive integer", "+2035", BigDecimal.valueOf(2035)), // Object with custom toString returning numeric string
        Arguments.of("Object whose toString returns a numeric string should be parsed and converted", new Object() {

            @Override
            public String toString() {
                return "2036";
            }
        }, BigDecimal.valueOf(2036)));
    }

    /**
     * Helper to extract the year component from a {@link Date}.
     */
    private static int getYearFromDate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        return cal.get(Calendar.YEAR);
    }

    private static List<Arguments> exceptionConversionProvider() {
        // Prepare a mock converter that throws RuntimeException for the last case.
        Converter throwingMock = mock(Converter.class);
        when(throwingMock.convert(any(), eq(Integer.class))).thenThrow(new RuntimeException("forced"));
        return List.of(// Unsupported collection type
        Arguments.of("Unsupported collection type should cause IllegalArgumentException", new ArrayList<>(), IllegalArgumentException.class, null), // Array input
        Arguments.of("Array input is not supported and should trigger IllegalArgumentException", new int[] { 2020 }, IllegalArgumentException.class, null), // Map YEAR entry with Boolean value -> NumberFormatException
        Arguments.of("Map YEAR entry with Boolean value cannot be parsed to int, leading to NumberFormatException", Collections.singletonMap(YEAR, true), NumberFormatException.class, null), // Map YEAR entry with null value -> IllegalArgumentException
        Arguments.of("Map YEAR entry with null value should cause IllegalArgumentException", Collections.singletonMap(YEAR, null), IllegalArgumentException.class, null), // Double.NaN -> IllegalArgumentException
        Arguments.of("NaN double cannot be converted to int, should throw IllegalArgumentException", Double.NaN, IllegalArgumentException.class, null), // Double.POSITIVE_INFINITY -> IllegalArgumentException
        Arguments.of("Infinite double cannot be converted to int, should throw IllegalArgumentException", Double.POSITIVE_INFINITY, IllegalArgumentException.class, null), // Object with default toString (non‑numeric) -> NumberFormatException
        Arguments.of("Object with default toString not numeric causes NumberFormatException", new Object(), NumberFormatException.class, null), // Converter that throws RuntimeException should propagate it
        Arguments.of("Converter that throws RuntimeException should propagate the exception", 2021, RuntimeException.class, throwingMock));
    }

    @Test
    void toBigDecimal_givenYear_returnsBigDecimalOfYearValue() {
        // GIVEN
        Year year = Year.of(2023);
        // YearConversions.toInt will call converter.convert(year, Integer.class)
        // We stub that call to return the numeric value of the Year.
        when(mockConverter.convert(eq(year), eq(Integer.class))).thenReturn(year.getValue());
        // WHEN
        BigDecimal result = YearConversions.toBigDecimal(year, mockConverter);
        // THEN
        assertEquals(BigDecimal.valueOf(2023), result);
        verify(mockConverter, times(1)).convert(eq(year), eq(Integer.class));
    }

    @Test
    void toBigDecimal_givenInteger_returnsBigDecimalOfSameValue() {
        // GIVEN
        Integer source = 42;
        when(mockConverter.convert(eq(source), eq(Integer.class))).thenReturn(source);
        // WHEN
        BigDecimal result = YearConversions.toBigDecimal(source, mockConverter);
        // THEN
        assertEquals(BigDecimal.valueOf(42), result);
        verify(mockConverter, times(1)).convert(eq(source), eq(Integer.class));
    }

    @Test
    void toBigDecimal_givenNull_returnsZeroBigDecimal() {
        // GIVEN
        // For a null source the typical behaviour of the conversion framework is to
        // return the default primitive value (0 for int).  We therefore stub the
        // converter to return 0 when asked to convert a null to Integer.
        when(mockConverter.convert(isNull(), eq(Integer.class))).thenReturn(0);
        // WHEN
        BigDecimal result = YearConversions.toBigDecimal(null, mockConverter);
        // THEN
        assertEquals(BigDecimal.ZERO, result);
        verify(mockConverter, times(1)).convert(isNull(), eq(Integer.class));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalConversionProvider")
    void toBigDecimal_normalCases(String description, Object from, BigDecimal expected) {
        // The real conversion logic does not need a Converter, so we pass null.
        BigDecimal result = YearConversions.toBigDecimal(from, null);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionConversionProvider")
    void toBigDecimal_exceptionCases(String description, Object from, Class<? extends Throwable> expectedException, Converter converter) {
        // The method under test should propagate the expected exception.
        assertThrows(expectedException, () -> YearConversions.toBigDecimal(from, converter), description);
    }
}
