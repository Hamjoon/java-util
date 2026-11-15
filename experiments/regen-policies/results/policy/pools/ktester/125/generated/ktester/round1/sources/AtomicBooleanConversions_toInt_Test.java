package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toInt_Test {

    /**
     * Mocked Converter – the method under test should not call any of its methods.
     */
    @Mock
    private Converter mockConverter;

    /* --------------------------------------------------------------
     *  Helper holder for parameterized arguments.
     * -------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final AtomicBoolean from;

        final Converter converter;

        final Integer expected;

        final String description;

        ArgumentsHolder(AtomicBoolean from, Converter converter, Integer expected, String description) {
            this.from = from;
            this.converter = converter;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /* --------------------------------------------------------------
     *  Parameterized tests for the normal true/false cases.
     * -------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> normalCases() {
        return Stream.of(new ArgumentsHolder(new AtomicBoolean(true), null, CommonValues.INTEGER_ONE, "true, null converter"), new ArgumentsHolder(new AtomicBoolean(false), null, CommonValues.INTEGER_ZERO, "false, null converter"), new ArgumentsHolder(new AtomicBoolean(true), mock(Converter.class), CommonValues.INTEGER_ONE, "true, mock converter"), new ArgumentsHolder(new AtomicBoolean(false), mock(Converter.class), CommonValues.INTEGER_ZERO, "false, mock converter"));
    }

    @Test
    void toInt_whenAtomicBooleanTrue_returnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Integer result = AtomicBooleanConversions.toInt(source, mockConverter);
        // then
        assertEquals(CommonValues.INTEGER_ONE, result, "AtomicBoolean true should be converted to Integer 1");
        // verify that the converter was not used
        // (no interactions expected)
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInt_whenAtomicBooleanFalse_returnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Integer result = AtomicBooleanConversions.toInt(source, mockConverter);
        // then
        assertEquals(CommonValues.INTEGER_ZERO, result, "AtomicBoolean false should be converted to Integer 0");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInt_whenSourceIsNotAtomicBoolean_throwsClassCastException() {
        // given
        Object notAtomic = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toInt(notAtomic, mockConverter), "Passing a non‑AtomicBoolean should result in ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "toInt({1}) → {2}")
    @MethodSource("normalCases")
    void toInt_normalCases(ArgumentsHolder holder) {
        // when
        Integer result = AtomicBooleanConversions.toInt(holder.from, holder.converter);
        // then
        assertEquals(holder.expected, result, "AtomicBoolean " + holder.from.get() + " should be converted to " + holder.expected);
        // the method must not interact with the converter, even if it is a mock
        verifyNoInteractions(holder.converter);
    }

    /* --------------------------------------------------------------
     *  Tests for exceptional situations.
     * -------------------------------------------------------------- */
    @Test
    void toInt_whenSourceIsNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toInt(source, mockConverter), "Passing null as the source should throw NullPointerException");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toInt_withConverterThatThrows_isIgnored() {
        // given a Converter that would throw if called
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object o, Class<?> c) {
                throw new RuntimeException("should not be called");
            }
        };
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Integer result = AtomicBooleanConversions.toInt(source, throwingConverter);
        // then
        assertEquals(CommonValues.INTEGER_ONE, result, "Method must ignore the converter even if it throws");
        // No way to verify interaction because we used a concrete instance,
        // but the absence of exception proves it was not called.
    }

    @Test
    void toInt_withConverterThatThrows_andFalseValue_isIgnored() {
        // given a Converter that would throw if called
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object o, Class<?> c) {
                throw new RuntimeException("should not be called");
            }
        };
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Integer result = AtomicBooleanConversions.toInt(source, throwingConverter);
        // then
        assertEquals(CommonValues.INTEGER_ZERO, result, "Method must ignore the converter even if it throws");
    }
}
