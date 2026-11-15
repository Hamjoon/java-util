package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class NumberConversions_toFloatZero_Test {

    /**
     * A mocked {@link Converter} – the implementation is irrelevant for this method.
     */
    @Mock
    Converter mockConverter;

    /**
     * Provider for the *basic behaviour* group.
     */
    static Stream<Arguments> basicBehaviorProvider() {
        // A real, but otherwise inert, Converter instance.
        Converter realConverter = new Converter(new ConverterOptions());
        return Stream.of(Arguments.of(null, null, "Both arguments are null – should still return zero"), Arguments.of(123, null, "Integer source, null converter – still zero"), Arguments.of("42.5", null, "String numeric source, null converter – still zero"), Arguments.of(new Date(), null, "Date instance, null converter – still zero"), Arguments.of(null, realConverter, "Null source with a real Converter – still zero"), Arguments.of(new Object(), realConverter, "Arbitrary object with a real Converter – still zero"));
    }

    /**
     * Provider for the *diverse inputs* group.
     */
    static Stream<Arguments> diverseInputsProvider() {
        return Stream.of(Arguments.of(Float.MAX_VALUE, null, "Maximum float value – ignored"), Arguments.of(Float.NaN, null, "Float NaN – ignored"), Arguments.of(Double.POSITIVE_INFINITY, null, "Double positive infinity – ignored"), Arguments.of(new BigDecimal("12345.6789"), null, "BigDecimal instance – ignored"), Arguments.of(new BigInteger("9876543210"), null, "BigInteger instance – ignored"), Arguments.of(new int[] { 1, 2, 3 }, null, "Primitive int array – ignored"), Arguments.of(Collections.emptyList(), null, "Empty collection – ignored"), Arguments.of(new Object() {

            @Override
            public String toString() {
                return "99.9";
            }
        }, null, "Object with overridden toString returning numeric string – ignored"), Arguments.of("not a number", null, "Non‑numeric string – ignored"), Arguments.of(Long.valueOf(999L), null, "Long wrapper – ignored"));
    }

    /**
     * Provider for the *converter side‑effects* group.
     */
    static Stream<Arguments> converterSideEffectsProvider() {
        // Converter that throws if its convert method is ever called.
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                throw new RuntimeException("Converter should never be invoked");
            }
        };
        // Converter that merely logs; we verify that no log appears by the fact that the test passes.
        Converter loggingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> target) {
                System.out.println("Converter invoked with: " + src);
                return null;
            }
        };
        return Stream.of(Arguments.of(null, throwingConverter, "Converter that throws if invoked – method must not call it"), Arguments.of("any", loggingConverter, "Converter that logs when used – method must not invoke it"));
    }

    @Test
    void toFloatZeroReturnsZeroWhenSourceIsNull() {
        Float result = NumberConversions.toFloatZero(null, mockConverter);
        assertEquals(Float.valueOf(0f), result, "Expected zero Float for null source");
    }

    @Test
    void toFloatZeroReturnsZeroWhenSourceIsANumber() {
        Float result = NumberConversions.toFloatZero(123, mockConverter);
        assertEquals(Float.valueOf(0f), result, "Expected zero Float for numeric source");
    }

    @Test
    void toFloatZeroReturnsZeroWhenSourceIsAString() {
        Float result = NumberConversions.toFloatZero("45.67", mockConverter);
        assertEquals(Float.valueOf(0f), result, "Expected zero Float for String source");
    }

    @Test
    void toFloatZeroReturnsZeroWhenSourceIsAnObject() {
        Object dummy = new Object();
        Float result = NumberConversions.toFloatZero(dummy, mockConverter);
        assertEquals(Float.valueOf(0f), result, "Expected zero Float for arbitrary object source");
    }

    @Test
    void toFloatZeroIgnoresConverterImplementation() {
        // Create a real Converter with default options to prove that the method does not depend on it.
        Converter realConverter = new Converter(new ConverterOptions());
        Float result = NumberConversions.toFloatZero("ignored", realConverter);
        assertEquals(Float.valueOf(0f), result, "Expected zero Float regardless of real Converter");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicBehaviorProvider")
    void toFloatZeroBasicBehavior(Object from, Converter converter, String description) {
        Float result = NumberConversions.toFloatZero(from, converter);
        assertEquals(Float.valueOf(0f), result, description);
        // The method returns a shared constant – verify the exact instance.
        assertSame(CommonValues.FLOAT_ZERO, result, "Should return the shared constant");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("diverseInputsProvider")
    void toFloatZeroDiverseInputs(Object from, Converter converter, String description) {
        Float result = NumberConversions.toFloatZero(from, converter);
        assertEquals(Float.valueOf(0f), result, description);
        assertSame(CommonValues.FLOAT_ZERO, result);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("converterSideEffectsProvider")
    void toFloatZeroConverterSideEffects(Object from, Converter converter, String description) {
        Float result = NumberConversions.toFloatZero(from, converter);
        assertEquals(Float.valueOf(0f), result, description);
        assertSame(CommonValues.FLOAT_ZERO, result);
    }
}
