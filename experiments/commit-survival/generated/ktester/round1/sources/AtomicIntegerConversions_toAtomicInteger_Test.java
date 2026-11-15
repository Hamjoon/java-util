package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.time.LocalTime;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class AtomicIntegerConversions_toAtomicInteger_Test {

    @Mock
    private Converter // not used by the method but required by signature
    converter;

    private static Stream<org.junit.jupiter.params.provider.Arguments> validConversionProvider() {
        return Stream.of(arguments(new AtomicInteger(0), 0), arguments(new AtomicInteger(-12345), -12345), arguments(new AtomicInteger(Integer.MAX_VALUE), Integer.MAX_VALUE), arguments(new AtomicInteger(Integer.MIN_VALUE), Integer.MIN_VALUE), arguments(new AtomicInteger(42), 42));
    }

    @Test
    void shouldReturnNewAtomicIntegerWithSameValue() {
        // given
        AtomicInteger original = new AtomicInteger(42);
        // when
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(original, converter);
        // then
        assertNotNull(result, "Resulting AtomicInteger should not be null");
        assertEquals(original.intValue(), result.intValue(), "Resulting AtomicInteger should have the same int value as the original");
        assertNotSame(original, result, "Resulting AtomicInteger should be a new instance, not the same reference");
    }

    @Test
    void shouldThrowClassCastExceptionWhenInputIsNotAtomicInteger() {
        // given
        Object notAtomic = "I am not an AtomicInteger";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicIntegerConversions.toAtomicInteger(notAtomic, converter), "Method should throw ClassCastException when the source object is not an AtomicInteger");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldThrowNullPointerExceptionWhenFromIsNull() {
        // given
        Object nullFrom = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicIntegerConversions.toAtomicInteger(nullFrom, converter), "Passing a null `from` argument should result in a NullPointerException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldIgnoreConverterEvenIfItThrowsOnInvocation() {
        // given: a Converter mock that throws on any method call
        Converter throwingConverter = Mockito.mock(Converter.class);
        Mockito.doThrow(new UnsupportedOperationException("converter should not be called")).when(throwingConverter).// any method, to illustrate behaviour
        toString();
        AtomicInteger input = new AtomicInteger(-7);
        // when
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(input, throwingConverter);
        // then
        assertNotNull(result);
        assertEquals(-7, result.intValue());
        assertNotSame(input, result);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void shouldPropagateExceptionFromAtomicIntegerIntValue() {
        // given: a mock AtomicInteger whose intValue() throws RuntimeException
        AtomicInteger faultyAtomic = Mockito.mock(AtomicInteger.class);
        RuntimeException failure = new RuntimeException("intValue failure");
        Mockito.when(faultyAtomic.intValue()).thenThrow(failure);
        // when / then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> AtomicIntegerConversions.toAtomicInteger(faultyAtomic, converter), "The RuntimeException from intValue() should be propagated");
        assertSame(failure, thrown, "The same exception instance should be propagated");
    }
}
