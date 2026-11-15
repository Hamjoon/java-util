package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toLong_Test {

    /**
     * A Converter instance – not required for the conversion itself but part of the signature.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise a default Converter that can be reused across all tests.
        // The ConverterOptions constructor uses default settings.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static Converter reference.
        converter = null;
    }

    /**
     * Provides a matrix of inputs:
     * <ul>
     *   <li>Boolean.TRUE / Boolean.FALSE</li>
     *   <li>converter = null, a normal instance, or a mock that throws</li>
     * </ul>
     * Expected result is always the corresponding {@code CommonValues} constant.
     */
    private static Stream<Arguments> provideToLongParameters() {
        // a regular, non‑null Converter (same as the one created in @BeforeAll)
        Converter dummyConverter = new Converter(new ConverterOptions());
        // a mock that throws on any method invocation – the conversion must not call it
        Converter throwingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new IllegalStateException("Converter method should not be called");
        });
        return Stream.of(// null converter cases
        Arguments.of(Boolean.TRUE, null, CommonValues.LONG_ONE), Arguments.of(Boolean.FALSE, null, CommonValues.LONG_ZERO), // normal dummy converter cases
        Arguments.of(Boolean.TRUE, dummyConverter, CommonValues.LONG_ONE), Arguments.of(Boolean.FALSE, dummyConverter, CommonValues.LONG_ZERO), // throwing converter – should still succeed because the method never uses it
        Arguments.of(Boolean.TRUE, throwingConverter, CommonValues.LONG_ONE));
    }

    @Test
    void toLong_returnsOneWhenTrue() {
        // GIVEN
        Boolean source = Boolean.TRUE;
        // WHEN
        Long result = BooleanConversions.toLong(source, converter);
        // THEN
        assertEquals(CommonValues.LONG_ONE, result, "Converting Boolean.TRUE should yield CommonValues.LONG_ONE (1L)");
    }

    @Test
    void toLong_returnsZeroWhenFalse() {
        // GIVEN
        Boolean source = Boolean.FALSE;
        // WHEN
        Long result = BooleanConversions.toLong(source, converter);
        // THEN
        assertEquals(CommonValues.LONG_ZERO, result, "Converting Boolean.FALSE should yield CommonValues.LONG_ZERO (0L)");
    }

    @Test
    void toLong_throwsClassCastExceptionForNonBoolean() {
        // GIVEN
        Object notABoolean = "not a boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> BooleanConversions.toLong(notABoolean, converter), "Passing a non‑Boolean object should result in a ClassCastException");
    }

    @Test
    void toLong_throwsNullPointerExceptionWhenSourceIsNull() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> BooleanConversions.toLong(nullSource, converter), "Passing null as the source should result in a NullPointerException");
    }

    @ParameterizedTest(name = "from={0}, converter={1} => expected={2}")
    @MethodSource("provideToLongParameters")
    void toLong_behavesCorrectlyWithVariousConverters(Object from, Converter conv, Long expected) {
        // WHEN
        Long result = BooleanConversions.toLong(from, conv);
        // THEN
        assertEquals(expected, result, () -> String.format("Conversion of %s with converter %s should yield %d", from, (conv == null ? "null" : conv.getClass().getSimpleName()), expected));
    }
}
