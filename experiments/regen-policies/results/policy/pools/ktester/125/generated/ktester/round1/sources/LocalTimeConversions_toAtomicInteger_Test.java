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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalTimeConversions_toAtomicInteger_Test {

    /**
     * Shared {@link Converter} instance used by all test cases.
     */
    private static Converter converter;

    /**
     * Shared real {@link Converter} instance used for the “identity” cases.
     */
    private static Converter identityConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly required but keeps the test tidy.
        converter = null;
    }

    private static Stream<Arguments> validNumericConversions() {
        return Stream.of(Arguments.of("Integer → AtomicInteger", Integer.valueOf(42), 42), Arguments.of("Long within int range → AtomicInteger", Long.valueOf(9_876_543L), 9_876_543), Arguments.of("String numeric → AtomicInteger", "256", 256), Arguments.of("BigDecimal (truncated) → AtomicInteger", new BigDecimal("12345.67"), 12345), Arguments.of("null → AtomicInteger (treated as 0)", null, 0));
    }

    private static Stream<Arguments> invalidConversions() {
        return Stream.of(Arguments.of("Non‑numeric string → NumberFormatException", "notANumber", NumberFormatException.class), Arguments.of("Unsupported object type → IllegalArgumentException", new Object(), IllegalArgumentException.class), Arguments.of("Empty string → NumberFormatException", "", NumberFormatException.class), Arguments.of("Whitespace only string → NumberFormatException", "   ", NumberFormatException.class), // handled by the method itself
        Arguments.of("Null converter → NullPointerException", null, NullPointerException.class));
    }

    private static Stream<Arguments> boundaryConversions() {
        return Stream.of(Arguments.of("Integer.MAX_VALUE → unchanged", Integer.valueOf(Integer.MAX_VALUE), Integer.MAX_VALUE), Arguments.of("Integer.MIN_VALUE → unchanged", Integer.valueOf(Integer.MIN_VALUE), Integer.MIN_VALUE), // Long.MAX_VALUE = 0x7FFF_FFFF_FFFF_FFFF → low 32 bits = 0xFFFF_FFFF = -1
        Arguments.of("Long.MAX_VALUE overflow → -1", Long.valueOf(Long.MAX_VALUE), -1), // Long.MIN_VALUE = 0x8000_0000_0000_0000 → low 32 bits = 0x0000_0000 = 0
        Arguments.of("Long.MIN_VALUE overflow → 0", Long.valueOf(Long.MIN_VALUE), 0), // BigInteger > Long.MAX_VALUE, low 32 bits are zero
        Arguments.of("BigInteger overflow → 0", new BigInteger("9223372036854775808"), 0), // BigDecimal truncation before cast
        Arguments.of("BigDecimal with fraction → truncated", new BigDecimal("123456789.987"), 123456789));
    }

    private static Stream<Arguments> otherSupportedTypes() {
        return Stream.of(Arguments.of("Double → truncated", Double.valueOf(12345.67), 12345), Arguments.of("Float → truncated", Float.valueOf(-12.34f), -12), Arguments.of("Short → promoted", Short.valueOf((short) 200), 200), Arguments.of("Byte → promoted", Byte.valueOf((byte) -5), -5), Arguments.of("AtomicInteger → unwrapped", new AtomicInteger(99), 99), // 3_000_000_000L = 0xB2D05E00 → low 32 bits = 0xB2D05E00 = -1294967296
        Arguments.of("AtomicLong overflow → casted", new AtomicLong(3_000_000_000L), -1294967296));
    }

    private static Stream<Arguments> stringEdgeCases() {
        return Stream.of(// will be handled by exception test below
        Arguments.of("Empty string → NumberFormatException", "", 0), // same as above
        Arguments.of("Whitespace only → NumberFormatException", "   ", 0), Arguments.of("Explicit plus sign → parsed as 5", "+5", 5), Arguments.of("Explicit minus sign → parsed as -7", "-7", -7));
    }

    private static Stream<Arguments> stringEdgeCasesExceptions() {
        return Stream.of(Arguments.of("Empty string → NumberFormatException", "", NumberFormatException.class), Arguments.of("Whitespace only → NumberFormatException", "   ", NumberFormatException.class));
    }

    @Test
    void shouldConvertIntegerDirectly() {
        Integer source = 42;
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(42, result.get());
    }

    @Test
    void shouldConvertStringRepresentingNumber() {
        String source = "12345";
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        assertEquals(12345, result.get());
    }

    @Test
    void shouldConvertLongValue() {
        Long source = 9_876_543L;
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        // The long value fits within an int, so the cast should preserve the numeric value.
        assertEquals(9_876_543, result.get());
    }

    @Test
    void shouldHandleNullInputAsZero() {
        Object source = null;
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        // According to Converter's null‑handling contract, numeric primitives become 0.
        assertEquals(0, result.get());
    }

    @Test
    void shouldConvertDoubleTruncatingFraction() {
        Double source = 77.99;
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, converter);
        assertNotNull(result);
        // Conversion to long truncates the fractional part.
        assertEquals(77, result.get());
    }

    /* ------------------------------------------------------------
     *  Valid numeric conversions
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("validNumericConversions")
    void shouldConvertValidNumericValues(String description, Object source, int expected) {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, identityConverter);
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), description);
    }

    /* ------------------------------------------------------------
     *  Invalid or exceptional conversions
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidConversions")
    void shouldThrowOnInvalidConversions(String description, Object source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalTimeConversions.toAtomicInteger(source, identityConverter), description);
    }

    /* ------------------------------------------------------------
     *  Boundary numeric conversions (overflow / underflow)
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("boundaryConversions")
    void shouldHandleBoundaryValues(String description, Object source, int expected) {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, identityConverter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    /* ------------------------------------------------------------
     *  Other supported numeric types
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("otherSupportedTypes")
    void shouldConvertOtherSupportedTypes(String description, Object source, int expected) {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, identityConverter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    /* ------------------------------------------------------------
     *  Custom converter behavior
     * ------------------------------------------------------------ */
    @Test
    void shouldUseCustomConverterStringToDouble() {
        // Converter that turns a String into a Double
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq("10"), eq(Long.class))).thenReturn(10L);
        when(mockConverter.convert(eq("10"), eq(Double.class))).thenReturn(10.0);
        AtomicInteger result = LocalTimeConversions.toAtomicInteger("10", mockConverter);
        assertNotNull(result);
        assertEquals(10, result.get(), "Custom string‑to‑double converter should yield 10");
    }

    @Test
    void shouldUseCustomConverterDoubleToInt() {
        // Converter that turns a String into an Integer directly
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(eq("20"), eq(Long.class))).thenReturn(20L);
        when(mockConverter.convert(eq("20"), eq(Integer.class))).thenReturn(20);
        AtomicInteger result = LocalTimeConversions.toAtomicInteger("20", mockConverter);
        assertNotNull(result);
        assertEquals(20, result.get(), "Custom double‑to‑int converter should yield 20");
    }

    @Test
    void shouldHandleNullReturningCustomConverter() {
        // Converter that always returns null
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.convert(any(), any())).thenReturn(null);
        AtomicInteger result = LocalTimeConversions.toAtomicInteger("any", mockConverter);
        assertNotNull(result);
        // Null is treated as 0 by the library’s toLong implementation
        assertEquals(0, result.get(), "Null‑returning converter should produce 0");
    }

    /* ------------------------------------------------------------
     *  String edge cases
     * ------------------------------------------------------------ */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringEdgeCases")
    void shouldHandleStringEdgeCases(String description, String source, int expected) {
        AtomicInteger result = LocalTimeConversions.toAtomicInteger(source, identityConverter);
        assertNotNull(result);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("stringEdgeCasesExceptions")
    void shouldThrowOnInvalidStringEdgeCases(String description, String source, Class<? extends Throwable> expectedException) {
        assertThrows(expectedException, () -> LocalTimeConversions.toAtomicInteger(source, identityConverter), description);
    }
}
