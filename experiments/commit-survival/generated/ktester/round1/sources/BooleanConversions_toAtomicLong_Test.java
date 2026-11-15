package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class BooleanConversions_toAtomicLong_Test {

    @Mock
    private Converter // not used by the method, but required by the signature
    converter;

    @Mock
    private Converter // not used by the method, but required by the signature
    mockConverter;

    private static Stream<Arguments> booleanInputProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, 1L), Arguments.of(Boolean.FALSE, 0L));
    }

    @Test
    void toAtomicLong_givenTrue_returnsAtomicLongWithOne() {
        // given
        Boolean input = Boolean.TRUE;
        // when
        AtomicLong result = BooleanConversions.toAtomicLong(input, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(1L, result.get(), "AtomicLong value should be 1 for Boolean.TRUE");
    }

    @Test
    void toAtomicLong_givenFalse_returnsAtomicLongWithZero() {
        // given
        Boolean input = Boolean.FALSE;
        // when
        AtomicLong result = BooleanConversions.toAtomicLong(input, converter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(0L, result.get(), "AtomicLong value should be 0 for Boolean.FALSE");
    }

    @Test
    void toAtomicLong_givenNull_throwsNullPointerException() {
        // given
        Object input = null;
        // when / then
        assertThrows(NullPointerException.class, () -> BooleanConversions.toAtomicLong(input, mockConverter), "Passing null should result in NullPointerException due to unboxing");
    }

    /**
     * Verifies that a {@link Boolean} argument is correctly converted to an {@link AtomicLong}
     * when the {@code converter} parameter is {@code null}.
     */
    @ParameterizedTest(name = "toAtomicLong({0}) → AtomicLong({1})")
    @MethodSource("booleanInputProvider")
    void toAtomicLong_givenBooleanAndNullConverter_returnsExpectedAtomicLong(Object from, long expected) {
        // when
        AtomicLong result = BooleanConversions.toAtomicLong(from, null);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(expected, result.get(), () -> "AtomicLong value should be " + expected + " for input " + from);
    }

    @Test
    void toAtomicLong_givenTrue_andMockConverter_returnsAtomicLongWithOne() {
        // given
        Boolean input = Boolean.TRUE;
        // when
        AtomicLong result = BooleanConversions.toAtomicLong(input, mockConverter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(1L, result.get(), "AtomicLong value should be 1 for Boolean.TRUE");
    }

    @Test
    void toAtomicLong_givenFalse_andMockConverter_returnsAtomicLongWithZero() {
        // given
        Boolean input = Boolean.FALSE;
        // when
        AtomicLong result = BooleanConversions.toAtomicLong(input, mockConverter);
        // then
        assertNotNull(result, "Resulting AtomicLong should not be null");
        assertEquals(0L, result.get(), "AtomicLong value should be 0 for Boolean.FALSE");
    }

    @Test
    void toAtomicLong_givenString_throwsClassCastException() {
        // given
        Object input = "true";
        // when / then
        assertThrows(ClassCastException.class, () -> BooleanConversions.toAtomicLong(input, mockConverter), "A non‑Boolean object should cause a ClassCastException when cast to Boolean");
    }
}
