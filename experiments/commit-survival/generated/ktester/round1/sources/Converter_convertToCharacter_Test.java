package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Stream;

class Converter_convertToCharacter_Test {

    /**
     * Provides a stream of arguments for the {@link #convertToCharacterParameterized}
     * test.  Each argument consists of:
     * <ul>
     *   <li>the input object</li>
     *   <li>the expected outcome – either a {@link Character},
     *       {@code null}, or a {@link Class} representing the exception to be thrown</li>
     *   <li>a short description (used only for readability)</li>
     * </ul>
     */
    static Stream<Arguments> provideConvertToCharacterCases() {
        return Stream.of(// ----- empty String -------------------------------------------------
        Arguments.of("", IllegalArgumentException.class, "Empty String cannot be represented as a Character"), // ----- multi‑character String ---------------------------------------
        Arguments.of("abc", IllegalArgumentException.class, "String longer than one character is ambiguous for Character conversion"), // ----- numeric types within char range -------------------------------
        Arguments.of(65, 'A', "Integer 65 corresponds to the Unicode character 'A'"), Arguments.of(0, '\0', "Zero maps to the null character"), Arguments.of(0.0, '\0', "Floating‑point zero should be truncated to char '\\0'"), Arguments.of(97.9, 'a', "Floating‑point numbers are truncated (97) before casting to char, yielding 'a'"), Arguments.of(1234567890123L, IllegalArgumentException.class, "Long values outside the 0‑65535 char range must cause an exception"), // ----- numeric types out of range -----------------------------------
        Arguments.of(-1, IllegalArgumentException.class, "Negative numeric values cannot be represented as a Character"), Arguments.of(70000, IllegalArgumentException.class, "Values greater than Character.MAX_VALUE (65535) should trigger an exception"), // ----- Boolean values ------------------------------------------------
        Arguments.of(Boolean.TRUE, IllegalArgumentException.class, "Boolean values are not convertible to Character and must raise an exception"), Arguments.of(Boolean.FALSE, IllegalArgumentException.class, "Boolean values are not convertible to Character and must raise an exception"), // ----- unsupported object types --------------------------------------
        Arguments.of(new Date(0), IllegalArgumentException.class, "Date objects have no meaningful Character representation"), Arguments.of(Arrays.asList(1, 2, 3), IllegalArgumentException.class, "Collections are not supported for Character conversion"), // ----- CharSequence implementations ----------------------------------
        Arguments.of(new StringBuilder(""), IllegalArgumentException.class, "Empty CharSequence (StringBuilder) cannot be represented as a Character"), Arguments.of(new StringBuilder("ab"), IllegalArgumentException.class, "CharSequence longer than one character is ambiguous for Character conversion"), // ----- floating point special values --------------------------------
        Arguments.of(Double.NaN, IllegalArgumentException.class, "NaN cannot be mapped to a valid char"), Arguments.of(Float.POSITIVE_INFINITY, IllegalArgumentException.class, "Positive infinity is not a representable numeric value for a char"), Arguments.of(Float.NEGATIVE_INFINITY, IllegalArgumentException.class, "Negative infinity cannot be converted to a char"), // ----- BigDecimal edge cases -----------------------------------------
        Arguments.of(new BigDecimal("65.5"), IllegalArgumentException.class, "BigDecimal with a fractional part cannot be cleanly cast to char"), Arguments.of(new BigDecimal("70000"), IllegalArgumentException.class, "BigDecimal value exceeding Character.MAX_VALUE must trigger IllegalArgumentException"), // ----- BigInteger out of range ---------------------------------------
        Arguments.of(new BigInteger("70000"), IllegalArgumentException.class, "BigInteger larger than the char range should cause IllegalArgumentException"), Arguments.of(new BigInteger("-5"), IllegalArgumentException.class, "Negative BigInteger cannot be represented as a Character"), // ----- unsupported temporal types ------------------------------------
        Arguments.of(LocalDate.now(), IllegalArgumentException.class, "LocalDate has no meaningful Character representation"), Arguments.of(ZonedDateTime.now(), IllegalArgumentException.class, "ZonedDateTime cannot be converted to a Character"), // ----- Enum type -----------------------------------------------------
        Arguments.of(SampleEnum.VALUE, IllegalArgumentException.class, "Enum constants are not convertible to Character"));
    }

