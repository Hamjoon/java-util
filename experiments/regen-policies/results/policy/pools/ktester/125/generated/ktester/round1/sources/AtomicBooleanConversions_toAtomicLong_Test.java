package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toAtomicLong_Test {

    /**
     * Shared Converter instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter does not affect the conversion logic, but we create a valid instance
        // for the tests that explicitly pass a non‑null value.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the shared Converter reference.
        converter = null;
    }

    /**
     * Supplies true/false cases together with both a real and a null Converter.
     */
    private static Stream<Arguments> validBooleanProvider() {
        AtomicBoolean trueBool = new AtomicBoolean(true);
        AtomicBoolean falseBool = new AtomicBoolean(false);
        return Stream.of(// description, source, converter, expected long value
        Arguments.of("True value with non‑null Converter", trueBool, converter, 1L), Arguments.of("True value with null Converter", trueBool, null, 1L), Arguments.of("False value with non‑null Converter", falseBool, converter, 0L), Arguments.of("False value with null Converter", falseBool, null, 0L));
    }

    @Test
    void toAtomicLong_returnsOneWhenAtomicBooleanIsTrue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(1L, result.get(), "AtomicLong value should be 1 for true");
    }

    @Test
    void toAtomicLong_returnsZeroWhenAtomicBooleanIsFalse() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(0L, result.get(), "AtomicLong value should be 0 for false");
    }

    @Test
    void toAtomicLong_throwsClassCastExceptionForInvalidSourceType() {
        // given
        Object invalidSource = "not an AtomicBoolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicLong(invalidSource, converter), "Expected ClassCastException when source is not an AtomicBoolean");
    }

    @Test
    void toAtomicLong_throwsNullPointerExceptionWhenSourceIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toAtomicLong(nullSource, converter), "Expected NullPointerException when source is null");
    }

    /**
     * Verifies that a true {@link AtomicBoolean} yields an {@link AtomicLong}
     * containing {@code 1}, and a false value yields {@code 0}.
     *
     * The test runs twice: once with a real {@link Converter} instance and once
     * with {@code null} to confirm that the converter argument is truly ignored.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validBooleanProvider")
    void toAtomicLong_returnsExpectedValue(String description, AtomicBoolean source, Converter suppliedConverter, long expected) {
        // when
        AtomicLong result = AtomicBooleanConversions.toAtomicLong(source, suppliedConverter);
        // then
        assertNotNull(result, "Result must not be null");
        assertEquals(expected, result.get(), () -> "AtomicLong value should be " + expected + " for source " + source);
    }
}
