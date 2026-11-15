package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toAtomicBoolean_Test {

    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not affect the behaviour of toAtomicBoolean, but we provide a real instance
        // to satisfy the method signature.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    private static Stream<Arguments> normalCasesProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), true), Arguments.of(new AtomicBoolean(false), false));
    }

    private static Stream<Arguments> nonAtomicSourceProvider() {
        return Stream.of(Arguments.of("true"), Arguments.of(Integer.valueOf(1)));
    }

    @Test
    void shouldCopyTrueValueIntoNewAtomicBoolean() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, converter);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.get(), "Result should contain true");
        assertNotSame(source, result, "Result must be a new instance, not the original");
    }

    @Test
    void shouldCopyFalseValueIntoNewAtomicBoolean() {
        AtomicBoolean source = new AtomicBoolean(false);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, converter);
        assertNotNull(result, "Result must not be null");
        assertFalse(result.get(), "Result should contain false");
        assertNotSame(source, result, "Result must be a new instance, not the original");
    }

    @Test
    void shouldThrowNullPointerExceptionWhenSourceIsNull() {
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toAtomicBoolean(null, converter), "Null source should cause NullPointerException");
    }

    @Test
    void shouldThrowClassCastExceptionWhenSourceIsNotAtomicBoolean() {
        Object notAtomic = "I am not an AtomicBoolean";
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicBoolean(notAtomic, converter), "Non‑AtomicBoolean source should cause ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Normal conversion cases – true and false values.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] source={0}, expected={1}")
    @MethodSource("normalCasesProvider")
    void shouldCopyValueIntoNewAtomicBoolean(AtomicBoolean source, boolean expected) {
        // Act
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, null);
        // Assert
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), "Result should contain the same boolean value as the source");
        assertNotSame(source, result, "Result must be a new instance, not the original");
    }

    /* -------------------------------------------------------------
     *  Converter argument is ignored – using a real Converter must not
     *  change the outcome.
     * ------------------------------------------------------------- */
    @Test
    void shouldIgnoreConverterArgument() {
        AtomicBoolean source = new AtomicBoolean(true);
        // Pass the real converter created in @BeforeAll
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, converter);
        assertNotNull(result, "Result must not be null");
        assertTrue(result.get(), "Result should contain true");
        assertNotSame(source, result, "Result must be a new instance");
    }

    /* -------------------------------------------------------------
     *  Independence of the returned instance.
     *
     *  1. Mutate the source *before* conversion – the returned value must
     *     reflect the current state of the source at conversion time.
     *
     *  2. Mutate the source *after* conversion – the returned instance must
     *     retain the value captured during conversion (deep copy semantics).
     * ------------------------------------------------------------- */
    @Test
    void shouldReflectCurrentValueWhenSourceChangedBeforeConversion() {
        AtomicBoolean source = new AtomicBoolean(false);
        // change before conversion
        source.set(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, null);
        assertTrue(result.get(), "Result should reflect the source's value at conversion time (true)");
        assertNotSame(source, result, "Result must be a distinct object");
    }

    @Test
    void shouldRemainUnchangedWhenSourceMutatedAfterConversion() {
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, null);
        // mutate after conversion
        source.set(false);
        assertTrue(result.get(), "Result must retain the original value (true) even after the source is mutated");
        assertNotSame(source, result, "Result must be a distinct object");
    }
}
