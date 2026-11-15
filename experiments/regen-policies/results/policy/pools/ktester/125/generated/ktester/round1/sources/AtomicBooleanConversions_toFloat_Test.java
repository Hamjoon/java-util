package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toFloat_Test {

    /**
     * Mocked {@link Converter} – the method under test does not use it, but it must be supplied.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Minimal stub for a Converter – the implementation is irrelevant because the method under test never calls it.
     */
    private static class MockConverter implements Converter {
        // No methods are required for the test; the class exists solely to satisfy the type system.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (the @Mock field above).
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid any accidental interaction leakage between tests.
        reset(mockConverter);
    }

    private static Stream<Arguments> provideNormalBehavior() {
        return Stream.of(// true → 1.0f, converter is null (acceptable because it is unused)
        Arguments.of(new AtomicBoolean(true), null, CommonValues.FLOAT_ONE), // false → 0.0f, a non‑null Converter instance (also unused)
        Arguments.of(new AtomicBoolean(false), new MockConverter(), CommonValues.FLOAT_ZERO));
    }

    private static Stream<Arguments> provideConverterSideEffect() {
        // A mock Converter that throws on any method call – used to ensure the method under test never touches it.
        Converter throwing = mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter method should not be called");
        });
        return Stream.of(Arguments.of(new AtomicBoolean(true), throwing, CommonValues.FLOAT_ONE), Arguments.of(new AtomicBoolean(false), throwing, CommonValues.FLOAT_ZERO));
    }

    @Test
    void toFloat_returnsOneWhenAtomicBooleanIsTrue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Float result = AtomicBooleanConversions.toFloat(source, mockConverter);
        // then
        assertEquals(CommonValues.FLOAT_ONE, result, "AtomicBoolean true should be converted to Float 1.0");
    }

    @Test
    void toFloat_returnsZeroWhenAtomicBooleanIsFalse() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Float result = AtomicBooleanConversions.toFloat(source, mockConverter);
        // then
        assertEquals(CommonValues.FLOAT_ZERO, result, "AtomicBoolean false should be converted to Float 0.0");
    }

    @Test
    void toFloat_throwsClassCastExceptionWhenSourceIsNotAtomicBoolean() {
        // given
        Object notAtomic = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toFloat(notAtomic, mockConverter), "Passing a non‑AtomicBoolean should result in a ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Normal behavior
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}, expected={2}")
    @MethodSource("provideNormalBehavior")
    void toFloat_normalBehavior(AtomicBoolean from, Converter converter, Float expected) {
        // when
        Float result = AtomicBooleanConversions.toFloat(from, converter);
        // then
        assertEquals(expected, result, () -> "AtomicBoolean " + from.get() + " should be converted to " + expected);
    }

    /* -------------------------------------------------------------
     *  Null input
     * ------------------------------------------------------------- */
    @Test
    void toFloat_nullFrom_throwsNullPointerException() {
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toFloat(null, null), "Passing a null `from` should result in a NullPointerException");
    }

    /* -------------------------------------------------------------
     *  Invalid type
     * ------------------------------------------------------------- */
    @Test
    void toFloat_invalidFrom_throwsClassCastException() {
        // given
        Integer notAtomic = Integer.valueOf(5);
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toFloat(notAtomic, mockConverter), "Providing a non‑AtomicBoolean should cause a ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Converter side‑effect
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, throwingConverter, expected={2}")
    @MethodSource("provideConverterSideEffect")
    void toFloat_converterSideEffect(AtomicBoolean from, Converter throwingConverter, Float expected) {
        // when
        Float result = AtomicBooleanConversions.toFloat(from, throwingConverter);
        // then
        assertEquals(expected, result, () -> "Even with a throwing Converter, true should map to " + expected);
        // verify that the converter was never interacted with
        verifyNoInteractions(throwingConverter);
    }
}
