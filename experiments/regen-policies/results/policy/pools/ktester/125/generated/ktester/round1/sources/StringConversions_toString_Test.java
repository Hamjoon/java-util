package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toString_Test {

    @Mock
    private Converter // Converter is not used by the method but we provide a mock
    mockConverter;

    /**
     * Simple helper class whose {@code toString()} method returns a predictable value.
     */
    private static final class SampleObject {

        private final String value;

        SampleObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "SampleObject:" + value;
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock was not interacted with (method should ignore the converter)
        Mockito.verifyNoInteractions(mockConverter);
    }

    private static Stream<Arguments> nullAndNonNullProvider() {
        return Stream.of(// Null input
        Arguments.of("When the source object is null the method should return null regardless of the converter.", null, null), // Simple String
        Arguments.of("When the source object is a non‑null String, the method should return the same string.", "sample text", "sample text"), // Integer
        Arguments.of("When the source object is a non‑null Integer, the method should return its string representation.", Integer.valueOf(42), "42"), // Date (epoch)
        Arguments.of("When the source object is a non‑null Date, the method should return Date.toString().", new Date(0L), new Date(0L).toString()), // Custom object with overridden toString()
        Arguments.of("Custom object with overridden toString() should have that value returned.", new Object() {

            @Override
            public String toString() {
                return "custom";
            }
        }, "custom"), // Empty string
        Arguments.of("Empty string should be returned unchanged.", "", ""));
    }

    private static Stream<Arguments> diverseTypesProvider() {
        return Stream.of(// Boolean
        Arguments.of("Boolean true should be converted to its string representation \"true\".", Boolean.TRUE, "true"), // Double
        Arguments.of("Double value should be converted using Double.toString().", Double.valueOf(3.1415), "3.1415"), // BigDecimal
        Arguments.of("BigDecimal should return its plain string representation.", new BigDecimal("12345.6789"), "12345.6789"), // Character
        Arguments.of("Character instance should be converted to a single‑character string.", Character.valueOf('A'), "A"), // StringBuilder
        Arguments.of("StringBuilder's toString() should be used, returning the built string.", new StringBuilder("builder"), "builder"), // Enum
        Arguments.of("Enum constant should be converted to its name via Enum.toString().", SampleEnum.ONE, "ONE"), // toString() that throws RuntimeException – expect propagation
        Arguments.of("If the object's toString() throws a RuntimeException, the same exception should propagate.", new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("boom");
            }
        }, RuntimeException.class), // toString() returns null – method should return null
        Arguments.of("When toString() returns null, the method should return null (no additional handling).", new Object() {

            @Override
            public String toString() {
                return null;
            }
        }, null), // Non‑null converter should not affect result
        Arguments.of("A non‑null converter argument must not affect the result; the string is returned unchanged.", "converter test", "converter test"));
    }

    private static Stream<Arguments> exceptionScenariosProvider() {
        return Stream.of(Arguments.of("The object's toString() throws an IllegalStateException; the method should propagate the same exception.", new Object() {

            @Override
            public String toString() {
                throw new IllegalStateException("illegal");
            }
        }, IllegalStateException.class), Arguments.of("The object's toString() throws a NullPointerException; the method should propagate it unchanged.", new Object() {

            @Override
            public String toString() {
                throw new NullPointerException();
            }
        }, NullPointerException.class), Arguments.of("The object's toString() throws a generic RuntimeException; the method must not catch it.", new Object() {

            @Override
            public String toString() {
                throw new RuntimeException("boom");
            }
        }, RuntimeException.class), Arguments.of("The object's toString() throws an Error; the method should allow it to propagate.", new Object() {

            @Override
            public String toString() {
                throw new AssertionError("serious");
            }
        }, AssertionError.class));
    }

    @Test
    void toString_WithNullInput_ReturnsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        String result = StringConversions.toString(input, mockConverter);
        // THEN
        assertNull(result, "Expected null when input is null");
    }

    @Test
    void toString_WithNonNullInput_ReturnsObjectToString() {
        // GIVEN
        Object input = new SampleObject("test-value");
        // WHEN
        String result = StringConversions.toString(input, mockConverter);
        // THEN
        assertEquals("SampleObject:test-value", result, "Expected the object's toString() representation to be returned");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullAndNonNullProvider")
    void toString_NullAndNonNullHandling(String description, Object input, String expected) {
        // WHEN
        String result = StringConversions.toString(input, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("diverseTypesProvider")
    void toString_DiverseTypes(String description, Object input, String expected) {
        // WHEN
        String result = StringConversions.toString(input, mockConverter);
        // THEN
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("exceptionScenariosProvider")
    void toString_ExceptionScenarios(String description, Object input, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        Throwable thrown = assertThrows(expectedException, () -> StringConversions.toString(input, mockConverter), description);
        // Verify that the thrown exception type matches expectation (message is not asserted)
        assertEquals(expectedException, thrown.getClass(), description);
    }
}
