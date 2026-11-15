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
class CharacterConversions_toLong_Test {

    /**
     * Mocked {@link Converter} – not used by the method under test.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Supplies the arguments for {@link #toLong_basicAndEdgeCases(...)}.
     */
    private static Stream<Arguments> basicAndEdgeCases() {
        return Stream.of(// Valid conversion: 'A' → 65
        Arguments.of(Character.valueOf('A'), null, 65L, "Valid conversion: a Character containing 'A' (Unicode 65) should be cast to char and returned as long 65."), // Null character → 0
        Arguments.of(Character.valueOf('\u0000'), null, 0L, "Valid conversion of the null character (Unicode 0) to long 0."), // Highest char value → 65535
        Arguments.of(Character.valueOf('\uffff'), null, 65535L, "Valid conversion of the highest char value (65535) to long."), // Null source → NPE
        Arguments.of(null, null, NullPointerException.class, "Passing null as the source object should cause a NullPointerException when the cast to char is attempted."), // Integer source → CCE
        Arguments.of(Integer.valueOf(65), null, ClassCastException.class, "An Integer cannot be cast to char; the method should throw ClassCastException."), // String source → CCE
        Arguments.of("B", null, ClassCastException.class, "A String is not a Character; casting to char should fail with ClassCastException."), // Valid conversion with a non‑null Converter instance (ignored)
        Arguments.of(Character.valueOf('z'), new Converter() {
        }, 122L, "Even when a non‑null Converter instance is supplied (which is ignored by the implementation), the conversion should succeed and return the Unicode value of 'z'."));
    }

    /**
     * Supplies the arguments for {@link #toLong_additionalTypeAndSurrogateTests(...)}.
     */
    private static Stream<Arguments> additionalTypeAndSurrogateCases() {
        return Stream.of(// Boolean source → CCE
        Arguments.of(Boolean.TRUE, null, ClassCastException.class, "A Boolean cannot be cast to char; the method should throw ClassCastException."), // Double source → CCE
        Arguments.of(Double.valueOf(65.0), null, ClassCastException.class, "A Double cannot be cast to char; expect ClassCastException."), // Arbitrary Object source → CCE
        Arguments.of(new Object(), null, ClassCastException.class, "An arbitrary Object is not a Character; casting to char should fail with ClassCastException."), // High‑surrogate character (U+D800) → 55296
        Arguments.of(Character.valueOf('\uD800'), null, 55296L, "High‑surrogate character (U+D800) should be cast to its numeric value 55296."), // Low‑surrogate character (U+DC00) → 56320
        Arguments.of(Character.valueOf('\uDC00'), null, 56320L, "Low‑surrogate character (U+DC00) should be cast to its numeric value 56320."), // Greek capital letter Omega (U+03A9) → 937
        Arguments.of(Character.valueOf('\u03A9'), null, 937L, "Greek capital letter Omega (U+03A9) should be converted to its Unicode code point 937."), // Character U+1234 with non‑null Converter (ignored)
        Arguments.of(Character.valueOf('\u1234'), new Converter() {
        }, 4660L, "A non‑null Converter is ignored; the character U+1234 should be converted to long 4660."), // Character 'M' with a Converter that would throw if used (still ignored)
        Arguments.of(Character.valueOf('M'), new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new RuntimeException("should not be called");
            }
        }, 77L, "Even if the supplied Converter would throw an exception when used, the implementation ignores it; conversion of 'M' should succeed with value 77."));
    }

    @Test
    void toLong_givenCharacter_returnsCorrectLongValue() {
        // GIVEN
        // Unicode value 65
        Character input = 'A';
        // WHEN
        long result = CharacterConversions.toLong(input, mockConverter);
        // THEN
        assertEquals(65L, result, "The long value should match the char's numeric value");
    }

    @Test
    void toLong_givenNonCharacterObject_throwsClassCastException() {
        // GIVEN
        // Not a Character
        Integer input = 123;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterConversions.toLong(input, mockConverter), "Casting a non‑Character object to char should throw ClassCastException");
    }

    @Test
    void toLong_givenNull_throwsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterConversions.toLong(input, mockConverter), "Casting null to char should result in NullPointerException");
    }

    /**
     * Parameterised test covering the “basic and edge cases” group.
     *
     * @param from        the source object supplied to {@code toLong}
     * @param converter   the {@link Converter} argument (may be {@code null})
     * @param expected    either a {@link Long} with the expected result or a
     *                    {@link Class} of the exception that should be thrown
     * @param description short description used as the test name
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("basicAndEdgeCases")
    void toLong_basicAndEdgeCases(Object from, Converter converter, Object expected, String description) {
        if (expected instanceof Long) {
            long result = CharacterConversions.toLong(from, converter);
            assertEquals((Long) expected, result, () -> "Expected long value " + expected + " but got " + result);
        } else if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> CharacterConversions.toLong(from, converter), () -> "Expected exception " + ((Class<?>) expected).getSimpleName());
        } else {
            fail("Invalid expected value: " + expected);
        }
    }

    /**
     * Parameterised test covering the “additional type and surrogate tests” group.
     *
     * @param from        the source object
     * @param converter   the {@link Converter} argument (may be {@code null})
     * @param expected    either a {@link Long} with the expected result or a
     *                    {@link Class} of the exception that should be thrown
     * @param description short description used as the test name
     */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("additionalTypeAndSurrogateCases")
    void toLong_additionalTypeAndSurrogateTests(Object from, Converter converter, Object expected, String description) {
        if (expected instanceof Long) {
            long result = CharacterConversions.toLong(from, converter);
            assertEquals((Long) expected, result, () -> "Expected long value " + expected + " but got " + result);
        } else if (expected instanceof Class) {
            assertThrows((Class<? extends Throwable>) expected, () -> CharacterConversions.toLong(from, converter), () -> "Expected exception " + ((Class<?>) expected).getSimpleName());
        } else {
            fail("Invalid expected value: " + expected);
        }
    }
}
