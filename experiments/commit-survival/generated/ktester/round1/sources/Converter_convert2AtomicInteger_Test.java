package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cedarsoftware.util.convert.ConversionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2AtomicInteger_Test {

    /**
     * Keep a reference to the original singleton so we can restore it after all tests.
     */
    private static Converter originalInstance;

    /**
     * Mock that will replace the static {@code Converter.instance} during each test.
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Save the original instance so we can put it back later.
        originalInstance = Converter.instance;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Replace the static singleton with the Mockito mock.
        Converter.instance = mockConverter;
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear any stubbings / interactions for the next test.
        clearInvocations(mockConverter);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original singleton to avoid side‑effects for other test classes.
        Converter.instance = originalInstance;
    }

    /**
     * Provides all *valid* conversion scenarios extracted from the JSON test cases.
     * Each argument consists of:
     * <ul>
     *   <li>the input object</li>
     *   <li>the expected int value wrapped in an {@link AtomicInteger}</li>
     *   <li>a human‑readable description (used as the test name)</li>
     * </ul>
     */
    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Integer → AtomicInteger
        Arguments.of(42, 42, "Integer 42 should be converted to AtomicInteger(42)"), // Numeric String → AtomicInteger
        Arguments.of("123", 123, "String \"123\" should be parsed to AtomicInteger(123)"), // Double with zero fractional part → AtomicInteger
        Arguments.of(7.0, 7, "Double 7.0 should be truncated to AtomicInteger(7)"), // Boolean true → AtomicInteger(1)
        Arguments.of(Boolean.TRUE, 1, "Boolean true maps to AtomicInteger(1)"), // Boolean false → AtomicInteger(0)
        Arguments.of(Boolean.FALSE, 0, "Boolean false maps to AtomicInteger(0)"), // Long exceeding Integer.MAX_VALUE → lower 32‑bits (standard Java cast)
        Arguments.of(1234567890123L, (int) 1234567890123L, "Large Long should be cast to lower 32‑bits"), // String with surrounding whitespace → trimmed before parsing
        Arguments.of("   55   ", 55, "Whitespace‑padded String should be trimmed and parsed"), // Boundary values
        Arguments.of(Integer.MAX_VALUE, Integer.MAX_VALUE, "Integer.MAX_VALUE should be preserved"), Arguments.of(Integer.MIN_VALUE, Integer.MIN_VALUE, "Integer.MIN_VALUE should be preserved"), // Long just above Integer.MAX_VALUE → wraps to Integer.MIN_VALUE
        Arguments.of(2147483648L, (int) 2147483648L, "Long 2147483648 wraps to Integer.MIN_VALUE"), // Long just below Integer.MIN_VALUE → wraps to Integer.MAX_VALUE
        Arguments.of(-2147483649L, (int) -2147483649L, "Long -2147483649 wraps to Integer.MAX_VALUE"));
    }

    /**
     * Provides all *invalid* conversion scenarios extracted from the JSON test cases.
     * Each argument consists of:
     * <ul>
     *   <li>the input object that cannot be converted</li>
     *   <li>a description for the test name</li>
     * </ul>
     */
    private static Stream<Arguments> invalidConversionProvider() {
        return Stream.of(Arguments.of(new Object(), "Arbitrary Object should trigger ConversionException"), Arguments.of("not-a-number", "Non‑numeric String should trigger ConversionException"), Arguments.of(3.14159, "Non‑integral Double should trigger ConversionException"));
    }

    @Test
    void convert2AtomicInteger_NullInput_ReturnsZeroAtomicInteger() {
        // GIVEN a null input
        Object input = null;
        // WHEN the conversion method is invoked
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // THEN a non‑null AtomicInteger with value 0 is returned
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.get(), "AtomicInteger should represent logical zero for null input");
        // AND the underlying Converter instance should not be consulted
        verifyNoInteractions(mockConverter);
    }

    @Test
    void convert2AtomicInteger_NonNullInput_DelegatesToInstanceConvert() {
        // GIVEN a non‑null input object
        Object input = "42";
        AtomicInteger expected = new AtomicInteger(42);
        // Stub the mock to return the expected value when asked to convert.
        when(mockConverter.convert(input, AtomicInteger.class)).thenReturn(expected);
        // WHEN the conversion method is invoked
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // THEN the result from the mock is returned unchanged
        assertSame(expected, result, "Result should be the exact instance returned by the mock");
        // AND the mock's convert method was called with the correct arguments
        verify(mockConverter, times(1)).convert(input, AtomicInteger.class);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void convert2AtomicInteger_ValidConversions(Object input, int expectedValue, String description) {
        // Arrange: mock the underlying conversion to return a pre‑created AtomicInteger.
        AtomicInteger expectedAtomic = new AtomicInteger(expectedValue);
        when(mockConverter.convert(input, AtomicInteger.class)).thenReturn(expectedAtomic);
        // Act
        AtomicInteger result = Converter.convert2AtomicInteger(input);
        // Assert: the mock's instance is returned unchanged and contains the expected value.
        assertSame(expectedAtomic, result, "Result should be the exact instance returned by the mock for: " + description);
        assertEquals(expectedValue, result.get(), "AtomicInteger should hold the expected int value for: " + description);
        // Verify interaction
        verify(mockConverter, times(1)).convert(input, AtomicInteger.class);
        // Clean up for the next parameterized invocation
        clearInvocations(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("invalidConversionProvider")
    void convert2AtomicInteger_InvalidConversions(Object input, String description) {
        // Arrange: mock the underlying conversion to throw the library's ConversionException.
        when(mockConverter.convert(input, AtomicInteger.class)).thenThrow(new ConversionException("Invalid conversion for test case"));
        // Act & Assert
        assertThrows(ConversionException.class, () -> Converter.convert2AtomicInteger(input), "Expected ConversionException for: " + description);
        // Verify that the mock was consulted exactly once.
        verify(mockConverter, times(1)).convert(input, AtomicInteger.class);
        clearInvocations(mockConverter);
    }
}
