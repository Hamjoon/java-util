package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigInteger;
import java.time.Year;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class YearConversions_toBigInteger_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared real {@link Converter} instance – used for the happy‑path cases.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter is inexpensive to create and does not require any external resources.
        // We use the default options (an empty {@link ConverterOptions}) so that the built‑in
        // conversions are available.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /**
     * Provides a stream of valid inputs together with the expected {@link BigInteger}
     * result.  The {@code converter} argument is always the real converter instance,
     * because the conversion logic lives inside {@code YearConversions.toInt(...)}.
     */
    private static Stream<Arguments> validInputs() {
        return Stream.of(// primitive wrappers
        Arguments.of(123, BigInteger.valueOf(123), "Integer literal"), Arguments.of("456", BigInteger.valueOf(456), "String numeric"), Arguments.of(Year.of(2020), BigInteger.valueOf(2020), "Year instance"), // BigDecimal truncates the fractional part
        Arguments.of(new BigDecimal("789.01"), BigInteger.valueOf(789), "BigDecimal with fraction"), // Atomic types
        Arguments.of(new AtomicInteger(55), BigInteger.valueOf(55), "AtomicInteger"), Arguments.of(new AtomicLong(999L), BigInteger.valueOf(999), "AtomicLong"), Arguments.of(new AtomicBoolean(true), BigInteger.valueOf(1), "AtomicBoolean true"), Arguments.of(new AtomicBoolean(false), BigInteger.valueOf(0), "AtomicBoolean false"), // Map containing the YEAR key (String key defined in MapConversions.YEAR)
        Arguments.of(createMap("YEAR", 2022), BigInteger.valueOf(2022), "Map with YEAR int"), Arguments.of(createMap("YEAR", "1999"), BigInteger.valueOf(1999), "Map with YEAR numeric string"), // Negative number
        Arguments.of(-123, BigInteger.valueOf(-123), "Negative integer"), // Long larger than Integer.MAX_VALUE – will be cast/truncated
        Arguments.of(Long.MAX_VALUE, BigInteger.valueOf(Integer.MAX_VALUE), "Long.MAX_VALUE overflow"));
    }

    /**
     * Creates a {@link CompactLinkedMap} with a single entry.
     */
    private static CompactLinkedMap<String, Object> createMap(String key, Object value) {
        CompactLinkedMap<String, Object> map = new CompactLinkedMap<>();
        map.put(key, value);
        return map;
    }

    @Test
    void toBigInteger_fromYear_returnsYearValue() {
        // given
        Year year = Year.of(2023);
        // when
        BigInteger result = YearConversions.toBigInteger(year, converter);
        // then
        assertEquals(BigInteger.valueOf(2023), result, "BigInteger value should match the numeric year");
    }

    @Test
    void toBigInteger_fromInteger_returnsSameValue() {
        // given
        Integer source = 42;
        // when
        BigInteger result = YearConversions.toBigInteger(source, converter);
        // then
        assertEquals(BigInteger.valueOf(42), result);
    }

    @Test
    void toBigInteger_fromString_parsesNumber() {
        // given
        String source = "12345";
        // when
        BigInteger result = YearConversions.toBigInteger(source, converter);
        // then
        assertEquals(BigInteger.valueOf(12345), result);
    }

    @Test
    void toBigInteger_fromNull_returnsZero() {
        // given
        Object source = null;
        // when
        BigInteger result = YearConversions.toBigInteger(source, converter);
        // then
        assertEquals(BigInteger.ZERO, result, "Null input should be converted to zero for primitive numeric targets");
    }

    @Test
    void toBigInteger_fromLong_returnsLongValue() {
        // given
        Long source = 9_876_543_210L;
        // when
        BigInteger result = YearConversions.toBigInteger(source, converter);
        // then
        assertEquals(BigInteger.valueOf(source), result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validInputs")
    void toBigInteger_validInputs(Object from, BigInteger expected, String description) {
        // when
        BigInteger result = YearConversions.toBigInteger(from, realConverter);
        // then
        assertEquals(expected, result, description);
    }

    @Test
    void toBigInteger_nullFromAndNullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> YearConversions.toBigInteger(null, null), "Both parameters null should cause NPE when toInt is invoked");
    }

    @Test
    void toBigInteger_nullFrom_nonNullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> YearConversions.toBigInteger(null, realConverter), "Null `from` with a non‑null converter should cause NPE");
    }

    @Test
    void toBigInteger_nonNumericString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> YearConversions.toBigInteger("abc", realConverter));
    }

    @Test
    void toBigInteger_emptyString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> YearConversions.toBigInteger("", realConverter));
    }

    @Test
    void toBigInteger_whitespaceString_throwsNumberFormatException() {
        assertThrows(NumberFormatException.class, () -> YearConversions.toBigInteger("   42  ", realConverter));
    }

    @Test
    void toBigInteger_mapMissingYearKey_throwsIllegalArgumentException() {
        // lower‑case key
        Map<String, Object> map = createMap("year", 2000);
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toBigInteger(map, realConverter));
    }

    @Test
    void toBigInteger_dateObject_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toBigInteger(new java.util.Date(), realConverter));
    }

    @Test
    void toBigInteger_localDateObject_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toBigInteger(java.time.LocalDate.of(2020, 1, 1), realConverter));
    }

    @Test
    void toBigInteger_objectToStringReturnsNull_throwsNullPointerException() {
        Object obj = new Object() {

            @Override
            public String toString() {
                return null;
            }
        };
        assertThrows(NullPointerException.class, () -> YearConversions.toBigInteger(obj, realConverter));
    }

    @Test
    void toBigInteger_mapYearKeyNullValue_throwsNullPointerException() {
        Map<String, Object> map = createMap("YEAR", null);
        assertThrows(NullPointerException.class, () -> YearConversions.toBigInteger(map, realConverter));
    }

    @Test
    void toBigInteger_bigIntegerOutOfRange_throwsArithmeticException() {
        // The implementation of toInt casts to int; overflow may surface as
        // ArithmeticException depending on internal logic. We assert that *some*
        // exception is thrown and that it is an ArithmeticException.
        assertThrows(ArithmeticException.class, () -> YearConversions.toBigInteger(new java.math.BigInteger("12345678901234567890"), realConverter));
    }

    @Test
    void toBigInteger_unsupportedCollection_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toBigInteger(new java.util.ArrayList<>(), realConverter));
    }

    @Test
    void toBigInteger_mapYearKeyWithUnsupportedObject_throwsIllegalArgumentException() {
        Map<String, Object> map = createMap("YEAR", new Object());
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toBigInteger(map, realConverter));
    }

    @Test
    void toBigInteger_arbitraryObjectWithParsableToString_works() {
        Object obj = new Object() {

            @Override
            public String toString() {
                return "2025";
            }
        };
        BigInteger result = YearConversions.toBigInteger(obj, realConverter);
        assertEquals(BigInteger.valueOf(2025), result);
    }
}
