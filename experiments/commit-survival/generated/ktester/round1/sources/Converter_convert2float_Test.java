package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.CommonValues;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2float_Test {

    /**
     * Holds the original singleton instance so we can restore it after each test.
     */
    private static Converter originalInstance;

    /**
     * Mocked Converter used to verify delegation.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Preserve the real instance and replace it with a mock.
        originalInstance = Converter.instance;
        mockConverter = mock(Converter.class);
        // By default, make the mock return a deterministic float value.
        when(mockConverter.convert(any(), eq(float.class))).thenReturn(1.23f);
        Converter.instance = mockConverter;
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original singleton instance so other tests are not affected.
        Converter.instance = originalInstance;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// primitive numeric types
        Arguments.of(42, 42.0f), Arguments.of(3.14, 3.14f), Arguments.of((short) -7, -7.0f), Arguments.of((byte) 127, 127.0f), Arguments.of(Float.valueOf(5.5f), 5.5f), // large numeric types
        Arguments.of(Long.MAX_VALUE, 9.223372E18f), // rounded to nearest float
        Arguments.of("12345.6789", 12345.679f), Arguments.of("9876543210", 9.876543E9f), // boolean conversions
        Arguments.of(Boolean.TRUE, 1.0f), Arguments.of(Boolean.FALSE, 0.0f), // character conversion (String containing a single char)
        Arguments.of("A", 65.0f), // special float strings
        Arguments.of("NaN", Float.NaN), Arguments.of("Infinity", Float.POSITIVE_INFINITY), Arguments.of("-Infinity", Float.NEGATIVE_INFINITY), // whitespace handling
        Arguments.of("  10  ", 10.0f));
    }

    private static Stream<Arguments> provideInvalidConversions() {
        return Stream.of(// empty string
        Arguments.of(""), // non‑numeric string
        Arguments.of("abc123"), // unsupported source type (plain map)
        Arguments.of(Collections.emptyMap()), // array input
        Arguments.of(new int[] { 1, 2, 3 }), // collection input
        Arguments.of(Arrays.asList(1, 2, 3)), // date input (represented as a Date instance)
        Arguments.of(new Date()), // BigDecimal input
        Arguments.of(new BigDecimal("12345.6789")), // Character object (not a String)
        Arguments.of(Character.valueOf('B')), // malformed numeric string with internal whitespace
        Arguments.of("1 0"), // malformed numeric string with commas
        Arguments.of("1,000"));
    }

    @Test
    void convert2float_WithNull_ReturnsLogicalZero() {
        // Replace the singleton with a Mockito mock.
        mockConverter = mock(Converter.class);
        Converter.instance = mockConverter;
        // GIVEN a null input
        Object input = null;
        // WHEN converting to float
        float result = Converter.convert2float(input);
        // THEN the result should be the logical zero for float
        assertEquals(CommonValues.FLOAT_ZERO, result, "convert2float(null) should return CommonValues.FLOAT_ZERO");
        // No interaction with the mock should have occurred because the method returns early.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void convert2float_WithNonNull_DelegatesToInstanceConvert() {
        // GIVEN a non‑null input value
        String input = "123.45";
        // WHEN converting to float
        float result = Converter.convert2float(input);
        // THEN the result should be whatever the mock instance returns
        assertEquals(1.23f, result, "convert2float should delegate to Converter.instance.convert and return its value");
        // AND the mock's convert method should have been called exactly once with the correct arguments
        verify(mockConverter, times(1)).convert(eq(input), eq(float.class));
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for successful conversions.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] convert2float({0}) => {1}")
    @MethodSource("provideSuccessfulConversions")
    void convert2float_SuccessfulConversions(Object input, float expected) {
        // Use a real Converter instance for actual conversion.
        Converter.instance = new Converter(new DefaultConverterOptions());
        float actual = Converter.convert2float(input);
        if (Float.isNaN(expected)) {
            assertTrue(Float.isNaN(actual), "Result should be NaN");
        } else if (Float.isInfinite(expected)) {
            assertEquals(expected, actual, "Result should be infinite");
        } else {
            // Use a small delta for floating‑point comparison.
            assertEquals(expected, actual, 0.0001f, () -> "convert2float(" + input + ") expected " + expected + " but got " + actual);
        }
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for expected IllegalArgumentException.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] convert2float({0}) throws IAE")
    @MethodSource("provideInvalidConversions")
    void convert2float_InvalidInputs_ThrowsIllegalArgumentException(Object input) {
        // Use a real Converter instance for actual conversion.
        Converter.instance = new Converter(new DefaultConverterOptions());
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2float(input), () -> "convert2float(" + input + ") should throw IllegalArgumentException");
    }
}
