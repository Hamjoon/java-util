package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class NumberConversions_isIntTypeNotZero_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance; using the default constructor
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
        converter = null;
    }

    /* -------------------------------------------------------------
     *  Helper to obtain the appropriate Converter instance.
     * ------------------------------------------------------------- */
    private Converter getConverter(String type) {
        if ("faultyConverter".equals(type)) {
            // A mock that throws ConversionException for any conversion attempt
            Converter mock = mock(Converter.class);
            when(mock.convert(any(), any())).thenThrow(new ConversionException("forced failure"));
            return mock;
        }
        if ("null".equals(type)) {
            // deliberately passing a null converter
            return null;
        }
        // For "default" and "stringToNumberConverter" we use the real instance.
        // The default Converter already handles string‑to‑number conversion.
        return converter;
    }

    /* -------------------------------------------------------------
     *  Data providers – boolean results.
     * ------------------------------------------------------------- */
    private static Stream<Arguments> provideBooleanCases() {
        return Stream.of(// Primitive wrappers and primitives
        Arguments.of(5, "default", true), Arguments.of(0, "default", false), Arguments.of(10L, "default", true), Arguments.of(0L, "default", false), // String parsing (default converter)
        Arguments.of("123", "default", true), Arguments.of("0", "default", false), // String parsing (explicit string converter – same behaviour)
        Arguments.of("123", "stringToNumberConverter", true), Arguments.of("0", "stringToNumberConverter", false), // BigInteger
        Arguments.of(BigInteger.ZERO, "default", false), Arguments.of(new BigInteger("42"), "default", true), // AtomicInteger
        Arguments.of(new AtomicInteger(0), "default", false), Arguments.of(new AtomicInteger(-7), "default", true), // Null source
        Arguments.of(null, "default", false), // Negative numbers
        Arguments.of(-5, "default", true), Arguments.of(-10L, "default", true), // Short wrapper
        Arguments.of((short) 0, "default", false), Arguments.of((short) 7, "default", true), // Byte wrapper
        Arguments.of((byte) 0, "default", false), Arguments.of((byte) 8, "default", true), // Float values (truncation semantics)
        Arguments.of(0.0f, "default", false), Arguments.of(0.5f, "default", false), Arguments.of(1.2f, "default", true), // Double values (truncation semantics)
        Arguments.of(0.0d, "default", false), Arguments.of(-2.9d, "default", true), // BigDecimal values
        Arguments.of(BigDecimal.ZERO, "default", false), Arguments.of(new BigDecimal("123.456"), "default", true), // AtomicLong
        Arguments.of(new AtomicLong(0L), "default", false), Arguments.of(new AtomicLong(1234567890123L), "default", true), // Strings with whitespace / sign
        Arguments.of("  7 ", "stringToNumberConverter", true), Arguments.of("+5", "stringToNumberConverter", true), // Boolean handling
        Arguments.of(Boolean.TRUE, "default", true), Arguments.of(Boolean.FALSE, "default", false));
    }

    /* -------------------------------------------------------------
     *  Data providers – expected exceptions.
     * ------------------------------------------------------------- */
    private static Stream<Arguments> provideExceptionCases() {
        return Stream.of(// Faulty converter that always throws
        Arguments.of(9999999999L, "faultyConverter", ConversionException.class), // Invalid string inputs
        Arguments.of("", "stringToNumberConverter", ConversionException.class), Arguments.of("abc", "stringToNumberConverter", ConversionException.class), // Null converter should cause NPE inside NumberConversions.toLong
        Arguments.of(42, "null", NullPointerException.class), // Unsupported source types
        Arguments.of(new int[] { 1, 2, 3 }, "default", ConversionException.class), Arguments.of(new Object(), "default", ConversionException.class), // Out‑of‑range numeric values
        Arguments.of("9223372036854775808", "stringToNumberConverter", ConversionException.class), Arguments.of(new BigInteger("9223372036854775808"), "default", ConversionException.class), Arguments.of(new BigDecimal("9223372036854775808.5"), "default", ConversionException.class), // Special floating‑point values
        Arguments.of(Float.NaN, "default", ConversionException.class), Arguments.of(Double.POSITIVE_INFINITY, "default", ConversionException.class), // Non‑numeric object types
        // character
        Arguments.of('A', "default", ConversionException.class), Arguments.of(Duration.parse("PT1H"), "default", ConversionException.class));
    }

    @Test
    void returnsTrueWhenIntegerIsNonZero() {
        // given
        Integer source = 42;
        // when
        boolean result = NumberConversions.isIntTypeNotZero(source, converter);
        // then
        assertTrue(result, "Non‑zero Integer should be reported as not zero");
    }

    @Test
    void returnsFalseWhenIntegerIsZero() {
        // given
        Integer source = 0;
        // when
        boolean result = NumberConversions.isIntTypeNotZero(source, converter);
        // then
        assertFalse(result, "Zero Integer should be reported as zero");
    }

    @Test
    void returnsTrueWhenStringRepresentsNonZeroNumber() {
        // given
        String source = "12345";
        // when
        boolean result = NumberConversions.isIntTypeNotZero(source, converter);
        // then
        assertTrue(result, "String representing a non‑zero number should be reported as not zero");
    }

    @Test
    void returnsFalseWhenStringRepresentsZero() {
        // given
        String source = "0";
        // when
        boolean result = NumberConversions.isIntTypeNotZero(source, converter);
        // then
        assertFalse(result, "String representing zero should be reported as zero");
    }

    @Test
    void returnsFalseWhenSourceIsNull() {
        // given
        Object source = null;
        // when
        boolean result = NumberConversions.isIntTypeNotZero(source, converter);
        // then
        assertFalse(result, "Null source should be treated as zero");
    }

    @Test
    void returnsTrueForNegativeNonZeroNumber() {
        // given
        Long source = -7L;
        // when
        boolean result = NumberConversions.isIntTypeNotZero(source, converter);
        // then
        assertTrue(result, "Negative non‑zero number should be reported as not zero");
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for successful boolean outcomes.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} ({1}) → expected={2}")
    @MethodSource("provideBooleanCases")
    void testIsIntTypeNotZeroReturns(Object from, String converterType, boolean expected) {
        Converter conv = getConverter(converterType);
        boolean result = NumberConversions.isIntTypeNotZero(from, conv);
        assertEquals(expected, result, () -> String.format("Unexpected result for source=%s, converter=%s", from, converterType));
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for expected exceptions.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} ({1}) → throws {2}")
    @MethodSource("provideExceptionCases")
    void testIsIntTypeNotZeroThrows(Object from, String converterType, Class<? extends Throwable> expectedException) {
        Converter conv = getConverter(converterType);
        assertThrows(expectedException, () -> NumberConversions.isIntTypeNotZero(from, conv), () -> String.format("Expected %s for source=%s, converter=%s", expectedException.getSimpleName(), from, converterType));
    }
}
