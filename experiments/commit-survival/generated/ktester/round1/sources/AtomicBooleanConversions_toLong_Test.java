package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toLong_Test {

    /**
     * Mocked {@link Converter} – not used by the method but required by the signature.
     */
    @Mock
    private Converter converter;

    /**
     * Mocked {@link Converter} – the method does not use it but the signature requires a value.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> normalConversionProvider() {
        return Stream.of(arguments(new AtomicBoolean(true), CommonValues.LONG_ONE), arguments(new AtomicBoolean(false), CommonValues.LONG_ZERO));
    }

    private static Stream<Arguments> converterIgnoredProvider() {
        return Stream.of(arguments(new AtomicBoolean(true), CommonValues.LONG_ONE), arguments(new AtomicBoolean(false), CommonValues.LONG_ZERO));
    }

    @Test
    @DisplayName("toLong returns LONG_ONE when AtomicBoolean is true")
    void toLong_returnsOne_whenTrue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Long result = AtomicBooleanConversions.toLong(source, converter);
        // then
        assertEquals(CommonValues.LONG_ONE, result, "Expected LONG_ONE for a true AtomicBoolean");
    }

    @Test
    @DisplayName("toLong returns LONG_ZERO when AtomicBoolean is false")
    void toLong_returnsZero_whenFalse() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Long result = AtomicBooleanConversions.toLong(source, converter);
        // then
        assertEquals(CommonValues.LONG_ZERO, result, "Expected LONG_ZERO for a false AtomicBoolean");
    }

    @Test
    @DisplayName("toLong throws ClassCastException for non‑AtomicBoolean input")
    void toLong_throwsClassCastException_whenNotAtomicBoolean() {
        // given
        Object notAtomic = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toLong(notAtomic, converter), "Expected ClassCastException when source is not an AtomicBoolean");
    }

    @Test
    @DisplayName("toLong throws NullPointerException when source is null")
    void toLong_throwsNullPointerException_whenSourceIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toLong(nullSource, converter), "Expected NullPointerException when source is null");
    }

    /* ----------------------------------------------------------------------
     *  Normal conversion – null Converter (group: "Normal conversion")
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}, expected={2}")
    @MethodSource("normalConversionProvider")
    @DisplayName("toLong normal conversion with null Converter")
    void toLong_normalConversion_nullConverter(AtomicBoolean from, Long expected) {
        Long result = AtomicBooleanConversions.toLong(from, null);
        assertEquals(expected, result, "Conversion result does not match expected value");
    }

    /* ----------------------------------------------------------------------
     *  Converter influence – non‑null Converter (group: "Converter influence")
     * ---------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => from={0}, expected={2}")
    @MethodSource("converterIgnoredProvider")
    @DisplayName("toLong conversion ignores supplied non‑null Converter")
    void toLong_ignoresConverter(AtomicBoolean from, Long expected) {
        Long result = AtomicBooleanConversions.toLong(from, mockConverter);
        assertEquals(expected, result, "Result should be independent of the supplied Converter");
    }

    /* ----------------------------------------------------------------------
     *  Reference equality with CommonValues constants (group: "Reference equality")
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("toLong returns the exact LONG_ONE instance")
    void toLong_returnsReference_LONG_ONE() {
        AtomicBoolean src = new AtomicBoolean(true);
        Long result = AtomicBooleanConversions.toLong(src, null);
        assertSame(CommonValues.LONG_ONE, result, "Method must return the exact CommonValues.LONG_ONE instance");
    }

    @Test
    @DisplayName("toLong returns the exact LONG_ZERO instance")
    void toLong_returnsReference_LONG_ZERO() {
        AtomicBoolean src = new AtomicBoolean(false);
        Long result = AtomicBooleanConversions.toLong(src, null);
        assertSame(CommonValues.LONG_ZERO, result, "Method must return the exact CommonValues.LONG_ZERO instance");
    }

    /* ----------------------------------------------------------------------
     *  Exception scenarios (group: "Exception scenarios")
     * ---------------------------------------------------------------------- */
    @Test
    @DisplayName("toLong throws NullPointerException when source is null (null Converter)")
    void toLong_nullSource_nullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toLong(null, null), "Null source should cause NullPointerException");
    }

    @Test
    @DisplayName("toLong throws NullPointerException when source is null (non‑null Converter)")
    void toLong_nullSource_nonNullConverter_throwsNPE() {
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toLong(null, mockConverter), "Null source should cause NullPointerException even with a Converter");
    }

    @Test
    @DisplayName("toLong throws ClassCastException for non‑AtomicBoolean input")
    void toLong_nonAtomicBoolean_throwsClassCastException() {
        Object notAtomic = "not an atomic boolean";
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toLong(notAtomic, null), "Providing a non‑AtomicBoolean should trigger ClassCastException");
    }
}
