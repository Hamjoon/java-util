package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toDouble_Test {

    @Mock
    private Converter // not used by the method but required for the signature
    mockConverter;

    /**
     * Provides a stream of arguments for the parameterised test.
     * Each argument consists of:
     *   - the source object {@code from}
     *   - the {@code Converter} instance (may be {@code null})
     *   - the expected outcome:
     *        * a {@link Double} value for successful conversion, or
     *        * a {@link Class} object representing the expected exception type.
     */
    private static Stream<Arguments> toDoubleArguments() {
        // a Converter mock that throws on any method invocation
        Converter throwingConverter = mock(Converter.class, invocation -> {
            throw new RuntimeException("Converter should not be called");
        });
        return Stream.of(// true → 1.0, null converter
        Arguments.of(Boolean.TRUE, null, 1.0d), // false → 0.0, null converter
        Arguments.of(Boolean.FALSE, null, 0.0d), // true → 1.0, non‑null mock converter (should be ignored)
        Arguments.of(Boolean.TRUE, mock(Converter.class), 1.0d), // false → 0.0, non‑null mock converter (should be ignored)
        Arguments.of(Boolean.FALSE, mock(Converter.class), 0.0d), // true → 1.0, converter that throws if called (method must not call it)
        Arguments.of(Boolean.TRUE, throwingConverter, 1.0d), // false → 0.0, converter that throws if called
        Arguments.of(Boolean.FALSE, throwingConverter, 0.0d), // distinct Boolean instances (new Boolean(true/false))
        Arguments.of(new Boolean(true), null, 1.0d), Arguments.of(new Boolean(false), null, 0.0d), // non‑Boolean source → ClassCastException
        Arguments.of(Integer.valueOf(5), null, ClassCastException.class), // null source → NullPointerException (already covered but kept for completeness)
        Arguments.of(null, null, NullPointerException.class));
    }

    @Test
    void toDouble_ShouldReturnOne_WhenBooleanIsTrue() {
        // given
        Boolean from = Boolean.TRUE;
        // when
        Double result = BooleanConversions.toDouble(from, mockConverter);
        // then
        assertEquals(CommonValues.DOUBLE_ONE, result, "Conversion of Boolean.TRUE should yield CommonValues.DOUBLE_ONE");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toDouble_ShouldReturnZero_WhenBooleanIsFalse() {
        // given
        Boolean from = Boolean.FALSE;
        // when
        Double result = BooleanConversions.toDouble(from, mockConverter);
        // then
        assertEquals(CommonValues.DOUBLE_ZERO, result, "Conversion of Boolean.FALSE should yield CommonValues.DOUBLE_ZERO");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toDouble_ShouldThrowNullPointerException_WhenInputIsNull() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toDouble(from, mockConverter), "Passing null should result in NullPointerException due to autounboxing");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("toDoubleArguments")
    void toDouble_VariousScenarios(Object from, Converter converter, Object expected) {
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> BooleanConversions.toDouble(from, converter), () -> "Expected " + exClass.getSimpleName() + " for input: " + from);
        } else {
            // Expected a Double result
            Double result = BooleanConversions.toDouble(from, converter);
            assertEquals(((Double) expected).doubleValue(), result.doubleValue(), 0.0, () -> "Unexpected conversion result for input: " + from);
            // Verify that the converter was never used (even if it is a mock)
            if (converter != null) {
                verifyNoInteractions(converter);
            }
        }
    }
}
