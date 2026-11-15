package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharacterArrayConversions_toStringBuilder_Test {

    /**
     * The {@link Converter} instance used for all test cases.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Mock a ConverterOptions that returns an empty map for overrides.
        ConverterOptions mockOptions = mock(ConverterOptions.class);
        when(mockOptions.getConverterOverrides()).thenReturn(Collections.emptyMap());
        // Create a real Converter using the mocked options.
        converter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup needed.
        converter = null;
    }

    private static Stream<Arguments> normalAndEdgeCases() {
        return Stream.of(// Standard non‑empty array, null converter
        Arguments.of("Standard case: non‑empty Character[] with null converter", new Character[] { 'a', 'b', 'c' }, null, "abc"), // Empty array, null converter
        Arguments.of("Edge case: empty Character[] with null converter", new Character[0], null, ""), // Array containing a null element – StringBuilder.append(Object) writes "null"
        Arguments.of("Array with null element: should render \"xnully\"", new Character[] { 'x', null, 'y' }, null, "xnully"), // Unicode boundary characters, null converter
        Arguments.of("Boundary Unicode characters (\\u0000 and \\uFFFF)", new Character[] { '\u0000', '\uFFFF' }, null, // The string representation contains the two characters; building it directly:
        new String(new char[] { '\u0000', '\uFFFF' })), // Non‑null converter (real instance) – should behave identically
        Arguments.of("Non‑null Converter supplied (real instance)", new Character[] { 'a', 'b', 'c' }, // The actual Converter will be injected later in the test method via the field.
        // Here we pass a placeholder; the test method will replace null with the field.
        null, "abc"));
    }

    private static Stream<Arguments> nullAndTypeSafetyCases() {
        return Stream.of(Arguments.of("Null input should trigger NullPointerException", null, null, NullPointerException.class), Arguments.of("Non‑Character array should trigger ClassCastException", new Integer[] { 1, 2, 3 }, null, ClassCastException.class), Arguments.of("Primitive char[] should trigger ClassCastException", new char[] { 'a', 'b' }, null, ClassCastException.class));
    }

    @Test
    void toStringBuilder_convertsTypicalCharacterArray() {
        // GIVEN
        Character[] input = new Character[] { 'J', 'a', 'v', 'a' };
        // WHEN
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder should not be null");
        assertEquals("Java", result.toString(), "StringBuilder content should match the character sequence");
        assertEquals(input.length, result.length(), "Length of StringBuilder should equal input array length");
    }

    @Test
    void toStringBuilder_convertsEmptyCharacterArray() {
        // GIVEN
        Character[] input = new Character[0];
        // WHEN
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, converter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder should not be null even for empty input");
        assertEquals(0, result.length(), "StringBuilder should be empty for an empty input array");
        assertEquals("", result.toString(), "StringBuilder string representation should be empty");
    }

    @Test
    void toStringBuilder_nullInputThrowsClassCastException() {
        // GIVEN
        // The method casts directly to Character[], so null leads to NPE before loop.
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> CharacterArrayConversions.toStringBuilder(input, converter), "Passing null should result in a NullPointerException");
    }

    @Test
    void toStringBuilder_wrongTypeThrowsClassCastException() {
        // GIVEN
        // Not a Character[].
        Object input = new Integer[] { 1, 2, 3 };
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterArrayConversions.toStringBuilder(input, converter), "Passing a non‑Character array should result in a ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Normal and edge array handling
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalAndEdgeCases")
    void toStringBuilder_normalAndEdge(String description, Character[] input, Converter suppliedConverter, String expectedString) {
        // WHEN
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, suppliedConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder should never be null");
        assertEquals(expectedString, result.toString(), "StringBuilder content should match the expected concatenation");
        assertEquals(input.length, result.length(), "Length of StringBuilder should equal the input array length");
    }

    /* -------------------------------------------------------------
     *  Null and type‑safety checks
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullAndTypeSafetyCases")
    void toStringBuilder_nullAndTypeSafety(String description, Object input, Converter suppliedConverter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> CharacterArrayConversions.toStringBuilder(input, suppliedConverter), description);
    }

    /* -------------------------------------------------------------
     *  Primitive array type mismatch – separate explicit test
     * ------------------------------------------------------------- */
    @Test
    void toStringBuilder_primitiveCharArrayThrowsClassCastException() {
        // GIVEN
        char[] primitiveArray = new char[] { 'a', 'b' };
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> CharacterArrayConversions.toStringBuilder(primitiveArray, converter), "Passing a primitive char[] must result in a ClassCastException");
    }

    // Adjust the test method to handle the placeholder logic:
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("normalAndEdgeCases")
    void toStringBuilder_normalAndEdgeAdjusted(String description, Character[] input, Converter suppliedConverter, String expectedString, TestInfo testInfo) {
        // If the supplied converter is null but the description indicates a real instance is wanted,
        // use the field 'converter' that was created in @BeforeEach.
        Converter effectiveConverter = suppliedConverter;
        if (description.contains("Non‑null Converter supplied")) {
            effectiveConverter = converter;
        }
        // WHEN
        StringBuilder result = CharacterArrayConversions.toStringBuilder(input, effectiveConverter);
        // THEN
        assertNotNull(result, "Resulting StringBuilder should never be null");
        assertEquals(expectedString, result.toString(), "StringBuilder content should match the expected concatenation");
        assertEquals(input.length, result.length(), "Length of StringBuilder should equal the input array length");
    }
}
