package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_floatingPointToBigInteger_Test {

    /**
     * The Converter used by the static method under test.
     */
    private static Converter converter;

    /**
     * --------------------------------------------------------------------- *
     *  Lifecycle management
     *  ---------------------------------------------------------------------
     */
    @BeforeAll
    static void setupBeforeAll() {
        // Converter needs a ConverterOptions instance – the default options are sufficient
        // for the built‑in numeric conversions used by the method.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – helps the JVM when many tests run.
        converter = null;
    }

    /**
     * --------------------------------------------------------------------- *
     *  Method source that builds the test matrix from the JSON specification.
     *  ---------------------------------------------------------------------
     */
    private static Stream<Arguments> provideFloatingPointToBigIntegerCases() {
        // Helper reference to the default converter (used for most cases).
        Converter defaultConv = converter;
        return Stream.of(// 1. Positive double – floor
        Arguments.of(123.987, defaultConv, "123", null, "Positive double – the method should use Math.floor because d > 0.0, format to \"123\" and return BigInteger 123."), // 2. Positive integer (boxed Integer)
        Arguments.of(Integer.valueOf(123), defaultConv, "123", null, "Positive integer (boxed Integer) – toDouble yields 123.0, Math.floor(123.0) = 123, result BigInteger 123."), // 3. Zero
        Arguments.of(0.0, defaultConv, "0", null, "Zero – d is not > 0.0, so Math.ceil(0.0) = 0, formatted as \"0\", result BigInteger 0."), // 4. Negative double – ceil
        Arguments.of(-45.6, defaultConv, "-45", null, "Negative double – d <= 0.0, method uses Math.ceil, Math.ceil(-45.6) = -45, formatted as \"-45\", result BigInteger -45."), // 5. Negative integer
        Arguments.of(Integer.valueOf(-78), defaultConv, "-78", null, "Negative integer – toDouble yields -78.0, Math.ceil(-78.0) = -78, result BigInteger -78."), // 6. Positive double very close to 2 – floor (note: 2.0 floors to 2)
        Arguments.of(2.0, defaultConv, "2", null, "Positive double very close to 2 – floor truncates to 2, ensuring rounding down for positive numbers."), // 7. Negative value whose ceiling is 0
        Arguments.of(-1e-07, defaultConv, "0", null, "Negative value whose ceiling is 0 – verifies that Math.ceil is used for negative numbers."), // 8. String representing a positive floating‑point number
        Arguments.of("123.45", defaultConv, "123", null, "String representing a positive floating‑point number – custom converter parses the string to double, then floor is applied."), // 9. String with whitespace and negative number
        Arguments.of(" -99.1 ", defaultConv, "-99", null, "String with whitespace and negative number – converter trims and parses, then ceil is applied."), // 10. NaN string
        Arguments.of("NaN", defaultConv, null, NumberFormatException.class, "NaN value – String.format(\"%.0f\", Double.NaN) yields \"NaN\", BigInteger constructor throws NumberFormatException."), // 11. Positive infinity string
        Arguments.of("Infinity", defaultConv, null, NumberFormatException.class, "Positive infinity – formatted as \"Infinity\", which cannot be parsed by BigInteger, resulting in NumberFormatException."), // 12. Negative infinity string
        Arguments.of("-Infinity", defaultConv, null, NumberFormatException.class, "Negative infinity – same reasoning as positive infinity, expecting NumberFormatException."), // 13. Very large positive double
        Arguments.of(1e+19, defaultConv, "9999999999999999232", null, "Very large positive double close to Long.MAX_VALUE – floor applied, verifies that formatting does not lose precision beyond the double's 15‑16 decimal digits."), // 14. Very large negative double
        Arguments.of(-1e+19, defaultConv, "-9999999999999999232", null, "Very large negative double – ceil applied, checks handling of large magnitude negative numbers."), // 15. Smallest positive subnormal double (Double.MIN_VALUE)
        Arguments.of(5e-324, defaultConv, "0", null, "Smallest positive subnormal double (Double.MIN_VALUE) floors to 0."), // 16. Positive double just above an integer
        Arguments.of(5.000000000000001, defaultConv, "5", null, "Positive double just above an integer; Math.floor truncates the fractional part."), // 17. Negative double just below an integer
        Arguments.of(-5.000000000000001, defaultConv, "-5", null, "Negative double just below an integer; Math.ceil truncates toward zero."), // 18. High‑precision decimal string
        Arguments.of("123456789.987654321", defaultConv, "123456789", null, "String representing a high‑precision decimal; converter parses to double, floor applied."), // 19. String with surrounding whitespace
        Arguments.of("   42   ", defaultConv, "42", null, "String with surrounding whitespace; converter trims before parsing, floor yields 42."), // 20. Non‑numeric string
        Arguments.of("abc", defaultConv, null, NumberFormatException.class, "Non‑numeric string causes parsing failure, resulting in NumberFormatException."), // 21. Null input object
        Arguments.of(null, defaultConv, null, NullPointerException.class, "Null input object leads to NullPointerException inside toDouble."), // 22. Null converter argument
        Arguments.of(Integer.valueOf(123), null, null, NullPointerException.class, "Null converter argument causes NullPointerException when used."), // 23. Largest integer exactly representable in a double (2^53‑1)
        Arguments.of(9007199254740991L, defaultConv, "9007199254740991", null, "Largest integer exactly representable in a double (2^53‑1) remains unchanged after conversion."), // 24. Negative counterpart of the largest exactly representable double integer
        Arguments.of(-9007199254740991L, defaultConv, "-9007199254740991", null, "Negative counterpart of the largest exactly representable double integer; conversion preserves value."), // 25. Unsupported Boolean source type
        Arguments.of(Boolean.TRUE, defaultConv, null, IllegalArgumentException.class, "Unsupported Boolean source type – toDouble cannot handle Boolean and should throw IllegalArgumentException."), // 26. Array source type (int[])
        Arguments.of(new int[] { 1, 2 }, defaultConv, null, IllegalArgumentException.class, "Array source type – toDouble does not know how to convert an int[] and must raise IllegalArgumentException."), // 27. Float.NaN object
        Arguments.of(Float.NaN, defaultConv, null, NumberFormatException.class, "Float.NaN object – toDouble returns Double.NaN, String.format produces \"NaN\", and BigInteger constructor throws NumberFormatException."), // 28. Double.POSITIVE_INFINITY object
        Arguments.of(Double.POSITIVE_INFINITY, defaultConv, null, NumberFormatException.class, "Double.POSITIVE_INFINITY object – formatting yields \"Infinity\", which cannot be parsed by BigInteger, causing NumberFormatException."), // 29. Generic Object source
        Arguments.of(new Object(), defaultConv, null, IllegalArgumentException.class, "Generic Object source – toDouble cannot convert an arbitrary Object and should throw IllegalArgumentException."));
    }

    @Test
    void convertsPositiveDoubleUsingFloor() {
        // GIVEN
        // positive non‑integral value
        Double source = 12.9;
        // WHEN
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        // THEN
        assertEquals(BigInteger.valueOf(12), result, "Positive values must be rounded toward negative infinity (floor).");
    }

    @Test
    void convertsNegativeDoubleUsingCeil() {
        // GIVEN
        // negative non‑integral value
        Double source = -12.9;
        // WHEN
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        // THEN
        assertEquals(BigInteger.valueOf(-12), result, "Negative values must be rounded toward positive infinity (ceil).");
    }

    @Test
    void convertsExactIntegralDoubleUnchanged() {
        // GIVEN
        Double source = 42.0;
        // WHEN
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        // THEN
        assertEquals(BigInteger.valueOf(42), result);
    }

    @Test
    void convertsStringRepresentation() {
        // GIVEN
        String source = "123.456";
        // WHEN
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        // THEN
        // 123.456 -> floor -> 123
        assertEquals(BigInteger.valueOf(123), result);
    }

    @Test
    void convertsBigDecimal() {
        // GIVEN
        BigDecimal source = new BigDecimal("-987.654");
        // WHEN
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        // THEN
        // -987.654 -> ceil -> -987
        assertEquals(BigInteger.valueOf(-987), result);
    }

    @Test
    void convertsInteger() {
        // GIVEN
        // already an integral type, toDouble will produce 7.0
        Integer source = 7;
        // WHEN
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        // THEN
        assertEquals(BigInteger.valueOf(7), result);
    }

    @Test
    void convertsZeroCorrectly() {
        // GIVEN
        Double source = 0.0;
        // WHEN
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        // THEN
        assertEquals(BigInteger.ZERO, result);
    }

    @Test
    void convertsNegativeZeroHandledAsZero() {
        // GIVEN
        // Java treats -0.0 as 0.0 for numeric operations
        Double source = -0.0;
        // WHEN
        BigInteger result = NumberConversions.floatingPointToBigInteger(source, converter);
        // THEN
        assertEquals(BigInteger.ZERO, result);
    }

    /**
     * --------------------------------------------------------------------- *
     *  Parameterized test covering the exhaustive JSON test matrix.
     *  ---------------------------------------------------------------------
     */
    @ParameterizedTest(name = "{4}")
    @MethodSource("provideFloatingPointToBigIntegerCases")
    void testFloatingPointToBigInteger(Object from, Converter conv, String expectedValue, Class<? extends Throwable> expectedException, String description) {
        if (expectedException != null) {
            // Expect an exception – the method should throw the specified type.
            assertThrows(expectedException, () -> NumberConversions.floatingPointToBigInteger(from, conv), description);
        } else {
            // Normal conversion – verify the resulting BigInteger.
            BigInteger result = NumberConversions.floatingPointToBigInteger(from, conv);
            assertEquals(new BigInteger(expectedValue), result, description);
        }
    }
}
