package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class VoidConversions_toBoolean_Test {

    /**
     * Mocked Converter – the method under test should never interact with it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Real Converter created once for the whole test suite (optional, shown for completeness).
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Create a real Converter with default options – not required for the current tests
        // but demonstrates how to initialise shared resources if needed later.
        realConverter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to ensure no stray interactions.
        Mockito.reset(mockConverter);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release shared resources if any (none for this simple case).
        realConverter = null;
    }

    private static Stream<Arguments> group1Cases() {
        return Stream.of(// Both arguments null
        Arguments.of(null, null, Boolean.FALSE, "Both arguments are null – method should still return Boolean.FALSE."), // Non‑null from, null converter
        Arguments.of("any string", null, Boolean.FALSE, "Non‑null `from` (String) and null `converter` – expected result is Boolean.FALSE."), // Integer from, null converter
        Arguments.of(12345, null, Boolean.FALSE, "`from` is an Integer, `converter` is null – method must ignore the value and return Boolean.FALSE."), // Non‑null from and mock converter
        Arguments.of(new Object(), null, Boolean.FALSE, "Both arguments are non‑null – a mock Converter is supplied, but method still returns Boolean.FALSE."), // Boolean true with mock converter
        Arguments.of(Boolean.TRUE, null, Boolean.FALSE, "`from` is a Boolean true, `converter` provided – method does not perform conversion and returns Boolean.FALSE."));
    }

    private static Stream<Arguments> group2Cases() {
        return Stream.of(// Re‑covered case from group 1 (kept for completeness)
        Arguments.of(null, null, Boolean.FALSE, "Both arguments are null – already covered, but retained for completeness."), // Null from, mock converter
        Arguments.of(null, null, Boolean.FALSE, "`from` is null while a non‑null Converter is supplied – method must still ignore both and return Boolean.FALSE."), // Boolean.FALSE with null converter
        Arguments.of(Boolean.FALSE, null, Boolean.FALSE, "`from` is Boolean false and `converter` is null – result remains Boolean.FALSE."), // Boolean.TRUE with null converter
        Arguments.of(Boolean.TRUE, null, Boolean.FALSE, "`from` is Boolean true and `converter` is null – method does not perform conversion, returns Boolean.FALSE."), // int array with null converter
        Arguments.of(new int[] { 1, 2, 3 }, null, Boolean.FALSE, "`from` is an int array, `converter` is null – method should still return Boolean.FALSE."), // Integer zero with null converter
        Arguments.of(0, null, Boolean.FALSE, "`from` is Integer zero, `converter` is null – expected Boolean.FALSE."));
    }

    private static Stream<Arguments> group3Cases() {
        return Stream.of(// String with mock converter
        Arguments.of("any string", null, Boolean.FALSE, "`from` is a non‑null String and a mock Converter is supplied – method still returns Boolean.FALSE."), // Integer with mock converter
        Arguments.of(12345, null, Boolean.FALSE, "`from` is an Integer, `converter` is a mock – method ignores both and returns Boolean.FALSE."), // Generic Object with mock converter
        Arguments.of(new Object(), null, Boolean.FALSE, "`from` is a generic Object, `converter` is a mock – still Boolean.FALSE."), // Boolean true with mock converter
        Arguments.of(Boolean.TRUE, null, Boolean.FALSE, "`from` is Boolean true, `converter` is a mock – method returns Boolean.FALSE."), // Boolean false with mock converter
        Arguments.of(Boolean.FALSE, null, Boolean.FALSE, "`from` is Boolean false, `converter` is a mock – result remains Boolean.FALSE."), // char array with mock converter
        Arguments.of(new char[] { 'a', 'b' }, null, Boolean.FALSE, "`from` is a char array, `converter` is a mock – method must still return Boolean.FALSE."));
    }

    @Test
    void toBoolean_returnsFalse_forNonNullObject_andMockConverter() {
        Object source = "any string";
        Boolean result = VoidConversions.toBoolean(source, mockConverter);
        assertEquals(Boolean.FALSE, result, "toBoolean should always return Boolean.FALSE");
        // Verify that the method does not interact with the Converter at all.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toBoolean_returnsFalse_forNullObject_andMockConverter() {
        Boolean result = VoidConversions.toBoolean(null, mockConverter);
        assertEquals(Boolean.FALSE, result, "toBoolean should return Boolean.FALSE even when source is null");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toBoolean_returnsFalse_whenConverterIsNull() {
        // The method signature allows a null Converter; the implementation ignores it.
        Boolean result = VoidConversions.toBoolean("irrelevant", null);
        assertEquals(Boolean.FALSE, result, "toBoolean should return Boolean.FALSE even when Converter is null");
    }

    @Test
    void toBoolean_returnsFalse_usingRealConverterInstance() {
        // Demonstrate that a real Converter instance does not affect the outcome.
        Boolean result = VoidConversions.toBoolean(123, realConverter);
        assertEquals(Boolean.FALSE, result, "toBoolean should always return Boolean.FALSE regardless of real Converter");
    }

    /* -------------------------------------------------------------
     *  Group 1 – “always returns Boolean.FALSE”
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("group1Cases")
    void toBoolean_alwaysReturnsFalse_group1(Object from, Converter converter, Boolean expected, String description) {
        // Reset mock at the start of each invocation (important for repeated calls
        // within the same parameterized test method).
        Mockito.reset(mockConverter);
        Boolean result = VoidConversions.toBoolean(from, converter);
        assertEquals(expected, result, () -> "Expected Boolean.FALSE for case: " + description);
        // If a mock Converter was supplied we must ensure the method never touched it.
        if (converter == mockConverter) {
            verifyNoInteractions(mockConverter);
        }
    }

    /* -------------------------------------------------------------
     *  Group 2 – “various inputs with null converter”
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("group2Cases")
    void toBoolean_variousInputs_nullConverter_group2(Object from, Converter converter, Boolean expected, String description) {
        // No need to reset mock here because the converter is null.
        Boolean result = VoidConversions.toBoolean(from, converter);
        assertEquals(expected, result, () -> "Expected Boolean.FALSE for case: " + description);
    }

    /* -------------------------------------------------------------
     *  Group 3 – “various inputs with non‑null converter”
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("group3Cases")
    void toBoolean_variousInputs_nonNullConverter_group3(Object from, Converter converter, Boolean expected, String description) {
        // Reset mock before each invocation.
        Mockito.reset(mockConverter);
        Boolean result = VoidConversions.toBoolean(from, converter);
        assertEquals(expected, result, () -> "Expected Boolean.FALSE for case: " + description);
        // The supplied converter is always the mock; verify no interaction.
        verifyNoInteractions(mockConverter);
    }
}
