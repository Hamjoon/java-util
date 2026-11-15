package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toShort_Test {

    @Mock
    private Converter // injected by Mockito
    mockConverter;

    private static Stream<Arguments> provideToShortConversionCases() {
        return Stream.of(// true → SHORT_ONE
        Arguments.of(Boolean.TRUE, null, CommonValues.SHORT_ONE, "When the input object is Boolean.TRUE the method should return CommonValues.SHORT_ONE (short value 1)."), // false → SHORT_ZERO
        Arguments.of(Boolean.FALSE, null, CommonValues.SHORT_ZERO, "When the input object is Boolean.FALSE the method should return CommonValues.SHORT_ZERO (short value 0)."), // null → NPE (unboxing a null Boolean)
        Arguments.of(null, NullPointerException.class, null, "A null Boolean argument leads to unboxing of a null reference, which should throw NullPointerException."), // non‑Boolean → ClassCastException
        Arguments.of("notABoolean", ClassCastException.class, null, "If the supplied object is not a Boolean (e.g., a String), the cast to Boolean fails and a ClassCastException must be thrown."));
    }

    private static Stream<Arguments> provideExplicitBooleanCases() {
        return Stream.of(// new Boolean(true) → SHORT_ONE
        Arguments.of(new Boolean(true), CommonValues.SHORT_ONE, "Pass an explicitly constructed Boolean true (new Boolean(true)); method should return Short value 1."), // new Boolean(false) → SHORT_ZERO
        Arguments.of(new Boolean(false), CommonValues.SHORT_ZERO, "Pass an explicitly constructed Boolean false (new Boolean(false)); method should return Short value 0."));
    }

    @Test
    void toShort_trueReturnsOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        Short result = BooleanConversions.toShort(source, mockConverter);
        // then
        assertEquals(CommonValues.SHORT_ONE, result, "Converting Boolean.TRUE should yield CommonValues.SHORT_ONE");
    }

    @Test
    void toShort_falseReturnsZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        Short result = BooleanConversions.toShort(source, mockConverter);
        // then
        assertEquals(CommonValues.SHORT_ZERO, result, "Converting Boolean.FALSE should yield CommonValues.SHORT_ZERO");
    }

    @Test
    void toShort_nullThrowsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toShort(source, mockConverter), "Converting null should throw NullPointerException due to unboxing");
    }

    @Test
    void toShort_nonBooleanThrowsClassCastException() {
        // given
        Object source = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toShort(source, mockConverter), "Converting a non‑Boolean object should throw ClassCastException");
    }

    /* -------------------------------------------------------------
     *  Parameterised tests – group: "toShort conversion"
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideToShortConversionCases")
    void toShort_conversion(Object from, Class<? extends Throwable> expectedException, Short expectedValue, String description) {
        // The converter is irrelevant for BooleanConversions.toShort, but we still pass the mock.
        if (expectedException != null) {
            // Expect an exception (null or wrong type)
            assertThrows(expectedException, () -> BooleanConversions.toShort(from, mockConverter), description);
        } else {
            // Normal conversion – verify the returned Short matches the expected constant.
            Short result = BooleanConversions.toShort(from, mockConverter);
            assertEquals(expectedValue, result, description);
        }
    }

    /* -------------------------------------------------------------
     *  Parameterised tests – group: "toShort conversion with explicit Boolean objects"
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("provideExplicitBooleanCases")
    void toShort_explicitBoolean(Object from, Short expectedValue, String description) {
        // No exception expected for these cases – they are valid Boolean instances.
        Short result = BooleanConversions.toShort(from, mockConverter);
        assertEquals(expectedValue, result, description);
    }
}
