package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toAtomicBoolean_Test {

    /**
     * Mocked Converter – not used by the method under test but required for the signature.
     */
    @Mock
    private Converter converter;

    /**
     * Mocked Converter – not used by the method under test but required for the signature.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> provideNormalBehaviorCases() {
        return Stream.of(// null converter
        Arguments.of(true, true), // null converter
        Arguments.of(false, true), // non‑null mock converter
        Arguments.of(true, false), // non‑null mock converter
        Arguments.of(false, false));
    }

    private static Stream<Arguments> provideInvalidTypeCases() {
        return Stream.of(Arguments.of(Boolean.TRUE), Arguments.of(new AtomicInteger(5)));
    }

    @Test
    void toAtomicBoolean_copiesTrueValue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.get(), "Result should contain true");
        assertNotSame(source, result, "Result should be a new instance, not the original");
    }

    @Test
    void toAtomicBoolean_copiesFalseValue() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.get(), "Result should contain false");
        assertNotSame(source, result, "Result should be a new instance, not the original");
    }

    @Test
    void toAtomicBoolean_throwsClassCastExceptionWhenInputIsNotAtomicBoolean() {
        // given
        Object notAnAtomicBoolean = "I am not an AtomicBoolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicBoolean(notAnAtomicBoolean, converter), "Method should throw ClassCastException for invalid input type");
    }

    @ParameterizedTest(name = "{index} => value={0}, converterIsNull={1}")
    @MethodSource("provideNormalBehaviorCases")
    void toAtomicBoolean_copiesValue(boolean value, boolean converterIsNull) {
        // given
        AtomicBoolean source = new AtomicBoolean(value);
        Converter conv = converterIsNull ? null : mockConverter;
        // when
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, conv);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(value, result.get(), "Result should contain the same boolean value");
        assertNotSame(source, result, "Result must be a different instance from the source");
    }

    @Test
    void toAtomicBoolean_nullFrom_throwsNullPointerException() {
        // given
        Object from = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toAtomicBoolean(from, null), "Passing null as 'from' should result in NullPointerException");
    }

    @ParameterizedTest(name = "{index} => invalidObject={0}")
    @MethodSource("provideInvalidTypeCases")
    void toAtomicBoolean_invalidType_throwsClassCastException(Object invalid) {
        // given
        // converter is irrelevant – pass null
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicBoolean(invalid, null), "Method should throw ClassCastException for input type: " + invalid.getClass().getSimpleName());
    }

    @Test
    void toAtomicBoolean_copiesTrueValue_explicit() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, null);
        // then
        assertNotNull(result);
        assertTrue(result.get());
        assertNotSame(source, result);
    }

    @Test
    void toAtomicBoolean_copiesFalseValue_explicit() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        AtomicBoolean result = AtomicBooleanConversions.toAtomicBoolean(source, null);
        // then
        assertNotNull(result);
        assertFalse(result.get());
        assertNotSame(source, result);
    }

    @Test
    void toAtomicBoolean_throwsClassCastExceptionWhenInputIsNotAtomicBoolean_explicit() {
        // given
        Object notAnAtomicBoolean = "I am not an AtomicBoolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicBoolean(notAnAtomicBoolean, null));
    }
}
