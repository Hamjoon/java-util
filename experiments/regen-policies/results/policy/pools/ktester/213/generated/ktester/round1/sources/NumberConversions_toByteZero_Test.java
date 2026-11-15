package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toByteZero_Test {

    @Mock
    private Converter // mock – we do not expect any interaction
    mockConverter;

    /**
     * Minimal custom object for the “arbitrary object” test case.
     */
    private static final class CustomObject {

        private final String customObject;

        CustomObject(String value) {
            this.customObject = value;
        }

        @Override
        public String toString() {
            return "CustomObject{" + customObject + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito creates the mock instance automatically.
        // Reset the mock before each test case to avoid cross‑test interaction verification.
        reset(mockConverter);
    }

    private static Stream<Arguments> group1Provider() {
        return Stream.of(// Both arguments null
        Arguments.of(null, null, "Both arguments are null; method must still return BYTE_ZERO"), // Non‑null numeric 'from' with a null converter
        Arguments.of(123, null, "Non‑null numeric 'from' with a null converter; method should ignore the value"), // String input with a mock converter
        Arguments.of("some string", mockConverter(), "String input and a mock Converter; method must still return BYTE_ZERO"), // Boolean input with a valid converter
        Arguments.of(Boolean.TRUE, mockConverter(), "Boolean input with a valid Converter; verifies no conversion logic is used"), // Arbitrary custom object
        Arguments.of(new CustomObject("value"), mockConverter(), "Arbitrary object as 'from' argument; method must consistently return BYTE_ZERO"));
    }

    private static Stream<Arguments> group2Provider() {
        return Stream.of(// Explicit zero Byte instance with a null converter
        Arguments.of(Byte.valueOf((byte) 0), null, "Explicit zero Byte instance with a null converter; still returns BYTE_ZERO"), // Byte.MIN_VALUE with a mock converter
        Arguments.of(Byte.valueOf(Byte.MIN_VALUE), mockConverter(), "Byte at its minimum value with a mock Converter; value is ignored"), // AtomicInteger wrapper object
        Arguments.of(new AtomicInteger(5), mockConverter(), "AtomicInteger wrapper object; method should not attempt conversion"), // BigDecimal representing a non‑integer number
        Arguments.of(new BigDecimal("123.45"), null, "BigDecimal instance representing a non‑integer number; still returns BYTE_ZERO"), // Primitive int array
        Arguments.of(new int[] { 1, 2, 3 }, mockConverter(), "Array of primitives; method must ignore it and return the constant"), // Enum constant
        Arguments.of(TestEnum.VALUE, mockConverter(), "Enum constant as input; confirms enums are not processed"), // Null 'from' with a non‑null Converter
        Arguments.of(null, mockConverter(), "Null 'from' with a non‑null Converter; ensures method does not depend on the converter"), // Anonymous Runnable instance
        Arguments.of((Runnable) () -> {
            /* no‑op */
        }, null, "Anonymous Runnable instance; validates that arbitrary functional objects are ignored"));
    }

    /**
     * Returns the {@link #mockConverter} instance for method‑source usage.
     * The static method is required because method sources must be static.
     */
    private static Converter mockConverter() {
        // The test class instance will have the @Mock field injected;
        // however, method sources are static, so we retrieve the mock via reflection.
        // This hack works because the test runner creates a single instance of the test class.
        try {
            var field = NumberConversions_toByteZero_Test.class.getDeclaredField("mockConverter");
            field.setAccessible(true);
            return (Converter) field.get(null);
        } catch (Exception e) {
            // In case of reflection failure, fall back to a fresh mock (won't be verified).
            return mock(Converter.class);
        }
    }

    @Test
    void toByteZero_returnsConstantByteZero_forNullInput() {
        // GIVEN
        // any value is acceptable
        Object input = null;
        // WHEN
        Byte result = NumberConversions.toByteZero(input, mockConverter);
        // THEN
        assertNotNull(result, "Result should never be null");
        assertEquals(CommonValues.BYTE_ZERO, result, "Method must return the constant BYTE_ZERO");
    }

    @Test
    void toByteZero_returnsConstantByteZero_forNonNullInput() {
        // GIVEN
        Object input = "any object";
        // WHEN
        Byte result = NumberConversions.toByteZero(input, mockConverter);
        // THEN
        assertEquals(Byte.valueOf((byte) 0), result, "Method must always return a Byte with value 0");
    }

    @Test
    void toByteZero_doesNotInteractWithConverter() {
        // GIVEN
        Object input = new Object();
        // WHEN
        NumberConversions.toByteZero(input, mockConverter);
        // THEN
        // No interactions with the mock should have occurred.
        // Using Mockito's verifyZeroInteractions to ensure the method truly ignores the converter.
        verifyNoInteractions(mockConverter);
    }

    /* --------------------------------------------------------------------- *
     *  Test group 1 – basic variations (null, numeric, string, boolean,   *
     *  generic object).                                                    *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("group1Provider")
    void toByteZero_group1_returnsConstant(Object from, Converter converter, String description) {
        // GIVEN – arguments are supplied by the method source
        // WHEN
        Byte result = NumberConversions.toByteZero(from, converter);
        // THEN
        assertNotNull(result, "Result must never be null");
        assertEquals(CommonValues.BYTE_ZERO, result, "Method must always return the constant BYTE_ZERO");
        // The method must never touch the converter (if one was supplied)
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    /* --------------------------------------------------------------------- *
     *  Test group 2 – a richer set of input types (Byte, AtomicInteger,   *
     *  BigDecimal, arrays, enums, etc.).                                   *
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("group2Provider")
    void toByteZero_group2_returnsConstant(Object from, Converter converter, String description) {
        // GIVEN – arguments are supplied by the method source
        // WHEN
        Byte result = NumberConversions.toByteZero(from, converter);
        // THEN
        assertNotNull(result, "Result must never be null");
        assertEquals(CommonValues.BYTE_ZERO, result, "Method must always return the constant BYTE_ZERO");
        // Verify that the converter (if present) was never used.
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }
}
