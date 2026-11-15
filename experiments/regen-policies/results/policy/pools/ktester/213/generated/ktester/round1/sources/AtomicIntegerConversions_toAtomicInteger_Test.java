package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class AtomicIntegerConversions_toAtomicInteger_Test {

    @Mock
    private Converter // not used by the method, but required by signature
    mockConverter;

    /* --------------------------------------------------------------
     *  Dummy Converter implementation used in one test case
     * -------------------------------------------------------------- */
    private static class DummyConverter implements Converter {
        // No methods are needed for the current tests because the conversion method never calls them.
    }

    @BeforeEach
    void setupBeforeEach() {
        // Mockito @Mock fields are initialised automatically by the extension.
        // Ensure the mock has no unexpected stubbing.
        reset(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method under test does not interact with the Converter.
        verifyNoInteractions(mockConverter);
    }

    private static Stream<Integer> validAtomicIntegerProvider() {
        return Stream.of(0, -42, Integer.MAX_VALUE, Integer.MIN_VALUE);
    }

    private static Stream<Arguments> invalidSourceProvider() {
        return Stream.of(Arguments.of(Integer.valueOf(5)), Arguments.of("some string"));
    }

    @Test
    void toAtomicInteger_returnsNewInstanceWithSameValue() {
        // given
        AtomicInteger source = new AtomicInteger(42);
        // when
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(source.intValue(), result.intValue(), "Result should contain the same int value as the source");
        assertNotSame(source, result, "Result should be a new AtomicInteger instance, not the original");
    }

    @Test
    void toAtomicInteger_returnsNewInstanceWhenSourceIsZero() {
        // given
        AtomicInteger source = new AtomicInteger(0);
        // when
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, mockConverter);
        // then
        assertEquals(0, result.intValue(), "Result should correctly represent zero value");
        assertNotSame(source, result, "Result should be a distinct instance even when the value is zero");
    }

    @Test
    void toAtomicInteger_throwsClassCastException_forInvalidSourceType() {
        // given
        Object invalidSource = "not an AtomicInteger";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicIntegerConversions.toAtomicInteger(invalidSource, mockConverter), "Method should throw ClassCastException when source is not an AtomicInteger");
    }

    /* --------------------------------------------------------------
     *  Null input handling – expect NullPointerException
     * -------------------------------------------------------------- */
    @Test
    void toAtomicInteger_throwsNullPointerException_whenSourceIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicIntegerConversions.toAtomicInteger(nullSource, mockConverter), "Method should throw NullPointerException when source is null");
    }

    /* --------------------------------------------------------------
     *  Converter non‑null usage – method should ignore the converter
     * -------------------------------------------------------------- */
    @Test
    void toAtomicInteger_ignoresNonNullConverter_andReturnsCorrectValue() {
        // given
        AtomicInteger source = new AtomicInteger(123);
        // not used by the method
        Converter dummyConverter = new DummyConverter();
        // when
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, dummyConverter);
        // then
        assertNotNull(result);
        assertEquals(123, result.intValue());
        assertNotSame(source, result);
    }

    /* --------------------------------------------------------------
     *  Returned instance independence – mutating the original does not affect result
     * -------------------------------------------------------------- */
    @Test
    void toAtomicInteger_returnedInstanceIsIndependentFromSource() {
        // given
        AtomicInteger source = new AtomicInteger(7);
        // when
        AtomicInteger result = AtomicIntegerConversions.toAtomicInteger(source, mockConverter);
        // mutate the original after conversion
        source.set(99);
        // then
        assertEquals(7, result.intValue(), "Result must retain the original value even after the source is mutated");
        assertEquals(99, source.intValue(), "Source should reflect the mutation");
    }
}
