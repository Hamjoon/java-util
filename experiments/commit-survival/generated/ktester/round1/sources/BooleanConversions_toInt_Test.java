package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.stubbing.Answer;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toInt_Test {

    /**
     * A mock {@link Converter}. It is not used by the method under test but is required by the signature.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> branchCoverageArguments() {
        return Stream.of(// true → 1
        Arguments.of(Boolean.TRUE, CommonValues.INTEGER_ONE), // false → 0
        Arguments.of(Boolean.FALSE, CommonValues.INTEGER_ZERO), // null source → NullPointerException (Boolean unboxing)
        Arguments.of(null, NullPointerException.class), // non‑Boolean (Integer) → ClassCastException
        Arguments.of(42, ClassCastException.class));
    }

    private static Stream<Arguments> dummyConverterArguments() {
        return Stream.of(Arguments.of(Boolean.TRUE, CommonValues.INTEGER_ONE), Arguments.of(Boolean.FALSE, CommonValues.INTEGER_ZERO));
    }

    private static Stream<Arguments> throwingConverterArguments() {
        return Stream.of(Arguments.of(Boolean.TRUE, CommonValues.INTEGER_ONE), Arguments.of(Boolean.FALSE, CommonValues.INTEGER_ZERO));
    }

    @Test
    void toInt_whenBooleanTrue_returnsIntegerOne() {
        // given
        Boolean source = Boolean.TRUE;
        // when
        Integer result = BooleanConversions.toInt(source, mockConverter);
        // then
        assertEquals(CommonValues.INTEGER_ONE, result, "toInt should return CommonValues.INTEGER_ONE for Boolean.TRUE");
    }

    @Test
    void toInt_whenBooleanFalse_returnsIntegerZero() {
        // given
        Boolean source = Boolean.FALSE;
        // when
        Integer result = BooleanConversions.toInt(source, mockConverter);
        // then
        assertEquals(CommonValues.INTEGER_ZERO, result, "toInt should return CommonValues.INTEGER_ZERO for Boolean.FALSE");
    }

    @Test
    void toInt_whenSourceIsNotBoolean_throwsClassCastException() {
        // given
        Object source = "not a boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toInt(source, mockConverter), "toInt should throw ClassCastException when the source is not a Boolean");
    }

    @ParameterizedTest(name = "toInt({0}, null) → expected {1}")
    @MethodSource("branchCoverageArguments")
    void toInt_branchCoverage(Object source, Object expected) {
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> BooleanConversions.toInt(source, null), () -> "Expected " + exClass.getSimpleName() + " for source: " + source);
        } else {
            // Expected a normal Integer result
            Integer result = BooleanConversions.toInt(source, null);
            assertEquals(expected, result, () -> "toInt should return " + expected + " for source: " + source);
        }
    }

    @ParameterizedTest(name = "toInt({0}, dummyConverter) → expected {1}")
    @MethodSource("dummyConverterArguments")
    void toInt_withDummyConverter(Boolean source, Integer expected) {
        // A dummy (lenient) mock – does nothing and never throws.
        Converter dummyConverter = mock(Converter.class);
        Integer result = BooleanConversions.toInt(source, dummyConverter);
        assertEquals(expected, result, () -> "toInt should ignore the dummy converter and return " + expected);
    }

    @ParameterizedTest(name = "toInt({0}, throwingConverter) → expected {1}")
    @MethodSource("throwingConverterArguments")
    void toInt_withThrowingConverter(Boolean source, Integer expected) {
        // A converter that throws on any method call. The method under test must NOT call it.
        Converter throwingConverter = mock(Converter.class, (Answer<Object>) invocation -> {
            throw new RuntimeException("Converter should not be invoked");
        });
        Integer result = BooleanConversions.toInt(source, throwingConverter);
        assertEquals(expected, result, () -> "toInt should ignore the throwing converter and return " + expected);
    }
}
