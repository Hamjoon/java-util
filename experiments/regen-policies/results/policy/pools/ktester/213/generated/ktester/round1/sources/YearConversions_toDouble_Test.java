package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YearConversions_toDouble_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Shared {@link Converter} instance used for the majority of tests (null‑converter cases use {@code null}).
     */
    private static Converter defaultConverter;

    /* --------------------------------------------------------------
     *  Helper holder class for parameterised tests
     * -------------------------------------------------------------- */
    private static final class ArgumentsHolder {

        final Object from;

        // may be null
        final Converter converter;

        final double expected;

        ArgumentsHolder(Object from, Converter converter, double expected) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return String.valueOf(from);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a Converter with default options – no special configuration required.
        ConverterOptions options = new ConverterOptions();
        defaultConverter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        converter = null;
    }

    /* --------------------------------------------------------------
     *  Numeric primitive wrappers
     * -------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> numericWrapperProvider() {
        return Stream.of(new ArgumentsHolder(Integer.valueOf(42), null, 42.0), new ArgumentsHolder(Long.valueOf(123456789L), null, 123456789.0), new ArgumentsHolder(Short.valueOf((short) 7), null, 7.0), new ArgumentsHolder(Byte.valueOf((byte) -3), null, -3.0));
    }

    /* --------------------------------------------------------------
     *  String representations of numbers
     * -------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> stringNumberProvider() {
        return Stream.of(new ArgumentsHolder("256", null, 256.0), new ArgumentsHolder("   -42  ", null, -42.0), new ArgumentsHolder("0x10", null, 16.0), new ArgumentsHolder("+77", null, 77.0), new ArgumentsHolder("00042", null, 42.0), new ArgumentsHolder("  +0x2A  ", null, 42.0));
    }

    /* --------------------------------------------------------------
     *  Boundary numeric values
     * -------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> boundaryProvider() {
        return Stream.of(new ArgumentsHolder(Integer.MAX_VALUE, null, (double) Integer.MAX_VALUE), new ArgumentsHolder(Integer.MIN_VALUE, null, (double) Integer.MIN_VALUE));
    }

    /* --------------------------------------------------------------
     *  Atomic types
     * -------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> atomicProvider() {
        return Stream.of(new ArgumentsHolder(new AtomicInteger(55), null, 55.0), new ArgumentsHolder(new AtomicLong(123456789L), null, 123456789.0));
    }

    @Test
    void toDouble_fromYear_returnsYearValueAsDouble() {
        // GIVEN
        Year year = Year.of(1999);
        // WHEN
        double result = YearConversions.toDouble(year, converter);
        // THEN
        assertEquals(1999.0, result, "Year should be converted to its numeric value as double");
    }

    @Test
    void toDouble_fromMapWithYearKey_returnsYearValueAsDouble() {
        // GIVEN
        Map<String, Object> map = new LinkedHashMap<>();
        // MapConversions.YEAR is "year"
        map.put("year", 2005);
        // WHEN
        double result = YearConversions.toDouble(map, converter);
        // THEN
        assertEquals(2005.0, result, "Map containing a 'year' entry should be converted to its numeric value");
    }

    @Test
    void toDouble_fromInteger_returnsSameValueAsDouble() {
        // GIVEN
        Integer number = 42;
        // WHEN
        double result = YearConversions.toDouble(number, converter);
        // THEN
        assertEquals(42.0, result, "Integer should be converted to the same numeric value as double");
    }

    @Test
    void toDouble_fromNull_returnsZero() {
        // GIVEN
        Object nullObj = null;
        // WHEN
        double result = YearConversions.toDouble(nullObj, converter);
        // THEN
        assertEquals(0.0, result, "Null input should result in 0.0 according to Converter's null handling");
    }

    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("numericWrapperProvider")
    void toDouble_numericWrappers(ArgumentsHolder holder) {
        double result = YearConversions.toDouble(holder.from, holder.converter);
        assertEquals(holder.expected, result, "Wrapper numeric conversion failed");
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" → {2}")
    @MethodSource("stringNumberProvider")
    void toDouble_stringNumbers(ArgumentsHolder holder) {
        double result = YearConversions.toDouble(holder.from, holder.converter);
        assertEquals(holder.expected, result, "String number conversion failed");
    }

    @Test
    void toDouble_stringNonNumeric_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble("notANumber", null), "Non‑numeric string must cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  java.time.Year handling
     * -------------------------------------------------------------- */
    @Test
    void toDouble_yearInstance() {
        double result = YearConversions.toDouble(Year.of(2023), null);
        assertEquals(2023.0, result, "Year instance should be converted to its int value");
    }

    @Test
    void toDouble_negativeYear() {
        double result = YearConversions.toDouble(Year.of(-500), null);
        assertEquals(-500.0, result, "Negative Year should be converted correctly");
    }

    /* --------------------------------------------------------------
     *  Null and unsupported types
     * -------------------------------------------------------------- */
    @Test
    void toDouble_nullSource_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> YearConversions.toDouble(null, null), "Passing null as the source object should result in NullPointerException");
    }

    @Test
    void toDouble_unsupportedObject_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(new Object(), null), "Unsupported object type should trigger IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  Custom Converter usage
     * -------------------------------------------------------------- */
    @Test
    void toDouble_customConverterReturnsNumber(@Mock Converter mockConverter) {
        // Mock behaviour: when the converter is asked to convert the string "custom"
        // it returns the Integer 99.
        when(mockConverter.convert(eq("custom"), any())).thenReturn(99);
        double result = YearConversions.toDouble("custom", mockConverter);
        assertEquals(99.0, result, "Mock converter should supply 99 which is then returned as double");
    }

    @Test
    void toDouble_converterReturnsDoubleTruncated(@Mock Converter mockConverter) {
        // Converter returns a Double; toInt should truncate the fractional part.
        when(mockConverter.convert(eq("123"), any())).thenReturn(45.6);
        double result = YearConversions.toDouble("123", mockConverter);
        assertEquals(45.0, result, "Double returned by converter should be truncated to int before widening");
    }

    @Test
    void toDouble_converterReturnsNull_throwsIllegalArgumentException(@Mock Converter mockConverter) {
        when(mockConverter.convert(eq("unknown"), any())).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble("unknown", mockConverter), "Converter returning null should cause IllegalArgumentException");
    }

    @Test
    void toDouble_converterThrowsRuntimeException(@Mock Converter mockConverter) {
        when(mockConverter.convert(eq("notNumber"), any())).thenThrow(new RuntimeException("conversion failed"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> YearConversions.toDouble("notNumber", mockConverter), "RuntimeException from converter must propagate");
        assertEquals("conversion failed", ex.getMessage());
    }

    @Test
    void toDouble_converterReturnsOutOfRangeNumber(@Mock Converter mockConverter) {
        when(mockConverter.convert(eq("key"), any())).thenReturn(Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble("key", mockConverter), "Number outside int range supplied by converter must cause IllegalArgumentException");
    }

    @Test
    void toDouble_converterReturnsNonNumber(@Mock Converter mockConverter) {
        when(mockConverter.convert(eq("key2"), any())).thenReturn(Boolean.TRUE);
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble("key2", mockConverter), "Non‑Number object returned by converter must cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("boundaryProvider")
    void toDouble_boundaryIntegers(ArgumentsHolder holder) {
        double result = YearConversions.toDouble(holder.from, holder.converter);
        assertEquals(holder.expected, result);
    }

    @Test
    void toDouble_longBeyondIntRange_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(Long.MAX_VALUE, null), "Long value exceeding int range must cause IllegalArgumentException");
    }

    @ParameterizedTest(name = "[{index}] {0} → {2}")
    @MethodSource("atomicProvider")
    void toDouble_atomicNumeric(ArgumentsHolder holder) {
        double result = YearConversions.toDouble(holder.from, holder.converter);
        assertEquals(holder.expected, result);
    }

    @Test
    void toDouble_atomicBoolean_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(new AtomicBoolean(true), null), "AtomicBoolean is not a numeric type and must trigger IllegalArgumentException");
    }

    @Test
    void toDouble_atomicLongOverflow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(new AtomicLong(Long.MAX_VALUE), null), "AtomicLong value exceeds int range – should throw IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  Map handling (including YEAR key edge cases)
     * -------------------------------------------------------------- */
    @Test
    void toDouble_mapWithYearKeyInteger() {
        Map<String, Object> map = Collections.singletonMap(YEAR, Integer.valueOf(1999));
        double result = YearConversions.toDouble(map, null);
        assertEquals(1999.0, result);
    }

    @Test
    void toDouble_mapWithYearKeyString() {
        Map<String, Object> map = Collections.singletonMap(YEAR, "2020");
        double result = YearConversions.toDouble(map, null);
        assertEquals(2020.0, result);
    }

    @Test
    void toDouble_mapMissingYearKey_throwsIllegalArgumentException() {
        Map<String, Object> map = Collections.singletonMap("otherKey", Integer.valueOf(1));
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(map, null), "Map lacking the YEAR key must cause IllegalArgumentException");
    }

    @Test
    void toDouble_mapYearKeyNullValue_throwsIllegalArgumentException() {
        Map<String, Object> map = Collections.singletonMap(YEAR, null);
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(map, null), "Map with YEAR key but null value must fail");
    }

    @Test
    void toDouble_mapYearKeyNonNumericString_throwsIllegalArgumentException() {
        Map<String, Object> map = Collections.singletonMap(YEAR, "abc");
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(map, null), "Non‑numeric string for YEAR key must fail");
    }

    @Test
    void toDouble_mapYearKeyLongOutOfRange_throwsIllegalArgumentException() {
        Map<String, Object> map = Collections.singletonMap(YEAR, Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(map, null), "Numeric value exceeding int range for YEAR key must fail");
    }

    /* --------------------------------------------------------------
     *  Non‑numeric Number types (BigDecimal, BigInteger)
     * -------------------------------------------------------------- */
    @Test
    void toDouble_bigDecimalTruncates() {
        double result = YearConversions.toDouble(new BigDecimal("12345.67"), null);
        assertEquals(12345.0, result, "BigDecimal should be truncated to its int part");
    }

    @Test
    void toDouble_bigIntegerOutOfRange_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(new BigInteger("2147483648"), null), "BigInteger exceeding int range must cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------
     *  Boolean and Character handling
     * -------------------------------------------------------------- */
    @Test
    void toDouble_booleanWrapper_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(Boolean.TRUE, null), "Boolean wrapper is not a numeric type");
    }

    @Test
    void toDouble_characterWrapper_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(Character.valueOf('c'), null), "Character is not a numeric type");
    }

    /* --------------------------------------------------------------
     *  Unsupported CharSequence types
     * -------------------------------------------------------------- */
    @Test
    void toDouble_stringBuilder_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(new StringBuilder("123"), null), "StringBuilder is not a supported CharSequence");
    }

    /* --------------------------------------------------------------
     *  Floating point special values
     * -------------------------------------------------------------- */
    @Test
    void toDouble_doubleNaN_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(Double.NaN, null), "NaN cannot be represented as an int");
    }

    @Test
    void toDouble_doublePositiveInfinity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(Double.POSITIVE_INFINITY, null), "Infinity cannot be represented as an int");
    }

    @Test
    void toDouble_floatNaN_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(Float.NaN, null), "Float NaN cannot be represented as an int");
    }

    @Test
    void toDouble_floatNegativeInfinity_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(Float.NEGATIVE_INFINITY, null), "Float Infinity cannot be represented as an int");
    }

    /* --------------------------------------------------------------
     *  BigDecimal overflow
     * -------------------------------------------------------------- */
    @Test
    void toDouble_bigDecimalOverflow_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toDouble(new BigDecimal("2147483648"), null), "BigDecimal exceeding Integer.MAX_VALUE must cause IllegalArgumentException");
    }
}
