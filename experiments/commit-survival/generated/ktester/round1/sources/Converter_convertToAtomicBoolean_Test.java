package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class Converter_convertToAtomicBoolean_Test {

    private static Stream<Arguments> validConversionsProvider() {
        return Stream.of(arguments("primitive boolean true → true", true, true), arguments("primitive boolean false → false", false, false), arguments("Boolean.TRUE → true", Boolean.TRUE, true), arguments("Boolean.FALSE → false", Boolean.FALSE, false), arguments("String \"true\" (case‑insensitive) → true", "true", true), arguments("String \"FALSE\" (case‑insensitive) → false", "FALSE", false), arguments("Integer 1 (non‑zero) → true", 1, true), arguments("Integer 0 → false", 0, false), arguments("Existing AtomicBoolean(true) → copy true", new AtomicBoolean(true), true));
    }

    private static Stream<Arguments> numericEdgeCasesProvider() {
        return Stream.of(arguments("Byte 0 → false", Byte.valueOf((byte) 0), false), arguments("Byte 127 → true", Byte.valueOf((byte) 127), true), arguments("Short -1 → true", Short.valueOf((short) -1), true), arguments("Long 0L → false", Long.valueOf(0L), false), arguments("Long 123456789L → true", Long.valueOf(123456789L), true), arguments("Float 0.0f → false", Float.valueOf(0.0f), false), arguments("Float -0.001f → true", Float.valueOf(-0.001f), true), arguments("Double 0.0d → false", Double.valueOf(0.0d), false), arguments("Double 3.14d → true", Double.valueOf(3.14d), true), arguments("BigInteger 0 → false", new BigInteger("0"), false), arguments("BigInteger -42 → true", new BigInteger("-42"), true), arguments("BigDecimal 0.00 → false", new BigDecimal("0.00"), false), arguments("BigDecimal 0.001 → true", new BigDecimal("0.001"), true));
    }

    private static Stream<Arguments> stringVariationsProvider() {
        return Stream.of(arguments("Mixed‑case \"TrUe\" → true", "TrUe", true), arguments("Whitespace‑padded \"  false  \" → false", "  false  ", false), arguments("Numeric string \"1\" → true", "1", true), arguments("Numeric string \"0\" → false", "0", false));
    }

    private static Stream<Arguments> stringInvalidProvider() {
        return Stream.of(arguments("Unsupported textual \"yes\" → exception", "yes"), arguments("Unsupported textual \"no\" → exception", "no"), arguments("Non‑numeric mixed \"123abc\" → exception", "123abc"), arguments("Minus sign only \"-\" → exception", "-"));
    }

    private static Stream<Arguments> characterInputsProvider() {
        return Stream.of(arguments("Character 't' → true", 't', true), arguments("Character 'F' → false", 'F', false), arguments("Character digit '1' → true", '1', true), arguments("Character digit '0' → false", '0', false));
    }

    private static Stream<Arguments> unsupportedTypesProvider() {
        return Stream.of(arguments("Arbitrary Object → exception", new Object()), arguments("Array of int → exception", new int[] { 1, 0 }), arguments("Empty List → exception", Collections.emptyList()));
    }

    @Test
    void convertFromBooleanTrue() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // THEN
        assertNotNull(result, "Result should not be null for non‑null input");
        assertTrue(result.get(), "AtomicBoolean should contain true");
    }

    @Test
    void convertFromBooleanFalse() {
        // GIVEN
        Boolean source = Boolean.FALSE;
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // THEN
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should contain false");
    }

    @Test
    void convertFromStringTrue() {
        // GIVEN
        String source = "true";
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // THEN
        assertNotNull(result);
        assertTrue(result.get(), "String \"true\" should be converted to true");
    }

    @Test
    void convertFromStringFalse() {
        // GIVEN
        String source = "false";
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // THEN
        assertNotNull(result);
        assertFalse(result.get(), "String \"false\" should be converted to false");
    }

    @Test
    void convertFromIntegerOne() {
        // GIVEN
        Integer source = 1;
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // THEN
        assertNotNull(result);
        assertTrue(result.get(), "Integer 1 should be interpreted as true");
    }

    @Test
    void convertFromIntegerZero() {
        // GIVEN
        Integer source = 0;
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // THEN
        assertNotNull(result);
        assertFalse(result.get(), "Integer 0 should be interpreted as false");
    }

    @Test
    void convertFromNullReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // THEN
        assertNull(result, "Null input should result in null output");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validConversionsProvider")
    void validConversions(String description, Object input, boolean expectedValue) {
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(input);
        // THEN
        assertNotNull(result, description);
        assertEquals(expectedValue, result.get(), description);
    }

    @Test
    void existingAtomicBooleanIsCopied() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(true);
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // THEN
        assertNotNull(result);
        assertTrue(result.get());
        assertNotSame(source, result, "Conversion should return a new AtomicBoolean instance, not the original");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void nullInputReturnsNull() {
        // GIVEN
        Object source = null;
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(source);
        // THEN
        assertNull(result, "Null input should result in null output");
    }

    @Test
    void emptyStringThrowsException() {
        // GIVEN
        String source = "";
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicBoolean(source), "Empty string should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("numericEdgeCasesProvider")
    void numericEdgeCases(String description, Number input, boolean expected) {
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(input);
        // THEN
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringVariationsProvider")
    void stringVariations(String description, String input, boolean expected) {
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(input);
        // THEN
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("stringInvalidProvider")
    void stringInvalidValues(String description, String input) {
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicBoolean(input), description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("characterInputsProvider")
    void characterInputs(String description, Character input, boolean expected) {
        // WHEN
        AtomicBoolean result = Converter.convertToAtomicBoolean(input);
        // THEN
        assertNotNull(result, description);
        assertEquals(expected, result.get(), description);
    }

    @Test
    void unsupportedCharacterThrowsException() {
        // GIVEN
        Character input = 'x';
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicBoolean(input), "Unsupported character should cause IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedTypesProvider")
    void unsupportedTypes(String description, Object input) {
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> Converter.convertToAtomicBoolean(input), description);
    }
}
