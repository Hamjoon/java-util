package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toAtomicInteger_Test {

    /**
     * Shared ConverterOptions for all tests.
     */
    private static ConverterOptions sharedOptions;

    /**
     * Converter instance created for each test.
     */
    private Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a fresh options object once for the whole test suite.
        sharedOptions = new ConverterOptions();
    }

    @BeforeEach
    void setupBeforeEach() {
        // A new Converter for every test guarantees isolation.
        converter = new Converter(sharedOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse.
        converter = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Clear static reference.
        sharedOptions = null;
    }

    private static Stream<Arguments> validNumericProvider() {
        return Stream.of(Arguments.of("Integer value", 123, 123), Arguments.of("String numeric", "456", 456), Arguments.of("String with whitespace", "   789   ", 789), Arguments.of("Boolean true → 1", true, 1), Arguments.of("Boolean false → 0", false, 0));
    }

    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(Arguments.of("Non‑numeric String", "notANumber"), Arguments.of("Floating‑point number", 3.14), Arguments.of("Arbitrary Object", new Object()));
    }

    private static Stream<Arguments> numericTypesProvider() {
        return Stream.of(Arguments.of("Long within int range", 100L, 100), Arguments.of("Short value", (short) 42, 42), Arguments.of("Byte value", (byte) 7, 7), Arguments.of("BigInteger fitting int", new BigInteger("12345"), 12345), Arguments.of("BigDecimal integral value", new BigDecimal("6789"), 6789));
    }

    private static Stream<Arguments> boundaryProvider() {
        return Stream.of(// valid extremes
        Arguments.of("Integer.MAX_VALUE", Integer.MAX_VALUE, null), Arguments.of("Integer.MIN_VALUE", Integer.MIN_VALUE, null), // overflow cases
        Arguments.of("Long.MAX_VALUE (out of int range)", Long.MAX_VALUE, IllegalArgumentException.class), Arguments.of("BigInteger just beyond max int", new BigInteger("2147483648"), IllegalArgumentException.class), Arguments.of("BigDecimal just below min int", new BigDecimal("-2147483649"), IllegalArgumentException.class));
    }

    private static Stream<Arguments> stringEdgeProvider() {
        return Stream.of(Arguments.of("Explicit plus sign", "+321", 321), Arguments.of("Explicit minus sign", "-987", -987), Arguments.of("Hexadecimal not supported", "0xFF", IllegalArgumentException.class), Arguments.of("Whitespace only", "   ", IllegalArgumentException.class), Arguments.of("Empty string", "", IllegalArgumentException.class));
    }

    private static Stream<Arguments> characterProvider() {
        return Stream.of(Arguments.of("Digit character '5'", '5', 5), Arguments.of("Digit character '0'", '0', 0), Arguments.of("Non‑numeric character 'a'", 'a', IllegalArgumentException.class));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(Arguments.of("java.util.Date instance", new Date()), Arguments.of("java.util.UUID instance", UUID.randomUUID()), Arguments.of("java.time.LocalDate instance", LocalDate.now()), Arguments.of("Generic Object", new Object()), Arguments.of("Enum value (RoundingMode.HALF_UP)", RoundingMode.HALF_UP));
    }

    private static Stream<Arguments> stringOverflowProvider() {
        return Stream.of(Arguments.of("String > Integer.MAX_VALUE", "2147483648"), Arguments.of("String < Integer.MIN_VALUE", "-2147483649"));
    }

    private static Stream<Arguments> floatingPointStringProvider() {
        return Stream.of(Arguments.of("String NaN", "NaN"), Arguments.of("String Infinity", "Infinity"), Arguments.of("String -Infinity", "-Infinity"));
    }

    private static Stream<Arguments> arrayTypesProvider() {
        return Stream.of(Arguments.of("byte[] array", new byte[] { 1, 2, 3 }), Arguments.of("char[] array", new char[] { '1', '2' }));
    }

    @Test
    @DisplayName("Convert Integer to AtomicInteger")
    void testConvertInteger() {
        Integer source = 7;
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(7, result.get());
    }

    @Test
    @DisplayName("Convert numeric String to AtomicInteger")
    void testConvertStringNumber() {
        String source = "123";
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(123, result.get());
    }

    @Test
    @DisplayName("Convert null source to AtomicInteger (should be 0)")
    void testConvertNull() {
        AtomicInteger result = StringConversions.toAtomicInteger(null, converter);
        assertNotNull(result);
        assertEquals(0, result.get());
    }

    @Test
    @DisplayName("Convert non‑numeric String throws NumberFormatException")
    void testConvertInvalidString() {
        String source = "not-a-number";
        assertThrows(NumberFormatException.class, () -> StringConversions.toAtomicInteger(source, converter));
    }

    @Test
    @DisplayName("Convert Long to AtomicInteger (truncates long value)")
    void testConvertLong() {
        Long source = 42L;
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(42, result.get());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validNumericProvider")
    @DisplayName("Valid numeric conversions")
    void testValidNumericConversions(String description, Object source, int expected) {
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), description);
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Null source should be treated as 0")
    void testNullSource() {
        AtomicInteger result = StringConversions.toAtomicInteger(null, converter);
        assertNotNull(result);
        assertEquals(0, result.get());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidConversionProvider")
    @DisplayName("Invalid conversions – expect IllegalArgumentException")
    void testInvalidConversions(String description, Object source) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicInteger(source, converter), description);
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Custom converter returns 42")
    void testCustomConverterReturns42() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Integer.class))).thenReturn(42);
        AtomicInteger result = StringConversions.toAtomicInteger("anyValue", mock);
        assertNotNull(result);
        assertEquals(42, result.get());
    }

    @Test
    @DisplayName("Custom converter throws exception – should propagate")
    void testCustomConverterThrowsException() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Integer.class))).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> StringConversions.toAtomicInteger(null, mock));
        assertEquals("boom", ex.getMessage());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericTypesProvider")
    @DisplayName("Conversion from various numeric types")
    void testNumericTypes(String description, Object source, int expected) {
        AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("boundaryProvider")
    @DisplayName("Boundary values and overflow handling")
    void testBoundaryValues(String description, Object source, Class<? extends Throwable> expectedException) {
        if (expectedException == null) {
            AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
            assertNotNull(result);
            assertEquals(source, result.get(), description);
        } else {
            assertThrows(expectedException, () -> StringConversions.toAtomicInteger(source, converter), description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringEdgeProvider")
    @DisplayName("String edge case handling")
    void testStringEdgeCases(String description, String source, Object expected) {
        if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> StringConversions.toAtomicInteger(source, converter), description);
        } else {
            AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
            assertNotNull(result);
            assertEquals(expected, result.get(), description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("characterProvider")
    @DisplayName("Character to AtomicInteger conversion")
    void testCharacterConversion(String description, char source, Object expected) {
        if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> StringConversions.toAtomicInteger(source, converter), description);
        } else {
            AtomicInteger result = StringConversions.toAtomicInteger(source, converter);
            assertNotNull(result);
            assertEquals(expected, result.get(), description);
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedTypesProvider")
    @DisplayName("Unsupported source types should raise IllegalArgumentException")
    void testUnsupportedTypes(String description, Object source) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicInteger(source, converter), description);
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Custom converter returns null → IllegalArgumentException")
    void testCustomConverterReturnsNull() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Integer.class))).thenReturn(null);
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicInteger("anyValue", mock));
    }

    @Test
    @DisplayName("Custom converter returns non‑numeric String → IllegalArgumentException")
    void testCustomConverterReturnsString() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Integer.class))).thenReturn("notANumber");
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicInteger("anyValue", mock));
    }

    @Test
    @DisplayName("Custom converter returns Integer -5")
    void testCustomConverterReturnsMinusFive() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Integer.class))).thenReturn(-5);
        AtomicInteger result = StringConversions.toAtomicInteger("123", mock);
        assertNotNull(result);
        assertEquals(-5, result.get());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringOverflowProvider")
    @DisplayName("String overflow beyond int range")
    void testStringOverflow(String description, String source) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicInteger(source, converter), description);
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("floatingPointStringProvider")
    @DisplayName("Floating‑point string values are invalid")
    void testFloatingPointStrings(String description, String source) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicInteger(source, converter), description);
    }

    // -----------------------------------------------------------------
    @Test
    @DisplayName("Custom converter returns Long > Integer.MAX_VALUE → IllegalArgumentException")
    void testCustomConverterReturnsLongOverflow() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Integer.class))).thenReturn(Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicInteger("anyValue", mock));
    }

    @Test
    @DisplayName("Custom converter returns Double with fraction → IllegalArgumentException")
    void testCustomConverterReturnsDoubleFraction() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Integer.class))).thenReturn(3.5);
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicInteger("anyValue", mock));
    }

    @Test
    @DisplayName("Custom converter returns numeric String overflow → IllegalArgumentException")
    void testCustomConverterReturnsStringOverflow() {
        Converter mock = mock(Converter.class);
        when(mock.convert(any(), eq(Integer.class))).thenReturn("2147483648");
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicInteger("anyValue", mock));
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("arrayTypesProvider")
    @DisplayName("Array types are not convertible to AtomicInteger")
    void testArrayTypes(String description, Object source) {
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toAtomicInteger(source, converter), description);
    }
}