    @Test
    void convertNullReturnsNull() {
        Character result = Converter.convertToCharacter(null);
        assertNull(result, "Converting null should return null");
    }

    @Test
    void convertCharacterReturnsSameInstance() {
        Character input = 'Z';
        Character result = Converter.convertToCharacter(input);
        assertNotNull(result);
        assertEquals(input, result, "Converting a Character should return the same value");
    }

    @Test
    void convertSingleCharStringReturnsCharacter() {
        String input = "g";
        Character result = Converter.convertToCharacter(input);
        assertNotNull(result);
        assertEquals('g', result.charValue(), "String of length 1 should be converted to its character");
    }

    @Test
    void convertIntegerAsciiValueReturnsCorrespondingChar() {
        // ASCII for 'A'
        Integer input = 65;
        Character result = Converter.convertToCharacter(input);
        assertNotNull(result);
        assertEquals('A', result.charValue(), "Integer 65 should be converted to character 'A'");
    }

    @Test
    void convertLongAsciiValueReturnsCorrespondingChar() {
        // ASCII for 'b'
        Long input = 98L;
        Character result = Converter.convertToCharacter(input);
        assertNotNull(result);
        assertEquals('b', result.charValue(), "Long 98 should be converted to character 'b'");
    }

    @Test
    void convertByteAsciiValueReturnsCorrespondingChar() {
        // ASCII for 'C'
        Byte input = 67;
        Character result = Converter.convertToCharacter(input);
        assertNotNull(result);
        assertEquals('C', result.charValue(), "Byte 67 should be converted to character 'C'");
    }

    @Test
    void convertShortAsciiValueReturnsCorrespondingChar() {
        // ASCII for 'd'
        Short input = 100;
        Character result = Converter.convertToCharacter(input);
        assertNotNull(result);
        assertEquals('d', result.charValue(), "Short 100 should be converted to character 'd'");
    }

    @Test
    void convertBooleanIsNotSupportedReturnsNull() {
        // The library does not define a conversion from Boolean to Character,
        // so the expected result is null (no conversion found).
        Boolean input = Boolean.TRUE;
        Character result = Converter.convertToCharacter(input);
        assertNull(result, "Boolean to Character conversion is not supported and should return null");
    }

    @Test
    void convertUnsupportedObjectReturnsNull() {
        Object input = new Object();
        Character result = Converter.convertToCharacter(input);
        assertNull(result, "Conversion from an arbitrary Object to Character should return null");
    }

    /**
     * Parameterised test that executes all cases supplied by
     * {@link #provideConvertToCharacterCases()}.
     *
     * @param input       the object to be converted
     * @param expected    either a {@link Character}, {@code null}, or an exception {@link Class}
     * @param description a short description of the test case (used only for readability)
     */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideConvertToCharacterCases")
    void convertToCharacterParameterized(Object input, Object expected, String description) {
        if (expected == null) {
            // Expected a null result
            Character result = Converter.convertToCharacter(input);
            assertNull(result, description);
        } else if (expected instanceof Class && Throwable.class.isAssignableFrom((Class<?>) expected)) {
            // Expected an exception
            Class<? extends Throwable> exceptionClass = (Class<? extends Throwable>) expected;
            assertThrows(exceptionClass, () -> Converter.convertToCharacter(input), description);
        } else if (expected instanceof Character) {
            // Expected a concrete Character result
            Character result = Converter.convertToCharacter(input);
            assertNotNull(result, description);
            assertEquals(expected, result, description);
        } else {
            fail("Test configuration error: unexpected expected value type: " + expected);
        }
    }
}
