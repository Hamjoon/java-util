package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toIntZero_Test {

    /**
     * A mock {@link Converter}. The method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid any accidental interaction leakage between tests.
        // (Not strictly necessary for this stateless test, but good practice.)
        org.mockito.Mockito.reset(mockConverter);
    }

    /**
     * Supplies the arguments for {@link #toIntZero_AlwaysReturnsZero(String, Object, Converter)}.
     *
     * The data mirrors the JSON test cases:
     * <ul>
     *   <li>both parameters null</li>
     *   <li>String input with a mock converter</li>
     *   <li>Integer input with a real {@code DefaultConverter}</li>
     *   <li>Custom object with a null converter</li>
     * </ul>
     *
     * @return a stream of arguments for the parameterized test
     */
    private static Stream<Arguments> toIntZeroArguments() {
        // Create a real Converter instance for the third case – assuming a no‑arg constructor exists.
        Converter defaultConverter = null;
        try {
            // The actual implementation class may be named DefaultConverter; adjust if necessary.
            Class<?> clazz = Class.forName("com.cedarsoftware.util.convert.DefaultConverter");
            defaultConverter = (Converter) clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            // If the class is not present in the test runtime, fall back to null.
            // The method under test does not use the converter, so null is acceptable.
        }
        return Stream.of(Arguments.of("Both parameters are null", null, null), Arguments.of("String input with a mock converter", "123", // The mock will be injected by JUnit; we use a placeholder that will be replaced
        // at runtime via the test method's field. To keep the stream static, we pass null
        // and let the test method use the @Mock field when appropriate.
        null), Arguments.of("Integer input with a real DefaultConverter", Integer.valueOf(5), defaultConverter), Arguments.of("Custom object with a null converter", new Object(), null));
    }

    @Test
    void toIntZero_ReturnsZero_WhenInputIsNull() {
        Integer result = NumberConversions.toIntZero(null, mockConverter);
        assertEquals(Integer.valueOf(0), result, "Expected zero for null input");
    }

    @Test
    void toIntZero_ReturnsZero_WhenInputIsInteger() {
        Integer result = NumberConversions.toIntZero(123, mockConverter);
        assertEquals(Integer.valueOf(0), result, "Expected zero regardless of integer input");
    }

    @Test
    void toIntZero_ReturnsZero_WhenInputIsString() {
        Integer result = NumberConversions.toIntZero("456", mockConverter);
        assertEquals(Integer.valueOf(0), result, "Expected zero regardless of string input");
    }

    @Test
    void toIntZero_ReturnsZero_WhenInputIsBigDecimal() {
        Integer result = NumberConversions.toIntZero(new java.math.BigDecimal("789.01"), mockConverter);
        assertEquals(Integer.valueOf(0), result, "Expected zero regardless of BigDecimal input");
    }

    @Test
    void toIntZero_ReturnsZero_WhenInputIsCustomObject() {
        Object custom = new Object();
        Integer result = NumberConversions.toIntZero(custom, mockConverter);
        assertEquals(Integer.valueOf(0), result, "Expected zero regardless of arbitrary object input");
    }

    /**
     * Parameterized test that covers all JSON‑specified cases for {@code toIntZero}.
     *
     * @param description textual description of the scenario (used only for readability)
     * @param from        the first argument to {@code toIntZero}
     * @param converter   the second argument to {@code toIntZero}
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("toIntZeroArguments")
    void toIntZero_AlwaysReturnsZero(String description, Object from, Converter converter) {
        // Act
        Integer result = NumberConversions.toIntZero(from, converter);
        // Assert: the constant zero is returned
        assertEquals(Integer.valueOf(0), result, () -> "Expected Integer.ZERO for case: " + description);
        // Verify that the converter (if non‑null) is never used.
        if (converter != null) {
            // The mock is the only Converter instance we can verify; for a real instance we
            // simply ensure no exception is thrown (the method does not touch it).
            if (converter == mockConverter) {
                verifyNoInteractions(mockConverter);
            }
        }
    }
}
