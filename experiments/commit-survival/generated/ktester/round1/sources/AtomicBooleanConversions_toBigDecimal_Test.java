package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toBigDecimal_Test {

    @Mock
    private Converter // Converter is not used by the method but must be supplied
    mockConverter;

    @Test
    void toBigDecimal_whenAtomicBooleanTrue_returnsBigDecimalOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(BigDecimal.ONE, result, "AtomicBoolean true should convert to BigDecimal.ONE");
    }

    @Test
    void toBigDecimal_whenAtomicBooleanFalse_returnsBigDecimalZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(BigDecimal.ZERO, result, "AtomicBoolean false should convert to BigDecimal.ZERO");
    }

    @Test
    void toBigDecimal_whenSourceIsNotAtomicBoolean_throwsClassCastException() {
        // given
        Object notAtomic = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toBigDecimal(notAtomic, mockConverter), "Passing a non‑AtomicBoolean should result in ClassCastException");
    }

    @Test
    void toBigDecimal_whenSourceIsNull_throwsNullPointerException() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toBigDecimal(nullSource, mockConverter), "Passing null should result in NullPointerException due to cast");
    }

    @Test
    void toBigDecimal_whenConverterIsNull_andTrue_returnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        Converter nullConverter = null;
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, nullConverter);
        // then
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    void toBigDecimal_whenConverterIsNull_andFalse_returnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        Converter nullConverter = null;
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, nullConverter);
        // then
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void toBigDecimal_whenDefaultConstructedAtomicBoolean_returnsZero() {
        // given
        // defaults to false
        AtomicBoolean source = new AtomicBoolean();
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, mockConverter);
        // then
        assertEquals(BigDecimal.ZERO, result, "Default‑constructed AtomicBoolean (false) should convert to BigDecimal.ZERO");
    }

    @Test
    void toBigDecimal_whenConverterIsThrowing_doesNotInvokeIt_andReturnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // A mock that throws on any method call – verifies the method never uses it.
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, throwingConverter);
        // then
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    void toBigDecimal_whenConverterIsThrowing_doesNotInvokeIt_andReturnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, throwingConverter);
        // then
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void toBigDecimal_whenConverterIsIdentity_doesNotAffectResult_true() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // Identity behaviour is irrelevant – we just supply a mock.
        Converter identityConverter = mock(Converter.class);
        // No stubbing needed because the method never calls the converter.
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, identityConverter);
        // then
        assertEquals(BigDecimal.ONE, result);
    }

    @Test
    void toBigDecimal_whenConverterIsIdentity_doesNotAffectResult_false() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        Converter identityConverter = mock(Converter.class);
        // when
        BigDecimal result = AtomicBooleanConversions.toBigDecimal(source, identityConverter);
        // then
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void toBigDecimal_whenSourceIsWrongType_throwsClassCastException() {
        // given
        Object notAtomic = new AtomicInteger(5);
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toBigDecimal(notAtomic, mockConverter), "Providing an object of wrong type (AtomicInteger) should result in ClassCastException");
    }
}
