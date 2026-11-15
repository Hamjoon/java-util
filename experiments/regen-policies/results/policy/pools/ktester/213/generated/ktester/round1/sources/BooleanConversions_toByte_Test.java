package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toByte_Test {

    /**
     * Converter instance required by the method signature – its behavior is irrelevant here.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Converter has a constructor that requires ConverterOptions.
        // Using default options is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and avoid accidental reuse.
        converter = null;
    }

    /**
     * Supplies test data matching the JSON description.
     *
     * <ul>
     *   <li>Boolean.TRUE → BYTE_ONE (1)</li>
     *   <li>Boolean.FALSE → BYTE_ZERO (0)</li>
     *   <li>null source → NullPointerException</li>
     *   <li>non‑Boolean source → ClassCastException</li>
     *   <li>Boolean.TRUE with a mocked Converter → BYTE_ONE (1)</li>
     * </ul>
     *
     * @return a stream of arguments for {@link #toByte_variations(Object, Converter, Object)}
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> toByteProvider() {
        // Mocked converter for the “dummyConverter” case
        Converter dummyConverter = org.mockito.Mockito.mock(Converter.class);
        return Stream.of(// true → BYTE_ONE
        arguments(Boolean.TRUE, null, CommonValues.BYTE_ONE), // false → BYTE_ZERO
        arguments(Boolean.FALSE, null, CommonValues.BYTE_ZERO), // null source → NPE
        arguments(null, null, NullPointerException.class), // non‑Boolean source → ClassCastException
        arguments("true", null, ClassCastException.class), // true with dummy (non‑null) converter → BYTE_ONE
        arguments(Boolean.TRUE, dummyConverter, CommonValues.BYTE_ONE));
    }

    @Test
    @DisplayName("toByte(true) returns BYTE_ONE")
    void toByte_true_returnsByteOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        Byte result = BooleanConversions.toByte(source, converter);
        // then
        assertEquals(CommonValues.BYTE_ONE, result, "Converting Boolean.TRUE should yield CommonValues.BYTE_ONE");
    }

    @Test
    @DisplayName("toByte(false) returns BYTE_ZERO")
    void toByte_false_returnsByteZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        Byte result = BooleanConversions.toByte(source, converter);
        // then
        assertEquals(CommonValues.BYTE_ZERO, result, "Converting Boolean.FALSE should yield CommonValues.BYTE_ZERO");
    }

    @Test
    @DisplayName("toByte(null) throws NullPointerException")
    void toByte_null_throwsNPE() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toByte(source, converter), "Passing null should result in a NullPointerException");
    }

    @Test
    @DisplayName("toByte(non‑Boolean) throws ClassCastException")
    void toByte_nonBoolean_throwsClassCastException() {
        // given
        Object source = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toByte(source, converter), "Passing a non‑Boolean object should result in a ClassCastException");
    }

    /* --------------------------------------------------------------
     *  Parameterized tests covering the JSON test‑case matrix
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} ⇒ from={0}, converter={1}, expected={2}")
    @MethodSource("toByteProvider")
    void toByte_variations(Object from, Converter suppliedConverter, Object expected) {
        // -----------------------------------------------------------------
        // Arrange
        // -----------------------------------------------------------------
        // If the test case supplies a dummy converter, use a mock; otherwise
        // fall back to the real converter created in @BeforeEach.
        Converter conv = suppliedConverter != null ? suppliedConverter : converter;
        // -----------------------------------------------------------------
        // Act & Assert
        // -----------------------------------------------------------------
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> BooleanConversions.toByte(from, conv), () -> "Expected " + exClass.getSimpleName() + " for input: " + from);
        } else {
            // Expected a concrete Byte value
            Byte result = BooleanConversions.toByte(from, conv);
            assertEquals(expected, result, () -> "Unexpected conversion result for input: " + from);
        }
    }

    @Test
    @DisplayName("toByte(true) with a mocked Converter still returns BYTE_ONE")
    void toByte_true_withMockedConverter_returnsByteOne(@Mock Converter mockConverter) {
        Byte result = BooleanConversions.toByte(Boolean.TRUE, mockConverter);
        assertEquals(CommonValues.BYTE_ONE, result, "Even with a mocked Converter the method should return BYTE_ONE for Boolean.TRUE");
    }
}
