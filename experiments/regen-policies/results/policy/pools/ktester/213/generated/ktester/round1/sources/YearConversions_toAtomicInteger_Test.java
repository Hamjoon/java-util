package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Year;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class YearConversions_toAtomicInteger_Test {

    /**
     * Shared {@link Converter} used by all tests.
     */
    private static Converter converter;

    /**
     * Simple holder for parameterised test arguments.
     */
    private static class ArgumentsHolder {

        final Object input;

        final int expected;

        final String description;

        ArgumentsHolder(Object input, int expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * Custom {@link Converter} that always returns the integer {@code 555}
     * regardless of the input.
     */
    private static class CustomConverterReturning555 extends Converter {

        CustomConverterReturning555() {
            super(new ConverterOptions());
        }

        @Override
        public Object convert(Object source, Class<?> target) {
            // The conversion contract expects an Integer for toInt().
            if (target == Integer.class || target == int.class) {
                return 555;
            }
            return super.convert(source, target);
        }
    }

    /**
     * Custom {@link Converter} that returns {@code null} for any conversion.
     */
    private static class CustomConverterReturningNull extends Converter {

        CustomConverterReturningNull() {
            super(new ConverterOptions());
        }

        @Override
        public Object convert(Object source, Class<?> target) {
            return null;
        }
    }

    /**
     * Custom {@link Converter} that throws {@link IllegalArgumentException}
     * for every conversion attempt.
     */
    private static class CustomConverterThrowingIAE extends Converter {

        CustomConverterThrowingIAE() {
            super(new ConverterOptions());
        }

        @Override
        public Object convert(Object source, Class<?> target) {
            throw new IllegalArgumentException("Conversion not supported by custom converter");
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Converter with default options.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the
        // test class tidy.
        converter = null;
    }

    /**
     * Primitive and wrapper types.
     */
    static Stream<ArgumentsHolder> primitiveAndWrapperProvider() {
        return Stream.of(new ArgumentsHolder(42, 42, "Integer"), new ArgumentsHolder(123L, 123, "Long (truncated)"), new ArgumentsHolder("56", 56, "Numeric String"), new ArgumentsHolder("-7", -7, "Negative numeric String"));
    }

    /**
     * Special Java types.
     */
    static Stream<ArgumentsHolder> specialJavaTypesProvider() {
        return Stream.of(new ArgumentsHolder(Year.of(2021), 2021, "Year"), new ArgumentsHolder(new BigDecimal("7.9"), 7, "BigDecimal (truncation)"), new ArgumentsHolder(new BigInteger("12345"), 12345, "BigInteger"), new ArgumentsHolder(true, 1, "Boolean true"), new ArgumentsHolder(false, 0, "Boolean false"), new ArgumentsHolder(new AtomicInteger(88), 88, "Existing AtomicInteger"));
    }

    @Test
    void shouldConvertYearToAtomicInteger() {
        Year year = Year.of(2023);
        AtomicInteger result = YearConversions.toAtomicInteger(year, converter);
        assertNotNull(result, "Result must not be null");
        assertEquals(2023, result.get(), "AtomicInteger should contain the year value");
    }

    @Test
    void shouldConvertStringNumberToAtomicInteger() {
        String source = "1999";
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(1999, result.get());
    }

    @Test
    void shouldConvertIntegerToAtomicInteger() {
        Integer source = 42;
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(42, result.get());
    }

    @Test
    void shouldReturnZeroWhenSourceIsNull() {
        // According to the contract, a null source converts to 0 for numeric primitives.
        AtomicInteger result = YearConversions.toAtomicInteger(null, converter);
        assertNotNull(result);
        assertEquals(0, result.get());
    }

    @Test
    void shouldHandleNonNumericStringGracefully() {
        // The default Converter returns 0 for a non‑numeric string when converting to int.
        String source = "not-a-number";
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(0, result.get(), "Non‑numeric string should convert to 0 according to Converter contract");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("primitiveAndWrapperProvider")
    void shouldConvertPrimitiveAndWrapperTypes(ArgumentsHolder holder) {
        Object source = holder.input;
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(holder.expected, result.get(), "Conversion of " + holder.description + " failed");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("specialJavaTypesProvider")
    void shouldConvertSpecialJavaTypes(ArgumentsHolder holder) {
        Object source = holder.input;
        AtomicInteger result = YearConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(holder.expected, result.get(), "Conversion of " + holder.description + " failed");
    }

    /**
     * Map with YEAR key.
     */
    @Test
    void shouldConvertMapContainingYearKey() {
        Map<Object, Object> map = Map.of(YEAR, 1999);
        AtomicInteger result = YearConversions.toAtomicInteger(map, converter);
        assertNotNull(result);
        assertEquals(1999, result.get());
    }

    @Test
    void shouldThrowNumberFormatExceptionForMalformedString() {
        String source = "notANumber";
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicInteger(source, converter), "A non‑numeric String should cause NumberFormatException");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenSourceIsNullAndConverterIsCustom() {
        Converter custom = new CustomConverterReturning555();
        // The method validates the source before delegating to the converter.
        assertThrows(NullPointerException.class, () -> YearConversions.toAtomicInteger(null, custom), "Null source should raise NPE even with a custom converter");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForUnsupportedObject() {
        Object source = new Object();
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toAtomicInteger(source, converter), "Unsupported object type should raise IllegalArgumentException");
    }

    @Test
    void shouldParseStringValueFromMapYearKey() {
        Map<Object, Object> map = Map.of(YEAR, "2022");
        AtomicInteger result = YearConversions.toAtomicInteger(map, converter);
        assertNotNull(result);
        assertEquals(2022, result.get());
    }

    @Test
    void shouldThrowNumberFormatExceptionWhenMapYearValueIsNonNumericString() {
        Map<Object, Object> map = Map.of(YEAR, "notANumber");
        assertThrows(NumberFormatException.class, () -> YearConversions.toAtomicInteger(map, converter), "Map YEAR key with non‑numeric String should cause NumberFormatException");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenMapYearValueIsNull() {
        Map<Object, Object> map = Map.of(YEAR, null);
        assertThrows(NullPointerException.class, () -> YearConversions.toAtomicInteger(map, converter), "Map YEAR key with null value should raise NullPointerException");
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenMapLacksYearKey() {
        Map<Object, Object> map = Map.of("someOtherKey", 1234);
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toAtomicInteger(map, converter), "Map without YEAR key should raise IllegalArgumentException");
    }

    @Test
    void shouldConvertByteWrapper() {
        Byte src = Byte.valueOf((byte) 9);
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        assertEquals(9, result.get());
    }

    @Test
    void shouldConvertShortWrapperNegative() {
        Short src = Short.valueOf((short) -3);
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        assertEquals(-3, result.get());
    }

    @Test
    void shouldConvertFloatTruncatingFraction() {
        Float src = Float.valueOf(12.34f);
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        assertEquals(12, result.get());
    }

    @Test
    void shouldConvertDoubleNegativeTruncatingFraction() {
        Double src = Double.valueOf(-56.78);
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        assertEquals(-56, result.get());
    }

    @Test
    void shouldDemonstrateOverflowWithLongMaxValue() {
        Long src = Long.MAX_VALUE;
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        // int cast of Long.MAX_VALUE yields -1 due to overflow.
        assertEquals(-1, result.get());
    }

    @Test
    void shouldDemonstrateOverflowWithBigIntegerJustAboveIntMax() {
        // Integer.MAX_VALUE + 1
        BigInteger src = new BigInteger("2147483648");
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        // Two's‑complement overflow wraps to Integer.MIN_VALUE.
        assertEquals(Integer.MIN_VALUE, result.get());
    }

    @Test
    void shouldConvertNegativeBigDecimalTruncatingFraction() {
        BigDecimal src = new BigDecimal("-9.999");
        AtomicInteger result = YearConversions.toAtomicInteger(src, converter);
        assertEquals(-9, result.get());
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForDate() {
        java.util.Date src = new java.util.Date();
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toAtomicInteger(src, converter));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForCharArray() {
        char[] src = new char[] { 'a', 'b' };
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toAtomicInteger(src, converter));
    }

    @Test
    void customConverterShouldReturn555() {
        Converter custom = new CustomConverterReturning555();
        // The source is irrelevant; the custom converter always returns 555.
        AtomicInteger result = YearConversions.toAtomicInteger("anyValue", custom);
        assertEquals(555, result.get());
    }

    @Test
    void customConverterReturningNullShouldPropagateNPE() {
        Converter custom = new CustomConverterReturningNull();
        assertThrows(NullPointerException.class, () -> YearConversions.toAtomicInteger("ignored", custom));
    }

    @Test
    void customConverterThrowingIAEShouldPropagate() {
        Converter custom = new CustomConverterThrowingIAE();
        assertThrows(IllegalArgumentException.class, () -> YearConversions.toAtomicInteger("123", custom));
    }
}
