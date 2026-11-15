package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Year;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;

@ExtendWith(MockitoExtension.class)
class YearConversions_toAtomicInteger_Test {

    /**
     * The Converter used for all tests.
     */
    private static Converter converter;

    /**
     * Mocked ConverterOptions required by the Converter constructor.
     */
    @Mock
    private static ConverterOptions mockOptions;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter constructor expects a ConverterOptions instance.
        // We mock it and make it return an empty map for overrides.
        when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        // Create the real Converter – we are not mocking conversion logic itself.
        converter = new Converter(mockOptions);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static references.
        converter = null;
    }

    @Test
    void toAtomicInteger_givenYear_returnsAtomicIntegerWithYearValue() {
        // GIVEN
        Year year = Year.of(2023);
        // WHEN
        AtomicInteger result = YearConversions.toAtomicInteger(year, converter);
        // THEN
        assertNotNull(result, "Result should not be null");
        assertEquals(2023, result.get(), "AtomicInteger should contain the year value");
    }

    @Test
    void toAtomicInteger_givenInteger_returnsSameValue() {
        // GIVEN
        Integer source = 42;
        // WHEN
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(42, result.get());
    }

    @Test
    void toAtomicInteger_givenStringNumber_returnsParsedValue() {
        // GIVEN
        String source = "7";
        // WHEN
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(7, result.get());
    }

    @Test
    void toAtomicInteger_givenNull_returnsZeroAtomicInteger() {
        // GIVEN
        Object source = null;
        // WHEN
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(0, result.get(), "Null source should be converted to zero for primitive int");
    }

    @Test
    void toAtomicInteger_givenMapWithValueKey_returnsMappedInteger() {
        // GIVEN
        Map<String, Object> source = Collections.singletonMap("value", 123);
        // WHEN
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        // THEN
        assertNotNull(result);
        assertEquals(123, result.get());
    }

    // ---------- basic conversions ----------
    @Test
    void basic_numericString_isConverted() {
        AtomicInteger result = YearConversions.toAtomicInteger("123", converter);
        assertEquals(123, result.get());
    }

    @Test
    void basic_YearInstance_isConverted() {
        AtomicInteger result = YearConversions.toAtomicInteger(Year.of(2025), converter);
        assertEquals(2025, result.get());
    }

    @Test
    void basic_AtomicInteger_isCopied() {
        AtomicInteger src = new AtomicInteger(42);
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        assertEquals(42, result.get());
        // ensure a new instance is created
        assertNotSame(src, result);
    }

    @Test
    void basic_nullYieldsZero() {
        AtomicInteger result = YearConversions.toAtomicInteger(null, converter);
        assertEquals(0, result.get());
    }

    @Test
    void basic_booleanTrueYieldsOne() {
        AtomicInteger result = YearConversions.toAtomicInteger(true, converter);
        assertEquals(1, result.get());
    }

    @Test
    void basic_booleanFalseYieldsZero() {
        AtomicInteger result = YearConversions.toAtomicInteger(false, converter);
        assertEquals(0, result.get());
    }

    @Test
    void basic_BigDecimal_isTruncated() {
        AtomicInteger result = YearConversions.toAtomicInteger(new BigDecimal("987.65"), converter);
        assertEquals(987, result.get());
    }

    @Test
    void basic_BigInteger_usesIntValue() {
        BigInteger big = new BigInteger("12345678901234567890");
        AtomicInteger result = YearConversions.toAtomicInteger(big, converter);
        // intValue() keeps the low‑order 32 bits
        assertEquals(big.intValue(), result.get());
    }

    @Test
    void basic_customObjectToString_isParsed() {
        Object src = new Object() {

            @Override
            public String toString() {
                return "77";
            }
        };
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        assertEquals(77, result.get());
    }

    @Test
    void basic_invalidString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicInteger("not a number", converter));
    }

    // ---------- numeric primitive wrappers ----------
    @Test
    void wrapper_Integer_isConverted() {
        AtomicInteger result = YearConversions.toAtomicInteger(Integer.valueOf(56), converter);
        assertEquals(56, result.get());
    }

    @Test
    void wrapper_Long_truncatesToLow32Bits() {
        long val = 1234567890123L;
        AtomicInteger result = YearConversions.toAtomicInteger(Long.valueOf(val), converter);
        assertEquals((int) val, result.get());
    }

    @Test
    void wrapper_Short_isPreserved() {
        AtomicInteger result = YearConversions.toAtomicInteger(Short.valueOf((short) -300), converter);
        assertEquals(-300, result.get());
    }

    @Test
    void wrapper_Byte_isPreserved() {
        AtomicInteger result = YearConversions.toAtomicInteger(Byte.valueOf((byte) 127), converter);
        assertEquals(127, result.get());
    }

    @Test
    void wrapper_Double_isTruncatedTowardZero() {
        AtomicInteger result = YearConversions.toAtomicInteger(Double.valueOf(45.99), converter);
        assertEquals(45, result.get());
    }

    @Test
    void wrapper_Float_isTruncatedTowardZero() {
        AtomicInteger result = YearConversions.toAtomicInteger(Float.valueOf(-12.34f), converter);
        assertEquals(-12, result.get());
    }

    // ---------- atomic types ----------
    @Test
    void atomic_Long_isTruncated() {
        AtomicLong src = new AtomicLong(9876543210L);
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        assertEquals((int) 9876543210L, result.get());
    }

    @Test
    void atomic_BooleanTrue_isOne() {
        AtomicBoolean src = new AtomicBoolean(true);
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        assertEquals(1, result.get());
    }

    @Test
    void atomic_BooleanFalse_isZero() {
        AtomicBoolean src = new AtomicBoolean(false);
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        assertEquals(0, result.get());
    }

    // ---------- map with YEAR key ----------
    @Test
    void map_withYearKey_numericValue() {
        CompactLinkedMap<Object, Object> map = new CompactLinkedMap<>().withEntry(YEAR, 1999);
        AtomicInteger result = YearConversions.toAtomicInteger(map, converter);
        assertEquals(1999, result.get());
    }

    @Test
    void map_withYearKey_stringValue() {
        CompactLinkedMap<Object, Object> map = new CompactLinkedMap<>().withEntry(YEAR, "2005");
        AtomicInteger result = YearConversions.toAtomicInteger(map, converter);
        assertEquals(2005, result.get());
    }

    // ---------- string edge cases ----------
    @Test
    void string_negativeNumber() {
        AtomicInteger result = YearConversions.toAtomicInteger("-15", converter);
        assertEquals(-15, result.get());
    }

    @Test
    void string_positiveWithPlusSign() {
        AtomicInteger result = YearConversions.toAtomicInteger("+10", converter);
        assertEquals(10, result.get());
    }

    @Test
    void string_leadingZeros() {
        AtomicInteger result = YearConversions.toAtomicInteger("00123", converter);
        assertEquals(123, result.get());
    }

    @Test
    void string_empty_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicInteger("", converter));
    }

    @Test
    void string_whitespaceOnly_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicInteger(" 42 ", converter));
    }

    // ---------- special floating‑point values ----------
    @Test
    void double_NaN_convertsToZero() {
        AtomicInteger result = YearConversions.toAtomicInteger(Double.NaN, converter);
        assertEquals(0, result.get());
    }

    @Test
    void double_PositiveInfinity_convertsToMaxInt() {
        AtomicInteger result = YearConversions.toAtomicInteger(Double.POSITIVE_INFINITY, converter);
        assertEquals(Integer.MAX_VALUE, result.get());
    }

    @Test
    void double_NegativeInfinity_convertsToMinInt() {
        AtomicInteger result = YearConversions.toAtomicInteger(Double.NEGATIVE_INFINITY, converter);
        assertEquals(Integer.MIN_VALUE, result.get());
    }

    // ---------- custom Converter usage ----------
    @Test
    void customConverter_returnsFixedValue() {
        // a mock Converter that always returns 777 irrespective of input
        Converter custom = mock(Converter.class);
        when(custom.convert(any())).thenReturn(777);
        AtomicInteger result = YearConversions.toAtomicInteger("custom", custom);
        assertEquals(777, result.get());
    }

    @Test
    void customConverter_doublesInteger() {
        Converter custom = mock(Converter.class);
        when(custom.convert(any())).thenAnswer(invocation -> {
            Object arg = invocation.getArgument(0);
            return ((Integer) arg) * 2;
        });
        AtomicInteger result = YearConversions.toAtomicInteger(Integer.valueOf(5), custom);
        assertEquals(10, result.get());
    }

    // ---------- overflow numeric string ----------
    @Test
    void overflow_stringAboveMax_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicInteger("2147483648", converter));
    }

    @Test
    void overflow_stringBelowMin_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicInteger("-2147483649", converter));
    }

    // ---------- map missing YEAR key ----------
    @Test
    void map_missingYearKey_throwsIllegalArgumentException() {
        CompactLinkedMap<Object, Object> map = new CompactLinkedMap<>().withEntry("someOtherKey", 2020);
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toAtomicInteger(map, converter));
    }

    // ---------- map YEAR value non‑numeric ----------
    @Test
    void map_yearKeyNonNumeric_throwsNumberFormatException() {
        CompactLinkedMap<Object, Object> map = new CompactLinkedMap<>().withEntry(YEAR, "notANumber");
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicInteger(map, converter));
    }

    // ---------- custom converter throws exception ----------
    @Test
    void customConverterThrows_propagatesException() {
        Converter custom = mock(Converter.class);
        when(custom.convert(any())).thenThrow(new RuntimeException("converter failure"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> YearConversions.toAtomicInteger("anyValue", custom));
        assertEquals("converter failure", ex.getMessage());
    }

    // ---------- object toString throws exception ----------
    @Test
    void objectToStringThrows_propagatesException() {
        Object bad = new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("bad toString");
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> YearConversions.toAtomicInteger(bad, converter));
        assertEquals("bad toString", ex.getMessage());
    }

    // ---------- object toString non‑numeric ----------
    @Test
    void objectToStringNonNumeric_throwsNumberFormatException() {
        Object src = new Object() {

            @Override
            public String toString() {
                return "abcXYZ";
            }
        };
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicInteger(src, converter));
    }

    // ---------- hex string invalid ----------
    @Test
    void hexString_invalid_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicInteger("0x10", converter));
    }
}
