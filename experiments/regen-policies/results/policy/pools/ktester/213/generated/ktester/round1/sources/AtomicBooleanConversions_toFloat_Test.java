package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

// Mockito not strictly required but kept from template
@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toFloat_Test {

    /**
     * Shared Converter instance used by the tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real Converter is not needed for toFloat, but we initialise it to honour the original
        // test‑class contract.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference.
        converter = null;
    }

    @Test
    void toFloat_returnsOneWhenAtomicBooleanTrue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Float result = AtomicBooleanConversions.toFloat(source, converter);
        // then
        assertEquals(Float.valueOf(1.0f), result, "AtomicBoolean true should convert to Float 1.0");
    }

    @Test
    void toFloat_returnsZeroWhenAtomicBooleanFalse() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Float result = AtomicBooleanConversions.toFloat(source, converter);
        // then
        assertEquals(Float.valueOf(0.0f), result, "AtomicBoolean false should convert to Float 0.0");
    }

    @Test
    void toFloat_throwsNPEWhenSourceIsNull() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toFloat(source, null), "Passing null for 'from' should result in NullPointerException");
    }

    @Test
    void toFloat_throwsClassCastExceptionWhenSourceIsNotAtomicBoolean() {
        // given
        Object source = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toFloat(source, converter), "Passing a non‑AtomicBoolean should result in ClassCastException");
    }

    /**
     * Parameterized test covering the two valid conversion paths:
     * <ul>
     *   <li>{@code true}  → {@code 1.0f}</li>
     *   <li>{@code false} → {@code 0.0f}</li>
     * </ul>
     *
     * The {@code converter} argument is passed as {@code null} because the method
     * does not use it.
     */
    @ParameterizedTest(name = "AtomicBoolean({0}) should convert to Float {1}")
    @CsvSource({ "true, 1.0", "false, 0.0" })
    void toFloat_returnsExpectedValue(boolean atomicValue, float expectedFloat) {
        // given
        AtomicBoolean source = new AtomicBoolean(atomicValue);
        // when
        Float result = AtomicBooleanConversions.toFloat(source, null);
        // then
        assertEquals(Float.valueOf(expectedFloat), result, () -> "AtomicBoolean " + atomicValue + " should convert to Float " + expectedFloat);
    }
}
