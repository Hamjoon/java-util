package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class VoidConversions_toCharacter_Test {

    /**
     * A mocked {@link Converter} – the method under test does not invoke any behaviour on it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A generic mock – the method under test must never call any behaviour on it.
     */
    @Mock
    private Converter genericMockConverter;

    /**
     * A converter that throws if any of its methods are invoked.
     */
    private static final Converter throwingConverter = new Converter() {

        @Override
        public Object convert(Object source, Class<?> target) {
            throw new RuntimeException("Converter should not be invoked");
        }
    };

    private static Stream<Arguments> basicBehaviorProvider() {
        return Stream.of(// Both arguments null
        Arguments.of(null, null, "Both arguments are null; should return Character.MIN_VALUE."), // Non‑null `from` (String) with null converter
        Arguments.of("some string", null, "Non‑null `from` (String) with null converter."), // Non‑null `from` (Integer) with null converter
        Arguments.of(12345, null, "Non‑null `from` (Integer) with null converter."), // Non‑null `from` (String) with a non‑null Converter instance
        Arguments.of("A", new Converter() {

            @Override
            public Object convert(Object source, Class<?> target) {
                // stub – should never be called
                return null;
            }
        }, "Non‑null `from` (String) with a non‑null Converter instance."), // Arbitrary object with a valid Converter
        Arguments.of(new Object(), new Converter() {

            @Override
            public Object convert(Object source, Class<?> target) {
                return null;
            }
        }, "Arbitrary object and a valid Converter."));
    }

    private static Stream<Arguments> additionalScenariosProvider() {
        return Stream.of(// Null `from` with a non‑null Converter
        Arguments.of(null, new Converter() {

            @Override
            public Object convert(Object source, Class<?> target) {
                return null;
            }
        }, "Null `from` with a non‑null Converter."), // `from` is a Character instance
        Arguments.of(Character.valueOf('Z'), null, "`from` is a Character instance."), // Primitive char (autoboxed)
        Arguments.of('z', null, "Primitive char (autoboxed) supplied."), // Boolean primitive
        Arguments.of(true, null, "`from` is a Boolean primitive."), // int array
        Arguments.of(new int[] { 1, 2, 3 }, null, "`from` is an int array."), // List<String>
        Arguments.of(Arrays.asList("a", "b"), null, "`from` is a List<String>."), // Map
        Arguments.of(Collections.singletonMap("key", 1), null, "`from` is a Map."), // Custom object with overridden toString()
        Arguments.of(new Object() {

            @Override
            public String toString() {
                return "X";
            }
        }, null, "`from` is a custom object with overridden toString."), // Converter that would throw if invoked
        Arguments.of("irrelevant", throwingConverter, "Converter that throws if its convert method is invoked."), // Long string input
        Arguments.of("A very long string that exceeds typical lengths...", null, "Long string input."));
    }

    @Test
    void toCharacter_returnsCharacterMinValue_forVariousInputs() {
        // GIVEN
        Object[] inputs = { null, "", "any string", 123, true, new Object() };
        // WHEN / THEN
        for (Object input : inputs) {
            Character result = VoidConversions.toCharacter(input, mockConverter);
            assertEquals(Character.MIN_VALUE, result, () -> "Expected Character.MIN_VALUE for input: " + input);
        }
    }

    /* --------------------------------------------------------------
     *  Test group: VoidConversions.toCharacter – basic behavior
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicBehaviorProvider")
    void toCharacter_basicBehavior(Object from, Converter converter, String description) {
        // WHEN
        Character result = VoidConversions.toCharacter(from, converter);
        // THEN
        assertEquals(Character.MIN_VALUE, result, () -> "Expected Character.MIN_VALUE for case: " + description);
    }

    /* --------------------------------------------------------------
     *  Test group: VoidConversions.toCharacter – additional scenarios
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("additionalScenariosProvider")
    void toCharacter_additionalScenarios(Object from, Converter converter, String description) {
        // WHEN
        Character result = VoidConversions.toCharacter(from, converter);
        // THEN
        assertEquals(Character.MIN_VALUE, result, () -> "Expected Character.MIN_VALUE for case: " + description);
    }
}
