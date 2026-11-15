package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toIntZero_Test {

    @Mock
    private Converter // mocked, but should not be used by the method
    converter;

    @Mock
    private Converter // Mockito mock – should never be used
    mockConverter;

    class TrackingConverter implements Converter {

        boolean called = false;

        @Override
        public <T> T convert(Object src, Class<T> target) {
            called = true;
            return null;
        }

        boolean wasCalled() {
            return called;
        }
    }

    /**
     * Simple custom Number subclass used in one of the test cases.
     */
    private static class MyNumber extends Number {

        private final int value;

        MyNumber(int value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return value;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public double doubleValue() {
            return value;
        }
    }

    class TrackingConverter implements Converter {

        boolean called = false;

        @Override
        public <T> T convert(Object src, Class<T> target) {
            called = true;
            return null;
        }

        boolean wasCalled() {
            return called;
        }
    }

    /**
     * Supplies all source objects described in the JSON test cases.
     */
    private static Stream<Arguments> provideFromValues() {
        return Stream.of(// null
        Arguments.of((Object) null), // Simple non‑numeric objects
        Arguments.of("123"), Arguments.of(42), Arguments.of(new BigDecimal("123.45")), Arguments.of(new Date()), Arguments.of(new Long(123L)), Arguments.of(new Short((short) 5)), Arguments.of(new Byte((byte) 2)), Arguments.of(new AtomicInteger(7)), Arguments.of(new AtomicLong(9L)), Arguments.of(Boolean.TRUE), Arguments.of(Character.valueOf('a')), Arguments.of(new int[] { 1, 2, 3 }), Arguments.of(Arrays.asList(1, 2, 3)), Arguments.of(new MyNumber(42)), // Additional numeric wrappers
        Arguments.of(BigInteger.valueOf(999)), // Any other object
        Arguments.of("any"));
    }

    @Test
    void toIntZero_returnsZeroForNullInput() {
        Integer result = NumberConversions.toIntZero(null, converter);
        assertEquals(0, result);
    }

    @Test
    void toIntZero_returnsZeroForStringInput() {
        Integer result = NumberConversions.toIntZero("12345", converter);
        assertEquals(0, result);
    }

    @Test
    void toIntZero_returnsZeroForNumericInput() {
        Integer result = NumberConversions.toIntZero(9876L, converter);
        assertEquals(0, result);
    }

    @Test
    void toIntZero_doesNotInteractWithConverter() {
        // Call the method with a non‑null value to ensure the converter is truly unused
        NumberConversions.toIntZero("any", converter);
        // Verify that the mock converter was never called
        Mockito.verifyNoInteractions(converter);
    }

    /**
     * Parameterized test that supplies a wide variety of {@code from} arguments.
     * The expected result is always {@link CommonValues#INTEGER_ZERO}.
     */
    @ParameterizedTest(name = "{index}: from={0}")
    @MethodSource("provideFromValues")
    void toIntZero_returnsCanonicalZero(Object from) {
        // Act
        Integer result = NumberConversions.toIntZero(from, mockConverter);
        // Assert value is zero
        assertEquals(0, result.intValue(), "toIntZero should always return an Integer with value 0");
        // Assert identity – the method must return the shared constant instance
        assertSame(CommonValues.INTEGER_ZERO, result, "Returned Integer must be the canonical instance from CommonValues");
        // Verify that the converter mock was never touched
        verifyNoInteractions(mockConverter);
    }

    /**
     * Verify that a custom {@link Converter} implementation that would throw
     * if invoked is never called.
     */
    @Test
    void toIntZero_doesNotInvokeThrowingConverter() {
        // Arrange: a Converter that throws when its convert method is called
        Converter throwingConverter = new Converter() {

            @Override
            public <T> T convert(Object src, Class<T> target) {
                throw new RuntimeException("Converter should not be invoked");
            }
        };
        // Act
        Integer result = NumberConversions.toIntZero("ignored", throwingConverter);
        // Assert
        assertEquals(0, result.intValue());
        assertSame(CommonValues.INTEGER_ZERO, result);
    }

    /**
     * Verify that a custom {@link Converter} implementation that records a call
     * is not invoked.
     */
    @Test
    void toIntZero_doesNotInvokeTrackingConverter() {
        // Arrange: a Converter that records whether it was called
        class TrackingConverter implements Converter {

            boolean called = false;

            @Override
            public <T> T convert(Object src, Class<T> target) {
                called = true;
                return null;
            }

            boolean wasCalled() {
                return called;
            }
        }
        TrackingConverter trackingConverter = new TrackingConverter();
        // Act
        Integer result = NumberConversions.toIntZero(null, trackingConverter);
        // Assert
        assertEquals(0, result.intValue());
        assertSame(CommonValues.INTEGER_ZERO, result);
        assertFalse(trackingConverter.wasCalled(), "Converter should not have been invoked by toIntZero");
    }
}
