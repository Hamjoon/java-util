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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class VoidConversions_toBoolean_Test {

    /**
     * A mock {@link Converter} used as a dummy dependency for the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A real {@link Converter} instance that could be used in future tests.
     */
    private static Converter realConverter;

    class CustomClass {
    }

    class CustomClass {
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a real Converter once for the whole test suite.
        // The Converter requires a ConverterOptions instance; we can use the default constructor.
        realConverter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to ensure a clean interaction history.
        Mockito.reset(mockConverter);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Nullify the static converter to aid garbage collection.
        realConverter = null;
    }

    /**
     * Supplies all test cases defined in the JSON input.
     *
     * @return a stream of {@link Arguments} where each argument consists of:
     *         <ul>
     *           <li>source object</li>
     *           <li>converter instance (null, mock, or throwing mock)</li>
     *           <li>description string</li>
     *         </ul>
     */
    private static Stream<Arguments> provideToBooleanTestCases() {
        // Create a mock Converter that will be used for cases expecting a mock.
        Converter mockForCases = mock(Converter.class);
        // Create a mock Converter that throws on any method call.
        Converter throwingMock = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter method should not be invoked");
        });
        // Helper instances used in test cases
        List<Object> list = Arrays.asList("a", "b");
        class CustomClass {
        }
        CustomClass customInstance = new CustomClass();
        return Stream.of(// ---- Basic functionality group ----
        Arguments.of(null, null, "Both arguments are null; method should still return Boolean.FALSE."), Arguments.of("true", null, "String \"true\" is passed as the source object; method ignores it and returns Boolean.FALSE."), Arguments.of(123, null, "An Integer source object is supplied; expected result remains Boolean.FALSE."), Arguments.of(new Object(), null, "A generic Object instance is used as input; method should still return Boolean.FALSE."), Arguments.of("any", mockForCases, "A mock Converter is supplied together with a String; method does not interact with the converter and returns Boolean.FALSE."), Arguments.of(null, mockForCases, "Null source with a non‑null Converter; method still returns Boolean.FALSE."), // ---- Edge cases group ----
        Arguments.of(Boolean.TRUE, null, "Source object is Boolean.TRUE; method should ignore the value and still return Boolean.FALSE."), Arguments.of(Boolean.FALSE, mockForCases, "Source object is Boolean.FALSE with a mock Converter; method must not invoke the converter and must return Boolean.FALSE."), Arguments.of(new int[] { 1, 2, 3 }, null, "Source object is an int array; regardless of array content, the method should return Boolean.FALSE."), Arguments.of(list, mockForCases, "Source object is a List; method must still return Boolean.FALSE and must not interact with the provided Converter."), Arguments.of(customInstance, mockForCases, "Source object is an instance of a user‑defined class; method should ignore it and return Boolean.FALSE."), Arguments.of(null, throwingMock, "Converter implementation throws an exception if any method is called; method must not call the converter and must still return Boolean.FALSE."));
    }

    /**
     * Verifies that {@code VoidConversions.toBoolean} returns {@code Boolean.FALSE}
     * for a {@code null} source value when a mock {@link Converter} is supplied.
     */
    @Test
    void toBoolean_returnsFalse_forNullSource_withMockConverter() {
        // given
        Object source = null;
        // when
        Boolean result = VoidConversions.toBoolean(source, mockConverter);
        // then
        assertSame(Boolean.FALSE, result, "Expected Boolean.FALSE for null source");
        // No interaction with the mock is required; verify that none occurred.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Verifies that {@code VoidConversions.toBoolean} returns {@code Boolean.FALSE}
     * for a non‑null source value when a real {@link Converter} instance is supplied.
     */
    @Test
    void toBoolean_returnsFalse_forNonNullSource_withRealConverter() {
        // given
        Object source = "any value";
        // when
        Boolean result = VoidConversions.toBoolean(source, realConverter);
        // then
        assertSame(Boolean.FALSE, result, "Expected Boolean.FALSE for any non‑null source");
    }

    /**
     * Verifies that the method consistently returns {@code Boolean.FALSE}
     * for a variety of source types.
     */
    @Test
    void toBoolean_alwaysReturnsFalse_forVariousSources() {
        // given
        Object[] diverseSources = { "string", 123, 45.6, true, new Object(), new int[] { 1, 2, 3 }, null };
        // when / then
        for (Object src : diverseSources) {
            Boolean result = VoidConversions.toBoolean(src, mockConverter);
            assertSame(Boolean.FALSE, result, () -> "Expected Boolean.FALSE for source: " + (src == null ? "null" : src.getClass()));
        }
        // Ensure the mock was never used.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Parameterized test that iterates over all JSON‑defined cases.
     *
     * @param from        the source object passed to {@code toBoolean}
     * @param converter   the {@link Converter} (may be {@code null}, a mock, or a throwing mock)
     * @param description textual description of the scenario (used in the assertion message)
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideToBooleanTestCases")
    void toBoolean_behaviourDrivenTests(Object from, Converter converter, String description) {
        // when
        Boolean result = VoidConversions.toBoolean(from, converter);
        // then
        assertSame(Boolean.FALSE, result, () -> "Expected Boolean.FALSE for case: " + description);
        // Verify that the converter (if any) was never interacted with.
        if (converter != null) {
            // For the special throwing mock we also expect no interaction.
            verifyNoInteractions(converter);
        }
    }
}
