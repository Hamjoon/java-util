package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toInt_Test {

    /**
     * The {@link Converter} is not used by the method under test, but we provide a mock
     *  to satisfy the method signature and to demonstrate typical usage.
     */
    @Mock
    private Converter converter;

    /**
     * The {@link Converter} is not used by the method under test, but we provide a mock
     *  to satisfy the method signature and to demonstrate typical usage.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> validConversionProvider() {
        // a real (dummy) Converter instance – the class is empty for the test purpose
        Converter realConverter = new Converter() {
        };
        return Stream.of(// true with null converter
        Arguments.of(new AtomicBoolean(true), null, CommonValues.INTEGER_ONE), // false with null converter
        Arguments.of(new AtomicBoolean(false), null, CommonValues.INTEGER_ZERO), // true with a non‑null (real) converter
        Arguments.of(new AtomicBoolean(true), realConverter, CommonValues.INTEGER_ONE));
    }

    private static Stream<Arguments> exceptionProvider() {
        Converter realConverter = new Converter() {
        };
        return Stream.of(// null source, null converter
        Arguments.of(null, null, NullPointerException.class), // null source, non‑null converter
        Arguments.of(null, realConverter, NullPointerException.class), // non‑AtomicBoolean: Boolean
        Arguments.of(Boolean.TRUE, null, ClassCastException.class), // non‑AtomicBoolean: Integer
        Arguments.of(Integer.valueOf(1), null, ClassCastException.class));
    }

    @Test
    void toInt_returnsOneWhenAtomicBooleanIsTrue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Integer result = AtomicBooleanConversions.toInt(source, mockConverter);
        // then
        assertEquals(Integer.valueOf(1), result, "AtomicBoolean true should be converted to Integer 1");
    }

    @Test
    void toInt_returnsZeroWhenAtomicBooleanIsFalse() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Integer result = AtomicBooleanConversions.toInt(source, mockConverter);
        // then
        assertEquals(Integer.valueOf(0), result, "AtomicBoolean false should be converted to Integer 0");
    }

    @Test
    void toInt_throwsNullPointerExceptionWhenSourceIsNull() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toInt(source, mockConverter), "Passing null as the source should result in NPE");
    }

    @Test
    void toInt_throwsClassCastExceptionWhenSourceIsNotAtomicBoolean() {
        // given
        Object source = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toInt(source, mockConverter), "Passing a non‑AtomicBoolean object should result in ClassCastException");
    }

    /**
     * Valid conversion scenarios – both {@code null} and a real {@link Converter}
     * instance are supplied to prove that the argument is ignored.
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1} → expected={2}")
    @MethodSource("validConversionProvider")
    void toInt_validConversions(AtomicBoolean from, Converter converter, Integer expected) {
        // when
        Integer result = AtomicBooleanConversions.toInt(from, converter);
        // then
        assertEquals(expected, result, () -> String.format("AtomicBoolean %s should be converted to %d", from.get(), expected));
    }

    /**
     * Edge‑case scenarios that should result in exceptions.
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1} → throws {2}")
    @MethodSource("exceptionProvider")
    void toInt_invalidInputs(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> AtomicBooleanConversions.toInt(from, converter));
    }
}
