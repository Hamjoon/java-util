package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

class AtomicBooleanConversions_toBoolean_Test {

    /**
     * Shared Converter instance – the method under test does not actually use it,
     *  but it must be supplied to match the signature.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Use default options – no special configuration required for this test.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static reference – not strictly necessary but keeps the pattern tidy.
        converter = null;
    }

    private static Stream<Arguments> validAtomicBooleanProvider() {
        // a real Converter instance (null is also acceptable – the method never uses it)
        Converter mockConverter = mock(Converter.class);
        return Stream.of(// converter == null
        Arguments.of(new AtomicBoolean(true), null, true), Arguments.of(new AtomicBoolean(false), null, false), // mock converter – should be ignored
        Arguments.of(new AtomicBoolean(true), mockConverter, true), Arguments.of(new AtomicBoolean(false), mockConverter, false));
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(// null source → NullPointerException when casting
        Arguments.of(null, NullPointerException.class), // wrong type → ClassCastException during cast
        Arguments.of(Integer.valueOf(5), ClassCastException.class));
    }

    private static Stream<Arguments> validAtomicBooleanWithBadConverterProvider() {
        // Converter that throws on any method call
        Answer<Object> throwingAnswer = invocation -> {
            throw new RuntimeException("Converter should not be invoked");
        };
        Converter throwingConverter = mock(Converter.class, throwingAnswer);
        // Converter that returns a non‑boolean value from its convert method
        Converter nonBooleanConverter = mock(Converter.class);
        when(nonBooleanConverter.convert(any())).thenReturn("not a boolean");
        return Stream.of(Arguments.of(new AtomicBoolean(true), throwingConverter, true), Arguments.of(new AtomicBoolean(false), throwingConverter, false), Arguments.of(new AtomicBoolean(true), nonBooleanConverter, true), Arguments.of(new AtomicBoolean(false), nonBooleanConverter, false));
    }

    @Test
    void toBoolean_returnsTrueWhenAtomicBooleanIsTrue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        boolean result = AtomicBooleanConversions.toBoolean(source, converter);
        // then
        assertTrue(result, "Expected true when source AtomicBoolean holds true");
    }

    @Test
    void toBoolean_returnsFalseWhenAtomicBooleanIsFalse() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        boolean result = AtomicBooleanConversions.toBoolean(source, converter);
        // then
        assertFalse(result, "Expected false when source AtomicBoolean holds false");
    }

    @Test
    void toBoolean_throwsNullPointerExceptionWhenSourceIsNull() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toBoolean(source, converter), "Expected NPE when source object is null");
    }

    @Test
    void toBoolean_throwsClassCastExceptionWhenSourceIsNotAtomicBoolean() {
        // given
        Object source = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toBoolean(source, converter), "Expected CCE when source is not an AtomicBoolean");
    }

    /* -------------------------------------------------------------
     *  Group 1 – Valid AtomicBoolean inputs (converter = null or mock)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => atomic={0}, converter={1}, expected={2}")
    @MethodSource("validAtomicBooleanProvider")
    @DisplayName("Valid AtomicBoolean inputs – should return the stored boolean")
    void toBoolean_validAtomicBooleanInputs(AtomicBoolean source, Converter suppliedConverter, boolean expected) {
        // when
        boolean result = AtomicBooleanConversions.toBoolean(source, suppliedConverter);
        // then
        assertEquals(expected, result, () -> "Expected " + expected + " when source holds " + source.get());
    }

    /* -------------------------------------------------------------
     *  Group 2 – Invalid inputs causing exceptions
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}, expectedException={1}")
    @MethodSource("invalidInputProvider")
    @DisplayName("Invalid inputs – should throw the appropriate exception")
    void toBoolean_invalidInputs(Object from, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> AtomicBooleanConversions.toBoolean(from, converter), () -> "Expected " + expectedException.getSimpleName() + " for input: " + from);
    }

    /* -------------------------------------------------------------
     *  Group 3 – Valid AtomicBoolean inputs with non‑null Converter
     *            (converter that would throw or return non‑boolean values)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => atomic={0}, converterDesc={1}, expected={2}")
    @MethodSource("validAtomicBooleanWithBadConverterProvider")
    @DisplayName("Valid AtomicBoolean with a misbehaving Converter – should ignore Converter")
    void toBoolean_validAtomicBooleanWithBadConverter(AtomicBoolean source, Converter badConverter, boolean expected) {
        // when
        boolean result = AtomicBooleanConversions.toBoolean(source, badConverter);
        // then
        assertEquals(expected, result, () -> "Result should be the AtomicBoolean's value even when Converter misbehaves");
    }

    /* -------------------------------------------------------------
     *  Group 4 – AtomicBoolean subclass that throws from get()
     * ------------------------------------------------------------- */
    @Test
    @DisplayName("AtomicBoolean subclass overriding get() to throw – exception should propagate")
    void toBoolean_atomicBooleanSubclassThrows() {
        // given: anonymous subclass that throws from get()
        AtomicBoolean badAtomic = new AtomicBoolean(true) {

            @Override
            public boolean get() {
                throw new RuntimeException("forced");
            }
        };
        // when / then
        RuntimeException ex = assertThrows(RuntimeException.class, () -> AtomicBooleanConversions.toBoolean(badAtomic, converter), "Expected RuntimeException propagated from overridden get()");
        assertEquals("forced", ex.getMessage(), "Exception message should match the one thrown from the subclass");
    }
}
