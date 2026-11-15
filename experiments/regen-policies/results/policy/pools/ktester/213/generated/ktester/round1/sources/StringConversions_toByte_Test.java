package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class StringConversions_toByte_Test {

    /**
     * Shared Converter instance – the class under test does not keep state, so a single instance is fine.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions object; the default options are sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // No external resources to release.
        converter = null;
    }

    /**
     * Provides a stream of arguments:
     * <ul>
     *   <li>input – the value passed as the {@code from} argument (may be {@code null}).</li>
     *   <li>converter – the {@link Converter} instance to use (null, a mock or the shared instance).</li>
     *   <li>expected – either a {@link Byte} (as a string) or the fully‑qualified exception class name.</li>
     * </ul>
     */
    private static Stream<Arguments> toByteProvider() {
        Converter mockConverter = Mockito.mock(Converter.class);
        return Stream.of(// Empty / null handling
        arguments("", null, (byte) 0), arguments(null, null, (byte) 0), // Direct parsing – within range
        arguments("127", null, (byte) 127), arguments("-128", null, (byte) -128), arguments("0", null, (byte) 0), arguments("+5", null, (byte) 5), arguments("5", null, (byte) 5), arguments("100", null, (byte) 100), // Leading zeros / plus sign
        arguments("00127", null, (byte) 127), arguments("+00127", null, (byte) 127), // Decimal strings – fallback toLong truncates toward zero
        arguments("12.34", null, (byte) 12), arguments("-12.34", null, (byte) -12), arguments("0.0", null, (byte) 0), arguments("-0.9", null, (byte) 0), // Values just outside the byte range after truncation
        arguments("130.5", null, IllegalArgumentException.class), arguments("-130.5", null, IllegalArgumentException.class), // Out‑of‑range integer strings
        arguments("128", null, IllegalArgumentException.class), arguments("-129", null, IllegalArgumentException.class), arguments("200", null, IllegalArgumentException.class), arguments("999999999999999999999", null, IllegalArgumentException.class), // Unsupported formats
        arguments("1e2", null, IllegalArgumentException.class), arguments("0x7F", null, IllegalArgumentException.class), // Whitespace handling – not trimmed by the method
        arguments(" 42 ", null, IllegalArgumentException.class), arguments(" +10 ", null, IllegalArgumentException.class), // Internal whitespace / control characters
        arguments("1 2", null, IllegalArgumentException.class), arguments("\n", null, IllegalArgumentException.class), arguments("\t", null, IllegalArgumentException.class), // Non‑String argument – should raise ClassCastException
        arguments(5, null, ClassCastException.class), // Explicit non‑null converter (ignored)
        arguments("100", mockConverter, (byte) 100));
    }

    @Test
    void toByte_EmptyString_ReturnsZero() {
        // given
        String source = "";
        // when
        Byte result = StringConversions.toByte(source, converter);
        // then
        assertEquals(Byte.valueOf((byte) 0), result);
    }

    @Test
    void toByte_ValidPositiveByteString_ReturnsByte() {
        // given
        String source = "127";
        // when
        Byte result = StringConversions.toByte(source, converter);
        // then
        assertEquals(Byte.valueOf((byte) 127), result);
    }

    @Test
    void toByte_ValidNegativeByteString_ReturnsByte() {
        // given
        String source = "-128";
        // when
        Byte result = StringConversions.toByte(source, converter);
        // then
        assertEquals(Byte.valueOf((byte) -128), result);
    }

    @Test
    void toByte_ValueOutsideByteRange_ThrowsIllegalArgumentException() {
        // given
        // larger than Byte.MAX_VALUE
        String source = "200";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toByte(source, converter));
        assertTrue(ex.getMessage().contains("not parseable as a byte value"));
    }

    @Test
    void toByte_NonNumericString_ThrowsIllegalArgumentException() {
        // given
        String source = "abc123";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toByte(source, converter));
        assertTrue(ex.getMessage().contains("not parseable as a byte value"));
    }

    @Test
    void toByte_NonStringArgument_ThrowsClassCastException() {
        // given
        // not a String
        Object source = 123;
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toByte(source, converter));
    }

    @ParameterizedTest(name = "[{index}] from=\"{0}\" → expected={2}")
    @MethodSource("toByteProvider")
    void toByte_VariousScenarios(Object from, Converter conv, Object expected) {
        // -------------------------------------------------------------
        // Arrange – choose the converter to pass (null, mock or shared)
        // -------------------------------------------------------------
        // may be null – the method tolerates it
        Converter usedConverter = conv;
        // -------------------------------------------------------------
        // Act / Assert – decide based on the expected type
        // -------------------------------------------------------------
        if (expected instanceof Byte) {
            Byte result = StringConversions.toByte(from, usedConverter);
            assertEquals(expected, result);
        } else if (expected instanceof Class && IllegalArgumentException.class.equals(expected)) {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toByte(from, usedConverter));
            assertTrue(ex.getMessage().contains("not parseable as a byte value"));
        } else if (expected instanceof Class && ClassCastException.class.equals(expected)) {
            assertThrows(ClassCastException.class, () -> StringConversions.toByte(from, usedConverter));
        } else {
            fail("Unexpected expected value: " + expected);
        }
    }
}
