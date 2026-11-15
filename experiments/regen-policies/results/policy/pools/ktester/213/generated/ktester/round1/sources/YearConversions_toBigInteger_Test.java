package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.math.BigInteger;
import java.time.Year;
import static org.junit.jupiter.api.Assertions.*;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import com.cedarsoftware.util.convert.conversion.ConversionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YearConversions_toBigInteger_Test {

    private static Converter converter;

    /**
     * Default, fully‑functional converter used for the majority of test cases.
     */
    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The library’s default options give us a working Converter implementation.
        ConverterOptions options = new ConverterOptions();
        defaultConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Help GC – not strictly necessary but keeps the pattern symmetrical.
        converter = null;
    }

    /**
     * Valid numeric inputs that should be converted to a matching {@link BigInteger}.
     */
    static Stream<Arguments> validNumericProvider() {
        return Stream.of(// primitive / wrapper numbers
        Arguments.of("123", BigInteger.valueOf(123)), Arguments.of(Integer.valueOf(456), BigInteger.valueOf(456)), Arguments.of(Long.valueOf(789L), BigInteger.valueOf(789)), Arguments.of(Short.valueOf((short) 12), BigInteger.valueOf(12)), Arguments.of(Byte.valueOf((byte) 5), BigInteger.valueOf(5)), // Year instances
        Arguments.of(Year.of(2023), BigInteger.valueOf(2023)), Arguments.of(Year.of(-1), BigInteger.valueOf(-1)), Arguments.of(Year.of(0), BigInteger.valueOf(0)), // Maps containing the YEAR key
        Arguments.of(mapOf(YEAR, 1999), BigInteger.valueOf(1999)), Arguments.of(mapOf(YEAR, "2001"), BigInteger.valueOf(2001)), // Atomic types
        Arguments.of(new AtomicInteger(77), BigInteger.valueOf(77)), Arguments.of(new AtomicLong(12345L), BigInteger.valueOf(12345)), // BigDecimal (fractional part is truncated)
        Arguments.of(new BigDecimal("456.78"), BigInteger.valueOf(456)), // Strings with whitespace / signs / leading zeros
        Arguments.of("   42   ", BigInteger.valueOf(42)), Arguments.of("+99", BigInteger.valueOf(99)), Arguments.of("00123", BigInteger.valueOf(123)), // Boundary values
        Arguments.of(Integer.MAX_VALUE, BigInteger.valueOf(Integer.MAX_VALUE)), Arguments.of(Integer.MIN_VALUE, BigInteger.valueOf(Integer.MIN_VALUE)), Arguments.of("0", BigInteger.ZERO), Arguments.of("-123", BigInteger.valueOf(-123)), // Null input (treated as zero by the library)
        Arguments.of(null, BigInteger.ZERO));
    }

    /**
     * Inputs that are expected to cause an exception during conversion.
     */
    static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Non‑numeric string
        Arguments.of("notANumber", NumberFormatException.class), // Long beyond int range
        Arguments.of(Long.valueOf(3_000_000_000L), NumberFormatException.class), Arguments.of(Long.valueOf(-3_000_000_000L), NumberFormatException.class), // AtomicBoolean (not numeric)
        Arguments.of(new AtomicBoolean(true), NumberFormatException.class), // BigInteger outside int range
        Arguments.of(new BigInteger("9876543210"), NumberFormatException.class), // Map with missing YEAR key
        Arguments.of(mapOf("notYear", 2022), ClassCastException.class), // Map with YEAR key but null value
        Arguments.of(mapOf(YEAR, null), NullPointerException.class), // Map with non‑numeric YEAR value
        Arguments.of(mapOf(YEAR, "NaN"), NumberFormatException.class), Arguments.of(mapOf(YEAR, "3.14"), NumberFormatException.class), Arguments.of(mapOf(YEAR, new BigDecimal("12345678901234567890")), NumberFormatException.class), // Floating‑point numbers
        Arguments.of(Float.valueOf(12.34f), NumberFormatException.class), Arguments.of(Double.valueOf(56.78), NumberFormatException.class), // Boolean / Character
        Arguments.of(Boolean.TRUE, ClassCastException.class), Arguments.of(Character.valueOf('7'), ClassCastException.class), // Malformed numeric strings
        Arguments.of("123abc", NumberFormatException.class), Arguments.of("   ", NumberFormatException.class), Arguments.of("+", NumberFormatException.class), Arguments.of("-", NumberFormatException.class), Arguments.of("1e3", NumberFormatException.class), Arguments.of("", NumberFormatException.class), // AtomicLong overflow
        Arguments.of(new AtomicLong(5_000_000_000L), NumberFormatException.class), // Null converter
        // third flag = null‑converter test
        Arguments.of("123", NullPointerException.class, true));
    }

    /**
     * Convenience factory for a map that uses the {@link MapConversions#YEAR} key.
     */
    private static Map<String, Object> mapOf(Object key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(String.valueOf(key), value);
        return map;
    }

    @Test
    void toBigInteger_fromYearInstance() {
        Year year = Year.of(2023);
        BigInteger result = YearConversions.toBigInteger(year, converter);
        assertEquals(BigInteger.valueOf(2023), result, "Conversion from Year should produce its numeric value as BigInteger");
    }

    @Test
    void toBigInteger_fromString() {
        String source = "1999";
        BigInteger result = YearConversions.toBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(1999), result, "Conversion from numeric String should produce the corresponding BigInteger");
    }

    @Test
    void toBigInteger_fromNumber() {
        Long source = 123L;
        BigInteger result = YearConversions.toBigInteger(source, converter);
        assertEquals(BigInteger.valueOf(123), result, "Conversion from Number should produce the same numeric value as BigInteger");
    }

    @Test
    void toBigInteger_fromMapWithYearKey() {
        Map<String, Object> map = new HashMap<>();
        // MapConversions.YEAR is the expected key
        map.put(YEAR, 2005);
        BigInteger result = YearConversions.toBigInteger(map, converter);
        assertEquals(BigInteger.valueOf(2005), result, "Conversion from Map containing YEAR key should yield that year's value");
    }

    @Test
    void toBigInteger_fromNull() {
        BigInteger result = YearConversions.toBigInteger(null, converter);
        assertEquals(BigInteger.ZERO, result, "Null input should be treated as 0 for numeric conversions");
    }

    @ParameterizedTest(name = "[{index}] toBigInteger({0}) → {1}")
    @MethodSource("validNumericProvider")
    void testToBigInteger_ValidInputs(Object from, BigInteger expected) {
        assertEquals(expected, YearConversions.toBigInteger(from, defaultConverter), () -> "Expected conversion of " + from + " to be " + expected);
    }

    @ParameterizedTest(name = "[{index}] toBigInteger({0}) throws {1}")
    @MethodSource("exceptionProvider")
    void testToBigInteger_Exceptions(Object from, Class<? extends Throwable> expectedException) {
        // Normal case – use the default converter
        assertThrows(expectedException, () -> YearConversions.toBigInteger(from, defaultConverter), () -> "Conversion of " + from + " should throw " + expectedException.getSimpleName());
    }

    /**
     * Overloaded version that also handles the “null converter” scenario.
     */
    @ParameterizedTest(name = "[{index}] toBigInteger({0}) with null converter throws {1}")
    @MethodSource("exceptionProvider")
    void testToBigInteger_Exceptions_WithNullConverter(Object from, Class<? extends Throwable> expectedException, boolean nullConverterFlag) {
        // This overload is only invoked for the special case where the third argument is true.
        if (!nullConverterFlag) {
            // Let the other method handle it.
            return;
        }
        assertThrows(expectedException, () -> YearConversions.toBigInteger(from, null), () -> "Calling with a null Converter should throw " + expectedException.getSimpleName());
    }

    @Test
    void testToBigInteger_CustomConverterReturning42() {
        // Mock a Converter that pretends to convert any input to the int value 42.
        Converter custom = mock(Converter.class);
        when(custom.convert(any(), eq(Integer.class))).thenReturn(42);
        BigInteger result = YearConversions.toBigInteger(new Object(), custom);
        assertEquals(BigInteger.valueOf(42), result, "Custom converter that returns 42 should yield BigInteger 42");
    }

    @Test
    void testToBigInteger_CustomConverterThrowingConversionException() {
        Converter throwing = mock(Converter.class);
        when(throwing.convert(any(), eq(Integer.class))).thenThrow(new ConversionException("forced failure"));
        assertThrows(ConversionException.class, () -> YearConversions.toBigInteger("anyValue", throwing), "A Converter that throws ConversionException should have it propagated");
    }

    @Test
    void testToBigInteger_CustomConverterReturningNull() {
        Converter nullReturning = mock(Converter.class);
        when(nullReturning.convert(any(), eq(Integer.class))).thenReturn(null);
        assertThrows(NullPointerException.class, () -> YearConversions.toBigInteger("any", nullReturning), "A Converter that returns null should cause a NullPointerException");
    }

    @Test
    void testToBigInteger_CustomConverterReturningWrongType() {
        Converter wrongType = mock(Converter.class);
        when(wrongType.convert(any(), eq(Integer.class))).thenReturn("not an integer");
        assertThrows(ClassCastException.class, () -> YearConversions.toBigInteger("any", wrongType), "A Converter that returns a wrong type should cause a ClassCastException");
    }
}
