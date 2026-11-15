package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.cedarsoftware.util.convert.CommonValues;
import com.cedarsoftware.util.convert.DefaultConverterOptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.*;
import java.util.Date;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2double_Test {

    /**
     * Holds the original static instance so we can restore it after each test.
     */
    private static Converter originalInstance;

    /**
     * Mocked Converter that will replace the static instance during tests.
     */
    private Converter mockInstance;

    @BeforeAll
    static void setupBeforeAll() {
        // Save the original singleton instance once for the whole test suite.
        originalInstance = Converter.instance;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for every test and replace the static field.
        mockInstance = mock(Converter.class);
        Converter.instance = mockInstance;
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original static instance after each test to avoid side‑effects.
        Converter.instance = originalInstance;
    }

    /**
     * Replaces the static {@code Converter.instance} with a real instance that
     * performs actual conversions.  This method is called at the start of each
     * parameterized test that needs real conversion logic.
     */
    private void useRealConverter() {
        Converter.instance = new Converter(new DefaultConverterOptions());
    }

    static Stream<Arguments> validConversionProvider() {
        return Stream.of(Arguments.of("Integer value 5 → 5.0", 5, 5.0), Arguments.of("String \"3.14\" → 3.14", "3.14", 3.14), Arguments.of("BigDecimal 123.456 → 123.456", new BigDecimal("123.456"), 123.456), Arguments.of("Boolean true → 1.0", true, 1.0), Arguments.of("Boolean false → 0.0", false, 0.0));
    }

    static Stream<Arguments> numericTypesProvider() {
        return Stream.of(Arguments.of("Long 1234567890123 → 1234567890123.0", 1234567890123L, 1234567890123.0), Arguments.of("Short 42 → 42.0", (short) 42, 42.0), Arguments.of("Byte -8 → -8.0", (byte) -8, -8.0), Arguments.of("Float 2.5f → 2.5", 2.5f, 2.5), Arguments.of("Double -3.14159 → -3.14159", -3.14159, -3.14159), Arguments.of("BigInteger 9876543210 → 9876543210.0", new BigInteger("9876543210"), 9876543210.0));
    }

    static Stream<Arguments> atomicTypesProvider() {
        return Stream.of(Arguments.of("AtomicInteger 10 → 10.0", new AtomicInteger(10), 10.0), Arguments.of("AtomicLong 10000000000 → 10000000000.0", new AtomicLong(10_000_000_000L), 10_000_000_000.0), Arguments.of("AtomicBoolean true → 1.0", new AtomicBoolean(true), 1.0), Arguments.of("AtomicBoolean false → 0.0", new AtomicBoolean(false), 0.0));
    }

    static Stream<Arguments> stringEdgeCasesProvider() {
        return Stream.of(Arguments.of("Whitespace trimmed \"  42  \" → 42.0", "  42  ", 42.0), Arguments.of("Explicit plus sign \"+5.5\" → 5.5", "+5.5", 5.5), Arguments.of("Scientific notation \"1e3\" → 1000.0", "1e3", 1000.0), Arguments.of("Negative string \"-123.45\" → -123.45", "-123.45", -123.45), Arguments.of("String \"NaN\" → Double.NaN", "NaN", Double.NaN), Arguments.of("String \"Infinity\" → Double.POSITIVE_INFINITY", "Infinity", Double.POSITIVE_INFINITY));
    }

    static Stream<Arguments> dateTimeProvider() {
        return Stream.of(Arguments.of("java.util.Date epoch → 0.0", new Date(0L), 0.0), Arguments.of("java.sql.Timestamp 1622548800000 → 1622548800000.0", new Timestamp(1622548800000L), 1622548800000.0), Arguments.of("LocalDate epoch → 0.0", LocalDate.ofEpochDay(0), 0.0), Arguments.of("LocalDateTime epoch → 0.0", LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC), 0.0), Arguments.of("ZonedDateTime UTC epoch → 0.0", ZonedDateTime.ofInstant(Instant.EPOCH, ZoneOffset.UTC), 0.0));
    }

    static Stream<Arguments> largeNumbersProvider() {
        return Stream.of(Arguments.of("BigDecimal == Double.MAX_VALUE", new BigDecimal("1.7976931348623157E308"), Double.MAX_VALUE), Arguments.of("BigDecimal > Double.MAX_VALUE", new BigDecimal("1.7976931348623159E308"), Double.POSITIVE_INFINITY));
    }

    static Stream<Arguments> stringParsingExceptionProvider() {
        return Stream.of(Arguments.of("Only whitespace \"   \"", "   "), Arguments.of("Hexadecimal notation \"0xFF\"", "0xFF"), Arguments.of("Trailing characters \"123abc\"", "123abc"), Arguments.of("Literal \"null\"", "null"));
    }

    @Test
    void convert2double_NullInput_ReturnsLogicalZero() {
        // GIVEN – a null argument (no special setup needed)
        // WHEN
        double result = Converter.convert2double(null);
        // THEN – the method should return the logical zero for doubles.
        assertEquals(CommonValues.DOUBLE_ZERO, result, "convert2double(null) should return CommonValues.DOUBLE_ZERO");
    }

    @Test
    void convert2double_NonNullInput_DelegatesToInstanceConvert() {
        // GIVEN
        Object source = "123.45";
        double expected = 123.45;
        // Stub the mock to return the expected value when called with the source and double.class.
        when(mockInstance.convert(eq(source), eq(double.class))).thenReturn(expected);
        // WHEN
        double actual = Converter.convert2double(source);
        // THEN
        assertEquals(expected, actual, "convert2double should return the value supplied by the instance conversion");
        // Verify that the mock's convert method was invoked exactly once with the correct arguments.
        verify(mockInstance, times(1)).convert(eq(source), eq(double.class));
    }

    @Test
    void convert2double_NonNullInput_UsesInstanceEvenIfResultIsZero() {
        // GIVEN
        // any non‑null object
        Object source = new Object();
        when(mockInstance.convert(eq(source), eq(double.class))).thenReturn(0.0);
        // WHEN
        double result = Converter.convert2double(source);
        // THEN
        assertEquals(0.0, result, "Result should be the value returned by the mocked instance (zero in this case)");
        verify(mockInstance, times(1)).convert(eq(source), eq(double.class));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionProvider")
    void convert2double_ValidConversions(String description, Object input, double expected) {
        // use the real conversion engine
        useRealConverter();
        double actual = Converter.convert2double(input);
        assertEquals(expected, actual, () -> "Failed on case: " + description);
    }

    @Test
    void convert2double_InvalidConversion_ThrowsException() {
        useRealConverter();
        // no registered conversion to double
        Object input = new Object();
        assertThrows(RuntimeException.class, () -> Converter.convert2double(input), "An object with no registered conversion should cause a conversion exception");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericTypesProvider")
    void convert2double_NumericTypes(String description, Object input, double expected) {
        useRealConverter();
        double actual = Converter.convert2double(input);
        assertEquals(expected, actual, () -> "Failed on case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("atomicTypesProvider")
    void convert2double_AtomicTypes(String description, Object input, double expected) {
        useRealConverter();
        double actual = Converter.convert2double(input);
        assertEquals(expected, actual, () -> "Failed on case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringEdgeCasesProvider")
    void convert2double_StringEdgeCases(String description, String input, Double expected) {
        useRealConverter();
        double actual = Converter.convert2double(input);
        if (expected.isNaN()) {
            assertTrue(Double.isNaN(actual), "Expected NaN for case: " + description);
        } else if (expected.isInfinite()) {
            assertEquals(expected, actual, "Expected Infinity for case: " + description);
        } else {
            assertEquals(expected, actual, "Failed on case: " + description);
        }
    }

    @Test
    void convert2double_StringEdgeCases_EmptyString_ThrowsException() {
        useRealConverter();
        assertThrows(RuntimeException.class, () -> Converter.convert2double(""), "Empty string should raise a conversion exception");
    }

    @Test
    void convert2double_StringEdgeCases_NonNumeric_ThrowsException() {
        useRealConverter();
        assertThrows(RuntimeException.class, () -> Converter.convert2double("abc123"), "Non‑numeric string should cause a conversion exception");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("dateTimeProvider")
    void convert2double_DateAndTimeTypes(String description, Object input, double expected) {
        useRealConverter();
        double actual = Converter.convert2double(input);
        assertEquals(expected, actual, () -> "Failed on case: " + description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("largeNumbersProvider")
    void convert2double_LargeNumbers(String description, BigDecimal input, double expected) {
        useRealConverter();
        double actual = Converter.convert2double(input);
        if (Double.isInfinite(expected)) {
            assertEquals(Double.POSITIVE_INFINITY, actual, () -> "Expected Infinity for case: " + description);
        } else {
            assertEquals(expected, actual, () -> "Failed on case: " + description);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringParsingExceptionProvider")
    void convert2double_StringParsingExceptions(String description, String input) {
        useRealConverter();
        assertThrows(RuntimeException.class, () -> Converter.convert2double(input), "Expected a conversion exception for case: " + description);
    }
}
