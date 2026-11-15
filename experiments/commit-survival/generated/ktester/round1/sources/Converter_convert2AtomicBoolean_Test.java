package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2AtomicBoolean_Test {

    /**
     * Keep a reference to the original singleton so we can restore it after the tests.
     */
    private static Converter originalInstance;

    /**
     * Mock that will replace the singleton during each test.
     */
    private Converter mockInstance;

    /**
     * Simple helper class used in the custom conversion test.
     */
    private static final class FailingConversionObject {
        // No state needed.
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Save the original instance once for the whole test suite.
        originalInstance = Converter.instance;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh mock for every test and inject it into the static field.
        mockInstance = mock(Converter.class);
        Converter.instance = mockInstance;
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear any interactions on the mock to avoid cross‑test contamination.
        clearInvocations(mockInstance);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original singleton so other test classes are not affected.
        Converter.instance = originalInstance;
    }

    /**
     * Provides a stream of successful conversion inputs together with the expected
     * {@code boolean} value of the resulting {@link AtomicBoolean}.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Boolean inputs
        Arguments.of(true, true, "Boolean true → true"), Arguments.of(false, false, "Boolean false → false"), // String inputs (case‑insensitive and synonyms)
        Arguments.of("true", true, "String \"true\" → true"), Arguments.of("FALSE", false, "String \"FALSE\" → false"), Arguments.of("yes", true, "String \"yes\" → true"), Arguments.of("no", false, "String \"no\" → false"), Arguments.of("randomText", false, "Unrecognised string defaults to false"), // Numeric inputs
        Arguments.of(1, true, "Integer 1 → true"), Arguments.of(0, false, "Integer 0 → false"), Arguments.of(-5, true, "Negative integer → true"), Arguments.of(0.0, false, "Double 0.0 → false"), Arguments.of(3.1415, true, "Non‑zero double → true"), Arguments.of(1234567890123L, true, "Non‑zero long → true"), // Atomic types
        Arguments.of(new AtomicBoolean(true), true, "AtomicBoolean(true) → true"), Arguments.of(new AtomicBoolean(false), false, "AtomicBoolean(false) → false"), Arguments.of(new AtomicInteger(7), true, "AtomicInteger(7) → true"), Arguments.of(new AtomicInteger(0), false, "AtomicInteger(0) → false"), Arguments.of(new AtomicLong(42L), true, "AtomicLong(42L) → true"), Arguments.of(new AtomicLong(0L), false, "AtomicLong(0L) → false"), // BigNumber inputs
        Arguments.of(BigDecimal.ZERO, false, "BigDecimal.ZERO → false"), Arguments.of(BigDecimal.ONE, true, "BigDecimal.ONE → true"), Arguments.of(BigInteger.ZERO, false, "BigInteger.ZERO → false"), Arguments.of(BigInteger.TEN, true, "BigInteger.TEN → true"));
    }

    /**
     * Provides inputs that should cause the conversion to throw an {@link IllegalArgumentException}.
     */
    private static Stream<Arguments> illegalArgumentProvider() {
        return Stream.of(Arguments.of("maybe", "String \"maybe\" is not a recognised boolean literal"), Arguments.of("2", "Numeric string \"2\" cannot be parsed as a boolean"), Arguments.of(new int[] { 1, 2, 3 }, "Primitive int[] is unsupported"), Arguments.of(new String[] { "true", "false" }, "Object array String[] is unsupported"), Arguments.of(DayOfWeek.MONDAY, "Enum DayOfWeek.MONDAY is unsupported"), Arguments.of(new AtomicReference<>(Boolean.TRUE), "AtomicReference is unsupported"));
    }

    /**
     * Provides inputs that should cause a {@link NumberFormatException}.
     */
    private static Stream<Arguments> numberFormatProvider() {
        return Stream.of(Arguments.of("123abc", "String \"123abc\" contains non‑numeric characters"));
    }

    @Test
    void convert2AtomicBoolean_nullInput_returnsFalseAtomicBoolean() {
        // GIVEN – a null argument (no special setup required)
        // WHEN
        AtomicBoolean result = Converter.convert2AtomicBoolean(null);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertFalse(result.get(), "AtomicBoolean created from null should be false");
        // No interaction with the mock should have happened because the method returns early.
        verifyNoInteractions(mockInstance);
    }

    @Test
    void convert2AtomicBoolean_nonNullInput_delegatesToInstanceConvert() {
        // GIVEN
        Object source = "true";
        AtomicBoolean expected = new AtomicBoolean(true);
        when(mockInstance.convert(source, AtomicBoolean.class)).thenReturn(expected);
        // WHEN
        AtomicBoolean actual = Converter.convert2AtomicBoolean(source);
        // THEN
        assertSame(expected, actual, "The method should return exactly what the mock instance provides");
        verify(mockInstance, times(1)).convert(source, AtomicBoolean.class);
    }

    @Test
    void convert2AtomicBoolean_otherTypes_delegatesCorrectly() {
        // GIVEN
        // Integer should be convertible to AtomicBoolean via the real implementation,
        Object source = 1;
        // but we are only checking delegation.
        AtomicBoolean expected = new AtomicBoolean(true);
        when(mockInstance.convert(source, AtomicBoolean.class)).thenReturn(expected);
        // WHEN
        AtomicBoolean actual = Converter.convert2AtomicBoolean(source);
        // THEN
        assertSame(expected, actual);
        verify(mockInstance).convert(source, AtomicBoolean.class);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("successfulConversionProvider")
    void convert2AtomicBoolean_successfulConversions(Object input, boolean expectedValue, String description) {
        // Replace the mock with the real instance for this test.
        Converter.instance = originalInstance;
        try {
            AtomicBoolean result = Converter.convert2AtomicBoolean(input);
            assertNotNull(result, "Result must not be null");
            assertEquals(expectedValue, result.get(), () -> "Conversion of " + description + " produced unexpected value");
        } finally {
            // Reinstate the mock for the remaining tests.
            Converter.instance = mockInstance;
        }
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("illegalArgumentProvider")
    void convert2AtomicBoolean_illegalArgumentExceptions(Object input, String description) {
        // Use the real converter for these cases.
        Converter.instance = originalInstance;
        try {
            assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicBoolean(input), () -> "Expected IllegalArgumentException for " + description);
        } finally {
            Converter.instance = mockInstance;
        }
    }

    @ParameterizedTest(name = "[{index}] {1}")
    @MethodSource("numberFormatProvider")
    void convert2AtomicBoolean_numberFormatExceptions(Object input, String description) {
        Converter.instance = originalInstance;
        try {
            assertThrows(NumberFormatException.class, () -> Converter.convert2AtomicBoolean(input), () -> "Expected NumberFormatException for " + description);
        } finally {
            Converter.instance = mockInstance;
        }
    }

    /**
     * Test that a custom conversion function that throws a {@link RuntimeException}
     * propagates the exception.
     */
    @Test
    void convert2AtomicBoolean_customConversionThrowsRuntimeException() {
        // Register a conversion that deliberately throws.
        Converter.instance = originalInstance;
        try {
            Converter.instance.addConversion(FailingConversionObject.class, AtomicBoolean.class, (src) -> {
                throw new RuntimeException("Deliberate failure");
            });
            FailingConversionObject obj = new FailingConversionObject();
            RuntimeException ex = assertThrows(RuntimeException.class, () -> Converter.convert2AtomicBoolean(obj), "Custom conversion should propagate RuntimeException");
            assertEquals("Deliberate failure", ex.getMessage());
        } finally {
            // Clean‑up: remove the custom conversion to avoid side‑effects on other tests.
            // The library does not expose a direct removal API, so we simply restore the original instance.
            Converter.instance = mockInstance;
        }
    }
}
