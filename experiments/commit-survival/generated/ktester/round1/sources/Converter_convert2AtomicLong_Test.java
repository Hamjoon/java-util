package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2AtomicLong_Test {

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
        // Save the real singleton instance.
        originalInstance = Converter.instance;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Replace the static instance with a Mockito mock.
        Converter.instance = mockConverter;
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock for the next test (optional, but keeps interactions clean).
        reset(mockConverter);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the real singleton instance so other test classes are not affected.
        Converter.instance = originalInstance;
    }

    /**
     * Provides a stream of successful conversion cases.
     *
     * Each argument consists of:
     * <ul>
     *   <li>the source object supplied to {@code convert2AtomicLong}</li>
     *   <li>the expected long value inside the resulting {@link AtomicLong}</li>
     * </ul>
     */
    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// Numeric primitive wrappers
        Arguments.of(0, 0L), Arguments.of(42, 42L), Arguments.of(1_234_567_890_123L, 1_234_567_890_123L), // truncation
        Arguments.of(3.14, 3L), Arguments.of(790.0, 790L), // String representations
        Arguments.of("0", 0L), Arguments.of("-15", -15L), Arguments.of("9876543210", 9_876_543_210L), Arguments.of("  42  ", 42L), // Atomic types
        Arguments.of(new AtomicLong(12_345L), 12_345L), Arguments.of(new AtomicInteger(678), 678L), Arguments.of(new AtomicBoolean(true), 1L), // Big number types (values that fit into a long)
        Arguments.of(new BigInteger("1234567890123456789"), 1_234_567_890_123_456_789L), Arguments.of(new BigDecimal("-98765.4321"), -98_765L));
    }

    /**
     * Provides a stream of inputs that should cause {@code convert2AtomicLong}
     * to throw {@link IllegalArgumentException}.
     *
     * Each argument is the source object that triggers the failure.
     */
    private static Stream<Arguments> provideFailureConversions() {
        return Stream.of(// Non‑numeric strings
        Arguments.of("not-a-number"), // Unsupported object types
        Arguments.of(new Object()), Arguments.of(new java.util.Date()), // Overflow scenarios
        // Long.MAX_VALUE + 1
        Arguments.of(new BigInteger("9223372036854775808")), Arguments.of(new BigDecimal("9223372036854775808.123")), Arguments.of("9223372036854775808"), // Invalid numeric values
        Arguments.of(Double.NaN), Arguments.of(Double.POSITIVE_INFINITY), Arguments.of(Float.NEGATIVE_INFINITY), // Temporal types
        Arguments.of(java.time.LocalDate.now()), Arguments.of(java.time.LocalDateTime.now()), Arguments.of(java.time.ZonedDateTime.now()), Arguments.of(java.util.Calendar.getInstance()), Arguments.of(new java.sql.Timestamp(System.currentTimeMillis())), // Primitive booleans and characters (wrapped as objects)
        Arguments.of(Boolean.TRUE), Arguments.of(Boolean.FALSE), Arguments.of('A'));
    }

    @Test
    void convert2AtomicLong_nullInput_returnsZeroAtomicLong() {
        // Act
        AtomicLong result = Converter.convert2AtomicLong(null);
        // Assert
        assertNotNull(result, "Result should never be null");
        assertEquals(0L, result.get(), "Null input must produce an AtomicLong with value 0");
        // No delegation to the mock should happen for a null argument.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void convert2AtomicLong_nonNullInput_delegatesToInstanceConvert() {
        // Arrange
        Object source = "12345";
        AtomicLong expected = new AtomicLong(12_345L);
        when(mockConverter.convert(source, AtomicLong.class)).thenReturn(expected);
        // Act
        AtomicLong actual = Converter.convert2AtomicLong(source);
        // Assert
        assertSame(expected, actual, "The method should return exactly what the mock Converter provides");
        verify(mockConverter, times(1)).convert(source, AtomicLong.class);
    }

    @Test
    void convert2AtomicLong_nonNullInput_convertReturnsNull_returnsNull() {
        // Arrange
        Object source = new Object();
        when(mockConverter.convert(source, AtomicLong.class)).thenReturn(null);
        // Act
        AtomicLong actual = Converter.convert2AtomicLong(source);
        // Assert
        assertNull(actual, "If the underlying Converter returns null, the static method should propagate null");
        verify(mockConverter, times(1)).convert(source, AtomicLong.class);
    }

    @ParameterizedTest(name = "[{index}] convert2AtomicLong({0}) → {1}")
    @MethodSource("provideSuccessfulConversions")
    void convert2AtomicLong_successfulConversions(Object source, long expectedValue) {
        // Arrange: mock the conversion to return the expected AtomicLong
        AtomicLong expected = new AtomicLong(expectedValue);
        when(mockConverter.convert(source, AtomicLong.class)).thenReturn(expected);
        // Act
        AtomicLong actual = Converter.convert2AtomicLong(source);
        // Assert
        assertNotNull(actual, "Result should never be null for successful conversion");
        assertEquals(expectedValue, actual.get(), () -> "Converted value should match expected for input: " + source);
        // Verify that the mock was called exactly once with the correct arguments
        verify(mockConverter, times(1)).convert(source, AtomicLong.class);
    }

    @ParameterizedTest(name = "[{index}] convert2AtomicLong({0}) throws IllegalArgumentException")
    @MethodSource("provideFailureConversions")
    void convert2AtomicLong_failureConversions(Object source) {
        // Arrange: mock the conversion to throw the expected exception
        when(mockConverter.convert(source, AtomicLong.class)).thenThrow(new IllegalArgumentException("Conversion not supported for: " + source));
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicLong(source), () -> "Expected IllegalArgumentException for input: " + source);
        // Verify that the mock was invoked exactly once
        verify(mockConverter, times(1)).convert(source, AtomicLong.class);
    }
}
