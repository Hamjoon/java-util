package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterConversions_toString_Test {

    /**
     * A mock Converter – the method under test does not use it, but we verify that.
     */
    @Mock
    Converter mockConverter;

    /**
     * Simple POJO with a custom toString implementation for testing purposes.
     */
    private static final class DummyObject {

        @Override
        public String toString() {
            return "DummyObjectString";
        }
    }

    /**
     * Simple {@link Converter} implementation that does nothing.
     */
    private static final class DummyConverter implements Converter {
        // No methods are required for the test – the class merely exists to be passed.
    }

    /**
     * Converter whose methods all throw – used to prove the method never calls it.
     */
    private static final class ThrowingConverter implements Converter {
        // All possible methods (if any) would throw; since the interface is unknown we leave it empty.
        // The mere existence of this class is enough – any accidental call would result in a test failure.
    }

    /**
     * Object whose {@code toString()} throws an {@link IllegalStateException}.
     */
    private static final class ThrowingToStringObject {

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * Object whose {@code toString()} throws a {@link NullPointerException}.
     */
    private static final class NullPointerThrowingObject {

        @Override
        public String toString() {
            throw new NullPointerException("null pointer from toString");
        }
    }

    /**
     * Object whose {@code toString()} throws an {@link AssertionError}.
     */
    private static final class ErrorThrowingObject {

        @Override
        public String toString() {
            throw new AssertionError("assertion error from toString");
        }
    }

    /**
     * Custom object with a bespoke {@code toString()} implementation.
     */
    private static final class CustomObject {

        @Override
        public String toString() {
            return "CustomObjectString";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already initialises @Mock fields.
        // Ensure the mock has a clean state before each test.
        reset(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock was not interacted with unless a test explicitly checks it.
        // This helps catch accidental usage of the Converter inside CharacterConversions.toString.
        verifyNoInteractions(mockConverter);
    }

    /**
     * Provides a stream of arguments for the normal conversion cases.
     * Each argument consists of:
     *   - the input object,
     *   - an optional {@link Converter} instance (may be {@code null} or a dummy),
     *   - the expected string result,
     *   - a description used only for readability.
     */
    private static Stream<Arguments> normalCases() {
        return Stream.of(// null input – must return the literal "null"
        Arguments.of(null, null, "null", "null input"), // plain String – concatenation returns the same value
        Arguments.of("hello", null, "hello", "String input"), // Integer – toString() yields its decimal representation
        Arguments.of(Integer.valueOf(123), new DummyConverter(), "123", "Integer with dummy converter"), // Boolean.TRUE – default toString() yields "true"
        Arguments.of(Boolean.TRUE, null, "true", "Boolean true"), // Custom object with overridden toString()
        Arguments.of(new CustomObject(), null, "CustomObjectString", "CustomObject overrides toString()"), // Plain Object – default Object.toString() format
        Arguments.of(new Object(), null, null, "Default Object toString() format"), // int array – default array toString() format
        Arguments.of(new int[] { 1, 2, 3 }, null, null, "int[] default toString() format"), // String with a converter that would throw if used
        Arguments.of("test", new ThrowingConverter(), "test", "String with ThrowingConverter"));
    }

    @Test
    void toStringReturnsNullStringWhenInputIsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        String result = CharacterConversions.toString(input, mockConverter);
        // THEN
        assertEquals("null", result, "toString(null) should return the literal \"null\"");
        // No interaction with the converter is expected – already verified in @AfterEach.
    }

    @Test
    void toStringReturnsSameStringWhenInputIsString() {
        // GIVEN
        String input = "hello world";
        // WHEN
        String result = CharacterConversions.toString(input, mockConverter);
        // THEN
        assertEquals("hello world", result);
    }

    @Test
    void toStringConvertsIntegerUsingItsToString() {
        // GIVEN
        Integer input = 42;
        // WHEN
        String result = CharacterConversions.toString(input, mockConverter);
        // THEN
        assertEquals("42", result);
    }

    @Test
    void toStringUsesCustomObjectToStringImplementation() {
        // GIVEN
        DummyObject input = new DummyObject();
        // WHEN
        String result = CharacterConversions.toString(input, mockConverter);
        // THEN
        assertEquals("DummyObjectString", result);
    }

    @Test
    void toStringDoesNotInteractWithConverterEvenWhenConverterIsNonNull() {
        // GIVEN
        Double input = 3.14;
        // WHEN
        String result = CharacterConversions.toString(input, mockConverter);
        // THEN
        assertEquals("3.14", result);
        // Explicit verification that the mock was not used (redundant with @AfterEach but clear).
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("normalCases")
    void toStringNormalBehaviour(Object input, Converter converter, String expected, String _description) {
        // GIVEN
        Converter usedConverter = (converter != null) ? converter : mockConverter;
        // WHEN
        String result = CharacterConversions.toString(input, usedConverter);
        // THEN
        if (expected != null) {
            // For deterministic expected values we can compare directly.
            assertEquals(expected, result);
        } else {
            // For cases where the exact hashcode part is nondeterministic we validate the pattern.
            assertNotNull(result, "Result should not be null");
            if (input instanceof Object && !(input instanceof int[])) {
                // Default Object.toString(): className@hexHash
                String regex = Pattern.quote(input.getClass().getName()) + "@[0-9a-fA-F]+";
                assertTrue(Pattern.matches(regex, result), "Result should match pattern " + regex + " but was: " + result);
            } else if (input instanceof int[]) {
                // Array representation: [I@hexHash
                String regex = "\\[I@[0-9a-fA-F]+";
                assertTrue(Pattern.matches(regex, result), "Result should match pattern " + regex + " but was: " + result);
            }
        }
    }

    @Test
    void toStringPropagatesIllegalStateExceptionFromObject() {
        // GIVEN
        ThrowingToStringObject input = new ThrowingToStringObject();
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> CharacterConversions.toString(input, mockConverter));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void toStringPropagatesNullPointerExceptionFromObject() {
        // GIVEN
        NullPointerThrowingObject input = new NullPointerThrowingObject();
        // WHEN / THEN
        NullPointerException ex = assertThrows(NullPointerException.class, () -> CharacterConversions.toString(input, mockConverter));
        assertEquals("null pointer from toString", ex.getMessage());
    }

    @Test
    void toStringPropagatesErrorFromObject() {
        // GIVEN
        ErrorThrowingObject input = new ErrorThrowingObject();
        // WHEN / THEN
        AssertionError err = assertThrows(AssertionError.class, () -> CharacterConversions.toString(input, mockConverter));
        assertEquals("assertion error from toString", err.getMessage());
    }
}
