package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toLong_Test {

    /**
     * A mock {@link Converter}. The method under test does not use it, but the signature requires it.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Simple {@link Converter} implementation used for the “non‑null converter” test cases.
     * The class does not implement any behaviour because {@code BooleanConversions.toLong}
     * never invokes the converter.
     */
    private static class MockConverter implements Converter {
        // No methods required for the test – this is a marker implementation.
    }

    /**
     * A {@link Converter} that throws an exception on any method call.
     * The method under test never calls the converter, so the exception should never be observed.
     */
    private static class ThrowingConverter implements Converter {

        public ThrowingConverter() {
            // Intentionally empty – any real method would throw.
        }
        // If the {@code Converter} interface defines methods, they should be overridden
        // to throw. As the interface is unknown in this context, we rely on the fact
        // that {@code BooleanConversions.toLong} never touches the converter.
    }

    /**
     * Provides a stream of arguments for successful conversions.
     *
     * Each argument consists of:
     *   - the source object (must be a {@link Boolean} or autoboxed primitive)
     *   - the {@link Converter} instance to pass (null or a mock)
     *   - the expected {@link Long} value
     */
    private static Stream<Arguments> successProvider() {
        return Stream.of(// true / false with a null converter (already covered above, but kept for completeness)
        Arguments.of(Boolean.TRUE, null, CommonValues.LONG_ONE), Arguments.of(Boolean.FALSE, null, CommonValues.LONG_ZERO), // true / false with a non‑null mock converter
        Arguments.of(Boolean.TRUE, new MockConverter(), CommonValues.LONG_ONE), Arguments.of(Boolean.FALSE, new MockConverter(), CommonValues.LONG_ZERO), // new Boolean instances (not the cached constants)
        Arguments.of(new Boolean(true), null, CommonValues.LONG_ONE), Arguments.of(new Boolean(false), null, CommonValues.LONG_ZERO), // Boolean.valueOf(null) returns Boolean.FALSE
        Arguments.of(Boolean.valueOf(null), null, CommonValues.LONG_ZERO), // primitive booleans are autoboxed when passed as Object
        Arguments.of(true, null, CommonValues.LONG_ONE), Arguments.of(false, null, CommonValues.LONG_ZERO));
    }

    /**
     * Provides a stream of arguments that are expected to throw.
     *
     * Each argument consists of:
     *   - the source object (null or non‑Boolean)
     *   - the {@link Converter} instance to pass (null or a mock)
     *   - the expected exception class
     */
    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// Null source – NPE from the cast
        Arguments.of(null, null, NullPointerException.class), // Non‑Boolean objects – ClassCastException from the cast
        Arguments.of("some string", null, ClassCastException.class), Arguments.of(123, null, ClassCastException.class));
    }

    @Test
    void toLong_whenTrue_returnsLongOne() {
        // GIVEN
        Boolean from = Boolean.TRUE;
        // WHEN
        Long result = BooleanConversions.toLong(from, mockConverter);
        // THEN
        assertEquals(CommonValues.LONG_ONE, result, "Converting Boolean.TRUE should yield CommonValues.LONG_ONE");
    }

    @Test
    void toLong_whenFalse_returnsLongZero() {
        // GIVEN
        Boolean from = Boolean.FALSE;
        // WHEN
        Long result = BooleanConversions.toLong(from, mockConverter);
        // THEN
        assertEquals(CommonValues.LONG_ZERO, result, "Converting Boolean.FALSE should yield CommonValues.LONG_ZERO");
    }

    @Test
    void toLong_whenNull_throwsNullPointerException() {
        // GIVEN
        Object from = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toLong(from, mockConverter), "Passing null should result in NullPointerException");
    }

    @Test
    void toLong_whenNotBoolean_throwsClassCastException() {
        // GIVEN
        Object from = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toLong(from, mockConverter), "Passing a non‑Boolean should result in ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("successProvider")
    void toLong_successfulConversions(Object from, Converter converter, Long expected) {
        // WHEN
        Long result = BooleanConversions.toLong(from, converter);
        // THEN
        assertEquals(expected, result, () -> String.format("Converting %s with converter %s should yield %s", from, converter, expected));
    }

    @ParameterizedTest(name = "[{index}] from={0} expects {2}")
    @MethodSource("exceptionProvider")
    void toLong_expectedExceptions(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> BooleanConversions.toLong(from, converter), () -> String.format("Converting %s should throw %s", from, expectedException.getSimpleName()));
    }

    @Test
    void toLong_withThrowingConverter_stillReturnsLongOne() {
        // GIVEN
        Boolean from = Boolean.TRUE;
        Converter throwing = new ThrowingConverter();
        // WHEN
        Long result = BooleanConversions.toLong(from, throwing);
        // THEN
        assertEquals(CommonValues.LONG_ONE, result, "Even when a Converter that throws is supplied, the result must be LONG_ONE");
    }
}
