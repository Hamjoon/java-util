package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toFloat_Test {

    /**
     * A mock Converter – the BooleanConversions.toFloat implementation does not use it,
     *  but the method signature requires it.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideSuccessfulConversions() {
        // a dummy (non‑null) converter – plain mock with default (do nothing) behaviour
        Converter dummyConverter = Mockito.mock(Converter.class);
        // a converter that throws on any method call – created with a default Answer that throws
        Converter throwingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be invoked");
        });
        return Stream.of(// Group 1 – Boolean handling (converter is null)
        arguments(Boolean.TRUE, null, CommonValues.FLOAT_ONE), arguments(Boolean.FALSE, null, CommonValues.FLOAT_ZERO), // Group 2 – Converter parameter handling
        arguments(Boolean.TRUE, dummyConverter, CommonValues.FLOAT_ONE), arguments(Boolean.FALSE, dummyConverter, CommonValues.FLOAT_ZERO), arguments(Boolean.TRUE, throwingConverter, CommonValues.FLOAT_ONE), arguments(Boolean.FALSE, throwingConverter, CommonValues.FLOAT_ZERO));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> provideExceptionConversions() {
        // dummy converter – same as above
        Converter dummyConverter = Mockito.mock(Converter.class);
        // throwing converter – same as above
        Converter throwingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be invoked");
        });
        return Stream.of(// Group 1 – Boolean handling (converter is null)
        arguments(null, null, NullPointerException.class), arguments("notABoolean", null, ClassCastException.class), // Group 2 – Converter parameter handling (dummy converter)
        arguments(null, dummyConverter, NullPointerException.class), arguments("notABoolean", dummyConverter, ClassCastException.class), // Group 2 – Converter parameter handling (throwing converter)
        arguments(null, throwingConverter, NullPointerException.class), arguments("notABoolean", throwingConverter, ClassCastException.class));
    }

    @Test
    void toFloat_givenTrueBoolean_returnsOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        Float result = BooleanConversions.toFloat(source, mockConverter);
        // then
        assertEquals(CommonValues.FLOAT_ONE, result, "Converting Boolean.TRUE should yield CommonValues.FLOAT_ONE");
    }

    @Test
    void toFloat_givenFalseBoolean_returnsZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        Float result = BooleanConversions.toFloat(source, mockConverter);
        // then
        assertEquals(CommonValues.FLOAT_ZERO, result, "Converting Boolean.FALSE should yield CommonValues.FLOAT_ZERO");
    }

    @Test
    void toFloat_givenNullSource_throwsClassCastException() {
        // given
        Object source = null;
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toFloat(source, mockConverter), "Passing null (or a non‑Boolean) should result in a ClassCastException");
    }

    @Test
    void toFloat_givenNonBooleanObject_throwsClassCastException() {
        // given
        Object source = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toFloat(source, mockConverter), "Passing a non‑Boolean object should result in a ClassCastException");
    }

    /**
     * Parameterized test covering all scenarios where the method should return a Float.
     *
     * <p>The JSON test data defines six successful cases:
     * <ul>
     *   <li>Boolean true → 1.0f (with null, dummy or throwing converter)</li>
     *   <li>Boolean false → 0.0f (with null, dummy or throwing converter)</li>
     * </ul>
     * </p>
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("provideSuccessfulConversions")
    void toFloat_successful(Object from, Converter converter, Float expected) {
        // when
        Float result = BooleanConversions.toFloat(from, converter);
        // then
        assertEquals(expected, result, () -> String.format("Converting %s should yield %s", from, expected));
    }

    /**
     * Parameterized test covering all scenarios where the method is expected
     * to throw an exception (either {@link NullPointerException} or
     * {@link ClassCastException}).
     *
     * <p>The JSON test data defines four exception cases for each of the two
     * groups (null source and non‑Boolean source), totalling eight cases.</p>
     */
    @ParameterizedTest(name = "[{index}] from={0}, converter={1} → expect {2}")
    @MethodSource("provideExceptionConversions")
    void toFloat_exceptions(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // when / then
        assertThrows(expectedException, () -> BooleanConversions.toFloat(from, converter), () -> String.format("Converting %s should throw %s", from, expectedException.getSimpleName()));
    }
}
