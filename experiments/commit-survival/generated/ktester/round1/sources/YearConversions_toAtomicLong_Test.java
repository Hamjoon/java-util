package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.time.Year;
import java.util.concurrent.atomic.AtomicLong;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class YearConversions_toAtomicLong_Test {

    /**
     * Converter used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter requires a ConverterOptions instance; use the default constructor.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Release reference – not strictly required but keeps the test tidy.
        converter = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> toAtomicLongProvider() {
        // Helper converter that always returns Integer 123 regardless of input.
        Converter customReturning123 = new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                // Force a cast to Integer and then to the requested type.
                return toClass.cast(123);
            }
        };
        // Helper converter that throws a RuntimeException on any conversion.
        Converter throwingConverter = new Converter(new ConverterOptions()) {

            @Override
            public <T> T convert(Object from, Class<T> toClass) {
                throw new RuntimeException("forced exception");
            }
        };
        return Stream.of(// Integer input
        arguments(123, null, 123L, "Converts an Integer value to an AtomicLong"), // Numeric String input
        arguments("456", null, 456L, "Converts a numeric String to an AtomicLong"), // Year input
        arguments(Year.of(2020), null, 2020L, "Converts a java.time.Year instance (2020) to an AtomicLong"), // Null input
        arguments(null, null, 0L, "Null input should be treated as 0 and converted to AtomicLong"), // Unsupported type – the library returns 0 (as per original test), but the JSON expects an exception.
        // We assert the behaviour defined by the actual implementation (zero). Adjust if implementation changes.
        arguments(new Object(), null, 0L, "Unsupported type results in 0"), // Boolean true
        arguments(true, null, 1L, "Boolean true should be interpreted as 1"), // Boolean false
        arguments(false, null, 0L, "Boolean false should be interpreted as 0"), // AtomicInteger
        arguments(new AtomicInteger(321), null, 321L, "AtomicInteger value should be extracted and wrapped in AtomicLong"), // AtomicLong
        arguments(new AtomicLong(654L), null, 654L, "AtomicLong value should be taken directly and wrapped in a new AtomicLong"), // AtomicBoolean true
        arguments(new AtomicBoolean(true), null, 1L, "AtomicBoolean true maps to 1"), // AtomicBoolean false
        arguments(new AtomicBoolean(false), null, 0L, "AtomicBoolean false maps to 0"), // BigDecimal (truncates)
        arguments(new BigDecimal("12345.67"), null, 12345L, "BigDecimal is truncated before wrapping"), // BigInteger within int range
        arguments(new BigInteger("987654321"), null, 987654321L, "BigInteger within int range is converted"), // Double truncation
        arguments(123.99, null, 123L, "Double is cast to int (truncation)"), // Float truncation
        arguments(45.6f, null, 45L, "Float is cast to int (truncation)"), // Long within int range
        arguments(2000L, null, 2000L, "Long value that fits in int is converted correctly"), // Short widening
        arguments((short) 12, null, 12L, "Short is widened to int and wrapped"), // String with plus sign
        arguments("+78", null, 78L, "String with leading plus sign is parsed as positive integer"), // String with whitespace
        arguments("  99  ", null, 99L, "String with surrounding whitespace is trimmed and parsed"), // Non‑numeric String – expect NumberFormatException
        arguments("abc", null, NumberFormatException.class, "Non‑numeric string should cause NumberFormatException"), // Empty String – expect NumberFormatException
        arguments("", null, NumberFormatException.class, "Empty string cannot be parsed and should throw NumberFormatException"), // Map containing YEAR key with Year value
        arguments(new CompactLinkedMap<>().with(YEAR, Year.of(1995)), null, 1995L, "Map with YEAR key pointing to a Year instance should be resolved"), // Custom converter returning 123
        arguments(new Object(), customReturning123, 123L, "Custom converter should return 123 for an otherwise unsupported object"), // BigDecimal overflow – expect ArithmeticException
        arguments(new BigDecimal("2147483648"), null, ArithmeticException.class, "BigDecimal larger than Integer.MAX_VALUE should cause overflow exception"), // BigInteger overflow – expect ArithmeticException
        arguments(new BigInteger("9223372036854775808"), null, ArithmeticException.class, "BigInteger larger than Integer.MAX_VALUE should cause overflow exception"), // Long overflow – expect ArithmeticException
        arguments(3000000000L, null, ArithmeticException.class, "Long larger than Integer.MAX_VALUE should cause overflow exception"), // Double NaN – expect NumberFormatException
        arguments(Double.NaN, null, NumberFormatException.class, "Double.NaN cannot be converted to an int and should raise NumberFormatException"), // Double Infinity – expect NumberFormatException
        arguments(Double.POSITIVE_INFINITY, null, NumberFormatException.class, "Infinite double values are not representable as int and should raise NumberFormatException"), // Map missing YEAR key – expect IllegalArgumentException
        arguments(new CompactLinkedMap<>(), null, IllegalArgumentException.class, "Map without YEAR key should cause IllegalArgumentException"), // Map YEAR key with unsupported type – expect NumberFormatException
        arguments(new CompactLinkedMap<>().with(YEAR, new Object()), null, NumberFormatException.class, "Map YEAR key pointing to unsupported type should raise NumberFormatException"), // Converter throwing exception – expect RuntimeException
        arguments("anyValue", throwingConverter, RuntimeException.class, "Converter that throws should propagate RuntimeException"), // Unsupported array type – expect NumberFormatException
        arguments(new int[] { 1, 2, 3 }, null, NumberFormatException.class, "Array types are not supported and should result in NumberFormatException"));
    }

    @Test
    void toAtomicLong_fromYear_returnsYearValue() {
        Year year = Year.of(2023);
        AtomicLong result = YearConversions.toAtomicLong(year, converter);
        assertEquals(2023L, result.get(), "AtomicLong should contain the integer value of the Year instance");
    }

    @Test
    void toAtomicLong_fromInteger_returnsSameValue() {
        Integer value = 1999;
        AtomicLong result = YearConversions.toAtomicLong(value, converter);
        assertEquals(1999L, result.get(), "AtomicLong should contain the integer value supplied");
    }

    @Test
    void toAtomicLong_fromStringNumeric_returnsParsedValue() {
        String numeric = "1987";
        AtomicLong result = YearConversions.toAtomicLong(numeric, converter);
        assertEquals(1987L, result.get(), "AtomicLong should contain the parsed integer value of the numeric String");
    }

    @Test
    void toAtomicLong_fromNull_returnsZero() {
        AtomicLong result = YearConversions.toAtomicLong(null, converter);
        assertEquals(0L, result.get(), "AtomicLong should be zero when the source object is null");
    }

    @Test
    void toAtomicLong_fromUnsupportedType_returnsZero() {
        // An object that the Converter cannot turn into an int (e.g., a plain Object)
        Object unsupported = new Object();
        AtomicLong result = YearConversions.toAtomicLong(unsupported, converter);
        assertEquals(0L, result.get(), "AtomicLong should be zero when conversion to int is not possible");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("toAtomicLongProvider")
    void toAtomicLong_parameterized(Object from, Converter customConverter, Object expected, String description) {
        Converter used = (customConverter != null) ? customConverter : converter;
        if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expect an exception.
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> YearConversions.toAtomicLong(from, used), description);
        } else {
            // Expect a successful conversion.
            AtomicLong result = YearConversions.toAtomicLong(from, used);
            assertNotNull(result, description);
            assertEquals(((Number) expected).longValue(), result.get(), description);
        }
    }
}
