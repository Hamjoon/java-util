package com.cedarsoftware.util.convert;

import java.math.BigInteger;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toBigInteger_Test {

    @Mock
    private Converter // the method does not use the converter, but it must be supplied
    mockConverter;

    private static Stream<Arguments> conversionProvider() {
        return Stream.of(// true value, converter null
        Arguments.of(new AtomicBoolean(true), null, BigInteger.ONE), // false value, converter null
        Arguments.of(new AtomicBoolean(false), null, BigInteger.ZERO), // true value, non‑null (mock) converter
        Arguments.of(new AtomicBoolean(true), mock(Converter.class), BigInteger.ONE), // false value, non‑null (mock) converter
        Arguments.of(new AtomicBoolean(false), mock(Converter.class), BigInteger.ZERO));
    }

    @Test
    void toBigInteger_trueAtomicBoolean_returnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, mockConverter);
        // then
        assertEquals(BigInteger.ONE, result, "AtomicBoolean true should convert to BigInteger.ONE");
    }

    @Test
    void toBigInteger_falseAtomicBoolean_returnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        BigInteger result = AtomicBooleanConversions.toBigInteger(source, mockConverter);
        // then
        assertEquals(BigInteger.ZERO, result, "AtomicBoolean false should convert to BigInteger.ZERO");
    }

    @Test
    void toBigInteger_nonAtomicBoolean_throwsClassCastException() {
        // given
        Object notAtomic = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toBigInteger(notAtomic, mockConverter), "Passing a non‑AtomicBoolean should result in ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Parameterised conversion tests (expected normal results)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1} => expected={2}")
    @MethodSource("conversionProvider")
    void toBigInteger_convertsAtomicBooleanCorrectly(Object from, Converter converter, BigInteger expected) {
        // when
        BigInteger result = AtomicBooleanConversions.toBigInteger(from, converter);
        // then
        assertEquals(expected, result, () -> "AtomicBoolean value should convert to the expected BigInteger");
        // The method does not use the converter, so verify that no interaction occurred
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    /* -------------------------------------------------------------
     *  Exception handling tests
     * ------------------------------------------------------------- */
    @Test
    void toBigInteger_nullSource_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toBigInteger(source, mockConverter), "Passing a null source should throw NullPointerException");
    }
}
