package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toBigInteger_Test {

    /**
     * A mock Converter – the implementation of {@code toBigInteger} does not use it,
     *  but the method signature requires it.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> validConverterProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, BigInteger.ONE), Arguments.of(Boolean.FALSE, BigInteger.ZERO));
    }

    /**
     * Helper that uses reflection to read the private {@code invoked} flag from the anonymous
     * {@link Converter} implementation used in the side‑effect tests.
     */
    private boolean wasConverterInvoked(Converter converter) {
        try {
            var field = converter.getClass().getDeclaredField("invoked");
            field.setAccessible(true);
            return field.getBoolean(converter);
        } catch (ReflectiveOperationException e) {
            fail("Unable to inspect converter's invoked flag", e);
            // unreachable
            return true;
        }
    }

    @Test
    void toBigInteger_True_ReturnsOne() {
        // given
        Object from = Boolean.TRUE;
        // when
        BigInteger result = BooleanConversions.toBigInteger(from, mockConverter);
        // then
        assertEquals(BigInteger.ONE, result, "Boolean true should convert to BigInteger.ONE");
    }

    @Test
    void toBigInteger_False_ReturnsZero() {
        // given
        Object from = Boolean.FALSE;
        // when
        BigInteger result = BooleanConversions.toBigInteger(from, mockConverter);
        // then
        assertEquals(BigInteger.ZERO, result, "Boolean false should convert to BigInteger.ZERO");
    }

    @Test
    void toBigInteger_Null_ThrowsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toBigInteger(from, mockConverter), "Passing null should result in NullPointerException");
    }

    @Test
    void toBigInteger_NonBoolean_ThrowsClassCastException() {
        // given
        Object from = "true";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toBigInteger(from, mockConverter), "Passing a non‑Boolean should result in ClassCastException");
    }

    @Test
    void toBigInteger_True_WithNullConverter_ReturnsOne() {
        // given
        Object from = Boolean.TRUE;
        Converter converter = null;
        // when
        BigInteger result = BooleanConversions.toBigInteger(from, converter);
        // then
        assertEquals(BigInteger.ONE, result, "Boolean.TRUE should be converted to BigInteger.ONE even when converter is null");
    }

    @Test
    void toBigInteger_False_WithNullConverter_ReturnsZero() {
        // given
        Object from = Boolean.FALSE;
        Converter converter = null;
        // when
        BigInteger result = BooleanConversions.toBigInteger(from, converter);
        // then
        assertEquals(BigInteger.ZERO, result, "Boolean.FALSE should be converted to BigInteger.ZERO even when converter is null");
    }

    @Test
    void toBigInteger_NullSource_ThrowsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toBigInteger(from, mockConverter), "Passing null as the source should throw NullPointerException");
    }

    @Test
    void toBigInteger_NonBooleanSource_ThrowsClassCastException() {
        // given
        Object from = Integer.valueOf(1);
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toBigInteger(from, mockConverter), "Passing a non‑Boolean should result in ClassCastException");
    }

    @ParameterizedTest(name = "{index}: from={0}, expected={1}")
    @MethodSource("validConverterProvider")
    void toBigInteger_WithValidConverter_IgnoresConverter(Object from, BigInteger expected) {
        // given
        Converter converter = new Converter() {
        };
        // when
        BigInteger result = BooleanConversions.toBigInteger(from, converter);
        // then
        assertEquals(expected, result, "Converter should be ignored; result must depend only on Boolean value");
    }

    @Test
    void toBigInteger_NewBooleanTrue_ReturnsOne() {
        Object from = new Boolean(true);
        BigInteger result = BooleanConversions.toBigInteger(from, mockConverter);
        assertEquals(BigInteger.ONE, result, "new Boolean(true) should convert to BigInteger.ONE");
    }

    @Test
    void toBigInteger_NewBooleanFalse_ReturnsZero() {
        Object from = new Boolean(false);
        BigInteger result = BooleanConversions.toBigInteger(from, mockConverter);
        assertEquals(BigInteger.ZERO, result, "new Boolean(false) should convert to BigInteger.ZERO");
    }

    @Test
    void toBigInteger_True_ConverterThrowsIfUsed_Ignored() {
        Object from = Boolean.TRUE;
        Converter explodingConverter = new Converter() {

            @Override
            public <T> T convert(Object o, Class<T> c) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // when
        BigInteger result = BooleanConversions.toBigInteger(from, explodingConverter);
        // then
        assertEquals(BigInteger.ONE, result, "Even a throwing converter must be ignored; true should yield BigInteger.ONE");
    }

    @Test
    void toBigInteger_False_ConverterThrowsIfUsed_Ignored() {
        Object from = Boolean.FALSE;
        Converter explodingConverter = new Converter() {

            @Override
            public <T> T convert(Object o, Class<T> c) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // when
        BigInteger result = BooleanConversions.toBigInteger(from, explodingConverter);
        // then
        assertEquals(BigInteger.ZERO, result, "Even a throwing converter must be ignored; false should yield BigInteger.ZERO");
    }

    @Test
    void toBigInteger_True_ConverterSideEffect_NotInvoked() {
        Object from = Boolean.TRUE;
        // Converter that records whether convert() was called
        Converter spyConverter = new Converter() {

            boolean invoked = false;

            @Override
            public <T> T convert(Object o, Class<T> c) {
                invoked = true;
                return null;
            }

            boolean wasInvoked() {
                return invoked;
            }
        };
        // when
        BigInteger result = BooleanConversions.toBigInteger(from, spyConverter);
        // then
        assertEquals(BigInteger.ONE, result, "Result must be ONE for true");
        // Verify that the converter's convert method was never called
        assertFalse(((BooleanConversions_toBigInteger_Test) this).wasConverterInvoked(spyConverter), "Converter's convert method should not have been invoked");
    }

    @Test
    void toBigInteger_False_ConverterSideEffect_NotInvoked() {
        Object from = Boolean.FALSE;
        Converter spyConverter = new Converter() {

            boolean invoked = false;

            @Override
            public <T> T convert(Object o, Class<T> c) {
                invoked = true;
                return null;
            }

            boolean wasInvoked() {
                return invoked;
            }
        };
        // when
        BigInteger result = BooleanConversions.toBigInteger(from, spyConverter);
        // then
        assertEquals(BigInteger.ZERO, result, "Result must be ZERO for false");
        assertFalse(((BooleanConversions_toBigInteger_Test) this).wasConverterInvoked(spyConverter), "Converter's convert method should not have been invoked");
    }
}
