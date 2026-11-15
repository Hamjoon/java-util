package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2char_Test {

    /**
     * Holds the original static instance so we can restore it after each test.
     */
    private static Converter originalInstance;

    /**
     * Mock that will replace the static {@code Converter.instance}.
     */
    private Converter mockInstance;

    @BeforeEach
    void setupBeforeEach() {
        // Save the real instance and replace it with a Mockito mock.
        originalInstance = Converter.instance;
        mockInstance = mock(Converter.class);
        Converter.instance = mockInstance;
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original static instance so other tests are not affected.
        Converter.instance = originalInstance;
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Direct Character
        Arguments.of(Character.valueOf('a'), 'a'), Arguments.of(Character.valueOf('\u0000'), '\u0000'), // Single‑character Strings
        Arguments.of("Z", 'Z'), Arguments.of("\u03A9", '\u03A9'), // Number conversions
        // 65 → 'A'
        Arguments.of(Integer.valueOf(65), 'A'), // 97 → 'a'
        Arguments.of(Long.valueOf(97L), 'a'), // 48 → '0'
        Arguments.of(Byte.valueOf((byte) 48), '0'), // 0 → null char
        Arguments.of(Short.valueOf((short) 0), '\u0000'), // 66 → 'B'
        Arguments.of(new BigDecimal("66"), 'B'), // 67 → 'C'
        Arguments.of(new BigInteger("67"), 'C'), // Boolean conversions
        // true → char 1
        Arguments.of(Boolean.TRUE, '\u0001'), // false → char 0
        Arguments.of(Boolean.FALSE, '\u0000'), // Edge numeric values (overflow handling)
        // 0xFFFF → '\uffff'
        Arguments.of(Integer.valueOf(65535), '\uffff'), // -1 → unsigned 0xFFFF
        Arguments.of(Long.valueOf(-1L), '\uffff'));
    }

    private static Stream<Arguments> exceptionConversionProvider() {
        return Stream.of(// String length > 1 or empty
        Arguments.of("ab"), Arguments.of(""), // Unsupported types
        Arguments.of(new Date()), Arguments.of(new ArrayList<>()), // Unsupported CharSequence implementations
        Arguments.of(new StringBuilder("a")), Arguments.of(new StringBuilder("ab")), // Fractional numbers
        Arguments.of(Float.valueOf(65.5f)), Arguments.of(Double.valueOf(97.9)), Arguments.of(new BigDecimal("66.1")), // NaN / Infinity
        Arguments.of(Float.NaN), Arguments.of(Float.POSITIVE_INFINITY), Arguments.of(Double.NEGATIVE_INFINITY), // Unsupported object types
        Arguments.of(new HashMap<>()), Arguments.of(new HashSet<>()), Arguments.of(new AtomicInteger(5)), // Enum conversion
        Arguments.of(TestEnum.A));
    }

    @Test
    void convert2char_NullInput_ReturnsZeroChar() {
        // Act
        char result = Converter.convert2char(null);
        // Assert
        assertEquals('\0', result, "When the input is null the method should return the logical zero char.");
        // Verify that the mock was never invoked because the method should short‑circuit.
        verifyNoInteractions(mockInstance);
    }

    @Test
    void convert2char_NonNullInput_DelegatesToInstanceConvert() {
        // Arrange
        Object source = "any value";
        char expected = 'Z';
        when(mockInstance.convert(eq(source), eq(char.class))).thenReturn(expected);
        // Act
        char result = Converter.convert2char(source);
        // Assert
        assertEquals(expected, result, "The returned char should be whatever the mocked convert method provides.");
        // Verify that the static instance's convert method was called exactly once with the correct arguments.
        verify(mockInstance, times(1)).convert(eq(source), eq(char.class));
    }

    @ParameterizedTest(name = "[{index}] convert2char({0}) → ''{1}''")
    @MethodSource("successfulConversionProvider")
    void convert2char_SuccessfulConversions(Object input, char expected) {
        // Mock the conversion to return the expected char.
        when(mockInstance.convert(eq(input), eq(char.class))).thenReturn(expected);
        // Act
        char result = Converter.convert2char(input);
        // Assert
        assertEquals(expected, result, () -> "Expected conversion of " + input + " to be '" + expected + "'");
        verify(mockInstance, times(1)).convert(eq(input), eq(char.class));
    }

    @ParameterizedTest(name = "[{index}] convert2char({0}) throws IllegalArgumentException")
    @MethodSource("exceptionConversionProvider")
    void convert2char_IllegalArgumentExceptions(Object input) {
        // Mock the conversion to throw IllegalArgumentException for the given input.
        when(mockInstance.convert(eq(input), eq(char.class))).thenThrow(new IllegalArgumentException("Unsupported conversion"));
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2char(input), () -> "Expected IllegalArgumentException for input: " + input);
        verify(mockInstance, times(1)).convert(eq(input), eq(char.class));
    }
}
