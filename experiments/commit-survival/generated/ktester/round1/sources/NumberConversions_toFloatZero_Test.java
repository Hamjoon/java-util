package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toFloatZero_Test {

    /**
     * A mock {@link Converter} – the implementation does not matter for this method.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Supplies the test data extracted from the provided JSON.
     *
     * @return a stream of arguments for the parameterised test
     */
    private static Stream<Arguments> provideTestCases() {
        // Re‑use the mock created by Mockito for the cases that need a non‑null converter.
        // The mock will be injected by JUnit before each test execution.
        return Stream.of(// 1. both arguments null
        Arguments.of(null, null, "When both arguments are null, method should still return the constant Float zero."), // 2. String input, any converter
        Arguments.of("123.45", mockConverter(), "Regardless of the actual object or converter, toFloatZero always returns Float zero."), // 3. BigDecimal input, mock converter
        Arguments.of(new BigDecimal("5.0"), mockConverter(), "Even when a numeric type is supplied, the method does not perform conversion and returns Float zero."), // 4. Integer input, null converter
        Arguments.of(42, null, "Null converter does not affect the constant return value."), // 5. Generic Object input, mock converter
        Arguments.of(new Object(), mockConverter(), "Method should return Float zero even when 'from' is a generic Object and a mock Converter is supplied."), // 6. int array input, mock converter
        Arguments.of(new int[] { 1, 2, 3 }, mockConverter(), "Method should ignore array input and still return Float zero."), // 7. Boolean input, mock converter
        Arguments.of(true, mockConverter(), "Method should return Float zero for Boolean input regardless of converter."), // 8. Null 'from' with non‑null converter
        Arguments.of(null, mockConverter(), "Even when 'from' is null but converter is provided, the constant Float zero is returned."));
    }

    /**
     * Helper method to obtain a fresh mock {@link Converter} for each argument set.
     * JUnit creates a new test instance per execution, so a static method cannot inject
     * the @Mock field directly. We therefore create a new mock via Mockito.
     *
     * @return a new mock Converter
     */
    private static Converter mockConverter() {
        return mock(Converter.class);
    }

    @Test
    void toFloatZero_returnsZeroFloat_forNullInput() {
        Float result = NumberConversions.toFloatZero(null, mockConverter);
        assertEquals(Float.valueOf(0f), result, "toFloatZero should return Float zero when the source object is null");
    }

    @Test
    void toFloatZero_returnsZeroFloat_forStringInput() {
        Float result = NumberConversions.toFloatZero("123.45", mockConverter);
        assertEquals(Float.valueOf(0f), result, "toFloatZero should return Float zero for a String source");
    }

    @Test
    void toFloatZero_returnsZeroFloat_forNumberInput() {
        Float result = NumberConversions.toFloatZero(42, mockConverter);
        assertEquals(Float.valueOf(0f), result, "toFloatZero should return Float zero for a numeric source");
    }

    @Test
    void toFloatZero_returnsZeroFloat_forComplexObject() {
        Object complex = new java.util.Date();
        Float result = NumberConversions.toFloatZero(complex, mockConverter);
        assertEquals(Float.valueOf(0f), result, "toFloatZero should return Float zero for any object type");
    }

    @Test
    void toFloatZero_ignoresConverterBehaviour() {
        // Configure the mock to throw if any method is called – the method under test must not use it.
        doThrow(new RuntimeException("Converter should not be used")).when(mockConverter).convert(any(), any());
        Float result = NumberConversions.toFloatZero("unused", mockConverter);
        assertEquals(Float.valueOf(0f), result, "toFloatZero must not interact with the Converter instance");
    }

    /**
     * Parameterised test that covers all scenarios described in the JSON test cases.
     *
     * @param from        the source object passed to {@code toFloatZero}
     * @param converter   the {@link Converter} instance (may be {@code null})
     * @param description short description of the scenario (used in assertion messages)
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideTestCases")
    void toFloatZero_alwaysReturnsConstantFloatZero(Object from, Converter converter, String description) {
        // If we want to ensure the method truly ignores the converter, we can set up a strict mock.
        if (converter != null) {
            // Any unexpected interaction will cause the test to fail.
            doThrow(new AssertionError("Converter should not be used")).when(converter).convert(any(), any());
        }
        Float result = NumberConversions.toFloatZero(from, converter);
        assertEquals(CommonValues.FLOAT_ZERO, result, () -> "Expected FLOAT_ZERO for case: " + description);
    }
}
