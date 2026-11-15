package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toByte_Test {

    @Mock
    private Converter // not used by the method but required by the signature
    converter;

    private static Stream<Arguments> provideAtomicBooleanValues() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), CommonValues.BYTE_ONE), Arguments.of(new AtomicBoolean(false), CommonValues.BYTE_ZERO));
    }

    @Test
    void toByte_whenAtomicBooleanTrue_returnsByteOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Byte result = AtomicBooleanConversions.toByte(source, converter);
        // then
        assertEquals(CommonValues.BYTE_ONE, result, "AtomicBoolean true should be converted to BYTE_ONE");
    }

    @Test
    void toByte_whenAtomicBooleanFalse_returnsByteZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Byte result = AtomicBooleanConversions.toByte(source, converter);
        // then
        assertEquals(CommonValues.BYTE_ZERO, result, "AtomicBoolean false should be converted to BYTE_ZERO");
    }

    @Test
    void toByte_whenFromIsNull_throwsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toByte(from, null), "Passing null as the source should throw NullPointerException");
    }

    @Test
    void toByte_whenFromIsNotAtomicBoolean_throwsClassCastException() {
        // given
        Object from = Integer.valueOf(1);
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toByte(from, null), "A non‑AtomicBoolean source should cause a ClassCastException");
    }

    @Test
    void toByte_ignoresConverter_argument() {
        // given a true AtomicBoolean
        AtomicBoolean source = new AtomicBoolean(true);
        // a Converter that would throw if its convert method were called
        Converter badConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // when
        Byte result = AtomicBooleanConversions.toByte(source, badConverter);
        // then
        assertEquals(CommonValues.BYTE_ONE, result, "Converter argument is ignored; result should still be BYTE_ONE");
    }

    @Test
    void toByte_subclassAtomicBooleanOverridesGet_returnsByteOne() {
        // given an anonymous subclass that forces get() to true
        AtomicBoolean source = new AtomicBoolean(true) {

            @Override
            public boolean get() {
                return true;
            }
        };
        // when
        Byte result = AtomicBooleanConversions.toByte(source, null);
        // then
        assertEquals(CommonValues.BYTE_ONE, result, "Subclass overriding get() to true must return BYTE_ONE");
    }

    @Test
    void toByte_subclassAtomicBooleanOverridesGet_returnsByteZero() {
        // given an anonymous subclass that forces get() to false
        AtomicBoolean source = new AtomicBoolean(false) {

            @Override
            public boolean get() {
                return false;
            }
        };
        // when
        Byte result = AtomicBooleanConversions.toByte(source, null);
        // then
        assertEquals(CommonValues.BYTE_ZERO, result, "Subclass overriding get() to false must return BYTE_ZERO");
    }

    @Test
    void toByte_subclassAtomicBooleanGetThrows_propagatesException() {
        // given an anonymous subclass whose get() throws IllegalStateException
        AtomicBoolean source = new AtomicBoolean(true) {

            @Override
            public boolean get() {
                throw new IllegalStateException("forced exception");
            }
        };
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> AtomicBooleanConversions.toByte(source, null), "Exception from overridden get() should propagate");
        assertEquals("forced exception", ex.getMessage());
    }

    @Test
    void toByte_afterMutatingAtomicBoolean_reflectsCurrentValue_true() {
        // given an AtomicBoolean initially true, then set to false
        AtomicBoolean source = new AtomicBoolean(true);
        // mutate
        source.set(false);
        // when
        Byte result = AtomicBooleanConversions.toByte(source, null);
        // then
        assertEquals(CommonValues.BYTE_ZERO, result, "Method must read the current value after mutation (false → BYTE_ZERO)");
    }

    @Test
    void toByte_afterMutatingAtomicBoolean_reflectsCurrentValue_false() {
        // given an AtomicBoolean initially false, then set to true
        AtomicBoolean source = new AtomicBoolean(false);
        // mutate
        source.set(true);
        // when
        Byte result = AtomicBooleanConversions.toByte(source, null);
        // then
        assertEquals(CommonValues.BYTE_ONE, result, "Method must read the current value after mutation (true → BYTE_ONE)");
    }

    @ParameterizedTest(name = "toByte({0}) => {1}")
    @MethodSource("provideAtomicBooleanValues")
    void toByte_parameterized(AtomicBoolean input, Byte expected) {
        // when
        Byte result = AtomicBooleanConversions.toByte(input, null);
        // then
        assertEquals(expected, result);
    }
}
