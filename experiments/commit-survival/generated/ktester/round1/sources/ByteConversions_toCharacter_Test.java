package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ByteConversions_toCharacter_Test {

    /**
     * Mocked {@link Converter} – not used by the method but required by the signature.
     */
    @Mock
    private Converter converter;

    private AutoCloseable mocks;

    /**
     * Mocked {@link Converter} – not used by the method but required by the signature.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple container for the arguments of a single test case.
     * It can hold either an expected character (for successful conversions)
     * or an expected exception class (for failure cases).
     */
    private static class ArgumentsHolder {

        final Object from;

        final Converter converter;

        final Character expectedChar;

        final Class<? extends Throwable> expectedException;

        final String description;

        // Constructor for successful conversion cases
        ArgumentsHolder(Object from, Converter converter, char expectedChar, String description) {
            this.from = from;
            this.converter = converter;
            this.expectedChar = expectedChar;
            this.expectedException = null;
            this.description = description;
        }

        // Constructor for exception cases
        ArgumentsHolder(Object from, Converter converter, Class<? extends Throwable> expectedException, String description) {
            this.from = from;
            this.converter = converter;
            this.expectedChar = null;
            this.expectedException = expectedException;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources.
        mocks.close();
    }

    /**
     * Valid byte conversions – includes the original simple tests and the extra cases.
     */
    static Stream<ArgumentsHolder> validByteConversions() {
        return Stream.of(// from, converter, expected char, description
        new ArgumentsHolder(Byte.valueOf((byte) 0), null, '\u0000', "Convert Byte value 0 to Character; expect the null character."), new ArgumentsHolder(Byte.valueOf((byte) 65), null, 'A', "Convert Byte value 65 (ASCII 'A') to Character; expect 'A'."), new ArgumentsHolder(Byte.valueOf((byte) -1), null, '\u00FF', "Convert Byte value -1 (0xFF) to Character; expect character with code point 255."), new ArgumentsHolder(Byte.valueOf((byte) -128), null, '\uFF80', "Convert Byte value -128 to Character; due to sign‑extension the resulting char code is 0xFF80."), new ArgumentsHolder(Byte.valueOf((byte) 127), null, '\u007F', "Convert Byte value 127 (DEL) to Character; expect character with code point 127."), new ArgumentsHolder(Byte.valueOf((byte) 42), new Converter() {

            @Override
            public String toString() {
                return "custom";
            }
        }, '*', "Pass a non‑null Converter (even though it is unused) with Byte value 42; expect '*'."), // Additional valid byte conversions
        new ArgumentsHolder(Byte.valueOf((byte) -2), null, '\u00FE', "Convert Byte value -2 (0xFE) to Character; expect character with Unicode code point 254."), new ArgumentsHolder(Byte.valueOf((byte) -127), null, '\uFF81', "Convert Byte value -127 (0x81) to Character; due to sign‑extension the resulting char code is 0xFF81."), new ArgumentsHolder(Byte.valueOf((byte) -64), null, '\uFFC0', "Convert Byte value -64 (0xC0) to Character; expect Unicode code point 0xFFC0."), new ArgumentsHolder(Byte.valueOf((byte) 1), null, '\u0001', "Convert Byte value 1 to Character; expect the control character with code point 1."), new ArgumentsHolder(Byte.valueOf((byte) 100), null, 'd', "Convert Byte value 100 to Character; ASCII 100 corresponds to the letter 'd'."), // Converter ignored variations
        new ArgumentsHolder(Byte.valueOf((byte) 90), new Converter() {

            @Override
            public String toString() {
                return "custom";
            }
        }, 'Z', "Pass a non‑null, overridden Converter instance while converting Byte value 90; the method should ignore the converter and return 'Z'."), new ArgumentsHolder(Byte.valueOf((byte) 55), new Converter() {

            public void someMethod() {
                throw new RuntimeException("Should not be called");
            }
        }, '7', "Provide a Converter whose methods would throw if invoked; toCharacter must not call any Converter method and should correctly return the character for byte 55 ('7')."));
    }

    /**
     * Invalid inputs that should trigger exceptions.
     */
    static Stream<ArgumentsHolder> invalidInputs() {
        return Stream.of(new ArgumentsHolder(null, null, NullPointerException.class, "Calling toCharacter with a null 'from' should throw NullPointerException when accessing byteValue()."), new ArgumentsHolder(null, new Converter() {
        }, NullPointerException.class, "Calling toCharacter with a null 'from' should still throw NullPointerException even when a non‑null Converter instance is supplied."), new ArgumentsHolder(Integer.valueOf(5), null, ClassCastException.class, "Passing an Integer instead of a Byte should cause a ClassCastException on the cast."), new ArgumentsHolder("some string", null, ClassCastException.class, "Passing a String instead of a Byte should cause a ClassCastException."), new ArgumentsHolder(Double.valueOf(5.0), null, ClassCastException.class, "Passing a Double object (not a Byte) should result in a ClassCastException."), new ArgumentsHolder(Boolean.TRUE, null, ClassCastException.class, "Passing a Boolean (true) instead of a Byte should trigger a ClassCastException."), new ArgumentsHolder(new Object(), null, ClassCastException.class, "Passing a generic Object that is not a Byte should cause a ClassCastException."));
    }

    @Test
    void toCharacter_givenByteWithinAsciiRange_returnsCorrespondingCharacter() {
        // GIVEN
        // ASCII for 'A'
        Byte input = 65;
        // WHEN
        Character result = ByteConversions.toCharacter(input, converter);
        // THEN
        assertEquals('A', result);
    }

    @Test
    void toCharacter_givenNegativeByte_returnsCharWithUnsignedValue() {
        // GIVEN
        // 0xFF as a signed byte
        Byte input = -1;
        // WHEN
        Character result = ByteConversions.toCharacter(input, converter);
        // THEN
        // (char) -1 == 0xFFFF (65535)
        assertEquals((char) -1, result);
    }

    @Test
    void toCharacter_givenNullObject_throwsNullPointerException() {
        // GIVEN
        Object input = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ByteConversions.toCharacter(input, converter));
    }

    @Test
    void toCharacter_givenNonByteObject_throwsClassCastException() {
        // GIVEN
        Object input = "not a byte";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ByteConversions.toCharacter(input, converter));
    }

    @Test
    void toCharacter_givenNullConverter_stillPerformsConversion() {
        // GIVEN
        // ASCII for '0'
        Byte input = 48;
        // WHEN
        Character result = ByteConversions.toCharacter(input, null);
        // THEN
        assertEquals('0', result);
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("validByteConversions")
    void toCharacter_validConversions(ArgumentsHolder holder) {
        // WHEN
        Character result = ByteConversions.toCharacter(holder.from, holder.converter);
        // THEN
        assertEquals(holder.expectedChar, result, () -> "Expected character '" + holder.expectedChar + "' but got '" + result + "'");
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("invalidInputs")
    void toCharacter_invalidInputs(ArgumentsHolder holder) {
        // WHEN / THEN
        assertThrows(holder.expectedException, () -> ByteConversions.toCharacter(holder.from, holder.converter));
    }
}
