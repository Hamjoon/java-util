package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ByteConversions_toCharacter_Test {

    /**
     * A Converter instance used for the tests – the implementation is not exercised by the method.
     */
    private static Converter converter;

    /**
     * A real {@link Converter} instance – used for the few cases that explicitly require a non‑null value.
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter requires a ConverterOptions instance; default options are sufficient.
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    private static Stream<Arguments> validConversionProvider() {
        return Stream.of(// Convert Byte zero to character '\u0000'.
        arguments((byte) 0, null, '\u0000'), // Convert Byte 65 to character 'A'.
        arguments((byte) 65, null, 'A'), // Convert Byte -1 (0xFF) to character '\uffff'.
        arguments((byte) -1, null, '\uffff'), // Convert Byte max value 127 to character '\u007f'.
        arguments((byte) 127, null, '\u007f'), // Convert Byte minimum value -128 (0x80) to character '\u0080'.
        arguments((byte) -128, null, '\u0080'), // Convert Byte 0x7F (127) – same as above but expressed as a hex literal.
        arguments((byte) 0x7F, null, '\u007f'), // Non‑standard numeric type: (byte)255 wraps to -1 → '\uffff'.
        arguments((byte) 255, null, '\uffff'), // Converter usage (ignored) – supply a mock instead of {@code null}.
        arguments((byte) 65, Mockito.mock(Converter.class), 'A'));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// Passing null as 'from' should throw NullPointerException.
        arguments(null, NullPointerException.class), // Passing a non‑Byte object should throw ClassCastException.
        arguments("string", ClassCastException.class), // Attempt to pass an anonymous subclass of Byte – impossible because Byte is final.
        // Instead we simulate the scenario by using a proxy that does not extend Byte.
        // The method will still throw ClassCastException.
        arguments(Mockito.mock(Object.class), ClassCastException.class));
    }

    @Test
    void toCharacter_convertsPositiveByte() {
        // given
        // ASCII 'A'
        Byte input = (byte) 65;
        // when
        Character result = ByteConversions.toCharacter(input, converter);
        // then
        assertEquals('A', result);
    }

    @Test
    void toCharacter_convertsNegativeByte() {
        // given
        // 0xFF -> char 0xFFFF (65535)
        Byte input = (byte) -1;
        // when
        Character result = ByteConversions.toCharacter(input, converter);
        // then
        // Casting a signed byte to char yields the unsigned value of the byte.
        assertEquals((char) 0xFF, result);
    }

    @Test
    void toCharacter_convertsZeroByte() {
        // given
        Byte input = (byte) 0;
        // when
        Character result = ByteConversions.toCharacter(input, converter);
        // then
        assertEquals('\u0000', result);
    }

    @Test
    void toCharacter_nullInput_throwsNullPointerException() {
        // given
        Byte input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> ByteConversions.toCharacter(input, converter));
    }

    @Test
    void toCharacter_wrongType_throwsClassCastException() {
        // given
        Object notAByte = "not a byte";
        // when / then
        assertThrows(ClassCastException.class, () -> ByteConversions.toCharacter(notAByte, converter));
    }

    /* -------------------------------------------------------------
     *  Valid conversions – includes zero, positive, negative and
     *  boundary values.  The {@code converter} argument is {@code null}
     *  for most cases because the method does not depend on it.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → expected=''{2}''")
    @MethodSource("validConversionProvider")
    void toCharacter_validConversions(Object from, Converter converter, char expected) {
        // when
        Character result = ByteConversions.toCharacter(from, converter);
        // then
        assertEquals(expected, result);
    }

    /* -------------------------------------------------------------
     *  Invalid inputs – null {@code from} and wrong object type.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} expects {1}")
    @MethodSource("invalidInputProvider")
    void toCharacter_invalidInputs(Object from, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> ByteConversions.toCharacter(from, null));
    }

    /* -------------------------------------------------------------
     *  Additional sanity check – ensure that a non‑null Converter is
     *  truly ignored (the method never touches it).
     * ------------------------------------------------------------- */
    @Test
    void toCharacter_converterIsIgnored() {
        // given
        Byte input = (byte) 42;
        Converter mockConverter = Mockito.mock(Converter.class);
        // when
        Character result = ByteConversions.toCharacter(input, mockConverter);
        // then
        assertEquals((char) 42, result);
        // Verify that the mock was never interacted with.
        Mockito.verifyNoInteractions(mockConverter);
    }
}
