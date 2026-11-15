package com.cedarsoftware.util.convert;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toBigInteger_Test {

    /**
     * The {@link Converter} dependency is not used by the method under test, but we provide a mock
     *  to satisfy the method signature.
     */
    @Mock
    Converter converter;

    /**
     * The {@link Converter} dependency is not used by the method under test,
     *  but we provide a mock to satisfy the method signature where appropriate.
     */
    @Mock
    Converter mockConverter;

    /**
     * A minimal stub implementation of {@link Converter}.  The real
     *  interface is unknown for this exercise, but an empty class is enough
     *  because the method under test never calls any of its methods.
     */
    private static class DummyConverter implements Converter {
        // No behaviour required.
    }

    /**
     * Provides the combinations of {@code AtomicBoolean} values and
     *  different {@link Converter} instances that should all produce the
     *  expected {@link BigInteger} result.
     */
    private static Stream<Arguments> booleanAndConverterProvider() {
        Converter dummyConverter = new DummyConverter();
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter invoked");
        });
        return Stream.of(// from,                converter,          expected
        Arguments.of(new AtomicBoolean(true), null, BigInteger.ONE), Arguments.of(new AtomicBoolean(false), null, BigInteger.ZERO), Arguments.of(new AtomicBoolean(true), dummyConverter, BigInteger.ONE), Arguments.of(new AtomicBoolean(false), dummyConverter, BigInteger.ZERO), Arguments.of(new AtomicBoolean(true), throwingConverter, BigInteger.ONE), Arguments.of(new AtomicBoolean(false), throwingConverter, BigInteger.ZERO));
    }

    @Test
    void toBigInteger_whenAtomicBooleanTrue_returnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, converter);
        // then
        assertEquals(BigInteger.ONE, result, "Expected ONE for true AtomicBoolean");
    }

    @Test
    void toBigInteger_whenAtomicBooleanFalse_returnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, converter);
        // then
        assertEquals(BigInteger.ZERO, result, "Expected ZERO for false AtomicBoolean");
    }

    @Test
    void toBigInteger_whenNullSource_throwsClassCastException() {
        // given
        Object source = null;
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toBigInteger(source, converter), "Null source should cause ClassCastException");
    }

    @Test
    void toBigInteger_whenNonAtomicBooleanSource_throwsClassCastException() {
        // given
        Object source = "not an AtomicBoolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toBigInteger(source, converter), "Non‑AtomicBoolean source should cause ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1} → expected={2}")
    @MethodSource("booleanAndConverterProvider")
    void toBigInteger_variousInputs_returnsExpectedBigInteger(AtomicBoolean from, Converter converter, BigInteger expected) {
        // when
        BigInteger result = AtomicBooleanConversions.toBigInteger(from, converter);
        // then
        assertEquals(expected, result, () -> "Conversion of AtomicBoolean(" + from.get() + ") should yield " + expected);
    }

    @Test
    void toBigInteger_whenSourceIsNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toBigInteger(source, mockConverter), "A null source should cause NullPointerException when .get() is invoked");
    }

    @Test
    void toBigInteger_whenSourceIsWrongType_throwsClassCastException() {
        // given
        // any non‑AtomicBoolean type
        Object source = new AtomicInteger(5);
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toBigInteger(source, mockConverter), "A non‑AtomicBoolean source should cause ClassCastException");
    }
}
