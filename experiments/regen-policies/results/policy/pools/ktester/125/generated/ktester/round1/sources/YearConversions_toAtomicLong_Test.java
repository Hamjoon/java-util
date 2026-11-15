package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

class YearConversions_toAtomicLong_Test {

    private static ConverterOptions options;

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – they provide all built‑in conversions
        options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(// Year instance
        Arguments.of("Year.of(2023) → AtomicLong(2023)", Year.of(2023), new NoOpConverter(), 2023L), // Integer
        Arguments.of("Integer.valueOf(42) → AtomicLong(42)", Integer.valueOf(42), new NoOpConverter(), 42L), // Long (truncated to int)
        Arguments.of("Long.valueOf(123456789L) → AtomicLong(123456789)", Long.valueOf(123456789L), new NoOpConverter(), 123456789L), // Numeric String
        Arguments.of("\"9876\" → AtomicLong(9876)", "9876", new NoOpConverter(), 9876L), // Non‑numeric String → NumberFormatException
        Arguments.of("\"notANumber\" → NumberFormatException", "notANumber", new NoOpConverter(), NumberFormatException.class), // Map with YEAR key (int value)
        Arguments.of("Map with YEAR→2000 → AtomicLong(2000)", Collections.singletonMap(MapConversions.YEAR, 2000), new NoOpConverter(), 2000L), // Null source → NullPointerException (as per JSON spec)
        Arguments.of("null source → NullPointerException", null, new NoOpConverter(), NullPointerException.class), // Boolean source → IllegalArgumentException
        Arguments.of("Boolean.TRUE → IllegalArgumentException", Boolean.TRUE, new NoOpConverter(), IllegalArgumentException.class));
    }

    private static Stream<Arguments> additionalConversionProvider() {
        return Stream.of(Arguments.of("Short.valueOf((short)7) → AtomicLong(7)", Short.valueOf((short) 7), new NoOpConverter(), 7L), Arguments.of("Byte.valueOf((byte)-3) → AtomicLong(-3)", Byte.valueOf((byte) -3), new NoOpConverter(), -3L), Arguments.of("Float.valueOf(12.34f) → AtomicLong(12)", Float.valueOf(12.34f), new NoOpConverter(), 12L), Arguments.of("Double.valueOf(56.78) → AtomicLong(56)", Double.valueOf(56.78), new NoOpConverter(), 56L), Arguments.of("AtomicInteger(55) → AtomicLong(55)", new AtomicInteger(55), new NoOpConverter(), 55L), Arguments.of("AtomicLong(9999999999L) → overflow → AtomicLong(1410065407)", new AtomicLong(9_999_999_999L), new NoOpConverter(), 1_410_065_407L), Arguments.of("BigInteger(\"2147483648\") → overflow → AtomicLong(-2147483648)", new BigInteger("2147483648"), new NoOpConverter(), -2_147_483_648L), Arguments.of("BigDecimal(\"12345.67\") → AtomicLong(12345)", new BigDecimal("12345.67"), new NoOpConverter(), 12_345L), Arguments.of("Map YEAR→\"2022\" (String) → AtomicLong(2022)", Collections.singletonMap(MapConversions.YEAR, "2022"), new NoOpConverter(), 2_022L), Arguments.of("Map without YEAR key → IllegalArgumentException", Collections.singletonMap("someOtherKey", 1999), new NoOpConverter(), IllegalArgumentException.class), Arguments.of("Empty map → IllegalArgumentException", Collections.emptyMap(), new NoOpConverter(), IllegalArgumentException.class), Arguments.of("String with whitespace \"   42   \" → AtomicLong(42)", "   42   ", new NoOpConverter(), 42L), Arguments.of("Empty string \"\" → NumberFormatException", "", new NoOpConverter(), NumberFormatException.class), Arguments.of("FixedValueConverter(777) overrides source → AtomicLong(777)", "anyValue", new FixedValueConverter(777), 777L));
    }

    private static Stream<Arguments> exceptionalConversionProvider() {
        return Stream.of(Arguments.of("Null Converter → NullPointerException", Year.of(2020), null, NullPointerException.class), Arguments.of("Map YEAR→null → NullPointerException", Collections.singletonMap(MapConversions.YEAR, null), new NoOpConverter(), NullPointerException.class), Arguments.of("Map YEAR→Boolean.TRUE → IllegalArgumentException", Collections.singletonMap(MapConversions.YEAR, Boolean.TRUE), new NoOpConverter(), IllegalArgumentException.class), Arguments.of("Array int[]{1,2,3} → IllegalArgumentException", new int[] { 1, 2, 3 }, new NoOpConverter(), IllegalArgumentException.class), Arguments.of("Character.valueOf('A') → IllegalArgumentException", Character.valueOf('A'), new NoOpConverter(), IllegalArgumentException.class), Arguments.of("ThrowingConverter propagates IllegalStateException", Integer.valueOf(5), new ThrowingConverter(), IllegalStateException.class), Arguments.of("String only whitespace \"   \" → NumberFormatException", "   ", new NoOpConverter(), NumberFormatException.class));
    }

    @Test
    void toAtomicLong_fromYear_returnsCorrectValue() {
        // given
        Year year = Year.of(2023);
        // when
        AtomicLong result = YearConversions.toAtomicLong(year, converter);
        // then
        assertNotNull(result);
        assertEquals(2023L, result.get());
    }

    @Test
    void toAtomicLong_fromStringNumber_returnsCorrectValue() {
        // given
        String number = "12345";
        // when
        AtomicLong result = YearConversions.toAtomicLong(number, converter);
        // then
        assertNotNull(result);
        assertEquals(12345L, result.get());
    }

    @Test
    void toAtomicLong_fromInteger_returnsCorrectValue() {
        // given
        Integer intValue = 42;
        // when
        AtomicLong result = YearConversions.toAtomicLong(intValue, converter);
        // then
        assertNotNull(result);
        assertEquals(42L, result.get());
    }

    @Test
    void toAtomicLong_fromNull_returnsZeroAtomicLong() {
        // given
        Object nullObj = null;
        // when
        AtomicLong result = YearConversions.toAtomicLong(nullObj, converter);
        // then
        assertNotNull(result);
        // Converter returns 0 for numeric primitives when source is null
        assertEquals(0L, result.get());
    }

    @Test
    void toAtomicLong_fromUnsupportedType_throwsException() {
        // given
        Object unsupported = new Object();
        // when / then
        assertThrows(ConversionException.class, () -> YearConversions.toAtomicLong(unsupported, converter));
    }

    /* -----------------------------------------------------------
     *  Parameterized tests for the "normal" conversion group
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalConversionProvider")
    void toAtomicLong_normalConversions(String description, Object from, Converter conv, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception
            assertThrows((Class<? extends Throwable>) expected, () -> YearConversions.toAtomicLong(from, conv), description);
        } else {
            // Expected a successful conversion
            AtomicLong result = YearConversions.toAtomicLong(from, conv);
            assertNotNull(result, description);
            assertEquals(((Number) expected).longValue(), result.get(), description);
        }
    }

    /* -----------------------------------------------------------
     *  Parameterized tests for the "additional" conversion group
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalConversionProvider")
    void toAtomicLong_additionalConversions(String description, Object from, Converter conv, Object expected) {
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            assertThrows((Class<? extends Throwable>) expected, () -> YearConversions.toAtomicLong(from, conv), description);
        } else {
            AtomicLong result = YearConversions.toAtomicLong(from, conv);
            assertNotNull(result, description);
            assertEquals(((Number) expected).longValue(), result.get(), description);
        }
    }

    /* -----------------------------------------------------------
     *  Parameterized tests for the "exceptional" conversion group
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("exceptionalConversionProvider")
    void toAtomicLong_exceptionalCases(String description, Object from, Converter conv, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> YearConversions.toAtomicLong(from, conv), description);
    }
}
