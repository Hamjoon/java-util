package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toAtomicInteger_Test {

    @Mock
    private Converter // Converter is not used by the method but required for the signature
    mockConverter;

    @AfterEach
    void teardownAfterEach() {
        // Clear reference to help GC (optional)
        mockConverter = null;
    }

    /**
     * Provides true/false AtomicBoolean instances together with the expected integer value.
     */
    private static Stream<Arguments> booleanSourceProvider() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), 1), Arguments.of(new AtomicBoolean(false), 0));
    }

    @Test
    void toAtomicInteger_whenAtomicBooleanTrue_returnsAtomicIntegerWithOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, mockConverter);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(1, result.get(), "AtomicBoolean true should map to AtomicInteger with value 1");
    }

    @Test
    void toAtomicInteger_whenAtomicBooleanFalse_returnsAtomicIntegerWithZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, mockConverter);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(0, result.get(), "AtomicBoolean false should map to AtomicInteger with value 0");
    }

    @Test
    void toAtomicInteger_whenSourceIsNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toAtomicInteger(source, mockConverter), "Passing null should result in NullPointerException because of the cast");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicInteger_whenSourceIsNotAtomicBoolean_throwsClassCastException() {
        // given
        Object source = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicInteger(source, mockConverter), "Passing an object that is not an AtomicBoolean should cause ClassCastException");
        verifyNoInteractions(mockConverter);
    }

    /**
     * Parameterized test covering the two valid boolean branches.
     *
     * @param source   the AtomicBoolean input
     * @param expected the expected integer value inside the resulting AtomicInteger
     */
    @ParameterizedTest(name = "toAtomicInteger({0}) → AtomicInteger({1})")
    @MethodSource("booleanSourceProvider")
    void toAtomicInteger_whenAtomicBoolean_returnsCorrespondingAtomicInteger(AtomicBoolean source, int expected) {
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, mockConverter);
        // then
        assertNotNull(result, "Result should never be null");
        assertEquals(expected, result.get(), () -> "AtomicBoolean " + source.get() + " should map to AtomicInteger with value " + expected);
        // The converter is not used – verify no interaction.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicInteger_whenConverterIsNull_stillWorksForTrue() {
        // This test mirrors the JSON where converter is null.
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, null);
        // then
        assertNotNull(result);
        assertEquals(1, result.get());
    }

    @Test
    void toAtomicInteger_whenConverterIsNull_stillWorksForFalse() {
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, null);
        // then
        assertNotNull(result);
        assertEquals(0, result.get());
    }
}
